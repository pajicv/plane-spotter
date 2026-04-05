package com.planespotter.ui.map

import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.planespotter.data.model.Flight
import com.planespotter.ui.theme.HudColors
import com.planespotter.ui.theme.HudTypography
import com.planespotter.viewmodel.MainViewModel
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon

private val darkTileSource = XYTileSource(
    "CartoDB-Dark", 0, 19, 256, ".png",
    arrayOf(
        "https://a.basemaps.cartocdn.com/dark_all/",
        "https://b.basemaps.cartocdn.com/dark_all/",
        "https://c.basemaps.cartocdn.com/dark_all/"
    )
)

@Composable
fun MapScreen(viewModel: MainViewModel) {
    val flights by viewModel.flights.collectAsState()
    val location by viewModel.location.collectAsState()
    val selectedFlight by viewModel.selectedFlight.collectAsState()
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(darkTileSource)
                    setMultiTouchControls(true)
                    controller.setZoom(10.0)
                    // Dim the map slightly for HUD effect
                    overlayManager.tilesOverlay.setColorFilter(
                        ColorMatrixColorFilter(ColorMatrix().apply {
                            setScale(0.8f, 0.9f, 0.8f, 1f)
                        })
                    )
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { mapView ->
                // Clear old markers (keep tiles overlay)
                mapView.overlays.removeAll { it is Marker || it is Polygon }

                location?.let { loc ->
                    val userPoint = GeoPoint(loc.latitude, loc.longitude)

                    // Center on first load
                    if (mapView.mapCenter.latitude == 0.0) {
                        mapView.controller.setCenter(userPoint)
                    }

                    // 50km radius circle
                    val circle = Polygon(mapView).apply {
                        points = Polygon.pointsAsCircle(userPoint, 50000.0)
                        outlinePaint.apply {
                            color = 0x6600FF9D.toInt()
                            strokeWidth = 2f
                            style = Paint.Style.STROKE
                        }
                        fillPaint.color = 0x1A00FF9D.toInt()
                    }
                    mapView.overlays.add(circle)

                    // User marker
                    val userMarker = Marker(mapView).apply {
                        position = userPoint
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        title = "You"
                        icon = createDotDrawable(context, 0xFF00FF9D.toInt(), 16)
                    }
                    mapView.overlays.add(userMarker)
                }

                // Flight markers
                flights.forEach { flight ->
                    val markerColor = when {
                        flight.dist < 15 -> 0xFFFF3C3C.toInt()
                        flight.dist > 50 -> 0xFF00FF9D.toInt()
                        else -> 0xFFFFB800.toInt()
                    }

                    val marker = Marker(mapView).apply {
                        position = GeoPoint(flight.lat, flight.lon)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        title = flight.callsign
                        snippet = "${formatAlt(flight.altM)} | ${flight.dist.toInt()}km | HDG ${flight.hdg.toInt()}°"
                        icon = createPlaneDrawable(context, flight.hdg, markerColor)
                        setOnMarkerClickListener { _, _ ->
                            viewModel.selectFlight(flight)
                            true
                        }
                    }
                    mapView.overlays.add(marker)
                }

                mapView.invalidate()
            }
        )

        // Top gradient overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .align(Alignment.TopStart)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(HudColors.bg, androidx.compose.ui.graphics.Color.Transparent)
                    )
                )
        ) {
            Text(
                text = "MAP VIEW",
                style = HudTypography.title,
                modifier = Modifier.padding(16.dp)
            )

            Text(
                text = "FLT ${flights.size}",
                style = HudTypography.mono,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            )
        }

        // Bottom info
        if (selectedFlight != null) {
            val sf = selectedFlight!!
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart)
                    .background(HudColors.bg.copy(alpha = 0.9f))
                    .padding(16.dp)
            ) {
                Column {
                    Text(sf.callsign, style = HudTypography.callsign)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${formatAlt(sf.altM)} alt · ${sf.dist.toInt()}km dist · ${(sf.velMs * 3.6).toInt()}km/h · HDG ${sf.hdg.toInt()}°",
                        style = HudTypography.flightInfo
                    )
                }
            }
        }
    }
}

private fun createDotDrawable(context: android.content.Context, color: Int, sizePx: Int): BitmapDrawable {
    val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
    canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f, paint)
    return BitmapDrawable(context.resources, bmp)
}

private fun createPlaneDrawable(context: android.content.Context, heading: Float, color: Int): BitmapDrawable {
    val size = 32
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    canvas.rotate(heading, size / 2f, size / 2f)

    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.FILL
    }

    // Simple plane shape pointing up
    val path = Path().apply {
        moveTo(size / 2f, 4f)           // nose
        lineTo(size / 2f + 10f, size / 2f + 4f)  // right wing
        lineTo(size / 2f, size / 2f - 2f)
        lineTo(size / 2f - 10f, size / 2f + 4f)  // left wing
        close()
        // Tail
        moveTo(size / 2f - 5f, size - 8f)
        lineTo(size / 2f + 5f, size - 8f)
        lineTo(size / 2f, size - 12f)
        close()
    }
    // Fuselage
    canvas.drawRect(size / 2f - 2f, 6f, size / 2f + 2f, size - 6f, paint)
    canvas.drawPath(path, paint)

    return BitmapDrawable(context.resources, bmp)
}

private fun formatAlt(altM: Double): String {
    return if (altM >= 1000) "${"%.1f".format(altM / 1000)}km" else "${altM.toInt()}m"
}

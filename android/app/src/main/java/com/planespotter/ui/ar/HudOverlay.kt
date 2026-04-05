package com.planespotter.ui.ar

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.planespotter.data.model.Flight
import com.planespotter.domain.GeoUtils
import com.planespotter.domain.ProjectionUtils
import com.planespotter.sensor.LocationStatus
import com.planespotter.ui.theme.*
import com.planespotter.viewmodel.ConnectionStatus

@Composable
fun HudOverlay(
    azimuth: Float,
    elevation: Float,
    flights: List<Flight>,
    selectedFlight: Flight?,
    locationStatus: LocationStatus,
    connectionStatus: ConnectionStatus,
    onFlightSelected: (Flight) -> Unit,
    modifier: Modifier = Modifier
) {
    val config = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidthPx = with(density) { config.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { config.screenHeightDp.dp.toPx() }

    Box(modifier = modifier.fillMaxSize()) {
        // Flight labels
        flights.forEach { flight ->
            val proj = ProjectionUtils.projectToScreen(
                azimuth, elevation, flight.bearing, flight.elev,
                screenWidthPx, screenHeightPx
            )
            if (proj.visible) {
                val deltaAngle = GeoUtils.angleDiff(azimuth, flight.bearing)
                FlightLabel(
                    flight = flight,
                    isSelected = selectedFlight?.icao == flight.icao,
                    deltaAngle = deltaAngle,
                    onClick = { onFlightSelected(flight) },
                    modifier = Modifier.offset { IntOffset(proj.x.toInt(), proj.y.toInt()) }
                )
            }
        }

        // Crosshair
        Crosshair(Modifier.align(Alignment.Center))

        // Compass tape
        CompassTape(
            azimuth = azimuth,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 56.dp)
        )

        // Azimuth readout
        Text(
            text = "AZ ${azimuth.toInt()}° | EL ${elevation.toInt()}°",
            style = HudTypography.mono.copy(fontSize = 11.sp),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 88.dp)
        )

        // Horizon line
        HorizonLine(elevation = elevation, screenHeightPx = screenHeightPx)

        // Elevation scale
        ElevationScale(
            Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 8.dp)
        )

        // HUD top bar
        HudTopBar(
            azimuth = azimuth,
            flightCount = flights.size,
            locationStatus = locationStatus,
            modifier = Modifier.align(Alignment.TopStart)
        )

        // HUD bottom bar
        HudBottomBar(
            flights = flights,
            modifier = Modifier.align(Alignment.BottomStart)
        )

        // Scanlines + vignette
        ScanlineOverlay(Modifier.fillMaxSize())
    }
}

@Composable
fun Crosshair(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(60.dp)) {
        val w = size.width
        val h = size.height
        val stroke = 1f
        val color = HudColors.green.copy(alpha = 0.7f)
        val cornerLen = 10.dp.toPx()
        val cornerStroke = 2f
        val cornerColor = HudColors.green.copy(alpha = 0.9f)

        // Center cross
        drawLine(color, Offset(w / 2, 0f), Offset(w / 2, h), strokeWidth = stroke)
        drawLine(color, Offset(0f, h / 2), Offset(w, h / 2), strokeWidth = stroke)

        // Corner brackets
        // Top-left
        drawLine(cornerColor, Offset(0f, 0f), Offset(cornerLen, 0f), strokeWidth = cornerStroke)
        drawLine(cornerColor, Offset(0f, 0f), Offset(0f, cornerLen), strokeWidth = cornerStroke)
        // Top-right
        drawLine(cornerColor, Offset(w, 0f), Offset(w - cornerLen, 0f), strokeWidth = cornerStroke)
        drawLine(cornerColor, Offset(w, 0f), Offset(w, cornerLen), strokeWidth = cornerStroke)
        // Bottom-left
        drawLine(cornerColor, Offset(0f, h), Offset(cornerLen, h), strokeWidth = cornerStroke)
        drawLine(cornerColor, Offset(0f, h), Offset(0f, h - cornerLen), strokeWidth = cornerStroke)
        // Bottom-right
        drawLine(cornerColor, Offset(w, h), Offset(w - cornerLen, h), strokeWidth = cornerStroke)
        drawLine(cornerColor, Offset(w, h), Offset(w, h - cornerLen), strokeWidth = cornerStroke)
    }
}

@Composable
fun HorizonLine(elevation: Float, screenHeightPx: Float) {
    val density = LocalDensity.current
    val pxPerDeg = screenHeightPx / ProjectionUtils.FOV_V
    val offsetY = elevation * pxPerDeg
    val offsetDp = with(density) { offsetY.toDp() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = offsetDp)
            .padding(start = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .align(Alignment.Center)
                .background(HudColors.green.copy(alpha = 0.25f))
        )
        Text(
            text = "HORIZON",
            style = HudTypography.monoSmall.copy(
                fontSize = 8.sp,
                letterSpacing = 2.sp,
                color = HudColors.green.copy(alpha = 0.4f)
            ),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 8.dp)
        )
    }
}

@Composable
fun ElevationScale(modifier: Modifier = Modifier) {
    val marks = listOf("+60°", "+45°", "+30°", "+15°", "0°", "-15°")
    Column(
        modifier = modifier.fillMaxHeight(0.6f),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        marks.forEach { label ->
            Text(
                text = label,
                style = HudTypography.monoSmall.copy(fontSize = 8.sp, color = HudColors.green.copy(alpha = 0.3f)),
                textAlign = TextAlign.End
            )
        }
    }
}

@Composable
fun HudTopBar(
    azimuth: Float,
    flightCount: Int,
    locationStatus: LocationStatus,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(HudColors.bg, Color.Transparent),
                    startY = 0f,
                    endY = 200f
                )
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = "FLIGHT AR",
            style = HudTypography.title,
            modifier = Modifier.align(Alignment.TopStart)
        )

        val locText = when (locationStatus) {
            LocationStatus.OK -> "OK"
            LocationStatus.DEMO -> "DEMO"
            LocationStatus.ERROR -> "ERR"
            LocationStatus.WAITING -> "--"
        }

        Column(
            modifier = Modifier.align(Alignment.TopEnd),
            horizontalAlignment = Alignment.End
        ) {
            Text("LOC $locText", style = HudTypography.mono)
            Text("HDG ${azimuth.toInt().toString().padStart(3, '0')}°", style = HudTypography.mono)
            Text("FLT $flightCount", style = HudTypography.mono)
        }
    }
}

@Composable
fun HudBottomBar(
    flights: List<Flight>,
    modifier: Modifier = Modifier
) {
    val closest = flights.firstOrNull()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color.Transparent, HudColors.bg.copy(alpha = 0.75f)),
                    startY = 0f,
                    endY = 150f
                )
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Visible flights
            Column {
                Text("VIDLJIVI LETOVI", style = HudTypography.metricLabel)
                Text("${flights.size}", style = HudTypography.metricValue)
            }
            // Closest
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("NAJBLIŽI", style = HudTypography.metricLabel)
                Text(
                    closest?.callsign?.take(7) ?: "---",
                    style = HudTypography.metricValue
                )
            }
            // Radius
            Column(horizontalAlignment = Alignment.End) {
                Text("RADIUS", style = HudTypography.metricLabel)
                Text("50km", style = HudTypography.metricValue)
            }
        }
    }
}

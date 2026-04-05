package com.planespotter.ui.ar

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@Composable
fun ScanlineOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Scanlines (horizontal lines every 2px)
        val scanlineColor = Color.Black.copy(alpha = 0.03f)
        var y = 0f
        while (y < h) {
            drawLine(scanlineColor, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
            y += 2f
        }

        // Vignette (radial gradient from transparent center to dark edges)
        drawRect(
            Brush.radialGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.Transparent,
                    Color.Black.copy(alpha = 0.4f),
                    Color.Black.copy(alpha = 0.8f)
                ),
                center = Offset(w / 2, h / 2),
                radius = maxOf(w, h) * 0.7f
            )
        )
    }
}

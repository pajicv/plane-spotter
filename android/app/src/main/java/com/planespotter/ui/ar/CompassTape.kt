package com.planespotter.ui.ar

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.planespotter.ui.theme.HudColors

private val cardinals = mapOf(
    0 to "N", 45 to "NE", 90 to "E", 135 to "SE",
    180 to "S", 225 to "SW", 270 to "W", 315 to "NW"
)

@Composable
fun CompassTape(azimuth: Float, modifier: Modifier = Modifier) {
    val tapeWidthDp = 280.dp
    val tapeHeightDp = 28.dp

    Canvas(modifier = modifier.width(tapeWidthDp).height(tapeHeightDp)) {
        val pxPerDeg = size.width / 60f // show 60° of compass
        val centerX = size.width / 2f
        val paint = android.graphics.Paint().apply {
            color = HudColors.green.hashCode()
            textSize = 10.dp.toPx()
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.MONOSPACE
            isAntiAlias = true
        }

        // Draw compass marks for visible range
        val startDeg = (azimuth - 35).toInt()
        val endDeg = (azimuth + 35).toInt()

        for (deg in startDeg..endDeg) {
            val norm = ((deg % 360) + 360) % 360
            val offset = (deg - azimuth) * pxPerDeg
            val x = centerX + offset

            when {
                norm % 45 == 0 -> {
                    // Cardinal/intercardinal
                    paint.alpha = 255
                    paint.textSize = 11.dp.toPx()
                    paint.isFakeBoldText = true
                    drawContext.canvas.nativeCanvas.drawText(
                        cardinals[norm] ?: "$norm",
                        x, size.height * 0.7f, paint
                    )
                    paint.isFakeBoldText = false
                    drawLine(
                        HudColors.green, Offset(x, 0f), Offset(x, 6.dp.toPx()),
                        strokeWidth = 2f
                    )
                }
                norm % 15 == 0 -> {
                    // Minor tick
                    paint.alpha = 180
                    paint.textSize = 9.dp.toPx()
                    drawContext.canvas.nativeCanvas.drawText(
                        "$norm", x, size.height * 0.65f, paint
                    )
                    drawLine(
                        HudColors.green.copy(alpha = 0.5f),
                        Offset(x, 0f), Offset(x, 4.dp.toPx()),
                        strokeWidth = 1f
                    )
                }
                norm % 5 == 0 -> {
                    // Micro dot
                    drawCircle(
                        HudColors.green.copy(alpha = 0.3f),
                        radius = 1.dp.toPx(),
                        center = Offset(x, size.height * 0.3f)
                    )
                }
            }
        }

        // Center marker (triangle)
        val triSize = 6.dp.toPx()
        val path = android.graphics.Path().apply {
            moveTo(centerX - triSize, 0f)
            lineTo(centerX + triSize, 0f)
            lineTo(centerX, triSize)
            close()
        }
        val triPaint = android.graphics.Paint().apply {
            color = HudColors.green.hashCode()
            style = android.graphics.Paint.Style.FILL
            isAntiAlias = true
        }
        drawContext.canvas.nativeCanvas.drawPath(path, triPaint)
    }
}

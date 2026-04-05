package com.planespotter.ui.ar

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.planespotter.data.model.Flight
import com.planespotter.ui.theme.HudColors
import com.planespotter.ui.theme.HudTypography
import kotlin.math.abs

@Composable
fun FlightLabel(
    flight: Flight,
    isSelected: Boolean,
    deltaAngle: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dotColor = when {
        flight.dist < 15 -> HudColors.red
        flight.dist > 50 -> HudColors.green.copy(alpha = 0.7f)
        else -> HudColors.amber
    }

    val borderColor = if (isSelected) HudColors.amber else HudColors.green.copy(alpha = 0.4f)

    // Pulse animation for dot
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Column(
        modifier = modifier.clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Dot
        Box(
            modifier = Modifier
                .size(10.dp)
                .scale(pulseScale)
                .alpha(pulseAlpha)
                .background(dotColor, CircleShape)
        )

        // Card
        Column(
            modifier = Modifier
                .padding(top = 8.dp)
                .border(1.dp, borderColor)
                .background(Color(0xD9000A06))
                .padding(horizontal = 9.dp, vertical = 6.dp)
                .widthIn(min = 110.dp)
        ) {
            Text(
                text = flight.callsign,
                style = HudTypography.callsign
            )

            Spacer(Modifier.height(3.dp))

            Text(
                text = "${formatAlt(flight.altM)} alt · ${formatDist(flight.dist)} dist",
                style = HudTypography.flightInfo
            )
            Text(
                text = "${formatSpeed(flight.velMs)} · HDG ${flight.hdg.toInt()}°",
                style = HudTypography.flightInfo
            )
        }

        // Angular delta
        Spacer(Modifier.height(3.dp))
        val absDelta = abs(deltaAngle)
        if (absDelta < 5f) {
            Text(
                text = "◎ U VIDOKRUGU",
                style = HudTypography.monoSmall.copy(
                    fontSize = 9.sp,
                    color = HudColors.amber
                )
            )
        } else {
            Text(
                text = "${"%.1f".format(absDelta)}° od centra",
                style = HudTypography.monoSmall.copy(
                    fontSize = 9.sp,
                    color = Color.White.copy(alpha = 0.4f)
                )
            )
        }
    }
}

private fun formatAlt(altM: Double): String {
    return if (altM >= 1000) "${"%.1f".format(altM / 1000)}km" else "${altM.toInt()}m"
}

private fun formatDist(distKm: Double): String {
    return if (distKm >= 1) "${distKm.toInt()}km" else "${(distKm * 1000).toInt()}m"
}

private fun formatSpeed(velMs: Double): String {
    val kmh = (velMs * 3.6).toInt()
    return "${kmh}km/h"
}

package com.planespotter.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.planespotter.R

object HudColors {
    val green = Color(0xFF00FF9D)
    val amber = Color(0xFFFFB800)
    val red = Color(0xFFFF3C3C)
    val bg = Color(0xFF030A08)
    val dim = Color(0x2600FF9D)
    val greenDim = Color(0x9900FF9D)
    val greenFaint = Color(0x4D00FF9D)
}

val SpaceMono = FontFamily(
    Font(R.font.space_mono_regular, FontWeight.Normal),
    Font(R.font.space_mono_bold, FontWeight.Bold)
)

val Rajdhani = FontFamily(
    Font(R.font.rajdhani_light, FontWeight.Light),
    Font(R.font.rajdhani_medium, FontWeight.Medium),
    Font(R.font.rajdhani_bold, FontWeight.Bold)
)

object HudTypography {
    val title = TextStyle(
        fontFamily = Rajdhani,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        letterSpacing = 4.sp,
        color = HudColors.green
    )
    val mono = TextStyle(
        fontFamily = SpaceMono,
        fontWeight = FontWeight.Normal,
        fontSize = 9.sp,
        color = HudColors.green
    )
    val monoSmall = TextStyle(
        fontFamily = SpaceMono,
        fontWeight = FontWeight.Normal,
        fontSize = 8.sp,
        color = HudColors.greenDim
    )
    val metricLabel = TextStyle(
        fontFamily = SpaceMono,
        fontWeight = FontWeight.Normal,
        fontSize = 8.sp,
        letterSpacing = 1.sp,
        color = HudColors.greenDim
    )
    val metricValue = TextStyle(
        fontFamily = Rajdhani,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        color = HudColors.green
    )
    val callsign = TextStyle(
        fontFamily = Rajdhani,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        letterSpacing = 1.sp,
        color = HudColors.amber
    )
    val flightInfo = TextStyle(
        fontFamily = SpaceMono,
        fontWeight = FontWeight.Normal,
        fontSize = 8.sp,
        color = HudColors.greenDim
    )
}

private val DarkColorScheme = darkColorScheme(
    primary = HudColors.green,
    secondary = HudColors.amber,
    background = HudColors.bg,
    surface = HudColors.bg,
    onPrimary = HudColors.bg,
    onBackground = HudColors.green,
    onSurface = HudColors.green
)

@Composable
fun HudTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}

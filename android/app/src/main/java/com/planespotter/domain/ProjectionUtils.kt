package com.planespotter.domain

data class ScreenProjection(
    val x: Float,
    val y: Float,
    val visible: Boolean
)

object ProjectionUtils {
    const val FOV_H = 60f
    const val FOV_V = 45f

    fun projectToScreen(
        userAzimuth: Float,
        userElevation: Float,
        flightBearing: Float,
        flightElevation: Float,
        screenWidth: Float,
        screenHeight: Float
    ): ScreenProjection {
        val dAz = GeoUtils.angleDiff(userAzimuth, flightBearing)
        val dEl = flightElevation - userElevation

        val pxPerDegH = screenWidth / FOV_H
        val pxPerDegV = screenHeight / FOV_V

        val x = screenWidth / 2 + dAz * pxPerDegH
        val y = screenHeight / 2 - dEl * pxPerDegV

        val margin = 200f
        val visible = x > -margin && x < screenWidth + margin &&
                      y > -margin && y < screenHeight + margin

        return ScreenProjection(x, y, visible)
    }
}

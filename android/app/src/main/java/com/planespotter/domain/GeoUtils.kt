package com.planespotter.domain

import kotlin.math.*

object GeoUtils {
    private const val R = 6371.0 // Earth radius km

    fun bearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val φ1 = Math.toRadians(lat1)
        val φ2 = Math.toRadians(lat2)
        val Δλ = Math.toRadians(lon2 - lon1)
        val y = sin(Δλ) * cos(φ2)
        val x = cos(φ1) * sin(φ2) - sin(φ1) * cos(φ2) * cos(Δλ)
        return ((Math.toDegrees(atan2(y, x)) + 360) % 360).toFloat()
    }

    fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return R * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    fun elevationAngle(myLat: Double, myLon: Double, acLat: Double, acLon: Double, acAltM: Double): Float {
        val dist = distanceKm(myLat, myLon, acLat, acLon) * 1000 // meters
        if (dist < 1) return 90f
        return Math.toDegrees(atan2(acAltM, dist)).toFloat()
    }

    fun angleDiff(a: Float, b: Float): Float {
        return ((b - a + 540) % 360) - 180
    }

    fun smoothAngle(current: Float, target: Float, factor: Float): Float {
        val diff = angleDiff(current, target)
        return (current + diff * factor + 360) % 360
    }
}

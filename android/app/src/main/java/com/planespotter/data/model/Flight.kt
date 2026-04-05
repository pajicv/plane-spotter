package com.planespotter.data.model

data class Flight(
    val icao: String,
    val callsign: String,
    val lat: Double,
    val lon: Double,
    val altM: Double,      // altitude in meters
    val velMs: Double,     // velocity in m/s
    val hdg: Float,        // heading degrees
    val dist: Double,      // distance from user in km
    val bearing: Float,    // compass bearing to flight
    val elev: Float,       // elevation angle degrees
    val age: Double,       // seconds since last update
    val isDemo: Boolean = false
)

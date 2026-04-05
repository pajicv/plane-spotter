package com.planespotter.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AdsbResponse(
    val ac: List<AircraftDto>? = null
)

@Serializable
data class AircraftDto(
    val hex: String = "",
    val flight: String? = null,
    val lat: Double? = null,
    val lon: Double? = null,
    val alt_geom: Int? = null,
    val alt_baro: kotlinx.serialization.json.JsonElement? = null,
    val gs: Double? = null,
    val track: Float? = null,
    val true_heading: Float? = null,
    val seen: Double? = null,
    val dst: Double? = null,
    val dir: Float? = null,
    val r: String? = null,       // registration
    val t: String? = null,       // aircraft type
) {
    val altitudeFeet: Int?
        get() = alt_geom ?: try {
            alt_baro?.let {
                kotlinx.serialization.json.Json.decodeFromJsonElement(
                    kotlinx.serialization.builtins.serializer<Int>(), it
                )
            }
        } catch (_: Exception) { null }
}

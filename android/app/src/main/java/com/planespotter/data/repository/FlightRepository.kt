package com.planespotter.data.repository

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.planespotter.data.model.Flight
import com.planespotter.data.remote.AdsbApi
import com.planespotter.domain.GeoUtils
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

class FlightRepository {

    private val json = Json { ignoreUnknownKeys = true }

    private val api: AdsbApi = Retrofit.Builder()
        .baseUrl(AdsbApi.BASE_URL)
        .client(
            OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build()
        )
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(AdsbApi::class.java)

    suspend fun fetchFlights(userLat: Double, userLon: Double, radiusKm: Int): List<Flight> {
        val response = api.getFlights(userLat, userLon, radiusKm)
        return response.ac
            ?.filter { it.lat != null && it.lon != null && (it.altitudeFeet ?: 0) > 100 }
            ?.map { dto ->
                val altM = (dto.altitudeFeet ?: 0) * 0.3048
                val lat = dto.lat!!
                val lon = dto.lon!!
                val dist = if (dto.dst != null) dto.dst * 1.852 else GeoUtils.distanceKm(userLat, userLon, lat, lon)
                val bearing = dto.dir ?: GeoUtils.bearing(userLat, userLon, lat, lon)

                Flight(
                    icao = dto.hex,
                    callsign = dto.flight?.trim()?.ifEmpty { null } ?: dto.hex,
                    lat = lat,
                    lon = lon,
                    altM = altM,
                    velMs = (dto.gs ?: 0.0) * 0.514444,
                    hdg = dto.track ?: dto.true_heading ?: 0f,
                    dist = dist,
                    bearing = bearing,
                    elev = GeoUtils.elevationAngle(userLat, userLon, lat, lon, altM),
                    age = dto.seen ?: 0.0
                )
            }
            ?.sortedBy { it.dist }
            ?: emptyList()
    }

    fun getDemoFlights(userLat: Double, userLon: Double): List<Flight> {
        data class Demo(val cs: String, val dLat: Double, val dLon: Double, val alt: Double, val hdg: Float)
        val demos = listOf(
            Demo("DAN123", 0.3, 0.5, 9500.0, 270f),
            Demo("AFR442", -0.1, 0.8, 11000.0, 90f),
            Demo("TUR71", 0.6, -0.2, 7500.0, 180f),
        )
        return demos.map { d ->
            val lat = userLat + d.dLat
            val lon = userLon + d.dLon
            Flight(
                icao = d.cs.lowercase(),
                callsign = d.cs,
                lat = lat, lon = lon,
                altM = d.alt, velMs = 230.0, hdg = d.hdg,
                dist = GeoUtils.distanceKm(userLat, userLon, lat, lon),
                bearing = GeoUtils.bearing(userLat, userLon, lat, lon),
                elev = GeoUtils.elevationAngle(userLat, userLon, lat, lon, d.alt),
                age = 2.0,
                isDemo = true
            )
        }
    }
}

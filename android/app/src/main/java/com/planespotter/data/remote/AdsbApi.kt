package com.planespotter.data.remote

import retrofit2.http.GET
import retrofit2.http.Path

interface AdsbApi {
    @GET("v2/lat/{lat}/lon/{lon}/dist/{dist}")
    suspend fun getFlights(
        @Path("lat") lat: Double,
        @Path("lon") lon: Double,
        @Path("dist") dist: Int
    ): AdsbResponse

    companion object {
        const val BASE_URL = "https://api.adsb.lol/"
    }
}

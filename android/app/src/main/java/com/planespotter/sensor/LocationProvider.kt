package com.planespotter.sensor

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LocationProvider(context: Context) {

    private val client = LocationServices.getFusedLocationProviderClient(context)

    private val _location = MutableStateFlow<Location?>(null)
    val location: StateFlow<Location?> = _location.asStateFlow()

    private val _status = MutableStateFlow(LocationStatus.WAITING)
    val status: StateFlow<LocationStatus> = _status.asStateFlow()

    private val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let {
                _location.value = it
                _status.value = LocationStatus.OK
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun start() {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .setMinUpdateIntervalMillis(2000)
            .build()
        client.requestLocationUpdates(request, callback, null)

        // Try to get last known location immediately
        client.lastLocation.addOnSuccessListener { loc ->
            if (loc != null && _location.value == null) {
                _location.value = loc
                _status.value = LocationStatus.OK
            }
        }
    }

    fun stop() {
        client.removeLocationUpdates(callback)
    }

    fun useDemoLocation() {
        _location.value = Location("demo").apply {
            latitude = 44.787
            longitude = 20.457
        }
        _status.value = LocationStatus.DEMO
    }
}

enum class LocationStatus { WAITING, OK, DEMO, ERROR }

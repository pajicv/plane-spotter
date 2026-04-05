package com.planespotter.viewmodel

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.planespotter.data.model.Flight
import com.planespotter.data.repository.FlightRepository
import com.planespotter.sensor.LocationProvider
import com.planespotter.sensor.LocationStatus
import com.planespotter.sensor.OrientationData
import com.planespotter.sensor.OrientationProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        const val SEARCH_RADIUS_KM = 50
        const val FETCH_INTERVAL_MS = 10_000L
    }

    private val repository = FlightRepository()
    val orientationProvider = OrientationProvider(application)
    val locationProvider = LocationProvider(application)

    val orientation: StateFlow<OrientationData> = orientationProvider.orientation
    val location: StateFlow<Location?> = locationProvider.location
    val locationStatus: StateFlow<LocationStatus> = locationProvider.status

    private val _flights = MutableStateFlow<List<Flight>>(emptyList())
    val flights: StateFlow<List<Flight>> = _flights.asStateFlow()

    private val _selectedFlight = MutableStateFlow<Flight?>(null)
    val selectedFlight: StateFlow<Flight?> = _selectedFlight.asStateFlow()

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.LOADING)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private var fetchJob: Job? = null

    fun startSensors() {
        orientationProvider.start()
        locationProvider.start()
        startFetchLoop()
    }

    fun stopSensors() {
        orientationProvider.stop()
        locationProvider.stop()
        fetchJob?.cancel()
    }

    private fun startFetchLoop() {
        fetchJob?.cancel()
        fetchJob = viewModelScope.launch {
            // Wait for location
            location.filterNotNull().first()
            while (true) {
                fetchFlights()
                delay(FETCH_INTERVAL_MS)
            }
        }
    }

    private suspend fun fetchFlights() {
        val loc = location.value ?: return
        try {
            val result = repository.fetchFlights(loc.latitude, loc.longitude, SEARCH_RADIUS_KM)
            _flights.value = result
            _connectionStatus.value = ConnectionStatus.OK
        } catch (e: Exception) {
            if (_flights.value.isEmpty()) {
                _flights.value = repository.getDemoFlights(loc.latitude, loc.longitude)
                _connectionStatus.value = ConnectionStatus.DEMO
            } else {
                _connectionStatus.value = ConnectionStatus.ERROR
            }
        }
    }

    fun selectFlight(flight: Flight?) {
        _selectedFlight.value = if (_selectedFlight.value?.icao == flight?.icao) null else flight
    }

    override fun onCleared() {
        stopSensors()
    }
}

enum class ConnectionStatus { LOADING, OK, DEMO, ERROR }

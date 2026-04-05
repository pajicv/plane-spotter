package com.planespotter.ui.ar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import com.planespotter.viewmodel.MainViewModel

@Composable
fun ArScreen(viewModel: MainViewModel) {
    val orientation by viewModel.orientation.collectAsState()
    val flights by viewModel.flights.collectAsState()
    val selectedFlight by viewModel.selectedFlight.collectAsState()
    val locationStatus by viewModel.locationStatus.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        // Layer 1: Camera
        CameraPreview(
            modifier = Modifier.fillMaxSize().alpha(0.85f)
        )

        // Layer 2: HUD overlay
        HudOverlay(
            azimuth = orientation.azimuth,
            elevation = orientation.elevation,
            flights = flights,
            selectedFlight = selectedFlight,
            locationStatus = locationStatus,
            connectionStatus = connectionStatus,
            onFlightSelected = { viewModel.selectFlight(it) }
        )
    }
}

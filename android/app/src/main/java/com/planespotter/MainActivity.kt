package com.planespotter

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.planespotter.ui.navigation.MainScreen
import com.planespotter.ui.theme.HudColors
import com.planespotter.ui.theme.HudTheme
import com.planespotter.ui.theme.HudTypography
import com.planespotter.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            HudTheme {
                val viewModel: MainViewModel = viewModel()
                var permissionsGranted by remember { mutableStateOf(false) }

                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { permissions ->
                    permissionsGranted = permissions.values.all { it }
                    if (permissionsGranted) {
                        viewModel.startSensors()
                    } else {
                        // Start with demo location if location denied
                        viewModel.locationProvider.useDemoLocation()
                        viewModel.startSensors()
                        permissionsGranted = true
                    }
                }

                if (!permissionsGranted) {
                    PermissionScreen(
                        onStart = {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.CAMERA,
                                    Manifest.permission.ACCESS_FINE_LOCATION
                                )
                            )
                        }
                    )
                } else {
                    MainScreen(viewModel)
                }
            }
        }
    }
}

@Composable
private fun PermissionScreen(onStart: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(HudColors.bg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "✈",
                fontSize = 48.sp
            )

            Text(
                text = "FLIGHT AR",
                style = HudTypography.title.copy(fontSize = 24.sp, letterSpacing = 4.sp)
            )

            Text(
                text = "Usmeri kameru ka nebu i identifikuj letove u realnom vremenu.\n\nPotreban pristup kameri, lokaciji i senzorima orijentacije.",
                style = HudTypography.monoSmall.copy(fontSize = 11.sp),
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 280.dp)
            )

            androidx.compose.material3.OutlinedButton(
                onClick = onStart,
                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                    contentColor = HudColors.green
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, HudColors.green),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text(
                    text = "POKRENI",
                    style = HudTypography.mono.copy(fontSize = 12.sp, letterSpacing = 3.sp)
                )
            }
        }
    }
}

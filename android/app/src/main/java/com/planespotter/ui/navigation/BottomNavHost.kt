package com.planespotter.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.planespotter.ui.ar.ArScreen
import com.planespotter.ui.map.MapScreen
import com.planespotter.ui.theme.HudColors
import com.planespotter.ui.theme.SpaceMono
import com.planespotter.viewmodel.MainViewModel

enum class Screen(val route: String, val label: String, val icon: ImageVector) {
    AR("ar", "AR", Icons.Default.CameraAlt),
    MAP("map", "MAP", Icons.Default.Map)
}

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val currentEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentEntry?.destination?.route

    Scaffold(
        containerColor = HudColors.bg,
        bottomBar = {
            NavigationBar(
                containerColor = HudColors.bg,
                contentColor = HudColors.green
            ) {
                Screen.entries.forEach { screen ->
                    NavigationBarItem(
                        selected = currentRoute == screen.route,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(screen.icon, contentDescription = screen.label) },
                        label = {
                            Text(
                                screen.label,
                                fontFamily = SpaceMono,
                                letterSpacing = 3.sp,
                                fontSize = 10.sp
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = HudColors.green,
                            selectedTextColor = HudColors.green,
                            unselectedIconColor = HudColors.green.copy(alpha = 0.4f),
                            unselectedTextColor = HudColors.green.copy(alpha = 0.4f),
                            indicatorColor = HudColors.green.copy(alpha = 0.1f)
                        )
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.AR.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Screen.AR.route) { ArScreen(viewModel) }
            composable(Screen.MAP.route) { MapScreen(viewModel) }
        }
    }
}

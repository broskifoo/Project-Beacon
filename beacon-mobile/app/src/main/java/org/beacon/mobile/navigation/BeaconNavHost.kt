package org.beacon.mobile.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.beacon.mobile.ui.screens.HomeScreen
import org.beacon.mobile.ui.screens.MapScreen
import org.beacon.mobile.ui.screens.MessagesScreen
import org.beacon.mobile.ui.screens.NetworkScreen
import org.beacon.mobile.ui.screens.ResourcesScreen
import org.beacon.mobile.ui.screens.AlertsScreen
import org.beacon.mobile.ui.screens.SettingsScreen
import org.beacon.mobile.ui.screens.SosScreen
import org.beacon.mobile.viewmodel.MainViewModel

@Composable
fun BeaconNavHost(viewModel: MainViewModel) {
    val navController = rememberNavController()
    NavHost(navController, startDestination = "home") {
        composable("home") {
            HomeScreen(onNavigate = { route ->
                navController.navigate(route)
            })
        }
        composable("map") {
            MapScreen(viewModel = viewModel)
        }
        composable("messages") {
            MessagesScreen(viewModel = viewModel)
        }
        composable("network") {
            NetworkScreen(viewModel = viewModel)
        }
        composable("resources") {
            ResourcesScreen(viewModel = viewModel)
        }
        composable("alerts") {
            AlertsScreen(viewModel = viewModel)
        }
        composable("settings") {
            SettingsScreen(viewModel = viewModel)
        }
        composable("sos") {
            SosScreen(viewModel = viewModel)
        }
    }
}
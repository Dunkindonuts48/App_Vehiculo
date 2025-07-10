package com.example.autocare.navigation

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.autocare.HomeScreen
import com.example.autocare.sensor.TrackingScreen
import com.example.autocare.sensor.TrackingScreenTest2
import com.example.autocare.sensor.DrivingSessionListScreen
import com.example.autocare.vehicle.*

@Composable
fun AppNavigation(viewModel: VehicleViewModel) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "list") {
        composable("home") { HomeScreen(viewModel) }
        composable("list") { VehicleListScreen(navController, viewModel) }
        composable("form") { VehicleFormScreen(navController, viewModel, null) }
        composable("form/{vehicleId}") { backStackEntry ->
            val vehicleId = backStackEntry.arguments?.getString("vehicleId")?.toIntOrNull()
            if (vehicleId != null) {
                VehicleFormScreen(navController, viewModel, vehicleId)
            }
        }
        composable("detail/{vehicleId}") { backStackEntry ->
            val vehicleId = backStackEntry.arguments?.getString("vehicleId")?.toIntOrNull()
            if (vehicleId != null) {
                VehicleDetailScreen(vehicleId, viewModel, navController)
            }
        }
        composable("maintenance/{vehicleId}") { backStackEntry ->
            val vehicleId = backStackEntry.arguments?.getString("vehicleId")?.toIntOrNull()
            if (vehicleId != null) {
                MaintenanceListScreen(vehicleId, viewModel, navController)
            }
        }
        composable("tracking/{vehicleId}") { backStackEntry ->
            val vehicleId = backStackEntry.arguments?.getString("vehicleId")?.toIntOrNull()
            if (vehicleId != null) {
                TrackingScreen(vehicleId, navController)
            }
        }

        composable("trackingHz/{vehicleId}") { backStackEntry ->
            val vehicleId = backStackEntry.arguments?.getString("vehicleId")?.toIntOrNull()
            if (vehicleId != null) {
                TrackingScreenTest2(vehicleId, navController)
            }
        }

        composable("sessions/{vehicleId}") { backStackEntry ->
            val vehicleId = backStackEntry.arguments?.getString("vehicleId")?.toIntOrNull()
            if (vehicleId != null) {
                DrivingSessionListScreen(vehicleId, viewModel, navController)
            }
        }
    }
}
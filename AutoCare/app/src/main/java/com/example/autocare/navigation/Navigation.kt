package com.example.autocare.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.autocare.HomeScreen
import com.example.autocare.sensor.TrackingScreen
import com.example.autocare.sensor.DrivingSessionListScreen
import com.example.autocare.vehicle.*
import com.example.autocare.vehicle.detail.VehicleDetailScreen
import com.example.autocare.vehicle.form.VehicleFormScreen
import com.example.autocare.vehicle.fuel.FuelAddScreen
import com.example.autocare.vehicle.fuel.FuelListScreen
import com.example.autocare.vehicle.list.VehicleListScreen
import com.example.autocare.vehicle.maintenance.MaintenanceListScreen
import com.example.autocare.vehicle.registration.steps.StepRegisterAliasScreen
import com.example.autocare.vehicle.registration.steps.StepRegisterBrandScreen
import com.example.autocare.vehicle.registration.steps.StepRegisterMileageScreen
import com.example.autocare.vehicle.registration.steps.StepRegisterPlateScreen
import com.example.autocare.vehicle.registration.steps.StepRegisterRevisionScreen
import com.example.autocare.vehicle.registration.steps.StepRegisterTypeScreen
import com.example.autocare.bluetooth.BluetoothScreen


@Composable
fun AppNavigation(mainVm: VehicleViewModel) {
    val navController = rememberNavController()
    val registrationVm: VehicleRegistrationViewModel = viewModel()

    NavHost(navController = navController, startDestination = "list") {
        composable("home") {
            HomeScreen(mainVm)
        }
        composable("list") {
            VehicleListScreen(navController, mainVm)
        }
        composable("register/type") {
            StepRegisterTypeScreen(registrationVm, navController)
        }
        composable("register/brand") {
            StepRegisterBrandScreen(registrationVm, navController)
        }
        composable("register/plate") {
            StepRegisterPlateScreen(registrationVm, navController)
        }
        composable("register/mileage") {
            StepRegisterMileageScreen(registrationVm, navController)
        }
        composable("register/revision") {
            StepRegisterRevisionScreen(registrationVm, navController)
        }
        composable("register/alias") {
            StepRegisterAliasScreen(registrationVm, mainVm, navController)
        }
        composable("form") {
            VehicleFormScreen(navController, mainVm, null)
        }
        composable(
            "form/{vehicleId}",
            arguments = listOf(navArgument("vehicleId") { type = NavType.IntType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getInt("vehicleId") ?: return@composable
            VehicleFormScreen(navController, mainVm, id)
        }
        composable(
            "detail/{vehicleId}",
            arguments = listOf(navArgument("vehicleId") { type = NavType.IntType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getInt("vehicleId") ?: return@composable
            VehicleDetailScreen(id, mainVm, navController)
        }

        composable(
            "maintenance/{vehicleId}",
            arguments = listOf(navArgument("vehicleId") { type = NavType.IntType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getInt("vehicleId") ?: return@composable
            MaintenanceListScreen(id, mainVm, navController)
        }

        composable(
            route = "tracking/{vehicleId}",
            arguments = listOf(navArgument("vehicleId") { type = NavType.IntType }),
            deepLinks = listOf(
                navDeepLink { uriPattern = "autocare://tracking/{vehicleId}" }
            )
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getInt("vehicleId") ?: return@composable
            TrackingScreen(id, navController)
        }

        composable(
            "sessions/{vehicleId}",
            arguments = listOf(navArgument("vehicleId") { type = NavType.IntType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getInt("vehicleId") ?: return@composable
            DrivingSessionListScreen(id, mainVm, navController)
        }

        composable("fuel/{vehicleId}",
            arguments = listOf(navArgument("vehicleId") { type = NavType.IntType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments!!.getInt("vehicleId")
            FuelListScreen(id, mainVm, navController)
        }

        composable("fuel_add/{vehicleId}",
            arguments = listOf(navArgument("vehicleId") { type = NavType.IntType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments!!.getInt("vehicleId")
            FuelAddScreen(id, mainVm, navController)
        }

        composable(
            "bluetooth/{vehicleId}",
            arguments = listOf(navArgument("vehicleId") { type = NavType.IntType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments!!.getInt("vehicleId")
            BluetoothScreen(
                vehicleId = id,
                onDeviceSelected = { device ->
                    mainVm.saveBluetoothForVehicle(id, device.address)
                },
                navController = navController
            )
        }
    }
}
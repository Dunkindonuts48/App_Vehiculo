package com.example.autocare

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.autocare.navigation.AppNavigation
import com.example.autocare.ui.theme.AutoCareTheme
import com.example.autocare.vehicle.VehicleDatabase
import com.example.autocare.vehicle.VehicleViewModel
import com.example.autocare.vehicle.VehicleViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AutoCareTheme {
                val db = VehicleDatabase.getDatabase(applicationContext)
                val factory = VehicleViewModelFactory(
                    db.vehicleDao(),
                    db.maintenanceDao(),
                    db.drivingSessionDao()
                )
                val viewModel: VehicleViewModel = viewModel(factory = factory)
                AppNavigation(viewModel)
            }
        }
    }
}
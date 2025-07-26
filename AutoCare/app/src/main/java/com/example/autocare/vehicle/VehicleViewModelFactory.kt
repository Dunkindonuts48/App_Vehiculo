package com.example.autocare.vehicle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.autocare.sensor.DrivingSessionDao
import com.example.autocare.vehicle.fuel.FuelEntryDao
import com.example.autocare.vehicle.maintenance.MaintenanceDao

class VehicleViewModelFactory(
    private val vehicleDao: VehicleDao,
    private val maintenanceDao: MaintenanceDao,
    private val drivingSessionDao: DrivingSessionDao,
    private val fuelEntryDao: FuelEntryDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VehicleViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return VehicleViewModel(
                vehicleDao,
                maintenanceDao,
                drivingSessionDao,
                fuelEntryDao
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
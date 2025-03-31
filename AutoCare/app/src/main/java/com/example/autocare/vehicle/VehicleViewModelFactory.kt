package com.example.autocare.vehicle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.autocare.sensor.DrivingSessionDao

class VehicleViewModelFactory(
    private val dao: VehicleDao,
    private val maintenanceDao: MaintenanceDao,
    private val drivingSessionDao: DrivingSessionDao
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VehicleViewModel::class.java)) {
            return VehicleViewModel(dao, maintenanceDao, drivingSessionDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
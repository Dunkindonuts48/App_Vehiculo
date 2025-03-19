package com.example.car_app.viewmodel

import androidx.lifecycle.ViewModel
import com.example.car_app.data.entities.Vehicle
import com.example.car_app.data.repository.VehicleRepository

class VehicleViewModel(private val repository: VehicleRepository) : ViewModel() {
    fun getAllVehicles(): List<Vehicle> = repository.getAllVehicles()
    fun insertVehicle(vehicle: Vehicle) = repository.insertVehicle(vehicle)
}

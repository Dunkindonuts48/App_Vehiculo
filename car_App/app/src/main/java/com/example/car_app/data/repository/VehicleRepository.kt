package com.example.car_app.data.repository

import com.example.car_app.data.dao.VehicleDao
import com.example.car_app.data.entities.Vehicle

class VehicleRepository(private val vehicleDao: VehicleDao) {
    fun getAllVehicles(): List<Vehicle> = vehicleDao.getAllVehicles()
    fun insertVehicle(vehicle: Vehicle) = vehicleDao.insertVehicle(vehicle)
    fun deleteVehicle(vehicle: Vehicle) = vehicleDao.deleteVehicle(vehicle)
}

package com.example.autocare.util
import com.example.autocare.vehicle.Vehicle

fun getVehicleDisplayName(vehicle: Vehicle): String =
    vehicle.alias?.takeIf { it.isNotBlank() }
        ?: "${vehicle.brand} ${vehicle.model} (${vehicle.plateNumber})"
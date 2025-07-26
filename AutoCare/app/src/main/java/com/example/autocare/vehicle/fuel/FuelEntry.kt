package com.example.autocare.vehicle.fuel

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fuel_entries")
data class FuelEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val vehicleId: Int,
    val date: String,
    val mileage: Int,
    val liters: Float,
    val pricePerLiter: Float
)
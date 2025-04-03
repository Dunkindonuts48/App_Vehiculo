package com.example.autocare.vehicle

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vehicles")
data class Vehicle(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val brand: String,
    val model: String,
    val type: String,
    val plateNumber: String,
    val mileage: Int,
    val purchaseDate: String,
    val lastMaintenanceDate: String,
    val maintenanceFrequencyKm: Int,
    val maintenanceFrequencyMonths: Int,
    val alias: String? = null
)

package com.example.autocare.vehicle.maintenance

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "maintenances")
data class Maintenance(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val vehicleId: Int,
    val type: String,
    val date: String,
    val cost: Double,
    val  mileageAtMaintenance: Int
)
package com.example.autocare.vehicle.maintenance

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.autocare.vehicle.Vehicle
import java.time.LocalDate

@Entity(
    tableName = "maintenances",
    foreignKeys = [ForeignKey(entity = Vehicle::class, parentColumns = ["id"], childColumns = ["vehicleId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("vehicleId")]
)
data class Maintenance(@PrimaryKey(autoGenerate = true) val id: Int = 0,
    val vehicleId: Int,
    val type: String,
    val date: LocalDate,
    val cost: Double,
    val  mileageAtMaintenance: Int
)
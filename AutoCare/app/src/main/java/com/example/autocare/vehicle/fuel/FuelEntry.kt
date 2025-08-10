package com.example.autocare.vehicle.fuel

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.autocare.vehicle.Vehicle
import java.time.LocalDate

@Entity(
    tableName = "fuel_entries",
    foreignKeys = [
        ForeignKey(
            entity = Vehicle::class,
            parentColumns = ["id"],
            childColumns = ["vehicleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("vehicleId")]
)
data class FuelEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val vehicleId: Int,
    val date: LocalDate,
    val mileage: Int,
    val liters: Float,
    val pricePerLiter: Float
)
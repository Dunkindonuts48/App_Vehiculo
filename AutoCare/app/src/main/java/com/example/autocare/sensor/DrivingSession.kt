package com.example.autocare.sensor

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.autocare.vehicle.Vehicle

@Entity(
    tableName = "driving_sessions",
    foreignKeys = [
        ForeignKey(
            entity = Vehicle::class,
            parentColumns = ["id"],
            childColumns = ["vehicleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("vehicleId"), Index("endTime")]
)
data class DrivingSession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val vehicleId: Int,
    val startTime: Long,
    val endTime: Long,
    val maxSpeed: Float,
    val averageSpeed: Float,
    val accelerations: Int,
    val brakings: Int,
    val distanceMeters: Float
)

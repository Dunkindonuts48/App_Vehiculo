package com.example.autocare.sensor

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "driving_sessions")
data class DrivingSession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val vehicleId: Int,
    val startTime: Long,
    val endTime: Long,
    val maxSpeed: Float,
    val averageSpeed: Float,
    val accelerations: Int,
    val brakings: Int
)

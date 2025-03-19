package com.example.car_app.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vehicle")
data class Vehicle(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val brand: String,
    val model: String,
    val year: Int,
    val mileage: Int
)

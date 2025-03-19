package com.example.car_app.data.dao

import androidx.room.*
import androidx.room.Delete
import com.example.car_app.data.entities.Vehicle

@Dao
interface VehicleDao {
    @Query("SELECT * FROM vehicle")
    fun getAllVehicles(): List<Vehicle>

    @Query("SELECT * FROM vehicle WHERE brand = :brand AND model = :model AND year = :year LIMIT 1")
    fun getVehicle(brand: String, model: String, year: Int): Vehicle?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertVehicle(vehicle: Vehicle)

    @Delete
    fun deleteVehicle(vehicle: Vehicle)
}

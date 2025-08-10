package com.example.autocare.vehicle

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface VehicleDao {
    @Insert
    suspend fun insertVehicle(vehicle: Vehicle): Long
    @Update
    suspend fun updateVehicle(vehicle: Vehicle)
    @Delete
    suspend fun deleteVehicle(vehicle: Vehicle)
    @Query("SELECT * FROM vehicles")
    fun getAllVehicles(): Flow<List<Vehicle>>
    @Query("SELECT * FROM vehicles")
    suspend fun getAll(): List<Vehicle>
    @Query("SELECT * FROM vehicles WHERE id = :id LIMIT 1")
    fun getByIdSync(id: Int): Vehicle?

}
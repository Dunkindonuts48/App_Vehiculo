package com.example.autocare.vehicle.fuel

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FuelEntryDao {
    @Insert suspend fun insert(entry: FuelEntry)
    @Delete
    suspend fun delete(entry: FuelEntry)
    @Query("SELECT * FROM fuel_entries WHERE vehicleId=:vehicleId ORDER BY date DESC")
    fun getByVehicle(vehicleId: Int): Flow<List<FuelEntry>>
}


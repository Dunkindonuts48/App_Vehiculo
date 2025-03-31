package com.example.autocare.vehicle

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MaintenanceDao {
    @Insert
    suspend fun insert(maintenance: Maintenance)

    @Query("SELECT * FROM maintenances WHERE vehicleId = :vehicleId ORDER BY date DESC")
    fun getByVehicle(vehicleId: Int): Flow<List<Maintenance>>

    @Delete
    suspend fun delete(maintenance: Maintenance)
}

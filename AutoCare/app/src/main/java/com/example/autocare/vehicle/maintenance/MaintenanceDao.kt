package com.example.autocare.vehicle.maintenance

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
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
package com.example.autocare.sensor

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SensorDataDao {
    @Insert
    suspend fun insert(data: SensorData)

    @Query("SELECT * FROM sensor_data WHERE vehicleId = :vehicleId ORDER BY timestamp DESC")
    fun getByVehicle(vehicleId: Int): Flow<List<SensorData>>

    @Query("DELETE FROM sensor_data")
    suspend fun clearAll()
}

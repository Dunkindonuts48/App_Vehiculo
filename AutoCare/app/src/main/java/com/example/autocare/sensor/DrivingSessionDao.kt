package com.example.autocare.sensor

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DrivingSessionDao {
    @Insert
    suspend fun insert(session: DrivingSession)
    @Query("SELECT * FROM driving_sessions WHERE vehicleId = :vehicleId ORDER BY endTime DESC")
    fun getSessionsByVehicle(vehicleId: Int): Flow<List<DrivingSession>>
    @Query("DELETE FROM driving_sessions")
    suspend fun clearAll()
    @Delete
    suspend fun delete(session: DrivingSession)

}

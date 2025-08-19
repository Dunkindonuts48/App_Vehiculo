package com.example.autocare.util
import com.example.autocare.sensor.DrivingSessionDao
import com.example.autocare.sensor.aggrPer100km
import kotlinx.coroutines.flow.first

suspend fun avgAggressivenessLastSessions(
    dao: DrivingSessionDao,
    vehicleId: Int,
    lastN: Int = 10
): Float {
    val sessions = dao.getSessionsByVehicle(vehicleId).first()
        .sortedByDescending { it.endTime }
        .take(lastN)
    if (sessions.isEmpty()) return 0f
    return sessions.map { it.aggrPer100km() }.average().toFloat()
}
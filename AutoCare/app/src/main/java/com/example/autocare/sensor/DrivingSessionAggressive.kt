package com.example.autocare.sensor
fun DrivingSession.aggrPer100km(): Float {
    val km = (distanceMeters / 1000f).coerceAtLeast(0.1f)
    val events = accelerations + brakings
    return (events / km) * 100f
}
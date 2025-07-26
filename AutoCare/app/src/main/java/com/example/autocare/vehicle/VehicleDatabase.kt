package com.example.autocare.vehicle

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.autocare.sensor.DrivingSession
import com.example.autocare.sensor.DrivingSessionDao
import com.example.autocare.sensor.SensorData
import com.example.autocare.sensor.SensorDataDao
import com.example.autocare.vehicle.maintenance.Maintenance
import com.example.autocare.vehicle.maintenance.MaintenanceDao
import com.example.autocare.vehicle.fuel.FuelEntry
import com.example.autocare.vehicle.fuel.FuelEntryDao

@Database(entities = [Vehicle::class, Maintenance::class, SensorData::class, DrivingSession::class, FuelEntry::class], version = 6)
abstract class VehicleDatabase : RoomDatabase() {
    abstract fun vehicleDao(): VehicleDao
    abstract fun maintenanceDao(): MaintenanceDao
    abstract fun sensorDataDao(): SensorDataDao
    abstract fun drivingSessionDao(): DrivingSessionDao
    abstract fun fuelEntryDao(): FuelEntryDao

    companion object {
        @Volatile
        private var INSTANCE: VehicleDatabase? = null
        fun getDatabase(context: Context): VehicleDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VehicleDatabase::class.java,
                    "vehicle_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
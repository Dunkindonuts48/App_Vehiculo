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
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Vehicle::class, Maintenance::class, SensorData::class, DrivingSession::class, FuelEntry::class], version = 7)
abstract class VehicleDatabase : RoomDatabase() {
    abstract fun vehicleDao(): VehicleDao
    abstract fun maintenanceDao(): MaintenanceDao
    abstract fun sensorDataDao(): SensorDataDao
    abstract fun drivingSessionDao(): DrivingSessionDao
    abstract fun fuelEntryDao(): FuelEntryDao
    /*
    val migration_6_7 = object : Migration(6,7){
        override fun migrate(database: SupportSQLiteDatabase) {
        // NUEVAS TABLAS EN FUTURAS ENTIDADES
        // EJEMPLO PARA ENSEÑAR EN TFG: database.execSQL("ALTER TABLE vehicles ADD COLUMN color TEXT")
        }
    }
    */
    companion object {
        @Volatile
        private var INSTANCE: VehicleDatabase? = null

        fun getDatabase(context: Context): VehicleDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VehicleDatabase::class.java,
                    "vehicle_database"
                )
                    // Esto destruye y recrea la base de datos si cambia la versión
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
    /*
    companion object {
        @Volatile
        private var INSTANCE: VehicleDatabase? = null
        fun getDatabase(context: Context): VehicleDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                                context.applicationContext,
                                VehicleDatabase::class.java,
                                "vehicle_database"
                            ).fallbackToDestructiveMigrationOnDowngrade(false).build()
                            //FUTURO CAMBIO EN CASO DE CAMBIAR DE VERSIO: .addMigrations(migration_6_7).build()
                INSTANCE = instance
                instance
            }
        }
    }
     */
}
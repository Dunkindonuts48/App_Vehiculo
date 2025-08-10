package com.example.autocare.vehicle

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.autocare.sensor.DrivingSession
import com.example.autocare.sensor.DrivingSessionDao
import com.example.autocare.sensor.SensorData
import com.example.autocare.sensor.SensorDataDao
import com.example.autocare.vehicle.fuel.FuelEntry
import com.example.autocare.vehicle.fuel.FuelEntryDao
import com.example.autocare.vehicle.maintenance.Maintenance
import com.example.autocare.vehicle.maintenance.MaintenanceDao
import com.example.autocare.db.Converters

@Database(
    entities = [
        Vehicle::class,
        Maintenance::class,
        FuelEntry::class,
        SensorData::class,
        DrivingSession::class
    ],
    version = 9,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class VehicleDatabase : RoomDatabase() {
    abstract fun vehicleDao(): VehicleDao
    abstract fun maintenanceDao(): MaintenanceDao
    abstract fun sensorDataDao(): SensorDataDao
    abstract fun drivingSessionDao(): DrivingSessionDao
    abstract fun fuelEntryDao(): FuelEntryDao
    companion object {
        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS maintenances_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        vehicleId INTEGER NOT NULL,
                        type TEXT NOT NULL,
                        date TEXT NOT NULL,
                        cost REAL NOT NULL,
                        mileageAtMaintenance INTEGER NOT NULL,
                        FOREIGN KEY(vehicleId) REFERENCES vehicles(id) ON DELETE CASCADE
                    );
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_maintenances_vehicleId ON maintenances_new(vehicleId);")
                db.execSQL("""
                    INSERT INTO maintenances_new(id, vehicleId, type, date, cost, mileageAtMaintenance)
                    SELECT id, vehicleId, type, date, cost, mileageAtMaintenance FROM maintenances;
                """.trimIndent())
                db.execSQL("DROP TABLE maintenances;")
                db.execSQL("ALTER TABLE maintenances_new RENAME TO maintenances;")

                // FUEL ENTRIES
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS fuel_entries_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        vehicleId INTEGER NOT NULL,
                        date TEXT NOT NULL,
                        mileage INTEGER NOT NULL,
                        liters REAL NOT NULL,
                        pricePerLiter REAL NOT NULL,
                        FOREIGN KEY(vehicleId) REFERENCES vehicles(id) ON DELETE CASCADE
                    );
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_fuel_entries_vehicleId ON fuel_entries_new(vehicleId);")
                db.execSQL("""
                    INSERT INTO fuel_entries_new(id, vehicleId, date, mileage, liters, pricePerLiter)
                    SELECT id, vehicleId, date, mileage, liters, pricePerLiter FROM fuel_entries;
                """.trimIndent())
                db.execSQL("DROP TABLE fuel_entries;")
                db.execSQL("ALTER TABLE fuel_entries_new RENAME TO fuel_entries;")

                // SENSOR DATA
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS sensor_data_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        vehicleId INTEGER NOT NULL,
                        timestamp INTEGER NOT NULL,
                        speed REAL NOT NULL,
                        accelX REAL NOT NULL,
                        accelY REAL NOT NULL,
                        accelZ REAL NOT NULL,
                        gyroX REAL NOT NULL,
                        gyroY REAL NOT NULL,
                        gyroZ REAL NOT NULL,
                        FOREIGN KEY(vehicleId) REFERENCES vehicles(id) ON DELETE CASCADE
                    );
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_sensor_data_vehicleId ON sensor_data_new(vehicleId);")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_sensor_data_timestamp ON sensor_data_new(timestamp);")
                db.execSQL("""
                    INSERT INTO sensor_data_new(id, vehicleId, timestamp, speed, accelX, accelY, accelZ, gyroX, gyroY, gyroZ)
                    SELECT id, vehicleId, timestamp, speed, accelX, accelY, accelZ, gyroX, gyroY, gyroZ FROM sensor_data;
                """.trimIndent())
                db.execSQL("DROP TABLE sensor_data;")
                db.execSQL("ALTER TABLE sensor_data_new RENAME TO sensor_data;")

                // DRIVING SESSIONS
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS driving_sessions_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        vehicleId INTEGER NOT NULL,
                        startTime INTEGER NOT NULL,
                        endTime INTEGER NOT NULL,
                        maxSpeed REAL NOT NULL,
                        averageSpeed REAL NOT NULL,
                        accelerations INTEGER NOT NULL,
                        brakings INTEGER NOT NULL,
                        distanceMeters REAL NOT NULL,
                        FOREIGN KEY(vehicleId) REFERENCES vehicles(id) ON DELETE CASCADE
                    );
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_driving_sessions_vehicleId ON driving_sessions_new(vehicleId);")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_driving_sessions_endTime ON driving_sessions_new(endTime);")
                db.execSQL("""
                    INSERT INTO driving_sessions_new(id, vehicleId, startTime, endTime, maxSpeed, averageSpeed, accelerations, brakings, distanceMeters)
                    SELECT id, vehicleId, startTime, endTime, maxSpeed, averageSpeed, accelerations, brakings, distanceMeters FROM driving_sessions;
                """.trimIndent())
                db.execSQL("DROP TABLE driving_sessions;")
                db.execSQL("ALTER TABLE driving_sessions_new RENAME TO driving_sessions;")

                db.execSQL("PRAGMA foreign_keys=ON;")
            }
        }
        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
            }
        }

        @Volatile
        private var INSTANCE: VehicleDatabase? = null

        fun getDatabase(context: Context): VehicleDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VehicleDatabase::class.java,
                    "vehicle_database"
                ).addMigrations(MIGRATION_7_8, MIGRATION_8_9).fallbackToDestructiveMigrationFrom(1, 2, 3, 4, 5, 6).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
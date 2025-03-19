package com.example.car_app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.car_app.data.dao.VehicleDao
import com.example.car_app.data.entities.Vehicle

@Database(entities = [Vehicle::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun vehicleDao(): VehicleDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "car_health_database"
                )
                    .fallbackToDestructiveMigration() // Evita crasheos por cambios de versión
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
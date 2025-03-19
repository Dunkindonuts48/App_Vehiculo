package com.example.car_app

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.car_app.data.AppDatabase
import com.example.car_app.data.entities.Vehicle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val db = AppDatabase.getDatabase(this)
        val vehicleDao = db.vehicleDao()

        lifecycleScope.launch(Dispatchers.IO) {
            val existingVehicle = vehicleDao.getVehicle("Toyota", "Corolla", 2020)

            if (existingVehicle == null) {
                val vehicle = Vehicle(brand = "Toyota", model = "Corolla", year = 2020, mileage = 50000)
                vehicleDao.insertVehicle(vehicle)
                Log.d("DB_TEST", "Nuevo vehículo insertado")
            } else {
                Log.d("DB_TEST", "El vehículo ya existe, no se inserta")
            }

            val vehicles = vehicleDao.getAllVehicles()
            vehicles.forEach {
                Log.d("DB_TEST", "Vehículo: ${it.brand} ${it.model} (${it.year})")
            }
        }
    }
}

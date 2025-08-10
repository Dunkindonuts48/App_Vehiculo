package com.example.autocare

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.autocare.bluetooth.BluetoothManager
import com.example.autocare.navigation.AppNavigation
import com.example.autocare.ui.theme.AutoCareTheme
import com.example.autocare.vehicle.VehicleDatabase
import com.example.autocare.vehicle.VehicleViewModel
import com.example.autocare.vehicle.VehicleViewModelFactory

class MainActivity : ComponentActivity() {
    private lateinit var btManager: BluetoothManager
    private val requestBluetoothConnect = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) btManager.register()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = VehicleDatabase.getDatabase(applicationContext)
        val factory = VehicleViewModelFactory(
            db.vehicleDao(),
            db.maintenanceDao(),
            db.drivingSessionDao(),
            db.fuelEntryDao()
        )
        val viewModel = ViewModelProvider(this, factory).get(VehicleViewModel::class.java)
        btManager = BluetoothManager(this, db.vehicleDao())
        val dest = intent?.getStringExtra("dest")
        val vehicleId = intent?.getIntExtra("vehicleId", -1) ?: -1
        if (dest == "tracking" && vehicleId > 0) {
            val deepLink = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("autocare://tracking/$vehicleId"),
                this,
                MainActivity::class.java
            )
            setIntent(deepLink)
        }

        setContent {
            AutoCareTheme {
                AppNavigation(viewModel)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED -> {
                    btManager.register()
                }
                shouldShowRequestPermissionRationale(Manifest.permission.BLUETOOTH_CONNECT) -> {
                    requestBluetoothConnect.launch(Manifest.permission.BLUETOOTH_CONNECT)
                }
                else -> {
                    requestBluetoothConnect.launch(Manifest.permission.BLUETOOTH_CONNECT)
                }
            }
        } else {
            btManager.register()
        }
    }
    override fun onStop() {
        super.onStop()
        btManager.unregister()
    }
}
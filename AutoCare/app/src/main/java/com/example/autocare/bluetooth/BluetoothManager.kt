package com.example.autocare.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.autocare.sensor.SensorTrackingService
import com.example.autocare.vehicle.VehicleDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BluetoothManager(
    private val context: Context,
    private val vehicleDao: VehicleDao
) {
    private val adapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

    private val receiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(ctx: Context?, intent: Intent?) {
            if (!hasBluetoothConnectPermission()) return

            val action = intent?.action ?: return
            val device: BluetoothDevice? = try {
                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
            } catch (e: SecurityException) {
                Log.w("BluetoothManager", "Permission denied reading EXTRA_DEVICE", e)
                null
            }

            if (device == null) return

            CoroutineScope(Dispatchers.IO).launch {
                val vehicles = vehicleDao.getAll()
                vehicles
                    .filter { it.bluetoothMac.equals(device.address, ignoreCase = true) }
                    .forEach { vehicle ->
                        when (action) {
                            BluetoothDevice.ACTION_ACL_CONNECTED -> startTracking(vehicle.id)
                            BluetoothDevice.ACTION_ACL_DISCONNECTED -> stopTracking()
                        }
                    }
            }
        }
    }

    fun register() {
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }
        context.registerReceiver(receiver, filter)
    }

    fun unregister() {
        context.unregisterReceiver(receiver)
    }

    private fun startTracking(vehicleId: Int) {
        val svc = Intent(context, SensorTrackingService::class.java).apply {
            putExtra("vehicleId", vehicleId)
        }
        ContextCompat.startForegroundService(context, svc)
    }

    private fun stopTracking() {
        val svc = Intent(context, SensorTrackingService::class.java)
        context.stopService(svc)
    }

    @SuppressLint("MissingPermission")
    fun getBondedDevices(): Set<BluetoothDevice> {
        return if (hasBluetoothConnectPermission()) {
            try {
                adapter?.bondedDevices.orEmpty()
            } catch (e: SecurityException) {
                Log.w("BluetoothManager", "Permission denied accessing bondedDevices", e)
                emptySet()
            }
        } else {
            emptySet()
        }
    }

    private fun hasBluetoothConnectPermission(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
}
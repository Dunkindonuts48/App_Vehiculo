package com.example.autocare.bluetooth

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.example.autocare.sensor.SensorTrackingService
import com.example.autocare.vehicle.VehicleDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BluetoothReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val action = intent.action ?: return
        val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
            ?: return
        val dao = VehicleDatabase.getDatabase(ctx).vehicleDao()
        CoroutineScope(Dispatchers.IO).launch {
            dao.getAll().firstOrNull { it.bluetoothMac == device.address }?.let { vehicle ->
                when (action) {
                    BluetoothDevice.ACTION_ACL_CONNECTED -> {
                        val svc = Intent(ctx, SensorTrackingService::class.java).apply { putExtra("vehicleId", vehicle.id) }
                        ContextCompat.startForegroundService(ctx, svc)
                    }
                    BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                        ctx.stopService(Intent(ctx, SensorTrackingService::class.java))
                    }
                }
            }
        }
    }
}

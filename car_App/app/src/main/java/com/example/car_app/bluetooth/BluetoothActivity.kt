package com.example.car_app.bluetooth

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.car_app.R

class BluetoothActivity : AppCompatActivity() {

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private lateinit var bluetoothService: BluetoothService
    private val REQUEST_ENABLE_BT = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bluetooth)

        bluetoothService = BluetoothService()

        findViewById<Button>(R.id.btnConnect).setOnClickListener {
            if (bluetoothAdapter == null) {
                Toast.makeText(this, "Bluetooth no soportado", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!bluetoothAdapter!!.isEnabled) {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
            } else {
                connectToDevice()
            }
        }
    }

    private fun connectToDevice() {
        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
        pairedDevices?.forEach { device ->
            Log.d("BluetoothActivity", "Dispositivo encontrado: ${device.name} - ${device.address}")

            if (device.name.contains("OBD") || device.name.contains("ELM")) {
                val isConnected = bluetoothService.connectToDevice(device)
                if (isConnected) {
                    Toast.makeText(this, "Conectado a ${device.name}", Toast.LENGTH_SHORT).show()
                    bluetoothService.sendCommand("ATZ") // Resetea OBD-II
                } else {
                    Toast.makeText(this, "Error al conectar", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        bluetoothService.closeConnection()
    }
}

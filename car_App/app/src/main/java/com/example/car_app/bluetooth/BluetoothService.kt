package com.example.car_app.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*

class BluetoothService {
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var socket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    fun connectToDevice(device: BluetoothDevice): Boolean {
        try {
            val uuid = device.uuids[0].uuid
            socket = device.createRfcommSocketToServiceRecord(uuid)
            socket?.connect()

            inputStream = socket?.inputStream
            outputStream = socket?.outputStream

            Log.d("BluetoothService", "Conexión establecida con ${device.name}")
            return true
        } catch (e: IOException) {
            Log.e("BluetoothService", "Error al conectar con el dispositivo", e)
            return false
        }
    }

    fun sendCommand(command: String) {
        try {
            outputStream?.write(command.toByteArray())
            outputStream?.flush()
        } catch (e: IOException) {
            Log.e("BluetoothService", "Error al enviar comando", e)
        }
    }

    fun readResponse(): String {
        return try {
            val buffer = ByteArray(1024)
            val bytesRead = inputStream?.read(buffer) ?: 0
            String(buffer, 0, bytesRead)
        } catch (e: IOException) {
            Log.e("BluetoothService", "Error al leer respuesta", e)
            ""
        }
    }

    fun closeConnection() {
        try {
            inputStream?.close()
            outputStream?.close()
            socket?.close()
        } catch (e: IOException) {
            Log.e("BluetoothService", "Error al cerrar conexión", e)
        }
    }
}

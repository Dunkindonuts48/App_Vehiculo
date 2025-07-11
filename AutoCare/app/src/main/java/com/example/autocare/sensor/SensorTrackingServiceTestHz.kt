// SensorTrackingServiceTestHz.kt
package com.example.autocare.sensor

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.autocare.R
import com.example.autocare.vehicle.VehicleDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sqrt

class SensorTrackingServiceTestHz : Service(), SensorEventListener {
    private val NOTIF_ID = 2
    private val CHANNEL_ID = "tracking_hz_channel"
    private lateinit var notificationManager: NotificationManager
    private lateinit var sensorManager: SensorManager
    private lateinit var dao: SensorDataDao

    private var vehicleId = -1
    private var sessionStart = 0L
    private var lastAccel = floatArrayOf(0f,0f,0f)

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        sensorManager       = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        dao                 = VehicleDatabase.getDatabase(applicationContext).sensorDataDao()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel(
                CHANNEL_ID, "Seguimiento Hz", NotificationManager.IMPORTANCE_LOW
            ).also { notificationManager.createNotificationChannel(it) }
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.also {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)?.also {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        vehicleId    = intent?.getIntExtra("vehicleId", -1) ?: -1
        sessionStart = System.currentTimeMillis()
        val notif = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Seguimiento Hz activo")
            .setContentText("Midiendo acelerómetro…")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        startForeground(NOTIF_ID, notif)
        return START_STICKY
    }

    override fun onSensorChanged(evt: SensorEvent) {
        when(evt.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> lastAccel = evt.values.copyOf()
            Sensor.TYPE_GYROSCOPE     -> {}
        }
        val data = SensorData(
            vehicleId = vehicleId,
            timestamp = System.currentTimeMillis(),
            speed     = 0f,
            accelX    = lastAccel[0],
            accelY    = lastAccel[1],
            accelZ    = lastAccel[2],
            gyroX     = 0f,
            gyroY     = 0f,
            gyroZ     = 0f
        )
        CoroutineScope(Dispatchers.IO).launch { dao.insert(data) }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)

        if (vehicleId != -1) {
            CoroutineScope(Dispatchers.IO).launch {
                val all = dao.getByVehicle(vehicleId).first()
                    .filter { it.timestamp >= sessionStart }
                if (all.isEmpty()) return@launch
                var accelCount = 0
                all.forEach { d ->
                    val mag = sqrt(d.accelX*d.accelX + d.accelY*d.accelY + d.accelZ*d.accelZ)
                    if (mag > 2f) accelCount++
                }

                val score = (accelCount.toFloat()/all.size)*100f
                Log.i("HzService", "Aggressiveness Hz = $score")
            }
        }
    }

    companion object {
        suspend fun calculateAggressivenessHz(context: Context, vehicleId: Int): Float {
            val dao = VehicleDatabase.getDatabase(context).sensorDataDao()
            val data = dao.getByVehicle(vehicleId).first()
            if (data.isEmpty()) return 0f
            var count = 0
            data.forEach {
                val m = sqrt(it.accelX*it.accelX + it.accelY*it.accelY + it.accelZ*it.accelZ)
                if (m > 2f) count++
            }
            return (count.toFloat()/data.size)*100f
        }
    }
}
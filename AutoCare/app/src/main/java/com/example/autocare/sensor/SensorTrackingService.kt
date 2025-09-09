package com.example.autocare.sensor

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.autocare.R
import com.example.autocare.util.Notifier
import com.example.autocare.vehicle.VehicleDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.abs

class SensorTrackingService : Service(), SensorEventListener, LocationListener {
    private val NOTIFICATION_ID = 1
    private val CHANNEL_ID = "tracking_channel"
    private lateinit var notificationManager: NotificationManager
    private lateinit var sensorManager: SensorManager
    private lateinit var locationManager: LocationManager
    private lateinit var sensorDataDao: SensorDataDao
    private lateinit var drivingSessionDao: DrivingSessionDao
    private var vehicleId: Int = -1
    private var speed: Float = 0.0f
    private var accel = FloatArray(3)
    private var gyro = FloatArray(3)
    private var sessionStartTime: Long = 0L
    private var linAcc = FloatArray(3)

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val db = VehicleDatabase.getDatabase(applicationContext)
        sensorDataDao = db.sensorDataDao()
        drivingSessionDao = db.drivingSessionDao()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Seguimiento Predictivo",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }

        sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)?.also {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)?.also {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }

        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                500L,
                0f,
                this
            )
        } catch (e: SecurityException) {
            Log.e("SensorService", "Permiso de ubicación denegado: ${e.message}")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        isRunning = true
        vehicleId = intent?.getIntExtra("vehicleId", -1) ?: -1
        sessionStartTime = System.currentTimeMillis()

        val largeIcon = BitmapFactory.decodeResource(resources, R.drawable.logo_notificacion_autocare)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.logo_notificacion_autocare)
            .setLargeIcon(largeIcon)
            .setContentTitle("Seguimiento activo")
            .setContentText("Recopilando datos de conducción…")
            .setColor(ContextCompat.getColor(this, R.color.teal_700))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setContentIntent(Notifier.pendingToTracking(this, vehicleId))
            .build()

        startForeground(NOTIFICATION_ID, notification)
        return START_STICKY
    }

    override fun onSensorChanged(event: SensorEvent?) {
        /*
            event?.let {
            when (it.sensor.type) {
                Sensor.TYPE_LINEAR_ACCELERATION -> linAcc = it.values.copyOf()
                Sensor.TYPE_GYROSCOPE -> gyro = it.values.copyOf()
                Sensor.TYPE_ACCELEROMETER -> accel = it.values.copyOf()
            }
        }
        */
    }

    override fun onLocationChanged(location: Location) {
        speed = location.speed
        updateNotification(speed)

        if (vehicleId != -1) {
            val data = SensorData(
                vehicleId = vehicleId,
                timestamp = System.currentTimeMillis(),
                speed = speed,
                accelX = accel[0],
                accelY = accel[1],
                accelZ = accel[2],
                gyroX = gyro[0],
                gyroY = gyro[1],
                gyroZ = gyro[2]
            )
            CoroutineScope(Dispatchers.IO).launch {
                sensorDataDao.insert(data)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    override fun onBind(intent: Intent?) = null

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        try { sensorManager.unregisterListener(this) } catch (_: Exception) {}
        try { locationManager.removeUpdates(this) } catch (_: SecurityException) {}

        if (vehicleId == -1) return

        CoroutineScope(Dispatchers.IO).launch {
            val allData = sensorDataDao.getByVehicle(vehicleId).first()
                .filter { it.timestamp >= sessionStartTime }

            if (allData.size < 2) return@launch

            val endTime = System.currentTimeMillis()
            val maxSpeed = allData.maxOf { it.speed }
            val avgSpeed = allData.map { it.speed }.average().toFloat()
            var accelerations = 0
            var brakings = 0
            var lastEventMs = 0L
            val alpha = 0.25f
            var emaPrev = allData.first().speed
            var distanceMeters = 0f

            for (i in 1 until allData.size) {
                val prev = allData[i - 1]
                val curr = allData[i]
                val dt = (curr.timestamp - prev.timestamp) / 1000f
                if (dt !in 0.4f..6.0f) continue

                distanceMeters += 0.5f * (prev.speed + curr.speed) * dt

                val emaCurr = alpha * curr.speed + (1 - alpha) * emaPrev
                val a = (emaCurr - emaPrev) / dt
                val dv = (emaCurr - emaPrev)
                val nowTs = curr.timestamp
                val free = (nowTs - lastEventMs) > 1200L
                val minSpeedForAccel = emaPrev > 3.0f
                val minSpeedForBrake = emaPrev > 5.0f

                if (free && minSpeedForAccel && a > 1.8f && dv > 2.0f) {
                    accelerations++
                    lastEventMs = nowTs
                } else if (free && minSpeedForBrake && a < -2.2f && dv < -2.0f) {
                    brakings++
                    lastEventMs = nowTs
                }

                emaPrev = emaCurr
            }

            val session = DrivingSession(
                vehicleId = vehicleId,
                startTime = sessionStartTime,
                endTime = endTime,
                maxSpeed = maxSpeed,
                averageSpeed = avgSpeed,
                accelerations = accelerations,
                brakings = brakings,
                distanceMeters = distanceMeters
            )
            drivingSessionDao.insert(session)
            sensorDataDao.deleteRange(vehicleId, sessionStartTime, endTime)
        }
    }

    companion object {
        @Volatile
        var isRunning = false

        suspend fun calculateAggressiveness(context: Context, vehicleId: Int): Float {
            val dao = VehicleDatabase.getDatabase(context).sensorDataDao()
            val data = dao.getByVehicle(vehicleId).first()
            if (data.isEmpty()) return 0f
            var score = 0f
            data.forEach {
                if (abs(it.gyroZ) > 2f) score += 1f
                if (it.speed > 30f) score += 0.5f
            }
            val normalized = (score / data.size) * 100f
            return normalized.coerceIn(0f, 100f)
        }
    }

    private fun updateNotification(speed: Float) {
        val speedKmH = (speed * 3.6f).toInt()
        val largeIcon = BitmapFactory.decodeResource(resources, R.drawable.logo_notificacion_autocare)
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.logo_notificacion_autocare)
            .setLargeIcon(largeIcon)
            .setContentTitle("Seguimiento en curso")
            .setContentText("Velocidad actual: $speedKmH km/h")
            .setColor(ContextCompat.getColor(this, R.color.teal_700))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setContentIntent(Notifier.pendingToTracking(this, vehicleId))
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
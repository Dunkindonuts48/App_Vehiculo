package com.example.autocare.sensor

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.autocare.util.Notifier
import com.example.autocare.vehicle.Vehicle
import com.example.autocare.vehicle.VehicleDatabase
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min

class PredictiveCheckWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    private val db by lazy { VehicleDatabase.Companion.getDatabase(appContext) }
    private val vehicleDao get() = db.vehicleDao()
    private val maintenanceDao get() = db.maintenanceDao()
    private val sessionDao get() = db.drivingSessionDao()
    private val SOON_DAYS = 20
    private val SOON_KM = 5_000
    private val SCORE_UMBRAL = 60
    private val DISPLAY_FMT: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd/MM/yyyy").withLocale(Locale.getDefault())
    private fun parseDisplayDateOrToday(s: String?): LocalDate =
        try { LocalDate.parse(s, DISPLAY_FMT) } catch (_: Exception) { LocalDate.now() }

    override suspend fun doWork(): Result {
        val vehicles: List<Vehicle> = vehicleDao.getAll()
        val today = LocalDate.now()

        vehicles.forEach { v ->
            if (!hasAnySoonMaintenance(v, today)) return@forEach
            val score = computeWearScoreLast7Days(v.id)
            if (score >= SCORE_UMBRAL) {
                Notifier.showPredictive(applicationContext, v, score)
            }
        }
        return Result.success()
    }

    private suspend fun hasAnySoonMaintenance(vehicle: Vehicle, today: LocalDate): Boolean {
        val history = maintenanceDao.getByVehicle(vehicle.id).first()
        val sessions = sessionDao.getSessionsByVehicle(vehicle.id).first()
        val extraKm = sessions.sumOf { it.distanceMeters.toDouble() / 1000 }.toInt()
        val currentKm = vehicle.mileage + extraKm

        val byMonths = mapOf(
            "Neumáticos: estado" to 60,
            "Filtro de habitáculo" to 12,
            "Líquido de frenos" to 24,
            "Batería 12 V" to 36,
            "Cambio de aceite de motor" to 12,
            "Filtro de aceite" to 12,
            "Filtro de aire de motor" to 12,
            "Refrigerante / anticongelante" to 24,
            "Correa de distribución / cadena" to 60
        )
        val byKm = mapOf(
            "Neumáticos: estado" to 60_000,
            "Filtro de aire de motor" to 30_000,
            "Filtro de combustible" to 40_000,
            "Cambio de aceite de motor" to 30_000,
            "Filtro de aceite" to 30_000,
            "Frenos: pastillas" to 70_000,
            "Frenos: discos" to 140_000,
            "Correa de distribución / cadena" to 160_000
        )

        val types = (byMonths.keys + byKm.keys).distinct()

        for (type in types) {
            val lastDate: LocalDate = history
                .filter { it.type == type }
                .maxByOrNull { it.date }
                ?.date
                ?: parseDisplayDateOrToday(vehicle.purchaseDate)

            val lastKm = history
                .filter { it.type == type }
                .maxByOrNull { it.mileageAtMaintenance }
                ?.mileageAtMaintenance
                ?: vehicle.mileage

            byMonths[type]?.let { months ->
                val nextDate = lastDate.plusMonths(months.toLong())
                val leftDays = ChronoUnit.DAYS.between(today, nextDate).toInt()
                if (leftDays in 0..SOON_DAYS) return true
            }

            byKm[type]?.let { stepKm ->
                val nextKm = lastKm + stepKm
                val leftKm = nextKm - currentKm
                if (leftKm in 0..SOON_KM) return true
            }
        }
        return false
    }
    private suspend fun computeWearScoreLast7Days(vehicleId: Int): Int {
        val sevenDaysAgoMs = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
        val sessions = sessionDao.getSessionsByVehicle(vehicleId).first()
            .filter { it.startTime >= sevenDaysAgoMs }
        if (sessions.isEmpty()) return 0
        val totalKm = sessions.sumOf { it.distanceMeters.toDouble() / 1000.0 }
        val totalTimeSec = sessions.sumOf {
            max(0L, it.endTime - it.startTime) / 1000.0
        }
        val totalAccel = sessions.sumOf { it.accelerations }
        val totalBrakes = sessions.sumOf { it.brakings }
        val kmScore = ((min(totalKm, 500.0) / 500.0) * 40.0)
        val eventsPerKm = if (totalKm > 0) (totalAccel + totalBrakes) / totalKm else 0.0
        val eventsScore = (min(eventsPerKm, 10.0) / 10.0) * 35.0
        val avgSpeed = if (totalTimeSec > 0) (totalKm / (totalTimeSec / 3600.0)) else 0.0
        val speedScore = (min(avgSpeed, 120.0) / 120.0) * 25.0
        return (kmScore + eventsScore + speedScore).toInt()
    }
    companion object {
        private const val WORK_NAME = "predictive_check_work"
        fun schedule(ctx: Context) {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()

            val request = PeriodicWorkRequestBuilder<PredictiveCheckWorker>(
                24, TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .addTag(WORK_NAME)
                .build()

            WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
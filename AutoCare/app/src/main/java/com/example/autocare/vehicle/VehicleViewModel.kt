package com.example.autocare.vehicle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.autocare.sensor.DrivingSession
import com.example.autocare.sensor.DrivingSessionDao
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.threeten.bp.LocalDate
import org.threeten.bp.temporal.ChronoUnit
import org.threeten.bp.format.DateTimeFormatter

class VehicleViewModel(
    private val dao: VehicleDao,
    private val maintenanceDao: MaintenanceDao,
    private val drivingSessionDao: DrivingSessionDao
) : ViewModel() {

    private val _vehicles = MutableStateFlow<List<Vehicle>>(emptyList())
    val vehicles: StateFlow<List<Vehicle>> = _vehicles.asStateFlow()

    private val _vehicleMaintenances = MutableStateFlow<Map<Int, List<Maintenance>>>(emptyMap())
    val vehicleMaintenances: StateFlow<Map<Int, List<Maintenance>>> = _vehicleMaintenances.asStateFlow()

    init {
        loadVehicles()
    }

    fun loadVehicles() {
        viewModelScope.launch {
            val allVehicles = dao.getAll()
            _vehicles.value = allVehicles

            val maintenanceMap = mutableMapOf<Int, List<Maintenance>>()
            allVehicles.forEach { vehicle ->
                maintenanceDao.getByVehicle(vehicle.id).collect { list ->
                    maintenanceMap[vehicle.id] = list
                    _vehicleMaintenances.value = maintenanceMap.toMap()
                }
            }
        }
    }

    suspend fun getVehiclesWithUrgentMaintenanceSuspended(): List<Vehicle> {
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        val today = LocalDate.now()

        return vehicles.value.filter { vehicle ->
            val maintenances = maintenanceDao.getByVehicle(vehicle.id).first()

            val lastReviewDate = try {
                LocalDate.parse(vehicle.lastMaintenanceDate, formatter)
            } catch (e: Exception) {
                null
            }

            val monthsPassed = lastReviewDate?.let { ChronoUnit.MONTHS.between(it, today) } ?: 0
            val isTimeExceeded = vehicle.maintenanceFrequencyMonths > 0 &&
                    monthsPassed >= vehicle.maintenanceFrequencyMonths

            val predicted = getPredictedMaintenance(vehicle)

            val isCriticalMaintenance = maintenances.any { m ->
                m.type.contains("aceite", ignoreCase = true) ||
                        m.type.contains("frenos", ignoreCase = true) ||
                        m.type.contains("batería", ignoreCase = true)
            }

            (isTimeExceeded && isCriticalMaintenance) || predicted.isNotEmpty()
        }
    }

    fun registerVehicle(vehicle: Vehicle) {
        viewModelScope.launch {
            val newId = dao.insertVehicle(vehicle).toInt()
            registerDefaultMaintenances(newId, vehicle)
            loadVehicles()
        }
    }

    private suspend fun registerDefaultMaintenances(vehicleId: Int, vehicle: Vehicle) {
        val date = vehicle.purchaseDate
        val type = vehicle.type.lowercase()

        val common = listOf("Neumáticos", "Frenos", "Revisión general")
        val diesel = listOf("Cambio de aceite", "Filtro de partículas", "AdBlue")
        val gasolina = listOf("Cambio de aceite", "Bujías")
        val electrico = listOf("Revisión batería", "Sistema eléctrico")

        val tipoMantenimientos = mutableListOf<String>()
        tipoMantenimientos += common
        when {
            "eléctrico" in type -> tipoMantenimientos += electrico
            "diésel" in type -> tipoMantenimientos += diesel
            "gasolina" in type || "híbrido" in type -> tipoMantenimientos += gasolina
        }

        tipoMantenimientos.distinct().forEach { tipo ->
            maintenanceDao.insert(
                Maintenance(
                    id = 0,
                    vehicleId = vehicleId,
                    type = tipo,
                    date = date,
                    cost = 0.0
                )
            )
        }
    }

    fun updateVehicle(vehicle: Vehicle) {
        viewModelScope.launch {
            dao.updateVehicle(vehicle)
            loadVehicles()
        }
    }

    fun deleteVehicle(vehicle: Vehicle) {
        viewModelScope.launch {
            dao.deleteVehicle(vehicle)
            loadVehicles()
        }
    }

    fun getMaintenancesForVehicle(vehicleId: Int): StateFlow<List<Maintenance>> {
        val flow = MutableStateFlow<List<Maintenance>>(emptyList())
        viewModelScope.launch {
            maintenanceDao.getByVehicle(vehicleId).collect {
                flow.value = it
            }
        }
        return flow.asStateFlow()
    }

    fun getDrivingSessionsForVehicle(vehicleId: Int): Flow<List<DrivingSession>> {
        return drivingSessionDao.getSessionsByVehicle(vehicleId)
    }

    fun registerMaintenance(maintenance: Maintenance) {
        viewModelScope.launch {
            maintenanceDao.insert(maintenance)
            loadVehicles()
        }
    }

    fun deleteMaintenance(maintenance: Maintenance) {
        viewModelScope.launch {
            maintenanceDao.delete(maintenance)
            loadVehicles()
        }
    }

    suspend fun getPredictedMaintenance(vehicle: Vehicle): List<String> {
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        val today = LocalDate.now()
        val maintenances = maintenanceDao.getByVehicle(vehicle.id).first()

        val revision = maintenances.filter { it.type.equals("Revisión general", ignoreCase = true) }
            .maxByOrNull { it.date }

        val result = mutableListOf<String>()

        val isTimeExceeded = revision?.let {
            val lastDate = try {
                LocalDate.parse(it.date, formatter)
            } catch (e: Exception) {
                null
            }
            val monthsPassed = lastDate?.let { ChronoUnit.MONTHS.between(it, today) } ?: 0
            monthsPassed >= vehicle.maintenanceFrequencyMonths
        } ?: false

        if (isTimeExceeded) result += "Revisión general"
        val sessions = drivingSessionDao.getSessionsByVehicle(vehicle.id).first()
        var highAggressiveCount = 0
        var highSpeedCount = 0

        sessions.forEach { session ->
            if (session.accelerations >= 5 || session.brakings >= 5) highAggressiveCount++
            if (session.averageSpeed > 33f) highSpeedCount++
        }

        val totalSessions = highAggressiveCount + highSpeedCount
        if (totalSessions >= 3) result += "Revisión anticipada por conducción agresiva"

        return result
    }

    fun deleteDrivingSession(session: DrivingSession) {
        viewModelScope.launch {
            drivingSessionDao.delete(session)
        }
    }
}
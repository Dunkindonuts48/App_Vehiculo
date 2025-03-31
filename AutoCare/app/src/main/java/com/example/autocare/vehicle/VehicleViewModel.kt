package com.example.autocare.vehicle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.autocare.sensor.DrivingSession
import com.example.autocare.sensor.DrivingSessionDao
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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

    fun getVehiclesWithUrgentMaintenance(): List<Vehicle> {
        return vehicles.value.filter { vehicle ->
            val maintenances = vehicleMaintenances.value[vehicle.id] ?: emptyList()
            maintenances.any { m ->
                m.type.contains("aceite", ignoreCase = true) ||
                        m.type.contains("frenos", ignoreCase = true) ||
                        m.type.contains("batería", ignoreCase = true)
            }
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

    fun getPredictedMaintenance(vehicle: Vehicle, history: List<Maintenance>): List<String> {
        val revision = history.filter { it.type.equals("Revisión general", ignoreCase = true) }
            .maxByOrNull { it.date }

        if (revision != null) {
            // Usar el formato correcto
            val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
            val lastDate = LocalDate.parse(revision.date, formatter)
            val today = LocalDate.now()
            val monthsPassed = ChronoUnit.MONTHS.between(lastDate, today)

            if (vehicle.maintenanceFrequencyMonths > 0 && monthsPassed >= vehicle.maintenanceFrequencyMonths) {
                return listOf("Revisión general")
            }
        }

        return emptyList()
    }


    fun deleteDrivingSession(session: DrivingSession) {
        viewModelScope.launch {
            drivingSessionDao.delete(session)
        }
    }
}
package com.example.autocare.vehicle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.autocare.sensor.DrivingSession
import com.example.autocare.sensor.DrivingSessionDao
import com.example.autocare.vehicle.fuel.FuelEntry
import com.example.autocare.vehicle.fuel.FuelEntryDao
import com.example.autocare.vehicle.maintenance.Maintenance
import com.example.autocare.vehicle.maintenance.MaintenanceDao
import com.example.autocare.vehicle.maintenance.NextMaintenance
import com.example.autocare.vehicle.maintenance.ReviewStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

class VehicleViewModel(
    private val dao: VehicleDao,
    private val maintenanceDao: MaintenanceDao,
    private val drivingSessionDao: DrivingSessionDao,
    private val fuelEntryDao: FuelEntryDao
) : ViewModel() {
    private val _vehicles = MutableStateFlow<List<Vehicle>>(emptyList())
    val vehicles: StateFlow<List<Vehicle>> = _vehicles.asStateFlow()
    private val _vehicleMaintenances = MutableStateFlow<Map<Int, List<Maintenance>>>(emptyMap())
    val vehicleMaintenances: StateFlow<Map<Int, List<Maintenance>>> = _vehicleMaintenances.asStateFlow()
    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()
    private val _nextMaint = MutableStateFlow<Map<Int, List<NextMaintenance>>>(emptyMap())
    val nextMaint: StateFlow<Map<Int, List<NextMaintenance>>> = _nextMaint.asStateFlow()
    private val DISPLAY_FMT: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd/MM/yyyy").withLocale(Locale.getDefault())

    private fun isKmType(type: String) = intervalKms.containsKey(type)
    private fun isTimeType(type: String) = intervalMonths.containsKey(type)
    private fun parseDisplayDateOrToday(s: String?): LocalDate =
        try { LocalDate.parse(s, DISPLAY_FMT) } catch (_: Exception) { LocalDate.now() }
    private val intervalMonths = mapOf(
        "Neumáticos: estado" to 60,
        "Filtro de habitáculo" to 12,
        "Líquido de frenos" to 24,
        "Batería 12 V" to 36,
        "Cambio de aceite de motor" to 12,
        "Filtro de aceite" to 12,
        "Filtro de aire de motor" to 12,
        "Refrigerante / anticongelante" to 24,
        "Correa de distribución / cadena" to 60,
        "Inspección de admisión de aire" to 12,
        "Batería híbrida: estado y refrigeración" to 120,
        "Revisión del sistema regenerativo" to 24,
        "Revisión del freno motor eléctrico" to 12,
        "Batería de tracción: estado y balance" to 48,
        "Líquido de refrigeración de batería" to 24,
        "Sistema de recarga: conectores" to 24,
        "Freno regenerativo" to 12
    )
    private val intervalKms = mapOf(
        "Neumáticos: estado" to 60000,
        "Filtro de aire de motor" to 30000,
        "Filtro de combustible" to 40000,
        "Cambio de aceite de motor" to 30000,
        "Filtro de aceite" to 30000,
        "Filtro de partículas diésel (DPF)" to 150000,
        "Frenos: pastillas" to 70000,
        "Frenos: discos" to 140000,
        "Filtro de habitáculo" to 20000,
        "Líquido de frenos" to 50000,
        "Refrigerante / anticongelante" to 40000,
        "Sistema de escape: fugas" to 30000,
        "Correa de distribución / cadena" to 160000,
        "Bujías de gasolina" to 100000,
        "Sistema de encendido (bobinas)" to 160000,
        "Limpieza de inyectores" to 60000,
        "Inspección de admisión de aire" to 15000,
        "AdBlue" to 20000,
        "Sistema de inyección diésel: boquillas" to 200000,
        "Bujías de precalentamiento" to 120000,
        "Batería híbrida: estado y refrigeración" to 240000,
        "Revisión del sistema regenerativo" to 30000,
        "Revisión del freno motor eléctrico" to 40000,
        "Líquido de refrigeración de batería" to 40000,
        "Freno regenerativo" to 15000
    )
    init { loadVehicles() }
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
            refreshNextMaintenances()
        }
    }
    fun registerVehicleWithRevisions(
        vehicle: Vehicle,
        revisionsDates: Map<String, String>,
        revisionsKms: Map<String, String>
    ) {
        viewModelScope.launch {
            val newId = dao.insertVehicle(vehicle).toInt()

            revisionsDates.forEach { (type, dateStr) ->
                val kmAt = revisionsKms[type]?.toIntOrNull() ?: vehicle.mileage
                maintenanceDao.insert(
                    Maintenance(
                        id = 0,
                        vehicleId = newId,
                        type = type,
                        date = parseDisplayDateOrToday(dateStr),
                        cost = 0.0,
                        mileageAtMaintenance = kmAt
                    )
                )
            }
            loadVehicles()
        }
    }
    fun updateVehicle(vehicle: Vehicle) = viewModelScope.launch {
        dao.updateVehicle(vehicle); loadVehicles()
    }
    fun deleteVehicle(vehicle: Vehicle) = viewModelScope.launch {
        dao.deleteVehicle(vehicle); loadVehicles()
    }
    fun getMaintenancesForVehicle(vehicleId: Int): StateFlow<List<Maintenance>> {
        val flow = MutableStateFlow<List<Maintenance>>(emptyList())
        viewModelScope.launch {
            maintenanceDao.getByVehicle(vehicleId).collect { flow.value = it }
        }
        return flow.asStateFlow()
    }
    fun onTrackingStarted() { _isTracking.value = true }
    fun onTrackingStopped() { _isTracking.value = false }
    fun getDrivingSessionsForVehicle(vehicleId: Int): Flow<List<DrivingSession>> =
        drivingSessionDao.getSessionsByVehicle(vehicleId)
    fun registerMaintenance(maintenance: Maintenance) = viewModelScope.launch {
        maintenanceDao.insert(maintenance); loadVehicles()
    }
    fun deleteMaintenance(maintenance: Maintenance) = viewModelScope.launch {
        maintenanceDao.delete(maintenance); loadVehicles()
    }
    private fun statusFrom(leftKm: Int?, leftDays: Int?): ReviewStatus {
        if (leftKm != null && leftKm < 0) return ReviewStatus.OVERDUE
        if (leftDays != null && leftDays < 0) return ReviewStatus.OVERDUE
        if (leftKm != null && leftKm <= 5000) return ReviewStatus.SOON
        if (leftDays != null && leftDays <= 30) return ReviewStatus.SOON
        return ReviewStatus.OK
    }
    private suspend fun computeNextMaintenancesStructured(
        vehicle: Vehicle
    ): List<NextMaintenance> = withContext(Dispatchers.IO) {
        val history = maintenanceDao.getByVehicle(vehicle.id).first()
        val t = vehicle.type.lowercase(Locale.getDefault())

        val commonAll = listOf(
            "Neumáticos: estado",
            "Frenos: pastillas",
            "Frenos: discos",
            "Filtro de habitáculo",
            "Líquido de frenos",
            "Batería 12 V"
        )
        val commonICEHybrid = listOf(
            "Cambio de aceite de motor",
            "Filtro de aceite",
            "Filtro de aire de motor",
            "Sistema de escape: fugas",
            "Refrigerante / anticongelante",
            "Correa de distribución / cadena"
        )
        val gasolina = listOf(
            "Bujías de gasolina",
            "Filtro de combustible",
            "Sistema de encendido (bobinas)",
            "Limpieza de inyectores",
            "Inspección de admisión de aire"
        )
        val diesel = listOf(
            "Filtro de partículas diésel (DPF)",
            "AdBlue: nivel e inyección",
            "Sistema de inyección diésel: boquillas",
            "Bujías de precalentamiento",
            "Filtro de combustible"
        )
        val hybrid = listOf(
            "Batería híbrida: estado y refrigeración",
            "Revisión del sistema regenerativo",
            "Electrónica de potencia",
            "Revisión del freno motor eléctrico"
        )
        val electric = listOf(
            "Batería de tracción: estado y balance",
            "Líquido de refrigeración de batería",
            "Software/firmware del vehículo",
            "Inspección de cableado de alta tensión (HV)",
            "Sistema de recarga: conectores",
            "Freno regenerativo"
        )

        val sessions = drivingSessionDao.getSessionsByVehicle(vehicle.id).first()
        val extraKm = sessions.sumOf { it.distanceMeters.toDouble() / 1000 }.toInt()
        val currentKm = vehicle.mileage + extraKm

        val options = mutableListOf<String>().apply {
            addAll(commonAll)
            if (!t.contains("eléctrico")) addAll(commonICEHybrid)
            when {
                t.contains("gasolina") -> addAll(gasolina)
                t.contains("diésel") || t.contains("diesel") -> addAll(diesel)
                t.contains("híbrido") || t.contains("hibrido") -> addAll(hybrid)
                t.contains("eléctrico") || t.contains("electrico") -> addAll(electric)
            }
        }.distinct()

        options.mapNotNull { type ->
            val lastItemByDate = history.filter { it.type == type }.maxByOrNull { it.date }
            val lastDate: LocalDate = lastItemByDate?.date ?: parseDisplayDateOrToday(vehicle.lastMaintenanceDate)

            val lastItemByKm = history.filter { it.type == type }.maxByOrNull { it.mileageAtMaintenance }
            val lastKm = lastItemByKm?.mileageAtMaintenance ?: vehicle.mileage

            val stepKm = intervalKms[type]
            val stepMonths = intervalMonths[type]

            val nextKm = stepKm?.let { lastKm + it }
            val leftKm = nextKm?.let { it - currentKm }

            val nextDate = stepMonths?.let { lastDate.plusMonths(it.toLong()) }
            val leftDays = nextDate?.let { ChronoUnit.DAYS.between(LocalDate.now(), it).toInt() }

            if (stepKm == null && stepMonths == null) return@mapNotNull null

            NextMaintenance(
                type = type,
                leftKm = leftKm,
                leftDays = leftDays,
                status = statusFrom(leftKm, leftDays),
                nextKm = nextKm,
                nextDate = nextDate
            )
        }
    }
    fun refreshNextMaintenances() {
        viewModelScope.launch {
            val all = dao.getAll()
            _nextMaint.value = all.associate { v ->
                v.id to computeNextMaintenancesStructured(v)
            }
        }
    }
    fun getFuelEntriesForVehicle(vehicleId: Int): Flow<List<FuelEntry>> =
        fuelEntryDao.getByVehicle(vehicleId)
    fun insertFuelEntry(entry: FuelEntry) = viewModelScope.launch {
        fuelEntryDao.insert(entry)
    }
    fun getTotalFuelCost(vehicleId: Int): Flow<Float> =
        fuelEntryDao.getByVehicle(vehicleId)
            .map { entries ->
                val total = entries.sumOf { (it.liters * it.pricePerLiter).toDouble() }
                total.toFloat()
            }
    fun deleteFuelEntry(entry: FuelEntry) = viewModelScope.launch {
        fuelEntryDao.delete(entry)
    }
    fun saveBluetoothForVehicle(vehicleId: Int, mac: String) = viewModelScope.launch {
        val all = dao.getAll()
        val v = all.firstOrNull { it.id == vehicleId } ?: return@launch
        dao.updateVehicle(v.copy(bluetoothMac = mac))
        loadVehicles()
    }
    fun deleteDrivingSession(session: com.example.autocare.sensor.DrivingSession) =
        viewModelScope.launch {
            drivingSessionDao.delete(session)
        }
}
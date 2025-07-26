package com.example.autocare.vehicle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.autocare.sensor.DrivingSession
import com.example.autocare.sensor.DrivingSessionDao
import com.example.autocare.vehicle.fuel.FuelEntry
import com.example.autocare.vehicle.fuel.FuelEntryDao
import com.example.autocare.vehicle.maintenance.Maintenance
import com.example.autocare.vehicle.maintenance.MaintenanceDao
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.temporal.ChronoUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

    private val intervalMonths = mapOf(
        "Neumáticos: presión y estado"                to 3,
        "Frenos: pastillas y discos"                  to 6,
        "Filtro de habitáculo"                        to 12,
        "Luces: faros, intermitentes, luces de freno" to 12,
        "Líquido de frenos"                           to 24,
        "Batería 12 V"                                to 36,
        "Cambio de aceite de motor"                   to 6,
        "Filtro de aceite"                            to 6,
        "Filtro de aire de motor"                     to 12,
        "Sistema de escape: fugas"                    to 24,
        "Refrigerante / anticongelante"               to 24,
        "Correa de distribución / cadena"             to 60,
        "Bujías de gasolina"                          to 24,
        "Sistema de encendido (bobinas)"              to 24,
        "Limpieza de inyectores"                      to 24,
        "Inspección de admisión de aire"              to 24,
        "Filtro de partículas diésel (DPF)"            to 24,
        "AdBlue: nivel e inyección"                   to 12,
        "Sistema de inyección diésel: boquillas"      to 24,
        "Bujías de precalentamiento"                  to 24,
        "Batería híbrida: estado y refrigeración"      to 36,
        "Revisión del sistema regenerativo"           to 12,
        "Electrónica de potencia"                     to 24,
        "Revisión del freno motor eléctrico"          to 12,
        "Batería de tracción: estado y balance"       to 48,
        "Líquido de refrigeración de batería"         to 24,
        "Software/firmware del vehículo"              to 12,
        "Inspección de cableado de alta tensión (HV)" to 24,
        "Sistema de recarga: conectores"              to 24,
        "Freno regenerativo"                          to 12
    )

    private val intervalKms = mapOf(
        "Neumáticos: presión y estado"     to 10000,
        "Filtro de aire de motor"          to 20000,
        "Filtro de combustible"            to 20000,
        "Filtro de partículas diésel (DPF)" to 50000
    )

    private val _mixedCounters = MutableStateFlow<Map<Int, Map<String, String>>>(emptyMap())
    val mixedCounters: StateFlow<Map<Int, Map<String, String>>> = _mixedCounters.asStateFlow()

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
            refreshMixedCounters()
        }
    }

    fun registerVehicleWithRevisions(
        vehicle: Vehicle,
        revisions: Map<String, String>
    ) {
        viewModelScope.launch {
            val newId = dao.insertVehicle(vehicle).toInt()
            revisions.forEach { (type, date) ->
                maintenanceDao.insert(
                    Maintenance(
                        id = 0,
                        vehicleId = newId,
                        type = type,
                        date = date,
                        cost = 0.0,
                        mileageAtMaintenance = vehicle.mileage
                    )
                )
            }
            loadVehicles()
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
    fun onTrackingStarted() {
        _isTracking.value = true
    }
    fun onTrackingStopped() {
        _isTracking.value = false
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
    suspend fun computeNextMaintenancesMixed(vehicle: Vehicle): Map<String, String> = withContext(Dispatchers.IO) {
        val fmt     = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        val today   = LocalDate.now()
        val history = maintenanceDao.getByVehicle(vehicle.id).first()

        val commonAll = listOf(
            "Neumáticos: presión y estado",
            "Frenos: pastillas y discos",
            "Filtro de habitáculo",
            "Luces: faros, intermitentes, luces de freno",
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

        val t = vehicle.type.lowercase()

        val sessions = drivingSessionDao.getSessionsByVehicle(vehicle.id).first()
        val extraKm = sessions.sumOf { it.distanceMeters.toDouble() / 1000 }.toInt()
        val currentKm = vehicle.mileage + extraKm

        val options = mutableListOf<String>().apply {
            addAll(commonAll)
            if (!t.contains("eléctrico")) addAll(commonICEHybrid)
            when {
                t.contains("gasolina") -> addAll(gasolina)
                t.contains("diésel")   -> addAll(diesel)
                t.contains("híbrido")  -> addAll(hybrid)
                t.contains("eléctrico")-> addAll(electric)
            }
        }.distinct()

        options.mapNotNull { type ->
            val lastDate = history
                .filter { it.type == type }
                .maxByOrNull { LocalDate.parse(it.date, fmt) }
                ?.let { LocalDate.parse(it.date, fmt) }
                ?: LocalDate.parse(vehicle.purchaseDate, fmt)

            val lastKm = history
                .filter { it.type == type }
                .maxByOrNull { it.mileageAtMaintenance }
                ?.mileageAtMaintenance
                ?: vehicle.mileage

            when {
                intervalKms.containsKey(type) -> {
                    val nextKm = lastKm + (intervalKms[type] ?: 0)
                    val leftKm = nextKm - currentKm
                    type to "${leftKm.coerceAtLeast(0)} km restantes"
                }
                intervalMonths.containsKey(type) -> {
                    val nextDate = lastDate.plusMonths((intervalMonths[type] ?: 0).toLong())
                    val days = ChronoUnit.DAYS.between(today, nextDate).toInt()
                    type to if (days >= 0) "$days días restantes" else "Venció hace ${-days} días"
                }
                else -> null
            }
        }.toMap()
    }
    fun refreshMixedCounters() {
        viewModelScope.launch {
            val all = dao.getAll()
            _mixedCounters.value = all.associate { v ->
                v.id to computeNextMaintenancesMixed(v)
            }
        }
    }
    fun deleteDrivingSession(session: DrivingSession) {
        viewModelScope.launch {
            drivingSessionDao.delete(session)
        }
    }
    fun getFuelEntriesForVehicle(vehicleId: Int): Flow<List<FuelEntry>> =
        fuelEntryDao.getByVehicle(vehicleId)

    fun insertFuelEntry(entry: FuelEntry) {
        viewModelScope.launch {
            fuelEntryDao.insert(entry)
        }
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

    fun saveBluetoothForVehicle(vehicleId: Int, mac: String) {
        viewModelScope.launch {
            val all = dao.getAll()
            val v = all.firstOrNull { it.id == vehicleId } ?: return@launch
            dao.updateVehicle(v.copy(bluetoothMac = mac))
            loadVehicles()
        }
    }
}
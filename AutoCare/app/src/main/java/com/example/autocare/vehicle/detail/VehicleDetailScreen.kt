package com.example.autocare.vehicle.detail

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.autocare.AppHeader
import com.example.autocare.sensor.SensorTrackingService
import com.example.autocare.util.getVehicleDisplayName
import com.example.autocare.vehicle.maintenance.MaintenanceItemView
import com.example.autocare.vehicle.VehicleViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@SuppressLint("InlinedApi")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleDetailScreen(
    vehicleId: Int,
    viewModel: VehicleViewModel,
    navController: NavHostController
) {
    val vehicles by viewModel.vehicles.collectAsState(initial = emptyList())
    val vehicle = vehicles.firstOrNull { it.id == vehicleId } ?: return
    val maintenances by viewModel.getMaintenancesForVehicle(vehicleId).collectAsState(initial = emptyList())
    val totalCost by remember(maintenances) {
        derivedStateOf { maintenances.sumOf { it.cost } }
    }
    val counters by viewModel.mixedCounters.collectAsState()
    LaunchedEffect(vehicle) { viewModel.refreshMixedCounters() }
    val nexts = counters[vehicleId] ?: emptyMap()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showSheet by remember { mutableStateOf(false) }
    var showForm by remember { mutableStateOf(false) }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
        if (perms.values.all { it }) {
            startTrackingServiceAndNavigate(context, vehicle.id, navController)
        } else {
            Toast.makeText(context, "Permisos denegados", Toast.LENGTH_SHORT).show()
        }
    }
    val fuelEntries by viewModel
        .getFuelEntriesForVehicle(vehicleId)
        .collectAsState(initial = emptyList())
    val fmt = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }
    val today = remember { LocalDate.now() }
    val requestTrackingPermissions = { permLauncher.launch(getTrackingPermissions()) }
    val thisVehicle = vehicles.firstOrNull { it.id == vehicleId }
    val actions = buildList<Pair<String, () -> Unit>> {
        add("Editar Vehículo" to { navController.navigate("form/${vehicle.id}") })
        add((if (showForm) "Cancelar" else "Añadir mantenimiento") to { showForm = !showForm })
        add("Ver repostajes"        to { navController.navigate("fuel/${vehicle.id}") })
        add("Añadir repostaje"      to { navController.navigate("fuel_add/${vehicle.id}") })
        add("Ver mantenimientos" to { navController.navigate("maintenance/${vehicle.id}") })
        add((if (!SensorTrackingService.isRunning) "Iniciar seguimiento" else "Ir a seguimiento") to {
            if (!SensorTrackingService.isRunning) {
                permLauncher.launch(getTrackingPermissions())
            }
            navController.navigate("tracking/$vehicleId")
        })
        add("Sesiones de conducción" to { navController.navigate("sessions/${vehicle.id}") })
        add("Vincular Bluetooth" to { navController.navigate("bluetooth/${vehicle.id}") })
        add("Eliminar Vehículo" to { viewModel.deleteVehicle(vehicle)
            navController.popBackStack("list", false)
        })
    }
    val (monthTotal, yearTotal, allTotal) = remember(fuelEntries) {
        var m = 0f
        var y = 0f
        var a = 0f

        fuelEntries.forEach { e ->
            val date = try {
                LocalDate.parse(e.date, fmt)
            } catch (_: Exception) {
                null
            }
            val cost = e.liters * e.pricePerLiter
            a += cost
            if (date != null) {
                if (date.year == today.year) {
                    y += cost
                    if (date.month == today.month) {
                        m += cost
                    }
                }
            }
        }
        Triple(m, y, a)
    }
    val sessions by viewModel.getDrivingSessionsForVehicle(vehicleId)
        .collectAsState(initial = emptyList())
    val extraKm = remember(sessions) {
        sessions.sumOf { it.distanceMeters.toDouble() / 1000 }.toInt()
    }

    Scaffold(
        topBar = { AppHeader(title = "Vehículo", onBack = { navController.popBackStack() }) },
        floatingActionButton = {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                FloatingActionButton(onClick = { showSheet = true }) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = null)
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = spacedBy(16.dp)
            ) {
                Text(
                    getVehicleDisplayName(vehicle),
                    style = MaterialTheme.typography.titleLarge.copy(textDecoration = TextDecoration.Underline),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = spacedBy(8.dp)) {
                        vehicle?.let { v ->
                            val displayKm = v.mileage + extraKm
                            thisVehicle?.bluetoothMac?.let { mac ->
                                Text("Bluetooth vinculado: $mac", style = MaterialTheme.typography.bodyLarge)
                            }
                            Text(
                                "Marca: ${vehicle.brand}",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                "Modelo: ${vehicle.model}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                "Tipo: ${vehicle.type}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                "Matrícula: ${vehicle.plateNumber}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                "Kilometraje: $displayKm km",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = spacedBy(8.dp)) {
                        Text("Gasto combustible", style = MaterialTheme.typography.titleMedium)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Este mes", style = MaterialTheme.typography.bodyLarge)
                            Text("%.2f €".format(monthTotal), style = MaterialTheme.typography.bodyLarge)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Este año", style = MaterialTheme.typography.bodyLarge)
                            Text("%.2f €".format(yearTotal), style = MaterialTheme.typography.bodyLarge)
                        }
                        Divider(Modifier.padding(vertical = 8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Desde el inicio", style = MaterialTheme.typography.bodyLarge)
                            Text("%.2f €".format(allTotal), style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = spacedBy(4.dp)) {
                        Text("Gasto total", style = MaterialTheme.typography.titleMedium)
                        Text("%.2f €".format(totalCost), style = MaterialTheme.typography.titleLarge)
                    }
                }
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
                    Column(Modifier.padding(12.dp), verticalArrangement = spacedBy(8.dp)) {
                        Text("Próximos mantenimientos:", style = MaterialTheme.typography.titleMedium)
                        if (nexts.isEmpty()) {
                            Text("(calculando…)", style = MaterialTheme.typography.bodySmall)
                        } else {
                            nexts.forEach { (type, info) ->
                                val (numStr, unit) = info.split(" ", limit = 2)
                                val num = numStr.toIntOrNull() ?: 0
                                val bg = when {
                                    info.startsWith("Venció") ->
                                        Color(0xFFF44336)
                                    unit.startsWith("km") && num <= 1000 ->
                                        Color(0xFFF44336)
                                    unit.startsWith("días") && num <= 20 ->
                                        Color(0xFFF44336)
                                    unit.startsWith("km") && num < 5000 ->
                                        Color(0xFFFFC107)
                                    unit.startsWith("días") && num in 21 until 60 ->
                                        Color(0xFFFFC107)
                                    else ->
                                        Color(0xFF4CAF50)
                                }
                                val display = if (unit.startsWith("días") && num > 365) {
                                    val years = num / 365
                                    val days = num % 365
                                    "$years año${if (years > 1) "s" else ""} y $days días"
                                } else info
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, Color.Black, RoundedCornerShape(4.dp))
                                        .background(bg.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(type, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                                    Text(display, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
            if (showForm) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(8.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 4.dp
                ) {
                    MaintenanceItemView(
                        vehicleId = vehicle.id,
                        vehicleType = vehicle.type,
                        currentMileage = vehicle.mileage,
                        onSave = {
                            viewModel.registerMaintenance(it)
                            viewModel.refreshMixedCounters()
                            showForm = false
                        },
                        onCancel = { showForm = false }
                    )
                }
            }
            if (showSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showSheet = false },
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = spacedBy(8.dp)) {
                        actions.forEach { (label, action) ->
                            Button(
                                onClick = {
                                    showSheet = false
                                    action()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = if (label == "Eliminar Vehículo")
                                    ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                else ButtonDefaults.buttonColors()
                            ) {
                                Text(label)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun getTrackingPermissions(): Array<String> = mutableListOf<String>().apply {
    add(Manifest.permission.ACCESS_FINE_LOCATION)
    add(Manifest.permission.ACCESS_COARSE_LOCATION)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) add(Manifest.permission.FOREGROUND_SERVICE_LOCATION)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
}.toTypedArray()

private fun startTrackingServiceAndNavigate(
    context: Context,
    vehicleId: Int,
    navController: NavHostController
) {
    Intent(context, SensorTrackingService::class.java).apply {
        putExtra("vehicleId", vehicleId)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            context.startForegroundService(this)
        else context.startService(this)
    }
    navController.navigate("tracking/$vehicleId")
}
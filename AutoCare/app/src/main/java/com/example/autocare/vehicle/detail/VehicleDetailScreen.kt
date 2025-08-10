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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.autocare.AppHeader
import com.example.autocare.sensor.SensorTrackingService
import com.example.autocare.util.getVehicleDisplayName
import com.example.autocare.vehicle.VehicleViewModel
import com.example.autocare.vehicle.maintenance.MaintenanceItemView
import com.example.autocare.vehicle.maintenance.ReviewStatus
import java.time.LocalDate

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
    val nextMaint by viewModel.nextMaint.collectAsState()
    LaunchedEffect(vehicle) { viewModel.refreshNextMaintenances() }

    val context = LocalContext.current
    var showSheet by remember { mutableStateOf(false) }
    var showForm by remember { mutableStateOf(false) }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        if (perms.values.all { it }) {
            startTrackingServiceAndNavigate(context, vehicle.id, navController)
        } else {
            Toast.makeText(context, "Permisos denegados", Toast.LENGTH_SHORT).show()
        }
    }

    val fuelEntries by viewModel
        .getFuelEntriesForVehicle(vehicleId)
        .collectAsState(initial = emptyList())

    val today = remember { LocalDate.now() }
    val thisVehicle = vehicles.firstOrNull { it.id == vehicleId }

    val actions = buildList<Pair<String, () -> Unit>> {
        add("Editar Vehículo" to { navController.navigate("form/${vehicle.id}") })
        add((if (showForm) "Cancelar" else "Añadir mantenimiento") to { showForm = !showForm })
        add("Ver repostajes"        to { navController.navigate("fuel/${vehicle.id}") })
        add("Añadir repostaje"      to { navController.navigate("fuel_add/${vehicle.id}") })
        add("Ver mantenimientos"    to { navController.navigate("maintenance/${vehicle.id}") })
        add((if (!SensorTrackingService.isRunning) "Iniciar seguimiento" else "Ir a seguimiento") to {
            if (!SensorTrackingService.isRunning) {
                permLauncher.launch(getTrackingPermissions())
            }
            navController.navigate("tracking/$vehicleId")
        })
        add("Sesiones de conducción" to { navController.navigate("sessions/${vehicle.id}") })
        add("Vincular Bluetooth" to { navController.navigate("bluetooth/${vehicle.id}") })
        add("Eliminar Vehículo" to {
            viewModel.deleteVehicle(vehicle)
            navController.popBackStack("list", false)
        })
    }

    val (monthTotal, yearTotal, allTotal) = remember(fuelEntries) {
        var m = 0f; var y = 0f; var a = 0f
        fuelEntries.forEach { e ->
            val date = e.date
            val cost = e.liters * e.pricePerLiter
            a += cost
            if (date.year == today.year) {
                y += cost
                if (date.month == today.month) m += cost
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
                        val displayKm = vehicle.mileage + extraKm
                        thisVehicle?.bluetoothMac?.let { mac ->
                            Text("Bluetooth vinculado: $mac", style = MaterialTheme.typography.bodyLarge)
                        }
                        Text("Marca: ${vehicle.brand}", style = MaterialTheme.typography.bodyLarge)
                        Text("Modelo: ${vehicle.model}", style = MaterialTheme.typography.bodyMedium)
                        Text("Tipo: ${vehicle.type}", style = MaterialTheme.typography.bodyMedium)
                        Text("Matrícula: ${vehicle.plateNumber}", style = MaterialTheme.typography.bodyMedium)
                        Text("Kilometraje: $displayKm km", style = MaterialTheme.typography.bodyMedium)
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
                        val items = nextMaint[vehicleId].orEmpty()
                        if (items.isEmpty()) {
                            Text("(calculando…)", style = MaterialTheme.typography.bodySmall)
                        } else {
                            items.forEach { nm ->
                                val bg = when (nm.status) {
                                    ReviewStatus.OVERDUE -> Color(0xFFF44336)
                                    ReviewStatus.SOON    -> Color(0xFFFFC107)
                                    ReviewStatus.OK      -> Color(0xFF4CAF50)
                                }

                                val kmText: String? = nm.leftKm?.let { k ->
                                    if (k >= 0) "$k km Restantes" else "Venció hace ${-k} km"
                                }
                                val daysText: String? = nm.leftDays?.let { d ->
                                    if (d >= 365) {
                                        val years = d / 365
                                        val days = d % 365
                                        "$years año${if (years > 1) "s" else ""} y $days días"
                                    } else {
                                        if (d >= 0) "$d días Restantes" else "Venció hace ${-d} días"
                                    }
                                }

                                Column(
                                    Modifier
                                        .fillMaxWidth()
                                        .height(84.dp) // misma altura para todas
                                        .border(1.dp, Color.Black, RoundedCornerShape(6.dp))
                                        .background(bg.copy(alpha = 0.18f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        nm.type,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 6.dp),
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        textAlign = TextAlign.Center
                                    )

                                    if (kmText != null && daysText != null) {
                                        // Dos columnas independientes
                                        Row(
                                            Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                kmText,
                                                modifier = Modifier.weight(1f),
                                                style = MaterialTheme.typography.bodyMedium,
                                                textAlign = TextAlign.Start
                                            )
                                            Text(
                                                daysText,
                                                modifier = Modifier.weight(1f),
                                                style = MaterialTheme.typography.bodyMedium,
                                                textAlign = TextAlign.End
                                            )
                                        }
                                    } else {
                                        // Sólo una métrica: la centramos
                                        Text(
                                            kmText ?: daysText ?: "-",
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 2.dp),
                                            style = MaterialTheme.typography.bodyMedium,
                                            textAlign = TextAlign.Center
                                        )
                                    }
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
                            viewModel.refreshNextMaintenances()
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
                            ) { Text(label) }
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
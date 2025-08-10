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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.autocare.AppHeader
import com.example.autocare.sensor.SensorTrackingService
import com.example.autocare.util.getVehicleDisplayName
import com.example.autocare.vehicle.VehicleViewModel
import com.example.autocare.vehicle.maintenance.MaintenanceItemView
import com.example.autocare.vehicle.maintenance.NextMaintenance
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
    val totalCost by remember(maintenances) { derivedStateOf { maintenances.sumOf { it.cost } } }

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

    val fuelEntries by viewModel.getFuelEntriesForVehicle(vehicleId).collectAsState(initial = emptyList())
    val today = remember { LocalDate.now() }
    val thisVehicle = vehicles.firstOrNull { it.id == vehicleId }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val actions = buildList<Pair<String, () -> Unit>> {
        add("Añadir repostaje" to { navController.navigate("fuel_add/${vehicle.id}") })
        add((if (showForm) "Cancelar" else "Añadir mantenimiento") to { showForm = !showForm })
        add("Ver mantenimientos" to { navController.navigate("maintenance/${vehicle.id}") })
        add((if (!SensorTrackingService.isRunning) "Iniciar seguimiento" else "Ir a seguimiento") to {
            if (!SensorTrackingService.isRunning) permLauncher.launch(getTrackingPermissions())
            navController.navigate("tracking/$vehicleId")
        })
        add("Sesiones de conducción" to { navController.navigate("sessions/${vehicle.id}") })
        add("Vincular Bluetooth" to { navController.navigate("bluetooth/${vehicle.id}") })
        add("Eliminar Vehículo" to {
            showDeleteConfirm = true
        })
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Confirmar eliminación") },
            text = { Text("¿Estás seguro de que quieres eliminar este vehículo? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteVehicle(vehicle)
                    navController.popBackStack("list", false)
                    showDeleteConfirm = false
                }) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancelar")
                }
            }
        )
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
    val extraKm = remember(sessions) { sessions.sumOf { it.distanceMeters.toDouble() / 1000 }.toInt() }

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
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    getVehicleDisplayName(vehicle),
                    style = MaterialTheme.typography.titleLarge.copy(textDecoration = TextDecoration.Underline),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Card(Modifier.fillMaxWidth().clickable { navController.navigate("form/${vehicle.id}") }, shape = RoundedCornerShape(12.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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

                Card(Modifier.fillMaxWidth().clickable { navController.navigate("fuel/${vehicle.id}") }, shape = RoundedCornerShape(12.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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

                Card(Modifier.fillMaxWidth().clickable { navController.navigate("maintenance/${vehicle.id}") }, shape = RoundedCornerShape(12.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Gasto total", style = MaterialTheme.typography.titleMedium)
                        Text("%.2f €".format(totalCost), style = MaterialTheme.typography.titleLarge)
                    }
                }

                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Próximos mantenimientos:", style = MaterialTheme.typography.titleMedium)
                        val items = nextMaint[vehicleId].orEmpty()
                        if (items.isEmpty()) {
                            Text("(calculando…)", style = MaterialTheme.typography.bodySmall)
                        } else {
                            items.forEach { nm ->
                                MaintenanceRow(nm)
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
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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

@Composable
private fun MaintenanceRow(nm: NextMaintenance) {
    val bg = statusBg(nm.status)
    val outline = statusOutline(nm.status)

    val rowHeight = 96.dp
    val shape = RoundedCornerShape(10.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(rowHeight)
            .background(bg, shape)
            .border(1.dp, outline, shape)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Text(nm.type, style = MaterialTheme.typography.titleMedium)
        }

        if (nm.leftKm != null && nm.leftDays != null) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        formatKm(nm.leftKm),
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        formatDays(nm.leftDays),
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Text(
                    nm.leftKm?.let { formatKm(it) } ?: nm.leftDays?.let { formatDays(it) } ?: "-",
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

private fun statusBg(s: ReviewStatus): Color = when (s) {
    ReviewStatus.OVERDUE -> Color(0x33F44336)
    ReviewStatus.SOON    -> Color(0x33FFC107)
    ReviewStatus.OK      -> Color(0x334CAF50)
}

private fun statusOutline(s: ReviewStatus): Color = when (s) {
    ReviewStatus.OVERDUE -> Color(0xFFB71C1C)
    ReviewStatus.SOON    -> Color(0xFFFFA000)
    ReviewStatus.OK      -> Color(0xFF2E7D32)
}

private fun formatKm(leftKm: Int): String =
    if (leftKm >= 0) "$leftKm km restantes" else "Venció hace ${-leftKm} km"

private fun formatDays(leftDays: Int): String =
    if (leftDays >= 0) {
        if (leftDays >= 365) {
            val years = leftDays / 365
            val days = leftDays % 365
            "$years año${if (years > 1) "s" else ""} y $days días restantes"
        } else {
            "$leftDays días restantes"
        }
    } else {
        "Venció hace ${-leftDays} días"
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(this)
        } else {
            context.startService(this)
        }
    }
    navController.navigate("tracking/$vehicleId")
}
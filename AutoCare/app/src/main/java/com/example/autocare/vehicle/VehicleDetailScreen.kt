package com.example.autocare.vehicle
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.autocare.AppHeader
import com.example.autocare.sensor.SensorTrackingService
import com.example.autocare.util.getVehicleDisplayName
import kotlinx.coroutines.launch

@SuppressLint("InlinedApi")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleDetailScreen(
    vehicleId: Int,
    viewModel: VehicleViewModel,
    navController: NavHostController
) {
    val vehicles by viewModel.vehicles.collectAsState(initial = emptyList())
    val vehicle = vehicles.firstOrNull { it.id == vehicleId }

    val maintenances by viewModel.getMaintenancesForVehicle(vehicleId)
        .collectAsState(initial = emptyList())
    val totalCost by remember(maintenances) {
        derivedStateOf { maintenances.sumOf { it.cost } }
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showSheet by remember { mutableStateOf(false) }
    var showForm by remember { mutableStateOf(false) }
    var predicted by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(vehicle) {
        vehicle?.let { predicted = viewModel.getPredictedMaintenance(it) }
    }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        if (perms.values.all { it } && vehicle != null) {
            startTrackingServiceAndNavigate(context, vehicle.id, navController)
        } else {
            Toast.makeText(context, "Permisos denegados", Toast.LENGTH_SHORT).show()
        }
    }
    val requestTrackingPermissions = {
        permLauncher.launch(getTrackingPermissions())
    }

    val actions = buildList<Pair<String, () -> Unit>> {
        vehicle?.let {
            add("Editar Vehículo" to { navController.navigate("form/${it.id}") })
            add((if (showForm) "Cancelar" else "Añadir mantenimiento") to { showForm = !showForm })
            add("Ver mantenimientos" to { navController.navigate("maintenance/${it.id}") })
            add("Iniciar seguimiento" to { requestTrackingPermissions() })
            add("Historial de sesiones" to { navController.navigate("sessions/${it.id}") })
            add("Eliminar Vehículo" to {
                viewModel.deleteVehicle(it)
                navController.popBackStack("list", false)
            })
        }
    }

    Scaffold(
        topBar = {
            AppHeader(
                title = "Vehículo",
                onBack = { navController.popBackStack() }
            )
        },
        floatingActionButton = {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                FloatingActionButton(onClick = { showSheet = true }) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Opciones")
                }
            }
        }
    ) { padding ->
        if (showSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSheet = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = spacedBy(8.dp)
                ) {
                    actions.forEach { (label, action) ->
                        Button(
                            onClick = {
                                scope.launch {
                                    showSheet = false
                                    action()
                                }
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

        Column(
            Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = spacedBy(16.dp)
        ) {
            vehicle?.let { v ->
                Text(
                    getVehicleDisplayName(v),
                    style = MaterialTheme.typography.titleLarge
                        .copy(textDecoration = TextDecoration.Underline),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Card(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = spacedBy(8.dp)
                    ) {
                        Text("Marca: ${v.brand}", style = MaterialTheme.typography.bodyLarge)
                        Text("Modelo: ${v.model}", style = MaterialTheme.typography.bodyMedium)
                        Text("Tipo: ${v.type}", style = MaterialTheme.typography.bodyMedium)
                        Text("Matrícula: ${v.plateNumber}", style = MaterialTheme.typography.bodyMedium)
                        Text("Kilometraje: ${v.mileage} km", style = MaterialTheme.typography.bodyMedium)
                    }
                }

                Card(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = spacedBy(4.dp)
                    ) {
                        Text("Gasto total", style = MaterialTheme.typography.titleMedium)
                        Text("%.2f €".format(totalCost), style = MaterialTheme.typography.titleLarge)
                    }
                }

                if (predicted.isNotEmpty()) {
                    Text("Predicciones:", style = MaterialTheme.typography.titleMedium)
                    Column(verticalArrangement = spacedBy(4.dp)) {
                        predicted.forEach { type ->
                            Card(Modifier.fillMaxWidth()) {
                                Text(type, Modifier.padding(12.dp), style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }

                if (showForm) {
                    MaintenanceItemView(
                        vehicleId = v.id,
                        onSave = {
                            viewModel.registerMaintenance(it)
                            showForm = false
                        },
                        onCancel = { showForm = false }
                    )
                }
            } ?: Text("Vehículo no encontrado", color = MaterialTheme.colorScheme.error)
        }
    }
}

private fun getTrackingPermissions(): Array<String> = mutableListOf<String>().apply {
    add(Manifest.permission.ACCESS_FINE_LOCATION)
    add(Manifest.permission.ACCESS_COARSE_LOCATION)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        add(Manifest.permission.FOREGROUND_SERVICE_LOCATION)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        add(Manifest.permission.POST_NOTIFICATIONS)
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
        else
            context.startService(this)
    }
    navController.navigate("tracking/$vehicleId")
}
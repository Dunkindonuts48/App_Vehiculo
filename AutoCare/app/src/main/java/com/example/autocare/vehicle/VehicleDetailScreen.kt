package com.example.autocare.vehicle

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import com.example.autocare.AppHeader
import com.example.autocare.sensor.SensorTrackingService
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleDetailScreen(vehicleId: Int, viewModel: VehicleViewModel, navController: NavHostController) {
    val vehicle = viewModel.vehicles.collectAsState().value.find { it.id == vehicleId }
    val maintenances = viewModel.getMaintenancesForVehicle(vehicleId).collectAsState(initial = emptyList())
    val totalCost = remember(maintenances.value) { maintenances.value.sumOf { it.cost } }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showForm by remember { mutableStateOf(false) }
    var showSheet by remember { mutableStateOf(false) }
    var predicted by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(vehicle) {
        if (vehicle != null) {
            predicted = viewModel.getPredictedMaintenance(vehicle)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            val granted = permissions.values.all { it }
            if (granted && vehicle != null) {
                startTrackingServiceAndNavigate(context, vehicle.id, navController)
            } else {
                Toast.makeText(context, "Permisos denegados", Toast.LENGTH_SHORT).show()
            }
        }
    )

    fun requestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.FOREGROUND_SERVICE_LOCATION)
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = bottomSheetState
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Button(onClick = {
                    coroutineScope.launch {
                        bottomSheetState.hide()
                        showSheet = false
                        navController.navigate("form/${vehicle?.id}")
                    }
                }, modifier = Modifier.fillMaxWidth()) {
                    Text("Editar Vehículo")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = {
                    coroutineScope.launch {
                        bottomSheetState.hide()
                        showSheet = false
                        showForm = !showForm
                    }
                }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (showForm) "Cancelar" else "Añadir mantenimiento")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = {
                    coroutineScope.launch {
                        bottomSheetState.hide()
                        showSheet = false
                        vehicle?.let { navController.navigate("maintenance/${it.id}") }
                    }
                }, modifier = Modifier.fillMaxWidth()) {
                    Text("Ver mantenimientos")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = {
                    coroutineScope.launch {
                        bottomSheetState.hide()
                        showSheet = false
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                        ) {
                            Toast.makeText(context, "Activa las notificaciones para el seguimiento", Toast.LENGTH_LONG).show()
                            return@launch
                        }
                        if (vehicle != null) startTrackingServiceAndNavigate(context, vehicle.id, navController)
                    }
                }, modifier = Modifier.fillMaxWidth()) {
                    Text("Iniciar seguimiento")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = {
                    coroutineScope.launch {
                        bottomSheetState.hide()
                        showSheet = false
                        vehicle?.let { navController.navigate("sessions/${it.id}") }
                    }
                }, modifier = Modifier.fillMaxWidth()) {
                    Text("Historial de sesiones")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = {
                    coroutineScope.launch {
                        bottomSheetState.hide()
                        showSheet = false
                        vehicle?.let {
                            viewModel.deleteVehicle(it)
                            navController.popBackStack("list", inclusive = false)
                        }
                    }
                }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error), modifier = Modifier.fillMaxWidth()) {
                    Text("Eliminar Vehículo")
                }
            }
        }
    }

    Scaffold(
        topBar = { AppHeader("Vehículo") },
        floatingActionButton = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.BottomCenter
            ) {
                FloatingActionButton(onClick = { showSheet = true }) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Mostrar opciones")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
                .fillMaxSize()
        ) {
            if (vehicle != null) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (vehicle.alias != null)
                        {
                            Text("${vehicle.alias}", style = MaterialTheme.typography.titleLarge.copy(textDecoration = TextDecoration.Underline))
                        }
                        else
                        {
                            Text("Vehicle Nº${vehicle.id}", style = MaterialTheme.typography.titleLarge.copy(textDecoration = TextDecoration.Underline))
                        }
                        Text("Marca: ${vehicle.brand}")
                        Text("Modelo: ${vehicle.model}")
                        Text("Tipo: ${vehicle.type}")
                        Text("Matrícula: ${vehicle.plateNumber}")
                        Text("Kilometraje: ${vehicle.mileage} km")
                        Text("Fecha de compra: ${vehicle.purchaseDate}")
                        Text("Última revisión: ${vehicle.lastMaintenanceDate}")
                        Text("Frecuencia (km): ${vehicle.maintenanceFrequencyKm}")
                        Text("Frecuencia (meses): ${vehicle.maintenanceFrequencyMonths}")

                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("💰 Gasto total en mantenimiento: %.2f €".format(totalCost))
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                if (predicted.isNotEmpty()) {
                    Text("🔔 Mantenimientos Recomendados", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    predicted.forEach { type ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                        ) {
                            Text(type, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                if (showForm && vehicle != null) {
                    MaintenanceItemView(
                        vehicleId = vehicle.id,
                        onSave = { maintenance ->
                            viewModel.registerMaintenance(maintenance)
                            showForm = false
                        },
                        onCancel = {
                            showForm = false
                        }
                    )
                }
            } else {
                Text("Vehículo no encontrado", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

fun startTrackingServiceAndNavigate(context: Context, vehicleId: Int, navController: NavHostController) {
    val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val hasForegroundService = ContextCompat.checkSelfPermission(context, Manifest.permission.FOREGROUND_SERVICE_LOCATION) == PackageManager.PERMISSION_GRANTED

    if (hasForegroundService && (hasFine || hasCoarse)) {
        val intent = Intent(context, SensorTrackingService::class.java).apply {
            putExtra("vehicleId", vehicleId)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
        navController.navigate("tracking/$vehicleId")
    } else {
        Toast.makeText(context, "Permisos de ubicación requeridos para iniciar el seguimiento", Toast.LENGTH_LONG).show()
    }
}

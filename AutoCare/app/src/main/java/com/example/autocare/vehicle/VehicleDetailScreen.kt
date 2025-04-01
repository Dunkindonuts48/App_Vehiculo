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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import com.example.autocare.sensor.SensorTrackingService

@Composable
fun VehicleDetailScreen(vehicleId: Int, viewModel: VehicleViewModel, navController: NavHostController) {
    val vehicle = viewModel.vehicles.collectAsState().value.find { it.id == vehicleId }
    val maintenances = viewModel.getMaintenancesForVehicle(vehicleId).collectAsState(initial = emptyList())
    val totalCost = remember(maintenances.value) { maintenances.value.sumOf { it.cost } }

    var showForm by remember { mutableStateOf(false) }

    var predicted by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(vehicle) {
        if (vehicle != null) {
            predicted = viewModel.getPredictedMaintenance(vehicle)
        }
    }

    val context = LocalContext.current
    val activity = context as? Activity

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            val granted = permissions.values.all { it }
            if (granted && vehicle != null) {
                startTrackingServiceAndNavigate(context, vehicle.id, navController)
            } else {
                Toast.makeText(context, "Permisos de ubicación denegados", Toast.LENGTH_SHORT).show()
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

    fun hasAllPermissions(): Boolean {
        val permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.FOREGROUND_SERVICE_LOCATION
        )
        return permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.systemBars
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).padding(16.dp).fillMaxSize()) {

            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                if (vehicle != null) {
                    Text("Detalle del Vehículo", style = MaterialTheme.typography.headlineSmall)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Marca: ${vehicle.brand}")
                    Text("Modelo: ${vehicle.model}")
                    Text("Tipo: ${vehicle.type}")
                    Text("Matrícula: ${vehicle.plateNumber}")
                    Text("Kilometraje: ${vehicle.mileage} km")
                    Text("Fecha de compra: ${vehicle.purchaseDate}")
                    Text("Última revisión: ${vehicle.lastMaintenanceDate}")
                    Text("Frecuencia (km): ${vehicle.maintenanceFrequencyKm}")
                    Text("Frecuencia (meses): ${vehicle.maintenanceFrequencyMonths}")

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("💰 Gasto total en mantenimiento: %.2f €".format(totalCost), style = MaterialTheme.typography.titleMedium)

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
                                Text("$type", modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                } else {
                    Text("Vehículo no encontrado", color = MaterialTheme.colorScheme.error)
                }
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                Button(onClick = {
                    if (vehicle != null) navController.navigate("form/${vehicle.id}")
                }, modifier = Modifier.fillMaxWidth()) {
                    Text("Editar Vehículo")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        if (vehicle != null) {
                            viewModel.deleteVehicle(vehicle)
                            navController.popBackStack("list", inclusive = false)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Eliminar Vehículo")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(onClick = { showForm = !showForm }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (showForm) "Cancelar" else "Añadir mantenimiento")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(onClick = {
                    if (vehicle != null) navController.navigate("maintenance/${vehicle.id}")
                }, modifier = Modifier.fillMaxWidth()) {
                    Text("Ver mantenimientos")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(onClick = {
                    if (hasAllPermissions()) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                            != PackageManager.PERMISSION_GRANTED
                        ) {
                            Toast.makeText(
                                context,
                                "Debes permitir notificaciones para iniciar el seguimiento",
                                Toast.LENGTH_LONG
                            ).show()
                            return@Button
                        }

                        if (vehicle != null) {
                            startTrackingServiceAndNavigate(context, vehicle.id, navController)
                        }
                    } else {
                        requestPermissions()
                    }
                }, modifier = Modifier.fillMaxWidth()) {
                    Text("Iniciar seguimiento predictivo")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(onClick = {
                    if (vehicle != null) navController.navigate("sessions/${vehicle.id}")
                }, modifier = Modifier.fillMaxWidth()) {
                    Text("Historial de sesiones")
                }

                Spacer(modifier = Modifier.height(12.dp))

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
            }
        }
    }
}

fun startTrackingServiceAndNavigate(context: Context, vehicleId: Int, navController: NavHostController) {
    val intent = Intent(context, SensorTrackingService::class.java).apply {
        putExtra("vehicleId", vehicleId)
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(intent)
    } else {
        context.startService(intent)
    }
    navController.navigate("tracking/$vehicleId")
}
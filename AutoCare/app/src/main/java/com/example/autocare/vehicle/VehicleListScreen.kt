// VehicleListScreen.kt (reemplazo de TooltipBox por texto visible para mantenimiento urgente)
package com.example.autocare.vehicle

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import com.example.autocare.AppHeader

@Composable
fun VehicleListScreen(navController: NavHostController, viewModel: VehicleViewModel) {
    val vehicles = viewModel.vehicles.collectAsState()
    val urgentVehicles = remember { mutableStateOf<Set<Int>>(emptySet()) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            val granted = permissions.values.all { it }
            if (!granted) {
                Toast.makeText(context, "Permisos requeridos no concedidos", Toast.LENGTH_SHORT).show()
            }
        }
    )

    fun requestPermissionsIfNeeded() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.FOREGROUND_SERVICE_LOCATION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        val notGranted = permissions.any {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (notGranted) {
            permissionLauncher.launch(permissions.toTypedArray())
        }
    }

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            val urgent = viewModel.getVehiclesWithUrgentMaintenanceSuspended()
            urgentVehicles.value = urgent.map { it.id }.toSet()
        }
        requestPermissionsIfNeeded()
    }

    Scaffold(
        topBar = {
            AppHeader("Vehículos Registrados") },
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.systemBars
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).padding(16.dp)) {
            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = { navController.navigate("form") }) {
                Text("Añadir Vehículo")
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn {
                items(vehicles.value) { vehicle ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .clickable { navController.navigate("detail/${vehicle.id}") }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                if (vehicle.alias != null) {
                                    Text(vehicle.alias, style = MaterialTheme.typography.titleLarge.copy(textDecoration = TextDecoration.Underline))
                                    Text("Marca: ${vehicle.brand}", style = MaterialTheme.typography.bodyLarge)
                                    Text("Modelo: ${vehicle.model}")
                                    Text("Matricula: ${vehicle.plateNumber}")
                                    Text("Matricula: ${vehicle.mileage} km")
                                } else {
                                    Text("Vehicle Nº${vehicle.id}", style = MaterialTheme.typography.titleLarge.copy(textDecoration = TextDecoration.Underline))
                                    Text("Marca: ${vehicle.brand}", style = MaterialTheme.typography.bodyLarge)
                                    Text("Modelo: ${vehicle.model}")
                                    Text("Matrícula: ${vehicle.plateNumber}")
                                    Text("Kilometraje: ${vehicle.mileage} km")
                                }
                            }

                            if (urgentVehicles.value.contains(vehicle.id)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .background(Color.Red, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "¡Mantenimiento urgente!",
                                        color = Color.Red,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
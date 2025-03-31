package com.example.autocare

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.autocare.vehicle.Vehicle
import com.example.autocare.vehicle.VehicleViewModel
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(viewModel: VehicleViewModel) {
    var urgentVehicles by remember { mutableStateOf<List<Vehicle>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            urgentVehicles = viewModel.getVehiclesWithUrgentMaintenanceSuspended()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Bienvenido a AutoCare", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))
        if (urgentVehicles.isNotEmpty()) {
            Text("🚨 Vehículos con mantenimiento urgente:")
            urgentVehicles.forEach {
                Text("• ${it.brand} ${it.model} (${it.plateNumber})")
            }
        } else {
            Text("No hay mantenimientos urgentes.")
        }
    }
}
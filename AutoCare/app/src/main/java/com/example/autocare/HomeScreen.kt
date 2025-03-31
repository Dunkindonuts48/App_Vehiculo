// HomeScreen.kt (corregido: llamada correcta a getVehiclesWithUrgentMaintenance)
package com.example.autocare

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.autocare.vehicle.VehicleViewModel

@Composable
fun HomeScreen(viewModel: VehicleViewModel) {
    val urgentVehicles = viewModel.getVehiclesWithUrgentMaintenance()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
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
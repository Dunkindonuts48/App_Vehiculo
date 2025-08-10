// HomeScreen.kt
package com.example.autocare

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.autocare.vehicle.VehicleViewModel
import com.example.autocare.util.getVehicleDisplayName
import com.example.autocare.vehicle.maintenance.ReviewStatus

@Composable
fun HomeScreen(viewModel: VehicleViewModel) {
    // Nuevo: usamos nextMaint y la lista de vehículos
    val nextMaintByVehicle by viewModel.nextMaint.collectAsState()
    val vehicles by viewModel.vehicles.collectAsState(initial = emptyList())

    // Lista de vehículos con mantenimiento urgente
    val urgentVehicles by remember(nextMaintByVehicle, vehicles) {
        mutableStateOf(
            vehicles
                .filter { v ->
                    val items = nextMaintByVehicle[v.id].orEmpty()
                    // Define qué es "urgente"
                    items.any { nm ->
                        // Urgente si está vencido o muy cercano
                        nm.status == ReviewStatus.OVERDUE ||
                                (nm.status == ReviewStatus.SOON &&
                                        ((nm.leftDays ?: Int.MAX_VALUE) <= 7 ||
                                                (nm.leftKm   ?: Int.MAX_VALUE) <= 1000))
                    }
                }
                .map { v -> getVehicleDisplayName(v) }
        )
    }

    Scaffold(
        topBar = { AppHeader("Inicio") }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Bienvenido a AutoCare",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(Modifier.height(24.dp))

            if (urgentVehicles.isNotEmpty()) {
                Text("🚨 Vehículos con mantenimiento urgente:")
                urgentVehicles.forEach { displayName ->
                    Text(
                        "• $displayName",
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else {
                Text("No hay mantenimientos urgentes.")
            }
        }
    }
}
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

@Composable
fun HomeScreen(viewModel: VehicleViewModel) {
    val mixedCounters by viewModel.mixedCounters.collectAsState()
    var urgentVehicles by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(mixedCounters) {
        urgentVehicles = mixedCounters
            .filter { (_, counters) ->
                counters.values.any { info ->
                    info.startsWith("0 ") &&
                            (info.endsWith("días restantes") || info.endsWith("km restantes"))
                }
            }
            .mapNotNull { (vehicleId, _) ->
                viewModel.vehicles.value.firstOrNull { it.id == vehicleId }
            }
            .map { getVehicleDisplayName(it) }
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
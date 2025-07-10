package com.example.autocare
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.autocare.vehicle.Vehicle
import com.example.autocare.vehicle.VehicleViewModel
import com.example.autocare.util.getVehicleDisplayName
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(viewModel: VehicleViewModel) {
    var urgentVehicles by remember { mutableStateOf<List<Vehicle>>(emptyList()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            urgentVehicles = viewModel.getVehiclesWithUrgentMaintenanceSuspended()
        }
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
                urgentVehicles.forEach { v ->
                    Text(
                        "• ${getVehicleDisplayName(v)}",
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
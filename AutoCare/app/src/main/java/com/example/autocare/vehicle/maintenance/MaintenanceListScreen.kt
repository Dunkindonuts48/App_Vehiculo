package com.example.autocare.vehicle.maintenance

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.autocare.AppHeader
import com.example.autocare.vehicle.VehicleViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaintenanceListScreen(
    vehicleId: Int,
    viewModel: VehicleViewModel,
    navController: NavHostController
) {
    val maintenances = viewModel.getMaintenancesForVehicle(vehicleId)
        .collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            AppHeader(
                title = "Historial de Mantenimientos",
                onBack = { navController.popBackStack() }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("stats/$vehicleId") }) {
                Icon(Icons.Filled.Insights, contentDescription = "Ver estadísticas")
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
            if (maintenances.value.isEmpty()) {
                Text("Este vehículo no tiene mantenimientos registrados.")
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(maintenances.value) { m ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text("Tipo: ${m.type}", style = MaterialTheme.typography.bodyLarge)
                                Text("Fecha: ${m.date}")
                                Text("Coste: ${m.cost} €")
                                Text("Kilometraje: ${m.mileageAtMaintenance} km")
                                Spacer(Modifier.height(8.dp))
                                Button(
                                    onClick = { viewModel.deleteMaintenance(m) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("Eliminar") }
                            }
                        }
                    }
                }
            }
        }
    }
}
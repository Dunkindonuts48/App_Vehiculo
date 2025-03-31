// VehicleListScreen.kt (reemplazo de TooltipBox por texto visible para mantenimiento urgente)
package com.example.autocare.vehicle

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController

@Composable
fun VehicleListScreen(navController: NavHostController, viewModel: VehicleViewModel) {
    val vehicles = viewModel.vehicles.collectAsState()
    val urgentVehicles = viewModel.getVehiclesWithUrgentMaintenance().map { it.id }.toSet()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.systemBars
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).padding(16.dp)) {
            Text("Vehículos Registrados", style = MaterialTheme.typography.headlineSmall)
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
                                Text("Marca: ${vehicle.brand}", style = MaterialTheme.typography.bodyLarge)
                                Text("Modelo: ${vehicle.model}")
                                Text("Matrícula: ${vehicle.plateNumber}")
                                Text("Kilometraje: ${vehicle.mileage} km")
                            }

                            if (urgentVehicles.contains(vehicle.id)) {
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
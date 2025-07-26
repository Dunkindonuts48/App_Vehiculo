package com.example.autocare.vehicle.fuel

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.autocare.AppHeader
import com.example.autocare.vehicle.VehicleViewModel

@Composable
fun FuelListScreen(
    vehicleId: Int,
    viewModel: VehicleViewModel,
    navController: NavHostController
) {
    val entries by viewModel.getFuelEntriesForVehicle(vehicleId).collectAsState(initial = emptyList())
    val totalCost by viewModel.getTotalFuelCost(vehicleId).collectAsState(initial = 0f)

    Scaffold(
        topBar = { AppHeader("Repostajes", onBack = { navController.popBackStack() }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("fuel_add/$vehicleId") }) {
                Icon(Icons.Default.LocalGasStation, contentDescription = "Añadir repostaje")
            }
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text("Gasto combustible: %.2f €".format(totalCost), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            LazyColumn {
                items(entries) { e ->
                    Card(Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text("Fecha: ${e.date}")
                            Text("Km: ${e.mileage}")
                            Text("Litros: ${e.liters}")
                            Text("Precio/litro: %.2f €".format(e.pricePerLiter))
                            Text("Total: %.2f €".format(e.liters * e.pricePerLiter))
                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = { viewModel.deleteFuelEntry(e); },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Eliminar repostaje")
                            }
                        }
                    }
                }
            }
        }
    }
}

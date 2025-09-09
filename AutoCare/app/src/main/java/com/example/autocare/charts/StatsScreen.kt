package com.example.autocare.charts

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.autocare.AppHeader
import com.example.autocare.vehicle.VehicleViewModel
import kotlinx.coroutines.launch
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    vehicleId: Int,
    viewModel: VehicleViewModel,
    navController: NavHostController
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val vehicles by viewModel.vehicles.collectAsState(initial = emptyList())
    val vehicle = vehicles.firstOrNull { it.id == vehicleId }

    val sessions by viewModel.getDrivingSessionsForVehicle(vehicleId)
        .collectAsState(initial = emptyList())

    val fuelEntries by viewModel.getFuelEntriesForVehicle(vehicleId)
        .collectAsState(initial = emptyList())

    val maintenances by viewModel.getMaintenancesForVehicle(vehicleId)
        .collectAsState(initial = emptyList())

    val maintItems = maintenances.map {
        MaintenanceItem(
            epochMillis = it.date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            cost = it.cost.toFloat(),
            category = it.type
        )
    }

    Scaffold(
        topBar = {
            AppHeader(
                title = "Estadísticas",
                onBack = { navController.popBackStack() }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    scope.launch {
                        val v = vehicle
                        if (v != null) {
                            viewModel.addRandomDemoData(
                                vehicleId = vehicleId,
                                baseMileage = v.mileage,
                                firstYear = 2020
                            )
                            snackbarHostState.showSnackbar("Datos demo añadidos 👌")
                        } else {
                            snackbarHostState.showSnackbar("Vehículo no encontrado")
                        }
                    }
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Añadir datos demo")
            }
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            KmOverTimeCard(
                sessions = sessions,
                title = "Kilómetros recorridos"
            )
            FuelCostOverTimeCard(
                entries = fuelEntries,
                title = "Gastos en combustible"
            )
            FuelPriceOverTimeCard(
                entries = fuelEntries,
                title = "Precio del combustible"
            )
            MaintenanceCostOverTimeCard(
                items = maintItems,
                title = "Gasto de mantenimiento"
            )
            MaintenanceByCategoryCard(
                items = maintItems,
                title = "Gasto por categoría"
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}
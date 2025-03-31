package com.example.autocare.sensor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.autocare.vehicle.VehicleViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrivingSessionListScreen(vehicleId: Int, viewModel: VehicleViewModel, navController: NavHostController) {
    val sessions = viewModel.getDrivingSessionsForVehicle(vehicleId).collectAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Sesiones de Conducción") })
        },
        content = { padding ->
            Column(modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize()) {
                if (sessions.value.isEmpty()) {
                    Text("No hay sesiones registradas para este vehículo.")
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(sessions.value) { session ->
                            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Inicio: ${session.startTime.toReadableTime()}")
                                    Text("Fin: ${session.endTime.toReadableTime()}")
                                    Text("Velocidad Máxima: ${"%.1f".format(session.maxSpeed * 3.6)} km/h")
                                    Text("Velocidad Media: ${"%.1f".format(session.averageSpeed * 3.6)} km/h")
                                    Text("Acelerones: ${session.accelerations}")
                                    Text("Frenazos: ${session.brakings}")

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Button(onClick = {
                                        coroutineScope.launch {
                                            viewModel.deleteDrivingSession(session)
                                        }
                                    }, modifier = Modifier.fillMaxWidth()) {
                                        Text("Eliminar esta sesión")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    )
}

fun Long.toReadableTime(): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(this))
}

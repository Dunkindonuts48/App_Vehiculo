package com.example.autocare.sensor

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.autocare.vehicle.VehicleDatabase
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackingScreen(vehicleId: Int, navController: NavHostController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Escuchar cambios en SensorData en tiempo real
    val sensorDataList = VehicleDatabase.getDatabase(context)
        .sensorDataDao()
        .getByVehicle(vehicleId)
        .collectAsState(initial = emptyList())

    val latest = sensorDataList.value.firstOrNull()
    val currentSpeedKmH = latest?.speed?.times(3.6f) ?: 0f

    var aggressiveness by remember { mutableStateOf(0f) }

    // Recalcular agresividad cuando los datos cambien
    LaunchedEffect(sensorDataList.value) {
        coroutineScope.launch {
            aggressiveness = SensorTrackingService.calculateAggressiveness(context, vehicleId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Seguimiento Predictivo") })
        },
        content = { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
                    .fillMaxSize()
            ) {
                Text(
                    "Índice de agresividad de conducción:",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "%.1f / 100".format(aggressiveness),
                    style = MaterialTheme.typography.headlineMedium
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text("🚗 Velocidad actual:", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "%.1f km/h".format(currentSpeedKmH),
                    style = MaterialTheme.typography.headlineMedium
                )

                Spacer(modifier = Modifier.height(24.dp))

                if (aggressiveness > 70f) {
                    Text(
                        "⚠️ Conducción agresiva detectada. Se recomienda revisar el vehículo.",
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Button(
                    onClick = {
                        val intent = Intent(context, SensorTrackingService::class.java)
                        context.stopService(intent)
                        navController.popBackStack()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Detener seguimiento")
                }
            }
        }
    )
}
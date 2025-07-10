package com.example.autocare.sensor

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.autocare.AppHeader
import com.example.autocare.vehicle.VehicleDatabase
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackingScreen(
    vehicleId: Int,
    navController: NavHostController
) {
    val context = LocalContext.current
    val sensorDataList = VehicleDatabase.getDatabase(context)
        .sensorDataDao()
        .getByVehicle(vehicleId)
        .collectAsState(initial = emptyList())

    val latest = sensorDataList.value.firstOrNull()
    val speedKmH = (latest?.speed ?: 0f) * 3.6f
    var aggressiveness by remember { mutableStateOf(0f) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(sensorDataList.value) {
        scope.launch {
            aggressiveness = SensorTrackingService.calculateAggressiveness(context, vehicleId)
        }
    }

    Scaffold(
        topBar = {
            AppHeader(
                title = "Seguimiento Predictivo",
                onBack = { navController.popBackStack() }
            )
        },
        content = { padding ->
            Column(
                Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Índice de agresividad:", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text(
                    "%.1f / 100".format(aggressiveness),
                    style = MaterialTheme.typography.headlineMedium
                )

                Spacer(Modifier.height(24.dp))

                // Velocímetro gráfico
                Canvas(modifier = Modifier.size(200.dp)) {
                    // Fondo de arco
                    drawArc(
                        color = Color.DarkGray,
                        startAngle = 135f,
                        sweepAngle = 270f,
                        useCenter = false,
                        style = Stroke(width = 12f)
                    )
                    // Sección "llenada" en verde
                    val sweep = (speedKmH / 200f * 270f).coerceIn(0f, 270f)
                    drawArc(
                        color = Color.Green,
                        startAngle = 135f,
                        sweepAngle = sweep,
                        useCenter = false,
                        style = Stroke(width = 12f)
                    )
                    // Aguja
                    val center = size.center
                    val angle = Math.toRadians((135 + sweep).toDouble())
                    val radius = size.minDimension / 2
                    val end = Offset(
                        x = center.x + radius * cos(angle).toFloat(),
                        y = center.y + radius * sin(angle).toFloat()
                    )
                    drawLine(color = Color.Red, start = center, end = end, strokeWidth = 4f)
                }

                Spacer(Modifier.height(16.dp))
                Text("🚗 %.1f km/h".format(speedKmH), style = MaterialTheme.typography.headlineMedium)

                Spacer(Modifier.height(24.dp))
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
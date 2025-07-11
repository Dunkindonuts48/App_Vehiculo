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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.autocare.AppHeader
import com.example.autocare.vehicle.VehicleDatabase
import kotlinx.coroutines.launch
import androidx.compose.ui.geometry.center
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackingScreenTest2(
    vehicleId: Int,
    navController: NavHostController
) {
    val context = LocalContext.current

    // 1) Leer datos
    val sensorDataList by VehicleDatabase
        .getDatabase(context)
        .sensorDataDao()
        .getByVehicle(vehicleId)
        .collectAsState(initial = emptyList())

    val latest = sensorDataList.firstOrNull()
    val speedKmH = (latest?.speed ?: 0f) * 3.6f

    // 2) Calcular agresividad por Hz
    var aggressiveness by remember { mutableStateOf(0f) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(sensorDataList) {
        scope.launch {
            aggressiveness = SensorTrackingServiceTestHz
                .calculateAggressivenessHz(context, vehicleId)
        }
    }

    Scaffold(
        topBar = {
            AppHeader(
                title = "Seguimiento Hz",
                onBack = { navController.popBackStack() }
            )
        },
        content = { padding ->
            Column(
                Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // --- Nuevo gauge lineal para agresividad ---
                Text("Índice de agresividad (Hz):", style = MaterialTheme.typography.titleMedium)
                LinearGauge(
                    value = aggressiveness,
                    modifier = Modifier.fillMaxWidth(),
                    height = 20.dp,
                    maxValue = 100f,
                    gradient = listOf(Color(0xFF4CAF50), Color(0xFFFFC107), Color(0xFFF44336))
                )
                Text("%.1f / 100".format(aggressiveness), style = MaterialTheme.typography.bodyLarge)
                
                Text("Velocidad:", style = MaterialTheme.typography.titleMedium)
                Canvas(Modifier.size(200.dp)) {
                    // fondo de arco
                    drawArc(
                        color = Color.DarkGray,
                        startAngle = 135f,
                        sweepAngle = 270f,
                        useCenter = false,
                        style = Stroke(width = 12f)
                    )
                    // sección verde proporcional
                    val sweepSpd = (speedKmH / 200f * 270f).coerceIn(0f, 270f)
                    drawArc(
                        color = Color.Green,
                        startAngle = 135f,
                        sweepAngle = sweepSpd,
                        useCenter = false,
                        style = Stroke(width = 12f)
                    )
                    // aguja
                    val center = size.center
                    val angleSpd = Math.toRadians((135 + sweepSpd).toDouble())
                    val radius = size.minDimension / 2
                    val end = Offset(
                        x = center.x + radius * cos(angleSpd).toFloat(),
                        y = center.y + radius * sin(angleSpd).toFloat()
                    )
                    drawLine(color = Color.Red, start = center, end = end, strokeWidth = 4f)
                }
                Text("🚗 %.1f km/h".format(speedKmH), style = MaterialTheme.typography.bodyLarge)

                Spacer(Modifier.weight(1f))

                Button(
                    onClick = {
                        Intent(context, SensorTrackingServiceTestHz::class.java).also {
                            context.stopService(it)
                        }
                        navController.popBackStack()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Detener Seguimiento")
                }
            }
        }
    )
}

/**
 * Gauge lineal con degradado y marcador vertical.
 */
@Composable
fun LinearGauge(
    value: Float,
    modifier: Modifier = Modifier,
    height: Dp = 16.dp,
    maxValue: Float = 100f,
    gradient: List<Color> = listOf(Color.Green, Color.Yellow, Color.Red)
) {
    Box(modifier = modifier.height(height)) {
        // fondo gris
        Canvas(Modifier.matchParentSize()) {
            drawRect(color = Color.DarkGray)
        }
        // degradado proporcional
        Canvas(Modifier.matchParentSize()) {
            val fraction = (value.coerceIn(0f, maxValue) / maxValue)
            val width = size.width * fraction
            drawRect(
                brush = Brush.horizontalGradient(gradient),
                size = androidx.compose.ui.geometry.Size(width, size.height)
            )
        }
        // marcador
        Canvas(Modifier.matchParentSize()) {
            val x = size.width * (value.coerceIn(0f, maxValue) / maxValue)
            drawLine(
                color = Color.Black,
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = 2.dp.toPx()
            )
        }
    }
}
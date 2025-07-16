package com.example.autocare.sensor

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.BroadcastReceiver
import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
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
    val scope = rememberCoroutineScope()

    val sensorDataList by VehicleDatabase
        .getDatabase(context)
        .sensorDataDao()
        .getByVehicle(vehicleId)
        .collectAsState(initial = emptyList())

    val latest = sensorDataList.firstOrNull()
    val speedKmH = (latest?.speed ?: 0f) * 3.6f

    var aggressiveness by remember { mutableStateOf(0f) }
    var accelerations by remember { mutableStateOf(0) }
    var brakings by remember { mutableStateOf(0) }

    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == "com.example.autocare.UPDATE_EVENTS") {
                    accelerations = intent.getIntExtra("accelerations", 0)
                    brakings = intent.getIntExtra("brakings", 0)
                }
            }
        }
        val filter = IntentFilter("com.example.autocare.UPDATE_EVENTS")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            ContextCompat.registerReceiver(
                context,
                receiver,
                filter,
                ContextCompat.RECEIVER_EXPORTED
            )
        }
        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    LaunchedEffect(sensorDataList) {
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
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Índice de agresividad:", style = MaterialTheme.typography.titleMedium)
            LinearGauge(
                value = aggressiveness,
                modifier = Modifier.fillMaxWidth(),
                height = 20.dp,
                maxValue = 100f,
                gradient = listOf(
                    Color(0xFF4CAF50),
                    Color(0xFFFFC107),
                    Color(0xFFF44336)
                )
            )
            Text("%.1f / 100".format(aggressiveness),
                style = MaterialTheme.typography.bodyLarge)

            Text("Velocidad:", style = MaterialTheme.typography.titleMedium)

            Canvas(Modifier.size(200.dp)) {
                val strokeW = 12f
                val center = size.center
                val radius = size.minDimension / 2 - strokeW
                drawArc(
                    color = Color.DarkGray,
                    startAngle = 135f,
                    sweepAngle = 270f,
                    useCenter = false,
                    style = Stroke(width = strokeW)
                )
                val sweepSpd = (speedKmH / 200f * 270f).coerceIn(0f, 270f)
                drawArc(
                    color = Color.Green,
                    startAngle = 135f,
                    sweepAngle = sweepSpd,
                    useCenter = false,
                    style = Stroke(width = strokeW)
                )
                val ang = Math.toRadians((135 + sweepSpd).toDouble())
                val endPoint = Offset(
                    x = center.x + radius * cos(ang).toFloat(),
                    y = center.y + radius * sin(ang).toFloat()
                )
                drawLine(
                    color = Color.Red,
                    start = center,
                    end = endPoint,
                    strokeWidth = 4f
                )

                val paintText = android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = 12.sp.toPx()
                    textAlign = android.graphics.Paint.Align.CENTER
                }
                for (v in 0..200 step 20) {
                    val angDeg = 135f + (v / 200f * 270f)
                    val rad = Math.toRadians(angDeg.toDouble())
                    val innerR = radius - strokeW / 2
                    val outerR = innerR - if (v % 40 == 0) 16f else 8f
                    val p1 = Offset(
                        x = center.x + innerR * cos(rad).toFloat(),
                        y = center.y + innerR * sin(rad).toFloat()
                    )
                    val p2 = Offset(
                        x = center.x + outerR * cos(rad).toFloat(),
                        y = center.y + outerR * sin(rad).toFloat()
                    )
                    drawLine(
                        color = Color.White,
                        start = p1,
                        end = p2,
                        strokeWidth = 2f
                    )
                    if (v % 40 == 0) {
                        val txtR = outerR - 12f
                        val tx = center.x + txtR * cos(rad).toFloat()
                        val ty = center.y + txtR * sin(rad).toFloat() + paintText.textSize / 2
                        drawContext.canvas.nativeCanvas.drawText("$v", tx, ty, paintText)
                    }
                }
            }

            Text("%.1f km/h".format(speedKmH), style = MaterialTheme.typography.headlineMedium)

            // 🔻 Aquí se muestran los eventos detectados en tiempo real
            Text("🚀 Acelerones: $accelerations", style = MaterialTheme.typography.bodyLarge)
            Text("🛑 Frenazos: $brakings", style = MaterialTheme.typography.bodyLarge)

            Spacer(Modifier.weight(1f))

            Button(
                onClick = {
                    Intent(context, SensorTrackingService::class.java).also {
                        context.stopService(it)
                    }
                    navController.popBackStack()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Detener seguimiento")
            }
        }
    }
}

@Composable
fun LinearGauge(
    value: Float,
    modifier: Modifier = Modifier,
    height: Dp = 16.dp,
    maxValue: Float = 100f,
    gradient: List<Color> = listOf(Color.Green, Color.Yellow, Color.Red)
) {
    Box(modifier = modifier.height(height)) {
        Canvas(Modifier.matchParentSize()) {
            drawRect(color = Color.DarkGray)
        }
        Canvas(Modifier.matchParentSize()) {
            val fraction = (value.coerceIn(0f, maxValue) / maxValue)
            val width = size.width * fraction
            drawRect(
                brush = Brush.horizontalGradient(gradient),
                size = androidx.compose.ui.geometry.Size(width, size.height)
            )
        }
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
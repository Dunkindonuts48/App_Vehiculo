// TrackingScreenTest2.kt
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackingScreenTest2(
    vehicleId: Int,
    navController: NavHostController
) {
    val context = LocalContext.current
    var aggressiveness by remember { mutableStateOf(0f) }
    val scope = rememberCoroutineScope()

    // launch once to compute
    LaunchedEffect(Unit) {
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
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Índice de agresividad (Hz):", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text("%.1f / 100".format(aggressiveness),
                    style = MaterialTheme.typography.headlineMedium)

                Spacer(Modifier.height(24.dp))
                // simple gauge
                Canvas(Modifier.size(180.dp)) {
                    // background arc
                    drawArc(
                        color = Color.LightGray,
                        startAngle = 135f, sweepAngle = 270f,
                        useCenter = false, style = Stroke(12f)
                    )
                    // filled portion
                    val sweep = (aggressiveness/100f * 270f).coerceIn(0f,270f)
                    drawArc(
                        color = Color.Cyan,
                        startAngle = 135f, sweepAngle = sweep,
                        useCenter = false, style = Stroke(12f)
                    )
                    // needle
                    val c = size.center
                    val angle = Math.toRadians((135+sweep).toDouble())
                    val r = size.minDimension/2
                    val end = Offset(
                        x = c.x + (r-6)*kotlin.math.cos(angle).toFloat(),
                        y = c.y + (r-6)*kotlin.math.sin(angle).toFloat()
                    )
                    drawLine(Color.Blue, c, end, strokeWidth = 4f)
                }

                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = {
                        // stop the Hz service if running
                        val stop = Intent(context, SensorTrackingServiceTestHz::class.java)
                        context.stopService(stop)
                        navController.popBackStack()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Detener Hz")
                }
            }
        }
    )
}

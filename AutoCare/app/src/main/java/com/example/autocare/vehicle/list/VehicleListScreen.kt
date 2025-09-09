package com.example.autocare.vehicle.list

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import com.example.autocare.AppHeader
import com.example.autocare.util.getVehicleDisplayName
import com.example.autocare.vehicle.maintenance.ReviewStatus
import com.example.autocare.vehicle.VehicleViewModel

@Composable
fun VehicleListScreen(navController: NavHostController, viewModel: VehicleViewModel) {
    val vehicles by viewModel.vehicles.collectAsState(initial = emptyList())
    val nextMaintByVehicle by viewModel.nextMaint.collectAsState()
    val context = LocalContext.current
    val urgentSet: Set<Int> = remember(nextMaintByVehicle) {
        nextMaintByVehicle
            .filter { (_, list) ->
                list.any { nm ->
                    nm.status == ReviewStatus.OVERDUE ||
                            (nm.status == ReviewStatus.SOON &&
                                    ((nm.leftDays ?: Int.MAX_VALUE) <= 7 ||
                                            (nm.leftKm   ?: Int.MAX_VALUE) <= 1000))
                }
            }
            .keys
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { perms ->
            if (!perms.values.all { it }) {
                Toast.makeText(context, "Permisos no concedidos", Toast.LENGTH_SHORT).show()
            }
        }
    )

    LaunchedEffect(Unit) {
        val perms = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                add(Manifest.permission.FOREGROUND_SERVICE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                add(Manifest.permission.POST_NOTIFICATIONS)
        }
        val needed = perms.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (needed.isNotEmpty()) {
            permissionLauncher.launch(needed.toTypedArray())
        }
    }

    Scaffold(
        topBar = { AppHeader("Vehículos Registrados") },
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.systemBars
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(16.dp)
        ) {
            Button(
                onClick = { navController.navigate("register/type") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Añadir Vehículo")
            }
            Spacer(Modifier.height(16.dp))
            LazyColumn {
                items(vehicles) { vehicle ->
                    Card(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .clickable { navController.navigate("detail/${vehicle.id}") }
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                getVehicleDisplayName(vehicle),
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.weight(1f))
                            if (vehicle.id in urgentSet) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        Modifier
                                            .size(12.dp)
                                            .background(Color.Red, CircleShape)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "¡Urgente!",
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
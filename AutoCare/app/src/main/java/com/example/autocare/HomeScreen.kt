package com.example.autocare

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.autocare.vehicle.VehicleViewModel
import com.example.autocare.util.getVehicleDisplayName
import com.example.autocare.vehicle.maintenance.ReviewStatus

@Composable
fun HomeScreen(viewModel: VehicleViewModel) {
    Scaffold(topBar = { AppHeader("Inicio") }) {
        padding -> Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Bienvenido a AutoCare", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(24.dp))
        }
    }
}
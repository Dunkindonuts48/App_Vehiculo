package com.example.autocare.vehicle.registration.steps

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.autocare.AppHeader
import com.example.autocare.util.BrandModelRepository
import com.example.autocare.vehicle.VehicleRegistrationViewModel

@Composable
fun StepRegisterBrandScreen(
    vm: VehicleRegistrationViewModel,
    nav: NavHostController
) {
    val brands = BrandModelRepository(LocalContext.current)
        .getBrands()
        .map { it.name }

    Scaffold(
        topBar = { AppHeader("2/6: Marca y Modelo", onBack = { nav.popBackStack() }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(brands) { b ->
                    val isSelected = vm.brand == b
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected)
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            else Color.Transparent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { vm.brand = b }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Text(
                                text = b,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f)
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Seleccionado",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = vm.model,
                onValueChange = { vm.model = it },
                label = { Text("Modelo (opcional)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            Button(
                enabled = vm.canGoToPlate(),
                onClick = { nav.navigate("register/plate") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Siguiente")
            }
        }
    }
}
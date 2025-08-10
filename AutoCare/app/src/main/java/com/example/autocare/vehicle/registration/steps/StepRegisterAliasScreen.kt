package com.example.autocare.vehicle.registration.steps

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.autocare.AppHeader
import com.example.autocare.vehicle.VehicleRegistrationViewModel
import com.example.autocare.vehicle.VehicleViewModel

@Composable
fun StepRegisterAliasScreen(
    vm: VehicleRegistrationViewModel,
    mainVm: VehicleViewModel,
    nav: NavHostController
) {
    Scaffold(
        topBar = { AppHeader("6/6: Alias (opcional)", onBack = { nav.popBackStack() }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = vm.alias,
                onValueChange = { vm.alias = it },
                label = { Text("Alias") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.weight(1f))
            Button(
                onClick = {
                    mainVm.registerVehicleWithRevisions(
                        vm.toVehicle().copy(alias = vm.alias.ifBlank { null }),
                        vm.revisionDates,
                        revisionsKms = vm.revisionKms
                    )
                    nav.popBackStack("list", false)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Crear Vehículo")
            }
        }
    }
}
package com.example.autocare.vehicle.registration.steps

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.autocare.AppHeader
import com.example.autocare.vehicle.VehicleRegistrationViewModel

@Composable
fun StepRegisterTypeScreen(
    vm: VehicleRegistrationViewModel,
    nav: NavHostController
) {
    Scaffold(
        topBar = { AppHeader("1/6: Tipo", onBack = { nav.popBackStack() }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            listOf("Gasolina", "Diésel", "Híbrido", "Eléctrico").forEach { option ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { vm.type = option }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = vm.type == option,
                        onClick = { vm.type = option }
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(option, style = MaterialTheme.typography.bodyLarge)
                }
            }
            Spacer(Modifier.weight(1f))
            Button(
                enabled = vm.canGoToBrand(),
                onClick = { nav.navigate("register/brand") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Siguiente")
            }
        }
    }
}

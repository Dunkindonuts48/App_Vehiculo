package com.example.autocare.vehicle.registration.steps

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.autocare.AppHeader
import com.example.autocare.vehicle.VehicleRegistrationViewModel

@Composable
fun StepRegisterPlateScreen(
    vm: VehicleRegistrationViewModel,
    nav: NavHostController
) {
    Scaffold(
        topBar = { AppHeader("3/6: Matrícula", onBack = { nav.popBackStack() }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = vm.plate,
                onValueChange = {
                    vm.plate = it.uppercase()
                        .replace("[^A-Z0-9]".toRegex(), "")
                        .replace(Regex("(\\d{4})([A-Z]{0,3})"), "$1 $2")
                },
                label = { Text("4 dígitos + 3 letras") },
                isError = vm.plate.isNotEmpty() && !vm.isPlateValid(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.weight(1f))

            Button(
                enabled = vm.canGoToPlate(),
                onClick = { nav.navigate("register/mileage") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Siguiente")
            }
        }
    }
}
package com.example.autocare.vehicle.registration.steps

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.autocare.AppHeader
import com.example.autocare.vehicle.VehicleRegistrationViewModel
import java.util.*

@Composable
fun StepRegisterMileageScreen(
    vm: VehicleRegistrationViewModel,
    nav: NavHostController
) {
    var showPicker by remember { mutableStateOf(false) }
    val ctx = LocalContext.current

    if (showPicker) {
        DatePickerDialog(
            ctx,
            { _, y, m, d ->
                vm.purchaseDate = "%02d/%02d/%04d".format(d, m + 1, y)
                showPicker = false
            },
            Calendar.getInstance().get(Calendar.YEAR),
            Calendar.getInstance().get(Calendar.MONTH),
            Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    Scaffold(
        topBar = { AppHeader("4/6: Km y Fecha compra", onBack = { nav.popBackStack() }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = vm.mileage,
                onValueChange = { if (it.all(Char::isDigit)) vm.mileage = it },
                label = { Text("Kilometraje") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = vm.purchaseDate,
                onValueChange = {},
                label = { Text("Fecha de compra") },
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showPicker = true }
            )

            Spacer(Modifier.weight(1f))

            if (vm.canGoToRevision()) {
                Button(
                    onClick = { nav.navigate("register/revision") },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Siguiente") }
            }
        }
    }
}
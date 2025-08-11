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

import androidx.compose.runtime.*
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.KeyboardCapitalization


@Composable
fun StepRegisterPlateScreen(
    vm: VehicleRegistrationViewModel,
    nav: NavHostController
) {
    var plateField by remember { mutableStateOf(TextFieldValue(vm.plate)) }

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
                value = plateField,
                onValueChange = { tf ->
                    val raw = tf.text.uppercase().filter { it.isLetterOrDigit() }
                    var digits = ""
                    var letters = ""

                    for (ch in raw) {
                        if (digits.length < 4 && ch.isDigit()) {
                            digits += ch
                        } else if (digits.length == 4 && letters.length < 3 && ch.isLetter()) {
                            letters += ch
                        }
                        if (digits.length == 4 && letters.length == 3) break
                    }

                    val formatted = if (digits.length == 4 && letters.length == 3) {
                        "$digits $letters"
                    } else {
                        digits + letters
                    }

                    plateField = tf.copy(
                        text = formatted,
                        selection = TextRange(formatted.length)
                    )
                    vm.plate = formatted
                },
                label = { Text("4 dígitos + 3 letras") },
                isError = vm.plate.isNotEmpty() && !vm.isPlateValid(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Ascii,
                    capitalization = KeyboardCapitalization.Characters,
                    autoCorrect = false
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.weight(1f))

            if (vm.isPlateValid()) {
                Button(
                    onClick = { nav.navigate("register/mileage") },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Siguiente") }
            }
        }
    }
}
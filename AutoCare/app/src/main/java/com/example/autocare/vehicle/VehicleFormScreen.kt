// VehicleFormScreen.kt (fecha de compra y última revisión ahora abren correctamente el calendario)
package com.example.autocare.vehicle

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
import java.util.*

@Composable
fun VehicleFormScreen(navController: NavHostController, viewModel: VehicleViewModel, vehicleId: Int?) {
    val context = LocalContext.current
    val existingVehicle = vehicleId?.let { id ->
        viewModel.vehicles.collectAsState().value.find { it.id == id }
    }

    var type by remember { mutableStateOf(existingVehicle?.type ?: "") }
    var brand by remember { mutableStateOf(existingVehicle?.brand ?: "") }
    var model by remember { mutableStateOf(existingVehicle?.model ?: "") }
    var plate by remember { mutableStateOf(existingVehicle?.plateNumber ?: "") }
    var mileage by remember { mutableStateOf((existingVehicle?.mileage ?: 0).toString()) }
    var purchaseDate by remember { mutableStateOf(existingVehicle?.purchaseDate ?: "") }
    var lastReviewDate by remember { mutableStateOf(existingVehicle?.lastMaintenanceDate ?: "") }
    var freqKm by remember { mutableStateOf((existingVehicle?.maintenanceFrequencyKm ?: 0).toString()) }
    var freqMonths by remember { mutableStateOf((existingVehicle?.maintenanceFrequencyMonths ?: 0).toString()) }

    val types = listOf("Gasolina", "Diésel", "Eléctrico", "Híbrido")
    val brands = listOf("Audi", "BMW", "SEAT", "Volkswagen", "SMART" )
    val modelsByBrand = mapOf(
        "Audi" to listOf("A1", "A3", "Q2"),
        "BMW" to listOf("Serie 1", "X1"),
        "SEAT" to listOf("Ibiza", "León"),
        "Volkswagen" to listOf("Golf", "Polo"),
        "SMART" to listOf("ForFour", "ForTwo")
    )

    val monthsOptions = (0..100).map { it.toString() }

    fun showDatePicker(current: String, onDateSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        val parts = current.split("/").mapNotNull { it.toIntOrNull() }
        if (parts.size == 3) {
            calendar.set(Calendar.DAY_OF_MONTH, parts[0])
            calendar.set(Calendar.MONTH, parts[1] - 1)
            calendar.set(Calendar.YEAR, parts[2])
        }
        DatePickerDialog(
            context,
            { _, y, m, d -> onDateSelected(String.format("%02d/%02d/%04d", d, m + 1, y)) },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    Scaffold(modifier = Modifier.fillMaxSize(), contentWindowInsets = WindowInsets.systemBars) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Text("${if (vehicleId == null) "Registrar" else "Editar"} Vehículo", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(16.dp))

            DropdownField("Tipo", type, types) { type = it }
            DropdownField("Marca", brand, brands) { brand = it }

            val models = modelsByBrand[brand] ?: emptyList()
            if (models.isNotEmpty()) {
                DropdownField("Modelo", model, models) { model = it }
            } else {
                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = { Text("Modelo") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = plate,
                onValueChange = {
                    plate = it.uppercase()
                        .replace("[^A-Z0-9]".toRegex(), "")
                        .replace(Regex("(\\d{4})([A-Z]{0,3})"), "$1 $2")
                },
                label = { Text("Matrícula") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = mileage,
                onValueChange = { if (it.all { c -> c.isDigit() }) mileage = it },
                label = { Text("Kilometraje") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Box(modifier = Modifier.fillMaxWidth().clickable { showDatePicker(purchaseDate) { purchaseDate = it } }) {
                OutlinedTextField(
                    value = purchaseDate,
                    onValueChange = { purchaseDate = it },
                    label = { Text("Fecha de compra") },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Box(modifier = Modifier.fillMaxWidth().clickable { showDatePicker(lastReviewDate) { lastReviewDate = it } }) {
                OutlinedTextField(
                    value = lastReviewDate,
                    onValueChange = { lastReviewDate = it },
                    label = { Text("Última revisión") },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            OutlinedTextField(
                value = freqKm,
                onValueChange = { if (it.all { c -> c.isDigit() }) freqKm = it },
                label = { Text("Frecuencia (km)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            DropdownField("Frecuencia (meses)", freqMonths, monthsOptions) { freqMonths = it }

            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {
                val data = Vehicle(
                    id = existingVehicle?.id ?: 0,
                    brand = brand,
                    model = model,
                    type = type,
                    plateNumber = plate,
                    mileage = mileage.toIntOrNull() ?: 0,
                    purchaseDate = purchaseDate,
                    lastMaintenanceDate = lastReviewDate,
                    maintenanceFrequencyKm = freqKm.toIntOrNull() ?: 0,
                    maintenanceFrequencyMonths = freqMonths.toIntOrNull() ?: 0
                )
                if (vehicleId == null) viewModel.registerVehicle(data)
                else viewModel.updateVehicle(data)
                navController.popBackStack("list", inclusive = false)
            }, modifier = Modifier.fillMaxWidth()) {
                Text("Guardar")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownField(label: String, selected: String, options: List<String>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach {
                DropdownMenuItem(text = { Text(it) }, onClick = {
                    onSelect(it)
                    expanded = false
                })
            }
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
}
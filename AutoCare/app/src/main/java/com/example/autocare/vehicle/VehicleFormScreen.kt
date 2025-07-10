package com.example.autocare.vehicle

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.autocare.util.BrandModelRepository
import java.util.*

@Composable
operator fun PaddingValues.plus(other: PaddingValues): PaddingValues {
    val layoutDirection = LocalLayoutDirection.current
    return PaddingValues(
        start  = this.calculateStartPadding(layoutDirection) + other.calculateStartPadding(layoutDirection),
        top    = this.calculateTopPadding() + other.calculateTopPadding(),
        end    = this.calculateEndPadding(layoutDirection) + other.calculateEndPadding(layoutDirection),
        bottom = this.calculateBottomPadding() + other.calculateBottomPadding()
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleFormScreen(
    navController: NavHostController,
    viewModel: VehicleViewModel,
    vehicleId: Int?
) {
    val context = LocalContext.current
    val repo = remember { BrandModelRepository(context) }
    val brands = remember { repo.getBrands().map { it.name } }

    val existing = vehicleId
        ?.let { id ->
            viewModel.vehicles.collectAsState().value.find { it.id == id }
        }

    var type by remember { mutableStateOf(existing?.type ?: "") }
    var brand by remember { mutableStateOf(existing?.brand ?: "") }
    var model by remember { mutableStateOf(existing?.model ?: "") }
    var plate by remember { mutableStateOf(existing?.plateNumber ?: "") }
    var mileage by remember { mutableStateOf((existing?.mileage ?: 0).toString()) }
    var purchaseDate by remember { mutableStateOf(existing?.purchaseDate ?: "") }
    var lastReviewDate by remember { mutableStateOf(existing?.lastMaintenanceDate ?: "") }
    var freqKm by remember { mutableStateOf((existing?.maintenanceFrequencyKm ?: 0).toString()) }
    var freqMonths by remember { mutableStateOf((existing?.maintenanceFrequencyMonths ?: 0).toString()) }
    var alias by remember { mutableStateOf(existing?.alias ?: "") }

    val types = listOf("Gasolina", "Diésel", "Eléctrico", "Híbrido")
    val monthsOptions = (0..100).map { it.toString() }

    fun showDatePicker(current: String, onDateSelected: (String) -> Unit) {
        val cal = Calendar.getInstance().apply {
            current.split("/").mapNotNull(String::toIntOrNull)
                .takeIf { it.size == 3 }
                ?.let { (d, m, y) ->
                    set(Calendar.DAY_OF_MONTH, d)
                    set(Calendar.MONTH, m - 1)
                    set(Calendar.YEAR, y)
                }
        }
        DatePickerDialog(
            context,
            { _, y, m, d -> onDateSelected("%02d/%02d/%04d".format(d, m + 1, y)) },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    var brandExpanded by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.systemBars,
        bottomBar = {
            Button(
                onClick = {
                    val data = Vehicle(
                        id = existing?.id ?: 0,
                        brand = brand,
                        model = model,
                        type = type,
                        plateNumber = plate,
                        mileage = mileage.toIntOrNull() ?: 0,
                        purchaseDate = purchaseDate,
                        lastMaintenanceDate = lastReviewDate,
                        maintenanceFrequencyKm = freqKm.toIntOrNull() ?: 0,
                        maintenanceFrequencyMonths = freqMonths.toIntOrNull() ?: 0,
                        alias = alias.ifBlank { null }
                    )
                    if (vehicleId == null) viewModel.registerVehicle(data)
                    else viewModel.updateVehicle(data)
                    navController.popBackStack("list", inclusive = false)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("Guardar")
            }
        }
    ) { padding ->
        LazyColumn(
            contentPadding = padding + PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Text(
                    text = if (vehicleId == null) "Registrar Vehículo" else "Editar Vehículo",
                    style = MaterialTheme.typography.headlineSmall
                )
            }
            item {
                DropdownField("Tipo", type, types) { type = it }
            }
            item {
                ExposedDropdownMenuBox(
                    expanded = brandExpanded,
                    onExpandedChange = { brandExpanded = it }
                ) {
                    OutlinedTextField(
                        value = brand,
                        onValueChange = {
                            brand = it
                            brandExpanded = true
                        },
                        label = { Text("Marca") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = brandExpanded)
                        }
                    )
                    ExposedDropdownMenu(
                        expanded = brandExpanded,
                        onDismissRequest = { brandExpanded = false }
                    ) {
                        brands.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    brand = option
                                    brandExpanded = false
                                }
                            )
                        }
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = { Text("Modelo") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
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
            }
            item {
                OutlinedTextField(
                    value = mileage,
                    onValueChange = { if (it.all(Char::isDigit)) mileage = it },
                    label = { Text("Kilometraje") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker(purchaseDate) { purchaseDate = it } }
                ) {
                    OutlinedTextField(
                        value = purchaseDate,
                        onValueChange = {},
                        label = { Text("Fecha de compra") },
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker(lastReviewDate) { lastReviewDate = it } }
                ) {
                    OutlinedTextField(
                        value = lastReviewDate,
                        onValueChange = {},
                        label = { Text("Última revisión") },
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            item {
                OutlinedTextField(
                    value = freqKm,
                    onValueChange = { if (it.all(Char::isDigit)) freqKm = it },
                    label = { Text("Frecuencia (km)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                DropdownField("Frecuencia (meses)", freqMonths, monthsOptions) { freqMonths = it }
            }
            item {
                OutlinedTextField(
                    value = alias,
                    onValueChange = { alias = it },
                    label = { Text("Alias (opcional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item { Spacer(modifier = Modifier.height(48.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownField(
    label: String,
    selected: String,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            }
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt) },
                    onClick = {
                        onSelect(opt)
                        expanded = false
                    }
                )
            }
        }
    }
}
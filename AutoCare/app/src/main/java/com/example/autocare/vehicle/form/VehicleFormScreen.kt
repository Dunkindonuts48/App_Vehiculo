package com.example.autocare.vehicle.form

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
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.autocare.AppHeader
import com.example.autocare.util.BrandModelRepository
import com.example.autocare.vehicle.Vehicle
import com.example.autocare.vehicle.VehicleViewModel
import java.util.Calendar

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
    val allBrands = remember { repo.getBrands().map { it.name } }
    val existing = vehicleId?.let { id -> viewModel.vehicles.collectAsState().value.find { it.id == id } }
    val types = listOf("Gasolina", "Diésel", "Eléctrico", "Híbrido")
    var type by remember { mutableStateOf(existing?.type ?: "") }
    var brand by remember { mutableStateOf(existing?.brand ?: "") }
    var brandExpanded by remember { mutableStateOf(false) }
    var brandFilter by remember { mutableStateOf(brand) }
    var model by remember { mutableStateOf(existing?.model ?: "") }
    var plate by remember { mutableStateOf(existing?.plateNumber ?: "") }
    var mileage by remember { mutableStateOf((existing?.mileage ?: 0).toString()) }
    var purchaseDate by remember { mutableStateOf(existing?.purchaseDate ?: "") }
    var alias by remember { mutableStateOf(existing?.alias ?: "") }
    var triedSave by remember { mutableStateOf(false) }
    val typeError = triedSave && type.isBlank()
    val brandError = triedSave && brand.isBlank()
    val plateError = triedSave && plate.isBlank()
    val mileageError = triedSave && (mileage.isBlank() || (mileage.toIntOrNull() ?: -1) < 0)
    val purchaseDateError = triedSave && purchaseDate.isBlank()
    val isFormValid = !typeError && !brandError && !plateError && !mileageError && !purchaseDateError

    fun showDatePicker(current: String, onDateSelected: (String) -> Unit) {
        val cal = Calendar.getInstance().apply {
            val parts = current.split("/")
            if (parts.size == 3) {
                val d = parts[0].toIntOrNull() ?: get(Calendar.DAY_OF_MONTH)
                val m = (parts[1].toIntOrNull() ?: (get(Calendar.MONTH) + 1)) - 1
                val y = parts[2].toIntOrNull() ?: get(Calendar.YEAR)
                set(Calendar.DAY_OF_MONTH, d)
                set(Calendar.MONTH, m.coerceIn(0, 11))
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

    Scaffold(
        topBar = {
            AppHeader(
                title = if (vehicleId == null) "Registrar Vehículo" else "Editar Vehículo",
                onBack = { navController.popBackStack("list", false) }
            )
        },
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.systemBars,
        bottomBar = {
            Button(
                onClick = {
                    triedSave = true
                    if (!isFormValid) return@Button

                    val data = Vehicle(
                        id = existing?.id ?: 0,
                        brand = brand.trim(),
                        model = model.trim(),
                        type = type.trim(),
                        plateNumber = plate.trim(),
                        mileage = mileage.toIntOrNull() ?: 0,
                        purchaseDate = purchaseDate.trim(),
                        lastMaintenanceDate = existing?.lastMaintenanceDate ?: "",
                        maintenanceFrequencyKm = 0,
                        maintenanceFrequencyMonths = 0,
                        alias = alias.ifBlank { null }
                    )
                    if (vehicleId == null) {
                        viewModel.registerVehicleWithRevisions(
                            data,
                            revisionsDates = emptyMap<String, String>(),
                            revisionsKms   = emptyMap<String, String>()
                        )
                    } else {
                        viewModel.updateVehicle(data)
                    }
                    navController.popBackStack("list", inclusive = false)
                },
                enabled = isFormValid,
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
                DropdownField(
                    label = "Tipo",
                    selected = type,
                    options = types,
                    onSelect = { type = it },
                    isError = typeError,
                    supportingText = { if (typeError) Text("El tipo es obligatorio", fontSize = 12.sp) }
                )
            }
            item {
                ExposedDropdownMenuBox(
                    expanded = brandExpanded,
                    onExpandedChange = { brandExpanded = it }
                ) {
                    OutlinedTextField(
                        value = brandFilter,
                        onValueChange = {
                            brandFilter = it
                            brandExpanded = true
                        },
                        label = { Text("Marca") },
                        isError = brandError,
                        supportingText = {
                            if (brandError) Text("La marca es obligatoria", fontSize = 12.sp)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = brandExpanded)
                        }
                    )

                    val filtered = remember(brandFilter, allBrands) {
                        if (brandFilter.isBlank()) allBrands
                        else allBrands.filter { b -> b.contains(brandFilter, ignoreCase = true) }
                    }

                    ExposedDropdownMenu(
                        expanded = brandExpanded,
                        onDismissRequest = { brandExpanded = false }
                    ) {
                        if (filtered.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("— Sin coincidencias —", color = MaterialTheme.colorScheme.outline) },
                                onClick = {},
                                enabled = false
                            )
                        } else {
                            filtered.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        brand = option
                                        brandFilter = option
                                        brandExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = { Text("Modelo (opcional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = plate,
                    onValueChange = {
                        plate = it.uppercase()
                            .replace("[^A-Z0-9]".toRegex(), "")
                            .replace(Regex("(\\d{0,4})([A-Z]{0,3}).*"), "$1$2")
                    },
                    label = { Text("Matrícula") },
                    isError = plateError,
                    supportingText = {
                        if (plateError) Text("La matrícula es obligatoria", fontSize = 12.sp)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = mileage,
                    onValueChange = { if (it.all(Char::isDigit)) mileage = it },
                    label = { Text("Kilometraje") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = mileageError,
                    supportingText = {
                        if (mileageError) Text("Introduce un número válido (≥ 0)", fontSize = 12.sp)
                    },
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
                        isError = purchaseDateError,
                        supportingText = {
                            if (purchaseDateError) Text("La fecha de compra es obligatoria", fontSize = 12.sp)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
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
    onSelect: (String) -> Unit,
    isError: Boolean = false,
    supportingText: @Composable (() -> Unit)? = null
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
            isError = isError,
            supportingText = supportingText,
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

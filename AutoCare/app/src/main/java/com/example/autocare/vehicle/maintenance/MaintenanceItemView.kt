package com.example.autocare.vehicle.maintenance

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.autocare.vehicle.VehicleViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

private val DISPLAY_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
private fun LocalDate.asDisplay(): String = this.format(DISPLAY_FMT)
private fun parseDisplayDateOrToday(s: String?): LocalDate =
    try { LocalDate.parse(s, DISPLAY_FMT) } catch (_: Exception) { LocalDate.now() }
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaintenanceItemView(
    vehicleId: Int,
    vehicleType: String,
    currentMileage: Int,
    extraKmFromSessions: Int,
    onSave: (Maintenance) -> Unit,
    onCancel: () -> Unit
) {
    val commonAll = listOf(
        "Neumáticos: estado",
        "Frenos: pastillas",
        "Frenos: discos",
        "Filtro de habitáculo",
        "Líquido de frenos",
        "Batería 12 V"
    )
    val commonICEHybrid = listOf(
        "Cambio de aceite de motor",
        "Filtro de aceite",
        "Filtro de aire de motor",
        "Sistema de escape: fugas",
        "Refrigerante / anticongelante",
        "Correa de distribución / cadena"
    )
    val gasolina = listOf(
        "Bujías de gasolina",
        "Filtro de combustible",
        "Sistema de encendido (bobinas)",
        "Limpieza de inyectores",
        "Inspección de admisión de aire"
    )
    val diesel = listOf(
        "Filtro de partículas diésel (DPF)",
        "AdBlue: nivel e inyección",
        "Sistema de inyección diésel: boquillas",
        "Bujías de precalentamiento",
        "Filtro de combustible"
    )
    val hybrid = listOf(
        "Batería híbrida: estado y refrigeración",
        "Revisión del sistema regenerativo",
        "Electrónica de potencia",
        "Revisión del freno motor eléctrico"
    )
    val electric = listOf(
        "Batería de tracción: estado y balance",
        "Líquido de refrigeración de batería",
        "Software/firmware del vehículo",
        "Sistema de recarga: conectores",
        "Freno regenerativo"
    )

    val t = vehicleType.lowercase(Locale.getDefault())
    val typeOptions = remember(t) {
        mutableListOf<String>().apply {
            addAll(commonAll)
            if (!t.contains("eléctrico")) addAll(commonICEHybrid)
            when {
                t.contains("gasolina") -> addAll(gasolina)
                t.contains("diésel") || t.contains("diesel") -> addAll(diesel)
                t.contains("híbrido") || t.contains("hibrido") -> addAll(hybrid)
                t.contains("eléctrico") || t.contains("electrico") -> addAll(electric)
            }
        }.distinct()
    }
    val effectiveMileage = currentMileage + extraKmFromSessions
    var type by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var dateText by remember { mutableStateOf(LocalDate.now().asDisplay()) }
    var mileageText by remember { mutableStateOf(effectiveMileage.toString()) }
    var cost by remember { mutableStateOf("") }
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    val datePicker = DatePickerDialog(
        context,
        { _, y, m, d ->
            val picked = LocalDate.of(y, m + 1, d)
            dateText = picked.asDisplay()
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    fun toDoubleOrNullNormalized(s: String): Double? =
        s.replace(',', '.').toDoubleOrNull()

    Surface(
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 3.dp,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                "Añadir Mantenimiento",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.height(8.dp))

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = type,
                    onValueChange = {},
                    label = { Text("Tipo de mantenimiento") },
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    typeOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                type = option
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = dateText,
                onValueChange = {},
                label = { Text("Fecha (dd/MM/yyyy)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { datePicker.show() },
                readOnly = true
            )

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = cost,
                onValueChange = { cost = it },
                label = { Text("Coste") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = mileageText,
                onValueChange = { txt ->
                    if (txt.all(Char::isDigit)) mileageText = txt
                },
                label = { Text("Kilometraje en el momento") },
                placeholder = { Text(effectiveMileage.toString()) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                isError = mileageText.isBlank()
                        || (mileageText.toIntOrNull() ?: -1) < 0
                        || (mileageText.toIntOrNull() ?: 0) > effectiveMileage
            )


            Spacer(Modifier.height(16.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    )
                ) {
                    Text("Cancelar")
                }

                Button(
                    onClick = {
                        if (type.isBlank()) {
                            Toast.makeText(context, "Selecciona un tipo", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val kmAt = mileageText.toIntOrNull()
                        if (kmAt == null || kmAt < 0) {
                            Toast.makeText(context, "Kilometraje inválido", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (kmAt > effectiveMileage ) {
                            Toast.makeText(
                                context,
                                "El km del mantenimiento no puede superar el odómetro actual ($currentMileage)",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@Button
                        }

                        val costValue = toDoubleOrNullNormalized(cost) ?: 0.0
                        val maintenance = Maintenance(
                            id = 0,
                            vehicleId = vehicleId,
                            type = type,
                            date = parseDisplayDateOrToday(dateText),
                            cost = costValue,
                            mileageAtMaintenance = kmAt

                        )
                        onSave(maintenance)
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Guardar")
                }
            }
        }
    }
}
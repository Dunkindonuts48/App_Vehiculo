package com.example.autocare.vehicle.maintenance

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaintenanceItemView(
    vehicleId: Int,
    vehicleType: String,
    currentMileage: Int,
    onSave: (Maintenance) -> Unit,
    onCancel: () -> Unit
) {
    val commonAll = listOf(
        "Neumáticos: presión y estado",
        "Frenos: pastillas y discos",
        "Filtro de habitáculo",
        "Luces: faros, intermitentes, luces de freno",
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
        "Inspección de cableado de alta tensión (HV)",
        "Sistema de recarga: conectores",
        "Freno regenerativo"
    )

    val t = vehicleType.lowercase()
    val typeOptions = remember(t) {
        mutableListOf<String>().apply {
            addAll(commonAll)
            if (!t.contains("eléctrico")) addAll(commonICEHybrid)
            when {
                t.contains("gasolina") -> addAll(gasolina)
                t.contains("diésel")   -> addAll(diesel)
                t.contains("híbrido")  -> addAll(hybrid)
                t.contains("eléctrico")-> addAll(electric)
            }
        }.distinct()
    }

    var type by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var date by remember { mutableStateOf("") }
    var cost by remember { mutableStateOf("") }

    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    val datePicker = DatePickerDialog(
        context,
        { _, y, m, d ->
            val picked = Calendar.getInstance().apply { set(y, m, d) }
            date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(picked.time)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

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
                    onValueChange = { },
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
                value = date,
                onValueChange = { },
                label = { Text("Fecha") },
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
                modifier = Modifier.fillMaxWidth()
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
                        val m = Maintenance(
                            id = 0,
                            vehicleId = vehicleId,
                            type = type,
                            date = date,
                            cost = cost.toDoubleOrNull() ?: 0.0,
                            mileageAtMaintenance = currentMileage
                        )
                        onSave(m)
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

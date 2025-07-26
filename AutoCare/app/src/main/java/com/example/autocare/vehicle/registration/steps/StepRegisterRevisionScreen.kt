package com.example.autocare.vehicle.registration.steps

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.autocare.AppHeader
import com.example.autocare.vehicle.VehicleRegistrationViewModel
import java.util.*

@Composable
fun StepRegisterRevisionScreen(
    vm: VehicleRegistrationViewModel,
    nav: NavHostController
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

    val options = remember(vm.type) {
        val t = vm.type?.lowercase() ?: ""
        val base = commonAll +
                if ("eléctrico" in t) emptyList() else commonICEHybrid +
                        when {
                            "gasolina" in t   -> gasolina
                            "diésel" in t     -> diesel
                            "híbrido" in t    -> hybrid
                            "eléctrico" in t  -> electric
                            else              -> emptyList()
                        }
        base.distinct()
    }
    var allChecked by remember { mutableStateOf(false) }
    var useCommonDate by remember { mutableStateOf(false) }
    var commonDate by remember { mutableStateOf("") }

    Scaffold(
        topBar = { AppHeader("5/6: Última revisión", onBack = { nav.popBackStack() }) }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Checkbox(
                        checked = allChecked,
                        onCheckedChange = { checked ->
                            allChecked = checked
                            if (checked) {
                                options.forEach { opt ->
                                    vm.revisionDates[opt] = commonDate
                                }
                            } else {
                                vm.revisionDates.clear()
                            }
                        }
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Marcar todas",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = useCommonDate,
                            onCheckedChange = { checked ->
                                useCommonDate = checked
                                if (checked) {
                                    options.forEach { opt ->
                                        vm.revisionDates[opt] = commonDate
                                    }
                                }
                            }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Usar misma fecha para todas",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    if (useCommonDate) {
                        Spacer(Modifier.height(4.dp))
                        RevisionDatePicker(
                            date = commonDate,
                            onDateSelected = { picked ->
                                commonDate = picked
                                vm.revisionDates.keys.forEach { key ->
                                    vm.revisionDates[key] = picked
                                }
                            }
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            LazyColumn(Modifier.weight(1f)) {
                items(options) { opt ->
                    val selected = vm.revisionDates.containsKey(opt)
                    val date = vm.revisionDates[opt].orEmpty()
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Checkbox(
                            checked = selected,
                            onCheckedChange = {
                                if (it) {
                                    vm.revisionDates[opt] =
                                        if (useCommonDate && commonDate.isNotBlank()) commonDate else ""
                                } else {
                                    vm.revisionDates.remove(opt)
                                }
                                allChecked = vm.revisionDates.size == options.size
                            }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(opt, Modifier.weight(1f))
                        if (selected && !useCommonDate) {
                            RevisionDatePicker(
                                date = date,
                                onDateSelected = { picked ->
                                    vm.revisionDates[opt] = picked
                                }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Button(
                enabled = vm.revisionDates.isNotEmpty(),
                onClick = { nav.navigate("register/alias") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Siguiente")
            }
        }
    }
}

@Composable
private fun RevisionDatePicker(
    date: String,
    onDateSelected: (String) -> Unit
) {
    var showPicker by remember { mutableStateOf(false) }
    val ctx = LocalContext.current

    if (showPicker) {
        val today = Calendar.getInstance()
        DatePickerDialog(
            ctx,
            { _, y, m, d ->
                onDateSelected("%02d/%02d/%04d".format(d, m + 1, y))
                showPicker = false
            },
            today.get(Calendar.YEAR),
            today.get(Calendar.MONTH),
            today.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    OutlinedTextField(
        value = date,
        onValueChange = {},
        readOnly = true,
        label = { Text("Fecha") },
        modifier = Modifier
            .width(140.dp)
            .clickable { showPicker = true }
    )
}
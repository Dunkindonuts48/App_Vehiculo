package com.example.autocare.vehicle.registration.steps

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.autocare.AppHeader
import com.example.autocare.vehicle.VehicleRegistrationViewModel
import java.util.*
import kotlin.math.min

@Composable
fun StepRegisterRevisionScreen(
    vm: VehicleRegistrationViewModel,
    nav: NavHostController
) {
    val commonAll = listOf(
        "Neumáticos: estado",
        "Frenos: pastillas",
        "Frenos: discos",
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
        "AdBlue",
        "Sistema de inyección diésel: boquillas",
        "Bujías de precalentamiento",
        "Filtro de combustible"
    )
    val hybrid = listOf(
        "Batería híbrida: estado y refrigeración",
        "Revisión del sistema regenerativo",
        "Revisión del freno motor eléctrico"
    )
    val electric = listOf(
        "Batería de tracción: estado y balance",
        "Líquido de refrigeración de batería",
        "Inspección de cableado de alta tensión (HV)",
        "Sistema de recarga: conectores",
        "Freno regenerativo"
    )

    val options = remember(vm.type) {
        val t = vm.type?.lowercase().orEmpty()
        val base = commonAll +
                if ("eléctrico" in t || "electrico" in t) emptyList() else commonICEHybrid +
                        when {
                            "gasolina" in t -> gasolina
                            "diésel" in t || "diesel" in t -> diesel
                            "híbrido" in t || "hibrido" in t -> hybrid
                            "eléctrico" in t || "electrico" in t -> electric
                            else -> emptyList()
                        }
        base.distinct()
    }

    //val odo = remember(vm.mileage) { vm.mileage ?: 0 }

    var allChecked by remember { mutableStateOf(false) }
    var useCommonDate by remember { mutableStateOf(false) }
    var commonDate by remember { mutableStateOf("") }

    var useCommonKm by remember { mutableStateOf(false) }
    var commonKm by remember { mutableStateOf("") }

    val odo: Int = remember(vm.mileage) { vm.mileage?.toIntOrNull() ?: 0 }

    fun clampToOdometer(text: String): String {
        val digits = text.filter { it.isDigit() }
        val n = digits.toIntOrNull() ?: 0
        return min(n, odo).toString()
    }

    fun applyCommonKmToAllSelected() {
        if (!useCommonKm) return
        val clamped = clampToOdometer(commonKm)
        options.forEach { opt ->
            if (isKmType(opt) && vm.revisionDates.containsKey(opt)) {
                vm.revisionKms[opt] = clamped
            }
        }
    }

    Scaffold(
        topBar = { AppHeader("5/6: Última revisión", onBack = { nav.popBackStack() }) }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Marcar todas
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
                                    if (isKmType(opt)) {
                                        // Inicializamos sólo ahora; luego el usuario puede borrar/editar
                                        vm.revisionKms[opt] = odo.toString()
                                    }
                                }
                                if (useCommonKm && commonKm.isNotBlank()) applyCommonKmToAllSelected()
                            } else {
                                vm.revisionDates.clear()
                                vm.revisionKms.clear()
                            }
                        }
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Marcar todas", style = MaterialTheme.typography.bodyLarge)
                }
            }

            Spacer(Modifier.height(8.dp))

            // Usar misma fecha para todas
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
                        Text("Usar misma fecha para todas", style = MaterialTheme.typography.bodyLarge)
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

            Spacer(Modifier.height(8.dp))

            // Usar mismos KM para todas (sólo afecta a tipos con km)
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = useCommonKm,
                            onCheckedChange = { checked ->
                                useCommonKm = checked
                                if (checked && commonKm.isNotBlank()) {
                                    applyCommonKmToAllSelected()
                                }
                            }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Usar mismos km para todas (hasta $odo)", style = MaterialTheme.typography.bodyLarge)
                    }
                    if (useCommonKm) {
                        Spacer(Modifier.height(6.dp))
                        OutlinedTextField(
                            value = commonKm,
                            onValueChange = { new ->
                                commonKm = clampToOdometer(new)
                                applyCommonKmToAllSelected()
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            label = { Text("Km comunes") },
                            placeholder = { Text(odo.toString()) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Lista con fecha y km (si aplica)
            LazyColumn(Modifier.weight(1f)) {
                items(options) { opt ->
                    val selected = vm.revisionDates.containsKey(opt)
                    val date = vm.revisionDates[opt].orEmpty()
                    val kmValue = vm.revisionKms[opt].orEmpty()

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = selected,
                                onCheckedChange = { nowSelected ->
                                    if (nowSelected) {
                                        vm.revisionDates[opt] =
                                            if (useCommonDate && commonDate.isNotBlank()) commonDate else ""
                                        if (isKmType(opt)) {
                                            // Sólo inicializamos al seleccionar; luego el usuario controla el valor
                                            val initial = if (useCommonKm && commonKm.isNotBlank())
                                                clampToOdometer(commonKm)
                                            else kmValue.ifBlank { odo.toString() }
                                            vm.revisionKms[opt] = initial
                                        }
                                    } else {
                                        vm.revisionDates.remove(opt)
                                        vm.revisionKms.remove(opt)
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
                        if (selected && isKmType(opt) && !useCommonKm) {
                            Spacer(Modifier.height(6.dp))
                            OutlinedTextField(
                                value = kmValue,
                                onValueChange = { new ->
                                    val clamped = clampToOdometer(new)
                                    vm.revisionKms[opt] = clamped
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                label = { Text("Km en la última vez") },
                                placeholder = { Text(odo.toString()) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
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
private val intervalKms = setOf(
    "Neumáticos: estado","Filtro de aire de motor","Filtro de combustible",
    "Cambio de aceite de motor","Filtro de aceite","Filtro de partículas diésel (DPF)",
    "Frenos: pastillas","Frenos: discos","Filtro de habitáculo","Líquido de frenos",
    "Refrigerante / anticongelante","Sistema de escape: fugas","Correa de distribución / cadena",
    "Bujías de gasolina","Sistema de encendido (bobinas)","Limpieza de inyectores",
    "Inspección de admisión de aire","AdBlue","Sistema de inyección diésel: boquillas",
    "Bujías de precalentamiento","Batería híbrida: estado y refrigeración",
    "Revisión del sistema regenerativo","Revisión del freno motor eléctrico",
    "Líquido de refrigeración de batería","Freno regenerativo"
)
private fun isKmType(type: String) = intervalKms.contains(type)
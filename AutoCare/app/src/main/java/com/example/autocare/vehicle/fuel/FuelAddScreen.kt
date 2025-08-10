package com.example.autocare.vehicle.fuel

import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
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
import com.example.autocare.vehicle.VehicleViewModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DISPLAY_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
private fun parseDisplayDateOrToday(s: String?): LocalDate =
    try { LocalDate.parse(s, DISPLAY_FMT) } catch (_: Exception) { LocalDate.now() }
private fun recognizeTextFromUri(
    context: android.content.Context,
    uri: Uri,
    onResult: (String) -> Unit,
    onError: (Exception) -> Unit
) {
    val image = InputImage.fromFilePath(context, uri)
    TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        .process(image)
        .addOnSuccessListener { visionText -> onResult(visionText.text) }
        .addOnFailureListener { e -> onError(e) }
}

private fun recognizeTextFromBitmap(
    bmp: Bitmap,
    onResult: (String) -> Unit,
    onError: (Exception) -> Unit
) {
    val image = InputImage.fromBitmap(bmp, 0)
    TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        .process(image)
        .addOnSuccessListener { visionText -> onResult(visionText.text) }
        .addOnFailureListener { e -> onError(e) }
}

private fun parseText(
    raw: String,
    onLiters: (String) -> Unit,
    onPrice: (String) -> Unit
) {
    val cleaned = raw
        .uppercase(Locale.getDefault())
        .replace(Regex("""[^0-9€,/.L\s]"""), " ")
        .replace(Regex("\\s+"), " ")
        .trim()

    // Litros: "xx,xxx L" o "xx.xx L"
    Regex("""(\d+[.,]\d{1,3})(?=\s*(?:LITROS?\b|L\b))""")
        .findAll(cleaned)
        .lastOrNull()
        ?.groupValues?.get(1)
        ?.let { onLiters(it.replace("[,']".toRegex(), ".")) }

    // Precio €/L (preferente); si no, último valor seguido de €
    val idx = cleaned.indexOf("€/L")
    val priceMatch = if (idx >= 0) {
        val after = cleaned.substring(idx + 3)
        Regex("""\s*(\d+[.,]\d{1,3})""").find(after)?.groupValues?.get(1)
    } else {
        Regex("""(\d+[.,]\d{1,3})(?=\s*€)""")
            .findAll(cleaned)
            .lastOrNull()
            ?.groupValues
            ?.get(1)
    }

    priceMatch?.let { onPrice(it.replace("[,']".toRegex(), ".")) }
}
@Composable
fun FuelAddScreen(
    vehicleId: Int,
    viewModel: VehicleViewModel,
    navController: NavHostController
) {
    val context = LocalContext.current

    // Mantén la fecha en UI como String "dd/MM/yyyy"; convertimos a LocalDate al guardar.
    var dateText by remember { mutableStateOf(LocalDate.now().format(DISPLAY_FMT)) }

    var mileage by remember {
        mutableStateOf(
            viewModel.vehicles.value.firstOrNull { it.id == vehicleId }?.mileage?.toString() ?: ""
        )
    }
    var liters by remember { mutableStateOf("") }
    var pricePerLiter by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            imageUri = it
            recognizeTextFromUri(
                context,
                it,
                onResult = { raw ->
                    Log.d("OCR_RAW", raw)
                    parseText(raw, { l -> liters = l }, { p -> pricePerLiter = p })
                },
                onError = {
                    Toast.makeText(context, "Error al leer imagen", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    val takePhoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bmp ->
        bmp?.let {
            bitmap = it
            recognizeTextFromBitmap(
                it,
                onResult = { raw -> parseText(raw, { l -> liters = l }, { p -> pricePerLiter = p }) },
                onError = { Toast.makeText(context, "Error al leer foto", Toast.LENGTH_SHORT).show() }
            )
        }
    }

    fun toFloatOrNullNormalized(s: String): Float? = s.replace(',', '.').toFloatOrNull()

    Scaffold(
        topBar = { AppHeader("Añadir repostaje", onBack = { navController.popBackStack() }) }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = dateText,
                onValueChange = { dateText = it },
                label = { Text("Fecha (dd/MM/yyyy)") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = mileage,
                onValueChange = { if (it.all(Char::isDigit)) mileage = it },
                label = { Text("Kilometraje") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = liters,
                onValueChange = { liters = it },
                label = { Text("Litros") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = pricePerLiter,
                onValueChange = { pricePerLiter = it },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                label = { Text("Precio €/litro") },
                modifier = Modifier.fillMaxWidth()
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { pickImage.launch("image/*") }, Modifier.weight(1f)) {
                    Text("Importar imagen")
                }
                Button(onClick = { takePhoto.launch(null) }, Modifier.weight(1f)) {
                    Text("Hacer foto")
                }
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    val l = toFloatOrNullNormalized(liters)
                    val p = toFloatOrNullNormalized(pricePerLiter)
                    val m = mileage.toIntOrNull()

                    if (l == null || p == null) {
                        Toast.makeText(context, "Revisa litros y precio", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (m == null) {
                        Toast.makeText(context, "Kilometraje inválido", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val entry = FuelEntry(
                        id = 0,
                        vehicleId = vehicleId,
                        date = parseDisplayDateOrToday(dateText), // <-- LocalDate
                        mileage = m,
                        liters = l,
                        pricePerLiter = p
                    )
                    viewModel.insertFuelEntry(entry)
                    navController.popBackStack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.CenterHorizontally)
            ) {
                Text("Guardar", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
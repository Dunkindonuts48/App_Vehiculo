package com.example.autocare.vehicle.fuel

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.navigation.NavHostController
import com.example.autocare.AppHeader
import com.example.autocare.vehicle.VehicleViewModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DISPLAY_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
private fun parseDisplayDateOrToday(s: String?): LocalDate =
    try {
        LocalDate.parse(s, DISPLAY_FMT)
    } catch (_: Exception) {
        LocalDate.now()
    }

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
    val lines = raw
        .split('\n')
        .map { line ->
            line.uppercase(Locale.getDefault())
                .replace(Regex("""[^\dA-Z€/.,:\\s]"""), " ")
                .replace(Regex("\\s+"), " ")
                .trim()
        }
        .filter { it.isNotBlank() }

    fun firstNumberIn(s: String): String? =
        Regex("""\b(\d+[.,]\d{1,3})\b""")
            .find(s)
            ?.groupValues?.get(1)

    fun numberWithinNext(lines: List<String>, startIdx: Int, maxAhead: Int = 2): String? {
        for (i in startIdx..minOf(startIdx + maxAhead, lines.lastIndex)) {
            firstNumberIn(lines[i])?.let { return it }
        }
        return null
    }

    var priceStr: String? = null
    val priceIdx = lines.indexOfFirst { it.contains("€/L") || it.contains("EUR/L") || it.contains("€ / L") }
    if (priceIdx >= 0) {
        priceStr = Regex("""(?:€|EUR)\s*/\s*L\s*[:\-]?\s*(\d+[.,]\d{1,3})""")
            .find(lines[priceIdx])?.groupValues?.get(1)
            ?: numberWithinNext(lines, priceIdx + 1, 2)
    }

    var litersStr: String? = null
    val litersIdx = lines.indexOfFirst { it.contains("LITROS") || it.contains("CANTIDAD") || it.contains("VOL") }
    if (litersIdx >= 0) {
        litersStr = firstNumberIn(lines[litersIdx])
            ?: numberWithinNext(lines, litersIdx + 1, 1)
    } else {
        val headerIdx = lines.indexOfFirst { it.contains("PRODUCTO") && it.contains("€/L") && it.contains("LITROS") }
        if (headerIdx >= 0 && headerIdx + 1 <= lines.lastIndex) {
            val nums = Regex("""\d+[.,]\d{1,3}""")
                .findAll(lines[headerIdx + 1])
                .map { it.value }
                .toList()
            if (nums.size >= 2) litersStr = nums[1]
        }
    }

    priceStr?.let { onPrice(it.replace(',', '.')) }

    litersStr?.let { candidate ->
        val litersClean = candidate.replace(',', '.')
        val priceClean = priceStr?.replace(',', '.')
        if (priceClean != null && litersClean == priceClean) {
            if (litersIdx >= 0) {
                val alt = Regex("""\b(\d+[.,]\d{1,3})\b""")
                    .findAll(lines.getOrNull(litersIdx) ?: "")
                    .map { it.groupValues[1] }
                    .drop(1)
                    .firstOrNull()
                    ?: firstNumberIn(lines.getOrNull(litersIdx + 1) ?: "")
                if (alt != null && alt.replace(',', '.') != priceClean) {
                    onLiters(alt.replace(',', '.'))
                } else {
                    onLiters(litersClean)
                }
            } else {
                onLiters(litersClean)
            }
        } else {
            onLiters(litersClean)
        }
    }

    if (priceStr == null && litersStr != null) {
        val total = lines.firstNotNullOfOrNull { line ->
            Regex("""(?:TOTAL|IMPORTE(?:\s+TOTAL)?)\s*[:\-]?\s*(\d+[.,]\d{1,2})\s*(?:€|EUR)?""")
                .find(line)
                ?.groupValues?.get(1)
        }?.replace(',', '.')?.toFloatOrNull()

        val l = litersStr.replace(',', '.').toFloatOrNull()
        if (total != null && l != null && l > 0f) {
            onPrice(String.format(Locale.US, "%.3f", total / l))
        }
    }
}

@Composable
fun FuelAddScreen(
    vehicleId: Int,
    viewModel: VehicleViewModel,
    navController: NavHostController
) {
    val context = LocalContext.current
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
                    val beforeL = liters
                    val beforeP = pricePerLiter
                    parseText(raw, { l -> liters = l }, { p -> pricePerLiter = p })
                    if (raw.isBlank() || (liters == beforeL && pricePerLiter == beforeP)) {
                        Toast.makeText(
                            context,
                            "No se han podido extraer datos del ticket",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                onError = {
                    Toast.makeText(context, "Error al leer imagen", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    val photoUri = remember {
        val file = File(context.cacheDir, "ticket_${System.currentTimeMillis()}.jpg")
        FileProvider.getUriForFile(
            context,
            context.packageName + ".fileprovider",
            file
        )
    }

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) {
            recognizeTextFromUri(
                context,
                photoUri,
                onResult = { raw ->
                    Log.d("OCR_RAW", raw)
                    val beforeL = liters
                    val beforeP = pricePerLiter
                    parseText(raw, { liters = it }, { pricePerLiter = it })
                    if (raw.isBlank() || (liters == beforeL && pricePerLiter == beforeP)) {
                        Toast.makeText(
                            context,
                            "No se han podido extraer datos del ticket",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                onError = { Toast.makeText(context, "Error al leer foto", Toast.LENGTH_SHORT).show() }
            )
        } else {
            Toast.makeText(context, "Foto cancelada", Toast.LENGTH_SHORT).show()
        }
    }

    val requestCameraPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            takePicture.launch(photoUri)
        } else {
            Toast.makeText(
                context,
                "Se necesita permiso de cámara para hacer la foto",
                Toast.LENGTH_SHORT
            ).show()
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
                Button(
                    onClick = {
                        val granted = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED

                        if (granted) {
                            takePicture.launch(photoUri)
                        } else {
                            requestCameraPermission.launch(Manifest.permission.CAMERA)
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
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
                        date = parseDisplayDateOrToday(dateText),
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
package com.example.autocare.vehicle

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModel

class VehicleRegistrationViewModel : ViewModel() {
    var type by mutableStateOf<String?>(null)
    var brand by mutableStateOf<String?>(null)
    var model by mutableStateOf("")
    var plate by mutableStateOf("")
    var mileage by mutableStateOf("")
    var purchaseDate by mutableStateOf("")
    var lastRevision by mutableStateOf("")
    var alias by mutableStateOf("")
    val revisionDates = mutableStateMapOf<String, String>()
    val revisionKms   = mutableStateMapOf<String, String>()


    fun isPlateValid() = plate.matches(Regex("\\d{4} [A-Z]{3}"))
    fun canGoToBrand() = !type.isNullOrBlank()
    fun canGoToPlate() = !brand.isNullOrBlank()
    fun canGoToMileage() = isPlateValid()
    fun canGoToRevision() = mileage.isNotBlank() && purchaseDate.isNotBlank()
    fun canGoToAlias() = revisionDates.isNotEmpty()

    fun toVehicle(): Vehicle = Vehicle(
        brand                  = brand!!,
        model                  = model,
        type                   = type!!,
        plateNumber            = plate,
        mileage                = mileage.toInt(),
        purchaseDate           = purchaseDate,
        lastMaintenanceDate    = lastRevision,
        maintenanceFrequencyKm     = 0,
        maintenanceFrequencyMonths = 0,
        alias                  = alias.ifBlank { null },
    )
}


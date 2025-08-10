package com.example.autocare.vehicle.maintenance

import java.time.LocalDate

enum class ReviewStatus { OK, SOON, OVERDUE }
data class NextMaintenance(
    val type: String,
    val leftKm: Int?,
    val leftDays: Int?,
    val status: ReviewStatus,
    val nextKm: Int?,
    val nextDate: LocalDate?
)

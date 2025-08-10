package com.example.autocare.db

import androidx.room.TypeConverter
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

object Converters {
    private val ISO: DateTimeFormatter =
        DateTimeFormatter.ISO_LOCAL_DATE
    private val DISPLAY: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd/MM/yyyy").withLocale(Locale.getDefault())
    @TypeConverter
    @JvmStatic
    fun fromLocalDate(date: LocalDate?): String? {
        return date?.format(ISO)
    }
    @TypeConverter
    @JvmStatic
    fun toLocalDate(value: String?): LocalDate? {
        if (value.isNullOrBlank()) return null
        try { return LocalDate.parse(value, ISO) } catch (_: Exception) { /* sigue */ }
        try { return LocalDate.parse(value, DISPLAY) } catch (_: Exception) { /* sigue */ }
        val trimmed = value.trim().replace("\\s+".toRegex(), "")
        return try { LocalDate.parse(trimmed, DISPLAY) } catch (_: Exception) {
            null
        }
    }
}
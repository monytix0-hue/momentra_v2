package com.example.momentra.ui.shell.business.setup

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Display / storage helpers for Business Setup DateRow pickers.
 *
 * Callers: BusinessSetupDocumentKit.DateRow; FieldRow GSTIN uppercase via forceUppercaseInput;
 * BusinessSetupShell passes field keys into modeForKey/allowClearForKey.
 * No existing helper covers setup day vs planMonth storage; SetupDateTimeUtils is ISO-day only.
 * Schemas: preference strings startDate/endDate/gstEffectiveFrom (YYYY-MM-DD), planMonth (YYYY-MM), gstin uppercase.
 * User: "Implement the plan as specified" — Business Setup Date / Currency / GSTIN Controls.
 */
object BusinessSetupDateFormatting {
    enum class Mode { Day, Month }

    fun modeForKey(fieldKey: String): Mode =
        if (fieldKey == "planMonth") Mode.Month else Mode.Day

    fun allowClearForKey(fieldKey: String): Boolean = fieldKey == "endDate"

    fun forceUppercaseInput(raw: String): String = raw.uppercase(Locale.ROOT)

    fun formatDisplay(value: String, mode: Mode, placeholder: String): String {
        if (value.isBlank()) return placeholder
        return when (mode) {
            Mode.Day -> formatDayDisplay(value) ?: value
            Mode.Month -> formatMonthDisplay(value) ?: value
        }
    }

    fun parseToLocalDate(value: String): LocalDate? {
        val raw = value.trim()
        if (raw.isEmpty()) return null
        if (Regex("""^\d{4}-\d{2}-\d{2}""").containsMatchIn(raw)) {
            return runCatching { LocalDate.parse(raw.take(10)) }.getOrNull()
        }
        if (Regex("""^\d{4}-\d{2}$""").matches(raw)) {
            return runCatching { YearMonth.parse(raw).atDay(1) }.getOrNull()
        }
        val parts = raw.split(Regex("""\s+"""))
        if (parts.size == 3) {
            val day = parts[0].toIntOrNull() ?: return null
            val month = monthIndex(parts[1]) ?: return null
            val year = parts[2].toIntOrNull() ?: return null
            return runCatching { LocalDate.of(year, month, day) }.getOrNull()
        }
        if (parts.size == 2) {
            val month = monthIndex(parts[0]) ?: return null
            val year = parts[1].toIntOrNull() ?: return null
            return runCatching { YearMonth.of(year, month).atDay(1) }.getOrNull()
        }
        return null
    }

    fun toStorage(date: LocalDate, mode: Mode): String = when (mode) {
        Mode.Day -> date.format(DateTimeFormatter.ISO_LOCAL_DATE)
        Mode.Month -> YearMonth.from(date).format(DateTimeFormatter.ofPattern("yyyy-MM"))
    }

    private fun formatDayDisplay(value: String): String? {
        val date = parseToLocalDate(value) ?: return null
        return date.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH))
    }

    private fun formatMonthDisplay(value: String): String? {
        val date = parseToLocalDate(value) ?: return null
        return YearMonth.from(date).format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH))
    }

    private fun monthIndex(token: String): Int? {
        val key = token.lowercase(Locale.ENGLISH).take(3)
        return mapOf(
            "jan" to 1, "feb" to 2, "mar" to 3, "apr" to 4, "may" to 5, "jun" to 6,
            "jul" to 7, "aug" to 8, "sep" to 9, "oct" to 10, "nov" to 11, "dec" to 12,
        )[key]
    }
}

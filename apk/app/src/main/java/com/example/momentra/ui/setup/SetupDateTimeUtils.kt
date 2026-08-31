package com.example.momentra.ui.setup

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

object SetupDateTimeUtils {
    private val isoDate = DateTimeFormatter.ISO_LOCAL_DATE
    private val isoDateTime = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    fun formatDateDisplay(iso: String?): String {
        if (iso.isNullOrBlank()) return "Select date"
        return runCatching {
            LocalDate.parse(iso.take(10)).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
        }.getOrElse { iso }
    }

    fun formatDateTimeDisplay(iso: String?): String {
        if (iso.isNullOrBlank()) return "Select date & time"
        return runCatching {
            LocalDateTime.parse(iso).format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT))
        }.getOrElse { iso }
    }

    fun formatTimeDisplay(iso: String?): String {
        if (iso.isNullOrBlank()) return "Select time"
        return runCatching {
            LocalTime.parse(iso).format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
        }.getOrElse { iso }
    }

    fun localDateToIso(date: LocalDate): String = date.format(isoDate)

    fun localDateTimeToIso(date: LocalDate, time: LocalTime): String =
        LocalDateTime.of(date, time).format(isoDateTime)

    fun localTimeToIso(time: LocalTime): String = time.format(DateTimeFormatter.ISO_LOCAL_TIME)

    fun millisToLocalDate(millis: Long, zone: ZoneId = ZoneId.systemDefault()): LocalDate =
        Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()

    fun localDateToMillis(date: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Long =
        date.atStartOfDay(zone).toInstant().toEpochMilli()

    fun parseIsoDate(iso: String?): LocalDate? = runCatching {
        if (iso.isNullOrBlank()) null else LocalDate.parse(iso.take(10))
    }.getOrNull()

    fun parseIsoDateTime(iso: String?): Pair<LocalDate, LocalTime>? = runCatching {
        if (iso.isNullOrBlank()) return null
        val dt = LocalDateTime.parse(iso)
        dt.toLocalDate() to dt.toLocalTime()
    }.getOrNull()

    fun isoDateToStartInstant(iso: String?): String? = runCatching {
        if (iso.isNullOrBlank()) return null
        LocalDate.parse(iso.take(10)).atStartOfDay(ZoneId.of("UTC")).toInstant().toString()
    }.getOrNull()

    fun isoDateToEndInstant(iso: String?): String? = runCatching {
        if (iso.isNullOrBlank()) return null
        LocalDate.parse(iso.take(10)).atTime(23, 59).atZone(ZoneId.of("UTC")).toInstant().toString()
    }.getOrNull()

    fun formatDateRangeDisplay(startIso: String?, endIso: String?): String {
        val start = formatDateDisplay(startIso)
        val end = formatDateDisplay(endIso)
        if (startIso.isNullOrBlank() && endIso.isNullOrBlank()) return "Select dates"
        if (endIso.isNullOrBlank() || start == end) return start
        return "$start – $end"
    }
}

package com.example.momentra.ui.shell.business.service

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Formatting helpers for the retail section.
 *
 * The Figma frames always show money in Indian compact notation (`₹4.8L`, `₹18K`,
 * `₹1.18L`) — never a raw `480000.0000`. Missing money is `—`, never `₹0`.
 */
object ServiceFormat {

    /** Locale-aware compact money: `₹4.8L`, `₹18K`, `₹1.18L`, `₹42K`. */
    fun compact(currency: String?, raw: String?, visible: Boolean = true): String {
        if (!visible || raw.isNullOrBlank()) return "—"
        val value = raw.toBigDecimalOrNull() ?: return raw
        val symbol = if (currency.isNullOrBlank() || currency == "INR") "₹" else "$currency "
        val abs = value.abs()
        val sign = if (value.signum() < 0) "−" else ""
        val body = when {
            abs >= BigDecimal("10000000") -> trim(abs / BigDecimal("10000000")) + "Cr"
            abs >= BigDecimal("100000") -> trim(abs / BigDecimal("100000")) + "L"
            abs >= BigDecimal("1000") -> trim(abs / BigDecimal("1000")) + "K"
            else -> abs.setScale(0, RoundingMode.HALF_UP).toPlainString()
        }
        return "$sign$symbol$body"
    }

    /** Exact money for statements: `₹4,80,000`. */
    fun exact(currency: String?, raw: String?, visible: Boolean = true): String {
        if (!visibility(visible, raw)) return "—"
        val value = raw!!.toBigDecimalOrNull() ?: return raw
        val locale = if (currency.isNullOrBlank() || currency == "INR") Locale("en", "IN") else Locale.US
        val format = NumberFormat.getNumberInstance(locale).apply {
            minimumFractionDigits = 0
            maximumFractionDigits = 2
        }
        val symbol = if (currency.isNullOrBlank() || currency == "INR") "₹" else ""
        val suffix = if (symbol.isEmpty()) " ${currency.orEmpty()}" else ""
        return "$symbol${format.format(value)}$suffix"
    }

    private fun visibility(visible: Boolean, raw: String?) = visible && !raw.isNullOrBlank()

    private fun trim(value: BigDecimal): String {
        val scaled = value.setScale(2, RoundingMode.HALF_UP)
        return scaled.stripTrailingZeros().toPlainString()
    }

    /** `80%` from a part/whole pair. Returns `null` when the whole is zero — never `0%`. */
    fun percent(part: String?, whole: String?): String? {
        val p = part?.toBigDecimalOrNull() ?: return null
        val w = whole?.toBigDecimalOrNull() ?: return null
        if (w.signum() <= 0) return null
        val pct = p.divide(w, 4, RoundingMode.HALF_UP).multiply(BigDecimal(100))
        return "${pct.setScale(0, RoundingMode.HALF_UP).toPlainString()}%"
    }

    /** Fraction for a progress ring; `null` when there is no configured whole. */
    fun fraction(part: String?, whole: String?): Float? {
        val p = part?.toBigDecimalOrNull() ?: return null
        val w = whole?.toBigDecimalOrNull() ?: return null
        if (w.signum() <= 0) return null
        return (p / w).toFloat().coerceIn(0f, 1f)
    }

    /** `Nov 2026`. */
    fun month(iso: String?): String {
        val date = parse(iso) ?: LocalDate.now()
        return date.format(DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH))
    }

    /** `Nov 2026 · INR`. */
    fun periodLine(start: String?, currency: String?): String =
        "${month(start)} · ${currency?.ifBlank { null } ?: "INR"}"

    /** `29 Oct 2026`. */
    fun day(iso: String?): String {
        val date = parse(iso) ?: LocalDate.now()
        return date.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH))
    }

    /** `Today · 29 Oct 2026`. */
    fun todayLine(): String = "Today · ${day(LocalDate.now().toString())}"

    /** `02 Nov`. */
    fun shortDay(iso: String?): String? {
        val date = parse(iso) ?: return null
        return date.format(DateTimeFormatter.ofPattern("dd MMM", Locale.ENGLISH))
    }

    /** `As of 29 Oct`. */
    fun asOf(iso: String? = null): String = "As of ${shortDay(iso) ?: shortDay(LocalDate.now().toString())}"

    fun parse(iso: String?): LocalDate? {
        val text = iso?.take(10)?.ifBlank { null } ?: return null
        return runCatching { LocalDate.parse(text) }.getOrNull()
    }

    /** `186`, `12`, `₹1.2L` — a KPI value with the currency applied only to money. */
    fun count(value: Int): String = value.toString()

    /** Falls back to the exact formatter when the compact one would lose cents. */
    fun detail(currency: String?, detail: String?): String {
        if (detail.isNullOrBlank()) return "—"
        return Regex("""\d+\.\d{4}(?:\s+[A-Z]{3})?""").replace(detail) { match ->
            val token = match.value.substringBefore(' ')
            exact(currency, token, true)
        }
    }
}

/** Which rose tone a severity maps to, matching the Figma severity chips. */
fun serviceSeverityColor(severity: String?): androidx.compose.ui.graphics.Color = when (severity?.uppercase()) {
    "HIGH", "CRITICAL" -> com.example.momentra.ui.theme.ServicesTeal.negative
    "MEDIUM" -> com.example.momentra.ui.theme.ServicesTeal.warning
    else -> com.example.momentra.ui.theme.ServicesTeal.teal
}

/** Short label for an attention card (`Unavailable`, `Overdue`, `Alert`, `Reorder`, `Approval`). */
fun serviceSeverityLabel(kind: String?, severity: String?, hasSource: Boolean): String = when {
    !hasSource -> "Unavailable"
    kind?.uppercase()?.startsWith("OVERDUE") == true -> "Overdue"
    kind?.uppercase()?.startsWith("DUE") == true -> "Due"
    kind?.uppercase() == "APPROVAL" -> ("Approval")
    kind?.uppercase() == "LOW_STOCK" -> "Reorder"
    kind?.uppercase()?.contains("BUDGET") == true -> "Budget"
    kind?.uppercase() == "CONFLICT" -> "Conflict"
    kind?.uppercase() == "CHARGE_PENDING" -> "Charge"
    else -> if (severity?.uppercase() == "HIGH") "Alert" else "Open"
}

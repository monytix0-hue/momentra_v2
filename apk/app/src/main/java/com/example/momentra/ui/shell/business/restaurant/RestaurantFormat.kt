package com.example.momentra.ui.shell.business.restaurant

import androidx.compose.ui.graphics.Color
import com.example.momentra.ui.theme.RestaurantEmber
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Formatting helpers for the restaurant section.
 *
 * The Figma frames always show money in Indian compact notation (`₹3.2L`, `₹28K`,
 * `₹1.1L`) — never a raw `320000.0000`. Missing money is `—`, never `₹0`, and a
 * missing denominator is `Unavailable`, never `0%`.
 */
object RestaurantFormat {

    /** Locale-aware compact money: `₹3.2L`, `₹28K`, `₹1.1L`, `₹42K`. */
    fun compact(currency: String?, raw: String?, visible: Boolean = true): String {
        if (!visible || raw.isNullOrBlank()) return "—"
        val value = raw.toBigDecimalOrNull() ?: return "—"
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

    /** Exact money for statements: `₹3,20,000`. */
    fun exact(currency: String?, raw: String?, visible: Boolean = true): String {
        if (!visible || raw.isNullOrBlank()) return "—"
        val value = raw.toBigDecimalOrNull() ?: return "—"
        val locale = if (currency.isNullOrBlank() || currency == "INR") Locale("en", "IN") else Locale.US
        val format = NumberFormat.getNumberInstance(locale).apply {
            minimumFractionDigits = 0
            maximumFractionDigits = 2
        }
        val symbol = if (currency.isNullOrBlank() || currency == "INR") "₹" else ""
        val suffix = if (symbol.isEmpty()) " ${currency.orEmpty()}" else ""
        return "$symbol${format.format(value)}$suffix"
    }

    private fun trim(value: BigDecimal): String =
        value.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()

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

    /**
     * Client-side GST **estimate** from known line rates + amounts — never posts to ledger.
     * Returns null when a priced line lacks a parseable rate.
     */
    fun gstEstimate(
        lineAmounts: List<BigDecimal>,
        lineRates: List<String?>,
        inclusive: Boolean = true,
    ): BigDecimal? {
        if (lineAmounts.isEmpty()) return null
        var tax = BigDecimal.ZERO
        var sawPositive = false
        for (i in lineAmounts.indices) {
            val amount = lineAmounts.getOrNull(i) ?: BigDecimal.ZERO
            if (amount.signum() <= 0) continue
            sawPositive = true
            val raw = lineRates.getOrNull(i)?.trim().orEmpty()
            if (raw.isBlank()) return null
            if (raw.equals("Exempt", true) || raw == "0") continue
            val rate = raw.removeSuffix("%").toBigDecimalOrNull() ?: return null
            if (rate.signum() < 0) return null
            val piece = if (inclusive) {
                amount.multiply(rate).divide(BigDecimal(100).add(rate), 4, RoundingMode.HALF_UP)
            } else {
                amount.multiply(rate).divide(BigDecimal(100), 4, RoundingMode.HALF_UP)
            }
            tax = tax.add(piece)
        }
        return if (sawPositive) tax.setScale(2, RoundingMode.HALF_UP) else null
    }

    fun count(value: Int): String = value.toString()

    /** Replaces raw `1234.0000` tokens inside a board `detail` sentence. */
    fun detail(currency: String?, detail: String?): String {
        if (detail.isNullOrBlank()) return "—"
        return Regex("""\d+\.\d{4}(?:\s+[A-Z]{3})?""").replace(detail) { match ->
            val token = match.value.substringBefore(' ')
            exact(currency, token, true)
        }
    }
}

/** Ember colour for an attention severity, matching the Figma severity chips. */
fun restaurantSeverityColor(severity: String?): Color = when (severity?.uppercase()) {
    "HIGH", "CRITICAL" -> RestaurantEmber.negative
    "MEDIUM" -> RestaurantEmber.warning
    else -> RestaurantEmber.ember
}

/** Short label for an attention row: `Unavailable` when no source event exists. */
fun restaurantSeverityLabel(kind: String?, severity: String?, hasSource: Boolean): String {
    if (!hasSource) return "Unavailable"
    val upper = kind?.uppercase().orEmpty()
    return when {
        upper.startsWith("OVERDUE") -> "Overdue"
        upper.startsWith("DUE") -> "Due"
        upper == "APPROVAL" -> "Approval"
        upper == "LOW_STOCK" -> "Reorder"
        upper == "CONFLICT" -> "Conflict"
        upper == "CHARGE_PENDING" -> "Charge"
        upper.contains("BUDGET") -> "Budget"
        severity?.uppercase() == "HIGH" -> "Alert"
        else -> "Open"
    }
}

/** Human label for an operating event type, in kitchen language. */
fun restaurantEventLabel(code: String): String = when (code) {
    "SALE" -> "Bill"
    "PURCHASE" -> "Purchase"
    "SPEND" -> "Expense"
    "PAYMENT" -> "Payment"
    "COLLECTION" -> "Collection"
    "STOCK" -> "Stock move"
    "ORDER", "CUSTOMER_ORDER" -> "Order"
    "ISSUE" -> "Issue"
    "UPDATE" -> "Update"
    "APPROVAL" -> "Approval"
    "BUDGET" -> "Budget"
    "TARGET" -> "Target"
    "SHIFT" -> "Shift"
    "REVISION" -> "Revision"
    else -> code.lowercase().split('_').joinToString(" ") { word ->
        word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ENGLISH) else it.toString() }
    }
}

/** `Done` / `Open` / `Part` … for a payment or approval status. */
fun restaurantPrettyStatus(status: String): String = when (status) {
    "COLLECTED", "PAID", "SETTLED" -> "Done"
    "PENDING" -> "Open"
    "PARTIAL" -> "Part"
    "OVERDUE" -> "Overdue"
    "CANCELLED" -> "Void"
    else -> status.lowercase().replaceFirstChar { it.titlecase(Locale.ENGLISH) }
}

/** Order status → the kitchen-facing word (`PLACED` → `Sent`, `PREPARING` → `Firing`). */
fun restaurantOrderStatusLabel(status: String?): String = when (status?.uppercase().orEmpty()) {
    "PLACED" -> "Sent"
    "PREPARING" -> "Firing"
    "READY" -> "Ready"
    "COMPLETED" -> "Served"
    "CANCELLED" -> "Void"
    "ACCEPTED" -> "Accepted"
    else -> {
        val raw = status?.trim().orEmpty()
        if (raw.isEmpty()) "Open" else raw.lowercase().replaceFirstChar { it.titlecase(Locale.ENGLISH) }
    }
}

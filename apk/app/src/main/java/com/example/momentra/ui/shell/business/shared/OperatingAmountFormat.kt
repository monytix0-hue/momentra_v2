package com.example.momentra.ui.shell.business.shared

import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

fun formatOperatingAmount(currency: String?, raw: String?, visible: Boolean = true): String {
    if (!visible || raw.isNullOrBlank()) return "—"
    val value = raw.toBigDecimalOrNull() ?: return raw
    val locale = if (currency.isNullOrBlank() || currency == "INR") Locale("en", "IN") else Locale.US
    val format = NumberFormat.getNumberInstance(locale).apply {
        minimumFractionDigits = 0
        maximumFractionDigits = 2
    }
    val text = format.format(value)
    return if (currency.isNullOrBlank() || currency == "INR") "₹$text" else "$text $currency"
}

fun formatOperatingDetail(detail: String, currency: String?): String {
    return Regex("""\d+\.\d{4}(?:\s+[A-Z]{3})?""").replace(detail) { match ->
        val token = match.value.substringBefore(' ')
        formatOperatingAmount(currency, token, true)
    }
}

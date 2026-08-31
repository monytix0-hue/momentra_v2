package com.example.momentra.ui.shell.empty.group

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object GroupBudgetUtils {
    const val CUSTOM_OPTION = "Custom…"
    val PRESET_OPTIONS = listOf("₹80,000", "₹50,000", "₹1,20,000", "₹25,000", CUSTOM_OPTION)

    /** Parses display strings like `₹1,20,000` into API decimal `120000`. */
    fun parseDisplayToApiAmount(display: String): String? {
        val digits = display
            .replace("₹", "")
            .replace(",", "")
            .replace(" ", "")
            .trim()
        if (digits.isEmpty()) return null
        if (!digits.matches(Regex("^\\d+(\\.\\d{1,4})?$"))) return null
        return digits
    }

    fun resolveBudgetAmount(displayBudget: String, customAmount: String): String? {
        if (displayBudget == CUSTOM_OPTION) {
            return parseDisplayToApiAmount(customAmount)
        }
        return parseDisplayToApiAmount(displayBudget)
    }

    fun formatApiAmountForDisplay(amount: String, currencyCode: String = "INR"): String {
        val value = amount.toDoubleOrNull() ?: return amount
        val prefix = when (currencyCode) {
            "INR" -> "₹"
            "USD" -> "$"
            "EUR" -> "€"
            else -> "$currencyCode "
        }
        val symbols = DecimalFormatSymbols(Locale.US).apply { groupingSeparator = ',' }
        val pattern = if (value % 1.0 == 0.0) "#,##0" else "#,##0.00"
        return prefix + DecimalFormat(pattern, symbols).format(value)
    }

    fun formatCustomAmountInput(raw: String): String {
        val digits = raw.filter { it.isDigit() }
        if (digits.isEmpty()) return ""
        val num = digits.toLongOrNull() ?: return ""
        val symbols = DecimalFormatSymbols(Locale.US).apply { groupingSeparator = ',' }
        return DecimalFormat("#,###", symbols).format(num)
    }

    fun summaryLabel(displayBudget: String, customAmount: String): String {
        if (displayBudget == CUSTOM_OPTION) {
            val parsed = parseDisplayToApiAmount(customAmount)
            return if (parsed != null) formatApiAmountForDisplay(parsed) else "Custom amount"
        }
        return displayBudget
    }
}

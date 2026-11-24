package com.example.momentra.ui.shell.business.setup

/**
 * Live Indian amount grouping for Business Setup Amount fields.
 * Strips non-digits except one decimal point; groups the integer with
 * Indian commas (`##,##,###`); clamps fraction to at most 2 digits.
 */
object BusinessSetupAmountFormatting {

    fun isAmountHelper(helper: String): Boolean =
        helper == "Amount" || helper.startsWith("Amount ")

    fun plainAmount(raw: String): String = buildString {
        var dot = false
        for (c in raw) {
            if (c.isDigit()) append(c)
            else if (c == '.' && !dot) {
                append('.')
                dot = true
            }
        }
    }

    fun formatIndianAmount(raw: String): String {
        val cleaned = plainAmount(raw)
        if (cleaned.isEmpty()) return ""
        val dot = cleaned.indexOf('.')
        val intRaw = if (dot >= 0) cleaned.substring(0, dot) else cleaned
        // Keep the '.' plus at most 2 fraction digits (".5", ".50").
        val frac = if (dot >= 0) cleaned.substring(dot).take(3) else ""
        val digits = intRaw.trimStart('0').ifEmpty {
            if (intRaw.isNotEmpty() || frac.isNotEmpty()) "0" else ""
        }
        if (digits.isEmpty()) return ""
        return indianGroup(digits) + frac
    }

    private fun indianGroup(digits: String): String {
        if (digits.length <= 3) return digits
        val head = digits.dropLast(3)
        val tail = digits.takeLast(3)
        return head.reversed().chunked(2).joinToString(",").reversed() + "," + tail
    }
}

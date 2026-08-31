package com.example.momentra.domain.finance

/**
 * Decimal-safe amount helpers for Expense.Create.
 * Canonical amounts stay as strings — never Double/Float.
 */
object ExpenseMoney {
    private val OPENAPI_PATTERN = Regex("""^[0-9]+(\.[0-9]{1,4})?$""")

    /** Normalize user input for API: trim, strip leading zeros carefully, reject invalid. */
    fun normalize(raw: String): String? {
        val trimmed = raw.trim().replace(',', '.')
        if (trimmed.isEmpty()) return null
        if (trimmed.startsWith('-') || trimmed.startsWith('+')) return null
        val cleaned = trimmed.trimStart('0').let { if (it.startsWith('.') || it.isEmpty()) "0$it" else it }
            .removeSuffix(".")
            .ifEmpty { "0" }
        if (!OPENAPI_PATTERN.matches(cleaned)) return null
        return cleaned
    }

    fun isValidPositive(raw: String): Boolean {
        val n = normalize(raw) ?: return false
        return try {
            val parts = n.split('.')
            val whole = parts[0].toLong()
            val frac = parts.getOrNull(1)?.padEnd(4, '0')?.toLong() ?: 0L
            whole > 0 || frac > 0
        } catch (_: NumberFormatException) {
            false
        }
    }

    fun validateForSubmit(raw: String): String? {
        if (!isValidPositive(raw)) return null
        return normalize(raw)
    }
}

package com.example.momentra.ui.shell.group.shared

/**
 * Expense category chips keyed by group moment type code.
 * Labels match Maestro ledger Quick_Add values for correlation notes.
 */
object GroupExpenseCategoryCatalog {
    private val byType: Map<String, List<String>> = mapOf(
        // Shared Experience
        "TRIP" to listOf("Travel", "Stay", "Food", "Local Transport", "Activities", "Shopping", "Other"),
        "WEDDING" to listOf("Venue", "Catering", "Decor", "Photography", "Travel", "Gifts", "Other"),
        "HOUSE_PARTY" to listOf("Food", "Drinks", "Decor", "Supplies", "Other"),
        "OFFICE_OUTING" to listOf("Travel", "Venue", "Food", "Activities", "Other"),
        // Shared Purchase
        "GIFT_POOL" to listOf("Contribution", "Gift Purchase", "Delivery", "Other"),
        "GROUP_PURCHASE" to listOf("Item", "Shipping", "Tax", "Other"),
        "SHARED_ASSET" to listOf("Purchase", "Maintenance", "Repair", "Insurance", "Other"),
        "COMMUNITY_PURCHASE" to listOf("Custom Expense", "Custom Contribution"),
        // Shared Living
        "FLATMATES" to listOf("Rent", "Groceries", "Utilities", "Internet", "House Supplies", "Repairs", "Other"),
        "FAMILY_HOUSEHOLD" to listOf(
            "Utilities", "Rent", "Groceries", "Internet", "Maintenance", "Other",
        ),
        "CO_LIVING" to listOf("Rent", "Groceries", "Utilities", "Maintenance", "Other"),
        "COMMUNITY_LIVING" to listOf("Custom Expense", "Custom Contribution"),
        // DB aliases sometimes surfaced in older ledgers
        "SHARED_LIVING" to listOf("Rent", "Groceries", "Utilities", "Maintenance", "Other"),
    )

    private val fallback = listOf("Other")

    fun categories(momentTypeCode: String?): List<String> {
        val code = momentTypeCode?.trim()?.uppercase().orEmpty()
        if (code.isEmpty()) return fallback
        return byType[code] ?: fallback
    }

    fun defaultCategory(momentTypeCode: String?): String =
        categories(momentTypeCode).first()

    /** Embed category in description for API persistence (no category_code on group expenses yet). */
    fun descriptionWithCategory(category: String, userDescription: String): String {
        val note = userDescription.trim()
        val cat = category.trim().ifBlank { "Other" }
        if (note.isEmpty()) return cat
        if (note.contains(cat, ignoreCase = true)) return note
        // Maestro correlation form: "… | Category | amount" — keep category as a clear segment.
        return "$note | $cat"
    }

    /** Best-effort reverse of descriptionWithCategory for edit forms. */
    fun parseCategoryAndNote(description: String?, momentTypeCode: String?): Pair<String, String> {
        val cats = categories(momentTypeCode)
        val raw = description?.trim().orEmpty()
        if (raw.isEmpty()) return defaultCategory(momentTypeCode) to ""
        val sep = " | "
        val idx = raw.lastIndexOf(sep)
        if (idx >= 0) {
            val cat = raw.substring(idx + sep.length).trim()
            val match = cats.firstOrNull { it.equals(cat, ignoreCase = true) }
            if (match != null) {
                return match to raw.substring(0, idx).trim()
            }
        }
        cats.firstOrNull { it.equals(raw, ignoreCase = true) }?.let { return it to "" }
        return defaultCategory(momentTypeCode) to raw
    }
}

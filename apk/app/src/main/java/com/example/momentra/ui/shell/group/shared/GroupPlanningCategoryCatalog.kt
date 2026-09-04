package com.example.momentra.ui.shell.group.shared

/**
 * Planning category chips keyed by group moment type code.
 * Labels are shown in UI; [codeForLabel] / stored `category_code` uses UPPER_SNAKE.
 */
object GroupPlanningCategoryCatalog {
    data class Category(val label: String, val code: String)

    private val byType: Map<String, List<String>> = mapOf(
        "TRIP" to listOf("Travel", "Stay", "Food", "Day plan", "Activity", "Other"),
        "WEDDING" to listOf("Ceremony", "Venue", "Food", "Guests", "Travel", "Stay", "Day plan", "Other"),
        "HOUSE_PARTY" to listOf("Food", "Travel", "Hour plan", "Decor", "Music", "Other"),
        "OFFICE_OUTING" to listOf("Travel", "Stay", "Food", "Agenda", "Team", "Other"),
        // Shared Living
        "FLATMATES" to listOf("Chores", "Bills", "Maintenance", "House rules", "Shopping", "Other"),
        "FAMILY_HOUSEHOLD" to listOf("Chores", "Bills", "Maintenance", "House rules", "Shopping", "Other"),
        "CO_LIVING" to listOf("Chores", "Bills", "Maintenance", "House rules", "Shopping", "Other"),
        "COMMUNITY_LIVING" to listOf("Chores", "Bills", "Maintenance", "House rules", "Shopping", "Other"),
        "SHARED_LIVING" to listOf("Chores", "Bills", "Maintenance", "House rules", "Shopping", "Other"),
        // Shared Purchase
        "GIFT_POOL" to listOf("Funding", "Items", "Vendor", "Delivery", "Decision", "Other"),
        "GROUP_PURCHASE" to listOf("Funding", "Items", "Vendor", "Delivery", "Decision", "Other"),
        "SHARED_ASSET" to listOf("Funding", "Items", "Vendor", "Delivery", "Decision", "Other"),
        "COMMUNITY_PURCHASE" to listOf("Funding", "Items", "Vendor", "Delivery", "Decision", "Other"),
    )

    private val fallback = listOf("Other")

    fun labels(momentTypeCode: String?): List<String> {
        val code = momentTypeCode?.trim()?.uppercase().orEmpty()
        if (code.isEmpty()) return fallback
        return byType[code] ?: fallback
    }

    fun categories(momentTypeCode: String?): List<Category> =
        labels(momentTypeCode).map { Category(label = it, code = codeForLabel(it)) }

    fun defaultLabel(momentTypeCode: String?): String = labels(momentTypeCode).first()

    fun defaultCode(momentTypeCode: String?): String = codeForLabel(defaultLabel(momentTypeCode))

    fun codeForLabel(label: String): String =
        label.trim().uppercase()
            .replace(Regex("[^A-Z0-9]+"), "_")
            .trim('_')
            .ifBlank { "OTHER" }

    fun labelForCode(code: String?, momentTypeCode: String?): String {
        val normalized = code?.trim()?.uppercase().orEmpty()
        if (normalized.isEmpty()) return defaultLabel(momentTypeCode)
        return categories(momentTypeCode).firstOrNull { it.code == normalized }?.label
            ?: normalized.replace('_', ' ').lowercase().replaceFirstChar { it.titlecase() }
    }

    fun priorityCode(label: String): String = when (label.trim().lowercase()) {
        "low" -> "LOW"
        "high" -> "HIGH"
        else -> "MEDIUM"
    }

    fun urgencyCode(label: String): String =
        if (label.trim().equals("Urgent", ignoreCase = true)) "URGENT" else "NORMAL"
}

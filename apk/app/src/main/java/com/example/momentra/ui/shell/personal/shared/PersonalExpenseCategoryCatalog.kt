package com.example.momentra.ui.shell.personal.shared

/** Static category catalog for Master Expense + Edit Transaction (no backend catalog yet). */
object PersonalExpenseCategoryCatalog {
    data class Category(
        val code: String,
        val label: String,
        val emoji: String,
        val subcategories: List<Subcategory>,
    )

    data class Subcategory(
        val code: String,
        val label: String,
    )

    val masterCategories: List<Category> = listOf(
        Category("FOOD", "Food", "🍕", listOf(
            Subcategory("FOOD_DINING", "Food & Dining"),
            Subcategory("CAFE", "Cafe"),
            Subcategory("GROCERIES", "Groceries"),
            Subcategory("RESTAURANT", "Restaurant"),
        )),
        Category("TRANSPORT", "Transport", "🚗", listOf(
            Subcategory("TRANSPORT", "Transport"),
            Subcategory("FUEL", "Fuel"),
            Subcategory("RIDE", "Ride share"),
        )),
        Category("SHOPPING", "Shopping", "🛍️", listOf(
            Subcategory("SHOPPING", "Shopping"),
            Subcategory("CLOTHING", "Clothing"),
            Subcategory("ELECTRONICS", "Electronics"),
        )),
        Category("CAFE", "Cafe", "☕", listOf(
            Subcategory("CAFE", "Cafe"),
            Subcategory("COFFEE", "Coffee"),
        )),
        Category("HEALTH", "Health", "💊", listOf(
            Subcategory("HEALTH", "Health"),
            Subcategory("PHARMACY", "Pharmacy"),
            Subcategory("FITNESS", "Fitness"),
        )),
        Category("ENTERTAINMENT", "Entertainment", "🎬", listOf(
            Subcategory("ENTERTAINMENT", "Entertainment"),
            Subcategory("STREAMING", "Streaming"),
            Subcategory("EVENTS", "Events"),
        )),
        Category("BILLS", "Bills", "🧾", listOf(
            Subcategory("BILLS", "Bills"),
            Subcategory("UTILITIES", "Utilities"),
            Subcategory("RENT", "Rent"),
            Subcategory("HOUSING", "Housing"),
        )),
        Category("OTHER", "Other", "📦", listOf(
            Subcategory("OTHER", "Other"),
            Subcategory("MISC", "Miscellaneous"),
        )),
    )

    fun categoryForCode(code: String?): Category? {
        val normalized = code?.uppercase()?.substringBefore('_') ?: return null
        return masterCategories.find {
            it.code == normalized || it.subcategories.any { s -> s.code == code?.uppercase() }
        }
    }

    fun labelForCode(code: String?): String {
        if (code.isNullOrBlank()) return "Other"
        val upper = code.uppercase()
        masterCategories.forEach { cat ->
            cat.subcategories.find { it.code == upper }?.let { return it.label }
            if (cat.code == upper) return cat.label
        }
        return code.replace('_', ' ').lowercase().replaceFirstChar { it.titlecase() }
    }

    fun subcategoryLabel(code: String?): String? {
        if (code.isNullOrBlank()) return null
        val upper = code.uppercase()
        masterCategories.forEach { cat ->
            cat.subcategories.find { it.code == upper }?.let { return it.label }
        }
        if (upper.contains('_')) {
            return upper.substringAfter('_').replace('_', ' ').lowercase()
                .replaceFirstChar { it.titlecase() }
        }
        return null
    }

    /** Encode category + subcategory for API categoryCode. */
    fun encodeCategoryCode(categoryCode: String, subcategoryCode: String?): String {
        val sub = subcategoryCode?.takeIf { it.isNotBlank() } ?: return categoryCode.uppercase()
        return sub.uppercase()
    }

    fun decodeFromStored(stored: String?): Pair<String, String?> {
        if (stored.isNullOrBlank()) return "OTHER" to null
        val upper = stored.uppercase()
        masterCategories.forEach { cat ->
            cat.subcategories.find { it.code == upper }?.let {
                return cat.code to it.code
            }
        }
        return upper to null
    }
}

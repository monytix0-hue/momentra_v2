package com.example.momentra.ui.shell.group.shared

/**
 * Shared Experience Checklist categories stored on collaboration.planning_item.category_code.
 * Codes and labels are exact product strings — do not derive via [GroupPlanningCategoryCatalog.codeForLabel].
 */
object GroupExperienceChecklistCatalog {
    data class Category(val code: String, val label: String)

    val categories: List<Category> = listOf(
        Category("DOCUMENTS_MONEY", "Documents & Money"),
        Category("TRAVEL_ESSENTIALS", "Travel Essentials"),
        Category("MEDICINES_HEALTH", "Medicines & Health Kit"),
        Category("CLOTHING", "Clothing"),
    )

    val codes: Set<String> = categories.map { it.code }.toSet()

    fun isChecklistCode(code: String?): Boolean {
        val normalized = code?.trim()?.uppercase().orEmpty()
        return normalized.isNotEmpty() && normalized in codes
    }

    fun labelForCode(code: String?): String {
        val normalized = code?.trim()?.uppercase().orEmpty()
        return categories.firstOrNull { it.code == normalized }?.label
            ?: normalized.replace('_', ' ').lowercase().replaceFirstChar { it.titlecase() }
    }

    fun defaultCode(): String = categories.first().code

    fun defaultLabel(): String = categories.first().label

    /** Seed packing list from pilgrimage reference: categoryCode → titles. */
    val seedPackingList: List<Pair<String, String>> = listOf(
        "DOCUMENTS_MONEY" to "Aadhaar / ID Proof",
        "DOCUMENTS_MONEY" to "Train / Flight Tickets (Print + Mobile)",
        "DOCUMENTS_MONEY" to "Hotel Booking Details",
        "DOCUMENTS_MONEY" to "Cash (small denominations)",
        "DOCUMENTS_MONEY" to "ATM / Debit Card",
        "DOCUMENTS_MONEY" to "Emergency Contact Numbers",
        "TRAVEL_ESSENTIALS" to "Mobile Phone",
        "TRAVEL_ESSENTIALS" to "Charger",
        "TRAVEL_ESSENTIALS" to "Power Bank",
        "TRAVEL_ESSENTIALS" to "Water Bottle",
        "TRAVEL_ESSENTIALS" to "Small Backpack (Day Use)",
        "TRAVEL_ESSENTIALS" to "Snacks (Biscuits, Dry Fruits)",
        "TRAVEL_ESSENTIALS" to "Travel Pillow / Shawl",
        "TRAVEL_ESSENTIALS" to "Umbrella / Raincoat",
        "MEDICINES_HEALTH" to "Paracetamol (Fever)",
        "MEDICINES_HEALTH" to "Cold Tablets",
        "MEDICINES_HEALTH" to "Pain Relief Tablets / Spray",
        "MEDICINES_HEALTH" to "Acidity Tablets",
        "MEDICINES_HEALTH" to "ORS Packets",
        "MEDICINES_HEALTH" to "Loose Motion Tablets",
        "MEDICINES_HEALTH" to "Vomiting Medicine",
        "MEDICINES_HEALTH" to "Personal Medicines",
        "MEDICINES_HEALTH" to "Doctor Prescription",
        "MEDICINES_HEALTH" to "Band-aid / Antiseptic Cream",
        "MEDICINES_HEALTH" to "Hand Sanitizer",
        "MEDICINES_HEALTH" to "Masks",
        "CLOTHING" to "Shirts / T-shirts (3–4)",
        "CLOTHING" to "Pants / Track Pants (2–3)",
        "CLOTHING" to "Traditional Wear",
        "CLOTHING" to "Undergarments",
        "CLOTHING" to "Nightwear",
        "CLOTHING" to "Walking Shoes",
        "CLOTHING" to "Slippers / Sandals",
        "CLOTHING" to "Toothbrush & Paste",
    )

    fun checklistItems(items: List<com.example.momentra.data.api.GroupLifePlanningItemDto>) =
        items.filter { isChecklistCode(it.categoryCode) }

    fun nonChecklistItems(items: List<com.example.momentra.data.api.GroupLifePlanningItemDto>) =
        items.filterNot { isChecklistCode(it.categoryCode) }

    /** Group checklist items by category order; skip empty categories. */
    fun groupedByCategory(
        items: List<com.example.momentra.data.api.GroupLifePlanningItemDto>,
    ): List<Pair<Category, List<com.example.momentra.data.api.GroupLifePlanningItemDto>>> {
        val checklist = checklistItems(items)
        return categories.mapNotNull { cat ->
            val group = checklist.filter {
                it.categoryCode?.trim()?.equals(cat.code, ignoreCase = true) == true
            }
            if (group.isEmpty()) null else cat to group
        }
    }
}

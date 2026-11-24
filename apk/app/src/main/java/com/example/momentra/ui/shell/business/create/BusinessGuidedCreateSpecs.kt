package com.example.momentra.ui.shell.business.create

/**
 * Copy for Figma `Create a moment · Guided business selection` (`2:1514`).
 *
 * Keep in lockstep with `BusinessGuidedCreateSpecs.swift` on iOS.
 */
enum class BusinessGuidedCreateOption(
    val id: String,
    val titleLine1: String,
    val titleLine2: String,
    val meaning: String,
    val iconKey: String,
    val familyCode: String,
    val momentTypeCode: String,
    val previewEyebrow: String,
    val previewHeadline: String,
    val previewBody: String,
    val capabilities: List<Pair<String, String>>,
) {
    Marketing(
        id = "marketing",
        titleLine1 = "Marketing &",
        titleLine2 = "Campaigns",
        meaning = "Ads, promotions & leads",
        iconKey = "megaphone",
        familyCode = "MARKETING_CAMPAIGNS",
        momentTypeCode = "CLIENT_CAMPAIGN",
        previewEyebrow = "YOUR MARKETING MOMENT",
        previewHeadline = "Built for the way you promote.",
        previewBody = "Run client campaigns or your own ads with budgets, channels, and lead tracking in one place.",
        capabilities = listOf(
            "wallet" to "Campaign budgets",
            "target" to "Lead tracking",
            "users" to "Client retainers",
            "badge-check" to "Creative approvals",
        ),
    ),
    Trading(
        id = "trading",
        titleLine1 = "Trading &",
        titleLine2 = "Distribution",
        meaning = "Bulk buying & selling",
        iconKey = "truck",
        familyCode = "TRADING_DISTRIBUTION",
        momentTypeCode = "TRADING_WHOLESALE",
        previewEyebrow = "YOUR TRADING MOMENT",
        previewHeadline = "Built for the way you move goods.",
        previewBody = "Buy in bulk, sell wholesale, and track collections across dealers and routes.",
        capabilities = listOf(
            "receipt" to "Purchase orders",
            "route" to "Shipments & tracking",
            "banknote" to "Collections",
            "package" to "Stock positions",
        ),
    ),
    Retail(
        id = "retail",
        titleLine1 = "Retail &",
        titleLine2 = "Consumer",
        meaning = "Shop & counter sales",
        iconKey = "store",
        familyCode = "RETAIL_CONSUMER",
        momentTypeCode = "RETAIL_STORE",
        previewEyebrow = "YOUR RETAIL MOMENT",
        previewHeadline = "Built for the way you sell.",
        previewBody = "Counter sales, inventory, and day-close numbers for a shop or consumer brand.",
        capabilities = listOf(
            "camera" to "POS sales",
            "package" to "Shelf stock",
            "users" to "Customer tabs",
            "calculator" to "Day close",
        ),
    ),
    Manufacturing(
        id = "manufacturing",
        titleLine1 = "Manufacturing &",
        titleLine2 = "Production",
        meaning = "Batch & job work",
        iconKey = "factory",
        familyCode = "MANUFACTURING_PRODUCTION",
        momentTypeCode = "STANDARD_PRODUCTION",
        previewEyebrow = "YOUR MANUFACTURING MOMENT",
        previewHeadline = "Built for the way you make.",
        previewBody = "Make products in batches from raw materials or manage made-to-order job work.",
        capabilities = listOf(
            "clipboard-list" to "Quotes & job orders",
            "factory" to "Production & WIP",
            "layers" to "BOM & materials",
            "receipt-text" to "Invoices against orders",
        ),
    ),
    ;

    val title: String get() = "$titleLine1 $titleLine2"
}

object BusinessGuidedCreateSpecs {
    const val QUESTION_LINE_1 = "What kind of"
    const val QUESTION_LINE_2 = "business do you run?"
    const val SUBTITLE = "Choose a type. We'll tailor your moment."
    const val CONTINUE_LABEL = "Continue"
    const val FOOTNOTE = "You can change this later in Manage moment."

    val options: List<BusinessGuidedCreateOption> = BusinessGuidedCreateOption.entries

    fun option(id: String): BusinessGuidedCreateOption =
        options.firstOrNull { it.id == id } ?: BusinessGuidedCreateOption.Manufacturing
}

package com.example.momentra.ui.shell.business.restaurant

/**
 * Figma-locked Restaurant money / goods QA copy (ember Spice Route chrome).
 *
 * Nodes: `2400:30129` Purchase · `2400:30193` Collection · `2400:30257` Payment ·
 * `2400:30310` Expense · `2400:30363` Stock. Never reuse Service teal or Retail rose
 * Why / CTA wording.
 */
object RestaurantCaptureCopy {

    const val TAGLINE = "Order. Serve. Collect."
    const val DRAFT_FOOTER = "Draft preserved if you close or open a picker"
    const val DRAFT_UNTIL_POSTED = "Saved as draft until posted"
    const val SAVED_TOAST = "✓ Saved · sheet closes and returns back"
    const val SCAN_PLACEHOLDER = "INV-8841 · Fresh Farm"
    const val STOCK_SCAN_PLACEHOLDER = "Paneer · Cold store"
    const val MORE_DETAILS_HINT = "Notes · Proof · Tags"

    /** Mode chips under the sheet title. */
    fun modeChips(mode: String): List<String> = when (mode.uppercase()) {
        "PURCHASE", "VENDOR" -> listOf("Ingredients", "Supplies")
        "COLLECTION" -> listOf("Platform", "Customer")
        "PAYMENT", "REIMBURSE" -> listOf("Supplier", "Other")
        "SPEND", "EXPENSE" -> listOf("Kitchen", "Platform", "Running")
        "STOCK" -> listOf("Count", "Waste", "Usage")
        "ORDER", "SALES_ORDER", "PURCHASE_ORDER" -> listOf("Sales Order", "Purchase Order")
        else -> emptyList()
    }

    val soCategories = listOf("Mains", "Breads", "Starters", "Drinks", "Dessert")
    val poCategories = listOf("Dairy", "Grains", "Oils", "Spices", "Produce", "Pack")

    val soChannels = listOf("Dine-in", "Takeaway", "Catering")
    val soStatusChips = listOf("Preparing", "Placed", "Ready")
    val poStatusChips = listOf("Open", "Partial", "Received")

    const val SO_COMMITMENT_FOOTER = "Draft preserved · Send KOT from ticket after save"
    const val PO_COMMITMENT_FOOTER = DRAFT_FOOTER
    const val SO_BROWSER_TRAILING = "Search · Scan"
    const val PO_BROWSER_TRAILING = "Raw · scan"
    const val SO_NEW_DISH = "+ New dish"
    const val PO_NEW_RAW = "+ New raw material"

    fun sheetTitle(mode: String): String = when (mode.uppercase()) {
        "PURCHASE", "VENDOR" -> "Record Purchase"
        "COLLECTION" -> "Record Collection"
        "PAYMENT", "REIMBURSE" -> "Record Payment"
        "SPEND", "EXPENSE" -> "Record Expense"
        "STOCK" -> "Stock Update"
        "SALES_ORDER", "ORDER_SALES" -> "Sales Order"
        "PURCHASE_ORDER", "ORDER_PURCHASE" -> "Purchase Order"
        else -> mode
    }

    fun why(mode: String): String = when (mode.uppercase()) {
        "PURCHASE", "VENDOR" ->
            "Stock += accepted qty only. No automatic recipe deduction (v5 exclusion)."
        "COLLECTION" ->
            "Platform receivable needs explicit settlement — fees must not duplicate cost."
        "PAYMENT", "REIMBURSE" ->
            "Settles open AP only — never invents a purchase."
        "SPEND", "EXPENSE" ->
            "Kitchen/platform running cost; payment state separate from the expense fact."
        "STOCK" ->
            "Quantity delta only — never invent recipe consumption."
        "SALES_ORDER", "ORDER_SALES" ->
            "Menu Add only builds the commitment cart. Revenue posts on bill / POS Sale. KOT send is separate and never posts."
        "PURCHASE_ORDER", "ORDER_PURCHASE" ->
            "Raw material Add builds PO lines only. AP and stock post on Purchase / GRN receive — never on save PO. Recipe deduction still excluded in v5."
        else ->
            "One capture posts one event. Collection/payment allocate — never duplicate facts."
    }

    fun cta(mode: String, amountLabel: String? = null): String {
        val amount = amountLabel?.takeIf { it.isNotBlank() }
        return when (mode.uppercase()) {
            "PURCHASE", "VENDOR" -> if (amount != null) "Post purchase · $amount" else "Post purchase"
            "COLLECTION" -> if (amount != null) "Post collection · $amount" else "Post collection"
            "PAYMENT", "REIMBURSE" -> if (amount != null) "Post payment · $amount" else "Post payment"
            "SPEND", "EXPENSE" -> if (amount != null) "Post expense · $amount" else "Post expense"
            "STOCK" -> "Post stock update"
            "SALES_ORDER", "ORDER_SALES" -> if (amount != null) "Save sales order · $amount" else "Save sales order"
            "PURCHASE_ORDER", "ORDER_PURCHASE" -> if (amount != null) "Save purchase order · $amount" else "Save purchase order"
            else -> "Save"
        }
    }

    fun amountLabel(mode: String): String = when (mode.uppercase()) {
        "PURCHASE", "VENDOR" -> "Purchase total"
        "COLLECTION" -> "Collect now"
        "PAYMENT", "REIMBURSE" -> "Pay now"
        "SPEND", "EXPENSE" -> "Expense"
        "STOCK" -> "Qty delta"
        "SALES_ORDER", "ORDER_SALES" -> "Order value"
        "PURCHASE_ORDER", "ORDER_PURCHASE" -> "PO value"
        else -> "Amount"
    }

    fun footerCaveat(mode: String): String = when (mode.uppercase()) {
        "PURCHASE", "VENDOR" -> DRAFT_FOOTER
        else -> DRAFT_UNTIL_POSTED
    }

    fun purchaseSubtitle(): String = "Receive ingredients · stock on accept"

    fun salesOrderSubtitle(kitchen: String, owner: String): String =
        "Customer commitment · add from menu"

    fun purchaseOrderSubtitle(kitchen: String, owner: String): String =
        "Supplier commitment · add raw materials"

    fun salesOrderHeroCaption(): String = "Commitment only · not revenue"

    fun purchaseOrderHeroCaption(): String = "Commitment only · not AP yet"

    fun collectionSubtitle(
        settleLabel: String,
        kitchen: String,
        owner: String,
    ): String = listOf(settleLabel, "Platform AR", kitchen, owner)
        .filter { it.isNotBlank() }
        .joinToString(" · ")

    fun paymentSubtitle(
        supplier: String,
        kitchen: String,
        owner: String,
    ): String = listOf(
        supplier.ifBlank { "Supplier" },
        "open AP",
        kitchen,
        owner,
    ).joinToString(" · ")

    fun expenseSubtitle(
        type: String,
        payee: String,
        kitchen: String,
        owner: String,
    ): String = listOf(
        type.ifBlank { "Expense" },
        payee,
        kitchen,
        owner,
    ).filter { it.isNotBlank() }.joinToString(" · ")

    fun stockSubtitle(
        item: String,
        status: String,
        kitchen: String,
        owner: String,
    ): String = listOf(
        item.ifBlank { "Item" },
        status,
        kitchen,
        owner,
    ).filter { it.isNotBlank() }.joinToString(" · ")

    /** Brand strip for wine context chip — Figma `Spice Route POS`. */
    fun posBrand(modelTitle: String?): String {
        val brand = brandOnly(modelTitle)
        return "$brand POS"
    }

    fun kitchenLine(modelTitle: String?, location: String?): String {
        val brand = brandOnly(modelTitle)
        val loc = location?.takeIf { it.isNotBlank() }
        return when {
            loc != null && loc.contains("Kitchen", ignoreCase = true) -> loc
            loc != null && !brand.contains(loc, ignoreCase = true) -> "$brand $loc"
            else -> "$brand Kitchen"
        }
    }

    fun ownerHint(model: String?): String =
        model?.takeIf { it.isNotBlank() } ?: "Kiran Rao"

    fun brandOnly(title: String?): String {
        val raw = title?.takeIf { it.isNotBlank() } ?: "Spice Route"
        return when {
            raw.contains("Spice Route", ignoreCase = true) -> "Spice Route"
            raw.contains(' ') -> raw.substringBeforeLast(' ').ifBlank { raw }
            else -> raw
        }
    }

    /** Map Count / Waste / Usage → contract stockAction. */
    fun stockActionCode(mode: String): String = when (mode.trim().lowercase()) {
        "count" -> "SET"
        "waste", "usage" -> "REDUCE"
        else -> "SET"
    }

    /** Map Count / Waste / Usage → accepted stockReason. */
    fun stockReasonCode(mode: String): String = when (mode.trim().lowercase()) {
        "count" -> "stocktake"
        "waste" -> "waste"
        "usage" -> "correction"
        else -> "stocktake"
    }

    // ── Phase 7 Budget / Issue (`2400:30547`, `2400:30605`) ─────────────────

    const val BUDGET_FIXTURE_PREVIOUS = "350000"
    const val BUDGET_FIXTURE_NEW = "400000"
    const val BUDGET_FIXTURE_COVERS_PREV = "86"
    const val BUDGET_FIXTURE_COVERS_NEW = "100"
    const val BUDGET_EFFECTIVE = "01 Nov 2026"
    const val ISSUE_FIXTURE_TITLE = "Spoilage · dal"
    const val ISSUE_FIXTURE_META = "2 kg · Prep"
    const val ISSUE_FIXTURE_LINK = "Stock waste"
    const val ISSUE_FIXTURE_OWNER = "Chef Ama"
    const val ISSUE_FIXTURE_DETAIL = "Spoilage · dal 2kg"
    val issueSeverities = listOf("Alert", "Watch", "Info")

    fun budgetModeChips(): List<String> = listOf("Sales", "Covers")

    fun issueModeChips(): List<String> = listOf("Issue", "Approval", "Update")

    fun budgetSubtitle(kitchen: String, owner: String, metric: String = "Sales"): String =
        "$kitchen · $owner · ${if (metric == "Covers") "Covers target" else "Sales target"}"

    fun issueSubtitle(kitchen: String, owner: String): String =
        "$kitchen · $owner · Spoilage · dal"

    fun budgetWhy(): String =
        "Versioned + effective-dated. Achievement uses posted bills only."

    fun issueWhy(): String =
        "Audit / action — approval ≠ disbursement."

    fun budgetCta(): String = "Save revision"

    fun issueCta(kind: String): String = when (kind) {
        "Approval" -> "Post approval"
        "Update" -> "Post update"
        else -> "Post issue"
    }

    // ── Phase 6 Masters Why / CTAs ──────────────────────────────────────────

    const val MENU_WHY =
        "Menu lists saved masters. GST badge is the dish rate (Restaurant 5%); tax posts on bill / POS Sale only."
    const val VENDORS_WHY =
        "Vendor is a master write. GSTIN here drives input tax credit only when a Purchase / GRN posts — never on PO save."
    const val STATIONS_WHY =
        "Stations are KOT routing targets. Busy = open tickets over threshold; status is live, not a setting."
    const val INVITE_WHY =
        "Invite grants a role and its access. Joining ≠ approval ≠ payment."
    const val DISH_WHY =
        "Create Dish is a master write. Sales Order only picks saved dishes. GST rate here drives bill tax; posting still happens on bill / POS Sale only."
    const val RAW_WHY =
        "Create Raw Material is a master write. Purchase Order only references saved items; input GST and stock post on Purchase / GRN — never on save PO."
    const val VENDOR_FORM_WHY =
        "Vendor is a master write. GSTIN state code decides CGST+SGST vs IGST on purchases."
    const val STATION_FORM_WHY =
        "Stations are KOT routing targets. Dishes pick a station in Create menu dish."

    val menuCategories = soCategories
    val rawCategories = poCategories
    val vendorCategories = listOf("Produce", "Dairy", "Dry", "Meat & Fish", "Beverages", "Packaging")
    val stationTypes = listOf("Kitchen", "Service", "Bar")
    val stationPrinters = listOf("Kitchen printer 1", "Bar printer", "Screen only")
    val dishVariants = listOf("Half / Full", "Single", "Size set")
    val dishGstChips = listOf("Rest. 5%", "18% + ITC", "Exempt")
    val dishTypes = listOf("Veg", "Non-veg", "Egg")
    val kitchenStations = listOf("Tandoor", "Hot", "Cold", "Bar")
    val rawUnits = listOf("kg", "L", "pcs", "bag", "can")
    val rawGstChips = listOf("0%", "5%", "12%", "18%")

    // ── Phase 8 adjacent HTR ────────────────────────────────────────────────

    const val HOLD_WHY =
        "Hold parks a draft order on this device. No revenue, no stock, no KOT until resume / bill."
    const val TRANSFER_WHY =
        "Transfer moves stock between stations at cost. No revenue and no GST within the same GSTIN."
    const val REPRINT_RECEIPT_WHY =
        "Reprint re-renders an existing posted bill. Never allocates a new bill id or posts revenue."
}

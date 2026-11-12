package com.example.momentra.ui.shell.business.retail

/**
 * Mirrored Figma RTL-QA money / capture copy catalog (rose Harbor Mart chrome).
 *
 * Parallel to [com.example.momentra.ui.shell.business.shared.ServiceCaptureCopy] but
 * RETAIL rose — never reuse Service teal Why / CTAs. Locked to nodes
 * `2399:30130` Purchase · `2399:30194` Collection · `2399:30253` Payment ·
 * `2443:29784` PO · `2399:30418` SO · `2399:30359` Stock · `2399:30482` Status ·
 * `2399:30539` Budget · `2399:30597` Issue.
 */
object RetailCaptureCopy {

    const val TAGLINE = "Stock. Sell. Settle."
    const val DRAFT_FOOTER = "Draft preserved if you close or open a picker"
    const val SAVED_TOAST = "✓ Saved · sheet closes and returns back"
    const val MORE_DETAILS_HINT = "Notes · Proof · Tags"
    const val PURCHASE_MORE_HINT = "Supplier GSTIN 27 · intra-state · Regular"
    const val SCAN_PLACEHOLDER = "INV-8841  ·  tap to scan"
    const val STOCK_SCAN_PLACEHOLDER = "HM-044  ·  tap to scan"

    const val SO_BROWSER_TRAILING = "Tap Add · set qty on line"
    const val PO_BROWSER_TRAILING = "Master + on-hand"
    const val SO_ADD_LINE_HINT = "From favourites · Price list"
    const val PO_ADD_LINE_HINT = "From supplier catalogue"
    const val STOCK_ADD_SKU_HINT = "Recent · Low stock"
    const val STATUS_MORE_HINT = "Notes · Photo · Tags"
    const val STOCK_MORE_HINT = "Transfer stock · between locations"
    const val SO_MORE_HINT = "Notes · Delivery · Tags"
    const val PO_MORE_HINT = "Notes · Terms · Tags"
    /** RTL-QA-09 More details hint. */
    const val BUDGET_MORE_HINT = "Notes · Attachment · Tags"
    /** RTL-QA-10 More details hint. */
    const val ISSUE_MORE_HINT = "Notes · Evidence · Tags"

    const val SO_COMMITMENT_FOOTER = "Commitment only · revenue, stock & GST post on Sale / POS"
    const val PO_COMMITMENT_FOOTER = "Commitment only · AP, stock & input GST post on Purchase / GRN"

    /** Harbor Mart Budget fixture — previous plan amount when projection is blank. */
    const val BUDGET_FIXTURE_PREVIOUS = "500000"
    /** Harbor Mart Budget fixture — revised amount shown in Figma. */
    const val BUDGET_FIXTURE_NEW = "600000"
    const val BUDGET_FIXTURE_REASON = "Festive demand"
    const val BUDGET_FIXTURE_OWNER = "Kiran Rao"
    const val ISSUE_FIXTURE_TITLE = "POS shift unreconciled"
    const val ISSUE_FIXTURE_META = "Cash vs UPI variance"
    const val ISSUE_FIXTURE_CODE = "REG-2"
    const val ISSUE_FIXTURE_OWNER = "Kiran Rao"
    const val ISSUE_FIXTURE_LINK = "Register 2 · today"

    val soCategories = listOf("Staples", "Snacks", "Personal care", "Beverages", "Home care")
    val poCategories = listOf("Staples", "Beverages", "Dairy", "Packaging", "Home care", "Consumables")
    val issueSeverities = listOf("Alert", "Warn", "Info")

    /** Mode chips under the sheet title. Empty = no chips. */
    fun modeChips(mode: String): List<String> = when (mode.uppercase()) {
        "PURCHASE", "VENDOR" -> listOf("Goods", "Supplies")
        "SALE", "POS" -> listOf("Scan cart", "Hold recall", "Quick amount")
        "STOCK" -> listOf("Set current", "Add", "Reduce")
        "ORDER", "SALES_ORDER", "PURCHASE_ORDER" -> listOf("Sales order", "Purchase order")
        "STATUS" -> listOf("Dispatch", "Fulfil", "Pick")
        "BUDGET", "TARGET" -> listOf("Sales target", "Budget")
        "ISSUE", "UPDATE", "APPROVAL" -> listOf("Issue", "Approval", "Update")
        "HOLD", "TRANSFER", "REPRINT" -> listOf("Hold", "Transfer", "Reprint")
        else -> emptyList()
    }

    fun sheetTitle(mode: String): String = when (mode.uppercase()) {
        "SALE", "POS" -> "POS Sale"
        "PURCHASE", "VENDOR" -> "Record Purchase"
        "COLLECTION" -> "Record Collection"
        "PAYMENT", "REIMBURSE" -> "Record Payment"
        "SPEND" -> "Record Expense"
        "STOCK" -> "Stock Update"
        "ORDER", "SALES_ORDER" -> "Sales Order"
        "PURCHASE_ORDER" -> "Purchase Order"
        "STATUS" -> "Status / Assignment"
        "BUDGET", "TARGET" -> "Budget / Target Revision"
        "ISSUE", "UPDATE", "APPROVAL" -> "Issue / Approval / Update"
        "HOLD" -> "Hold / Park bill"
        "TRANSFER" -> "Stock transfer"
        "REPRINT" -> "Reprint receipt"
        else -> mode
    }

    fun why(mode: String): String = when (mode.uppercase()) {
        "PURCHASE", "VENDOR" ->
            "Stock += accepted qty only on receipt. AP from posted purchase; payment allocates later — never double-cost."
        "COLLECTION" ->
            "Allocates open AR — never creates new sales. Receipt evidence ≠ separate revenue."
        "PAYMENT", "REIMBURSE" ->
            "Settles open AP only — never posts new purchase cost."
        "SPEND" ->
            "Expense is running cost; payment state is separate. Never invent stock from expense."
        "STOCK" ->
            "Quantity delta only — never invent unit cost or BOM. Damage uses Reduce + reason."
        "ORDER", "SALES_ORDER" ->
            "Sales order is commitment only — revenue + stock on posted Sale / POS Charge. Hold parks draft. Never posts GST as sale by itself."
        "PURCHASE_ORDER" ->
            "Purchase order is commitment only — no AP, no stock, no GST posting. Cost + stock on goods receipt / posted Purchase. Partial receive stays on PO."
        "STATUS" ->
            "Operational state only — does not post revenue or stock by itself. Sale / POS Charge posts facts."
        "BUDGET", "TARGET" ->
            "Versioned + effective-dated plan. Never rewrite posted sales to match target."
        "ISSUE", "UPDATE", "APPROVAL" ->
            "Audit / action / permission — approval ≠ disbursement. Shift variance opens Issue, not a sale edit."
        "HOLD" ->
            "Hold parks a device-local draft cart — no revenue, no stock, no tender. Not shared across devices until a register API exists."
        "TRANSFER" ->
            "Stock move between locations — never a sale or purchase cost."
        "REPRINT" ->
            "Reprint shares a duplicate copy from posted SALE fields. Client re-render only — no new SALE, no tender change."
        else ->
            "One capture posts one event. Collection/payment allocate — never duplicate facts."
    }

    fun cta(mode: String, amountLabel: String? = null): String {
        val amount = amountLabel?.takeIf { it.isNotBlank() }
        return when (mode.uppercase()) {
            "PURCHASE", "VENDOR" -> if (amount != null) "Post purchase · $amount" else "Post purchase"
            "COLLECTION" -> if (amount != null) "Post collection · $amount" else "Post collection"
            "PAYMENT", "REIMBURSE" -> if (amount != null) "Post payment · $amount" else "Post payment"
            "SPEND" -> if (amount != null) "Post expense · $amount" else "Post expense"
            "STOCK" -> "Post stock update"
            "ORDER", "SALES_ORDER" -> if (amount != null) "Save sales order · $amount" else "Save sales order"
            "PURCHASE_ORDER" -> if (amount != null) "Save purchase order · $amount" else "Save purchase order"
            "STATUS" -> "Update status"
            "BUDGET", "TARGET" -> if (amount != null) "Save revision · $amount" else "Save revision"
            "ISSUE", "UPDATE", "APPROVAL" -> "Post issue"
            "HOLD" -> "Park bill · HOLD-22"
            "TRANSFER" -> "Confirm transfer · 32 units"
            "REPRINT" -> "Reprint · duplicate copy"
            else -> "Save"
        }
    }

    /** CTA for Issue / Approval / Update mode chips (`2399:30597`). */
    fun issueCta(kind: String): String = when (kind) {
        "Approval" -> "Post approval"
        "Update" -> "Post update"
        else -> "Post issue"
    }

    fun amountLabel(mode: String): String = when (mode.uppercase()) {
        "PURCHASE", "VENDOR" -> "Purchase total"
        "COLLECTION" -> "Collect now"
        "PAYMENT", "REIMBURSE" -> "Pay now"
        "SPEND" -> "Expense"
        "BUDGET", "TARGET" -> "New target"
        else -> "Amount"
    }

    /** Payable label on the Budget totals card — Sales target vs Budget chip. */
    fun budgetPayableLabel(metric: String): String =
        if (metric.equals("Budget", ignoreCase = true)) "New budget" else "New target"

    fun purchaseSubtitle(location: String, storeLine: String, gstOn: Boolean): String =
        "$location · $storeLine · ${if (gstOn) "GST on" else "GST off"}"

    fun collectionSubtitle(storeLine: String): String =
        "Register 2 · $storeLine · AR settle"

    fun paymentSubtitle(storeLine: String): String =
        "AP desk · $storeLine · settle supplier"

    fun salesOrderSubtitle(storeLine: String): String =
        "Customer commitment · $storeLine · not revenue"

    fun purchaseOrderSubtitle(storeLine: String): String =
        "Supplier commitment · $storeLine · not AP"

    fun stockSubtitle(locationHint: String? = null): String {
        val loc = locationHint?.takeIf { it.isNotBlank() } ?: "Register aisle"
        return "Floor count · $loc · Andheri"
    }

    fun statusSubtitle(storeLine: String): String {
        return "Floor board · ${brandOnly(storeLine)} · ops only"
    }

    /** `2399:30539` — Owner plan · Harbor Mart · versioned */
    fun budgetSubtitle(storeLine: String): String =
        "Owner plan · ${brandOnly(storeLine)} · versioned"

    /** `2399:30597` — Register 2 · Harbor Mart · audit trail */
    fun issueSubtitle(storeLine: String): String =
        "Register 2 · ${brandOnly(storeLine)} · audit trail"

    /** Harbor Mart fixture store strip when projection title is blank. */
    fun storeLine(modelTitle: String?, locationHint: String? = null): String {
        val store = modelTitle?.takeIf { it.isNotBlank() } ?: "Harbor Mart"
        val loc = locationHint?.takeIf { it.isNotBlank() }
        return when {
            loc != null && !store.contains(loc, ignoreCase = true) -> "$store $loc"
            store.contains("Andheri", ignoreCase = true) -> store
            else -> if (store.equals("Harbor Mart", ignoreCase = true)) "Harbor Mart Andheri" else store
        }
    }

    /** Drop location suffix; keep brand (Harbor Mart). */
    private fun brandOnly(storeLine: String): String = when {
        storeLine.contains("Harbor Mart", ignoreCase = true) -> "Harbor Mart"
        storeLine.contains(' ') ->
            storeLine.substringBeforeLast(' ').takeIf { it.isNotBlank() } ?: storeLine
        else -> storeLine.ifBlank { "Harbor Mart" }
    }
}

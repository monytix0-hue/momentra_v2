package com.example.momentra.ui.shell.business.shared

/**
 * Mirrored Figma DST-QA-01…12 copy catalog (SoT with iOS `DistributionQuickAddCopy.swift`).
 * Brand prefix is `{brand} Distribution · ` where blank brand falls back to `"Distribution"`.
 *
 * Core context/WHY/CTA strings match Trading Figma (verified on DST frames). HOLD / TRANSFER /
 * REPRINT WHY bodies are Distribution-specific from DST-QA-HOLD / TRANSFER / REPRINT.
 */
object DistributionQuickAddCopy {

    const val MORE_DETAILS_LABEL = "More details · notes, attachments, tags"

    fun context(mode: String, brand: String?): String {
        val prefix = brand.orEmpty().ifBlank { "Distribution" }
        val suffix = contextSuffix(mode) ?: return "$prefix Distribution"
        return "$prefix Distribution · $suffix"
    }

    fun contextSuffix(mode: String): String? = when (mode.uppercase()) {
        "SALE" -> "Sale ≠ receipt · stock only on dispatch"
        "PURCHASE" -> "Purchase ≠ payment · stock only when received"
        "COLLECTION" -> "Never posts a new sale · allocates to open AR"
        "PAYMENT", "REIMBURSE" -> "Never posts a new purchase · allocates to open AP"
        "SPEND" -> "Expense ≠ payment · payable if unpaid"
        "STOCK" -> "Reconciliation/adjustment — not a purchase"
        "RETURN" -> "Reversal only · stock changes need explicit goods movement"
        "BUDGET", "TARGET" -> "Versioned plan · never mutates past actuals"
        "UPDATE", "ISSUE" -> "Operational only — never money or stock by inference"
        "APPROVAL" -> "Approval ≠ disbursement · maker-checker aware"
        "ORDER" -> "Commitment only — no sale, purchase, stock, or money"
        "SHIPMENT", "DELIVERY" -> "Physical movement only — not a new sale or purchase"
        "HOLD" -> "Freeze open order or dispatch"
        "TRANSFER" -> "Move qty between locations"
        "REPRINT" -> "Read only · never posts"
        else -> null
    }

    fun why(mode: String): String = when (mode.uppercase()) {
        "SALE" ->
            "Mode chips re-render the form — item fields hidden in Quick amount. Part paid posts one sale + linked partial receipt + remaining AR. Itemized total is read-only sum of lines."
        "PURCHASE" ->
            "Itemized + received-now adds stock; receive-later leaves goods pending for Delivery → Incoming. Payable remains until Payment allocates."
        "COLLECTION" ->
            "Single-source default; multi-allocation only when enabled. Allocations must equal collection amount — no over-allocation."
        "PAYMENT", "REIMBURSE" ->
            "Only unpaid/part-paid posted purchase/expense sources. Sum of allocations must equal payment."
        "SPEND" ->
            "Paid posts expense + linked disbursement once. Pay later requires payee and due date when policy demands."
        "STOCK" ->
            "Set current reconciles to a target qty. Never invents a purchase or sale. Amount-only sales do not move stock."
        "RETURN" ->
            "Credit cannot exceed eligible source. Stock changes only when goods movement is explicit — not inferred from credit amount."
        "BUDGET", "TARGET" ->
            "Preserves prior version. Pulse Target & Budget uses the effective version for the selected period."
        "UPDATE", "ISSUE" ->
            "Issue fields appear only for Issue mode. Creates Needs Attention + Activity entries, not financial facts."
        "APPROVAL" ->
            "Only role-authorized actions shown. Self-approval blocked when maker-checker enabled. Reimburse posts payment separately from approval."
        "ORDER" ->
            "Save creates commitment. Record Sale / Record Purchase select eligible orders and prefill unexecuted lines. Direct sale without order remains allowed."
        "SHIPMENT", "DELIVERY" ->
            "Outgoing reduces undelivered sale qty + stock once. Incoming reduces unreceived purchase qty + increases stock. Money settlement stays on Collection/Payment."
        "HOLD" ->
            "Hold pauses a delivery order. No dispatch, no stock move and no GST; resume continues the same DO."
        "TRANSFER" ->
            "Transfer moves stock between warehouses at cost: − from, + to on confirm. No revenue, no AP, and no GST within the same GSTIN (inter-state branch transfer needs an e-way bill)."
        "REPRINT" ->
            "Reprint re-renders the posted invoice with the same number and GST split (IGST for inter-state dealers). No new sale, no stock or AR change."
        else -> "One capture posts one event. Nothing here rewrites an approved record."
    }

    fun cta(mode: String): String = when (mode.uppercase()) {
        "SALE" -> "Save sale"
        "PURCHASE" -> "Save purchase"
        "COLLECTION" -> "Save collection"
        "PAYMENT" -> "Save payment"
        "SPEND" -> "Save expense"
        "STOCK" -> "Save stock update"
        "RETURN" -> "Save return"
        "BUDGET", "TARGET" -> "Save revision"
        "UPDATE", "ISSUE" -> "Post"
        "APPROVAL" -> "Confirm action"
        "ORDER" -> "Save order"
        "SHIPMENT", "DELIVERY" -> "Save delivery"
        "HOLD" -> "Hold DO"
        "TRANSFER" -> "Confirm transfer"
        "REPRINT" -> "Reprint · duplicate copy"
        else -> "Save"
    }
}

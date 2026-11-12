package com.example.momentra.ui.shell.business.marketing

/**
 * Mirrored Figma MKT-QA-01…10 (+08 Approval, HOLD/TRANSFER/REPRINT) copy catalog
 * (SoT with iOS `MarketingClientQuickAddCopy.swift`).
 *
 * Context lines are `{brand} · Client Campaign · {suffix}` where blank brand falls
 * back to `"Campaign"`. Catalog keys use semantic QA modes (ACTIVITY, SPEND, …),
 * not raw capture `mode` strings (e.g. Invoice posts as SALE).
 */
object MarketingClientQuickAddCopy {

    const val MORE_DETAILS_LABEL = "More details"
    const val MORE_DETAILS_HINT = "Notes · Attachment · Tags"
    const val STICKY_META = "Draft preserved if you close or open a picker"
    /** Rose chip label on Invoice / Collection (Figma `#F43F5E`). */
    const val CLIENT_ONLY_CHIP = "Client only"
    const val CLIENT_ONLY_GATE = "Client only · Hidden & server-rejected in Own Business mode"

    /** Resolve Figma catalog key from capture eventType + mode. */
    fun catalogMode(eventType: String, mode: String): String {
        val et = eventType.uppercase()
        val m = mode.uppercase()
        return when {
            et == "ACTIVITY" || m == "ACTIVITY" -> "ACTIVITY"
            et == "PROGRESS" -> "PROGRESS"
            et == "INVOICE" || (m == "SALE" && et == "INVOICE") -> "INVOICE"
            et == "REVISION" || m == "BUDGET" || m == "TARGET" || m == "REVISION" -> "BUDGET"
            et == "APPROVAL" || m == "APPROVAL" -> "APPROVAL"
            et == "UPDATE" || m == "UPDATE" -> "UPDATE"
            et == "ISSUE" || m == "ISSUE" -> "UPDATE"
            et == "RESULT" -> "RESULT"
            et == "SPEND" || m == "SPEND" -> "SPEND"
            et == "PAYMENT" || m == "PAYMENT" -> "PAYMENT"
            et == "COLLECTION" || m == "COLLECTION" -> "COLLECTION"
            m == "HOLD" || et == "HOLD" -> "HOLD"
            m == "TRANSFER" || et == "TRANSFER" -> "TRANSFER"
            m == "REPRINT" || et == "REPRINT" -> "REPRINT"
            else -> m.ifBlank { et }
        }
    }

    fun context(mode: String, brand: String?, period: String? = null): String {
        val prefix = brand.orEmpty().ifBlank { "Campaign" }
        val key = mode.uppercase()
        val suffix = contextSuffix(key)
        val periodBit = period?.takeIf { it.isNotBlank() }
        return when {
            suffix != null && periodBit != null -> "$prefix · Client Campaign · $periodBit · $suffix"
            suffix != null -> "$prefix · Client Campaign · $suffix"
            periodBit != null -> "$prefix · Client Campaign · $periodBit"
            else -> "$prefix · Client Campaign"
        }
    }

    fun contextSuffix(mode: String): String? = when (mode.uppercase()) {
        "ACTIVITY" -> "Plan item · no money"
        "PROGRESS" -> "Pick an open activity"
        "SPEND" -> "Spend ≠ payment · creates payable"
        "PAYMENT" -> "Settle open payable · AP"
        "INVOICE" -> "Client Campaign · raises AR"
        "COLLECTION" -> "Against open invoice · AR"
        "BUDGET", "TARGET", "REVISION" -> "Versioned plan · effective-dated"
        "APPROVAL" -> "Decision only · no disbursement"
        "UPDATE", "ISSUE" -> "Visible to team"
        "RESULT" -> "Record a measured outcome"
        "HOLD" -> "Pause campaign · no spend / billing / GST"
        "TRANSFER" -> "Move planned budget between channels"
        "REPRINT" -> "Same invoice no. · no new sale / AR"
        else -> null
    }

    fun why(mode: String): String = when (mode.uppercase()) {
        "ACTIVITY" ->
            "An activity is a plan item — it carries no money. Spend and results are recorded against it later."
        "PROGRESS" ->
            "Incremental vs cumulative is explicit so updates are never double-counted."
        "SPEND" ->
            "Spend ≠ payment. Recording spend creates a payable; cash leaves only when you record a Payment."
        "PAYMENT" ->
            "A payment only settles an open payable — it never creates new spend or changes budget use."
        "INVOICE" ->
            "Invoice ≠ collection. Raising an invoice never implies cash received."
        "COLLECTION" ->
            "Collection reduces AR on an existing invoice — it never creates duplicate revenue."
        "BUDGET", "TARGET", "REVISION" ->
            "Revisions are versioned and effective-dated. Past achievement keeps its original denominator."
        "APPROVAL" ->
            "Approval ≠ disbursement — paying is a separate Payment. You cannot approve your own controlled action."
        "UPDATE", "ISSUE" ->
            "Issues and risks raise alerts on Pulse; plain updates do not."
        "RESULT" ->
            "Empty ≠ zero: leave blank when not measured. ROAS is not profit; attributed sales ≠ booked revenue."
        "HOLD" ->
            "Hold pauses a campaign. No spend, no billing and no GST; resume continues the same campaign."
        "TRANSFER" ->
            "Transfer moves planned budget between channels: − from, + to on confirm. No revenue, no AP and no GST — spend posts only on vendor bill."
        "REPRINT" ->
            "Reprint re-renders the posted invoice with the same number and GST split (IGST for out-of-state clients). No new sale, no AR change."
        else -> "One capture posts one event. Nothing here rewrites an approved record."
    }

    fun cta(mode: String, budgetVersion: Int? = null): String = when (mode.uppercase()) {
        "ACTIVITY" -> "Save activity"
        "PROGRESS" -> "Save progress"
        "SPEND" -> "Save spend"
        "PAYMENT" -> "Save payment"
        "INVOICE" -> "Save invoice"
        "COLLECTION" -> "Save collection"
        "BUDGET", "TARGET", "REVISION" ->
            "Save revision${budgetVersion?.let { " v$it" } ?: ""}"
        "APPROVAL" -> "Save decision"
        "UPDATE", "ISSUE" -> "Post update"
        "RESULT" -> "Save result"
        "HOLD" -> "Hold campaign"
        "TRANSFER" -> "Confirm transfer"
        "REPRINT" -> "Reprint · duplicate copy"
        else -> "Save"
    }

    fun title(mode: String): String = when (mode.uppercase()) {
        "ACTIVITY" -> "Add Activity"
        "PROGRESS" -> "Update Progress"
        "SPEND" -> "Record Spend"
        "PAYMENT" -> "Record Payment"
        "INVOICE" -> "Record Invoice"
        "COLLECTION" -> "Record Collection"
        "BUDGET", "TARGET", "REVISION" -> "Budget / Target Revision"
        "APPROVAL" -> "Approval / Decision"
        "UPDATE", "ISSUE" -> "Team Update / Issue"
        "RESULT" -> "Campaign Result"
        "HOLD" -> "Campaign hold"
        "TRANSFER" -> "Budget transfer"
        "REPRINT" -> "Reprint invoice"
        else -> "Quick Add"
    }
}

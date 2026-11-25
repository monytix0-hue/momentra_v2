package com.example.momentra.ui.shell.business.marketing

/**
 * Mirrored Figma MKT-OWN-QA-01…04 / 07…10 (+ HOLD/TRANSFER chrome) copy catalog
 * (SoT with iOS `MarketingOwnQuickAddCopy.swift`).
 *
 * Context lines are `{brand} · Own Business Campaign · {suffix}` where blank brand
 * falls back to `"Campaign"`. Invoice / Collection / Reprint are Client-only and
 * are not part of the Own hub surface.
 */
object MarketingOwnQuickAddCopy {

    const val MORE_DETAILS_LABEL = "More details"
    const val MORE_DETAILS_HINT = "Notes · Attachment · Tags"
    const val STICKY_META = "Draft preserved if you close or open a picker"
    /** Masters Invite WHY — soft-magenta body `#FAE8FF` (Figma `2584:29990`). */
    const val INVITE_WHY = "Invite grants a role and its access. Joining ≠ approval ≠ payment."

    /** Resolve Figma catalog key from capture eventType + mode. */
    fun catalogMode(eventType: String, mode: String): String {
        val et = eventType.uppercase()
        val m = mode.uppercase()
        return when {
            et == "ACTIVITY" || m == "ACTIVITY" -> "ACTIVITY"
            et == "PROGRESS" -> "PROGRESS"
            et == "REVISION" || m == "BUDGET" || m == "TARGET" || m == "REVISION" -> "BUDGET"
            et == "APPROVAL" || m == "APPROVAL" -> "APPROVAL"
            et == "UPDATE" || m == "UPDATE" -> "UPDATE"
            et == "ISSUE" || m == "ISSUE" -> "UPDATE"
            et == "RESULT" -> "RESULT"
            et == "SPEND" || m == "SPEND" -> "SPEND"
            et == "PAYMENT" || m == "PAYMENT" -> "PAYMENT"
            m == "HOLD" || et == "HOLD" -> "HOLD"
            m == "TRANSFER" || et == "TRANSFER" -> "TRANSFER"
            else -> m.ifBlank { et }
        }
    }

    /**
     * @param apAmount compact AP when projection has open payable (e.g. `₹40K`) — Payment only.
     * @param budgetAmount compact plan amount when present (e.g. `₹1.4L`) — Budget only.
     * @param budgetPacing projection pacing string when present — Budget only.
     * Missing projection figures fall back to the static Figma catalog line.
     */
    fun context(
        mode: String,
        brand: String?,
        period: String? = null,
        apAmount: String? = null,
        budgetAmount: String? = null,
        budgetPacing: String? = null,
    ): String {
        val prefix = brand.orEmpty().ifBlank { "Campaign" }
        val key = mode.uppercase()
        // Figma Progress / Payment context lines are standalone (no brand prefix).
        when (key) {
            "PROGRESS" -> return "Pick an open activity in this Moment"
            "PAYMENT" -> {
                val ap = apAmount?.takeIf { it.isNotBlank() && it != "—" && it != "₹0" }
                return if (ap != null) "Settle an open payable · AP $ap" else "Settle an open payable · AP"
            }
        }
        val suffix = contextSuffix(key)
        val periodBit = period?.takeIf { it.isNotBlank() }
        val budgetBits = buildList {
            budgetAmount?.takeIf { it.isNotBlank() && it != "—" }?.let(::add)
            budgetPacing?.takeIf { it.isNotBlank() }?.let(::add)
        }
        val budgetTail = when {
            key !in setOf("BUDGET", "TARGET", "REVISION") || budgetBits.isEmpty() -> null
            else -> budgetBits.joinToString(" · ")
        }
        return when {
            // Activity Figma: `{brand} · Own Business Campaign · {period}` (no extra suffix).
            key == "ACTIVITY" && periodBit != null -> "$prefix · Own Business Campaign · $periodBit"
            suffix != null && periodBit != null && budgetTail != null ->
                "$prefix · Own Business Campaign · $periodBit · $suffix · $budgetTail"
            suffix != null && budgetTail != null ->
                "$prefix · Own Business Campaign · $suffix · $budgetTail"
            suffix != null && periodBit != null -> "$prefix · Own Business Campaign · $periodBit · $suffix"
            suffix != null -> "$prefix · Own Business Campaign · $suffix"
            periodBit != null -> "$prefix · Own Business Campaign · $periodBit"
            else -> "$prefix · Own Business Campaign"
        }
    }

    fun contextSuffix(mode: String): String? = when (mode.uppercase()) {
        "ACTIVITY" -> null
        "PROGRESS" -> "Pick an open activity in this Moment"
        "SPEND" -> "Spend ≠ payment · creates payable"
        "PAYMENT" -> "Settle an open payable · AP"
        "BUDGET", "TARGET", "REVISION" -> "Versioned plan · effective-dated"
        "APPROVAL" -> "Decision only · no disbursement"
        "UPDATE", "ISSUE" -> "Visible to team"
        "RESULT" -> "Record a measured outcome"
        "HOLD" -> "Pause campaign · no spend / billing / GST"
        "TRANSFER" -> "Move planned budget between channels"
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
        else -> "One capture posts one event. Nothing here rewrites an approved record."
    }

    fun cta(mode: String, budgetVersion: Int? = null): String = when (mode.uppercase()) {
        "ACTIVITY" -> "Save activity"
        "PROGRESS" -> "Save progress"
        "SPEND" -> "Save spend"
        "PAYMENT" -> "Save payment"
        "BUDGET", "TARGET", "REVISION" ->
            "Save revision${budgetVersion?.let { " v$it" } ?: ""}"
        "APPROVAL" -> "Save decision"
        "UPDATE", "ISSUE" -> "Post update"
        "RESULT" -> "Save result"
        "HOLD" -> "Hold campaign"
        "TRANSFER" -> "Confirm transfer"
        else -> "Save"
    }

    fun title(mode: String): String = when (mode.uppercase()) {
        "ACTIVITY" -> "Add Activity"
        "PROGRESS" -> "Update Progress"
        "SPEND" -> "Record Spend"
        "PAYMENT" -> "Record Payment"
        "BUDGET", "TARGET", "REVISION" -> "Budget / Target Revision"
        "APPROVAL" -> "Approval / Decision"
        "UPDATE", "ISSUE" -> "Team Update / Issue"
        "RESULT" -> "Campaign Result"
        "HOLD" -> "Campaign hold"
        "TRANSFER" -> "Budget transfer"
        else -> "Quick Add"
    }
}

package com.example.momentra.ui.shell.business.shared

/**
 * Mirrored Figma SVC-QA-01…10 copy catalog.
 * CTAs, Why notes, mode chips, amount labels, and draft footer are locked to
 * nodes `2401:30060`…`2401:30587` (Book. Deliver. Charge.).
 */
object ServiceCaptureCopy {

    const val TAGLINE = "Book. Deliver. Charge."
    const val DRAFT_FOOTER = "Draft preserved if you close or open a picker"
    const val SAVED_TOAST = "✓ Saved · sheet closes and returns back"
    const val MORE_DETAILS_HINT = "Notes · Attachment · Tags"

    /** Mode chips shown under the sheet title. Empty = no chips. */
    fun modeChips(mode: String): List<String> = when (mode.uppercase()) {
        "SERVICE" -> listOf("From booking", "Walk-in charge", "Reprint")
        "PURCHASE", "VENDOR" -> listOf("Consumables", "Equipment")
        "STOCK" -> listOf("Set current", "Add", "Reduce")
        "APPOINTMENT" -> listOf("Appointment", "Walk-in")
        "STATUS" -> listOf("Start", "Complete", "Reschedule")
        "BUDGET", "TARGET" -> listOf("Charges", "Sessions")
        "ISSUE", "UPDATE", "APPROVAL" -> listOf("Issue", "Approval", "Update")
        else -> emptyList()
    }

    fun amountLabel(mode: String): String = when (mode.uppercase()) {
        "SERVICE" -> "Charge amount"
        "PURCHASE", "VENDOR" -> "Purchase amount"
        "COLLECTION" -> "Collect now"
        "PAYMENT", "REIMBURSE" -> "Pay now"
        "SPEND" -> "Expense"
        "APPOINTMENT" -> "Estimate"
        "BUDGET", "TARGET" -> "New target"
        else -> "Amount"
    }

    fun amountHint(mode: String): String = when (mode.uppercase()) {
        "SERVICE" -> "From booking or walk-in"
        "PURCHASE", "VENDOR" -> "Supplier total"
        "COLLECTION" -> "Package balance"
        "PAYMENT", "REIMBURSE" -> "Open AP"
        "SPEND" -> "Running cost"
        "APPOINTMENT" -> "Not revenue"
        "BUDGET", "TARGET" -> "Effective-dated"
        else -> "Positive amount"
    }

    fun why(mode: String): String = when (mode.uppercase()) {
        "SERVICE" ->
            "Finalized charge posts revenue + output GST once (CGST+SGST in MH, IGST if place of supply is another state). Booking/completion alone do not."
        "PURCHASE", "VENDOR" ->
            "Stock only when received. AP + input GST from posted purchase — CGST+SGST if supplier in MH, IGST if out of state. A PO is commitment only."
        "COLLECTION" ->
            "Allocates charge AR — never creates new revenue. Deposit allocation is explicit."
        "PAYMENT", "REIMBURSE" ->
            "Settles open AP only."
        "SPEND" ->
            "Service running cost; payment state separate."
        "STOCK" ->
            "Optional consumables — never invent usage from booking."
        "APPOINTMENT" ->
            "Commitment only — no revenue, no GST. SAC + rate shown as estimate; tax posts on Finalize Charge by place of supply."
        "STATUS" ->
            "Operational/appointment state only — does not post charge."
        "BUDGET", "TARGET" ->
            "Versioned + effective-dated."
        "ISSUE", "UPDATE", "APPROVAL" ->
            "Audit / action — approval ≠ disbursement."
        "HOLD" ->
            "Hold reserves a slot only. No booking commitment, no revenue, no GST until Finalize Charge; expires if not confirmed."
        "TRANSFER" ->
            "Transfer reassigns staff or room on an open booking. No revenue, no charge, no GST; commission follows the pro who completes."
        "REPRINT" ->
            "Reprint re-renders the posted invoice with the same number and GST split (IGST if client is out of state). No new charge, no revenue or tender change."
        else ->
            "One capture posts one event. Booking ≠ revenue; GST posts on Finalize Charge."
    }

    fun cta(mode: String): String = when (mode.uppercase()) {
        "SERVICE" -> "Post charge"
        "PURCHASE", "VENDOR" -> "Post purchase"
        "COLLECTION" -> "Post collection"
        "PAYMENT", "REIMBURSE" -> "Post payment"
        "SPEND" -> "Post expense"
        "STOCK" -> "Post stock update"
        "APPOINTMENT" -> "Save booking"
        "STATUS" -> "Update status"
        "BUDGET", "TARGET" -> "Save revision"
        "ISSUE", "UPDATE", "APPROVAL" -> "Post"
        "HOLD" -> "Hold slot · HLD-22"
        "TRANSFER" -> "Confirm transfer · 2 bookings"
        "REPRINT" -> "Reprint · duplicate copy"
        else -> "Save"
    }

    fun sheetTitle(mode: String): String = when (mode.uppercase()) {
        "SERVICE" -> "Finalize Charge"
        "PURCHASE", "VENDOR" -> "Record Purchase"
        "COLLECTION" -> "Record Collection"
        "PAYMENT", "REIMBURSE" -> "Record Payment"
        "SPEND" -> "Record Expense"
        "STOCK" -> "Stock Update"
        "APPOINTMENT" -> "Booking"
        "STATUS" -> "Status / Assignment"
        "BUDGET", "TARGET" -> "Budget / Target Revision"
        "ISSUE", "UPDATE", "APPROVAL" -> "Issue / Approval / Update"
        "HOLD" -> "Appointment hold"
        "TRANSFER" -> "Staff / room transfer"
        "REPRINT" -> "Reprint invoice"
        else -> mode
    }
}

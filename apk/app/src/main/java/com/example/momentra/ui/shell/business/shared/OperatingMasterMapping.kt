package com.example.momentra.ui.shell.business.shared

import com.example.momentra.data.api.OperatingMasterBody
import com.example.momentra.data.api.OperatingMasterUpdateBody

/**
 * What the master form collects, independent of Compose. Keeping the draft as plain data
 * means the body mapping (trims, blanks-to-null, GST-on-rate) can be unit tested without
 * a running composition.
 */
data class OperatingMasterDraft(
    val kind: String,
    val name: String = "",
    val code: String = "",
    val unit: String = "",
    val sellPrice: String = "",
    val purchasePrice: String = "",
    val mrp: String = "",
    val hsn: String = "",
    val gstRate: String = "",
    val gstin: String = "",
    val billingState: String = "",
    val parentMasterId: String = "",
    val momentId: String? = null,
) {
    /** Blank strings mean "not supplied", never an empty value on the wire. */
    internal fun String.nz(): String? = trim().ifBlank { null }

    /**
     * GST applicability is derived: a rate is the only thing that makes a master taxable.
     * Without a rate the master stays out of the GST position instead of defaulting to 0%.
     */
    val gstApplicable: String? get() = gstRate.nz()?.let { "YES" }

    fun toCreateBody(): OperatingMasterBody = OperatingMasterBody(
        kind = kind,
        name = name.trim(),
        code = code.nz(),
        momentId = momentId,
        unit = unit.nz(),
        sellPrice = sellPrice.nz(),
        mrp = mrp.nz(),
        purchasePrice = purchasePrice.nz(),
        gstApplicable = gstApplicable,
        gstRate = gstRate.nz(),
        hsn = hsn.nz(),
        gstin = gstin.nz(),
        billingState = billingState.nz(),
        parentMasterId = parentMasterId.nz(),
    )

    fun toUpdateBody(expectedVersion: Int): OperatingMasterUpdateBody = OperatingMasterUpdateBody(
        name = name.trim(),
        code = code.nz(),
        unit = unit.nz(),
        sellPrice = sellPrice.nz(),
        mrp = mrp.nz(),
        purchasePrice = purchasePrice.nz(),
        gstApplicable = gstApplicable,
        gstRate = gstRate.nz(),
        hsn = hsn.nz(),
        gstin = gstin.nz(),
        billingState = billingState.nz(),
        parentMasterId = parentMasterId.nz(),
        expectedVersion = expectedVersion,
    )

    /** A master must be named before it can be saved. */
    val canSave: Boolean get() = name.isNotBlank()
}

/**
 * The operator-facing words must map onto the contract's fixed vocabulary, because a stray
 * literal is a rejected write rather than a validation message the operator can act on.
 */
object OperatingStockReasons {
    /** The only reasons `operating-retail-contract.ts` accepts, in their wire case. */
    val accepted = setOf("opening", "stocktake", "damage", "waste", "return", "correction", "TRANSFER")

    fun reasonCode(label: String): String? {
        val key = label.trim().lowercase()
        if (key.isBlank()) return null
        return when (key) {
            "opening", "opening count" -> "opening"
            "stocktake", "count", "stock count" -> "stocktake"
            "damage", "damaged" -> "damage"
            "waste" -> "waste"
            "return", "returned" -> "return"
            "correction", "replenishment", "replenish" -> "correction"
            "transfer", "move" -> "TRANSFER"
            else -> "correction"
        }
    }

    /** A transfer's own reason, or the closest accepted code. */
    fun contractCode(label: String): String? = reasonCode(label)

    fun stockAction(label: String): String = when (label.trim().lowercase()) {
        "out", "reduce", "issued" -> "REDUCE"
        "set", "count" -> "SET"
        "transfer", "move" -> "TRANSFER"
        else -> "ADD"
    }

    fun ownership(label: String): String =
        if (label.trim().uppercase().startsWith("CONSIGN")) "CONSIGNMENT" else "OWNED"

    fun holdStatus(label: String): String =
        if (label.trim().equals("No hold", ignoreCase = true)) "RELEASED" else "HELD"
}

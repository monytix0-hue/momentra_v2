package com.example.momentra.ui.shell.business.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.api.OperatingActivityDto
import com.example.momentra.data.api.OperatingDocumentDto
import com.example.momentra.data.api.OperatingProjectionDto
import com.example.momentra.data.repository.BusinessSliceRepository
import com.example.momentra.ui.theme.PlusJakartaSans
import kotlinx.coroutines.launch

/**
 * The twelve capture layouts the QA frames pin down for Trading and Distribution —
 * `TRD-QA-01..12` and `DST-QA-01..12`. The renderers themselves live next to the shared
 * capture sheet (one function per layout); this type is the single place that maps a
 * capture mode onto its frame, so tests can assert the mapping without Compose.
 */
enum class OperatingCaptureLayout(
    val mode: String,
    val qaCode: String,
    val label: String,
) {
    SALE("SALE", "QA-01", "Sale"),
    PURCHASE("PURCHASE", "QA-02", "Purchase"),
    COLLECTION("COLLECTION", "QA-03", "Collection"),
    PAYMENT("PAYMENT", "QA-04", "Payment"),
    SPEND("SPEND", "QA-05", "Expense"),
    STOCK("STOCK", "QA-06", "Stock"),
    RETURN("RETURN", "QA-07", "Return"),
    BUDGET("BUDGET", "QA-08", "Budget"),
    UPDATE("UPDATE", "QA-09", "Update / Issue"),
    APPROVAL("APPROVAL", "QA-10", "Approval / Reimbursement"),
    ORDER("ORDER", "QA-11", "Order"),
    SHIPMENT("SHIPMENT", "QA-12", "Delivery"),
    ;

    companion object {
        /** Capture modes that reuse another layout's frame. */
        fun forMode(mode: String): OperatingCaptureLayout? = when (mode) {
            "SALE", "SERVICE" -> SALE
            "PURCHASE", "VENDOR" -> PURCHASE
            "COLLECTION" -> COLLECTION
            "PAYMENT", "REIMBURSE" -> PAYMENT
            "SPEND" -> SPEND
            "STOCK", "TRANSFER", "HOLD" -> STOCK
            "RETURN" -> RETURN
            "BUDGET", "TARGET" -> BUDGET
            "UPDATE", "ISSUE" -> UPDATE
            "APPROVAL" -> APPROVAL
            "ORDER" -> ORDER
            "SHIPMENT", "DELIVERY" -> SHIPMENT
            else -> null
        }
    }
}

/** The hold choices `TRD-QA-HOLD` offers. */
internal val OPERATING_HOLD_CHOICES = listOf("No hold", "Hold order", "Hold dispatch")

/** The two document kinds `TRD-QA-REPRINT` can read. */
internal val OPERATING_REPRINT_KINDS = listOf("INVOICE", "CHALLAN")

/**
 * `TRD-QA-HOLD` — freezes an open order or dispatch. A hold keeps the record's place and
 * amount; it never completes or cancels anything, which is why a reason is required.
 */
@Composable
internal fun HoldCaptureLayout(
    form: CaptureForm,
    model: OperatingProjectionDto,
) {
    ChoiceBlock("Hold", listOf("Order", "Dispatch"), form.holdTarget) { form.holdTarget = it }
    val wanted = if (form.holdTarget == "Dispatch") "SHIPMENT" else "ORDER"
    val open = model.activity.filter { it.eventType == wanted }
    ChoiceBlock("Hold", OPERATING_HOLD_CHOICES, form.choice.ifBlank { OPERATING_HOLD_CHOICES.first() }) {
        form.choice = it
    }
    Text(
        if (open.isEmpty()) "Nothing open to hold on this moment." else "Open ${wanted.lowercase()}s",
        color = SheetMuted,
        fontFamily = PlusJakartaSans,
        fontSize = 12.sp,
    )
    open.take(8).forEach { row ->
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(if (form.parentId == row.id) SheetHigh else Color.Transparent)
                .border(
                    1.dp,
                    if (form.parentId == row.id) SheetAccent else SheetBorder,
                    RoundedCornerShape(12.dp),
                )
                .clickable {
                    form.parentId = row.id
                    form.purpose = row.title
                }
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                row.title,
                color = SheetInk,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
            )
            Text(
                listOfNotNull(
                    row.counterpartyName?.takeIf { it.isNotBlank() },
                    row.status?.takeIf { it.isNotBlank() },
                    row.amount?.takeIf { it.isNotBlank() },
                ).joinToString(" · "),
                color = SheetMuted,
                fontFamily = PlusJakartaSans,
                fontSize = 12.sp,
            )
        }
    }
    EditField("Hold reason", form.holdReason, "Required") { form.holdReason = it.take(200) }
    Text(
        "A hold freezes the record where it is. Release it before completing or delivering.",
        color = SheetMuted,
        fontFamily = PlusJakartaSans,
        fontSize = 12.sp,
    )
    SummaryCard(
        "After save",
        listOf(
            if (form.choice == "No hold") "Releases the hold, if one was set." else "Marks the record as held.",
            "No money, stock, or GST movement happens here.",
        ),
    )
}

/**
 * `TRD-QA-TRANSFER` — moves the same quantity out of one location and into another.
 * The transfer must never read as a global gain or loss.
 */
@Composable
internal fun TransferCaptureLayout(
    form: CaptureForm,
    model: OperatingProjectionDto,
    actor: String,
) {
    LovField("Item", form.purpose, "product") { form.lovKind = "PRODUCT" }
    val locations = model.masters.orEmpty()
        .filter { it.kind.equals("WAREHOUSE", true) || it.kind.equals("LOCATION", true) || it.kind.equals("ROUTE", true) }
    if (locations.isNotEmpty()) {
        Text(
            "Tap From on the source and To on the destination.",
            color = SheetMuted,
            fontFamily = PlusJakartaSans,
            fontSize = 12.sp,
        )
        LocationPicker(form, locations)
    } else {
        EditField("From location", form.transferFromName, "Required") { form.transferFromName = it }
        EditField("To location", form.transferToName, "Required") { form.transferToName = it }
    }
    FieldRow {
        EditField("Quantity", form.quantity, "Required", Modifier.weight(1f)) { form.quantity = it }
        LovField("Unit", form.unit, "UOM", Modifier.weight(1f)) { form.lovKind = "UOM" }
    }
    ChoiceBlock(
        "Reason",
        listOf("Stocktake", "Replenishment", "Damage", "Correction"),
        form.stockReason,
    ) { form.stockReason = it }
    ChoiceBlock("Ownership", listOf("Owned", "Consignment"), form.ownership) { form.ownership = it }
    Text(
        "A transfer moves stock between two places. Total on-hand does not change.",
        color = SheetMuted,
        fontFamily = PlusJakartaSans,
        fontSize = 12.sp,
    )
    SummaryCard(
        "After save",
        listOf(
            "Removes ${form.quantity.ifBlank { "the quantity" }} from ${form.transferFromName.ifBlank { "the source" }}.",
            "Adds the same quantity to ${form.transferToName.ifBlank { "the destination" }}.",
            "No purchase, sale, or money record is created.",
        ),
    )
}

@Composable
private fun LocationPicker(form: CaptureForm, locations: List<com.example.momentra.data.api.OperatingMasterDto>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        locations.take(8).forEach { master ->
            val isFrom = form.transferFromId == master.id
            val isTo = form.transferToId == master.id
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isFrom || isTo) SheetHigh else Color.Transparent)
                    .border(1.dp, if (isFrom || isTo) SheetAccent else SheetBorder, RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    master.name,
                    color = SheetInk,
                    fontFamily = PlusJakartaSans,
                    fontSize = 13.sp,
                    modifier = Modifier.weight(1f),
                )
                // A location can be either end of the move, never both, so choosing it for
                // one side clears the other rather than leaving a same-place transfer.
                EndOfMoveChip("From", isFrom) {
                    form.transferFromId = if (isFrom) null else master.id
                    form.transferFromName = if (isFrom) "" else master.name
                    if (!isFrom && form.transferToId == master.id) {
                        form.transferToId = null
                        form.transferToName = ""
                    }
                }
                EndOfMoveChip("To", isTo) {
                    form.transferToId = if (isTo) null else master.id
                    form.transferToName = if (isTo) "" else master.name
                    if (!isTo && form.transferFromId == master.id) {
                        form.transferFromId = null
                        form.transferFromName = ""
                    }
                }
            }
        }
    }
}

@Composable
private fun EndOfMoveChip(label: String, selected: Boolean, onTap: () -> Unit) {
    Text(
        label,
        color = if (selected) SheetAccent else SheetMuted,
        fontFamily = PlusJakartaSans,
        fontSize = 12.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) SheetHigh else Color.Transparent)
            .clickable(onClick = onTap)
            .padding(horizontal = 9.dp, vertical = 5.dp),
    )
}

/**
 * `TRD-QA-REPRINT` — a read of an already-recorded document. Reprinting never posts a
 * new event, so the sheet only picks the source and the kind, then loads the payload.
 */
@Composable
internal fun ReprintCaptureLayout(
    form: CaptureForm,
    model: OperatingProjectionDto,
    currency: String,
) {
    val repo = remember { BusinessSliceRepository() }
    val scope = rememberCoroutineScope()
    var document by remember { mutableStateOf<OperatingDocumentDto?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val momentId = OperatingChromeState.momentId
    val sources = model.activity.filter { it.eventType in setOf("SALE", "PURCHASE", "SERVICE") }
    ChoiceBlock("Document", listOf("Invoice", "Challan"), form.reprintKind.lowercase().replaceFirstChar { it.uppercase() }) {
        form.reprintKind = if (it.equals("Challan", true)) "CHALLAN" else "INVOICE"
    }
    Text("Source record", color = SheetMuted, fontFamily = PlusJakartaSans, fontSize = 12.sp)
    sources.take(10).forEach { row ->
        val on = form.reprintSourceId == row.id
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(if (on) SheetHigh else Color.Transparent)
                .border(1.dp, if (on) SheetAccent else SheetBorder, RoundedCornerShape(12.dp))
                .clickable {
                    form.reprintSourceId = row.id
                    form.purpose = row.title
                    form.amount = row.amount.orEmpty()
                }
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    row.title.ifBlank { row.eventType },
                    color = SheetInk,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                )
                Text(
                    listOfNotNull(
                        row.counterpartyName?.takeIf { it.isNotBlank() },
                        formatOperatingAmount(currency, row.amount),
                        row.occurredAt.take(10).takeIf { it.isNotBlank() },
                    ).joinToString(" · "),
                    color = SheetMuted,
                    fontFamily = PlusJakartaSans,
                    fontSize = 12.sp,
                )
            }
            OperatingBadgeChip("Recorded", TradeSheetThemePlaceholder)
        }
    }
    LaunchedEffect(form.reprintSourceId, form.reprintKind) {
        document = null
        error = null
        if (form.reprintSourceId.isBlank() || momentId == null) return@LaunchedEffect
        repo.getOperatingDocument(momentId, form.reprintSourceId, form.reprintKind).fold(
            onSuccess = { document = it },
            onFailure = { error = it.message },
        )
    }
    document?.let { doc ->
        SummaryCard(
            doc.documentNumber.ifBlank { "Document" },
            listOfNotNull(
                doc.buyer.name?.takeIf { it.isNotBlank() }?.let { "To $it" },
                if (doc.lines.isNotEmpty()) "${doc.lines.size} line${if (doc.lines.size == 1) "" else "s"}" else null,
                doc.grandTotal?.takeIf { it.isNotBlank() }?.let { "Total ${formatOperatingAmount(doc.currency, it)}" },
                "Reprint reads the record. Nothing is posted.",
            ),
        )
    }
    if (error != null) {
        Text(error ?: "", color = Color(0xFFF87171), fontFamily = PlusJakartaSans, fontSize = 12.sp)
    }
    Text(
        "A reprint is a read — the invoice or challan is drawn on device from the recorded data.",
        color = SheetMuted,
        fontFamily = PlusJakartaSans,
        fontSize = 12.sp,
    )
}

/**
 * `MTO-QA · Create Quotation` / Accept / Quote revise — shared lifecycle fields.
 * Mode chips: Quotation · Revise · Accept · Cancel map onto `CONFIRM` / `QUOTE_REVISE` /
 * `ORDER` capture modes from the MTO hub.
 */
@Composable
internal fun MtoQuoteLifecycleCapture(
    form: CaptureForm,
    mode: String,
    model: OperatingProjectionDto,
    currency: String,
) {
    val quotes = model.activity.filter { it.eventType == "CUSTOMER_ORDER" }
    when (mode) {
        "CONFIRM" -> {
            if (form.goodsStatusLabel.isBlank()) form.goodsStatusLabel = "Accepted"
            Text("Accepted quote", color = SheetMuted, fontFamily = PlusJakartaSans, fontSize = 12.sp)
            quotes.take(8).forEach { row ->
                TextButton(onClick = {
                    form.parentId = row.id
                    form.party = row.counterpartyName.orEmpty().ifBlank { row.title }
                    form.amount = row.amount.orEmpty()
                    form.quantity = row.quantity.orEmpty()
                }) {
                    Text(
                        if (form.parentId == row.id) "Selected ${row.title}" else row.title,
                        color = Color.White,
                        fontFamily = PlusJakartaSans,
                        fontSize = 13.sp,
                    )
                }
            }
            AmountBlock("Confirmed value (ex tax)", currency, form.amount, "Commitment · not revenue") { form.amount = it }
            EditField("Customer", form.party, "Required") { form.party = it }
            EditField("Quantity", form.quantity, "Required") { form.quantity = it }
            EditField("Unit", form.unit, "pcs") { form.unit = it }
            Text(
                "Acceptance creates the confirmed order and turns on job pickers. Still no revenue, AR or GST.",
                color = SheetMuted,
                fontFamily = PlusJakartaSans,
                fontSize = 12.sp,
            )
        }
        else -> {
            if (form.goodsStatusLabel.isBlank()) form.goodsStatusLabel = "Expired"
            Text("Quote to revise", color = SheetMuted, fontFamily = PlusJakartaSans, fontSize = 12.sp)
            quotes.take(8).forEach { row ->
                TextButton(onClick = {
                    form.parentId = row.id
                    form.party = row.counterpartyName.orEmpty().ifBlank { row.title }
                    form.amount = row.amount.orEmpty()
                    form.quantity = row.quantity.orEmpty()
                }) {
                    Text(
                        if (form.parentId == row.id) "Revising ${row.title}" else row.title,
                        color = Color.White,
                        fontFamily = PlusJakartaSans,
                        fontSize = 13.sp,
                    )
                }
            }
            ChoiceBlock(
                "Status",
                listOf("Draft", "Sent", "Accepted", "Rejected", "Expired"),
                form.goodsStatusLabel,
            ) { form.goodsStatusLabel = it }
            AmountBlock("Revised quote value", currency, form.amount, "Preview only · no AR") { form.amount = it }
            EditField("Customer", form.party, "Required") { form.party = it }
            EditField("Quantity", form.quantity, "Required") { form.quantity = it }
            Text(
                "A revise replaces the prior quote status. It still creates no revenue until an invoice posts.",
                color = SheetMuted,
                fontFamily = PlusJakartaSans,
                fontSize = 12.sp,
            )
        }
    }
}

/** `MTO-QA · Open Production Job` / Job status change. */
@Composable
internal fun MtoJobLifecycleCapture(
    form: CaptureForm,
    mode: String,
    model: OperatingProjectionDto,
) {
    val orders = model.activity.filter { it.eventType == "CUSTOMER_ORDER" }
    Text(
        if (mode == "OPEN_JOB") "Confirmed order" else "Open job / order",
        color = SheetMuted,
        fontFamily = PlusJakartaSans,
        fontSize = 12.sp,
    )
    orders.take(8).forEach { row ->
        TextButton(onClick = {
            form.parentId = row.id
            form.party = row.counterpartyName.orEmpty()
            form.quantity = row.quantity.orEmpty()
        }) {
            Text(
                if (form.parentId == row.id) "Selected ${row.title}" else row.title,
                color = Color.White,
                fontFamily = PlusJakartaSans,
                fontSize = 13.sp,
            )
        }
    }
    if (mode == "OPEN_JOB") {
        EditField("Job ID", form.purpose, "e.g. J-07") { form.purpose = it }
        EditField("Job target qty", form.quantity, "From confirmed order") { form.quantity = it }
        EditField("Unit", form.unit, "pcs") { form.unit = it }
        NoteField("BOM / notes", form.note, "Optional") { form.note = it }
        Text(
            "A job carries the confirmed order target. Opening it posts no output, stock or sale.",
            color = SheetMuted,
            fontFamily = PlusJakartaSans,
            fontSize = 12.sp,
        )
    } else {
        ChoiceBlock(
            "Job status",
            listOf("Open", "In progress", "On hold", "Complete", "Cancelled"),
            form.choice.ifBlank { "In progress" },
        ) { form.choice = it }
        NoteField("Status note", form.purpose, "Required") { form.purpose = it }
        Text(
            "Status is an operational signal. It never posts production output or revenue.",
            color = SheetMuted,
            fontFamily = PlusJakartaSans,
            fontSize = 12.sp,
        )
    }
}

/**
 * `MFG-QA-14 · Rework / Scrap / Job Work` — first-class modes, not a buried scrap qty on production.
 * Scrap reduces sellable stock (waste). Rework and job-work-out are operational updates — they
 * never invent finished goods.
 */
@Composable
internal fun ReworkScrapCapture(
    form: CaptureForm,
    model: OperatingProjectionDto,
    actor: String,
) {
    ChoiceBlock("Mode", listOf("Rework", "Scrap", "Job work out"), form.choice.ifBlank { "Rework" }) {
        form.choice = it
    }
    ChoiceBlock(
        "Action",
        when (form.choice) {
            "Scrap" -> listOf("Scrap to waste", "Scrap & quarantine")
            "Job work out" -> listOf("Send to vendor", "Receive from vendor")
            else -> listOf("Send to rework", "Return from rework")
        },
        form.purpose.ifBlank {
            when (form.choice) {
                "Scrap" -> "Scrap to waste"
                "Job work out" -> "Send to vendor"
                else -> "Send to rework"
            }
        },
    ) { form.purpose = it }
    model.activity.filter { it.eventType == "PRODUCTION" || it.eventType == "CUSTOMER_ORDER" }.take(6).forEach { row ->
        TextButton(onClick = { form.parentId = row.id }) {
            Text(
                if (form.parentId == row.id) "Source ${row.title}" else row.title,
                color = Color.White,
                fontFamily = PlusJakartaSans,
                fontSize = 13.sp,
            )
        }
    }
    EditField("Source batch / job", form.link, "Optional reference") { form.link = it }
    FieldRow {
        EditField("Qty", form.quantity, "Required", Modifier.weight(1f)) { form.quantity = it }
        EditField("Unit", form.unit, "pcs", Modifier.weight(1f)) { form.unit = it }
    }
    if (form.choice == "Job work out" || form.choice == "Rework") {
        EditField("Vendor", form.party, "Optional") { form.party = it }
    }
    NoteField("Notes", form.note, "Optional") { form.note = it }
    Text(
        "Distinct canonical events — never silent overwrite of accepted stock. Scrap does not become finished stock.",
        color = SheetMuted,
        fontFamily = PlusJakartaSans,
        fontSize = 12.sp,
    )
    Text("Recorded by ${actor.substringBefore(" ·")}", color = SheetMuted, fontFamily = PlusJakartaSans, fontSize = 11.sp)
}

/**
 * A neutral palette for chips drawn outside a themed sheet frame. The capture sheet is
 * the only caller today, so the tones stay the shared sheet tokens.
 */
internal val TradeSheetThemePlaceholder = OperatingSheetTheme(
    accent = SheetAccent,
    accentSoft = SheetAccent,
    canvas = BusinessSheetTokens.bg,
    surface = BusinessSheetTokens.bg,
    inset = BusinessSheetTokens.field,
    border = SheetBorder,
    ink = SheetInk,
    body = SheetMuted,
    warning = OperatingSheetWarning,
    negative = OperatingSheetNegative,
)

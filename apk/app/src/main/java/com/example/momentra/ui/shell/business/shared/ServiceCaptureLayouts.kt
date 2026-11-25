package com.example.momentra.ui.shell.business.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.api.OperatingActivityDto
import com.example.momentra.data.api.OperatingProjectionDto
import com.example.momentra.ui.theme.PlusJakartaSans
import com.example.momentra.ui.theme.ServicesTeal

/** Figma SVC mode chips — selected = teal fill + lime border + dark ink. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ServiceModeChips(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    if (options.isEmpty()) return
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        options.forEach { option ->
            val on = option == selected
            Text(
                option,
                color = if (on) ServicesTeal.inkOnTeal else ServicesTeal.textBody,
                fontFamily = PlusJakartaSans,
                fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                fontSize = 12.sp,
                maxLines = 1,
                modifier = Modifier
                    .clip(ServicesTeal.rPill)
                    .then(
                        if (on) {
                            Modifier.shadow(
                                8.dp,
                                ServicesTeal.rPill,
                                clip = false,
                                ambientColor = ServicesTeal.teal.copy(alpha = 0.4f),
                                spotColor = ServicesTeal.teal.copy(alpha = 0.4f),
                            )
                        } else {
                            Modifier
                        },
                    )
                    .background(if (on) ServicesTeal.teal else ServicesTeal.chip)
                    .border(
                        1.dp,
                        if (on) ServicesTeal.lime else ServicesTeal.chipBorder,
                        ServicesTeal.rPill,
                    )
                    .clickable { onSelect(option) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
    }
}

/** SVC Why note — lime “Why” + cream body on teal plate. */
@Composable
internal fun ServiceWhyPanel(body: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(ServicesTeal.teal.copy(alpha = 0.12f))
            .border(1.dp, ServicesTeal.teal.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            "Why",
            color = ServicesTeal.lime,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            maxLines = 1,
        )
        Text(
            body,
            color = ServicesTeal.whyBody,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.Normal,
            fontSize = 11.sp,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
internal fun ServiceMoreDetails(open: Boolean, onToggle: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, ServicesTeal.teal.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .clickable(onClick = onToggle)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            "More details",
            color = ServicesTeal.lime,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
        )
        Text(
            ServiceCaptureCopy.MORE_DETAILS_HINT,
            color = ServicesTeal.textBody,
            fontFamily = PlusJakartaSans,
            fontSize = 11.sp,
            modifier = Modifier.weight(1f),
            maxLines = 1,
        )
        Text(
            if (open) "⌃" else "⌄",
            color = ServicesTeal.lime,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
        )
    }
}

// ---------------------------------------------------------------------------------
// SVC-QA-01 · Finalize Charge
// ---------------------------------------------------------------------------------

@Composable
internal fun ServiceFinalizeChargeCapture(
    form: CaptureForm,
    @Suppress("UNUSED_PARAMETER") model: OperatingProjectionDto,
    currency: String,
    onDismiss: () -> Unit,
) {
    val chips = ServiceCaptureCopy.modeChips("SERVICE")
    val chip = when {
        form.entryMode.equals("Reprint", true) -> "Reprint"
        form.entryMode.contains("Walk", ignoreCase = true) -> "Walk-in charge"
        else -> "From booking"
    }
    ServiceModeChips(chips, chip) { picked ->
        when (picked) {
            "Reprint" -> {
                form.entryMode = "Reprint"
                OperatingChromeState.quickAddHint = "reprint"
                onDismiss()
            }
            "Walk-in charge" -> form.entryMode = "Walk-in charge"
            else -> form.entryMode = "From booking"
        }
    }
    AmountBlock(
        ServiceCaptureCopy.amountLabel("SERVICE"),
        currency,
        form.amount,
        form.itemName.ifBlank { form.purpose }.ifBlank { ServiceCaptureCopy.amountHint("SERVICE") },
    ) { form.amount = it }
    EditField("Client", form.party, "Named client") { form.party = it }
    LovField(
        "Service",
        form.itemName.ifBlank { form.purpose },
        "Pick from Services",
    ) { OperatingChromeState.servicesBrowserOpen = "SERVICE" }
    ChoiceBlock(
        "Payment state",
        listOf("Collect now", "Part collect", "Pay later"),
        form.choice.ifBlank { "Collect now" },
    ) { form.choice = it }
    if (form.choice.contains("Part", ignoreCase = true) || form.choice.contains("Collect now", ignoreCase = true) && form.receivedAmount.isNotBlank()) {
        EditField("Collect now", form.receivedAmount, "Optional partial") { form.receivedAmount = it }
    }
    val sac = form.detail.takeIf { it.isNotBlank() }
    val gstLine = if (sac != null) {
        "18% · SAC $sac · CGST 9% + SGST 9%"
    } else {
        "18% · SAC from offering · CGST 9% + SGST 9%"
    }
    ReadField("GST", gstLine)
    ServiceMoreDetails(form.moreOpen) { form.moreOpen = !form.moreOpen }
    if (form.moreOpen) NoteField("Note", form.note, "Optional") { form.note = it }
    ReadField("Charged on", form.dateText) { form.dateOpen = true }
    ServiceWhyPanel(ServiceCaptureCopy.why("SERVICE"))
}

// ---------------------------------------------------------------------------------
// SVC-QA-02 · Record Purchase
// ---------------------------------------------------------------------------------

@Composable
internal fun ServicePurchaseCapture(
    form: CaptureForm,
    @Suppress("UNUSED_PARAMETER") model: OperatingProjectionDto,
    currency: String,
) {
    val chips = ServiceCaptureCopy.modeChips("PURCHASE")
    val chip = if (form.entryMode.contains("Equip", ignoreCase = true)) "Equipment" else "Consumables"
    ServiceModeChips(chips, chip) { form.entryMode = it }
    AmountBlock(
        ServiceCaptureCopy.amountLabel("PURCHASE"),
        currency,
        form.amount,
        form.party.ifBlank { ServiceCaptureCopy.amountHint("PURCHASE") },
    ) { form.amount = it }
    EditField("Supplier", form.party, "Required") { form.party = it }
    ChoiceBlock(
        "Receive now",
        listOf("Full", "Partial", "Later"),
        form.receiptState.ifBlank { "Full" },
    ) { form.receiptState = it }
    ChoiceBlock(
        "Payment state",
        listOf("Paid now", "Part paid", "Pay later"),
        form.choice.ifBlank { "Pay later" },
    ) { form.choice = it }
    ServiceMoreDetails(form.moreOpen) { form.moreOpen = !form.moreOpen }
    if (form.moreOpen) NoteField("Note", form.note, "Optional") { form.note = it }
    ReadField("Expected on", form.dateText) { form.dateOpen = true }
    ServiceWhyPanel(ServiceCaptureCopy.why("PURCHASE"))
}

// ---------------------------------------------------------------------------------
// SVC-QA-03 · Record Collection
// ---------------------------------------------------------------------------------

@Composable
internal fun ServiceCollectionCapture(
    form: CaptureForm,
    model: OperatingProjectionDto,
    currency: String,
) {
    AmountBlock(
        ServiceCaptureCopy.amountLabel("COLLECTION"),
        currency,
        form.amount,
        form.purpose.ifBlank { ServiceCaptureCopy.amountHint("COLLECTION") },
    ) { form.amount = it }
    EditField("Client", form.party, "Named client") { form.party = it }
    val open = model.paymentReadiness.firstOrNull { it.id == form.parentId }
    LovField(
        "Source",
        open?.title?.ifBlank { null } ?: form.purpose.ifBlank { "Open charge / package" },
        if (model.paymentReadiness.none { it.relevance == "RECEIVABLE" }) "Nothing is open" else "Open charge / package",
    ) { form.lovKind = "OBLIGATION" }
    ChoiceBlock(
        "Method",
        listOf("UPI", "Cash", "Card", "Bank transfer"),
        form.detail.ifBlank { "UPI" },
    ) { form.detail = it }
    ServiceMoreDetails(form.moreOpen) { form.moreOpen = !form.moreOpen }
    if (form.moreOpen) NoteField("Note", form.note, "Optional") { form.note = it }
    ReadField("Received on", form.dateText) { form.dateOpen = true }
    ServiceWhyPanel(ServiceCaptureCopy.why("COLLECTION"))
}

// ---------------------------------------------------------------------------------
// SVC-QA-04 · Record Payment
// ---------------------------------------------------------------------------------

@Composable
internal fun ServicePaymentCapture(
    form: CaptureForm,
    model: OperatingProjectionDto,
    currency: String,
) {
    AmountBlock(
        ServiceCaptureCopy.amountLabel("PAYMENT"),
        currency,
        form.amount,
        form.party.ifBlank { ServiceCaptureCopy.amountHint("PAYMENT") },
    ) { form.amount = it }
    EditField("Payee", form.party, "Supplier / payee") { form.party = it }
    val open = model.paymentReadiness.firstOrNull { it.id == form.parentId }
    LovField(
        "Source",
        open?.title?.ifBlank { null } ?: form.purpose.ifBlank { "Supplier invoice" },
        if (model.paymentReadiness.none { it.relevance == "PAYABLE" }) "Nothing is open" else "Supplier invoice",
    ) { form.lovKind = "OBLIGATION" }
    ChoiceBlock(
        "Method",
        listOf("Bank transfer", "UPI", "Cash", "Card"),
        form.detail.ifBlank { "Bank transfer" },
    ) { form.detail = it }
    ServiceMoreDetails(form.moreOpen) { form.moreOpen = !form.moreOpen }
    if (form.moreOpen) NoteField("Note", form.note, "Optional") { form.note = it }
    ReadField("Paid on", form.dateText) { form.dateOpen = true }
    ServiceWhyPanel(ServiceCaptureCopy.why("PAYMENT"))
}

// ---------------------------------------------------------------------------------
// SVC-QA-05 · Record Expense
// ---------------------------------------------------------------------------------

@Composable
internal fun ServiceExpenseCapture(
    form: CaptureForm,
    currency: String,
) {
    AmountBlock(
        ServiceCaptureCopy.amountLabel("SPEND"),
        currency,
        form.amount,
        form.purpose.ifBlank { ServiceCaptureCopy.amountHint("SPEND") },
    ) { form.amount = it }
    EditField("Type", form.purpose, "Studio › category") { form.purpose = it }
    EditField("Payee", form.party, "Optional") { form.party = it }
    ChoiceBlock(
        "Payment state",
        listOf("Paid now", "Part paid", "Pay later"),
        form.choice.ifBlank { "Paid now" },
    ) { form.choice = it }
    ServiceMoreDetails(form.moreOpen) { form.moreOpen = !form.moreOpen }
    if (form.moreOpen) NoteField("Note", form.note, "Optional") { form.note = it }
    ReadField("Period", monthPickerLabel(form.eventDate)) { form.monthOpen = true }
    ServiceWhyPanel(ServiceCaptureCopy.why("SPEND"))
}

// ---------------------------------------------------------------------------------
// SVC-QA-06 · Stock Update
// ---------------------------------------------------------------------------------

@Composable
internal fun ServiceStockCapture(
    form: CaptureForm,
) {
    val chips = ServiceCaptureCopy.modeChips("STOCK")
    val chip = when {
        form.stockAction.startsWith("Add", true) -> "Add"
        form.stockAction.startsWith("Reduce", true) -> "Reduce"
        else -> "Set current"
    }
    ServiceModeChips(chips, chip) {
        form.stockAction = when (it) {
            "Add" -> "Add"
            "Reduce" -> "Reduce"
            else -> "Set current stock"
        }
    }
    EditField("Supply", form.purpose, "Consumable / SKU") { form.purpose = it }
    EditField("Location", form.channel, "Room / store") { form.channel = it }
    EditField("Quantity", form.quantity, "Required") { form.quantity = it }
    ChoiceBlock(
        "Reason",
        listOf("Usage / count", "Stocktake", "Damage", "Waste", "Correction"),
        form.stockReason.ifBlank { "Usage / count" },
    ) { form.stockReason = it }
    ServiceMoreDetails(form.moreOpen) { form.moreOpen = !form.moreOpen }
    if (form.moreOpen) NoteField("Note", form.note, "Optional") { form.note = it }
    ServiceWhyPanel(ServiceCaptureCopy.why("STOCK"))
}

// ---------------------------------------------------------------------------------
// SVC-QA-07 · Booking
// ---------------------------------------------------------------------------------

@Composable
internal fun ServiceBookingCapture(
    form: CaptureForm,
    model: OperatingProjectionDto,
    currency: String,
) {
    val chips = ServiceCaptureCopy.modeChips("APPOINTMENT")
    val chip = if (form.entryMode.contains("Walk", ignoreCase = true)) "Walk-in" else "Appointment"
    ServiceModeChips(chips, chip) { form.entryMode = it }
    AmountBlock(
        ServiceCaptureCopy.amountLabel("APPOINTMENT"),
        currency,
        form.amount,
        ServiceCaptureCopy.amountHint("APPOINTMENT"),
    ) { form.amount = it }
    EditField("Client", form.party, "Named client") { form.party = it }
    LovField(
        "Service",
        form.purpose.ifBlank { form.itemName },
        "Pick from Services",
    ) { OperatingChromeState.servicesBrowserOpen = "APPOINTMENT" }
    ReadField("Slot", form.dateText) { form.dateOpen = true }
    EditField("Pro", form.detail, "Staff") { form.detail = it }
    ChoiceBlock(
        "Status",
        listOf("Booked", "Confirmed", "Arrived", "No-show"),
        form.choice.ifBlank { "Booked" },
    ) { form.choice = it }
    ServiceMoreDetails(form.moreOpen) { form.moreOpen = !form.moreOpen }
    if (form.moreOpen) NoteField("Note", form.note, "Optional") { form.note = it }
    ServiceWhyPanel(ServiceCaptureCopy.why("APPOINTMENT"))
}

// ---------------------------------------------------------------------------------
// SVC-QA-08 · Status / Assignment
// ---------------------------------------------------------------------------------

@Composable
internal fun ServiceStatusCapture(
    form: CaptureForm,
    model: OperatingProjectionDto,
    seed: OperatingActivityDto?,
) {
    val chips = ServiceCaptureCopy.modeChips("STATUS")
    val chip = when {
        form.choice.contains("Complete", ignoreCase = true) -> "Complete"
        form.choice.contains("Reschedule", ignoreCase = true) -> "Reschedule"
        else -> "Start"
    }
    ServiceModeChips(chips, chip) { form.choice = it }
    val bookings = model.activity.filter { it.eventType.equals("APPOINTMENT", ignoreCase = true) }
    val bookingLabel = bookings.firstOrNull { it.id == form.parentId }?.title
        ?: seed?.title
        ?: form.purpose.ifBlank { "Select booking" }
    if (bookings.isNotEmpty()) {
        ChoiceBlock(
            "Booking",
            bookings.map { it.title },
            bookingLabel,
        ) { title ->
            val row = bookings.firstOrNull { it.title == title }
            form.parentId = row?.id.orEmpty()
            form.purpose = title
        }
    } else {
        EditField("Booking", form.purpose, "Booking ref") { form.purpose = it }
    }
    ChoiceBlock(
        "Status",
        listOf("In progress", "Completed", "Rescheduled", "No-show", "Cancelled"),
        form.activityStatus.ifBlank {
            when (chip) {
                "Complete" -> "Completed"
                "Reschedule" -> "Rescheduled"
                else -> "In progress"
            }
        },
    ) { form.activityStatus = it }
    EditField("Pro", form.detail, "Staff") { form.detail = it }
    EditField("Room", form.channel, "Room / chair") { form.channel = it }
    ServiceMoreDetails(form.moreOpen) { form.moreOpen = !form.moreOpen }
    if (form.moreOpen) NoteField("Note", form.note, "Optional") { form.note = it }
    ReadField("Due", form.dateText) { form.dateOpen = true }
    ServiceWhyPanel(ServiceCaptureCopy.why("STATUS"))
}

// ---------------------------------------------------------------------------------
// SVC-QA-09 · Budget / Target Revision
// ---------------------------------------------------------------------------------

@Composable
internal fun ServiceBudgetCapture(
    form: CaptureForm,
    currency: String,
) {
    val chips = ServiceCaptureCopy.modeChips("BUDGET")
    val chip = if (form.choice.contains("Session", ignoreCase = true)) "Sessions" else "Charges"
    ServiceModeChips(chips, chip) { form.choice = it }
    AmountBlock(
        ServiceCaptureCopy.amountLabel("BUDGET"),
        currency,
        form.amount,
        form.purpose.ifBlank { ServiceCaptureCopy.amountHint("BUDGET") },
    ) { form.amount = it }
    ReadField("Period", monthPickerLabel(form.eventDate)) { form.monthOpen = true }
    if (chip == "Sessions") {
        EditField("Sessions target", form.quantity, "Count") { form.quantity = it }
    }
    EditField("Previous", form.detail, "Prior version") { form.detail = it }
    ServiceMoreDetails(form.moreOpen) { form.moreOpen = !form.moreOpen }
    if (form.moreOpen) {
        EditField("Revision reason", form.purpose, "Optional") { form.purpose = it }
        NoteField("Note", form.note, "Optional") { form.note = it }
    }
    ServiceWhyPanel(ServiceCaptureCopy.why("BUDGET"))
}

// ---------------------------------------------------------------------------------
// SVC-QA-10 · Issue / Approval / Update
// ---------------------------------------------------------------------------------

@Composable
internal fun ServiceIssueCapture(
    form: CaptureForm,
    actor: String,
) {
    val chips = ServiceCaptureCopy.modeChips("ISSUE")
    val chip = when {
        form.choice.contains("Approval", ignoreCase = true) -> "Approval"
        form.choice.contains("Update", ignoreCase = true) -> "Update"
        else -> "Issue"
    }
    ServiceModeChips(chips, chip) { form.choice = it }
    EditField("Title", form.purpose, "What needs attention") { form.purpose = it }
    if (chip == "Issue") {
        ChoiceBlock(
            "Severity",
            listOf("Info", "Alert", "Critical"),
            form.activityStatus.ifBlank { "Alert" },
        ) { form.activityStatus = it }
    }
    EditField("Owner", form.party, actor.substringBefore(" ·").ifBlank { "Owner" }) { form.party = it }
    EditField("Link", form.itemName, "Bookings / charge / purchase") { form.itemName = it }
    ServiceMoreDetails(form.moreOpen) { form.moreOpen = !form.moreOpen }
    if (form.moreOpen) NoteField("Note", form.note, "Optional") { form.note = it }
    ServiceWhyPanel(ServiceCaptureCopy.why("ISSUE"))
}

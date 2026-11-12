package com.example.momentra.ui.shell.business.service

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.api.OperatingContractBody
import com.example.momentra.data.api.OperatingProjectionDto
import com.example.momentra.ui.theme.PlusJakartaSans
import com.example.momentra.ui.theme.ServicesTeal

/**
 * Phase S — amount-first SVC capture bodies (Finalize Charge … Issue) with Teal chrome.
 * Hold / Transfer / Reprint stay on [ServiceHoldTransferReprintSheet].
 */
@Composable
internal fun ServiceMoneyCaptureSheet(
    sheet: ServiceCaptureSheet,
    model: OperatingProjectionDto,
    onDismiss: () -> Unit,
    onSubmit: (ServiceEventDraft) -> Unit,
) {
    val currency = model.currency ?: "INR"
    var amount by remember { mutableStateOf("") }
    var party by remember { mutableStateOf("") }
    var service by remember { mutableStateOf("") }
    var pay by remember { mutableStateOf("Collect now") }
    var method by remember { mutableStateOf("UPI") }
    var title by remember { mutableStateOf("") }
    var kind by remember { mutableStateOf("") }
    var qty by remember { mutableStateOf("") }

    val postable = when (sheet) {
        ServiceCaptureSheet.SERVICE, ServiceCaptureSheet.PURCHASE, ServiceCaptureSheet.COLLECTION,
        ServiceCaptureSheet.PAYMENT, ServiceCaptureSheet.SPEND, ServiceCaptureSheet.BUDGET,
        -> amount.toBigDecimalOrNull()?.signum() == 1
        ServiceCaptureSheet.APPOINTMENT, ServiceCaptureSheet.STATUS, ServiceCaptureSheet.ISSUE ->
            party.isNotBlank() || title.isNotBlank() || service.isNotBlank()
        ServiceCaptureSheet.STOCK -> productOk(service, qty)
        else -> false
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(ServicesTeal.card)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ServicesTeal.gutter, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TealSheetGrabber()
        TealSheetHeader(title = sheet.title, subtitle = svcSubtitle(sheet), onClose = onDismiss)

        when (sheet) {
            ServiceCaptureSheet.SERVICE -> {
                svcAmountHero("Charge amount", amount, currency, if (service.isBlank()) "Walk-in / package" else service)
                svcKeypad(amount) { amount = it }
                svcField("Client", party) { party = it }
                svcField("Service", service) { service = it }
                svcChips("Payment state", listOf("Collect now", "Part collect", "Pay later"), pay) { pay = it }
                svcGstUnavailable()
            }
            ServiceCaptureSheet.PURCHASE -> {
                svcAmountHero("Purchase amount", amount, currency, party.ifBlank { "Supplier invoice" })
                svcKeypad(amount) { amount = it }
                svcField("Supplier", party) { party = it }
                svcChips("Receive now", listOf("Full", "Partial", "Later"), kind.ifBlank { "Full" }) { kind = it }
                svcChips("Payment state", listOf("Paid now", "Part paid", "Pay later"), pay) { pay = it }
                svcGstUnavailable(label = "Input GST · Unavailable until GRN")
            }
            ServiceCaptureSheet.COLLECTION -> {
                svcAmountHero("Collected", amount, currency, "Allocate open charge AR")
                svcKeypad(amount) { amount = it }
                svcField("Client", party) { party = it }
                svcField("Source charge", title) { title = it }
                svcChips("Method", listOf("UPI", "Cash", "Card", "Bank transfer"), method) { method = it }
            }
            ServiceCaptureSheet.PAYMENT -> {
                svcAmountHero("Paid", amount, currency, "Settle AP · never new purchase")
                svcKeypad(amount) { amount = it }
                svcField("Payee", party) { party = it }
                svcField("Source", title) { title = it }
                svcChips("Method", listOf("Bank transfer", "UPI", "Cash", "Card"), method) { method = it }
            }
            ServiceCaptureSheet.SPEND -> {
                svcChips("Type", listOf("Rent", "Utilities", "Supplies", "Marketing", "Other"), kind.ifBlank { "Utilities" }) { kind = it }
                svcAmountHero("Expense", amount, currency, kind.ifBlank { "Service running" })
                svcKeypad(amount) { amount = it }
                svcField("Payee", party) { party = it }
            }
            ServiceCaptureSheet.STOCK -> {
                svcChips(null, listOf("Set current", "Add", "Reduce", "Transfer"), kind.ifBlank { "Set current" }) { kind = it }
                svcField("Supply / SKU", service) { service = it }
                svcField("Qty", qty) { qty = it.filter { ch -> ch.isDigit() || ch == '.' }.take(10) }
            }
            ServiceCaptureSheet.APPOINTMENT -> {
                svcChips(null, listOf("Appoint", "Walk-in"), kind.ifBlank { "Appoint" }) { kind = it }
                svcField("Client", party) { party = it }
                svcField("Service", service) { service = it }
                Text(
                    "Booking is not revenue or GST",
                    style = ServicesTeal.rowCaption(ServicesTeal.warning),
                )
            }
            ServiceCaptureSheet.STATUS -> {
                svcChips(null, listOf("Start", "Complete", "No-show", "Cancel"), kind.ifBlank { "Start" }) { kind = it }
                svcField("Booking / client", party) { party = it }
                Text(
                    "Status never posts a charge",
                    style = ServicesTeal.rowCaption(ServicesTeal.warning),
                )
            }
            ServiceCaptureSheet.BUDGET -> {
                svcChips(null, listOf("Sales basis", "Visits"), kind.ifBlank { "Sales basis" }) { kind = it }
                svcAmountHero("New target", amount, currency, "Versioned revision")
                svcKeypad(amount) { amount = it }
            }
            ServiceCaptureSheet.ISSUE -> {
                svcChips(null, listOf("Update", "Issue", "Risk", "Approval"), kind.ifBlank { "Issue" }) { kind = it }
                svcField("Title", title) { title = it }
                svcChips("Severity", listOf("Low", "Medium", "High"), method) { method = it }
            }
            else -> Unit
        }

        WhyNote(svcWhy(sheet), tinted = true)
        TealGradientCta(
            label = svcCta(sheet),
            caption = "Draft preserved if you close or open a picker",
            enabled = postable,
            onClick = {
                onSubmit(buildServiceDraft(sheet, amount, party, service, pay, method, title, kind, qty))
            },
        )
        Spacer(Modifier.height(16.dp))
    }
}

private fun productOk(service: String, qty: String) =
    service.isNotBlank() && qty.toBigDecimalOrNull() != null

private fun svcSubtitle(sheet: ServiceCaptureSheet): String = when (sheet) {
    ServiceCaptureSheet.SERVICE -> "Finalize once · GST on charge only"
    ServiceCaptureSheet.APPOINTMENT -> "Booking ≠ revenue"
    ServiceCaptureSheet.STATUS -> "Ops only · no charge"
    ServiceCaptureSheet.COLLECTION -> "Allocate AR · never new charge"
    ServiceCaptureSheet.PAYMENT -> "Settle AP · never new purchase"
    else -> "Consumer Services"
}

private fun svcWhy(sheet: ServiceCaptureSheet): String = when (sheet) {
    ServiceCaptureSheet.SERVICE ->
        "Finalized charge posts revenue once. Booking and completion are not revenue. GST stamps on the charge invoice only — never invent CGST/SGST here."
    ServiceCaptureSheet.PURCHASE ->
        "Purchase opens AP. Stock += accepted qty on receipt. Payment allocates later."
    ServiceCaptureSheet.COLLECTION ->
        "Collection allocates against open charges only — never creates a new SERVICE / SALE."
    ServiceCaptureSheet.PAYMENT ->
        "Payment allocates against open AP only — never creates a new PURCHASE."
    ServiceCaptureSheet.SPEND ->
        "Expense posts service running cost for the period."
    ServiceCaptureSheet.STOCK ->
        "Stock updates are explicit movements — never inferred from booking or charge."
    ServiceCaptureSheet.APPOINTMENT ->
        "Booking reserves capacity only. Charge posts later on Finalize Charge."
    ServiceCaptureSheet.STATUS ->
        "Status updates the appointment board. It never posts revenue or GST."
    ServiceCaptureSheet.BUDGET ->
        "Revisions are versioned and effective-dated — they do not rewrite posted charges."
    ServiceCaptureSheet.ISSUE ->
        "Issues and updates are signals — not money events."
    else -> "Service capture posts through existing operating events."
}

private fun svcCta(sheet: ServiceCaptureSheet): String = when (sheet) {
    ServiceCaptureSheet.SERVICE -> "Post charge"
    ServiceCaptureSheet.PURCHASE -> "Post purchase"
    ServiceCaptureSheet.COLLECTION -> "Post collection"
    ServiceCaptureSheet.PAYMENT -> "Post payment"
    ServiceCaptureSheet.SPEND -> "Post expense"
    ServiceCaptureSheet.STOCK -> "Post stock update"
    ServiceCaptureSheet.APPOINTMENT -> "Save booking"
    ServiceCaptureSheet.STATUS -> "Update status"
    ServiceCaptureSheet.BUDGET -> "Save revision"
    ServiceCaptureSheet.ISSUE -> "Post update"
    else -> "Post"
}

private fun buildServiceDraft(
    sheet: ServiceCaptureSheet,
    amount: String,
    party: String,
    service: String,
    pay: String,
    method: String,
    title: String,
    kind: String,
    qty: String,
): ServiceEventDraft {
    val paymentStatus = when {
        pay.contains("now", true) || pay.contains("Paid", true) || pay.contains("Collect now", true) -> "PAID"
        pay.contains("Part", true) -> "PARTIAL"
        else -> "OPEN"
    }
    return when (sheet) {
        ServiceCaptureSheet.SERVICE -> ServiceEventDraft(
            eventType = "SERVICE",
            title = if (service.isBlank()) "Charge · ${party.ifBlank { "Walk-in" }}" else "Charge · $service",
            amount = amount,
            note = null,
            channelName = null,
            counterpartyName = party.ifBlank { null },
            paymentStatus = paymentStatus,
            itemName = service.ifBlank { null },
            contract = OperatingContractBody(paymentState = pay.uppercase(), taxMode = "UNAVAILABLE"),
        )
        ServiceCaptureSheet.PURCHASE -> ServiceEventDraft(
            eventType = "PURCHASE",
            title = "Purchase · ${party.ifBlank { "Supplier" }}",
            amount = amount,
            note = kind,
            channelName = null,
            counterpartyName = party.ifBlank { null },
            paymentStatus = paymentStatus,
            contract = OperatingContractBody(receiptState = kind.uppercase(), paymentState = pay.uppercase(), taxMode = "UNAVAILABLE"),
        )
        ServiceCaptureSheet.COLLECTION -> ServiceEventDraft(
            eventType = "COLLECTION",
            title = "Collection · ${party.ifBlank { "Client" }}",
            amount = amount,
            note = listOfNotNull(title.takeIf { it.isNotBlank() }, method).joinToString(" · "),
            channelName = null,
            counterpartyName = party.ifBlank { null },
            paymentStatus = "PENDING",
        )
        ServiceCaptureSheet.PAYMENT -> ServiceEventDraft(
            eventType = "PAYMENT",
            title = "Payment · ${party.ifBlank { "Payee" }}",
            amount = amount,
            note = method,
            channelName = null,
            counterpartyName = party.ifBlank { null },
            paymentStatus = "PENDING",
        )
        ServiceCaptureSheet.SPEND -> ServiceEventDraft(
            eventType = "SPEND",
            title = "Expense · ${kind.ifBlank { "Ops" }}",
            amount = amount,
            note = null,
            channelName = null,
            counterpartyName = party.ifBlank { null },
            paymentStatus = null,
            contract = OperatingContractBody(expenseCategory = kind.ifBlank { null }),
        )
        ServiceCaptureSheet.STOCK -> ServiceEventDraft(
            eventType = "STOCK",
            title = "Stock · ${kind.ifBlank { "Set" }} · $service",
            amount = null,
            note = kind,
            channelName = null,
            counterpartyName = null,
            paymentStatus = null,
            quantity = qty,
            itemName = service,
            contract = OperatingContractBody(stockAction = kind.uppercase().replace(' ', '_')),
        )
        ServiceCaptureSheet.APPOINTMENT -> ServiceEventDraft(
            eventType = "APPOINTMENT",
            title = "Booking · ${party.ifBlank { service.ifBlank { "Client" } }}",
            amount = null,
            note = kind,
            channelName = null,
            counterpartyName = party.ifBlank { null },
            paymentStatus = null,
            itemName = service.ifBlank { null },
            contract = OperatingContractBody(appointmentAction = kind.uppercase()),
        )
        ServiceCaptureSheet.STATUS -> ServiceEventDraft(
            eventType = "APPOINTMENT",
            title = "Status · ${kind.ifBlank { "Update" }} · ${party.ifBlank { "Booking" }}",
            amount = null,
            note = "STATUS · $kind",
            channelName = null,
            counterpartyName = party.ifBlank { null },
            paymentStatus = null,
            contract = OperatingContractBody(appointmentAction = kind.uppercase()),
        )
        ServiceCaptureSheet.BUDGET -> ServiceEventDraft(
            eventType = "BUDGET",
            title = "Revision · ${kind.ifBlank { "Sales basis" }}",
            amount = amount,
            note = kind,
            channelName = null,
            counterpartyName = null,
            paymentStatus = null,
        )
        ServiceCaptureSheet.ISSUE -> ServiceEventDraft(
            eventType = if (kind == "Issue" || kind == "Risk") "ISSUE" else "UPDATE",
            title = title.ifBlank { kind.ifBlank { "Update" } },
            amount = null,
            note = "severity=$method",
            channelName = null,
            counterpartyName = null,
            paymentStatus = null,
        )
        else -> ServiceEventDraft(
            eventType = sheet.eventType,
            title = sheet.title,
            amount = amount.ifBlank { null },
            note = null,
            channelName = null,
            counterpartyName = party.ifBlank { null },
            paymentStatus = null,
        )
    }
}

@Composable
private fun svcAmountHero(label: String, amount: String, currency: String, caption: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(ServicesTeal.rCardTight)
            .background(ServicesTeal.chip)
            .border(1.5.dp, ServicesTeal.teal, ServicesTeal.rCardTight)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = ServicesTeal.rowCaption())
            Spacer(Modifier.weight(1f))
            Text(currency, style = ServicesTeal.rowCaption(ServicesTeal.lime).copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold))
        }
        Text(
            if (amount.isBlank()) "₹0" else ServiceFormat.exact(currency, amount),
            style = ServicesTeal.metricValue().copy(fontSize = 36.sp),
            maxLines = 1,
        )
        Text(caption, style = ServicesTeal.rowCaption(), maxLines = 2)
    }
}

@Composable
private fun svcField(label: String, value: String, onChange: (String) -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = ServicesTeal.rowCaption(ServicesTeal.tealMuted))
        Box(
            Modifier
                .fillMaxWidth()
                .height(46.dp)
                .clip(ServicesTeal.rInner)
                .background(ServicesTeal.field)
                .border(1.dp, ServicesTeal.chipBorder, ServicesTeal.rInner)
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = true,
                textStyle = TextStyle(color = ServicesTeal.textPrimary, fontFamily = PlusJakartaSans, fontSize = 14.sp),
                cursorBrush = SolidColor(ServicesTeal.teal),
                decorationBox = { inner ->
                    if (value.isEmpty()) Text(label, style = ServicesTeal.rowCaption(ServicesTeal.textFaint))
                    inner()
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun svcChips(label: String?, options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        label?.let { Text(it, style = ServicesTeal.rowCaption(ServicesTeal.tealMuted)) }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            options.forEach { option ->
                TealModeChip(option, selected = option == selected, onClick = { onSelect(option) })
            }
        }
    }
}

@Composable
private fun svcGstUnavailable(label: String = "GST · Unavailable until charge invoice") {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Column(Modifier.weight(1f)) {
            Text(label, style = ServicesTeal.rowCaption())
            Text("Never invent CGST/SGST/IGST in the sheet", style = ServicesTeal.rowCaption(ServicesTeal.textFaint).copy(fontSize = 10.sp))
        }
        Text("Unavailable", style = ServicesTeal.rowValue(ServicesTeal.warning))
    }
}

@Composable
private fun svcKeypad(value: String, onValueChange: (String) -> Unit) {
    val keys = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf(".", "0", "⌫"),
    )
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        keys.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { key ->
                    Box(
                        Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(ServicesTeal.chip)
                            .border(1.dp, ServicesTeal.chipBorder, RoundedCornerShape(12.dp))
                            .clickable {
                                onValueChange(
                                    when (key) {
                                        "⌫" -> value.dropLast(1)
                                        "." -> if (value.contains('.')) value else if (value.isEmpty()) "0." else value + "."
                                        else -> if (value == "0") key else (value + key).take(12)
                                    },
                                )
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(key, style = ServicesTeal.rowValue().copy(fontSize = 18.sp))
                    }
                }
            }
        }
    }
}

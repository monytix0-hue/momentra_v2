package com.example.momentra.ui.shell.business.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.api.OperatingActivityDto
import com.example.momentra.data.api.OperatingMasterDto
import com.example.momentra.data.api.OperatingPaymentDto
import com.example.momentra.data.api.OperatingProjectionDto
import com.example.momentra.ui.theme.PlusJakartaSans
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

internal val SheetElevated = Color(0xFF3A3842)
internal val SheetFieldBg = Color(0xFF201E28)
internal val SheetHigh = Color(0xFF35333E)
internal val SheetInk = Color(0xFFE5E0EE)
internal val SheetMuted = Color(0xFFC9C4D8)
internal val SheetBrand = Color(0xFFC9BFFF)
internal val SheetOnBrand = Color(0xFF2F009C)
internal val SheetAccent = Color(0xFFB7B8FF)
internal val SheetBorder = Color(0xFF938EA1)
internal val SheetIdentity = Color(0xFF6C4EF2)

data class OperatingCaptureDraft(
    val eventType: String,
    val title: String,
    val amount: String?,
    val note: String?,
    val channelName: String?,
    val counterpartyName: String?,
    val paymentStatus: String?,
    val occurredAt: String?,
    val quantity: String? = null,
    val unit: String? = null,
    val unitPrice: String? = null,
    val itemName: String? = null,
    val parentEventId: String? = null,
    val revisesEventId: String? = null,
    val masterKind: String? = null,
    val dueAt: String? = null,
    val contract: com.example.momentra.data.api.OperatingContractBody? = null,
)

private val dayFormat = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)
private val moneyFormat = NumberFormat.getNumberInstance(Locale.ENGLISH).apply {
    minimumFractionDigits = 2
    maximumFractionDigits = 2
}

private fun money(raw: String?): BigDecimal =
    raw?.toBigDecimalOrNull()?.setScale(2, RoundingMode.HALF_UP) ?: BigDecimal.ZERO.setScale(2)

private fun formatMoney(value: BigDecimal, currency: String): String {
    val text = moneyFormat.format(value)
    return if (currency.equals("INR", ignoreCase = true)) "₹$text" else "$text $currency"
}

private fun contextLine(model: OperatingProjectionDto): String {
    val role = OperatingMomentCatalog.roles.firstOrNull { it.first == model.role }?.second ?: "Member"
    val title = model.title.orEmpty().trim()
    return if (title.isEmpty()) role else "$role · $title"
}

@Composable
fun OperatingSheetScrim(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    val absorb = remember { MutableInteractionSource() }
    Column(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(onClick = onDismiss)
            .padding(top = 80.dp),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(SheetElevated)
                .clickable(interactionSource = absorb, indication = null, onClick = {}),
        ) {
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OperatingCaptureSheet(
    title: String,
    icon: Int,
    eventType: String,
    mode: String,
    model: OperatingProjectionDto,
    channelLabel: String = "Channel",
    variant: String = "",
    onDismiss: () -> Unit,
    onSubmit: suspend (OperatingCaptureDraft) -> Unit,
    onDecide: (suspend (String, String) -> Unit)? = null,
    seed: OperatingActivityDto? = null,
) {
    val currency = model.currency?.ifBlank { null } ?: "INR"
    var eventDate by remember { mutableStateOf(LocalDate.now()) }
    var dateOpen by remember { mutableStateOf(false) }
    val dateText = eventDate.format(dayFormat)
    val actor = contextLine(model)
    var amount by remember { mutableStateOf("") }
    var channel by remember { mutableStateOf(model.life?.channels?.firstOrNull().orEmpty()) }
    var party by remember { mutableStateOf("") }
    var purpose by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var choice by remember(mode, variant) {
        mutableStateOf(
            when {
                variant == "restaurant" && mode == "ORDER" -> "Preparing"
                variant == "service" && mode == "APPOINTMENT" -> "Scheduled"
                variant == "trading" && mode == "ORDER" -> "Ready for dispatch"
                variant == "trading" && mode == "SHIPMENT" -> "Out for delivery"
                variant == "distribution" && mode == "ORDER" -> "In progress"
                variant == "distribution" && mode == "SHIPMENT" -> "Out for delivery"
                (variant == "client" || variant == "own") && mode == "RESULT" -> "Accepted"
                (variant == "client" || variant == "own") && mode == "SPEND" -> "Pending / unpaid"
                variant == "client" && mode == "COLLECTION" -> "Mark Received / Collect"
                variant == "own" && mode == "COLLECTION" -> "Linked campaign receipt"
                variant == "client" && mode == "TARGET" -> "Extend launch outcome"
                variant == "own" && mode == "TARGET" -> "Leads"
                (variant == "client" || variant == "own") && mode == "DELIVERABLE" -> "Deliverable"
                (variant == "client" || variant == "own") && mode == "COMPLETE" -> "Complete"
                (variant == "client" || variant == "own") && mode == "PARTNER" -> "Partner"
                (variant == "standard" || variant == "job") && mode == "SALE" -> "Pending"
                (variant == "standard" || variant == "job") && mode == "ORDER" -> "In production"
                (variant == "standard" || variant == "job") && mode == "DELIVERY" -> "Delivered"
                (variant == "standard" || variant == "job") && mode == "STOCK" -> "Needs check"
                (variant == "standard" || variant == "job") && mode == "BUDGET" -> "Budget"
                else -> defaultChoice(mode)
            },
        )
    }
    var moreOpen by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val typed = amount.toBigDecimalOrNull()
    val channels = model.life?.channels.orEmpty()
    val restaurant = variant == "restaurant"
    val service = variant == "service"
    val trading = variant == "trading"
    val distribution = variant == "distribution"
    val client = variant == "client"
    val own = variant == "own"
    val marketing = client || own
    val standard = variant == "standard"
    val job = variant == "job"
    val production = standard || job
    var link by remember { mutableStateOf("") }
    var itemName by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("piece") }
    var unitPrice by remember { mutableStateOf("") }
    var parentId by remember { mutableStateOf("") }
    var masterKind by remember { mutableStateOf(masterKinds(model.subtype).first()) }
    LaunchedEffect(seed?.id, seed?.parentEventId) {
        val row = seed ?: return@LaunchedEffect
        amount = row.amount.orEmpty()
        party = row.counterpartyName.orEmpty()
        channel = row.channelName ?: channel
        note = row.note.orEmpty()
        purpose = row.title
        itemName = row.title
        quantity = row.quantity.orEmpty()
        unit = row.unit ?: unit
        parentId = row.parentEventId.orEmpty()
    }
    var detail by remember(mode, variant) {
        mutableStateOf(if ((variant == "client" || variant == "own") && mode == "DELIVERABLE") "Due" else "")
    }

    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 17.dp)
                .padding(bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(Modifier.fillMaxWidth().height(22.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.size(width = 36.dp, height = 4.dp).clip(RoundedCornerShape(2.dp)).background(SheetMuted)) {}
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(
                    painterResource(icon),
                    contentDescription = null,
                    tint = SheetInk,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(SheetIdentity)
                        .padding(7.dp),
                )
                Text(title, color = SheetInk, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, modifier = Modifier.weight(1f))
                Text(
                    "Close  ×",
                    color = SheetMuted,
                    fontFamily = PlusJakartaSans,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(SheetHigh)
                        .clickable(onClick = onDismiss)
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                )
            }
            Text(actor, color = SheetAccent, fontFamily = PlusJakartaSans, fontSize = 12.sp)
            Text(statusLine(mode), color = SheetMuted, fontFamily = PlusJakartaSans, fontSize = 12.sp)

            when (mode) {
                "SALE", "SERVICE" -> if (production && mode == "SALE") {
                    ItemLine(itemName, quantity, unit, unitPrice, { itemName = it }, { quantity = it }, { unit = it }, { unitPrice = it }, model.masters.orEmpty())
                    AmountBlock("Amount", currency, amount, "Positive amount · 2 decimal places") { amount = it }
                    if (channels.isNotEmpty()) {
                        ChoiceBlock("Buyer / channel", channels, channel) { channel = it }
                    } else {
                        EditField("Buyer", party, "Required") { party = it }
                    }
                    FieldRow {
                        ReadField("Sale date", dateText, Modifier.weight(1f)) { dateOpen = true }
                        EditField("Fulfilment status", detail, "Dispatched", Modifier.weight(1f)) { detail = it }
                    }
                    ChoiceBlock("Payment state", listOf("Paid", "Pending"), choice) { choice = it }
                    Text(
                        "Buyer and fulfilment status required. Reference and output quantity are optional.",
                        color = SheetMuted,
                        fontFamily = PlusJakartaSans,
                        fontSize = 12.sp,
                    )
                    MoreDetails(moreOpen, "Batch, output quantity, and reference. Optional.") { moreOpen = !moreOpen }
                    if (moreOpen) NoteField("Note", note, "Optional") { note = it }
                    SummaryCard("After save", saleSummary(model, currency, typed, choice == "Paid"))
                } else {
                    ItemLine(itemName, quantity, unit, unitPrice, { itemName = it }, { quantity = it }, { unit = it }, { unitPrice = it }, model.masters.orEmpty())
                    AmountBlock("Amount", currency, amount, "Positive amount · 2 decimal places") { amount = it }
                    if (channels.isNotEmpty()) ChoiceBlock(channelLabel, channels, channel) { channel = it }
                    if (distribution && mode == "SALE") EditField("Buyer / dealer", party, "Required") { party = it }
                    FieldRow {
                        ReadField(if (restaurant || service || trading || distribution) "Sale date" else "Date", dateText, Modifier.weight(1f)) { dateOpen = true }
                        ReadField(if (restaurant || service || trading || distribution) "Receipt actor" else "Recorded by", actor.substringBefore(" ·"), Modifier.weight(1f))
                    }
                    ChoiceBlock(if (trading) "Received status" else "Money received", listOf("Received", "Pending"), choice) { choice = it }
                    Text(
                        when {
                            distribution -> "One sale and its linked receipt. This is not a separate collection."
                            trading -> "One sale and its linked receipt. This is not a second collection."
                            service -> "One sale and its linked receipt. This is not a second sale or collection."
                            restaurant -> "One sale and its linked receipt. This is not a separate collection."
                            else -> "One sale and its receipt. A collection is a separate event."
                        },
                        color = SheetMuted,
                        fontFamily = PlusJakartaSans,
                        fontSize = 12.sp,
                    )
                    MoreDetails(
                        moreOpen,
                        when {
                            distribution -> "Note and reference. Optional."
                            trading -> "Buyer, reference, and shipment context. Optional."
                            service -> "Customer, note and evidence. Optional."
                            else -> "Note, reference and evidence. Optional."
                        },
                    ) { moreOpen = !moreOpen }
                    if (moreOpen) NoteField("Note", note, "Optional") { note = it }
                    SummaryCard(
                        "After save",
                        saleSummary(model, currency, typed, choice == "Received"),
                    )
                }
                "PURCHASE", "VENDOR" -> if (production && mode == "PURCHASE") {
                    ItemLine(itemName, quantity, unit, unitPrice, { itemName = it }, { quantity = it }, { unit = it }, { unitPrice = it }, model.masters.orEmpty())
                    AmountBlock("Amount", currency, amount, "Positive amount · 2 decimal places") { amount = it }
                    FieldRow {
                        EditField("Supplier", party, "Required", Modifier.weight(1f)) { party = it }
                        ReadField("Purchase date", dateText, Modifier.weight(1f)) { dateOpen = true }
                    }
                    EditField("Purpose", purpose, "What was bought") { purpose = it }
                    ChoiceBlock("Payment state", listOf("Pending / unpaid", "Paid"), choice) { choice = it }
                    Text("Pending approval. Submission is not payment.", color = SheetMuted, fontFamily = PlusJakartaSans, fontSize = 12.sp)
                    MoreDetails(moreOpen, "Quantity, reference, and due date. Optional.") { moreOpen = !moreOpen }
                    if (moreOpen) NoteField("Note", note, "Optional") { note = it }
                    SummaryCard("After approval", purchaseSummary(model, currency, typed, choice.startsWith("Pending")))
                } else if (marketing && mode == "VENDOR") {
                    ItemLine(itemName, quantity, unit, unitPrice, { itemName = it }, { quantity = it }, { unit = it }, { unitPrice = it }, model.masters.orEmpty())
                    AmountBlock("Amount", currency, amount, "Positive amount · 2 decimal places") { amount = it }
                    EditField("Counterparty", party, "Required") { party = it }
                    EditField("Purpose", purpose, "What this covers") { purpose = it }
                    ChoiceBlock("Paid / pending", listOf("Pending / unpaid", "Paid"), choice) { choice = it }
                    Text("Pending approval. Submission is not payment.", color = SheetMuted, fontFamily = PlusJakartaSans, fontSize = 12.sp)
                    SummaryCard("After approval", purchaseSummary(model, currency, typed, choice.startsWith("Pending")))
                } else {
                    ItemLine(itemName, quantity, unit, unitPrice, { itemName = it }, { quantity = it }, { unit = it }, { unitPrice = it }, model.masters.orEmpty())
                    AmountBlock("Amount", currency, amount, "Positive amount · 2 decimal places") { amount = it }
                    FieldRow {
                        EditField(if (distribution) "Principal · Optional" else "Supplier · Optional", party, "Name", Modifier.weight(1f)) { party = it }
                        ReadField(if (restaurant || service || trading || distribution) "Purchase date" else "Date", dateText, Modifier.weight(1f)) { dateOpen = true }
                    }
                    EditField("Purpose", purpose, "What was bought") { purpose = it }
                    ChoiceBlock("Payment state", listOf("Pending / unpaid", "Paid"), choice) { choice = it }
                    Text("Pending approval. Submission is not payment.", color = SheetMuted, fontFamily = PlusJakartaSans, fontSize = 12.sp)
                    MoreDetails(moreOpen, "Due date, reference and evidence. Optional.") { moreOpen = !moreOpen }
                    if (moreOpen) NoteField("Note", note, "Optional") { note = it }
                    SummaryCard("After approval", purchaseSummary(model, currency, typed, choice.startsWith("Pending")))
                }
                "SPEND" -> if (production) {
                    AmountBlock("Amount", currency, amount, "Positive amount · 2 decimal places") { amount = it }
                    FieldRow {
                        EditField("Purpose", purpose, "What this was for", Modifier.weight(1f)) { purpose = it }
                        ReadField("Expense date", dateText, Modifier.weight(1f)) { dateOpen = true }
                    }
                    ChoiceBlock("Funding source", listOf("Business", "Personal"), choice) { choice = it }
                    EditField("Paid by", party, actor.substringBefore(" ·")) { party = it }
                    ChoiceBlock("Claim after approval?", listOf("Yes · Reimburse", "No"), detail) { detail = it }
                    Text(
                        "A personal claim is reimbursed later. It is not a second expense.",
                        color = SheetMuted,
                        fontFamily = PlusJakartaSans,
                        fontSize = 12.sp,
                    )
                    MoreDetails(moreOpen, "Reference and note. Optional.") { moreOpen = !moreOpen }
                    if (moreOpen) NoteField("Note", note, "Optional") { note = it }
                    SummaryCard("After approval", spendSummary(model, currency, typed))
                } else if (marketing) {
                    AmountBlock("Amount", currency, amount, "Positive amount · 2 decimal places") { amount = it }
                    EditField("Purpose / Category", purpose, "What this was for") { purpose = it }
                    EditField("Paid by", party, actor.substringBefore(" ·")) { party = it }
                    if (channels.isNotEmpty()) ChoiceBlock(channelLabel, channels, channel) { channel = it }
                    ChoiceBlock("Paid / pending", listOf("Pending / unpaid", "Paid"), choice) { choice = it }
                    SummaryCard("After approval", spendSummary(model, currency, typed))
                } else {
                    AmountBlock("Amount", currency, amount, "Positive amount · 2 decimal places") { amount = it }
                    FieldRow {
                        EditField("Purpose", purpose, "What this was for", Modifier.weight(1f)) { purpose = it }
                        ReadField(if (restaurant || service || trading || distribution) "Expense date" else "Date", dateText, Modifier.weight(1f)) { dateOpen = true }
                    }
                    ChoiceBlock("Funding source", listOf("Business", "Personal"), choice) { choice = it }
                    EditField("Paid by", party, actor.substringBefore(" ·")) { party = it }
                    if (service || trading || distribution) {
                        Text(
                            "A personal claim is reimbursed later. It is not a second expense.",
                            color = SheetMuted,
                            fontFamily = PlusJakartaSans,
                            fontSize = 12.sp,
                        )
                    }
                    MoreDetails(moreOpen, "Reference and note. Optional.") { moreOpen = !moreOpen }
                    if (moreOpen) NoteField("Note", note, "Optional") { note = it }
                    SummaryCard("After approval", spendSummary(model, currency, typed))
                }
                "PAYMENT", "REIMBURSE" -> if (marketing && mode == "PAYMENT") {
                    OpenRecords(mode, model.paymentReadiness, parentId) { row ->
                        parentId = row.id
                        row.counterpartyName?.takeIf { it.isNotBlank() }?.let { party = it }
                    }
                    AmountBlock("Payment amount", currency, amount, "Positive amount · 2 decimal places") { amount = it }
                    EditField("Who paid", detail, "Payer") { detail = it }
                    EditField("Paid to", party, "Required") { party = it }
                    EditField("Purpose / reference", purpose, "Required") { purpose = it }
                    ReadField("Date", dateText) { dateOpen = true }
                    Text("Manual confirmation only. This is not a live transfer.", color = SheetMuted, fontFamily = PlusJakartaSans, fontSize = 12.sp)
                } else {
                    OpenRecords(mode, model.paymentReadiness, parentId) { row ->
                        parentId = row.id
                        row.counterpartyName?.takeIf { it.isNotBlank() }?.let { party = it }
                    }
                    AmountBlock(if (mode == "REIMBURSE") "Reimbursement amount" else "Amount", currency, amount, "Positive amount · 2 decimal places") { amount = it }
                    EditField(
                        when {
                            (service || trading || distribution) && mode == "REIMBURSE" -> "Business payer"
                            mode == "REIMBURSE" -> "Payee"
                            else -> "Paid to"
                        },
                        party,
                        "Name",
                    ) { party = it }
                    ReadField(if ((restaurant || service || trading || distribution) && mode == "REIMBURSE") "Recorded date" else "Date", dateText) { dateOpen = true }
                    NoteField("Reference · Optional", note, "Optional") { note = it }
                    SummaryCard(
                        "After save",
                        listOf("Business paid moves by the amount you enter.", "This settles an existing claim. It is not a second expense."),
                    )
                }
                "COLLECTION" -> if (production) {
                    OpenRecords(mode, model.paymentReadiness, parentId) { row ->
                        parentId = row.id
                        row.counterpartyName?.takeIf { it.isNotBlank() }?.let { party = it }
                    }
                    if (channels.isNotEmpty()) {
                        ChoiceBlock("Received from", channels, channel) { channel = it }
                    } else {
                        EditField("Received from", party, "Required") { party = it }
                    }
                    AmountBlock("Amount received", currency, amount, "Positive amount") { amount = it }
                    FieldRow {
                        ReadField("Received by", actor.substringBefore(" ·"), Modifier.weight(1f))
                        ReadField("Date", dateText, Modifier.weight(1f)) { dateOpen = true }
                    }
                    NoteField("Note", note, "Optional") { note = it }
                    Text("Records the receipt. This is not a live transfer.", color = SheetMuted, fontFamily = PlusJakartaSans, fontSize = 12.sp)
                    SummaryCard("After mark received", collectionSummary(model, currency, typed))
                } else if (client) {
                    OpenRecords(mode, model.paymentReadiness, parentId) { row ->
                        parentId = row.id
                        row.counterpartyName?.takeIf { it.isNotBlank() }?.let { party = it }
                    }
                    ChoiceBlock("Collection path", listOf("Mark Received / Collect", "Create Receivable"), choice) { choice = it }
                    Text(
                        "Mark Received records a receipt. Create Receivable records the obligation.",
                        color = SheetMuted,
                        fontFamily = PlusJakartaSans,
                        fontSize = 12.sp,
                    )
                    AmountBlock("Amount received", currency, amount, "Positive amount") { amount = it }
                    EditField("Received from", party, "Required") { party = it }
                    FieldRow {
                        ReadField("Received by", actor.substringBefore(" ·"), Modifier.weight(1f))
                        ReadField("Date", dateText, Modifier.weight(1f)) { dateOpen = true }
                    }
                    EditField("Purpose / reference", purpose, "Required") { purpose = it }
                    SummaryCard("After mark received", collectionSummary(model, currency, typed))
                } else if (own) {
                    OpenRecords(mode, model.paymentReadiness, parentId) { row ->
                        parentId = row.id
                        row.counterpartyName?.takeIf { it.isNotBlank() }?.let { party = it }
                    }
                    ChoiceBlock("Received from", listOf("Linked campaign receipt", "Other campaign receipt"), choice) { choice = it }
                    Text(
                        "Campaign-linked receipt only. No client billing or receivable.",
                        color = SheetMuted,
                        fontFamily = PlusJakartaSans,
                        fontSize = 12.sp,
                    )
                    AmountBlock("Amount received", currency, amount, "Positive amount") { amount = it }
                    FieldRow {
                        ReadField("Received by", actor.substringBefore(" ·"), Modifier.weight(1f))
                        ReadField("Date", dateText, Modifier.weight(1f)) { dateOpen = true }
                    }
                    EditField("Purpose / reference", purpose, "Required") { purpose = it }
                    SummaryCard("After mark received", collectionSummary(model, currency, typed))
                } else {
                    OpenRecords(mode, model.paymentReadiness, parentId) { row ->
                        parentId = row.id
                        row.counterpartyName?.takeIf { it.isNotBlank() }?.let { party = it }
                    }
                    if (channels.isNotEmpty()) ChoiceBlock(if (distribution) "Buyer / dealer" else "Received from", channels, channel) { channel = it }
                    AmountBlock("Amount received", currency, amount, "Positive amount") { amount = it }
                    FieldRow {
                        ReadField("Received by", actor.substringBefore(" ·"), Modifier.weight(1f))
                        ReadField("Date", dateText, Modifier.weight(1f)) { dateOpen = true }
                    }
                    NoteField("Note · Optional", note, "Optional") { note = it }
                    SummaryCard("After mark received", collectionSummary(model, currency, typed))
                }
                "BUDGET", "TARGET" -> if (production && mode == "BUDGET") {
                    ReadField("Plan month", dateText) { dateOpen = true }
                    ChoiceBlock("Revise", listOf("Budget", "Target"), choice) { choice = it }
                    SummaryCard(
                        "Current plan",
                        listOf(
                            "Budget ${formatMoney(money(model.budget?.amount), currency)} · Target ${formatMoney(money(model.target?.value), currency)}",
                            "The current plan stays active until this revision is approved.",
                        ),
                    )
                    AmountBlock(if (choice == "Target") "Proposed target" else "Proposed budget", currency, amount, "Proposal only") { amount = it }
                    EditField("Revision reason", purpose, "Required") { purpose = it }
                    SummaryCard("After approval", budgetSummary(model, currency, typed))
                } else if (client && mode == "TARGET") {
                    ReadField("Campaign", model.title.orEmpty().ifBlank { "This campaign" })
                    EditField("Metric", purpose, "Required") { purpose = it }
                    ChoiceBlock(
                        "Revision reason",
                        listOf("Extend launch outcome", "Change launch scope", "Correct planning error"),
                        choice,
                    ) { choice = it }
                    NoteField("Revision note", note, "Required") { note = it }
                    ReadField("Effective date", dateText) { dateOpen = true }
                    AmountBlock("Proposed target", currency, amount, "Proposal only") { amount = it }
                    SummaryCard(
                        "Current plan",
                        listOf("The current target stays active until this revision is approved."),
                    )
                } else if (own && mode == "TARGET") {
                    ReadField("Effective date", dateText) { dateOpen = true }
                    ChoiceBlock(
                        "Target measure",
                        listOf("Leads", "Revenue / Sales", "Orders / Conversions", "Custom Count"),
                        choice,
                    ) { choice = it }
                    AmountBlock("Proposed target", currency, amount, "Positive value") { amount = it }
                    EditField("Revision reason", purpose, "Required") { purpose = it }
                    SummaryCard(
                        "Current plan",
                        listOf("The current target stays active until this revision is approved."),
                    )
                } else if (marketing && mode == "BUDGET") {
                    ReadField("Plan month", dateText) { dateOpen = true }
                    SummaryCard(
                        "Current plan",
                        listOf(
                            "Budget ${formatMoney(money(model.budget?.amount), currency)} · Used ${formatMoney(money(model.budget?.spent), currency)}",
                            "The current plan stays active until this revision is approved.",
                        ),
                    )
                    AmountBlock("Proposed budget", currency, amount, "Proposed only. The current plan stays active until approval.") { amount = it }
                    EditField("Revision reason", purpose, "Required") { purpose = it }
                    SummaryCard("After approval", budgetSummary(model, currency, typed))
                } else {
                    ReadField(if (mode == "TARGET") "Plan" else "Plan month", dateText) { dateOpen = true }
                    SummaryCard(
                        "Current plan",
                        listOf(
                            "Budget ${formatMoney(money(model.budget?.amount), currency)} · Used ${formatMoney(money(model.budget?.spent), currency)}",
                            "The current plan stays active until this revision is approved.",
                        ),
                    )
                    AmountBlock(if (mode == "TARGET") "Proposed target" else "Proposed budget", currency, amount, "Proposal only") { amount = it }
                    EditField("Revision reason", purpose, "Why this changes") { purpose = it }
                    SummaryCard("After approval", budgetSummary(model, currency, typed))
                }
                "ORDER" -> if (production) {
                    EditField("Customer", party, "Required") { party = it }
                    ChoiceBlock(
                        "Order status",
                        if (job) {
                            listOf("Open", "In production", "Ready", "Delivered", "Cancelled", "Delayed", "Blocked")
                        } else {
                            listOf("Open", "In production", "Ready", "Delivered")
                        },
                        choice,
                    ) { choice = it }
                    AmountBlock("Value", currency, amount, "Positive amount") { amount = it }
                    ReadField("Due date", dateText) { dateOpen = true }
                    EditField("Reference", purpose, "Optional") { purpose = it }
                    NoteField("Specification note", note, "Optional") { note = it }
                    Text(
                        "An order is a commitment, not revenue.",
                        color = SheetMuted,
                        fontFamily = PlusJakartaSans,
                        fontSize = 12.sp,
                    )
                } else when {
                    restaurant -> {
                        EditField("Order / context", purpose, "What changed") { purpose = it }
                        ChoiceBlock("Order status", listOf("Preparing", "Ready", "Handoff delayed"), choice) { choice = it }
                        NoteField("Operational note", note, "Optional") { note = it }
                        EditField("Customer", party, "Optional") { party = it }
                        FieldRow {
                            ReadField("Recorded by", actor.substringBefore(" ·"), Modifier.weight(1f))
                            ReadField("Draft time", dateText, Modifier.weight(1f)) { dateOpen = true }
                        }
                        SummaryCard(
                            "After save",
                            listOf("Saves an operational note.", "This does not create a sale or a money record."),
                        )
                    }
                    distribution -> {
                        EditField("Buyer / dealer", party, "Required") { party = it }
                        EditField("Order / dispatch reference", purpose, "Required") { purpose = it }
                        ChoiceBlock(
                            "Fulfillment status",
                            listOf("Open", "In progress", "Dispatched", "Completed", "Cancelled", "Delayed", "Blocked"),
                            choice,
                        ) { choice = it }
                        NoteField("Fulfillment note", note, "Optional") { note = it }
                        AmountBlock("Value · Optional", currency, amount, "Leave blank if there is no amount") { amount = it }
                        FieldRow {
                            ReadField("Recorded by", actor.substringBefore(" ·"), Modifier.weight(1f))
                            ReadField("Draft time", dateText, Modifier.weight(1f)) { dateOpen = true }
                        }
                        SummaryCard(
                            "After save",
                            listOf("Saves a dispatch note.", "This does not create a sale or a money record."),
                        )
                    }
                    trading -> {
                        EditField("Buyer / order reference", purpose, "Who and which order") { purpose = it }
                        ChoiceBlock(
                            "Dispatch status",
                            listOf("Pending", "Ready for dispatch", "In progress", "Delayed", "Blocked", "Completed"),
                            choice,
                        ) { choice = it }
                        NoteField("Dispatch note", note, "What changed") { note = it }
                        FieldRow {
                            ReadField("Recorded by", actor.substringBefore(" ·"), Modifier.weight(1f))
                            ReadField("Draft time", dateText, Modifier.weight(1f)) { dateOpen = true }
                        }
                        SummaryCard(
                            "After save",
                            listOf("Saves an operational note.", "Money totals stay unchanged."),
                        )
                    }
                    else -> {
                        NoteField("Order", purpose, "What happened") { purpose = it }
                        EditField("Name", party, "Who this is for") { party = it }
                        FieldRow {
                            ReadField("Recorded by", actor.substringBefore(" ·"), Modifier.weight(1f))
                            ReadField("Draft time", dateText, Modifier.weight(1f)) { dateOpen = true }
                        }
                        SummaryCard("After save", listOf("Saved as activity on this business.", "Money totals stay as they are unless this record carries an amount."))
                    }
                }
                "SHIPMENT" -> if (distribution) {
                    EditField("Shipment / delivery reference", purpose, "Required") { purpose = it }
                    ChoiceBlock(
                        "Delivery status",
                        listOf("Delivered", "Out for delivery", "Delayed", "Blocked", "Cancelled"),
                        choice,
                    ) { choice = it }
                    NoteField("Delivery note", note, "Required") { note = it }
                    EditField("Dealer", party, "Required") { party = it }
                    EditField("Linked dispatch", link, "Optional") { link = it }
                    AmountBlock("Value · Optional", currency, amount, "Leave blank if there is no amount") { amount = it }
                    SummaryCard(
                        "After save",
                        listOf("Saves a delivery note.", "Money totals stay unchanged."),
                    )
                } else if (trading) {
                    EditField("Shipment / delivery reference", purpose, "Which shipment") { purpose = it }
                    ChoiceBlock(
                        "Delivery status",
                        listOf("Pending", "Out for delivery", "Delayed", "Blocked", "Delivered"),
                        choice,
                    ) { choice = it }
                    NoteField("Delivery note", note, "What changed") { note = it }
                    FieldRow {
                        ReadField("Recorded by", actor.substringBefore(" ·"), Modifier.weight(1f))
                        ReadField("Draft time", dateText, Modifier.weight(1f)) { dateOpen = true }
                    }
                    SummaryCard(
                        "After save",
                        listOf("Saves a shipment note.", "Money totals stay unchanged."),
                    )
                } else {
                    NoteField("Shipment", purpose, "What happened") { purpose = it }
                    EditField("Name", party, "Who this is for") { party = it }
                    FieldRow {
                        ReadField("Recorded by", actor.substringBefore(" ·"), Modifier.weight(1f))
                        ReadField("Draft time", dateText, Modifier.weight(1f)) { dateOpen = true }
                    }
                    SummaryCard("After save", listOf("Saved as activity on this business.", "Money totals stay as they are unless this record carries an amount."))
                }
                "APPOINTMENT" -> if (service) {
                    EditField("Job / context", purpose, "What the job is") { purpose = it }
                    if (channels.isNotEmpty()) ChoiceBlock(channelLabel, channels, channel) { channel = it }
                    FieldRow {
                        ReadField("Appointment date", dateText, Modifier.weight(1f)) { dateOpen = true }
                        EditField("Professional", party, "Who is assigned", Modifier.weight(1f)) { party = it }
                    }
                    ChoiceBlock("Job status", listOf("Scheduled", "Completed"), choice) { choice = it }
                    Text(
                        "Job status is operational only. Completing a job does not create revenue or a receivable.",
                        color = SheetMuted,
                        fontFamily = PlusJakartaSans,
                        fontSize = 12.sp,
                    )
                    MoreDetails(moreOpen, "Customer. Optional.") { moreOpen = !moreOpen }
                    if (moreOpen) NoteField("Customer", note, "Optional") { note = it }
                    SummaryCard(
                        "After save",
                        listOf("Saves the job as activity.", "Revenue and collections stay unchanged."),
                    )
                } else {
                    NoteField("Appointment", purpose, "What happened") { purpose = it }
                    EditField("Name", party, "Who this is for") { party = it }
                    FieldRow {
                        ReadField("Recorded by", actor.substringBefore(" ·"), Modifier.weight(1f))
                        ReadField("Draft time", dateText, Modifier.weight(1f)) { dateOpen = true }
                    }
                    SummaryCard("After save", listOf("Saved as activity on this business.", "Money totals stay as they are unless this record carries an amount."))
                }
                "UPDATE", "DELIVERY", "DELIVERABLE", "COMPLETE", "RESULT", "PRODUCTION" -> if (production && mode == "PRODUCTION") {
                    AmountBlock("Quantity / output", currency, amount, "Positive output") { amount = it }
                    FieldRow {
                        EditField("Unit", detail, "Required", Modifier.weight(1f)) { detail = it }
                        ReadField("Date", dateText, Modifier.weight(1f)) { dateOpen = true }
                    }
                    EditField("Output context", purpose, "Required") { purpose = it }
                    Text(
                        if (job) {
                            "Unit compatible with this job context. Custom jobs and unlike units are never aggregated."
                        } else {
                            "Unit compatible with this output context. Unlike units are never aggregated."
                        },
                        color = SheetMuted,
                        fontFamily = PlusJakartaSans,
                        fontSize = 12.sp,
                    )
                    FieldRow {
                        EditField(if (job) "Linked order" else "Batch", link, "Optional", Modifier.weight(1f)) { link = it }
                        EditField(if (job) "Batch / ref" else "Order / ref", party, "Optional", Modifier.weight(1f)) { party = it }
                    }
                    NoteField("Note", note, "Optional") { note = it }
                    ReadField("Recorded by", actor.substringBefore(" ·"))
                    Text(
                        "Catalogue, BOM, and machine setup are optional. Money totals stay unchanged.",
                        color = SheetMuted,
                        fontFamily = PlusJakartaSans,
                        fontSize = 12.sp,
                    )
                } else if (production && mode == "UPDATE") {
                    NoteField("Update text", purpose, "Required") { purpose = it }
                    EditField("Linked operational context", note, "Optional") { note = it }
                    FieldRow {
                        ReadField("Recorded by", actor.substringBefore(" ·"), Modifier.weight(1f))
                        ReadField("Draft time", dateText, Modifier.weight(1f)) { dateOpen = true }
                    }
                    SummaryCard(
                        "After save",
                        listOf("Saved as production activity.", "Money totals stay unchanged."),
                    )
                } else if (production && mode == "DELIVERY") {
                    EditField(if (job) "Order" else "Order / buyer", purpose, "Required") { purpose = it }
                    ChoiceBlock("Delivery status", listOf("Pending", "Partial", "Delivered"), choice) { choice = it }
                    NoteField("Delivered quantity", note, "Required") { note = it }
                    AmountBlock("Value · Optional", currency, amount, "Leave blank if there is no amount") { amount = it }
                    if (job) {
                        FieldRow {
                            EditField("Customer", party, "Required", Modifier.weight(1f)) { party = it }
                            EditField("Linked order", link, "Optional", Modifier.weight(1f)) { link = it }
                        }
                    }
                    ReadField("Delivery date", dateText) { dateOpen = true }
                    Text(
                        "Delivered is not collected. It does not recognize the sale again.",
                        color = SheetMuted,
                        fontFamily = PlusJakartaSans,
                        fontSize = 12.sp,
                    )
                } else if (marketing && mode == "RESULT") {
                    AmountBlock("Value", currency, amount, "Positive amount · 2 decimal places") { amount = it }
                    if (channels.isNotEmpty()) ChoiceBlock(channelLabel, channels, channel) { channel = it }
                    FieldRow {
                        ReadField("Result date", dateText, Modifier.weight(1f)) { dateOpen = true }
                        EditField("Metric", purpose, "What was measured", Modifier.weight(1f)) { purpose = it }
                    }
                    ChoiceBlock("Status", listOf("Accepted", "Pending"), choice) { choice = it }
                    MoreDetails(moreOpen, "Optional note, evidence link or source context.") { moreOpen = !moreOpen }
                    if (moreOpen) NoteField("Note", note, "Optional") { note = it }
                    Text(
                        "Accepted Result counts toward target, not its Receipt.",
                        color = SheetMuted,
                        fontFamily = PlusJakartaSans,
                        fontSize = 12.sp,
                    )
                } else if (marketing && mode == "UPDATE") {
                    NoteField("Update text", purpose, "Required") { purpose = it }
                    EditField("Linked operational context", note, "Optional") { note = it }
                    FieldRow {
                        ReadField("Recorded by", actor.substringBefore(" ·"), Modifier.weight(1f))
                        ReadField("Draft time", dateText, Modifier.weight(1f)) { dateOpen = true }
                    }
                    SummaryCard("After save", listOf("Saved as campaign activity.", "Money totals stay unchanged."))
                } else if (marketing && mode == "DELIVERABLE") {
                    EditField("Deliverable / milestone name", purpose, "Required") { purpose = it }
                    ChoiceBlock("Type", listOf("Deliverable", "Milestone"), choice) { choice = it }
                    EditField("Owner", party, "Required") { party = it }
                    FieldRow {
                        ReadField("Due date", dateText, Modifier.weight(1f)) { dateOpen = true }
                        Column(Modifier.weight(1f)) {
                            ChoiceBlock("Status", listOf("Launched", "Due", "Complete", "Blocked"), detail) { detail = it }
                        }
                    }
                    if (channels.isNotEmpty()) ChoiceBlock("Channel", channels, channel) { channel = it }
                    EditField("Dependency", note, "Optional") { note = it }
                    Text("Launched, Due, Complete, or Blocked. Closing retains obligations.", color = SheetMuted, fontFamily = PlusJakartaSans, fontSize = 12.sp)
                } else if (marketing && mode == "COMPLETE") {
                    EditField("Result / deliverable reference", purpose, "Required") { purpose = it }
                    ChoiceBlock("Completion status", listOf("Assigned", "In review", "Blocked", "Complete", "Partial"), choice) { choice = it }
                    NoteField("Completion note", note, "Required") { note = it }
                    ReadField("Completion date", dateText) { dateOpen = true }
                    Text(
                        "Completion does not add revenue or a receipt.",
                        color = SheetMuted,
                        fontFamily = PlusJakartaSans,
                        fontSize = 12.sp,
                    )
                } else {
                    if (mode == "RESULT" || mode == "PRODUCTION") {
                        AmountBlock(if (mode == "PRODUCTION") "Output" else "Result", currency, amount, "Amount for this record") { amount = it }
                        if (channels.isNotEmpty()) ChoiceBlock(channelLabel, channels, channel) { channel = it }
                    }
                    NoteField(
                        when {
                            (service || trading || distribution) && mode == "UPDATE" -> "Update text"
                            restaurant && mode == "UPDATE" -> "Update"
                            else -> textLabel(mode)
                        },
                        purpose,
                        "What happened",
                    ) { purpose = it }
                    if ((restaurant || service || trading || distribution) && mode == "UPDATE") {
                        EditField(if (service || trading || distribution) "Linked operational context" else "Linked context", note, "Optional") { note = it }
                    }
                    if (mode == "DELIVERY") {
                        EditField("Name", party, "Who this is for") { party = it }
                    }
                    FieldRow {
                        ReadField("Recorded by", actor.substringBefore(" ·"), Modifier.weight(1f))
                        ReadField("Draft time", dateText, Modifier.weight(1f)) { dateOpen = true }
                    }
                    SummaryCard(
                        "After save",
                        when {
                            (trading || distribution) && mode == "UPDATE" -> listOf("Saved as activity on this business.", "Money totals stay unchanged.")
                            service && mode == "UPDATE" -> listOf("Saved as activity on this service.", "Money totals stay unchanged.")
                            restaurant && mode == "UPDATE" -> listOf("Saved as activity on this restaurant.", "Money totals stay unchanged.")
                            else -> listOf("Saved as activity on this business.", "Money totals stay as they are unless this record carries an amount.")
                        },
                    )
                }
                "ISSUE" -> {
                    NoteField("Issue", purpose, "What needs attention") { purpose = it }
                    ChoiceBlock("Priority", listOf("Low", "Normal", "High"), choice) { choice = it }
                    EditField("Owner", party, actor.substringBefore(" ·")) { party = it }
                    if (restaurant || service || trading || distribution || marketing || production) {
                        ReadField("Due date", dateText) { dateOpen = true }
                        EditField("Operational context", note, "Optional") { note = it }
                    }
                    SummaryCard(
                        "After save",
                        listOf("Opens an attention item.", "Money totals stay unchanged."),
                    )
                }
                "STOCK" -> if (production) {
                    EditField("Material / finished-goods context", purpose, "Required") { purpose = it }
                    ChoiceBlock("Quantity / stock status", listOf("Available", "Needs check", "Low availability"), choice) { choice = it }
                    NoteField("Stock note", note, "Required") { note = it }
                    FieldRow {
                        ReadField("Recorded by", actor.substringBefore(" ·"), Modifier.weight(1f))
                        ReadField("Draft time", dateText, Modifier.weight(1f)) { dateOpen = true }
                    }
                    Text(
                        "This is an operational signal. It creates no purchase, sale, or money record.",
                        color = SheetMuted,
                        fontFamily = PlusJakartaSans,
                        fontSize = 12.sp,
                    )
                } else {
                    EditField(if (distribution) "Availability context" else "Stock context", purpose, "What changed") { purpose = it }
                    ChoiceBlock(
                        if (distribution) "Availability status" else "Stock status",
                        when {
                            distribution -> listOf("Available", "Needs check", "Limited availability")
                            trading -> listOf("Available", "Needs check", "Low availability")
                            else -> listOf("In stock", "Needs check", "Low stock")
                        },
                        choice,
                    ) { choice = it }
                    NoteField(if (distribution) "Availability note" else "Stock note", note, if (trading || distribution) "Required" else "What you saw") { note = it }
                    if (trading || distribution) {
                        FieldRow {
                            ReadField("Recorded by", actor.substringBefore(" ·"), Modifier.weight(1f))
                            ReadField("Draft time", dateText, Modifier.weight(1f)) { dateOpen = true }
                        }
                    } else {
                        ReadField("Recorded by", actor.substringBefore(" ·"))
                    }
                    SummaryCard("After save", listOf("Saves an operational signal.", "No purchase, sale, or money record is created."))
                }
                "PERSON", "ROLE", "INVITE" -> {
                    EditField("Name", party, "Person") { party = it }
                    EditField(
                        when {
                            restaurant -> "Restaurant-facing role"
                            service -> "Service-facing role"
                            trading -> "Trade-facing role"
                            distribution -> "Distribution-facing role"
                            marketing -> "Campaign-facing role"
                            production -> "Manufacturing role"
                            else -> "Role"
                        },
                        purpose,
                        "How they work here",
                    ) { purpose = it }
                    ChoiceBlock("Access", OperatingMomentCatalog.roles.map { it.second }, choice) { choice = it }
                    if (restaurant || service || trading || distribution || marketing || production) EditField("Scope", note, "Optional") { note = it }
                    Text("A job label is not a grant. Access applies after the invitation is accepted.", color = SheetMuted, fontFamily = PlusJakartaSans, fontSize = 12.sp)
                }
                "SUPPLIER", "PARTNER" -> if (production && mode == "PARTNER") {
                    EditField("Supplier name", party, "Required") { party = it }
                    Text("Enter a supplier name to enable Save.", color = SheetAccent, fontFamily = PlusJakartaSans, fontSize = 12.sp)
                    EditField("What they supply", purpose, "Optional") { purpose = it }
                    EditField("Payment terms", note, "Optional") { note = it }
                    AmountBlock("Opening due · Optional", currency, amount, "Leave blank if nothing is owed") { amount = it }
                    if (channels.isNotEmpty()) ChoiceBlock("Channel", channels, channel) { channel = it }
                    Text("Saving creates no purchase or expense.", color = SheetMuted, fontFamily = PlusJakartaSans, fontSize = 12.sp)
                } else if (marketing && mode == "PARTNER") {
                    EditField(if (own) "Partner / counterparty" else "Name", party, "Required") { party = it }
                    Text(
                        "Enter a name to enable Save.",
                        color = SheetAccent,
                        fontFamily = PlusJakartaSans,
                        fontSize = 12.sp,
                    )
                    if (client) {
                        ChoiceBlock("Relationship", listOf("Client", "Vendor", "Creator", "Platform", "Partner"), choice) { choice = it }
                        EditField("Purpose", purpose, "Optional") { purpose = it }
                    }
                    if (channels.isNotEmpty()) {
                        ChoiceBlock(if (own) "Accepted channel" else "Channel", channels, channel) { channel = it }
                    }
                    Text(
                        if (own) "Saving creates no spend event." else "Saving creates no purchase or expense.",
                        color = SheetMuted,
                        fontFamily = PlusJakartaSans,
                        fontSize = 12.sp,
                    )
                } else {
                    EditField(
                        when {
                            distribution -> "Principal / supplier name"
                            restaurant || service || trading -> "Supplier name"
                            else -> "Name"
                        },
                        party,
                        "Name",
                    ) { party = it }
                    if (restaurant || service || trading || distribution) {
                        Text(
                            when {
                                service -> "A name is required to save this relationship."
                                distribution -> "Enter principal / supplier name to enable Save."
                                else -> "Enter a supplier name to enable Save."
                            },
                            color = SheetAccent,
                            fontFamily = PlusJakartaSans,
                            fontSize = 12.sp,
                        )
                    }
                    EditField(
                        if (distribution) "Goods supplied" else "What they supply",
                        purpose,
                        when {
                            restaurant -> "Ingredients or packaging"
                            trading || distribution -> "Stock or packaging"
                            else -> "Goods or service"
                        },
                    ) { purpose = it }
                    ChoiceBlock("Payment terms", listOf("Due on receipt", "7 days", "30 days"), choice) { choice = it }
                    AmountBlock("Opening due", currency, amount, "Leave at 0 if nothing is owed") { amount = it }
                    if (channels.isNotEmpty()) ChoiceBlock(if (restaurant || service || trading || distribution) "Where you sell" else "Channel", channels, channel) { channel = it }
                    Text("Saving creates the relationship. It does not create a purchase or an expense.", color = SheetMuted, fontFamily = PlusJakartaSans, fontSize = 12.sp)
                }
                "MASTERS" -> {
                    ChoiceBlock("Master", masterKinds(model.subtype), masterKind) { masterKind = it }
                    EditField("Name", itemName, "Required") { itemName = it }
                    if (masterKind in setOf("PRODUCT", "MATERIAL", "CONSUMABLE")) {
                        ChoiceBlock("Unit", listOf("piece", "kg", "litre", "count"), unit) { unit = it }
                    }
                    EditField("Detail", purpose, "Optional") { purpose = it }
                    Text("Saving adds a selectable record. It does not create a sale or a cost.", color = SheetMuted, fontFamily = PlusJakartaSans, fontSize = 12.sp)
                }
                "APPROVAL" -> ApprovalBody(
                    model,
                    currency,
                    choice,
                    note,
                    { choice = it },
                    { note = it },
                    onDecide,
                    if (marketing) listOf("Approve", "Reject", "Return") else listOf("Approve", "Reject"),
                )
                else -> NoteField("Note", purpose, "What happened") { purpose = it }
            }
            if (error != null) Text(error ?: "", color = Color(0xFFF87171), fontFamily = PlusJakartaSans, fontSize = 12.sp)
        }
        Column(
            Modifier
                .fillMaxWidth()
                .border(1.dp, SheetBorder, RoundedCornerShape(0.dp))
                .padding(horizontal = 17.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(footerLine(mode, restaurant, service, trading, distribution, marketing, client, production, job), color = SheetMuted, fontFamily = PlusJakartaSans, fontSize = 12.sp)
            val action = actionLabel(mode, choice, restaurant, service, trading, distribution, marketing, client, production)
            val enabled = when {
                mode == "SUPPLIER" || mode == "PARTNER" -> party.isNotBlank()
                distribution && mode == "SALE" -> party.isNotBlank()
                distribution && mode == "ORDER" -> party.isNotBlank() && purpose.isNotBlank()
                distribution && mode == "SHIPMENT" -> party.isNotBlank() && purpose.isNotBlank() && note.isNotBlank()
                marketing && mode == "VENDOR" -> party.isNotBlank()
                client && mode == "COLLECTION" -> party.isNotBlank() && purpose.isNotBlank()
                own && mode == "COLLECTION" -> purpose.isNotBlank()
                client && mode == "TARGET" -> purpose.isNotBlank() && note.isNotBlank()
                own && mode == "TARGET" -> purpose.isNotBlank()
                marketing && mode == "BUDGET" -> purpose.isNotBlank()
                marketing && mode == "DELIVERABLE" -> purpose.isNotBlank() && party.isNotBlank()
                marketing && mode == "COMPLETE" -> purpose.isNotBlank() && note.isNotBlank()
                marketing && mode == "PAYMENT" -> party.isNotBlank() && purpose.isNotBlank()
                marketing && mode == "ISSUE" -> purpose.isNotBlank() && party.isNotBlank()
                production && mode == "PRODUCTION" -> detail.isNotBlank() && purpose.isNotBlank()
                production && mode == "SALE" -> if (channels.isNotEmpty()) channel.isNotBlank() else party.isNotBlank()
                production && mode == "PURCHASE" -> party.isNotBlank()
                production && mode == "ORDER" -> party.isNotBlank()
                production && mode == "DELIVERY" -> purpose.isNotBlank() && note.isNotBlank() && (!job || party.isNotBlank())
                production && mode == "STOCK" -> purpose.isNotBlank() && note.isNotBlank()
                production && mode == "BUDGET" -> purpose.isNotBlank()
                production && mode == "ISSUE" -> purpose.isNotBlank() && party.isNotBlank()
                production && mode == "UPDATE" -> purpose.isNotBlank()
                else -> true
            }
            Text(
                action,
                color = if (enabled) SheetOnBrand else SheetMuted,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (enabled) SheetBrand else Color(0xFF353A59))
                    .clickable(enabled = enabled) {
                        if (mode == "APPROVAL") {
                            val item = model.attention.firstOrNull { it.kind == "APPROVAL" }
                            val decide = onDecide
                            if (item == null || decide == null) {
                                error = "Nothing is waiting."
                                return@clickable
                            }
                            if ((choice == "Reject" || choice == "Return") && note.isBlank()) {
                                error = "A reason is required."
                                return@clickable
                            }
                            error = null
                            scope.launch {
                                decide(item.id, if (choice == "Approve") "APPROVED" else "REJECTED")
                                onDismiss()
                            }
                            return@clickable
                        }
                        val draftNote = if (distribution && mode == "SHIPMENT" && link.isNotBlank()) {
                            listOf(note, link).filter { it.isNotBlank() }.joinToString(" · ")
                        } else {
                            note
                        }
                        val draft = buildDraft(
                            mode, eventType, title, amount, channel, party, purpose, draftNote, choice,
                            occurredAtValue(eventDate), trading, distribution, client, own, detail,
                            standard, job, link, itemName, quantity, unit, unitPrice, parentId,
                            seed?.id, masterKind, model.paymentReadiness,
                            retail = model.subtype in setOf(
                                "RETAIL_STORE",
                                "RESTAURANT_FOOD",
                                "CONSUMER_SERVICE",
                                "TRADING_WHOLESALE",
                                "DISTRIBUTION_DEALERSHIP",
                            ) || trading || distribution,
                            restaurant = restaurant,
                            service = service,
                        )
                        if (draft == null) {
                            error = requiredMessage(mode)
                            return@clickable
                        }
                        error = null
                        scope.launch { onSubmit(draft) }
                    }
                    .padding(vertical = 15.dp),
            )
        }
    }
    if (dateOpen) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = localDateToUtcMillis(eventDate))
        DatePickerDialog(
            onDismissRequest = { dateOpen = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { eventDate = utcMillisToLocalDate(it) }
                    dateOpen = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { dateOpen = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
private fun ApprovalBody(
    model: OperatingProjectionDto,
    currency: String,
    choice: String,
    reason: String,
    onChoice: (String) -> Unit,
    onReason: (String) -> Unit,
    onDecide: (suspend (String, String) -> Unit)?,
    decisions: List<String> = listOf("Approve", "Reject"),
) {
    val pending = model.attention.filter { it.kind == "APPROVAL" }
    if (pending.isEmpty() || onDecide == null) {
        Text("Nothing is waiting for a decision.", color = SheetMuted, fontFamily = PlusJakartaSans, fontSize = 14.sp)
        return
    }
    pending.forEach { item ->
        SummaryCard("Waiting", listOf(item.title, "Still pending until you decide."))
    }
    ChoiceBlock("Decision", decisions, choice, onChoice)
    NoteField("Decision reason", reason, if (decisions.contains("Return")) "Required for Reject or Return" else "Required when you reject") { onReason(it) }
    Text("Approval records the decision. It does not move money by itself.", color = SheetMuted, fontFamily = PlusJakartaSans, fontSize = 12.sp)
}

private fun defaultChoice(mode: String): String = when (mode) {
    "SALE", "SERVICE" -> "Received"
    "PURCHASE", "VENDOR" -> "Pending / unpaid"
    "SPEND" -> "Business"
    "ISSUE" -> "Normal"
    "STOCK" -> "Needs check"
    "PERSON", "ROLE", "INVITE" -> "Contributor"
    "SUPPLIER", "PARTNER" -> "Due on receipt"
    "APPROVAL" -> "Approve"
    else -> ""
}

private fun statusLine(mode: String): String = when (mode) {
    "PURCHASE", "VENDOR", "SPEND", "BUDGET", "TARGET" -> "Draft · Not yet submitted"
    "APPROVAL" -> "Decision draft · Still pending"
    else -> "Draft · Not yet saved"
}

private fun footerLine(mode: String, restaurant: Boolean = false, service: Boolean = false, trading: Boolean = false, distribution: Boolean = false, marketing: Boolean = false, client: Boolean = false, production: Boolean = false, job: Boolean = false): String = when (mode) {
    "SALE" -> if (service || distribution) "Draft · Sale entry granted · Not yet saved" else "Draft · Not yet saved"
    "SERVICE" -> "Draft · Not yet saved"
    "PURCHASE", "VENDOR", "SPEND" -> "Submission is not approval or payment"
    "COLLECTION" -> if (marketing || production) "Draft · No live transfer" else "Records the receipt. No live transfer."
    "BUDGET", "TARGET" -> if (marketing && mode == "TARGET") "Draft · Not yet submitted" else "Pending until approved. The current plan stays active."
    "APPROVAL" -> if (marketing) "Decision only · No money execution · Audit preserved" else "Decision only · Audit is kept"
    "SUPPLIER", "PARTNER" -> when {
        service -> "Ready to save. No purchase or expense is created."
        restaurant -> "Relationship only. Saving does not create a purchase or an expense."
        marketing && !client -> "Saving creates no spend event."
        trading || distribution || marketing || production -> "Saving creates no purchase or expense."
        else -> "A name is required"
    }
    "STOCK", "ORDER", "SHIPMENT" -> when {
        production && job && mode == "ORDER" -> "Draft · Value / evidence optional · Not yet saved"
        production && (mode == "ORDER" || mode == "STOCK") -> "Draft · Operational signal, not an inventory ledger"
        restaurant && mode == "ORDER" -> "Draft · Operational note only"
        distribution && mode == "ORDER" -> "Draft · Value optional · Not yet saved"
        distribution && mode == "SHIPMENT" -> "Draft · Not yet saved"
        trading || (distribution && mode == "STOCK") -> "Draft · Operational signal, not an inventory ledger"
        else -> "Draft · Nothing saved yet"
    }
    "APPOINTMENT" -> if (service) "Draft · Assigned job scope · Not yet saved" else "Draft · Nothing saved yet"
    "UPDATE" -> when {
        production -> "Draft · Production activity only · Nothing saved yet"
        marketing -> "Draft · Campaign activity only · Nothing saved yet"
        service -> "Draft · Service activity only · Nothing saved yet"
        trading -> "Draft · Trade activity only · Nothing saved yet"
        distribution -> "Draft · Distribution activity only · Nothing saved yet"
        else -> "Draft · Nothing saved yet"
    }
    "ISSUE" -> if (production || marketing || service || trading || distribution) "Draft · Opens an attention item only after save" else "Draft · Nothing saved yet"
    "REIMBURSE" -> if (service || trading || distribution) "Records the reimbursement. No live transfer." else "Draft · Nothing saved yet"
    "PERSON", "ROLE", "INVITE" -> if (production || marketing || service || trading || distribution) "No access until the invitation is accepted." else "Draft · Nothing saved yet"
    "PAYMENT" -> if (marketing) "Draft · Manual confirmation only · No live transfer" else "Draft · Nothing saved yet"
    "RESULT", "COMPLETE" -> "Draft · Not yet saved"
    "DELIVERABLE" -> if (marketing) "Assigned work only" else "Draft · Nothing saved yet"
    "PRODUCTION" -> "Draft · Not yet saved"
    "DELIVERY" -> if (production && job) "Draft · No evidence attached · Not yet saved" else if (production) "Draft · Operational signal, not an inventory ledger" else "Draft · Nothing saved yet"
    else -> "Draft · Nothing saved yet"
}

private fun actionLabel(mode: String, choice: String, restaurant: Boolean = false, service: Boolean = false, trading: Boolean = false, distribution: Boolean = false, marketing: Boolean = false, client: Boolean = false, production: Boolean = false): String = when (mode) {
    "SALE" -> if (service && choice == "Received") "Mark Received & Save Sale" else "Save Sale"
    "SERVICE" -> "Save Service"
    "PURCHASE", "VENDOR", "SPEND" -> if (service) "Submit for Approval" else "Submit for approval"
    "PAYMENT" -> if (marketing) "Mark Paid" else "Record Payment"
    "REIMBURSE" -> "Mark Reimbursed"
    "COLLECTION" -> "Mark Received"
    "BUDGET" -> if (marketing) "Submit plan revision" else "Submit revision"
    "TARGET" -> when {
        marketing && choice in setOf("Extend launch outcome", "Change launch scope", "Correct planning error") -> "Submit target revision"
        marketing -> "Submit revision"
        else -> "Submit target"
    }
    "UPDATE" -> "Save update"
    "ISSUE" -> "Save issue"
    "STOCK" -> if (distribution) "Save availability update" else "Save stock update"
    "PRODUCTION" -> "Save production"
    "RESULT" -> if (marketing) "Save Result" else "Save result"
    "PERSON", "ROLE", "INVITE" -> "Save person"
    "SUPPLIER", "PARTNER" -> when {
        distribution && mode == "SUPPLIER" -> "Save Principal / Supplier"
        (restaurant || service || trading) && mode == "SUPPLIER" -> "Save Supplier"
        marketing && mode == "PARTNER" -> if (client) "Save partner" else "Save Partners & Channels"
        production && mode == "PARTNER" -> "Save Supplier"
        else -> "Save"
    }
    "APPROVAL" -> when (choice) {
        "Reject" -> "Reject"
        "Return" -> "Return"
        else -> "Approve"
    }
    "ORDER" -> when {
        production -> "Save order"
        restaurant -> "Save order update"
        trading || distribution -> "Save dispatch update"
        else -> "Save order"
    }
    "SHIPMENT" -> when {
        distribution -> "Save delivery update"
        trading -> "Save Delivery Update"
        else -> "Save shipment"
    }
    "DELIVERY" -> "Save delivery"
    "APPOINTMENT" -> if (service) "Save job update" else "Save appointment"
    "DELIVERABLE" -> if (marketing) "Add Deliverable" else "Save deliverable"
    "COMPLETE" -> if (marketing) "Mark Complete" else "Mark complete"
    else -> "Save"
}

private fun textLabel(mode: String): String = when (mode) {
    "UPDATE" -> "Update"
    "PRODUCTION" -> "Production note"
    "RESULT" -> "Result note"
    "ORDER" -> "Order"
    "SHIPMENT" -> "Shipment"
    "DELIVERY" -> "Delivery"
    "APPOINTMENT" -> "Appointment"
    "DELIVERABLE" -> "Deliverable"
    "COMPLETE" -> "What was completed"
    else -> "Note"
}

private fun requiredMessage(mode: String): String = when (mode) {
    "SALE", "SERVICE", "PURCHASE", "VENDOR", "SPEND", "PAYMENT", "REIMBURSE", "COLLECTION", "BUDGET", "TARGET", "RESULT", "PRODUCTION" ->
        "Amount must be positive."
    "ISSUE", "UPDATE", "STOCK", "ORDER", "SHIPMENT", "DELIVERY", "APPOINTMENT", "DELIVERABLE", "COMPLETE" ->
        "A description is required."
    "PERSON", "ROLE", "INVITE", "SUPPLIER", "PARTNER" -> "A name is required."
    else -> "Check the fields and try again."
}

private fun occurredAtValue(date: LocalDate): String? {
    if (date == LocalDate.now()) return null
    return DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")
        .format(date.atStartOfDay(ZoneId.systemDefault()))
}

private fun localDateToUtcMillis(date: LocalDate): Long =
    date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun utcMillisToLocalDate(millis: Long): LocalDate =
    Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()

private fun buildDraft(
    mode: String,
    eventType: String,
    title: String,
    amount: String,
    channel: String,
    party: String,
    purpose: String,
    note: String,
    choice: String,
    occurredAt: String?,
    trading: Boolean = false,
    distribution: Boolean = false,
    client: Boolean = false,
    own: Boolean = false,
    detail: String = "",
    standard: Boolean = false,
    job: Boolean = false,
    link: String = "",
    itemName: String = "",
    quantity: String = "",
    unit: String = "",
    unitPrice: String = "",
    parentId: String = "",
    revisesEventId: String? = null,
    masterKind: String = "",
    readiness: List<OperatingPaymentDto> = emptyList(),
    retail: Boolean = false,
    restaurant: Boolean = false,
    service: Boolean = false,
): OperatingCaptureDraft? {
    if (mode == "MASTERS") {
        if (itemName.isBlank()) return null
        return OperatingCaptureDraft(
            eventType = "UPDATE",
            title = itemName.trim(),
            amount = null,
            note = purpose.ifBlank { null },
            channelName = null,
            counterpartyName = null,
            paymentStatus = null,
            occurredAt = occurredAt,
            unit = unit.ifBlank { null },
            itemName = itemName.trim(),
            masterKind = masterKind,
        )
    }
    val marketing = client || own
    val production = standard || job
    val needsAmount = mode in setOf("SALE", "SERVICE", "PURCHASE", "VENDOR", "SPEND", "PAYMENT", "REIMBURSE", "COLLECTION", "BUDGET", "TARGET", "RESULT", "PRODUCTION")
    val computed = quantity.toBigDecimalOrNull()?.multiply(unitPrice.toBigDecimalOrNull() ?: BigDecimal.ZERO)
    val resolvedAmount = amount.ifBlank {
        computed?.takeIf { it > BigDecimal.ZERO }?.setScale(2, RoundingMode.HALF_UP)?.toPlainString() ?: ""
    }
    val value = resolvedAmount.toBigDecimalOrNull()
    if (needsAmount && (value == null || value <= BigDecimal.ZERO)) return null
    val needsText = mode in setOf("UPDATE", "ISSUE", "STOCK", "ORDER", "SHIPMENT", "DELIVERY", "APPOINTMENT", "DELIVERABLE", "COMPLETE")
    if (needsText && !(production && mode == "ORDER") && purpose.isBlank() && note.isBlank()) return null
    if (trading && mode in setOf("STOCK", "ORDER", "SHIPMENT") && (purpose.isBlank() || note.isBlank())) return null
    if (distribution && mode == "STOCK" && (purpose.isBlank() || note.isBlank())) return null
    if (distribution && mode == "SALE" && party.isBlank()) return null
    if (distribution && mode == "ORDER" && (purpose.isBlank() || party.isBlank())) return null
    if (distribution && mode == "SHIPMENT" && (purpose.isBlank() || note.isBlank() || party.isBlank())) return null
    if (marketing && mode == "RESULT" && purpose.isBlank()) return null
    if (marketing && mode == "VENDOR" && party.isBlank()) return null
    if (client && mode == "COLLECTION" && (party.isBlank() || purpose.isBlank())) return null
    if (own && mode == "COLLECTION" && purpose.isBlank()) return null
    if (client && mode == "TARGET" && (purpose.isBlank() || note.isBlank())) return null
    if (own && mode == "TARGET" && purpose.isBlank()) return null
    if (marketing && mode == "BUDGET" && purpose.isBlank()) return null
    if (marketing && mode == "DELIVERABLE" && (purpose.isBlank() || party.isBlank())) return null
    if (marketing && mode == "COMPLETE" && (purpose.isBlank() || note.isBlank())) return null
    if (marketing && mode == "PAYMENT" && (party.isBlank() || purpose.isBlank())) return null
    if (marketing && mode == "ISSUE" && (purpose.isBlank() || party.isBlank())) return null
    if (production && mode == "PRODUCTION" && (detail.isBlank() || purpose.isBlank())) return null
    if (production && mode == "SALE" && channel.isBlank() && party.isBlank()) return null
    if (production && mode == "PURCHASE" && party.isBlank()) return null
    if (production && mode == "ORDER" && (party.isBlank() || value == null || value <= BigDecimal.ZERO)) return null
    if (production && mode == "DELIVERY" && (purpose.isBlank() || note.isBlank() || (job && party.isBlank()))) return null
    if (production && mode == "DELIVERY" && amount.isNotBlank() && (value == null || value <= BigDecimal.ZERO)) return null
    if (production && mode == "STOCK" && (purpose.isBlank() || note.isBlank())) return null
    if (production && mode == "BUDGET" && purpose.isBlank()) return null
    if (production && mode == "ISSUE" && (purpose.isBlank() || party.isBlank())) return null
    if (mode in setOf("PERSON", "ROLE", "INVITE", "SUPPLIER", "PARTNER") && party.isBlank()) return null
    val open = readiness.filter { row ->
        if (mode == "COLLECTION") row.relevance == "RECEIVABLE" else row.relevance == "PAYABLE" || row.relevance == "REIMBURSEMENT"
    }
    if (mode in setOf("PAYMENT", "REIMBURSE", "COLLECTION") && open.isNotEmpty() && parentId.isBlank() && !(client && mode == "COLLECTION" && choice == "Create Receivable")) return null
    val savedTitle = when (mode) {
        "PERSON", "ROLE", "INVITE", "SUPPLIER", "PARTNER" -> party.trim()
        "UPDATE", "ISSUE", "ORDER", "SHIPMENT", "DELIVERY", "APPOINTMENT", "DELIVERABLE", "COMPLETE" ->
            if (production && mode == "ORDER") party.trim() else purpose.trim().take(80)
        "STOCK" -> purpose.trim().ifBlank { title }
        "PURCHASE", "VENDOR", "SPEND" -> itemName.trim().ifBlank { purpose.trim() }.ifBlank { title }
        "SALE", "SERVICE" -> itemName.trim().ifBlank { title }
        else -> title
    }
    val payment = when (mode) {
        "SALE", "SERVICE" -> when {
            production && choice == "Paid" -> "COLLECTED"
            production -> "PENDING"
            choice == "Received" -> "COLLECTED"
            else -> "PENDING"
        }
        "PURCHASE", "VENDOR" -> if (choice == "Paid") "PAID" else "PENDING"
        "SPEND" -> if (marketing) if (choice == "Paid") "PAID" else "PENDING" else null
        "PAYMENT", "REIMBURSE" -> "PAID"
        "COLLECTION" -> if (client && choice == "Create Receivable") "PENDING" else "COLLECTED"
        else -> null
    }
    val marketingNote = if (marketing) {
        when (mode) {
            "RESULT" -> listOf(purpose, choice, note)
            "COLLECTION" -> listOf(choice, purpose, note)
            "TARGET" -> if (client) listOf(purpose, choice, note) else listOf(choice, purpose)
            "DELIVERABLE" -> listOf(choice, detail, note)
            "COMPLETE" -> listOf(choice, note)
            "PAYMENT" -> listOf(purpose, detail)
            "UPDATE" -> listOf(note)
            "SPEND", "VENDOR" -> listOf(note, choice)
            "BUDGET" -> listOf(purpose)
            "PARTNER" -> if (client) listOf(purpose, choice) else listOf(purpose)
            else -> null
        }?.filter { it.isNotBlank() }?.joinToString(" · ")?.ifBlank { null }
    } else {
        null
    }
    val productionNote = if (production) {
        when (mode) {
            "PRODUCTION" -> listOf(detail, purpose, link, party, note)
            "SALE" -> listOf(detail, note)
            "SPEND" -> listOf(note, choice, detail)
            "ORDER" -> listOf(choice, purpose, note)
            "DELIVERY" -> listOf(choice, note, if (job) party else "", link)
            "STOCK" -> listOf(choice, note)
            "BUDGET" -> listOf(choice, purpose)
            "PARTNER" -> listOf(purpose, note)
            "UPDATE" -> listOf(note)
            else -> null
        }?.filter { it.isNotBlank() }?.joinToString(" · ")?.ifBlank { null }
    } else {
        null
    }
    val savedNote = marketingNote ?: productionNote ?: listOfNotNull(
        purpose.takeIf { it.isNotBlank() && mode !in setOf("PURCHASE", "VENDOR", "SPEND", "UPDATE", "ISSUE") },
        note.takeIf { it.isNotBlank() },
        choice.takeIf { it.isNotBlank() && mode in setOf("ISSUE", "STOCK", "ORDER", "SHIPMENT", "APPOINTMENT", "SPEND", "PERSON", "ROLE", "INVITE", "SUPPLIER", "PARTNER") },
    ).joinToString(" · ").ifBlank { null }
    val retailContract = if (retail || restaurant || service || trading || distribution) {
        val itemized = itemName.isNotBlank() && quantity.isNotBlank() && unitPrice.isNotBlank()
        val salePay = when {
            choice.equals("Received", ignoreCase = true) || choice.equals("Paid", ignoreCase = true) -> "RECEIVED"
            choice.equals("Part paid", ignoreCase = true) -> "PART_PAID"
            else -> "PAY_LATER"
        }
        val purchasePay = when {
            choice.equals("Paid", ignoreCase = true) -> "PAID"
            choice.equals("Part paid", ignoreCase = true) -> "PART_PAID"
            else -> "PAY_LATER"
        }
        val stockAction = when (choice.lowercase()) {
            "set current stock", "set" -> "SET"
            "add" -> "ADD"
            "reduce" -> "REDUCE"
            else -> if (mode == "STOCK") "SET" else null
        }
        val stockReason = when (choice.lowercase()) {
            "opening count", "opening" -> "opening"
            "stocktake" -> "stocktake"
            "damage" -> "damage"
            "waste" -> "waste"
            "return" -> "return"
            "correction" -> "correction"
            else -> if (mode == "STOCK") "stocktake" else null
        }
        val orderStatus = when {
            mode != "ORDER" -> null
            choice.equals("Preparing", ignoreCase = true) || choice.equals("Scheduled", ignoreCase = true) -> "PREPARING"
            choice.equals("Completed", ignoreCase = true) -> "COMPLETED"
            choice.equals("Cancelled", ignoreCase = true) -> "CANCELLED"
            else -> "PLACED"
        }
        val shipmentStatus = when {
            mode !in setOf("SHIPMENT", "DELIVERY") -> null
            choice.contains("deliver", ignoreCase = true) -> "DELIVERED"
            choice.contains("cancel", ignoreCase = true) -> "CANCELLED"
            choice.contains("placed", ignoreCase = true) -> "PLACED"
            else -> "IN_TRANSIT"
        }
        val ownership = when {
            mode !in setOf("PURCHASE", "STOCK") || !(trading || distribution) -> null
            choice.contains("consign", ignoreCase = true) || note.contains("consign", ignoreCase = true) -> "CONSIGNMENT"
            else -> "OWNED"
        }
        val appointmentAction = when (choice.lowercase()) {
            "reschedule" -> "RESCHEDULE"
            "cancel" -> "CANCEL"
            "no-show", "no show" -> "NO_SHOW"
            "start" -> "START"
            "complete" -> "COMPLETE"
            else -> null
        }
        val allocations = if (mode in setOf("COLLECTION", "PAYMENT", "REIMBURSE") && parentId.isNotBlank() && resolvedAmount.isNotBlank()) {
            listOf(com.example.momentra.data.api.OperatingAllocationBody(parentId, resolvedAmount))
        } else null
        val promised = link.takeIf { it.matches(Regex("""\d{4}-\d{2}-\d{2}.*""")) }
        com.example.momentra.data.api.OperatingContractBody(
            entryMode = if (mode in setOf("SALE", "SERVICE")) if (itemized) "ITEMIZED" else "QUICK" else null,
            paymentState = when (mode) {
                "SALE", "SERVICE" -> salePay
                "PURCHASE", "SPEND" -> purchasePay
                else -> null
            },
            receiptState = if (mode == "PURCHASE") "RECEIVED" else null,
            receivedAmount = if (mode in setOf("SALE", "SERVICE") && salePay == "RECEIVED") resolvedAmount.ifBlank { null } else null,
            stockAction = stockAction,
            stockReason = stockReason,
            appointmentAction = appointmentAction,
            orderStatus = orderStatus,
            staffName = if (mode == "APPOINTMENT") detail.ifBlank { null } else null,
            estimatedAmount = if (mode in setOf("ORDER", "APPOINTMENT")) resolvedAmount.ifBlank { null } else null,
            allocations = allocations,
            ownership = ownership,
            shipmentStatus = shipmentStatus,
            promisedAt = if (mode in setOf("ORDER", "SHIPMENT", "DELIVERY")) promised else null,
        )
    } else null
    return OperatingCaptureDraft(
        eventType = if (production && mode == "BUDGET" && choice == "Target") "TARGET" else eventType,
        title = savedTitle.ifBlank { title },
        amount = resolvedAmount.ifBlank { null },
        note = savedNote,
        channelName = channel.ifBlank { null },
        counterpartyName = if (production && mode == "PRODUCTION") null else party.ifBlank { null },
        paymentStatus = payment,
        occurredAt = occurredAt,
        quantity = quantity.ifBlank { null },
        unit = unit.ifBlank { null },
        unitPrice = unitPrice.ifBlank { null },
        itemName = itemName.ifBlank { null },
        parentEventId = parentId.ifBlank { null },
        revisesEventId = revisesEventId?.takeIf { it.isNotBlank() },
        masterKind = if (mode == "MASTERS") masterKind.ifBlank { null } else null,
        dueAt = link.takeIf { it.matches(Regex("""\d{4}-\d{2}-\d{2}.*""")) },
        contract = retailContract,
    )
}

private fun saleSummary(model: OperatingProjectionDto, currency: String, typed: BigDecimal?, received: Boolean): List<String> {
    val add = typed ?: BigDecimal.ZERO
    val sales = money(model.finance?.sales)
    val got = money(model.finance?.received)
    val due = money(model.finance?.outstanding)
    val lines = mutableListOf("Sales ${formatMoney(sales, currency)} → ${formatMoney(sales + add, currency)}")
    if (received) lines += "Received ${formatMoney(got, currency)} → ${formatMoney(got + add, currency)}"
    else lines += "Outstanding ${formatMoney(due, currency)} → ${formatMoney(due + add, currency)}"
    return lines
}

private fun purchaseSummary(model: OperatingProjectionDto, currency: String, typed: BigDecimal?, pending: Boolean): List<String> {
    val add = typed ?: BigDecimal.ZERO
    val used = money(model.budget?.spent)
    val due = money(model.finance?.outstanding)
    val lines = mutableListOf("Used ${formatMoney(used, currency)} → ${formatMoney(used + add, currency)}")
    if (pending) lines += "Payables ${formatMoney(due, currency)} → ${formatMoney(due + add, currency)}"
    lines += "Draft and pending records stay out of the totals until approved."
    return lines
}

private fun spendSummary(model: OperatingProjectionDto, currency: String, typed: BigDecimal?): List<String> {
    val add = typed ?: BigDecimal.ZERO
    val used = money(model.budget?.spent)
    return listOf(
        "Used ${formatMoney(used, currency)} → ${formatMoney(used + add, currency)}",
        "A personal claim is reimbursed later. That is not a second expense.",
    )
}

private fun collectionSummary(model: OperatingProjectionDto, currency: String, typed: BigDecimal?): List<String> {
    val add = typed ?: BigDecimal.ZERO
    val got = money(model.finance?.received)
    val due = money(model.finance?.outstanding)
    return listOf(
        "Received ${formatMoney(got, currency)} → ${formatMoney(got + add, currency)}",
        "Outstanding ${formatMoney(due, currency)} → ${formatMoney((due - add).coerceAtLeast(BigDecimal.ZERO), currency)}",
        "Sales stay unchanged.",
    )
}

private fun budgetSummary(model: OperatingProjectionDto, currency: String, typed: BigDecimal?): List<String> {
    val proposed = typed ?: money(model.budget?.amount)
    val used = money(model.budget?.spent)
    val remaining = (proposed - used).coerceAtLeast(BigDecimal.ZERO)
    return listOf(
        "Used ${formatMoney(used, currency)} of ${formatMoney(proposed, currency)}",
        "Remaining ${formatMoney(remaining, currency)}",
    )
}

@Composable
private fun AmountBlock(label: String, currency: String, value: String, hint: String, onValue: (String) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SheetFieldBg)
            .border(1.dp, SheetBrand, RoundedCornerShape(12.dp))
            .padding(10.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = SheetMuted, fontFamily = PlusJakartaSans, fontSize = 12.sp)
            Text(currency, color = SheetAccent, fontFamily = PlusJakartaSans, fontSize = 12.sp)
        }
        BasicTextField(
            value = value,
            onValueChange = { raw -> onValue(raw.filter { it.isDigit() || it == '.' }) },
            textStyle = TextStyle(color = SheetInk, fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 30.sp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            cursorBrush = SolidColor(SheetBrand),
            decorationBox = { inner ->
                if (value.isEmpty()) Text("0.00", color = SheetMuted, fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 30.sp)
                inner()
            },
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        )
        Text(hint, color = SheetMuted, fontFamily = PlusJakartaSans, fontSize = 12.sp)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChoiceBlock(label: String, options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, color = SheetMuted, fontFamily = PlusJakartaSans, fontSize = 12.sp)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            options.forEach { option ->
                val on = option == selected
                Text(
                    if (on) "✓ $option" else option,
                    color = if (on) SheetAccent else SheetMuted,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (on) SheetIdentity else SheetFieldBg)
                        .border(1.dp, if (on) SheetBrand else SheetBorder, RoundedCornerShape(10.dp))
                        .clickable { onSelect(option) }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun FieldRow(content: @Composable RowScope.() -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), content = content)
}

@Composable
private fun ReadField(
    label: String,
    value: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    onClick: (() -> Unit)? = null,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SheetFieldBg)
            .border(1.dp, SheetBorder, RoundedCornerShape(12.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 10.dp, vertical = 7.dp),
    ) {
        Text(label, color = SheetMuted, fontFamily = PlusJakartaSans, fontSize = 12.sp)
        Text(value, color = SheetInk, fontFamily = PlusJakartaSans, fontSize = 14.sp)
    }
}

@Composable
private fun EditField(label: String, value: String, placeholder: String, modifier: Modifier = Modifier.fillMaxWidth(), onValue: (String) -> Unit) {
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SheetFieldBg)
            .border(1.dp, SheetBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp),
    ) {
        Text(label, color = SheetMuted, fontFamily = PlusJakartaSans, fontSize = 12.sp)
        BasicTextField(
            value = value,
            onValueChange = onValue,
            textStyle = TextStyle(color = SheetInk, fontFamily = PlusJakartaSans, fontSize = 14.sp),
            cursorBrush = SolidColor(SheetBrand),
            decorationBox = { inner ->
                if (value.isEmpty()) Text(placeholder, color = SheetMuted, fontFamily = PlusJakartaSans, fontSize = 14.sp)
                inner()
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun NoteField(label: String, value: String, placeholder: String, onValue: (String) -> Unit) {
    EditField(label, value, placeholder, Modifier.fillMaxWidth(), onValue)
}

@Composable
private fun MoreDetails(open: Boolean, hint: String, onToggle: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onToggle)) {
        Text(if (open) "More details  ∧" else "More details  ∨", color = SheetAccent, fontFamily = PlusJakartaSans, fontSize = 14.sp)
        Text(hint, color = SheetMuted, fontFamily = PlusJakartaSans, fontSize = 12.sp)
    }
}

@Composable
private fun SummaryCard(title: String, lines: List<String>) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SheetHigh)
            .border(1.dp, SheetBorder, RoundedCornerShape(12.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(title, color = SheetAccent, fontFamily = PlusJakartaSans, fontSize = 14.sp)
        lines.forEach { Text(it, color = SheetInk, fontFamily = PlusJakartaSans, fontSize = 14.sp) }
    }
}

internal fun masterKinds(subtype: String?): List<String> = when (subtype) {
    "CONSUMER_SERVICE" -> listOf("SERVICE", "STAFF", "CUSTOMER", "SUPPLIER", "CONSUMABLE", "CHANNEL")
    "CLIENT_CAMPAIGN", "OWN_BUSINESS_CAMPAIGN" -> listOf("METRIC", "CHANNEL", "BUDGET_CATEGORY", "PARTY")
    "STANDARD_PRODUCTION", "MADE_TO_ORDER" -> listOf("MATERIAL", "PRODUCT", "SUPPLIER", "BUYER", "STAFF")
    "TRADING_WHOLESALE", "DISTRIBUTION_DEALERSHIP" -> listOf("PRODUCT", "SUPPLIER", "BUYER", "CHANNEL")
    else -> listOf("PRODUCT", "SUPPLIER", "CUSTOMER", "CHANNEL", "STAFF")
}

fun formatOperatingAmount(currency: String?, raw: String?, visible: Boolean = true): String {
    if (!visible || raw.isNullOrBlank()) return "—"
    val value = raw.toBigDecimalOrNull() ?: return raw
    val locale = if (currency.isNullOrBlank() || currency == "INR") Locale("en", "IN") else Locale.US
    val format = NumberFormat.getNumberInstance(locale).apply {
        minimumFractionDigits = 0
        maximumFractionDigits = 2
    }
    val text = format.format(value)
    return if (currency.isNullOrBlank() || currency == "INR") "₹$text" else "$text $currency"
}

fun formatOperatingDetail(detail: String, currency: String?): String {
    return Regex("""\d+\.\d{4}(?:\s+[A-Z]{3})?""").replace(detail) { match ->
        val token = match.value.substringBefore(' ')
        formatOperatingAmount(currency, token, true)
    }
}

@Composable
private fun ItemLine(
    itemName: String,
    quantity: String,
    unit: String,
    unitPrice: String,
    onItem: (String) -> Unit,
    onQuantity: (String) -> Unit,
    onUnit: (String) -> Unit,
    onPrice: (String) -> Unit,
    masters: List<OperatingMasterDto> = emptyList(),
) {
    if (masters.isNotEmpty()) {
        Text("Saved items", color = SheetMuted, fontFamily = PlusJakartaSans, fontSize = 12.sp)
        masters.forEach { master ->
            Text(
                master.name,
                color = SheetInk,
                fontFamily = PlusJakartaSans,
                fontSize = 13.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, SheetBorder, RoundedCornerShape(8.dp))
                    .clickable {
                        onItem(master.name)
                        master.unit?.let(onUnit)
                    }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            )
        }
    }
    EditField("Item", itemName, "Item name") { onItem(it) }
    FieldRow {
        EditField("Qty", quantity, "1", Modifier.weight(1f)) { onQuantity(it) }
        EditField("Unit", unit, "piece", Modifier.weight(1f)) { onUnit(it) }
        EditField("Price", unitPrice, "0", Modifier.weight(1f)) { onPrice(it) }
    }
    Text("Quantity, unit, and price stay on the sale or purchase. Stock changes only when you record that the goods were received or dispatched.", color = SheetMuted, fontFamily = PlusJakartaSans, fontSize = 12.sp)
}

@Composable
private fun OpenRecords(
    mode: String,
    readiness: List<OperatingPaymentDto>,
    selected: String,
    onPick: (OperatingPaymentDto) -> Unit,
) {
    val rows = readiness.filter { row ->
        if (mode == "COLLECTION") row.relevance == "RECEIVABLE" else row.relevance == "PAYABLE" || row.relevance == "REIMBURSEMENT"
    }
    Text(
        if (rows.isEmpty()) {
            if (mode == "COLLECTION") "No unpaid sale is open." else "No unpaid purchase or expense is open."
        } else if (mode == "COLLECTION") "Unpaid sales" else "Unpaid purchases and expenses",
        color = SheetMuted,
        fontFamily = PlusJakartaSans,
        fontSize = 12.sp,
    )
    rows.forEach { row ->
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(if (selected == row.id) SheetHigh else Color.Transparent)
                .border(1.dp, if (selected == row.id) SheetAccent else SheetBorder, RoundedCornerShape(12.dp))
                .clickable { onPick(row) }
                .padding(10.dp),
        ) {
            Text(row.title, color = SheetInk, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(
                listOfNotNull(row.counterpartyName, row.amount, row.dueAt).filter { it.isNotBlank() }.joinToString(" · "),
                color = SheetMuted,
                fontFamily = PlusJakartaSans,
                fontSize = 12.sp,
            )
        }
    }
}

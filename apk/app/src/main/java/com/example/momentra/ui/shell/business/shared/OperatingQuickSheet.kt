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
    val conversionToBase: String? = null,
    val dueAt: String? = null,
    val contract: com.example.momentra.data.api.OperatingContractBody? = null,
    val masterId: String? = null,
    val rejectedQuantity: String? = null,
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
    /** kind, name, unit, optional conversionToBase */
    onCreateMaster: (suspend (String, String, String?, String?) -> Unit)? = null,
) {
    val currency = model.currency?.ifBlank { null } ?: "INR"
    var eventDate by remember { mutableStateOf(LocalDate.now()) }
    var dateOpen by remember { mutableStateOf(false) }
    var dueDate by remember { mutableStateOf(LocalDate.now()) }
    var dueDateOpen by remember { mutableStateOf(false) }
    val dueDateText = dueDate.format(dayFormat)
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
                (variant == "trading" || variant == "distribution" || variant == "restaurant" || variant == "service") &&
                    mode in setOf("PURCHASE", "SPEND") -> "Pay later"
                (variant == "trading" || variant == "distribution" || variant == "restaurant" || variant == "service") &&
                    mode in setOf("SALE", "SERVICE") -> "Received"
                variant == "restaurant" && mode == "ORDER" -> "Preparing"
                variant == "service" && mode == "APPOINTMENT" -> "Scheduled"
                variant == "trading" && mode == "ORDER" -> "Placed"
                variant == "trading" && mode == "SHIPMENT" -> "In transit"
                variant == "distribution" && mode == "ORDER" -> "Placed"
                variant == "distribution" && mode == "SHIPMENT" -> "In transit"
                (variant == "client" || variant == "own") && mode == "RESULT" -> "Accepted"
                (variant == "client" || variant == "own") && mode == "SPEND" -> "Pending / unpaid"
                variant == "client" && mode == "COLLECTION" -> "Mark Received / Collect"
                variant == "own" && mode == "COLLECTION" -> "Linked campaign receipt"
                variant == "client" && mode == "TARGET" -> "Extend launch outcome"
                variant == "own" && mode == "TARGET" -> "Leads"
                (variant == "client" || variant == "own") && mode == "DELIVERABLE" -> "Deliverable"
                (variant == "client" || variant == "own") && mode == "COMPLETE" -> "Complete"
                (variant == "client" || variant == "own") && mode == "PARTNER" -> "Partner"
                (variant == "standard" || variant == "job") && mode == "SALE" -> "Received"
                (variant == "standard" || variant == "job") && mode == "ORDER" -> "Open"
                (variant == "standard" || variant == "job") && mode == "DELIVERY" -> "Delivered"
                (variant == "standard" || variant == "job") && mode == "STOCK" -> "Add"
                (variant == "standard" || variant == "job") && mode == "PURCHASE" -> "Pay later"
                (variant == "standard" || variant == "job") && mode == "SPEND" -> "Paid"
                (variant == "standard" || variant == "job") && mode == "UPDATE" -> "Update"
                (variant == "standard" || variant == "job") && mode == "PRODUCTION" -> "Completed"
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
    var quantityReceived by remember { mutableStateOf("") }
    var unit by remember {
        mutableStateOf(
            if (variant == "trading" || variant == "distribution" || model.subtype in setOf("TRADING_WHOLESALE", "DISTRIBUTION_DEALERSHIP")) "pc" else "piece",
        )
    }
    var unitPrice by remember { mutableStateOf("") }
    var parentId by remember { mutableStateOf("") }
    var masterKind by remember { mutableStateOf(masterKinds(model.subtype).first()) }
    var entryMode by remember { mutableStateOf("Quick amount") }
    var receiptState by remember {
        mutableStateOf(if (variant == "trading" || variant == "distribution") "Fully received" else "Received")
    }
    var ownership by remember { mutableStateOf("Owned") }
    var stockAction by remember { mutableStateOf("Set current stock") }
    var stockReason by remember { mutableStateOf("Stocktake") }
    var receivedAmount by remember { mutableStateOf("") }
    val contractFamily = restaurant || service || trading || distribution || model.subtype in setOf(
        "RETAIL_STORE", "RESTAURANT_FOOD", "CONSUMER_SERVICE", "TRADING_WHOLESALE", "DISTRIBUTION_DEALERSHIP",
    )
    val lovTrade = trading || distribution || model.subtype in setOf("TRADING_WHOLESALE", "DISTRIBUTION_DEALERSHIP")
    var productMasterId by remember { mutableStateOf<String?>(null) }
    var lovKind by remember { mutableStateOf<String?>(null) }
    var localMasters by remember { mutableStateOf<List<OperatingLovOption>>(emptyList()) }
    var taxMode by remember { mutableStateOf("Not configured") }
    var taxAmount by remember { mutableStateOf("") }
    var originalCurrency by remember { mutableStateOf("") }
    var fxRate by remember { mutableStateOf("") }
    var returnDirection by remember { mutableStateOf("Customer return") }
    var goodsOutcome by remember { mutableStateOf("To stock") }
    var goodsStatusLabel by remember { mutableStateOf("Deliver later") }
    var orderSideLabel by remember { mutableStateOf("Sales order") }
    var movementLabel by remember { mutableStateOf("Outgoing sale") }
    var returnReason by remember { mutableStateOf("") }
    var expenseCategory by remember { mutableStateOf("") }
    fun applyLov(kind: String, option: OperatingLovOption) {
        when (kind) {
            "PRODUCT" -> {
                itemName = option.name
                if (mode == "STOCK") purpose = option.name
                productMasterId = option.id.takeUnless { it.startsWith("local-") || it.startsWith("seed-") }
                option.unit?.takeIf { it.isNotBlank() }?.let { unit = it }
            }
            "BUYER", "CUSTOMER", "SUPPLIER" -> party = option.name
            "CHANNEL" -> channel = option.name
            "EXPENSE_CATEGORY" -> expenseCategory = option.name
            "UOM" -> unit = option.name
        }
    }
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
                    AmountBlock("Amount", currency, amount, "Quick amount posts revenue and no SKU quantity") { amount = it }
                    if (channels.isNotEmpty()) ChoiceBlock("Buyer / channel", channels, channel) { channel = it }
                    else EditField("Buyer", party, "Required when a balance remains") { party = it }
                    ReadField("Sale date", dateText) { dateOpen = true }
                    ChoiceBlock("Payment state", listOf("Received", "Part paid", "Pay later"), choice) { choice = it }
                    if (choice == "Part paid") EditField("Received now", receivedAmount, "Required") { receivedAmount = it }
                    model.activity.filter { it.eventType == "CUSTOMER_ORDER" }.forEach { row ->
                        TextButton(onClick = { parentId = row.id }) { Text(if (parentId == row.id) "Order ${row.title}" else row.title, color = Color.White) }
                    }
                    Text("A sale is not a delivery.", color = SheetMuted, fontFamily = PlusJakartaSans, fontSize = 12.sp)
                } else if (contractFamily) {
                    ChoiceBlock("Entry", listOf("Quick amount", "Itemized"), entryMode) { entryMode = it }
                    if (entryMode == "Itemized") {
                        if (lovTrade) {
                            LovItemLine(itemName, quantity, unit, unitPrice, { lovKind = "PRODUCT" }, { lovKind = "UOM" }, { quantity = it }, { unitPrice = it })
                        } else {
                            ItemLine(itemName, quantity, unit, unitPrice, { itemName = it }, { quantity = it }, { unit = it }, { unitPrice = it }, model.masters.orEmpty())
                        }
                    } else {
                        AmountBlock("Sale amount", currency, amount, "Positive amount · 2 decimal places") { amount = it }
                    }
                    if (lovTrade) LovField(channelLabel, channel, "channel") { lovKind = "CHANNEL" }
                    else if (channels.isNotEmpty()) ChoiceBlock(channelLabel, channels, channel) { channel = it }
                    else EditField(channelLabel, channel, "Required when channel tracking is on") { channel = it }
                    FieldRow {
                        ReadField("Sale date", dateText, Modifier.weight(1f)) { dateOpen = true }
                        ReadField("Receipt actor", actor.substringBefore(" ·"), Modifier.weight(1f))
                    }
                    ChoiceBlock("Payment state", listOf("Received", "Part paid", "Pay later"), choice) { choice = it }
                    if (choice == "Part paid") EditField("Received amount", receivedAmount, "Less than the total") { receivedAmount = it }
                    if (choice != "Received") {
                        if (lovTrade) LovField(if (distribution) "Buyer / dealer" else "Buyer", party, "buyer") { lovKind = "BUYER" }
                        else EditField("Customer", party, "Required when a balance remains") { party = it }
                        ReadField("Due date", dueDateText) { dueDateOpen = true }
                    }
                    if (lovTrade) {
                        val openOrders = model.activity.filter { it.eventType == "ORDER" && it.orderSide != "PURCHASE" && it.status != "COMPLETED" && it.status != "CANCELLED" }
                        if (openOrders.isNotEmpty()) {
                            Text("Sales order", color = SheetMuted, fontFamily = PlusJakartaSans, fontSize = 12.sp)
                            openOrders.take(6).forEach { row ->
                                TextButton(onClick = {
                                    parentId = row.id
                                    row.counterpartyName?.takeIf { it.isNotBlank() }?.let { party = it }
                                    row.channelName?.takeIf { it.isNotBlank() }?.let { channel = it }
                                    row.quantity?.takeIf { it.isNotBlank() }?.let { quantity = it }
                                    row.unit?.takeIf { it.isNotBlank() }?.let { unit = it }
                                    if (itemName.isBlank()) itemName = row.title
                                }) {
                                    Text(if (parentId == row.id) "Using ${row.title}" else row.title, color = Color.White, fontSize = 13.sp)
                                }
                            }
                        }
                        ChoiceBlock("Goods", listOf("Fully delivered", "Partially delivered", "Deliver later"), goodsStatusLabel) { goodsStatusLabel = it }
                        if (goodsStatusLabel == "Partially delivered") {
                            EditField("Delivered quantity", quantity, "Does not change stock") { quantity = it }
                        }
                        Text("Goods status does not post stock. Dispatch stays a delivery.", color = SheetMuted, fontFamily = PlusJakartaSans, fontSize = 12.sp)
                    }
                    Text(
                        if (trading || distribution) {
                            "A quick amount does not change stock. Itemized sales do not deduct stock until dispatch."
                        } else {
                            "One sale and its linked receipt. This is not a separate collection."
                        },
                        color = SheetMuted,
                        fontFamily = PlusJakartaSans,
                        fontSize = 12.sp,
                    )
                    MoreDetails(moreOpen, if (lovTrade) "Tax, FX, note, and reference. Optional." else "Note and reference. Optional.") { moreOpen = !moreOpen }
                    if (moreOpen) {
                        if (lovTrade) {
                            TaxFxFields(currency, taxMode, taxAmount, originalCurrency, fxRate, { taxMode = it }, { taxAmount = it }, { originalCurrency = it }, { fxRate = it })
                        }
                        NoteField("Note", note, "Optional") { note = it }
                    }
                    SummaryCard("After save", saleSummary(model, currency, typed, choice == "Received"))
                } else {
                    ItemLine(itemName, quantity, unit, unitPrice, { itemName = it }, { quantity = it }, { unit = it }, { unitPrice = it }, model.masters.orEmpty())
                    AmountBlock("Amount", currency, amount, "Positive amount · 2 decimal places") { amount = it }
                    if (channels.isNotEmpty()) ChoiceBlock(channelLabel, channels, channel) { channel = it }
                    FieldRow {
                        ReadField("Date", dateText, Modifier.weight(1f)) { dateOpen = true }
                        ReadField("Recorded by", actor.substringBefore(" ·"), Modifier.weight(1f))
                    }
                    ChoiceBlock("Money received", listOf("Received", "Pending"), choice) { choice = it }
                    Text("One sale and its receipt. A collection is a separate event.", color = SheetMuted, fontFamily = PlusJakartaSans, fontSize = 12.sp)
                    MoreDetails(moreOpen, "Note, reference and evidence. Optional.") { moreOpen = !moreOpen }
                    if (moreOpen) NoteField("Note", note, "Optional") { note = it }
                    SummaryCard("After save", saleSummary(model, currency, typed, choice == "Received"))
                }
                "PURCHASE", "VENDOR" -> if (production && mode == "PURCHASE") {
                    AmountBlock("Amount", currency, amount, "Counted once toward budget and payables") { amount = it }
                    FieldRow {
                        EditField("Supplier", party, "Required", Modifier.weight(1f)) { party = it }
                        ReadField("Purchase date", dateText, Modifier.weight(1f)) { dateOpen = true }
                    }
                    EditField("Material", itemName, "Optional item") { itemName = it }
                    FieldRow {
                        EditField("Qty", quantity, "Qty", Modifier.weight(1f)) { quantity = it }
                        EditField("Unit", unit, "kg", Modifier.weight(1f)) { unit = it }
                    }
                    ChoiceBlock("Materials received", listOf("Received", "Part received", "Awaiting"), receiptState) { receiptState = it }
                    if (receiptState == "Part received") EditField("Quantity received", quantityReceived, "Required") { quantityReceived = it }
                    ChoiceBlock("Payment state", listOf("Paid", "Part paid", "Pay later"), choice) { choice = it }
                    Text("Stock increases only for the quantity you mark as received.", color = SheetMuted, fontFamily = PlusJakartaSans, fontSize = 12.sp)
                } else if (marketing && mode == "VENDOR") {
                    ItemLine(itemName, quantity, unit, unitPrice, { itemName = it }, { quantity = it }, { unit = it }, { unitPrice = it }, model.masters.orEmpty())
                    AmountBlock("Amount", currency, amount, "Positive amount · 2 decimal places") { amount = it }
                    EditField("Counterparty", party, "Required") { party = it }
                    EditField("Purpose", purpose, "What this covers") { purpose = it }
                    ChoiceBlock("Paid / pending", listOf("Pending / unpaid", "Paid"), choice) { choice = it }
                    Text("Pending approval. Submission is not payment.", color = SheetMuted, fontFamily = PlusJakartaSans, fontSize = 12.sp)
                    SummaryCard("After approval", purchaseSummary(model, currency, typed, choice.startsWith("Pending")))
                } else if (contractFamily) {
                    if (lovTrade) {
                        LovItemLine(itemName, quantity, unit, unitPrice, { lovKind = "PRODUCT" }, { lovKind = "UOM" }, { quantity = it }, { unitPrice = it })
                    } else {
                        ItemLine(itemName, quantity, unit, unitPrice, { itemName = it }, { quantity = it }, { unit = it }, { unitPrice = it }, model.masters.orEmpty())
                    }
                    AmountBlock("Amount", currency, amount, "Positive amount, or leave blank when items have prices") { amount = it }
                    if (lovTrade) LovField(if (distribution) "Principal / supplier" else "Supplier", party, "supplier") { lovKind = "SUPPLIER" }
                    else EditField(if (distribution) "Principal / supplier" else "Supplier", party, "Required") { party = it }
                    ReadField("Purchase date", dateText) { dateOpen = true }
                    if (lovTrade) {
                        val purchaseOrders = model.activity.filter { it.eventType == "ORDER" && it.orderSide == "PURCHASE" && it.status != "COMPLETED" && it.status != "CANCELLED" }
                        if (purchaseOrders.isNotEmpty()) {
                            Text("Purchase order", color = SheetMuted, fontFamily = PlusJakartaSans, fontSize = 12.sp)
                            purchaseOrders.take(6).forEach { row ->
                                TextButton(onClick = {
                                    parentId = row.id
                                    row.counterpartyName?.takeIf { it.isNotBlank() }?.let { party = it }
                                    row.quantity?.takeIf { it.isNotBlank() }?.let { quantity = it }
                                    row.unit?.takeIf { it.isNotBlank() }?.let { unit = it }
                                    if (itemName.isBlank()) itemName = row.title
                                }) {
                                    Text(if (parentId == row.id) "Using ${row.title}" else row.title, color = Color.White, fontSize = 13.sp)
                                }
                            }
                        }
                        ChoiceBlock("Goods receipt", listOf("Fully received", "Partially received", "Receive later"), receiptState) { receiptState = it }
                    } else {
                        ChoiceBlock("Goods receipt", listOf("Received", "Part received", "Awaiting receipt"), receiptState) { receiptState = it }
                    }
                    if (receiptState == "Part received" || receiptState == "Partially received") {
                        EditField("Quantity received", quantityReceived, "Less than ordered qty") { quantityReceived = it }
                    }
                    if (trading || distribution) {
                        ChoiceBlock("Ownership", listOf("Owned", "Consignment"), ownership) { ownership = it }
                        Text("Consignment is not owned stock.", color = SheetMuted, fontFamily = PlusJakartaSans, fontSize = 12.sp)
                    }
                    ChoiceBlock("Payment state", listOf("Paid", "Part paid", "Pay later"), choice) { choice = it }
                    if (choice == "Part paid") EditField("Amount paid", receivedAmount, "Less than the total") { receivedAmount = it }
                    if (choice != "Paid") ReadField("Due date", dueDateText) { dueDateOpen = true }
                    Text("Purchase obligation is not payment. Stock moves only for received quantity.", color = SheetMuted, fontFamily = PlusJakartaSans, fontSize = 12.sp)
                    MoreDetails(moreOpen, if (lovTrade) "Tax, FX, and note. Optional." else "Note. Optional.") { moreOpen = !moreOpen }
                    if (moreOpen) {
                        if (lovTrade) {
                            TaxFxFields(currency, taxMode, taxAmount, originalCurrency, fxRate, { taxMode = it }, { taxAmount = it }, { originalCurrency = it }, { fxRate = it })
                        }
                        NoteField("Note", note, "Optional") { note = it }
                    }
                    SummaryCard("After save", purchaseSummary(model, currency, typed, choice != "Paid"))
                } else {
                    ItemLine(itemName, quantity, unit, unitPrice, { itemName = it }, { quantity = it }, { unit = it }, { unitPrice = it }, model.masters.orEmpty())
                    AmountBlock("Amount", currency, amount, "Positive amount · 2 decimal places") { amount = it }
                    FieldRow {
                        EditField("Supplier · Optional", party, "Name", Modifier.weight(1f)) { party = it }
                        ReadField("Date", dateText, Modifier.weight(1f)) { dateOpen = true }
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
                } else if (lovTrade) {
                    AmountBlock("Amount", currency, amount, "Positive amount · 2 decimal places") { amount = it }
                    LovField("Expense category", expenseCategory, "category") { lovKind = "EXPENSE_CATEGORY" }
                    FieldRow {
                        EditField("Purpose", purpose, "What this was for", Modifier.weight(1f)) { purpose = it }
                        ReadField("Expense date", dateText, Modifier.weight(1f)) { dateOpen = true }
                    }
                    ChoiceBlock("Payment state", listOf("Paid", "Part paid", "Pay later"), choice) { choice = it }
                    if (choice != "Paid") {
                        EditField("Payee", party, "Required") { party = it }
                        ReadField("Due date", dueDateText) { dueDateOpen = true }
                    }
                    if (choice == "Part paid") EditField("Amount paid", receivedAmount, "Less than the total") { receivedAmount = it }
                    MoreDetails(moreOpen, "Note. Optional.") { moreOpen = !moreOpen }
                    if (moreOpen) NoteField("Note", note, "Optional") { note = it }
                    SummaryCard("After save", spendSummary(model, currency, typed))
                } else {
                    AmountBlock("Amount", currency, amount, "Positive amount · 2 decimal places") { amount = it }
                    FieldRow {
                        EditField("Purpose", purpose, "What this was for", Modifier.weight(1f)) { purpose = it }
                        ReadField(if (restaurant || service) "Expense date" else "Date", dateText, Modifier.weight(1f)) { dateOpen = true }
                    }
                    ChoiceBlock("Funding source", listOf("Business", "Personal"), choice) { choice = it }
                    EditField("Paid by", party, actor.substringBefore(" ·")) { party = it }
                    if (service) {
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
                    EditField("Buyer", party, "Required") { party = it }
                    EditField("Quantity", quantity, "Required") { quantity = it }
                    EditField("Unit", unit, "pcs") { unit = it }
                    AmountBlock("Order value · Optional", currency, amount, "Leave blank when the order is unpriced") { amount = it }
                    ReadField("Order date", dateText) { dateOpen = true }
                    Text("An order is not revenue, cash, stock, or a receivable.", color = SheetMuted, fontFamily = PlusJakartaSans, fontSize = 12.sp)
                } else when {
                    restaurant || model.subtype == "RETAIL_STORE" -> {
                        if (channels.isNotEmpty()) ChoiceBlock("Order channel", channels, channel) { channel = it }
                        else EditField("Order channel", channel, "Required") { channel = it }
                        ChoiceBlock("Order status", listOf("Placed", "Preparing", "Completed", "Cancelled"), choice.ifBlank { "Placed" }) { choice = it }
                        model.activity.filter { it.eventType == "ORDER" }.forEach { row ->
                            TextButton(onClick = { parentId = row.id }) {
                                Text(if (parentId == row.id) "Updating ${row.title}" else row.title, color = Color.White, fontFamily = PlusJakartaSans, fontSize = 13.sp)
                            }
                        }
                        EditField("Guest or customer", party, "Optional") { party = it }
                        AmountBlock("Estimated amount", currency, amount, "Pipeline only") { amount = it }
                        ReadField("Order time", dateText) { dateOpen = true }
                        Text("An order is not a sale until you bill it.", color = SheetMuted, fontFamily = PlusJakartaSans, fontSize = 12.sp)
                    }
                    lovTrade -> {
                        ChoiceBlock("Order", listOf("Sales order", "Purchase order"), orderSideLabel) { orderSideLabel = it }
                        if (lovTrade) {
                            LovItemLine(itemName, quantity, unit, unitPrice, { lovKind = "PRODUCT" }, { lovKind = "UOM" }, { quantity = it }, { unitPrice = it })
                        }
                        LovField(if (orderSideLabel == "Purchase order") "Supplier" else "Buyer", party, "party") {
                            lovKind = if (orderSideLabel == "Purchase order") "SUPPLIER" else "BUYER"
                        }
                        LovField("Channel", channel, "channel") { lovKind = "CHANNEL" }
                        ChoiceBlock("Status", listOf("Placed", "Preparing", "Completed", "Cancelled"), choice.ifBlank { "Placed" }) { choice = it }
                        AmountBlock("Estimated amount", currency, amount, "Commitment only") { amount = it }
                        EditField("Promised date", link, "YYYY-MM-DD") { link = it }
                        Text("An order is not a sale or a purchase until you bill it.", color = SheetMuted, fontFamily = PlusJakartaSans, fontSize = 12.sp)
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
                "SHIPMENT" -> if (lovTrade) {
                    ChoiceBlock("Movement", listOf("Outgoing sale", "Incoming purchase"), movementLabel) {
                        movementLabel = it
                        parentId = ""
                    }
                    val sourceType = if (movementLabel == "Incoming purchase") "PURCHASE" else "SALE"
                    val sources = model.activity.filter { it.eventType == sourceType }
                    Text(
                        if (sources.isEmpty()) {
                            if (sourceType == "SALE") "No posted sale to deliver." else "No posted purchase to receive."
                        } else "Source",
                        color = SheetMuted,
                        fontFamily = PlusJakartaSans,
                        fontSize = 12.sp,
                    )
                    sources.take(8).forEach { row ->
                        TextButton(onClick = {
                            parentId = row.id
                            purpose = row.title
                            row.counterpartyName?.takeIf { it.isNotBlank() }?.let { party = it }
                            row.quantity?.takeIf { it.isNotBlank() }?.let { quantity = it }
                            row.unit?.takeIf { it.isNotBlank() }?.let { unit = it }
                        }) {
                            Text(if (parentId == row.id) "Using ${row.title}" else row.title, color = Color.White, fontSize = 13.sp)
                        }
                    }
                    EditField("Quantity", quantity, "Required") { quantity = it }
                    LovField("Unit", unit, "UOM") { lovKind = "UOM" }
                    ChoiceBlock("Status", listOf("Placed", "In transit", "Delivered", "Cancelled"), choice.ifBlank { "In transit" }) { choice = it }
                    Text("Delivery moves goods against the source. It does not create another sale or purchase.", color = SheetMuted, fontFamily = PlusJakartaSans, fontSize = 12.sp)
                } else if (distribution) {
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
                    EditField("Service", purpose, "Required") { purpose = it }
                    ReadField("Appointment time", dateText) { dateOpen = true }
                    EditField("Guest or customer", party, "Optional for a walk-in") { party = it }
                    EditField("Assigned person", detail, "Optional") { detail = it }
                    model.activity.filter { it.eventType == "APPOINTMENT" }.forEach { row ->
                        TextButton(onClick = { parentId = row.id }) {
                            Text(if (parentId == row.id) "Updating ${row.title}" else row.title, color = Color.White, fontFamily = PlusJakartaSans, fontSize = 13.sp)
                        }
                    }
                    if (parentId.isNotBlank() || seed?.id?.isNotBlank() == true) {
                        ChoiceBlock("Action", listOf("Reschedule", "Cancel", "No-show", "Start", "Complete"), choice) { choice = it }
                    }
                    Text(
                        "Booking, starting, and completing do not post revenue. Finalize a charge separately.",
                        color = SheetMuted,
                        fontFamily = PlusJakartaSans,
                        fontSize = 12.sp,
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
                "UPDATE", "DELIVERY", "DELIVERABLE", "COMPLETE", "RESULT", "PRODUCTION" -> if (lovTrade && mode == "UPDATE") {
                    EditField("Update", purpose, "What happened") { purpose = it }
                    ReadField("Date", dateText) { dateOpen = true }
                    Text("Saved as activity. Money totals stay unchanged.", color = SheetMuted, fontFamily = PlusJakartaSans, fontSize = 12.sp)
                } else if (production && mode == "PRODUCTION") {
                    ChoiceBlock("Output", listOf("Quick output", "Detailed batch"), entryMode) { entryMode = it }
                    AmountBlock("Output quantity", currency, amount, "Recorded output. This is not accepted stock.") { amount = it }
                    FieldRow {
                        EditField("Unit", detail, "Required", Modifier.weight(1f)) { detail = it }
                        ReadField("Date", dateText, Modifier.weight(1f)) { dateOpen = true }
                    }
                    EditField(if (entryMode == "Detailed batch") "Finished product" else "Output context", purpose, "Required for a detailed batch") { purpose = it }
                    if (entryMode == "Detailed batch") {
                        EditField("Accepted quantity", quantity, "Required to add finished stock") { quantity = it }
                        EditField("Rejected quantity", note, "Optional") { note = it }
                        ChoiceBlock("Status", listOf("Planned", "In progress", "Completed"), choice) { choice = it }
                    }
                    model.activity.filter { it.eventType == "CUSTOMER_ORDER" }.forEach { row ->
                        TextButton(onClick = { parentId = row.id }) { Text(row.title, color = Color.White) }
                    }
                    Text("Quick output does not create a finished SKU.", color = SheetMuted, fontFamily = PlusJakartaSans, fontSize = 12.sp)
                } else if (production && mode == "UPDATE") {
                    ChoiceBlock("Record", listOf("Update", "Issue", "Approval", "Budget", "Target"), choice) { choice = it }
                    if (choice == "Budget" || choice == "Target") {
                        AmountBlock(if (choice == "Target") "Proposed target" else "Proposed budget", currency, amount, "Effective when approved") { amount = it }
                    }
                    NoteField("What happened", purpose, "Required") { purpose = it }
                } else if (production && mode == "DELIVERY") {
                    Text("Customer order", color = SheetMuted, fontFamily = PlusJakartaSans, fontSize = 12.sp)
                    model.activity.filter { it.eventType == "CUSTOMER_ORDER" }.forEach { row ->
                        TextButton(onClick = { parentId = row.id }) { Text(if (parentId == row.id) "Selected ${row.title}" else row.title, color = Color.White) }
                    }
                    ChoiceBlock("Delivery", listOf("Quick status", "Itemized"), entryMode) { entryMode = it }
                    ChoiceBlock("Status", listOf("Planned", "Dispatched", "Delivered", "Accepted", "Returned"), choice) { choice = it }
                    if (entryMode == "Itemized") {
                        EditField("Delivered quantity", quantity, "Cannot exceed the open order") { quantity = it }
                        EditField("Unit", unit, "pcs") { unit = it }
                    }
                    ReadField("Delivery date", dateText) { dateOpen = true }
                    Text("A status-only delivery does not post a quantity or a sale.", color = SheetMuted, fontFamily = PlusJakartaSans, fontSize = 12.sp)
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
                "STOCK" -> if (contractFamily && (trading || distribution || restaurant || service)) {
                    if (lovTrade) LovField("Item", purpose, "product") { lovKind = "PRODUCT" }
                    else EditField("Item", purpose, "Product or SKU") { purpose = it }
                    ChoiceBlock("Action", listOf("Set current stock", "Add", "Reduce"), stockAction) { stockAction = it }
                    EditField("Quantity", quantity, "Required") { quantity = it }
                    if (lovTrade) LovField("Unit", unit, "UOM") { lovKind = "UOM" }
                    else EditField("Unit", unit, "pcs, kg, L") { unit = it }
                    ChoiceBlock("Reason", listOf("Opening count", "Stocktake", "Damage", "Waste", "Return", "Correction"), stockReason) { stockReason = it }
                    if (trading || distribution) {
                        ChoiceBlock("Ownership", listOf("Owned", "Consignment"), ownership) { ownership = it }
                    }
                    EditField("Reorder point", detail, "Optional") { detail = it }
                    Text("Set replaces the count. Add and reduce move it. This is not a purchase.", color = SheetMuted, fontFamily = PlusJakartaSans, fontSize = 12.sp)
                } else if (production) {
                    EditField("Item", purpose, "Required") { purpose = it }
                    ChoiceBlock("Action", listOf("Set", "Add", "Reduce"), choice) { choice = it }
                    ChoiceBlock("Reason", listOf("opening", "stocktake", "damage", "waste", "return", "correction"), note) { note = it }
                    FieldRow {
                        EditField("Quantity", quantity, "Required", Modifier.weight(1f)) { quantity = it }
                        EditField("Unit", unit, "kg", Modifier.weight(1f)) { unit = it }
                    }
                    Text("Damage is not an expense or a sale.", color = SheetMuted, fontFamily = PlusJakartaSans, fontSize = 12.sp)
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
                "RETURN" -> {
                    val customerReturn = returnDirection.startsWith("Customer")
                    val sourceType = if (customerReturn) "SALE" else "PURCHASE"
                    ChoiceBlock("Return type", listOf("Customer return", "Supplier return"), returnDirection) {
                        returnDirection = it
                        parentId = ""
                    }
                    val sources = model.activity
                        .filter { it.eventType == sourceType && it.approvalStatus != "REJECTED" && it.approvalStatus?.startsWith("PENDING") != true }
                        .take(25)
                    Text(
                        if (sources.isEmpty()) {
                            if (customerReturn) "No posted sale to return against." else "No posted purchase to return against."
                        } else if (customerReturn) "Source sale" else "Source purchase",
                        color = SheetMuted,
                        fontFamily = PlusJakartaSans,
                        fontSize = 12.sp,
                    )
                    sources.forEach { row ->
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (parentId == row.id) SheetHigh else Color.Transparent)
                                .border(1.dp, if (parentId == row.id) SheetAccent else SheetBorder, RoundedCornerShape(12.dp))
                                .clickable {
                                    parentId = row.id
                                    if (amount.isBlank()) row.amount?.takeIf { it.isNotBlank() }?.let { amount = it }
                                    row.counterpartyName?.takeIf { it.isNotBlank() }?.let { party = it }
                                }
                                .padding(10.dp),
                        ) {
                            Text(row.title, color = SheetInk, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(
                                listOfNotNull(row.counterpartyName, row.amount, row.occurredAt.take(10)).filter { it.isNotBlank() }.joinToString(" · "),
                                color = SheetMuted,
                                fontFamily = PlusJakartaSans,
                                fontSize = 12.sp,
                            )
                        }
                    }
                    AmountBlock("Credit amount", currency, amount, "Positive amount · cannot exceed the source") { amount = it }
                    ChoiceBlock("Goods", listOf("To stock", "Not returned", "Pending inspection"), goodsOutcome) { goodsOutcome = it }
                    EditField("Reason", returnReason, "Required") { returnReason = it.take(80) }
                    ChoiceBlock("Items", listOf("Quick amount", "Itemized"), entryMode) { entryMode = it }
                    if (entryMode == "Itemized") {
                        LovItemLine(itemName, quantity, unit, unitPrice, { lovKind = "PRODUCT" }, { lovKind = "UOM" }, { quantity = it }, { unitPrice = it }, showPrice = false)
                    } else if (goodsOutcome == "To stock") {
                        FieldRow {
                            EditField("Qty returned", quantity, "Required", Modifier.weight(1f)) { quantity = it }
                            LovField("UOM", unit, "UOM", Modifier.weight(1f)) { lovKind = "UOM" }
                        }
                    }
                    ReadField("Return date", dateText) { dateOpen = true }
                    NoteField("Note · Optional", note, "Optional") { note = it }
                    Text(
                        "A return is a credit against the source. Stock moves only when goods go to stock.",
                        color = SheetMuted,
                        fontFamily = PlusJakartaSans,
                        fontSize = 12.sp,
                    )
                    SummaryCard(
                        "After save",
                        listOf(
                            if (customerReturn) "Credits the customer against the source sale." else "Credits the supplier against the source purchase.",
                            "No refund is paid by this record.",
                        ),
                    )
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
                mode == "RETURN" -> parentId.isNotBlank() && returnReason.isNotBlank()
                lovTrade && mode == "SPEND" -> expenseCategory.isNotBlank() && amount.toBigDecimalOrNull()?.let { it > BigDecimal.ZERO } == true && (choice == "Paid" || party.isNotBlank())
                lovTrade && mode == "SALE" -> {
                    val amountOk = if (entryMode.equals("Itemized", ignoreCase = true)) itemName.isNotBlank() else amount.toBigDecimalOrNull()?.let { it > BigDecimal.ZERO } == true
                    amountOk && channel.isNotBlank() && (choice == "Received" || party.isNotBlank())
                }
                lovTrade && mode == "STOCK" -> purpose.isNotBlank() && quantity.isNotBlank()
                lovTrade && mode == "UPDATE" -> purpose.isNotBlank()
                lovTrade && mode == "ISSUE" -> purpose.isNotBlank()
                lovTrade && mode == "ORDER" -> party.isNotBlank() || channel.isNotBlank()
                lovTrade && mode == "SHIPMENT" -> parentId.isNotBlank() && quantity.isNotBlank()
                mode == "SUPPLIER" || mode == "PARTNER" -> party.isNotBlank()
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
                production && mode == "SALE" -> amount.toBigDecimalOrNull()?.let { it > BigDecimal.ZERO } == true && (choice == "Received" || party.isNotBlank() || channel.isNotBlank())
                production && mode == "PURCHASE" -> party.isNotBlank() && amount.toBigDecimalOrNull()?.let { it > BigDecimal.ZERO } == true
                production && mode == "ORDER" -> party.isNotBlank() && quantity.isNotBlank()
                production && mode == "DELIVERY" -> parentId.isNotBlank() && (entryMode != "Itemized" || quantity.isNotBlank())
                production && mode == "STOCK" -> purpose.isNotBlank() && quantity.isNotBlank()
                production && mode == "PRODUCTION" -> amount.toBigDecimalOrNull()?.let { it > BigDecimal.ZERO } == true && detail.isNotBlank() && (entryMode != "Detailed batch" || purpose.isNotBlank())
                production && mode == "UPDATE" -> purpose.isNotBlank() || choice == "Budget" || choice == "Target"
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
                        if (lovTrade && mode in setOf("SALE", "PURCHASE") && originalCurrency.isNotBlank()) {
                            val rate = fxRate.toBigDecimalOrNull()
                            if (rate == null || rate <= BigDecimal.ZERO) {
                                error = "Enter an FX rate for the original currency."
                                return@clickable
                            }
                        }
                        val draftNote = if (distribution && mode == "SHIPMENT" && link.isNotBlank()) {
                            listOf(note, link).filter { it.isNotBlank() }.joinToString(" · ")
                        } else {
                            note
                        }
                        val unpaidDue = when (mode) {
                            "SALE", "SERVICE" -> choice != "Received"
                            "PURCHASE", "SPEND" -> choice != "Paid"
                            else -> false
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
                            entryMode = entryMode,
                            receiptState = receiptState,
                            ownership = ownership,
                            stockActionLabel = stockAction,
                            stockReasonLabel = stockReason,
                            receivedAmount = receivedAmount,
                            productMasterId = productMasterId,
                            taxModeLabel = taxMode,
                            taxAmount = taxAmount,
                            originalCurrency = originalCurrency,
                            fxRate = fxRate,
                            returnDirectionLabel = returnDirection,
                            goodsOutcomeLabel = goodsOutcome,
                            returnReason = returnReason,
                            expenseCategory = expenseCategory,
                            lovTrade = lovTrade,
                            quantityReceived = quantityReceived,
                            orderSideLabel = orderSideLabel,
                            goodsStatusLabel = goodsStatusLabel,
                            dueAtDay = when {
                                mode == "ISSUE" -> eventDate.toString()
                                unpaidDue -> dueDate.toString()
                                else -> link.takeIf { it.matches(Regex("""\d{4}-\d{2}-\d{2}.*""")) }
                            },
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
    if (dueDateOpen) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = localDateToUtcMillis(dueDate))
        DatePickerDialog(
            onDismissRequest = { dueDateOpen = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { dueDate = utcMillisToLocalDate(it) }
                    dueDateOpen = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { dueDateOpen = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
    lovKind?.let { kind ->
        SearchableOperatingLovSheet(
            title = lovTitle(kind),
            kind = kind,
            options = lovOptions(kind, model.masters.orEmpty(), channels, localMasters).let { rows ->
                if (!kind.equals("UOM", ignoreCase = true)) rows
                else {
                    val base = model.masters.orEmpty().firstOrNull { it.id == productMasterId }?.unit
                        ?: localMasters.firstOrNull { it.kind.equals("PRODUCT", true) && (it.name == itemName || it.name == purpose) }?.unit
                    val conversion = model.masters.orEmpty().firstOrNull { it.id == productMasterId }?.conversionToBase
                    if (base.isNullOrBlank()) rows else rows.filter { OperatingSeedUoms.compatible(it.name, base, conversion) }
                }
            },
            canAdd = onCreateMaster != null,
            unitChoices = if (kind.equals("PRODUCT", ignoreCase = true)) OperatingSeedUoms.all else emptyList(),
            accent = if (distribution) Color(0xFF66C7A4) else Color(0xFF6DDCFF),
            onSelect = { option -> applyLov(kind, option) },
            onAdd = { name, unitArg, conversion ->
                val created = OperatingLovOption(
                    id = "local-$kind-${name.lowercase()}",
                    name = name,
                    kind = kind,
                    unit = unitArg ?: if (kind == "UOM") name else null,
                    subtitle = unitArg,
                )
                localMasters = localMasters + created
                applyLov(kind, created)
                onCreateMaster?.let { create -> scope.launch { create(kind, name, unitArg, conversion) } }
            },
            onDismiss = { lovKind = null },
        )
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
    "RETURN" -> "Draft · Credit note only · No refund is paid"
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
    "RETURN" -> "Save return"
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
    "RETURN" -> "Choose the source, a positive credit amount, and a reason."
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

private fun payCode(label: String): String = when {
    label.equals("Received", true) -> "RECEIVED"
    label.equals("Paid", true) -> "PAID"
    label.equals("Part paid", true) -> "PART_PAID"
    else -> "PAY_LATER"
}

private fun manufacturingDraft(
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
    detail: String,
    link: String,
    itemName: String,
    quantity: String,
    unit: String,
    parentId: String,
    entryMode: String,
    receiptState: String,
    receivedAmount: String,
    quantityReceived: String,
): OperatingCaptureDraft? {
    fun draft(
        type: String,
        savedTitle: String,
        savedAmount: String?,
        qty: String? = null,
        savedUnit: String? = null,
        parent: String? = null,
        payment: String? = null,
        contract: com.example.momentra.data.api.OperatingContractBody? = null,
        rejected: String? = null,
        item: String? = null,
    ) = OperatingCaptureDraft(
        eventType = type,
        title = savedTitle.ifBlank { title },
        amount = savedAmount,
        note = null,
        channelName = channel.ifBlank { null },
        counterpartyName = party.ifBlank { null },
        paymentStatus = payment,
        occurredAt = occurredAt,
        quantity = qty,
        unit = savedUnit,
        itemName = item,
        parentEventId = parent,
        contract = contract,
        rejectedQuantity = rejected,
    )
    return when (mode) {
        "PRODUCTION" -> {
            val output = amount.toBigDecimalOrNull()
            if (output == null || output <= BigDecimal.ZERO || detail.isBlank()) return null
            val detailed = entryMode.equals("Detailed batch", true)
            if (detailed && purpose.isBlank()) return null
            draft(
                "PRODUCTION",
                if (detailed) purpose else purpose.ifBlank { title },
                amount,
                qty = amount,
                savedUnit = detail,
                parent = parentId.ifBlank { null },
                contract = com.example.momentra.data.api.OperatingContractBody(
                    entryMode = if (detailed) "ITEMIZED" else "QUICK",
                    acceptedQuantity = if (detailed) quantity.ifBlank { null } else null,
                    productionStatus = if (!detailed) null else when (choice) {
                        "Planned" -> "PLANNED"
                        "In progress" -> "IN_PROGRESS"
                        else -> "COMPLETED"
                    },
                ),
                rejected = if (detailed) note.ifBlank { null } else null,
            )
        }
        "PURCHASE" -> {
            val value = amount.toBigDecimalOrNull()
            if (party.isBlank() || value == null || value <= BigDecimal.ZERO) return null
            val receipt = when {
                receiptState.equals("Part received", true) -> "PART_RECEIVED"
                receiptState.equals("Awaiting", true) || receiptState.equals("Awaiting receipt", true) -> "AWAITING"
                else -> "RECEIVED"
            }
            draft(
                "PURCHASE",
                itemName.ifBlank { purpose.ifBlank { title } },
                amount,
                qty = quantity.ifBlank { null },
                savedUnit = unit.ifBlank { null },
                contract = com.example.momentra.data.api.OperatingContractBody(
                    entryMode = if (itemName.isBlank()) "QUICK" else "ITEMIZED",
                    paymentState = payCode(choice),
                    receiptState = receipt,
                    receivedAmount = if (choice.equals("Part paid", true)) receivedAmount.ifBlank { null } else null,
                    quantityReceived = if (receipt == "PART_RECEIVED") quantityReceived.ifBlank { null } else quantity.ifBlank { null },
                ),
                item = itemName.ifBlank { null },
            )
        }
        "SPEND" -> {
            val value = amount.toBigDecimalOrNull()
            if (value == null || value <= BigDecimal.ZERO) return null
            if (!choice.equals("Paid", true) && party.isBlank()) return null
            draft("SPEND", purpose.ifBlank { title }, amount, contract = com.example.momentra.data.api.OperatingContractBody(paymentState = payCode(choice)))
        }
        "SALE" -> {
            val value = amount.toBigDecimalOrNull()
            if (value == null || value <= BigDecimal.ZERO) return null
            if (!choice.equals("Received", true) && party.isBlank() && channel.isBlank()) return null
            draft(
                "SALE",
                title,
                amount,
                parent = parentId.ifBlank { null },
                contract = com.example.momentra.data.api.OperatingContractBody(
                    entryMode = "QUICK",
                    paymentState = payCode(choice),
                    receivedAmount = if (choice.equals("Part paid", true)) receivedAmount.ifBlank { null } else null,
                ),
            )
        }
        "ORDER" -> {
            if (party.isBlank() || quantity.isBlank()) return null
            draft(
                "CUSTOMER_ORDER",
                party,
                amount.ifBlank { null },
                qty = quantity,
                savedUnit = unit.ifBlank { null },
                payment = "NONE",
                contract = com.example.momentra.data.api.OperatingContractBody(orderStatus = "PLACED", estimatedAmount = amount.ifBlank { null }),
            )
        }
        "DELIVERY" -> {
            if (parentId.isBlank()) return null
            val itemized = entryMode.equals("Itemized", true)
            if (itemized && (quantity.toBigDecimalOrNull() ?: BigDecimal.ZERO) <= BigDecimal.ZERO) return null
            draft(
                "DELIVERY",
                "Delivery",
                null,
                qty = if (itemized) quantity else null,
                savedUnit = if (itemized) unit.ifBlank { null } else null,
                parent = parentId,
                contract = com.example.momentra.data.api.OperatingContractBody(
                    entryMode = if (itemized) "ITEMIZED" else "QUICK",
                    deliveryStatus = when (choice) {
                        "Planned" -> "PLANNED"
                        "Dispatched" -> "DISPATCHED"
                        "Accepted" -> "ACCEPTED"
                        "Returned" -> "RETURNED"
                        else -> "DELIVERED"
                    },
                ),
            )
        }
        "COLLECTION" -> {
            val value = amount.toBigDecimalOrNull()
            if (value == null || value <= BigDecimal.ZERO || party.isBlank()) return null
            draft("COLLECTION", "Collection", amount, parent = parentId.ifBlank { null }, payment = "COLLECTED")
        }
        "PAYMENT" -> {
            val value = amount.toBigDecimalOrNull()
            if (value == null || value <= BigDecimal.ZERO || parentId.isBlank()) return null
            draft("PAYMENT", "Payment", amount, parent = parentId, payment = "PAID")
        }
        "STOCK" -> {
            if (purpose.isBlank()) return null
            val action = when (choice.lowercase()) {
                "reduce" -> "REDUCE"
                "add" -> "ADD"
                else -> "SET"
            }
            if (action != "SET" && (quantity.toBigDecimalOrNull() ?: BigDecimal.ZERO) <= BigDecimal.ZERO) return null
            draft(
                "STOCK",
                purpose,
                null,
                qty = quantity.ifBlank { "0" },
                savedUnit = unit.ifBlank { null },
                contract = com.example.momentra.data.api.OperatingContractBody(
                    stockAction = action,
                    stockReason = note.ifBlank { "correction" },
                    declaredQuantity = if (action == "SET") quantity.ifBlank { null } else null,
                ),
            )
        }
        "BUDGET" -> {
            val value = amount.toBigDecimalOrNull()
            if (value == null || value <= BigDecimal.ZERO || purpose.isBlank()) return null
            draft(if (choice == "Target") "TARGET" else "BUDGET", purpose, amount)
        }
        "UPDATE", "ISSUE" -> {
            if (choice == "Budget" || choice == "Target") {
                val value = amount.toBigDecimalOrNull()
                if (value == null || value <= BigDecimal.ZERO || purpose.isBlank()) return null
                draft(if (choice == "Target") "TARGET" else "BUDGET", purpose, amount)
            } else {
                if (purpose.isBlank()) return null
                val code = when (choice) {
                    "Issue" -> "ISSUE"
                    "Approval" -> "APPROVAL"
                    else -> "UPDATE"
                }
                draft(code, purpose, null)
            }
        }
        else -> null
    }
}

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
    entryMode: String = "Quick amount",
    receiptState: String = "Received",
    ownership: String = "Owned",
    stockActionLabel: String = "Set current stock",
    stockReasonLabel: String = "Stocktake",
    receivedAmount: String = "",
    productMasterId: String? = null,
    taxModeLabel: String = "Not configured",
    taxAmount: String = "",
    originalCurrency: String = "",
    fxRate: String = "",
    returnDirectionLabel: String = "Customer return",
    goodsOutcomeLabel: String = "To stock",
    returnReason: String = "",
    expenseCategory: String = "",
    lovTrade: Boolean = false,
    quantityReceived: String = "",
    dueAtDay: String? = null,
    orderSideLabel: String = "Sales order",
    goodsStatusLabel: String = "Deliver later",
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
    if (production && mode in setOf("PRODUCTION", "PURCHASE", "SPEND", "SALE", "ORDER", "DELIVERY", "PAYMENT", "COLLECTION", "STOCK", "BUDGET", "UPDATE", "ISSUE")) {
        return manufacturingDraft(
            mode, eventType, title, amount, channel, party, purpose, note, choice, occurredAt,
            detail, link, itemName, quantity, unit, parentId, entryMode, receiptState, receivedAmount, quantityReceived,
        )
    }
    val needsAmount = mode in setOf("SALE", "SERVICE", "PURCHASE", "VENDOR", "SPEND", "PAYMENT", "REIMBURSE", "COLLECTION", "BUDGET", "TARGET", "RESULT", "PRODUCTION", "RETURN")
    val computed = quantity.toBigDecimalOrNull()?.multiply(unitPrice.toBigDecimalOrNull() ?: BigDecimal.ZERO)
    val resolvedAmount = amount.ifBlank {
        computed?.takeIf { it > BigDecimal.ZERO }?.setScale(2, RoundingMode.HALF_UP)?.toPlainString() ?: ""
    }
    val value = resolvedAmount.toBigDecimalOrNull()
    if (needsAmount && (value == null || value <= BigDecimal.ZERO)) return null
    val contractStock = (retail || lovTrade) && mode == "STOCK"
    val governedTrade = lovTrade && mode in setOf("SALE", "PURCHASE", "SPEND", "STOCK", "ORDER", "COLLECTION", "PAYMENT", "SHIPMENT", "DELIVERY", "RETURN", "UPDATE", "ISSUE")
    val needsText = mode in setOf("UPDATE", "ISSUE", "STOCK", "ORDER", "SHIPMENT", "DELIVERY", "APPOINTMENT", "DELIVERABLE", "COMPLETE")
    if (needsText && !(production && mode == "ORDER") && !contractStock && !governedTrade && purpose.isBlank() && note.isBlank()) return null
    // Contract STOCK has no note field — item + quantity are enough (iOS retailDraft parity).
    if (contractStock && (purpose.isBlank() || quantity.isBlank())) return null
    if (lovTrade && mode == "SALE" && (channel.isBlank() || (choice != "Received" && party.isBlank()))) return null
    if (lovTrade && mode == "ORDER" && party.isBlank() && channel.isBlank()) return null
    if (lovTrade && mode == "SHIPMENT" && (parentId.isBlank() || quantity.isBlank())) return null
    if (lovTrade && mode in setOf("UPDATE", "ISSUE") && purpose.isBlank()) return null
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
    if (mode == "RETURN" && (parentId.isBlank() || returnReason.isBlank())) return null
    if (mode in setOf("PAYMENT", "REIMBURSE", "COLLECTION") && open.isNotEmpty() && parentId.isBlank() && !(client && mode == "COLLECTION" && choice == "Create Receivable")) return null
    val lineItemName = if (mode == "RETURN" && !entryMode.equals("Itemized", ignoreCase = true)) "" else itemName
    val savedTitle = when (mode) {
        "PERSON", "ROLE", "INVITE", "SUPPLIER", "PARTNER" -> party.trim()
        "UPDATE", "ISSUE", "ORDER", "SHIPMENT", "DELIVERY", "APPOINTMENT", "DELIVERABLE", "COMPLETE" ->
            when {
                production && mode == "ORDER" -> party.trim()
                lovTrade && mode == "ORDER" -> itemName.trim().ifBlank { party.trim() }.ifBlank { title }
                lovTrade && mode in setOf("UPDATE", "ISSUE", "SHIPMENT") -> purpose.trim().ifBlank { title }
                else -> purpose.trim().take(80)
            }
        "STOCK" -> purpose.trim().ifBlank { title }
        "PURCHASE", "VENDOR", "SPEND" -> itemName.trim().ifBlank { purpose.trim() }.ifBlank { title }
        "SALE", "SERVICE" -> itemName.trim().ifBlank { title }
        "RETURN" -> lineItemName.trim().ifBlank { title }
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
        val itemized = entryMode.equals("Itemized", ignoreCase = true) ||
            (itemName.isNotBlank() && quantity.isNotBlank() && unitPrice.isNotBlank())
        val salePay = when {
            choice.equals("Received", ignoreCase = true) || choice.equals("Paid", ignoreCase = true) -> "RECEIVED"
            choice.equals("Part paid", ignoreCase = true) -> "PART_PAID"
            choice.equals("Pay later", ignoreCase = true) || choice.equals("Pending", ignoreCase = true) -> "PAY_LATER"
            else -> "PAY_LATER"
        }
        val purchasePay = when {
            choice.equals("Paid", ignoreCase = true) -> "PAID"
            choice.equals("Part paid", ignoreCase = true) -> "PART_PAID"
            else -> "PAY_LATER"
        }
        val stockActionCode = when (stockActionLabel.lowercase()) {
            "set current stock", "set" -> "SET"
            "add" -> "ADD"
            "reduce" -> "REDUCE"
            else -> if (mode == "STOCK") "SET" else null
        }
        val stockReasonCode = when (stockReasonLabel.lowercase()) {
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
        val ownershipCode = when {
            mode !in setOf("PURCHASE", "STOCK") || !(trading || distribution) -> null
            ownership.equals("Consignment", ignoreCase = true) -> "CONSIGNMENT"
            else -> "OWNED"
        }
        val receiptCode = when {
            mode != "PURCHASE" -> null
            entryMode.equals("Quick amount", ignoreCase = true) && itemName.isBlank() -> "AWAITING"
            receiptState.equals("Part received", ignoreCase = true) || receiptState.equals("Partially received", ignoreCase = true) -> "PART_RECEIVED"
            receiptState.equals("Awaiting receipt", ignoreCase = true) || receiptState.equals("Receive later", ignoreCase = true) -> "AWAITING"
            else -> "RECEIVED"
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
        val paidOrReceived = when {
            mode in setOf("SALE", "SERVICE") && salePay == "PART_PAID" -> receivedAmount.ifBlank { null }
            mode in setOf("SALE", "SERVICE") && salePay == "RECEIVED" -> resolvedAmount.ifBlank { null }
            mode in setOf("PURCHASE", "SPEND") && purchasePay == "PART_PAID" -> receivedAmount.ifBlank { null }
            mode in setOf("PURCHASE", "SPEND") && purchasePay == "PAID" -> resolvedAmount.ifBlank { null }
            else -> null
        }
        val taxApplies = lovTrade && mode in setOf("SALE", "PURCHASE")
        val fxApplies = taxApplies && originalCurrency.isNotBlank()
        val taxCode = when (taxModeLabel.lowercase()) {
            "inclusive" -> "INCLUSIVE"
            "exclusive" -> "EXCLUSIVE"
            else -> "NOT_CONFIGURED"
        }
        com.example.momentra.data.api.OperatingContractBody(
            entryMode = if (mode in setOf("SALE", "SERVICE")) if (itemized) "ITEMIZED" else "QUICK" else if (mode == "PURCHASE") if (itemized) "ITEMIZED" else "QUICK" else null,
            paymentState = when (mode) {
                "SALE", "SERVICE" -> salePay
                "PURCHASE", "SPEND" -> purchasePay
                else -> null
            },
            receiptState = receiptCode,
            receivedAmount = paidOrReceived,
            quantityReceived = if (mode == "PURCHASE" && receiptCode == "PART_RECEIVED") quantityReceived.ifBlank { null } else null,
            stockAction = stockActionCode,
            stockReason = stockReasonCode,
            appointmentAction = appointmentAction,
            orderStatus = orderStatus,
            staffName = if (mode == "APPOINTMENT") detail.ifBlank { null } else null,
            estimatedAmount = if (mode in setOf("ORDER", "APPOINTMENT")) resolvedAmount.ifBlank { null } else null,
            allocations = allocations,
            ownership = ownershipCode,
            shipmentStatus = shipmentStatus,
            promisedAt = if (mode in setOf("ORDER", "SHIPMENT", "DELIVERY")) promised else null,
            declaredQuantity = if (mode == "STOCK" && stockActionCode == "SET") quantity.ifBlank { null } else null,
            expenseCategory = if (lovTrade && mode == "SPEND") expenseCategory.trim().ifBlank { null } else null,
            taxMode = if (taxApplies) taxCode else null,
            taxAmount = if (taxApplies && taxCode != "NOT_CONFIGURED") taxAmount.ifBlank { null } else null,
            originalCurrency = if (fxApplies) originalCurrency.trim().uppercase() else null,
            fxRate = if (fxApplies) fxRate.ifBlank { null } else null,
            returnDirection = if (mode == "RETURN") {
                if (returnDirectionLabel.startsWith("Supplier", ignoreCase = true)) "SUPPLIER" else "CUSTOMER"
            } else null,
            goodsOutcome = if (mode == "RETURN") {
                when (goodsOutcomeLabel.lowercase()) {
                    "not returned" -> "NOT_RETURNED"
                    "pending inspection" -> "PENDING_INSPECTION"
                    else -> "TO_STOCK"
                }
            } else null,
            returnReason = if (mode == "RETURN") returnReason.trim().take(80).ifBlank { null } else null,
            orderSide = if (lovTrade && mode == "ORDER") if (orderSideLabel.startsWith("Purchase")) "PURCHASE" else "SALES" else null,
            goodsStatus = if (lovTrade && mode in setOf("SALE", "SERVICE")) when (goodsStatusLabel) {
                "Fully delivered" -> "DELIVERED"
                "Partially delivered" -> "PARTIAL"
                else -> "LATER"
            } else null,
        )
    } else null
    val stockTitle = if (mode == "STOCK" && purpose.isNotBlank()) purpose else savedTitle
    return OperatingCaptureDraft(
        eventType = if (production && mode == "BUDGET" && choice == "Target") "TARGET" else eventType,
        title = if (mode == "STOCK") stockTitle.ifBlank { title } else savedTitle.ifBlank { title },
        amount = resolvedAmount.ifBlank { null },
        note = savedNote,
        channelName = channel.ifBlank { null },
        counterpartyName = if (production && mode == "PRODUCTION") null else party.ifBlank { null },
        paymentStatus = payment,
        occurredAt = occurredAt,
        quantity = quantity.ifBlank { null },
        unit = unit.ifBlank { null },
        unitPrice = unitPrice.ifBlank { null },
        itemName = lineItemName.ifBlank { null },
        parentEventId = if (mode == "ORDER" || mode == "APPOINTMENT") null else parentId.ifBlank { null },
        revisesEventId = if ((mode == "ORDER" || mode == "APPOINTMENT") && parentId.isNotBlank()) parentId else revisesEventId?.takeIf { it.isNotBlank() },
        masterKind = if (mode == "MASTERS") masterKind.ifBlank { null } else null,
        dueAt = dueAtDay?.takeIf { it.matches(Regex("""\d{4}-\d{2}-\d{2}.*""")) }
            ?: link.takeIf { it.matches(Regex("""\d{4}-\d{2}-\d{2}.*""")) },
        contract = retailContract,
        masterId = if (lineItemName.isNotBlank()) productMasterId?.takeIf { it.isNotBlank() } else null,
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

private val ExpenseCategorySeeds = listOf(
    "Rent", "Salaries", "Transport", "Freight", "Utilities", "Packaging", "Marketing",
    "Maintenance", "Office supplies", "Insurance", "Fees and charges", "Other",
).map { OperatingLovOption(id = "seed-expense-$it", name = it, kind = "EXPENSE_CATEGORY") }

private fun lovTitle(kind: String): String = when (kind) {
    "PRODUCT" -> "Product"
    "BUYER" -> "Buyer"
    "SUPPLIER" -> "Supplier"
    "CHANNEL" -> "Channel"
    "EXPENSE_CATEGORY" -> "Expense category"
    "UOM" -> "UOM"
    else -> kind.lowercase().replaceFirstChar { it.uppercase() }
}

private fun lovOptions(
    kind: String,
    masters: List<OperatingMasterDto>,
    channels: List<String>,
    local: List<OperatingLovOption>,
): List<OperatingLovOption> {
    val fromMasters = masters
        .filter { master ->
            val masterKind = master.kind.uppercase()
            masterKind == kind || (kind == "BUYER" && masterKind == "CUSTOMER")
        }
        .map { master ->
            OperatingLovOption(
                id = master.id,
                name = master.name,
                kind = kind,
                unit = master.unit ?: if (kind == "UOM") master.name else null,
                subtitle = master.unit?.takeIf { it.isNotBlank() && kind == "PRODUCT" }?.let { "Base unit: $it" },
            )
        }
    val seeds = when (kind) {
        "UOM" -> OperatingSeedUoms.all
        "CHANNEL" -> channels.map { OperatingLovOption(id = "seed-channel-$it", name = it, kind = "CHANNEL") }
        "EXPENSE_CATEGORY" -> ExpenseCategorySeeds
        else -> emptyList()
    }
    return (fromMasters + local.filter { it.kind == kind } + seeds)
        .distinctBy { it.name.trim().lowercase() }
}

@Composable
private fun LovField(label: String, value: String, placeholder: String, modifier: Modifier = Modifier.fillMaxWidth(), onClick: () -> Unit) {
    ReadField(label, value.ifBlank { "Select $placeholder  ›" }, modifier, onClick)
}

@Composable
private fun LovItemLine(
    itemName: String,
    quantity: String,
    unit: String,
    unitPrice: String,
    onPickProduct: () -> Unit,
    onPickUom: () -> Unit,
    onQuantity: (String) -> Unit,
    onPrice: (String) -> Unit,
    showPrice: Boolean = true,
) {
    LovField("Product", itemName, "product", onClick = onPickProduct)
    FieldRow {
        EditField("Qty", quantity, "1", Modifier.weight(1f)) { onQuantity(it) }
        LovField("UOM", unit, "UOM", Modifier.weight(1f), onPickUom)
        if (showPrice) EditField("Price", unitPrice, "0", Modifier.weight(1f)) { onPrice(it) }
    }
    if (showPrice) {
        Text("Quantity, unit, and price stay on the sale or purchase. Stock changes only when you record that the goods were received or dispatched.", color = SheetMuted, fontFamily = PlusJakartaSans, fontSize = 12.sp)
    }
}

@Composable
private fun TaxFxFields(
    currency: String,
    taxMode: String,
    taxAmount: String,
    originalCurrency: String,
    fxRate: String,
    onTaxMode: (String) -> Unit,
    onTaxAmount: (String) -> Unit,
    onOriginalCurrency: (String) -> Unit,
    onFxRate: (String) -> Unit,
) {
    ChoiceBlock("Tax", listOf("Not configured", "Inclusive", "Exclusive"), taxMode, onTaxMode)
    if (taxMode != "Not configured") {
        EditField("Tax amount", taxAmount, "0.00") { onTaxAmount(it.filter { c -> c.isDigit() || c == '.' }) }
    }
    FieldRow {
        EditField("Original currency", originalCurrency, "USD", Modifier.weight(1f)) {
            onOriginalCurrency(it.filter { c -> c.isLetter() }.take(3).uppercase())
        }
        EditField("FX rate", fxRate, if (originalCurrency.isBlank()) "Needs currency" else "Required", Modifier.weight(1f)) {
            onFxRate(it.filter { c -> c.isDigit() || c == '.' })
        }
    }
    Text(
        "Amounts are saved in $currency. An FX rate is only needed when an original currency is set.",
        color = SheetMuted,
        fontFamily = PlusJakartaSans,
        fontSize = 12.sp,
    )
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

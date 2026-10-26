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
    val masterGstApplicable: String? = null,
    val masterGstClassification: String? = null,
    val masterGstRate: String? = null,
    val masterPriceIncludesGst: String? = null,
    val masterSalePriceIncludesGst: String? = null,
    val masterPurchasePriceIncludesGst: String? = null,
    val masterGstEffectiveFrom: String? = null,
    val masterHsn: String? = null,
    val masterGstRegistration: String? = null,
    val masterBusinessState: String? = null,
    val masterBillingState: String? = null,
    val masterDeliveryState: String? = null,
    val masterGstin: String? = null,
    val reportingMode: String? = null,
    val metricCode: String? = null,
    val budgetCategory: String? = null,
    val activityStatus: String? = null,
    val attributedSales: String? = null,
    val revisionKind: String? = null,
    val costBasis: String? = null,
    val overlapDeduped: Boolean? = null,
    val creativeVersion: String? = null,
    val resultSource: String? = null,
    val idempotencyKey: String? = null,
    val effectiveFrom: String? = null,
    val periodStart: String? = null,
    val periodEnd: String? = null,
    val supersedesEventId: String? = null,
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

private class CaptureForm(
    channel: String,
    choice: String,
    unit: String,
    masterKind: String,
    receiptState: String,
    detail: String,
) {
    var eventDate by mutableStateOf(LocalDate.now())
    var dateOpen by mutableStateOf(false)
    var dueDate by mutableStateOf(LocalDate.now())
    var dueDateOpen by mutableStateOf(false)
    var goodsDueDate by mutableStateOf(LocalDate.now())
    var goodsDueOpen by mutableStateOf(false)
    var amount by mutableStateOf("")
    var channel by mutableStateOf(channel)
    var party by mutableStateOf("")
    var purpose by mutableStateOf("")
    var note by mutableStateOf("")
    var choice by mutableStateOf(choice)
    var moreOpen by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    var link by mutableStateOf("")
    var itemName by mutableStateOf("")
    var quantity by mutableStateOf("")
    var quantityReceived by mutableStateOf("")
    var unit by mutableStateOf(unit)
    var unitPrice by mutableStateOf("")
    var parentId by mutableStateOf("")
    var masterKind by mutableStateOf(masterKind)
    var entryMode by mutableStateOf("Quick amount")
    var receiptState by mutableStateOf(receiptState)
    var ownership by mutableStateOf("Owned")
    var stockAction by mutableStateOf("Set current stock")
    var stockReason by mutableStateOf("Stocktake")
    var receivedAmount by mutableStateOf("")
    var productMasterId by mutableStateOf<String?>(null)
    var lovKind by mutableStateOf<String?>(null)
    var localMasters by mutableStateOf<List<OperatingLovOption>>(emptyList())
    var taxMode by mutableStateOf("Not configured")
    var taxAmount by mutableStateOf("")
    var gstRate by mutableStateOf("")
    var supplyChoice by mutableStateOf("")
    var gstDetailsOpen by mutableStateOf(false)
    var gstApplicable by mutableStateOf("Not sure")
    var gstClassification by mutableStateOf("Taxable")
    var hsn by mutableStateOf("")
    var masterPriceIncludes by mutableStateOf("Use business default")
    var masterSaleIncludes by mutableStateOf("Use business default")
    var masterPurchaseIncludes by mutableStateOf("Use business default")
    var gstEffectiveFrom by mutableStateOf("")
    var partyGstRegistration by mutableStateOf("Not sure")
    var businessState by mutableStateOf("")
    var billingState by mutableStateOf("")
    var deliveryState by mutableStateOf("")
    var partyGstin by mutableStateOf("")
    var reportingMode by mutableStateOf("Incremental")
    var metricCode by mutableStateOf("LEADS")
    var activityStatus by mutableStateOf("IN_PROGRESS")
    var costBasis by mutableStateOf("Agency borne")
    var overlapDeduped by mutableStateOf(false)
    var creativeVersion by mutableStateOf("1")
    var resultSource by mutableStateOf("")
    var periodStart by mutableStateOf("")
    var periodEnd by mutableStateOf("")
    var supersedesId by mutableStateOf("")
    var idempotencyKey by mutableStateOf(java.util.UUID.randomUUID().toString())
    var originalCurrency by mutableStateOf("")
    var fxRate by mutableStateOf("")
    var returnDirection by mutableStateOf("Customer return")
    var goodsOutcome by mutableStateOf("To stock")
    var goodsStatusLabel by mutableStateOf("Deliver later")
    var orderSideLabel by mutableStateOf("Sales order")
    var movementLabel by mutableStateOf("Outgoing sale")
    var returnReason by mutableStateOf("")
    var expenseCategory by mutableStateOf("")
    var detail by mutableStateOf(detail)

    val dateText: String get() = eventDate.format(dayFormat)
    val dueDateText: String get() = dueDate.format(dayFormat)
    val goodsDueText: String get() = goodsDueDate.format(dayFormat)

    fun applySeed(row: OperatingActivityDto) {
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

    fun applyLov(kind: String, option: OperatingLovOption, mode: String, model: OperatingProjectionDto) {
        when (kind) {
            "PRODUCT" -> {
                itemName = option.name
                if (mode == "STOCK") purpose = option.name
                productMasterId = option.id.takeUnless { it.startsWith("local-") || it.startsWith("seed-") }
                option.unit?.takeIf { it.isNotBlank() }?.let { unit = it }
                model.masters.orEmpty().firstOrNull { it.id == option.id }?.let { master ->
                    displayGstRate(master.gstRate)?.let { gstRate = it }
                    when (master.priceIncludesGst) {
                        "YES" -> taxMode = "Inclusive"
                        "NO" -> taxMode = "Exclusive"
                    }
                }
            }
            "BUYER", "CUSTOMER", "SUPPLIER" -> {
                party = option.name
                applyPartySupply(option.name, model)
            }
            "CHANNEL" -> channel = option.name
            "EXPENSE_CATEGORY" -> expenseCategory = option.name
            "UOM" -> unit = option.name
            "SALES_ORDER", "PURCHASE_ORDER" -> {
                parentId = option.id
                model.activity.firstOrNull { it.id == option.id }?.let { row ->
                    row.counterpartyName?.takeIf { it.isNotBlank() }?.let { party = it }
                    row.channelName?.takeIf { it.isNotBlank() }?.let { channel = it }
                    row.quantity?.takeIf { it.isNotBlank() }?.let { quantity = it }
                    row.unit?.takeIf { it.isNotBlank() }?.let { unit = it }
                    if (itemName.isBlank()) itemName = row.title
                }
            }
            "OBLIGATION" -> {
                parentId = option.id
                option.detailAmount?.takeIf { it.isNotBlank() }?.let { amount = it }
                model.paymentReadiness.firstOrNull { it.id == option.id }?.counterpartyName
                    ?.takeIf { it.isNotBlank() }
                    ?.let { party = it }
            }
        }
    }

    private fun applyPartySupply(name: String, model: OperatingProjectionDto) {
        val party = model.masters.orEmpty().firstOrNull {
            it.name.equals(name, ignoreCase = true) && it.kind.uppercase() in setOf("CUSTOMER", "BUYER", "SUPPLIER")
        } ?: return
        val business = model.gstState.orEmpty().trim()
        val supply = listOf(party.deliveryState, party.billingState, party.businessState)
            .firstOrNull { !it.isNullOrBlank() }
            .orEmpty()
            .trim()
        if (business.isBlank() || supply.isBlank()) return
        supplyChoice = if (supply.equals(business, ignoreCase = true)) "INTRA" else "INTER"
    }
}

private fun displayGstRate(raw: String?): String? {
    val value = raw?.trim().orEmpty()
    if (value.isEmpty()) return null
    return value.toBigDecimalOrNull()?.stripTrailingZeros()?.toPlainString() ?: value
}

private fun initialCaptureChoice(mode: String, variant: String): String = when {
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
}

private fun initialCaptureDetail(mode: String, variant: String): String =
    if ((variant == "client" || variant == "own") && mode == "DELIVERABLE") "Due" else ""

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
    val actor = contextLine(model)
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
    val contractFamily = restaurant || service || trading || distribution || model.subtype in setOf(
        "RETAIL_STORE", "RESTAURANT_FOOD", "CONSUMER_SERVICE", "TRADING_WHOLESALE", "DISTRIBUTION_DEALERSHIP",
    )
    val lovTrade = trading || distribution || model.subtype in setOf("TRADING_WHOLESALE", "DISTRIBUTION_DEALERSHIP")
    val form = remember {
        CaptureForm(
            channel = model.life?.channels?.firstOrNull().orEmpty(),
            choice = initialCaptureChoice(mode, variant),
            unit = if (variant == "trading" || variant == "distribution" || model.subtype in setOf("TRADING_WHOLESALE", "DISTRIBUTION_DEALERSHIP")) "pc" else "piece",
            masterKind = masterKinds(model.subtype).first(),
            receiptState = if (variant == "trading" || variant == "distribution") "Fully received" else "Received",
            detail = initialCaptureDetail(mode, variant),
        )
    }
    LaunchedEffect(mode, variant) {
        form.choice = initialCaptureChoice(mode, variant)
        form.detail = initialCaptureDetail(mode, variant)
        if (lovTrade && model.gstTracking == "YES") {
            val includes = if (mode == "PURCHASE") model.purchasePriceIncludesGst else model.salePriceIncludesGst
            if (includes == "YES") form.taxMode = "Inclusive"
            if (includes == "NO") form.taxMode = "Exclusive"
        }
    }
    LaunchedEffect(seed?.id, seed?.parentEventId) {
        val row = seed ?: return@LaunchedEffect
        form.applySeed(row)
    }
    val scope = rememberCoroutineScope()

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
                "SALE", "SERVICE" -> SaleServiceCapture(form, mode, model, currency, channels, channelLabel, actor, production, contractFamily, lovTrade, trading, distribution, marketing)
                "PURCHASE", "VENDOR" -> PurchaseVendorCapture(form, mode, model, currency, production, contractFamily, lovTrade, trading, distribution, marketing)
                "SPEND" -> SpendCapture(form, model, currency, channels, channelLabel, actor, production, lovTrade, marketing, restaurant, service)
                "PAYMENT", "REIMBURSE" -> PaymentReimburseCapture(form, mode, model, currency, lovTrade, trading, distribution, marketing, restaurant, service)
                "COLLECTION" -> CollectionCapture(form, mode, model, currency, channels, actor, production, lovTrade, distribution, client, own)
                "BUDGET", "TARGET" -> BudgetTargetCapture(form, mode, model, currency, production, lovTrade, marketing, client, own)
                "ORDER" -> OrderCapture(form, model, currency, channels, actor, production, lovTrade, trading, distribution, restaurant)
                "SHIPMENT" -> ShipmentCapture(form, model, currency, actor, lovTrade, trading, distribution)
                "APPOINTMENT" -> AppointmentCapture(form, model, actor, service, seed)
                "UPDATE", "DELIVERY", "DELIVERABLE", "COMPLETE", "RESULT", "PRODUCTION" -> ActivityCapture(form, mode, model, currency, channels, channelLabel, actor, production, lovTrade, trading, distribution, marketing, restaurant, service, eventType)
                "ISSUE" -> IssueCapture(form, actor, production, trading, distribution, marketing, restaurant, service)
                "STOCK" -> StockCapture(form, actor, production, contractFamily, lovTrade, trading, distribution, restaurant, service)
                "RETURN" -> ReturnCapture(form, model, currency)
                "PERSON", "ROLE", "INVITE" -> PersonCapture(form, production, trading, distribution, marketing, restaurant, service)
                "SUPPLIER", "PARTNER" -> SupplierPartnerCapture(form, mode, currency, channels, production, trading, distribution, marketing, restaurant, service, client, own)
                "MASTERS" -> MastersCapture(form, model)
                "APPROVAL" -> ApprovalCapture(form, model, currency, marketing, onDecide)
                else -> FallbackNoteCapture(form)
            }
            if (form.error != null) Text(form.error ?: "", color = Color(0xFFF87171), fontFamily = PlusJakartaSans, fontSize = 12.sp)
        }
        CaptureSheetFooter(form, mode, eventType, title, model, restaurant, service, trading, distribution, marketing, client, own, production, job, standard, lovTrade, seed, onDecide, onDismiss, onSubmit, scope)
    }
    CaptureDateDialogs(form)
    CaptureLovHost(form, model, mode, channels, distribution, onCreateMaster, scope)
}

@Composable
private fun SaleServiceCapture(
    form: CaptureForm,
    mode: String,
    model: OperatingProjectionDto,
    currency: String,
    channels: List<String>,
    channelLabel: String,
    actor: String,
    production: Boolean,
    contractFamily: Boolean,
    lovTrade: Boolean,
    trading: Boolean,
    distribution: Boolean,
    marketing: Boolean = false,
) {
    val typed = form.amount.toBigDecimalOrNull()
    if (marketing && mode == "SALE") {
        androidx.compose.runtime.LaunchedEffect(model.partyName) {
            form.party = model.partyName.orEmpty()
        }
        ReadField("Client", model.partyName.orEmpty().ifBlank { "Set the client in setup" })
        AmountBlock("Invoice amount", currency, form.amount, "Positive amount") { form.amount = it }
        ReadField("Invoice date", form.dateText) { form.dateOpen = true }
        Text("This invoice uses the campaign client.", color = SheetMuted, fontFamily = PlusJakartaSans, fontSize = 12.sp)
    } else if (production && mode == "SALE") {
        AmountBlock("Amount", currency, form.amount, "Quick amount posts revenue and no SKU quantity") { form.amount = it }
        if (channels.isNotEmpty()) ChoiceBlock("Buyer / channel", channels, form.channel) { form.channel = it }
        else EditField("Buyer", form.party, "Required when a balance remains") { form.party = it }
        ReadField("Sale date", form.dateText) { form.dateOpen = true }
        ChoiceBlock("Payment state", listOf("Received", "Part paid", "Pay later"), form.choice) { form.choice = it }
        if (form.choice == "Part paid") EditField("Received now", form.receivedAmount, "Required") { form.receivedAmount = it }
        GstPriceFields(form, model, purchase = false)
        model.activity.filter { it.eventType == "CUSTOMER_ORDER" }.forEach { row ->
            TextButton(onClick = { form.parentId = row.id }) { Text(if (form.parentId == row.id) "Order ${row.title}" else row.title, color = Color.White) }
        }
        Text("A sale is not a delivery.", color = SheetMuted, fontFamily = PlusJakartaSans, fontSize = 12.sp)
    } else if (contractFamily) {
        if (lovTrade) {
            val linked = model.activity.firstOrNull { it.id == form.parentId }?.title.orEmpty()
            LovField("Sales order", linked, "None · optional") { form.lovKind = "SALES_ORDER" }
        }
        ChoiceBlock("Entry", listOf("Quick amount", "Itemized"), form.entryMode) { form.entryMode = it }
        if (form.entryMode == "Itemized") {
            if (lovTrade) {
                LovItemLine(form.itemName, form.quantity, form.unit, form.unitPrice, { form.lovKind = "PRODUCT" }, { form.lovKind = "UOM" }, { form.quantity = it }, { form.unitPrice = it })
            } else {
                ItemLine(form.itemName, form.quantity, form.unit, form.unitPrice, { form.itemName = it }, { form.quantity = it }, { form.unit = it }, { form.unitPrice = it }, model.masters.orEmpty())
            }
        } else {
            AmountBlock("Sale amount", currency, form.amount, "Positive amount · 2 decimal places") { form.amount = it }
        }
        GstPriceFields(form, model, purchase = false)
        if (lovTrade) LovField(channelLabel, form.channel, "channel") { form.lovKind = "CHANNEL" }
        else if (channels.isNotEmpty()) ChoiceBlock(channelLabel, channels, form.channel) { form.channel = it }
        else EditField(channelLabel, form.channel, "Required when channel tracking is on") { form.channel = it }
        FieldRow {
            ReadField("Sale date", form.dateText, Modifier.weight(1f)) { form.dateOpen = true }
            ReadField("Receipt actor", actor.substringBefore(" ·"), Modifier.weight(1f))
        }
        ChoiceBlock("Payment state", listOf("Received", "Part paid", "Pay later"), form.choice) { form.choice = it }
        if (form.choice == "Part paid") EditField("Received amount", form.receivedAmount, "Less than the total") { form.receivedAmount = it }
        if (form.choice != "Received") {
            if (lovTrade) LovField(if (distribution) "Buyer / dealer" else "Buyer", form.party, "buyer") { form.lovKind = "BUYER" }
            else EditField("Customer", form.party, "Required when a balance remains") { form.party = it }
            ReadField(if (lovTrade) "Payment due date" else "Due date", form.dueDateText) { form.dueDateOpen = true }
        }
        if (lovTrade) {
            ChoiceBlock("Goods", listOf("Fully delivered", "Partially delivered", "Deliver later"), form.goodsStatusLabel) { form.goodsStatusLabel = it }
            if (form.goodsStatusLabel == "Partially delivered") {
                EditField("Delivered quantity", form.quantity, "Does not change stock") { form.quantity = it }
            }
            if (form.goodsStatusLabel != "Fully delivered") {
                ReadField("Delivery due date", form.goodsDueText) { form.goodsDueOpen = true }
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
        MoreDetails(form.moreOpen, if (lovTrade) "Tax, FX, note, and reference. Optional." else "Note and reference. Optional.") { form.moreOpen = !form.moreOpen }
        if (form.moreOpen) {
            if (lovTrade) {
                TaxFxFields(currency, form.taxMode, form.taxAmount, form.originalCurrency, form.fxRate, { form.taxMode = it }, { form.taxAmount = it }, { form.originalCurrency = it }, { form.fxRate = it }, hideTax = model.gstTracking == "YES")
            }
            NoteField("Note", form.note, "Optional") { form.note = it }
        }
        SummaryCard("After save", saleSummary(model, currency, typed, form.choice == "Received"))
    } else {
        ItemLine(form.itemName, form.quantity, form.unit, form.unitPrice, { form.itemName = it }, { form.quantity = it }, { form.unit = it }, { form.unitPrice = it }, model.masters.orEmpty())
        AmountBlock("Amount", currency, form.amount, "Positive amount · 2 decimal places") { form.amount = it }
        if (channels.isNotEmpty()) ChoiceBlock(channelLabel, channels, form.channel) { form.channel = it }
        FieldRow {
            ReadField("Date", form.dateText, Modifier.weight(1f)) { form.dateOpen = true }
            ReadField("Recorded by", actor.substringBefore(" ·"), Modifier.weight(1f))
        }
        ChoiceBlock("Money received", listOf("Received", "Pending"), form.choice) { form.choice = it }
        Text("One sale and its receipt. A collection is a separate event.", color = SheetMuted, fontFamily = PlusJakartaSans, fontSize = 12.sp)
        MoreDetails(form.moreOpen, "Note, reference and evidence. Optional.") { form.moreOpen = !form.moreOpen }
        if (form.moreOpen) NoteField("Note", form.note, "Optional") { form.note = it }
        SummaryCard("After save", saleSummary(model, currency, typed, form.choice == "Received"))
    }
}

@Composable
private fun PurchaseVendorCapture(
    form: CaptureForm,
    mode: String,
    model: OperatingProjectionDto,
    currency: String,
    production: Boolean,
    contractFamily: Boolean,
    lovTrade: Boolean,
    trading: Boolean,
    distribution: Boolean,
    marketing: Boolean,
) {
    val typed = form.amount.toBigDecimalOrNull()
    if (production && mode == "PURCHASE") {
        AmountBlock("Amount", currency, form.amount, "Counted once toward budget and payables") { form.amount = it }
        GstPriceFields(form, model, purchase = true)
        FieldRow {
            EditField("Supplier", form.party, "Required", Modifier.weight(1f)) { form.party = it }
            ReadField("Purchase date", form.dateText, Modifier.weight(1f)) { form.dateOpen = true }
        }
        EditField("Material", form.itemName, "Optional item") { form.itemName = it }
        FieldRow {
            EditField("Qty", form.quantity, "Qty", Modifier.weight(1f)) { form.quantity = it }
            EditField("Unit", form.unit, "kg", Modifier.weight(1f)) { form.unit = it }
        }
        ChoiceBlock("Materials received", listOf("Received", "Part received", "Awaiting"), form.receiptState) { form.receiptState = it }
        if (form.receiptState == "Part received") EditField("Quantity received", form.quantityReceived, "Required") { form.quantityReceived = it }
        ChoiceBlock("Payment state", listOf("Paid", "Part paid", "Pay later"), form.choice) { form.choice = it }
        Text("Stock increases only for the quantity you mark as received.", color = SheetMuted, fontFamily = PlusJakartaSans, fontSize = 12.sp)
    } else if (marketing && mode == "VENDOR") {
        ItemLine(form.itemName, form.quantity, form.unit, form.unitPrice, { form.itemName = it }, { form.quantity = it }, { form.unit = it }, { form.unitPrice = it }, model.masters.orEmpty())
        AmountBlock("Amount", currency, form.amount, "Positive amount · 2 decimal places") { form.amount = it }
        EditField("Counterparty", form.party, "Required") { form.party = it }
        EditField("Purpose", form.purpose, "What this covers") { form.purpose = it }
        ChoiceBlock("Paid / pending", listOf("Pending / unpaid", "Paid"), form.choice) { form.choice = it }
        Text("Pending approval. Submission is not payment.", color = SheetMuted, fontFamily = PlusJakartaSans, fontSize = 12.sp)
        SummaryCard("After approval", purchaseSummary(model, currency, typed, form.choice.startsWith("Pending")))
    } else if (contractFamily) {
        if (lovTrade) {
            val linked = model.activity.firstOrNull { it.id == form.parentId }?.title.orEmpty()
            LovField("Purchase order", linked, "None · optional") { form.lovKind = "PURCHASE_ORDER" }
            LovItemLine(form.itemName, form.quantity, form.unit, form.unitPrice, { form.lovKind = "PRODUCT" }, { form.lovKind = "UOM" }, { form.quantity = it }, { form.unitPrice = it })
        } else {
            ItemLine(form.itemName, form.quantity, form.unit, form.unitPrice, { form.itemName = it }, { form.quantity = it }, { form.unit = it }, { form.unitPrice = it }, model.masters.orEmpty())
        }
        AmountBlock("Amount", currency, form.amount, "Positive amount, or leave blank when items have prices") { form.amount = it }
        GstPriceFields(form, model, purchase = true)
        if (lovTrade) LovField(if (distribution) "Principal / supplier" else "Supplier", form.party, "supplier") { form.lovKind = "SUPPLIER" }
        else EditField(if (distribution) "Principal / supplier" else "Supplier", form.party, "Required") { form.party = it }
        ReadField("Purchase date", form.dateText) { form.dateOpen = true }
        if (lovTrade) {
            ChoiceBlock("Goods receipt", listOf("Fully received", "Partially received", "Receive later"), form.receiptState) { form.receiptState = it }
            if (form.receiptState != "Fully received") {
                ReadField("Receipt due date", form.goodsDueText) { form.goodsDueOpen = true }
            }
        } else {
            ChoiceBlock("Goods receipt", listOf("Received", "Part received", "Awaiting receipt"), form.receiptState) { form.receiptState = it }
        }
        if (form.receiptState == "Part received" || form.receiptState == "Partially received") {
            EditField("Quantity received", form.quantityReceived, "Less than ordered qty") { form.quantityReceived = it }
        }
        if (trading || distribution) {
            ChoiceBlock("Ownership", listOf("Owned", "Consignment"), form.ownership) { form.ownership = it }
            Text("Consignment is not owned stock.", color = SheetMuted, fontFamily = PlusJakartaSans, fontSize = 12.sp)
        }
        ChoiceBlock("Payment state", listOf("Paid", "Part paid", "Pay later"), form.choice) { form.choice = it }
        if (form.choice == "Part paid") EditField("Amount paid", form.receivedAmount, "Less than the total") { form.receivedAmount = it }
        if (form.choice != "Paid") ReadField(if (lovTrade) "Payment due date" else "Due date", form.dueDateText) { form.dueDateOpen = true }
        Text("Purchase obligation is not payment. Stock moves only for received quantity.", color = SheetMuted, fontFamily = PlusJakartaSans, fontSize = 12.sp)
        MoreDetails(form.moreOpen, if (lovTrade) "Tax, FX, and note. Optional." else "Note. Optional.") { form.moreOpen = !form.moreOpen }
        if (form.moreOpen) {
            if (lovTrade) {
                TaxFxFields(currency, form.taxMode, form.taxAmount, form.originalCurrency, form.fxRate, { form.taxMode = it }, { form.taxAmount = it }, { form.originalCurrency = it }, { form.fxRate = it }, hideTax = model.gstTracking == "YES")
            }
            NoteField("Note", form.note, "Optional") { form.note = it }
        }
        SummaryCard("After save", purchaseSummary(model, currency, typed, form.choice != "Paid"))
    } else {
        ItemLine(form.itemName, form.quantity, form.unit, form.unitPrice, { form.itemName = it }, { form.quantity = it }, { form.unit = it }, { form.unitPrice = it }, model.masters.orEmpty())
        AmountBlock("Amount", currency, form.amount, "Positive amount · 2 decimal places") { form.amount = it }
        FieldRow {
            EditField("Supplier · Optional", form.party, "Name", Modifier.weight(1f)) { form.party = it }
            ReadField("Date", form.dateText, Modifier.weight(1f)) { form.dateOpen = true }
        }
        EditField("Purpose", form.purpose, "What was bought") { form.purpose = it }
        ChoiceBlock("Payment state", listOf("Pending / unpaid", "Paid"), form.choice) { form.choice = it }
        Text("Pending approval. Submission is not payment.", color = SheetMuted, fontFamily = PlusJakartaSans, fontSize = 12.sp)
        MoreDetails(form.moreOpen, "Due date, reference and evidence. Optional.") { form.moreOpen = !form.moreOpen }
        if (form.moreOpen) NoteField("Note", form.note, "Optional") { form.note = it }
        SummaryCard("After approval", purchaseSummary(model, currency, typed, form.choice.startsWith("Pending")))
    }
}

@Composable
private fun SpendCapture(
    form: CaptureForm,
    model: OperatingProjectionDto,
    currency: String,
    channels: List<String>,
    channelLabel: String,
    actor: String,
    production: Boolean,
    lovTrade: Boolean,
    marketing: Boolean,
    restaurant: Boolean,
    service: Boolean,
) {
    val typed = form.amount.toBigDecimalOrNull()
    if (production) {
        AmountBlock("Amount", currency, form.amount, "Positive amount · 2 decimal places") { form.amount = it }
        FieldRow {
            EditField("Purpose", form.purpose, "What this was for", Modifier.weight(1f)) { form.purpose = it }
            ReadField("Expense date", form.dateText, Modifier.weight(1f)) { form.dateOpen = true }
        }
        ChoiceBlock("Funding source", listOf("Business", "Personal"), form.choice) { form.choice = it }
        EditField("Paid by", form.party, actor.substringBefore(" ·")) { form.party = it }
        ChoiceBlock("Claim after approval?", listOf("Yes · Reimburse", "No"), form.detail) { form.detail = it }
        Text(
            "A personal claim is reimbursed later. It is not a second expense.",
            color = SheetMuted,
            fontFamily = PlusJakartaSans,
            fontSize = 12.sp,
        )
        MoreDetails(form.moreOpen, "Reference and note. Optional.") { form.moreOpen = !form.moreOpen }
        if (form.moreOpen) NoteField("Note", form.note, "Optional") { form.note = it }
        SummaryCard("After approval", spendSummary(model, currency, typed))
    } else if (marketing) {
        AmountBlock("Amount", currency, form.amount, "Positive amount · 2 decimal places") { form.amount = it }
        EditField("Purpose / Category", form.purpose, "What this was for") { form.purpose = it }
        EditField("Paid by", form.party, actor.substringBefore(" ·")) { form.party = it }
        if (channels.isNotEmpty()) ChoiceBlock(channelLabel, channels, form.channel) { form.channel = it }
        ChoiceBlock("Paid / pending", listOf("Pending / unpaid", "Paid"), form.choice) { form.choice = it }
        if (model.subtype == "CLIENT_CAMPAIGN") {
            ChoiceBlock("Who bears the cost", listOf("Agency borne", "Client reimbursable"), form.costBasis) { form.costBasis = it }
        }
        SummaryCard("After approval", spendSummary(model, currency, typed))
    } else if (lovTrade) {
        AmountBlock("Amount", currency, form.amount, "Positive amount · 2 decimal places") { form.amount = it }
        LovField("Expense category", form.expenseCategory, "category") { form.lovKind = "EXPENSE_CATEGORY" }
        FieldRow {
            EditField("Purpose", form.purpose, "What this was for", Modifier.weight(1f)) { form.purpose = it }
            ReadField("Expense date", form.dateText, Modifier.weight(1f)) { form.dateOpen = true }
        }
        ChoiceBlock("Payment state", listOf("Paid", "Part paid", "Pay later"), form.choice) { form.choice = it }
        if (form.choice != "Paid") {
            EditField("Payee", form.party, "Required") { form.party = it }
            ReadField("Due date", form.dueDateText) { form.dueDateOpen = true }
        }
        if (form.choice == "Part paid") EditField("Amount paid", form.receivedAmount, "Less than the total") { form.receivedAmount = it }
        MoreDetails(form.moreOpen, "Note. Optional.") { form.moreOpen = !form.moreOpen }
        if (form.moreOpen) NoteField("Note", form.note, "Optional") { form.note = it }
        SummaryCard("After save", spendSummary(model, currency, typed))
    } else {
        AmountBlock("Amount", currency, form.amount, "Positive amount · 2 decimal places") { form.amount = it }
        FieldRow {
            EditField("Purpose", form.purpose, "What this was for", Modifier.weight(1f)) { form.purpose = it }
            ReadField(if (restaurant || service) "Expense date" else "Date", form.dateText, Modifier.weight(1f)) { form.dateOpen = true }
        }
        ChoiceBlock("Funding source", listOf("Business", "Personal"), form.choice) { form.choice = it }
        EditField("Paid by", form.party, actor.substringBefore(" ·")) { form.party = it }
        if (service) {
            Text(
                "A personal claim is reimbursed later. It is not a second expense.",
                color = SheetMuted,
                fontFamily = PlusJakartaSans,
                fontSize = 12.sp,
            )
        }
        MoreDetails(form.moreOpen, "Reference and note. Optional.") { form.moreOpen = !form.moreOpen }
        if (form.moreOpen) NoteField("Note", form.note, "Optional") { form.note = it }
        SummaryCard("After approval", spendSummary(model, currency, typed))
    }
}

@Composable
private fun PaymentReimburseCapture(
    form: CaptureForm,
    mode: String,
    model: OperatingProjectionDto,
    currency: String,
    lovTrade: Boolean,
    trading: Boolean,
    distribution: Boolean,
    marketing: Boolean,
    restaurant: Boolean,
    service: Boolean,
) {
    if (marketing && mode == "PAYMENT") {
        OpenRecords(
            mode,
            model.paymentReadiness,
            form.parentId,
            title = "Open payables",
            empty = "Record a spend before paying a vendor.",
        ) { row ->
            form.parentId = row.id
            if (form.amount.isBlank()) row.amount?.let { form.amount = it }
            row.counterpartyName?.takeIf { it.isNotBlank() }?.let { form.party = it }
        }
        AmountBlock("Payment amount", currency, form.amount, "Positive amount · 2 decimal places") { form.amount = it }
        EditField("Who paid", form.detail, "Payer") { form.detail = it }
        EditField("Paid to", form.party, "Required") { form.party = it }
        EditField("Purpose / reference", form.purpose, "Required") { form.purpose = it }
        ReadField("Date", form.dateText) { form.dateOpen = true }
        Text("Manual confirmation only. This is not a live transfer.", color = SheetMuted, fontFamily = PlusJakartaSans, fontSize = 12.sp)
    } else {
        if (lovTrade && mode == "PAYMENT") {
            val selected = model.paymentReadiness.firstOrNull { it.id == form.parentId }?.title.orEmpty()
            LovField("Unpaid purchase or expense", selected, if (model.paymentReadiness.none { it.relevance == "PAYABLE" }) "Nothing is open" else "Search open records") {
                form.lovKind = "OBLIGATION"
            }
        } else {
            OpenRecords(mode, model.paymentReadiness, form.parentId) { row ->
                form.parentId = row.id
                row.counterpartyName?.takeIf { it.isNotBlank() }?.let { form.party = it }
            }
        }
        AmountBlock(if (mode == "REIMBURSE") "Reimbursement amount" else "Amount", currency, form.amount, "Positive amount · 2 decimal places") { form.amount = it }
        EditField(
            when {
                (service || trading || distribution) && mode == "REIMBURSE" -> "Business payer"
                mode == "REIMBURSE" -> "Payee"
                else -> "Paid to"
            },
            form.party,
            "Name",
        ) { form.party = it }
        ReadField(if ((restaurant || service || trading || distribution) && mode == "REIMBURSE") "Recorded date" else "Date", form.dateText) { form.dateOpen = true }
        NoteField("Reference · Optional", form.note, "Optional") { form.note = it }
        SummaryCard(
            "After save",
            listOf("Business paid moves by the amount you enter.", "This settles an existing claim. It is not a second expense."),
        )
    }
}

@Composable
private fun CollectionCapture(
    form: CaptureForm,
    mode: String,
    model: OperatingProjectionDto,
    currency: String,
    channels: List<String>,
    actor: String,
    production: Boolean,
    lovTrade: Boolean,
    distribution: Boolean,
    client: Boolean,
    own: Boolean,
) {
    val typed = form.amount.toBigDecimalOrNull()
    if (production) {
        OpenRecords(mode, model.paymentReadiness, form.parentId) { row ->
            form.parentId = row.id
            row.counterpartyName?.takeIf { it.isNotBlank() }?.let { form.party = it }
        }
        if (channels.isNotEmpty()) {
            ChoiceBlock("Received from", channels, form.channel) { form.channel = it }
        } else {
            EditField("Received from", form.party, "Required") { form.party = it }
        }
        AmountBlock("Amount received", currency, form.amount, "Positive amount") { form.amount = it }
        FieldRow {
            ReadField("Received by", actor.substringBefore(" ·"), Modifier.weight(1f))
            ReadField("Date", form.dateText, Modifier.weight(1f)) { form.dateOpen = true }
        }
        NoteField("Note", form.note, "Optional") { form.note = it }
        Text("Records the receipt. This is not a live transfer.", color = SheetMuted, fontFamily = PlusJakartaSans, fontSize = 12.sp)
        SummaryCard("After mark received", collectionSummary(model, currency, typed))
    } else if (client) {
        OpenRecords(
            mode,
            model.paymentReadiness,
            form.parentId,
            title = "Open client invoices",
            empty = "Record an invoice before collecting.",
        ) { row ->
            form.parentId = row.id
            if (form.amount.isBlank()) row.amount?.let { form.amount = it }
            row.counterpartyName?.takeIf { it.isNotBlank() }?.let { form.party = it }
        }
        ChoiceBlock("Collection path", listOf("Mark Received / Collect", "Create Receivable"), form.choice) { form.choice = it }
        Text(
            "Mark Received records a receipt. Create Receivable records the obligation.",
            color = SheetMuted,
            fontFamily = PlusJakartaSans,
            fontSize = 12.sp,
        )
        AmountBlock("Amount received", currency, form.amount, "Positive amount") { form.amount = it }
        EditField("Received from", form.party, "Required") { form.party = it }
        FieldRow {
            ReadField("Received by", actor.substringBefore(" ·"), Modifier.weight(1f))
            ReadField("Date", form.dateText, Modifier.weight(1f)) { form.dateOpen = true }
        }
        EditField("Purpose / reference", form.purpose, "Required") { form.purpose = it }
        SummaryCard("After mark received", collectionSummary(model, currency, typed))
    } else if (own) {
        OpenRecords(mode, model.paymentReadiness, form.parentId) { row ->
            form.parentId = row.id
            row.counterpartyName?.takeIf { it.isNotBlank() }?.let { form.party = it }
        }
        ChoiceBlock("Received from", listOf("Linked campaign receipt", "Other campaign receipt"), form.choice) { form.choice = it }
        Text(
            "Campaign-linked receipt only. No client billing or receivable.",
            color = SheetMuted,
            fontFamily = PlusJakartaSans,
            fontSize = 12.sp,
        )
        AmountBlock("Amount received", currency, form.amount, "Positive amount") { form.amount = it }
        FieldRow {
            ReadField("Received by", actor.substringBefore(" ·"), Modifier.weight(1f))
            ReadField("Date", form.dateText, Modifier.weight(1f)) { form.dateOpen = true }
        }
        EditField("Purpose / reference", form.purpose, "Required") { form.purpose = it }
        SummaryCard("After mark received", collectionSummary(model, currency, typed))
    } else {
        if (lovTrade) {
            val selected = model.paymentReadiness.firstOrNull { it.id == form.parentId }?.title.orEmpty()
            LovField("Unpaid sale", selected, if (model.paymentReadiness.none { it.relevance == "RECEIVABLE" }) "Nothing is open" else "Search open records") {
                form.lovKind = "OBLIGATION"
            }
        } else {
            OpenRecords(mode, model.paymentReadiness, form.parentId) { row ->
                form.parentId = row.id
                row.counterpartyName?.takeIf { it.isNotBlank() }?.let { form.party = it }
            }
        }
        if (channels.isNotEmpty()) ChoiceBlock(if (distribution) "Buyer / dealer" else "Received from", channels, form.channel) { form.channel = it }
        AmountBlock("Amount received", currency, form.amount, "Positive amount") { form.amount = it }
        FieldRow {
            ReadField("Received by", actor.substringBefore(" ·"), Modifier.weight(1f))
            ReadField("Date", form.dateText, Modifier.weight(1f)) { form.dateOpen = true }
        }
        NoteField("Note · Optional", form.note, "Optional") { form.note = it }
        SummaryCard("After mark received", collectionSummary(model, currency, typed))
    }
}

@Composable
private fun BudgetTargetCapture(
    form: CaptureForm,
    mode: String,
    model: OperatingProjectionDto,
    currency: String,
    production: Boolean,
    lovTrade: Boolean,
    marketing: Boolean,
    client: Boolean,
    own: Boolean,
) {
    val typed = form.amount.toBigDecimalOrNull()
    if (production && mode == "BUDGET") {
        ReadField("Plan month", form.dateText) { form.dateOpen = true }
        ChoiceBlock("Revise", listOf("Budget", "Target"), form.choice) { form.choice = it }
        SummaryCard(
            "Current plan",
            listOf(
                "Budget ${formatMoney(money(model.budget?.amount), currency)} · Target ${formatMoney(money(model.target?.value), currency)}",
                "The current plan stays active until this revision is approved.",
            ),
        )
        AmountBlock(if (form.choice == "Target") "Proposed target" else "Proposed budget", currency, form.amount, "Proposal only") { form.amount = it }
        EditField("Revision reason", form.purpose, "Required") { form.purpose = it }
        SummaryCard("After approval", budgetSummary(model, currency, typed))
    } else if (client && mode == "TARGET") {
        ReadField("Campaign", model.title.orEmpty().ifBlank { "This campaign" })
        EditField("Metric", form.purpose, "Required") { form.purpose = it }
        ChoiceBlock(
            "Revision reason",
            listOf("Extend launch outcome", "Change launch scope", "Correct planning error"),
            form.choice,
        ) { form.choice = it }
        NoteField("Revision note", form.note, "Required") { form.note = it }
        ReadField("Effective date", form.dateText) { form.dateOpen = true }
        AmountBlock("Proposed target", currency, form.amount, "Proposal only") { form.amount = it }
        SummaryCard(
            "Current plan",
            listOf("The current target stays active until this revision is approved."),
        )
    } else if (own && mode == "TARGET") {
        ReadField("Effective date", form.dateText) { form.dateOpen = true }
        ChoiceBlock(
            "Target measure",
            listOf("Leads", "Revenue / Sales", "Orders / Conversions", "Custom Count"),
            form.choice,
        ) { form.choice = it }
        AmountBlock("Proposed target", currency, form.amount, "Positive value") { form.amount = it }
        EditField("Revision reason", form.purpose, "Required") { form.purpose = it }
        SummaryCard(
            "Current plan",
            listOf("The current target stays active until this revision is approved."),
        )
    } else if (marketing && mode == "BUDGET") {
        ReadField("Plan month", form.dateText) { form.dateOpen = true }
        SummaryCard(
            "Current plan",
            listOf(
                "Budget ${formatMoney(money(model.budget?.amount), currency)} · Used ${formatMoney(money(model.budget?.spent), currency)}",
                "The current plan stays active until this revision is approved.",
            ),
        )
        AmountBlock("Proposed budget", currency, form.amount, "Proposed only. The current plan stays active until approval.") { form.amount = it }
        EditField("Revision reason", form.purpose, "Required") { form.purpose = it }
        SummaryCard("After approval", budgetSummary(model, currency, typed))
    } else if (lovTrade && mode == "BUDGET") {
        ReadField("Plan month", form.dateText) { form.dateOpen = true }
        if (model.budget?.configured == true) {
            SummaryCard(
                "Current plan",
                listOf(
                    "Budget ${formatMoney(money(model.budget?.amount), currency)} · Used ${formatMoney(money(model.budget?.spent), currency)}",
                    "The current plan stays active until this revision is approved.",
                ),
            )
        } else {
            Text(
                "No budget yet. A proposal stays inactive until it is approved.",
                color = SheetMuted,
                fontFamily = PlusJakartaSans,
                fontSize = 13.sp,
            )
        }
        AmountBlock("Proposed budget", currency, form.amount, "Proposal only") { form.amount = it }
        EditField("Revision reason", form.purpose, "Why this changes") { form.purpose = it }
        SummaryCard("After approval", budgetSummary(model, currency, typed))
    } else {
        ReadField(if (mode == "TARGET") "Plan" else "Plan month", form.dateText) { form.dateOpen = true }
        SummaryCard(
            "Current plan",
            listOf(
                "Budget ${formatMoney(money(model.budget?.amount), currency)} · Used ${formatMoney(money(model.budget?.spent), currency)}",
                "The current plan stays active until this revision is approved.",
            ),
        )
        AmountBlock(if (mode == "TARGET") "Proposed target" else "Proposed budget", currency, form.amount, "Proposal only") { form.amount = it }
        EditField("Revision reason", form.purpose, "Why this changes") { form.purpose = it }
        SummaryCard("After approval", budgetSummary(model, currency, typed))
    }
}

@Composable
private fun OrderCapture(
    form: CaptureForm,
    model: OperatingProjectionDto,
    currency: String,
    channels: List<String>,
    actor: String,
    production: Boolean,
    lovTrade: Boolean,
    trading: Boolean,
    distribution: Boolean,
    restaurant: Boolean,
) {
    if (production) {
        EditField("Buyer", form.party, "Required") { form.party = it }
        EditField("Quantity", form.quantity, "Required") { form.quantity = it }
        EditField("Unit", form.unit, "pcs") { form.unit = it }
        AmountBlock("Order value · Optional", currency, form.amount, "Leave blank when the order is unpriced") { form.amount = it }
        ReadField("Order date", form.dateText) { form.dateOpen = true }
        if (model.subtype == "MADE_TO_ORDER") {
            ChoiceBlock("Quotation", listOf("Draft", "Sent", "Accepted", "Rejected", "Expired"), form.goodsStatusLabel.ifBlank { "Accepted" }) {
                form.goodsStatusLabel = it
            }
            Text("Accepted confirms the order. It does not post revenue, a receivable, or GST.", color = SheetMuted, fontFamily = PlusJakartaSans, fontSize = 12.sp)
        } else {
            Text("An order is optional. It is not revenue, cash, stock, or a receivable.", color = SheetMuted, fontFamily = PlusJakartaSans, fontSize = 12.sp)
        }
    } else when {
        restaurant || model.subtype == "RETAIL_STORE" -> {
            if (channels.isNotEmpty()) ChoiceBlock("Order channel", channels, form.channel) { form.channel = it }
            else EditField("Order channel", form.channel, "Required") { form.channel = it }
            ChoiceBlock("Order status", listOf("Placed", "Preparing", "Completed", "Cancelled"), form.choice.ifBlank { "Placed" }) { form.choice = it }
            model.activity.filter { it.eventType == "ORDER" }.forEach { row ->
                TextButton(onClick = { form.parentId = row.id }) {
                    Text(if (form.parentId == row.id) "Updating ${row.title}" else row.title, color = Color.White, fontFamily = PlusJakartaSans, fontSize = 13.sp)
                }
            }
            EditField("Guest or customer", form.party, "Optional") { form.party = it }
            AmountBlock("Estimated amount", currency, form.amount, "Pipeline only") { form.amount = it }
            ReadField("Order time", form.dateText) { form.dateOpen = true }
            Text("An order is not a sale until you bill it.", color = SheetMuted, fontFamily = PlusJakartaSans, fontSize = 12.sp)
        }
        lovTrade -> {
            ChoiceBlock("Order", listOf("Sales order", "Purchase order"), form.orderSideLabel) { form.orderSideLabel = it }
            if (lovTrade) {
                LovItemLine(form.itemName, form.quantity, form.unit, form.unitPrice, { form.lovKind = "PRODUCT" }, { form.lovKind = "UOM" }, { form.quantity = it }, { form.unitPrice = it })
            }
            LovField(if (form.orderSideLabel == "Purchase order") "Supplier" else "Buyer", form.party, "party") {
                form.lovKind = if (form.orderSideLabel == "Purchase order") "SUPPLIER" else "BUYER"
            }
            LovField("Channel", form.channel, "channel") { form.lovKind = "CHANNEL" }
            ChoiceBlock("Status", listOf("Placed", "Preparing", "Completed", "Cancelled"), form.choice.ifBlank { "Placed" }) { form.choice = it }
            AmountBlock("Estimated amount", currency, form.amount, "Commitment only") { form.amount = it }
            EditField("Promised date", form.link, "YYYY-MM-DD") { form.link = it }
            Text("An order is not a sale or a purchase until you bill it.", color = SheetMuted, fontFamily = PlusJakartaSans, fontSize = 12.sp)
        }
        distribution -> {
            EditField("Buyer / dealer", form.party, "Required") { form.party = it }
            EditField("Order / dispatch reference", form.purpose, "Required") { form.purpose = it }
            ChoiceBlock(
                "Fulfillment status",
                listOf("Open", "In progress", "Dispatched", "Completed", "Cancelled", "Delayed", "Blocked"),
                form.choice,
            ) { form.choice = it }
            NoteField("Fulfillment note", form.note, "Optional") { form.note = it }
            AmountBlock("Value · Optional", currency, form.amount, "Leave blank if there is no amount") { form.amount = it }
            FieldRow {
                ReadField("Recorded by", actor.substringBefore(" ·"), Modifier.weight(1f))
                ReadField("Draft time", form.dateText, Modifier.weight(1f)) { form.dateOpen = true }
            }
            SummaryCard(
                "After save",
                listOf("Saves a dispatch note.", "This does not create a sale or a money record."),
            )
        }
        trading -> {
            EditField("Buyer / order reference", form.purpose, "Who and which order") { form.purpose = it }
            ChoiceBlock(
                "Dispatch status",
                listOf("Pending", "Ready for dispatch", "In progress", "Delayed", "Blocked", "Completed"),
                form.choice,
            ) { form.choice = it }
            NoteField("Dispatch note", form.note, "What changed") { form.note = it }
            FieldRow {
                ReadField("Recorded by", actor.substringBefore(" ·"), Modifier.weight(1f))
                ReadField("Draft time", form.dateText, Modifier.weight(1f)) { form.dateOpen = true }
            }
            SummaryCard(
                "After save",
                listOf("Saves an operational note.", "Money totals stay unchanged."),
            )
        }
        else -> {
            NoteField("Order", form.purpose, "What happened") { form.purpose = it }
            EditField("Name", form.party, "Who this is for") { form.party = it }
            FieldRow {
                ReadField("Recorded by", actor.substringBefore(" ·"), Modifier.weight(1f))
                ReadField("Draft time", form.dateText, Modifier.weight(1f)) { form.dateOpen = true }
            }
            SummaryCard("After save", listOf("Saved as activity on this business.", "Money totals stay as they are unless this record carries an amount."))
        }
    }
}

@Composable
private fun ShipmentCapture(
    form: CaptureForm,
    model: OperatingProjectionDto,
    currency: String,
    actor: String,
    lovTrade: Boolean,
    trading: Boolean,
    distribution: Boolean,
) {
    if (lovTrade) {
        ChoiceBlock("Movement", listOf("Outgoing sale", "Incoming purchase"), form.movementLabel) {
            form.movementLabel = it
            form.parentId = ""
        }
        val sourceType = if (form.movementLabel == "Incoming purchase") "PURCHASE" else "SALE"
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
                form.parentId = row.id
                form.purpose = row.title
                row.counterpartyName?.takeIf { it.isNotBlank() }?.let { form.party = it }
                row.quantity?.takeIf { it.isNotBlank() }?.let { form.quantity = it }
                row.unit?.takeIf { it.isNotBlank() }?.let { form.unit = it }
            }) {
                Text(if (form.parentId == row.id) "Using ${row.title}" else row.title, color = Color.White, fontSize = 13.sp)
            }
        }
        EditField("Quantity", form.quantity, "Required") { form.quantity = it }
        LovField("Unit", form.unit, "UOM") { form.lovKind = "UOM" }
        ChoiceBlock("Status", listOf("Placed", "In transit", "Delivered", "Cancelled"), form.choice.ifBlank { "In transit" }) { form.choice = it }
        Text("Delivery moves goods against the source. It does not create another sale or purchase.", color = SheetMuted, fontFamily = PlusJakartaSans, fontSize = 12.sp)
    } else if (distribution) {
        EditField("Shipment / delivery reference", form.purpose, "Required") { form.purpose = it }
        ChoiceBlock(
            "Delivery status",
            listOf("Delivered", "Out for delivery", "Delayed", "Blocked", "Cancelled"),
            form.choice,
        ) { form.choice = it }
        NoteField("Delivery note", form.note, "Required") { form.note = it }
        EditField("Dealer", form.party, "Required") { form.party = it }
        EditField("Linked dispatch", form.link, "Optional") { form.link = it }
        AmountBlock("Value · Optional", currency, form.amount, "Leave blank if there is no amount") { form.amount = it }
        SummaryCard(
            "After save",
            listOf("Saves a delivery note.", "Money totals stay unchanged."),
        )
    } else if (trading) {
        EditField("Shipment / delivery reference", form.purpose, "Which shipment") { form.purpose = it }
        ChoiceBlock(
            "Delivery status",
            listOf("Pending", "Out for delivery", "Delayed", "Blocked", "Delivered"),
            form.choice,
        ) { form.choice = it }
        NoteField("Delivery note", form.note, "What changed") { form.note = it }
        FieldRow {
            ReadField("Recorded by", actor.substringBefore(" ·"), Modifier.weight(1f))
            ReadField("Draft time", form.dateText, Modifier.weight(1f)) { form.dateOpen = true }
        }
        SummaryCard(
            "After save",
            listOf("Saves a shipment note.", "Money totals stay unchanged."),
        )
    } else {
        NoteField("Shipment", form.purpose, "What happened") { form.purpose = it }
        EditField("Name", form.party, "Who this is for") { form.party = it }
        FieldRow {
            ReadField("Recorded by", actor.substringBefore(" ·"), Modifier.weight(1f))
            ReadField("Draft time", form.dateText, Modifier.weight(1f)) { form.dateOpen = true }
        }
        SummaryCard("After save", listOf("Saved as activity on this business.", "Money totals stay as they are unless this record carries an amount."))
    }
}

@Composable
private fun AppointmentCapture(
    form: CaptureForm,
    model: OperatingProjectionDto,
    actor: String,
    service: Boolean,
    seed: OperatingActivityDto?,
) {
    if (service) {
        EditField("Service", form.purpose, "Required") { form.purpose = it }
        ReadField("Appointment time", form.dateText) { form.dateOpen = true }
        EditField("Guest or customer", form.party, "Optional for a walk-in") { form.party = it }
        EditField("Assigned person", form.detail, "Optional") { form.detail = it }
        model.activity.filter { it.eventType == "APPOINTMENT" }.forEach { row ->
            TextButton(onClick = { form.parentId = row.id }) {
                Text(if (form.parentId == row.id) "Updating ${row.title}" else row.title, color = Color.White, fontFamily = PlusJakartaSans, fontSize = 13.sp)
            }
        }
        if (form.parentId.isNotBlank() || seed?.id?.isNotBlank() == true) {
            ChoiceBlock("Action", listOf("Reschedule", "Cancel", "No-show", "Start", "Complete"), form.choice) { form.choice = it }
        }
        Text(
            "Booking, starting, and completing do not post revenue. Finalize a charge separately.",
            color = SheetMuted,
            fontFamily = PlusJakartaSans,
            fontSize = 12.sp,
        )
    } else {
        NoteField("Appointment", form.purpose, "What happened") { form.purpose = it }
        EditField("Name", form.party, "Who this is for") { form.party = it }
        FieldRow {
            ReadField("Recorded by", actor.substringBefore(" ·"), Modifier.weight(1f))
            ReadField("Draft time", form.dateText, Modifier.weight(1f)) { form.dateOpen = true }
        }
        SummaryCard("After save", listOf("Saved as activity on this business.", "Money totals stay as they are unless this record carries an amount."))
    }
}

@Composable
private fun ActivityCapture(
    form: CaptureForm,
    mode: String,
    model: OperatingProjectionDto,
    currency: String,
    channels: List<String>,
    channelLabel: String,
    actor: String,
    production: Boolean,
    lovTrade: Boolean,
    trading: Boolean,
    distribution: Boolean,
    marketing: Boolean,
    restaurant: Boolean,
    service: Boolean,
    eventType: String = "",
) {
    if (lovTrade && mode == "UPDATE") {
        EditField("Update", form.purpose, "What happened") { form.purpose = it }
        ReadField("Date", form.dateText) { form.dateOpen = true }
        Text("Saved as activity. Money totals stay unchanged.", color = SheetMuted, fontFamily = PlusJakartaSans, fontSize = 12.sp)
    } else if (production && mode == "PRODUCTION") {
        ChoiceBlock("Output", listOf("Quick output", "Detailed batch"), form.entryMode) { form.entryMode = it }
        AmountBlock("Output quantity", currency, form.amount, "Recorded output. This is not accepted stock.") { form.amount = it }
        FieldRow {
            EditField("Unit", form.detail, "Required", Modifier.weight(1f)) { form.detail = it }
            ReadField("Date", form.dateText, Modifier.weight(1f)) { form.dateOpen = true }
        }
        EditField(if (form.entryMode == "Detailed batch") "Finished product" else "Output context", form.purpose, "Required for a detailed batch") { form.purpose = it }
        if (form.entryMode == "Detailed batch") {
            EditField("Accepted quantity", form.quantity, "Required to add finished stock") { form.quantity = it }
            EditField("Rejected quantity", form.note, "Optional") { form.note = it }
            EditField("Scrap quantity", form.receivedAmount, "Does not become finished stock") { form.receivedAmount = it }
            Text("An optional bill of materials stays on the run. It does not consume material.", color = SheetMuted, fontFamily = PlusJakartaSans, fontSize = 12.sp)
            ChoiceBlock("Status", listOf("Planned", "In progress", "Completed"), form.choice) { form.choice = it }
        }
        model.activity.filter { it.eventType == "CUSTOMER_ORDER" }.forEach { row ->
            TextButton(onClick = { form.parentId = row.id }) { Text(row.title, color = Color.White) }
        }
        Text("Quick output does not create a finished SKU.", color = SheetMuted, fontFamily = PlusJakartaSans, fontSize = 12.sp)
    } else if (production && mode == "UPDATE") {
        ChoiceBlock("Record", listOf("Update", "Issue", "Approval", "Budget", "Target"), form.choice) { form.choice = it }
        if (form.choice == "Budget" || form.choice == "Target") {
            AmountBlock(if (form.choice == "Target") "Proposed target" else "Proposed budget", currency, form.amount, "Effective when approved") { form.amount = it }
        }
        NoteField("What happened", form.purpose, "Required") { form.purpose = it }
    } else if (production && mode == "DELIVERY") {
        Text("Customer order", color = SheetMuted, fontFamily = PlusJakartaSans, fontSize = 12.sp)
        model.activity.filter { it.eventType == "CUSTOMER_ORDER" || it.eventType == "SALE" }.forEach { row ->
            TextButton(onClick = { form.parentId = row.id }) { Text(if (form.parentId == row.id) "Selected ${row.title}" else row.title, color = Color.White) }
        }
        ChoiceBlock("Delivery", listOf("Quick status", "Itemized"), form.entryMode) { form.entryMode = it }
        ChoiceBlock("Status", listOf("Planned", "Dispatched", "Delivered", "Accepted", "Returned"), form.choice) { form.choice = it }
        if (form.entryMode == "Itemized") {
            EditField("Delivered quantity", form.quantity, "Cannot exceed the open order") { form.quantity = it }
            EditField("Unit", form.unit, "pcs") { form.unit = it }
        }
        ReadField("Delivery date", form.dateText) { form.dateOpen = true }
        Text("A status-only delivery does not post a quantity or a sale.", color = SheetMuted, fontFamily = PlusJakartaSans, fontSize = 12.sp)
    } else if (marketing && mode == "RESULT" && eventType == "PROGRESS") {
        val open = model.activity.filter { it.eventType == "ACTIVITY" && it.approvalStatus != "REJECTED" && it.approvalStatus != "PENDING" }
        Text(if (open.isEmpty()) "Add an activity before updating progress." else "Open activity", color = SheetMuted, fontFamily = PlusJakartaSans, fontSize = 12.sp)
        open.forEach { row ->
            TextButton(onClick = { form.parentId = row.id }) {
                Text(if (form.parentId == row.id) "Selected ${row.title}" else row.title, color = Color.White)
            }
        }
        ChoiceBlock("Status", listOf("In progress", "Blocked", "Postponed", "Completed", "Cancelled"), form.activityStatus) { form.activityStatus = it }
        AmountBlock("Result quantity", currency, form.amount, "Leave blank to update status only") { form.amount = it }
        ChoiceBlock("Reporting", listOf("Incremental", "Cumulative"), form.reportingMode) { form.reportingMode = it }
        ChoiceBlock("Metric", listOf("LEADS", "CONVERSIONS", "REACH", "ENGAGEMENT", "ORDERS"), form.metricCode) { form.metricCode = it }
        Text("A status update does not add to the target.", color = SheetMuted, fontFamily = PlusJakartaSans, fontSize = 12.sp)
    } else if (marketing && mode == "RESULT") {
        AmountBlock("Value", currency, form.amount, "Positive amount · 2 decimal places") { form.amount = it }
        if (channels.isNotEmpty()) ChoiceBlock(channelLabel, channels, form.channel) { form.channel = it }
        FieldRow {
            ReadField("Result date", form.dateText, Modifier.weight(1f)) { form.dateOpen = true }
            EditField("Metric", form.purpose, "What was measured", Modifier.weight(1f)) { form.purpose = it }
        }
        ChoiceBlock("Reporting", listOf("Incremental", "Cumulative"), form.reportingMode) { form.reportingMode = it }
        ChoiceBlock("Metric", listOf("LEADS", "CONVERSIONS", "REACH", "ENGAGEMENT", "ORDERS"), form.metricCode) { form.metricCode = it }
        ChoiceBlock("Status", listOf("Accepted", "Pending"), form.choice) { form.choice = it }
        EditField("Source", form.resultSource, "Where this result was observed") { form.resultSource = it }
        EditField("Period start", form.periodStart, "YYYY-MM-DD") { form.periodStart = it }
        EditField("Period end", form.periodEnd, "YYYY-MM-DD") { form.periodEnd = it }
        val earlier = model.activity.filter { it.eventType == "RESULT" && it.approvalStatus != "REJECTED" }
        if (earlier.isNotEmpty()) {
            Text("Correct an earlier result", color = SheetMuted, fontFamily = PlusJakartaSans, fontSize = 12.sp)
            earlier.forEach { row ->
                TextButton(onClick = { form.supersedesId = if (form.supersedesId == row.id) "" else row.id }) {
                    Text(if (form.supersedesId == row.id) "Replaces ${row.title}" else row.title, color = Color.White)
                }
            }
        }
        ChoiceBlock("Overlap", listOf("Channel figure", "Deduplicated reach"), if (form.overlapDeduped) "Deduplicated reach" else "Channel figure") {
            form.overlapDeduped = it == "Deduplicated reach"
        }
        Text("Attributed sales stay an observation. They are not revenue.", color = SheetMuted, fontFamily = PlusJakartaSans, fontSize = 12.sp)
        MoreDetails(form.moreOpen, "Optional note, evidence link or source context.") { form.moreOpen = !form.moreOpen }
        if (form.moreOpen) NoteField("Note", form.note, "Optional") { form.note = it }
        Text(
            "Accepted Result counts toward target, not its Receipt.",
            color = SheetMuted,
            fontFamily = PlusJakartaSans,
            fontSize = 12.sp,
        )
    } else if (marketing && mode == "UPDATE") {
        ChoiceBlock(
            "Status",
            listOf("In progress", "Blocked", "Postponed", "Completed", "Cancelled"),
            form.activityStatus,
        ) { form.activityStatus = it }
        NoteField("Update text", form.purpose, "Required") { form.purpose = it }
        EditField("Linked operational context", form.note, "Optional") { form.note = it }
        FieldRow {
            ReadField("Recorded by", actor.substringBefore(" ·"), Modifier.weight(1f))
            ReadField("Draft time", form.dateText, Modifier.weight(1f)) { form.dateOpen = true }
        }
        SummaryCard("After save", listOf("Saved as campaign activity.", "Money totals stay unchanged."))
    } else if (marketing && mode == "DELIVERABLE") {
        EditField("Deliverable / milestone name", form.purpose, "Required") { form.purpose = it }
        ChoiceBlock("Type", listOf("Deliverable", "Milestone"), form.choice) { form.choice = it }
        EditField("Owner", form.party, "Required") { form.party = it }
        FieldRow {
            ReadField("Due date", form.dateText, Modifier.weight(1f)) { form.dateOpen = true }
            Column(Modifier.weight(1f)) {
                ChoiceBlock("Status", listOf("Launched", "Due", "Complete", "Blocked"), form.detail) { form.detail = it }
            }
        }
        if (channels.isNotEmpty()) ChoiceBlock("Channel", channels, form.channel) { form.channel = it }
        EditField("Dependency", form.note, "Optional") { form.note = it }
        Text("Launched, Due, Complete, or Blocked. Closing retains obligations.", color = SheetMuted, fontFamily = PlusJakartaSans, fontSize = 12.sp)
    } else if (marketing && mode == "COMPLETE") {
        EditField("Result / deliverable reference", form.purpose, "Required") { form.purpose = it }
        ChoiceBlock("Completion status", listOf("Assigned", "In review", "Blocked", "Complete", "Partial"), form.choice) { form.choice = it }
        NoteField("Completion note", form.note, "Required") { form.note = it }
        ReadField("Completion date", form.dateText) { form.dateOpen = true }
        Text(
            "Completion does not add revenue or a receipt.",
            color = SheetMuted,
            fontFamily = PlusJakartaSans,
            fontSize = 12.sp,
        )
    } else {
        if (mode == "RESULT" || mode == "PRODUCTION") {
            AmountBlock(if (mode == "PRODUCTION") "Output" else "Result", currency, form.amount, "Amount for this record") { form.amount = it }
            if (channels.isNotEmpty()) ChoiceBlock(channelLabel, channels, form.channel) { form.channel = it }
        }
        NoteField(
            when {
                (service || trading || distribution) && mode == "UPDATE" -> "Update text"
                restaurant && mode == "UPDATE" -> "Update"
                else -> textLabel(mode)
            },
            form.purpose,
            "What happened",
        ) { form.purpose = it }
        if ((restaurant || service || trading || distribution) && mode == "UPDATE") {
            EditField(if (service || trading || distribution) "Linked operational context" else "Linked context", form.note, "Optional") { form.note = it }
        }
        if (mode == "DELIVERY") {
            EditField("Name", form.party, "Who this is for") { form.party = it }
        }
        FieldRow {
            ReadField("Recorded by", actor.substringBefore(" ·"), Modifier.weight(1f))
            ReadField("Draft time", form.dateText, Modifier.weight(1f)) { form.dateOpen = true }
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
}

@Composable
private fun IssueCapture(
    form: CaptureForm,
    actor: String,
    production: Boolean,
    trading: Boolean,
    distribution: Boolean,
    marketing: Boolean,
    restaurant: Boolean,
    service: Boolean,
) {
    NoteField("Issue", form.purpose, "What needs attention") { form.purpose = it }
        ChoiceBlock("Priority", listOf("Low", "Normal", "High"), form.choice) { form.choice = it }
        EditField("Owner", form.party, actor.substringBefore(" ·")) { form.party = it }
        if (restaurant || service || trading || distribution || marketing || production) {
            ReadField("Due date", form.dateText) { form.dateOpen = true }
            EditField("Operational context", form.note, "Optional") { form.note = it }
        }
        SummaryCard(
            "After save",
            listOf("Opens an attention item.", "Money totals stay unchanged."),
        )
}

@Composable
private fun StockCapture(
    form: CaptureForm,
    actor: String,
    production: Boolean,
    contractFamily: Boolean,
    lovTrade: Boolean,
    trading: Boolean,
    distribution: Boolean,
    restaurant: Boolean,
    service: Boolean,
) {
    if (contractFamily && (trading || distribution || restaurant || service)) {
        if (lovTrade) LovField("Item", form.purpose, "product") { form.lovKind = "PRODUCT" }
        else EditField("Item", form.purpose, "Product or SKU") { form.purpose = it }
        ChoiceBlock("Action", listOf("Set current stock", "Add", "Reduce"), form.stockAction) { form.stockAction = it }
        EditField("Quantity", form.quantity, "Required") { form.quantity = it }
        if (lovTrade) LovField("Unit", form.unit, "UOM") { form.lovKind = "UOM" }
        else EditField("Unit", form.unit, "pcs, kg, L") { form.unit = it }
        ChoiceBlock("Reason", listOf("Opening count", "Stocktake", "Damage", "Waste", "Return", "Correction"), form.stockReason) { form.stockReason = it }
        if (trading || distribution) {
            ChoiceBlock("Ownership", listOf("Owned", "Consignment"), form.ownership) { form.ownership = it }
        }
        EditField("Reorder point", form.detail, "Optional") { form.detail = it }
        Text("Set replaces the count. Add and reduce move it. This is not a purchase.", color = SheetMuted, fontFamily = PlusJakartaSans, fontSize = 12.sp)
    } else if (production) {
        EditField("Item", form.purpose, "Required") { form.purpose = it }
        ChoiceBlock("Action", listOf("Set", "Add", "Reduce"), form.choice) { form.choice = it }
        ChoiceBlock("Reason", listOf("opening", "stocktake", "damage", "waste", "return", "correction"), form.note) { form.note = it }
        FieldRow {
            EditField("Quantity", form.quantity, "Required", Modifier.weight(1f)) { form.quantity = it }
            EditField("Unit", form.unit, "kg", Modifier.weight(1f)) { form.unit = it }
        }
        Text("Damage is not an expense or a sale.", color = SheetMuted, fontFamily = PlusJakartaSans, fontSize = 12.sp)
    } else {
        EditField(if (distribution) "Availability context" else "Stock context", form.purpose, "What changed") { form.purpose = it }
        ChoiceBlock(
            if (distribution) "Availability status" else "Stock status",
            when {
                distribution -> listOf("Available", "Needs check", "Limited availability")
                trading -> listOf("Available", "Needs check", "Low availability")
                else -> listOf("In stock", "Needs check", "Low stock")
            },
            form.choice,
        ) { form.choice = it }
        NoteField(if (distribution) "Availability note" else "Stock note", form.note, if (trading || distribution) "Required" else "What you saw") { form.note = it }
        if (trading || distribution) {
            FieldRow {
                ReadField("Recorded by", actor.substringBefore(" ·"), Modifier.weight(1f))
                ReadField("Draft time", form.dateText, Modifier.weight(1f)) { form.dateOpen = true }
            }
        } else {
            ReadField("Recorded by", actor.substringBefore(" ·"))
        }
        SummaryCard("After save", listOf("Saves an operational signal.", "No purchase, sale, or money record is created."))
    }
}

@Composable
private fun ReturnCapture(
    form: CaptureForm,
    model: OperatingProjectionDto,
    currency: String,
) {
    val customerReturn = form.returnDirection.startsWith("Customer")
        val sourceType = if (customerReturn) "SALE" else "PURCHASE"
        ChoiceBlock("Return type", listOf("Customer return", "Supplier return"), form.returnDirection) {
            form.returnDirection = it
            form.parentId = ""
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
                    .background(if (form.parentId == row.id) SheetHigh else Color.Transparent)
                    .border(1.dp, if (form.parentId == row.id) SheetAccent else SheetBorder, RoundedCornerShape(12.dp))
                    .clickable {
                        form.parentId = row.id
                        if (form.amount.isBlank()) row.amount?.takeIf { it.isNotBlank() }?.let { form.amount = it }
                        row.counterpartyName?.takeIf { it.isNotBlank() }?.let { form.party = it }
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
        AmountBlock("Credit amount", currency, form.amount, "Positive amount · cannot exceed the source") { form.amount = it }
        ChoiceBlock("Goods", listOf("To stock", "Not returned", "Pending inspection"), form.goodsOutcome) { form.goodsOutcome = it }
        EditField("Reason", form.returnReason, "Required") { form.returnReason = it.take(80) }
        ChoiceBlock("Items", listOf("Quick amount", "Itemized"), form.entryMode) { form.entryMode = it }
        if (form.entryMode == "Itemized") {
            LovItemLine(form.itemName, form.quantity, form.unit, form.unitPrice, { form.lovKind = "PRODUCT" }, { form.lovKind = "UOM" }, { form.quantity = it }, { form.unitPrice = it }, showPrice = false)
        } else if (form.goodsOutcome == "To stock") {
            FieldRow {
                EditField("Qty returned", form.quantity, "Required", Modifier.weight(1f)) { form.quantity = it }
                LovField("UOM", form.unit, "UOM", Modifier.weight(1f)) { form.lovKind = "UOM" }
            }
        }
        ReadField("Return date", form.dateText) { form.dateOpen = true }
        NoteField("Note · Optional", form.note, "Optional") { form.note = it }
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

@Composable
private fun PersonCapture(
    form: CaptureForm,
    production: Boolean,
    trading: Boolean,
    distribution: Boolean,
    marketing: Boolean,
    restaurant: Boolean,
    service: Boolean,
) {
    EditField("Name", form.party, "Person") { form.party = it }
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
            form.purpose,
            "How they work here",
        ) { form.purpose = it }
        ChoiceBlock("Access", OperatingMomentCatalog.roles.map { it.second }, form.choice) { form.choice = it }
        if (restaurant || service || trading || distribution || marketing || production) EditField("Scope", form.note, "Optional") { form.note = it }
        Text("A job label is not a grant. Access applies after the invitation is accepted.", color = SheetMuted, fontFamily = PlusJakartaSans, fontSize = 12.sp)
}

@Composable
private fun SupplierPartnerCapture(
    form: CaptureForm,
    mode: String,
    currency: String,
    channels: List<String>,
    production: Boolean,
    trading: Boolean,
    distribution: Boolean,
    marketing: Boolean,
    restaurant: Boolean,
    service: Boolean,
    client: Boolean,
    own: Boolean,
) {
    if (production && mode == "PARTNER") {
        EditField("Supplier name", form.party, "Required") { form.party = it }
        Text("Enter a supplier name to enable Save.", color = SheetAccent, fontFamily = PlusJakartaSans, fontSize = 12.sp)
        EditField("What they supply", form.purpose, "Optional") { form.purpose = it }
        EditField("Payment terms", form.note, "Optional") { form.note = it }
        AmountBlock("Opening due · Optional", currency, form.amount, "Leave blank if nothing is owed") { form.amount = it }
        if (channels.isNotEmpty()) ChoiceBlock("Channel", channels, form.channel) { form.channel = it }
        Text("Saving creates no purchase or expense.", color = SheetMuted, fontFamily = PlusJakartaSans, fontSize = 12.sp)
    } else if (marketing && mode == "PARTNER") {
        EditField(if (own) "Partner / counterparty" else "Name", form.party, "Required") { form.party = it }
        Text(
            "Enter a name to enable Save.",
            color = SheetAccent,
            fontFamily = PlusJakartaSans,
            fontSize = 12.sp,
        )
        if (client) {
            ChoiceBlock("Relationship", listOf("Client", "Vendor", "Creator", "Platform", "Partner"), form.choice) { form.choice = it }
            EditField("Purpose", form.purpose, "Optional") { form.purpose = it }
        }
        if (channels.isNotEmpty()) {
            ChoiceBlock(if (own) "Accepted channel" else "Channel", channels, form.channel) { form.channel = it }
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
            form.party,
            "Name",
        ) { form.party = it }
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
            form.purpose,
            when {
                restaurant -> "Ingredients or packaging"
                trading || distribution -> "Stock or packaging"
                else -> "Goods or service"
            },
        ) { form.purpose = it }
        ChoiceBlock("Payment terms", listOf("Due on receipt", "7 days", "30 days"), form.choice) { form.choice = it }
        AmountBlock("Opening due", currency, form.amount, "Leave at 0 if nothing is owed") { form.amount = it }
        if (channels.isNotEmpty()) ChoiceBlock(if (restaurant || service || trading || distribution) "Where you sell" else "Channel", channels, form.channel) { form.channel = it }
        Text("Saving creates the relationship. It does not create a purchase or an expense.", color = SheetMuted, fontFamily = PlusJakartaSans, fontSize = 12.sp)
    }
}

@Composable
private fun MastersCapture(
    form: CaptureForm,
    model: OperatingProjectionDto,
) {
    ChoiceBlock("Master", masterKinds(model.subtype), form.masterKind) { form.masterKind = it }
        EditField("Name", form.itemName, "Required") { form.itemName = it }
        if (form.masterKind in setOf("PRODUCT", "MATERIAL", "CONSUMABLE")) {
            ChoiceBlock("Unit", listOf("piece", "kg", "litre", "count"), form.unit) { form.unit = it }
        }
        EditField("Detail", form.purpose, "Optional") { form.purpose = it }
        MasterGstFields(form, model)
        Text("Saving adds a selectable record. It does not create a sale or a cost.", color = SheetMuted, fontFamily = PlusJakartaSans, fontSize = 12.sp)
}

@Composable
private fun ApprovalCapture(
    form: CaptureForm,
    model: OperatingProjectionDto,
    currency: String,
    marketing: Boolean,
    onDecide: (suspend (String, String) -> Unit)?,
) {
    ApprovalBody(
        model,
        currency,
        form.choice,
        form.note,
        { form.choice = it },
        { form.note = it },
        onDecide,
        if (marketing) listOf("Approve", "Reject", "Return") else listOf("Approve", "Reject"),
    )
}

@Composable
private fun FallbackNoteCapture(
    form: CaptureForm,
) {
    NoteField("Note", form.purpose, "What happened") { form.purpose = it }
}

@Composable
private fun CaptureSheetFooter(
    form: CaptureForm,
    mode: String,
    eventType: String,
    title: String,
    model: OperatingProjectionDto,
    restaurant: Boolean,
    service: Boolean,
    trading: Boolean,
    distribution: Boolean,
    marketing: Boolean,
    client: Boolean,
    own: Boolean,
    production: Boolean,
    job: Boolean,
    standard: Boolean,
    lovTrade: Boolean,
    seed: OperatingActivityDto?,
    onDecide: (suspend (String, String) -> Unit)?,
    onDismiss: () -> Unit,
    onSubmit: suspend (OperatingCaptureDraft) -> Unit,
    scope: kotlinx.coroutines.CoroutineScope,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .border(1.dp, SheetBorder, RoundedCornerShape(0.dp))
            .padding(horizontal = 17.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(footerLine(mode, restaurant, service, trading, distribution, marketing, client, production, job), color = SheetMuted, fontFamily = PlusJakartaSans, fontSize = 12.sp)
        val action = actionLabel(mode, form.choice, restaurant, service, trading, distribution, marketing, client, production)
        val enabled = when {
            mode == "RETURN" -> form.parentId.isNotBlank() && form.returnReason.isNotBlank()
            lovTrade && mode == "SPEND" -> form.expenseCategory.isNotBlank() && form.amount.toBigDecimalOrNull()?.let { it > BigDecimal.ZERO } == true && (form.choice == "Paid" || form.party.isNotBlank())
            lovTrade && mode == "SALE" -> {
                val amountOk = if (form.entryMode.equals("Itemized", ignoreCase = true)) form.itemName.isNotBlank() else form.amount.toBigDecimalOrNull()?.let { it > BigDecimal.ZERO } == true
                amountOk && form.channel.isNotBlank() && (form.choice == "Received" || form.party.isNotBlank())
            }
            lovTrade && mode == "STOCK" -> form.purpose.isNotBlank() && form.quantity.isNotBlank()
            lovTrade && mode == "UPDATE" -> form.purpose.isNotBlank()
            lovTrade && mode == "ISSUE" -> form.purpose.isNotBlank()
            lovTrade && mode == "ORDER" -> form.party.isNotBlank() || form.channel.isNotBlank()
            lovTrade && mode == "SHIPMENT" -> form.parentId.isNotBlank() && form.quantity.isNotBlank()
            lovTrade && mode == "BUDGET" -> form.purpose.isNotBlank() && form.amount.toBigDecimalOrNull()?.let { it > BigDecimal.ZERO } == true
            lovTrade && (mode == "COLLECTION" || mode == "PAYMENT") -> {
                val entered = form.amount.toBigDecimalOrNull()
                val cap = model.paymentReadiness.firstOrNull { it.id == form.parentId }?.amount?.toBigDecimalOrNull()
                entered != null && entered > BigDecimal.ZERO && form.parentId.isNotBlank() && (cap == null || entered <= cap)
            }
            mode == "SUPPLIER" || mode == "PARTNER" -> form.party.isNotBlank()
            marketing && mode == "VENDOR" -> form.party.isNotBlank()
            client && mode == "COLLECTION" -> form.party.isNotBlank() && form.purpose.isNotBlank()
            own && mode == "COLLECTION" -> form.purpose.isNotBlank()
            client && mode == "TARGET" -> form.purpose.isNotBlank() && form.note.isNotBlank()
            own && mode == "TARGET" -> form.purpose.isNotBlank()
            marketing && mode == "BUDGET" -> form.purpose.isNotBlank()
            marketing && mode == "DELIVERABLE" -> form.purpose.isNotBlank() && form.party.isNotBlank()
            marketing && mode == "COMPLETE" -> form.purpose.isNotBlank() && form.note.isNotBlank()
            marketing && mode == "PAYMENT" -> form.party.isNotBlank() && form.purpose.isNotBlank()
            marketing && mode == "ISSUE" -> form.purpose.isNotBlank() && form.party.isNotBlank()
            production && mode == "SALE" -> form.amount.toBigDecimalOrNull()?.let { it > BigDecimal.ZERO } == true && (form.choice == "Received" || form.party.isNotBlank() || form.channel.isNotBlank())
            production && mode == "PURCHASE" -> form.party.isNotBlank() && form.amount.toBigDecimalOrNull()?.let { it > BigDecimal.ZERO } == true
            production && mode == "ORDER" -> form.party.isNotBlank() && form.quantity.isNotBlank()
            production && mode == "DELIVERY" -> form.parentId.isNotBlank() && (form.entryMode != "Itemized" || form.quantity.isNotBlank())
            production && mode == "STOCK" -> form.purpose.isNotBlank() && form.quantity.isNotBlank()
            production && mode == "PRODUCTION" -> form.amount.toBigDecimalOrNull()?.let { it > BigDecimal.ZERO } == true && form.detail.isNotBlank() && (form.entryMode != "Detailed batch" || form.purpose.isNotBlank())
            production && mode == "UPDATE" -> form.purpose.isNotBlank() || form.choice == "Budget" || form.choice == "Target"
            production && mode == "BUDGET" -> form.purpose.isNotBlank()
            production && mode == "ISSUE" -> form.purpose.isNotBlank() && form.party.isNotBlank()
            production && mode == "UPDATE" -> form.purpose.isNotBlank()
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
                            form.error = "Nothing is waiting."
                            return@clickable
                        }
                        if ((form.choice == "Reject" || form.choice == "Return") && form.note.isBlank()) {
                            form.error = "A reason is required."
                            return@clickable
                        }
                        form.error = null
                        scope.launch {
                            decide(item.id, if (form.choice == "Approve") "APPROVED" else "REJECTED")
                            onDismiss()
                        }
                        return@clickable
                    }
                    if (lovTrade && mode in setOf("SALE", "PURCHASE") && form.originalCurrency.isNotBlank()) {
                        val rate = form.fxRate.toBigDecimalOrNull()
                        if (rate == null || rate <= BigDecimal.ZERO) {
                            form.error = "Enter an FX rate for the original currency."
                            return@clickable
                        }
                    }
                    val draftNote = if (distribution && mode == "SHIPMENT" && form.link.isNotBlank()) {
                        listOf(form.note, form.link).filter { it.isNotBlank() }.joinToString(" · ")
                    } else {
                        form.note
                    }
                    val unpaidDue = when (mode) {
                        "SALE", "SERVICE" -> form.choice != "Received"
                        "PURCHASE", "SPEND" -> form.choice != "Paid"
                        else -> false
                    }
                    val draft = buildDraft(
                        mode, eventType, title, form.amount, form.channel, form.party, form.purpose, draftNote, form.choice,
                        occurredAtValue(form.eventDate), trading, distribution, client, own, form.detail,
                        standard, job, form.link, form.itemName, form.quantity, form.unit, form.unitPrice, form.parentId,
                        seed?.id, form.masterKind, model.paymentReadiness,
                        retail = model.subtype in setOf(
                            "RETAIL_STORE",
                            "RESTAURANT_FOOD",
                            "CONSUMER_SERVICE",
                            "TRADING_WHOLESALE",
                            "DISTRIBUTION_DEALERSHIP",
                        ) || trading || distribution,
                        restaurant = restaurant,
                        service = service,
                        entryMode = form.entryMode,
                        receiptState = form.receiptState,
                        ownership = form.ownership,
                        stockActionLabel = form.stockAction,
                        stockReasonLabel = form.stockReason,
                        receivedAmount = form.receivedAmount,
                        productMasterId = form.productMasterId,
                        taxModeLabel = form.taxMode,
                        taxAmount = form.taxAmount,
                        originalCurrency = form.originalCurrency,
                        fxRate = form.fxRate,
                        returnDirectionLabel = form.returnDirection,
                        goodsOutcomeLabel = form.goodsOutcome,
                        returnReason = form.returnReason,
                        expenseCategory = form.expenseCategory,
                        lovTrade = lovTrade,
                        quantityReceived = form.quantityReceived,
                        orderSideLabel = form.orderSideLabel,
                        goodsStatusLabel = form.goodsStatusLabel,
                        goodsDueDay = form.goodsDueDate.toString(),
                        gstRate = form.gstRate,
                        supplyChoice = form.supplyChoice,
                        gstOn = model.gstTracking == "YES" && (trading || distribution),
                        gstApplicable = form.gstApplicable,
                        gstClassification = form.gstClassification,
                        hsn = form.hsn,
                        masterPriceIncludes = form.masterSaleIncludes,
                        masterPurchaseIncludes = form.masterPurchaseIncludes,
                        gstEffectiveFrom = form.gstEffectiveFrom,
                        partyGstRegistration = form.partyGstRegistration,
                        businessState = form.businessState,
                        billingState = form.billingState,
                        deliveryState = form.deliveryState,
                        partyGstin = form.partyGstin,
                        reportingMode = form.reportingMode,
                        metricCode = form.metricCode,
                        activityStatus = form.activityStatus,
                        costBasis = form.costBasis,
                        overlapDeduped = form.overlapDeduped,
                        creativeVersion = form.creativeVersion,
                        resultSource = form.resultSource,
                        idempotencyKey = form.idempotencyKey,
                        periodStart = form.periodStart,
                        periodEnd = form.periodEnd,
                        supersedesId = form.supersedesId,
                        effectiveDay = form.eventDate.toString(),
                        dueAtDay = when {
                            mode == "ISSUE" -> form.eventDate.toString()
                            unpaidDue -> form.dueDate.toString()
                            else -> form.link.takeIf { it.matches(Regex("""\d{4}-\d{2}-\d{2}.*""")) }
                        },
                    )
                    if (draft == null) {
                        form.error = requiredMessage(mode)
                        return@clickable
                    }
                    form.error = null
                    scope.launch {
                        try {
                            onSubmit(draft)
                        } catch (failure: Exception) {
                            form.error = failure.message?.takeIf { it.isNotBlank() } ?: "Could not save."
                        }
                    }
                }
                .padding(vertical = 15.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CaptureDateDialogs(form: CaptureForm) {
    if (form.dateOpen) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = localDateToUtcMillis(form.eventDate))
        DatePickerDialog(
            onDismissRequest = { form.dateOpen = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { form.eventDate = utcMillisToLocalDate(it) }
                    form.dateOpen = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { form.dateOpen = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
    if (form.dueDateOpen) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = localDateToUtcMillis(form.dueDate))
        DatePickerDialog(
            onDismissRequest = { form.dueDateOpen = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { form.dueDate = utcMillisToLocalDate(it) }
                    form.dueDateOpen = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { form.dueDateOpen = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
    if (form.goodsDueOpen) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = localDateToUtcMillis(form.goodsDueDate))
        DatePickerDialog(
            onDismissRequest = { form.goodsDueOpen = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { form.goodsDueDate = utcMillisToLocalDate(it) }
                    form.goodsDueOpen = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { form.goodsDueOpen = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
private fun CaptureLovHost(
    form: CaptureForm,
    model: OperatingProjectionDto,
    mode: String,
    channels: List<String>,
    distribution: Boolean,
    onCreateMaster: (suspend (String, String, String?, String?) -> Unit)?,
    scope: kotlinx.coroutines.CoroutineScope,
) {
    form.lovKind?.let { kind ->
        val pickerKind = kind.uppercase()
        SearchableOperatingLovSheet(
            title = lovTitle(kind),
            kind = kind,
            pageSize = if (pickerKind in setOf("SALES_ORDER", "PURCHASE_ORDER", "OBLIGATION")) 20 else null,
            options = when (pickerKind) {
                "SALES_ORDER", "PURCHASE_ORDER" -> orderLovOptions(model, pickerKind)
                "OBLIGATION" -> obligationLovOptions(mode, model)
                else -> lovOptions(kind, model.masters.orEmpty(), channels, form.localMasters).let { rows ->
                    if (!kind.equals("UOM", ignoreCase = true)) rows
                    else {
                        val base = model.masters.orEmpty().firstOrNull { it.id == form.productMasterId }?.unit
                            ?: form.localMasters.firstOrNull { it.kind.equals("PRODUCT", true) && (it.name == form.itemName || it.name == form.purpose) }?.unit
                        val conversion = model.masters.orEmpty().firstOrNull { it.id == form.productMasterId }?.conversionToBase
                        if (base.isNullOrBlank()) rows else rows.filter { OperatingSeedUoms.compatible(it.name, base, conversion) }
                    }
                }
            },
            canAdd = onCreateMaster != null && pickerKind !in setOf("UOM", "SALES_ORDER", "PURCHASE_ORDER", "OBLIGATION"),
            unitChoices = if (kind.equals("PRODUCT", ignoreCase = true)) OperatingSeedUoms.all else emptyList(),
            accent = if (distribution) Color(0xFF66C7A4) else Color(0xFF6DDCFF),
            onSelect = { option -> form.applyLov(kind, option, mode, model) },
            onAdd = { name, unitArg, conversion ->
                val created = OperatingLovOption(
                    id = "local-$kind-${name.lowercase()}",
                    name = name,
                    kind = kind,
                    unit = unitArg ?: if (kind == "UOM") name else null,
                    subtitle = unitArg,
                )
                form.localMasters = form.localMasters + created
                form.applyLov(kind, created, mode, model)
                onCreateMaster?.let { create -> scope.launch { create(kind, name, unitArg, conversion) } }
            },
            onDismiss = { form.lovKind = null },
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
    taxMode: String? = null,
    gstRate: String? = null,
    taxJurisdiction: String? = null,
    quotationStatus: String? = null,
    scrapQuantity: String? = null,
    idempotencyKey: String? = null,
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
        idempotencyKey = idempotencyKey,
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
                    scrapQuantity = if (detailed) scrapQuantity else null,
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
                    taxMode = taxMode,
                    gstRate = gstRate,
                    taxJurisdiction = taxJurisdiction,
                ),
                item = itemName.ifBlank { null },
            )
        }
        "SPEND" -> {
            val value = amount.toBigDecimalOrNull()
            if (value == null || value <= BigDecimal.ZERO) return null
            if (!choice.equals("Paid", true) && party.isBlank()) return null
            draft("SPEND", purpose.ifBlank { title }, amount, contract = com.example.momentra.data.api.OperatingContractBody(paymentState = payCode(choice), taxMode = taxMode, gstRate = gstRate, taxJurisdiction = taxJurisdiction))
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
                    taxMode = taxMode,
                    gstRate = gstRate,
                    taxJurisdiction = taxJurisdiction,
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
                contract = com.example.momentra.data.api.OperatingContractBody(
                    orderStatus = if (quotationStatus == null || quotationStatus == "ACCEPTED") "PLACED" else null,
                    estimatedAmount = amount.ifBlank { null },
                    quotationStatus = quotationStatus,
                ),
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
    goodsDueDay: String? = null,
    gstRate: String = "",
    supplyChoice: String = "",
    gstOn: Boolean = false,
    gstApplicable: String = "",
    gstClassification: String = "",
    hsn: String = "",
    masterPriceIncludes: String = "",
    masterPurchaseIncludes: String = "",
    gstEffectiveFrom: String = "",
    partyGstRegistration: String = "",
    businessState: String = "",
    billingState: String = "",
    deliveryState: String = "",
    partyGstin: String = "",
    reportingMode: String = "INCREMENTAL",
    metricCode: String = "LEADS",
    activityStatus: String = "IN_PROGRESS",
    costBasis: String = "Agency borne",
    overlapDeduped: Boolean = false,
    creativeVersion: String = "1",
    resultSource: String = "",
    idempotencyKey: String = "",
    periodStart: String = "",
    periodEnd: String = "",
    supersedesId: String = "",
    effectiveDay: String = "",
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
        val postedTax = if (gstOn && mode in setOf("SALE", "PURCHASE", "SPEND")) when (taxModeLabel.lowercase()) {
            "inclusive" -> "INCLUSIVE"
            "exclusive" -> "EXCLUSIVE"
            else -> null
        } else null
        val quote = if (job && mode == "ORDER") when (goodsStatusLabel) {
            "Draft" -> "DRAFT"
            "Sent" -> "SENT"
            "Rejected" -> "REJECTED"
            "Expired" -> "EXPIRED"
            else -> "ACCEPTED"
        } else null
        return manufacturingDraft(
            mode, eventType, title, amount, channel, party, purpose, note, choice, occurredAt,
            detail, link, itemName, quantity, unit, parentId, entryMode, receiptState, receivedAmount, quantityReceived,
            taxMode = postedTax,
            gstRate = if (postedTax != null) gstRate.filter { it.isDigit() || it == '.' }.ifBlank { null } else null,
            taxJurisdiction = if (postedTax != null && (supplyChoice == "INTRA" || supplyChoice == "INTER")) supplyChoice else null,
            quotationStatus = quote,
            scrapQuantity = if (mode == "PRODUCTION") receivedAmount.ifBlank { null } else null,
            idempotencyKey = idempotencyKey.ifBlank { null },
        )
    }
    val statusOnlyProgress = marketing && eventType == "PROGRESS" && amount.isBlank()
    val needsAmount = mode in setOf("SALE", "SERVICE", "PURCHASE", "VENDOR", "SPEND", "PAYMENT", "REIMBURSE", "COLLECTION", "BUDGET", "TARGET", "RESULT", "PRODUCTION", "RETURN") && !statusOnlyProgress
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
    if (marketing && mode == "RESULT" && eventType != "PROGRESS" && purpose.isBlank()) return null
    if (marketing && eventType == "PROGRESS" && parentId.isBlank()) return null
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
    if (lovTrade && mode in setOf("PAYMENT", "COLLECTION")) {
        val cap = readiness.firstOrNull { it.id == parentId }?.amount?.toBigDecimalOrNull()
        if (value == null || value <= BigDecimal.ZERO || (cap != null && value > cap)) return null
    }
    if (lovTrade && mode == "BUDGET" && (purpose.isBlank() || value == null || value <= BigDecimal.ZERO)) return null
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
            promisedAt = when {
                lovTrade && (
                    (mode in setOf("SALE", "SERVICE") && goodsStatusLabel != "Fully delivered") ||
                        (mode == "PURCHASE" && receiptState != "Fully received" && receiptState != "Received")
                    ) -> goodsDueDay
                mode in setOf("ORDER", "SHIPMENT", "DELIVERY") -> promised
                else -> null
            },
            declaredQuantity = if (mode == "STOCK" && stockActionCode == "SET") quantity.ifBlank { null } else null,
            expenseCategory = if (lovTrade && mode == "SPEND") expenseCategory.trim().ifBlank { null } else null,
            taxMode = if (taxApplies) taxCode else null,
            taxAmount = if (taxApplies && taxCode != "NOT_CONFIGURED" && !gstOn) taxAmount.ifBlank { null } else null,
            gstRate = if (gstOn && taxApplies) gstRate.filter { it.isDigit() || it == '.' }.ifBlank { null } else null,
            taxJurisdiction = if (gstOn && taxApplies) supplyChoice.takeIf { it == "INTRA" || it == "INTER" } else null,
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
        masterGstApplicable = if (mode == "MASTERS") when (gstApplicable) {
            "Yes" -> "YES"
            "No" -> "NO"
            "Not sure" -> "NOT_SURE"
            else -> null
        } else null,
        masterGstClassification = if (mode == "MASTERS") when (gstClassification) {
            "Taxable" -> "TAXABLE"
            "Exempt" -> "EXEMPT"
            "Nil-rated" -> "NIL"
            "Non-GST" -> "NON_GST"
            else -> null
        } else null,
        masterGstRate = if (mode == "MASTERS") gstRate.filter { it.isDigit() || it == '.' }.ifBlank { null } else null,
        masterPriceIncludesGst = if (mode == "MASTERS") priceModeCode(masterPriceIncludes) else null,
        masterSalePriceIncludesGst = if (mode == "MASTERS") priceModeCode(masterPriceIncludes) else null,
        masterPurchasePriceIncludesGst = if (mode == "MASTERS") priceModeCode(masterPurchaseIncludes) else null,
        masterGstEffectiveFrom = if (mode == "MASTERS") gstEffectiveFrom.trim().takeIf { it.matches(Regex("""\d{4}-\d{2}-\d{2}""")) } else null,
        masterHsn = if (mode == "MASTERS") hsn.trim().ifBlank { null } else null,
        masterGstRegistration = if (mode == "MASTERS") when (partyGstRegistration) {
            "Registered" -> "REGISTERED"
            "Unregistered" -> "UNREGISTERED"
            "Not sure" -> "NOT_SURE"
            else -> null
        } else null,
        masterBusinessState = if (mode == "MASTERS") businessState.trim().ifBlank { null } else null,
        masterBillingState = if (mode == "MASTERS") billingState.trim().ifBlank { null } else null,
        masterDeliveryState = if (mode == "MASTERS") deliveryState.trim().ifBlank { null } else null,
        masterGstin = if (mode == "MASTERS") partyGstin.trim().ifBlank { null } else null,
        reportingMode = if (marketing && mode == "RESULT" && !statusOnlyProgress) {
            if (reportingMode == "Cumulative") "CUMULATIVE" else "INCREMENTAL"
        } else null,
        metricCode = if (marketing && mode == "RESULT" && !statusOnlyProgress) metricCode else null,
        activityStatus = if (marketing && (mode == "UPDATE" || eventType == "PROGRESS")) when (activityStatus) {
            "Blocked" -> "BLOCKED"
            "Postponed" -> "POSTPONED"
            "Completed" -> "COMPLETED"
            "Cancelled" -> "CANCELLED"
            else -> "IN_PROGRESS"
        } else null,
        costBasis = if (marketing && mode == "SPEND" && client) {
            if (costBasis == "Client reimbursable") "REIMBURSABLE" else "AGENCY"
        } else null,
        overlapDeduped = if (marketing && mode == "RESULT") overlapDeduped else null,
        creativeVersion = if (marketing && mode == "APPROVAL") creativeVersion.ifBlank { "1" } else null,
        resultSource = if (marketing && mode == "RESULT" && eventType != "PROGRESS") resultSource.ifBlank { null } else null,
        revisionKind = if (marketing && mode == "BUDGET") "BUDGET" else if (marketing && mode == "TARGET") "TARGET" else null,
        effectiveFrom = if (marketing && mode in setOf("BUDGET", "TARGET")) effectiveDay.takeIf { it.matches(Regex("""\d{4}-\d{2}-\d{2}""")) } else null,
        periodStart = if (marketing && mode == "RESULT" && eventType != "PROGRESS") periodStart.takeIf { it.matches(Regex("""\d{4}-\d{2}-\d{2}""")) } else null,
        periodEnd = if (marketing && mode == "RESULT" && eventType != "PROGRESS") periodEnd.takeIf { it.matches(Regex("""\d{4}-\d{2}-\d{2}""")) } else null,
        supersedesEventId = if (marketing && mode == "RESULT") supersedesId.ifBlank { null } else null,
        idempotencyKey = if (marketing || retail || restaurant || service || standard || job) idempotencyKey else null,
        dueAt = dueAtDay?.takeIf { it.matches(Regex("""\d{4}-\d{2}-\d{2}.*""")) }
            ?: link.takeIf { it.matches(Regex("""\d{4}-\d{2}-\d{2}.*""")) },
        contract = retailContract,
        masterId = if (lineItemName.isNotBlank()) productMasterId?.takeIf { it.isNotBlank() } else null,
    )
}

private fun priceModeCode(label: String): String? = when (label) {
    "GST included in price", "Prices include GST" -> "YES"
    "GST added separately" -> "NO"
    else -> null
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

private fun lovTitle(kind: String): String = when (kind.uppercase()) {
    "PRODUCT" -> "Product"
    "SALES_ORDER" -> "Sales order"
    "PURCHASE_ORDER" -> "Purchase order"
    "OBLIGATION" -> "Open record"
    "BUYER" -> "Buyer"
    "SUPPLIER" -> "Supplier"
    "CHANNEL" -> "Channel"
    "EXPENSE_CATEGORY" -> "Expense category"
    "UOM" -> "UOM"
    else -> kind.lowercase().replaceFirstChar { it.uppercase() }
}

private fun orderLovOptions(model: OperatingProjectionDto, side: String): List<OperatingLovOption> {
    val childType = if (side == "PURCHASE_ORDER") "PURCHASE" else "SALE"
    return model.activity.mapNotNull { row ->
        if (row.eventType != "ORDER" || row.status == "COMPLETED" || row.status == "CANCELLED") return@mapNotNull null
        if (side == "PURCHASE_ORDER") {
            if (row.orderSide != "PURCHASE") return@mapNotNull null
        } else if (row.orderSide == "PURCHASE") {
            return@mapNotNull null
        }
        val estimated = row.estimatedAmount?.toBigDecimalOrNull() ?: BigDecimal.ZERO
        val executed = model.activity
            .filter { it.parentEventId == row.id && it.eventType == childType }
            .fold(BigDecimal.ZERO) { total, child -> total + (child.amount?.toBigDecimalOrNull() ?: BigDecimal.ZERO) }
        if (estimated > BigDecimal.ZERO && executed >= estimated) return@mapNotNull null
        val remaining = estimated - executed
        val subtitle = listOfNotNull(
            row.counterpartyName?.takeIf { it.isNotBlank() },
            if (estimated > BigDecimal.ZERO) "Ordered ${estimated.stripTrailingZeros().toPlainString()}" else null,
            "Executed ${executed.stripTrailingZeros().toPlainString()}",
            if (estimated > BigDecimal.ZERO) "Remaining ${remaining.stripTrailingZeros().toPlainString()}" else null,
        ).joinToString(" · ")
        OperatingLovOption(id = row.id, name = row.title, kind = side, subtitle = subtitle.ifBlank { null })
    }
}

private fun obligationLovOptions(mode: String, model: OperatingProjectionDto): List<OperatingLovOption> {
    return model.paymentReadiness.mapNotNull { row ->
        val wanted = if (mode == "COLLECTION") row.relevance == "RECEIVABLE" else row.relevance == "PAYABLE" || row.relevance == "REIMBURSEMENT"
        if (!wanted) return@mapNotNull null
        val subtitle = listOfNotNull(
            row.counterpartyName?.takeIf { it.isNotBlank() },
            row.occurredAt?.take(10)?.takeIf { it.isNotBlank() },
            row.total?.takeIf { it.isNotBlank() }?.let { "Total $it" },
            "Open ${row.amount}",
            row.dueAt?.take(10)?.takeIf { it.isNotBlank() },
        ).joinToString(" · ")
        OperatingLovOption(id = row.id, name = row.title, kind = "OBLIGATION", subtitle = subtitle.ifBlank { null }, detailAmount = row.amount)
    }
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

private data class GstPreview(
    val taxable: BigDecimal,
    val cgst: BigDecimal,
    val sgst: BigDecimal,
    val igst: BigDecimal,
    val gst: BigDecimal,
    val invoice: BigDecimal,
)

private fun previewGst(form: CaptureForm): GstPreview? {
    val rate = form.gstRate.toBigDecimalOrNull() ?: return null
    if (rate.signum() <= 0) return null
    val qty = form.quantity.toBigDecimalOrNull()?.takeIf { it.signum() > 0 } ?: BigDecimal.ONE
    val price = form.unitPrice.toBigDecimalOrNull()?.takeIf { it.signum() > 0 }
    val entered = if (price != null) qty.multiply(price) else form.amount.toBigDecimalOrNull() ?: return null
    if (entered.signum() <= 0) return null
    val moneyScale = 2
    val inclusive = form.taxMode == "Inclusive"
    val taxable: BigDecimal
    val gst: BigDecimal
    val invoice: BigDecimal
    if (inclusive) {
        invoice = entered.setScale(moneyScale, RoundingMode.HALF_UP)
        taxable = invoice.divide(BigDecimal.ONE.add(rate.movePointLeft(2)), moneyScale, RoundingMode.HALF_UP)
        gst = invoice.subtract(taxable)
    } else {
        taxable = entered.setScale(moneyScale, RoundingMode.HALF_UP)
        gst = taxable.multiply(rate).movePointLeft(2).setScale(moneyScale, RoundingMode.HALF_UP)
        invoice = taxable.add(gst)
    }
    val intra = form.supplyChoice == "INTRA"
    val cgst = if (intra) gst.divide(BigDecimal(2), moneyScale, RoundingMode.HALF_UP) else BigDecimal.ZERO.setScale(moneyScale)
    val sgst = if (intra) gst.subtract(cgst) else BigDecimal.ZERO.setScale(moneyScale)
    val igst = if (form.supplyChoice == "INTER") gst else BigDecimal.ZERO.setScale(moneyScale)
    return GstPreview(taxable, cgst, sgst, igst, gst, invoice)
}

@Composable
private fun GstPriceFields(form: CaptureForm, model: OperatingProjectionDto, purchase: Boolean) {
    if (model.gstTracking != "YES") return
    val included = "GST included in price"
    val added = "GST added separately"
    val selected = when (form.taxMode) {
        "Inclusive" -> included
        "Exclusive" -> added
        else -> ""
    }
    ChoiceBlock(if (purchase) "Purchase price" else "Sale price", listOf(included, added), selected) {
        form.taxMode = if (it == included) "Inclusive" else "Exclusive"
    }
    EditField("GST rate %", form.gstRate, "From the product, or type a rate") {
        form.gstRate = it.filter { c -> c.isDigit() || c == '.' }
    }
    ChoiceBlock(
        "Supply",
        listOf("Inside your state", "Outside your state"),
        when (form.supplyChoice) {
            "INTRA" -> "Inside your state"
            "INTER" -> "Outside your state"
            else -> ""
        },
    ) { form.supplyChoice = if (it.startsWith("Inside")) "INTRA" else "INTER" }
    if (form.supplyChoice.isBlank()) {
        Text("Confirm whether this supply is inside your state or outside it.", color = SheetMuted, fontFamily = PlusJakartaSans, fontSize = 12.sp)
    }
    val preview = previewGst(form)
    val modeLabel = if (form.taxMode == "Inclusive") "Included in price" else "Added separately"
    val rateLabel = form.gstRate.ifBlank { "—" }
    Text(
        if (form.gstDetailsOpen) "GST $rateLabel% · $modeLabel · Hide" else "GST $rateLabel% · $modeLabel · View",
        color = SheetAccent,
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.clickable { form.gstDetailsOpen = !form.gstDetailsOpen },
    )
    if (purchase && preview != null) {
        Text("Supplier invoice total ${formatMoney(preview.invoice, model.currency ?: "INR")}", color = SheetInk, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Text("Amount paid stays separate from this invoice total.", color = SheetMuted, fontFamily = PlusJakartaSans, fontSize = 12.sp)
    }
    if (form.gstDetailsOpen && preview != null) {
        Text("Taxable ${formatMoney(preview.taxable, model.currency ?: "INR")}", color = SheetMuted, fontFamily = PlusJakartaSans, fontSize = 12.sp)
        if (form.supplyChoice == "INTER") {
            Text("IGST ${formatMoney(preview.igst, model.currency ?: "INR")}", color = SheetMuted, fontFamily = PlusJakartaSans, fontSize = 12.sp)
        } else if (form.supplyChoice == "INTRA") {
            Text("CGST ${formatMoney(preview.cgst, model.currency ?: "INR")} · SGST ${formatMoney(preview.sgst, model.currency ?: "INR")}", color = SheetMuted, fontFamily = PlusJakartaSans, fontSize = 12.sp)
        }
        Text("GST ${formatMoney(preview.gst, model.currency ?: "INR")} · Invoice ${formatMoney(preview.invoice, model.currency ?: "INR")}", color = SheetMuted, fontFamily = PlusJakartaSans, fontSize = 12.sp)
        Text("Place of supply ${if (form.supplyChoice == "INTER") "outside your state" else if (form.supplyChoice == "INTRA") "inside your state" else "not confirmed"}", color = SheetMuted, fontFamily = PlusJakartaSans, fontSize = 12.sp)
    } else if (preview == null) {
        Text("A missing rate is not treated as 0%.", color = SheetMuted, fontFamily = PlusJakartaSans, fontSize = 12.sp)
    }
}

@Composable
private fun MasterGstFields(form: CaptureForm, model: OperatingProjectionDto) {
    val gstFamily = model.subtype in setOf("TRADING_WHOLESALE", "DISTRIBUTION_DEALERSHIP", "RETAIL_STORE", "RESTAURANT_FOOD", "CONSUMER_SERVICE", "STANDARD_PRODUCTION", "MADE_TO_ORDER")
    if (!gstFamily || model.gstTracking != "YES") return
    val product = form.masterKind in setOf("PRODUCT", "MATERIAL", "CONSUMABLE")
    val party = form.masterKind in setOf("CUSTOMER", "BUYER", "SUPPLIER")
    if (!product && !party) return
    if (party) {
        EditField("Delivery location", form.deliveryState, "Where goods usually go") { form.deliveryState = it }
    }
    Text(
        if (product) "Add GST Details (optional)" else "GST & Business Location",
        color = SheetAccent,
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.clickable { form.gstDetailsOpen = !form.gstDetailsOpen },
    )
    if (!form.gstDetailsOpen) return
    if (product) {
        Text("Set the product’s GST rate for automatic calculations.", color = SheetMuted, fontFamily = PlusJakartaSans, fontSize = 12.sp)
        ChoiceBlock("GST applicable", listOf("Yes", "No", "Not sure"), form.gstApplicable) { form.gstApplicable = it }
        ChoiceBlock("Classification", listOf("Taxable", "Exempt", "Nil-rated", "Non-GST"), form.gstClassification) { form.gstClassification = it }
        EditField("GST rate %", form.gstRate, "Your rate") { form.gstRate = it.filter { c -> c.isDigit() || c == '.' } }
        ChoiceBlock("Selling prices", listOf("Use business default", "GST included in price", "GST added separately"), form.masterSaleIncludes) { form.masterSaleIncludes = it }
        ChoiceBlock("Purchase prices", listOf("Use business default", "GST included in price", "GST added separately"), form.masterPurchaseIncludes) { form.masterPurchaseIncludes = it }
        EditField("Effective from", form.gstEffectiveFrom, "YYYY-MM-DD optional") { form.gstEffectiveFrom = it.take(10) }
        EditField("HSN Code (Optional)", form.hsn, "Add the product’s HSN code if you know it") { form.hsn = it.take(16) }
    } else {
        Text("Save their tax details for future transactions.", color = SheetMuted, fontFamily = PlusJakartaSans, fontSize = 12.sp)
        ChoiceBlock("GST registration", listOf("Registered", "Unregistered", "Not sure"), form.partyGstRegistration) { form.partyGstRegistration = it }
        EditField("Business state", form.businessState, "State") { form.businessState = it }
        EditField("Billing state", form.billingState, "State") { form.billingState = it }
        EditField("GSTIN (Optional)", form.partyGstin, "Add their GST number if you have it") { form.partyGstin = it.take(15) }
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
    hideTax: Boolean = false,
) {
    if (!hideTax) {
        ChoiceBlock("Tax", listOf("Not configured", "Inclusive", "Exclusive"), taxMode, onTaxMode)
        if (taxMode != "Not configured") {
            EditField("Tax amount", taxAmount, "0.00") { onTaxAmount(it.filter { c -> c.isDigit() || c == '.' }) }
        }
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
    title: String? = null,
    empty: String? = null,
    onPick: (OperatingPaymentDto) -> Unit,
) {
    val rows = readiness.filter { row ->
        if (mode == "COLLECTION") row.relevance == "RECEIVABLE" else row.relevance == "PAYABLE" || row.relevance == "REIMBURSEMENT"
    }
    Text(
        if (rows.isEmpty()) {
            empty ?: if (mode == "COLLECTION") "No unpaid sale is open." else "No unpaid purchase or expense is open."
        } else {
            title ?: if (mode == "COLLECTION") "Unpaid sales" else "Unpaid purchases and expenses"
        },
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

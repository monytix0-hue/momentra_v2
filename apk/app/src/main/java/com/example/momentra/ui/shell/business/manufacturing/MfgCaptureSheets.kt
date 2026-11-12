package com.example.momentra.ui.shell.business.manufacturing

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.momentra.data.api.OperatingAllocationBody
import com.example.momentra.data.api.OperatingContractBody
import com.example.momentra.data.api.OperatingLineBody
import com.example.momentra.data.api.OperatingProjectionDto
import com.example.momentra.ui.shell.business.shared.OperatingChromeState
import com.example.momentra.ui.theme.ManufacturingAmber
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private fun mfgMonthLabel(ym: String): String {
    val parsed = runCatching { YearMonth.parse(ym.take(7)) }.getOrElse { YearMonth.now() }
    return parsed.format(DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH))
}

@Composable
internal fun MfgCaptureSheet(
    mode: String,
    model: OperatingProjectionDto,
    onDismiss: () -> Unit,
    onSubmit: (ManufacturingEventDraft) -> Unit,
    onNavigateUtility: ((MfgUtilityNav) -> Unit)? = null,
) {
    when (mode.uppercase()) {
        "SALE" -> MfgSaleSheet(model, onDismiss, onSubmit, onNavigateUtility)
        "PURCHASE" -> MfgPurchaseSheet(model, onDismiss, onSubmit)
        "COLLECTION" -> MfgCollectionSheet(model, onDismiss, onSubmit)
        "PAYMENT", "REIMBURSE" -> MfgPaymentSheet(model, onDismiss, onSubmit, reimburse = mode.equals("REIMBURSE", true))
        "SPEND" -> MfgExpenseSheet(model, onDismiss, onSubmit)
        "RETURN" -> MfgReturnSheet(model, onDismiss, onSubmit)
        "STOCK" -> MfgStockSheet(model, onDismiss, onSubmit, onNavigateUtility)
        "BUDGET", "TARGET" -> MfgBudgetSheet(model, onDismiss, onSubmit, target = mode.equals("TARGET", true))
        "UPDATE", "ISSUE" -> MfgIssueSheet(model, onDismiss, onSubmit, issue = mode.equals("ISSUE", true))
        "APPROVAL" -> MfgApprovalSheet(model, onDismiss, onSubmit)
        "ORDER", "CUSTOMER_ORDER" -> MfgOrderSheet(model, onDismiss, onSubmit)
        "DELIVERY", "SHIPMENT" -> MfgDeliverySheet(model, onDismiss, onSubmit)
        "PRODUCTION" -> MfgProductionSheet(model, onDismiss, onSubmit, onNavigateUtility)
        "REWORK" -> MfgReworkSheet(model, onDismiss, onSubmit)
        else -> MfgSaleSheet(model, onDismiss, onSubmit, onNavigateUtility)
    }
}

@Composable
private fun mfgCaptureScaffold(
    title: String,
    subtitle: String?,
    onDismiss: () -> Unit,
    why: String,
    content: @Composable () -> Unit,
) {
    MfgSheetBody(Modifier.verticalScroll(rememberScrollState())) {
        MfgGrabber()
        MfgSheetHeader(title = title, subtitle = subtitle, onClose = onDismiss)
        content()
        MfgWhyBox(why)
        Spacer(Modifier.height(16.dp))
    }
}

/** Figma `2396:30095` Record Sale — Quick amount | Itemized | Return | Reprint. */
@Composable
private fun MfgSaleSheet(
    model: OperatingProjectionDto,
    onDismiss: () -> Unit,
    onSubmit: (ManufacturingEventDraft) -> Unit,
    onNavigateUtility: ((MfgUtilityNav) -> Unit)?,
) {
    val currency = model.currency ?: "INR"
    var mode by remember { mutableStateOf("Quick amount") }
    var amount by remember { mutableStateOf("") }
    var customer by remember { mutableStateOf("") }
    var saleDate by remember { mutableStateOf(LocalDate.now().toString()) }
    var channel by remember { mutableStateOf("Wholesale") }
    var payState by remember { mutableStateOf("Partial") }
    var moreOpen by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    val lineItems = remember { mutableStateListOf<Pair<String, String>>() }
    val parties = model.masters.orEmpty().filter {
        it.kind.equals("CUSTOMER", true) || it.kind.equals("BUYER", true) || it.kind.equals("PARTY", true)
    }
    val finishedParts = model.masters.orEmpty().filter {
        it.kind.equals("SKU", true) || it.kind.equals("PART", true) || it.kind.equals("PRODUCT", true)
    }
    val itemizedTotal = lineItems.sumOf { it.second.toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO }
    val postable = when (mode) {
        "Itemized" -> lineItems.isNotEmpty() && customer.isNotBlank()
        else -> amount.toBigDecimalOrNull()?.signum() == 1 && customer.isNotBlank()
    }

    mfgCaptureScaffold(
        title = "Record Sale",
        subtitle = null,
        onDismiss = onDismiss,
        why = "Posted invoice owns revenue and output GST once — CGST+SGST in MH, IGST when place of supply is another state. Collection later allocates.",
    ) {
        MfgChipRow(null, listOf("Quick amount", "Itemized", "Return", "Reprint"), mode) { next ->
            when (next) {
                "Return" -> { mode = next }
                "Reprint" -> onNavigateUtility?.invoke(MfgUtilityNav.REPRINT) ?: run { mode = next }
                else -> mode = next
            }
        }
        when (mode) {
            "Itemized" -> {
                MfgAmountHero(
                    label = "Itemized total",
                    amount = if (lineItems.isEmpty()) "—" else MfgFormat.exact(currency, itemizedTotal.toPlainString()),
                    caption = if (lineItems.isEmpty()) "Add finished parts below · GST Unavailable" else "${lineItems.size} lines · GST Unavailable",
                    currencyBadge = currency,
                )
                MfgLinesCard("Lines", lineItems.size) {
                    if (lineItems.isEmpty()) {
                        MfgEmpty(
                            "No lines yet",
                            if (finishedParts.isEmpty()) {
                                "Parts browser empty — Itemized stays locked until a finished part exists."
                            } else {
                                "Tap a finished part below to add a real line. No pretend rows."
                            },
                        )
                    } else {
                        lineItems.forEachIndexed { idx, (name, price) ->
                            MfgPickerRow(
                                name,
                                MfgFormat.exact(currency, price),
                                onClick = { lineItems.removeAt(idx) },
                            )
                        }
                    }
                    finishedParts.take(8).forEach { p ->
                        val price = p.sellPrice?.takeIf { it.isNotBlank() } ?: "0"
                        MfgPickerRow(
                            "+ ${p.name}",
                            listOfNotNull(p.code, p.unit, MfgFormat.exact(currency, price)).joinToString(" · "),
                            onClick = {
                                if (lineItems.none { it.first == p.name }) {
                                    lineItems += p.name to price
                                    amount = lineItems
                                        .sumOf { it.second.toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO }
                                        .toPlainString()
                                }
                            },
                        )
                    }
                    if (finishedParts.isEmpty()) {
                        MfgLockedChip("Itemized locked · no parts in catalogue")
                    }
                }
                MfgUnavailableGstRow(label = "Output GST · CGST+SGST/IGST")
            }
            "Return" -> {
                MfgChipRow("Return type", listOf("Customer return", "Supplier return"), "Customer return") {}
                MfgAmountHero("Credit amount", if (amount.isBlank()) "₹0" else MfgFormat.exact(currency, amount), currencyBadge = currency)
            }
            else -> {
                MfgAmountHero(
                    label = "Sale amount",
                    amount = if (amount.isBlank()) "₹0" else MfgFormat.exact(currency, amount),
                    caption = listOfNotNull(customer.takeIf { it.isNotBlank() }?.let { "Customer $it" }, "INV draft").joinToString(" · "),
                    currencyBadge = currency,
                )
                MfgNumericKeypad(amount) { amount = it }
            }
        }
        MfgPickerRow("Customer", customer.ifBlank { "Select buyer" }, onClick = {
            customer = parties.firstOrNull()?.name ?: customer.ifBlank { "Walk-in" }
        }, externalLabel = true)
        parties.take(4).forEach { MfgPickerRow(it.name, it.gstin ?: "Buyer", onClick = { customer = it.name }) }
        MfgPickerRow("Sale date", MfgFormat.day(saleDate), onClick = { showDatePicker = true }, externalLabel = true)
        MfgDateQuickPicker(saleDate) { saleDate = it }
        MfgChipRow("Channel", listOf("Wholesale", "Retail counter", "Export", "Job work"), channel) { channel = it }
        MfgChipRow("Payment state", listOf("Paid", "Partial", "Open"), payState) { payState = it }
        MfgPickerRow(
            "GST",
            "Unavailable · stamped on invoice (Est. from HSN master)",
            onClick = null,
            externalLabel = true,
        )
        MfgMoreDetails(expanded = moreOpen, onToggle = { moreOpen = !moreOpen }, hint = "Notes · Attachment · Tags") {
            MfgTextField(note, "Notes", onValueChange = { note = it }, singleLine = false)
        }
        MfgStickyFooter(
            label = "Post sale",
            enabled = postable || (mode == "Return" && amount.toBigDecimalOrNull()?.signum() == 1),
            caveat = "Draft preserved if you close or open a picker",
            onClick = {
                val event = if (mode == "Return") "RETURN" else "SALE"
                val saleAmount = if (mode == "Itemized") itemizedTotal.toPlainString() else amount
                onSubmit(
                    ManufacturingEventDraft(
                        eventType = event,
                        title = if (event == "RETURN") "Credit · $customer" else "Sale · $customer",
                        amount = saleAmount,
                        note = note.ifBlank { null },
                        channelName = channel,
                        counterpartyName = customer,
                        paymentStatus = when (payState) {
                            "Paid" -> "PAID"
                            "Partial" -> "PARTIAL"
                            else -> "OPEN"
                        },
                        occurredAt = saleDate,
                        lines = if (mode == "Itemized") {
                            lineItems.map { (name, price) ->
                                OperatingLineBody(itemName = name, quantity = "1", unit = "nos", unitPrice = price)
                            }
                        } else null,
                        contract = OperatingContractBody(
                            entryMode = if (mode == "Itemized") "ITEMIZED" else "QUICK_AMOUNT",
                            paymentState = payState.uppercase(),
                            taxMode = "UNAVAILABLE",
                        ),
                    ),
                )
            },
        )
    }
    MfgDatePickerModal(
        selectedIso = saleDate,
        visible = showDatePicker,
        onDismiss = { showDatePicker = false },
        onConfirm = { saleDate = it },
    )
}

@Composable
private fun MfgPurchaseSheet(model: OperatingProjectionDto, onDismiss: () -> Unit, onSubmit: (ManufacturingEventDraft) -> Unit) {
    val currency = model.currency ?: "INR"
    var kind by remember { mutableStateOf("Materials") }
    var amount by remember { mutableStateOf("") }
    var supplier by remember { mutableStateOf("") }
    var receiveNow by remember { mutableStateOf("Receive now") }
    var pay by remember { mutableStateOf("Pay later") }
    var moreOpen by remember { mutableStateOf(false) }
    var prefilledMaterial by remember { mutableStateOf<String?>(null) }
    var prefilledMasterId by remember { mutableStateOf<String?>(null) }
    val vendors = model.masters.orEmpty().filter {
        it.kind.equals("SUPPLIER", true) || it.kind.equals("VENDOR", true)
    }
    val postable = amount.toBigDecimalOrNull()?.signum() == 1 && supplier.isNotBlank()
    LaunchedEffect(OperatingChromeState.browserSelection?.id) {
        val selection = OperatingChromeState.browserSelection ?: return@LaunchedEffect
        if (!selection.captureHint.equals("PURCHASE", ignoreCase = true)) return@LaunchedEffect
        prefilledMaterial = selection.name
        prefilledMasterId = selection.id.takeUnless { it.startsWith("local-") || it.startsWith("seed-") }
        selection.unitPrice?.takeIf { it.isNotBlank() && amount.isBlank() }?.let { amount = it }
        OperatingChromeState.browserSelection = null
    }
    mfgCaptureScaffold(
        title = "Record Purchase",
        subtitle = "$kind · GST on GRN only",
        onDismiss = onDismiss,
        why = "AP opens from posted purchase. Stock += accepted qty on receipt. Payment allocates later — never double-cost. GST posts on GRN/invoice only.",
    ) {
        MfgChipRow(null, listOf("Materials", "Consumables"), kind) { kind = it }
        MfgAmountHero("Purchase amount", if (amount.isBlank()) "₹0" else MfgFormat.exact(currency, amount), currencyBadge = currency)
        MfgNumericKeypad(amount) { amount = it }
        prefilledMaterial?.let { mat ->
            MfgPickerRow("Material from browser", mat, onClick = null, externalLabel = true)
        }
        MfgUnavailableGstRow(label = "Input GST · CGST+SGST")
        MfgPickerRow("Supplier", supplier.ifBlank { "Select vendor" }, onClick = {
            supplier = vendors.firstOrNull()?.name ?: supplier
        }, externalLabel = true)
        vendors.take(4).forEach { MfgPickerRow(it.name, it.gstin ?: "Vendor", onClick = { supplier = it.name }) }
        MfgChipRow("Receive", listOf("Receive now", "Receive later"), receiveNow) { receiveNow = it }
        MfgChipRow("Payment state", listOf("Pay later", "Pay now", "Part"), pay) { pay = it }
        MfgMoreDetails(expanded = moreOpen, onToggle = { moreOpen = !moreOpen }, hint = "Link job · Freight") {
            MfgTextField("", "Link job / WO (optional)", onValueChange = {})
        }
        MfgStickyFooter(label = "Post purchase", enabled = postable, caveat = "Draft preserved if you close or open a picker", onClick = {
            onSubmit(
                ManufacturingEventDraft(
                    eventType = "PURCHASE",
                    title = "Purchase · $supplier",
                    amount = amount,
                    itemName = prefilledMaterial,
                    masterId = prefilledMasterId,
                    note = kind,
                    channelName = null,
                    counterpartyName = supplier,
                    paymentStatus = when (pay) {
                        "Pay now" -> "PAID"
                        "Part" -> "PARTIAL"
                        else -> "OPEN"
                    },
                    contract = OperatingContractBody(
                        receiptState = if (receiveNow == "Receive now") "RECEIVED" else "PENDING",
                        paymentState = pay.uppercase(),
                        taxMode = "UNAVAILABLE",
                    ),
                ),
            )
        })
    }
}

@Composable
private fun MfgCollectionSheet(model: OperatingProjectionDto, onDismiss: () -> Unit, onSubmit: (ManufacturingEventDraft) -> Unit) {
    val currency = model.currency ?: "INR"
    var amount by remember { mutableStateOf("") }
    var customer by remember { mutableStateOf("") }
    var method by remember { mutableStateOf("UPI") }
    var source by remember { mutableStateOf("") }
    var reference by remember { mutableStateOf("") }
    var receivedOn by remember { mutableStateOf(LocalDate.now().toString()) }
    var moreOpen by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    val openAr = model.paymentReadiness.filter { it.relevance == "RECEIVABLE" }
    val parties = model.masters.orEmpty().filter { it.kind.equals("CUSTOMER", true) || it.kind.equals("BUYER", true) }
    val postable = amount.toBigDecimalOrNull()?.signum() == 1 && customer.isNotBlank()
    mfgCaptureScaffold(
        title = "Record Collection",
        subtitle = "Allocate AR · never new SALE",
        onDismiss = onDismiss,
        why = "Collection allocates against open invoices only — it never creates a new SALE or revenue line.",
    ) {
        MfgAmountHero(
            "Collect now",
            if (amount.isBlank()) "₹0" else MfgFormat.exact(currency, amount),
            caption = source.takeIf { it.isNotBlank() }?.let { "Against $it" },
            currencyBadge = currency,
        )
        MfgNumericKeypad(amount) { amount = it }
        MfgPickerRow("Customer", customer.ifBlank { "Select" }, onClick = {
            customer = parties.firstOrNull()?.name ?: openAr.firstOrNull()?.counterpartyName.orEmpty()
        }, externalLabel = true)
        parties.take(3).forEach { MfgPickerRow(it.name, "AR", onClick = { customer = it.name }) }
        MfgPickerRow(
            "Source invoice",
            source.ifBlank { if (openAr.isEmpty()) "Unavailable · no open AR" else "Select open invoice" },
            onClick = { openAr.firstOrNull()?.let { source = it.title; customer = it.counterpartyName ?: customer } },
            externalLabel = true,
        )
        openAr.take(4).forEach { row ->
            MfgPickerRow(row.title, MfgFormat.exact(currency, row.amount), onClick = {
                source = row.title
                customer = row.counterpartyName ?: customer
            })
        }
        MfgChipRow("Method", listOf("UPI", "NEFT", "Cash", "Cheque"), method) { method = it }
        MfgTextField(reference, "Reference / UTR", onValueChange = { reference = it })
        MfgMoreDetails(expanded = moreOpen, onToggle = { moreOpen = !moreOpen }, hint = "Notes · Attachment · Tags") {
            MfgTextField(note, "Notes", onValueChange = { note = it }, singleLine = false)
        }
        MfgPickerRow("Received on", MfgFormat.day(receivedOn), onClick = { showDatePicker = true }, externalLabel = true)
        MfgStickyFooter(label = "Post collection", enabled = postable, caveat = "Draft preserved if you close or open a picker", onClick = {
            val alloc = openAr.firstOrNull { it.title == source }
            onSubmit(
                ManufacturingEventDraft(
                    eventType = "COLLECTION",
                    title = "Collection · $customer",
                    amount = amount,
                    note = listOfNotNull(
                        source.takeIf { it.isNotBlank() },
                        method,
                        reference.takeIf { it.isNotBlank() }?.let { "ref=$it" },
                        note.takeIf { it.isNotBlank() },
                    ).joinToString(" · "),
                    channelName = method,
                    counterpartyName = customer,
                    paymentStatus = "PENDING",
                    occurredAt = receivedOn,
                    parentEventId = alloc?.id,
                    contract = OperatingContractBody(
                        allocations = alloc?.let { listOf(OperatingAllocationBody(it.id, amount)) },
                    ),
                ),
            )
        })
    }
    MfgDatePickerModal(
        selectedIso = receivedOn,
        visible = showDatePicker,
        onDismiss = { showDatePicker = false },
        onConfirm = { receivedOn = it },
    )
}

@Composable
private fun MfgPaymentSheet(
    model: OperatingProjectionDto,
    onDismiss: () -> Unit,
    onSubmit: (ManufacturingEventDraft) -> Unit,
    reimburse: Boolean,
) {
    val currency = model.currency ?: "INR"
    var amount by remember { mutableStateOf("") }
    var payee by remember { mutableStateOf("") }
    var method by remember { mutableStateOf("NEFT") }
    var reference by remember { mutableStateOf("") }
    val openAp = model.paymentReadiness.filter { it.relevance != "RECEIVABLE" }
    val vendors = model.masters.orEmpty().filter { it.kind.equals("SUPPLIER", true) || it.kind.equals("VENDOR", true) }
    val postable = amount.toBigDecimalOrNull()?.signum() == 1 && payee.isNotBlank()
    mfgCaptureScaffold(
        title = if (reimburse) "Approval / Reimbursement" else "Record Payment",
        subtitle = if (reimburse) "Approval ≠ disbursement" else "Settle AP · never new PURCHASE",
        onDismiss = onDismiss,
        why = if (reimburse) "Submit for approval — approval is not disbursement. Payment posts separately when paid."
        else "Payment allocates against open AP only — it never creates a new PURCHASE.",
    ) {
        if (reimburse) MfgChipRow(null, listOf("Approval", "Reimbursement"), "Reimbursement") {}
        MfgAmountHero(if (reimburse) "Claim amount" else "Paid", if (amount.isBlank()) "₹0" else MfgFormat.exact(currency, amount), currencyBadge = currency)
        MfgNumericKeypad(amount) { amount = it }
        MfgPickerRow("Payee", payee.ifBlank { "Select" }, onClick = {
            payee = vendors.firstOrNull()?.name ?: openAp.firstOrNull()?.counterpartyName.orEmpty()
        }, externalLabel = true)
        vendors.take(3).forEach { MfgPickerRow(it.name, "AP", onClick = { payee = it.name }) }
        openAp.take(3).forEach { MfgPickerRow(it.title, MfgFormat.exact(currency, it.amount), onClick = {
            payee = it.counterpartyName ?: payee
        }) }
        MfgChipRow("Method", listOf("NEFT", "UPI", "Cash", "Cheque"), method) { method = it }
        MfgTextField(reference, "Reference / UTR", onValueChange = { reference = it })
        MfgPickerRow("Paid on", MfgFormat.todayLine(), onClick = null, externalLabel = true)
        MfgStickyFooter(label = if (reimburse) "Submit for approval" else "Post payment", enabled = postable, caveat = "Draft preserved if you close or open a picker", onClick = {
            onSubmit(
                ManufacturingEventDraft(
                    eventType = "PAYMENT",
                    title = if (reimburse) "Reimburse · $payee" else "Payment · $payee",
                    amount = amount,
                    note = listOfNotNull(method, reference.takeIf { it.isNotBlank() }?.let { "ref=$it" }).joinToString(" · "),
                    channelName = method,
                    counterpartyName = payee,
                    paymentStatus = "PENDING",
                ),
            )
        })
    }
}

@Composable
private fun MfgExpenseSheet(model: OperatingProjectionDto, onDismiss: () -> Unit, onSubmit: (ManufacturingEventDraft) -> Unit) {
    val currency = model.currency ?: "INR"
    var amount by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("Utilities") }
    var payee by remember { mutableStateOf("") }
    var period by remember { mutableStateOf(java.time.YearMonth.now().toString()) }
    var showMonth by remember { mutableStateOf(false) }
    val postable = amount.toBigDecimalOrNull()?.signum() == 1
    mfgCaptureScaffold(
        title = "Record Expense",
        subtitle = "Type · Payee · Job link · Period",
        onDismiss = onDismiss,
        why = "Expense posts spend for the period. Link a job when cost should follow the WO — GST only if a tax invoice is attached server-side.",
    ) {
        MfgChipRow("Type", listOf("Utilities", "Freight", "Consumables", "Job work", "Other"), type) { type = it }
        MfgAmountHero("Expense", if (amount.isBlank()) "₹0" else MfgFormat.exact(currency, amount), currencyBadge = currency)
        MfgNumericKeypad(amount) { amount = it }
        MfgTextField(payee, "Payee", onValueChange = { payee = it })
        MfgPickerRow("Period", mfgMonthLabel(period), onClick = { showMonth = true }, externalLabel = true)
        MfgMonthPicker(period) { period = it }
        MfgStickyFooter(label = "Post expense", enabled = postable, caveat = "Draft preserved if you close or open a picker", onClick = {
            onSubmit(
                ManufacturingEventDraft(
                    eventType = "SPEND",
                    title = "Expense · $type",
                    amount = amount,
                    note = "period=$period",
                    channelName = null,
                    counterpartyName = payee.ifBlank { null },
                    paymentStatus = null,
                    contract = OperatingContractBody(expenseCategory = type),
                ),
            )
        })
    }
    MfgMonthPickerModal(
        selectedYm = period,
        visible = showMonth,
        onDismiss = { showMonth = false },
        onConfirm = { period = it },
    )
}

@Composable
private fun MfgReturnSheet(model: OperatingProjectionDto, onDismiss: () -> Unit, onSubmit: (ManufacturingEventDraft) -> Unit) {
    val currency = model.currency ?: "INR"
    var side by remember { mutableStateOf("Customer return") }
    var amount by remember { mutableStateOf("") }
    var party by remember { mutableStateOf("") }
    var restock by remember { mutableStateOf("Restock") }
    val postable = amount.toBigDecimalOrNull()?.signum() == 1 && party.isNotBlank()
    mfgCaptureScaffold(
        title = "Return / Credit Note",
        subtitle = "$side · credit note",
        onDismiss = onDismiss,
        why = "Credit note adjusts the source invoice. Restock is explicit — never silent FG overwrite.",
    ) {
        MfgChipRow(null, listOf("Customer return", "Supplier return"), side) { side = it }
        MfgAmountHero("Credit amount", if (amount.isBlank()) "₹0" else MfgFormat.exact(currency, amount), currencyBadge = currency)
        MfgNumericKeypad(amount) { amount = it }
        MfgTextField(party, "Party", onValueChange = { party = it })
        MfgChipRow("Restock", listOf("Restock", "Scrap", "No stock"), restock) { restock = it }
        MfgStickyFooter(label = "Post credit note", enabled = postable, caveat = "Draft preserved if you close or open a picker", onClick = {
            onSubmit(
                ManufacturingEventDraft(
                    eventType = "RETURN",
                    title = "Return · $party",
                    amount = amount,
                    note = "$side · $restock",
                    channelName = null,
                    counterpartyName = party,
                    paymentStatus = null,
                    contract = OperatingContractBody(stockAction = restock.uppercase()),
                ),
            )
        })
    }
}

@Composable
private fun MfgStockSheet(
    model: OperatingProjectionDto,
    onDismiss: () -> Unit,
    onSubmit: (ManufacturingEventDraft) -> Unit,
    onNavigateUtility: ((MfgUtilityNav) -> Unit)?,
) {
    var action by remember { mutableStateOf("Set current") }
    var product by remember { mutableStateOf("") }
    var qty by remember { mutableStateOf("") }
    var location by remember { mutableStateOf(model.location ?: "Raw store") }
    var reason by remember { mutableStateOf("Cycle count") }
    val skus = model.masters.orEmpty().filter {
        it.kind.equals("SKU", true) || it.kind.equals("PART", true) || it.kind.equals("PRODUCT", true)
    }
    val postable = product.isNotBlank() && qty.toBigDecimalOrNull() != null
    mfgCaptureScaffold(
        title = "Stock Update",
        subtitle = "Explicit movement only",
        onDismiss = onDismiss,
        why = "Stock updates are explicit Set / Add / Reduce / Transfer movements — never inferred from sale or WO save.",
    ) {
        MfgChipRow(null, listOf("Set current", "Add", "Reduce", "Transfer"), action) { next ->
            if (next == "Transfer") {
                onNavigateUtility?.invoke(MfgUtilityNav.TRANSFER) ?: run { action = next }
            } else {
                action = next
            }
        }
        MfgTextField(product, "Product / part", onValueChange = { product = it })
        skus.take(4).forEach { MfgPickerRow(it.name, it.unit ?: "nos", onClick = { product = it.name }) }
        MfgTextField(qty, "Qty", onValueChange = { qty = it.filter { ch -> ch.isDigit() || ch == '.' }.take(10) })
        MfgPickerRow("Location", location, onClick = null, externalLabel = true)
        MfgChipRow("Reason", listOf("Cycle count", "Damage", "Correction", "Custody"), reason) { reason = it }
        MfgStickyFooter(label = "Post stock update", enabled = postable, caveat = "Draft preserved if you close or open a picker", onClick = {
            onSubmit(
                ManufacturingEventDraft(
                    eventType = "STOCK",
                    title = "Stock · $action · $product",
                    amount = null,
                    note = reason,
                    channelName = location,
                    counterpartyName = null,
                    paymentStatus = null,
                    quantity = qty,
                    itemName = product,
                    contract = OperatingContractBody(stockAction = action.uppercase(), stockReason = reason),
                ),
            )
        })
    }
}

@Composable
private fun MfgBudgetSheet(model: OperatingProjectionDto, onDismiss: () -> Unit, onSubmit: (ManufacturingEventDraft) -> Unit, target: Boolean) {
    val currency = model.currency ?: "INR"
    var basis by remember { mutableStateOf(if (target) "Accepted units" else "Sales basis") }
    var amount by remember { mutableStateOf("") }
    var period by remember { mutableStateOf(java.time.YearMonth.now().toString()) }
    var showMonth by remember { mutableStateOf(false) }
    val postable = amount.toBigDecimalOrNull()?.signum() == 1
    mfgCaptureScaffold(
        title = "Budget / Target Revision",
        subtitle = "Versioned · effective-dated",
        onDismiss = onDismiss,
        why = "Revisions are versioned and effective-dated. They do not rewrite posted production or sales history.",
    ) {
        MfgChipRow(null, listOf("Sales basis", "Accepted units"), basis) { basis = it }
        MfgAmountHero("New target", if (amount.isBlank()) "—" else if (basis == "Accepted units") amount else MfgFormat.exact(currency, amount), currencyBadge = if (basis == "Accepted units") "units" else currency)
        MfgNumericKeypad(amount) { amount = it }
        MfgPickerRow("Period", mfgMonthLabel(period), onClick = { showMonth = true }, externalLabel = true)
        MfgMonthPicker(period) { period = it }
        MfgStickyFooter(label = "Save revision", enabled = postable, caveat = "Draft preserved if you close or open a picker", onClick = {
            onSubmit(
                ManufacturingEventDraft(
                    eventType = if (target || basis == "Accepted units") "TARGET" else "BUDGET",
                    title = "Revision · $period",
                    amount = if (basis == "Accepted units") null else amount,
                    note = "basis=$basis · period=$period",
                    channelName = null,
                    counterpartyName = null,
                    paymentStatus = null,
                    quantity = if (basis == "Accepted units") amount else null,
                ),
            )
        })
    }
    MfgMonthPickerModal(
        selectedYm = period,
        visible = showMonth,
        onDismiss = { showMonth = false },
        onConfirm = { period = it },
    )
}

@Composable
private fun MfgIssueSheet(model: OperatingProjectionDto, onDismiss: () -> Unit, onSubmit: (ManufacturingEventDraft) -> Unit, issue: Boolean) {
    var kind by remember { mutableStateOf(if (issue) "Issue" else "Update") }
    var title by remember { mutableStateOf("") }
    var severity by remember { mutableStateOf("Medium") }
    mfgCaptureScaffold(
        title = "Post Update / Issue",
        subtitle = "Plant signal · not a money event",
        onDismiss = onDismiss,
        why = "Updates and issues are plant signals. They do not post revenue, stock, or GST by themselves.",
    ) {
        MfgChipRow(null, listOf("Update", "Issue", "Risk", "Approval"), kind) { kind = it }
        MfgTextField(title, "Title", onValueChange = { title = it })
        MfgChipRow("Severity", listOf("Low", "Medium", "High"), severity) { severity = it }
        MfgStickyFooter(label = "Post update", enabled = title.isNotBlank(), caveat = "Draft preserved if you close or open a picker", onClick = {
            onSubmit(
                ManufacturingEventDraft(
                    eventType = if (kind == "Issue" || kind == "Risk") "ISSUE" else "UPDATE",
                    title = title,
                    amount = null,
                    note = "severity=$severity",
                    channelName = null,
                    counterpartyName = null,
                    paymentStatus = null,
                ),
            )
        })
    }
}

@Composable
private fun MfgApprovalSheet(model: OperatingProjectionDto, onDismiss: () -> Unit, onSubmit: (ManufacturingEventDraft) -> Unit) {
    var mode by remember { mutableStateOf("Approval") }
    var actionTitle by remember { mutableStateOf("") }
    var requester by remember { mutableStateOf("") }
    var approver by remember { mutableStateOf("") }
    var policy by remember { mutableStateOf("Finance ≥ ₹1L") }
    var moreOpen by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf("") }
    var decideBy by remember { mutableStateOf(LocalDate.now().plusDays(2).toString()) }
    var showDatePicker by remember { mutableStateOf(false) }
    val people = model.participants.filter { it.isNotBlank() }
    mfgCaptureScaffold(
        title = "Approval / Reimbursement",
        subtitle = "Approval ≠ disbursement",
        onDismiss = onDismiss,
        why = "Approval ≠ disbursement. Approver cannot approve own controlled action. Money moves only when a Payment is posted.",
    ) {
        MfgChipRow(null, listOf("Approval", "Reimbursement"), mode) { mode = it }
        MfgTextField(actionTitle, "Action", onValueChange = { actionTitle = it })
        MfgTextField(requester, "Requester", onValueChange = { requester = it })
        people.take(3).forEach { MfgPickerRow(it, "Team", onClick = { requester = it }) }
        MfgPickerRow(
            "Approver",
            approver.ifBlank { people.firstOrNull { it != requester } ?: "Select approver" },
            onClick = {
                approver = people.firstOrNull { it != requester } ?: approver
            },
            externalLabel = true,
        )
        people.filter { it != requester }.take(3).forEach {
            MfgPickerRow(it, "Approver", onClick = { approver = it })
        }
        MfgChipRow("Policy", listOf("Finance ≥ ₹1L", "Manager any", "Owner only"), policy) { policy = it }
        MfgMoreDetails(expanded = moreOpen, onToggle = { moreOpen = !moreOpen }, hint = "Notes · Attachment · Tags") {
            MfgTextField(note, "Notes", onValueChange = { note = it }, singleLine = false)
        }
        MfgPickerRow("Decide by", MfgFormat.day(decideBy), onClick = { showDatePicker = true }, externalLabel = true)
        MfgDateQuickPicker(decideBy) { decideBy = it }
        MfgStickyFooter(
            label = "Submit for approval",
            enabled = requester.isNotBlank() && actionTitle.isNotBlank(),
            caveat = "Draft preserved if you close or open a picker",
            onClick = {
                onSubmit(
                    ManufacturingEventDraft(
                        eventType = "UPDATE",
                        title = "$mode · ${actionTitle.ifBlank { requester }}",
                        amount = null,
                        note = listOfNotNull(
                            "APPROVAL_REQUEST",
                            "approver=${approver.ifBlank { "Unset" }}",
                            "policy=$policy",
                            "decideBy=$decideBy",
                            note.takeIf { it.isNotBlank() },
                        ).joinToString(" · "),
                        channelName = policy,
                        counterpartyName = requester,
                        paymentStatus = null,
                        dueAt = decideBy,
                    ),
                )
            },
        )
    }
    MfgDatePickerModal(
        selectedIso = decideBy,
        visible = showDatePicker,
        onDismiss = { showDatePicker = false },
        onConfirm = { decideBy = it },
    )
}

@Composable
private fun MfgOrderSheet(model: OperatingProjectionDto, onDismiss: () -> Unit, onSubmit: (ManufacturingEventDraft) -> Unit) {
    var kind by remember { mutableStateOf("Work order") }
    var party by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var prefilledPart by remember { mutableStateOf<String?>(null) }
    var prefilledMasterId by remember { mutableStateOf<String?>(null) }
    var prefilledUnit by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(OperatingChromeState.browserSelection?.id) {
        val selection = OperatingChromeState.browserSelection ?: return@LaunchedEffect
        if (!selection.captureHint.equals("ORDER", ignoreCase = true)) return@LaunchedEffect
        prefilledPart = selection.name
        prefilledMasterId = selection.id.takeUnless { it.startsWith("local-") || it.startsWith("seed-") }
        prefilledUnit = selection.unit
        if (title.isBlank()) title = selection.name
        when (selection.browserKind) {
            com.example.momentra.ui.shell.business.shared.OperatingBrowserKind.Materials -> kind = "Purchase order"
            else -> if (kind == "Work order" || kind.isBlank()) kind = "Sales order"
        }
        OperatingChromeState.browserSelection = null
    }
    mfgCaptureScaffold(
        title = "Order",
        subtitle = "Commitment only · no revenue / stock / GST",
        onDismiss = onDismiss,
        why = "Orders are commitments only — SO / PO / WO / Delivery promises. Revenue, stock, and GST post on Sale, receipt, or accepted production.",
    ) {
        MfgChipRow(null, listOf("Sales order", "Purchase order", "Work order", "Delivery"), kind) { kind = it }
        MfgAmountHero("Commitment", title.ifBlank { "Untitled $kind" }, caption = "No revenue · no stock · no GST on save", currencyBadge = "Promise")
        MfgTextField(title, "Title", onValueChange = { title = it })
        prefilledPart?.let { part ->
            MfgPickerRow(
                "Part from browser",
                listOfNotNull(part, prefilledUnit).joinToString(" · "),
                onClick = null,
                externalLabel = true,
            )
        }
        MfgTextField(party, "Party / buyer / vendor", onValueChange = { party = it })
        MfgUnavailableGstRow(label = "GST on commitment", caption = "Est. on invoice/GRN only — Unavailable here")
        MfgStickyFooter(label = "Save order", enabled = title.isNotBlank(), caveat = "Draft preserved if you close or open a picker", onClick = {
            onSubmit(
                ManufacturingEventDraft(
                    eventType = "ORDER",
                    title = title.ifBlank { kind },
                    amount = null,
                    note = listOfNotNull(
                        "commitment only · $kind",
                        prefilledPart?.let { "part=$it" },
                    ).joinToString(" · "),
                    channelName = null,
                    counterpartyName = party.ifBlank { null },
                    paymentStatus = null,
                    itemName = prefilledPart,
                    masterId = prefilledMasterId,
                    unit = prefilledUnit,
                    quantity = prefilledPart?.let { "1" },
                    lines = prefilledPart?.let { name ->
                        listOf(
                            OperatingLineBody(
                                itemName = name,
                                quantity = "1",
                                unit = prefilledUnit ?: "nos",
                                masterId = prefilledMasterId,
                            ),
                        )
                    },
                    contract = OperatingContractBody(
                        orderStatus = "OPEN",
                        entryMode = kind.uppercase().replace(' ', '_'),
                    ),
                ),
            )
        })
    }
}

@Composable
private fun MfgDeliverySheet(model: OperatingProjectionDto, onDismiss: () -> Unit, onSubmit: (ManufacturingEventDraft) -> Unit) {
    var direction by remember { mutableStateOf("Outgoing dispatch") }
    var source by remember { mutableStateOf("") }
    var qty by remember { mutableStateOf("") }
    var dispatchSlot by remember { mutableStateOf("Morning · 09:00") }
    var dispatchIso by remember { mutableStateOf(LocalDate.now().toString()) }
    var showDateTime by remember { mutableStateOf(false) }
    val postable = source.isNotBlank()
    mfgCaptureScaffold(
        title = "Delivery",
        subtitle = "Incoming receipt · Outgoing dispatch",
        onDismiss = onDismiss,
        why = "Delivery confirms movement against a commitment. It does not create a duplicate invoice or revenue.",
    ) {
        MfgChipRow(null, listOf("Incoming receipt", "Outgoing dispatch"), direction) { direction = it }
        MfgTextField(source, "Source SO / PO / WO", onValueChange = { source = it })
        MfgTextField(qty, "Qty confirming", onValueChange = { qty = it.filter { ch -> ch.isDigit() || ch == '.' }.take(10) })
        MfgPickerRow(
            "Dispatch on",
            "${MfgFormat.day(dispatchIso)} · $dispatchSlot",
            onClick = { showDateTime = true },
            externalLabel = true,
        )
        MfgDateTimeSlots(dispatchSlot) { dispatchSlot = it }
        MfgStickyFooter(label = "Post delivery", enabled = postable, caveat = "Draft preserved if you close or open a picker", onClick = {
            onSubmit(
                ManufacturingEventDraft(
                    eventType = "DELIVERY",
                    title = "Delivery · $source",
                    amount = null,
                    note = "$direction · $dispatchIso · $dispatchSlot",
                    channelName = null,
                    counterpartyName = null,
                    paymentStatus = null,
                    quantity = qty.ifBlank { null },
                    itemName = source,
                    dueAt = dispatchIso,
                    occurredAt = dispatchIso,
                ),
            )
        })
    }
    MfgDateTimePickerModal(
        selectedSlot = dispatchSlot,
        selectedIso = dispatchIso,
        visible = showDateTime,
        onDismiss = { showDateTime = false },
        onConfirm = { iso, slot ->
            dispatchIso = iso
            dispatchSlot = slot
        },
    )
}

@Composable
private fun MfgProductionSheet(
    model: OperatingProjectionDto,
    onDismiss: () -> Unit,
    onSubmit: (ManufacturingEventDraft) -> Unit,
    onNavigateUtility: ((MfgUtilityNav) -> Unit)?,
) {
    var mode by remember { mutableStateOf("Batch/run") }
    var product by remember { mutableStateOf("") }
    var accepted by remember { mutableStateOf("") }
    var rejected by remember { mutableStateOf("0") }
    var batch by remember { mutableStateOf("") }
    var scrapReason by remember { mutableStateOf("") }
    var materials by remember { mutableStateOf("Explicit consumption") }
    var moreOpen by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf("") }
    var producedOn by remember { mutableStateOf(LocalDate.now().toString()) }
    var showDatePicker by remember { mutableStateOf(false) }
    val skus = model.masters.orEmpty().filter {
        it.kind.equals("SKU", true) || it.kind.equals("PART", true) || it.kind.equals("PRODUCT", true)
    }
    val postable = product.isNotBlank() && accepted.toBigDecimalOrNull()?.signum() == 1
    mfgCaptureScaffold(
        title = "Record Production",
        subtitle = "Accepted ≠ rejected · accepted only sellable",
        onDismiss = onDismiss,
        why = "Only accepted qty becomes sellable FG. Rejected and rework are distinct — never silent overwrite of finished stock.",
    ) {
        MfgChipRow(null, listOf("Batch/run", "MTO job", "Rework", "Hold WO"), mode) {
            if (it == "Hold WO") {
                onNavigateUtility?.invoke(MfgUtilityNav.HOLD) ?: run { mode = it }
            } else {
                mode = it
            }
        }
        MfgTextField(product, "Finished product", onValueChange = { product = it })
        skus.take(4).forEach { MfgPickerRow(it.name, it.unit ?: "nos", onClick = { product = it.name }) }
        MfgTextField(batch, "Batch / heat", onValueChange = { batch = it })
        MfgFieldLabel("Accepted qty")
        MfgTextField(accepted, "0", onValueChange = { accepted = it.filter { ch -> ch.isDigit() || ch == '.' }.take(10) })
        MfgFieldLabel("Rejected qty")
        MfgTextField(rejected, "0", onValueChange = { rejected = it.filter { ch -> ch.isDigit() || ch == '.' }.take(10) })
        MfgTextField(scrapReason, "Scrap reason", onValueChange = { scrapReason = it })
        MfgChipRow(
            "Materials",
            listOf("Explicit consumption", "Backflush", "None"),
            materials,
        ) { materials = it }
        MfgMoreDetails(expanded = moreOpen, onToggle = { moreOpen = !moreOpen }, hint = "Notes · Attachment · Tags") {
            MfgTextField(note, "Notes", onValueChange = { note = it }, singleLine = false)
        }
        MfgPickerRow("Produced on", MfgFormat.day(producedOn), onClick = { showDatePicker = true }, externalLabel = true)
        MfgDateQuickPicker(producedOn) { producedOn = it }
        MfgStickyFooter(label = "Post production", enabled = postable, caveat = "Draft preserved if you close or open a picker", onClick = {
            onSubmit(
                ManufacturingEventDraft(
                    eventType = "PRODUCTION",
                    title = "Production · $product",
                    amount = null,
                    note = listOfNotNull(
                        "mode=$mode",
                        "batch=$batch",
                        "materials=$materials",
                        scrapReason.takeIf { it.isNotBlank() }?.let { "scrap=$it" },
                        note.takeIf { it.isNotBlank() },
                    ).joinToString(" · "),
                    channelName = materials,
                    counterpartyName = null,
                    paymentStatus = null,
                    quantity = accepted,
                    rejectedQuantity = rejected,
                    itemName = product,
                    unit = "nos",
                    occurredAt = producedOn,
                    contract = OperatingContractBody(
                        productionStatus = mode.uppercase().replace(' ', '_'),
                        scrapQuantity = rejected.takeIf { it.toBigDecimalOrNull()?.signum() == 1 },
                        acceptedQuantity = accepted,
                    ),
                ),
            )
        })
    }
    MfgDatePickerModal(
        selectedIso = producedOn,
        visible = showDatePicker,
        onDismiss = { showDatePicker = false },
        onConfirm = { producedOn = it },
    )
}

@Composable
private fun MfgReworkSheet(model: OperatingProjectionDto, onDismiss: () -> Unit, onSubmit: (ManufacturingEventDraft) -> Unit) {
    var action by remember { mutableStateOf("Rework") }
    var source by remember { mutableStateOf("") }
    var qty by remember { mutableStateOf("") }
    var vendor by remember { mutableStateOf("") }
    var expectedReturn by remember { mutableStateOf(LocalDate.now().plusDays(7).toString()) }
    val postable = source.isNotBlank() && qty.toBigDecimalOrNull()?.signum() == 1
    mfgCaptureScaffold(
        title = "Rework / Scrap / Job Work",
        subtitle = "Distinct events · no silent FG overwrite",
        onDismiss = onDismiss,
        why = "Rework, scrap, and job work out are distinct events. They never silently overwrite accepted finished goods.",
    ) {
        MfgChipRow(null, listOf("Rework", "Scrap", "Job work out"), action) { action = it }
        MfgTextField(source, "Source batch / WO", onValueChange = { source = it })
        MfgTextField(qty, "Qty", onValueChange = { qty = it.filter { ch -> ch.isDigit() || ch == '.' }.take(10) })
        if (action == "Job work out") {
            MfgTextField(vendor, "Vendor", onValueChange = { vendor = it })
            MfgLockedChip("Challan · Unavailable")
            MfgPickerRow("Expected return", MfgFormat.day(expectedReturn), onClick = null, externalLabel = true)
            MfgDateQuickPicker(expectedReturn) { expectedReturn = it }
        }
        MfgStickyFooter(label = "Post action", enabled = postable, caveat = "Draft preserved if you close or open a picker", onClick = {
            onSubmit(
                ManufacturingEventDraft(
                    eventType = "STOCK",
                    title = "$action · $source",
                    amount = null,
                    note = "REWORK_FAMILY · $action" + if (action == "Job work out") " · return=$expectedReturn" else "",
                    channelName = null,
                    counterpartyName = vendor.ifBlank { null },
                    paymentStatus = null,
                    quantity = qty,
                    itemName = source,
                    dueAt = if (action == "Job work out") expectedReturn else null,
                    contract = OperatingContractBody(stockAction = action.uppercase().replace(' ', '_')),
                ),
            )
        })
    }
}

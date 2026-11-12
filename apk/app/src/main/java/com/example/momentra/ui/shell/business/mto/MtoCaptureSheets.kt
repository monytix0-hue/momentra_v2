package com.example.momentra.ui.shell.business.mto
import com.example.momentra.ui.shell.business.manufacturing.ManufacturingEventDraft

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.momentra.data.api.OperatingAllocationBody
import com.example.momentra.data.api.OperatingContractBody
import com.example.momentra.data.api.OperatingProjectionDto
import com.example.momentra.ui.shell.business.shared.OperatingChromeState
import com.example.momentra.ui.theme.MadeToOrderAmber
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private fun mtoMonthLabel(ym: String): String {
    val parsed = runCatching { YearMonth.parse(ym.take(7)) }.getOrElse { YearMonth.now() }
    return parsed.format(DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH))
}

@Composable
internal fun MtoCaptureSheet(
    mode: String,
    model: OperatingProjectionDto,
    onDismiss: () -> Unit,
    onSubmit: (ManufacturingEventDraft) -> Unit,
    onNavigateUtility: ((MtoUtilityNav) -> Unit)? = null,
) {
    when (mode.uppercase()) {
        "SALE", "INVOICE" -> MtoInvoiceSheet(model, onDismiss, onSubmit, onNavigateUtility)
        "PURCHASE", "MATERIALS" -> MtoMaterialsSheet(model, onDismiss, onSubmit)
        "COLLECTION" -> MtoCollectionSheet(model, onDismiss, onSubmit)
        "PAYMENT", "REIMBURSE" -> MtoPaymentSheet(model, onDismiss, onSubmit, reimburse = mode.equals("REIMBURSE", true))
        "SPEND", "EXPENSE" -> MtoExpenseSheet(model, onDismiss, onSubmit)
        "RETURN" -> MtoReturnSheet(model, onDismiss, onSubmit)
        "STOCK" -> MtoStockSheet(model, onDismiss, onSubmit, onNavigateUtility)
        "BUDGET", "TARGET" -> MtoBudgetSheet(model, onDismiss, onSubmit, target = mode.equals("TARGET", true))
        "UPDATE", "ISSUE" -> MtoIssueSheet(model, onDismiss, onSubmit, issue = mode.equals("ISSUE", true))
        "APPROVAL" -> MtoApprovalSheet(model, onDismiss, onSubmit)
        "ORDER", "CUSTOMER_ORDER", "QUOTATION", "QUOTE" -> MtoQuotationSheet(model, onDismiss, onSubmit)
        "CONFIRM", "ACCEPT", "CONFIRM_ORDER" -> MtoConfirmOrderSheet(model, onDismiss, onSubmit)
        "OPEN_JOB", "JOB", "OPENJOB" -> MtoOpenJobSheet(model, onDismiss, onSubmit, onNavigateUtility)
        "QUOTE_REVISE", "REVISE" -> MtoQuoteReviseSheet(model, onDismiss, onSubmit)
        "JOB_STATUS", "STATUS" -> MtoJobStatusSheet(model, onDismiss, onSubmit)
        "DELIVERY", "SHIPMENT", "DISPATCH" -> MtoDeliverySheet(model, onDismiss, onSubmit)
        "PRODUCTION" -> MtoProductionSheet(model, onDismiss, onSubmit, onNavigateUtility)
        "REWORK", "SCRAP", "REWORK_SCRAP" -> MtoReworkSheet(model, onDismiss, onSubmit)
        else -> MtoQuotationSheet(model, onDismiss, onSubmit)
    }
}

@Composable
private fun mtoCaptureScaffold(
    title: String,
    subtitle: String?,
    onDismiss: () -> Unit,
    why: String,
    content: @Composable () -> Unit,
) {
    MtoSheetBody(Modifier.verticalScroll(rememberScrollState())) {
        MtoGrabber()
        MtoSheetHeader(title = title, subtitle = subtitle, onClose = onDismiss)
        content()
        MtoWhyNote(why)
        Spacer(Modifier.height(16.dp))
    }
}

/** Figma `2686:31427` Invoice against order — From order | New sale | Return | Reprint. */
@Composable
private fun MtoInvoiceSheet(
    model: OperatingProjectionDto,
    onDismiss: () -> Unit,
    onSubmit: (ManufacturingEventDraft) -> Unit,
    onNavigateUtility: ((MtoUtilityNav) -> Unit)?,
) {
    val currency = model.currency ?: "INR"
    var mode by remember { mutableStateOf("From order") }
    var amount by remember { mutableStateOf("") }
    var customer by remember { mutableStateOf("") }
    var order by remember { mutableStateOf("") }
    var saleDate by remember { mutableStateOf(LocalDate.now().toString()) }
    var channel by remember { mutableStateOf("Wholesale") }
    var payState by remember { mutableStateOf("Partial") }
    var moreOpen by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    val parties = model.masters.orEmpty().filter {
        it.kind.equals("CUSTOMER", true) || it.kind.equals("BUYER", true) || it.kind.equals("PARTY", true)
    }
    val orders = model.activity.filter {
        it.eventType.equals("ORDER", true) ||
            it.eventType.equals("CUSTOMER_ORDER", true) ||
            it.eventType.equals("CONFIRM", true) ||
            it.eventType.equals("QUOTATION", true)
    }
    // Figma MTO-QA Invoice chips are From order | New sale | Return | Reprint only —
    // no Itemized mode. Keep GST Unavailable (never invent CGST/SGST math).
    val postable = amount.toBigDecimalOrNull()?.signum() == 1 && customer.isNotBlank()

    mtoCaptureScaffold(
        title = "Invoice against order",
        subtitle = null,
        onDismiss = onDismiss,
        why = "Only a posted invoice owns revenue, AR and output GST. Quotes, confirms, advances and dispatch never invent GST math here.",
    ) {
        MtoChipRow(null, listOf("From order", "New sale", "Return", "Reprint"), mode) { next ->
            when (next) {
                "Return" -> { mode = next }
                "Reprint" -> onNavigateUtility?.invoke(MtoUtilityNav.REPRINT) ?: run { mode = next }
                else -> mode = next
            }
        }
        when (mode) {
            "Return" -> {
                MtoChipRow("Return type", listOf("Customer return", "Supplier return"), "Customer return") {}
                MtoAmountHero("Credit amount", if (amount.isBlank()) "₹0" else MtoFormat.exact(currency, amount), currencyBadge = currency)
                MtoNumericKeypad(amount) { amount = it }
            }
            else -> {
                MtoAmountHero(
                    label = "Invoice gross",
                    amount = if (amount.isBlank()) "₹0" else MtoFormat.exact(currency, amount),
                    caption = listOfNotNull(
                        order.takeIf { it.isNotBlank() }?.let { "Order $it" },
                        customer.takeIf { it.isNotBlank() }?.let { "Customer $it" },
                        "INV draft",
                    ).joinToString(" · "),
                    currencyBadge = currency,
                )
                MtoNumericKeypad(amount) { amount = it }
            }
        }
        MtoUnavailableGstRow(label = "Output GST · CGST+SGST/IGST · Unavailable")
        MtoPickerRow("Order", order.ifBlank { "Select SO / confirmed order" }, onClick = {
            order = orders.firstOrNull()?.title ?: order.ifBlank { "SO · confirmed" }
        }, externalLabel = true)
        orders.take(4).forEach { MtoPickerRow(it.title, it.status ?: "Confirmed", onClick = { order = it.title }) }
        MtoPickerRow("Customer", customer.ifBlank { "Select buyer" }, onClick = {
            customer = parties.firstOrNull()?.name ?: customer.ifBlank { "Walk-in" }
        }, externalLabel = true)
        parties.take(4).forEach { MtoPickerRow(it.name, it.gstin ?: "Buyer", onClick = { customer = it.name }) }
        MtoPickerRow("Sale date", MtoFormat.day(saleDate), onClick = { showDatePicker = true }, externalLabel = true)
        MtoDateQuickPicker(saleDate) { saleDate = it }
        MtoChipRow("Channel", listOf("Wholesale", "Retail counter", "Export", "Job work"), channel) { channel = it }
        MtoChipRow("Payment state", listOf("Paid", "Partial", "Open"), payState) { payState = it }
        MtoPickerRow(
            "GST",
            "Unavailable · stamped on invoice (Est. from HSN master)",
            onClick = null,
            externalLabel = true,
        )
        MtoMoreDetails(expanded = moreOpen, onToggle = { moreOpen = !moreOpen }, hint = "Notes · Attachment · Tags") {
            MtoTextField(note, "Notes", onValueChange = { note = it }, singleLine = false)
        }
        MtoStickyFooter(
            label = "Post invoice",
            enabled = postable || (mode == "Return" && amount.toBigDecimalOrNull()?.signum() == 1),
            caveat = "Draft preserved if you close or open a picker",
            onClick = {
                val event = if (mode == "Return") "RETURN" else "SALE"
                onSubmit(
                    ManufacturingEventDraft(
                        eventType = event,
                        title = if (event == "RETURN") "Credit · $customer" else "Sale · $customer",
                        amount = amount,
                        note = listOfNotNull(
                            note.takeIf { it.isNotBlank() },
                            order.takeIf { it.isNotBlank() }?.let { "order=$it" },
                        ).joinToString(" · ").ifBlank { null },
                        channelName = channel,
                        counterpartyName = customer,
                        paymentStatus = when (payState) {
                            "Paid" -> "PAID"
                            "Partial" -> "PARTIAL"
                            else -> "OPEN"
                        },
                        occurredAt = saleDate,
                        itemName = order.ifBlank { null },
                        contract = OperatingContractBody(
                            entryMode = "QUICK_AMOUNT",
                            paymentState = payState.uppercase(),
                            taxMode = "UNAVAILABLE",
                        ),
                    ),
                )
            },
        )
    }
    MtoDatePickerModal(
        selectedIso = saleDate,
        visible = showDatePicker,
        onDismiss = { showDatePicker = false },
        onConfirm = { saleDate = it },
    )
}

@Composable
private fun MtoMaterialsSheet(model: OperatingProjectionDto, onDismiss: () -> Unit, onSubmit: (ManufacturingEventDraft) -> Unit) {
    val currency = model.currency ?: "INR"
    var kind by remember { mutableStateOf("Job materials") }
    var amount by remember { mutableStateOf("") }
    var supplier by remember { mutableStateOf("") }
    var receiveNow by remember { mutableStateOf("Receive now") }
    var pay by remember { mutableStateOf("Pay later") }
    var jobLink by remember { mutableStateOf("") }
    var moreOpen by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf("") }
    var prefilledMaterial by remember { mutableStateOf<String?>(null) }
    var prefilledMasterId by remember { mutableStateOf<String?>(null) }
    val vendors = model.masters.orEmpty().filter {
        it.kind.equals("SUPPLIER", true) || it.kind.equals("VENDOR", true)
    }
    val jobs = model.activity.filter {
        it.eventType.equals("UPDATE", true) ||
            it.eventType.equals("ORDER", true) ||
            it.eventType.equals("CUSTOMER_ORDER", true) ||
            it.eventType.equals("PRODUCTION", true)
    }
    val jobRequired = kind == "Job materials"
    val postable = amount.toBigDecimalOrNull()?.signum() == 1 &&
        supplier.isNotBlank() &&
        (!jobRequired || jobLink.isNotBlank())
    LaunchedEffect(OperatingChromeState.browserSelection?.id) {
        val selection = OperatingChromeState.browserSelection ?: return@LaunchedEffect
        if (!selection.captureHint.equals("PURCHASE", ignoreCase = true)) return@LaunchedEffect
        prefilledMaterial = selection.name
        prefilledMasterId = selection.id.takeUnless { it.startsWith("local-") || it.startsWith("seed-") }
        selection.unitPrice?.takeIf { it.isNotBlank() && amount.isBlank() }?.let { amount = it }
        OperatingChromeState.browserSelection = null
    }
    mtoCaptureScaffold(
        title = "Purchase job materials",
        subtitle = "$kind · GST on GRN only",
        onDismiss = onDismiss,
        why = "Job-linked: cost lands on the job, not general stock. PO alone is not AP; GRN books input GST; payment settles payable only.",
    ) {
        MtoChipRow(null, listOf("Job materials", "General stock"), kind) { kind = it }
        MtoAmountHero("Purchase amount", if (amount.isBlank()) "₹0" else MtoFormat.exact(currency, amount), currencyBadge = currency)
        MtoNumericKeypad(amount) { amount = it }
        prefilledMaterial?.let { mat ->
            MtoPickerRow("Material from browser", mat, onClick = null, externalLabel = true)
        }
        MtoUnavailableGstRow(label = "Input GST · CGST+SGST")
        MtoPickerRow("Supplier", supplier.ifBlank { "Select vendor" }, onClick = {
            supplier = vendors.firstOrNull()?.name ?: supplier
        }, externalLabel = true)
        vendors.take(4).forEach { MtoPickerRow(it.name, it.gstin ?: "Vendor", onClick = { supplier = it.name }) }
        MtoChipRow("Receive", listOf("Receive now", "Receive later"), receiveNow) { receiveNow = it }
        MtoChipRow("Payment state", listOf("Pay later", "Pay now", "Part"), pay) { pay = it }
        MtoPickerRow(
            if (jobRequired) "Job (required)" else "Link job",
            jobLink.ifBlank {
                when {
                    jobs.isEmpty() -> "Unavailable · no open job"
                    jobRequired -> "Select job"
                    else -> "Optional · select job"
                }
            },
            onClick = { jobs.firstOrNull()?.let { jobLink = it.title } },
            externalLabel = true,
        )
        jobs.take(4).forEach { MtoPickerRow(it.title, it.status ?: "Job", onClick = { jobLink = it.title }) }
        MtoMoreDetails(expanded = moreOpen, onToggle = { moreOpen = !moreOpen }, hint = "Freight · Notes · Tags") {
            MtoTextField(note, "Notes / freight", onValueChange = { note = it }, singleLine = false)
        }
        MtoStickyFooter(label = "Post purchase", enabled = postable, caveat = "Draft preserved if you close or open a picker", onClick = {
            onSubmit(
                ManufacturingEventDraft(
                    eventType = "PURCHASE",
                    title = "Purchase · $supplier",
                    amount = amount,
                    itemName = prefilledMaterial,
                    masterId = prefilledMasterId,
                    note = listOfNotNull(
                        kind,
                        jobLink.takeIf { it.isNotBlank() }?.let { "job=$it" },
                        note.takeIf { it.isNotBlank() },
                    ).joinToString(" · "),
                    channelName = jobLink.ifBlank { null },
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
private fun MtoCollectionSheet(model: OperatingProjectionDto, onDismiss: () -> Unit, onSubmit: (ManufacturingEventDraft) -> Unit) {
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
    mtoCaptureScaffold(
        title = "Collection / advance",
        subtitle = "Advance = liability until INV · allocate AR only",
        onDismiss = onDismiss,
        why = "Advance on an uninvoiced order is held as liability — never revenue. Against INV, collection allocates open AR only and never creates a new SALE.",
    ) {
        MtoAmountHero(
            "Advance / collect now",
            if (amount.isBlank()) "₹0" else MtoFormat.exact(currency, amount),
            caption = source.takeIf { it.isNotBlank() }?.let { "Against $it" },
            currencyBadge = currency,
        )
        MtoNumericKeypad(amount) { amount = it }
        MtoPickerRow("Customer", customer.ifBlank { "Select" }, onClick = {
            customer = parties.firstOrNull()?.name ?: openAr.firstOrNull()?.counterpartyName.orEmpty()
        }, externalLabel = true)
        parties.take(3).forEach { MtoPickerRow(it.name, "AR", onClick = { customer = it.name }) }
        MtoPickerRow(
            "Source invoice",
            source.ifBlank { if (openAr.isEmpty()) "Unavailable · no open AR" else "Select open invoice" },
            onClick = { openAr.firstOrNull()?.let { source = it.title; customer = it.counterpartyName ?: customer } },
            externalLabel = true,
        )
        openAr.take(4).forEach { row ->
            MtoPickerRow(row.title, MtoFormat.exact(currency, row.amount), onClick = {
                source = row.title
                customer = row.counterpartyName ?: customer
            })
        }
        MtoChipRow("Method", listOf("UPI", "NEFT", "Cash", "Cheque"), method) { method = it }
        MtoTextField(reference, "Reference / UTR", onValueChange = { reference = it })
        MtoMoreDetails(expanded = moreOpen, onToggle = { moreOpen = !moreOpen }, hint = "Notes · Attachment · Tags") {
            MtoTextField(note, "Notes", onValueChange = { note = it }, singleLine = false)
        }
        MtoPickerRow("Received on", MtoFormat.day(receivedOn), onClick = { showDatePicker = true }, externalLabel = true)
        MtoStickyFooter(
            label = if (source.isBlank()) "Hold advance" else "Post collection",
            enabled = postable,
            caveat = "Draft preserved if you close or open a picker",
            onClick = {
            val alloc = openAr.firstOrNull { it.title == source }
            onSubmit(
                ManufacturingEventDraft(
                    eventType = "COLLECTION",
                    title = if (source.isBlank()) "Advance · $customer" else "Collection · $customer",
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
    MtoDatePickerModal(
        selectedIso = receivedOn,
        visible = showDatePicker,
        onDismiss = { showDatePicker = false },
        onConfirm = { receivedOn = it },
    )
}

@Composable
private fun MtoPaymentSheet(
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
    mtoCaptureScaffold(
        title = if (reimburse) "Approval / Reimbursement" else "Record Payment",
        subtitle = if (reimburse) "Approval ≠ disbursement" else "Settle AP · never new PURCHASE",
        onDismiss = onDismiss,
        why = if (reimburse) "Submit for approval — approval is not disbursement. Payment posts separately when paid."
        else "Payment allocates against open AP only — it never creates a new PURCHASE.",
    ) {
        if (reimburse) MtoChipRow(null, listOf("Approval", "Reimbursement"), "Reimbursement") {}
        MtoAmountHero(if (reimburse) "Claim amount" else "Paid", if (amount.isBlank()) "₹0" else MtoFormat.exact(currency, amount), currencyBadge = currency)
        MtoNumericKeypad(amount) { amount = it }
        MtoPickerRow("Payee", payee.ifBlank { "Select" }, onClick = {
            payee = vendors.firstOrNull()?.name ?: openAp.firstOrNull()?.counterpartyName.orEmpty()
        }, externalLabel = true)
        vendors.take(3).forEach { MtoPickerRow(it.name, "AP", onClick = { payee = it.name }) }
        openAp.take(3).forEach { MtoPickerRow(it.title, MtoFormat.exact(currency, it.amount), onClick = {
            payee = it.counterpartyName ?: payee
        }) }
        MtoChipRow("Method", listOf("NEFT", "UPI", "Cash", "Cheque"), method) { method = it }
        MtoTextField(reference, "Reference / UTR", onValueChange = { reference = it })
        MtoPickerRow("Paid on", MtoFormat.todayLine(), onClick = null, externalLabel = true)
        MtoStickyFooter(label = if (reimburse) "Submit for approval" else "Post payment", enabled = postable, caveat = "Draft preserved if you close or open a picker", onClick = {
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
private fun MtoExpenseSheet(model: OperatingProjectionDto, onDismiss: () -> Unit, onSubmit: (ManufacturingEventDraft) -> Unit) {
    val currency = model.currency ?: "INR"
    var amount by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("Utilities") }
    var payee by remember { mutableStateOf("") }
    var jobLink by remember { mutableStateOf("") }
    var payState by remember { mutableStateOf("Payable · 15 days") }
    var period by remember { mutableStateOf(java.time.YearMonth.now().toString()) }
    var moreOpen by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf("") }
    var showMonth by remember { mutableStateOf(false) }
    val payOptions = listOf("Paid", "Payable · 7 days", "Payable · 15 days", "Payable · 30 days")
    val jobs = model.activity.filter {
        it.eventType.equals("UPDATE", true) ||
            it.eventType.equals("ORDER", true) ||
            it.eventType.equals("PRODUCTION", true)
    }
    val postable = amount.toBigDecimalOrNull()?.signum() == 1
    mtoCaptureScaffold(
        title = "Record Expense · job link",
        subtitle = "Type · Payee · Job link · Payment state · Period",
        onDismiss = onDismiss,
        why = "Expense is cost on the job margin; payment state is separate. Never reduces quote or order value.",
    ) {
        MtoChipRow("Type", listOf("Utilities", "Freight", "Consumables", "Job work", "Other"), type) { type = it }
        MtoAmountHero("Expense", if (amount.isBlank()) "₹0" else MtoFormat.exact(currency, amount), currencyBadge = currency)
        MtoNumericKeypad(amount) { amount = it }
        MtoTextField(payee, "Payee", onValueChange = { payee = it })
        MtoPickerRow(
            "Job link",
            jobLink.ifBlank { if (jobs.isEmpty()) "Unavailable · no open job" else "Select job (optional)" },
            onClick = { jobs.firstOrNull()?.let { jobLink = it.title } },
            externalLabel = true,
        )
        jobs.take(4).forEach { MtoPickerRow(it.title, it.status ?: "Job", onClick = { jobLink = it.title }) }
        MtoPickerRow(
            "Payment state",
            payState,
            onClick = {
                val i = payOptions.indexOf(payState).coerceAtLeast(0)
                payState = payOptions[(i + 1) % payOptions.size]
            },
            externalLabel = true,
        )
        MtoMoreDetails(expanded = moreOpen, onToggle = { moreOpen = !moreOpen }, hint = "Notes · Attachment · Tags") {
            MtoTextField(note, "Notes", onValueChange = { note = it }, singleLine = false)
        }
        MtoPickerRow("Period", mtoMonthLabel(period), onClick = { showMonth = true }, externalLabel = true)
        MtoMonthPicker(period) { period = it }
        MtoStickyFooter(label = "Post expense", enabled = postable, caveat = "Draft preserved if you close or open a picker", onClick = {
            onSubmit(
                ManufacturingEventDraft(
                    eventType = "SPEND",
                    title = "Expense · $type",
                    amount = amount,
                    note = listOfNotNull(
                        "period=$period",
                        jobLink.takeIf { it.isNotBlank() }?.let { "job=$it" },
                        "pay=$payState",
                        note.takeIf { it.isNotBlank() },
                    ).joinToString(" · "),
                    channelName = jobLink.ifBlank { null },
                    counterpartyName = payee.ifBlank { null },
                    paymentStatus = if (payState == "Paid") "PAID" else "OPEN",
                    contract = OperatingContractBody(
                        expenseCategory = type,
                        paymentState = payState,
                    ),
                ),
            )
        })
    }
    MtoMonthPickerModal(
        selectedYm = period,
        visible = showMonth,
        onDismiss = { showMonth = false },
        onConfirm = { period = it },
    )
}

@Composable
private fun MtoReturnSheet(model: OperatingProjectionDto, onDismiss: () -> Unit, onSubmit: (ManufacturingEventDraft) -> Unit) {
    val currency = model.currency ?: "INR"
    var side by remember { mutableStateOf("Customer return") }
    var amount by remember { mutableStateOf("") }
    var party by remember { mutableStateOf("") }
    var sourceInv by remember { mutableStateOf("") }
    var restock by remember { mutableStateOf("Restock to Job FG") }
    var reason by remember { mutableStateOf("") }
    var creditDate by remember { mutableStateOf(LocalDate.now().toString()) }
    var moreOpen by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf("") }
    var showDate by remember { mutableStateOf(false) }
    val invoices = model.activity.filter {
        it.eventType.equals("SALE", true) || it.eventType.equals("INVOICE", true) ||
            it.title.contains("INV", ignoreCase = true)
    }
    val postable = amount.toBigDecimalOrNull()?.signum() == 1 && party.isNotBlank()
    mtoCaptureScaffold(
        title = "Return / Credit Note",
        subtitle = "$side · credit note",
        onDismiss = onDismiss,
        why = "Return references the source invoice. Credit note reduces AR on that invoice only; restock is explicit — never silent FG overwrite.",
    ) {
        MtoChipRow(null, listOf("Customer return", "Supplier return"), side) { side = it }
        MtoAmountHero(
            "Credit amount",
            if (amount.isBlank()) "₹0" else MtoFormat.exact(currency, amount),
            caption = sourceInv.takeIf { it.isNotBlank() }?.let { "Against $it" },
            currencyBadge = currency,
        )
        MtoNumericKeypad(amount) { amount = it }
        MtoTextField(party, "Party", onValueChange = { party = it })
        MtoPickerRow(
            "Source",
            sourceInv.ifBlank { if (invoices.isEmpty()) "Unavailable · no posted INV" else "Select source INV" },
            onClick = { invoices.firstOrNull()?.let { sourceInv = it.title } },
            externalLabel = true,
        )
        invoices.take(4).forEach { row ->
            MtoPickerRow(row.title, row.amount?.let { MtoFormat.exact(currency, it) } ?: "INV", onClick = {
                sourceInv = row.title
                if (party.isBlank()) party = row.counterpartyName ?: party
            })
        }
        MtoChipRow("Restock", listOf("Restock to Job FG", "Scrap", "No stock"), restock) { restock = it }
        MtoTextField(reason, "Reason", onValueChange = { reason = it })
        MtoMoreDetails(expanded = moreOpen, onToggle = { moreOpen = !moreOpen }, hint = "Notes · Attachment · Tags") {
            MtoTextField(note, "Notes", onValueChange = { note = it }, singleLine = false)
        }
        MtoPickerRow("Credit note date", MtoFormat.day(creditDate), onClick = { showDate = true }, externalLabel = true)
        MtoDateQuickPicker(creditDate) { creditDate = it }
        MtoStickyFooter(label = "Post credit note", enabled = postable, caveat = "Draft preserved if you close or open a picker", onClick = {
            onSubmit(
                ManufacturingEventDraft(
                    eventType = "RETURN",
                    title = "Return · $party",
                    amount = amount,
                    note = listOfNotNull(
                        side,
                        restock,
                        sourceInv.takeIf { it.isNotBlank() }?.let { "source=$it" },
                        reason.takeIf { it.isNotBlank() }?.let { "reason=$it" },
                        note.takeIf { it.isNotBlank() },
                    ).joinToString(" · "),
                    channelName = sourceInv.ifBlank { null },
                    counterpartyName = party,
                    paymentStatus = null,
                    occurredAt = creditDate,
                    parentEventId = invoices.firstOrNull { it.title == sourceInv }?.id,
                    contract = OperatingContractBody(
                        stockAction = when (restock) {
                            "Restock to Job FG" -> "RESTOCK_JOB_FG"
                            "No stock" -> "NO_STOCK"
                            else -> restock.uppercase()
                        },
                    ),
                ),
            )
        })
    }
    MtoDatePickerModal(
        selectedIso = creditDate,
        visible = showDate,
        onDismiss = { showDate = false },
        onConfirm = { creditDate = it },
    )
}

@Composable
private fun MtoStockSheet(
    model: OperatingProjectionDto,
    onDismiss: () -> Unit,
    onSubmit: (ManufacturingEventDraft) -> Unit,
    onNavigateUtility: ((MtoUtilityNav) -> Unit)?,
) {
    var action by remember { mutableStateOf("Set current") }
    var product by remember { mutableStateOf("") }
    var qty by remember { mutableStateOf("") }
    var location by remember { mutableStateOf(model.location ?: "Job FG bay · J-tag") }
    var jobTag by remember { mutableStateOf("J-07") }
    var reason by remember { mutableStateOf("Cycle count") }
    val skus = model.masters.orEmpty().filter {
        it.kind.equals("SKU", true) || it.kind.equals("PART", true) || it.kind.equals("PRODUCT", true)
    }
    val postable = product.isNotBlank() && qty.toBigDecimalOrNull() != null
    mtoCaptureScaffold(
        title = "Stock update (job-linked)",
        subtitle = "Explicit movement · J-tag location",
        onDismiss = onDismiss,
        why = "Stock updates are explicit Set / Add / Reduce / Transfer movements — never inferred from sale or job save. Location stays job-tagged when linked to an open job.",
    ) {
        MtoChipRow(null, listOf("Set current", "Add", "Reduce", "Transfer"), action) { next ->
            if (next == "Transfer") {
                onNavigateUtility?.invoke(MtoUtilityNav.TRANSFER) ?: run { action = next }
            } else {
                action = next
            }
        }
        MtoTextField(product, "Product / part", onValueChange = { product = it })
        skus.take(4).forEach { MtoPickerRow(it.name, it.unit ?: "nos", onClick = { product = it.name }) }
        MtoTextField(qty, "Qty", onValueChange = { qty = it.filter { ch -> ch.isDigit() || ch == '.' }.take(10) })
        MtoTextField(jobTag, "Job tag", onValueChange = { jobTag = it })
        MtoPickerRow(
            "Location (job-tagged)",
            if (jobTag.isBlank()) location else "$location · $jobTag",
            onClick = null,
            externalLabel = true,
        )
        MtoChipRow("Reason", listOf("Cycle count", "Damage", "Correction", "Custody"), reason) { reason = it }
        MtoStickyFooter(label = "Post stock update", enabled = postable, caveat = "Draft preserved if you close or open a picker", onClick = {
            onSubmit(
                ManufacturingEventDraft(
                    eventType = "STOCK",
                    title = "Stock · $action · $product",
                    amount = null,
                    note = listOfNotNull(reason, jobTag.takeIf { it.isNotBlank() }?.let { "J-tag=$it" }).joinToString(" · "),
                    channelName = if (jobTag.isBlank()) location else "$location · $jobTag",
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
private fun MtoBudgetSheet(model: OperatingProjectionDto, onDismiss: () -> Unit, onSubmit: (ManufacturingEventDraft) -> Unit, target: Boolean) {
    val currency = model.currency ?: "INR"
    var basis by remember { mutableStateOf(if (target) "Confirmed orders" else "Invoiced basis") }
    var amount by remember { mutableStateOf("") }
    var period by remember { mutableStateOf(java.time.YearMonth.now().toString()) }
    var reason by remember { mutableStateOf("") }
    var moreOpen by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf("") }
    var showMonth by remember { mutableStateOf(false) }
    val previousRaw = when (basis) {
        "Confirmed orders" -> model.target?.value?.takeIf { model.target?.configured == true && it.isNotBlank() }
        else -> model.budget?.amount?.takeIf { model.budget?.configured == true && it.isNotBlank() }
            ?: model.target?.value?.takeIf { model.target?.configured == true && it.isNotBlank() }
    }
    val previousLabel = previousRaw?.let { "${MtoFormat.exact(currency, it)} · v1" } ?: "Unavailable · no prior revision"
    val postable = amount.toBigDecimalOrNull()?.signum() == 1
    mtoCaptureScaffold(
        title = "Budget / Target Revision",
        subtitle = "Versioned · effective-dated",
        onDismiss = onDismiss,
        why = "Versioned + effective-dated. Target counts invoiced jobs — quotes ≠ revenue, open quotes never count.",
    ) {
        MtoChipRow(null, listOf("Invoiced basis", "Confirmed orders"), basis) { basis = it }
        MtoAmountHero(
            "New target",
            if (amount.isBlank()) "—" else MtoFormat.exact(currency, amount),
            caption = "Effective · ${if (basis == "Invoiced basis") "invoiced only" else "confirmed orders"}",
            currencyBadge = currency,
        )
        MtoNumericKeypad(amount) { amount = it }
        MtoPickerRow("Period", mtoMonthLabel(period), onClick = { showMonth = true }, externalLabel = true)
        MtoMonthPicker(period) { period = it }
        MtoPickerRow("Previous", previousLabel, onClick = null, externalLabel = true)
        MtoTextField(reason, "Reason", onValueChange = { reason = it })
        MtoMoreDetails(expanded = moreOpen, onToggle = { moreOpen = !moreOpen }, hint = "Notes · Attachment · Tags") {
            MtoTextField(note, "Notes", onValueChange = { note = it }, singleLine = false)
        }
        MtoStickyFooter(label = "Save revision", enabled = postable, caveat = "Draft preserved if you close or open a picker", onClick = {
            onSubmit(
                ManufacturingEventDraft(
                    eventType = if (target || basis == "Confirmed orders") "TARGET" else "BUDGET",
                    title = "Revision · $period",
                    amount = amount,
                    note = listOfNotNull(
                        "basis=$basis",
                        "period=$period",
                        previousRaw?.let { "previous=$it" },
                        reason.takeIf { it.isNotBlank() }?.let { "reason=$it" },
                        note.takeIf { it.isNotBlank() },
                    ).joinToString(" · "),
                    channelName = null,
                    counterpartyName = null,
                    paymentStatus = null,
                ),
            )
        })
    }
    MtoMonthPickerModal(
        selectedYm = period,
        visible = showMonth,
        onDismiss = { showMonth = false },
        onConfirm = { period = it },
    )
}

@Composable
private fun MtoIssueSheet(model: OperatingProjectionDto, onDismiss: () -> Unit, onSubmit: (ManufacturingEventDraft) -> Unit, issue: Boolean) {
    var kind by remember { mutableStateOf(if (issue) "Issue" else "Update") }
    var title by remember { mutableStateOf("") }
    var severity by remember { mutableStateOf("Medium") }
    var owner by remember { mutableStateOf("") }
    var link by remember { mutableStateOf("") }
    var due by remember { mutableStateOf(LocalDate.now().plusDays(2).toString()) }
    var showDate by remember { mutableStateOf(false) }
    val people = model.participants.filter { it.isNotBlank() }
    mtoCaptureScaffold(
        title = "Post update / Issue",
        subtitle = "Blockers link J / SO · not a money event",
        onDismiss = onDismiss,
        why = "Updates and issues are plant signals. They do not post revenue, stock, or GST by themselves. Link a job or SO when the blocker is job-scoped.",
    ) {
        MtoChipRow(null, listOf("Update", "Issue", "Risk", "Approval"), kind) { kind = it }
        MtoTextField(title, "Title", onValueChange = { title = it })
        MtoChipRow("Severity", listOf("Low", "Medium", "High"), severity) { severity = it }
        MtoPickerRow(
            "Owner",
            owner.ifBlank { people.firstOrNull() ?: "Select owner" },
            onClick = { owner = people.firstOrNull() ?: owner },
            externalLabel = true,
        )
        people.take(3).forEach { MtoPickerRow(it, "Owner", onClick = { owner = it }) }
        MtoTextField(link, "Link · J / SO", onValueChange = { link = it })
        MtoPickerRow("Due", MtoFormat.day(due), onClick = { showDate = true }, externalLabel = true)
        MtoStickyFooter(label = "Post update", enabled = title.isNotBlank(), caveat = "Draft preserved if you close or open a picker", onClick = {
            onSubmit(
                ManufacturingEventDraft(
                    eventType = if (kind == "Issue" || kind == "Risk") "ISSUE" else "UPDATE",
                    title = title,
                    amount = null,
                    note = "severity=$severity · owner=$owner · link=$link",
                    channelName = null,
                    counterpartyName = owner.ifBlank { null },
                    paymentStatus = null,
                    itemName = link.ifBlank { null },
                    dueAt = due,
                ),
            )
        })
    }
    MtoDatePickerModal(
        selectedIso = due,
        visible = showDate,
        onDismiss = { showDate = false },
        onConfirm = { due = it },
    )
}

@Composable
private fun MtoApprovalSheet(model: OperatingProjectionDto, onDismiss: () -> Unit, onSubmit: (ManufacturingEventDraft) -> Unit) {
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
    mtoCaptureScaffold(
        title = "Approval / Reimbursement",
        subtitle = "Approval ≠ disbursement",
        onDismiss = onDismiss,
        why = "Approval ≠ disbursement. Approver cannot approve own controlled action. Money moves only when a Payment is posted.",
    ) {
        MtoChipRow(null, listOf("Approval", "Reimbursement"), mode) { mode = it }
        MtoTextField(actionTitle, "Action", onValueChange = { actionTitle = it })
        MtoTextField(requester, "Requester", onValueChange = { requester = it })
        people.take(3).forEach { MtoPickerRow(it, "Team", onClick = { requester = it }) }
        MtoPickerRow(
            "Approver",
            approver.ifBlank { people.firstOrNull { it != requester } ?: "Select approver" },
            onClick = {
                approver = people.firstOrNull { it != requester } ?: approver
            },
            externalLabel = true,
        )
        people.filter { it != requester }.take(3).forEach {
            MtoPickerRow(it, "Approver", onClick = { approver = it })
        }
        MtoChipRow("Policy", listOf("Finance ≥ ₹1L", "Manager any", "Owner only"), policy) { policy = it }
        MtoMoreDetails(expanded = moreOpen, onToggle = { moreOpen = !moreOpen }, hint = "Notes · Attachment · Tags") {
            MtoTextField(note, "Notes", onValueChange = { note = it }, singleLine = false)
        }
        MtoPickerRow("Decide by", MtoFormat.day(decideBy), onClick = { showDatePicker = true }, externalLabel = true)
        MtoDateQuickPicker(decideBy) { decideBy = it }
        MtoStickyFooter(
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
    MtoDatePickerModal(
        selectedIso = decideBy,
        visible = showDatePicker,
        onDismiss = { showDatePicker = false },
        onConfirm = { decideBy = it },
    )
}

@Composable
private fun MtoQuotationSheet(model: OperatingProjectionDto, onDismiss: () -> Unit, onSubmit: (ManufacturingEventDraft) -> Unit) {
    val currency = model.currency ?: "INR"
    var mode by remember { mutableStateOf("Quotation") }
    var amount by remember { mutableStateOf("") }
    var customer by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Draft → Sent") }
    var lines by remember { mutableStateOf("") }
    var validity by remember { mutableStateOf("15 days") }
    var moreOpen by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf("") }
    val parties = model.masters.orEmpty().filter {
        it.kind.equals("CUSTOMER", true) || it.kind.equals("BUYER", true) || it.kind.equals("PARTY", true)
    }
    val previewGst = amount.toBigDecimalOrNull()?.multiply(java.math.BigDecimal("0.18"))
    val postable = amount.toBigDecimalOrNull()?.signum() == 1 && customer.isNotBlank()
    mtoCaptureScaffold(
        title = "Create Quotation",
        subtitle = null,
        onDismiss = onDismiss,
        why = "A quotation creates no revenue, AR or GST. Tax shown is a preview — only a posted invoice owns AR/GST.",
    ) {
        MtoChipRow(null, listOf("Quotation", "Revise", "Accept", "Cancel"), mode) { mode = it }
        MtoAmountHero(
            label = "Quote value (ex tax)",
            amount = if (amount.isBlank()) "—" else MtoFormat.exact(currency, amount),
            caption = if (previewGst == null) "GST preview · no AR" else "GST preview ${MtoFormat.exact(currency, previewGst.toPlainString())} · no AR",
            currencyBadge = currency,
        )
        MtoNumericKeypad(amount) { amount = it }
        MtoPickerRow("Customer", customer.ifBlank { "Select customer" }, onClick = {
            customer = parties.firstOrNull()?.name.orEmpty()
        }, externalLabel = true)
        parties.take(4).forEach { MtoPickerRow(it.name, "Customer", onClick = { customer = it.name }) }
        MtoPickerRow("Status", status, onClick = {
            status = when (status) {
                "Draft → Sent" -> "Sent"
                "Sent" -> "Accepted"
                else -> "Draft → Sent"
            }
        }, externalLabel = true)
        MtoTextField(lines, "Lines · item × qty @ rate", onValueChange = { lines = it })
        MtoChipRow("Validity", listOf("7 days", "15 days", "30 days"), validity) { validity = it }
        MtoMoreDetails(expanded = moreOpen, onToggle = { moreOpen = !moreOpen }, hint = "Notes · Attachment · Tags") {
            MtoTextField(note, "Notes", onValueChange = { note = it }, singleLine = false)
        }
        MtoUnavailableGstRow(label = "GST on quote", caption = "Preview only · Unavailable for posting")
        MtoStickyFooter(label = "Send quotation", enabled = postable, caveat = "Draft preserved if you close or open a picker", onClick = {
            onSubmit(
                ManufacturingEventDraft(
                    eventType = "CUSTOMER_ORDER",
                    title = "Quotation · $customer",
                    amount = amount,
                    note = listOfNotNull(
                        "QUOTE · mode=$mode",
                        "status=$status",
                        "validity=$validity",
                        lines.takeIf { it.isNotBlank() },
                        note.takeIf { it.isNotBlank() },
                        "no revenue · no AR · no GST",
                    ).joinToString(" · "),
                    channelName = null,
                    counterpartyName = customer,
                    paymentStatus = null,
                    contract = OperatingContractBody(
                        orderStatus = "QUOTE",
                        entryMode = mode.uppercase().replace(' ', '_'),
                    ),
                ),
            )
        })
    }
}

@Composable
private fun MtoConfirmOrderSheet(model: OperatingProjectionDto, onDismiss: () -> Unit, onSubmit: (ManufacturingEventDraft) -> Unit) {
    val currency = model.currency ?: "INR"
    var mode by remember { mutableStateOf("Accept") }
    var amount by remember { mutableStateOf("") }
    var quote by remember { mutableStateOf("") }
    var approval by remember { mutableStateOf("Owner") }
    var spec by remember { mutableStateOf("") }
    var due by remember { mutableStateOf(LocalDate.now().plusDays(21).toString()) }
    var showDate by remember { mutableStateOf(false) }
    val quotes = model.activity.filter {
        it.eventType.equals("CUSTOMER_ORDER", true) || it.eventType.equals("ORDER", true) ||
            it.title.contains("quot", ignoreCase = true)
    }
    val postable = quote.isNotBlank() || amount.toBigDecimalOrNull()?.signum() == 1
    mtoCaptureScaffold(
        title = "Accept / Confirm Order",
        subtitle = "Commitment only · no revenue",
        onDismiss = onDismiss,
        why = "Confirm creates an SO commitment only — no revenue, AR, stock, or GST until a posted invoice.",
    ) {
        MtoChipRow(null, listOf("Quotation", "Revise", "Accept", "Cancel"), mode) { mode = it }
        MtoAmountHero(
            "Confirmed value",
            if (amount.isBlank()) "—" else MtoFormat.exact(currency, amount),
            caption = "Commitment · not revenue",
            currencyBadge = currency,
        )
        MtoNumericKeypad(amount) { amount = it }
        MtoPickerRow("Accepted quote", quote.ifBlank { if (quotes.isEmpty()) "Select quote" else "Pick quote" }, onClick = {
            quotes.firstOrNull()?.let { quote = it.title; amount = it.amount ?: amount }
        }, externalLabel = true)
        quotes.take(4).forEach { row ->
            MtoPickerRow(row.title, MtoFormat.exact(currency, row.amount), onClick = {
                quote = row.title
                amount = row.amount ?: amount
            })
        }
        MtoChipRow("Approval", listOf("Owner", "Manager", "Estimator"), approval) { approval = it }
        MtoTextField(spec, "Spec / revision", onValueChange = { spec = it })
        MtoPickerRow("Due", MtoFormat.day(due), onClick = { showDate = true }, externalLabel = true)
        MtoStickyFooter(label = "Confirm order", enabled = postable, caveat = "Draft preserved if you close or open a picker", onClick = {
            onSubmit(
                ManufacturingEventDraft(
                    eventType = "CUSTOMER_ORDER",
                    title = "Confirm · ${quote.ifBlank { "order" }}",
                    amount = amount.ifBlank { null },
                    note = "CONFIRM · commitment only · approval=$approval · spec=$spec · no revenue",
                    channelName = null,
                    counterpartyName = null,
                    paymentStatus = null,
                    dueAt = due,
                    parentEventId = quotes.firstOrNull { it.title == quote }?.id,
                    contract = OperatingContractBody(orderStatus = "CONFIRMED", entryMode = "CONFIRM"),
                ),
            )
        })
    }
    MtoDatePickerModal(selectedIso = due, visible = showDate, onDismiss = { showDate = false }, onConfirm = { due = it })
}

@Composable
private fun MtoOpenJobSheet(
    model: OperatingProjectionDto,
    onDismiss: () -> Unit,
    onSubmit: (ManufacturingEventDraft) -> Unit,
    onNavigateUtility: ((MtoUtilityNav) -> Unit)?,
) {
    var mode by remember { mutableStateOf("From order") }
    var order by remember { mutableStateOf("") }
    var jobId by remember { mutableStateOf("J-${(model.activity.size % 90) + 7}") }
    var target by remember { mutableStateOf("") }
    var planned by remember { mutableStateOf(LocalDate.now().toString()) }
    var bomRev by remember { mutableStateOf("r1") }
    var reserve by remember { mutableStateOf("Reserve material") }
    var due by remember { mutableStateOf(LocalDate.now().plusDays(14).toString()) }
    var dateTarget by remember { mutableStateOf("planned") }
    var showDate by remember { mutableStateOf(false) }
    val orders = model.activity.filter {
        it.eventType.equals("CUSTOMER_ORDER", true) || it.eventType.equals("ORDER", true)
    }
    val postable = order.isNotBlank() || target.isNotBlank()
    mtoCaptureScaffold(
        title = "Open Production Job",
        subtitle = "Reserve ≠ consume · no output / sale",
        onDismiss = onDismiss,
        why = "Open job creates the job card and optional material reserve only — no output, stock consume, or sale until production / invoice.",
    ) {
        MtoChipRow(null, listOf("From order", "Job", "BOM", "Hold"), mode) {
            if (it == "Hold") onNavigateUtility?.invoke(MtoUtilityNav.HOLD) ?: run { mode = it }
            else mode = it
        }
        MtoPickerRow("Confirmed order", order.ifBlank { "Select SO" }, onClick = {
            orders.firstOrNull()?.let { order = it.title }
        }, externalLabel = true)
        orders.take(4).forEach { MtoPickerRow(it.title, it.eventType, onClick = { order = it.title }) }
        MtoTextField(jobId, "Job ID", onValueChange = { jobId = it })
        MtoTextField(target, "Target / finished part", onValueChange = { target = it })
        MtoPickerRow("Planned start", MtoFormat.day(planned), onClick = {
            dateTarget = "planned"
            showDate = true
        }, externalLabel = true)
        MtoChipRow("BOM rev", listOf("r1", "r2", "r3"), bomRev) { bomRev = it }
        MtoChipRow("Materials", listOf("Reserve material", "No reserve"), reserve) { reserve = it }
        MtoPickerRow("Due", MtoFormat.day(due), onClick = {
            dateTarget = "due"
            showDate = true
        }, externalLabel = true)
        MtoStickyFooter(label = "Open job", enabled = postable, caveat = "Draft preserved if you close or open a picker", onClick = {
            onSubmit(
                ManufacturingEventDraft(
                    eventType = "UPDATE",
                    title = "Open job · $jobId",
                    amount = null,
                    note = "OPEN_JOB · $mode · order=$order · bom=$bomRev · $reserve · no output/stock/sale",
                    channelName = reserve,
                    counterpartyName = null,
                    paymentStatus = null,
                    itemName = target.ifBlank { null },
                    dueAt = due,
                    occurredAt = planned,
                    contract = OperatingContractBody(orderStatus = "OPEN_JOB", entryMode = mode.uppercase().replace(' ', '_')),
                ),
            )
        })
    }
    MtoDatePickerModal(
        selectedIso = if (dateTarget == "due") due else planned,
        visible = showDate,
        onDismiss = { showDate = false },
        onConfirm = {
            if (dateTarget == "due") due = it else planned = it
        },
    )
}

@Composable
private fun MtoQuoteReviseSheet(model: OperatingProjectionDto, onDismiss: () -> Unit, onSubmit: (ManufacturingEventDraft) -> Unit) {
    val currency = model.currency ?: "INR"
    var mode by remember { mutableStateOf("Revise") }
    var amount by remember { mutableStateOf("") }
    var quote by remember { mutableStateOf("") }
    var version by remember { mutableStateOf("v2") }
    val quotes = model.activity.filter {
        it.eventType.equals("CUSTOMER_ORDER", true) || it.title.contains("quot", ignoreCase = true)
    }
    val postable = quote.isNotBlank() && amount.toBigDecimalOrNull()?.signum() == 1
    mtoCaptureScaffold(
        title = "Quote revise / expire",
        subtitle = "Versioned quote ≠ revenue",
        onDismiss = onDismiss,
        why = "Quote revise / extend / expire updates the quote version only — never revenue, AR, or GST.",
    ) {
        MtoChipRow(null, listOf("Q-id", "Revise", "Extend", "Expire"), mode) { mode = it }
        MtoAmountHero("Versioned value", if (amount.isBlank()) "—" else MtoFormat.exact(currency, amount), caption = "Quotes ≠ revenue", currencyBadge = currency)
        MtoNumericKeypad(amount) { amount = it }
        MtoPickerRow("Quote", quote.ifBlank { "Select Q-id" }, onClick = {
            quotes.firstOrNull()?.let { quote = it.title; amount = it.amount ?: amount }
        }, externalLabel = true)
        quotes.take(4).forEach { row ->
            MtoPickerRow(row.title, MtoFormat.exact(currency, row.amount), onClick = { quote = row.title; amount = row.amount ?: amount })
        }
        MtoChipRow("Version", listOf("v2", "v3", "v4"), version) { version = it }
        MtoStickyFooter(label = "Send revision $version", enabled = postable, caveat = "Draft preserved if you close or open a picker", onClick = {
            onSubmit(
                ManufacturingEventDraft(
                    eventType = "CUSTOMER_ORDER",
                    title = "Quote $version · $quote",
                    amount = amount,
                    note = "QUOTE_REVISE · $mode · $version · quotes ≠ revenue",
                    channelName = null,
                    counterpartyName = null,
                    paymentStatus = null,
                    revisesEventId = quotes.firstOrNull { it.title == quote }?.id,
                    contract = OperatingContractBody(orderStatus = "QUOTE_REVISE", entryMode = mode.uppercase()),
                ),
            )
        })
    }
}

@Composable
private fun MtoJobStatusSheet(model: OperatingProjectionDto, onDismiss: () -> Unit, onSubmit: (ManufacturingEventDraft) -> Unit) {
    var status by remember { mutableStateOf("Active") }
    var job by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var owner by remember { mutableStateOf("") }
    var impact by remember { mutableStateOf("None") }
    var since by remember { mutableStateOf(LocalDate.now().toString()) }
    var reviewBy by remember { mutableStateOf(LocalDate.now().plusDays(3).toString()) }
    val jobs = model.activity.filter {
        it.title.contains("job", ignoreCase = true) || it.eventType.equals("UPDATE", true) ||
            it.eventType.equals("ORDER", true)
    }
    val postable = job.isNotBlank()
    mtoCaptureScaffold(
        title = "Job status change",
        subtitle = "No stock · no cost · no sale",
        onDismiss = onDismiss,
        why = "Job status is a status event only — Active / Pending / Blocked / On hold never move stock, cost, or sale.",
    ) {
        MtoChipRow(null, listOf("Active", "Pending", "Blocked", "On hold"), status) { status = it }
        MtoPickerRow("Job", job.ifBlank { "Select job" }, onClick = { jobs.firstOrNull()?.let { job = it.title } }, externalLabel = true)
        jobs.take(4).forEach { MtoPickerRow(it.title, it.eventType, onClick = { job = it.title }) }
        MtoTextField(reason, "Reason", onValueChange = { reason = it })
        MtoPickerRow("Since", MtoFormat.day(since), onClick = null, externalLabel = true)
        MtoTextField(owner, "Owner", onValueChange = { owner = it })
        MtoChipRow("Customer impact", listOf("None", "Delay", "Notify"), impact) { impact = it }
        MtoPickerRow("Review by", MtoFormat.day(reviewBy), onClick = null, externalLabel = true)
        MtoStickyFooter(label = "Set status", enabled = postable, caveat = "Draft preserved if you close or open a picker", onClick = {
            onSubmit(
                ManufacturingEventDraft(
                    eventType = "UPDATE",
                    title = "Job status · $job · $status",
                    amount = null,
                    note = "JOB_STATUS · $status · reason=$reason · owner=$owner · impact=$impact · no stock/cost/sale",
                    channelName = null,
                    counterpartyName = null,
                    paymentStatus = null,
                    occurredAt = since,
                    dueAt = reviewBy,
                    contract = OperatingContractBody(orderStatus = status.uppercase().replace(' ', '_')),
                ),
            )
        })
    }
}

@Composable
private fun MtoDeliverySheet(model: OperatingProjectionDto, onDismiss: () -> Unit, onSubmit: (ManufacturingEventDraft) -> Unit) {
    var direction by remember { mutableStateOf("Dispatch to buyer") }
    var job by remember { mutableStateOf("") }
    var source by remember { mutableStateOf("") }
    var qty by remember { mutableStateOf("") }
    var acceptedRemaining by remember { mutableStateOf("12") }
    var confirmPod by remember { mutableStateOf("Pending") }
    var dispatchSlot by remember { mutableStateOf("Morning · 09:00") }
    var dispatchIso by remember { mutableStateOf(LocalDate.now().toString()) }
    var showDateTime by remember { mutableStateOf(false) }
    val qtyNum = qty.toBigDecimalOrNull()
    val remainingNum = acceptedRemaining.toBigDecimalOrNull()
    val withinAccepted = qtyNum == null || remainingNum == null || qtyNum <= remainingNum
    val postable = source.isNotBlank() && job.isNotBlank() && withinAccepted && confirmPod != "POD photo"
    mtoCaptureScaffold(
        title = "Delivery / dispatch against job",
        subtitle = "Accepted units only · never re-book INV",
        onDismiss = onDismiss,
        why = "Dispatch moves accepted job units against the source SO/J/INV only — never re-books invoice revenue or exceeds accepted qty. Confirm/POD is custody chrome — POD photo stays locked without a camera stack.",
    ) {
        MtoChipRow(null, listOf("Material in", "Dispatch to buyer"), direction) { direction = it }
        MtoTextField(job, "Job", onValueChange = { job = it })
        MtoTextField(source, "Source SO / J / INV", onValueChange = { source = it })
        MtoTextField(
            acceptedRemaining,
            "Accepted remaining",
            onValueChange = { acceptedRemaining = it.filter { ch -> ch.isDigit() || ch == '.' }.take(10) },
        )
        MtoTextField(qty, "Qty · capped at accepted", onValueChange = { qty = it.filter { ch -> ch.isDigit() || ch == '.' }.take(10) })
        MtoPickerRow(
            "Qty remaining against accepted",
            if (acceptedRemaining.isBlank()) {
                "Unavailable · no accepted qty"
            } else {
                val left = remainingNum?.let { rem ->
                    qtyNum?.let { rem - it }?.takeIf { it.signum() >= 0 } ?: rem
                }
                "${left ?: acceptedRemaining} of $acceptedRemaining remaining · against accepted"
            },
            onClick = null,
            externalLabel = true,
        )
        if (!withinAccepted) {
            MtoLockedChip("Qty exceeds accepted · reduce before post")
        }
        MtoChipRow("Confirm / POD", listOf("Pending", "Confirmed", "POD photo"), confirmPod) { next ->
            if (next == "POD photo") {
                // D5 chrome-only — no camera / POD upload stack
                confirmPod = "Confirmed"
            } else {
                confirmPod = next
            }
        }
        MtoLockedChip("POD photo · Unavailable")
        MtoPickerRow(
            "Dispatch on",
            "${MtoFormat.day(dispatchIso)} · $dispatchSlot",
            onClick = { showDateTime = true },
            externalLabel = true,
        )
        MtoDateTimeSlots(dispatchSlot) { dispatchSlot = it }
        MtoStickyFooter(label = "Post dispatch", enabled = postable, caveat = "Draft preserved if you close or open a picker", onClick = {
            onSubmit(
                ManufacturingEventDraft(
                    eventType = "DELIVERY",
                    title = "Delivery · ${job.ifBlank { source }}",
                    amount = null,
                    note = "$direction · confirm=$confirmPod · acceptedRemaining=$acceptedRemaining · $dispatchIso · $dispatchSlot",
                    channelName = job.ifBlank { null },
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
    MtoDateTimePickerModal(
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
private fun MtoProductionSheet(
    model: OperatingProjectionDto,
    onDismiss: () -> Unit,
    onSubmit: (ManufacturingEventDraft) -> Unit,
    onNavigateUtility: ((MtoUtilityNav) -> Unit)?,
) {
    var mode by remember { mutableStateOf("Against job") }
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
    mtoCaptureScaffold(
        title = "Record Production",
        subtitle = "Accepted ≠ rejected · accepted only sellable",
        onDismiss = onDismiss,
        why = "Only accepted qty becomes sellable FG. Rejected and rework are distinct — never silent overwrite of finished stock.",
    ) {
        MtoChipRow(null, listOf("Against job", "Batch", "Rework", "WIP move"), mode) {
            if (it == "WIP move") {
                onNavigateUtility?.invoke(MtoUtilityNav.TRANSFER) ?: run { mode = it }
            } else {
                mode = it
            }
        }
        MtoTextField(product, "Finished product", onValueChange = { product = it })
        skus.take(4).forEach { MtoPickerRow(it.name, it.unit ?: "nos", onClick = { product = it.name }) }
        MtoTextField(batch, "Batch / heat", onValueChange = { batch = it })
        MtoFieldLabel("Accepted qty")
        MtoTextField(accepted, "0", onValueChange = { accepted = it.filter { ch -> ch.isDigit() || ch == '.' }.take(10) })
        MtoFieldLabel("Rejected qty")
        MtoTextField(rejected, "0", onValueChange = { rejected = it.filter { ch -> ch.isDigit() || ch == '.' }.take(10) })
        MtoTextField(scrapReason, "Scrap reason", onValueChange = { scrapReason = it })
        MtoChipRow(
            "Materials",
            listOf("Explicit consumption", "Backflush", "None"),
            materials,
        ) { materials = it }
        MtoMoreDetails(expanded = moreOpen, onToggle = { moreOpen = !moreOpen }, hint = "Notes · Attachment · Tags") {
            MtoTextField(note, "Notes", onValueChange = { note = it }, singleLine = false)
        }
        MtoPickerRow("Produced on", MtoFormat.day(producedOn), onClick = { showDatePicker = true }, externalLabel = true)
        MtoDateQuickPicker(producedOn) { producedOn = it }
        MtoStickyFooter(label = "Post production", enabled = postable, caveat = "Draft preserved if you close or open a picker", onClick = {
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
    MtoDatePickerModal(
        selectedIso = producedOn,
        visible = showDatePicker,
        onDismiss = { showDatePicker = false },
        onConfirm = { producedOn = it },
    )
}

@Composable
private fun MtoReworkSheet(model: OperatingProjectionDto, onDismiss: () -> Unit, onSubmit: (ManufacturingEventDraft) -> Unit) {
    var action by remember { mutableStateOf("Rework") }
    var source by remember { mutableStateOf("") }
    var qty by remember { mutableStateOf("") }
    var vendor by remember { mutableStateOf("") }
    var expectedReturn by remember { mutableStateOf(LocalDate.now().plusDays(7).toString()) }
    val postable = source.isNotBlank() && qty.toBigDecimalOrNull()?.signum() == 1
    mtoCaptureScaffold(
        title = "Rework / Scrap / Job Work",
        subtitle = "Distinct events · no silent FG overwrite",
        onDismiss = onDismiss,
        why = "Rework, scrap, and job work out are distinct events. They never silently overwrite accepted finished goods.",
    ) {
        MtoChipRow(null, listOf("Rework", "Scrap", "Job work out"), action) { action = it }
        MtoTextField(source, "Source batch / WO", onValueChange = { source = it })
        MtoTextField(qty, "Qty", onValueChange = { qty = it.filter { ch -> ch.isDigit() || ch == '.' }.take(10) })
        if (action == "Job work out") {
            MtoTextField(vendor, "Vendor", onValueChange = { vendor = it })
            MtoLockedChip("Challan · Unavailable")
            MtoPickerRow("Expected return", MtoFormat.day(expectedReturn), onClick = null, externalLabel = true)
            MtoDateQuickPicker(expectedReturn) { expectedReturn = it }
        }
        MtoStickyFooter(label = "Post action", enabled = postable, caveat = "Draft preserved if you close or open a picker", onClick = {
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

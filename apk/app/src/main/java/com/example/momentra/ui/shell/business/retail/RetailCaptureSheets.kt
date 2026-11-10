package com.example.momentra.ui.shell.business.retail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.momentra.data.api.OperatingAllocationBody
import com.example.momentra.data.api.OperatingContractBody
import com.example.momentra.data.api.OperatingLineBody
import com.example.momentra.data.api.OperatingMasterDto
import com.example.momentra.data.api.OperatingPaymentDto
import com.example.momentra.data.api.OperatingProjectionDto
import com.example.momentra.ui.shell.business.shared.OperatingSheetScrim
import com.example.momentra.ui.theme.RetailRose
import java.math.BigDecimal
import java.math.RoundingMode

@Composable
internal fun RetailCaptureSheet(
    sheet: RetailSheet,
    model: OperatingProjectionDto,
    onDismiss: () -> Unit,
    onSubmit: (RetailEventDraft) -> Unit,
    companyId: String? = null,
    onMasterSubmit: ((RetailEventDraft) -> Unit)? = null,
) {
    when (sheet) {
        RetailSheet.PURCHASE -> RetailPurchaseSheet(model, onDismiss, onSubmit)
        RetailSheet.COLLECTION -> RetailCollectionSheet(model, onDismiss, onSubmit)
        RetailSheet.PAYMENT -> RetailPaymentSheet(model, onDismiss, onSubmit)
        RetailSheet.SPEND -> RetailExpenseSheet(model, onDismiss, onSubmit)
        RetailSheet.STOCK -> RetailStockUpdateSheet(model, onDismiss, onSubmit)
        RetailSheet.ORDER -> RetailOrderSheet(
            model = model,
            onDismiss = onDismiss,
            onSubmit = onSubmit,
            companyId = companyId,
            onMasterSubmit = onMasterSubmit ?: onSubmit,
        )
        RetailSheet.STATUS -> RetailStatusSheet(model, onDismiss, onSubmit)
        RetailSheet.BUDGET -> RetailBudgetSheet(model, onDismiss, onSubmit)
        RetailSheet.ISSUE -> RetailIssueSheet(model, onDismiss, onSubmit)
        else -> Unit
    }
}

private data class CaptureLineDraft(
    val id: Long,
    var sku: String = "",
    var name: String = "",
    var qty: String = "1",
    var rate: String = "",
    var hsn: String = "",
    var unit: String = "pcs",
)

private fun CaptureLineDraft.lineTotal(): BigDecimal {
    val q = qty.toBigDecimalOrNull() ?: return BigDecimal.ZERO
    val r = rate.toBigDecimalOrNull() ?: return BigDecimal.ZERO
    return q.multiply(r).setScale(2, RoundingMode.HALF_UP)
}

private fun List<CaptureLineDraft>.goodsTotal(): BigDecimal =
    fold(BigDecimal.ZERO) { acc, line -> acc.add(line.lineTotal()) }

private fun newCaptureLine(seed: Int = 0) = CaptureLineDraft(
    id = System.nanoTime() + seed,
    name = "",
    qty = "1",
    rate = "",
)

/** `2399:30130` Record Purchase */
@Composable
private fun RetailPurchaseSheet(
    model: OperatingProjectionDto,
    onDismiss: () -> Unit,
    onSubmit: (RetailEventDraft) -> Unit,
) {
    val currency = model.currency ?: "INR"
    var goodsMode by remember { mutableStateOf("Goods") }
    var scanQuery by remember { mutableStateOf("") }
    var supplier by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("Back store") }
    var linkedPo by remember { mutableStateOf("") }
    var freight by remember { mutableStateOf("0") }
    var tender by remember { mutableStateOf("Pay later") }
    var note by remember { mutableStateOf("") }
    var moreOpen by remember { mutableStateOf(false) }
    val lines = remember {
        mutableStateListOf(newCaptureLine(1), newCaptureLine(2))
    }
    val suppliers = model.masters.orEmpty().filter { it.kind.equals("SUPPLIER", true) }
    val locations = model.masters.orEmpty().filter {
        it.kind.equals("LOCATION", true) || it.kind.equals("STORE", true) || it.kind.equals("CHANNEL", true)
    }
    val openPos = model.activity.filter {
        it.eventType.equals("ORDER", true) &&
            (it.orderSide.equals("PURCHASE", true) || it.title.contains("PO", ignoreCase = true))
    }
    val goods = lines.goodsTotal()
    val freightBd = freight.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val purchaseTotal = goods.add(freightBd).setScale(2, RoundingMode.HALF_UP)
    val amountStr = purchaseTotal.stripTrailingZeros().toPlainString()
    val postable = supplier.isNotBlank() && purchaseTotal.signum() == 1 && lines.count { it.name.isNotBlank() } >= 1

    retailCaptureScaffold(
        title = "Record Purchase",
        subtitle = "$goodsMode · ${model.title ?: "Store"} · GST on",
        onDismiss = onDismiss,
        why = "Stock += accepted qty only on receipt. AP from posted purchase; payment allocates later — never double-cost.",
        cta = if (postable) "Post purchase · ${RetailFormat.exact(currency, amountStr)}" else "Post purchase",
        enabled = postable,
        hideFooter = true,
    ) {
        RoseChipRow(label = null, options = listOf("Goods", "Supplies"), selected = goodsMode) { goodsMode = it }
        RoseFieldLabel("Scan supplier invoice / SKU")
        RoseScanField(
            query = scanQuery,
            placeholder = "INV / SKU · tap to scan",
            onQueryChange = { scanQuery = it },
            onScan = {
                if (scanQuery.isNotBlank()) {
                    val blank = lines.indexOfFirst { it.name.isBlank() }
                    if (blank >= 0) {
                        lines[blank] = lines[blank].copy(name = scanQuery, sku = scanQuery.take(8).uppercase())
                    } else {
                        lines.add(newCaptureLine().copy(name = scanQuery, sku = scanQuery.take(8).uppercase()))
                    }
                    scanQuery = ""
                }
            },
        )
        RoseLinesCard(title = "Receive lines", lineCount = lines.count { it.name.isNotBlank() }) {
            lines.forEachIndexed { index, line ->
                purchaseLineRow(
                    currency = currency,
                    line = line,
                    onChange = { lines[index] = it },
                    onRemove = { if (lines.size > 2) lines.removeAt(index) },
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "+ Add line",
                    style = RetailRose.viewAll(),
                    modifier = Modifier.clickable { lines.add(newCaptureLine()) },
                )
                Text(
                    "From PO · Favourites",
                    style = RetailRose.rowCaption(RetailRose.amber),
                    modifier = Modifier.clickable {
                        openPos.firstOrNull()?.let { linkedPo = it.title }
                        retailCatalogMasters(model).take(2).forEachIndexed { i, sku ->
                            val target = if (i < lines.size) i else {
                                lines.add(newCaptureLine(i))
                                lines.lastIndex
                            }
                            lines[target] = lines[target].copy(
                                sku = sku.code ?: sku.name.take(6).uppercase(),
                                name = sku.name,
                                rate = sku.purchasePrice ?: sku.sellPrice.orEmpty(),
                                unit = sku.unit ?: "pcs",
                                hsn = sku.hsn.orEmpty(),
                            )
                        }
                    },
                )
            }
        }
        RoseTotalsCard(
            payableLabel = "Purchase total",
            payable = RetailFormat.exact(currency, amountStr),
            tender = if (tender == "Pay later") "AP open" else tender,
        ) {
            RoseTotalRow("Goods", RetailFormat.exact(currency, goods.toPlainString()))
            RoseUnavailableGstRow(label = "Input GST · CGST+SGST")
            RoseTotalRow("Freight", RetailFormat.exact(currency, freight.ifBlank { "0" }))
        }
        RoseChipRow(
            label = "Tender / method",
            options = listOf("Pay later", "Pay now", "Part"),
            selected = tender,
        ) { tender = it }
        RoseFieldLabel("Supplier")
        RoseTextField(supplier, "Supplier name", onValueChange = { supplier = it })
        suppliers.take(4).forEach { row ->
            RosePickerRow(row.name, row.gstin?.let { "GSTIN $it" } ?: "Vendor", onClick = { supplier = row.name })
        }
        RoseFieldLabel("Location")
        RosePickerRow("Location", location, onClick = {
            location = locations.firstOrNull()?.name ?: location
        })
        locations.take(3).forEach { row ->
            RosePickerRow(row.name, row.kind, onClick = { location = row.name })
        }
        RoseFieldLabel("Linked PO")
        RosePickerRow("Linked PO", linkedPo.ifBlank { "Optional" }, onClick = {
            linkedPo = openPos.firstOrNull()?.title.orEmpty()
        })
        openPos.take(3).forEach { row ->
            RosePickerRow(row.title, row.status ?: "OPEN", onClick = { linkedPo = row.title })
        }
        RoseMoreDetails(expanded = moreOpen, onToggle = { moreOpen = !moreOpen }) {
            RoseFieldLabel("Freight")
            RoseTextField(freight, "0", onValueChange = { freight = it.filter { ch -> ch.isDigit() || ch == '.' }.take(10) })
            RoseTextField(note, "GRN note · supplier GSTIN", onValueChange = { note = it }, singleLine = false)
        }
        RoseStickyFooter(
            label = if (postable) "Post purchase · ${RetailFormat.exact(currency, amountStr)}" else "Post purchase",
            enabled = postable,
            caveat = "Draft preserved if you close or open a picker",
            onClick = {
                val filled = lines.filter { it.name.isNotBlank() }
                val paymentStatus = when (tender) {
                    "Pay now" -> "PAID"
                    "Part" -> "PARTIAL"
                    else -> "OPEN"
                }
                onSubmit(
                    RetailEventDraft(
                        eventType = "PURCHASE",
                        title = filled.firstOrNull()?.name?.let { "Purchase · $it" } ?: "Purchase · $supplier",
                        amount = amountStr,
                        note = listOfNotNull(
                            note.takeIf { it.isNotBlank() },
                            linkedPo.takeIf { it.isNotBlank() }?.let { "Linked PO: $it" },
                            "Mode: $goodsMode",
                            "Location: $location",
                        ).joinToString(" · ").ifBlank { null },
                        channelName = goodsMode,
                        counterpartyName = supplier,
                        paymentStatus = paymentStatus,
                        quantity = filled.firstOrNull()?.qty,
                        itemName = filled.firstOrNull()?.name,
                        lines = filled.map {
                            OperatingLineBody(
                                itemName = it.name,
                                quantity = it.qty,
                                unit = it.unit,
                                unitPrice = it.rate.ifBlank { null },
                            )
                        },
                        contract = OperatingContractBody(
                            receiptState = "PARTIAL",
                            paymentState = when (tender) {
                                "Pay now" -> "PAID"
                                "Part" -> "PART_PAID"
                                else -> "PAY_LATER"
                            },
                            locationName = location,
                            entryMode = "ITEMIZED",
                            goodsStatus = "RECEIVED",
                        ),
                    ),
                )
            },
        )
    }
}

@Composable
private fun purchaseLineRow(
    currency: String,
    line: CaptureLineDraft,
    onChange: (CaptureLineDraft) -> Unit,
    onRemove: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(RetailRose.field)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(line.sku.ifBlank { "SKU" }, style = RetailRose.rowCaption(RetailRose.amber))
            Text(
                RetailFormat.exact(currency, line.lineTotal().takeIf { it.signum() > 0 }?.toPlainString()),
                style = RetailRose.rowValue(RetailRose.amber),
            )
        }
        RoseTextField(line.name, "Item name", onValueChange = { onChange(line.copy(name = it)) })
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f)) {
                RoseFieldLabel("Qty")
                RoseTextField(line.qty, "1", onValueChange = {
                    onChange(line.copy(qty = it.filter { ch -> ch.isDigit() || ch == '.' }.take(9)))
                })
            }
            Column(Modifier.weight(1f)) {
                RoseFieldLabel("Rate")
                RoseTextField(line.rate, "0", onValueChange = {
                    onChange(line.copy(rate = it.filter { ch -> ch.isDigit() || ch == '.' }.take(12)))
                })
            }
            Column(Modifier.weight(1f)) {
                RoseFieldLabel("HSN")
                RoseTextField(line.hsn, "HSN", onValueChange = { onChange(line.copy(hsn = it.take(8))) })
            }
        }
        Text("Remove", style = RetailRose.rowCaption(RetailRose.warning), modifier = Modifier.clickable(onClick = onRemove))
    }
}

/** `2399:30194` Collection */
@Composable
private fun RetailCollectionSheet(
    model: OperatingProjectionDto,
    onDismiss: () -> Unit,
    onSubmit: (RetailEventDraft) -> Unit,
) {
    val currency = model.currency ?: "INR"
    val open = model.paymentReadiness.filter { it.relevance == "RECEIVABLE" }
    var selectedId by remember { mutableStateOf<String?>(open.firstOrNull()?.id) }
    var amount by remember { mutableStateOf(open.firstOrNull()?.amount.orEmpty()) }
    var tender by remember { mutableStateOf("UPI") }
    var reference by remember { mutableStateOf("") }
    var receiptPrint by remember { mutableStateOf("On") }
    var moreOpen by remember { mutableStateOf(false) }
    val row = open.firstOrNull { it.id == selectedId }
    val openAr = row?.amount?.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val receipt = amount.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val remaining = (openAr - receipt).max(BigDecimal.ZERO)
    val postable = row != null && receipt.signum() == 1

    retailCaptureScaffold(
        title = "Record Collection",
        subtitle = "${open.size} open AR · ${model.title ?: "Store"}",
        onDismiss = onDismiss,
        why = "Allocates open AR — never creates new sales. Receipt evidence ≠ separate revenue.",
        cta = if (postable) "Post collection · ${RetailFormat.exact(currency, amount)}" else "Post collection",
        enabled = postable,
        hideFooter = true,
    ) {
        RoseLinesCard(
            title = "Allocate against",
            lineCount = if (row == null) 0 else 1,
        ) {
            if (open.isEmpty()) RoseEmpty("Nothing to collect", "Credit sales with open AR appear here.")
            open.take(6).forEach { item ->
                paymentRow(item, currency, selectedId == item.id) {
                    selectedId = item.id
                    if (amount.isBlank()) amount = item.amount.orEmpty()
                }
            }
        }
        RoseTotalsCard(
            payableLabel = "Collect now",
            payable = RetailFormat.exact(currency, amount),
            tender = tender,
        ) {
            RoseTotalRow("Open AR", RetailFormat.exact(currency, row?.amount))
            RoseTotalRow("This receipt", RetailFormat.exact(currency, amount.ifBlank { "0" }))
            RoseTotalRow("Remaining", RetailFormat.exact(currency, remaining.toPlainString()))
        }
        RoseChipRow(
            label = "Method",
            options = listOf("UPI", "Cash", "Card", "Bank"),
            selected = tender,
        ) { tender = it }
        RoseFieldLabel("Customer")
        RosePickerRow("Customer", row?.counterpartyName ?: "Select open AR", onClick = { })
        RoseFieldLabel("Reference")
        RoseTextField(reference, "UPI / NEFT ref", onValueChange = { reference = it })
        RoseFieldLabel("Receipt print")
        RosePickerRow("Receipt print", receiptPrint, onClick = {
            receiptPrint = if (receiptPrint == "On") "Off" else "On"
        })
        RoseFieldLabel("Collected amount")
        RoseTextField(
            amount,
            row?.amount?.let { "Up to ${RetailFormat.exact(currency, it)}" } ?: "0",
            onValueChange = { amount = it.filter { ch -> ch.isDigit() || ch == '.' }.take(12) },
        )
        RoseMoreDetails(expanded = moreOpen, onToggle = { moreOpen = !moreOpen }) {
            Text("Notes · Proof · Tags", style = RetailRose.rowCaption())
        }
        RoseStickyFooter(
            label = if (postable) "Post collection · ${RetailFormat.exact(currency, amount)}" else "Post collection",
            enabled = postable,
            caveat = "Draft preserved if you close or open a picker",
            onClick = {
                onSubmit(
                    RetailEventDraft(
                        eventType = "COLLECTION",
                        title = "Collection · ${row?.title ?: "AR"}",
                        amount = amount,
                        note = listOfNotNull(
                            "Tender: $tender",
                            reference.takeIf { it.isNotBlank() }?.let { "Ref: $it" },
                            "Receipt print: $receiptPrint",
                        ).joinToString(" · "),
                        channelName = tender,
                        counterpartyName = row?.counterpartyName,
                        paymentStatus = "COLLECTED",
                        parentEventId = row?.id,
                        contract = OperatingContractBody(
                            paymentState = "RECEIVED",
                            receivedAmount = amount,
                            allocations = listOf(OperatingAllocationBody(row!!.id, amount)),
                        ),
                    ),
                )
            },
        )
    }
}

/** `2399:30253` Payment */
@Composable
private fun RetailPaymentSheet(
    model: OperatingProjectionDto,
    onDismiss: () -> Unit,
    onSubmit: (RetailEventDraft) -> Unit,
) {
    val currency = model.currency ?: "INR"
    val open = model.paymentReadiness.filter { it.relevance != "RECEIVABLE" }
    var selectedId by remember { mutableStateOf<String?>(open.firstOrNull()?.id) }
    var amount by remember { mutableStateOf(open.firstOrNull()?.amount.orEmpty()) }
    var tender by remember { mutableStateOf("Bank") }
    var reference by remember { mutableStateOf("") }
    var moreOpen by remember { mutableStateOf(false) }
    val row = open.firstOrNull { it.id == selectedId }
    val openAp = row?.amount?.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val paid = amount.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val remaining = (openAp - paid).max(BigDecimal.ZERO)
    val postable = row != null && paid.signum() == 1

    retailCaptureScaffold(
        title = "Record Payment",
        subtitle = "${open.size} open AP · settle supplier",
        onDismiss = onDismiss,
        why = "Settles open AP only — never posts new purchase cost.",
        cta = if (postable) "Post payment · ${RetailFormat.exact(currency, amount)}" else "Post payment",
        enabled = postable,
        hideFooter = true,
    ) {
        RoseLinesCard(title = "Allocate against", lineCount = if (row == null) 0 else 1) {
            if (open.isEmpty()) RoseEmpty("Nothing to pay", "Open purchases and expenses appear here.")
            open.take(6).forEach { item ->
                paymentRow(item, currency, selectedId == item.id) {
                    selectedId = item.id
                    if (amount.isBlank()) amount = item.amount.orEmpty()
                }
            }
        }
        RoseTotalsCard(
            payableLabel = "Pay now",
            payable = RetailFormat.exact(currency, amount),
            tender = tender,
        ) {
            RoseTotalRow("Open AP", RetailFormat.exact(currency, row?.amount))
            RoseTotalRow("This payment", RetailFormat.exact(currency, amount.ifBlank { "0" }))
            RoseTotalRow("Remaining", RetailFormat.exact(currency, remaining.toPlainString()))
        }
        RoseChipRow(
            label = "Method",
            options = listOf("Bank", "UPI", "Cash", "Cheque"),
            selected = tender,
        ) { tender = it }
        RoseFieldLabel("Payee")
        RosePickerRow("Payee", row?.counterpartyName ?: "Select payable", onClick = { })
        RoseFieldLabel("Reference")
        RoseTextField(reference, "NEFT / cheque no.", onValueChange = { reference = it })
        RoseFieldLabel("Linked purchase")
        RosePickerRow("Linked purchase", row?.title ?: "—", onClick = { })
        RoseFieldLabel("Payment amount")
        RoseTextField(amount, "0", onValueChange = { amount = it.filter { ch -> ch.isDigit() || ch == '.' }.take(12) })
        RoseMoreDetails(expanded = moreOpen, onToggle = { moreOpen = !moreOpen }) {
            Text("Notes · Proof · Tags", style = RetailRose.rowCaption())
        }
        RoseStickyFooter(
            label = if (postable) "Post payment · ${RetailFormat.exact(currency, amount)}" else "Post payment",
            enabled = postable,
            caveat = "Draft preserved if you close or open a picker",
            onClick = {
                onSubmit(
                    RetailEventDraft(
                        eventType = "PAYMENT",
                        title = "Payment · ${row?.title ?: "AP"}",
                        amount = amount,
                        note = listOfNotNull(
                            "Tender: $tender",
                            reference.takeIf { it.isNotBlank() }?.let { "Ref: $it" },
                        ).joinToString(" · "),
                        channelName = tender,
                        counterpartyName = row?.counterpartyName,
                        paymentStatus = "PAID",
                        parentEventId = row?.id,
                        contract = OperatingContractBody(
                            paymentState = "PAID",
                            receivedAmount = amount,
                            allocations = listOf(OperatingAllocationBody(row!!.id, amount)),
                        ),
                    ),
                )
            },
        )
    }
}

/** `2399:30306` Expense */
@Composable
private fun RetailExpenseSheet(
    model: OperatingProjectionDto,
    onDismiss: () -> Unit,
    onSubmit: (RetailEventDraft) -> Unit,
) {
    val currency = model.currency ?: "INR"
    var mode by remember { mutableStateOf("Store") }
    var amount by remember { mutableStateOf("") }
    var payState by remember { mutableStateOf("Paid now") }
    var typeChip by remember { mutableStateOf("Packaging") }
    var typeCustom by remember { mutableStateOf("") }
    var payee by remember { mutableStateOf("") }
    val defaultPeriod = RetailFormat.month(model.startDate)
    var periodChip by remember { mutableStateOf(defaultPeriod) }
    var periodCustom by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var moreOpen by remember { mutableStateOf(false) }
    val typeOptions = listOf("Packaging", "Rent", "Utilities", "Transport", "Other")
    val expenseType = typeCustom.ifBlank { typeChip }
    val period = if (periodChip == "Custom") periodCustom.ifBlank { defaultPeriod } else periodChip
    val postable = amount.toBigDecimalOrNull()?.signum() == 1

    retailCaptureScaffold(
        title = "Record Expense",
        subtitle = "Store ops · ${model.title ?: "Store"} · cost",
        onDismiss = onDismiss,
        why = "Expense is cost; payment state is separate. Never invent stock from expense.",
        cta = if (postable) "Post expense · ${RetailFormat.exact(currency, amount)}" else "Post expense",
        enabled = postable,
        hideFooter = true,
    ) {
        RoseChipRow(label = null, options = listOf("Store", "Ops", "Other"), selected = mode) { mode = it }
        RoseTotalsCard(
            payableLabel = "Expense",
            payable = RetailFormat.exact(currency, amount),
            tender = expenseType,
        ) {
            RoseTotalRow("Amount", RetailFormat.exact(currency, amount.ifBlank { "0" }))
            RoseUnavailableGstRow(label = "GST (if any)", caption = "Unavailable / ₹0 — never invent GST on expense")
            RoseTotalRow("Cash impact", if (payState == "Paid now") "Paid now" else "Pay later")
        }
        RoseChipRow(
            label = "Payment state",
            options = listOf("Paid now", "Pay later"),
            selected = payState,
        ) { payState = it }
        RoseFieldLabel("Type")
        RoseChipRow(label = null, options = typeOptions, selected = typeChip) {
            typeChip = it
            if (it != "Other") typeCustom = ""
        }
        if (typeChip == "Other") {
            RoseTextField(typeCustom, "Custom type", onValueChange = { typeCustom = it })
        }
        RoseFieldLabel("Payee")
        RoseTextField(payee, "Optional vendor", onValueChange = { payee = it })
        RoseFieldLabel("Period")
        RoseChipRow(
            label = null,
            options = listOf(defaultPeriod, "This week", "Custom"),
            selected = periodChip,
        ) { periodChip = it }
        if (periodChip == "Custom") {
            RoseTextField(periodCustom, "e.g. Nov 2026", onValueChange = { periodCustom = it })
        }
        RoseAmountHero("Expense", RetailFormat.exact(currency, amount), caption = mode)
        RoseFieldLabel("Amount")
        RoseTextField(amount, "0", onValueChange = { amount = it.filter { ch -> ch.isDigit() || ch == '.' }.take(12) })
        RoseMoreDetails(expanded = moreOpen, onToggle = { moreOpen = !moreOpen }) {
            RoseTextField(note, "Notes · Bill photo · Tags", onValueChange = { note = it }, singleLine = false)
        }
        RoseStickyFooter(
            label = if (postable) "Post expense · ${RetailFormat.exact(currency, amount)}" else "Post expense",
            enabled = postable,
            caveat = "Draft preserved if you close or open a picker",
            onClick = {
                onSubmit(
                    RetailEventDraft(
                        eventType = "SPEND",
                        title = "$mode · $expenseType",
                        amount = amount,
                        note = listOfNotNull(
                            note.takeIf { it.isNotBlank() },
                            "Period: $period",
                        ).joinToString(" · ").ifBlank { null },
                        counterpartyName = payee.takeIf { it.isNotBlank() },
                        paymentStatus = if (payState == "Paid now") "PAID" else "OPEN",
                        contract = OperatingContractBody(
                            expenseCategory = "$mode/$expenseType",
                            paymentState = if (payState == "Paid now") "PAID" else "PAY_LATER",
                        ),
                    ),
                )
            },
        )
    }
}

/** `2399:30359` Stock update */
@Composable
private fun RetailStockUpdateSheet(
    model: OperatingProjectionDto,
    onDismiss: () -> Unit,
    onSubmit: (RetailEventDraft) -> Unit,
) {
    var action by remember { mutableStateOf("COUNT") }
    var sku by remember { mutableStateOf("") }
    var qty by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    val skus = retailCatalogMasters(model)
    val postable = sku.isNotBlank() && qty.toBigDecimalOrNull() != null

    retailCaptureScaffold(
        title = "Stock Update",
        subtitle = "Count · damage · adjust",
        onDismiss = onDismiss,
        why = "Stock posts quantity delta only — not a sale or purchase.",
        cta = "Post stock",
        enabled = postable,
        hideFooter = true,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("COUNT", "DAMAGE", "ADJUST").forEach { mode ->
                RoseModeChip(mode, mode == action, onClick = { action = mode })
            }
        }
        RoseSectionCard("SKU") {
            RoseTextField(sku, "Item", onValueChange = { sku = it })
            skus.take(4).forEach { master ->
                RosePickerRow(master.name, master.unit ?: "pcs", onClick = { sku = master.name })
            }
            RoseFieldLabel("Quantity delta")
            RoseTextField(qty, "+/− units", onValueChange = { qty = it.filter { ch -> ch.isDigit() || ch == '-' || ch == '.' }.take(9) })
            RoseTextField(reason, "Reason", onValueChange = { reason = it })
        }
        RoseStickyFooter(
            label = "Post stock move",
            enabled = postable,
            caveat = "Signed delta on hand",
            onClick = {
                val item = skus.firstOrNull { it.name.equals(sku, true) }
                onSubmit(
                    RetailEventDraft(
                        eventType = "STOCK",
                        title = "Stock · $action · $sku",
                        amount = null,
                        note = reason.takeIf { it.isNotBlank() },
                        quantity = qty,
                        unit = item?.unit,
                        itemName = sku,
                        contract = OperatingContractBody(stockAction = action, stockReason = reason.takeIf { it.isNotBlank() }),
                    ),
                )
            },
        )
    }
}

private enum class OrderSheetMode { SalesOrder, PurchaseOrder }

/** `2399:30418` Sales order · `2443:29784` Purchase order — D1-A combined + mode chips */
@Composable
private fun RetailOrderSheet(
    model: OperatingProjectionDto,
    onDismiss: () -> Unit,
    onSubmit: (RetailEventDraft) -> Unit,
    companyId: String? = null,
    onMasterSubmit: (RetailEventDraft) -> Unit = onSubmit,
) {
    var mode by remember { mutableStateOf(OrderSheetMode.SalesOrder) }
    var stateChip by remember { mutableStateOf(if (mode == OrderSheetMode.SalesOrder) "Confirmed" else "Sent") }
    var party by remember { mutableStateOf("") }
    var promise by remember { mutableStateOf("") }
    var deliverTo by remember { mutableStateOf(if (mode == OrderSheetMode.SalesOrder) "Pickup · store" else "Back store") }
    var placeOfSupply by remember { mutableStateOf("Maharashtra · intra-state") }
    var search by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var moreOpen by remember { mutableStateOf(false) }
    var createKind by remember { mutableStateOf<RetailMasterFormKind?>(null) }
    val currency = model.currency ?: "INR"
    val catalog = retailCatalogMasters(model)
    val lines = remember { mutableStateListOf(newCaptureLine(1), newCaptureLine(2)) }
    val goods = lines.goodsTotal()
    val amountStr = goods.stripTrailingZeros().toPlainString()
    val postable = goods.signum() == 1 || lines.any { it.name.isNotBlank() }
    val isPo = mode == OrderSheetMode.PurchaseOrder
    val suppliers = model.masters.orEmpty().filter { it.kind.equals("SUPPLIER", true) }
    val customers = model.masters.orEmpty().filter {
        it.kind.equals("CUSTOMER", true) || it.kind.equals("BUYER", true)
    }

    Box {
    retailCaptureScaffold(
        title = if (isPo) "Purchase Order" else "Sales Order",
        subtitle = if (isPo) {
            "Supplier commitment · not AP"
        } else {
            "Customer commitment · not revenue"
        },
        onDismiss = onDismiss,
        why = if (isPo) {
            "Purchase order is commitment only — no AP, no stock, no GST posting. Cost + stock on goods receipt / posted Purchase."
        } else {
            "Sales order is commitment only — revenue + stock on posted Sale / POS Charge. Never posts GST as sale by itself."
        },
        cta = if (postable) {
            if (isPo) "Save purchase order · ${RetailFormat.exact(currency, amountStr)}"
            else "Save sales order · ${RetailFormat.exact(currency, amountStr)}"
        } else {
            if (isPo) "Save purchase order" else "Save sales order"
        },
        enabled = postable,
        hideFooter = true,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            RoseModeChip("Sales order", !isPo, onClick = {
                mode = OrderSheetMode.SalesOrder
                stateChip = "Confirmed"
                deliverTo = "Pickup · store"
            })
            RoseModeChip("Purchase order", isPo, onClick = {
                mode = OrderSheetMode.PurchaseOrder
                stateChip = "Sent"
                deliverTo = "Back store"
            })
        }
        RoseChipRow(
            label = null,
            options = if (isPo) listOf("Sent", "Draft", "Partial") else listOf("Confirmed", "Hold", "Draft"),
            selected = stateChip,
        ) { stateChip = it }

        RoseSectionCard(if (isPo) "Stock items" else "Catalogue") {
            RoseScanField(
                query = search,
                placeholder = if (isPo) "Search SKU / item" else "Search SKU, name or barcode",
                onQueryChange = { search = it },
                onScan = {},
            )
            val filtered = catalog.filter {
                search.isBlank() || it.name.contains(search, true) || (it.code?.contains(search, true) == true)
            }.take(6)
            if (filtered.isEmpty()) {
                Text("No catalogue rows yet — add lines manually.", style = RetailRose.rowCaption())
            }
            filtered.forEach { sku ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(RetailRose.field)
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(sku.name, style = RetailRose.rowTitle(), maxLines = 1)
                        Text(
                            listOfNotNull(sku.code, sku.unit, sku.hsn?.let { "HSN $it" }).joinToString(" · "),
                            style = RetailRose.rowCaption(),
                            maxLines = 1,
                        )
                    }
                    Text(
                        RetailFormat.exact(
                            currency,
                            if (isPo) sku.purchasePrice ?: sku.sellPrice else sku.sellPrice ?: sku.purchasePrice,
                        ),
                        style = RetailRose.rowValue(),
                    )
                    Text(
                        "Add",
                        style = RetailRose.chipLabel(true).copy(color = Color.White),
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(RetailRose.rose)
                            .clickable {
                                val blank = lines.indexOfFirst { it.name.isBlank() }
                                val draft = CaptureLineDraft(
                                    id = System.nanoTime(),
                                    sku = sku.code ?: sku.name.take(6).uppercase(),
                                    name = sku.name,
                                    qty = "1",
                                    rate = (if (isPo) sku.purchasePrice else sku.sellPrice)
                                        ?: sku.purchasePrice ?: sku.sellPrice.orEmpty(),
                                    hsn = sku.hsn.orEmpty(),
                                    unit = sku.unit ?: "pcs",
                                )
                                if (blank >= 0) lines[blank] = draft else lines.add(draft)
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }
            // Figma 2443:29784 / 2399:30418 — denser create CTA under browser.
            RoseDenseCreateCta(
                title = if (isPo) "+ New stock item" else "+ New product",
                hint = if (isPo) "Name · unit · HSN · GST" else "Name · HSN · GST · MRP",
                onClick = {
                    createKind = if (isPo) RetailMasterFormKind.StockItem else RetailMasterFormKind.Product
                },
            )
        }

        RoseLinesCard(
            title = if (isPo) "PO lines" else "SO lines",
            lineCount = lines.count { it.name.isNotBlank() },
        ) {
            lines.forEachIndexed { index, line ->
                orderLineRow(
                    currency = currency,
                    line = line,
                    onChange = { lines[index] = it },
                    onRemove = { if (lines.size > 1) lines.removeAt(index) },
                )
            }
            Text(
                "+ Add line",
                style = RetailRose.viewAll(),
                modifier = Modifier.clickable { lines.add(newCaptureLine()) },
            )
        }

        RoseTotalsCard(
            payableLabel = if (isPo) "PO value" else "SO value",
            payable = RetailFormat.exact(currency, amountStr),
            tender = stateChip,
        ) {
            if (isPo) {
                RoseTotalRow("Goods", RetailFormat.exact(currency, amountStr))
                RoseUnavailableGstRow(
                    label = "Est. input GST · on GRN",
                    caption = "Estimate label only — never posted from this sheet",
                )
                RoseTotalRow("Freight (est.)", "₹0")
            } else {
                RoseTotalRow("Lines value", RetailFormat.exact(currency, amountStr))
                RoseUnavailableGstRow(
                    label = "Est. GST · on bill",
                    caption = "Estimate label only — never posted from this sheet",
                )
                RoseTotalRow("Balance due on sale", RetailFormat.exact(currency, amountStr))
            }
        }

        RoseFieldLabel(if (isPo) "Supplier" else "Customer")
        RoseTextField(party, if (isPo) "Supplier" else "Customer", onValueChange = { party = it })
        (if (isPo) suppliers else customers).take(4).forEach { row ->
            RosePickerRow(row.name, row.kind, onClick = { party = row.name })
        }
        RoseFieldLabel(if (isPo) "Expect by" else "Fulfil by")
        RoseTextField(promise, "e.g. 05 Nov", onValueChange = { promise = it })
        RoseFieldLabel(if (isPo) "Deliver to" else "Ship / pickup")
        RoseTextField(deliverTo, if (isPo) "Back store" else "Pickup · store", onValueChange = { deliverTo = it })
        RoseFieldLabel("Place of supply")
        RosePickerRow("Place of supply", placeOfSupply, onClick = {
            placeOfSupply = "Maharashtra (27) · intra-state"
        })
        RoseMoreDetails(expanded = moreOpen, onToggle = { moreOpen = !moreOpen }) {
            RoseTextField(note, "Notes · Terms · Tags", onValueChange = { note = it }, singleLine = false)
        }
        RoseStickyFooter(
            label = if (postable) {
                if (isPo) "Save purchase order · ${RetailFormat.exact(currency, amountStr)}"
                else "Save sales order · ${RetailFormat.exact(currency, amountStr)}"
            } else {
                if (isPo) "Save purchase order" else "Save sales order"
            },
            enabled = postable,
            caveat = if (isPo) {
                "Commitment only · AP, stock & input GST post on Purchase / GRN"
            } else {
                "Commitment only · revenue, stock & GST post on Sale / POS"
            },
            onClick = {
                val filled = lines.filter { it.name.isNotBlank() }
                onSubmit(
                    RetailEventDraft(
                        eventType = "ORDER",
                        title = if (isPo) {
                            "PO · ${party.ifBlank { "Supplier" }}"
                        } else {
                            "Sales order · ${party.ifBlank { "Customer" }}"
                        },
                        amount = amountStr.takeIf { goods.signum() == 1 },
                        note = listOfNotNull(
                            note.takeIf { it.isNotBlank() },
                            "Deliver: $deliverTo",
                            "POS: $placeOfSupply",
                            "State: $stateChip",
                        ).joinToString(" · "),
                        counterpartyName = party.takeIf { it.isNotBlank() },
                        dueAt = promise.takeIf { it.isNotBlank() },
                        lines = filled.map {
                            OperatingLineBody(
                                itemName = it.name,
                                quantity = it.qty,
                                unit = it.unit,
                                unitPrice = it.rate.ifBlank { null },
                            )
                        }.ifEmpty { null },
                        contract = OperatingContractBody(
                            orderSide = if (isPo) "PURCHASE" else "SALES",
                            orderStatus = when (stateChip) {
                                "Hold" -> "ON_HOLD"
                                "Draft" -> "DRAFT"
                                "Partial" -> "PARTIAL"
                                "Sent", "Confirmed" -> "OPEN"
                                else -> "OPEN"
                            },
                            promisedAt = promise.takeIf { it.isNotBlank() },
                            estimatedAmount = amountStr.takeIf { goods.signum() == 1 },
                            placeOfSupply = placeOfSupply,
                            locationName = deliverTo,
                            holdStatus = if (stateChip == "Hold") "HELD" else null,
                        ),
                    ),
                )
            },
        )
    }

    createKind?.let { formKind ->
        OperatingSheetScrim(
            onDismiss = { createKind = null },
            accent = RetailRose.rose,
            surface = RetailRose.cardSlate,
            border = RetailRose.roseBorder.copy(alpha = 0.45f),
        ) {
            RetailMasterFormSheet(
                kind = formKind,
                model = model,
                companyId = companyId,
                onDismiss = { createKind = null },
                onSubmit = { draft ->
                    onMasterSubmit(draft)
                    createKind = null
                },
            )
        }
    }
    }
}

@Composable
private fun orderLineRow(
    currency: String,
    line: CaptureLineDraft,
    onChange: (CaptureLineDraft) -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(RetailRose.field)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(line.sku.ifBlank { "SKU" }, style = RetailRose.rowCaption(RetailRose.amber))
            RoseTextField(line.name, "Item", onValueChange = { onChange(line.copy(name = it)) })
            Text(
                listOfNotNull(
                    line.rate.takeIf { it.isNotBlank() }?.let { RetailFormat.exact(currency, it) },
                    line.unit,
                    line.hsn.takeIf { it.isNotBlank() }?.let { "HSN $it" },
                ).joinToString(" · ").ifBlank { "Set qty · rate" },
                style = RetailRose.rowCaption(),
            )
        }
        val qtyInt = line.qty.toIntOrNull() ?: 1
        RoseStepper(
            quantity = qtyInt,
            onDecrement = { if (qtyInt > 1) onChange(line.copy(qty = (qtyInt - 1).toString())) else onRemove() },
            onIncrement = { onChange(line.copy(qty = (qtyInt + 1).toString())) },
        )
        Text(
            RetailFormat.exact(currency, line.lineTotal().takeIf { it.signum() > 0 }?.toPlainString()),
            style = RetailRose.rowValue(RetailRose.amber),
        )
    }
    RoseTextField(line.rate, "Rate", onValueChange = {
        onChange(line.copy(rate = it.filter { ch -> ch.isDigit() || ch == '.' }.take(12)))
    })
}

/** `2399:30482` Status / assignment */
@Composable
private fun RetailStatusSheet(
    model: OperatingProjectionDto,
    onDismiss: () -> Unit,
    onSubmit: (RetailEventDraft) -> Unit,
) {
    val orders = model.activity.filter { it.eventType == "ORDER" }.take(6)
    var selectedId by remember { mutableStateOf<String?>(orders.firstOrNull()?.id) }
    var mode by remember { mutableStateOf("Dispatch") }
    var status by remember { mutableStateOf("Ready to pick") }
    var assignee by remember { mutableStateOf("") }
    var due by remember { mutableStateOf("Today") }
    var notify by remember { mutableStateOf("Customer WhatsApp") }
    var moreOpen by remember { mutableStateOf(false) }
    val row = orders.firstOrNull { it.id == selectedId }
    val postable = row != null
    val statusToken = when (status) {
        "Picking" -> "PICKING"
        "Ready" -> "READY"
        "Dispatched" -> "DISPATCHED"
        else -> "READY_TO_PICK"
    }

    retailCaptureScaffold(
        title = "Status / Assignment",
        subtitle = "Floor board · ops only",
        onDismiss = onDismiss,
        why = "Operational state only — does not post revenue or stock by itself. Sale / POS Charge posts facts.",
        cta = "Update status",
        enabled = postable,
        hideFooter = true,
    ) {
        RoseChipRow(label = null, options = listOf("Dispatch", "Fulfil", "Pick"), selected = mode) { mode = it }
        RoseLinesCard(title = "Source", lineCount = orders.size) {
            if (orders.isEmpty()) RoseEmpty("No open orders", "Record an order first.")
            orders.forEach { order ->
                val picked = order.id == selectedId
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (picked) RetailRose.rose.copy(alpha = 0.16f) else RetailRose.field)
                        .border(1.dp, if (picked) RetailRose.rose else Color.White.copy(alpha = 0.06f), RoundedCornerShape(10.dp))
                        .clickable { selectedId = order.id }
                        .padding(12.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(order.title, style = RetailRose.rowTitle(), maxLines = 1)
                        Text(order.status ?: "OPEN", style = RetailRose.rowCaption())
                    }
                    Text(order.status ?: "Ready", style = RetailRose.rowCaption(RetailRose.amber))
                }
            }
        }
        RoseChipRow(
            label = "Next status",
            options = listOf("Ready to pick", "Picking", "Ready", "Dispatched"),
            selected = status,
        ) { status = it }
        RoseFieldLabel("Assignee")
        RoseTextField(assignee, "Staff · floor · courier", onValueChange = { assignee = it })
        RoseFieldLabel("Due")
        RoseTextField(due, "Today · 6pm", onValueChange = { due = it })
        RoseFieldLabel("Notify")
        RosePickerRow("Notify", notify, onClick = {
            notify = if (notify == "Customer WhatsApp") "None" else "Customer WhatsApp"
        })
        RoseMoreDetails(expanded = moreOpen, onToggle = { moreOpen = !moreOpen }) {
            Text("Notes · Photo · Tags", style = RetailRose.rowCaption())
        }
        RoseStickyFooter(
            label = "Update status",
            enabled = postable,
            caveat = "Draft preserved if you close or open a picker",
            onClick = {
                onSubmit(
                    RetailEventDraft(
                        eventType = "ORDER",
                        title = "Status · ${row?.title ?: "Order"}",
                        amount = null,
                        note = listOfNotNull(
                            assignee.takeIf { it.isNotBlank() }?.let { "Assigned: $it" },
                            "Mode: $mode",
                            "Due: $due",
                            "Notify: $notify",
                        ).joinToString(" · "),
                        parentEventId = row?.id,
                        activityStatus = statusToken,
                        contract = OperatingContractBody(
                            orderStatus = statusToken,
                            shipmentStatus = statusToken,
                            staffName = assignee.takeIf { it.isNotBlank() },
                            deliveryStatus = mode,
                        ),
                    ),
                )
            },
        )
    }
}

/** `2399:30539` Budget revision */
@Composable
private fun RetailBudgetSheet(
    model: OperatingProjectionDto,
    onDismiss: () -> Unit,
    onSubmit: (RetailEventDraft) -> Unit,
) {
    val currency = model.currency ?: "INR"
    var metric by remember { mutableStateOf("Sales target") }
    var amount by remember { mutableStateOf(model.target?.value.orEmpty()) }
    var period by remember { mutableStateOf(RetailFormat.month(model.startDate)) }
    var reason by remember { mutableStateOf("") }
    var owner by remember { mutableStateOf("") }
    var moreOpen by remember { mutableStateOf(false) }
    val previous = model.target?.value
    val prevBd = previous?.toBigDecimalOrNull()
    val nextBd = amount.toBigDecimalOrNull()
    val delta = if (prevBd != null && nextBd != null) nextBd - prevBd else null
    val postable = amount.toBigDecimalOrNull()?.signum() == 1

    retailCaptureScaffold(
        title = "Budget / Target Revision",
        subtitle = "Owner plan · versioned",
        onDismiss = onDismiss,
        why = "Versioned + effective-dated plan. Never rewrite posted sales to match target.",
        cta = if (postable) "Save revision · ${RetailFormat.exact(currency, amount)}" else "Save revision",
        enabled = postable,
        hideFooter = true,
    ) {
        RoseChipRow(label = null, options = listOf("Sales target", "Budget"), selected = metric) { metric = it }
        RoseTotalsCard(
            payableLabel = "New target",
            payable = RetailFormat.exact(currency, amount),
            tender = period,
        ) {
            RoseTotalRow("Previous v1", RetailFormat.exact(currency, previous))
            RoseTotalRow(
                "Delta",
                delta?.let {
                    val sign = if (it.signum() >= 0) "+ " else "− "
                    sign + RetailFormat.exact(currency, it.abs().toPlainString())
                } ?: "—",
            )
            RoseTotalRow("Effective", "On save")
        }
        RoseFieldLabel("Period")
        RoseChipRow(
            label = null,
            options = listOf(RetailFormat.month(model.startDate), "Next month", "Custom"),
            selected = period,
        ) { period = it }
        RoseFieldLabel("Target amount")
        RoseTextField(amount, model.target?.value ?: "0", onValueChange = {
            amount = it.filter { ch -> ch.isDigit() || ch == '.' }.take(14)
        })
        RoseFieldLabel("Reason")
        RoseTextField(reason, "Festive demand", onValueChange = { reason = it })
        RoseFieldLabel("Owner")
        RoseTextField(owner, "Optional", onValueChange = { owner = it })
        RoseMoreDetails(expanded = moreOpen, onToggle = { moreOpen = !moreOpen }) {
            Text("Notes · Attachment · Tags", style = RetailRose.rowCaption())
        }
        RoseStickyFooter(
            label = if (postable) "Save revision · ${RetailFormat.exact(currency, amount)}" else "Save revision",
            enabled = postable,
            caveat = "Draft preserved if you close or open a picker",
            onClick = {
                onSubmit(
                    RetailEventDraft(
                        eventType = "BUDGET",
                        title = "Budget · $metric",
                        amount = amount,
                        note = listOfNotNull(
                            reason.takeIf { it.isNotBlank() },
                            "Period: $period",
                            owner.takeIf { it.isNotBlank() }?.let { "Owner: $it" },
                        ).joinToString(" · ").ifBlank { null },
                        contract = OperatingContractBody(estimatedAmount = amount),
                    ),
                )
            },
        )
    }
}

/** `2399:30597` Issue / update */
@Composable
private fun RetailIssueSheet(
    model: OperatingProjectionDto,
    onDismiss: () -> Unit,
    onSubmit: (RetailEventDraft) -> Unit,
) {
    var kind by remember { mutableStateOf("Issue") }
    var severity by remember { mutableStateOf("Alert") }
    var title by remember { mutableStateOf("") }
    var owner by remember { mutableStateOf("") }
    var link by remember { mutableStateOf("Register · today") }
    var note by remember { mutableStateOf("") }
    var moreOpen by remember { mutableStateOf(false) }
    val postable = title.isNotBlank()
    val eventType = when (kind) {
        "Issue" -> "ISSUE"
        "Approval" -> "UPDATE"
        else -> "UPDATE"
    }

    retailCaptureScaffold(
        title = "Issue / Approval / Update",
        subtitle = "Register · audit trail",
        onDismiss = onDismiss,
        why = "Audit / action / permission — approval ≠ disbursement. Shift variance opens Issue, not a sale edit.",
        cta = if (kind == "Issue") "Post issue" else "Submit $kind",
        enabled = postable,
        hideFooter = true,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            RoseModeChip("Issue", kind == "Issue", onClick = { kind = "Issue" })
            RoseModeChip("Approval", kind == "Approval", locked = "Use tile flow")
            RoseModeChip("Update", kind == "Update", onClick = { kind = "Update" })
        }
        RoseSectionCard("Linked") {
            Text(link, style = RetailRose.rowTitle())
            Text("Attachment chrome only — no upload API", style = RetailRose.rowCaption())
        }
        RoseChipRow(
            label = "Severity",
            options = listOf("Alert", "Warn", "Info"),
            selected = severity,
        ) { severity = it }
        RoseFieldLabel("Title")
        RoseTextField(title, "Short title", onValueChange = { title = it.take(80) })
        RoseFieldLabel("Owner / assignee")
        RoseTextField(owner, "Staff", onValueChange = { owner = it })
        RoseFieldLabel("Link")
        RoseTextField(link, "Register · shift", onValueChange = { link = it })
        RoseMoreDetails(expanded = moreOpen, onToggle = { moreOpen = !moreOpen }) {
            RoseTextField(note, "Notes · Evidence · Tags", onValueChange = { note = it }, singleLine = false)
            RosePickerRow("Attachment", "Add photo (chrome only)", onClick = null)
        }
        RoseStickyFooter(
            label = if (kind == "Issue") "Post issue" else "Submit $kind",
            enabled = postable,
            caveat = "Draft preserved if you close or open a picker",
            onClick = {
                onSubmit(
                    RetailEventDraft(
                        eventType = eventType,
                        title = title.trim(),
                        amount = null,
                        note = listOfNotNull(
                            note.takeIf { it.isNotBlank() },
                            "Severity: $severity",
                            owner.takeIf { it.isNotBlank() }?.let { "Owner: $it" },
                            "Link: $link",
                        ).joinToString(" · "),
                        contract = OperatingContractBody(staffName = owner.takeIf { it.isNotBlank() }),
                    ),
                )
            },
        )
    }
}

@Composable
private fun retailCaptureScaffold(
    title: String,
    subtitle: String,
    onDismiss: () -> Unit,
    why: String,
    cta: String,
    enabled: Boolean,
    hideFooter: Boolean = false,
    content: @Composable () -> Unit,
) {
    RetailSheetBody(Modifier.verticalScroll(rememberScrollState())) {
        RoseGrabber()
        RoseSheetHeader(title = title, subtitle = subtitle, onClose = onDismiss)
        content()
        RoseWhyBox(why, tinted = true)
        if (!hideFooter) {
            RoseStickyFooter(label = cta, enabled = enabled, caveat = "Draft cleared on successful post", onClick = {})
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun paymentRow(
    item: OperatingPaymentDto,
    currency: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) RetailRose.rose.copy(alpha = 0.16f) else RetailRose.field)
            .border(1.dp, if (selected) RetailRose.rose else Color.White.copy(alpha = 0.06f), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(item.title, style = RetailRose.rowTitle(), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                listOfNotNull(item.counterpartyName, item.dueAt?.take(10)).joinToString(" · "),
                style = RetailRose.rowCaption(),
            )
        }
        Text(RetailFormat.exact(currency, item.amount), style = RetailRose.rowValue(RetailRose.amber))
    }
}

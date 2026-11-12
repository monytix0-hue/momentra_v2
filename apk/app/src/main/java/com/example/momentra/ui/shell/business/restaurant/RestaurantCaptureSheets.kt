package com.example.momentra.ui.shell.business.restaurant

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.api.OperatingAllocationBody
import com.example.momentra.data.api.OperatingContractBody
import com.example.momentra.data.api.OperatingLineBody
import com.example.momentra.data.api.OperatingMasterDto
import com.example.momentra.data.api.OperatingPaymentDto
import com.example.momentra.data.api.OperatingProjectionDto
import com.example.momentra.ui.shell.business.mto.MtoToastHost
import com.example.momentra.ui.shell.business.mto.MtoToastKind
import com.example.momentra.ui.shell.business.mto.MtoToastSpec
import com.example.momentra.ui.theme.PlusJakartaSans
import com.example.momentra.ui.theme.RestaurantEmber
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
internal fun RestaurantCaptureSheet(
    kind: RestaurantSheetKind,
    model: OperatingProjectionDto,
    onDismiss: () -> Unit,
    onSubmit: (RestaurantEventDraft) -> Unit,
    onMasterSubmit: ((RestaurantEventDraft) -> Unit)? = null,
) {
    when (kind) {
        RestaurantSheetKind.Purchase -> RestaurantPurchaseSheet(model, onDismiss, onSubmit)
        RestaurantSheetKind.Collection -> RestaurantCollectionSheet(model, onDismiss, onSubmit)
        RestaurantSheetKind.Payment -> RestaurantPaymentSheet(model, onDismiss, onSubmit)
        RestaurantSheetKind.Spend -> RestaurantExpenseSheet(model, onDismiss, onSubmit)
        RestaurantSheetKind.Stock -> RestaurantStockSheet(model, onDismiss, onSubmit)
        RestaurantSheetKind.Order -> RestaurantOrderCaptureSheet(model, onDismiss, onSubmit, onMasterSubmit)
        RestaurantSheetKind.Budget -> RestaurantBudgetSheet(model, onDismiss, onSubmit)
        RestaurantSheetKind.Issue -> RestaurantIssueSheet(model, onDismiss, onSubmit)
        else -> Unit
    }
}

private data class CaptureLineDraft(
    val id: Long,
    var sku: String = "",
    var name: String = "",
    var qty: String = "1",
    var rate: String = "",
    var unit: String = "pcs",
    var gst: String = "",
    var wasQty: String = "",
    var note: String = "",
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
    qty = "1",
)

// ── Phase 4: Purchase `2400:30129` ───────────────────────────────────────────

@Composable
private fun RestaurantPurchaseSheet(
    model: OperatingProjectionDto,
    onDismiss: () -> Unit,
    onSubmit: (RestaurantEventDraft) -> Unit,
) {
    val currency = model.currency ?: "INR"
    val kitchen = RestaurantCaptureCopy.kitchenLine(model.title, model.location)
    val owner = RestaurantCaptureCopy.ownerHint(model.partyName ?: model.participants.firstOrNull())
    var goodsMode by remember { mutableStateOf("Ingredients") }
    var scanQuery by remember { mutableStateOf("") }
    var supplier by remember { mutableStateOf("") }
    var tender by remember { mutableStateOf("Pay later") }
    var note by remember { mutableStateOf("") }
    var moreOpen by remember { mutableStateOf(false) }
    val lines = remember { mutableStateListOf(newCaptureLine(1), newCaptureLine(2)) }
    val suppliers = model.masters.orEmpty().filter { it.kind.equals("SUPPLIER", true) }
    val filledCount = lines.count { it.name.isNotBlank() }
    val goods = lines.goodsTotal()
    val amountStr = goods.stripTrailingZeros().toPlainString()
    val amountLabel = goods.takeIf { it.signum() == 1 }?.let { RestaurantFormat.exact(currency, amountStr) }
    val postable = supplier.isNotBlank() && goods.signum() == 1 && filledCount >= 1
    val gstOn = model.gstTracking.equals("YES", true)
    val gstEstimate = if (gstOn) {
        RestaurantFormat.gstEstimate(
            lineAmounts = lines.map { it.lineTotal() },
            lineRates = lines.map { it.gst.takeIf { g -> g.isNotBlank() } },
            inclusive = false,
        )
    } else null

    restaurantCaptureScaffold(
        title = RestaurantCaptureCopy.sheetTitle("PURCHASE"),
        subtitle = RestaurantCaptureCopy.purchaseSubtitle(),
        onDismiss = onDismiss,
        why = RestaurantCaptureCopy.why("PURCHASE"),
    ) {
        EmberChipRow(RestaurantCaptureCopy.modeChips("PURCHASE"), goodsMode) { goodsMode = it }
        EmberFieldLabel("Scan supplier invoice / INV")
        EmberCaptureScanField(
            query = scanQuery,
            placeholder = RestaurantCaptureCopy.SCAN_PLACEHOLDER,
            onQueryChange = { scanQuery = it },
            onScan = {
                val q = scanQuery.trim()
                if (q.isBlank()) return@EmberCaptureScanField
                val blank = lines.indexOfFirst { it.name.isBlank() }
                val draft = newCaptureLine().copy(name = q, sku = q.take(8).uppercase())
                if (blank >= 0) lines[blank] = draft else lines.add(draft)
                scanQuery = ""
            },
        )
        EmberLinesCard(
            title = "Receive lines",
            trailing = when {
                filledCount == 0 -> "No lines yet"
                else -> "$filledCount line${if (filledCount == 1) "" else "s"}"
            },
        ) {
            lines.forEachIndexed { index, line ->
                purchaseLineRow(currency, line, onChange = { lines[index] = it }, onRemove = {
                    if (lines.size > 1) lines.removeAt(index)
                })
            }
            Text(
                "+ Add line",
                style = RestaurantEmber.rowValue(RestaurantEmber.gold).copy(fontSize = 13.sp),
                modifier = Modifier.clickable { lines.add(newCaptureLine()) },
            )
        }
        EmberTotalsHero(
            label = RestaurantCaptureCopy.amountLabel("PURCHASE"),
            value = RestaurantFormat.exact(currency, amountStr),
            side = tender,
            caption = RestaurantCaptureCopy.purchaseHeroCaption(),
        ) {
            EmberTotalRow("Subtotal", RestaurantFormat.exact(currency, goods.toPlainString()))
            EmberGstRow(
                label = "GST",
                estimate = gstEstimate,
                currency = currency,
                missing = "Unavailable — rate missing on line / master",
            )
            EmberTotalRow("Payable", RestaurantFormat.exact(currency, amountStr), valueColor = RestaurantEmber.gold)
        }
        EmberChipRow(listOf("Pay later", "UPI now", "Cash"), tender, tenderStyle = true) { tender = it }
        EmberFieldLabel("Supplier")
        EmberChevronField(
            label = "Supplier",
            value = supplier.ifBlank { suppliers.firstOrNull()?.name ?: "Select supplier" },
            onClick = {
                supplier = suppliers.firstOrNull()?.name?.takeIf { it.isNotBlank() }
                    ?: supplier.ifBlank { "Fresh Farm" }
            },
        )
        suppliers.take(3).forEach { row ->
            EmberChevronField(label = "", value = row.name, onClick = { supplier = row.name })
        }
        EmberMoreToggle(moreOpen, { moreOpen = !moreOpen }, RestaurantCaptureCopy.MORE_DETAILS_HINT) {
            EmberTextField(note, "GRN note · supplier GSTIN", onValueChange = { note = it })
        }
        EmberSheetFooter(
            label = RestaurantCaptureCopy.cta("PURCHASE", amountLabel),
            enabled = postable,
            caveat = RestaurantCaptureCopy.footerCaveat("PURCHASE"),
        ) {
            val filled = lines.filter { it.name.isNotBlank() }
            val paymentStatus = when (tender) {
                "UPI now", "Cash" -> "PAID"
                else -> "OPEN"
            }
            captureSavedToast()
            onSubmit(
                RestaurantEventDraft(
                    eventType = "PURCHASE",
                    title = filled.firstOrNull()?.name?.let { "Purchase · $it" } ?: "Purchase · $supplier",
                    amount = amountStr,
                    note = listOfNotNull(
                        note.takeIf { it.isNotBlank() },
                        "Mode: $goodsMode",
                        "Kitchen: $kitchen",
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
                        receiptState = "RECEIVED",
                        paymentState = when (tender) {
                            "UPI now", "Cash" -> "PAID"
                            else -> "PAY_LATER"
                        },
                        entryMode = "ITEMIZED",
                        goodsStatus = "RECEIVED",
                        staffName = owner,
                    ),
                ),
            )
        }
    }
}

private fun RestaurantCaptureCopy.purchaseHeroCaption(): String = "Purchase total · Posts stock on accept"

@Composable
private fun purchaseLineRow(
    currency: String,
    line: CaptureLineDraft,
    onChange: (CaptureLineDraft) -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            if (line.name.isBlank()) {
                EmberTextField(line.name, "Item", onValueChange = { onChange(line.copy(name = it)) })
            } else {
                Text(line.name, style = RestaurantEmber.rowTitle().copy(fontSize = 13.sp), maxLines = 1)
                Text(
                    listOfNotNull(line.sku.takeIf { it.isNotBlank() }, line.unit).joinToString(" · "),
                    style = RestaurantEmber.rowCaption().copy(fontSize = 10.sp),
                )
            }
        }
        val qtyInt = line.qty.toIntOrNull() ?: 1
        EmberStepper(
            onMinus = { if (qtyInt > 1) onChange(line.copy(qty = (qtyInt - 1).toString())) else onRemove() },
            onPlus = { onChange(line.copy(qty = (qtyInt + 1).toString())) },
        )
        Text(line.qty, style = RestaurantEmber.rowValue().copy(fontSize = 13.sp))
        Column(horizontalAlignment = Alignment.End) {
            EmberTextField(
                line.rate,
                "Rate",
                onValueChange = { onChange(line.copy(rate = it.filter { ch -> ch.isDigit() || ch == '.' }.take(10))) },
            )
            Text(
                RestaurantFormat.exact(currency, line.lineTotal().takeIf { it.signum() > 0 }?.toPlainString()),
                style = RestaurantEmber.rowCaption().copy(fontSize = 11.sp),
            )
        }
    }
}

// ── Collection `2400:30193` ───────────────────────────────────────────────────

@Composable
private fun RestaurantCollectionSheet(
    model: OperatingProjectionDto,
    onDismiss: () -> Unit,
    onSubmit: (RestaurantEventDraft) -> Unit,
) {
    val currency = model.currency ?: "INR"
    val kitchen = RestaurantCaptureCopy.kitchenLine(model.title, model.location)
    val owner = RestaurantCaptureCopy.ownerHint(model.partyName ?: model.participants.firstOrNull())
    var settleMode by remember { mutableStateOf("Platform") }
    val openAll = model.paymentReadiness.filter { it.relevance == "RECEIVABLE" }
    val open = remember(settleMode, openAll) {
        if (settleMode == "Platform") {
            openAll.filter {
                val t = "${it.title} ${it.counterpartyName}".lowercase()
                t.contains("platform") || t.contains("zomato") || t.contains("swiggy") || t.contains("cloud")
            }.ifEmpty { openAll }
        } else openAll
    }
    var selectedId by remember(open) { mutableStateOf(open.firstOrNull()?.id) }
    var amount by remember(open) { mutableStateOf(open.firstOrNull()?.amount.orEmpty()) }
    var tender by remember { mutableStateOf("UPI") }
    var reference by remember { mutableStateOf("") }
    val row = open.firstOrNull { it.id == selectedId }
    val receipt = amount.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val amountLabel = receipt.takeIf { it.signum() == 1 }?.let { RestaurantFormat.exact(currency, amount) }
    val postable = row != null && receipt.signum() == 1

    restaurantCaptureScaffold(
        title = RestaurantCaptureCopy.sheetTitle("COLLECTION"),
        subtitle = RestaurantCaptureCopy.collectionSubtitle(settleMode, kitchen, owner),
        onDismiss = onDismiss,
        why = RestaurantCaptureCopy.why("COLLECTION"),
    ) {
        EmberChipRow(RestaurantCaptureCopy.modeChips("COLLECTION"), settleMode) { settleMode = it }
        EmberLinesCard(
            title = "Allocate against",
            trailing = if (open.isEmpty()) "No open AR" else "${open.size} open",
        ) {
            if (open.isEmpty()) {
                Text("Nothing to collect", style = RestaurantEmber.rowCaption())
            } else {
                open.take(6).forEach { item ->
                    allocateRow(item, currency, selectedId == item.id, AllocateKind.Receivable) {
                        selectedId = item.id
                        amount = item.amount.orEmpty()
                    }
                }
            }
        }
        EmberTotalsHero(
            label = RestaurantCaptureCopy.amountLabel("COLLECTION"),
            value = RestaurantFormat.exact(currency, amount),
            side = tender,
        ) {
            EmberTotalRow("Open AR", RestaurantFormat.exact(currency, row?.amount), valueColor = RestaurantEmber.negative)
            EmberTotalRow("This receipt", RestaurantFormat.exact(currency, amount.ifBlank { "0" }))
        }
        EmberChipRow(listOf("UPI", "Cash", "Card", "Bank"), tender, tenderStyle = true) { tender = it }
        EmberFieldLabel("Collected amount")
        EmberTextField(
            amount,
            row?.amount?.let { "Up to ${RestaurantFormat.exact(currency, it)}" } ?: "0",
            onValueChange = { amount = it.filter { ch -> ch.isDigit() || ch == '.' }.take(12) },
        )
        EmberChevronField(
            label = "Settle",
            value = row?.title ?: "Platform receivable",
            onClick = {
                selectedId = open.firstOrNull()?.id
                amount = open.firstOrNull()?.amount.orEmpty()
            },
        )
        EmberTextField(reference, "UPI / NEFT ref", onValueChange = { reference = it })
        EmberSheetFooter(
            label = RestaurantCaptureCopy.cta("COLLECTION", amountLabel),
            enabled = postable,
            caveat = RestaurantCaptureCopy.DRAFT_FOOTER,
        ) {
            captureSavedToast()
            onSubmit(
                RestaurantEventDraft(
                    eventType = "COLLECTION",
                    title = "Collection · ${row?.title ?: settleMode}",
                    amount = amount,
                    note = listOfNotNull(
                        "Mode: $settleMode",
                        tender.takeIf { it.isNotBlank() }?.let { "Tender: $it" },
                        reference.takeIf { it.isNotBlank() }?.let { "Ref: $it" },
                    ).joinToString(" · "),
                    channelName = tender,
                    counterpartyName = row?.counterpartyName,
                    paymentStatus = "COLLECTED",
                    parentEventId = row?.id,
                    contract = OperatingContractBody(
                        paymentState = "RECEIVED",
                        receivedAmount = amount,
                        allocations = row?.let { listOf(OperatingAllocationBody(it.id, amount)) },
                    ),
                ),
            )
        }
    }
}

// ── Payment `2400:30257` ────────────────────────────────────────────────────

@Composable
private fun RestaurantPaymentSheet(
    model: OperatingProjectionDto,
    onDismiss: () -> Unit,
    onSubmit: (RestaurantEventDraft) -> Unit,
) {
    val currency = model.currency ?: "INR"
    val kitchen = RestaurantCaptureCopy.kitchenLine(model.title, model.location)
    val owner = RestaurantCaptureCopy.ownerHint(model.partyName ?: model.participants.firstOrNull())
    var payMode by remember { mutableStateOf("Supplier") }
    val open = model.paymentReadiness.filter { it.relevance != "RECEIVABLE" }
    var selectedId by remember { mutableStateOf(open.firstOrNull()?.id) }
    var amount by remember { mutableStateOf(open.firstOrNull()?.amount.orEmpty()) }
    var tender by remember { mutableStateOf("UPI") }
    var reference by remember { mutableStateOf("") }
    val row = open.firstOrNull { it.id == selectedId }
    val paid = amount.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val amountLabel = paid.takeIf { it.signum() == 1 }?.let { RestaurantFormat.exact(currency, amount) }
    val postable = row != null && paid.signum() == 1
    val payee = row?.counterpartyName?.takeIf { it.isNotBlank() } ?: "Supplier"

    restaurantCaptureScaffold(
        title = RestaurantCaptureCopy.sheetTitle("PAYMENT"),
        subtitle = RestaurantCaptureCopy.paymentSubtitle(payee, kitchen, owner),
        onDismiss = onDismiss,
        why = RestaurantCaptureCopy.why("PAYMENT"),
    ) {
        EmberChipRow(RestaurantCaptureCopy.modeChips("PAYMENT"), payMode) { payMode = it }
        EmberLinesCard("Allocate against", if (open.isEmpty()) "No open AP" else "${open.size} open") {
            if (open.isEmpty()) Text("Nothing to pay", style = RestaurantEmber.rowCaption())
            else open.take(6).forEach { item ->
                allocateRow(item, currency, selectedId == item.id, AllocateKind.Payable) {
                    selectedId = item.id
                    amount = item.amount.orEmpty()
                }
            }
        }
        EmberTotalsHero(
            RestaurantCaptureCopy.amountLabel("PAYMENT"),
            RestaurantFormat.exact(currency, amount),
            tender,
        ) {
            EmberTotalRow("Open AP", RestaurantFormat.exact(currency, row?.amount), valueColor = RestaurantEmber.negative)
            EmberTotalRow("This payment", RestaurantFormat.exact(currency, amount.ifBlank { "0" }))
        }
        EmberChipRow(listOf("UPI", "Bank", "Cash", "Cheque"), tender, tenderStyle = true) { tender = it }
        EmberChevronField("Payee", payee, onClick = {
            selectedId = open.firstOrNull()?.id
            amount = open.firstOrNull()?.amount.orEmpty()
        })
        EmberFieldLabel("Payment amount")
        EmberTextField(amount, "0", onValueChange = { amount = it.filter { ch -> ch.isDigit() || ch == '.' }.take(12) })
        EmberTextField(reference, "NEFT / cheque no.", onValueChange = { reference = it })
        EmberSheetFooter(
            RestaurantCaptureCopy.cta("PAYMENT", amountLabel),
            postable,
            RestaurantCaptureCopy.DRAFT_FOOTER,
        ) {
            captureSavedToast()
            onSubmit(
                RestaurantEventDraft(
                    eventType = "PAYMENT",
                    title = "Payment · ${row?.title ?: payMode}",
                    amount = amount,
                    note = listOfNotNull(
                        "Mode: $payMode",
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
                        allocations = row?.let { listOf(OperatingAllocationBody(it.id, amount)) },
                    ),
                ),
            )
        }
    }
}

// ── Expense `2400:30310` ─────────────────────────────────────────────────────

@Composable
private fun RestaurantExpenseSheet(
    model: OperatingProjectionDto,
    onDismiss: () -> Unit,
    onSubmit: (RestaurantEventDraft) -> Unit,
) {
    val currency = model.currency ?: "INR"
    val kitchen = RestaurantCaptureCopy.kitchenLine(model.title, model.location)
    val owner = RestaurantCaptureCopy.ownerHint(model.partyName ?: model.participants.firstOrNull())
    var expenseMode by remember { mutableStateOf("Kitchen") }
    var amount by remember { mutableStateOf("") }
    var payState by remember { mutableStateOf("Paid now") }
    var payee by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var moreOpen by remember { mutableStateOf(false) }
    val postable = amount.toBigDecimalOrNull()?.signum() == 1
    val amountLabel = amount.toBigDecimalOrNull()?.takeIf { it.signum() == 1 }
        ?.let { RestaurantFormat.exact(currency, amount) }

    restaurantCaptureScaffold(
        title = RestaurantCaptureCopy.sheetTitle("SPEND"),
        subtitle = RestaurantCaptureCopy.expenseSubtitle(expenseMode, payee, kitchen, owner),
        onDismiss = onDismiss,
        why = RestaurantCaptureCopy.why("SPEND"),
    ) {
        EmberChipRow(RestaurantCaptureCopy.modeChips("SPEND"), expenseMode) { expenseMode = it }
        EmberTotalsHero(
            RestaurantCaptureCopy.amountLabel("SPEND"),
            RestaurantFormat.exact(currency, amount),
            if (payState == "Paid now") "Paid now" else "Pay later",
        ) {
            EmberTotalRow("Amount", RestaurantFormat.exact(currency, amount.ifBlank { "0" }))
            EmberTotalRow("GST (if any)", "Unavailable", caption = "Never invent GST on expense")
        }
        EmberChipRow(listOf("Paid now", "Pay later"), payState) { payState = it }
        EmberFieldLabel("Payee")
        EmberTextField(payee, "Vendor · platform fee", onValueChange = { payee = it })
        EmberFieldLabel("Amount")
        EmberTextField(amount, "0", onValueChange = { amount = it.filter { ch -> ch.isDigit() || ch == '.' }.take(12) })
        EmberMoreToggle(moreOpen, { moreOpen = !moreOpen }, RestaurantCaptureCopy.MORE_DETAILS_HINT) {
            EmberTextField(note, "Notes · Bill photo", onValueChange = { note = it })
        }
        EmberSheetFooter(
            RestaurantCaptureCopy.cta("SPEND", amountLabel),
            postable,
            RestaurantCaptureCopy.DRAFT_UNTIL_POSTED,
        ) {
            onSubmit(
                RestaurantEventDraft(
                    eventType = "SPEND",
                    title = "$expenseMode · ${payee.ifBlank { "Running cost" }}",
                    amount = amount,
                    note = listOfNotNull(note.takeIf { it.isNotBlank() }, "Kitchen: $kitchen").joinToString(" · "),
                    channelName = expenseMode,
                    counterpartyName = payee.takeIf { it.isNotBlank() },
                    paymentStatus = if (payState == "Paid now") "PAID" else "OPEN",
                    contract = OperatingContractBody(
                        expenseCategory = expenseMode,
                        paymentState = if (payState == "Paid now") "PAID" else "PAY_LATER",
                        staffName = owner,
                    ),
                ),
            )
        }
    }
}

// ── Stock `2400:30363` ────────────────────────────────────────────────────────

@Composable
private fun RestaurantStockSheet(
    model: OperatingProjectionDto,
    onDismiss: () -> Unit,
    onSubmit: (RestaurantEventDraft) -> Unit,
) {
    val kitchen = RestaurantCaptureCopy.kitchenLine(model.title, model.location)
    val owner = RestaurantCaptureCopy.ownerHint(model.partyName ?: model.participants.firstOrNull())
    var action by remember { mutableStateOf("Count") }
    var scanQuery by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    val skus = restaurantRawMasters(model)
    val seed = skus.firstOrNull()
    val lines = remember {
        mutableStateListOf(
            newCaptureLine().copy(
                sku = seed?.code ?: "PNR",
                name = seed?.name ?: "Paneer",
                unit = seed?.unit ?: "kg",
                qty = "8",
                wasQty = "12",
            ),
        )
    }
    val filled = lines.filter { it.name.isNotBlank() }
    val first = filled.firstOrNull()
    val qtyValue = first?.qty?.toBigDecimalOrNull()
    val stockActionCode = RestaurantCaptureCopy.stockActionCode(action)
    val stockReasonCode = RestaurantCaptureCopy.stockReasonCode(action)
    val postable = first != null && qtyValue != null &&
        (stockActionCode == "SET" && qtyValue.signum() >= 0 || stockActionCode != "SET" && qtyValue.signum() > 0)

    restaurantCaptureScaffold(
        title = RestaurantCaptureCopy.sheetTitle("STOCK"),
        subtitle = RestaurantCaptureCopy.stockSubtitle(first?.name.orEmpty(), action, kitchen, owner),
        onDismiss = onDismiss,
        why = RestaurantCaptureCopy.why("STOCK"),
    ) {
        EmberChipRow(RestaurantCaptureCopy.modeChips("STOCK"), action) { action = it }
        EmberFieldLabel("Scan SKU / barcode")
        EmberCaptureScanField(
            scanQuery,
            RestaurantCaptureCopy.STOCK_SCAN_PLACEHOLDER,
            onQueryChange = { scanQuery = it },
            onScan = {
                val q = scanQuery.trim()
                if (q.isBlank()) return@EmberCaptureScanField
                val match = skus.firstOrNull { it.name.contains(q, true) || it.code?.contains(q, true) == true }
                val draft = newCaptureLine().copy(
                    name = match?.name ?: q,
                    sku = (match?.code ?: q.take(6)).uppercase(),
                    unit = match?.unit ?: "kg",
                    qty = "1",
                )
                val blank = lines.indexOfFirst { it.name.isBlank() }
                if (blank >= 0) lines[blank] = draft else lines.add(draft)
                scanQuery = ""
            },
        )
        EmberLinesCard("Qty lines", "${filled.size} SKU") {
            lines.forEachIndexed { index, line ->
                stockLineRow(line, onChange = { lines[index] = it }, onRemove = {
                    if (lines.size > 1) lines.removeAt(index)
                })
            }
            Text(
                "+ Add SKU",
                style = RestaurantEmber.rowValue(RestaurantEmber.gold).copy(fontSize = 13.sp),
                modifier = Modifier.clickable { lines.add(newCaptureLine()) },
            )
        }
        EmberSheetFooter(
            RestaurantCaptureCopy.cta("STOCK"),
            postable,
            RestaurantCaptureCopy.DRAFT_FOOTER,
        ) {
            val primary = filled.firstOrNull() ?: return@EmberSheetFooter
            onSubmit(
                RestaurantEventDraft(
                    eventType = "STOCK",
                    title = primary.name,
                    amount = null,
                    note = listOfNotNull(note.takeIf { it.isNotBlank() }, "Action: $action").joinToString(" · "),
                    channelName = action,
                    counterpartyName = null,
                    paymentStatus = "NONE",
                    quantity = primary.qty,
                    unit = primary.unit,
                    itemName = primary.name,
                    contract = OperatingContractBody(
                        stockAction = stockActionCode,
                        stockReason = stockReasonCode,
                        staffName = owner,
                        declaredQuantity = if (stockActionCode == "SET") primary.qty else null,
                    ),
                    lines = filled.map {
                        OperatingLineBody(itemName = it.name, quantity = it.qty, unit = it.unit, unitPrice = null)
                    },
                ),
            )
        }
    }
}

@Composable
private fun stockLineRow(
    line: CaptureLineDraft,
    onChange: (CaptureLineDraft) -> Unit,
    onRemove: () -> Unit,
) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(line.name.ifBlank { "Item" }, style = RestaurantEmber.rowTitle().copy(fontSize = 13.sp))
            Text(
                listOfNotNull(line.sku, line.wasQty.takeIf { it.isNotBlank() }?.let { "was $it" }).joinToString(" · "),
                style = RestaurantEmber.rowCaption().copy(fontSize = 10.sp),
            )
        }
        val qtyInt = line.qty.toIntOrNull() ?: 0
        EmberStepper(
            onMinus = { if (qtyInt > 0) onChange(line.copy(qty = (qtyInt - 1).toString())) else onRemove() },
            onPlus = { onChange(line.copy(qty = (qtyInt + 1).toString())) },
        )
        Text("→ ${line.qty}", style = RestaurantEmber.rowValue().copy(fontSize = 13.sp), modifier = Modifier.padding(start = 6.dp))
    }
}

// ── Phase 5: Sales Order + Purchase Order dual sheet ──────────────────────────

private enum class OrderCaptureMode { SalesOrder, PurchaseOrder }

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RestaurantOrderCaptureSheet(
    model: OperatingProjectionDto,
    onDismiss: () -> Unit,
    onSubmit: (RestaurantEventDraft) -> Unit,
    onMasterSubmit: ((RestaurantEventDraft) -> Unit)? = null,
) {
    var sheetMode by remember { mutableStateOf(OrderCaptureMode.SalesOrder) }
    var createForm by remember { mutableStateOf<RestaurantMasterFormKind?>(null) }
    val isPo = sheetMode == OrderCaptureMode.PurchaseOrder
    val currency = model.currency ?: "INR"
    val kitchen = RestaurantCaptureCopy.kitchenLine(model.title, model.location)
    val owner = RestaurantCaptureCopy.ownerHint(model.partyName ?: model.participants.firstOrNull())
    val menu = restaurantMenuMasters(model)
    val raw = restaurantRawMasters(model)
    val lines = remember { mutableStateListOf<CaptureLineDraft>() }
    var channel by remember { mutableStateOf("Dine-in") }
    var goodsChip by remember { mutableStateOf("Ingredients") }
    var status by remember { mutableStateOf("Placed") }
    var tableGuest by remember { mutableStateOf("Table 4 · 2 covers") }
    var guest by remember { mutableStateOf("") }
    var supplier by remember { mutableStateOf("") }
    var fulfilBy by remember { mutableStateOf("Tonight · 20:00") }
    var expectedDelivery by remember { mutableStateOf("Fri · cold chain") }
    var category by remember {
        mutableStateOf(
            if (isPo) RestaurantCaptureCopy.poCategories.first() else RestaurantCaptureCopy.soCategories.first(),
        )
    }
    var search by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    val goods = lines.goodsTotal()
    val amountStr = goods.stripTrailingZeros().toPlainString()
    val amountLabel = goods.takeIf { it.signum() == 1 }?.let { RestaurantFormat.exact(currency, amountStr) }
    val postable = goods.signum() == 1
    val copyMode = if (isPo) "PURCHASE_ORDER" else "SALES_ORDER"
    val suppliers = model.masters.orEmpty().filter { it.kind.equals("SUPPLIER", true) }
    val categories = if (isPo) RestaurantCaptureCopy.poCategories else RestaurantCaptureCopy.soCategories
    val catalog = if (isPo) raw else menu

    fun applySheetMode(next: OrderCaptureMode) {
        sheetMode = next
        category = if (next == OrderCaptureMode.PurchaseOrder) {
            RestaurantCaptureCopy.poCategories.first()
        } else {
            RestaurantCaptureCopy.soCategories.first()
        }
        status = if (next == OrderCaptureMode.PurchaseOrder) "Open" else "Placed"
        goodsChip = if (next == OrderCaptureMode.PurchaseOrder) "Ingredients" else goodsChip
        supplier = suppliers.firstOrNull()?.name.orEmpty()
    }

    fun addFromMaster(master: OperatingMasterDto) {
        val rate = (if (isPo) master.purchasePrice else master.sellPrice)
            ?: master.purchasePrice ?: master.sellPrice.orEmpty()
        val draft = newCaptureLine().copy(
            sku = master.code ?: master.name.take(6).uppercase(),
            name = master.name,
            qty = "1",
            rate = rate,
            unit = master.unit ?: if (isPo) "kg" else "plate",
            gst = master.gstRate.orEmpty(),
            note = if (!isPo && lines.isEmpty()) "Hold for fire" else "",
        )
        val blank = lines.indexOfFirst { it.name.isBlank() }
        if (blank >= 0) lines[blank] = draft else lines.add(draft)
    }

    val gstEstimate = RestaurantFormat.gstEstimate(
        lineAmounts = lines.map { it.lineTotal() },
        lineRates = lines.map { it.gst.takeIf { g -> g.isNotBlank() } },
        inclusive = !isPo,
    )

    restaurantCaptureScaffold(
        title = RestaurantCaptureCopy.sheetTitle(copyMode),
        subtitle = if (isPo) {
            RestaurantCaptureCopy.purchaseOrderSubtitle(kitchen, owner)
        } else {
            RestaurantCaptureCopy.salesOrderSubtitle(kitchen, owner)
        },
        onDismiss = onDismiss,
        why = RestaurantCaptureCopy.why(copyMode),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            EmberModeChip("Sales Order", !isPo, onClick = { applySheetMode(OrderCaptureMode.SalesOrder) })
            EmberModeChip("Purchase Order", isPo, onClick = { applySheetMode(OrderCaptureMode.PurchaseOrder) })
        }
        if (isPo) {
            EmberChipRow(listOf("Ingredients", "Supplies"), goodsChip) { goodsChip = it }
        } else {
            EmberChipRow(RestaurantCaptureCopy.soChannels, channel) { channel = it }
        }
        Text(
            if (isPo) RestaurantCaptureCopy.purchaseOrderHeroCaption() else RestaurantCaptureCopy.salesOrderHeroCaption(),
            style = RestaurantEmber.rowCaption(RestaurantEmber.gold).copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
        )
        if (!isPo) {
            EmberChevronField("Table / guest", tableGuest, onClick = {
                tableGuest = if (tableGuest.contains("4")) "Table 7 · 4 covers" else "Table 4 · 2 covers"
            })
            EmberChevronField("Guest", guest.ifBlank { "Walk-in · add name" }, onClick = {
                guest = guest.ifBlank { "Priya Shah" }
            })
        } else {
            EmberChevronField("Supplier", supplier.ifBlank { "Fresh Farm · preferred" }, onClick = {
                supplier = suppliers.firstOrNull()?.name ?: "Fresh Farm"
            })
            EmberChevronField("Expected delivery", expectedDelivery, onClick = {
                expectedDelivery = if (expectedDelivery.contains("Fri")) "Mon · 06:00" else "Fri · cold chain"
            })
        }
        EmberSectionCard(if (isPo) "Raw browser" else "Menu browser", if (isPo) RestaurantCaptureCopy.PO_BROWSER_TRAILING else RestaurantCaptureCopy.SO_BROWSER_TRAILING) {
            EmberCaptureScanField(search, if (isPo) "Search raw / scan" else "Search dish · Scan", { search = it }) {
                val q = search.trim()
                if (q.isBlank()) return@EmberCaptureScanField
                catalog.firstOrNull { it.name.contains(q, true) }?.let { addFromMaster(it) }
                search = ""
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                categories.forEach { cat ->
                    EmberModeChip(cat, cat == category, onClick = { category = cat })
                }
            }
            val pool = catalog.filter { masterMatchesCategory(it, category, isPo) }.ifEmpty { catalog }
            val filtered = pool.filter {
                search.isBlank() || it.name.contains(search, true) || it.code?.contains(search, true) == true
            }.take(6)
            filtered.forEach { master ->
                Row(
                    Modifier.fillMaxWidth().clickable { addFromMaster(master) }.padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(master.name, style = RestaurantEmber.rowTitle().copy(fontSize = 13.sp), maxLines = 1)
                        Text(
                            master.code ?: master.unit ?: "—",
                            style = RestaurantEmber.rowCaption().copy(fontSize = 10.sp),
                        )
                    }
                    Text("Add", style = RestaurantEmber.rowValue(RestaurantEmber.ember).copy(fontSize = 12.sp))
                }
            }
            Text(
                if (isPo) RestaurantCaptureCopy.PO_NEW_RAW else RestaurantCaptureCopy.SO_NEW_DISH,
                style = RestaurantEmber.rowValue(RestaurantEmber.gold).copy(fontSize = 12.sp),
                modifier = Modifier.clickable {
                    createForm = if (isPo) RestaurantMasterFormKind.Raw else RestaurantMasterFormKind.Dish
                },
            )
        }
        val filled = lines.count { it.name.isNotBlank() }
        EmberLinesCard(
            if (isPo) "On this PO" else "On this order",
            if (filled == 0) "No lines yet" else "$filled lines",
        ) {
            lines.filter { it.name.isNotBlank() }.forEach { line ->
                val idx = lines.indexOfFirst { it.id == line.id }
                orderLineRow(currency, line, isPo, onChange = { if (idx >= 0) lines[idx] = it }, onRemove = {
                    if (idx >= 0) lines.removeAt(idx)
                })
            }
        }
        EmberTotalsHero(
            if (isPo) "PO value" else "Order value",
            RestaurantFormat.exact(currency, amountStr),
            status,
            caption = if (isPo) RestaurantCaptureCopy.purchaseOrderHeroCaption() else RestaurantCaptureCopy.salesOrderHeroCaption(),
        ) {
            EmberTotalRow("Lines", RestaurantFormat.exact(currency, amountStr))
            EmberGstRow(
                "Est. GST",
                gstEstimate,
                currency,
                if (isPo) "Never posted from PO — GST on Purchase / GRN" else "Estimate only — posts on POS bill",
            )
            EmberTotalRow("Commitment", RestaurantFormat.exact(currency, amountStr), valueColor = RestaurantEmber.gold)
        }
        EmberChipRow(
            if (isPo) RestaurantCaptureCopy.poStatusChips else RestaurantCaptureCopy.soStatusChips,
            status,
        ) { status = it }
        if (!isPo) {
            EmberChevronField("Fulfil by", fulfilBy, onClick = {
                fulfilBy = if (fulfilBy.contains("Tonight")) "Tomorrow · lunch" else "Tonight · 20:00"
            })
        }
        EmberTextField(note, "Order note", onValueChange = { note = it })
        EmberSheetFooter(
            RestaurantCaptureCopy.cta(copyMode, amountLabel),
            postable,
            if (isPo) RestaurantCaptureCopy.PO_COMMITMENT_FOOTER else RestaurantCaptureCopy.SO_COMMITMENT_FOOTER,
        ) {
            val filledLines = lines.filter { it.name.isNotBlank() }
            captureSavedToast()
            onSubmit(
                RestaurantEventDraft(
                    eventType = "ORDER",
                    title = if (isPo) "PO · ${supplier.ifBlank { "Kitchen" }}" else "Sales order · $channel",
                    amount = amountStr.takeIf { goods.signum() == 1 },
                    note = listOfNotNull(
                        note.takeIf { it.isNotBlank() },
                        if (!isPo) "Table: $tableGuest" else "Delivery: $expectedDelivery",
                        if (!isPo) channel else "Goods: $goodsChip",
                        filledLines.mapNotNull { it.note.takeIf { n -> n.isNotBlank() } }.joinToString("; ").takeIf { it.isNotBlank() },
                    ).joinToString(" · "),
                    channelName = if (isPo) goodsChip else channel,
                    counterpartyName = if (isPo) supplier.takeIf { it.isNotBlank() } else guest.takeIf { it.isNotBlank() },
                    paymentStatus = "NONE",
                    dueAt = if (isPo) expectedDelivery else fulfilBy,
                    lines = filledLines.map {
                        OperatingLineBody(
                            itemName = if (it.note.isNotBlank()) "${it.name} · ${it.note}" else it.name,
                            quantity = it.qty,
                            unit = it.unit,
                            unitPrice = it.rate.ifBlank { null },
                        )
                    },
                    contract = OperatingContractBody(
                        orderSide = if (isPo) "PURCHASE" else "SALES",
                        orderStatus = orderStatusToken(status),
                        promisedAt = if (isPo) expectedDelivery else fulfilBy,
                        estimatedAmount = amountStr.takeIf { goods.signum() == 1 },
                        locationName = if (isPo) kitchen else tableGuest,
                        staffName = owner,
                    ),
                ),
            )
        }
    }

    createForm?.let { kind ->
        com.example.momentra.ui.shell.business.shared.OperatingSheetScrim(
            onDismiss = { createForm = null },
            accent = RestaurantEmber.ember,
            surface = RestaurantEmber.card,
            border = RestaurantEmber.sheetBorder,
            topCorner = RestaurantEmber.sheetTopCorner,
        ) {
            RestaurantMasterFormSheet(
                kind = kind,
                model = model,
                onDismiss = { createForm = null },
                onSubmit = { draft ->
                    (onMasterSubmit ?: onSubmit)(draft)
                    createForm = null
                    captureSavedToast()
                },
            )
        }
    }
}

@Composable
private fun orderLineRow(
    currency: String,
    line: CaptureLineDraft,
    isPo: Boolean,
    onChange: (CaptureLineDraft) -> Unit,
    onRemove: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(line.name, style = RestaurantEmber.rowTitle().copy(fontSize = 13.sp), maxLines = 1)
                if (line.note.isNotBlank()) {
                    Text(line.note, style = RestaurantEmber.rowCaption(RestaurantEmber.amber).copy(fontSize = 10.sp))
                }
            }
            val qtyInt = line.qty.toIntOrNull() ?: 1
            EmberStepper(
                onMinus = { if (qtyInt > 1) onChange(line.copy(qty = (qtyInt - 1).toString())) else onRemove() },
                onPlus = { onChange(line.copy(qty = (qtyInt + 1).toString())) },
            )
            Text(
                RestaurantFormat.exact(currency, line.lineTotal().takeIf { it.signum() > 0 }?.toPlainString()),
                style = RestaurantEmber.rowValue().copy(fontSize = 12.sp),
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        if (!isPo) {
            EmberTextField(line.note, "Line note · Hold for fire", onValueChange = { onChange(line.copy(note = it)) })
        }
    }
}

private fun orderStatusToken(status: String): String = when (status) {
    "Preparing" -> "PREPARING"
    "Placed" -> "PLACED"
    "Ready" -> "READY"
    "Partial" -> "PARTIAL"
    "Received" -> "RECEIVED"
    else -> "OPEN"
}

// ── Shared chrome ─────────────────────────────────────────────────────────────

@Composable
private fun restaurantCaptureScaffold(
    title: String,
    subtitle: String,
    onDismiss: () -> Unit,
    why: String,
    content: @Composable () -> Unit,
) {
  Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        EmberSheetGrabber()
        EmberSheetHeader(title, subtitle, onDismiss)
        content()
        EmberWhyNote(why)
        Spacer(Modifier.height(12.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EmberChipRow(
    options: List<String>,
    selected: String,
    tenderStyle: Boolean = false,
    onSelect: (String) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        options.forEach { opt ->
            EmberModeChip(
                text = opt,
                selected = opt == selected,
                selectedFill = if (tenderStyle) RestaurantEmber.positive else RestaurantEmber.ember,
                onClick = { onSelect(opt) },
            )
        }
    }
}

@Composable
private fun EmberLinesCard(title: String, trailing: String, content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(RestaurantEmber.card)
            .border(1.dp, RestaurantEmber.hairline.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, style = RestaurantEmber.rowTitle().copy(fontSize = 13.sp))
            Text(trailing, style = RestaurantEmber.rowCaption().copy(fontSize = 10.sp))
        }
        content()
    }
}

@Composable
private fun EmberSectionCard(label: String, trailing: String, content: @Composable () -> Unit) {
    EmberLinesCard(label, trailing, content)
}

@Composable
private fun EmberTotalsHero(
    label: String,
    value: String,
    side: String,
    caption: String? = null,
    rows: @Composable () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                androidx.compose.ui.graphics.Brush.linearGradient(
                    listOf(RestaurantEmber.heroStart.copy(alpha = 0.35f), RestaurantEmber.heroEnd.copy(alpha = 0.2f)),
                ),
            )
            .border(1.dp, RestaurantEmber.gold.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        caption?.let {
            Text(it, style = RestaurantEmber.rowCaption(RestaurantEmber.gold).copy(fontSize = 10.sp))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = RestaurantEmber.rowTitle().copy(fontSize = 14.sp))
            Text(value, style = RestaurantEmber.heroMetric().copy(fontSize = 20.sp))
        }
        Text(side, style = RestaurantEmber.rowCaption().copy(fontSize = 10.sp))
        rows()
    }
}

@Composable
private fun EmberGstRow(label: String, estimate: BigDecimal?, currency: String, missing: String) {
    val value = estimate?.let { RestaurantFormat.exact(currency, it.toPlainString()) } ?: "Unavailable"
    EmberTotalRow(label, value, caption = if (estimate == null) missing else null)
}

@Composable
private fun EmberCaptureScanField(
    query: String,
    placeholder: String,
    onQueryChange: (String) -> Unit,
    onScan: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(RestaurantEmber.chip)
            .border(1.5.dp, RestaurantEmber.ember, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = TextStyle(color = RestaurantEmber.textPrimary, fontFamily = PlusJakartaSans, fontSize = 13.sp),
            cursorBrush = SolidColor(RestaurantEmber.ember),
            decorationBox = { inner ->
                if (query.isEmpty()) Text(placeholder, style = RestaurantEmber.rowCaption(RestaurantEmber.textFaint))
                inner()
            },
            modifier = Modifier.weight(1f),
        )
        Text(
            "Scan",
            style = RestaurantEmber.chipLabel(true).copy(fontSize = 11.sp, color = RestaurantEmber.inkOnEmber),
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(RestaurantEmber.ember)
                .clickable(onClick = onScan)
                .padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun EmberStepper(onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        stepperGlyph("−", onMinus)
        stepperGlyph("+", onPlus)
    }
}

@Composable
private fun stepperGlyph(glyph: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(RestaurantEmber.chip)
            .border(1.dp, RestaurantEmber.hairline, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(glyph, style = RestaurantEmber.rowValue().copy(fontSize = 14.sp))
    }
}

@Composable
private fun EmberMoreToggle(expanded: Boolean, onToggle: () -> Unit, hint: String, content: @Composable () -> Unit) {
    Text(
        if (expanded) "Less details" else "More details",
        style = RestaurantEmber.rowValue(RestaurantEmber.gold).copy(fontSize = 12.sp),
        modifier = Modifier.clickable(onClick = onToggle),
    )
    if (expanded) {
        Text(hint, style = RestaurantEmber.rowCaption().copy(fontSize = 10.sp))
        content()
    }
}

private enum class AllocateKind { Receivable, Payable }

@Composable
private fun allocateRow(
    item: OperatingPaymentDto,
    currency: String,
    selected: Boolean,
    kind: AllocateKind,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) RestaurantEmber.ember.copy(alpha = 0.14f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(item.title, style = RestaurantEmber.rowTitle().copy(fontSize = 13.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                item.counterpartyName.orEmpty(),
                style = RestaurantEmber.rowCaption().copy(fontSize = 10.sp),
                maxLines = 1,
            )
        }
        Text(
            RestaurantFormat.exact(currency, item.amount),
            style = RestaurantEmber.rowValue(
                if (kind == AllocateKind.Receivable) RestaurantEmber.negative else RestaurantEmber.textPrimary,
            ).copy(fontSize = 13.sp),
        )
    }
}

private fun restaurantMenuMasters(model: OperatingProjectionDto): List<OperatingMasterDto> {
    val masters = model.masters.orEmpty()
    val menu = masters.filter { it.kind.uppercase() in setOf("MENU_DISH", "DISH", "MENU_ITEM") }
    if (menu.isNotEmpty()) return menu
    return masters.filter { it.kind.uppercase() in setOf("PRODUCT", "CATALOGUE", "SKU") }
}

private fun restaurantRawMasters(model: OperatingProjectionDto): List<OperatingMasterDto> {
    val masters = model.masters.orEmpty()
    val raw = masters.filter { it.kind.uppercase() in setOf("STOCK_ITEM", "RAW", "MATERIAL", "INGREDIENT") }
    return raw.ifEmpty { masters.filter { !it.kind.equals("MENU_DISH", true) && !it.kind.equals("SUPPLIER", true) } }
}

private fun masterMatchesCategory(master: OperatingMasterDto, category: String, isPo: Boolean): Boolean {
    val name = master.name.lowercase()
    val cat = category.lowercase()
    return when {
        cat.contains("main") -> name.contains("curry") || name.contains("biryani") || name.contains("thali")
        cat.contains("bread") -> name.contains("naan") || name.contains("roti") || name.contains("bread")
        cat.contains("starter") -> name.contains("starter") || name.contains("tikka") || name.contains("chaat")
        cat.contains("drink") -> name.contains("lassi") || name.contains("drink") || name.contains("chai")
        cat.contains("dessert") -> name.contains("dessert") || name.contains("kulfi") || name.contains("sweet")
        isPo && cat.contains("dairy") -> name.contains("milk") || name.contains("paneer") || name.contains("dairy")
        isPo && cat.contains("grain") -> name.contains("rice") || name.contains("flour") || name.contains("dal")
        isPo && cat.contains("oil") -> name.contains("oil") || name.contains("ghee")
        isPo && cat.contains("spice") -> name.contains("spice") || name.contains("masala")
        isPo && cat.contains("produce") -> name.contains("onion") || name.contains("tomato") || name.contains("veg")
        isPo && cat.contains("pack") -> name.contains("pack") || name.contains("foil") || name.contains("box")
        else -> master.dimension?.equals(category, true) == true
    }
}

private fun captureSavedToast() {
    MtoToastHost.show(
        MtoToastSpec(
            kind = MtoToastKind.Saved,
            title = RestaurantCaptureCopy.SAVED_TOAST,
            body = RestaurantCaptureCopy.TAGLINE,
            actionLabel = "Done",
            accent = RestaurantEmber.positive,
        ),
    )
}


// ── Phase 7: Budget / Issue ───────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RestaurantBudgetSheet(
    model: OperatingProjectionDto,
    onDismiss: () -> Unit,
    onSubmit: (RestaurantEventDraft) -> Unit,
) {
    val currency = model.currency ?: "INR"
    val kitchen = RestaurantCaptureCopy.kitchenLine(model.title, model.location)
    val owner = RestaurantCaptureCopy.ownerHint(model.partyName ?: model.participants.firstOrNull())
    var metric by remember { mutableStateOf("Sales") }
    val previous = model.target?.value?.takeIf { it.isNotBlank() }
        ?: model.budget?.amount?.takeIf { it.isNotBlank() }
        ?: RestaurantCaptureCopy.BUDGET_FIXTURE_PREVIOUS
    var amount by remember {
        mutableStateOf(RestaurantCaptureCopy.BUDGET_FIXTURE_NEW)
    }
    var coversPrev by remember { mutableStateOf(RestaurantCaptureCopy.BUDGET_FIXTURE_COVERS_PREV) }
    var coversNew by remember { mutableStateOf(RestaurantCaptureCopy.BUDGET_FIXTURE_COVERS_NEW) }
    var period by remember { mutableStateOf(RestaurantFormat.month(model.startDate)) }
    var effective by remember { mutableStateOf(RestaurantCaptureCopy.BUDGET_EFFECTIVE) }
    var moreOpen by remember { mutableStateOf(false) }
    val prevBd = previous.toBigDecimalOrNull()
    val nextBd = amount.toBigDecimalOrNull()
    val delta = if (prevBd != null && nextBd != null) nextBd - prevBd else null
    val pct = if (prevBd != null && prevBd.signum() > 0 && delta != null) {
        delta.multiply(BigDecimal("100")).divide(prevBd, 0, RoundingMode.HALF_UP).toPlainString()
    } else null
    val postable = amount.toBigDecimalOrNull()?.signum() == 1
    val amountExact = RestaurantFormat.exact(currency, amount)

    restaurantCaptureScaffold(
        title = "Budget / Target Revision",
        subtitle = RestaurantCaptureCopy.budgetSubtitle(kitchen, owner, metric),
        onDismiss = onDismiss,
        why = RestaurantCaptureCopy.budgetWhy(),
    ) {
        EmberChipRow(RestaurantCaptureCopy.budgetModeChips(), metric) { metric = it }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            EmberModeChip(RestaurantCaptureCopy.posBrand(model.title), selected = true, selectedFill = RestaurantEmber.wine, selectedInk = RestaurantEmber.textPrimary, onClick = null)
            EmberModeChip(period, selected = false, onClick = null)
            EmberModeChip("v2 draft", selected = false, onClick = null)
        }
        EmberTotalsHero(
            "New target",
            amountExact,
            "Effective 01 Nov · was ₹3.5L",
            caption = null,
        ) {
            EmberTotalRow("Previous", "₹3.5L · v1")
            EmberTotalRow("New", "₹4.0L · v2", valueColor = RestaurantEmber.gold)
            EmberTotalRow("Effective", effective, valueColor = RestaurantEmber.gold)
        }
        EmberLinesCard("Revision lines", "v1 → v2") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Sales target", style = RestaurantEmber.rowTitle().copy(fontSize = 14.sp))
                    Text("v1 → v2", style = RestaurantEmber.rowCaption().copy(fontSize = 11.sp))
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("₹3.5L → ₹4.0L", style = RestaurantEmber.rowValue().copy(fontSize = 14.sp))
                    Text(pct?.let { "+$it%" } ?: "+14%", style = RestaurantEmber.rowCaption(RestaurantEmber.gold).copy(fontSize = 11.sp))
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Covers / day", style = RestaurantEmber.rowTitle().copy(fontSize = 14.sp))
                    Text("avg target", style = RestaurantEmber.rowCaption().copy(fontSize = 11.sp))
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("$coversPrev → $coversNew", style = RestaurantEmber.rowValue().copy(fontSize = 14.sp))
                    Text("+14", style = RestaurantEmber.rowCaption(RestaurantEmber.gold).copy(fontSize = 11.sp))
                }
            }
        }
        EmberMoreToggle(moreOpen, { moreOpen = !moreOpen }, "Edit amounts · period · covers") {
            EmberFieldLabel("New target")
            EmberTextField(amount, RestaurantCaptureCopy.BUDGET_FIXTURE_NEW) {
                amount = it.filter { ch -> ch.isDigit() || ch == '.' }.take(14)
            }
            EmberFieldLabel("Covers new")
            EmberTextField(coversNew, "100") { coversNew = it.filter { ch -> ch.isDigit() }.take(4) }
            EmberFieldLabel("Period")
            EmberTextField(period, "Nov 2026") { period = it.take(32) }
            EmberFieldLabel("Effective")
            EmberTextField(effective, RestaurantCaptureCopy.BUDGET_EFFECTIVE) { effective = it.take(24) }
        }
        EmberSheetFooter(RestaurantCaptureCopy.budgetCta(), postable, RestaurantCaptureCopy.DRAFT_UNTIL_POSTED) {
            captureSavedToast()
            onSubmit(
                RestaurantEventDraft(
                    eventType = "BUDGET",
                    title = "$metric · $period",
                    amount = amount,
                    note = listOf(
                        "Metric: $metric",
                        "Period: $period",
                        "Effective: $effective",
                        "Previous: $previous",
                        "Covers: $coversPrev→$coversNew",
                        "Versioned plan — achievement uses posted bills only",
                    ).joinToString(" · "),
                    channelName = null,
                    counterpartyName = null,
                    paymentStatus = null,
                    contract = OperatingContractBody(estimatedAmount = amount),
                ),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RestaurantIssueSheet(
    model: OperatingProjectionDto,
    onDismiss: () -> Unit,
    onSubmit: (RestaurantEventDraft) -> Unit,
) {
    val kitchen = RestaurantCaptureCopy.kitchenLine(model.title, model.location)
    val owner = RestaurantCaptureCopy.ownerHint(model.partyName ?: model.participants.firstOrNull())
    var kind by remember { mutableStateOf("Issue") }
    var severity by remember { mutableStateOf("Alert") }
    var title by remember { mutableStateOf(RestaurantCaptureCopy.ISSUE_FIXTURE_TITLE) }
    var issueOwner by remember { mutableStateOf(RestaurantCaptureCopy.ISSUE_FIXTURE_OWNER) }
    var link by remember { mutableStateOf(RestaurantCaptureCopy.ISSUE_FIXTURE_LINK) }
    var note by remember { mutableStateOf("") }
    var moreOpen by remember { mutableStateOf(false) }
    val postable = title.isNotBlank()
    val eventType = when (kind) {
        "Issue" -> "ISSUE"
        "Approval" -> "APPROVAL"
        else -> "UPDATE"
    }

    restaurantCaptureScaffold(
        title = "Issue / Approval / Update",
        subtitle = RestaurantCaptureCopy.issueSubtitle(kitchen, owner),
        onDismiss = onDismiss,
        why = RestaurantCaptureCopy.issueWhy(),
    ) {
        EmberChipRow(RestaurantCaptureCopy.issueModeChips(), kind) { kind = it }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            EmberModeChip(RestaurantCaptureCopy.posBrand(model.title), selected = true, selectedFill = RestaurantEmber.wine, selectedInk = RestaurantEmber.textPrimary, onClick = null)
            EmberModeChip("Kitchen ops", selected = false, onClick = null)
            EmberModeChip("Audit", selected = false, onClick = null)
        }
        EmberTotalsHero(
            "Severity",
            severity,
            RestaurantCaptureCopy.ISSUE_FIXTURE_DETAIL,
        ) {}
        EmberLinesCard("Linked", "Register · shift") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(title, style = RestaurantEmber.rowTitle().copy(fontSize = 14.sp))
                    Text(RestaurantCaptureCopy.ISSUE_FIXTURE_META, style = RestaurantEmber.rowCaption().copy(fontSize = 11.sp))
                }
                Column(horizontalAlignment = Alignment.End) {
                    EmberSeverityChip(severity, RestaurantEmber.negative, bordered = true)
                    Text(link, style = RestaurantEmber.rowCaption(RestaurantEmber.gold).copy(fontSize = 11.sp))
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Owner", style = RestaurantEmber.rowTitle().copy(fontSize = 14.sp))
                    Text(issueOwner, style = RestaurantEmber.rowCaption().copy(fontSize = 11.sp))
                }
                Column(horizontalAlignment = Alignment.End) {
                    EmberSeverityChip("Open", RestaurantEmber.amber, bordered = true)
                    Text("Assign", style = RestaurantEmber.rowCaption(RestaurantEmber.gold).copy(fontSize = 11.sp))
                }
            }
        }
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(RestaurantEmber.card)
                .border(1.dp, RestaurantEmber.ember.copy(alpha = 0.27f), RoundedCornerShape(14.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            EmberTotalRow("Link", link)
            EmberTotalRow("Owner", issueOwner)
            EmberTotalRow("State", "Open", valueColor = RestaurantEmber.gold)
        }
        EmberChipRow(RestaurantCaptureCopy.issueSeverities, severity) { severity = it }
        EmberMoreToggle(moreOpen, { moreOpen = !moreOpen }, "Notes · Evidence · Tags") {
            EmberFieldLabel("Title (edit)")
            EmberTextField(title, RestaurantCaptureCopy.ISSUE_FIXTURE_TITLE) { title = it.take(80) }
            EmberFieldLabel("Owner (edit)")
            EmberTextField(issueOwner, RestaurantCaptureCopy.ISSUE_FIXTURE_OWNER) { issueOwner = it.take(40) }
            EmberFieldLabel("Link (edit)")
            EmberTextField(link, RestaurantCaptureCopy.ISSUE_FIXTURE_LINK) { link = it.take(40) }
            EmberFieldLabel("Notes · Evidence")
            EmberTextField(note, "Notes · Evidence · Tags") { note = it }
        }
        EmberSheetFooter(RestaurantCaptureCopy.issueCta(kind), postable, RestaurantCaptureCopy.DRAFT_UNTIL_POSTED) {
            captureSavedToast()
            onSubmit(
                RestaurantEventDraft(
                    eventType = eventType,
                    title = title,
                    amount = null,
                    note = listOfNotNull(
                        "Severity: $severity",
                        "Owner: $issueOwner",
                        "Link: $link",
                        note.takeIf { it.isNotBlank() },
                        "approval ≠ disbursement",
                    ).joinToString(" · "),
                    channelName = null,
                    counterpartyName = issueOwner.takeIf { it.isNotBlank() },
                    paymentStatus = if (kind == "Approval") "PENDING" else null,
                ),
            )
        }
    }
}

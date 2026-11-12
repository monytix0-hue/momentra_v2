package com.example.momentra.ui.shell.business.retail

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
import com.example.momentra.ui.shell.business.shared.OperatingSheetScrim
import com.example.momentra.ui.shell.business.shared.OperatingStockReasons
import com.example.momentra.ui.theme.RetailRose
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

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
    var gst: String = "",
    /** Stock count: previous on-hand shown as “was N”. */
    var wasQty: String = "",
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
    val storeLine = RetailCaptureCopy.storeLine(model.title, model.location)
    var goodsMode by remember { mutableStateOf(RetailCaptureCopy.modeChips("PURCHASE").first()) }
    var scanQuery by remember { mutableStateOf("") }
    var showScanner by remember { mutableStateOf(false) }
    var supplier by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("Back store") }
    var linkedPo by remember { mutableStateOf("") }
    var freight by remember { mutableStateOf("0") }
    var tender by remember { mutableStateOf("Pay later") }
    var note by remember { mutableStateOf("") }
    var moreOpen by remember { mutableStateOf(false) }
    // Start empty — Scan must open camera / apply a code, never seed blank receive cards.
    val lines = remember { mutableStateListOf<CaptureLineDraft>() }
    val catalog = remember(model.masters) { retailCatalogMasters(model) }
    val suppliers = model.masters.orEmpty().filter { it.kind.equals("SUPPLIER", true) }
    val locations = model.masters.orEmpty().filter {
        it.kind.equals("LOCATION", true) || it.kind.equals("STORE", true) || it.kind.equals("CHANNEL", true)
    }
    val openPos = model.activity.filter {
        it.eventType.equals("ORDER", true) &&
            (it.orderSide.equals("PURCHASE", true) || it.title.contains("PO", ignoreCase = true))
    }
    val filledCount = lines.count { it.name.isNotBlank() }
    val goods = lines.goodsTotal()
    val freightBd = freight.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val purchaseTotal = goods.add(freightBd).setScale(2, RoundingMode.HALF_UP)
    val amountStr = purchaseTotal.stripTrailingZeros().toPlainString()
    val amountLabel = if (purchaseTotal.signum() == 1) RetailFormat.exact(currency, amountStr) else null
    val postable = supplier.isNotBlank() && purchaseTotal.signum() == 1 && filledCount >= 1
    val gstOn = model.gstTracking.equals("YES", true)

    fun applyPurchaseScan(raw: String) {
        val q = raw.trim()
        if (q.isBlank()) return
        val match = catalog.firstOrNull {
            it.barcode.equals(q, true) || it.code.equals(q, true) ||
                (q.length >= 3 && it.name.contains(q, true))
        }
        val draft = CaptureLineDraft(
            id = System.nanoTime(),
            sku = (match?.code ?: q.take(12)).uppercase(),
            name = match?.name ?: q,
            qty = "1",
            rate = match?.purchasePrice ?: match?.sellPrice.orEmpty(),
            hsn = match?.hsn.orEmpty(),
            unit = match?.unit ?: "pcs",
            gst = match?.gstRate.orEmpty(),
        )
        val blank = lines.indexOfFirst { it.name.isBlank() }
        if (blank >= 0) lines[blank] = draft else lines.add(draft)
        scanQuery = ""
    }

    Box {
    retailCaptureScaffold(
        title = RetailCaptureCopy.sheetTitle("PURCHASE"),
        subtitle = RetailCaptureCopy.purchaseSubtitle(location, storeLine, gstOn),
        onDismiss = onDismiss,
        why = RetailCaptureCopy.why("PURCHASE"),
        cta = RetailCaptureCopy.cta("PURCHASE", amountLabel),
        enabled = postable,
        hideFooter = true,
    ) {
        RoseChipRow(
            label = null,
            options = RetailCaptureCopy.modeChips("PURCHASE"),
            selected = goodsMode,
        ) { goodsMode = it }
        RoseFieldLabel("Scan supplier invoice / SKU")
        RoseScanField(
            query = scanQuery,
            placeholder = RetailCaptureCopy.SCAN_PLACEHOLDER,
            onQueryChange = { scanQuery = it },
            onScan = {
                val q = scanQuery.trim()
                if (q.isNotBlank()) {
                    applyPurchaseScan(q)
                } else {
                    showScanner = true
                }
            },
        )
        RoseLinesCard(
            title = "Receive lines",
            lineCount = filledCount,
            trailingOverride = when {
                filledCount == 0 -> "No lines yet"
                else -> "$filledCount line${if (filledCount == 1) "" else "s"} · full receive"
            },
        ) {
            if (lines.isEmpty()) {
                RoseEmpty("No receive lines", "Scan a barcode / invoice QR, or + Add line.")
            }
            lines.forEachIndexed { index, line ->
                purchaseLineRow(
                    currency = currency,
                    line = line,
                    onChange = { lines[index] = it },
                    onRemove = { lines.removeAt(index) },
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
                    style = RetailRose.rowCaption(RetailRose.textBody),
                    modifier = Modifier.clickable {
                        openPos.firstOrNull()?.let { linkedPo = it.title }
                        catalog.take(2).forEach { sku ->
                            applyPurchaseScan(sku.code ?: sku.barcode ?: sku.name)
                        }
                    },
                )
            }
        }
        RoseTotalsCard(
            payableLabel = RetailCaptureCopy.amountLabel("PURCHASE"),
            payable = RetailFormat.exact(currency, amountStr),
            tender = when (tender) {
                "Pay later" -> "AP open"
                "Pay now" -> "Settled"
                else -> "Partial"
            },
            sideLabel = tender,
            sideValueColor = if (tender == "Pay later") RetailRose.amber else RetailRose.positive,
            sideLabelColor = RetailRose.amber,
        ) {
            RoseTotalRow("Goods", RetailFormat.exact(currency, goods.toPlainString()))
            val purchaseGst = RetailFormat.gstEstimate(
                lineAmounts = lines.map { it.lineTotal() },
                lineRates = lines.map { it.gst.takeIf { g -> g.isNotBlank() } },
                inclusive = false,
            )
            RoseGstEstimateRow(
                label = "Input GST · CGST+SGST (est.)",
                estimate = purchaseGst,
                currency = currency,
                captionWhenKnown = "Estimate from line rates — GST posts only on Purchase post",
                captionWhenMissing = "Unavailable — rate missing on line / master",
            )
            RoseTotalRow("Freight", RetailFormat.exact(currency, freight.ifBlank { "0" }))
        }
        RoseChipRow(
            label = "Tender / method",
            options = listOf("Pay later", "Pay now", "Part"),
            selected = tender,
            tenderSelection = true,
        ) { tender = it }
        RosePickerRow(
            label = "Supplier",
            value = supplier.ifBlank { suppliers.firstOrNull()?.name ?: "Select supplier" },
            externalLabel = true,
            onClick = {
                supplier = suppliers.firstOrNull()?.name?.takeIf { it.isNotBlank() }
                    ?: supplier.ifBlank { "Wholesale Foods" }
            },
        )
        suppliers.take(3).forEach { row ->
            RosePickerRow(row.name, row.gstin?.let { "GSTIN $it" } ?: "Vendor", onClick = { supplier = row.name })
        }
        RosePickerRow(
            label = "Location",
            value = location,
            externalLabel = true,
            onClick = {
                location = locations.firstOrNull()?.name ?: location
            },
        )
        RosePickerRow(
            label = "Linked PO",
            value = linkedPo.ifBlank { openPos.firstOrNull()?.title?.let { "$it (optional)" } ?: "PO-219 (optional)" },
            externalLabel = true,
            onClick = {
                linkedPo = openPos.firstOrNull()?.title.orEmpty()
            },
        )
        RoseMoreDetails(
            expanded = moreOpen,
            onToggle = { moreOpen = !moreOpen },
            hint = RetailCaptureCopy.PURCHASE_MORE_HINT,
        ) {
            RoseFieldLabel("Freight")
            RoseTextField(freight, "0", onValueChange = { freight = it.filter { ch -> ch.isDigit() || ch == '.' }.take(10) })
            RoseTextField(note, "GRN note · supplier GSTIN", onValueChange = { note = it }, singleLine = false)
        }
        RoseStickyFooter(
            label = RetailCaptureCopy.cta("PURCHASE", amountLabel),
            enabled = postable,
            caveat = RetailCaptureCopy.DRAFT_FOOTER,
            onClick = {
                val filled = lines.filter { it.name.isNotBlank() }
                val paymentStatus = when (tender) {
                    "Pay now" -> "PAID"
                    "Part" -> "PARTIAL"
                    else -> "OPEN"
                }
                retailMoneySavedToast()
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
                            receiptState = "RECEIVED",
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
    if (showScanner) {
        RetailBarcodeScanner(
            title = "Scan invoice / SKU",
            subtitle = "Point at a supplier invoice QR or product barcode",
            onCode = { code ->
                showScanner = false
                applyPurchaseScan(code)
            },
            onDismiss = { showScanner = false },
        )
    }
    }
}

@Composable
private fun purchaseLineRow(
    currency: String,
    line: CaptureLineDraft,
    onChange: (CaptureLineDraft) -> Unit,
    onRemove: () -> Unit,
) {
    var editing by remember(line.id) { mutableStateOf(line.name.isBlank()) }
    val meta = listOfNotNull(
        line.qty.takeIf { it.isNotBlank() }?.let { "$it ${line.unit}" },
        line.rate.toBigDecimalOrNull()?.takeIf { it.signum() > 0 }
            ?.let { RetailFormat.exact(currency, it.toPlainString()) },
        line.hsn.takeIf { it.isNotBlank() }?.let { "HSN $it" },
    ).joinToString(" · ")
    if (!editing && line.name.isNotBlank()) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { editing = true }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    line.sku.ifBlank { "SKU" },
                    style = RetailRose.rowCaption(RetailRose.amber).copy(fontSize = 10.sp),
                    maxLines = 1,
                )
                Text(line.name, style = RetailRose.rowTitle().copy(fontSize = 13.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (meta.isNotBlank()) {
                    Text(meta, style = RetailRose.rowCaption(RetailRose.textBody).copy(fontSize = 11.sp), maxLines = 1)
                }
            }
            Text(
                RetailFormat.exact(currency, line.lineTotal().takeIf { it.signum() > 0 }?.toPlainString()),
                style = RetailRose.rowValue().copy(fontSize = 13.sp),
            )
        }
        return
    }
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
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Done", style = RetailRose.viewAll(), modifier = Modifier.clickable { editing = false })
            Text("Remove", style = RetailRose.rowCaption(RetailRose.warning), modifier = Modifier.clickable(onClick = onRemove))
        }
    }
}

/** `2399:30194` Collection — allocates open AR only; never new revenue. */
@Composable
private fun RetailCollectionSheet(
    model: OperatingProjectionDto,
    onDismiss: () -> Unit,
    onSubmit: (RetailEventDraft) -> Unit,
) {
    val currency = model.currency ?: "INR"
    val storeLine = RetailCaptureCopy.storeLine(model.title, model.location)
    val open = model.paymentReadiness.filter { it.relevance == "RECEIVABLE" }
    var selectedId by remember { mutableStateOf<String?>(open.firstOrNull()?.id) }
    var amount by remember { mutableStateOf(open.firstOrNull()?.amount.orEmpty()) }
    var tender by remember { mutableStateOf("UPI") }
    var reference by remember { mutableStateOf("UPI/2810/9912") }
    var receiptPrint by remember { mutableStateOf("On") }
    var moreOpen by remember { mutableStateOf(false) }
    val row = open.firstOrNull { it.id == selectedId }
    val openAr = row?.amount?.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val receipt = amount.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val remaining = (openAr - receipt).max(BigDecimal.ZERO)
    val amountLabel = amount.toBigDecimalOrNull()?.takeIf { it.signum() == 1 }
        ?.let { RetailFormat.exact(currency, amount) }
    val postable = row != null && receipt.signum() == 1
    val party = row?.counterpartyName?.takeIf { it.isNotBlank() } ?: "Mehta Traders"

    retailCaptureScaffold(
        title = RetailCaptureCopy.sheetTitle("COLLECTION"),
        subtitle = RetailCaptureCopy.collectionSubtitle(storeLine),
        onDismiss = onDismiss,
        why = RetailCaptureCopy.why("COLLECTION"),
        cta = RetailCaptureCopy.cta("COLLECTION", amountLabel),
        enabled = postable,
        hideFooter = true,
    ) {
        RoseLinesCard(
            title = "Allocate against",
            lineCount = if (row == null) 0 else 1,
            trailingOverride = when {
                open.isEmpty() -> "No open AR"
                else -> "${open.size} open · $party"
            },
        ) {
            if (open.isEmpty()) RoseEmpty("Nothing to collect", "Credit sales with open AR appear here.")
            open.take(6).forEach { item ->
                allocateAgainstRow(
                    item = item,
                    currency = currency,
                    selected = selectedId == item.id,
                    kind = AllocateKind.Receivable,
                    onClick = {
                        selectedId = item.id
                        amount = item.amount.orEmpty()
                    },
                )
            }
        }
        RoseTotalsCard(
            payableLabel = RetailCaptureCopy.amountLabel("COLLECTION"),
            payable = RetailFormat.exact(currency, amount),
            tender = tender,
            sideLabel = "Method",
        ) {
            RoseTotalRow(
                "Open AR",
                RetailFormat.exact(currency, row?.amount),
                valueColor = RetailRose.negative,
            )
            RoseTotalRow("This receipt", RetailFormat.exact(currency, amount.ifBlank { "0" }))
            RoseTotalRow("Remaining", RetailFormat.exact(currency, remaining.toPlainString()))
        }
        RoseChipRow(
            label = "Method",
            options = listOf("UPI", "Cash", "Card", "Bank"),
            selected = tender,
            tenderSelection = true,
        ) { tender = it }
        RosePickerRow(
            label = "Customer",
            value = party,
            externalLabel = true,
            onClick = {
                selectedId = open.firstOrNull()?.id
                amount = open.firstOrNull()?.amount.orEmpty()
            },
        )
        RosePickerRow(
            label = "Reference",
            value = reference.ifBlank { "UPI / NEFT ref" },
            externalLabel = true,
            onClick = { /* keep typed / fixture ref; expand via More details */ },
        )
        RosePickerRow(
            label = "Receipt print",
            value = receiptPrint,
            externalLabel = true,
            onClick = { receiptPrint = if (receiptPrint == "On") "Off" else "On" },
        )
        RoseFieldLabel("Collected amount")
        RoseTextField(
            amount,
            row?.amount?.let { "Up to ${RetailFormat.exact(currency, it)}" } ?: "0",
            onValueChange = { amount = it.filter { ch -> ch.isDigit() || ch == '.' }.take(12) },
        )
        RoseMoreDetails(
            expanded = moreOpen,
            onToggle = { moreOpen = !moreOpen },
            hint = RetailCaptureCopy.MORE_DETAILS_HINT,
        ) {
            RoseFieldLabel("Reference")
            RoseTextField(reference, "UPI / NEFT ref", onValueChange = { reference = it })
            Text(RetailCaptureCopy.MORE_DETAILS_HINT, style = RetailRose.rowCaption())
        }
        RoseStickyFooter(
            label = RetailCaptureCopy.cta("COLLECTION", amountLabel),
            enabled = postable,
            caveat = RetailCaptureCopy.DRAFT_FOOTER,
            onClick = {
                retailMoneySavedToast()
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

/** `2399:30253` Payment — settles open AP only; never new purchase cost. */
@Composable
private fun RetailPaymentSheet(
    model: OperatingProjectionDto,
    onDismiss: () -> Unit,
    onSubmit: (RetailEventDraft) -> Unit,
) {
    val currency = model.currency ?: "INR"
    val storeLine = RetailCaptureCopy.storeLine(model.title, model.location)
    val open = model.paymentReadiness.filter { it.relevance != "RECEIVABLE" }
    var selectedId by remember { mutableStateOf<String?>(open.firstOrNull()?.id) }
    var amount by remember { mutableStateOf(open.firstOrNull()?.amount.orEmpty()) }
    var tender by remember { mutableStateOf("Bank") }
    var reference by remember { mutableStateOf("NEFT/…") }
    var moreOpen by remember { mutableStateOf(false) }
    val row = open.firstOrNull { it.id == selectedId }
    val openAp = row?.amount?.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val paid = amount.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val remaining = (openAp - paid).max(BigDecimal.ZERO)
    val amountLabel = amount.toBigDecimalOrNull()?.takeIf { it.signum() == 1 }
        ?.let { RetailFormat.exact(currency, amount) }
    val postable = row != null && paid.signum() == 1
    val payee = row?.counterpartyName?.takeIf { it.isNotBlank() } ?: "Wholesale Foods"
    val linked = row?.title?.takeIf { it.isNotBlank() } ?: "PI-8841"

    retailCaptureScaffold(
        title = RetailCaptureCopy.sheetTitle("PAYMENT"),
        subtitle = RetailCaptureCopy.paymentSubtitle(storeLine),
        onDismiss = onDismiss,
        why = RetailCaptureCopy.why("PAYMENT"),
        cta = RetailCaptureCopy.cta("PAYMENT", amountLabel),
        enabled = postable,
        hideFooter = true,
    ) {
        RoseLinesCard(
            title = "Allocate against",
            lineCount = if (row == null) 0 else 1,
            trailingOverride = when {
                open.isEmpty() -> "No open AP"
                else -> "${open.size} open · $payee"
            },
        ) {
            if (open.isEmpty()) RoseEmpty("Nothing to pay", "Open purchases and expenses appear here.")
            open.take(6).forEach { item ->
                allocateAgainstRow(
                    item = item,
                    currency = currency,
                    selected = selectedId == item.id,
                    kind = AllocateKind.Payable,
                    onClick = {
                        selectedId = item.id
                        amount = item.amount.orEmpty()
                    },
                )
            }
        }
        RoseTotalsCard(
            payableLabel = RetailCaptureCopy.amountLabel("PAYMENT"),
            payable = RetailFormat.exact(currency, amount),
            tender = tender,
            sideLabel = "Method",
        ) {
            RoseTotalRow(
                "Open AP",
                RetailFormat.exact(currency, row?.amount),
                valueColor = RetailRose.negative,
            )
            RoseTotalRow("This payment", RetailFormat.exact(currency, amount.ifBlank { "0" }))
            RoseTotalRow("Remaining", RetailFormat.exact(currency, remaining.toPlainString()))
        }
        RoseChipRow(
            label = "Method",
            options = listOf("Bank", "UPI", "Cash", "Cheque"),
            selected = tender,
            tenderSelection = true,
        ) { tender = it }
        RosePickerRow(
            label = "Payee",
            value = payee,
            externalLabel = true,
            onClick = {
                selectedId = open.firstOrNull()?.id
                amount = open.firstOrNull()?.amount.orEmpty()
            },
        )
        RosePickerRow(
            label = "Reference",
            value = reference.ifBlank { "NEFT / cheque no." },
            externalLabel = true,
            onClick = { /* edit via More details */ },
        )
        RosePickerRow(
            label = "Linked purchase",
            value = linked,
            externalLabel = true,
            onClick = {
                selectedId = open.firstOrNull()?.id
                amount = open.firstOrNull()?.amount.orEmpty()
            },
        )
        RoseFieldLabel("Payment amount")
        RoseTextField(amount, "0", onValueChange = { amount = it.filter { ch -> ch.isDigit() || ch == '.' }.take(12) })
        RoseMoreDetails(
            expanded = moreOpen,
            onToggle = { moreOpen = !moreOpen },
            hint = RetailCaptureCopy.MORE_DETAILS_HINT,
        ) {
            RoseFieldLabel("Reference")
            RoseTextField(reference, "NEFT / cheque no.", onValueChange = { reference = it })
            Text(RetailCaptureCopy.MORE_DETAILS_HINT, style = RetailRose.rowCaption())
        }
        RoseStickyFooter(
            label = RetailCaptureCopy.cta("PAYMENT", amountLabel),
            enabled = postable,
            caveat = RetailCaptureCopy.DRAFT_FOOTER,
            onClick = {
                retailMoneySavedToast()
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

/**
 * Expense · `2399:30306` — outside the 23-node POS batch; kept on hub for running-cost capture.
 * Rose money chrome + honesty Why (cost vs payment) aligned with Collection / Payment siblings.
 */
@Composable
private fun RetailExpenseSheet(
    model: OperatingProjectionDto,
    onDismiss: () -> Unit,
    onSubmit: (RetailEventDraft) -> Unit,
) {
    val currency = model.currency ?: "INR"
    val storeLine = RetailCaptureCopy.storeLine(model.title, model.location)
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
        subtitle = "Running cost · $storeLine",
        onDismiss = onDismiss,
        why = RetailCaptureCopy.why("SPEND"),
        cta = if (postable) "Post expense · ${RetailFormat.exact(currency, amount)}" else "Post expense",
        enabled = postable,
        hideFooter = true,
    ) {
        RoseChipRow(label = null, options = listOf("Store", "Ops", "Other"), selected = mode) { mode = it }
        RoseTotalsCard(
            payableLabel = "Expense",
            payable = RetailFormat.exact(currency, amount),
            tender = if (payState == "Paid now") "Paid now" else "Pay later",
            sideLabel = expenseType,
            sideValueColor = RetailRose.amber,
            sideLabelColor = RetailRose.amber,
        ) {
            RoseTotalRow("Amount", RetailFormat.exact(currency, amount.ifBlank { "0" }))
            RoseUnavailableGstRow(
                label = "GST (if any)",
                caption = "Unavailable — never invent GST on expense; payment allocates separately",
            )
            RoseTotalRow("Cash impact", if (payState == "Paid now") "Paid now" else "Pay later · AP later")
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

/** `2399:30359` Stock update — Set current / Add / Reduce */
@Composable
/** `2399:30359` Stock Update — quantity delta only; never invents unit cost / BOM. */
private fun RetailStockUpdateSheet(
    model: OperatingProjectionDto,
    onDismiss: () -> Unit,
    onSubmit: (RetailEventDraft) -> Unit,
) {
    var action by remember { mutableStateOf(RetailCaptureCopy.modeChips("STOCK").first()) }
    var scanQuery by remember { mutableStateOf("") }
    var showScanner by remember { mutableStateOf(false) }
    var reason by remember { mutableStateOf("Cycle count") }
    var location by remember { mutableStateOf("Aisle 3") }
    var countedBy by remember { mutableStateOf("Ravi · floor") }
    var note by remember { mutableStateOf("") }
    var moreOpen by remember { mutableStateOf(false) }
    val seed = retailCatalogMasters(model).firstOrNull()
    val lines = remember {
        mutableStateListOf(
            CaptureLineDraft(
                id = System.nanoTime(),
                sku = seed?.code ?: "HM-044",
                name = seed?.name ?: "Basmati 5kg",
                qty = "8",
                unit = seed?.unit ?: "bag",
                wasQty = "12",
            ),
        )
    }
    val skus = retailCatalogMasters(model)
    val filled = lines.filter { it.name.isNotBlank() }
    val first = filled.firstOrNull()
    val qtyValue = first?.qty?.toBigDecimalOrNull()
    val stockActionCode = OperatingStockReasons.stockAction(action)
    val stockReasonCode = OperatingStockReasons.reasonCode(reason)
    val postable = first != null && stockReasonCode != null && qtyValue != null &&
        (stockActionCode == "SET" && qtyValue.signum() >= 0 || stockActionCode != "SET" && qtyValue.signum() > 0)

    fun applyStockScan(raw: String) {
        val q = raw.trim()
        if (q.isBlank()) return
        val match = skus.firstOrNull {
            it.name.contains(q, true) || (it.code?.contains(q, true) == true) ||
                (it.barcode?.contains(q, true) == true)
        }
        val draft = CaptureLineDraft(
            id = System.nanoTime(),
            name = match?.name ?: q,
            sku = (match?.code ?: q.take(12)).uppercase(),
            unit = match?.unit ?: "pcs",
            qty = "1",
            wasQty = "",
        )
        val blank = lines.indexOfFirst { it.name.isBlank() }
        if (blank >= 0) lines[blank] = draft else lines.add(draft)
        scanQuery = ""
    }

    Box {
    retailCaptureScaffold(
        title = RetailCaptureCopy.sheetTitle("STOCK"),
        subtitle = RetailCaptureCopy.stockSubtitle("Register aisle"),
        onDismiss = onDismiss,
        why = RetailCaptureCopy.why("STOCK"),
        cta = RetailCaptureCopy.cta("STOCK"),
        enabled = postable,
        hideFooter = true,
    ) {
        RoseChipRow(
            label = null,
            options = RetailCaptureCopy.modeChips("STOCK"),
            selected = action,
        ) { action = it }

        RoseFieldLabel("Scan SKU / barcode")
        RoseScanField(
            query = scanQuery,
            placeholder = RetailCaptureCopy.STOCK_SCAN_PLACEHOLDER,
            onQueryChange = { scanQuery = it },
            onScan = {
                val q = scanQuery.trim()
                if (q.isNotBlank()) {
                    applyStockScan(q)
                } else {
                    showScanner = true
                }
            },
        )

        RoseLinesCard(
            title = "Count lines",
            lineCount = filled.size,
            trailingOverride = "${filled.size} SKU${if (filled.size == 1) "" else "s"} · ${reason.lowercase()}",
        ) {
            lines.forEachIndexed { index, line ->
                stockCountLineRow(
                    line = line,
                    location = location,
                    onChange = { lines[index] = it },
                    onRemove = { if (lines.size > 1) lines.removeAt(index) },
                )
            }
            Row(
                Modifier.fillMaxWidth().padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "+ Add SKU",
                    style = RetailRose.viewAll().copy(fontSize = 13.sp),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { lines.add(newCaptureLine()) },
                )
                Text(RetailCaptureCopy.STOCK_ADD_SKU_HINT, style = RetailRose.rowCaption())
            }
        }

        RosePickerRow(
            label = "Reason",
            value = reason,
            externalLabel = true,
            onClick = { reason = nextStockReason(reason) },
        )
        RosePickerRow(
            label = "Location",
            value = location,
            externalLabel = true,
            onClick = { location = nextStockLocation(location) },
        )
        RosePickerRow(
            label = "Counted by",
            value = countedBy,
            externalLabel = true,
            onClick = { countedBy = nextCountedBy(countedBy) },
        )

        RoseMoreDetails(
            expanded = moreOpen,
            onToggle = { moreOpen = !moreOpen },
            hint = RetailCaptureCopy.STOCK_MORE_HINT,
        ) {
            RoseTextField(countedBy, "Counted by (custom)", onValueChange = { countedBy = it })
            RoseTextField(note, "Optional reference", onValueChange = { note = it }, singleLine = false)
        }

        RoseStickyFooter(
            label = RetailCaptureCopy.cta("STOCK"),
            enabled = postable,
            caveat = RetailCaptureCopy.DRAFT_FOOTER,
            onClick = {
                val primary = filled.firstOrNull() ?: return@RoseStickyFooter
                val extras = filled.drop(1).map { it.name }.filter { it.isNotBlank() }
                val noteParts = listOfNotNull(
                    note.takeIf { it.isNotBlank() },
                    "Action: $action",
                    "Location: $location",
                    countedBy.takeIf { it.isNotBlank() }?.let { "Counted by: $it" },
                    extras.takeIf { it.isNotEmpty() }?.let { "SKUs: ${it.joinToString(", ")}" },
                )
                onSubmit(
                    RetailEventDraft(
                        eventType = "STOCK",
                        title = primary.name,
                        amount = null,
                        note = noteParts.joinToString(" · ").ifBlank { null },
                        quantity = primary.qty,
                        unit = primary.unit.takeIf { it.isNotBlank() },
                        itemName = primary.name,
                        contract = OperatingContractBody(
                            stockAction = stockActionCode,
                            stockReason = stockReasonCode,
                            locationName = location.takeIf { it.isNotBlank() },
                            staffName = countedBy.takeIf { it.isNotBlank() },
                            declaredQuantity = if (stockActionCode == "SET") primary.qty else null,
                        ),
                        lines = filled.map {
                            OperatingLineBody(
                                itemName = it.name,
                                quantity = it.qty,
                                unit = it.unit.takeIf { u -> u.isNotBlank() },
                                unitPrice = null,
                            )
                        },
                    ),
                )
            },
        )
    }
    if (showScanner) {
        RetailBarcodeScanner(
            title = "Scan SKU / barcode",
            subtitle = "Point at a product barcode or QR",
            onCode = { code ->
                showScanner = false
                applyStockScan(code)
            },
            onDismiss = { showScanner = false },
        )
    }
    }
}

@Composable
private fun stockCountLineRow(
    line: CaptureLineDraft,
    location: String,
    onChange: (CaptureLineDraft) -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                line.sku.ifBlank { "SKU" },
                style = RetailRose.rowCaption(RetailRose.amber).copy(fontSize = 10.sp),
                maxLines = 1,
            )
            if (line.name.isBlank()) {
                RoseTextField(line.name, "Item", onValueChange = { onChange(line.copy(name = it)) })
            } else {
                Text(line.name, style = RetailRose.rowTitle().copy(fontSize = 13.sp), maxLines = 1)
            }
            Text(
                listOfNotNull(
                    location.takeIf { it.isNotBlank() },
                    line.wasQty.takeIf { it.isNotBlank() }?.let { "was $it" },
                ).joinToString(" · ").ifBlank { "Set qty" },
                style = RetailRose.rowCaption(RetailRose.textBody).copy(fontSize = 11.sp),
            )
        }
        val qtyInt = line.qty.toIntOrNull() ?: 0
        RoseStepper(
            quantity = qtyInt,
            onDecrement = {
                if (qtyInt > 0) onChange(line.copy(qty = (qtyInt - 1).toString()))
                else onRemove()
            },
            onIncrement = { onChange(line.copy(qty = (qtyInt + 1).toString())) },
        )
        Text("→ ${line.qty}", style = RetailRose.rowValue().copy(fontSize = 13.sp))
    }
}

private fun nextStockReason(current: String): String {
    val options = listOf("Cycle count", "Opening count", "Stocktake", "Damage", "Waste", "Return", "Correction")
    val idx = options.indexOf(current).let { if (it < 0) 0 else it }
    return options[(idx + 1) % options.size]
}

private fun nextStockLocation(current: String): String {
    val options = listOf("Aisle 3", "Back store", "Register", "Floor")
    val idx = options.indexOf(current).let { if (it < 0) 0 else it }
    return options[(idx + 1) % options.size]
}

private fun nextCountedBy(current: String): String {
    val options = listOf("Ravi · floor", "Counter · shift", "Owner")
    val idx = options.indexOf(current)
    return options[(idx + 1) % options.size]
}

private enum class OrderSheetMode { SalesOrder, PurchaseOrder }

/** `2399:30418` Sales Order · `2443:29784` Purchase Order — commitment only (never SALE/AP/stock/GST). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RetailOrderSheet(
    model: OperatingProjectionDto,
    onDismiss: () -> Unit,
    onSubmit: (RetailEventDraft) -> Unit,
    companyId: String? = null,
    onMasterSubmit: (RetailEventDraft) -> Unit = onSubmit,
) {
    var mode by remember { mutableStateOf(OrderSheetMode.SalesOrder) }
    var stateChip by remember { mutableStateOf("Confirmed") }
    var category by remember { mutableStateOf("Staples") }
    var party by remember { mutableStateOf("Shah Mart") }
    var promise by remember { mutableStateOf("02 Nov") }
    var deliverTo by remember { mutableStateOf("Pickup · Andheri") }
    var placeOfSupply by remember { mutableStateOf("Maharashtra · CGST+SGST") }
    var search by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var moreOpen by remember { mutableStateOf(false) }
    var createKind by remember { mutableStateOf<RetailMasterFormKind?>(null) }
    val currency = model.currency ?: "INR"
    val storeLine = RetailCaptureCopy.storeLine(model.title, model.location)
    val catalog = retailCatalogMasters(model)
    val lines = remember { mutableStateListOf<CaptureLineDraft>() }
    val goods = lines.goodsTotal()
    val amountStr = goods.stripTrailingZeros().toPlainString()
    val amountLabel = if (goods.signum() == 1) RetailFormat.exact(currency, amountStr) else null
    val postable = goods.signum() == 1
    val isPo = mode == OrderSheetMode.PurchaseOrder
    val copyMode = if (isPo) "PURCHASE_ORDER" else "SALES_ORDER"
    val categories = if (isPo) RetailCaptureCopy.poCategories else RetailCaptureCopy.soCategories
    val suppliers = model.masters.orEmpty().filter { it.kind.equals("SUPPLIER", true) }
    val customers = model.masters.orEmpty().filter {
        it.kind.equals("CUSTOMER", true) || it.kind.equals("BUYER", true)
    }
    val filledCount = lines.count { it.name.isNotBlank() }
    val linesTrailing = buildString {
        append("$filledCount SKU${if (filledCount == 1) "" else "s"}")
        if (promise.isNotBlank()) {
            append(if (isPo) " · expect by $promise" else " · fulfil by $promise")
        }
    }

    fun applyMode(next: OrderSheetMode) {
        mode = next
        if (next == OrderSheetMode.PurchaseOrder) {
            stateChip = "Sent"
            party = suppliers.firstOrNull()?.name ?: "Wholesale Foods"
            promise = "05 Nov"
            deliverTo = "Back store"
            placeOfSupply = "Maharashtra (27) · intra-state"
            category = RetailCaptureCopy.poCategories.first()
        } else {
            stateChip = "Confirmed"
            party = customers.firstOrNull()?.name ?: "Shah Mart"
            promise = "02 Nov"
            deliverTo = "Pickup · Andheri"
            placeOfSupply = "Maharashtra · CGST+SGST"
            category = RetailCaptureCopy.soCategories.first()
        }
    }

    fun addSku(sku: OperatingMasterDto, defaultQty: String = "1") {
        val rate = (if (isPo) sku.purchasePrice else sku.sellPrice)
            ?: sku.purchasePrice ?: sku.sellPrice.orEmpty()
        val draft = CaptureLineDraft(
            id = System.nanoTime(),
            sku = sku.code ?: sku.name.take(6).uppercase(),
            name = sku.name,
            qty = defaultQty,
            rate = rate,
            hsn = sku.hsn.orEmpty(),
            unit = sku.unit ?: "pcs",
            gst = sku.gstRate.orEmpty(),
        )
        val blank = lines.indexOfFirst { it.name.isBlank() }
        if (blank >= 0) lines[blank] = draft else lines.add(draft)
    }

    Box {
        retailCaptureScaffold(
            title = RetailCaptureCopy.sheetTitle(copyMode),
            subtitle = if (isPo) {
                RetailCaptureCopy.purchaseOrderSubtitle(storeLine)
            } else {
                RetailCaptureCopy.salesOrderSubtitle(storeLine)
            },
            onDismiss = onDismiss,
            why = RetailCaptureCopy.why(copyMode),
            cta = RetailCaptureCopy.cta(copyMode, amountLabel),
            enabled = postable,
            hideFooter = true,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                RoseModeChip("Sales order", !isPo, onClick = { applyMode(OrderSheetMode.SalesOrder) })
                RoseModeChip("Purchase order", isPo, onClick = { applyMode(OrderSheetMode.PurchaseOrder) })
            }
            RoseChipRow(
                label = null,
                options = if (isPo) listOf("Sent", "Draft", "Partial") else listOf("Confirmed", "Hold", "Draft"),
                selected = stateChip,
            ) { stateChip = it }

            RoseSectionCard(
                label = if (isPo) "Stock items" else "Catalogue",
                trailing = if (isPo) RetailCaptureCopy.PO_BROWSER_TRAILING else RetailCaptureCopy.SO_BROWSER_TRAILING,
            ) {
                RoseScanField(
                    query = search,
                    placeholder = if (isPo) "Search SKU / item" else "Search SKU, name or barcode",
                    onQueryChange = { search = it },
                    onScan = {
                        val q = search.trim()
                        if (q.isBlank()) return@RoseScanField
                        val match = catalog.firstOrNull {
                            it.name.contains(q, true) || it.code?.contains(q, true) == true ||
                                it.barcode?.contains(q, true) == true
                        }
                        if (match != null) addSku(match)
                        search = ""
                    },
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    categories.forEach { cat ->
                        RoseModeChip(
                            text = cat,
                            selected = cat == category,
                            category = true,
                            onClick = { category = cat },
                        )
                    }
                }
                val byCategory = catalog.filter { it.retailStation().equals(category, true) }
                val pool = byCategory.ifEmpty { catalog }
                val filtered = pool.filter { sku ->
                    search.isBlank() ||
                        sku.name.contains(search, true) ||
                        (sku.code?.contains(search, true) == true) ||
                        (sku.barcode?.contains(search, true) == true)
                }.take(6)
                if (filtered.isEmpty()) {
                    Text("No rows in $category — try another chip or + New.", style = RetailRose.rowCaption())
                }
                filtered.forEach { sku ->
                    orderBrowserRow(
                        currency = currency,
                        sku = sku,
                        isPo = isPo,
                        onAdd = { addSku(sku) },
                    )
                }
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
                lineCount = filledCount,
                trailingOverride = if (filledCount == 0) "No lines yet" else linesTrailing,
            ) {
                lines.filter { it.name.isNotBlank() }.forEach { line ->
                    val index = lines.indexOfFirst { it.id == line.id }
                    orderLineRow(
                        currency = currency,
                        line = line,
                        onChange = { if (index >= 0) lines[index] = it },
                        onRemove = { if (index >= 0) lines.removeAt(index) },
                    )
                }
                Row(
                    Modifier.fillMaxWidth().padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "+ Add line",
                        style = RetailRose.viewAll().copy(fontSize = 13.sp),
                        modifier = Modifier.clickable { lines.add(newCaptureLine()) },
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        if (isPo) RetailCaptureCopy.PO_ADD_LINE_HINT else RetailCaptureCopy.SO_ADD_LINE_HINT,
                        style = RetailRose.rowCaption(),
                    )
                }
            }

            RoseTotalsCard(
                payableLabel = if (isPo) "PO value" else "SO value",
                payable = RetailFormat.exact(currency, amountStr),
                tender = stateChip,
                sideLabel = "Status",
                sideValueColor = RetailRose.positive,
                sideLabelColor = RetailRose.amber,
            ) {
                val orderGst = RetailFormat.gstEstimate(
                    lineAmounts = lines.map { it.lineTotal() },
                    lineRates = lines.map { it.gst.takeIf { g -> g.isNotBlank() } },
                    inclusive = !isPo,
                )
                if (isPo) {
                    RoseTotalRow("Goods", RetailFormat.exact(currency, amountStr))
                    RoseGstEstimateRow(
                        label = "Est. input GST · CGST+SGST · on GRN",
                        estimate = orderGst,
                        currency = currency,
                        captionWhenKnown = "Estimate only — never posted from PO; GST on Purchase / GRN",
                        captionWhenMissing = "Unavailable — rate missing on line / master",
                    )
                    RoseTotalRow("Freight (est.)", "₹0")
                } else {
                    RoseTotalRow("Lines value", RetailFormat.exact(currency, amountStr))
                    RoseGstEstimateRow(
                        label = "Est. GST · on bill",
                        estimate = orderGst,
                        currency = currency,
                        captionWhenKnown = "Estimate only — GST posts on Sale / POS Charge",
                        captionWhenMissing = "Unavailable — rate missing on line / master",
                    )
                    RoseTotalRow(
                        "Balance due on sale",
                        RetailFormat.exact(currency, amountStr),
                        valueColor = RetailRose.negative,
                    )
                }
            }

            RosePickerRow(
                label = if (isPo) "Supplier" else "Customer",
                value = party,
                externalLabel = true,
                onClick = {
                    val pool = if (isPo) suppliers else customers
                    party = pool.firstOrNull { it.name != party }?.name
                        ?: pool.firstOrNull()?.name
                        ?: if (isPo) "Wholesale Foods" else "Shah Mart"
                },
            )
            RosePickerRow(
                label = if (isPo) "Expect by" else "Fulfil by",
                value = promise,
                externalLabel = true,
                onClick = {
                    promise = if (isPo) {
                        if (promise == "05 Nov") "12 Nov" else "05 Nov"
                    } else {
                        if (promise == "02 Nov") "05 Nov" else "02 Nov"
                    }
                },
            )
            RosePickerRow(
                label = if (isPo) "Deliver to" else "Ship / pickup",
                value = deliverTo,
                externalLabel = true,
                onClick = {
                    deliverTo = if (isPo) {
                        if (deliverTo == "Back store") "Floor" else "Back store"
                    } else {
                        if (deliverTo.contains("Pickup")) "Delivery · Andheri" else "Pickup · Andheri"
                    }
                },
            )
            RosePickerRow(
                label = "Place of supply",
                value = placeOfSupply,
                externalLabel = true,
                onClick = {
                    placeOfSupply = if (placeOfSupply.contains("27")) {
                        "Maharashtra · CGST+SGST"
                    } else {
                        "Maharashtra (27) · intra-state"
                    }
                },
            )
            RoseMoreDetails(
                expanded = moreOpen,
                onToggle = { moreOpen = !moreOpen },
                hint = if (isPo) RetailCaptureCopy.PO_MORE_HINT else RetailCaptureCopy.SO_MORE_HINT,
            ) {
                RoseTextField(note, "Notes · Terms · Tags", onValueChange = { note = it }, singleLine = false)
            }
            RoseStickyFooter(
                label = RetailCaptureCopy.cta(copyMode, amountLabel),
                enabled = postable,
                caveat = if (isPo) {
                    RetailCaptureCopy.PO_COMMITMENT_FOOTER
                } else {
                    RetailCaptureCopy.SO_COMMITMENT_FOOTER
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
private fun orderBrowserRow(
    currency: String,
    sku: OperatingMasterDto,
    isPo: Boolean,
    onAdd: () -> Unit,
) {
    val price = if (isPo) sku.purchasePrice ?: sku.sellPrice else sku.sellPrice ?: sku.purchasePrice
    val priceLabel = when {
        price.isNullOrBlank() -> "—"
        isPo && sku.unit != null -> "${RetailFormat.exact(currency, price)}/${sku.unit}"
        else -> RetailFormat.exact(currency, price)
    }
    val meta = if (isPo) {
        listOfNotNull(
            sku.unit,
            "Back store",
            sku.retailOnHandCaption(),
        ).joinToString(" · ")
    } else {
        listOfNotNull(
            sku.code,
            sku.hsn?.let { "HSN $it" },
            sku.gstRate?.let { if (it.equals("Exempt", true)) "Exempt" else "$it%" },
        ).joinToString(" · ")
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF334155))
            .border(1.dp, RetailRose.rose.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(sku.name, style = RetailRose.rowTitle().copy(fontSize = 13.sp), maxLines = 1)
            if (meta.isNotBlank()) {
                Text(meta, style = RetailRose.rowCaption(RetailRose.textFaint).copy(fontSize = 11.sp), maxLines = 1)
            }
        }
        Text(priceLabel, style = RetailRose.rowValue(RetailRose.amber).copy(fontSize = 13.sp))
        Text(
            "Add",
            style = RetailRose.chipLabel(true).copy(
                color = RetailRose.field,
                fontSize = 12.sp,
            ),
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(RetailRose.rose)
                .clickable(onClick = onAdd)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun orderLineRow(
    currency: String,
    line: CaptureLineDraft,
    onChange: (CaptureLineDraft) -> Unit,
    onRemove: () -> Unit,
) {
    val meta = listOfNotNull(
        line.rate.takeIf { it.isNotBlank() }?.let { RetailFormat.exact(currency, it) },
        line.unit.takeIf { it.isNotBlank() },
        line.hsn.takeIf { it.isNotBlank() }?.let { "HSN $it" },
        line.gst.takeIf { it.isNotBlank() }?.let { if (it.equals("Exempt", true)) "Exempt" else "$it%" },
    ).joinToString(" · ")
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                line.sku.ifBlank { "SKU" },
                style = RetailRose.rowCaption(RetailRose.amber).copy(fontSize = 10.sp),
                maxLines = 1,
            )
            Text(line.name, style = RetailRose.rowTitle().copy(fontSize = 13.sp), maxLines = 1)
            if (meta.isNotBlank()) {
                Text(meta, style = RetailRose.rowCaption(RetailRose.textBody).copy(fontSize = 11.sp), maxLines = 1)
            }
        }
        val qtyInt = line.qty.toIntOrNull() ?: 1
        RoseStepper(
            quantity = qtyInt,
            onDecrement = { if (qtyInt > 1) onChange(line.copy(qty = (qtyInt - 1).toString())) else onRemove() },
            onIncrement = { onChange(line.copy(qty = (qtyInt + 1).toString())) },
        )
        Text(
            RetailFormat.exact(currency, line.lineTotal().takeIf { it.signum() > 0 }?.toPlainString()),
            style = RetailRose.rowValue().copy(fontSize = 13.sp),
        )
    }
}

/**
 * `2399:30482` Status / Assignment — Dispatch / Fulfil / Pick.
 * Ops-only: posts ORDER status signal with amount=null; never SALE or STOCK.
 */
@Composable
private fun RetailStatusSheet(
    model: OperatingProjectionDto,
    onDismiss: () -> Unit,
    onSubmit: (RetailEventDraft) -> Unit,
) {
    val storeLine = RetailCaptureCopy.storeLine(model.title, model.location)
    val orders = model.activity.filter {
        it.eventType.equals("ORDER", true) || it.eventType.equals("CUSTOMER_ORDER", true)
    }.take(6)
    data class SourceRow(
        val id: String?,
        val code: String,
        val title: String,
        val meta: String,
        val badge: String,
        val badgePositive: Boolean,
    )
    val sources = remember(orders) {
        if (orders.isEmpty()) {
            listOf(
                SourceRow(
                    id = null,
                    code = "SO-441",
                    title = "Customer order",
                    meta = "12 SKUs · fulfil today",
                    badge = "Ready",
                    badgePositive = true,
                ),
                SourceRow(
                    id = null,
                    code = "Line pick",
                    title = "Basmati 5kg ×4",
                    meta = "Aisle 3",
                    badge = "Queued",
                    badgePositive = false,
                ),
            )
        } else {
            orders.map { order ->
                SourceRow(
                    id = order.id,
                    code = order.title.substringBefore('·').trim().ifBlank { "SO" },
                    title = order.title,
                    meta = order.status ?: "OPEN",
                    badge = order.status ?: "Ready",
                    badgePositive = order.status?.contains("READY", true) == true ||
                        order.status?.contains("OPEN", true) == true,
                )
            }
        }
    }
    var selectedKey by remember {
        mutableStateOf(sources.firstOrNull()?.let { it.id ?: it.code })
    }
    var mode by remember { mutableStateOf(RetailCaptureCopy.modeChips("STATUS").first()) }
    var status by remember { mutableStateOf("Ready to pick") }
    var assignee by remember { mutableStateOf("Ravi · floor") }
    var due by remember { mutableStateOf("Today · 6pm") }
    var notify by remember { mutableStateOf("Customer WhatsApp") }
    var moreOpen by remember { mutableStateOf(false) }
    val selected = sources.firstOrNull { (it.id ?: it.code) == selectedKey } ?: sources.firstOrNull()
    // Fixture-only rows stay postable as ops signal (amount null) — never invent SALE/STOCK.
    val postable = selected != null
    val statusToken = when (status) {
        "Picking" -> "PICKING"
        "Ready" -> "READY"
        "Dispatched" -> "DISPATCHED"
        else -> "READY_TO_PICK"
    }
    val sourceTrailing = selected?.let { "${it.code} · ${it.title.substringAfter('·').trim().ifBlank { "Shah Mart" }}" }
        ?: "SO-441 · Shah Mart"

    retailCaptureScaffold(
        title = RetailCaptureCopy.sheetTitle("STATUS"),
        subtitle = RetailCaptureCopy.statusSubtitle(storeLine),
        onDismiss = onDismiss,
        why = RetailCaptureCopy.why("STATUS"),
        cta = RetailCaptureCopy.cta("STATUS"),
        enabled = postable,
        hideFooter = true,
    ) {
        RoseChipRow(
            label = null,
            options = RetailCaptureCopy.modeChips("STATUS"),
            selected = mode,
        ) { mode = it }

        RoseLinesCard(
            title = "Source",
            lineCount = sources.size,
            trailingOverride = sourceTrailing,
        ) {
            sources.forEach { row ->
                val key = row.id ?: row.code
                val picked = key == selectedKey
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (picked) RetailRose.rose.copy(alpha = 0.16f) else Color.Transparent)
                        .clickable { selectedKey = key }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            row.code,
                            style = RetailRose.rowCaption(RetailRose.amber).copy(fontSize = 10.sp),
                            maxLines = 1,
                        )
                        Text(row.title, style = RetailRose.rowTitle().copy(fontSize = 13.sp), maxLines = 1)
                        Text(row.meta, style = RetailRose.rowCaption(RetailRose.textBody).copy(fontSize = 11.sp), maxLines = 1)
                    }
                    Text(
                        row.badge,
                        style = RetailRose.rowValue(
                            if (row.badgePositive) RetailRose.positive else RetailRose.amber,
                        ).copy(fontSize = 13.sp),
                    )
                }
            }
        }

        RoseChipRow(
            label = "Next status",
            options = listOf("Ready to pick", "Picking", "Ready", "Dispatched"),
            selected = status,
            tenderSelection = true,
        ) { status = it }

        RosePickerRow(
            label = "Assignee",
            value = assignee,
            externalLabel = true,
            onClick = { assignee = nextCountedBy(assignee) },
        )
        RosePickerRow(
            label = "Due",
            value = due,
            externalLabel = true,
            onClick = {
                due = if (due.contains("6pm")) "Tomorrow · 11am" else "Today · 6pm"
            },
        )
        RosePickerRow(
            label = "Notify",
            value = notify,
            externalLabel = true,
            onClick = {
                notify = if (notify == "Customer WhatsApp") "None" else "Customer WhatsApp"
            },
        )
        RoseMoreDetails(
            expanded = moreOpen,
            onToggle = { moreOpen = !moreOpen },
            hint = RetailCaptureCopy.STATUS_MORE_HINT,
        ) {
            Text(RetailCaptureCopy.STATUS_MORE_HINT, style = RetailRose.rowCaption())
        }
        RoseStickyFooter(
            label = RetailCaptureCopy.cta("STATUS"),
            enabled = postable,
            caveat = RetailCaptureCopy.DRAFT_FOOTER,
            onClick = {
                // Ops signal only — amount null, eventType ORDER with activityStatus; never SALE/STOCK.
                onSubmit(
                    RetailEventDraft(
                        eventType = "ORDER",
                        title = "Status · ${selected?.title ?: "Order"}",
                        amount = null,
                        note = listOfNotNull(
                            assignee.takeIf { it.isNotBlank() }?.let { "Assigned: $it" },
                            "Mode: $mode",
                            "Due: $due",
                            "Notify: $notify",
                            "Next: $status",
                        ).joinToString(" · "),
                        parentEventId = selected?.id,
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

/** `2399:30539` Budget / Target Revision — versioned targets only; never rewrites posted sales. */
@Composable
private fun RetailBudgetSheet(
    model: OperatingProjectionDto,
    onDismiss: () -> Unit,
    onSubmit: (RetailEventDraft) -> Unit,
) {
    val currency = model.currency ?: "INR"
    val storeLine = RetailCaptureCopy.storeLine(model.title, model.location)
    var metric by remember { mutableStateOf(RetailCaptureCopy.modeChips("BUDGET").first()) }
    val previousForMetric: (String) -> String = { selected ->
        val fromModel = if (selected.equals("Budget", ignoreCase = true)) {
            model.budget?.amount
        } else {
            model.target?.value
        }
        fromModel?.takeIf { it.isNotBlank() } ?: RetailCaptureCopy.BUDGET_FIXTURE_PREVIOUS
    }
    var amount by remember {
        mutableStateOf(
            previousForMetric("Sales target").toBigDecimalOrNull()
                ?.add(BigDecimal("100000"))
                ?.stripTrailingZeros()
                ?.toPlainString()
                ?: RetailCaptureCopy.BUDGET_FIXTURE_NEW,
        )
    }
    var period by remember { mutableStateOf(RetailFormat.month(model.startDate)) }
    var effective by remember {
        mutableStateOf(
            RetailFormat.shortDay(model.startDate)
                ?: RetailFormat.shortDay(LocalDate.now().withDayOfMonth(1).toString())
                ?: "01 Nov",
        )
    }
    var reason by remember { mutableStateOf(RetailCaptureCopy.BUDGET_FIXTURE_REASON) }
    var owner by remember { mutableStateOf(RetailCaptureCopy.BUDGET_FIXTURE_OWNER) }
    var note by remember { mutableStateOf("") }
    var moreOpen by remember { mutableStateOf(false) }
    val previous = previousForMetric(metric)
    val prevBd = previous.toBigDecimalOrNull()
    val nextBd = amount.toBigDecimalOrNull()
    val delta = if (prevBd != null && nextBd != null) nextBd - prevBd else null
    val postable = amount.toBigDecimalOrNull()?.signum() == 1
    val amountExact = RetailFormat.exact(currency, amount)
    val cta = RetailCaptureCopy.cta("BUDGET", if (postable) amountExact else null)
    val payableLabel = RetailCaptureCopy.budgetPayableLabel(metric)

    retailCaptureScaffold(
        title = RetailCaptureCopy.sheetTitle("BUDGET"),
        subtitle = RetailCaptureCopy.budgetSubtitle(storeLine),
        onDismiss = onDismiss,
        why = RetailCaptureCopy.why("BUDGET"),
        cta = cta,
        enabled = postable,
        hideFooter = true,
    ) {
        RoseChipRow(
            label = null,
            options = RetailCaptureCopy.modeChips("BUDGET"),
            selected = metric,
        ) { metric = it }

        RoseTotalsCard(
            payableLabel = payableLabel,
            payable = amountExact,
            tender = period,
            sideLabel = "Period",
            sideValueColor = RetailRose.amber,
            sideLabelColor = RetailRose.amber,
        ) {
            RoseTotalRow(
                "Previous v1",
                RetailFormat.exact(currency, previous),
                valueColor = RetailRose.textBody,
            )
            RoseTotalRow(
                "Delta",
                delta?.let {
                    val sign = if (it.signum() >= 0) "+ " else "− "
                    sign + RetailFormat.exact(currency, it.abs().toPlainString())
                } ?: "—",
                valueColor = RetailRose.textBody,
            )
            RoseTotalRow("Effective", effective, valueColor = RetailRose.textBody)
        }

        RosePickerRow("Period", period, onClick = null, externalLabel = true)
        RosePickerRow("Reason", reason.ifBlank { RetailCaptureCopy.BUDGET_FIXTURE_REASON }, onClick = null, externalLabel = true)
        RosePickerRow("Owner", owner.ifBlank { RetailCaptureCopy.BUDGET_FIXTURE_OWNER }, onClick = null, externalLabel = true)

        RoseMoreDetails(
            expanded = moreOpen,
            onToggle = { moreOpen = !moreOpen },
            hint = RetailCaptureCopy.BUDGET_MORE_HINT,
        ) {
            RoseFieldLabel(payableLabel)
            RoseTextField(
                amount,
                RetailCaptureCopy.BUDGET_FIXTURE_NEW,
                onValueChange = { amount = it.filter { ch -> ch.isDigit() || ch == '.' }.take(14) },
            )
            RoseFieldLabel("Period (edit)")
            RoseTextField(period, RetailFormat.month(model.startDate), onValueChange = { period = it.take(32) })
            RoseFieldLabel("Reason (edit)")
            RoseTextField(reason, RetailCaptureCopy.BUDGET_FIXTURE_REASON, onValueChange = { reason = it })
            RoseFieldLabel("Owner (edit)")
            RoseTextField(owner, RetailCaptureCopy.BUDGET_FIXTURE_OWNER, onValueChange = { owner = it })
            RoseFieldLabel("Effective")
            RoseTextField(effective, "01 Nov", onValueChange = { effective = it.take(16) })
            RoseFieldLabel("Notes")
            RoseTextField(note, "Optional note", onValueChange = { note = it }, singleLine = false)
        }

        RoseStickyFooter(
            label = cta,
            enabled = postable,
            caveat = RetailCaptureCopy.DRAFT_FOOTER,
            onClick = {
                onSubmit(
                    RetailEventDraft(
                        eventType = "BUDGET",
                        title = "$metric · $period",
                        amount = amount,
                        note = listOfNotNull(
                            reason.takeIf { it.isNotBlank() },
                            "Period: $period",
                            "Effective: $effective",
                            owner.takeIf { it.isNotBlank() }?.let { "Owner: $it" },
                            note.takeIf { it.isNotBlank() },
                            "Versioned plan — does not rewrite posted sales",
                        ).joinToString(" · ").ifBlank { null },
                        contract = OperatingContractBody(estimatedAmount = amount),
                    ),
                )
            },
        )
    }
}

/**
 * `2399:30597` Issue / Approval / Update —
 * register variance audit only; approval ≠ disbursement; never a sale edit.
 */
@Composable
private fun RetailIssueSheet(
    model: OperatingProjectionDto,
    onDismiss: () -> Unit,
    onSubmit: (RetailEventDraft) -> Unit,
) {
    val storeLine = RetailCaptureCopy.storeLine(model.title, model.location)
    var kind by remember { mutableStateOf(RetailCaptureCopy.modeChips("ISSUE").first()) }
    var severity by remember { mutableStateOf(RetailCaptureCopy.issueSeverities.first()) }
    var title by remember { mutableStateOf(RetailCaptureCopy.ISSUE_FIXTURE_TITLE) }
    var owner by remember { mutableStateOf(RetailCaptureCopy.ISSUE_FIXTURE_OWNER) }
    var link by remember { mutableStateOf(RetailCaptureCopy.ISSUE_FIXTURE_LINK) }
    var note by remember { mutableStateOf("") }
    var moreOpen by remember { mutableStateOf(false) }
    val postable = title.isNotBlank()
    val eventType = when (kind) {
        "Issue" -> "ISSUE"
        "Approval" -> "APPROVAL"
        else -> "UPDATE"
    }
    val cta = RetailCaptureCopy.issueCta(kind)
    val linkedTitle = title.ifBlank { RetailCaptureCopy.ISSUE_FIXTURE_TITLE }

    retailCaptureScaffold(
        title = RetailCaptureCopy.sheetTitle("ISSUE"),
        subtitle = RetailCaptureCopy.issueSubtitle(storeLine),
        onDismiss = onDismiss,
        why = RetailCaptureCopy.why("ISSUE"),
        cta = cta,
        enabled = postable,
        hideFooter = true,
    ) {
        RoseChipRow(
            label = null,
            options = RetailCaptureCopy.modeChips("ISSUE"),
            selected = kind,
        ) { kind = it }

        RoseSectionCard(
            label = "Linked",
            trailing = "Register · shift",
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column(
                    Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        RetailCaptureCopy.ISSUE_FIXTURE_CODE,
                        style = RetailRose.rowCaption(RetailRose.amber).copy(
                            fontSize = 10.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        ),
                        maxLines = 1,
                    )
                    Text(linkedTitle, style = RetailRose.rowTitle().copy(fontSize = 13.sp), maxLines = 2)
                    Text(
                        RetailCaptureCopy.ISSUE_FIXTURE_META,
                        style = RetailRose.rowCaption().copy(fontSize = 11.sp),
                        maxLines = 1,
                    )
                }
                Text(
                    severity,
                    style = RetailRose.rowValue(RetailRose.rose).copy(
                        fontSize = 13.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    ),
                    maxLines = 1,
                )
            }
        }

        RoseChipRow(
            label = "Severity",
            options = RetailCaptureCopy.issueSeverities,
            selected = severity,
        ) { severity = it }

        RosePickerRow("Title", title.ifBlank { RetailCaptureCopy.ISSUE_FIXTURE_TITLE }, onClick = null, externalLabel = true)
        RosePickerRow("Owner", owner.ifBlank { RetailCaptureCopy.ISSUE_FIXTURE_OWNER }, onClick = null, externalLabel = true)
        RosePickerRow("Link", link.ifBlank { RetailCaptureCopy.ISSUE_FIXTURE_LINK }, onClick = null, externalLabel = true)

        RoseMoreDetails(
            expanded = moreOpen,
            onToggle = { moreOpen = !moreOpen },
            hint = RetailCaptureCopy.ISSUE_MORE_HINT,
        ) {
            RoseFieldLabel("Title (edit)")
            RoseTextField(title, RetailCaptureCopy.ISSUE_FIXTURE_TITLE, onValueChange = { title = it.take(80) })
            RoseFieldLabel("Owner (edit)")
            RoseTextField(owner, RetailCaptureCopy.ISSUE_FIXTURE_OWNER, onValueChange = { owner = it })
            RoseFieldLabel("Link (edit)")
            RoseTextField(link, RetailCaptureCopy.ISSUE_FIXTURE_LINK, onValueChange = { link = it })
            RoseFieldLabel("Notes · Evidence")
            RoseTextField(note, "Notes · Evidence · Tags", onValueChange = { note = it }, singleLine = false)
            RosePickerRow("Evidence", "Add photo (chrome only)", onClick = null)
        }

        RoseStickyFooter(
            label = cta,
            enabled = postable,
            caveat = RetailCaptureCopy.DRAFT_FOOTER,
            onClick = {
                onSubmit(
                    RetailEventDraft(
                        eventType = eventType,
                        title = title.trim(),
                        amount = null,
                        note = listOfNotNull(
                            note.takeIf { it.isNotBlank() },
                            "Severity: $severity",
                            "Kind: $kind",
                            owner.takeIf { it.isNotBlank() }?.let { "Owner: $it" },
                            "Link: $link",
                            "Approval ≠ disbursement",
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
            RoseStickyFooter(label = cta, enabled = enabled, caveat = RetailCaptureCopy.DRAFT_FOOTER, onClick = {})
        }
        Spacer(Modifier.height(16.dp))
    }
}

private enum class AllocateKind { Receivable, Payable }

@Composable
private fun allocateAgainstRow(
    item: OperatingPaymentDto,
    currency: String,
    selected: Boolean,
    kind: AllocateKind,
    onClick: () -> Unit,
) {
    val code = allocateCode(item, kind)
    val headline = allocateHeadline(item, kind)
    val due = allocateDueLine(item, kind)
    val amountColor = when {
        kind == AllocateKind.Receivable -> RetailRose.negative
        else -> RetailRose.textPrimary
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) RetailRose.rose.copy(alpha = 0.16f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(code, style = RetailRose.rowCaption(RetailRose.amber).copy(fontSize = 10.sp), maxLines = 1)
            Text(headline, style = RetailRose.rowTitle().copy(fontSize = 13.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                due,
                style = RetailRose.rowCaption(
                    if (kind == AllocateKind.Receivable && due.equals("Due today", true)) {
                        RetailRose.negative
                    } else {
                        RetailRose.textBody
                    },
                ).copy(fontSize = 11.sp),
                maxLines = 1,
            )
        }
        Text(
            RetailFormat.exact(currency, item.amount),
            style = RetailRose.rowValue(amountColor).copy(fontSize = 13.sp),
        )
    }
}

@Composable
private fun paymentRow(
    item: OperatingPaymentDto,
    currency: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    allocateAgainstRow(
        item = item,
        currency = currency,
        selected = selected,
        kind = if (item.relevance.equals("RECEIVABLE", true)) AllocateKind.Receivable else AllocateKind.Payable,
        onClick = onClick,
    )
}

private fun allocateCode(item: OperatingPaymentDto, kind: AllocateKind): String {
    val fromTitle = item.title.substringBefore('·').substringBefore(' ').trim()
    if (fromTitle.isNotBlank() && fromTitle.any { it.isDigit() }) return fromTitle
    return when (kind) {
        AllocateKind.Receivable -> "INV-${item.id.takeLast(3).ifBlank { "441" }}"
        AllocateKind.Payable -> "PI-${item.id.takeLast(4).ifBlank { "8841" }}"
    }
}

private fun allocateHeadline(item: OperatingPaymentDto, kind: AllocateKind): String {
    val date = shortDate(item.occurredAt ?: item.dueAt)
    return when (kind) {
        AllocateKind.Receivable -> listOfNotNull("Credit sale", date).joinToString(" · ")
        AllocateKind.Payable -> listOfNotNull("Purchase", date).joinToString(" · ")
    }.ifBlank { item.title }
}

private fun allocateDueLine(item: OperatingPaymentDto, kind: AllocateKind): String {
    if (kind == AllocateKind.Payable) {
        return when {
            item.status.contains("RECEIV", ignoreCase = true) -> "Goods received"
            item.status.isNotBlank() -> item.status
            else -> "Goods received"
        }
    }
    val due = item.dueAt?.take(10)
    if (due != null) {
        runCatching {
            val dueDate = LocalDate.parse(due)
            val today = LocalDate.now()
            return when {
                dueDate.isEqual(today) -> "Due today"
                dueDate.isBefore(today) -> "Overdue"
                else -> "Due ${dueDate.format(DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH))}"
            }
        }
    }
    return item.dueBucket?.takeIf { it.isNotBlank() } ?: "Open AR"
}

private fun shortDate(raw: String?): String? {
    val day = raw?.take(10)?.takeIf { it.length >= 10 } ?: return null
    return runCatching {
        LocalDate.parse(day).format(DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH))
    }.getOrNull()
}

private fun retailMoneySavedToast() {
    MtoToastHost.show(
        MtoToastSpec(
            kind = MtoToastKind.Saved,
            title = RetailCaptureCopy.SAVED_TOAST,
            body = RetailCaptureCopy.TAGLINE,
            actionLabel = "Done",
            accent = RetailRose.positive,
        ),
    )
}

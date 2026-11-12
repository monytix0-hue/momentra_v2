package com.example.momentra.ui.shell.business.retail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.api.OperatingContractBody
import com.example.momentra.data.api.OperatingLineBody
import com.example.momentra.data.api.OperatingMasterDto
import com.example.momentra.data.api.OperatingProjectionDto
import com.example.momentra.ui.shell.business.shared.PosSettlement
import com.example.momentra.ui.theme.RetailRose
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.launch

private enum class RetailPosMode { ScanCart, HoldRecall, QuickAmount }

/**
 * POS Sale — Figma `2421:29655` Scan cart (default), `2438:29655` Hold recall, `2439:29655` Quick amount.
 *
 * Posts `SALE` via [onSubmit] / [RetailEventDraft]. Quick amount is amount-only (no lines / no stock).
 * Hold recall restores a session park into Scan cart — Charge still posts from Scan cart.
 */
@Composable
internal fun RetailPosSheet(
    model: OperatingProjectionDto,
    holdSession: RetailHoldSession,
    onDismiss: () -> Unit,
    onSubmit: suspend (RetailEventDraft) -> Unit,
    companyId: String? = null,
    momentId: String? = null,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currency = model.currency?.takeIf { it.isNotBlank() } ?: "INR"
    val skus = remember(model.masters) { retailCatalogMasters(model) }
    val channels = remember(model) {
        model.life?.channels.orEmpty().filter { it.isNotBlank() }
            .ifEmpty { listOf("Walk-in", "Counter", "Online") }
    }
    val registerContext = remember(model) { retailPosRegisterContext(model) }

    var posMode by remember { mutableStateOf(RetailPosMode.ScanCart) }
    var channel by remember { mutableStateOf(channels.first()) }
    var tender by remember { mutableStateOf("UPI") }
    var splitTender by remember { mutableStateOf(false) }
    var splitUpi by remember { mutableStateOf("") }
    var splitCash by remember { mutableStateOf("") }
    var splitCard by remember { mutableStateOf("") }
    var discount by remember { mutableStateOf("") }
    var cessAmount by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }
    var holdQuery by remember { mutableStateOf("") }
    var customer by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var quickAmount by remember { mutableStateOf("") }
    var selectedHoldId by remember { mutableStateOf<String?>(null) }
    var gstPicker by remember { mutableStateOf("Inclusive 5%") }
    var billPrint by remember { mutableStateOf("Thermal · share UPI QR") }
    val cart = remember { mutableStateListOf<RetailPosLine>() }
    var detailsOpen by remember { mutableStateOf(false) }
    var showSkuPicker by remember { mutableStateOf(false) }
    var showScanner by remember { mutableStateOf(false) }
    var posting by remember { mutableStateOf(false) }
    var postError by remember { mutableStateOf<String?>(null) }
    var postedReceipt by remember { mutableStateOf<RetailReceiptModel?>(null) }
    var paper by remember { mutableStateOf(RetailReceiptPaperSize.Default) }
    var shareFormat by remember { mutableStateOf(RetailReceiptShareFormat.JPG) }
    var logoTick by remember { mutableStateOf(0) }
    val logo = remember(companyId, momentId, logoTick) {
        RetailReceiptLogoStore.loadBitmap(context, companyId, momentId)
    }

    val holds = holdSession.holds
    val filteredHolds = remember(holds, holdQuery) {
        val q = holdQuery.trim()
        if (q.isEmpty()) holds
        else holds.filter {
            it.label.contains(q, true) ||
                it.holdCode.contains(q, true) ||
                it.id.contains(q, true) ||
                it.note.orEmpty().contains(q, true)
        }
    }
    val recalled = holds.firstOrNull { it.id == selectedHoldId }

    val cartUnits = cart.sumOf { it.quantity }
    val subtotal = when (posMode) {
        RetailPosMode.QuickAmount -> quickAmount.toBigDecimalOrNull() ?: BigDecimal.ZERO
        else -> cart.fold(BigDecimal.ZERO) { acc, line -> acc + line.amount() }
    }
    val discountBd = PosSettlement.parseAmount(discount)
    val cessBd = PosSettlement.parseAmount(cessAmount)
    val discountOk = discountBd <= subtotal || subtotal.signum() <= 0
    val payable = PosSettlement.payable(subtotal, discountBd, cessBd)
    val splitAmounts = mapOf("UPI" to splitUpi, "Cash" to splitCash, "Card" to splitCard)
    val paymentsPreview = PosSettlement.buildPayments(
        split = splitTender,
        primary = tender,
        payable = payable,
        splitAmounts = splitAmounts,
    )
    val splitOk = !splitTender || PosSettlement.splitSumsToPayable(splitAmounts, payable)
    val subtotalText = if (subtotal.signum() <= 0) {
        "—"
    } else {
        RetailFormat.exact(currency, subtotal.stripTrailingZeros().toPlainString())
    }
    val payableText = if (payable.signum() <= 0) {
        "—"
    } else {
        RetailFormat.exact(currency, payable.stripTrailingZeros().toPlainString())
    }
    val discountText = if (discountBd.signum() <= 0) {
        "—"
    } else {
        "−${RetailFormat.exact(currency, discountBd.stripTrailingZeros().toPlainString())}"
    }
    val tenderLabel = PosSettlement.tenderLabel(splitTender, tender, paymentsPreview)
    val pricedLines = cart.filter { it.unitPrice.toBigDecimalOrNull()?.signum() == 1 }
    val postable = when (posMode) {
        RetailPosMode.QuickAmount ->
            quickAmount.toBigDecimalOrNull()?.signum() == 1 && discountOk && splitOk && payable.signum() == 1
        RetailPosMode.HoldRecall -> false
        RetailPosMode.ScanCart ->
            cart.isNotEmpty() && pricedLines.size == cart.size && discountOk && splitOk && payable.signum() == 1
    }
    val recallReady = posMode == RetailPosMode.HoldRecall && recalled != null
    val scanGstEstimate = remember(cart.toList()) {
        RetailFormat.gstEstimate(
            lineAmounts = cart.map { it.amount() },
            lineRates = cart.map { it.gstRate },
            inclusive = true,
        )
    }
    val quickGstRate = remember(gstPicker) {
        Regex("""(\d+(?:\.\d+)?)\s*%""").find(gstPicker)?.groupValues?.getOrNull(1)
    }
    val quickGstEstimate = remember(quickAmount, gstPicker) {
        if (!gstPicker.startsWith("Inclusive")) null
        else RetailFormat.gstEstimateOnTotal(
            amount = quickAmount.toBigDecimalOrNull(),
            ratePercent = quickGstRate,
            inclusive = true,
        )
    }
    val channelDisplay = remember(channel) {
        if (channel.contains("Register", ignoreCase = true) || channel.contains("In-store", ignoreCase = true)) {
            channel
        } else {
            "In-store · Register 2"
        }
    }

    fun applyHoldToCart(hold: RetailParkedCart) {
        selectedHoldId = hold.id
        cart.clear()
        cart.addAll(hold.lines)
        if (customer.isBlank()) customer = hold.label
        hold.note?.takeIf { note.isBlank() }?.let { note = it }
        hold.channelName?.takeIf { channels.contains(it) }?.let { channel = it }
    }

    fun recallSelectedToScanCart() {
        val hold = recalled ?: return
        applyHoldToCart(hold)
        posMode = RetailPosMode.ScanCart
    }

    fun postSale() {
        if (posting || !postable) return
        val payablePlain = PosSettlement.money(payable)
        val discountPlain = discountBd.takeIf { it.signum() > 0 }?.let { PosSettlement.money(it) }
        val cessPlain = cessBd.takeIf { it.signum() > 0 }?.let { PosSettlement.money(it) }
        val payments = PosSettlement.buildPayments(
            split = splitTender,
            primary = tender,
            payable = payable,
            splitAmounts = splitAmounts,
        )
        val noteParts = listOfNotNull(note.takeIf { it.isNotBlank() })
        val wantsThermal = billPrint.startsWith("Thermal")
        val draft: RetailEventDraft
        val receiptSnapshot: RetailReceiptModel
        when (posMode) {
            RetailPosMode.QuickAmount -> {
                val title = customer.takeIf { it.isNotBlank() }?.let { "Sale · $it" } ?: "Quick amount sale"
                val gst = retailPosGstContract(
                    model = model,
                    taxModeOverride = when {
                        gstPicker.startsWith("Inclusive", ignoreCase = true) -> "INCLUSIVE"
                        gstPicker.startsWith("Exclusive", ignoreCase = true) -> "EXCLUSIVE"
                        else -> null
                    },
                    gstRateOverride = quickGstRate,
                )
                draft = RetailEventDraft(
                    eventType = "SALE",
                    title = title,
                    amount = quickAmount,
                    note = noteParts.joinToString(" · ").ifBlank { null },
                    channelName = channel,
                    counterpartyName = customer.takeIf { it.isNotBlank() },
                    paymentStatus = "COLLECTED",
                    contract = OperatingContractBody(
                        entryMode = "QUICK",
                        paymentState = "RECEIVED",
                        receivedAmount = payablePlain,
                        discount = discountPlain,
                        cessAmount = cessPlain,
                        payments = payments,
                        taxMode = gst.taxMode,
                        gstRate = gst.gstRate,
                        placeOfSupply = gst.placeOfSupply,
                    ),
                    lines = null,
                )
                receiptSnapshot = buildRetailReceiptFromPosCart(
                    model = model,
                    title = title,
                    lines = emptyList(),
                    totalPlain = payablePlain,
                    tender = tenderLabel,
                    customer = customer.takeIf { it.isNotBlank() },
                    quickAmountOnly = true,
                    gstEstimatePlain = quickGstEstimate?.stripTrailingZeros()?.toPlainString(),
                )
            }
            RetailPosMode.ScanCart -> {
                val gst = retailPosGstContract(model)
                val lines = cart.map {
                    OperatingLineBody(
                        itemName = it.name,
                        quantity = it.quantity.toString(),
                        unit = it.unit,
                        unitPrice = it.unitPrice,
                        masterId = productionMasterId(it.masterId),
                        taxMode = gst.taxMode.takeIf { mode -> mode != "NOT_CONFIGURED" },
                        gstRate = it.gstRate?.takeIf { rate ->
                            rate.isNotBlank() && !rate.equals("Exempt", true) && rate != "0"
                        },
                    )
                }
                val title = when {
                    customer.isNotBlank() -> "Sale · ${customer.trim()}"
                    recalled != null -> "Sale · ${recalled.label}"
                    else -> "Sale · ${cart.size} line${if (cart.size == 1) "" else "s"}"
                }
                val grossPlain = subtotal.stripTrailingZeros().toPlainString()
                draft = RetailEventDraft(
                    eventType = "SALE",
                    title = title,
                    amount = grossPlain,
                    note = noteParts.joinToString(" · ").ifBlank { null },
                    channelName = channel,
                    counterpartyName = customer.takeIf { it.isNotBlank() },
                    paymentStatus = "COLLECTED",
                    contract = OperatingContractBody(
                        entryMode = "ITEMIZED",
                        paymentState = "RECEIVED",
                        receivedAmount = payablePlain,
                        discount = discountPlain,
                        cessAmount = cessPlain,
                        payments = payments,
                        taxMode = gst.taxMode,
                        placeOfSupply = gst.placeOfSupply,
                    ),
                    lines = lines,
                )
                receiptSnapshot = buildRetailReceiptFromPosCart(
                    model = model,
                    title = title,
                    lines = cart.toList(),
                    totalPlain = payablePlain,
                    tender = tenderLabel,
                    customer = customer.takeIf { it.isNotBlank() },
                    quickAmountOnly = false,
                    gstEstimatePlain = scanGstEstimate?.stripTrailingZeros()?.toPlainString(),
                )
            }
            RetailPosMode.HoldRecall -> return
        }
        scope.launch {
            posting = true
            postError = null
            val result = runCatching { onSubmit(draft) }
            posting = false
            result.onFailure { postError = it.message ?: "Post failed" }
            result.onSuccess {
                if (posMode == RetailPosMode.ScanCart) {
                    selectedHoldId?.let { id -> holdSession.remove(id) }
                }
                if (wantsThermal && posMode == RetailPosMode.ScanCart) {
                    postedReceipt = receiptSnapshot
                } else {
                    onDismiss()
                }
            }
        }
    }

    fun addMatchedSku(match: OperatingMasterDto) {
        val existing = cart.indexOfFirst { it.masterId == match.id }
        if (existing >= 0) {
            val line = cart[existing]
            cart[existing] = line.copy(quantity = (line.quantity + 1).coerceAtMost(99))
        } else {
            cart += RetailPosLine.of(match)
        }
        query = ""
        showSkuPicker = false
    }

    fun applyPosScan(raw: String) {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return
        val match = skus.firstOrNull {
            it.barcode.equals(trimmed, true) ||
                it.code.equals(trimmed, true) ||
                it.name.contains(trimmed, true)
        }
        if (match != null) addMatchedSku(match)
        else query = trimmed
    }

    if (postedReceipt != null) {
        val receipt = postedReceipt!!
        RetailSheetBody(Modifier.verticalScroll(rememberScrollState())) {
            RoseGrabber()
            RoseSheetHeader(
                title = "Sale posted",
                subtitle = "Customer copy · share or print",
                onClose = onDismiss,
            )
            RetailReceiptPreview(receipt, paper = paper, logo = logo)
            Text(
                "Charge already posted. This slip is a client render — no second SALE, no fiscal bill allocation.",
                style = RetailRose.rowCaption(RetailRose.textFaint),
            )
            RetailReceiptShareControls(
                model = receipt,
                companyId = companyId,
                momentId = momentId,
                paper = paper,
                onPaperChange = { paper = it },
                format = shareFormat,
                onFormatChange = { shareFormat = it },
                logo = logo,
                onLogoChanged = { logoTick += 1 },
            )
            RoseStickyFooter(
                label = "Done",
                enabled = true,
                caveat = "Receipt is client copy until receipt.pdf API exists",
                onClick = onDismiss,
            )
            Spacer(Modifier.height(16.dp))
        }
        return
    }

    RetailSheetBody(Modifier.verticalScroll(rememberScrollState())) {
        RoseGrabber()
        RoseSheetHeader(
            title = "POS Sale",
            subtitle = registerContext,
            onClose = onDismiss,
            // Figma ALT only on Hold recall / Quick amount — Scan cart has no badge.
            showAltBadge = posMode != RetailPosMode.ScanCart,
        )

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            RoseModeChip("Scan cart", selected = posMode == RetailPosMode.ScanCart, onClick = {
                posMode = RetailPosMode.ScanCart
            })
            RoseModeChip("Hold recall", selected = posMode == RetailPosMode.HoldRecall, onClick = {
                posMode = RetailPosMode.HoldRecall
            })
            RoseModeChip("Quick amount", selected = posMode == RetailPosMode.QuickAmount, onClick = {
                posMode = RetailPosMode.QuickAmount
            })
        }

        when (posMode) {
            RetailPosMode.ScanCart -> {
                RoseFieldLabel("Scan or search SKU")
                RoseScanField(
                    query = query,
                    placeholder = if (skus.isEmpty()) {
                        "No SKU — add under Catalogue"
                    } else {
                        skus.firstOrNull()?.code?.takeIf { it.isNotBlank() }?.let { "$it  ·  tap to scan" }
                            ?: "Barcode or product name  ·  tap to scan"
                    },
                    onQueryChange = { query = it },
                    onScan = {
                        val trimmed = query.trim()
                        if (trimmed.isBlank()) {
                            if (skus.isNotEmpty()) showScanner = true
                            return@RoseScanField
                        }
                        applyPosScan(trimmed)
                    },
                )
                retailSkuQuickPick(skus, query, currency) { sku ->
                    val existing = cart.indexOfFirst { it.masterId == sku.id }
                    if (existing >= 0) {
                        val line = cart[existing]
                        cart[existing] = line.copy(quantity = (line.quantity + 1).coerceAtMost(99))
                    } else {
                        cart += RetailPosLine.of(sku)
                    }
                    query = ""
                    showSkuPicker = false
                }

                RoseLinesCard("Cart", cart.size, units = cartUnits) {
                    retailDenseCartLines(
                        cart = cart,
                        currency = currency,
                        onUpdate = { index, line -> cart[index] = line },
                        onRemove = { index -> cart.removeAt(index) },
                    )
                    Row(
                        Modifier.fillMaxWidth().padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "+ Add line",
                            style = RetailRose.viewAll().copy(fontSize = 13.sp),
                            modifier = Modifier.clickable { showSkuPicker = !showSkuPicker },
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            "Recent · Favourites",
                            style = RetailRose.rowCaption(),
                            modifier = Modifier.clickable { showSkuPicker = !showSkuPicker },
                        )
                    }
                    if (showSkuPicker) {
                        val picks = skus.filter { master -> cart.none { it.masterId == master.id } }.take(6)
                        if (picks.isEmpty()) {
                            Text("No more catalogue SKUs to add.", style = RetailRose.rowCaption())
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                picks.forEach { sku ->
                                    RosePickerRow(
                                        label = sku.code?.takeIf { it.isNotBlank() } ?: "SKU",
                                        value = listOfNotNull(
                                            sku.name,
                                            sku.sellPrice?.takeIf { it.isNotBlank() }?.let {
                                                RetailFormat.exact(currency, it)
                                            },
                                        ).joinToString(" · "),
                                        onClick = {
                                            cart += RetailPosLine.of(sku)
                                            showSkuPicker = false
                                        },
                                    )
                                }
                            }
                        }
                    }
                }

                RoseTotalsCard("Payable now", payableText, tender = tenderLabel) {
                    RoseTotalRow("Subtotal", subtotalText)
                    if (discountBd.signum() > 0) {
                        RoseTotalRow("Discount", discountText, valueColor = RetailRose.positive)
                    }
                    if (cessBd.signum() > 0) {
                        RoseTotalRow(
                            "Cess",
                            RetailFormat.exact(currency, cessBd.stripTrailingZeros().toPlainString()),
                        )
                    }
                    RoseGstEstimateRow(
                        label = "CGST + SGST (incl. est.)",
                        estimate = scanGstEstimate,
                        currency = currency,
                        captionWhenKnown = "Estimate from line rates — GST posts only on Charge",
                        captionWhenMissing = "Unavailable — rate missing on line / master",
                    )
                }

                RoseFieldLabel("Discount")
                RoseTextField(
                    discount,
                    "0",
                    onValueChange = { discount = it.filter { ch -> ch.isDigit() || ch == '.' }.take(12) },
                )
                RetailPosTenderBlock(
                    tender = tender,
                    splitTender = splitTender,
                    splitUpi = splitUpi,
                    splitCash = splitCash,
                    splitCard = splitCard,
                    splitOk = splitOk,
                    onTender = { tender = it },
                    onSplit = { splitTender = it },
                    onSplitUpi = { splitUpi = it },
                    onSplitCash = { splitCash = it },
                    onSplitCard = { splitCard = it },
                )

                RosePickerRow(
                    label = "Customer",
                    value = customer.ifBlank { "Walk-in · B2C · MH intra-state" },
                    externalLabel = true,
                    onClick = { detailsOpen = true },
                )
                RosePickerRow(
                    label = "Bill / print",
                    value = billPrint,
                    externalLabel = true,
                    onClick = {
                        billPrint = if (billPrint.startsWith("Thermal")) {
                            "A4 invoice · email"
                        } else {
                            "Thermal · share UPI QR"
                        }
                    },
                )

                RoseMoreDetails(
                    detailsOpen,
                    { detailsOpen = !detailsOpen },
                    hint = "Notes · Loyalty · Hold bill",
                ) {
                    RoseFieldLabel("Customer name")
                    RoseTextField(customer, "Walk-in or credit buyer", onValueChange = { customer = it })
                    RoseFieldLabel("Sales channel")
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        channels.take(3).forEach { ch ->
                            RoseModeChip(ch, selected = ch == channel, onClick = { channel = ch })
                        }
                    }
                    RoseFieldLabel("Note")
                    RoseTextField(note, "Notes · Loyalty · Hold bill", onValueChange = { note = it }, singleLine = false)
                    RoseFieldLabel("Cess ₹ (optional · explicit only)")
                    RoseTextField(
                        cessAmount,
                        "0",
                        onValueChange = { cessAmount = it.filter { ch -> ch.isDigit() || ch == '.' }.take(12) },
                    )
                    Text(
                        "Cess is operator-entered only — no HSN/CBIC schedule lookup.",
                        style = RetailRose.rowCaption(RetailRose.textFaint).copy(fontSize = 10.sp),
                    )
                    if (cart.isNotEmpty()) {
                        val peek = holdSession.peekHoldCode()
                        RoseModeChip(
                            "Park bill · $peek",
                            selected = false,
                            onClick = {
                                holdSession.park(
                                    label = customer.trim().ifBlank { "Walk-in · $channel" },
                                    lines = cart.toList(),
                                    note = note.takeIf { it.isNotBlank() },
                                    channelName = channel,
                                    summary = "${cart.size} line${if (cart.size == 1) "" else "s"} · $channel",
                                )
                                cart.clear()
                                selectedHoldId = null
                                posMode = RetailPosMode.HoldRecall
                            },
                        )
                        Text(
                            "Parks device-local draft — no revenue, no stock. Survives restart on this phone. Recall later under Hold recall.",
                            style = RetailRose.rowCaption(RetailRose.textFaint),
                        )
                    }
                }

                RoseWhyBox(
                    "POS posts one itemized sale + linked tender receipt atomically. Stock − on post when tracked. Hold is draft only — no revenue, no stock. Quick amount stays the original sheet.",
                    tinted = true,
                )

                if (postError != null) {
                    Text(postError ?: "", style = RetailRose.rowCaption(RetailRose.negative))
                }
                RoseStickyFooter(
                    label = when {
                        posting -> "Posting…"
                        postable -> "Charge $payableText · Post & print"
                        else -> "Charge · Post & print"
                    },
                    enabled = postable && !posting,
                    caveat = "Idempotent post · then thermal receipt preview",
                    onClick = { postSale() },
                )
            }

            RetailPosMode.HoldRecall -> {
                RoseFieldLabel("Find parked bill")
                RoseTextField(
                    holdQuery,
                    "Name · table · bill #",
                    onValueChange = { holdQuery = it.take(40) },
                )

                RoseSectionCard(
                    label = "Parked on this register",
                    trailing = if (holds.isEmpty()) {
                        "No holds · draft only"
                    } else {
                        "${holds.size} hold${if (holds.size == 1) "" else "s"} · draft only"
                    },
                ) {
                    if (filteredHolds.isEmpty()) {
                        RoseEmpty(
                            if (holds.isEmpty()) "No parked bills" else "No matches",
                            if (holds.isEmpty()) {
                                "Park from Scan cart → More details · Hold bill, or Utilities · Hold. Parks are device-local until a register API exists."
                            } else {
                                "Try another name or bill #."
                            },
                        )
                    } else {
                        filteredHolds.forEach { hold ->
                            val selected = hold.id == selectedHoldId
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(RetailRose.cardSlate)
                                    .border(
                                        1.dp,
                                        if (selected) RetailRose.rose else RetailRose.rose.copy(alpha = 0.3f),
                                        RoundedCornerShape(12.dp),
                                    )
                                    .clickable {
                                        selectedHoldId = if (selected) null else hold.id
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        hold.holdCode,
                                        style = RetailRose.rowCaption(RetailRose.amber).copy(
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                        ),
                                    )
                                    Text(
                                        hold.label,
                                        style = RetailRose.rowTitle().copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        hold.summary ?: listOf(
                                            "${hold.lines.size} line${if (hold.lines.size == 1) "" else "s"}",
                                            hold.channelName ?: "Session",
                                        ).joinToString(" · "),
                                        style = RetailRose.rowCaption(),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        RetailFormat.exact(
                                            currency,
                                            hold.total().stripTrailingZeros().toPlainString(),
                                        ),
                                        style = RetailRose.rowValue().copy(fontSize = 14.sp),
                                    )
                                    Text(hold.ageLabel(), style = RetailRose.rowCaption().copy(fontSize = 10.sp))
                                }
                            }
                        }
                        Text(
                            "Empty holds auto-expire end of shift",
                            style = RetailRose.rowCaption(),
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }

                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RetailRose.rCardTight)
                        .background(RetailRose.cardSlate)
                        .border(1.5.dp, RetailRose.rose, RetailRose.rCardTight)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Selected", style = RetailRose.rowCaption().copy(fontSize = 11.sp))
                        Spacer(Modifier.weight(1f))
                        Text(
                            recalled?.holdCode ?: "—",
                            style = RetailRose.rowCaption(RetailRose.amber).copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                    }
                    Text(
                        recalled?.let {
                            RetailFormat.exact(currency, it.total().stripTrailingZeros().toPlainString())
                        } ?: "—",
                        style = RetailRose.metricValue().copy(fontSize = 28.sp),
                        maxLines = 1,
                    )
                    Text(
                        "Tap Recall to refill Scan cart — still draft until Charge",
                        style = RetailRose.rowCaption().copy(fontSize = 11.sp),
                    )
                }

                RoseMoreDetails(
                    detailsOpen,
                    { detailsOpen = !detailsOpen },
                    hint = "Void hold · Merge · Transfer register",
                ) {
                    Text(
                        "Void hold · Merge · Transfer register — chrome only on this sheet.",
                        style = RetailRose.rowCaption(),
                    )
                    RoseFieldLabel("Note")
                    RoseTextField(note, "Recall note", onValueChange = { note = it }, singleLine = false)
                }

                RoseWhyBox(
                    "Hold is a device-local draft cart — no revenue, no stock, no tender. Survives app restart on this phone until recall/charge. Not shared across devices until a register API exists. Recall opens Scan cart; Charge clears the hold.",
                    tinted = true,
                )

                RoseStickyFooter(
                    label = if (recallReady) {
                        "Recall ${recalled!!.holdCode} → Scan cart"
                    } else {
                        "Recall → Scan cart"
                    },
                    enabled = recallReady,
                    caveat = "Does not post · Charge still required on Scan cart",
                    onClick = { recallSelectedToScanCart() },
                )
            }

            RetailPosMode.QuickAmount -> {
                RoseAmountHero(
                    label = "Sale amount",
                    amount = if (quickAmount.isBlank()) "—" else RetailFormat.exact(currency, quickAmount),
                    caption = "No SKU lines · revenue only · stock unchanged",
                    currencyBadge = currency,
                )

                RoseSectionCard(label = "Keypad", trailing = "tap to enter") {
                    RoseNumericKeypad(value = quickAmount, onValueChange = { quickAmount = it })
                }

                RoseTotalsCard("Payable now", payableText, tender = tenderLabel) {
                    RoseTotalRow("Subtotal", subtotalText)
                    if (discountBd.signum() > 0) {
                        RoseTotalRow("Discount", discountText, valueColor = RetailRose.positive)
                    }
                    if (cessBd.signum() > 0) {
                        RoseTotalRow(
                            "Cess",
                            RetailFormat.exact(currency, cessBd.stripTrailingZeros().toPlainString()),
                        )
                    }
                    RoseGstEstimateRow(
                        label = if (gstPicker.startsWith("Inclusive")) "GST (inclusive est.)" else "GST (exclusive · selection)",
                        estimate = quickGstEstimate,
                        currency = currency,
                        captionWhenKnown = "Estimate from keypad + rate — GST posts only on Charge",
                        captionWhenMissing = "Unavailable — pick Inclusive rate or wait for tax config on post",
                    )
                }

                RoseFieldLabel("Discount")
                RoseTextField(
                    discount,
                    "0",
                    onValueChange = { discount = it.filter { ch -> ch.isDigit() || ch == '.' }.take(12) },
                )
                RetailPosTenderBlock(
                    tender = tender,
                    splitTender = splitTender,
                    splitUpi = splitUpi,
                    splitCash = splitCash,
                    splitCard = splitCard,
                    splitOk = splitOk,
                    onTender = { tender = it },
                    onSplit = { splitTender = it },
                    onSplitUpi = { splitUpi = it },
                    onSplitCash = { splitCash = it },
                    onSplitCard = { splitCard = it },
                )

                RosePickerRow(
                    label = "Customer",
                    value = customer.ifBlank { "Walk-in (cash/UPI)" },
                    externalLabel = true,
                    onClick = { detailsOpen = true },
                )
                RosePickerRow(
                    label = "Channel",
                    value = channelDisplay,
                    externalLabel = true,
                    onClick = {
                        val idx = channels.indexOf(channel)
                        channel = channels[(idx + 1).coerceAtLeast(0) % channels.size]
                    },
                )
                RosePickerRow(
                    label = "GST",
                    value = gstPicker,
                    externalLabel = true,
                    onClick = {
                        gstPicker = if (gstPicker.startsWith("Inclusive")) {
                            "Exclusive · selection only"
                        } else {
                            "Inclusive 5%"
                        }
                    },
                )

                RoseMoreDetails(
                    detailsOpen,
                    { detailsOpen = !detailsOpen },
                    hint = "Notes · Attachment · Tags",
                ) {
                    RoseFieldLabel("Customer name")
                    RoseTextField(customer, "Walk-in or named credit buyer", onValueChange = { customer = it })
                    RoseFieldLabel("Note")
                    RoseTextField(note, "Notes · Attachment · Tags", onValueChange = { note = it }, singleLine = false)
                    RoseFieldLabel("Cess ₹ (optional · explicit only)")
                    RoseTextField(
                        cessAmount,
                        "0",
                        onValueChange = { cessAmount = it.filter { ch -> ch.isDigit() || ch == '.' }.take(12) },
                    )
                    Text(
                        "Cess is operator-entered only — no HSN/CBIC schedule lookup.",
                        style = RetailRose.rowCaption(RetailRose.textFaint).copy(fontSize = 10.sp),
                    )
                }

                RoseWhyBox(
                    "Quick amount posts revenue + linked tender with zero SKU movement. Same rule as original QA-01 — kept inside POS so the counter never leaves this sheet. Credit still needs a named customer.",
                    tinted = true,
                )

                if (postError != null) {
                    Text(postError ?: "", style = RetailRose.rowCaption(RetailRose.negative))
                }
                RoseStickyFooter(
                    label = when {
                        posting -> "Posting…"
                        postable -> "Charge $payableText · Post"
                        else -> "Charge · Post"
                    },
                    enabled = postable && !posting,
                    caveat = "No stock move · draft kept on mode switch",
                    onClick = { postSale() },
                )
            }
        }

        Spacer(Modifier.height(16.dp))
    }

    if (showScanner) {
        RetailBarcodeScanner(
            title = "Scan SKU",
            subtitle = "Point at a product barcode or QR",
            onCode = { code ->
                showScanner = false
                applyPosScan(code)
            },
            onDismiss = { showScanner = false },
        )
    }
}

@Composable
private fun retailDenseCartLines(
    cart: List<RetailPosLine>,
    currency: String,
    onUpdate: (Int, RetailPosLine) -> Unit,
    onRemove: (Int) -> Unit,
) {
    if (cart.isEmpty()) {
        RoseEmpty("Nothing on the ticket", "Scan or search a catalogue SKU.")
        return
    }
    Column(Modifier.fillMaxWidth()) {
        cart.forEachIndexed { index, line ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    line.skuCode?.takeIf { it.isNotBlank() }?.let {
                        Text(
                            it,
                            style = RetailRose.rowCaption(RetailRose.amber).copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            maxLines = 1,
                        )
                    }
                    Text(line.name, style = RetailRose.rowTitle(), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        buildString {
                            append(
                                if (line.unitPrice.isBlank()) {
                                    "Price needed"
                                } else {
                                    "${RetailFormat.exact(currency, line.unitPrice)} / ${line.unit}"
                                },
                            )
                            line.hsn?.takeIf { it.isNotBlank() }?.let { append(" · HSN $it") }
                        },
                        style = RetailRose.rowCaption(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                RoseStepper(
                    line.quantity,
                    onDecrement = {
                        if (line.quantity <= 1) onRemove(index)
                        else onUpdate(index, line.copy(quantity = line.quantity - 1))
                    },
                    onIncrement = {
                        if (line.quantity < 99) onUpdate(index, line.copy(quantity = line.quantity + 1))
                    },
                )
                Text(
                    RetailFormat.exact(currency, line.amount().stripTrailingZeros().toPlainString()),
                    style = RetailRose.rowValue().copy(fontSize = 13.sp),
                )
            }
            if (index < cart.lastIndex) {
                Spacer(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(RetailRose.divider),
                )
            }
        }
    }
}

private fun RetailParkedCart.ageLabel(): String {
    val mins = TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - createdAt).coerceAtLeast(0)
    return when {
        mins < 1 -> "just now"
        mins < 60 -> "${mins}m ago"
        else -> "${mins / 60}h ago"
    }
}

/** Location-derived register strip — never invents shift/register IDs as posted facts. */
internal fun retailPosRegisterContext(model: OperatingProjectionDto): String {
    val store = model.title?.takeIf { it.isNotBlank() } ?: "Harbor Mart"
    val location = model.location?.takeIf { it.isNotBlank() }
        ?: model.life?.channels?.firstOrNull { it.contains("Andheri", true) }
        ?: "Andheri"
    return "Register 2 · Shift open · $location · $store"
}

data class RetailPosLine(
    val masterId: String,
    val name: String,
    val unit: String,
    val unitPrice: String,
    val quantity: Int,
    val skuCode: String? = null,
    val hsn: String? = null,
    /** Master GST % for client estimate rows — never posted as fabricated tax. */
    val gstRate: String? = null,
) {
    fun amount(): BigDecimal = (unitPrice.toBigDecimalOrNull() ?: BigDecimal.ZERO)
        .multiply(BigDecimal(quantity))
        .setScale(2, RoundingMode.HALF_UP)

    companion object {
        fun of(master: OperatingMasterDto) = RetailPosLine(
            masterId = master.id,
            name = master.name,
            unit = master.unit?.takeIf { it.isNotBlank() } ?: "pcs",
            unitPrice = master.sellPrice?.takeIf { it.isNotBlank() }.orEmpty(),
            quantity = 1,
            skuCode = master.code?.takeIf { it.isNotBlank() } ?: master.barcode?.takeIf { it.isNotBlank() },
            hsn = master.hsn?.takeIf { it.isNotBlank() },
            gstRate = master.gstRate?.takeIf { it.isNotBlank() },
        )
    }
}

@Composable
private fun RetailPosTenderBlock(
    tender: String,
    splitTender: Boolean,
    splitUpi: String,
    splitCash: String,
    splitCard: String,
    splitOk: Boolean,
    onTender: (String) -> Unit,
    onSplit: (Boolean) -> Unit,
    onSplitUpi: (String) -> Unit,
    onSplitCash: (String) -> Unit,
    onSplitCard: (String) -> Unit,
) {
    RoseFieldLabel("Tender")
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        PosSettlement.tenderMethods.forEach { option ->
            RoseModeChip(
                option,
                selected = !splitTender && option == tender,
                tender = true,
                onClick = {
                    onSplit(false)
                    onTender(option)
                },
            )
        }
        RoseModeChip(
            "Split",
            selected = splitTender,
            tender = true,
            onClick = { onSplit(true) },
        )
    }
    if (splitTender) {
        RoseFieldLabel("UPI")
        RoseTextField(splitUpi, "0", onValueChange = { onSplitUpi(it.filter { ch -> ch.isDigit() || ch == '.' }.take(12)) })
        RoseFieldLabel("Cash")
        RoseTextField(splitCash, "0", onValueChange = { onSplitCash(it.filter { ch -> ch.isDigit() || ch == '.' }.take(12)) })
        RoseFieldLabel("Card")
        RoseTextField(splitCard, "0", onValueChange = { onSplitCard(it.filter { ch -> ch.isDigit() || ch == '.' }.take(12)) })
        Text(
            if (splitOk) {
                "Split instruments post on the contract and must add up to payable."
            } else {
                "Split amounts must add up to payable before Charge."
            },
            style = RetailRose.rowCaption(
                if (splitOk) RetailRose.textFaint else RetailRose.negative,
            ).copy(fontSize = 10.sp),
        )
    } else {
        Text(
            "One instrument posts as contract.payments · use Split for UPI+Cash+Card.",
            style = RetailRose.rowCaption(RetailRose.textFaint).copy(fontSize = 10.sp),
        )
    }
}

/** Harbor fixture ids (`fx-*`) must never ride a production Charge. */
internal fun productionMasterId(masterId: String?): String? {
    val id = masterId?.takeIf { it.isNotBlank() } ?: return null
    return if (id.startsWith("fx-", ignoreCase = true)) null else id
}

internal data class RetailPosGstContract(
    val taxMode: String,
    val gstRate: String? = null,
    val placeOfSupply: String? = null,
)

/**
 * When gstTracking=YES + REGULAR, send inclusive/exclusive (+ optional rate) so the server
 * stamps GST via masterRefs. Otherwise keep honesty `NOT_CONFIGURED`.
 */
internal fun retailPosGstContract(
    model: OperatingProjectionDto,
    taxModeOverride: String? = null,
    gstRateOverride: String? = null,
): RetailPosGstContract {
    val gstOn = model.gstTracking.equals("YES", true) &&
        model.gstRegistration.equals("REGULAR", true)
    if (!gstOn) return RetailPosGstContract(taxMode = "NOT_CONFIGURED")
    val taxMode = taxModeOverride
        ?: when {
            model.salePriceIncludesGst.equals("YES", true) -> "INCLUSIVE"
            model.salePriceIncludesGst.equals("NO", true) -> "EXCLUSIVE"
            else -> "INCLUSIVE"
        }
    return RetailPosGstContract(
        taxMode = taxMode,
        gstRate = gstRateOverride?.takeIf { it.isNotBlank() },
        placeOfSupply = model.gstState?.takeIf { it.isNotBlank() },
    )
}

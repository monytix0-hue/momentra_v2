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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.api.OperatingContractBody
import com.example.momentra.data.api.OperatingLineBody
import com.example.momentra.data.api.OperatingMasterDto
import com.example.momentra.data.api.OperatingProjectionDto
import com.example.momentra.ui.theme.RetailRose
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.concurrent.TimeUnit

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
    onSubmit: (RetailEventDraft) -> Unit,
) {
    val currency = model.currency?.takeIf { it.isNotBlank() } ?: "INR"
    val skus = remember(model.masters) { retailCatalogMasters(model) }
    val channels = remember(model) {
        model.life?.channels.orEmpty().filter { it.isNotBlank() }
            .ifEmpty { listOf("Walk-in", "Counter", "Online") }
    }
    val registerLabel = model.title?.takeIf { it.isNotBlank() } ?: "Session"

    var posMode by remember { mutableStateOf(RetailPosMode.ScanCart) }
    var channel by remember { mutableStateOf(channels.first()) }
    var tender by remember { mutableStateOf("UPI") }
    var query by remember { mutableStateOf("") }
    var holdQuery by remember { mutableStateOf("") }
    var customer by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var quickAmount by remember { mutableStateOf("") }
    var selectedHoldId by remember { mutableStateOf<String?>(null) }
    var gstPicker by remember { mutableStateOf("Inclusive · selection only") }
    var billPrint by remember { mutableStateOf("Thermal · share UPI QR") }
    val cart = remember { mutableStateListOf<RetailPosLine>() }
    var detailsOpen by remember { mutableStateOf(false) }
    var showSkuPicker by remember { mutableStateOf(false) }

    val holds = holdSession.holds
    val filteredHolds = remember(holds, holdQuery) {
        val q = holdQuery.trim()
        if (q.isEmpty()) holds
        else holds.filter {
            it.label.contains(q, true) ||
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
    val subtotalText = if (subtotal.signum() <= 0) {
        "—"
    } else {
        RetailFormat.exact(currency, subtotal.stripTrailingZeros().toPlainString())
    }
    val pricedLines = cart.filter { it.unitPrice.toBigDecimalOrNull()?.signum() == 1 }
    val postable = when (posMode) {
        RetailPosMode.QuickAmount -> quickAmount.toBigDecimalOrNull()?.signum() == 1
        RetailPosMode.HoldRecall -> false
        RetailPosMode.ScanCart -> cart.isNotEmpty() && pricedLines.size == cart.size
    }
    val recallReady = posMode == RetailPosMode.HoldRecall && recalled != null

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
        val noteParts = listOfNotNull("Tender: $tender", note.takeIf { it.isNotBlank() })
        when (posMode) {
            RetailPosMode.QuickAmount -> onSubmit(
                RetailEventDraft(
                    eventType = "SALE",
                    title = customer.takeIf { it.isNotBlank() }?.let { "Sale · $it" } ?: "Quick amount sale",
                    amount = quickAmount,
                    note = noteParts.joinToString(" · "),
                    channelName = channel,
                    counterpartyName = customer.takeIf { it.isNotBlank() },
                    paymentStatus = "COLLECTED",
                    contract = OperatingContractBody(
                        entryMode = "QUICK_AMOUNT",
                        paymentState = "RECEIVED",
                        receivedAmount = quickAmount,
                        taxMode = "NOT_CONFIGURED",
                    ),
                    lines = null,
                ),
            )
            RetailPosMode.ScanCart -> {
                val lines = cart.map {
                    OperatingLineBody(
                        itemName = it.name,
                        quantity = it.quantity.toString(),
                        unit = it.unit,
                        unitPrice = it.unitPrice,
                        masterId = it.masterId,
                    )
                }
                val title = when {
                    customer.isNotBlank() -> "Sale · ${customer.trim()}"
                    recalled != null -> "Sale · ${recalled.label}"
                    else -> "Sale · ${cart.size} line${if (cart.size == 1) "" else "s"}"
                }
                onSubmit(
                    RetailEventDraft(
                        eventType = "SALE",
                        title = title,
                        amount = subtotal.stripTrailingZeros().toPlainString(),
                        note = noteParts.joinToString(" · "),
                        channelName = channel,
                        counterpartyName = customer.takeIf { it.isNotBlank() },
                        paymentStatus = "COLLECTED",
                        contract = OperatingContractBody(
                            entryMode = "ITEMIZED",
                            paymentState = "RECEIVED",
                            receivedAmount = subtotal.stripTrailingZeros().toPlainString(),
                            taxMode = "NOT_CONFIGURED",
                        ),
                        lines = lines,
                    ),
                )
                selectedHoldId?.let { id -> holdSession.holds.removeAll { it.id == id } }
            }
            RetailPosMode.HoldRecall -> Unit
        }
    }

    RetailSheetBody(Modifier.verticalScroll(rememberScrollState())) {
        RoseGrabber()
        RoseSheetHeader(
            title = "POS Sale",
            subtitle = listOfNotNull(
                model.title?.takeIf { it.isNotBlank() },
                when (posMode) {
                    RetailPosMode.ScanCart ->
                        if (cart.isEmpty()) "Scan cart"
                        else "${cart.size} lines · $cartUnits unit${if (cartUnits == 1) "" else "s"}"
                    RetailPosMode.HoldRecall ->
                        "${holds.size} hold${if (holds.size == 1) "" else "s"} · draft only"
                    RetailPosMode.QuickAmount -> "Amount only · no stock"
                },
            ).joinToString(" · "),
            onClose = onDismiss,
        )

        RoseFieldLabel("Mode")
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
                    placeholder = if (skus.isEmpty()) "No SKU — add under Catalogue" else "Barcode or product name",
                    onQueryChange = { query = it },
                    onScan = {
                        val trimmed = query.trim()
                        val match = skus.firstOrNull {
                            it.barcode.equals(trimmed, true) ||
                                it.code.equals(trimmed, true) ||
                                (trimmed.isNotBlank() && it.name.contains(trimmed, true))
                        } ?: skus.firstOrNull()
                        if (match != null && cart.none { it.masterId == match.id }) {
                            cart += RetailPosLine.of(match)
                        }
                        query = ""
                        showSkuPicker = false
                    },
                )
                retailSkuQuickPick(skus, query, currency) { sku ->
                    if (cart.none { it.masterId == sku.id }) cart += RetailPosLine.of(sku)
                    query = ""
                    showSkuPicker = false
                }

                RoseLinesCard("Cart", cart.size) {
                    Text(
                        if (cart.isEmpty()) "No lines yet"
                        else "${cart.size} lines · $cartUnits units",
                        style = RetailRose.rowCaption(),
                    )
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
                            style = RetailRose.viewAll(),
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

                RoseTotalsCard("Payable now", subtotalText, tender = tender) {
                    RoseTotalRow("Subtotal", subtotalText)
                    RoseTotalRow("Discount", "− ₹0")
                    RoseUnavailableGstRow(
                        label = "CGST + SGST (incl.)",
                        caption = "Unavailable — stamped server-side on bill",
                    )
                }

                RoseFieldLabel("Tender")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("UPI", "Cash", "Card").forEach { option ->
                        RoseModeChip(option, selected = option == tender, onClick = { tender = option })
                    }
                    RoseLockedChip("Split")
                }

                RosePickerRow(
                    label = "Customer",
                    value = customer.ifBlank { "Walk-in · B2C" },
                    onClick = { detailsOpen = true },
                )
                RosePickerRow(
                    label = "Bill / print",
                    value = billPrint,
                    onClick = {
                        billPrint = if (billPrint.startsWith("Thermal")) {
                            "A4 invoice · email"
                        } else {
                            "Thermal · share UPI QR"
                        }
                    },
                )

                RoseMoreDetails(detailsOpen, { detailsOpen = !detailsOpen }) {
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
                }

                RoseWhyBox(
                    "POS posts one itemized sale + linked tender receipt atomically. Stock − on post when tracked. Hold is draft only — no revenue, no stock. Quick amount stays amount-only.",
                    tinted = true,
                )

                RoseStickyFooter(
                    label = if (postable) "Charge $subtotalText · Post & print" else "Charge · Post & print",
                    enabled = postable,
                    caveat = "Idempotent post · draft kept on picker · print chrome-only",
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

                RoseSectionCard("Parked on this register") {
                    Text(
                        if (holds.isEmpty()) "No holds · draft only"
                        else "${holds.size} hold${if (holds.size == 1) "" else "s"} · draft only",
                        style = RetailRose.rowCaption(),
                    )
                    if (filteredHolds.isEmpty()) {
                        RoseEmpty(
                            if (holds.isEmpty()) "No parked bills" else "No matches",
                            if (holds.isEmpty()) {
                                "Park one under Utilities · Hold, then recall here."
                            } else {
                                "Try another name or bill #."
                            },
                        )
                    } else {
                        filteredHolds.forEach { hold ->
                            val selected = hold.id == selectedHoldId
                            val parkId = hold.parkDisplayId()
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (selected) RetailRose.rose.copy(alpha = 0.16f) else RetailRose.field)
                                    .border(
                                        1.dp,
                                        if (selected) RetailRose.rose else Color.White.copy(alpha = 0.06f),
                                        RoundedCornerShape(12.dp),
                                    )
                                    .clickable {
                                        if (selected) {
                                            selectedHoldId = null
                                        } else {
                                            selectedHoldId = hold.id
                                        }
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(parkId, style = RetailRose.rowCaption(RetailRose.amber).copy(fontSize = 10.sp))
                                    Text(hold.label, style = RetailRose.rowTitle(), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        listOf(
                                            "${hold.lines.size} line${if (hold.lines.size == 1) "" else "s"}",
                                            registerLabel,
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
                                        style = RetailRose.rowValue(),
                                    )
                                    Text(hold.ageLabel(), style = RetailRose.rowCaption().copy(fontSize = 10.sp))
                                }
                            }
                        }
                        Text(
                            "Empty holds auto-expire end of shift · session parks only",
                            style = RetailRose.rowCaption(),
                        )
                    }
                }

                RoseTotalsCard(
                    "Selected",
                    recalled?.let {
                        RetailFormat.exact(currency, it.total().stripTrailingZeros().toPlainString())
                    } ?: "—",
                    tender = null,
                ) {
                    RoseTotalRow(
                        "Hold",
                        recalled?.parkDisplayId() ?: "Pick a parked bill",
                        valueColor = RetailRose.amber,
                    )
                    Text(
                        "Tap Recall to refill Scan cart — still draft until Charge",
                        style = RetailRose.rowCaption(),
                    )
                }

                RoseMoreDetails(detailsOpen, { detailsOpen = !detailsOpen }) {
                    Text(
                        "Void hold · Merge · Transfer register — chrome only on this sheet.",
                        style = RetailRose.rowCaption(),
                    )
                    RoseFieldLabel("Note")
                    RoseTextField(note, "Recall note", onValueChange = { note = it }, singleLine = false)
                }

                RoseWhyBox(
                    "Hold is draft cart JSON only — no revenue, no stock, no tender. Recall opens Scan cart with lines restored. Void discards draft.",
                    tinted = true,
                )

                RoseStickyFooter(
                    label = if (recallReady) {
                        "Recall ${recalled!!.parkDisplayId()} → Scan cart"
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
                    label = "Sale amount · $currency",
                    amount = if (quickAmount.isBlank()) "—" else RetailFormat.exact(currency, quickAmount),
                    caption = "No SKU lines · revenue only · stock unchanged",
                )

                RoseSectionCard("Keypad") {
                    Text("tap to enter", style = RetailRose.rowCaption())
                    RoseNumericKeypad(value = quickAmount, onValueChange = { quickAmount = it })
                }

                RoseTotalsCard("Payable now", subtotalText, tender = tender) {
                    RoseUnavailableGstRow(
                        label = "GST (inclusive)",
                        caption = "Unavailable — no client tax engine",
                    )
                    RoseTotalRow("Discount", "− ₹0")
                }

                RoseFieldLabel("Tender")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("UPI", "Cash", "Card").forEach { option ->
                        RoseModeChip(option, selected = option == tender, onClick = { tender = option })
                    }
                    RoseLockedChip("Split")
                }

                RosePickerRow(
                    label = "Customer",
                    value = customer.ifBlank { "Walk-in (cash/UPI)" },
                    onClick = { detailsOpen = true },
                )
                RosePickerRow(
                    label = "Channel",
                    value = channel,
                    onClick = {
                        val idx = channels.indexOf(channel)
                        channel = channels[(idx + 1).coerceAtLeast(0) % channels.size]
                    },
                )
                RosePickerRow(
                    label = "GST",
                    value = gstPicker,
                    onClick = {
                        gstPicker = if (gstPicker.startsWith("Inclusive")) {
                            "Exclusive · selection only"
                        } else {
                            "Inclusive · selection only"
                        }
                    },
                )

                RoseMoreDetails(detailsOpen, { detailsOpen = !detailsOpen }) {
                    RoseFieldLabel("Customer name")
                    RoseTextField(customer, "Walk-in or named credit buyer", onValueChange = { customer = it })
                    RoseFieldLabel("Note")
                    RoseTextField(note, "Notes · Attachment · Tags", onValueChange = { note = it }, singleLine = false)
                }

                RoseWhyBox(
                    "Quick amount posts revenue + linked tender with zero SKU movement. Same rule as original QA-01 — kept inside POS so the counter never leaves this sheet. Credit still needs a named customer.",
                    tinted = true,
                )

                RoseStickyFooter(
                    label = if (postable) "Charge $subtotalText · Post" else "Charge · Post",
                    enabled = postable,
                    caveat = "No stock move · draft kept on mode switch",
                    onClick = { postSale() },
                )
            }
        }

        Spacer(Modifier.height(16.dp))
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
                        Text(it, style = RetailRose.rowCaption(RetailRose.amber).copy(fontSize = 10.sp), maxLines = 1)
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
                        .background(RetailRose.divider.copy(alpha = 0.6f)),
                )
            }
        }
    }
}

private fun RetailParkedCart.parkDisplayId(): String {
    val short = id.replace("-", "").take(4).uppercase()
    return if (short.isBlank()) "HOLD" else "HOLD-$short"
}

private fun RetailParkedCart.ageLabel(): String {
    val mins = TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - createdAt).coerceAtLeast(0)
    return when {
        mins < 1 -> "just now"
        mins < 60 -> "${mins}m ago"
        else -> "${mins / 60}h ago"
    }
}

internal data class RetailPosLine(
    val masterId: String,
    val name: String,
    val unit: String,
    val unitPrice: String,
    val quantity: Int,
    val skuCode: String? = null,
    val hsn: String? = null,
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
        )
    }
}

package com.example.momentra.ui.shell.business.retail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.getValue
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
import com.example.momentra.data.api.OperatingActivityDto
import com.example.momentra.data.api.OperatingContractBody
import com.example.momentra.data.api.OperatingLineBody
import com.example.momentra.data.api.OperatingMasterDto
import com.example.momentra.data.api.OperatingProjectionDto
import com.example.momentra.ui.theme.RetailRose
import java.math.BigDecimal
import java.util.UUID

/** Cross-nav targets for Hold | Transfer | Reprint chips (Figma utility strip). */
internal enum class RetailUtilityNav { HOLD, TRANSFER, REPRINT }

/** In-memory parked carts for this app session — no park API. */
internal class RetailHoldSession {
    val holds = mutableStateListOf<RetailParkedCart>()
}

internal data class RetailParkedCart(
    val id: String = UUID.randomUUID().toString(),
    val holdCode: String,
    val label: String,
    val lines: List<RetailPosLine>,
    val note: String?,
    val channelName: String?,
    val createdAt: Long = System.currentTimeMillis(),
) {
    fun total(): BigDecimal = lines.fold(BigDecimal.ZERO) { acc, line -> acc + line.amount() }
}

internal fun retailHoldCodeFor(id: String): String {
    val short = id.replace("-", "").take(4).uppercase()
    return if (short.isBlank()) "HOLD" else "HOLD-$short"
}

internal fun RetailHoldSession.park(
    label: String,
    lines: List<RetailPosLine>,
    note: String?,
    channelName: String?,
    id: String = UUID.randomUUID().toString(),
    holdCode: String = retailHoldCodeFor(id),
) {
    holds += RetailParkedCart(
        id = id,
        holdCode = holdCode,
        label = label,
        lines = lines.toList(),
        note = note,
        channelName = channelName,
    )
}

private val RetailStationDefaults = listOf("All lines", "Staples", "Snacks", "Dairy", "Beverages")

/** `2508:29333` Park bill — parks draft cart locally; no revenue post. */
@Composable
internal fun RetailParkBillSheet(
    model: OperatingProjectionDto,
    holdSession: RetailHoldSession,
    onDismiss: () -> Unit,
    onParked: () -> Unit = onDismiss,
    onNavigate: ((RetailUtilityNav) -> Unit)? = null,
) {
    val currency = model.currency?.takeIf { it.isNotBlank() } ?: "INR"
    val registers = remember(model) {
        model.life?.channels.orEmpty().filter { it.isNotBlank() }
            .ifEmpty { listOf("Register 1", "Register 2", "Online") }
            .take(3)
    }
    val skus = remember(model.masters) { retailCatalogMasters(model) }
    val location = model.location?.takeIf { it.isNotBlank() } ?: "Store"
    val holdId = remember { UUID.randomUUID().toString() }
    val holdCode = remember(holdId) { retailHoldCodeFor(holdId) }

    var register by remember { mutableStateOf(registers.getOrElse(1) { registers.first() }) }
    var customer by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var station by remember { mutableStateOf("All lines") }
    var query by remember { mutableStateOf("") }
    var slipTo by remember { mutableStateOf("Counter printer 2") }
    val cart = remember { mutableStateListOf<RetailPosLine>() }

    val masterById = remember(skus) { skus.associateBy { it.id } }
    val stations = remember(cart, masterById) {
        val cats = cart.mapNotNull { masterById[it.masterId]?.retailStation() }.distinct()
        listOf("All lines") + (if (cats.isEmpty()) RetailStationDefaults.drop(1) else cats)
    }
    val visibleCart = cart.filter { station == "All lines" || masterById[it.masterId]?.retailStation() == station }
    val postable = cart.isNotEmpty()
    val heroCustomer = customer.ifBlank { "Walk-in" }

    RetailSheetBody(Modifier.verticalScroll(rememberScrollState())) {
        RoseGrabber()
        RoseSheetHeader(
            title = "Hold / Park bill",
            subtitle = "Parked cart · no revenue, no stock",
            onClose = onDismiss,
        )
        RetailUtilityCrossNav(RetailUtilityNav.HOLD, onNavigate)
        RetailRegisterChips(registers, register) { register = it }
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            RoseModeChip("$register · Shift open", selected = true)
            RoseModeChip("Park · recall later", selected = false)
            RoseModeChip(
                if (model.gstTracking.equals("ON", true) || model.gstRegistration != null) "GST on" else "GST off",
                selected = false,
            )
        }
        RoseAmountHero(
            label = "Hold slip · $holdCode",
            amount = "$heroCustomer",
            caption = "${cart.size} line${if (cart.size == 1) "" else "s"} · $location · $register",
        )
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            stations.forEach { name ->
                RoseModeChip(name, selected = name == station, onClick = { station = name })
            }
        }
        RoseLinesCard("Parked lines", cart.size) {
            RoseScanField(
                query = query,
                placeholder = if (skus.isEmpty()) "No SKU — add catalogue first" else "Scan or search SKU",
                onQueryChange = { query = it },
                onScan = {},
            )
            retailSkuQuickPick(skus, query, currency) { sku ->
                if (cart.none { it.masterId == sku.id }) cart += RetailPosLine.of(sku)
                query = ""
            }
            if (visibleCart.isEmpty()) {
                RoseEmpty("Nothing on the hold slip", "Scan Harbor Mart SKUs to park a draft cart.")
            } else {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RetailRose.rInner)
                        .background(RetailRose.chip)
                        .padding(horizontal = 12.dp),
                ) {
                    visibleCart.forEach { line ->
                        val index = cart.indexOfFirst { it.masterId == line.masterId }
                        val master = masterById[line.masterId]
                        val cat = master?.retailStation() ?: "Staples"
                        val meta = listOfNotNull(
                            master?.code?.takeIf { it.isNotBlank() }
                                ?: master?.barcode?.takeIf { it.isNotBlank() },
                            line.unitPrice.takeIf { it.isNotBlank() }?.let { RetailFormat.exact(currency, it) },
                            master?.hsn?.takeIf { it.isNotBlank() }?.let { "HSN $it" },
                            master?.gstRate?.takeIf { it.isNotBlank() }?.let { "$it%" }
                                ?: master?.gstApplicable?.takeIf { it.equals("EXEMPT", true) }?.let { "Exempt" },
                        ).joinToString(" · ").ifBlank { line.unit }
                        RetailUtilityLineRow(
                            title = line.name,
                            qty = line.quantity,
                            meta = meta,
                            category = cat,
                            statusChip = "Parked",
                            onDecrement = if (index >= 0) {
                                {
                                    if (line.quantity <= 1) cart.removeAt(index)
                                    else cart[index] = line.copy(quantity = line.quantity - 1)
                                }
                            } else null,
                            onIncrement = if (index >= 0) {
                                {
                                    if (line.quantity < 99) cart[index] = line.copy(quantity = line.quantity + 1)
                                }
                            } else null,
                            onRemove = if (index >= 0) {
                                { cart.removeAt(index) }
                            } else null,
                        )
                    }
                }
            }
        }
        RoseSectionCard("Ticket") {
            RoseFieldLabel("Customer / hold label")
            RoseTextField(customer, "Walk-in · Priya", onValueChange = { customer = it.take(60) })
            RoseFieldLabel("Note")
            RoseTextField(note, "Customer stepped out", onValueChange = { note = it }, singleLine = false)
        }
        RoseMetaRows(
            listOf(
                "Slip to" to slipTo,
                "Cashier" to (model.participants.firstOrNull()?.takeIf { it.isNotBlank() } ?: "Cashier"),
                "Linked bill" to "Not posted · draft",
            ),
        )
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            listOf("Counter printer 1", "Counter printer 2", "Screen only").forEach { printer ->
                RoseModeChip(printer, selected = printer == slipTo, onClick = { slipTo = printer })
            }
            RoseLockedChip("Thermal print")
        }
        Text("Slip to is chrome-only — no print stack.", style = RetailRose.rowCaption(RetailRose.textFaint))
        RoseWhyBox(
            "Hold parks a draft cart. No revenue, no stock, no tender and no GST until Charge on Scan cart.",
            tinted = true,
        )
        RoseStickyFooter(
            label = if (postable) "Park bill · $holdCode" else "Park bill",
            enabled = postable,
            caveat = "Draft only — revenue + GST post on Charge",
            onClick = {
                holdSession.park(
                    id = holdId,
                    label = customer.trim().ifBlank { "Walk-in · $register" },
                    lines = cart.toList(),
                    note = note.takeIf { it.isNotBlank() },
                    channelName = register,
                    holdCode = holdCode,
                )
                onParked()
            },
        )
        Spacer(Modifier.height(16.dp))
    }
}

/** `2508:29447` Stock transfer between locations — multi-line cart; existing STOCK+TRANSFER path. */
@Composable
internal fun RetailStockTransferSheet(
    model: OperatingProjectionDto,
    onDismiss: () -> Unit,
    onSubmit: (RetailEventDraft) -> Unit,
    onNavigate: ((RetailUtilityNav) -> Unit)? = null,
) {
    val currency = model.currency?.takeIf { it.isNotBlank() } ?: "INR"
    val locations = remember(model.masters) {
        model.masters.orEmpty().filter { it.kind in setOf("LOCATION", "STORE", "CHANNEL", "WAREHOUSE") }
    }
    val skus = remember(model.masters) { retailCatalogMasters(model) }
    val registers = remember(model) {
        model.life?.channels.orEmpty().filter { it.isNotBlank() }
            .ifEmpty { listOf("Register 1", "Register 2", "Online") }
            .take(3)
    }
    val transferCode = remember { "TRF-${(100..999).random()}" }

    var register by remember { mutableStateOf(registers.getOrElse(1) { registers.first() }) }
    var fromName by remember { mutableStateOf(locations.firstOrNull()?.name.orEmpty()) }
    var toName by remember { mutableStateOf(locations.getOrNull(1)?.name.orEmpty()) }
    var station by remember { mutableStateOf("All lines") }
    var query by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    val cart = remember { mutableStateListOf<RetailPosLine>() }

    val from = locations.firstOrNull { it.name.equals(fromName, true) }
    val to = locations.firstOrNull { it.name.equals(toName, true) }
    val masterById = remember(skus) { skus.associateBy { it.id } }
    val stations = remember(cart, masterById) {
        val cats = cart.mapNotNull { masterById[it.masterId]?.retailStation() }.distinct()
        listOf("All lines") + (if (cats.isEmpty()) listOf("Staples", "Beverages") else cats)
    }
    val visibleCart = cart.filter { station == "All lines" || masterById[it.masterId]?.retailStation() == station }
    val totalUnits = cart.sumOf { it.quantity }
    val postable = from != null && to != null && from.id != to.id && cart.isNotEmpty()
    val routeLabel = when {
        fromName.isBlank() || toName.isBlank() -> "Pick locations"
        else -> "$fromName → $toName"
    }

    RetailSheetBody(Modifier.verticalScroll(rememberScrollState())) {
        RoseGrabber()
        RoseSheetHeader(
            title = "Stock transfer",
            subtitle = "Move stock between locations · no revenue",
            onClose = onDismiss,
        )
        RetailUtilityCrossNav(RetailUtilityNav.TRANSFER, onNavigate)
        RetailRegisterChips(registers, register) { register = it }
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            RoseModeChip(routeLabel, selected = true)
            RoseModeChip("Same GSTIN", selected = false)
            RoseModeChip("GST on", selected = false)
        }
        RoseAmountHero(
            label = "Transfer slip · $transferCode",
            amount = routeLabel,
            caption = "${cart.size} line${if (cart.size == 1) "" else "s"} · dispatch now",
        )
        RoseSectionCard("Route") {
            if (locations.size < 2) {
                RoseEmpty("Need two locations", "Add stores or aisles under Locations first.")
            }
            RosePickerRow("From", fromName.ifBlank { "Pick location" }, onClick = null)
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                locations.take(8).forEach { loc ->
                    RoseModeChip(loc.name, selected = loc.name == fromName, onClick = { fromName = loc.name })
                }
            }
            RosePickerRow("To", toName.ifBlank { "Pick location" }, onClick = null)
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                locations.take(8).forEach { loc ->
                    RoseModeChip(loc.name, selected = loc.name == toName, onClick = { toName = loc.name })
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            stations.forEach { name ->
                RoseModeChip(name, selected = name == station, onClick = { station = name })
            }
        }
        RoseLinesCard("Transfer lines", cart.size) {
            RoseScanField(
                query = query,
                placeholder = if (skus.isEmpty()) "No SKU — add catalogue first" else "Scan or search SKU",
                onQueryChange = { query = it },
                onScan = {},
            )
            retailSkuQuickPick(skus, query, currency) { sku ->
                val existing = cart.indexOfFirst { it.masterId == sku.id }
                if (existing >= 0) {
                    val line = cart[existing]
                    if (line.quantity < 99) cart[existing] = line.copy(quantity = line.quantity + 1)
                } else {
                    cart += RetailPosLine.of(sku)
                }
                query = ""
            }
            if (visibleCart.isEmpty()) {
                RoseEmpty("No lines to move", "Scan SKUs to build a multi-line transfer.")
            } else {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RetailRose.rInner)
                        .background(RetailRose.chip)
                        .padding(horizontal = 12.dp),
                ) {
                    visibleCart.forEach { line ->
                        val index = cart.indexOfFirst { it.masterId == line.masterId }
                        val master = masterById[line.masterId]
                        val cat = master?.retailStation() ?: "Staples"
                        val code = master?.code?.takeIf { it.isNotBlank() }
                            ?: master?.barcode?.takeIf { it.isNotBlank() }
                            ?: "SKU"
                        RetailUtilityLineRow(
                            title = line.name,
                            qty = line.quantity,
                            meta = "$code · ${line.quantity} ${line.unit} · on hand —",
                            category = cat,
                            statusChip = "Move",
                            onDecrement = if (index >= 0) {
                                {
                                    if (line.quantity <= 1) cart.removeAt(index)
                                    else cart[index] = line.copy(quantity = line.quantity - 1)
                                }
                            } else null,
                            onIncrement = if (index >= 0) {
                                {
                                    if (line.quantity < 99) cart[index] = line.copy(quantity = line.quantity + 1)
                                }
                            } else null,
                            onRemove = if (index >= 0) {
                                { cart.removeAt(index) }
                            } else null,
                        )
                    }
                }
            }
        }
        RoseMetaRows(
            listOf(
                "Valuation" to "At cost · no GST within the same GSTIN",
                "Receiver" to (toName.ifBlank { "—" }),
                "Sale impact" to "None",
            ),
        )
        RoseFieldLabel("Note")
        RoseTextField(note, "Dispatch note", onValueChange = { note = it }, singleLine = false)
        RoseWhyBox(
            "Transfer moves stock between locations at cost: − from, + to on confirm. No revenue, no AP, and no GST within the same GSTIN.",
            tinted = true,
        )
        RoseStickyFooter(
            label = if (postable) "Confirm transfer · $totalUnits units" else "Confirm transfer",
            enabled = postable,
            caveat = "Transfer never posts a sale",
            onClick = {
                val lines = cart.map {
                    OperatingLineBody(
                        itemName = it.name,
                        quantity = it.quantity.toString(),
                        unit = it.unit,
                        unitPrice = it.unitPrice.takeIf { p -> p.isNotBlank() },
                        masterId = it.masterId,
                    )
                }
                val first = cart.first()
                onSubmit(
                    RetailEventDraft(
                        eventType = "STOCK",
                        title = "Transfer · ${cart.size} line${if (cart.size == 1) "" else "s"}",
                        amount = null,
                        note = note.takeIf { it.isNotBlank() } ?: "Register $register",
                        channelName = fromName,
                        counterpartyName = toName,
                        paymentStatus = null,
                        quantity = totalUnits.toString(),
                        unit = first.unit,
                        itemName = if (cart.size == 1) first.name else "${cart.size} SKUs",
                        contract = OperatingContractBody(
                            stockAction = "TRANSFER",
                            transferFromName = fromName,
                            transferToName = toName,
                            transferFromMasterId = from?.id,
                            transferToMasterId = to?.id,
                        ),
                        lines = lines,
                    ),
                )
            },
        )
        Spacer(Modifier.height(16.dp))
    }
}

/** `2508:29537` Reprint — read-only; never posts a new sale. */
@Composable
internal fun RetailReprintSheet(
    model: OperatingProjectionDto,
    onDismiss: () -> Unit,
    onNavigate: ((RetailUtilityNav) -> Unit)? = null,
) {
    val currency = model.currency?.takeIf { it.isNotBlank() } ?: "INR"
    val registers = remember(model) {
        model.life?.channels.orEmpty().filter { it.isNotBlank() }
            .ifEmpty { listOf("Register 1", "Register 2", "Online") }
            .take(3)
    }
    val sales = remember(model.activity) {
        model.activity
            .filter { it.eventType == "SALE" }
            .sortedByDescending { it.occurredAt }
            .take(8)
    }
    var register by remember { mutableStateOf(registers.getOrElse(1) { registers.first() }) }
    var selected by remember { mutableStateOf<OperatingActivityDto?>(sales.firstOrNull()) }
    var station by remember { mutableStateOf("All lines") }
    var reason by remember { mutableStateOf("Customer copy lost") }
    val amount = selected?.amount?.let { RetailFormat.exact(currency, it) } ?: "—"
    val billNo = selected?.clientKey?.takeIf { it.isNotBlank() }
        ?: selected?.id?.takeLast(5)?.let { "HM-B-$it" }
        ?: "—"
    val postedAt = selected?.occurredAt?.takeLast(5)?.takeIf { it.contains(":") }
        ?: selected?.occurredAt?.take(10)
        ?: "—"
    val tenderHint = selected?.paymentStatus?.takeIf { it.isNotBlank() } ?: "Settled"

    RetailSheetBody(Modifier.verticalScroll(rememberScrollState())) {
        RoseGrabber()
        RoseSheetHeader(
            title = "Reprint receipt",
            subtitle = "Re-print posted bill · same bill no.",
            onClose = onDismiss,
        )
        RetailUtilityCrossNav(RetailUtilityNav.REPRINT, onNavigate)
        RetailRegisterChips(registers, register) { register = it }
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            RoseModeChip("Bill $billNo", selected = true)
            RoseModeChip("Posted $postedAt · $tenderHint", selected = false)
            RoseModeChip("GST on", selected = false)
        }
        RoseAmountHero(
            label = "Tax invoice · $billNo",
            amount = selected?.let {
                listOfNotNull(
                    it.counterpartyName?.takeIf { n -> n.isNotBlank() }?.let { n -> "B2C · $n" },
                    model.gstState?.takeIf { s -> s.isNotBlank() }?.let { s -> "$s intra-state" },
                ).joinToString(" · ").ifBlank { amount }
            } ?: "Nothing selected",
            caption = listOfNotNull(
                selected?.quantity?.let { "qty $it" } ?: selected?.let { "Posted sale" },
                amount.takeIf { it != "—" },
                "CGST + SGST stamped server-side",
            ).joinToString(" · "),
        )
        RoseSectionCard("Pick sale") {
            if (sales.isEmpty()) {
                RoseEmpty("Nothing to reprint", "Posted sales will appear here.")
            } else {
                sales.forEach { sale ->
                    val picked = sale.id == selected?.id
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (picked) RetailRose.rose.copy(alpha = 0.16f) else RetailRose.field)
                            .border(
                                1.dp,
                                if (picked) RetailRose.rose else Color.White.copy(alpha = 0.06f),
                                RoundedCornerShape(10.dp),
                            )
                            .clickable { selected = sale }
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                sale.title.ifBlank { "Sale" },
                                style = RetailRose.rowTitle(),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                listOfNotNull(
                                    sale.occurredAt?.take(10),
                                    sale.channelName,
                                ).joinToString(" · "),
                                style = RetailRose.rowCaption().copy(fontSize = 10.sp),
                            )
                        }
                        Text(
                            RetailFormat.exact(currency, sale.amount),
                            style = RetailRose.rowValue(RetailRose.amber).copy(fontSize = 12.sp),
                        )
                    }
                }
            }
        }
        selected?.let { sale ->
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                listOf("All lines", "Staples", "Dairy").forEach { name ->
                    RoseModeChip(name, selected = name == station, onClick = { station = name })
                }
            }
            RoseLinesCard("Receipt preview", 1) {
                val previewMeta = sale.subtitle?.takeIf { it.isNotBlank() }
                    ?: sale.note?.takeIf { it.isNotBlank() }
                    ?: "Line detail not on activity row — amount only."
                RetailUtilityLineRow(
                    title = sale.title.ifBlank { "Posted sale" },
                    qty = sale.quantity?.toBigDecimalOrNull()?.toInt() ?: 1,
                    meta = previewMeta,
                    category = sale.channelName ?: "Posted",
                    statusChip = "Posted",
                )
                Text(
                    "Preview chrome only — same bill number, no new SALE.",
                    style = RetailRose.rowCaption(RetailRose.textFaint),
                )
            }
            RoseMetaRows(
                listOf(
                    "Original print" to listOfNotNull(postedAt, sale.channelName ?: register).joinToString(" · "),
                    "Reprint reason" to reason,
                    "Payment" to listOfNotNull(
                        sale.paymentStatus ?: "Settled",
                        amount.takeIf { it != "—" },
                    ).joinToString(" · "),
                ),
            )
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                listOf("Customer copy lost", "Printer jam", "Audit copy").forEach { option ->
                    RoseModeChip(option, selected = option == reason, onClick = { reason = option })
                }
            }
            RoseLockedChip("PDF / share")
        }
        RoseWhyBox(
            "Reprint re-renders the posted bill with the same number and GST split. No new sale, no stock or tender change.",
            tinted = true,
        )
        RoseStickyFooter(
            label = "Reprint · duplicate copy",
            enabled = selected != null,
            caveat = "Same bill no. — never duplicates revenue",
            onClick = { /* read-only — no re-post */ },
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun RetailUtilityCrossNav(
    active: RetailUtilityNav,
    onNavigate: ((RetailUtilityNav) -> Unit)?,
) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        RetailUtilityNav.entries.forEach { dest ->
            val label = when (dest) {
                RetailUtilityNav.HOLD -> "Hold"
                RetailUtilityNav.TRANSFER -> "Transfer"
                RetailUtilityNav.REPRINT -> "Reprint"
            }
            RoseModeChip(
                text = label,
                selected = dest == active,
                onClick = {
                    if (dest != active) onNavigate?.invoke(dest)
                },
            )
        }
    }
}

@Composable
private fun RetailRegisterChips(
    registers: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        registers.forEach { name ->
            RoseModeChip(name, selected = name == selected, onClick = { onSelect(name) })
        }
    }
}

@Composable
private fun RetailUtilityLineRow(
    title: String,
    qty: Int,
    meta: String,
    category: String,
    statusChip: String,
    onDecrement: (() -> Unit)? = null,
    onIncrement: (() -> Unit)? = null,
    onRemove: (() -> Unit)? = null,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                title,
                style = RetailRose.rowTitle(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (onDecrement != null && onIncrement != null) {
                RoseStepper(qty, onDecrement = onDecrement, onIncrement = onIncrement)
            } else {
                Text("×$qty", style = RetailRose.rowValue().copy(fontSize = 13.sp))
            }
            onRemove?.let {
                Text(
                    "✕",
                    modifier = Modifier.clickable(onClick = it).padding(4.dp),
                    style = RetailRose.rowCaption(RetailRose.textFaint),
                )
            }
        }
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                meta,
                style = RetailRose.rowCaption().copy(fontSize = 10.sp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            SeverityChip(category, RetailRose.amber, bordered = false)
            SeverityChip(statusChip, RetailRose.rose, bordered = false)
        }
    }
}

private fun OperatingMasterDto.retailStation(): String {
    val raw = dimension?.takeIf { it.isNotBlank() }
        ?: name.substringBefore(' ').takeIf { it.length in 3..12 }
        ?: "Staples"
    return when {
        raw.contains("snack", true) -> "Snacks"
        raw.contains("dairy", true) || raw.contains("milk", true) -> "Dairy"
        raw.contains("beverage", true) || raw.contains("drink", true) -> "Beverages"
        raw.contains("personal", true) || raw.contains("care", true) -> "Personal care"
        raw.contains("home", true) -> "Home care"
        else -> raw.replaceFirstChar { it.uppercase() }.take(16)
    }
}

internal fun retailCatalogMasters(model: OperatingProjectionDto): List<OperatingMasterDto> =
    model.masters.orEmpty()
        .filter { it.kind in setOf("PRODUCT", "ITEM", "SKU", "MATERIAL") }
        .sortedBy { it.name.lowercase() }

@Composable
internal fun retailSkuQuickPick(
    skus: List<OperatingMasterDto>,
    query: String,
    currency: String,
    onPick: (OperatingMasterDto) -> Unit,
) {
    if (skus.isEmpty()) return
    val trimmed = query.trim()
    val matches = when {
        trimmed.isBlank() -> emptyList()
        else -> skus.filter {
            it.name.contains(trimmed, true) || it.barcode?.contains(trimmed, true) == true ||
                it.code?.contains(trimmed, true) == true
        }.take(4)
    }
    if (matches.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            matches.forEach { sku ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(RetailRose.field)
                        .clickable { onPick(sku) }
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(sku.name, style = RetailRose.rowTitle(), modifier = Modifier.weight(1f), maxLines = 1)
                    sku.sellPrice?.takeIf { it.isNotBlank() }?.let {
                        Text(RetailFormat.exact(currency, it), style = RetailRose.rowCaption(RetailRose.amber))
                    }
                }
            }
        }
    }
}

@Composable
internal fun retailCartLines(
    cart: List<RetailPosLine>,
    currency: String,
    onUpdate: (Int, RetailPosLine) -> Unit,
    onRemove: (Int) -> Unit,
) {
    if (cart.isEmpty()) {
        RoseEmpty("Nothing on the ticket", "Scan or search a catalogue SKU.")
        return
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RetailRose.rInner)
            .background(RetailRose.chip)
            .padding(horizontal = 12.dp),
    ) {
        cart.forEachIndexed { index, line ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(line.name, style = RetailRose.rowTitle(), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    RoseTextField(
                        line.unitPrice,
                        "Price",
                        onValueChange = { raw ->
                            onUpdate(index, line.copy(unitPrice = raw.filter { it.isDigit() || it == '.' }.take(9)))
                        },
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
                    style = RetailRose.rowValue().copy(fontSize = 12.sp),
                )
                Text(
                    "✕",
                    modifier = Modifier.clickable { onRemove(index) }.padding(4.dp),
                    style = RetailRose.rowCaption(RetailRose.textFaint),
                )
            }
        }
    }
}

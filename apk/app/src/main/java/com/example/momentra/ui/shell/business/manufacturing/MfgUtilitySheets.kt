package com.example.momentra.ui.shell.business.manufacturing

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.api.OperatingProjectionDto
import com.example.momentra.ui.theme.ManufacturingAmber

internal enum class MfgUtilityNav { HOLD, TRANSFER, REPRINT }

@Composable
internal fun MfgUtilityCrossNav(current: MfgUtilityNav, onNavigate: ((MfgUtilityNav) -> Unit)?) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        MfgUtilityNav.entries.forEach { nav ->
            val label = when (nav) {
                MfgUtilityNav.HOLD -> "Hold"
                MfgUtilityNav.TRANSFER -> "Transfer"
                MfgUtilityNav.REPRINT -> "Reprint"
            }
            MfgModeChip(
                text = label,
                selected = nav == current,
                onClick = { if (nav != current) onNavigate?.invoke(nav) },
            )
        }
    }
}

/** Figma `2547:29335` Work order hold — no stock / cost / GST. */
@Composable
internal fun MfgHoldWoSheet(
    model: OperatingProjectionDto,
    onDismiss: () -> Unit,
    onNavigate: ((MfgUtilityNav) -> Unit)?,
    onSubmit: (ManufacturingEventDraft) -> Unit,
) {
    val openOrders = model.activity.filter {
        it.eventType.equals("ORDER", true) || it.eventType.equals("CUSTOMER_ORDER", true) ||
            it.title.contains("WO", ignoreCase = true)
    }
    var line by remember { mutableStateOf("Line 1") }
    var stationFilter by remember { mutableStateOf("All items") }
    val wohCode = remember {
        val n = (model.activity.size % 90) + 10
        "WOH-$n"
    }
    val wo = openOrders.firstOrNull()
    val woTitle = wo?.title ?: "WO · open work order"
    val plant = model.location?.takeIf { it.isNotBlank() } ?: model.title ?: "Plant"
    val lines = remember(model.masters) {
        model.masters.orEmpty()
            .filter { it.kind.equals("SKU", true) || it.kind.equals("PART", true) || it.kind.equals("PRODUCT", true) }
            .take(4)
            .mapIndexed { i, m ->
                val cat = listOf("Raw", "Raw", "WIP", "Consumables").getOrElse(i) { "Raw" }
                Triple(m.name, listOfNotNull(m.code, m.unit?.let { "1 $it" }, m.hsn?.let { "HSN $it" }).joinToString(" · "), cat)
            }
            .ifEmpty {
                listOf(
                    Triple("CRC sheet 1.2mm", "RM-044 · 0.4 kg/unit · HSN 7209", "Raw"),
                    Triple("M8 bolt set", "CS-112 · 4 nos/unit · HSN 7318", "Raw"),
                    Triple("Paint · grey", "CS-031 · 20 ml/unit · HSN 3208", "WIP"),
                    Triple("Packing carton", "PK-207 · 1/10 units · HSN 4819", "Consumables"),
                )
            }
    }
    val visible = lines.filter { stationFilter == "All items" || it.third == stationFilter }

    MfgSheetBody(Modifier.verticalScroll(rememberScrollState())) {
        MfgGrabber()
        MfgSheetHeader(
            title = "Work order hold",
            subtitle = "Paused WO · no stock move, no cost posting",
            onClose = onDismiss,
        )
        MfgUtilityCrossNav(MfgUtilityNav.HOLD, onNavigate)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Line 1", "Line 2", "QC bay").forEach { MfgModeChip(it, selected = line == it, onClick = { line = it }) }
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            MfgModeChip("$line · Shift A", selected = true)
            MfgModeChip("Hold · resume or release", selected = false)
            MfgModeChip(
                if (model.gstTracking.equals("ON", true) || model.gstRegistration != null) "GST on" else "GST off",
                selected = false,
            )
        }
        MfgAmountHero(
            label = "Hold slip",
            amount = woTitle,
            caption = "${lines.size} components · $plant · $line",
            currencyBadge = wohCode,
        )
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("All items", "Raw", "WIP", "Finished", "Consumables").forEach {
                MfgModeChip(it, selected = stationFilter == it, onClick = { stationFilter = it })
            }
        }
        MfgLinesCard("Held components", visible.size) {
            if (visible.isEmpty()) {
                MfgEmpty("Nothing on hold slip", "Pick a WO with components to hold.")
            } else {
                visible.forEach { (name, meta, cat) ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(ManufacturingAmber.chip)
                            .border(1.dp, ManufacturingAmber.accent.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(name, style = ManufacturingAmber.rowTitle().copy(fontSize = 14.sp), modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("×1", style = ManufacturingAmber.rowValue(ManufacturingAmber.accentSoft))
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(meta, style = ManufacturingAmber.rowCaption(), modifier = Modifier.weight(1f), maxLines = 2)
                            MfgModeChip(cat, selected = false)
                            MfgModeChip("Held", selected = true, tender = true)
                        }
                    }
                }
            }
        }
        MfgMetaRows(
            listOf(
                "Notify" to "Line supervisor",
                "Held by" to (model.role?.takeIf { it.isNotBlank() } ?: "Operator"),
                "Linked SO" to (openOrders.getOrNull(1)?.title ?: "—"),
            ),
        )
        MfgWhyBox("Hold pauses a work order. No material issue, no production posting and no GST; resume continues the same WO.")
        MfgStickyFooter(
            label = "Hold WO · $wohCode",
            enabled = true,
            caveat = "No stock or cost moves while held",
            onClick = {
                // D7: post ORDER hold note — never invent stock moves.
                onSubmit(
                    ManufacturingEventDraft(
                        eventType = "ORDER",
                        title = "Hold · $wohCode · $woTitle",
                        amount = null,
                        note = "HOLD · no stock · no cost · no GST · line=$line · $wohCode",
                        channelName = line,
                        counterpartyName = null,
                        paymentStatus = null,
                        contract = com.example.momentra.data.api.OperatingContractBody(
                            orderStatus = "HOLD",
                            clientKey = wohCode,
                        ),
                    ),
                )
            },
        )
        Spacer(Modifier.height(16.dp))
    }
}

/** Figma `2547:29449` Stock / WIP transfer — no revenue. */
@Composable
internal fun MfgTransferSheet(
    model: OperatingProjectionDto,
    onDismiss: () -> Unit,
    onNavigate: ((MfgUtilityNav) -> Unit)?,
    onSubmit: (ManufacturingEventDraft) -> Unit,
) {
    val locations = model.masters.orEmpty().filter {
        it.kind.equals("LOCATION", true) || it.kind.equals("STORE", true) || it.kind.equals("LINE", true)
    }
    var from by remember { mutableStateOf(locations.getOrNull(0)?.name ?: "Raw store") }
    var to by remember { mutableStateOf(locations.getOrNull(1)?.name ?: "Press line 1") }
    var qty by remember { mutableStateOf("1") }
    var item by remember { mutableStateOf("") }
    val skus = model.masters.orEmpty().filter {
        it.kind.equals("SKU", true) || it.kind.equals("PART", true) || it.kind.equals("PRODUCT", true)
    }
    val postable = item.isNotBlank() && from != to && (qty.toBigDecimalOrNull()?.signum() == 1)

    MfgSheetBody(Modifier.verticalScroll(rememberScrollState())) {
        MfgGrabber()
        MfgSheetHeader(title = "Stock / WIP transfer", subtitle = "Move at cost · no revenue · same GSTIN", onClose = onDismiss)
        MfgUtilityCrossNav(MfgUtilityNav.TRANSFER, onNavigate)
        MfgAmountHero(
            label = "Transfer slip",
            amount = if (item.isBlank()) "Select material" else item,
            caption = "$from → $to · valuation at standard cost",
            currencyBadge = "Sale impact · None",
        )
        MfgFieldLabel("Material")
        MfgTextField(item, "Part / WIP / FG", onValueChange = { item = it })
        skus.take(4).forEach { m ->
            MfgPickerRow(m.name, m.code ?: m.kind, onClick = { item = m.name })
        }
        MfgPickerRow("From", from, onClick = { from = locations.firstOrNull()?.name ?: from }, externalLabel = true)
        locations.take(3).forEach { MfgPickerRow(it.name, it.kind, onClick = { from = it.name }) }
        MfgPickerRow("To", to, onClick = { to = locations.getOrNull(1)?.name ?: to }, externalLabel = true)
        locations.take(3).forEach { MfgPickerRow(it.name, it.kind, onClick = { to = it.name }) }
        MfgFieldLabel("Qty")
        MfgTextField(qty, "1", onValueChange = { qty = it.filter { ch -> ch.isDigit() || ch == '.' }.take(10) })
        MfgLockedChip("Job work out · challan locked")
        MfgWhyBox("Transfer moves stock/WIP at cost inside the same GSTIN — never a sale. Job work out needs a challan (Unavailable until API).")
        MfgStickyFooter(
            label = if (postable) "Confirm transfer · 1 item" else "Confirm transfer",
            enabled = postable,
            caveat = "Draft preserved if you close or open a picker",
            onClick = {
                onSubmit(
                    ManufacturingEventDraft(
                        eventType = "STOCK",
                        title = "Transfer · $item",
                        amount = null,
                        note = "TRANSFER · $from → $to · same-GSTIN · no revenue",
                        channelName = to,
                        counterpartyName = from,
                        paymentStatus = null,
                        quantity = qty,
                        itemName = item,
                        unit = "nos",
                    ),
                )
            },
        )
        Spacer(Modifier.height(16.dp))
    }
}

/** Figma `2547:29539` Reprint — read-only duplicate; never new SALE. */
@Composable
internal fun MfgReprintSheet(
    model: OperatingProjectionDto,
    onDismiss: () -> Unit,
    onNavigate: ((MfgUtilityNav) -> Unit)?,
) {
    val sales = model.activity.filter { it.eventType.equals("SALE", true) }
    var selected by remember { mutableStateOf(sales.firstOrNull()?.title ?: "") }
    var reason by remember { mutableStateOf("Customer copy") }
    val sale = sales.firstOrNull { it.title == selected } ?: sales.firstOrNull()
    val inv = sale?.title ?: "No posted invoice"

    MfgSheetBody(Modifier.verticalScroll(rememberScrollState())) {
        MfgGrabber()
        MfgSheetHeader(title = "Reprint invoice / job card", subtitle = "Duplicate copy · same number · no new revenue", onClose = onDismiss)
        MfgUtilityCrossNav(MfgUtilityNav.REPRINT, onNavigate)
        MfgAmountHero(
            label = "Tax invoice / job card",
            amount = inv,
            caption = listOfNotNull(sale?.counterpartyName, sale?.amount?.let { MfgFormat.exact(model.currency, it) }).joinToString(" · ").ifBlank { "Posted lines · original print" },
            currencyBadge = "Duplicate",
        )
        MfgFieldLabel("Source invoice / job card")
        sales.take(5).forEach { row ->
            MfgPickerRow(row.title, row.status ?: "POSTED", onClick = { selected = row.title })
        }
        if (sales.isEmpty()) MfgEmpty("No posted invoices", "Reprint needs a posted sale or job card.")
        MfgChipRow("Reprint reason", listOf("Customer copy", "Job card", "Lost print", "Audit"), reason) { reason = it }
        MfgMetaRows(
            listOf(
                "Original print" to (sale?.occurredAt?.take(10) ?: "—"),
                "Payment" to (sale?.paymentStatus ?: "—"),
                "Print" to "Locked · chrome only",
            ),
        )
        MfgLockedChip("Thermal / PDF print · Unavailable")
        MfgWhyBox("Reprint re-renders the same invoice or job card. It never creates a new SALE, AR, or invoice number.")
        MfgStickyFooter(
            label = "Reprint · duplicate copy",
            enabled = sale != null,
            caveat = "Same number · no new revenue",
            onClick = onDismiss, // D5 chrome-only — no SALE post
        )
        Spacer(Modifier.height(16.dp))
    }
}

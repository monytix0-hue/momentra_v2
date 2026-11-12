package com.example.momentra.ui.shell.business.mto
import com.example.momentra.ui.shell.business.manufacturing.ManufacturingEventDraft

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
import com.example.momentra.ui.theme.MadeToOrderAmber

internal enum class MtoUtilityNav { HOLD, TRANSFER, REPRINT }

@Composable
internal fun MtoUtilityCrossNav(current: MtoUtilityNav, onNavigate: ((MtoUtilityNav) -> Unit)?) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        MtoUtilityNav.entries.forEach { nav ->
            val label = when (nav) {
                MtoUtilityNav.HOLD -> "Hold"
                MtoUtilityNav.TRANSFER -> "Transfer"
                MtoUtilityNav.REPRINT -> "Reprint"
            }
            MtoModeChip(
                text = label,
                selected = nav == current,
                onClick = { if (nav != current) onNavigate?.invoke(nav) },
            )
        }
    }
}

/** Figma `2547:29335` Job hold — no stock / cost / GST. */
@Composable
internal fun MtoHoldJobSheet(
    model: OperatingProjectionDto,
    onDismiss: () -> Unit,
    onNavigate: ((MtoUtilityNav) -> Unit)?,
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
        "JH-$n"
    }
    val wo = openOrders.firstOrNull()
    val woTitle = wo?.title ?: "J · open job"
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

    MtoSheetBody(Modifier.verticalScroll(rememberScrollState())) {
        MtoGrabber()
        MtoSheetHeader(
            title = "Job hold",
            subtitle = "Paused J · no stock move, no cost posting",
            onClose = onDismiss,
        )
        MtoUtilityCrossNav(MtoUtilityNav.HOLD, onNavigate)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Line 1", "Line 2", "QC bay").forEach { MtoModeChip(it, selected = line == it, onClick = { line = it }) }
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            MtoModeChip("$line · Shift A", selected = true)
            MtoModeChip("Hold · resume or release", selected = false)
            MtoModeChip(
                if (model.gstTracking.equals("ON", true) || model.gstRegistration != null) "GST on" else "GST off",
                selected = false,
            )
        }
        MtoAmountHero(
            label = "Hold slip",
            amount = woTitle,
            caption = "${lines.size} components · $plant · $line",
            currencyBadge = wohCode,
        )
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("All items", "Raw", "WIP", "Finished", "Consumables").forEach {
                MtoModeChip(it, selected = stationFilter == it, onClick = { stationFilter = it })
            }
        }
        MtoLinesCard("Held components", visible.size) {
            if (visible.isEmpty()) {
                MtoEmpty("Nothing on hold slip", "Pick a job with components to hold.")
            } else {
                visible.forEach { (name, meta, cat) ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MadeToOrderAmber.chip)
                            .border(1.dp, MadeToOrderAmber.accent.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(name, style = MadeToOrderAmber.rowTitle().copy(fontSize = 14.sp), modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("×1", style = MadeToOrderAmber.rowValue(MadeToOrderAmber.accentSoft))
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(meta, style = MadeToOrderAmber.rowCaption(), modifier = Modifier.weight(1f), maxLines = 2)
                            MtoModeChip(cat, selected = false)
                            MtoModeChip("Held", selected = true, tender = true)
                        }
                    }
                }
            }
        }
        MtoMetaRows(
            listOf(
                "Notify" to "Line supervisor",
                "Held by" to (model.role?.takeIf { it.isNotBlank() } ?: "Operator"),
                "Linked SO" to (openOrders.getOrNull(1)?.title ?: "—"),
            ),
        )
        MtoWhyNote("Hold pauses a job. No material issue, no production posting and no GST; resume continues the same job.")
        MtoStickyFooter(
            label = "Hold job · $wohCode",
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
internal fun MtoTransferSheet(
    model: OperatingProjectionDto,
    onDismiss: () -> Unit,
    onNavigate: ((MtoUtilityNav) -> Unit)?,
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

    MtoSheetBody(Modifier.verticalScroll(rememberScrollState())) {
        MtoGrabber()
        MtoSheetHeader(title = "Stock / WIP transfer", subtitle = "Move at cost · no revenue · same GSTIN", onClose = onDismiss)
        MtoUtilityCrossNav(MtoUtilityNav.TRANSFER, onNavigate)
        MtoAmountHero(
            label = "Transfer slip",
            amount = if (item.isBlank()) "Select material" else item,
            caption = "$from → $to · valuation at standard cost",
            currencyBadge = "Sale impact · None",
        )
        MtoFieldLabel("Material")
        MtoTextField(item, "Part / WIP / FG", onValueChange = { item = it })
        skus.take(4).forEach { m ->
            MtoPickerRow(m.name, m.code ?: m.kind, onClick = { item = m.name })
        }
        MtoPickerRow("From", from, onClick = { from = locations.firstOrNull()?.name ?: from }, externalLabel = true)
        locations.take(3).forEach { MtoPickerRow(it.name, it.kind, onClick = { from = it.name }) }
        MtoPickerRow("To", to, onClick = { to = locations.getOrNull(1)?.name ?: to }, externalLabel = true)
        locations.take(3).forEach { MtoPickerRow(it.name, it.kind, onClick = { to = it.name }) }
        MtoFieldLabel("Qty")
        MtoTextField(qty, "1", onValueChange = { qty = it.filter { ch -> ch.isDigit() || ch == '.' }.take(10) })
        MtoLockedChip("Job work out · challan locked")
        MtoWhyNote("Transfer moves stock/WIP at cost inside the same GSTIN — never a sale. Job work out needs a challan (Unavailable until API).")
        MtoStickyFooter(
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

/** Figma `2698:38140` Reprint — read-only duplicate; never new SALE. */
@Composable
internal fun MtoReprintSheet(
    model: OperatingProjectionDto,
    onDismiss: () -> Unit,
    onNavigate: ((MtoUtilityNav) -> Unit)?,
) {
    val sales = model.activity.filter { it.eventType.equals("SALE", true) }
    var selected by remember { mutableStateOf(sales.firstOrNull()?.title ?: "") }
    var reason by remember { mutableStateOf("Customer copy") }
    var station by remember { mutableStateOf("All") }
    val sale = sales.firstOrNull { it.title == selected } ?: sales.firstOrNull()
    val inv = sale?.title ?: "No posted invoice"
    val postedLines = listOf(
        Triple("Harbor flange set", "FG · J-07 · 12 nos", "Cut"),
        Triple("CRC sheet 2.0mm", "Raw · 48 kg", "Machine"),
        Triple("Packing crate · hardwood", "Consumable · 1 set", "Pack"),
        Triple("Plate finish · out", "Plate (out) · vendor", "Plate"),
    )
    val visibleLines = postedLines.filter { station == "All" || it.third == station }

    MtoSheetBody(Modifier.verticalScroll(rememberScrollState())) {
        MtoGrabber()
        MtoSheetHeader(title = "Reprint invoice / job card", subtitle = "Duplicate copy · same number · no new revenue", onClose = onDismiss)
        MtoUtilityCrossNav(MtoUtilityNav.REPRINT, onNavigate)
        MtoAmountHero(
            label = "Tax invoice / job card",
            amount = inv,
            caption = listOfNotNull(sale?.counterpartyName, sale?.amount?.let { MtoFormat.exact(model.currency, it) }).joinToString(" · ").ifBlank { "Posted lines · original print" },
            currencyBadge = "Duplicate",
        )
        MtoFieldLabel("Source invoice / job card")
        sales.take(5).forEach { row ->
            MtoPickerRow(row.title, row.status ?: "POSTED", onClick = { selected = row.title })
        }
        if (sales.isEmpty()) MtoEmpty("No posted invoices", "Reprint needs a posted sale or job card.")
        MtoChipRow(
            "Station",
            listOf("All", "Cut", "Machine", "Plate", "QC", "Pack", "Dispatch"),
            station,
        ) { station = it }
        MtoLinesCard("Posted lines", visibleLines.size) {
            if (visibleLines.isEmpty()) {
                MtoEmpty("No lines for $station", "Switch station chip or pick another invoice.")
            } else {
                visibleLines.forEach { (name, meta, st) ->
                    MtoPickerRow(name, "$meta · $st", onClick = null)
                }
            }
        }
        MtoChipRow("Reprint reason", listOf("Customer copy", "Job card", "Lost print", "Audit"), reason) { reason = it }
        MtoMetaRows(
            listOf(
                "Original print" to (sale?.occurredAt?.take(10) ?: "—"),
                "Payment" to (sale?.paymentStatus ?: "—"),
                "Print" to "Locked · chrome only",
            ),
        )
        MtoLockedChip("Thermal / PDF print · Unavailable")
        MtoWhyNote("Reprint re-renders the same invoice or job card. It never creates a new SALE, AR, or invoice number.")
        MtoStickyFooter(
            label = "Reprint · duplicate copy",
            enabled = sale != null,
            caveat = "Same number · no new revenue",
            onClick = onDismiss, // D5 chrome-only — no SALE post
        )
        Spacer(Modifier.height(16.dp))
    }
}

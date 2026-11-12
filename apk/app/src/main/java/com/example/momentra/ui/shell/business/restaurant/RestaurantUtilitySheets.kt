package com.example.momentra.ui.shell.business.restaurant

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
import com.example.momentra.data.api.OperatingContractBody
import com.example.momentra.data.api.OperatingProjectionDto
import com.example.momentra.ui.theme.RestaurantEmber

/** Cross-nav for adjacent hub Hold · Transfer · Reprint (not trading layouts). */
internal enum class RestaurantUtilityNav { HOLD, TRANSFER, REPRINT }

@Composable
internal fun RestaurantUtilityCrossNav(
    current: RestaurantUtilityNav,
    onNavigate: ((RestaurantUtilityNav) -> Unit)?,
) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        RestaurantUtilityNav.entries.forEach { nav ->
            val label = when (nav) {
                RestaurantUtilityNav.HOLD -> "Hold"
                RestaurantUtilityNav.TRANSFER -> "Transfer"
                RestaurantUtilityNav.REPRINT -> "Reprint"
            }
            EmberModeChip(
                text = label,
                selected = nav == current,
                onClick = { if (nav != current) onNavigate?.invoke(nav) },
            )
        }
    }
}

/** Park order — device-local hold; never posts revenue. */
@Composable
internal fun RestaurantHoldSheet(
    model: OperatingProjectionDto,
    onDismiss: () -> Unit,
    onNavigate: ((RestaurantUtilityNav) -> Unit)?,
    onSubmit: (RestaurantEventDraft) -> Unit,
) {
    val open = openOrders(model)
    var selectedId by remember { mutableStateOf(open.firstOrNull()?.id) }
    val selected = open.firstOrNull { it.id == selectedId } ?: open.firstOrNull()
    val holdCode = remember { "HOLD-${(open.size % 90) + 10}" }
    val brand = RestaurantCaptureCopy.posBrand(model.title)

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        EmberSheetGrabber()
        EmberSheetHeader("Hold / park order", "Park draft · no revenue · no KOT", onDismiss)
        RestaurantUtilityCrossNav(RestaurantUtilityNav.HOLD, onNavigate)
        FlowRowChips(listOf(brand, "Floor hold", "No sale"), firstSelected = true)
        EmberUtilityTicketHero(
            eyebrow = "Hold slip",
            badge = holdCode,
            headline = selected?.title?.ifBlank { null } ?: "Open ticket",
            detail = "${selected?.note ?: "Park until guest ready"} · $holdCode",
        )
        EmberFieldLabel("Open tickets")
        if (open.isEmpty()) {
            EmberEmpty("No open order", "Send a KOT or place an order before parking.")
        } else {
            open.take(6).forEach { ticket ->
                val active = ticket.id == selected?.id
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (active) RestaurantEmber.ember.copy(alpha = 0.14f) else RestaurantEmber.chip)
                        .border(1.dp, if (active) RestaurantEmber.ember else RestaurantEmber.hairline, RoundedCornerShape(14.dp))
                        .clickable { selectedId = ticket.id }
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(ticket.title.ifBlank { "Order" }, style = RestaurantEmber.rowTitle().copy(fontSize = 13.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(ticket.note?.take(48) ?: "Open", style = RestaurantEmber.rowCaption().copy(fontSize = 10.sp), maxLines = 1)
                    }
                    EmberModeChip(if (active) "Hold" else "Park", selected = active, onClick = { selectedId = ticket.id })
                }
            }
        }
        EmberWhyNote(RestaurantCaptureCopy.HOLD_WHY, tinted = true)
        EmberSheetFooter("Hold order · $holdCode", selected != null, "No revenue or stock while held") {
            val ticket = selected ?: return@EmberSheetFooter
            onSubmit(
                RestaurantEventDraft(
                    eventType = "ORDER",
                    title = "Hold · $holdCode · ${ticket.title}",
                    amount = null,
                    note = "HOLD · no revenue · no KOT · $holdCode",
                    channelName = ticket.channelName,
                    counterpartyName = ticket.counterpartyName,
                    paymentStatus = null,
                    revisesEventId = ticket.id,
                    contract = OperatingContractBody(orderStatus = "HOLD", clientKey = holdCode),
                ),
            )
        }
        Spacer(Modifier.height(16.dp))
    }
}

/** Stock transfer between stations — never a sale. */
@Composable
internal fun RestaurantTransferSheet(
    model: OperatingProjectionDto,
    onDismiss: () -> Unit,
    onNavigate: ((RestaurantUtilityNav) -> Unit)?,
    onSubmit: (RestaurantEventDraft) -> Unit,
) {
    val stations = model.masters.orEmpty()
        .filter { it.kind.uppercase() in setOf("STATION", "KITCHEN_STATION", "LOCATION", "STORE") }
        .map { it.name }
        .ifEmpty { listOf("Cold store", "Hot pass", "Bar", "Dry store") }
    var from by remember { mutableStateOf(stations.first()) }
    var to by remember { mutableStateOf(stations.getOrElse(1) { stations.first() }) }
    var item by remember { mutableStateOf("Paneer") }
    var qty by remember { mutableStateOf("2") }
    val code = remember { "TRF-${((model.activity.size % 90) + 1).toString().padStart(3, '0')}" }
    val postable = from != to && qty.toBigDecimalOrNull()?.signum() == 1

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        EmberSheetGrabber()
        EmberSheetHeader("Transfer stock", "Station → station · cost only", onDismiss)
        RestaurantUtilityCrossNav(RestaurantUtilityNav.TRANSFER, onNavigate)
        FlowRowChips(listOf(RestaurantCaptureCopy.posBrand(model.title), "No sale", "Same GSTIN"), firstSelected = true)
        EmberUtilityTicketHero(
            eyebrow = "Transfer slip",
            badge = code,
            headline = "$from → $to",
            detail = "$item · $qty · $code",
        )
        EmberChevronField("From", from, onClick = {
            val idx = stations.indexOf(from)
            from = stations[(idx + 1).coerceAtLeast(0) % stations.size]
        })
        EmberChevronField("To", to, onClick = {
            val idx = stations.indexOf(to)
            to = stations[(idx + 1).coerceAtLeast(0) % stations.size]
        })
        EmberFieldLabel("Item")
        EmberTextField(item, "Paneer") { item = it.take(40) }
        EmberFieldLabel("Qty")
        EmberTextField(qty, "2") { qty = it.filter { ch -> ch.isDigit() || ch == '.' }.take(8) }
        EmberWhyNote(RestaurantCaptureCopy.TRANSFER_WHY, tinted = true)
        EmberSheetFooter("Confirm transfer · $code", postable, "Transfer never posts a sale") {
            onSubmit(
                RestaurantEventDraft(
                    eventType = "STOCK",
                    title = "Transfer · $item · $from → $to",
                    amount = null,
                    note = "TRANSFER · $code · from=$from · to=$to · no sale",
                    channelName = from,
                    counterpartyName = to,
                    paymentStatus = null,
                    quantity = qty,
                    itemName = item,
                    contract = OperatingContractBody(
                        stockAction = "TRANSFER",
                        locationName = from,
                        clientKey = code,
                    ),
                ),
            )
        }
        Spacer(Modifier.height(16.dp))
    }
}

/** Receipt reprint — same bill id; never a new sale. */
@Composable
internal fun RestaurantReprintReceiptSheet(
    model: OperatingProjectionDto,
    onDismiss: () -> Unit,
    onNavigate: ((RestaurantUtilityNav) -> Unit)?,
    onSubmit: (RestaurantEventDraft) -> Unit,
) {
    val bills = model.activity.filter { it.eventType.equals("SALE", true) }
    var selectedId by remember { mutableStateOf(bills.firstOrNull()?.id) }
    val selected = bills.firstOrNull { it.id == selectedId } ?: bills.firstOrNull()
    var reason by remember { mutableStateOf("Guest copy · printer jam") }

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        EmberSheetGrabber()
        EmberSheetHeader("Reprint receipt", "Same bill id · no new sale", onDismiss)
        RestaurantUtilityCrossNav(RestaurantUtilityNav.REPRINT, onNavigate)
        FlowRowChips(listOf(RestaurantCaptureCopy.posBrand(model.title), "Posted bill", "Share"), firstSelected = true)
        EmberUtilityTicketHero(
            eyebrow = "Receipt",
            badge = selected?.id?.takeLast(6)?.let { "BILL-$it" } ?: "—",
            headline = selected?.title?.ifBlank { null } ?: "No posted bill",
            detail = selected?.let { RestaurantFormat.exact(model.currency ?: "INR", it.amount) } ?: "Unavailable",
        )
        EmberFieldLabel("Posted bills")
        if (bills.isEmpty()) {
            EmberEmpty("No posted bill", "Charge a POS bill first — reprint never invents revenue.")
        } else {
            bills.take(6).forEach { bill ->
                val active = bill.id == selected?.id
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (active) RestaurantEmber.ember.copy(alpha = 0.14f) else RestaurantEmber.chip)
                        .border(1.dp, if (active) RestaurantEmber.ember else RestaurantEmber.hairline, RoundedCornerShape(14.dp))
                        .clickable { selectedId = bill.id }
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(bill.title.ifBlank { "Sale" }, style = RestaurantEmber.rowTitle().copy(fontSize = 13.sp), maxLines = 1)
                        Text(RestaurantFormat.exact(model.currency ?: "INR", bill.amount), style = RestaurantEmber.rowCaption().copy(fontSize = 10.sp))
                    }
                    EmberModeChip(if (active) "Reprint" else "Pick", selected = active, onClick = { selectedId = bill.id })
                }
            }
        }
        EmberFieldLabel("Reprint reason")
        EmberTextField(reason, "Guest copy · printer jam") { reason = it.take(60) }
        EmberWhyNote(RestaurantCaptureCopy.REPRINT_RECEIPT_WHY, tinted = true)
        EmberSheetFooter("Reprint receipt", selected != null, "Same bill id — never duplicates the sale") {
            val bill = selected ?: return@EmberSheetFooter
            onSubmit(
                RestaurantEventDraft(
                    eventType = "UPDATE",
                    title = "Reprint · ${bill.title}",
                    amount = null,
                    note = "REPRINT receipt · same id ${bill.id} · $reason · no new sale",
                    channelName = bill.channelName,
                    counterpartyName = bill.counterpartyName,
                    paymentStatus = null,
                    parentEventId = bill.id,
                ),
            )
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun FlowRowChips(options: List<String>, firstSelected: Boolean) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        options.forEachIndexed { index, opt ->
            EmberModeChip(
                text = opt,
                selected = firstSelected && index == 0,
                selectedFill = if (index == 0) RestaurantEmber.wine else RestaurantEmber.ember,
                selectedInk = RestaurantEmber.textPrimary,
                onClick = null,
            )
        }
    }
}

@Composable
private fun EmberUtilityTicketHero(
    eyebrow: String,
    badge: String,
    headline: String,
    detail: String,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(RestaurantEmber.chip)
            .border(1.5.dp, RestaurantEmber.ember, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(eyebrow, style = RestaurantEmber.rowCaption().copy(fontSize = 11.sp))
            Text(badge, style = RestaurantEmber.rowValue(RestaurantEmber.gold).copy(fontSize = 12.sp))
        }
        Text(headline, style = RestaurantEmber.screenTitle().copy(fontSize = 22.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(detail, style = RestaurantEmber.rowCaption().copy(fontSize = 12.sp), maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

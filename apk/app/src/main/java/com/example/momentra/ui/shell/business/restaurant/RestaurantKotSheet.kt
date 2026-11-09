package com.example.momentra.ui.shell.business.restaurant

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.example.momentra.data.api.OperatingActivityDto
import com.example.momentra.data.api.OperatingContractBody
import com.example.momentra.data.api.OperatingLineBody
import com.example.momentra.data.api.OperatingProjectionDto
import com.example.momentra.ui.theme.RestaurantEmber

/**
 * Kitchen ticket — Figma `RST-QA-01-ALT · KOT · Send` `2449:29655`,
 * `· Fire` `2449:29769`, `· Reprint` `2449:29859`.
 *
 * Send posts an `ORDER` with `orderStatus = PLACED` and real `lines[]` — an order is
 * never revenue, which is exactly what the frame says. Fire revises that same ORDER to
 * `PREPARING` through `PATCH …/operating-events/:eventId`, keeping the ticket's lines.
 * Reprint has no ticket-PDF endpoint, so it is absent rather than mocked.
 *
 * Station routing (Tandoor / Hot / Cold / Bar) persists as `STATION` masters and is echoed
 * in the ticket note; there is no per-line station field on the contract.
 */
@Composable
internal fun RestaurantKotSheet(
    model: OperatingProjectionDto,
    onDismiss: () -> Unit,
    onSubmit: (RestaurantEventDraft) -> Unit,
) {
    val stations = remember(model.masters) {
        model.masters.orEmpty()
            .filter { it.kind.uppercase() == "STATION" }
            .sortedBy { it.name.lowercase() }
    }
    val menuDishes = remember(model.masters) {
        model.masters.orEmpty()
            .filter { it.kind.uppercase() == "MENU_DISH" }
            .sortedBy { it.name.lowercase() }
    }
    val tickets = remember(model.activity) { openOrders(model).sortedByDescending { it.occurredAt } }

    var tab by remember { mutableStateOf(KotTab.Send) }
    var ticketId by remember { mutableStateOf<String?>(tickets.firstOrNull()?.id) }
    var station by remember { mutableStateOf(stations.firstOrNull()?.name.orEmpty()) }
    var dishQuery by remember { mutableStateOf("") }
    var guest by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    val lines = remember { mutableStateListOf<KotLine>() }

    val selectedTicket = tickets.firstOrNull { it.id == ticketId }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = RestaurantEmber.gutter, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        EmberSheetGrabber()
        EmberSheetHeader(
            title = "Kitchen Ticket",
            subtitle = "${tickets.size} open ticket${if (tickets.size == 1) "" else "s"} · " +
                if (stations.isEmpty()) "no stations set" else "${stations.size} station${if (stations.size == 1) "" else "s"}",
            onClose = onDismiss,
        )

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            EmberModeChip("Send", selected = tab == KotTab.Send) { tab = KotTab.Send }
            EmberModeChip("Fire", selected = tab == KotTab.Fire) { tab = KotTab.Fire }
            EmberModeChip("Reprint", selected = false, locked = "No PDF endpoint", onClick = null)
        }
        Text(
            when (tab) {
                KotTab.Send -> "Send posts an order. An order is never revenue — only a posted bill is."
                KotTab.Fire -> "Fire revises that same order to Preparing, so the kitchen sees one ticket, not two."
            },
            style = RestaurantEmber.rowCaption(RestaurantEmber.textFaint).copy(fontSize = 10.sp),
        )

        if (tab == KotTab.Send) {
            SendBody(
                stations = stations,
                menuDishes = menuDishes,
                station = station,
                onStation = { station = it },
                dishQuery = dishQuery,
                onDishQuery = { dishQuery = it },
                guest = guest,
                onGuest = { guest = it },
                note = note,
                onNote = { note = it },
                lines = lines,
            )
        } else {
            FireBody(
                tickets = tickets,
                selectedTicket = selectedTicket,
                selectedId = ticketId,
                onSelect = { ticketId = it },
            )
        }

        EmberWhyNote(
            when (tab) {
                KotTab.Send -> "Station rides on the ticket note. The contract has no per-line station field."
                KotTab.Fire -> "Fire never reposts revenue. The bill posts separately once served."
            },
            tinted = true,
        )

        EmberSheetFooter(
            label = when (tab) {
                KotTab.Send -> "Send ticket"
                KotTab.Fire -> "Fire ticket"
            },
            enabled = if (tab == KotTab.Send) {
                lines.isNotEmpty() && station.isNotBlank()
            } else {
                selectedTicket != null && selectedTicket.status?.uppercase() != "PREPARING"
            },
            caveat = "Draft preserved if you close the sheet",
            onClick = {
                if (tab == KotTab.Send) {
                    val ticketLines = lines.map {
                        OperatingLineBody(
                            itemName = it.name,
                            quantity = it.quantity.toString(),
                            unit = it.unit,
                            unitPrice = null,
                            masterId = it.masterId,
                        )
                    }
                    onSubmit(
                        RestaurantEventDraft(
                            eventType = "ORDER",
                            title = if (guest.isNotBlank()) {
                                "Ticket · $guest"
                            } else {
                                "Ticket · ${lines.size} item${if (lines.size == 1) "" else "s"}"
                            },
                            amount = null,
                            note = listOf(
                                station.takeIf { it.isNotBlank() }?.let { "Station: $it" },
                                note.takeIf { it.isNotBlank() },
                            ).filterNotNull().joinToString(" · ").takeIf { it.isNotBlank() },
                            channelName = station.takeIf { it.isNotBlank() },
                            counterpartyName = guest.takeIf { it.isNotBlank() },
                            paymentStatus = "NONE",
                            contract = OperatingContractBody(
                                orderStatus = "PLACED",
                                taxMode = "NOT_CONFIGURED",
                            ),
                            lines = ticketLines,
                        ),
                    )
                } else {
                    val ticket = selectedTicket ?: return@EmberSheetFooter
                    onSubmit(
                        RestaurantEventDraft(
                            eventType = "ORDER",
                            title = ticket.title.ifBlank { "Kitchen ticket" },
                            amount = ticket.amount,
                            note = ticket.note,
                            channelName = ticket.channelName,
                            counterpartyName = ticket.counterpartyName,
                            paymentStatus = "NONE",
                            revisesEventId = ticket.id,
                            contract = OperatingContractBody(orderStatus = "PREPARING"),
                        ),
                    )
                }
            },
        )
        Spacer(Modifier.height(16.dp))
    }
}

private enum class KotTab { Send, Fire }

@Composable
private fun SendBody(
    stations: List<com.example.momentra.data.api.OperatingMasterDto>,
    menuDishes: List<com.example.momentra.data.api.OperatingMasterDto>,
    station: String,
    onStation: (String) -> Unit,
    dishQuery: String,
    onDishQuery: (String) -> Unit,
    guest: String,
    onGuest: (String) -> Unit,
    note: String,
    onNote: (String) -> Unit,
    lines: MutableList<KotLine>,
) {
    EmberFieldLabel("Station")
    if (stations.isEmpty()) {
        EmberEmpty(
            "No station yet",
            "Add Tandoor / Hot / Cold / Bar under Stations so the pass knows where to send.",
        )
    } else {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            stations.take(4).forEach { master ->
                EmberModeChip(
                    text = master.name,
                    selected = master.name == station,
                    onClick = { onStation(master.name) },
                )
            }
        }
    }

    EmberFieldLabel("Dish")
    if (menuDishes.isEmpty()) {
        EmberEmpty("Menu not set up", "Add dishes under Menu first — a ticket line needs a real dish.")
    } else {
        EmberTextField(
            value = dishQuery,
            placeholder = "Search menu",
            onValueChange = onDishQuery,
        )
        if (dishQuery.isNotBlank()) {
            val matches = menuDishes
                .filter { it.name.contains(dishQuery.trim(), ignoreCase = true) }
                .take(4)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                matches.forEach { dish ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(RestaurantEmber.surfaceWell)
                            .clickable {
                                if (lines.none { it.masterId == dish.id }) {
                                    lines += KotLine(
                                        masterId = dish.id,
                                        name = dish.name,
                                        unit = dish.unit?.takeIf { it.isNotBlank() } ?: "plate",
                                        quantity = 1,
                                    )
                                }
                                onDishQuery("")
                            }
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(dish.name, style = RestaurantEmber.rowTitle(), maxLines = 1, modifier = Modifier.weight(1f))
                        Text("+", style = RestaurantEmber.rowValue(RestaurantEmber.ember), maxLines = 1)
                    }
                }
            }
        }
    }

    if (lines.isEmpty()) {
        EmberEmpty("No dish on the ticket", "Pick from the menu above. Each line becomes a real order line.")
    } else {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(RestaurantEmber.chip)
                .padding(horizontal = 12.dp),
        ) {
            lines.forEachIndexed { index, line ->
                KotLineRow(
                    line = line,
                    onDecrement = {
                        if (line.quantity <= 1) lines.removeAt(index)
                        else lines[index] = line.copy(quantity = line.quantity - 1)
                    },
                    onIncrement = { if (line.quantity < 99) lines[index] = line.copy(quantity = line.quantity + 1) },
                    onRemove = { lines.removeAt(index) },
                )
            }
        }
    }

    EmberFieldLabel("Guest / table reference")
    EmberTextField(
        value = guest,
        placeholder = "Walk-in or covers reference",
        onValueChange = onGuest,
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf("Table", "Covers", "KOT no.").forEach { label ->
            Text(
                label,
                style = RestaurantEmber.chipLabel(false).copy(color = RestaurantEmber.textFaint),
                maxLines = 1,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(RestaurantEmber.chip.copy(alpha = 0.5f))
                    .border(1.dp, RestaurantEmber.hairline.copy(alpha = 0.5f), RoundedCornerShape(999.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
    }
    EmberFieldLabel("Kitchen note")
    EmberTextField(
        value = note,
        placeholder = "No onion, extra spice, allergy",
        onValueChange = onNote,
    )
}

@Composable
private fun FireBody(
    tickets: List<OperatingActivityDto>,
    selectedTicket: OperatingActivityDto?,
    selectedId: String?,
    onSelect: (String) -> Unit,
) {
    EmberFieldLabel("Open tickets")
    if (tickets.isEmpty()) {
        EmberEmpty("Nothing in the pass", "Send a ticket first — only a posted order can be fired.")
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        tickets.take(6).forEach { ticket ->
            val status = ticket.status?.uppercase().orEmpty()
            val fired = status == "PREPARING"
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (ticket.id == selectedId) RestaurantEmber.ember.copy(alpha = 0.16f)
                        else RestaurantEmber.surfaceWell,
                    )
                    .border(
                        1.dp,
                        if (ticket.id == selectedId) RestaurantEmber.ember else Color.White.copy(alpha = 0.06f),
                        RoundedCornerShape(12.dp),
                    )
                    .clickable { onSelect(ticket.id) }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        ticket.title.ifBlank { "Kitchen ticket" },
                        style = RestaurantEmber.rowTitle(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        listOfNotNull(
                            ticket.channelName?.takeIf { it.isNotBlank() },
                            ticket.counterpartyName?.takeIf { it.isNotBlank() },
                            RestaurantFormat.shortDay(ticket.occurredAt)?.let { "Sent $it" },
                        ).joinToString(" · ").ifBlank { "Order" },
                        style = RestaurantEmber.rowCaption().copy(fontSize = 10.sp),
                        maxLines = 1,
                    )
                }
                EmberSeverityChip(
                    label = restaurantOrderStatusLabel(ticket.status),
                    color = if (fired) RestaurantEmber.amber else RestaurantEmber.ember,
                )
            }
        }
    }
    if (selectedTicket?.status?.uppercase() == "PREPARING") {
        EmberEmpty(
            "Already firing",
            "This ticket was fired. Nothing to do until the kitchen marks it ready.",
        )
    }
}

private data class KotLine(
    val masterId: String,
    val name: String,
    val unit: String,
    val quantity: Int,
)

@Composable
private fun KotLineRow(
    line: KotLine,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(line.name, style = RestaurantEmber.rowValue().copy(fontSize = 13.sp), maxLines = 1)
            Text(line.unit, style = RestaurantEmber.rowCaption().copy(fontSize = 10.sp), maxLines = 1)
        }
        KotStepper("−", onDecrement)
        Text(line.quantity.toString(), style = RestaurantEmber.rowValue().copy(fontSize = 13.sp), maxLines = 1)
        KotStepper("+", onIncrement)
        Text(
            "✕",
            style = RestaurantEmber.rowCaption(RestaurantEmber.textFaint).copy(fontSize = 11.sp),
            maxLines = 1,
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .clickable(onClick = onRemove)
                .padding(4.dp),
        )
    }
}

@Composable
private fun KotStepper(glyph: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(RestaurantEmber.chip)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(glyph, style = RestaurantEmber.rowValue(RestaurantEmber.textPrimary).copy(fontSize = 15.sp))
    }
}

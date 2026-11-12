package com.example.momentra.ui.shell.business.restaurant

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.api.OperatingActivityDto
import com.example.momentra.data.api.OperatingContractBody
import com.example.momentra.data.api.OperatingProjectionDto
import com.example.momentra.ui.theme.RestaurantEmber

/** Due field border from Figma `2400:30488` (`#c2410c`). */
private val StatusDueBorder = Color(0xFFC2410C)

/**
 * Status / Assignment board — Figma `RST-QA-08 · Status / Assignment` `2400:30488`.
 *
 * Prepare · Ready · Fulfil modes bump kitchen ticket state only. Posts ORDER with
 * `revisesEventId` + `orderStatus` — never SALE, never invents revenue.
 */
internal enum class StatusBoardMode(val label: String) {
    Prepare("Prepare"),
    Ready("Ready"),
    Fulfil("Fulfil"),
}

internal enum class StatusBoardChip(val label: String, val orderStatus: String) {
    Preparing("Preparing", "PREPARING"),
    Ready("Ready", "READY"),
    Served("Served", "COMPLETED"),
}

internal data class StatusLineRow(
    val name: String,
    val detail: String,
    val chip: String,
    val chipPositive: Boolean,
    val assignee: String,
)

/**
 * @param onOpenKot keeps Full KOT Send/Fire/Reprint reachable when Status owns the hub tile.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun RestaurantStatusSheet(
    model: OperatingProjectionDto,
    onDismiss: () -> Unit,
    onSubmit: (RestaurantEventDraft) -> Unit,
    onOpenKot: (() -> Unit)? = null,
) {
    val tickets = remember(model.activity) {
        openOrders(model).sortedByDescending { it.occurredAt }
    }
    var mode by remember { mutableStateOf(StatusBoardMode.Ready) }
    var statusChip by remember { mutableStateOf(StatusBoardChip.Ready) }
    var ticketId by remember { mutableStateOf(tickets.firstOrNull()?.id) }
    var due by remember { mutableStateOf("Today · 7:30 pm") }

    val selected = tickets.firstOrNull { it.id == ticketId } ?: tickets.firstOrNull()
    val station = selected?.let { statusStationLabel(it) }.orEmpty().ifBlank { "Hot pass" }
    val assignee = selected?.let { statusAssigneeLabel(it, model) }.orEmpty().ifBlank { "Floor · unassigned" }
    val kotId = kotDisplayId(selected)
    val tableLine = statusTableLine(selected)
    val kitchenName = listOfNotNull(
        model.title?.takeIf { it.isNotBlank() }?.let { "$it Kitchen" },
        model.partyName?.takeIf { it.isNotBlank() },
    ).joinToString(" · ").ifBlank { "Kitchen · Floor" }
    val subtitle = listOfNotNull(
        tableLine.takeIf { it.isNotBlank() },
        "Ready at $station".takeIf { mode == StatusBoardMode.Ready },
        "Prep at $station".takeIf { mode == StatusBoardMode.Prepare },
        "Serve from $station".takeIf { mode == StatusBoardMode.Fulfil },
        kitchenName,
    ).distinct().joinToString(" · ")
    val posLabel = model.title?.takeIf { it.isNotBlank() }?.let { "$it POS" } ?: "Restaurant POS"
    val nextLabel = statusNextLabel(mode, statusChip)
    val lines = remember(selected, assignee, station) {
        statusBoardLines(selected, assignee, station)
    }
    val postable = selected != null

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = RestaurantEmber.gutter, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        EmberSheetGrabber()
        EmberSheetHeader(
            title = "Status / Assignment",
            subtitle = if (selected != null) subtitle else "Bump prep · ready · fulfil — ops only",
            onClose = onDismiss,
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StatusBoardMode.entries.forEach { option ->
                EmberModeChip(
                    text = option.label,
                    selected = option == mode,
                    onClick = {
                        mode = option
                        statusChip = defaultChipForMode(option)
                    },
                )
            }
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            EmberModeChip(
                text = posLabel,
                selected = true,
                selectedFill = RestaurantEmber.wine,
                selectedInk = RestaurantEmber.textPrimary,
                onClick = null,
            )
            if (tickets.isEmpty()) {
                EmberModeChip(text = "No open KOT", selected = false, onClick = null)
            } else {
                tickets.take(4).forEach { ticket ->
                    EmberModeChip(
                        text = kotDisplayId(ticket),
                        selected = ticket.id == selected?.id,
                        onClick = { ticketId = ticket.id },
                    )
                }
            }
            EmberModeChip(
                text = station,
                selected = false,
                onClick = null,
            )
            if (onOpenKot != null) {
                EmberModeChip(
                    text = "Open KOT ›",
                    selected = false,
                    onClick = onOpenKot,
                )
            }
        }

        if (selected == null) {
            EmberEmpty(
                "No kitchen tickets",
                "Send a KOT first — status bumps never invent a ticket or post a sale.",
            )
            if (onOpenKot != null) {
                EmberModeChip(text = "Send KOT ›", selected = true, onClick = onOpenKot)
            }
        } else {
            StatusTicketHero(
                kotId = kotId,
                detail = listOfNotNull(
                    tableLine.takeIf { it.isNotBlank() },
                    when (mode) {
                        StatusBoardMode.Prepare -> "Prep at $station"
                        StatusBoardMode.Ready -> "Ready at $station"
                        StatusBoardMode.Fulfil -> "Serve from $station"
                    },
                ).joinToString(" · "),
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                lines.forEach { line ->
                    StatusLineCard(line)
                }
            }

            StatusMetaBlock(
                station = station,
                assignee = assignee,
                nextLabel = nextLabel,
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StatusBoardChip.entries.forEach { option ->
                    EmberModeChip(
                        text = option.label,
                        selected = option == statusChip,
                        onClick = {
                            statusChip = option
                            mode = modeForChip(option)
                        },
                    )
                }
            }

            EmberChevronField(
                label = "Due",
                value = due,
                borderColor = StatusDueBorder,
                surfaceColor = RestaurantEmber.card,
                onClick = {
                    due = if (due.contains("7:30")) "Today · 8:00 pm" else "Today · 7:30 pm"
                },
            )
        }

        EmberWhyNote("Operational state only — does not post revenue.", tinted = true)

        EmberSheetFooter(
            label = "Update status",
            enabled = postable,
            caveat = "Saved as draft until posted",
            onClick = {
                val ticket = selected ?: return@EmberSheetFooter
                onSubmit(
                    buildStatusUpdateDraft(
                        ticket = ticket,
                        mode = mode,
                        status = statusChip,
                        station = station,
                        assignee = assignee,
                        due = due,
                    ),
                )
            },
        )
        Spacer(Modifier.height(12.dp))
    }
}

// ---------------------------------------------------------------------------------
// Pure status semantics (unit-testable)
// ---------------------------------------------------------------------------------

internal fun defaultChipForMode(mode: StatusBoardMode): StatusBoardChip = when (mode) {
    StatusBoardMode.Prepare -> StatusBoardChip.Preparing
    StatusBoardMode.Ready -> StatusBoardChip.Ready
    StatusBoardMode.Fulfil -> StatusBoardChip.Served
}

internal fun modeForChip(chip: StatusBoardChip): StatusBoardMode = when (chip) {
    StatusBoardChip.Preparing -> StatusBoardMode.Prepare
    StatusBoardChip.Ready -> StatusBoardMode.Ready
    StatusBoardChip.Served -> StatusBoardMode.Fulfil
}

internal fun statusNextLabel(mode: StatusBoardMode, chip: StatusBoardChip): String = when {
    chip == StatusBoardChip.Served || mode == StatusBoardMode.Fulfil -> "Close ticket"
    chip == StatusBoardChip.Ready || mode == StatusBoardMode.Ready -> "Fulfil / serve"
    else -> "Ready at pass"
}

internal fun statusStationLabel(ticket: OperatingActivityDto): String {
    ticket.locationName?.takeIf { it.isNotBlank() }?.let { return it }
    val match = Regex("""Station:\s*([^·\n]+)""", RegexOption.IGNORE_CASE).find(ticket.note.orEmpty())
    return match?.groupValues?.get(1)?.trim().orEmpty()
}

internal fun statusAssigneeLabel(ticket: OperatingActivityDto, model: OperatingProjectionDto): String {
    val fromNote = Regex("""Assignee:\s*([^·\n]+)""", RegexOption.IGNORE_CASE)
        .find(ticket.note.orEmpty())
        ?.groupValues
        ?.get(1)
        ?.trim()
    if (!fromNote.isNullOrBlank()) return fromNote
    ticket.counterpartyName?.takeIf { it.isNotBlank() }?.let { return it }
    return model.partyName?.takeIf { it.isNotBlank() }.orEmpty()
}

internal fun statusTableLine(ticket: OperatingActivityDto?): String {
    if (ticket == null) return ""
    ticket.tableNumber?.takeIf { it.isNotBlank() }?.let { return "Table $it" }
    val title = ticket.title.trim()
    if (title.startsWith("Table ", ignoreCase = true)) return title
    return title.takeIf { it.isNotBlank() }.orEmpty()
}

internal fun parseFiredLineNames(note: String?): List<String> {
    val match = Regex("""Fired:\s*([^·\n]+)""", RegexOption.IGNORE_CASE).find(note.orEmpty()) ?: return emptyList()
    return match.groupValues[1].split(",").map { it.trim() }.filter { it.isNotBlank() }
}

/**
 * Line cards prefer Fired / Hold markers from the KOT note. When the projection has no
 * per-line payload, show one honest summary row from the ticket — never invent dishes.
 */
internal fun statusBoardLines(
    ticket: OperatingActivityDto?,
    assignee: String,
    station: String,
): List<StatusLineRow> {
    if (ticket == null) return emptyList()
    val cook = assignee.ifBlank { "Floor" }
    val stationLabel = station.ifBlank { "Pass" }
    val fired = parseFiredLineNames(ticket.note)
    val held = parseHeldLineNames(ticket.note)
    val rows = buildList {
        fired.forEach { name ->
            add(
                StatusLineRow(
                    name = name,
                    detail = "$stationLabel · done",
                    chip = "Ready",
                    chipPositive = true,
                    assignee = cook,
                ),
            )
        }
        held.forEach { name ->
            add(
                StatusLineRow(
                    name = name,
                    detail = "$stationLabel · held",
                    chip = "Preparing",
                    chipPositive = false,
                    assignee = cook,
                ),
            )
        }
    }
    if (rows.isNotEmpty()) return rows
    val chip = when (ticket.status?.uppercase()) {
        "READY" -> "Ready"
        "COMPLETED" -> "Served"
        "PREPARING" -> "Preparing"
        else -> "Preparing"
    }
    return listOf(
        StatusLineRow(
            name = ticket.title.ifBlank { kotDisplayId(ticket) },
            detail = listOfNotNull(
                stationLabel.takeIf { it.isNotBlank() },
                restaurantOrderStatusLabel(ticket.status),
            ).joinToString(" · "),
            chip = chip,
            chipPositive = chip == "Ready" || chip == "Served",
            assignee = cook,
        ),
    )
}

internal fun buildStatusUpdateDraft(
    ticket: OperatingActivityDto,
    mode: StatusBoardMode,
    status: StatusBoardChip,
    station: String,
    assignee: String,
    due: String,
): RestaurantEventDraft {
    val composedNote = listOfNotNull(
        station.takeIf { it.isNotBlank() }?.let { "Station: $it" },
        assignee.takeIf { it.isNotBlank() }?.let { "Assignee: $it" },
        "Mode: ${mode.label}",
        "Due: $due",
        ticket.note?.takeIf { it.isNotBlank() },
    ).joinToString(" · ")
    return RestaurantEventDraft(
        eventType = "ORDER",
        title = ticket.title.ifBlank { "Status · ${kotDisplayId(ticket)}" },
        amount = null,
        note = composedNote,
        channelName = ticket.channelName,
        counterpartyName = ticket.counterpartyName ?: assignee.takeIf { it.isNotBlank() },
        paymentStatus = "NONE",
        quantity = ticket.quantity,
        revisesEventId = ticket.id,
        dueAt = null,
        contract = OperatingContractBody(
            orderStatus = status.orderStatus,
            tableNumber = ticket.tableNumber,
            covers = ticket.covers,
            locationName = station.takeIf { it.isNotBlank() } ?: ticket.locationName,
            clientKey = ticket.clientKey,
            staffName = assignee.takeIf { it.isNotBlank() },
            deliveryStatus = mode.label,
        ),
    )
}

// ---------------------------------------------------------------------------------
// UI atoms
// ---------------------------------------------------------------------------------

@Composable
private fun StatusTicketHero(kotId: String, detail: String) {
    val shape = RestaurantEmber.rSheetCta
    Column(
        Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 10.dp,
                shape = shape,
                clip = false,
                ambientColor = RestaurantEmber.ember.copy(alpha = 0.33f),
                spotColor = RestaurantEmber.ember.copy(alpha = 0.33f),
            )
            .clip(shape)
            .background(RestaurantEmber.card)
            .border(1.5.dp, RestaurantEmber.ember, shape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            "Ticket",
            style = RestaurantEmber.rowCaption().copy(fontSize = 12.sp, fontWeight = FontWeight.Medium),
        )
        Text(
            kotId,
            style = RestaurantEmber.screenTitle().copy(fontSize = 32.sp, lineHeight = 38.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            detail,
            style = RestaurantEmber.rowCaption(RestaurantEmber.gold).copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun StatusLineCard(line: StatusLineRow) {
    val shape = RoundedCornerShape(12.dp)
    val chipColor = if (line.chipPositive) RestaurantEmber.positive else RestaurantEmber.amber
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(RestaurantEmber.card)
            .border(1.dp, RestaurantEmber.divider, shape)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                line.name,
                style = RestaurantEmber.rowValue().copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                line.detail,
                style = RestaurantEmber.rowCaption().copy(fontSize = 11.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            EmberSeverityChip(line.chip, chipColor)
            Text(
                line.assignee,
                style = RestaurantEmber.rowCaption(RestaurantEmber.gold).copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun StatusMetaBlock(station: String, assignee: String, nextLabel: String) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(RestaurantEmber.card)
            .border(1.dp, RestaurantEmber.ember.copy(alpha = 0.27f), shape)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StatusMetaRow(label = "Station", value = station, valueColor = RestaurantEmber.textPrimary)
        StatusMetaRow(label = "Assignee", value = assignee, valueColor = RestaurantEmber.textPrimary)
        StatusMetaRow(label = "Next", value = nextLabel, valueColor = RestaurantEmber.gold, boldLabel = true)
    }
}

@Composable
private fun StatusMetaRow(
    label: String,
    value: String,
    valueColor: Color,
    boldLabel: Boolean = false,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            style = RestaurantEmber.rowCaption().copy(
                fontSize = 13.sp,
                fontWeight = if (boldLabel) FontWeight.Bold else FontWeight.Normal,
                color = if (boldLabel) RestaurantEmber.textPrimary else RestaurantEmber.textBody,
            ),
            modifier = Modifier.weight(1f),
        )
        Text(
            value,
            style = RestaurantEmber.rowValue(valueColor).copy(
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

package com.example.momentra.ui.shell.business.restaurant

import android.content.Intent
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.api.OperatingActivityDto
import com.example.momentra.data.api.OperatingContractBody
import com.example.momentra.data.api.OperatingLineBody
import com.example.momentra.data.api.OperatingMasterDto
import com.example.momentra.data.api.OperatingProjectionDto
import com.example.momentra.ui.theme.RestaurantEmber

/**
 * Full KOT ticket — Figma `RST-QA-KOT · Send · In-dining` `2449:29655`,
 * `· Fire · In-dining` `2449:29769`, `· Reprint · Cloud` `2449:29859`.
 *
 * Mode chips: Send · Fire · Reprint. Channel: In-dining · Cloud · Takeaway.
 * Course: Now / Hold on Send; Fire now on Fire; Sent on Reprint.
 *
 * Semantics:
 * - Send → `ORDER` `orderStatus=PLACED` (never SALE). Held lines stay until Fire.
 * - Fire → PATCH same event via `revisesEventId` (held → Now; never SALE).
 * - Reprint → client share / re-render; **same** `clientKey` / KOT id; no new ORDER.
 */
internal enum class KotTab { Send, Fire, Reprint }

internal enum class KotChannel(val label: String) {
    InDining("In-dining"),
    Cloud("Cloud"),
    Takeaway("Takeaway"),
}

internal enum class KotCourse { Now, Hold }

internal const val STATION_ALL = "All stations"
internal const val STATION_HELD = "Held only"
internal const val STATION_CLOUD = "Cloud queue"
internal const val STATION_PACK = "Pack"

internal data class KotLine(
    val masterId: String?,
    val name: String,
    val unit: String,
    val quantity: Int,
    val course: KotCourse = KotCourse.Now,
    val station: String = "",
    val detail: String = "",
)

/**
 * @param initialTab opens Send by default; Fire/Reprint when deep-linked.
 * @param onBillFromKot handoff into POS Bill with the selected in-memory ticket (avoids empty wait for refetch).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun RestaurantKotSheet(
    model: OperatingProjectionDto,
    onDismiss: () -> Unit,
    onSubmit: (RestaurantEventDraft) -> Unit,
    initialTab: KotTab = KotTab.Send,
    onBillFromKot: ((OperatingActivityDto) -> Unit)? = null,
) {
    val context = LocalContext.current
    val stations = remember(model.masters) {
        model.masters.orEmpty()
            .filter { it.kind.uppercase() in setOf("STATION", "KITCHEN_STATION") }
            .sortedBy { it.name.lowercase() }
    }
    val menuDishes = remember(model.masters) {
        model.masters.orEmpty()
            .filter { it.kind.uppercase() in setOf("MENU_DISH", "DISH", "MENU_ITEM") }
            .sortedBy { it.name.lowercase() }
    }
    val tickets = remember(model.activity) {
        openOrders(model).sortedByDescending { it.occurredAt }
    }

    var tab by remember { mutableStateOf(initialTab) }
    var channel by remember { mutableStateOf(KotChannel.InDining) }
    var stationFilter by remember { mutableStateOf(defaultStationFilter(initialTab, KotChannel.InDining)) }
    var ticketId by remember { mutableStateOf<String?>(tickets.firstOrNull()?.id) }
    var dishQuery by remember { mutableStateOf("") }
    var guest by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var tableNumber by remember { mutableStateOf("") }
    var covers by remember { mutableStateOf("") }
    var platform by remember { mutableStateOf("Zomato") }
    var platformOrder by remember { mutableStateOf("") }
    var assignee by remember { mutableStateOf("") }
    var reprintReason by remember { mutableStateOf("Kitchen printer jam") }
    val lines = remember { mutableStateListOf<KotLine>() }

    val selectedTicket = tickets.firstOrNull { it.id == ticketId }
    val fireTarget = selectedTicket?.let { nextFireStatus(it.status) }
    val ticketLines = remember(selectedTicket) { parseKotLinesFromTicket(selectedTicket) }
    val heldNames = remember(selectedTicket) { heldLineNamesForTicket(selectedTicket) }
    val previewKotId = remember { allocateKotClientKey() }

    val title = when (tab) {
        KotTab.Send -> "Send KOT"
        KotTab.Fire -> "Fire course"
        KotTab.Reprint -> "Reprint KOT"
    }
    val subtitle = when (tab) {
        KotTab.Send -> "Kitchen ticket · no revenue"
        KotTab.Fire -> "Release held lines to kitchen"
        KotTab.Reprint -> "Re-print kitchen ticket · no new ticket id"
    }
    val why = when (tab) {
        KotTab.Send ->
            "Send KOT never posts revenue or stock. Held lines stay on ticket until Fire. Recipe deduction excluded in v5."
        KotTab.Fire ->
            "Course fire is operational state. Does not create revenue, collection, or stock movement."
        KotTab.Reprint ->
            "Reprint and cloud ticket sync are operational. Revenue only when bill posts; platform settle via Collection."
    }
    val cta = when (tab) {
        KotTab.Send -> "Send to kitchen"
        KotTab.Fire -> "Fire to kitchen"
        KotTab.Reprint -> "Reprint ticket"
    }
    val caption = when (tab) {
        KotTab.Send -> "KOT is operational only — bill posts revenue later"
        KotTab.Fire -> "Fire updates ticket state only — never posts a sale"
        KotTab.Reprint -> "Reprint keeps same KOT id — never duplicates the order"
    }
    val ctaEnabled = when (tab) {
        KotTab.Send -> lines.isNotEmpty()
        KotTab.Fire -> selectedTicket != null && fireTarget != null
        KotTab.Reprint -> selectedTicket != null
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = RestaurantEmber.gutter, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        EmberSheetGrabber()
        EmberSheetHeader(title = title, subtitle = subtitle, onClose = onDismiss)

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            KotTab.entries.forEach { option ->
                EmberModeChip(
                    text = option.name,
                    selected = option == tab,
                    onClick = {
                        tab = option
                        stationFilter = defaultStationFilter(option, channel)
                    },
                )
            }
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            KotChannel.entries.forEach { option ->
                EmberModeChip(
                    text = option.label,
                    selected = option == channel,
                    selectedFill = RestaurantEmber.wine,
                    onClick = {
                        channel = option
                        if (tab == KotTab.Reprint) {
                            stationFilter = defaultStationFilter(KotTab.Reprint, option)
                        }
                    },
                )
            }
        }

        ContextChipRow(
            tab = tab,
            channel = channel,
            tableNumber = tableNumber,
            covers = covers,
            platform = platform,
            platformOrder = platformOrder,
            selectedTicket = selectedTicket,
        )

        when (tab) {
            KotTab.Send -> KotTicketHero(
                eyebrow = "Ticket",
                kotId = previewKotId,
                headline = when (channel) {
                    KotChannel.InDining ->
                        tableNumber.takeIf { it.isNotBlank() }?.let { "Table $it" } ?: "New ticket"
                    KotChannel.Cloud ->
                        platformOrder.takeIf { it.isNotBlank() } ?: platform
                    KotChannel.Takeaway ->
                        guest.takeIf { it.isNotBlank() } ?: "Pickup"
                },
                detail = sendHeroDetail(channel, covers, stations, stationFilter, lines),
            )
            KotTab.Fire -> KotTicketHero(
                eyebrow = "Ticket",
                kotId = kotDisplayId(selectedTicket),
                headline = selectedTicket?.tableNumber?.takeIf { it.isNotBlank() }?.let { "Fire · Table $it" }
                    ?: selectedTicket?.title?.takeIf { it.isNotBlank() }?.let { "Fire · $it" }
                    ?: "Fire course",
                detail = fireHeroDetail(heldNames, selectedTicket),
            )
            KotTab.Reprint -> KotTicketHero(
                eyebrow = if (channel == KotChannel.Cloud) "Cloud ticket" else "Ticket",
                kotId = kotDisplayId(selectedTicket),
                headline = reprintHeadline(selectedTicket, channel, platformOrder, platform),
                detail = reprintHeroDetail(selectedTicket, channel),
            )
        }

        StationFilterRow(
            tab = tab,
            channel = channel,
            stations = stations,
            selected = stationFilter,
            onSelect = { stationFilter = it },
        )

        when (tab) {
            KotTab.Send -> SendComposer(
                menuDishes = menuDishes,
                stations = stations,
                stationFilter = stationFilter,
                dishQuery = dishQuery,
                onDishQuery = { dishQuery = it },
                lines = lines,
                channel = channel,
                tableNumber = tableNumber,
                onTable = { tableNumber = it },
                covers = covers,
                onCovers = { covers = it },
                guest = guest,
                onGuest = { guest = it },
                note = note,
                onNote = { note = it },
                platform = platform,
                onPlatform = { platform = it },
                platformOrder = platformOrder,
                onPlatformOrder = { platformOrder = it },
                assignee = assignee,
                onAssignee = { assignee = it },
            )
            KotTab.Fire -> FireComposer(
                tickets = tickets,
                selectedTicket = selectedTicket,
                selectedId = ticketId,
                onSelect = { ticketId = it },
                heldNames = heldNames,
                heldLines = ticketLines.filter { it.course == KotCourse.Hold },
                fireTarget = fireTarget,
            )
            KotTab.Reprint -> ReprintComposer(
                tickets = tickets,
                selectedTicket = selectedTicket,
                selectedId = ticketId,
                onSelect = { ticketId = it },
                reprintReason = reprintReason,
                onReason = { reprintReason = it },
                reprintLines = ticketLines,
            )
        }

        when (tab) {
            KotTab.Send -> KotMetaBlock(
                rows = listOf(
                    "Print to" to printTargetLabel(stations, stationFilter),
                    "Assignee" to assignee.ifBlank { "Floor · unassigned" },
                    "Linked bill" to "Open · not posted",
                ),
            )
            KotTab.Fire -> KotMetaBlock(
                rows = listOf(
                    "Already sent" to alreadySentLabel(selectedTicket, heldNames),
                    "Station alert" to (
                        selectedTicket?.locationName?.takeIf { it.isNotBlank() }?.let { "$it buzz" }
                            ?: "Hot pass buzz"
                        ),
                    "Linked bill" to "Still open",
                ),
            )
            KotTab.Reprint -> KotMetaBlock(
                rows = listOf(
                    "Original sent" to listOfNotNull(
                        RestaurantFormat.shortDay(selectedTicket?.occurredAt),
                        selectedTicket?.counterpartyName?.takeIf { it.isNotBlank() },
                    ).joinToString(" · ").ifBlank { "On ticket" },
                    "Reprint reason" to reprintReason.ifBlank { "Kitchen printer jam" },
                    "Settlement" to if (channel == KotChannel.Cloud) {
                        "Platform AR · Collection later"
                    } else {
                        "Bill posts revenue later"
                    },
                ),
            )
        }

        if (onBillFromKot != null && selectedTicket != null && tab != KotTab.Send) {
            EmberModeChip(
                text = "Bill from KOT · POS",
                selected = false,
                onClick = { onBillFromKot(selectedTicket) },
            )
        }

        EmberWhyNote(why, tinted = true)

        EmberSheetFooter(
            label = cta,
            enabled = ctaEnabled,
            caveat = caption,
            onClick = {
                when (tab) {
                    KotTab.Send -> onSubmit(
                        buildKotSendDraft(
                            channel = channel,
                            lines = lines.toList(),
                            stationFilter = stationFilter,
                            stations = stations.map { it.name },
                            tableNumber = tableNumber,
                            covers = covers,
                            guest = guest,
                            note = note,
                            platform = platform,
                            platformOrder = platformOrder,
                            assignee = assignee,
                            clientKey = previewKotId,
                        ),
                    )
                    KotTab.Fire -> {
                        val ticket = selectedTicket ?: return@EmberSheetFooter
                        val next = fireTarget ?: return@EmberSheetFooter
                        onSubmit(
                            buildKotFireDraft(
                                ticket = ticket,
                                nextStatus = next,
                                heldNames = heldNames,
                            ),
                        )
                    }
                    KotTab.Reprint -> {
                        val ticket = selectedTicket ?: return@EmberSheetFooter
                        val body = formatKotReprintShare(
                            ticket = ticket,
                            reason = reprintReason.ifBlank { "Kitchen printer jam" },
                        )
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, body)
                            putExtra(Intent.EXTRA_SUBJECT, "KOT reprint · ${kotDisplayId(ticket)}")
                        }
                        context.startActivity(
                            Intent.createChooser(intent, "Reprint ${kotDisplayId(ticket)}"),
                        )
                        onDismiss()
                    }
                }
            },
        )
        Spacer(Modifier.height(16.dp))
    }
}

// ---------------------------------------------------------------------------------
// Pure KOT semantics (unit-testable)
// ---------------------------------------------------------------------------------

/** PLACED → PREPARING (Fire); PREPARING → READY. Later states are no-ops here. */
internal fun nextFireStatus(status: String?): String? = when (status?.uppercase()) {
    null, "", "PLACED" -> "PREPARING"
    "PREPARING" -> "READY"
    else -> null
}

/** Prefer contract `clientKey` (e.g. KOT-442); never invent a new id for reprint. */
internal fun kotDisplayId(ticket: OperatingActivityDto?): String {
    val key = ticket?.clientKey?.takeIf { it.isNotBlank() }
    if (key != null) return key
    val short = ticket?.id?.take(8)?.uppercase()?.takeIf { it.isNotBlank() }
    return short?.let { "KOT-$it" } ?: "KOT-—"
}

internal fun allocateKotClientKey(): String =
    "KOT-${(System.currentTimeMillis() % 100_000).toString().padStart(3, '0')}"

internal fun parseHeldLineNames(note: String?): List<String> {
    val marker = Regex("""Hold:\s*([^·\n]+)""", RegexOption.IGNORE_CASE)
    val match = marker.find(note.orEmpty()) ?: return emptyList()
    return match.groupValues[1].split(",").map { it.trim() }.filter { it.isNotBlank() }
}

/** Prefer posted ORDER lines; fall back to Hold: note marker for older tickets. */
internal fun parseKotLinesFromTicket(ticket: OperatingActivityDto?): List<KotLine> {
    if (ticket == null) return emptyList()
    val held = parseHeldLineNames(ticket.note).map { it.lowercase() }.toSet()
    val posted = ticket.lines.orEmpty().filter { it.itemName.isNotBlank() }
    if (posted.isNotEmpty()) {
        return posted.map { row ->
            val qty = row.quantity?.toIntOrNull()?.coerceIn(1, 99) ?: 1
            KotLine(
                masterId = row.masterId,
                name = row.itemName,
                unit = row.unit?.takeIf { it.isNotBlank() } ?: ticket.unit ?: "pc",
                quantity = qty,
                course = if (held.contains(row.itemName.trim().lowercase())) KotCourse.Hold else KotCourse.Now,
                station = ticket.locationName.orEmpty(),
                detail = if (held.contains(row.itemName.trim().lowercase())) "Hold until guest ready" else "",
            )
        }
    }
    // Legacy: one synthetic line from title when payload lines were not projected.
    return listOf(
        KotLine(
            masterId = null,
            name = ticket.title.ifBlank { "Kitchen line" },
            unit = ticket.unit ?: "pc",
            quantity = ticket.quantity?.toIntOrNull()?.coerceAtLeast(1) ?: 1,
            course = if (held.isNotEmpty()) KotCourse.Hold else KotCourse.Now,
            station = ticket.locationName.orEmpty().ifBlank { "Hot" },
            detail = ticket.note.orEmpty().take(48),
        ),
    )
}

internal fun heldLineNamesForTicket(ticket: OperatingActivityDto?): List<String> {
    val fromLines = parseKotLinesFromTicket(ticket).filter { it.course == KotCourse.Hold }.map { it.name }
    if (fromLines.isNotEmpty()) return fromLines
    return parseHeldLineNames(ticket?.note)
}

internal fun defaultStationFilter(tab: KotTab, channel: KotChannel): String = when (tab) {
    KotTab.Send -> STATION_ALL
    KotTab.Fire -> STATION_HELD
    KotTab.Reprint -> when (channel) {
        KotChannel.Cloud -> STATION_CLOUD
        KotChannel.Takeaway -> STATION_PACK
        KotChannel.InDining -> STATION_ALL
    }
}

internal fun buildKotSendDraft(
    channel: KotChannel,
    lines: List<KotLine>,
    stationFilter: String,
    stations: List<String>,
    tableNumber: String,
    covers: String,
    guest: String,
    note: String,
    platform: String,
    platformOrder: String,
    assignee: String,
    clientKey: String,
): RestaurantEventDraft {
    val ticketLines = lines.map {
        OperatingLineBody(
            itemName = it.name,
            quantity = it.quantity.toString(),
            unit = it.unit,
            unitPrice = null,
            masterId = it.masterId,
        )
    }
    val held = lines.filter { it.course == KotCourse.Hold }.map { it.name }
    val primaryStation = when (stationFilter) {
        STATION_ALL, STATION_HELD, STATION_CLOUD, STATION_PACK ->
            lines.map { it.station }.firstOrNull { it.isNotBlank() }
                ?: stations.firstOrNull().orEmpty()
        else -> stationFilter
    }
    val composedNote = listOfNotNull(
        primaryStation.takeIf { it.isNotBlank() }?.let { "Station: $it" },
        held.takeIf { it.isNotEmpty() }?.let { "Hold: ${it.joinToString(", ")}" },
        assignee.takeIf { it.isNotBlank() }?.let { "Assignee: $it" },
        note.takeIf { it.isNotBlank() },
    ).joinToString(" · ").takeIf { it.isNotBlank() }

    val table = tableNumber.trim()
    val coverCount = covers.trim()
    val title = when (channel) {
        KotChannel.InDining -> when {
            table.isNotBlank() -> "Table $table"
            guest.isNotBlank() -> "Ticket · $guest"
            else -> "Ticket · ${lines.size} item${if (lines.size == 1) "" else "s"}"
        }
        KotChannel.Cloud -> platformOrder.trim().ifBlank {
            platform.trim().ifBlank { "Cloud ticket" }
        }
        KotChannel.Takeaway -> guest.trim().ifBlank {
            "Pickup · ${lines.size} item${if (lines.size == 1) "" else "s"}"
        }
    }

    return RestaurantEventDraft(
        eventType = "ORDER",
        title = title,
        amount = null,
        note = composedNote,
        channelName = channel.label,
        counterpartyName = when (channel) {
            KotChannel.Cloud -> platform.takeIf { it.isNotBlank() }
            else -> guest.takeIf { it.isNotBlank() } ?: assignee.takeIf { it.isNotBlank() }
        },
        paymentStatus = "NONE",
        quantity = coverCount.takeIf { it.isNotBlank() && channel == KotChannel.InDining },
        contract = OperatingContractBody(
            orderStatus = "PLACED",
            taxMode = "NOT_CONFIGURED",
            locationName = primaryStation.takeIf { it.isNotBlank() },
            tableNumber = table.takeIf { it.isNotBlank() && channel == KotChannel.InDining },
            covers = coverCount.takeIf { it.isNotBlank() && channel == KotChannel.InDining },
            clientKey = clientKey,
            staffName = assignee.takeIf { it.isNotBlank() },
        ),
        lines = ticketLines,
    )
}

internal fun buildKotFireDraft(
    ticket: OperatingActivityDto,
    nextStatus: String,
    heldNames: List<String>,
): RestaurantEventDraft {
    val clearedHold = stripHoldMarker(ticket.note)
    val firedNote = if (heldNames.isNotEmpty()) {
        listOfNotNull(clearedHold, "Fired: ${heldNames.joinToString(", ")}").joinToString(" · ")
    } else {
        clearedHold
    }
    return RestaurantEventDraft(
        eventType = "ORDER",
        title = ticket.title.ifBlank { "Kitchen ticket" },
        amount = ticket.amount,
        note = firedNote,
        channelName = ticket.channelName,
        counterpartyName = ticket.counterpartyName,
        paymentStatus = "NONE",
        quantity = ticket.quantity,
        revisesEventId = ticket.id,
        contract = OperatingContractBody(
            orderStatus = nextStatus,
            tableNumber = ticket.tableNumber,
            covers = ticket.covers,
            locationName = ticket.locationName,
            clientKey = ticket.clientKey,
        ),
    )
}

internal fun stripHoldMarker(note: String?): String? {
    if (note.isNullOrBlank()) return note
    return note
        .replace(Regex("""\s*·\s*Hold:\s*[^·]+""", RegexOption.IGNORE_CASE), "")
        .replace(Regex("""^Hold:\s*[^·]+(\s*·\s*)?""", RegexOption.IGNORE_CASE), "")
        .trim()
        .trim('·', ' ')
        .ifBlank { null }
}

/** Reprint share text — reuses existing KOT id; never allocates a new ticket. */
internal fun formatKotReprintShare(ticket: OperatingActivityDto, reason: String): String {
    val id = kotDisplayId(ticket)
    val lines = parseKotLinesFromTicket(ticket)
    return buildString {
        appendLine("REPRINT · $id")
        appendLine(ticket.title.ifBlank { "Kitchen ticket" })
        listOfNotNull(
            ticket.tableNumber?.takeIf { it.isNotBlank() }?.let { "Table $it" },
            ticket.covers?.takeIf { it.isNotBlank() }?.let { "$it covers" },
            ticket.channelName?.takeIf { it.isNotBlank() },
            ticket.locationName?.takeIf { it.isNotBlank() },
        ).takeIf { it.isNotEmpty() }?.let { appendLine(it.joinToString(" · ")) }
        if (lines.isNotEmpty()) {
            lines.forEach { line ->
                appendLine("${line.quantity}× ${line.name}${line.station.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""}")
            }
        } else {
            ticket.note?.takeIf { it.isNotBlank() }?.let { appendLine(it) }
        }
        appendLine("Reprint reason: $reason")
        appendLine("Same KOT id — never duplicates the order")
        append("No PDF endpoint — client re-render / share only")
    }
}

// ---------------------------------------------------------------------------------
// UI atoms
// ---------------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ContextChipRow(
    tab: KotTab,
    channel: KotChannel,
    tableNumber: String,
    covers: String,
    platform: String,
    platformOrder: String,
    selectedTicket: OperatingActivityDto?,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        when (tab) {
            KotTab.Send -> when (channel) {
                KotChannel.InDining -> {
                    EmberModeChip(
                        text = tableCoverChip(tableNumber, covers),
                        selected = tableNumber.isNotBlank(),
                        selectedFill = RestaurantEmber.wine,
                        onClick = null,
                    )
                    EmberModeChip(text = "Course · Now + Hold", selected = false, onClick = null)
                    EmberModeChip(text = "GST on", selected = false, onClick = null)
                }
                KotChannel.Cloud -> {
                    EmberModeChip(
                        text = platformChip(platform, platformOrder),
                        selected = true,
                        selectedFill = RestaurantEmber.wine,
                        onClick = null,
                    )
                    EmberModeChip(text = "Platform ticket", selected = false, onClick = null)
                    EmberModeChip(text = "GST on", selected = false, onClick = null)
                }
                KotChannel.Takeaway -> {
                    EmberModeChip(
                        text = "Pickup",
                        selected = true,
                        selectedFill = RestaurantEmber.wine,
                        onClick = null,
                    )
                    EmberModeChip(text = "Course · Now + Hold", selected = false, onClick = null)
                    EmberModeChip(text = "GST on", selected = false, onClick = null)
                }
            }
            KotTab.Fire -> {
                EmberModeChip(
                    text = tableCoverChip(
                        selectedTicket?.tableNumber ?: tableNumber,
                        selectedTicket?.covers ?: selectedTicket?.quantity ?: covers,
                    ),
                    selected = true,
                    selectedFill = RestaurantEmber.wine,
                    onClick = null,
                )
                EmberModeChip(text = "Held → Now", selected = false, onClick = null)
                EmberModeChip(text = "GST on", selected = false, onClick = null)
            }
            KotTab.Reprint -> when (channel) {
                KotChannel.Cloud -> {
                    EmberModeChip(
                        text = platformChip(
                            platform,
                            selectedTicket?.title?.takeIf { it.startsWith("ZOM", true) }
                                ?: platformOrder.ifBlank { selectedTicket?.clientKey.orEmpty() },
                        ),
                        selected = true,
                        selectedFill = RestaurantEmber.wine,
                        onClick = null,
                    )
                    EmberModeChip(text = "Platform ticket", selected = false, onClick = null)
                    EmberModeChip(text = "GST on", selected = false, onClick = null)
                }
                else -> {
                    EmberModeChip(
                        text = tableCoverChip(
                            selectedTicket?.tableNumber ?: tableNumber,
                            selectedTicket?.covers ?: selectedTicket?.quantity ?: covers,
                        ),
                        selected = true,
                        selectedFill = RestaurantEmber.wine,
                        onClick = null,
                    )
                    EmberModeChip(text = "Same KOT id", selected = false, onClick = null)
                    EmberModeChip(text = "GST on", selected = false, onClick = null)
                }
            }
        }
    }
}

@Composable
private fun KotTicketHero(
    eyebrow: String,
    kotId: String,
    headline: String,
    detail: String,
) {
    val shape = RestaurantEmber.rSheetCta
    Column(
        Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 10.dp,
                shape = shape,
                clip = false,
                ambientColor = RestaurantEmber.ember.copy(alpha = 0.4f),
                spotColor = RestaurantEmber.ember.copy(alpha = 0.4f),
            )
            .clip(shape)
            .background(RestaurantEmber.chip)
            .border(1.5.dp, RestaurantEmber.ember, shape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                eyebrow,
                style = RestaurantEmber.rowCaption().copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
            )
            Spacer(Modifier.weight(1f))
            Text(
                kotId,
                style = RestaurantEmber.rowValue(RestaurantEmber.gold).copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
        }
        Text(
            headline,
            style = RestaurantEmber.screenTitle().copy(fontSize = 28.sp, lineHeight = 34.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            detail,
            style = RestaurantEmber.rowCaption().copy(fontSize = 12.sp),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StationFilterRow(
    tab: KotTab,
    channel: KotChannel,
    stations: List<OperatingMasterDto>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    val chips = buildList {
        when (tab) {
            KotTab.Send -> {
                add(STATION_ALL)
                stations.take(4).forEach { add(it.name) }
            }
            KotTab.Fire -> {
                add(STATION_HELD)
                stations.take(3).forEach { add(it.name) }
            }
            KotTab.Reprint -> when (channel) {
                KotChannel.Cloud -> {
                    add(STATION_CLOUD)
                    add("Hot")
                    add(STATION_PACK)
                }
                KotChannel.Takeaway -> {
                    add(STATION_PACK)
                    stations.take(2).forEach { add(it.name) }
                }
                KotChannel.InDining -> {
                    add(STATION_ALL)
                    stations.take(3).forEach { add(it.name) }
                }
            }
        }
    }
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        chips.forEach { label ->
            EmberModeChip(
                text = label,
                selected = selected == label,
                onClick = { onSelect(label) },
            )
        }
    }
}

@Composable
private fun KotMetaBlock(rows: List<Pair<String, String>>) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(RestaurantEmber.chip)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        rows.forEach { (label, value) ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    label,
                    style = RestaurantEmber.rowCaption().copy(fontSize = 12.sp, fontWeight = FontWeight.Medium),
                    modifier = Modifier.weight(1f),
                )
                Text(
                    value,
                    style = RestaurantEmber.rowValue().copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun CourseChip(label: String, fill: Color, onClick: (() -> Unit)? = null) {
    val shape = RoundedCornerShape(999.dp)
    Text(
        label,
        style = RestaurantEmber.chipLabel(filled = true).copy(color = RestaurantEmber.inkOnEmber),
        maxLines = 1,
        modifier = Modifier
            .clip(shape)
            .background(fill)
            .border(1.dp, RestaurantEmber.gold, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun KotLineCard(
    name: String,
    quantity: Int,
    detail: String,
    station: String,
    courseLabel: String,
    courseFill: Color,
    holdBorder: Boolean,
    onCourseClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val shape = RoundedCornerShape(14.dp)
    val borderColor = if (holdBorder) {
        RestaurantEmber.amber.copy(alpha = 0.4f)
    } else {
        RestaurantEmber.ember.copy(alpha = 0.3f)
    }
    Column(
        Modifier
            .fillMaxWidth()
            .then(
                if (holdBorder) {
                    Modifier.shadow(
                        elevation = 4.dp,
                        shape = shape,
                        clip = false,
                        ambientColor = RestaurantEmber.amber.copy(alpha = 0.2f),
                        spotColor = RestaurantEmber.amber.copy(alpha = 0.2f),
                    )
                } else {
                    Modifier
                },
            )
            .clip(shape)
            .background(RestaurantEmber.chip)
            .border(1.dp, borderColor, shape)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                name,
                style = RestaurantEmber.rowValue().copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                "×$quantity",
                style = RestaurantEmber.rowValue(RestaurantEmber.gold).copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
            trailing?.invoke()
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (detail.isNotBlank()) {
                Text(
                    detail,
                    style = RestaurantEmber.rowCaption().copy(fontSize = 11.sp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (station.isNotBlank()) {
                EmberModeChip(text = station, selected = false, onClick = null)
            }
            CourseChip(label = courseLabel, fill = courseFill, onClick = onCourseClick)
        }
    }
}

// ---------------------------------------------------------------------------------
// Mode bodies
// ---------------------------------------------------------------------------------

@Composable
private fun SendComposer(
    menuDishes: List<OperatingMasterDto>,
    stations: List<OperatingMasterDto>,
    stationFilter: String,
    dishQuery: String,
    onDishQuery: (String) -> Unit,
    lines: MutableList<KotLine>,
    channel: KotChannel,
    tableNumber: String,
    onTable: (String) -> Unit,
    covers: String,
    onCovers: (String) -> Unit,
    guest: String,
    onGuest: (String) -> Unit,
    note: String,
    onNote: (String) -> Unit,
    platform: String,
    onPlatform: (String) -> Unit,
    platformOrder: String,
    onPlatformOrder: (String) -> Unit,
    assignee: String,
    onAssignee: (String) -> Unit,
) {
    when (channel) {
        KotChannel.InDining -> {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    EmberFieldLabel("Table")
                    EmberTextField(value = tableNumber, placeholder = "12", onValueChange = { onTable(it.take(40)) })
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    EmberFieldLabel("Covers")
                    EmberTextField(
                        value = covers,
                        placeholder = "4",
                        onValueChange = { onCovers(it.filter { ch -> ch.isDigit() }.take(8)) },
                    )
                }
            }
        }
        KotChannel.Cloud -> {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    EmberFieldLabel("Platform")
                    EmberTextField(value = platform, placeholder = "Zomato", onValueChange = onPlatform)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    EmberFieldLabel("Order id")
                    EmberTextField(
                        value = platformOrder,
                        placeholder = "ZOM-90821",
                        onValueChange = onPlatformOrder,
                    )
                }
            }
        }
        KotChannel.Takeaway -> {
            EmberFieldLabel("Guest / pickup")
            EmberTextField(value = guest, placeholder = "Walk-in or guest name", onValueChange = onGuest)
        }
    }

    EmberFieldLabel("Dish")
    if (menuDishes.isEmpty()) {
        EmberEmpty("Menu not set up", "Add dishes under Menu first — a ticket line needs a real dish.")
    } else {
        EmberTextField(value = dishQuery, placeholder = "Search menu", onValueChange = onDishQuery)
        if (dishQuery.isNotBlank()) {
            val matches = menuDishes
                .filter { it.name.contains(dishQuery.trim(), ignoreCase = true) }
                .take(4)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                matches.forEach { dish ->
                    val route = when {
                        stationFilter !in setOf(STATION_ALL, STATION_HELD, STATION_CLOUD, STATION_PACK) ->
                            stationFilter
                        else -> stations.firstOrNull()?.name.orEmpty()
                    }
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
                                        course = KotCourse.Now,
                                        station = route,
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
        EmberEmpty("No dish on the ticket", "Pick from the menu above. Toggle Now / Hold per line.")
    } else {
        val visible = lines.filter { line ->
            when (stationFilter) {
                STATION_ALL, STATION_HELD, STATION_CLOUD, STATION_PACK -> true
                else -> line.station.isBlank() || line.station.equals(stationFilter, ignoreCase = true)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            visible.forEach { line ->
                val realIndex = lines.indexOfFirst {
                    it.masterId == line.masterId && it.name == line.name
                }
                if (realIndex < 0) return@forEach
                KotLineCard(
                    name = line.name,
                    quantity = line.quantity,
                    detail = line.detail.ifBlank {
                        if (line.course == KotCourse.Hold) "Hold until guest ready" else line.unit
                    },
                    station = line.station,
                    courseLabel = if (line.course == KotCourse.Hold) "Hold" else "Now",
                    courseFill = if (line.course == KotCourse.Hold) RestaurantEmber.amber else RestaurantEmber.positive,
                    holdBorder = line.course == KotCourse.Hold,
                    onCourseClick = {
                        lines[realIndex] = line.copy(
                            course = if (line.course == KotCourse.Hold) KotCourse.Now else KotCourse.Hold,
                        )
                    },
                    trailing = {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            KotStepper("−") {
                                if (line.quantity <= 1) lines.removeAt(realIndex)
                                else lines[realIndex] = line.copy(quantity = line.quantity - 1)
                            }
                            KotStepper("+") {
                                if (line.quantity < 99) {
                                    lines[realIndex] = line.copy(quantity = line.quantity + 1)
                                }
                            }
                        }
                    },
                )
            }
        }
    }

    EmberFieldLabel("Assignee")
    EmberTextField(value = assignee, placeholder = "Chef Ama", onValueChange = onAssignee)
    EmberFieldLabel("Kitchen note")
    EmberTextField(
        value = note,
        placeholder = "No onion, extra spice, allergy",
        onValueChange = onNote,
    )
}

@Composable
private fun FireComposer(
    tickets: List<OperatingActivityDto>,
    selectedTicket: OperatingActivityDto?,
    selectedId: String?,
    onSelect: (String) -> Unit,
    heldNames: List<String>,
    heldLines: List<KotLine>,
    fireTarget: String?,
) {
    EmberFieldLabel("Open tickets")
    if (tickets.isEmpty()) {
        EmberEmpty("Nothing in the pass", "Send a ticket first — only a posted order can be fired.")
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        tickets.take(6).forEach { ticket ->
            TicketPickRow(
                ticket = ticket,
                selected = ticket.id == selectedId,
                onSelect = { onSelect(ticket.id) },
            )
        }
    }

    when {
        selectedTicket == null -> Unit
        fireTarget == null -> EmberEmpty(
            "Already advanced",
            "This ticket is ${restaurantOrderStatusLabel(selectedTicket.status)}. Fire only moves Sent → Preparing → Ready.",
        )
        heldNames.isEmpty() -> Text(
            "No held lines on this ticket — Fire still advances ${restaurantOrderStatusLabel(selectedTicket.status)} → ${restaurantOrderStatusLabel(fireTarget)}.",
            style = RestaurantEmber.rowCaption(RestaurantEmber.textFaint).copy(fontSize = 10.sp),
        )
        else -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val rows = heldLines.ifEmpty { heldNames.map { KotLine(masterId = null, name = it, unit = "pc", quantity = 1) } }
            rows.forEach { line ->
                KotLineCard(
                    name = line.name,
                    quantity = line.quantity,
                    detail = line.detail.ifBlank { "Was Hold · fire with mains" },
                    station = line.station.ifBlank { selectedTicket.locationName.orEmpty().ifBlank { "Hot" } },
                    courseLabel = "Fire now",
                    courseFill = RestaurantEmber.amber,
                    holdBorder = true,
                )
            }
        }
    }
}

@Composable
private fun ReprintComposer(
    tickets: List<OperatingActivityDto>,
    selectedTicket: OperatingActivityDto?,
    selectedId: String?,
    onSelect: (String) -> Unit,
    reprintReason: String,
    onReason: (String) -> Unit,
    reprintLines: List<KotLine>,
) {
    EmberFieldLabel("Ticket to reprint")
    if (tickets.isEmpty()) {
        EmberEmpty("No kitchen tickets", "Send a KOT first — reprint never invents a new ticket id.")
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        tickets.take(6).forEach { ticket ->
            TicketPickRow(
                ticket = ticket,
                selected = ticket.id == selectedId,
                onSelect = { onSelect(ticket.id) },
                subtitleOverride = "${kotDisplayId(ticket)} · same id on reprint",
            )
        }
    }

    selectedTicket?.let { ticket ->
        if (reprintLines.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                reprintLines.take(8).forEach { line ->
                    KotLineCard(
                        name = line.name,
                        quantity = line.quantity,
                        detail = line.detail.ifBlank { if (line.course == KotCourse.Hold) "Hold" else "Sent" },
                        station = line.station.ifBlank { ticket.locationName.orEmpty().ifBlank { "Hot" } },
                        courseLabel = if (line.course == KotCourse.Hold) "Hold" else "Sent",
                        courseFill = if (line.course == KotCourse.Hold) RestaurantEmber.amber else RestaurantEmber.positive,
                        holdBorder = line.course == KotCourse.Hold,
                    )
                }
            }
        } else {
            KotLineCard(
                name = ticket.title.ifBlank { "Kitchen ticket" },
                quantity = 1,
                detail = ticket.note?.takeIf { it.isNotBlank() } ?: "Reprint · same KOT id",
                station = ticket.locationName.orEmpty().ifBlank { "Pass" },
                courseLabel = "Sent",
                courseFill = RestaurantEmber.positive,
                holdBorder = false,
            )
        }
    }

    EmberFieldLabel("Reprint reason")
    EmberTextField(
        value = reprintReason,
        placeholder = "Kitchen printer jam",
        onValueChange = onReason,
    )
    Text(
        "No PDF endpoint — share uses the same ${kotDisplayId(selectedTicket)} text ticket.",
        style = RestaurantEmber.rowCaption(RestaurantEmber.textFaint).copy(fontSize = 10.sp),
    )
}

@Composable
private fun TicketPickRow(
    ticket: OperatingActivityDto,
    selected: Boolean,
    onSelect: () -> Unit,
    subtitleOverride: String? = null,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (selected) RestaurantEmber.ember.copy(alpha = 0.16f)
                else RestaurantEmber.surfaceWell,
            )
            .border(
                1.dp,
                if (selected) RestaurantEmber.ember else Color.White.copy(alpha = 0.06f),
                RoundedCornerShape(12.dp),
            )
            .clickable(onClick = onSelect)
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
                subtitleOverride ?: listOfNotNull(
                    kotDisplayId(ticket),
                    ticket.tableNumber?.takeIf { it.isNotBlank() }?.let { "Table $it" },
                    ticket.locationName?.takeIf { it.isNotBlank() },
                    RestaurantFormat.shortDay(ticket.occurredAt)?.let { "Sent $it" },
                ).joinToString(" · "),
                style = RestaurantEmber.rowCaption(
                    if (subtitleOverride != null) RestaurantEmber.gold else RestaurantEmber.textBody,
                ).copy(fontSize = 10.sp),
                maxLines = 1,
            )
        }
        EmberSeverityChip(
            label = restaurantOrderStatusLabel(ticket.status),
            color = when (ticket.status?.uppercase()) {
                "PREPARING" -> RestaurantEmber.amber
                "READY" -> RestaurantEmber.gold
                else -> RestaurantEmber.ember
            },
        )
    }
}

@Composable
private fun KotStepper(glyph: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(RestaurantEmber.surfaceWell)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(glyph, style = RestaurantEmber.rowValue(RestaurantEmber.textPrimary).copy(fontSize = 15.sp))
    }
}

// ---------------------------------------------------------------------------------
// Labels
// ---------------------------------------------------------------------------------

private fun tableCoverChip(table: String?, covers: String?): String {
    val t = table?.takeIf { it.isNotBlank() }
    val c = covers?.takeIf { it.isNotBlank() }
    return when {
        t != null && c != null -> "Table $t · $c covers"
        t != null -> "Table $t"
        c != null -> "$c covers"
        else -> "Table · covers"
    }
}

private fun platformChip(platform: String, orderId: String): String {
    val p = platform.ifBlank { "Cloud" }
    val o = orderId.takeIf { it.isNotBlank() }
    return if (o != null) "$p · $o" else p
}

private fun sendHeroDetail(
    channel: KotChannel,
    covers: String,
    stations: List<OperatingMasterDto>,
    stationFilter: String,
    lines: List<KotLine>,
): String {
    val coverBit = covers.takeIf { it.isNotBlank() }?.let { "$it covers" }
    val stationBit = when (stationFilter) {
        STATION_CLOUD -> "Cloud queue"
        STATION_PACK -> "Pack"
        STATION_ALL, STATION_HELD -> lines.map { it.station }.filter { it.isNotBlank() }.distinct()
            .take(2).joinToString(" + ")
            .ifBlank { stations.take(2).joinToString(" + ") { it.name } }
            .ifBlank { "Pass" }
        else -> stationFilter
    }
    return when (channel) {
        KotChannel.InDining -> listOfNotNull(coverBit, "Dinner", stationBit).joinToString(" · ")
        KotChannel.Cloud -> listOf(
            "${lines.size} item${if (lines.size == 1) "" else "s"}",
            "Pack for rider",
        ).joinToString(" · ")
        KotChannel.Takeaway -> listOf(
            "${lines.size} item${if (lines.size == 1) "" else "s"}",
            "Counter pickup",
        ).joinToString(" · ")
    }
}

private fun fireHeroDetail(heldNames: List<String>, ticket: OperatingActivityDto?): String {
    val heldBit = when (heldNames.size) {
        0 -> "No held lines"
        1 -> "1 held line ready"
        else -> "${heldNames.size} held lines ready"
    }
    val pass = ticket?.locationName?.takeIf { it.isNotBlank() } ?: "Hot pass"
    return "$heldBit · $pass"
}

private fun reprintHeadline(
    ticket: OperatingActivityDto?,
    channel: KotChannel,
    platformOrder: String,
    platform: String,
): String {
    if (ticket == null) return "Pick a ticket"
    return when (channel) {
        KotChannel.Cloud ->
            ticket.title.takeIf { it.isNotBlank() && !it.startsWith("Table", true) }
                ?: platformOrder.ifBlank { platform }
        else ->
            ticket.tableNumber?.takeIf { it.isNotBlank() }?.let { "Table $it" }
                ?: ticket.title.ifBlank { kotDisplayId(ticket) }
    }
}

private fun reprintHeroDetail(ticket: OperatingActivityDto?, channel: KotChannel): String {
    if (ticket == null) return "Select an open KOT"
    return listOfNotNull(
        ticket.channelName?.takeIf { it.isNotBlank() } ?: channel.label,
        "same id",
        ticket.locationName?.takeIf { it.isNotBlank() },
    ).joinToString(" · ")
}

private fun printTargetLabel(stations: List<OperatingMasterDto>, stationFilter: String): String {
    val name = when (stationFilter) {
        STATION_CLOUD -> "Cloud queue"
        STATION_PACK -> "Pack"
        STATION_ALL, STATION_HELD -> stations.firstOrNull()?.name
        else -> stationFilter
    }
    return if (name.isNullOrBlank()) "Kitchen printer · Pass" else "Kitchen printer · $name"
}

private fun alreadySentLabel(ticket: OperatingActivityDto?, heldNames: List<String>): String {
    if (ticket == null) return "—"
    val withoutHold = stripHoldMarker(ticket.note).orEmpty()
    val bits = withoutHold
        .split(" · ")
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .filterNot {
            it.startsWith("Station:", true) ||
                it.startsWith("Assignee:", true) ||
                it.startsWith("Fired:", true)
        }
    if (bits.isNotEmpty()) return bits.joinToString(" · ")
    if (heldNames.isNotEmpty()) return "Mains already on pass"
    return ticket.title.ifBlank { "On pass" }
}

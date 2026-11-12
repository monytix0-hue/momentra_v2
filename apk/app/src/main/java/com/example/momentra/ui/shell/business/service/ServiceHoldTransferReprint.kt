package com.example.momentra.ui.shell.business.service

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.api.OperatingContractBody
import com.example.momentra.data.api.OperatingLineBody
import com.example.momentra.data.api.OperatingProjectionDto
import com.example.momentra.ui.shell.business.shared.ServiceCaptureCopy
import com.example.momentra.ui.theme.PlusJakartaSans
import com.example.momentra.ui.theme.ServicesTeal

/**
 * SVC-QA-HOLD / TRANSFER / REPRINT — Figma `2533:29334`, `2533:29448`, `2533:29538`.
 *
 * Restaurant/KOT-style ticket sheets (not trading Hold/Transfer/Reprint layouts).
 * Mode chip trio switches between the three utilities in-place.
 *
 * Semantics (locked):
 * - Hold / Transfer → no revenue, no new GST
 * - Reprint → same invoice number; never a new charge
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ServiceHoldTransferReprintSheet(
    initial: ServiceUtilityTab,
    model: OperatingProjectionDto,
    onDismiss: () -> Unit,
    onSubmit: (ServiceEventDraft) -> Unit,
    onReprintDone: () -> Unit = onDismiss,
) {
    var tab by remember(initial) { mutableStateOf(initial) }
    val rooms = remember(model.masters) { serviceRoomLabels(model) }
    val staff = remember(model.masters) { serviceStaffLabels(model) }
    val gstOn = model.gstTracking == "YES"
    val gstChip = if (gstOn) "GST on" else "GST off"

    var room by remember(rooms) { mutableStateOf(rooms.firstOrNull().orEmpty()) }
    var category by remember { mutableStateOf("All services") }

    val holdId = remember { "HLD-${(System.currentTimeMillis() % 100).toString().padStart(2, '0')}" }
    val transferId = remember { "TRF-${(System.currentTimeMillis() % 1000).toString().padStart(3, '0')}" }

    val appointments = remember(model.activity) {
        model.activity.filter {
            it.eventType.equals("APPOINTMENT", true) ||
                it.eventType.equals("BOOKING", true)
        }
    }
    val postedCharges = remember(model.activity) {
        model.activity.filter {
            it.eventType.equals("SERVICE", true) ||
                it.eventType.equals("SALE", true)
        }
    }

    var holdClient by remember {
        mutableStateOf(
            appointments.firstOrNull()?.counterpartyName?.takeIf { it.isNotBlank() }
                ?: "Ananya Mehta · walk-in",
        )
    }
    var holdContext by remember {
        mutableStateOf(
            listOfNotNull(
                room.takeIf { it.isNotBlank() },
                staff.firstOrNull(),
                "4:00 pm",
            ).joinToString(" · ").ifBlank { "Chair 2 · Priya · 4:00 pm" },
        )
    }
    val holdLines = remember {
        mutableStateListOf<ServiceTicketLine>().also { it.addAll(demoHoldLines()) }
    }
    val selectedHold = remember { mutableStateListOf<String>().also { list -> holdLines.forEach { list += it.id } } }

    var fromStaff by remember(staff) { mutableStateOf(staff.getOrNull(0) ?: "Meera") }
    var toStaff by remember(staff) { mutableStateOf(staff.getOrNull(1) ?: staff.firstOrNull() ?: "Priya") }
    var toRoom by remember(rooms) { mutableStateOf(rooms.firstOrNull() ?: "Chair 1") }
    val transferLines = remember(appointments) {
        mutableStateListOf<ServiceTicketLine>().also { bucket ->
            if (appointments.isEmpty()) {
                bucket.addAll(demoTransferLines())
            } else {
                appointments.take(4).forEachIndexed { index, row ->
                    bucket += ServiceTicketLine(
                        id = row.id.ifBlank { "xfer-$index" },
                        title = row.title.ifBlank { "Booking" },
                        qtyLabel = "×1",
                        meta = listOfNotNull(
                            row.occurredAt.takeLast(5).takeIf { it.length == 5 },
                            row.counterpartyName?.takeIf { it.isNotBlank() },
                            row.quantity?.takeIf { it.isNotBlank() }?.let { "$it min" },
                        ).joinToString(" · ").ifBlank { row.subtitle.orEmpty() },
                        category = "Hair",
                        badge = "Move",
                        badgeTone = TicketBadgeTone.Cream,
                        selected = true,
                        sourceEventId = row.id.takeIf { it.isNotBlank() },
                    )
                }
            }
        }
    }
    val selectedTransfer = remember {
        mutableStateListOf<String>().also { list -> transferLines.filter { it.selected }.forEach { list += it.id } }
    }

    var reprintSourceId by remember(postedCharges) {
        mutableStateOf(postedCharges.firstOrNull()?.id.orEmpty())
    }
    val reprintRow = postedCharges.firstOrNull { it.id == reprintSourceId } ?: postedCharges.firstOrNull()
    val invoiceNo = remember(reprintRow) {
        reprintRow?.clientKey?.takeIf { it.isNotBlank() }
            ?: reprintRow?.id?.takeIf { it.isNotBlank() }?.let { "BC-INV-${it.takeLast(4).uppercase()}" }
            ?: "BC-INV-2048"
    }
    val reprintLines = remember(reprintRow) {
        if (reprintRow == null) demoReprintLines()
        else listOf(
            ServiceTicketLine(
                id = reprintRow.id.ifBlank { "inv-1" },
                title = reprintRow.title.ifBlank { "Posted charge" },
                qtyLabel = "×${reprintRow.quantity?.takeIf { it.isNotBlank() } ?: "1"}",
                meta = listOfNotNull(
                    reprintRow.amount?.takeIf { it.isNotBlank() }?.let { "₹$it" },
                    if (gstOn) "GST posted" else null,
                ).joinToString(" · ").ifBlank { "Posted · same invoice" },
                category = "Hair",
                badge = "Posted",
                badgeTone = TicketBadgeTone.Positive,
                selected = true,
                sourceEventId = reprintRow.id,
            ),
        )
    }

    val categories = when (tab) {
        ServiceUtilityTab.Hold -> listOf("All services", "Hair", "Skin", "Spa", "Wellness")
        ServiceUtilityTab.Transfer -> listOf("All services", "Hair", "Wellness")
        ServiceUtilityTab.Reprint -> listOf("All services", "Hair", "Spa")
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ServicesTeal.gutter, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        TealSheetGrabber()
        TealSheetHeader(
            title = when (tab) {
                ServiceUtilityTab.Hold -> "Appointment hold"
                ServiceUtilityTab.Transfer -> "Staff / room transfer"
                ServiceUtilityTab.Reprint -> "Reprint invoice"
            },
            subtitle = when (tab) {
                ServiceUtilityTab.Hold -> "Held slot · no revenue, no charge"
                ServiceUtilityTab.Transfer -> "Move a booking to another pro or room · no revenue"
                ServiceUtilityTab.Reprint -> "Re-print posted charge · same invoice no."
            },
            onClose = onDismiss,
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            ServiceUtilityTab.entries.forEach { option ->
                TicketModeChip(
                    text = option.chipLabel,
                    selected = tab == option,
                    accent = TicketChipAccent.Lime,
                    onClick = {
                        tab = option
                        category = "All services"
                    },
                )
            }
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            rooms.forEach { label ->
                TicketModeChip(
                    text = label,
                    selected = room == label,
                    accent = TicketChipAccent.Cream,
                    onClick = {
                        room = label
                        if (tab == ServiceUtilityTab.Hold) {
                            holdContext = listOfNotNull(label, staff.firstOrNull(), "4:00 pm")
                                .joinToString(" · ")
                        }
                        if (tab == ServiceUtilityTab.Transfer) toRoom = label
                    },
                )
            }
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            when (tab) {
                ServiceUtilityTab.Hold -> {
                    TicketModeChip(holdContext, selected = true, accent = TicketChipAccent.Cream)
                    TicketModeChip("Hold 15 min · release or book", selected = false)
                    TicketModeChip(gstChip, selected = false)
                }
                ServiceUtilityTab.Transfer -> {
                    TicketModeChip(
                        "$fromStaff → $toStaff",
                        selected = true,
                        accent = TicketChipAccent.Cream,
                    )
                    TicketModeChip("Same GSTIN", selected = false)
                    TicketModeChip(gstChip, selected = false)
                }
                ServiceUtilityTab.Reprint -> {
                    TicketModeChip(
                        "Invoice $invoiceNo",
                        selected = true,
                        accent = TicketChipAccent.Cream,
                    )
                    TicketModeChip(
                        reprintRow?.let { row ->
                            val stamp = row.occurredAt.takeLast(5).takeIf { it.length == 5 } ?: "posted"
                            val pay = row.paymentStatus?.takeIf { it.isNotBlank() } ?: "UPI"
                            "Posted $stamp · $pay"
                        } ?: "Posted 17:05 · UPI",
                        selected = false,
                    )
                    TicketModeChip(gstChip, selected = false)
                }
            }
        }

        TicketHero(
            eyebrow = when (tab) {
                ServiceUtilityTab.Hold -> "Hold slip"
                ServiceUtilityTab.Transfer -> "Transfer slip"
                ServiceUtilityTab.Reprint -> "Tax invoice"
            },
            code = when (tab) {
                ServiceUtilityTab.Hold -> holdId
                ServiceUtilityTab.Transfer -> transferId
                ServiceUtilityTab.Reprint -> invoiceNo
            },
            title = when (tab) {
                ServiceUtilityTab.Hold -> holdClient
                ServiceUtilityTab.Transfer -> "$fromStaff → $toStaff"
                ServiceUtilityTab.Reprint -> invoiceNo
            },
            subtitle = when (tab) {
                ServiceUtilityTab.Hold -> {
                    val count = selectedHold.size.coerceAtLeast(holdLines.size)
                    listOfNotNull(
                        "$count service${if (count == 1) "" else "s"}",
                        model.title?.takeIf { it.isNotBlank() } ?: "Bandra",
                        room.takeIf { it.isNotBlank() },
                        "4:00 pm",
                    ).joinToString(" · ")
                }
                ServiceUtilityTab.Transfer -> {
                    val fromRoom = rooms.getOrNull(1) ?: "Room 2"
                    val n = selectedTransfer.size.coerceAtLeast(1)
                    "$fromRoom → $toRoom · $n booking${if (n == 1) "" else "s"} today"
                }
                ServiceUtilityTab.Reprint ->
                    if (gstOn) "B2C · place of supply MH · CGST 9% + SGST 9%"
                    else "B2C · posted charge · same invoice no."
            },
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            categories.forEach { label ->
                TicketModeChip(
                    text = label,
                    selected = category == label,
                    accent = TicketChipAccent.Lime,
                    onClick = { category = label },
                )
            }
        }

        when (tab) {
            ServiceUtilityTab.Hold -> {
                holdLines
                    .filter { category == "All services" || it.category.equals(category, true) }
                    .forEach { line ->
                        val on = line.id in selectedHold
                        TicketLineCard(
                            line = line.copy(
                                selected = on,
                                badge = if (on) "Held" else "Skip",
                                badgeTone = when {
                                    !on -> TicketBadgeTone.Idle
                                    line.badgeTone == TicketBadgeTone.Cream -> TicketBadgeTone.Cream
                                    else -> TicketBadgeTone.Positive
                                },
                            ),
                            onToggle = {
                                if (on) selectedHold.remove(line.id) else selectedHold += line.id
                            },
                        )
                    }
                TicketMetaBlock(
                    rows = listOf(
                        "Notify" to "SMS + WhatsApp",
                        "Held by" to (staff.firstOrNull()?.let { "Front desk · $it" } ?: "Front desk · Ravi"),
                        "Linked booking" to "Not booked · draft",
                    ),
                )
            }
            ServiceUtilityTab.Transfer -> {
                if (staff.size >= 2) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        staff.take(4).forEach { name ->
                            TicketModeChip(
                                text = "From $name",
                                selected = fromStaff == name,
                                accent = TicketChipAccent.Cream,
                                onClick = {
                                    fromStaff = name
                                    if (toStaff == name) {
                                        toStaff = staff.firstOrNull { it != name } ?: name
                                    }
                                },
                            )
                        }
                        staff.take(4).forEach { name ->
                            TicketModeChip(
                                text = "To $name",
                                selected = toStaff == name,
                                accent = TicketChipAccent.Cream,
                                onClick = {
                                    toStaff = name
                                    if (fromStaff == name) {
                                        fromStaff = staff.firstOrNull { it != name } ?: name
                                    }
                                },
                            )
                        }
                    }
                }
                transferLines
                    .filter { category == "All services" || it.category.equals(category, true) }
                    .forEach { line ->
                        TicketLineCard(
                            line = line.copy(
                                selected = line.id in selectedTransfer,
                                badge = if (line.id in selectedTransfer) "Move" else "Skip",
                                badgeTone = if (line.id in selectedTransfer) {
                                    TicketBadgeTone.Cream
                                } else {
                                    TicketBadgeTone.Idle
                                },
                            ),
                            onToggle = {
                                if (line.id in selectedTransfer) selectedTransfer.remove(line.id)
                                else selectedTransfer += line.id
                            },
                        )
                    }
                TicketMetaBlock(
                    rows = listOf(
                        "Price" to "Unchanged · senior rate needs approval",
                        "New pro" to "$toStaff · $toRoom",
                        "Revenue impact" to "None · charge posts once on Finalize",
                    ),
                )
            }
            ServiceUtilityTab.Reprint -> {
                if (postedCharges.size > 1) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        postedCharges.take(4).forEach { row ->
                            val label = row.clientKey?.takeIf { it.isNotBlank() }
                                ?: row.title.take(18).ifBlank { row.id.takeLast(6) }
                            TicketModeChip(
                                text = label,
                                selected = reprintSourceId == row.id,
                                accent = TicketChipAccent.Cream,
                                onClick = { reprintSourceId = row.id },
                            )
                        }
                    }
                }
                reprintLines
                    .filter { category == "All services" || it.category.equals(category, true) }
                    .forEach { line -> TicketLineCard(line = line, onToggle = null) }
                TicketMetaBlock(
                    rows = listOf(
                        "Original print" to listOfNotNull(
                            reprintRow?.occurredAt?.takeLast(5)?.takeIf { it.length == 5 } ?: "17:05",
                            staff.firstOrNull() ?: "Ravi",
                            "Front desk",
                        ).joinToString(" · "),
                        "Reprint reason" to "Client asked for email copy",
                        "Payment" to listOfNotNull(
                            when (reprintRow?.paymentStatus?.uppercase()) {
                                "COLLECTED", "PAID", "SETTLED" -> "Settled"
                                else -> "Settled"
                            },
                            reprintRow?.paymentStatus?.takeIf { it.isNotBlank() } ?: "UPI",
                            reprintRow?.amount?.takeIf { it.isNotBlank() }?.let { "₹$it" } ?: "₹5,782",
                        ).joinToString(" · "),
                    ),
                )
            }
        }

        WhyNote(
            body = when (tab) {
                ServiceUtilityTab.Hold -> ServiceCaptureCopy.why("HOLD")
                ServiceUtilityTab.Transfer -> ServiceCaptureCopy.why("TRANSFER")
                ServiceUtilityTab.Reprint -> ServiceCaptureCopy.why("REPRINT")
            },
            tinted = true,
        )

        TealGradientCta(
            label = when (tab) {
                ServiceUtilityTab.Hold -> "Hold slot · $holdId"
                ServiceUtilityTab.Transfer -> {
                    val n = selectedTransfer.size.coerceAtLeast(1)
                    "Confirm transfer · $n booking${if (n == 1) "" else "s"}"
                }
                ServiceUtilityTab.Reprint -> "Reprint · duplicate copy"
            },
            caption = when (tab) {
                ServiceUtilityTab.Hold -> "Draft only — revenue + GST post on Finalize Charge"
                ServiceUtilityTab.Transfer -> "Transfer never posts a charge"
                ServiceUtilityTab.Reprint -> "Same invoice no. — never duplicates revenue"
            },
            enabled = when (tab) {
                ServiceUtilityTab.Hold -> selectedHold.isNotEmpty() && room.isNotBlank()
                ServiceUtilityTab.Transfer -> selectedTransfer.isNotEmpty() &&
                    fromStaff.isNotBlank() &&
                    toStaff.isNotBlank() &&
                    fromStaff != toStaff
                ServiceUtilityTab.Reprint -> invoiceNo.isNotBlank()
            },
            onClick = {
                when (tab) {
                    ServiceUtilityTab.Hold -> {
                        val lines = holdLines.filter { it.id in selectedHold }.map {
                            OperatingLineBody(
                                itemName = it.title,
                                quantity = it.qtyLabel.removePrefix("×").ifBlank { "1" },
                                unit = "session",
                                unitPrice = null,
                            )
                        }
                        onSubmit(
                            ServiceEventDraft(
                                eventType = "APPOINTMENT",
                                title = "Hold · $holdClient",
                                amount = null,
                                note = "Hold reserves a slot only · $holdId",
                                channelName = room.takeIf { it.isNotBlank() },
                                counterpartyName = holdClient.substringBefore(" ·").trim()
                                    .takeIf { it.isNotBlank() },
                                paymentStatus = "NONE",
                                contract = OperatingContractBody(
                                    holdStatus = "HELD",
                                    holdReason = "Hold 15 min · release or book",
                                    appointmentAction = null,
                                    staffName = staff.firstOrNull(),
                                    locationName = room.takeIf { it.isNotBlank() },
                                    clientKey = holdId,
                                    estimatedAmount = null,
                                    taxMode = "NOT_CONFIGURED",
                                ),
                                lines = lines.ifEmpty { null },
                            ),
                        )
                    }
                    ServiceUtilityTab.Transfer -> {
                        val moving = transferLines.filter { it.id in selectedTransfer }
                        val parent = moving.firstOrNull()?.sourceEventId
                        onSubmit(
                            ServiceEventDraft(
                                eventType = "APPOINTMENT",
                                title = "Transfer · $fromStaff → $toStaff",
                                amount = null,
                                note = "Staff/room transfer · $transferId · no revenue",
                                channelName = toRoom.takeIf { it.isNotBlank() },
                                counterpartyName = null,
                                paymentStatus = "NONE",
                                parentEventId = parent,
                                revisesEventId = parent,
                                contract = OperatingContractBody(
                                    appointmentAction = null,
                                    staffName = toStaff,
                                    locationName = toRoom.takeIf { it.isNotBlank() },
                                    clientKey = transferId,
                                    holdStatus = null,
                                    taxMode = "NOT_CONFIGURED",
                                ),
                                lines = moving.map {
                                    OperatingLineBody(
                                        itemName = it.title,
                                        quantity = "1",
                                        unit = "booking",
                                        unitPrice = null,
                                    )
                                }.ifEmpty { null },
                            ),
                        )
                    }
                    ServiceUtilityTab.Reprint -> {
                        // Read-only: same invoice number — never posts a new SERVICE charge.
                        onReprintDone()
                    }
                }
            },
        )
        Spacer(Modifier.height(16.dp))
    }
}

internal enum class ServiceUtilityTab(val chipLabel: String) {
    Hold("Hold"),
    Transfer("Transfer"),
    Reprint("Reprint"),
}

internal fun ServiceCaptureSheet.toUtilityTab(): ServiceUtilityTab? = when (this) {
    ServiceCaptureSheet.HOLD -> ServiceUtilityTab.Hold
    ServiceCaptureSheet.TRANSFER -> ServiceUtilityTab.Transfer
    ServiceCaptureSheet.REPRINT -> ServiceUtilityTab.Reprint
    else -> null
}

private enum class TicketChipAccent { Lime, Cream, Idle }
private enum class TicketBadgeTone { Positive, Cream, Idle }

private data class ServiceTicketLine(
    val id: String,
    val title: String,
    val qtyLabel: String,
    val meta: String,
    val category: String,
    val badge: String,
    val badgeTone: TicketBadgeTone,
    val selected: Boolean = true,
    val sourceEventId: String? = null,
)

@Composable
private fun TicketHero(
    eyebrow: String,
    code: String,
    title: String,
    subtitle: String,
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 10.dp,
                shape = shape,
                clip = false,
                ambientColor = ServicesTeal.lime.copy(alpha = 0.35f),
                spotColor = ServicesTeal.lime.copy(alpha = 0.35f),
            )
            .clip(shape)
            .background(ServicesTeal.chip)
            .border(1.5.dp, ServicesTeal.lime, shape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                eyebrow,
                style = ServicesTeal.rowCaption(ServicesTeal.grab).copy(fontSize = 11.sp),
                maxLines = 1,
            )
            Spacer(Modifier.weight(1f))
            Text(
                code,
                style = ServicesTeal.rowCaption(ServicesTeal.whyBody).copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                ),
                maxLines = 1,
            )
        }
        Text(
            title,
            style = ServicesTeal.heroTitle().copy(fontSize = 28.sp, lineHeight = 34.sp),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            subtitle,
            style = ServicesTeal.rowCaption(ServicesTeal.grab).copy(fontSize = 12.sp),
            maxLines = 2,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TicketLineCard(
    line: ServiceTicketLine,
    onToggle: (() -> Unit)?,
) {
    val highlight = line.badgeTone == TicketBadgeTone.Cream && line.selected
    val shape = RoundedCornerShape(14.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .then(
                if (highlight) {
                    Modifier.shadow(
                        elevation = 6.dp,
                        shape = shape,
                        clip = false,
                        ambientColor = ServicesTeal.amber.copy(alpha = 0.2f),
                        spotColor = ServicesTeal.amber.copy(alpha = 0.2f),
                    )
                } else {
                    Modifier
                },
            )
            .clip(shape)
            .background(ServicesTeal.chip)
            .border(
                1.dp,
                if (highlight) ServicesTeal.whyBody.copy(alpha = 0.4f)
                else ServicesTeal.lime.copy(alpha = 0.3f),
                shape,
            )
            .then(if (onToggle != null) Modifier.clickable(onClick = onToggle) else Modifier)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                line.title,
                style = ServicesTeal.rowTitle().copy(fontSize = 14.sp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                line.qtyLabel,
                style = ServicesTeal.rowCaption(ServicesTeal.whyBody).copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                ),
                maxLines = 1,
            )
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                line.meta,
                style = ServicesTeal.rowCaption(ServicesTeal.grab).copy(fontSize = 11.sp),
                maxLines = 2,
            )
            TicketModeChip(line.category, selected = false)
            StatusBadge(line.badge, line.badgeTone)
        }
    }
}

@Composable
private fun TicketMetaBlock(rows: List<Pair<String, String>>) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ServicesTeal.chip)
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
                    style = ServicesTeal.rowCaption(ServicesTeal.grab).copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                    ),
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                )
                Text(
                    value,
                    style = TextStyleTicketMeta(),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun TextStyleTicketMeta() = androidx.compose.ui.text.TextStyle(
    fontFamily = PlusJakartaSans,
    fontWeight = FontWeight.Bold,
    fontSize = 13.sp,
    color = ServicesTeal.textPrimary,
)

@Composable
private fun TicketModeChip(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    accent: TicketChipAccent = TicketChipAccent.Idle,
    onClick: (() -> Unit)? = null,
) {
    val fill = when {
        selected && accent == TicketChipAccent.Lime -> ServicesTeal.lime
        else -> ServicesTeal.chip
    }
    val border = when {
        selected && accent == TicketChipAccent.Lime -> ServicesTeal.lime
        selected && accent == TicketChipAccent.Cream -> ServicesTeal.amber
        else -> ServicesTeal.chipBorder
    }
    val ink = when {
        selected && accent == TicketChipAccent.Lime -> ServicesTeal.inkOnTeal
        selected && accent == TicketChipAccent.Cream -> ServicesTeal.inkOnTeal
        else -> ServicesTeal.grab
    }
    val click = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Text(
        text,
        style = ServicesTeal.chipLabel(selected).copy(
            color = ink,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 12.sp,
        ),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .then(
                if (selected) {
                    Modifier.shadow(
                        elevation = 6.dp,
                        shape = ServicesTeal.rPill,
                        clip = false,
                        ambientColor = ServicesTeal.lime.copy(alpha = 0.35f),
                        spotColor = ServicesTeal.lime.copy(alpha = 0.35f),
                    )
                } else {
                    Modifier
                },
            )
            .clip(ServicesTeal.rPill)
            .background(fill)
            .border(1.dp, border, ServicesTeal.rPill)
            .then(click)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

@Composable
private fun StatusBadge(label: String, tone: TicketBadgeTone) {
    val fill = when (tone) {
        TicketBadgeTone.Positive -> ServicesTeal.positive
        TicketBadgeTone.Cream -> ServicesTeal.whyBody
        TicketBadgeTone.Idle -> ServicesTeal.chip
    }
    val border = when (tone) {
        TicketBadgeTone.Idle -> ServicesTeal.teal
        else -> ServicesTeal.whyBody
    }
    val ink = when (tone) {
        TicketBadgeTone.Idle -> ServicesTeal.grab
        else -> ServicesTeal.inkOnTeal
    }
    Box(
        Modifier
            .clip(ServicesTeal.rPill)
            .background(fill)
            .border(1.dp, border, ServicesTeal.rPill)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            label,
            color = ink,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            maxLines = 1,
        )
    }
}

private fun serviceRoomLabels(model: OperatingProjectionDto): List<String> {
    val fromMasters = model.masters.orEmpty()
        .filter { it.kind.uppercase() in setOf("ROOM", "CHAIR", "LOCATION", "CHANNEL") }
        .map { it.name.trim() }
        .filter { it.isNotBlank() }
        .distinct()
    val fromChannels = model.life?.channels.orEmpty().filter { it.isNotBlank() }
    return (fromMasters + fromChannels)
        .distinct()
        .ifEmpty { listOf("Chair 1", "Chair 2", "Spa room") }
}

private fun serviceStaffLabels(model: OperatingProjectionDto): List<String> {
    return model.masters.orEmpty()
        .filter { it.kind.uppercase() in setOf("STAFF", "EMPLOYEE", "THERAPIST") }
        .map { it.name.trim() }
        .filter { it.isNotBlank() }
        .distinct()
        .ifEmpty { listOf("Meera", "Priya", "Ravi") }
}

private fun demoHoldLines() = listOf(
    ServiceTicketLine(
        id = "hold-1",
        title = "Haircut & style",
        qtyLabel = "×1",
        meta = "45 min · ₹899 · SAC 999721 · 18%",
        category = "Hair",
        badge = "Held",
        badgeTone = TicketBadgeTone.Positive,
    ),
    ServiceTicketLine(
        id = "hold-2",
        title = "Hair spa",
        qtyLabel = "×2",
        meta = "30 min · ₹1,200 · SAC 999721 · 18%",
        category = "Hair",
        badge = "Held",
        badgeTone = TicketBadgeTone.Positive,
    ),
    ServiceTicketLine(
        id = "hold-3",
        title = "Add-on: head massage",
        qtyLabel = "×1",
        meta = "15 min · ₹400 · SAC 999722 · 18%",
        category = "Skin",
        badge = "Held",
        badgeTone = TicketBadgeTone.Cream,
    ),
    ServiceTicketLine(
        id = "hold-4",
        title = "Package session 2/3",
        qtyLabel = "×2",
        meta = "Keratin pkg · prepaid · no new revenue",
        category = "Wellness",
        badge = "Held",
        badgeTone = TicketBadgeTone.Positive,
    ),
)

private fun demoTransferLines() = listOf(
    ServiceTicketLine(
        id = "xfer-1",
        title = "S-120 Signature facial",
        qtyLabel = "×1",
        meta = "4:00 pm · Ananya Mehta · 60 min",
        category = "Hair",
        badge = "Move",
        badgeTone = TicketBadgeTone.Cream,
        sourceEventId = null,
    ),
    ServiceTicketLine(
        id = "xfer-2",
        title = "S-124 Haircut & style",
        qtyLabel = "×1",
        meta = "5:30 pm · Rohan Kapoor · 45 min",
        category = "Hair",
        badge = "Move",
        badgeTone = TicketBadgeTone.Cream,
        sourceEventId = null,
    ),
)

private fun demoReprintLines() = listOf(
    ServiceTicketLine(
        id = "inv-1",
        title = "Signature facial",
        qtyLabel = "×1",
        meta = "SAC 999721 · ₹4,500 · GST 18%",
        category = "Hair",
        badge = "Posted",
        badgeTone = TicketBadgeTone.Positive,
    ),
    ServiceTicketLine(
        id = "inv-2",
        title = "Head massage add-on",
        qtyLabel = "×1",
        meta = "SAC 999722 · ₹400 · GST 18%",
        category = "Spa",
        badge = "Posted",
        badgeTone = TicketBadgeTone.Positive,
    ),
)

package com.example.momentra.ui.shell.business.service

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.momentra.data.api.OperatingProjectionDto
import com.example.momentra.ui.shell.business.shared.OperatingCaptureDraft
import com.example.momentra.ui.shell.business.shared.OperatingCaptureSheet
import com.example.momentra.ui.shell.business.shared.OperatingChromeState
import com.example.momentra.ui.shell.business.shared.OperatingSheetScrim
import com.example.momentra.ui.theme.ServicesTeal

/**
 * Quick Add hub — Figma RTL-HUB-v5 `2399:29887`.
 *
 * 9 core tiles (3 × 3) + 5 support tiles (3 + 2 wide). Tile order, labels and
 * captions match the frame; a tile the role cannot submit renders locked instead of
 * disappearing, so the grid keeps its shape.
 */
@Composable
fun ServiceQuickAddHub(
    model: OperatingProjectionDto,
    onRecord: suspend (ServiceEventDraft) -> Unit,
    onDecide: suspend (String, String) -> Unit,
    momentId: String? = null,
    onDismiss: () -> Unit = {},
) {
    val allowed = model.quickAdds.map { it.code }.toSet()
    val scope = rememberCoroutineScope()
    var sheet by remember { mutableStateOf<ServiceCaptureSheet?>(null) }
    var utilityTab by remember { mutableStateOf<ServiceUtilityTab?>(null) }
    var mastersOpen by remember { mutableStateOf<ServiceMastersTab?>(null) }
    var approvalOpen by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var savedToast by remember { mutableStateOf(false) }

    LaunchedEffect(OperatingChromeState.quickAddHint) {
        val hint = OperatingChromeState.quickAddHint?.uppercase() ?: return@LaunchedEffect
        when (hint) {
            "HOLD" -> {
                utilityTab = ServiceUtilityTab.Hold
                sheet = null
            }
            "TRANSFER" -> {
                utilityTab = ServiceUtilityTab.Transfer
                sheet = null
            }
            "REPRINT" -> {
                utilityTab = ServiceUtilityTab.Reprint
                sheet = null
            }
            else -> {
                sheet = ServiceCaptureSheet.entries.firstOrNull {
                    it.eventType.equals(hint, ignoreCase = true) ||
                        it.mode.equals(hint, ignoreCase = true) ||
                        it.name == hint
                } ?: when (hint) {
                    "SERVICE", "SALE", "CHARGE" -> ServiceCaptureSheet.SERVICE
                    "APPOINTMENT", "BOOKING" -> ServiceCaptureSheet.APPOINTMENT
                    "PURCHASE" -> ServiceCaptureSheet.PURCHASE
                    "COLLECTION" -> ServiceCaptureSheet.COLLECTION
                    "PAYMENT" -> ServiceCaptureSheet.PAYMENT
                    "SPEND", "EXPENSE" -> ServiceCaptureSheet.SPEND
                    "STOCK" -> ServiceCaptureSheet.STOCK
                    "BUDGET", "TARGET" -> ServiceCaptureSheet.BUDGET
                    "STATUS" -> ServiceCaptureSheet.STATUS
                    "ISSUE", "UPDATE" -> ServiceCaptureSheet.ISSUE
                    else -> null
                }
            }
        }
        OperatingChromeState.quickAddHint = null
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ServicesTeal.gutter, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(ServicesTeal.sectionGap),
    ) {
        ScreenHeader(
            title = "Quick Adds",
            roleLine = "${model.title?.takeIf { it.isNotBlank() } ?: "Consumer Service"} · ${model.subtypeLabel ?: "Service"}",
        )
        ContextChips(
            subtypeLabel = model.subtypeLabel ?: "Consumer Service",
            periodLine = ServiceFormat.periodLine(model.startDate, model.currency),
            gstOn = model.gstTracking == "YES",
        )

        QuickAddHero(hasService = "SERVICE" in allowed, onRecord = { sheet = ServiceCaptureSheet.SERVICE })

        GridSectionHeader("Core · 9", "Services journey")
        QuickAddGrid(
            tiles = ServiceCoreTiles,
            allowed = allowed,
            onSheet = { sheet = it },
            onMasters = { mastersOpen = it },
            onApproval = { approvalOpen = true },
        )

        GridSectionHeader("Support · 5", "Clients · masters")
        QuickAddGrid(
            tiles = ServiceSupportTiles,
            allowed = allowed,
            onSheet = { sheet = it },
            onMasters = { mastersOpen = it },
            onApproval = { approvalOpen = true },
        )

        // Hold / Transfer / Reprint are not on SVC-HUB-v5 `2401:29887` but remain required
        // SVC screens. Keep a compact Utilities row for discovery (same decision as iOS
        // ServiceHubScreen) instead of folding into Support · 5 or Pulse-only deep-links.
        GridSectionHeader("Utilities · 3", "Hold · move · reprint")
        QuickAddGrid(
            tiles = ServiceUtilityTiles,
            allowed = allowed,
            onSheet = { open ->
                open.toUtilityTab()?.let { utilityTab = it } ?: run { sheet = open }
            },
            onMasters = { mastersOpen = it },
            onApproval = { approvalOpen = true },
        )

        WhyNote(
            "Boundaries: book→assign→perform→finalize charge→collect. Appointment state ≠ financial state. Empty = Unavailable.",
            tinted = true,
        )

        if (error != null) {
            Text(
                error ?: "",
                style = ServicesTeal.rowCaption(ServicesTeal.negative),
            )
        }
        Spacer(Modifier.height(16.dp))
    }

    sheet?.let { open ->
        OperatingSheetScrim(
            onDismiss = { sheet = null },
            accent = ServicesTeal.teal,
            surface = ServicesTeal.card,
            border = ServicesTeal.sheetBorder,
            topCorner = 28.dp,
        ) {
            ServiceMoneyCaptureSheet(
                sheet = open,
                model = model,
                onDismiss = { sheet = null },
                onSubmit = { draft ->
                    scope.launch {
                        runCatching { onRecord(draft) }
                            .onSuccess {
                                savedToast = true
                                sheet = null
                            }
                            .onFailure { error = it.message }
                    }
                },
            )
        }
    }

    utilityTab?.let { open ->
        OperatingSheetScrim(
            onDismiss = { utilityTab = null },
            accent = ServicesTeal.lime,
            surface = ServicesTeal.card,
            border = ServicesTeal.sheetBorder,
        ) {
            ServiceHoldTransferReprintSheet(
                initial = open,
                model = model,
                onDismiss = { utilityTab = null },
                onSubmit = { draft ->
                    scope.launch {
                        runCatching { onRecord(draft) }
                            .onSuccess {
                                savedToast = true
                                utilityTab = null
                            }
                            .onFailure { error = it.message }
                    }
                },
                onReprintDone = {
                    savedToast = true
                    utilityTab = null
                },
            )
        }
    }
    if (savedToast) {
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(1400)
            savedToast = false
        }
        TealSavedToast(Modifier.padding(horizontal = ServicesTeal.gutter, vertical = 8.dp))
    }

    mastersOpen?.let { open ->
        OperatingSheetScrim(
            onDismiss = { mastersOpen = null },
            accent = ServicesTeal.lime,
            surface = ServicesTeal.card,
            border = ServicesTeal.sheetBorder,
        ) {
            ServiceMastersSheet(
                tab = open,
                model = model,
                momentId = momentId,
                onClose = { mastersOpen = null },
                onRecord = onRecord,
                onError = { error = it },
            )
        }
    }

    if (approvalOpen) {
        OperatingSheetScrim(onDismiss = { approvalOpen = false }, accent = ServicesTeal.teal) {
            OperatingCaptureSheet(
                title = "Review Approval",
                icon = ServiceTileIcons.status,
                eventType = "APPROVAL",
                mode = "APPROVAL",
                model = model,
                variant = "service",
                onDismiss = { approvalOpen = false },
                onSubmit = {},
                onDecide = onDecide,
            )
        }
    }
}

/** Teal→lime hero with the cream `+ Finalize Charge` pill (SVC-HUB-v5 `2401:29887`). */
@Composable
private fun QuickAddHero(hasService: Boolean, onRecord: () -> Unit) {
    val shape = ServicesTeal.rHero
    Box(
        Modifier
            .fillMaxWidth()
            .shadow(12.dp, shape, clip = false, ambientColor = ServicesTeal.lime.copy(alpha = 0.3f), spotColor = ServicesTeal.lime.copy(alpha = 0.3f))
            .shadow(24.dp, shape, clip = false, ambientColor = ServicesTeal.teal.copy(alpha = 0.5f), spotColor = ServicesTeal.teal.copy(alpha = 0.5f))
            .clip(shape)
            .background(Brush.linearGradient(listOf(ServicesTeal.teal, ServicesTeal.lime)))
            .padding(20.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Text("CONSUMER SERVICES", style = ServicesTeal.heroEyebrow(), maxLines = 1)
            Text("Book. Deliver. Charge.", style = ServicesTeal.heroHeadline(), maxLines = 1)
            Text(
                "Booking and completion are not revenue. Finalized charge posts once. Deposits stay distinct until allocated.",
                style = ServicesTeal.heroBody(),
                maxLines = 3,
            )
            if (hasService) {
                Spacer(Modifier.height(4.dp))
                TealCta("+  Finalize Charge", onClick = onRecord)
            }
        }
    }
}

private data class QuickTile(
    val label: String,
    val caption: String,
    val icon: Int,
    val sheet: ServiceCaptureSheet? = null,
    val requires: String? = null,
    val wide: Boolean = false,
    val masterTab: ServiceMastersTab? = null,
    val approval: Boolean = false,
)

private val ServiceCoreTiles = listOf(
    QuickTile("Charge", "Finalize / walk-in", ServiceTileIcons.sale, ServiceCaptureSheet.SERVICE, requires = "SERVICE"),
    QuickTile("Purchase", "Supplies · receive", ServiceTileIcons.purchase, ServiceCaptureSheet.PURCHASE, requires = "PURCHASE"),
    QuickTile("Collection", "Charge AR", ServiceTileIcons.collection, ServiceCaptureSheet.COLLECTION, requires = "COLLECTION"),
    QuickTile("Payment", "Settle AP", ServiceTileIcons.payment, ServiceCaptureSheet.PAYMENT, requires = "PAYMENT"),
    QuickTile("Expense", "Service running", ServiceTileIcons.expense, ServiceCaptureSheet.SPEND, requires = "SPEND"),
    QuickTile("Stock", "Supply count", ServiceTileIcons.stock, ServiceCaptureSheet.STOCK, requires = "STOCK"),
    QuickTile("Booking", "Appoint / walk-in", ServiceTileIcons.order, ServiceCaptureSheet.APPOINTMENT, requires = "APPOINTMENT"),
    QuickTile("Status", "Start · complete", ServiceTileIcons.status, ServiceCaptureSheet.STATUS, requires = "APPOINTMENT"),
    QuickTile("Budget", "Versioned revise", ServiceTileIcons.budget, ServiceCaptureSheet.BUDGET, requires = "BUDGET"),
)

private val ServiceSupportTiles = listOf(
    QuickTile("Clients", "Guests · patients", ServiceTileIcons.suppliers, masterTab = ServiceMastersTab.Clients, wide = false),
    QuickTile("Services", "Offerings · packages", ServiceTileIcons.catalogue, masterTab = ServiceMastersTab.Services, wide = false),
    QuickTile("Rooms", "Rooms · chairs · staff", ServiceTileIcons.locations, masterTab = ServiceMastersTab.RoomsStaff, wide = false),
    QuickTile("Invite & QR", "Join · people · roles", ServiceTileIcons.invite, masterTab = ServiceMastersTab.Invite, wide = true),
    QuickTile("Issue", "Update · approval", ServiceTileIcons.issue, ServiceCaptureSheet.ISSUE, requires = "UPDATE", wide = true),
)

private val ServiceUtilityTiles = listOf(
    QuickTile("Hold", "Park booking", ServiceTileIcons.status, ServiceCaptureSheet.HOLD, requires = "APPOINTMENT"),
    QuickTile("Transfer", "Between rooms", ServiceTileIcons.collection, ServiceCaptureSheet.TRANSFER, requires = "APPOINTMENT"),
    QuickTile("Reprint", "Receipt · bill", ServiceTileIcons.issue, ServiceCaptureSheet.REPRINT, requires = "SERVICE"),
)

/** 3-column grid; `wide` tiles span the remaining width in their row. */
@Composable
private fun QuickAddGrid(
    tiles: List<QuickTile>,
    allowed: Set<String>,
    onSheet: (ServiceCaptureSheet) -> Unit,
    onMasters: (ServiceMastersTab) -> Unit,
    onApproval: () -> Unit,
) {
    val columns = 3
    val rows = buildList {
        var row = mutableListOf<QuickTile>()
        tiles.forEach { tile ->
            if (tile.wide) {
                if (row.isNotEmpty()) { add(row.toList()); row = mutableListOf() }
                val last = lastOrNull()
                if (last != null && last.firstOrNull()?.wide == true && last.size < 2) {
                    removeAt(lastIndex)
                    add(last + tile)
                } else {
                    add(listOf(tile))
                }
            } else {
                row += tile
                if (row.size == columns) { add(row.toList()); row = mutableListOf() }
            }
        }
        if (row.isNotEmpty()) add(row.toList())
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { cells ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                cells.forEachIndexed { index, tile ->
                    val locked = when {
                        tile.masterTab != null -> null
                        tile.approval -> null
                        tile.requires == null -> null
                        tile.requires !in allowed -> "Role locked"
                        else -> null
                    }
                    val weight = when {
                        cells.size == 1 -> 1f
                        cells.all { it.wide } -> 1f
                        cells.size < columns && index == cells.lastIndex -> (columns - cells.size + 1).toFloat()
                        else -> 1f
                    }
                    QuickAddTile(
                        iconRes = tile.icon,
                        label = tile.label,
                        caption = tile.caption,
                        locked = locked,
                        modifier = Modifier.weight(weight),
                        onClick = {
                            when {
                                tile.masterTab != null -> onMasters(tile.masterTab)
                                tile.approval -> onApproval()
                                tile.sheet != null -> onSheet(tile.sheet)
                            }
                        },
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------
// Draft contract (matches the pre-existing `POST …/operating-events` body)
// ---------------------------------------------------------------------------------

/**
 * Retail-side capture draft. Field-for-field the same shape the shared
 * [OperatingCaptureSheet] produces, carried across the retail module boundary.
 */
data class ServiceEventDraft(
    val eventType: String,
    val title: String,
    val amount: String?,
    val note: String?,
    val channelName: String?,
    val counterpartyName: String?,
    val paymentStatus: String?,
    val occurredAt: String? = null,
    val quantity: String? = null,
    val unit: String? = null,
    val unitPrice: String? = null,
    val itemName: String? = null,
    val parentEventId: String? = null,
    val revisesEventId: String? = null,
    val masterKind: String? = null,
    val dueAt: String? = null,
    val contract: com.example.momentra.data.api.OperatingContractBody? = null,
    val lines: List<com.example.momentra.data.api.OperatingLineBody>? = null,
)

internal fun OperatingCaptureDraft.toService() = ServiceEventDraft(
    eventType = eventType,
    title = title,
    amount = amount,
    note = note,
    channelName = channelName,
    counterpartyName = counterpartyName,
    paymentStatus = paymentStatus,
    occurredAt = occurredAt,
    quantity = quantity,
    unit = unit,
    unitPrice = unitPrice,
    itemName = itemName,
    parentEventId = parentEventId,
    revisesEventId = revisesEventId,
    masterKind = masterKind,
    dueAt = dueAt,
    contract = contract,
    lines = lines,
)

/** The ten capture sheets, named for the Figma frames (SVC-QA-01…10). Hold/Transfer/Reprint → Phase 5. */
internal enum class ServiceCaptureSheet(
    val title: String,
    val eventType: String,
    val mode: String,
    val icon: Int,
) {
    SERVICE("Finalize Charge", "SERVICE", "SERVICE", ServiceTileIcons.sale),
    PURCHASE("Record Purchase", "PURCHASE", "PURCHASE", ServiceTileIcons.purchase),
    COLLECTION("Record Collection", "COLLECTION", "COLLECTION", ServiceTileIcons.collection),
    PAYMENT("Record Payment", "PAYMENT", "PAYMENT", ServiceTileIcons.payment),
    SPEND("Record Expense", "SPEND", "SPEND", ServiceTileIcons.expense),
    STOCK("Stock Update", "STOCK", "STOCK", ServiceTileIcons.stock),
    APPOINTMENT("Booking", "APPOINTMENT", "APPOINTMENT", ServiceTileIcons.order),
    /** Mode `STATUS` keeps eventType APPOINTMENT so ops updates never post revenue. */
    STATUS("Status / Assignment", "APPOINTMENT", "STATUS", ServiceTileIcons.status),
    BUDGET("Budget / Target Revision", "BUDGET", "BUDGET", ServiceTileIcons.budget),
    ISSUE("Issue / Approval / Update", "ISSUE", "ISSUE", ServiceTileIcons.issue),
    /** Hold parks a slot only — never revenue / GST. */
    HOLD("Appointment hold", "APPOINTMENT", "HOLD", ServiceTileIcons.status),
    /** Transfer reassigns staff/room on open bookings — never a charge. */
    TRANSFER("Staff / room transfer", "APPOINTMENT", "TRANSFER", ServiceTileIcons.collection),
    /** Reprint re-renders a posted invoice — same number, no new charge. */
    REPRINT("Reprint invoice", "SERVICE", "REPRINT", ServiceTileIcons.issue),
}



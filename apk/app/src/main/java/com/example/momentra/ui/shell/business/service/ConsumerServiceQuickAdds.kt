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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.momentra.R
import com.example.momentra.data.api.OperatingProjectionDto
import com.example.momentra.ui.shell.business.shared.BusinessInviteQr
import com.example.momentra.ui.shell.business.shared.OperatingCaptureDraft
import com.example.momentra.ui.shell.business.shared.OperatingCaptureSheet
import com.example.momentra.ui.shell.business.shared.OperatingMomentCatalog
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
    var sheet by remember { mutableStateOf<ServiceCaptureSheet?>(null) }
    var mastersOpen by remember { mutableStateOf<ServiceMastersTab?>(null) }
    var approvalOpen by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

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

        GridSectionHeader("Core · 9", "Service journey")
        QuickAddGrid(
            tiles = ServiceCoreTiles,
            allowed = allowed,
            onSheet = { sheet = it },
            onMasters = { mastersOpen = it },
            onApproval = { approvalOpen = true },
        )

        GridSectionHeader("Support · 5", "Network · masters")
        QuickAddGrid(
            tiles = ServiceSupportTiles,
            allowed = allowed,
            onSheet = { sheet = it },
            onMasters = { mastersOpen = it },
            onApproval = { approvalOpen = true },
        )

        GridSectionHeader("Utilities · 3", "Hold · move · reprint")
        QuickAddGrid(
            tiles = ServiceUtilityTiles,
            allowed = allowed,
            onSheet = { sheet = it },
            onMasters = { mastersOpen = it },
            onApproval = { approvalOpen = true },
        )

        WhyNote(
            "Boundaries: finalized charge = revenue once. Booking ≠ charge. Collection/payment allocate — never duplicate facts. " +
                "Empty analytics = Unavailable.",
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
        OperatingSheetScrim(onDismiss = { sheet = null }, accent = ServicesTeal.teal) {
            OperatingCaptureSheet(
                title = open.title,
                icon = open.icon,
                eventType = open.eventType,
                mode = open.mode,
                model = model,
                channelLabel = when (open) {
                    ServiceCaptureSheet.SERVICE, ServiceCaptureSheet.COLLECTION -> "Service channel"
                    ServiceCaptureSheet.APPOINTMENT, ServiceCaptureSheet.STATUS -> "Room / staff"
                    else -> "Channel"
                },
                variant = "service",
                onDismiss = { sheet = null },
                onSubmit = { draft ->
                    runCatching { onRecord(draft.toService()) }
                        .onFailure { error = it.message }
                    sheet = null
                },
            )
        }
    }

    mastersOpen?.let { open ->
        OperatingSheetScrim(onDismiss = { mastersOpen = null }, accent = ServicesTeal.teal) {
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

/** Rose gradient hero with the `+ Record Sale` white pill. */
@Composable
private fun QuickAddHero(hasService: Boolean, onRecord: () -> Unit) {
    val shape = ServicesTeal.rHero
    Box(
        Modifier
            .fillMaxWidth()
            .shadow(12.dp, shape, clip = false, ambientColor = ServicesTeal.tealDeep.copy(alpha = 0.5f), spotColor = ServicesTeal.tealDeep.copy(alpha = 0.5f))
            .shadow(24.dp, shape, clip = false, ambientColor = ServicesTeal.teal.copy(alpha = 0.3f), spotColor = ServicesTeal.teal.copy(alpha = 0.3f))
            .clip(shape)
            .background(Brush.linearGradient(listOf(ServicesTeal.teal, ServicesTeal.teal)))
            .padding(20.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Text("CONSUMER SERVICE", style = ServicesTeal.heroEyebrow(), maxLines = 1)
            Text("Book. Charge. Settle.", style = ServicesTeal.heroHeadline(), maxLines = 1)
            Text(
                "A booking is not revenue until the charge is finalized. Named clients are required for credit. Settlement never reposts the charge.",
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
    QuickTile("Finalize Charge", "Service · GST", ServiceTileIcons.sale, ServiceCaptureSheet.SERVICE, requires = "SERVICE"),
    QuickTile("Purchase", "Goods · receive", ServiceTileIcons.purchase, ServiceCaptureSheet.PURCHASE, requires = "PURCHASE"),
    QuickTile("Collection", "Allocate open AR", ServiceTileIcons.collection, ServiceCaptureSheet.COLLECTION, requires = "COLLECTION"),
    QuickTile("Payment", "Settle open AP", ServiceTileIcons.payment, ServiceCaptureSheet.PAYMENT, requires = "PAYMENT"),
    QuickTile("Expense", "Store running", ServiceTileIcons.expense, ServiceCaptureSheet.SPEND, requires = "SPEND"),
    QuickTile("Stock", "Count · damage", ServiceTileIcons.stock, ServiceCaptureSheet.STOCK, requires = "STOCK"),
    QuickTile("Booking", "Schedule · room", ServiceTileIcons.order, ServiceCaptureSheet.APPOINTMENT, requires = "APPOINTMENT"),
    QuickTile("Status", "Room · staff · hold", ServiceTileIcons.status, ServiceCaptureSheet.STATUS, requires = "APPOINTMENT"),
    QuickTile("Budget", "Target · revise", ServiceTileIcons.budget, ServiceCaptureSheet.BUDGET, requires = "BUDGET"),
)

private val ServiceSupportTiles = listOf(
    QuickTile("Clients", "Customers · CRM", ServiceTileIcons.suppliers, masterTab = ServiceMastersTab.Clients, wide = false),
    QuickTile("Services", "Menu · rate card", ServiceTileIcons.catalogue, masterTab = ServiceMastersTab.Services, wide = false),
    QuickTile("Rooms & Staff", "Capacity · roster", ServiceTileIcons.locations, masterTab = ServiceMastersTab.RoomsStaff, wide = false),
    QuickTile("Invite & QR", "Join · people · roles", ServiceTileIcons.invite, masterTab = ServiceMastersTab.Invite, wide = true),
    QuickTile("Issue", "Hold · transfer · reprint", ServiceTileIcons.issue, ServiceCaptureSheet.ISSUE, requires = "UPDATE", wide = true),
)

private val ServiceUtilityTiles = listOf(
    QuickTile("Hold", "Park booking", ServiceTileIcons.status, ServiceCaptureSheet.HOLD, requires = "APPOINTMENT"),
    QuickTile("Transfer", "Between rooms", ServiceTileIcons.collection, ServiceCaptureSheet.TRANSFER, requires = "UPDATE"),
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
// Masters screens (Suppliers · Catalogue · Locations · Invite & QR)
// ---------------------------------------------------------------------------------

enum class ServiceMastersTab(val title: String, val masterKind: String?) {
    Clients("Clients", "CUSTOMER"),
    Services("Services", "PRODUCT"),
    RoomsStaff("Rooms & Staff", "LOCATION"),
    Invite("Invite & QR", null),
}

/** Masters surface backed by real `masters` rows + the existing master-create endpoint. */
@Composable
private fun ServiceMastersSheet(
    tab: ServiceMastersTab,
    model: OperatingProjectionDto,
    momentId: String?,
    onClose: () -> Unit,
    onRecord: suspend (ServiceEventDraft) -> Unit,
    onError: (String) -> Unit,
) {
    var createOpen by remember { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ServicesTeal.gutter, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.fillMaxWidth().height(22.dp), horizontalArrangement = Arrangement.Center) {
            Box(
                Modifier
                    .width(40.dp).height(5.dp)
                    .clip(RoundedCornerShape(100.dp))
                    .background(Color.White.copy(alpha = 0.2f)),
            )
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(tab.title, style = ServicesTeal.screenTitle(), modifier = Modifier.weight(1f))
            Text(
                "Close",
                style = ServicesTeal.viewAll().copy(color = ServicesTeal.textBody),
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .clickable(onClick = onClose)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            )
        }
        when (tab) {
            ServiceMastersTab.Invite -> InviteAndQr(model, momentId, onError)
            else -> MasterList(
                tab = tab,
                model = model,
                onCreate = { createOpen = true },
            )
        }
        Spacer(Modifier.height(20.dp))
    }

    if (createOpen) {
        OperatingSheetScrim(onDismiss = { createOpen = false }, accent = ServicesTeal.teal) {
            OperatingCaptureSheet(
                title = "New ${tab.title.trimEnd('s')}",
                icon = when (tab) {
                    ServiceMastersTab.Clients -> ServiceTileIcons.suppliers
                    ServiceMastersTab.Services -> ServiceTileIcons.catalogue
                    ServiceMastersTab.RoomsStaff -> ServiceTileIcons.locations
                    ServiceMastersTab.Invite -> ServiceTileIcons.invite
                },
                eventType = "UPDATE",
                mode = "MASTERS",
                model = model,
                onDismiss = { createOpen = false },
                onSubmit = { draft ->
                    runCatching { onRecord(draft.toService()) }.onFailure { onError(it.message ?: "Could not save") }
                    createOpen = false
                },
            )
        }
    }
}

@Composable
private fun MasterList(
    tab: ServiceMastersTab,
    model: OperatingProjectionDto,
    onCreate: () -> Unit,
) {
    val rows = model.masters.orEmpty().filter { master ->
        when (tab) {
            ServiceMastersTab.Clients -> master.kind in setOf("CUSTOMER", "CLIENT", "STAFF")
            ServiceMastersTab.Services -> master.kind in setOf("PRODUCT", "ITEM", "SERVICE", "CONSUMABLE")
            ServiceMastersTab.RoomsStaff -> master.kind in setOf("LOCATION", "CHANNEL", "STAFF")
            ServiceMastersTab.Invite -> false
        }
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(ServicesTeal.rCardTight)
            .background(ServicesTeal.teal)
            .clickable(onClick = onCreate)
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            "New ${tab.title.trimEnd('s').lowercase()}",
            style = ServicesTeal.storyCtaTitle(),
            maxLines = 1,
        )
    }
    if (rows.isEmpty()) {
        TealEmpty("Nothing here yet", "Records you add will be listed here.", glyph = "◇")
        WhyNote(
            when (tab) {
                ServiceMastersTab.Services -> "Rate-card rows only change on an explicit master record — never from a quick charge."
                ServiceMastersTab.RoomsStaff -> "Rooms and staff group capacity. Opening a new room does not move bookings."
                else -> "Named clients are required for credit and collection tracking."
            },
        )
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { master ->
            AccentRow(
                title = master.name.ifBlank { "Unnamed" },
                caption = listOfNotNull(
                    master.kind.lowercase().replaceFirstChar { it.uppercase() },
                    master.unit?.takeIf { it.isNotBlank() }?.let { "UOM $it" },
                    master.gstRate?.takeIf { it.isNotBlank() }?.let { "GST $it%" },
                ).joinToString(" · "),
                accent = ServicesTeal.teal,
                severityLabel = master.barcode?.takeIf { it.isNotBlank() }?.let { "Scan" },
                severityColor = ServicesTeal.cyan,
            )
        }
    }
    WhyNote(
        when (tab) {
            ServiceMastersTab.Services -> "Rate-card rows only change on an explicit master record — never from a quick charge."
            ServiceMastersTab.RoomsStaff -> "Rooms and staff group capacity. Opening a new room does not move bookings."
            else -> "Named partners are required for credit and payable tracking."
        },
    )
}

/** Invite & QR `2506:29333` — real mint, real QR, real contacts (shared surface). */
@Composable
private fun InviteAndQr(
    model: OperatingProjectionDto,
    momentId: String?,
    onError: (String) -> Unit,
) {
    BusinessInviteQr(
        momentId = momentId,
        title = model.title.orEmpty(),
        role = model.role,
        palette = TealInvitePalette,
        iconRes = ServiceTileIcons.invite,
        noun = "service",
        onError = onError,
    )
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

/** The ten capture sheets, named for the Figma frames. */
internal enum class ServiceCaptureSheet(
    val title: String,
    val eventType: String,
    val mode: String,
    val icon: Int,
) {
    SERVICE("Finalize Charge", "SERVICE", "SERVICE", ServiceTileIcons.sale),
    PURCHASE("Record Purchase", "PURCHASE", "PURCHASE", ServiceTileIcons.purchase),
    COLLECTION("Money Collection", "COLLECTION", "COLLECTION", ServiceTileIcons.collection),
    PAYMENT("Record Payment", "PAYMENT", "PAYMENT", ServiceTileIcons.payment),
    SPEND("Record Expense", "SPEND", "SPEND", ServiceTileIcons.expense),
    STOCK("Stock Update", "STOCK", "STOCK", ServiceTileIcons.stock),
    APPOINTMENT("Booking", "APPOINTMENT", "APPOINTMENT", ServiceTileIcons.order),
    STATUS("Status / Assignment", "APPOINTMENT", "APPOINTMENT", ServiceTileIcons.status),
    BUDGET("Revise Budget", "BUDGET", "BUDGET", ServiceTileIcons.budget),
    ISSUE("Issue / Update", "ISSUE", "ISSUE", ServiceTileIcons.issue),
    HOLD("Hold / release", "APPOINTMENT", "APPOINTMENT", ServiceTileIcons.status),
    TRANSFER("Transfer", "UPDATE", "UPDATE", ServiceTileIcons.collection),
    REPRINT("Reprint receipt", "SERVICE", "SERVICE", ServiceTileIcons.issue),
}


internal val TealInvitePalette = com.example.momentra.ui.shell.business.shared.InvitePalette(
    accent = ServicesTeal.teal,
    accentMuted = ServicesTeal.tealMuted,
    card = ServicesTeal.card,
    field = ServicesTeal.field,
    chip = ServicesTeal.chip,
    chipStroke = ServicesTeal.chipStroke,
    divider = ServicesTeal.divider,
    qrPlate = ServicesTeal.card,
    textPrimary = ServicesTeal.textPrimary,
    textBody = ServicesTeal.textBody,
    textMuted = ServicesTeal.textMuted,
    textFaint = ServicesTeal.textFaint,
    warning = ServicesTeal.warning,
    negative = ServicesTeal.negative,
)

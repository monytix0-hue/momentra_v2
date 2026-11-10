package com.example.momentra.ui.shell.business.retail

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
import com.example.momentra.ui.shell.business.shared.BusinessInviteQr
import com.example.momentra.ui.shell.business.shared.OperatingCaptureDraft
import com.example.momentra.ui.shell.business.shared.OperatingCaptureSheet
import com.example.momentra.ui.shell.business.shared.OperatingMomentCatalog
import com.example.momentra.ui.shell.business.shared.OperatingSheetScrim
import com.example.momentra.ui.shell.business.shared.RoseInvitePalette
import com.example.momentra.ui.theme.RetailRose

/**
 * Quick Add hub — Figma RTL-HUB-v5 `2399:29887`.
 *
 * 9 core tiles (3 × 3) + 5 support tiles (3 + 2 wide). Tile order, labels and
 * captions match the frame; a tile the role cannot submit renders locked instead of
 * disappearing, so the grid keeps its shape.
 */
@Composable
fun RetailQuickAddHub(
    model: OperatingProjectionDto,
    onRecord: suspend (RetailEventDraft) -> Unit,
    onDecide: suspend (String, String) -> Unit,
    momentId: String? = null,
    companyId: String? = null,
    onDismiss: () -> Unit = {},
) {
    val allowed = model.quickAdds.map { it.code }.toSet()
    var sheet by remember { mutableStateOf<RetailSheet?>(null) }
    var mastersOpen by remember { mutableStateOf<MastersTab?>(null) }
    var approvalOpen by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val holdSession = remember { RetailHoldSession() }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = RetailRose.gutter, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(RetailRose.sectionGap),
    ) {
        ScreenHeader(
            title = "Quick Adds",
            roleLine = "${model.title?.takeIf { it.isNotBlank() } ?: "Retail Store"} · ${model.subtypeLabel ?: "Retail"}",
        )
        ContextChips(
            subtypeLabel = model.subtypeLabel ?: "Retail Store",
            periodLine = RetailFormat.periodLine(model.startDate, model.currency),
            gstOn = model.gstTracking == "YES",
        )

        QuickAddHero(hasSale = "SALE" in allowed, onRecord = { sheet = RetailSheet.SALE })

        GridSectionHeader("Core · 9", "Retail journey")
        QuickAddGrid(
            tiles = RetailCoreTiles,
            allowed = allowed,
            onSheet = { sheet = it },
            onMasters = { mastersOpen = it },
            onApproval = { approvalOpen = true },
        )

        GridSectionHeader("Support · 5", "Network · masters")
        QuickAddGrid(
            tiles = RetailSupportTiles,
            allowed = allowed,
            onSheet = { sheet = it },
            onMasters = { mastersOpen = it },
            onApproval = { approvalOpen = true },
        )

        GridSectionHeader("Utilities · 3", "Hold · move · reprint")
        QuickAddGrid(
            tiles = RetailUtilityTiles,
            allowed = allowed,
            onSheet = { sheet = it },
            onMasters = { mastersOpen = it },
            onApproval = { approvalOpen = true },
        )

        WhyNote(
            "Boundaries: posted sale = revenue once. Quick amount ≠ SKU movement. Order is commitment only. " +
                "Collection/payment allocate — never duplicate facts. Empty analytics = Unavailable.",
            tinted = true,
        )

        if (error != null) {
            Text(
                error ?: "",
                style = RetailRose.rowCaption(RetailRose.negative),
            )
        }
        Spacer(Modifier.height(16.dp))
    }

    sheet?.let { open ->
        OperatingSheetScrim(
            onDismiss = { sheet = null },
            accent = RetailRose.rose,
            surface = RetailRose.cardSlate,
            border = RetailRose.roseBorder.copy(alpha = 0.45f),
        ) {
            when (open) {
                RetailSheet.SALE -> RetailPosSheet(
                    model = model,
                    holdSession = holdSession,
                    onDismiss = { sheet = null },
                    onSubmit = { draft ->
                        scope.launch {
                            runCatching { onRecord(draft) }.onFailure { error = it.message }
                            sheet = null
                        }
                    },
                )
                RetailSheet.HOLD -> RetailParkBillSheet(
                    model = model,
                    holdSession = holdSession,
                    onDismiss = { sheet = null },
                    onNavigate = { dest ->
                        sheet = when (dest) {
                            RetailUtilityNav.HOLD -> RetailSheet.HOLD
                            RetailUtilityNav.TRANSFER -> RetailSheet.TRANSFER
                            RetailUtilityNav.REPRINT -> RetailSheet.REPRINT
                        }
                    },
                )
                RetailSheet.TRANSFER -> RetailStockTransferSheet(
                    model = model,
                    onDismiss = { sheet = null },
                    onNavigate = { dest ->
                        sheet = when (dest) {
                            RetailUtilityNav.HOLD -> RetailSheet.HOLD
                            RetailUtilityNav.TRANSFER -> RetailSheet.TRANSFER
                            RetailUtilityNav.REPRINT -> RetailSheet.REPRINT
                        }
                    },
                    onSubmit = { draft ->
                        scope.launch {
                            runCatching { onRecord(draft) }.onFailure { error = it.message }
                            sheet = null
                        }
                    },
                )
                RetailSheet.REPRINT -> RetailReprintSheet(
                    model = model,
                    onDismiss = { sheet = null },
                    onNavigate = { dest ->
                        sheet = when (dest) {
                            RetailUtilityNav.HOLD -> RetailSheet.HOLD
                            RetailUtilityNav.TRANSFER -> RetailSheet.TRANSFER
                            RetailUtilityNav.REPRINT -> RetailSheet.REPRINT
                        }
                    },
                )
                else -> RetailCaptureSheet(
                    sheet = open,
                    model = model,
                    onDismiss = { sheet = null },
                    onSubmit = { draft ->
                        scope.launch {
                            runCatching { onRecord(draft) }.onFailure { error = it.message }
                            sheet = null
                        }
                    },
                )
            }
        }
    }

    mastersOpen?.let { open ->
        OperatingSheetScrim(onDismiss = { mastersOpen = null }, accent = RetailRose.rose) {
            RetailMastersSheet(
                tab = open,
                model = model,
                momentId = momentId,
                companyId = companyId,
                onClose = { mastersOpen = null },
                onRecord = onRecord,
                onError = { error = it },
            )
        }
    }

    if (approvalOpen) {
        OperatingSheetScrim(onDismiss = { approvalOpen = false }, accent = RetailRose.rose) {
            OperatingCaptureSheet(
                title = "Review Approval",
                icon = RetailTileIcons.status,
                eventType = "APPROVAL",
                mode = "APPROVAL",
                model = model,
                onDismiss = { approvalOpen = false },
                onSubmit = {},
                onDecide = onDecide,
            )
        }
    }
}

/** Rose gradient hero with the `+ Record Sale` white pill. */
@Composable
private fun QuickAddHero(hasSale: Boolean, onRecord: () -> Unit) {
    val shape = RetailRose.rHero
    Box(
        Modifier
            .fillMaxWidth()
            .shadow(12.dp, shape, clip = false, ambientColor = RetailRose.indigo.copy(alpha = 0.5f), spotColor = RetailRose.indigo.copy(alpha = 0.5f))
            .shadow(24.dp, shape, clip = false, ambientColor = RetailRose.rose.copy(alpha = 0.3f), spotColor = RetailRose.rose.copy(alpha = 0.3f))
            .clip(shape)
            .background(Brush.linearGradient(listOf(RetailRose.rose, RetailRose.rose)))
            .padding(20.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Text("RETAIL STORE", style = RetailRose.heroEyebrow(), maxLines = 1)
            Text("Stock. Sell. Settle.", style = RetailRose.heroHeadline(), maxLines = 1)
            Text(
                "Quick amount never moves SKU stock. Credit sales need a named customer. Settlement never reposts the sale.",
                style = RetailRose.heroBody(),
                maxLines = 3,
            )
            if (hasSale) {
                Spacer(Modifier.height(4.dp))
                RoseCta("+  Record Sale", onClick = onRecord)
            }
        }
    }
}

private data class QuickTile(
    val label: String,
    val caption: String,
    val icon: Int,
    val sheet: RetailSheet? = null,
    val requires: String? = null,
    val wide: Boolean = false,
    val masterTab: MastersTab? = null,
    val approval: Boolean = false,
)

private val RetailCoreTiles = listOf(
    QuickTile("Sale", "Quick or itemized", RetailTileIcons.sale, RetailSheet.SALE, requires = "SALE"),
    QuickTile("Purchase", "Goods · receive", RetailTileIcons.purchase, RetailSheet.PURCHASE, requires = "PURCHASE"),
    QuickTile("Collection", "Allocate open AR", RetailTileIcons.collection, RetailSheet.COLLECTION, requires = "COLLECTION"),
    QuickTile("Payment", "Settle open AP", RetailTileIcons.payment, RetailSheet.PAYMENT, requires = "PAYMENT"),
    QuickTile("Expense", "Store running", RetailTileIcons.expense, RetailSheet.SPEND, requires = "SPEND"),
    QuickTile("Stock", "Count · damage", RetailTileIcons.stock, RetailSheet.STOCK, requires = "STOCK"),
    QuickTile("Order", "Optional customer", RetailTileIcons.order, RetailSheet.ORDER, requires = "ORDER"),
    QuickTile("Status", "Dispatch / fulfil", RetailTileIcons.status, RetailSheet.STATUS, requires = "ORDER"),
    QuickTile("Budget", "Versioned revise", RetailTileIcons.budget, RetailSheet.BUDGET, requires = "BUDGET"),
)

private val RetailSupportTiles = listOf(
    QuickTile("Suppliers", "Vendors", RetailTileIcons.suppliers, masterTab = MastersTab.Suppliers, wide = false),
    QuickTile("Catalogue", "SKU · UOM", RetailTileIcons.catalogue, masterTab = MastersTab.Catalogue, wide = false),
    QuickTile("Locations", "Stores · aisles", RetailTileIcons.locations, masterTab = MastersTab.Locations, wide = false),
    QuickTile("Invite & QR", "Join · people · roles", RetailTileIcons.invite, masterTab = MastersTab.Invite, wide = true),
    QuickTile("Issue", "Update · approval", RetailTileIcons.issue, RetailSheet.ISSUE, requires = "UPDATE", wide = true),
)

private val RetailUtilityTiles = listOf(
    QuickTile("Hold", "Park order", RetailTileIcons.hold, RetailSheet.HOLD, requires = "ORDER"),
    QuickTile("Transfer", "Between stores", RetailTileIcons.transfer, RetailSheet.TRANSFER, requires = "STOCK"),
    QuickTile("Reprint", "Receipt · bill", RetailTileIcons.reprint, RetailSheet.REPRINT, requires = "SALE"),
)

/** 3-column grid; `wide` tiles span the remaining width in their row. */
@Composable
private fun QuickAddGrid(
    tiles: List<QuickTile>,
    allowed: Set<String>,
    onSheet: (RetailSheet) -> Unit,
    onMasters: (MastersTab) -> Unit,
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

enum class MastersTab(val title: String, val masterKind: String?) {
    Suppliers("Suppliers", "SUPPLIER"),
    Catalogue("Catalogue", "PRODUCT"),
    Locations("Locations", "LOCATION"),
    Invite("Invite & QR", null),
}

/** Masters surface backed by real `masters` rows + the existing master-create endpoint. */
@Composable
private fun RetailMastersSheet(
    tab: MastersTab,
    model: OperatingProjectionDto,
    momentId: String?,
    companyId: String?,
    onClose: () -> Unit,
    onRecord: suspend (RetailEventDraft) -> Unit,
    onError: (String) -> Unit,
) {
    var createKind by remember { mutableStateOf<RetailMasterFormKind?>(null) }
    val scope = rememberCoroutineScope()
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = RetailRose.gutter, vertical = 12.dp),
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
            Text(tab.title, style = RetailRose.screenTitle(), modifier = Modifier.weight(1f))
            Text(
                "Close",
                style = RetailRose.viewAll().copy(color = RetailRose.textBody),
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .clickable(onClick = onClose)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            )
        }
        when (tab) {
            MastersTab.Invite -> InviteAndQr(model, momentId, onError)
            MastersTab.Catalogue -> {
                MasterList(
                    tab = tab,
                    model = model,
                    onCreate = { createKind = RetailMasterFormKind.Product },
                    createLabel = "New product / SKU",
                )
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RetailRose.rCardTight)
                        .background(RetailRose.chip)
                        .clickable { createKind = RetailMasterFormKind.StockItem }
                        .padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text("New stock item", style = RetailRose.storyCtaTitle().copy(color = RetailRose.textPrimary), maxLines = 1)
                }
            }
            else -> MasterList(
                tab = tab,
                model = model,
                onCreate = {
                    createKind = when (tab) {
                        MastersTab.Suppliers -> RetailMasterFormKind.Supplier
                        MastersTab.Locations -> RetailMasterFormKind.Location
                        else -> null
                    }
                },
            )
        }
        Spacer(Modifier.height(20.dp))
    }

    createKind?.let { formKind ->
        OperatingSheetScrim(
            onDismiss = { createKind = null },
            accent = RetailRose.rose,
            surface = RetailRose.cardSlate,
            border = RetailRose.roseBorder.copy(alpha = 0.45f),
        ) {
            RetailMasterFormSheet(
                kind = formKind,
                model = model,
                companyId = companyId,
                onDismiss = { createKind = null },
                onSubmit = { draft ->
                    scope.launch {
                        runCatching { onRecord(draft) }.onFailure { onError(it.message ?: "Could not save") }
                        createKind = null
                    }
                },
            )
        }
    }
}

@Composable
private fun MasterList(
    tab: MastersTab,
    model: OperatingProjectionDto,
    onCreate: () -> Unit,
    createLabel: String? = null,
) {
    val rows = model.masters.orEmpty().filter { master ->
        when (tab) {
            MastersTab.Suppliers -> master.kind.equals("SUPPLIER", true)
            MastersTab.Catalogue -> master.kind in setOf("PRODUCT", "ITEM", "SKU", "MATERIAL", "CONSUMABLE")
            MastersTab.Locations -> master.kind in setOf("LOCATION", "STORE", "CHANNEL", "WAREHOUSE")
            MastersTab.Invite -> false
        }
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RetailRose.rCardTight)
            .background(RetailRose.rose)
            .clickable(onClick = onCreate)
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            createLabel ?: "New ${tab.title.trimEnd('s').lowercase()}",
            style = RetailRose.storyCtaTitle(),
            maxLines = 1,
        )
    }
    if (rows.isEmpty()) {
        RoseEmpty("Nothing here yet", "Records you add will be listed here.", glyph = "◇")
        WhyNote(
            when (tab) {
                MastersTab.Catalogue -> "Catalogue rows only change on an explicit master record — never from a quick sale."
                MastersTab.Locations -> "A location groups stock. Opening a new one does not move existing quantities."
                else -> "Named partners are required for credit and payable tracking."
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
                accent = RetailRose.rose,
                severityLabel = master.barcode?.takeIf { it.isNotBlank() }?.let { "Scan" },
                severityColor = RetailRose.cyan,
            )
        }
    }
    WhyNote(
        when (tab) {
            MastersTab.Catalogue -> "Catalogue rows only change on an explicit master record — never from a quick sale."
            MastersTab.Locations -> "A location groups stock. Opening a new one does not move existing quantities."
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
        palette = RoseInvitePalette,
        iconRes = RetailTileIcons.invite,
        noun = "store",
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
data class RetailEventDraft(
    val eventType: String,
    val title: String,
    val amount: String?,
    val note: String?,
    val channelName: String? = null,
    val counterpartyName: String? = null,
    val paymentStatus: String? = null,
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
    val master: com.example.momentra.data.api.OperatingMasterBody? = null,
    val activityStatus: String? = null,
)

internal fun OperatingCaptureDraft.toRetail() = RetailEventDraft(
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
internal enum class RetailSheet(
    val title: String,
    val eventType: String,
    val mode: String,
    val icon: Int,
) {
    SALE("Record Sale", "SALE", "SALE", RetailTileIcons.sale),
    PURCHASE("Record Purchase", "PURCHASE", "PURCHASE", RetailTileIcons.purchase),
    COLLECTION("Money Collection", "COLLECTION", "COLLECTION", RetailTileIcons.collection),
    PAYMENT("Record Payment", "PAYMENT", "PAYMENT", RetailTileIcons.payment),
    SPEND("Record Expense", "SPEND", "SPEND", RetailTileIcons.expense),
    STOCK("Stock Update", "STOCK", "STOCK", RetailTileIcons.stock),
    ORDER("Record Order", "ORDER", "ORDER", RetailTileIcons.order),
    STATUS("Status / Assignment", "ORDER", "ORDER", RetailTileIcons.status),
    BUDGET("Revise Budget", "BUDGET", "BUDGET", RetailTileIcons.budget),
    ISSUE("Issue / Update", "ISSUE", "ISSUE", RetailTileIcons.issue),
    HOLD("Hold / release", "ORDER", "HOLD", RetailTileIcons.hold),
    TRANSFER("Transfer stock", "STOCK", "TRANSFER", RetailTileIcons.transfer),
    REPRINT("Reprint receipt", "SALE", "REPRINT", RetailTileIcons.reprint),
}

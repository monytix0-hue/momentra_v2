package com.example.momentra.ui.shell.business.retail

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.example.momentra.data.api.OperatingProjectionDto
import com.example.momentra.ui.shell.business.mto.MtoToastHost
import com.example.momentra.ui.shell.business.mto.MtoToastKind
import com.example.momentra.ui.shell.business.mto.MtoToastSpec
import com.example.momentra.ui.shell.business.shared.OperatingCaptureDraft
import com.example.momentra.ui.shell.business.shared.OperatingCaptureSheet
import com.example.momentra.ui.shell.business.shared.OperatingSheetScrim
import com.example.momentra.ui.theme.RetailRose

/**
 * Quick Add hub — Figma RTL-HUB-v5 `2399:29887`.
 *
 * 9 core tiles (3 × 3) + 5 support tiles (3 + 2 wide). Tile order, labels and
 * captions match the frame; a tile the role cannot submit renders locked instead of
 * disappearing, so the grid keeps its shape.
 *
 * **Utilities · 3 decision:** Figma hub shows Core · 9 + Support · 5 only (no Hold /
 * Transfer / Reprint band). Those remain required RTL screens (`2508:29333` / `29447` /
 * `29537`). Keep a muted Utilities row for HTR discovery — same as Service —
 * rather than deleting HTR or folding into Support · 5.
 */
@Composable
fun RetailQuickAddHub(
    model: OperatingProjectionDto,
    onRecord: suspend (RetailEventDraft) -> Unit,
    onDecide: suspend (String, String) -> Unit,
    momentId: String? = null,
    companyId: String? = null,
    onDismiss: () -> Unit = {},
    holdSession: RetailHoldSession? = null,
) {
    val allowed = model.quickAdds.map { it.code }.toSet()
    var sheet by remember { mutableStateOf<RetailSheet?>(null) }
    var mastersOpen by remember { mutableStateOf<MastersTab?>(null) }
    var approvalOpen by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val holdContext = androidx.compose.ui.platform.LocalContext.current
    val parks = holdSession ?: remember(momentId, companyId) {
        RetailHoldSession(
            context = holdContext.applicationContext,
            momentId = momentId,
            companyId = companyId,
        )
    }

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

        // Figma RTL-HUB omits Utilities; kept muted for HTR discovery (see file KDoc).
        GridSectionHeader("Utilities · 3", "Hold · move · reprint", muted = true)
        QuickAddGrid(
            tiles = RetailUtilityTiles,
            allowed = allowed,
            onSheet = { sheet = it },
            onMasters = { mastersOpen = it },
            onApproval = { approvalOpen = true },
            muted = true,
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
            topCorner = 28.dp,
        ) {
            when (open) {
                RetailSheet.SALE -> RetailPosSheet(
                    model = model,
                    holdSession = parks,
                    onDismiss = { sheet = null },
                    companyId = companyId,
                    momentId = momentId,
                    onSubmit = { draft ->
                        onRecord(draft)
                    },
                )
                RetailSheet.HOLD -> RetailParkBillSheet(
                    model = model,
                    holdSession = parks,
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
                    holdSession = parks,
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
                    companyId = companyId,
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
                    companyId = companyId,
                    onSubmit = { draft ->
                        scope.launch {
                            error = null
                            runCatching { onRecord(draft) }
                                .onSuccess {
                                    if (draft.eventType.equals("COLLECTION", true) ||
                                        draft.eventType.equals("PAYMENT", true)
                                    ) {
                                        MtoToastHost.show(
                                            MtoToastSpec(
                                                kind = MtoToastKind.Saved,
                                                title = RetailCaptureCopy.SAVED_TOAST,
                                                body = RetailCaptureCopy.TAGLINE,
                                                actionLabel = "Done",
                                                accent = RetailRose.positive,
                                            ),
                                        )
                                    }
                                    sheet = null
                                }
                                .onFailure {
                                    error = it.message ?: "Could not save"
                                }
                        }
                    },
                    onMasterSubmit = { draft ->
                        scope.launch {
                            runCatching { onRecord(draft) }.onFailure { error = it.message }
                            // Keep Order sheet open; projection refresh fills the browser.
                        }
                    },
                )
            }
        }
    }

    mastersOpen?.let { open ->
        OperatingSheetScrim(
            onDismiss = { mastersOpen = null },
            accent = RetailRose.rose,
            surface = RetailRose.cardSlate,
            border = RetailRose.roseBorder.copy(alpha = 0.45f),
            topCorner = 28.dp,
        ) {
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
    QuickTile("Expense", "Running cost", RetailTileIcons.expense, RetailSheet.SPEND, requires = "SPEND"),
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
    QuickTile("Hold", "Park bill", RetailTileIcons.hold, RetailSheet.HOLD, requires = "SALE"),
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
    muted: Boolean = false,
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
                        muted = muted,
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
    master = masterKind?.let { kind ->
        com.example.momentra.data.api.OperatingMasterBody(
            kind = kind,
            name = title,
            unit = unit,
            sellPrice = unitPrice,
            gstApplicable = masterGstApplicable,
            gstClassification = masterGstClassification,
            gstRate = masterGstRate,
            priceIncludesGst = masterPriceIncludesGst,
            salePriceIncludesGst = masterSalePriceIncludesGst,
            purchasePriceIncludesGst = masterPurchasePriceIncludesGst,
            gstEffectiveFrom = masterGstEffectiveFrom,
            hsn = masterHsn,
            gstRegistration = masterGstRegistration,
            businessState = masterBusinessState,
            billingState = masterBillingState,
            deliveryState = masterDeliveryState,
            gstin = masterGstin,
        )
    },
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
    COLLECTION("Record Collection", "COLLECTION", "COLLECTION", RetailTileIcons.collection),
    PAYMENT("Record Payment", "PAYMENT", "PAYMENT", RetailTileIcons.payment),
    SPEND("Record Expense", "SPEND", "SPEND", RetailTileIcons.expense),
    STOCK("Stock Update", "STOCK", "STOCK", RetailTileIcons.stock),
    /** Opens SO/PO sheet; Save posts ORDER commitment only — never SALE/AP/stock/GST. */
    ORDER("Sales Order", "ORDER", "ORDER", RetailTileIcons.order),
    /** Ops status only — activityStatus on ORDER parent; never SALE/STOCK. */
    STATUS("Status / Assignment", "ORDER", "ORDER", RetailTileIcons.status),
    BUDGET("Budget / Target Revision", "BUDGET", "BUDGET", RetailTileIcons.budget),
    ISSUE("Issue / Approval / Update", "ISSUE", "ISSUE", RetailTileIcons.issue),
    HOLD("Hold / Park bill", "ORDER", "HOLD", RetailTileIcons.hold),
    TRANSFER("Transfer stock", "STOCK", "TRANSFER", RetailTileIcons.transfer),
    REPRINT("Reprint receipt", "SALE", "REPRINT", RetailTileIcons.reprint),
}

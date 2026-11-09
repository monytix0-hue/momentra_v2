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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.R
import com.example.momentra.data.api.OperatingMasterBody
import com.example.momentra.data.api.OperatingProjectionDto
import com.example.momentra.ui.shell.business.shared.OperatingCaptureDraft
import com.example.momentra.ui.shell.business.shared.OperatingCaptureSheet
import com.example.momentra.ui.shell.business.shared.OperatingMomentCatalog
import com.example.momentra.ui.shell.business.shared.OperatingSheetScrim
import com.example.momentra.ui.theme.PlusJakartaSans
import com.example.momentra.ui.theme.RestaurantEmber
import kotlinx.coroutines.launch

/**
 * Quick Add hub — Figma `RST-HUB-v5` `2400:29892`.
 *
 * 9 core tiles (3 × 3) + 5 support tiles (3 + the 177-wide `Invite & QR` / `Issue` pair).
 * A tile the role cannot submit stays in the grid and renders locked, so the grid keeps
 * its shape instead of silently shrinking.
 */
@Composable
fun RestaurantQuickAddHub(
    model: OperatingProjectionDto,
    onRecord: suspend (RestaurantEventDraft) -> Unit,
    onDecide: suspend (String, String) -> Unit,
    momentId: String? = null,
) {
    val allowed = model.quickAdds.map { it.code }.toSet()
    var sheet by remember { mutableStateOf<RestaurantHubSheet?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = RestaurantEmber.gutter, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(RestaurantEmber.sectionGap),
    ) {
        EmberScreenHeader(
            title = "Quick Adds",
            roleLine = "${model.title?.takeIf { it.isNotBlank() } ?: "Restaurant"} · " +
                (model.subtypeLabel ?: "Restaurant"),
        )
        EmberContextChips(
            subtypeLabel = model.subtypeLabel ?: "Restaurant",
            periodLine = RestaurantFormat.periodLine(model.startDate, model.currency),
            gstOn = model.gstTracking == "YES",
        )

        HubHero(hasSale = "SALE" in allowed, onRecord = { sheet = RestaurantHubSheet.Pos })

        EmberGridSectionHeader("Core · 9", "RFC Food journey")
        HubGrid(
            tiles = RestaurantCoreTiles,
            allowed = allowed,
            onSheet = { sheet = it },
        )

        EmberGridSectionHeader("Support · 5", "People · menu")
        HubGrid(
            tiles = RestaurantSupportTiles,
            allowed = allowed,
            onSheet = { sheet = it },
        )

        EmberWhyNote(
            "Boundaries: order→prep→ready→fulfil→bill posted→collect. Spoilage and aggregator fees " +
                "must not duplicate facts. Empty = Unavailable.",
            tinted = true,
        )

        error?.let { Text(it, style = RestaurantEmber.rowCaption(RestaurantEmber.negative)) }
        Spacer(Modifier.height(16.dp))
    }

    when (val open = sheet) {
        null -> Unit
        RestaurantHubSheet.Pos -> OperatingSheetScrim(
            onDismiss = { sheet = null },
            accent = RestaurantEmber.ember,
        ) {
            RestaurantPosSheet(
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

        RestaurantHubSheet.Kot -> OperatingSheetScrim(
            onDismiss = { sheet = null },
            accent = RestaurantEmber.ember,
        ) {
            RestaurantKotSheet(
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

        is RestaurantHubSheet.Masters -> OperatingSheetScrim(
            onDismiss = { sheet = null },
            accent = RestaurantEmber.ember,
        ) {
            RestaurantMastersSheet(
                tab = open.tab,
                model = model,
                momentId = momentId,
                onClose = { sheet = null },
                onRecord = onRecord,
                onError = { error = it },
            )
        }

        is RestaurantHubSheet.Capture -> OperatingSheetScrim(
            onDismiss = { sheet = null },
            accent = RestaurantEmber.ember,
        ) {
            val kind = open.kind
            OperatingCaptureSheet(
                title = kind.title,
                icon = kind.icon,
                eventType = kind.eventType,
                mode = kind.mode,
                model = model,
                channelLabel = when (kind) {
                    RestaurantSheetKind.Collection -> "Received from"
                    RestaurantSheetKind.Sale -> "Sales channel"
                    else -> "Channel"
                },
                variant = "restaurant",
                onDismiss = { sheet = null },
                onSubmit = { draft ->
                    scope.launch {
                        runCatching { onRecord(draft.toRestaurantDraft()) }.onFailure { error = it.message }
                        sheet = null
                    }
                },
                onDecide = onDecide,
            )
        }

        RestaurantHubSheet.Approval -> OperatingSheetScrim(
            onDismiss = { sheet = null },
            accent = RestaurantEmber.ember,
        ) {
            OperatingCaptureSheet(
                title = "Review Approval",
                icon = RestaurantTileIcons.status,
                eventType = "APPROVAL",
                mode = "APPROVAL",
                model = model,
                variant = "restaurant",
                onDismiss = { sheet = null },
                onSubmit = {},
                onDecide = onDecide,
            )
        }
    }
}

/** Wine → ember hero with the white `+ Record Sale` pill. */
@Composable
private fun HubHero(hasSale: Boolean, onRecord: () -> Unit) {
    val shape = RestaurantEmber.rHero
    Box(
        Modifier
            .fillMaxWidth()
            .shadow(
                12.dp,
                shape,
                clip = false,
                ambientColor = RestaurantEmber.ember.copy(alpha = 0.5f),
                spotColor = RestaurantEmber.ember.copy(alpha = 0.5f),
            )
            .shadow(
                24.dp,
                shape,
                clip = false,
                ambientColor = RestaurantEmber.gold.copy(alpha = 0.3f),
                spotColor = RestaurantEmber.gold.copy(alpha = 0.3f),
            )
            .clip(shape)
            .background(Brush.linearGradient(listOf(RestaurantEmber.heroStart, RestaurantEmber.heroEnd)))
            .padding(20.dp),
    ) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("RESTAURANT / FOOD", style = RestaurantEmber.heroEyebrow(), maxLines = 1)
            Text(
                "Order. Serve. Collect.",
                style = RestaurantEmber.heroHeadline(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "Order and preparation are not revenue. Posted bill owns the sale. " +
                    "Platform AR needs an explicit settlement link.",
                style = RestaurantEmber.heroBody(),
                maxLines = 3,
            )
            if (hasSale) {
                Spacer(Modifier.height(4.dp))
                EmberCta(label = "+  Record Sale", onClick = onRecord)
            }
        }
    }
}

internal sealed interface RestaurantHubSheet {
    data object Pos : RestaurantHubSheet
    data object Kot : RestaurantHubSheet
    data object Approval : RestaurantHubSheet
    data class Capture(val kind: RestaurantSheetKind) : RestaurantHubSheet
    data class Masters(val tab: RestaurantMastersTab) : RestaurantHubSheet
}

private data class HubTile(
    val label: String,
    val caption: String,
    val icon: Int,
    val kind: RestaurantSheetKind? = null,
    val requires: String? = null,
    val master: RestaurantMastersTab? = null,
    val hub: RestaurantHubSheet? = null,
    val wide: Boolean = false,
)

private val RestaurantCoreTiles = listOf(
    HubTile("Sale", "Bill / direct sale", RestaurantTileIcons.sale, hub = RestaurantHubSheet.Pos, requires = "SALE"),
    HubTile("Purchase", "Ingredients", RestaurantTileIcons.purchase, kind = RestaurantSheetKind.Purchase, requires = "PURCHASE"),
    HubTile("Collection", "AR · platform", RestaurantTileIcons.collection, kind = RestaurantSheetKind.Collection, requires = "COLLECTION"),
    HubTile("Payment", "Settle AP", RestaurantTileIcons.payment, kind = RestaurantSheetKind.Payment, requires = "PAYMENT"),
    HubTile("Expense", "Kitchen · platform", RestaurantTileIcons.expense, kind = RestaurantSheetKind.Spend, requires = "SPEND"),
    HubTile("Stock", "Count · waste", RestaurantTileIcons.stock, kind = RestaurantSheetKind.Stock, requires = "STOCK"),
    HubTile("Order", "Food / catering", RestaurantTileIcons.order, kind = RestaurantSheetKind.Order, requires = "ORDER"),
    HubTile("Status", "Prep · fulfil", RestaurantTileIcons.status, hub = RestaurantHubSheet.Kot, requires = "ORDER"),
    HubTile("Budget", "Versioned revise", RestaurantTileIcons.budget, kind = RestaurantSheetKind.Budget, requires = "BUDGET"),
)

private val RestaurantSupportTiles = listOf(
    HubTile("Vendors", "Suppliers", RestaurantTileIcons.vendors, master = RestaurantMastersTab.Vendors),
    HubTile("Menu", "Items · modifiers", RestaurantTileIcons.menu, master = RestaurantMastersTab.Menu),
    HubTile("Stations", "Prep · pass", RestaurantTileIcons.stations, master = RestaurantMastersTab.Stations),
    HubTile("Invite & QR", "Join Moment", RestaurantTileIcons.invite, master = RestaurantMastersTab.Invite, wide = true),
    HubTile("Issue", "Update · approval", RestaurantTileIcons.issue, kind = RestaurantSheetKind.Issue, requires = "UPDATE", wide = true),
)

/** 3-column grid; the two `wide` tiles pair two-up at 177pt, as in the frame. */
@Composable
private fun HubGrid(
    tiles: List<HubTile>,
    allowed: Set<String>,
    onSheet: (RestaurantHubSheet) -> Unit,
) {
    val rows = buildList {
        var row = mutableListOf<HubTile>()
        fun flush() {
            if (row.isNotEmpty()) {
                add(row.toList())
                row = mutableListOf()
            }
        }
        tiles.forEach { tile ->
            if (tile.wide) {
                flush()
                val last = lastOrNull()
                if (last != null && last.firstOrNull()?.wide == true && last.size < 2) {
                    removeAt(lastIndex)
                    add(last + tile)
                } else {
                    add(listOf(tile))
                }
            } else {
                row += tile
                if (row.size == 3) flush()
            }
        }
        flush()
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { cells ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                cells.forEachIndexed { index, tile ->
                    val locked = tile.requires?.takeIf { it !in allowed }?.let { "Role locked" }
                    val weight = when {
                        cells.size == 1 -> 1f
                        cells.all { it.wide } -> 1f
                        cells.size < 3 && index == cells.lastIndex -> (3 - cells.size + 1).toFloat()
                        else -> 1f
                    }
                    EmberQuickAddTile(
                        iconRes = tile.icon,
                        label = tile.label,
                        caption = tile.caption,
                        locked = locked,
                        modifier = Modifier.weight(weight),
                        onClick = {
                            val target = tile.hub
                                ?: tile.master?.let { RestaurantHubSheet.Masters(it) }
                                ?: tile.kind?.let { RestaurantHubSheet.Capture(it) }
                            target?.let(onSheet)
                        },
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------
// Capture kinds (the 9 non-POS / non-KOT sheets)
// ---------------------------------------------------------------------------------

internal enum class RestaurantSheetKind(
    val title: String,
    val eventType: String,
    val mode: String,
    val icon: Int,
) {
    Sale("Record Sale", "SALE", "SALE", RestaurantTileIcons.sale),
    Purchase("Record Purchase", "PURCHASE", "PURCHASE", RestaurantTileIcons.purchase),
    Collection("Money Collection", "COLLECTION", "COLLECTION", RestaurantTileIcons.collection),
    Payment("Record Payment", "PAYMENT", "PAYMENT", RestaurantTileIcons.payment),
    Spend("Record Expense", "SPEND", "SPEND", RestaurantTileIcons.expense),
    Stock("Stock Update", "STOCK", "STOCK", RestaurantTileIcons.stock),
    Order("Record Order", "ORDER", "ORDER", RestaurantTileIcons.order),
    Status("Status / Assignment", "ORDER", "ORDER", RestaurantTileIcons.status),
    Budget("Revise Budget", "BUDGET", "BUDGET", RestaurantTileIcons.budget),
    Issue("Issue / Update", "ISSUE", "ISSUE", RestaurantTileIcons.issue),
}

// ---------------------------------------------------------------------------------
// Draft carried across the module boundary
// ---------------------------------------------------------------------------------

/**
 * Restaurant-side capture draft: exactly the fields the shared sheet produces plus the
 * optional master payload, so `POST …/operating-events` keeps its existing body.
 */
data class RestaurantEventDraft(
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
    val master: OperatingMasterBody? = null,
)

internal fun OperatingCaptureDraft.toRestaurantDraft() = RestaurantEventDraft(
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

// ---------------------------------------------------------------------------------
// Shared sheet chrome helpers (ember)
// ---------------------------------------------------------------------------------

@Composable
internal fun EmberSheetGrabber() {
    Row(Modifier.fillMaxWidth().height(22.dp), horizontalArrangement = Arrangement.Center) {
        Box(
            Modifier
                .width(40.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(100.dp))
                .background(RestaurantEmber.ember.copy(alpha = 0.55f)),
        )
    }
}

@Composable
internal fun EmberSheetHeader(title: String, subtitle: String?, onClose: () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            title,
            style = RestaurantEmber.screenTitle().copy(fontSize = 20.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Box(
            Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.06f))
                .clickable(onClick = onClose),
            contentAlignment = Alignment.Center,
        ) {
            Text("✕", style = RestaurantEmber.rowValue(RestaurantEmber.textBody).copy(fontSize = 12.sp))
        }
    }
    subtitle?.takeIf { it.isNotBlank() }?.let {
        Text(it, style = RestaurantEmber.rowCaption(), maxLines = 2)
    }
}

/** Field label above an input (`11/Medium` + tracking, as in every `RST-QA-*` frame). */
@Composable
internal fun EmberFieldLabel(text: String) {
    Text(
        text,
        style = RestaurantEmber.metricLabel().copy(fontSize = 10.sp, letterSpacing = 0.4.sp),
        maxLines = 1,
    )
}

/** `#44403c` text field row used by the POS / KOT / master forms. */
@Composable
internal fun EmberTextField(
    value: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    onValueChange: (String) -> Unit,
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(RestaurantEmber.chip)
            .border(1.dp, RestaurantEmber.hairline.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(
                color = RestaurantEmber.textPrimary,
                fontFamily = PlusJakartaSans,
                fontSize = 14.sp,
            ),
            cursorBrush = SolidColor(RestaurantEmber.ember),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(placeholder, style = RestaurantEmber.rowCaption(RestaurantEmber.textFaint))
                }
                inner()
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Disabled row that states exactly why the frame's control cannot be posted. */
@Composable
internal fun EmberLockedField(label: String, value: String, reason: String) {
    EmberFieldLabel(label)
    Row(
        Modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(RestaurantEmber.surfaceWell)
            .border(1.dp, RestaurantEmber.hairline.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(value, style = RestaurantEmber.rowTitle().copy(color = RestaurantEmber.textFaint), maxLines = 1)
        Spacer(Modifier.weight(1f))
        EmberSeverityChip("Unavailable", RestaurantEmber.warning)
    }
    Text(reason, style = RestaurantEmber.rowCaption(RestaurantEmber.amber.copy(alpha = 0.85f)).copy(fontSize = 10.sp))
}

/** Footer CTA + the frame's one-line caveat underneath. */
@Composable
internal fun EmberSheetFooter(
    label: String,
    enabled: Boolean,
    caveat: String,
    onClick: () -> Unit,
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        EmberPrimaryButton(label = label, enabled = enabled, onClick = onClick)
        Text(
            caveat,
            style = RestaurantEmber.rowCaption(RestaurantEmber.textFaint).copy(fontSize = 10.sp),
            maxLines = 2,
        )
    }
}

/** `Subtotal / GST / Service` style money row. */
@Composable
internal fun EmberTotalRow(
    label: String,
    value: String,
    valueColor: Color = RestaurantEmber.textPrimary,
    caption: String? = null,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(label, style = RestaurantEmber.rowCaption(RestaurantEmber.textBody).copy(fontSize = 12.sp))
            caption?.let {
                Text(it, style = RestaurantEmber.rowCaption(RestaurantEmber.textFaint).copy(fontSize = 10.sp))
            }
        }
        Text(value, style = RestaurantEmber.rowValue(valueColor).copy(fontSize = 14.sp))
    }
}

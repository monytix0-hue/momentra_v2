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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.api.OperatingActivityDto
import com.example.momentra.data.api.OperatingMasterDto
import com.example.momentra.data.api.OperatingProjectionDto
import com.example.momentra.ui.shell.business.shared.BusinessInviteQr
import com.example.momentra.ui.shell.business.shared.EmberInvitePalette
import com.example.momentra.ui.shell.business.shared.OperatingCaptureSheet
import com.example.momentra.ui.shell.business.shared.OperatingSheetScrim
import com.example.momentra.ui.theme.RestaurantEmber
import kotlinx.coroutines.launch

/**
 * Masters — Figma `RST-MASTER · Menu` `2465:30143`, `· Stations` `2465:30265`,
 * `· Vendors` `2465:30021`, `· Invite & QR` `2465:29655`.
 *
 * Rows come from `model.masters`, which the projection derives from posted events that
 * carried a `master` payload. A master cannot be created on its own, so the create action
 * opens the existing capture sheet (`UPDATE` + `masterKind`) — the documented way to
 * introduce a master ahead of a transaction — instead of pretending a CRUD endpoint exists.
 */
internal enum class RestaurantMastersTab(
    val title: String,
    val masterKind: String?,
    val singular: String,
    val icon: Int,
) {
    Menu("Menu", "MENU_DISH", "dish", RestaurantTileIcons.menu),
    Stations("Stations", "STATION", "station", RestaurantTileIcons.stations),
    Vendors("Vendors", "VENDOR", "vendor", RestaurantTileIcons.vendors),
    Invite("Invite & QR", null, "invite", RestaurantTileIcons.invite),
    ;

    /** Master `kind` aliases the projection may report for this tab. */
    val kinds: Set<String>
        get() = when (this) {
            Menu -> setOf("MENU_DISH", "DISH", "MENU_ITEM", "PRODUCT")
            Stations -> setOf("STATION", "KITCHEN_STATION")
            Vendors -> setOf("VENDOR", "SUPPLIER", "PRINCIPAL", "PARTNER")
            Invite -> emptySet()
        }

    val note: String
        get() = when (this) {
            Menu ->
                "A dish row changes only on an explicit master record — never from a posted bill."
            Stations ->
                "A station groups the pass. Adding one does not move an open ticket."
            Vendors ->
                "Named vendors are required for credit and payable tracking."
            Invite ->
                "Invite grants a role and its access. Joining ≠ approval ≠ payment."
        }
}

@Composable
internal fun RestaurantMastersSheet(
    tab: RestaurantMastersTab,
    model: OperatingProjectionDto,
    momentId: String?,
    onClose: () -> Unit,
    onRecord: suspend (RestaurantEventDraft) -> Unit,
    onError: (String) -> Unit,
) {
    var createOpen by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val rows = remember(model.masters, tab) {
        model.masters.orEmpty()
            .filter { tab.kinds.contains(it.kind.uppercase()) }
            .sortedBy { it.name.lowercase() }
    }
    val canCreate = tab != RestaurantMastersTab.Invite && model.role == "OWNER_ADMIN"

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = RestaurantEmber.gutter, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        EmberSheetGrabber()
        EmberSheetHeader(
            title = tab.title,
            subtitle = when (tab) {
                RestaurantMastersTab.Invite ->
                    model.title?.takeIf { it.isNotBlank() } ?: "this restaurant"
                else ->
                    "${rows.size} record${if (rows.size == 1) "" else "s"} · " +
                        (model.title?.takeIf { it.isNotBlank() } ?: "Restaurant")
            },
            onClose = onClose,
        )

        if (tab == RestaurantMastersTab.Invite) {
            BusinessInviteQr(
                momentId = momentId,
                title = model.title.orEmpty(),
                role = model.role,
                palette = EmberInvitePalette,
                iconRes = RestaurantTileIcons.invite,
                noun = "restaurant",
                onError = onError,
            )
        } else {
            if (canCreate) {
                Text(
                    "New ${tab.singular}",
                    style = RestaurantEmber.storyCtaTitle().copy(color = Color.White),
                    maxLines = 1,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RestaurantEmber.rCardTight)
                        .background(RestaurantEmber.ember)
                        .clickable { createOpen = true }
                        .padding(vertical = 14.dp),
                )
            }
            when (tab) {
                RestaurantMastersTab.Menu -> MenuList(rows, model)
                RestaurantMastersTab.Stations -> StationList(rows, model)
                else -> VendorList(rows, model)
            }
            EmberWhyNote(tab.note, tinted = true)
        }
        Spacer(Modifier.height(20.dp))
    }

    if (createOpen) {
        OperatingSheetScrim(
            onDismiss = { createOpen = false },
            accent = RestaurantEmber.ember,
            surface = RestaurantEmber.sheet,
            border = RestaurantEmber.divider,
        ) {
            OperatingCaptureSheet(
                title = "New ${tab.singular}",
                icon = tab.icon,
                eventType = "UPDATE",
                mode = "MASTERS",
                model = model,
                variant = "restaurant",
                onDismiss = { createOpen = false },
                onSubmit = { draft ->
                    scope.launch {
                        runCatching { onRecord(draft.toRestaurantDraft()) }
                            .onFailure { onError(it.message ?: "Could not save") }
                        createOpen = false
                    }
                },
            )
        }
    }
}

/** `2465:30143` — dish name + GST badge from `master.gstRate`. */
@Composable
private fun MenuList(rows: List<OperatingMasterDto>, model: OperatingProjectionDto) {
    if (rows.isEmpty()) {
        EmberEmpty(
            "Menu not set up",
            "Add a dish here and it becomes pickable on the POS bill and the kitchen ticket.",
        )
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { master ->
            val gst = master.gstRate?.takeIf { it.isNotBlank() }
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RestaurantEmber.rRow)
                    .background(RestaurantEmber.card)
                    .border(1.dp, RestaurantEmber.ember.copy(alpha = 0.16f), RestaurantEmber.rRow)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(RestaurantEmber.ember.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("◈", color = RestaurantEmber.ember, fontSize = 13.sp)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        master.name.ifBlank { "Unnamed dish" },
                        style = RestaurantEmber.rowTitle(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        listOfNotNull(
                            master.unit?.takeIf { it.isNotBlank() }?.let { "Serves / $it" },
                            master.hsn?.takeIf { it.isNotBlank() }?.let { "HSN $it" },
                            if (gst == null) "GST rate not set" else null,
                        ).joinToString(" · ").ifBlank { "No unit recorded" },
                        style = RestaurantEmber.rowCaption().copy(fontSize = 10.sp),
                        maxLines = 1,
                    )
                }
                if (gst != null) {
                    EmberSeverityChip("GST $gst%", RestaurantEmber.gold, bordered = false)
                } else {
                    EmberSeverityChip("No GST", RestaurantEmber.warning, bordered = false)
                }
            }
        }
    }
    Text(
        "Dish price is typed on each bill line — the master carries a GST rate, not a rate card.",
        style = RestaurantEmber.rowCaption(RestaurantEmber.textFaint).copy(fontSize = 10.sp),
    )
}

/** `2465:30265` — station + a live open-ticket count derived from `ORDER` rows. */
@Composable
private fun StationList(rows: List<OperatingMasterDto>, model: OperatingProjectionDto) {
    val open = openOrders(model)
    if (rows.isEmpty()) {
        EmberEmpty(
            "No station yet",
            "Add Tandoor / Hot / Cold / Bar so the pass knows where a ticket belongs.",
        )
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { master ->
            val tickets = open.count { ticketCarriesStation(it, master.name) }
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RestaurantEmber.rRow)
                    .background(RestaurantEmber.card)
                    .border(1.dp, RestaurantEmber.ember.copy(alpha = 0.16f), RestaurantEmber.rRow)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(RestaurantEmber.ember.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("⌘", color = RestaurantEmber.ember, fontSize = 13.sp)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        master.name.ifBlank { "Unnamed station" },
                        style = RestaurantEmber.rowTitle(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        if (tickets > 0) "$tickets open ticket${if (tickets == 1) "" else "s"}" else "No open ticket",
                        style = RestaurantEmber.rowCaption().copy(fontSize = 10.sp),
                        maxLines = 1,
                    )
                }
                EmberSeverityChip(
                    if (tickets > 0) "Firing" else "Idle",
                    if (tickets > 0) RestaurantEmber.amber else RestaurantEmber.textMuted,
                    bordered = false,
                )
            }
        }
    }
    Text(
        "Counted from posted orders whose status is not Completed or Void. The contract has no per-line station field.",
        style = RestaurantEmber.rowCaption(RestaurantEmber.textFaint).copy(fontSize = 10.sp),
    )
}

/** `2465:30021` — vendor name + GSTIN. */
@Composable
private fun VendorList(rows: List<OperatingMasterDto>, model: OperatingProjectionDto) {
    if (rows.isEmpty()) {
        EmberEmpty(
            "No vendor yet",
            "Add the suppliers you buy ingredients or packaging from.",
        )
        return
    }
    val payables = model.paymentReadiness.filter { it.relevance != "RECEIVABLE" }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { master ->
            val open = payables
                .filter { it.counterpartyName?.equals(master.name, ignoreCase = true) == true }
                .sumOf { it.amount.toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO }
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RestaurantEmber.rRow)
                    .background(RestaurantEmber.card)
                    .border(1.dp, RestaurantEmber.ember.copy(alpha = 0.16f), RestaurantEmber.rRow)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(RestaurantEmber.ember.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("◆", color = RestaurantEmber.ember, fontSize = 13.sp)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        master.name.ifBlank { "Unnamed vendor" },
                        style = RestaurantEmber.rowTitle(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        master.gstin?.takeIf { it.isNotBlank() }?.let { "GSTIN $it" } ?: "GSTIN not recorded",
                        style = RestaurantEmber.rowCaption().copy(fontSize = 10.sp),
                        maxLines = 1,
                    )
                }
                Text(
                    if (open.signum() > 0) {
                        RestaurantFormat.compact(model.currency, open.toPlainString(), model.financeVisible)
                    } else {
                        "Nil"
                    },
                    style = RestaurantEmber.rowValue(
                        if (open.signum() > 0) RestaurantEmber.amber else RestaurantEmber.textMuted,
                    ),
                    maxLines = 1,
                )
            }
        }
    }
    Text(
        "Open AP per vendor comes from unpaid purchase events — never estimated.",
        style = RestaurantEmber.rowCaption(RestaurantEmber.textFaint).copy(fontSize = 10.sp),
    )
}

/** A ticket belongs to a station when its note carries `Station: <name>`, as KOT Send writes it. */
private fun ticketCarriesStation(ticket: OperatingActivityDto, station: String): Boolean {
    val note = ticket.note.orEmpty()
    return note.contains("Station: $station", ignoreCase = true) ||
        ticket.channelName?.equals(station, ignoreCase = true) == true
}

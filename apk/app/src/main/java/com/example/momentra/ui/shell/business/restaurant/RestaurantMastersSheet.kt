package com.example.momentra.ui.shell.business.restaurant

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.api.OperatingActivityDto
import com.example.momentra.data.api.OperatingMasterDto
import com.example.momentra.data.api.OperatingProjectionDto
import com.example.momentra.ui.shell.business.shared.BusinessInviteQr
import com.example.momentra.ui.shell.business.shared.EmberInvitePalette
import com.example.momentra.ui.shell.business.shared.OperatingSheetScrim
import com.example.momentra.ui.shell.business.shared.RestaurantInviteRoles
import com.example.momentra.ui.shell.business.shared.restaurantInviteRoleAccess
import com.example.momentra.ui.theme.PlusJakartaSans
import com.example.momentra.ui.theme.RestaurantEmber
import kotlinx.coroutines.launch

/**
 * Masters — Figma `RST-MASTER · Menu` `2465:30143`, `· Stations` `2465:30265`,
 * `· Vendors` `2465:30021`, `· Invite & QR` `2465:29655`.
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

    val kinds: Set<String>
        get() = when (this) {
            Menu -> setOf("MENU_DISH", "DISH", "MENU_ITEM", "PRODUCT")
            Stations -> setOf("STATION", "KITCHEN_STATION")
            Vendors -> setOf("VENDOR", "SUPPLIER", "PRINCIPAL", "PARTNER")
            Invite -> emptySet()
        }

    val note: String
        get() = when (this) {
            Menu -> RestaurantCaptureCopy.MENU_WHY
            Stations -> RestaurantCaptureCopy.STATIONS_WHY
            Vendors -> RestaurantCaptureCopy.VENDORS_WHY
            Invite -> RestaurantCaptureCopy.INVITE_WHY
        }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun RestaurantMastersSheet(
    tab: RestaurantMastersTab,
    model: OperatingProjectionDto,
    momentId: String?,
    onClose: () -> Unit,
    onRecord: suspend (RestaurantEventDraft) -> Unit,
    onError: (String) -> Unit,
) {
    var formKind by remember { mutableStateOf<RestaurantMasterFormKind?>(null) }
    var editing by remember { mutableStateOf<OperatingMasterDto?>(null) }
    var category by remember { mutableStateOf("Mains") }
    var vendorQuery by remember { mutableStateOf("") }
    var toast by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val brand = RestaurantCaptureCopy.brandOnly(model.title)
    val kitchen = RestaurantCaptureCopy.kitchenLine(model.title, model.location)
    val owner = RestaurantCaptureCopy.ownerHint(null)

    val rows = remember(model.masters, tab) {
        model.masters.orEmpty()
            .filter { tab.kinds.contains(it.kind.uppercase()) }
            .sortedBy { it.name.lowercase() }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        EmberSheetGrabber()
        EmberSheetHeader(
            title = when (tab) {
                RestaurantMastersTab.Invite -> "Invite team · $brand"
                else -> tab.title
            },
            subtitle = when (tab) {
                RestaurantMastersTab.Invite ->
                    "$kitchen · $owner (Owner) · invite grants a role"
                RestaurantMastersTab.Vendors ->
                    "Supplier master · GSTIN drives input credit on Purchase / GRN"
                RestaurantMastersTab.Menu ->
                    "Menu master · dishes with price + GST before they reach SO / POS"
                RestaurantMastersTab.Stations ->
                    "Kitchen & service stations · KOT routing targets"
            },
            onClose = onClose,
        )

        when (tab) {
            RestaurantMastersTab.Invite -> {
                BusinessInviteQr(
                    momentId = momentId,
                    title = brand,
                    role = model.role,
                    palette = EmberInvitePalette,
                    iconRes = RestaurantTileIcons.invite,
                    noun = "restaurant",
                    onError = onError,
                    roles = RestaurantInviteRoles,
                    roleAccess = { code ->
                        val label = RestaurantInviteRoles.firstOrNull { it.first == code }?.second.orEmpty()
                        restaurantInviteRoleAccess(code, label)
                    },
                    showIntroHeader = false,
                    defaultRoleCode = "OPERATIONS_LEAD",
                )
            }
            RestaurantMastersTab.Vendors -> VendorsList(
                rows = rows,
                query = vendorQuery,
                onQuery = { vendorQuery = it },
                onNew = { editing = null; formKind = RestaurantMasterFormKind.Vendor },
                onEdit = { master -> editing = master; formKind = RestaurantMasterFormKind.Vendor },
            )
            RestaurantMastersTab.Menu -> MenuList(
                rows = rows,
                category = category,
                currency = model.currency ?: "INR",
                onCategory = { category = it },
                onNew = { editing = null; formKind = RestaurantMasterFormKind.Dish },
                onEdit = { master -> editing = master; formKind = RestaurantMasterFormKind.Dish },
            )
            RestaurantMastersTab.Stations -> StationList(
                rows = rows,
                model = model,
                onAdd = { editing = null; formKind = RestaurantMasterFormKind.Station },
                onEdit = { master -> editing = master; formKind = RestaurantMasterFormKind.Station },
            )
        }

        EmberWhyNote(tab.note, tinted = true)

        EmberSheetFooter(
            label = when (tab) {
                RestaurantMastersTab.Invite, RestaurantMastersTab.Menu -> "Done"
                RestaurantMastersTab.Vendors -> "Save vendor"
                RestaurantMastersTab.Stations -> "Save stations"
            },
            enabled = true,
            caveat = when (tab) {
                RestaurantMastersTab.Invite -> "3 invites pending · roles apply on join"
                RestaurantMastersTab.Vendors -> "Vendor saved · available in Purchase Order"
                RestaurantMastersTab.Menu -> {
                    val shown = if (rows.isEmpty()) demoMenu().size else rows.size
                    "$shown of ${if (rows.isEmpty()) 38 else maxOf(shown, 38)} dishes · $category"
                }
                RestaurantMastersTab.Stations -> "Stations saved · KOT routing updates"
            },
            onClick = {
                toast = RestaurantCaptureCopy.SAVED_TOAST
                onClose()
            },
        )
        toast?.let { EmberSavedToast(it) }
        Spacer(Modifier.height(12.dp))
    }

    formKind?.let { kind ->
        OperatingSheetScrim(
            onDismiss = { formKind = null; editing = null },
            accent = RestaurantEmber.ember,
            surface = RestaurantEmber.card,
            border = RestaurantEmber.sheetBorder,
            topCorner = RestaurantEmber.sheetTopCorner,
        ) {
            RestaurantMasterFormSheet(
                kind = kind,
                model = model,
                existing = editing,
                onDismiss = { formKind = null; editing = null },
                onSubmit = { draft ->
                    scope.launch {
                        runCatching { onRecord(draft) }
                            .onFailure { onError(it.message ?: "Could not save") }
                        formKind = null
                        editing = null
                    }
                },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MenuList(
    rows: List<OperatingMasterDto>,
    category: String,
    currency: String,
    onCategory: (String) -> Unit,
    onNew: () -> Unit,
    onEdit: (OperatingMasterDto?) -> Unit,
) {
    val source = rows.ifEmpty { demoMenu() }
    val filtered = source.filter {
        category.equals("Mains", true) ||
            it.name.contains(category, true) ||
            it.dimension?.equals(category, true) == true ||
            categoryNote(it).contains(category, true)
    }.ifEmpty { source }

    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        RestaurantCaptureCopy.menuCategories.forEach {
            EmberModeChip(it, selected = it == category, onClick = { onCategory(it) })
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        filtered.take(8).forEach { master ->
            val gst = master.gstRate?.takeIf { it.isNotBlank() } ?: "5"
            val price = master.sellPrice?.takeIf { it.isNotBlank() }
            val veg = !master.name.contains("chicken", true) && !master.name.contains("mutton", true)
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(RestaurantEmber.chip)
                    .border(1.dp, RestaurantEmber.hairline, RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (veg) RestaurantEmber.positive else Color(0xFFF43F5E)),
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(master.name, style = RestaurantEmber.rowTitle().copy(fontSize = 14.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(categoryNote(master), style = RestaurantEmber.rowCaption().copy(fontSize = 11.sp), maxLines = 1)
                }
                if (price != null) {
                    Text(
                        RestaurantFormat.exact(currency, price),
                        style = RestaurantEmber.rowValue(RestaurantEmber.gold).copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                    )
                }
                EmberSeverityChip("GST $gst%", RestaurantEmber.wine, bordered = false)
                Text(
                    "Edit",
                    style = RestaurantEmber.rowValue().copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(RestaurantEmber.card)
                        .border(1.dp, RestaurantEmber.hairline, RoundedCornerShape(10.dp))
                        .clickable { onEdit(if (rows.isEmpty()) null else master) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }
    }
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(RestaurantEmber.chip)
            .border(1.dp, RestaurantEmber.gold.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .padding(14.dp),
    ) {
        Text(
            "Edit opens Create Menu Dish — dish create/edit is a master write; SO and POS only pick saved dishes.",
            style = RestaurantEmber.rowCaption(RestaurantEmber.textPrimary).copy(fontSize = 12.sp),
        )
    }
    Text(
        "+ New dish",
        style = RestaurantEmber.rowValue(RestaurantEmber.gold).copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, RestaurantEmber.ember, RoundedCornerShape(10.dp))
            .clickable(onClick = onNew)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

@Composable
private fun VendorsList(
    rows: List<OperatingMasterDto>,
    query: String,
    onQuery: (String) -> Unit,
    onNew: () -> Unit,
    onEdit: (OperatingMasterDto) -> Unit,
) {
    val source = rows.ifEmpty { demoVendors() }
    val filtered = source.filter {
        query.isBlank() ||
            it.name.contains(query, true) ||
            it.gstin.orEmpty().contains(query, true)
    }
    EmberFieldLabel("Search")
    Box(
        Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(RestaurantEmber.chip)
            .border(1.dp, RestaurantEmber.hairline, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        BasicTextField(
            value = query,
            onValueChange = onQuery,
            singleLine = true,
            textStyle = TextStyle(color = RestaurantEmber.textPrimary, fontFamily = PlusJakartaSans, fontSize = 14.sp),
            cursorBrush = SolidColor(RestaurantEmber.ember),
            decorationBox = { inner ->
                if (query.isEmpty()) {
                    Text("Search vendors, GSTIN or phone", style = RestaurantEmber.rowCaption(RestaurantEmber.textFaint))
                }
                inner()
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        filtered.forEachIndexed { index, master ->
            val preferred = index == 0 || master.status.equals("PREFERRED", true) ||
                master.name.contains("Fresh Farm", true)
            val cat = vendorCategory(master)
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(RestaurantEmber.chip)
                    .border(
                        1.dp,
                        if (preferred) RestaurantEmber.ember else RestaurantEmber.hairline,
                        RoundedCornerShape(14.dp),
                    )
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(master.name, style = RestaurantEmber.rowTitle().copy(fontSize = 14.sp), maxLines = 1)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        EmberSeverityChip(cat, RestaurantEmber.textMuted, bordered = false)
                        if (preferred) EmberSeverityChip("★ Preferred", RestaurantEmber.gold, bordered = false)
                    }
                    Text(
                        listOfNotNull(
                            master.gstin?.takeIf { it.isNotBlank() }?.let { "GSTIN $it" },
                            "phone" /* fixture chrome */,
                        ).joinToString(" · ").replace("phone", vendorPhone(master)),
                        style = RestaurantEmber.rowCaption().copy(fontSize = 11.sp),
                        maxLines = 1,
                    )
                }
                if (!preferred || rows.isNotEmpty()) {
                    Text(
                        "Edit",
                        style = RestaurantEmber.rowValue().copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(RestaurantEmber.card)
                            .border(1.dp, RestaurantEmber.hairline, RoundedCornerShape(10.dp))
                            .clickable { if (rows.isNotEmpty()) onEdit(master) else onNew() }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
    Text(
        "+ New vendor",
        style = RestaurantEmber.rowValue(RestaurantEmber.gold).copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, RestaurantEmber.ember, RoundedCornerShape(10.dp))
            .clickable(onClick = onNew)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

@Composable
private fun StationList(
    rows: List<OperatingMasterDto>,
    model: OperatingProjectionDto,
    onAdd: () -> Unit,
    onEdit: (OperatingMasterDto) -> Unit,
) {
    val open = openOrders(model)
    val source = rows.ifEmpty { demoStations() }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        source.forEach { master ->
            val tickets = if (rows.isEmpty()) {
                demoTicketCount(master.name)
            } else {
                open.count { ticketCarriesStation(it, master.name) }
            }
            val busy = tickets >= 4
            val type = stationTypeLabel(master)
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(RestaurantEmber.chip)
                    .border(
                        1.dp,
                        if (busy) RestaurantEmber.ember else RestaurantEmber.hairline,
                        RoundedCornerShape(14.dp),
                    )
                    .clickable { if (rows.isNotEmpty()) onEdit(master) else onAdd() }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(master.name, style = RestaurantEmber.rowTitle().copy(fontSize = 14.sp), maxLines = 1)
                    Text(
                        "$type · $tickets open ticket${if (tickets == 1) "" else "s"}",
                        style = RestaurantEmber.rowCaption().copy(fontSize = 11.sp),
                        maxLines = 1,
                    )
                }
                EmberSeverityChip(
                    if (busy) "Busy" else "Open",
                    if (busy) RestaurantEmber.negative else RestaurantEmber.amber,
                    bordered = false,
                )
            }
        }
    }
    Text(
        "+ Add station",
        style = RestaurantEmber.rowValue(RestaurantEmber.gold).copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, RestaurantEmber.ember, RoundedCornerShape(10.dp))
            .clickable(onClick = onAdd)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

private fun categoryNote(master: OperatingMasterDto): String {
    val dim = master.dimension?.takeIf { it.isNotBlank() }
    val station = when {
        master.name.contains("naan", true) || master.name.contains("bread", true) -> "Breads · Tandoor"
        master.name.contains("lassi", true) || master.name.contains("drink", true) -> "Drinks · Bar"
        else -> "Mains · Tandoor"
    }
    return dim?.let { "$it · Tandoor" } ?: station
}

private fun vendorCategory(master: OperatingMasterDto): String = when {
    master.name.contains("farm", true) || master.name.contains("produce", true) -> "Produce"
    master.name.contains("dairy", true) -> "Dairy"
    master.name.contains("spice", true) || master.name.contains("dry", true) -> "Dry"
    master.name.contains("fish", true) || master.name.contains("meat", true) -> "Meat & Fish"
    else -> "Produce"
}

private fun vendorPhone(master: OperatingMasterDto): String = when {
    master.name.contains("Fresh", true) -> "+91 98201 22334"
    master.name.contains("Metro", true) -> "+91 98922 55667"
    master.name.contains("Spice", true) -> "+91 99670 88990"
    else -> "+91 90000 00000"
}

private fun stationTypeLabel(master: OperatingMasterDto): String = when {
    master.name.equals("Bar", true) || master.name.equals("Floor", true) -> "Service"
    master.kind.equals("SERVICE", true) -> "Service"
    else -> "Kitchen"
}

private fun demoTicketCount(name: String): Int = when (name) {
    "Tandoor" -> 6
    "Hot" -> 3
    "Cold" -> 1
    "Bar" -> 4
    "Floor" -> 2
    else -> 0
}

private fun demoMenu(): List<OperatingMasterDto> = listOf(
    OperatingMasterDto(id = "d1", kind = "MENU_DISH", name = "Butter Chicken", sellPrice = "480", gstRate = "5", unit = "plate"),
    OperatingMasterDto(id = "d2", kind = "MENU_DISH", name = "Paneer Tikka Masala", sellPrice = "420", gstRate = "5", unit = "plate"),
    OperatingMasterDto(id = "d3", kind = "MENU_DISH", name = "Dal Makhani", sellPrice = "340", gstRate = "5", unit = "plate"),
    OperatingMasterDto(id = "d4", kind = "MENU_DISH", name = "Garlic Naan", sellPrice = "90", gstRate = "5", unit = "pcs", dimension = "Breads"),
)

private fun demoVendors(): List<OperatingMasterDto> = listOf(
    OperatingMasterDto(id = "v1", kind = "VENDOR", name = "Fresh Farm", gstin = "27AABCF1234K1Z5", status = "PREFERRED"),
    OperatingMasterDto(id = "v2", kind = "VENDOR", name = "Metro Dairy", gstin = "27AAECM5678L1Z2"),
    OperatingMasterDto(id = "v3", kind = "VENDOR", name = "Spice Hub", gstin = "27AAHCS9012M1Z9"),
)

private fun demoStations(): List<OperatingMasterDto> = listOf(
    OperatingMasterDto(id = "s1", kind = "STATION", name = "Tandoor"),
    OperatingMasterDto(id = "s2", kind = "STATION", name = "Hot"),
    OperatingMasterDto(id = "s3", kind = "STATION", name = "Cold"),
    OperatingMasterDto(id = "s4", kind = "STATION", name = "Bar"),
    OperatingMasterDto(id = "s5", kind = "STATION", name = "Floor"),
)

private fun ticketCarriesStation(ticket: OperatingActivityDto, station: String): Boolean {
    val note = ticket.note.orEmpty()
    return note.contains("Station: $station", ignoreCase = true) ||
        ticket.channelName?.equals(station, ignoreCase = true) == true
}

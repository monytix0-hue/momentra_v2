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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.api.OperatingMasterDto
import com.example.momentra.data.api.OperatingProjectionDto
import com.example.momentra.ui.shell.business.shared.BusinessInviteQr
import com.example.momentra.ui.shell.business.shared.InvitePalette
import com.example.momentra.ui.shell.business.shared.OperatingSheetScrim
import com.example.momentra.ui.shell.business.shared.ServiceInviteRoles
import com.example.momentra.ui.shell.business.shared.serviceInviteRoleAccess
import com.example.momentra.ui.theme.PlusJakartaSans
import com.example.momentra.ui.theme.ServicesTeal

/**
 * SVC masters lists — Figma `2530:29334` Invite & QR, `2530:29476` Services,
 * `2530:29418` Clients, `2530:29556` Rooms & Staff.
 *
 * Structural port of [com.example.momentra.ui.shell.business.restaurant.RestaurantMastersSheet]
 * with teal/lime chrome and BrightCare fixture rows when projection masters are empty.
 */
enum class ServiceMastersTab(
    val title: String,
    val masterKind: String?,
    val icon: Int,
) {
    Clients("Clients", "CUSTOMER", ServiceTileIcons.suppliers),
    Services("Services", "PRODUCT", ServiceTileIcons.catalogue),
    RoomsStaff("Rooms & Staff", "LOCATION", ServiceTileIcons.locations),
    Invite("Invite & QR", null, ServiceTileIcons.invite),
    ;

    val kinds: Set<String>
        get() = when (this) {
            Clients -> setOf("CUSTOMER", "CLIENT", "GUEST")
            Services -> setOf("PRODUCT", "ITEM", "SERVICE", "PACKAGE", "CONSUMABLE")
            RoomsStaff -> setOf("LOCATION", "CHANNEL", "STAFF", "ROOM", "CHAIR")
            Invite -> emptySet()
        }

    val why: String
        get() = when (this) {
            Invite ->
                "Invite grants a role and its access. Joining ≠ approval ≠ payment."
            Services ->
                "Catalogue lists saved offerings. GST badge is the SAC rate — output GST posts on Finalize Charge, never on booking save."
            Clients ->
                "Client is a master write. Booking picks a saved client; GST posts on Finalize Charge, never on booking save."
            RoomsStaff ->
                "Rooms and staff are capacity. Booking holds a slot; only Finalize Charge creates revenue."
        }
}

internal val TealInvitePalette = InvitePalette(
    accent = ServicesTeal.teal,
    accentMuted = ServicesTeal.tealMuted,
    card = ServicesTeal.card,
    field = ServicesTeal.field,
    chip = ServicesTeal.chip,
    chipStroke = ServicesTeal.teal,
    divider = ServicesTeal.divider,
    qrPlate = ServicesTeal.card,
    textPrimary = ServicesTeal.textPrimary,
    textBody = ServicesTeal.textBody,
    textMuted = ServicesTeal.textMuted,
    textFaint = ServicesTeal.grab,
    warning = ServicesTeal.warning,
    negative = ServicesTeal.negative,
    chipSelected = ServicesTeal.lime,
    chipSelectedInk = ServicesTeal.inkOnTeal,
    chipSelectedStroke = ServicesTeal.lime,
)

private val ServiceCategories = listOf("Hair", "Skin", "Spa", "Wellness", "Nails")

@Composable
@Suppress("UNUSED_PARAMETER")
internal fun ServiceMastersSheet(
    tab: ServiceMastersTab,
    model: OperatingProjectionDto,
    momentId: String?,
    onClose: () -> Unit,
    onRecord: suspend (ServiceEventDraft) -> Unit,
    onError: (String) -> Unit,
) {
    var formKind by remember { mutableStateOf<ServiceMasterFormKind?>(null) }
    var editing by remember { mutableStateOf<OperatingMasterDto?>(null) }
    var category by remember { mutableStateOf("Hair") }
    var clientQuery by remember { mutableStateOf("") }
    var toast by remember { mutableStateOf<String?>(null) }
    val brand = model.title?.takeIf { it.isNotBlank() } ?: "BrightCare"

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
        TealSheetGrabber()
        TealSheetHeader(
            title = when (tab) {
                ServiceMastersTab.Invite -> "Invite team · $brand"
                else -> tab.title
            },
            subtitle = when (tab) {
                ServiceMastersTab.Invite ->
                    "$brand · ${ownerLine(model)} · invite grants a role"
                ServiceMastersTab.Services ->
                    "Services catalogue · offerings & packages with price, duration + GST"
                ServiceMastersTab.Clients ->
                    "Client master · guests & business clients · GSTIN only for B2B invoices"
                ServiceMastersTab.RoomsStaff ->
                    "Rooms, chairs & staff · where and by whom services are delivered"
            },
            onClose = onClose,
        )

        when (tab) {
            ServiceMastersTab.Invite -> {
                BusinessInviteQr(
                    momentId = momentId,
                    title = brand,
                    role = model.role,
                    palette = TealInvitePalette,
                    iconRes = ServiceTileIcons.invite,
                    noun = "service",
                    onError = onError,
                    roles = ServiceInviteRoles,
                    roleAccess = ::serviceInviteRoleAccess,
                    showIntroHeader = false,
                    defaultRoleCode = "OPERATIONS_LEAD",
                )
            }
            ServiceMastersTab.Services -> ServicesCatalogue(
                rows = rows,
                category = category,
                onCategory = { category = it },
                onNewService = {
                    editing = null
                    formKind = ServiceMasterFormKind.Service
                },
                onNewPackage = {
                    editing = null
                    formKind = ServiceMasterFormKind.Package
                },
                onEdit = { master ->
                    editing = master
                    formKind = if (master?.kind.equals("PACKAGE", ignoreCase = true) == true) {
                        ServiceMasterFormKind.Package
                    } else {
                        ServiceMasterFormKind.Service
                    }
                },
            )
            ServiceMastersTab.Clients -> ClientsList(
                rows = rows,
                query = clientQuery,
                onQuery = { clientQuery = it },
                onNew = {
                    editing = null
                    formKind = ServiceMasterFormKind.Client
                },
                onEdit = { master ->
                    editing = master
                    formKind = ServiceMasterFormKind.Client
                },
            )
            ServiceMastersTab.RoomsStaff -> RoomsStaffList(
                rows = rows,
                onAdd = {
                    editing = null
                    formKind = ServiceMasterFormKind.RoomStaff
                },
                onEdit = { master ->
                    editing = master
                    formKind = ServiceMasterFormKind.RoomStaff
                },
            )
        }

        WhyNote(tab.why, tinted = true)

        TealGradientCta(
            label = when (tab) {
                ServiceMastersTab.Invite, ServiceMastersTab.Services -> "Done"
                ServiceMastersTab.Clients -> "Save client"
                ServiceMastersTab.RoomsStaff -> "Save rooms"
            },
            caption = when (tab) {
                ServiceMastersTab.Invite -> "3 invites pending · roles apply on join"
                ServiceMastersTab.Services -> {
                    val shown = if (rows.isEmpty()) demoServices().size else rows.size
                    "$shown of ${if (rows.isEmpty()) 26 else maxOf(shown, 26)} services · $category"
                }
                ServiceMastersTab.Clients -> "Client saved · available in Booking"
                ServiceMastersTab.RoomsStaff -> "Rooms saved · calendar slots update"
            },
            onClick = {
                toast = "✓ Saved · sheet closes and returns back"
                onClose()
            },
        )

        toast?.let { TealSavedToast() }
        Spacer(Modifier.height(12.dp))
    }

    formKind?.let { kind ->
        OperatingSheetScrim(
            onDismiss = {
                formKind = null
                editing = null
            },
            accent = ServicesTeal.teal,
            surface = ServicesTeal.sheet,
            border = ServicesTeal.sheetBorder,
        ) {
            ServiceMasterFormSheet(
                kind = kind,
                model = model,
                momentId = momentId,
                existing = editing,
                onDismiss = {
                    formKind = null
                    editing = null
                },
                onSaved = {
                    toast = "✓ Saved · sheet closes and returns back"
                    formKind = null
                    editing = null
                },
                onError = onError,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ServicesCatalogue(
    rows: List<OperatingMasterDto>,
    category: String,
    onCategory: (String) -> Unit,
    onNewService: () -> Unit,
    onNewPackage: () -> Unit,
    onEdit: (OperatingMasterDto?) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        ServiceCategories.forEach { chip ->
            TealModeChip(
                text = chip,
                selected = chip == category,
                limeSelected = true,
                onClick = { onCategory(chip) },
            )
        }
    }

    if (rows.isEmpty()) {
        // Figma Hair selection still shows the fixture set (incl. Skin facial).
        val items = demoServices()
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items.forEach { row ->
                ServiceOfferingRow(row = row, onEdit = { onEdit(null) })
            }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            rows.forEach { master ->
                val row = ServiceRowUi(
                    name = master.name.ifBlank { "Unnamed service" },
                    meta = listOfNotNull(
                        categoryFrom(master) ?: category,
                        master.unit?.takeIf { it.isNotBlank() }
                            ?: "45 min",
                    ).joinToString(" · "),
                    price = master.sellPrice?.takeIf { it.isNotBlank() }?.let { "₹$it" } ?: "—",
                    gst = master.gstRate?.takeIf { it.isNotBlank() }?.let { "GST $it%" } ?: "GST —",
                    accent = ServicesTeal.lime,
                )
                ServiceOfferingRow(row = row, onEdit = { onEdit(master) })
            }
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlineAction("+ New service", onClick = onNewService)
        OutlineAction("+ New package", onClick = onNewPackage)
    }
}

@Composable
private fun ClientsList(
    rows: List<OperatingMasterDto>,
    query: String,
    onQuery: (String) -> Unit,
    onNew: () -> Unit,
    onEdit: (OperatingMasterDto?) -> Unit,
) {
    FieldLabel("Search")
    SearchField(
        value = query,
        placeholder = "Search clients, phone or GSTIN",
        onValueChange = onQuery,
    )

    if (rows.isEmpty()) {
        val items = demoClients().filter {
            query.isBlank() ||
                it.name.contains(query, ignoreCase = true) ||
                it.caption.contains(query, ignoreCase = true)
        }
        if (items.isEmpty()) {
            TealEmpty("No clients yet", "Named clients are required for credit and booking picks.")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items.forEach { client ->
                    ClientCard(client = client, onEdit = { onEdit(null) })
                }
            }
        }
    } else {
        val paired = rows.mapIndexed { index, master ->
            master to ClientRowUi(
                name = master.name.ifBlank { "Unnamed client" },
                badge = when {
                    master.gstin?.isNotBlank() == true -> "Corporate"
                    index == 0 -> "Regular"
                    else -> "New"
                },
                vip = index == 0,
                caption = listOfNotNull(
                    master.gstin?.takeIf { it.isNotBlank() }?.let { "GSTIN $it" },
                    master.code?.takeIf { it.isNotBlank() },
                ).joinToString(" · ").ifBlank { "Saved client" },
                selected = index == 0,
            )
        }.filter { (_, ui) ->
            query.isBlank() ||
                ui.name.contains(query, ignoreCase = true) ||
                ui.caption.contains(query, ignoreCase = true)
        }
        if (paired.isEmpty()) {
            TealEmpty("No clients yet", "Named clients are required for credit and booking picks.")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                paired.forEach { (master, client) ->
                    ClientCard(client = client, onEdit = { onEdit(master) })
                }
            }
        }
    }
    OutlineAction("+ New client", onClick = onNew)
}

@Composable
private fun RoomsStaffList(
    rows: List<OperatingMasterDto>,
    onAdd: () -> Unit,
    onEdit: (OperatingMasterDto?) -> Unit,
) {
    if (rows.isEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            demoRooms().forEach { room ->
                RoomStaffRow(room = room, onEdit = { onEdit(null) })
            }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            rows.forEachIndexed { index, master ->
                RoomStaffRow(
                    room = RoomRowUi(
                        name = master.name.ifBlank { "Unnamed capacity" },
                        caption = listOfNotNull(
                            master.kind.lowercase().replaceFirstChar { it.uppercase() },
                            master.unit?.takeIf { it.isNotBlank() },
                        ).joinToString(" · ").ifBlank { "Capacity" },
                        busy = index % 2 == 0,
                    ),
                    onEdit = { onEdit(master) },
                )
            }
        }
    }
    OutlineAction("+ Add room / staff", onClick = onAdd)
}

@Composable
private fun ServiceOfferingRow(row: ServiceRowUi, onEdit: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(ServicesTeal.chip)
            .border(1.dp, ServicesTeal.teal, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(row.accent),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                row.name,
                style = ServicesTeal.rowTitle().copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(row.meta, style = ServicesTeal.rowCaption(ServicesTeal.grab).copy(fontSize = 11.sp), maxLines = 1)
        }
        Text(
            row.price,
            style = ServicesTeal.rowValue(ServicesTeal.whyBody).copy(fontSize = 14.sp),
            maxLines = 1,
        )
        Text(
            row.gst,
            style = ServicesTeal.severity().copy(color = ServicesTeal.whyBody),
            maxLines = 1,
            modifier = Modifier
                .clip(ServicesTeal.rPill)
                .background(ServicesTeal.chip)
                .padding(horizontal = 8.dp, vertical = 4.dp),
        )
        OutlineAction("Edit", compact = true, onClick = onEdit)
    }
}

@Composable
private fun ClientCard(client: ClientRowUi, onEdit: () -> Unit) {
    val border = if (client.selected) ServicesTeal.lime else ServicesTeal.teal
    Column(
        Modifier
            .fillMaxWidth()
            .then(
                if (client.selected) {
                    Modifier.shadow(
                        elevation = 10.dp,
                        shape = RoundedCornerShape(14.dp),
                        clip = false,
                        ambientColor = ServicesTeal.lime.copy(alpha = 0.25f),
                        spotColor = ServicesTeal.lime.copy(alpha = 0.25f),
                    )
                } else Modifier,
            )
            .clip(RoundedCornerShape(14.dp))
            .background(ServicesTeal.chip)
            .border(1.dp, border, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                client.name,
                style = ServicesTeal.rowTitle().copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                client.badge,
                style = ServicesTeal.severity().copy(color = ServicesTeal.textPrimary),
                modifier = Modifier
                    .clip(ServicesTeal.rPill)
                    .background(ServicesTeal.teal)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
            Spacer(Modifier.weight(1f))
            if (client.vip) {
                Text(
                    "★ VIP",
                    style = ServicesTeal.severity().copy(color = ServicesTeal.inkOnTeal),
                    modifier = Modifier
                        .clip(ServicesTeal.rPill)
                        .background(ServicesTeal.whyBody)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
            OutlineAction("Edit", compact = true, onClick = onEdit)
        }
        Text(client.caption, style = ServicesTeal.rowCaption(ServicesTeal.grab).copy(fontSize = 11.sp), maxLines = 1)
    }
}

@Composable
private fun RoomStaffRow(room: RoomRowUi, onEdit: () -> Unit) {
    val border = if (room.busy) ServicesTeal.lime else ServicesTeal.teal
    Row(
        Modifier
            .fillMaxWidth()
            .then(
                if (room.busy) {
                    Modifier.shadow(
                        elevation = 10.dp,
                        shape = RoundedCornerShape(14.dp),
                        clip = false,
                        ambientColor = ServicesTeal.lime.copy(alpha = 0.25f),
                        spotColor = ServicesTeal.lime.copy(alpha = 0.25f),
                    )
                } else Modifier,
            )
            .clip(RoundedCornerShape(14.dp))
            .background(ServicesTeal.chip)
            .border(1.dp, border, RoundedCornerShape(14.dp))
            .clickable(onClick = onEdit)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                room.name,
                style = ServicesTeal.rowTitle().copy(fontWeight = FontWeight.Bold, fontSize = 15.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(room.caption, style = ServicesTeal.rowCaption(ServicesTeal.grab).copy(fontSize = 11.sp), maxLines = 1)
        }
        Text(
            if (room.busy) "Busy" else "Open",
            style = ServicesTeal.severity().copy(
                color = if (room.busy) ServicesTeal.textPrimary else ServicesTeal.inkOnTeal,
            ),
            modifier = Modifier
                .clip(ServicesTeal.rPill)
                .background(if (room.busy) ServicesTeal.lime else ServicesTeal.whyBody)
                .padding(horizontal = 8.dp, vertical = 4.dp),
        )
        OutlineAction("Edit", compact = true, onClick = onEdit)
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text,
        style = ServicesTeal.metricLabel().copy(color = ServicesTeal.grab, fontSize = 11.sp),
        maxLines = 1,
    )
}

@Composable
private fun SearchField(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(ServicesTeal.chip)
            .border(1.dp, ServicesTeal.teal, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(
                color = ServicesTeal.textPrimary,
                fontFamily = PlusJakartaSans,
                fontSize = 14.sp,
            ),
            cursorBrush = SolidColor(ServicesTeal.teal),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(placeholder, color = ServicesTeal.grab, fontFamily = PlusJakartaSans, fontSize = 14.sp)
                }
                inner()
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun OutlineAction(
    label: String,
    onClick: () -> Unit,
    compact: Boolean = false,
) {
    Text(
        label,
        style = ServicesTeal.rowValue(
            if (label.startsWith("+")) ServicesTeal.whyBody else ServicesTeal.textPrimary,
        ).copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(ServicesTeal.card)
            .border(
                1.dp,
                if (label.startsWith("+")) ServicesTeal.lime else ServicesTeal.teal,
                RoundedCornerShape(10.dp),
            )
            .clickable(onClick = onClick)
            .padding(
                horizontal = if (compact) 12.dp else 12.dp,
                vertical = if (compact) 8.dp else 8.dp,
            ),
    )
}

private fun ownerLine(model: OperatingProjectionDto): String {
    val role = when (model.role) {
        "OWNER_ADMIN" -> "Owner"
        "FINANCE" -> "Finance"
        "APPROVER" -> "Manager"
        "OPERATIONS_LEAD" -> "Stylist"
        else -> "Member"
    }
    return "Kiran Rao ($role)"
}

private fun categoryFrom(master: OperatingMasterDto): String? =
    master.dimension?.takeIf { it.isNotBlank() }
        ?: master.code?.takeIf { ServiceCategories.any { c -> it.equals(c, ignoreCase = true) } }

private data class ServiceRowUi(
    val name: String,
    val meta: String,
    val price: String,
    val gst: String,
    val accent: Color,
)

private data class ClientRowUi(
    val name: String,
    val badge: String,
    val vip: Boolean,
    val caption: String,
    val selected: Boolean,
)

private data class RoomRowUi(
    val name: String,
    val caption: String,
    val busy: Boolean,
)

/** BrightCare fixtures for visual QA when projection masters are empty. */
private fun demoServices(): List<ServiceRowUi> = listOf(
    ServiceRowUi("Haircut & style", "Hair · 45 min", "₹899", "GST 18%", ServicesTeal.lime),
    ServiceRowUi("Hair colour (global)", "Hair · 45 min", "₹2,499", "GST 18%", ServicesTeal.positive),
    ServiceRowUi("Keratin package · 3 sessions", "Hair · 45 min", "₹7,999", "GST 18%", ServicesTeal.positive),
    ServiceRowUi("Deep-cleanse facial", "Skin · 60 min", "₹1,499", "GST 18%", ServicesTeal.positive),
)

private fun demoClients(): List<ClientRowUi> = listOf(
    ClientRowUi("Ananya Mehta", "Regular", vip = true, caption = "12 visits · +91 98201 22334", selected = true),
    ClientRowUi("Rohan Kapoor", "New", vip = false, caption = "2 visits · +91 98922 55667", selected = false),
    ClientRowUi("Zenith Corp (B2B)", "Corporate", vip = false, caption = "GSTIN 27AAHCS9012M1Z9 · +91 99670 88990", selected = false),
)

private fun demoRooms(): List<RoomRowUi> = listOf(
    RoomRowUi("Chair 1 · Priya", "Chair · 6 bookings today", busy = true),
    RoomRowUi("Chair 2 · Arjun", "Chair · 3 bookings today", busy = false),
    RoomRowUi("Spa room A", "Room · 1 booking today", busy = false),
    RoomRowUi("Treatment room B", "Room · 4 bookings today", busy = true),
    RoomRowUi("Front desk", "Desk · 2 walk-ins waiting", busy = false),
)

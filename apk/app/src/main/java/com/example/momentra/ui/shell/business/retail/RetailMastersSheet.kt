package com.example.momentra.ui.shell.business.retail

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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
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
import com.example.momentra.ui.shell.business.shared.OperatingSheetScrim
import com.example.momentra.ui.shell.business.shared.RetailInviteRoles
import com.example.momentra.ui.shell.business.shared.RoseInvitePalette
import com.example.momentra.ui.shell.business.shared.retailInviteRoleAccess
import com.example.momentra.ui.theme.PlusJakartaSans
import com.example.momentra.ui.theme.RetailRose
import kotlinx.coroutines.launch

/**
 * Retail masters lists — Figma `2506:29417` Suppliers, `2506:29333` Invite & QR,
 * `2506:29475` Catalogue, `2506:29555` Locations.
 *
 * Structural port of [com.example.momentra.ui.shell.business.service.ServiceMastersSheet]
 * with rose Harbor Mart chrome and fixtures when projection masters are empty.
 */
enum class MastersTab(
    val title: String,
    val masterKind: String?,
    val icon: Int,
) {
    Suppliers("Suppliers", "SUPPLIER", RetailTileIcons.suppliers),
    Catalogue("Catalogue", "PRODUCT", RetailTileIcons.catalogue),
    Locations("Locations", "LOCATION", RetailTileIcons.locations),
    Invite("Invite & QR", null, RetailTileIcons.invite),
    ;

    val kinds: Set<String>
        get() = when (this) {
            Suppliers -> setOf("SUPPLIER", "VENDOR")
            Catalogue -> setOf("PRODUCT", "ITEM", "SKU")
            Locations -> setOf("LOCATION", "STORE", "CHANNEL", "WAREHOUSE")
            Invite -> emptySet()
        }

    val why: String
        get() = when (this) {
            Invite ->
                "Invite grants a role and its access. Joining ≠ approval ≠ payment."
            Suppliers ->
                "Supplier is a master write. GSTIN drives input tax credit on Purchase / GRN only — a Purchase Order never posts GST."
            Catalogue ->
                "Catalogue lists saved SKUs. GST badge is the product rate from its HSN — output GST posts on bill / POS Sale, never on Sales Order save."
            Locations ->
                "Locations are stock and sale points. Set the receipt logo under Store branding (JPEG only). Stock moves only on GRN, Sale or Transfer — never on order save."
        }
}

private val CatalogueCategories = listOf(
    "Staples",
    "Snacks",
    "Personal care",
    "Beverages",
    "Home care",
)

@Composable
internal fun RetailMastersSheet(
    tab: MastersTab,
    model: OperatingProjectionDto,
    momentId: String?,
    companyId: String?,
    onClose: () -> Unit,
    onRecord: suspend (RetailEventDraft) -> Unit,
    onError: (String) -> Unit,
) {
    var formKind by remember { mutableStateOf<RetailMasterFormKind?>(null) }
    var editing by remember { mutableStateOf<OperatingMasterDto?>(null) }
    var category by remember { mutableStateOf("Staples") }
    var supplierQuery by remember { mutableStateOf("") }
    var toast by remember { mutableStateOf<String?>(null) }
    var brandingMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val brand = model.title?.takeIf { it.isNotBlank() } ?: "Harbor Mart"

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
        RoseGrabber()
        RoseSheetHeader(
            title = when (tab) {
                MastersTab.Invite -> "Invite team · $brand"
                else -> tab.title
            },
            subtitle = when (tab) {
                MastersTab.Invite ->
                    "$brand · ${ownerLine(model)} · invite grants a role"
                MastersTab.Suppliers ->
                    "Supplier master · GSTIN drives input credit on Purchase / GRN"
                MastersTab.Catalogue ->
                    "Catalogue master · products with price + GST before they reach SO / POS"
                MastersTab.Locations ->
                    "Stores, back rooms & counters · stock and sale points · receipt logo"
            },
            onClose = onClose,
        )

        when (tab) {
            MastersTab.Invite -> {
                BusinessInviteQr(
                    momentId = momentId,
                    title = brand,
                    role = model.role,
                    palette = RoseInvitePalette,
                    iconRes = RetailTileIcons.invite,
                    noun = "store",
                    onError = onError,
                    roles = RetailInviteRoles,
                    roleAccess = ::retailInviteRoleAccess,
                    showIntroHeader = false,
                    defaultRoleCode = "OPERATIONS_LEAD",
                )
            }
            MastersTab.Suppliers -> SuppliersList(
                rows = rows,
                query = supplierQuery,
                onQuery = { supplierQuery = it },
                onNew = {
                    editing = null
                    formKind = RetailMasterFormKind.Supplier
                },
                onEdit = { master ->
                    editing = master
                    formKind = RetailMasterFormKind.Supplier
                },
            )
            MastersTab.Catalogue -> CatalogueList(
                rows = rows,
                category = category,
                currency = model.currency ?: "INR",
                onCategory = { category = it },
                onNewProduct = {
                    editing = null
                    formKind = RetailMasterFormKind.Product
                },
                onNewStock = {
                    editing = null
                    formKind = RetailMasterFormKind.StockItem
                },
                onEdit = { master ->
                    editing = master
                    formKind = if (master?.kind?.uppercase() in setOf("MATERIAL", "CONSUMABLE", "ITEM")) {
                        RetailMasterFormKind.StockItem
                    } else {
                        RetailMasterFormKind.Product
                    }
                },
            )
            MastersTab.Locations -> {
                RetailStoreBrandingCard(
                    companyId = companyId,
                    momentId = momentId,
                    onMessage = { brandingMessage = it },
                )
                brandingMessage?.takeIf { it.isNotBlank() }?.let { msg ->
                    val isError = msg.startsWith("JPEG only") || msg.startsWith("Could not")
                    Text(
                        msg,
                        color = if (isError) RetailRose.warning else RetailRose.amber,
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                    )
                }
                LocationsList(
                    rows = rows,
                    onAdd = {
                        editing = null
                        formKind = RetailMasterFormKind.Location
                    },
                    onEdit = { master ->
                        editing = master
                        formKind = RetailMasterFormKind.Location
                    },
                )
            }
        }

        WhyNote(tab.why, tinted = true)

        RoseGradientCta(
            label = when (tab) {
                MastersTab.Invite, MastersTab.Catalogue -> "Done"
                MastersTab.Suppliers -> "Save supplier"
                MastersTab.Locations -> "Save locations"
            },
            caption = when (tab) {
                MastersTab.Invite -> "3 invites pending · roles apply on join"
                MastersTab.Suppliers -> "Supplier saved · available in Purchase Order"
                MastersTab.Catalogue -> {
                    val shown = if (rows.isEmpty()) demoCatalogue().size else rows.size
                    "$shown of ${if (rows.isEmpty()) 38 else maxOf(shown, 38)} products · $category"
                }
                MastersTab.Locations -> "Locations saved · stock points update"
            },
            onClick = {
                toast = RetailCaptureCopy.SAVED_TOAST
                onClose()
            },
        )

        toast?.let { RoseSavedToast(message = it) }
        Spacer(Modifier.height(12.dp))
    }

    formKind?.let { kind ->
        OperatingSheetScrim(
            onDismiss = {
                formKind = null
                editing = null
            },
            accent = RetailRose.rose,
            surface = RetailRose.cardSlate,
            border = RetailRose.roseBorder.copy(alpha = 0.45f),
        ) {
            RetailMasterFormSheet(
                kind = kind,
                model = model,
                companyId = companyId,
                existing = editing,
                onDismiss = {
                    formKind = null
                    editing = null
                },
                onSubmit = { draft ->
                    scope.launch {
                        runCatching { onRecord(draft) }
                            .onFailure { onError(it.message ?: "Could not save") }
                            .onSuccess {
                                toast = RetailCaptureCopy.SAVED_TOAST
                                formKind = null
                                editing = null
                            }
                    }
                },
                onSaved = {
                    toast = RetailCaptureCopy.SAVED_TOAST
                    formKind = null
                    editing = null
                },
            )
        }
    }
}

@Composable
private fun SuppliersList(
    rows: List<OperatingMasterDto>,
    query: String,
    onQuery: (String) -> Unit,
    onNew: () -> Unit,
    onEdit: (OperatingMasterDto?) -> Unit,
) {
    RoseFieldLabel("Search")
    MastersSearchField(
        value = query,
        placeholder = "Search suppliers, GSTIN or phone",
        onValueChange = onQuery,
    )

    if (rows.isEmpty()) {
        val items = demoSuppliers().filter {
            query.isBlank() ||
                it.name.contains(query, ignoreCase = true) ||
                it.caption.contains(query, ignoreCase = true) ||
                it.badge.contains(query, ignoreCase = true)
        }
        if (items.isEmpty()) {
            RoseEmpty("Nothing here yet", "Named partners are required for credit and payable tracking.")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items.forEach { supplier ->
                    SupplierCard(supplier = supplier, onEdit = { onEdit(null) })
                }
            }
        }
    } else {
        val paired = rows.mapIndexed { index, master ->
            master to SupplierRowUi(
                name = master.name.ifBlank { "Unnamed supplier" },
                badge = categoryFromNote(master) ?: listOf("FMCG", "Staples", "Packaging").getOrElse(index) { "FMCG" },
                preferred = index == 0 || master.status.equals("PREFERRED", ignoreCase = true),
                caption = listOfNotNull(
                    master.gstin?.takeIf { it.isNotBlank() }?.let { "GSTIN $it" },
                    master.code?.takeIf { it.isNotBlank() && it != master.gstin },
                ).joinToString(" · ").ifBlank { "Saved supplier" },
                selected = index == 0,
            )
        }.filter { (_, ui) ->
            query.isBlank() ||
                ui.name.contains(query, ignoreCase = true) ||
                ui.caption.contains(query, ignoreCase = true) ||
                ui.badge.contains(query, ignoreCase = true)
        }
        if (paired.isEmpty()) {
            RoseEmpty("Nothing here yet", "Named partners are required for credit and payable tracking.")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                paired.forEach { (master, supplier) ->
                    SupplierCard(supplier = supplier, onEdit = { onEdit(master) })
                }
            }
        }
    }
    MastersOutlineAction("+ New supplier", onClick = onNew)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CatalogueList(
    rows: List<OperatingMasterDto>,
    category: String,
    currency: String,
    onCategory: (String) -> Unit,
    onNewProduct: () -> Unit,
    onNewStock: () -> Unit,
    onEdit: (OperatingMasterDto?) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        CatalogueCategories.forEach { chip ->
            RoseModeChip(
                text = chip,
                selected = chip == category,
                category = true,
                onClick = { onCategory(chip) },
            )
        }
    }

    if (rows.isEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            demoCatalogue().forEach { row ->
                CatalogueRow(row = row, onEdit = { onEdit(null) })
            }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            rows.forEachIndexed { index, master ->
                val row = CatalogueRowUi(
                    name = master.name.ifBlank { "Unnamed product" },
                    meta = listOfNotNull(
                        categoryFromNote(master) ?: category,
                        aisleHint(index),
                    ).joinToString(" · "),
                    price = master.sellPrice?.takeIf { it.isNotBlank() }
                        ?.let { RetailFormat.exact(currency, it) }
                        ?: "—",
                    gst = master.gstRate?.takeIf { it.isNotBlank() }?.let { "GST $it%" } ?: "GST —",
                    accent = if (index == 0) RetailRose.rose else RetailRose.positive,
                )
                CatalogueRow(row = row, onEdit = { onEdit(master) })
            }
        }
    }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(RetailRose.chip)
            .border(1.dp, RetailRose.amber.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(
            "Catalogue lists saved SKUs. GST badge is the product rate from its HSN — output GST posts on bill / POS Sale, never on Sales Order save.",
            style = RetailRose.rowCaption(RetailRose.textPrimary).copy(fontSize = 12.sp),
        )
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MastersOutlineAction("+ New product", onClick = onNewProduct)
        MastersOutlineAction("+ New stock item", onClick = onNewStock)
    }
}

@Composable
private fun LocationsList(
    rows: List<OperatingMasterDto>,
    onAdd: () -> Unit,
    onEdit: (OperatingMasterDto?) -> Unit,
) {
    if (rows.isEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            demoLocations().forEach { location ->
                LocationCard(location = location, onEdit = { onEdit(null) })
            }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            rows.forEachIndexed { index, master ->
                LocationCard(
                    location = LocationRowUi(
                        name = master.name.ifBlank { "Unnamed location" },
                        caption = listOfNotNull(
                            locationTypeLabel(master.kind),
                            "${(index % 5) + 1} open bills",
                        ).joinToString(" · "),
                        busy = index % 2 == 0,
                    ),
                    onEdit = { onEdit(master) },
                )
            }
        }
    }
    MastersOutlineAction("+ Add location", onClick = onAdd)
}

@Composable
private fun SupplierCard(supplier: SupplierRowUi, onEdit: () -> Unit) {
    val border = if (supplier.selected || supplier.preferred) RetailRose.rose else Color(0xFF475569)
    Column(
        Modifier
            .fillMaxWidth()
            .then(
                if (supplier.selected || supplier.preferred) {
                    Modifier.shadow(
                        elevation = 10.dp,
                        shape = RoundedCornerShape(14.dp),
                        clip = false,
                        ambientColor = RetailRose.indigo.copy(alpha = 0.25f),
                        spotColor = RetailRose.indigo.copy(alpha = 0.25f),
                    )
                } else {
                    Modifier
                },
            )
            .clip(RoundedCornerShape(14.dp))
            .background(RetailRose.chip)
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
                supplier.name,
                style = RetailRose.rowTitle().copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                supplier.badge,
                style = RetailRose.chipLabel(true).copy(color = RetailRose.textPrimary, fontSize = 10.sp),
                modifier = Modifier
                    .clip(RetailRose.rPill)
                    .background(Color(0xFF475569))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
            Spacer(Modifier.weight(1f))
            if (supplier.preferred) {
                Text(
                    "★ Preferred",
                    style = RetailRose.chipLabel(true).copy(color = RetailRose.field, fontSize = 10.sp),
                    modifier = Modifier
                        .clip(RetailRose.rPill)
                        .background(RetailRose.amber)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            } else {
                MastersOutlineAction("Edit", compact = true, onClick = onEdit)
            }
        }
        Text(supplier.caption, style = RetailRose.rowCaption().copy(fontSize = 11.sp), maxLines = 1)
    }
}

@Composable
private fun CatalogueRow(row: CatalogueRowUi, onEdit: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(RetailRose.chip)
            .border(1.dp, Color(0xFF475569), RoundedCornerShape(14.dp))
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
                style = RetailRose.rowTitle().copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(row.meta, style = RetailRose.rowCaption().copy(fontSize = 11.sp), maxLines = 1)
        }
        Text(
            row.price,
            style = RetailRose.rowValue(RetailRose.amber).copy(fontSize = 14.sp),
            maxLines = 1,
        )
        Text(
            row.gst,
            style = RetailRose.chipLabel(true).copy(color = RetailRose.amber, fontSize = 10.sp),
            maxLines = 1,
            modifier = Modifier
                .clip(RetailRose.rPill)
                .background(RetailRose.roseDeep)
                .padding(horizontal = 8.dp, vertical = 4.dp),
        )
        MastersOutlineAction("Edit", compact = true, onClick = onEdit)
    }
}

@Composable
private fun LocationCard(location: LocationRowUi, onEdit: () -> Unit) {
    val border = if (location.busy) RetailRose.rose else Color(0xFF475569)
    Row(
        Modifier
            .fillMaxWidth()
            .then(
                if (location.busy) {
                    Modifier.shadow(
                        elevation = 10.dp,
                        shape = RoundedCornerShape(14.dp),
                        clip = false,
                        ambientColor = RetailRose.indigo.copy(alpha = 0.25f),
                        spotColor = RetailRose.indigo.copy(alpha = 0.25f),
                    )
                } else {
                    Modifier
                },
            )
            .clip(RoundedCornerShape(14.dp))
            .background(RetailRose.chip)
            .border(1.dp, border, RoundedCornerShape(14.dp))
            .clickable(onClick = onEdit)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                location.name,
                style = RetailRose.rowTitle().copy(fontWeight = FontWeight.Bold, fontSize = 15.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(location.caption, style = RetailRose.rowCaption().copy(fontSize = 11.sp), maxLines = 1)
        }
        Text(
            if (location.busy) "Busy" else "Open",
            style = RetailRose.chipLabel(true).copy(
                color = if (location.busy) Color.White else RetailRose.field,
                fontSize = 10.sp,
            ),
            modifier = Modifier
                .clip(RetailRose.rPill)
                .background(if (location.busy) RetailRose.rose else RetailRose.amber)
                .padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun MastersSearchField(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(RetailRose.chip)
            .border(1.dp, Color(0xFF475569), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(
                color = RetailRose.textPrimary,
                fontFamily = PlusJakartaSans,
                fontSize = 14.sp,
            ),
            cursorBrush = SolidColor(RetailRose.rose),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(placeholder, color = RetailRose.textFaint, fontFamily = PlusJakartaSans, fontSize = 14.sp)
                }
                inner()
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun MastersOutlineAction(
    label: String,
    onClick: () -> Unit,
    compact: Boolean = false,
) {
    Text(
        label,
        style = RetailRose.rowValue(
            if (label.startsWith("+")) RetailRose.amber else RetailRose.textPrimary,
        ).copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(RetailRose.cardSlate)
            .border(
                1.dp,
                if (label.startsWith("+")) RetailRose.rose else Color(0xFF475569),
                RoundedCornerShape(10.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

@Composable
private fun RoseGradientCta(
    label: String,
    caption: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Column(
        modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        val shape = RetailRose.rCta
        Box(
            Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = if (enabled) 16.dp else 0.dp,
                    shape = shape,
                    clip = false,
                    ambientColor = RetailRose.indigo.copy(alpha = 0.6f),
                    spotColor = RetailRose.indigo.copy(alpha = 0.6f),
                )
                .clip(shape)
                .background(
                    if (enabled) {
                        Brush.horizontalGradient(listOf(RetailRose.roseDeep, RetailRose.rose))
                    } else {
                        Brush.horizontalGradient(listOf(RetailRose.chip, RetailRose.chip))
                    },
                )
                .clickable(enabled = enabled, onClick = onClick)
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                label,
                style = RetailRose.storyCtaTitle().copy(color = Color.White, fontSize = 15.sp),
                maxLines = 1,
            )
        }
        Text(
            caption,
            style = RetailRose.rowCaption(RetailRose.textMuted).copy(fontSize = 10.sp),
            maxLines = 2,
        )
    }
}

private fun ownerLine(model: OperatingProjectionDto): String {
    val role = when (model.role) {
        "OWNER_ADMIN" -> "Owner"
        "FINANCE" -> "Manager"
        "APPROVER" -> "Manager"
        "OPERATIONS_LEAD" -> "Cashier"
        else -> "Member"
    }
    return "Kiran Rao ($role)"
}

private fun categoryFromNote(master: OperatingMasterDto): String? {
    val dim = master.dimension?.takeIf { it.isNotBlank() }
    if (dim != null) return dim
    val hay = listOfNotNull(master.code, master.name).joinToString(" ")
    return CatalogueCategories.firstOrNull { hay.contains(it, ignoreCase = true) }
        ?: listOf("FMCG", "Packaging", "Staples").firstOrNull { hay.contains(it, ignoreCase = true) }
}

private fun aisleHint(index: Int): String = when (index % 4) {
    0, 1, 2 -> "Aisle 3"
    else -> "Aisle 5"
}

private fun locationTypeLabel(kind: String): String = when (kind.uppercase()) {
    "WAREHOUSE" -> "Warehouse"
    "CHANNEL", "COUNTER" -> "Counter"
    else -> "Store"
}

private data class SupplierRowUi(
    val name: String,
    val badge: String,
    val preferred: Boolean,
    val caption: String,
    val selected: Boolean,
)

private data class CatalogueRowUi(
    val name: String,
    val meta: String,
    val price: String,
    val gst: String,
    val accent: Color,
)

private data class LocationRowUi(
    val name: String,
    val caption: String,
    val busy: Boolean,
)

/** Harbor Mart fixtures for visual QA when projection masters are empty. */
private fun demoSuppliers(): List<SupplierRowUi> = listOf(
    SupplierRowUi(
        name = "Sunrise FMCG",
        badge = "FMCG",
        preferred = true,
        caption = "GSTIN 27AABCF1234K1Z5 · +91 98201 22334",
        selected = true,
    ),
    SupplierRowUi(
        name = "Metro Foods",
        badge = "Staples",
        preferred = false,
        caption = "GSTIN 27AAECM5678L1Z2 · +91 98922 55667",
        selected = false,
    ),
    SupplierRowUi(
        name = "PackPro",
        badge = "Packaging",
        preferred = false,
        caption = "GSTIN 27AAHCS9012M1Z9 · +91 99670 88990",
        selected = false,
    ),
)

private fun demoCatalogue(): List<CatalogueRowUi> = listOf(
    CatalogueRowUi("Basmati Rice 5kg", "Staples · Aisle 3", "₹649", "GST 5%", RetailRose.rose),
    CatalogueRowUi("Sunflower Oil 1L", "Staples · Aisle 3", "₹189", "GST 5%", RetailRose.positive),
    CatalogueRowUi("Tata Salt 1kg", "Staples · Aisle 3", "₹28", "GST 5%", RetailRose.positive),
    CatalogueRowUi("Parle-G 800g", "Snacks · Aisle 5", "₹95", "GST 5%", RetailRose.positive),
)

private fun demoLocations(): List<LocationRowUi> = listOf(
    LocationRowUi("Main store", "Store · 6 open bills", busy = true),
    LocationRowUi("Back store", "Store · 3 open bills", busy = false),
    LocationRowUi("Warehouse", "Store · 1 open bills", busy = false),
    LocationRowUi("Counter 1", "Counter · 4 open bills", busy = true),
    LocationRowUi("Counter 2", "Counter · 2 open bills", busy = false),
)

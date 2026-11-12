package com.example.momentra.ui.shell.business.shared

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.api.OperatingMasterBody
import com.example.momentra.data.api.OperatingMasterDto
import com.example.momentra.data.api.OperatingMasterUpdateBody
import com.example.momentra.data.api.OperatingPriceListBody
import com.example.momentra.data.api.OperatingPriceListDto
import com.example.momentra.data.api.OperatingPriceListLineBody
import com.example.momentra.data.api.OperatingPriceListUpdateBody
import com.example.momentra.data.api.OperatingProjectionDto
import com.example.momentra.data.repository.BusinessSliceRepository
import com.example.momentra.ui.theme.PlusJakartaSans
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Everything the masters sheets need that the projection does not carry: the company
 * the masters belong to, the moment they are scoped to, and a way to republish the
 * projection after a write.
 */
data class OperatingMasterContext(
    val companyId: String?,
    val momentId: String?,
    val onChanged: () -> Unit,
)

/** The six Figma masters tabs (`2566:31888`-`2566:32380`, `2558:29420`-`2558:29912`). */enum class OperatingMasterTab(
    val title: String,
    val singular: String,
    val note: String,
) {
    Principals(
        "Business contacts",
        "supplier or customer",
        "Identity is stable here: a contact row is referenced by id. One record can be Supplier, Customer, or both.",
    ),
    Catalogue(
        "Catalogue & SKU",
        "SKU",
        "A SKU carries unit precision and standing prices. A posted bill never rewrites it.",
    ),
    Locations(
        "Warehouses & routes",
        "warehouse or route",
        "A route belongs to a warehouse. Moving stock between them is a transfer, not a sale.",
    ),
    PriceLists(
        "Price lists",
        "price list",
        "A price list is effective-dated. Two lists may overlap in name but never in a day.",
    ),
    Invite(
        "Invite & QR",
        "invite",
        "Invite grants a role and its access. Joining ≠ approval ≠ payment.",
    ),
}

/** Master kinds each tab lists. */
internal fun OperatingMasterTab.kinds(): Set<String> = when (this) {
    OperatingMasterTab.Principals -> setOf("PRINCIPAL", "DEALER", "SUPPLIER", "BUYER", "CUSTOMER", "CHANNEL")
    OperatingMasterTab.Catalogue -> setOf("SKU", "PRODUCT", "BATCH")
    OperatingMasterTab.Locations -> setOf("WAREHOUSE", "LOCATION", "ROUTE")
    OperatingMasterTab.PriceLists -> emptySet()
    OperatingMasterTab.Invite -> emptySet()
}

/**
 * Masters sheet for both verticals. Rows come from `business.operating_master` through
 * the company endpoint; the projection's `masters` list is only the offline fallback.
 */
@Composable
internal fun OperatingMastersSheet(
    tab: OperatingMasterTab,
    model: OperatingProjectionDto,
    context: OperatingMasterContext,
    theme: OperatingSheetTheme,
    onClose: () -> Unit,
    /** Setup §04 `+ Create SKU` opens the add form immediately instead of the list. */
    initialForm: OperatingMasterTab? = null,
) {
    val repo = remember { BusinessSliceRepository() }
    val scope = rememberCoroutineScope()
    var refresh by remember(tab) { mutableIntStateOf(0) }
    var remote by remember(tab) { mutableStateOf<List<OperatingMasterDto>>(emptyList()) }
    var prices by remember(tab) { mutableStateOf<List<OperatingPriceListDto>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var form by remember(tab) { mutableStateOf(initialForm) }
    var editing by remember { mutableStateOf<OperatingMasterDto?>(null) }
    var priceFormOpen by remember { mutableStateOf(false) }

    LaunchedEffect(context.companyId, tab, refresh) {
        val companyId = context.companyId ?: return@LaunchedEffect
        if (tab == OperatingMasterTab.Invite) return@LaunchedEffect
        if (tab == OperatingMasterTab.PriceLists) {
            repo.listPriceLists(companyId, context.momentId).fold(
                onSuccess = { prices = it; error = null },
                onFailure = { error = it.message },
            )
            return@LaunchedEffect
        }
        repo.listMasters(companyId, momentId = context.momentId).fold(
            onSuccess = { remote = it; error = null },
            onFailure = { error = it.message },
        )
    }

    // Table rows are authority; event-derived rows fill kinds the table has not got yet.
    val rows = remember(remote, model.masters, tab) {
        val kinds = tab.kinds()
        val fromTable = remote.filter { kinds.contains(it.kind.uppercase()) }
        val seen = fromTable.map { "${it.kind.uppercase()}|${it.name.trim().lowercase()}" }.toSet()
        val fromEvents = model.masters.orEmpty()
            .filter { kinds.contains(it.kind.uppercase()) }
            .filterNot { "${it.kind.uppercase()}|${it.name.trim().lowercase()}" in seen }
        (fromTable + fromEvents).sortedBy { it.name.lowercase() }
    }

    val dist = model.subtype.equals("DISTRIBUTION_DEALERSHIP", ignoreCase = true) ||
        OperatingChromeState.distributionChrome
    val sheetTitle = if (dist) DistributionMasterCopy.tabTitle(tab) else tab.title
    val sheetSubtitle = if (dist) {
        DistributionMasterCopy.tabSubtitle(tab)
    } else if (tab != OperatingMasterTab.Invite) {
        "${rows.size} record${if (rows.size == 1) "" else "s"} · " +
            (model.title?.takeIf { it.isNotBlank() } ?: "this moment")
    } else {
        null
    }
    val whyNote = if (dist) DistributionMasterCopy.tabNote(tab) else tab.note
    val newLabel = if (dist) DistributionMasterCopy.newCta(tab) else "New ${tab.singular}"

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 17.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.fillMaxWidth().height(22.dp), horizontalArrangement = Arrangement.Center) {
            Box(
                Modifier
                    .width(40.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(100.dp))
                    .background(Color.White.copy(alpha = 0.2f)),
            )
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                sheetTitle,
                color = theme.ink,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 24.sp,
                modifier = Modifier.weight(1f),
            )
            Text(
                "Close",
                color = theme.body,
                fontFamily = PlusJakartaSans,
                fontSize = 13.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .clickable(onClick = onClose)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            )
        }
        if (sheetSubtitle != null) {
            Text(
                sheetSubtitle,
                color = theme.body,
                fontFamily = PlusJakartaSans,
                fontSize = 12.sp,
            )
        }

        when (tab) {
            OperatingMasterTab.Invite -> BusinessInviteQr(
                momentId = context.momentId,
                title = model.title.orEmpty(),
                role = model.role,
                palette = InvitePalette(
                    accent = theme.accent,
                    accentMuted = theme.accentSoft,
                    card = theme.surface,
                    field = theme.inset,
                    chip = theme.inset,
                    chipStroke = theme.border,
                    divider = theme.border,
                    qrPlate = theme.inset,
                    textPrimary = theme.ink,
                    textBody = theme.body,
                    textMuted = theme.body,
                    textFaint = theme.body,
                    warning = theme.warning,
                    negative = theme.negative,
                ),
                noun = "moment",
                roles = if (dist) {
                    OperatingMomentCatalog.distributionInviteRoles.map { it.code to it.label }
                } else {
                    BusinessInviteRoles
                },
                onError = { error = it },
            )
            OperatingMasterTab.PriceLists -> {
                if (context.companyId == null) {
                    MasterHint("Masters need a company. This moment is not attached to one yet.", theme)
                } else {
                    MasterCta(newLabel, theme) { priceFormOpen = true }
                    if (prices.isEmpty()) {
                        BusinessEmptyState(
                            title = "No price list yet",
                            body = "Create one and its dated lines become the standing price for those SKUs.",
                            accent = theme.accent,
                            glyph = "≡",
                            compact = true,
                        )
                    }
                    prices.forEach { list -> PriceListRow(list, theme) }
                }
            }
            else -> {
                if (context.companyId == null) {
                    MasterHint("Masters need a company. This moment is not attached to one yet.", theme)
                } else {
                    MasterCta(newLabel, theme) { form = tab }
                    if (rows.isEmpty()) {
                        BusinessEmptyState(
                            title = "Nothing here yet",
                            body = "Add the first row and it becomes pickable on every capture sheet.",
                            accent = theme.accent,
                            glyph = "◇",
                            compact = true,
                        )
                    }
                    rows.forEach { master ->
                        MasterRow(master, theme) {
                            // Event-derived rows cannot be edited in place: they have no version.
                            if (master.source != "EVENT") {
                                editing = master
                                form = tab
                            }
                        }
                    }
                }
            }
        }
        OperatingWhyFootnote(whyNote, theme)
        if (error != null) {
            Text(error ?: "", color = theme.negative, fontFamily = PlusJakartaSans, fontSize = 12.sp)
        }
        Spacer(Modifier.height(24.dp))
    }

    val openForm = form
    if (openForm != null && context.companyId != null) {
        OperatingSheetScrim(
            onDismiss = { form = null; editing = null },
            accent = theme.accent,
            surface = theme.canvas,
            border = theme.border,
        ) {
            OperatingMasterForm(
                tab = openForm,
                existing = editing,
                model = model,
                context = context,
                theme = theme,
                onDismiss = { form = null; editing = null },
                onSaved = {
                    form = null
                    editing = null
                    refresh += 1
                    context.onChanged()
                },
                onError = { error = it },
            )
        }
    }
    if (priceFormOpen && context.companyId != null) {
        OperatingSheetScrim(
            onDismiss = { priceFormOpen = false },
            accent = theme.accent,
            surface = theme.canvas,
            border = theme.border,
        ) {
            OperatingPriceListForm(
                masters = rows.filter { it.kind.uppercase() == "SKU" || it.kind.uppercase() == "PRODUCT" },
                context = context,
                theme = theme,
                onDismiss = { priceFormOpen = false },
                onSaved = {
                    priceFormOpen = false
                    refresh += 1
                    context.onChanged()
                },
                onError = { error = it },
            )
        }
    }
}

@Composable
private fun MasterRow(master: OperatingMasterDto, theme: OperatingSheetTheme, onEdit: () -> Unit) {
    val meta = listOfNotNull(
        master.unit?.takeIf { it.isNotBlank() }?.let { "per $it" },
        master.sellPrice?.takeIf { it.isNotBlank() }?.let { "sell $it" },
        master.purchasePrice?.takeIf { it.isNotBlank() }?.let { "buy $it" },
        master.gstRate?.takeIf { it.isNotBlank() }?.let { "GST $it%" },
        master.hsn?.takeIf { it.isNotBlank() }?.let { "HSN $it" },
        master.gstin?.takeIf { it.isNotBlank() }?.let { "GSTIN $it" },
    ).joinToString(" · ")
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(theme.surface)
            .border(1.dp, theme.border, RoundedCornerShape(14.dp))
            .clickable(onClick = onEdit)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(theme.accent.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(master.kind.take(2), color = theme.accent, fontFamily = PlusJakartaSans, fontSize = 11.sp)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                master.name.ifBlank { "Unnamed ${master.kind.lowercase()}" },
                color = theme.ink,
                fontFamily = PlusJakartaSans,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                meta.ifBlank { if (master.source == "EVENT") "From a posted record · not editable" else "No price or GST set" },
                color = theme.body,
                fontFamily = PlusJakartaSans,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (master.source == "EVENT") {
            OperatingBadgeChip("From record", theme)
        } else if (master.status != null && master.status != "ACTIVE") {
            OperatingBadgeChip(master.status, theme)
        }
    }
}

@Composable
private fun PriceListRow(list: OperatingPriceListDto, theme: OperatingSheetTheme) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(theme.surface)
            .border(1.dp, theme.border, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                list.name.ifBlank { "Untitled price list" },
                color = theme.ink,
                fontFamily = PlusJakartaSans,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (list.status != null && list.status != "ACTIVE") OperatingBadgeChip(list.status, theme)
        }
        Text(
            listOfNotNull(
                list.channel?.takeIf { it.isNotBlank() },
                "${list.validFrom.take(10)}${list.validTo?.take(10)?.let { " → $it" } ?: " → open"}",
                "${list.lines.size} line${if (list.lines.size == 1) "" else "s"}",
                list.gstRate?.takeIf { it.isNotBlank() }?.let { "GST $it%" },
            ).joinToString(" · "),
            color = theme.body,
            fontFamily = PlusJakartaSans,
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun MasterHint(text: String, theme: OperatingSheetTheme) {
    Text(text, color = theme.warning, fontFamily = PlusJakartaSans, fontSize = 12.sp)
}

@Composable
private fun MasterCta(label: String, theme: OperatingSheetTheme, onClick: () -> Unit) {
    Text(
        label,
        color = theme.canvas,
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(theme.accent)
            .clickable(onClick = onClick)
            .padding(vertical = 13.dp),
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
    )
}

/**
 * One form for principals/dealers, warehouses/routes, and SKUs. Which fields are shown
 * depends on the tab; the body sent is always a full `OperatingMasterBody`.
 */
@Composable
private fun OperatingMasterForm(
    tab: OperatingMasterTab,
    existing: OperatingMasterDto?,
    model: OperatingProjectionDto,
    context: OperatingMasterContext,
    theme: OperatingSheetTheme,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    onError: (String) -> Unit,
) {
    val repo = remember { BusinessSliceRepository() }
    val scope = rememberCoroutineScope()
    var contactRole by remember {
        mutableStateOf(
            when (existing?.kind?.uppercase()) {
                "SUPPLIER" -> "Supplier"
                "BUYER", "CUSTOMER", "DEALER" -> "Customer"
                "PRINCIPAL" -> "Both"
                else -> "Customer"
            },
        )
    }
    val kind = when {
        tab != OperatingMasterTab.Principals -> existing?.kind?.uppercase() ?: defaultKindFor(tab)
        contactRole == "Supplier" -> "SUPPLIER"
        contactRole == "Both" -> "PRINCIPAL"
        else -> "CUSTOMER"
    }
    val isSku = kind == "SKU" || kind == "PRODUCT"
    val isRoute = kind == "ROUTE"

    var name by remember { mutableStateOf(existing?.name.orEmpty()) }
    var code by remember { mutableStateOf(existing?.code.orEmpty()) }
    var unit by remember { mutableStateOf(existing?.unit.orEmpty()) }
    var unitCategory by remember {
        mutableStateOf(
            OperatingMomentCatalog.unitMaster
                .firstOrNull { it.label.equals(existing?.unit.orEmpty(), true) || it.code.equals(existing?.unit.orEmpty(), true) }
                ?.category
                ?: OperatingMomentCatalog.unitCategories.first(),
        )
    }
    var conversionToBase by remember { mutableStateOf(existing?.conversionToBase.orEmpty()) }
    var sellPrice by remember { mutableStateOf(existing?.sellPrice.orEmpty()) }
    var purchasePrice by remember { mutableStateOf(existing?.purchasePrice.orEmpty()) }
    var mrp by remember { mutableStateOf(existing?.mrp.orEmpty()) }
    var hsn by remember { mutableStateOf(existing?.hsn.orEmpty()) }
    var gstRate by remember { mutableStateOf(existing?.gstRate.orEmpty()) }
    var gstin by remember { mutableStateOf(existing?.gstin.orEmpty()) }
    var state by remember { mutableStateOf(existing?.billingState.orEmpty()) }
    var picker by remember { mutableStateOf(false) }
    var parentId by remember { mutableStateOf(existing?.parentMasterId.orEmpty()) }
    var saving by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }
    val parents = remember(model.masters, tab, isRoute) {
        model.masters.orEmpty().filter {
            val k = it.kind.uppercase()
            if (isRoute) k == "WAREHOUSE" || k == "LOCATION" else k == "PRINCIPAL"
        }
    }

    val dist = model.subtype.equals("DISTRIBUTION_DEALERSHIP", ignoreCase = true) ||
        OperatingChromeState.distributionChrome
    val formHeading = when {
        dist -> DistributionMasterCopy.formTitle(tab, existing?.name)
        tab == OperatingMasterTab.Principals && existing == null -> "Add Business Contact"
        existing != null -> "Edit ${existing.name}"
        else -> "New ${tab.singular}"
    }
    val saveLabel = when {
        saving -> "Saving…"
        dist -> DistributionMasterCopy.saveCta(tab, existing != null)
        existing == null -> "Create"
        else -> "Save changes"
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 17.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            formHeading,
            color = theme.ink,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 22.sp,
        )
        if (tab == OperatingMasterTab.Principals) {
            // QA-01 / CF-02 — Supplier vs Customer segments; PRINCIPAL = both roles, one record.
            Text("Contact role", color = theme.body, fontFamily = PlusJakartaSans, fontSize = 12.sp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    "Supplier" to "Someone you purchase from",
                    "Customer" to "Someone you sell to",
                    "Both" to "Supplier and customer",
                ).forEach { (label, hint) ->
                    val on = contactRole == label
                    Column(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (on) theme.accent else theme.inset)
                            .border(1.dp, if (on) theme.accent else theme.border, RoundedCornerShape(10.dp))
                            .clickable { contactRole = label }
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                    ) {
                        Text(label, color = if (on) theme.canvas else theme.ink, fontFamily = PlusJakartaSans, fontSize = 13.sp)
                        Text(hint, color = if (on) theme.canvas.copy(alpha = 0.85f) else theme.body, fontFamily = PlusJakartaSans, fontSize = 10.sp)
                    }
                }
            }
        }
        MasterField(if (tab == OperatingMasterTab.Principals) "Business / Contact Name" else "Name", name, theme) { name = it }
        MasterField("Code (optional)", code, theme) { code = it }
        if (isSku) {
            Text("Unit category", color = theme.body, fontFamily = PlusJakartaSans, fontSize = 12.sp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OperatingMomentCatalog.unitCategories.forEach { category ->
                    val on = unitCategory == category
                    Text(
                        category,
                        color = if (on) theme.canvas else theme.ink,
                        fontFamily = PlusJakartaSans,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (on) theme.accent else theme.inset)
                            .clickable { unitCategory = category }
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                    )
                }
            }
            Text("Unit", color = theme.body, fontFamily = PlusJakartaSans, fontSize = 12.sp)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OperatingMomentCatalog.unitsForCategory(unitCategory).forEach { entry ->
                    val on = unit == entry.label || unit.equals(entry.code, ignoreCase = true)
                    Text(
                        entry.label,
                        color = if (on) theme.canvas else theme.ink,
                        fontFamily = PlusJakartaSans,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (on) theme.accent else theme.inset)
                            .border(1.dp, if (on) theme.accent else theme.border, RoundedCornerShape(10.dp))
                            .clickable { unit = entry.label }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                    )
                }
            }
            MasterField("Packaging conversion to base", conversionToBase, theme, KeyboardType.Decimal) { conversionToBase = it }
            Text(
                "How many base units this packaging holds (e.g. 12 for a dozen). Leave blank when the unit is already base.",
                color = theme.body,
                fontFamily = PlusJakartaSans,
                fontSize = 11.sp,
            )
            MasterField("Sell price", sellPrice, theme, KeyboardType.Decimal) { sellPrice = it }
            MasterField("Purchase price", purchasePrice, theme, KeyboardType.Decimal) { purchasePrice = it }
            MasterField("MRP (optional)", mrp, theme, KeyboardType.Decimal) { mrp = it }
            MasterField("HSN", hsn, theme) { hsn = it }
            MasterField("GST rate %", gstRate, theme, KeyboardType.Decimal) { gstRate = it }
        }
        if (tab == OperatingMasterTab.Principals) {
            MasterField("GSTIN (optional)", gstin, theme) { gstin = it.uppercase() }
            OperatingPickerRow(
                label = "Billing state",
                value = state,
                theme = theme,
                hint = IndiaStateCatalog.gstinStateHint(gstin, state),
                placeholder = "Pick a state",
                onClick = { picker = true },
            )
        }
        if (parents.isNotEmpty()) {
            Text("Attach to", color = theme.body, fontFamily = PlusJakartaSans, fontSize = 12.sp)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                parents.forEach { parent ->
                    val on = parentId == parent.id
                    Text(
                        parent.name,
                        color = if (on) theme.canvas else theme.ink,
                        fontFamily = PlusJakartaSans,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (on) theme.accent else theme.inset)
                            .border(1.dp, if (on) theme.accent else theme.border, RoundedCornerShape(10.dp))
                            .clickable { parentId = if (on) "" else parent.id }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                    )
                }
            }
        }
        if (existing != null) {
            Text(
                "Archive",
                color = theme.negative,
                fontFamily = PlusJakartaSans,
                fontSize = 13.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, theme.negative.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                    .clickable(enabled = !saving) {
                        scope.launch {
                            saving = true
                            repo.archiveMaster(context.companyId.orEmpty(), existing.id, existing.version).fold(
                                onSuccess = {
                                    if (model.subtype.equals("MADE_TO_ORDER", ignoreCase = true)) {
                                        com.example.momentra.ui.shell.business.mto.MtoToastHost.show(
                                            com.example.momentra.ui.shell.business.mto.MtoToastKind.Deleted,
                                            entity = existing.name.ifBlank { "this record" },
                                        )
                                    }
                                    onSaved()
                                },
                                onFailure = { localError = it.message; saving = false },
                            )
                        }
                    }
                    .padding(horizontal = 12.dp, vertical = 9.dp),
            )
        }
        if (localError != null) {
            Text(localError ?: "", color = theme.negative, fontFamily = PlusJakartaSans, fontSize = 12.sp)
        }
        Text(
            saveLabel,
            color = theme.canvas,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(theme.accent)
                .clickable(enabled = !saving && name.isNotBlank()) {
                    val companyId = context.companyId ?: return@clickable
                    val draft = OperatingMasterDraft(
                        kind = kind,
                        name = name,
                        code = code,
                        unit = unit,
                        conversionToBase = conversionToBase,
                        sellPrice = sellPrice,
                        purchasePrice = purchasePrice,
                        mrp = mrp,
                        hsn = hsn,
                        gstRate = gstRate,
                        gstin = gstin,
                        billingState = state,
                        parentMasterId = parentId,
                        momentId = context.momentId,
                    )
                    scope.launch {
                        saving = true
                        localError = null
                        val result = if (existing == null) {
                            repo.createMaster(companyId, draft.toCreateBody())
                        } else {
                            repo.updateMaster(companyId, existing.id, draft.toUpdateBody(existing.version))
                        }
                        result.fold(
                            onSuccess = {
                                if (model.subtype.equals("MADE_TO_ORDER", ignoreCase = true)) {
                                    com.example.momentra.ui.shell.business.mto.MtoToastHost.show(
                                        com.example.momentra.ui.shell.business.mto.MtoToastCatalog.forMasterCreated(
                                            kind = kind,
                                            entity = name.ifBlank { "this record" },
                                        ),
                                    )
                                }
                                onSaved()
                            },
                            onFailure = { localError = it.message; onError(it.message ?: "Could not save"); saving = false },
                        )
                    }
                }
                .padding(vertical = 13.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        if (dist) {
            Text(
                DistributionMasterCopy.saveFootnote(tab),
                color = theme.body,
                fontFamily = PlusJakartaSans,
                fontSize = 10.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
        Text(
            "Close",
            color = theme.body,
            fontFamily = PlusJakartaSans,
            fontSize = 13.sp,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .clickable(onClick = onDismiss)
                .padding(vertical = 10.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }

    if (picker) {
        IndiaStateSheet(
            selected = state,
            theme = theme,
            onDismiss = { picker = false },
            onPick = { picked ->
                state = picked.name
                picker = false
            },
        )
    }
}

private val JakartSansBold = PlusJakartaSans

/** `2566:32380` / `2558:29912` — a dated list with per-SKU prices. */
@Composable
private fun OperatingPriceListForm(
    masters: List<OperatingMasterDto>,
    context: OperatingMasterContext,
    theme: OperatingSheetTheme,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    onError: (String) -> Unit,
) {
    val repo = remember { BusinessSliceRepository() }
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var channel by remember { mutableStateOf("") }
    var validTo by remember { mutableStateOf("") }
    var gstRate by remember { mutableStateOf("") }
    var prices by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var saving by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 17.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("New price list", color = theme.ink, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 22.sp)
        MasterField("Name", name, theme) { name = it }
        MasterField("Channel (optional)", channel, theme) { channel = it }
        MasterField("Valid to (YYYY-MM-DD)", validTo, theme) { validTo = it }
        MasterField("GST rate %", gstRate, theme, KeyboardType.Decimal) { gstRate = it }
        if (masters.isEmpty()) {
            Text(
                "Create a SKU first — a price list prices SKUs, it does not invent them.",
                color = theme.body,
                fontFamily = PlusJakartaSans,
                fontSize = 12.sp,
            )
        }
        masters.forEach { master ->
            val value = prices[master.id].orEmpty()
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    master.name,
                    color = theme.ink,
                    fontFamily = PlusJakartaSans,
                    fontSize = 13.sp,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Box(Modifier.width(110.dp)) {
                    MasterField("Price", value, theme, KeyboardType.Decimal) { input ->
                        prices = prices + (master.id to input)
                    }
                }
            }
        }
        if (localError != null) {
            Text(localError ?: "", color = theme.negative, fontFamily = PlusJakartaSans, fontSize = 12.sp)
        }
        Text(
            if (saving) "Saving…" else "Create price list",
            color = theme.canvas,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(theme.accent)
                .clickable(enabled = !saving && name.isNotBlank()) {
                    val companyId = context.companyId ?: return@clickable
                    val lines = prices.entries
                        .filter { it.value.isNotBlank() }
                        .map { OperatingPriceListLineBody(masterId = it.key, price = it.value.trim()) }
                    scope.launch {
                        saving = true
                        repo.createPriceList(
                            companyId,
                            OperatingPriceListBody(
                                name = name.trim(),
                                validFrom = LocalDate.now().toString(),
                                momentId = context.momentId,
                                channel = channel.trim().ifBlank { null },
                                gstRate = gstRate.trim().ifBlank { null },
                                validTo = validTo.trim().ifBlank { null },
                                lines = lines.ifEmpty { null },
                            ),
                        ).fold(
                            onSuccess = { onSaved() },
                            onFailure = { localError = it.message; onError(it.message ?: "Could not save"); saving = false },
                        )
                    }
                }
                .padding(vertical = 13.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Text(
            "Close",
            color = theme.body,
            fontFamily = PlusJakartaSans,
            fontSize = 13.sp,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .clickable(onClick = onDismiss)
                .padding(vertical = 10.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

@Composable
private fun MasterField(
    label: String,
    value: String,
    theme: OperatingSheetTheme,
    keyboard: KeyboardType = KeyboardType.Text,
    onValueChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, color = theme.body, fontFamily = PlusJakartaSans, fontSize = 11.sp)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(color = theme.ink, fontFamily = PlusJakartaSans, fontSize = 14.sp),
            cursorBrush = SolidColor(theme.accent),
            keyboardOptions = KeyboardOptions(keyboardType = keyboard),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(theme.inset)
                .border(1.dp, theme.border, RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 11.dp),
        )
    }
}

internal fun defaultKindFor(tab: OperatingMasterTab): String = when (tab) {
    OperatingMasterTab.Principals -> "CUSTOMER"
    OperatingMasterTab.Catalogue -> "SKU"
    OperatingMasterTab.Locations -> "WAREHOUSE"
    OperatingMasterTab.PriceLists -> "PRICE_LIST"
    OperatingMasterTab.Invite -> "CHANNEL"
}

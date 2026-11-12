package com.example.momentra.ui.shell.business.manufacturing

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.momentra.data.api.OperatingMasterDto
import com.example.momentra.data.api.OperatingProjectionDto
import com.example.momentra.ui.shell.business.shared.BusinessInviteQr
import com.example.momentra.ui.shell.business.shared.InvitePalette
import com.example.momentra.ui.shell.business.shared.OperatingChromeState
import com.example.momentra.ui.theme.ManufacturingAmber

enum class MfgMasterTab { INVITE, VENDORS, PRODUCTS_BOM, LINES_STORES }

/** Prefer real master status / GSTIN; never invent Preferred from the display name. */
internal fun mfgVendorBadge(v: OperatingMasterDto): String {
    val status = v.status.orEmpty().uppercase()
    val reg = v.gstRegistration.orEmpty().uppercase()
    return when {
        status.contains("PREFERRED") || reg.contains("PREFERRED") -> "Preferred"
        status.contains("APPROVED") || !v.gstin.isNullOrBlank() -> "Approved"
        status.contains("INACTIVE") || status.contains("ARCHIVED") -> "Inactive"
        status.contains("ACTIVE") -> "Active"
        status.isNotBlank() -> status.lowercase().replaceFirstChar { it.uppercase() }
        else -> "Unavailable"
    }
}

/** Busy/Open only when status carries capacity; otherwise honest Unavailable. */
internal fun mfgLineCapacity(loc: OperatingMasterDto): String {
    val status = loc.status.orEmpty().uppercase()
    return when {
        status.contains("BUSY") || status.contains("PAUSED") || status.contains("HELD") -> "Busy"
        status.contains("OPEN") || status.contains("ACTIVE") || status.contains("IDLE") -> "Open"
        status.isNotBlank() -> status.lowercase().replaceFirstChar { it.uppercase() }
        else -> "Unavailable"
    }
}

/**
 * Part / BOM category for Products & BOM chips — Figma `2545:29477`.
 * Uses gstClassification written at create (`category=…`); falls back to kind/name heuristics.
 */
internal fun mfgPartCategory(p: OperatingMasterDto): String {
    val classif = p.gstClassification.orEmpty().uppercase()
    when {
        classif.contains("CONSUMABLE") -> return "Consumables"
        classif.contains("SPARE") -> return "Spares"
        classif.contains("WIP") || classif.contains("SEMI") -> return "WIP"
        classif.contains("RAW") || classif.contains("MATERIAL") -> return "Raw"
        classif.contains("FINISHED") || classif.contains("FG") -> return "Finished"
    }
    val blob = listOfNotNull(p.status, p.code, p.name, p.kind).joinToString(" ").uppercase()
    return when {
        blob.contains("CONSUMABLE") -> "Consumables"
        blob.contains("SPARE") -> "Spares"
        blob.contains("WIP") || blob.contains("SEMI") -> "WIP"
        blob.contains("RAW") || blob.contains("MATERIAL") -> "Raw"
        p.kind.equals("BOM", true) -> "Finished"
        else -> "Finished"
    }
}

internal fun mfgGstBadge(p: OperatingMasterDto): String? {
    val rate = p.gstRate?.trim()?.trimEnd('%')?.takeIf { it.isNotBlank() } ?: return null
    return "GST $rate%"
}

/** Forge & Form invite roles — Figma `2545:29335`. Backend codes stay FINANCE|APPROVER|OPERATIONS_LEAD|CONTRIBUTOR. */
val MfgInviteRoles = listOf(
    "FINANCE" to "Owner",
    "APPROVER" to "Manager",
    "OPERATIONS_LEAD" to "Supervisor",
    "CONTRIBUTOR" to "Storekeeper",
    "CONTRIBUTOR" to "Operator",
)

fun mfgInviteRoleAccess(roleCode: String): String = when (roleCode) {
    "FINANCE" ->
        "Owner → full access · reports, payouts and invites. The role you pick sets their access when they join"
    "APPROVER" ->
        "Manager → work orders, production approvals and QC · no payouts. The role you pick sets their access when they join"
    "OPERATIONS_LEAD" ->
        "Supervisor → work orders, production & QC · no payouts or pricing. The role you pick sets their access when they join"
    else ->
        "Storekeeper / Operator → stock, issue and line activity · no reports or payouts. The role you pick sets their access when they join"
}

internal val MfgInvitePalette = InvitePalette(
    accent = ManufacturingAmber.accent,
    accentMuted = ManufacturingAmber.accentSoft,
    card = ManufacturingAmber.cardSlate,
    field = ManufacturingAmber.chip,
    chip = ManufacturingAmber.chip,
    chipStroke = ManufacturingAmber.chipBorder,
    divider = ManufacturingAmber.divider,
    qrPlate = ManufacturingAmber.chip,
    textPrimary = ManufacturingAmber.textPrimary,
    textBody = ManufacturingAmber.textBody,
    textMuted = ManufacturingAmber.textMuted,
    textFaint = ManufacturingAmber.textFaint,
    warning = ManufacturingAmber.warning,
    negative = ManufacturingAmber.negative,
    chipSelected = ManufacturingAmber.accent,
    chipSelectedInk = ManufacturingAmber.onAccent,
    chipSelectedStroke = ManufacturingAmber.accentSoft,
)

@Composable
internal fun MfgMastersSheet(
    tab: MfgMasterTab,
    model: OperatingProjectionDto,
    onClose: () -> Unit,
    onCreate: (ManufacturingEventDraft) -> Unit,
    onOpenForm: (MfgMasterForm) -> Unit,
    onBrowseParts: (() -> Unit)? = null,
) {
    when (tab) {
        MfgMasterTab.INVITE -> MfgInviteList(model, onClose)
        MfgMasterTab.VENDORS -> MfgVendorsList(model, onClose, onOpenForm)
        MfgMasterTab.PRODUCTS_BOM -> MfgProductsBomList(model, onClose, onOpenForm, onBrowseParts)
        MfgMasterTab.LINES_STORES -> MfgLinesStoresList(model, onClose, onOpenForm)
    }
}

enum class MfgMasterForm { VENDOR, LINE_STORE, PART, BOM }

@Composable
private fun MfgInviteList(
    model: OperatingProjectionDto,
    onClose: () -> Unit,
) {
    val plant = model.title?.takeIf { it.isNotBlank() } ?: "Forge & Form"
    var inviteError by remember { mutableStateOf<String?>(null) }
    MfgSheetBody(Modifier.verticalScroll(rememberScrollState())) {
        MfgGrabber()
        MfgSheetHeader(
            title = "Invite team · $plant",
            subtitle = "$plant · invite grants a role",
            onClose = onClose,
        )
        BusinessInviteQr(
            momentId = OperatingChromeState.momentId,
            title = plant,
            role = model.role,
            palette = MfgInvitePalette,
            noun = "plant",
            onError = { inviteError = it },
            roles = MfgInviteRoles,
            roleAccess = ::mfgInviteRoleAccess,
            showIntroHeader = false,
            defaultRoleCode = "OPERATIONS_LEAD",
        )
        inviteError?.let {
            MfgEmpty("Invite error", it)
        }
        MfgWhyBox("Invite grants a role and its access. Joining ≠ approval ≠ payment.")
        MfgStickyFooter(
            label = "Done",
            enabled = true,
            caveat = "Pending invites · roles apply on join",
            onClick = onClose,
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun MfgVendorsList(model: OperatingProjectionDto, onClose: () -> Unit, onOpenForm: (MfgMasterForm) -> Unit) {
    val vendors = model.masters.orEmpty().filter {
        it.kind.equals("SUPPLIER", true) || it.kind.equals("VENDOR", true) || it.kind.equals("BUYER", true)
    }
    var query by remember { mutableStateOf("") }
    val filtered = vendors.filter { query.isBlank() || it.name.contains(query, true) }
    MfgSheetBody(Modifier.verticalScroll(rememberScrollState())) {
        MfgGrabber()
        MfgSheetHeader("Vendors & buyers", "Preferred · Approved", onClose = onClose)
        MfgTextField(query, "Search vendors", onValueChange = { query = it })
        filtered.forEach { v ->
            val badge = mfgVendorBadge(v)
            MfgPickerRow(
                v.name,
                listOfNotNull(
                    v.gstin?.takeIf { it.isNotBlank() }?.let { "GSTIN $it" },
                    badge,
                ).joinToString(" · "),
                // D10: Edit locked without hydrate — open Create form only via + New.
                onClick = null,
            )
        }
        if (vendors.isEmpty()) MfgEmpty("No vendors yet", "Add a vendor to buy materials and job work.")
        MfgDenseCreateCta("+ New vendor", "Supplies · GSTIN · terms", onClick = { onOpenForm(MfgMasterForm.VENDOR) })
        MfgLockedChip("Edit existing · Unavailable until hydrate")
        MfgWhyBox("Vendor list is identity only. GSTIN never posts AP or GST — invoices do.")
        MfgStickyFooter(
            label = "Done",
            enabled = true,
            caveat = "${filtered.size} of ${vendors.size} vendors",
            onClick = onClose,
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun MfgProductsBomList(
    model: OperatingProjectionDto,
    onClose: () -> Unit,
    onOpenForm: (MfgMasterForm) -> Unit,
    onBrowseParts: (() -> Unit)?,
) {
    val parts = model.masters.orEmpty().filter {
        it.kind.equals("SKU", true) || it.kind.equals("PART", true) || it.kind.equals("PRODUCT", true) || it.kind.equals("BOM", true)
    }
    var cat by remember { mutableStateOf("Finished") }
    val filtered = parts.filter { mfgPartCategory(it) == cat }
    val currency = model.currency ?: "INR"
    MfgSheetBody(Modifier.verticalScroll(rememberScrollState())) {
        MfgGrabber()
        MfgSheetHeader(
            "Products & BOM",
            "Products & BOM · raw, WIP and finished parts with HSN + GST before SO / WO",
            onClose = onClose,
        )
        MfgChipRow(null, listOf("Raw", "WIP", "Finished", "Consumables", "Spares"), cat) { cat = it }
        filtered.forEach { p ->
            val category = mfgPartCategory(p)
            val meta = when {
                p.kind.equals("BOM", true) -> "$category · BOM"
                !p.unit.isNullOrBlank() -> "$category · ${p.unit}"
                else -> category
            }
            val price = p.sellPrice?.takeIf { it.isNotBlank() }?.let { MfgFormat.exact(currency, it) } ?: "—"
            MfgCatalogPartRow(
                name = p.name,
                meta = meta,
                priceLabel = price,
                gstBadge = mfgGstBadge(p) ?: "GST · —",
                editLocked = true,
                accentDot = category == "Finished" || category == "Raw",
            )
        }
        if (filtered.isEmpty()) {
            MfgEmpty(
                if (parts.isEmpty()) "No parts yet" else "No $cat parts",
                if (parts.isEmpty()) "Create a part / SKU or BOM." else "Switch chip or create a $cat part.",
            )
        }
        MfgDenseCreateCta("+ New part / SKU", "Sell price · HSN · GST chip", onClick = { onOpenForm(MfgMasterForm.PART) })
        MfgDenseCreateCta("+ New BOM", "Routing · scrap % · backflush", onClick = { onOpenForm(MfgMasterForm.BOM) })
        onBrowseParts?.let {
            MfgDenseCreateCta("Browse parts · SO / WO", "Pick into order draft", onClick = it)
        }
        MfgWhyBox(
            "Lists saved parts and BOMs. GST badge is the HSN rate — output GST posts on Sale invoice, never on SO or work-order save. Edit locked without hydrate (D10).",
        )
        MfgStickyFooter(
            label = "Done",
            enabled = true,
            caveat = "${filtered.size} of ${parts.size} parts · $cat",
            onClick = onClose,
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun MfgLinesStoresList(model: OperatingProjectionDto, onClose: () -> Unit, onOpenForm: (MfgMasterForm) -> Unit) {
    val locs = model.masters.orEmpty().filter {
        it.kind.equals("LOCATION", true) || it.kind.equals("STORE", true) || it.kind.equals("LINE", true) || it.kind.equals("STATION", true)
    }
    MfgSheetBody(Modifier.verticalScroll(rememberScrollState())) {
        MfgGrabber()
        MfgSheetHeader("Lines & stores", "Line · Station · Store · Busy/Open", onClose = onClose)
        locs.forEach { loc ->
            val capacity = mfgLineCapacity(loc)
            val typeLabel = when {
                loc.kind.equals("LINE", true) -> "Line"
                loc.kind.equals("STATION", true) -> "Station"
                loc.kind.equals("STORE", true) -> "Store"
                else -> loc.kind.replaceFirstChar { it.uppercase() }
            }
            MfgPickerRow(
                loc.name,
                "$typeLabel · $capacity",
                // D10: Edit locked without hydrate.
                onClick = null,
            )
        }
        if (locs.isEmpty()) MfgEmpty("No lines or stores", "Add a line, station, or store.")
        MfgDenseCreateCta("+ Add line / store", "Type · printer · holds stock", onClick = { onOpenForm(MfgMasterForm.LINE_STORE) })
        MfgLockedChip("Edit existing · Unavailable until hydrate")
        MfgWhyBox("Lines and stores hold capacity only. Busy/Open never invents stock or WIP balances.")
        MfgStickyFooter(
            label = "Done",
            enabled = true,
            caveat = "${locs.size} lines & stores",
            onClick = onClose,
        )
        Spacer(Modifier.height(16.dp))
    }
}

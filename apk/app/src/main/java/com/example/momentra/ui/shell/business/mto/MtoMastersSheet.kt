package com.example.momentra.ui.shell.business.mto
import com.example.momentra.ui.shell.business.manufacturing.ManufacturingEventDraft

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
import com.example.momentra.ui.theme.MadeToOrderAmber

enum class MtoMasterTab { INVITE, VENDORS, PRODUCTS_BOM, LINES_STORES }

/**
 * Figma Customers & suppliers badges: Customer / Supplier / Job-work (+ status when known).
 * Prefer real master kind/status; never invent Preferred from an empty GSTIN.
 */
internal fun mtoVendorBadge(v: OperatingMasterDto): String {
    val kind = v.kind.orEmpty().uppercase()
    val status = v.status.orEmpty().uppercase()
    val note = listOfNotNull(v.name, v.code, v.status).joinToString(" ").uppercase()
    val role = when {
        note.contains("JOB WORK") || note.contains("JOB-WORK") || note.contains("JOBWORK") ||
            status.contains("JOB") -> "Job-work"
        kind.contains("CUSTOMER") || kind.contains("BUYER") || kind.contains("PARTY") -> "Customer"
        kind.contains("SUPPLIER") || kind.contains("VENDOR") -> "Supplier"
        else -> "Supplier"
    }
    val statusBadge = when {
        status.contains("PREFERRED") -> "Preferred"
        status.contains("INACTIVE") || status.contains("ARCHIVED") -> "Inactive"
        status.contains("ACTIVE") || status.contains("APPROVED") -> "Active"
        status.isNotBlank() -> status.lowercase().replaceFirstChar { it.uppercase() }
        !v.gstin.isNullOrBlank() -> "Active"
        else -> "Unavailable"
    }
    return "$role · $statusBadge"
}

/** Figma Lines & stores: Active / Planned (Busy/Open when capacity known); else Unavailable. */
internal fun mtoLineCapacity(loc: OperatingMasterDto): String {
    val status = loc.status.orEmpty().uppercase()
    return when {
        status.contains("PLANNED") || status.contains("PENDING") || status.contains("DRAFT") -> "Planned"
        status.contains("BUSY") || status.contains("PAUSED") || status.contains("HELD") -> "Busy"
        status.contains("OPEN") || status.contains("ACTIVE") || status.contains("IDLE") -> "Active"
        status.isNotBlank() -> status.lowercase().replaceFirstChar { it.uppercase() }
        else -> "Unavailable"
    }
}

/**
 * Part / BOM category for Products & BOM chips — Figma `2545:29477`.
 * Uses gstClassification written at create (`category=…`); falls back to kind/name heuristics.
 */
internal fun mtoPartCategory(p: OperatingMasterDto): String {
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

internal fun mtoGstBadge(p: OperatingMasterDto): String? {
    val rate = p.gstRate?.trim()?.trimEnd('%')?.takeIf { it.isNotBlank() } ?: return null
    return "GST $rate%"
}

/** live moment invite roles — Figma `2545:29335`. Backend codes stay FINANCE|APPROVER|OPERATIONS_LEAD|CONTRIBUTOR. */
val MtoInviteRoles = listOf(
    "FINANCE" to "Owner",
    "APPROVER" to "Manager",
    "OPERATIONS_LEAD" to "Job lead",
    "CONTRIBUTOR" to "Estimator",
    "CONTRIBUTOR" to "Operator",
)

fun mtoInviteRoleAccess(roleCode: String): String = when (roleCode) {
    "FINANCE" ->
        "Owner → full access · quotes, invoices and invites. Join ≠ quote approval ≠ payment."
    "APPROVER" ->
        "Manager → confirm orders and job approvals · no payouts. Join ≠ quote approval ≠ payment."
    "OPERATIONS_LEAD" ->
        "Job lead → open jobs, production & dispatch · no payouts or pricing. Join ≠ quote approval ≠ payment."
    else ->
        "Estimator / Operator → quotes, production and job activity · no reports or payouts. Join ≠ quote approval ≠ payment."
}

internal val MtoInvitePalette = InvitePalette(
    accent = MadeToOrderAmber.accent,
    accentMuted = MadeToOrderAmber.accentSoft,
    card = MadeToOrderAmber.cardSlate,
    field = MadeToOrderAmber.chip,
    chip = MadeToOrderAmber.chip,
    chipStroke = MadeToOrderAmber.chipBorder,
    divider = MadeToOrderAmber.divider,
    qrPlate = MadeToOrderAmber.chip,
    textPrimary = MadeToOrderAmber.textPrimary,
    textBody = MadeToOrderAmber.textBody,
    textMuted = MadeToOrderAmber.textMuted,
    textFaint = MadeToOrderAmber.textFaint,
    warning = MadeToOrderAmber.warning,
    negative = MadeToOrderAmber.negative,
    chipSelected = MadeToOrderAmber.accent,
    chipSelectedInk = MadeToOrderAmber.onAccent,
    chipSelectedStroke = MadeToOrderAmber.accentSoft,
)

@Composable
internal fun MtoMastersSheet(
    tab: MtoMasterTab,
    model: OperatingProjectionDto,
    onClose: () -> Unit,
    onCreate: (ManufacturingEventDraft) -> Unit,
    onOpenForm: (MtoMasterForm) -> Unit,
    onBrowseParts: (() -> Unit)? = null,
) {
    when (tab) {
        MtoMasterTab.INVITE -> MtoInviteList(model, onClose)
        MtoMasterTab.VENDORS -> MtoVendorsList(model, onClose, onOpenForm)
        MtoMasterTab.PRODUCTS_BOM -> MtoProductsBomList(model, onClose, onOpenForm, onBrowseParts)
        MtoMasterTab.LINES_STORES -> MtoLinesStoresList(model, onClose, onOpenForm)
    }
}

enum class MtoMasterForm { VENDOR, LINE_STORE, PART, BOM }

@Composable
private fun MtoInviteList(
    model: OperatingProjectionDto,
    onClose: () -> Unit,
) {
    val plant = model.title?.takeIf { it.isNotBlank() } ?: "live moment"
    var inviteError by remember { mutableStateOf<String?>(null) }
    MtoSheetBody(Modifier.verticalScroll(rememberScrollState())) {
        MtoGrabber()
        MtoSheetHeader(
            title = "Invite team · $plant",
            subtitle = "$plant · invite grants a role",
            onClose = onClose,
        )
        BusinessInviteQr(
            momentId = OperatingChromeState.momentId,
            title = plant,
            role = model.role,
            palette = MtoInvitePalette,
            noun = "plant",
            onError = { inviteError = it },
            roles = MtoInviteRoles,
            roleAccess = ::mtoInviteRoleAccess,
            showIntroHeader = false,
            defaultRoleCode = "OPERATIONS_LEAD",
        )
        inviteError?.let {
            MtoEmpty("Invite error", it)
        }
        MtoWhyNote("Invite grants a role and its access. Joining ≠ approval ≠ payment.")
        MtoStickyFooter(
            label = "Done",
            enabled = true,
            caveat = "Pending invites · roles apply on join",
            onClick = onClose,
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun MtoVendorsList(model: OperatingProjectionDto, onClose: () -> Unit, onOpenForm: (MtoMasterForm) -> Unit) {
    val vendors = model.masters.orEmpty().filter {
        it.kind.equals("SUPPLIER", true) ||
            it.kind.equals("VENDOR", true) ||
            it.kind.equals("BUYER", true) ||
            it.kind.equals("CUSTOMER", true) ||
            it.kind.equals("PARTY", true)
    }
    var query by remember { mutableStateOf("") }
    val filtered = vendors.filter { query.isBlank() || it.name.contains(query, true) }
    MtoSheetBody(Modifier.verticalScroll(rememberScrollState())) {
        MtoGrabber()
        MtoSheetHeader("Customers & suppliers", "Customer · Supplier · Job-work", onClose = onClose)
        MtoTextField(query, "Search parties", onValueChange = { query = it })
        filtered.forEach { v ->
            val badge = mtoVendorBadge(v)
            MtoPickerRow(
                v.name,
                listOfNotNull(
                    badge,
                    v.gstin?.takeIf { it.isNotBlank() }?.let { "GSTIN $it" },
                ).joinToString(" · "),
                // D9: Edit locked without hydrate — open Create form only via + New.
                onClick = null,
            )
        }
        if (vendors.isEmpty()) MtoEmpty("No parties yet", "Add a customer, supplier, or job-work party.")
        MtoDenseCreateCta("+ New customer / supplier", "Role · GSTIN · terms", onClick = { onOpenForm(MtoMasterForm.VENDOR) })
        MtoLockedChip("Edit existing · Unavailable until hydrate")
        MtoWhyNote("Party list is identity only. GSTIN never posts AP or GST — invoices do.")
        MtoStickyFooter(
            label = "Done",
            enabled = true,
            caveat = "${filtered.size} of ${vendors.size} parties",
            onClick = onClose,
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun MtoProductsBomList(
    model: OperatingProjectionDto,
    onClose: () -> Unit,
    onOpenForm: (MtoMasterForm) -> Unit,
    onBrowseParts: (() -> Unit)?,
) {
    val parts = model.masters.orEmpty().filter {
        it.kind.equals("SKU", true) || it.kind.equals("PART", true) || it.kind.equals("PRODUCT", true) || it.kind.equals("BOM", true)
    }
    var cat by remember { mutableStateOf("Finished") }
    val filtered = parts.filter { mtoPartCategory(it) == cat }
    val currency = model.currency ?: "INR"
    MtoSheetBody(Modifier.verticalScroll(rememberScrollState())) {
        MtoGrabber()
        MtoSheetHeader(
            "Products & BOM",
            "Products & BOM · raw, WIP and finished parts with HSN + GST before SO / WO",
            onClose = onClose,
        )
        MtoChipRow(null, listOf("Raw", "WIP", "Finished", "Consumables", "Spares"), cat) { cat = it }
        filtered.forEach { p ->
            val category = mtoPartCategory(p)
            val meta = when {
                p.kind.equals("BOM", true) -> "$category · BOM"
                !p.unit.isNullOrBlank() -> "$category · ${p.unit}"
                else -> category
            }
            val price = p.sellPrice?.takeIf { it.isNotBlank() }?.let { MtoFormat.exact(currency, it) } ?: "—"
            MtoCatalogPartRow(
                name = p.name,
                meta = meta,
                priceLabel = price,
                gstBadge = mtoGstBadge(p) ?: "GST · —",
                editLocked = true,
                accentDot = category == "Finished" || category == "Raw",
            )
        }
        if (filtered.isEmpty()) {
            MtoEmpty(
                if (parts.isEmpty()) "No parts yet" else "No $cat parts",
                if (parts.isEmpty()) "Create a part / SKU or BOM." else "Switch chip or create a $cat part.",
            )
        }
        MtoDenseCreateCta("+ New part / SKU", "Sell price · HSN · GST chip", onClick = { onOpenForm(MtoMasterForm.PART) })
        MtoDenseCreateCta("+ New BOM", "Routing · scrap % · backflush", onClick = { onOpenForm(MtoMasterForm.BOM) })
        onBrowseParts?.let {
            MtoDenseCreateCta("Browse parts · SO / WO", "Pick into order draft", onClick = it)
        }
        MtoWhyNote(
            "Lists saved parts and BOMs. GST badge is the HSN rate — output GST posts on Sale invoice, never on SO or work-order save. Edit locked without hydrate (D10).",
        )
        MtoStickyFooter(
            label = "Done",
            enabled = true,
            caveat = "${filtered.size} of ${parts.size} parts · $cat",
            onClick = onClose,
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun MtoLinesStoresList(model: OperatingProjectionDto, onClose: () -> Unit, onOpenForm: (MtoMasterForm) -> Unit) {
    val locs = model.masters.orEmpty().filter {
        it.kind.equals("LOCATION", true) || it.kind.equals("STORE", true) || it.kind.equals("LINE", true) || it.kind.equals("STATION", true)
    }
    MtoSheetBody(Modifier.verticalScroll(rememberScrollState())) {
        MtoGrabber()
        MtoSheetHeader("Lines & stores", "Line · Station · Store · Active/Planned", onClose = onClose)
        locs.forEach { loc ->
            val capacity = mtoLineCapacity(loc)
            val typeLabel = when {
                loc.kind.equals("LINE", true) -> "Line"
                loc.kind.equals("STATION", true) -> "Station"
                loc.kind.equals("STORE", true) -> "Store"
                else -> loc.kind.replaceFirstChar { it.uppercase() }
            }
            MtoPickerRow(
                loc.name,
                "$typeLabel · $capacity",
                // D9: Edit locked without hydrate.
                onClick = null,
            )
        }
        if (locs.isEmpty()) MtoEmpty("No lines or stores", "Add a line, station, or store.")
        MtoDenseCreateCta("+ Add line / store", "Type · printer · holds stock", onClick = { onOpenForm(MtoMasterForm.LINE_STORE) })
        MtoLockedChip("Edit existing · Unavailable until hydrate")
        MtoWhyNote("Lines and stores hold capacity only. Active/Planned never invents stock or WIP balances.")
        MtoStickyFooter(
            label = "Done",
            enabled = true,
            caveat = "${locs.size} lines & stores",
            onClick = onClose,
        )
        Spacer(Modifier.height(16.dp))
    }
}

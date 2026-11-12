package com.example.momentra.ui.shell.business.mto

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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.R
import com.example.momentra.data.api.OperatingProjectionDto
import com.example.momentra.ui.shell.business.manufacturing.ManufacturingEventDraft
import com.example.momentra.ui.shell.business.shared.OperatingChromeState
import com.example.momentra.ui.shell.business.shared.OperatingSheetScrim
import com.example.momentra.ui.theme.MadeToOrderAmber
import com.example.momentra.ui.theme.PlusJakartaSans
import kotlinx.coroutines.launch

/**
 * `MTO-HUB · Quick Adds` `2686:30690` + dedicated MTO sheet router (29 Figma frames).
 *
 * Core tiles are the make-to-order journey (Quote → Confirm → Open job → Produce → Invoice).
 * HOLD / TRANSFER / REPRINT → [MtoUtilitySheets]; masters → [MtoMastersSheet]; else [MtoCaptureSheet].
 */
@Composable
fun MadeToOrderQuickAddHub(
    model: OperatingProjectionDto,
    onRecord: suspend (ManufacturingEventDraft) -> Unit,
    onDecide: suspend (String, String) -> Unit,
) {
    val allowed = model.quickAdds.map { it.code }.toSet()
    var sheet by remember { mutableStateOf<MtoSheet?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val accent = MadeToOrderAmber.accent
    val role = model.role.orEmpty()
    val approver = role == "APPROVER" || role == "OWNER_ADMIN"

    fun openCapture(code: String) {
        val upper = code.uppercase()
        sheet = when (upper) {
            "HOLD" -> MtoSheet.Utility(MtoUtilityNav.HOLD)
            "TRANSFER", "WIP" -> MtoSheet.Utility(MtoUtilityNav.TRANSFER)
            "REPRINT" -> MtoSheet.Utility(MtoUtilityNav.REPRINT)
            "INVITE", "PERSON", "ROLE" -> MtoSheet.Masters(MtoMasterTab.INVITE)
            "SUPPLIER", "VENDORS", "PRINCIPALS" -> MtoSheet.Masters(MtoMasterTab.VENDORS)
            "CATALOGUE", "PRODUCTS", "PARTS" -> MtoSheet.Masters(MtoMasterTab.PRODUCTS_BOM)
            "LOCATIONS", "LINES" -> MtoSheet.Masters(MtoMasterTab.LINES_STORES)
            else -> {
                val capture = MtoCapture.entries.firstOrNull {
                    it.captureMode == upper || it.name == upper || it.aliases.any { a -> a.equals(upper, true) }
                }
                MtoSheet.Capture(capture?.captureMode ?: upper)
            }
        }
    }

    LaunchedEffect(OperatingChromeState.quickAddHint) {
        val hint = OperatingChromeState.quickAddHint?.uppercase() ?: return@LaunchedEffect
        openCapture(hint)
        OperatingChromeState.quickAddHint = null
    }

    Box(Modifier.fillMaxSize()) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            "Quick Adds",
            color = Color(0xFFF8F9FD),
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
        )
        Text(
            "${model.title?.takeIf { it.isNotBlank() } ?: "Make to order"} · Make to order",
            color = MadeToOrderAmber.accentSoft,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
        )
        MtoContextChips(model, MadeToOrderAmber.accentSoft)
        MtoHero(
            accentLead = MadeToOrderAmber.accentSoft,
            accentSolid = MadeToOrderAmber.accent,
            onQuote = { openCapture("QUOTATION") },
            onConfirm = { openCapture("CONFIRM") },
        )
        MtoSectionHeader("Core · 9", "Make-to-order journey")
        MtoTileGrid(
            MtoCoreTiles,
            allowed,
            accent,
            onSheet = { openCapture(it.captureMode) },
            onMasters = { tab ->
                sheet = when {
                    tab.equals("Catalogue", true) -> MtoSheet.Masters(MtoMasterTab.PRODUCTS_BOM)
                    tab.equals("Locations", true) -> MtoSheet.Masters(MtoMasterTab.LINES_STORES)
                    tab.equals("Principals", true) -> MtoSheet.Masters(MtoMasterTab.VENDORS)
                    tab.equals("Invite", true) -> MtoSheet.Masters(MtoMasterTab.INVITE)
                    else -> MtoSheet.Masters(MtoMasterTab.VENDORS)
                }
            },
            onApproval = { openCapture("APPROVAL") },
        )
        MtoSectionHeader("Support · 5", "Network · masters")
        MtoTileGrid(
            MtoSupportTiles,
            allowed,
            accent,
            onSheet = { openCapture(it.captureMode) },
            onMasters = { tab ->
                sheet = when {
                    tab.equals("Catalogue", true) -> MtoSheet.Masters(MtoMasterTab.PRODUCTS_BOM)
                    tab.equals("Locations", true) -> MtoSheet.Masters(MtoMasterTab.LINES_STORES)
                    tab.equals("Principals", true) -> MtoSheet.Masters(MtoMasterTab.VENDORS)
                    tab.equals("Invite", true) -> MtoSheet.Masters(MtoMasterTab.INVITE)
                    else -> MtoSheet.Masters(MtoMasterTab.VENDORS)
                }
            },
            onApproval = { openCapture("APPROVAL") },
        )
        MtoSectionHeader("Utilities · 12", "Job ops · hold · dispatch · revise")
        MtoTileGrid(
            MtoUtilityTiles,
            allowed,
            accent,
            onSheet = { openCapture(it.captureMode) },
            onMasters = {},
            onApproval = { openCapture("APPROVAL") },
        )
        MtoWhyNoteHub(accent)
        if (error != null) {
            Text(error ?: "", color = Color(0xFFF87171), fontFamily = PlusJakartaSans, fontSize = 13.sp)
        }
        if (approver) {
            Text(
                "Review approvals",
                color = accent,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clickable { openCapture("APPROVAL") }
                    .padding(vertical = 4.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
    }

    fun post(draft: ManufacturingEventDraft, captureMode: String? = null) {
        scope.launch {
            runCatching { onRecord(draft) }
                .onSuccess {
                    MtoToastHost.show(
                        MtoToastCatalog.forCaptureSuccess(
                            captureMode = captureMode ?: draft.eventType,
                            eventType = draft.eventType,
                            entity = draft.title.ifBlank { model.title ?: "this job" },
                        ),
                    )
                }
                .onFailure { error = it.message }
            sheet = null
        }
    }

    sheet?.let { open ->
        OperatingSheetScrim(
            onDismiss = { sheet = null },
            accent = MadeToOrderAmber.accent,
            surface = MadeToOrderAmber.cardSlate,
            border = MadeToOrderAmber.accent.copy(alpha = 0.4f),
            topCorner = 28.dp,
        ) {
            when (open) {
                is MtoSheet.Utility -> when (open.nav) {
                    MtoUtilityNav.HOLD -> MtoHoldJobSheet(
                        model = model,
                        onDismiss = { sheet = null },
                        onNavigate = { sheet = MtoSheet.Utility(it) },
                        onSubmit = { post(it, "HOLD") },
                    )
                    MtoUtilityNav.TRANSFER -> MtoTransferSheet(
                        model = model,
                        onDismiss = { sheet = null },
                        onNavigate = { sheet = MtoSheet.Utility(it) },
                        onSubmit = { post(it, "TRANSFER") },
                    )
                    MtoUtilityNav.REPRINT -> MtoReprintSheet(
                        model = model,
                        onDismiss = { sheet = null },
                        onNavigate = { sheet = MtoSheet.Utility(it) },
                    )
                }
                is MtoSheet.Capture -> MtoCaptureSheet(
                    mode = open.mode,
                    model = model,
                    onDismiss = { sheet = null },
                    onSubmit = { post(it, open.mode) },
                    onNavigateUtility = { sheet = MtoSheet.Utility(it) },
                )
                is MtoSheet.Masters -> MtoMastersSheet(
                    tab = open.tab,
                    model = model,
                    onClose = { sheet = null },
                    onCreate = { post(it) },
                    onOpenForm = { sheet = MtoSheet.Form(it) },
                    onBrowseParts = { sheet = MtoSheet.Browser(parts = true) },
                )
                is MtoSheet.Form -> MtoMasterFormSheet(
                    form = open.form,
                    model = model,
                    onDismiss = { sheet = null },
                    onSubmit = { post(it) },
                )
                is MtoSheet.Browser -> if (open.parts) {
                    MtoPartsBrowserSheet(
                        model = model,
                        onDismiss = { sheet = null },
                        onAdd = { part ->
                            OperatingChromeState.browserSelection =
                                com.example.momentra.ui.shell.business.shared.OperatingBrowserSelection(
                                    id = part.id,
                                    name = part.name,
                                    unit = part.unit,
                                    unitPrice = part.sellPrice,
                                    masterKind = part.kind,
                                    code = part.code,
                                    browserKind = com.example.momentra.ui.shell.business.shared.OperatingBrowserKind.Parts,
                                    captureHint = "ORDER",
                                )
                            sheet = MtoSheet.Capture("QUOTATION")
                        },
                        onNewPart = { sheet = MtoSheet.Form(MtoMasterForm.PART) },
                    )
                } else {
                    MtoMaterialsBrowserSheet(
                        model = model,
                        onDismiss = { sheet = null },
                        onAdd = { mat ->
                            OperatingChromeState.browserSelection =
                                com.example.momentra.ui.shell.business.shared.OperatingBrowserSelection(
                                    id = mat.id,
                                    name = mat.name,
                                    unit = mat.unit,
                                    unitPrice = mat.purchasePrice ?: mat.sellPrice,
                                    masterKind = mat.kind,
                                    code = mat.code,
                                    browserKind = com.example.momentra.ui.shell.business.shared.OperatingBrowserKind.Materials,
                                    captureHint = "PURCHASE",
                                )
                            sheet = MtoSheet.Capture("MATERIALS")
                        },
                        onNewBom = { sheet = MtoSheet.Form(MtoMasterForm.BOM) },
                    )
                }
            }
        }
    }

    MtoToastOverlay()

    @Suppress("UNUSED_EXPRESSION")
    onDecide
    } // Box
}

private sealed class MtoSheet {
    data class Capture(val mode: String) : MtoSheet()
    data class Utility(val nav: MtoUtilityNav) : MtoSheet()
    data class Masters(val tab: MtoMasterTab) : MtoSheet()
    data class Form(val form: MtoMasterForm) : MtoSheet()
    data class Browser(val parts: Boolean) : MtoSheet()
}

internal enum class MtoCapture(
    val title: String,
    val eventType: String,
    val captureMode: String,
    val icon: Int,
    val aliases: List<String> = emptyList(),
) {
    QUOTATION("Create Quotation", "CUSTOMER_ORDER", "ORDER", R.drawable.qa_clipboard, listOf("QUOTE", "QUOTATION")),
    CONFIRM("Accept / Confirm Order", "CUSTOMER_ORDER", "CONFIRM", R.drawable.qa_check, listOf("ACCEPT", "CONFIRM_ORDER")),
    OPEN_JOB("Open Production Job", "UPDATE", "OPEN_JOB", R.drawable.qa_factory, listOf("JOB", "OPENJOB")),
    PRODUCTION("Record Production", "PRODUCTION", "PRODUCTION", R.drawable.qa_factory),
    INVOICE("Invoice against order", "SALE", "SALE", R.drawable.qa_receipt, listOf("SALE", "INVOICE")),
    COLLECTION("Collection / advance", "COLLECTION", "COLLECTION", R.drawable.qa_arrow_left_right),
    PAYMENT("Record Payment", "PAYMENT", "PAYMENT", R.drawable.qa_credit_card),
    MATERIALS("Purchase job materials", "PURCHASE", "PURCHASE", R.drawable.qa_wallet, listOf("PURCHASE", "MATERIALS")),
    EXPENSE("Record Expense", "SPEND", "SPEND", R.drawable.qa_budget, listOf("SPEND")),
    RETURN("Return / Credit Note", "RETURN", "RETURN", R.drawable.qa_arrow_left_right),
    REWORK("Rework / Scrap / Job Work", "STOCK", "REWORK", R.drawable.qa_alert, listOf("SCRAP", "REWORK_SCRAP")),
    STOCK("Stock / Material Update", "STOCK", "STOCK", R.drawable.qa_package),
    WIP("Transfer stock / WIP", "STOCK", "TRANSFER", R.drawable.qa_shipment, listOf("TRANSFER", "WIP")),
    REPRINT("Reprint invoice / job card", "SALE", "REPRINT", R.drawable.qa_receipt),
    APPROVAL("Review Approval", "APPROVAL", "APPROVAL", R.drawable.qa_shield),
    UPDATE("Post Update", "UPDATE", "UPDATE", R.drawable.qa_message),
    BUDGET("Revise Budget", "BUDGET", "BUDGET", R.drawable.qa_budget),
    QUOTE_REVISE("Quote revise / expire", "CUSTOMER_ORDER", "QUOTE_REVISE", R.drawable.qa_clipboard, listOf("REVISE")),
    JOB_STATUS("Job status change", "UPDATE", "JOB_STATUS", R.drawable.qa_clipboard, listOf("STATUS")),
    ISSUE("Report Issue", "ISSUE", "ISSUE", R.drawable.qa_alert),
    HOLD("Hold / release job", "ORDER", "HOLD", R.drawable.qa_shield),
    DELIVERY("Delivery / dispatch", "DELIVERY", "DELIVERY", R.drawable.qa_shipment, listOf("DISPATCH", "SHIPMENT")),
}

private data class MtoTile(
    val label: String,
    val caption: String,
    val icon: Int,
    val capture: MtoCapture? = null,
    val requires: String? = null,
    val master: String? = null,
    val approval: Boolean = false,
    val wide: Boolean = false,
)

private val MtoCoreTiles = listOf(
    MtoTile("Quotation", "Quote → accept → job", R.drawable.qa_clipboard, MtoCapture.QUOTATION, "ORDER"),
    MtoTile("Confirm", "Accept order · no revenue", R.drawable.qa_check, MtoCapture.CONFIRM, "ORDER"),
    MtoTile("Open job", "From confirmed order", R.drawable.qa_factory, MtoCapture.OPEN_JOB, "UPDATE"),
    MtoTile("Production", "Against job · accept / reject", R.drawable.qa_factory, MtoCapture.PRODUCTION, "PRODUCTION"),
    MtoTile("Invoice", "Against order · GST", R.drawable.qa_receipt, MtoCapture.INVOICE, "SALE"),
    MtoTile("Collection", "Advance · allocate AR", R.drawable.qa_arrow_left_right, MtoCapture.COLLECTION, "COLLECTION"),
    MtoTile("Payment", "Settle open AP", R.drawable.qa_credit_card, MtoCapture.PAYMENT, "PAYMENT"),
    MtoTile("Materials", "Job materials · supplier", R.drawable.qa_wallet, MtoCapture.MATERIALS, "PURCHASE"),
    MtoTile("Expense", "Running cost · job link", R.drawable.qa_budget, MtoCapture.EXPENSE, "SPEND"),
)

private val MtoSupportTiles = listOf(
    MtoTile("Vendors", "Suppliers · buyers", R.drawable.qa_users, master = "Principals"),
    MtoTile("Products & BOM", "Parts · WIP · FG", R.drawable.qa_package, master = "Catalogue"),
    MtoTile("Lines & stores", "Stations · floors", R.drawable.qa_truck, master = "Locations"),
    MtoTile("Invite & QR", "Join · people · roles", R.drawable.qa_qr, master = "Invite", wide = true),
    MtoTile("Issue", "Update · blocker · risk", R.drawable.qa_alert, MtoCapture.ISSUE, "UPDATE", wide = true),
)

/** Utilities include Hold + Delivery (D3/D8) plus HTR and job-ops. */
private val MtoUtilityTiles = listOf(
    MtoTile("Return", "Credit · goods outcome", R.drawable.qa_arrow_left_right, MtoCapture.RETURN, "RETURN"),
    MtoTile("Rework", "Rework · scrap · job work", R.drawable.qa_alert, MtoCapture.REWORK, "STOCK"),
    MtoTile("Stock", "Adjust · count · custody", R.drawable.qa_package, MtoCapture.STOCK, "STOCK"),
    MtoTile("WIP", "Transfer between stations", R.drawable.qa_shipment, MtoCapture.WIP, "STOCK"),
    MtoTile("Hold", "Park job · no stock/cost", R.drawable.qa_shield, MtoCapture.HOLD, "ORDER"),
    MtoTile("Delivery", "Dispatch · accepted only", R.drawable.qa_shipment, MtoCapture.DELIVERY, "DELIVERY"),
    MtoTile("Reprint", "Invoice · job card", R.drawable.qa_receipt, MtoCapture.REPRINT, "SALE"),
    MtoTile("Approval", "Pending decisions", R.drawable.qa_shield, approval = true),
    MtoTile("Update", "Progress · note", R.drawable.qa_message, MtoCapture.UPDATE, "UPDATE"),
    MtoTile("Budget", "Versioned revise", R.drawable.qa_budget, MtoCapture.BUDGET, "BUDGET"),
    MtoTile("Quote revise", "Revise · expire", R.drawable.qa_clipboard, MtoCapture.QUOTE_REVISE, "ORDER"),
    MtoTile("Job status", "Active · blocked · hold", R.drawable.qa_clipboard, MtoCapture.JOB_STATUS, "UPDATE"),
)

@Composable
private fun MtoContextChips(model: OperatingProjectionDto, accent: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Chip("Make to order", accent, filled = true)
        Chip(periodLine(model), Color(0xFFA8A29E), filled = false)
        Chip(
            if (model.gstTracking.equals("YES", true)) "GST on" else "GST off",
            Color(0xFFA8A29E),
            filled = false,
        )
    }
}

@Composable
private fun Chip(label: String, color: Color, filled: Boolean) {
    Text(
        label,
        color = color,
        fontFamily = PlusJakartaSans,
        fontWeight = if (filled) FontWeight.Bold else FontWeight.Medium,
        fontSize = 12.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (filled) color.copy(alpha = 0.14f) else Color(0xFF2A2740))
            .border(1.dp, if (filled) color.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.08f), RoundedCornerShape(999.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun MtoHero(
    accentLead: Color,
    accentSolid: Color,
    onQuote: () -> Unit,
    onConfirm: () -> Unit,
) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .shadow(12.dp, shape, ambientColor = accentLead.copy(alpha = 0.18f), spotColor = accentSolid.copy(alpha = 0.18f))
            .clip(shape)
            .background(
                Brush.horizontalGradient(listOf(accentLead.copy(alpha = 0.20f), accentSolid.copy(alpha = 0.14f))),
            )
            .border(1.dp, accentLead.copy(alpha = 0.45f), shape)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("MAKE TO ORDER", color = accentLead, fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        Text("Quote. Confirm. Make. Invoice.", color = Color.White, fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 22.sp)
        Text(
            "Quote, confirm, make and invoice in one Moment — quotes and orders never become revenue until a posted invoice.",
            color = Color(0xFFF8F9FD),
            fontFamily = PlusJakartaSans,
            fontSize = 12.sp,
        )
        Spacer(Modifier.height(4.dp))
        HeroCta("+  Create Quotation", accentSolid, filled = true, onQuote)
        HeroCta("Confirm Order", accentLead, filled = false, onConfirm)
    }
}

@Composable
private fun HeroCta(label: String, accent: Color, filled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (filled) Color.White else Color.White.copy(alpha = 0.06f))
            .border(1.dp, if (filled) Color.Transparent else accent.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (filled) accent else Color(0xFFF8F9FD),
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
        )
    }
}

@Composable
private fun MtoSectionHeader(title: String, trailing: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = Color(0xFFF8F9FD), fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.weight(1f))
        Text(trailing, color = Color(0xFFA8A29E), fontFamily = PlusJakartaSans, fontWeight = FontWeight.Medium, fontSize = 11.sp)
    }
}

@Composable
private fun MtoTileGrid(
    tiles: List<MtoTile>,
    allowed: Set<String>,
    accent: Color,
    onSheet: (MtoCapture) -> Unit,
    onMasters: (String) -> Unit,
    onApproval: () -> Unit,
) {
    val columns = 3
    val rows = buildList {
        var row = mutableListOf<MtoTile>()
        tiles.forEach { tile ->
            if (tile.wide) {
                if (row.isNotEmpty()) {
                    add(row.toList())
                    row = mutableListOf()
                }
                val last = lastOrNull()
                if (last != null && last.firstOrNull()?.wide == true && last.size < 2) {
                    removeAt(lastIndex)
                    add(last + tile)
                } else {
                    add(listOf(tile))
                }
            } else {
                row += tile
                if (row.size == columns) {
                    add(row.toList())
                    row = mutableListOf()
                }
            }
        }
        if (row.isNotEmpty()) add(row.toList())
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { cells ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                cells.forEachIndexed { index, tile ->
                    val capability = when (tile.requires) {
                        "ORDER" -> listOf("ORDER", "CUSTOMER_ORDER")
                        "DELIVERY" -> listOf("DELIVERY", "SHIPMENT", "UPDATE")
                        else -> listOfNotNull(tile.requires)
                    }
                    val locked = when {
                        tile.master != null || tile.approval -> null
                        tile.requires == null || allowed.isEmpty() -> null
                        capability.none { it in allowed } ->
                            if (tile.requires == "RETURN") "Unavailable" else "Role locked"
                        else -> null
                    }
                    val weight = when {
                        cells.size == 1 -> 1f
                        cells.all { it.wide } -> 1f
                        cells.size < columns && index == cells.lastIndex -> (columns - cells.size + 1).toFloat()
                        else -> 1f
                    }
                    MtoQuickTile(
                        tile = tile,
                        accent = accent,
                        locked = locked,
                        modifier = Modifier.weight(weight),
                        onClick = {
                            when {
                                tile.master != null -> onMasters(tile.master)
                                tile.approval -> onApproval()
                                tile.capture != null -> onSheet(tile.capture)
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun MtoQuickTile(
    tile: MtoTile,
    accent: Color,
    locked: String?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val enabled = locked == null
    Column(
        modifier
            .height(112.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1C1F28))
            .border(1.dp, accent.copy(alpha = if (enabled) 0.35f else 0.12f), RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(accent.copy(alpha = 0.16f))
                .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(tile.icon),
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(18.dp),
            )
        }
        Text(
            tile.label,
            color = Color(0xFFF8F9FD).copy(alpha = if (enabled) 1f else 0.45f),
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            locked ?: tile.caption,
            color = Color(0xFFA8A29E),
            fontFamily = PlusJakartaSans,
            fontSize = 10.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun MtoWhyNoteHub(accent: Color) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(accent.copy(alpha = 0.12f))
            .border(1.dp, accent.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Why", color = accent, fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        Text(
            "Financial boundaries: Quote/order/advance ≠ revenue. Only posted invoice owns revenue/AR. Accepted production posts finished stock once — rejected/rework never inflate sellable qty. Empty analytics = Unavailable, never zero.",
            color = accent,
            fontFamily = PlusJakartaSans,
            fontSize = 11.sp,
            modifier = Modifier.weight(1f),
        )
    }
}

private fun periodLine(model: OperatingProjectionDto): String {
    val start = model.startDate?.take(7) ?: return model.currency ?: "INR"
    val parts = start.split("-")
    val month = parts.getOrNull(1)?.toIntOrNull()?.let {
        listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec").getOrNull(it - 1)
    } ?: start
    val year = parts.getOrNull(0) ?: ""
    return "$month $year · ${model.currency ?: "INR"}"
}

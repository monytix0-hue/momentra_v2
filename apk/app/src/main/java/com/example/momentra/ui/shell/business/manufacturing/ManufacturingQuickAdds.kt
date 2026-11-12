package com.example.momentra.ui.shell.business.manufacturing

import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.api.OperatingProjectionDto
import com.example.momentra.ui.shell.business.shared.OperatingChromeState
import com.example.momentra.ui.shell.business.shared.OperatingSheetScrim
import com.example.momentra.ui.theme.ManufacturingAmber
import com.example.momentra.ui.theme.PlusJakartaSans
import kotlinx.coroutines.launch

data class ManufacturingEventDraft(
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
    val conversionToBase: String? = null,
    val dueAt: String? = null,
    val contract: com.example.momentra.data.api.OperatingContractBody? = null,
    val masterId: String? = null,
    val masterGstApplicable: String? = null,
    val masterGstClassification: String? = null,
    val masterGstRate: String? = null,
    val masterPriceIncludesGst: String? = null,
    val masterSalePriceIncludesGst: String? = null,
    val masterPurchasePriceIncludesGst: String? = null,
    val masterGstEffectiveFrom: String? = null,
    val masterHsn: String? = null,
    val masterGstRegistration: String? = null,
    val masterBusinessState: String? = null,
    val masterBillingState: String? = null,
    val masterDeliveryState: String? = null,
    val masterGstin: String? = null,
    val lines: List<com.example.momentra.data.api.OperatingLineBody>? = null,
    val rejectedQuantity: String? = null,
)

/** Hub capture modes — internal so JVM smoke tests can assert wiring without Compose. */
internal enum class ManufacturingCapture(
    val title: String,
    val eventType: String,
) {
    PRODUCTION("Record Production", "PRODUCTION"),
    SALE("Record Sale", "SALE"),
    PURCHASE("Material Purchase", "PURCHASE"),
    SPEND("Record Expense", "SPEND"),
    PAYMENT("Record Payment", "PAYMENT"),
    COLLECTION("Money collection", "COLLECTION"),
    CUSTOMER_ORDER("Customer / Job Order", "CUSTOMER_ORDER"),
    DELIVERY("Delivery / Dispatch", "DELIVERY"),
    BUDGET("Revise Budget", "BUDGET"),
    TARGET("Revise Target", "TARGET"),
    UPDATE("Post Update", "UPDATE"),
    ISSUE("Report Issue", "ISSUE"),
    STOCK("Stock / Material Update", "STOCK"),
    ORDER("Work Order Update", "ORDER"),
    SHIPMENT("Delivery Update", "SHIPMENT"),
    RETURN("Return / Credit Note", "RETURN"),
    REIMBURSE("Record Reimbursement", "PAYMENT"),
    PERSON("Add or invite person", "UPDATE"),
    ROLE("Add or invite person", "UPDATE"),
    INVITE("Invite & QR", "UPDATE"),
    SUPPLIER("Add Vendor / Supplier", "UPDATE"),
    HOLD("Hold / release work order", "ORDER"),
    TRANSFER("Transfer stock / WIP", "STOCK"),
    REPRINT("Reprint invoice / job card", "SALE"),
    REWORK("Rework / Scrap / Job Work", "STOCK"),
    APPROVAL("Review Approval", "APPROVAL"),
}

private sealed class MfgSheet {
    data class Capture(val mode: String) : MfgSheet()
    data class Utility(val nav: MfgUtilityNav) : MfgSheet()
    data class Masters(val tab: MfgMasterTab) : MfgSheet()
    data class Form(val form: MfgMasterForm) : MfgSheet()
    data class Browser(val parts: Boolean) : MfgSheet()
}

@Composable
fun ManufacturingQuickAddHub(
    model: OperatingProjectionDto,
    onRecord: suspend (ManufacturingEventDraft) -> Unit,
    onDecide: suspend (String, String) -> Unit,
) {
    var sheet by remember { mutableStateOf<MfgSheet?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val theme = remember(model.subtype) {
        com.example.momentra.ui.shell.business.shared.OperatingV12Theme.forSubtype(model.subtype)
    }
    val role = model.role.orEmpty()
    val approver = role == "APPROVER" || role == "OWNER_ADMIN"

    fun openCapture(code: String) {
        val upper = code.uppercase()
        sheet = when (upper) {
            "HOLD" -> MfgSheet.Utility(MfgUtilityNav.HOLD)
            "TRANSFER" -> MfgSheet.Utility(MfgUtilityNav.TRANSFER)
            "REPRINT" -> MfgSheet.Utility(MfgUtilityNav.REPRINT)
            "INVITE", "PERSON", "ROLE" -> MfgSheet.Masters(MfgMasterTab.INVITE)
            "SUPPLIER" -> MfgSheet.Masters(MfgMasterTab.VENDORS)
            // PO / materials entry — Figma Materials browser (`2547:29687`), not bare Purchase first.
            "PURCHASE" -> MfgSheet.Browser(parts = false)
            else -> MfgSheet.Capture(upper)
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
                "Plan. Make. Fulfil.",
                color = ManufacturingAmber.accentSoft,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
            )
            com.example.momentra.ui.shell.business.shared.OperatingV12HubScreen(
                model = model,
                theme = theme,
                onCapture = { code -> openCapture(code) },
                onOpenMasters = { tabName ->
                    when {
                        // Hub “Parts & BOM” → Products & BOM masters (Figma 2545:29477).
                        // Parts browser stays reachable from New part CTAs / SO·WO add paths.
                        tabName.equals("Catalogue", ignoreCase = true) ||
                            tabName.equals("Products", ignoreCase = true) ||
                            tabName.equals("Parts", ignoreCase = true) -> {
                            sheet = MfgSheet.Masters(MfgMasterTab.PRODUCTS_BOM)
                        }
                        tabName.equals("Materials", ignoreCase = true) ||
                            tabName.equals("Material", ignoreCase = true) -> {
                            sheet = MfgSheet.Browser(parts = false)
                        }
                        tabName.equals("Vendors", ignoreCase = true) ||
                            tabName.equals("Suppliers", ignoreCase = true) ||
                            tabName.equals("Principals", ignoreCase = true) -> {
                            sheet = MfgSheet.Masters(MfgMasterTab.VENDORS)
                        }
                        tabName.equals("Locations", ignoreCase = true) ||
                            tabName.equals("Lines", ignoreCase = true) -> {
                            sheet = MfgSheet.Masters(MfgMasterTab.LINES_STORES)
                        }
                        tabName.equals("Invite", ignoreCase = true) ||
                            tabName.equals("People", ignoreCase = true) -> {
                            sheet = MfgSheet.Masters(MfgMasterTab.INVITE)
                        }
                        else -> {
                            OperatingChromeState.mastersOpen = tabName
                        }
                    }
                },
            )
            if (error != null) {
                Text(
                    error ?: "",
                    color = ManufacturingAmber.negative,
                    fontFamily = PlusJakartaSans,
                    fontSize = 13.sp,
                )
            }
            Text(
                "Review approvals",
                color = if (approver) ManufacturingAmber.accent else ManufacturingAmber.textBody,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clickable(enabled = approver) { sheet = MfgSheet.Capture("APPROVAL") }
                    .padding(vertical = 4.dp),
            )
            Text(
                "Role-locked tiles stay visible with Role locked — capability gates unchanged.",
                color = ManufacturingAmber.textFaint,
                fontFamily = PlusJakartaSans,
                fontSize = 11.sp,
            )
        }

        val post: (ManufacturingEventDraft) -> Unit = { draft ->
            scope.launch {
                runCatching { onRecord(draft) }
                    .onFailure { error = it.message }
                sheet = null
            }
        }

        sheet?.let { open ->
            OperatingSheetScrim(
                onDismiss = { sheet = null },
                accent = ManufacturingAmber.accent,
                surface = ManufacturingAmber.cardSlate,
                border = ManufacturingAmber.accent.copy(alpha = 0.4f),
                topCorner = 28.dp,
            ) {
                when (open) {
                    is MfgSheet.Utility -> when (open.nav) {
                        MfgUtilityNav.HOLD -> MfgHoldWoSheet(
                            model = model,
                            onDismiss = { sheet = null },
                            onNavigate = { sheet = MfgSheet.Utility(it) },
                            onSubmit = post,
                        )
                        MfgUtilityNav.TRANSFER -> MfgTransferSheet(
                            model = model,
                            onDismiss = { sheet = null },
                            onNavigate = { sheet = MfgSheet.Utility(it) },
                            onSubmit = post,
                        )
                        MfgUtilityNav.REPRINT -> MfgReprintSheet(
                            model = model,
                            onDismiss = { sheet = null },
                            onNavigate = { sheet = MfgSheet.Utility(it) },
                        )
                    }
                    is MfgSheet.Capture -> MfgCaptureSheet(
                        mode = open.mode,
                        model = model,
                        onDismiss = { sheet = null },
                        onSubmit = post,
                        onNavigateUtility = { sheet = MfgSheet.Utility(it) },
                    )
                    is MfgSheet.Masters -> MfgMastersSheet(
                        tab = open.tab,
                        model = model,
                        onClose = { sheet = null },
                        onCreate = post,
                        onOpenForm = { sheet = MfgSheet.Form(it) },
                        onBrowseParts = { sheet = MfgSheet.Browser(parts = true) },
                    )
                    is MfgSheet.Form -> MfgMasterFormSheet(
                        form = open.form,
                        model = model,
                        onDismiss = { sheet = null },
                        onSubmit = post,
                    )
                    is MfgSheet.Browser -> if (open.parts) {
                        MfgPartsBrowserSheet(
                            model = model,
                            onDismiss = { sheet = null },
                            onAdd = { part ->
                                OperatingChromeState.partsBrowserOpen = null
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
                                sheet = MfgSheet.Capture("ORDER")
                            },
                            onNewPart = { sheet = MfgSheet.Form(MfgMasterForm.PART) },
                        )
                    } else {
                        MfgMaterialsBrowserSheet(
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
                                sheet = MfgSheet.Capture("PURCHASE")
                            },
                            onNewBom = { sheet = MfgSheet.Form(MfgMasterForm.BOM) },
                        )
                    }
                }
            }
        }

        // onDecide remains available for approval hosts; capture sheets post via onRecord.
        @Suppress("UNUSED_EXPRESSION")
        onDecide
    }
}

// ManufacturingCapture retained as route vocabulary for hub hint mapping.
@Suppress("unused")
private fun manufacturingCaptureHint(hint: String): ManufacturingCapture? =
    ManufacturingCapture.entries.firstOrNull { it.name == hint || it.eventType == hint }

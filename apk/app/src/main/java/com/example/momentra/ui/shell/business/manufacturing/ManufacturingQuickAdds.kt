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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.R
import com.example.momentra.data.api.OperatingProjectionDto
import com.example.momentra.ui.shell.business.shared.OperatingCaptureDraft
import com.example.momentra.ui.shell.business.shared.OperatingCaptureSheet
import com.example.momentra.ui.shell.business.shared.OperatingChromeState
import com.example.momentra.ui.shell.business.shared.OperatingSheetScrim
import com.example.momentra.ui.theme.PlusJakartaSans

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

private enum class ManufacturingCapture(
    val title: String,
    val eventType: String,
    val needsAmount: Boolean,
    val needsChannel: Boolean,
    val needsParty: Boolean,
    val paymentStatus: String?,
) {
    PRODUCTION("Record Production", "PRODUCTION", true, false, false, null),
    SALE("Record Sale", "SALE", true, true, false, null),
    PURCHASE("Material Purchase", "PURCHASE", true, false, true, null),
    SPEND("Record Expense", "SPEND", true, false, false, null),
    PAYMENT("Record Payment", "PAYMENT", true, false, true, "PENDING"),
    COLLECTION("Money collection", "COLLECTION", true, true, false, "PENDING"),
    CUSTOMER_ORDER("Customer / Job Order", "CUSTOMER_ORDER", false, false, true, null),
    DELIVERY("Delivery / Dispatch", "DELIVERY", false, false, true, null),
    BUDGET("Revise Budget", "BUDGET", true, false, false, null),
    TARGET("Revise Target", "TARGET", true, false, false, null),
    UPDATE("Post Update", "UPDATE", false, false, false, null),
    ISSUE("Report Issue", "ISSUE", false, false, false, null),
    STOCK("Stock / Material Update", "STOCK", false, false, false, null),
    ORDER("Work Order Update", "ORDER", false, false, false, null),
    SHIPMENT("Delivery Update", "SHIPMENT", false, false, false, null),
    RETURN("Return / Credit Note", "RETURN", true, false, false, null),
    REIMBURSE("Record Reimbursement", "PAYMENT", true, false, true, "PENDING"),
    PERSON("Add or invite person", "UPDATE", false, false, true, null),
    ROLE("Add or invite person", "UPDATE", false, false, true, null),
    INVITE("Invite & QR", "UPDATE", false, false, true, null),
    SUPPLIER("Add Vendor / Supplier", "UPDATE", false, false, true, null),
    HOLD("Hold / release work order", "ORDER", false, false, false, null),
    TRANSFER("Transfer stock / WIP", "STOCK", false, false, false, null),
    REPRINT("Reprint invoice / job card", "SALE", false, false, false, null),
}

@Composable
fun ManufacturingQuickAddHub(
    model: OperatingProjectionDto,
    onRecord: suspend (ManufacturingEventDraft) -> Unit,
    onDecide: suspend (String, String) -> Unit,
) {
    val allowed = model.quickAdds.map { it.code }.toSet()
    var sheet by remember { mutableStateOf<ManufacturingCapture?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var approvalOpen by remember { mutableStateOf(false) }
    val theme = remember(model.subtype) {
        com.example.momentra.ui.shell.business.shared.OperatingV12Theme.forSubtype(model.subtype)
    }
    val role = model.role.orEmpty()
    val approver = role == "APPROVER" || role == "OWNER_ADMIN"
    LaunchedEffect(OperatingChromeState.quickAddHint) {
        val hint = OperatingChromeState.quickAddHint?.uppercase() ?: return@LaunchedEffect
        sheet = ManufacturingCapture.entries.firstOrNull { it.captureMode == hint || it.name == hint }
            ?: when (hint.lowercase()) {
                "shipment", "delivery" -> ManufacturingCapture.DELIVERY
                "production" -> ManufacturingCapture.PRODUCTION
                "customer_order", "job" -> ManufacturingCapture.CUSTOMER_ORDER
                "stock" -> ManufacturingCapture.STOCK
                "budget" -> ManufacturingCapture.BUDGET
                "target" -> ManufacturingCapture.TARGET
                "sale" -> ManufacturingCapture.SALE
                "purchase" -> ManufacturingCapture.PURCHASE
                "collection" -> ManufacturingCapture.COLLECTION
                "payment" -> ManufacturingCapture.PAYMENT
                "spend", "expense" -> ManufacturingCapture.SPEND
                "order" -> ManufacturingCapture.ORDER
                "update", "issue" -> ManufacturingCapture.UPDATE
                else -> null
            }
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
            com.example.momentra.ui.shell.business.shared.OperatingV12HubScreen(
                model = model,
                theme = theme,
                onCapture = { code ->
                    sheet = ManufacturingCapture.entries.firstOrNull { it.captureMode == code || it.eventType == code }
                        ?: ManufacturingCapture.UPDATE
                },
                onOpenMasters = { tabName -> OperatingChromeState.mastersOpen = tabName },
            )
            if (error != null) {
                Text(error ?: "", color = Color(0xFFF87171), fontFamily = PlusJakartaSans, fontSize = 13.sp)
            }
            Text(
                "Review approvals",
                color = if (approver) MfgAccent else MfgSecondary,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clickable(enabled = approver) { approvalOpen = true }
                    .padding(vertical = 4.dp),
            )
        }
        val createMaster: suspend (String, String, String?, String?) -> Unit = { kind, name, unit, conversion ->
            onRecord(
                ManufacturingEventDraft(
                    eventType = "UPDATE",
                    title = name,
                    amount = null,
                    note = null,
                    channelName = null,
                    counterpartyName = null,
                    paymentStatus = null,
                    unit = unit,
                    itemName = name,
                    masterKind = kind,
                    conversionToBase = conversion,
                ),
            )
        }
        val open = sheet
        if (open != null) {
            OperatingSheetScrim(onDismiss = { sheet = null }, accent = MfgBrand) {
                OperatingCaptureSheet(
                    title = open.title,
                    icon = open.hubIcon,
                    eventType = open.eventType,
                    mode = open.captureMode,
                    model = model,
                    channelLabel = when (open.captureMode) {
                        "SALE" -> "Sales channel"
                        "COLLECTION" -> "Buyer / customer"
                        else -> "Channel"
                    },
                    // OperatingCaptureSheet gates plant/job UX on `standard` / `job`.
                    variant = if (theme.isMadeToOrder) "job" else "standard",
                    onDismiss = { sheet = null },
                    onSubmit = { draft ->
                        onRecord(draft.toManufacturing())
                        sheet = null
                    },
                    onDecide = onDecide,
                    onCreateMaster = if ("UPDATE" in allowed) createMaster else null,
                )
            }
        }
        if (approvalOpen) {
            OperatingSheetScrim(onDismiss = { approvalOpen = false }, accent = MfgBrand) {
                OperatingCaptureSheet(
                    title = "Review Approval",
                    icon = R.drawable.qa_shield,
                    eventType = "APPROVAL",
                    mode = "APPROVAL",
                    model = model,
                    variant = if (theme.isMadeToOrder) "job" else "standard",
                    onDismiss = { approvalOpen = false },
                    onSubmit = {},
                    onDecide = onDecide,
                )
            }
        }
    }
}

private val ManufacturingCapture.hubIcon: Int
    get() = when (this) {
        ManufacturingCapture.PRODUCTION, ManufacturingCapture.TARGET -> R.drawable.qa_clipboard
        ManufacturingCapture.SALE, ManufacturingCapture.SPEND, ManufacturingCapture.REPRINT -> R.drawable.qa_receipt
        ManufacturingCapture.PURCHASE -> R.drawable.qa_wallet
        ManufacturingCapture.PAYMENT, ManufacturingCapture.REIMBURSE -> R.drawable.qa_credit_card
        ManufacturingCapture.COLLECTION -> R.drawable.qa_arrow_left_right
        ManufacturingCapture.CUSTOMER_ORDER, ManufacturingCapture.ORDER -> R.drawable.qa_clipboard
        ManufacturingCapture.DELIVERY, ManufacturingCapture.SHIPMENT, ManufacturingCapture.TRANSFER -> R.drawable.qa_shipment
        ManufacturingCapture.BUDGET -> R.drawable.qa_budget
        ManufacturingCapture.UPDATE -> R.drawable.qa_message
        ManufacturingCapture.ISSUE -> R.drawable.qa_alert
        ManufacturingCapture.STOCK -> R.drawable.qa_package
        ManufacturingCapture.RETURN -> R.drawable.qa_arrow_left_right
        ManufacturingCapture.PERSON -> R.drawable.qa_user_plus
        ManufacturingCapture.ROLE -> R.drawable.qa_users
        ManufacturingCapture.INVITE -> R.drawable.qa_qr
        ManufacturingCapture.SUPPLIER -> R.drawable.qa_network
        ManufacturingCapture.HOLD -> R.drawable.qa_shield
    }

private val ManufacturingCapture.captureMode: String
    get() = when (this) {
        ManufacturingCapture.PRODUCTION -> "PRODUCTION"
        ManufacturingCapture.SALE -> "SALE"
        ManufacturingCapture.PURCHASE -> "PURCHASE"
        ManufacturingCapture.SPEND -> "SPEND"
        ManufacturingCapture.PAYMENT -> "PAYMENT"
        ManufacturingCapture.COLLECTION -> "COLLECTION"
        ManufacturingCapture.CUSTOMER_ORDER -> "CUSTOMER_ORDER"
        ManufacturingCapture.DELIVERY -> "DELIVERY"
        ManufacturingCapture.BUDGET -> "BUDGET"
        ManufacturingCapture.TARGET -> "TARGET"
        ManufacturingCapture.UPDATE -> "UPDATE"
        ManufacturingCapture.ISSUE -> "ISSUE"
        ManufacturingCapture.STOCK -> "STOCK"
        ManufacturingCapture.ORDER -> "ORDER"
        ManufacturingCapture.SHIPMENT -> "SHIPMENT"
        ManufacturingCapture.RETURN -> "RETURN"
        ManufacturingCapture.REIMBURSE -> "REIMBURSE"
        ManufacturingCapture.PERSON -> "PERSON"
        ManufacturingCapture.ROLE -> "ROLE"
        ManufacturingCapture.INVITE -> "INVITE"
        ManufacturingCapture.SUPPLIER -> "SUPPLIER"
        ManufacturingCapture.HOLD -> "HOLD"
        ManufacturingCapture.TRANSFER -> "TRANSFER"
        ManufacturingCapture.REPRINT -> "REPRINT"
    }

private fun OperatingCaptureDraft.toManufacturing() = ManufacturingEventDraft(
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
    conversionToBase = conversionToBase,
    dueAt = dueAt,
    contract = contract,
    masterId = masterId,
    masterGstApplicable = masterGstApplicable,
    masterGstClassification = masterGstClassification,
    masterGstRate = masterGstRate,
    masterPriceIncludesGst = masterPriceIncludesGst,
    masterSalePriceIncludesGst = masterSalePriceIncludesGst,
    masterPurchasePriceIncludesGst = masterPurchasePriceIncludesGst,
    masterGstEffectiveFrom = masterGstEffectiveFrom,
    masterHsn = masterHsn,
    masterGstRegistration = masterGstRegistration,
    masterBusinessState = masterBusinessState,
    masterBillingState = masterBillingState,
    masterDeliveryState = masterDeliveryState,
    masterGstin = masterGstin,
    lines = lines,
    rejectedQuantity = null,
)

package com.example.momentra.ui.shell.business.distribution

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

data class DistributionEventDraft(
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
)

private enum class DistributionSheet(
    val title: String,
    val eventType: String,
    val needsAmount: Boolean,
    val needsChannel: Boolean,
    val needsParty: Boolean,
    val paymentStatus: String?,
) {
    SALE("Record Sale", "SALE", true, true, false, null),
    PURCHASE("Record Purchase", "PURCHASE", true, false, true, null),
    SPEND("Record Expense", "SPEND", true, false, false, null),
    PAYMENT("Record Payment", "PAYMENT", true, false, true, "PENDING"),
    COLLECTION("Money collection", "COLLECTION", true, true, false, "PENDING"),
    BUDGET("Revise Budget", "BUDGET", true, false, false, null),
    UPDATE("Post Update", "UPDATE", false, false, false, null),
    ISSUE("Report Issue", "ISSUE", false, false, false, null),
    STOCK("Stock / Availability Update", "STOCK", false, false, false, null),
    ORDER("Order / Dispatch Update", "ORDER", false, false, false, null),
    SHIPMENT("Delivery Update", "SHIPMENT", false, false, false, null),
    RETURN("Return / Credit Note", "RETURN", true, false, false, null),
    REIMBURSE("Record Reimbursement", "PAYMENT", true, false, true, "PENDING"),
    PERSON("Add or invite person", "UPDATE", false, false, true, null),
    ROLE("Add or invite person", "UPDATE", false, false, true, null),
    INVITE("Add or invite person", "UPDATE", false, false, true, null),
    SUPPLIER("Add Principal / Supplier", "UPDATE", false, false, true, null),
    HOLD("Hold / release order", "ORDER", false, false, false, null),
    TRANSFER("Transfer stock", "STOCK", false, false, false, null),
    REPRINT("Reprint invoice / challan", "SALE", false, false, false, null),
}

@Composable
fun DistributionQuickAddHub(
    model: OperatingProjectionDto,
    onRecord: suspend (DistributionEventDraft) -> Unit,
    onDecide: suspend (String, String) -> Unit,
) {
    val allowed = model.quickAdds.map { it.code }.toSet()
    var sheet by remember { mutableStateOf<DistributionSheet?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var approvalOpen by remember { mutableStateOf(false) }
    val theme = remember { com.example.momentra.ui.shell.business.shared.OperatingV12Theme.distribution() }
    val role = model.role.orEmpty()
    val approver = role == "APPROVER" || role == "OWNER_ADMIN"
    LaunchedEffect(OperatingChromeState.quickAddHint) {
        val hint = OperatingChromeState.quickAddHint?.uppercase() ?: return@LaunchedEffect
        sheet = DistributionSheet.entries.firstOrNull { it.captureMode == hint || it.name == hint }
            ?: when (hint.lowercase()) {
                "shipment", "delivery" -> DistributionSheet.SHIPMENT
                "stock" -> DistributionSheet.STOCK
                "budget" -> DistributionSheet.BUDGET
                "sale" -> DistributionSheet.SALE
                "purchase" -> DistributionSheet.PURCHASE
                "collection" -> DistributionSheet.COLLECTION
                "payment" -> DistributionSheet.PAYMENT
                "spend", "expense" -> DistributionSheet.SPEND
                "order" -> DistributionSheet.ORDER
                "update", "issue" -> DistributionSheet.UPDATE
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
                    sheet = DistributionSheet.entries.firstOrNull { it.captureMode == code || it.eventType == code }
                        ?: DistributionSheet.UPDATE
                },
                onOpenMasters = { tabName -> OperatingChromeState.mastersOpen = tabName },
            )
            if (error != null) {
                Text(error ?: "", color = Color(0xFFF87171), fontFamily = PlusJakartaSans, fontSize = 13.sp)
            }
            Text(
                "Review approvals",
                color = if (approver) DistAccent else DistSecondary,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clickable(enabled = approver) { approvalOpen = true }
                    .padding(vertical = 4.dp),
            )
        }
        val createMaster: suspend (String, String, String?, String?) -> Unit = { kind, name, unit, conversion ->
            onRecord(
                DistributionEventDraft(
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
            OperatingSheetScrim(onDismiss = { sheet = null }, accent = DistBrand) {
                OperatingCaptureSheet(
                    title = open.title,
                    icon = open.hubIcon,
                    eventType = open.eventType,
                    mode = open.captureMode,
                    model = model,
                    channelLabel = when (open.captureMode) {
                        "SALE" -> "Sales channel"
                        "COLLECTION" -> "Buyer / dealer"
                        else -> "Channel"
                    },
                    variant = "distribution",
                    onDismiss = { sheet = null },
                    onSubmit = { draft ->
                        onRecord(draft.toDistribution())
                        sheet = null
                    },
                    onDecide = onDecide,
                    onCreateMaster = if ("UPDATE" in allowed) createMaster else null,
                )
            }
        }
        if (approvalOpen) {
            OperatingSheetScrim(onDismiss = { approvalOpen = false }, accent = DistBrand) {
                OperatingCaptureSheet(
                    title = "Review Approval",
                    icon = R.drawable.qa_shield,
                    eventType = "APPROVAL",
                    mode = "APPROVAL",
                    model = model,
                    variant = "distribution",
                    onDismiss = { approvalOpen = false },
                    onSubmit = {},
                    onDecide = onDecide,
                )
            }
        }
    }
}

private val DistributionSheet.hubIcon: Int
    get() = when (this) {
        DistributionSheet.SALE, DistributionSheet.SPEND -> R.drawable.qa_receipt
        DistributionSheet.PURCHASE -> R.drawable.qa_wallet
        DistributionSheet.PAYMENT, DistributionSheet.REIMBURSE -> R.drawable.qa_credit_card
        DistributionSheet.COLLECTION -> R.drawable.qa_arrow_left_right
        DistributionSheet.BUDGET -> R.drawable.qa_budget
        DistributionSheet.UPDATE -> R.drawable.qa_message
        DistributionSheet.ISSUE -> R.drawable.qa_alert
        DistributionSheet.STOCK -> R.drawable.qa_package
        DistributionSheet.ORDER -> R.drawable.qa_truck
        DistributionSheet.SHIPMENT -> R.drawable.qa_package
        DistributionSheet.RETURN -> R.drawable.qa_arrow_left_right
        DistributionSheet.PERSON -> R.drawable.qa_user_plus
        DistributionSheet.ROLE -> R.drawable.qa_users
        DistributionSheet.INVITE -> R.drawable.qa_qr
        DistributionSheet.SUPPLIER -> R.drawable.qa_network
        DistributionSheet.HOLD -> R.drawable.qa_shield
        DistributionSheet.TRANSFER -> R.drawable.qa_truck
        DistributionSheet.REPRINT -> R.drawable.qa_receipt
    }

private val DistributionSheet.captureMode: String
    get() = when (this) {
        DistributionSheet.SALE -> "SALE"
        DistributionSheet.PURCHASE -> "PURCHASE"
        DistributionSheet.SPEND -> "SPEND"
        DistributionSheet.PAYMENT -> "PAYMENT"
        DistributionSheet.COLLECTION -> "COLLECTION"
        DistributionSheet.BUDGET -> "BUDGET"
        DistributionSheet.UPDATE -> "UPDATE"
        DistributionSheet.ISSUE -> "ISSUE"
        DistributionSheet.STOCK -> "STOCK"
        DistributionSheet.ORDER -> "ORDER"
        DistributionSheet.SHIPMENT -> "SHIPMENT"
        DistributionSheet.RETURN -> "RETURN"
        DistributionSheet.REIMBURSE -> "REIMBURSE"
        DistributionSheet.PERSON -> "PERSON"
        DistributionSheet.ROLE -> "ROLE"
        DistributionSheet.INVITE -> "INVITE"
        DistributionSheet.SUPPLIER -> "SUPPLIER"
        DistributionSheet.HOLD -> "HOLD"
        DistributionSheet.TRANSFER -> "TRANSFER"
        DistributionSheet.REPRINT -> "REPRINT"
    }

private fun OperatingCaptureDraft.toDistribution() = DistributionEventDraft(
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
)

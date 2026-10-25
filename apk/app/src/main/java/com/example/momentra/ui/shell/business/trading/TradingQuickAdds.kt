package com.example.momentra.ui.shell.business.trading

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.R
import com.example.momentra.data.api.OperatingProjectionDto
import androidx.compose.runtime.LaunchedEffect
import com.example.momentra.ui.shell.business.shared.OperatingCaptureDraft
import com.example.momentra.ui.shell.business.shared.OperatingCaptureSheet
import com.example.momentra.ui.shell.business.shared.OperatingChromeState
import com.example.momentra.ui.shell.business.shared.OperatingMomentCatalog
import com.example.momentra.ui.shell.business.shared.OperatingSheetScrim
import com.example.momentra.ui.theme.PlusJakartaSans
import kotlinx.coroutines.launch

data class TradingEventDraft(
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
)

private enum class TradingSheet(
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
    COLLECTION("Record Collection", "COLLECTION", true, true, false, "PENDING"),
    BUDGET("Revise Budget", "BUDGET", true, false, false, null),
    UPDATE("Post Update", "UPDATE", false, false, false, null),
    ISSUE("Report Issue", "ISSUE", false, false, false, null),
    STOCK("Stock / Availability Update", "STOCK", false, false, false, null),
    ORDER("Order / Dispatch Update", "ORDER", false, false, false, null),
    SHIPMENT("Shipment / Delivery Update", "SHIPMENT", false, false, false, null),
    RETURN("Return / Credit Note", "RETURN", true, false, false, null),
    REIMBURSE("Record Reimbursement", "PAYMENT", true, false, true, "PENDING"),
    PERSON("Add or invite person", "UPDATE", false, false, true, null),
    ROLE("Add or invite person", "UPDATE", false, false, true, null),
    INVITE("Add or invite person", "UPDATE", false, false, true, null),
    SUPPLIER("Add Supplier", "UPDATE", false, false, true, null),
}

@Composable
fun TradingQuickAddHub(
    model: OperatingProjectionDto,
    onRecord: suspend (TradingEventDraft) -> Unit,
    onDecide: suspend (String, String) -> Unit,
) {
    val allowed = model.quickAdds.map { it.code }.toSet()
    var query by remember { mutableStateOf("") }
    var sheet by remember { mutableStateOf<TradingSheet?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var approvalOpen by remember { mutableStateOf(false) }
    var mastersOpen by remember { mutableStateOf(false) }
    val role = model.role.orEmpty()
    val owner = role == "OWNER_ADMIN"
    val approver = role == "APPROVER" || owner
    LaunchedEffect(Unit) {
        when (OperatingChromeState.quickAddHint) {
            "shipment" -> sheet = TradingSheet.SHIPMENT
            "stock" -> sheet = TradingSheet.STOCK
            "budget" -> sheet = TradingSheet.BUDGET
        }
        OperatingChromeState.quickAddHint = null
    }
    Box(Modifier.fillMaxSize()) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 17.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
    Text("Quick Adds", color = TradeInk, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 28.sp)
    Text("Record what happened · ${model.title.orEmpty()}", color = TradeSecondary, fontFamily = PlusJakartaSans, fontSize = 13.sp)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        HubChip(OperatingMomentCatalog.roles.firstOrNull { it.first == role }?.second ?: "Trading", filled = true)
        HubChip("Trading", filled = false)
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(TradeNotice)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Source. Sell. Collect.", color = TradeInk, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
        Text("Capture a sale, a cost, or a note while it is still clear.", color = TradeSecondary, fontFamily = PlusJakartaSans, fontSize = 13.sp)
        if ("SALE" in allowed) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(TradeBrand)
                    .clickable { sheet = TradingSheet.SALE }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(painterResource(R.drawable.qa_plus), contentDescription = null, tint = TradeBg, modifier = Modifier.size(14.dp))
                Text("Record Sale", color = TradeBg, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold)
            }
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(TradeField)
            .border(1.dp, TradeBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(painterResource(R.drawable.qa_search), contentDescription = null, tint = TradeSecondary, modifier = Modifier.size(18.dp))
        BasicTextField(
            value = query,
            onValueChange = { query = it },
            textStyle = TextStyle(color = TradeInk, fontFamily = PlusJakartaSans, fontSize = 14.sp),
            cursorBrush = SolidColor(TradeBrand),
            decorationBox = { inner ->
                if (query.isEmpty()) Text("Search actions", color = TradeSecondary, fontFamily = PlusJakartaSans, fontSize = 14.sp)
                inner()
            },
            modifier = Modifier.weight(1f),
        )
    }
    val core = listOf(
        TradingSheet.SALE to "Sale",
        TradingSheet.PURCHASE to "Purchase",
        TradingSheet.SPEND to "Expense",
        TradingSheet.PAYMENT to "Payment",
        TradingSheet.COLLECTION to "Collection",
        TradingSheet.BUDGET to "Budget",
        TradingSheet.UPDATE to "Update",
        TradingSheet.ISSUE to "Issue",
        TradingSheet.STOCK to "Stock",
        TradingSheet.ORDER to "Order",
        TradingSheet.SHIPMENT to "Shipment",
        TradingSheet.RETURN to "Return",
    ).filter { (kind, label) ->
        kind.eventType in allowed && label.contains(query, ignoreCase = true)
    }
    Text("Core actions · ${core.size}", color = TradeSecondary, fontFamily = PlusJakartaSans, fontSize = 12.sp)
    core.chunked(3).forEach { row ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            row.forEach { (kind, label) ->
                ActionCard(label, kind.hubIcon, locked = null, modifier = Modifier.weight(1f)) { sheet = kind }
            }
        }
    }
    Text(
        "Masters",
        color = Color.White,
        fontFamily = PlusJakartaSans,
        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
        modifier = Modifier.clickable { mastersOpen = true }.padding(vertical = 8.dp),
    )
    Text("Support & management", color = TradeSecondary, fontFamily = PlusJakartaSans, fontSize = 12.sp)
    val support = listOf(
        Triple(TradingSheet.REIMBURSE, "Reimbursement", if ("PAYMENT" in allowed) null else "Finance only"),
        Triple(null, "Approval review", if (approver) null else "Approver only"),
        Triple(TradingSheet.PERSON, "Add person", if (owner && "UPDATE" in allowed) null else "Owner only"),
        Triple(TradingSheet.ROLE, "Role & access", if (owner && "UPDATE" in allowed) null else "Owner only"),
        Triple(TradingSheet.SUPPLIER, "Suppliers & channels", if ("UPDATE" in allowed) null else "View only"),
        Triple(TradingSheet.INVITE, "Invite & QR", if (owner && "UPDATE" in allowed) null else "Owner only"),
    ).filter { (_, label, _) -> label.contains(query, ignoreCase = true) }
    support.chunked(3).forEach { row ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            row.forEach { (kind, label, lock) ->
                ActionCard(label, if (kind == null) R.drawable.qa_shield else kind.hubIcon, lock, Modifier.weight(1f)) {
                    if (label == "Approval review" && approver) approvalOpen = true
                    else if (kind != null && lock == null) sheet = kind
                }
            }
        }
    }
    Text(
        "Review approvals",
        color = if (approver) TradeAccent else TradeSecondary,
        fontFamily = PlusJakartaSans,
        modifier = Modifier.clickable(enabled = approver) { approvalOpen = true },
    )
    if (!owner) {
        Text("Some support actions stay with the Owner or Approver.", color = TradeSecondary, fontFamily = PlusJakartaSans, fontSize = 12.sp)
    }
    if (error != null) Text(error ?: "", color = Color(0xFFF87171), fontFamily = PlusJakartaSans, fontSize = 13.sp)
    }
    val createMaster: suspend (String, String, String?, String?) -> Unit = { kind, name, unit, conversion ->
        onRecord(
            TradingEventDraft(
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
        OperatingSheetScrim(onDismiss = { sheet = null }) {
            OperatingCaptureSheet(
                title = open.title,
                icon = open.hubIcon,
                eventType = open.eventType,
                mode = open.captureMode,
                model = model,
                channelLabel = when (open.captureMode) {
                    "SALE" -> "Sales channel"
                    "COLLECTION" -> "Received from"
                    else -> "Channel"
                },
                variant = "trading",
                onDismiss = { sheet = null },
                onSubmit = { draft ->
                    onRecord(draft.toTrading())
                    sheet = null
                },
                onDecide = onDecide,
                onCreateMaster = if ("UPDATE" in allowed) createMaster else null,
            )
        }
    }
    if (mastersOpen) {
        OperatingSheetScrim(onDismiss = { mastersOpen = false }) {
            OperatingCaptureSheet(
                title = "Masters",
                icon = R.drawable.qa_shield,
                eventType = "UPDATE",
                mode = "MASTERS",
                model = model,
                onDismiss = { mastersOpen = false },
                onSubmit = { draft ->
                    onRecord(draft.toTrading())
                    mastersOpen = false
                },
            )
        }
    }
    if (approvalOpen) {
        OperatingSheetScrim(onDismiss = { approvalOpen = false }) {
            OperatingCaptureSheet(
                title = "Review Approval",
                icon = R.drawable.qa_shield,
                eventType = "APPROVAL",
                mode = "APPROVAL",
                model = model,
                variant = "trading",
                onDismiss = { approvalOpen = false },
                onSubmit = {},
                onDecide = onDecide,
            )
        }
    }
    }
}



private val TradingSheet.hubIcon: Int
    get() = when (this) {
        TradingSheet.SALE, TradingSheet.SPEND -> R.drawable.qa_receipt
        TradingSheet.PURCHASE -> R.drawable.qa_wallet
        TradingSheet.PAYMENT, TradingSheet.REIMBURSE -> R.drawable.qa_credit_card
        TradingSheet.COLLECTION -> R.drawable.qa_arrow_left_right
        TradingSheet.BUDGET -> R.drawable.qa_budget
        TradingSheet.UPDATE -> R.drawable.qa_message
        TradingSheet.ISSUE -> R.drawable.qa_alert
        TradingSheet.STOCK -> R.drawable.qa_package
        TradingSheet.ORDER -> R.drawable.qa_clipboard
        TradingSheet.SHIPMENT -> R.drawable.qa_shipment
        TradingSheet.RETURN -> R.drawable.qa_arrow_left_right
        TradingSheet.PERSON -> R.drawable.qa_user_plus
        TradingSheet.ROLE -> R.drawable.qa_users
        TradingSheet.INVITE -> R.drawable.qa_qr
        TradingSheet.SUPPLIER -> R.drawable.qa_network
    }

@Composable
private fun ActionCard(label: String, icon: Int, locked: String?, modifier: Modifier, onClick: () -> Unit) {
    val ink = if (locked == null) TradeInk else TradeSecondary
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(TradeCard)
            .border(1.dp, TradeBorder, RoundedCornerShape(14.dp))
            .clickable(enabled = locked == null, onClick = onClick)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = ink, modifier = Modifier.size(28.dp))
        Text(label, color = ink, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, textAlign = TextAlign.Center)
        if (locked != null) Text(locked, color = TradeSecondary, fontFamily = PlusJakartaSans, fontSize = 11.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun HubChip(text: String, filled: Boolean) {
    Text(
        text,
        color = if (filled) TradeBg else TradeSecondary,
        fontFamily = PlusJakartaSans,
        fontSize = 12.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (filled) TradeBrand else Color.Transparent)
            .border(1.dp, if (filled) TradeBrand else TradeBorder, RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}


private val TradingSheet.captureMode: String
    get() = when (this) {
        TradingSheet.SALE -> "SALE"
        TradingSheet.PURCHASE -> "PURCHASE"
        TradingSheet.SPEND -> "SPEND"
        TradingSheet.PAYMENT -> "PAYMENT"
        TradingSheet.COLLECTION -> "COLLECTION"
        TradingSheet.BUDGET -> "BUDGET"
        TradingSheet.UPDATE -> "UPDATE"
        TradingSheet.ISSUE -> "ISSUE"
        TradingSheet.STOCK -> "STOCK"
        TradingSheet.ORDER -> "ORDER"
        TradingSheet.SHIPMENT -> "SHIPMENT"
        TradingSheet.RETURN -> "RETURN"
        TradingSheet.REIMBURSE -> "REIMBURSE"
        TradingSheet.PERSON -> "PERSON"
        TradingSheet.ROLE -> "ROLE"
        TradingSheet.INVITE -> "INVITE"
        TradingSheet.SUPPLIER -> "SUPPLIER"
    }

private fun OperatingCaptureDraft.toTrading() = TradingEventDraft(
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
)

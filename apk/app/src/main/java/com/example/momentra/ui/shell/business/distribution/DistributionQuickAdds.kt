package com.example.momentra.ui.shell.business.distribution

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
import com.example.momentra.ui.shell.business.shared.OperatingCaptureDraft
import com.example.momentra.ui.shell.business.shared.OperatingCaptureSheet
import com.example.momentra.ui.shell.business.shared.OperatingMomentCatalog
import com.example.momentra.ui.shell.business.shared.OperatingSheetScrim
import com.example.momentra.ui.theme.PlusJakartaSans
import kotlinx.coroutines.launch

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
    val dueAt: String? = null,
    val contract: com.example.momentra.data.api.OperatingContractBody? = null,
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
    COLLECTION("Record Collection", "COLLECTION", true, true, false, "PENDING"),
    BUDGET("Revise Budget", "BUDGET", true, false, false, null),
    UPDATE("Post Update", "UPDATE", false, false, false, null),
    ISSUE("Report Issue", "ISSUE", false, false, false, null),
    STOCK("Stock / Availability Update", "STOCK", false, false, false, null),
    ORDER("Order / Dispatch Update", "ORDER", false, false, false, null),
    SHIPMENT("Delivery Update", "SHIPMENT", false, false, false, null),
    REIMBURSE("Record Reimbursement", "PAYMENT", true, false, true, "PENDING"),
    PERSON("Add or invite person", "UPDATE", false, false, true, null),
    ROLE("Add or invite person", "UPDATE", false, false, true, null),
    INVITE("Add or invite person", "UPDATE", false, false, true, null),
    SUPPLIER("Add Principal / Supplier", "UPDATE", false, false, true, null),
}

@Composable
fun DistributionQuickAddHub(
    model: OperatingProjectionDto,
    onRecord: suspend (DistributionEventDraft) -> Unit,
    onDecide: suspend (String, String) -> Unit,
) {
    val allowed = model.quickAdds.map { it.code }.toSet()
    var query by remember { mutableStateOf("") }
    var sheet by remember { mutableStateOf<DistributionSheet?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var approvalOpen by remember { mutableStateOf(false) }
    var mastersOpen by remember { mutableStateOf(false) }
    val role = model.role.orEmpty()
    val owner = role == "OWNER_ADMIN"
    val approver = role == "APPROVER" || owner
    Box(Modifier.fillMaxSize()) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 17.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
    Text("Quick Adds", color = DistInk, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 28.sp)
    Text("Record what happened · ${model.title.orEmpty()}", color = DistSecondary, fontFamily = PlusJakartaSans, fontSize = 13.sp)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        HubChip(OperatingMomentCatalog.roles.firstOrNull { it.first == role }?.second ?: "Distribution", filled = true)
        HubChip("Distribution", filled = false)
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(DistNotice)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Move. Sell. Collect.", color = DistInk, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
        Text("Capture a sale, a cost, or a note while it is still clear.", color = DistSecondary, fontFamily = PlusJakartaSans, fontSize = 13.sp)
        if ("SALE" in allowed) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(DistBrand)
                    .clickable { sheet = DistributionSheet.SALE }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(painterResource(R.drawable.qa_plus), contentDescription = null, tint = DistBg, modifier = Modifier.size(14.dp))
                Text("Record Sale", color = DistBg, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold)
            }
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DistField)
            .border(1.dp, DistBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(painterResource(R.drawable.qa_search), contentDescription = null, tint = DistSecondary, modifier = Modifier.size(18.dp))
        BasicTextField(
            value = query,
            onValueChange = { query = it },
            textStyle = TextStyle(color = DistInk, fontFamily = PlusJakartaSans, fontSize = 14.sp),
            cursorBrush = SolidColor(DistBrand),
            decorationBox = { inner ->
                if (query.isEmpty()) Text("Search actions", color = DistSecondary, fontFamily = PlusJakartaSans, fontSize = 14.sp)
                inner()
            },
            modifier = Modifier.weight(1f),
        )
    }
    val core = listOf(
        DistributionSheet.SALE to "Sale",
        DistributionSheet.PURCHASE to "Purchase",
        DistributionSheet.SPEND to "Expense",
        DistributionSheet.PAYMENT to "Payment",
        DistributionSheet.COLLECTION to "Collection",
        DistributionSheet.UPDATE to "Update",
        DistributionSheet.ISSUE to "Issue",
        DistributionSheet.STOCK to "Stock",
        DistributionSheet.ORDER to "Order",
        DistributionSheet.SHIPMENT to "Shipment",
    ).filter { (kind, label) ->
        kind.eventType in allowed && label.contains(query, ignoreCase = true)
    }
    Text("Core actions · ${core.size}", color = DistSecondary, fontFamily = PlusJakartaSans, fontSize = 12.sp)
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
    Text("Support & management", color = DistSecondary, fontFamily = PlusJakartaSans, fontSize = 12.sp)
    val support = listOf(
        Triple(DistributionSheet.REIMBURSE, "Reimbursement", if ("PAYMENT" in allowed) null else "Finance only"),
        Triple(null, "Approval review", if (approver) null else "Approver only"),
        Triple(DistributionSheet.PERSON, "Add person", if (owner && "UPDATE" in allowed) null else "Owner only"),
        Triple(DistributionSheet.ROLE, "Role & access", if (owner && "UPDATE" in allowed) null else "Owner only"),
        Triple(DistributionSheet.SUPPLIER, "Suppliers & channels", if ("UPDATE" in allowed) null else "View only"),
        Triple(DistributionSheet.INVITE, "Invite & QR", if (owner && "UPDATE" in allowed) null else "Owner only"),
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
        color = if (approver) DistAccent else DistSecondary,
        fontFamily = PlusJakartaSans,
        modifier = Modifier.clickable(enabled = approver) { approvalOpen = true },
    )
    if (!owner) {
        Text("Some support actions stay with the Owner or Approver.", color = DistSecondary, fontFamily = PlusJakartaSans, fontSize = 12.sp)
    }
    if (error != null) Text(error ?: "", color = Color(0xFFF87171), fontFamily = PlusJakartaSans, fontSize = 13.sp)
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
            )
        }
    }
    if (mastersOpen) {
        OperatingCaptureSheet(
            title = "Masters",
            icon = R.drawable.qa_shield,
            eventType = "UPDATE",
            mode = "MASTERS",
            model = model,
            onDismiss = { mastersOpen = false },
            onSubmit = { draft ->
                onRecord(draft.toDistribution())
                mastersOpen = false
            },
        )
    }
    if (approvalOpen) {
        OperatingSheetScrim(onDismiss = { approvalOpen = false }) {
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
        DistributionSheet.PERSON -> R.drawable.qa_user_plus
        DistributionSheet.ROLE -> R.drawable.qa_users
        DistributionSheet.INVITE -> R.drawable.qa_qr
        DistributionSheet.SUPPLIER -> R.drawable.qa_network
    }

@Composable
private fun ActionCard(label: String, icon: Int, locked: String?, modifier: Modifier, onClick: () -> Unit) {
    val ink = if (locked == null) DistInk else DistSecondary
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(DistCard)
            .border(1.dp, DistBorder, RoundedCornerShape(14.dp))
            .clickable(enabled = locked == null, onClick = onClick)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = ink, modifier = Modifier.size(28.dp))
        Text(label, color = ink, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, textAlign = TextAlign.Center)
        if (locked != null) Text(locked, color = DistSecondary, fontFamily = PlusJakartaSans, fontSize = 11.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun HubChip(text: String, filled: Boolean) {
    Text(
        text,
        color = if (filled) DistBg else DistSecondary,
        fontFamily = PlusJakartaSans,
        fontSize = 12.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (filled) DistBrand else Color.Transparent)
            .border(1.dp, if (filled) DistBrand else DistBorder, RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    )
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
        DistributionSheet.REIMBURSE -> "REIMBURSE"
        DistributionSheet.PERSON -> "PERSON"
        DistributionSheet.ROLE -> "ROLE"
        DistributionSheet.INVITE -> "INVITE"
        DistributionSheet.SUPPLIER -> "SUPPLIER"
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
    dueAt = dueAt,
    contract = contract,
)

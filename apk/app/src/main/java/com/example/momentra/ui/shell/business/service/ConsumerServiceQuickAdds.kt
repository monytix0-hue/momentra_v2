package com.example.momentra.ui.shell.business.service

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

data class ServiceEventDraft(
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

private enum class ServiceSheet(
    val title: String,
    val eventType: String,
    val needsAmount: Boolean,
    val needsChannel: Boolean,
    val needsParty: Boolean,
    val paymentStatus: String?,
) {
    SERVICE("Finalize Charge", "SERVICE", true, true, false, null),
    APPOINTMENT("Booking", "APPOINTMENT", false, false, false, null),
    SALE("Record Sale", "SALE", true, true, false, null),
    PURCHASE("Record Purchase", "PURCHASE", true, false, true, null),
    SPEND("Record Expense", "SPEND", true, false, false, null),
    PAYMENT("Record Payment", "PAYMENT", true, false, true, "PENDING"),
    COLLECTION("Record Collection", "COLLECTION", true, true, false, "PENDING"),
    BUDGET("Revise Budget", "BUDGET", true, false, false, null),
    UPDATE("Issue / Approval / Update", "UPDATE", false, false, false, null),
    ISSUE("Report Issue", "ISSUE", false, false, false, null),
    STOCK("Stock Update", "STOCK", false, false, false, null),
    STATUS("Status / Assignment", "APPOINTMENT", false, false, false, null),
    REVISION("Budget / Target Revision", "BUDGET", true, false, false, null),
    REIMBURSE("Record Reimbursement", "PAYMENT", true, false, true, "PENDING"),
    PERSON("Add or invite person", "UPDATE", false, false, true, null),
    ROLE("Add or invite person", "UPDATE", false, false, true, null),
    INVITE("Add or invite person", "UPDATE", false, false, true, null),
    SUPPLIER("Add Supplier", "UPDATE", false, false, true, null),
}

@Composable
fun ServiceQuickAddHub(
    model: OperatingProjectionDto,
    onRecord: suspend (ServiceEventDraft) -> Unit,
    onDecide: suspend (String, String) -> Unit,
) {
    val allowed = model.quickAdds.map { it.code }.toSet()
    var query by remember { mutableStateOf("") }
    var sheet by remember { mutableStateOf<ServiceSheet?>(null) }
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
    Text("Quick Adds", color = SvcInk, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 28.sp)
    Text("Record what happened · ${model.title.orEmpty()}", color = SvcSecondary, fontFamily = PlusJakartaSans, fontSize = 13.sp)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        HubChip(OperatingMomentCatalog.roles.firstOrNull { it.first == role }?.second ?: "Service", filled = true)
        HubChip("Service", filled = false)
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(SvcNotice)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Every service moment counts.", color = SvcInk, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
        Text("Capture a service, a cost, or a note while it is still clear.", color = SvcSecondary, fontFamily = PlusJakartaSans, fontSize = 13.sp)
        if ("SERVICE" in allowed) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(SvcBrand)
                    .clickable { sheet = ServiceSheet.SERVICE }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(painterResource(R.drawable.qa_plus), contentDescription = null, tint = SvcBg, modifier = Modifier.size(14.dp))
                Text("Record Service", color = SvcBg, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold)
            }
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SvcField)
            .border(1.dp, SvcBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(painterResource(R.drawable.qa_search), contentDescription = null, tint = SvcSecondary, modifier = Modifier.size(18.dp))
        BasicTextField(
            value = query,
            onValueChange = { query = it },
            textStyle = TextStyle(color = SvcInk, fontFamily = PlusJakartaSans, fontSize = 14.sp),
            cursorBrush = SolidColor(SvcBrand),
            decorationBox = { inner ->
                if (query.isEmpty()) Text("Search actions", color = SvcSecondary, fontFamily = PlusJakartaSans, fontSize = 14.sp)
                inner()
            },
            modifier = Modifier.weight(1f),
        )
    }
    val core = listOf(
        ServiceSheet.SERVICE to "Finalize Charge",
        ServiceSheet.PURCHASE to "Purchase",
        ServiceSheet.COLLECTION to "Collection",
        ServiceSheet.PAYMENT to "Payment",
        ServiceSheet.SPEND to "Expense",
        ServiceSheet.STOCK to "Stock",
        ServiceSheet.APPOINTMENT to "Booking",
        ServiceSheet.STATUS to "Status",
        ServiceSheet.REVISION to "Budget / Target",
        ServiceSheet.UPDATE to "Issue / Update",
    ).filter { (kind, label) ->
        kind.eventType in allowed && label.contains(query, ignoreCase = true)
    }
    Text("Core actions · ${core.size}", color = SvcSecondary, fontFamily = PlusJakartaSans, fontSize = 12.sp)
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
    Text("Support & management", color = SvcSecondary, fontFamily = PlusJakartaSans, fontSize = 12.sp)
    val support = listOf(
        Triple(ServiceSheet.REIMBURSE, "Reimbursement", if ("PAYMENT" in allowed) null else "Finance only"),
        Triple(null, "Approval review", if (approver) null else "Approver only"),
        Triple(ServiceSheet.PERSON, "Add person", if (owner && "UPDATE" in allowed) null else "Owner only"),
        Triple(ServiceSheet.ROLE, "Role & access", if (owner && "UPDATE" in allowed) null else "Owner only"),
        Triple(ServiceSheet.SUPPLIER, "Suppliers & channels", if ("UPDATE" in allowed) null else "View only"),
        Triple(ServiceSheet.INVITE, "Invite & QR", if (owner && "UPDATE" in allowed) null else "Owner only"),
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
        color = if (approver) SvcAccent else SvcSecondary,
        fontFamily = PlusJakartaSans,
        modifier = Modifier.clickable(enabled = approver) { approvalOpen = true },
    )
    if (!owner) {
        Text("Some support actions stay with the Owner or Approver.", color = SvcSecondary, fontFamily = PlusJakartaSans, fontSize = 12.sp)
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
                    "SERVICE", "APPOINTMENT" -> "Service channel"
                    "COLLECTION" -> "Received from"
                    else -> "Channel"
                },
                variant = "service",
                onDismiss = { sheet = null },
                onSubmit = { draft ->
                    onRecord(draft.toService())
                    sheet = null
                },
                onDecide = onDecide,
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
                    onRecord(draft.toService())
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
                variant = "service",
                onDismiss = { approvalOpen = false },
                onSubmit = {},
                onDecide = onDecide,
            )
        }
    }
    }
}



private val ServiceSheet.hubIcon: Int
    get() = when (this) {
        ServiceSheet.SERVICE -> R.drawable.rs_setup_service
        ServiceSheet.APPOINTMENT -> R.drawable.qa_calendar
        ServiceSheet.SALE, ServiceSheet.SPEND -> R.drawable.qa_receipt
        ServiceSheet.PAYMENT -> R.drawable.qa_credit_card
        ServiceSheet.COLLECTION -> R.drawable.qa_arrow_left_right
        ServiceSheet.BUDGET, ServiceSheet.REVISION -> R.drawable.qa_budget
        ServiceSheet.UPDATE -> R.drawable.qa_message
        ServiceSheet.ISSUE -> R.drawable.qa_alert
        ServiceSheet.PURCHASE -> R.drawable.qa_wallet
        ServiceSheet.STOCK, ServiceSheet.STATUS -> R.drawable.qa_package
        ServiceSheet.REIMBURSE -> R.drawable.qa_wallet
        ServiceSheet.PERSON -> R.drawable.qa_user_plus
        ServiceSheet.ROLE -> R.drawable.qa_users
        ServiceSheet.INVITE -> R.drawable.qa_qr
        ServiceSheet.SUPPLIER -> R.drawable.qa_network
    }

@Composable
private fun ActionCard(label: String, icon: Int, locked: String?, modifier: Modifier, onClick: () -> Unit) {
    val ink = if (locked == null) SvcInk else SvcSecondary
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(SvcCard)
            .border(1.dp, SvcBorder, RoundedCornerShape(14.dp))
            .clickable(enabled = locked == null, onClick = onClick)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = ink, modifier = Modifier.size(28.dp))
        Text(label, color = ink, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, textAlign = TextAlign.Center)
        if (locked != null) Text(locked, color = SvcSecondary, fontFamily = PlusJakartaSans, fontSize = 11.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun HubChip(text: String, filled: Boolean) {
    Text(
        text,
        color = if (filled) SvcBg else SvcSecondary,
        fontFamily = PlusJakartaSans,
        fontSize = 12.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (filled) SvcBrand else Color.Transparent)
            .border(1.dp, if (filled) SvcBrand else SvcBorder, RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}


private val ServiceSheet.captureMode: String
    get() = when (this) {
        ServiceSheet.SERVICE -> "SERVICE"
        ServiceSheet.APPOINTMENT -> "APPOINTMENT"
        ServiceSheet.SALE -> "SALE"
        ServiceSheet.SPEND -> "SPEND"
        ServiceSheet.PAYMENT -> "PAYMENT"
        ServiceSheet.COLLECTION -> "COLLECTION"
        ServiceSheet.BUDGET, ServiceSheet.REVISION -> "BUDGET"
        ServiceSheet.UPDATE -> "UPDATE"
        ServiceSheet.ISSUE -> "ISSUE"
        ServiceSheet.PURCHASE -> "PURCHASE"
        ServiceSheet.STOCK -> "STOCK"
        ServiceSheet.STATUS -> "APPOINTMENT"
        ServiceSheet.REIMBURSE -> "REIMBURSE"
        ServiceSheet.PERSON -> "PERSON"
        ServiceSheet.ROLE -> "ROLE"
        ServiceSheet.INVITE -> "INVITE"
        ServiceSheet.SUPPLIER -> "SUPPLIER"
    }

private fun OperatingCaptureDraft.toService() = ServiceEventDraft(
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

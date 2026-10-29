package com.example.momentra.ui.shell.business.production

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

data class ProductionEventDraft(
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
    val rejectedQuantity: String? = null,
    val lines: List<com.example.momentra.data.api.OperatingLineBody>? = null,
)

private enum class ProductionSheet(
    val title: String,
    val eventType: String,
    val needsAmount: Boolean,
    val needsChannel: Boolean,
    val needsParty: Boolean,
    val paymentStatus: String?,
) {
    PRODUCTION("Record Production", "PRODUCTION", true, false, false, null),
    PURCHASE("Material Purchase", "PURCHASE", true, false, true, null),
    SPEND("Expense", "SPEND", true, false, false, null),
    SALE("Sale / Dispatch", "SALE", true, false, true, null),
    CUSTOMER_ORDER("Customer Order", "CUSTOMER_ORDER", false, false, true, null),
    DELIVERY("Delivery", "DELIVERY", false, false, true, null),
    PAYMENT("Payment", "PAYMENT", true, false, true, "PENDING"),
    COLLECTION("Collection", "COLLECTION", true, false, false, "PENDING"),
    BUDGET("Budget / Target", "BUDGET", true, false, false, null),
    TARGET("Target", "TARGET", true, false, false, null),
    UPDATE("Update", "UPDATE", false, false, false, null),
    ISSUE("Issue", "ISSUE", false, false, false, null),
    STOCK("Material / Stock", "STOCK", false, false, false, null),
    REIMBURSE("Reimbursement", "SPEND", true, false, false, null),
    APPROVAL("Approval review", "APPROVAL", false, false, false, null),
    PERSON("Add person", "UPDATE", false, false, true, null),
    ROLE("Role & access", "UPDATE", false, false, true, null),
    PARTNER("Suppliers", "UPDATE", false, false, true, null),
    INVITE("Invite & QR", "UPDATE", false, false, false, null),
}

@Composable
fun ProductionQuickAddHub(
    model: OperatingProjectionDto,
    onRecord: suspend (ProductionEventDraft) -> Unit,
    onDecide: suspend (String, String) -> Unit,
) {
    val allowed = model.quickAdds.map { it.code }.toSet()
    var query by remember { mutableStateOf("") }
    var sheet by remember { mutableStateOf<ProductionSheet?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var approvalOpen by remember { mutableStateOf(false) }
    var mastersOpen by remember { mutableStateOf(false) }
    val role = model.role.orEmpty()
    val owner = role == "OWNER_ADMIN"
    val approver = role == "APPROVER" || owner
    val job = model.subtype == "MADE_TO_ORDER"
    Box(Modifier.fillMaxSize()) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 17.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
    Text("Quick Adds", color = PlantInk, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 28.sp)
    Text("Record what happened · ${model.title.orEmpty()}", color = PlantSecondary, fontFamily = PlusJakartaSans, fontSize = 13.sp)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        HubChip(OperatingMomentCatalog.roles.firstOrNull { it.first == role }?.second ?: "Production", filled = true)
        HubChip(if (job) "Job work" else "Standard", filled = false)
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(PlantCard)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            if (job) "Job output. Order. Produce. Deliver. Collect." else "Standard output. Produce. Dispatch. Collect.",
            color = PlantInk,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
        )
        Text(
            if (job) "Job output is a quantity. A detailed batch adds finished stock only from the accepted quantity. A quotation is not revenue."
            else "Standard output is a quantity. A detailed batch adds finished stock only from the accepted quantity.",
            color = PlantSecondary,
            fontFamily = PlusJakartaSans,
            fontSize = 13.sp,
        )
        val primary = if (job && "CUSTOMER_ORDER" in allowed) ProductionSheet.CUSTOMER_ORDER to "Record Customer Order"
            else if ("PRODUCTION" in allowed) ProductionSheet.PRODUCTION to "Record production"
            else null
        if (primary != null) {
            val chipInk = if (job) Color(0xFF66C7A4) else Color(0xFF2563EB)
            val chipFill = if (job) Color(0xFFEFF5F2) else Color.White
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(chipFill)
                    .clickable { sheet = primary.first }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(painterResource(R.drawable.qa_plus), contentDescription = null, tint = chipInk, modifier = Modifier.size(14.dp))
                Text(primary.second, color = chipInk, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold)
            }
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(PlantField)
            .border(1.dp, PlantBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(painterResource(R.drawable.qa_search), contentDescription = null, tint = PlantInk, modifier = Modifier.size(18.dp))
        BasicTextField(
            value = query,
            onValueChange = { query = it },
            textStyle = TextStyle(color = PlantInk, fontFamily = PlusJakartaSans, fontSize = 14.sp),
            cursorBrush = SolidColor(PlantBrand),
            decorationBox = { inner ->
                if (query.isEmpty()) Text("Search actions", color = PlantSecondary, fontFamily = PlusJakartaSans, fontSize = 14.sp)
                inner()
            },
            modifier = Modifier.weight(1f),
        )
    }
    val core = listOf(
        ProductionSheet.CUSTOMER_ORDER to "Customer order",
        ProductionSheet.PURCHASE to "Material purchase",
        ProductionSheet.PRODUCTION to "Production",
        ProductionSheet.SALE to "Sale",
        ProductionSheet.DELIVERY to "Delivery",
        ProductionSheet.COLLECTION to "Collection",
        ProductionSheet.PAYMENT to "Payment",
        ProductionSheet.SPEND to "Expense",
        ProductionSheet.STOCK to "Stock",
        ProductionSheet.UPDATE to "Update / Plan",
    ).filter { (kind, label) ->
        kind.eventType in allowed && label.contains(query, ignoreCase = true)
    }
    Text("Core actions · ${core.size}", color = PlantSecondary, fontFamily = PlusJakartaSans, fontSize = 12.sp)
    core.chunked(3).forEach { row ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            row.forEach { (kind, label) ->
                ActionCard(label, kind.icon(), locked = null, modifier = Modifier.weight(1f)) { sheet = kind }
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
    Text("Support & management", color = PlantSecondary, fontFamily = PlusJakartaSans, fontSize = 12.sp)
    val partyLabel = if (job) "Suppliers / customers" else "Suppliers / buyers"
    val support = listOf(
        Triple(ProductionSheet.REIMBURSE, "Reimbursement", null),
        Triple(ProductionSheet.APPROVAL, "Approval review", if (approver) null else "Approver only"),
        Triple(ProductionSheet.PERSON, "Add person", if (owner) null else "Owner only"),
        Triple(ProductionSheet.ROLE, "Role & access", if (owner) null else "Owner only"),
        Triple(ProductionSheet.PARTNER, partyLabel, if (owner) null else "Owner only"),
        Triple(ProductionSheet.INVITE, "Invite & QR", if (owner) null else "Owner only"),
    ).filter { (_, label, _) -> label.contains(query, ignoreCase = true) }
    support.chunked(3).forEach { row ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            row.forEach { (kind, label, lock) ->
                ActionCard(label, kind.icon(), lock, Modifier.weight(1f)) {
                    if (lock == null) sheet = kind
                }
            }
        }
    }
    Text(
        "Review approvals",
        color = if (approver) PlantAccent else PlantSecondary,
        fontFamily = PlusJakartaSans,
        modifier = Modifier.clickable(enabled = approver) { approvalOpen = true },
    )
    if (!owner) {
        Text("Some support actions stay with the Owner or Approver.", color = PlantSecondary, fontFamily = PlusJakartaSans, fontSize = 12.sp)
    }
    if (error != null) Text(error ?: "", color = Color(0xFFF87171), fontFamily = PlusJakartaSans, fontSize = 13.sp)
    }
    val open = sheet
    if (open != null) {
        OperatingSheetScrim(onDismiss = { sheet = null }) {
            OperatingCaptureSheet(
                title = open.sheetTitle(),
                icon = open.icon(),
                eventType = open.eventType,
                mode = open.captureMode,
                model = model,
                channelLabel = "Channel",
                variant = if (job) "job" else "standard",
                onDismiss = { sheet = null },
                onSubmit = { draft ->
                    onRecord(draft.toProduction())
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
                    onRecord(draft.toProduction())
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
                variant = if (job) "job" else "standard",
                onDismiss = { approvalOpen = false },
                onSubmit = {},
                onDecide = onDecide,
            )
        }
    }
    }
}



private fun ProductionSheet.icon(): Int = when (this) {
    ProductionSheet.PRODUCTION -> R.drawable.qa_factory
    ProductionSheet.PURCHASE -> R.drawable.qa_wallet
    ProductionSheet.SPEND, ProductionSheet.SALE -> R.drawable.qa_receipt
    ProductionSheet.PAYMENT -> R.drawable.qa_credit_card
    ProductionSheet.COLLECTION -> R.drawable.qa_arrow_left_right
    ProductionSheet.BUDGET, ProductionSheet.TARGET -> R.drawable.qa_budget
    ProductionSheet.UPDATE -> R.drawable.qa_message
    ProductionSheet.ISSUE -> R.drawable.qa_alert
    ProductionSheet.CUSTOMER_ORDER, ProductionSheet.STOCK -> R.drawable.qa_clipboard
    ProductionSheet.DELIVERY -> R.drawable.qa_truck
    ProductionSheet.REIMBURSE -> R.drawable.qa_receipt
    ProductionSheet.APPROVAL -> R.drawable.qa_shield
    ProductionSheet.PERSON -> R.drawable.qa_user_plus
    ProductionSheet.ROLE -> R.drawable.qa_users
    ProductionSheet.PARTNER -> R.drawable.qa_network
    ProductionSheet.INVITE -> R.drawable.qa_qr
}

@Composable
private fun ActionCard(label: String, icon: Int, locked: String?, modifier: Modifier, onClick: () -> Unit) {
    val ink = if (locked == null) PlantInk else PlantSecondary
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(PlantCard)
            .border(1.dp, PlantBorder, RoundedCornerShape(14.dp))
            .clickable(enabled = locked == null, onClick = onClick)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = ink, modifier = Modifier.size(28.dp))
        Text(label, color = ink, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, textAlign = TextAlign.Center)
        if (locked != null) Text(locked, color = PlantSecondary, fontFamily = PlusJakartaSans, fontSize = 11.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun HubChip(text: String, filled: Boolean) {
    Text(
        text,
        color = if (filled) PlantBg else PlantSecondary,
        fontFamily = PlusJakartaSans,
        fontSize = 12.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (filled) PlantBrand else Color.Transparent)
            .border(1.dp, if (filled) PlantBrand else PlantBorder, RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}


private fun ProductionSheet.sheetTitle(): String = when (this) {
    ProductionSheet.PRODUCTION -> "Record Production"
    ProductionSheet.PURCHASE -> "Material Purchase"
    ProductionSheet.SPEND -> "Record Expense"
    ProductionSheet.SALE -> "Sale / Dispatch"
    ProductionSheet.CUSTOMER_ORDER -> "Customer Order"
    ProductionSheet.DELIVERY -> "Delivery"
    ProductionSheet.PAYMENT -> "Record Payment"
    ProductionSheet.COLLECTION -> "Money collection"
    ProductionSheet.BUDGET -> "Budget / Target"
    ProductionSheet.UPDATE -> "Post Update"
    ProductionSheet.ISSUE -> "Report Issue"
    ProductionSheet.STOCK -> "Material / Stock Update"
    ProductionSheet.REIMBURSE -> "Record Reimbursement"
    ProductionSheet.PERSON, ProductionSheet.ROLE, ProductionSheet.INVITE -> "Add or invite person"
    ProductionSheet.PARTNER -> "Add Supplier"
    ProductionSheet.TARGET -> "Target"
    ProductionSheet.APPROVAL -> "Review Approval"
}

private val ProductionSheet.captureMode: String
    get() = when (this) {
        ProductionSheet.PRODUCTION -> "PRODUCTION"
        ProductionSheet.PURCHASE -> "PURCHASE"
        ProductionSheet.SPEND -> "SPEND"
        ProductionSheet.SALE -> "SALE"
        ProductionSheet.CUSTOMER_ORDER -> "ORDER"
        ProductionSheet.DELIVERY -> "DELIVERY"
        ProductionSheet.PAYMENT -> "PAYMENT"
        ProductionSheet.COLLECTION -> "COLLECTION"
        ProductionSheet.BUDGET -> "BUDGET"
        ProductionSheet.TARGET -> "TARGET"
        ProductionSheet.UPDATE -> "UPDATE"
        ProductionSheet.ISSUE -> "ISSUE"
        ProductionSheet.STOCK -> "STOCK"
        ProductionSheet.REIMBURSE -> "REIMBURSE"
        ProductionSheet.APPROVAL -> "APPROVAL"
        ProductionSheet.PERSON -> "PERSON"
        ProductionSheet.ROLE -> "ROLE"
        ProductionSheet.PARTNER -> "PARTNER"
        ProductionSheet.INVITE -> "INVITE"
    }

private fun OperatingCaptureDraft.toProduction() = ProductionEventDraft(
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
    rejectedQuantity = rejectedQuantity,
    lines = lines,
)

package com.example.momentra.ui.shell.business.marketing

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

data class MarketingEventDraft(
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
    val reportingMode: String? = null,
    val metricCode: String? = null,
    val budgetCategory: String? = null,
    val activityStatus: String? = null,
    val attributedSales: String? = null,
    val revisionKind: String? = null,
    val costBasis: String? = null,
    val overlapDeduped: Boolean? = null,
    val creativeVersion: String? = null,
    val resultSource: String? = null,
    val idempotencyKey: String? = null,
    val effectiveFrom: String? = null,
    val periodStart: String? = null,
    val periodEnd: String? = null,
    val supersedesEventId: String? = null,
)

private enum class MarketingSheet(
    val title: String,
    val eventType: String,
    val needsAmount: Boolean,
    val needsChannel: Boolean,
    val needsParty: Boolean,
    val paymentStatus: String?,
) {
    ACTIVITY("Add Activity", "ACTIVITY", false, false, false, null),
    PROGRESS("Update Progress", "PROGRESS", true, false, false, null),
    SPEND("Record Spend", "SPEND", true, true, false, null),
    RESULT("Campaign Result", "RESULT", true, true, false, null),
    PAYMENT("Record Payment", "PAYMENT", true, false, true, "PENDING"),
    INVOICE("Record Invoice", "INVOICE", true, false, true, "PENDING"),
    COLLECTION("Record Collection", "COLLECTION", true, true, false, "PENDING"),
    REVISION("Budget / Target Revision", "REVISION", true, false, false, null),
    BUDGET("Add Budget", "BUDGET", true, false, false, null),
    TARGET("Add Target", "TARGET", true, false, false, null),
    UPDATE("Add Update", "UPDATE", false, false, false, null),
    ISSUE("Add Issue", "ISSUE", false, false, false, null),
    APPROVAL("Request Approval", "APPROVAL", false, false, false, null),
    DELIVERABLE("Add Deliverable", "DELIVERABLE", false, false, false, null),
    VENDOR_COST("Add Vendor Cost", "VENDOR_COST", true, false, true, null),
    COMPLETE("Mark Complete", "COMPLETE", false, false, false, null),
    PERSON("People", "UPDATE", false, false, true, null),
    ROLE("People & Roles", "UPDATE", false, false, true, null),
    PARTNER("Partners", "UPDATE", false, false, true, null),
    CHANNELS("Channels", "UPDATE", false, false, false, null),
}

@Composable
fun MarketingQuickAddHub(
    model: OperatingProjectionDto,
    onRecord: suspend (MarketingEventDraft) -> Unit,
    onDecide: suspend (String, String) -> Unit,
) {
    val allowed = model.quickAdds.map { it.code }.toSet()
    var query by remember { mutableStateOf("") }
    var sheet by remember { mutableStateOf<MarketingSheet?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var approvalOpen by remember { mutableStateOf(false) }
    var mastersOpen by remember { mutableStateOf(false) }
    val role = model.role.orEmpty()
    val owner = role == "OWNER_ADMIN"
    val approver = role == "APPROVER" || owner
    val client = model.subtype != "OWN_BUSINESS_CAMPAIGN"
    Box(Modifier.fillMaxSize()) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 17.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
    Text("Quick Adds", color = CampInk, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 28.sp)
    Text("Record what happened · ${model.title.orEmpty()}", color = CampSecondary, fontFamily = PlusJakartaSans, fontSize = 13.sp)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        HubChip(OperatingMomentCatalog.roles.firstOrNull { it.first == role }?.second ?: "Marketing", filled = true)
        HubChip("Marketing", filled = false)
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (client) androidx.compose.ui.graphics.SolidColor(CampNotice)
                else androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(Color(0xFF6C4EF2), Color(0xFF3A3842))),
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            if (client) "Spend. Results. Approvals." else "Every marketing moment counts.",
            color = if (client) CampInk else Color(0xFFEFF5F2),
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
        )
        Text(
            if (client) "Record controlled spend, outcomes and handoffs." else "Capture spend, results, payments and approvals.",
            color = if (client) CampSecondary else Color(0xFFAFC1C3),
            fontFamily = PlusJakartaSans,
            fontSize = 13.sp,
        )
        if (client) {
            val chips = listOfNotNull(
                model.partyName?.takeIf { it.isNotBlank() }?.let { "Client · $it" },
                model.currency?.let { "Currency · $it" },
                model.life?.channels?.takeIf { it.isNotEmpty() }?.let { "Channels · ${it.joinToString(" ")}" },
                model.budget?.amount?.let { "Budget · $it" },
                model.target?.value?.let { "Target · $it" },
            )
            chips.forEach { label ->
                Text(label, color = CampSecondary, fontFamily = PlusJakartaSans, fontSize = 12.sp)
            }
        }
        if ("SPEND" in allowed) {
            val chipInk = if (client) CampBg else Color(0xFF66C7A4)
            val chipFill = if (client) CampBrand else Color(0xFFEFF5F2)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(chipFill)
                    .clickable { sheet = MarketingSheet.SPEND }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(painterResource(R.drawable.qa_plus), contentDescription = null, tint = chipInk, modifier = Modifier.size(14.dp))
                Text("Record Spend", color = chipInk, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold)
            }
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CampField)
            .border(1.dp, CampBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(painterResource(R.drawable.qa_search), contentDescription = null, tint = CampInk, modifier = Modifier.size(18.dp))
        BasicTextField(
            value = query,
            onValueChange = { query = it },
            textStyle = TextStyle(color = CampInk, fontFamily = PlusJakartaSans, fontSize = 14.sp),
            cursorBrush = SolidColor(CampBrand),
            decorationBox = { inner ->
                if (query.isEmpty()) Text("Search actions", color = CampSecondary, fontFamily = PlusJakartaSans, fontSize = 14.sp)
                inner()
            },
            modifier = Modifier.weight(1f),
        )
    }
    val core = listOf(
        MarketingSheet.ACTIVITY to "Add Activity",
        MarketingSheet.PROGRESS to "Update Progress",
        MarketingSheet.SPEND to "Record Spend",
        MarketingSheet.PAYMENT to "Record Payment",
        MarketingSheet.INVOICE to "Record Invoice",
        MarketingSheet.COLLECTION to "Record Collection",
        MarketingSheet.REVISION to "Budget / Target Revision",
        MarketingSheet.APPROVAL to "Approval / Decision",
        MarketingSheet.UPDATE to "Team Update",
        MarketingSheet.ISSUE to "Issue",
        MarketingSheet.RESULT to "Campaign Result",
    ).filter { (kind, label) ->
        kind.eventType in allowed && label.contains(query, ignoreCase = true)
    }
    Text("Core actions · ${core.size}", color = CampSecondary, fontFamily = PlusJakartaSans, fontSize = 12.sp)
    core.chunked(3).forEach { row ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            row.forEach { (kind, label) ->
                ActionCard(label, kind.icon(client), locked = null, modifier = Modifier.weight(1f)) { sheet = kind }
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
    Text("Support & management", color = CampSecondary, fontFamily = PlusJakartaSans, fontSize = 12.sp)
    val support = if (client) {
        listOf(
            Triple(MarketingSheet.PERSON, "People", if (owner) null else "Owner only"),
            Triple(MarketingSheet.PARTNER, "Partners", if (owner) null else "Owner only"),
            Triple(MarketingSheet.CHANNELS, "Channels", if (owner) null else "Owner only"),
        )
    } else {
        listOf(
            Triple(MarketingSheet.ROLE, "People & Roles", if (owner) null else "Owner only"),
            Triple(MarketingSheet.PARTNER, "Partners & Channels", if (owner) null else "Owner only"),
        )
    }.filter { (_, label, _) -> label.contains(query, ignoreCase = true) }
    support.chunked(3).forEach { row ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            row.forEach { (kind, label, lock) ->
                ActionCard(label, kind.icon(client), lock, Modifier.weight(1f)) {
                    if (lock == null) sheet = kind
                }
            }
        }
    }
    Text(
        "Review approvals",
        color = if (approver) CampAccent else CampSecondary,
        fontFamily = PlusJakartaSans,
        modifier = Modifier.clickable(enabled = approver) { approvalOpen = true },
    )
    if (!owner) {
        Text("Some support actions stay with the Owner or Approver.", color = CampSecondary, fontFamily = PlusJakartaSans, fontSize = 12.sp)
    }
    if (error != null) Text(error ?: "", color = Color(0xFFF87171), fontFamily = PlusJakartaSans, fontSize = 13.sp)
    }
    val open = sheet
    if (open != null) {
        OperatingSheetScrim(onDismiss = { sheet = null }) {
            OperatingCaptureSheet(
                title = open.sheetTitle(client),
                icon = open.icon(client),
                eventType = open.eventType,
                mode = open.captureMode,
                model = model,
                channelLabel = "Channel",
                variant = if (client) "client" else "own",
                onDismiss = { sheet = null },
                onSubmit = { draft ->
                    onRecord(draft.toMarketing())
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
                    onRecord(draft.toMarketing())
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
                variant = if (client) "client" else "own",
                onDismiss = { approvalOpen = false },
                onSubmit = {},
                onDecide = onDecide,
            )
        }
    }
    }
}



private fun MarketingSheet.icon(client: Boolean): Int = when (this) {
        MarketingSheet.ACTIVITY -> R.drawable.qa_message
        MarketingSheet.PROGRESS -> if (client) R.drawable.qa_message else R.drawable.qa_budget
        MarketingSheet.INVOICE -> R.drawable.qa_arrow_left_right
        MarketingSheet.REVISION -> R.drawable.qa_budget
        MarketingSheet.SPEND -> if (client) R.drawable.qa_receipt else R.drawable.qa_wallet
    MarketingSheet.RESULT -> if (client) R.drawable.qa_message else R.drawable.qa_budget
    MarketingSheet.PAYMENT -> R.drawable.qa_credit_card
    MarketingSheet.COLLECTION -> R.drawable.qa_arrow_left_right
    MarketingSheet.BUDGET, MarketingSheet.TARGET -> if (client && this == MarketingSheet.TARGET) R.drawable.qa_alert else R.drawable.qa_budget
    MarketingSheet.UPDATE -> R.drawable.qa_message
    MarketingSheet.ISSUE -> R.drawable.qa_alert
    MarketingSheet.APPROVAL -> R.drawable.qa_shield
    MarketingSheet.DELIVERABLE -> if (client) R.drawable.qa_clipboard else R.drawable.qa_message
    MarketingSheet.VENDOR_COST -> if (client) R.drawable.qa_wallet else R.drawable.qa_network
    MarketingSheet.COMPLETE -> if (client) R.drawable.qa_check else R.drawable.qa_clipboard
    MarketingSheet.PERSON -> R.drawable.qa_user_plus
    MarketingSheet.ROLE -> R.drawable.qa_users
    MarketingSheet.PARTNER -> R.drawable.qa_network
    MarketingSheet.CHANNELS -> R.drawable.qa_message
}

@Composable
private fun ActionCard(label: String, icon: Int, locked: String?, modifier: Modifier, onClick: () -> Unit) {
    val ink = if (locked == null) CampInk else CampSecondary
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(CampCard)
            .border(1.dp, CampBorder, RoundedCornerShape(14.dp))
            .clickable(enabled = locked == null, onClick = onClick)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = ink, modifier = Modifier.size(28.dp))
        Text(label, color = ink, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, textAlign = TextAlign.Center)
        if (locked != null) Text(locked, color = CampSecondary, fontFamily = PlusJakartaSans, fontSize = 11.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun HubChip(text: String, filled: Boolean) {
    Text(
        text,
        color = if (filled) CampBg else CampSecondary,
        fontFamily = PlusJakartaSans,
        fontSize = 12.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (filled) CampBrand else Color.Transparent)
            .border(1.dp, if (filled) CampBrand else CampBorder, RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}


private val MarketingSheet.captureMode: String
    get() = when (this) {
        MarketingSheet.ACTIVITY -> "UPDATE"
        MarketingSheet.PROGRESS -> "RESULT"
        MarketingSheet.INVOICE -> "SALE"
        MarketingSheet.REVISION -> "BUDGET"
        MarketingSheet.SPEND -> "SPEND"
        MarketingSheet.RESULT -> "RESULT"
        MarketingSheet.PAYMENT -> "PAYMENT"
        MarketingSheet.COLLECTION -> "COLLECTION"
        MarketingSheet.BUDGET -> "BUDGET"
        MarketingSheet.TARGET -> "TARGET"
        MarketingSheet.UPDATE -> "UPDATE"
        MarketingSheet.ISSUE -> "ISSUE"
        MarketingSheet.APPROVAL -> "UPDATE"
        MarketingSheet.DELIVERABLE -> "DELIVERABLE"
        MarketingSheet.VENDOR_COST -> "VENDOR"
        MarketingSheet.COMPLETE -> "COMPLETE"
        MarketingSheet.PERSON -> "PERSON"
        MarketingSheet.ROLE -> "ROLE"
        MarketingSheet.PARTNER -> "PARTNER"
        MarketingSheet.CHANNELS -> "PARTNER"
    }

private fun MarketingSheet.sheetTitle(client: Boolean): String = when (this) {
    MarketingSheet.ACTIVITY -> "Add Activity"
    MarketingSheet.PROGRESS -> "Update Progress"
    MarketingSheet.INVOICE -> "Record Invoice"
    MarketingSheet.REVISION -> "Budget / Target Revision"
    MarketingSheet.SPEND -> "Add Spend / Expense"
    MarketingSheet.RESULT -> "Add Result"
    MarketingSheet.PAYMENT -> "Add Payment"
    MarketingSheet.COLLECTION -> if (client) "Client Billing / Collection" else "Add Collection"
    MarketingSheet.BUDGET -> "Add Budget"
    MarketingSheet.TARGET -> if (client) "Target Revision" else "Add Target"
    MarketingSheet.UPDATE -> "Add Update"
    MarketingSheet.ISSUE -> "Add Issue"
    MarketingSheet.APPROVAL -> "Request Approval"
    MarketingSheet.DELIVERABLE -> "Add Deliverable / Milestone"
    MarketingSheet.VENDOR_COST -> "Add Vendor / Creator Cost"
    MarketingSheet.COMPLETE -> "Mark Result / Deliverable Complete"
    MarketingSheet.PERSON, MarketingSheet.ROLE -> "Add or invite person"
    MarketingSheet.PARTNER, MarketingSheet.CHANNELS -> if (client) "Client / Partners / Channels" else "Partners & Channels"
}

private fun OperatingCaptureDraft.toMarketing() = MarketingEventDraft(
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
    reportingMode = reportingMode ?: if (eventType == "RESULT" || (eventType == "PROGRESS" && !amount.isNullOrBlank())) "INCREMENTAL" else null,
    metricCode = metricCode ?: if (eventType == "RESULT" || (eventType == "PROGRESS" && !amount.isNullOrBlank())) "LEADS" else null,
    budgetCategory = if (eventType == "SPEND" || eventType == "INVOICE") "DIGITAL_ADS" else null,
    activityStatus = activityStatus ?: if (eventType == "ACTIVITY") "IN_PROGRESS" else null,
    revisionKind = revisionKind ?: if (eventType == "REVISION") "BUDGET" else null,
    costBasis = costBasis,
    overlapDeduped = overlapDeduped,
    creativeVersion = creativeVersion,
    resultSource = resultSource,
    idempotencyKey = idempotencyKey,
    effectiveFrom = effectiveFrom,
    periodStart = periodStart,
    periodEnd = periodEnd,
    supersedesEventId = supersedesEventId,
)

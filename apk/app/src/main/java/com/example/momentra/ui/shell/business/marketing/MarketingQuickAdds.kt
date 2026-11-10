package com.example.momentra.ui.shell.business.marketing

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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.R
import com.example.momentra.data.api.OperatingProjectionDto
import com.example.momentra.ui.shell.business.shared.BusinessInviteQr
import com.example.momentra.ui.shell.business.shared.InvitePalette
import com.example.momentra.ui.shell.business.shared.OperatingCaptureDraft
import com.example.momentra.ui.shell.business.shared.OperatingCaptureSheet
import com.example.momentra.ui.shell.business.shared.OperatingSheetScrim
import com.example.momentra.ui.shell.business.shared.OperatingV12Date
import com.example.momentra.ui.shell.business.shared.OperatingV12Theme
import com.example.momentra.ui.theme.PlusJakartaSans

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
    val lines: List<com.example.momentra.data.api.OperatingLineBody>? = null,
)

private enum class MarketingSheet(
    val title: String,
    val eventType: String,
    val mode: String,
) {
    ACTIVITY("Add Activity", "ACTIVITY", "UPDATE"),
    PROGRESS("Update Progress", "PROGRESS", "RESULT"),
    SPEND("Record Spend", "SPEND", "SPEND"),
    RESULT("Campaign Result", "RESULT", "RESULT"),
    PAYMENT("Record Payment", "PAYMENT", "PAYMENT"),
    INVOICE("Record Invoice", "INVOICE", "SALE"),
    COLLECTION("Money collection", "COLLECTION", "COLLECTION"),
    REVISION("Budget / Target Revision", "REVISION", "BUDGET"),
    BUDGET("Add Budget", "BUDGET", "BUDGET"),
    TARGET("Add Target", "TARGET", "TARGET"),
    UPDATE("Add Update", "UPDATE", "UPDATE"),
    ISSUE("Add Issue", "ISSUE", "ISSUE"),
    APPROVAL("Request Approval", "APPROVAL", "UPDATE"),
    DELIVERABLE("Add Deliverable", "DELIVERABLE", "DELIVERABLE"),
    VENDOR_COST("Add Vendor Cost", "VENDOR_COST", "VENDOR"),
    COMPLETE("Mark Complete", "COMPLETE", "COMPLETE"),
    PERSON("People", "UPDATE", "PERSON"),
    ROLE("People & Roles", "UPDATE", "ROLE"),
    PARTNER("Partners", "UPDATE", "PARTNER"),
    CHANNELS("Channels", "UPDATE", "PARTNER"),
    HOLD("Campaign hold", "ACTIVITY", "HOLD"),
    /** Budget move — reuse versioned budget/target revision capture (no inventing a transfer API). */
    TRANSFER("Budget transfer", "REVISION", "BUDGET"),
    REPRINT("Reprint invoice", "INVOICE", "REPRINT"),
}

private enum class MarketingMastersTab(val title: String) {
    ClientsVendors("Clients & vendors"),
    ServicesRate("Services & rate card"),
    ChannelsTeams("Channels & teams"),
    Invite("Invite & QR"),
}

private data class HubTile(
    val label: String,
    val caption: String,
    val icon: Int,
    val sheet: MarketingSheet? = null,
    val requires: String? = null,
    val masters: MarketingMastersTab? = null,
    val approval: Boolean = false,
    val clientOnly: Boolean = false,
    /** Honest non-actionable tile — shown but never opens a capture that would invent types. */
    val unavailable: Boolean = false,
)

@Composable
fun MarketingQuickAddHub(
    model: OperatingProjectionDto,
    onRecord: suspend (MarketingEventDraft) -> Unit,
    onDecide: suspend (String, String) -> Unit,
    momentId: String? = null,
) {
    val theme = remember(model.subtype) { OperatingV12Theme.forSubtype(model.subtype) }
    val brand = theme.accent
    val client = isClientCampaign(model.subtype)
    val allowed = model.quickAdds.map { it.code }.toSet()
    var query by remember { mutableStateOf("") }
    var sheet by remember { mutableStateOf<MarketingSheet?>(null) }
    var mastersOpen by remember { mutableStateOf<MarketingMastersTab?>(null) }
    var approvalOpen by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
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
            Text("Quick Adds", color = CampInk, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 28.sp)
            Text(
                "Plan. Launch. Measure.",
                color = brand,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
            )
            ContextChips(model, theme)
            HubHero(model, theme, client) {
                if ("ACTIVITY" in allowed) sheet = MarketingSheet.ACTIVITY
            }
            SearchBar(query = query, onQuery = { query = it }, brand = brand)
            val core = coreTiles(client).filter { tile ->
                tile.label.contains(query, ignoreCase = true) || tile.caption.contains(query, ignoreCase = true)
            }
            SectionLabel("Core actions · ${core.size}", "Tap to record", brand)
            TileGrid(
                tiles = core,
                allowed = allowed,
                brand = brand,
                onSheet = { sheet = it },
                onMasters = { mastersOpen = it },
                onApproval = { approvalOpen = true },
            )
            val support = supportTiles(client).filter { tile ->
                tile.label.contains(query, ignoreCase = true) || tile.caption.contains(query, ignoreCase = true)
            }
            SectionLabel("Support & management · ${support.size}", "Role-gated", brand)
            TileGrid(
                tiles = support,
                allowed = allowed,
                brand = brand,
                ownerOnly = !owner,
                onSheet = { sheet = it },
                onMasters = { mastersOpen = it },
                onApproval = { approvalOpen = true },
            )
            val utilities = utilityTiles(client).filter { tile ->
                tile.label.contains(query, ignoreCase = true) || tile.caption.contains(query, ignoreCase = true)
            }
            SectionLabel("Utilities · ${utilities.size}", "Hold · transfer · reprint", brand)
            TileGrid(
                tiles = utilities,
                allowed = allowed,
                brand = brand,
                onSheet = { sheet = it },
                onMasters = { mastersOpen = it },
                onApproval = { approvalOpen = true },
            )
            WhyTheseTiles(client, brand)
            if (approver) {
                Text(
                    "Review approvals",
                    color = brand,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { approvalOpen = true },
                )
            }
            if (!owner) {
                Text("Some support actions stay with the Owner or Approver.", color = CampSecondary, fontFamily = PlusJakartaSans, fontSize = 12.sp)
            }
            if (error != null) Text(error ?: "", color = CampDanger, fontFamily = PlusJakartaSans, fontSize = 13.sp)
            Spacer(Modifier.height(16.dp))
        }

        sheet?.let { open ->
            OperatingSheetScrim(onDismiss = { sheet = null }, accent = brand) {
                OperatingCaptureSheet(
                    title = open.sheetTitle(client),
                    icon = open.icon(client),
                    eventType = open.eventType,
                    mode = open.mode,
                    model = model,
                    channelLabel = "Channel",
                    variant = if (client) "client" else "own",
                    onDismiss = { sheet = null },
                    onSubmit = { draft ->
                        runCatching { onRecord(draft.toMarketing()) }
                            .onFailure { error = it.message }
                        sheet = null
                    },
                    onDecide = onDecide,
                )
            }
        }
        mastersOpen?.let { tab ->
            OperatingSheetScrim(onDismiss = { mastersOpen = null }, accent = brand) {
                when (tab) {
                    MarketingMastersTab.Invite -> {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                                .background(CampBg)
                                .padding(16.dp),
                        ) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Invite & QR", color = CampInk, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
                                Text("Close", color = CampSecondary, fontFamily = PlusJakartaSans, modifier = Modifier.clickable { mastersOpen = null })
                            }
                            Spacer(Modifier.height(12.dp))
                            BusinessInviteQr(
                                momentId = momentId,
                                title = model.title.orEmpty(),
                                role = model.role,
                                palette = InvitePalette(
                                    accent = brand,
                                    accentMuted = brand.copy(alpha = 0.7f),
                                    card = CampCard,
                                    field = CampField,
                                    chip = CampNotice,
                                    chipStroke = CampBorder,
                                    divider = CampBorder,
                                    qrPlate = CampCard,
                                    textPrimary = CampInk,
                                    textBody = CampSecondary,
                                    textMuted = CampSecondary,
                                    textFaint = CampSecondary.copy(alpha = 0.7f),
                                    warning = CampWarn,
                                    negative = CampDanger,
                                ),
                                iconRes = R.drawable.ic_mkt_qr,
                                noun = "campaign",
                                onError = { error = it },
                            )
                        }
                    }
                    else -> {
                        OperatingCaptureSheet(
                            title = tab.title,
                            icon = when (tab) {
                                MarketingMastersTab.ClientsVendors -> R.drawable.ic_mkt_users
                                MarketingMastersTab.ServicesRate -> R.drawable.ic_mkt_tag
                                MarketingMastersTab.ChannelsTeams -> R.drawable.ic_mkt_channels
                                MarketingMastersTab.Invite -> R.drawable.ic_mkt_qr
                            },
                            eventType = "UPDATE",
                            mode = when (tab) {
                                MarketingMastersTab.ClientsVendors -> "PARTNER"
                                MarketingMastersTab.ServicesRate -> "MASTERS"
                                MarketingMastersTab.ChannelsTeams -> "PARTNER"
                                MarketingMastersTab.Invite -> "INVITE"
                            },
                            model = model,
                            variant = if (client) "client" else "own",
                            onDismiss = { mastersOpen = null },
                            onSubmit = { draft ->
                                onRecord(draft.toMarketing())
                                mastersOpen = null
                            },
                        )
                    }
                }
            }
        }
        if (approvalOpen) {
            OperatingSheetScrim(onDismiss = { approvalOpen = false }, accent = brand) {
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

@Composable
private fun HubHero(model: OperatingProjectionDto, theme: OperatingV12Theme, client: Boolean, onAddActivity: () -> Unit) {
    val period = listOfNotNull(
        model.title?.takeIf { it.isNotBlank() },
        model.partyName?.takeIf { it.isNotBlank() },
        if (client) "Client" else "Own",
        OperatingV12Date.monthYear(model.startDate),
        model.currency,
    ).joinToString(" · ")
    val shape = RoundedCornerShape(20.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .shadow(12.dp, shape, clip = false, ambientColor = theme.accent.copy(alpha = 0.35f), spotColor = theme.accent.copy(alpha = 0.35f))
            .clip(shape)
            .background(CampCard)
            .border(1.dp, theme.accent.copy(alpha = 0.35f), shape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            if (client) "Spend. Results. Approvals." else "Every marketing moment counts.",
            color = CampInk,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
        )
        Text(
            if (client) "Record controlled spend, outcomes and handoffs." else "Capture spend, results, payments and approvals.",
            color = CampSecondary,
            fontFamily = PlusJakartaSans,
            fontSize = 13.sp,
        )
        Text(period, color = CampSecondary, fontFamily = PlusJakartaSans, fontSize = 12.sp)
        Text(
            "Add Activity",
            color = Color.White,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(theme.accent)
                .clickable(onClick = onAddActivity)
                .padding(vertical = 14.dp),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun SearchBar(query: String, onQuery: (String) -> Unit, brand: Color) {
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
            onValueChange = onQuery,
            textStyle = TextStyle(color = CampInk, fontFamily = PlusJakartaSans, fontSize = 14.sp),
            cursorBrush = SolidColor(brand),
            decorationBox = { inner ->
                if (query.isEmpty()) Text("Search actions…", color = CampSecondary, fontFamily = PlusJakartaSans, fontSize = 14.sp)
                inner()
            },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SectionLabel(title: String, trailing: String, brand: Color) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = CampSecondary, fontFamily = PlusJakartaSans, fontSize = 12.sp)
        Text(trailing, color = brand.copy(alpha = 0.85f), fontFamily = PlusJakartaSans, fontSize = 11.sp)
    }
}

@Composable
private fun TileGrid(
    tiles: List<HubTile>,
    allowed: Set<String>,
    brand: Color,
    ownerOnly: Boolean = false,
    onSheet: (MarketingSheet) -> Unit,
    onMasters: (MarketingMastersTab) -> Unit,
    onApproval: () -> Unit,
) {
    tiles.chunked(3).forEach { row ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            row.forEach { tile ->
                val locked = when {
                    tile.unavailable -> "Unavailable"
                    tile.masters != null && ownerOnly -> "Owner only"
                    tile.requires != null && tile.requires !in allowed -> "Role locked"
                    else -> null
                }
                ActionCard(
                    label = tile.label,
                    caption = tile.caption,
                    icon = tile.icon,
                    locked = locked,
                    brand = brand,
                    modifier = Modifier.weight(1f),
                ) {
                    when {
                        locked != null -> Unit
                        tile.approval -> onApproval()
                        tile.masters != null -> onMasters(tile.masters)
                        tile.sheet != null -> onSheet(tile.sheet)
                    }
                }
            }
            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun ActionCard(
    label: String,
    caption: String,
    icon: Int,
    locked: String?,
    brand: Color,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val ink = if (locked == null) CampInk else CampSecondary
    Column(
        modifier
            .height(112.dp)
            .shadow(6.dp, RoundedCornerShape(14.dp), ambientColor = brand.copy(alpha = 0.25f), spotColor = brand.copy(alpha = 0.3f))
            .clip(RoundedCornerShape(14.dp))
            .background(CampCard)
            .border(1.dp, brand.copy(alpha = 0.28f), RoundedCornerShape(14.dp))
            .clickable(enabled = locked == null, onClick = onClick)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(brand.copy(alpha = 0.16f))
                .border(1.dp, brand.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(icon), contentDescription = null, tint = if (locked == null) brand else CampSecondary, modifier = Modifier.size(16.dp))
        }
        Text(label, color = ink, fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(locked ?: caption, color = CampSecondary, fontFamily = PlusJakartaSans, fontSize = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun WhyTheseTiles(client: Boolean, brand: Color) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(brand.copy(alpha = 0.08f))
            .border(1.dp, brand.copy(alpha = 0.28f), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Why these tiles save differently", color = CampInk, fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        val lines = buildList {
            add("Spend ≠ payment · Invoice ≠ collection")
            add("Attributed sales ≠ ledger revenue")
            add("Empty ≠ zero · Approval ≠ disbursement")
            if (client) {
                add("Client Invoice / Collection available — Own hides them")
            } else {
                add("Invoice & Collection unavailable in Own Business — server-rejected")
            }
            add("Results are measured, never booked as income")
        }
        lines.forEach { line ->
            Text("·  $line", color = CampSecondary, fontFamily = PlusJakartaSans, fontSize = 12.sp)
        }
    }
}

@Composable
private fun MastersFallback(tab: MarketingMastersTab, brand: Color, onClose: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .background(CampBg)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(tab.title, color = CampInk, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
            Text("Close", color = CampSecondary, fontFamily = PlusJakartaSans, modifier = Modifier.clickable(onClick = onClose))
        }
        Text("Open this moment to manage ${tab.title.lowercase()}.", color = CampSecondary, fontFamily = PlusJakartaSans, fontSize = 13.sp)
        Text(
            "Done",
            color = Color.White,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(brand)
                .clickable(onClick = onClose)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}

private fun coreTiles(client: Boolean): List<HubTile> {
    val base = listOf(
        HubTile("Activity", "Task · brief · due", R.drawable.qa_message, MarketingSheet.ACTIVITY, "ACTIVITY"),
        HubTile("Progress", "Milestone update", R.drawable.qa_budget, MarketingSheet.PROGRESS, "PROGRESS"),
        HubTile("Spend", "Media · vendor bill", R.drawable.qa_wallet, MarketingSheet.SPEND, "SPEND"),
        HubTile("Payment", "Settle open AP", R.drawable.qa_credit_card, MarketingSheet.PAYMENT, "PAYMENT"),
    )
    val clientMoney = listOf(
        HubTile("Invoice", "Client bill · GST", R.drawable.qa_receipt, MarketingSheet.INVOICE, "INVOICE", clientOnly = true),
        HubTile("Collection", "Allocate open AR", R.drawable.qa_arrow_left_right, MarketingSheet.COLLECTION, "COLLECTION", clientOnly = true),
    )
    val rest = listOf(
        HubTile("Budget", "Versioned revise", R.drawable.qa_budget, MarketingSheet.REVISION, "REVISION"),
        HubTile("Result", "Campaign outcome", R.drawable.ic_mkt_mega, MarketingSheet.RESULT, "RESULT"),
        HubTile("Approval", "Decision · sign-off", R.drawable.qa_shield, MarketingSheet.APPROVAL, "APPROVAL"),
    )
    return if (client) base + clientMoney + rest else base + rest
}

private fun supportTiles(client: Boolean): List<HubTile> = listOf(
    HubTile(
        if (client) "Clients & vendors" else "Vendors & creators",
        if (client) "Brands · agencies" else "Agencies · media",
        R.drawable.ic_mkt_users,
        masters = MarketingMastersTab.ClientsVendors,
    ),
    HubTile(
        if (client) "Services & rate card" else "Vendor rates",
        if (client) "Retainers · rates" else "Media · creator rates",
        R.drawable.ic_mkt_tag,
        masters = MarketingMastersTab.ServicesRate,
    ),
    HubTile("Channels & teams", "Media · pods", R.drawable.ic_mkt_channels, masters = MarketingMastersTab.ChannelsTeams),
    HubTile("Invite & QR", "Join · people · roles", R.drawable.ic_mkt_qr, masters = MarketingMastersTab.Invite),
    HubTile("Issue", "Update · blocker · risk", R.drawable.qa_alert, MarketingSheet.ISSUE, "ISSUE"),
)

private fun utilityTiles(client: Boolean): List<HubTile> = buildList {
    // Marketing contract has no campaign-hold / budget-transfer / invoice-reprint writes.
    // Keep the tiles visible so the hub stays honest — never open a fake capture that posts
    // ACTIVITY / REVISION / INVOICE under the wrong mode.
    add(HubTile("Hold", "Park campaign work", R.drawable.qa_shield, MarketingSheet.HOLD, "ACTIVITY", unavailable = true))
    add(HubTile("Transfer", "Budget move", R.drawable.qa_arrow_left_right, MarketingSheet.TRANSFER, "REVISION", unavailable = true))
    if (client) {
        add(HubTile("Reprint", "Invoice · receipt", R.drawable.qa_receipt, MarketingSheet.REPRINT, "INVOICE", unavailable = true))
    }
}

private fun MarketingSheet.icon(client: Boolean): Int = when (this) {
    MarketingSheet.ACTIVITY -> R.drawable.qa_message
    MarketingSheet.PROGRESS -> if (client) R.drawable.qa_message else R.drawable.qa_budget
    MarketingSheet.INVOICE, MarketingSheet.REPRINT -> R.drawable.qa_receipt
    MarketingSheet.REVISION, MarketingSheet.BUDGET, MarketingSheet.TARGET, MarketingSheet.TRANSFER -> R.drawable.qa_budget
    MarketingSheet.SPEND -> if (client) R.drawable.qa_receipt else R.drawable.qa_wallet
    MarketingSheet.RESULT -> R.drawable.ic_mkt_mega
    MarketingSheet.PAYMENT -> R.drawable.qa_credit_card
    MarketingSheet.COLLECTION -> R.drawable.qa_arrow_left_right
    MarketingSheet.UPDATE -> R.drawable.qa_message
    MarketingSheet.ISSUE -> R.drawable.qa_alert
    MarketingSheet.APPROVAL, MarketingSheet.HOLD -> R.drawable.qa_shield
    MarketingSheet.DELIVERABLE, MarketingSheet.COMPLETE -> R.drawable.qa_clipboard
    MarketingSheet.VENDOR_COST -> R.drawable.qa_wallet
    MarketingSheet.PERSON -> R.drawable.qa_user_plus
    MarketingSheet.ROLE -> R.drawable.qa_users
    MarketingSheet.PARTNER, MarketingSheet.CHANNELS -> R.drawable.qa_network
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
    MarketingSheet.HOLD -> "Campaign hold"
    MarketingSheet.TRANSFER -> "Budget transfer"
    MarketingSheet.REPRINT -> "Reprint invoice"
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
    lines = lines,
    attributedSales = attributedSales,
)

package com.example.momentra.ui.shell.business.marketing

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.R
import com.example.momentra.data.api.OperatingAttentionDto
import com.example.momentra.data.api.OperatingBoardSectionDto
import com.example.momentra.data.api.OperatingPaymentDto
import com.example.momentra.data.api.OperatingProjectionDto
import com.example.momentra.ui.shell.business.shared.BusinessActivityList
import com.example.momentra.ui.shell.business.shared.BusinessEmptyState
import com.example.momentra.ui.shell.business.shared.OperatingChromeState
import com.example.momentra.ui.shell.business.shared.OperatingChromeSheets
import com.example.momentra.ui.shell.business.shared.OperatingMomentCatalog
import com.example.momentra.ui.shell.business.shared.OperatingSheetScrim
import com.example.momentra.ui.shell.business.shared.OperatingTab
import com.example.momentra.ui.shell.business.shared.OperatingV12
import com.example.momentra.ui.shell.business.shared.OperatingV12Card
import com.example.momentra.ui.shell.business.shared.OperatingV12Date
import com.example.momentra.ui.shell.business.shared.OperatingV12Money
import com.example.momentra.ui.shell.business.shared.OperatingV12Ring
import com.example.momentra.ui.shell.business.shared.OperatingV12Theme
import com.example.momentra.ui.shell.business.shared.OperatingV12Tile
import com.example.momentra.ui.shell.business.shared.OperatingV12Why
import com.example.momentra.ui.shell.business.shared.formatOperatingAmount
import com.example.momentra.ui.shell.business.shared.formatOperatingDetail
import com.example.momentra.ui.shell.business.shared.openOperatingAttention
import com.example.momentra.ui.shell.business.shared.operatingAttentionCtaLabel
import com.example.momentra.ui.shell.business.shared.pulseGroup
import com.example.momentra.ui.theme.PlusJakartaSans
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Client indigo — kept for any legacy call sites; prefer [campBrandFor]. */
@Deprecated("Use campBrandFor(subtype)", ReplaceWith("campBrandFor(subtype)"))
internal val CampBrand = Color(0xFF6366F1)
@Deprecated("Use campBrandFor(subtype)", ReplaceWith("campBrandFor(subtype)"))
internal val CampAccent = Color(0xFF6366F1)

internal val CampBg = Color(0xFF09121F)
internal val CampCard = Color(0xFF122235)
internal val CampNotice = Color(0xFF16405C)
internal val CampField = Color(0xFF09121F)
internal val CampBorder = Color(0xFF2B4B63)
internal val CampSecondary = Color(0xFFA5BBCE)
internal val CampInk = Color(0xFFE5E0EE)
internal val CampSuccess = Color(0xFF22C55E)
internal val CampWarn = Color(0xFFF59E0B)
internal val CampDanger = Color(0xFFEF4444)

internal fun isOwnCampaign(subtype: String?): Boolean =
    subtype.equals("OWN_BUSINESS_CAMPAIGN", ignoreCase = true)

internal fun isClientCampaign(subtype: String?): Boolean =
    !isOwnCampaign(subtype)

/** Client `#6366F1` · Own `#D946EF` — Figma MKT / MKT-OWN v12. */
internal fun campBrandFor(subtype: String?): Color =
    if (isOwnCampaign(subtype)) Color(0xFFD946EF) else Color(0xFF6366F1)

private sealed interface MarketingBoardSheet {
    data object Attention : MarketingBoardSheet
    data object TargetBudget : MarketingBoardSheet
    data object Payment : MarketingBoardSheet
    data object Finance : MarketingBoardSheet
    data object Activity : MarketingBoardSheet
    data object Commitments : MarketingBoardSheet
    data object Campaign : MarketingBoardSheet
    data object People : MarketingBoardSheet
    data object Channels : MarketingBoardSheet
    data object LifeCurrent : MarketingBoardSheet
    data object LifeChannels : MarketingBoardSheet
    data object LifeMoney : MarketingBoardSheet
    data object LifePeople : MarketingBoardSheet
    data object MemoryMonthly : MarketingBoardSheet
    data object MemoryMoney : MarketingBoardSheet
    data object MemoryPeople : MarketingBoardSheet
    data object MemoryPatterns : MarketingBoardSheet
}

@Composable
fun MarketingActive(
    model: OperatingProjectionDto,
    tab: OperatingTab,
    onOpenQuickAdd: () -> Unit,
    onRecord: suspend (MarketingEventDraft) -> Unit,
    onDecide: suspend (String, String) -> Unit,
    momentId: String? = null,
    modifier: Modifier = Modifier,
) {
    val theme = remember(model.subtype) { OperatingV12Theme.forSubtype(model.subtype) }
    val brand = theme.accent
    var boardSheet by remember { mutableStateOf<MarketingBoardSheet?>(null) }

    Box(modifier.fillMaxSize().background(theme.canvas)) {
        if (tab == OperatingTab.QUICK_ADD) {
            MarketingQuickAddHub(model, onRecord, onDecide, momentId)
        } else {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 17.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                ScreenHeader(model, tab, theme)
                ContextChips(model, theme)
                when (tab) {
                    OperatingTab.PULSE -> MarketingPulse(
                        model = model,
                        theme = theme,
                        onOpenQuickAdd = onOpenQuickAdd,
                        onDecide = onDecide,
                        onOpenSheet = { boardSheet = it },
                    )
                    OperatingTab.MOMENTS -> MarketingMoments(
                        model = model,
                        theme = theme,
                        onOpenQuickAdd = onOpenQuickAdd,
                        onOpenSheet = { boardSheet = it },
                    )
                    OperatingTab.LIFE -> MarketingLife(
                        model = model,
                        theme = theme,
                        onOpenSheet = { boardSheet = it },
                    )
                    OperatingTab.MEMORY -> MarketingMemory(
                        model = model,
                        theme = theme,
                        momentId = momentId,
                        onOpenSheet = { boardSheet = it },
                    )
                    OperatingTab.QUICK_ADD -> Unit
                }
                Spacer(Modifier.height(24.dp))
            }
        }
        boardSheet?.let { open ->
            MarketingViewAllSheet(
                sheet = open,
                model = model,
                theme = theme,
                brand = brand,
                onClose = { boardSheet = null },
                onDecide = onDecide,
                onOpenQuickAdd = onOpenQuickAdd,
            )
        }
        OperatingChromeSheets(model)
    }
}

@Composable
private fun ScreenHeader(model: OperatingProjectionDto, tab: OperatingTab, theme: OperatingV12Theme) {
    val title = when (tab) {
        OperatingTab.PULSE -> "${model.title?.ifBlank { null } ?: "Campaign"} Pulse"
        OperatingTab.MOMENTS -> "Moments"
        OperatingTab.LIFE -> "Life"
        OperatingTab.MEMORY -> "Memory"
        OperatingTab.QUICK_ADD -> "Quick Adds"
    }
    val role = OperatingMomentCatalog.roles.firstOrNull { it.first == model.role }?.second ?: "Marketing"
    val party = model.partyName?.takeIf { it.isNotBlank() }
    Text(title, color = CampInk, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 28.sp)
    Text(
        listOfNotNull(role, party).joinToString(" · ").ifBlank { theme.label },
        color = theme.accent.copy(alpha = 0.85f),
        fontFamily = PlusJakartaSans,
        fontSize = 13.sp,
    )
}

@Composable
internal fun ContextChips(model: OperatingProjectionDto, theme: OperatingV12Theme) {
    val period = listOfNotNull(
        OperatingV12Date.monthYear(model.startDate),
        model.currency?.takeIf { it.isNotBlank() },
    ).joinToString(" · ").ifBlank { "Period · currency" }
    val campaign = model.title?.takeIf { it.isNotBlank() } ?: "Campaign"
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ContextChip(theme.label, filled = true, brand = theme.accent)
        ContextChip(campaign, filled = false, brand = theme.accent)
        ContextChip(period, filled = false, brand = theme.accent)
    }
}

@Composable
private fun ContextChip(text: String, filled: Boolean, brand: Color) {
    Text(
        text,
        color = if (filled) Color.White else CampSecondary,
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (filled) brand else CampCard)
            .border(1.dp, if (filled) brand else CampBorder, RoundedCornerShape(999.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

// ---------------------------------------------------------------------------------
// Pulse — MKT-PULSE-v12 / MKT-OWN-PULSE-v12
// ---------------------------------------------------------------------------------

@Composable
private fun MarketingPulse(
    model: OperatingProjectionDto,
    theme: OperatingV12Theme,
    onOpenQuickAdd: () -> Unit,
    onDecide: suspend (String, String) -> Unit,
    onOpenSheet: (MarketingBoardSheet) -> Unit,
) {
    PulseHero(model, theme, onOpenQuickAdd)
    if (model.financeVisible) {
        AttentionCard(model, theme, onDecide, onOpenQuickAdd, onOpenSheet)
        TargetBudgetCard(model, theme, onOpenSheet)
        PaymentReadinessCard(model, theme, onOpenSheet)
        FinanceCard(model, theme, onOpenSheet)
    } else {
        RestrictedPulse(model, theme)
        AttentionCard(model, theme, onDecide, onOpenQuickAdd, onOpenSheet)
    }
    ActivityCard(model, theme, onOpenQuickAdd, onOpenSheet)
}

@Composable
private fun PulseHero(model: OperatingProjectionDto, theme: OperatingV12Theme, onOpenQuickAdd: () -> Unit) {
    val identity = (model.title ?: "Campaign").uppercase(Locale.ENGLISH)
    val mode = theme.label.uppercase(Locale.ENGLISH)
    val measured = model.activity.any { it.eventType == "RESULT" || it.eventType == "PROGRESS" }
    val leads = if (measured) OperatingV12Money.compact(model.target?.progress, model.currency) else "—"
    val target = if (model.target?.configured == true) {
        OperatingV12Money.compact(model.target?.value, model.currency)
    } else {
        "—"
    }
    val budgetPct = percentLabel(model.budget?.spent, model.budget?.amount)
    val summary = if (theme.isMarketingOwn) {
        val orders = model.marketing?.orderAchievement?.let { "Orders $it%" } ?: "Orders $leads / $target"
        val roas = model.marketing?.roas?.let { "ROAS ${it}x observed" }
        listOfNotNull(orders, "Budget use $budgetPct", roas).joinToString(" · ")
    } else {
        val ar = OperatingV12Money.compact(model.finance?.outstanding, model.currency)
        "Leads $leads / $target · Budget use $budgetPct · AR $ar open"
    }
    val shape = RoundedCornerShape(22.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .shadow(16.dp, shape, clip = false, ambientColor = theme.accent.copy(alpha = 0.45f), spotColor = theme.accent.copy(alpha = 0.45f))
            .clip(shape)
            .background(Brush.linearGradient(listOf(theme.heroStart, theme.heroEnd), start = Offset(0f, 0f), end = Offset(900f, 400f)))
            .padding(20.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(end = 48.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("$identity · $mode", color = Color.White.copy(alpha = 0.85f), fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 0.6.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(theme.motionLine, color = Color.White, fontFamily = PlusJakartaSans, fontWeight = FontWeight.Black, fontSize = 26.sp, maxLines = 1)
            Text(summary, color = Color.White.copy(alpha = 0.9f), fontFamily = PlusJakartaSans, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if ("ACTIVITY" in model.quickAdds.map { it.code }) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "+  Add Activity",
                    color = theme.canvas,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .clickable(onClick = onOpenQuickAdd)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                )
            }
        }
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(theme.canvas.copy(alpha = 0.9f))
                .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.ic_mkt_mega), contentDescription = null, tint = theme.accent, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun AttentionCard(
    model: OperatingProjectionDto,
    theme: OperatingV12Theme,
    onDecide: suspend (String, String) -> Unit,
    onOpenQuickAdd: () -> Unit,
    onOpenSheet: (MarketingBoardSheet) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val overdue = model.attention.count { it.severity.equals("CRITICAL", true) || it.severity.equals("HIGH", true) || it.kind.contains("OVERDUE", true) }
    OperatingV12Card(theme = theme, fill = OperatingV12.pulseSurface) {
        SectionHead("Needs Attention", chip = if (model.attention.isNotEmpty()) "Alert" else null, chipTone = CampDanger, onViewAll = { onOpenSheet(MarketingBoardSheet.Attention) })
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OperatingV12Tile("Alerts", model.attention.size.toString(), tone = CampDanger, modifier = Modifier.weight(1f))
            OperatingV12Tile("Overdue", overdue.toString(), tone = CampWarn, modifier = Modifier.weight(1f))
        }
        if (model.attention.isEmpty()) {
            Quiet("Nothing is waiting", "Approvals, overdue items, and blockers will show here.")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                model.attention.take(3).forEach { item ->
                    AttentionRow(item, model, theme, onOpenQuickAdd) { id, decision ->
                        scope.launch { onDecide(id, decision) }
                    }
                }
            }
        }
        OperatingV12Why(
            model.pulseGroup("ATTENTION")?.why
                ?: "Each alert links to the Quick Add that resolves it. Unmapped results stay Unavailable — never invent 0.",
            tone = theme.accent,
        )
    }
}

@Composable
private fun AttentionRow(
    item: OperatingAttentionDto,
    model: OperatingProjectionDto,
    theme: OperatingV12Theme,
    onOpenQuickAdd: () -> Unit,
    onDecide: (String, String) -> Unit,
) {
    val severity = severityLabel(item)
    val severityTone = severityColor(item)
    val canDecide = (item.kind == "APPROVAL" || item.kind == "ATT-03" || item.cta == "approval") &&
        (model.role == "OWNER_ADMIN" || model.role == "APPROVER")
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.Black.copy(alpha = 0.28f))
            .border(1.dp, CampBorder.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
            .clickable {
                if (item.id.startsWith("open-")) {
                    val payment = model.paymentReadiness.firstOrNull { it.id == item.id.removePrefix("open-") }
                    if (payment != null) OperatingChromeState.settling = payment
                    else openOperatingAttention(item, model, onOpenQuickAdd)
                } else {
                    openOperatingAttention(item, model, onOpenQuickAdd)
                }
            }
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(item.title, color = CampInk, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
            SeverityChip(severity, severityTone)
        }
        val meta = listOfNotNull(
            item.counterpartyName?.takeIf { it.isNotBlank() },
            OperatingV12Date.short(item.dueAt)?.let { "Due $it" },
            if (model.financeVisible) item.amount?.takeIf { it.isNotBlank() }?.let { OperatingV12Money.compact(it, model.currency) } else null,
        ).joinToString(" · ")
        if (meta.isNotBlank()) Text(meta, color = CampSecondary, fontFamily = PlusJakartaSans, fontSize = 12.sp)
        if (canDecide) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Approve", color = CampSuccess, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { onDecide(item.id, "APPROVED") })
                Text("Reject", color = CampDanger, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { onDecide(item.id, "REJECTED") })
            }
        } else if (item.id.startsWith("open-")) {
            Text("Settle", color = theme.accent, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        } else if (!item.cta.isNullOrBlank()) {
            Text(operatingAttentionCtaLabel(item), color = theme.accent, fontFamily = PlusJakartaSans, fontSize = 13.sp)
        }
    }
}

@Composable
private fun TargetBudgetCard(model: OperatingProjectionDto, theme: OperatingV12Theme, onOpenSheet: (MarketingBoardSheet) -> Unit) {
    val target = model.target
    val budget = model.budget
    val configured = target?.configured == true
    val progress = OperatingV12Money.number(target?.progress) ?: 0.0
    val goal = OperatingV12Money.number(target?.value) ?: 0.0
    val fraction = if (goal > 0) progress / goal else 0.0
    val pct = if (goal > 0) "${(fraction * 100).roundToInt()}%" else "—"
    val behind = goal > 0 && fraction < 0.75
    val achieved = OperatingV12Money.compact(target?.progress, model.currency)
    val targetLabel = if (configured) OperatingV12Money.compact(target?.value, model.currency) else "not set"
    val noun = if (theme.isMarketingOwn) "Orders" else "Leads"
    OperatingV12Card(theme = theme, fill = OperatingV12.pulseSurface) {
        SectionHead("Target & Budget", onViewAll = { onOpenSheet(MarketingBoardSheet.TargetBudget) })
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(theme.canvas)
                .border(1.dp, theme.accent.copy(alpha = 0.22f), RoundedCornerShape(16.dp))
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OperatingV12Ring(fraction = fraction, caption = pct, note = "of target", accent = theme.accent)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("ACHIEVEMENT VS TARGET", color = theme.accent, fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 0.6.sp)
                Text(
                    if (configured && goal > 0) "$noun $achieved of $targetLabel" else "$noun $achieved · no target set",
                    color = Color.White,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 2,
                )
                StatusPill(
                    if (!configured || goal <= 0) "● Unavailable" else if (behind) "● Behind pace" else "● On track",
                    if (!configured || goal <= 0) CampWarn else if (behind) CampWarn else CampSuccess,
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OperatingV12Tile("Budget use", percentLabel(budget?.spent, budget?.amount), tone = theme.accent, modifier = Modifier.weight(1f))
            OperatingV12Tile(noun, if (configured) "$achieved/$targetLabel" else achieved, modifier = Modifier.weight(1f))
        }
        MetricLine(if (theme.isMarketingOwn) "Orders (attributed)" else "Qualified leads", "Target $targetLabel", achieved, theme.accent)
        MetricLine(if (theme.isMarketingOwn) "Own marketing budget" else "Budget", model.marketing?.pacing ?: "Effective plan", OperatingV12Money.compact(budget?.amount, model.currency), CampInk)
        MetricLine("Eligible spend", "Accepted spend · vendor costs", OperatingV12Money.compact(model.finance?.eligibleCost ?: budget?.spent, model.currency), CampInk)
        listOfNotNull(
            model.marketing?.cpl?.let { "CPL $it" },
            model.marketing?.orderAchievement?.let { "Orders $it%" },
            model.marketing?.salesAchievement?.let { "Attributed sales $it%" },
            model.marketing?.roas?.let { "ROAS ${it}x" },
        ).takeIf { it.isNotEmpty() }?.let { chips ->
            Text(chips.joinToString(" · "), color = CampSecondary, fontFamily = PlusJakartaSans, fontSize = 12.sp)
        }
        OperatingV12Why(
            model.pulseGroup("TARGET_BUDGET")?.why
                ?: "Achievement = accepted results ÷ effective target. Empty ≠ zero — coverage noted when attribution is partial.",
            tone = theme.accent,
        )
    }
}

@Composable
private fun PaymentReadinessCard(model: OperatingProjectionDto, theme: OperatingV12Theme, onOpenSheet: (MarketingBoardSheet) -> Unit) {
    val client = isClientCampaign(model.subtype)
    val toCollect = if (client) model.paymentReadiness.filter { it.relevance == "RECEIVABLE" } else emptyList()
    val toPay = model.paymentReadiness.filter { it.relevance != "RECEIVABLE" }
    val ap = toPay.mapNotNull { OperatingV12Money.number(it.amount) }.sum()
    val ar = toCollect.mapNotNull { OperatingV12Money.number(it.amount) }.sum()
    val paid = OperatingV12Money.number(model.finance?.cashPaid) ?: 0.0
    OperatingV12Card(theme = theme, fill = OperatingV12.pulseSurface) {
        SectionHead("Payment Readiness", onViewAll = { onOpenSheet(MarketingBoardSheet.Payment) })
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OperatingV12Tile(
                if (client) "Vendor dues (AP)" else "Vendor due (AP)",
                OperatingV12Money.compact(ap, model.currency),
                tone = CampDanger,
                modifier = Modifier.weight(1f),
            )
            if (client) {
                OperatingV12Tile("Client AR due", OperatingV12Money.compact(ar, model.currency), tone = CampDanger, modifier = Modifier.weight(1f))
            } else {
                OperatingV12Tile("Paid", OperatingV12Money.compact(paid, model.currency), tone = CampSuccess, modifier = Modifier.weight(1f))
            }
        }
        val rows = (toPay + toCollect).take(3)
        if (rows.isEmpty()) {
            Quiet("No open payments", if (client) "Vendor payables and client collections will show here." else "Vendor payables waiting to settle will show here.")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                rows.forEach { row -> PaymentRow(row, model, theme) }
            }
        }
        OperatingV12Why(
            model.pulseGroup("PAYMENT_READINESS")?.why
                ?: if (client) "Spend ≠ payment · Invoice ≠ collection. Settle opens the matching obligation."
                else "Own Business: spend vs payment only — no client invoice or AR.",
            tone = theme.accent,
        )
    }
}

@Composable
private fun PaymentRow(row: OperatingPaymentDto, model: OperatingProjectionDto, theme: OperatingV12Theme) {
    val overdue = row.dueBucket.equals("OVERDUE", true)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.24f))
            .clickable { OperatingChromeState.settling = row }
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(row.title.ifBlank { row.counterpartyName ?: "Open item" }, color = CampInk, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                listOfNotNull(row.counterpartyName?.takeIf { it.isNotBlank() }, OperatingV12Date.short(row.dueAt)?.let { "Due $it" }, row.status.takeIf { it.isNotBlank() }).joinToString(" · "),
                color = if (overdue) CampDanger else CampSecondary,
                fontFamily = PlusJakartaSans,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            OperatingV12Money.compact(row.amount, model.currency),
            color = if (overdue) CampDanger else theme.accent,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
        )
    }
}

@Composable
private fun FinanceCard(model: OperatingProjectionDto, theme: OperatingV12Theme, onOpenSheet: (MarketingBoardSheet) -> Unit) {
    val client = isClientCampaign(model.subtype)
    val finance = model.finance
    val measured = model.activity.any { it.eventType == "RESULT" || it.eventType == "PROGRESS" }
    OperatingV12Card(theme = theme, fill = OperatingV12.pulseSurface) {
        SectionHead("Finance", onViewAll = { onOpenSheet(MarketingBoardSheet.Finance) })
        if (client) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OperatingV12Tile("Invoiced", OperatingV12Money.compact(finance?.sales, model.currency), modifier = Modifier.weight(1f))
                OperatingV12Tile("Collected", OperatingV12Money.compact(finance?.received, model.currency), tone = CampSuccess, modifier = Modifier.weight(1f))
                OperatingV12Tile("AR", OperatingV12Money.compact(finance?.outstanding, model.currency), tone = CampDanger, modifier = Modifier.weight(1f))
            }
            MetricLine("Client invoices", "Posted invoices", OperatingV12Money.compact(finance?.sales, model.currency), theme.accent)
            MetricLine("Received", "Collections allocated", OperatingV12Money.compact(finance?.received, model.currency), CampSuccess)
            MetricLine("Still to collect", "Open AR", OperatingV12Money.compact(finance?.outstanding, model.currency), CampWarn)
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OperatingV12Tile("Spend", OperatingV12Money.compact(finance?.eligibleCost ?: model.budget?.spent, model.currency), modifier = Modifier.weight(1f))
                OperatingV12Tile("Paid", OperatingV12Money.compact(finance?.cashPaid, model.currency), tone = CampSuccess, modifier = Modifier.weight(1f))
                OperatingV12Tile("AP", OperatingV12Money.compact(finance?.payable, model.currency), tone = theme.accent, modifier = Modifier.weight(1f))
            }
            MetricLine(
                "Attributed sales (observed)",
                "Not ledger revenue",
                model.marketing?.salesAchievement?.let { "$it%" }
                    ?: if (measured) OperatingV12Money.compact(model.target?.progress, model.currency) else "—",
                CampSuccess,
            )
            MetricLine("ROAS (observational)", "Attributed sales ÷ eligible spend", model.marketing?.roas?.let { "${it}x" } ?: "—", CampInk)
            MetricLine("Orders", "Attributed", model.marketing?.orderAchievement?.let { "$it%" } ?: OperatingV12Money.compact(model.target?.progress, model.currency), CampInk)
        }
        MetricLine("Eligible cost", "Spend · vendor costs", OperatingV12Money.compact(finance?.eligibleCost, model.currency), CampInk)
        MetricLine("Business cash paid", "Settled AP", OperatingV12Money.compact(finance?.cashPaid, model.currency), CampInk)
        MetricLine("Still to pay", "Open AP", OperatingV12Money.compact(finance?.payable, model.currency), CampWarn)
        OperatingV12Why(
            model.pulseGroup("FINANCE")?.why
                ?: if (client) "Posted invoice creates AR. Collection allocates — never new revenue. Accepted results ≠ money collected."
                else "Own Business: no client invoice, collection or AR. Attributed sales are observational — never booked as income.",
            tone = theme.accent,
        )
    }
}

@Composable
private fun ActivityCard(
    model: OperatingProjectionDto,
    theme: OperatingV12Theme,
    onOpenQuickAdd: () -> Unit,
    onOpenSheet: (MarketingBoardSheet) -> Unit,
) {
    val open = model.activity.count { it.approvalStatus == "PENDING" || it.status in setOf("IN_PROGRESS", "PLANNED", "OPEN") }
    val done = model.activity.count {
        it.approvalStatus == "APPROVED" || it.status in setOf("DONE", "COMPLETED", "SETTLED") ||
            it.paymentStatus in setOf("COLLECTED", "PAID", "SETTLED")
    }
    val today = LocalDate.now().toString()
    val todayCount = model.activity.count { it.occurredAt.startsWith(today) }
    OperatingV12Card(theme = theme, fill = OperatingV12.pulseSurface) {
        SectionHead("Activity", onViewAll = { onOpenSheet(MarketingBoardSheet.Activity) })
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OperatingV12Tile("Open", open.toString(), modifier = Modifier.weight(1f))
            OperatingV12Tile("Done", done.toString(), tone = CampSuccess, modifier = Modifier.weight(1f))
            OperatingV12Tile("Events today", todayCount.toString(), tone = CampDanger, modifier = Modifier.weight(1f))
        }
        if (model.activity.isEmpty()) {
            BusinessEmptyState(
                title = "Nothing recorded yet",
                body = "Campaigns, spend, and results will show here.",
                accent = theme.accent,
                glyph = "📌",
                actionLabel = "Use quick add",
                onAction = onOpenQuickAdd,
            )
        } else {
            BusinessActivityList(
                rows = model.activity.take(5),
                model = model,
                accent = theme.accent,
                onEdit = { OperatingChromeState.editing = it },
            )
        }
        OperatingV12Why(
            model.pulseGroup("ACTIVITY")?.why
                ?: "Progress increments toward the target only when accepted. Status-only updates never invent results.",
            tone = theme.accent,
        )
    }
}

@Composable
private fun RestrictedPulse(model: OperatingProjectionDto, theme: OperatingV12Theme) {
    OperatingV12Card(theme = theme, fill = OperatingV12.pulseSurface) {
        Text(model.title ?: "Campaign", color = CampInk, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
        val channels = model.life?.channels?.joinToString(" · ").orEmpty()
        if (channels.isNotBlank()) Text("Channels · $channels", color = CampSecondary, fontFamily = PlusJakartaSans, fontSize = 13.sp)
        if (!model.partyName.isNullOrBlank()) Text(model.partyName!!, color = CampSecondary, fontFamily = PlusJakartaSans, fontSize = 13.sp)
        Text("Financial totals stay hidden in this view.", color = CampSecondary, fontFamily = PlusJakartaSans, fontSize = 12.sp)
    }
}

// ---------------------------------------------------------------------------------
// Moments — MKT-MOMENTS-v12
// ---------------------------------------------------------------------------------

@Composable
private fun MarketingMoments(
    model: OperatingProjectionDto,
    theme: OperatingV12Theme,
    onOpenQuickAdd: () -> Unit,
    onOpenSheet: (MarketingBoardSheet) -> Unit,
) {
    val moments = model.boards?.moments.orEmpty()
    MomentsHero(model, theme)
    BoardSectionCard(
        title = "Open Commitments",
        section = moments.firstOrNull { it.code == "M4" },
        model = model,
        theme = theme,
        chips = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OperatingV12Tile("AP", OperatingV12Money.compact(model.finance?.payable, model.currency), tone = CampWarn, modifier = Modifier.weight(1f))
                if (isClientCampaign(model.subtype)) {
                    OperatingV12Tile("AR", OperatingV12Money.compact(model.finance?.outstanding, model.currency), tone = CampDanger, modifier = Modifier.weight(1f))
                } else {
                    OperatingV12Tile("Open", (model.life?.openCommitments ?: moments.firstOrNull { it.code == "M4" }?.rows?.size ?: 0).toString(), modifier = Modifier.weight(1f))
                }
            }
        },
        why = "Open payables and receivables are tracked separately — spend vs payment, invoice vs collection.",
        onViewAll = { onOpenSheet(MarketingBoardSheet.Commitments) },
        fallbackEmpty = "Orders, payables and approvals will show here.",
    )
    BoardSectionCard(
        title = "Campaign / Business Moment",
        section = moments.firstOrNull { it.code == "M1" },
        model = model,
        theme = theme,
        chips = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OperatingV12Tile("Status", if (model.retired) "Closed" else "Live", tone = if (model.retired) CampSecondary else CampSuccess, modifier = Modifier.weight(1f))
                OperatingV12Tile("Day", dayOfPeriod(model), modifier = Modifier.weight(1f))
                OperatingV12Tile("Mode", if (theme.isMarketingOwn) "Own" else "Client", tone = theme.accent, modifier = Modifier.weight(1f))
            }
        },
        why = "Quick Adds and analytics depend on the business moment setup.",
        onViewAll = { onOpenSheet(MarketingBoardSheet.Campaign) },
        fallbackEmpty = "Campaign identity and plan will show here.",
    )
    BoardSectionCard(
        title = "People",
        section = moments.firstOrNull { it.code == "M2" },
        model = model,
        theme = theme,
        chips = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OperatingV12Tile("Team", model.participants.size.toString(), modifier = Modifier.weight(1f))
                OperatingV12Tile(
                    "Approvers",
                    if (model.role == "OWNER_ADMIN" || model.role == "APPROVER") "1+" else "—",
                    tone = theme.accent,
                    modifier = Modifier.weight(1f),
                )
            }
        },
        why = "Approvers cannot approve their own controlled action.",
        onViewAll = { onOpenSheet(MarketingBoardSheet.People) },
        fallbackEmpty = "Invite owners, leads and approvers.",
        preferParticipants = true,
    )
    BoardSectionCard(
        title = "Channels & Network",
        section = moments.firstOrNull { it.code == "M3" },
        model = model,
        theme = theme,
        chips = {
            val channels = model.life?.channels.orEmpty()
            val vendors = model.masters.orEmpty().count { it.kind in setOf("VENDOR", "SUPPLIER", "PARTNER", "CREATOR") }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OperatingV12Tile("Channels", channels.size.toString(), modifier = Modifier.weight(1f))
                OperatingV12Tile("Vendors", vendors.toString(), modifier = Modifier.weight(1f))
                if (isClientCampaign(model.subtype)) {
                    OperatingV12Tile("Client", if (model.partyName.isNullOrBlank()) "0" else "1", modifier = Modifier.weight(1f))
                }
            }
        },
        why = "Channel metrics need attribution coverage. Empty coverage shows Unavailable — never averaged as zero.",
        onViewAll = { onOpenSheet(MarketingBoardSheet.Channels) },
        fallbackEmpty = "Channels, vendors and partners will show here.",
    )
    OperatingV12Card(theme = theme, fill = OperatingV12.pulseSurface) {
        Text("Create the next campaign moment", color = CampInk, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        Text(
            "Open Quick Add",
            color = theme.canvas,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(theme.accent)
                .clickable(onClick = onOpenQuickAdd)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}

@Composable
private fun MomentsHero(model: OperatingProjectionDto, theme: OperatingV12Theme) {
    OperatingV12Card(theme = theme, fill = OperatingV12.surface, borderOpacity = 0.4f) {
        Text(model.title ?: "Campaign", color = Color.White, fontFamily = PlusJakartaSans, fontWeight = FontWeight.Black, fontSize = 24.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusPill(theme.label, theme.accent)
            StatusPill(if (model.retired) "● Closed" else "● Campaign live", if (model.retired) CampSecondary else CampSuccess)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OperatingV12Tile("People · Team", model.participants.size.toString(), modifier = Modifier.weight(1f))
            OperatingV12Tile("Channels", (model.life?.channels?.size ?: 0).toString(), modifier = Modifier.weight(1f))
            OperatingV12Tile("AP open", OperatingV12Money.compact(model.finance?.payable, model.currency), tone = CampWarn, modifier = Modifier.weight(1f))
        }
    }
}

// ---------------------------------------------------------------------------------
// Life — MKT-LIFE-v12
// ---------------------------------------------------------------------------------

@Composable
private fun MarketingLife(model: OperatingProjectionDto, theme: OperatingV12Theme, onOpenSheet: (MarketingBoardSheet) -> Unit) {
    val life = model.boards?.life.orEmpty()
    OperatingV12Card(theme = theme, fill = OperatingV12.surface, borderOpacity = 0.4f) {
        Text("TODAY", color = theme.accent, fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 1.sp)
        Text("${model.title ?: "Campaign"} · today", color = Color.White, fontFamily = PlusJakartaSans, fontWeight = FontWeight.Black, fontSize = 22.sp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OperatingV12Tile("Open", (model.life?.openCommitments ?: 0).toString(), tone = theme.accent, modifier = Modifier.weight(1f))
            OperatingV12Tile(
                "Done",
                model.activity.count { it.status in setOf("DONE", "COMPLETED") || it.paymentStatus in setOf("COLLECTED", "PAID", "SETTLED") }.toString(),
                tone = CampSuccess,
                modifier = Modifier.weight(1f),
            )
            OperatingV12Tile(
                if (isClientCampaign(model.subtype)) "Collected" else "Paid",
                OperatingV12Money.compact(if (isClientCampaign(model.subtype)) model.finance?.received else model.finance?.cashPaid, model.currency),
                modifier = Modifier.weight(1f),
            )
        }
    }
    BoardSectionCard("Current Campaign", life.firstOrNull { it.code == "L1" }, model, theme, why = "Today shows accepted results and open activities only.", onViewAll = { onOpenSheet(MarketingBoardSheet.LifeCurrent) })
    BoardSectionCard("Channel Activity", life.firstOrNull { it.code == "L2" }, model, theme, why = "Channel progress needs coverage — Unavailable when unmapped.", onViewAll = { onOpenSheet(MarketingBoardSheet.LifeChannels) })
    BoardSectionCard(
        "Money Moving",
        life.firstOrNull { it.code == "L3" },
        model,
        theme,
        why = if (isClientCampaign(model.subtype)) "Collection/payment allocate — never repost invoice or spend."
        else "Own Business: payments settle AP only — no client AR.",
        onViewAll = { onOpenSheet(MarketingBoardSheet.LifeMoney) },
    )
    BoardSectionCard("People Today", life.firstOrNull { it.code == "L5" } ?: life.firstOrNull { it.title.contains("People", true) }, model, theme, why = "People rows reflect posted activity owners today.", onViewAll = { onOpenSheet(MarketingBoardSheet.LifePeople) })
    if (life.isEmpty()) {
        Quiet("Nothing recorded yet", "Board updates and the live position will show here.")
    }
}

// ---------------------------------------------------------------------------------
// Memory — MKT-MEMORY-v12
// ---------------------------------------------------------------------------------

@Composable
private fun MarketingMemory(
    model: OperatingProjectionDto,
    theme: OperatingV12Theme,
    momentId: String?,
    onOpenSheet: (MarketingBoardSheet) -> Unit,
) {
    val memory = model.boards?.memory.orEmpty()
    val period = OperatingV12Date.monthYear(model.startDate) ?: periodLabel(model.startDate)
    OperatingV12Card(theme = theme, fill = OperatingV12.surface, borderOpacity = 0.4f) {
        Text("${model.title ?: "Campaign"} · memory", color = Color.White, fontFamily = PlusJakartaSans, fontWeight = FontWeight.Black, fontSize = 22.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PeriodPill(period, selected = true, brand = theme.accent)
            PeriodPill("Current", selected = false, brand = theme.accent)
            PeriodPill("Prior", selected = false, brand = theme.accent)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val progress = if (model.target?.configured == true) {
                val p = OperatingV12Money.compact(model.target?.progress, model.currency)
                val t = OperatingV12Money.compact(model.target?.value, model.currency)
                "$p/$t"
            } else {
                "—"
            }
            OperatingV12Tile(if (theme.isMarketingOwn) "Orders" else "Leads", progress, modifier = Modifier.weight(1f))
            OperatingV12Tile("vs last period", lastPeriodDelta(model), tone = CampWarn, modifier = Modifier.weight(1f))
            OperatingV12Tile("Budget", OperatingV12Money.compact(model.budget?.amount, model.currency), tone = theme.accent, modifier = Modifier.weight(1f))
        }
    }
    BoardSectionCard("Monthly Memory", memory.firstOrNull { it.code == "ME1" }, model, theme, why = "Months with missing coverage show Unavailable — never averaged as zero.", onViewAll = { onOpenSheet(MarketingBoardSheet.MemoryMonthly) })
    BoardSectionCard(
        "Money Memory",
        memory.firstOrNull { it.code == "ME2" },
        model,
        theme,
        why = "Revisions are versioned with effective dates; memory never overwrites the prior plan.",
        onViewAll = { onOpenSheet(MarketingBoardSheet.MemoryMoney) },
    )
    BoardSectionCard("People & Activity", memory.firstOrNull { it.code == "ME3" }, model, theme, why = "On-time rates use posted activity with due dates.", onViewAll = { onOpenSheet(MarketingBoardSheet.MemoryPeople) })
    BoardSectionCard("Patterns & Decisions", memory.firstOrNull { it.code == "ME4" }, model, theme, why = "Approval ≠ disbursement. Attributed sales stay observational.", onViewAll = { onOpenSheet(MarketingBoardSheet.MemoryPatterns) })
    StoryCta(theme, period) {
        if (momentId != null) OperatingChromeState.previewStoryId = momentId
    }
}

@Composable
private fun StoryCta(theme: OperatingV12Theme, month: String, onTap: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .shadow(10.dp, shape, clip = false, ambientColor = theme.accent.copy(alpha = 0.4f), spotColor = theme.accent.copy(alpha = 0.4f))
            .clip(shape)
            .background(Brush.linearGradient(listOf(theme.heroStart, theme.heroEnd)))
            .clickable(onClick = onTap)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text("Generate Business Story", color = Color.White, fontFamily = PlusJakartaSans, fontWeight = FontWeight.Black, fontSize = 16.sp)
        Text(
            "Eight pages from recorded activity · $month preselected",
            color = Color.White.copy(alpha = 0.85f),
            fontFamily = PlusJakartaSans,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ---------------------------------------------------------------------------------
// Shared board / chrome
// ---------------------------------------------------------------------------------

@Composable
private fun BoardSectionCard(
    title: String,
    section: OperatingBoardSectionDto?,
    model: OperatingProjectionDto,
    theme: OperatingV12Theme,
    why: String,
    onViewAll: () -> Unit,
    chips: (@Composable () -> Unit)? = null,
    fallbackEmpty: String = "Nothing recorded yet.",
    preferParticipants: Boolean = false,
) {
    OperatingV12Card(theme = theme, fill = OperatingV12.pulseSurface) {
        SectionHead(title, onViewAll = onViewAll)
        chips?.invoke()
        when {
            preferParticipants && model.participants.isNotEmpty() -> {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    model.participants.take(3).forEach { name ->
                        MetricLine(name, if (name == model.partyName) roleLabel(model.role) else "Member", if (name == model.partyName) "Owner" else "Member", theme.accent)
                    }
                }
            }
            section != null && section.rows.isNotEmpty() -> {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    section.rows.take(3).forEach { row ->
                        MetricLine(
                            row.title.ifBlank { row.eventType },
                            listOfNotNull(row.channelName, row.counterpartyName, row.paymentStatus?.takeIf { it != "NONE" }).joinToString(" · ").ifBlank {
                                formatOperatingDetail(section.detail, model.currency)
                            },
                            if (model.financeVisible && !row.amount.isNullOrBlank()) formatOperatingAmount(model.currency, row.amount, true) else row.status ?: "Open",
                            theme.accent,
                        )
                    }
                }
            }
            !section?.detail.isNullOrBlank() && section!!.detail != "Nothing recorded yet." -> {
                Text(formatOperatingDetail(section.detail, model.currency), color = CampSecondary, fontFamily = PlusJakartaSans, fontSize = 13.sp)
                if (!section.coverage.isNullOrBlank()) StatusPill(section.coverage!!, CampWarn)
            }
            else -> Quiet("Nothing recorded yet", fallbackEmpty)
        }
        OperatingV12Why(section?.let { model.pulseGroup(it.code)?.why } ?: why, tone = theme.accent)
    }
}

@Composable
private fun MarketingViewAllSheet(
    sheet: MarketingBoardSheet,
    model: OperatingProjectionDto,
    theme: OperatingV12Theme,
    brand: Color,
    onClose: () -> Unit,
    onDecide: suspend (String, String) -> Unit,
    onOpenQuickAdd: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val title = when (sheet) {
        MarketingBoardSheet.Attention -> "Needs Attention"
        MarketingBoardSheet.TargetBudget -> "Target & Budget"
        MarketingBoardSheet.Payment -> "Payment Readiness"
        MarketingBoardSheet.Finance -> "Finance"
        MarketingBoardSheet.Activity -> "All activity"
        MarketingBoardSheet.Commitments -> "Open Commitments"
        MarketingBoardSheet.Campaign -> "Campaign / Business Moment"
        MarketingBoardSheet.People -> "People"
        MarketingBoardSheet.Channels -> "Channels & Network"
        MarketingBoardSheet.LifeCurrent -> "Current Campaign"
        MarketingBoardSheet.LifeChannels -> "Channel Activity"
        MarketingBoardSheet.LifeMoney -> "Money Moving"
        MarketingBoardSheet.LifePeople -> "People Today"
        MarketingBoardSheet.MemoryMonthly -> "Monthly Memory"
        MarketingBoardSheet.MemoryMoney -> "Money Memory"
        MarketingBoardSheet.MemoryPeople -> "People & Activity"
        MarketingBoardSheet.MemoryPatterns -> "Patterns & Decisions"
    }
    // Bounded sheet height (fillMaxHeight 0.92f) so this verticalScroll is not nested
    // under MarketingActive's page scroll with infinity max height.
    OperatingSheetScrim(
        onDismiss = onClose,
        accent = brand,
        surface = theme.canvas,
        border = CampBorder,
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(Modifier.fillMaxWidth().height(22.dp), horizontalArrangement = Arrangement.Center) {
                Box(
                    Modifier
                        .size(width = 40.dp, height = 5.dp)
                        .clip(RoundedCornerShape(100.dp))
                        .background(Color.White.copy(alpha = 0.2f)),
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title, color = CampInk, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 22.sp)
                Text("Close", color = CampSecondary, fontFamily = PlusJakartaSans, modifier = Modifier.clickable(onClick = onClose))
            }
            Text(model.title ?: "Marketing", color = CampSecondary, fontFamily = PlusJakartaSans, fontSize = 13.sp)
            ContextChips(model, theme)
            when (sheet) {
                MarketingBoardSheet.Attention -> {
                    if (model.attention.isEmpty()) Quiet("Nothing is waiting", "Approvals and overdue items will show here.")
                    model.attention.forEach { item ->
                        AttentionRow(item, model, theme, onOpenQuickAdd) { id, decision -> scope.launch { onDecide(id, decision) } }
                    }
                }
                MarketingBoardSheet.Activity -> {
                    if (model.activity.isEmpty()) Quiet("Nothing recorded yet", "Sales, spend, and money collection will show here.")
                    BusinessActivityList(rows = model.activity, model = model, accent = brand, onEdit = { OperatingChromeState.editing = it })
                }
                MarketingBoardSheet.Payment -> {
                    val client = isClientCampaign(model.subtype)
                    val toCollect = if (client) model.paymentReadiness.filter { it.relevance == "RECEIVABLE" } else emptyList()
                    val toPay = model.paymentReadiness.filter { it.relevance != "RECEIVABLE" }
                    if (client && toCollect.isNotEmpty()) {
                        Text("To collect", color = brand, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold)
                        toCollect.forEach { PaymentRow(it, model, theme) }
                    }
                    if (toPay.isNotEmpty()) {
                        Text("To pay", color = brand, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold)
                        toPay.forEach { PaymentRow(it, model, theme) }
                    }
                    if (model.paymentReadiness.isEmpty()) Quiet("No open payments", "Nothing waiting to settle.")
                }
                MarketingBoardSheet.Finance -> FinanceCard(model, theme, onOpenSheet = {})
                MarketingBoardSheet.TargetBudget -> TargetBudgetCard(model, theme, onOpenSheet = {})
                else -> {
                    val section = boardSectionFor(sheet, model)
                    if (section == null || (section.rows.isEmpty() && section.detail.isBlank())) {
                        Quiet("Nothing recorded yet", "Details will show here when posted.")
                    } else {
                        if (section.detail.isNotBlank()) {
                            Text(formatOperatingDetail(section.detail, model.currency), color = CampSecondary, fontFamily = PlusJakartaSans, fontSize = 13.sp)
                        }
                        section.rows.forEach { row ->
                            MetricLine(
                                row.title.ifBlank { row.eventType },
                                listOfNotNull(row.channelName, row.counterpartyName).joinToString(" · "),
                                if (model.financeVisible && !row.amount.isNullOrBlank()) formatOperatingAmount(model.currency, row.amount, true) else "Open",
                                brand,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

private fun boardSectionFor(sheet: MarketingBoardSheet, model: OperatingProjectionDto): OperatingBoardSectionDto? {
    val moments = model.boards?.moments.orEmpty()
    val life = model.boards?.life.orEmpty()
    val memory = model.boards?.memory.orEmpty()
    return when (sheet) {
        MarketingBoardSheet.Commitments -> moments.firstOrNull { it.code == "M4" }
        MarketingBoardSheet.Campaign -> moments.firstOrNull { it.code == "M1" }
        MarketingBoardSheet.People -> moments.firstOrNull { it.code == "M2" }
        MarketingBoardSheet.Channels -> moments.firstOrNull { it.code == "M3" }
        MarketingBoardSheet.LifeCurrent -> life.firstOrNull { it.code == "L1" }
        MarketingBoardSheet.LifeChannels -> life.firstOrNull { it.code == "L2" }
        MarketingBoardSheet.LifeMoney -> life.firstOrNull { it.code == "L3" }
        MarketingBoardSheet.LifePeople -> life.firstOrNull { it.code == "L5" } ?: life.firstOrNull { it.title.contains("People", true) }
        MarketingBoardSheet.MemoryMonthly -> memory.firstOrNull { it.code == "ME1" }
        MarketingBoardSheet.MemoryMoney -> memory.firstOrNull { it.code == "ME2" }
        MarketingBoardSheet.MemoryPeople -> memory.firstOrNull { it.code == "ME3" }
        MarketingBoardSheet.MemoryPatterns -> memory.firstOrNull { it.code == "ME4" }
        else -> null
    }
}

@Composable
private fun SectionHead(title: String, chip: String? = null, chipTone: Color = CampDanger, onViewAll: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, color = CampInk, fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
        if (chip != null) SeverityChip(chip, chipTone)
        if (onViewAll != null) {
            Text("View all ›", color = OperatingV12.viewAll, fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.clickable(onClick = onViewAll))
        }
    }
}

@Composable
private fun MetricLine(title: String, caption: String, trailing: String, tone: Color) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, color = CampInk, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (caption.isNotBlank()) Text(caption, color = CampSecondary, fontFamily = PlusJakartaSans, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(trailing, color = tone, fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
private fun SeverityChip(label: String, tone: Color) {
    Text(
        label,
        color = tone,
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Bold,
        fontSize = 10.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(tone.copy(alpha = 0.16f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

@Composable
private fun StatusPill(label: String, tone: Color) {
    Text(
        label,
        color = tone,
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Bold,
        fontSize = 10.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(tone.copy(alpha = 0.16f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

@Composable
private fun PeriodPill(label: String, selected: Boolean, brand: Color) {
    Text(
        label,
        color = if (selected) Color.White else CampSecondary,
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) brand else CampCard)
            .border(1.dp, if (selected) brand else CampBorder, RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}

@Composable
private fun Quiet(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, color = CampInk, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Text(body, color = CampSecondary, fontFamily = PlusJakartaSans, fontSize = 12.sp)
    }
}

private fun severityLabel(item: OperatingAttentionDto): String = when {
    item.severity.equals("CRITICAL", true) || item.kind.contains("OVERDUE", true) -> "Overdue"
    item.severity.equals("HIGH", true) -> "Alert"
    item.severity.equals("MEDIUM", true) -> "Unavailable"
    item.cta == "approval" || item.kind == "ATT-03" || item.kind == "APPROVAL" -> "Alert"
    else -> item.severity?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "Open"
}

private fun severityColor(item: OperatingAttentionDto): Color = when (severityLabel(item)) {
    "Overdue" -> CampDanger
    "Alert" -> CampDanger
    "Unavailable" -> CampWarn
    else -> CampSecondary
}

private fun percentLabel(part: String?, whole: String?): String {
    val p = part?.toDoubleOrNull() ?: return "—"
    val w = whole?.toDoubleOrNull() ?: return "—"
    if (w <= 0.0) return "—"
    return String.format(Locale.US, "%.1f%%", (p / w) * 100.0)
}

private fun periodLabel(start: String?): String {
    val date = start?.let { runCatching { LocalDate.parse(it.take(10)) }.getOrNull() } ?: LocalDate.now()
    return date.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH))
}

private fun roleLabel(code: String?): String =
    OperatingMomentCatalog.roles.firstOrNull { it.first == code }?.second ?: "Marketing"

private fun dayOfPeriod(model: OperatingProjectionDto): String {
    val start = model.startDate?.take(10)?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return "—"
    val end = model.endDate?.take(10)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    val today = LocalDate.now()
    if (today < start) return "0"
    val day = java.time.temporal.ChronoUnit.DAYS.between(start, today).toInt() + 1
    val total = end?.let { java.time.temporal.ChronoUnit.DAYS.between(start, it).toInt() + 1 }
    return if (total != null && total > 0) "$day/$total" else day.toString()
}

private fun lastPeriodDelta(model: OperatingProjectionDto): String {
    val monthly = model.boards?.memory.orEmpty().firstOrNull { it.code == "ME1" }?.rows.orEmpty()
    if (monthly.size < 2) return "—"
    val current = monthly[0].amount?.toBigDecimalOrNull() ?: return "—"
    val previous = monthly[1].amount?.toBigDecimalOrNull() ?: return "—"
    if (previous.signum() <= 0) return "—"
    val delta = (current - previous).divide(previous, 4, java.math.RoundingMode.HALF_UP)
        .multiply(java.math.BigDecimal(100))
        .setScale(0, java.math.RoundingMode.HALF_UP)
    val sign = if (delta.signum() < 0) "−" else "+"
    return "$sign${delta.abs().toPlainString()}%"
}

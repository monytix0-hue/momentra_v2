package com.example.momentra.ui.shell.group.purchase

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.api.ActivityItemDto
import com.example.momentra.data.api.AnalyticsInsightItemDto
import com.example.momentra.data.api.GroupFinancePayloadDto
import com.example.momentra.data.api.GroupLifePlanningItemDto
import com.example.momentra.data.api.GroupParticipantDto
import com.example.momentra.data.api.GroupPulsePayloadDto
import com.example.momentra.data.repository.GroupSliceRepository
import com.example.momentra.data.security.BalanceMask
import com.example.momentra.data.security.SecurityPreferences
import com.example.momentra.ui.shell.group.GroupActiveLoading
import com.example.momentra.ui.shell.group.GroupFinanceFormat
import com.example.momentra.ui.shell.group.GroupPulseInsightsHeroCard
import com.example.momentra.ui.shell.group.GroupTabDataCache
import com.example.momentra.ui.shell.group.loadGroupPulseTab
import com.example.momentra.ui.theme.PlusJakartaSans
import java.math.BigDecimal
import java.math.RoundingMode

/** Figma 601:12707 — Shared Purchase Pulse (Gift Pool layout). Live APIs only. */
@Composable
fun PurchasePulseActiveContent(
    theme: PurchaseActiveTheme,
    momentId: String?,
    momentTitle: String?,
    refreshToken: Long,
    onAddExpense: () -> Unit,
    onOpenQuickAdd: () -> Unit = onAddExpense,
    onViewSplits: () -> Unit = onAddExpense,
    onOpenFinance: () -> Unit = onViewSplits,
    onQuickAddKind: (PurchaseQuickAddKind) -> Unit = {},
    repository: GroupSliceRepository = remember { GroupSliceRepository() },
    modifier: Modifier = Modifier,
) {
    var loading by remember { mutableStateOf(true) }
    var pulse by remember { mutableStateOf<GroupPulsePayloadDto?>(null) }
    var finance by remember { mutableStateOf<GroupFinancePayloadDto?>(null) }
    var activities by remember { mutableStateOf<List<ActivityItemDto>>(emptyList()) }
    var participants by remember { mutableStateOf<List<GroupParticipantDto>>(emptyList()) }
    var planningItems by remember { mutableStateOf<List<GroupLifePlanningItemDto>>(emptyList()) }
    var insights by remember { mutableStateOf<List<AnalyticsInsightItemDto>>(emptyList()) }
    var title by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(refreshToken, momentId) {
        if (momentId.isNullOrBlank()) {
            loading = false
            return@LaunchedEffect
        }
        error = null
        GroupTabDataCache.peekPulse(momentId)?.let { cached ->
            title = cached.title
            pulse = cached.pulse
            finance = cached.finance
            activities = cached.activities
            insights = cached.insights
            loading = false
        } ?: run { loading = true }
        loadGroupPulseTab(repository, momentId).fold(
            onSuccess = { data ->
                title = data.title
                pulse = data.pulse
                finance = data.finance
                activities = data.activities
                insights = data.insights
                loading = false
            },
            onFailure = { e ->
                error = e.message
                loading = false
            },
        )
        repository.getParticipants(momentId).onSuccess { participants = it.participants }
        repository.listPlanningItems(momentId).onSuccess { planningItems = it.items }
            .onFailure {
                repository.getLife(momentId).onSuccess { facet ->
                    planningItems = facet.payload?.planningItems.orEmpty()
                }
            }
    }

    if (loading && pulse == null && finance == null) {
        GroupActiveLoading(modifier.fillMaxSize())
        return
    }

    val primaryTotal = finance?.totals?.firstOrNull()
    val currency = primaryTotal?.currencyCode ?: "INR"
    val budgetTotal = primaryTotal?.budgetTotal
    val contributionTotal = primaryTotal?.contributionTotal
    val participantCount = pulse?.participantCount ?: participants.size
    val hideBalances = SecurityPreferences(LocalContext.current).hideBalances()
    val displayTitle = momentTitle ?: title ?: theme.pulseTitle
    val openTasks = pulse?.openTaskCount ?: 0
    val fundedPercent = PurchasePulseMath.fundedPercent(contributionTotal, budgetTotal)
    val positions = finance?.positions.orEmpty()
    val nameById = participants.associateBy({ it.participantId }, { it.displayName ?: it.participantId.take(8) })
    val deadlines = planningItems.filter { !it.dueAt.isNullOrBlank() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(theme.bg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .padding(bottom = 56.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        error?.let {
            Text(it, color = Color(0xFFF87171), fontSize = 12.sp, fontFamily = PlusJakartaSans)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(theme.pulseHeroGradient)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                "${theme.heroEmoji} ${theme.typeLabel}",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
            )
            Text(displayTitle, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, fontFamily = PlusJakartaSans)
            Text(
                "Track contributions, budget, and purchase progress with live data only.",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 13.sp,
                fontFamily = PlusJakartaSans,
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            theme.quickChips.forEach { (emoji, label, kind) ->
                PurchaseEmojiChip(
                    theme = theme,
                    label = label,
                    emoji = emoji,
                    enabled = true,
                    onClick = { onQuickAddKind(kind) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        PurchaseSectionCard(
            theme = theme,
            title = "Total Collected",
            trailing = {
                Text(
                    if (fundedPercent != null) "$fundedPercent% funded" else "— funded",
                    color = theme.accentLight,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = PlusJakartaSans,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(theme.accentSoft)
                        .border(1.dp, theme.accent.copy(alpha = 0.35f), RoundedCornerShape(999.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                )
            },
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                PurchaseAccentRing(
                    percent = fundedPercent ?: 0,
                    centerLabel = fundedPercent?.toString() ?: "—",
                    centerSub = if (fundedPercent != null) "/ 100" else "funded",
                    accent = theme.accent,
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(theme.healthLabel, color = theme.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
                    Text(
                        BalanceMask.mask(GroupFinanceFormat.formatMoney(contributionTotal, currency), hideBalances),
                        color = theme.accentLight,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = PlusJakartaSans,
                    )
                    Text(
                        if (budgetTotal != null) {
                            "of ${BalanceMask.mask(GroupFinanceFormat.formatMoney(budgetTotal, currency), hideBalances)} goal"
                        } else {
                            "Set a budget to track funding progress."
                        },
                        color = theme.secondary,
                        fontSize = 12.sp,
                        fontFamily = PlusJakartaSans,
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            PurchaseStatCard("PEOPLE", "$participantCount", theme.statGradients[0], Modifier.weight(1f))
            PurchaseStatCard(
                "FUNDED %",
                fundedPercent?.let { "$it%" } ?: "—",
                theme.statGradients[1],
                Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            PurchaseStatCard(
                "BUDGET",
                GroupFinanceFormat.compactMoney(budgetTotal, currency),
                theme.statGradients[2],
                Modifier.weight(1f),
            )
            val momentsValue = when {
                activities.isNotEmpty() -> "${activities.size}"
                openTasks > 0 -> "$openTasks"
                else -> "—"
            }
            PurchaseStatCard("MOMENTS", momentsValue, theme.statGradients.getOrElse(3) { theme.statGradients[0] }, Modifier.weight(1f))
        }

        PurchaseSectionCard(theme = theme, title = theme.contributionsTitle) {
            when {
                positions.isNotEmpty() -> {
                    positions.take(8).forEachIndexed { idx, pos ->
                        val amount = pos.contributionTotal.takeIf { it.isNotBlank() && it != "0" }
                            ?: pos.paidTotal.takeIf { it.isNotBlank() && it != "0" }
                        PurchaseCrewRow(
                            theme = theme,
                            name = nameById[pos.participantId] ?: pos.participantId.take(8),
                            role = participants.find { it.participantId == pos.participantId }?.roleCode ?: "Member",
                            amountLabel = if (amount != null) {
                                BalanceMask.mask(GroupFinanceFormat.formatMoney(amount, pos.currencyCode), hideBalances)
                            } else {
                                "—"
                            },
                            featured = idx == 0 && amount != null,
                        )
                    }
                }
                participants.isNotEmpty() -> {
                    participants.take(8).forEach { p ->
                        PurchaseCrewRow(
                            theme = theme,
                            name = p.displayName ?: p.participantId.take(8),
                            role = p.roleCode,
                            amountLabel = "—",
                        )
                    }
                }
                else -> PurchaseEmptyBlock(theme, "No contributions yet", "Record a contribution or invite members — nothing is invented.")
            }
        }

        PurchaseSectionCard(theme = theme, title = "Upcoming Deadlines") {
            if (deadlines.isEmpty()) {
                PurchaseEmptyBlock(theme, "No deadlines yet", "Planning items with due dates will appear here.")
            } else {
                deadlines.take(8).forEach { item ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(theme.bg)
                            .border(1.dp, theme.border, RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            item.title ?: item.planningItemId.orEmpty(),
                            color = theme.text,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = PlusJakartaSans,
                        )
                        Text(item.dueAt.orEmpty(), color = theme.secondary, fontSize = 11.sp, fontFamily = PlusJakartaSans)
                    }
                }
            }
        }

        PurchaseSectionCard(
            theme = theme,
            title = theme.budgetTitle,
            trailing = {
                Text(
                    "View Splits →",
                    color = theme.accentLight,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = PlusJakartaSans,
                    modifier = Modifier.clickable(onClick = onViewSplits),
                )
            },
        ) {
            if (primaryTotal == null) {
                PurchaseEmptyBlock(theme, "No finance totals yet", "Add an expense or contribution to see live totals.")
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Collected", color = theme.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
                    Text(
                        BalanceMask.mask(GroupFinanceFormat.formatMoney(contributionTotal, currency), hideBalances),
                        color = theme.accentLight,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = PlusJakartaSans,
                    )
                }
                Text(
                    "Open Group Finance",
                    color = theme.accent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = PlusJakartaSans,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .clickable(onClick = onOpenFinance),
                )
            }
        }

        PurchaseSectionCard(theme = theme, title = "Recent Activity") {
            if (activities.isEmpty()) {
                PurchaseEmptyBlock(theme, "No recent activity", "Contributions, expenses, and updates will show here.")
            } else {
                activities.forEach { item ->
                    Column(modifier = Modifier.padding(vertical = 6.dp)) {
                        Text(item.title, color = theme.text, fontSize = 13.sp, fontWeight = FontWeight.Medium, fontFamily = PlusJakartaSans)
                        Text(item.occurredAt, color = theme.secondary, fontSize = 11.sp, fontFamily = PlusJakartaSans)
                    }
                }
            }
        }

        GroupPulseInsightsHeroCard(
            headerTitle = "🧠 ${theme.insightsTitle}",
            insights = insights,
            gradient = theme.heroGradient,
            footerLabel = "+ Open Quick Add",
            onFooterClick = onOpenQuickAdd,
        )
    }
}

@Composable
private fun PurchaseAccentRing(percent: Int, centerLabel: String, centerSub: String, accent: Color) {
    val display = percent.coerceIn(0, 100)
    Box(modifier = Modifier.size(180.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(180.dp)) {
            val stroke = 12.dp.toPx()
            val pad = stroke / 2
            drawArc(
                color = Color(0xFF2A2538),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(pad, pad),
                size = Size(size.width - stroke, size.height - stroke),
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            if (display > 0 && centerLabel != "—") {
                drawArc(
                    color = accent,
                    startAngle = -90f,
                    sweepAngle = 360f * (display / 100f),
                    useCenter = false,
                    topLeft = Offset(pad, pad),
                    size = Size(size.width - stroke, size.height - stroke),
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(centerLabel, color = Color(0xFFE5E0EE), fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, fontFamily = PlusJakartaSans)
            Text(centerSub, color = Color(0xFFC9C4D8), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
        }
    }
}

private object PurchasePulseMath {
    fun fundedPercent(contributionTotal: String?, budgetTotal: String?): Int? {
        val budget = parse(budgetTotal) ?: return null
        if (budget.compareTo(BigDecimal.ZERO) <= 0) return null
        val contrib = parse(contributionTotal) ?: BigDecimal.ZERO
        return contrib.multiply(BigDecimal(100))
            .divide(budget, 0, RoundingMode.HALF_UP)
            .toInt()
            .coerceIn(0, 100)
    }

    private fun parse(raw: String?): BigDecimal? {
        if (raw.isNullOrBlank()) return null
        return runCatching { BigDecimal(raw.trim()) }.getOrNull()
    }
}

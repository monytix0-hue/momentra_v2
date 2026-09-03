package com.example.momentra.ui.shell.group.wedding.pulse

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.api.ActivityItemDto
import com.example.momentra.data.api.AnalyticsInsightItemDto
import com.example.momentra.data.api.GroupFinancePayloadDto
import com.example.momentra.data.api.GroupFinancePositionDto
import com.example.momentra.data.api.GroupPulsePayloadDto
import com.example.momentra.data.repository.GroupSliceRepository
import com.example.momentra.data.security.BalanceMask
import com.example.momentra.data.security.SecurityPreferences
import com.example.momentra.ui.shell.group.shared.GroupActiveLoading
import com.example.momentra.ui.shell.group.shared.GroupFinanceFormat
import com.example.momentra.ui.shell.group.shared.GroupPulseInsightsHeroCard
import com.example.momentra.ui.shell.group.shared.GroupProgressBar
import com.example.momentra.ui.shell.group.shared.GroupProgressRing
import com.example.momentra.ui.shell.group.shared.GroupTabDataCache
import com.example.momentra.ui.shell.group.shared.loadGroupPulseTab
import com.example.momentra.ui.shell.maestro.MaestroIds
import com.example.momentra.ui.theme.PlusJakartaSans
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.example.momentra.ui.shell.group.wedding.create.WeddingActiveTheme
import com.example.momentra.ui.shell.group.wedding.create.WeddingActivityRow
import com.example.momentra.ui.shell.group.wedding.create.WeddingEmojiChip
import com.example.momentra.ui.shell.group.wedding.create.WeddingEmptyBlock
import com.example.momentra.ui.shell.group.wedding.create.WeddingFadeIn
import com.example.momentra.ui.shell.group.wedding.create.WeddingQuickAddKind
import com.example.momentra.ui.shell.group.wedding.create.WeddingSectionCard
import com.example.momentra.ui.shell.group.wedding.create.WeddingStatCard

/** Figma 575:14939 — Wedding Pulse. Live APIs only; no demo seeds. */
@Composable
fun WeddingPulseActiveContent(
    momentId: String?,
    momentTitle: String?,
    refreshToken: Long,
    onAddExpense: () -> Unit,
    onOpenQuickAdd: () -> Unit = onAddExpense,
    onViewSplits: () -> Unit = onAddExpense,
    onOpenFinance: () -> Unit = onViewSplits,
    onQuickAddKind: (WeddingQuickAddKind) -> Unit = {},
    repository: GroupSliceRepository = remember { GroupSliceRepository() },
    modifier: Modifier = Modifier,
) {
    var loading by remember { mutableStateOf(true) }
    var pulse by remember { mutableStateOf<GroupPulsePayloadDto?>(null) }
    var finance by remember { mutableStateOf<GroupFinancePayloadDto?>(null) }
    var activity by remember { mutableStateOf<List<ActivityItemDto>>(emptyList()) }
    var insights by remember { mutableStateOf<List<AnalyticsInsightItemDto>>(emptyList()) }
    var title by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(refreshToken, momentId) {
        if (momentId.isNullOrBlank()) {
            loading = false
            pulse = null
            finance = null
            activity = emptyList()
            title = null
            return@LaunchedEffect
        }
        error = null
        GroupTabDataCache.peekPulse(momentId)?.let { cached ->
            title = cached.title
            pulse = cached.pulse
            finance = cached.finance
            activity = cached.activities
            insights = cached.insights
            loading = false
        } ?: run { loading = true }
        loadGroupPulseTab(repository, momentId).fold(
            onSuccess = { data ->
                title = data.title
                pulse = data.pulse
                finance = data.finance
                activity = data.activities
                insights = data.insights
                loading = false
            },
            onFailure = { e ->
                error = e.message
                loading = false
            },
        )
    }

    if (loading && pulse == null && finance == null) {
        GroupActiveLoading(modifier.fillMaxSize())
        return
    }

    val primaryTotal = finance?.totals?.firstOrNull()
    val currency = primaryTotal?.currencyCode ?: "INR"
    val budgetTotal = primaryTotal?.budgetTotal
    val expenseTotal = primaryTotal?.expenseTotal
    val participantCount = pulse?.participantCount ?: 0
    val positions = finance?.positions.orEmpty()
    val viewer = finance?.viewerPosition
    val hideBalances = SecurityPreferences(LocalContext.current).hideBalances()
    val displayTitle = momentTitle ?: title ?: "Wedding Pulse"
    val hasLiveAttention = (pulse?.attentionCount ?: 0) > 0
    val openTasks = pulse?.openTaskCount ?: 0
    val utilization = GroupFinanceFormat.utilizationPercent(expenseTotal, budgetTotal)
    val poolLabel = if (budgetTotal != null) {
        BalanceMask.mask(GroupFinanceFormat.formatMoney(budgetTotal, currency), hideBalances)
    } else {
        "—"
    }
    val budgetCompact = if (budgetTotal != null) {
        GroupFinanceFormat.compactMoney(budgetTotal, currency)
    } else {
        "—"
    }

    WeddingFadeIn {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(WeddingActiveTheme.Bg)
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
                    .clip(RoundedCornerShape(WeddingActiveTheme.HeroRadius))
                    .background(WeddingActiveTheme.HeroGradient)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    HeroPill("✓  $displayTitle", solid = true)
                    HeroPill("Wedding", solid = false)
                }
                Text("💕", fontSize = 22.sp)
                Text(
                    displayTitle,
                    color = WeddingActiveTheme.DarkText,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = PlusJakartaSans,
                )
                Text(
                    "$participantCount people · ${finance?.expenseCount ?: 0} expenses",
                    color = WeddingActiveTheme.DarkText.copy(alpha = 0.75f),
                    fontSize = 13.sp,
                    fontFamily = PlusJakartaSans,
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                WeddingEmojiChip(
                    label = "Photos",
                    emoji = "📸",
                    enabled = true,
                    onClick = { onQuickAddKind(WeddingQuickAddKind.MEMORY) },
                    modifier = Modifier.weight(1f),
                )
                WeddingEmojiChip(
                    label = "RSVPs",
                    emoji = "💬",
                    enabled = true,
                    onClick = { onQuickAddKind(WeddingQuickAddKind.ATTENDANCE) },
                    modifier = Modifier.weight(1f),
                )
                WeddingEmojiChip(
                    label = "Registry",
                    emoji = "📋",
                    enabled = true,
                    onClick = { onQuickAddKind(WeddingQuickAddKind.EXPENSE) },
                    modifier = Modifier.weight(1f),
                )
                WeddingEmojiChip(
                    label = "Gifts",
                    emoji = "🎁",
                    enabled = true,
                    onClick = { onQuickAddKind(WeddingQuickAddKind.CONTRIBUTION) },
                    modifier = Modifier.weight(1f),
                )
            }

            WeddingSectionCard(title = "💕  Wedding Pulse") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Wedding Health", color = WeddingActiveTheme.Text, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
                        WeddingEmptyBlock(
                            message = "Score not available yet",
                            detail = "Health scoring is coming soon — no invented numbers.",
                        )
                    }
                    GroupProgressRing(percent = 0, centerLabel = "—", centerSub = "Soon")
                }
            }

            WeddingSectionCard(
                title = "⚠️  Needs Attention",
                trailing = if (hasLiveAttention) {
                    {
                        Text(
                            "${pulse?.attentionCount}",
                            color = WeddingActiveTheme.DarkText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = PlusJakartaSans,
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(WeddingActiveTheme.Accent)
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                } else {
                    null
                },
            ) {
                if (hasLiveAttention) {
                    Text(
                        "${pulse?.attentionCount} items flagged",
                        color = WeddingActiveTheme.AccentLight,
                        fontSize = 13.sp,
                        fontFamily = PlusJakartaSans,
                    )
                } else {
                    WeddingEmptyBlock(
                        message = "All clear for now",
                        detail = "Attention items appear when the backend exposes them.",
                    )
                }
            }

            WeddingSectionCard(
                title = "📈  Wedding Progress",
                modifier = Modifier
                    .testTag(MaestroIds.PULSE_PROGRESS)
                    .clickable(onClick = onOpenFinance),
                trailing = {
                    Text(
                        if (budgetTotal != null) "$utilization% used" else "—",
                        color = WeddingActiveTheme.AccentLight,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = PlusJakartaSans,
                    )
                },
            ) {
                if (budgetTotal != null) {
                    Text(
                        "$utilization% of budget used",
                        color = WeddingActiveTheme.Text,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = PlusJakartaSans,
                    )
                    GroupProgressBar(percent = utilization)
                } else {
                    WeddingEmptyBlock(
                        message = "No budget yet",
                        detail = "Set a budget or add expenses to track progress.",
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WeddingStatCard(
                        label = "TASKS",
                        value = "$openTasks",
                        gradient = WeddingActiveTheme.StatGradientA,
                        icon = "✅",
                        modifier = Modifier.weight(1f),
                    )
                    WeddingStatCard(
                        label = "BUDGET",
                        value = budgetCompact,
                        gradient = WeddingActiveTheme.StatGradientC,
                        icon = "💰",
                        modifier = Modifier.weight(1f).testTag(MaestroIds.PULSE_PROGRESS_BUDGET),
                    )
                    WeddingStatCard(
                        label = "PEOPLE",
                        value = "$participantCount",
                        gradient = WeddingActiveTheme.StatGradientB,
                        icon = "👥",
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            WeddingSectionCard(title = "👥  Wedding Party") {
                if (positions.isEmpty()) {
                    WeddingEmptyBlock(
                        message = "No participation data yet",
                        detail = "Positions appear after shared expenses are recorded.",
                    )
                } else {
                    positions.take(5).forEach { pos ->
                        val net = runCatching { BigDecimal(pos.netPosition) }.getOrNull() ?: BigDecimal.ZERO
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                pos.participantId.take(8),
                                color = WeddingActiveTheme.Text,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = PlusJakartaSans,
                            )
                            Text(
                                BalanceMask.mask(GroupFinanceFormat.formatMoney(pos.netPosition, pos.currencyCode), hideBalances),
                                color = if (net >= BigDecimal.ZERO) Color(0xFF4ADE80) else Color(0xFFFF7A3D),
                                fontSize = 12.sp,
                                fontFamily = PlusJakartaSans,
                            )
                        }
                    }
                }
            }

            WeddingSectionCard(
                title = "💰  Wedding Budget",
                trailing = {
                    Text(
                        "View Splits →",
                        color = WeddingActiveTheme.Accent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = PlusJakartaSans,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(WeddingActiveTheme.AccentSoft)
                            .clickable(onClick = onViewSplits)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                },
            ) {
                if (primaryTotal == null) {
                    WeddingEmptyBlock(
                        message = "No finance totals yet",
                        detail = "Add an expense to see settlement and budget data.",
                    )
                } else {
                    Text(
                        "Your Balance: ${viewerBalanceShort(viewer, hideBalances)}",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = PlusJakartaSans,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(WeddingActiveTheme.MagentaGradient)
                            .clickable(onClick = onOpenFinance)
                            .padding(16.dp),
                    )
                    Text(
                        "$poolLabel Total Pool",
                        color = WeddingActiveTheme.Text,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = PlusJakartaSans,
                        modifier = Modifier.clickable(onClick = onOpenFinance),
                    )
                    Text(
                        "Spent ${BalanceMask.mask(GroupFinanceFormat.formatMoney(expenseTotal, currency), hideBalances)}",
                        color = WeddingActiveTheme.Secondary,
                        fontSize = 13.sp,
                        fontFamily = PlusJakartaSans,
                    )
                }
            }

            WeddingSectionCard(title = "📅  Recent Activity") {
                if (activity.isEmpty()) {
                    WeddingEmptyBlock(
                        message = "No recent activity",
                        detail = "Expenses, plans, and updates will show here.",
                    )
                } else {
                    activity.forEach { item ->
                        WeddingActivityRow("📌", item.title, formatOccurredAt(item.occurredAt))
                    }
                }
            }

            GroupPulseInsightsHeroCard(
                headerTitle = "🧠 Wedding Insights",
                insights = insights,
                gradient = WeddingActiveTheme.HeroGradient,
            )
        }
    }
}

@Composable
private fun HeroPill(label: String, solid: Boolean) {
    Text(
        label,
        color = WeddingActiveTheme.DarkText,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = PlusJakartaSans,
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (solid) Color.White.copy(alpha = 0.75f) else Color.White.copy(alpha = 0.35f))
            .border(1.dp, WeddingActiveTheme.Accent.copy(alpha = 0.45f), RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

private fun viewerBalanceShort(viewer: GroupFinancePositionDto?, hide: Boolean): String {
    if (viewer == null) return "No balance yet"
    val net = runCatching { BigDecimal(viewer.netPosition) }.getOrNull() ?: return "No balance yet"
    val formatted = BalanceMask.mask(GroupFinanceFormat.formatMoney(viewer.netPosition, viewer.currencyCode), hide)
    return when {
        net > BigDecimal.ZERO -> "You are owed $formatted"
        net < BigDecimal.ZERO -> "You owe $formatted"
        else -> "Settled up"
    }
}

private fun formatOccurredAt(raw: String): String = try {
    val instant = Instant.parse(raw)
    DateTimeFormatter.ofPattern("d MMM · HH:mm", Locale.getDefault())
        .withZone(ZoneId.systemDefault())
        .format(instant)
} catch (_: Exception) {
    raw
}

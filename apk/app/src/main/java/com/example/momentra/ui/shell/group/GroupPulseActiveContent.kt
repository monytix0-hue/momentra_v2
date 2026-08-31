package com.example.momentra.ui.shell.group

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.api.ActivityItemDto
import com.example.momentra.data.api.GroupFinancePayloadDto
import com.example.momentra.data.api.GroupFinancePositionDto
import com.example.momentra.data.api.GroupParticipantDto
import com.example.momentra.data.api.GroupPulsePayloadDto
import com.example.momentra.data.repository.GroupSliceRepository
import com.example.momentra.data.security.BalanceMask
import com.example.momentra.data.security.SecurityPreferences
import com.example.momentra.ui.shell.maestro.MaestroIds
import com.example.momentra.ui.theme.PlusJakartaSans
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.min
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

@Composable
fun GroupPulseActiveContent(
    momentId: String?,
    momentTitle: String?,
    refreshToken: Long,
    onAddExpense: () -> Unit,
    onViewSplits: () -> Unit = onAddExpense,
    onOpenFinance: () -> Unit = onViewSplits,
    repository: GroupSliceRepository = remember { GroupSliceRepository() },
    modifier: Modifier = Modifier,
) {
    var loading by remember { mutableStateOf(true) }
    var pulse by remember { mutableStateOf<GroupPulsePayloadDto?>(null) }
    var finance by remember { mutableStateOf<GroupFinancePayloadDto?>(null) }
    var activity by remember { mutableStateOf<List<ActivityItemDto>>(emptyList()) }
    var participants by remember { mutableStateOf<List<GroupParticipantDto>>(emptyList()) }
    var title by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(refreshToken, momentId) {
        if (momentId.isNullOrBlank()) {
            loading = false
            pulse = null
            finance = null
            activity = emptyList()
            participants = emptyList()
            title = null
            return@LaunchedEffect
        }
        error = null
        GroupTabDataCache.peekPulse(momentId)?.let { cached ->
            title = cached.title
            pulse = cached.pulse
            finance = cached.finance
            activity = cached.activities
            loading = false
        } ?: run { loading = true }
        coroutineScope {
            val pulseJob = async { loadGroupPulseTab(repository, momentId) }
            val peopleJob = async { repository.getParticipants(momentId) }
            pulseJob.await().fold(
                onSuccess = { data ->
                    title = data.title
                    pulse = data.pulse
                    finance = data.finance
                    activity = data.activities
                    loading = false
                },
                onFailure = { e ->
                    error = e.message
                    loading = false
                },
            )
            peopleJob.await().onSuccess { participants = it.participants }
        }
    }

    if (loading && pulse == null) {
        GroupActiveLoading(modifier.fillMaxSize())
        return
    }

    val primaryTotal = finance?.totals?.firstOrNull()
    val currency = primaryTotal?.currencyCode ?: "INR"
    val budgetTotal = primaryTotal?.budgetTotal
    val expenseTotal = primaryTotal?.expenseTotal
    val contributionTotal = primaryTotal?.contributionTotal
    val utilization = GroupFinanceFormat.utilizationPercent(expenseTotal, budgetTotal)
    val participantCount = pulse?.participantCount ?: participants.size
    val expenseCount = finance?.expenseCount ?: 0
    val openTasks = pulse?.openTaskCount ?: 0
    val attentionCount = pulse?.attentionCount ?: 0
    val positions = finance?.positions.orEmpty()
    val viewer = finance?.viewerPosition
    val hideBalances = SecurityPreferences(LocalContext.current).hideBalances()
    val displayTitle = momentTitle ?: title ?: "Trip"
    val nameById = remember(participants) {
        participants.associate { it.participantId to (it.displayName ?: it.participantId.take(8)) }
    }
    val maxAbsNet = positions.maxOfOrNull {
        GroupFinanceFormat.parseAmount(it.netPosition).abs()
    } ?: BigDecimal.ZERO

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(GroupActiveTheme.Bg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .padding(bottom = 56.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        error?.let {
            Text(it, color = Color(0xFFF87171), fontSize = 12.sp, fontFamily = PlusJakartaSans)
        }

        AnimatedVisibility(
            visible = true,
            enter = fadeIn() + slideInVertically { it / 8 },
        ) {
            TripHeroHeader(
                title = displayTitle,
                peopleCount = participantCount,
            )
        }

        AnimatedVisibility(visible = true, enter = fadeIn() + slideInVertically { it / 6 }) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TripQuickTile(
                    label = "Photos",
                    emoji = "📸",
                    accent = Color(0xFFFBBF24),
                    enabled = false,
                    onClick = {},
                    modifier = Modifier.weight(1f),
                )
                TripQuickTile(
                    label = "Chat",
                    emoji = "💬",
                    accent = Color(0xFFA855F7),
                    enabled = false,
                    onClick = {},
                    modifier = Modifier.weight(1f),
                )
                TripQuickTile(
                    label = "Itinerary",
                    emoji = "🗺️",
                    accent = Color(0xFFA16207),
                    enabled = false,
                    onClick = {},
                    modifier = Modifier.weight(1f),
                )
                TripQuickTile(
                    label = "Splits",
                    emoji = "💸",
                    accent = Color(0xFF22C55E),
                    enabled = true,
                    onClick = onViewSplits,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        AnimatedVisibility(visible = true, enter = fadeIn()) {
            GroupSectionCard(title = "Group Pulse") {
                Text(
                    "Live trip signals from your group — no invented health scores.",
                    color = GroupActiveTheme.Secondary,
                    fontSize = 12.sp,
                    fontFamily = PlusJakartaSans,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    GroupQuickChip(label = "$openTasks tasks", enabled = false, onClick = {})
                    GroupQuickChip(label = "$participantCount people", enabled = false, onClick = {})
                    GroupQuickChip(label = "$expenseCount expenses", enabled = false, onClick = {})
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Health score", color = GroupActiveTheme.Secondary, fontSize = 12.sp, fontFamily = PlusJakartaSans)
                        GroupEmptySection(
                            message = "Score not available yet",
                            detail = "Group health scoring is coming soon — no invented numbers.",
                        )
                    }
                    GroupProgressRing(percent = 0, centerLabel = "—", centerSub = "Coming soon")
                }
            }
        }

        AnimatedVisibility(visible = true, enter = fadeIn()) {
            GroupSectionCard(
                title = "Needs Attention",
                badge = if (attentionCount > 0) {
                    {
                        Text(
                            "$attentionCount",
                            color = Color(0xFF131313),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = PlusJakartaSans,
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(GroupActiveTheme.AccentOrange)
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                } else {
                    null
                },
            ) {
                if (attentionCount > 0) {
                    Text(
                        "$attentionCount items need a look from the group.",
                        color = GroupActiveTheme.Brand,
                        fontSize = 13.sp,
                        fontFamily = PlusJakartaSans,
                    )
                } else {
                    GroupEmptySection(
                        message = "All clear for now",
                        detail = "Attention items appear when the backend flags them — nothing invented here.",
                    )
                }
            }
        }

        AnimatedVisibility(visible = true, enter = fadeIn()) {
            GroupSectionCard(
                title = "Progress Tracker",
                modifier = Modifier
                    .testTag(MaestroIds.PULSE_PROGRESS)
                    .clickable(onClick = onOpenFinance),
            ) {
                Text(
                    "$utilization% of budget used",
                    color = GroupActiveTheme.Text,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    fontFamily = PlusJakartaSans,
                )
                GroupProgressBar(percent = utilization)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GroupMetricTile(
                        label = "Tasks",
                        value = "$openTasks",
                        modifier = Modifier.weight(1f),
                    )
                    GroupMetricTile(
                        label = "Budget",
                        value = GroupFinanceFormat.compactMoney(budgetTotal, currency),
                        modifier = Modifier.weight(1f).testTag(MaestroIds.PULSE_PROGRESS_BUDGET),
                    )
                    GroupMetricTile(
                        label = "Moments",
                        value = "$expenseCount",
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        AnimatedVisibility(visible = true, enter = fadeIn()) {
            GroupSectionCard(title = "Participation") {
                if (positions.isEmpty() && participants.isEmpty()) {
                    GroupEmptySection(
                        message = "No participation data yet",
                        detail = "Positions appear after shared expenses are recorded.",
                    )
                } else {
                    val rows = if (positions.isNotEmpty()) {
                        positions.take(6)
                    } else {
                        emptyList()
                    }
                    if (rows.isEmpty()) {
                        participants.take(6).forEach { person ->
                            Text(
                                person.displayName ?: person.participantId.take(8),
                                color = GroupActiveTheme.Text,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                fontFamily = PlusJakartaSans,
                                modifier = Modifier.padding(vertical = 4.dp),
                            )
                        }
                    } else {
                        rows.forEach { pos ->
                            ParticipationRow(
                                name = nameById[pos.participantId] ?: pos.participantId.take(8),
                                position = pos,
                                hide = hideBalances,
                                barPercent = participationBarPercent(pos.netPosition, maxAbsNet, positions.size),
                            )
                        }
                    }
                }
            }
        }

        AnimatedVisibility(visible = true, enter = fadeIn()) {
            GroupSectionCard(
                title = "Settlement & Contributions",
                badge = {
                    Text(
                        "View Splits",
                        color = GroupActiveTheme.AccentOrange,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = PlusJakartaSans,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x33FF7A3D))
                            .clickable(onClick = onViewSplits)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                },
            ) {
                if (primaryTotal == null && viewer == null) {
                    GroupEmptySection(message = "No finance totals yet", detail = "Add an expense to see settlement data.")
                } else {
                    OrangeBalanceCard(
                        viewer = viewer,
                        outstandingTotal = primaryTotal?.outstandingTotal,
                        currency = currency,
                        hide = hideBalances,
                        onClick = onOpenFinance,
                    )
                    if (primaryTotal != null) {
                        FinanceBarRow(
                            label = "Expenses",
                            value = expenseTotal,
                            currency = currency,
                            hide = hideBalances,
                            fill = GroupActiveTheme.AccentOrange,
                            max = maxOf(
                                GroupFinanceFormat.parseAmount(expenseTotal),
                                GroupFinanceFormat.parseAmount(contributionTotal),
                                GroupFinanceFormat.parseAmount(budgetTotal),
                                BigDecimal.ONE,
                            ),
                        )
                        FinanceBarRow(
                            label = "Contributions",
                            value = contributionTotal,
                            currency = currency,
                            hide = hideBalances,
                            fill = Color(0xFF22C55E),
                            max = maxOf(
                                GroupFinanceFormat.parseAmount(expenseTotal),
                                GroupFinanceFormat.parseAmount(contributionTotal),
                                GroupFinanceFormat.parseAmount(budgetTotal),
                                BigDecimal.ONE,
                            ),
                        )
                    }
                }
            }
        }

        AnimatedVisibility(visible = true, enter = fadeIn()) {
            GroupSectionCard(title = "Recent Activity") {
                if (activity.isEmpty()) {
                    GroupEmptySection(message = "No recent activity", detail = "Expenses and contributions will show here.")
                } else {
                    activity.forEach { ActivityChromeRow(it) }
                }
            }
        }

        AnimatedVisibility(visible = true, enter = fadeIn()) {
            GroupSectionCard(title = "Momentra Insights", badge = { GroupComingSoonBadge() }) {
                GroupEmptySection(
                    message = "AI insights for groups",
                    detail = "Personalized trip insights are on the roadmap — nothing invented.",
                )
            }
        }

        GroupCtaButton(label = "Add Expense", enabled = true, onClick = onAddExpense)
    }
}

@Composable
private fun TripHeroHeader(title: String, peopleCount: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(GroupActiveTheme.HeroGradient)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState()),
        ) {
            HeroPill(label = title, solid = true)
            HeroPill(label = "Trip · $peopleCount people", solid = false)
        }
        Text("🌴", fontSize = 20.sp)
        Text(
            title,
            color = GroupActiveTheme.Text,
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = PlusJakartaSans,
        )
        Text(
            "Shared trip pulse",
            color = GroupActiveTheme.Secondary,
            fontSize = 13.sp,
            fontFamily = PlusJakartaSans,
        )
    }
}

@Composable
private fun HeroPill(label: String, solid: Boolean) {
    Text(
        label,
        color = GroupActiveTheme.Text,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = PlusJakartaSans,
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (solid) Color(0x33FFB598) else Color(0x221A1512))
            .border(1.dp, GroupActiveTheme.Border, RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

@Composable
private fun TripQuickTile(
    label: String,
    emoji: String,
    accent: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.45f)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(accent.copy(alpha = 0.22f))
                .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(emoji, fontSize = 22.sp)
        }
        Text(
            label,
            color = GroupActiveTheme.Text,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = PlusJakartaSans,
        )
    }
}

@Composable
private fun OrangeBalanceCard(
    viewer: GroupFinancePositionDto?,
    outstandingTotal: String?,
    currency: String,
    hide: Boolean,
    onClick: () -> Unit,
) {
    val net = viewer?.let { GroupFinanceFormat.parseAmount(it.netPosition) }
    val headline = when {
        viewer != null && net != null && net < BigDecimal.ZERO ->
            "You owe ${BalanceMask.mask(GroupFinanceFormat.formatMoney(net.abs().toPlainString(), currency), hide)}"
        viewer != null && net != null && net > BigDecimal.ZERO ->
            "You are owed ${BalanceMask.mask(GroupFinanceFormat.formatMoney(viewer.netPosition, currency), hide)}"
        viewer != null -> "You're settled up"
        else -> "Outstanding ${BalanceMask.mask(GroupFinanceFormat.formatMoney(outstandingTotal, currency), hide)}"
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(GroupActiveTheme.AccentOrange, TripSheetTokens.AccentEnd),
                ),
            )
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            "YOUR BALANCE",
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = PlusJakartaSans,
        )
        Text(
            headline,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = PlusJakartaSans,
        )
    }
}

@Composable
private fun FinanceBarRow(
    label: String,
    value: String?,
    currency: String,
    hide: Boolean,
    fill: Color,
    max: BigDecimal,
) {
    val amount = GroupFinanceFormat.parseAmount(value)
    val percent = if (max > BigDecimal.ZERO) {
        amount.multiply(BigDecimal(100)).divide(max, 0, RoundingMode.HALF_UP).toInt().coerceIn(0, 100)
    } else {
        0
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = GroupActiveTheme.Secondary, fontSize = 12.sp, fontFamily = PlusJakartaSans)
            Text(
                BalanceMask.mask(GroupFinanceFormat.formatMoney(value, currency), hide),
                color = GroupActiveTheme.Text,
                fontSize = 12.sp,
                fontFamily = PlusJakartaSans,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(100.dp))
                .background(Color(0xFF2A2624)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(percent / 100f)
                    .clip(RoundedCornerShape(100.dp))
                    .background(fill)
                    .padding(vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun ParticipationRow(
    name: String,
    position: GroupFinancePositionDto,
    hide: Boolean,
    barPercent: Int,
) {
    val net = GroupFinanceFormat.parseAmount(position.netPosition)
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(name, color = GroupActiveTheme.Text, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, fontFamily = PlusJakartaSans)
            Text(
                BalanceMask.mask(GroupFinanceFormat.formatMoney(position.netPosition, position.currencyCode), hide),
                color = if (net >= BigDecimal.ZERO) Color(0xFF4ADE80) else GroupActiveTheme.AccentOrange,
                fontSize = 12.sp,
                fontFamily = PlusJakartaSans,
            )
        }
        GroupProgressBar(percent = barPercent)
    }
}

@Composable
private fun ActivityChromeRow(item: ActivityItemDto) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(GroupActiveTheme.BrandSoft)
                .border(1.dp, GroupActiveTheme.Border, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(activityGlyph(item.activityCode), fontSize = 14.sp)
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(item.title, color = GroupActiveTheme.Text, fontSize = 13.sp, fontWeight = FontWeight.Medium, fontFamily = PlusJakartaSans)
            Text(formatOccurredAt(item.occurredAt), color = GroupActiveTheme.Secondary, fontSize = 11.sp, fontFamily = PlusJakartaSans)
        }
    }
}

private fun participationBarPercent(netRaw: String?, maxAbs: BigDecimal, count: Int): Int {
    if (maxAbs > BigDecimal.ZERO) {
        val abs = GroupFinanceFormat.parseAmount(netRaw).abs()
        return abs.multiply(BigDecimal(100)).divide(maxAbs, 0, RoundingMode.HALF_UP).toInt().coerceIn(8, 100)
    }
    return if (count > 0) min(100, 100 / count) else 0
}

private fun activityGlyph(code: String): String = when {
    code.contains("EXPENSE", ignoreCase = true) -> "💸"
    code.contains("SETTLE", ignoreCase = true) -> "✅"
    code.contains("CONTRIB", ignoreCase = true) -> "🤝"
    else -> "📌"
}

private fun formatOccurredAt(raw: String): String = try {
    val instant = Instant.parse(raw)
    DateTimeFormatter.ofPattern("d MMM · HH:mm", Locale.getDefault())
        .withZone(ZoneId.systemDefault())
        .format(instant)
} catch (_: Exception) {
    raw
}

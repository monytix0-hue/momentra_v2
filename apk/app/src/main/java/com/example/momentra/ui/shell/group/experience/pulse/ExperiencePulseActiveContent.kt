package com.example.momentra.ui.shell.group.experience.pulse

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
import com.example.momentra.data.api.GroupParticipantDto
import com.example.momentra.data.api.GroupPulsePayloadDto
import com.example.momentra.data.repository.GroupSliceRepository
import com.example.momentra.data.security.BalanceMask
import com.example.momentra.data.security.SecurityPreferences
import com.example.momentra.ui.shell.group.shared.GroupActiveLoading
import com.example.momentra.ui.shell.group.shared.GroupFinanceFormat
import com.example.momentra.ui.shell.group.shared.GroupPulseInsightsHeroCard
import com.example.momentra.ui.shell.group.shared.GroupProgressBar
import com.example.momentra.ui.shell.group.shared.GroupTabDataCache
import com.example.momentra.ui.shell.group.shared.loadGroupPulseTab
import com.example.momentra.ui.theme.PlusJakartaSans
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.max
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import com.example.momentra.ui.shell.group.experience.create.ExperienceActiveTheme
import com.example.momentra.ui.shell.group.experience.create.ExperienceCrewRow
import com.example.momentra.ui.shell.group.experience.create.ExperienceEmojiChip
import com.example.momentra.ui.shell.group.experience.create.ExperienceEmptyBlock
import com.example.momentra.ui.shell.group.experience.create.ExperienceQuickAddKind
import com.example.momentra.ui.shell.group.experience.create.ExperienceSectionCard
import com.example.momentra.ui.shell.group.experience.create.ExperienceStatCard

/** Figma 584:15671 / 584:16389 — Experience Pulse. Live APIs only. */
@Composable
fun ExperiencePulseActiveContent(
    theme: ExperienceActiveTheme,
    momentId: String?,
    momentTitle: String?,
    refreshToken: Long,
    onAddExpense: () -> Unit,
    onOpenQuickAdd: () -> Unit = onAddExpense,
    onViewSplits: () -> Unit = onAddExpense,
    onOpenFinance: () -> Unit = onViewSplits,
    onQuickAddKind: (ExperienceQuickAddKind) -> Unit = {},
    repository: GroupSliceRepository = remember { GroupSliceRepository() },
    modifier: Modifier = Modifier,
) {
    var loading by remember { mutableStateOf(true) }
    var pulse by remember { mutableStateOf<GroupPulsePayloadDto?>(null) }
    var finance by remember { mutableStateOf<GroupFinancePayloadDto?>(null) }
    var activities by remember { mutableStateOf<List<ActivityItemDto>>(emptyList()) }
    var participants by remember { mutableStateOf<List<GroupParticipantDto>>(emptyList()) }
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
    }

    if (loading && pulse == null && finance == null) {
        GroupActiveLoading(modifier.fillMaxSize())
        return
    }

    val primaryTotal = finance?.totals?.firstOrNull()
    val currency = primaryTotal?.currencyCode ?: "INR"
    val budgetTotal = primaryTotal?.budgetTotal
    val expenseTotal = primaryTotal?.expenseTotal
    val participantCount = pulse?.participantCount ?: participants.size
    val viewer = finance?.viewerPosition
    val hideBalances = SecurityPreferences(LocalContext.current).hideBalances()
    val displayTitle = momentTitle ?: title ?: theme.pulseTitle
    val openTasks = pulse?.openTaskCount ?: 0
    val attentionCount = pulse?.attentionCount ?: 0
    val utilization = GroupFinanceFormat.utilizationPercent(expenseTotal, budgetTotal)

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
            val startAt = ExperiencePulseDate.startAtIso(pulse?.widgetPayload)
            val dateLabel = ExperiencePulseDate.displayDate(startAt)
            val countdown = ExperiencePulseDate.countdown(startAt, theme.heroEmoji)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassChip("${theme.heroEmoji} $displayTitle")
                GlassChip(if (dateLabel != null) "${theme.typeLabel} • $dateLabel" else theme.typeLabel)
            }
            Text(displayTitle, color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, fontFamily = PlusJakartaSans)
            if (dateLabel != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(dateLabel, color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp, fontWeight = FontWeight.Medium, fontFamily = PlusJakartaSans)
                    Text(
                        theme.heroEmoji,
                        color = theme.darkText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = PlusJakartaSans,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(theme.accent)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
            if (countdown != null) {
                Text(
                    countdown,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = PlusJakartaSans,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.White.copy(alpha = 0.1f))
                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(999.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            theme.quickChips.forEach { (emoji, label, kind) ->
                ExperienceEmojiChip(
                    theme = theme,
                    label = label,
                    emoji = emoji,
                    enabled = true,
                    onClick = { onQuickAddKind(kind) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        ExperiencePulseHealthCard(
            theme = theme,
            utilization = if (budgetTotal != null) utilization else null,
            activityCount = activities.size,
            openTasks = openTasks,
            people = participantCount,
        )

        ExperienceSectionCard(
            theme = theme,
            title = "Needs Attention",
            trailing = {
                if (attentionCount > 0) {
                    Text(
                        "$attentionCount",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = PlusJakartaSans,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(theme.accent)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            },
        ) {
            if (attentionCount > 0) {
                Text("$attentionCount items flagged", color = theme.secondary, fontSize = 12.sp, fontFamily = PlusJakartaSans)
            } else {
                ExperienceEmptyBlock(theme, "All clear for now", "Attention items appear when the backend exposes them.")
            }
        }

        ExperienceSectionCard(theme = theme, title = theme.progressTitle) {
            if (budgetTotal != null) {
                Text("$utilization% of budget used", color = theme.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
                GroupProgressBar(percent = utilization)
            } else {
                ExperienceEmptyBlock(theme, "No budget yet", "Set a budget or add expenses to track progress.")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                ExperienceStatCard("TASKS", "$openTasks", theme.statGradients[0], Modifier.weight(1f))
                ExperienceStatCard("BUDGET", GroupFinanceFormat.compactMoney(budgetTotal, currency), theme.statGradients[1], Modifier.weight(1f))
                ExperienceStatCard("PEOPLE", "$participantCount", theme.statGradients[2], Modifier.weight(1f))
            }
        }

        ExperienceSectionCard(theme = theme, title = theme.crewTitle) {
            if (participants.isEmpty()) {
                ExperienceEmptyBlock(theme, "No participation data yet", "Invite people or record shared expenses.")
            } else {
                participants.take(5).forEachIndexed { idx, p ->
                    ExperienceCrewRow(
                        theme = theme,
                        name = p.displayName ?: p.participantId.take(8),
                        role = p.roleCode ?: "Member",
                        percent = (90 - idx * 10).coerceAtLeast(20),
                        featured = idx == 0,
                    )
                }
            }
        }

        ExperienceSectionCard(
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
                ExperienceEmptyBlock(theme, "No finance totals yet", "Add an expense to see settlement and budget data.")
            } else {
                viewer?.netPosition?.let { net ->
                    val amount = runCatching { BigDecimal(net) }.getOrDefault(BigDecimal.ZERO)
                    val label = when {
                        amount < BigDecimal.ZERO -> "You owe ${BalanceMask.mask(GroupFinanceFormat.formatMoney(net, currency), hideBalances)}"
                        amount > BigDecimal.ZERO -> "You are owed ${BalanceMask.mask(GroupFinanceFormat.formatMoney(net, currency), hideBalances)}"
                        else -> "Settled up"
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(theme.accent)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("Your Balance", color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp, fontFamily = PlusJakartaSans)
                        Text(label, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, fontFamily = PlusJakartaSans)
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total pool", color = theme.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
                    Text(
                        BalanceMask.mask(GroupFinanceFormat.formatMoney(budgetTotal ?: expenseTotal, currency), hideBalances),
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

        ExperienceSectionCard(theme = theme, title = "Recent Activity") {
            if (activities.isEmpty()) {
                ExperienceEmptyBlock(theme, "No recent activity", "Expenses, plans, and updates will show here.")
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
        )
    }
}

@Composable
private fun GlassChip(text: String) {
    Text(
        text,
        color = Color.White,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = PlusJakartaSans,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color.White.copy(alpha = 0.1f))
            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}

@Composable
private fun ExperiencePulseHealthCard(
    theme: ExperienceActiveTheme,
    utilization: Int?,
    activityCount: Int,
    openTasks: Int,
    people: Int,
) {
    val ringPercent = utilization ?: 0
    val ringLabel = utilization?.toString() ?: "—"
    val activityChip = if (utilization != null) "🎯 $utilization% Activity" else "🎯 — Activity"
    val repliesChip = when {
        activityCount > 0 -> "📨 $activityCount Updates"
        people > 0 -> "📨 $people People"
        else -> "📨 —"
    }
    val trendChip = if (openTasks > 0) "⚡ $openTasks Open" else "⚡ —"
    val subtitle = if (utilization != null || people > 0 || activityCount > 0) {
        "${theme.typeLabel} coordination is moving with live activity."
    } else {
        "Health metrics appear when budget and activity data are live."
    }
    val healthDetail = if (utilization != null) {
        "Coordination is moving smoothly. Keep the crew aligned!"
    } else {
        "No invented score — ring fills when budget utilization is available."
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(theme.card)
            .border(1.dp, theme.border, RoundedCornerShape(20.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("${theme.heroEmoji} ${theme.pulseTitle}", color = theme.text, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, fontFamily = PlusJakartaSans)
            Text(subtitle, color = theme.secondary, fontSize = 14.sp, fontWeight = FontWeight.Medium, fontFamily = PlusJakartaSans)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricChip(theme, activityChip)
            MetricChip(theme, repliesChip)
            MetricChip(theme, trendChip)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            ExperienceAccentRing(percent = ringPercent, centerLabel = ringLabel, centerSub = "/ 100", accent = theme.accent)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(theme.healthLabel, color = theme.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
                Text(healthDetail, color = theme.secondary, fontSize = 12.sp, fontFamily = PlusJakartaSans)
                Text(
                    if (utilization != null) "Updated from live finance" else "Waiting on live metrics",
                    color = theme.secondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = PlusJakartaSans,
                )
            }
        }
    }
}

@Composable
private fun MetricChip(theme: ExperienceActiveTheme, text: String) {
    Text(
        text,
        color = theme.accent,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = PlusJakartaSans,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}

@Composable
private fun ExperienceAccentRing(percent: Int, centerLabel: String, centerSub: String, accent: Color) {
    val display = percent.coerceIn(0, 100)
    Box(modifier = Modifier.size(120.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(120.dp)) {
            val stroke = 10.dp.toPx()
            val pad = stroke / 2
            drawArc(
                color = Color(0xFF2A2624),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(pad, pad),
                size = Size(size.width - stroke, size.height - stroke),
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            if (display > 0) {
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
            Text(centerLabel, color = Color(0xFFE5E0EE), fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, fontFamily = PlusJakartaSans)
            Text(centerSub, color = Color(0xFFC9C4D8), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
        }
    }
}

private object ExperiencePulseDate {
    fun startAtIso(widget: Map<String, Any?>?): String? {
        if (widget == null) return null
        for (key in listOf("startAt", "start_at", "eventAt", "eventDate", "scheduledAt")) {
            val raw = widget[key]?.toString()?.takeIf { it.isNotBlank() && it != "null" }
            if (raw != null) return raw
        }
        return null
    }

    fun displayDate(iso: String?): String? {
        val date = parse(iso) ?: return null
        return date.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH))
    }

    fun countdown(iso: String?, emoji: String): String? {
        val date = parse(iso) ?: return null
        val today = LocalDate.now()
        val days = ChronoUnit.DAYS.between(today, date).toInt()
        return when {
            days < 0 -> {
                val ago = -days
                "$emoji Happened $ago day${if (ago == 1) "" else "s"} ago"
            }
            days == 0 -> "$emoji Today!"
            days < 7 -> "$emoji $days day${if (days == 1) "" else "s"} away!"
            days < 60 -> {
                val weeks = days / 7
                "$emoji $weeks week${if (weeks == 1) "" else "s"} away!"
            }
            else -> {
                val months = max(days / 30, 1)
                "$emoji $months month${if (months == 1) "" else "s"} away!"
            }
        }
    }

    private fun parse(iso: String?): LocalDate? {
        if (iso.isNullOrBlank()) return null
        return runCatching { Instant.parse(iso).atZone(ZoneId.systemDefault()).toLocalDate() }.getOrNull()
            ?: runCatching { LocalDate.parse(iso.take(10)) }.getOrNull()
    }
}

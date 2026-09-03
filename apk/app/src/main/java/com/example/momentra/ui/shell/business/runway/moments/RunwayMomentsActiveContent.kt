package com.example.momentra.ui.shell.business.runway.moments

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.api.ActivityItemDto
import com.example.momentra.data.api.BusinessFinancePayloadDto
import com.example.momentra.data.api.BusinessTimelineDto
import com.example.momentra.data.api.BusinessTimelineItemDto
import com.example.momentra.data.api.MomDeltasDto
import com.example.momentra.data.api.ProgressSnapshotDto
import com.example.momentra.data.repository.BusinessSliceRepository
import com.example.momentra.data.security.BalanceMask
import com.example.momentra.data.security.SecurityPreferences
import com.example.momentra.ui.shell.business.shared.BusinessActiveTheme
import com.example.momentra.ui.shell.business.runway.components.RunwayActivityRow
import com.example.momentra.ui.shell.business.runway.components.RunwayBackgroundGlow
import com.example.momentra.ui.shell.business.runway.components.RunwayColors
import com.example.momentra.ui.shell.business.runway.components.RunwayFilterChipRow
import com.example.momentra.ui.shell.business.runway.components.RunwayGradientPrimaryButton
import com.example.momentra.ui.shell.business.runway.components.RunwayProgressSnapshot
import com.example.momentra.ui.shell.business.runway.components.RunwayTimelineEntryRow
import com.example.momentra.ui.shell.business.runway.components.RunwayTimelineHeroCard
import com.example.momentra.ui.theme.PlusJakartaSans
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

private val BaseFilters = listOf("All", "Revenue", "Expenses")

/** Figma `692:37078` — Financial Timeline; timeline primary + activity fallback. */
@Composable
fun RunwayMomentsActiveContent(
    momentId: String?,
    momentTitle: String?,
    refreshToken: Long = 0L,
    onLogExpense: () -> Unit = {},
    onOpenQuickAdd: () -> Unit = {},
    repository: BusinessSliceRepository = remember { BusinessSliceRepository() },
    modifier: Modifier = Modifier,
) {
    val theme = BusinessActiveTheme.BusinessRunway
    var loading by remember { mutableStateOf(true) }
    var activities by remember { mutableStateOf<List<ActivityItemDto>>(emptyList()) }
    var timeline by remember { mutableStateOf<BusinessTimelineDto?>(null) }
    var finance by remember { mutableStateOf<BusinessFinancePayloadDto?>(null) }
    var progressSnapshot by remember { mutableStateOf<ProgressSnapshotDto?>(null) }
    var momDeltas by remember { mutableStateOf<MomDeltasDto?>(null) }
    var filter by remember { mutableStateOf("All") }
    var error by remember { mutableStateOf<String?>(null) }
    val hide = SecurityPreferences(LocalContext.current).hideBalances()

    LaunchedEffect(refreshToken, momentId) {
        if (momentId.isNullOrBlank()) {
            loading = false
            activities = emptyList()
            progressSnapshot = null
            momDeltas = null
            error = "Select a Business Moment."
            return@LaunchedEffect
        }
        loading = activities.isEmpty() && timeline?.items.isNullOrEmpty()
        error = null
        coroutineScope {
            val activityDef = async { repository.getActivity(momentId) }
            val timelineDef = async { repository.getMomentTimeline(momentId) }
            val financeDef = async { repository.getFinance(momentId) }
            val progressDef = async { repository.getProgressSnapshot(momentId) }
            val deltasDef = async { repository.getMomDeltas(momentId) }
            timelineDef.await().fold(
                onSuccess = { timeline = it },
                onFailure = { /* activity fallback */ },
            )
            activityDef.await().fold(
                onSuccess = { activities = it.items },
                onFailure = { error = it.message },
            )
            financeDef.await().fold(
                onSuccess = { finance = it.payload },
                onFailure = { /* optional */ },
            )
            progressDef.await().fold(
                onSuccess = { progressSnapshot = it },
                onFailure = { /* optional — keep existing ratios */ },
            )
            deltasDef.await().fold(
                onSuccess = { momDeltas = it },
                onFailure = { /* optional */ },
            )
        }
        loading = false
    }

    val timelineItems = timeline?.items.orEmpty()
    val kpis = timeline?.kpis

    if (loading && activities.isEmpty() && timelineItems.isEmpty()) {
        Box(modifier.fillMaxSize().background(theme.bg), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = theme.accent)
        }
        return
    }

    fun matchesFilter(hay: String, f: String): Boolean {
        val h = hay.lowercase()
        return when (f.lowercase()) {
            "revenue" -> h.contains("revenue") || h.contains("invoice") || h.contains("collection")
            "expenses" -> h.contains("expense") || h.contains("spend") || h.contains("burn") ||
                h.contains("cost")
            else -> true
        }
    }

    val filteredTimeline = remember(timelineItems, filter) {
        if (filter == "All") timelineItems
        else timelineItems.filter {
            matchesFilter("${it.title} ${it.category} ${it.eventType}", filter)
        }
    }
    val filteredActivities = remember(activities, filter) {
        if (filter == "All") activities
        else activities.filter {
            matchesFilter("${it.title} ${it.activityCode}", filter)
        }
    }
    val hasTimeline = timelineItems.isNotEmpty()
    val showEmpty = if (hasTimeline) filteredTimeline.isEmpty() else filteredActivities.isEmpty()

    val entryCount = when {
        timelineItems.isNotEmpty() -> "${timelineItems.size}"
        activities.isNotEmpty() -> "${activities.size}"
        else -> "—"
    }
    val totals = finance?.totals?.firstOrNull()
    val revenue = totals?.revenueTotal?.takeIf { it.isNotBlank() && it != "0" }
        ?.let { BalanceMask.mask(it, hide) }
        ?: kpis?.updateCount?.takeIf { it > 0 }?.toString()
        ?: "—"
    val activityChip = kpis?.spendEvents?.takeIf { it > 0 }?.toString()
        ?: activities.size.takeIf { it > 0 }?.toString()
        ?: "—"

    val highlights = if (timelineItems.isNotEmpty()) {
        timelineItems.filter {
            val t = it.eventType.uppercase()
            t.contains("REVENUE") || t.contains("MILESTONE") || t.contains("INVOICE") ||
                t.contains("UPDATE")
        }.take(3)
    } else {
        activities.filter {
            val code = it.activityCode.uppercase()
            code.contains("REVENUE") || code.contains("MILESTONE") || code.contains("INVOICE") ||
                code.contains("UPDATE")
        }.take(3).map { item ->
            BusinessTimelineItemDto(
                eventId = item.activityPayload?.activityId ?: item.activityCode,
                eventType = item.activityCode,
                title = item.title.ifBlank { item.activityCode },
                category = item.activityCode,
                occurredAt = item.occurredAt,
            )
        }
    }

    val burnRatio = kpis?.spendEvents?.takeIf { it > 0 }?.let { (it / 20f).coerceIn(0f, 1f) }
    val collectionsRatio = progressSnapshot?.collections
        ?.takeIf { it.isNotEmpty() }
        ?.let { (it.size / 5f).coerceIn(0f, 1f) }
    val healthRatio = when (progressSnapshot?.health?.uppercase()) {
        "STRONG" -> 1f
        "STABLE" -> 0.7f
        else -> null
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(theme.bg),
    ) {
        RunwayBackgroundGlow(modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .padding(bottom = 56.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            error?.let {
                Text(it, color = RunwayColors.Red, fontSize = 12.sp, fontFamily = PlusJakartaSans)
            }

            RunwayTimelineHeroCard(
                entries = entryCount,
                revenue = revenue,
                savings = activityChip,
                theme = theme,
            )

            RunwayFilterChipRow(
                chips = BaseFilters,
                selected = filter,
                onSelect = { filter = it },
                theme = theme,
            )

            if (showEmpty) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(theme.card)
                        .border(1.dp, theme.border, RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        "Nothing on the financial timeline yet",
                        color = theme.text,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        fontFamily = PlusJakartaSans,
                    )
                    Text(
                        "Revenues, expenses, and milestones appear after live writes.",
                        color = theme.secondary,
                        fontSize = 13.sp,
                        fontFamily = PlusJakartaSans,
                    )
                }
            } else if (hasTimeline) {
                filteredTimeline.forEach { RunwayTimelineEntryRow(item = it, theme = theme) }
            } else {
                filteredActivities.forEach { RunwayActivityRow(item = it, theme = theme) }
            }

            if (!showEmpty) {
                Text(
                    "See Full History →",
                    color = RunwayColors.LinkAmber,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = PlusJakartaSans,
                    modifier = Modifier.clickable(onClick = onOpenQuickAdd),
                )
            }

            RunwayProgressSnapshot(
                burnRatio = burnRatio,
                collectionsRatio = collectionsRatio,
                healthRatio = healthRatio,
                theme = theme,
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Recent Highlights",
                    color = theme.text,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = PlusJakartaSans,
                )
                Text(
                    "Key financial wins from this period.",
                    color = theme.muted,
                    fontSize = 11.sp,
                    fontFamily = PlusJakartaSans,
                )
                if (highlights.isEmpty()) {
                    Text(
                        "Highlights appear from live revenues, milestones, and updates.",
                        color = theme.secondary,
                        fontSize = 12.sp,
                        fontFamily = PlusJakartaSans,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(theme.card)
                            .border(1.dp, theme.border, RoundedCornerShape(12.dp))
                            .padding(12.dp),
                    )
                } else {
                    highlights.forEach { item ->
                        Text(
                            item.title.ifBlank { item.eventType },
                            color = theme.text,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = PlusJakartaSans,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(theme.card)
                                .border(1.dp, theme.border, RoundedCornerShape(12.dp))
                                .padding(12.dp),
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(theme.card)
                    .border(1.dp, theme.border, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    "Log spend or revenue to keep the financial timeline current.",
                    color = theme.secondary,
                    fontSize = 13.sp,
                    fontFamily = PlusJakartaSans,
                )
                RunwayGradientPrimaryButton(
                    label = "Log Expense",
                    enabled = !momentId.isNullOrBlank(),
                    onClick = onLogExpense,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

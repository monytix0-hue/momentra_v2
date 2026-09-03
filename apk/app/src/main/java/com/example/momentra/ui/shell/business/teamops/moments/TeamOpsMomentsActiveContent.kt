package com.example.momentra.ui.shell.business.teamops.moments

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.api.ActivityItemDto
import com.example.momentra.data.api.BusinessTimelineDto
import com.example.momentra.data.api.BusinessTimelineItemDto
import com.example.momentra.data.api.CapacityDto
import com.example.momentra.data.repository.BusinessSliceRepository
import com.example.momentra.ui.shell.business.shared.BusinessActiveTheme
import com.example.momentra.ui.shell.business.shared.BusinessTabDataCache
import com.example.momentra.ui.shell.business.teamops.components.TeamOpsActivityRow
import com.example.momentra.ui.shell.business.teamops.components.TeamOpsBackgroundGlow
import com.example.momentra.ui.shell.business.teamops.components.TeamOpsColors
import com.example.momentra.ui.shell.business.teamops.components.TeamOpsFilterChipRow
import com.example.momentra.ui.shell.business.teamops.components.TeamOpsGradientPrimaryButton
import com.example.momentra.ui.shell.business.teamops.components.TeamOpsProgressSnapshot
import com.example.momentra.ui.shell.business.teamops.components.TeamOpsTimelineEntryRow
import com.example.momentra.ui.shell.business.teamops.components.TeamOpsTimelineHeroCard
import com.example.momentra.ui.theme.PlusJakartaSans
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

private val BaseFilters = listOf("All", "Milestones", "Decisions", "Deliveries")

/** Figma `692:35199` — Team Timeline; live timeline + activity; honest empty KPIs. */
@Composable
fun TeamOpsMomentsActiveContent(
    momentId: String?,
    momentTitle: String?,
    refreshToken: Long = 0L,
    onLogWin: () -> Unit = {},
    onOpenQuickAdd: () -> Unit = {},
    repository: BusinessSliceRepository = remember { BusinessSliceRepository() },
    modifier: Modifier = Modifier,
) {
    val theme = BusinessActiveTheme.TeamOperations
    var loading by remember { mutableStateOf(true) }
    var activities by remember { mutableStateOf<List<ActivityItemDto>>(emptyList()) }
    var timeline by remember { mutableStateOf<BusinessTimelineDto?>(null) }
    var capacityData by remember { mutableStateOf<CapacityDto?>(null) }
    var filter by remember { mutableStateOf("All") }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(refreshToken, momentId) {
        if (momentId.isNullOrBlank()) {
            loading = false
            activities = emptyList()
            capacityData = null
            error = "Select a Business Moment."
            return@LaunchedEffect
        }
        loading = activities.isEmpty() && timeline?.items.isNullOrEmpty()
        error = null
        BusinessTabDataCache.peekPulse(momentId)?.activities?.let { cached ->
            if (activities.isEmpty()) activities = cached
        }
        coroutineScope {
            val activityDef = async { repository.getActivity(momentId) }
            val timelineDef = async { repository.getMomentTimeline(momentId) }
            val capacityDef = async { repository.getCapacity(momentId) }
            timelineDef.await().fold(
                onSuccess = { timeline = it },
                onFailure = { /* activity fallback */ },
            )
            activityDef.await().fold(
                onSuccess = { activities = it.items },
                onFailure = { error = it.message },
            )
            capacityDef.await().fold(
                onSuccess = { capacityData = it },
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
            "milestones" -> h.contains("milestone") || h.contains("ship") || h.contains("release")
            "decisions" -> h.contains("decision") || h.contains("align") || h.contains("approval")
            "deliveries" -> h.contains("deliver") || h.contains("ship") || h.contains("update") ||
                h.contains("expense") || h.contains("win")
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

    val members = "—"
    val pending = kpis?.highPriorityIssues?.takeIf { it > 0 }?.toString()
        ?: kpis?.updateCount?.takeIf { it > 0 }?.toString()
        ?: "—"
    val issues = kpis?.issueCount?.takeIf { it > 0 }?.toString()
        ?: activities.count {
            val h = (it.title + it.activityCode).uppercase()
            h.contains("ISSUE") || h.contains("BLOCK")
        }.takeIf { it > 0 }?.toString()
        ?: "—"

    val highlights = if (timelineItems.isNotEmpty()) {
        timelineItems.filter {
            val t = it.eventType.uppercase()
            t.contains("UPDATE") || t.contains("MILESTONE") || t.contains("IMPROVEMENT") || t.contains("SHIP")
        }.take(3)
    } else {
        activities.filter {
            val code = it.activityCode.uppercase()
            code.contains("UPDATE") || code.contains("MILESTONE") || code.contains("IMPROVEMENT")
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

    val deliveryRatio = kpis?.updateCount?.takeIf { it > 0 }?.let { (it / 15f).coerceIn(0f, 1f) }
    val capacityRatio: Float? = capacityData?.capacityPct
        ?.toFloat()?.div(100f)?.coerceIn(0f, 1f)
    val approvalsRatio = kpis?.let {
        if (it.issueCount <= 0) null
        else 1f - (it.highPriorityIssues.toFloat() / it.issueCount.coerceAtLeast(1))
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(theme.bg),
    ) {
        TeamOpsBackgroundGlow(modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .padding(bottom = 56.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            error?.let {
                Text(it, color = TeamOpsColors.Red, fontSize = 12.sp, fontFamily = PlusJakartaSans)
            }

            TeamOpsTimelineHeroCard(
                members = members,
                pending = pending,
                issues = issues,
                theme = theme,
            )

            TeamOpsFilterChipRow(
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
                        "Nothing on the team timeline yet",
                        color = theme.text,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        fontFamily = PlusJakartaSans,
                    )
                    Text(
                        "Milestones, decisions, and deliveries appear after live writes.",
                        color = theme.secondary,
                        fontSize = 13.sp,
                        fontFamily = PlusJakartaSans,
                    )
                }
            } else if (hasTimeline) {
                filteredTimeline.forEach { TeamOpsTimelineEntryRow(item = it, theme = theme) }
            } else {
                filteredActivities.forEach { TeamOpsActivityRow(item = it, theme = theme) }
            }

            if (!showEmpty) {
                Text(
                    "See Full History →",
                    color = TeamOpsColors.LinkBlue,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = PlusJakartaSans,
                    modifier = Modifier.clickable(onClick = onOpenQuickAdd),
                )
            }

            TeamOpsProgressSnapshot(
                deliveryRatio = deliveryRatio,
                capacityRatio = capacityRatio,
                approvalsRatio = approvalsRatio,
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
                    "Key wins from this week.",
                    color = theme.muted,
                    fontSize = 11.sp,
                    fontFamily = PlusJakartaSans,
                )
                if (highlights.isEmpty()) {
                    Text(
                        "Highlights appear from live updates and milestones.",
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
                    "Record what just happened. Review open items or record a delivery update.",
                    color = theme.secondary,
                    fontSize = 13.sp,
                    fontFamily = PlusJakartaSans,
                )
                TeamOpsGradientPrimaryButton(
                    label = "Log a Win",
                    enabled = !momentId.isNullOrBlank(),
                    onClick = onLogWin,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "See Full History →",
                    color = TeamOpsColors.LinkBlue,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = PlusJakartaSans,
                    modifier = Modifier.clickable(onClick = onOpenQuickAdd),
                )
            }
        }
    }
}

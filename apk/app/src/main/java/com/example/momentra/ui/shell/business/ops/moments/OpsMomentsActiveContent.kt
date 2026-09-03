package com.example.momentra.ui.shell.business.ops.moments

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
import com.example.momentra.data.repository.BusinessSliceRepository
import com.example.momentra.ui.shell.business.shared.BusinessActiveTheme
import com.example.momentra.ui.shell.business.ops.components.OpsActivityTimelineRow
import com.example.momentra.ui.shell.business.ops.components.OpsBackgroundGlow
import com.example.momentra.ui.shell.business.ops.components.OpsColors
import com.example.momentra.ui.shell.business.ops.components.OpsFilterChipRow
import com.example.momentra.ui.shell.business.ops.components.OpsGradientPrimaryButton
import com.example.momentra.ui.shell.business.ops.components.OpsProgressSnapshot
import com.example.momentra.ui.shell.business.ops.components.OpsTimelineEntryRow
import com.example.momentra.ui.shell.business.ops.components.OpsTimelineHeroCard
import com.example.momentra.ui.theme.PlusJakartaSans
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

private val BaseFilters = listOf("Budget", "Vendors", "Issues", "Updates")

/** Figma `692:44116` Business Operations Moments — live activity timeline. */
@Composable
fun OpsMomentsActiveContent(
    momentId: String?,
    momentTitle: String?,
    refreshToken: Long = 0L,
    onLogSpend: () -> Unit = {},
    onOpenQuickAdd: () -> Unit = {},
    repository: BusinessSliceRepository = remember { BusinessSliceRepository() },
    modifier: Modifier = Modifier,
) {
    val theme = BusinessActiveTheme.BusinessOperations
    var loading by remember { mutableStateOf(true) }
    var activities by remember { mutableStateOf<List<ActivityItemDto>>(emptyList()) }
    var timeline by remember { mutableStateOf<BusinessTimelineDto?>(null) }
    var filter by remember { mutableStateOf("All") }
    var error by remember { mutableStateOf<String?>(null) }

    val filterChips = remember(momentTitle) {
        val scopeLabel = momentTitle?.takeIf { it.isNotBlank() }?.let {
            if (it.length > 14) it.take(12) + "…" else it
        } ?: "All ops"
        listOf(scopeLabel) + BaseFilters
    }

    LaunchedEffect(refreshToken, momentId) {
        if (momentId.isNullOrBlank()) {
            loading = false
            activities = emptyList()
            error = "Select a Business Moment."
            return@LaunchedEffect
        }
        loading = activities.isEmpty()
        error = null
        coroutineScope {
            val activityDef = async { repository.getActivity(momentId) }
            val timelineDef = async { repository.getMomentTimeline(momentId) }
            timelineDef.await().fold(
                onSuccess = { timeline = it },
                onFailure = { /* activity fallback */ },
            )
            activityDef.await().fold(
                onSuccess = { activities = it.items },
                onFailure = { error = it.message },
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

    fun matchesFilter(item: ActivityItemDto, f: String): Boolean {
        val hay = (item.title + " " + item.activityCode).lowercase()
        return when (f.lowercase()) {
            "budget", "spend" -> hay.contains("spend") || hay.contains("expense") || hay.contains("budget")
            "vendors" -> hay.contains("vendor") || hay.contains("contract") || hay.contains("sla")
            "issues" -> hay.contains("issue") || hay.contains("blocker") || hay.contains("risk")
            "updates" -> hay.contains("update") || hay.contains("approval") || hay.contains("improvement")
            else -> true
        }
    }

    val activeFilter = if (filter == "All") filterChips.firstOrNull() ?: "All" else filter

    val filteredActivities = remember(activities, activeFilter, filter) {
        if (filter == "All") activities
        else activities.filter { matchesFilter(it, activeFilter) }
    }
    val filteredTimeline = remember(timelineItems, activeFilter, filter) {
        if (filter == "All") timelineItems
        else {
            timelineItems.filter { item ->
                val hay = (item.title + " " + item.category + " " + item.eventType).lowercase()
                when (activeFilter.lowercase()) {
                    "budget", "spend" -> hay.contains("spend") || hay.contains("expense") || item.eventType == "EXPENSE"
                    "vendors" -> hay.contains("vendor") || hay.contains("contract") || hay.contains("sla")
                    "issues" -> hay.contains("issue") || item.eventType == "ISSUE"
                    "updates" -> hay.contains("update") || item.eventType == "UPDATE" || item.eventType == "IMPROVEMENT"
                    else -> true
                }
            }
        }
    }
    val hasTimeline = timelineItems.isNotEmpty()
    val showEmpty = if (hasTimeline) filteredTimeline.isEmpty() else filteredActivities.isEmpty()

    val entryCount = timelineItems.size.takeIf { it > 0 }
        ?: activities.size
    val vendorCount = kpis?.vendorCount ?: kpis?.activeContracts
        ?: activities.count { matchesFilter(it, "Vendors") }
    val issueCount = kpis?.issueCount ?: activities.count { matchesFilter(it, "Issues") }

    val highlights = if (timelineItems.isNotEmpty()) {
        timelineItems.filter {
            val t = it.eventType.uppercase()
            t.contains("ISSUE") || t.contains("IMPROVEMENT") || t.contains("UPDATE")
        }.take(3)
    } else {
        activities.filter {
            val code = it.activityCode.uppercase()
            code.contains("ISSUE") || code.contains("IMPROVEMENT") || code.contains("UPDATE")
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

    val budgetRatio = kpis?.spendEvents?.takeIf { it > 0 }?.let { (it / 20f).coerceIn(0f, 1f) }
        ?: if (activities.isEmpty()) null else filteredActivities.size.toFloat() / activities.size.coerceAtLeast(1)
    val issuesRatio = kpis?.let {
        if (it.issueCount <= 0) null
        else 1f - (it.highPriorityIssues.toFloat() / it.issueCount.coerceAtLeast(1))
    }
    val milestonesRatio = kpis?.updateCount?.takeIf { it > 0 }?.let { (it / 15f).coerceIn(0f, 1f) }
        ?: if (timelineItems.isEmpty()) null else highlights.size.toFloat() / timelineItems.size.coerceAtLeast(1)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(theme.bg),
    ) {
        OpsBackgroundGlow(modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .padding(bottom = 56.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            error?.let {
                Text(it, color = OpsColors.Red, fontSize = 12.sp, fontFamily = PlusJakartaSans)
            }

            OpsTimelineHeroCard(
                entries = entryCount,
                vendors = vendorCount,
                issues = issueCount,
                theme = theme,
            )

            OpsFilterChipRow(
                chips = filterChips,
                selected = if (filter == "All") filterChips.first() else filter,
                onSelect = { chip ->
                    filter = if (chip == filterChips.first()) "All" else chip
                },
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
                        "Nothing on the ops timeline yet",
                        color = theme.text,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        fontFamily = PlusJakartaSans,
                    )
                    Text(
                        "Spend, vendors, issues, and updates appear after live writes.",
                        color = theme.secondary,
                        fontSize = 13.sp,
                        fontFamily = PlusJakartaSans,
                    )
                }
            } else if (hasTimeline) {
                filteredTimeline.forEach { item ->
                    OpsTimelineEntryRow(item = item, theme = theme)
                }
            } else {
                filteredActivities.forEach { item ->
                    OpsActivityTimelineRow(item = item, theme = theme)
                }
            }

            if (!showEmpty) {
                Text(
                    "See full history →",
                    color = OpsColors.LinkBlue,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = PlusJakartaSans,
                    modifier = Modifier.clickable(onClick = onOpenQuickAdd),
                )
            }

            OpsProgressSnapshot(
                budgetRatio = budgetRatio,
                issuesRatio = issuesRatio,
                milestonesRatio = milestonesRatio,
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
                if (highlights.isEmpty()) {
                    Text(
                        "Highlights appear from live issues, improvements, and updates.",
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

            OpsGradientPrimaryButton(
                label = "+ Log Spend",
                enabled = !momentId.isNullOrBlank(),
                onClick = onLogSpend,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

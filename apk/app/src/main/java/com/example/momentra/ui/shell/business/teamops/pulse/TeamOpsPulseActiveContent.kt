package com.example.momentra.ui.shell.business.teamops.pulse

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.api.ActivityItemDto
import com.example.momentra.data.api.BusinessLifePayloadDto
import com.example.momentra.data.api.BusinessPulsePayloadDto
import com.example.momentra.data.api.CapacityDto
import com.example.momentra.data.api.WorkloadDto
import com.example.momentra.data.repository.BusinessSliceRepository
import com.example.momentra.ui.shell.business.shared.BusinessActiveTheme
import com.example.momentra.ui.shell.business.teamops.components.TeamOpsActivityRow
import com.example.momentra.ui.shell.business.teamops.components.TeamOpsAttentionCard
import com.example.momentra.ui.shell.business.teamops.components.TeamOpsBackgroundGlow
import com.example.momentra.ui.shell.business.teamops.components.TeamOpsColors
import com.example.momentra.ui.shell.business.teamops.components.TeamOpsGradientPrimaryButton
import com.example.momentra.ui.shell.business.teamops.components.TeamOpsHeroHealthRing
import com.example.momentra.ui.shell.business.teamops.components.TeamOpsIntelligenceSection
import com.example.momentra.ui.shell.business.teamops.components.TeamOpsOutlineButton
import com.example.momentra.ui.shell.business.teamops.components.TeamOpsTintedMetricTile
import com.example.momentra.ui.shell.business.teamops.components.TeamOpsWorkloadSection
import com.example.momentra.ui.shell.business.shared.BusinessTabDataCache
import com.example.momentra.ui.shell.business.shared.loadBusinessPulseTab
import com.example.momentra.ui.shell.perf.ShellPerf
import com.example.momentra.ui.theme.PlusJakartaSans

/** Figma `692:34967` — section stack; live bind; honest empties for workload/AI. */
@Composable
fun TeamOpsPulseActiveContent(
    momentId: String?,
    momentTitle: String?,
    refreshToken: Long,
    onLogDelivery: () -> Unit = {},
    onOpenQuickAdd: () -> Unit = {},
    repository: BusinessSliceRepository = remember { BusinessSliceRepository() },
    modifier: Modifier = Modifier,
) {
    val theme = BusinessActiveTheme.TeamOperations
    var loading by remember { mutableStateOf(true) }
    var pulse by remember { mutableStateOf<BusinessPulsePayloadDto?>(null) }
    var life by remember { mutableStateOf<BusinessLifePayloadDto?>(null) }
    var activities by remember { mutableStateOf<List<ActivityItemDto>>(emptyList()) }
    var capacityData by remember { mutableStateOf<CapacityDto?>(null) }
    var workloadData by remember { mutableStateOf<WorkloadDto?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(refreshToken, momentId) {
        if (momentId.isNullOrBlank()) {
            loading = false
            pulse = null
            life = null
            activities = emptyList()
            capacityData = null
            workloadData = null
            error = "Select a Business Moment."
            return@LaunchedEffect
        }
        error = null
        BusinessTabDataCache.peekPulse(momentId)?.let { cached ->
            pulse = cached.pulse
            life = cached.life
            activities = cached.activities
            capacityData = cached.capacity
            workloadData = cached.workload
            loading = false
        } ?: run { loading = pulse == null }
        loadBusinessPulseTab(
            repository = repository,
            momentId = momentId,
            fetchTeamOpsMetrics = true,
        ).fold(
            onSuccess = { data ->
                pulse = data.pulse
                life = data.life
                activities = data.activities
                capacityData = data.capacity
                workloadData = data.workload
                loading = false
            },
            onFailure = { e ->
                error = e.message
                loading = false
            },
        )
    }

    if (loading && pulse == null) {
        Box(modifier.fillMaxSize().background(theme.bg), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = theme.accent)
        }
        return
    }

    val healthScore = pulse?.financialHealthScore?.trim()?.takeIf { it.isNotEmpty() } ?: "—"
    val hasLive = healthScore != "—"
    val attentionCount = pulse?.attentionCount ?: 0
    val memberFromLife = life?.teamOperationsPayload?.get("memberCapacity")?.toString()
        ?.trim()?.takeIf { it.isNotEmpty() }
    val members = memberFromLife
        ?: pulse?.activeMomentCount?.takeIf { it > 0 }?.toString()
        ?: "—"
    val capacityPct = capacityData?.capacityPct?.toString()
        ?.trim()?.takeIf { it.isNotEmpty() && it != "null" }
    val capacity = capacityPct?.let { "${it}%" } ?: "—"
    val openItems = if (attentionCount > 0) "$attentionCount" else "—"

    val narrative = when (val n = healthScore.toDoubleOrNull()) {
        null -> if (attentionCount > 0) "Needs attention" else "Awaiting live health signal"
        in 80.0..Double.MAX_VALUE -> "Strong & Stable"
        in 50.0..79.999 -> "Needs focus"
        else -> "At risk"
    }
    val subtitle = when {
        attentionCount > 0 ->
            "$attentionCount item${if (attentionCount == 1) "" else "s"} need your eye today."
        hasLive -> "Execution health from live pulse."
        else -> "Status updates as team activity projects."
    }

    val attentionActs = activities.filter {
        val hay = (it.title + " " + it.activityCode).uppercase()
        hay.contains("ISSUE") || hay.contains("BLOCK") || hay.contains("RISK") || hay.contains("APPROVAL")
    }.take(5)

    val deliveryActs = activities.take(5)

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
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            error?.let {
                Text(it, color = TeamOpsColors.Red, fontSize = 12.sp, fontFamily = PlusJakartaSans)
            }

            TeamOpsHealthHeroCard(
                theme = theme,
                healthScore = healthScore,
                showLive = hasLive,
                narrative = narrative,
                subtitle = subtitle,
                members = members,
                capacity = capacity,
                openItems = openItems,
            )

            TeamOpsWorkloadSection(theme = theme, workloadData = workloadData)

            TeamOpsNeedsAttentionSection(
                theme = theme,
                attentionCount = attentionCount,
                activities = attentionActs,
            )

            TeamOpsRecentDeliverySection(
                theme = theme,
                activities = deliveryActs,
                onViewAll = onOpenQuickAdd,
            )

            TeamOpsIntelligenceSection(theme = theme)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TeamOpsGradientPrimaryButton(
                    label = "+ Log Delivery",
                    enabled = !momentId.isNullOrBlank(),
                    onClick = onLogDelivery,
                    modifier = Modifier.weight(1f),
                )
                TeamOpsOutlineButton(
                    label = "View This Week's Report",
                    enabled = true,
                    onClick = onOpenQuickAdd,
                    theme = theme,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun TeamOpsHealthHeroCard(
    theme: BusinessActiveTheme,
    healthScore: String,
    showLive: Boolean,
    narrative: String,
    subtitle: String,
    members: String,
    capacity: String,
    openItems: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF161B26), Color(0xFF1A1F2E))))
            .border(1.dp, theme.border, RoundedCornerShape(20.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TeamOpsHeroHealthRing(
                score = healthScore,
                showLive = showLive,
                theme = theme,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    "EXECUTION HEALTH",
                    color = theme.muted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = PlusJakartaSans,
                )
                Text(
                    narrative,
                    color = theme.text,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = PlusJakartaSans,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    subtitle,
                    color = theme.secondary,
                    fontSize = 13.sp,
                    fontFamily = PlusJakartaSans,
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TeamOpsTintedMetricTile(
                value = members,
                label = "members",
                detail = "Team capacity",
                tint = TeamOpsColors.Lavender,
                theme = theme,
                modifier = Modifier.weight(1f),
            )
            TeamOpsTintedMetricTile(
                value = capacity,
                label = "capacity",
                detail = if (capacity != "—") "Team utilisation" else "API pending",
                tint = TeamOpsColors.Emerald,
                theme = theme,
                valueColor = TeamOpsColors.Emerald,
                modifier = Modifier.weight(1f),
            )
            TeamOpsTintedMetricTile(
                value = openItems,
                label = "open items",
                detail = "Needs attention",
                tint = TeamOpsColors.Amber,
                theme = theme,
                valueColor = TeamOpsColors.Amber,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun TeamOpsNeedsAttentionSection(
    theme: BusinessActiveTheme,
    attentionCount: Int,
    activities: List<ActivityItemDto>,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Needs Attention",
                color = theme.text,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = PlusJakartaSans,
                modifier = Modifier.weight(1f),
            )
            val badge = maxOf(attentionCount, activities.size)
            if (badge > 0) {
                Text(
                    "$badge",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = PlusJakartaSans,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(TeamOpsColors.Red)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
        }
        if (activities.isEmpty()) {
            Text(
                "No items need attention",
                color = theme.secondary,
                fontSize = 13.sp,
                fontFamily = PlusJakartaSans,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(theme.card)
                    .border(1.dp, theme.border, RoundedCornerShape(16.dp))
                    .padding(16.dp),
            )
        } else {
            activities.forEachIndexed { index, act ->
                TeamOpsAttentionCard(
                    title = act.title.ifBlank { act.activityCode },
                    severity = if (index == 0) "HIGH" else "MED",
                    detail = act.occurredAt.take(16).ifBlank { act.activityCode },
                    theme = theme,
                )
            }
        }
    }
}

@Composable
private fun TeamOpsRecentDeliverySection(
    theme: BusinessActiveTheme,
    activities: List<ActivityItemDto>,
    onViewAll: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Recent Delivery",
                color = theme.text,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = PlusJakartaSans,
                modifier = Modifier.weight(1f),
            )
            Text(
                "This Week",
                color = theme.secondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
                modifier = Modifier
                    .border(1.dp, theme.border, RoundedCornerShape(999.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
        if (activities.isEmpty()) {
            Text(
                "Deliveries appear as team updates and activity project.",
                color = theme.secondary,
                fontSize = 13.sp,
                fontFamily = PlusJakartaSans,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(theme.card)
                    .border(1.dp, theme.border, RoundedCornerShape(16.dp))
                    .padding(16.dp),
            )
        } else {
            activities.forEach { TeamOpsActivityRow(item = it, theme = theme) }
            Text(
                "View all activity →",
                color = TeamOpsColors.LinkBlue,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 4.dp)
                    .clickable(onClick = onViewAll),
            )
        }
    }
}

package com.example.momentra.ui.shell.business.ops.pulse

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
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
import com.example.momentra.data.api.BusinessPulsePayloadDto
import com.example.momentra.data.api.OpsAttentionDto
import com.example.momentra.data.repository.BusinessSliceRepository
import com.example.momentra.ui.shell.business.shared.BusinessActiveTheme
import com.example.momentra.ui.shell.business.ops.components.OpsActivityTimelineSection
import com.example.momentra.ui.shell.business.ops.components.OpsAttentionCard
import com.example.momentra.ui.shell.business.ops.components.OpsBackgroundGlow
import com.example.momentra.ui.shell.business.ops.components.OpsCategoryBarSection
import com.example.momentra.ui.shell.business.ops.components.OpsColors
import com.example.momentra.ui.shell.business.ops.components.OpsGradientPrimaryButton
import com.example.momentra.ui.shell.business.ops.components.OpsHeroHealthRing
import com.example.momentra.ui.shell.business.ops.components.OpsIntelligenceSection
import com.example.momentra.ui.shell.business.ops.components.OpsOutlineButton
import com.example.momentra.ui.shell.business.ops.components.OpsTintedMetricTile
import com.example.momentra.ui.shell.business.shared.BusinessTabDataCache
import com.example.momentra.ui.shell.business.shared.loadBusinessPulseTab
import com.example.momentra.ui.shell.perf.ShellPerf
import com.example.momentra.ui.theme.PlusJakartaSans

/** Figma `692:43993` Business Operations Pulse — live ops bind; honest empties. */
@Composable
fun OpsPulseActiveContent(
    momentId: String?,
    momentTitle: String?,
    refreshToken: Long,
    onLogSpend: () -> Unit = {},
    onOpenQuickAdd: () -> Unit = {},
    repository: BusinessSliceRepository = remember { BusinessSliceRepository() },
    modifier: Modifier = Modifier,
) {
    val theme = BusinessActiveTheme.BusinessOperations
    var loading by remember { mutableStateOf(true) }
    var pulse by remember { mutableStateOf<BusinessPulsePayloadDto?>(null) }
    var activities by remember { mutableStateOf<List<ActivityItemDto>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(refreshToken, momentId) {
        if (momentId.isNullOrBlank()) {
            loading = false
            pulse = null
            activities = emptyList()
            error = "Select a Business Moment."
            return@LaunchedEffect
        }
        error = null
        BusinessTabDataCache.peekPulse(momentId)?.let { cached ->
            pulse = cached.pulse
            activities = cached.activities
            loading = false
        } ?: run { loading = pulse == null }
        loadBusinessPulseTab(repository, momentId).fold(
            onSuccess = { data ->
                pulse = data.pulse
                activities = data.activities
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

    val ops = pulse?.operations
    val openIssues = ops?.openIssueCount ?: 0
    val slaPct = ops?.slaCompliancePct
    val healthScore = slaPct?.toString() ?: "—"
    val hasLiveScore = slaPct != null
    val monthlySpend = ops?.monthlySpend?.trim()?.takeIf { it.isNotEmpty() && it != "0" && it != "0.00" }
    val vendors = ops?.activeVendorCount
    val spendCats = ops?.spendByCategory.orEmpty()
    val attention = ops?.needsAttention.orEmpty()

    val narrative = when (val n = slaPct) {
        null -> if (openIssues > 0) "Needs attention" else "Awaiting live ops signal"
        in 90..100 -> "Stable & Optimizing"
        in 70..89 -> "Needs focus"
        else -> "At risk"
    }
    val subtitle = when {
        openIssues > 0 -> "$openIssues open issue${if (openIssues == 1) "" else "s"} need attention"
        slaPct != null -> "SLA compliance at $slaPct%"
        else -> "Status from open issues and SLA checks"
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(theme.bg),
    ) {
        OpsBackgroundGlow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            error?.let {
                Text(it, color = OpsColors.Red, fontSize = 12.sp, fontFamily = PlusJakartaSans)
            }

            OpsHealthHeroCard(
                theme = theme,
                healthScore = healthScore,
                showLive = hasLiveScore,
                narrative = narrative,
                subtitle = subtitle,
                monthlySpend = monthlySpend ?: "—",
                vendors = vendors?.toString() ?: "—",
                sla = slaPct?.let { "$it%" } ?: "—",
            )

            OpsCategoryBarSection(categories = spendCats, theme = theme)

            OpsNeedsAttentionSection(
                theme = theme,
                attention = attention,
                activities = activities,
            )

            OpsActivityTimelineSection(
                activities = activities,
                onViewAll = onOpenQuickAdd,
                theme = theme,
            )

            OpsIntelligenceSection(theme = theme)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OpsGradientPrimaryButton(
                    label = "+ Log Delivery",
                    enabled = !momentId.isNullOrBlank(),
                    onClick = onLogSpend,
                    modifier = Modifier.weight(1f),
                )
                OpsOutlineButton(
                    label = "View Report",
                    enabled = true,
                    onClick = onOpenQuickAdd,
                    theme = theme,
                )
            }
        }
    }
}

@Composable
private fun OpsHealthHeroCard(
    theme: BusinessActiveTheme,
    healthScore: String,
    showLive: Boolean,
    narrative: String,
    subtitle: String,
    monthlySpend: String,
    vendors: String,
    sla: String,
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
            OpsHeroHealthRing(
                score = healthScore,
                showLive = showLive,
                theme = theme,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    "OPERATIONS HEALTH",
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
            OpsTintedMetricTile(
                value = monthlySpend,
                label = "monthly spend",
                detail = "Trend unavailable",
                tint = OpsColors.Green,
                theme = theme,
                modifier = Modifier.weight(1f),
            )
            OpsTintedMetricTile(
                value = vendors,
                label = "vendors",
                detail = "Trend unavailable",
                tint = OpsColors.IndigoLight,
                theme = theme,
                modifier = Modifier.weight(1f),
            )
            OpsTintedMetricTile(
                value = sla,
                label = "sla",
                detail = "Trend unavailable",
                tint = OpsColors.Amber,
                theme = theme,
                valueColor = OpsColors.Amber,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun OpsNeedsAttentionSection(
    theme: BusinessActiveTheme,
    attention: List<OpsAttentionDto>,
    activities: List<ActivityItemDto>,
) {
    val items = attention.ifEmpty {
        activities.filter {
            val hay = (it.title + " " + it.activityCode).uppercase()
            hay.contains("ISSUE") || hay.contains("BLOCK") || hay.contains("SLA")
        }.take(5).map { act ->
            OpsAttentionDto(
                title = act.title.ifBlank { act.activityCode },
                severity = "OPEN",
                issueId = act.activityPayload?.activityId,
            )
        }
    }
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
            if (items.isNotEmpty()) {
                Text(
                    "${items.size}",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = PlusJakartaSans,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(OpsColors.Red)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
        }
        if (items.isEmpty()) {
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
            items.forEach { item ->
                OpsAttentionCard(item = item, theme = theme)
            }
        }
    }
}

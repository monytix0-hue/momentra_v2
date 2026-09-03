package com.example.momentra.ui.shell.business.runway.pulse

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.api.ActivityItemDto
import com.example.momentra.data.api.BusinessFinancePayloadDto
import com.example.momentra.data.api.BusinessLifePayloadDto
import com.example.momentra.data.api.BusinessPulsePayloadDto
import com.example.momentra.data.repository.BusinessSliceRepository
import com.example.momentra.data.security.BalanceMask
import com.example.momentra.data.security.SecurityPreferences
import com.example.momentra.ui.shell.business.shared.BusinessActiveTheme
import com.example.momentra.ui.shell.business.runway.components.RunwayActivityRow
import com.example.momentra.ui.shell.business.runway.components.RunwayAttentionCard
import com.example.momentra.ui.shell.business.runway.components.RunwayBackgroundGlow
import com.example.momentra.ui.shell.business.runway.components.RunwayBurnCategoryRow
import com.example.momentra.ui.shell.business.runway.components.RunwayColors
import com.example.momentra.ui.shell.business.runway.components.RunwayGradientPrimaryButton
import com.example.momentra.ui.shell.business.runway.components.RunwayHeroHealthRing
import com.example.momentra.ui.shell.business.runway.components.RunwayIntelligenceSection
import com.example.momentra.ui.shell.business.runway.components.RunwayOutlineButton
import com.example.momentra.ui.shell.business.runway.components.RunwayTintedMetricTile
import com.example.momentra.ui.shell.business.shared.BusinessTabDataCache
import com.example.momentra.ui.shell.business.shared.loadBusinessPulseTab
import com.example.momentra.ui.theme.PlusJakartaSans

private data class CategoryBurnRow(val label: String, val amount: String, val pct: Int)

@Suppress("UNCHECKED_CAST")
private fun parseCategoryBreakdown(widgetPayload: Map<String, Any?>?): List<CategoryBurnRow> {
    val raw = widgetPayload?.get("categoryBreakdown") as? List<*> ?: return emptyList()
    return raw.mapNotNull { entry ->
        val m = entry as? Map<*, *> ?: return@mapNotNull null
        CategoryBurnRow(
            label = m["label"]?.toString() ?: return@mapNotNull null,
            amount = m["amount"]?.toString() ?: "0",
            pct = (m["pct"] as? Number)?.toInt() ?: 0,
        )
    }
}

private fun mapStr(map: Map<String, Any?>?, key: String): String? =
    map?.get(key)?.toString()?.trim()?.takeIf { it.isNotEmpty() && it != "null" }

/** Figma `692:36956` — Runway Pulse; live bind; honest empties for AI. */
@Composable
fun RunwayPulseActiveContent(
    momentId: String?,
    momentTitle: String?,
    refreshToken: Long,
    onLogExpense: () -> Unit = {},
    onOpenQuickAdd: () -> Unit = {},
    repository: BusinessSliceRepository = remember { BusinessSliceRepository() },
    modifier: Modifier = Modifier,
) {
    val theme = BusinessActiveTheme.BusinessRunway
    var loading by remember { mutableStateOf(true) }
    var pulse by remember { mutableStateOf<BusinessPulsePayloadDto?>(null) }
    var finance by remember { mutableStateOf<BusinessFinancePayloadDto?>(null) }
    var life by remember { mutableStateOf<BusinessLifePayloadDto?>(null) }
    var activities by remember { mutableStateOf<List<ActivityItemDto>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    val hide = SecurityPreferences(LocalContext.current).hideBalances()

    LaunchedEffect(refreshToken, momentId) {
        if (momentId.isNullOrBlank()) {
            loading = false
            pulse = null
            finance = null
            life = null
            activities = emptyList()
            error = "Select a Business Moment."
            return@LaunchedEffect
        }
        error = null
        BusinessTabDataCache.peekPulse(momentId)?.let { cached ->
            pulse = cached.pulse
            finance = cached.finance
            life = cached.life
            activities = cached.activities
            loading = false
        } ?: run { loading = pulse == null }
        loadBusinessPulseTab(repository, momentId).fold(
            onSuccess = { data ->
                pulse = data.pulse
                finance = data.finance
                life = data.life
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

    val runwayPayload = life?.runwayPayload
    val healthScore = pulse?.financialHealthScore?.trim()?.takeIf { it.isNotEmpty() } ?: "—"
    val hasLive = healthScore != "—"
    val attentionCount = pulse?.attentionCount ?: 0
    val runway = pulse?.runwayMonths?.trim()?.takeIf { it.isNotEmpty() } ?: "—"
    val totals = finance?.totals?.firstOrNull()
    val cash = mapStr(runwayPayload, "availableCash")
        ?: mapStr(runwayPayload, "expenseTotal")
        ?: totals?.expenseTotal?.takeIf { it.isNotBlank() && it != "0" }
    val burn = mapStr(runwayPayload, "monthlySpending")
        ?: totals?.expenseTotal?.takeIf { it.isNotBlank() && it != "0" }
    val cashDisplay = cash?.let { BalanceMask.mask(it, hide) }?.ifBlank { "—" } ?: "—"
    val burnDisplay = burn?.let { BalanceMask.mask(it, hide) }?.ifBlank { "—" } ?: "—"
    val categoryBurn = parseCategoryBreakdown(pulse?.widgetPayload)
    val statusLabel = mapStr(runwayPayload, "statusLabel")

    val narrative = when (val n = healthScore.toDoubleOrNull()) {
        null -> if (attentionCount > 0) "Needs attention" else "Awaiting live health signal"
        in 80.0..Double.MAX_VALUE -> "Strong & Growing"
        in 50.0..79.999 -> "Needs focus"
        else -> "At risk"
    }
    val subtitle = when {
        attentionCount > 0 ->
            "$attentionCount item${if (attentionCount == 1) "" else "s"} need your eye today."
        hasLive -> statusLabel ?: "Capital runway is stable with current burn rates."
        else -> "Status updates as finance activity projects."
    }

    val attentionActs = activities.filter {
        val hay = (it.title + " " + it.activityCode).uppercase()
        hay.contains("ISSUE") || hay.contains("BUDGET") || hay.contains("TAX") ||
            hay.contains("RISK") || hay.contains("ALERT") || hay.contains("OVERDUE")
    }.take(5)
    val recentActs = activities.take(5)

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
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            error?.let {
                Text(it, color = RunwayColors.Red, fontSize = 12.sp, fontFamily = PlusJakartaSans)
            }

            RunwayHealthHeroCard(
                theme = theme,
                healthScore = healthScore,
                showLive = hasLive,
                narrative = narrative,
                subtitle = subtitle,
                runway = runway,
                cash = cashDisplay,
                burn = burnDisplay,
            )

            RunwayBurnSection(theme = theme, rows = categoryBurn)

            RunwayNeedsAttentionSection(
                theme = theme,
                attentionCount = attentionCount,
                activities = attentionActs,
            )

            RunwayRecentActivitySection(
                theme = theme,
                activities = recentActs,
                onViewAll = onOpenQuickAdd,
            )

            RunwayIntelligenceSection(theme = theme)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RunwayGradientPrimaryButton(
                    label = "+ Log Expense",
                    enabled = !momentId.isNullOrBlank(),
                    onClick = onLogExpense,
                    modifier = Modifier.weight(1f),
                )
                RunwayOutlineButton(
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
private fun RunwayHealthHeroCard(
    theme: BusinessActiveTheme,
    healthScore: String,
    showLive: Boolean,
    narrative: String,
    subtitle: String,
    runway: String,
    cash: String,
    burn: String,
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
            RunwayHeroHealthRing(
                score = healthScore,
                showLive = showLive,
                theme = theme,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    "RUNWAY HEALTH",
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
            RunwayTintedMetricTile(
                value = runway,
                label = "months runway",
                detail = "From pulse",
                tint = RunwayColors.Amber,
                theme = theme,
                modifier = Modifier.weight(1f),
            )
            RunwayTintedMetricTile(
                value = cash,
                label = "cash balance",
                detail = "Live or prefs",
                tint = RunwayColors.Amber,
                theme = theme,
                modifier = Modifier.weight(1f),
            )
            RunwayTintedMetricTile(
                value = burn,
                label = "monthly burn",
                detail = "Spend total",
                tint = RunwayColors.Amber,
                theme = theme,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun RunwayBurnSection(
    theme: BusinessActiveTheme,
    rows: List<CategoryBurnRow>,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "Burn Rate by Category",
                color = theme.text,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = PlusJakartaSans,
            )
            Text(
                "Monthly allocation breakdown",
                color = theme.muted,
                fontSize = 11.sp,
                fontFamily = PlusJakartaSans,
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(theme.card)
                .border(1.dp, theme.border, RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (rows.isEmpty()) {
                Text(
                    "Category burn appears when live breakdown is projected.",
                    color = theme.secondary,
                    fontSize = 13.sp,
                    fontFamily = PlusJakartaSans,
                )
            } else {
                rows.take(6).forEach { row ->
                    RunwayBurnCategoryRow(label = row.label, pct = row.pct, theme = theme)
                }
            }
        }
    }
}

@Composable
private fun RunwayNeedsAttentionSection(
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
                        .background(RunwayColors.Red)
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
                RunwayAttentionCard(
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
private fun RunwayRecentActivitySection(
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
                "Recent Financial Activity",
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
                "Financial activity appears as revenues, expenses, and updates project.",
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(theme.card)
                    .border(1.dp, theme.border, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                activities.forEach { RunwayActivityRow(item = it, theme = theme) }
                Text(
                    "View all activity →",
                    color = RunwayColors.LinkAmber,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = PlusJakartaSans,
                    modifier = Modifier
                        .align(Alignment.End)
                        .clickable(onClick = onViewAll),
                )
            }
        }
    }
}

package com.example.momentra.ui.shell.business.teamops.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.api.ActivityItemDto
import com.example.momentra.data.api.BusinessTimelineItemDto
import com.example.momentra.data.api.WorkloadDto
import com.example.momentra.ui.shell.business.shared.BusinessActiveTheme
import com.example.momentra.ui.theme.PlusJakartaSans

object TeamOpsColors {
    val Emerald = Color(0xFF10B981)
    val EmeraldDark = Color(0xFF059669)
    val EmeraldLight = Color(0xFF34D399)
    val Indigo = Color(0xFF6366F1)
    val IndigoLight = Color(0xFF818CF8)
    val Lavender = Color(0xFFA78BFA)
    val Amber = Color(0xFFF59E0B)
    val Red = Color(0xFFEF4444)
    val LinkBlue = Color(0xFF818CF8)
    val CtaText = Color(0xFF0C0F15)
    val DayMuted = Color(0xFF475569)
}

@Composable
fun TeamOpsBackgroundGlow(modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .size(200.dp)
                .offset(x = (-40).dp, y = (-20).dp)
                .clip(CircleShape)
                .background(TeamOpsColors.Emerald.copy(alpha = 0.08f)),
        )
        Box(
            modifier = Modifier
                .size(160.dp)
                .align(Alignment.TopEnd)
                .offset(x = 40.dp, y = 60.dp)
                .clip(CircleShape)
                .background(TeamOpsColors.Indigo.copy(alpha = 0.06f)),
        )
    }
}

@Composable
fun TeamOpsHeroHealthRing(
    score: String,
    showLive: Boolean,
    theme: BusinessActiveTheme,
    ringSizeDp: Int = 110,
    modifier: Modifier = Modifier,
) {
    val fraction = score.toIntOrNull()?.takeIf { it > 0 }?.let {
        (it / 100f).coerceIn(0f, 1f)
    } ?: 0f
    Box(modifier = modifier.size(ringSizeDp.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(ringSizeDp.dp)) {
            val stroke = 10.dp.toPx()
            val pad = stroke / 2
            drawArc(
                color = TeamOpsColors.Emerald.copy(alpha = 0.25f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(pad, pad),
                size = Size(size.width - stroke, size.height - stroke),
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            if (fraction > 0f) {
                drawArc(
                    color = TeamOpsColors.Emerald,
                    startAngle = -90f,
                    sweepAngle = 360f * fraction,
                    useCenter = false,
                    topLeft = Offset(pad, pad),
                    size = Size(size.width - stroke, size.height - stroke),
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                score,
                color = theme.text,
                fontSize = if (ringSizeDp >= 130) 28.sp else 30.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = PlusJakartaSans,
            )
            if (score != "—" && ringSizeDp < 130) {
                Text(
                    "/100",
                    color = theme.muted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = PlusJakartaSans,
                )
            }
        }
        if (showLive) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-6).dp)
                    .clip(RoundedCornerShape(100.dp))
                    .background(TeamOpsColors.Emerald.copy(alpha = 0.12f))
                    .border(1.dp, TeamOpsColors.Emerald.copy(alpha = 0.2f), RoundedCornerShape(100.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(TeamOpsColors.Emerald))
                Text(
                    "LIVE",
                    color = TeamOpsColors.Emerald,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = PlusJakartaSans,
                )
            }
        }
    }
}

@Composable
fun TeamOpsTintedMetricTile(
    value: String,
    label: String,
    detail: String,
    tint: Color,
    theme: BusinessActiveTheme,
    modifier: Modifier = Modifier,
    valueColor: Color = Color.Unspecified,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(tint.copy(alpha = 0.04f))
            .border(1.dp, tint.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            value,
            color = if (valueColor == Color.Unspecified) theme.text else valueColor,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = PlusJakartaSans,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            label.uppercase(),
            color = theme.muted,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = PlusJakartaSans,
        )
        Text(
            detail,
            color = theme.muted,
            fontSize = 10.sp,
            fontFamily = PlusJakartaSans,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun TeamOpsFilterChipRow(
    chips: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    theme: BusinessActiveTheme,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        chips.forEach { chip ->
            val isSelected = selected == chip
            Text(
                chip,
                color = if (isSelected) TeamOpsColors.CtaText else theme.secondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        if (isSelected) {
                            Brush.horizontalGradient(
                                listOf(TeamOpsColors.EmeraldLight, TeamOpsColors.Emerald),
                            )
                        } else {
                            Brush.linearGradient(listOf(theme.card, theme.card))
                        },
                    )
                    .border(
                        1.dp,
                        if (isSelected) TeamOpsColors.Emerald.copy(alpha = 0.4f) else theme.border,
                        RoundedCornerShape(999.dp),
                    )
                    .clickable { onSelect(chip) }
                    .padding(horizontal = 12.dp, vertical = 7.dp),
            )
        }
    }
}

@Composable
fun TeamOpsGradientPrimaryButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.horizontalGradient(listOf(TeamOpsColors.EmeraldLight, TeamOpsColors.EmeraldDark)),
            )
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = Color.White.copy(alpha = if (enabled) 1f else 0.5f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = PlusJakartaSans,
        )
    }
}

@Composable
fun TeamOpsOutlineButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    theme: BusinessActiveTheme,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, theme.border, RoundedCornerShape(24.dp))
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = theme.text.copy(alpha = if (enabled) 1f else 0.55f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = PlusJakartaSans,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun TeamOpsDiamondDivider(theme: BusinessActiveTheme, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Box(modifier = Modifier.weight(1f).height(1.dp).background(theme.border))
        Box(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .size(8.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(
                    Brush.linearGradient(listOf(TeamOpsColors.EmeraldLight, TeamOpsColors.EmeraldDark)),
                ),
        )
        Box(modifier = Modifier.weight(1f).height(1.dp).background(theme.border))
    }
}

/** Workload section — renders live byDepartment data when available, honest empty otherwise. */
@Suppress("UNCHECKED_CAST")
@Composable
fun TeamOpsWorkloadSection(
    theme: BusinessActiveTheme,
    workloadData: WorkloadDto? = null,
    modifier: Modifier = Modifier,
) {
    val deptRows: List<Pair<String, Int>> = workloadData?.byDepartment?.map { row ->
        row.name to row.count
    } ?: emptyList()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(theme.card)
            .border(1.dp, theme.border, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "Workload by Department",
                color = theme.text,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = PlusJakartaSans,
            )
            Text(
                "Daily workload intensity this week",
                color = theme.muted,
                fontSize = 11.sp,
                fontFamily = PlusJakartaSans,
            )
        }
        if (deptRows.isEmpty()) {
            Text(
                "Workload heatmap unavailable — department intensity API not mounted.",
                color = theme.secondary,
                fontSize = 13.sp,
                fontFamily = PlusJakartaSans,
            )
            listOf("Engineering", "Design", "Operations").forEach { dept ->
                WorkloadDeptPlaceholder(dept = dept, theme = theme)
            }
        } else {
            deptRows.forEach { (dept, count) ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val intensity = (count / 10f).coerceIn(0f, 1f)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(dept, color = theme.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
                        Text("$count open", color = theme.muted, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(TeamOpsColors.Emerald.copy(alpha = 0.15f + intensity * 0.75f)),
                    )
                }
            }
        }
    }
}

@Composable
private fun WorkloadDeptPlaceholder(dept: String, theme: BusinessActiveTheme) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(dept, color = theme.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
            Text("—", color = theme.muted, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            repeat(7) {
                Box(modifier = Modifier.weight(1f).height(10.dp).clip(RoundedCornerShape(2.dp)).background(TeamOpsColors.DayMuted.copy(alpha = 0.35f)))
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach { d ->
                Text(d, color = theme.muted, fontSize = 9.sp, fontFamily = PlusJakartaSans, modifier = Modifier.weight(1f))
            }
        }
    }
}

/** Honest empty — no Team Intelligence AI feed. */
@Composable
fun TeamOpsIntelligenceSection(theme: BusinessActiveTheme, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                "Team Intelligence",
                color = theme.text,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = PlusJakartaSans,
            )
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .height(2.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(TeamOpsColors.EmeraldLight, TeamOpsColors.Emerald, TeamOpsColors.EmeraldLight),
                        ),
                    ),
            )
            Text(
                "AI-powered insights based on your team data.",
                color = theme.muted,
                fontSize = 11.sp,
                fontFamily = PlusJakartaSans,
            )
        }
        listOf("Capacity Alert", "Pattern Found").forEach { title ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(theme.card)
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(TeamOpsColors.Lavender.copy(alpha = 0.08f)),
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        title,
                        color = theme.text,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = PlusJakartaSans,
                    )
                    Text(
                        "Insights unavailable until team intelligence API projects signals.",
                        color = theme.secondary,
                        fontSize = 12.sp,
                        fontFamily = PlusJakartaSans,
                    )
                }
            }
        }
    }
}

@Composable
fun TeamOpsTimelineHeroCard(
    members: String,
    pending: String,
    issues: String,
    theme: BusinessActiveTheme,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF161B26), Color(0xFF1A1F2E))))
            .border(1.dp, TeamOpsColors.Emerald.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            "TEAM OPERATIONS • MOMENTS",
            color = TeamOpsColors.IndigoLight,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = PlusJakartaSans,
        )
        Text(
            "Team Timeline",
            color = theme.text,
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = PlusJakartaSans,
        )
        Text(
            "Track milestones, decisions, and what your team shipped.",
            color = theme.secondary,
            fontSize = 13.sp,
            fontFamily = PlusJakartaSans,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TeamOpsStatChip(members, "MEMBERS", "from pulse", theme, Modifier.weight(1f))
            TeamOpsStatChip(pending, "PENDING", "needs review", theme, Modifier.weight(1f), TeamOpsColors.Amber)
            TeamOpsStatChip(issues, "ISSUES", "this week", theme, Modifier.weight(1f), TeamOpsColors.Red)
        }
    }
}

@Composable
private fun TeamOpsStatChip(
    value: String,
    label: String,
    detail: String,
    theme: BusinessActiveTheme,
    modifier: Modifier = Modifier,
    valueColor: Color = Color.Unspecified,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(theme.card)
            .border(1.dp, theme.border, RoundedCornerShape(12.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            value,
            color = if (valueColor == Color.Unspecified) theme.text else valueColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = PlusJakartaSans,
        )
        Text(label, color = theme.muted, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
        Text(detail, color = theme.muted, fontSize = 10.sp, fontFamily = PlusJakartaSans)
    }
}

@Composable
fun TeamOpsProgressSnapshot(
    deliveryRatio: Float?,
    capacityRatio: Float?,
    approvalsRatio: Float?,
    theme: BusinessActiveTheme,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(theme.card)
            .border(1.dp, theme.border, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Progress Snapshot",
                color = theme.text,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = PlusJakartaSans,
                modifier = Modifier.weight(1f),
            )
            Text(
                "This Quarter",
                color = theme.secondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
                modifier = Modifier
                    .border(1.dp, theme.border, RoundedCornerShape(999.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
        Text(
            "How your team is performing this quarter.",
            color = theme.muted,
            fontSize = 11.sp,
            fontFamily = PlusJakartaSans,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TeamOpsMiniGauge("Delivery", deliveryRatio, TeamOpsColors.IndigoLight, theme, Modifier.weight(1f))
            TeamOpsMiniGauge("Capacity", capacityRatio, TeamOpsColors.Emerald, theme, Modifier.weight(1f))
            TeamOpsMiniGauge("Approvals", approvalsRatio, TeamOpsColors.Amber, theme, Modifier.weight(1f))
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(theme.border.copy(alpha = 0.4f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "Trend unavailable",
                color = theme.muted,
                fontSize = 11.sp,
                fontFamily = PlusJakartaSans,
            )
        }
    }
}

@Composable
private fun TeamOpsMiniGauge(
    label: String,
    ratio: Float?,
    color: Color,
    theme: BusinessActiveTheme,
    modifier: Modifier = Modifier,
) {
    val pct = ratio?.let { "${(it * 100).toInt()}%" } ?: "—"
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(modifier = Modifier.size(56.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(56.dp)) {
                val stroke = 6.dp.toPx()
                val pad = stroke / 2
                drawArc(
                    color = color.copy(alpha = 0.2f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = Offset(pad, pad),
                    size = Size(size.width - stroke, size.height - stroke),
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
                if (ratio != null && ratio > 0f) {
                    drawArc(
                        color = color,
                        startAngle = -90f,
                        sweepAngle = 360f * ratio.coerceIn(0f, 1f),
                        useCenter = false,
                        topLeft = Offset(pad, pad),
                        size = Size(size.width - stroke, size.height - stroke),
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                    )
                }
            }
            Text(pct, color = theme.text, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
        }
        Text(label, color = theme.muted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
    }
}

@Composable
fun TeamOpsMemoryHeroSection(
    ringLabel: String,
    learnings: String,
    patterns: String,
    accuracy: String,
    showLive: Boolean,
    theme: BusinessActiveTheme,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
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
                score = ringLabel,
                showLive = showLive,
                ringSizeDp = 130,
                theme = theme,
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Team Memory",
                    color = theme.muted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = PlusJakartaSans,
                )
                Text(
                    if (ringLabel == "—") "Awaiting signal" else "Growing",
                    color = theme.text,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = PlusJakartaSans,
                )
                Text(
                    if (showLive) "Trends vs benchmarks: live learnings" else "Record learnings to grow memory",
                    color = theme.secondary,
                    fontSize = 12.sp,
                    fontFamily = PlusJakartaSans,
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TeamOpsTintedMetricTile(learnings, "patterns", "discovered", TeamOpsColors.Lavender, theme, Modifier.weight(1f))
            TeamOpsTintedMetricTile(patterns, "active rules", "playbook", TeamOpsColors.IndigoLight, theme, Modifier.weight(1f))
            TeamOpsTintedMetricTile(accuracy, "accuracy", "rate", TeamOpsColors.Emerald, theme, Modifier.weight(1f))
        }
    }
}

@Composable
fun TeamOpsEmptyAiCard(
    title: String,
    emptyCopy: String,
    theme: BusinessActiveTheme,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(theme.card)
            .border(1.dp, theme.border, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, color = theme.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
        Text(emptyCopy, color = theme.secondary, fontSize = 13.sp, fontFamily = PlusJakartaSans)
    }
}

@Composable
fun TeamOpsTimelineEntryRow(
    item: BusinessTimelineItemDto,
    theme: BusinessActiveTheme,
    modifier: Modifier = Modifier,
) {
    val type = item.eventType.uppercase()
    val accent = when {
        type.contains("ISSUE") || type.contains("BLOCK") -> TeamOpsColors.Red
        type.contains("UPDATE") || type.contains("DELIVER") || type.contains("SHIP") -> TeamOpsColors.Emerald
        type.contains("DECISION") || type.contains("MILESTONE") -> TeamOpsColors.IndigoLight
        else -> TeamOpsColors.Lavender
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(theme.card)
            .border(1.dp, theme.border, RoundedCornerShape(14.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                item.title.firstOrNull()?.uppercaseChar()?.toString() ?: "•",
                color = accent,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                item.title.ifBlank { item.eventType },
                color = theme.text,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = PlusJakartaSans,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                listOfNotNull(item.category.takeIf { it.isNotBlank() }, item.occurredAt.take(10).takeIf { it.isNotBlank() })
                    .joinToString(" • "),
                color = theme.muted,
                fontSize = 11.sp,
                fontFamily = PlusJakartaSans,
            )
        }
    }
}

@Composable
fun TeamOpsActivityRow(
    item: ActivityItemDto,
    theme: BusinessActiveTheme,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(theme.card)
            .border(1.dp, theme.border, RoundedCornerShape(14.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(TeamOpsColors.Emerald),
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                item.title.ifBlank { item.activityCode },
                color = theme.text,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = PlusJakartaSans,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${item.activityCode} • ${item.occurredAt.take(10)}",
                color = theme.muted,
                fontSize = 11.sp,
                fontFamily = PlusJakartaSans,
            )
        }
    }
}

@Composable
fun TeamOpsAttentionCard(
    title: String,
    severity: String?,
    detail: String,
    theme: BusinessActiveTheme,
    modifier: Modifier = Modifier,
) {
    val sev = severity?.uppercase().orEmpty()
    val badgeColor = when {
        sev.contains("HIGH") || sev.contains("CRITICAL") -> TeamOpsColors.Red
        sev.contains("MED") -> TeamOpsColors.Amber
        else -> theme.accent
    }
    val action = "Escalation API not mounted"
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(theme.card)
            .border(1.dp, badgeColor.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(badgeColor))
            if (sev.isNotBlank()) {
                Text(
                    sev.take(6),
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = PlusJakartaSans,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(badgeColor)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
            Spacer(Modifier.weight(1f))
        }
        Text(title, color = theme.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
        Text(detail, color = theme.muted, fontSize = 12.sp, fontFamily = PlusJakartaSans)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(badgeColor.copy(alpha = 0.25f)),
        )
        Text(action, color = theme.muted, fontSize = 12.sp, fontFamily = PlusJakartaSans)
    }
}

@Composable
fun TeamOpsMemoryListSection(
    title: String,
    emptyCopy: String,
    items: List<Map<String, Any?>>,
    theme: BusinessActiveTheme,
    accentBorder: Color,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, color = theme.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
        if (items.isEmpty()) {
            Text(
                emptyCopy,
                color = theme.secondary,
                fontSize = 13.sp,
                fontFamily = PlusJakartaSans,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(theme.card)
                    .border(1.dp, theme.border, RoundedCornerShape(12.dp))
                    .padding(12.dp),
            )
        } else {
            items.take(8).forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(theme.card)
                        .border(1.dp, accentBorder.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(accentBorder))
                    Text(
                        item["title"]?.toString()?.ifBlank { "Memory" } ?: "Memory",
                        color = theme.text,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = PlusJakartaSans,
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

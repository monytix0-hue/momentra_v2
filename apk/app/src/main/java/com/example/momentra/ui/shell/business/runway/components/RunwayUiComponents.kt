package com.example.momentra.ui.shell.business.runway.components

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
import com.example.momentra.ui.shell.business.BusinessActiveTheme
import com.example.momentra.ui.theme.PlusJakartaSans

object RunwayColors {
    val Amber = Color(0xFFF59E0B)
    val AmberDark = Color(0xFFD97706)
    val AmberLight = Color(0xFFFBBF24)
    val Indigo = Color(0xFF6366F1)
    val IndigoLight = Color(0xFF818CF8)
    val Lavender = Color(0xFFA78BFA)
    val Emerald = Color(0xFF10B981)
    val Red = Color(0xFFEF4444)
    val LinkAmber = Color(0xFFF59E0B)
    val CtaText = Color(0xFF0C0F15)
}

@Composable
fun RunwayBackgroundGlow(modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .size(200.dp)
                .offset(x = (-40).dp, y = (-20).dp)
                .clip(CircleShape)
                .background(RunwayColors.Amber.copy(alpha = 0.08f)),
        )
        Box(
            modifier = Modifier
                .size(160.dp)
                .align(Alignment.TopEnd)
                .offset(x = 40.dp, y = 60.dp)
                .clip(CircleShape)
                .background(RunwayColors.Indigo.copy(alpha = 0.06f)),
        )
    }
}

@Composable
fun RunwayHeroHealthRing(
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
                color = RunwayColors.Amber.copy(alpha = 0.25f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(pad, pad),
                size = Size(size.width - stroke, size.height - stroke),
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            if (fraction > 0f) {
                drawArc(
                    color = RunwayColors.Amber,
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
                    .background(RunwayColors.Amber.copy(alpha = 0.12f))
                    .border(1.dp, RunwayColors.Amber.copy(alpha = 0.2f), RoundedCornerShape(100.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(RunwayColors.Amber))
                Text(
                    "LIVE",
                    color = RunwayColors.Amber,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = PlusJakartaSans,
                )
            }
        }
    }
}

@Composable
fun RunwayTintedMetricTile(
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
fun RunwayFilterChipRow(
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
                color = if (isSelected) RunwayColors.CtaText else theme.secondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        if (isSelected) {
                            Brush.horizontalGradient(
                                listOf(RunwayColors.AmberLight, RunwayColors.Amber),
                            )
                        } else {
                            Brush.linearGradient(listOf(theme.card, theme.card))
                        },
                    )
                    .border(
                        1.dp,
                        if (isSelected) RunwayColors.Amber.copy(alpha = 0.4f) else theme.border,
                        RoundedCornerShape(999.dp),
                    )
                    .clickable { onSelect(chip) }
                    .padding(horizontal = 12.dp, vertical = 7.dp),
            )
        }
    }
}

@Composable
fun RunwayGradientPrimaryButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.horizontalGradient(listOf(RunwayColors.Amber, RunwayColors.Indigo)),
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
fun RunwayOutlineButton(
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
fun RunwayDiamondDivider(theme: BusinessActiveTheme, modifier: Modifier = Modifier) {
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
                    Brush.linearGradient(listOf(RunwayColors.AmberLight, RunwayColors.AmberDark)),
                ),
        )
        Box(modifier = Modifier.weight(1f).height(1.dp).background(theme.border))
    }
}

/** Honest empty — no Financial Intelligence AI feed. */
@Composable
fun RunwayIntelligenceSection(theme: BusinessActiveTheme, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                "Financial Intelligence",
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
                            listOf(RunwayColors.Amber, RunwayColors.Indigo, RunwayColors.Amber),
                        ),
                    ),
            )
            Text(
                "AI-powered insights based on your financial data",
                color = theme.muted,
                fontSize = 11.sp,
                fontFamily = PlusJakartaSans,
            )
        }
        listOf("Burn Efficiency", "Scenario Insight").forEach { title ->
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
                        .background(RunwayColors.Amber.copy(alpha = 0.08f)),
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
                        "Insights unavailable until financial intelligence API projects signals.",
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
fun RunwayTimelineHeroCard(
    entries: String,
    revenue: String,
    savings: String,
    theme: BusinessActiveTheme,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF161B26), Color(0xFF1A1F2E))))
            .border(1.dp, RunwayColors.Amber.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            "FINANCIAL TIMELINE • RUNWAY",
            color = RunwayColors.Amber,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = PlusJakartaSans,
        )
        Text(
            "Financial Timeline",
            color = theme.text,
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = PlusJakartaSans,
        )
        Text(
            "Track revenues, expenses, and financial milestones.",
            color = theme.secondary,
            fontSize = 13.sp,
            fontFamily = PlusJakartaSans,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RunwayStatChip(entries, "ENTRIES", "timeline", theme, Modifier.weight(1f))
            RunwayStatChip(revenue, "REVENUE", "live total", theme, Modifier.weight(1f), RunwayColors.Amber)
            RunwayStatChip(savings, "ACTIVITY", "events", theme, Modifier.weight(1f), RunwayColors.Emerald)
        }
    }
}

@Composable
private fun RunwayStatChip(
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
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(label, color = theme.muted, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
        Text(detail, color = theme.muted, fontSize = 10.sp, fontFamily = PlusJakartaSans)
    }
}

@Composable
fun RunwayProgressSnapshot(
    burnRatio: Float?,
    collectionsRatio: Float?,
    healthRatio: Float?,
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
            "How runway is tracking this quarter.",
            color = theme.muted,
            fontSize = 11.sp,
            fontFamily = PlusJakartaSans,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            RunwayMiniGauge("Burn rate", burnRatio, RunwayColors.Amber, theme, Modifier.weight(1f))
            RunwayMiniGauge("Collections", collectionsRatio, RunwayColors.Emerald, theme, Modifier.weight(1f))
            RunwayMiniGauge("Health", healthRatio, RunwayColors.AmberLight, theme, Modifier.weight(1f))
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
private fun RunwayMiniGauge(
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
fun RunwayMemoryHeroSection(
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
            RunwayHeroHealthRing(
                score = ringLabel,
                showLive = showLive,
                ringSizeDp = 130,
                theme = theme,
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Learning",
                    color = theme.muted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = PlusJakartaSans,
                )
                Text(
                    if (ringLabel == "—") "Awaiting signal" else "Insight feed is running",
                    color = theme.text,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = PlusJakartaSans,
                )
                Text(
                    if (showLive) "Live learnings from recorded memory" else "Record learnings to grow memory",
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
            RunwayTintedMetricTile(learnings, "learnings", "recorded", RunwayColors.Amber, theme, Modifier.weight(1f))
            RunwayTintedMetricTile(patterns, "patterns", "AI pending", RunwayColors.Emerald, theme, Modifier.weight(1f))
            RunwayTintedMetricTile(accuracy, "accuracy", "AI pending", RunwayColors.AmberLight, theme, Modifier.weight(1f))
        }
    }
}

@Composable
fun RunwayEmptyAiCard(
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
fun RunwayTimelineEntryRow(
    item: BusinessTimelineItemDto,
    theme: BusinessActiveTheme,
    modifier: Modifier = Modifier,
) {
    val type = item.eventType.uppercase()
    val accent = when {
        type.contains("EXPENSE") || type.contains("ISSUE") || type.contains("RISK") -> RunwayColors.Red
        type.contains("REVENUE") || type.contains("MILESTONE") || type.contains("INVOICE") -> RunwayColors.Amber
        type.contains("UPDATE") -> RunwayColors.Emerald
        else -> RunwayColors.Lavender
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
                listOfNotNull(
                    item.category.takeIf { it.isNotBlank() },
                    item.occurredAt.take(10).takeIf { it.isNotBlank() },
                ).joinToString(" • "),
                color = theme.muted,
                fontSize = 11.sp,
                fontFamily = PlusJakartaSans,
            )
        }
    }
}

@Composable
fun RunwayActivityRow(
    item: ActivityItemDto,
    theme: BusinessActiveTheme,
    modifier: Modifier = Modifier,
) {
    val code = item.activityCode.uppercase()
    val accent = when {
        code.contains("EXPENSE") || code.contains("ISSUE") -> RunwayColors.Red
        code.contains("REVENUE") || code.contains("INVOICE") -> RunwayColors.Amber
        else -> RunwayColors.Emerald
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
                .size(10.dp)
                .clip(CircleShape)
                .background(accent),
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
fun RunwayAttentionCard(
    title: String,
    severity: String?,
    detail: String,
    theme: BusinessActiveTheme,
    modifier: Modifier = Modifier,
) {
    val sev = severity?.uppercase().orEmpty()
    val badgeColor = when {
        sev.contains("HIGH") || sev.contains("CRITICAL") -> RunwayColors.Red
        sev.contains("MED") -> RunwayColors.Amber
        else -> theme.accent
    }
    val action = when {
        sev.contains("HIGH") || sev.contains("CRITICAL") -> "Review budget →"
        else -> "Prepare docs →"
    }
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
                    color = badgeColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = PlusJakartaSans,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(badgeColor.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
            Spacer(modifier.weight(1f))
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
        Text(action, color = RunwayColors.LinkAmber, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
    }
}

@Composable
fun RunwayBurnCategoryRow(
    label: String,
    pct: Int,
    theme: BusinessActiveTheme,
    modifier: Modifier = Modifier,
) {
    val blocks = ((pct / 10).coerceIn(1, 10)).coerceAtLeast(if (pct > 0) 1 else 0)
    val opacity = (pct / 100f).coerceIn(0.15f, 0.55f)
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            label,
            color = theme.secondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = PlusJakartaSans,
            modifier = Modifier.width(90.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(blocks) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(RunwayColors.Amber.copy(alpha = opacity)),
                )
            }
        }
        Text(
            "$pct%",
            color = theme.text,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = PlusJakartaSans,
            modifier = Modifier.width(40.dp),
        )
    }
}

@Composable
fun RunwayMemoryListSection(
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

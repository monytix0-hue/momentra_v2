package com.example.momentra.ui.shell.business.ops.components

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.api.ActivityItemDto
import com.example.momentra.data.api.BusinessTimelineItemDto
import com.example.momentra.data.api.OpsAttentionDto
import com.example.momentra.data.api.OpsSpendCategoryDto
import com.example.momentra.ui.shell.business.BusinessActiveTheme
import com.example.momentra.ui.theme.PlusJakartaSans

object OpsColors {
    val Lavender = Color(0xFFA78BFA)
    val Indigo = Color(0xFF6366F1)
    val IndigoLight = Color(0xFF818CF8)
    val Green = Color(0xFF10B981)
    val Amber = Color(0xFFF59E0B)
    val Red = Color(0xFFEF4444)
    val LinkBlue = Color(0xFF60A5FA)
    val CtaText = Color(0xFF14121B)
    val BarPrimary = Color(0xFFA78BFA)
    val BarSecondary = Color(0xFF6366F1)
}

@Composable
fun OpsBackgroundGlow(modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .size(200.dp)
                .offset(x = (-40).dp, y = (-20).dp)
                .clip(CircleShape)
                .background(OpsColors.Lavender.copy(alpha = 0.08f)),
        )
        Box(
            modifier = Modifier
                .size(160.dp)
                .align(Alignment.TopEnd)
                .offset(x = 40.dp, y = 60.dp)
                .clip(CircleShape)
                .background(OpsColors.Indigo.copy(alpha = 0.06f)),
        )
    }
}

@Composable
fun OpsHeroHealthRing(
    score: String,
    showLive: Boolean,
    ringSizeDp: Int = 110,
    theme: BusinessActiveTheme,
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
                color = OpsColors.Lavender.copy(alpha = 0.25f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(pad, pad),
                size = Size(size.width - stroke, size.height - stroke),
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            if (fraction > 0f) {
                drawArc(
                    color = OpsColors.Lavender,
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
                    .background(OpsColors.Green.copy(alpha = 0.12f))
                    .border(1.dp, OpsColors.Green.copy(alpha = 0.2f), RoundedCornerShape(100.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(OpsColors.Green))
                Text(
                    "LIVE",
                    color = OpsColors.Green,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = PlusJakartaSans,
                )
            }
        }
    }
}

@Composable
fun OpsTintedMetricTile(
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
fun OpsFilterChipRow(
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
                color = if (isSelected) OpsColors.CtaText else theme.secondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        if (isSelected) Brush.horizontalGradient(listOf(OpsColors.IndigoLight, OpsColors.Lavender))
                        else Brush.linearGradient(listOf(theme.card, theme.card)),
                    )
                    .border(1.dp, if (isSelected) OpsColors.IndigoLight.copy(alpha = 0.4f) else theme.border, RoundedCornerShape(999.dp))
                    .clickable { onSelect(chip) }
                    .padding(horizontal = 12.dp, vertical = 7.dp),
            )
        }
    }
}

@Composable
fun OpsDiamondDivider(theme: BusinessActiveTheme, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(theme.border),
        )
        Box(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .size(8.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(
                    Brush.linearGradient(listOf(OpsColors.Lavender, OpsColors.Indigo)),
                ),
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(theme.border),
        )
    }
}

@Composable
fun OpsGradientPrimaryButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.horizontalGradient(listOf(OpsColors.Lavender, OpsColors.Indigo)))
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
fun OpsOutlineButton(
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
fun OpsCategoryBarSection(
    categories: List<OpsSpendCategoryDto>,
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
        Text(
            "Spend by Category",
            color = theme.text,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = PlusJakartaSans,
        )
        if (categories.isEmpty()) {
            Text(
                "No categorized spend yet",
                color = theme.secondary,
                fontSize = 13.sp,
                fontFamily = PlusJakartaSans,
            )
        } else {
            categories.forEachIndexed { index, cat ->
                val barColor = if (index % 2 == 0) OpsColors.BarPrimary else OpsColors.BarSecondary
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            cat.label.ifBlank { "OTHER" },
                            color = theme.text,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = PlusJakartaSans,
                        )
                        Text(
                            "${cat.pct}%",
                            color = barColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = PlusJakartaSans,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(theme.border),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction = (cat.pct / 100f).coerceIn(0f, 1f))
                                .height(8.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(barColor),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OpsAttentionCard(
    item: OpsAttentionDto,
    theme: BusinessActiveTheme,
    modifier: Modifier = Modifier,
) {
    val severity = item.severity?.uppercase().orEmpty()
    val badgeColor = when {
        severity.contains("HIGH") || severity.contains("CRITICAL") -> OpsColors.Red
        severity.contains("MED") -> OpsColors.Amber
        else -> theme.accent
    }
    val actionLabel = when {
        severity.contains("HIGH") || severity.contains("CRITICAL") -> "Escalate →"
        else -> "Send reminder →"
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(theme.card)
            .border(1.dp, badgeColor.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (severity.isNotBlank()) {
                Text(
                    severity.take(6),
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
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(badgeColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    item.title.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                    color = badgeColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = PlusJakartaSans,
                )
            }
            Text(
                item.title.ifBlank { "Issue" },
                color = theme.text,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = PlusJakartaSans,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(theme.border),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(badgeColor.copy(alpha = 0.6f)),
            )
        }
        if (!item.issueId.isNullOrBlank()) {
            Text(
                actionLabel,
                color = OpsColors.LinkBlue,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
            )
        }
    }
}

@Composable
fun OpsActivityTimelineSection(
    activities: List<ActivityItemDto>,
    onViewAll: () -> Unit,
    theme: BusinessActiveTheme,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Recent Operations Activity",
                color = theme.text,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = PlusJakartaSans,
                modifier = Modifier.weight(1f),
            )
            Text(
                "This Week",
                color = theme.secondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = PlusJakartaSans,
                modifier = Modifier
                    .border(1.dp, theme.border, RoundedCornerShape(100.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
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
            if (activities.isEmpty()) {
                Text(
                    "Activity appears after live writes.",
                    color = theme.secondary,
                    fontSize = 13.sp,
                    fontFamily = PlusJakartaSans,
                )
            } else {
                activities.take(8).forEach { item ->
                    val completed = item.activityCode.uppercase().let {
                        it.contains("COMPLETE") || it.contains("APPROVED") || it.contains("RESOLVED")
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(theme.accent),
                            )
                            Box(
                                modifier = Modifier
                                    .width(2.dp)
                                    .height(24.dp)
                                    .background(theme.border),
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    item.title.ifBlank { item.activityCode },
                                    color = theme.text,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = PlusJakartaSans,
                                    modifier = Modifier.weight(1f),
                                )
                                if (completed) {
                                    Text(
                                        "Completed",
                                        color = OpsColors.Green,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = PlusJakartaSans,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(OpsColors.Green.copy(alpha = 0.12f))
                                            .padding(horizontal = 8.dp, vertical = 2.dp),
                                    )
                                }
                            }
                            Text(
                                item.occurredAt,
                                color = theme.muted,
                                fontSize = 11.sp,
                                fontFamily = PlusJakartaSans,
                            )
                        }
                    }
                }
            }
            Text(
                "View all activity →",
                color = OpsColors.LinkBlue,
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

@Composable
fun OpsIntelligenceSection(theme: BusinessActiveTheme, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                "Operations Intelligence",
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
                    .background(Brush.horizontalGradient(listOf(OpsColors.Lavender, OpsColors.Indigo, OpsColors.Lavender))),
            )
            Text(
                "AI-powered insights based on your operations data",
                color = theme.muted,
                fontSize = 11.sp,
                fontFamily = PlusJakartaSans,
            )
        }
        listOf("Cost Optimization", "Vendor Pattern").forEach { title ->
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
                        .background(OpsColors.Lavender.copy(alpha = 0.08f)),
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
                        "Insights unavailable until operations pulse projects signals.",
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
fun OpsTimelineHeroCard(
    entries: Int,
    vendors: Int,
    issues: Int,
    theme: BusinessActiveTheme,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF161B26), Color(0xFF1A1F2E))))
            .border(1.dp, OpsColors.IndigoLight.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            "Operations Timeline",
            color = theme.text,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = PlusJakartaSans,
        )
        Text(
            "Live ops events from spend, vendors, issues, and updates.",
            color = theme.secondary,
            fontSize = 12.sp,
            fontFamily = PlusJakartaSans,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OpsHeroStatBox("$entries", "ENTRIES", theme, Modifier.weight(1f))
            OpsHeroStatBox("$vendors", "VENDORS", theme, Modifier.weight(1f))
            OpsHeroStatBox("$issues", "ISSUES", theme, Modifier.weight(1f))
        }
    }
}

@Composable
private fun OpsHeroStatBox(
    value: String,
    label: String,
    theme: BusinessActiveTheme,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(OpsColors.IndigoLight.copy(alpha = 0.06f))
            .border(1.dp, OpsColors.IndigoLight.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            value,
            color = theme.text,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = PlusJakartaSans,
        )
        Text(
            label,
            color = theme.muted,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = PlusJakartaSans,
        )
    }
}

@Composable
fun OpsTimelineEntryRow(
    item: BusinessTimelineItemDto,
    theme: BusinessActiveTheme,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(theme.card)
            .border(1.dp, theme.border, RoundedCornerShape(16.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(theme.accent),
            )
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(40.dp)
                    .background(theme.border),
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    item.title.ifBlank { item.eventType },
                    color = theme.text,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = PlusJakartaSans,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(theme.accent.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        item.category.firstOrNull()?.uppercaseChar()?.toString() ?: "O",
                        color = theme.accent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = PlusJakartaSans,
                    )
                }
            }
            if (item.category.isNotBlank()) {
                Text(
                    item.category,
                    color = OpsColors.CtaText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = PlusJakartaSans,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(theme.accent)
                        .padding(horizontal = 10.dp, vertical = 3.dp),
                )
            }
            Text(
                item.occurredAt,
                color = theme.secondary,
                fontSize = 11.sp,
                fontFamily = PlusJakartaSans,
            )
        }
    }
}

@Composable
fun OpsActivityTimelineRow(
    item: ActivityItemDto,
    theme: BusinessActiveTheme,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(theme.card)
            .border(1.dp, theme.border, RoundedCornerShape(16.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(theme.accent),
            )
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(40.dp)
                    .background(theme.border),
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                item.title.ifBlank { item.activityCode },
                color = theme.text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
            )
            Text(
                item.activityCode,
                color = OpsColors.CtaText,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(theme.accent)
                    .padding(horizontal = 10.dp, vertical = 3.dp),
            )
            Text(
                item.occurredAt,
                color = theme.secondary,
                fontSize = 11.sp,
                fontFamily = PlusJakartaSans,
            )
        }
    }
}

@Composable
fun OpsProgressSnapshot(
    budgetRatio: Float?,
    issuesRatio: Float?,
    milestonesRatio: Float?,
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
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
        OpsSparklinePlaceholder(theme = theme)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OpsGauge("Budget", budgetRatio, theme, Modifier.weight(1f))
            OpsGauge("Issues", issuesRatio, theme, Modifier.weight(1f))
            OpsGauge("Milestones", milestonesRatio, theme, Modifier.weight(1f))
        }
    }
}

@Composable
private fun OpsSparklinePlaceholder(theme: BusinessActiveTheme) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp),
    ) {
        val path = Path()
        val w = size.width
        val h = size.height
        path.moveTo(0f, h * 0.7f)
        path.lineTo(w * 0.2f, h * 0.5f)
        path.lineTo(w * 0.4f, h * 0.6f)
        path.lineTo(w * 0.6f, h * 0.35f)
        path.lineTo(w * 0.8f, h * 0.45f)
        path.lineTo(w, h * 0.3f)
        drawPath(
            path = path,
            color = OpsColors.Lavender.copy(alpha = 0.35f),
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
        )
    }
}

@Composable
private fun OpsGauge(
    label: String,
    ratio: Float?,
    theme: BusinessActiveTheme,
    modifier: Modifier = Modifier,
) {
    val labelValue = ratio?.let { "${(it * 100).toInt()}%" } ?: "—"
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(56.dp)) {
            CircularProgressIndicator(
                progress = { ratio?.coerceIn(0f, 1f) ?: 0f },
                modifier = Modifier.size(56.dp),
                color = if (ratio != null) theme.accent else theme.border,
                strokeWidth = 6.dp,
                trackColor = theme.border.copy(alpha = 0.45f),
                strokeCap = StrokeCap.Round,
            )
            Text(
                labelValue,
                color = theme.text,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = PlusJakartaSans,
            )
        }
        Text(
            label,
            color = theme.secondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = PlusJakartaSans,
        )
    }
}

@Composable
fun OpsMemoryHeroSection(
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
            OpsHeroHealthRing(
                score = ringLabel,
                showLive = showLive,
                ringSizeDp = 130,
                theme = theme,
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Operations Memory",
                    color = theme.muted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = PlusJakartaSans,
                )
                Text(
                    if (ringLabel == "—") "Awaiting signal" else "Optimizing",
                    color = theme.text,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = PlusJakartaSans,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OpsHeroStatInline(learnings, "LEARNINGS", theme)
                    OpsHeroStatInline(patterns, "PATTERNS", theme)
                    OpsHeroStatInline(accuracy, "ACCURACY", theme)
                }
            }
        }
    }
}

@Composable
private fun OpsHeroStatInline(value: String, label: String, theme: BusinessActiveTheme) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            value,
            color = theme.text,
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = PlusJakartaSans,
        )
        Text(
            label,
            color = theme.muted,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = PlusJakartaSans,
        )
    }
}

@Composable
fun OpsBiggestLearningCard(
    quote: String?,
    theme: BusinessActiveTheme,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            "Biggest Learning",
            color = theme.text,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = PlusJakartaSans,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(theme.card)
                .border(1.dp, theme.border, RoundedCornerShape(16.dp)),
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(80.dp)
                    .background(OpsColors.Lavender),
            )
            Text(
                quote ?: "Your top ops learning appears here once memories are recorded.",
                color = if (quote != null) theme.text else theme.secondary,
                fontSize = if (quote != null) 14.sp else 12.sp,
                fontWeight = if (quote != null) FontWeight.SemiBold else FontWeight.Normal,
                fontStyle = if (quote != null) androidx.compose.ui.text.font.FontStyle.Italic else androidx.compose.ui.text.font.FontStyle.Normal,
                fontFamily = PlusJakartaSans,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@Composable
fun OpsPatternNetworkSection(theme: BusinessActiveTheme, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            "Pattern Network",
            color = theme.text,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = PlusJakartaSans,
        )
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
                "Cross-memory patterns stay empty until live learnings exist.",
                color = theme.secondary,
                fontSize = 12.sp,
                fontFamily = PlusJakartaSans,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    "Cause",
                    color = theme.muted,
                    fontSize = 11.sp,
                    fontFamily = PlusJakartaSans,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(theme.bg)
                        .border(1.dp, theme.border, RoundedCornerShape(999.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                )
                Text("→", color = theme.muted, fontSize = 12.sp)
                Text(
                    "Effect",
                    color = theme.muted,
                    fontSize = 11.sp,
                    fontFamily = PlusJakartaSans,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(theme.bg)
                        .border(1.dp, theme.border, RoundedCornerShape(999.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                )
                Spacer(Modifier.weight(1f))
                Text("—", color = theme.muted, fontSize = 11.sp, fontFamily = PlusJakartaSans)
            }
        }
    }
}

@Composable
fun OpsPlaybookSection(theme: BusinessActiveTheme, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            "Business Playbook",
            color = theme.text,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = PlusJakartaSans,
        )
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
                "Playbook entries are deferred — empty shell only.",
                color = theme.secondary,
                fontSize = 12.sp,
                fontFamily = PlusJakartaSans,
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(theme.border),
            )
        }
    }
}

@Composable
fun OpsWisdomQuoteSection(theme: BusinessActiveTheme, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(theme.card)
            .border(1.dp, OpsColors.Lavender.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            "\"Operations wisdom compounds with every recorded learning.\"",
            color = theme.text,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
            fontFamily = PlusJakartaSans,
        )
        Text(
            "momentra intelligence",
            color = theme.muted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = PlusJakartaSans,
        )
    }
}

@Composable
fun OpsKnowledgeJourneySection(
    items: List<Map<String, Any?>>,
    theme: BusinessActiveTheme,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            "Knowledge Journey",
            color = theme.text,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = PlusJakartaSans,
        )
        if (items.isEmpty()) {
            Text(
                "Journey timeline appears when memory history is projected.",
                color = theme.secondary,
                fontSize = 12.sp,
                fontFamily = PlusJakartaSans,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(theme.card)
                    .border(1.dp, theme.border, RoundedCornerShape(16.dp))
                    .padding(16.dp),
            )
        } else {
            items.take(5).forEach { item ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(theme.card)
                        .border(1.dp, theme.border, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(theme.accent),
                    )
                    Text(
                        item["title"]?.toString()?.takeIf { it.isNotBlank() } ?: "Memory",
                        color = theme.text,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = PlusJakartaSans,
                    )
                }
            }
        }
    }
}

@Composable
fun OpsMemoryListSection(
    title: String,
    emptyCopy: String,
    items: List<Map<String, Any?>>,
    theme: BusinessActiveTheme,
    accentBorder: Color? = null,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title,
            color = theme.text,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = PlusJakartaSans,
        )
        if (items.isEmpty()) {
            Text(
                emptyCopy,
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
            items.forEach { item ->
                val titleText = item["title"]?.toString()?.takeIf { it.isNotBlank() } ?: "Memory"
                val body = item["body"]?.toString()?.takeIf { it.isNotBlank() }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(theme.card)
                        .border(
                            1.dp,
                            accentBorder?.copy(alpha = 0.35f) ?: theme.border,
                            RoundedCornerShape(12.dp),
                        )
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        titleText,
                        color = theme.text,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = PlusJakartaSans,
                    )
                    body?.let {
                        Text(it, color = theme.secondary, fontSize = 12.sp, fontFamily = PlusJakartaSans)
                    }
                }
            }
        }
    }
}

@Composable
fun OpsScopeDropdown(
    label: String,
    theme: BusinessActiveTheme,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(theme.card)
            .border(1.dp, theme.border, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            label,
            color = theme.text,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = PlusJakartaSans,
        )
        Text("▾", color = theme.secondary, fontSize = 12.sp)
    }
}

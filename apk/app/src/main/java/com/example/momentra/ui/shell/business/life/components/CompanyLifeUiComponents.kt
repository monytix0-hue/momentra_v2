package com.example.momentra.ui.shell.business.life.components

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
import com.example.momentra.data.api.BusinessLifeActivityDto
import com.example.momentra.data.api.BusinessLifeJourneyDto
import com.example.momentra.data.api.BusinessLifeModuleCardDto
import com.example.momentra.data.api.BusinessLifeSignalDto
import com.example.momentra.data.api.BusinessLifeTrendsDto
import com.example.momentra.ui.theme.PlusJakartaSans

object CompanyLifeColors {
    val Bg = Color(0xFF0C0F15)
    val Card = Color(0xFF161B26)
    val Border = Color(0xFF1E293B)
    val Text = Color(0xFFE5E0EE)
    val Secondary = Color(0xFF94A3B8)
    val Muted = Color(0xFF64748B)
    val Indigo = Color(0xFF818CF8)
    val IndigoSolid = Color(0xFF6366F1)
    val Lavender = Color(0xFFA78BFA)
    val Team = Color(0xFF10B981)
    val Runway = Color(0xFFF59E0B)
    val Ops = Color(0xFFA78BFA)
    val Red = Color(0xFFEF4444)
    val Watch = Color(0xFFF59E0B)
}

enum class CompanyLifeFilter(val label: String, val familyKey: String?) {
    ALL("All Modules", null),
    TEAM("Team Ops", "TEAM_OPS"),
    RUNWAY("Runway", "RUNWAY"),
    OPS("Biz Ops", "OPERATIONS"),
}

fun companyLifeFamilyColor(family: String?): Color = when (family?.uppercase()) {
    "TEAM_OPS", "TEAM_OPERATIONS" -> CompanyLifeColors.Team
    "RUNWAY", "BUSINESS_RUNWAY" -> CompanyLifeColors.Runway
    else -> CompanyLifeColors.Ops
}

@Composable
fun CompanyLifeFilterChips(
    selected: CompanyLifeFilter,
    onSelect: (CompanyLifeFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CompanyLifeFilter.entries.forEach { filter ->
            val accent = when (filter) {
                CompanyLifeFilter.ALL -> CompanyLifeColors.Indigo
                CompanyLifeFilter.TEAM -> CompanyLifeColors.Team
                CompanyLifeFilter.RUNWAY -> CompanyLifeColors.Runway
                CompanyLifeFilter.OPS -> CompanyLifeColors.Lavender
            }
            val selectedChip = selected == filter
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(100.dp))
                    .background(accent.copy(alpha = if (selectedChip) 0.12f else 0.06f))
                    .border(1.dp, accent.copy(alpha = if (selectedChip) 0.35f else 0.2f), RoundedCornerShape(100.dp))
                    .clickable { onSelect(filter) }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(accent),
                )
                Text(
                    filter.label,
                    color = accent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = PlusJakartaSans,
                )
            }
        }
    }
}

@Composable
fun CompanyLifeHealthRing(
    score: String,
    ringSizeDp: Int = 106,
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
                color = CompanyLifeColors.Indigo.copy(alpha = 0.25f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(pad, pad),
                size = Size(size.width - stroke, size.height - stroke),
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            if (fraction > 0f) {
                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(CompanyLifeColors.Indigo, CompanyLifeColors.Lavender, CompanyLifeColors.Indigo),
                    ),
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
                color = CompanyLifeColors.Text,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = PlusJakartaSans,
            )
            if (score != "—") {
                Text(
                    "/100",
                    color = CompanyLifeColors.Muted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = PlusJakartaSans,
                )
            }
        }
    }
}

@Composable
fun CompanyLifeHealthHeader(
    score: String,
    narrative: String,
    subtitle: String,
    activeModules: String,
    totalMoments: String,
    avgRunway: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CompanyLifeColors.Card)
            .border(1.dp, CompanyLifeColors.Border, RoundedCornerShape(20.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "BUSINESS LIFE",
                color = CompanyLifeColors.Indigo,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
            )
            Text(
                "Your business, unified",
                color = CompanyLifeColors.Text,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
            )
            Text(
                "Health across team operations, financial runway, and business operations.",
                color = CompanyLifeColors.Secondary,
                fontSize = 13.sp,
                fontFamily = PlusJakartaSans,
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CompanyLifeHealthRing(score = score)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    narrative,
                    color = CompanyLifeColors.Text,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = PlusJakartaSans,
                )
                Text(
                    subtitle,
                    color = CompanyLifeColors.Secondary,
                    fontSize = 13.sp,
                    fontFamily = PlusJakartaSans,
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(CompanyLifeColors.Border),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            CompanyLifeStatCell(label = "Active Modules", value = activeModules)
            CompanyLifeStatCell(label = "Total Moments", value = totalMoments)
            CompanyLifeStatCell(label = "Avg Runway", value = avgRunway)
        }
    }
}

@Composable
private fun CompanyLifeStatCell(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            label.uppercase(),
            color = CompanyLifeColors.Muted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = PlusJakartaSans,
        )
        Text(
            value,
            color = CompanyLifeColors.Text,
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = PlusJakartaSans,
        )
    }
}

@Composable
fun CompanyLifeModuleCards(
    team: BusinessLifeModuleCardDto?,
    runway: BusinessLifeModuleCardDto?,
    ops: BusinessLifeModuleCardDto?,
    vendor: BusinessLifeModuleCardDto? = null,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Active Business Systems",
            color = CompanyLifeColors.Text,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = PlusJakartaSans,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CompanyLifeModuleCard(
                title = "Team Operations",
                accent = CompanyLifeColors.Team,
                card = team,
                fallbackSubtitle = "Delivery Command",
                modifier = Modifier.weight(1f),
            )
            CompanyLifeModuleCard(
                title = "Business Runway",
                accent = CompanyLifeColors.Runway,
                card = runway,
                fallbackSubtitle = "Runway",
                runwayMonths = runway?.runwayMonths,
                modifier = Modifier.weight(1f),
            )
            CompanyLifeModuleCard(
                title = "Business Operations",
                accent = CompanyLifeColors.Ops,
                card = ops,
                fallbackSubtitle = "Control Center",
                modifier = Modifier.weight(1f),
            )
        }
        if (vendor?.active == true) {
            CompanyLifeModuleCard(
                title = "Vendor Operations",
                accent = CompanyLifeColors.Ops,
                card = vendor,
                fallbackSubtitle = "Vendor health",
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun CompanyLifeModuleCard(
    title: String,
    accent: Color,
    card: BusinessLifeModuleCardDto?,
    fallbackSubtitle: String,
    modifier: Modifier = Modifier,
    runwayMonths: String? = null,
) {
    val active = card?.active == true
    val subtitle = when {
        !active -> "Not activated"
        !runwayMonths.isNullOrBlank() -> "$runwayMonths months"
        !card?.statusLabel.isNullOrBlank() -> card!!.statusLabel!!
        else -> fallbackSubtitle
    }
    val score = if (active) (card?.score?.takeIf { it.isNotBlank() } ?: "—") else "—"
    val statusChip = when {
        !active -> "Inactive"
        !card?.statusLabel.isNullOrBlank() -> card!!.statusLabel!!
        else -> "Active"
    }
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(accent.copy(alpha = 0.04f))
            .border(1.dp, accent.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                title.uppercase(),
                color = accent,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                subtitle,
                color = accent.copy(alpha = 0.85f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                score,
                color = CompanyLifeColors.Text,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = PlusJakartaSans,
            )
            Text(
                statusChip,
                color = accent,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
        }
    }
}

@Composable
fun CompanyLifeSignalsSection(
    signals: List<BusinessLifeSignalDto>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Cross-Module Signals",
                color = CompanyLifeColors.Text,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = PlusJakartaSans,
            )
            Text(
                if (signals.isEmpty()) "0 signals" else "${signals.size} signals",
                color = CompanyLifeColors.Secondary,
                fontSize = 11.sp,
                fontFamily = PlusJakartaSans,
            )
        }
        if (signals.isEmpty()) {
            CompanyLifeEmptyCard("No signals yet")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                signals.forEach { signal ->
                    CompanyLifeSignalCard(signal)
                }
            }
        }
    }
}

@Composable
private fun CompanyLifeSignalCard(signal: BusinessLifeSignalDto) {
    val familyColor = companyLifeFamilyColor(signal.family)
    val status = signal.statusLabel.ifBlank { "Watch" }
    val statusColor = when (status.uppercase()) {
        "HEALTHY" -> CompanyLifeColors.Team
        "ACTION" -> CompanyLifeColors.Red
        else -> CompanyLifeColors.Watch
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CompanyLifeColors.Card)
            .border(1.dp, CompanyLifeColors.Border, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                familyLabel(signal.family),
                color = familyColor,
                fontSize = 9.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = PlusJakartaSans,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(familyColor.copy(alpha = 0.12f))
                    .padding(horizontal = 6.dp, vertical = 3.dp),
            )
            Text(
                status,
                color = statusColor,
                fontSize = 9.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = PlusJakartaSans,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(statusColor.copy(alpha = 0.1f))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
        Text(
            signal.title,
            color = CompanyLifeColors.Text,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = PlusJakartaSans,
        )
    }
}

@Composable
fun CompanyLifeActivitySection(
    items: List<BusinessLifeActivityDto>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Live Activity Feed",
            color = CompanyLifeColors.Text,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = PlusJakartaSans,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CompanyLifeColors.Card)
                .border(1.dp, CompanyLifeColors.Border, RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (items.isEmpty()) {
                Text(
                    "No activity yet",
                    color = CompanyLifeColors.Secondary,
                    fontSize = 13.sp,
                    fontFamily = PlusJakartaSans,
                )
            } else {
                items.forEach { item ->
                    CompanyLifeActivityRow(item)
                }
            }
        }
    }
}

@Composable
private fun CompanyLifeActivityRow(item: BusinessLifeActivityDto) {
    val accent = companyLifeFamilyColor(item.family)
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(accent),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(36.dp)
                    .background(CompanyLifeColors.Border),
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    item.title.ifBlank { item.activityCode },
                    color = CompanyLifeColors.Text,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = PlusJakartaSans,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    formatLifeDate(item.occurredAt),
                    color = CompanyLifeColors.Secondary,
                    fontSize = 10.sp,
                    fontFamily = PlusJakartaSans,
                )
            }
            Text(
                item.description?.takeIf { it.isNotBlank() }
                    ?: "${familyDisplay(item.family)} · ${item.activityCode}",
                color = CompanyLifeColors.Secondary,
                fontSize = 12.sp,
                fontFamily = PlusJakartaSans,
            )
        }
    }
}

@Composable
fun CompanyLifeJourneySection(
    steps: List<BusinessLifeJourneyDto>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Business Journey",
            color = CompanyLifeColors.Text,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = PlusJakartaSans,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CompanyLifeColors.Card)
                .border(1.dp, CompanyLifeColors.Border, RoundedCornerShape(16.dp))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            if (steps.isEmpty()) {
                Text(
                    "No modules activated yet",
                    color = CompanyLifeColors.Secondary,
                    fontSize = 13.sp,
                    fontFamily = PlusJakartaSans,
                )
            } else {
                steps.forEach { step ->
                    val accent = companyLifeFamilyColor(step.family)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(accent),
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                journeyTitle(step),
                                color = CompanyLifeColors.Text,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = PlusJakartaSans,
                            )
                            Text(
                                formatLifeDateLong(step.createdAt),
                                color = CompanyLifeColors.Secondary,
                                fontSize = 11.sp,
                                fontFamily = PlusJakartaSans,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CompanyLifeTrendsSection(
    trends: BusinessLifeTrendsDto?,
    modifier: Modifier = Modifier,
) {
    val series = trends?.series.orEmpty()
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Health Trends (6-Month)",
            color = CompanyLifeColors.Text,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = PlusJakartaSans,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(CompanyLifeColors.Card)
                .border(1.dp, CompanyLifeColors.Border, RoundedCornerShape(12.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (series.isEmpty()) {
                Text(
                    "Trend history builds as monthly snapshots are recorded.",
                    color = CompanyLifeColors.Secondary,
                    fontSize = 12.sp,
                    fontFamily = PlusJakartaSans,
                )
            } else {
                series.forEach { point ->
                    val score = point.financialHealthScore?.toString() ?: "—"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            point.month,
                            color = CompanyLifeColors.Secondary,
                            fontSize = 12.sp,
                            fontFamily = PlusJakartaSans,
                        )
                        Text(
                            score,
                            color = CompanyLifeColors.Text,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = PlusJakartaSans,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CompanyLifeTrendsDeferred(modifier: Modifier = Modifier) {
    CompanyLifeTrendsSection(trends = null, modifier = modifier)
}

@Composable
fun CompanyLifeGradientButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(CompanyLifeColors.Indigo, CompanyLifeColors.IndigoSolid),
                ),
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
fun CompanyLifeOutlineButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, CompanyLifeColors.Border, RoundedCornerShape(24.dp))
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = CompanyLifeColors.Text.copy(alpha = if (enabled) 1f else 0.45f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = PlusJakartaSans,
        )
    }
}

@Composable
private fun CompanyLifeEmptyCard(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CompanyLifeColors.Card)
            .border(1.dp, CompanyLifeColors.Border, RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        Text(
            message,
            color = CompanyLifeColors.Secondary,
            fontSize = 13.sp,
            fontFamily = PlusJakartaSans,
        )
    }
}

private fun familyLabel(family: String?): String = when (family?.uppercase()) {
    "TEAM_OPS", "TEAM_OPERATIONS" -> "TEAM OPS"
    "RUNWAY", "BUSINESS_RUNWAY" -> "RUNWAY"
    else -> "OPERATIONS"
}

private fun familyDisplay(family: String?): String = when (family?.uppercase()) {
    "TEAM_OPS", "TEAM_OPERATIONS" -> "Team Ops"
    "RUNWAY", "BUSINESS_RUNWAY" -> "Runway"
    else -> "Operations"
}

private fun journeyTitle(step: BusinessLifeJourneyDto): String {
    val labeled = when (step.familyCode.uppercase()) {
        "TEAM_OPERATIONS" -> "Team Operations activated"
        "BUSINESS_RUNWAY" -> "Business Runway launched"
        "BUSINESS_OPERATIONS" -> "Operations module added"
        else -> step.title.ifBlank { "${familyDisplay(step.family)} activated" }
    }
    return labeled
}

private fun formatLifeDate(iso: String): String {
    if (iso.length < 10) return iso
    val parts = iso.take(10).split("-")
    if (parts.size != 3) return iso.take(10)
    val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    val month = parts[1].toIntOrNull()?.let { months.getOrNull(it - 1) } ?: parts[1]
    return "${parts[2].toIntOrNull() ?: parts[2]} $month"
}

private fun formatLifeDateLong(iso: String): String {
    if (iso.length < 10) return iso
    val parts = iso.take(10).split("-")
    if (parts.size != 3) return iso.take(10)
    val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    val month = parts[1].toIntOrNull()?.let { months.getOrNull(it - 1) } ?: parts[1]
    return "${parts[2].toIntOrNull() ?: parts[2]} $month ${parts[0]}"
}

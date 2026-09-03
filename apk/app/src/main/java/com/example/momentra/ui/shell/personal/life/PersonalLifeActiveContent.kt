package com.example.momentra.ui.shell.personal.life

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.api.LifeAreaScoreDto
import com.example.momentra.data.api.LifeBalanceAxisDto
import com.example.momentra.data.api.LifeEmotionSegmentDto
import com.example.momentra.data.api.LifeEmotionSeriesDto
import com.example.momentra.data.api.LifeImpactDto
import com.example.momentra.data.api.LifeJourneyItemDto
import com.example.momentra.data.api.PersonalLifeDto
import com.example.momentra.data.repository.PersonalSliceRepository
import com.example.momentra.ui.theme.PlusJakartaSans

private val LifeBg = Color(0xFF14121B)
private val LifeCard = Color(0xFF1C1B2E)
private val LifeCardAlt = Color(0xFF161B26)
private val LifeText = Color(0xFFE5E0EE)
private val LifeMuted = Color(0xFFC9C4D8)
private val LifeDim = Color(0xFF8C8C9E)
private val LifePurple = Color(0xFF7C5CFC)
private val LifeGreen = Color(0xFF10B981)
private val LifeRed = Color(0xFFEF4444)
private val LifeAmber = Color(0xFFF59E0B)
private val LifeBlue = Color(0xFF3B82F6)
private val LifePink = Color(0xFFE12A9E)
private val BorderSoft = Color.White.copy(alpha = 0.08f)

/** Figma `1047:7689` body — Personal Life populated (cross-moment). */
@Composable
fun PersonalLifeActiveContent(
    refreshToken: Long,
    onLogRecovery: () -> Unit,
    repository: PersonalSliceRepository = remember { PersonalSliceRepository() },
    modifier: Modifier = Modifier,
) {
    var loading by remember { mutableStateOf(true) }
    var life by remember { mutableStateOf<PersonalLifeDto?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var selectedChip by remember { mutableStateOf("Life Health") }

    LaunchedEffect(refreshToken) {
        if (life != null) loading = false else loading = true
        error = null
        repository.getLife().fold(
            onSuccess = { life = it; loading = false },
            onFailure = { error = it.message; loading = false },
        )
    }

    if (loading && life == null) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = LifePurple)
        }
        return
    }

    val data = life
    if (data == null) {
        Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(
                error ?: "Life projection unavailable",
                color = LifeMuted,
                fontSize = 14.sp,
                fontFamily = PlusJakartaSans,
            )
        }
        return
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LifeBg)
            .verticalScroll(rememberScrollState()),
    ) {
        LifeChipRow(
            selected = selectedChip,
            onSelect = { selectedChip = it },
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (data.sectionQuality.values.any { it.equals("API_GAP", ignoreCase = true) }) {
                Text(
                    "Some Life sections are not available yet. Core areas and journey data are live when present.",
                    color = LifeAmber,
                    fontSize = 11.sp,
                    fontFamily = PlusJakartaSans,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(LifeCard)
                        .border(1.dp, LifeAmber.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                )
            }
            error?.let {
                Text(it, color = LifeRed, fontSize = 12.sp, fontFamily = PlusJakartaSans)
            }
            LifeHealthSummaryCard(data)
            LifeDriftCard(data)
            LifeLeverageCard(data, onLogRecovery = onLogRecovery)
            LifeBalanceSection(data.balance)
            LifeEmotionalTrendCard(data)
            LifeDominantEmotionCard(data)
            LifeHappyDriversCard(data)
            LifeJourneyCard(data)
            LifeAiInsightsCard(data)
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun LifeChipRow(
    selected: String,
    onSelect: (String) -> Unit,
) {
    val chips = listOf(
        "Life Health" to LifePurple,
        "Future Building" to LifeBlue,
        "Lifestyle" to LifeAmber,
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0C0F15)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            chips.forEach { (label, dot) ->
                val active = selected == label
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (active) LifePurple.copy(alpha = 0.12f) else LifeCardAlt,
                        )
                        .border(
                            width = if (active) 1.5.dp else 1.dp,
                            color = if (active) LifePurple.copy(alpha = 0.5f) else Color(0xFF1E293B).copy(alpha = 0.4f),
                            shape = RoundedCornerShape(20.dp),
                        )
                        .clickable { onSelect(label) }
                        .padding(horizontal = if (active) 12.dp else 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(dot),
                    )
                    Text(
                        label,
                        color = if (active) Color.White else LifeDim,
                        fontSize = if (active) 12.sp else 11.sp,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                        fontFamily = PlusJakartaSans,
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(LifeCardAlt)
                    .border(1.dp, Color(0xFF1E293B).copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text("⚙", color = Color(0xFF808094), fontSize = 12.sp)
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0xFF1E293B).copy(alpha = 0.3f)),
        )
    }
}

@Composable
private fun LifeHealthSummaryCard(data: PersonalLifeDto) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(LifeCard)
            .border(1.dp, BorderSoft, RoundedCornerShape(24.dp))
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF7C3AED).copy(alpha = 0.18f),
                            Color.Transparent,
                        ),
                    ),
                    radius = size.minDimension * 0.45f,
                    center = Offset(size.width * 0.92f, size.height * 0.85f),
                )
            }
            .padding(20.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "PERSONAL LIFE HEALTH",
                        color = LifeDim,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = PlusJakartaSans,
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            "${data.score}",
                            color = LifeText,
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = PlusJakartaSans,
                        )
                        Text(
                            "/${data.scoreMax}",
                            color = LifeMuted,
                            fontSize = 16.sp,
                            fontFamily = PlusJakartaSans,
                            modifier = Modifier.padding(bottom = 10.dp, start = 2.dp),
                        )
                    }
                    Text(
                        data.statusLabel,
                        color = LifeText,
                        fontSize = 14.sp,
                        fontFamily = PlusJakartaSans,
                    )
                    Text(
                        data.trendLabel,
                        color = LifeGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = PlusJakartaSans,
                    )
                }
                LifeScoreRing(score = data.score, areas = data.areaScores)
            }
            if (data.insight.isNotBlank()) {
                Text(
                    "\"${data.insight}\"",
                    color = LifeMuted,
                    fontSize = 13.sp,
                    fontFamily = PlusJakartaSans,
                    lineHeight = 18.sp,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                data.areaScores.chunked(2).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        row.forEach { area ->
                            AreaScoreChip(area, Modifier.weight(1f))
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun AreaScoreChip(area: LifeAreaScoreDto, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(LifeCardAlt.copy(alpha = 0.6f))
            .padding(horizontal = 10.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(parseHexColor(area.color)),
        )
        Text(
            "${area.label}: ${area.score}",
            color = LifeText,
            fontSize = 12.sp,
            fontFamily = PlusJakartaSans,
        )
    }
}

@Composable
private fun LifeScoreRing(score: Int, areas: List<LifeAreaScoreDto>) {
    val colors = areas.map { parseHexColor(it.color) }.ifEmpty {
        listOf(LifeBlue, LifeGreen, LifeAmber, LifePink)
    }
    Box(modifier = Modifier.size(110.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(110.dp)) {
            val stroke = 8.dp.toPx()
            val pad = stroke / 2
            colors.forEachIndexed { i, c ->
                val inset = i * (stroke + 4.dp.toPx())
                drawArc(
                    color = c.copy(alpha = 0.85f),
                    startAngle = -90f + i * 20f,
                    sweepAngle = 220f + (score / 100f) * 40f,
                    useCenter = false,
                    topLeft = Offset(pad + inset, pad + inset),
                    size = Size(size.width - 2 * (pad + inset), size.height - 2 * (pad + inset)),
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
        }
        Text(
            "$score",
            color = LifeText,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = PlusJakartaSans,
        )
    }
}

@Composable
private fun LifeDriftCard(data: PersonalLifeDto) {
    val drift = data.drift ?: return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                drawRoundRect(
                    brush = Brush.radialGradient(
                        colors = listOf(LifeRed.copy(alpha = 0.28f), Color.Transparent),
                        center = center,
                        radius = size.maxDimension * 0.7f,
                    ),
                )
            }
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF2A1520))
            .border(1.dp, LifeRed.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            drift.title,
            color = LifeRed,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = PlusJakartaSans,
        )
        Text(
            drift.headline,
            color = LifeText,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = PlusJakartaSans,
        )
        Text(
            drift.body,
            color = LifeMuted,
            fontSize = 13.sp,
            fontFamily = PlusJakartaSans,
            lineHeight = 18.sp,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1A1218))
                .border(1.dp, LifeRed.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                .clickable { /* View Suggestions — no destination yet */ }
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                drift.ctaLabel,
                color = LifeRed,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = PlusJakartaSans,
            )
        }
    }
}

@Composable
private fun LifeLeverageCard(data: PersonalLifeDto, onLogRecovery: () -> Unit) {
    val lev = data.leverage ?: return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(LifeCard)
            .border(1.dp, LifeGreen.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("🎯", fontSize = 12.sp)
            Text(
                lev.title,
                color = LifeGreen,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    lev.actionTitle,
                    color = LifeText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = PlusJakartaSans,
                )
                Text(
                    lev.actionBody,
                    color = LifeMuted,
                    fontSize = 12.sp,
                    fontFamily = PlusJakartaSans,
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(LifeGreen.copy(alpha = 0.15f))
                    .border(1.dp, LifeGreen.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                    .clickable(onClick = onLogRecovery)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text(
                    lev.ctaLabel,
                    color = LifeGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = PlusJakartaSans,
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(BorderSoft),
        )
        Text(
            "EXPECTED IMPACT",
            color = LifeDim,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = PlusJakartaSans,
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            lev.impacts.forEach { impact ->
                ImpactCell(impact)
            }
        }
    }
}

@Composable
private fun ImpactCell(impact: LifeImpactDto) {
    val tone = when (impact.tone) {
        "up" -> LifeGreen
        "down" -> LifeRed
        else -> LifeMuted
    }
    Column(horizontalAlignment = Alignment.Start) {
        Text(impact.label, color = LifeDim, fontSize = 11.sp, fontFamily = PlusJakartaSans)
        Text(
            impact.delta,
            color = tone,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = PlusJakartaSans,
        )
    }
}

@Composable
private fun LifeBalanceSection(axes: List<LifeBalanceAxisDto>) {
    if (axes.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Life Balance Model",
            color = LifeText,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = PlusJakartaSans,
        )
        axes.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                row.forEach { axis ->
                    BalanceTile(axis, Modifier.weight(1f))
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun BalanceTile(axis: LifeBalanceAxisDto, modifier: Modifier = Modifier) {
    val badgeColor = when (axis.badgeTone) {
        "amber" -> LifeAmber
        "green" -> LifeGreen
        "blue" -> LifeBlue
        "pink" -> LifePink
        else -> LifePurple
    }
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(LifeCardAlt)
            .border(1.dp, BorderSoft, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                axis.label,
                color = LifeDim,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(badgeColor.copy(alpha = 0.15f))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(
                    axis.badge,
                    color = badgeColor,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = PlusJakartaSans,
                )
            }
        }
        Text(
            "${axis.score}",
            color = LifeText,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = PlusJakartaSans,
        )
    }
}

@Composable
private fun LifeEmotionalTrendCard(data: PersonalLifeDto) {
    val trend = data.emotionalTrend ?: return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(LifeCard)
            .border(1.dp, BorderSoft, RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column {
            Text(
                "Emotional Trend",
                color = LifeText,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = PlusJakartaSans,
            )
            Text(
                trend.subtitle,
                color = LifeDim,
                fontSize = 12.sp,
                fontFamily = PlusJakartaSans,
            )
        }
        EmotionalTrendChart(series = trend.series, modifier = Modifier.fillMaxWidth().height(120.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            trend.series.chunked(2).forEach { col ->
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                    col.forEach { s ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(parseHexColor(s.color)),
                            )
                            Text(s.label, color = LifeMuted, fontSize = 11.sp, fontFamily = PlusJakartaSans)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmotionalTrendChart(
    series: List<LifeEmotionSeriesDto>,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val gridYs = listOf(0.25f, 0.5f, 0.75f)
        gridYs.forEach { f ->
            val y = h * f
            drawLine(
                color = Color.White.copy(alpha = 0.06f),
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = 1.dp.toPx(),
            )
        }
        series.forEach { s ->
            val pts = s.points
            if (pts.size < 2) return@forEach
            val maxV = 100.0
            val minV = 0.0
            val path = Path()
            pts.forEachIndexed { i, v ->
                val x = w * (i.toFloat() / (pts.size - 1).coerceAtLeast(1))
                val y = h * (1f - ((v - minV) / (maxV - minV)).toFloat().coerceIn(0f, 1f))
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(
                path = path,
                color = parseHexColor(s.color),
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
            )
        }
    }
}

@Composable
private fun LifeDominantEmotionCard(data: PersonalLifeDto) {
    val dom = data.dominantEmotion ?: return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(LifeCard)
            .border(1.dp, BorderSoft, RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            dom.title,
            color = LifeText,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = PlusJakartaSans,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            DominantDonut(segments = dom.segments, modifier = Modifier.size(80.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                Text(
                    dom.headline,
                    color = LifeText,
                    fontSize = 13.sp,
                    fontFamily = PlusJakartaSans,
                    lineHeight = 18.sp,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    dom.segments.filter { it.label != "Connection" && it.label != "Other" }.take(3).forEach { seg ->
                        Text(
                            "${seg.label} (${seg.percent}%)",
                            color = LifeDim,
                            fontSize = 11.sp,
                            fontFamily = PlusJakartaSans,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DominantDonut(segments: List<LifeEmotionSegmentDto>, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stroke = 12.dp.toPx()
        val total = segments.sumOf { it.percent }.coerceAtLeast(1)
        var start = -90f
        segments.forEach { seg ->
            val sweep = 360f * (seg.percent.toFloat() / total)
            drawArc(
                color = parseHexColor(seg.color),
                startAngle = start,
                sweepAngle = sweep,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Butt),
                topLeft = Offset(stroke / 2, stroke / 2),
                size = Size(this.size.width - stroke, this.size.height - stroke),
            )
            start += sweep
        }
    }
}

@Composable
private fun LifeHappyDriversCard(data: PersonalLifeDto) {
    val happy = data.happyDrivers ?: return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(LifeCard)
            .border(1.dp, BorderSoft, RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column {
            Text(
                happy.title,
                color = LifeText,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = PlusJakartaSans,
            )
            Text(
                happy.subtitle,
                color = LifeDim,
                fontSize = 12.sp,
                fontFamily = PlusJakartaSans,
            )
        }
        happy.items.forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(LifePurple.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("✨", fontSize = 10.sp)
                }
                Text(item, color = LifeText, fontSize = 13.sp, fontFamily = PlusJakartaSans)
            }
        }
    }
}

@Composable
private fun LifeJourneyCard(data: PersonalLifeDto) {
    val journey = data.journey ?: return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(LifeCard)
            .border(1.dp, BorderSoft, RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column {
            Text(
                journey.title,
                color = LifeText,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = PlusJakartaSans,
            )
            Text(
                journey.subtitle,
                color = LifeDim,
                fontSize = 12.sp,
                fontFamily = PlusJakartaSans,
            )
        }
        journey.items.forEach { item ->
            LifeJourneyItemRow(item)
        }
    }
}

@Composable
private fun LifeJourneyItemRow(item: LifeJourneyItemDto) {
    val tone = when (item.tone) {
        "up" -> LifeGreen
        "down" -> LifeRed
        else -> LifeMuted
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(LifeCardAlt),
            contentAlignment = Alignment.Center,
        ) {
            Text(item.icon, fontSize = 14.sp)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(item.title, color = LifeText, fontSize = 13.sp, fontFamily = PlusJakartaSans)
            Text(item.whenLabel, color = LifeDim, fontSize = 11.sp, fontFamily = PlusJakartaSans)
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(tone.copy(alpha = 0.12f))
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Text(
                item.value,
                color = tone,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = PlusJakartaSans,
            )
        }
    }
}

@Composable
private fun LifeAiInsightsCard(data: PersonalLifeDto) {
    val ai = data.aiInsights ?: return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF1E1A32), LifeCard),
                ),
            )
            .border(1.dp, LifePurple.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("✨", fontSize = 14.sp)
            Text(
                ai.title,
                color = LifeText,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = PlusJakartaSans,
            )
        }
        Text(
            ai.lead,
            color = LifeText,
            fontSize = 13.sp,
            fontFamily = PlusJakartaSans,
            lineHeight = 18.sp,
        )
        Text(
            ai.body,
            color = LifeMuted,
            fontSize = 12.sp,
            fontFamily = PlusJakartaSans,
            lineHeight = 17.sp,
        )
    }
}

private fun parseHexColor(hex: String): Color {
    return try {
        val cleaned = hex.removePrefix("#")
        val long = cleaned.toLong(16)
        when (cleaned.length) {
            6 -> Color(
                red = ((long shr 16) and 0xFF) / 255f,
                green = ((long shr 8) and 0xFF) / 255f,
                blue = (long and 0xFF) / 255f,
            )
            8 -> Color(
                alpha = ((long shr 24) and 0xFF) / 255f,
                red = ((long shr 16) and 0xFF) / 255f,
                green = ((long shr 8) and 0xFF) / 255f,
                blue = (long and 0xFF) / 255f,
            )
            else -> LifePurple
        }
    } catch (_: Exception) {
        LifePurple
    }
}

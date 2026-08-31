package com.example.momentra.ui.shell.personal

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.api.PersonalPulseDto
import com.example.momentra.data.repository.PersonalSliceRepository
import com.example.momentra.ui.theme.PlusJakartaSans
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

private val Bg = Color(0xFF14121B)
private val Card = Color(0xFF152022)
private val CardAlt = Color(0xFF161B26)
private val TextMain = Color(0xFFE5E0EE)
private val Muted = Color(0xFFC9C4D8)
private val Dim = Color(0xFF8C8C9E)
private val Teal = Color(0xFF0EA5A4)
private val TealSoft = Color(0xFF5EEAD4)
private val BorderSoft = Color.White.copy(alpha = 0.08f)

/** Figma `505:12365` — Lifestyle Vitality Index Pulse (honest scores from widgetPayload). */
@Composable
fun PersonalLifestylePulseActiveContent(
    refreshToken: Long,
    momentTitle: String?,
    momentId: String?,
    onAddExpense: () -> Unit,
    onLifestyleQuickAdd: (LifestyleQuickAddKind) -> Unit,
    onViewAllActivity: () -> Unit,
    repository: PersonalSliceRepository = remember { PersonalSliceRepository() },
    modifier: Modifier = Modifier,
) {
    var loading by remember { mutableStateOf(true) }
    var pulse by remember { mutableStateOf<PersonalPulseDto?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(refreshToken, momentId) {
        error = null
        PersonalTabDataCache.peek(momentId)?.let { cached ->
            pulse = cached.pulse
            loading = false
        } ?: run { loading = true }
        repository.getPulse(momentId = momentId).fold(
            onSuccess = { pulse = it; loading = false },
            onFailure = { error = it.message; loading = false },
        )
    }

    if (loading && pulse == null) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Teal)
        }
        return
    }

    val theme = PersonalPulseFamily.LIFESTYLE.theme()
    val vitalityScore = PersonalLifestyleDerived.vitalityIndex(pulse)
    val axes = PersonalLifestyleDerived.axisScores(pulse)
    val spendPairs = PersonalLifestyleDerived.spendPairs(pulse)
    val momentum = PersonalLifestyleDerived.momentumLabels(pulse, emptyList())

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Bg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        error?.let {
            Text(it, color = Color(0xFFF87171), fontSize = 12.sp, fontFamily = PlusJakartaSans)
        }
        if (!momentTitle.isNullOrBlank()) {
            Text(
                momentTitle,
                color = Muted,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = PlusJakartaSans,
            )
        }

        VitalityIndexHero(
            score = vitalityScore,
            subtitle = PersonalLifestyleDerived.networkStability(pulse),
            axes = axes,
            theme = theme,
        )

        MetricTilesGrid(
            labels = theme.tileLabels,
            values = listOf(axes.joy, axes.fulfillment, axes.vitality, axes.exploration),
        )

        TodaysMomentumRow(labels = momentum)

        LifestyleSpendCard(spendPairs = spendPairs, onAddExpense = onAddExpense)

        ProtectRitualCard(
            onLogExperience = { onLifestyleQuickAdd(LifestyleQuickAddKind.EXPERIENCE) },
        )

        LifestyleQuickAddRow(onLifestyleQuickAdd = onLifestyleQuickAdd)

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun VitalityIndexHero(
    score: Int?,
    subtitle: String,
    axes: PersonalLifestyleDerived.AxisScores,
    theme: PersonalPulseFamilyTheme,
) {
    val scoreLabel = score?.toString() ?: "—"
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF0D2A2A), Card)))
            .border(1.dp, Teal.copy(alpha = 0.25f), RoundedCornerShape(24.dp))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            theme.heroTitle,
            color = Dim,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = PlusJakartaSans,
        )
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                scoreLabel,
                color = TextMain,
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
            )
            if (score != null) {
                Text(
                    "/100",
                    color = Muted,
                    fontSize = 16.sp,
                    fontFamily = PlusJakartaSans,
                    modifier = Modifier.padding(bottom = 8.dp, start = 2.dp),
                )
            }
        }
        Text(subtitle, color = TealSoft, fontSize = 13.sp, fontFamily = PlusJakartaSans)
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(140.dp)) {
            Canvas(
                modifier = Modifier
                    .size(140.dp)
                    .drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Teal.copy(alpha = 0.35f), Color.Transparent),
                            ),
                            radius = size.minDimension * 0.55f,
                        )
                    },
            ) {
                val stroke = 14.dp.toPx()
                drawArc(
                    color = Teal.copy(alpha = 0.2f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                    topLeft = Offset(stroke / 2, stroke / 2),
                    size = Size(size.width - stroke, size.height - stroke),
                )
                if (score != null) {
                    drawArc(
                        color = Teal,
                        startAngle = -90f,
                        sweepAngle = 360f * (score.coerceIn(0, 100) / 100f),
                        useCenter = false,
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                        topLeft = Offset(stroke / 2, stroke / 2),
                        size = Size(size.width - stroke, size.height - stroke),
                    )
                }
            }
            Text(
                scoreLabel,
                color = TextMain,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            theme.heroMetrics.zip(
                listOf(axes.joy, axes.fulfillment, axes.vitality, axes.exploration),
            ).forEach { (label, value) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(label, color = Dim, fontSize = 10.sp, fontFamily = PlusJakartaSans)
                    Text(
                        value,
                        color = TextMain,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = PlusJakartaSans,
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricTilesGrid(labels: List<String>, values: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        labels.chunked(2).zip(values.chunked(2)).forEach { (labelRow, valueRow) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                labelRow.forEachIndexed { i, label ->
                    val value = valueRow.getOrElse(i) { "—" }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(CardAlt)
                            .border(1.dp, BorderSoft, RoundedCornerShape(16.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(label, color = Dim, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
                        Text(value, color = TextMain, fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
                        Text(
                            PersonalLifestyleDerived.statusBadge(value, label),
                            color = TealSoft,
                            fontSize = 10.sp,
                            fontFamily = PlusJakartaSans,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TodaysMomentumRow(labels: List<String>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Card)
            .border(1.dp, BorderSoft, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("TODAY'S MOMENTUM", color = Dim, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
        labels.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { label ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Teal.copy(alpha = 0.12f))
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(label, color = TextMain, fontSize = 11.sp, fontFamily = PlusJakartaSans)
                    }
                }
            }
        }
    }
}

@Composable
private fun LifestyleSpendCard(
    spendPairs: List<Pair<String, String>>,
    onAddExpense: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Card)
            .border(1.dp, BorderSoft, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("LIFESTYLE SPEND", color = Dim, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
            Text(
                "+ Add Expense",
                color = Teal,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = PlusJakartaSans,
                modifier = Modifier.clickable(onClick = onAddExpense),
            )
        }
        if (spendPairs.isEmpty()) {
            Text(
                "Not projected yet — log spend to surface a snapshot.",
                color = Muted,
                fontSize = 12.sp,
                fontFamily = PlusJakartaSans,
            )
        } else {
            spendPairs.forEach { (currency, amount) ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(currency, color = Muted, fontSize = 12.sp, fontFamily = PlusJakartaSans)
                    Text(amount, color = TextMain, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
                }
            }
        }
    }
}

@Composable
private fun ProtectRitualCard(onLogExperience: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF0D2A2A))
            .border(1.5.dp, Teal.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "Protect a ritual",
            color = TextMain,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = PlusJakartaSans,
        )
        Text(
            "Log one experience to protect your lifestyle rhythm.",
            color = Muted,
            fontSize = 13.sp,
            fontFamily = PlusJakartaSans,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(999.dp))
                .background(Teal)
                .clickable(onClick = onLogExperience)
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "Log Experience",
                color = Bg,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
            )
        }
    }
}

@Composable
private fun LifestyleQuickAddRow(onLifestyleQuickAdd: (LifestyleQuickAddKind) -> Unit) {
    val actions = listOf(
        "✨" to LifestyleQuickAddKind.EXPERIENCE,
        "🌿" to LifestyleQuickAddKind.WELLBEING,
        "🔍" to LifestyleQuickAddKind.DISCOVERY,
        "🎨" to LifestyleQuickAddKind.EXPRESSION,
        "⚙" to LifestyleQuickAddKind.ADJUST,
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        actions.forEach { (emoji, kind) ->
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Card)
                    .border(1.dp, Teal.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                    .clickable { onLifestyleQuickAdd(kind) },
                contentAlignment = Alignment.Center,
            ) {
                Text(emoji, fontSize = 20.sp)
            }
        }
    }
}

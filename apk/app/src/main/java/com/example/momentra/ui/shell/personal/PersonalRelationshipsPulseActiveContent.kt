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
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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

private val RelBg = Color(0xFF14121B)
private val RelCard = Color(0xFF1C1B2E)
private val RelCardAlt = Color(0xFF161B26)
private val RelText = Color(0xFFE5E0EE)
private val RelMuted = Color(0xFFC9C4D8)
private val RelDim = Color(0xFF8C8C9E)
private val RelPink = Color(0xFFE12A9E)
private val RelPinkSoft = Color(0xFFF472B6)
private val RelPurple = Color(0xFF7C5CFC)
private val RelBlue = Color(0xFF3B82F6)
private val BorderSoft = Color.White.copy(alpha = 0.08f)

/** Figma `505:11793` — Relationships populated Pulse (honest scores; no on-device fakes). */
@Composable
fun PersonalRelationshipsPulseActiveContent(
    refreshToken: Long,
    momentTitle: String?,
    momentId: String?,
    onAddExpense: () -> Unit,
    onRelationshipsQuickAdd: (RelationshipsQuickAddKind) -> Unit,
    onOpenRecentActivity: () -> Unit,
    repository: PersonalSliceRepository = remember { PersonalSliceRepository() },
    modifier: Modifier = Modifier,
) {
    var loading by remember { mutableStateOf(true) }
    var pulse by remember { mutableStateOf<PersonalPulseDto?>(null) }
    var activities by remember { mutableStateOf<List<RelationshipsActivityItem>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(refreshToken, momentId) {
        error = null
        PersonalTabDataCache.peek(momentId)?.let { cached ->
            pulse = cached.pulse
            activities = mapActivityDtosToRelationships(cached.activities)
            loading = false
        } ?: run { loading = true }
        loadPersonalPulseTab(repository, momentId).fold(
            onSuccess = { (p, items) ->
                pulse = p
                activities = mapActivityDtosToRelationships(items)
                loading = false
            },
            onFailure = { e ->
                error = e.message
                loading = false
            },
        )
    }

    if (loading && pulse == null) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = RelPink)
        }
        return
    }

    val bondScore = PersonalRelationshipsDerived.bondIndex(pulse)
    val bondAxes = PersonalRelationshipsDerived.bondAxes(pulse)
    val spendPairs = PersonalRelationshipsDerived.spendPairs(pulse)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(RelBg)
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
                color = RelMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = PlusJakartaSans,
            )
        }

        BondIndexHero(score = bondScore, axes = bondAxes, subtitle = PersonalRelationshipsDerived.bondSubtitle(pulse))

        HonestPlaceholderCard(
            title = "CAPACITY",
            body = "Not projected yet — capacity bars appear when bond signals arrive.",
        )

        LifeSignalsRow(
            onTrust = { onRelationshipsQuickAdd(RelationshipsQuickAddKind.CONNECTION) },
            onCare = { onRelationshipsQuickAdd(RelationshipsQuickAddKind.SUPPORT) },
            onMoney = onAddExpense,
        )

        HonestPlaceholderCard(
            title = "SCORE DRIVERS & STATE",
            body = "Not projected yet — drivers and rings need API bond axes.",
        )

        RecentActivityPreview(
            items = activities.take(3),
            onViewAll = onOpenRecentActivity,
        )

        SharedSpendAndAccounts(
            spendPairs = spendPairs,
            onAddExpense = onAddExpense,
        )

        HonestPlaceholderCard(
            title = "CURRENT TRENDS (30 DAYS)",
            body = "Not projected yet — no trend series on this pulse.",
        )

        ProtectConnectionCard(
            onLogConnection = { onRelationshipsQuickAdd(RelationshipsQuickAddKind.CONNECTION) },
        )

        HonestPlaceholderCard(
            title = "INTELLIGENCE",
            body = "Not projected yet — relationship intelligence arrives with bond projections.",
        )

        AiInsightsComingSoon()

        QuickAddLauncher(onRelationshipsQuickAdd = onRelationshipsQuickAdd)

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun BondIndexHero(
    score: Int?,
    axes: PersonalRelationshipsDerived.BondAxes,
    subtitle: String,
) {
    val scoreLabel = score?.toString() ?: "—"
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(listOf(Color(0xFF2A1530), RelCard)),
            )
            .border(1.dp, RelPink.copy(alpha = 0.25f), RoundedCornerShape(24.dp))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            "BOND INDEX",
            color = RelDim,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = PlusJakartaSans,
        )
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                scoreLabel,
                color = RelText,
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
            )
            if (score != null) {
                Text(
                    "/100",
                    color = RelMuted,
                    fontSize = 16.sp,
                    fontFamily = PlusJakartaSans,
                    modifier = Modifier.padding(bottom = 8.dp, start = 2.dp),
                )
            }
        }
        Text(
            subtitle,
            color = RelPinkSoft,
            fontSize = 13.sp,
            fontFamily = PlusJakartaSans,
        )
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(140.dp)) {
            Canvas(
                modifier = Modifier
                    .size(140.dp)
                    .drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(RelPink.copy(alpha = 0.35f), Color.Transparent),
                            ),
                            radius = size.minDimension * 0.55f,
                        )
                    },
            ) {
                val stroke = 14.dp.toPx()
                drawArc(
                    color = RelPink.copy(alpha = 0.2f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                    topLeft = Offset(stroke / 2, stroke / 2),
                    size = Size(size.width - stroke, size.height - stroke),
                )
                if (score != null) {
                    drawArc(
                        color = RelPink,
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
                color = RelText,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
            )
        }
        val hasAnyAxis = listOf(axes.trust, axes.care, axes.support, axes.presence).any { it != "—" }
        Text(
            if (hasAnyAxis) "Bond axes from your logs" else "Bond axes unavailable",
            color = RelDim,
            fontSize = 11.sp,
            fontFamily = PlusJakartaSans,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            listOf(
                "Trust" to axes.trust,
                "Support" to axes.support,
                "Presence" to axes.presence,
                "Care" to axes.care,
            ).forEach { (label, value) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(label, color = RelDim, fontSize = 10.sp, fontFamily = PlusJakartaSans)
                    Text(
                        value,
                        color = RelText,
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
private fun HonestPlaceholderCard(title: String, body: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(RelCardAlt)
            .border(1.dp, BorderSoft, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(title, color = RelDim, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
        Text(body, color = RelMuted, fontSize = 12.sp, fontFamily = PlusJakartaSans)
    }
}

@Composable
private fun LifeSignalsRow(
    onTrust: () -> Unit,
    onCare: () -> Unit,
    onMoney: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SignalChip("Trust +", RelPink, Modifier.weight(1f), onTrust)
        SignalChip("Care →", RelBlue, Modifier.weight(1f), onCare)
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(RelPurple.copy(alpha = 0.25f))
                .border(1.dp, RelPurple.copy(alpha = 0.5f), CircleShape)
                .clickable(onClick = onMoney),
            contentAlignment = Alignment.Center,
        ) {
            Text("₹", color = RelPurple, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SignalChip(label: String, tint: Color, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(tint.copy(alpha = 0.18f))
            .border(1.dp, tint.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = RelText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
    }
}

@Composable
private fun RecentActivityPreview(
    items: List<RelationshipsActivityItem>,
    onViewAll: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(RelCard)
            .border(1.dp, BorderSoft, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("RECENT ACTIVITY", color = RelDim, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
            Text(
                "View All",
                color = RelPink,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
                modifier = Modifier.clickable(onClick = onViewAll),
            )
        }
        Text(
            "Latest logs across your personal moments.",
            color = RelMuted,
            fontSize = 11.sp,
            fontFamily = PlusJakartaSans,
        )
        if (items.isEmpty()) {
            Text(
                "No relationship activity yet.",
                color = RelDim,
                fontSize = 12.sp,
                fontFamily = PlusJakartaSans,
            )
        } else {
            items.forEach { item ->
                RelationshipsActivityPreviewRow(item, onClick = onViewAll)
            }
        }
    }
}

@Composable
fun RelationshipsActivityPreviewRow(
    item: RelationshipsActivityItem,
    onClick: () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(RelPink),
            contentAlignment = Alignment.Center,
        ) {
            Text(item.emoji, fontSize = 14.sp)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(item.title, color = RelText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
            Text(item.whenLabel, color = RelDim, fontSize = 11.sp, fontFamily = PlusJakartaSans)
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(RelPink)
                .padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
            Text(
                item.impact,
                color = RelBg,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = PlusJakartaSans,
            )
        }
    }
}

@Composable
private fun SharedSpendAndAccounts(
    spendPairs: List<Pair<String, String>>,
    onAddExpense: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(RelCard)
            .border(1.dp, BorderSoft, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("SHARED SPEND", color = RelDim, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
        if (spendPairs.isEmpty()) {
            Text(
                "Not projected yet — log shared expenses to surface spend.",
                color = RelMuted,
                fontSize = 12.sp,
                fontFamily = PlusJakartaSans,
            )
        } else {
            spendPairs.forEach { (currency, amount) ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(currency, color = RelMuted, fontSize = 12.sp, fontFamily = PlusJakartaSans)
                    Text(amount, color = RelText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("ACCOUNTS", color = RelDim, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
            Text(
                "+ Add Expense",
                color = RelPink,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = PlusJakartaSans,
                modifier = Modifier.clickable(onClick = onAddExpense),
            )
        }
        Text(
            "Not projected yet — account bonds appear when shared finance is linked.",
            color = RelMuted,
            fontSize = 12.sp,
            fontFamily = PlusJakartaSans,
        )
    }
}

@Composable
private fun ProtectConnectionCard(onLogConnection: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                drawRoundRect(
                    brush = Brush.radialGradient(
                        colors = listOf(RelPink.copy(alpha = 0.22f), Color.Transparent),
                        center = center,
                        radius = size.maxDimension * 0.6f,
                    ),
                )
            }
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF2A1524))
            .border(1.5.dp, RelPink.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "Protect Connection",
            color = RelText,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = PlusJakartaSans,
        )
        Text(
            "Log a connection before your next high-pressure stretch.",
            color = RelMuted,
            fontSize = 13.sp,
            fontFamily = PlusJakartaSans,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(999.dp))
                .background(RelPink)
                .clickable(onClick = onLogConnection)
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "Log Connection",
                color = RelBg,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
            )
        }
    }
}

@Composable
private fun AiInsightsComingSoon() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(RelCardAlt)
            .border(1.dp, BorderSoft, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("AI Insights", color = RelText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
        Text("Coming Soon", color = RelPink, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
        Text(
            "Momentra will soon analyze recent activity to uncover emerging relationship patterns.",
            color = RelMuted,
            fontSize = 12.sp,
            fontFamily = PlusJakartaSans,
        )
    }
}

@Composable
private fun QuickAddLauncher(onRelationshipsQuickAdd: (RelationshipsQuickAddKind) -> Unit) {
    val actions = listOf(
        "💬" to RelationshipsQuickAddKind.CONNECTION,
        "🫶" to RelationshipsQuickAddKind.SHARED,
        "📅" to RelationshipsQuickAddKind.INVESTMENT,
        "⚡" to RelationshipsQuickAddKind.SUPPORT,
        "🙂" to RelationshipsQuickAddKind.ADJUST,
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
                    .background(RelCard)
                    .border(1.dp, RelPink.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                    .clickable { onRelationshipsQuickAdd(kind) },
                contentAlignment = Alignment.Center,
            ) {
                Text(emoji, fontSize = 20.sp)
            }
        }
    }
}

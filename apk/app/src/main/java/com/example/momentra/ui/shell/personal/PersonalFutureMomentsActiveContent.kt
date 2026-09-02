package com.example.momentra.ui.shell.personal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.api.ActivityItemDto
import com.example.momentra.data.api.PersonalPulseDto
import com.example.momentra.data.repository.PersonalSliceRepository
import com.example.momentra.ui.theme.PlusJakartaSans

private val Bg = Color(0xFF14121B)
private val CardBg = Color(0xFF191622)
private val TextMain = Color(0xFFE5E0EE)
private val Muted = Color(0xFFC9C4D8)
private val Purple = Color(0xFF7C5CFC)
private val PurpleSoft = Color(0xFFA78BFA)
private val PurpleMid = Color(0xFF8B5CF6)
private val Green = Color(0xFF10B981)
private val Blue = Color(0xFF3B82F6)
private val Cyan = Color(0xFF06B6D4)
private val Violet = Color(0xFFA855F7)
private val Amber = Color(0xFFF59E0B)
private val Red = Color(0xFFF87171)
private val BorderSoft = Color.White.copy(alpha = 0.08f)

/** Future Building Moments populated body — Figma 505:13146. */
@Composable
fun PersonalFutureMomentsActiveContent(
    refreshToken: Long,
    momentId: String?,
    momentTitle: String? = null,
    onOpenQuickAdd: () -> Unit,
    onAddExpense: () -> Unit,
    repository: PersonalSliceRepository = remember { PersonalSliceRepository() },
    modifier: Modifier = Modifier,
) {
    var loading by remember { mutableStateOf(true) }
    var pulse by remember { mutableStateOf<PersonalPulseDto?>(null) }
    var activities by remember { mutableStateOf<List<ActivityItemDto>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(refreshToken, momentId) {
        error = null
        PersonalTabDataCache.peek(momentId)?.let { cached ->
            pulse = cached.pulse
            activities = cached.activities
            loading = false
        } ?: run { loading = true }
        loadPersonalPulseTab(repository, momentId).fold(
            onSuccess = { (p, items) ->
                pulse = p
                activities = items
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
            CircularProgressIndicator(color = Purple)
        }
        return
    }

    val spend = futureSpendPairs(pulse)
    val futureScore = PersonalLifeOpsDerived.displayScore(pulse?.wellbeingScore)
    val stage = PersonalLifeOpsDerived.stageBand(pulse?.wellbeingScore)
    val milestoneCount = activities.count { it.activityCode.contains("MILESTONE", ignoreCase = true) }
    val learningCount = activities.count { it.activityCode.contains("LEARNING", ignoreCase = true) }
    val progressCount = activities.count { it.activityCode.contains("PROGRESS", ignoreCase = true) }
    val spendCompact = futureCompactSpend(spend)
    val milestoneStreak = PersonalLifeOpsDerived.streakDays(
        activities.filter { it.activityCode.contains("MILESTONE", ignoreCase = true) }.map { it.occurredAt },
    )
    val learningStreak = PersonalLifeOpsDerived.streakDays(
        activities.filter { it.activityCode.contains("LEARNING", ignoreCase = true) }.map { it.occurredAt },
    )
    val hasExpense = activities.any { it.activityCode.contains("EXPENSE", ignoreCase = true) } || spend.isNotEmpty()
    val bandBadge = when {
        futureScore == "—" -> "Building"
        stage == "Thriving" -> "Thriving"
        stage == "Structured" -> "Stable and Improving"
        else -> "Stabilizing"
    }
    val insight = when {
        futureScore == "—" -> "Log milestones, learning, and progress to reveal your Future Score."
        stage == "Thriving" -> "Learning and execution are compounding together."
        stage == "Structured" -> "Your future is compounding through learning and execution."
        else -> "Your future is compounding…"
    }

    Box(modifier = modifier.fillMaxSize().background(Bg)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .padding(bottom = 56.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            error?.let {
                Text(it, color = Red, fontSize = 12.sp, fontFamily = PlusJakartaSans)
            }

            // Hero — Future Journey
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(CardBg)
                    .border(1.dp, BorderSoft, RoundedCornerShape(20.dp))
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "Future Journey",
                            color = TextMain,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = PlusJakartaSans,
                        )
                        Text(insight, color = Muted, fontSize = 13.sp, fontFamily = PlusJakartaSans)
                    }
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .border(
                                6.dp,
                                if (futureScore == "—") Color.White.copy(alpha = 0.15f) else Green,
                                CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                futureScore,
                                color = TextMain,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = PlusJakartaSans,
                            )
                            Text("SCORE", color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50.dp))
                        .background(Brush.horizontalGradient(listOf(Green, Color(0xFF34D399))))
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                ) {
                    Text(bandBadge, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
                }
                Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Stabilizing", "Structured", "Thriving").forEach { band ->
                        val filled = futureScore != "—" && futureBandRank(band) <= futureBandRank(stage)
                        val active = band == stage && futureScore != "—"
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(99.dp))
                                    .background(if (filled) Purple else Color(0xFF35333E)),
                            )
                            Text(
                                band,
                                color = Muted,
                                fontSize = 10.sp,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                fontFamily = PlusJakartaSans,
                            )
                        }
                    }
                }
                Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FutureMiniStat("🏆", if (milestoneCount > 0) "$milestoneCount" else "—", "Milestones", Green, Modifier.weight(1f))
                    FutureMiniStat("📚", if (learningCount > 0) "$learningCount" else "—", "Learning", Blue, Modifier.weight(1f))
                    FutureMiniStat("💰", spendCompact, "Invested", Cyan, Modifier.weight(1f))
                    FutureMiniStat("📈", if (progressCount > 0) "$progressCount" else "—", "Progress", Violet, Modifier.weight(1f))
                }
            }

            Text("Journey Timeline", color = TextMain, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
            if (activities.isEmpty()) {
                FutureEmptyCard("No journey entries yet. Capture a milestone, learning, or progress to start the timeline.")
            } else {
                activities.take(8).forEach { item ->
                    FutureMomentsJourneyItem(item)
                }
            }

            Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Capital Journey", color = TextMain, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
                Text("From pulse", color = Muted, fontSize = 11.sp, fontFamily = PlusJakartaSans)
            }
            if (spend.isEmpty()) {
                FutureEmptyCard("No capital invested for this moment yet.")
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .border(1.dp, BorderSoft, RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Total Invested", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
                    Text(
                        spend.joinToString(" · ") { "${futureCurrencySymbol(it.first)}${futureFormatMoney(it.second)}" },
                        color = TextMain,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = PlusJakartaSans,
                    )
                    spend.forEach { (currency, amount) ->
                        Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(currency, color = TextMain, fontSize = 13.sp, fontFamily = PlusJakartaSans)
                            Text(
                                futureFormatMoney(amount),
                                color = TextMain,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = PlusJakartaSans,
                            )
                        }
                    }
                }
            }

            Text("Best Breakthroughs", color = TextMain, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
            Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FutureBestCard(
                    emoji = "⚡",
                    title = if (milestoneStreak > 0) "Milestone streak" else "Milestones",
                    detail = if (milestoneStreak > 0) "+$milestoneStreak streak" else "Building…",
                    accent = Green,
                    modifier = Modifier.weight(1f),
                )
                FutureBestCard(
                    emoji = "⚖️",
                    title = when {
                        learningStreak > 0 -> "Learning streak"
                        hasExpense -> "Capital logged"
                        else -> "Balance"
                    },
                    detail = when {
                        learningStreak > 0 -> "+$learningStreak streak"
                        hasExpense -> "Expense presence on this moment"
                        else -> "Building…"
                    },
                    accent = Blue,
                    modifier = Modifier.weight(1f),
                )
            }
            Text("Turning Points", color = TextMain, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
            FutureTurningRow(
                emoji = "🔥",
                title = "First milestone streak",
                body = if (milestoneStreak > 0) {
                    "$milestoneStreak consecutive milestone day${if (milestoneStreak == 1) "" else "s"}"
                } else {
                    "Log milestones on consecutive days to unlock this turning point."
                },
            )
            FutureTurningRow(
                emoji = "🔒",
                title = "Capital signal",
                body = if (hasExpense) {
                    "Spend is flowing into this moment’s capital journey."
                } else {
                    "Log an expense to mark your first capital turning point."
                },
            )

            FutureInsightsComingSoon("Patterns across milestones, learning, progress, and capital will surface here.")

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.linearGradient(listOf(Purple, PurpleMid)))
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("Capture a new moment", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, fontFamily = PlusJakartaSans)
                Text(
                    "Add a milestone, learning, progress, or capital log",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 13.sp,
                    fontFamily = PlusJakartaSans,
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.5.dp, Color.White.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
                        .clickable(onClick = onOpenQuickAdd)
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("+ Open Quick Add", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, fontFamily = PlusJakartaSans)
                }
            }
        }
    }
}

@Composable
private fun FutureMiniStat(emoji: String, value: String, label: String, tint: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(tint)
            .border(1.dp, tint, RoundedCornerShape(16.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(emoji, fontSize = 16.sp)
        }
        Text(
            value,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = PlusJakartaSans,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(label, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
    }
}

@Composable
private fun FutureMomentsJourneyItem(item: ActivityItemDto) {
    val meta = futureActivityVisual(item.activityCode)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(meta.color.copy(alpha = 0.1f))
            .border(1.dp, meta.color.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(meta.color),
            contentAlignment = Alignment.Center,
        ) {
            Text(meta.emoji, fontSize = 18.sp)
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(meta.label, color = TextMain, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(meta.color.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    Text(
                        PersonalLifeOpsDerived.relativeTime(item.occurredAt),
                        color = meta.color,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = PlusJakartaSans,
                    )
                }
            }
            Text(item.title, color = Muted, fontSize = 12.sp, fontFamily = PlusJakartaSans)
        }
    }
}

@Composable
private fun FutureBestCard(emoji: String, title: String, detail: String, accent: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(accent.copy(alpha = 0.1f))
            .border(1.dp, accent.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(accent),
            contentAlignment = Alignment.Center,
        ) {
            Text(emoji, fontSize = 16.sp)
        }
        Text(title, color = TextMain, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
        Text(detail, color = accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
    }
}

@Composable
private fun FutureTurningRow(emoji: String, title: String, body: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, BorderSoft, RoundedCornerShape(16.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(emoji, fontSize = 18.sp)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = TextMain, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
            Text(body, color = Muted, fontSize = 12.sp, fontFamily = PlusJakartaSans)
        }
    }
}

@Composable
private fun FutureEmptyCard(message: String) {
    Text(
        message,
        color = Muted,
        fontSize = 13.sp,
        fontFamily = PlusJakartaSans,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, BorderSoft, RoundedCornerShape(16.dp))
            .padding(16.dp),
    )
}

@Composable
private fun FutureInsightsComingSoon(body: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, BorderSoft, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("AI Insights", color = TextMain, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(100.dp))
                    .background(Purple.copy(alpha = 0.2f))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            ) {
                Text("Coming Soon", color = PurpleSoft, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, fontFamily = PlusJakartaSans)
            }
        }
        Text(body, color = Muted, fontSize = 12.sp, fontFamily = PlusJakartaSans)
    }
}

private data class FutureActivityVisual(val label: String, val emoji: String, val color: Color)

private fun futureActivityVisual(code: String): FutureActivityVisual {
    val upper = code.uppercase()
    return when {
        upper.contains("MILESTONE") -> FutureActivityVisual("Milestone", "🏆", Green)
        upper.contains("LEARNING") -> FutureActivityVisual("Learning", "📚", Blue)
        upper.contains("PROGRESS") -> FutureActivityVisual("Progress", "📈", Violet)
        upper.contains("OPPORTUNITY") -> FutureActivityVisual("Opportunity", "✨", Cyan)
        upper.contains("PIVOT") -> FutureActivityVisual("Pivot", "🔄", Amber)
        upper.contains("EXPENSE") || upper.contains("MONEY") -> FutureActivityVisual("Capital", "💰", Amber)
        else -> FutureActivityVisual("Activity", "◎", Purple)
    }
}

private fun futureSpendPairs(pulse: PersonalPulseDto?): List<Pair<String, String>> =
    (pulse?.widgetPayload?.get("spendByCurrency") as? Map<*, *>)
        ?.entries
        ?.mapNotNull { e ->
            val code = e.key?.toString() ?: return@mapNotNull null
            val amount = e.value?.toString() ?: return@mapNotNull null
            code to amount
        }
        .orEmpty()

private fun futureCompactSpend(spend: List<Pair<String, String>>): String {
    val first = spend.firstOrNull() ?: return "—"
    val n = first.second.toDoubleOrNull() ?: return "—"
    val symbol = futureCurrencySymbol(first.first)
    return if (n >= 1000) {
        val k = n / 1000.0
        val formatted = if (k >= 10) String.format("%.0fK", k) else String.format("%.1fK", k)
        "$symbol$formatted"
    } else {
        "$symbol${futureFormatMoney(first.second)}"
    }
}

private fun futureCurrencySymbol(code: String): String = when (code.uppercase()) {
    "INR" -> "₹"
    "USD" -> "$"
    "EUR" -> "€"
    "GBP" -> "£"
    else -> "$code "
}

private fun futureFormatMoney(amount: String): String {
    val n = amount.toDoubleOrNull() ?: return amount
    return String.format("%,.2f", n)
}

private fun futureBandRank(band: String): Int = when (band) {
    "Thriving" -> 3
    "Structured" -> 2
    else -> 1
}

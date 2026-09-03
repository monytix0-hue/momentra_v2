package com.example.momentra.ui.shell.personal.lifeops.moments

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
import kotlin.math.roundToInt
import com.example.momentra.ui.shell.personal.lifeops.create.PersonalLifeOpsDerived
import com.example.momentra.ui.shell.personal.shared.loadPersonalPulseTab
import com.example.momentra.ui.shell.personal.shared.PersonalTabDataCache

private val Bg = Color(0xFF14121B)
private val TextMain = Color(0xFFE5E0EE)
private val Muted = Color(0xFFC9C4D8)
private val Purple = Color(0xFF7C5CFC)
private val PurpleSoft = Color(0xFFA78BFA)
private val PurpleMid = Color(0xFF8B5CF6)
private val Green = Color(0xFF10B981)
private val Cyan = Color(0xFF4CD6FF)
private val Red = Color(0xFFF87171)
private val Orange = Color(0xFFFF9800)
private val BorderSoft = Color.White.copy(alpha = 0.08f)

/** Life Ops Moments populated body — Figma 353:9649. */
@Composable
fun PersonalLifeOpsMomentsActiveContent(
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

    val spend = spendPairs(pulse)
    val wellbeing = PersonalLifeOpsDerived.displayScore(pulse?.wellbeingScore)
    val recovery = PersonalLifeOpsDerived.displayScore(pulse?.recoveryScore)
    val stage = PersonalLifeOpsDerived.stageBand(pulse?.wellbeingScore)
    val daysActive = activities.mapNotNull {
        PersonalLifeOpsDerived.parseInstant(it.occurredAt)
            ?.atZone(java.time.ZoneId.systemDefault())?.toLocalDate()?.toString()
    }.toSet().size
    val spendCompact = compactSpend(spend)
    val recoveryStreak = PersonalLifeOpsDerived.streakDays(
        activities.filter { it.activityCode.contains("RECOVERY", ignoreCase = true) }.map { it.occurredAt },
    )
    val hasExpense = activities.any { it.activityCode.contains("EXPENSE", ignoreCase = true) } || spend.isNotEmpty()
    val bandBadge = when {
        wellbeing == "—" -> "Building"
        stage == "Thriving" -> "Thriving"
        stage == "Structured" -> "Stable and Improving"
        else -> "Stabilizing"
    }
    val insight = when {
        wellbeing == "—" -> "Log recovery, mood, and spend to reveal your wellbeing score."
        stage == "Thriving" -> "Structure and recovery are working together."
        stage == "Structured" -> "Your operating rhythm is becoming more intentional."
        else -> "Early signals form as recovery and attention logs accumulate."
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
            if (!momentTitle.isNullOrBlank()) {
                Text(
                    momentTitle,
                    color = Muted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = PlusJakartaSans,
                )
            }

            // Hero
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Brush.horizontalGradient(listOf(Purple, PurpleSoft)))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        wellbeing,
                        color = Color.White,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = PlusJakartaSans,
                    )
                    Box(
                        modifier = Modifier
                            .padding(bottom = 10.dp)
                            .clip(RoundedCornerShape(50.dp))
                            .background(Color.White.copy(alpha = 0.1f))
                            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(50.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                    ) {
                        Text(bandBadge, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
                    }
                }
                Text(insight, color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp, fontFamily = PlusJakartaSans)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Stabilizing", "Structured", "Thriving").forEach { band ->
                        val filled = wellbeing != "—" && bandRank(band) <= bandRank(stage)
                        val active = band == stage && wellbeing != "—"
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
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 10.sp,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                fontFamily = PlusJakartaSans,
                            )
                        }
                    }
                }
                Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MiniStat("📅", if (daysActive > 0) "$daysActive" else "—", "Days Active", Modifier.weight(1f))
                    MiniStat("📝", if (activities.isEmpty()) "—" else "${activities.size}", "Logs", Modifier.weight(1f))
                    MiniStat("💰", spendCompact, "Spend", Modifier.weight(1f))
                    MiniStat("🔋", recovery, "Recovery", Modifier.weight(1f))
                }
            }

            // Journey Timeline
            Text("Journey Timeline", color = TextMain, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
            if (activities.isEmpty()) {
                EmptyCard("No journey entries yet. Capture a recovery, mood, or expense to start the timeline.")
            } else {
                activities.take(8).forEach { item ->
                    MomentsActivityJourneyItem(item)
                }
            }

            // Money Journey
            Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Money Journey", color = TextMain, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
                Text("From pulse", color = Muted, fontSize = 11.sp, fontFamily = PlusJakartaSans)
            }
            if (spend.isEmpty()) {
                EmptyCard("No spend recorded for this moment yet.")
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
                    Text("Total Spend", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
                    Text(
                        spend.joinToString(" · ") { "${currencySymbol(it.first)}${formatMoney(it.second)}" },
                        color = TextMain,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = PlusJakartaSans,
                    )
                    spend.forEach { (currency, amount) ->
                        Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(currency, color = TextMain, fontSize = 13.sp, fontFamily = PlusJakartaSans)
                            Text(formatMoney(amount), color = TextMain, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
                        }
                    }
                }
            }

            // Best Moments / Turning Points
            Text("Best Moments", color = TextMain, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
            Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BestCard(
                    emoji = "🏆",
                    title = if (recoveryStreak > 0) "Recovery streak" else "Recovery",
                    detail = if (recoveryStreak > 0) "$recoveryStreak-day streak" else "Building…",
                    accent = Purple,
                    modifier = Modifier.weight(1f),
                )
                BestCard(
                    emoji = "💰",
                    title = if (hasExpense) "Spend logged" else "Balance",
                    detail = if (hasExpense) "Expense presence on this moment" else "Building…",
                    accent = Green,
                    modifier = Modifier.weight(1f),
                )
            }
            Text("Turning Points", color = TextMain, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
            TurningRow(
                emoji = "🔥",
                title = "First recovery streak",
                body = if (recoveryStreak > 0) {
                    "$recoveryStreak consecutive recovery day${if (recoveryStreak == 1) "" else "s"}"
                } else {
                    "Log recovery on consecutive days to unlock this turning point."
                },
            )
            TurningRow(
                emoji = "🔒",
                title = "Budget signal",
                body = if (hasExpense) {
                    "Spend is flowing into this moment’s money journey."
                } else {
                    "Log an expense to mark your first money turning point."
                },
            )

            InsightsComingSoon("Patterns across activities, finances, and recovery will surface here.")

            // Capture CTA
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
                    "Add a log, expense, mood check-in or update",
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
private fun MiniStat(emoji: String, value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(16.dp))
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, fontSize = 14.sp)
            Text(
                value,
                color = TextMain,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = PlusJakartaSans,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(label, color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
    }
}

@Composable
private fun MomentsActivityJourneyItem(item: ActivityItemDto) {
    val meta = activityVisual(item.activityCode)
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
private fun BestCard(emoji: String, title: String, detail: String, accent: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, BorderSoft, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(emoji, fontSize = 18.sp)
        Text(title, color = TextMain, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
        Text(detail, color = accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
    }
}

@Composable
private fun TurningRow(emoji: String, title: String, body: String) {
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
private fun EmptyCard(message: String) {
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
private fun InsightsComingSoon(body: String) {
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

private data class ActivityVisual(val label: String, val emoji: String, val color: Color)

private fun activityVisual(code: String): ActivityVisual {
    val upper = code.uppercase()
    return when {
        upper.contains("RECOVERY") -> ActivityVisual("Recovery", "🔋", Cyan)
        upper.contains("EXPENSE") || upper.contains("MONEY") -> ActivityVisual("Money", "💰", Red)
        upper.contains("MOOD") -> ActivityVisual("Mood", "😌", PurpleSoft)
        upper.contains("ATTENTION") -> ActivityVisual("Attention", "🎯", Orange)
        upper.contains("RHYTHM") || upper.contains("WELLBEING") -> ActivityVisual("Rhythm", "⚡", Green)
        else -> ActivityVisual("Activity", "◎", Purple)
    }
}

private fun spendPairs(pulse: PersonalPulseDto?): List<Pair<String, String>> =
    (pulse?.widgetPayload?.get("spendByCurrency") as? Map<*, *>)
        ?.entries
        ?.mapNotNull { e ->
            val code = e.key?.toString() ?: return@mapNotNull null
            val amount = e.value?.toString() ?: return@mapNotNull null
            code to amount
        }
        .orEmpty()

private fun compactSpend(spend: List<Pair<String, String>>): String {
    val first = spend.firstOrNull() ?: return "—"
    val n = first.second.toDoubleOrNull() ?: return "—"
    val symbol = currencySymbol(first.first)
    return if (n >= 1000) {
        val k = n / 1000.0
        val formatted = if (k >= 10) String.format("%.0fK", k) else String.format("%.1fK", k)
        "$symbol$formatted"
    } else {
        "$symbol${formatMoney(first.second)}"
    }
}

private fun currencySymbol(code: String): String = when (code.uppercase()) {
    "INR" -> "₹"
    "USD" -> "$"
    "EUR" -> "€"
    "GBP" -> "£"
    else -> "$code "
}

private fun formatMoney(amount: String): String {
    val n = amount.toDoubleOrNull() ?: return amount
    return String.format("%,.2f", n)
}

private fun bandRank(band: String): Int = when (band) {
    "Thriving" -> 3
    "Structured" -> 2
    else -> 1
}

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
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

private val Bg = Color(0xFF14121B)
private val CardBg = Color(0xFF152022)
private val TextMain = Color(0xFFE5E0EE)
private val Muted = Color(0xFFC9C4D8)
private val Teal = Color(0xFF0EA5A4)
private val TealSoft = Color(0xFF5EEAD4)
private val Red = Color(0xFFF87171)
private val BorderSoft = Color.White.copy(alpha = 0.08f)

/** Lifestyle Moments — Figma `505:12574`; pulse + activity fetch. */
@Composable
fun PersonalLifestyleMomentsActiveContent(
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
            CircularProgressIndicator(color = Teal)
        }
        return
    }

    val theme = PersonalPulseFamily.LIFESTYLE.theme()
    val vitality = PersonalLifestyleDerived.vitalityIndexDisplay(pulse)
    val axes = PersonalLifestyleDerived.axisScores(pulse)
    val spend = PersonalLifestyleDerived.spendPairs(pulse)
    val experienceCount = PersonalLifestyleDerived.experienceCount(pulse)
    val stability = PersonalLifestyleDerived.networkStability(pulse)
    val insight = when {
        vitality == "—" -> "Log experiences and wellbeing to reveal your Vitality Index."
        stability == "Flourishing" -> "Joy and exploration are compounding together."
        stability == "Growing" -> "Your lifestyle rhythm is becoming more intentional."
        else -> "Early signals form as experiences and rituals accumulate."
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
            error?.let { Text(it, color = Red, fontSize = 12.sp, fontFamily = PlusJakartaSans) }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Brush.horizontalGradient(listOf(Teal, TealSoft)))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(theme.heroTitle, color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
                Text(vitality, color = Color.White, fontSize = 48.sp, fontWeight = FontWeight.ExtraBold, fontFamily = PlusJakartaSans)
                Text(insight, color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp, fontFamily = PlusJakartaSans)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(axes.joy, axes.fulfillment, axes.vitality, axes.exploration).forEachIndexed { i, score ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White.copy(alpha = 0.12f))
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(theme.heroMetrics[i], color = Color.White.copy(alpha = 0.7f), fontSize = 9.sp, fontFamily = PlusJakartaSans)
                                Text(score, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LifestyleMiniStat("✨", if (experienceCount > 0) "$experienceCount" else "—", "Experiences", Modifier.weight(1f))
                    LifestyleMiniStat("📝", if (activities.isEmpty()) "—" else "${activities.size}", "Logs", Modifier.weight(1f))
                    LifestyleMiniStat("🌿", stability.take(8), "Network", Modifier.weight(1f))
                    LifestyleMiniStat("💰", if (spend.isEmpty()) "—" else spend.first().second, "Spend", Modifier.weight(1f))
                }
            }

            Text("Experience Journey", color = TextMain, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
            if (activities.isEmpty()) {
                LifestyleEmptyCard("No journey entries yet. Log an experience or wellbeing check to start.")
            } else {
                activities.take(8).forEach { LifestyleJourneyItem(it) }
            }

            FamilySpendSection(title = theme.moneyTitle, spend = spend, accent = Teal, onAddExpense = onAddExpense)

            LifestyleEmptyCard(
                if (vitality == "—") {
                    "Patterns across joy, fulfillment, and exploration will surface as you log."
                } else {
                    "Insights: $stability — keep logging rituals to deepen vitality memory."
                },
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Teal)
                    .clickable(onClick = onOpenQuickAdd)
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(theme.nudgeCta, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
            }
        }
    }
}

@Composable
private fun LifestyleMiniStat(emoji: String, value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(emoji, fontSize = 12.sp)
            Text(value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(label, color = Color.White.copy(alpha = 0.7f), fontSize = 9.sp, fontFamily = PlusJakartaSans)
    }
}

@Composable
private fun LifestyleJourneyItem(item: ActivityItemDto) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Teal.copy(alpha = 0.1f))
            .border(1.dp, Teal.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(18.dp)).background(Teal),
            contentAlignment = Alignment.Center,
        ) { Text("✨", fontSize = 16.sp) }
        Column(Modifier.weight(1f)) {
            Text(item.title, color = TextMain, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(item.occurredAt.take(16), color = Muted, fontSize = 11.sp, fontFamily = PlusJakartaSans)
        }
    }
}

@Composable
private fun LifestyleEmptyCard(body: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .border(1.dp, BorderSoft, RoundedCornerShape(16.dp))
            .padding(14.dp),
    ) {
        Text(body, color = Muted, fontSize = 12.sp, fontFamily = PlusJakartaSans)
    }
}

@Composable
internal fun FamilyMomentsSection(
    title: String,
    emptyBody: String,
    activities: List<ActivityItemDto>,
    accent: Color,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .border(1.dp, BorderSoft, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(title, color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
        if (activities.isEmpty()) {
            Text(emptyBody, color = Muted, fontSize = 12.sp, fontFamily = PlusJakartaSans)
        } else {
            activities.take(8).forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.title, color = TextMain, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(item.occurredAt.take(16), color = Muted, fontSize = 11.sp, fontFamily = PlusJakartaSans)
                    }
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(accent.copy(alpha = 0.2f)).padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text(item.activityCode.take(12), color = accent, fontSize = 10.sp, fontFamily = PlusJakartaSans)
                    }
                }
            }
        }
    }
}

@Composable
internal fun FamilySpendSection(
    title: String,
    spend: List<Pair<String, String>>,
    accent: Color,
    onAddExpense: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .border(1.dp, BorderSoft, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
            Text("+ Expense", color = accent, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans, modifier = Modifier.clickable(onClick = onAddExpense))
        }
        if (spend.isEmpty()) {
            Text("Not projected yet — log spend to surface a snapshot.", color = Muted, fontSize = 12.sp, fontFamily = PlusJakartaSans)
        } else {
            spend.forEach { (currency, amount) ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(currency, color = Muted, fontSize = 12.sp, fontFamily = PlusJakartaSans)
                    Text(amount, color = TextMain, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
                }
            }
        }
    }
}

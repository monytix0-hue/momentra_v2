package com.example.momentra.ui.shell.personal.future.memory

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
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.api.ActivityItemDto
import com.example.momentra.data.api.PersonalPulseDto
import com.example.momentra.data.repository.PersonalSliceRepository
import com.example.momentra.ui.theme.PlusJakartaSans
import kotlin.math.min
import com.example.momentra.ui.shell.personal.lifeops.create.PersonalLifeOpsDerived
import com.example.momentra.ui.shell.personal.shared.loadPersonalPulseTab
import com.example.momentra.ui.shell.personal.shared.PersonalTabDataCache

private val Bg = Color(0xFF14121B)
private val CardBg = Color(0xFF191622)
private val TextMain = Color(0xFFE5E0EE)
private val Muted = Color(0xFFC9C4D8)
private val Purple = Color(0xFF7C5CFC)
private val PurpleSoft = Color(0xFFA78BFA)
private val Green = Color(0xFF10B981)
private val Blue = Color(0xFF3B82F6)
private val Cyan = Color(0xFF06B6D4)
private val Orange = Color(0xFFFF9800)
private val Red = Color(0xFFF87171)
private val Pink = Color(0xFFE91E63)
private val Leaf = Color(0xFF4CAF50)
private val BorderSoft = Color.White.copy(alpha = 0.08f)

/** Future Building Memory populated body — Figma 505:13237. */
@Composable
fun PersonalFutureMemoryActiveContent(
    refreshToken: Long,
    momentId: String?,
    onProtectMilestone: () -> Unit,
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

    val stage = PersonalLifeOpsDerived.stageBand(pulse?.wellbeingScore)
    val thinData = activities.size < 2 && PersonalLifeOpsDerived.scoreNumber(pulse?.wellbeingScore) == null
    val confidenceLabel = if (thinData) {
        "Building…"
    } else {
        "${min(92, 55 + activities.size * 3)}% confidence"
    }
    val identityBody = if (thinData) {
        "Log milestones, learning, and progress to reveal your builder identity."
    } else {
        "You respond best when learning and execution work together."
    }
    val (helping, hurting) = futureHelpingHurting(activities.map { it.activityCode to it.title })
    val hasMilestone = activities.any { it.activityCode.contains("MILESTONE", ignoreCase = true) }
    val hasLearning = activities.any { it.activityCode.contains("LEARNING", ignoreCase = true) }
    val hasProgress = activities.any { it.activityCode.contains("PROGRESS", ignoreCase = true) }
    val hasExpense = activities.any { it.activityCode.contains("EXPENSE", ignoreCase = true) }
    val patternConfidence = if (thinData) {
        "Building…"
    } else {
        "${min(92, 40 + activities.size * 5)}% pattern confidence"
    }
    val aiBody = when {
        thinData -> "Building… Log milestones, learning, and progress to unlock an interpretation."
        hasLearning && hasProgress ->
            "Your future compounds when learning is paired with execution, not stored for later."
        hasMilestone ->
            "Milestone reviews keep momentum from drifting — protect them before the week fills up."
        hasExpense ->
            "Capital signals are present — pair them with learning blocks so spend fuels growth."
        else -> "Keep logging learning and execution together to reveal how your future compounds."
    }
    val evolutionCurrent = when (stage) {
        "Thriving" -> "Adaptive"
        "Structured" -> "Structured"
        else -> "Stabilizing"
    }
    val behaviors = buildList {
        if (hasMilestone) add("Weekly milestone review" to "High")
        if (hasLearning) add("Same-day learning log" to "High")
        if (hasProgress) add("Progress check-ins" to "Medium")
        if (hasExpense) add("Capital logged same day" to "Medium")
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Bg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        error?.let {
            Text(it, color = Red, fontSize = 12.sp, fontFamily = PlusJakartaSans)
        }

        // 1 Identity Snapshot
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(CardBg)
                .border(1.dp, BorderSoft, RoundedCornerShape(20.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "1 IDENTITY SNAPSHOT",
                color = Muted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                fontFamily = PlusJakartaSans,
            )
            Text(
                "Builder in Motion",
                color = TextMain,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = PlusJakartaSans,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(Green.copy(alpha = 0.12f))
                        .border(1.dp, Green, RoundedCornerShape(999.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text(
                        confidenceLabel,
                        color = Green,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = PlusJakartaSans,
                    )
                }
                Text(
                    identityBody,
                    color = Muted,
                    fontSize = 14.sp,
                    fontFamily = PlusJakartaSans,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // 2 Core Pattern
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CardBg)
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "2 CORE PATTERN",
                color = Muted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                fontFamily = PlusJakartaSans,
            )
            if (thinData) {
                Text(
                    "Building… Need a few more logs to show Learning → Execution → Momentum.",
                    color = Muted,
                    fontSize = 13.sp,
                    fontFamily = PlusJakartaSans,
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FuturePatternStep("📚", "Learning", Green, Modifier.weight(1f))
                    Text("›", color = Muted, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    FuturePatternStep("💪", "Execution", Blue, Modifier.weight(1f))
                    Text("›", color = Muted, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    FuturePatternStep("⚖️", "Momentum", Cyan, Modifier.weight(1f))
                }
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Green.copy(alpha = 0.12f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text(patternConfidence, color = Green, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, fontFamily = PlusJakartaSans)
            }
        }

        // 3 / 4 Drivers
        Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FutureDriverColumn(
                title = "3 BEST DRIVERS",
                titleColor = Leaf,
                items = helping,
                modifier = Modifier.weight(1f),
            )
            FutureDriverColumn(
                title = "4 LOWEST DRIVERS",
                titleColor = Pink,
                items = hurting,
                modifier = Modifier.weight(1f),
            )
        }

        // 5 Return Behaviors
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .border(1.dp, BorderSoft, RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "5 RETURN BEHAVIORS",
                color = Muted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                fontFamily = PlusJakartaSans,
            )
            if (behaviors.isEmpty()) {
                Text("Building…", color = Muted, fontSize = 13.sp, fontFamily = PlusJakartaSans)
            } else {
                behaviors.forEach { (label, badge) ->
                    val badgeColor = if (badge == "High") Green else Orange
                    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(label, color = TextMain, fontSize = 13.sp, fontWeight = FontWeight.Medium, fontFamily = PlusJakartaSans)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(100.dp))
                                .background(badgeColor.copy(alpha = 0.15f))
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Text(badge, color = badgeColor, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, fontFamily = PlusJakartaSans)
                        }
                    }
                }
            }
        }

        // 6 Emotional DNA — empty until mood signals exist for Future
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .border(1.dp, BorderSoft, RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "6 EMOTIONAL DNA",
                color = Muted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                fontFamily = PlusJakartaSans,
            )
            Text("Building…", color = Muted, fontSize = 13.sp, fontFamily = PlusJakartaSans)
        }

        // 8 Evolution Timeline
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .border(1.dp, BorderSoft, RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "8 EVOLUTION TIMELINE",
                color = Muted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                fontFamily = PlusJakartaSans,
            )
            listOf("Stabilizing", "Structured", "Adaptive").forEach { band ->
                Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        band,
                        color = if (band == evolutionCurrent) Purple else TextMain,
                        fontSize = 14.sp,
                        fontWeight = if (band == evolutionCurrent) FontWeight.ExtraBold else FontWeight.Medium,
                        fontFamily = PlusJakartaSans,
                    )
                    when {
                        band == evolutionCurrent -> Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(100.dp))
                                .background(Purple)
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Text("Now", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, fontFamily = PlusJakartaSans)
                        }
                        PersonalLifeOpsDerived.scoreNumber(pulse?.wellbeingScore) == null ->
                            Text("Building…", color = Muted, fontSize = 12.sp, fontFamily = PlusJakartaSans)
                    }
                }
            }
        }

        // 9 AI Interpretation
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .border(1.dp, BorderSoft, RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "9 AI INTERPRETATION ✦",
                color = Muted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                fontFamily = PlusJakartaSans,
            )
            Text(aiBody, color = TextMain, fontSize = 14.sp, fontFamily = PlusJakartaSans)
        }

        // 10 Next Growth Edge
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Brush.linearGradient(listOf(Purple, Blue)))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                "10 YOUR NEXT GROWTH EDGE",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                fontFamily = PlusJakartaSans,
            )
            Text(
                "Protect one weekly milestone review",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = PlusJakartaSans,
                textAlign = TextAlign.Center,
            )
            Text(
                "Schedule it before the commitment, not after the crash.",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 13.sp,
                fontFamily = PlusJakartaSans,
                textAlign = TextAlign.Center,
            )
            Text(
                "Protect Milestone Now",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = PlusJakartaSans,
                modifier = Modifier
                    .clickable(enabled = momentId != null, onClick = onProtectMilestone)
                    .padding(vertical = 8.dp),
            )
        }

        FutureMemoryInsightsComingSoon(
            "Recurring themes and growth patterns across your Future Building journey will appear here.",
        )
    }
}

@Composable
private fun FutureMemoryInsightsComingSoon(body: String) {
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

@Composable
private fun FuturePatternStep(emoji: String, label: String, tint: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(tint.copy(alpha = 0.1f))
            .border(1.dp, tint.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .padding(0.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(tint)
                .padding(8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(emoji, fontSize = 14.sp)
        }
        Text(
            label,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = PlusJakartaSans,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun FutureDriverColumn(
    title: String,
    titleColor: Color,
    items: List<String>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(titleColor.copy(alpha = 0.1f))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            title,
            color = titleColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            fontFamily = PlusJakartaSans,
        )
        if (items.isEmpty()) {
            Text("Building…", color = Muted, fontSize = 13.sp, fontFamily = PlusJakartaSans)
        } else {
            items.forEach { label ->
                Text(label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium, fontFamily = PlusJakartaSans)
            }
        }
    }
}

private fun futureHelpingHurting(activities: List<Pair<String, String>>): Pair<List<String>, List<String>> {
    val helping = mutableListOf<String>()
    val hurting = mutableListOf<String>()
    activities.take(8).forEach { (code, title) ->
        val upper = code.uppercase()
        when {
            upper.contains("MILESTONE") -> helping += "Milestone +"
            upper.contains("LEARNING") -> helping += "Learning · $title"
            upper.contains("PROGRESS") -> helping += "Progress +"
            upper.contains("OPPORTUNITY") -> helping += "Opportunity · $title"
            upper.contains("PIVOT") -> hurting += "Pivot · $title"
            upper.contains("EXPENSE") -> hurting += "Capital · $title"
        }
    }
    return helping.take(3) to hurting.take(3)
}

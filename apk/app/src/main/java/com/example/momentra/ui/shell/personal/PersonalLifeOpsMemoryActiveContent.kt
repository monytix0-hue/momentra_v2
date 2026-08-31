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
import androidx.compose.foundation.layout.width
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
import kotlin.math.roundToInt

private val Bg = Color(0xFF14121B)
private val TextMain = Color(0xFFE5E0EE)
private val Muted = Color(0xFFC9C4D8)
private val Purple = Color(0xFF7C5CFC)
private val PurpleSoft = Color(0xFFA78BFA)
private val PurpleMid = Color(0xFF8B5CF6)
private val Green = Color(0xFF10B981)
private val Orange = Color(0xFFFF9800)
private val Red = Color(0xFFF87171)
private val Pink = Color(0xFFE91E63)
private val Blue = Color(0xFF2196F3)
private val Leaf = Color(0xFF4CAF50)
private val BorderSoft = Color.White.copy(alpha = 0.08f)

/** Life Ops Memory populated body — Figma 353:10273. */
@Composable
fun PersonalLifeOpsMemoryActiveContent(
    refreshToken: Long,
    momentId: String?,
    onProtectRecovery: () -> Unit,
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

    val identity = PersonalLifeOpsDerived.identityLabel(
        wellbeing = pulse?.wellbeingScore,
        recovery = pulse?.recoveryScore,
        activityCount = activities.size,
    )
    val stage = PersonalLifeOpsDerived.stageBand(pulse?.wellbeingScore)
    val (helping, hurting) = PersonalLifeOpsDerived.helpingHurting(
        activities.map { it.activityCode to it.title },
    )
    val hasRecovery = activities.any { it.activityCode.contains("RECOVERY", ignoreCase = true) } ||
        PersonalLifeOpsDerived.scoreNumber(pulse?.recoveryScore) != null
    val hasExpense = activities.any { it.activityCode.contains("EXPENSE", ignoreCase = true) }
    val hasRhythm = activities.any {
        val c = it.activityCode.uppercase()
        c.contains("RHYTHM") || c.contains("ATTENTION") || c.contains("WELLBEING")
    }
    val thinData = activities.size < 2 &&
        PersonalLifeOpsDerived.scoreNumber(pulse?.wellbeingScore) == null &&
        PersonalLifeOpsDerived.scoreNumber(pulse?.recoveryScore) == null
    val moodBars = moodBars(pulse, activities)
    val moodTotal = moodBars.sumOf { it.count }
    val patternConfidence = if (thinData) {
        "Building…"
    } else {
        "${min(92, 40 + activities.size * 5)}% pattern confidence"
    }
    val aiBody = when {
        thinData -> "Building… Log recovery after pressure and a few moods to unlock an interpretation."
        hasRecovery -> "Your system rewards recovery placed immediately after pressure, not the next morning."
        hasExpense -> "Spend signals are present — pair them with recovery blocks to keep pressure from compounding."
        else -> "Keep logging pressure and recovery together to reveal how your system stabilizes."
    }
    val evolutionCurrent = when (stage) {
        "Thriving" -> "Adaptive"
        "Structured" -> "Structured"
        else -> "Stabilizing"
    }
    val behaviors = buildList {
        if (hasRecovery) add("Recovery before meetings" to "High")
        if (hasExpense) add("Same-day expense log" to "High")
        if (hasRhythm) add("Rhythm check-ins" to "Medium")
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
                .clip(RoundedCornerShape(24.dp))
                .background(Brush.horizontalGradient(listOf(Purple, PurpleSoft)))
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "1 IDENTITY SNAPSHOT",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                fontFamily = PlusJakartaSans,
            )
            Text(
                identity.first,
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = PlusJakartaSans,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.2f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text(
                        identity.second,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = PlusJakartaSans,
                    )
                }
                Text(
                    identity.third,
                    color = Color.White.copy(alpha = 0.8f),
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
                .background(Color(0xFF14121C))
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "2 CORE PATTERN",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                fontFamily = PlusJakartaSans,
            )
            if (thinData) {
                Text(
                    "Building… Need a few more logs to show Pressure → Recovery → Stability.",
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
                    PatternStep("🔥", "Pressure peak", Pink, Modifier.weight(1f))
                    Text("›", color = Muted, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    PatternStep("🌿", "Recovery block", Leaf, Modifier.weight(1f))
                    Text("›", color = Muted, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    PatternStep("✨", "Stability", Blue, Modifier.weight(1f))
                }
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Purple.copy(alpha = 0.15f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text(patternConfidence, color = Purple, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, fontFamily = PlusJakartaSans)
            }
        }

        // 3 / 4 Drivers
        Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DriverColumn(
                title = "3 BEST DRIVERS",
                titleColor = Leaf,
                items = helping.map { it.label },
                modifier = Modifier.weight(1f),
            )
            DriverColumn(
                title = "4 LOWEST DRIVERS",
                titleColor = Pink,
                items = hurting.map { it.label },
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

        // 6 Emotional DNA
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
            if (moodTotal == 0) {
                Text(
                    "No mood signals yet. Log a mood to map Calm / Focused / Strained / Lifted.",
                    color = Muted,
                    fontSize = 13.sp,
                    fontFamily = PlusJakartaSans,
                )
            } else {
                moodBars.forEach { bar ->
                    val pct = ((bar.count.toDouble() / moodTotal) * 100).roundToInt()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(bar.emoji, fontSize = 14.sp)
                        Text(
                            bar.label,
                            color = TextMain,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = PlusJakartaSans,
                            modifier = Modifier.width(64.dp),
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(8.dp)
                                .clip(RoundedCornerShape(99.dp))
                                .background(Color.White.copy(alpha = 0.08f)),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(pct / 100f)
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(99.dp))
                                    .background(bar.color),
                            )
                        }
                        Text(
                            "$pct%",
                            color = Muted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = PlusJakartaSans,
                            modifier = Modifier.width(36.dp),
                            textAlign = TextAlign.End,
                        )
                    }
                }
            }
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

        // Next Growth Edge
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Brush.linearGradient(listOf(Purple, PurpleMid)))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                "Protect a recovery block on high-pressure days",
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
                "Protect Recovery Now",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = PlusJakartaSans,
                modifier = Modifier
                    .clickable(enabled = momentId != null, onClick = onProtectRecovery)
                    .padding(vertical = 8.dp),
            )
        }

        InsightsComingSoon("Recurring themes and growth patterns across your Life Ops journey will appear here.")
    }
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

@Composable
private fun PatternStep(emoji: String, label: String, tint: Color, modifier: Modifier = Modifier) {
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
                .size(32.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(tint),
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
private fun DriverColumn(
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

private data class MoodBar(val label: String, val emoji: String, val color: Color, val count: Int)

private fun moodBars(pulse: PersonalPulseDto?, activities: List<ActivityItemDto>): List<MoodBar> {
    val counts = mutableMapOf("Calm" to 0, "Focused" to 0, "Strained" to 0, "Lifted" to 0)
    activities.filter { it.activityCode.contains("MOOD", ignoreCase = true) }.forEach { item ->
        val title = item.title.lowercase()
        when {
            title.contains("calm") -> counts["Calm"] = counts.getValue("Calm") + 1
            title.contains("focus") -> counts["Focused"] = counts.getValue("Focused") + 1
            title.contains("strain") || title.contains("stain") || title.contains("stress") ->
                counts["Strained"] = counts.getValue("Strained") + 1
            title.contains("lift") || title.contains("energ") ->
                counts["Lifted"] = counts.getValue("Lifted") + 1
        }
    }
    val mood = pulse?.moodState?.trim().orEmpty()
    if (mood.isNotEmpty() && counts.values.sum() == 0) {
        val lower = mood.lowercase()
        val key = when {
            lower.contains("calm") -> "Calm"
            lower.contains("focus") -> "Focused"
            lower.contains("strain") || lower.contains("stress") -> "Strained"
            lower.contains("lift") -> "Lifted"
            else -> "Calm"
        }
        counts[key] = 1
    }
    return listOf(
        MoodBar("Calm", "😌", Blue, counts.getValue("Calm")),
        MoodBar("Focused", "🎯", Purple, counts.getValue("Focused")),
        MoodBar("Strained", "😮‍💨", Orange, counts.getValue("Strained")),
        MoodBar("Lifted", "✨", Green, counts.getValue("Lifted")),
    )
}

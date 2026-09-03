package com.example.momentra.ui.shell.personal.lifestyle.memory

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.api.ActivityItemDto
import com.example.momentra.data.api.PersonalPulseDto
import com.example.momentra.data.repository.PersonalSliceRepository
import com.example.momentra.ui.theme.PlusJakartaSans
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import com.example.momentra.ui.shell.personal.lifestyle.create.PersonalLifestyleDerived
import com.example.momentra.ui.shell.personal.shared.loadPersonalPulseTab
import com.example.momentra.ui.shell.personal.shared.PersonalTabDataCache

private val Bg = Color(0xFF14121B)
private val CardBg = Color(0xFF152022)
private val TextMain = Color(0xFFE5E0EE)
private val Muted = Color(0xFFC9C4D8)
private val Teal = Color(0xFF0EA5A4)
private val TealSoft = Color(0xFF5EEAD4)
private val Red = Color(0xFFF87171)
private val BorderSoft = Color.White.copy(alpha = 0.08f)

/** Lifestyle Memory — Figma `505:12665`; derives from pulse + activity (not GET /memory). */
@Composable
fun PersonalLifestyleMemoryActiveContent(
    refreshToken: Long,
    momentId: String?,
    onLogExperience: () -> Unit,
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

    val vitality = PersonalLifestyleDerived.vitalityIndexDisplay(pulse)
    val axes = PersonalLifestyleDerived.axisScores(pulse)
    val stability = PersonalLifestyleDerived.networkStability(pulse)
    val experienceCount = PersonalLifestyleDerived.experienceCount(pulse)
    val thinData = activities.size < 2 && vitality == "—"
    val patternConfidence = if (thinData) "Building…" else "${(40 + activities.size * 5).coerceAtMost(92)}% pattern confidence"
    val aiBody = when {
        thinData -> "Building… Log experiences and wellbeing to unlock vitality memory."
        experienceCount > 0 -> "Your system rewards experiences logged with intention — joy and exploration compound."
        else -> "Keep logging rituals and discovery moments to reveal lifestyle patterns."
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Bg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        error?.let { Text(it, color = Red, fontSize = 12.sp, fontFamily = PlusJakartaSans) }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Brush.horizontalGradient(listOf(Teal, TealSoft)))
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("VITALITY MEMORY", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
            Text(
                if (activities.isEmpty() && vitality == "—") "Awaiting first lifestyle signals" else stability,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
            )
            Text(patternConfidence, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp, fontFamily = PlusJakartaSans)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CardBg)
                .border(1.dp, BorderSoft, RoundedCornerShape(16.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("AXIS SNAPSHOT", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
            listOf("Joy" to axes.joy, "Fulfillment" to axes.fulfillment, "Vitality" to axes.vitality, "Exploration" to axes.exploration).forEach { (label, score) ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(label, color = TextMain, fontSize = 13.sp, fontFamily = PlusJakartaSans)
                    Text(score, color = TealSoft, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CardBg)
                .border(1.dp, BorderSoft, RoundedCornerShape(16.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("MEMORY INTERPRETATION", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
            Text(aiBody, color = TextMain, fontSize = 13.sp, fontFamily = PlusJakartaSans)
        }

        FamilyMemoryBody(
            memoryEmpty = true,
            activityTitles = activities.map { it.title },
            accent = Teal,
            emptyHint = "No memory projection yet. Activity-derived summaries below.",
            ctaLabel = "Log Experience",
            onCta = onLogExperience,
        )
    }
}

@Composable
internal fun FamilyMemoryBody(
    memoryEmpty: Boolean,
    activityTitles: List<String>,
    accent: Color,
    emptyHint: String,
    ctaLabel: String,
    onCta: () -> Unit,
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
        Text("RECENT ACTIVITY", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
        Text(emptyHint, color = Muted, fontSize = 13.sp, fontFamily = PlusJakartaSans)
        if (activityTitles.isNotEmpty()) {
            activityTitles.take(5).forEach { title ->
                Text("· $title", color = Muted, fontSize = 12.sp, fontFamily = PlusJakartaSans)
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(accent)
                .clickable(onClick = onCta)
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(ctaLabel, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
        }
    }
}

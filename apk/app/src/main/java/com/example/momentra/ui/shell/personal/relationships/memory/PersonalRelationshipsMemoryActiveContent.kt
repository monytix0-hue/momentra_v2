package com.example.momentra.ui.shell.personal.relationships.memory

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.example.momentra.ui.shell.personal.lifestyle.memory.FamilyMemoryBody
import com.example.momentra.ui.shell.personal.relationships.create.PersonalRelationshipsDerived
import com.example.momentra.ui.shell.personal.shared.loadPersonalPulseTab
import com.example.momentra.ui.shell.personal.shared.PersonalTabDataCache

private val Bg = Color(0xFF14121B)
private val CardBg = Color(0xFF1C1B2E)
private val TextMain = Color(0xFFE5E0EE)
private val Muted = Color(0xFFC9C4D8)
private val Pink = Color(0xFFE91E63)
private val PinkSoft = Color(0xFFF472B6)
private val Red = Color(0xFFF87171)
private val BorderSoft = Color.White.copy(alpha = 0.08f)

/** Relationships Memory — Figma `505:12093`; derives from pulse + activity (not GET /memory). */
@Composable
fun PersonalRelationshipsMemoryActiveContent(
    refreshToken: Long,
    momentId: String?,
    onLogConnection: () -> Unit,
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
            CircularProgressIndicator(color = Pink)
        }
        return
    }

    val bond = PersonalRelationshipsDerived.bondIndexDisplay(pulse)
    val axes = PersonalRelationshipsDerived.bondAxes(pulse)
    val subtitle = PersonalRelationshipsDerived.bondSubtitle(pulse)
    val thinData = activities.size < 2 && bond == "—"
    val patternConfidence = if (thinData) "Building…" else "${(40 + activities.size * 5).coerceAtMost(92)}% pattern confidence"
    val aiBody = when {
        thinData -> "Building… Log connections and support to unlock bond memory."
        axes.trust != "—" -> "Trust signals from logged connection are shaping your bond memory."
        else -> "Keep logging care and presence to reveal relationship patterns."
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
                .background(Brush.horizontalGradient(listOf(Pink, PinkSoft)))
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("BOND MEMORY", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
            Text(
                if (activities.isEmpty() && bond == "—") "Awaiting first bond signals" else subtitle,
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
            Text("BOND AXES", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
            listOf("Trust" to axes.trust, "Care" to axes.care, "Support" to axes.support, "Presence" to axes.presence).forEach { (label, score) ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(label, color = TextMain, fontSize = 13.sp, fontFamily = PlusJakartaSans)
                    Text(score, color = PinkSoft, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
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
            accent = Pink,
            emptyHint = "No memory projection yet. Activity-derived summaries below.",
            ctaLabel = "Log Connection",
            onCta = onLogConnection,
        )
    }
}

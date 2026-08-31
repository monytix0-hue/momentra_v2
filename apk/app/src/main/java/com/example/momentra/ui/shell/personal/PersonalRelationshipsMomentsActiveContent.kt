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
private val CardBg = Color(0xFF1C1B2E)
private val TextMain = Color(0xFFE5E0EE)
private val Muted = Color(0xFFC9C4D8)
private val Pink = Color(0xFFE91E63)
private val PinkSoft = Color(0xFFF472B6)
private val Red = Color(0xFFF87171)
private val BorderSoft = Color.White.copy(alpha = 0.08f)

/** Relationships Moments — Figma `505:12002`; pulse + activity fetch. */
@Composable
fun PersonalRelationshipsMomentsActiveContent(
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
            CircularProgressIndicator(color = Pink)
        }
        return
    }

    val theme = PersonalPulseFamily.RELATIONSHIPS.theme()
    val bond = PersonalRelationshipsDerived.bondIndexDisplay(pulse)
    val axes = PersonalRelationshipsDerived.bondAxes(pulse)
    val spend = PersonalRelationshipsDerived.spendPairs(pulse)
    val connectionCount = activities.count {
        it.activityCode.contains("CONNECTION", ignoreCase = true) ||
            it.activityCode.contains("INTERACTION", ignoreCase = true)
    }
    val insight = when {
        bond == "—" -> "Log connections and support to reveal your Bond Index."
        connectionCount > 0 -> "Trust and care signals are deepening with logged connection."
        else -> "Early bond signals form as connection and support logs accumulate."
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
            if (!momentTitle.isNullOrBlank()) {
                Text(momentTitle, color = Muted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Brush.horizontalGradient(listOf(Pink, PinkSoft)))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(theme.heroTitle, color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
                Text(bond, color = Color.White, fontSize = 48.sp, fontWeight = FontWeight.ExtraBold, fontFamily = PlusJakartaSans)
                Text(insight, color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp, fontFamily = PlusJakartaSans)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(axes.trust, axes.care, axes.support, axes.presence).forEachIndexed { i, score ->
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
                    BondMiniStat("💬", if (connectionCount > 0) "$connectionCount" else "—", "Connections", Modifier.weight(1f))
                    BondMiniStat("📝", if (activities.isEmpty()) "—" else "${activities.size}", "Logs", Modifier.weight(1f))
                    BondMiniStat("🫶", axes.care, "Care", Modifier.weight(1f))
                    BondMiniStat("💰", if (spend.isEmpty()) "—" else spend.first().second, "Spend", Modifier.weight(1f))
                }
            }

            Text("Bond Journey", color = TextMain, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
            if (activities.isEmpty()) {
                BondEmptyCard("No bond entries yet. Log a connection or support moment to start.")
            } else {
                activities.take(8).forEach { BondJourneyItem(it) }
            }

            FamilySpendSection(title = theme.moneyTitle, spend = spend, accent = Pink, onAddExpense = onAddExpense)

            BondEmptyCard(
                if (bond == "—") {
                    "Patterns across trust, care, and presence will surface as you log."
                } else {
                    "Bond memory builds from logged connection — keep investing in presence."
                },
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Pink)
                    .clickable(onClick = onOpenQuickAdd)
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(theme.nudgeCta, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 16.dp)
                .size(52.dp)
                .clip(CircleShape)
                .background(Pink)
                .clickable(enabled = momentId != null, onClick = onAddExpense),
            contentAlignment = Alignment.Center,
        ) {
            Text("₹+", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, fontFamily = PlusJakartaSans)
        }
    }
}

@Composable
private fun BondMiniStat(emoji: String, value: String, label: String, modifier: Modifier = Modifier) {
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
private fun BondJourneyItem(item: ActivityItemDto) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Pink.copy(alpha = 0.1f))
            .border(1.dp, Pink.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(18.dp)).background(Pink),
            contentAlignment = Alignment.Center,
        ) { Text("💬", fontSize = 16.sp) }
        Column(Modifier.weight(1f)) {
            Text(item.title, color = TextMain, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(item.occurredAt.take(16), color = Muted, fontSize = 11.sp, fontFamily = PlusJakartaSans)
        }
    }
}

@Composable
private fun BondEmptyCard(body: String) {
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

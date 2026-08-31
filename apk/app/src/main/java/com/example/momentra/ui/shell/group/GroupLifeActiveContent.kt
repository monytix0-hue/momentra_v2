package com.example.momentra.ui.shell.group

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.api.GroupLifeBalanceBarDto
import com.example.momentra.data.api.GroupLifeDomainMetricDto
import com.example.momentra.data.api.GroupLifePayloadDto
import com.example.momentra.data.repository.GroupSliceRepository
import com.example.momentra.ui.shell.empty.group.GeBg
import com.example.momentra.ui.shell.empty.group.GeBorder
import com.example.momentra.ui.shell.empty.group.GeCard
import com.example.momentra.ui.shell.empty.group.GeSecondary
import com.example.momentra.ui.shell.empty.group.GeText
import com.example.momentra.ui.theme.PlusJakartaSans
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** Figma 1267:11212 — Group Life intelligence (shared across all group moment types). */
enum class GroupLifeQuickAction {
    EXPERIENCE,
    PURCHASE,
    LIVING,
    GOAL,
    COMMUNITY,
}

private val LifeOrange = Color(0xFFF97316)
private val LifeYellow = Color(0xFFEAB308)
private val LifeTeal = Color(0xFF14B8A6)
private val LifeGreen = Color(0xFF22C55E)
private val LifePurple = Color(0xFFA855F7)
private val LifeBlue = Color(0xFF6366F1)
private val LifeMuted = Color(0xFF9CA3AF)
private val Red = Color(0xFFF87171)

@Composable
fun GroupLifeActiveContent(
    momentId: String?,
    momentTitle: String?,
    refreshToken: Long,
    onQuickAction: (GroupLifeQuickAction) -> Unit = {},
    repository: GroupSliceRepository = remember { GroupSliceRepository() },
    modifier: Modifier = Modifier,
) {
    var loading by remember { mutableStateOf(true) }
    var payload by remember { mutableStateOf<GroupLifePayloadDto?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(refreshToken, momentId) {
        if (momentId.isNullOrBlank()) {
            loading = false
            payload = null
            return@LaunchedEffect
        }
        error = null
        GroupTabDataCache.peekLife(momentId)?.let { cached ->
            payload = cached
            loading = false
        }
        if (payload == null && GroupTabDataCache.peekPulse(momentId) != null) {
            loading = false
        } else if (payload == null) {
            loading = true
        }
        repository.getLife(momentId).fold(
            onSuccess = {
                payload = it.payload
                GroupTabDataCache.putLife(momentId, it.payload)
                loading = false
            },
            onFailure = { e ->
                error = e.message
                loading = false
            },
        )
    }

    if (loading && payload == null) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = LifeTeal)
        }
        return
    }

    val domains = payload?.domains
    val health = payload?.health
    val balance = payload?.balance
    val drivers = payload?.drivers.orEmpty()
    val planningItems = payload?.planningItems.orEmpty()
    val bookings = payload?.bookings.orEmpty()
    val updates = payload?.updates.orEmpty()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(GeBg)
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
                color = GeSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = PlusJakartaSans,
            )
        }
        Text(
            "Group Life",
            color = GeText,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = PlusJakartaSans,
        )

        LifeRadarCard(
            healthLabel = health?.label ?: "—",
            healthScore = health?.score,
            experience = domains?.experience,
            purchase = domains?.purchase,
            living = domains?.living,
            goal = domains?.goal,
            community = domains?.community,
        )

        SectionLabel("AT A GLANCE")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            GlanceChip("EXP", domains?.experience, LifeOrange, Modifier.weight(1f))
            GlanceChip("PUR", domains?.purchase, LifeYellow, Modifier.weight(1f))
            GlanceChip("LIV", domains?.living, LifeTeal, Modifier.weight(1f))
            GlanceChip("GOL", domains?.goal, LifeGreen, Modifier.weight(1f))
            GlanceChip("COM", domains?.community, LifePurple, Modifier.weight(1f))
        }

        SectionLabel("BALANCE MODEL")
        LifeCard {
            BalanceRow("PARTICIPATION", balance?.participation, LifeTeal)
            BalanceRow("CONTRIBUTION", balance?.contribution, LifeOrange)
            BalanceRow("COORDINATION", balance?.coordination, LifeBlue)
            BalanceRow("PROGRESS", balance?.progress, LifeYellow)
            BalanceRow("COMMUNITY", balance?.community, LifeTeal)
        }

        SectionLabel("WHAT DRIVES YOUR GROUP")
        if (drivers.isEmpty()) {
            HonestEmptyCard("Driver insights appear when domain scores diverge.")
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                drivers.forEach { d ->
                    Column(
                        modifier = Modifier
                            .width(200.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(GeCard)
                            .border(1.dp, GeBorder, RoundedCornerShape(14.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            d.title ?: d.domain.orEmpty(),
                            color = GeText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            fontFamily = PlusJakartaSans,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            d.detail.orEmpty(),
                            color = GeSecondary,
                            fontSize = 11.sp,
                            fontFamily = PlusJakartaSans,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            HonestEmptyCard(
                "Group drift alerts need a longer activity history.",
                modifier = Modifier.weight(1f),
                eyebrow = "GROUP DRIFT",
            )
            HonestEmptyCard(
                "Highest leverage unlocks with analytics history.",
                modifier = Modifier.weight(1f),
                eyebrow = "HIGHEST LEVERAGE",
            )
        }

        SectionLabel("EVOLUTION")
        HonestEmptyCard("Sparklines need month-over-month history (coming soon).")

        SectionLabel("WHAT CHANGED THIS MONTH")
        HonestEmptyCard("Monthly deltas require historical snapshots.")

        SectionLabel("GROUP JOURNEY")
        HonestEmptyCard("Journey timeline lights up as multi-moment history accumulates.")

        SectionLabel("MOMENTRA INTELLIGENCE")
        HonestEmptyCard("AI narrative insights are not available yet.")

        SectionLabel("QUICK ACTIONS")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            QuickPill("Experience", LifeOrange) { onQuickAction(GroupLifeQuickAction.EXPERIENCE) }
            QuickPill("Purchase", LifeYellow) { onQuickAction(GroupLifeQuickAction.PURCHASE) }
            QuickPill("Living", LifeTeal) { onQuickAction(GroupLifeQuickAction.LIVING) }
            QuickPill("Goal", LifeGreen) { onQuickAction(GroupLifeQuickAction.GOAL) }
            QuickPill("Community", LifePurple) { onQuickAction(GroupLifeQuickAction.COMMUNITY) }
        }

        if (planningItems.isNotEmpty()) {
            SectionLabel("OPEN PLANS")
            planningItems.forEach { item ->
                ListRow(item.title ?: item.planningItemId.orEmpty())
            }
        }
        if (bookings.isNotEmpty()) {
            SectionLabel("BOOKINGS")
            bookings.forEach { item ->
                ListRow(item.title ?: item.bookingId.orEmpty())
            }
        }
        if (updates.isNotEmpty()) {
            SectionLabel("UPDATES")
            updates.take(10).forEach { item ->
                ListRow(item.message ?: item.updateId.orEmpty())
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        color = LifeMuted,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = PlusJakartaSans,
        letterSpacing = 0.8.sp,
    )
}

@Composable
private fun LifeCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(GeCard)
            .border(1.dp, GeBorder, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content,
    )
}

@Composable
private fun HonestEmptyCard(
    body: String,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(GeCard)
            .border(1.dp, GeBorder, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (eyebrow != null) {
            Text(
                eyebrow,
                color = LifeMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
            )
        }
        Text(body, color = GeSecondary, fontSize = 12.sp, fontFamily = PlusJakartaSans)
    }
}

@Composable
private fun GlanceChip(
    code: String,
    metric: GroupLifeDomainMetricDto?,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(GeCard)
            .border(1.dp, GeBorder, RoundedCornerShape(10.dp))
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(accent),
        )
        Spacer(Modifier.height(6.dp))
        Text(code, color = LifeMuted, fontSize = 9.sp, fontFamily = PlusJakartaSans)
        Text(
            metric?.label ?: "—",
            color = GeText,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            fontFamily = PlusJakartaSans,
        )
    }
}

@Composable
private fun BalanceRow(name: String, bar: GroupLifeBalanceBarDto?, accent: Color) {
    val value = bar?.value
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        androidx.compose.foundation.layout.Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(name, color = GeSecondary, fontSize = 10.sp, fontFamily = PlusJakartaSans)
            Text(
                if (value == null) "-" else "${bar?.label ?: value}",
                color = GeText,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = PlusJakartaSans,
            )
        }
        LinearProgressIndicator(
            progress = { (value ?: 0).coerceIn(0, 100) / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = if (value == null) GeBorder else accent,
            trackColor = GeBorder,
        )
    }
}

@Composable
private fun QuickPill(label: String, accent: Color, onClick: () -> Unit) {
    Text(
        label,
        color = GeText,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        fontFamily = PlusJakartaSans,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(accent.copy(alpha = 0.18f))
            .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

@Composable
private fun ListRow(text: String) {
    Text(
        text,
        color = GeSecondary,
        fontSize = 13.sp,
        fontFamily = PlusJakartaSans,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(GeCard)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    )
}

@Composable
private fun LifeRadarCard(
    healthLabel: String,
    healthScore: Int?,
    experience: GroupLifeDomainMetricDto?,
    purchase: GroupLifeDomainMetricDto?,
    living: GroupLifeDomainMetricDto?,
    goal: GroupLifeDomainMetricDto?,
    community: GroupLifeDomainMetricDto?,
) {
    // Vertex order (clockwise from top): Living, Community, Goal, Purchase, Experience
    val scores = listOf(
        (living?.score ?: 0) / 100f,
        (community?.score ?: 0) / 100f,
        (goal?.score ?: 0) / 100f,
        (purchase?.score ?: 0) / 100f,
        (experience?.score ?: 0) / 100f,
    )
    val accents = listOf(LifeTeal, LifePurple, LifeGreen, LifeYellow, LifeOrange)
    val labels = listOf(
        "LIVING" to (living?.label ?: "—"),
        "COMMUNITY" to (community?.label ?: "—"),
        "GOAL" to (goal?.label ?: "—"),
        "PURCHASE" to (purchase?.label ?: "—"),
        "EXPERIENCE" to (experience?.label ?: "—"),
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(GeCard)
            .border(1.dp, GeBorder, RoundedCornerShape(18.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(28.dp)) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val r = min(size.width, size.height) / 2f
            val n = 5
            fun point(i: Int, scale: Float): Offset {
                val angle = Math.toRadians((-90.0 + i * 72.0))
                return Offset(
                    cx + (r * scale * cos(angle)).toFloat(),
                    cy + (r * scale * sin(angle)).toFloat(),
                )
            }
            listOf(0.35f, 0.65f, 1f).forEach { ring ->
                val path = Path()
                repeat(n) { i ->
                    val p = point(i, ring)
                    if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
                }
                path.close()
                drawPath(path, GeBorder, style = Stroke(width = 1.5f))
            }
            repeat(n) { i ->
                drawLine(GeBorder, Offset(cx, cy), point(i, 1f), strokeWidth = 1f)
            }
            val data = Path()
            repeat(n) { i ->
                val scale = 0.12f + scores[i] * 0.88f
                val p = point(i, scale)
                if (i == 0) data.moveTo(p.x, p.y) else data.lineTo(p.x, p.y)
            }
            data.close()
            drawPath(data, LifePurple.copy(alpha = 0.22f))
            drawPath(data, LifePurple.copy(alpha = 0.85f), style = Stroke(width = 2.5f, cap = StrokeCap.Round))
            repeat(n) { i ->
                val scale = 0.12f + scores[i] * 0.88f
                drawCircle(accents[i], radius = 5f, center = point(i, scale))
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                healthLabel,
                color = GeText,
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = PlusJakartaSans,
            )
            Text(
                "HEALTH SCORE",
                color = LifeMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
            )
            if (healthScore == null) {
                Text(
                    "No signal yet",
                    color = GeSecondary,
                    fontSize = 11.sp,
                    fontFamily = PlusJakartaSans,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
        // Corner domain labels
        DomainCornerLabel(labels[0].first, labels[0].second, LifeTeal, Alignment.TopCenter)
        DomainCornerLabel(labels[1].first, labels[1].second, LifePurple, Alignment.CenterEnd)
        DomainCornerLabel(labels[2].first, labels[2].second, LifeGreen, Alignment.BottomEnd)
        DomainCornerLabel(labels[3].first, labels[3].second, LifeYellow, Alignment.BottomStart)
        DomainCornerLabel(labels[4].first, labels[4].second, LifeOrange, Alignment.CenterStart)
    }
}

@Composable
private fun BoxScope.DomainCornerLabel(
    name: String,
    value: String,
    accent: Color,
    align: Alignment,
) {
    Column(
        modifier = Modifier
            .align(align)
            .padding(10.dp),
        horizontalAlignment = when (align) {
            Alignment.CenterStart -> Alignment.Start
            Alignment.CenterEnd -> Alignment.End
            else -> Alignment.CenterHorizontally
        },
    ) {
        Text(name, color = accent, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
        Text(value, color = GeText, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
    }
}

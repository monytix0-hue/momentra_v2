package com.example.momentra.ui.shell.group.experience.create

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.ui.shell.group.shared.GroupExperienceFamily
import com.example.momentra.ui.theme.PlusJakartaSans

/** Figma House Party 584:* (blue) / Office Outing 584:* (teal). */
data class ExperienceActiveTheme(
    val bg: Color,
    val accent: Color,
    val accentLight: Color,
    val accentSoft: Color,
    val accentSolid: Color,
    val text: Color,
    val secondary: Color,
    val muted: Color,
    val card: Color,
    val border: Color,
    val darkText: Color,
    val typeLabel: String,
    val pulseTitle: String,
    val crewTitle: String,
    val budgetTitle: String,
    val progressTitle: String,
    val insightsTitle: String,
    val healthLabel: String,
    val hubHeroRes: Int,
    val heroEmoji: String,
    val participantRoles: List<String>,
    val participantSubtitle: String,
    val heroGradient: Brush,
    val pulseHeroGradient: Brush,
    val includesVendor: Boolean,
    val quickChips: List<Triple<String, String, ExperienceQuickAddKind>>,
    val statGradients: List<Brush>,
) {
    companion object {
        val HouseParty = ExperienceActiveTheme(
            bg = Color(0xFF131313),
            accent = Color(0xFF3B82F6),
            accentLight = Color(0xFF60A5FA),
            accentSoft = Color(0x333B82F6),
            accentSolid = Color(0xFF3B82F6),
            text = Color(0xFFE5E2E1),
            secondary = Color(0xFFA8B4C8),
            muted = Color(0xFFA8A19E),
            card = Color(0xFF201F1F),
            border = Color(0x1AFFFFFF),
            darkText = Color(0xFF14121B),
            typeLabel = "House Party",
            pulseTitle = "Party Pulse",
            crewTitle = "Party Crew",
            budgetTitle = "Party Budget",
            progressTitle = "Party Progress",
            insightsTitle = "Party Insights",
            healthLabel = "Party Health",
            hubHeroRes = com.example.momentra.R.drawable.house_party_hub_hero,
            heroEmoji = "🎉",
            participantRoles = listOf("Host", "Co-host", "Guest"),
            participantSubtitle = "Invite and manage party guest list",
            heroGradient = Brush.horizontalGradient(listOf(Color(0xFF60A5FA), Color(0xFF2563EB))),
            pulseHeroGradient = Brush.linearGradient(
                colorStops = arrayOf(
                    0.25f to Color(0xFF669EFA),
                    0.75f to Color(0xFF2659D9).copy(alpha = 0.85f),
                ),
                start = androidx.compose.ui.geometry.Offset(40f, 0f),
                end = androidx.compose.ui.geometry.Offset(360f, 400f),
            ),
            includesVendor = true,
            quickChips = listOf(
                Triple("📷", "Photos", ExperienceQuickAddKind.MEMORY),
                Triple("🎵", "Playlist", ExperienceQuickAddKind.UPDATE),
                Triple("📋", "Menu", ExperienceQuickAddKind.PLANNING),
                Triple("🍹", "Drinks", ExperienceQuickAddKind.EXPENSE),
            ),
            statGradients = listOf(
                Brush.horizontalGradient(listOf(Color(0xFF2563EB), Color(0xFF1D4ED8))),
                Brush.horizontalGradient(listOf(Color(0xFF3B82F6), Color(0xFF1E40AF))),
                Brush.horizontalGradient(listOf(Color(0xFF60A5FA), Color(0xFF2563EB))),
            ),
        )

        val OfficeOuting = ExperienceActiveTheme(
            bg = Color(0xFF131313),
            accent = Color(0xFF14B8A6),
            accentLight = Color(0xFF2DD4BF),
            accentSoft = Color(0x3314B8A6),
            accentSolid = Color(0xFF14B8A6),
            text = Color(0xFFE5E2E1),
            secondary = Color(0xFFA8C4C0),
            muted = Color(0xFFA8A19E),
            card = Color(0xFF201F1F),
            border = Color(0x1AFFFFFF),
            darkText = Color(0xFF14121B),
            typeLabel = "Office Outing",
            pulseTitle = "Team Retreat Pulse",
            crewTitle = "Team Leads",
            budgetTitle = "Retreat Budget",
            progressTitle = "Retreat Progress",
            insightsTitle = "Team Retreat Insights",
            healthLabel = "Team Health",
            hubHeroRes = com.example.momentra.R.drawable.office_outing_hub_hero,
            heroEmoji = "🧳",
            participantRoles = listOf("Organizer", "Teammate", "Guest"),
            participantSubtitle = "Invite and manage outing attendees",
            heroGradient = Brush.horizontalGradient(listOf(Color(0xFF2DD4BF), Color(0xFF0F766E))),
            pulseHeroGradient = Brush.linearGradient(
                colorStops = arrayOf(
                    0.25f to Color(0xFF2DD4BF),
                    0.75f to Color(0xFF0F766E).copy(alpha = 0.9f),
                ),
                start = androidx.compose.ui.geometry.Offset(40f, 0f),
                end = androidx.compose.ui.geometry.Offset(360f, 400f),
            ),
            includesVendor = false,
            quickChips = listOf(
                Triple("📷", "Photos", ExperienceQuickAddKind.MEMORY),
                Triple("💬", "Agenda", ExperienceQuickAddKind.PLANNING),
                Triple("🧳", "Transport", ExperienceQuickAddKind.BOOKING),
                Triple("💰", "Expenses", ExperienceQuickAddKind.EXPENSE),
            ),
            statGradients = listOf(
                Brush.horizontalGradient(listOf(Color(0xFF0D9488), Color(0xFF0F766E))),
                Brush.horizontalGradient(listOf(Color(0xFF14B8A6), Color(0xFF0F766E))),
                Brush.horizontalGradient(listOf(Color(0xFF2DD4BF), Color(0xFF0D9488))),
            ),
        )

        fun forFamily(family: GroupExperienceFamily): ExperienceActiveTheme =
            when (family) {
                GroupExperienceFamily.OFFICE_OUTING -> OfficeOuting
                else -> HouseParty
            }
    }
}

enum class ExperienceQuickAddKind {
    PARTICIPANT,
    PLANNING,
    EXPENSE,
    BUDGET,
    CONTRIBUTION,
    SETTLE,
    VENDOR,
    ATTENDANCE,
    UPDATE,
    POLL,
    MEMORY,
    BOOKING,
}

fun ExperienceQuickAddKind.label(): String = when (this) {
    ExperienceQuickAddKind.PARTICIPANT -> "Participant"
    ExperienceQuickAddKind.PLANNING -> "Planning Item"
    ExperienceQuickAddKind.EXPENSE -> "Expense"
    ExperienceQuickAddKind.BUDGET -> "Budget"
    ExperienceQuickAddKind.CONTRIBUTION -> "Contribution"
    ExperienceQuickAddKind.SETTLE -> "Settle"
    ExperienceQuickAddKind.VENDOR -> "Vendor"
    ExperienceQuickAddKind.ATTENDANCE -> "Attendance"
    ExperienceQuickAddKind.UPDATE -> "Update"
    ExperienceQuickAddKind.POLL -> "Poll"
    ExperienceQuickAddKind.MEMORY -> "Memory"
    ExperienceQuickAddKind.BOOKING -> "Booking"
}

fun ExperienceQuickAddKind.emoji(): String = when (this) {
    ExperienceQuickAddKind.PARTICIPANT -> "👤"
    ExperienceQuickAddKind.PLANNING -> "📋"
    ExperienceQuickAddKind.EXPENSE -> "💳"
    ExperienceQuickAddKind.BUDGET -> "💰"
    ExperienceQuickAddKind.CONTRIBUTION -> "🎁"
    ExperienceQuickAddKind.SETTLE -> "⚖️"
    ExperienceQuickAddKind.VENDOR -> "🏪"
    ExperienceQuickAddKind.ATTENDANCE -> "✅"
    ExperienceQuickAddKind.UPDATE -> "📢"
    ExperienceQuickAddKind.POLL -> "📊"
    ExperienceQuickAddKind.MEMORY -> "📷"
    ExperienceQuickAddKind.BOOKING -> "🧳"
}

fun experienceHubTiles(includesVendor: Boolean): List<ExperienceQuickAddKind> {
    val tiles = mutableListOf(
        ExperienceQuickAddKind.PARTICIPANT,
        ExperienceQuickAddKind.PLANNING,
        ExperienceQuickAddKind.EXPENSE,
        ExperienceQuickAddKind.BUDGET,
    )
    if (includesVendor) tiles.add(ExperienceQuickAddKind.VENDOR)
    tiles.addAll(
        listOf(
            ExperienceQuickAddKind.ATTENDANCE,
            ExperienceQuickAddKind.UPDATE,
            ExperienceQuickAddKind.POLL,
            ExperienceQuickAddKind.MEMORY,
        ),
    )
    return tiles
}

@Composable
fun ExperienceSectionCard(
    theme: ExperienceActiveTheme,
    title: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(theme.card)
            .border(1.dp, theme.border, RoundedCornerShape(20.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, color = theme.text, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, fontFamily = PlusJakartaSans)
            trailing?.invoke()
        }
        content()
    }
}

@Composable
fun ExperienceEmptyBlock(theme: ExperienceActiveTheme, message: String, detail: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(message, color = theme.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
        Text(detail, color = theme.secondary, fontSize = 12.sp, fontFamily = PlusJakartaSans)
    }
}

@Composable
fun ExperienceStatCard(label: String, value: String, gradient: Brush, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(gradient)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(label, color = Color.White.copy(alpha = 0.95f), fontSize = 10.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
        Text(value, color = Color.White.copy(alpha = 0.95f), fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
    }
}

@Composable
fun ExperienceEmojiChip(
    theme: ExperienceActiveTheme,
    label: String,
    emoji: String,
    enabled: Boolean = true,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(theme.card)
                .border(1.dp, theme.border, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(emoji, fontSize = 22.sp)
        }
        Text(label, color = theme.text, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
    }
}

@Composable
fun ExperienceCrewRow(
    theme: ExperienceActiveTheme,
    name: String,
    role: String,
    percent: Int,
    featured: Boolean = false,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(theme.accentSoft),
                contentAlignment = Alignment.Center,
            ) {
                Text(name.take(1).uppercase(), color = theme.accentLight, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "$name ($role)${if (featured) " ★" else ""}",
                    color = theme.text,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = PlusJakartaSans,
                )
                Text(if (featured) "Most active" else "Active", color = theme.muted, fontSize = 11.sp, fontFamily = PlusJakartaSans)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(theme.accentSoft),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(percent.coerceIn(0, 100) / 100f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(theme.accent),
                    )
                }
            }
            Text("$percent%", color = theme.accentLight, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
        }
    }
}

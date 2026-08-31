package com.example.momentra.ui.shell.group.living

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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.ui.shell.group.GroupExperienceFamily
import com.example.momentra.ui.theme.PlusJakartaSans

/** Figma Flatmates 621:* / Co-living 629:* / Family Household 629:* / Custom Living 629:*. */
data class LivingActiveTheme(
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
    val contributionsTitle: String,
    val budgetTitle: String,
    val insightsTitle: String,
    val healthLabel: String,
    val financeTitle: String,
    val hubHeroRes: Int,
    val heroEmoji: String,
    val participantRoles: List<String>,
    val participantSubtitle: String,
    val heroGradient: Brush,
    val pulseHeroGradient: Brush,
    val includesContribution: Boolean,
    val quickChips: List<Triple<String, String, LivingQuickAddKind>>,
    val statGradients: List<Brush>,
) {
    companion object {
        val Flatmates = LivingActiveTheme(
            bg = Color(0xFF14121B),
            accent = Color(0xFFE8744F),
            accentLight = Color(0xFFFF7A3D),
            accentSoft = Color(0x33E8744F),
            accentSolid = Color(0xFFEA580C),
            text = Color(0xFFE5E2E1),
            secondary = Color(0xFF9CA3AF),
            muted = Color(0xFFA8A19E),
            card = Color(0xFF1C1926),
            border = Color(0xFF2A2538),
            darkText = Color(0xFF14121B),
            typeLabel = "Flatmates",
            pulseTitle = "Household Pulse",
            contributionsTitle = "Member Contributions",
            budgetTitle = "Household Budget",
            insightsTitle = "Household Insights",
            healthLabel = "Household Health",
            financeTitle = "Group Finance",
            hubHeroRes = com.example.momentra.R.drawable.flatmates_hub_hero,
            heroEmoji = "🏠",
            participantRoles = listOf("Organizer", "Flatmate", "Guest"),
            participantSubtitle = "Add someone to the household roster",
            heroGradient = Brush.horizontalGradient(listOf(Color(0xFFFF7A3D), Color(0xFFE8744F))),
            pulseHeroGradient = Brush.linearGradient(
                colorStops = arrayOf(
                    0.25f to Color(0xFFFF7A3D),
                    0.75f to Color(0xFFEA580C).copy(alpha = 0.9f),
                ),
                start = Offset(40f, 0f),
                end = Offset(360f, 400f),
            ),
            includesContribution = true,
            quickChips = listOf(
                Triple("🏠", "Resident", LivingQuickAddKind.RESIDENT),
                Triple("💳", "Expense", LivingQuickAddKind.EXPENSE),
                Triple("🎁", "Contribute", LivingQuickAddKind.CONTRIBUTION),
                Triple("✅", "Task", LivingQuickAddKind.TASK),
            ),
            statGradients = listOf(
                Brush.horizontalGradient(listOf(Color(0xFFEA580C), Color(0xFFC2410C))),
                Brush.horizontalGradient(listOf(Color(0xFFE8744F), Color(0xFFEA580C))),
                Brush.horizontalGradient(listOf(Color(0xFFFF7A3D), Color(0xFFE8744F))),
                Brush.horizontalGradient(listOf(Color(0xFFFDBA74), Color(0xFFE8744F))),
            ),
        )

        // Figma 629:10541 hub — cyan #06B6D4
        val CoLiving = LivingActiveTheme(
            bg = Color(0xFF14121B),
            accent = Color(0xFF06B6D4),
            accentLight = Color(0xFF22D3EE),
            accentSoft = Color(0x3306B6D4),
            accentSolid = Color(0xFF0891B2),
            text = Color(0xFFE5E2E1),
            secondary = Color(0xFF9CA3AF),
            muted = Color(0xFFA8A19E),
            card = Color(0xFF1C1926),
            border = Color(0xFF2A2538),
            darkText = Color(0xFF14121B),
            typeLabel = "Co-living",
            pulseTitle = "Community Pulse",
            contributionsTitle = "Member Contributions",
            budgetTitle = "Community Budget",
            insightsTitle = "Community Insights",
            healthLabel = "Community Health",
            financeTitle = "Community Hub",
            hubHeroRes = com.example.momentra.R.drawable.coliving_hub_hero,
            heroEmoji = "🏢",
            participantRoles = listOf("Organizer", "Resident", "Guest"),
            participantSubtitle = "Invite people sharing this co-living space",
            heroGradient = Brush.horizontalGradient(listOf(Color(0xFF068CA6), Color(0xFF043744))),
            pulseHeroGradient = Brush.linearGradient(
                colorStops = arrayOf(
                    0.25f to Color(0xFF22D3EE),
                    0.75f to Color(0xFF0E7490).copy(alpha = 0.9f),
                ),
                start = Offset(40f, 0f),
                end = Offset(360f, 400f),
            ),
            includesContribution = true,
            quickChips = listOf(
                Triple("🏠", "Resident", LivingQuickAddKind.RESIDENT),
                Triple("💳", "Expense", LivingQuickAddKind.EXPENSE),
                Triple("🎁", "Contribute", LivingQuickAddKind.CONTRIBUTION),
                Triple("🔧", "Maintain", LivingQuickAddKind.MAINTENANCE),
            ),
            statGradients = listOf(
                Brush.horizontalGradient(listOf(Color(0xFF0E7490), Color(0xFF155E75))),
                Brush.horizontalGradient(listOf(Color(0xFF06B6D4), Color(0xFF0891B2))),
                Brush.horizontalGradient(listOf(Color(0xFF22D3EE), Color(0xFF06B6D4))),
                Brush.horizontalGradient(listOf(Color(0xFF67E8F9), Color(0xFF06B6D4))),
            ),
        )

        // Figma 629:16126 hub — amber #F59E0B
        val FamilyHousehold = LivingActiveTheme(
            bg = Color(0xFF14121B),
            accent = Color(0xFFF59E0B),
            accentLight = Color(0xFFFBBF24),
            accentSoft = Color(0x33F59E0B),
            accentSolid = Color(0xFFD97706),
            text = Color(0xFFE5E2E1),
            secondary = Color(0xFF9CA3AF),
            muted = Color(0xFFA8A19E),
            card = Color(0xFF1C1926),
            border = Color(0xFF2A2538),
            darkText = Color(0xFF14121B),
            typeLabel = "Family Household",
            pulseTitle = "Family Pulse",
            contributionsTitle = "Household Members",
            budgetTitle = "Family Budget",
            insightsTitle = "Family Insights",
            healthLabel = "Family Health",
            financeTitle = "Group Finance",
            hubHeroRes = com.example.momentra.R.drawable.family_household_hub_hero,
            heroEmoji = "👨‍👩‍👧",
            participantRoles = listOf("Organizer", "Family", "Guest"),
            participantSubtitle = "Add a family member to the household",
            heroGradient = Brush.horizontalGradient(listOf(Color(0xFFF59E0B), Color(0xFF78350F))),
            pulseHeroGradient = Brush.linearGradient(
                colorStops = arrayOf(
                    0.25f to Color(0xFFFBBF24),
                    0.75f to Color(0xFFB45309).copy(alpha = 0.9f),
                ),
                start = Offset(40f, 0f),
                end = Offset(360f, 400f),
            ),
            includesContribution = false,
            quickChips = listOf(
                Triple("🏠", "Resident", LivingQuickAddKind.RESIDENT),
                Triple("💳", "Expense", LivingQuickAddKind.EXPENSE),
                Triple("✅", "Task", LivingQuickAddKind.TASK),
                Triple("📷", "Photos", LivingQuickAddKind.MEMORY),
            ),
            statGradients = listOf(
                Brush.horizontalGradient(listOf(Color(0xFFB45309), Color(0xFF92400E))),
                Brush.horizontalGradient(listOf(Color(0xFFF59E0B), Color(0xFFD97706))),
                Brush.horizontalGradient(listOf(Color(0xFFFBBF24), Color(0xFFF59E0B))),
                Brush.horizontalGradient(listOf(Color(0xFFFCD34D), Color(0xFFF59E0B))),
            ),
        )

        // Figma 629:15586 hub — emerald #10B981
        val CustomLiving = LivingActiveTheme(
            bg = Color(0xFF14121B),
            accent = Color(0xFF10B981),
            accentLight = Color(0xFF34D399),
            accentSoft = Color(0x3310B981),
            accentSolid = Color(0xFF059669),
            text = Color(0xFFE5E2E1),
            secondary = Color(0xFF9CA3AF),
            muted = Color(0xFFA8A19E),
            card = Color(0xFF1C1926),
            border = Color(0xFF2A2538),
            darkText = Color(0xFF14121B),
            typeLabel = "Custom Living",
            pulseTitle = "Living Pulse",
            contributionsTitle = "Household Members",
            budgetTitle = "Living Budget",
            insightsTitle = "Living Insights",
            healthLabel = "Living Health",
            financeTitle = "Property Hub",
            hubHeroRes = com.example.momentra.R.drawable.custom_living_hub_hero,
            heroEmoji = "✨",
            participantRoles = listOf("Organizer", "Resident", "Member"),
            participantSubtitle = "Add someone to this living arrangement",
            heroGradient = Brush.horizontalGradient(listOf(Color(0xFF10B981), Color(0xFF0F766E))),
            pulseHeroGradient = Brush.linearGradient(
                colorStops = arrayOf(
                    0.25f to Color(0xFF34D399),
                    0.75f to Color(0xFF047857).copy(alpha = 0.9f),
                ),
                start = Offset(40f, 0f),
                end = Offset(360f, 400f),
            ),
            includesContribution = false,
            quickChips = listOf(
                Triple("🏠", "Resident", LivingQuickAddKind.RESIDENT),
                Triple("💳", "Expense", LivingQuickAddKind.EXPENSE),
                Triple("✅", "Task", LivingQuickAddKind.TASK),
                Triple("📷", "Photos", LivingQuickAddKind.MEMORY),
            ),
            statGradients = listOf(
                Brush.horizontalGradient(listOf(Color(0xFF047857), Color(0xFF065F46))),
                Brush.horizontalGradient(listOf(Color(0xFF10B981), Color(0xFF059669))),
                Brush.horizontalGradient(listOf(Color(0xFF34D399), Color(0xFF10B981))),
                Brush.horizontalGradient(listOf(Color(0xFF6EE7B7), Color(0xFF10B981))),
            ),
        )

        fun forFamily(family: GroupExperienceFamily): LivingActiveTheme =
            when (family) {
                GroupExperienceFamily.FAMILY_HOUSEHOLD -> FamilyHousehold
                GroupExperienceFamily.CO_LIVING -> CoLiving
                GroupExperienceFamily.CUSTOM_LIVING -> CustomLiving
                else -> Flatmates
            }
    }
}

enum class LivingQuickAddKind {
    RESIDENT,
    EXPENSE,
    CONTRIBUTION,
    TASK,
    RULE,
    ASSET,
    MAINTENANCE,
    UPDATE,
    POLL,
    MEMORY,
}

fun LivingQuickAddKind.label(): String = when (this) {
    LivingQuickAddKind.RESIDENT -> "Resident"
    LivingQuickAddKind.EXPENSE -> "Expense"
    LivingQuickAddKind.CONTRIBUTION -> "Contribution"
    LivingQuickAddKind.TASK -> "Task"
    LivingQuickAddKind.RULE -> "House Rule"
    LivingQuickAddKind.ASSET -> "Asset"
    LivingQuickAddKind.MAINTENANCE -> "Maintenance"
    LivingQuickAddKind.UPDATE -> "Update"
    LivingQuickAddKind.POLL -> "Poll"
    LivingQuickAddKind.MEMORY -> "Memory"
}

fun LivingQuickAddKind.emoji(): String = when (this) {
    LivingQuickAddKind.RESIDENT -> "🏠"
    LivingQuickAddKind.EXPENSE -> "💳"
    LivingQuickAddKind.CONTRIBUTION -> "🎁"
    LivingQuickAddKind.TASK -> "✅"
    LivingQuickAddKind.RULE -> "📋"
    LivingQuickAddKind.ASSET -> "📦"
    LivingQuickAddKind.MAINTENANCE -> "🔧"
    LivingQuickAddKind.UPDATE -> "📢"
    LivingQuickAddKind.POLL -> "📊"
    LivingQuickAddKind.MEMORY -> "📷"
}

fun LivingQuickAddKind.isLive(): Boolean = this != LivingQuickAddKind.RULE

fun livingHubTiles(theme: LivingActiveTheme): List<LivingQuickAddKind> {
    val tiles = mutableListOf(
        LivingQuickAddKind.RESIDENT,
        LivingQuickAddKind.EXPENSE,
    )
    if (theme.includesContribution) tiles.add(LivingQuickAddKind.CONTRIBUTION)
    tiles.add(LivingQuickAddKind.TASK)
    if (theme.includesContribution) {
        tiles.add(LivingQuickAddKind.RULE)
        tiles.add(LivingQuickAddKind.ASSET)
        tiles.add(LivingQuickAddKind.MAINTENANCE)
        tiles.add(LivingQuickAddKind.UPDATE)
        tiles.add(LivingQuickAddKind.POLL)
        tiles.add(LivingQuickAddKind.MEMORY)
    } else {
        // Family / Custom: Resident, Expense, Task, Asset, Maintenance, Update, Rule, Poll, Memory
        tiles.add(LivingQuickAddKind.ASSET)
        tiles.add(LivingQuickAddKind.MAINTENANCE)
        tiles.add(LivingQuickAddKind.UPDATE)
        tiles.add(LivingQuickAddKind.RULE)
        tiles.add(LivingQuickAddKind.POLL)
        tiles.add(LivingQuickAddKind.MEMORY)
    }
    return tiles
}

@Composable
fun LivingSectionCard(
    theme: LivingActiveTheme,
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
fun LivingEmptyBlock(theme: LivingActiveTheme, message: String, detail: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(message, color = theme.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
        Text(detail, color = theme.secondary, fontSize = 12.sp, fontFamily = PlusJakartaSans)
    }
}

@Composable
fun LivingStatCard(label: String, value: String, gradient: Brush, modifier: Modifier = Modifier) {
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
fun LivingEmojiChip(
    theme: LivingActiveTheme,
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
fun LivingCrewRow(
    theme: LivingActiveTheme,
    name: String,
    role: String,
    amountLabel: String,
    featured: Boolean = false,
) {
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
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "$name ($role)${if (featured) " ★" else ""}",
                color = theme.text,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = PlusJakartaSans,
            )
            Text(if (featured) "Top contributor" else "Resident", color = theme.muted, fontSize = 11.sp, fontFamily = PlusJakartaSans)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(theme.accentSoft),
            )
        }
        Text(amountLabel, color = theme.accentLight, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
    }
}

package com.example.momentra.ui.shell.personal.shared

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.momentra.ui.shell.personal.shared.theme
import com.example.momentra.ui.shell.personal.shared.heroBrush

/** Personal Pulse family variants — Figma populated Pulse frames. */
enum class PersonalPulseFamily {
    LIFE_OPERATIONS,
    FUTURE_BUILDING,
    LIFESTYLE,
    RELATIONSHIPS,
}

fun personalPulseFamilyFor(momentTypeCode: String?): PersonalPulseFamily {
    val code = momentTypeCode?.uppercase() ?: return PersonalPulseFamily.LIFE_OPERATIONS
    return when {
        code.startsWith("LIFE_") || code == "LIFE_OPERATIONS" || code == "LIFE_RHYTHM" ->
            PersonalPulseFamily.LIFE_OPERATIONS
        code.startsWith("FUTURE_") || code == "FUTURE_BUILDING" ->
            PersonalPulseFamily.FUTURE_BUILDING
        code.startsWith("LIFESTYLE") ->
            PersonalPulseFamily.LIFESTYLE
        code.startsWith("RELATIONSHIP_") || code == "RELATIONSHIPS" ->
            PersonalPulseFamily.RELATIONSHIPS
        else -> PersonalPulseFamily.LIFE_OPERATIONS
    }
}

data class PersonalPulseFamilyTheme(
    val heroTitle: String,
    val heroSubtitleFilled: String,
    val heroSubtitleEmpty: String,
    val heroMetrics: List<String>,
    val tileLabels: List<String>,
    val nudgeTitle: String,
    val nudgeBody: String,
    val nudgeCta: String,
    val moneyTitle: String,
    val quickActions: List<String>,
    val heroStart: Color,
    val heroEnd: Color,
    val accent: Color,
)

fun PersonalPulseFamily.theme(): PersonalPulseFamilyTheme = when (this) {
    PersonalPulseFamily.LIFE_OPERATIONS -> PersonalPulseFamilyTheme(
        heroTitle = "YOUR DAY",
        heroSubtitleFilled = "Your day is taking shape",
        heroSubtitleEmpty = "Nothing logged yet",
        // Figma 353:8893 hero chips
        heroMetrics = listOf("Pressure", "Recovery", "Discipline", "Attention"),
        // Figma metric tiles (2x2)
        tileLabels = listOf("Pressure", "Recovery", "Discipline", "Attention"),
        nudgeTitle = "Protect Recovery",
        nudgeBody = "You've been busy — rest for a bit before the next stretch.",
        nudgeCta = "Log Recovery Now",
        moneyTitle = "This month's money",
        quickActions = listOf("Recovery", "Attention", "Mood", "Adjust"),
        heroStart = Color(0xFF7C5CFC),
        heroEnd = Color(0xFFA78BFA),
        accent = Color(0xFF7C5CFC),
    )
    PersonalPulseFamily.FUTURE_BUILDING -> PersonalPulseFamilyTheme(
        heroTitle = "YOUR FUTURE",
        heroSubtitleFilled = "Your trajectory is strong",
        heroSubtitleEmpty = "Nothing logged yet",
        heroMetrics = listOf("Vision", "Growth", "Momentum", "Discipline"),
        tileLabels = listOf("Vision", "Growth", "Momentum", "Discipline"),
        nudgeTitle = "Accelerate Growth",
        nudgeBody = "Log a milestone to keep momentum compounding.",
        nudgeCta = "Log Milestone",
        moneyTitle = "This month's investments",
        quickActions = listOf("Milestone", "Opportunity", "Pivot", "Progress", "Learning"),
        // SCREEN_STALE fix (G8): align with MomentThemes emerald, not purple Pulse leftover.
        heroStart = Color(0xFF10B981),
        heroEnd = Color(0xFF34D399),
        accent = Color(0xFF10B981),
    )
    PersonalPulseFamily.LIFESTYLE -> PersonalPulseFamilyTheme(
        heroTitle = "YOUR LIFESTYLE",
        heroSubtitleFilled = "Your lifestyle is taking shape",
        heroSubtitleEmpty = "Nothing logged yet",
        heroMetrics = listOf("Joy", "Fulfillment", "Vitality", "Exploration"),
        tileLabels = listOf("Joy", "Fulfillment", "Vitality", "Exploration"),
        nudgeTitle = "Protect a ritual",
        nudgeBody = "Log one experience to protect your lifestyle rhythm.",
        nudgeCta = "Log Experience",
        moneyTitle = "This month's lifestyle spend",
        quickActions = listOf("Experience", "Wellbeing", "Discovery", "Create", "Adjust"),
        heroStart = Color(0xFF0EA5A4),
        heroEnd = Color(0xFF7C5CFC),
        accent = Color(0xFF7C5CFC),
    )
    PersonalPulseFamily.RELATIONSHIPS -> PersonalPulseFamilyTheme(
        heroTitle = "YOUR PEOPLE",
        heroSubtitleFilled = "Your bonds are deepening",
        heroSubtitleEmpty = "Nothing logged yet",
        heroMetrics = listOf("Trust", "Care", "Support", "Presence"),
        tileLabels = listOf("Trust", "Care", "Support", "Presence"),
        nudgeTitle = "Protect Connection",
        nudgeBody = "Log a connection before the next busy stretch.",
        nudgeCta = "Log Connection",
        moneyTitle = "This month's shared spend",
        quickActions = listOf("Connection", "Shared", "Investment", "Support", "Adjust"),
        heroStart = Color(0xFFE91E63),
        heroEnd = Color(0xFFA78BFA),
        accent = Color(0xFFE12A9E),
    )
}

fun PersonalPulseFamily.loggingLabel(): String = when (this) {
    PersonalPulseFamily.LIFE_OPERATIONS -> "Life"
    PersonalPulseFamily.FUTURE_BUILDING -> "Future"
    PersonalPulseFamily.LIFESTYLE -> "Lifestyle"
    PersonalPulseFamily.RELATIONSHIPS -> "Relationships"
}

/** Plain life-system chip for Personal moment switcher. */
fun PersonalPulseFamily.switcherLabel(): String = when (this) {
    PersonalPulseFamily.LIFE_OPERATIONS -> "Everyday"
    PersonalPulseFamily.FUTURE_BUILDING -> "Future"
    PersonalPulseFamily.LIFESTYLE -> "Lifestyle"
    PersonalPulseFamily.RELATIONSHIPS -> "People"
}

fun personalSwitcherLabelFor(momentTypeCode: String?): String =
    personalPulseFamilyFor(momentTypeCode).switcherLabel()

fun PersonalPulseFamilyTheme.heroBrush(): Brush =
    Brush.horizontalGradient(listOf(heroStart, heroEnd))

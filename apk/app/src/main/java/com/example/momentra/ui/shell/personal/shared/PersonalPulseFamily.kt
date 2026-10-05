package com.example.momentra.ui.shell.personal.shared

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

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
    val todayActionLabels: List<String>,
    val nudgeTitle: String,
    val nudgeBody: String,
    val nudgeCta: String,
    val moneyTitle: String,
    val moneyCompactTitle: String,
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
        heroMetrics = listOf("Pressure", "Recovery", "Discipline", "Attention"),
        tileLabels = listOf("Pressure", "Recovery", "Discipline", "Attention"),
        todayActionLabels = listOf("Spend", "Mood", "Recovery"),
        nudgeTitle = "A small win today",
        nudgeBody = "Log recovery if you want it on today's record.",
        nudgeCta = "Log Recovery",
        moneyTitle = "This month's money",
        moneyCompactTitle = "This month",
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
        todayActionLabels = listOf("Milestone", "Progress", "Learning"),
        nudgeTitle = "Keep building",
        nudgeBody = "Log a milestone to keep momentum compounding.",
        nudgeCta = "Log Milestone",
        moneyTitle = "This month's investments",
        moneyCompactTitle = "This month",
        quickActions = listOf("Milestone", "Opportunity", "Pivot", "Progress", "Learning"),
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
        todayActionLabels = listOf("Experience", "Wellbeing", "Discovery"),
        nudgeTitle = "Protect a ritual",
        nudgeBody = "Log one experience to protect your lifestyle rhythm.",
        nudgeCta = "Log Experience",
        moneyTitle = "This month's lifestyle spend",
        moneyCompactTitle = "This month",
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
        todayActionLabels = listOf("Connect", "Shared", "Support"),
        nudgeTitle = "Stay close",
        nudgeBody = "Log a connection before the next busy stretch.",
        nudgeCta = "Log Connection",
        moneyTitle = "This month's shared spend",
        moneyCompactTitle = "This month",
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

data class VisiblePulseNudge(
    val title: String,
    val body: String?,
    val cta: String,
)

/**
 * Everyday nudge is omitted until today has a log, and never claims the person has been busy.
 * Other families keep their instruction copy.
 */
fun PersonalPulseFamily.visibleNudge(todayLogCount: Int): VisiblePulseNudge? {
    val theme = theme()
    if (this == PersonalPulseFamily.LIFE_OPERATIONS && todayLogCount <= 0) return null
    val body = if (this == PersonalPulseFamily.LIFE_OPERATIONS) {
        theme.nudgeBody.takeUnless { it.contains("busy", ignoreCase = true) }
    } else {
        theme.nudgeBody
    }
    return VisiblePulseNudge(theme.nudgeTitle, body, theme.nudgeCta)
}

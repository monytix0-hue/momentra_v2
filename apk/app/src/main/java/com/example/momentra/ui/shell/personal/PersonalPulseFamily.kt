package com.example.momentra.ui.shell.personal

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
        heroTitle = "WELLBEING SCORE",
        heroSubtitleFilled = "Your rhythm is building",
        heroSubtitleEmpty = "Awaiting first signals",
        // Figma 353:8893 hero chips
        heroMetrics = listOf("Pressure", "Recovery", "Discipline", "Attention"),
        // Figma metric tiles (2x2)
        tileLabels = listOf("Pressure", "Recovery", "Discipline", "Attention"),
        nudgeTitle = "Protect Recovery",
        nudgeBody = "Add a recovery block before your next busy stretch.",
        nudgeCta = "Log Recovery Now",
        moneyTitle = "MONEY SNAPSHOT",
        quickActions = listOf("Recovery", "Attention", "Mood", "Money", "Adjust"),
        heroStart = Color(0xFF7C5CFC),
        heroEnd = Color(0xFFA78BFA),
        accent = Color(0xFF7C5CFC),
    )
    PersonalPulseFamily.FUTURE_BUILDING -> PersonalPulseFamilyTheme(
        heroTitle = "FUTURE SCORE",
        heroSubtitleFilled = "Your trajectory is strong",
        heroSubtitleEmpty = "Awaiting first signals",
        heroMetrics = listOf("Vision", "Growth", "Momentum", "Discipline"),
        tileLabels = listOf("Vision", "Growth", "Momentum", "Discipline"),
        nudgeTitle = "Accelerate Growth",
        nudgeBody = "Log a milestone to keep momentum compounding.",
        nudgeCta = "Log Milestone",
        moneyTitle = "INVESTMENT SNAPSHOT",
        quickActions = listOf("Milestone", "Opportunity", "Pivot", "Progress", "Learning"),
        // SCREEN_STALE fix (G8): align with MomentThemes emerald, not purple Pulse leftover.
        heroStart = Color(0xFF10B981),
        heroEnd = Color(0xFF34D399),
        accent = Color(0xFF10B981),
    )
    PersonalPulseFamily.LIFESTYLE -> PersonalPulseFamilyTheme(
        // Figma `505:12365` — VITALITY INDEX axes: Joy / Fulfillment / Vitality / Exploration
        heroTitle = "VITALITY INDEX",
        heroSubtitleFilled = "Network stability · Flourishing",
        heroSubtitleEmpty = "Awaiting first signals",
        heroMetrics = listOf("Joy", "Fulfillment", "Vitality", "Exploration"),
        tileLabels = listOf("Joy", "Fulfillment", "Vitality", "Exploration"),
        nudgeTitle = "Protect a ritual",
        nudgeBody = "Log one experience to protect your lifestyle rhythm.",
        nudgeCta = "Log Experience",
        moneyTitle = "LIFESTYLE SPEND",
        quickActions = listOf("Experience", "Wellbeing", "Discovery", "Create", "Adjust"),
        heroStart = Color(0xFF0EA5A4),
        heroEnd = Color(0xFF7C5CFC),
        accent = Color(0xFF7C5CFC),
    )
    PersonalPulseFamily.RELATIONSHIPS -> PersonalPulseFamilyTheme(
        // Figma `505:11793` — BOND INDEX axes include Trust / Care (+ Support / Presence)
        heroTitle = "BOND INDEX",
        heroSubtitleFilled = "Stable and deepening",
        heroSubtitleEmpty = "Awaiting first signals",
        heroMetrics = listOf("Trust", "Care", "Support", "Presence"),
        tileLabels = listOf("Trust", "Care", "Support", "Presence"),
        nudgeTitle = "Protect Connection",
        nudgeBody = "Log a connection before the next busy stretch.",
        nudgeCta = "Log Connection",
        moneyTitle = "SHARED SPEND",
        quickActions = listOf("Connection", "Shared", "Investment", "Support", "Adjust"),
        heroStart = Color(0xFFE91E63),
        heroEnd = Color(0xFFA78BFA),
        accent = Color(0xFFE12A9E),
    )
}
fun PersonalPulseFamilyTheme.heroBrush(): Brush =
    Brush.horizontalGradient(listOf(heroStart, heroEnd))

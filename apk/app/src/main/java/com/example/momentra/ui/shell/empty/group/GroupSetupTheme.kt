package com.example.momentra.ui.shell.empty.group

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

data class GroupTypePalette(
    val accent: Color,
    val accentLight: Color,
    val accentGradient: Brush,
    val stepGlow: Color,
    val organizerRoleBg: Color,
    val organizerRoleBorder: Color,
)

/** Figma 575:9917 Group setup wizard theme — separate from Group empty-state (Ge*) tokens. */
object GroupSetupTheme {
    val Bg = Color(0xFF14121B)
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFF9CA3AF)
    val Card = Color(0xFF1C1926)
    val Border = Color(0xFF2A2538)
    val IconSurface = Color(0xFF2A2538)
    val CtaText = Color(0xFF14121B)
    val StepInactiveBg = Color(0xFF14121B)
    val MemberRoleBg = Border.copy(alpha = 0.1f)
    val MemberRoleBorder = Border.copy(alpha = 0.2f)
    val TypeIconBg = Color.White.copy(alpha = 0.1f)

    val TripAccent = Color(0xFFE8744F)
    val TripAccentLight = Color(0xFFFF8E63)
    val WeddingAccent = Color(0xFFEC4899)
    val WeddingAccentLight = Color(0xFFF472B6)
    val PartyAccent = Color(0xFF3B82F6)
    val PartyAccentLight = Color(0xFF60A5FA)
    val OutingAccent = Color(0xFF14B8A6)
    val OutingAccentLight = Color(0xFF2DD4BF)

    val TripGradient = Brush.horizontalGradient(listOf(TripAccent, TripAccentLight))
    val WeddingGradient = Brush.horizontalGradient(listOf(WeddingAccent, WeddingAccentLight))
    val PartyGradient = Brush.horizontalGradient(listOf(PartyAccent, PartyAccentLight))
    val OutingGradient = Brush.horizontalGradient(listOf(OutingAccent, OutingAccentLight))

    val tripPalette = palette(TripAccent, TripAccentLight, TripGradient)
    val weddingPalette = palette(WeddingAccent, WeddingAccentLight, WeddingGradient)
    val partyPalette = palette(PartyAccent, PartyAccentLight, PartyGradient)
    val outingPalette = palette(OutingAccent, OutingAccentLight, OutingGradient)

    fun paletteForOption(option: GroupTypeOption): GroupTypePalette = option.palette

    private fun palette(accent: Color, accentLight: Color, gradient: Brush) = GroupTypePalette(
        accent = accent,
        accentLight = accentLight,
        accentGradient = gradient,
        stepGlow = accent.copy(alpha = 0.4f),
        organizerRoleBg = accent.copy(alpha = 0.1f),
        organizerRoleBorder = accent.copy(alpha = 0.2f),
    )
}

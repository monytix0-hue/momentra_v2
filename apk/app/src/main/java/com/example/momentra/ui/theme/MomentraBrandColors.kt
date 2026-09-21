package com.example.momentra.ui.theme

import androidx.compose.ui.graphics.Color

/** Global brand tokens from `design/momentra_theme.css` v1.1. */
object MomentraBrandColors {
    // Indigo scale
    val Indigo50 = Color(0xFFEEE9FF)
    val Indigo100 = Color(0xFFC4BDEE)
    val Indigo300 = Color(0xFF8C83D4)
    val Indigo500 = Color(0xFF4B3EA8)
    val Indigo700 = Color(0xFF2D1F5E)
    val Indigo900 = Color(0xFF1A0F3D)

    // Ember scale
    val Ember50 = Color(0xFFFFF0E8)
    val Ember300 = Color(0xFFF59060)
    val Ember500 = Color(0xFFE8621A)
    val Ember700 = Color(0xFFA8390A)

    // Amber scale
    val Amber50 = Color(0xFFFEF8EC)
    val Amber500 = Color(0xFFF5A623)
    val Amber700 = Color(0xFFA86800)

    // Teal scale
    val Teal50 = Color(0xFFE6F6F1)
    val Teal500 = Color(0xFF1D9E75)
    val Teal700 = Color(0xFF0A6640)

    // Semantic
    val Brand = Indigo700
    val Cta = Ember500
    val Progress = Amber500
    val Safe = Teal500
    val TextOnDark = Color(0xFFF5F0FF)
    val TextOnEmber = Color(0xFFFFF5F0)

    // Light surfaces
    val BackgroundPrimary = Color(0xFFFFFFFF)
    val BackgroundSecondary = Color(0xFFF7F5FC)
    val TextPrimary = Indigo900
    val TextSecondary = Color(0xFF3C3489)

    // Moment Story editorial (Figma cream + purple accent)
    val StoryCream = Color(0xFFF7F4EE)
    val StoryCreamBright = Color(0xFFFFFEFB)
    val StoryInk = Color(0xFF25231F)
    val StoryMuted = Color(0xFF746F67)
    val StoryTerracotta = Color(0xFFB45F3D)
    val StoryAccentPurple = Color(0xFF6C4EF2)
    val StoryDarkClose = Color(0xFF201E28)
    val StoryPeach = Color(0xFFF4E5DA)

    // Component
    val MomentCardBg = Indigo700
    val MomentCardOnBrandBg = Indigo500
    val MomentCardAccent = Ember500
}

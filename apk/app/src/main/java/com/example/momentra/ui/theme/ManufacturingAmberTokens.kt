package com.example.momentra.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Manufacturing "Amber / Zinc" design system from Figma MFG frames
 * (MFG-PULSE-v12 / QuickAdds sheets · Forge & Form).
 *
 * Source: `design/tokens/business.tokens.json` → `palettes.manufacturing`.
 * Do not hard-code RetailRose or ServicesTeal into MFG sheets.
 */
object ManufacturingAmber {
    // ---- Screen -----------------------------------------------------------------
    val canvas = Color(0xFF18181B)
    val canvasTop = Color(0xFF3F3F46)
    val canvasDeep = Color(0xFF09090B)
    val screenTop = canvasTop
    val screenMid = canvas
    val screenBottom = canvasDeep

    // ---- Surfaces ---------------------------------------------------------------
    val surface = Color(0xFF27272A)
    val card = surface
    val cardSlate = Color(0xFF27272A)
    val surfaceRow = Color(0xFF18181B)
    val inset = Color(0xFF18181B)
    val field = inset
    val chip = Color(0xFF3F3F46)
    val tile = Color(0xFF27272A)
    val kpiOnSlate = Color(0xFF000000).copy(alpha = 0.28f)

    // ---- Accents ----------------------------------------------------------------
    val accent = Color(0xFFF59E0B)
    val amber = accent
    val accentSoft = Color(0xFFFBBF24)
    val amberSoft = accentSoft
    val accentDeep = Color(0xFFB45309)
    val onAccent = Color(0xFF1C1917)
    val grab = accentSoft
    val heroStart = Color(0xFFF59E0B)
    val heroEnd = Color(0xFFB45309)
    val cyan = Color(0xFF06B6D4)
    val positive = Color(0xFF22C55E)
    val negative = Color(0xFFEF4444)
    val warning = Color(0xFFFBBF24)
    val indigo = Color(0xFF4F46E5)
    /** Why-note cream body — warm amber wash for MFG. */
    val whyBody = Color(0xFFFEF3C7)

    // ---- Text -------------------------------------------------------------------
    val textPrimary = Color(0xFFFFFFFF)
    val textBody = Color(0xFFA1A1AA)
    val textMuted = Color(0xFFA1A1AA)
    val textFaint = Color(0xFF71717A)
    val inkOnAmber = onAccent

    // ---- Hairlines --------------------------------------------------------------
    val cardStroke = accent.copy(alpha = 0.28f)
    val cardStrokeStrong = accent.copy(alpha = 0.40f)
    val cardStrokeHeavy = accent.copy(alpha = 0.45f)
    val divider = canvasTop.copy(alpha = 0.6f)
    val chipStroke = Color.White.copy(alpha = 0.06f)
    val tileStroke = accent.copy(alpha = 0.35f)
    val accentBorder = accentDeep
    /** Unselected chip stroke — Figma zinc `#52525B`. */
    val chipBorder = Color(0xFF52525B)

    // ---- Radii ------------------------------------------------------------------
    val rPill = RoundedCornerShape(999.dp)
    val rHero = RoundedCornerShape(24.dp)
    val rCard = RoundedCornerShape(20.dp)
    val rCardTight = RoundedCornerShape(16.dp)
    val rTile = RoundedCornerShape(16.dp)
    val rMetric = RoundedCornerShape(14.dp)
    val rRow = RoundedCornerShape(12.dp)
    val rInner = RoundedCornerShape(12.dp)
    val rHeroIcon = RoundedCornerShape(12.dp)
    val rStoryCta = RoundedCornerShape(18.dp)
    val rSheetTop = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    val rCta = RoundedCornerShape(16.dp)

    // ---- Spacing ----------------------------------------------------------------
    val s2 = 2.dp
    val s4 = 4.dp
    val s6 = 6.dp
    val s8 = 8.dp
    val s10 = 10.dp
    val s12 = 12.dp
    val s14 = 14.dp
    val s16 = 16.dp
    val s20 = 20.dp
    val gutter = 20.dp
    val sectionGap = 16.dp
    val cardGap = 14.dp

    // ---- Typography -------------------------------------------------------------
    fun screenTitle() = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        color = textPrimary,
    )

    fun roleLine() = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 15.sp,
        color = accentSoft,
    )

    fun heroMetric() = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        color = textPrimary,
    )

    fun sectionTitle() = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 20.sp,
        color = textPrimary,
    )

    fun metricValue(color: Color = textPrimary) = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 23.sp,
        color = color,
    )

    fun metricLabel() = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        color = textBody.copy(alpha = 0.62f),
    )

    fun rowTitle() = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 16.sp,
        color = textPrimary,
    )

    fun rowCaption(color: Color = textBody) = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        color = color,
    )

    fun rowValue(color: Color = textPrimary) = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        lineHeight = 16.sp,
        color = color,
    )

    fun chipLabel(filled: Boolean) = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = if (filled) FontWeight.Bold else FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 15.sp,
        color = if (filled) textPrimary else textBody,
    )

    fun severity() = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Bold,
        fontSize = 10.sp,
        lineHeight = 13.sp,
    )

    fun viewAll() = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        lineHeight = 15.sp,
        color = accentSoft,
    )

    fun why() = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        color = Color.White.copy(alpha = 0.5f),
        textAlign = TextAlign.Start,
    )

    fun heroEyebrow() = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        color = accentSoft,
    )

    fun heroHeadline() = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        color = textPrimary,
    )

    fun heroBody() = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 15.sp,
        color = whyBody,
    )

    fun tileLabel() = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        color = textPrimary,
    )

    fun tileCaption() = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp,
        lineHeight = 13.sp,
        color = textMuted,
    )

    fun storyCtaTitle() = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 15.sp,
        lineHeight = 19.sp,
        color = inkOnAmber,
    )

    fun storyCtaCaption() = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        color = inkOnAmber.copy(alpha = 0.8f),
    )

    fun boardEyebrow() = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 10.sp,
        lineHeight = 13.sp,
        letterSpacing = 1.2.sp,
        color = accent,
    )

    fun boardHeadline(size: Int = 22) = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.ExtraBold,
        fontSize = size.sp,
        lineHeight = (size * 1.26f).sp,
        color = textPrimary,
    )

    fun boardChip() = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
    )

    fun kpiBig(color: Color = textBody) = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 23.sp,
        color = color,
    )

    fun kpiBigLabel() = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        color = Color.White.copy(alpha = 0.6f),
    )

    fun tone(color: Color) = color.copy(alpha = 0.16f)

    fun toneStroke(color: Color) = color.copy(alpha = 0.45f)
}

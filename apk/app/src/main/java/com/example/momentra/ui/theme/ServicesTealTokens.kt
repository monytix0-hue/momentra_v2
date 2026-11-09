package com.example.momentra.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Consumer Service "Teal / Lime" design system from Figma `2487:43413`
 * (SVC-PULSE-v12 / SVC-HUB-v5 / SVC-MOMENTS-v12 / SVC-LIFE-v12 / SVC-MEMORY-v12 / SVC-SETUP-v6).
 *
 * Domain services tokens: accent `#0D9488`, soft `#84CC16`, canvas `#042F2E`.
 */
object ServicesTeal {
    val screenTop = Color(0xFF115E59)
    val screenMid = Color(0xFF042F2E)
    val screenBottom = Color(0xFF022C22)

    val heroStart = Color(0xFF0D9488)
    val heroEnd = Color(0xFF84CC16)

    val card = Color(0xFF134E4A)
    val cardSlate = Color(0xFF134E4A)
    val surfaceRow = Color(0xFF0B2F2C)
    val surfaceWell = Color(0xFF042F2E)
    val sheet = Color(0xFF042F2E)
    val field = Color(0xFF042F2E)
    val chip = Color(0xFF115E59)
    val tile = Color(0xFF134E4A)
    val kpiOnSlate = Color(0xFF000000).copy(alpha = 0.28f)
    val tealMuted = Color(0xFF5EEAD4)
    val tealSoft = Color(0xFFCCFBF1)

    val teal = Color(0xFF0D9488)
    val lime = Color(0xFF84CC16)
    val tealDeep = Color(0xFF115E59)
    val cyan = Color(0xFF06B6D4)
    val amber = Color(0xFFFBBF24)
    val positive = Color(0xFF22C55E)
    val negative = Color(0xFFEF4444)
    val warning = Color(0xFFF59E0B)

    val textPrimary = Color(0xFFF0FDFA)
    val textBody = Color(0xFF99F6E4)
    val textMuted = Color(0xFF5EEAD4)
    val textFaint = Color(0xFF64748B)
    val inkOnTeal = Color(0xFF042F2E)

    val cardStroke = Color(0xFF0D9488).copy(alpha = 0.35f)
    val cardStrokeStrong = Color(0xFF0D9488).copy(alpha = 0.50f)
    val divider = Color(0xFF115E59)
    val hairline = Color.White.copy(alpha = 0.06f)
    val tileStroke = Color(0xFF0D9488)

    val rPill = RoundedCornerShape(999.dp)
    val rHero = RoundedCornerShape(24.dp)
    val rCard = RoundedCornerShape(20.dp)
    val rTile = RoundedCornerShape(16.dp)
    val rRow = RoundedCornerShape(12.dp)
    val rCardTight = RoundedCornerShape(16.dp)
    val rMetric = RoundedCornerShape(14.dp)
    val rInner = RoundedCornerShape(12.dp)
    val rHeroIcon = RoundedCornerShape(12.dp)
    val rStoryCta = RoundedCornerShape(18.dp)
    val chipStroke = Color.White.copy(alpha = 0.06f)

    val gutter = 16.dp
    val sectionGap = 16.dp
    val cardGap = 14.dp

    fun heroTitle() = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Black,
        fontSize = 28.sp,
        color = textPrimary,
    )

    fun sectionTitle() = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        color = textPrimary,
    )

    fun body() = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        color = textMuted,
    )

    fun eyebrow() = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        color = lime,
        letterSpacing = 1.2.sp,
    )

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
        color = lime,
    )

    fun heroMetric() = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
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
        color = lime,
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
        color = tealMuted,
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
        color = tealSoft,
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
        color = inkOnTeal,
    )

    fun storyCtaCaption() = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        color = inkOnTeal.copy(alpha = 0.8f),
    )

    fun boardEyebrow() = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 10.sp,
        lineHeight = 13.sp,
        letterSpacing = 1.2.sp,
        color = teal,
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

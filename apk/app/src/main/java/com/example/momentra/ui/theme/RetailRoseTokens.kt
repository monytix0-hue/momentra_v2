package com.example.momentra.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Retail "Rose / Maroon" design system extracted from Figma `2487:37758`
 * (RTL-PULSE-v12 / RTL-HUB-v5 / RTL-MOMENTS-v12 / RTL-LIFE-v12 / RTL-MEMORY-v12).
 *
 * This replaces the former `RetailStoreChrome` (dark blue `#0B1020` + green `#34D399`).
 * Every value below is taken from the Figma frames — do not round it to a "close" color.
 */
object RetailRose {
    // ---- Screen -----------------------------------------------------------------
    /** Vertical 3-stop screen wash: `#4c0519` → `#0b0a1f` (35%) → `#050414`. */
    val screenTop = Color(0xFF4C0519)
    val screenMid = Color(0xFF0B0A1F)
    val screenBottom = Color(0xFF050414)

    // ---- Surfaces ---------------------------------------------------------------
    /** Nested/inset card surface (Pulse cards use `#2a1a1f`). */
    val card = Color(0xFF2A1A1F)
    /** Rows and metric chips sit on the deeper `#170d11`. */
    val surfaceRow = Color(0xFF170D11)
    /** Moment/Life/Memory cards and Quick-Add tiles use `#1e293b`. */
    val cardSlate = Color(0xFF1E293B)
    /** Context chips + nav chrome. */
    val chip = Color(0xFF1E293B)
    /** Field / header chrome. */
    val field = Color(0xFF0F172A)
    /** Tile background inside the Quick Add grid. */
    val tile = Color(0xFF1E293B)
    /** KPI tile sitting inside a slate card. */
    val kpiOnSlate = Color(0xFF000000).copy(alpha = 0.28f)

    // ---- Accents ----------------------------------------------------------------
    val rose = Color(0xFFF43F5E)
    val roseBorder = Color(0xFFE11D48)
    /** Selected register / context chip fill — Figma HTR `#881337`. */
    val roseDeep = Color(0xFF881337)
    val roseSoft = Color(0xFFFFE4E6)
    val roseMuted = Color(0xFFFECDD3)
    /** Why-note cream body — Figma HTR `#FEF3C7`. */
    val whyCream = Color(0xFFFEF3C7)
    /** Sheet grab handle — Figma `#FB7185`. */
    val grab = Color(0xFFFB7185)
    /** ALT / soft-rose pill fill — Figma `#FB7185`. */
    val rosePill = Color(0xFFFB7185)
    val cyan = Color(0xFF06B6D4)
    val amber = Color(0xFFFBBF24)
    val positive = Color(0xFF22C55E)
    val negative = Color(0xFFEF4444)
    val warning = Color(0xFFF59E0B)
    val indigo = Color(0xFF4F46E5)
    /** Why-note body — Figma POS uses rose-soft cream `#FFE4E6`. */
    val whyBody = Color(0xFFFFE4E6)

    // ---- Text -------------------------------------------------------------------
    val textPrimary = Color(0xFFF8FAFC)
    val textBody = Color(0xFFCBD5E1)
    val textMuted = Color(0xFF94A3B8)
    val textFaint = Color(0xFF64748B)
    /**
     * Dark ink on rose — used by RTL-FORM Supplier CTA (`2506:29612` / slate `#0F172A` family).
     * QA sticky CTAs (POS / money / HTR) use **white** on rose per Figma — see [RoseStickyFooter].
     */
    val inkOnRose = Color(0xFF04110C)
    /** Placeholder / scan field hint — Figma `#FB7185`. */
    val textRoseHint = Color(0xFFFB7185)

    // ---- Hairlines --------------------------------------------------------------
    /** Card border: rose at 28% (Pulse) / 40% (Moments) / 45–50% (Life, Memory). */
    val cardStroke = Color(0xFFF43F5E).copy(alpha = 0.28f)
    val cardStrokeStrong = Color(0xFFF43F5E).copy(alpha = 0.40f)
    val cardStrokeHeavy = Color(0xFFF43F5E).copy(alpha = 0.45f)
    /** Row divider inside Pulse cards. */
    val divider = Color(0xFF4C0519).copy(alpha = 0.6f)
    /** Metric chip hairline: white at 6%. */
    val chipStroke = Color.White.copy(alpha = 0.06f)
    /** Tile hairline: rose at 35%. */
    val tileStroke = Color(0xFFF43F5E).copy(alpha = 0.35f)

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
    val rTileIcon = RoundedCornerShape(10.dp)
    val rStoryCta = RoundedCornerShape(18.dp)
    /** Bottom-sheet top corners — Figma ~28. */
    val rSheetTop = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    /** Primary sticky CTA — Figma 16. */
    val rCta = RoundedCornerShape(16.dp)

    // ---- Spacing (8pt-derived, exact Figma values) ------------------------------
    val s2 = 2.dp
    val s4 = 4.dp
    val s6 = 6.dp
    val s8 = 8.dp
    val s10 = 10.dp
    val s12 = 12.dp
    val s14 = 14.dp
    val s16 = 16.dp
    val s20 = 20.dp

    /** Screen gutter is 20 inside the 402pt frame. */
    val gutter = 20.dp
    val sectionGap = 16.dp
    /** Card-internal vertical rhythm. */
    val cardGap = 14.dp

    // ---- Typography (Plus Jakarta Sans) -----------------------------------------
    /** Screen title: 24 / Bold. */
    fun screenTitle() = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        color = textPrimary,
    )

    /** Amber role line under the screen title: 12 / Medium. */
    fun roleLine() = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 15.sp,
        color = amber,
    )

    /** Hero metric: 28 / SemiBold (legacy large numbers) or 22 / Bold in the design. */
    fun heroMetric() = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        color = textPrimary,
    )

    /** Card / section header: 16 / Bold. */
    fun sectionTitle() = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 20.sp,
        color = textPrimary,
    )

    /** Metric value: 18 / Bold. */
    fun metricValue(color: Color = textPrimary) = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 23.sp,
        color = color,
    )

    /** Metric label: 11 / Medium, 62% opacity. */
    fun metricLabel() = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        color = textBody.copy(alpha = 0.62f),
    )

    /** Row title: 13 / Medium. */
    fun rowTitle() = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 16.sp,
        color = textPrimary,
    )

    /** Row caption: 11 / Regular. */
    fun rowCaption(color: Color = textBody) = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        color = color,
    )

    /** Row trailing value: 13 / Bold. */
    fun rowValue(color: Color = textPrimary) = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        lineHeight = 16.sp,
        color = color,
    )

    /** Context chip label: 12 / Bold (filled) or 12 / Medium (outline). */
    fun chipLabel(filled: Boolean) = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = if (filled) FontWeight.Bold else FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 15.sp,
        color = if (filled) textPrimary else textBody,
    )

    /** Severity pill: 10 / Bold. */
    fun severity() = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Bold,
        fontSize = 10.sp,
        lineHeight = 13.sp,
    )

    /** View all affordance: 12 / Bold amber. */
    fun viewAll() = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        lineHeight = 15.sp,
        color = amber,
    )

    /** Why caption: 11, 50% opacity, white. */
    fun why() = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        color = Color.White.copy(alpha = 0.5f),
        textAlign = TextAlign.Start,
    )

    /** Hero eyebrow: 11 / Bold, rose-muted. */
    fun heroEyebrow() = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        color = roseMuted,
    )

    /** Hero headline: 22 / Bold. */
    fun heroHeadline() = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        color = textPrimary,
    )

    /** Hero body: 12 / Regular. */
    fun heroBody() = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 15.sp,
        color = roseSoft,
    )

    /** Quick Add tile label: 14 / Bold. */
    fun tileLabel() = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        color = textPrimary,
    )

    /** Quick Add tile caption: 10 / Regular, slate-400. */
    fun tileCaption() = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp,
        lineHeight = 13.sp,
        color = textMuted,
    )

    /** Story CTA title: 15 / ExtraBold. */
    fun storyCtaTitle() = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 15.sp,
        lineHeight = 19.sp,
        color = inkOnRose,
    )

    /** Story CTA caption: 11 / Medium. */
    fun storyCtaCaption() = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        color = inkOnRose.copy(alpha = 0.8f),
    )

    /** Board hero eyebrow (ACTIVE MOMENT / TODAY / MEMORY · THIS MONTH): 10 / SemiBold +1.2 tracking. */
    fun boardEyebrow() = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 10.sp,
        lineHeight = 13.sp,
        letterSpacing = 1.2.sp,
        color = rose,
    )

    /** Board hero headline: 22–24 / ExtraBold. */
    fun boardHeadline(size: Int = 22) = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.ExtraBold,
        fontSize = size.sp,
        lineHeight = (size * 1.26f).sp,
        color = textPrimary,
    )

    /** Small status chip inside the board hero (Retail / Store open / vs Oct): 11 / SemiBold. */
    fun boardChip() = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
    )

    /** Big KPI inside a slate card: 18 / Bold (design uses it for "Partial receive 40/60"). */
    fun kpiBig(color: Color = textBody) = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 23.sp,
        color = color,
    )

    /** Big KPI label: 11 / Medium white 60%. */
    fun kpiBigLabel() = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        color = Color.White.copy(alpha = 0.6f),
    )

    /** Tonal (16%) fill used by severity pills and hero chips. */
    fun tone(color: Color) = color.copy(alpha = 0.16f)

    /** Tonal hairline (45%) used by severity pills. */
    fun toneStroke(color: Color) = color.copy(alpha = 0.45f)
}

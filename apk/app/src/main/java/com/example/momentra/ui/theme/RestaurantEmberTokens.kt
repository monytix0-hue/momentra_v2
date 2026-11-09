package com.example.momentra.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Restaurant "Ember / Stone" design system extracted from Figma `2487:37611`
 * (`RST-PULSE-v12` `2400:29655`, `RST-HUB-v5` `2400:29892`, `RST-MOMENTS-v12` `2400:30664`,
 * `RST-LIFE-v12` `2400:30824`, `RST-MEMORY-v12` `2400:30984`, `RST-SETUP-v6` `2470:29187`).
 *
 * This replaces the former stone-green chrome (`#101412` + `#F59E0B`).
 * Every value below is taken from the Figma frames — do not round it to a "close" colour.
 */
object RestaurantEmber {
    // ---- Screen -----------------------------------------------------------------
    /** Vertical 3-stop screen wash: `#44403c` → `#1c1917` (35%) → `#0c0a09`. */
    val screenTop = Color(0xFF44403C)
    val screenMid = Color(0xFF1C1917)
    val screenBottom = Color(0xFF0C0A09)

    /** Hero gradient `#881337` → `#EA580C` (Pulse / Moments / Life / Memory / setup CTA). */
    val heroStart = Color(0xFF881337)
    val heroEnd = Color(0xFFEA580C)

    // ---- Surfaces ---------------------------------------------------------------
    /** Pulse and board card surface (`#292524`, radius 20). */
    val card = Color(0xFF292524)
    /** Inset well: health-ring hero, metric chips, list-row icon wells (`#1c1917`). */
    val surfaceWell = Color(0xFF1C1917)
    /** Sheet and field chrome — every `RST-QA-*` and `RST-SHEET-*` root (`#1c1917`). */
    val sheet = Color(0xFF1C1917)
    /** Field / input background inside a sheet. */
    val field = Color(0xFF1C1917)
    /** Context chips on the non-filled state (`#44403c`). */
    val chip = Color(0xFF44403C)
    /** Quick Add tile background (`#292524`). */
    val tile = Color(0xFF292524)
    /** Nav / header chrome. */
    val chrome = Color(0xFF1C1917)

    // ---- Accents ----------------------------------------------------------------
    /** Primary ember `#EA580C` — tile borders, icon wells, ring progress, `+ Quick Adds`. */
    val ember = Color(0xFFEA580C)
    /** Gold `#FCD34D` — metric values, `View all ›`, hero status line. */
    val gold = Color(0xFFFCD34D)
    /** Wine `#881337` — filled context chip and hero gradient start. */
    val wine = Color(0xFF881337)
    /** Soft hero text tints from `2400:29655 hero`. */
    val heroEyebrowTint = Color(0xFFFEF3C7)
    val heroBodyTint = Color(0xFFFDE68A)
    val amber = Color(0xFFFBBF24)
    val cyan = Color(0xFF06B6D4)
    val positive = Color(0xFF22C55E)
    val negative = Color(0xFFEF4444)
    val warning = Color(0xFFF59E0B)

    // ---- Text -------------------------------------------------------------------
    val textPrimary = Color(0xFFFAFAF9)
    val textBody = Color(0xFFA8A29E)
    val textMuted = Color(0xFF94A3B8)
    val textFaint = Color(0xFF78716C)
    /** Ink used on gold / ember filled controls. */
    val inkOnEmber = Color(0xFF1C1917)

    // ---- Hairlines --------------------------------------------------------------
    /** Card border: white at 6% inside cards. */
    val cardStroke = Color.White.copy(alpha = 0.06f)
    /** Row divider inside Pulse cards (`#44403c`). */
    val divider = Color(0xFF44403C)
    /** Faint hairline (`#57534e`). */
    val hairline = Color(0xFF57534E)
    /** Tile hairline: solid ember (`#ea580c`). */
    val tileStroke = Color(0xFFEA580C)
    val chipStroke = Color(0xFF44403C)

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

    /** Gold role line under the screen title: 12 / Medium. */
    fun roleLine() = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 15.sp,
        color = gold,
    )

    /** Hero metric: 22 / Bold. */
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
        color = if (filled) inkOnEmber else textBody,
    )

    /** Severity pill: 10 / Bold. */
    fun severity() = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Bold,
        fontSize = 10.sp,
        lineHeight = 13.sp,
    )

    /** View all affordance: 12 / Bold gold. */
    fun viewAll() = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        lineHeight = 15.sp,
        color = gold,
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

    /** Hero eyebrow: 11 / Bold, `#fef3c7`. */
    fun heroEyebrow() = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        color = heroEyebrowTint,
    )

    /** Hero headline: 22 / Bold. */
    fun heroHeadline() = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        color = Color.White,
    )

    /** Hero body: 12 / Regular, `#fde68a`. */
    fun heroBody() = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 15.sp,
        color = heroBodyTint,
    )

    /** Quick Add tile label: 13 / SemiBold (frame uses 13). */
    fun tileLabel() = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 17.sp,
        color = textPrimary,
    )

    /** Quick Add tile caption: 10 / Regular, `#a8a29e`. */
    fun tileCaption() = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp,
        lineHeight = 13.sp,
        color = textBody,
    )

    /** Story CTA title: 15 / ExtraBold. */
    fun storyCtaTitle() = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 15.sp,
        lineHeight = 19.sp,
        color = inkOnEmber,
    )

    /** Story CTA caption: 11 / Medium. */
    fun storyCtaCaption() = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        color = inkOnEmber.copy(alpha = 0.8f),
    )

    /** Board hero eyebrow (ACTIVE MOMENT / TODAY / MEMORY · THIS MONTH): 10 / SemiBold +1.2 tracking. */
    fun boardEyebrow() = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 10.sp,
        lineHeight = 13.sp,
        letterSpacing = 1.2.sp,
        color = ember,
    )

    /** Board hero headline: 22–24 / ExtraBold. */
    fun boardHeadline(size: Int = 22) = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.ExtraBold,
        fontSize = size.sp,
        lineHeight = (size * 1.26f).sp,
        color = textPrimary,
    )

    /** Small status chip inside the board hero: 11 / SemiBold. */
    fun boardChip() = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
    )

    /** Big KPI inside a card: 18 / Bold. */
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

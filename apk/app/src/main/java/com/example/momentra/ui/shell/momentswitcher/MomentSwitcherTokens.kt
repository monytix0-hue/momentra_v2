package com.example.momentra.ui.shell.momentswitcher

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Tokens for the Figma `MomentSwitcher` frame (`2678:37791`) — the Group and Business moment
 * switcher overlay.
 *
 * These are read from the five state frames directly:
 * `2678:37481` Ongoing, `2678:37561` Completed, `2678:37634` Empty, `2678:37715` SearchEmpty,
 * `2678:37790` LoadError.
 *
 * Deliberately *not* folded into [com.example.momentra.ui.shell.business.appstate.BusinessAppStateTokens]:
 * that kit backs a different Figma family (`2852:39645`) whose values are asserted by
 * `BusinessAppStateSpecsTest`, and the switcher now serves Group too.
 *
 * Keep in lockstep with `MomentSwitcherTokens.swift` on iOS.
 */
object MomentSwitcherTokens {
    // Canvas
    /** Frame root fill — `#12151C`. */
    val Page = Color(0xFF12151C)
    /** The `wash` ellipse: emerald at 12%, 360dp, gaussian blur 50, pegged to (-120, -80). */
    val Wash = Color(0xFF10B981)
    val WashAlpha = 0.12f
    val WashSize = 360.dp
    val WashBlur = 50.dp
    val WashOffsetX = (-120).dp
    val WashOffsetY = (-80).dp

    // Glass
    /** White @ 8% — state cards, metric tiles, search field. */
    val GlassFill = Color(0x14FFFFFF)
    /** White @ 14% — state card / search stroke. */
    val GlassStroke = Color(0x24FFFFFF)
    /** White @ 6% — tab container fill. */
    val SheetFill = Color(0x0FFFFFFF)
    /** White @ 12% — tab container stroke. */
    val SheetStroke = Color(0x1FFFFFFF)
    /** White @ 10% fill / 20% stroke — the 40dp close button. */
    val IconButtonFill = Color(0x1AFFFFFF)
    val IconButtonStroke = Color(0x33FFFFFF)

    // Accent
    /** `appstate/accent` collapsed to emerald. */
    val Accent = Color(0xFF10B981)
    /** Selected tab gradient: emerald @10% -> violet @33%. */
    val TabStart = Color(0x1A10B981)
    val TabEnd = Color(0x548B5CF6)
    /** `#00E599` — the LIVE NOW dot and label. */
    val LiveDot = Color(0xFF00E599)
    /** `#F5A623` — the Categories metric and the Story pill ring. */
    val Warm = Color(0xFFF5A623)
    /** Amber @ 28% — the Categories metric stroke. */
    val WarmStroke = Color(0x47F5A623)
    /** `#FF5C38` — the error card stroke and badge. */
    val Error = Color(0xFFFF5C38)
    /** Error card stroke: `#FF5C38` @ 30%. */
    val ErrorStroke = Color(0x4DFF5C38)
    /** Diagnostic pill fill/stroke. */
    val ErrorPillFill = Color(0x24FF5C38)
    val ErrorPillStroke = Color(0x66FF5C38)

    // Gradients
    /** `chip · All` and every primary CTA: `#E8621A` -> `#F5A623`. */
    val EmberGradient = listOf(Color(0xFFE8621A), Color(0xFFF5A623))
    /**
     * `Live card` fill: `#312E81` at the top, `rgba(14,124,102,0.3)` at 55%, and
     * `rgba(16,185,129,0.3)` at the bottom.
     */
    val LiveCardGradient = listOf(
        Color(0xFF312E81),
        Color(0x4D0E7C66),
        Color(0x4D10B981),
    )
    val LiveCardStops = listOf(0f, 0.55f, 1f)
    /** Unselected bucket chip: white @ 12% fill, white @ 22% stroke. */
    val ChipFill = Color(0x1FFFFFFF)
    val ChipStroke = Color(0x38FFFFFF)
    /** LIVE NOW chip: black @ 25% fill, white @ 20% stroke. */
    val LiveChipFill = Color(0x40000000)
    val LiveChipStroke = Color(0x33FFFFFF)
    /** Story pill: white @ 16% fill. */
    val StoryPillFill = Color(0x29FFFFFF)
    /** Clear-search pill: white @ 14% fill. */
    val ClearPillFill = Color(0x24FFFFFF)

    // Text
    val Text = Color(0xFFFFFFFF)
    /** Headline body at 70% — subtitles, metric labels, empty-card body. */
    val TextMuted = Color(0xB3FFFFFF)
    /** Placeholder at 55%. */
    val TextSubtle = Color(0x8CFFFFFF)
    /** Unselected tab label at 55%. */
    val TabIdle = Color(0x8CFFFFFF)
    /** Card microcopy — `rgba(255,255,255,0.85)`. */
    val TextOnCard = Color(0xD9FFFFFF)

    // Geometry
    val ScreenPadding = 24.dp
    val BodyWidth = 354.dp
    val BodyTop = 56.dp
    val BodyGap = 16.dp
    val CardRadius = 22.dp
    val StateCardRadius = 32.dp
    val LiveCardRadius = 28.dp
    val MetricRadius = 20.dp
    val SearchRadius = 20.dp
    val PillRadius = 999.dp
    val CloseSize = 40.dp
    val CloseGlyphSize = 17.dp
    val AvatarSize = 40.dp
    val StoryAvatarSize = 28.dp
    val FilterIconSize = 12.dp
    val SearchIconSize = 19.dp
    val LiveDotSize = 8.dp
    val HeroSize = 136.dp
    val HeroSearchSize = 124.dp
    val HeroRingSize = 108.dp
    val HeroSearchRingSize = 96.dp
    val HeroInset = 14.dp
    val HeroMarkSize = 96.dp
    val HeroSearchMarkSize = 84.dp
    val HeroMarkInset = 20.dp
    val HeroBadgeSize = 40.dp
    val HeroBadgeInset = 92.dp
    val HeroSearchBadgeInset = 80.dp
    val HeroBadgeBorder = 3.dp
    val LiveCardShadowRadius = 32.dp
    val LiveCardShadowY = 10.dp
    val CtaShadowRadius = 20.dp
    val CtaShadowY = 6.dp

    // Type scale
    val HeadlineSize = 22f
    val SubtitleSize = 13f
    val TabLabelSize = 14f
    val MetricValueSize = 32f
    val MetricLabelSize = 12f
    val SearchTextSize = 14f
    val ChipLabelSize = 13f
    val EyebrowSize = 12f
    val EyebrowTracking = 2f
    val CardTitleSize = 26f
    val CardMetaSize = 13f
    val CardMicroSize = 12f
    val LiveChipSize = 11f
    val LiveChipTracking = 1.5f
    val StoryLabelSize = 14f
    val StoryGlyphSize = 10f
    val StateTitleSize = 24f
    val StateTitleSmallSize = 17f
    val StateBodySize = 14f
    val StateBodyLineHeight = 1.35f
    val FooterTitleSize = 15f
    val FooterBodySize = 12f
    val CtaLabelSize = 14f
    val CtaTracking = 1f
    val ErrorCodeSize = 11f
    val ErrorCodeTracking = 1.5f
    val ClearPillSize = 12f
    val FilterLabelSize = 12f
    val HeroBadgeGlyphSize = 18f

    // State muting (Figma `muted` group opacity)
    /** `Empty`: tabs + search muted to 55%. */
    val MutedEmpty = 0.55f
    /** `LoadError`: tabs + search muted to 45%. */
    val MutedError = 0.45f
}

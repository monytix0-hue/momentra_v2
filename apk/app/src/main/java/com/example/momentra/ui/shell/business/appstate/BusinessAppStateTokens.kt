package com.example.momentra.ui.shell.business.appstate

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Tokens for the Figma "App States · Shared (all Business verticals)" kit.
 *
 * Rebuilt against Figma "Business v3 · International Obsidian · Emerald Theme" (node 2852:39645).
 * That frame replaces the flat dark grey surface with a deep obsidian canvas, a volumetric
 * dual-orb glow mesh, high-refraction glass cards and floating neon indicator badges.
 *
 * Every accent-driven layer in the kit binds a single variable, `appstate/accent`, collapsed to one
 * emerald value shared by every Business vertical, so the whole kit reads as a single accent
 * system. These are the exact Figma values and are the source of truth for this layer — they
 * intentionally do not follow [com.example.momentra.ui.shell.business.shared.BusinessActiveTheme].
 *
 * Keep in lockstep with `BusinessAppStateTokens.swift` on iOS.
 */
enum class BusinessAppStateAccent(val color: Color) {
    /** App States · Accent / Emerald — #10B981 (unified across every Business vertical) */
    Emerald(Color(0xFF10B981)),
    ;

    companion object {
        /**
         * Resolve the accent for an operating moment subtype code.
         *
         * The accent is unified, so every code resolves to emerald. The *vertical* still matters
         * for copy overrides — see [BusinessAppStateVertical].
         */
        fun forTypeCode(momentTypeCode: String?): BusinessAppStateAccent = Emerald
    }
}

/**
 * The business vertical a moment belongs to.
 *
 * The accent is unified across verticals, but the kit still carries per-vertical *text* overrides
 * (Figma "Hub label overrides"), so the vertical stays a first-class discriminator even though the
 * colour no longer varies.
 *
 * Keep in lockstep with `BusinessAppStateVertical.kt` on iOS.
 */
enum class BusinessAppStateVertical {
    Trading,
    Retail,
    Rst,
    Services,
    Mfg,
    Marketing,
    Distribution,
    ;

    companion object {
        /**
         * Resolve the vertical for an operating moment subtype code.
         *
         * Mirrors the Figma mode set exactly, so the same moment is classified identically on every
         * platform.
         */
        fun forTypeCode(momentTypeCode: String?): BusinessAppStateVertical =
            when (momentTypeCode?.trim()?.uppercase()) {
                "TRADING_WHOLESALE" -> Trading
                "DISTRIBUTION_DEALERSHIP" -> Distribution
                "RETAIL_STORE" -> Retail
                "RESTAURANT_FOOD" -> Rst
                "CONSUMER_SERVICE" -> Services
                "STANDARD_PRODUCTION", "MADE_TO_ORDER" -> Mfg
                "CLIENT_CAMPAIGN", "OWN_BUSINESS_CAMPAIGN" -> Marketing
                else -> Trading
            }
    }
}

/**
 * Surface ramp shared by every app-state variant.
 *
 * Figma "Obsidian & Neon Refraction": a deep `#05070B` spatial canvas, a volumetric dual-orb glow
 * mesh, `rgba(16,22,35,0.72)` refraction glass at a 1.25dp `rgba(16,185,129,0.35)` border, and the
 * emerald `#059669 → #34D399` CTA ramp. Every value here is read from the frame nodes directly.
 */
object BusinessAppStateTokens {
    // Palette primitives
    /** Figma emerald 500 — the kit's single accent. */
    val Emerald = Color(0xFF10B981)
    /** Figma emerald 600 — CTA start and the warm glow orb. */
    val EmeraldDeep = Color(0xFF059669)
    /** Figma emerald 400 — CTA end. */
    val EmeraldBright = Color(0xFF34D399)
    /** Figma error / diagnostic red. */
    val ErrorRing = Color(0xFFFF5C38)
    /** Figma amber, used for "Not recorded" / "Hidden until online" values. */
    val Warning = Color(0xFFF5A624)

    // Surfaces
    /** Figma root background for every App States frame (`#05070B`). */
    val Page = Color(0xFF05070B)
    /** Hero badge core fill (`#0F141F`). */
    val Sheet = Color(0xFF0F141F)
    val SurfaceRaised = Color(0xFF151D2E)
    val Border = Color(0xFF1E293B)

    // Text
    val Text = Color(0xFFFFFFFF)
    /** Body copy and muted subtitles (`#94A4B8`). */
    val TextMuted = Color(0xFF94A4B8)
    /** Footnoted microcopy (`#738099`). */
    val TextSubtle = Color(0xFF738099)
    /** Glass-card row labels (`#C7D1E0`). */
    val RowLabelColor = Color(0xFFC7D1E0)

    // Glass (backdrop-blurred chrome: icon buttons, dividers)
    val GlassFill = Color(0x14FFFFFF) // white @ 8%
    val GlassStroke = Color(0x38FFFFFF) // white @ 22%
    val GlassStrokeSoft = Color(0x24FFFFFF) // white @ 14%
    val BlurRadius = 24.dp

    /** Refraction glass card fill: `rgba(16,22,35,0.72)`. */
    val GlassCardFill = Color(0xFF101623).copy(alpha = 0.72f)
    /** Refraction glass card stroke: emerald @35%. */
    val GlassCardStroke = Color(0x5910B981)
    /** Figma draws the card border at `1.25px`. */
    val GlassCardStrokeWidth = 1.25.dp
    /** Small emerald hairline used by chips and tab containers. */
    val HairlineStroke = Color(0x2E10B981)

    // Primary CTA — the emerald gradient glow
    /** Figma CTA start (`#059669`). */
    val CtaStart = Color(0xFF059669)
    /** Figma CTA end (`#34D399`). */
    val CtaEnd = Color(0xFF34D399)
    /** Figma `0 4px 12px rgba(16,185,129,0.35)`. */
    val CtaShadow = Color(0xFF10B981)
    val CtaShadowRadius = 12.dp
    val CtaShadowY = 4.dp

    /** Secondary CTA: no fill on screen bodies, a 6% white wash inside the switcher sheet. */
    val SecondaryButtonFill = Color(0x00FFFFFF)
    val SecondaryButtonStroke = Color(0x2E10B981)

    // Ambient glow mesh
    /** Atmospheric accent orb behind the state body (`#10B981`). */
    val GlowAccent = Color(0xFF10B981)
    val GlowAccentSize = 360.dp
    /** Stop 0 alpha; the mesh fades to 8% at 60% and 0 at the rim. */
    val GlowAccentAlpha = 0.28f
    val GlowAccentMidAlpha = 0.08f
    val GlowAccentMidStop = 0.60f
    /** Warm secondary orb (`#059669`). */
    val GlowWarm = Color(0xFF059669)
    val GlowWarmSize = 280.dp
    val GlowWarmAlpha = 0.15f
    /** Error states swap the accent orb for `#FF5C38`. */
    val GlowError = Color(0xFFFF5C38)
    val GlowErrorAlpha = 0.25f
    /** Soft blur applied to each orb. */
    val GlowBlur = 120.dp

    // Hero indicator badge (Figma "Hero Indicator Badge", 112dp)
    val HeroSize = 112.dp
    /** Outer halo: 96 inset 8, emerald @8% fill, emerald @45% 2dp ring. */
    val HeroRingSize = 96.dp
    val HeroRingInset = 8.dp
    val HeroRingStroke = 2.dp
    val HeroRingFillAlpha = 0.08f
    val HeroRingStrokeAlpha = 0.45f
    /** Inner core: 76 inset 18, `#0F141F` fill, emerald @18% 1.5dp ring. */
    val HeroCoreSize = 76.dp
    val HeroCoreInset = 18.dp
    val HeroCoreStroke = 1.5.dp
    val HeroCoreFill = Color(0xFF0F141F)
    val HeroCoreStrokeAlpha = 0.18f
    /** Floating status dot: 36 at (72,72), state-coloured fill, emerald 3.5dp ring. */
    val HeroDotSize = 36.dp
    val HeroDotInset = 72.dp
    val HeroDotStroke = 3.5.dp
    /** Hero glyph inside the core, and the glyph inside the status dot. */
    val HeroGlyphSize = 28.dp
    val HeroDotGlyphSize = 17.sp
    val HeroDotGlyphColor = Color(0xFF080A0F)

    // Semantic
    val Danger = Color(0xFFEF4444)
    /** Diagnostic pill: red @15% fill, emerald @45% stroke, `#FF5C38` label. */
    val ErrorPillFill = Color(0x26FF5C38)
    val ErrorPillStroke = Color(0x7310B981)
    /** Offline status pill: `rgba(38,46,61,0.9)` fill, emerald @40% stroke. */
    val OfflineBannerFill = Color(0xFF262E3D).copy(alpha = 0.90f)
    val OfflineBannerStroke = Color(0x6610B981)
    val OfflineBannerHeight = 34.dp
    val OfflineGrey = Color(0xFF94A4B8)

    // Geometry
    val ScreenHorizontalPadding = 24.dp
    val BodyWidth = 354.dp
    val BodyGap = 12.dp
    val CardRadius = 22.dp
    val CardPaddingHorizontal = 20.dp
    val CardPaddingVertical = 18.dp
    val CardRowGap = 12.dp
    val ButtonRadius = 999.dp
    val PrimaryButtonHeight = 50.dp
    val SecondaryButtonHeight = 46.dp

    // Hub capture grid (Figma "Quick Adds Glass Grid")
    /** Six `111x67` tiles across the `354` body width: `3*111 + 2*10 = 353`. */
    val HubTileWidth = 111.dp
    val HubTileHeight = 67.dp
    val HubColumnGap = 10.dp
    val HubRowGap = 10.dp
    val HubTileRadius = 16.dp
    val HubTileFill = Color(0x0DFFFFFF) // white @ 5%
    /** Tile hairline: emerald @ 25%. */
    val HubTileStroke = Color(0x4010B981)
    val HubIconSize = 18.dp
    val HubLabelSize = 11.5f
    val HubTileLabelGap = 6.dp
    val HubTileVerticalPadding = 14.dp

    // Loading skeleton (Figma "Loading Glass Skeleton Box")
    val SkeletonBoxHeight = 150.dp
    val SkeletonPadHorizontal = 20.dp
    val SkeletonPadVertical = 22.dp
    val SkeletonGap = 14.dp
    val SkeletonBarHeight = 16.dp
    val SkeletonBarRadius = 8.dp
    /** Shimmer ramp: base → 18% at 50% → base. */
    val SkeletonBarBase = Color(0x0AFFFFFF) // white @ 4%
    val SkeletonBarHighlight = Color(0x2EFFFFFF) // white @ 18%
    val SkeletonTrailingSpace = 110.dp

    // Type scale
    val TitleSize = 24f
    val BodySize = 13.5f
    /** Figma body line height over the 13.5sp ramp. */
    val BodyLineHeight = 19f
    val EyebrowSize = 11f
    val RowLabelSize = 13f
    val RowValueSize = 13f
    val ButtonLabelSize = 13.5f
    val ErrorCodeSize = 11f
    val FootnoteSize = 11f
    val OfflineBannerLabelSize = 11.5f

    val EyebrowTracking = 1.5f
    val ButtonTracking = 1f
    val ErrorCodeTracking = 1.5f

    // Alphas still referenced by shared chrome
    /** Footnote alpha over [TextSubtle]. Figma draws footnotes solid, so this stays at 1. */
    val FootnoteMutedOpacity = 1f
}

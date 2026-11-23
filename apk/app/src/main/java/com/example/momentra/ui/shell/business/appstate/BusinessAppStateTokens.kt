package com.example.momentra.ui.shell.business.appstate

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.ui.theme.generated.BusinessTokens

/**
 * Tokens for the Figma "App States · Shared (all Business verticals)" kit.
 *
 * Obsidian Emerald (Figma rknf `2:67`). Palette primitives **alias** [BusinessTokens.Colors]
 * so AppState stays in lockstep with regenerated Business design tokens.
 *
 * Keep in lockstep with `BusinessAppStateTokens.swift` on iOS.
 */
enum class BusinessAppStateAccent(val color: Color) {
    /** System emerald / success chrome `#10B981` (not trading amber). */
    Emerald(BusinessTokens.Colors.SignalPositive),
    /** Trading / Distribution — TRD-v12 amber `#F59E0B`. */
    Amber(BusinessTokens.Colors.TRADINGAccent),
    /** Retail Empty (Rose) `#F43F5E`. */
    Rose(BusinessTokens.Colors.RETAILAccent),
    /** RST Food Empty (Flame) `#EA580C`. */
    Flame(BusinessTokens.Colors.RESTAURANTAccent),
    /** View All (Cyan) `#06B6D4`. */
    Cyan(BusinessTokens.Colors.SignalInfo),
    /** Client Campaign indigo `#6366F1`. */
    Indigo(BusinessTokens.Colors.MARKETINGAccent),
    ;

    companion object {
        /**
         * Resolve the accent for an operating moment subtype code.
         *
         * Trading → Emerald, Distribution → Flame, Retail → Rose, Restaurant → Flame;
         * Client Campaign → Indigo; Services/MFG/Own Marketing → Emerald unless they grow own accents.
         * ViewAll screens pass [Cyan] explicitly at the call site.
         */
        fun forTypeCode(momentTypeCode: String?): BusinessAppStateAccent =
            when (momentTypeCode?.trim()?.uppercase()) {
                "TRADING_WHOLESALE" -> Emerald
                "DISTRIBUTION_DEALERSHIP" -> Flame
                "RETAIL_STORE" -> Rose
                "RESTAURANT_FOOD" -> Flame
                "CLIENT_CAMPAIGN" -> Indigo
                else -> Emerald
            }
    }
}

/**
 * The business vertical a moment belongs to.
 *
 * Accent can vary (Rose / Flame / Emerald), and the kit still carries per-vertical *text*
 * overrides (Figma "Hub label overrides").
 *
 * Keep in lockstep with `BusinessAppStateVertical` on iOS.
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
 * Figma "Obsidian & Neon Refraction": deep `#05070B` canvas, refraction glass, emerald CTA ramp.
 * Color values alias [BusinessTokens] where a matching token exists.
 */
object BusinessAppStateTokens {
    // Palette primitives (alias BusinessTokens)
    /** System emerald — success toast / positive signals (stays `#10B981`). */
    val Emerald = BusinessTokens.Colors.SignalPositive
    val EmeraldDeep = BusinessTokens.Colors.CtaStart
    val EmeraldBright = BusinessTokens.Colors.CtaEnd
    /** Trading moment amber (TRD-v12). */
    val TradingAmber = BusinessTokens.Colors.TRADINGAccent
    val TradingFlame = BusinessTokens.Colors.TRADINGAccentDeep
    val ErrorRing = BusinessTokens.Colors.SignalDanger
    val Warning = BusinessTokens.Colors.SignalWarning

    // Surfaces
    val Page = BusinessTokens.Colors.BgCanvas
    val Sheet = Color(0xFF0F141F)
    val SurfaceRaised = Color(0xFF151D2E)
    val Border = BusinessTokens.Colors.BorderDefault

    // Text
    val Text = BusinessTokens.Colors.TextPrimary
    val TextMuted = BusinessTokens.Colors.TextMuted
    val TextSubtle = Color(0xFF738099)
    val RowLabelColor = BusinessTokens.Colors.TextSecondary

    // Glass
    val GlassFill = Color(0x14FFFFFF)
    val GlassStroke = Color(0x38FFFFFF)
    val GlassStrokeSoft = Color(0x24FFFFFF)
    val BlurRadius = 24.dp

    val GlassCardFill = BusinessTokens.Colors.SurfaceCard.copy(alpha = 0.72f)
    val GlassCardStroke = Emerald.copy(alpha = 0.35f)
    val GlassCardStrokeWidth = 1.25.dp
    val HairlineStroke = Emerald.copy(alpha = 0.18f)

    // Primary CTA
    val CtaStart = BusinessTokens.Colors.CtaStart
    val CtaEnd = BusinessTokens.Colors.CtaEnd
    val CtaShadow = Emerald
    val CtaShadowRadius = 12.dp
    val CtaShadowY = 4.dp

    val SecondaryButtonFill = Color(0x00FFFFFF)
    val SecondaryButtonStroke = Emerald.copy(alpha = 0.18f)

    // Ambient glow mesh
    val GlowAccent = Emerald
    val GlowAccentSize = 360.dp
    val GlowAccentAlpha = 0.28f
    val GlowAccentMidAlpha = 0.08f
    val GlowAccentMidStop = 0.60f
    val GlowWarm = EmeraldDeep
    val GlowWarmSize = 280.dp
    val GlowWarmAlpha = 0.15f
    val GlowError = ErrorRing
    val GlowErrorAlpha = 0.25f
    val GlowBlur = 120.dp

    // Hero indicator badge
    val HeroSize = 112.dp
    val HeroRingSize = 96.dp
    val HeroRingInset = 8.dp
    val HeroRingStroke = 2.dp
    val HeroRingFillAlpha = 0.08f
    val HeroRingStrokeAlpha = 0.45f
    val HeroCoreSize = 76.dp
    val HeroCoreInset = 18.dp
    val HeroCoreStroke = 1.5.dp
    val HeroCoreFill = Color(0xFF0F141F)
    val HeroCoreStrokeAlpha = 0.18f
    val HeroDotSize = 36.dp
    val HeroDotInset = 72.dp
    val HeroDotStroke = 3.5.dp
    val HeroGlyphSize = 28.dp
    val HeroDotGlyphSize = 17.sp
    val HeroDotGlyphColor = BusinessTokens.Colors.TextInverse

    // Semantic
    val Danger = Color(0xFFEF4444)
    val ErrorPillFill = ErrorRing.copy(alpha = 0.15f)
    val ErrorPillStroke = Emerald.copy(alpha = 0.45f)
    val OfflineBannerFill = Color(0xFF262E3D).copy(alpha = 0.90f)
    val OfflineBannerStroke = Emerald.copy(alpha = 0.40f)
    val OfflineBannerHeight = 34.dp
    val OfflineGrey = BusinessTokens.Colors.TextMuted

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

    // Hub capture grid
    val HubTileWidth = 111.dp
    val HubTileHeight = 67.dp
    val HubColumnGap = 10.dp
    val HubRowGap = 10.dp
    val HubTileRadius = 16.dp
    val HubTileFill = Color(0x0DFFFFFF)
    val HubTileStroke = Emerald.copy(alpha = 0.25f)
    val HubIconSize = 18.dp
    val HubLabelSize = 11.5f
    val HubTileLabelGap = 6.dp
    val HubTileVerticalPadding = 14.dp

    // Loading skeleton
    val SkeletonBoxHeight = 150.dp
    val SkeletonPadHorizontal = 20.dp
    val SkeletonPadVertical = 22.dp
    val SkeletonGap = 14.dp
    val SkeletonBarHeight = 16.dp
    val SkeletonBarRadius = 8.dp
    val SkeletonBarBase = Color(0x0AFFFFFF)
    val SkeletonBarHighlight = Color(0x2EFFFFFF)
    val SkeletonTrailingSpace = 110.dp

    // Type scale
    val TitleSize = 24f
    val BodySize = 13.5f
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

    val FootnoteMutedOpacity = 1f

    // Toaster / banner system (Figma 2:1375)
    val ToastRadius = 14.dp
    val ToastPadHorizontal = 14.dp
    val ToastPadVertical = 12.dp
    val ToastLabelSize = 12.5f
    val ToastTitleSize = 13.5f
    val ToastSubtitleSize = 11.5f
    val SetupActivatedFill = Color(0xFF16120E).copy(alpha = 0.96f)
    val SetupActivatedStroke = BusinessTokens.Colors.RESTAURANTAccent.copy(alpha = 0.45f)
    val SetupActivatedCheck = BusinessTokens.Colors.RESTAURANTAccent
    val SuccessToastFill = Color(0xFF0B1F18).copy(alpha = 0.94f)
    val SuccessToastStroke = Emerald.copy(alpha = 0.55f)
    val SuccessToastCheck = Emerald
    val QueueToastFill = Color(0xFF262E3D).copy(alpha = 0.92f)
    val QueueToastStroke = Emerald.copy(alpha = 0.35f)
    val ErrorToastFill = ErrorRing.copy(alpha = 0.16f)
    val ErrorToastStroke = ErrorRing.copy(alpha = 0.45f)
}

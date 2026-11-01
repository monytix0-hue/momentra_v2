package com.example.momentra.ui.shell.business.appstate

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Tokens for the Figma "App States · Shared (all Business verticals)" kit
 * (node 2666:29489, variable collection "App States · Accent").
 *
 * Every accent-driven layer in the kit binds a single variable, `appstate/accent`, which has one
 * mode per business vertical. These are the exact Figma values and are the source of truth for
 * this layer — they intentionally do not follow [com.example.momentra.ui.shell.business.shared.BusinessActiveTheme].
 */
enum class BusinessAppStateAccent(val color: Color) {
    /** App States · Accent / Trading — #10B981 */
    Trading(Color(0xFF10B981)),

    /** App States · Accent / Retail — #F43F5E */
    Retail(Color(0xFFF43F5E)),

    /** App States · Accent / RST (restaurant / food) — #EA580C */
    Rst(Color(0xFFEA580C)),

    /** App States · Accent / Services — #84CC16 */
    Services(Color(0xFF84CC16)),

    /** App States · Accent / MFG (manufacturing) — #F59E0B */
    Mfg(Color(0xFFF59E0B)),

    /** App States · Accent / Marketing — #6366F1 */
    Marketing(Color(0xFF6366F1)),

    /** App States · Accent / Distribution — #EA580C */
    Distribution(Color(0xFFEA580C)),
    ;

    companion object {
        /**
         * Resolve the accent for an operating moment subtype code.
         *
         * Mirrors the Figma mode set exactly, so the same moment paints the same colour on every
         * platform. Keep Android and iOS in lockstep — see BusinessAppStateAccent.swift.
         */
        fun forTypeCode(momentTypeCode: String?): BusinessAppStateAccent =
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
 * Surface ramp shared by all 17 app-state variants.
 *
 * These come from the "Momentra Business v2" collection in the same file
 * (`color/bg`, `color/surface`, `color/border`, `color/text`, `color/text-muted`) plus the
 * literal values Figma uses inline for glass and the CTA ramp.
 */
object BusinessAppStateTokens {
    // Surfaces
    val Page = Color(0xFF090D16)
    val Sheet = Color(0xFF0F1623)
    val SurfaceRaised = Color(0xFF151D2E)
    val Border = Color(0xFF1E293B)

    // Text
    val Text = Color(0xFFFFFFFF)
    val TextMuted = Color(0xFF94A3B8)
    val TextSubtle = Color(0xFF64748B)

    // Glass (backdrop-blurred cards and secondary buttons)
    val GlassFill = Color(0x14FFFFFF) // white @ 8%
    val GlassStroke = Color(0x38FFFFFF) // white @ 22%
    val GlassStrokeSoft = Color(0x24FFFFFF) // white @ 14%
    val BlurRadius = 24.dp

    // Primary CTA — always the orange ramp, never the accent
    val CtaStart = Color(0xFFE8621A)
    val CtaEnd = Color(0xFFF5A623)
    val CtaShadow = Color(0x54E8621A) // rgba(232, 98, 26, 0.33)

    // Semantic
    val ErrorRing = Color(0xFFFF5C38)
    val Danger = Color(0xFFEF4444)
    val Warning = Color(0xFFF5A623)
    val WarningSurface = Color(0x24F5A623) // amber @ 14%
    val WarningBorder = Color(0x80F5A623) // amber @ 50%
    val OfflineGrey = Color(0xFF94A3B8)

    // Geometry
    val ScreenHorizontalPadding = 24.dp
    val BodyWidth = 354.dp
    val BodyGap = 12.dp
    val GlowSize = 440.dp
    val GlowBlur = 160.dp
    val GlowAlpha = 0.12f
    val HaloBlur = 18.dp
    val RingSize = 92.dp
    val RingStroke = 3.dp
    val MarkSize = 80.dp
    val BadgeSize = 40.dp
    val BadgeBorder = 3.dp
    val CardRadius = 20.dp
    val ButtonRadius = 999.dp
    val PrimaryButtonHeight = 50.dp
    val SecondaryButtonHeight = 48.dp

    // Type scale
    val TitleSize = 24f
    val BodySize = 14f
    val EyebrowSize = 12f
    val RowLabelSize = 13f
    val RowValueSize = 14f
    val ButtonLabelSize = 14f
    val ErrorCodeSize = 11f
    val FootnoteSize = 11f

    val EyebrowTracking = 2f
    val ButtonTracking = 1f
    val ErrorCodeTracking = 1.5f
}

import SwiftUI

/// Tokens for the Figma "App States · Shared (all Business verticals)" kit.
///
/// Obsidian Emerald (Figma rknf `2:67`). Palette primitives **alias** `BusinessTokens.Colors`
/// so AppState stays in lockstep with regenerated Business design tokens.
///
/// Keep in lockstep with `BusinessAppStateTokens.kt` on Android.
enum BusinessAppStateAccent: String, CaseIterable {
    /// System emerald / success chrome `#10B981` (not trading amber).
    case emerald
    /// Trading / Distribution — TRD-v12 amber `#F59E0B`.
    case amber
    /// Retail Empty (Rose) `#F43F5E`.
    case rose
    /// RST Food Empty (Flame) `#EA580C`.
    case flame
    /// View All (Cyan) `#06B6D4`.
    case cyan
    /// Client Campaign indigo `#6366F1`.
    case indigo
    /// Own Business Campaign magenta `#D946EF`.
    case magenta

    var color: Color {
        switch self {
        case .emerald: return BusinessTokens.Colors.signalPositive
        case .amber: return BusinessTokens.Colors.tradingAccent
        case .rose: return BusinessTokens.Colors.retailAccent
        case .flame: return BusinessTokens.Colors.restaurantAccent
        case .cyan: return BusinessTokens.Colors.signalInfo
        case .indigo: return BusinessTokens.Colors.marketingAccent
        case .magenta: return BusinessTokens.Colors.marketingOwnAccent
        }
    }

    /// Trading wholesale → Emerald; Distribution dealership → Flame (`#EA580C`); Retail → Rose;
    /// Client Campaign → Indigo; Own Business Campaign → Magenta; else Emerald.
    /// ViewAll screens pass `.cyan` explicitly at the call site.
    static func forTypeCode(_ momentTypeCode: String?) -> BusinessAppStateAccent {
        switch momentTypeCode?.trimmingCharacters(in: .whitespacesAndNewlines).uppercased() {
        case "TRADING_WHOLESALE": return .emerald
        case "DISTRIBUTION_DEALERSHIP": return .flame
        case "RETAIL_STORE": return .rose
        case "RESTAURANT_FOOD": return .flame
        case "CLIENT_CAMPAIGN": return .indigo
        case "OWN_BUSINESS_CAMPAIGN": return .magenta
        default: return .emerald
        }
    }
}

/// The business vertical a moment belongs to.
enum BusinessAppStateVertical: String, CaseIterable {
    case trading
    case retail
    case rst
    case services
    case mfg
    case marketing
    case distribution

    static func forTypeCode(_ momentTypeCode: String?) -> BusinessAppStateVertical {
        switch momentTypeCode?.trimmingCharacters(in: .whitespacesAndNewlines).uppercased() {
        case "TRADING_WHOLESALE": return .trading
        case "DISTRIBUTION_DEALERSHIP": return .distribution
        case "RETAIL_STORE": return .retail
        case "RESTAURANT_FOOD": return .rst
        case "CONSUMER_SERVICE": return .services
        case "STANDARD_PRODUCTION", "MADE_TO_ORDER": return .mfg
        case "CLIENT_CAMPAIGN", "OWN_BUSINESS_CAMPAIGN": return .marketing
        default: return .trading
        }
    }
}

/// Surface ramp shared by every app-state variant. Color values alias `BusinessTokens` where matched.
enum BusinessAppStateTokens {
    // MARK: Palette primitives (alias BusinessTokens)
    /// System emerald — success toast / positive signals (stays `#10B981`).
    static let emerald = BusinessTokens.Colors.signalPositive
    static let emeraldDeep = BusinessTokens.Colors.ctaStart
    static let emeraldBright = BusinessTokens.Colors.ctaEnd
    /// Trading moment amber (TRD-v12).
    static let tradingAmber = BusinessTokens.Colors.tradingAccent
    static let tradingFlame = BusinessTokens.Colors.tradingAccentDeep
    static let errorRing = BusinessTokens.Colors.signalDanger
    static let warning = BusinessTokens.Colors.signalWarning

    // MARK: Surfaces
    static let page = BusinessTokens.Colors.bgCanvas
    static let sheet = Color(hex: "#0F141F")
    static let surfaceRaised = Color(hex: "#151D2E")
    static let border = BusinessTokens.Colors.borderDefault

    // MARK: Text
    static let text = BusinessTokens.Colors.textPrimary
    static let textMuted = BusinessTokens.Colors.textMuted
    static let textSubtle = Color(hex: "#738099")
    static let rowLabelColor = BusinessTokens.Colors.textSecondary

    // MARK: Glass
    static let glassFill = Color.white.opacity(0.08)
    static let glassStroke = Color.white.opacity(0.22)
    static let glassStrokeSoft = Color.white.opacity(0.14)
    static let blurRadius: CGFloat = 24

    // Lockstep APK GlassCardFill (SurfaceCard @ 0.72).
    // Called from BusinessAppStateKit / BusinessGlassSheetChrome / ViewAll sheet.
    static let glassCardFill = BusinessTokens.Colors.surfaceCard.opacity(0.72)
    static let glassCardStroke = emerald.opacity(0.35)
    static let glassCardStrokeWidth: CGFloat = 1.25
    static let hairlineStroke = emerald.opacity(0.18)
    static let glassBackdropBlur: CGFloat = 12

    // MARK: Primary CTA — Figma `0 4px 12px rgba(16,185,129,0.35)`
    static let ctaStart = BusinessTokens.Colors.ctaStart
    static let ctaEnd = BusinessTokens.Colors.ctaEnd
    static let ctaShadow = emerald.opacity(0.35)
    static let ctaShadowRadius: CGFloat = 12
    static let ctaShadowY: CGFloat = 4
    static let ctaShadowPad: CGFloat = 16

    static let secondaryButtonFill = Color.clear
    static let secondaryButtonStroke = emerald.opacity(0.18)
    static let secondaryButtonFrostAlpha: Double = 0.40

    // MARK: Ambient glow mesh (Figma 2:221 dual-orb)
    static let glowAccent = emerald
    static let glowAccentSize: CGFloat = 360
    static let glowAccentAlpha: Double = 0.28
    static let glowAccentMidAlpha: Double = 0.14
    static let glowAccentMidStop: CGFloat = 0.60
    static let glowWarm = emeraldDeep
    static let glowWarmSize: CGFloat = 280
    static let glowWarmAlpha: Double = 0.22
    static let glowError = errorRing
    static let glowErrorAlpha: Double = 0.25
    static let glowBlur: CGFloat = 48

    // MARK: Hero indicator badge — Figma halo fill ~8% + layer blur
    static let heroSize: CGFloat = 112
    static let heroRingSize: CGFloat = 96
    static let heroRingInset: CGFloat = 8
    static let heroRingStroke: CGFloat = 2
    static let heroRingFillAlpha: Double = 0.08
    static let heroRingStrokeAlpha: Double = 0.45
    static let heroBloomBlur: CGFloat = 10
    static let heroCoreSize: CGFloat = 76
    static let heroCoreInset: CGFloat = 18
    static let heroCoreStroke: CGFloat = 1.5
    static let heroCoreFill = Color(hex: "#0F141F")
    static let heroCoreStrokeAlpha: Double = 0.18
    static let heroDotSize: CGFloat = 36
    static let heroDotInset: CGFloat = 72
    static let heroDotStroke: CGFloat = 3.5
    static let heroGlyphSize: CGFloat = 28
    static let heroDotGlyphSize: CGFloat = 17
    static let heroDotGlyphColor = BusinessTokens.Colors.textInverse

    // MARK: Semantic
    static let danger = Color(hex: "#EF4444")
    static let errorPillFill = errorRing.opacity(0.15)
    static let errorPillStroke = emerald.opacity(0.45)
    static let offlineBannerFill = Color(red: 38 / 255, green: 46 / 255, blue: 61 / 255).opacity(0.90)
    static let offlineBannerStroke = emerald.opacity(0.40)
    static let offlineBannerHeight: CGFloat = 34
    static let offlineGrey = BusinessTokens.Colors.textMuted

    // MARK: Geometry
    static let screenHorizontalPadding: CGFloat = 24
    static let bodyWidth: CGFloat = 354
    static let bodyGap: CGFloat = 12
    static let cardRadius: CGFloat = 22
    static let cardPaddingHorizontal: CGFloat = 20
    static let cardPaddingVertical: CGFloat = 18
    static let cardRowGap: CGFloat = 12
    static let buttonRadius: CGFloat = 999
    static let primaryButtonHeight: CGFloat = 50
    static let secondaryButtonHeight: CGFloat = 46

    // MARK: Hub capture grid
    static let hubTileWidth: CGFloat = 111
    static let hubTileHeight: CGFloat = 67
    static let hubColumnGap: CGFloat = 10
    static let hubRowGap: CGFloat = 10
    static let hubTileRadius: CGFloat = 16
    static let hubTileFill = Color.white.opacity(0.05)
    static let hubTileStroke = emerald.opacity(0.25)
    static let hubIconSize: CGFloat = 18
    static let hubLabelSize: CGFloat = 11.5
    static let hubTileLabelGap: CGFloat = 6
    static let hubTileVerticalPadding: CGFloat = 14

    // MARK: Loading skeleton
    static let skeletonBoxHeight: CGFloat = 150
    static let skeletonPadHorizontal: CGFloat = 20
    static let skeletonPadVertical: CGFloat = 22
    static let skeletonGap: CGFloat = 14
    static let skeletonBarHeight: CGFloat = 16
    static let skeletonBarRadius: CGFloat = 8
    static let skeletonBarBase = Color.white.opacity(0.04)
    static let skeletonBarHighlight = Color.white.opacity(0.18)
    static let skeletonTrailingSpace: CGFloat = 110

    // MARK: Type scale
    static let titleSize: CGFloat = 24
    static let bodySize: CGFloat = 13.5
    static let bodyLineSpacing: CGFloat = 3
    static let eyebrowSize: CGFloat = 11
    static let rowLabelSize: CGFloat = 13
    static let rowValueSize: CGFloat = 13
    static let buttonLabelSize: CGFloat = 13.5
    static let errorCodeSize: CGFloat = 11
    static let footnoteSize: CGFloat = 11
    static let offlineBannerLabelSize: CGFloat = 11.5

    static let eyebrowTracking: CGFloat = 1.5
    static let buttonTracking: CGFloat = 1
    static let errorCodeTracking: CGFloat = 1.5

    static let footnoteMutedOpacity: CGFloat = 1

    // MARK: Toaster / banner system (Figma 2:1375)
    static let toastRadius: CGFloat = 14
    static let toastPadHorizontal: CGFloat = 14
    static let toastPadVertical: CGFloat = 12
    static let toastLabelSize: CGFloat = 12.5
    static let toastTitleSize: CGFloat = 13.5
    static let toastSubtitleSize: CGFloat = 11.5
    static let setupActivatedFill = Color(hex: "#16120E").opacity(0.96)
    static let setupActivatedStroke = BusinessTokens.Colors.restaurantAccent.opacity(0.45)
    static let setupActivatedCheck = BusinessTokens.Colors.restaurantAccent
    static let successToastFill = Color(hex: "#0B1F18").opacity(0.94)
    static let successToastStroke = emerald.opacity(0.55)
    static let successToastCheck = emerald
    static let queueToastFill = Color(red: 38 / 255, green: 46 / 255, blue: 61 / 255).opacity(0.92)
    static let queueToastStroke = emerald.opacity(0.35)
    static let errorToastFill = errorRing.opacity(0.16)
    static let errorToastStroke = errorRing.opacity(0.45)
}

import SwiftUI

/// Tokens for the Figma "App States · Shared (all Business verticals)" kit
/// (node 2666:29489, variable collection "App States · Accent").
///
/// Every accent-driven layer in the kit binds a single variable, `appstate/accent`, which has one
/// mode per business vertical. These are the exact Figma values and are the source of truth for
/// this layer — they intentionally do not follow `BusinessActiveTheme`.
///
/// Keep in lockstep with `BusinessAppStateTokens.kt` on Android.
enum BusinessAppStateAccent: String, CaseIterable {
    /// App States · Accent / Trading — #10B981
    case trading
    /// App States · Accent / Retail — #F43F5E
    case retail
    /// App States · Accent / RST (restaurant / food) — #EA580C
    case rst
    /// App States · Accent / Services — #84CC16
    case services
    /// App States · Accent / MFG (manufacturing) — #F59E0B
    case mfg
    /// App States · Accent / Marketing — #6366F1
    case marketing
    /// App States · Accent / Distribution — #EA580C
    case distribution

    var color: Color {
        switch self {
        case .trading: return Color(hex: "#10B981")
        case .retail: return Color(hex: "#F43F5E")
        case .rst: return Color(hex: "#EA580C")
        case .services: return Color(hex: "#84CC16")
        case .mfg: return Color(hex: "#F59E0B")
        case .marketing: return Color(hex: "#6366F1")
        case .distribution: return Color(hex: "#EA580C")
        }
    }

    /// Resolve the accent for an operating moment subtype code.
    ///
    /// Mirrors the Figma mode set exactly, so the same moment paints the same colour on every
    /// platform. Keep Android and iOS in lockstep — see `BusinessAppStateAccent.kt`.
    static func forTypeCode(_ momentTypeCode: String?) -> BusinessAppStateAccent {
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

/// Surface ramp shared by all 17 app-state variants.
///
/// These come from the "Momentra Business v2" collection in the same Figma file
/// (`color/bg`, `color/surface`, `color/border`, `color/text`, `color/text-muted`) plus the
/// literal values Figma uses inline for glass and the CTA ramp.
enum BusinessAppStateTokens {
    // MARK: Surfaces
    static let page = Color(hex: "#090D16")
    static let sheet = Color(hex: "#0F1623")
    static let surfaceRaised = Color(hex: "#151D2E")
    static let border = Color(hex: "#1E293B")

    // MARK: Text
    static let text = Color.white
    static let textMuted = Color(hex: "#94A3B8")
    static let textSubtle = Color(hex: "#64748B")

    // MARK: Glass (backdrop-blurred cards and secondary buttons)
    static let glassFill = Color.white.opacity(0.08)
    static let glassStroke = Color.white.opacity(0.22)
    static let glassStrokeSoft = Color.white.opacity(0.14)
    static let blurRadius: CGFloat = 12

    // MARK: Primary CTA — always the orange ramp, never the accent
    static let ctaStart = Color(hex: "#E8621A")
    static let ctaEnd = Color(hex: "#F5A623")
    static let ctaShadow = Color(hex: "#E8621A").opacity(0.33)

    // MARK: Semantic
    static let errorRing = Color(hex: "#FF5C38")
    static let danger = Color(hex: "#EF4444")
    static let warning = Color(hex: "#F5A623")
    static let warningSurface = warning.opacity(0.14)
    static let warningBorder = warning.opacity(0.50)
    static let offlineGrey = Color(hex: "#94A3B8")

    // MARK: Geometry
    static let screenHorizontalPadding: CGFloat = 24
    static let bodyWidth: CGFloat = 354
    static let bodyGap: CGFloat = 12
    static let glowSize: CGFloat = 440
    static let glowAlpha: Double = 0.12
    static let haloAlpha: Double = 0.55
    static let ringSize: CGFloat = 92
    static let ringStroke: CGFloat = 3
    static let markSize: CGFloat = 80
    static let badgeSize: CGFloat = 40
    static let badgeBorder: CGFloat = 3
    static let cardRadius: CGFloat = 20
    static let buttonRadius: CGFloat = 999
    static let primaryButtonHeight: CGFloat = 50
    static let secondaryButtonHeight: CGFloat = 48

    // MARK: Type scale
    static let titleSize: CGFloat = 24
    static let bodySize: CGFloat = 14
    static let eyebrowSize: CGFloat = 12
    static let rowLabelSize: CGFloat = 13
    static let rowValueSize: CGFloat = 14
    static let buttonLabelSize: CGFloat = 14
    static let errorCodeSize: CGFloat = 11
    static let footnoteSize: CGFloat = 11

    static let eyebrowTracking: CGFloat = 2
    static let buttonTracking: CGFloat = 1
    static let errorCodeTracking: CGFloat = 1.5
}

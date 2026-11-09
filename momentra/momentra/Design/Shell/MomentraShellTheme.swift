import SwiftUI

enum GlobalTheme {
    static let topBarBackground = Color(hex: 0x0C0F15)
    static let bottomBarBackground = Color(hex: 0x0C0F15)
    /// Trading / Distribution canvas under the business tab bar (TRD-PULSE-v12).
    static let businessBottomBarBackground = Color(hex: 0x090D16)

    /// Tab bar fill tuned to the active business moment's domain canvas.
    static func businessBottomBarBackground(momentTypeCode: String?) -> Color {
        guard let vertical = BusinessOperatingVertical.forSubtype(momentTypeCode) else {
            return businessBottomBarBackground
        }
        switch vertical {
        case .trading: return BusinessTokens.Colors.tradingCanvas
        case .distribution: return BusinessTokens.Colors.distributionCanvas
        case .standardProduction, .madeToOrder: return BusinessTokens.Colors.manufacturingCanvas
        case .clientCampaign: return BusinessTokens.Colors.marketingCanvas
        case .ownBusinessCampaign: return BusinessTokens.Colors.marketingOwnCanvas
        case .retailStore: return BusinessTokens.Colors.retailCanvas
        case .restaurantFood: return BusinessTokens.Colors.restaurantCanvas
        case .consumerService: return BusinessTokens.Colors.servicesCanvas
        }
    }
    static let surfaceContent = Color(hex: 0x14121B)
    static let contextUnselected = Color(hex: 0xC9C4D9)
    static let bottomSelected = Color(hex: 0xC9BFFF)
    static let bottomUnselected = Color(hex: 0xC9C4D8)
    /// TRD/DST bottom nav idle icons + labels (`#64748B` slate).
    static let businessBottomUnselected = Color(hex: 0x64748B)
    static let actionCircle = Color(hex: 0x1E293B)
    static let statusOnline = Color(hex: 0x10B981)
    static let moduleCardBackground = Color(hex: 0x161B26)
    static let createMomentCta = Color(hex: 0xE8621A)
}

struct ContextTheme {
    let context: AppContextKind
    let contextAccent: Color
    let contextAccentSecondary: Color

    static func of(_ context: AppContextKind) -> ContextTheme {
        switch context {
        case .personal:
            return ContextTheme(context: .personal, contextAccent: Color(hex: 0x7C5CFC), contextAccentSecondary: Color(hex: 0xA78BFA))
        case .group:
            return ContextTheme(context: .group, contextAccent: Color(hex: 0xE8621A), contextAccentSecondary: Color(hex: 0xFF8E63))
        case .business:
            // Business context chrome — Figma vertical accent `#EA580C` / soft `#FFB598`.
            return ContextTheme(context: .business, contextAccent: Color(hex: 0xEA580C), contextAccentSecondary: Color(hex: 0xFFB598))
        case .circle:
            return ContextTheme(context: .circle, contextAccent: Color(hex: 0xE86BA3), contextAccentSecondary: Color(hex: 0xFF6B8A))
        }
    }
}

/// Cross-context global surface — NOT a selectable AppContext. Figma Coming Soon `1075:7637`.
enum GlobalSurfaceTheme {
    struct Life360 {
        let surface = GlobalTheme.topBarBackground
        let action = GlobalTheme.actionCircle
        let online = GlobalTheme.statusOnline
        /// Coming Soon page background — Figma `#14121B`.
        let comingSoonBackground = Color(hex: 0x14121B)
        let card = Color(hex: 0x161B26)
        let gold = Color(hex: 0xF2CA50)
        let goldEnd = Color(hex: 0xFFAB40)
        let textPrimary = Color(hex: 0xE5E0EE)
        let textSecondary = Color(hex: 0xC9C4D8)
        let decorativeProgressFraction: CGFloat = 0.65
    }

    static let life360 = Life360()
}

/// Circle context Coming Soon surface — Figma `1075:7556`.
enum CircleComingSoonTheme {
    static let pageStart = Color(hex: 0x14121B)
    static let pageEnd = Color(hex: 0x1C1B1B)
    static let card = Color(hex: 0x161B26)
    static let cardAlt = Color(hex: 0x1C1B1B)
    static let accent = Color(hex: 0xE86BA3)
    static let accentEnd = Color(hex: 0xFF6B8A)
    static let lavender = Color(hex: 0xB794F6)
    static let peach = Color(hex: 0xFFB5A7)
    static let textPrimary = Color(hex: 0xE5E2E1)
    static let textSecondary = Color(hex: 0xD0C5AF).opacity(0.8)
    static let selectedTab = Color(hex: 0xFC6A8B)
    static let decorativeProgressFraction: CGFloat = 0.45
}

struct MomentTheme {
    let family: String
    let type: String
    let primary: Color
    let secondary: Color
    let surfaceTint: Color
    let icon: Color
}

enum MomentThemes {
    static func resolve(context: AppContextKind, momentTypeCode: String?) -> MomentTheme {
        switch context {
        case .personal: return personal(momentTypeCode)
        case .group: return group(momentTypeCode)
        case .business: return business(momentTypeCode)
        case .circle: return personal(nil)
        }
    }

    static func personal(_ momentTypeCode: String?) -> MomentTheme {
        let code = momentTypeCode?.uppercased() ?? ""
        if code == "FUTURE_GOAL" || code == "FUTURE_BUILDING" {
            return base("PERSONAL", "FUTURE_BUILDING", Color(hex: 0x10B981), Color(hex: 0x34D399))
        }
        if code == "LIFESTYLE" {
            return base("PERSONAL", "LIFESTYLE", Color(hex: 0x0EA5A4), Color(hex: 0x7C5CFC))
        }
        if code == "RELATIONSHIP_CONNECTION" || code == "RELATIONSHIPS" {
            return base("PERSONAL", "RELATIONSHIPS", Color(hex: 0xE91E63), Color(hex: 0xE12A9E))
        }
        return base("PERSONAL", "LIFE_OPERATIONS", Color(hex: 0x7C5CFC), Color(hex: 0xA78BFA))
    }

    static func group(_ momentTypeCode: String?) -> MomentTheme {
        let code = momentTypeCode?.uppercased() ?? ""
        if code.contains("WEDDING") { return base("GROUP", "WEDDING", Color(hex: 0xEC4899), Color(hex: 0xF472B6)) }
        if code.contains("FLATMATES") { return base("GROUP", "FLATMATES", Color(hex: 0xE8744F), Color(hex: 0xFF7A3D)) }
        if code.contains("FAMILY_HOUSEHOLD") { return base("GROUP", "FAMILY_HOUSEHOLD", Color(hex: 0xEC4899), Color(hex: 0xF472B6)) }
        if code.contains("CO_LIVING") { return base("GROUP", "CO_LIVING", Color(hex: 0x3B82F6), Color(hex: 0x60A5FA)) }
        if code.contains("COMMUNITY_LIVING") || code.contains("CUSTOM_LIVING") {
            return base("GROUP", "COMMUNITY_LIVING", Color(hex: 0x10B981), Color(hex: 0x34D399))
        }
        if code.contains("LIVING") && !code.contains("CO_LIVING") {
            return base("GROUP", "COMMUNITY_LIVING", Color(hex: 0x10B981), Color(hex: 0x34D399))
        }
        if code.contains("HOUSE_PARTY") || code.contains("PARTY") { return base("GROUP", "HOUSE_PARTY", Color(hex: 0x3B82F6), Color(hex: 0x60A5FA)) }
        if code.contains("OUTING") || code.contains("OFFICE") { return base("GROUP", "OFFICE_OUTING", Color(hex: 0x14B8A6), Color(hex: 0x2DD4BF)) }
        if code.contains("GIFT") { return base("GROUP", "GIFT_POOL", Color(hex: 0xE8621A), Color(hex: 0xFF8E63)) }
        if code.contains("PURCHASE") || code.contains("ASSET") { return base("GROUP", "GROUP_PURCHASE", Color(hex: 0xE8621A), Color(hex: 0xFF8E63)) }
        if code.contains("TRIP") { return base("GROUP", "TRIP", Color(hex: 0xE8744F), Color(hex: 0xFF8E63)) }
        return base("GROUP", "CUSTOM", Color(hex: 0xE8621A), Color(hex: 0xC9C4D9))
    }

    static func business(_ momentTypeCode: String?) -> MomentTheme {
        let code = momentTypeCode?.uppercased() ?? ""
        // Domain Color System — matches design/tokens/business.tokens.json (nav + chrome accent).
        if code == "DISTRIBUTION_DEALERSHIP" || code.contains("DEALERSHIP") {
            // Distribution · S `#EA580C` / soft `#FFB598` (DST-PULSE-v12)
            return base("BUSINESS", code.isEmpty ? "DISTRIBUTION_DEALERSHIP" : code, Color(hex: 0xEA580C), Color(hex: 0xFFB598))
        }
        if code == "TRADING_WHOLESALE" || code.contains("WHOLESALE") || code == "TRADING_DISTRIBUTION" {
            // FinTech / Trading · S `#10B981` / soft `#6EE7B7` (TRD-PULSE-v12)
            return base("BUSINESS", code.isEmpty ? "TRADING_WHOLESALE" : code, Color(hex: 0x10B981), Color(hex: 0x6EE7B7))
        }
        if code == "RESTAURANT_FOOD" || code.contains("RESTAURANT") {
            // Restaurant · S `#EA580C` / H `#FCD34D` (RST-PULSE-v12)
            return base("BUSINESS", code, Color(hex: 0xEA580C), Color(hex: 0xFCD34D))
        }
        if code == "RETAIL_STORE" || code == "RETAIL_CONSUMER" || code.contains("RETAIL") {
            return base("BUSINESS", code, Color(hex: 0xF43F5E), Color(hex: 0xFB7185))
        }
        if code == "CONSUMER_SERVICE" || (code.contains("SERVICE") && !code.contains("CAMPAIGN")) {
            return base("BUSINESS", code, Color(hex: 0x0D9488), Color(hex: 0x84CC16))
        }
        if code == "STANDARD_PRODUCTION" || code == "MANUFACTURING_PRODUCTION" || code == "MADE_TO_ORDER"
            || code.contains("MANUFACTURING") {
            return base("BUSINESS", code, Color(hex: 0xF59E0B), Color(hex: 0xFBBF24))
        }
        if code == "OWN_BUSINESS_CAMPAIGN" {
            return base("BUSINESS", code, Color(hex: 0xD946EF), Color(hex: 0xE879F9))
        }
        if code.contains("MARKETING") || code == "CLIENT_CAMPAIGN" {
            return base("BUSINESS", code.isEmpty ? "CLIENT_CAMPAIGN" : code, Color(hex: 0x6366F1), Color(hex: 0x818CF8))
        }
        return base("BUSINESS", "BUSINESS_OPERATIONS", Color(hex: 0x818CF8), Color(hex: 0xA5B4FC))
    }

    private static func base(_ family: String, _ type: String, _ primary: Color, _ secondary: Color) -> MomentTheme {
        MomentTheme(
            family: family,
            type: type,
            primary: primary,
            secondary: secondary,
            surfaceTint: primary.opacity(0.16),
            icon: primary
        )
    }
}

extension Color {
    init(hex: UInt32, alpha: Double = 1) {
        self.init(
            .sRGB,
            red: Double((hex >> 16) & 0xFF) / 255,
            green: Double((hex >> 8) & 0xFF) / 255,
            blue: Double(hex & 0xFF) / 255,
            opacity: alpha
        )
    }
}

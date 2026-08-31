import SwiftUI

struct GroupTypePalette {
    let accent: Color
    let accentLight: Color
    let stepGlow: Color
    let organizerRoleBg: Color
    let organizerRoleBorder: Color

    var accentGradient: LinearGradient {
        LinearGradient(colors: [accent, accentLight], startPoint: .leading, endPoint: .trailing)
    }
}

/// Figma 575:9917 — Group setup wizard theme (separate from Group empty-state tokens).
enum GroupSetupTheme {
    static let bg = Color(hex: "#14121B")
    static let textPrimary = Color.white
    static let textSecondary = Color(hex: "#9CA3AF")
    static let card = Color(hex: "#1C1926")
    static let border = Color(hex: "#2A2538")
    static let iconSurface = Color(hex: "#2A2538")
    static let ctaText = Color(hex: "#14121B")
    static let stepInactiveBg = Color(hex: "#14121B")
    static let typeIconBg = Color.white.opacity(0.1)

    static let tripAccent = Color(hex: "#E8744F")
    static let tripAccentLight = Color(hex: "#FF8E63")
    static let weddingAccent = Color(hex: "#EC4899")
    static let weddingAccentLight = Color(hex: "#F472B6")
    static let partyAccent = Color(hex: "#3B82F6")
    static let partyAccentLight = Color(hex: "#60A5FA")
    static let outingAccent = Color(hex: "#14B8A6")
    static let outingAccentLight = Color(hex: "#2DD4BF")

    static let tripPalette = palette(accent: tripAccent, accentLight: tripAccentLight)
    static let weddingPalette = palette(accent: weddingAccent, accentLight: weddingAccentLight)
    static let partyPalette = palette(accent: partyAccent, accentLight: partyAccentLight)
    static let outingPalette = palette(accent: outingAccent, accentLight: outingAccentLight)

    static var tripGradient: LinearGradient {
        LinearGradient(colors: [tripAccent, tripAccentLight], startPoint: .leading, endPoint: .trailing)
    }

    static var weddingGradient: LinearGradient {
        LinearGradient(colors: [weddingAccent, weddingAccentLight], startPoint: .leading, endPoint: .trailing)
    }

    static var partyGradient: LinearGradient {
        LinearGradient(colors: [partyAccent, partyAccentLight], startPoint: .leading, endPoint: .trailing)
    }

    static var outingGradient: LinearGradient {
        LinearGradient(colors: [outingAccent, outingAccentLight], startPoint: .leading, endPoint: .trailing)
    }

    static func gradient(for code: String) -> LinearGradient {
        palette(for: code).accentGradient
    }

    static func selectedBorder(for code: String) -> Color {
        palette(for: code).accent
    }

    static func palette(for code: String) -> GroupTypePalette {
        switch code {
        case "WEDDING", "GIFT_POOL", "FAMILY_HOUSEHOLD":
            return weddingPalette
        case "HOUSE_PARTY", "SHARED_ASSET", "CO_LIVING":
            return partyPalette
        case "OFFICE_OUTING", "CUSTOM", "COMMUNITY_PURCHASE", "COMMUNITY_LIVING":
            return outingPalette
        case "GROUP_PURCHASE":
            return tripPalette
        case "FLATMATES":
            return tripPalette
        default:
            return tripPalette
        }

    }

    static func resolveShellAccent(
        context: AppContextKind,
        phase: GroupCreatePhase = .chooser,
        typeCode: String? = nil
    ) -> Color {
        if context == .group, phase != .chooser, let code = typeCode {
            return palette(for: code).accent
        }
        switch context {
        case .group: return Color(hex: "#E8621A")
        case .business: return Color(hex: "#818CF8")
        case .personal: return Color(hex: "#7C5CFC")
        case .circle: return Color(hex: "#FC6A8B")
        }
    }

    private static func palette(accent: Color, accentLight: Color) -> GroupTypePalette {
        GroupTypePalette(
            accent: accent,
            accentLight: accentLight,
            stepGlow: accent.opacity(0.4),
            organizerRoleBg: accent.opacity(0.1),
            organizerRoleBorder: accent.opacity(0.2)
        )
    }
}

extension Font {
    static func plusJakarta(size: CGFloat, weight: Font.Weight = .regular) -> Font {
        let name: String
        switch weight {
        case .heavy, .black: name = "PlusJakartaSans-ExtraBold"
        case .bold: name = "PlusJakartaSans-Bold"
        case .semibold, .medium: name = "PlusJakartaSans-SemiBold"
        default: name = "PlusJakartaSans-Regular"
        }
        return .custom(name, size: size)
    }
}

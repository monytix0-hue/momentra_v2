import SwiftUI

/// Restaurant "Ember / Stone" design system extracted from Figma `2487:37611`
/// (`RST-PULSE-v12` `2400:29655`, `RST-HUB-v5` `2400:29892`, `RST-MOMENTS-v12` `2400:30664`,
/// `RST-LIFE-v12` `2400:30824`, `RST-MEMORY-v12` `2400:30984`, `RST-SETUP-v6` `2470:29187`).
///
/// Replaces the former `RestaurantChrome` (stone green `#101412` + amber `#F59E0B`).
/// Every value below is read from the frames — nothing is rounded to a "close" colour.
enum RestaurantEmber {
    // MARK: Screen
    /// Vertical 3-stop screen wash: `#44403c` → `#1c1917` (35%) → `#0c0a09`.
    static let screenTop = Color(hex: "#44403C")
    static let screenMid = Color(hex: "#1C1917")
    static let screenBottom = Color(hex: "#0C0A09")

    static var screenGradient: LinearGradient {
        LinearGradient(
            stops: [
                .init(color: screenTop, location: 0),
                .init(color: screenMid, location: 0.35),
                .init(color: screenBottom, location: 1),
            ],
            startPoint: .top,
            endPoint: .bottom
        )
    }

    /// Hero gradient used by the Pulse / Moments / Life / Memory hero and the setup CTA:
    /// `#881337` → `#EA580C`.
    static var heroGradient: LinearGradient {
        LinearGradient(
            colors: [Color(hex: "#881337"), ember],
            startPoint: .topLeading,
            endPoint: .bottomTrailing
        )
    }

    // MARK: Surfaces
    /// Pulse and board card surface (`#292524`, radius 20).
    static let card = Color(hex: "#292524")
    /// Inset well: health-ring hero, metric chips, list-row icon wells (`#1c1917`).
    static let surfaceWell = Color(hex: "#1C1917")
    /// Sheet and field chrome — every `RST-QA-*` and `RST-SHEET-*` root (`#1c1917`).
    static let sheet = Color(hex: "#1C1917")
    /// Field / input background inside a sheet.
    static let field = Color(hex: "#1C1917")
    /// Context chips on the non-filled state (`#44403c`).
    static let chip = Color(hex: "#44403C")
    /// Quick Add tile background (`#292524`).
    static let tile = Color(hex: "#292524")
    /// Nav / header chrome.
    static let chrome = Color(hex: "#1C1917")

    // MARK: Accents
    /// Primary ember `#EA580C` — tile borders, icon wells, ring progress, `+ Quick Adds`.
    static let ember = Color(hex: "#EA580C")
    /// Gold `#FCD34D` — metric values, `View all ›`, hero status line.
    static let gold = Color(hex: "#FCD34D")
    /// Wine `#881337` — filled context chip and hero gradient start.
    static let wine = Color(hex: "#881337")
    /// Soft hero text tints from `2400:29655 hero`.
    static let heroEyebrowTint = Color(hex: "#FEF3C7")
    static let heroBodyTint = Color(hex: "#FDE68A")
    static let amber = Color(hex: "#FBBF24")
    static let cyan = Color(hex: "#06B6D4")
    static let positive = Color(hex: "#22C55E")
    static let negative = Color(hex: "#EF4444")
    static let warning = Color(hex: "#F59E0B")

    // MARK: Text
    static let textPrimary = Color(hex: "#FAFAF9")
    static let textBody = Color(hex: "#A8A29E")
    static let textMuted = Color(hex: "#94A3B8")
    static let textFaint = Color(hex: "#78716C")
    /// Ink used on gold / ember filled controls.
    static let inkOnEmber = Color(hex: "#1C1917")

    // MARK: Hairlines
    /// Card border — the frames use a 1pt stroke on tiles, and hairlines inside cards.
    static let cardStroke = Color.white.opacity(0.06)
    static let divider = Color(hex: "#44403C")
    static let hairline = Color(hex: "#57534E")
    static let tileStroke = ember
    static let chipStroke = Color(hex: "#44403C")

    // MARK: Radii
    static let rPill: CGFloat = 999
    static let rHero: CGFloat = 24
    static let rCard: CGFloat = 20
    static let rCardTight: CGFloat = 16
    static let rTile: CGFloat = 16
    static let rMetric: CGFloat = 14
    static let rRow: CGFloat = 12
    static let rHeroIcon: CGFloat = 12
    static let rTileIcon: CGFloat = 10
    static let rStoryCta: CGFloat = 18

    // MARK: Layout
    static let gutter: CGFloat = 20
    static let sectionGap: CGFloat = 16
    static let cardGap: CGFloat = 14

    static func tone(_ color: Color) -> Color { color.opacity(0.16) }
    static func toneStroke(_ color: Color) -> Color { color.opacity(0.45) }
}

/// Shadow stack the design applies to ember cards.
extension View {
    /// Tile / card ambient ember glow: `0 8px 28px rgba(234,88,12,.18)`.
    func emberCardShadow() -> some View {
        self
            .shadow(color: RestaurantEmber.ember.opacity(0.18), radius: 14, x: 0, y: 8)
            .shadow(color: .black.opacity(0.35), radius: 3, x: 0, y: 2)
    }

    /// The hero frame carries two stacked shadows (`2400:29655 hero`): a tight ember
    /// shadow at 50% and a wide gold glow at 30%.
    func emberHeroShadow() -> some View {
        self
            .shadow(color: RestaurantEmber.ember.opacity(0.50), radius: 16, x: 0, y: 12)
            .shadow(color: RestaurantEmber.gold.opacity(0.30), radius: 32, x: 0, y: 24)
    }

    /// `inset 0 1px 0 rgba(255,255,255,.05)` drawn as a top hairline over the clipped card.
    func emberTopHighlight(_ radius: CGFloat = RestaurantEmber.rCard) -> some View {
        overlay(alignment: .top) {
            Rectangle()
                .fill(Color.white.opacity(0.05))
                .frame(height: 1)
                .clipShape(UnevenRoundedRectangle(
                    topLeadingRadius: radius,
                    bottomLeadingRadius: 0,
                    bottomTrailingRadius: 0,
                    topTrailingRadius: radius,
                    style: .continuous
                ))
        }
    }
}

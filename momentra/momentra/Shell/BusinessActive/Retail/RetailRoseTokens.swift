import SwiftUI

/// Retail "Rose / Maroon" design system extracted from Figma `2487:37758`
/// (RTL-PULSE-v12 / RTL-HUB-v5 / RTL-MOMENTS-v12 / RTL-LIFE-v12 / RTL-MEMORY-v12).
///
/// Replaces the former `RetailStoreChrome` (dark blue `#0B1020` + green `#34D399`).
enum RetailRose {
    // MARK: Screen
    /// Vertical 3-stop screen wash: `#4c0519` → `#0b0a1f` (35%) → `#050414`.
    static let screenTop = Color(hex: "#4C0519")
    static let screenMid = Color(hex: "#0B0A1F")
    static let screenBottom = Color(hex: "#050414")

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

    // MARK: Surfaces
    /// Nested/inset card surface (Pulse cards use `#2a1a1f`).
    static let card = Color(hex: "#2A1A1F")
    /// Rows and metric chips sit on the deeper `#170d11`.
    static let surfaceRow = Color(hex: "#170D11")
    /// Moment/Life/Memory cards and Quick-Add tiles use `#1e293b`.
    static let cardSlate = Color(hex: "#1E293B")
    /// Context chips + nav chrome.
    static let chip = Color(hex: "#1E293B")
    /// Field / header chrome.
    static let field = Color(hex: "#0F172A")
    /// Tile background inside the Quick Add grid.
    static let tile = Color(hex: "#1E293B")
    /// KPI tile sitting inside a slate card.
    static let kpiOnSlate = Color.black.opacity(0.28)

    // MARK: Accents
    static let rose = Color(hex: "#F43F5E")
    static let roseBorder = Color(hex: "#E11D48")
    static let roseSoft = Color(hex: "#FFE4E6")
    static let roseMuted = Color(hex: "#FECDD3")
    static let cyan = Color(hex: "#06B6D4")
    static let amber = Color(hex: "#FBBF24")
    static let positive = Color(hex: "#22C55E")
    static let negative = Color(hex: "#EF4444")
    static let warning = Color(hex: "#F59E0B")
    static let indigo = Color(hex: "#4F46E5")

    // MARK: Text
    static let textPrimary = Color(hex: "#F8FAFC")
    static let textBody = Color(hex: "#CBD5E1")
    static let textMuted = Color(hex: "#94A3B8")
    static let textFaint = Color(hex: "#64748B")
    static let inkOnRose = Color(hex: "#04110C")

    // MARK: Hairlines
    static let cardStroke = rose.opacity(0.28)
    static let cardStrokeStrong = rose.opacity(0.40)
    static let cardStrokeHeavy = rose.opacity(0.45)
    static let divider = Color(hex: "#4C0519").opacity(0.6)
    static let chipStroke = Color.white.opacity(0.06)
    static let tileStroke = rose.opacity(0.35)

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

/// Shadow stack the design applies to every rose card.
extension View {
    /// `0 8px 28px -4px rgba(244,63,94,.18)` + `0 2px 6px rgba(0,0,0,.35)`
    func roseCardShadow() -> some View {
        self
            .shadow(color: RetailRose.rose.opacity(0.18), radius: 14, x: 0, y: 8)
            .shadow(color: .black.opacity(0.35), radius: 3, x: 0, y: 2)
    }

    /// `inset 0 1px 0 rgba(255,255,255,.05)` drawn as a top hairline over the clipped card.
    func roseTopHighlight(_ radius: CGFloat = RetailRose.rCard) -> some View {
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

import SwiftUI

/// `TRD-SETUP-v6` / `DST-SETUP-v6` document tokens.
///
/// The six-section setup document is a document, not an operating screen: it has its own card
/// fill, inset, panel stroke and faint tone. Reusing `OperatingTheme` here is what previously
/// made the iOS document read as a flat form over the operating canvas, so this type mirrors
/// `SetupDocTheme` in `SetupDocumentKit.kt` field for field and value for value.
struct BusinessSetupDocTheme {
    let accent: Color
    let canvas: Color
    let card: Color
    let inset: Color
    let border: Color
    let panelStroke: Color
    let ink: Color
    let muted: Color
    let faint: Color
    let positive: Color
    let negative: Color

    /// `TRD-SETUP-v6` — emerald on the trading canvas (`2566:32923`).
    static let trading = BusinessSetupDocTheme(
        accent: Color(hex: "#10B981"),
        canvas: Color(hex: "#090D16"),
        card: Color(hex: "#151D2E"),
        inset: Color(hex: "#090D16"),
        border: Color(hex: "#1E293B"),
        panelStroke: Color(hex: "#334155"),
        ink: Color(hex: "#F8FAFC"),
        muted: Color(hex: "#94A3B8"),
        faint: Color(hex: "#64748B"),
        positive: Color(hex: "#22C55E"),
        negative: Color(hex: "#F87171")
    )

    /// `DST-SETUP-v6` — orange on the distribution canvas (aligned to DST-PULSE-v12).
    static let distribution = BusinessSetupDocTheme(
        accent: Color(hex: "#EA580C"),
        canvas: Color(hex: "#0F172A"),
        card: Color(hex: "#151D2E"),
        inset: Color(hex: "#0F172A"),
        border: Color(hex: "#1E293B"),
        panelStroke: Color(hex: "#334155"),
        ink: Color(hex: "#F8FAFC"),
        muted: Color(hex: "#94A3B8"),
        faint: Color(hex: "#64748B"),
        positive: Color(hex: "#22C55E"),
        negative: Color(hex: "#F87171")
    )

    /// `MKT-SETUP-v6` — indigo Client campaign (`2527:43783`).
    static let clientCampaign = BusinessSetupDocTheme(
        accent: Color(hex: "#6366F1"),
        canvas: Color(hex: "#0F172A"),
        card: Color(hex: "#1E293B"),
        inset: Color(hex: "#0F172A"),
        border: Color(hex: "#334155"),
        panelStroke: Color(hex: "#475569"),
        ink: Color(hex: "#F8FAFC"),
        muted: Color(hex: "#94A3B8"),
        faint: Color(hex: "#64748B"),
        positive: Color(hex: "#22C55E"),
        negative: Color(hex: "#F87171")
    )

    /// `MKT-OWN-SETUP-v6` — magenta Own Business campaign (`2572:45122`).
    static let ownBusinessCampaign = BusinessSetupDocTheme(
        accent: Color(hex: "#D946EF"),
        canvas: Color(hex: "#0F172A"),
        card: Color(hex: "#1E293B"),
        inset: Color(hex: "#0F172A"),
        border: Color(hex: "#334155"),
        panelStroke: Color(hex: "#475569"),
        ink: Color(hex: "#F8FAFC"),
        muted: Color(hex: "#94A3B8"),
        faint: Color(hex: "#64748B"),
        positive: Color(hex: "#22C55E"),
        negative: Color(hex: "#F87171")
    )

    /// Warm orange emblem glow shared by Client and Own.
    static let emblemGlow = Color(hex: "#EA580C")

    /// `RTL-SETUP-v6` — rose on the retail canvas.
    static let retail = BusinessSetupDocTheme(
        accent: Color(hex: "#F43F5E"),
        canvas: Color(hex: "#0B0A1F"),
        card: Color(hex: "#2A1A1F"),
        inset: Color(hex: "#170D11"),
        border: Color(hex: "#4C0519"),
        panelStroke: Color(hex: "#881337"),
        ink: Color(hex: "#F8FAFC"),
        muted: Color(hex: "#94A3B8"),
        faint: Color(hex: "#64748B"),
        positive: Color(hex: "#22C55E"),
        negative: Color(hex: "#F87171")
    )

    /// `RST-SETUP-v6` — ember on restaurant stone.
    static let restaurant = BusinessSetupDocTheme(
        accent: Color(hex: "#EA580C"),
        canvas: Color(hex: "#1C1917"),
        card: Color(hex: "#292524"),
        inset: Color(hex: "#1C1917"),
        border: Color(hex: "#44403C"),
        panelStroke: Color(hex: "#57534E"),
        ink: Color(hex: "#FAFAF9"),
        muted: Color(hex: "#A8A29E"),
        faint: Color(hex: "#78716C"),
        positive: Color(hex: "#22C55E"),
        negative: Color(hex: "#F87171")
    )

    /// `SVC-SETUP-v6` — teal on services canvas.
    static let service = BusinessSetupDocTheme(
        accent: Color(hex: "#0D9488"),
        canvas: Color(hex: "#042F2E"),
        card: Color(hex: "#134E4A"),
        inset: Color(hex: "#042F2E"),
        border: Color(hex: "#115E59"),
        panelStroke: Color(hex: "#0F766E"),
        ink: Color(hex: "#F0FDFA"),
        muted: Color(hex: "#5EEAD4"),
        faint: Color(hex: "#64748B"),
        positive: Color(hex: "#22C55E"),
        negative: Color(hex: "#F87171")
    )

    static func theme(for vertical: BusinessOperatingVertical) -> BusinessSetupDocTheme {
        switch vertical {
        case .distribution: return .distribution
        case .clientCampaign: return .clientCampaign
        case .ownBusinessCampaign: return .ownBusinessCampaign
        case .retailStore: return .retail
        case .restaurantFood: return .restaurant
        case .consumerService: return .service
        case .standardProduction, .madeToOrder:
            return BusinessSetupDocTheme(
                accent: Color(hex: "#F59E0B"),
                canvas: Color(hex: "#18181B"),
                card: Color(hex: "#27272A"),
                inset: Color(hex: "#18181B"),
                border: Color(hex: "#3F3F46"),
                panelStroke: Color(hex: "#52525B"),
                ink: Color(hex: "#F8FAFC"),
                muted: Color(hex: "#A1A1AA"),
                faint: Color(hex: "#71717A"),
                positive: Color(hex: "#22C55E"),
                negative: Color(hex: "#F87171")
            )
        case .trading: return .trading
        }
    }
}

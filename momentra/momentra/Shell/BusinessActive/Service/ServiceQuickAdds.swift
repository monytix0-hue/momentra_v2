import SwiftUI

/// Quick Add hub — Figma `SVC-HUB-v5` `2401:29887`.
/// Port of Android `ServiceQuickAddHub` / `ServiceCoreTiles` · Support · Utilities.
///
/// **Utilities · 3 decision:** Figma hub `2401:29887` shows only Core · 9 + Support · 5
/// (no Hold / Transfer / Reprint). Those sheets remain required SVC screens
/// (`2533:29334` / `29448` / `29538`). Keep a compact Utilities row for discovery —
/// same as Android `ConsumerServiceQuickAdds` — rather than burying HTR under Support
/// (which would break the Figma Support · 5 count) or Pulse-only deep-links.
struct ServiceHubScreen: View {
    @ObservedObject var model: BusinessOperatingModel
    let theme: OperatingTheme
    var onCapture: (String) -> Void
    var onOpenMasters: (OperatingMasterTab) -> Void
    var onOpenStory: () -> Void = {}

    var body: some View {
        RetailFamilyHubScreen(
            theme: theme,
            eyebrow: "CONSUMER SERVICES",
            headline: ServiceCaptureCopy.tagline,
            heroBody: "Booking and completion are not revenue. Finalized charge posts once. Deposits stay distinct until allocated.",
            heroCta: "+  Finalize Charge",
            heroCapture: "SERVICE",
            whyNote: "Boundaries: book→assign→perform→finalize charge→collect. Appointment state ≠ financial state. Empty = Unavailable.",
            core: Self.core,
            support: Self.support,
            utilities: Self.utilities,
            coreSection: "Core · 9",
            supportSection: "Support · 5",
            utilitySection: "Utilities · 3",
            model: model,
            onCapture: onCapture,
            onOpenMasters: onOpenMasters,
            onOpenStory: onOpenStory
        )
    }

    /// Figma SVC-HUB-v5 `2401:29887` tile labels (Android `ServiceCoreTiles` parity).
    static let core: [RetailFamilyHubTile] = [
        .capture(code: "SERVICE", title: "Charge", subtitle: "Finalize / walk-in", glyph: "checkmark.seal", requires: "SERVICE"),
        .capture(code: "PURCHASE", title: "Purchase", subtitle: "Supplies · receive", glyph: "cart", requires: "PURCHASE"),
        .capture(code: "COLLECTION", title: "Collection", subtitle: "Charge AR", glyph: "arrow.down.circle", requires: "COLLECTION"),
        .capture(code: "PAYMENT", title: "Payment", subtitle: "Settle AP", glyph: "creditcard", requires: "PAYMENT"),
        .capture(code: "SPEND", title: "Expense", subtitle: "Service running", glyph: "wallet.pass", requires: "SPEND"),
        .capture(code: "STOCK", title: "Stock", subtitle: "Supply count", glyph: "shippingbox", requires: "STOCK"),
        .capture(code: "APPOINTMENT", title: "Booking", subtitle: "Appoint / walk-in", glyph: "calendar", requires: "APPOINTMENT"),
        .capture(code: "STATUS", title: "Status", subtitle: "Start · complete", glyph: "flag", requires: "APPOINTMENT"),
        .capture(code: "BUDGET", title: "Budget", subtitle: "Versioned revise", glyph: "target", requires: "BUDGET"),
    ]

    static let support: [RetailFamilyHubTile] = [
        .masters(tab: .principals, title: "Clients", subtitle: "Guests · patients", glyph: "person.2"),
        .masters(tab: .catalogue, title: "Services", subtitle: "Offerings · packages", glyph: "tag"),
        .masters(tab: .locations, title: "Rooms", subtitle: "Rooms · chairs · staff", glyph: "building.2"),
        .masters(tab: .invite, title: "Invite & QR", subtitle: "Join · people · roles", glyph: "qrcode", wide: true),
        .capture(code: "ISSUE", title: "Issue", subtitle: "Update · approval", glyph: "exclamationmark.triangle", requires: "UPDATE", wide: true),
    ]

    static let utilities: [RetailFamilyHubTile] = [
        .capture(code: "HOLD", title: "Hold", subtitle: "Park booking", glyph: "pause.circle", requires: "APPOINTMENT"),
        .capture(code: "TRANSFER", title: "Transfer", subtitle: "Between rooms", glyph: "arrow.left.arrow.right", requires: "APPOINTMENT"),
        .capture(code: "REPRINT", title: "Reprint", subtitle: "Receipt · bill", glyph: "printer", requires: "SERVICE"),
    ]
}

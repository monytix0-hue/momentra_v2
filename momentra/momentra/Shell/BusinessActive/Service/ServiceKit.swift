import SwiftUI

/// Consumer Service kit — Figma `SVC-HUB-v5` `2401:29887`.
/// Port of Android `ConsumerServiceActive` / `ConsumerServiceQuickAdds`.
/// Hero = Finalize Charge; Booking is a distinct non-revenue tile.
/// Moments/Life/Memory card IA lives in `RetailFamilyActiveScreens` (Clients & Services / Rooms & Staff).

struct ServiceActiveView: View {
    @ObservedObject var model: BusinessOperatingModel
    let destination: BottomDestination
    let refreshToken: UInt64
    var onOpenQuickAdd: () -> Void = {}
    var onOpenStory: () -> Void = {}
    var onOpenMasters: (OperatingMasterTab) -> Void = { _ in }
    var onCapture: ((String) -> Void)? = nil

    var body: some View {
        RetailFamilyActiveView(
            model: model,
            destination: destination,
            refreshToken: refreshToken,
            onOpenQuickAdd: onOpenQuickAdd,
            onOpenStory: onOpenStory,
            onOpenMasters: onOpenMasters,
            onCapture: onCapture,
            remapCapture: { code in
                switch code.uppercased() {
                case "SALE": return "SERVICE"
                default: return code
                }
            }
        ) {
            ServiceHubScreen(
                model: model,
                theme: OperatingTheme(vertical: model.vertical),
                onCapture: { onCapture?($0) ?? onOpenQuickAdd() },
                onOpenMasters: onOpenMasters,
                onOpenStory: onOpenStory
            )
        }
    }
}

struct ServiceHubScreen: View {
    @ObservedObject var model: BusinessOperatingModel
    let theme: OperatingTheme
    var onCapture: (String) -> Void
    var onOpenMasters: (OperatingMasterTab) -> Void
    var onOpenStory: () -> Void = {}

    var body: some View {
        RetailFamilyHubScreen(
            theme: theme,
            eyebrow: "CONSUMER SERVICE",
            headline: "Book. Charge. Settle.",
            heroBody: "A booking is not revenue until the charge is finalized. Named clients are required for credit. Settlement never reposts the charge.",
            heroCta: "+  Finalize Charge",
            heroCapture: "SERVICE",
            whyNote: "Boundaries: finalized charge = revenue once. Booking ≠ charge. Collection/payment allocate — never duplicate facts. Empty analytics = Unavailable.",
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

    /// Android `ServiceCoreTiles` — Finalize Charge + Booking distinct.
    static let core: [RetailFamilyHubTile] = [
        .capture(code: "SERVICE", title: "Finalize Charge", subtitle: "Service · GST", glyph: "checkmark.seal", requires: "SERVICE"),
        .capture(code: "PURCHASE", title: "Purchase", subtitle: "Goods · receive", glyph: "cart", requires: "PURCHASE"),
        .capture(code: "COLLECTION", title: "Collection", subtitle: "Allocate open AR", glyph: "arrow.down.circle", requires: "COLLECTION"),
        .capture(code: "PAYMENT", title: "Payment", subtitle: "Settle open AP", glyph: "creditcard", requires: "PAYMENT"),
        .capture(code: "SPEND", title: "Expense", subtitle: "Store running", glyph: "wallet.pass", requires: "SPEND"),
        .capture(code: "STOCK", title: "Stock", subtitle: "Count · damage", glyph: "shippingbox", requires: "STOCK"),
        .capture(code: "APPOINTMENT", title: "Booking", subtitle: "Schedule · room", glyph: "calendar", requires: "APPOINTMENT"),
        .capture(code: "STATUS", title: "Status", subtitle: "Room · staff · hold", glyph: "flag", requires: "APPOINTMENT"),
        .capture(code: "BUDGET", title: "Budget", subtitle: "Target · revise", glyph: "target", requires: "BUDGET"),
    ]

    static let support: [RetailFamilyHubTile] = [
        .masters(tab: .principals, title: "Clients", subtitle: "Customers · CRM", glyph: "person.2"),
        .masters(tab: .catalogue, title: "Services", subtitle: "Menu · rate card", glyph: "tag"),
        .masters(tab: .locations, title: "Rooms & Staff", subtitle: "Capacity · roster", glyph: "building.2"),
        .masters(tab: .invite, title: "Invite & QR", subtitle: "Join · people · roles", glyph: "qrcode", wide: true),
        .capture(code: "ISSUE", title: "Issue", subtitle: "Hold · transfer · reprint", glyph: "exclamationmark.triangle", requires: "UPDATE", wide: true),
    ]

    static let utilities: [RetailFamilyHubTile] = [
        .capture(code: "HOLD", title: "Hold", subtitle: "Park booking", glyph: "pause.circle", requires: "APPOINTMENT"),
        .capture(code: "TRANSFER", title: "Transfer", subtitle: "Between rooms", glyph: "arrow.left.arrow.right", requires: "UPDATE"),
        .capture(code: "REPRINT", title: "Reprint", subtitle: "Receipt · bill", glyph: "printer", requires: "SERVICE"),
    ]
}

import SwiftUI

/// Restaurant / Food kit — Figma `RST-HUB-v5` `2400:29892`.
/// Port of Android `RestaurantActive` / `RestaurantQuickAdds`. Sale → POS, Status → KOT.
/// Moments/Life/Memory card IA lives in `RetailFamilyActiveScreens` (ember theme via vertical).

struct RestaurantActiveView: View {
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
                case "SALE": return "POS"
                case "STATUS", "ORDER": return code.uppercased() == "STATUS" ? "KOT" : code
                default: return code
                }
            }
        ) {
            RestaurantHubScreen(
                model: model,
                theme: OperatingTheme(vertical: model.vertical),
                onCapture: { onCapture?($0) ?? onOpenQuickAdd() },
                onOpenMasters: onOpenMasters,
                onOpenStory: onOpenStory
            )
        }
    }
}

struct RestaurantHubScreen: View {
    @ObservedObject var model: BusinessOperatingModel
    let theme: OperatingTheme
    var onCapture: (String) -> Void
    var onOpenMasters: (OperatingMasterTab) -> Void
    var onOpenStory: () -> Void = {}

    var body: some View {
        RetailFamilyHubScreen(
            theme: theme,
            eyebrow: "RESTAURANT / FOOD",
            headline: "Order. Serve. Collect.",
            heroBody: "Order and preparation are not revenue. Posted bill owns the sale. Platform AR needs an explicit settlement link.",
            heroCta: "+  Record Sale",
            heroCapture: "POS",
            whyNote: "Boundaries: order→prep→ready→fulfil→bill posted→collect. Spoilage and aggregator fees must not duplicate facts. Empty = Unavailable.",
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

    /// Android `RestaurantCoreTiles` — Sale opens POS, Status opens KOT.
    static let core: [RetailFamilyHubTile] = [
        .capture(code: "POS", title: "Sale", subtitle: "Bill / direct sale", glyph: "fork.knife", requires: "SALE"),
        .capture(code: "PURCHASE", title: "Purchase", subtitle: "Ingredients", glyph: "cart", requires: "PURCHASE"),
        .capture(code: "COLLECTION", title: "Collection", subtitle: "AR · platform", glyph: "arrow.down.circle", requires: "COLLECTION"),
        .capture(code: "PAYMENT", title: "Payment", subtitle: "Settle AP", glyph: "creditcard", requires: "PAYMENT"),
        .capture(code: "SPEND", title: "Expense", subtitle: "Kitchen · platform", glyph: "wallet.pass", requires: "SPEND"),
        .capture(code: "STOCK", title: "Stock", subtitle: "Count · waste", glyph: "shippingbox", requires: "STOCK"),
        .capture(code: "ORDER", title: "Order", subtitle: "Food / catering", glyph: "list.bullet.rectangle", requires: "ORDER"),
        .capture(code: "KOT", title: "Status", subtitle: "Prep · fulfil", glyph: "flame", requires: "ORDER"),
        .capture(code: "BUDGET", title: "Budget", subtitle: "Versioned revise", glyph: "target", requires: "BUDGET"),
    ]

    static let support: [RetailFamilyHubTile] = [
        .masters(tab: .principals, title: "Vendors", subtitle: "Suppliers", glyph: "person.2"),
        .masters(tab: .catalogue, title: "Menu", subtitle: "Items · modifiers", glyph: "book.closed"),
        .masters(tab: .locations, title: "Stations", subtitle: "Prep · pass", glyph: "square.grid.3x3"),
        .masters(tab: .invite, title: "Invite & QR", subtitle: "Join Moment", glyph: "qrcode", wide: true),
        .capture(code: "ISSUE", title: "Issue", subtitle: "Update · approval", glyph: "exclamationmark.triangle", requires: "UPDATE", wide: true),
    ]

    static let utilities: [RetailFamilyHubTile] = [
        .capture(code: "HOLD", title: "Hold", subtitle: "Park order", glyph: "pause.circle", requires: "ORDER"),
        .capture(code: "TRANSFER", title: "Transfer", subtitle: "Between stores", glyph: "arrow.left.arrow.right", requires: "STOCK"),
        .capture(code: "REPRINT", title: "Reprint", subtitle: "Receipt · bill", glyph: "printer", requires: "SALE"),
    ]
}

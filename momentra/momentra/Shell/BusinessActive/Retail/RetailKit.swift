import SwiftUI

/// Retail Store kit — Figma `RTL-HUB-v5` `2399:29887` (+ Pulse/Moments/Life/Memory frames).
/// Port of Android `RetailStoreActive` / `RetailQuickAdds`.
/// Moments/Life/Memory card IA lives in `RetailFamilyActiveScreens`.
///
/// **Utilities · 3 decision:** Figma hub shows Core · 9 + Support · 5 only. Hold /
/// Transfer / Reprint remain required RTL screens (`2508:29333` / `29447` / `29537`).
/// Keep a muted Utilities row for HTR discovery (same as Service) — do not delete.

struct RetailActiveView: View {
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
            onCapture: onCapture
        ) {
            RetailHubScreen(
                model: model,
                theme: OperatingTheme(vertical: model.vertical),
                onCapture: { onCapture?($0) ?? onOpenQuickAdd() },
                onOpenMasters: onOpenMasters,
                onOpenStory: onOpenStory
            )
        }
    }
}

struct RetailHubScreen: View {
    @ObservedObject var model: BusinessOperatingModel
    let theme: OperatingTheme
    var onCapture: (String) -> Void
    var onOpenMasters: (OperatingMasterTab) -> Void
    var onOpenStory: () -> Void = {}

    var body: some View {
        RetailFamilyHubScreen(
            theme: theme,
            eyebrow: "RETAIL STORE",
            headline: "Stock. Sell. Settle.",
            heroBody: "Quick amount never moves SKU stock. Credit sales need a named customer. Settlement never reposts the sale.",
            heroCta: "+  Record Sale",
            heroCapture: "SALE",
            whyNote: "Boundaries: posted sale = revenue once. Quick amount ≠ SKU movement. Order is commitment only. Collection/payment allocate — never duplicate facts. Empty analytics = Unavailable.",
            core: Self.core,
            support: Self.support,
            utilities: Self.utilities,
            coreSection: "Core · 9",
            supportSection: "Support · 5",
            utilitySection: "Utilities · 3",
            coreTrailing: "Retail journey",
            supportTrailing: "Network · masters",
            model: model,
            onCapture: onCapture,
            onOpenMasters: onOpenMasters,
            onOpenStory: onOpenStory
        )
    }

    /// Android `RetailCoreTiles` / `RetailUtilityTiles` order and captions (Phase 7/8 lock).
    static let core: [RetailFamilyHubTile] = [
        .capture(code: "SALE", title: "Sale", subtitle: "Quick or itemized", glyph: "bag", requires: "SALE"),
        .capture(code: "PURCHASE", title: "Purchase", subtitle: "Goods · receive", glyph: "cart", requires: "PURCHASE"),
        .capture(code: "COLLECTION", title: "Collection", subtitle: "Allocate open AR", glyph: "arrow.down.circle", requires: "COLLECTION"),
        .capture(code: "PAYMENT", title: "Payment", subtitle: "Settle open AP", glyph: "creditcard", requires: "PAYMENT"),
        .capture(code: "SPEND", title: "Expense", subtitle: "Running cost", glyph: "wallet.pass", requires: "SPEND"),
        .capture(code: "STOCK", title: "Stock", subtitle: "Count · damage", glyph: "shippingbox", requires: "STOCK"),
        .capture(code: "ORDER", title: "Order", subtitle: "Optional customer", glyph: "list.bullet.rectangle", requires: "ORDER"),
        .capture(code: "STATUS", title: "Status", subtitle: "Dispatch / fulfil", glyph: "flag", requires: "ORDER"),
        .capture(code: "BUDGET", title: "Budget", subtitle: "Versioned revise", glyph: "target", requires: "BUDGET"),
    ]

    static let support: [RetailFamilyHubTile] = [
        .masters(tab: .principals, title: "Suppliers", subtitle: "Vendors", glyph: "person.2"),
        .masters(tab: .catalogue, title: "Catalogue", subtitle: "SKU · UOM", glyph: "tag"),
        .masters(tab: .locations, title: "Locations", subtitle: "Stores · aisles", glyph: "map"),
        .masters(tab: .invite, title: "Invite & QR", subtitle: "Join · people · roles", glyph: "qrcode", wide: true),
        .capture(code: "ISSUE", title: "Issue", subtitle: "Update · approval", glyph: "exclamationmark.triangle", requires: "UPDATE", wide: true),
    ]

    static let utilities: [RetailFamilyHubTile] = [
        // Hold parks a POS draft — gate on SALE (Android `RetailUtilityTiles`), not ORDER.
        .capture(code: "HOLD", title: "Hold", subtitle: "Park bill", glyph: "pause.circle", requires: "SALE"),
        .capture(code: "TRANSFER", title: "Transfer", subtitle: "Between stores", glyph: "arrow.left.arrow.right", requires: "STOCK"),
        .capture(code: "REPRINT", title: "Reprint", subtitle: "Receipt · bill", glyph: "printer", requires: "SALE"),
    ]
}

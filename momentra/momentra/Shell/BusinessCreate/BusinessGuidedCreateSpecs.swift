import Foundation

/// Copy for Figma `Create a moment · Guided business selection` (`2:1514`).
/// Keep in lockstep with `BusinessGuidedCreateSpecs.kt` on Android.
struct BusinessGuidedCreateOption: Identifiable, Equatable {
    let id: String
    let titleLine1: String
    let titleLine2: String
    let meaning: String
    let iconKey: String
    let familyCode: String
    let momentTypeCode: String
    let previewEyebrow: String
    let previewHeadline: String
    let previewBody: String
    let capabilities: [(iconKey: String, label: String)]

    var title: String { "\(titleLine1) \(titleLine2)" }

    static func == (lhs: BusinessGuidedCreateOption, rhs: BusinessGuidedCreateOption) -> Bool {
        lhs.id == rhs.id
    }
}

enum BusinessGuidedCreateSpecs {
    static let questionLine1 = "What kind of"
    static let questionLine2 = "business do you run?"
    static let subtitle = "Choose a type. We'll tailor your moment."
    static let continueLabel = "Continue"
    static let footnote = "You can change this later in Manage moment."

    static let options: [BusinessGuidedCreateOption] = [
        .init(
            id: "marketing",
            titleLine1: "Marketing &",
            titleLine2: "Campaigns",
            meaning: "Ads, promotions & leads",
            iconKey: "megaphone",
            familyCode: "MARKETING_CAMPAIGNS",
            momentTypeCode: "CLIENT_CAMPAIGN",
            previewEyebrow: "YOUR MARKETING MOMENT",
            previewHeadline: "Built for the way you promote.",
            previewBody: "Run client campaigns or your own ads with budgets, channels, and lead tracking in one place.",
            capabilities: [
                ("wallet", "Campaign budgets"),
                ("target", "Lead tracking"),
                ("users", "Client retainers"),
                ("badge-check", "Creative approvals"),
            ]
        ),
        .init(
            id: "trading",
            titleLine1: "Trading &",
            titleLine2: "Distribution",
            meaning: "Bulk buying & selling",
            iconKey: "truck",
            familyCode: "TRADING_DISTRIBUTION",
            momentTypeCode: "TRADING_WHOLESALE",
            previewEyebrow: "YOUR TRADING MOMENT",
            previewHeadline: "Built for the way you move goods.",
            previewBody: "Buy in bulk, sell wholesale, and track collections across dealers and routes.",
            capabilities: [
                ("receipt", "Purchase orders"),
                ("route", "Shipments & tracking"),
                ("banknote", "Collections"),
                ("package", "Stock positions"),
            ]
        ),
        .init(
            id: "retail",
            titleLine1: "Retail &",
            titleLine2: "Consumer",
            meaning: "Shop & counter sales",
            iconKey: "store",
            familyCode: "RETAIL_CONSUMER",
            momentTypeCode: "RETAIL_STORE",
            previewEyebrow: "YOUR RETAIL MOMENT",
            previewHeadline: "Built for the way you sell.",
            previewBody: "Counter sales, inventory, and day-close numbers for a shop or consumer brand.",
            capabilities: [
                ("camera", "POS sales"),
                ("package", "Shelf stock"),
                ("users", "Customer tabs"),
                ("calculator", "Day close"),
            ]
        ),
        .init(
            id: "manufacturing",
            titleLine1: "Manufacturing &",
            titleLine2: "Production",
            meaning: "Batch & job work",
            iconKey: "factory",
            familyCode: "MANUFACTURING_PRODUCTION",
            momentTypeCode: "STANDARD_PRODUCTION",
            previewEyebrow: "YOUR MANUFACTURING MOMENT",
            previewHeadline: "Built for the way you make.",
            previewBody: "Make products in batches from raw materials or manage made-to-order job work.",
            capabilities: [
                ("clipboard-list", "Quotes & job orders"),
                ("factory", "Production & WIP"),
                ("layers", "BOM & materials"),
                ("receipt-text", "Invoices against orders"),
            ]
        ),
    ]

    static func option(id: String) -> BusinessGuidedCreateOption {
        options.first(where: { $0.id == id }) ?? options[3]
    }
}

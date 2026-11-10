import Foundation
import SwiftUI

// MARK: - Vertical

/// The two operating verticals this build ships. Both read the same projection; they differ in
/// accent, copy, and which noun the story uses.
enum BusinessOperatingVertical: String, CaseIterable {
    case trading = "TRADING_WHOLESALE"
    case distribution = "DISTRIBUTION_DEALERSHIP"
    case standardProduction = "STANDARD_PRODUCTION"
    case madeToOrder = "MADE_TO_ORDER"
    case clientCampaign = "CLIENT_CAMPAIGN"
    case ownBusinessCampaign = "OWN_BUSINESS_CAMPAIGN"
    case retailStore = "RETAIL_STORE"
    case restaurantFood = "RESTAURANT_FOOD"
    case consumerService = "CONSUMER_SERVICE"

    var domain: BusinessDomain {
        switch self {
        case .trading: return .trading
        case .distribution: return .distribution
        case .standardProduction, .madeToOrder: return .manufacturing
        case .clientCampaign: return .marketing
        case .ownBusinessCampaign: return .marketingOwn
        case .retailStore: return .retail
        case .restaurantFood: return .restaurant
        case .consumerService: return .services
        }
    }

    var accent: Color { BusinessTokens.Colors.accent(for: domain) }

    var canvas: Color {
        switch self {
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

    var surface: Color {
        switch self {
        case .trading: return BusinessTokens.Colors.tradingSurface
        case .distribution: return BusinessTokens.Colors.distributionSurface
        case .standardProduction, .madeToOrder: return BusinessTokens.Colors.manufacturingSurface
        case .clientCampaign: return BusinessTokens.Colors.marketingSurface
        case .ownBusinessCampaign: return BusinessTokens.Colors.marketingOwnSurface
        case .retailStore: return BusinessTokens.Colors.retailSurface
        case .restaurantFood: return BusinessTokens.Colors.restaurantSurface
        case .consumerService: return BusinessTokens.Colors.servicesSurface
        }
    }

    var inset: Color {
        switch self {
        case .trading: return BusinessTokens.Colors.tradingInset
        case .distribution: return BusinessTokens.Colors.distributionInset
        case .standardProduction, .madeToOrder: return BusinessTokens.Colors.manufacturingInset
        case .clientCampaign: return BusinessTokens.Colors.marketingInset
        case .ownBusinessCampaign: return BusinessTokens.Colors.marketingOwnInset
        case .retailStore: return BusinessTokens.Colors.retailInset
        case .restaurantFood: return BusinessTokens.Colors.restaurantInset
        case .consumerService: return BusinessTokens.Colors.servicesInset
        }
    }

    var border: Color { BusinessTokens.Colors.borderDefault }
    var ink: Color { BusinessTokens.Colors.textPrimary }
    var body: Color { BusinessTokens.Colors.textSecondary }
    var muted: Color { BusinessTokens.Colors.textMuted }
    var onAccent: Color {
        switch self {
        case .trading: return BusinessTokens.Colors.tradingOnAccent
        case .distribution: return BusinessTokens.Colors.distributionOnAccent
        case .standardProduction, .madeToOrder: return BusinessTokens.Colors.manufacturingOnAccent
        case .clientCampaign: return BusinessTokens.Colors.marketingOnAccent
        case .ownBusinessCampaign: return BusinessTokens.Colors.marketingOwnOnAccent
        case .retailStore: return BusinessTokens.Colors.retailOnAccent
        case .restaurantFood: return BusinessTokens.Colors.restaurantOnAccent
        case .consumerService: return BusinessTokens.Colors.servicesOnAccent
        }
    }

    var partyNoun: String {
        switch self {
        case .trading: return "Buyers"
        case .distribution: return "Dealers"
        case .standardProduction: return "Buyers"
        case .madeToOrder: return "Customers"
        case .clientCampaign: return "Clients"
        case .ownBusinessCampaign: return "Brand"
        case .retailStore: return "Customers"
        case .restaurantFood: return "Guests"
        case .consumerService: return "Clients"
        }
    }

    var motionLine: String {
        switch self {
        case .trading: return "Source. Sell. Collect."
        case .distribution: return "Source. Move. Settle."
        case .standardProduction: return "Plan. Make. Fulfil."
        case .madeToOrder: return "Quote. Confirm. Make. Invoice."
        case .clientCampaign: return "Plan. Spend. Report."
        case .ownBusinessCampaign: return "Plan. Promote. Grow."
        case .retailStore: return "Stock. Sell. Collect."
        case .restaurantFood: return "Prep. Serve. Collect."
        case .consumerService: return "Book. Serve. Charge."
        }
    }

    var storyNoun: String {
        switch self {
        case .trading: return "trading"
        case .distribution: return "distribution"
        case .standardProduction: return "manufacturing"
        case .madeToOrder: return "make-to-order"
        case .clientCampaign: return "client"
        case .ownBusinessCampaign: return "own"
        case .retailStore: return "retail"
        case .restaurantFood: return "restaurant"
        case .consumerService: return "service"
        }
    }

    var isMarketing: Bool {
        self == .clientCampaign || self == .ownBusinessCampaign
    }

    var isManufacturing: Bool {
        self == .standardProduction || self == .madeToOrder
    }

    var isRetailFamily: Bool {
        self == .retailStore || self == .restaurantFood || self == .consumerService
    }

    var isMadeToOrder: Bool { self == .madeToOrder }

    /// Host kit selection — retail-family, marketing, and MTO own hubs; MFG stays on manufacturing kit.
    var kit: OperatingKit {
        switch self {
        case .trading: return .tradingV12
        case .distribution: return .distributionV12
        case .standardProduction: return .manufacturing
        case .madeToOrder: return .madeToOrder
        case .clientCampaign, .ownBusinessCampaign: return .marketing
        case .retailStore: return .retail
        case .restaurantFood: return .restaurant
        case .consumerService: return .service
        }
    }

    /// The eight chapters the backend composes for this vertical.
    var storyPages: [String] {
        switch self {
        case .trading, .standardProduction:
            return ["Moment", "Motion", "Orders", "Money", "Stock", "Network", "Attention", "So far"]
        case .distribution: return ["Moment", "Motion", "Dealers", "Money", "Stock", "Network", "Attention", "So far"]
        case .madeToOrder:
            return ["Jobs", "One job", "Confirmed", "Money", "Production", "Customers", "Attention", "So far"]
        case .clientCampaign, .ownBusinessCampaign:
            return ["Moment", "Motion", "Spend", "Results", "Creative", "Network", "Attention", "So far"]
        case .retailStore, .restaurantFood, .consumerService:
            return ["Moment", "Motion", "Earned", "Money", "Work", "People", "Forward", "So far"]
        }
    }

    static func forSubtype(_ subtype: String?) -> BusinessOperatingVertical? {
        guard let subtype else { return nil }
        let code = subtype.uppercased()
        if let exact = BusinessOperatingVertical(rawValue: code) { return exact }
        if code.contains("MARKETING") { return .clientCampaign }
        if code.contains("MANUFACTURING") { return .standardProduction }
        if code.contains("RETAIL") { return .retailStore }
        if code.contains("RESTAURANT") || code.contains("FOOD") { return .restaurantFood }
        if code.contains("SERVICE") && !code.contains("CAMPAIGN") { return .consumerService }
        return nil
    }
}

/// Experience kit the host mounts for a subtype. Keeps later vertical swaps off ad-hoc `if` trees.
enum OperatingKit: String, CaseIterable, Equatable {
    case tradingV12
    case distributionV12
    case manufacturing
    case madeToOrder
    case marketing
    case retail
    case restaurant
    case service
}

// MARK: - Sheet catalogue

/// One `View all ›` surface. The eighteen per vertical map onto a board section `code`, or onto a
/// Pulse metric group when `pulseSurface` is true.
struct OperatingSheetSpec: Identifiable, Equatable {
    let code: String
    let title: String
    let eyebrow: String
    let emptyTitle: String
    let emptyBody: String
    var pulseSurface: Bool = false

    var id: String { code }

    /// Figma `DST-SHEET` / `TRD-SHEET` node id for this surface.
    var figmaId: String { OperatingSheetChrome.figmaId(for: code) }
    /// Status chips under the period row (All / Open / Done, …).
    var statusFilters: [String] { OperatingSheetChrome.statusFilters(for: code) }
    /// Sticky footer primary CTA label.
    var footerCta: String { OperatingSheetChrome.footerCta(for: code) }
    var periodFilters: [String] { OperatingSheetChrome.periodFilters }
}

/// Sticky-header chrome shared by every view-all sheet (`DST-SHEET` / `TRD-SHEET`).
enum OperatingSheetChrome {
    static let periodFilters = ["This month", "Last month", "Custom range ›"]

    static func figmaId(for code: String) -> String {
        switch code {
        case "TARGET_BUDGET": return "2561:29337"
        case "FINANCE": return "2561:29430"
        case "PAYMENT_READINESS": return "2561:29507"
        case "ATTENTION": return "2561:29581"
        case "ACTIVITY": return "2561:29673"
        case "GST": return "2561:29759"
        case "orders": return "2561:29845"
        case "stock": return "2561:29941"
        case "network": return "2561:30031"
        case "M4": return "2561:30116"
        case "L1": return "2561:30214"
        case "L2": return "2561:30294"
        case "L3": return "2561:30398"
        case "L4": return "2561:30492"
        case "ME1": return "2561:30588"
        case "ME2": return "2561:30680"
        case "fulfilment": return "2561:30773"
        case "ME4": return "2561:30852"
        default: return ""
        }
    }

    static func statusFilters(for code: String) -> [String] {
        switch code {
        case "FINANCE", "PAYMENT_READINESS", "ATTENTION", "orders", "sales", "purchases", "services", "appointments", "M4", "L3":
            return ["Open", "Overdue", "Settled"]
        case "ME1", "ME2", "ME3", "fulfilment", "ME4":
            return ["All", "Open", "Settled"]
        case "production":
            return ["All", "Open", "Done"]
        default:
            return ["All", "Open", "Done"]
        }
    }

    static func footerCta(for code: String) -> String {
        switch code {
        case "PAYMENT_READINESS", "L3": return "Record payment"
        case "ATTENTION": return "Resolve all"
        case "orders", "sales", "purchases", "services", "appointments", "M4": return "Resolve commitments"
        case "production": return "Open Quick Add"
        case "ME1", "ME2", "ME3", "fulfilment", "ME4": return "Open Memory"
        default: return "Open Quick Add"
        }
    }

    /// Footer CTA → capture mode or chrome action the host should take.
    static func footerAction(for code: String) -> String {
        switch footerCta(for: code) {
        case "Record payment": return "PAYMENT"
        case "Resolve all", "Resolve commitments": return "ATTENTION"
        case "Open Memory": return "MEMORY"
        default: return "QUICK_ADD"
        }
    }
}

/// Shared view-all surface catalogue.
/// Trading/Distribution keep the original eighteen (6 pulse + 12 board). Marketing adds five
/// board codes (`M1`/`M2`/`M3`/`L5`/`ME3`) that the campaign projection already emits.
/// Manufacturing adds `production` (plant/job output) in place of trading `network`.
/// Copy still lives in the vertical via `spec(forCode:vertical:)`.
enum OperatingSheetCatalog {
    static let all: [OperatingSheetSpec] = [
        OperatingSheetSpec(
            code: "TARGET_BUDGET",
            title: "Target & Budget",
            eyebrow: "What this period is aiming at",
            emptyTitle: "No target set",
            emptyBody: "Set a sales target and a period budget to measure the business against them.",
            pulseSurface: true
        ),
        OperatingSheetSpec(
            code: "FINANCE",
            title: "Finance",
            eyebrow: "Recorded sales, money in, money still out",
            emptyTitle: "No finance recorded",
            emptyBody: "Sales, collections, and payables appear once recorded.",
            pulseSurface: true
        ),
        OperatingSheetSpec(
            code: "PAYMENT_READINESS",
            title: "Payment Readiness",
            eyebrow: "What can be collected and what must be paid",
            emptyTitle: "Nothing awaiting settlement",
            emptyBody: "Open receivables and payables appear here as they are recorded.",
            pulseSurface: true
        ),
        OperatingSheetSpec(
            code: "ATTENTION",
            title: "Needs Attention",
            eyebrow: "Overdue, blocked, and waiting on a decision",
            emptyTitle: "Nothing is waiting",
            emptyBody: "Approvals, overdue items, and blockers will show here.",
            pulseSurface: true
        ),
        OperatingSheetSpec(
            code: "ACTIVITY",
            title: "Activity",
            eyebrow: "Everything recorded in this moment",
            emptyTitle: "Nothing recorded yet",
            emptyBody: "Sales, purchases, and money collection will show here.",
            pulseSurface: true
        ),
        OperatingSheetSpec(
            code: "GST",
            title: "GST Position",
            eyebrow: "Indicative tax on recorded records",
            emptyTitle: "No GST recorded",
            emptyBody: "GST appears on records once tracking is on and a rate is supplied.",
            pulseSurface: true
        ),
        OperatingSheetSpec(
            code: "orders",
            title: "Orders & Commitments",
            eyebrow: "Commitments are not revenue until billed",
            emptyTitle: "No orders yet",
            emptyBody: "Buyer orders and their dispatches will show here."
        ),
        OperatingSheetSpec(
            code: "stock",
            title: "Stock & Products",
            eyebrow: "Owned on-hand only — consignment stays out",
            emptyTitle: "No stock recorded",
            emptyBody: "Open stock, purchases, and dispatches build the on-hand position."
        ),
        OperatingSheetSpec(
            code: "network",
            title: "Dealers, Principals & Routes",
            eyebrow: "Who the network runs through",
            emptyTitle: "No people yet",
            emptyBody: "Named dealers, principals, and routes appear as you record them."
        ),
        OperatingSheetSpec(
            code: "production",
            title: "Production",
            eyebrow: "Recorded vs accepted output",
            emptyTitle: "No production yet",
            emptyBody: "Production batches and accepted output appear here."
        ),
        OperatingSheetSpec(
            code: "sales",
            title: "Sales & Orders",
            eyebrow: "Posted sales — orders stay commitment until billed",
            emptyTitle: "No sales yet",
            emptyBody: "Posted sales and customer orders will show here."
        ),
        OperatingSheetSpec(
            code: "purchases",
            title: "Purchases",
            eyebrow: "Goods received · payable until settled",
            emptyTitle: "No purchases yet",
            emptyBody: "Supplier purchases and receipts will show here."
        ),
        OperatingSheetSpec(
            code: "channels",
            title: "People & Network",
            eyebrow: "Customers, suppliers and channels",
            emptyTitle: "No network yet",
            emptyBody: "Named customers, suppliers, and channels appear as you record them."
        ),
        OperatingSheetSpec(
            code: "customers",
            title: "Customers & Network",
            eyebrow: "Named clients and channels",
            emptyTitle: "No customers yet",
            emptyBody: "Named clients and channels appear as you record them."
        ),
        OperatingSheetSpec(
            code: "services",
            title: "Services",
            eyebrow: "Finalized charges — revenue once",
            emptyTitle: "No services yet",
            emptyBody: "Finalized service charges will show here."
        ),
        OperatingSheetSpec(
            code: "appointments",
            title: "Appointments / Jobs",
            eyebrow: "Bookings are not revenue until charged",
            emptyTitle: "No appointments yet",
            emptyBody: "Bookings and job slots will show here."
        ),
        OperatingSheetSpec(
            code: "M1",
            title: "Campaign / Moment",
            eyebrow: "The campaign identity this period",
            emptyTitle: "No campaign moment yet",
            emptyBody: "Campaign name, client / brand, and brief appear here."
        ),
        OperatingSheetSpec(
            code: "M2",
            title: "People",
            eyebrow: "Who is on this campaign",
            emptyTitle: "No people yet",
            emptyBody: "Named participants appear as you invite them."
        ),
        OperatingSheetSpec(
            code: "M3",
            title: "Channels & Network",
            eyebrow: "Channels and counterparties in play",
            emptyTitle: "No channels yet",
            emptyBody: "Named channels and partners appear as you record them."
        ),
        OperatingSheetSpec(
            code: "M4",
            title: "Open Commitments",
            eyebrow: "What is still owed in either direction",
            emptyTitle: "No open commitments",
            emptyBody: "Open orders, unsettled money, and dispatches in transit appear here."
        ),
        OperatingSheetSpec(
            code: "L1",
            title: "Current Business",
            eyebrow: "The network today",
            emptyTitle: "Nothing recorded yet",
            emptyBody: "Board updates and the live position will show here."
        ),
        OperatingSheetSpec(
            code: "L2",
            title: "Dealers & Channels",
            eyebrow: "Attributed sales by dealer and channel",
            emptyTitle: "No channels recorded",
            emptyBody: "Dealer-attributed sales appear once a channel is chosen."
        ),
        OperatingSheetSpec(
            code: "L3",
            title: "Money Moving",
            eyebrow: "Collected, paid, and the pay lag between",
            emptyTitle: "No settlement yet",
            emptyBody: "Collections and payments will show here."
        ),
        OperatingSheetSpec(
            code: "L4",
            title: "People & Activity",
            eyebrow: "Who recorded what",
            emptyTitle: "No activity yet",
            emptyBody: "Stock moves, issues, and updates will show here."
        ),
        OperatingSheetSpec(
            code: "L5",
            title: "People Today",
            eyebrow: "Who recorded what today",
            emptyTitle: "No activity today",
            emptyBody: "Today's updates and issues will show here."
        ),
        OperatingSheetSpec(
            code: "ME1",
            title: "Monthly Memory",
            eyebrow: "Each month this business has recorded",
            emptyTitle: "No month recorded",
            emptyBody: "Months appear once records exist in them."
        ),
        OperatingSheetSpec(
            code: "ME2",
            title: "Money Memory",
            eyebrow: "Collected, paid, and returned",
            emptyTitle: "No money memory",
            emptyBody: "Settlements and returns will show here."
        ),
        OperatingSheetSpec(
            code: "ME3",
            title: "People & Activity",
            eyebrow: "Who recorded what across the period",
            emptyTitle: "No people memory",
            emptyBody: "People and activity history will show here."
        ),
        OperatingSheetSpec(
            code: "fulfilment",
            title: "Fulfilment Memory",
            eyebrow: "Delivered, received, and how late",
            emptyTitle: "No fulfilment recorded",
            emptyBody: "Dispatch and receipt status build this picture."
        ),
        OperatingSheetSpec(
            code: "ME4",
            title: "Patterns & Decisions",
            eyebrow: "What repeats and what is waiting on approval",
            emptyTitle: "No pattern yet",
            emptyBody: "Repeated issues and pending approvals will show here."
        ),
    ]

    /// The six codes that read a Pulse metric group rather than a board section.
    static let pulseCodes: Set<String> = ["TARGET_BUDGET", "FINANCE", "PAYMENT_READINESS", "ATTENTION", "ACTIVITY", "GST"]

    static func spec(forCode code: String) -> OperatingSheetSpec? {
        all.first { $0.code == code }
    }

    /// Plant vs job-order / marketing copy for the shared board codes.
    static func spec(forCode code: String, vertical: BusinessOperatingVertical) -> OperatingSheetSpec? {
        guard var spec = spec(forCode: code) else { return nil }
        if vertical.isMarketing {
            switch code {
            case "M1":
                spec = OperatingSheetSpec(
                    code: spec.code,
                    title: "Campaign / Moment",
                    eyebrow: "The campaign identity this period",
                    emptyTitle: "No campaign moment yet",
                    emptyBody: "Campaign name, client / brand, and brief appear here.",
                    pulseSurface: false
                )
            case "M3":
                spec = OperatingSheetSpec(
                    code: spec.code,
                    title: "Channels & Network",
                    eyebrow: "Channels and counterparties in play",
                    emptyTitle: "No channels yet",
                    emptyBody: "Named channels and partners appear as you record them.",
                    pulseSurface: false
                )
            case "M4":
                spec = OperatingSheetSpec(
                    code: spec.code,
                    title: "Open Commitments",
                    eyebrow: "Open activities still in flight",
                    emptyTitle: "No open commitments",
                    emptyBody: "Open activities and unsettled money appear here.",
                    pulseSurface: false
                )
            case "L1":
                spec = OperatingSheetSpec(
                    code: spec.code,
                    title: "Current Campaign",
                    eyebrow: "As-of today · live position",
                    emptyTitle: "Nothing recorded yet",
                    emptyBody: "Campaign progress and coverage will show here.",
                    pulseSurface: false
                )
            case "L2":
                spec = OperatingSheetSpec(
                    code: spec.code,
                    title: "Channel Activity",
                    eyebrow: "Attributed results by channel",
                    emptyTitle: "No channels recorded",
                    emptyBody: "Channel-attributed results appear once a channel is chosen.",
                    pulseSurface: false
                )
            case "L4":
                spec = OperatingSheetSpec(
                    code: spec.code,
                    title: "Execution",
                    eyebrow: "Activities, progress, results, updates",
                    emptyTitle: "No activity yet",
                    emptyBody: "Activities, updates, and issues will show here.",
                    pulseSurface: false
                )
            case "L5":
                spec = OperatingSheetSpec(
                    code: spec.code,
                    title: "People Today",
                    eyebrow: "Who recorded what today",
                    emptyTitle: "No activity today",
                    emptyBody: "Today's updates and issues will show here.",
                    pulseSurface: false
                )
            case "ME1":
                spec = OperatingSheetSpec(
                    code: spec.code,
                    title: "Monthly Memory",
                    eyebrow: "Each month this campaign has recorded",
                    emptyTitle: "No month recorded",
                    emptyBody: "Months appear once records exist in them.",
                    pulseSurface: false
                )
            case "ME2":
                spec = OperatingSheetSpec(
                    code: spec.code,
                    title: "Money Memory",
                    eyebrow: vertical == .clientCampaign
                        ? "Invoices, collections, and spend"
                        : "Spend and settlements — no client AR",
                    emptyTitle: "No money memory",
                    emptyBody: "Settlements will show here.",
                    pulseSurface: false
                )
            case "ME3":
                spec = OperatingSheetSpec(
                    code: spec.code,
                    title: "People & Activity",
                    eyebrow: "Who recorded what across the campaign",
                    emptyTitle: "No people memory",
                    emptyBody: "People and activity history will show here.",
                    pulseSurface: false
                )
            case "ME4":
                spec = OperatingSheetSpec(
                    code: spec.code,
                    title: "Patterns & Decisions",
                    eyebrow: "What repeats and what is waiting on approval",
                    emptyTitle: "No pattern yet",
                    emptyBody: "Repeated issues and pending approvals will show here.",
                    pulseSurface: false
                )
            case "TARGET_BUDGET":
                spec = OperatingSheetSpec(
                    code: spec.code,
                    title: "Target & Budget",
                    eyebrow: "Results toward target · budget use",
                    emptyTitle: "No target set",
                    emptyBody: "Set a results target and period budget to measure the campaign.",
                    pulseSurface: true
                )
            case "FINANCE":
                spec = OperatingSheetSpec(
                    code: spec.code,
                    title: "Finance",
                    eyebrow: vertical == .clientCampaign
                        ? "Invoices, collections, eligible cost"
                        : "Attributed sales, spend, ROAS",
                    emptyTitle: "No finance recorded",
                    emptyBody: vertical == .clientCampaign
                        ? "Client invoices and collections appear once recorded."
                        : "Spend and attributed sales appear once recorded.",
                    pulseSurface: true
                )
            case "ACTIVITY":
                spec = OperatingSheetSpec(
                    code: spec.code,
                    title: "Activity",
                    eyebrow: "Everything recorded in this campaign",
                    emptyTitle: "Nothing recorded yet",
                    emptyBody: "Spend, results, and approvals will show here.",
                    pulseSurface: true
                )
            default:
                break
            }
            return spec
        }
        if vertical.isRetailFamily {
            switch code {
            case "stock":
                spec = OperatingSheetSpec(
                    code: spec.code,
                    title: "Stock & Supplies",
                    eyebrow: "On-hand · counts · low stock",
                    emptyTitle: "No stock recorded",
                    emptyBody: "Stock counts, purchases, and adjustments build the on-hand position.",
                    pulseSurface: false
                )
            case "sales":
                spec = OperatingSheetSpec(
                    code: spec.code,
                    title: vertical == .consumerService ? "Sales" : "Sales & Orders",
                    eyebrow: vertical == .consumerService
                        ? "Posted retail sales alongside service"
                        : "Posted sales — orders stay commitment until billed",
                    emptyTitle: "No sales yet",
                    emptyBody: "Posted sales and customer orders will show here.",
                    pulseSurface: false
                )
            case "channels":
                spec = OperatingSheetSpec(
                    code: spec.code,
                    title: "Business Network",
                    eyebrow: "Suppliers and credit buyers",
                    emptyTitle: "No network yet",
                    emptyBody: "Suppliers and credit buyers you record will show here.",
                    pulseSurface: false
                )
            case "customers":
                spec = OperatingSheetSpec(
                    code: spec.code,
                    title: "Clients & Services",
                    eyebrow: "Named clients and channels",
                    emptyTitle: "No network yet",
                    emptyBody: "Suppliers and credit buyers you record will show here.",
                    pulseSurface: false
                )
            case "M2":
                spec = OperatingSheetSpec(
                    code: spec.code,
                    title: "People",
                    eyebrow: "Staff on this moment",
                    emptyTitle: "No people yet",
                    emptyBody: "Invite the shop floor, billing and approvers.",
                    pulseSurface: false
                )
            case "L1":
                spec = OperatingSheetSpec(
                    code: spec.code,
                    title: vertical == .restaurantFood ? "Current Service" : "Current Business",
                    eyebrow: "As-of today · live position",
                    emptyTitle: "Nothing recorded yet",
                    emptyBody: vertical == .restaurantFood
                        ? "Today's kitchen tickets and posted bills will show here."
                        : "Today's posted sales and stock moves will show here.",
                    pulseSurface: false
                )
            case "L2":
                spec = OperatingSheetSpec(
                    code: spec.code,
                    title: vertical == .consumerService ? "Bookings & Services" : "Sales & Orders",
                    eyebrow: vertical == .consumerService
                        ? "Bookings · charges · holds · this period"
                        : "Tickets · credit · holds · this period",
                    emptyTitle: "Nothing recorded yet",
                    emptyBody: "Channel and ticket activity will show here.",
                    pulseSurface: false
                )
            case "L4":
                spec = OperatingSheetSpec(
                    code: spec.code,
                    title: vertical == .consumerService ? "Rooms & Staff" : "Operations / Stock",
                    eyebrow: vertical == .consumerService
                        ? "Rooms, roster, receipts and alerts"
                        : "Receipts, counts, adjustments and alerts",
                    emptyTitle: "No activity yet",
                    emptyBody: "Stock moves, issues, and updates will show here.",
                    pulseSurface: false
                )
            case "ME3":
                spec = OperatingSheetSpec(
                    code: spec.code,
                    title: "People & Activity",
                    eyebrow: "Who recorded what across the period",
                    emptyTitle: "No people memory",
                    emptyBody: "People and activity history will show here.",
                    pulseSurface: false
                )
            case "ME4":
                spec = OperatingSheetSpec(
                    code: spec.code,
                    title: "Patterns & Decisions",
                    eyebrow: "What repeats and what is waiting on approval",
                    emptyTitle: "No pattern yet",
                    emptyBody: "Repeated issues and pending approvals will show here.",
                    pulseSurface: false
                )
            default:
                break
            }
            return spec
        }
        guard vertical.isManufacturing else { return spec }
        if vertical == .madeToOrder {
            switch code {
            case "orders":
                spec = OperatingSheetSpec(
                    code: spec.code,
                    title: "Current Jobs",
                    eyebrow: spec.eyebrow,
                    emptyTitle: "No jobs yet",
                    emptyBody: "Confirmed jobs and their operations will show here.",
                    pulseSurface: spec.pulseSurface
                )
            case "production":
                spec = OperatingSheetSpec(
                    code: spec.code,
                    title: "Job Output",
                    eyebrow: "Recorded vs accepted against open jobs",
                    emptyTitle: "No job output yet",
                    emptyBody: "Accepted production against confirmed jobs appears here.",
                    pulseSurface: spec.pulseSurface
                )
            case "stock":
                spec = OperatingSheetSpec(
                    code: spec.code,
                    title: "Job Operations",
                    eyebrow: "WIP against open jobs",
                    emptyTitle: "No job stock yet",
                    emptyBody: "Materials issued to jobs and finished goods appear here.",
                    pulseSurface: spec.pulseSurface
                )
            case "M4":
                spec = OperatingSheetSpec(
                    code: spec.code,
                    title: "Open Jobs",
                    eyebrow: spec.eyebrow,
                    emptyTitle: spec.emptyTitle,
                    emptyBody: spec.emptyBody,
                    pulseSurface: spec.pulseSurface
                )
            case "L1":
                spec = OperatingSheetSpec(
                    code: spec.code,
                    title: "Current Jobs Today",
                    eyebrow: "As-of today · live job position",
                    emptyTitle: spec.emptyTitle,
                    emptyBody: spec.emptyBody,
                    pulseSurface: spec.pulseSurface
                )
            case "L2":
                spec = OperatingSheetSpec(
                    code: spec.code,
                    title: "Jobs & Customers",
                    eyebrow: "Attributed work by customer and channel",
                    emptyTitle: spec.emptyTitle,
                    emptyBody: spec.emptyBody,
                    pulseSurface: spec.pulseSurface
                )
            case "L4":
                spec = OperatingSheetSpec(
                    code: spec.code,
                    title: "Production Operations",
                    eyebrow: "Job execution · issues · stock signals",
                    emptyTitle: spec.emptyTitle,
                    emptyBody: spec.emptyBody,
                    pulseSurface: spec.pulseSurface
                )
            case "fulfilment":
                spec = OperatingSheetSpec(
                    code: spec.code,
                    title: "Job Fulfilment Memory",
                    eyebrow: "Job → produce → invoice → dispatch",
                    emptyTitle: spec.emptyTitle,
                    emptyBody: spec.emptyBody,
                    pulseSurface: spec.pulseSurface
                )
            default:
                break
            }
        } else if vertical == .standardProduction {
            switch code {
            case "orders":
                spec = OperatingSheetSpec(
                    code: spec.code,
                    title: "Work Orders & Commitments",
                    eyebrow: "Commitments are not revenue until billed",
                    emptyTitle: "No work orders yet",
                    emptyBody: "Work orders and their dispatches will show here.",
                    pulseSurface: spec.pulseSurface
                )
            case "production":
                spec = OperatingSheetSpec(
                    code: spec.code,
                    title: "Production",
                    eyebrow: "Recorded vs accepted plant output",
                    emptyTitle: "No production yet",
                    emptyBody: "Production batches and accepted output appear here.",
                    pulseSurface: spec.pulseSurface
                )
            case "L1":
                spec = OperatingSheetSpec(
                    code: spec.code,
                    title: "Current Production / Business",
                    eyebrow: "As-of today · live plant position",
                    emptyTitle: spec.emptyTitle,
                    emptyBody: spec.emptyBody,
                    pulseSurface: spec.pulseSurface
                )
            case "L2":
                spec = OperatingSheetSpec(
                    code: spec.code,
                    title: "Orders / Buyers",
                    eyebrow: "Attributed plant sales by buyer and channel",
                    emptyTitle: spec.emptyTitle,
                    emptyBody: spec.emptyBody,
                    pulseSurface: spec.pulseSurface
                )
            case "L4":
                spec = OperatingSheetSpec(
                    code: spec.code,
                    title: "Production Operations",
                    eyebrow: "Plant execution · issues · stock signals",
                    emptyTitle: spec.emptyTitle,
                    emptyBody: spec.emptyBody,
                    pulseSurface: spec.pulseSurface
                )
            default:
                break
            }
        }
        return spec
    }

    /// Board section code → the surface it opens. Pulse surfaces are opened by their own buttons.
    static func code(forSection sectionCode: String) -> String? {
        guard all.contains(where: { $0.code == sectionCode }) else { return nil }
        return sectionCode
    }
}

// MARK: - Capture layout

/// The twelve QA capture frames — `TRD-QA-01..12` / `DST-QA-01..12`.
enum OperatingCaptureLayout: String, CaseIterable {
    case sale = "SALE"
    case purchase = "PURCHASE"
    case collection = "COLLECTION"
    case payment = "PAYMENT"
    case spend = "SPEND"
    case stock = "STOCK"
    case saleReturn = "RETURN"
    case budget = "BUDGET"
    case update = "UPDATE"
    case approval = "APPROVAL"
    case order = "ORDER"
    case shipment = "SHIPMENT"

    var qaCode: String {
        let index = (Self.allCases.firstIndex(of: self) ?? 0) + 1
        return String(format: "QA-%02d", index)
    }

    var label: String {
        switch self {
        case .sale: return "Sale"
        case .purchase: return "Purchase"
        case .collection: return "Collection"
        case .payment: return "Payment"
        case .spend: return "Expense"
        case .stock: return "Stock"
        case .saleReturn: return "Return"
        case .budget: return "Budget"
        case .update: return "Update / Issue"
        case .approval: return "Approval / Reimbursement"
        case .order: return "Order"
        case .shipment: return "Delivery"
        }
    }

    static func forMode(_ mode: String) -> OperatingCaptureLayout? {
        switch mode.trimmingCharacters(in: .whitespacesAndNewlines).uppercased() {
        case "SALE", "SERVICE": return .sale
        case "PURCHASE", "VENDOR": return .purchase
        case "COLLECTION": return .collection
        case "PAYMENT", "REIMBURSE": return .payment
        case "SPEND": return .spend
        case "STOCK", "TRANSFER", "HOLD": return .stock
        case "RETURN": return .saleReturn
        case "BUDGET", "TARGET": return .budget
        case "UPDATE", "ISSUE": return .update
        case "APPROVAL": return .approval
        case "ORDER", "CUSTOMER_ORDER", "PRODUCTION", "APPOINTMENT", "BOOKING", "STATUS",
             "CONFIRM", "QUOTE_REVISE", "OPEN_JOB", "JOB_STATUS": return .order
        case "REWORK": return .stock
        case "SHIPMENT", "DELIVERY": return .shipment
        default: return nil
        }
    }

    /// The three modes that have their own frame on top of the twelve.
    static let holdMode = "HOLD"
    static let transferMode = "TRANSFER"
    static let reprintMode = "REPRINT"

    static let holdChoices = ["No hold", "Hold order", "Hold dispatch"]
    static let reprintKinds = ["INVOICE", "CHALLAN"]
}

// MARK: - Masters

/// The six masters tabs (`2566:31888`-`2566:32380`, `2558:29420`-`2558:29912`).
enum OperatingMasterTab: String, CaseIterable, Identifiable {
    case principals
    case catalogue
    case locations
    case priceLists
    case invite

    var id: String { rawValue }

    var title: String {
        switch self {
        case .principals: return "Principals & dealers"
        case .catalogue: return "Catalogue & SKU"
        case .locations: return "Warehouses & routes"
        case .priceLists: return "Price lists"
        case .invite: return "Invite & QR"
        }
    }

    var singular: String {
        switch self {
        case .principals: return "principal or dealer"
        case .catalogue: return "SKU"
        case .locations: return "warehouse or route"
        case .priceLists: return "price list"
        case .invite: return "invite"
        }
    }

    var note: String {
        switch self {
        case .principals:
            return "Identity is stable here: a dealer row is referenced by id, never by its typed name."
        case .catalogue:
            return "A SKU carries unit precision and standing prices. A posted bill never rewrites it."
        case .locations:
            return "A route belongs to a warehouse. Moving stock between them is a transfer, not a sale."
        case .priceLists:
            return "A price list is effective-dated. Two lists may overlap in name but never in a day."
        case .invite:
            return "Invite grants a role and its access. Joining ≠ approval ≠ payment."
        }
    }

    var kinds: Set<String> {
        switch self {
        case .principals: return ["PRINCIPAL", "DEALER", "SUPPLIER", "BUYER", "CUSTOMER", "CHANNEL"]
        case .catalogue: return ["SKU", "PRODUCT", "BATCH"]
        case .locations: return ["WAREHOUSE", "LOCATION", "ROUTE"]
        case .priceLists, .invite: return []
        }
    }

    var defaultKind: String {
        switch self {
        case .principals: return "DEALER"
        case .catalogue: return "SKU"
        case .locations: return "WAREHOUSE"
        case .priceLists: return "PRICE_LIST"
        case .invite: return "CHANNEL"
        }
    }
}

// MARK: - Drafts (pure mapping)

/// What the master form collects, independent of SwiftUI, so the wire body can be tested.
struct OperatingMasterDraft {
    var kind: String
    var name: String = ""
    var code: String = ""
    var unit: String = ""
    var sellPrice: String = ""
    var purchasePrice: String = ""
    var mrp: String = ""
    var hsn: String = ""
    var gstRate: String = ""
    var gstin: String = ""
    var billingState: String = ""
    var parentMasterId: String = ""
    var momentId: String? = nil

    /// Blank means "not supplied", never an empty value on the wire.
    private func nz(_ raw: String) -> String? {
        let trimmed = raw.trimmingCharacters(in: .whitespacesAndNewlines)
        return trimmed.isEmpty ? nil : trimmed
    }

    /// GST applicability is derived: a rate is the only thing that makes a master taxable.
    var gstApplicable: String? { nz(gstRate) == nil ? nil : "YES" }

    var canSave: Bool { !name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty }

    func toCreateBody() -> OperatingMasterCreateBody {
        OperatingMasterCreateBody(
            kind: kind,
            name: name.trimmingCharacters(in: .whitespacesAndNewlines),
            code: nz(code),
            momentId: momentId,
            unit: nz(unit),
            sellPrice: nz(sellPrice),
            mrp: nz(mrp),
            purchasePrice: nz(purchasePrice),
            gstApplicable: gstApplicable,
            gstRate: nz(gstRate),
            hsn: nz(hsn),
            gstin: nz(gstin),
            billingState: nz(billingState),
            parentMasterId: nz(parentMasterId)
        )
    }

    func toUpdateBody(expectedVersion: Int) -> OperatingMasterUpdateBody {
        OperatingMasterUpdateBody(
            name: name.trimmingCharacters(in: .whitespacesAndNewlines),
            code: nz(code),
            unit: nz(unit),
            sellPrice: nz(sellPrice),
            mrp: nz(mrp),
            purchasePrice: nz(purchasePrice),
            gstApplicable: gstApplicable,
            gstRate: nz(gstRate),
            hsn: nz(hsn),
            gstin: nz(gstin),
            billingState: nz(billingState),
            parentMasterId: nz(parentMasterId),
            expectedVersion: expectedVersion
        )
    }
}

// MARK: - Contract normalisation

/// The capture layouts speak in operator words (`In`, `Out`, `Owned`) while the contract
/// accepts a fixed vocabulary (`ADD`, `REDUCE`, `OWNED`). The mapping lives here, in pure code,
/// so both a test and the sheet can use the same one — a wrong literal is a rejected write.
enum OperatingContractVocabulary {
    /// `SET` replaces the count, `ADD`/`REDUCE` move it, `TRANSFER` moves it between places.
    /// Retail Figma chips use Set current / Add / Reduce; legacy IN / OUT / COUNT still map.
    static func stockAction(_ raw: String) -> String {
        let key = raw.trimmingCharacters(in: .whitespaces).uppercased()
        switch key {
        case "IN", "ADD", "RECEIVED": return "ADD"
        case "OUT", "REDUCE", "ISSUED": return "REDUCE"
        case "SET", "COUNT", "SET CURRENT", "SET CURRENT STOCK": return "SET"
        case "TRANSFER", "MOVE": return "TRANSFER"
        default:
            if key.hasPrefix("SET") { return "SET" }
            if key.hasPrefix("ADD") { return "ADD" }
            if key.hasPrefix("REDUCE") { return "REDUCE" }
            if key.hasPrefix("TRANSFER") { return "TRANSFER" }
            return "ADD"
        }
    }

    /// The server accepts these reasons only; anything else is folded into `correction`.
    static func stockReason(_ raw: String) -> String? {
        let trimmed = raw.trimmingCharacters(in: .whitespaces)
        guard !trimmed.isEmpty else { return nil }
        switch trimmed.lowercased() {
        case "opening", "opening count": return "opening"
        case "stocktake", "count", "cycle count", "stock count": return "stocktake"
        case "damage", "damaged": return "damage"
        case "waste": return "waste"
        case "return", "returned": return "return"
        case "correction", "replenishment", "replenish": return "correction"
        case "transfer", "move": return "TRANSFER"
        default: return "correction"
        }
    }

    /// Consignment stock is never counted as owned on-hand, so the two words must not drift.
    static func ownership(_ raw: String) -> String {
        raw.trimmingCharacters(in: .whitespaces).uppercased().hasPrefix("CONSIGN") ? "CONSIGNMENT" : "OWNED"
    }

    /// `No hold` is a release, never an absence of an answer.
    static func holdStatus(_ raw: String) -> String {
        raw.trimmingCharacters(in: .whitespaces).lowercased() == "no hold" ? "RELEASED" : "HELD"
    }
}

// MARK: - Number formatting

/// Money is formatted per locale; a missing figure is an em dash, never a zero.
enum OperatingMoneyFormat {
    static func format(_ currency: String?, _ raw: String?, visible: Bool = true) -> String {
        guard visible, let raw, !raw.isEmpty else { return "—" }
        guard let value = Decimal(string: raw) else { return raw }
        let code = currency?.uppercased() ?? "INR"
        let formatter = NumberFormatter()
        formatter.numberStyle = .decimal
        formatter.minimumFractionDigits = 0
        formatter.maximumFractionDigits = 2
        formatter.locale = Locale(identifier: code == "INR" ? "en_IN" : "en_US")
        let text = formatter.string(from: value as NSDecimalNumber) ?? raw
        return code == "INR" ? "₹\(text)" : "\(text) \(code)"
    }

    /// The tile value plus its unit, except when the value already reads as a sentence
    /// (`Not recorded`), which must stand alone.
    static func tileValue(_ value: String, unit: String?) -> String {
        guard let unit, !unit.isEmpty, value != "Not recorded" else { return value }
        return "\(value) \(unit)"
    }
}

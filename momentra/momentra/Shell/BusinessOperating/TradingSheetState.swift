import Foundation

/// One of the eighteen TRADING_WHOLESALE `View all ›` surfaces (Figma TRD-SHEET).
struct OperatingSheetSpec: Equatable {
    let code: String
    let title: String
    let eyebrow: String
    let emptyTitle: String
    let emptyBody: String
    var pulseSurface: Bool = false

    var statusFilters: [String] { OperatingSheetChrome.statusFilters(code) }
    var footerCta: String { OperatingSheetChrome.footerCta(code) }
    var footerAction: String { OperatingSheetChrome.footerAction(code) }
    var periodFilters: [String] { OperatingSheetChrome.periodFilters }
}

enum TradingSheetState: Equatable, Identifiable {
    case targetBudget
    case finance
    case paymentReadiness
    case needsAttention
    case activity
    case gst
    case ordersCommitments
    case stockProducts
    case peopleNetwork
    case openCommitments
    case lifeCurrentBusiness
    case lifeDealersChannels
    case lifeMoneyMoving
    case lifePeopleActivity
    case monthlyMemory
    case moneyMemory
    case fulfilmentMemory
    case peoplePatterns

    var id: String { spec.code }

    var spec: OperatingSheetSpec {
        switch self {
        case .targetBudget:
            return OperatingSheetSpec(
                code: "TARGET_BUDGET",
                title: "Target & Budget",
                eyebrow: "What this period is aiming at",
                emptyTitle: "No target set",
                emptyBody: "Set a sales target and a period budget to measure the trade against them.",
                pulseSurface: true
            )
        case .finance:
            return OperatingSheetSpec(
                code: "FINANCE",
                title: "Finance",
                eyebrow: "Recorded sales, money in, money still out",
                emptyTitle: "No finance recorded",
                emptyBody: "Sales, collections, and payables appear once recorded.",
                pulseSurface: true
            )
        case .paymentReadiness:
            return OperatingSheetSpec(
                code: "PAYMENT_READINESS",
                title: "Payment Readiness",
                eyebrow: "What can be collected and what must be paid",
                emptyTitle: "Nothing awaiting settlement",
                emptyBody: "Open receivables and payables appear here as they are recorded.",
                pulseSurface: true
            )
        case .needsAttention:
            return OperatingSheetSpec(
                code: "ATTENTION",
                title: "Needs Attention",
                eyebrow: "Overdue, blocked, and waiting on a decision",
                emptyTitle: "Nothing is waiting",
                emptyBody: "Approvals, overdue items, and blockers will show here.",
                pulseSurface: true
            )
        case .activity:
            return OperatingSheetSpec(
                code: "ACTIVITY",
                title: "Activity",
                eyebrow: "Everything recorded in this moment",
                emptyTitle: "Nothing recorded yet",
                emptyBody: "Sales, purchases, and money collection will show here.",
                pulseSurface: true
            )
        case .gst:
            return OperatingSheetSpec(
                code: "GST",
                title: "GST Position",
                eyebrow: "Indicative tax on recorded records",
                emptyTitle: "No GST recorded",
                emptyBody: "GST appears on records once tracking is on and a rate is supplied.",
                pulseSurface: true
            )
        case .ordersCommitments:
            return OperatingSheetSpec(
                code: "orders",
                title: "Orders & Commitments",
                eyebrow: "Commitments are not revenue until billed",
                emptyTitle: "No orders yet",
                emptyBody: "Buyer orders and their dispatches will show here."
            )
        case .stockProducts:
            return OperatingSheetSpec(
                code: "stock",
                title: "Stock & Products",
                eyebrow: "Owned on-hand only — consignment stays out",
                emptyTitle: "No stock recorded",
                emptyBody: "Open stock, purchases, and dispatches build the on-hand position."
            )
        case .peopleNetwork:
            return OperatingSheetSpec(
                code: "network",
                title: "People & Network",
                eyebrow: "Who the trade runs through",
                emptyTitle: "No people yet",
                emptyBody: "Named buyers, suppliers, and channels appear as you record them."
            )
        case .openCommitments:
            return OperatingSheetSpec(
                code: "M4",
                title: "Open Commitments",
                eyebrow: "What is still owed in either direction",
                emptyTitle: "No open commitments",
                emptyBody: "Open orders, unsettled money, and dispatches in transit appear here."
            )
        case .lifeCurrentBusiness:
            return OperatingSheetSpec(
                code: "L1",
                title: "Current Business",
                eyebrow: "The trade today",
                emptyTitle: "Nothing recorded yet",
                emptyBody: "Board updates and the live position will show here."
            )
        case .lifeDealersChannels:
            return OperatingSheetSpec(
                code: "L2",
                title: "Buyers & Channels",
                eyebrow: "Attributed sales by channel",
                emptyTitle: "No channels recorded",
                emptyBody: "Channel-attributed sales appear once a channel is chosen."
            )
        case .lifeMoneyMoving:
            return OperatingSheetSpec(
                code: "L3",
                title: "Money Moving",
                eyebrow: "Collected, paid, and the pay lag between",
                emptyTitle: "No settlement yet",
                emptyBody: "Collections and payments will show here."
            )
        case .lifePeopleActivity:
            return OperatingSheetSpec(
                code: "L4",
                title: "People & Activity",
                eyebrow: "Who recorded what",
                emptyTitle: "No activity yet",
                emptyBody: "Stock moves, issues, and updates will show here."
            )
        case .monthlyMemory:
            return OperatingSheetSpec(
                code: "ME1",
                title: "Monthly Memory",
                eyebrow: "Each month this trade has recorded",
                emptyTitle: "No month recorded",
                emptyBody: "Months appear once records exist in them."
            )
        case .moneyMemory:
            return OperatingSheetSpec(
                code: "ME2",
                title: "Money Memory",
                eyebrow: "Collected, paid, and returned",
                emptyTitle: "No money memory",
                emptyBody: "Settlements and returns will show here."
            )
        case .fulfilmentMemory:
            return OperatingSheetSpec(
                code: "fulfilment",
                title: "Fulfilment Memory",
                eyebrow: "Delivered, received, and how late",
                emptyTitle: "No fulfilment recorded",
                emptyBody: "Dispatch and receipt status build this picture."
            )
        case .peoplePatterns:
            return OperatingSheetSpec(
                code: "ME4",
                title: "Patterns & Decisions",
                eyebrow: "What repeats and what is waiting on approval",
                emptyTitle: "No pattern yet",
                emptyBody: "Repeated issues and pending approvals will show here."
            )
        }
    }

    static let allCases: [TradingSheetState] = [
        .targetBudget, .finance, .paymentReadiness, .needsAttention, .activity, .gst,
        .ordersCommitments, .stockProducts, .peopleNetwork, .openCommitments,
        .lifeCurrentBusiness, .lifeDealersChannels, .lifeMoneyMoving, .lifePeopleActivity,
        .monthlyMemory, .moneyMemory, .fulfilmentMemory, .peoplePatterns,
    ]
}

enum OperatingSheetChrome {
    static let periodFilters = ["This month", "Last month", "Custom range ›"]

    static func statusFilters(_ code: String) -> [String] {
        switch code {
        case "FINANCE", "PAYMENT_READINESS", "ATTENTION", "orders", "M4", "L3":
            return ["Open", "Overdue", "Settled"]
        case "ME1", "ME2", "fulfilment", "ME4":
            return ["All", "Open", "Settled"]
        default:
            return ["All", "Open", "Done"]
        }
    }

    static func footerCta(_ code: String) -> String {
        switch code {
        case "PAYMENT_READINESS", "L3": return "Record payment"
        case "ATTENTION": return "Resolve all"
        case "orders", "M4": return "Resolve commitments"
        case "ME1", "ME2", "fulfilment", "ME4": return "Open Memory"
        default: return "Open Quick Add"
        }
    }

    static func footerAction(_ code: String) -> String {
        switch footerCta(code) {
        case "Record payment": return "PAYMENT"
        case "Resolve all", "Resolve commitments": return "ATTENTION"
        case "Open Memory": return "MEMORY"
        default: return "QUICK_ADD"
        }
    }

    static func matchesStatus(_ filter: String, badge: String?) -> Bool {
        if filter == "All" { return true }
        let b = (badge ?? "").lowercased()
        switch filter {
        case "Open":
            return b.contains("open") || b.contains("alert") || b.contains("due") || b.isEmpty
        case "Overdue":
            return b.contains("overdue") || b.contains("critical")
        case "Settled", "Done":
            return b.contains("settled") || b.contains("done") || b.contains("paid") || b.contains("closed")
        default:
            return true
        }
    }
}

func tradingSheetForCode(_ code: String) -> TradingSheetState? {
    switch code {
    case "orders": return .ordersCommitments
    case "stock": return .stockProducts
    case "network": return .peopleNetwork
    case "M4": return .openCommitments
    case "L1": return .lifeCurrentBusiness
    case "L2": return .lifeDealersChannels
    case "L3": return .lifeMoneyMoving
    case "L4": return .lifePeopleActivity
    case "ME1": return .monthlyMemory
    case "ME2": return .moneyMemory
    case "fulfilment": return .fulfilmentMemory
    case "ME4": return .peoplePatterns
    default: return nil
    }
}

func tradingSheetForPulseOrBoard(_ code: String) -> TradingSheetState? {
    switch code.uppercased() {
    case "ATTENTION": return .needsAttention
    case "TARGET_BUDGET": return .targetBudget
    case "PAYMENT_READINESS": return .paymentReadiness
    case "FINANCE": return .finance
    case "ACTIVITY": return .activity
    case "GST": return .gst
    default: return tradingSheetForCode(code)
    }
}

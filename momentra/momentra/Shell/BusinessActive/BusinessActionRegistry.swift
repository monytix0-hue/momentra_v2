import Foundation
import SwiftUI

/// V019 Business Quick Add capability codes (S4 + Ops). Fail-closed like Personal.
enum BusinessActionCode: String, CaseIterable, Equatable {
    case expenseCreate = "EXPENSE_CREATE"
    case revenueRecord = "REVENUE_RECORD"
    case invoiceCreate = "INVOICE_CREATE"
    case memberManage = "MEMBER_MANAGE"
    case vendorManage = "VENDOR_MANAGE"
    case issueCreate = "ISSUE_CREATE"
    case slaManage = "SLA_MANAGE"
}

enum BusinessActionDestination: Equatable {
    case spend
    case vendor
    case issue
    case sla
    case revenue
    case invoice
    case members
}

struct BusinessActionTile: Identifiable {
    var id: String { "\(code.rawValue)-\(label)" }
    let code: BusinessActionCode
    let label: String
    let icon: String
    let colors: [Color]
    let destination: BusinessActionDestination
    let enabledWhenMomentActive: Bool
}

enum BusinessActionRegistry {
    static func destination(for capabilityCode: String) -> BusinessActionDestination? {
        switch capabilityCode.uppercased() {
        case BusinessActionCode.expenseCreate.rawValue: return .spend
        case BusinessActionCode.vendorManage.rawValue: return .vendor
        case BusinessActionCode.issueCreate.rawValue: return .issue
        case BusinessActionCode.slaManage.rawValue: return .sla
        case BusinessActionCode.revenueRecord.rawValue: return .revenue
        case BusinessActionCode.invoiceCreate.rawValue: return .invoice
        case BusinessActionCode.memberManage.rawValue: return .members
        default: return nil
        }
    }

    static func defaultCodes() -> [BusinessActionCode] {
        [.expenseCreate, .revenueRecord, .invoiceCreate, .memberManage, .vendorManage, .issueCreate, .slaManage]
    }

    /// Empty / nil capabilities fail closed (mirror Personal).
    static func isDestinationEnabled(_ capabilities: [String]?, destination target: BusinessActionDestination) -> Bool {
        guard let capabilities, !capabilities.isEmpty else {
            return false
        }
        return capabilities.contains { destination(for: $0) == target }
    }

    static func destination(for kind: BusinessQuickAddKind) -> BusinessActionDestination? {
        switch kind {
        case .expense, .spendEntry, .requestApproval, .budgetReview: return .spend
        case .updateVendor: return .vendor
        case .reportIssue, .logImprovement: return .issue
        case .slaCheck: return .sla
        case .revenue: return .revenue
        case .invoice: return .invoice
        default: return nil
        }
    }

    /// Unmapped kinds (update/memory) stay available when moment is active.
    static func isKindEnabled(_ kind: BusinessQuickAddKind, capabilities: [String]?) -> Bool {
        guard let dest = destination(for: kind) else { return true }
        return isDestinationEnabled(capabilities, destination: dest)
    }

    /// Builds hub tiles.
    /// - `nil` capabilityCodes → default V019 codes (catalog / tests)
    /// - empty array → fail closed (no tiles)
    /// - non-empty → destination-level enablement
    static func tiles(
        hasActiveMoment: Bool,
        capabilityCodes: [String]? = nil
    ) -> [BusinessActionTile] {
        let effectiveCaps = capabilityCodes ?? defaultCodes().map(\.rawValue)
        return catalogTiles().compactMap { tile in
            if !isDestinationEnabled(effectiveCaps, destination: tile.destination) {
                return nil
            }
            return BusinessActionTile(
                code: tile.code,
                label: tile.label,
                icon: tile.icon,
                colors: tile.colors,
                destination: tile.destination,
                enabledWhenMomentActive: tile.enabledWhenMomentActive && hasActiveMoment
            )
        }
    }

    private static func catalogTiles() -> [BusinessActionTile] {
        let accent = Color(red: 0.506, green: 0.549, blue: 0.973)
        let soft = Color(red: 0.647, green: 0.706, blue: 0.988)
        return [
            BusinessActionTile(
                code: .expenseCreate,
                label: "Expense",
                icon: "creditcard",
                colors: [accent, soft],
                destination: .spend,
                enabledWhenMomentActive: true
            ),
            BusinessActionTile(
                code: .revenueRecord,
                label: "Revenue",
                icon: "chart.line.uptrend.xyaxis",
                colors: [Color(hex: "#14B8A6"), Color(hex: "#2DD4BF")],
                destination: .revenue,
                enabledWhenMomentActive: true
            ),
            BusinessActionTile(
                code: .invoiceCreate,
                label: "Invoice",
                icon: "doc.text",
                colors: [Color(hex: "#3B82F6"), Color(hex: "#60A5FA")],
                destination: .invoice,
                enabledWhenMomentActive: true
            ),
            BusinessActionTile(
                code: .memberManage,
                label: "People",
                icon: "person.2",
                colors: [Color(hex: "#6366F1"), Color(hex: "#818CF8")],
                destination: .members,
                enabledWhenMomentActive: true
            ),
        ]
    }
}

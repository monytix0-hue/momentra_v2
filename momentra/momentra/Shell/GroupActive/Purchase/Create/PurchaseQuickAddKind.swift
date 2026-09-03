import SwiftUI

enum PurchaseQuickAddKind: String, Identifiable, CaseIterable {
    case contributor
    case contribution
    case expense
    case budget
    case vendor
    case poll
    case update
    case memory
    case purchaseItem
    case delivery
    case ownership

    var id: String { rawValue }

    var isLive: Bool {
        switch self {
        case .contribution, .expense, .budget, .vendor, .poll, .update, .memory, .purchaseItem, .contributor, .delivery, .ownership:
            return true
        }
    }

    var label: String {
        switch self {
        case .contributor: return "Contributor"
        case .contribution: return "Contribution"
        case .expense: return "Expense"
        case .budget: return "Budget"
        case .vendor: return "Vendor"
        case .poll: return "Poll"
        case .update: return "Update"
        case .memory: return "Memory"
        case .purchaseItem: return "Purchase Item"
        case .delivery: return "Delivery"
        case .ownership: return "Ownership"
        }
    }

    var emoji: String {
        switch self {
        case .contributor: return "👤"
        case .contribution: return "🎁"
        case .expense: return "💳"
        case .budget: return "💰"
        case .vendor: return "🏪"
        case .poll: return "📊"
        case .update: return "📢"
        case .memory: return "📷"
        case .purchaseItem: return "🛍️"
        case .delivery: return "📦"
        case .ownership: return "🔑"
        }
    }

    func gradient(theme: PurchaseActiveTheme) -> [Color] {
        switch self {
        case .contributor: return [theme.accentLight, theme.accent]
        case .contribution: return [Color(hex: "#2DD4BF"), Color(hex: "#0F766E")]
        case .expense: return [Color(hex: "#34D399"), Color(hex: "#059669")]
        case .budget: return [theme.accentLight, theme.accentSolid]
        case .vendor: return [Color(hex: "#93C5FD"), Color(hex: "#3B82F6")]
        case .poll: return [Color(hex: "#A854F7"), Color(hex: "#7D3BED")]
        case .update: return [Color(hex: "#818CF8"), Color(hex: "#4F46E5")]
        case .memory: return [Color(hex: "#F59E0B"), Color(hex: "#D97706")]
        case .purchaseItem: return [theme.accentLight, theme.accent]
        case .delivery: return [Color(hex: "#FB923C"), Color(hex: "#EA580C")]
        case .ownership: return [Color(hex: "#A78BFA"), Color(hex: "#7C3AED")]
        }
    }

    static func hubTiles(theme: PurchaseActiveTheme) -> [PurchaseQuickAddKind] {
        var tiles: [PurchaseQuickAddKind] = []
        if theme.includesContributor {
            tiles.append(.contributor)
        }
        tiles.append(contentsOf: [.contribution, .purchaseItem, .expense, .poll])
        if theme.includesBudget {
            tiles.append(.budget)
        }
        tiles.append(.update)
        if theme.includesDelivery {
            tiles.append(.delivery)
        }
        tiles.append(.memory)
        if theme.includesVendor {
            tiles.append(.vendor)
        }
        if theme.includesOwnership {
            tiles.append(.ownership)
        }
        return tiles
    }
}

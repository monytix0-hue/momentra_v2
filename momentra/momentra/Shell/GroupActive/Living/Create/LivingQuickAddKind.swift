import SwiftUI

enum LivingQuickAddKind: String, Identifiable, CaseIterable {
    case resident
    case expense
    case contribution
    case task
    case rule
    case asset
    case maintenance
    case update
    case poll
    case memory

    var id: String { rawValue }

    var isLive: Bool {
        switch self {
        case .resident, .expense, .contribution, .task, .asset, .maintenance, .update, .poll, .memory:
            return true
        case .rule:
            return false
        }
    }

    var label: String {
        switch self {
        case .resident: return "Invite"
        case .expense: return "Expense"
        case .contribution: return "Contribution"
        case .task: return "Task"
        case .rule: return "Rule"
        case .asset: return "Asset"
        case .maintenance: return "Maintenance"
        case .update: return "Update"
        case .poll: return "Poll"
        case .memory: return "Memory"
        }
    }

    var emoji: String {
        switch self {
        case .resident: return "👤"
        case .expense: return "💳"
        case .contribution: return "💰"
        case .task: return "✅"
        case .rule: return "📋"
        case .asset: return "📦"
        case .maintenance: return "🔧"
        case .update: return "📢"
        case .poll: return "📊"
        case .memory: return "📷"
        }
    }

    func gradient(theme: LivingActiveTheme) -> [Color] {
        switch self {
        case .resident: return [theme.accentLight, theme.accent]
        case .expense: return [Color(hex: "#34D399"), Color(hex: "#059669")]
        case .contribution: return [Color(hex: "#2DD4BF"), Color(hex: "#0F766E")]
        case .task: return [Color(hex: "#60A5FA"), Color(hex: "#2563EB")]
        case .rule: return [Color(hex: "#FBBF24"), Color(hex: "#D97706")]
        case .asset: return [Color(hex: "#A78BFA"), Color(hex: "#7C3AED")]
        case .maintenance: return [Color(hex: "#FB923C"), Color(hex: "#EA580C")]
        case .update: return [Color(hex: "#818CF8"), Color(hex: "#4F46E5")]
        case .poll: return [Color(hex: "#A854F7"), Color(hex: "#7D3BED")]
        case .memory: return [Color(hex: "#F59E0B"), Color(hex: "#D97706")]
        }
    }

    static func hubTiles(theme: LivingActiveTheme) -> [LivingQuickAddKind] {
        // Flatmates / Co-living: Figma 629:8697 / 629:10541
        if theme.includesContribution {
            return [.resident, .expense, .contribution, .task, .rule, .asset, .maintenance, .update, .poll, .memory]
        }
        // Family / Custom: Figma 629:16126 / 629:15586 — Rule after Update
        return [.resident, .expense, .task, .asset, .maintenance, .update, .rule, .poll, .memory]
    }
}

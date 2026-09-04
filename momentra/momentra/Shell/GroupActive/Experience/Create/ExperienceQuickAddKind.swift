import SwiftUI

enum ExperienceQuickAddKind: String, Identifiable, CaseIterable {
    case participant
    case planning
    case expense
    case budget
    case contribution
    case settle
    case vendor
    case attendance
    case update
    case poll
    case memory
    case booking

    var id: String { rawValue }

    var isLive: Bool {
        switch self {
        case .expense, .budget, .contribution, .settle, .planning, .poll, .update, .memory,
             .vendor, .attendance, .participant, .booking:
            return true
        }
    }

    var label: String {
        switch self {
        case .participant: return "Invite"
        case .planning: return "Planning Item"
        case .expense: return "Expense"
        case .budget: return "Budget"
        case .contribution: return "Contribution"
        case .settle: return "Settle"
        case .vendor: return "Vendor"
        case .attendance: return "Attendance"
        case .update: return "Update"
        case .poll: return "Poll"
        case .memory: return "Memory"
        case .booking: return "Booking"
        }
    }

    var emoji: String {
        switch self {
        case .participant: return "👤"
        case .planning: return "📋"
        case .expense: return "💳"
        case .budget: return "💰"
        case .contribution: return "🎁"
        case .settle: return "⚖️"
        case .vendor: return "🏪"
        case .attendance: return "✅"
        case .update: return "📢"
        case .poll: return "📊"
        case .memory: return "📷"
        case .booking: return "🧳"
        }
    }

    func gradient(theme: ExperienceActiveTheme) -> [Color] {
        switch self {
        case .participant: return [theme.accentLight, theme.accent]
        case .planning: return [Color(hex: "#60A5FA"), Color(hex: "#2563EB")]
        case .expense: return [Color(hex: "#34D399"), Color(hex: "#059669")]
        case .budget: return [theme.accentLight, theme.accentSolid]
        case .contribution: return [Color(hex: "#2DD4BF"), Color(hex: "#0F766E")]
        case .settle: return [Color(hex: "#059669"), Color(hex: "#10B981")]
        case .vendor: return [Color(hex: "#93C5FD"), Color(hex: "#3B82F6")]
        case .attendance: return [theme.accent, Color(hex: "#1E3A8A")]
        case .update: return [Color(hex: "#818CF8"), Color(hex: "#4F46E5")]
        case .poll: return [Color(hex: "#A854F7"), Color(hex: "#7D3BED")]
        case .memory: return [Color(hex: "#F59E0B"), Color(hex: "#D97706")]
        case .booking: return [Color(hex: "#FB923C"), Color(hex: "#EA580C")]
        }
    }

    static func hubTiles(includesVendor: Bool) -> [ExperienceQuickAddKind] {
        var tiles: [ExperienceQuickAddKind] = [
            .participant, .planning, .expense, .budget,
        ]
        if includesVendor {
            tiles.append(.vendor)
        }
        tiles.append(contentsOf: [.attendance, .update, .poll, .memory])
        return tiles
    }

    var asWeddingKind: WeddingQuickAddKind? {
        switch self {
        case .participant: return .participant
        case .planning: return .planning
        case .expense: return .expense
        case .budget: return .budget
        case .contribution: return .contribution
        case .settle: return .settle
        case .vendor: return .vendor
        case .attendance: return .attendance
        case .update: return .update
        case .poll: return .poll
        case .memory: return .memory
        case .booking: return nil
        }
    }
}

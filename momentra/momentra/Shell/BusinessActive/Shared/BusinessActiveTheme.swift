import SwiftUI

/// B01–B03 Business active tokens — Team Ops / Runway / Operations.
struct BusinessActiveTheme {
    let bg: Color
    let accent: Color
    let accentSolid: Color
    let accentLight: Color
    let accentSoft: Color
    let text: Color
    let secondary: Color
    let muted: Color
    let card: Color
    let border: Color
    let typeLabel: String
    let pulseTitle: String
    let momentsTitle: String
    let lifeTitle: String
    let memoryTitle: String
    let hubSubtitle: String
    let hubHeroTitle: String
    let hubHeroDetail: String
    let hubHeroAssetName: String
    let filterChips: [String]
    let heroGradientColors: [Color]
    let quickChips: [(emoji: String, label: String, kind: BusinessQuickAddKind)]

    var heroGradient: LinearGradient {
        LinearGradient(colors: heroGradientColors, startPoint: .leading, endPoint: .trailing)
    }

    static let teamOperations = BusinessActiveTheme(
        bg: Color(hex: "#0C0F15"),
        accent: Color(hex: "#10B981"),
        accentSolid: Color(hex: "#059669"),
        accentLight: Color(hex: "#34D399"),
        accentSoft: Color(hex: "#10B981").opacity(0.2),
        text: Color(hex: "#E5E0EE"),
        secondary: Color(hex: "#94A3B8"),
        muted: Color(hex: "#64748B"),
        card: Color(hex: "#161B26"),
        border: Color(hex: "#1E293B"),
        typeLabel: "Team Operations",
        pulseTitle: "Team Pulse",
        momentsTitle: "Team Moments",
        lifeTitle: "Team Life",
        memoryTitle: "Team Memory",
        hubSubtitle: "Bring your team operations to life",
        hubHeroTitle: "Bring your team operations to life",
        hubHeroDetail: "Add people, plans, decisions, and updates",
        hubHeroAssetName: "TeamOpsHubHero",
        filterChips: ["Team Sync", "Sprint Review"],
        heroGradientColors: [Color(hex: "#6366F1"), Color(hex: "#A855F7")],
        quickChips: [
            ("📢", "Update", .teamUpdate),
            ("🚩", "Decision", .decision),
            ("🛡", "Blocker", .blocker),
            ("📷", "Memory", .memory),
        ]
    )

    static let businessRunway = BusinessActiveTheme(
        bg: Color(hex: "#0C0F15"),
        accent: Color(hex: "#F59E0B"),
        accentSolid: Color(hex: "#D97706"),
        accentLight: Color(hex: "#FBBF24"),
        accentSoft: Color(hex: "#F59E0B").opacity(0.2),
        text: Color(hex: "#E5E0EE"),
        secondary: Color(hex: "#94A3B8"),
        muted: Color(hex: "#64748B"),
        card: Color(hex: "#161B26"),
        border: Color(hex: "#1E293B"),
        typeLabel: "Business Runway",
        pulseTitle: "Runway Pulse",
        momentsTitle: "Runway Moments",
        lifeTitle: "Runway Life",
        memoryTitle: "Runway Memory",
        hubSubtitle: "Bring your finances to life",
        hubHeroTitle: "Bring your finances to life",
        hubHeroDetail: "Track revenue, expenses, taxes and forecasts",
        hubHeroAssetName: "RunwayHubHero",
        filterChips: ["Revenue", "Expenses"],
        heroGradientColors: [Color(hex: "#6366F1"), Color(hex: "#A855F7")],
        quickChips: [
            ("💰", "Revenue", .revenue),
            ("💳", "Expense", .expense),
            ("📄", "Invoice", .invoice),
            ("📷", "Memory", .memory),
        ]
    )

    static let businessOperations = BusinessActiveTheme(
        bg: Color(hex: "#0C0F15"),
        accent: Color(hex: "#818CF8"),
        accentSolid: Color(hex: "#6366F1"),
        accentLight: Color(hex: "#A5B4FC"),
        accentSoft: Color(hex: "#818CF8").opacity(0.2),
        text: Color(hex: "#E5E0EE"),
        secondary: Color(hex: "#94A3B8"),
        muted: Color(hex: "#64748B"),
        card: Color(hex: "#161B26"),
        border: Color(hex: "#1E293B"),
        typeLabel: "Business Operations",
        pulseTitle: "Ops Pulse",
        momentsTitle: "Ops Moments",
        lifeTitle: "Ops Life",
        memoryTitle: "Ops Memory",
        hubSubtitle: "Bring your operations to life",
        hubHeroTitle: "Bring your operations to life",
        hubHeroDetail: "Add expenses, vendors, approvals and updates",
        hubHeroAssetName: "OpsHubHero",
        filterChips: ["Budget Ops", "Vendor Mgmt"],
        heroGradientColors: [Color(hex: "#6366F1"), Color(hex: "#A855F7")],
        quickChips: [
            ("💳", "Spend", .spendEntry),
            ("✅", "Approval", .requestApproval),
            ("🔧", "Issue", .reportIssue),
            ("📷", "Memory", .memory),
        ]
    )

    static func forKind(_ kind: BusinessSetupKind) -> BusinessActiveTheme {
        switch kind {
        case .teamOperations: return .teamOperations
        case .businessRunway: return .businessRunway
        case .businessOperations: return .businessOperations
        }
    }

    static func forTypeCode(_ momentTypeCode: String?) -> BusinessActiveTheme {
        let code = (momentTypeCode ?? "").uppercased()
        if code.contains("RUNWAY") { return .businessRunway }
        if code.contains("OPERATIONS") && !code.contains("TEAM") { return .businessOperations }
        if code.contains("TEAM") { return .teamOperations }
        return .teamOperations
    }
}

enum BusinessQuickAddKind: String, Identifiable, CaseIterable {
    // Team Ops
    case teamUpdate
    case decision
    case blocker
    case meeting
    case recognition
    case approval
    case milestone
    case retrospective
    case riskFlag
    case activityLog
    case poll
    case memory
    // Runway
    case revenue
    case expense
    case taxEntry
    case investorUpdate
    case budgetAlert
    case forecastUpdate
    case invoice
    case generalUpdate
    // Ops
    case spendEntry
    case updateVendor
    case requestApproval
    case reportIssue
    case logImprovement
    case budgetReview
    case slaCheck

    var id: String { rawValue }

    var isLive: Bool {
        switch self {
        case .expense, .spendEntry, .revenue, .invoice, .poll, .memory, .teamUpdate, .generalUpdate,
             .decision, .blocker, .meeting, .recognition, .approval, .milestone, .retrospective,
             .riskFlag, .activityLog,
             .taxEntry, .investorUpdate, .budgetAlert, .forecastUpdate,
             .updateVendor, .requestApproval, .reportIssue, .logImprovement, .budgetReview, .slaCheck:
            return true
        default:
            return false
        }
    }

    var label: String {
        switch self {
        case .teamUpdate: return "Team Update"
        case .decision: return "Decision"
        case .blocker: return "Blocker"
        case .meeting: return "Meeting"
        case .recognition: return "Recognition"
        case .approval: return "Approval"
        case .milestone: return "Milestone"
        case .retrospective: return "Retrospective"
        case .riskFlag: return "Risk Flag"
        case .activityLog: return "Activity Log"
        case .poll: return "Poll"
        case .memory: return "Memory"
        case .revenue: return "Log Revenue"
        case .expense: return "Log Expense"
        case .taxEntry: return "Tax Entry"
        case .investorUpdate: return "Investor Update"
        case .budgetAlert: return "Budget Alert"
        case .forecastUpdate: return "Forecast Update"
        case .invoice: return "Invoice Track"
        case .generalUpdate: return "General Update"
        case .spendEntry: return "Log Spend Entry"
        case .updateVendor: return "Update Vendor"
        case .requestApproval: return "Request Approval"
        case .reportIssue: return "Report Issue"
        case .logImprovement: return "Log Improvement"
        case .budgetReview: return "Budget Review"
        case .slaCheck: return "SLA Check"
        }
    }

    var subtitle: String {
        switch self {
        case .teamUpdate: return "Share progress"
        case .decision: return "Log choices"
        case .blocker: return "Flag issues"
        case .meeting: return "Capture notes"
        case .recognition: return "Celebrate wins"
        case .approval: return "Route requests"
        case .milestone: return "Mark progress"
        case .retrospective: return "Review & learn"
        case .riskFlag: return "Raise concerns"
        case .activityLog: return "Track actions"
        case .poll: return "Gather input"
        case .memory: return "Save learnings"
        case .revenue: return "Track income"
        case .expense: return "Record spend"
        case .taxEntry: return "File taxes"
        case .investorUpdate: return "Share metrics"
        case .budgetAlert: return "Flag overrun"
        case .forecastUpdate: return "Update projections"
        case .invoice: return "Monitor payments"
        case .generalUpdate: return "Share updates"
        case .spendEntry: return "Record expenses"
        case .updateVendor: return "Update suppliers"
        case .requestApproval: return "Request sign-off"
        case .reportIssue: return "Flag a problem"
        case .logImprovement: return "Log optimization"
        case .budgetReview: return "Check budgets"
        case .slaCheck: return "Monitor SLAs"
        }
    }

    var emoji: String {
        switch self {
        case .teamUpdate, .generalUpdate: return "📢"
        case .decision: return "🚩"
        case .blocker, .riskFlag, .reportIssue: return "🛡"
        case .meeting, .budgetReview: return "📅"
        case .recognition: return "⭐"
        case .approval, .requestApproval: return "🏪"
        case .milestone: return "🏁"
        case .retrospective: return "⚡"
        case .activityLog: return "📈"
        case .poll: return "📊"
        case .memory: return "📦"
        case .revenue: return "💰"
        case .expense, .spendEntry: return "💳"
        case .taxEntry: return "🧾"
        case .investorUpdate: return "📣"
        case .budgetAlert: return "🚨"
        case .forecastUpdate: return "📉"
        case .invoice: return "📄"
        case .updateVendor: return "🏷"
        case .logImprovement: return "✨"
        case .slaCheck: return "⏱"
        }
    }

    var stripeColor: Color {
        switch self {
        case .teamUpdate, .generalUpdate, .approval, .activityLog, .spendEntry, .budgetReview:
            return Color(hex: "#818CF8")
        case .decision, .investorUpdate, .updateVendor, .memory:
            return Color(hex: "#A78BFA")
        case .blocker, .riskFlag, .budgetAlert, .reportIssue: return Color(hex: "#EF4444")
        case .meeting, .revenue, .expense, .forecastUpdate, .requestApproval, .retrospective:
            return Color(hex: "#F59E0B")
        case .recognition, .taxEntry, .logImprovement, .poll: return Color(hex: "#10B981")
        case .milestone, .invoice, .slaCheck: return Color(hex: "#14B8A6")
        }
    }

    /// Figma 649:26162 Team Ops Action Center tile icons (MCP exports).
    var teamOpsHubIconName: String? {
        switch self {
        case .teamUpdate: return "TeamOpsQaUpdate"
        case .decision: return "TeamOpsQaDecision"
        case .blocker: return "TeamOpsQaBlocker"
        case .meeting: return "TeamOpsQaMeeting"
        case .recognition: return "TeamOpsQaRecognition"
        case .approval: return "TeamOpsQaApproval"
        case .milestone: return "TeamOpsQaMilestone"
        case .retrospective: return "TeamOpsQaRetro"
        case .riskFlag: return "TeamOpsQaRisk"
        case .activityLog: return "TeamOpsQaActivity"
        case .poll: return "TeamOpsQaPoll"
        case .memory: return "TeamOpsQaMemory"
        default: return nil
        }
    }

    static func hubTiles(theme: BusinessActiveTheme) -> [BusinessQuickAddKind] {
        switch theme.typeLabel {
        case "Business Runway":
            return [.revenue, .expense, .taxEntry, .investorUpdate, .budgetAlert, .forecastUpdate, .invoice, .generalUpdate, .memory]
        case "Business Operations":
            return [.spendEntry, .updateVendor, .requestApproval, .reportIssue, .logImprovement, .budgetReview, .slaCheck, .generalUpdate, .memory]
        default:
            return [.teamUpdate, .decision, .blocker, .meeting, .recognition, .approval, .milestone, .retrospective, .riskFlag, .activityLog, .poll, .memory]
        }
    }
}

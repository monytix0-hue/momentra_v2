import SwiftUI

enum BusinessSetupKind: String, CaseIterable, Identifiable {
    case teamOperations = "TEAM_OPERATIONS"
    case businessRunway = "BUSINESS_RUNWAY"
    case businessOperations = "BUSINESS_OPERATIONS"

    var id: String { rawValue }

    var title: String {
        switch self {
        case .teamOperations: return "Set up Team Operations"
        case .businessRunway: return "Set up Business Runway"
        case .businessOperations: return "Set up Business Operations"
        }
    }

    var familyCode: String { rawValue }

    var analyticsScreen: String {
        switch self {
        case .teamOperations: return AnalyticsScreens.businessSetupTeamOps
        case .businessRunway: return AnalyticsScreens.businessSetupRunway
        case .businessOperations: return AnalyticsScreens.businessSetupOps
        }
    }

    var activateColor: Color {
        switch self {
        case .teamOperations: return Color(hex: "#10B981")
        case .businessRunway: return Color(hex: "#FBBF24")
        case .businessOperations: return Color(hex: "#818CF8")
        }
    }

    var maestroTag: String {
        switch self {
        case .teamOperations: return "business.setup.team_operations"
        case .businessRunway: return "business.setup.business_runway"
        case .businessOperations: return "business.setup.business_operations"
        }
    }

    var ctaGradient: LinearGradient {
        switch self {
        case .teamOperations:
            return LinearGradient(colors: [Color(hex: "#10B981"), Color(hex: "#34D399")], startPoint: .leading, endPoint: .trailing)
        case .businessRunway:
            return LinearGradient(colors: [Color(hex: "#FBBF24"), Color(hex: "#F59E0B")], startPoint: .leading, endPoint: .trailing)
        case .businessOperations:
            return LinearGradient(colors: [SetupTokens.bizAccent, SetupTokens.accentPurple], startPoint: .leading, endPoint: .trailing)
        }
    }
}

import SwiftUI

enum PersonalPulseFamily: Equatable {
    case lifeOperations
    case futureBuilding
    case lifestyle
    case relationships

    static func forTypeCode(_ momentTypeCode: String?) -> PersonalPulseFamily {
        guard let raw = momentTypeCode?.uppercased() else { return .lifeOperations }
        if raw.hasPrefix("LIFE_") || raw == "LIFE_OPERATIONS" || raw == "LIFE_RHYTHM" {
            return .lifeOperations
        }
        if raw.hasPrefix("FUTURE_") || raw == "FUTURE_BUILDING" {
            return .futureBuilding
        }
        if raw.hasPrefix("LIFESTYLE") {
            return .lifestyle
        }
        if raw.hasPrefix("RELATIONSHIP_") || raw == "RELATIONSHIPS" {
            return .relationships
        }
        return .lifeOperations
    }
}

struct PersonalPulseFamilyTheme {
    let heroTitle: String
    let heroSubtitleFilled: String
    let heroSubtitleEmpty: String
    let heroMetrics: [String]
    let tileLabels: [String]
    let nudgeTitle: String
    let nudgeBody: String
    let nudgeCta: String
    let moneyTitle: String
    let quickActions: [String]
    let heroStart: Color
    let heroEnd: Color
    let accent: Color
}

extension PersonalPulseFamily {
    var theme: PersonalPulseFamilyTheme {
        switch self {
        case .lifeOperations:
            return PersonalPulseFamilyTheme(
                heroTitle: "WELLBEING SCORE",
                heroSubtitleFilled: "Your rhythm is building",
                heroSubtitleEmpty: "Awaiting first signals",
                heroMetrics: ["Pressure", "Recovery", "Discipline", "Attention"],
                tileLabels: ["Pressure", "Recovery", "Discipline", "Attention"],
                nudgeTitle: "Protect Recovery",
                nudgeBody: "Add a recovery block before your next busy stretch.",
                nudgeCta: "Log Recovery Now",
                moneyTitle: "MONEY SNAPSHOT",
                quickActions: ["Recovery", "Attention", "Mood", "Money", "Adjust"],
                heroStart: Color(hex: "#7C5CFC"),
                heroEnd: Color(hex: "#A78BFA"),
                accent: Color(hex: "#7C5CFC")
            )
        case .futureBuilding:
            // SCREEN_STALE fix (G8): align with MomentThemes / matrix emerald `#10B981` / `#34D399`.
            return PersonalPulseFamilyTheme(
                heroTitle: "FUTURE SCORE",
                heroSubtitleFilled: "Your trajectory is strong",
                heroSubtitleEmpty: "Awaiting first signals",
                heroMetrics: ["Vision", "Growth", "Momentum", "Discipline"],
                tileLabels: ["Vision", "Growth", "Momentum", "Discipline"],
                nudgeTitle: "Accelerate Growth",
                nudgeBody: "Log a milestone to keep momentum compounding.",
                nudgeCta: "Log Milestone",
                moneyTitle: "INVESTMENT SNAPSHOT",
                quickActions: ["Milestone", "Opportunity", "Pivot", "Progress", "Learning"],
                heroStart: Color(hex: "#10B981"),
                heroEnd: Color(hex: "#34D399"),
                accent: Color(hex: "#10B981")
            )
        case .lifestyle:
            return PersonalPulseFamilyTheme(
                heroTitle: "VITALITY INDEX",
                heroSubtitleFilled: "Network stability · Flourishing",
                heroSubtitleEmpty: "Awaiting first signals",
                heroMetrics: ["Joy", "Fulfillment", "Vitality", "Exploration"],
                tileLabels: ["Joy", "Fulfillment", "Vitality", "Exploration"],
                nudgeTitle: "Protect a ritual",
                nudgeBody: "Log one experience to protect your lifestyle rhythm.",
                nudgeCta: "Log Experience",
                moneyTitle: "LIFESTYLE SPEND",
                quickActions: ["Experience", "Wellbeing", "Discovery", "Create", "Adjust"],
                heroStart: Color(hex: "#0EA5A4"),
                heroEnd: Color(hex: "#7C5CFC"),
                accent: Color(hex: "#7C5CFC")
            )
        case .relationships:
            return PersonalPulseFamilyTheme(
                heroTitle: "BOND INDEX",
                heroSubtitleFilled: "Stable and deepening",
                heroSubtitleEmpty: "Awaiting first signals",
                heroMetrics: ["Trust", "Care", "Support", "Presence"],
                tileLabels: ["Trust", "Care", "Support", "Presence"],
                nudgeTitle: "Protect Connection",
                nudgeBody: "Log a connection before the next busy stretch.",
                nudgeCta: "Log Connection",
                moneyTitle: "SHARED SPEND",
                quickActions: ["Connection", "Shared", "Investment", "Support", "Adjust"],
                heroStart: Color(hex: "#E91E63"),
                heroEnd: Color(hex: "#A78BFA"),
                accent: Color(hex: "#E12A9E")
            )
        }
    }
}

func pulseQuickActionSymbol(_ label: String) -> String {
    switch label.lowercased() {
    case "recovery": return "waveform.path.ecg"
    case "attention": return "scope"
    case "mood", "reflection", "reflect": return "face.smiling"
    case "money": return "wallet.pass"
    case "adjust", "inbox": return "slider.horizontal.3"
    case "milestone", "progress", "growth", "learning", "opportunity", "post": return "chart.line.uptrend.xyaxis"
    case "chat", "check-in", "plan", "support", "presence": return "person.2"
    default: return "bolt.fill"
    }
}

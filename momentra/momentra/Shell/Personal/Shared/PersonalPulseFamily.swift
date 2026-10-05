import SwiftUI

enum PersonalPulseFamily: String, Equatable, CaseIterable {
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
    let todayActionLabels: [String]
    let nudgeTitle: String
    let nudgeBody: String
    let nudgeCta: String
    let moneyTitle: String
    let moneyCompactTitle: String
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
                heroTitle: "YOUR DAY",
                heroSubtitleFilled: "Your day is taking shape",
                heroSubtitleEmpty: "Nothing logged yet",
                heroMetrics: ["Pressure", "Recovery", "Discipline", "Attention"],
                tileLabels: ["Pressure", "Recovery", "Discipline", "Attention"],
                todayActionLabels: ["Spend", "Mood", "Recovery"],
                nudgeTitle: "A small win today",
                nudgeBody: "Log recovery if you want it on today's record.",
                nudgeCta: "Log Recovery",
                moneyTitle: "This month's money",
                moneyCompactTitle: "This month",
                quickActions: ["Recovery", "Attention", "Mood", "Adjust"],
                heroStart: Color(hex: "#7C5CFC"),
                heroEnd: Color(hex: "#A78BFA"),
                accent: Color(hex: "#7C5CFC")
            )
        case .futureBuilding:
            return PersonalPulseFamilyTheme(
                heroTitle: "YOUR FUTURE",
                heroSubtitleFilled: "Your trajectory is strong",
                heroSubtitleEmpty: "Nothing logged yet",
                heroMetrics: ["Vision", "Growth", "Momentum", "Discipline"],
                tileLabels: ["Vision", "Growth", "Momentum", "Discipline"],
                todayActionLabels: ["Milestone", "Progress", "Learning"],
                nudgeTitle: "Keep building",
                nudgeBody: "Log a milestone to keep momentum compounding.",
                nudgeCta: "Log Milestone",
                moneyTitle: "This month's investments",
                moneyCompactTitle: "This month",
                quickActions: ["Milestone", "Opportunity", "Pivot", "Progress", "Learning"],
                heroStart: Color(hex: "#10B981"),
                heroEnd: Color(hex: "#34D399"),
                accent: Color(hex: "#10B981")
            )
        case .lifestyle:
            return PersonalPulseFamilyTheme(
                heroTitle: "YOUR LIFESTYLE",
                heroSubtitleFilled: "Your lifestyle is taking shape",
                heroSubtitleEmpty: "Nothing logged yet",
                heroMetrics: ["Joy", "Fulfillment", "Vitality", "Exploration"],
                tileLabels: ["Joy", "Fulfillment", "Vitality", "Exploration"],
                todayActionLabels: ["Experience", "Wellbeing", "Discovery"],
                nudgeTitle: "Protect a ritual",
                nudgeBody: "Log one experience to protect your lifestyle rhythm.",
                nudgeCta: "Log Experience",
                moneyTitle: "This month's lifestyle spend",
                moneyCompactTitle: "This month",
                quickActions: ["Experience", "Wellbeing", "Discovery", "Create", "Adjust"],
                heroStart: Color(hex: "#0EA5A4"),
                heroEnd: Color(hex: "#A78BFA"),
                accent: Color(hex: "#7C5CFC")
            )
        case .relationships:
            return PersonalPulseFamilyTheme(
                heroTitle: "YOUR PEOPLE",
                heroSubtitleFilled: "Your bonds are deepening",
                heroSubtitleEmpty: "Nothing logged yet",
                heroMetrics: ["Trust", "Care", "Support", "Presence"],
                tileLabels: ["Trust", "Care", "Support", "Presence"],
                todayActionLabels: ["Connect", "Shared", "Support"],
                nudgeTitle: "Stay close",
                nudgeBody: "Log a connection before the next busy stretch.",
                nudgeCta: "Log Connection",
                moneyTitle: "This month's shared spend",
                moneyCompactTitle: "This month",
                quickActions: ["Connection", "Shared", "Investment", "Support", "Adjust"],
                heroStart: Color(hex: "#E91E63"),
                heroEnd: Color(hex: "#A78BFA"),
                accent: Color(hex: "#E12A9E")
            )
        }
    }

    var loggingLabel: String {
        switch self {
        case .lifeOperations: return "Life"
        case .futureBuilding: return "Future"
        case .lifestyle: return "Lifestyle"
        case .relationships: return "Relationships"
        }
    }

    var switcherLabel: String {
        switch self {
        case .lifeOperations: return "Everyday"
        case .futureBuilding: return "Future"
        case .lifestyle: return "Lifestyle"
        case .relationships: return "People"
        }
    }
}

struct VisiblePulseNudge: Equatable {
    let title: String
    let body: String?
    let cta: String
}

extension PersonalPulseFamily {
    /// Everyday nudge is omitted until today has a log, and never claims the person has been busy.
    /// Other families keep their instruction copy.
    func visibleNudge(todayLogCount: Int) -> VisiblePulseNudge? {
        if self == .lifeOperations && todayLogCount <= 0 { return nil }
        let body: String? = {
            if self == .lifeOperations && theme.nudgeBody.localizedCaseInsensitiveContains("busy") {
                return nil
            }
            return theme.nudgeBody
        }()
        return VisiblePulseNudge(title: theme.nudgeTitle, body: body, cta: theme.nudgeCta)
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

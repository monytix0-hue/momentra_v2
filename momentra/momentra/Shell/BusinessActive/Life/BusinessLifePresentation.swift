import Foundation

enum BusinessLifeLens: String, CaseIterable, Equatable {
    case overview
    case money
    case daily
    case team

    var label: String {
        switch self {
        case .overview: return "Overview"
        case .money: return "Money"
        case .daily: return "Daily"
        case .team: return "Team"
        }
    }
}

enum BusinessLifeMovementTab: Equatable {
    case activity
    case money
}

struct BusinessLifeSignalFact: Equatable {
    let title: String
    let family: String
    let statusLabel: String
}

struct BusinessLifeActivityFact: Equatable {
    let title: String
    let occurredAt: String
    let activityCode: String
}

struct BusinessLifeFacts: Equatable {
    var sections: [String: String] = [:]
    var runway: [String: String] = [:]
    var daily: [String: String] = [:]
    var team: [String: String] = [:]
    var runwayStatus: String?
    var dailyStatus: String?
    var teamStatus: String?
    var runwayMonths: String?
    var signals: [BusinessLifeSignalFact] = []
    var activity: [BusinessLifeActivityFact] = []
}

struct BusinessLifeStateLine: Equatable {
    let title: String
    let state: String
}

struct BusinessLifeValueLine: Equatable {
    let label: String
    let value: String
}

struct BusinessLifeCompanyState: Equatable {
    let overview: [BusinessLifeStateLine]
    let thisWeek: String
    let activity: [BusinessLifeActivityFact]
    let moneyMovement: [BusinessLifeValueLine]
    let needsAttention: [String]
    let working: [String]
    let financialPosition: [BusinessLifeValueLine]
}

let businessLifeFinanceScope = "Company totals"

private let businessLifeGenericStates: Set<String> = [
    "money & cash flow",
    "daily business",
    "team & work",
    "vendor operations",
]

func businessLifeLens(momentTypeCode: String?) -> BusinessLifeLens {
    let code = (momentTypeCode ?? "").uppercased()
    if code.contains("TEAM") { return .team }
    if code.contains("RUNWAY") { return .money }
    if code.contains("OPERATIONS") { return .daily }
    return .overview
}

func movementTab(for lens: BusinessLifeLens) -> BusinessLifeMovementTab {
    lens == .money ? .money : .activity
}

func buildBusinessLifeCompanyState(
    facts: BusinessLifeFacts,
    now: Date,
    timeZone: TimeZone
) -> BusinessLifeCompanyState {
    let overview = [
        BusinessLifeStateLine(
            title: "Money",
            state: familyState(
                section: facts.sections["runway"],
                payload: facts.runway,
                statusLabel: facts.runwayStatus,
                signals: facts.signals,
                families: ["RUNWAY"]
            )
        ),
        BusinessLifeStateLine(
            title: "Daily Business",
            state: familyState(
                section: facts.sections["businessOperations"],
                payload: facts.daily,
                statusLabel: facts.dailyStatus,
                signals: facts.signals,
                families: ["OPERATIONS"]
            )
        ),
        BusinessLifeStateLine(
            title: "Team",
            state: familyState(
                section: facts.sections["teamOperations"],
                payload: facts.team,
                statusLabel: facts.teamStatus,
                signals: facts.signals,
                families: ["TEAM_OPS", "TEAM_OPERATIONS"]
            )
        ),
    ]
    return BusinessLifeCompanyState(
        overview: overview,
        thisWeek: weekSummary(facts.activity, now: now, timeZone: timeZone),
        activity: facts.activity,
        moneyMovement: moneyMovement(facts),
        needsAttention: facts.signals
            .filter { $0.statusLabel.caseInsensitiveCompare("Action") == .orderedSame || $0.statusLabel.caseInsensitiveCompare("Watch") == .orderedSame }
            .map(\.title)
            .filter { !$0.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty },
        working: facts.signals
            .filter { $0.statusLabel.caseInsensitiveCompare("Healthy") == .orderedSame }
            .map(\.title)
            .filter { !$0.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty },
        financialPosition: financialPosition(facts)
    )
}

func businessLifeFacts(from inner: APIClient.BusinessLifePayload.LifeInner?) -> BusinessLifeFacts {
    guard let inner else { return BusinessLifeFacts() }
    return BusinessLifeFacts(
        sections: inner.sections ?? [:],
        runway: stringMap(inner.runwayPayload),
        daily: stringMap(inner.businessOperationsPayload),
        team: stringMap(inner.teamOperationsPayload),
        runwayStatus: statusLabel(inner.runwayPayload) ?? inner.modules?.runway?.statusLabel,
        dailyStatus: statusLabel(inner.businessOperationsPayload) ?? inner.modules?.businessOperations?.statusLabel,
        teamStatus: statusLabel(inner.teamOperationsPayload) ?? inner.modules?.teamOperations?.statusLabel,
        runwayMonths: inner.kpis?.runwayMonths,
        signals: (inner.signals ?? []).map {
            BusinessLifeSignalFact(title: $0.title, family: $0.family ?? "", statusLabel: $0.statusLabel ?? "")
        },
        activity: (inner.activity ?? []).map {
            BusinessLifeActivityFact(title: $0.title, occurredAt: $0.occurredAt, activityCode: $0.activityCode)
        }
    )
}

private func familyState(
    section: String?,
    payload: [String: String],
    statusLabel: String?,
    signals: [BusinessLifeSignalFact],
    families: Set<String>
) -> String {
    if section == "EMPTY_SUPPORTED" || payload.isEmpty { return "Not set up" }
    let own = signals.filter { families.contains($0.family.uppercased()) }
    if own.contains(where: { $0.statusLabel.caseInsensitiveCompare("Action") == .orderedSame }) { return "Needs attention" }
    if own.contains(where: { $0.statusLabel.caseInsensitiveCompare("Watch") == .orderedSame }) { return "Watch" }
    let label = statusLabel?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
    if !label.isEmpty && !businessLifeGenericStates.contains(label.lowercased()) { return label }
    if own.contains(where: { $0.statusLabel.caseInsensitiveCompare("Healthy") == .orderedSame }) { return "Steady" }
    return "Recorded"
}

private func weekSummary(_ activity: [BusinessLifeActivityFact], now: Date, timeZone: TimeZone) -> String {
    if activity.isEmpty { return "Nothing recorded yet." }
    var calendar = Calendar(identifier: .gregorian)
    calendar.timeZone = timeZone
    let startDay = calendar.date(byAdding: .day, value: -6, to: calendar.startOfDay(for: now)) ?? now
    let formatter = ISO8601DateFormatter()
    formatter.formatOptions = [.withInternetDateTime]
    let count = activity.filter { fact in
        guard let at = formatter.date(from: fact.occurredAt) else { return false }
        return at >= startDay && at <= now.addingTimeInterval(60)
    }.count
    if count == 0 { return "No recent company events are from this week." }
    let noun = count == 1 ? "event is" : "events are"
    return "\(count) recent company \(noun) from this week."
}

private func moneyMovement(_ facts: BusinessLifeFacts) -> [BusinessLifeValueLine] {
    var lines: [BusinessLifeValueLine] = []
    if let value = presentValue(facts.runway["availableCash"]) { lines.append(.init(label: "Available cash", value: value)) }
    if let value = presentValue(facts.runway["revenueTotal"]) { lines.append(.init(label: "Revenue", value: value)) }
    if let value = presentValue(facts.runway["expenseTotal"]) { lines.append(.init(label: "Expenses", value: value)) }
    if let value = presentValue(facts.runway["monthlyRevenue"]) { lines.append(.init(label: "Monthly revenue", value: value)) }
    if let value = presentValue(facts.runway["monthlySpending"]) { lines.append(.init(label: "Monthly spending", value: value)) }
    return lines
}

private func financialPosition(_ facts: BusinessLifeFacts) -> [BusinessLifeValueLine] {
    var lines: [BusinessLifeValueLine] = []
    if let value = presentValue(facts.runway["availableCash"]) { lines.append(.init(label: "Available cash", value: value)) }
    if let value = presentValue(facts.runway["revenueTotal"]) { lines.append(.init(label: "Revenue", value: value)) }
    if let value = presentValue(facts.runway["expenseTotal"]) { lines.append(.init(label: "Expenses", value: value)) }
    if let value = presentValue(facts.runwayMonths) { lines.append(.init(label: "Runway", value: "\(value) months")) }
    return lines
}

private func presentValue(_ value: String?) -> String? {
    guard let text = value?.trimmingCharacters(in: .whitespacesAndNewlines), !text.isEmpty, text.lowercased() != "null" else {
        return nil
    }
    return text
}

private func stringMap(_ raw: [String: AnyDecodable]?) -> [String: String] {
    guard let raw else { return [:] }
    var out: [String: String] = [:]
    for (key, item) in raw {
        if item.value is NSNull { continue }
        let text = String(describing: item.value).trimmingCharacters(in: .whitespacesAndNewlines)
        if text.isEmpty || text == "<null>" { continue }
        out[key] = text
    }
    return out
}

private func statusLabel(_ raw: [String: AnyDecodable]?) -> String? {
    guard let value = raw?["statusLabel"]?.value else { return nil }
    let text = String(describing: value).trimmingCharacters(in: .whitespacesAndNewlines)
    return text.isEmpty ? nil : text
}

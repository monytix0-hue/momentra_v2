import Foundation

struct BusinessMemoryItemFact: Equatable {
    var title: String
    var body: String? = nil
    var occurredAt: String? = nil
    var memoryType: String? = nil
    var businessFamily: String? = nil
}

struct BusinessMemoryWorthItem: Equatable {
    var title: String
    var familyLabel: String
    var dateLabel: String?
    var businessFamily: String?
}

struct BusinessMemoryPattern: Equatable {
    var title: String
    var evidence: [String]
}

struct BusinessMemoryComparison: Equatable {
    var thenTitle: String
    var thenDate: String
    var nowTitle: String
    var nowDate: String
}

struct BusinessMemoryPresentation: Equatable {
    var heroPeriod: String
    var heroSentence: String
    var heroCount: Int
    var worthRemembering: [BusinessMemoryWorthItem]
    var pattern: BusinessMemoryPattern?
    var worked: [String]
    var didnt: [String]
    var thenNow: BusinessMemoryComparison?
}

let businessMemoryWorthEmpty = "Nothing saved yet."
let businessMemoryWorkedEmpty = "No success memories yet."
let businessMemoryDidntEmpty = "No risk memories yet."

func businessMemoryIsRisk(title: String?, body: String?) -> Bool {
    let hay = "\(title ?? "") \(body ?? "")".lowercased()
    return hay.contains("risk") || hay.contains("issue") || hay.contains("incident")
}

func businessMemoryFamilyLabel(_ family: String?) -> String {
    let code = (family ?? "").uppercased()
    if code.contains("TEAM") { return "Team" }
    if code.contains("RUNWAY") { return "Money" }
    if code.contains("OPERATIONS") { return "Daily" }
    return ""
}

func buildBusinessMemoryPresentation(
    items: [BusinessMemoryItemFact],
    now: Date,
    zone: TimeZone
) -> BusinessMemoryPresentation {
    _ = now
    let dated = items.compactMap { datedMemory($0, zone: zone) }
    let dates = uniqueLocalDates(dated)
    let period: String
    if dates.isEmpty {
        period = "Company memory"
    } else if dates.count == 1 {
        period = formatMemoryDate(dates[0], zone: zone)
    } else {
        let sorted = dates.sorted()
        period = "\(formatMemoryDate(sorted[0], zone: zone)) – \(formatMemoryDate(sorted[sorted.count - 1], zone: zone))"
    }
    let risk = items.filter { businessMemoryIsRisk(title: $0.title, body: $0.body) }
    let worked = items.filter { !businessMemoryIsRisk(title: $0.title, body: $0.body) }
    return BusinessMemoryPresentation(
        heroPeriod: period,
        heroSentence: items.isEmpty ? businessMemoryWorthEmpty : "\(items.count) saved memories.",
        heroCount: items.count,
        worthRemembering: items.map { worthItem($0, zone: zone) },
        pattern: memoryPattern(items: items, risk: risk),
        worked: worked.map { displayTitle($0.title) },
        didnt: risk.map { displayTitle($0.title) },
        thenNow: memoryComparison(dated, zone: zone)
    )
}

func filterWorthRemembering(
    _ items: [BusinessMemoryWorthItem],
    lens: BusinessLifeLens
) -> [BusinessMemoryWorthItem] {
    switch lens {
    case .overview:
        return items
    case .money:
        return items.filter { ($0.businessFamily ?? "").uppercased().contains("RUNWAY") }
    case .daily:
        return items.filter {
            let code = ($0.businessFamily ?? "").uppercased()
            return code.contains("OPERATIONS") && !code.contains("TEAM")
        }
    case .team:
        return items.filter { ($0.businessFamily ?? "").uppercased().contains("TEAM") }
    }
}

private struct DatedMemory {
    var item: BusinessMemoryItemFact
    var instant: Date
    var localDate: Date
}

private func datedMemory(_ item: BusinessMemoryItemFact, zone: TimeZone) -> DatedMemory? {
    let raw = item.occurredAt?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
    guard !raw.isEmpty, let instant = parseMemoryInstant(raw) else { return nil }
    return DatedMemory(item: item, instant: instant, localDate: localDay(instant, zone: zone))
}

private func memoryComparison(_ dated: [DatedMemory], zone: TimeZone) -> BusinessMemoryComparison? {
    let dates = uniqueLocalDates(dated)
    guard dates.count >= 2 else { return nil }
    let earliest = dated.min { lhs, rhs in
        if lhs.localDate != rhs.localDate { return lhs.localDate < rhs.localDate }
        return lhs.instant < rhs.instant
    }
    let latest = dated.max { lhs, rhs in
        if lhs.localDate != rhs.localDate { return lhs.localDate < rhs.localDate }
        return lhs.instant < rhs.instant
    }
    guard let earliest, let latest else { return nil }
    return BusinessMemoryComparison(
        thenTitle: displayTitle(earliest.item.title),
        thenDate: formatMemoryDate(earliest.localDate, zone: zone),
        nowTitle: displayTitle(latest.item.title),
        nowDate: formatMemoryDate(latest.localDate, zone: zone)
    )
}

private func memoryPattern(
    items: [BusinessMemoryItemFact],
    risk: [BusinessMemoryItemFact]
) -> BusinessMemoryPattern? {
    let grouped = Dictionary(grouping: items.filter { typeKey($0.memoryType) != nil }) {
        typeKey($0.memoryType) ?? ""
    }
    let repeated = grouped.max { $0.value.count < $1.value.count }
    let typeQualifies = (repeated?.value.count ?? 0) >= 2
    let riskQualifies = risk.count >= 2
    if riskQualifies && typeQualifies, let repeated {
        if repeated.value.count > risk.count {
            return typePattern(repeated.key, repeated.value)
        }
        return riskPattern(risk)
    }
    if riskQualifies { return riskPattern(risk) }
    if typeQualifies, let repeated { return typePattern(repeated.key, repeated.value) }
    return nil
}

private func riskPattern(_ items: [BusinessMemoryItemFact]) -> BusinessMemoryPattern {
    BusinessMemoryPattern(
        title: "\(items.count) memories mention risk",
        evidence: items.map { displayTitle($0.title) }
    )
}

private func typePattern(_ type: String, _ items: [BusinessMemoryItemFact]) -> BusinessMemoryPattern {
    let lower = type.lowercased()
    let label = lower.prefix(1).uppercased() + lower.dropFirst()
    return BusinessMemoryPattern(
        title: "\(items.count) \(label) memories",
        evidence: items.map { displayTitle($0.title) }
    )
}

private func typeKey(_ memoryType: String?) -> String? {
    let key = (memoryType ?? "").trimmingCharacters(in: .whitespacesAndNewlines).uppercased()
    if key.isEmpty || key == "GENERAL" { return nil }
    return key
}

private func worthItem(_ item: BusinessMemoryItemFact, zone: TimeZone) -> BusinessMemoryWorthItem {
    let dated = datedMemory(item, zone: zone)
    return BusinessMemoryWorthItem(
        title: displayTitle(item.title),
        familyLabel: businessMemoryFamilyLabel(item.businessFamily),
        dateLabel: dated.map { formatMemoryDate($0.localDate, zone: zone) },
        businessFamily: item.businessFamily
    )
}

private func displayTitle(_ title: String) -> String {
    let trimmed = title.trimmingCharacters(in: .whitespacesAndNewlines)
    return trimmed.isEmpty ? "Memory" : trimmed
}

private func uniqueLocalDates(_ dated: [DatedMemory]) -> [Date] {
    var seen: [Date] = []
    for row in dated where !seen.contains(row.localDate) {
        seen.append(row.localDate)
    }
    return seen
}

private func localDay(_ instant: Date, zone: TimeZone) -> Date {
    var calendar = Calendar(identifier: .gregorian)
    calendar.timeZone = zone
    let parts = calendar.dateComponents([.year, .month, .day], from: instant)
    return calendar.date(from: parts) ?? instant
}

private func formatMemoryDate(_ date: Date, zone: TimeZone) -> String {
    let formatter = DateFormatter()
    formatter.locale = Locale(identifier: "en_US_POSIX")
    formatter.timeZone = zone
    formatter.dateFormat = "d MMM yyyy"
    return formatter.string(from: date)
}

private func parseMemoryInstant(_ raw: String) -> Date? {
    let formatter = ISO8601DateFormatter()
    formatter.formatOptions = [.withInternetDateTime]
    if let date = formatter.date(from: raw) { return date }
    formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
    return formatter.date(from: raw)
}

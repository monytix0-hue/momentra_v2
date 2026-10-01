import Foundation

/// Locked M3 family-state vocabulary — same labels on iOS and Android.
enum PersonalLifeFamilyStatus: String, Equatable, CaseIterable {
    case strong = "Strong"
    case growing = "Growing"
    case steady = "Steady"
    case quiet = "Quiet"
    case needsAttention = "Needs attention"
}

enum PersonalLifeWeekTier: String, Equatable {
    case rich, partial, thin, empty
}

enum PersonalLifeAllocationMode: String, Equatable {
    case activity, money
}

struct PersonalLifeFamilyState: Equatable, Identifiable {
    var id: String { familyCode }
    let familyCode: String
    let label: String
    let status: PersonalLifeFamilyStatus
}

struct PersonalLifeWeekFamilyCount: Equatable, Identifiable {
    var id: String { familyCode }
    let familyCode: String
    let label: String
    let periodLogs: Int
}

struct PersonalLifeWeekSummary: Equatable {
    let tier: PersonalLifeWeekTier
    /// Present for rich / partial tiers only.
    let sentence: String?
    let familyCounts: [PersonalLifeWeekFamilyCount]
    let filterLabel: String?
}

struct PersonalLifeAllocationSlice: Equatable, Identifiable {
    var id: String { familyCode }
    let familyCode: String
    let label: String
    let value: Double
    /// Whole-number percent; only set when allocation has data.
    let percent: Int
}

struct PersonalLifeAllocation: Equatable {
    let hasData: Bool
    let slices: [PersonalLifeAllocationSlice]
}

struct PersonalLifeInsight: Equatable {
    let headline: String
    let body: String
    let ctaLabel: String?
    /// LOG_RECOVERY | LOG_SPEND | OPEN_ADD | nil
    let ctaAction: String?
}

struct PersonalLifeMoneySnapshot: Equatable {
    let incomeTotal: Double
    let expenseTotal: Double
    let available: Double?
    let currencyCode: String?
    let byFamilySpend: [PersonalLifeAllocationSlice]
}

/// Dumb-view contract for Life — all interpretation happens in `from`.
struct PersonalLifeSummaryModel: Equatable {
    let familyStates: [PersonalLifeFamilyState]
    let weekSummary: PersonalLifeWeekSummary
    let activityAllocation: PersonalLifeAllocation
    let moneyAllocation: PersonalLifeAllocation
    let slipping: PersonalLifeInsight?
    let working: PersonalLifeInsight?
    let moneySnapshot: PersonalLifeMoneySnapshot?
    /// Secondary only; never anchors the screen.
    let globalScore: Int?
    let scoreMax: Int

    static let canonicalFamilies: [(code: String, label: String)] = [
        ("LIFE_OPERATIONS", "Everyday"),
        ("FUTURE_BUILDING", "Future"),
        ("LIFESTYLE", "Lifestyle"),
        ("RELATIONSHIPS", "People"),
    ]

    static func from(
        _ life: APIClient.PersonalLifePayload,
        familyFilter: String? = nil
    ) -> PersonalLifeSummaryModel {
        let sq = life.sectionQuality ?? [:]
        let week = life.thisWeek
        let byFamily = week?.byFamily ?? []

        let familyStates = canonicalFamilies.map { fam in
            let row = byFamily.first { $0.familyCode.caseInsensitiveCompare(fam.code) == .orderedSame }
            let score = life.areaScores?.first { $0.code.caseInsensitiveCompare(fam.code) == .orderedSame }?.score
            let logs = row?.periodLogs ?? 0
            let expense = parseAmount(row?.expenseTotal)
            let status = deriveFamilyStatus(logs: logs, expense: expense, areaScore: score)
            return PersonalLifeFamilyState(familyCode: fam.code, label: fam.label, status: status)
        }

        let weekSummary = deriveWeekSummary(week: week, familyFilter: familyFilter)

        let activityAllocation = deriveActivityAllocation(byFamily: byFamily)
        let moneyAllocation = deriveMoneyAllocation(byFamily: byFamily)

        let slipping = deriveSlipping(drift: life.drift, sectionQuality: sq)
        let working = deriveWorking(leverage: life.leverage, sectionQuality: sq)

        let moneySnapshot = deriveMoneySnapshot(week: week)
        let globalScore = life.score

        return PersonalLifeSummaryModel(
            familyStates: familyStates,
            weekSummary: weekSummary,
            activityAllocation: activityAllocation,
            moneyAllocation: moneyAllocation,
            slipping: slipping,
            working: working,
            moneySnapshot: moneySnapshot,
            globalScore: globalScore,
            scoreMax: life.scoreMax ?? 100
        )
    }

    // MARK: - Family status

    /// Deterministic rules shared with Android.
    /// Weak / insufficient → Quiet or Steady only (never Strong / Growing).
    static func deriveFamilyStatus(logs: Int, expense: Double, areaScore: Int?) -> PersonalLifeFamilyStatus {
        let hasRichSignal = logs >= 2 || (logs >= 1 && expense > 0) || areaScore != nil
        if !hasRichSignal {
            return (logs == 0 && expense <= 0) ? .quiet : .steady
        }
        if let score = areaScore {
            if score < 40 { return .needsAttention }
            if score >= 75 && logs >= 2 { return .strong }
            if score >= 55 || logs >= 4 { return .growing }
            return .steady
        }
        if logs >= 5 { return .strong }
        if logs >= 3 { return .growing }
        return .steady
    }

    // MARK: - This week

    private static func deriveWeekSummary(
        week: APIClient.PersonalLifePayload.ThisWeek?,
        familyFilter: String?
    ) -> PersonalLifeWeekSummary {
        let filterLabel: String? = {
            switch familyFilter?.uppercased() {
            case "LIFE_OPERATIONS": return "Everyday"
            case "FUTURE_BUILDING": return "Future"
            case "LIFESTYLE": return "Lifestyle"
            case "RELATIONSHIPS": return "People"
            default: return nil
            }
        }()

        let rows = (week?.byFamily ?? []).filter { row in
            guard let familyFilter else { return true }
            return row.familyCode.caseInsensitiveCompare(familyFilter) == .orderedSame
        }

        let familyCounts: [PersonalLifeWeekFamilyCount] = {
            if let familyFilter {
                let row = rows.first
                let meta = canonicalFamilies.first { $0.code.caseInsensitiveCompare(familyFilter) == .orderedSame }
                let logs = row?.periodLogs ?? 0
                return [PersonalLifeWeekFamilyCount(
                    familyCode: familyFilter,
                    label: meta?.label ?? row?.label ?? familyFilter,
                    periodLogs: logs
                )]
            }
            return canonicalFamilies.map { fam in
                let row = (week?.byFamily ?? []).first {
                    $0.familyCode.caseInsensitiveCompare(fam.code) == .orderedSame
                }
                return PersonalLifeWeekFamilyCount(
                    familyCode: fam.code,
                    label: fam.label,
                    periodLogs: row?.periodLogs ?? 0
                )
            }
        }()

        let totalLogs: Int = {
            if familyFilter != nil {
                return familyCounts.first?.periodLogs ?? 0
            }
            return week?.periodLogs ?? familyCounts.reduce(0) { $0 + $1.periodLogs }
        }()

        let highlights = (week?.highlights ?? []).filter { h in
            guard let familyFilter else { return true }
            return h.familyCode.caseInsensitiveCompare(familyFilter) == .orderedSame
        }
        let highlightTitles = highlights.map(\.title).filter { !$0.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty }
        let familiesWithLogs = familyCounts.filter { $0.periodLogs > 0 }.count

        if totalLogs <= 0 {
            return PersonalLifeWeekSummary(
                tier: .empty,
                sentence: nil,
                familyCounts: familyCounts,
                filterLabel: filterLabel
            )
        }

        if totalLogs <= 2 && highlightTitles.isEmpty {
            return PersonalLifeWeekSummary(
                tier: .thin,
                sentence: nil,
                familyCounts: familyCounts.filter { $0.periodLogs > 0 },
                filterLabel: filterLabel
            )
        }

        if highlightTitles.isEmpty || familiesWithLogs < 2 {
            let sentence = totalLogs == 1
                ? "1 activity logged this week"
                : "\(totalLogs) activities logged this week"
            return PersonalLifeWeekSummary(
                tier: .partial,
                sentence: sentence,
                familyCounts: familyCounts.filter { $0.periodLogs > 0 },
                filterLabel: filterLabel
            )
        }

        let first = highlightTitles[0]
        let sentence = familiesWithLogs > 1
            ? "\(first) — activity across \(familiesWithLogs) areas"
            : first
        return PersonalLifeWeekSummary(
            tier: .rich,
            sentence: sentence,
            familyCounts: familyCounts.filter { $0.periodLogs > 0 },
            filterLabel: filterLabel
        )
    }

    // MARK: - Allocation

    private static func deriveActivityAllocation(
        byFamily: [APIClient.PersonalLifePayload.ByFamily]
    ) -> PersonalLifeAllocation {
        let values: [(String, String, Double)] = canonicalFamilies.map { fam in
            let row = byFamily.first { $0.familyCode.caseInsensitiveCompare(fam.code) == .orderedSame }
            return (fam.code, fam.label, Double(row?.periodLogs ?? 0))
        }
        let total = values.reduce(0.0) { $0 + $1.2 }
        guard total > 0 else {
            return PersonalLifeAllocation(hasData: false, slices: [])
        }
        let slices: [PersonalLifeAllocationSlice] = values.compactMap { item in
            guard item.2 > 0 else { return nil }
            let pct = Int((item.2 / total * 100.0).rounded())
            return PersonalLifeAllocationSlice(
                familyCode: item.0,
                label: item.1,
                value: item.2,
                percent: pct
            )
        }
        return PersonalLifeAllocation(hasData: true, slices: rebalancePercents(slices))
    }

    private static func deriveMoneyAllocation(
        byFamily: [APIClient.PersonalLifePayload.ByFamily]
    ) -> PersonalLifeAllocation {
        let values: [(String, String, Double)] = canonicalFamilies.map { fam in
            let row = byFamily.first { $0.familyCode.caseInsensitiveCompare(fam.code) == .orderedSame }
            return (fam.code, fam.label, parseAmount(row?.expenseTotal))
        }
        let total = values.reduce(0.0) { $0 + $1.2 }
        guard total > 0 else {
            return PersonalLifeAllocation(hasData: false, slices: [])
        }
        let slices: [PersonalLifeAllocationSlice] = values.compactMap { item in
            guard item.2 > 0 else { return nil }
            let pct = Int((item.2 / total * 100.0).rounded())
            return PersonalLifeAllocationSlice(
                familyCode: item.0,
                label: item.1,
                value: item.2,
                percent: pct
            )
        }
        return PersonalLifeAllocation(hasData: true, slices: rebalancePercents(slices))
    }

    private static func rebalancePercents(_ slices: [PersonalLifeAllocationSlice]) -> [PersonalLifeAllocationSlice] {
        guard !slices.isEmpty else { return slices }
        let sum = slices.reduce(0) { $0 + $1.percent }
        let delta = 100 - sum
        guard delta != 0, let last = slices.indices.last else { return slices }
        var out = slices
        out[last] = PersonalLifeAllocationSlice(
            familyCode: out[last].familyCode,
            label: out[last].label,
            value: out[last].value,
            percent: max(out[last].percent + delta, 0)
        )
        return out
    }

    // MARK: - Insights

    private static func deriveSlipping(
        drift: APIClient.PersonalLifePayload.LifeDrift?,
        sectionQuality: [String: String]
    ) -> PersonalLifeInsight? {
        guard let drift else { return nil }
        if sectionQuality["drift"]?.uppercased() == "EMPTY_SUPPORTED" { return nil }
        let headline = drift.headline.trimmingCharacters(in: .whitespacesAndNewlines)
        let body = drift.body.trimmingCharacters(in: .whitespacesAndNewlines)
        if headline.isEmpty || headline.localizedCaseInsensitiveContains("not available") { return nil }
        if body.isEmpty { return nil }
        let cta = drift.ctaLabel.trimmingCharacters(in: .whitespacesAndNewlines)
        // No stable ctaAction on drift yet — never show a dead CTA button.
        return PersonalLifeInsight(headline: headline, body: body, ctaLabel: nil, ctaAction: nil)
    }

    private static func deriveWorking(
        leverage: APIClient.PersonalLifePayload.LifeLeverage?,
        sectionQuality: [String: String]
    ) -> PersonalLifeInsight? {
        guard let leverage else { return nil }
        if sectionQuality["leverage"]?.uppercased() == "EMPTY_SUPPORTED" { return nil }
        let action = (leverage.ctaAction ?? "NONE").uppercased()
        if action == "NONE" { return nil }
        let headline = leverage.actionTitle.trimmingCharacters(in: .whitespacesAndNewlines)
        let body = leverage.actionBody.trimmingCharacters(in: .whitespacesAndNewlines)
        if headline.isEmpty || headline.localizedCaseInsensitiveContains("not available") { return nil }
        if body.isEmpty { return nil }
        let cta = leverage.ctaLabel.trimmingCharacters(in: .whitespacesAndNewlines)
        let ctaLabel = (cta.isEmpty || cta.localizedCaseInsensitiveContains("coming soon")) ? nil : cta
        return PersonalLifeInsight(
            headline: headline,
            body: body,
            ctaLabel: ctaLabel,
            ctaAction: action
        )
    }

    // MARK: - Money snapshot

    private static func deriveMoneySnapshot(
        week: APIClient.PersonalLifePayload.ThisWeek?
    ) -> PersonalLifeMoneySnapshot? {
        guard let week else { return nil }
        let income = parseAmount(week.incomeTotal)
        let expense = parseAmount(week.expenseTotal)
        guard income > 0 || expense > 0 else { return nil }
        let currency = week.currencyCode
            ?? week.spendByCurrency?.max(by: { parseAmount($0.value) < parseAmount($1.value) })?.key
        let byFamilySpend = deriveMoneyAllocation(byFamily: week.byFamily ?? []).slices
        let available: Double? = (income > 0) ? (income - expense) : nil
        return PersonalLifeMoneySnapshot(
            incomeTotal: income,
            expenseTotal: expense,
            available: available,
            currencyCode: currency,
            byFamilySpend: byFamilySpend
        )
    }

    static func parseAmount(_ raw: String?) -> Double {
        guard let raw, !raw.isEmpty else { return 0 }
        return Double(raw) ?? 0
    }

    static func formatMoney(_ amount: Double, currencyCode: String?) -> String {
        let symbol = (currencyCode == nil || currencyCode == "INR") ? "₹" : "\(currencyCode!) "
        if amount == floor(amount) {
            return "\(symbol)\(Int(amount))"
        }
        return String(format: "%@%.2f", symbol, amount)
    }
}

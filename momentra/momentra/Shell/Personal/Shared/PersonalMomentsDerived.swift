import Foundation

enum PersonalMomentsHighlight {
    struct Item: Equatable {
        let title: String
        let detail: String
    }

    /// One optional highlight; omit when nothing qualifies.
    static func select(
        family: PersonalPulseFamily,
        activities: [APIClient.ActivityItemPayload]
    ) -> Item? {
        let visible = activities.filter { PersonalActivityTimelineDerived.isVisible($0) }
        guard !visible.isEmpty else { return nil }
        switch family {
        case .lifeOperations:
            if let recovery = visible.first(where: { $0.activityCode.uppercased().contains("RECOVERY") }) {
                return Item(
                    title: PersonalActivityTimelineDerived.displayTitle(recovery),
                    detail: "A recovery log that stood out"
                )
            }
            if let expense = visible.first(where: { PersonalActivityTimelineDerived.isExpense($0) }) {
                return Item(
                    title: PersonalActivityTimelineDerived.displayTitle(expense),
                    detail: "Notable spend this period"
                )
            }
            return nil
        case .futureBuilding:
            if let milestone = visible.first(where: { $0.activityCode.uppercased().contains("MILESTONE") }) {
                return Item(
                    title: PersonalActivityTimelineDerived.displayTitle(milestone),
                    detail: "Milestone logged"
                )
            }
            if let progress = visible.first(where: { $0.activityCode.uppercased().contains("PROGRESS") }) {
                return Item(
                    title: PersonalActivityTimelineDerived.displayTitle(progress),
                    detail: "Progress that moved the needle"
                )
            }
            return nil
        case .lifestyle:
            if let experience = visible.first(where: {
                let c = $0.activityCode.uppercased()
                return c.contains("EXPERIENCE") || c.contains("WELLBEING") || c.contains("DISCOVERY")
            }) {
                return Item(
                    title: PersonalActivityTimelineDerived.displayTitle(experience),
                    detail: "A moment worth keeping"
                )
            }
            return nil
        case .relationships:
            if let connection = visible.first(where: {
                let c = $0.activityCode.uppercased()
                return c.contains("CONNECTION") || c.contains("SHARED") || c.contains("SUPPORT")
            }) {
                return Item(
                    title: PersonalActivityTimelineDerived.displayTitle(connection),
                    detail: "A meaningful connection"
                )
            }
            return nil
        }
    }

    static func sectionTitle(for family: PersonalPulseFamily) -> String {
        switch family {
        case .lifeOperations: return "Turning Points"
        case .futureBuilding: return "Breakthroughs"
        case .lifestyle: return "Best Moments"
        case .relationships: return "Connections"
        }
    }
}

enum PersonalMomentsMoneyLine {
    /// Non-zero spend line only; nil when absent/zero.
    static func line(
        family: PersonalPulseFamily,
        spendPairs: [(String, String)]
    ) -> String? {
        let nonzero = spendPairs.compactMap { currency, amount -> (String, Double)? in
            guard let n = Double(amount), n > 0 else { return nil }
            return (currency, n)
        }
        guard !nonzero.isEmpty else { return nil }
        let formatted = nonzero.map { currency, n in
            let symbol = currency == "INR" ? "₹" : "\(currency) "
            let value = abs(n - n.rounded()) < 0.001 ? "\(Int(n.rounded()))" : String(format: "%.2f", n)
            return "\(symbol)\(value)"
        }.joined(separator: " · ")
        switch family {
        case .lifeOperations:
            return "\(formatted) was part of your everyday this month"
        case .futureBuilding:
            return "\(formatted) invested toward your future this month"
        case .lifestyle:
            return "\(formatted) was part of your experiences this month"
        case .relationships:
            return "\(formatted) was shared with people this month"
        }
    }
}

enum PersonalMomentsHeaderCopy {
    static func periodLabel(now: Date = Date()) -> String {
        let f = DateFormatter()
        f.dateFormat = "MMMM"
        return f.string(from: now)
    }

    /// Honest counts only — no synthetic narrative when data is thin.
    static func subtitle(momentCount: Int) -> String {
        if momentCount <= 0 { return "Nothing logged yet this period." }
        if momentCount == 1 { return "1 moment so far" }
        return "\(momentCount) moments so far"
    }
}

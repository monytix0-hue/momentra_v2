import Foundation
import SwiftUI

enum PersonalActivityTimelineDerived {
    struct FilterChip: Identifiable {
        let id: String
        let label: String
        let emoji: String
    }

    struct TimelineStats {
        let totalLogs: Int
        let thisMonth: Int
        let totalAmountLabel: String
    }

    struct RowVisual {
        let emoji: String
        let accent: Color
        let metadata: String
        let timeLabel: String
    }

    static let primaryFilters: [FilterChip] = [
        FilterChip(id: "Money", label: "Money", emoji: "💰"),
        FilterChip(id: "Attention", label: "Attention", emoji: "🎯"),
        FilterChip(id: "Recovery", label: "Recovery", emoji: "💪"),
    ]

    static let categoryFilters: [FilterChip] = [
        FilterChip(id: "Groceries", label: "Groceries", emoji: "🛒"),
        FilterChip(id: "Shopping", label: "Shopping", emoji: "🛍️"),
        FilterChip(id: "Food", label: "Food", emoji: "🍔"),
        FilterChip(id: "Housing", label: "Housing", emoji: "🏠"),
        FilterChip(id: "Transport", label: "Transport", emoji: "🚗"),
    ]

    static func matchesFilter(_ item: APIClient.ActivityItemPayload, filter: String) -> Bool {
        let code = item.activityCode.uppercased()
        let title = item.title.uppercased()
        let category = item.activityPayload?.categoryCode?.uppercased() ?? ""
        switch filter {
        case "All": return true
        case "Money": return code.contains("EXPENSE")
        case "Attention": return code.contains("ATTENTION") || title.contains("ATTENTION") || title.contains("DEEP WORK")
        case "Recovery": return code.contains("RECOVERY") || code.contains("WELLBEING") || title.contains("RECOVERY")
        case "Groceries": return category.contains("GROCER") || title.contains("GROCER")
        case "Shopping": return category.contains("SHOP") || title.contains("SHOP")
        case "Food": return category.contains("FOOD") || category.contains("CAFE") || title.contains("FOOD")
        case "Housing": return category.contains("HOUSING") || category.contains("RENT") || title.contains("HOUSING")
        case "Transport": return category.contains("TRANSPORT") || category.contains("FUEL") || title.contains("TRANSPORT")
        default: return true
        }
    }

    static func matchesSearch(_ item: APIClient.ActivityItemPayload, query: String) -> Bool {
        let q = query.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        guard !q.isEmpty else { return true }
        let haystack = [
            item.title,
            item.activityCode,
            item.activityPayload?.categoryCode,
            item.activityPayload?.amount,
            item.activityPayload?.description,
        ].compactMap { $0 }.joined(separator: " ").lowercased()
        return haystack.contains(q)
    }

    static func computeStats(_ items: [APIClient.ActivityItemPayload]) -> TimelineStats {
        let cal = Calendar.current
        let monthStart = cal.date(from: cal.dateComponents([.year, .month], from: Date())) ?? Date()
        let thisMonth = items.filter {
            guard let d = parseDate($0.occurredAt) else { return false }
            return d >= monthStart
        }.count
        let expenseAmounts = items.compactMap { item -> Double? in
            guard item.activityCode.uppercased().contains("EXPENSE") else { return nil }
            guard let raw = item.activityPayload?.amount else { return nil }
            return Double(raw)
        }
        let totalAmountLabel: String
        if expenseAmounts.isEmpty {
            totalAmountLabel = "—"
        } else {
            let sum = expenseAmounts.reduce(0, +)
            let currency = items.compactMap { $0.activityPayload?.currencyCode }.first ?? "INR"
            let symbol = currency == "INR" ? "₹" : currency
            totalAmountLabel = "\(symbol)\(Int(sum.rounded()))"
        }
        return TimelineStats(totalLogs: items.count, thisMonth: thisMonth, totalAmountLabel: totalAmountLabel)
    }

    static func rowVisual(_ item: APIClient.ActivityItemPayload) -> RowVisual {
        let code = item.activityCode.uppercased()
        let category = PersonalExpenseCategoryCatalog.labelForCode(item.activityPayload?.categoryCode)
        let amount = item.activityPayload?.amount.map { amt in
            let cur = item.activityPayload?.currencyCode == "INR" ? "₹" : (item.activityPayload?.currencyCode ?? "₹")
            return "\(cur)\(amt)"
        }
        let signal: String
        if code.contains("EXPENSE") { signal = "Low pressure" }
        else if code.contains("RECOVERY") { signal = "Calm" }
        else if code.contains("MOOD") || code.contains("WELLBEING") { signal = "Focused" }
        else if code.contains("ATTENTION") { signal = "High focus" }
        else { signal = "Logged" }
        var parts = [String]()
        parts.append(category)
        if code.contains("EXPENSE"), let amount { parts.append(amount) }
        if !code.contains("EXPENSE") { parts.append(typeLabel(code)) }
        parts.append(signal)
        return RowVisual(
            emoji: emojiFor(code),
            accent: accentFor(code),
            metadata: parts.joined(separator: " · "),
            timeLabel: formatTimelineTime(item.occurredAt)
        )
    }

    static func isExpense(_ item: APIClient.ActivityItemPayload) -> Bool {
        item.activityCode.uppercased().contains("EXPENSE") || item.activityPayload?.expenseId != nil
    }

    static func isVisible(_ item: APIClient.ActivityItemPayload) -> Bool {
        item.activityPayload?.status != "VOIDED"
    }

    private static func typeLabel(_ code: String) -> String {
        if code.contains("RECOVERY") { return "Recovery" }
        if code.contains("ATTENTION") { return "Attention" }
        if code.contains("MOOD") { return "Mood" }
        if code.contains("TRANSFER") { return "Money" }
        return code.replacingOccurrences(of: "_", with: " ").capitalized
    }

    private static func emojiFor(_ code: String) -> String {
        if code.contains("EXPENSE") { return "🛒" }
        if code.contains("RECOVERY") { return "💪" }
        if code.contains("ATTENTION") { return "🎯" }
        if code.contains("MOOD") { return "🧘" }
        if code.contains("TRANSFER") { return "💸" }
        return "•"
    }

    private static func accentFor(_ code: String) -> Color {
        if code.contains("EXPENSE") { return Color(hex: "#10B981") }
        if code.contains("RECOVERY") { return Color(hex: "#3B82F6") }
        if code.contains("ATTENTION") { return Color(hex: "#F97316") }
        if code.contains("MOOD") { return Color(hex: "#7C5CFC") }
        if code.contains("TRANSFER") { return Color(hex: "#10B981") }
        return Color(hex: "#EC4899")
    }

    private static func parseDate(_ iso: String) -> Date? {
        let f = ISO8601DateFormatter()
        f.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        if let d = f.date(from: iso) { return d }
        f.formatOptions = [.withInternetDateTime]
        return f.date(from: iso)
    }

    private static func formatTimelineTime(_ iso: String) -> String {
        guard let date = parseDate(iso) else { return iso }
        let cal = Calendar.current
        let days = cal.dateComponents([.day], from: cal.startOfDay(for: date), to: cal.startOfDay(for: Date())).day ?? 0
        let f = DateFormatter()
        f.dateFormat = "h:mm a"
        let time = f.string(from: date)
        switch days {
        case 0: return "TODAY \(time)"
        case 1: return "YESTERDAY \(time)"
        default: return "\(days) DAYS AGO \(time)"
        }
    }
}

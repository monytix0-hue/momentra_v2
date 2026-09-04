import Foundation

/// Planning category chips keyed by group moment type code.
/// Labels are shown in UI; stored `category_code` uses UPPER_SNAKE.
enum GroupPlanningCategoryCatalog {
    struct Category: Identifiable, Hashable {
        let label: String
        let code: String
        var id: String { code }
    }

    private static let byType: [String: [String]] = [
        "TRIP": ["Travel", "Stay", "Food", "Day plan", "Activity", "Other"],
        "WEDDING": ["Ceremony", "Venue", "Food", "Guests", "Travel", "Stay", "Day plan", "Other"],
        "HOUSE_PARTY": ["Food", "Travel", "Hour plan", "Decor", "Music", "Other"],
        "OFFICE_OUTING": ["Travel", "Stay", "Food", "Agenda", "Team", "Other"],
        // Shared Living
        "FLATMATES": ["Chores", "Bills", "Maintenance", "House rules", "Shopping", "Other"],
        "FAMILY_HOUSEHOLD": ["Chores", "Bills", "Maintenance", "House rules", "Shopping", "Other"],
        "CO_LIVING": ["Chores", "Bills", "Maintenance", "House rules", "Shopping", "Other"],
        "COMMUNITY_LIVING": ["Chores", "Bills", "Maintenance", "House rules", "Shopping", "Other"],
        "SHARED_LIVING": ["Chores", "Bills", "Maintenance", "House rules", "Shopping", "Other"],
        // Shared Purchase
        "GIFT_POOL": ["Funding", "Items", "Vendor", "Delivery", "Decision", "Other"],
        "GROUP_PURCHASE": ["Funding", "Items", "Vendor", "Delivery", "Decision", "Other"],
        "SHARED_ASSET": ["Funding", "Items", "Vendor", "Delivery", "Decision", "Other"],
        "COMMUNITY_PURCHASE": ["Funding", "Items", "Vendor", "Delivery", "Decision", "Other"],
    ]

    private static let fallback = ["Other"]

    static func labels(for momentTypeCode: String?) -> [String] {
        let code = (momentTypeCode ?? "").trimmingCharacters(in: .whitespacesAndNewlines).uppercased()
        guard !code.isEmpty else { return fallback }
        return byType[code] ?? fallback
    }

    static func categories(for momentTypeCode: String?) -> [Category] {
        labels(for: momentTypeCode).map { Category(label: $0, code: code(forLabel: $0)) }
    }

    static func defaultLabel(for momentTypeCode: String?) -> String {
        labels(for: momentTypeCode).first ?? "Other"
    }

    static func defaultCode(for momentTypeCode: String?) -> String {
        code(forLabel: defaultLabel(for: momentTypeCode))
    }

    static func code(forLabel label: String) -> String {
        let upper = label.trimmingCharacters(in: .whitespacesAndNewlines).uppercased()
        let cleaned = upper.replacingOccurrences(of: "[^A-Z0-9]+", with: "_", options: .regularExpression)
            .trimmingCharacters(in: CharacterSet(charactersIn: "_"))
        return cleaned.isEmpty ? "OTHER" : cleaned
    }

    static func label(forCode code: String?, momentTypeCode: String?) -> String {
        let normalized = (code ?? "").trimmingCharacters(in: .whitespacesAndNewlines).uppercased()
        if normalized.isEmpty { return defaultLabel(for: momentTypeCode) }
        if let match = categories(for: momentTypeCode).first(where: { $0.code == normalized }) {
            return match.label
        }
        return normalized.replacingOccurrences(of: "_", with: " ").capitalized
    }

    static func priorityCode(for label: String) -> String {
        switch label.trimmingCharacters(in: .whitespacesAndNewlines).lowercased() {
        case "low": return "LOW"
        case "high": return "HIGH"
        default: return "MEDIUM"
        }
    }

    static func urgencyCode(for label: String) -> String {
        label.trimmingCharacters(in: .whitespacesAndNewlines).caseInsensitiveCompare("Urgent") == .orderedSame
            ? "URGENT"
            : "NORMAL"
    }
}

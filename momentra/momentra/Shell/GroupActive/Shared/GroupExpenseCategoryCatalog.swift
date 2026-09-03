import Foundation

/// Expense category chips keyed by group moment type code.
/// Labels match Maestro ledger Quick_Add values for correlation notes.
enum GroupExpenseCategoryCatalog {
    private static let byType: [String: [String]] = [
        // Shared Experience
        "TRIP": ["Travel", "Stay", "Food", "Local Transport", "Activities", "Shopping", "Other"],
        "WEDDING": ["Venue", "Catering", "Decor", "Photography", "Travel", "Gifts", "Other"],
        "HOUSE_PARTY": ["Food", "Drinks", "Decor", "Supplies", "Other"],
        "OFFICE_OUTING": ["Travel", "Venue", "Food", "Activities", "Other"],
        // Shared Purchase
        "GIFT_POOL": ["Contribution", "Gift Purchase", "Delivery", "Other"],
        "GROUP_PURCHASE": ["Item", "Shipping", "Tax", "Other"],
        "SHARED_ASSET": ["Purchase", "Maintenance", "Repair", "Insurance", "Other"],
        "COMMUNITY_PURCHASE": ["Custom Expense", "Custom Contribution"],
        // Shared Living
        "FLATMATES": ["Rent", "Groceries", "Utilities", "Internet", "House Supplies", "Repairs", "Other"],
        "FAMILY_HOUSEHOLD": ["Utilities", "Rent", "Groceries", "Internet", "Maintenance", "Other"],
        "CO_LIVING": ["Rent", "Groceries", "Utilities", "Maintenance", "Other"],
        "COMMUNITY_LIVING": ["Custom Expense", "Custom Contribution"],
        // DB aliases sometimes surfaced in older ledgers
        "SHARED_LIVING": ["Rent", "Groceries", "Utilities", "Maintenance", "Other"],
    ]

    private static let fallback = ["Other"]

    static func categories(for momentTypeCode: String?) -> [String] {
        let code = (momentTypeCode ?? "").trimmingCharacters(in: .whitespacesAndNewlines).uppercased()
        guard !code.isEmpty else { return fallback }
        return byType[code] ?? fallback
    }

    static func defaultCategory(for momentTypeCode: String?) -> String {
        categories(for: momentTypeCode).first ?? "Other"
    }

    /// Embed category in description for API persistence (no category_code on group expenses yet).
    static func descriptionWithCategory(category: String, userDescription: String) -> String {
        let note = userDescription.trimmingCharacters(in: .whitespacesAndNewlines)
        let cat = category.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
            ? "Other"
            : category.trimmingCharacters(in: .whitespacesAndNewlines)
        if note.isEmpty { return cat }
        if note.range(of: cat, options: .caseInsensitive) != nil { return note }
        return "\(note) | \(cat)"
    }

    /// Best-effort reverse of `descriptionWithCategory` for edit forms.
    static func parseCategoryAndNote(from description: String?, momentTypeCode: String?) -> (category: String, note: String) {
        let cats = categories(for: momentTypeCode)
        let raw = (description ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
        if raw.isEmpty { return (defaultCategory(for: momentTypeCode), "") }
        if let sep = raw.range(of: " | ", options: .backwards) {
            let cat = String(raw[sep.upperBound...]).trimmingCharacters(in: .whitespacesAndNewlines)
            if let match = cats.first(where: { $0.caseInsensitiveCompare(cat) == .orderedSame }) {
                let note = String(raw[..<sep.lowerBound]).trimmingCharacters(in: .whitespacesAndNewlines)
                return (match, note)
            }
        }
        if let match = cats.first(where: { $0.caseInsensitiveCompare(raw) == .orderedSame }) {
            return (match, "")
        }
        return (defaultCategory(for: momentTypeCode), raw)
    }
}

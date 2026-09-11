import Foundation

/// Shared Experience Checklist categories stored on collaboration.planning_item.category_code.
/// Codes and labels are exact product strings — do not derive via `GroupPlanningCategoryCatalog.code(forLabel:)`.
enum GroupExperienceChecklistCatalog {
    struct Category: Identifiable, Hashable {
        let code: String
        let label: String
        var id: String { code }
    }

    static let categories: [Category] = [
        Category(code: "DOCUMENTS_MONEY", label: "Documents & Money"),
        Category(code: "TRAVEL_ESSENTIALS", label: "Travel Essentials"),
        Category(code: "MEDICINES_HEALTH", label: "Medicines & Health Kit"),
        Category(code: "CLOTHING", label: "Clothing"),
    ]

    static let codes: Set<String> = Set(categories.map(\.code))

    static func isChecklistCode(_ code: String?) -> Bool {
        let normalized = (code ?? "").trimmingCharacters(in: .whitespacesAndNewlines).uppercased()
        return !normalized.isEmpty && codes.contains(normalized)
    }

    static func label(forCode code: String?) -> String {
        let normalized = (code ?? "").trimmingCharacters(in: .whitespacesAndNewlines).uppercased()
        if let match = categories.first(where: { $0.code == normalized }) {
            return match.label
        }
        return normalized.replacingOccurrences(of: "_", with: " ").capitalized
    }

    static func defaultCode() -> String { categories[0].code }
    static func defaultLabel() -> String { categories[0].label }

    /// Seed packing list from pilgrimage reference: (categoryCode, title).
    static let seedPackingList: [(code: String, title: String)] = [
        ("DOCUMENTS_MONEY", "Aadhaar / ID Proof"),
        ("DOCUMENTS_MONEY", "Train / Flight Tickets (Print + Mobile)"),
        ("DOCUMENTS_MONEY", "Hotel Booking Details"),
        ("DOCUMENTS_MONEY", "Cash (small denominations)"),
        ("DOCUMENTS_MONEY", "ATM / Debit Card"),
        ("DOCUMENTS_MONEY", "Emergency Contact Numbers"),
        ("TRAVEL_ESSENTIALS", "Mobile Phone"),
        ("TRAVEL_ESSENTIALS", "Charger"),
        ("TRAVEL_ESSENTIALS", "Power Bank"),
        ("TRAVEL_ESSENTIALS", "Water Bottle"),
        ("TRAVEL_ESSENTIALS", "Small Backpack (Day Use)"),
        ("TRAVEL_ESSENTIALS", "Snacks (Biscuits, Dry Fruits)"),
        ("TRAVEL_ESSENTIALS", "Travel Pillow / Shawl"),
        ("TRAVEL_ESSENTIALS", "Umbrella / Raincoat"),
        ("MEDICINES_HEALTH", "Paracetamol (Fever)"),
        ("MEDICINES_HEALTH", "Cold Tablets"),
        ("MEDICINES_HEALTH", "Pain Relief Tablets / Spray"),
        ("MEDICINES_HEALTH", "Acidity Tablets"),
        ("MEDICINES_HEALTH", "ORS Packets"),
        ("MEDICINES_HEALTH", "Loose Motion Tablets"),
        ("MEDICINES_HEALTH", "Vomiting Medicine"),
        ("MEDICINES_HEALTH", "Personal Medicines"),
        ("MEDICINES_HEALTH", "Doctor Prescription"),
        ("MEDICINES_HEALTH", "Band-aid / Antiseptic Cream"),
        ("MEDICINES_HEALTH", "Hand Sanitizer"),
        ("MEDICINES_HEALTH", "Masks"),
        ("CLOTHING", "Shirts / T-shirts (3–4)"),
        ("CLOTHING", "Pants / Track Pants (2–3)"),
        ("CLOTHING", "Traditional Wear"),
        ("CLOTHING", "Undergarments"),
        ("CLOTHING", "Nightwear"),
        ("CLOTHING", "Walking Shoes"),
        ("CLOTHING", "Slippers / Sandals"),
        ("CLOTHING", "Toothbrush & Paste"),
    ]

    static func checklistItems(_ items: [GroupPlanningItem]) -> [GroupPlanningItem] {
        items.filter { isChecklistCode($0.categoryCode) }
    }

    static func nonChecklistItems(_ items: [GroupPlanningItem]) -> [GroupPlanningItem] {
        items.filter { !isChecklistCode($0.categoryCode) }
    }

    /// Group checklist items by category order; skip empty categories.
    static func groupedByCategory(
        _ items: [GroupPlanningItem]
    ) -> [(category: Category, items: [GroupPlanningItem])] {
        let checklist = checklistItems(items)
        return categories.compactMap { cat in
            let group = checklist.filter {
                ($0.categoryCode ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
                    .caseInsensitiveCompare(cat.code) == .orderedSame
            }
            return group.isEmpty ? nil : (cat, group)
        }
    }
}

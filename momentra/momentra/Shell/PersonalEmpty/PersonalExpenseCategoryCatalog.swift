import Foundation

enum PersonalExpenseCategoryCatalog {
    struct Category: Identifiable {
        let id: String
        let code: String
        let label: String
        let emoji: String
        let subcategories: [Subcategory]
    }

    struct Subcategory: Identifiable {
        let id: String
        let code: String
        let label: String
    }

    static let masterCategories: [Category] = [
        Category(id: "FOOD", code: "FOOD", label: "Food", emoji: "🍕", subcategories: [
            Subcategory(id: "FOOD_DINING", code: "FOOD_DINING", label: "Food & Dining"),
            Subcategory(id: "CAFE", code: "CAFE", label: "Cafe"),
            Subcategory(id: "GROCERIES", code: "GROCERIES", label: "Groceries"),
            Subcategory(id: "RESTAURANT", code: "RESTAURANT", label: "Restaurant"),
        ]),
        Category(id: "TRANSPORT", code: "TRANSPORT", label: "Transport", emoji: "🚗", subcategories: [
            Subcategory(id: "TRANSPORT", code: "TRANSPORT", label: "Transport"),
            Subcategory(id: "FUEL", code: "FUEL", label: "Fuel"),
            Subcategory(id: "RIDE", code: "RIDE", label: "Ride share"),
        ]),
        Category(id: "SHOPPING", code: "SHOPPING", label: "Shopping", emoji: "🛍️", subcategories: [
            Subcategory(id: "SHOPPING", code: "SHOPPING", label: "Shopping"),
            Subcategory(id: "CLOTHING", code: "CLOTHING", label: "Clothing"),
            Subcategory(id: "ELECTRONICS", code: "ELECTRONICS", label: "Electronics"),
        ]),
        Category(id: "CAFE", code: "CAFE", label: "Cafe", emoji: "☕", subcategories: [
            Subcategory(id: "CAFE", code: "CAFE", label: "Cafe"),
            Subcategory(id: "COFFEE", code: "COFFEE", label: "Coffee"),
        ]),
        Category(id: "HEALTH", code: "HEALTH", label: "Health", emoji: "💊", subcategories: [
            Subcategory(id: "HEALTH", code: "HEALTH", label: "Health"),
            Subcategory(id: "PHARMACY", code: "PHARMACY", label: "Pharmacy"),
            Subcategory(id: "FITNESS", code: "FITNESS", label: "Fitness"),
        ]),
        Category(id: "ENTERTAINMENT", code: "ENTERTAINMENT", label: "Entertainment", emoji: "🎬", subcategories: [
            Subcategory(id: "ENTERTAINMENT", code: "ENTERTAINMENT", label: "Entertainment"),
            Subcategory(id: "STREAMING", code: "STREAMING", label: "Streaming"),
            Subcategory(id: "EVENTS", code: "EVENTS", label: "Events"),
        ]),
        Category(id: "BILLS", code: "BILLS", label: "Bills", emoji: "🧾", subcategories: [
            Subcategory(id: "BILLS", code: "BILLS", label: "Bills"),
            Subcategory(id: "UTILITIES", code: "UTILITIES", label: "Utilities"),
            Subcategory(id: "RENT", code: "RENT", label: "Rent"),
            Subcategory(id: "HOUSING", code: "HOUSING", label: "Housing"),
        ]),
        Category(id: "OTHER", code: "OTHER", label: "Other", emoji: "📦", subcategories: [
            Subcategory(id: "OTHER", code: "OTHER", label: "Other"),
            Subcategory(id: "MISC", code: "MISC", label: "Miscellaneous"),
        ]),
    ]

    static func labelForCode(_ code: String?) -> String {
        guard let code, !code.isEmpty else { return "Other" }
        let upper = code.uppercased()
        for cat in masterCategories {
            if let sub = cat.subcategories.first(where: { $0.code == upper }) { return sub.label }
            if cat.code == upper { return cat.label }
        }
        return code.replacingOccurrences(of: "_", with: " ").capitalized
    }

    static func encodeCategoryCode(categoryCode: String, subcategoryCode: String?) -> String {
        guard let sub = subcategoryCode, !sub.isEmpty else { return categoryCode.uppercased() }
        return sub.uppercased()
    }

    static func decodeFromStored(_ stored: String?) -> (String, String?) {
        guard let stored, !stored.isEmpty else { return ("OTHER", nil) }
        let upper = stored.uppercased()
        for cat in masterCategories {
            if let sub = cat.subcategories.first(where: { $0.code == upper }) {
                return (cat.code, sub.code)
            }
        }
        return (upper, nil)
    }
}

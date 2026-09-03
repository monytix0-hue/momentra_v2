import SwiftUI

/// Figma 453:9376 Master Expense premium tokens — fixed purple accent.
enum PersonalMasterExpenseTheme {
    static let bg = Color(hex: "#191622")
    static let surface = Color(hex: "#181424").opacity(0.7)
    static let surfaceSolid = Color(hex: "#181424")
    static let border = Color(hex: "#342C44")
    static let accent = Color(hex: "#7C5CFC")
    static let accentLight = Color(hex: "#C9BFFF")
    static let textMain = Color(hex: "#E5E0EE")
    static let muted = Color(hex: "#9E9AA7")
    static let categoryUnselected = Color(hex: "#14131A")
    static let categoryBorder = Color.white.opacity(0.1)
    static let error = Color(hex: "#F87171")

    struct EmotionalOption: Identifiable {
        let id: String
        let emoji: String
        let label: String
    }

    struct RelationshipImpactOption: Identifiable {
        let id: String
        let emoji: String
        let label: String
    }

    static let emotionalOptions: [EmotionalOption] = [
        .init(id: "Relieved", emoji: "😌", label: "Relieved"),
        .init(id: "Happy", emoji: "😊", label: "Happy"),
        .init(id: "Connected", emoji: "🤝", label: "Connected"),
        .init(id: "Proud", emoji: "😎", label: "Proud"),
        .init(id: "Neutral", emoji: "😐", label: "Neutral"),
        .init(id: "Unsure", emoji: "🤔", label: "Unsure"),
        .init(id: "Guilty", emoji: "😔", label: "Guilty"),
        .init(id: "Blessed", emoji: "🙏", label: "Blessed"),
        .init(id: "Disappointed", emoji: "😞", label: "Disappointed"),
    ]

    static let sharedWithOptions = ["Spouse", "Parents", "Family", "Friends", "Custom"]

    static let relationshipImpactOptions: [RelationshipImpactOption] = [
        .init(id: "Strengthened Connection", emoji: "💪", label: "Strengthened Connection"),
        .init(id: "Celebration Together", emoji: "🎉", label: "Celebration Together"),
        .init(id: "Support Given", emoji: "🤲", label: "Support Given"),
    ]

    static let reasoningOptions = ["Celebration", "Daily Need", "Gift", "Travel", "Other"]
    static let segmentOptions = ["Low", "Medium", "High"]
    static let whenOptions = ["Now", "Today", "Yesterday"]
}

import Foundation
import SwiftUI

struct RelationshipsActivityItem: Identifiable, Equatable {
    var id: String
    var title: String
    var whenLabel: String
    var impact: String
    var emoji: String
    var relationship: String
    var notes: String
    var tags: [String]
    var filter: String
}

enum RelationshipsActivityModels {
    /// Empty API / failure → empty list. Never invent demo rows (S2 G2).
    static func from(api items: [APIClient.ActivityItemPayload]) -> [RelationshipsActivityItem] {
        items.map { dto in
            let meta = visual(for: dto.activityCode, title: dto.title)
            return RelationshipsActivityItem(
                id: dto.occurredAt + dto.title + (dto.activityPayload?.activityId ?? ""),
                title: dto.title.isEmpty ? meta.title : dto.title,
                whenLabel: formatOccurred(dto.occurredAt),
                impact: "",
                emoji: meta.emoji,
                relationship: meta.filter,
                notes: "",
                tags: [],
                filter: meta.filter
            )
        }
    }

    private static func visual(for code: String, title: String) -> (title: String, emoji: String, filter: String) {
        let c = code.uppercased()
        if c.contains("CONNECT") || title.localizedCaseInsensitiveContains("connection") {
            return ("Connection", "💬", "Partner")
        }
        if c.contains("SUPPORT") || title.localizedCaseInsensitiveContains("support") {
            return ("Support", "🫶", "Partner")
        }
        if c.contains("SHARED") {
            return ("Shared experience", "🤝", "Partner")
        }
        if c.contains("INVEST") {
            return ("Investment", "📈", "Partner")
        }
        if c.contains("FAMILY") || title.localizedCaseInsensitiveContains("family") {
            return ("Family", "📞", "Family")
        }
        if c.contains("FRIEND") {
            return ("Friends", "🤝", "Friends")
        }
        if c.contains("SELF") || c.contains("REFLECT") || c.contains("INTERACTION") {
            return ("Check-in", "🪞", "Self")
        }
        return ("Activity", "❤️", "Partner")
    }

    private static func formatOccurred(_ iso: String) -> String {
        let f = ISO8601DateFormatter()
        f.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        let date = f.date(from: iso) ?? ISO8601DateFormatter().date(from: iso)
        guard let date else { return iso }
        let out = DateFormatter()
        out.dateFormat = "MMM d, h:mm a"
        return out.string(from: date)
    }
}

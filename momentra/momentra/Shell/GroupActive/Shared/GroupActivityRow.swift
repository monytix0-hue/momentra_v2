import SwiftUI

/// Shared Pulse / All-activity row matching Figma 584:15872:
/// left accent bar + 36pt rounded icon + actor-prefixed title + relative time.
struct GroupActivityRow: View {
    let item: APIClient.ActivityItemPayload
    var accent: Color
    var textColor: Color = Color(hex: "#E5E0EE")
    var secondaryColor: Color = Color(hex: "#C9C4D8")
    var showChevron: Bool = false
    var compactPadding: Bool = true
    var action: (() -> Void)? = nil

    private var canTap: Bool { action != nil }

    var body: some View {
        Button {
            action?()
        } label: {
            HStack(spacing: 12) {
                RoundedRectangle(cornerRadius: 1)
                    .fill(accent.opacity(0.4))
                    .frame(width: 2, height: 36)

                Text(GroupActivityPresentation.glyph(for: item.activityCode))
                    .font(.system(size: 16))
                    .frame(width: 36, height: 36)
                    .background(Color.white.opacity(0.1))
                    .clipShape(RoundedRectangle(cornerRadius: 10))

                VStack(alignment: .leading, spacing: 2) {
                    Text(GroupActivityPresentation.displayTitle(for: item))
                        .font(.plusJakarta(size: 14, weight: .bold))
                        .foregroundStyle(textColor)
                        .multilineTextAlignment(.leading)
                        .frame(maxWidth: .infinity, alignment: .leading)
                    Text(GroupActivityPresentation.formatOccurredAt(item.occurredAt))
                        .font(.plusJakarta(size: 11, weight: .regular))
                        .foregroundStyle(secondaryColor)
                }

                if showChevron {
                    Image(systemName: "chevron.right")
                        .font(.system(size: 11, weight: .semibold))
                        .foregroundStyle(secondaryColor)
                }
            }
            .padding(.vertical, compactPadding ? 0 : 12)
            .padding(.horizontal, compactPadding ? 0 : 16)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .disabled(!canTap)
    }
}

enum GroupActivityPresentation {
    static func displayTitle(for item: APIClient.ActivityItemPayload) -> String {
        displayTitle(
            title: item.title,
            activityCode: item.activityCode,
            actorDisplayName: item.actorDisplayName
        )
    }

    static func displayTitle(title: String, activityCode: String, actorDisplayName: String?) -> String {
        let trimmedTitle = title.trimmingCharacters(in: .whitespacesAndNewlines)
        guard let actor = actorDisplayName?.trimmingCharacters(in: .whitespacesAndNewlines), !actor.isEmpty else {
            return trimmedTitle
        }
        let firstName = actor.split(whereSeparator: { $0.isWhitespace }).first.map(String.init) ?? actor
        if trimmedTitle.localizedCaseInsensitiveContains(firstName)
            || trimmedTitle.lowercased().hasPrefix(actor.lowercased()) {
            return trimmedTitle
        }
        return "\(firstName) \(verb(for: activityCode)) \(trimmedTitle)"
    }

    static func verb(for activityCode: String) -> String {
        let upper = activityCode.uppercased()
        if upper.contains("UPDATE") { return "shared" }
        if upper.contains("BOOKING") || upper.contains("ATTENDANCE") { return "confirmed" }
        if upper.contains("SETTLE") { return "recorded" }
        if upper.contains("POLL") || upper.contains("RULE") { return "created" }
        if upper.contains("OWNERSHIP") || upper.contains("TRANSFER") { return "updated" }
        if upper.contains("PLANNING")
            || upper.contains("EXPENSE")
            || upper.contains("PURCHASE")
            || upper.contains("MEMORY")
            || upper.contains("RESIDENT")
            || upper.contains("VENDOR")
            || upper.contains("ASSET")
            || upper.contains("MAINTENANCE")
            || upper.contains("MEMBER")
            || upper.contains("CONTRIB") {
            return "added"
        }
        return "updated"
    }

    static func glyph(for activityCode: String) -> String {
        let upper = activityCode.uppercased()
        if upper.contains("EXPENSE") || upper.contains("CONTRIB") { return "💸" }
        if upper.contains("SETTLE") || upper.contains("ATTENDANCE") { return "✅" }
        if upper.contains("BOOKING") { return "🎧" }
        if upper.contains("PURCHASE") || upper.contains("DELIVERY") { return "🍔" }
        if upper.contains("UPDATE") || upper.contains("MEMORY") || upper.contains("POLL") { return "🎵" }
        if upper.contains("MEMBER") || upper.contains("RESIDENT") { return "👋" }
        if upper.contains("PLANNING") || upper.contains("VENDOR") || upper.contains("RULE")
            || upper.contains("ASSET") || upper.contains("MAINTENANCE") {
            return "📌"
        }
        return "📌"
    }

    static func formatOccurredAt(_ raw: String) -> String {
        let iso = ISO8601DateFormatter()
        iso.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        let fallback = ISO8601DateFormatter()
        fallback.formatOptions = [.withInternetDateTime]
        guard let date = iso.date(from: raw) ?? fallback.date(from: raw) else { return raw }

        let calendar = Calendar.current
        let timeFormatter = DateFormatter()
        timeFormatter.locale = .current
        timeFormatter.dateFormat = "h:mm a"

        if calendar.isDateInToday(date) {
            return "Today, \(timeFormatter.string(from: date))"
        }
        if calendar.isDateInYesterday(date) {
            return "Yesterday, \(timeFormatter.string(from: date))"
        }

        let dayFormatter = DateFormatter()
        dayFormatter.locale = .current
        dayFormatter.dateFormat = "d MMM"
        return "\(dayFormatter.string(from: date)), \(timeFormatter.string(from: date))"
    }

    /// Accent cycle for Experience family rows (Figma 584:15872 blues).
    static func accentCycle(at index: Int) -> Color {
        let palette: [Color] = [
            Color(hex: "#3B82F6"),
            Color(hex: "#60A5FA"),
            Color(hex: "#93C5FD"),
        ]
        return palette[index % palette.count]
    }
}

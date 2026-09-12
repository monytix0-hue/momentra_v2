import SwiftUI

/// Nested activity parent with optional UPDATED / VOIDED children.
struct GroupActivityTreeNode: Identifiable {
    let item: APIClient.ActivityItemPayload
    let children: [APIClient.ActivityItemPayload]

    var id: String { item.id }
}

/// Shared Pulse / All-activity row matching Figma 584:15872:
/// left accent bar + 36pt rounded icon + actor-prefixed title + relative time + amount.
struct GroupActivityRow: View {
    let item: APIClient.ActivityItemPayload
    var accent: Color
    var textColor: Color = Color(hex: "#E5E0EE")
    var secondaryColor: Color = Color(hex: "#C9C4D8")
    var showChevron: Bool = false
    var compactPadding: Bool = true
    var isChild: Bool = false
    var action: (() -> Void)? = nil

    private var canTap: Bool { action != nil }

    private var amountLabel: String? {
        GroupActivityPresentation.amountLabel(for: item)
    }

    private var title: String {
        GroupActivityPresentation.rowTitle(for: item, isChild: isChild)
    }

    var body: some View {
        Button {
            action?()
        } label: {
            HStack(spacing: 12) {
                RoundedRectangle(cornerRadius: 1)
                    .fill(accent.opacity(isChild ? 0.25 : 0.4))
                    .frame(width: isChild ? 1.5 : 2, height: isChild ? 28 : 36)

                Text(
                    isChild && GroupActivityPresentation.isVoided(item.activityCode)
                        ? "🗑️"
                        : GroupActivityPresentation.glyph(for: item.activityCode)
                )
                .font(.system(size: isChild ? 13 : 16))
                .frame(width: isChild ? 28 : 36, height: isChild ? 28 : 36)
                .background(Color.white.opacity(0.1))
                .clipShape(RoundedRectangle(cornerRadius: 10))

                VStack(alignment: .leading, spacing: 2) {
                    Text(title)
                        .font(.plusJakarta(size: isChild ? 12 : 14, weight: isChild ? .semibold : .bold))
                        .foregroundStyle(isChild ? secondaryColor : textColor)
                        .multilineTextAlignment(.leading)
                        .frame(maxWidth: .infinity, alignment: .leading)
                    Text(GroupActivityPresentation.formatOccurredAt(item.occurredAt))
                        .font(.plusJakarta(size: 11, weight: .regular))
                        .foregroundStyle(secondaryColor)
                }

                if let amountLabel {
                    Text(amountLabel)
                        .font(.plusJakarta(size: isChild ? 12 : 13, weight: .semibold))
                        .foregroundStyle(secondaryColor)
                }

                if showChevron {
                    Image(systemName: "chevron.right")
                        .font(.system(size: 11, weight: .semibold))
                        .foregroundStyle(secondaryColor)
                }
            }
            .padding(.vertical, compactPadding ? 0 : 12)
            .padding(.trailing, compactPadding ? 0 : 16)
            .padding(.leading, isChild ? (compactPadding ? 28 : 44) : (compactPadding ? 0 : 16))
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

    static func rowTitle(for item: APIClient.ActivityItemPayload, isChild: Bool) -> String {
        if isChild && isVoided(item.activityCode) {
            return deletedTitle(for: item)
        }
        return displayTitle(for: item)
    }

    static func deletedTitle(for item: APIClient.ActivityItemPayload) -> String {
        guard let actor = item.actorDisplayName?.trimmingCharacters(in: .whitespacesAndNewlines), !actor.isEmpty else {
            return "Deleted activity"
        }
        let firstName = actor.split(whereSeparator: { $0.isWhitespace }).first.map(String.init) ?? actor
        return "\(firstName) deleted this activity"
    }

    static func amountLabel(for item: APIClient.ActivityItemPayload) -> String? {
        guard let raw = item.activityPayload?.amount,
              let value = Double(raw) else { return nil }
        let currency = item.activityPayload?.currencyCode ?? "INR"
        let symbol = currency.uppercased() == "INR" ? "₹" : "\(currency) "
        let rounded = Int(value.rounded())
        if abs(value - Double(rounded)) < 0.001 {
            return "\(symbol)\(rounded)"
        }
        return String(format: "%@%.2f", symbol, value)
    }

    static func groupKey(for item: APIClient.ActivityItemPayload) -> String? {
        if let expenseId = item.activityPayload?.expenseId?.trimmingCharacters(in: .whitespacesAndNewlines),
           !expenseId.isEmpty {
            return "expense:\(expenseId)"
        }
        if let contributionId = item.activityPayload?.contributionId?.trimmingCharacters(in: .whitespacesAndNewlines),
           !contributionId.isEmpty {
            return "contrib:\(contributionId)"
        }
        return nil
    }

    static func isVoided(_ activityCode: String) -> Bool {
        activityCode.uppercased().contains("VOIDED")
    }

    static func isUpdated(_ activityCode: String) -> Bool {
        activityCode.uppercased().contains("_UPDATED")
    }

    static func nodeHasVoidChild(_ node: GroupActivityTreeNode) -> Bool {
        isVoided(node.item.activityCode) || node.children.contains { isVoided($0.activityCode) }
    }

    /// Nest UPDATED / VOIDED lifecycle rows under the original expense or contribution.
    static func activityTree(from items: [APIClient.ActivityItemPayload]) -> [GroupActivityTreeNode] {
        guard !items.isEmpty else { return [] }

        var keyed: [String: [APIClient.ActivityItemPayload]] = [:]
        var keyedOrder: [String] = []
        var unkeyed: [APIClient.ActivityItemPayload] = []

        for item in items {
            guard let key = groupKey(for: item) else {
                unkeyed.append(item)
                continue
            }
            if keyed[key] == nil {
                keyedOrder.append(key)
                keyed[key] = []
            }
            keyed[key, default: []].append(item)
        }

        var nodes: [GroupActivityTreeNode] = []
        for key in keyedOrder {
            guard let group = keyed[key] else { continue }
            nodes.append(buildNode(from: group))
        }
        for item in unkeyed {
            nodes.append(GroupActivityTreeNode(item: item, children: []))
        }

        return nodes.sorted {
            occurredAtMillis($0.item.occurredAt) > occurredAtMillis($1.item.occurredAt)
        }
    }

    private static func buildNode(from group: [APIClient.ActivityItemPayload]) -> GroupActivityTreeNode {
        let sorted = group.sorted { occurredAtMillis($0.occurredAt) < occurredAtMillis($1.occurredAt) }
        let parent = sorted.first(where: {
            $0.activityCode.uppercased().contains("RECORDED") && !isVoided($0.activityCode)
        }) ?? sorted.first(where: { !isVoided($0.activityCode) })
            ?? sorted[0]

        let children = sorted.filter { item in
            guard item.id != parent.id else { return false }
            return isVoided(item.activityCode) || isUpdated(item.activityCode)
        }
        return GroupActivityTreeNode(item: parent, children: children)
    }

    private static func occurredAtMillis(_ raw: String) -> TimeInterval {
        let iso = ISO8601DateFormatter()
        iso.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        let fallback = ISO8601DateFormatter()
        fallback.formatOptions = [.withInternetDateTime]
        return (iso.date(from: raw) ?? fallback.date(from: raw))?.timeIntervalSince1970 ?? 0
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

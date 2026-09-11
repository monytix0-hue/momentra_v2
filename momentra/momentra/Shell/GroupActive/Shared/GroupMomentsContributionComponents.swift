import SwiftUI

typealias GroupContributionItem = APIClient.GroupContributionItem

enum ContributionPaymentMethodLabel {
    static func display(_ code: String?) -> String {
        switch (code ?? "").uppercased() {
        case "UPI": return "UPI"
        case "BANK_TRANSFER": return "Bank Transfer"
        case "CASH": return "Cash"
        case "CARD": return "Card"
        default: return code?.isEmpty == false ? code! : "—"
        }
    }
}

func contributionParseDate(_ iso: String?) -> Date? {
    guard let iso, !iso.isEmpty else { return nil }
    let withFraction = ISO8601DateFormatter()
    withFraction.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
    if let d = withFraction.date(from: iso) { return d }
    let plain = ISO8601DateFormatter()
    plain.formatOptions = [.withInternetDateTime]
    return plain.date(from: iso)
}

func contributionRelativeTime(_ iso: String?) -> String {
    guard let date = contributionParseDate(iso) else { return "" }
    let seconds = Int(-date.timeIntervalSinceNow)
    if seconds < 60 { return "just now" }
    if seconds < 3600 { return "\(seconds / 60)m ago" }
    if seconds < 86_400 { return "\(seconds / 3600)h ago" }
    if seconds < 172_800 { return "Yesterday" }
    let fmt = DateFormatter()
    fmt.dateFormat = "d MMM"
    return fmt.string(from: date)
}

func contributionDayKey(_ iso: String?) -> Date? {
    guard let date = contributionParseDate(iso) else { return nil }
    return Calendar.current.startOfDay(for: date)
}

func contributionDayHeader(_ day: Date, today: Date = Calendar.current.startOfDay(for: Date())) -> String {
    let cal = Calendar.current
    if cal.isDate(day, inSameDayAs: today) { return "Today" }
    if let y = cal.date(byAdding: .day, value: -1, to: today), cal.isDate(day, inSameDayAs: y) {
        return "Yesterday"
    }
    let fmt = DateFormatter()
    fmt.dateFormat = "EEEE, d MMM"
    return fmt.string(from: day)
}

private let contributionAvatarColors: [Color] = [
    Color(hex: "#FDBA74"), Color(hex: "#86EFAC"), Color(hex: "#F9A8D4"), Color(hex: "#93C5FD"),
]

struct MomentsContributionCard: View {
    let item: GroupContributionItem
    var chrome: MomentsChrome
    var avatarIndex: Int = 0
    var onTap: (() -> Void)? = nil

    private var name: String {
        let n = item.displayName?.trimmingCharacters(in: .whitespacesAndNewlines)
        return (n?.isEmpty == false) ? n! : "Member"
    }

    private var initials: String {
        let parts = name.split(separator: " ")
        if parts.count >= 2 {
            return "\(parts[0].prefix(1))\(parts[1].prefix(1))".uppercased()
        }
        return String(name.prefix(2)).uppercased()
    }

    private var meta: String {
        let time = contributionRelativeTime(item.contributedAt)
        let pool = item.label?.trimmingCharacters(in: .whitespacesAndNewlines)
        let poolPart = (pool?.isEmpty == false) ? pool! : "Pool"
        if time.isEmpty { return poolPart }
        return "\(time) · \(poolPart)"
    }

    private var amountText: String {
        GroupFinanceFormat.formatMoney(item.amount, currencyCode: item.currencyCode ?? "INR")
    }

    private var statusLabel: String {
        (item.status ?? "PAID").uppercased() == "PENDING" ? "PENDING" : "PAID"
    }

    var body: some View {
        Button {
            onTap?()
        } label: {
            VStack(alignment: .leading, spacing: 12) {
                HStack(spacing: 12) {
                    Text(initials)
                        .font(.plusJakarta(size: 14, weight: .bold))
                        .foregroundStyle(Color(hex: "#14121B"))
                        .frame(width: 40, height: 40)
                        .background(contributionAvatarColors[avatarIndex % contributionAvatarColors.count])
                        .clipShape(Circle())
                    VStack(alignment: .leading, spacing: 2) {
                        Text(name)
                            .font(.plusJakarta(size: 14, weight: .bold))
                            .foregroundStyle(chrome.text)
                        Text(meta)
                            .font(.plusJakarta(size: 11))
                            .foregroundStyle(chrome.secondary)
                    }
                    Spacer(minLength: 8)
                    if item.hasAttachment {
                        Image(systemName: "paperclip")
                            .font(.system(size: 12, weight: .semibold))
                            .foregroundStyle(chrome.accent)
                    }
                    Text(statusLabel)
                        .font(.plusJakarta(size: 10, weight: .bold))
                        .foregroundStyle(chrome.text)
                        .padding(.horizontal, 6)
                        .padding(.vertical, 2)
                        .background(chrome.accent.opacity(0.1))
                        .clipShape(RoundedRectangle(cornerRadius: 4))
                }
                HStack {
                    Text(amountText)
                        .font(.plusJakarta(size: 13, weight: .semibold))
                        .foregroundStyle(chrome.text)
                    Spacer()
                    Text(ContributionPaymentMethodLabel.display(item.paymentMethodCode))
                        .font(.plusJakarta(size: 11))
                        .foregroundStyle(chrome.secondary)
                }
            }
            .padding(14)
            .background(chrome.card)
            .clipShape(RoundedRectangle(cornerRadius: 14))
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .disabled(onTap == nil)
    }
}

struct MomentsContributionDetailsSection: View {
    let items: [GroupContributionItem]
    var chrome: MomentsChrome
    var momentId: String? = nil
    var onViewAll: (() -> Void)?
    var onEdit: ((GroupContributionItem) -> Void)? = nil

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            MomentsSectionHeader(title: "Contribution Details", chrome: chrome, onViewAll: onViewAll)
            if items.isEmpty {
                GroupEmptySection(
                    message: "No contributions yet",
                    detail: "Record a contribution from Quick Add — nothing is invented."
                )
            } else {
                ForEach(Array(items.prefix(3).enumerated()), id: \.element.id) { idx, item in
                    MomentsContributionCard(
                        item: item,
                        chrome: chrome,
                        avatarIndex: idx,
                        onTap: (momentId != nil && onEdit != nil) ? { onEdit?(item) } : nil
                    )
                }
            }
        }
    }
}

struct ContributionsListSheet: View {
    let items: [GroupContributionItem]
    var chrome: MomentsChrome
    var momentId: String? = nil
    var onDismiss: () -> Void
    var onEdit: ((GroupContributionItem) -> Void)? = nil

    private var grouped: [(day: Date, items: [GroupContributionItem])] {
        let today = Calendar.current.startOfDay(for: Date())
        var buckets: [Date: [GroupContributionItem]] = [:]
        var order: [Date] = []
        for item in items {
            let day = contributionDayKey(item.contributedAt) ?? today
            if buckets[day] == nil {
                order.append(day)
                buckets[day] = []
            }
            buckets[day]?.append(item)
        }
        return order.sorted(by: >).compactMap { day in
            guard let list = buckets[day], !list.isEmpty else { return nil }
            return (day, list)
        }
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                LazyVStack(alignment: .leading, spacing: 16) {
                    if grouped.isEmpty {
                        GroupEmptySection(
                            message: "No contributions yet",
                            detail: "Record a contribution from Quick Add — nothing is invented."
                        )
                        .padding(.top, 24)
                    } else {
                        ForEach(Array(grouped.enumerated()), id: \.offset) { _, group in
                            VStack(alignment: .leading, spacing: 10) {
                                Text(contributionDayHeader(group.day))
                                    .font(.plusJakarta(size: 13, weight: .bold))
                                    .foregroundStyle(chrome.secondary)
                                ForEach(Array(group.items.enumerated()), id: \.element.id) { idx, item in
                                    MomentsContributionCard(
                                        item: item,
                                        chrome: chrome,
                                        avatarIndex: idx,
                                        onTap: (momentId != nil && onEdit != nil) ? { onEdit?(item) } : nil
                                    )
                                }
                            }
                        }
                    }
                }
                .padding(20)
            }
            .background(chrome.bg)
            .navigationTitle("Contributions")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Close", action: onDismiss)
                        .foregroundStyle(chrome.accent)
                }
            }
        }
        .presentationDetents([.medium, .large])
    }
}

import SwiftUI

/// Figma 575:14470 — Group Memory active tab (live API only).
struct GroupMemoryActiveView: View {
    let refreshToken: UInt64
    let momentId: String?
    let momentTitle: String?
    var onOpenQuickAdd: () -> Void = {}

    @State private var memory: APIClient.GroupMemoryPayload?
    @State private var finance: APIClient.GroupFinancePayload?
    @State private var pulse: APIClient.GroupPulsePayload?
    @State private var loading = true
    @State private var error: String?

    private let accents: [Color] = [
        Color(hex: "#14B8A6"),
        Color(hex: "#B45309"),
        Color(hex: "#A855F7"),
        GroupActiveTheme.accentOrange,
    ]

    var body: some View {
        Group {
            if loading && memory == nil && pulse == nil {
                ProgressView().tint(GroupActiveTheme.brand)
            } else {
                NativeDashboardScaffold(background: GroupActiveTheme.bg) {

                    NativeListSection {

                    let items = memory?.payload?.items ?? []
                    let peopleCount = pulse?.payload?.participantCount ?? 0
                    let memoryCount = max(items.count, memory?.payload?.memoryCount ?? 0)
                    let total = finance?.totals?.first
                    let currency = total?.currencyCode ?? "INR"
                    let util = GroupFinanceFormat.utilizationPercent(
                        expenseTotal: total?.expenseTotal,
                        budgetTotal: total?.budgetTotal
                    )
                    VStack(alignment: .leading, spacing: 14) {
                        if let error {
                            Text(error).font(.caption).foregroundStyle(Color(hex: "#F87171"))
                        }
                        memoryHero(
                            title: momentTitle ?? "Group Memory",
                            peopleCount: peopleCount,
                            memoryCount: memoryCount
                        )
                        GroupSectionCard(title: "Memory Timeline") {
                            if items.isEmpty {
                                GroupEmptySection(message: "Timeline empty", detail: "Shared memories will appear here — nothing is invented.")
                            } else {
                                ForEach(Array(items.enumerated()), id: \.offset) { index, item in
                                    memoryRow(
                                        title: item.title ?? "Memory",
                                        meta: formatMemoryInstant(item.occurredAt),
                                        accent: accents[index % accents.count],
                                        glyph: memoryGlyph(index)
                                    )
                                }
                            }
                        }
                        GroupSectionCard(title: "Milestone Wall", badge: { GroupApiGapBadge() }) {
                            GroupEmptySection(message: "No milestones yet", detail: "Milestone capture is not live for groups.")
                        }
                        GroupSectionCard(title: "Gallery", badge: {
                            HStack(spacing: 6) {
                                GroupComingSoonBadge()
                                GroupApiGapBadge()
                            }
                        }) {
                            GroupEmptySection(message: "No photos yet", detail: "Shared gallery requires group media API.")
                        }
                        GroupSectionCard(title: "People Impact") {
                            Text(peopleCount > 0 ? "\(peopleCount) people shaped this shared story" : "No participants yet")
                                .font(.plusJakarta(size: 13, weight: .semibold))
                                .foregroundStyle(GroupActiveTheme.text)
                            GroupMetricTile(label: "Participants", value: "\(peopleCount)")
                        }
                        GroupSectionCard(title: "Budget Reflection") {
                            VStack(alignment: .leading, spacing: 10) {
                                HStack(spacing: 8) {
                                    VStack(alignment: .leading, spacing: 2) {
                                        Text("Planned")
                                            .font(.plusJakarta(size: 11))
                                            .foregroundStyle(.white.opacity(0.8))
                                        Text(GroupFinanceFormat.formatMoney(total?.budgetTotal, currencyCode: currency))
                                            .font(.plusJakarta(size: 16, weight: .bold))
                                            .foregroundStyle(.white)
                                    }
                                    .frame(maxWidth: .infinity, alignment: .leading)
                                    VStack(alignment: .leading, spacing: 2) {
                                        Text("Actual")
                                            .font(.plusJakarta(size: 11))
                                            .foregroundStyle(.white.opacity(0.8))
                                        Text(GroupFinanceFormat.formatMoney(total?.expenseTotal, currencyCode: currency))
                                            .font(.plusJakarta(size: 16, weight: .bold))
                                            .foregroundStyle(.white)
                                    }
                                    .frame(maxWidth: .infinity, alignment: .leading)
                                }
                                GroupProgressBar(percent: util)
                                Text(util > 0 ? "\(util)% of planned budget used" : "Budget utilization appears when planned and actual amounts exist")
                                    .font(.plusJakarta(size: 11))
                                    .foregroundStyle(.white.opacity(0.85))
                            }
                            .padding(14)
                            .background(LinearGradient(colors: [Color(hex: "#E8621A"), GroupActiveTheme.brand], startPoint: .leading, endPoint: .trailing))
                            .clipShape(RoundedRectangle(cornerRadius: 14))
                        }
                        GroupSectionCard(title: "Memory Intelligence", badge: { GroupComingSoonBadge() }) {
                            GroupEmptySection(message: "Insights coming soon", detail: "AI memory intelligence for groups is on the roadmap.")
                        }
                        VStack(alignment: .leading, spacing: 8) {
                            Text("Preserve this moment")
                                .font(.plusJakarta(size: 16, weight: .heavy))
                                .foregroundStyle(.white)
                            Text("Capture a photo, caption, or milestone for the group timeline.")
                                .font(.plusJakarta(size: 12))
                                .foregroundStyle(.white.opacity(0.85))
                            GroupCtaButton(label: "Preserve this moment", enabled: true, action: onOpenQuickAdd)
                        }
                        .padding(16)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(LinearGradient(colors: [GroupActiveTheme.brand, GroupActiveTheme.accentOrange], startPoint: .leading, endPoint: .trailing))
                        .clipShape(RoundedRectangle(cornerRadius: 18))
                    }
                

                    }

                }
            }
        }
        .background(GroupActiveTheme.bg)
        .task(id: "\(refreshToken)-\(momentId ?? "")") { await load() }
    }

    private func memoryHero(title: String, peopleCount: Int, memoryCount: Int) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 14) {
                Text("📷")
                    .font(.system(size: 24))
                    .frame(width: 56, height: 56)
                    .background(Color.white.opacity(0.18))
                    .clipShape(Circle())
                VStack(alignment: .leading, spacing: 4) {
                    Text(title)
                        .font(.plusJakarta(size: 20, weight: .heavy))
                        .foregroundStyle(.white)
                    Text("What you'll remember from this trip")
                        .font(.plusJakarta(size: 12))
                        .foregroundStyle(.white.opacity(0.85))
                }
            }
            HStack(spacing: 8) {
                Text("\(peopleCount) people")
                    .font(.plusJakarta(size: 11, weight: .semibold))
                    .foregroundStyle(.white)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 6)
                    .background(Color.white.opacity(0.16))
                    .clipShape(Capsule())
                Text(memoryCount > 0 ? "\(memoryCount) memories captured" : "No memories yet")
                    .font(.plusJakarta(size: 11, weight: .semibold))
                    .foregroundStyle(.white)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 6)
                    .background(Color.white.opacity(0.16))
                    .clipShape(Capsule())
            }
        }
        .padding(20)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            LinearGradient(
                colors: [Color(hex: "#E8621A"), GroupActiveTheme.brand, Color(hex: "#3D2A24")],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        )
        .clipShape(RoundedRectangle(cornerRadius: 24))
    }

    private func memoryRow(title: String, meta: String?, accent: Color, glyph: String) -> some View {
        HStack(spacing: 12) {
            Capsule().fill(accent).frame(width: 3, height: 40)
            Text(glyph)
                .frame(width: 36, height: 36)
                .background(accent.opacity(0.18))
                .clipShape(RoundedRectangle(cornerRadius: 12))
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.plusJakarta(size: 13, weight: .semibold))
                    .foregroundStyle(GroupActiveTheme.text)
                if let meta, !meta.isEmpty {
                    Text(meta)
                        .font(.plusJakarta(size: 11))
                        .foregroundStyle(GroupActiveTheme.secondary)
                }
            }
            Spacer(minLength: 0)
        }
        .padding(12)
        .background(Color(hex: "#181716"))
        .overlay(RoundedRectangle(cornerRadius: 14).stroke(accent.opacity(0.35)))
        .clipShape(RoundedRectangle(cornerRadius: 14))
    }

    private func memoryGlyph(_ index: Int) -> String {
        switch index % 4 {
        case 0: return "🌱"
        case 1: return "🗺️"
        case 2: return "💰"
        default: return "🌅"
        }
    }

    private func formatMemoryInstant(_ raw: String?) -> String? {
        guard let raw, !raw.isEmpty else { return nil }
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        if let date = formatter.date(from: raw) ?? ISO8601DateFormatter().date(from: raw) {
            let out = DateFormatter()
            out.locale = Locale(identifier: "en_US")
            out.dateFormat = "dd MMM yyyy"
            return out.string(from: date)
        }
        return String(raw.prefix(10))
    }

    private func load() async {
        guard let momentId else { loading = false; return }
        error = nil
        if let cachedPulse = GroupTabDataCache.peekPulse(momentId) {
            finance = cachedPulse.finance
            pulse = cachedPulse.pulse
            loading = false
        }
        if let cached = GroupTabDataCache.peekMemory(momentId) {
            memory = cached.memory
            finance = cached.finance ?? finance
            pulse = cached.pulse ?? pulse
            loading = false
        }
        if pulse == nil && finance == nil && memory == nil {
            loading = true
        }
        do {
            let cached = GroupTabDataCache.peekPulse(momentId)
            async let memoryResult = APIClient.shared.getGroupMemory(momentId: momentId)
            let loadedMemory = try await memoryResult
            var loadedFinance = cached?.finance
            var loadedPulse = cached?.pulse
            if loadedFinance == nil {
                loadedFinance = try await APIClient.shared.getGroupFinance(momentId: momentId).payload
            }
            if loadedPulse == nil {
                loadedPulse = try await APIClient.shared.getGroupPulse(momentId: momentId)
            }
            memory = loadedMemory
            finance = loadedFinance
            pulse = loadedPulse
            GroupTabDataCache.putMemory(momentId, .init(
                memory: loadedMemory,
                finance: loadedFinance,
                pulse: loadedPulse
            ))
        } catch {
            self.error = error.localizedDescription
        }
        loading = false
    }
}

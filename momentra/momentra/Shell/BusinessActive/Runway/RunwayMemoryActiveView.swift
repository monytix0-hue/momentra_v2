import SwiftUI

/// Figma `698:9970` Business Runway Memory — live memory facet; honest empties.
struct RunwayMemoryActiveView: View {
    let refreshToken: UInt64
    let momentId: String?
    let momentTitle: String?
    var onRecordLearning: () -> Void = {}
    var onOpenQuickAdd: () -> Void = {}

    @State private var memory: APIClient.BusinessMemoryPayload?
    @State private var scope = "All"
    @State private var loading = true
    @State private var error: String?

    private let theme = BusinessActiveTheme.businessRunway
    private let scopes = ["All", "Revenue", "Expenses", "Tax", "Investors"]

    private var items: [APIClient.BusinessMemoryPayload.MemoryInner.BusinessMemoryItem] {
        memory?.payload?.items ?? []
    }

    private var filtered: [APIClient.BusinessMemoryPayload.MemoryInner.BusinessMemoryItem] {
        guard scope != "All" else { return items }
        let q = scope.lowercased()
        return items.filter {
            let hay = ($0.title ?? "").lowercased()
            if q == "revenue" { return hay.contains("revenue") || hay.contains("invoice") || hay.contains("income") }
            if q == "expenses" { return hay.contains("expense") || hay.contains("spend") || hay.contains("burn") }
            if q == "tax" { return hay.contains("tax") || hay.contains("compliance") || hay.contains("filing") }
            if q == "investors" { return hay.contains("investor") || hay.contains("shareholder") || hay.contains("equity") }
            return true
        }
    }

    private var successItems: [APIClient.BusinessMemoryPayload.MemoryInner.BusinessMemoryItem] {
        filtered.filter { !isRisk($0) }
    }

    private var riskItems: [APIClient.BusinessMemoryPayload.MemoryInner.BusinessMemoryItem] {
        filtered.filter { isRisk($0) }
    }

    private var memoryCount: Int { memory?.payload?.memoryCount ?? items.count }
    private var biggestLearning: String? {
        filtered.first?.title?.trimmingCharacters(in: .whitespacesAndNewlines).nilIfEmpty
    }

    var body: some View {
        Group {
            if loading && memory == nil {
                ProgressView().tint(theme.accent)
            } else {
                ScrollView {
                    VStack(alignment: .leading, spacing: 16) {
                        if let error {
                            Text(error).font(.caption).foregroundStyle(RunwayColors.red)
                        }
                        RunwayFilterChipRow(chips: scopes, selected: scope, onSelect: { scope = $0 }, theme: theme)
                        RunwayMemoryHeroSection(
                            ringLabel: items.isEmpty ? "—" : "Live",
                            learnings: memoryCount > 0 ? "\(memoryCount)" : "—",
                            patterns: "—",
                            accuracy: "—",
                            showLive: !items.isEmpty,
                            theme: theme
                        )
                        RunwayEmptyAiCard(
                            title: "Biggest Learning",
                            emptyCopy: biggestLearning
                                ?? "Biggest learning appears when memory AI projects a signal — record learnings to seed it.",
                            theme: theme
                        )
                        RunwayDiamondDivider(theme: theme)
                        RunwayEmptyAiCard(
                            title: "Pattern Network",
                            emptyCopy: "Pattern network unavailable — memory.pattern API not mounted.",
                            theme: theme
                        )
                        RunwayDiamondDivider(theme: theme)
                        RunwayEmptyAiCard(
                            title: "Financial Playbook",
                            emptyCopy: "Playbook rules deferred until AI rule projection exists.",
                            theme: theme
                        )
                        RunwayDiamondDivider(theme: theme)
                        memoryList(title: "Success Memory", empty: "No success memories yet.", items: successItems, accent: RunwayColors.emerald)
                        memoryList(title: "Risk Memory", empty: "No risk memories yet.", items: riskItems, accent: RunwayColors.red)
                        RunwayEmptyAiCard(
                            title: "Financial Wisdom",
                            emptyCopy: "\"Capital efficiency beats growth velocity when runway is measured in quarters, not years.\" — runway intelligence",
                            theme: theme
                        )
                        RunwayEmptyAiCard(
                            title: "Knowledge Journey",
                            emptyCopy: filtered.isEmpty
                                ? "Journey milestones appear as memories are recorded."
                                : filtered.prefix(5).compactMap(\.title).joined(separator: " → "),
                            theme: theme
                        )
                        HStack(spacing: 10) {
                            RunwayGradientPrimaryButton(
                                label: "Record a Learning",
                                enabled: momentId?.isEmpty == false,
                                action: onRecordLearning
                            )
                            RunwayOutlineButton(
                                label: "Share",
                                enabled: false,
                                theme: theme,
                                action: onOpenQuickAdd
                            )
                        }
                    }
                    .padding(.horizontal, 16)
                    .padding(.vertical, 16)
                    .padding(.bottom, 56)
                }
            }
        }
        .background(theme.bg)
        .task(id: "\(refreshToken)-\(momentId ?? "")") { await load() }
    }

    private func memoryList(
        title: String,
        empty: String,
        items: [APIClient.BusinessMemoryPayload.MemoryInner.BusinessMemoryItem],
        accent: Color
    ) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title)
                .font(.plusJakarta(size: 14, weight: .semibold))
                .foregroundStyle(theme.text)
            if items.isEmpty {
                Text(empty)
                    .font(.plusJakarta(size: 13))
                    .foregroundStyle(theme.secondary)
                    .padding(12)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(theme.card)
                    .overlay(RoundedRectangle(cornerRadius: 12).stroke(theme.border))
                    .clipShape(RoundedRectangle(cornerRadius: 12))
            } else {
                ForEach(items.prefix(8)) { item in
                    HStack(spacing: 10) {
                        Circle().fill(accent).frame(width: 8, height: 8)
                        Text(item.title ?? "Memory")
                            .font(.plusJakarta(size: 13, weight: .semibold))
                            .foregroundStyle(theme.text)
                            .lineLimit(2)
                        Spacer(minLength: 0)
                    }
                    .padding(12)
                    .background(theme.card)
                    .overlay(RoundedRectangle(cornerRadius: 12).stroke(accent.opacity(0.35)))
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                }
            }
        }
    }

    private func isRisk(_ item: APIClient.BusinessMemoryPayload.MemoryInner.BusinessMemoryItem) -> Bool {
        let hay = (item.title ?? "").lowercased()
        return hay.contains("risk") || hay.contains("issue") || hay.contains("incident")
            || hay.contains("fail") || hay.contains("block") || hay.contains("problem")
    }

    private func load() async {
        guard let momentId, !momentId.isEmpty else {
            loading = false
            error = "Select a Business Moment."
            return
        }
        error = nil
        if let cached = BusinessTabDataCache.peekMemory(momentId)?.memory {
            memory = cached
            loading = false
        } else {
            loading = memory == nil
        }
        do {
            let tab = try await BusinessTabLoad.loadMemoryTab(momentId: momentId)
            memory = tab.memory
        } catch {
            self.error = error.localizedDescription
        }
        loading = false
    }
}

private extension String {
    var nilIfEmpty: String? {
        let t = trimmingCharacters(in: .whitespacesAndNewlines)
        return t.isEmpty ? nil : t
    }
}

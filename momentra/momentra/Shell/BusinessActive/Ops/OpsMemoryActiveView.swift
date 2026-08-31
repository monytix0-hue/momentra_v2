import SwiftUI

/// Figma `696:9450` Business Operations Memory — multi-section stack; honest empties.
struct OpsMemoryActiveView: View {
    let refreshToken: UInt64
    let momentId: String?
    let momentTitle: String?
    var onRecordLearning: () -> Void = {}

    @State private var memory: APIClient.BusinessMemoryPayload?
    @State private var scope = "All"
    @State private var loading = true
    @State private var error: String?

    private let theme = BusinessActiveTheme.businessOperations
    private let scopes = ["All", "Budget", "Vendors", "Approvals", "Issues"]

    private var items: [APIClient.BusinessMemoryPayload.MemoryInner.BusinessMemoryItem] {
        memory?.payload?.items ?? []
    }

    private var filtered: [APIClient.BusinessMemoryPayload.MemoryInner.BusinessMemoryItem] {
        guard scope != "All" else { return items }
        let q = scope.lowercased()
        return items.filter {
            let hay = ($0.title ?? "").lowercased()
            if q.hasPrefix("budget") { return hay.contains("budget") || hay.contains("spend") || hay.contains("cost") }
            if q.hasPrefix("vendor") { return hay.contains("vendor") || hay.contains("supplier") || hay.contains("sla") }
            if q.hasPrefix("approval") { return hay.contains("approval") || hay.contains("sign-off") || hay.contains("approve") }
            if q.hasPrefix("issue") { return hay.contains("issue") || hay.contains("incident") || hay.contains("risk") }
            return hay.contains(q)
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
        guard let first = filtered.first else { return nil }
        if let title = first.title, !title.isEmpty { return title }
        return nil
    }

    private var patternCount: String {
        items.count >= 3 ? "\(items.count / 3)" : "—"
    }

    private var accuracy: String {
        guard !items.isEmpty else { return "—" }
        let pct = Int((Double(successItems.count) / Double(max(items.count, 1))) * 100)
        return "\(pct)%"
    }

    var body: some View {
        Group {
            if loading && memory == nil {
                ProgressView().tint(theme.accent)
            } else {
                ScrollView {
                    VStack(alignment: .leading, spacing: 16) {
                        if let error {
                            Text(error).font(.caption).foregroundStyle(OpsColors.red)
                        }

                        OpsScopeDropdown(label: "Operations", theme: theme)

                        OpsFilterChipRow(chips: scopes, selected: scope, onSelect: { scope = $0 }, theme: theme)

                        OpsMemoryHeroSection(
                            ringLabel: items.isEmpty ? "—" : "Live",
                            learnings: "\(memoryCount)",
                            patterns: patternCount,
                            accuracy: accuracy,
                            showLive: !items.isEmpty,
                            theme: theme
                        )

                        OpsBiggestLearningCard(quote: biggestLearning, theme: theme)

                        OpsDiamondDivider(theme: theme)

                        patternNetworkSection

                        OpsDiamondDivider(theme: theme)

                        playbookSection

                        OpsDiamondDivider(theme: theme)

                        memoryListSection(title: "Success Memory", emptyCopy: "No success memories yet.", items: successItems, border: OpsColors.green)

                        memoryListSection(title: "Risk Memory", emptyCopy: "No risk memories yet.", items: riskItems, border: OpsColors.red)

                        wisdomSection

                        journeySection

                        HStack(spacing: 10) {
                            OpsGradientPrimaryButton(label: "Record a Learning", enabled: momentId != nil, action: onRecordLearning)
                            OpsOutlineButton(label: "Share with Team", enabled: false, action: {}, theme: theme)
                        }
                    }
                    .padding(.horizontal, 16)
                    .padding(.vertical, 16)
                    .padding(.bottom, 24)
                }
            }
        }
        .background(theme.bg)
        .task(id: "\(refreshToken)-\(momentId ?? "")") { await load() }
    }

    private var patternNetworkSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("Pattern Network")
                .font(.plusJakarta(size: 14, weight: .semibold))
                .foregroundStyle(theme.text)
            VStack(alignment: .leading, spacing: 12) {
                Text("Cross-memory patterns stay empty until live learnings exist.")
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(theme.secondary)
                HStack(spacing: 8) {
                    chip("Cause")
                    Text("→").foregroundStyle(theme.muted)
                    chip("Effect")
                    Spacer()
                    Text("—").font(.plusJakarta(size: 11)).foregroundStyle(theme.muted)
                }
            }
            .padding(16)
            .background(theme.card)
            .overlay(RoundedRectangle(cornerRadius: 16).stroke(theme.border))
            .clipShape(RoundedRectangle(cornerRadius: 16))
        }
    }

    private var playbookSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("Business Playbook")
                .font(.plusJakarta(size: 14, weight: .semibold))
                .foregroundStyle(theme.text)
            VStack(alignment: .leading, spacing: 8) {
                Text("Playbook entries are deferred — empty shell only.")
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(theme.secondary)
                Capsule().fill(theme.border).frame(height: 4)
            }
            .padding(16)
            .background(theme.card)
            .overlay(RoundedRectangle(cornerRadius: 16).stroke(theme.border))
            .clipShape(RoundedRectangle(cornerRadius: 16))
        }
    }

    private var wisdomSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("\"Operations wisdom compounds with every recorded learning.\"")
                .font(.plusJakarta(size: 13, weight: .semibold))
                .italic()
                .foregroundStyle(theme.text)
            Text("momentra intelligence")
                .font(.plusJakarta(size: 10, weight: .bold))
                .foregroundStyle(theme.muted)
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(theme.card)
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(OpsColors.lavender.opacity(0.2)))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private var journeySection: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("Knowledge Journey")
                .font(.plusJakarta(size: 14, weight: .semibold))
                .foregroundStyle(theme.text)
            if filtered.isEmpty {
                Text("Journey timeline appears when memory history is projected.")
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(theme.secondary)
                    .padding(16)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(theme.card)
                    .overlay(RoundedRectangle(cornerRadius: 16).stroke(theme.border))
                    .clipShape(RoundedRectangle(cornerRadius: 16))
            } else {
                ForEach(filtered.prefix(5)) { item in
                    HStack(spacing: 12) {
                        Circle().fill(theme.accent).frame(width: 8, height: 8)
                        Text(item.title ?? "Memory")
                            .font(.plusJakarta(size: 13, weight: .semibold))
                            .foregroundStyle(theme.text)
                    }
                    .padding(12)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(theme.card)
                    .overlay(RoundedRectangle(cornerRadius: 12).stroke(theme.border))
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                }
            }
        }
    }

    private func memoryListSection(
        title: String,
        emptyCopy: String,
        items: [APIClient.BusinessMemoryPayload.MemoryInner.BusinessMemoryItem],
        border: Color
    ) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title)
                .font(.plusJakarta(size: 14, weight: .semibold))
                .foregroundStyle(theme.text)
            if items.isEmpty {
                Text(emptyCopy)
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(theme.secondary)
                    .padding(12)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(theme.card)
                    .overlay(RoundedRectangle(cornerRadius: 12).stroke(theme.border))
                    .clipShape(RoundedRectangle(cornerRadius: 12))
            } else {
                ForEach(items) { item in
                    Text(item.title ?? "Memory")
                        .font(.plusJakarta(size: 14, weight: .semibold))
                        .foregroundStyle(theme.text)
                        .padding(12)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(theme.card)
                        .overlay(RoundedRectangle(cornerRadius: 12).stroke(border.opacity(0.35)))
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                }
            }
        }
    }

    private func chip(_ label: String) -> some View {
        Text(label)
            .font(.plusJakarta(size: 11))
            .foregroundStyle(theme.muted)
            .padding(.horizontal, 10)
            .padding(.vertical, 4)
            .background(theme.bg)
            .overlay(Capsule().stroke(theme.border))
            .clipShape(Capsule())
    }

    private func isRisk(_ item: APIClient.BusinessMemoryPayload.MemoryInner.BusinessMemoryItem) -> Bool {
        let hay = (item.title ?? "").lowercased()
        return hay.contains("risk") || hay.contains("issue") || hay.contains("incident")
            || hay.contains("fail") || hay.contains("block")
    }

    private func load() async {
        guard let momentId else {
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

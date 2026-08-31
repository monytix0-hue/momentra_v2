import SwiftUI

/// Figma `692:37078` Business Runway Moments — live activity timeline.
struct RunwayMomentsActiveView: View {
    let refreshToken: UInt64
    let momentTitle: String?
    let momentId: String?
    var onLogExpense: () -> Void = {}
    var onOpenQuickAdd: () -> Void = {}

    @State private var activities: [APIClient.ActivityItemPayload] = []
    @State private var timeline: APIClient.BusinessTimelinePayload?
    @State private var finance: APIClient.BusinessFinancePayload?
    @State private var filter = "All"
    @State private var loading = true
    @State private var error: String?

    private let theme = BusinessActiveTheme.businessRunway
    private let filters = ["All", "Revenue", "Expenses"]

    private var timelineItems: [APIClient.BusinessTimelineItem] { timeline?.items ?? [] }
    private var kpis: APIClient.BusinessTimelineKpis? { timeline?.kpis }

    private func matches(_ hay: String, _ f: String) -> Bool {
        let h = hay.lowercased()
        switch f.lowercased() {
        case "revenue": return h.contains("revenue") || h.contains("invoice") || h.contains("income")
        case "expenses": return h.contains("expense") || h.contains("spend") || h.contains("burn") || h.contains("cost")
        default: return true
        }
    }

    private var filteredTimeline: [APIClient.BusinessTimelineItem] {
        filter == "All" ? timelineItems : timelineItems.filter {
            matches("\($0.title) \($0.category) \($0.eventType)", filter)
        }
    }

    private var filteredActivities: [APIClient.ActivityItemPayload] {
        filter == "All" ? activities : activities.filter {
            matches("\($0.title) \($0.activityCode)", filter)
        }
    }

    private var hasTimeline: Bool { !timelineItems.isEmpty }
    private var showEmpty: Bool {
        hasTimeline ? filteredTimeline.isEmpty : filteredActivities.isEmpty
    }

    private var revenue: String {
        guard let val = finance?.totals?.first?.revenueTotal, !val.isEmpty else { return "—" }
        return maskedMoney(val)
    }

    private var spent: String {
        guard let val = finance?.totals?.first?.expenseTotal, !val.isEmpty else { return "—" }
        return maskedMoney(val)
    }

    private var spendEvents: Int {
        activities.filter {
            let h = ($0.title + $0.activityCode).uppercased()
            return h.contains("EXPENSE") || h.contains("SPEND") || h.contains("COST")
        }.count
    }

    private var burnRatio: CGFloat? {
        guard spendEvents > 0 else { return nil }
        return CGFloat(min(Double(spendEvents) / 20.0, 1.0))
    }

    private var healthRatio: CGFloat? {
        guard let quality = finance?.dataQuality?.uppercased(), quality == "OK" else { return nil }
        return 0.65
    }

    private var highlights: [APIClient.BusinessTimelineItem] {
        if !timelineItems.isEmpty {
            return Array(timelineItems.filter {
                let t = $0.eventType.uppercased()
                return t.contains("REVENUE") || t.contains("MILESTONE") || t.contains("INVOICE")
            }.prefix(3))
        }
        return Array(activities.filter {
            let code = $0.activityCode.uppercased()
            return code.contains("REVENUE") || code.contains("MILESTONE") || code.contains("INVOICE")
        }.prefix(3).map {
            APIClient.BusinessTimelineItem(
                eventId: $0.activityPayload?.activityId ?? $0.activityCode,
                eventType: $0.activityCode,
                title: $0.title.isEmpty ? $0.activityCode : $0.title,
                category: $0.activityCode,
                description: nil,
                occurredAt: $0.occurredAt
            )
        })
    }

    var body: some View {
        Group {
            if loading && activities.isEmpty && timelineItems.isEmpty {
                ProgressView().tint(theme.accent)
            } else {
                ScrollView {
                    VStack(alignment: .leading, spacing: 16) {
                        if let error {
                            Text(error).font(.caption).foregroundStyle(RunwayColors.red)
                        }
                        RunwayTimelineHeroCard(
                            entries: "\(hasTimeline ? timelineItems.count : activities.count)",
                            revenue: revenue,
                            activity: "\(activities.count)",
                            theme: theme
                        )
                        RunwayFilterChipRow(chips: filters, selected: filter, onSelect: { filter = $0 }, theme: theme)
                        timelineBlock
                        if !showEmpty {
                            Button("See Full History →") { onOpenQuickAdd() }
                                .font(.plusJakarta(size: 13, weight: .semibold))
                                .foregroundStyle(RunwayColors.linkAmber)
                        }
                        RunwayProgressSnapshot(
                            burnRatio: burnRatio,
                            collectionsRatio: nil,
                            healthRatio: healthRatio,
                            theme: theme
                        )
                        highlightsBlock
                        ctaCard
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

    @ViewBuilder
    private var timelineBlock: some View {
        if showEmpty {
            VStack(alignment: .leading, spacing: 8) {
                Text("Nothing on the financial timeline yet")
                    .font(.plusJakarta(size: 15, weight: .bold))
                    .foregroundStyle(theme.text)
                Text("Revenue, expenses, and milestones appear after live writes.")
                    .font(.plusJakarta(size: 13))
                    .foregroundStyle(theme.secondary)
            }
            .padding(16)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(theme.card)
            .overlay(RoundedRectangle(cornerRadius: 16).stroke(theme.border))
            .clipShape(RoundedRectangle(cornerRadius: 16))
        } else if hasTimeline {
            ForEach(filteredTimeline) { item in
                timelineRow(title: item.title.isEmpty ? item.eventType : item.title,
                            meta: [item.category, String(item.occurredAt.prefix(10))].filter { !$0.isEmpty }.joined(separator: " • "),
                            accent: accent(for: item.eventType))
            }
        } else {
            ForEach(Array(filteredActivities.enumerated()), id: \.offset) { _, act in
                timelineRow(
                    title: act.title.isEmpty ? act.activityCode : act.title,
                    meta: "\(act.activityCode) • \(String(act.occurredAt.prefix(10)))",
                    accent: RunwayColors.amber
                )
            }
        }
    }

    private func accent(for eventType: String) -> Color {
        let t = eventType.uppercased()
        if t.contains("EXPENSE") || t.contains("RISK") || t.contains("ISSUE") { return RunwayColors.red }
        if t.contains("REVENUE") || t.contains("INVOICE") || t.contains("MILESTONE") { return RunwayColors.amber }
        return RunwayColors.emerald
    }

    private func timelineRow(title: String, meta: String, accent: Color) -> some View {
        HStack(spacing: 12) {
            Circle()
                .fill(accent.opacity(0.15))
                .frame(width: 36, height: 36)
                .overlay(
                    Text(String(title.prefix(1)).uppercased())
                        .font(.plusJakarta(size: 14, weight: .bold))
                        .foregroundStyle(accent)
                )
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.plusJakarta(size: 14, weight: .semibold))
                    .foregroundStyle(theme.text)
                    .lineLimit(2)
                Text(meta)
                    .font(.plusJakarta(size: 11))
                    .foregroundStyle(theme.muted)
            }
            Spacer(minLength: 0)
        }
        .padding(14)
        .background(theme.card)
        .overlay(RoundedRectangle(cornerRadius: 14).stroke(theme.border))
        .clipShape(RoundedRectangle(cornerRadius: 14))
    }

    private var highlightsBlock: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Recent Highlights")
                .font(.plusJakarta(size: 14, weight: .semibold))
                .foregroundStyle(theme.text)
            Text("Key wins from recent activity.")
                .font(.plusJakarta(size: 11))
                .foregroundStyle(theme.muted)
            if highlights.isEmpty {
                Text("Highlights appear from live revenue and milestone items.")
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(theme.secondary)
                    .padding(12)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(theme.card)
                    .overlay(RoundedRectangle(cornerRadius: 12).stroke(theme.border))
                    .clipShape(RoundedRectangle(cornerRadius: 12))
            } else {
                ForEach(highlights) { item in
                    Text(item.title.isEmpty ? item.eventType : item.title)
                        .font(.plusJakarta(size: 13, weight: .semibold))
                        .foregroundStyle(theme.text)
                        .padding(12)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(theme.card)
                        .overlay(RoundedRectangle(cornerRadius: 12).stroke(theme.border))
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                }
            }
        }
    }

    private var ctaCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Record revenue or expense. Review financial milestones.")
                .font(.plusJakarta(size: 13))
                .foregroundStyle(theme.secondary)
            RunwayGradientPrimaryButton(
                label: "Log Expense",
                enabled: momentId?.isEmpty == false,
                action: onLogExpense
            )
            Button("See Full History →") { onOpenQuickAdd() }
                .font(.plusJakarta(size: 13, weight: .semibold))
                .foregroundStyle(RunwayColors.linkAmber)
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(theme.card)
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(theme.border))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private func load() async {
        guard let momentId, !momentId.isEmpty else {
            loading = false
            error = "Select a Business Moment."
            return
        }
        error = nil
        if let cached = BusinessTabDataCache.peekPulse(momentId) {
            if !cached.activities.isEmpty { activities = cached.activities }
            if finance == nil { finance = cached.finance }
            loading = false
        } else {
            loading = activities.isEmpty && timelineItems.isEmpty
        }
        async let timelineTask = APIClient.shared.getBusinessMomentTimeline(momentId: momentId)
        timeline = try? await timelineTask
        if finance == nil {
            finance = try? await APIClient.shared.getBusinessFinance(momentId: momentId).payload
        }
        if activities.isEmpty {
            do {
                activities = try await APIClient.shared.listBusinessActivity(momentId: momentId)
            } catch {
                self.error = error.localizedDescription
            }
        }
        loading = false
    }

    private func maskedMoney(_ value: String) -> String {
        guard !value.isEmpty else { return "—" }
        return UserDefaults.standard.bool(forKey: "momentra_hide_balances") ? "••••" : value
    }
}

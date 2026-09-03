import SwiftUI

/// Figma `692:35199` Team Operations Moments — timeline + activity; honest empty KPIs.
struct TeamOpsMomentsActiveView: View {
    let refreshToken: UInt64
    let momentTitle: String?
    let momentId: String?
    var onLogWin: () -> Void = {}
    var onOpenQuickAdd: () -> Void = {}

    @State private var activities: [APIClient.ActivityItemPayload] = []
    @State private var timeline: APIClient.BusinessTimelinePayload?
    @State private var capacityData: APIClient.BusinessCapacityPayload?
    @State private var filter = "All"
    @State private var loading = true
    @State private var error: String?

    private let theme = BusinessActiveTheme.teamOperations
    private let filters = ["All", "Milestones", "Decisions", "Deliveries"]

    private var timelineItems: [APIClient.BusinessTimelineItem] { timeline?.items ?? [] }
    private var kpis: APIClient.BusinessTimelineKpis? { timeline?.kpis }

    private func matches(_ hay: String, _ f: String) -> Bool {
        let h = hay.lowercased()
        switch f.lowercased() {
        case "milestones": return h.contains("milestone") || h.contains("ship") || h.contains("release")
        case "decisions": return h.contains("decision") || h.contains("align") || h.contains("approval")
        case "deliveries":
            return h.contains("deliver") || h.contains("ship") || h.contains("update")
                || h.contains("expense") || h.contains("win")
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

    private var pending: String {
        if let n = kpis?.highPriorityIssues, n > 0 { return "\(n)" }
        if let n = kpis?.updateCount, n > 0 { return "\(n)" }
        return "—"
    }

    private var issues: String {
        if let n = kpis?.issueCount, n > 0 { return "\(n)" }
        let count = activities.filter {
            let h = ($0.title + $0.activityCode).uppercased()
            return h.contains("ISSUE") || h.contains("BLOCK")
        }.count
        return count > 0 ? "\(count)" : "—"
    }

    private var highlights: [APIClient.BusinessTimelineItem] {
        if !timelineItems.isEmpty {
            return Array(timelineItems.filter {
                let t = $0.eventType.uppercased()
                return t.contains("UPDATE") || t.contains("MILESTONE") || t.contains("IMPROVEMENT") || t.contains("SHIP")
            }.prefix(3))
        }
        return Array(activities.filter {
            let code = $0.activityCode.uppercased()
            return code.contains("UPDATE") || code.contains("MILESTONE") || code.contains("IMPROVEMENT")
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

    private var capacityRatio: CGFloat? {
        guard let pct = capacityData?.capacityPct else { return nil }
        return CGFloat(min(max(pct, 0), 100)) / 100
    }

    private var deliveryRatio: CGFloat? {
        guard let n = kpis?.updateCount, n > 0 else { return nil }
        return CGFloat(min(Double(n) / 15.0, 1))
    }

    private var approvalsRatio: CGFloat? {
        guard let k = kpis, (k.issueCount ?? 0) > 0 else { return nil }
        return 1 - CGFloat(k.highPriorityIssues ?? 0) / CGFloat(max(k.issueCount ?? 0, 1))
    }

    var body: some View {
        Group {
            if loading && activities.isEmpty && timelineItems.isEmpty {
                ProgressView().tint(theme.accent)
            } else {
                NativeDashboardScaffold(background: theme.bg) {

                    NativeListSection {

                    VStack(alignment: .leading, spacing: 16) {
                        if let error {
                            Text(error).font(.caption).foregroundStyle(TeamOpsColors.red)
                        }
                        TeamOpsTimelineHeroCard(
                            members: "—",
                            pending: pending,
                            issues: issues,
                            theme: theme
                        )
                        TeamOpsFilterChipRow(chips: filters, selected: filter, onSelect: { filter = $0 }, theme: theme)
                        timelineBlock
                        if !showEmpty {
                            Button("See Full History →") { onOpenQuickAdd() }
                                .font(.plusJakarta(size: 13, weight: .semibold))
                                .foregroundStyle(TeamOpsColors.linkBlue)
                        }
                        TeamOpsProgressSnapshot(
                            deliveryRatio: deliveryRatio,
                            capacityRatio: capacityRatio,
                            approvalsRatio: approvalsRatio,
                            theme: theme
                        )
                        highlightsBlock
                        ctaCard
                    }

                    }

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
                Text("Nothing on the team timeline yet")
                    .font(.plusJakarta(size: 15, weight: .bold))
                    .foregroundStyle(theme.text)
                Text("Milestones, decisions, and deliveries appear after live writes.")
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
                    accent: TeamOpsColors.emerald
                )
            }
        }
    }

    private func accent(for eventType: String) -> Color {
        let t = eventType.uppercased()
        if t.contains("ISSUE") || t.contains("BLOCK") { return TeamOpsColors.red }
        if t.contains("UPDATE") || t.contains("DELIVER") || t.contains("SHIP") { return TeamOpsColors.emerald }
        if t.contains("DECISION") || t.contains("MILESTONE") { return TeamOpsColors.indigoLight }
        return TeamOpsColors.lavender
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
            Text("Key wins from this week.")
                .font(.plusJakarta(size: 11))
                .foregroundStyle(theme.muted)
            if highlights.isEmpty {
                Text("Highlights appear from live updates and milestones.")
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
            Text("Record what just happened. Review open items or record a delivery update.")
                .font(.plusJakarta(size: 13))
                .foregroundStyle(theme.secondary)
            TeamOpsGradientPrimaryButton(
                label: "Log a Win",
                enabled: momentId?.isEmpty == false,
                action: onLogWin
            )
            Button("See Full History →") { onOpenQuickAdd() }
                .font(.plusJakarta(size: 13, weight: .semibold))
                .foregroundStyle(TeamOpsColors.linkBlue)
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
            if activities.isEmpty { activities = cached.activities }
            capacityData = cached.capacity
            if !cached.activities.isEmpty { loading = false }
        } else if activities.isEmpty && timelineItems.isEmpty {
            loading = true
        }
        let capacityTask = capacityData == nil
            ? Task { try? await APIClient.shared.getBusinessCapacity(momentId: momentId) }
            : nil
        async let timelineTask = APIClient.shared.getBusinessMomentTimeline(momentId: momentId)
        timeline = try? await timelineTask
        if capacityData == nil {
            capacityData = await capacityTask?.value
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
}

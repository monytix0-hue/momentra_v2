import SwiftUI

/// Figma `692:44116` Business Operations Moments — live timeline + Figma layout.
struct OpsMomentsActiveView: View {
    let refreshToken: UInt64
    let momentId: String?
    let momentTitle: String?
    var onLogSpend: () -> Void = {}
    var onOpenQuickAdd: () -> Void = {}

    @State private var activities: [APIClient.ActivityItemPayload] = []
    @State private var timeline: APIClient.BusinessTimelinePayload?
    @State private var filter = "All"
    @State private var loading = true
    @State private var error: String?

    private let theme = BusinessActiveTheme.businessOperations
    private let baseFilters = ["Budget", "Vendors", "Issues", "Updates"]

    private var filterChips: [String] {
        let scope = momentTitle?.isEmpty == false
            ? (momentTitle!.count > 14 ? String(momentTitle!.prefix(12)) + "…" : momentTitle!)
            : "All ops"
        return [scope] + baseFilters
    }

    private var timelineItems: [APIClient.BusinessTimelineItem] { timeline?.items ?? [] }

    private var activeFilter: String {
        filter == "All" ? (filterChips.first ?? "All") : filter
    }

    private var filteredActivities: [APIClient.ActivityItemPayload] {
        if filter == "All" { return activities }
        return activities.filter { matchesFilter($0, filter: activeFilter) }
    }

    private var filteredTimeline: [APIClient.BusinessTimelineItem] {
        if filter == "All" { return timelineItems }
        return timelineItems.filter { matchesTimeline($0, filter: activeFilter) }
    }

    private var entryCount: Int { timelineItems.isEmpty ? activities.count : timelineItems.count }
    private var vendorCount: Int {
        timeline?.kpis?.vendorCount ?? timeline?.kpis?.activeContracts
            ?? activities.filter { matchesFilter($0, filter: "Vendors") }.count
    }
    private var issueCount: Int {
        timeline?.kpis?.issueCount ?? activities.filter { matchesFilter($0, filter: "Issues") }.count
    }

    private var budgetRatio: CGFloat? {
        if let n = timeline?.kpis?.spendEvents, n > 0 { return CGFloat(min(1, Double(n) / 20.0)) }
        return activities.isEmpty ? nil : CGFloat(filteredActivities.count) / CGFloat(max(activities.count, 1))
    }

    private var issuesRatio: CGFloat? {
        guard let k = timeline?.kpis, (k.issueCount ?? 0) > 0 else { return nil }
        let total = CGFloat(k.issueCount ?? 0)
        let high = CGFloat(k.highPriorityIssues ?? 0)
        return 1 - (high / max(total, 1))
    }

    private var milestonesRatio: CGFloat? {
        if let n = timeline?.kpis?.updateCount, n > 0 { return CGFloat(min(1, Double(n) / 15.0)) }
        return timelineItems.isEmpty ? nil : CGFloat(highlights.count) / CGFloat(max(timelineItems.count, 1))
    }

    private var highlights: [APIClient.BusinessTimelineItem] {
        if !timelineItems.isEmpty {
            return timelineItems.filter {
                let t = $0.eventType.uppercased()
                return t.contains("ISSUE") || t.contains("IMPROVEMENT") || t.contains("UPDATE")
            }.prefix(3).map { $0 }
        }
        return activities.filter {
            let c = $0.activityCode.uppercased()
            return c.contains("ISSUE") || c.contains("IMPROVEMENT") || c.contains("UPDATE")
        }.prefix(3).map {
            APIClient.BusinessTimelineItem(
                eventId: $0.id,
                eventType: $0.activityCode,
                title: $0.title.isEmpty ? $0.activityCode : $0.title,
                category: $0.activityCode,
                description: nil,
                occurredAt: $0.occurredAt
            )
        }
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
                            Text(error).font(.caption).foregroundStyle(OpsColors.red)
                        }

                        OpsTimelineHeroCard(entries: entryCount, vendors: vendorCount, issues: issueCount, theme: theme)

                        OpsFilterChipRow(
                            chips: filterChips,
                            selected: filter == "All" ? (filterChips.first ?? "All") : filter,
                            onSelect: { chip in
                                filter = chip == filterChips.first ? "All" : chip
                            },
                            theme: theme
                        )

                        let showEmpty = timelineItems.isEmpty ? filteredActivities.isEmpty : filteredTimeline.isEmpty
                        if showEmpty {
                            emptyTimelineCard
                        } else if !timelineItems.isEmpty {
                            ForEach(filteredTimeline) { item in
                                timelineEntryRow(item)
                            }
                        } else {
                            ForEach(filteredActivities) { item in
                                activityTimelineRow(item)
                            }
                        }

                        if !showEmpty {
                            Button(action: onOpenQuickAdd) {
                                Text("See full history →")
                                    .font(.plusJakarta(size: 13, weight: .semibold))
                                    .foregroundStyle(OpsColors.linkBlue)
                            }
                            .buttonStyle(.plain)
                        }

                        OpsProgressSnapshot(
                            budgetRatio: budgetRatio,
                            issuesRatio: issuesRatio,
                            milestonesRatio: milestonesRatio,
                            theme: theme
                        )

                        highlightsSection

                        OpsGradientPrimaryButton(
                            label: "+ Log Spend",
                            enabled: momentId != nil,
                            action: onLogSpend
                        )
                    }

                    }

                }
            }
        }
        .background(theme.bg)
        .task(id: "\(refreshToken)-\(momentId ?? "")") { await load() }
    }

    private var emptyTimelineCard: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Nothing on the ops timeline yet")
                .font(.plusJakarta(size: 15, weight: .bold))
                .foregroundStyle(theme.text)
            Text("Spend, vendors, issues, and updates appear after live writes.")
                .font(.plusJakarta(size: 13))
                .foregroundStyle(theme.secondary)
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(theme.card)
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(theme.border))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private var highlightsSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Recent Highlights")
                .font(.plusJakarta(size: 14, weight: .semibold))
                .foregroundStyle(theme.text)
            if highlights.isEmpty {
                Text("Highlights appear from live issues, improvements, and updates.")
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

    private func timelineEntryRow(_ item: APIClient.BusinessTimelineItem) -> some View {
        HStack(alignment: .top, spacing: 12) {
            VStack {
                Circle().fill(theme.accent).frame(width: 10, height: 10)
                Rectangle().fill(theme.border).frame(width: 2, height: 40)
            }
            VStack(alignment: .leading, spacing: 6) {
                HStack {
                    Text(item.title.isEmpty ? item.eventType : item.title)
                        .font(.plusJakarta(size: 14, weight: .bold))
                        .foregroundStyle(theme.text)
                    Spacer()
                    Circle()
                        .fill(theme.accent.opacity(0.15))
                        .frame(width: 24, height: 24)
                        .overlay(
                            Text(String(item.category.prefix(1)).uppercased())
                                .font(.plusJakarta(size: 10, weight: .bold))
                                .foregroundStyle(theme.accent)
                        )
                }
                if !item.category.isEmpty {
                    Text(item.category)
                        .font(.plusJakarta(size: 10, weight: .bold))
                        .foregroundStyle(OpsColors.ctaText)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 3)
                        .background(theme.accent)
                        .clipShape(Capsule())
                }
                Text(item.occurredAt)
                    .font(.plusJakarta(size: 11))
                    .foregroundStyle(theme.secondary)
            }
        }
        .padding(16)
        .background(theme.card)
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(theme.border))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private func activityTimelineRow(_ item: APIClient.ActivityItemPayload) -> some View {
        HStack(alignment: .top, spacing: 12) {
            VStack {
                Circle().fill(theme.accent).frame(width: 10, height: 10)
                Rectangle().fill(theme.border).frame(width: 2, height: 40)
            }
            VStack(alignment: .leading, spacing: 6) {
                Text(item.title.isEmpty ? item.activityCode : item.title)
                    .font(.plusJakarta(size: 14, weight: .bold))
                    .foregroundStyle(theme.text)
                Text(item.activityCode)
                    .font(.plusJakarta(size: 10, weight: .bold))
                    .foregroundStyle(OpsColors.ctaText)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 3)
                    .background(theme.accent)
                    .clipShape(Capsule())
                Text(item.occurredAt)
                    .font(.plusJakarta(size: 11))
                    .foregroundStyle(theme.secondary)
            }
        }
        .padding(16)
        .background(theme.card)
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(theme.border))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private func matchesFilter(_ item: APIClient.ActivityItemPayload, filter: String) -> Bool {
        let hay = (item.title + " " + item.activityCode).lowercased()
        let q = filter.lowercased()
        if q.hasPrefix("budget") || q.hasPrefix("spend") {
            return hay.contains("spend") || hay.contains("expense") || hay.contains("budget")
        }
        if q.hasPrefix("vendor") { return hay.contains("vendor") || hay.contains("sla") || hay.contains("contract") }
        if q.hasPrefix("issue") { return hay.contains("issue") || hay.contains("blocker") || hay.contains("risk") }
        if q.hasPrefix("update") { return hay.contains("update") || hay.contains("approval") || hay.contains("improvement") }
        return true
    }

    private func matchesTimeline(_ item: APIClient.BusinessTimelineItem, filter: String) -> Bool {
        let hay = (item.title + " " + item.category + " " + item.eventType).lowercased()
        let q = filter.lowercased()
        if q.hasPrefix("budget") || q.hasPrefix("spend") {
            return hay.contains("spend") || hay.contains("expense") || item.eventType == "EXPENSE"
        }
        if q.hasPrefix("vendor") { return hay.contains("vendor") || hay.contains("contract") || hay.contains("sla") }
        if q.hasPrefix("issue") { return hay.contains("issue") || item.eventType == "ISSUE" }
        if q.hasPrefix("update") {
            return hay.contains("update") || item.eventType == "UPDATE" || item.eventType == "IMPROVEMENT"
        }
        return true
    }

    private func load() async {
        guard let momentId else {
            loading = false
            error = "Select a Business Moment."
            return
        }
        error = nil
        if let cached = BusinessTabDataCache.peekPulse(momentId), !cached.activities.isEmpty {
            activities = cached.activities
            loading = false
        } else {
            loading = activities.isEmpty
        }
        async let timelineTask = APIClient.shared.getBusinessMomentTimeline(momentId: momentId)
        timeline = try? await timelineTask
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

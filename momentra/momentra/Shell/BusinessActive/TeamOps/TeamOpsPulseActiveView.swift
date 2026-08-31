import SwiftUI

/// Figma `692:34967` Team Operations Pulse — live bind; honest empties for workload/AI.
struct TeamOpsPulseActiveView: View {
    let refreshToken: UInt64
    let momentTitle: String?
    let momentId: String?
    var onLogDelivery: () -> Void = {}
    var onOpenQuickAdd: () -> Void = {}

    @State private var pulse: APIClient.BusinessPulsePayload?
    @State private var life: APIClient.BusinessLifePayload?
    @State private var activities: [APIClient.ActivityItemPayload] = []
    @State private var capacityData: APIClient.BusinessCapacityPayload?
    @State private var workloadData: APIClient.BusinessWorkloadPayload?
    @State private var loading = true
    @State private var error: String?

    private let theme = BusinessActiveTheme.teamOperations

    private var healthScore: String {
        let raw = pulse?.payload?.financialHealthScore?.trimmingCharacters(in: .whitespacesAndNewlines)
        return (raw?.isEmpty == false) ? raw! : "—"
    }

    private var hasLive: Bool { healthScore != "—" }
    private var attentionCount: Int { pulse?.payload?.attentionCount ?? 0 }

    private var members: String {
        if let raw = life?.payload?.teamOperationsPayload?["memberCapacity"]?.value {
            if let s = raw as? String, !s.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                return s
            }
            if let n = raw as? NSNumber { return n.stringValue }
            if let i = raw as? Int { return "\(i)" }
        }
        if let n = pulse?.payload?.activeMomentCount, n > 0 { return "\(n)" }
        return "—"
    }

    private var capacity: String {
        if let pct = capacityData?.capacityPct {
            return "\(pct)%"
        }
        return "—"
    }
    private var openItems: String { attentionCount > 0 ? "\(attentionCount)" : "—" }

    private var narrative: String {
        guard let n = Double(healthScore) else {
            return attentionCount > 0 ? "Needs attention" : "Awaiting live health signal"
        }
        if n >= 80 { return "Strong & Stable" }
        if n >= 50 { return "Needs focus" }
        return "At risk"
    }

    private var subtitle: String {
        if attentionCount > 0 {
            return "\(attentionCount) item\(attentionCount == 1 ? "" : "s") need your eye today."
        }
        if hasLive { return "Execution health from live pulse." }
        return "Status updates as team activity projects."
    }

    private var attentionActs: [APIClient.ActivityItemPayload] {
        activities.filter {
            let hay = ($0.title + " " + $0.activityCode).uppercased()
            return hay.contains("ISSUE") || hay.contains("BLOCK") || hay.contains("RISK") || hay.contains("APPROVAL")
        }.prefix(5).map { $0 }
    }

    var body: some View {
        Group {
            if loading && pulse == nil {
                ProgressView().tint(theme.accent)
            } else {
                ScrollView {
                    VStack(alignment: .leading, spacing: 24) {
                        if let error {
                            Text(error).font(.caption).foregroundStyle(TeamOpsColors.red)
                        }
                        healthCard
                        TeamOpsWorkloadSection(theme: theme, workloadData: workloadData)
                        needsAttentionSection
                        recentDeliverySection
                        TeamOpsIntelligenceSection(theme: theme)
                        ctaRow
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

    private var healthCard: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack(alignment: .center, spacing: 16) {
                TeamOpsHeroHealthRing(score: healthScore, showLive: hasLive, theme: theme)
                VStack(alignment: .leading, spacing: 4) {
                    Text("EXECUTION HEALTH")
                        .font(.plusJakarta(size: 10, weight: .bold))
                        .foregroundStyle(theme.muted)
                    Text(narrative)
                        .font(.plusJakarta(size: 18, weight: .bold))
                        .foregroundStyle(theme.text)
                        .lineLimit(2)
                    Text(subtitle)
                        .font(.plusJakarta(size: 13))
                        .foregroundStyle(theme.secondary)
                }
                Spacer(minLength: 0)
            }
            HStack(spacing: 8) {
                TeamOpsTintedMetricTile(value: members, label: "members", detail: "Team capacity", tint: TeamOpsColors.lavender, theme: theme)
                TeamOpsTintedMetricTile(value: capacity, label: "capacity", detail: capacityData == nil ? "API pending" : "Team utilization", tint: TeamOpsColors.emerald, theme: theme, valueColor: TeamOpsColors.emerald)
                TeamOpsTintedMetricTile(value: openItems, label: "open items", detail: "Needs attention", tint: TeamOpsColors.amber, theme: theme, valueColor: TeamOpsColors.amber)
            }
        }
        .padding(20)
        .background(LinearGradient(colors: [Color(hex: "#161B26"), Color(hex: "#1A1F2E")], startPoint: .topLeading, endPoint: .bottomTrailing))
        .overlay(RoundedRectangle(cornerRadius: 20).stroke(theme.border))
        .clipShape(RoundedRectangle(cornerRadius: 20))
    }

    private var needsAttentionSection: some View {
        let items = attentionActs
        return VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("Needs Attention")
                    .font(.plusJakarta(size: 14, weight: .semibold))
                    .foregroundStyle(theme.text)
                Spacer()
                let badge = max(attentionCount, items.count)
                if badge > 0 {
                    Text("\(badge)")
                        .font(.plusJakarta(size: 11, weight: .bold))
                        .foregroundStyle(.white)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 2)
                        .background(TeamOpsColors.red)
                        .clipShape(Capsule())
                }
            }
            if items.isEmpty {
                Text("No items need attention")
                    .font(.plusJakarta(size: 13))
                    .foregroundStyle(theme.secondary)
                    .padding(16)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(theme.card)
                    .overlay(RoundedRectangle(cornerRadius: 16).stroke(theme.border))
                    .clipShape(RoundedRectangle(cornerRadius: 16))
            } else {
                ForEach(Array(items.enumerated()), id: \.offset) { index, act in
                    attentionCard(
                        title: act.title.isEmpty ? act.activityCode : act.title,
                        severity: index == 0 ? "HIGH" : "MED",
                        detail: String(act.occurredAt.prefix(16))
                    )
                }
            }
        }
    }

    private func attentionCard(title: String, severity: String, detail: String) -> some View {
        let badgeColor: Color = severity.contains("HIGH") ? TeamOpsColors.red : TeamOpsColors.amber
        return VStack(alignment: .leading, spacing: 10) {
            HStack(spacing: 8) {
                Circle().fill(badgeColor).frame(width: 8, height: 8)
                Text(severity)
                    .font(.plusJakarta(size: 9, weight: .bold))
                    .foregroundStyle(.white)
                    .padding(.horizontal, 6)
                    .padding(.vertical, 2)
                    .background(badgeColor)
                    .clipShape(RoundedRectangle(cornerRadius: 4))
                Spacer()
            }
            Text(title)
                .font(.plusJakarta(size: 14, weight: .semibold))
                .foregroundStyle(theme.text)
            Text(detail)
                .font(.plusJakarta(size: 12))
                .foregroundStyle(theme.muted)
            Capsule().fill(badgeColor.opacity(0.25)).frame(height: 4)
            Text("Escalation API not mounted")
                .font(.plusJakarta(size: 12))
                .foregroundStyle(theme.muted)
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(theme.card)
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(badgeColor.opacity(0.35)))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private var recentDeliverySection: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("Recent Delivery")
                    .font(.plusJakarta(size: 14, weight: .semibold))
                    .foregroundStyle(theme.text)
                Spacer()
                Text("This Week")
                    .font(.plusJakarta(size: 10, weight: .bold))
                    .foregroundStyle(theme.secondary)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 4)
                    .overlay(Capsule().stroke(theme.border))
            }
            let rows = Array(activities.prefix(5))
            if rows.isEmpty {
                Text("Deliveries appear as team updates and activity project.")
                    .font(.plusJakarta(size: 13))
                    .foregroundStyle(theme.secondary)
                    .padding(16)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(theme.card)
                    .overlay(RoundedRectangle(cornerRadius: 16).stroke(theme.border))
                    .clipShape(RoundedRectangle(cornerRadius: 16))
            } else {
                ForEach(rows) { act in
                    HStack(spacing: 12) {
                        Circle().fill(TeamOpsColors.emerald).frame(width: 10, height: 10)
                        VStack(alignment: .leading, spacing: 2) {
                            Text(act.title.isEmpty ? act.activityCode : act.title)
                                .font(.plusJakarta(size: 14, weight: .semibold))
                                .foregroundStyle(theme.text)
                                .lineLimit(2)
                            Text("\(act.activityCode) • \(String(act.occurredAt.prefix(10)))")
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
                Button("View all activity →") { onOpenQuickAdd() }
                    .font(.plusJakarta(size: 12, weight: .bold))
                    .foregroundStyle(TeamOpsColors.linkBlue)
                    .frame(maxWidth: .infinity, alignment: .trailing)
            }
        }
    }

    private var ctaRow: some View {
        HStack(spacing: 12) {
            TeamOpsGradientPrimaryButton(
                label: "+ Log Delivery",
                enabled: momentId?.isEmpty == false,
                action: onLogDelivery
            )
            TeamOpsOutlineButton(
                label: "View This Week's Report",
                enabled: true,
                theme: theme,
                action: onOpenQuickAdd
            )
        }
    }

    private func load() async {
        guard let momentId, !momentId.isEmpty else {
            loading = false
            error = "Select a Business Moment."
            return
        }
        error = nil
        if let cached = BusinessTabDataCache.peekPulse(momentId) {
            pulse = cached.pulse
            life = cached.life
            activities = cached.activities
            capacityData = cached.capacity
            workloadData = cached.workload
            loading = false
        } else {
            loading = pulse == nil
        }
        do {
            let tab = try await BusinessTabLoad.loadPulseTab(momentId: momentId, fetchTeamOpsMetrics: true)
            pulse = tab.pulse
            life = tab.life
            activities = tab.activities
            capacityData = tab.capacity
            workloadData = tab.workload
        } catch {
            self.error = error.localizedDescription
        }
        loading = false
    }
}

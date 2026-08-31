import SwiftUI

/// Figma `692:43993` Business Operations Pulse — live bind; honest empties.
struct OpsPulseActiveView: View {
    let refreshToken: UInt64
    let momentTitle: String?
    let momentId: String?
    var onLogSpend: () -> Void = {}
    var onOpenQuickAdd: () -> Void = {}

    @State private var pulse: APIClient.BusinessPulsePayload?
    @State private var activities: [APIClient.ActivityItemPayload] = []
    @State private var loading = true
    @State private var error: String?

    private let theme = BusinessActiveTheme.businessOperations

    private var ops: APIClient.BusinessPulsePayload.PulseInner.OperationsExtras? {
        pulse?.payload?.operations
    }

    private var quality: [String: String] { ops?.sectionQuality ?? [:] }

    private func isReal(_ key: String) -> Bool {
        (quality[key] ?? "").uppercased() == "REAL_DATA"
    }

    private var healthScore: String {
        guard isReal("slaCompliance"), let n = ops?.slaCompliancePct else { return "—" }
        return "\(n)"
    }

    private var showLive: Bool { isReal("slaCompliance") && ops?.slaCompliancePct != nil }

    private var monthlySpend: String {
        guard isReal("monthlySpend"), let raw = ops?.monthlySpend, !raw.isEmpty else { return "—" }
        return formatMoney(raw)
    }

    private var vendorCount: String {
        guard isReal("activeVendors"), let n = ops?.activeVendorCount else { return "—" }
        return "\(n)"
    }

    private var slaPct: String {
        guard isReal("slaCompliance"), let n = ops?.slaCompliancePct else { return "—" }
        return "\(n)%"
    }

    private var openIssues: Int { ops?.openIssueCount ?? pulse?.payload?.attentionCount ?? 0 }

    private var attentionItems: [APIClient.BusinessPulsePayload.PulseInner.OperationsExtras.OpsAttentionItem] {
        guard isReal("needsAttention") else { return [] }
        return ops?.needsAttention ?? []
    }

    private var attentionDisplay: [(String, String, Bool)] {
        if !attentionItems.isEmpty {
            return attentionItems.map { ($0.title, $0.severity, !$0.issueId.isEmpty) }
        }
        return fallbackAttention
    }

    private var categories: [APIClient.BusinessPulsePayload.PulseInner.OperationsExtras.SpendCategorySlice] {
        guard isReal("spendByCategory") else { return [] }
        return ops?.spendByCategory ?? []
    }

    private var narrative: String {
        guard let n = ops?.slaCompliancePct, isReal("slaCompliance") else {
            return openIssues > 0 ? "Needs attention" : "Awaiting live ops signal"
        }
        if n >= 90 { return "Stable & Optimizing" }
        if n >= 70 { return "Needs focus" }
        return "At risk"
    }

    private var subtitle: String {
        if openIssues > 0 {
            return "\(openIssues) open issue\(openIssues == 1 ? "" : "s") need attention"
        }
        if let n = ops?.slaCompliancePct, isReal("slaCompliance") {
            return "SLA compliance at \(n)%"
        }
        return "Status from open issues and SLA checks"
    }

    var body: some View {
        Group {
            if loading && pulse == nil {
                ProgressView().tint(theme.accent)
            } else {
                ScrollView {
                    VStack(alignment: .leading, spacing: 24) {
                        if let error {
                            Text(error).font(.caption).foregroundStyle(OpsColors.red)
                        }
                        opsHealthCard
                        OpsCategoryBarSection(categories: categories, theme: theme)
                        needsAttentionSection
                        recentActivitySection
                        OpsIntelligenceSection(theme: theme)
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

    private var opsHealthCard: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack(alignment: .center, spacing: 16) {
                OpsHeroHealthRing(score: healthScore, showLive: showLive, theme: theme)
                VStack(alignment: .leading, spacing: 4) {
                    Text("OPERATIONS HEALTH")
                        .font(.plusJakarta(size: 10, weight: .bold))
                        .foregroundStyle(theme.muted)
                    Text(narrative)
                        .font(.plusJakarta(size: 18, weight: .bold))
                        .foregroundStyle(theme.text)
                    Text(subtitle)
                        .font(.plusJakarta(size: 13))
                        .foregroundStyle(theme.secondary)
                }
                Spacer(minLength: 0)
            }
            HStack(spacing: 8) {
                OpsTintedMetricTile(value: monthlySpend, label: "monthly spend", detail: "Trend unavailable", tint: OpsColors.green, theme: theme)
                OpsTintedMetricTile(value: vendorCount, label: "vendors", detail: "Trend unavailable", tint: OpsColors.indigoLight, theme: theme)
                OpsTintedMetricTile(value: slaPct, label: "sla", detail: "Trend unavailable", tint: OpsColors.amber, theme: theme, valueColor: OpsColors.amber)
            }
        }
        .padding(20)
        .background(LinearGradient(colors: [Color(hex: "#161B26"), Color(hex: "#1A1F2E")], startPoint: .topLeading, endPoint: .bottomTrailing))
        .overlay(RoundedRectangle(cornerRadius: 20).stroke(theme.border))
        .clipShape(RoundedRectangle(cornerRadius: 20))
    }

    private var needsAttentionSection: some View {
        let items = attentionDisplay
        return VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("Needs Attention")
                    .font(.plusJakarta(size: 14, weight: .semibold))
                    .foregroundStyle(theme.text)
                Spacer()
                if !items.isEmpty {
                    Text("\(items.count)")
                        .font(.plusJakarta(size: 11, weight: .bold))
                        .foregroundStyle(.white)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 2)
                        .background(OpsColors.red)
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
                ForEach(Array(items.enumerated()), id: \.offset) { _, item in
                    OpsAttentionCard(title: item.0, severity: item.1, hasAction: item.2, theme: theme)
                }
            }
        }
    }

    private var fallbackAttention: [(String, String, Bool)] {
        activities.filter {
            let hay = ($0.title + " " + $0.activityCode).uppercased()
            return hay.contains("ISSUE") || hay.contains("BLOCK") || hay.contains("SLA")
        }.prefix(5).map { ($0.title.isEmpty ? $0.activityCode : $0.title, "OPEN", true) }
    }

    private var recentActivitySection: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("Recent Operations Activity")
                    .font(.plusJakarta(size: 14, weight: .semibold))
                    .foregroundStyle(theme.text)
                Spacer()
                Text("This Week")
                    .font(.plusJakarta(size: 11, weight: .semibold))
                    .foregroundStyle(theme.secondary)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 4)
                    .overlay(Capsule().stroke(theme.border))
            }
            VStack(alignment: .leading, spacing: 16) {
                if activities.isEmpty {
                    Text("Activity appears after live writes.")
                        .font(.plusJakarta(size: 13))
                        .foregroundStyle(theme.secondary)
                } else {
                    ForEach(Array(activities.prefix(8))) { item in
                        let completed = item.activityCode.uppercased().contains("COMPLETE")
                            || item.activityCode.uppercased().contains("APPROVED")
                            || item.activityCode.uppercased().contains("RESOLVED")
                        HStack(alignment: .top, spacing: 12) {
                            VStack {
                                Circle().fill(theme.accent).frame(width: 10, height: 10)
                                Rectangle().fill(theme.border).frame(width: 2, height: 24)
                            }
                            VStack(alignment: .leading, spacing: 4) {
                                HStack {
                                    Text(item.title.isEmpty ? item.activityCode : item.title)
                                        .font(.plusJakarta(size: 13, weight: .bold))
                                        .foregroundStyle(theme.text)
                                    if completed {
                                        Text("Completed")
                                            .font(.plusJakarta(size: 9, weight: .bold))
                                            .foregroundStyle(OpsColors.green)
                                            .padding(.horizontal, 8)
                                            .padding(.vertical, 2)
                                            .background(OpsColors.green.opacity(0.12))
                                            .clipShape(Capsule())
                                    }
                                }
                                Text(item.occurredAt)
                                    .font(.plusJakarta(size: 11))
                                    .foregroundStyle(theme.muted)
                            }
                        }
                    }
                }
                Button(action: onOpenQuickAdd) {
                    Text("View all activity →")
                        .font(.plusJakarta(size: 12, weight: .bold))
                        .foregroundStyle(OpsColors.linkBlue)
                        .frame(maxWidth: .infinity, alignment: .trailing)
                }
                .buttonStyle(.plain)
            }
            .padding(16)
            .background(theme.card)
            .overlay(RoundedRectangle(cornerRadius: 16).stroke(theme.border))
            .clipShape(RoundedRectangle(cornerRadius: 16))
        }
    }

    private var ctaRow: some View {
        HStack(spacing: 12) {
            OpsGradientPrimaryButton(label: "+ Log Delivery", enabled: momentId != nil, action: onLogSpend)
            OpsOutlineButton(label: "View Report", enabled: true, action: onOpenQuickAdd, theme: theme)
        }
    }

    private func formatMoney(_ raw: String) -> String {
        guard let n = Double(raw) else { return raw }
        if n >= 100_000 { return String(format: "₹%.1fL", n / 100_000) }
        if n >= 1000 { return String(format: "₹%.1fK", n / 1000) }
        return String(format: "₹%.0f", n)
    }

    private func load() async {
        guard let momentId else {
            loading = false
            error = "Select a Business Moment."
            return
        }
        error = nil
        if let cached = BusinessTabDataCache.peekPulse(momentId) {
            pulse = cached.pulse
            activities = cached.activities
            loading = false
        } else {
            loading = pulse == nil
        }
        do {
            let tab = try await BusinessTabLoad.loadPulseTab(momentId: momentId)
            pulse = tab.pulse
            activities = tab.activities
        } catch {
            self.error = error.localizedDescription
        }
        loading = false
    }
}

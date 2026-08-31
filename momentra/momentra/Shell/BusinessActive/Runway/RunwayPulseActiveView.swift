import SwiftUI

/// Figma `692:36956` Business Runway Pulse — live bind; honest empties for AI.
struct RunwayPulseActiveView: View {
    let refreshToken: UInt64
    let momentTitle: String?
    let momentId: String?
    var onLogExpense: () -> Void = {}
    var onOpenQuickAdd: () -> Void = {}

    @State private var pulse: APIClient.BusinessPulsePayload?
    @State private var finance: APIClient.BusinessFinancePayload?
    @State private var life: APIClient.BusinessLifePayload?
    @State private var activities: [APIClient.ActivityItemPayload] = []
    @State private var loading = true
    @State private var error: String?

    private let theme = BusinessActiveTheme.businessRunway

    private var healthScore: String {
        let raw = pulse?.payload?.financialHealthScore?.trimmingCharacters(in: .whitespacesAndNewlines)
        return (raw?.isEmpty == false) ? raw! : "—"
    }

    private var hasLive: Bool { healthScore != "—" }
    private var attentionCount: Int { pulse?.payload?.attentionCount ?? 0 }

    private var runwayMonths: String {
        let raw = pulse?.payload?.runwayMonths?.trimmingCharacters(in: .whitespacesAndNewlines)
        return (raw?.isEmpty == false) ? raw! : "—"
    }

    private var cash: String {
        if let val = mapRunwayStr("availableCash") { return maskedMoney(val) }
        if let val = finance?.totals?.first?.expenseTotal, !val.isEmpty { return maskedMoney(val) }
        return "—"
    }

    private var burn: String {
        if let val = mapRunwayStr("monthlySpending") { return maskedMoney(val) }
        if let val = finance?.totals?.first?.expenseTotal, !val.isEmpty { return maskedMoney(val) }
        return "—"
    }

    private var categoryBurn: [(label: String, amount: String, pct: Int)] {
        guard let raw = pulse?.payload?.widgetPayload?["categoryBreakdown"]?.value as? [[String: Any]] else {
            return []
        }
        return raw.compactMap { row in
            guard let label = row["label"] as? String else { return nil }
            let amount = (row["amount"] as? String) ?? String(describing: row["amount"] ?? "0")
            let pct = (row["pct"] as? Int) ?? Int((row["pct"] as? Double) ?? 0)
            return (label, amount, pct)
        }
    }

    private var narrative: String {
        guard let n = Double(healthScore) else {
            return attentionCount > 0 ? "Needs attention" : "Awaiting live health signal"
        }
        if n >= 80 { return "Strong & Growing" }
        if n >= 50 { return "Needs focus" }
        return "At risk"
    }

    private var subtitle: String {
        if attentionCount > 0 {
            return "\(attentionCount) item\(attentionCount == 1 ? "" : "s") need your eye today."
        }
        if hasLive { return "Capital runway from live pulse / finance projection." }
        return "Status updates as finance activity projects."
    }

    private var attentionActs: [APIClient.ActivityItemPayload] {
        activities.filter {
            let hay = ($0.title + " " + $0.activityCode).uppercased()
            return hay.contains("ISSUE") || hay.contains("BUDGET") || hay.contains("TAX")
                || hay.contains("RISK") || hay.contains("ALERT") || hay.contains("OVERDUE")
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
                            Text(error).font(.caption).foregroundStyle(RunwayColors.red)
                        }
                        healthCard
                        burnSection
                        needsAttentionSection
                        recentActivitySection
                        RunwayIntelligenceSection(theme: theme)
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
                RunwayHeroHealthRing(score: healthScore, showLive: hasLive, theme: theme)
                VStack(alignment: .leading, spacing: 4) {
                    Text("RUNWAY HEALTH")
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
                RunwayTintedMetricTile(value: runwayMonths, label: "months runway", detail: "From pulse", tint: RunwayColors.amber, theme: theme)
                RunwayTintedMetricTile(value: cash, label: "cash balance", detail: "Live or prefs", tint: RunwayColors.amber, theme: theme)
                RunwayTintedMetricTile(value: burn, label: "monthly burn", detail: "Spend total", tint: RunwayColors.amber, theme: theme)
            }
        }
        .padding(20)
        .background(LinearGradient(colors: [Color(hex: "#161B26"), Color(hex: "#1A1F2E")], startPoint: .topLeading, endPoint: .bottomTrailing))
        .overlay(RoundedRectangle(cornerRadius: 20).stroke(theme.border))
        .clipShape(RoundedRectangle(cornerRadius: 20))
    }

    private var burnSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            VStack(alignment: .leading, spacing: 4) {
                Text("Burn Rate by Category")
                    .font(.plusJakarta(size: 14, weight: .semibold))
                    .foregroundStyle(theme.text)
                Text("Monthly allocation breakdown")
                    .font(.plusJakarta(size: 11))
                    .foregroundStyle(theme.muted)
            }
            if !categoryBurn.isEmpty {
                ForEach(Array(categoryBurn.prefix(6).enumerated()), id: \.offset) { _, row in
                    burnRow(row.label, shareLabel: maskedMoney(row.amount), pct: row.pct)
                }
            } else {
                Text("Category burn appears when live breakdown is projected. Showing — until then.")
                    .font(.plusJakarta(size: 13))
                    .foregroundStyle(theme.secondary)
                    .padding(16)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(theme.card)
                    .overlay(RoundedRectangle(cornerRadius: 16).stroke(theme.border))
                    .clipShape(RoundedRectangle(cornerRadius: 16))
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(theme.card)
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(theme.border))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private func burnRow(_ label: String, shareLabel: String, pct: Int? = nil) -> some View {
        let filled = pct.map { min(3, max(0, Int((Double($0) / 100.0) * 3.0))) } ?? 2
        return HStack {
            Text(label)
                .font(.plusJakarta(size: 13, weight: .semibold))
                .foregroundStyle(theme.text)
            Spacer()
            HStack(spacing: 4) {
                ForEach(0..<3, id: \.self) { i in
                    RoundedRectangle(cornerRadius: 3)
                        .fill(i < filled ? RunwayColors.amber : RunwayColors.amber.opacity(0.25))
                        .frame(width: 18, height: 10)
                }
            }
            Text(shareLabel)
                .font(.plusJakarta(size: 12, weight: .bold))
                .foregroundStyle(RunwayColors.amber)
                .frame(minWidth: 56, alignment: .trailing)
        }
    }

    private var needsAttentionSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("Needs Attention")
                    .font(.plusJakarta(size: 14, weight: .semibold))
                    .foregroundStyle(theme.text)
                Spacer()
                let badge = max(attentionCount, attentionActs.count)
                if badge > 0 {
                    Text("\(badge)")
                        .font(.plusJakarta(size: 11, weight: .bold))
                        .foregroundStyle(.white)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 2)
                        .background(RunwayColors.red)
                        .clipShape(Capsule())
                }
            }
            if attentionActs.isEmpty {
                Text("No items need attention")
                    .font(.plusJakarta(size: 13))
                    .foregroundStyle(theme.secondary)
                    .padding(16)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(theme.card)
                    .overlay(RoundedRectangle(cornerRadius: 16).stroke(theme.border))
                    .clipShape(RoundedRectangle(cornerRadius: 16))
            } else {
                ForEach(Array(attentionActs.enumerated()), id: \.offset) { index, act in
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
        let badgeColor: Color = severity.contains("HIGH") ? RunwayColors.red : RunwayColors.amber
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
            Text(severity.contains("HIGH") ? "Review budget →" : "Prepare docs →")
                .font(.plusJakarta(size: 12, weight: .bold))
                .foregroundStyle(RunwayColors.linkAmber)
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(theme.card)
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(badgeColor.opacity(0.35)))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private var recentActivitySection: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("Recent Financial Activity")
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
                Text("Financial activity appears as revenues, expenses, and updates project.")
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
                        Circle().fill(RunwayColors.amber).frame(width: 10, height: 10)
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
                    .foregroundStyle(RunwayColors.linkAmber)
                    .frame(maxWidth: .infinity, alignment: .trailing)
            }
        }
    }

    private var ctaRow: some View {
        HStack(spacing: 12) {
            RunwayGradientPrimaryButton(
                label: "+ Log Expense",
                enabled: momentId?.isEmpty == false,
                action: onLogExpense
            )
            RunwayOutlineButton(
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
            finance = cached.finance
            life = cached.life
            activities = cached.activities
            loading = false
        } else {
            loading = pulse == nil
        }
        do {
            let tab = try await BusinessTabLoad.loadPulseTab(momentId: momentId)
            pulse = tab.pulse
            finance = tab.finance
            life = tab.life
            activities = tab.activities
        } catch {
            self.error = error.localizedDescription
        }
        loading = false
    }

    private func maskedMoney(_ value: String) -> String {
        guard !value.isEmpty else { return "—" }
        return UserDefaults.standard.bool(forKey: "momentra_hide_balances") ? "••••" : value
    }

    private func mapRunwayStr(_ key: String) -> String? {
        guard let raw = life?.payload?.runwayPayload?[key]?.value else { return nil }
        if let s = raw as? String {
            let t = s.trimmingCharacters(in: .whitespacesAndNewlines)
            return t.isEmpty || t == "null" ? nil : t
        }
        if let n = raw as? NSNumber { return n.stringValue }
        let str = "\(raw)".trimmingCharacters(in: .whitespacesAndNewlines)
        return str.isEmpty || str == "null" ? nil : str
    }
}

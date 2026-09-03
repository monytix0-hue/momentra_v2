import SwiftUI

/// Figma 575:14165 — Group Pulse active tab (Trip fidelity).
struct GroupPulseActiveView: View {
    let refreshToken: UInt64
    let momentTitle: String?
    let momentId: String?
    var onAddExpense: () -> Void = {}
    var onViewSplits: () -> Void = {}
    var onOpenFinance: () -> Void = {}
    var onOpenMemory: () -> Void = {}
    var onOpenChat: () -> Void = {}
    var onOpenItinerary: () -> Void = {}

    @State private var pulse: APIClient.GroupPulsePayload?
    @State private var finance: APIClient.GroupFinancePayload?
    @State private var activity: [APIClient.ActivityItemPayload] = []
    @State private var participants: [APIClient.GroupParticipantPayload] = []
    @State private var insights: [AnalyticsInsightItemPayload] = []
    @State private var loading = true
    @State private var error: String?

    private var hideBalances: Bool {
        UserDefaults.standard.bool(forKey: "momentra_hide_balances")
    }

    private var primaryTotal: APIClient.GroupFinanceTotalsPayload? { finance?.totals?.first }
    private var currency: String { primaryTotal?.currencyCode ?? "INR" }
    private var utilization: Int {
        GroupFinanceFormat.utilizationPercent(
            expenseTotal: primaryTotal?.expenseTotal,
            budgetTotal: primaryTotal?.budgetTotal
        )
    }
    private var displayTitle: String { momentTitle ?? pulse?.title ?? "Trip" }
    private var participantCount: Int { pulse?.payload?.participantCount ?? participants.count }
    private var openTasks: Int { pulse?.payload?.openTaskCount ?? 0 }
    private var attentionCount: Int { pulse?.payload?.attentionCount ?? 0 }
    private var expenseCount: Int { finance?.expenseCount ?? 0 }
    private var positions: [APIClient.GroupFinancePositionPayload] { finance?.positions ?? [] }
    private var maxAbsNet: Double {
        positions
            .map { abs((GroupFinanceFormat.parseAmount($0.netPosition) as NSDecimalNumber).doubleValue) }
            .max() ?? 0
    }
    private var financeBarMax: Double {
        let expense = (GroupFinanceFormat.parseAmount(primaryTotal?.expenseTotal) as NSDecimalNumber).doubleValue
        let contribution = (GroupFinanceFormat.parseAmount(primaryTotal?.contributionTotal) as NSDecimalNumber).doubleValue
        let budget = (GroupFinanceFormat.parseAmount(primaryTotal?.budgetTotal) as NSDecimalNumber).doubleValue
        return max(expense, contribution, budget, 1)
    }
    private var nameById: [String: String] {
        Dictionary(uniqueKeysWithValues: participants.map {
            ($0.participantId, $0.displayName ?? String($0.participantId.prefix(8)))
        })
    }

    var body: some View {
        Group {
            if loading && pulse == nil {
                ProgressView().tint(GroupActiveTheme.brand)
            } else {
                NativeDashboardScaffold(background: GroupActiveTheme.bg) {

                    NativeListSection {

                    VStack(alignment: .leading, spacing: 14) {
                        if let error {
                            Text(error)
                                .font(.plusJakarta(size: 12))
                                .foregroundStyle(Color(hex: "#F87171"))
                        }

                        tripHeroHeader

                        HStack(spacing: 8) {
                            tripQuickTile(label: "Photos", emoji: "📸", accent: Color(hex: "#FBBF24"), enabled: true, action: onOpenMemory)
                            tripQuickTile(label: "Chat", emoji: "💬", accent: Color(hex: "#A855F7"), enabled: true, action: onOpenChat)
                            tripQuickTile(label: "Itinerary", emoji: "🗺️", accent: Color(hex: "#A16207"), enabled: true, action: onOpenItinerary)
                            tripQuickTile(label: "Splits", emoji: "💸", accent: Color(hex: "#22C55E"), enabled: true, action: onViewSplits)
                        }

                        GroupSectionCard(title: "Group Pulse") {
                            Text("Live trip signals from your group — no invented health scores.")
                                .font(.plusJakarta(size: 12))
                                .foregroundStyle(GroupActiveTheme.secondary)
                            ScrollView(.horizontal, showsIndicators: false) {
                                HStack(spacing: 8) {
                                    GroupQuickChip(label: "\(openTasks) tasks", enabled: false, action: {})
                                    GroupQuickChip(label: "\(participantCount) people", enabled: false, action: {})
                                    GroupQuickChip(label: "\(expenseCount) expenses", enabled: false, action: {})
                                }
                            }
                            HStack {
                                VStack(alignment: .leading, spacing: 6) {
                                    Text("Health score")
                                        .font(.plusJakarta(size: 12))
                                        .foregroundStyle(GroupActiveTheme.secondary)
                                    GroupEmptySection(
                                        message: "Score not available yet",
                                        detail: "Group health scoring is coming soon — no invented numbers."
                                    )
                                }
                                Spacer()
                                GroupProgressRing(percent: 0, centerLabel: "—", centerSub: "Coming soon")
                            }
                        }

                        GroupSectionCard(title: "Needs Attention", badge: {
                            if attentionCount > 0 {
                                Text("\(attentionCount)")
                                    .font(.plusJakarta(size: 11, weight: .bold))
                                    .foregroundStyle(Color(hex: "#131313"))
                                    .padding(.horizontal, 8)
                                    .padding(.vertical, 3)
                                    .background(GroupActiveTheme.accentOrange)
                                    .clipShape(Capsule())
                            }
                        }) {
                            if attentionCount > 0 {
                                Text("\(attentionCount) items need a look from the group.")
                                    .font(.plusJakarta(size: 13))
                                    .foregroundStyle(GroupActiveTheme.brand)
                            } else {
                                GroupEmptySection(
                                    message: "All clear for now",
                                    detail: "Attention items appear when the backend flags them — nothing invented here."
                                )
                            }
                        }

                        GroupSectionCard(title: "Progress Tracker") {
                            Text("\(utilization)% of budget used")
                                .font(.plusJakarta(size: 14, weight: .semibold))
                                .foregroundStyle(GroupActiveTheme.text)
                            GroupProgressBar(percent: utilization)
                            HStack(spacing: 8) {
                                GroupMetricTile(label: "Tasks", value: "\(openTasks)")
                                GroupMetricTile(
                                    label: "Budget",
                                    value: GroupFinanceFormat.compactMoney(primaryTotal?.budgetTotal, currencyCode: currency)
                                )
                                GroupMetricTile(label: "Moments", value: "\(expenseCount)")
                            }
                        }
                        .contentShape(Rectangle())
                        .onTapGesture(perform: onOpenFinance)

                        GroupSectionCard(title: "Participation") {
                            if positions.isEmpty && participants.isEmpty {
                                GroupEmptySection(
                                    message: "No participation data yet",
                                    detail: "Positions appear after shared expenses are recorded."
                                )
                            } else if positions.isEmpty {
                                ForEach(participants.prefix(6)) { person in
                                    Text(person.displayName ?? String(person.participantId.prefix(8)))
                                        .font(.plusJakarta(size: 13, weight: .semibold))
                                        .foregroundStyle(GroupActiveTheme.text)}
                            } else {
                                ForEach(positions.prefix(6)) { pos in
                                    participationRow(pos)
                                }
                            }
                        }

                        GroupSectionCard(title: "Settlement & Contributions", badge: {
                            Text("View Splits")
                                .font(.plusJakarta(size: 12, weight: .semibold))
                                .foregroundStyle(GroupActiveTheme.accentOrange)
                                .padding(.horizontal, 8)
                                .padding(.vertical, 4)
                                .background(Color(hex: "#33FF7A3D"))
                                .clipShape(RoundedRectangle(cornerRadius: 8))
                                .onTapGesture(perform: onViewSplits)
                        }) {
                            if primaryTotal == nil && finance?.viewerPosition == nil {
                                GroupEmptySection(message: "No finance totals yet", detail: "Add an expense to see settlement data.")
                            } else {
                                orangeBalanceCard
                                if primaryTotal != nil {
                                    financeBar(
                                        label: "Expenses",
                                        value: primaryTotal?.expenseTotal,
                                        color: GroupActiveTheme.accentOrange
                                    )
                                    financeBar(
                                        label: "Contributions",
                                        value: primaryTotal?.contributionTotal,
                                        color: Color(hex: "#22C55E")
                                    )
                                }
                            }
                        }

                        GroupSectionCard(title: "Recent Activity") {
                            if activity.isEmpty {
                                GroupEmptySection(message: "No recent activity", detail: "Expenses and contributions will show here.")
                            } else {
                                ForEach(activity) { item in
                                    activityRow(item)
                                }
                            }
                        }

                        GroupPulseInsightsSectionCard(insights: insights)
                    }

                    }

                }
                .nativeStickyFooter(background: GroupActiveTheme.bg) {
                    GroupCtaButton(label: "Add Expense", enabled: true, action: onAddExpense)
                }
            }
        }
        .background(GroupActiveTheme.bg)
        .task(id: "\(refreshToken)-\(momentId ?? "")") { await load() }
    }

    private var tripHeroHeader: some View {
        VStack(alignment: .leading, spacing: 10) {
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    heroPill(displayTitle, solid: true)
                    heroPill("Trip · \(participantCount) people", solid: false)
                }
            }
            Text("🌴")
                .font(.system(size: 20))
            Text(displayTitle)
                .font(.plusJakarta(size: 24, weight: .heavy))
                .foregroundStyle(GroupActiveTheme.text)
            Text("Shared trip pulse")
                .font(.plusJakarta(size: 13))
                .foregroundStyle(GroupActiveTheme.secondary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(20)
        .background(
            LinearGradient(
                colors: [Color(hex: "#3D2A24"), Color(hex: "#131313"), Color(hex: "#1A1512")],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        )
        .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
    }

    private func heroPill(_ label: String, solid: Bool) -> some View {
        Text(label)
            .font(.plusJakarta(size: 10, weight: .bold))
            .foregroundStyle(GroupActiveTheme.text)
            .lineLimit(1)
            .padding(.horizontal, 10)
            .padding(.vertical, 5)
            .background(solid ? Color(hex: "#33FFB598") : Color(hex: "#221A1512"))
            .overlay(Capsule().stroke(GroupActiveTheme.border, lineWidth: 1))
            .clipShape(Capsule())
    }

    private func tripQuickTile(
        label: String,
        emoji: String,
        accent: Color,
        enabled: Bool,
        action: @escaping () -> Void
    ) -> some View {
        Button(action: action) {
            VStack(spacing: 6) {
                Text(emoji)
                    .font(.system(size: 22))
                    .frame(width: 56, height: 56)
                    .background(accent.opacity(0.22))
                    .overlay(
                        RoundedRectangle(cornerRadius: 16, style: .continuous)
                            .stroke(accent.opacity(0.35), lineWidth: 1)
                    )
                    .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                Text(label)
                    .font(.plusJakarta(size: 11, weight: .semibold))
                    .foregroundStyle(GroupActiveTheme.text)
                    .lineLimit(1)
            }
            .frame(maxWidth: .infinity)
            .opacity(enabled ? 1 : 0.45)
        }
        .buttonStyle(.plain)
        .disabled(!enabled)
    }

    private var orangeBalanceCard: some View {
        let viewer = finance?.viewerPosition
        let outstanding = primaryTotal?.outstandingTotal
        let headline: String = {
            if let netRaw = viewer?.netPosition {
                let net = GroupFinanceFormat.parseAmount(netRaw)
                let v = (net as NSDecimalNumber).doubleValue
                if v < 0 {
                    let owed = GroupFinanceFormat.formatMoney(String(format: "%.2f", abs(v)), currencyCode: currency)
                    return "You owe \(maskMoney(owed))"
                }
                if v > 0 {
                    return "You are owed \(maskMoney(GroupFinanceFormat.formatMoney(netRaw, currencyCode: currency)))"
                }
                return "You're settled up"
            }
            return "Outstanding \(maskMoney(GroupFinanceFormat.formatMoney(outstanding, currencyCode: currency)))"
        }()
        return Button(action: onOpenFinance) {
            VStack(alignment: .leading, spacing: 4) {
                Text("YOUR BALANCE")
                    .font(.plusJakarta(size: 11, weight: .bold))
                    .foregroundStyle(Color.white.opacity(0.85))
                Text(headline)
                    .font(.plusJakarta(size: 16, weight: .heavy))
                    .foregroundStyle(.white)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(16)
            .background(
                LinearGradient(
                    colors: [GroupActiveTheme.accentOrange, TripSheetTokens.accentEnd],
                    startPoint: .leading,
                    endPoint: .trailing
                )
            )
            .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
        }
        .buttonStyle(.plain)
    }

    private func participationRow(_ pos: APIClient.GroupFinancePositionPayload) -> some View {
        let name = nameById[pos.participantId] ?? String(pos.participantId.prefix(8))
        let net = GroupFinanceFormat.parseAmount(pos.netPosition)
        let netValue = (net as NSDecimalNumber).doubleValue
        let pct: Int = {
            if maxAbsNet > 0 {
                return min(100, max(8, Int((abs(netValue) / maxAbsNet) * 100)))
            }
            return positions.isEmpty ? 0 : min(100, 100 / positions.count)
        }()
        let netLabel = maskMoney(GroupFinanceFormat.formatMoney(pos.netPosition, currencyCode: pos.currencyCode))
        return VStack(alignment: .leading, spacing: 6) {
            HStack {
                Text(name)
                    .font(.plusJakarta(size: 13, weight: .semibold))
                    .foregroundStyle(GroupActiveTheme.text)
                Spacer()
                Text(netLabel)
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(netValue >= 0 ? Color(hex: "#4ADE80") : GroupActiveTheme.accentOrange)
            }
            GroupProgressBar(percent: pct)
        }
        .padding(.vertical, 4)
    }

    private func financeBar(label: String, value: String?, color: Color) -> some View {
        let amt = (GroupFinanceFormat.parseAmount(value) as NSDecimalNumber).doubleValue
        let pct = financeBarMax > 0 ? min(100, Int((amt / financeBarMax) * 100)) : 0
        return VStack(alignment: .leading, spacing: 4) {
            HStack {
                Text(label)
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(GroupActiveTheme.secondary)
                Spacer()
                Text(maskMoney(GroupFinanceFormat.formatMoney(value, currencyCode: currency)))
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(GroupActiveTheme.text)
            }
            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    Capsule().fill(Color(hex: "#2A2624"))
                    Capsule()
                        .fill(color)
                        .frame(width: geo.size.width * CGFloat(pct) / 100)
                }
            }
            .frame(height: 8)
        }
    }

    private func activityRow(_ item: APIClient.ActivityItemPayload) -> some View {
        HStack(spacing: 12) {
            Text(activityGlyph(item.activityCode))
                .font(.system(size: 14))
                .frame(width: 36, height: 36)
                .background(GroupActiveTheme.brandSoft)
                .overlay(Circle().stroke(GroupActiveTheme.border, lineWidth: 1))
                .clipShape(Circle())
            VStack(alignment: .leading, spacing: 2) {
                Text(item.title)
                    .font(.plusJakarta(size: 13, weight: .medium))
                    .foregroundStyle(GroupActiveTheme.text)
                Text(formatOccurredAt(item.occurredAt))
                    .font(.plusJakarta(size: 11))
                    .foregroundStyle(GroupActiveTheme.secondary)
            }
        }
        .padding(.vertical, 6)
    }

    private func maskMoney(_ value: String) -> String {
        hideBalances ? "••••" : value
    }

    private func activityGlyph(_ code: String) -> String {
        let upper = code.uppercased()
        if upper.contains("EXPENSE") { return "💸" }
        if upper.contains("SETTLE") { return "✅" }
        if upper.contains("CONTRIB") { return "🤝" }
        return "📌"
    }

    private func formatOccurredAt(_ raw: String) -> String {
        let iso = ISO8601DateFormatter()
        iso.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        let fallback = ISO8601DateFormatter()
        fallback.formatOptions = [.withInternetDateTime]
        guard let date = iso.date(from: raw) ?? fallback.date(from: raw) else { return raw }
        let formatter = DateFormatter()
        formatter.dateFormat = "d MMM · HH:mm"
        return formatter.string(from: date)
    }

    private func load() async {
        guard let momentId else { loading = false; return }
        error = nil
        if let cached = GroupTabDataCache.peekPulse(momentId) {
            pulse = cached.pulse
            finance = cached.finance
            activity = cached.activities
            loading = false
        } else {
            loading = true
        }
        do {
            async let pulseResult = APIClient.shared.getGroupPulse(momentId: momentId)
            async let financeResult = APIClient.shared.getGroupFinance(momentId: momentId)
            async let activityResult = APIClient.shared.listGroupActivity(momentId: momentId, limit: 8)
            async let partsResult = APIClient.shared.listGroupParticipants(momentId: momentId)
            async let insightsResult = APIClient.shared.listAnalyticsInsights(scopeType: "MOMENT", scopeId: momentId)
            async let metricsResult = APIClient.shared.listAnalyticsMetrics(scopeType: "MOMENT", scopeId: momentId)
            async let refreshResult = APIClient.shared.refreshAnalytics(context: "GROUP_PULSE", momentId: momentId)
            let loadedPulse = try await pulseResult
            let finFacet = try await financeResult
            let loadedActivity = try await activityResult
            let loadedParts = (try? await partsResult) ?? []
            let loadedFinance = finFacet.payload ?? loadedPulse.payload?.finance
            pulse = loadedPulse
            finance = loadedFinance
            activity = loadedActivity
            participants = loadedParts
            insights = (try? await insightsResult)?.items ?? []
            _ = try? await metricsResult
            _ = try? await refreshResult
            GroupTabDataCache.putPulse(momentId, .init(
                title: loadedPulse.title,
                pulse: loadedPulse,
                finance: loadedFinance,
                activities: loadedActivity
            ))
        } catch {
            self.error = error.localizedDescription
        }
        loading = false
    }
}

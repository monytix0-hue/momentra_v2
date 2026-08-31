import SwiftUI

/// Figma 575:14165 — Group Pulse active tab (Trip fidelity).
struct GroupPulseActiveView: View {
    let refreshToken: UInt64
    let momentTitle: String?
    let momentId: String?
    var onAddExpense: () -> Void = {}
    var onViewSplits: () -> Void = {}
    var onOpenFinance: () -> Void = {}

    @State private var pulse: APIClient.GroupPulsePayload?
    @State private var finance: APIClient.GroupFinancePayload?
    @State private var activity: [APIClient.ActivityItemPayload] = []
    @State private var participants: [APIClient.GroupParticipantPayload] = []
    @State private var loading = true
    @State private var error: String?

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
                ScrollView {
                    VStack(alignment: .leading, spacing: 14) {
                        if let error {
                            Text(error).font(.caption).foregroundStyle(Color(hex: "#F87171"))
                        }

                        tripHeroHeader

                        HStack(spacing: 8) {
                            tripQuickTile(label: "Photos", emoji: "📸", accent: Color(hex: "#FBBF24"), enabled: false, action: {})
                            tripQuickTile(label: "Chat", emoji: "💬", accent: Color(hex: "#A855F7"), enabled: false, action: {})
                            tripQuickTile(label: "Itinerary", emoji: "🗺️", accent: Color(hex: "#A16207"), enabled: false, action: {})
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

                        GroupSectionCard(title: "Needs Attention") {
                            if attentionCount > 0 {
                                HStack {
                                    Spacer()
                                    Text("\(attentionCount)")
                                        .font(.plusJakarta(size: 11, weight: .bold))
                                        .foregroundStyle(Color(hex: "#131313"))
                                        .padding(.horizontal, 8)
                                        .padding(.vertical, 3)
                                        .background(GroupActiveTheme.accentOrange)
                                        .clipShape(Capsule())
                                }
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
                            let positions = finance?.positions ?? []
                            if positions.isEmpty {
                                GroupEmptySection(
                                    message: "No participation data yet",
                                    detail: "Positions appear after shared expenses are recorded."
                                )
                            } else {
                                let maxAbs = positions
                                    .map { abs((GroupFinanceFormat.parseAmount($0.netPosition) as NSDecimalNumber).doubleValue) }
                                    .max() ?? 0
                                ForEach(positions.prefix(6)) { pos in
                                    participationRow(pos, maxAbs: maxAbs)
                                }
                            }
                        }

                        GroupSectionCard(title: "Settlement & Contributions") {
                            if primaryTotal == nil {
                                GroupEmptySection(message: "No finance totals yet", detail: "Add an expense to see settlement data.")
                            } else {
                                balanceBanner
                                Text("\(GroupFinanceFormat.formatMoney(primaryTotal?.expenseTotal, currencyCode: currency)) Total Pool")
                                    .font(.plusJakarta(size: 12))
                                    .foregroundStyle(GroupActiveTheme.secondary)
                                financeBar(
                                    label: "Shared expenses",
                                    value: primaryTotal?.expenseTotal,
                                    color: Color(hex: "#22C55E")
                                )
                                financeBar(
                                    label: "Individual contributions",
                                    value: primaryTotal?.contributionTotal,
                                    color: GroupActiveTheme.accentOrange
                                )
                                Button("View Splits →", action: onViewSplits)
                                    .font(.plusJakarta(size: 13, weight: .semibold))
                                    .foregroundStyle(GroupActiveTheme.accentOrange)
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

                        GroupSectionCard(title: "Momentra Insights", badge: { GroupComingSoonBadge() }) {
                            GroupEmptySection(
                                message: "AI insights for groups",
                                detail: "Personalized group insights are on the roadmap."
                            )
                        }

                        GroupCtaButton(label: "Add Expense", enabled: momentId != nil, action: onAddExpense)
                    }
                    .padding(.horizontal, 16)
                    .padding(.vertical, 12)
                    .padding(.bottom, 56)
                }
            }
        }
        .background(GroupActiveTheme.bg)
        .task(id: "\(refreshToken)-\(momentId ?? "")") { await load() }
    }

    private var tripHeroHeader: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(spacing: 8) {
                Text(displayTitle)
                    .font(.plusJakarta(size: 11, weight: .semibold))
                    .foregroundStyle(GroupActiveTheme.text)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 5)
                    .background(Color.white.opacity(0.12))
                    .clipShape(Capsule())
                Text("Trip · \(participantCount) people")
                    .font(.plusJakarta(size: 11, weight: .semibold))
                    .foregroundStyle(GroupActiveTheme.text)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 5)
                    .background(Color.white.opacity(0.12))
                    .clipShape(Capsule())
            }
            HStack(alignment: .firstTextBaseline, spacing: 8) {
                Text("🌴")
                Text(displayTitle)
                    .font(.plusJakarta(size: 24, weight: .heavy))
                    .foregroundStyle(GroupActiveTheme.text)
            }
            Text("Shared experience pulse")
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

    private func tripQuickTile(
        label: String,
        emoji: String,
        accent: Color,
        enabled: Bool,
        action: @escaping () -> Void
    ) -> some View {
        Button(action: action) {
            VStack(spacing: 8) {
                Text(emoji)
                    .font(.system(size: 22))
                    .frame(width: 40, height: 40)
                    .background(accent.opacity(0.22))
                    .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                Text(label)
                    .font(.plusJakarta(size: 11, weight: .semibold))
                    .foregroundStyle(GroupActiveTheme.text)
                    .lineLimit(1)
            }
            .frame(maxWidth: .infinity)
            .frame(height: 72)
            .background(GroupActiveTheme.card)
            .overlay(
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .stroke(GroupActiveTheme.border, lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
            .opacity(enabled ? 1 : 0.45)
        }
        .buttonStyle(.plain)
        .disabled(!enabled)
    }

    private var balanceBanner: some View {
        let viewer = finance?.viewerPosition
        let outstanding = primaryTotal?.outstandingTotal
        let label: String = {
            if let net = viewer?.netPosition {
                let v = (GroupFinanceFormat.parseAmount(net) as NSDecimalNumber).doubleValue
                if v > 0 {
                    return "Your Balance: You get \(GroupFinanceFormat.formatMoney(net, currencyCode: currency))"
                }
                if v < 0 {
                    let owed = String(format: "%.2f", abs(v))
                    return "Your Balance: You owe \(GroupFinanceFormat.formatMoney(owed, currencyCode: currency))"
                }
                return "Your Balance: Settled up"
            }
            return "Outstanding: \(GroupFinanceFormat.formatMoney(outstanding, currencyCode: currency))"
        }()
        return Text(label)
            .font(.plusJakarta(size: 15, weight: .bold))
            .foregroundStyle(Color.white)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(16)
            .background(
                LinearGradient(
                    colors: [GroupActiveTheme.accentOrange, TripSheetTokens.accentEnd],
                    startPoint: .leading,
                    endPoint: .trailing
                )
            )
            .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
    }

    private func participationRow(_ pos: APIClient.GroupFinancePositionPayload, maxAbs: Double) -> some View {
        let name = nameById[pos.participantId] ?? String(pos.participantId.prefix(8))
        let net = abs((GroupFinanceFormat.parseAmount(pos.netPosition) as NSDecimalNumber).doubleValue)
        let pct = maxAbs > 0 ? min(100, Int((net / maxAbs) * 100)) : 0
        return VStack(alignment: .leading, spacing: 6) {
            HStack {
                Text(String(name.prefix(1)).uppercased())
                    .font(.plusJakarta(size: 12, weight: .bold))
                    .foregroundStyle(GroupActiveTheme.brand)
                    .frame(width: 32, height: 32)
                    .background(GroupActiveTheme.brandSoft)
                    .clipShape(Circle())
                VStack(alignment: .leading, spacing: 2) {
                    Text(name)
                        .font(.plusJakarta(size: 13, weight: .semibold))
                        .foregroundStyle(GroupActiveTheme.text)
                    Text("Net \(GroupFinanceFormat.formatMoney(pos.netPosition, currencyCode: pos.currencyCode))")
                        .font(.plusJakarta(size: 11))
                        .foregroundStyle(GroupActiveTheme.secondary)
                }
                Spacer()
            }
            GroupProgressBar(percent: pct)
        }
        .padding(.vertical, 4)
    }

    private func financeBar(label: String, value: String?, color: Color) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack {
                Text(label).font(.plusJakarta(size: 12)).foregroundStyle(GroupActiveTheme.secondary)
                Spacer()
                Text(GroupFinanceFormat.formatMoney(value, currencyCode: currency))
                    .font(.plusJakarta(size: 12, weight: .semibold))
                    .foregroundStyle(GroupActiveTheme.text)
            }
            GeometryReader { geo in
                let expense = (GroupFinanceFormat.parseAmount(primaryTotal?.expenseTotal) as NSDecimalNumber).doubleValue
                let amt = (GroupFinanceFormat.parseAmount(value) as NSDecimalNumber).doubleValue
                let w = expense > 0 ? min(1, amt / expense) : 0
                ZStack(alignment: .leading) {
                    Capsule().fill(Color(hex: "#2A2624")).frame(height: 6)
                    Capsule().fill(color).frame(width: geo.size.width * w, height: 6)
                }
            }
            .frame(height: 6)
        }
    }

    private func activityRow(_ item: APIClient.ActivityItemPayload) -> some View {
        HStack(alignment: .top, spacing: 10) {
            Text("•")
                .font(.plusJakarta(size: 14, weight: .bold))
                .foregroundStyle(GroupActiveTheme.brand)
                .frame(width: 28, height: 28)
                .background(GroupActiveTheme.card)
                .overlay(Circle().stroke(GroupActiveTheme.border, lineWidth: 1))
                .clipShape(Circle())
            VStack(alignment: .leading, spacing: 2) {
                Text(item.title)
                    .font(.plusJakarta(size: 13, weight: .medium))
                    .foregroundStyle(GroupActiveTheme.text)
                Text(item.occurredAt)
                    .font(.plusJakarta(size: 11))
                    .foregroundStyle(GroupActiveTheme.secondary)
            }
        }
        .padding(.vertical, 4)
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
            let loadedPulse = try await pulseResult
            let finFacet = try await financeResult
            let loadedActivity = try await activityResult
            let loadedParts = (try? await partsResult) ?? []
            let loadedFinance = finFacet.payload ?? loadedPulse.payload?.finance
            pulse = loadedPulse
            finance = loadedFinance
            activity = loadedActivity
            participants = loadedParts
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

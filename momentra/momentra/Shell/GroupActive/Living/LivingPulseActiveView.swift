import SwiftUI

/// Shared Living Pulse (G09–G12). Live APIs only — no invented bills or names.
struct LivingPulseActiveView: View {
    let theme: LivingActiveTheme
    let refreshToken: UInt64
    let momentTitle: String?
    let momentId: String?
    var onAddExpense: () -> Void = {}
    var onOpenQuickAdd: () -> Void = {}
    var onViewSplits: () -> Void = {}
    var onOpenFinance: () -> Void = {}
    var onQuickAddKind: (LivingQuickAddKind) -> Void = { _ in }

    @State private var pulse: APIClient.GroupPulsePayload?
    @State private var finance: APIClient.GroupFinancePayload?
    @State private var activities: [APIClient.ActivityItemPayload] = []
    @State private var participants: [APIClient.GroupParticipantPayload] = []
    @State private var residents: [APIClient.GroupResidentPayload] = []
    @State private var title: String?
    @State private var loading = true
    @State private var error: String?

    var body: some View {
        Group {
            if loading && pulse == nil && finance == nil {
                ProgressView().tint(theme.accent)
            } else {
                content
            }
        }
        .background(theme.bg)
        .task(id: "\(refreshToken)-\(momentId ?? "")") { await load() }
    }

    private var content: some View {
        let total = finance?.totals?.first
        let currency = total?.currencyCode ?? "INR"
        let funded = LivingFinanceMath.fundedPercent(
            contributionTotal: total?.contributionTotal,
            budgetTotal: total?.budgetTotal
        )
        let residentCount = !residents.isEmpty
            ? residents.count
            : (pulse?.payload?.participantCount ?? participants.count)
        let openTasks = pulse?.payload?.openTaskCount ?? 0
        let momentsValue: String = {
            if !activities.isEmpty { return "\(activities.count)" }
            if openTasks > 0 { return "\(openTasks)" }
            return "—"
        }()
        let positions = finance?.positions ?? []
        let displayTitle = momentTitle ?? title ?? theme.pulseTitle
        let gradients = theme.statGradients
        let g0 = gradients.indices.contains(0) ? gradients[0] : [theme.accent, theme.accentSolid]
        let g1 = gradients.indices.contains(1) ? gradients[1] : [theme.accentLight, theme.accent]
        let g2 = gradients.indices.contains(2) ? gradients[2] : [theme.accent, theme.accentSolid]
        let g3 = gradients.indices.contains(3) ? gradients[3] : [theme.accentLight, theme.accent]
        let upcomingBills = LivingFinanceMath.upcomingBills(from: activities)

        return ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                if let error {
                    Text(error).font(.caption).foregroundStyle(Color(hex: "#F87171"))
                }

                Text(displayTitle)
                    .font(.plusJakarta(size: 20, weight: .heavy))
                    .foregroundStyle(theme.text)

                HStack(spacing: 10) {
                    ForEach(theme.quickChips, id: \.label) { chip in
                        LivingEmojiChip(
                            theme: theme,
                            emoji: chip.emoji,
                            label: chip.label,
                            enabled: true
                        ) {
                            onQuickAddKind(chip.kind)
                        }
                    }
                }

                LivingTotalCollectedCard(
                    theme: theme,
                    contributionTotal: total?.contributionTotal,
                    budgetTotal: total?.budgetTotal,
                    currency: currency,
                    funded: funded
                )

                VStack(spacing: 10) {
                    HStack(spacing: 10) {
                        LivingStatCard(
                            label: "RESIDENTS",
                            value: residentCount > 0 ? "\(residentCount)" : "—",
                            colors: g0,
                            icon: "person.2.fill"
                        )
                        LivingStatCard(
                            label: "COLLECTED",
                            value: funded.map { "\($0)%" } ?? "—",
                            colors: g1,
                            icon: "clock.fill"
                        )
                    }
                    HStack(spacing: 10) {
                        LivingStatCard(
                            label: "RENT/MO",
                            value: GroupFinanceFormat.compactMoney(total?.budgetTotal, currencyCode: currency),
                            colors: g2,
                            icon: "creditcard.fill"
                        )
                        LivingStatCard(
                            label: "MOMENTS",
                            value: momentsValue,
                            colors: g3,
                            icon: "sparkles"
                        )
                    }
                }

                LivingSectionCard(theme: theme, title: theme.contributionsTitle) {
                    contributionSection(positions: positions, currency: currency)
                }

                LivingSectionCard(theme: theme, title: "Upcoming Bills") {
                    if upcomingBills.isEmpty {
                        LivingEmptyBlock(
                            theme: theme,
                            message: "No upcoming bills",
                            detail: "Bills appear from live activity — nothing is invented."
                        )
                    } else {
                        ForEach(upcomingBills) { item in
                            HStack(spacing: 10) {
                                Circle()
                                    .fill(theme.accent)
                                    .frame(width: 8, height: 8)
                                VStack(alignment: .leading, spacing: 2) {
                                    Text(item.title)
                                        .font(.plusJakarta(size: 13, weight: .medium))
                                        .foregroundStyle(theme.text)
                                    Text(item.occurredAt)
                                        .font(.plusJakarta(size: 11))
                                        .foregroundStyle(theme.secondary)
                                }
                                Spacer()
                            }
                            .padding(12)
                            .background(theme.bg)
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                            .overlay(RoundedRectangle(cornerRadius: 12).stroke(theme.border))
                        }
                    }
                }

                LivingSectionCard(theme: theme, title: "Recent Activity") {
                    if activities.isEmpty {
                        LivingEmptyBlock(
                            theme: theme,
                            message: "No recent activity",
                            detail: "Expenses, contributions, and updates will show here."
                        )
                    } else {
                        ForEach(activities) { item in
                            HStack(spacing: 10) {
                                Circle()
                                    .fill(theme.accent)
                                    .frame(width: 8, height: 8)
                                VStack(alignment: .leading, spacing: 2) {
                                    Text(item.title)
                                        .font(.plusJakarta(size: 13, weight: .medium))
                                        .foregroundStyle(theme.text)
                                    Text(item.occurredAt)
                                        .font(.plusJakarta(size: 11))
                                        .foregroundStyle(theme.secondary)
                                }
                                Spacer()
                                Image(systemName: "chevron.right")
                                    .font(.system(size: 11, weight: .semibold))
                                    .foregroundStyle(theme.secondary)
                            }
                            .padding(12)
                            .background(theme.bg)
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                            .overlay(RoundedRectangle(cornerRadius: 12).stroke(theme.border))
                        }
                    }
                }

                LivingSectionCard(theme: theme, title: theme.budgetTitle, trailing: {
                    Button("View Splits →", action: onViewSplits)
                        .font(.plusJakarta(size: 12, weight: .semibold))
                        .foregroundStyle(theme.accentLight)
                }) {
                    if total != nil {
                        HStack {
                            Text("Collected")
                                .font(.plusJakarta(size: 13, weight: .semibold))
                                .foregroundStyle(theme.text)
                            Spacer()
                            Text(GroupFinanceFormat.formatMoney(total?.contributionTotal, currencyCode: currency))
                                .font(.plusJakarta(size: 16, weight: .bold))
                                .foregroundStyle(theme.accentLight)
                        }
                        HStack {
                            Text("Budget")
                                .font(.plusJakarta(size: 12))
                                .foregroundStyle(theme.secondary)
                            Spacer()
                            Text(GroupFinanceFormat.formatMoney(total?.budgetTotal, currencyCode: currency))
                                .font(.plusJakarta(size: 14, weight: .semibold))
                                .foregroundStyle(theme.text)
                        }
                        Button("Open \(theme.financeTitle)", action: onOpenFinance)
                            .font(.plusJakarta(size: 13, weight: .semibold))
                            .foregroundStyle(theme.accent)
                            .padding(.top, 8)
                    } else {
                        LivingEmptyBlock(
                            theme: theme,
                            message: "No finance totals yet",
                            detail: "Add a contribution or budget to see live totals."
                        )
                    }
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
            .padding(.bottom, 56)
        }
    }

    @ViewBuilder
    private func contributionSection(
        positions: [APIClient.GroupFinancePositionPayload],
        currency: String
    ) -> some View {
        if !positions.isEmpty {
            let maxContribution = positions
                .map { GroupFinanceFormat.parseAmount($0.contributionTotal ?? $0.paidTotal) }
                .map { ($0 as NSDecimalNumber).doubleValue }
                .max() ?? 0
            VStack(alignment: .leading, spacing: 12) {
                ForEach(Array(positions.prefix(8).enumerated()), id: \.element.id) { idx, pos in
                    let name = participants.first(where: { $0.participantId == pos.participantId })?.displayName
                        ?? String(pos.participantId.prefix(8)) + "…"
                    let amountRaw = pos.contributionTotal ?? pos.paidTotal
                    let amount = GroupFinanceFormat.parseAmount(amountRaw)
                    let amountLabel: String = {
                        if amountRaw == nil || amount <= 0 { return "—" }
                        return GroupFinanceFormat.formatMoney(amountRaw, currencyCode: pos.currencyCode)
                    }()
                    let pct: Int = {
                        guard maxContribution > 0 else { return 0 }
                        return min(100, Int((((amount as NSDecimalNumber).doubleValue / maxContribution) * 100).rounded()))
                    }()
                    LivingCrewRow(
                        theme: theme,
                        name: name,
                        amountLabel: amountLabel,
                        percent: pct,
                        featured: idx == 0 && amount > 0
                    )
                }
            }
        } else if !participants.isEmpty {
            VStack(alignment: .leading, spacing: 12) {
                ForEach(Array(participants.prefix(8).enumerated()), id: \.element.id) { idx, p in
                    LivingCrewRow(
                        theme: theme,
                        name: p.displayName ?? String(p.participantId.prefix(8)),
                        amountLabel: "—",
                        percent: 0,
                        featured: idx == 0
                    )
                }
            }
        } else {
            LivingEmptyBlock(
                theme: theme,
                message: "No contributions yet",
                detail: "Invite residents or record a contribution — nothing is invented."
            )
        }
    }

    private func load() async {
        guard let momentId else { loading = false; return }
        error = nil
        if let cached = GroupTabDataCache.peekPulse(momentId) {
            title = cached.title
            pulse = cached.pulse
            finance = cached.finance
            activities = cached.activities
            loading = false
        } else {
            loading = true
        }
        do {
            async let pulseResult = APIClient.shared.getGroupPulse(momentId: momentId)
            async let financeResult = APIClient.shared.getGroupFinance(momentId: momentId)
            async let activityResult = APIClient.shared.listGroupActivity(momentId: momentId, limit: 5)
            async let participantsResult = APIClient.shared.listGroupParticipants(momentId: momentId)
            async let residentsResult = APIClient.shared.listResidents(momentId: momentId)
            let loadedPulse = try await pulseResult
            let finFacet = try await financeResult
            let loadedActivity = try await activityResult
            let loadedParticipants = (try? await participantsResult) ?? []
            let loadedResidents = (try? await residentsResult)?.items ?? []
            let loadedFinance = finFacet.payload ?? loadedPulse.payload?.finance
            title = loadedPulse.title
            pulse = loadedPulse
            finance = loadedFinance
            activities = loadedActivity
            participants = loadedParticipants
            residents = loadedResidents
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

// MARK: - Total Collected

private struct LivingTotalCollectedCard: View {
    let theme: LivingActiveTheme
    let contributionTotal: String?
    let budgetTotal: String?
    let currency: String
    let funded: Int?

    private var collectedLabel: String {
        let left = contributionTotal == nil
            ? "—"
            : GroupFinanceFormat.formatMoney(contributionTotal, currencyCode: currency)
        let right = budgetTotal == nil
            ? "—"
            : GroupFinanceFormat.formatMoney(budgetTotal, currencyCode: currency)
        return "\(left) / \(right)"
    }

    private var chipLabel: String {
        funded.map { "\($0)% FUNDED" } ?? "— FUNDED"
    }

    private var ringLabel: String {
        funded.map { "\($0)%" } ?? "—"
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 6) {
                    Text("TOTAL COLLECTED")
                        .font(.plusJakarta(size: 11, weight: .semibold))
                        .foregroundStyle(theme.secondary)
                    Text(collectedLabel)
                        .font(.plusJakarta(size: 24, weight: .heavy))
                        .foregroundStyle(theme.text)
                }
                Spacer()
                Text(chipLabel)
                    .font(.plusJakarta(size: 11, weight: .bold))
                    .foregroundStyle(Color.white)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 6)
                    .background(theme.accent)
                    .clipShape(Capsule())
            }

            HStack {
                Spacer(minLength: 0)
                LivingAccentRing(
                    percent: funded ?? 0,
                    centerLabel: ringLabel,
                    centerSub: funded == nil ? "" : "FUNDED",
                    accent: theme.accent
                )
                Spacer(minLength: 0)
            }
        }
        .padding(20)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(theme.card)
        .overlay(RoundedRectangle(cornerRadius: 20).stroke(theme.border))
        .clipShape(RoundedRectangle(cornerRadius: 20))
    }
}

private struct LivingAccentRing: View {
    let percent: Int
    let centerLabel: String
    let centerSub: String
    let accent: Color

    var body: some View {
        ZStack {
            Circle()
                .stroke(Color(hex: "#2A2538"), lineWidth: 12)
            Circle()
                .trim(from: 0, to: CGFloat(min(max(percent, 0), 100)) / 100)
                .stroke(accent, style: StrokeStyle(lineWidth: 12, lineCap: .round))
                .rotationEffect(.degrees(-90))
            VStack(spacing: 2) {
                Text(centerLabel)
                    .font(.plusJakarta(size: 28, weight: .heavy))
                    .foregroundStyle(Color(hex: "#E5E2E1"))
                if !centerSub.isEmpty {
                    Text(centerSub)
                        .font(.plusJakarta(size: 12, weight: .semibold))
                        .foregroundStyle(Color(hex: "#9CA3AF"))
                }
            }
        }
        .frame(width: 180, height: 180)
    }
}

enum LivingFinanceMath {
    static func fundedPercent(contributionTotal: String?, budgetTotal: String?) -> Int? {
        guard budgetTotal != nil, contributionTotal != nil else { return nil }
        let budget = GroupFinanceFormat.parseAmount(budgetTotal)
        guard budget > 0 else { return nil }
        let contribution = GroupFinanceFormat.parseAmount(contributionTotal)
        let pct = (contribution as NSDecimalNumber).doubleValue / (budget as NSDecimalNumber).doubleValue * 100
        return min(100, Int(pct.rounded()))
    }

    /// Only surface activity items that look like bills/rent — never invent entries.
    static func upcomingBills(from activities: [APIClient.ActivityItemPayload]) -> [APIClient.ActivityItemPayload] {
        let keywords = ["bill", "rent", "utility", "utilities", "due", "invoice"]
        return activities.filter { item in
            let hay = (item.title + " " + item.activityCode).lowercased()
            return keywords.contains { hay.contains($0) }
        }
    }
}

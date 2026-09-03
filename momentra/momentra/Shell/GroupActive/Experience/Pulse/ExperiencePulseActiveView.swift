import SwiftUI

/// Figma 584:15671 (House Party) / 584:16389 (Office Outing) — Pulse. Live APIs only.
struct ExperiencePulseActiveView: View {
    let theme: ExperienceActiveTheme
    let refreshToken: UInt64
    let momentTitle: String?
    let momentId: String?
    var momentTypeCode: String? = nil
    var onAddExpense: () -> Void = {}
    var onOpenQuickAdd: () -> Void = {}
    var onViewSplits: () -> Void = {}
    var onOpenFinance: () -> Void = {}
    var onQuickAddKind: (ExperienceQuickAddKind) -> Void = { _ in }

    @State private var pulse: APIClient.GroupPulsePayload?
    @State private var finance: APIClient.GroupFinancePayload?
    @State private var activities: [APIClient.ActivityItemPayload] = []
    @State private var participants: [APIClient.GroupParticipantPayload] = []
    @State private var insights: [AnalyticsInsightItemPayload] = []
    @State private var title: String?
    @State private var loading = true
    @State private var error: String?
    @State private var editingExpenseId: String?
    @State private var editExpensePresented = false

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
        .sheet(isPresented: $editExpensePresented) {
            if let momentId, let editingExpenseId {
                GroupExpenseSheet(
                    momentId: momentId,
                    isPresented: $editExpensePresented,
                    expenseId: editingExpenseId,
                    momentTypeCode: momentTypeCode,
                    onSaved: {
                        editExpensePresented = false
                        Task { await load() }
                    },
                    onDeleted: {
                        editExpensePresented = false
                        Task { await load() }
                    }
                )
            }
        }
        .onChange(of: editExpensePresented) { _, open in
            if !open { editingExpenseId = nil }
        }
    }

    @ViewBuilder
    private var content: some View {
        let total = finance?.totals?.first
        let currency = total?.currencyCode ?? "INR"
        let utilization = GroupFinanceFormat.utilizationPercent(
            expenseTotal: total?.expenseTotal,
            budgetTotal: total?.budgetTotal
        )
        let people = pulse?.payload?.participantCount ?? participants.count
        let attentionCount = pulse?.payload?.attentionCount ?? 0
        let openTasks = pulse?.payload?.openTaskCount ?? 0
        let positions = finance?.positions ?? []
        let viewer = finance?.viewerPosition
        let displayTitle = momentTitle ?? title ?? theme.pulseTitle
        NativeDashboardScaffold(background: theme.bg) {

            NativeListSection {

            VStack(alignment: .leading, spacing: 14) {
                if let error {
                    Text(error).font(.caption).foregroundStyle(Color(hex: "#F87171"))
                }

                ExperiencePulseHero(
                    theme: theme,
                    title: displayTitle,
                    startAtIso: ExperiencePulseDate.startAtIso(from: pulse)
                )

                HStack(spacing: 10) {
                    ForEach(theme.quickChips, id: \.label) { chip in
                        ExperienceEmojiChip(
                            theme: theme,
                            emoji: chip.emoji,
                            label: chip.label,
                            enabled: true
                        ) {
                            onQuickAddKind(chip.kind)
                        }
                    }
                }

                ExperiencePulseHealthCard(
                    theme: theme,
                    utilization: total?.budgetTotal != nil ? utilization : nil,
                    activityCount: activities.count,
                    openTasks: openTasks,
                    people: people
                )

                ExperienceSectionCard(theme: theme, title: "Needs Attention", trailing: {
                    if attentionCount > 0 {
                        Text("\(attentionCount)")
                            .font(.plusJakarta(size: 11, weight: .bold))
                            .foregroundStyle(.white)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 4)
                            .background(theme.accent)
                            .clipShape(Capsule())
                    }
                }) {
                    if attentionCount > 0 {
                        Text("\(attentionCount) items flagged")
                            .font(.plusJakarta(size: 12))
                            .foregroundStyle(theme.secondary)
                    } else {
                        ExperienceEmptyBlock(
                            theme: theme,
                            message: "All clear for now",
                            detail: "Attention items appear when the backend exposes them."
                        )
                    }
                }

                ExperienceSectionCard(theme: theme, title: theme.progressTitle) {
                    VStack(alignment: .leading, spacing: 12) {
                        if total?.budgetTotal != nil {
                            Text("\(utilization)% of budget used")
                                .font(.plusJakarta(size: 13, weight: .semibold))
                                .foregroundStyle(theme.text)
                            GroupProgressBar(percent: utilization)
                        } else {
                            ExperienceEmptyBlock(
                                theme: theme,
                                message: "No budget yet",
                                detail: "Set a budget or add expenses to track progress."
                            )
                        }
                        HStack(spacing: 8) {
                            ExperienceStatCard(
                                label: "TASKS",
                                value: "\(openTasks)",
                                colors: theme.statGradients[0]
                            )
                            ExperienceStatCard(
                                label: "BUDGET",
                                value: GroupFinanceFormat.compactMoney(total?.budgetTotal, currencyCode: currency),
                                colors: theme.statGradients[1]
                            )
                            ExperienceStatCard(
                                label: "PEOPLE",
                                value: "\(people)",
                                colors: theme.statGradients[2]
                            )
                        }
                    }
                }

                ExperienceSectionCard(theme: theme, title: theme.crewTitle) {
                    if participants.isEmpty && positions.isEmpty {
                        ExperienceEmptyBlock(
                            theme: theme,
                            message: "No participation data yet",
                            detail: "Invite people or record shared expenses."
                        )
                    } else if !participants.isEmpty {
                        VStack(alignment: .leading, spacing: 12) {
                            ForEach(Array(participants.prefix(5).enumerated()), id: \.element.id) { idx, p in
                                ExperienceCrewRow(
                                    theme: theme,
                                    name: p.displayName ?? String(p.participantId.prefix(8)),
                                    role: p.roleCode ?? "Member",
                                    percent: max(20, 90 - idx * 10),
                                    featured: idx == 0
                                )
                            }
                        }
                    } else {
                        VStack(alignment: .leading, spacing: 8) {
                            ForEach(Array(positions.prefix(5))) { pos in
                                HStack {
                                    Text(String(pos.participantId.prefix(8)) + "…")
                                        .font(.plusJakarta(size: 13, weight: .semibold))
                                        .foregroundStyle(theme.text)
                                    Spacer()
                                    Text(GroupFinanceFormat.formatMoney(pos.netPosition, currencyCode: pos.currencyCode))
                                        .font(.plusJakarta(size: 13, weight: .semibold))
                                        .foregroundStyle(
                                            GroupFinanceFormat.parseAmount(pos.netPosition) >= 0
                                                ? Color(hex: "#4ADE80")
                                                : Color(hex: "#FF7A3D")
                                        )
                                }}
                        }
                    }
                }

                ExperienceSectionCard(theme: theme, title: theme.budgetTitle, trailing: {
                    Button("View Splits →", action: onViewSplits)
                        .font(.plusJakarta(size: 12, weight: .semibold))
                        .foregroundStyle(theme.accentLight)
                }) {
                    if let total {
                        if let viewer, let net = viewer.netPosition {
                            VStack(alignment: .leading, spacing: 8) {
                                Text("Your Balance")
                                    .font(.plusJakarta(size: 12))
                                    .foregroundStyle(Color.white.opacity(0.9))
                                Text(balanceLabel(net: net, currency: viewer.currencyCode))
                                    .font(.plusJakarta(size: 20, weight: .heavy))
                                    .foregroundStyle(Color.white)
                            }
                            .padding(16)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .background(theme.accent)
                            .clipShape(RoundedRectangle(cornerRadius: 14))
                        }
                        HStack {
                            Text("Total pool")
                                .font(.plusJakarta(size: 13, weight: .semibold))
                                .foregroundStyle(theme.text)
                            Spacer()
                            Text(GroupFinanceFormat.formatMoney(total.budgetTotal ?? total.expenseTotal, currencyCode: currency))
                                .font(.plusJakarta(size: 16, weight: .bold))
                                .foregroundStyle(theme.accentLight)
                        }
                        .padding(.top, 8)
                        HStack {
                            Text("Spent")
                                .font(.plusJakarta(size: 12))
                                .foregroundStyle(theme.secondary)
                            Spacer()
                            Text(GroupFinanceFormat.formatMoney(total.expenseTotal, currencyCode: currency))
                                .font(.plusJakarta(size: 14, weight: .semibold))
                                .foregroundStyle(theme.text)
                        }
                        Button("Open Group Finance", action: onOpenFinance)
                            .font(.plusJakarta(size: 13, weight: .semibold))
                            .foregroundStyle(theme.accent)
                            .padding(.top, 8)
                    } else {
                        ExperienceEmptyBlock(
                            theme: theme,
                            message: "No finance totals yet",
                            detail: "Add an expense to see settlement and budget data."
                        )
                    }
                }

                ExperienceSectionCard(theme: theme, title: "Recent Activity") {
                    if activities.isEmpty {
                        ExperienceEmptyBlock(
                            theme: theme,
                            message: "No recent activity",
                            detail: "Expenses, plans, and updates will show here."
                        )
                    } else {
                        ForEach(activities) { item in
                            let expenseId = item.activityPayload?.expenseId
                            let canEdit = PersonalActivityTimelineDerived.isExpense(item) && expenseId != nil
                            Button {
                                guard let expenseId else { return }
                                editingExpenseId = expenseId
                                editExpensePresented = true
                            } label: {
                                HStack(spacing: 10) {
                                    VStack(alignment: .leading, spacing: 2) {
                                        Text(item.title)
                                            .font(.plusJakarta(size: 13, weight: .medium))
                                            .foregroundStyle(theme.text)
                                        Text(item.occurredAt)
                                            .font(.plusJakarta(size: 11))
                                            .foregroundStyle(theme.secondary)
                                    }
                                    Spacer()
                                    if canEdit {
                                        Image(systemName: "chevron.right")
                                            .font(.system(size: 11, weight: .semibold))
                                            .foregroundStyle(theme.secondary)
                                    }
                                }
                            }
                            .buttonStyle(.plain)
                            .disabled(!canEdit)
                        }
                    }
                }

                GroupPulseInsightsHeroCard(
                    headerTitle: "🧠 \(theme.insightsTitle)",
                    insights: insights,
                    gradient: theme.heroGradient
                )
            }

            }

        }
    }

    private func balanceLabel(net: String, currency: String) -> String {
        let amount = GroupFinanceFormat.parseAmount(net)
        if amount < 0 {
            return "You owe \(GroupFinanceFormat.formatMoney(String(describing: -amount), currencyCode: currency))"
        }
        if amount > 0 {
            return "You are owed \(GroupFinanceFormat.formatMoney(net, currencyCode: currency))"
        }
        return "Settled up"
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
            async let insightsResult = APIClient.shared.listAnalyticsInsights(scopeType: "MOMENT", scopeId: momentId)
            let loadedPulse = try await pulseResult
            let finFacet = try await financeResult
            let loadedActivity = try await activityResult
            let loadedParticipants = (try? await participantsResult) ?? []
            let loadedFinance = finFacet.payload ?? loadedPulse.payload?.finance
            title = loadedPulse.title
            pulse = loadedPulse
            finance = loadedFinance
            activities = loadedActivity
            participants = loadedParticipants
            insights = (try? await insightsResult)?.items ?? []
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

// MARK: - Hero (Figma 584:15689)

private struct ExperiencePulseHero: View {
    let theme: ExperienceActiveTheme
    let title: String
    let startAtIso: String?

    private var dateLabel: String? {
        ExperiencePulseDate.displayDate(from: startAtIso)
    }

    private var countdownLabel: String? {
        ExperiencePulseDate.countdown(from: startAtIso, emoji: theme.heroEmoji)
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(spacing: 8) {
                glassChip("\(theme.heroEmoji) \(title)")
                if let dateLabel {
                    glassChip("\(theme.typeLabel) • \(dateLabel)")
                } else {
                    glassChip(theme.typeLabel)
                }
            }
            Text(title)
                .font(.plusJakarta(size: 32, weight: .heavy))
                .foregroundStyle(Color.white)
            if let dateLabel {
                HStack(spacing: 8) {
                    Text(dateLabel)
                        .font(.plusJakarta(size: 14, weight: .medium))
                        .foregroundStyle(Color.white.opacity(0.8))
                    Text(theme.heroEmoji)
                        .font(.plusJakarta(size: 11, weight: .bold))
                        .foregroundStyle(theme.darkText)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 4)
                        .background(theme.accent)
                        .clipShape(Capsule())
                }
            }
            if let countdownLabel {
                Text(countdownLabel)
                    .font(.plusJakarta(size: 12, weight: .bold))
                    .foregroundStyle(Color.white)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 4)
                    .background(Color.white.opacity(0.1))
                    .overlay(Capsule().stroke(Color.white.opacity(0.2)))
                    .clipShape(Capsule())
            }
        }
        .padding(20)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(theme.pulseHeroGradient)
        .clipShape(RoundedRectangle(cornerRadius: 24))
    }

    private func glassChip(_ text: String) -> some View {
        Text(text)
            .font(.plusJakarta(size: 12, weight: .bold))
            .foregroundStyle(Color.white)
            .padding(.horizontal, 10)
            .padding(.vertical, 6)
            .background(Color.white.opacity(0.1))
            .overlay(Capsule().stroke(Color.white.opacity(0.2)))
            .clipShape(Capsule())
    }
}

// MARK: - Health card (Figma 584:15716)

private struct ExperiencePulseHealthCard: View {
    let theme: ExperienceActiveTheme
    let utilization: Int?
    let activityCount: Int
    let openTasks: Int
    let people: Int

    private var ringPercent: Int { utilization ?? 0 }
    private var ringLabel: String { utilization.map(String.init) ?? "—" }
    private var activityChip: String {
        if let utilization { return "🎯 \(utilization)% Activity" }
        return "🎯 — Activity"
    }
    private var repliesChip: String {
        if activityCount > 0 { return "📨 \(activityCount) Updates" }
        if people > 0 { return "📨 \(people) People" }
        return "📨 —"
    }
    private var trendChip: String {
        if openTasks > 0 { return "⚡ \(openTasks) Open" }
        return "⚡ —"
    }
    private var subtitle: String {
        if utilization != nil || people > 0 || activityCount > 0 {
            return "\(theme.typeLabel) coordination is moving with live activity."
        }
        return "Health metrics appear when budget and activity data are live."
    }
    private var healthDetail: String {
        if utilization != nil {
            return "Coordination is moving smoothly. Keep the crew aligned!"
        }
        return "No invented score — ring fills when budget utilization is available."
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            VStack(alignment: .leading, spacing: 4) {
                Text("\(theme.heroEmoji) \(theme.pulseTitle)")
                    .font(.plusJakarta(size: 18, weight: .heavy))
                    .foregroundStyle(theme.text)
                Text(subtitle)
                    .font(.plusJakarta(size: 14, weight: .medium))
                    .foregroundStyle(theme.secondary)
            }
            HStack(spacing: 8) {
                metricChip(activityChip)
                metricChip(repliesChip)
                metricChip(trendChip)
            }
            HStack(alignment: .center, spacing: 24) {
                ExperienceAccentRing(
                    percent: ringPercent,
                    centerLabel: ringLabel,
                    centerSub: "/ 100",
                    accent: theme.accent
                )
                VStack(alignment: .leading, spacing: 8) {
                    Text(theme.healthLabel)
                        .font(.plusJakarta(size: 14, weight: .semibold))
                        .foregroundStyle(theme.text)
                    Text(healthDetail)
                        .font(.plusJakarta(size: 12))
                        .foregroundStyle(theme.secondary)
                    Text(utilization != nil ? "Updated from live finance" : "Waiting on live metrics")
                        .font(.plusJakarta(size: 10, weight: .semibold))
                        .foregroundStyle(theme.secondary)
                }
                Spacer(minLength: 0)
            }
        }
        .padding(20)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(theme.card)
        .overlay(RoundedRectangle(cornerRadius: 20).stroke(theme.border))
        .clipShape(RoundedRectangle(cornerRadius: 20))
    }

    private func metricChip(_ text: String) -> some View {
        Text(text)
            .font(.plusJakarta(size: 12, weight: .bold))
            .foregroundStyle(theme.accent)
            .padding(.horizontal, 10)
            .padding(.vertical, 6)
            .background(Color.white.opacity(0.05))
            .overlay(Capsule().stroke(Color.white.opacity(0.1)))
            .clipShape(Capsule())
    }
}

private struct ExperienceAccentRing: View {
    let percent: Int
    let centerLabel: String
    let centerSub: String
    let accent: Color

    var body: some View {
        ZStack {
            Circle()
                .stroke(Color(hex: "#2A2624"), lineWidth: 10)
            Circle()
                .trim(from: 0, to: CGFloat(min(max(percent, 0), 100)) / 100)
                .stroke(accent, style: StrokeStyle(lineWidth: 10, lineCap: .round))
                .rotationEffect(.degrees(-90))
            VStack(spacing: 2) {
                Text(centerLabel)
                    .font(.plusJakarta(size: 28, weight: .heavy))
                    .foregroundStyle(Color(hex: "#E5E0EE"))
                Text(centerSub)
                    .font(.plusJakarta(size: 12, weight: .semibold))
                    .foregroundStyle(Color(hex: "#C9C4D8"))
            }
        }
        .frame(width: 120, height: 120)
    }
}

private enum ExperiencePulseDate {
    static func startAtIso(from pulse: APIClient.GroupPulsePayload?) -> String? {
        guard let widget = pulse?.payload?.widgetPayload else { return nil }
        for key in ["startAt", "start_at", "eventAt", "eventDate", "scheduledAt"] {
            if let raw = widget[key]?.value as? String, !raw.isEmpty { return raw }
        }
        return nil
    }

    static func displayDate(from iso: String?) -> String? {
        guard let date = parse(iso) else { return nil }
        let f = DateFormatter()
        f.locale = Locale(identifier: "en_GB")
        f.dateFormat = "d MMM yyyy"
        return f.string(from: date)
    }

    static func countdown(from iso: String?, emoji: String) -> String? {
        guard let date = parse(iso) else { return nil }
        let days = Calendar.current.dateComponents([.day], from: Calendar.current.startOfDay(for: Date()), to: Calendar.current.startOfDay(for: date)).day ?? 0
        if days < 0 { return "\(emoji) Happened \(-days) day\(-days == 1 ? "" : "s") ago" }
        if days == 0 { return "\(emoji) Today!" }
        if days < 7 { return "\(emoji) \(days) day\(days == 1 ? "" : "s") away!" }
        let weeks = days / 7
        if days < 60 { return "\(emoji) \(weeks) week\(weeks == 1 ? "" : "s") away!" }
        let months = days / 30
        return "\(emoji) \(max(months, 1)) month\(months == 1 ? "" : "s") away!"
    }

    private static func parse(_ iso: String?) -> Date? {
        guard let iso, !iso.isEmpty else { return nil }
        let isoFrac = ISO8601DateFormatter()
        isoFrac.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        if let d = isoFrac.date(from: iso) { return d }
        let isoBasic = ISO8601DateFormatter()
        isoBasic.formatOptions = [.withInternetDateTime]
        if let d = isoBasic.date(from: iso) { return d }
        let day = DateFormatter()
        day.locale = Locale(identifier: "en_US_POSIX")
        day.dateFormat = "yyyy-MM-dd"
        return day.date(from: String(iso.prefix(10)))
    }
}

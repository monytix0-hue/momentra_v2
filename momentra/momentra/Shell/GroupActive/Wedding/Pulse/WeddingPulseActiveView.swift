import SwiftUI

/// Figma 575:14939 — Wedding Pulse. Live APIs only; no demo seeds.
struct WeddingPulseActiveView: View {
    let refreshToken: UInt64
    let momentTitle: String?
    let momentId: String?
    var momentTypeCode: String? = nil
    var onAddExpense: () -> Void = {}
    var onOpenQuickAdd: () -> Void = {}
    var onViewSplits: () -> Void = {}
    var onOpenFinance: () -> Void = {}
    var onQuickAddKind: (WeddingQuickAddKind) -> Void = { _ in }
    var onViewAllActivity: () -> Void = {}

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
                ProgressView().tint(WeddingActiveTheme.accent)
            } else {
                content
            }
        }
        .background(WeddingActiveTheme.bg)
        .task(id: "\(refreshToken)-\(momentId ?? "")") { await load() }
        .sheet(isPresented: $editExpensePresented) {
            if let momentId, let editingExpenseId {
                GroupExpenseSheet(
                    momentId: momentId,
                    isPresented: $editExpensePresented,
                    expenseId: editingExpenseId,
                    isWedding: true,
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
        let expenseCount = finance?.expenseCount ?? 0
        let attentionCount = pulse?.payload?.attentionCount ?? 0
        let openTasks = pulse?.payload?.openTaskCount ?? 0
        let positions = finance?.positions ?? []
        let nameById = GroupParticipantNameMap.build(participants)
        NativeDashboardScaffold(background: WeddingActiveTheme.bg) {

            NativeListSection {

            VStack(alignment: .leading, spacing: 14) {
                if let error {
                    Text(error).font(.caption).foregroundStyle(Color(hex: "#F87171"))
                }

                VStack(alignment: .leading, spacing: 10) {
                    Text("Wedding")
                        .font(.plusJakarta(size: 10, weight: .bold))
                        .foregroundStyle(WeddingActiveTheme.darkText)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 5)
                        .background(Color.white.opacity(0.95))
                        .clipShape(Capsule())
                    Text(momentTitle ?? title ?? "Wedding Pulse")
                        .font(.plusJakarta(size: 24, weight: .heavy))
                        .foregroundStyle(WeddingActiveTheme.darkText)
                    Text("\(people) people · \(expenseCount) expenses")
                        .font(.plusJakarta(size: 13))
                        .foregroundStyle(WeddingActiveTheme.darkText.opacity(0.85))
                }
                .padding(20)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(WeddingActiveTheme.heroGradient)
                .clipShape(RoundedRectangle(cornerRadius: 20))

                HStack(spacing: 10) {
                    WeddingEmojiChip(emoji: "📷", label: "Photos", enabled: true) {
                        onQuickAddKind(.memory)
                    }
                    WeddingEmojiChip(emoji: "✉️", label: "RSVPs", enabled: true) {
                        onQuickAddKind(.attendance)
                    }
                    WeddingEmojiChip(emoji: "📋", label: "Registry", enabled: true) {
                        onQuickAddKind(.expense)
                    }
                    WeddingEmojiChip(emoji: "🎁", label: "Gifts", enabled: true) {
                        onQuickAddKind(.contribution)
                    }
                }

                WeddingSectionCard(title: "Wedding Health") {
                    HStack(alignment: .top, spacing: 14) {
                        VStack(alignment: .leading, spacing: 8) {
                            Text("Wedding Health")
                                .font(.plusJakarta(size: 14, weight: .bold))
                                .foregroundStyle(WeddingActiveTheme.text)
                            WeddingEmptyBlock(
                                message: "Score not available yet",
                                detail: "Health scoring is coming soon — no invented numbers."
                            )
                        }
                        Spacer()
                        GroupProgressRing(percent: 0, centerLabel: "—", centerSub: "Soon")
                    }
                }

                WeddingSectionCard(title: "Needs Attention", trailing: {
                    if attentionCount > 0 {
                        Text("\(attentionCount)")
                            .font(.plusJakarta(size: 11, weight: .bold))
                            .foregroundStyle(.white)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 4)
                            .background(WeddingActiveTheme.accent)
                            .clipShape(Capsule())
                    }
                }) {
                    if attentionCount > 0 {
                        Text("\(attentionCount) items flagged")
                            .font(.plusJakarta(size: 12))
                            .foregroundStyle(WeddingActiveTheme.secondary)
                    } else {
                        WeddingEmptyBlock(
                            message: "All clear for now",
                            detail: "Attention items appear when the backend exposes them."
                        )
                    }
                }

                WeddingSectionCard(title: "Wedding Progress") {
                    VStack(alignment: .leading, spacing: 12) {
                        if total?.budgetTotal != nil {
                            Text("\(utilization)% of budget used")
                                .font(.plusJakarta(size: 13, weight: .semibold))
                                .foregroundStyle(WeddingActiveTheme.text)
                            GroupProgressBar(percent: utilization)
                        } else {
                            WeddingEmptyBlock(
                                message: "No budget yet",
                                detail: "Set a budget or add expenses to track progress."
                            )
                        }
                        HStack(spacing: 8) {
                            WeddingStatCard(
                                label: "TASKS",
                                value: "\(openTasks)",
                                colors: [Color(hex: "#A62E66"), Color(hex: "#6B1A40")]
                            )
                            WeddingStatCard(
                                label: "BUDGET",
                                value: GroupFinanceFormat.compactMoney(total?.budgetTotal, currencyCode: currency),
                                colors: [Color(hex: "#8C1F59"), Color(hex: "#591438")]
                            )
                            WeddingStatCard(
                                label: "PEOPLE",
                                value: "\(people)",
                                colors: [Color(hex: "#992673"), Color(hex: "#661A4D")]
                            )
                        }
                    }
                }

                WeddingSectionCard(title: "Wedding Party") {
                    if positions.isEmpty {
                        WeddingEmptyBlock(
                            message: "No participation data yet",
                            detail: "Positions appear after shared expenses are recorded."
                        )
                    } else {
                        VStack(alignment: .leading, spacing: 8) {
                            ForEach(Array(positions.prefix(5))) { pos in
                                HStack {
                                    Text(GroupParticipantNameMap.resolve(
                                        participantId: pos.participantId,
                                        positionDisplayName: pos.displayName,
                                        nameById: nameById
                                    ))
                                        .font(.plusJakarta(size: 13, weight: .semibold))
                                        .foregroundStyle(WeddingActiveTheme.text)
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

                WeddingSectionCard(title: "Wedding Budget") {
                    if let total {
                        HStack {
                            Text("Total pool")
                                .font(.plusJakarta(size: 13, weight: .semibold))
                                .foregroundStyle(WeddingActiveTheme.text)
                            Spacer()
                            Text(GroupFinanceFormat.formatMoney(total.budgetTotal, currencyCode: currency))
                                .font(.plusJakarta(size: 16, weight: .bold))
                                .foregroundStyle(WeddingActiveTheme.accentLight)
                        }
                        HStack {
                            Text("Spent")
                                .font(.plusJakarta(size: 12))
                                .foregroundStyle(WeddingActiveTheme.secondary)
                            Spacer()
                            Text(GroupFinanceFormat.formatMoney(total.expenseTotal, currencyCode: currency))
                                .font(.plusJakarta(size: 14, weight: .semibold))
                                .foregroundStyle(WeddingActiveTheme.text)
                        }
                        Button("View Splits", action: onViewSplits)
                            .font(.plusJakarta(size: 13, weight: .semibold))
                            .foregroundStyle(WeddingActiveTheme.accent)
                            .padding(.top, 8)
                    } else {
                        WeddingEmptyBlock(
                            message: "No finance totals yet",
                            detail: "Add an expense to see settlement and budget data."
                        )
                    }
                }

                WeddingSectionCard(title: "Recent Activity") {
                    if activities.isEmpty {
                        WeddingEmptyBlock(
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
                                            .foregroundStyle(WeddingActiveTheme.text)
                                        Text(item.occurredAt)
                                            .font(.plusJakarta(size: 11))
                                            .foregroundStyle(WeddingActiveTheme.secondary)
                                    }
                                    Spacer()
                                    if canEdit {
                                        Image(systemName: "chevron.right")
                                            .font(.system(size: 11, weight: .semibold))
                                            .foregroundStyle(WeddingActiveTheme.secondary)
                                    }
                                }
                            }
                            .buttonStyle(.plain)
                            .disabled(!canEdit)
                        }
                        Button(action: onViewAllActivity) {
                            Text("View all activity →")
                                .font(.plusJakarta(size: 13, weight: .semibold))
                                .foregroundStyle(WeddingActiveTheme.accent)
                                .frame(maxWidth: .infinity, alignment: .leading)
                                .padding(.top, 8)
                        }
                        .buttonStyle(.plain)
                    }
                }

                GroupPulseInsightsHeroCard(
                    headerTitle: "💡 Insights",
                    insights: insights,
                    gradient: WeddingActiveTheme.heroGradient,
                    titleColor: WeddingActiveTheme.darkText,
                    bodyColor: WeddingActiveTheme.darkText.opacity(0.85)
                )
            }

            }

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
            insights = cached.insights
            loading = false
        } else {
            loading = true
        }
        do {
            async let tabResult = GroupTabLoad.loadPulseTab(momentId: momentId)
            async let participantsResult = APIClient.shared.listGroupParticipants(momentId: momentId)
            let tab = try await tabResult
            title = tab.title
            pulse = tab.pulse
            finance = tab.finance
            activities = tab.activities
            insights = tab.insights
            loading = false
            do {
                participants = try await participantsResult
            } catch is CancellationError {
            } catch {
                if participants.isEmpty { participants = [] }
            }
            Task {
                if let enriched = await GroupTabLoad.enrich(momentId: momentId) {
                    await MainActor.run { insights = enriched.insights }
                }
            }
        } catch {
            self.error = error.localizedDescription
            loading = false
        }
    }
}

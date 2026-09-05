import SwiftUI

/// Living Moments (Flatmates / Co-living / Family / Custom). Live APIs only.
struct LivingMomentsActiveView: View {
    let theme: LivingActiveTheme
    let refreshToken: UInt64
    let momentId: String?
    let momentTitle: String?
    var momentTypeCode: String? = nil
    var onOpenQuickAdd: () -> Void = {}

    @State private var pulse: APIClient.GroupPulsePayload?
    @State private var finance: APIClient.GroupFinancePayload?
    @State private var life: APIClient.GroupLifePayload?
    @State private var listPlanning: [GroupPlanningItem] = []
    @State private var listBookings: [APIClient.GroupLifePayload.LifeInner.BookingItem] = []
    @State private var listUpdates: [GroupUpdateItem] = []
    @State private var listPolls: [APIClient.GroupPollItemPayload] = []
    @State private var listMemoryItems: [GroupMemoryItem] = []
    @State private var livingRules: [APIClient.GroupLivingRulePayload] = []
    @State private var residents: [APIClient.GroupResidentPayload] = []
    @State private var sharedAssets: [APIClient.GroupSharedAssetPayload] = []
    @State private var maintenance: [APIClient.GroupMaintenanceRecordPayload] = []
    @State private var listExpenses: [APIClient.GroupExpenseListItemPayload] = []
    @State private var memoryCount: Int = 0
    @State private var selectedPollId: String?
    @State private var pollsListOpen = false
    @State private var scheduleOpen = false
    @State private var title: String?
    @State private var loading = true
    @State private var error: String?

    private var chrome: MomentsChrome { .living(theme) }

    private enum LivingKind {
        case flatmates, coLiving, familyHousehold, customLiving
    }

    private var kind: LivingKind {
        switch theme.typeLabel {
        case LivingActiveTheme.coLiving.typeLabel: return .coLiving
        case LivingActiveTheme.familyHousehold.typeLabel: return .familyHousehold
        case LivingActiveTheme.customLiving.typeLabel: return .customLiving
        default: return .flatmates
        }
    }

    private var showUpcoming: Bool {
        kind == .flatmates || kind == .familyHousehold
    }

    private var planningItems: [GroupPlanningItem] {
        listPlanning.isEmpty ? (life?.payload?.planningItems ?? []) : listPlanning
    }

    private var bookings: [APIClient.GroupLifePayload.LifeInner.BookingItem] {
        listBookings.isEmpty ? (life?.payload?.bookings ?? []) : listBookings
    }

    private var updates: [GroupUpdateItem] {
        listUpdates.isEmpty ? (life?.payload?.updates ?? []) : listUpdates
    }

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
        .sheet(isPresented: $scheduleOpen) {
            PlanningScheduleSheet(
                items: planningItems,
                momentTypeCode: momentTypeCode,
                accent: theme.accent,
                surface: theme.card,
                field: theme.bg,
                border: theme.border,
                text: theme.text,
                muted: theme.secondary,
                onDismiss: { scheduleOpen = false }
            )
        }
        .sheet(isPresented: $pollsListOpen) {
            GroupPollsListSheet(
                momentTitle: momentTitle ?? title,
                chrome: .living(theme),
                polls: listPolls,
                onDismiss: { pollsListOpen = false },
                onChanged: { Task { await load() } }
            )
        }
        .sheet(item: Binding(
            get: { selectedPollId.map { PollSheetItem(id: $0) } },
            set: { selectedPollId = $0?.id }
        )) { item in
            PollDetailSheet(
                pollId: item.id,
                onDismiss: { selectedPollId = nil },
                onSaved: { Task { await load() } }
            )
        }
    }

    private struct PollSheetItem: Identifiable {
        let id: String
    }

    @ViewBuilder
    private var content: some View {
        let budgetTotal = finance?.totals?.first?.budgetTotal
        let contributionTotal = finance?.totals?.first?.contributionTotal
        let currency = finance?.totals?.first?.currencyCode ?? "INR"
        let peopleCount = !residents.isEmpty
            ? residents.count
            : (pulse?.payload?.participantCount ?? 0)
        let openTasks = pulse?.payload?.openTaskCount ?? life?.payload?.openTaskCount ?? 0
        let moments = memoryCount > 0 ? memoryCount : listMemoryItems.count
        let funded = LivingFinanceMath.fundedPercent(
            contributionTotal: contributionTotal,
            budgetTotal: budgetTotal
        )
        let displayTitle = momentTitle ?? title ?? "\(theme.typeLabel) Moments"
        let status = (pulse?.status ?? life?.status ?? "PLANNING").uppercased()
        let dayGroups = itineraryDayGroups(planningItems)
        let upcoming = momentsUpcomingFromPlanning(planning: planningItems, bookings: bookings, finance: finance)
        let gradients = theme.statGradients
        let g0 = gradients.indices.contains(0) ? gradients[0] : [theme.accent, theme.accentSolid]
        let g1 = gradients.indices.contains(1) ? gradients[1] : [theme.accentLight, theme.accent]
        let g2 = gradients.indices.contains(2) ? gradients[2] : [theme.accent, theme.accentSolid]
        let g3 = gradients.indices.contains(3) ? gradients[3] : [theme.accentLight, theme.accentSolid]
        let highlights = listMemoryItems.prefix(3).compactMap { $0.title }.filter { !$0.isEmpty }

        let heroStats: [(label: String, value: String, colors: [Color])] = {
            switch kind {
            case .flatmates:
                return [
                    ("RESIDENTS", "\(peopleCount)", g0),
                    ("COLLECTED", funded.map { "\($0)%" } ?? "—", g1),
                    ("RENT", GroupFinanceFormat.compactMoney(budgetTotal, currencyCode: currency), g2),
                    ("MOMENTS", "\(moments)", g3),
                ]
            case .coLiving:
                return [
                    ("COMMUNITY", "\(peopleCount)", g0),
                    ("COLLECTED", funded.map { "\($0)%" } ?? "—", g1),
                    ("BUDGET", GroupFinanceFormat.compactMoney(budgetTotal, currencyCode: currency), g2),
                    ("TASKS", "\(openTasks)", g3),
                ]
            case .familyHousehold:
                return [
                    ("PEOPLE", "\(peopleCount)", g0),
                    ("EVENTS", "\(planningItems.count)", g1),
                    ("BUDGET", GroupFinanceFormat.compactMoney(budgetTotal, currencyCode: currency), g2),
                    ("MOMENTS", "\(moments)", g3),
                ]
            case .customLiving:
                return [
                    ("PROPERTY", "\(peopleCount)", g0),
                    ("ASSETS", "\(sharedAssets.count)", g1),
                    ("BUDGET", GroupFinanceFormat.compactMoney(budgetTotal, currencyCode: currency), g2),
                    ("MOMENTS", "\(moments)", g3),
                ]
            }
        }()

        NativeDashboardScaffold(background: theme.bg) {
            NativeListSection {
                VStack(alignment: .leading, spacing: 14) {
                    if let error {
                        Text(error).font(.caption).foregroundStyle(Color(hex: "#F87171"))
                    }

                    MomentsHeroHeader(
                        eyebrow: "SHARED LIVING",
                        title: displayTitle,
                        status: status,
                        stats: heroStats,
                        chrome: chrome
                    )

                    MomentsSectionHeader(title: "Polls  🗳️", chrome: chrome, onViewAll: {
                        pollsListOpen = true
                    })
                    if listPolls.isEmpty {
                        GroupEmptySection(message: "No polls yet", detail: "Create a poll from Quick Add to decide together.")
                    } else {
                        ForEach(Array(listPolls.prefix(2))) { poll in
                            MomentsPollPreviewCard(poll: poll, chrome: chrome) {
                                if let id = poll.pollId { selectedPollId = id }
                            }
                        }
                    }

                    MomentsSectionHeader(title: "Tasks / Planning", chrome: chrome, onViewAll: { scheduleOpen = true })
                    if dayGroups.isEmpty {
                        GroupEmptySection(message: "No tasks yet", detail: "Add a planning item from Quick Add — nothing is invented.")
                    } else {
                        ForEach(Array(dayGroups.enumerated()), id: \.offset) { index, group in
                            let first = group.items.first
                            MomentsItineraryDayCard(
                                dayIndex: index + 1,
                                day: group.day,
                                title: first?.title ?? "Plans",
                                timeLabel: [
                                    formatPlanningTime(first?.dueAt),
                                    "\(group.items.count) item\(group.items.count == 1 ? "" : "s")",
                                ].compactMap { $0 }.joined(separator: " · "),
                                chrome: chrome
                            )
                        }
                    }

                    MomentsSectionHeader(title: "Updates / Feed  📱", chrome: chrome)
                    if updates.isEmpty {
                        GroupEmptySection(message: "No updates yet", detail: "Share a status update from Quick Add.")
                    } else {
                        ForEach(Array(updates.prefix(5).enumerated()), id: \.offset) { index, item in
                            MomentsUpdateFeedRow(item: item, index: index, chrome: chrome)
                        }
                    }

                    MomentsSectionHeader(title: "Shared Gallery  📸", chrome: chrome)
                    MemoryPhotoGalleryStrip(
                        items: listMemoryItems,
                        emptyMessage: "Gallery empty",
                        emptyDetail: "Add a memory with a photo from Quick Add.",
                        text: chrome.text,
                        muted: chrome.secondary,
                        field: chrome.card,
                        border: chrome.border,
                        showMediaCountBadge: true
                    )

                    if !highlights.isEmpty {
                        MomentsSectionHeader(title: "Highlights", chrome: chrome)
                        ForEach(Array(highlights.enumerated()), id: \.offset) { _, title in
                            MomentsSimpleRowCard(title: title, chrome: chrome)
                        }
                    }

                    MomentsSectionHeader(title: kind == .customLiving ? "Property Rules" : "House Rules", chrome: chrome)
                    if livingRules.isEmpty {
                        GroupEmptySection(message: "No rules yet", detail: "Add a living rule from Quick Add.")
                    } else {
                        ForEach(Array(livingRules.prefix(5))) { rule in
                            MomentsSimpleRowCard(
                                title: rule.title,
                                meta: rule.ruleText,
                                status: rule.status,
                                chrome: chrome
                            )
                        }
                    }

                    MomentsSectionHeader(
                        title: kind == .customLiving ? "Property Assets" : "Shared Assets",
                        chrome: chrome
                    )
                    if sharedAssets.isEmpty {
                        GroupEmptySection(message: "No shared assets yet", detail: "Add a household asset from Quick Add.")
                    } else {
                        ForEach(Array(sharedAssets.prefix(6))) { asset in
                            MomentsSimpleRowCard(
                                title: asset.title ?? "Asset",
                                status: asset.status,
                                chrome: chrome
                            )
                        }
                    }

                    MomentsSectionHeader(title: "Maintenance", chrome: chrome)
                    if maintenance.isEmpty {
                        GroupEmptySection(message: "No maintenance records", detail: "Log maintenance from Quick Add when something needs care.")
                    } else {
                        ForEach(Array(maintenance.prefix(5))) { record in
                            MomentsSimpleRowCard(
                                title: record.title ?? "Maintenance",
                                meta: record.description,
                                status: record.status,
                                chrome: chrome
                            )
                        }
                    }

                    MomentsSectionHeader(title: "Residents", chrome: chrome)
                    if residents.isEmpty {
                        GroupEmptySection(message: "No residents yet", detail: "Add a resident from Quick Add.")
                    } else {
                        ForEach(Array(residents.prefix(8))) { resident in
                            MomentsSimpleRowCard(
                                title: resident.name ?? resident.participantId ?? "Resident",
                                meta: resident.roleCode,
                                status: resident.status,
                                chrome: chrome
                            )
                        }
                    }

                    if theme.includesContribution {
                        MomentsSectionHeader(title: "Contributions & Expenses", chrome: chrome)
                        VStack(alignment: .leading, spacing: 10) {
                            Text("Collected \(funded.map { "\($0)%" } ?? "—") · \(GroupFinanceFormat.formatMoney(contributionTotal, currencyCode: currency))")
                                .font(.plusJakarta(size: 13, weight: .semibold))
                                .foregroundStyle(chrome.text)
                            MomentsExpensesCard(
                                spent: finance?.totals?.first?.expenseTotal,
                                currency: currency,
                                peopleCount: peopleCount,
                                expenses: listExpenses,
                                chrome: chrome
                            )
                        }
                    } else {
                        MomentsSectionHeader(title: "Expenses  💸", chrome: chrome)
                        MomentsExpensesCard(
                            spent: finance?.totals?.first?.expenseTotal,
                            currency: currency,
                            peopleCount: peopleCount,
                            expenses: listExpenses,
                            chrome: chrome
                        )
                    }

                    if showUpcoming {
                        MomentsSectionHeader(title: "Upcoming Events  🗓", chrome: chrome)
                        if upcoming.isEmpty {
                            GroupEmptySection(message: "Nothing upcoming", detail: "Near-term plans will show here.")
                        } else {
                            ForEach(Array(upcoming.enumerated()), id: \.offset) { index, event in
                                MomentsUpcomingEventCard(event: event, highlight: index == 0, chrome: chrome)
                            }
                        }
                    }

                    MomentsQuickAddCta(
                        title: "Add to the \(theme.typeLabel.lowercased()) story",
                        subtitle: "Add a resident, expense, task, asset or memory.",
                        chrome: chrome,
                        onTap: onOpenQuickAdd
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
            loading = false
        } else {
            loading = true
        }
        do {
            async let pulseResult = APIClient.shared.getGroupPulse(momentId: momentId)
            async let financeResult = APIClient.shared.getGroupFinance(momentId: momentId)
            async let lifeResult = APIClient.shared.getGroupLife(momentId: momentId)
            async let plansResult = APIClient.shared.listPlanningItems(momentId: momentId)
            async let bookingsResult = APIClient.shared.listBookings(momentId: momentId)
            async let updatesResult = APIClient.shared.listGroupUpdates(momentId: momentId)
            async let pollsResult = APIClient.shared.listPolls(momentId: momentId)
            async let residentsResult = APIClient.shared.listResidents(momentId: momentId)
            async let assetsResult = APIClient.shared.listSharedAssets(momentId: momentId)
            async let maintenanceResult = APIClient.shared.listMaintenanceRecords(momentId: momentId)
            async let rulesResult = APIClient.shared.listLivingRules(momentId: momentId)
            async let memoriesResult = APIClient.shared.listGroupMemories(momentId: momentId)
            async let expensesResult = APIClient.shared.listGroupExpenses(momentId: momentId, limit: 10)

            let loadedPulse = try await pulseResult
            let finFacet = try await financeResult
            let loadedLife = try await lifeResult
            let loadedFinance = finFacet.payload ?? loadedPulse.payload?.finance
            title = loadedPulse.title
            pulse = loadedPulse
            finance = loadedFinance
            life = loadedLife
            listPlanning = (try? await plansResult)?.items ?? loadedLife.payload?.planningItems ?? []
            listBookings = (try? await bookingsResult)?.items ?? loadedLife.payload?.bookings ?? []
            listUpdates = (try? await updatesResult)?.items ?? loadedLife.payload?.updates ?? []
            listPolls = (try? await pollsResult)?.items ?? []
            residents = (try? await residentsResult)?.items ?? []
            sharedAssets = (try? await assetsResult)?.items ?? []
            maintenance = (try? await maintenanceResult)?.items ?? []
            livingRules = (try? await rulesResult)?.items ?? []
            listExpenses = (try? await expensesResult)?.items ?? []
            if let mems = try? await memoriesResult {
                listMemoryItems = mems.items
                memoryCount = mems.memoryCount ?? mems.items.count
            } else {
                listMemoryItems = (try? await APIClient.shared.getGroupMemory(momentId: momentId))?.payload?.items ?? []
                memoryCount = listMemoryItems.count
            }
            GroupTabDataCache.putPulse(momentId, .init(
                title: loadedPulse.title,
                pulse: loadedPulse,
                finance: loadedFinance,
                activities: GroupTabDataCache.peekPulse(momentId)?.activities ?? []
            ))
            GroupTabDataCache.putLife(momentId, loadedLife)
        } catch {
            self.error = error.localizedDescription
        }
        loading = false
    }
}

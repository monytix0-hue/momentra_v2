import SwiftUI

/// Figma 575:14327 — Experience Moments. Live APIs only.
struct ExperienceMomentsActiveView: View {
    let theme: ExperienceActiveTheme
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
    @State private var listVendors: [APIClient.GroupVendorItemPayload] = []
    @State private var listAttendance: [APIClient.GroupAttendanceItemPayload] = []
    @State private var listExpenses: [APIClient.GroupExpenseListItemPayload] = []
    @State private var listContributions: [APIClient.GroupContributionItem] = []
    @State private var memoryCount: Int = 0
    @State private var selectedPollId: String?
    @State private var pollsListOpen = false
    @State private var scheduleOpen = false
    @State private var checklistSheetOpen = false
    @State private var contributionsOpen = false
    @State private var expensesOpen = false
    @State private var editingContribution: APIClient.GroupContributionItem?
    @State private var editContributionPresented = false
    @State private var title: String?
    @State private var loading = true
    @State private var error: String?

    private var chrome: MomentsChrome { .experience(theme) }
    private var isOfficeOuting: Bool { theme.typeLabel == ExperienceActiveTheme.officeOuting.typeLabel }

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
                items: GroupExperienceChecklistCatalog.nonChecklistItems(planningItems),
                momentId: momentId,
                momentTypeCode: momentTypeCode,
                accent: theme.accent,
                surface: theme.card,
                field: theme.bg,
                border: theme.border,
                text: theme.text,
                muted: theme.secondary,
                onDismiss: { scheduleOpen = false },
                onSaved: { Task { await load() } }
            )
        }
        .sheet(isPresented: $checklistSheetOpen) {
            NativeSheetScaffold(
                title: "Checklist",
                onClose: { checklistSheetOpen = false },
                background: theme.bg
            ) {
                ScrollView {
                    ExperienceChecklistBody(
                        momentId: momentId,
                        onDismiss: { checklistSheetOpen = false },
                        onSaved: { Task { await reloadPlanning() } },
                        accent: SheetAccent(
                            accent: theme.accent,
                            accentEnd: theme.accentSolid,
                            soft: theme.accent.opacity(0.2)
                        )
                    )
                    .padding(.horizontal, 20)
                    .padding(.top, 16)
                    .padding(.bottom, 28)
                }
            }
            .presentationDetents([.large])
            .presentationDragIndicator(.visible)
        }
        .sheet(isPresented: $pollsListOpen) {
            GroupPollsListSheet(
                momentTitle: momentTitle ?? title,
                chrome: .experience(theme),
                polls: listPolls,
                onDismiss: { pollsListOpen = false },
                onChanged: { Task { await load() } }
            )
        }
        .sheet(isPresented: $contributionsOpen) {
            ContributionsListSheet(
                items: listContributions,
                chrome: .experience(theme),
                momentId: momentId,
                onDismiss: { contributionsOpen = false },
                onEdit: { item in
                    contributionsOpen = false
                    editingContribution = item
                    editContributionPresented = true
                }
            )
        }
        .sheet(isPresented: $editContributionPresented) {
            if let momentId, let editingContribution {
                GroupContributionSheet(
                    momentId: momentId,
                    isPresented: $editContributionPresented,
                    editingContribution: editingContribution,
                    onSaved: {
                        editContributionPresented = false
                        Task { await load() }
                    },
                    onDeleted: {
                        editContributionPresented = false
                        Task { await load() }
                    }
                )
            }
        }
        .onChange(of: editContributionPresented) { _, open in
            if !open { editingContribution = nil }
        }
        .sheet(isPresented: $expensesOpen) {
            ExpensesListSheet(
                items: listExpenses,
                chrome: .experience(theme),
                onDismiss: { expensesOpen = false }
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
        let allTotals = finance?.totals ?? []
        let primary = GroupFinanceFormat.resolvePrimaryTotal(
            allTotals,
            preferredCurrency: finance?.viewerPosition?.currencyCode
        )
        let budgetTotal = primary?.budgetTotal
        let currency = primary?.currencyCode ?? "INR"
        let peopleCount = pulse?.payload?.participantCount ?? 0
        let yourShareLine = GroupFinanceFormat.viewerAllocatedPartitionLine(
            viewer: finance?.viewerPosition,
            allPositions: finance?.positions ?? []
        )
        let openTasks = pulse?.payload?.openTaskCount ?? life?.payload?.openTaskCount ?? 0
        let confirmed = listAttendance.filter {
            let s = ($0.attendanceStatus ?? "").uppercased()
            return s == "CONFIRMED" || s == "ATTENDING" || s == "YES"
        }.count
        let moments = memoryCount > 0 ? memoryCount : listMemoryItems.count
        let displayTitle = momentTitle ?? title ?? "\(theme.typeLabel) Moments"
        let status = (pulse?.status ?? life?.status ?? "PLANNING").uppercased()
        let dayGroups = itineraryDayGroups(planningItems)
        let upcoming = momentsUpcomingFromPlanning(planning: planningItems, bookings: bookings, finance: finance)
        let gradients = theme.statGradients
        let g0 = gradients.indices.contains(0) ? gradients[0] : [theme.accent, theme.accentSolid]
        let g1 = gradients.indices.contains(1) ? gradients[1] : [theme.accentLight, theme.accent]
        let g2 = gradients.indices.contains(2) ? gradients[2] : [theme.accent, theme.accentSolid]
        let g3 = gradients.indices.contains(3) ? gradients[3] : [theme.accentLight, theme.accentSolid]

        let plansPct = planningPlansPercent(planningItems)
        let heroStats: [(label: String, value: String, colors: [Color])] = isOfficeOuting
            ? [
                ("GUESTS", "\(peopleCount)", g0),
                ("CONFIRMED", "\(confirmed)", g1),
                ("BUDGET", GroupFinanceFormat.compactMoney(budgetTotal, currencyCode: currency), g2),
                ("TASKS", "\(openTasks)", g3),
            ]
            : [
                ("PEOPLE", "\(peopleCount)", [Color(hex: "#14B8A6"), Color(hex: "#0F766E")]),
                ("PLANS", "\(plansPct)%", [Color(hex: "#FF8E63"), Color(hex: "#E8744F")]),
                ("BUDGET", GroupFinanceFormat.compactMoney(budgetTotal, currencyCode: currency), [Color(hex: "#E88A4F"), Color(hex: "#C2410C")]),
                ("MOMENTS", "\(moments)", [Color(hex: "#A855F7"), Color(hex: "#7C3AED")]),
            ]

        NativeDashboardScaffold(background: theme.bg) {
            NativeListSection {
                VStack(alignment: .leading, spacing: 14) {
                    if let error {
                        Text(error).font(.caption).foregroundStyle(Color(hex: "#F87171"))
                    }

                    MomentsHeroHeader(
                        eyebrow: "SHARED EXPERIENCE",
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

                    MomentsSectionHeader(title: "Itinerary", chrome: chrome, onViewAll: { scheduleOpen = true })
                    if dayGroups.isEmpty {
                        GroupEmptySection(message: "No itinerary days yet", detail: "Add a planning item from Quick Add — nothing is invented.")
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

                    MomentsChecklistSection(
                        items: planningItems,
                        momentId: momentId,
                        chrome: chrome,
                        onChanged: { Task { await reloadPlanning() } },
                        onAdd: { checklistSheetOpen = true }
                    )

                    MomentsSectionHeader(title: "Updates / Feed  📱", chrome: chrome)
                    if updates.isEmpty {
                        GroupEmptySection(message: "No updates yet", detail: "Share a status update from Quick Add.")
                    } else {
                        ForEach(Array(updates.prefix(5).enumerated()), id: \.offset) { index, item in
                            MomentsUpdateFeedRow(item: item, index: index, chrome: chrome)
                        }
                    }

                    if !isOfficeOuting || !listVendors.isEmpty {
                        MomentsSectionHeader(title: "Vendors  🏪", chrome: chrome)
                        if listVendors.isEmpty {
                            GroupEmptySection(message: "No vendors yet", detail: "Add a vendor from Quick Add when ready.")
                        } else {
                            ForEach(Array(listVendors.prefix(5))) { vendor in
                                MomentsSimpleRowCard(
                                    title: vendor.vendorName ?? "Vendor",
                                    meta: vendor.vendorType,
                                    status: vendor.status,
                                    chrome: chrome
                                )
                            }
                        }
                    }

                    MomentsSectionHeader(title: "Attendance  ✅", chrome: chrome)
                    if listAttendance.isEmpty {
                        GroupEmptySection(
                            message: isOfficeOuting ? "No attendance yet" : "No RSVPs yet",
                            detail: "Record attendance from Quick Add."
                        )
                    } else {
                        ForEach(Array(listAttendance.prefix(8))) { row in
                            MomentsSimpleRowCard(
                                title: row.displayName ?? "Guest",
                                meta: row.note,
                                status: row.attendanceStatus,
                                chrome: chrome
                            )
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

                    MomentsSectionHeader(title: "Bookings  🛎️", chrome: chrome)
                    if listBookings.isEmpty {
                        GroupEmptySection(message: "No bookings yet", detail: "Add a booking from Quick Add when ready.")
                    } else {
                        ForEach(Array(listBookings.prefix(4).enumerated()), id: \.offset) { _, booking in
                            MomentsBookingCard(booking: booking, chrome: chrome)
                        }
                    }

                    MomentsSectionHeader(title: "Upcoming Events  🗓", chrome: chrome)
                    if upcoming.isEmpty {
                        GroupEmptySection(message: "Nothing upcoming", detail: "Near-term bookings and plans will show here.")
                    } else {
                        ForEach(Array(upcoming.enumerated()), id: \.offset) { index, event in
                            MomentsUpcomingEventCard(event: event, highlight: index == 0, chrome: chrome)
                        }
                    }

                    MomentsContributionDetailsSection(
                        items: listContributions,
                        chrome: chrome,
                        momentId: momentId,
                        onViewAll: { contributionsOpen = true },
                        onEdit: { item in
                            editingContribution = item
                            editContributionPresented = true
                        }
                    )

                    MomentsSectionHeader(title: "Expenses & Budget  💸", chrome: chrome, onViewAll: {
                        expensesOpen = true
                    })
                    MomentsExpensesCard(
                        totals: allTotals,
                        yourAllocatedLine: yourShareLine,
                        expenses: listExpenses,
                        chrome: chrome,
                        fallbackCurrency: currency
                    )

                    MomentsQuickAddCta(chrome: chrome, onTap: onOpenQuickAdd)
                }
            }
        }
    }

    private func reloadPlanning() async {
        guard let momentId else { return }
        listPlanning = (try? await APIClient.shared.listPlanningItems(momentId: momentId))?.items ?? listPlanning
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
            let tab = try await GroupTabLoad.loadPulseTab(momentId: momentId)
            title = tab.title
            pulse = tab.pulse
            finance = tab.finance
            loading = false

            async let lifeResult = APIClient.shared.getGroupLife(momentId: momentId)
            async let plansResult = APIClient.shared.listPlanningItems(momentId: momentId)
            async let bookingsResult = APIClient.shared.listBookings(momentId: momentId)
            async let updatesResult = APIClient.shared.listGroupUpdates(momentId: momentId)
            async let pollsResult = APIClient.shared.listPolls(momentId: momentId)
            async let memoriesResult = APIClient.shared.listGroupMemories(momentId: momentId)
            async let vendorsResult = APIClient.shared.listGroupVendors(momentId: momentId)
            async let attendanceResult = APIClient.shared.listGroupAttendance(momentId: momentId)
            async let expensesResult = APIClient.shared.listGroupExpenses(momentId: momentId, limit: 50)
            async let contributionsResult = APIClient.shared.listContributions(momentId: momentId, limit: 50)

            let loadedLife = try await lifeResult
            life = loadedLife
            listPlanning = (try? await plansResult)?.items ?? loadedLife.payload?.planningItems ?? []
            listBookings = (try? await bookingsResult)?.items ?? loadedLife.payload?.bookings ?? []
            listUpdates = (try? await updatesResult)?.items ?? loadedLife.payload?.updates ?? []
            listPolls = (try? await pollsResult)?.items ?? []
            listVendors = (try? await vendorsResult)?.items ?? []
            listAttendance = (try? await attendanceResult)?.items ?? []
            listExpenses = (try? await expensesResult)?.items ?? []
            listContributions = (try? await contributionsResult)?.items ?? []
            if let listed = try? await memoriesResult {
                listMemoryItems = listed.items
                memoryCount = listed.memoryCount ?? listed.items.count
            } else {
                listMemoryItems = (try? await APIClient.shared.getGroupMemory(momentId: momentId))?.payload?.items ?? []
                memoryCount = listMemoryItems.count
            }
            let prior = GroupTabDataCache.peekPulse(momentId)
            GroupTabDataCache.putPulse(momentId, .init(
                title: tab.title ?? prior?.title,
                pulse: tab.pulse ?? prior?.pulse,
                finance: tab.finance ?? prior?.finance,
                activities: prior?.activities ?? tab.activities,
                insights: prior?.insights ?? []
            ))
            GroupTabDataCache.putLife(momentId, loadedLife)
        } catch {
            self.error = error.localizedDescription
            loading = false
        }
    }
}

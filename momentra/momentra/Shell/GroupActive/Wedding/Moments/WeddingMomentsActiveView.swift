import SwiftUI

/// Figma 575:14768 — Wedding Moments. Live APIs only; no demo seeds.
struct WeddingMomentsActiveView: View {
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
    @State private var memoryCount: Int = 0
    @State private var selectedPollId: String?
    @State private var pollsListOpen = false
    @State private var scheduleOpen = false
    @State private var title: String?
    @State private var loading = true
    @State private var error: String?

    private var chrome: MomentsChrome { .wedding }

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
                ProgressView().tint(WeddingActiveTheme.accent)
            } else {
                content
            }
        }
        .background(WeddingActiveTheme.bg)
        .task(id: "\(refreshToken)-\(momentId ?? "")") { await load() }
        .sheet(isPresented: $scheduleOpen) {
            PlanningScheduleSheet(
                items: planningItems,
                momentTypeCode: momentTypeCode,
                accent: WeddingActiveTheme.accent,
                surface: WeddingActiveTheme.bg,
                field: WeddingActiveTheme.card,
                border: WeddingActiveTheme.border,
                text: WeddingActiveTheme.text,
                muted: WeddingActiveTheme.secondary,
                onDismiss: { scheduleOpen = false }
            )
        }
        .sheet(isPresented: $pollsListOpen) {
            GroupPollsListSheet(
                momentTitle: momentTitle ?? title,
                chrome: .wedding,
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
        let currency = finance?.totals?.first?.currencyCode ?? "INR"
        let peopleCount = pulse?.payload?.participantCount ?? 0
        let openTasks = pulse?.payload?.openTaskCount ?? life?.payload?.openTaskCount ?? 0
        let moments = memoryCount > 0 ? memoryCount : listMemoryItems.count
        let displayTitle = momentTitle ?? title ?? "Wedding Moments"
        let status = (pulse?.status ?? life?.status ?? "PLANNING").uppercased()
        let dayGroups = itineraryDayGroups(planningItems)
        let upcoming = momentsUpcomingFromPlanning(
            planning: planningItems,
            bookings: bookings,
            finance: finance
        )

        NativeDashboardScaffold(background: WeddingActiveTheme.bg) {
            NativeListSection {
                VStack(alignment: .leading, spacing: 14) {
                    if let error {
                        Text(error).font(.caption).foregroundStyle(Color(hex: "#F87171"))
                    }

                    MomentsHeroHeader(
                        eyebrow: "SHARED EXPERIENCE",
                        title: displayTitle,
                        status: status,
                        stats: [
                            ("GUESTS", "\(peopleCount)", [Color(hex: "#A62E66"), Color(hex: "#6B1A40")]),
                            ("BUDGET", GroupFinanceFormat.compactMoney(budgetTotal, currencyCode: currency), [Color(hex: "#8C1F59"), Color(hex: "#591438")]),
                            ("MOMENTS", "\(moments)", [Color(hex: "#992673"), Color(hex: "#661A4D")]),
                            ("TASKS", "\(openTasks)", [Color(hex: "#7A1F66"), Color(hex: "#4D1440")]),
                        ],
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

                    MomentsSectionHeader(title: "Itinerary / Timeline", chrome: chrome, onViewAll: {
                        scheduleOpen = true
                    })
                    if dayGroups.isEmpty {
                        GroupEmptySection(message: "No timeline days yet", detail: "Add a planning item from Quick Add — nothing is invented.")
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

                    MomentsSectionHeader(title: "Vendors  🏪", chrome: chrome)
                    if listVendors.isEmpty {
                        GroupEmptySection(message: "No vendors yet", detail: "Add a vendor from Quick Add when ready.")
                    } else {
                        ForEach(Array(listVendors.prefix(5))) { vendor in
                            MomentsSimpleRowCard(
                                title: vendor.vendorName ?? "Vendor",
                                meta: [vendor.vendorType, vendor.createdAt.flatMap { formatBookingDay($0) }]
                                    .compactMap { $0 }
                                    .joined(separator: " · "),
                                status: vendor.status,
                                chrome: chrome
                            )
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

                    MomentsSectionHeader(title: "Attendance / RSVP  ✅", chrome: chrome)
                    if listAttendance.isEmpty {
                        GroupEmptySection(message: "No RSVPs yet", detail: "Record attendance from Quick Add.")
                    } else {
                        ForEach(Array(listAttendance.prefix(8))) { row in
                            MomentsSimpleRowCard(
                                title: row.displayName ?? "Guest",
                                meta: row.note,
                                status: row.attendanceStatus,
                                statusColor: attendanceColor(row.attendanceStatus),
                                chrome: chrome
                            )
                        }
                    }

                    MomentsSectionHeader(title: "Expenses & Budget  💸", chrome: chrome)
                    MomentsExpensesCard(
                        spent: finance?.totals?.first?.expenseTotal,
                        currency: currency,
                        peopleCount: peopleCount,
                        expenses: listExpenses,
                        chrome: chrome
                    )

                    MomentsQuickAddCta(
                        title: "Add to the wedding story",
                        subtitle: "Add a plan, expense, memory, poll or update.",
                        chrome: chrome,
                        onTap: onOpenQuickAdd
                    )
                }
            }
        }
    }

    private func attendanceColor(_ status: String?) -> Color {
        switch (status ?? "").uppercased() {
        case "CONFIRMED", "ATTENDING", "YES": return Color(hex: "#22C55E")
        case "DECLINED", "NO": return Color(hex: "#F87171")
        default: return chrome.accent
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
            async let memoriesResult = APIClient.shared.listGroupMemories(momentId: momentId)
            async let vendorsResult = APIClient.shared.listGroupVendors(momentId: momentId)
            async let attendanceResult = APIClient.shared.listGroupAttendance(momentId: momentId)
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
            listVendors = (try? await vendorsResult)?.items ?? []
            listAttendance = (try? await attendanceResult)?.items ?? []
            listExpenses = (try? await expensesResult)?.items ?? []
            if let listed = try? await memoriesResult {
                listMemoryItems = listed.items
                memoryCount = listed.memoryCount ?? listed.items.count
            } else if let facet = try? await APIClient.shared.getGroupMemory(momentId: momentId) {
                listMemoryItems = facet.payload?.items ?? []
                memoryCount = facet.payload?.memoryCount ?? listMemoryItems.count
            } else {
                listMemoryItems = []
                memoryCount = 0
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

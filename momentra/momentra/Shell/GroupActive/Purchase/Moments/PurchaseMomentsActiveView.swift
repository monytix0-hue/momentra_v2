import SwiftUI

/// Figma 601:12875 / 605:8874 / 605:10476 / 617:11896 — Purchase Moments. Live APIs only.
struct PurchaseMomentsActiveView: View {
    let theme: PurchaseActiveTheme
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
    @State private var purchaseItems: [APIClient.GroupPurchaseItemPayload] = []
    @State private var ownership: [APIClient.GroupOwnershipItemPayload] = []
    @State private var listVendors: [APIClient.GroupVendorItemPayload] = []
    @State private var listExpenses: [APIClient.GroupExpenseListItemPayload] = []
    @State private var participants: [APIClient.GroupParticipantPayload] = []
    @State private var memoryCount: Int = 0
    @State private var selectedPollId: String?
    @State private var pollsListOpen = false
    @State private var scheduleOpen = false
    @State private var title: String?
    @State private var loading = true
    @State private var error: String?

    private var chrome: MomentsChrome { .purchase(theme) }

    private enum PurchaseKind {
        case giftPool, groupPurchase, sharedAsset, customPurchase
    }

    private var kind: PurchaseKind {
        switch theme.typeLabel {
        case PurchaseActiveTheme.groupPurchase.typeLabel: return .groupPurchase
        case PurchaseActiveTheme.sharedAsset.typeLabel: return .sharedAsset
        case PurchaseActiveTheme.customPurchase.typeLabel: return .customPurchase
        default: return .giftPool
        }
    }

    private var showContributions: Bool { kind != .customPurchase || theme.includesContributor }
    private var showOwnership: Bool { kind == .sharedAsset || kind == .customPurchase || kind == .groupPurchase }
    private var ownershipLight: Bool { kind == .groupPurchase }
    private var showVendors: Bool { kind == .groupPurchase || kind == .customPurchase }
    private var showUpcoming: Bool { kind == .giftPool || kind == .groupPurchase }
    private var showGalleryAlways: Bool { kind == .giftPool || kind == .groupPurchase }
    private var showGalleryIfMedia: Bool { kind == .sharedAsset }

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
                chrome: .purchase(theme),
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
        let peopleCount = pulse?.payload?.participantCount ?? 0
        let moments = memoryCount > 0 ? memoryCount : listMemoryItems.count
        let funded = PurchaseFinanceMath.fundedPercent(
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
        let positions = finance?.positions ?? []

        let heroStats: [(label: String, value: String, colors: [Color])] = {
            switch kind {
            case .giftPool:
                return [
                    ("PEOPLE", "\(peopleCount)", g0),
                    ("FUNDED", funded.map { "\($0)%" } ?? "—", g1),
                    ("BUDGET", GroupFinanceFormat.compactMoney(budgetTotal, currencyCode: currency), g2),
                    ("MOMENTS", "\(moments)", g3),
                ]
            case .groupPurchase:
                return [
                    ("PEOPLE", "\(peopleCount)", g0),
                    ("FUNDED", funded.map { "\($0)%" } ?? "—", g1),
                    ("BUDGET", GroupFinanceFormat.compactMoney(budgetTotal, currencyCode: currency), g2),
                    ("ITEMS", "\(purchaseItems.count)", g3),
                ]
            case .sharedAsset:
                return [
                    ("ACTIVE", "Moment", g0),
                    ("PEOPLE", "\(peopleCount)", g1),
                    ("FUNDED", funded.map { "\($0)%" } ?? "—", g2),
                    ("OWNERS", "\(ownership.count)", g3),
                ]
            case .customPurchase:
                return [
                    ("CURRENT", "Moment", g0),
                    ("PEOPLE", "\(peopleCount)", g1),
                    ("ITEMS", "\(purchaseItems.count)", g2),
                    ("OWNERS", "\(ownership.count)", g3),
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
                        eyebrow: "SHARED PURCHASE",
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

                    MomentsSectionHeader(
                        title: kind == .sharedAsset || kind == .customPurchase ? "Moment Timeline" : "Itinerary",
                        chrome: chrome,
                        onViewAll: { scheduleOpen = true }
                    )
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

                    if showGalleryAlways || (showGalleryIfMedia && !listMemoryItems.isEmpty) {
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
                    }

                    if showContributions {
                        MomentsSectionHeader(title: theme.contributionsTitle, chrome: chrome)
                        contributionsCard(
                            funded: funded,
                            contributionTotal: contributionTotal,
                            budgetTotal: budgetTotal,
                            currency: currency,
                            positions: positions
                        )
                    }

                    MomentsSectionHeader(title: "Purchases & Deliveries", chrome: chrome)
                    if purchaseItems.isEmpty {
                        GroupEmptySection(message: "No purchase items yet", detail: "Add an item from Quick Add.")
                    } else {
                        ForEach(Array(purchaseItems.prefix(6))) { item in
                            MomentsSimpleRowCard(
                                title: item.label ?? "Item",
                                meta: [
                                    GroupFinanceFormat.formatMoney(item.amount, currencyCode: currency),
                                    item.createdAt.flatMap { formatBookingDay($0) },
                                ].compactMap { $0 }.joined(separator: " · "),
                                status: item.status,
                                chrome: chrome
                            )
                        }
                    }

                    if showOwnership {
                        MomentsSectionHeader(
                            title: ownershipLight ? "Ownership" : "Ownership / Members",
                            chrome: chrome
                        )
                        if ownership.isEmpty {
                            GroupEmptySection(
                                message: "No ownership records",
                                detail: ownershipLight
                                    ? "Ownership can be recorded when shares are set."
                                    : "Add ownership from Quick Add."
                            )
                        } else {
                            ForEach(Array(ownership.prefix(ownershipLight ? 3 : 8))) { row in
                                MomentsSimpleRowCard(
                                    title: row.displayName ?? "Member",
                                    meta: [
                                        row.ownershipShare.map { "Share \($0)" },
                                        row.ownershipNote,
                                    ].compactMap { $0 }.joined(separator: " · "),
                                    status: row.status,
                                    chrome: chrome
                                )
                            }
                        }
                    }

                    if showVendors {
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

                    MomentsSectionHeader(title: "Expenses & Budget  💸", chrome: chrome)
                    MomentsExpensesCard(
                        spent: finance?.totals?.first?.expenseTotal,
                        currency: currency,
                        peopleCount: peopleCount,
                        expenses: listExpenses,
                        chrome: chrome
                    )

                    MomentsQuickAddCta(
                        title: "Add to the \(theme.typeLabel.lowercased()) story",
                        subtitle: "Add an item, contribution, memory, poll or update.",
                        chrome: chrome,
                        onTap: onOpenQuickAdd
                    )
                }
            }
        }
    }

    @ViewBuilder
    private func contributionsCard(
        funded: Int?,
        contributionTotal: String?,
        budgetTotal: String?,
        currency: String,
        positions: [APIClient.GroupFinancePositionPayload]
    ) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("Funded")
                    .font(.plusJakarta(size: 13, weight: .semibold))
                    .foregroundStyle(chrome.secondary)
                Spacer()
                Text(funded.map { "\($0)%" } ?? "—")
                    .font(.plusJakarta(size: 16, weight: .bold))
                    .foregroundStyle(chrome.text)
            }
            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    Capsule().fill(Color(hex: "#252332"))
                    Capsule()
                        .fill(chrome.accent)
                        .frame(width: max(6, geo.size.width * CGFloat(funded ?? 0) / 100))
                }
            }
            .frame(height: 8)
            Text("\(GroupFinanceFormat.formatMoney(contributionTotal, currencyCode: currency)) of \(GroupFinanceFormat.formatMoney(budgetTotal, currencyCode: currency))")
                .font(.plusJakarta(size: 12))
                .foregroundStyle(chrome.secondary)
            if positions.isEmpty {
                Text("No member contribution positions yet.")
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(chrome.secondary)
            } else {
                ForEach(Array(positions.prefix(5))) { pos in
                    let name = participants.first(where: { $0.participantId == pos.participantId })?.displayName
                        ?? String(pos.participantId.prefix(8)) + "…"
                    HStack {
                        Text(name)
                            .font(.plusJakarta(size: 13, weight: .semibold))
                            .foregroundStyle(chrome.text)
                        Spacer()
                        Text(GroupFinanceFormat.formatMoney(pos.contributionTotal ?? pos.paidTotal, currencyCode: currency))
                            .font(.plusJakarta(size: 13, weight: .bold))
                            .foregroundStyle(chrome.text)
                    }
                }
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(chrome.card)
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(chrome.border))
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
            async let purchaseResult = APIClient.shared.listPurchaseItems(momentId: momentId)
            async let memoriesResult = APIClient.shared.listGroupMemories(momentId: momentId)
            async let expensesResult = APIClient.shared.listGroupExpenses(momentId: momentId, limit: 10)
            async let ownershipResult = APIClient.shared.listOwnershipRecords(momentId: momentId)
            async let vendorsResult = APIClient.shared.listGroupVendors(momentId: momentId)
            async let participantsResult = APIClient.shared.listGroupParticipants(momentId: momentId)

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
            purchaseItems = (try? await purchaseResult)?.items ?? []
            listExpenses = (try? await expensesResult)?.items ?? []
            ownership = showOwnership ? ((try? await ownershipResult)?.items ?? []) : []
            listVendors = showVendors ? ((try? await vendorsResult)?.items ?? []) : []
            participants = (try? await participantsResult) ?? []
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

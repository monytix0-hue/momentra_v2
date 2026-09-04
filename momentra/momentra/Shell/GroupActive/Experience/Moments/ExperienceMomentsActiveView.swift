import SwiftUI

/// Figma 584:15500 / 584:16218 — Experience Moments. Live APIs only.
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
    @State private var selectedPollId: String?
    @State private var scheduleOpen = false
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
        .sheet(isPresented: $scheduleOpen) {
            PlanningScheduleSheet(
                items: allPlanningItems,
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

    private var allPlanningItems: [GroupPlanningItem] {
        listPlanning.isEmpty ? (life?.payload?.planningItems ?? []) : listPlanning
    }

    @ViewBuilder
    private var content: some View {
        let budgetTotal = finance?.totals?.first?.budgetTotal
        let currency = finance?.totals?.first?.currencyCode ?? "INR"
        let peopleCount = pulse?.payload?.participantCount ?? 0
        let openTasks = pulse?.payload?.openTaskCount ?? life?.payload?.openTaskCount ?? 0
        let displayTitle = momentTitle ?? title ?? "\(theme.typeLabel) Moments"
        let recentPlans = recentOpenPlanningItems(allPlanningItems)
        let bookings = listBookings.isEmpty ? (life?.payload?.bookings ?? []) : listBookings
        let updates = listUpdates.isEmpty ? (life?.payload?.updates ?? []) : listUpdates
        NativeDashboardScaffold(background: theme.bg) {

            NativeListSection {

            VStack(alignment: .leading, spacing: 14) {
                if let error {
                    Text(error).font(.caption).foregroundStyle(Color(hex: "#F87171"))
                }

                VStack(alignment: .leading, spacing: 12) {
                    HStack(alignment: .top) {
                        VStack(alignment: .leading, spacing: 6) {
                            Text("SHARED EXPERIENCE")
                                .font(.plusJakarta(size: 11, weight: .semibold))
                                .foregroundStyle(theme.secondary)
                            Text(displayTitle)
                                .font(.plusJakarta(size: 24, weight: .bold))
                                .foregroundStyle(theme.text)
                        }
                        Spacer()
                        Text(theme.typeLabel.uppercased())
                            .font(.plusJakarta(size: 10, weight: .bold))
                            .foregroundStyle(theme.darkText)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 4)
                            .background(theme.accent)
                            .clipShape(RoundedRectangle(cornerRadius: 8))
                    }

                    HStack(spacing: 12) {
                        ExperienceStatCard(label: "PEOPLE", value: "\(peopleCount)", colors: theme.statGradients[0])
                        ExperienceStatCard(
                            label: "BUDGET",
                            value: GroupFinanceFormat.compactMoney(budgetTotal, currencyCode: currency),
                            colors: theme.statGradients[1]
                        )
                    }
                    HStack(spacing: 12) {
                        ExperienceStatCard(label: "UPDATES", value: "\(updates.count)", colors: theme.statGradients[2])
                        ExperienceStatCard(label: "TASKS", value: "\(openTasks)", colors: theme.statGradients[0])
                    }
                }
                .padding(16)
                .background(theme.card)
                .clipShape(RoundedRectangle(cornerRadius: 20))
                .overlay(RoundedRectangle(cornerRadius: 20).stroke(theme.border))

                ExperienceSectionCard(theme: theme, title: "Planning") {
                    MomentsPlanningHeader(
                        title: "Recent plans",
                        text: theme.text,
                        muted: theme.secondary,
                        accent: theme.accent,
                        onOpenSchedule: { scheduleOpen = true }
                    )
                    if recentPlans.isEmpty {
                        ExperienceEmptyBlock(
                            theme: theme,
                            message: "No timeline items yet",
                            detail: "Add a planning item from Quick Add — nothing is invented."
                        )
                    } else {
                        ForEach(Array(recentPlans.enumerated()), id: \.offset) { _, item in
                            MomentsPlanningRecentRow(
                                item: item,
                                momentTypeCode: momentTypeCode,
                                text: theme.text,
                                muted: theme.secondary,
                                accent: theme.accent,
                                field: theme.bg,
                                border: theme.border
                            )
                        }
                    }
                }

                ExperienceSectionCard(theme: theme, title: "Bookings") {
                    if bookings.isEmpty {
                        ExperienceEmptyBlock(
                            theme: theme,
                            message: "No bookings yet",
                            detail: "Add a booking from Quick Add when ready."
                        )
                    } else {
                        ForEach(bookings.indices, id: \.self) { i in
                            Text(bookings[i].title ?? bookings[i].bookingId ?? "")
                                .font(.plusJakarta(size: 13))
                                .foregroundStyle(theme.text)
                        }
                    }
                }

                ExperienceSectionCard(theme: theme, title: "Polls") {
                    if listPolls.isEmpty {
                        ExperienceEmptyBlock(
                            theme: theme,
                            message: "No polls yet",
                            detail: "Create a poll from Quick Add to decide together."
                        )
                    } else {
                        ForEach(listPolls) { item in
                            Button {
                                if let id = item.pollId { selectedPollId = id }
                            } label: {
                                Text(item.question ?? item.pollId ?? "Poll")
                                    .font(.plusJakarta(size: 13, weight: .semibold))
                                    .foregroundStyle(theme.text)
                                    .frame(maxWidth: .infinity, alignment: .leading)
                                    .padding(12)
                                    .background(theme.bg)
                                    .clipShape(RoundedRectangle(cornerRadius: 12))
                                    .overlay(RoundedRectangle(cornerRadius: 12).stroke(theme.border))
                            }
                            .buttonStyle(.plain)
                        }
                    }
                }

                ExperienceSectionCard(theme: theme, title: "Updates") {
                    if updates.isEmpty {
                        ExperienceEmptyBlock(
                            theme: theme,
                            message: "No updates yet",
                            detail: "Share a status update from Quick Add."
                        )
                    } else {
                        ForEach(Array(updates.prefix(8).enumerated()), id: \.offset) { _, item in
                            MomentsUrgentUpdateRow(
                                item: item,
                                text: theme.text,
                                muted: theme.secondary,
                                field: theme.bg,
                                border: theme.border
                            )
                        }
                    }
                }

                ExperienceSectionCard(theme: theme, title: "Shared Gallery") {
                    MemoryPhotoGalleryStrip(
                        items: listMemoryItems,
                        emptyMessage: "Gallery empty",
                        emptyDetail: "Add a memory with a photo from Quick Add.",
                        text: theme.text,
                        muted: theme.secondary,
                        field: theme.card,
                        border: theme.border
                    )
                }

                VStack(spacing: 16) {
                    Text("Add to the \(theme.typeLabel.lowercased()) story")
                        .font(.plusJakarta(size: 18, weight: .bold))
                        .foregroundStyle(Color.white)
                    Text("Add a plan, expense, memory, poll or update.")
                        .font(.plusJakarta(size: 13))
                        .foregroundStyle(Color.white.opacity(0.9))
                    Button(action: onOpenQuickAdd) {
                        Text("+ Open Quick Add")
                            .font(.plusJakarta(size: 14, weight: .bold))
                            .foregroundStyle(theme.darkText)
                            .frame(maxWidth: .infinity).background(Color.white)
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                    }
                    .buttonStyle(.plain)
                }
                .padding(20)
                .frame(maxWidth: .infinity)
                .background(theme.heroGradient)
                .clipShape(RoundedRectangle(cornerRadius: 20))
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
            async let memoriesResult = APIClient.shared.listGroupMemories(momentId: momentId)
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
            if let listed = try? await memoriesResult {
                listMemoryItems = listed.items
            } else if let cached = GroupTabDataCache.peekMemory(momentId)?.memory?.payload?.items {
                listMemoryItems = cached
            } else if let facet = try? await APIClient.shared.getGroupMemory(momentId: momentId) {
                listMemoryItems = facet.payload?.items ?? []
            } else {
                listMemoryItems = []
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

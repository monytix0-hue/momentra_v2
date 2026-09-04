import SwiftUI

/// Figma 575:14768 — Wedding Moments. Live APIs only; no demo seeds.
struct WeddingMomentsActiveView: View {
    let refreshToken: UInt64
    let momentId: String?
    let momentTitle: String?
    var onOpenQuickAdd: () -> Void = {}

    @State private var pulse: APIClient.GroupPulsePayload?
    @State private var finance: APIClient.GroupFinancePayload?
    @State private var life: APIClient.GroupLifePayload?
    @State private var listPlanning: [APIClient.GroupLifePayload.LifeInner.PlanningItem] = []
    @State private var listBookings: [APIClient.GroupLifePayload.LifeInner.BookingItem] = []
    @State private var listUpdates: [APIClient.GroupLifePayload.LifeInner.UpdateItem] = []
    @State private var listMemoryItems: [GroupMemoryItem] = []
    @State private var title: String?
    @State private var loading = true
    @State private var error: String?

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
    }

    @ViewBuilder
    private var content: some View {
        let budgetTotal = finance?.totals?.first?.budgetTotal
        let currency = finance?.totals?.first?.currencyCode ?? "INR"
        let peopleCount = pulse?.payload?.participantCount ?? 0
        let openTasks = pulse?.payload?.openTaskCount ?? life?.payload?.openTaskCount ?? 0
        let displayTitle = momentTitle ?? title ?? "Wedding Moments"
        let planningItems = listPlanning.isEmpty ? (life?.payload?.planningItems ?? []) : listPlanning
        let bookings = listBookings.isEmpty ? (life?.payload?.bookings ?? []) : listBookings
        let updates = listUpdates.isEmpty ? (life?.payload?.updates ?? []) : listUpdates
        NativeDashboardScaffold(background: WeddingActiveTheme.bg) {

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
                                .foregroundStyle(WeddingActiveTheme.secondary)
                            Text(displayTitle)
                                .font(.plusJakarta(size: 24, weight: .bold))
                                .foregroundStyle(WeddingActiveTheme.text)
                        }
                        Spacer()
                        Text("PLANNING")
                            .font(.plusJakarta(size: 10, weight: .bold))
                            .foregroundStyle(WeddingActiveTheme.darkText)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 4)
                            .background(WeddingActiveTheme.accent)
                            .clipShape(RoundedRectangle(cornerRadius: 8))
                    }

                    HStack(spacing: 12) {
                        WeddingStatCard(
                            label: "GUESTS",
                            value: "\(peopleCount)",
                            colors: [Color(hex: "#A62E66"), Color(hex: "#6B1A40")]
                        )
                        WeddingStatCard(
                            label: "BUDGET",
                            value: GroupFinanceFormat.compactMoney(budgetTotal, currencyCode: currency),
                            colors: [Color(hex: "#8C1F59"), Color(hex: "#591438")]
                        )
                    }
                    HStack(spacing: 12) {
                        WeddingStatCard(
                            label: "UPDATES",
                            value: "\(updates.count)",
                            colors: [Color(hex: "#992673"), Color(hex: "#661A4D")]
                        )
                        WeddingStatCard(
                            label: "TASKS",
                            value: "\(openTasks)",
                            colors: [Color(hex: "#7A1F66"), Color(hex: "#4D1440")]
                        )
                    }
                }
                .padding(16)
                .background(WeddingActiveTheme.card)
                .clipShape(RoundedRectangle(cornerRadius: 20))
                .overlay(RoundedRectangle(cornerRadius: 20).stroke(WeddingActiveTheme.border))

                WeddingSectionCard(title: "Wedding Timeline") {
                    if planningItems.isEmpty {
                        WeddingEmptyBlock(
                            message: "No timeline items yet",
                            detail: "Add a planning item from Quick Add — nothing is invented."
                        )
                    } else {
                        ForEach(planningItems.indices, id: \.self) { i in
                            Text(planningItems[i].title ?? planningItems[i].planningItemId ?? "")
                                .font(.plusJakarta(size: 13, weight: .semibold))
                                .foregroundStyle(WeddingActiveTheme.text)
                                .frame(maxWidth: .infinity, alignment: .leading)
                                .padding(12)
                                .background(WeddingActiveTheme.bg)
                                .clipShape(RoundedRectangle(cornerRadius: 12))
                                .overlay(RoundedRectangle(cornerRadius: 12).stroke(WeddingActiveTheme.border))
                        }
                    }
                }

                WeddingSectionCard(title: "Bookings") {
                    if bookings.isEmpty {
                        WeddingEmptyBlock(
                            message: "No bookings yet",
                            detail: "Add a booking from Quick Add when ready."
                        )
                    } else {
                        ForEach(bookings.indices, id: \.self) { i in
                            Text(bookings[i].title ?? bookings[i].bookingId ?? "")
                                .font(.plusJakarta(size: 13))
                                .foregroundStyle(WeddingActiveTheme.text)
                        }
                    }
                }

                WeddingSectionCard(title: "Updates") {
                    if updates.isEmpty {
                        WeddingEmptyBlock(
                            message: "No updates yet",
                            detail: "Share a status update from Quick Add."
                        )
                    } else {
                        ForEach(updates.prefix(8).indices, id: \.self) { i in
                            Text(updates[i].message ?? updates[i].updateId ?? "")
                                .font(.plusJakarta(size: 13))
                                .foregroundStyle(WeddingActiveTheme.text)
                        }
                    }
                }

                WeddingSectionCard(title: "Shared Gallery") {
                    MemoryPhotoGalleryStrip(
                        items: listMemoryItems,
                        emptyMessage: "Gallery empty",
                        emptyDetail: "Add a memory with a photo from Quick Add.",
                        text: WeddingActiveTheme.text,
                        muted: WeddingActiveTheme.secondary,
                        field: WeddingActiveTheme.card,
                        border: WeddingActiveTheme.border
                    )
                }

                WeddingPinkCta(
                    title: "Add to the wedding story",
                    subtitle: "Add a plan, expense, memory, poll or update.",
                    buttonLabel: "Open Quick Add",
                    enabled: true,
                    outlinedButton: true,
                    action: onOpenQuickAdd
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
            if let listed = try? await memoriesResult {
                listMemoryItems = listed.items
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

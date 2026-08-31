import SwiftUI

/// Figma 584:15500 / 584:16218 — Experience Moments. Live APIs only.
struct ExperienceMomentsActiveView: View {
    let theme: ExperienceActiveTheme
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
        let budgetTotal = finance?.totals?.first?.budgetTotal
        let currency = finance?.totals?.first?.currencyCode ?? "INR"
        let peopleCount = pulse?.payload?.participantCount ?? 0
        let openTasks = pulse?.payload?.openTaskCount ?? life?.payload?.openTaskCount ?? 0
        let displayTitle = momentTitle ?? title ?? "\(theme.typeLabel) Moments"
        let planningItems = listPlanning.isEmpty ? (life?.payload?.planningItems ?? []) : listPlanning
        let bookings = listBookings.isEmpty ? (life?.payload?.bookings ?? []) : listBookings
        let updates = listUpdates.isEmpty ? (life?.payload?.updates ?? []) : listUpdates
        return ScrollView {
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

                ExperienceSectionCard(theme: theme, title: "Timeline") {
                    if planningItems.isEmpty {
                        ExperienceEmptyBlock(
                            theme: theme,
                            message: "No timeline items yet",
                            detail: "Add a planning item from Quick Add — nothing is invented."
                        )
                    } else {
                        ForEach(planningItems.indices, id: \.self) { i in
                            Text(planningItems[i].title ?? planningItems[i].planningItemId ?? "")
                                .font(.plusJakarta(size: 13, weight: .semibold))
                                .foregroundStyle(theme.text)
                                .frame(maxWidth: .infinity, alignment: .leading)
                                .padding(12)
                                .background(theme.bg)
                                .clipShape(RoundedRectangle(cornerRadius: 12))
                                .overlay(RoundedRectangle(cornerRadius: 12).stroke(theme.border))
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

                ExperienceSectionCard(theme: theme, title: "Updates") {
                    if updates.isEmpty {
                        ExperienceEmptyBlock(
                            theme: theme,
                            message: "No updates yet",
                            detail: "Share a status update from Quick Add."
                        )
                    } else {
                        ForEach(updates.prefix(8).indices, id: \.self) { i in
                            Text(updates[i].message ?? updates[i].updateId ?? "")
                                .font(.plusJakarta(size: 13))
                                .foregroundStyle(theme.text)
                        }
                    }
                }

                ExperienceSectionCard(theme: theme, title: "Shared Gallery") {
                    ExperienceEmptyBlock(
                        theme: theme,
                        message: "Gallery empty",
                        detail: "Shared media will appear when group media API is live."
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
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 14)
                            .background(Color.white)
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                    }
                    .buttonStyle(.plain)
                }
                .padding(20)
                .frame(maxWidth: .infinity)
                .background(theme.heroGradient)
                .clipShape(RoundedRectangle(cornerRadius: 20))
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
            .padding(.bottom, 56)
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

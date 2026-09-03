import SwiftUI

/// Figma 601:12875 — Shared Purchase Moments. Live APIs only.
struct PurchaseMomentsActiveView: View {
    let theme: PurchaseActiveTheme
    let refreshToken: UInt64
    let momentId: String?
    let momentTitle: String?
    var onOpenQuickAdd: () -> Void = {}

    @State private var pulse: APIClient.GroupPulsePayload?
    @State private var finance: APIClient.GroupFinancePayload?
    @State private var life: APIClient.GroupLifePayload?
    @State private var listPlanning: [APIClient.GroupLifePayload.LifeInner.PlanningItem] = []
    @State private var listUpdates: [APIClient.GroupLifePayload.LifeInner.UpdateItem] = []
    @State private var purchaseItems: [APIClient.GroupPurchaseItemPayload] = []
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

    @ViewBuilder
    private var content: some View {
        let budgetTotal = finance?.totals?.first?.budgetTotal
        let contributionTotal = finance?.totals?.first?.contributionTotal
        let currency = finance?.totals?.first?.currencyCode ?? "INR"
        let peopleCount = pulse?.payload?.participantCount ?? 0
        let openTasks = pulse?.payload?.openTaskCount ?? life?.payload?.openTaskCount ?? 0
        let displayTitle = momentTitle ?? title ?? "\(theme.typeLabel) Moments"
        let planningItems = listPlanning.isEmpty ? (life?.payload?.planningItems ?? []) : listPlanning
        let updates = listUpdates.isEmpty ? (life?.payload?.updates ?? []) : listUpdates
        let funded = PurchaseFinanceMath.fundedPercent(
            contributionTotal: contributionTotal,
            budgetTotal: budgetTotal
        )
        let gradients = theme.statGradients
        let g0 = gradients.indices.contains(0) ? gradients[0] : [theme.accent, theme.accentSolid]
        let g1 = gradients.indices.contains(1) ? gradients[1] : [theme.accentLight, theme.accent]
        let g2 = gradients.indices.contains(2) ? gradients[2] : [theme.accent, theme.accentSolid]

        NativeDashboardScaffold(background: theme.bg) {


            NativeListSection {

            VStack(alignment: .leading, spacing: 14) {
                if let error {
                    Text(error).font(.caption).foregroundStyle(Color(hex: "#F87171"))
                }

                VStack(alignment: .leading, spacing: 12) {
                    HStack(alignment: .top) {
                        VStack(alignment: .leading, spacing: 6) {
                            Text("SHARED PURCHASE")
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
                        PurchaseStatCard(label: "PEOPLE", value: peopleCount > 0 ? "\(peopleCount)" : "—", colors: g0)
                        PurchaseStatCard(
                            label: "BUDGET",
                            value: GroupFinanceFormat.compactMoney(budgetTotal, currencyCode: currency),
                            colors: g1
                        )
                    }
                    HStack(spacing: 12) {
                        PurchaseStatCard(
                            label: "FUNDED",
                            value: funded.map { "\($0)%" } ?? "—",
                            colors: g2
                        )
                        PurchaseStatCard(
                            label: "ITEMS",
                            value: purchaseItems.isEmpty ? (openTasks > 0 ? "\(openTasks)" : "—") : "\(purchaseItems.count)",
                            colors: g0
                        )
                    }
                }
                .padding(16)
                .background(theme.card)
                .clipShape(RoundedRectangle(cornerRadius: 20))
                .overlay(RoundedRectangle(cornerRadius: 20).stroke(theme.border))

                PurchaseSectionCard(theme: theme, title: "Purchase Items") {
                    if purchaseItems.isEmpty {
                        PurchaseEmptyBlock(
                            theme: theme,
                            message: "No purchase items yet",
                            detail: "Add a purchase item from Quick Add — nothing is invented."
                        )
                    } else {
                        ForEach(purchaseItems) { item in
                            HStack {
                                Text(item.label ?? item.purchaseItemId ?? "Item")
                                    .font(.plusJakarta(size: 13, weight: .semibold))
                                    .foregroundStyle(theme.text)
                                Spacer()
                                Text(GroupFinanceFormat.formatMoney(item.amount, currencyCode: currency))
                                    .font(.plusJakarta(size: 13, weight: .semibold))
                                    .foregroundStyle(theme.accentLight)
                            }
                            .padding(12)
                            .background(theme.bg)
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                            .overlay(RoundedRectangle(cornerRadius: 12).stroke(theme.border))
                        }
                    }
                }

                PurchaseSectionCard(theme: theme, title: "Planning") {
                    if planningItems.isEmpty {
                        PurchaseEmptyBlock(
                            theme: theme,
                            message: "No planning items yet",
                            detail: "Add a planning item when ready — nothing is invented."
                        )
                    } else {
                        ForEach(planningItems.indices, id: \.self) { i in
                            VStack(alignment: .leading, spacing: 4) {
                                Text(planningItems[i].title ?? planningItems[i].planningItemId ?? "")
                                    .font(.plusJakarta(size: 13, weight: .semibold))
                                    .foregroundStyle(theme.text)
                                if let due = planningItems[i].dueAt, !due.isEmpty {
                                    Text(due)
                                        .font(.plusJakarta(size: 11))
                                        .foregroundStyle(theme.secondary)
                                }
                            }
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .padding(12)
                            .background(theme.bg)
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                            .overlay(RoundedRectangle(cornerRadius: 12).stroke(theme.border))
                        }
                    }
                }

                PurchaseSectionCard(theme: theme, title: "Updates") {
                    if updates.isEmpty {
                        PurchaseEmptyBlock(
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

                PurchaseSectionCard(theme: theme, title: "Shared Gallery") {
                    PurchaseEmptyBlock(
                        theme: theme,
                        message: "Gallery empty",
                        detail: "Shared media will appear when group media API is live."
                    )
                }

                VStack(spacing: 16) {
                    Text("Add to the \(theme.typeLabel.lowercased()) story")
                        .font(.plusJakarta(size: 18, weight: .bold))
                        .foregroundStyle(Color.white)
                    Text("Add an item, contribution, memory, poll or update.")
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
            async let updatesResult = APIClient.shared.listGroupUpdates(momentId: momentId)
            async let purchaseResult = APIClient.shared.listPurchaseItems(momentId: momentId)
            let loadedPulse = try await pulseResult
            let finFacet = try await financeResult
            let loadedLife = try await lifeResult
            let loadedFinance = finFacet.payload ?? loadedPulse.payload?.finance
            title = loadedPulse.title
            pulse = loadedPulse
            finance = loadedFinance
            life = loadedLife
            listPlanning = (try? await plansResult)?.items ?? loadedLife.payload?.planningItems ?? []
            listUpdates = (try? await updatesResult)?.items ?? loadedLife.payload?.updates ?? []
            purchaseItems = (try? await purchaseResult)?.items ?? []
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

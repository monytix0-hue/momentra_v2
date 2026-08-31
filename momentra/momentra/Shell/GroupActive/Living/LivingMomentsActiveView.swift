import SwiftUI

/// Shared Living Moments (G09–G12). Live APIs only.
struct LivingMomentsActiveView: View {
    let theme: LivingActiveTheme
    let refreshToken: UInt64
    let momentId: String?
    let momentTitle: String?
    var onOpenQuickAdd: () -> Void = {}

    @State private var pulse: APIClient.GroupPulsePayload?
    @State private var finance: APIClient.GroupFinancePayload?
    @State private var life: APIClient.GroupLifePayload?
    @State private var listPlanning: [APIClient.GroupLifePayload.LifeInner.PlanningItem] = []
    @State private var listUpdates: [APIClient.GroupLifePayload.LifeInner.UpdateItem] = []
    @State private var residents: [APIClient.GroupResidentPayload] = []
    @State private var sharedAssets: [APIClient.GroupSharedAssetPayload] = []
    @State private var maintenance: [APIClient.GroupMaintenanceRecordPayload] = []
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
        let contributionTotal = finance?.totals?.first?.contributionTotal
        let currency = finance?.totals?.first?.currencyCode ?? "INR"
        let peopleCount = !residents.isEmpty
            ? residents.count
            : (pulse?.payload?.participantCount ?? 0)
        let openTasks = pulse?.payload?.openTaskCount ?? life?.payload?.openTaskCount ?? 0
        let displayTitle = momentTitle ?? title ?? "\(theme.typeLabel) Moments"
        let planningItems = listPlanning.isEmpty ? (life?.payload?.planningItems ?? []) : listPlanning
        let updates = listUpdates.isEmpty ? (life?.payload?.updates ?? []) : listUpdates
        let funded = LivingFinanceMath.fundedPercent(
            contributionTotal: contributionTotal,
            budgetTotal: budgetTotal
        )
        let gradients = theme.statGradients
        let g0 = gradients.indices.contains(0) ? gradients[0] : [theme.accent, theme.accentSolid]
        let g1 = gradients.indices.contains(1) ? gradients[1] : [theme.accentLight, theme.accent]
        let g2 = gradients.indices.contains(2) ? gradients[2] : [theme.accent, theme.accentSolid]

        return ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                if let error {
                    Text(error).font(.caption).foregroundStyle(Color(hex: "#F87171"))
                }

                VStack(alignment: .leading, spacing: 12) {
                    HStack(alignment: .top) {
                        VStack(alignment: .leading, spacing: 6) {
                            Text("SHARED LIVING")
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
                        LivingStatCard(label: "RESIDENTS", value: peopleCount > 0 ? "\(peopleCount)" : "—", colors: g0)
                        LivingStatCard(
                            label: "RENT/MO",
                            value: GroupFinanceFormat.compactMoney(budgetTotal, currencyCode: currency),
                            colors: g1
                        )
                    }
                    HStack(spacing: 12) {
                        LivingStatCard(
                            label: "COLLECTED",
                            value: funded.map { "\($0)%" } ?? "—",
                            colors: g2
                        )
                        LivingStatCard(
                            label: "TASKS",
                            value: openTasks > 0 ? "\(openTasks)" : (planningItems.isEmpty ? "—" : "\(planningItems.count)"),
                            colors: g0
                        )
                    }
                }
                .padding(16)
                .background(theme.card)
                .clipShape(RoundedRectangle(cornerRadius: 20))
                .overlay(RoundedRectangle(cornerRadius: 20).stroke(theme.border))

                LivingSectionCard(theme: theme, title: "Residents") {
                    if residents.isEmpty {
                        LivingEmptyBlock(
                            theme: theme,
                            message: "No residents yet",
                            detail: "Add a resident from Quick Add — nothing is invented."
                        )
                    } else {
                        ForEach(residents) { resident in
                            HStack {
                                Text(resident.name ?? "Resident")
                                    .font(.plusJakarta(size: 13, weight: .semibold))
                                    .foregroundStyle(theme.text)
                                Spacer()
                                if let role = resident.roleCode, !role.isEmpty {
                                    Text(role)
                                        .font(.plusJakarta(size: 11, weight: .semibold))
                                        .foregroundStyle(theme.secondary)
                                }
                            }
                            .padding(12)
                            .background(theme.bg)
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                            .overlay(RoundedRectangle(cornerRadius: 12).stroke(theme.border))
                        }
                    }
                }

                LivingSectionCard(theme: theme, title: "Planning & Tasks") {
                    if planningItems.isEmpty {
                        LivingEmptyBlock(
                            theme: theme,
                            message: "No planning items yet",
                            detail: "Add a task when ready — nothing is invented."
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

                LivingSectionCard(theme: theme, title: "Shared Assets") {
                    if sharedAssets.isEmpty {
                        LivingEmptyBlock(
                            theme: theme,
                            message: "No shared assets yet",
                            detail: "Add a household asset from Quick Add."
                        )
                    } else {
                        ForEach(sharedAssets) { asset in
                            HStack {
                                Text(asset.title ?? "Asset")
                                    .font(.plusJakarta(size: 13, weight: .semibold))
                                    .foregroundStyle(theme.text)
                                Spacer()
                                if let status = asset.status, !status.isEmpty {
                                    Text(status)
                                        .font(.plusJakarta(size: 11, weight: .semibold))
                                        .foregroundStyle(theme.accentLight)
                                }
                            }
                            .padding(12)
                            .background(theme.bg)
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                            .overlay(RoundedRectangle(cornerRadius: 12).stroke(theme.border))
                        }
                    }
                }

                LivingSectionCard(theme: theme, title: "Maintenance") {
                    if maintenance.isEmpty {
                        LivingEmptyBlock(
                            theme: theme,
                            message: "No maintenance records",
                            detail: "Log maintenance from Quick Add when something needs care."
                        )
                    } else {
                        ForEach(maintenance) { record in
                            VStack(alignment: .leading, spacing: 4) {
                                Text(record.title ?? "Maintenance")
                                    .font(.plusJakarta(size: 13, weight: .semibold))
                                    .foregroundStyle(theme.text)
                                if let desc = record.description, !desc.isEmpty {
                                    Text(desc)
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

                LivingSectionCard(theme: theme, title: "Updates") {
                    if updates.isEmpty {
                        LivingEmptyBlock(
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

                LivingSectionCard(theme: theme, title: "Shared Gallery") {
                    LivingEmptyBlock(
                        theme: theme,
                        message: "Gallery empty",
                        detail: "Shared media will appear when group media API is live."
                    )
                }

                VStack(spacing: 16) {
                    Text("Add to the \(theme.typeLabel.lowercased()) story")
                        .font(.plusJakarta(size: 18, weight: .bold))
                        .foregroundStyle(Color.white)
                    Text("Add a resident, expense, task, asset or memory.")
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
            async let updatesResult = APIClient.shared.listGroupUpdates(momentId: momentId)
            async let residentsResult = APIClient.shared.listResidents(momentId: momentId)
            async let assetsResult = APIClient.shared.listSharedAssets(momentId: momentId)
            async let maintenanceResult = APIClient.shared.listMaintenanceRecords(momentId: momentId)
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
            residents = (try? await residentsResult)?.items ?? []
            sharedAssets = (try? await assetsResult)?.items ?? []
            maintenance = (try? await maintenanceResult)?.items ?? []
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

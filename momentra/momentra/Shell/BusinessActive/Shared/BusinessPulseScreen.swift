import SwiftUI

struct BusinessPulseScreen: View {
    let family: BusinessMomentFamily
    let refreshToken: UInt64
    let momentTitle: String?
    let momentId: String?
    var momentTypeCode: String?
    var capabilities: [String]? = nil
    var onToday: (BusinessQuickAddKind) -> Void = { _ in }
    var onOpenMoments: () -> Void = {}

    @State private var tab: BusinessTabDataCache.PulseTab?
    @State private var loading = true
    @State private var error: String?
    @State private var smallShop = false

    private var theme: BusinessActiveTheme {
        BusinessActiveTheme.forTypeCode(momentTypeCode ?? familyCode)
    }

    private var familyCode: String {
        switch family {
        case .money: return "BUSINESS_RUNWAY"
        case .daily: return "BUSINESS_OPERATIONS"
        case .team: return "TEAM_OPERATIONS"
        }
    }

    private var presentation: BusinessPulsePresentation? {
        guard let tab else { return nil }
        let totals = tab.finance?.totals?.first ?? tab.pulse.payload?.finance?.totals?.first
        let cash = tab.life?.payload?.runwayPayload?["availableCash"]?.value as? String
        let ops = tab.pulse.payload?.operations
        let cards = tab.activities.map { businessMomentCard(from: $0) }
        return buildBusinessPulsePresentation(BusinessPulseFactsInput(
            family: family,
            smallShop: smallShop,
            capabilities: capabilities,
            momentTypeCode: momentTypeCode,
            financeQuality: tab.finance?.dataQuality ?? tab.pulse.payload?.finance?.dataQuality,
            revenueTotal: totals?.revenueTotal,
            expenseTotal: totals?.expenseTotal,
            invoiceOutstandingTotal: totals?.invoiceOutstandingTotal,
            runwayMonths: tab.pulse.payload?.runwayMonths,
            availableCash: cash,
            lifeFailed: tab.lifeFailed,
            openIssueCount: ops?.openIssueCount,
            activeVendorCount: ops?.activeVendorCount,
            monthlySpend: ops?.monthlySpend,
            slaCompliancePct: ops?.slaCompliancePct,
            sectionQuality: ops?.sectionQuality ?? [:],
            needsAttention: tab.needsAttention.map { ($0.id, $0.title) },
            rosterCount: tab.rosterCount,
            approvalTitles: tab.approvalTitles?.map { ($0.id, $0.title) },
            issueTitles: tab.issueTitles?.map { ($0.id, $0.title) },
            activities: cards
        ))
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                Text(presentation?.familyTitle ?? theme.typeLabel)
                    .font(.plusJakarta(size: 12, weight: .bold))
                    .foregroundStyle(theme.secondary)
                if let momentTitle, !momentTitle.isEmpty {
                    Text(momentTitle)
                        .font(.plusJakarta(size: 22, weight: .heavy))
                        .foregroundStyle(.white)
                        .lineLimit(1)
                }
                if let error, presentation == nil {
                    Text(error)
                        .font(.plusJakarta(size: 13))
                        .foregroundStyle(theme.secondary)
                }
                if let model = presentation {
                    Text("Now")
                        .font(.plusJakarta(size: 16, weight: .bold))
                        .foregroundStyle(.white)
                    factBlock(model)
                    todayBlock(model.today)
                    attentionBlock(model)
                    recentBlock(model)
                    Text("Snapshot")
                        .font(.plusJakarta(size: 16, weight: .bold))
                        .foregroundStyle(.white)
                    factBlock(model)
                    if family == .money {
                        Text("Not financial, legal, or tax advice. Figures are for your records only.")
                            .font(.plusJakarta(size: 11))
                            .foregroundStyle(theme.muted)
                            .padding(.top, 4)
                            .accessibilityIdentifier("finance.disclaimer")
                    }
                } else if loading {
                    ProgressView().tint(theme.accent).frame(maxWidth: .infinity)
                }
            }
            .padding(16)
        }
        .background(theme.bg)
        .task(id: "\(refreshToken)-\(momentId ?? "")-\(family.rawValue)") {
            await reload()
        }
    }

    @ViewBuilder
    private func factBlock(_ model: BusinessPulsePresentation) -> some View {
        let facts = model.displayedFacts()
        VStack(alignment: .leading, spacing: 8) {
            if model.showsCompanyTotals() {
                Text(BusinessPulsePresentation.companyTotals)
                    .font(.plusJakarta(size: 11, weight: .semibold))
                    .foregroundStyle(theme.muted)
            }
            if facts.isEmpty {
                Text("No figures yet.")
                    .font(.plusJakarta(size: 14))
                    .foregroundStyle(theme.secondary)
            } else {
                HStack(spacing: 8) {
                    ForEach(Array(facts.enumerated()), id: \.offset) { _, fact in
                        factTile(fact)
                    }
                }
            }
        }
    }

    private func factTile(_ fact: PulseFact) -> some View {
        let value: String = {
            switch fact.metric {
            case .available(let text): return text
            case .zero: return "0"
            case .unavailable: return ""
            }
        }()
        return VStack(alignment: .leading, spacing: 4) {
            Text(fact.label)
                .font(.plusJakarta(size: 11))
                .foregroundStyle(theme.muted)
            Text(value)
                .font(.plusJakarta(size: 16, weight: .bold))
                .foregroundStyle(.white)
                .lineLimit(2)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(12)
        .background(theme.card)
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(theme.border))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    @ViewBuilder
    private func todayBlock(_ kinds: [BusinessQuickAddKind]) -> some View {
        if !kinds.isEmpty {
            VStack(alignment: .leading, spacing: 8) {
                Text("Today")
                    .font(.plusJakarta(size: 16, weight: .bold))
                    .foregroundStyle(.white)
                HStack(spacing: 8) {
                    ForEach(kinds) { kind in
                        Button {
                            onToday(kind)
                        } label: {
                            Text(kind.label(smallShop: smallShop))
                                .font(.plusJakarta(size: 13, weight: .semibold))
                                .foregroundStyle(.white)
                                .multilineTextAlignment(.center)
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 14)
                                .background(theme.accent.opacity(0.18))
                                .clipShape(RoundedRectangle(cornerRadius: 14))
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
        }
    }

    @ViewBuilder
    private func attentionBlock(_ model: BusinessPulsePresentation) -> some View {
        if !model.attention.isEmpty || model.attentionEmptyLine != nil {
            VStack(alignment: .leading, spacing: 8) {
                Text("Needs attention")
                    .font(.plusJakarta(size: 16, weight: .bold))
                    .foregroundStyle(.white)
                if model.attention.isEmpty {
                    Text(model.attentionEmptyLine ?? "")
                        .font(.plusJakarta(size: 13))
                        .foregroundStyle(theme.secondary)
                } else {
                    ForEach(model.attention) { item in
                        Text(item.title)
                            .font(.plusJakarta(size: 14))
                            .foregroundStyle(theme.text)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .padding(12)
                            .background(theme.card)
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                    }
                }
            }
        }
    }

    private func recentBlock(_ model: BusinessPulsePresentation) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text(BusinessPulsePresentation.recentTitle)
                    .font(.plusJakarta(size: 16, weight: .bold))
                    .foregroundStyle(.white)
                Spacer()
                Button("See all", action: onOpenMoments)
                    .font(.plusJakarta(size: 13, weight: .semibold))
                    .foregroundStyle(theme.accent)
            }
            if model.recent.isEmpty {
                Text("Nothing recorded yet.")
                    .font(.plusJakarta(size: 13))
                    .foregroundStyle(theme.secondary)
            } else {
                ForEach(model.recent) { card in
                    VStack(alignment: .leading, spacing: 4) {
                        Text(card.title)
                            .font(.plusJakarta(size: 14))
                            .foregroundStyle(theme.text)
                        let detail = [card.amountLabel, card.occurredAt].compactMap { $0 }.joined(separator: " · ")
                        if !detail.isEmpty {
                            Text(detail)
                                .font(.plusJakarta(size: 12))
                                .foregroundStyle(theme.muted)
                        }
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(12)
                    .background(theme.card)
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                }
            }
        }
    }

    private func reload() async {
        guard let momentId, !momentId.isEmpty else {
            loading = false
            tab = nil
            error = "Select a Business Moment."
            return
        }
        smallShop = await BusinessAudience.isSmallShopMoment(momentId: momentId, fallbackCompanyId: nil)
        error = nil
        if let cached = BusinessTabDataCache.peekPulse(momentId) {
            tab = cached
            loading = false
        }
        do {
            let loaded = try await BusinessTabLoad.loadPulseTab(momentId: momentId, family: family)
            tab = loaded
            loading = false
        } catch is CancellationError {
            loading = false
        } catch {
            self.error = error.localizedDescription
            loading = false
        }
    }
}

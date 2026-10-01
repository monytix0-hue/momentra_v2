import SwiftUI

/// Company Memory. The payload is the company. The selected moment only sets the lens.
struct BusinessMemoryActiveView: View {
    let refreshToken: UInt64
    let momentId: String?
    let momentTitle: String?
    var momentTypeCode: String? = nil
    var companyId: String? = nil
    var onOpenQuickAdd: () -> Void = {}

    @State private var memory: APIClient.BusinessMemoryPayload?
    @State private var shownCompany: String?
    @State private var loading = true
    @State private var error: String?
    @State private var lens: BusinessLifeLens = .overview
    @State private var retry = 0

    private var presentation: BusinessMemoryPresentation {
        buildBusinessMemoryPresentation(
            items: businessMemoryFacts(from: memory),
            now: Date(),
            zone: .current
        )
    }

    private var worth: [BusinessMemoryWorthItem] {
        filterWorthRemembering(presentation.worthRemembering, lens: lens)
    }

    var body: some View {
        Group {
            if loading && memory == nil {
                ProgressView().tint(CompanyLifeColors.indigo)
            } else {
                NativeDashboardScaffold(background: CompanyLifeColors.bg) {
                    NativeListSection {
                        VStack(alignment: .leading, spacing: 16) {
                            Text("Company memory")
                                .font(.plusJakarta(size: 22, weight: .semibold))
                                .foregroundStyle(CompanyLifeColors.text)
                            if let error {
                                Text(error)
                                    .font(.caption)
                                    .foregroundStyle(CompanyLifeColors.red)
                                Button("Try again") { retry += 1 }
                                    .font(.caption)
                            }
                            lensChips
                            memoryCard("Memory") {
                                Text(presentation.heroPeriod)
                                    .font(.plusJakarta(size: 12))
                                    .foregroundStyle(CompanyLifeColors.secondary)
                                Text(presentation.heroSentence)
                                    .font(.plusJakarta(size: 16, weight: .semibold))
                                    .foregroundStyle(CompanyLifeColors.text)
                            }
                            memoryCard("Worth remembering") {
                                if worth.isEmpty {
                                    Text(businessMemoryWorthEmpty)
                                        .font(.plusJakarta(size: 14))
                                        .foregroundStyle(CompanyLifeColors.secondary)
                                } else {
                                    ForEach(Array(worth.enumerated()), id: \.offset) { _, item in
                                        VStack(alignment: .leading, spacing: 2) {
                                            Text(item.title)
                                                .font(.plusJakarta(size: 14, weight: .semibold))
                                                .foregroundStyle(CompanyLifeColors.text)
                                            let meta = [item.familyLabel, item.dateLabel ?? ""].filter { !$0.isEmpty }.joined(separator: " · ")
                                            if !meta.isEmpty {
                                                Text(meta)
                                                    .font(.plusJakarta(size: 12))
                                                    .foregroundStyle(CompanyLifeColors.secondary)
                                            }
                                        }
                                    }
                                }
                            }
                            if let pattern = presentation.pattern {
                                memoryCard("Pattern worth knowing") {
                                    Text(pattern.title)
                                        .font(.plusJakarta(size: 14, weight: .semibold))
                                        .foregroundStyle(CompanyLifeColors.text)
                                    Text("See evidence")
                                        .font(.plusJakarta(size: 12))
                                        .foregroundStyle(CompanyLifeColors.secondary)
                                    ForEach(Array(pattern.evidence.enumerated()), id: \.offset) { _, title in
                                        Text(title)
                                            .font(.plusJakarta(size: 13))
                                            .foregroundStyle(CompanyLifeColors.text)
                                    }
                                }
                            }
                            memoryCard("What worked / What didn't") {
                                Text("What worked")
                                    .font(.plusJakarta(size: 13, weight: .semibold))
                                    .foregroundStyle(CompanyLifeColors.text)
                                if presentation.worked.isEmpty {
                                    Text(businessMemoryWorkedEmpty)
                                        .font(.plusJakarta(size: 13))
                                        .foregroundStyle(CompanyLifeColors.secondary)
                                } else {
                                    ForEach(Array(presentation.worked.enumerated()), id: \.offset) { _, title in
                                        Text(title).font(.plusJakarta(size: 13)).foregroundStyle(CompanyLifeColors.text)
                                    }
                                }
                                Text("What didn't")
                                    .font(.plusJakarta(size: 13, weight: .semibold))
                                    .foregroundStyle(CompanyLifeColors.text)
                                if presentation.didnt.isEmpty {
                                    Text(businessMemoryDidntEmpty)
                                        .font(.plusJakarta(size: 13))
                                        .foregroundStyle(CompanyLifeColors.secondary)
                                } else {
                                    ForEach(Array(presentation.didnt.enumerated()), id: \.offset) { _, title in
                                        Text(title).font(.plusJakarta(size: 13)).foregroundStyle(CompanyLifeColors.text)
                                    }
                                }
                            }
                            if let comparison = presentation.thenNow {
                                memoryCard("Then → Now") {
                                    Text("Then")
                                        .font(.plusJakarta(size: 12))
                                        .foregroundStyle(CompanyLifeColors.secondary)
                                    Text(comparison.thenTitle)
                                        .font(.plusJakarta(size: 14, weight: .semibold))
                                        .foregroundStyle(CompanyLifeColors.text)
                                    Text(comparison.thenDate)
                                        .font(.plusJakarta(size: 12))
                                        .foregroundStyle(CompanyLifeColors.secondary)
                                    Text("Now")
                                        .font(.plusJakarta(size: 12))
                                        .foregroundStyle(CompanyLifeColors.secondary)
                                    Text(comparison.nowTitle)
                                        .font(.plusJakarta(size: 14, weight: .semibold))
                                        .foregroundStyle(CompanyLifeColors.text)
                                    Text(comparison.nowDate)
                                        .font(.plusJakarta(size: 12))
                                        .foregroundStyle(CompanyLifeColors.secondary)
                                }
                            }
                            Button("Record learning", action: onOpenQuickAdd)
                                .font(.plusJakarta(size: 14, weight: .semibold))
                                .foregroundStyle(CompanyLifeColors.text)
                                .padding(.horizontal, 14)
                                .padding(.vertical, 10)
                                .background(CompanyLifeColors.indigoSolid)
                                .clipShape(RoundedRectangle(cornerRadius: 12))
                        }
                    }
                }
            }
        }
        .background(CompanyLifeColors.bg)
        .onChange(of: companyId) { _, next in
            if let next, !next.isEmpty, let shownCompany, next != shownCompany {
                memory = nil
                error = nil
                self.shownCompany = next
            }
        }
        .onChange(of: momentTypeCode) { _, code in
            lens = businessLifeLens(momentTypeCode: code)
        }
        .onAppear { lens = businessLifeLens(momentTypeCode: momentTypeCode) }
        .task(id: "\(refreshToken)-\(momentId ?? "")-\(companyId ?? "")-\(retry)") { await load() }
    }

    private var lensChips: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(BusinessLifeLens.allCases, id: \.self) { item in
                    Button(item.label) { lens = item }
                        .font(.plusJakarta(size: 13, weight: .semibold))
                        .foregroundStyle(item == lens ? CompanyLifeColors.text : CompanyLifeColors.secondary)
                        .padding(.horizontal, 14)
                        .padding(.vertical, 8)
                        .background(item == lens ? CompanyLifeColors.indigoSolid : CompanyLifeColors.card)
                        .clipShape(Capsule())
                }
            }
        }
    }

    @ViewBuilder
    private func memoryCard<Content: View>(_ title: String, @ViewBuilder content: () -> Content) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title)
                .font(.plusJakarta(size: 15, weight: .bold))
                .foregroundStyle(CompanyLifeColors.text)
            content()
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(CompanyLifeColors.card)
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private func load() async {
        guard let momentId, !momentId.isEmpty else {
            loading = false
            memory = nil
            error = "Select a Business Moment."
            return
        }
        if memory == nil, let cached = BusinessTabDataCache.peekMemory(momentId)?.memory {
            memory = cached
        }
        loading = memory == nil
        error = nil
        do {
            let facet = try await APIClient.shared.getBusinessMemory(momentId: momentId)
            let incoming = facet.companyId
            if let companyId, !companyId.isEmpty, let incoming, !incoming.isEmpty, incoming != companyId {
                loading = false
                return
            }
            if let shownCompany, !shownCompany.isEmpty, let incoming, !incoming.isEmpty, incoming != shownCompany, companyId == nil || companyId?.isEmpty == true {
                loading = false
                return
            }
            memory = facet
            shownCompany = incoming ?? companyId
            let previous = BusinessTabDataCache.peekMemory(momentId)
            BusinessTabDataCache.putMemory(
                momentId,
                BusinessTabDataCache.MemoryTab(
                    memory: facet,
                    pulse: previous?.pulse,
                    finance: previous?.finance,
                    life: previous?.life
                )
            )
        } catch {
            self.error = error.localizedDescription
        }
        loading = false
    }
}

func businessMemoryFacts(from payload: APIClient.BusinessMemoryPayload?) -> [BusinessMemoryItemFact] {
    (payload?.payload?.items ?? []).map { item in
        BusinessMemoryItemFact(
            title: item.title ?? "",
            body: item.body,
            occurredAt: item.occurredAt,
            memoryType: item.memoryType,
            businessFamily: item.businessFamily
        )
    }
}

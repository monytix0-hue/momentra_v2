import SwiftUI

/// Figma 601:13047 — Shared Purchase Memory. Live APIs only.
struct PurchaseMemoryActiveView: View {
    let theme: PurchaseActiveTheme
    let refreshToken: UInt64
    let momentId: String?
    let momentTitle: String?
    var onOpenQuickAdd: () -> Void = {}

    @State private var memory: APIClient.GroupMemoryPayload?
    @State private var finance: APIClient.GroupFinancePayload?
    @State private var pulse: APIClient.GroupPulsePayload?
    @State private var participants: [APIClient.GroupParticipantPayload] = []
    @State private var listMemoryItems: [APIClient.GroupMemoryPayload.MemoryInner.GroupMemoryItem] = []
    @State private var loading = true
    @State private var error: String?

    var body: some View {
        Group {
            if loading && pulse == nil && finance == nil && memory == nil {
                ProgressView().tint(theme.accent)
            } else {
                content
            }
        }
        .background(theme.bg)
        .task(id: "\(refreshToken)-\(momentId ?? "")") { await load() }
    }

    private var nameById: [String: String] {
        Dictionary(uniqueKeysWithValues: participants.map {
            ($0.participantId, $0.displayName ?? String($0.participantId.prefix(8)))
        })
    }

    @ViewBuilder
    private var content: some View {
        let total = finance?.totals?.first
        let currency = total?.currencyCode ?? "INR"
        let facetItems = memory?.payload?.items ?? []
        let items = listMemoryItems.isEmpty ? facetItems : listMemoryItems
        let memoryCount = max(items.count, memory?.payload?.memoryCount ?? 0)
        let people = pulse?.payload?.participantCount ?? 0
        let displayTitle = momentTitle ?? "\(theme.typeLabel) Memory"
        let funded = PurchaseFinanceMath.fundedPercent(
            contributionTotal: total?.contributionTotal,
            budgetTotal: total?.budgetTotal
        )
        let positions = finance?.positions ?? []
        NativeDashboardScaffold(background: theme.bg) {

            NativeListSection {

            VStack(alignment: .leading, spacing: 14) {
                if let error {
                    Text(error).font(.caption).foregroundStyle(Color(hex: "#F87171"))
                }

                VStack(alignment: .leading, spacing: 10) {
                    Text("GROUP MEMORY")
                        .font(.plusJakarta(size: 11, weight: .bold))
                        .foregroundStyle(Color.white.opacity(0.9))
                    Text(displayTitle)
                        .font(.plusJakarta(size: 22, weight: .heavy))
                        .foregroundStyle(Color.white)
                    Text(people > 0 ? "\(people) people shaped this purchase" : "People appear when participants are live")
                        .font(.plusJakarta(size: 13))
                        .foregroundStyle(Color.white.opacity(0.9))
                    Text(memoryCount > 0 ? "\(memoryCount) memories captured" : "No memories yet")
                        .font(.plusJakarta(size: 11, weight: .bold))
                        .foregroundStyle(theme.darkText)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 6)
                        .background(Color.white.opacity(0.9))
                        .clipShape(Capsule())
                }
                .padding(20)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(theme.heroGradient)
                .clipShape(RoundedRectangle(cornerRadius: 20))

                PurchaseSectionCard(theme: theme, title: "Memory Timeline") {
                    if items.isEmpty {
                        PurchaseEmptyBlock(
                            theme: theme,
                            message: "Timeline empty",
                            detail: "Shared memories will appear here — nothing is invented."
                        )
                    } else {
                        ForEach(items) { item in
                            HStack(spacing: 10) {
                                if let thumb = item.primaryDownloadUrl, !thumb.isEmpty {
                                    MemoryMediaThumb(
                                        urlString: thumb,
                                        size: 40,
                                        border: theme.accent.opacity(0.35),
                                        field: theme.accentSoft
                                    )
                                }
                                Text(item.title ?? "Memory")
                                    .font(.plusJakarta(size: 13))
                                    .foregroundStyle(theme.text)
                                    .frame(maxWidth: .infinity, alignment: .leading)
                            }
                            .padding(12)
                            .background(theme.accentSoft)
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                            .overlay(
                                RoundedRectangle(cornerRadius: 12)
                                    .stroke(theme.accent.opacity(0.35))
                            )
                        }
                    }
                }

                PurchaseSectionCard(theme: theme, title: "Memory Gallery") {
                    MemoryPhotoGalleryStrip(
                        items: items,
                        emptyMessage: "No photos yet",
                        emptyDetail: "Add a memory with a photo from Quick Add.",
                        text: theme.text,
                        muted: theme.secondary,
                        field: theme.card,
                        border: theme.border
                    )
                }

                PurchaseSectionCard(theme: theme, title: "People Impact") {
                    GroupMetricTile(label: "Participants", value: people > 0 ? "\(people)" : "—")
                    if positions.isEmpty {
                        PurchaseEmptyBlock(
                            theme: theme,
                            message: "No contributors yet",
                            detail: "People appear after contributions and activity are recorded."
                        )
                        .padding(.top, 8)
                    } else {
                        VStack(alignment: .leading, spacing: 6) {
                            ForEach(Array(positions.prefix(3))) { pos in
                                HStack {
                                    Text(nameById[pos.participantId] ?? String(pos.participantId.prefix(8)))
                                        .font(.plusJakarta(size: 12))
                                        .foregroundStyle(theme.text)
                                    Spacer()
                                    Text(GroupFinanceFormat.formatMoney(
                                        pos.contributionTotal ?? pos.netPosition,
                                        currencyCode: pos.currencyCode
                                    ))
                                        .font(.plusJakarta(size: 12, weight: .semibold))
                                        .foregroundStyle(theme.secondary)
                                }
                                .padding(.top, 6)
                            }
                        }
                    }
                }

                PurchaseSectionCard(theme: theme, title: theme.budgetTitle) {
                    HStack(spacing: 8) {
                        GroupMetricTile(
                            label: "Budget",
                            value: GroupFinanceFormat.formatMoney(total?.budgetTotal, currencyCode: currency)
                        )
                        GroupMetricTile(
                            label: "Collected",
                            value: GroupFinanceFormat.formatMoney(total?.contributionTotal, currencyCode: currency)
                        )
                    }
                    if let funded {
                        Text("\(funded)% funded")
                            .font(.plusJakarta(size: 12, weight: .semibold))
                            .foregroundStyle(theme.accentLight)
                            .padding(.top, 8)
                        GroupProgressBar(percent: funded)
                    } else {
                        Text("Set a budget and record contributions to track funding.")
                            .font(.plusJakarta(size: 12))
                            .foregroundStyle(theme.secondary)
                            .padding(.top, 8)
                    }
                }

                PurchaseSectionCard(theme: theme, title: "Memory Intelligence") {
                    PurchaseEmptyBlock(
                        theme: theme,
                        message: "Insights coming soon",
                        detail: "AI memory intelligence for groups is on the roadmap — no invented copy."
                    )
                }

                VStack(spacing: 16) {
                    Text("Preserve this moment")
                        .font(.plusJakarta(size: 18, weight: .bold))
                        .foregroundStyle(Color.white)
                    Text("Capture a memory, photo, or update for the \(theme.typeLabel.lowercased()) story.")
                        .font(.plusJakarta(size: 13))
                        .foregroundStyle(Color.white.opacity(0.9))
                    Button(action: onOpenQuickAdd) {
                        Text("+ Capture Memory")
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
        if let cachedPulse = GroupTabDataCache.peekPulse(momentId) {
            finance = cachedPulse.finance
            pulse = cachedPulse.pulse
            loading = false
        }
        if let cachedMemory = GroupTabDataCache.peekMemory(momentId) {
            memory = cachedMemory.memory
            finance = cachedMemory.finance ?? finance
            pulse = cachedMemory.pulse ?? pulse
            participants = cachedMemory.participants
            loading = false
        }
        if pulse == nil && finance == nil { loading = true }

        do {
            let cached = GroupTabDataCache.peekPulse(momentId)
            async let memoryResult = APIClient.shared.getGroupMemory(momentId: momentId)
            async let listResult = APIClient.shared.listGroupMemories(momentId: momentId)
            async let partsResult = APIClient.shared.listGroupParticipants(momentId: momentId)
            let loadedMemory = try await memoryResult
            listMemoryItems = (try? await listResult)?.items ?? []
            let loadedParts = (try? await partsResult) ?? []
            var loadedFinance = cached?.finance
            var loadedPulse = cached?.pulse
            if loadedFinance == nil {
                loadedFinance = try await APIClient.shared.getGroupFinance(momentId: momentId).payload
            }
            if loadedPulse == nil {
                loadedPulse = try await APIClient.shared.getGroupPulse(momentId: momentId)
            }
            memory = loadedMemory
            finance = loadedFinance
            pulse = loadedPulse
            participants = loadedParts
            GroupTabDataCache.putMemory(momentId, .init(
                memory: loadedMemory,
                finance: loadedFinance,
                pulse: loadedPulse,
                participants: loadedParts
            ))
        } catch is CancellationError {
            // Tab/refresh cancelled an in-flight load — not an API outage.
        } catch {
            if Task.isCancelled { return }
            let ns = error as NSError
            if ns.domain == NSURLErrorDomain && ns.code == NSURLErrorCancelled { return }
            self.error = error.localizedDescription
        }
        loading = false
    }
}

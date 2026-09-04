import SwiftUI

/// Figma 575:15203 — Wedding Memory. Live APIs only; no demo seeds.
struct WeddingMemoryActiveView: View {
    let refreshToken: UInt64
    let momentId: String?
    let momentTitle: String?
    var onOpenQuickAdd: () -> Void = {}

    @State private var memory: APIClient.GroupMemoryPayload?
    @State private var finance: APIClient.GroupFinancePayload?
    @State private var pulse: APIClient.GroupPulsePayload?
    @State private var listMemoryItems: [APIClient.GroupMemoryPayload.MemoryInner.GroupMemoryItem] = []
    @State private var loading = true
    @State private var error: String?

    var body: some View {
        Group {
            if loading && pulse == nil && finance == nil && memory == nil {
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
        let total = finance?.totals?.first
        let currency = total?.currencyCode ?? "INR"
        let facetItems = memory?.payload?.items ?? []
        let items = listMemoryItems.isEmpty ? facetItems : listMemoryItems
        let memoryCount = max(items.count, memory?.payload?.memoryCount ?? 0)
        let people = pulse?.payload?.participantCount ?? 0
        let displayTitle = momentTitle ?? "Group Memory"
        let utilization = GroupFinanceFormat.utilizationPercent(
            expenseTotal: total?.expenseTotal,
            budgetTotal: total?.budgetTotal
        )
        let positions = finance?.positions ?? []
        NativeDashboardScaffold(background: WeddingActiveTheme.bg) {

            NativeListSection {

            VStack(alignment: .leading, spacing: 14) {
                if let error {
                    Text(error).font(.caption).foregroundStyle(Color(hex: "#F87171"))
                }

                HStack(alignment: .top, spacing: 16) {
                    VStack(alignment: .leading, spacing: 10) {
                        Text("GROUP MEMORY")
                            .font(.plusJakarta(size: 11, weight: .bold))
                            .foregroundStyle(WeddingActiveTheme.darkText.opacity(0.9))
                        Text(displayTitle)
                            .font(.plusJakarta(size: 22, weight: .heavy))
                            .foregroundStyle(WeddingActiveTheme.darkText)
                        Text("\(people) people shaped this moment")
                            .font(.plusJakarta(size: 13))
                            .foregroundStyle(WeddingActiveTheme.darkText.opacity(0.85))
                        Text(memoryCount > 0 ? "\(memoryCount) memories captured" : "No memories yet")
                            .font(.plusJakarta(size: 11, weight: .bold))
                            .foregroundStyle(WeddingActiveTheme.darkText)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 6)
                            .background(Color.white.opacity(0.9))
                            .clipShape(Capsule())
                    }
                    Spacer()
                    if let _ = UIImage(named: "WeddingHubCake") {
                        Image("WeddingHubCake")
                            .resizable()
                            .scaledToFit()
                            .frame(width: 80, height: 80)
                    }
                }
                .padding(20)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(WeddingActiveTheme.heroGradient)
                .clipShape(RoundedRectangle(cornerRadius: 20))

                WeddingSectionCard(title: "Memory Timeline") {
                    if items.isEmpty {
                        WeddingEmptyBlock(
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
                                        border: WeddingActiveTheme.accent.opacity(0.35),
                                        field: WeddingActiveTheme.accentSoft
                                    )
                                }
                                Text(item.title ?? "Memory")
                                    .font(.plusJakarta(size: 13))
                                    .foregroundStyle(WeddingActiveTheme.text)
                                    .frame(maxWidth: .infinity, alignment: .leading)
                            }
                            .padding(12)
                            .background(WeddingActiveTheme.accentSoft)
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                            .overlay(
                                RoundedRectangle(cornerRadius: 12)
                                    .stroke(WeddingActiveTheme.accent.opacity(0.35))
                            )
                        }
                    }
                }

                WeddingSectionCard(title: "Memory Gallery") {
                    MemoryPhotoGalleryStrip(
                        items: items,
                        emptyMessage: "No photos yet",
                        emptyDetail: "Add a memory with a photo from Quick Add.",
                        text: WeddingActiveTheme.text,
                        muted: WeddingActiveTheme.secondary,
                        field: WeddingActiveTheme.card,
                        border: WeddingActiveTheme.border
                    )
                }

                WeddingSectionCard(title: "People Impact") {
                    GroupMetricTile(label: "Participants", value: "\(people)")
                    if positions.isEmpty {
                        WeddingEmptyBlock(
                            message: "No contributors yet",
                            detail: "People appear after expenses and activity are recorded."
                        )
                        .padding(.top, 8)
                    } else {
                        VStack(alignment: .leading, spacing: 6) {
                            ForEach(Array(positions.prefix(3))) { pos in
                                HStack {
                                    Text(String(pos.participantId.prefix(8)) + "…")
                                        .font(.plusJakarta(size: 12))
                                        .foregroundStyle(WeddingActiveTheme.text)
                                    Spacer()
                                    Text(GroupFinanceFormat.formatMoney(pos.netPosition, currencyCode: pos.currencyCode))
                                        .font(.plusJakarta(size: 12, weight: .semibold))
                                        .foregroundStyle(WeddingActiveTheme.secondary)
                                }
                                .padding(.top, 6)
                            }
                        }
                    }
                }

                WeddingSectionCard(title: "Wedding Budget") {
                    HStack(spacing: 8) {
                        GroupMetricTile(
                            label: "Planned",
                            value: GroupFinanceFormat.formatMoney(total?.budgetTotal, currencyCode: currency)
                        )
                        GroupMetricTile(
                            label: "Actual",
                            value: GroupFinanceFormat.formatMoney(total?.expenseTotal, currencyCode: currency)
                        )
                    }
                    if total?.budgetTotal != nil {
                        Text("\(utilization)% of planned")
                            .font(.plusJakarta(size: 12, weight: .semibold))
                            .foregroundStyle(WeddingActiveTheme.accentLight)
                            .padding(.top, 8)
                        GroupProgressBar(percent: utilization)
                    } else {
                        Text("Set a budget to track planned vs actual.")
                            .font(.plusJakarta(size: 12))
                            .foregroundStyle(WeddingActiveTheme.secondary)
                            .padding(.top, 8)
                    }
                }

                WeddingSectionCard(title: "Memory Intelligence") {
                    WeddingEmptyBlock(
                        message: "Insights coming soon",
                        detail: "AI memory intelligence for groups is on the roadmap — no invented copy."
                    )
                }

                WeddingPinkCta(
                    title: "Preserve this moment",
                    subtitle: "Capture a memory, photo, or update for the wedding story.",
                    buttonLabel: "Capture Memory",
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
        if let cachedPulse = GroupTabDataCache.peekPulse(momentId) {
            finance = cachedPulse.finance
            pulse = cachedPulse.pulse
            loading = false
        }
        if let cachedMemory = GroupTabDataCache.peekMemory(momentId) {
            memory = cachedMemory.memory
            finance = cachedMemory.finance ?? finance
            pulse = cachedMemory.pulse ?? pulse
            loading = false
        }
        if pulse == nil && finance == nil { loading = true }

        do {
            let cached = GroupTabDataCache.peekPulse(momentId)
            async let memoryResult = APIClient.shared.getGroupMemory(momentId: momentId)
            async let listResult = APIClient.shared.listGroupMemories(momentId: momentId)
            let loadedMemory = try await memoryResult
            listMemoryItems = (try? await listResult)?.items ?? []
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
            GroupTabDataCache.putMemory(momentId, .init(
                memory: loadedMemory,
                finance: loadedFinance,
                pulse: loadedPulse
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

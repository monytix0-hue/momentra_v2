import SwiftUI

/// Figma 575:14470 — Experience Memory. Live APIs only.
struct ExperienceMemoryActiveView: View {
    let theme: ExperienceActiveTheme
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

    private let timelineAccents: [Color] = [
        Color(hex: "#14B8A6"),
        Color(hex: "#B45309"),
        Color(hex: "#A855F7"),
        GroupActiveTheme.accentOrange,
    ]

    var body: some View {
        Group {
            if loading && pulse == nil && finance == nil && memory == nil {
                ProgressView().tint(GroupActiveTheme.brand)
            } else {
                content
            }
        }
        .background(GroupActiveTheme.bg)
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
        let displayTitle = momentTitle ?? "\(theme.typeLabel) Memory"
        let utilization = GroupFinanceFormat.utilizationPercent(
            expenseTotal: total?.expenseTotal,
            budgetTotal: total?.budgetTotal
        )

        NativeDashboardScaffold(background: GroupActiveTheme.bg) {
            NativeListSection {
                VStack(alignment: .leading, spacing: 14) {
                    if let error {
                        Text(error).font(.caption).foregroundStyle(Color(hex: "#F87171"))
                    }

                    experienceMemoryHero(title: displayTitle, peopleCount: people, memoryCount: memoryCount)

                    GroupSectionCard(title: "Memory Timeline") {
                        if items.isEmpty {
                            GroupEmptySection(
                                message: "Timeline empty",
                                detail: "Shared memories will appear here — nothing is invented."
                            )
                        } else {
                            ForEach(Array(items.enumerated()), id: \.offset) { index, item in
                                experienceTimelineRow(
                                    item: item,
                                    accent: timelineAccents[index % timelineAccents.count],
                                    glyph: experienceMemoryGlyph(index)
                                )
                            }
                        }
                    }

                    GroupSectionCard(title: "Milestone Wall", badge: { GroupApiGapBadge() }) {
                        GroupEmptySection(
                            message: "No milestones yet",
                            detail: "Milestone capture is not live for groups."
                        )
                    }

                    GroupSectionCard(title: "Memory Gallery") {
                        MemoryPhotoGalleryStrip(
                            items: items,
                            emptyMessage: "No photos yet",
                            emptyDetail: "Add a memory with a photo from Quick Add.",
                            text: GroupActiveTheme.text,
                            muted: GroupActiveTheme.secondary,
                            field: GroupActiveTheme.card,
                            border: GroupActiveTheme.border
                        )
                    }

                    GroupSectionCard(title: "People Impact") {
                        Text(people > 0 ? "\(people) people shaped this shared story" : "No participants yet")
                            .font(.plusJakarta(size: 13, weight: .semibold))
                            .foregroundStyle(GroupActiveTheme.text)
                        GroupMetricTile(label: "Participants", value: "\(people)")
                    }

                    GroupSectionCard(title: theme.budgetTitle) {
                        VStack(alignment: .leading, spacing: 10) {
                            HStack(spacing: 8) {
                                VStack(alignment: .leading, spacing: 2) {
                                    Text("Planned")
                                        .font(.plusJakarta(size: 11))
                                        .foregroundStyle(.white.opacity(0.8))
                                    Text(GroupFinanceFormat.formatMoney(total?.budgetTotal, currencyCode: currency))
                                        .font(.plusJakarta(size: 16, weight: .bold))
                                        .foregroundStyle(.white)
                                }
                                .frame(maxWidth: .infinity, alignment: .leading)
                                VStack(alignment: .leading, spacing: 2) {
                                    Text("Actual")
                                        .font(.plusJakarta(size: 11))
                                        .foregroundStyle(.white.opacity(0.8))
                                    Text(GroupFinanceFormat.formatMoney(total?.expenseTotal, currencyCode: currency))
                                        .font(.plusJakarta(size: 16, weight: .bold))
                                        .foregroundStyle(.white)
                                }
                                .frame(maxWidth: .infinity, alignment: .leading)
                            }
                            GroupProgressBar(percent: utilization)
                            Text(
                                utilization > 0
                                    ? "\(utilization)% of planned budget used"
                                    : "Set a budget to track planned vs actual."
                            )
                            .font(.plusJakarta(size: 11))
                            .foregroundStyle(.white.opacity(0.85))
                        }
                        .padding(14)
                        .background(
                            LinearGradient(
                                colors: [theme.accentSolid, theme.accentLight],
                                startPoint: .leading,
                                endPoint: .trailing
                            )
                        )
                        .clipShape(RoundedRectangle(cornerRadius: 14))
                    }

                    GroupSectionCard(title: "Memory Intelligence", badge: { GroupComingSoonBadge() }) {
                        GroupEmptySection(
                            message: "Insights coming soon",
                            detail: "AI memory intelligence for groups is on the roadmap — no invented copy."
                        )
                    }

                    VStack(alignment: .leading, spacing: 8) {
                        Text("Preserve this moment")
                            .font(.plusJakarta(size: 16, weight: .heavy))
                            .foregroundStyle(.white)
                        Text("Capture a memory, photo, or update for the \(theme.typeLabel.lowercased()) story.")
                            .font(.plusJakarta(size: 12))
                            .foregroundStyle(.white.opacity(0.85))
                        GroupCtaButton(label: "+ Capture Memory", enabled: true, action: onOpenQuickAdd)
                    }
                    .padding(16)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(
                        LinearGradient(
                            colors: [GroupActiveTheme.brand, GroupActiveTheme.accentOrange],
                            startPoint: .leading,
                            endPoint: .trailing
                        )
                    )
                    .clipShape(RoundedRectangle(cornerRadius: 18))
                }
            }
        }
    }

    private func experienceMemoryHero(title: String, peopleCount: Int, memoryCount: Int) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 14) {
                Text(theme.heroEmoji)
                    .font(.system(size: 24))
                    .frame(width: 56, height: 56)
                    .background(Color.white.opacity(0.18))
                    .clipShape(Circle())
                VStack(alignment: .leading, spacing: 4) {
                    Text(title)
                        .font(.plusJakarta(size: 20, weight: .heavy))
                        .foregroundStyle(.white)
                    Text("What you'll remember from this \(theme.typeLabel.lowercased())")
                        .font(.plusJakarta(size: 12))
                        .foregroundStyle(.white.opacity(0.85))
                }
            }
            HStack(spacing: 8) {
                experienceHeroChip("\(peopleCount) people")
                experienceHeroChip(memoryCount > 0 ? "\(memoryCount) memories captured" : "No memories yet")
            }
        }
        .padding(20)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            LinearGradient(
                colors: [theme.accentSolid, theme.accentLight, Color(hex: "#3D2A24")],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        )
        .clipShape(RoundedRectangle(cornerRadius: 24))
    }

    private func experienceHeroChip(_ label: String) -> some View {
        Text(label)
            .font(.plusJakarta(size: 11, weight: .semibold))
            .foregroundStyle(.white)
            .padding(.horizontal, 10)
            .padding(.vertical, 6)
            .background(Color.white.opacity(0.16))
            .clipShape(Capsule())
    }

    private func experienceTimelineRow(
        item: APIClient.GroupMemoryPayload.MemoryInner.GroupMemoryItem,
        accent: Color,
        glyph: String
    ) -> some View {
        HStack(spacing: 12) {
            RoundedRectangle(cornerRadius: 100)
                .fill(accent)
                .frame(width: 3, height: 40)
            if let thumb = item.primaryDownloadUrl, !thumb.isEmpty {
                MemoryMediaThumb(
                    urlString: thumb,
                    size: 44,
                    border: accent.opacity(0.35),
                    field: accent.opacity(0.18)
                )
            } else {
                Text(glyph)
                    .font(.system(size: 16))
                    .frame(width: 36, height: 36)
                    .background(accent.opacity(0.18))
                    .clipShape(RoundedRectangle(cornerRadius: 12))
            }
            VStack(alignment: .leading, spacing: 2) {
                Text(item.title ?? "Memory")
                    .font(.plusJakarta(size: 13, weight: .semibold))
                    .foregroundStyle(GroupActiveTheme.text)
                if let meta = formatExperienceMemoryInstant(item.occurredAt) {
                    Text(meta)
                        .font(.plusJakarta(size: 11))
                        .foregroundStyle(GroupActiveTheme.secondary)
                }
            }
            Spacer(minLength: 0)
        }
        .padding(12)
        .background(Color(hex: "#181716"))
        .clipShape(RoundedRectangle(cornerRadius: 14))
        .overlay(RoundedRectangle(cornerRadius: 14).stroke(accent.opacity(0.35)))
    }

    private func experienceMemoryGlyph(_ index: Int) -> String {
        switch index % 4 {
        case 0: return "📷"
        case 1: return "🎉"
        case 2: return "🌅"
        default: return "✨"
        }
    }

    private func formatExperienceMemoryInstant(_ raw: String?) -> String? {
        guard let raw, !raw.isEmpty else { return nil }
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        guard let date = formatter.date(from: raw) ?? ISO8601DateFormatter().date(from: raw) else { return nil }
        let out = DateFormatter()
        out.locale = Locale(identifier: "en_US_POSIX")
        out.dateFormat = "d MMM · h:mm a"
        return out.string(from: date)
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
        } catch {
            if Task.isCancelled { return }
            let ns = error as NSError
            if ns.domain == NSURLErrorDomain && ns.code == NSURLErrorCancelled { return }
            self.error = error.localizedDescription
        }
        loading = false
    }
}

import SwiftUI

private let momentsPageLimit = 20

struct BusinessMomentsScreen: View {
    let family: BusinessMomentFamily
    let momentId: String?
    let momentTitle: String?
    let refreshToken: UInt64
    var onOpenQuickAdd: () -> Void = {}

    @State private var feed = BusinessMomentsFeed()
    @State private var filter = BusinessMomentFilter.all
    @State private var retry = 0

    private var theme: BusinessActiveTheme {
        let code: String
        switch family {
        case .money: code = "BUSINESS_RUNWAY"
        case .daily: code = "BUSINESS_OPERATIONS"
        case .team: code = "TEAM_OPERATIONS"
        }
        return .forTypeCode(code)
    }

    private var presentation: BusinessMomentsPresentation {
        buildBusinessMomentsPresentation(
            family: family,
            items: feed.items,
            filter: filter,
            loading: feed.loading,
            error: feed.error,
            now: Date(),
            timeZone: .current
        )
    }

    var body: some View {
        Group {
            if presentation.content == .loading {
                ProgressView().tint(theme.accent)
            } else {
                ScrollView {
                    VStack(alignment: .leading, spacing: 12) {
                        if let momentTitle, !momentTitle.isEmpty {
                            Text(momentTitle)
                                .font(.plusJakarta(size: 12, weight: .semibold))
                                .foregroundStyle(theme.secondary)
                        }
                        Text(presentation.familyTitle)
                            .font(.plusJakarta(size: 20, weight: .heavy))
                            .foregroundStyle(theme.text)
                        Text(presentation.contextLine)
                            .font(.plusJakarta(size: 12))
                            .foregroundStyle(theme.secondary)
                        ScrollView(.horizontal, showsIndicators: false) {
                            HStack(spacing: 8) {
                                ForEach(presentation.chips) { chip in
                                    let selected = chip.filter == presentation.selected
                                    Button(chip.label) { filter = chip.filter }
                                        .font(.plusJakarta(size: 13, weight: .bold))
                                        .foregroundStyle(selected ? Color.white : theme.text)
                                        .padding(.horizontal, 12)
                                        .padding(.vertical, 8)
                                        .background(selected ? theme.accent : theme.card)
                                        .overlay(Capsule().stroke(selected ? theme.accent : theme.border))
                                        .clipShape(Capsule())
                                        .buttonStyle(.plain)
                                }
                            }
                        }
                        if let message = presentation.errorMessage {
                            Text(message).font(.caption).foregroundStyle(Color(hex: "#F87171"))
                            Button("Try again") { retry += 1 }
                                .font(.plusJakarta(size: 13, weight: .bold))
                                .foregroundStyle(theme.accent)
                                .buttonStyle(.plain)
                        }
                        switch presentation.content {
                        case .feedEmpty:
                            emptyBlock(
                                title: presentation.emptyTitle ?? "",
                                body: presentation.emptyBody,
                                action: BusinessMomentsPresentation.addLabel,
                                onAction: onOpenQuickAdd
                            )
                        case .filterEmpty:
                            emptyBlock(
                                title: presentation.emptyTitle ?? "",
                                body: nil,
                                action: "Show all",
                                onAction: { filter = .all }
                            )
                        case .timeline:
                            ForEach(presentation.groups) { group in
                                Text(group.label)
                                    .font(.plusJakarta(size: 11, weight: .bold))
                                    .foregroundStyle(theme.secondary)
                                ForEach(group.entries) { entry in
                                    entryCard(entry)
                                }
                            }
                            if feed.nextCursor != nil {
                                Button("Load more") { Task { await loadMore() } }
                                    .font(.plusJakarta(size: 13, weight: .bold))
                                    .foregroundStyle(theme.accent)
                                    .buttonStyle(.plain)
                            }
                        case .error, .loading:
                            EmptyView()
                        }
                    }
                    .padding(.horizontal, 16)
                    .padding(.vertical, 12)
                    .padding(.bottom, 88)
                }
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
        .background(theme.bg)
        .task(id: "\(refreshToken)-\(momentId ?? "")-\(retry)") { await reload() }
        .onChange(of: momentId) { _, _ in filter = .all }
    }

    private func emptyBlock(title: String, body: String?, action: String, onAction: @escaping () -> Void) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title)
                .font(.plusJakarta(size: 15, weight: .bold))
                .foregroundStyle(theme.text)
            if let body, !body.isEmpty {
                Text(body)
                    .font(.plusJakarta(size: 13))
                    .foregroundStyle(theme.secondary)
            }
            Button(action, action: onAction)
                .font(.plusJakarta(size: 14, weight: .heavy))
                .foregroundStyle(.white)
                .padding(.horizontal, 12)
                .padding(.vertical, 10)
                .background(theme.accent)
                .clipShape(Capsule())
                .buttonStyle(.plain)
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(theme.card)
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(theme.border))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private func entryCard(_ entry: BusinessMomentsEntry) -> some View {
        let card = entry.card
        let value = [card.amountLabel, card.subtitle].compactMap { $0 }.filter { !$0.isEmpty }.joined(separator: " · ")
        let meta = [entry.actorLine, entry.timeLabel].compactMap { $0 }.filter { !$0.isEmpty }.joined(separator: " · ")
        return VStack(alignment: .leading, spacing: 4) {
            if let eyebrow = entry.eyebrow {
                Text(eyebrow)
                    .font(.plusJakarta(size: 11, weight: .bold))
                    .foregroundStyle(theme.secondary)
            }
            Text(card.title)
                .font(.plusJakarta(size: 14, weight: .bold))
                .foregroundStyle(theme.text)
            if !value.isEmpty {
                Text(value)
                    .font(.plusJakarta(size: 13))
                    .foregroundStyle(theme.text)
            }
            if let status = card.statusLabel {
                Text(status)
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(theme.secondary)
            }
            if !meta.isEmpty {
                Text(meta)
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(theme.secondary)
            }
        }
        .padding(12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(theme.card)
        .overlay(RoundedRectangle(cornerRadius: 14).stroke(theme.border))
        .clipShape(RoundedRectangle(cornerRadius: 14))
    }

    private func reload() async {
        let started = feed.begin(momentId)
        feed = started
        guard let momentId, !momentId.isEmpty else { return }
        do {
            let page = try await APIClient.shared.listBusinessActivityPage(momentId: momentId, limit: momentsPageLimit)
            feed = feed.applyPage(
                momentId: momentId,
                generation: started.generation,
                pageItems: page.items,
                cursor: page.nextCursor,
                append: false
            )
        } catch {
            feed = feed.applyFailure(momentId: momentId, generation: started.generation, message: error.localizedDescription)
        }
    }

    private func loadMore() async {
        guard let momentId = feed.momentId, let cursor = feed.nextCursor else { return }
        let generation = feed.generation
        do {
            let page = try await APIClient.shared.listBusinessActivityPage(
                momentId: momentId,
                cursor: cursor,
                limit: momentsPageLimit
            )
            feed = feed.applyPage(
                momentId: momentId,
                generation: generation,
                pageItems: page.items,
                cursor: page.nextCursor,
                append: true
            )
        } catch {
            feed = feed.applyFailure(momentId: momentId, generation: generation, message: error.localizedDescription)
        }
    }
}

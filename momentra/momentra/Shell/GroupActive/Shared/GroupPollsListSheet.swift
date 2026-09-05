import SwiftUI

/// Figma 1604:16811 — Polls list (View all). Live APIs only.
struct GroupPollsListSheet: View {
    let momentTitle: String?
    var chrome: MomentsChrome = .trip
    let polls: [APIClient.GroupPollItemPayload]
    var onDismiss: () -> Void
    var onChanged: () -> Void = {}

    @State private var filter: PollsListFilter = .all
    @State private var selectedPollId: String?

    private enum PollsListFilter: String, CaseIterable, Identifiable {
        case all = "All"
        case active = "Active"
        case closed = "Closed"
        var id: String { rawValue }
    }

    private var avatarColors: [Color] {
        [Color(hex: "#FDBA74"), Color(hex: "#86EFAC"), Color(hex: "#93C5FD"), Color(hex: "#C4B5FD")]
    }

    private var filtered: [APIClient.GroupPollItemPayload] {
        switch filter {
        case .all: return polls
        case .active:
            return polls.filter { ($0.status ?? "").uppercased() == "OPEN" }
        case .closed:
            return polls.filter {
                let s = ($0.status ?? "").uppercased()
                return s == "CLOSED" || s == "CANCELLED"
            }
        }
    }

    var body: some View {
        VStack(spacing: 0) {
            header
            filterTabs
            ScrollView {
                LazyVStack(spacing: 16) {
                    if filtered.isEmpty {
                        GroupEmptySection(
                            message: emptyTitle,
                            detail: emptyDetail
                        )
                        .padding(.top, 24)
                    } else {
                        ForEach(Array(filtered.enumerated()), id: \.element.id) { index, poll in
                            pollCard(poll, index: index)
                        }
                    }
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 12)
                .padding(.bottom, 32)
            }
        }
        .background(chrome.bg.ignoresSafeArea())
        .sheet(item: Binding(
            get: { selectedPollId.map { PollSheetItem(id: $0) } },
            set: { selectedPollId = $0?.id }
        )) { item in
            PollDetailSheet(
                pollId: item.id,
                onDismiss: { selectedPollId = nil },
                onSaved: { onChanged() }
            )
        }
    }

    private struct PollSheetItem: Identifiable {
        let id: String
    }

    private var emptyTitle: String {
        switch filter {
        case .all: return "No polls yet"
        case .active: return "No active polls"
        case .closed: return "No closed polls"
        }
    }

    private var emptyDetail: String {
        switch filter {
        case .all: return "Create a poll from Quick Add — nothing is invented."
        case .active: return "Open polls will show here."
        case .closed: return "Closed polls will show here."
        }
    }

    private var header: some View {
        HStack(spacing: 12) {
            Button(action: onDismiss) {
                Image(systemName: "chevron.left")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(chrome.text)
                    .frame(width: 36, height: 36)
                    .background(chrome.card)
                    .clipShape(Circle())
                    .overlay(Circle().stroke(chrome.border))
            }
            .buttonStyle(.plain)
            VStack(alignment: .leading, spacing: 2) {
                Text("Polls")
                    .font(.plusJakarta(size: 18, weight: .bold))
                    .foregroundStyle(chrome.text)
                Text(momentTitle ?? "Shared moment")
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(chrome.secondary)
                    .lineLimit(1)
            }
            Spacer(minLength: 0)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 10)
    }

    private var filterTabs: some View {
        HStack(spacing: 8) {
            ForEach(PollsListFilter.allCases) { tab in
                let selected = filter == tab
                Button {
                    filter = tab
                } label: {
                    Text(tab.rawValue)
                        .font(.plusJakarta(size: 13, weight: selected ? .bold : .semibold))
                        .foregroundStyle(selected ? chrome.darkText : chrome.secondary)
                        .padding(.horizontal, 16)
                        .padding(.vertical, 8)
                        .background(selected ? chrome.accent : chrome.card)
                        .clipShape(Capsule())
                        .overlay(
                            Capsule().stroke(selected ? Color.clear : chrome.border)
                        )
                }
                .buttonStyle(.plain)
            }
            Spacer(minLength: 0)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 8)
    }

    @ViewBuilder
    private func pollCard(_ poll: APIClient.GroupPollItemPayload, index: Int) -> some View {
        let isOpen = (poll.status ?? "").uppercased() == "OPEN"
        let options = poll.options ?? []
        let total = max(poll.totalVotes ?? options.reduce(0) { $0 + ($1.voteCount ?? 0) }, 0)
        let totalDenom = max(total, 1)
        let creator = poll.createdByDisplayName ?? "Member"
        let endsTag = formatPollEndsTag(closesAt: poll.closesAt, status: poll.status)
        let winningCount = options.map { $0.voteCount ?? 0 }.max() ?? 0

        VStack(alignment: .leading, spacing: 14) {
            HStack {
                HStack(spacing: 8) {
                    Text(initialsFromName(creator))
                        .font(.plusJakarta(size: 10, weight: .bold))
                        .foregroundStyle(chrome.darkText)
                        .frame(width: 24, height: 24)
                        .background(avatarColors[index % avatarColors.count])
                        .clipShape(Circle())
                    Text(creator)
                        .font(.plusJakarta(size: 12, weight: .semibold))
                        .foregroundStyle(chrome.secondary)
                        .lineLimit(1)
                }
                Spacer(minLength: 8)
                Text(endsTag)
                    .font(.plusJakarta(size: 10, weight: .bold))
                    .foregroundStyle(isOpen ? chrome.accent : chrome.secondary)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(isOpen ? chrome.brandSoft : chrome.card)
                    .clipShape(RoundedRectangle(cornerRadius: 6))
                    .overlay(
                        RoundedRectangle(cornerRadius: 6)
                            .stroke(isOpen ? Color.clear : chrome.border)
                    )
            }

            Text(poll.question ?? "Poll")
                .font(.plusJakarta(size: 16, weight: .bold))
                .foregroundStyle(chrome.text)
                .fixedSize(horizontal: false, vertical: true)

            VStack(alignment: .leading, spacing: 12) {
                ForEach(Array(options.enumerated()), id: \.offset) { _, option in
                    let votes = option.voteCount ?? 0
                    let pct = Int((Double(votes) / Double(totalDenom) * 100).rounded())
                    let isWinner = !isOpen && winningCount > 0 && votes == winningCount
                    VStack(alignment: .leading, spacing: 6) {
                        HStack(alignment: .firstTextBaseline) {
                            HStack(spacing: 6) {
                                if isWinner {
                                    Image(systemName: "checkmark.circle.fill")
                                        .font(.system(size: 12))
                                        .foregroundStyle(chrome.accent)
                                }
                                Text(option.text ?? "Option")
                                    .font(.plusJakarta(size: 13, weight: .semibold))
                                    .foregroundStyle(chrome.text)
                            }
                            Spacer(minLength: 8)
                            Text("\(votes) votes (\(pct)%)")
                                .font(.plusJakarta(size: 12))
                                .foregroundStyle(chrome.secondary)
                        }
                        GeometryReader { geo in
                            ZStack(alignment: .leading) {
                                Capsule().fill(Color(hex: "#252332"))
                                Capsule()
                                    .fill(chrome.accent)
                                    .frame(width: max(total == 0 ? 0 : 6, geo.size.width * CGFloat(votes) / CGFloat(totalDenom)))
                            }
                        }
                        .frame(height: 8)
                    }
                }
            }

            HStack {
                Text(total == 1 ? "1 total vote" : "\(total) total votes")
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(chrome.secondary)
                Spacer()
                if isOpen {
                    Button {
                        if let id = poll.pollId { selectedPollId = id }
                    } label: {
                        Text("Vote")
                            .font(.plusJakarta(size: 13, weight: .bold))
                            .foregroundStyle(chrome.darkText)
                            .padding(.horizontal, 16)
                            .padding(.vertical, 6)
                            .background(chrome.accent)
                            .clipShape(RoundedRectangle(cornerRadius: 8))
                    }
                    .buttonStyle(.plain)
                }
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(chrome.card)
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(chrome.border))
        .contentShape(RoundedRectangle(cornerRadius: 16))
        .onTapGesture {
            if let id = poll.pollId { selectedPollId = id }
        }
    }
}

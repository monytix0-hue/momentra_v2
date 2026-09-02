import SwiftUI

/// Poll detail — vote, view results, close (live API).
struct PollDetailSheet: View {
    let pollId: String
    var onDismiss: () -> Void = {}
    var onSaved: () -> Void = {}

    @State private var poll: APIClient.GroupPollDetailPayload?
    @State private var loading = true
    @State private var submitting = false
    @State private var error: String?

    private var isOpen: Bool { poll?.status?.uppercased() == "OPEN" }
    private var totalVotes: Int {
        poll?.options?.reduce(0) { $0 + ($1.voteCount ?? 0) } ?? 0
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    if let error {
                        Text(error)
                            .font(.caption)
                            .foregroundStyle(Color(hex: "#F87171"))
                    }
                    if loading && poll == nil {
                        ProgressView().tint(GroupActiveTheme.brand)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 24)
                    } else if let poll {
                        SheetHeader(
                            icon: "chart.bar.fill",
                            title: poll.question ?? "Poll",
                            subtitle: poll.status?.capitalized,
                            accent: purpleAccent
                        )
                        ForEach(poll.options ?? []) { option in
                            pollOptionRow(option)
                        }
                        if isOpen {
                            PrimaryCta(
                                label: "Close poll",
                                enabled: !submitting,
                                accent: purpleAccent,
                                loading: submitting,
                                lightLabel: true
                            ) {
                                Task { await closePoll() }
                            }
                        }
                    }
                }
                .padding(16)
            }
            .background(GroupActiveTheme.bg)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Done", action: onDismiss)
                        .font(.plusJakarta(size: 14, weight: .semibold))
                }
            }
        }
        .task(id: pollId) { await load() }
    }

    @ViewBuilder
    private func pollOptionRow(_ option: APIClient.GroupPollDetailOptionPayload) -> some View {
        let count = option.voteCount ?? 0
        let pct = totalVotes > 0 ? Double(count) / Double(totalVotes) : 0
        let voted = option.votedByMe == true
        Button {
            guard isOpen, !submitting else { return }
            Task { await vote(optionId: option.pollOptionId) }
        } label: {
            VStack(alignment: .leading, spacing: 8) {
                HStack {
                    Text(option.text ?? "Option")
                        .font(.plusJakarta(size: 14, weight: .semibold))
                        .foregroundStyle(GroupActiveTheme.text)
                    Spacer()
                    Text("\(count)")
                        .font(.plusJakarta(size: 12, weight: .bold))
                        .foregroundStyle(GroupActiveTheme.secondary)
                }
                GeometryReader { geo in
                    ZStack(alignment: .leading) {
                        Capsule().fill(Color(hex: "#2A2826"))
                        Capsule()
                            .fill(voted ? purpleAccent.accent : purpleAccent.accent.opacity(0.55))
                            .frame(width: geo.size.width * pct)
                    }
                }
                .frame(height: 6)
            }
            .padding(12)
            .background(voted ? purpleAccent.soft : Color(hex: "#181716"))
            .overlay(
                RoundedRectangle(cornerRadius: 14)
                    .stroke(voted ? purpleAccent.accent.opacity(0.5) : GroupActiveTheme.border)
            )
            .clipShape(RoundedRectangle(cornerRadius: 14))
        }
        .buttonStyle(.plain)
        .disabled(!isOpen || submitting)
    }

    private func load() async {
        loading = true
        error = nil
        do {
            poll = try await APIClient.shared.getPoll(pollId: pollId)
        } catch {
            self.error = error.localizedDescription
        }
        loading = false
    }

    private func vote(optionId: String?) async {
        guard let optionId else { return }
        submitting = true
        error = nil
        do {
            try await APIClient.shared.votePoll(pollId: pollId, pollOptionId: optionId)
            submitting = false
            await load()
            onSaved()
        } catch {
            submitting = false
            self.error = error.localizedDescription
        }
    }

    private func closePoll() async {
        submitting = true
        error = nil
        do {
            try await APIClient.shared.closePoll(pollId: pollId)
            submitting = false
            await load()
            onSaved()
        } catch {
            submitting = false
            self.error = error.localizedDescription
        }
    }
}

import SwiftUI

/// Figma `1006:8434` Activity Timeline + edit routing for expense vs lifestyle.
struct PersonalRecentActivityFlow: View {
    let momentId: String?
    @Binding var isPresented: Bool
    var onChanged: () -> Void = {}

    @State private var items: [APIClient.ActivityItemPayload] = []
    @State private var filter = "All"
    @State private var searchQuery = ""
    @State private var editing: APIClient.ActivityItemPayload?
    @State private var loading = true

    private let accent = Color(hex: "#7C5CFC")

    private var filtered: [APIClient.ActivityItemPayload] {
        items
            .filter { PersonalActivityTimelineDerived.isVisible($0) }
            .filter { PersonalActivityTimelineDerived.matchesFilter($0, filter: filter) }
            .filter { PersonalActivityTimelineDerived.matchesSearch($0, query: searchQuery) }
    }

    private var stats: PersonalActivityTimelineDerived.TimelineStats {
        PersonalActivityTimelineDerived.computeStats(items)
    }

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                header
                searchBar
                filterRows
                statsRow
                content
            }
            .background(Color(hex: "#14121B"))
            .task { await load() }
            .sheet(item: $editing) { item in
                if let momentId {
                    if PersonalActivityTimelineDerived.isExpense(item) {
                        PersonalEditTransactionSheet(
                            momentId: momentId,
                            item: item,
                            onClose: { editing = nil },
                            onSaved: {
                                editing = nil
                                onChanged()
                                Task { await load() }
                            },
                            onDeleted: {
                                editing = nil
                                onChanged()
                                Task { await load() }
                            }
                        )
                        .presentationDetents([.large])
                    } else {
                        PersonalEditActivitySheet(
                            momentId: momentId,
                            item: item,
                            onClose: { editing = nil },
                            onSaved: {
                                editing = nil
                                onChanged()
                                Task { await load() }
                            },
                            onDeleteUnavailable: { editing = nil }
                        )
                        .presentationDetents([.large])
                    }
                }
            }
        }
    }

    private var header: some View {
        HStack {
            Button(action: { isPresented = false }) {
                HStack(spacing: 8) {
                    Text("‹").font(.system(size: 22, weight: .bold))
                        .frame(width: 32, height: 32)
                        .background(Color.white.opacity(0.08))
                        .clipShape(RoundedRectangle(cornerRadius: 8))
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Activity").font(.system(size: 20, weight: .bold)).foregroundStyle(.white)
                        Text("Your daily rhythm & money").font(.system(size: 12)).foregroundStyle(Color(hex: "#C9C4D8"))
                    }
                }
            }
            .buttonStyle(.plain)
            Spacer()
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
    }

    private var searchBar: some View {
        HStack(spacing: 8) {
            Text("🔍")
            TextField("Search activity…", text: $searchQuery)
                .foregroundStyle(Color(hex: "#E5E0EE"))
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
        .background(Color(hex: "#201E28"))
        .overlay(RoundedRectangle(cornerRadius: 14).stroke(Color.white.opacity(0.08)))
        .clipShape(RoundedRectangle(cornerRadius: 14))
        .padding(.horizontal, 16)
        .padding(.bottom, 12)
    }

    private var filterRows: some View {
        VStack(spacing: 8) {
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(PersonalActivityTimelineDerived.primaryFilters) { chip in
                        filterChip(chip)
                    }
                }
                .padding(.horizontal, 16)
            }
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(PersonalActivityTimelineDerived.categoryFilters) { chip in
                        filterChip(chip)
                    }
                }
                .padding(.horizontal, 16)
            }
        }
        .padding(.bottom, 12)
    }

    private func filterChip(_ chip: PersonalActivityTimelineDerived.FilterChip) -> some View {
        let selected = filter == chip.id
        return Text("\(chip.emoji) \(chip.label)")
            .font(.system(size: 12, weight: .semibold))
            .foregroundStyle(selected ? .white : Color(hex: "#C9C4D8"))
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
            .background(selected ? accent : Color.white.opacity(0.06))
            .overlay(Capsule().stroke(selected ? accent : Color.white.opacity(0.08)))
            .clipShape(Capsule())
            .onTapGesture { filter = filter == chip.id ? "All" : chip.id }
    }

    private var statsRow: some View {
        HStack(spacing: 8) {
            statCard("TOTAL LOGS", value: "\(stats.totalLogs)")
            statCard("THIS MONTH", value: "\(stats.thisMonth)")
            statCard("TOTAL AMOUNT", value: stats.totalAmountLabel)
        }
        .padding(.horizontal, 16)
        .padding(.bottom, 12)
    }

    private func statCard(_ label: String, value: String) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label).font(.system(size: 9, weight: .semibold)).foregroundStyle(Color(hex: "#C9C4D8"))
            Text(value).font(.system(size: 16, weight: .bold)).foregroundStyle(Color(hex: "#E5E0EE"))
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(12)
        .background(Color(hex: "#201E28"))
        .overlay(RoundedRectangle(cornerRadius: 14).stroke(Color.white.opacity(0.08)))
        .clipShape(RoundedRectangle(cornerRadius: 14))
    }

    @ViewBuilder
    private var content: some View {
        if loading {
            Spacer()
            ProgressView().tint(accent)
            Spacer()
        } else if filtered.isEmpty {
            Spacer()
            Text(searchQuery.isEmpty && filter == "All" ? "No activity yet — log your first entry from Pulse." : "No activity matches this filter.")
                .font(.system(size: 14))
                .foregroundStyle(Color(hex: "#C9C4D8"))
                .multilineTextAlignment(.center)
                .padding(24)
            Spacer()
        } else {
            ScrollView {
                LazyVStack(spacing: 8) {
                    ForEach(filtered) { item in
                        timelineRow(item)
                    }
                }
                .padding(16)
            }
        }
    }

    private func timelineRow(_ item: APIClient.ActivityItemPayload) -> some View {
        let visual = PersonalActivityTimelineDerived.rowVisual(item)
        return HStack(spacing: 0) {
            visual.accent.frame(width: 4)
            HStack(spacing: 12) {
                Text(visual.emoji).font(.system(size: 22))
                VStack(alignment: .leading, spacing: 2) {
                    Text(item.title)
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundStyle(Color(hex: "#E5E0EE"))
                    Text(visual.metadata)
                        .font(.system(size: 11))
                        .foregroundStyle(Color(hex: "#C9C4D8"))
                    Text(visual.timeLabel)
                        .font(.system(size: 10, weight: .semibold))
                        .foregroundStyle(Color(hex: "#C9C4D8").opacity(0.7))
                }
                Spacer(minLength: 4)
                Button { editing = item } label: {
                    Text("✏️")
                        .frame(width: 32, height: 32)
                        .background(accent.opacity(0.15))
                        .clipShape(RoundedRectangle(cornerRadius: 8))
                }
                .buttonStyle(.plain)
            }
            .padding(12)
        }
        .background(Color.white.opacity(0.04))
        .overlay(RoundedRectangle(cornerRadius: 14).stroke(Color.white.opacity(0.08)))
        .clipShape(RoundedRectangle(cornerRadius: 14))
    }

    private func load() async {
        loading = true
        defer { loading = false }
        do {
            items = try await APIClient.shared.listPersonalActivity(momentId: momentId, limit: 50)
        } catch {
            items = []
        }
    }
}

struct PersonalEditActivitySheet: View {
    let momentId: String?
    let item: APIClient.ActivityItemPayload
    var onClose: () -> Void
    var onSaved: () -> Void
    var onDeleteUnavailable: () -> Void

    @State private var name: String
    @State private var amount: String
    @State private var category: String
    @State private var whenLabel: String
    @State private var notes: String
    @State private var selectedTag: String
    @State private var submitting = false
    @State private var error: String?

    private let tags = ["Essential", "Planned", "Impulse", "Budget", "Weekly"]
    private let categories = ["Errands", "Food", "Transport", "Housing", "Wellness", "Social", "Other"]
    private let green = Color(hex: "#10B981")

    private var isExpense: Bool {
        PersonalActivityTimelineDerived.isExpense(item)
    }

    init(
        momentId: String?,
        item: APIClient.ActivityItemPayload,
        onClose: @escaping () -> Void,
        onSaved: @escaping () -> Void,
        onDeleteUnavailable: @escaping () -> Void
    ) {
        self.momentId = momentId
        self.item = item
        self.onClose = onClose
        self.onSaved = onSaved
        self.onDeleteUnavailable = onDeleteUnavailable
        _name = State(initialValue: item.title)
        let rawAmount = item.activityPayload?.amount ?? ""
        _amount = State(initialValue: rawAmount.isEmpty ? "" : rawAmount)
        _category = State(initialValue: item.activityPayload?.categoryCode ?? "Errands")
        _whenLabel = State(initialValue: formatOccurred(item.occurredAt))
        _notes = State(initialValue: item.activityPayload?.description ?? "")
        _selectedTag = State(initialValue: "Essential")
    }

    var body: some View {
        ScrollView {
            VStack(spacing: 14) {
                Capsule().fill(Color(hex: "#3A3842")).frame(width: 36, height: 4).padding(.top, 8)
                HStack {
                    Button(action: onClose) {
                        Text("×").font(.system(size: 22)).foregroundStyle(Color(hex: "#FF7A3D"))
                            .frame(width: 40, height: 40)
                            .background(Color.white.opacity(0.06))
                            .clipShape(Circle())
                    }
                    VStack(spacing: 4) {
                        Text("Edit Activity").font(.system(size: 18, weight: .semibold)).foregroundStyle(.white)
                        Text("Edit Entry Details").font(.system(size: 12)).foregroundStyle(Color(hex: "#94A3B8"))
                    }
                    .frame(maxWidth: .infinity)
                    Color.clear.frame(width: 40, height: 40)
                }

                field("ACTIVITY NAME", text: $name, border: green)
                if isExpense {
                    field("AMOUNT", text: $amount, border: Color(hex: "#938EA1"))
                }
                field("DATE & TIME", text: $whenLabel, border: Color(hex: "#938EA1"))
                    .disabled(true)

                VStack(alignment: .leading, spacing: 8) {
                    Text("NOTES").font(.system(size: 11, weight: .semibold)).foregroundStyle(Color(hex: "#64748B"))
                    TextEditor(text: $notes)
                        .scrollContentBackground(.hidden)
                        .foregroundStyle(Color(hex: "#E5E0EE"))
                        .frame(minHeight: 88)
                        .onChange(of: notes) { _, v in
                            if v.count > 200 { notes = String(v.prefix(200)) }
                        }
                }
                .padding(12)
                .background(Color(hex: "#14121B"))
                .overlay(RoundedRectangle(cornerRadius: 14).stroke(Color(hex: "#2A2538")))
                .clipShape(RoundedRectangle(cornerRadius: 14))

                if let error {
                    Text(error).font(.system(size: 12)).foregroundStyle(Color(hex: "#F87171"))
                }

                Button(action: save) {
                    Text(submitting ? "Saving…" : "Save Changes")
                        .font(.system(size: 17, weight: .bold))
                        .foregroundStyle(Color(hex: "#14121B"))
                        .frame(maxWidth: .infinity)
                        .frame(height: 52)
                        .background(green)
                        .clipShape(Capsule())
                }
                .disabled(submitting || name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || momentId == nil)
                .opacity(submitting ? 0.6 : 1)

                Button(action: onDeleteUnavailable) {
                    Text("Delete Activity")
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundStyle(Color(hex: "#F87171"))
                }
                .buttonStyle(.plain)
            }
            .padding(16)
            .padding(.bottom, 24)
        }
        .background(Color(hex: "#1C1926"))
    }

    private func field(_ label: String, text: Binding<String>, border: Color) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(label).font(.system(size: 11, weight: .semibold)).foregroundStyle(Color(hex: "#64748B"))
            TextField("", text: text)
                .foregroundStyle(Color(hex: "#E5E0EE"))
                .padding(.horizontal, 14)
                .padding(.vertical, 12)
                .background(Color(hex: "#3A3842"))
                .overlay(RoundedRectangle(cornerRadius: 14).stroke(border))
                .clipShape(RoundedRectangle(cornerRadius: 14))
        }
    }

    private func save() {
        guard let momentId, !submitting else { return }
        submitting = true
        error = nil
        let title = name.trimmingCharacters(in: .whitespacesAndNewlines)
        let note = notes.trimmingCharacters(in: .whitespacesAndNewlines)
        Task {
            do {
                if let activityId = item.activityPayload?.activityId {
                    _ = try await APIClient.shared.updateLifestyleActivity(
                        momentId: momentId,
                        activityId: activityId,
                        title: title,
                        description: note.isEmpty ? nil : note
                    )
                } else {
                    throw NSError(domain: "Momentra", code: 0, userInfo: [
                        NSLocalizedDescriptionKey: "This entry cannot be edited yet.",
                    ])
                }
                await MainActor.run {
                    submitting = false
                    onSaved()
                }
            } catch {
                await MainActor.run {
                    submitting = false
                    self.error = error.localizedDescription
                }
            }
        }
    }
}

private func formatOccurred(_ iso: String) -> String {
    let formatter = ISO8601DateFormatter()
    formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
    var date = formatter.date(from: iso)
    if date == nil {
        formatter.formatOptions = [.withInternetDateTime]
        date = formatter.date(from: iso)
    }
    guard let date else { return iso }
    let f = DateFormatter()
    f.dateFormat = "MMM d, h:mm a"
    return f.string(from: date)
}

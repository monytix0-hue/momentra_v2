import SwiftUI

/// Figma `1036:7697` Recent Activity + `1036:7727` Edit Activity.
struct PersonalRelationshipsActivityFlow: View {
    let momentId: String?
    @Binding var isPresented: Bool
    var onChanged: () -> Void = {}

    @State private var items: [RelationshipsActivityItem] = []
    @State private var filter = "All"
    @State private var editing: RelationshipsActivityItem?

    private let filters = ["All", "Partner", "Family", "Friends", "Self"]
    private let pink = Color(hex: "#E12A9E")
    private let bg = Color(hex: "#14121B")
    private let text = Color(hex: "#E5E0EE")
    private let muted = Color(hex: "#C9C4D8")

    private var filtered: [RelationshipsActivityItem] {
        filter == "All" ? items : items.filter { $0.filter.caseInsensitiveCompare(filter) == .orderedSame }
    }

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                HStack {
                    Button(action: { isPresented = false }) {
                        HStack(spacing: 8) {
                            Text("‹").font(.system(size: 22, weight: .bold))
                                .frame(width: 32, height: 32)
                                .background(Color.white.opacity(0.08))
                                .clipShape(RoundedRectangle(cornerRadius: 8))
                            Text("Recent Activity").font(.system(size: 20, weight: .bold)).foregroundStyle(.white)
                        }
                    }
                    .buttonStyle(.plain)
                    Spacer()
                    Text("🔔").frame(width: 36, height: 36).background(Color.white.opacity(0.06)).clipShape(Circle())
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 12)

                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(filters, id: \.self) { label in
                            let active = filter == label
                            Text(label)
                                .font(.system(size: 12, weight: .semibold))
                                .foregroundStyle(active ? pink : text)
                                .padding(.horizontal, 12)
                                .padding(.vertical, 6)
                                .background(Color(hex: "#201E28"))
                                .overlay(Capsule().stroke(active ? pink : Color(hex: "#C9BFFF").opacity(0.4)))
                                .clipShape(Capsule())
                                .onTapGesture { filter = label }
                        }
                    }
                    .padding(.horizontal, 16)
                }

                ScrollView {
                    LazyVStack(spacing: 4) {
                        ForEach(filtered) { item in
                            row(item)
                        }
                    }
                    .padding(16)
                }
            }
            .background(bg)
            .task {
                do {
                    let acts = try await APIClient.shared.listPersonalActivity(momentId: momentId, limit: 30)
                    items = RelationshipsActivityModels.from(api: acts)
                } catch {
                    items = []
                }
            }
            .sheet(item: $editing) { item in
                RelationshipsEditActivitySheet(
                    item: item,
                    onClose: { editing = nil },
                    onSave: { updated in
                        items = items.map { $0.id == updated.id ? updated : $0 }
                        editing = nil
                        onChanged()
                    },
                    onDelete: {
                        items.removeAll { $0.id == item.id }
                        editing = nil
                        onChanged()
                    }
                )
                .presentationDetents([.large])
            }
        }
    }

    private func row(_ item: RelationshipsActivityItem) -> some View {
        HStack(spacing: 8) {
            VStack(alignment: .leading, spacing: 2) {
                HStack(spacing: 8) {
                    Text(item.emoji).frame(width: 32, height: 32).background(pink).clipShape(RoundedRectangle(cornerRadius: 16))
                    Text(item.title).font(.system(size: 13, weight: .semibold)).foregroundStyle(text)
                }
                Text(item.whenLabel).font(.system(size: 11)).foregroundStyle(muted)
            }
            Spacer()
            if item.impact.isEmpty {
                Text("Logged")
                    .font(.system(size: 12, weight: .heavy))
                    .foregroundStyle(bg)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 4)
                    .background(pink)
                    .clipShape(Capsule())
            } else {
                Text(item.impact)
                    .font(.system(size: 12, weight: .heavy))
                    .foregroundStyle(bg)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 4)
                    .background(pink)
                    .clipShape(Capsule())
            }
            Button { editing = item } label: {
                Text("✏️").frame(width: 28, height: 28).background(Color(hex: "#2A2834")).clipShape(RoundedRectangle(cornerRadius: 8))
            }
            Button {
                items.removeAll { $0.id == item.id }
                onChanged()
            } label: {
                Text("🗑️").frame(width: 28, height: 28).background(Color(hex: "#3C1E1E")).clipShape(RoundedRectangle(cornerRadius: 8))
            }
        }
        .padding(.vertical, 8)
    }
}

struct RelationshipsEditActivitySheet: View {
    let item: RelationshipsActivityItem
    var onClose: () -> Void
    var onSave: (RelationshipsActivityItem) -> Void
    var onDelete: () -> Void

    @State private var name: String
    @State private var impact: String
    @State private var relationship: String
    @State private var whenLabel: String
    @State private var notes: String
    @State private var selectedTags: Set<String>

    private let chips = ["Romantic", "Supportive", "Playful", "Vulnerable", "Intentional"]
    private let pink = Color(hex: "#E12A9E")
    private let green = Color(hex: "#10B981")

    init(
        item: RelationshipsActivityItem,
        onClose: @escaping () -> Void,
        onSave: @escaping (RelationshipsActivityItem) -> Void,
        onDelete: @escaping () -> Void
    ) {
        self.item = item
        self.onClose = onClose
        self.onSave = onSave
        self.onDelete = onDelete
        _name = State(initialValue: item.title)
        _impact = State(initialValue: item.impact)
        _relationship = State(initialValue: item.relationship)
        _whenLabel = State(initialValue: item.whenLabel)
        _notes = State(initialValue: item.notes.isEmpty
            ? "Spent the evening cooking dinner together and watching a movie. Really felt present and connected. Need to do this more often."
            : item.notes)
        _selectedTags = State(initialValue: Set(item.tags.isEmpty ? ["Romantic"] : item.tags))
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

                field("ACTIVITY NAME", text: $name, focused: true)
                HStack(spacing: 12) {
                    field("IMPACT", text: $impact, focused: true)
                    VStack(alignment: .leading, spacing: 6) {
                        Text("RELATIONSHIP").font(.system(size: 11, weight: .semibold)).foregroundStyle(Color(hex: "#64748B"))
                        Picker("", selection: $relationship) {
                            ForEach(["Partner", "Family", "Friends", "Self"], id: \.self) { Text($0).tag($0) }
                        }
                        .pickerStyle(.menu)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 8)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(Color(hex: "#3A3842"))
                        .overlay(RoundedRectangle(cornerRadius: 14).stroke(Color(hex: "#938EA1")))
                        .clipShape(RoundedRectangle(cornerRadius: 14))
                    }
                }
                field("DATE & TIME", text: $whenLabel, focused: false)

                VStack(alignment: .leading, spacing: 8) {
                    Text("NOTES").font(.system(size: 11, weight: .semibold)).foregroundStyle(Color(hex: "#64748B"))
                    TextEditor(text: Binding(
                        get: { notes },
                        set: { notes = String($0.prefix(200)) }
                    ))
                    .frame(minHeight: 72)
                    .scrollContentBackground(.hidden)
                    .foregroundStyle(Color(hex: "#E5E0EE"))
                    HStack {
                        Text("Max 200 characters").font(.system(size: 11)).foregroundStyle(Color(hex: "#64748B"))
                        Spacer()
                        Text("\(min(notes.count, 200))/200").font(.system(size: 11, weight: .semibold)).foregroundStyle(green)
                    }
                }
                .padding(12)
                .background(Color(hex: "#14121B"))
                .overlay(RoundedRectangle(cornerRadius: 14).stroke(Color(hex: "#2A2538")))
                .clipShape(RoundedRectangle(cornerRadius: 14))

                FlowLayout(spacing: 8) {
                    ForEach(chips, id: \.self) { chip in
                        let selected = selectedTags.contains(chip)
                        Text(chip)
                            .font(.system(size: 12, weight: .medium))
                            .foregroundStyle(selected ? pink : Color(hex: "#9CA3AF"))
                            .padding(.horizontal, 12)
                            .padding(.vertical, 6)
                            .background(selected ? pink.opacity(0.15) : Color(hex: "#2A2538"))
                            .overlay(Capsule().stroke(selected ? pink.opacity(0.25) : .clear))
                            .clipShape(Capsule())
                            .onTapGesture {
                                if selected { selectedTags.remove(chip) } else { selectedTags.insert(chip) }
                            }
                    }
                }

                Button {
                    onSave(RelationshipsActivityItem(
                        id: item.id,
                        title: name,
                        whenLabel: whenLabel,
                        impact: impact,
                        emoji: item.emoji,
                        relationship: relationship,
                        notes: notes,
                        tags: Array(selectedTags),
                        filter: relationship
                    ))
                } label: {
                    Text("Save Changes")
                        .font(.system(size: 17, weight: .bold))
                        .foregroundStyle(Color(hex: "#14121B"))
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 16)
                        .background(green)
                        .clipShape(Capsule())
                }

                Button("Delete Activity", role: .destructive, action: onDelete)
                    .font(.system(size: 14, weight: .semibold))
            }
            .padding(.horizontal, 16)
            .padding(.bottom, 24)
        }
        .background(Color(hex: "#1C1926"))
    }

    private func field(_ label: String, text: Binding<String>, focused: Bool) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(label).font(.system(size: 11, weight: .semibold)).foregroundStyle(Color(hex: "#64748B"))
            TextField("", text: text)
                .padding(.horizontal, 14)
                .padding(.vertical, 12)
                .background(Color(hex: "#3A3842"))
                .overlay(RoundedRectangle(cornerRadius: 14).stroke(focused ? pink : Color(hex: "#938EA1")))
                .shadow(color: focused ? pink.opacity(0.35) : .clear, radius: 8)
                .clipShape(RoundedRectangle(cornerRadius: 14))
                .foregroundStyle(Color(hex: "#E5E0EE"))
        }
    }
}

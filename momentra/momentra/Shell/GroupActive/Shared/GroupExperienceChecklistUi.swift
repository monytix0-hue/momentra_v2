import SwiftUI

/// Quick Add checklist sheet: title + category chips + seed packing list.
struct ExperienceChecklistBody: View {
    var momentId: String?
    var onDismiss: () -> Void
    var onSaved: () -> Void
    var accent: SheetAccent = SheetAccent(
        accent: Color(hex: "#14B8A6"),
        accentEnd: Color(hex: "#0F766E"),
        soft: Color(hex: "#14B8A6").opacity(0.2)
    )
    /// When set, sheet edits an existing checklist item instead of creating.
    var editingItem: GroupPlanningItem? = nil

    @State private var title = ""
    @State private var selectedCategoryLabel = GroupExperienceChecklistCatalog.defaultLabel()
    @State private var submitting = false
    @State private var seeding = false
    @State private var deleting = false
    @State private var showDeleteConfirm = false
    @State private var error: String?
    @State private var didPrefill = false

    private var isEditing: Bool { editingItem?.planningItemId != nil }

    private var categoryCode: String {
        GroupExperienceChecklistCatalog.categories
            .first(where: { $0.label == selectedCategoryLabel })?.code
            ?? GroupExperienceChecklistCatalog.defaultCode()
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            SheetHeader(
                icon: "checklist",
                title: isEditing ? "Edit checklist" : "Checklist",
                subtitle: isEditing ? "Update packing & essentials" : "Shared packing & essentials",
                accent: accent
            )

            VStack(alignment: .leading, spacing: 8) {
                FieldLabel(text: "Item")
                SheetField(value: $title, placeholder: "What to pack or prepare", minHeight: 42)
            }

            VStack(alignment: .leading, spacing: 8) {
                FieldLabel(text: "Category")
                ChipRow(
                    options: GroupExperienceChecklistCatalog.categories.map(\.label),
                    selected: $selectedCategoryLabel,
                    accent: accent
                )
            }

            if let error {
                Text(error)
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(Color(hex: "#F87171"))
            }

            PrimaryCta(
                label: isEditing ? "Save" : "Add",
                enabled: !(momentId ?? "").isEmpty
                    && !title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
                    && !submitting
                    && !seeding
                    && !deleting,
                accent: accent,
                loading: submitting
            ) {
                Task { await saveItem() }
            }

            if isEditing {
                Button {
                    showDeleteConfirm = true
                } label: {
                    Text(deleting ? "Deleting…" : "Delete from this moment")
                        .font(.plusJakarta(size: 14, weight: .semibold))
                        .foregroundStyle(Color(hex: "#F87171"))
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.vertical, 8)
                }
                .buttonStyle(.plain)
                .disabled(submitting || deleting || (momentId ?? "").isEmpty)
            } else {
                PrimaryCta(
                    label: seeding ? "Seeding…" : "Seed packing list",
                    enabled: !(momentId ?? "").isEmpty && !submitting && !seeding,
                    accent: accent,
                    loading: seeding,
                    lightLabel: true
                ) {
                    Task { await seedPackingList() }
                }
            }
        }
        .onAppear { prefillIfNeeded() }
        .confirmationDialog(
            "Delete this checklist item?",
            isPresented: $showDeleteConfirm,
            titleVisibility: .visible
        ) {
            Button("Delete", role: .destructive) {
                Task { await deleteItem() }
            }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("Removes it from this moment only. The shared packing seed list is unchanged.")
        }
    }

    private func prefillIfNeeded() {
        guard !didPrefill, let item = editingItem else { return }
        didPrefill = true
        title = item.title ?? ""
        if let code = item.categoryCode, GroupExperienceChecklistCatalog.isChecklistCode(code) {
            selectedCategoryLabel = GroupExperienceChecklistCatalog.label(forCode: code)
        }
    }

    @MainActor
    private func saveItem() async {
        guard let momentId, !momentId.isEmpty else { return }
        let trimmed = title.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }
        submitting = true
        error = nil
        defer { submitting = false }
        do {
            if let planningItemId = editingItem?.planningItemId {
                _ = try await APIClient.shared.updatePlanningItem(
                    momentId: momentId,
                    planningItemId: planningItemId,
                    title: trimmed,
                    categoryCode: categoryCode,
                    status: editingItem?.status
                )
            } else {
                _ = try await APIClient.shared.createPlanningItem(
                    momentId: momentId,
                    title: trimmed,
                    categoryCode: categoryCode
                )
            }
            onSaved()
            onDismiss()
        } catch {
            self.error = error.localizedDescription
        }
    }

    @MainActor
    private func deleteItem() async {
        guard let momentId, !momentId.isEmpty,
              let planningItemId = editingItem?.planningItemId
        else { return }
        deleting = true
        error = nil
        defer { deleting = false }
        do {
            _ = try await APIClient.shared.deletePlanningItem(
                momentId: momentId,
                planningItemId: planningItemId
            )
            onSaved()
            onDismiss()
        } catch {
            self.error = error.localizedDescription
        }
    }

    @MainActor
    private func seedPackingList() async {
        guard let momentId, !momentId.isEmpty else { return }
        seeding = true
        error = nil
        defer { seeding = false }
        do {
            let existing = (try? await APIClient.shared.listPlanningItems(momentId: momentId))?.items ?? []
            var existingKeys = Set(
                existing
                    .filter { GroupExperienceChecklistCatalog.isChecklistCode($0.categoryCode) }
                    .compactMap { item -> String? in
                        guard let t = item.title?.trimmingCharacters(in: .whitespacesAndNewlines).lowercased(),
                              let c = item.categoryCode?.trimmingCharacters(in: .whitespacesAndNewlines).uppercased()
                        else { return nil }
                        return "\(c)|\(t)"
                    }
            )
            var created = 0
            for seed in GroupExperienceChecklistCatalog.seedPackingList {
                let key = "\(seed.code)|\(seed.title.trimmingCharacters(in: .whitespacesAndNewlines).lowercased())"
                if existingKeys.contains(key) { continue }
                _ = try await APIClient.shared.createPlanningItem(
                    momentId: momentId,
                    title: seed.title,
                    categoryCode: seed.code
                )
                existingKeys.insert(key)
                created += 1
            }
            onSaved()
            if created > 0 {
                onDismiss()
            } else {
                error = "Packing list already seeded"
            }
        } catch {
            self.error = error.localizedDescription
        }
    }
}

/// Moments Checklist section grouped by category with DONE ↔ OPEN toggle.
struct MomentsChecklistSection: View {
    let items: [GroupPlanningItem]
    var momentId: String?
    var chrome: MomentsChrome
    var onChanged: () -> Void = {}
    var onAdd: (() -> Void)? = nil

    @State private var togglingId: String?
    /// Category codes that are collapsed. Empty = all expanded (least surprise).
    @State private var collapsedCodes: Set<String> = []
    @State private var editingItem: GroupPlanningItem?

    private var groups: [(category: GroupExperienceChecklistCatalog.Category, items: [GroupPlanningItem])] {
        GroupExperienceChecklistCatalog.groupedByCategory(items)
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            MomentsSectionHeader(title: "Checklist  ✅", chrome: chrome)

            if groups.isEmpty {
                GroupEmptySection(
                    message: "No checklist items yet",
                    detail: "Add essentials from Quick Add, or seed the packing list."
                )
                if let onAdd {
                    Button("Add checklist item", action: onAdd)
                        .font(.plusJakarta(size: 12, weight: .semibold))
                        .foregroundStyle(chrome.accent)
                        .buttonStyle(.plain)
                }
            } else {
                ForEach(groups, id: \.category.code) { group in
                    let expanded = !collapsedCodes.contains(group.category.code)
                    let doneCount = group.items.filter {
                        ($0.status ?? "").caseInsensitiveCompare("DONE") == .orderedSame
                    }.count
                    VStack(alignment: .leading, spacing: 6) {
                        Button {
                            if expanded {
                                collapsedCodes.insert(group.category.code)
                            } else {
                                collapsedCodes.remove(group.category.code)
                            }
                        } label: {
                            HStack(spacing: 6) {
                                Image(systemName: expanded ? "chevron.down" : "chevron.right")
                                    .font(.system(size: 11, weight: .bold))
                                    .foregroundStyle(chrome.accent)
                                Text(group.category.label)
                                    .font(.plusJakarta(size: 12, weight: .bold))
                                    .foregroundStyle(chrome.accent)
                                Spacer(minLength: 0)
                                Text("\(doneCount)/\(group.items.count)")
                                    .font(.plusJakarta(size: 11, weight: .semibold))
                                    .foregroundStyle(chrome.secondary)
                            }
                        }
                        .buttonStyle(.plain)

                        if expanded {
                            ForEach(Array(group.items.enumerated()), id: \.offset) { _, item in
                                checklistRow(item)
                            }
                        }
                    }
                }
                if let onAdd {
                    Button("Add item", action: onAdd)
                        .font(.plusJakarta(size: 12, weight: .semibold))
                        .foregroundStyle(chrome.accent)
                        .buttonStyle(.plain)
                }
            }
        }
        .sheet(isPresented: Binding(
            get: { editingItem != nil },
            set: { if !$0 { editingItem = nil } }
        )) {
            if let item = editingItem {
                NativeSheetScaffold(
                    title: "Edit checklist",
                    onClose: { editingItem = nil },
                    background: chrome.card
                ) {
                    ScrollView {
                        ExperienceChecklistBody(
                            momentId: momentId,
                            onDismiss: { editingItem = nil },
                            onSaved: {
                                editingItem = nil
                                onChanged()
                            },
                            accent: SheetAccent(
                                accent: chrome.accent,
                                accentEnd: chrome.accent,
                                soft: chrome.accent.opacity(0.2)
                            ),
                            editingItem: item
                        )
                        .padding(.horizontal, 20)
                        .padding(.top, 16)
                        .padding(.bottom, 28)
                    }
                }
                .presentationDetents([.medium, .large])
                .presentationDragIndicator(.visible)
            }
        }
    }

    @ViewBuilder
    private func checklistRow(_ item: GroupPlanningItem) -> some View {
        let done = (item.status ?? "").caseInsensitiveCompare("DONE") == .orderedSame
        let id = item.planningItemId
        HStack(spacing: 4) {
            Button {
                guard let id, let momentId, !momentId.isEmpty, togglingId == nil else { return }
                let next = done ? "OPEN" : "DONE"
                Task { await toggle(item: item, id: id, momentId: momentId, status: next) }
            } label: {
                Image(systemName: done ? "checkmark.square.fill" : "square")
                    .font(.system(size: 20))
                    .foregroundStyle(done ? chrome.accent : chrome.secondary)
            }
            .buttonStyle(.plain)
            .disabled(id == nil || momentId == nil || togglingId == id)

            Text(item.title ?? "Item")
                .font(.plusJakarta(size: 13, weight: .medium))
                .foregroundStyle(done ? chrome.secondary : chrome.text)
                .strikethrough(done)
            Spacer(minLength: 0)

            if id != nil, momentId != nil {
                Button {
                    editingItem = item
                } label: {
                    Text("Edit")
                        .font(.plusJakarta(size: 12, weight: .semibold))
                        .foregroundStyle(chrome.accent)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 4)
                }
                .buttonStyle(.plain)
                .accessibilityLabel("Edit checklist item")
            }
        }
        .padding(.horizontal, 8)
        .padding(.vertical, 8)
        .background(chrome.card)
        .overlay(RoundedRectangle(cornerRadius: 12).stroke(chrome.border, lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }

    @MainActor
    private func toggle(item: GroupPlanningItem, id: String, momentId: String, status: String) async {
        togglingId = id
        defer { togglingId = nil }
        do {
            _ = try await APIClient.shared.updatePlanningItem(
                momentId: momentId,
                planningItemId: id,
                title: item.title ?? "Item",
                categoryCode: item.categoryCode,
                status: status
            )
            onChanged()
        } catch {
            // Keep prior UI state; list refresh will reconcile.
        }
    }
}

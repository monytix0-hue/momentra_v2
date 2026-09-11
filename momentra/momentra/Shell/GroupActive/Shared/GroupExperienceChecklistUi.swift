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

    @State private var title = ""
    @State private var selectedCategoryLabel = GroupExperienceChecklistCatalog.defaultLabel()
    @State private var submitting = false
    @State private var seeding = false
    @State private var error: String?

    private var categoryCode: String {
        GroupExperienceChecklistCatalog.categories
            .first(where: { $0.label == selectedCategoryLabel })?.code
            ?? GroupExperienceChecklistCatalog.defaultCode()
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            SheetHeader(
                icon: "checklist",
                title: "Checklist",
                subtitle: "Shared packing & essentials",
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
                label: "Add",
                enabled: !(momentId ?? "").isEmpty
                    && !title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
                    && !submitting
                    && !seeding,
                accent: accent,
                loading: submitting
            ) {
                Task { await addItem() }
            }

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

    @MainActor
    private func addItem() async {
        guard let momentId, !momentId.isEmpty else { return }
        let trimmed = title.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }
        submitting = true
        error = nil
        defer { submitting = false }
        do {
            _ = try await APIClient.shared.createPlanningItem(
                momentId: momentId,
                title: trimmed,
                categoryCode: categoryCode
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
                    VStack(alignment: .leading, spacing: 6) {
                        Text(group.category.label)
                            .font(.plusJakarta(size: 12, weight: .bold))
                            .foregroundStyle(chrome.accent)
                        ForEach(Array(group.items.enumerated()), id: \.offset) { _, item in
                            checklistRow(item)
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

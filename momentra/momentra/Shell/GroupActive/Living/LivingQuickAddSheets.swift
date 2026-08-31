import SwiftUI

/// Living Quick Add sheets — theme-accented; House Rule is a GAP (disabled CTA).
struct LivingGapQuickAddSheet: View {
    let theme: LivingActiveTheme
    let kind: LivingQuickAddKind
    var momentId: String? = nil
    var onClose: () -> Void
    var onSaved: () -> Void = {}

    private var accent: SheetAccent {
        SheetAccent(accent: theme.accent, accentEnd: theme.accentSolid, soft: theme.accentSoft)
    }

    var body: some View {
        ZStack(alignment: .top) {
            Color(hex: "#1C1A24").ignoresSafeArea()
            VStack(spacing: 0) {
                Capsule()
                    .fill(Color(hex: "#625E70"))
                    .frame(width: 48, height: 4)
                    .padding(.top, 12)
                ScrollView {
                    VStack(alignment: .leading, spacing: 16) {
                        sheetBody
                    }
                    .padding(.horizontal, 20)
                    .padding(.top, 16)
                    .padding(.bottom, 28)
                }
            }
        }
    }

    @ViewBuilder
    private var sheetBody: some View {
        switch kind {
        case .expense:
            WeddingExpenseBody(momentId: momentId, onDismiss: onClose, onSaved: onSaved, accent: accent)
        case .contribution:
            WeddingContributionBody(momentId: momentId, onDismiss: onClose, onSaved: onSaved, accent: accent)
        case .poll:
            WeddingPollBody(momentId: momentId, onDismiss: onClose, onSaved: onSaved, accent: accent)
        case .update:
            WeddingUpdateBody(momentId: momentId, onDismiss: onClose, onSaved: onSaved, accent: accent)
        case .memory:
            WeddingMemoryBody(momentId: momentId, onDismiss: onClose, onSaved: onSaved, accent: accent)
        case .task:
            WeddingPlanningBody(momentId: momentId, onDismiss: onClose, onSaved: onSaved, accent: accent)
        case .resident:
            LivingResidentBody(momentId: momentId, theme: theme, onDismiss: onClose, onSaved: onSaved, accent: accent)
        case .asset:
            LivingSharedAssetBody(momentId: momentId, onDismiss: onClose, onSaved: onSaved, accent: accent)
        case .maintenance:
            LivingMaintenanceBody(momentId: momentId, onDismiss: onClose, onSaved: onSaved, accent: accent)
        case .rule:
            LivingHouseRuleBody(theme: theme, accent: accent)
        }
    }
}

// MARK: - Resident (live)

private struct LivingResidentBody: View {
    var momentId: String?
    let theme: LivingActiveTheme
    var onDismiss: () -> Void
    var onSaved: () -> Void
    var accent: SheetAccent

    @State private var name = ""
    @State private var role = ""
    @State private var busy = false
    @State private var error: String?

    private var canSave: Bool {
        momentId != nil && !name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty && !busy
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            SheetHeader(
                icon: "person.badge.plus",
                title: "Add Resident",
                subtitle: theme.participantSubtitle,
                accent: accent
            )

            VStack(alignment: .leading, spacing: 8) {
                FieldLabel(text: "Resident Name")
                SheetField(value: $name, placeholder: "Full name", minHeight: 42)
            }

            VStack(alignment: .leading, spacing: 8) {
                FieldLabel(text: "Role")
                ChipRow(options: theme.participantRoles, selected: $role, accent: accent)
            }

            if let error {
                Text(error)
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(Color(hex: "#F87171"))
            }

            PrimaryCta(
                label: busy ? "Saving…" : "Add Resident",
                enabled: canSave,
                accent: accent,
                loading: busy,
                lightLabel: true
            ) {
                Task { await save() }
            }
        }
        .onAppear {
            if role.isEmpty {
                role = theme.participantRoles.first ?? "Resident"
            }
        }
    }

    private func save() async {
        guard let momentId else { return }
        let trimmed = name.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }
        busy = true
        error = nil
        do {
            _ = try await APIClient.shared.addResident(
                momentId: momentId,
                name: trimmed,
                roleCode: role.isEmpty ? nil : role.uppercased().replacingOccurrences(of: " ", with: "_")
            )
            onSaved()
            onDismiss()
        } catch {
            self.error = error.localizedDescription
        }
        busy = false
    }
}

// MARK: - Shared Asset (live)

private struct LivingSharedAssetBody: View {
    var momentId: String?
    var onDismiss: () -> Void
    var onSaved: () -> Void
    var accent: SheetAccent

    @State private var title = ""
    @State private var assetType = ""
    @State private var condition = "GOOD"
    @State private var busy = false
    @State private var error: String?

    private let conditions = ["NEW", "GOOD", "FAIR", "POOR", "OUT_OF_SERVICE"]

    private var canSave: Bool {
        momentId != nil && !title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty && !busy
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            SheetHeader(
                icon: "shippingbox.fill",
                title: "Add Shared Asset",
                subtitle: "Track something the household owns together",
                accent: accent
            )

            VStack(alignment: .leading, spacing: 8) {
                FieldLabel(text: "Asset Title")
                SheetField(value: $title, placeholder: "What is shared?", minHeight: 42)
            }

            VStack(alignment: .leading, spacing: 8) {
                FieldLabel(text: "Asset Type (optional)")
                SheetField(value: $assetType, placeholder: "Furniture, appliance, tool…", minHeight: 42)
            }

            VStack(alignment: .leading, spacing: 8) {
                FieldLabel(text: "Condition")
                ChipRow(options: conditions, selected: $condition, accent: accent)
            }

            if let error {
                Text(error)
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(Color(hex: "#F87171"))
            }

            PrimaryCta(
                label: busy ? "Saving…" : "Add Shared Asset",
                enabled: canSave,
                accent: accent,
                loading: busy,
                lightLabel: true
            ) {
                Task { await save() }
            }
        }
    }

    private func save() async {
        guard let momentId else { return }
        let trimmed = title.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }
        busy = true
        error = nil
        do {
            let type = assetType.trimmingCharacters(in: .whitespacesAndNewlines)
            _ = try await APIClient.shared.createSharedAsset(
                momentId: momentId,
                title: trimmed,
                assetType: type.isEmpty ? nil : type,
                conditionCode: condition.isEmpty ? nil : condition
            )
            onSaved()
            onDismiss()
        } catch {
            self.error = error.localizedDescription
        }
        busy = false
    }
}

// MARK: - Maintenance (live)

private struct LivingMaintenanceBody: View {
    var momentId: String?
    var onDismiss: () -> Void
    var onSaved: () -> Void
    var accent: SheetAccent

    @State private var title = ""
    @State private var description = ""
    @State private var busy = false
    @State private var error: String?

    private var canSave: Bool {
        momentId != nil && !title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty && !busy
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            SheetHeader(
                icon: "wrench.and.screwdriver.fill",
                title: "Log Maintenance",
                subtitle: "Record household upkeep or repairs",
                accent: accent
            )

            VStack(alignment: .leading, spacing: 8) {
                FieldLabel(text: "Title")
                SheetField(value: $title, placeholder: "What needs attention?", minHeight: 42)
            }

            VStack(alignment: .leading, spacing: 8) {
                FieldLabel(text: "Description")
                SheetField(value: $description, placeholder: "Details, vendor, notes…", singleLine: false, minHeight: 64)
            }

            if let error {
                Text(error)
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(Color(hex: "#F87171"))
            }

            PrimaryCta(
                label: busy ? "Saving…" : "Log Maintenance",
                enabled: canSave,
                accent: accent,
                loading: busy,
                lightLabel: true
            ) {
                Task { await save() }
            }
        }
    }

    private func save() async {
        guard let momentId else { return }
        let trimmed = title.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }
        busy = true
        error = nil
        do {
            let desc = description.trimmingCharacters(in: .whitespacesAndNewlines)
            _ = try await APIClient.shared.createMaintenanceRecord(
                momentId: momentId,
                title: trimmed,
                description: desc.isEmpty ? nil : desc
            )
            onSaved()
            onDismiss()
        } catch {
            self.error = error.localizedDescription
        }
        busy = false
    }
}

// MARK: - House Rule (GAP)

private struct LivingHouseRuleBody: View {
    let theme: LivingActiveTheme
    var accent: SheetAccent

    @State private var title = ""
    @State private var ruleText = ""

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            SheetHeader(
                icon: "list.bullet.rectangle",
                title: "House Rule",
                subtitle: "Set a shared expectation for the \(theme.typeLabel.lowercased())",
                accent: accent
            )

            VStack(alignment: .leading, spacing: 8) {
                FieldLabel(text: "Rule Title")
                SheetField(value: $title, placeholder: "Quiet hours, dishes, guests…", minHeight: 42)
            }

            VStack(alignment: .leading, spacing: 8) {
                FieldLabel(text: "Rule Details")
                SheetField(value: $ruleText, placeholder: "Describe the house rule…", singleLine: false, minHeight: 96)
            }

            Text("House Rule API is not wired in this shell yet — Coming soon.")
                .font(.plusJakarta(size: 12))
                .foregroundStyle(Color(hex: "#9E9AA8"))

            PrimaryCta(label: "Coming soon", enabled: false, accent: accent, lightLabel: true) {}
        }
    }
}

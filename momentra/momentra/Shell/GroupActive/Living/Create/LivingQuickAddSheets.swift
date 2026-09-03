import SwiftUI

/// Living Quick Add sheets — theme-accented; House Rule is a GAP (disabled CTA).
struct LivingGapQuickAddSheet: View {
    let theme: LivingActiveTheme
    let kind: LivingQuickAddKind
    var momentId: String? = nil
    var momentTypeCode: String? = nil
    var onClose: () -> Void
    var onSaved: () -> Void = {}

    private var accent: SheetAccent {
        SheetAccent(accent: theme.accent, accentEnd: theme.accentSolid, soft: theme.accentSoft)
    }

    var body: some View {
        if kind == .expense, let momentId {
            GroupExpenseSheet(
                momentId: momentId,
                isPresented: Binding(
                    get: { true },
                    set: { if !$0 { onClose() } }
                ),
                momentTypeCode: momentTypeCode,
                onSaved: {
                    onSaved()
                    onClose()
                }
            )
        } else {
            NativeSheetScaffold(
                title: kind.label,
                onClose: onClose,
                background: Color(hex: "#1C1A24")
            ) {
                ScrollView {
                    sheetBody
                        .padding(.horizontal, 20)
                        .padding(.top, 16)
                }
            }
            .presentationDetents([.large])
            .presentationDragIndicator(.visible)
        }
    }

    @ViewBuilder
    private var sheetBody: some View {
        switch kind {
        case .expense:
            EmptyView()
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
            LivingHouseRuleBody(momentId: momentId, theme: theme, onDismiss: onClose, onSaved: onSaved, accent: accent)
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
    @State private var contact = ""
    @State private var room = ""
    @State private var moveInDate = ""
    @State private var rentShare = ""
    @State private var role = ""
    @State private var sendInvitation = true
    @State private var addToExpenseSplit = true
    @State private var busy = false
    @State private var error: String?

    private let roomOptions = ["Unassigned", "Room 1", "Room 2", "Room 3", "Common area"]

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
                FieldLabel(text: "Full Name")
                SheetField(value: $name, placeholder: "Full name", minHeight: 42)
            }

            VStack(alignment: .leading, spacing: 8) {
                FieldLabel(text: "Email or Phone")
                SheetField(value: $contact, placeholder: "email@example.com or +91…", minHeight: 42)
            }

            VStack(alignment: .leading, spacing: 8) {
                FieldLabel(text: "Room Assignment")
                Menu {
                    ForEach(roomOptions, id: \.self) { option in
                        Button(option) { room = option }
                    }
                } label: {
                    SheetField(
                        value: .constant(room.isEmpty ? "" : room),
                        placeholder: "Select room",
                        minHeight: 42,
                        trailing: {
                            AnyView(
                                Image(systemName: "chevron.down")
                                    .font(.system(size: 12))
                                    .foregroundStyle(Color(hex: "#9E9AA8"))
                            )
                        }
                    )
                }
            }

            VStack(alignment: .leading, spacing: 8) {
                FieldLabel(text: "Move-in Date")
                WeddingDatePickField(value: $moveInDate, placeholder: "Select date")
            }

            VStack(alignment: .leading, spacing: 8) {
                FieldLabel(text: "Monthly Rent Share")
                SheetField(
                    value: $rentShare,
                    placeholder: "0.00",
                    minHeight: 42,
                    leading: {
                        AnyView(
                            Text("₹")
                                .font(.plusJakarta(size: 18, weight: .bold))
                                .foregroundStyle(accent.accent)
                        )
                    },
                    keyboardType: .decimalPad
                )
                .onChange(of: rentShare) { _, new in
                    rentShare = new.filter { $0.isNumber || $0 == "." }
                }
            }

            VStack(alignment: .leading, spacing: 8) {
                FieldLabel(text: "Role")
                ChipRow(options: theme.participantRoles, selected: $role, accent: accent)
            }

            LivingToggleRow(
                title: "Send Invitation",
                subtitle: "Email/SMS joining link to this person",
                isOn: $sendInvitation,
                accent: accent.accent
            )
            LivingToggleRow(
                title: "Add to Expense Split",
                subtitle: "Auto-add to active shared bills",
                isOn: $addToExpenseSplit,
                accent: accent.accent
            )

            if let error {
                Text(error)
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(Color(hex: "#F87171"))
            }

            PrimaryCta(
                label: busy ? "Saving…" : (sendInvitation ? "Invite Resident" : "Add Resident"),
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
        // contact / room / moveInDate / rentShare / toggles: SCHEMA_GAP — UI parity only
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

private struct LivingToggleRow: View {
    let title: String
    let subtitle: String
    @Binding var isOn: Bool
    var accent: Color

    var body: some View {
        HStack(alignment: .center) {
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.plusJakarta(size: 14, weight: .semibold))
                    .foregroundStyle(Color(hex: "#E5E0EE"))
                Text(subtitle)
                    .font(.plusJakarta(size: 11))
                    .foregroundStyle(Color(hex: "#9E9AA8"))
            }
            Spacer()
            Toggle("", isOn: $isOn)
                .labelsHidden()
                .tint(accent)
        }
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

// MARK: - House Rule

private struct LivingHouseRuleBody: View {
    var momentId: String?
    let theme: LivingActiveTheme
    var onDismiss: () -> Void
    var onSaved: () -> Void
    var accent: SheetAccent

    @State private var title = ""
    @State private var ruleText = ""
    @State private var busy = false
    @State private var error: String?

    private var canSave: Bool {
        momentId != nil
            && !title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
            && !ruleText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
            && !busy
    }

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

            if let error {
                Text(error)
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(Color(hex: "#F87171"))
            }

            PrimaryCta(
                label: busy ? "Saving…" : "Save House Rule",
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
        let trimmedTitle = title.trimmingCharacters(in: .whitespacesAndNewlines)
        let trimmedRule = ruleText.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmedTitle.isEmpty, !trimmedRule.isEmpty else { return }
        busy = true
        error = nil
        do {
            _ = try await APIClient.shared.createLivingRule(momentId: momentId, title: trimmedTitle, ruleText: trimmedRule)
            onSaved()
            onDismiss()
        } catch {
            self.error = error.localizedDescription
        }
        busy = false
    }
}

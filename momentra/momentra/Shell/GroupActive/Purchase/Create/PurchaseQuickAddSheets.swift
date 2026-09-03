import SwiftUI

/// Figma Gift Pool sheets 605:8670 family — moment-colored Purchase Quick Add sheets.
struct PurchaseGapQuickAddSheet: View {
    let theme: PurchaseActiveTheme
    let kind: PurchaseQuickAddKind
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
        case .budget:
            WeddingBudgetBody(momentId: momentId, onDismiss: onClose, onSaved: onSaved, accent: accent)
        case .poll:
            WeddingPollBody(momentId: momentId, onDismiss: onClose, onSaved: onSaved, accent: accent)
        case .update:
            WeddingUpdateBody(momentId: momentId, onDismiss: onClose, onSaved: onSaved, accent: accent)
        case .memory:
            WeddingMemoryBody(momentId: momentId, onDismiss: onClose, onSaved: onSaved, accent: accent)
        case .vendor:
            WeddingVendorBody(momentId: momentId, onDismiss: onClose, onSaved: onSaved, accent: accent)
        case .contributor:
            PurchaseParticipantBody(momentId: momentId, theme: theme, onDismiss: onClose, onSaved: onSaved, accent: accent)
        case .purchaseItem:
            PurchaseItemBody(momentId: momentId, onDismiss: onClose, onSaved: onSaved, accent: accent)
        case .delivery:
            PurchaseDeliveryBody(momentId: momentId, theme: theme, onDismiss: onClose, onSaved: onSaved, accent: accent)
        case .ownership:
            PurchaseOwnershipBody(momentId: momentId, theme: theme, onDismiss: onClose, onSaved: onSaved, accent: accent)
        }
    }
}

// MARK: - Contributor (gap invite UI)

private struct PurchaseParticipantBody: View {
    var momentId: String?
    let theme: PurchaseActiveTheme
    var onDismiss: () -> Void
    var onSaved: () -> Void
    var accent: SheetAccent

    @State private var name = ""
    @State private var contact = ""
    @State private var role = ""
    @State private var notes = ""
    @State private var busy = false
    @State private var error: String?

    private var canSave: Bool {
        momentId != nil && !name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty && !busy
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            SheetHeader(
                icon: "person.badge.plus",
                title: "Add Contributor",
                subtitle: theme.participantSubtitle,
                accent: accent
            )

            VStack(alignment: .leading, spacing: 8) {
                FieldLabel(text: "Contributor Name")
                SheetField(value: $name, placeholder: "Full name", minHeight: 42)
            }

            VStack(alignment: .leading, spacing: 8) {
                FieldLabel(text: "Email or Phone")
                SheetField(value: $contact, placeholder: "Email or phone", minHeight: 42, keyboardType: .emailAddress)
            }

            VStack(alignment: .leading, spacing: 8) {
                FieldLabel(text: "Role")
                ChipRow(options: theme.participantRoles, selected: $role, accent: accent)
            }

            VStack(alignment: .leading, spacing: 8) {
                FieldLabel(text: "Notes")
                SheetField(value: $notes, placeholder: "Optional notes", minHeight: 42)
            }

            if let error {
                Text(error)
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(Color(hex: "#F87171"))
            }

            PrimaryCta(
                label: busy ? "Saving…" : "Add Contributor",
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
                role = theme.participantRoles.first ?? "Contributor"
            }
        }
    }

    private func save() async {
        guard let momentId else { return }
        let trimmed = name.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }
        busy = true
        error = nil
        let contactTrim = contact.trimmingCharacters(in: .whitespacesAndNewlines)
        let email = contactTrim.contains("@") ? contactTrim : nil
        let phone = email == nil && !contactTrim.isEmpty ? contactTrim : nil
        do {
            _ = try await APIClient.shared.addGroupParticipant(
                momentId: momentId,
                displayName: trimmed,
                roleCode: "CONTRIBUTOR",
                email: email,
                phone: phone
            )
            onSaved()
            onDismiss()
        } catch {
            self.error = error.localizedDescription
        }
        busy = false
    }
}

// MARK: - Purchase Item (live)

private struct PurchaseItemBody: View {
    var momentId: String?
    var onDismiss: () -> Void
    var onSaved: () -> Void
    var accent: SheetAccent

    @State private var label = ""
    @State private var amount = ""
    @State private var busy = false
    @State private var error: String?

    private var canSave: Bool {
        momentId != nil && !label.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty && !busy
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            SheetHeader(
                icon: "bag.fill",
                title: "Add Purchase Item",
                subtitle: "Track something the group is buying together",
                accent: accent
            )

            VStack(alignment: .leading, spacing: 8) {
                FieldLabel(text: "Item Label")
                SheetField(value: $label, placeholder: "What are you buying?", minHeight: 42)
            }

            VStack(alignment: .leading, spacing: 8) {
                FieldLabel(text: "Amount (optional)")
                SheetField(value: $amount, placeholder: "0.00", minHeight: 42, keyboardType: .decimalPad)
            }

            if let error {
                Text(error)
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(Color(hex: "#F87171"))
            }

            PrimaryCta(
                label: busy ? "Saving…" : "Add Purchase Item",
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
        let trimmed = label.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }
        busy = true
        error = nil
        do {
            let amt = amount.trimmingCharacters(in: .whitespacesAndNewlines)
            _ = try await APIClient.shared.createPurchaseItem(
                momentId: momentId,
                label: trimmed,
                amount: amt.isEmpty ? nil : amt
            )
            onSaved()
            onDismiss()
        } catch {
            self.error = error.localizedDescription
        }
        busy = false
    }
}

// MARK: - Delivery / Handover (gap)

private struct PurchaseDeliveryBody: View {
    var momentId: String?
    let theme: PurchaseActiveTheme
    var onDismiss: () -> Void
    var onSaved: () -> Void
    var accent: SheetAccent

    @State private var recipient = ""
    @State private var method = "Hand delivery"
    @State private var address = ""
    @State private var notes = ""
    @State private var dueDate = ""
    @State private var submitting = false
    @State private var error: String?

    private let methods = ["Hand delivery", "Courier", "Pickup", "Digital"]

    private var canSave: Bool {
        momentId != nil && !address.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty && !submitting
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            SheetHeader(
                icon: "shippingbox.fill",
                title: "Delivery & Handover",
                subtitle: "Plan how the \(theme.typeLabel.lowercased()) gets to the recipient",
                accent: accent
            )

            VStack(alignment: .leading, spacing: 8) {
                FieldLabel(text: "Recipient")
                SheetField(value: $recipient, placeholder: "Who receives it?", minHeight: 42)
            }

            VStack(alignment: .leading, spacing: 8) {
                FieldLabel(text: "Delivery Method")
                ChipRow(options: methods, selected: $method, accent: accent)
            }

            VStack(alignment: .leading, spacing: 8) {
                FieldLabel(text: "Address / Location")
                SheetField(value: $address, placeholder: "Delivery address or meetup spot", minHeight: 42)
            }

            VStack(alignment: .leading, spacing: 8) {
                FieldLabel(text: "Target Date")
                SheetField(value: $dueDate, placeholder: "YYYY-MM-DD", minHeight: 42)
            }

            VStack(alignment: .leading, spacing: 8) {
                FieldLabel(text: "Notes")
                SheetField(value: $notes, placeholder: "Tracking, gift wrap, instructions…", singleLine: false, minHeight: 64)
            }

            if let error {
                Text(error)
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(Color(hex: "#F87171"))
            }

            PrimaryCta(
                label: submitting ? "Saving…" : "Save Delivery Plan",
                enabled: canSave,
                accent: accent,
                loading: submitting,
                lightLabel: true
            ) {
                Task { await save() }
            }
        }
    }

    private func save() async {
        guard let momentId else { return }
        submitting = true
        error = nil
        let handoverType: String = switch method {
        case "Hand delivery": "HANDOVER"
        case "Pickup": "PICKUP"
        default: "DELIVERY"
        }
        do {
            _ = try await APIClient.shared.createDeliveryHandover(
                momentId: momentId,
                recipientName: recipient.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? nil : recipient.trimmingCharacters(in: .whitespacesAndNewlines),
                handoverType: handoverType,
                scheduledAt: dueDate.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? nil : dueDate.trimmingCharacters(in: .whitespacesAndNewlines),
                address: address.trimmingCharacters(in: .whitespacesAndNewlines),
                note: notes.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? nil : notes.trimmingCharacters(in: .whitespacesAndNewlines)
            )
            submitting = false
            onSaved()
            onDismiss()
        } catch {
            submitting = false
            self.error = error.localizedDescription
        }
    }
}

// MARK: - Transfer Ownership

private struct PurchaseOwnershipBody: View {
    var momentId: String?
    let theme: PurchaseActiveTheme
    var onDismiss: () -> Void
    var onSaved: () -> Void
    var accent: SheetAccent

    @State private var assetLabel = ""
    @State private var fromOwner = ""
    @State private var toOwner = ""
    @State private var transferDate = ""
    @State private var notes = ""
    @State private var submitting = false
    @State private var error: String?

    private var canSave: Bool {
        momentId != nil && !toOwner.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty && !submitting
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            SheetHeader(
                icon: "key.fill",
                title: "Transfer Ownership",
                subtitle: "Record a change of ownership for this \(theme.typeLabel.lowercased())",
                accent: accent
            )

            VStack(alignment: .leading, spacing: 8) {
                FieldLabel(text: "Asset / Item")
                SheetField(value: $assetLabel, placeholder: "What is being transferred?", minHeight: 42)
            }

            VStack(alignment: .leading, spacing: 8) {
                FieldLabel(text: "From")
                SheetField(value: $fromOwner, placeholder: "Current owner", minHeight: 42)
            }

            VStack(alignment: .leading, spacing: 8) {
                FieldLabel(text: "To")
                SheetField(value: $toOwner, placeholder: "New owner", minHeight: 42)
            }

            VStack(alignment: .leading, spacing: 8) {
                FieldLabel(text: "Transfer Date")
                SheetField(value: $transferDate, placeholder: "YYYY-MM-DD", minHeight: 42)
            }

            VStack(alignment: .leading, spacing: 8) {
                FieldLabel(text: "Notes")
                SheetField(value: $notes, placeholder: "Terms, documents, conditions…", singleLine: false, minHeight: 64)
            }

            if let error {
                Text(error)
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(Color(hex: "#F87171"))
            }

            PrimaryCta(
                label: submitting ? "Saving…" : "Record Transfer",
                enabled: canSave,
                accent: accent,
                loading: submitting,
                lightLabel: true
            ) {
                Task { await save() }
            }
        }
    }

    private func save() async {
        guard let momentId else { return }
        submitting = true
        error = nil
        do {
            _ = try await APIClient.shared.createOwnershipRecord(
                momentId: momentId,
                assetLabel: assetLabel.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? nil : assetLabel.trimmingCharacters(in: .whitespacesAndNewlines),
                fromOwnerName: fromOwner.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? nil : fromOwner.trimmingCharacters(in: .whitespacesAndNewlines),
                toParticipantName: toOwner.trimmingCharacters(in: .whitespacesAndNewlines),
                ownershipNote: notes.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? nil : notes.trimmingCharacters(in: .whitespacesAndNewlines),
                effectiveAt: transferDate.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? nil : transferDate.trimmingCharacters(in: .whitespacesAndNewlines)
            )
            submitting = false
            onSaved()
            onDismiss()
        } catch {
            submitting = false
            self.error = error.localizedDescription
        }
    }
}

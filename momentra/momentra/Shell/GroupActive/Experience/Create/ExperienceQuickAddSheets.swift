import SwiftUI

/// Figma party 592:8580 / outing 592:7770 — moment-colored Experience Quick Add sheets.
struct ExperienceGapQuickAddSheet: View {
    let theme: ExperienceActiveTheme
    let kind: ExperienceQuickAddKind
    var momentId: String? = nil
    var momentTypeCode: String? = nil
    var onClose: () -> Void
    var onSaved: () -> Void = {}
    var onBooking: () -> Void = {}

    private var accent: SheetAccent {
        SheetAccent(accent: theme.accent, accentEnd: theme.accentSolid, soft: theme.accentSoft)
    }

    var body: some View {
        if kind == .booking {
            Color.clear
                .onAppear {
                    onBooking()
                    onClose()
                }
        } else if kind == .expense, let momentId {
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
        case .participant:
            ExperienceParticipantBody(momentId: momentId, theme: theme, onDismiss: onClose, onSaved: onSaved, accent: accent)
        case .vendor:
            WeddingVendorBody(momentId: momentId, onDismiss: onClose, onSaved: onSaved, accent: accent)
        case .planning:
            WeddingPlanningBody(momentId: momentId, momentTypeCode: momentTypeCode, onDismiss: onClose, onSaved: onSaved, accent: accent)
        case .attendance:
            WeddingAttendanceBody(momentId: momentId, onDismiss: onClose, onSaved: onSaved, accent: accent)
        case .poll:
            WeddingPollBody(momentId: momentId, onDismiss: onClose, onSaved: onSaved, accent: accent)
        case .memory:
            WeddingMemoryBody(momentId: momentId, onDismiss: onClose, onSaved: onSaved, accent: accent)
        case .update:
            WeddingUpdateBody(momentId: momentId, onDismiss: onClose, onSaved: onSaved, accent: accent)
        case .settle:
            WeddingSettleBody(momentId: momentId, onDismiss: onClose, onSaved: onSaved)
        case .booking:
            EmptyView()
        }
    }
}

private struct ExperienceParticipantBody: View {
    var momentId: String?
    let theme: ExperienceActiveTheme
    var onDismiss: () -> Void
    var onSaved: () -> Void
    var accent: SheetAccent

    @State private var name = ""
    @State private var contact = ""
    @State private var role = ""
    @State private var rsvp = "Pending"
    @State private var plusOne = false
    @State private var notes = ""
    @State private var busy = false
    @State private var error: String?

    private var canSave: Bool {
        momentId != nil && !name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty && !busy
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            SheetHeader(
                icon: "person.2.fill",
                title: "Add Participant",
                subtitle: theme.participantSubtitle,
                accent: accent
            )

            VStack(alignment: .leading, spacing: 8) {
                FieldLabel(text: "Participant Name")
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
                FieldLabel(text: "RSVP Status")
                ChipRow(options: ["Confirmed", "Pending", "Declined"], selected: $rsvp, accent: accent)
            }

            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text("Plus One Allowed")
                        .font(.plusJakarta(size: 14, weight: .semibold))
                        .foregroundStyle(Color(hex: "#FFFFFF"))
                    Text("Include guest's spouse or partner")
                        .font(.plusJakarta(size: 12))
                        .foregroundStyle(Color(hex: "#9E9AA8"))
                }
                Spacer()
                Toggle("", isOn: $plusOne)
                    .labelsHidden()
                    .tint(accent.accent)
            }
            .padding(.vertical, 4)

            VStack(alignment: .leading, spacing: 8) {
                FieldLabel(text: "Dietary Preferences / Notes")
                SheetField(value: $notes, placeholder: "Optional notes", minHeight: 42)
            }

            if let error {
                Text(error)
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(Color(hex: "#F87171"))
            }

            PrimaryCta(
                label: busy ? "Saving…" : "Add Participant",
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
                role = theme.participantRoles.first ?? "Guest"
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
                roleCode: "PARTICIPANT",
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

import SwiftUI

/// Figma party 592:8580 / outing 592:7770 — moment-colored Experience Quick Add sheets.
struct ExperienceGapQuickAddSheet: View {
    let theme: ExperienceActiveTheme
    let kind: ExperienceQuickAddKind
    var momentId: String? = nil
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
        } else {
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
    }

    @ViewBuilder
    private var sheetBody: some View {
        switch kind {
        case .expense:
            WeddingExpenseBody(momentId: momentId, onDismiss: onClose, onSaved: onSaved, accent: accent)
        case .contribution:
            WeddingContributionBody(momentId: momentId, onDismiss: onClose, onSaved: onSaved, accent: accent)
        case .budget:
            WeddingBudgetBody(momentId: momentId, onDismiss: onClose, onSaved: onSaved, accent: accent)
        case .participant:
            ExperienceParticipantBody(theme: theme, accent: accent)
        case .vendor:
            WeddingVendorBody(momentId: momentId, onDismiss: onClose, onSaved: onSaved, accent: accent)
        case .planning:
            WeddingPlanningBody(momentId: momentId, onDismiss: onClose, onSaved: onSaved, accent: accent)
        case .attendance:
            WeddingAttendanceBody(momentId: momentId, onDismiss: onClose, onSaved: onSaved, accent: accent)
        case .poll:
            WeddingPollBody(momentId: momentId, onDismiss: onClose, onSaved: onSaved, accent: accent)
        case .memory:
            WeddingMemoryBody(momentId: momentId, onDismiss: onClose, onSaved: onSaved, accent: accent)
        case .update:
            WeddingUpdateBody(momentId: momentId, onDismiss: onClose, onSaved: onSaved, accent: accent)
        case .settle:
            WeddingSettleBody()
        case .booking:
            EmptyView()
        }
    }
}

private struct ExperienceParticipantBody: View {
    let theme: ExperienceActiveTheme
    var accent: SheetAccent

    @State private var name = ""
    @State private var contact = ""
    @State private var role = ""
    @State private var rsvp = "Pending"
    @State private var plusOne = false
    @State private var notes = ""

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

            PrimaryCta(label: "Add Participant", enabled: false, accent: accent, lightLabel: true) {}
        }
        .onAppear {
            if role.isEmpty {
                role = theme.participantRoles.first ?? "Guest"
            }
        }
    }
}

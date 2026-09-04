import SwiftUI
import UIKit

/// Figma 575:9761 — Group Experience long-form setup (mirrors Android).
struct GroupExperienceSetupView: View {
    @ObservedObject var createModel: MomentCreateModel
    var onBack: () -> Void
    var onCreated: (CreateMomentOutcome) -> Void
    var onSetupTypeChanged: (String) -> Void = { _ in }
    var editingMomentId: String? = nil
    var initialTitle: String? = nil
    var initialTypeCode: String? = nil

    private struct DraftPerson: Identifiable {
        let id = UUID()
        var name: String
        var roleCode: String
        var roleLabel: String
        var avatarName: String
        var isOrganizer: Bool
        var photo: UIImage? = nil
        var useInitials: Bool = false
        var contactEmail: String? = nil
        var contactPhone: String? = nil
    }

    private let types = GroupSetupCatalog.experienceTypes

    @State private var selectedCode = "TRIP"
    @State private var name = "Goa Trip"
    @State private var startDateIso: String?
    @State private var endDateIso: String?
    @State private var destination = ""
    @State private var primaryGoal = "Enjoy time together"
    @State private var budget = "₹80,000"
    @State private var budgetCustomAmount = ""
    @State private var currency = "INR"
    @State private var splitStyle = "Equal split"
    @State private var multiCurrency = "Enabled"
    @State private var paymentRhythm = "After each expense"
    @State private var joinApproval = "Admin approval"
    @State private var notifyChanges = true
    @State private var expenseReminders = "Enabled"
    @State private var photoReminders = "Enabled"
    @State private var updateCadence = "Every week"
    @State private var people: [DraftPerson] = []
    @State private var peopleEdited = false
    @State private var issuedInvite: GroupInvite?
    @State private var mintingInvite = false
    @State private var inviteError: String?

    private var selected: GroupTypeOption { types.first { $0.code == selectedCode } ?? types[0] }
    private var palette: GroupTypePalette { GroupSetupTheme.palette(for: selectedCode) }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 8) {
                headerRow
                GroupSetupHero(
                    title: "Set up Shared Experience",
                    subtitle: "Plan a trip, celebration, or shared adventure together. Everything can be refined later.",
                    accent: palette.accent,
                    iconName: selected.iconName ?? "ges_type_trip"
                )
                GroupLongFormTypeChipStrip(
                    title: "Experience setups",
                    types: types,
                    selectedCode: selectedCode,
                    shortLabel: experienceChipLabel,
                    onSelect: { opt in
                        selectedCode = opt.code
                        name = opt.defaultName
                        startDateIso = nil
                        endDateIso = nil
                        people = defaultPeople(for: opt.code)
                        peopleEdited = false
                    }
                )
                GroupLongFormDiamondDivider()
                section01ExperienceBasics
                GroupLongFormDiamondDivider()
                section02DatesBudgetSplit
                GroupLongFormDiamondDivider()
                section03PeopleNotifications
                GroupLongFormDiamondDivider()
                section04ExperienceSummary
                if let error = createModel.state.error {
                    Text(error).font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#EF4444"))
                }
            }
            .padding(.horizontal, 24)
            .padding(.top, 16)
            .padding(.bottom, 48)
        }
        .background(GroupSetupTheme.bg.ignoresSafeArea())
        .onAppear {
            if let initialTypeCode, types.contains(where: { $0.code == initialTypeCode }) {
                selectedCode = initialTypeCode
            }
            if let initialTitle, !initialTitle.isEmpty {
                name = initialTitle
            }
            if people.isEmpty { people = defaultPeople(for: selectedCode) }
            onSetupTypeChanged(selectedCode)
            if let editingMomentId {
                Task {
                    if let prefs = try? await APIClient.shared.getMomentNotificationPreferences(momentId: editingMomentId) {
                        await MainActor.run { notifyChanges = prefs.notifyOnChanges }
                    }
                }
            }
        }
        .onChange(of: selectedCode) { _, code in
            issuedInvite = nil
            inviteError = nil
            onSetupTypeChanged(code)
        }
    }

    private func ensureInvite() async -> GroupInvite? {
        if let issuedInvite { return issuedInvite }
        mintingInvite = true
        inviteError = nil
        let title = name.trimmingCharacters(in: .whitespacesAndNewlines)
        let minted = await createModel.mintGroupInvite(
            title: title.isEmpty ? selected.defaultName : title,
            momentTypeCode: selectedCode,
            section: "experience"
        )
        mintingInvite = false
        guard let minted else {
            inviteError = "Couldn’t create invite link. Try again."
            return nil
        }
        issuedInvite = minted
        return minted
    }

    private func shareQr() {
        Task {
            guard let invite = await ensureInvite() else { return }
            let url = GroupInviteLink.qrPayload(code: invite.inviteCode)
            guard let qr = GroupQRCode.image(from: url, size: 512) else {
                inviteError = "Couldn’t create QR code."
                return
            }
            InviteOutboundShare.presentSystemShare(items: [qr, url])
        }
    }

    private func shareWhatsApp() {
        Task {
            guard let invite = await ensureInvite() else { return }
            let url = GroupInviteLink.copyText(code: invite.inviteCode)
            let title = name.trimmingCharacters(in: .whitespacesAndNewlines)
            InviteOutboundShare.sendWhatsApp(
                phone: nil,
                message: InviteOutboundShare.inviteMessage(
                    title: title.isEmpty ? selected.defaultName : title,
                    url: url
                )
            )
        }
    }

    private var headerRow: some View {
        HStack {
            Button(action: onBack) {
                HStack(spacing: 6) {
                    Text("×").font(.plusJakarta(size: 16)).foregroundStyle(GroupSetupTheme.textSecondary)
                    Text("Close").font(.plusJakarta(size: 14)).foregroundStyle(GroupSetupTheme.textSecondary)
                }
            }
            .buttonStyle(.plain)
            Spacer()
            Text("GROUP MODE")
                .font(.plusJakarta(size: 12, weight: .semibold))
                .foregroundStyle(GroupSetupTheme.textSecondary)
        }
        .padding(.horizontal, 4)
        .padding(.vertical, 16)
    }

    private var section01ExperienceBasics: some View {
        GroupLongFormSectionCard(step: "01", title: "Experience Basics", accent: palette.accent) {
            SetupTitleField(
                label: selected.nameLabel,
                value: $name,
                placeholder: selected.defaultName
            )
            subsectionTitle("Your Experience")
            GroupLongFormPrefRow(
                label: "Experience type",
                hint: "What are you planning?",
                value: experienceChipLabel(selected),
                options: types.map(experienceChipLabel),
                onValueChange: { label in
                    let next = types.first { experienceChipLabel($0) == label } ?? selected
                    selectedCode = next.code
                    name = next.defaultName
                    startDateIso = nil
                    endDateIso = nil
                    people = defaultPeople(for: next.code)
                },
                testTag: "setup.dropdown.experienceType"
            )
            GroupLongFormPrefRow(
                label: "Primary goal",
                hint: "What brings everyone together?",
                value: primaryGoal,
                options: ["Enjoy time together", "Celebrate", "Explore", "Reconnect"],
                onValueChange: { primaryGoal = $0 },
                testTag: "setup.dropdown.primaryGoal"
            )
            subsectionTitle("Experience Details")
            GroupLongFormDestinationField(
                label: "Destination",
                hint: "Where you're going",
                value: $destination,
                placeholder: "e.g. Goa, India",
                testTag: "setup.field.destination"
            )
            SetupDateRangeField(
                label: "Dates",
                startIso: $startDateIso,
                endIso: $endDateIso,
                testTag: "setup.date.dates"
            )
        }
    }

    private var section02DatesBudgetSplit: some View {
        GroupLongFormSectionCard(step: "02", title: "Dates, Budget & Split", accent: palette.accent) {
            groupTitle("Plan the Practical Details")
            GroupLongFormDestinationField(
                label: "Destination",
                hint: "Where you're going",
                value: $destination,
                placeholder: "e.g. Goa, India",
                testTag: "setup.field.destinationPractical"
            )
            SetupDateRangeField(
                label: "Dates",
                startIso: $startDateIso,
                endIso: $endDateIso,
                testTag: "setup.date.dates"
            )
            Divider().overlay(GroupSetupTheme.border)
            groupTitle("Money")
            GroupLongFormPrefRow(
                label: "Budget",
                hint: "Expected total",
                value: budget,
                options: GroupBudgetUtils.presetOptions,
                onValueChange: { budget = $0 },
                editableGlyph: true,
                testTag: "setup.dropdown.budget"
            )
            if budget == GroupBudgetUtils.customOption {
                GroupBudgetCustomField(
                    value: $budgetCustomAmount,
                    currencyCode: currency
                )
            }
            GroupLongFormPrefRow(
                label: "Currency",
                hint: "Default currency",
                value: currency,
                options: ["INR", "USD", "EUR"],
                onValueChange: { currency = $0 },
                testTag: "setup.dropdown.currency"
            )
            GroupLongFormPrefRow(
                label: "Split style",
                hint: "How costs are shared",
                value: splitStyle,
                options: ["Equal split", "By share", "Host pays", "Custom"],
                onValueChange: { splitStyle = $0 },
                testTag: "setup.dropdown.splitStyle"
            )
            Divider().overlay(GroupSetupTheme.border)
            groupTitle("Planning Preferences")
            GroupLongFormPrefRow(
                label: "Multi-currency",
                hint: "Track expenses in other currencies",
                value: multiCurrency,
                options: ["Enabled", "Disabled"],
                onValueChange: { multiCurrency = $0 },
                testTag: "setup.dropdown.multiCurrency"
            )
            GroupLongFormPrefRow(
                label: "Payment rhythm",
                hint: "How to settle",
                value: paymentRhythm,
                options: ["After each expense", "Weekly", "End of trip"],
                onValueChange: { paymentRhythm = $0 },
                testTag: "setup.dropdown.paymentRhythm"
            )
            GroupLongFormLocalOnlyNote()
        }
    }

    private var section03PeopleNotifications: some View {
        GroupLongFormSectionCard(step: "03", title: "People & Notifications", accent: palette.accent) {
            groupTitle("Participants")
            peopleCard
            Divider().overlay(GroupSetupTheme.border)
            groupTitle("Invitations")
            GroupLongFormPrefRow(
                label: "Join approval",
                hint: "Who can enter",
                value: joinApproval,
                options: ["Admin approval", "Anyone with link", "Invite only"],
                onValueChange: { joinApproval = $0 },
                testTag: "setup.dropdown.joinApproval"
            )
            GroupLongFormToggleRow(
                title: "Notify me on changes",
                subtitle: "Get alerts when people join or edit",
                checked: $notifyChanges,
                accent: palette.accent
            )
            Divider().overlay(GroupSetupTheme.border)
            groupTitle("Group Preferences")
            GroupLongFormPrefRow(
                label: "Expense reminders",
                hint: "Keep the group on track",
                value: expenseReminders,
                options: ["Enabled", "Disabled"],
                onValueChange: { expenseReminders = $0 },
                testTag: "setup.dropdown.expenseReminders"
            )
            GroupLongFormPrefRow(
                label: "Photo reminders",
                hint: "Capture shared memories",
                value: photoReminders,
                options: ["Enabled", "Disabled"],
                onValueChange: { photoReminders = $0 },
                testTag: "setup.dropdown.photoReminders"
            )
            GroupLongFormPrefRow(
                label: "Update cadence",
                hint: "How often to check in",
                value: updateCadence,
                options: ["Every week", "Daily", "Only on changes"],
                onValueChange: { updateCadence = $0 },
                editableGlyph: true,
                testTag: "setup.dropdown.updateCadence"
            )
            GroupLongFormLocalOnlyNote()
        }
    }

    private var section04ExperienceSummary: some View {
        GroupLongFormSectionCard(step: "04", title: "Experience Summary", accent: palette.accent) {
            VStack(alignment: .leading, spacing: 10) {
                summaryLine("Experience", name)
                summaryLine("Dates", SetupDateTimeUtils.formatDateRangeDisplay(startIso: startDateIso, endIso: endDateIso))
                summaryLine("Budget", GroupBudgetUtils.summaryLabel(displayBudget: budget, customAmount: budgetCustomAmount))
                summaryLine("Members", buildMemberSummary(people))
                HStack {
                    Text(experienceChipLabel(selected).uppercased())
                        .font(.plusJakarta(size: 10, weight: .semibold))
                        .foregroundStyle(GroupSetupTheme.textSecondary)
                    Spacer()
                    Text("SUMMARY")
                        .font(.plusJakarta(size: 10, weight: .semibold))
                        .foregroundStyle(GroupSetupTheme.textSecondary)
                }
            }
            .padding(16)
            .background(GroupSetupTheme.card, in: RoundedRectangle(cornerRadius: 16))
            .overlay(RoundedRectangle(cornerRadius: 16).stroke(GroupSetupTheme.border, lineWidth: 1))
            Text("\(people.count) people · preferences on this device")
                .font(.plusJakarta(size: 12))
                .foregroundStyle(GroupSetupTheme.textSecondary)
            GroupLongFormReadyBanner(message: "Your shared experience is ready")
            Button(action: activate) {
                ZStack {
                    if createModel.state.submitting { ProgressView().tint(GroupSetupTheme.ctaText) }
                    else { Text("Activate Shared Experience →").font(.plusJakarta(size: 16, weight: .heavy)).foregroundStyle(GroupSetupTheme.ctaText) }
                }
                .frame(maxWidth: .infinity, minHeight: 56)
                .background(palette.accentGradient, in: RoundedRectangle(cornerRadius: 16))
            }
            .buttonStyle(.plain)
            .disabled(createModel.state.submitting)
            Text("Modify, extend or change anytime.")
                .font(.plusJakarta(size: 12))
                .foregroundStyle(GroupSetupTheme.textSecondary)
        }
    }

    private var peopleCard: some View {
        VStack(alignment: .leading, spacing: 0) {
            ForEach(Array(people.enumerated()), id: \.element.id) { index, person in
                if index > 0 { Divider().overlay(GroupSetupTheme.border).padding(.vertical, 8) }
                HStack(spacing: 12) {
                    GroupSheetAvatar(name: person.name, photo: person.photo, assetName: person.avatarName, useInitials: person.useInitials, size: 44)
                    VStack(alignment: .leading, spacing: 2) {
                        Text(person.name).font(.plusJakarta(size: 14, weight: .semibold)).foregroundStyle(GroupSetupTheme.textPrimary)
                        HStack(spacing: 6) {
                            Image(person.isOrganizer ? "ges_icon_role_organizer" : "ges_icon_role_member")
                                .renderingMode(.template)
                                .resizable()
                                .scaledToFit()
                                .frame(width: 14, height: 14)
                                .foregroundStyle(person.isOrganizer ? palette.accent : GroupSetupTheme.textSecondary)
                            Text(person.roleLabel).font(.plusJakarta(size: 11, weight: .semibold))
                        }
                        .foregroundStyle(person.isOrganizer ? palette.accent : GroupSetupTheme.textSecondary)
                        .padding(.horizontal, 8).padding(.vertical, 4)
                        .background((person.isOrganizer ? palette.accent : GroupSetupTheme.border).opacity(0.1), in: Capsule())
                        .overlay(Capsule().stroke((person.isOrganizer ? palette.accent : GroupSetupTheme.border).opacity(0.2), lineWidth: 1))
                    }
                    Spacer(minLength: 8)
                    if person.roleCode != "ORGANIZER" {
                        Button {
                            peopleEdited = true
                            people.removeAll { $0.id == person.id }
                        } label: {
                            ZStack {
                                Circle().fill(GroupSetupTheme.iconSurface).frame(width: 32, height: 32)
                                Image("ges_icon_x_circle")
                                    .renderingMode(.template)
                                    .resizable()
                                    .scaledToFit()
                                    .frame(width: 14, height: 14)
                                    .foregroundStyle(GroupSetupTheme.textSecondary)
                            }
                        }
                        .buttonStyle(.plain)
                        .accessibilityLabel("Remove \(person.name)")
                    }
                }
            }
            HStack(spacing: 10) {
                Button {
                    shareQr()
                } label: {
                    Group {
                        if mintingInvite {
                            ProgressView()
                                .tint(palette.accent)
                        } else {
                            HStack(spacing: 8) {
                                Image(systemName: "qrcode")
                                    .font(.system(size: 14, weight: .semibold))
                                Text("Share QR").font(.plusJakarta(size: 13, weight: .bold))
                            }
                        }
                    }
                    .foregroundStyle(mintingInvite ? palette.accent.opacity(0.35) : palette.accent)
                    .frame(maxWidth: .infinity, minHeight: 48)
                    .overlay(
                        RoundedRectangle(cornerRadius: 14)
                            .strokeBorder(
                                mintingInvite ? palette.accent.opacity(0.35) : palette.accent,
                                style: StrokeStyle(lineWidth: 1, dash: [8, 8])
                            )
                    )
                }
                .buttonStyle(.plain)
                .disabled(mintingInvite)

                Button {
                    shareWhatsApp()
                } label: {
                    Text("WhatsApp")
                        .font(.plusJakarta(size: 13, weight: .bold))
                        .foregroundStyle(mintingInvite ? palette.accent.opacity(0.35) : palette.accent)
                        .frame(maxWidth: .infinity, minHeight: 48)
                        .overlay(
                            RoundedRectangle(cornerRadius: 14)
                                .strokeBorder(
                                    mintingInvite ? palette.accent.opacity(0.35) : palette.accent,
                                    style: StrokeStyle(lineWidth: 1, dash: [8, 8])
                                )
                        )
                }
                .buttonStyle(.plain)
                .disabled(mintingInvite)
            }
            .padding(.top, 12)
            if let inviteError, !inviteError.isEmpty {
                Text(inviteError)
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(Color(hex: "#FF5961"))
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
        }
    }

    private func subsectionTitle(_ text: String) -> some View {
        Text(text.uppercased())
            .font(.plusJakarta(size: 10, weight: .semibold))
            .foregroundStyle(GroupSetupTheme.textSecondary)
            .frame(maxWidth: .infinity, alignment: .leading)
    }

    private func groupTitle(_ text: String) -> some View {
        Text(text)
            .font(.plusJakarta(size: 15, weight: .semibold))
            .foregroundStyle(Color(hex: "#E5E0EE"))
            .frame(maxWidth: .infinity, alignment: .leading)
    }

    private func summaryLine(_ label: String, _ value: String) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(label.uppercased())
                .font(.plusJakarta(size: 10, weight: .bold))
                .foregroundStyle(GroupSetupTheme.textSecondary)
            Text(value)
                .font(.plusJakarta(size: 14, weight: .semibold))
                .foregroundStyle(GroupSetupTheme.textPrimary)
        }
    }

    private func buildMemberSummary(_ people: [DraftPerson]) -> String {
        let names = people.map(\.name).joined(separator: ", ")
        return "\(names) (\(people.count) members)"
    }

    private func defaultPeople(for code: String) -> [DraftPerson] {
        [
            .init(
                name: "You",
                roleCode: "ORGANIZER",
                roleLabel: "Organizer",
                avatarName: "ges_avatar_1",
                isOrganizer: true,
                useInitials: true
            ),
        ]
    }

    private func activate() {
        guard !name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { return }
        let startAt = SetupDateTimeUtils.isoDateToStartInstant(startDateIso)
        let endAt = SetupDateTimeUtils.isoDateToEndInstant(endDateIso ?? startDateIso)
        let invitees = people.filter { $0.roleCode != "ORGANIZER" }.map {
            CreateMomentParticipantInput(
                displayName: $0.name,
                roleCode: $0.roleCode,
                email: $0.contactEmail,
                phone: $0.contactPhone
            )
        }
        let budgetAmount = GroupBudgetUtils.resolveBudgetAmount(displayBudget: budget, customAmount: budgetCustomAmount)
        let groupSetup = budgetAmount.map {
            CreateMomentRequest.GroupSetupBlock(
                budgetAmount: $0,
                budgetCurrencyCode: currency,
                destinationText: destination.isEmpty ? nil : destination
            )
        }
        createModel.submitGroupMoment(
            section: "experience",
            momentTypeCode: selectedCode,
            title: name.trimmingCharacters(in: .whitespacesAndNewlines),
            description: selected.defaultNotes.isEmpty ? nil : selected.defaultNotes,
            startAt: startAt,
            endAt: endAt,
            participants: invitees,
            inviteCode: issuedInvite?.inviteCode,
            groupSetup: groupSetup,
            editingMomentId: editingMomentId,
            onSuccess: { outcome in
                Task {
                    _ = try? await APIClient.shared.patchMomentNotificationPreferences(
                        momentId: outcome.momentId,
                        notifyOnChanges: notifyChanges
                    )
                    await MainActor.run { onCreated(outcome) }
                }
            }
        )
    }
}

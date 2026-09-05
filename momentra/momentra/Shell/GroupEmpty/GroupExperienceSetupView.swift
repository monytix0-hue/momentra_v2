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

    private struct ExperiencePlaceDraft: Identifiable {
        let id = UUID()
        var label: String = ""
        var startIso: String? = nil
        var endIso: String? = nil
    }

    private struct ExtraBudgetDraft: Identifiable {
        let id = UUID()
        var currencyCode: String = "USD"
        var amount: String = ""
    }

    private let types = GroupSetupCatalog.experienceTypes

    @State private var selectedCode = "TRIP"
    @State private var name = "Goa Trip"
    @State private var startDateIso: String?
    @State private var endDateIso: String?
    @State private var destination = ""
    @State private var places: [ExperiencePlaceDraft] = [ExperiencePlaceDraft()]
    @State private var primaryGoal = "Enjoy time together"
    @State private var budget = "₹80,000"
    @State private var budgetCustomAmount = ""
    @State private var currency = "INR"
    @State private var extraBudgets: [ExtraBudgetDraft] = []
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
    @State private var editingMomentStatus: String?
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
                        places = [ExperiencePlaceDraft()]
                        extraBudgets = []
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
                Task { await loadEditPrefill(momentId: editingMomentId) }
            }
        }
        .onChange(of: selectedCode) { _, code in
            issuedInvite = nil
            inviteError = nil
            onSetupTypeChanged(code)
        }
        .onChange(of: places) { _, _ in
            syncLegacyDestinationFields()
        }
    }

    private func syncLegacyDestinationFields() {
        destination = places.first?.label ?? ""
        startDateIso = places.first?.startIso
        endDateIso = places.last(where: { $0.endIso != nil })?.endIso ?? places.first?.endIso
    }

    private func loadEditPrefill(momentId: String) async {
        if let prefill = await createModel.getGroupSetupPrefill(momentId: momentId) {
            await MainActor.run {
                editingMomentStatus = prefill.status
                if let title = prefill.title?.trimmingCharacters(in: .whitespacesAndNewlines), !title.isEmpty {
                    name = title
                }
                if let goal = prefill.primaryGoal {
                    primaryGoal = goal
                }
                if let enabled = prefill.multiCurrencyEnabled {
                    multiCurrency = enabled ? "Enabled" : "Disabled"
                }
                if let code = prefill.splitStyle {
                    splitStyle = splitStyleLabel(fromApi: code)
                }
                let placeRows = prefill.places?.map {
                    ExperiencePlaceDraft(
                        label: $0.label ?? "",
                        startIso: $0.startAt.map { String($0.prefix(10)) },
                        endIso: $0.endAt.map { String($0.prefix(10)) }
                    )
                }.filter { !$0.label.isEmpty }
                if let placeRows, !placeRows.isEmpty {
                    places = placeRows
                } else if let legacy = prefill.destinationText, !legacy.isEmpty {
                    places = [
                        ExperiencePlaceDraft(
                            label: legacy,
                            startIso: prefill.startAt.map { String($0.prefix(10)) },
                            endIso: prefill.endAt.map { String($0.prefix(10)) }
                        ),
                    ]
                }
                applyBudgetPrefill(prefill.budgets)
            }
        }
        if let prefs = try? await APIClient.shared.getMomentNotificationPreferences(momentId: momentId) {
            await MainActor.run {
                notifyChanges = prefs.notifyOnChanges
                let rem = prefs.reminderPreferences ?? [:]
                if let v = rem["expenseReminders"] { expenseReminders = v ? "Enabled" : "Disabled" }
                if let v = rem["photoReminders"] { photoReminders = v ? "Enabled" : "Disabled" }
            }
        }
        let hasPlaceLabels = await MainActor.run { places.contains { !$0.label.isEmpty } }
        if editingMomentId != nil, !hasPlaceLabels {
            if let finance = try? await APIClient.shared.getGroupFinance(momentId: momentId).payload {
                let totals = finance.totals ?? []
                let total = totals.first(where: { ($0.budgetTotal.flatMap { Double($0) } ?? 0) > 0 }) ?? totals.first
                if let total, let raw = total.budgetTotal, (Double(raw) ?? 0) > 0 {
                    await MainActor.run {
                        applyFinanceBudgetPrefill(totals: totals, primaryTotal: total, raw: raw)
                    }
                }
            }
        }
    }

    private func applyBudgetPrefill(_ budgets: [GroupSetupBudgetPrefill]?) {
        guard let budgets, !budgets.isEmpty else { return }
        let primary = budgets.first(where: { $0.isPrimary == true }) ?? budgets.first
        guard let primary else { return }
        currency = primary.currencyCode
        let display = GroupBudgetUtils.formatApiAmountForDisplay(primary.amount, currencyCode: primary.currencyCode)
        if GroupBudgetUtils.presetOptions.contains(display) {
            budget = display
            budgetCustomAmount = ""
        } else {
            budget = GroupBudgetUtils.customOption
            budgetCustomAmount = GroupBudgetUtils.formatCustomAmountInput(primary.amount)
        }
        extraBudgets = budgets
            .filter { $0.currencyCode != primary.currencyCode }
            .map {
                ExtraBudgetDraft(
                    currencyCode: $0.currencyCode,
                    amount: GroupBudgetUtils.formatCustomAmountInput($0.amount)
                )
            }
    }

    private func applyFinanceBudgetPrefill(
        totals: [APIClient.GroupFinanceTotalsPayload],
        primaryTotal: APIClient.GroupFinanceTotalsPayload,
        raw: String
    ) {
        let display = GroupBudgetUtils.formatApiAmountForDisplay(raw, currencyCode: primaryTotal.currencyCode)
        currency = primaryTotal.currencyCode
        if GroupBudgetUtils.presetOptions.contains(display) {
            budget = display
            budgetCustomAmount = ""
        } else {
            budget = GroupBudgetUtils.customOption
            budgetCustomAmount = GroupBudgetUtils.formatCustomAmountInput(raw)
        }
        extraBudgets = totals
            .filter { $0.currencyCode != currency }
            .compactMap { row in
                guard let amt = row.budgetTotal, (Double(amt) ?? 0) > 0 else { return nil }
                return ExtraBudgetDraft(
                    currencyCode: row.currencyCode,
                    amount: GroupBudgetUtils.formatCustomAmountInput(amt)
                )
            }
    }

    private func splitStyleLabel(fromApi code: String) -> String {
        switch code.uppercased() {
        case "SHARES": return "By share"
        case "POOLED": return "Host pays"
        case "EXACT", "PERCENTAGE": return "Custom"
        default: return "Equal split"
        }
    }

    private func apiSplitStyle(from label: String) -> String {
        switch label {
        case "By share": return "SHARES"
        case "Host pays": return "POOLED"
        case "Custom": return "EXACT"
        default: return "EQUAL"
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
            inviteError = "Couldn't create invite link. Try again."
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
                inviteError = "Couldn't create QR code."
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
            Button(action: {
                if let editingMomentId,
                   editingMomentStatus?.caseInsensitiveCompare("DRAFT") == .orderedSame {
                    createModel.discardMomentDraft(momentId: editingMomentId, onSuccess: onBack)
                } else {
                    onBack()
                }
            }) {
                HStack(spacing: 6) {
                    Text("×").font(.plusJakarta(size: 16)).foregroundStyle(GroupSetupTheme.textSecondary)
                    Text("Discard draft").font(.plusJakarta(size: 14)).foregroundStyle(GroupSetupTheme.textSecondary)
                }
            }
            .buttonStyle(.plain)
            .accessibilityLabel("Discard draft")
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
                label: "Primary goal",
                hint: "What brings everyone together?",
                value: primaryGoal,
                options: ["Enjoy time together", "Celebrate", "Explore", "Reconnect"],
                onValueChange: { primaryGoal = $0 },
                testTag: "setup.dropdown.primaryGoal"
            )
            subsectionTitle("Experience Details")
            Text("Destinations (\(places.count) places)")
                .font(.plusJakarta(size: 12))
                .foregroundStyle(GroupSetupTheme.textSecondary)
            ForEach(Array(places.enumerated()), id: \.element.id) { index, _ in
                GroupLongFormDestinationField(
                    label: "Place \(index + 1)",
                    hint: "City or region",
                    value: Binding(
                        get: { places[index].label },
                        set: { places[index].label = $0 }
                    ),
                    placeholder: "e.g. Goa, India",
                    testTag: "setup.field.place_\(index)"
                )
                SetupDateRangeField(
                    label: "Dates",
                    startIso: Binding(
                        get: { places[index].startIso },
                        set: { places[index].startIso = $0 }
                    ),
                    endIso: Binding(
                        get: { places[index].endIso },
                        set: { places[index].endIso = $0 }
                    ),
                    testTag: "setup.date.placeDates_\(index)"
                )
            }
            Button {
                places.append(ExperiencePlaceDraft())
            } label: {
                Text("+ Add another place")
                    .font(.plusJakarta(size: 14, weight: .semibold))
                    .foregroundStyle(palette.accent)
                    .padding(.vertical, 8)
            }
            .buttonStyle(.plain)
        }
    }

    private var section02DatesBudgetSplit: some View {
        GroupLongFormSectionCard(step: "02", title: "Dates, Budget & Split", accent: palette.accent) {
            groupTitle("Plan the Practical Details")
            Text(places.first?.label.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty == false
                ? (places.first?.label ?? "")
                : "Add places in section 01")
                .font(.plusJakarta(size: 13))
                .foregroundStyle(GroupSetupTheme.textSecondary)
            Divider().overlay(GroupSetupTheme.border)
            groupTitle("Money")
            GroupLongFormPrefRow(
                label: "Primary budget",
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
                label: "Primary currency",
                hint: "Default currency",
                value: currency,
                options: GroupTravelCurrencyCatalog.codes,
                onValueChange: { currency = $0 },
                testTag: "setup.dropdown.currency"
            )
            if multiCurrency.lowercased() == "enabled" {
                ForEach(Array(extraBudgets.enumerated()), id: \.element.id) { index, _ in
                    GroupLongFormPrefRow(
                        label: "Currency \(index + 2)",
                        hint: GroupTravelCurrencyCatalog.display(extraBudgets[index].currencyCode),
                        value: extraBudgets[index].currencyCode,
                        options: GroupTravelCurrencyCatalog.codes.filter { $0 != currency },
                        onValueChange: { extraBudgets[index].currencyCode = $0 },
                        testTag: "setup.dropdown.extraCurrency_\(index)"
                    )
                    GroupBudgetCustomField(
                        value: Binding(
                            get: { extraBudgets[index].amount },
                            set: { extraBudgets[index].amount = $0 }
                        ),
                        currencyCode: extraBudgets[index].currencyCode
                    )
                }
                Button {
                    let next = GroupTravelCurrencyCatalog.codes.first { code in
                        code != currency && !extraBudgets.contains(where: { $0.currencyCode == code })
                    } ?? "USD"
                    extraBudgets.append(ExtraBudgetDraft(currencyCode: next))
                } label: {
                    Text("+ Add currency")
                        .font(.plusJakarta(size: 14, weight: .semibold))
                        .foregroundStyle(palette.accent)
                        .padding(.vertical, 8)
                }
                .buttonStyle(.plain)
            }
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
                options: ["After each expense", "Weekly", "End of trip", "Manual expense"],
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
            Button(action: { submitExperience(status: "ACTIVE") }) {
                ZStack {
                    if createModel.state.submitting { ProgressView().tint(GroupSetupTheme.ctaText) }
                    else { Text("Activate Shared Experience →").font(.plusJakarta(size: 16, weight: .heavy)).foregroundStyle(GroupSetupTheme.ctaText) }
                }
                .frame(maxWidth: .infinity, minHeight: 56)
                .background(palette.accentGradient, in: RoundedRectangle(cornerRadius: 16))
            }
            .buttonStyle(.plain)
            .disabled(createModel.state.submitting)
            .accessibilityIdentifier("group.setup.submit")
            HStack(spacing: 12) {
                Button(action: { submitExperience(status: "DRAFT") }) {
                    Text("Save draft")
                        .font(.plusJakarta(size: 14, weight: .semibold))
                        .foregroundStyle(GroupSetupTheme.textPrimary)
                        .frame(maxWidth: .infinity, minHeight: 48)
                        .overlay(
                            RoundedRectangle(cornerRadius: 14)
                                .stroke(GroupSetupTheme.border, lineWidth: 1)
                        )
                }
                .buttonStyle(.plain)
                .disabled(createModel.state.submitting)
                .accessibilityIdentifier("setup.field.saveDraft")

                Button(action: onBack) {
                    Text("Schedule later")
                        .font(.plusJakarta(size: 14, weight: .medium))
                        .foregroundStyle(GroupSetupTheme.textSecondary)
                        .frame(maxWidth: .infinity, minHeight: 48)
                        .overlay(
                            RoundedRectangle(cornerRadius: 14)
                                .stroke(GroupSetupTheme.border, lineWidth: 1)
                        )
                }
                .buttonStyle(.plain)
                .disabled(createModel.state.submitting)
            }
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
                            Text("Remove")
                                .font(.plusJakarta(size: 12, weight: .semibold))
                                .foregroundStyle(Color(hex: "#EF4444"))
                                .padding(.horizontal, 12)
                                .padding(.vertical, 8)
                                .background(GroupSetupTheme.iconSurface)
                                .clipShape(RoundedRectangle(cornerRadius: 16))
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

    private func buildGroupSetupBlock() -> CreateMomentRequest.GroupSetupBlock? {
        guard let primaryAmount = GroupBudgetUtils.resolveBudgetAmount(displayBudget: budget, customAmount: budgetCustomAmount) else {
            return nil
        }
        let placeBlocks = places
            .filter { !$0.label.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty }
            .map {
                CreateMomentRequest.GroupSetupBlock.PlaceBlock(
                    label: $0.label.trimmingCharacters(in: .whitespacesAndNewlines),
                    startAt: SetupDateTimeUtils.isoDateToStartInstant($0.startIso),
                    endAt: SetupDateTimeUtils.isoDateToEndInstant($0.endIso ?? $0.startIso)
                )
            }
        let resolvedPlaces: [CreateMomentRequest.GroupSetupBlock.PlaceBlock]? = {
            if !placeBlocks.isEmpty { return placeBlocks }
            guard !destination.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { return nil }
            return [
                CreateMomentRequest.GroupSetupBlock.PlaceBlock(
                    label: destination.trimmingCharacters(in: .whitespacesAndNewlines),
                    startAt: SetupDateTimeUtils.isoDateToStartInstant(startDateIso),
                    endAt: SetupDateTimeUtils.isoDateToEndInstant(endDateIso ?? startDateIso)
                ),
            ]
        }()
        var budgetBlocks: [CreateMomentRequest.GroupSetupBlock.BudgetBlock] = [
            .init(currencyCode: currency, amount: primaryAmount, isPrimary: true),
        ]
        if multiCurrency.lowercased() == "enabled" {
            for row in extraBudgets {
                let amt = row.amount.filter { $0.isNumber || $0 == "." }
                guard !amt.isEmpty, row.currencyCode.caseInsensitiveCompare(currency) != .orderedSame else { continue }
                budgetBlocks.append(.init(currencyCode: row.currencyCode, amount: amt, isPrimary: false))
            }
        }
        let reminderPreferences: [String: Bool] = [
            "expenseReminders": expenseReminders.lowercased() == "enabled",
            "photoReminders": photoReminders.lowercased() == "enabled",
        ]
        return CreateMomentRequest.GroupSetupBlock(
            budgetAmount: primaryAmount,
            budgetCurrencyCode: currency,
            destinationText: resolvedPlaces?.first?.label ?? destination.nilIfBlank,
            places: resolvedPlaces,
            budgets: budgetBlocks,
            multiCurrencyEnabled: multiCurrency.lowercased() == "enabled",
            splitStyle: apiSplitStyle(from: splitStyle),
            primaryGoal: primaryGoal,
            reminderPreferences: reminderPreferences,
            setupPreferences: [
                "paymentRhythm": JSONEncodableValue(paymentRhythm),
                "joinApproval": JSONEncodableValue(joinApproval),
                "updateCadence": JSONEncodableValue(updateCadence),
            ]
        )
    }

    private func submitExperience(status: String) {
        guard !name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { return }
        let startAt = SetupDateTimeUtils.isoDateToStartInstant(places.first?.startIso ?? startDateIso)
        let endAt = SetupDateTimeUtils.isoDateToEndInstant(
            places.compactMap { $0.endIso ?? $0.startIso }.last ?? endDateIso ?? startDateIso
        )
        let invitees = people.filter { $0.roleCode != "ORGANIZER" }.map {
            CreateMomentParticipantInput(
                displayName: $0.name,
                roleCode: $0.roleCode,
                email: $0.contactEmail,
                phone: $0.contactPhone
            )
        }
        let groupSetup = buildGroupSetupBlock()
        let reminderPreferences: [String: Bool] = [
            "expenseReminders": expenseReminders.lowercased() == "enabled",
            "photoReminders": photoReminders.lowercased() == "enabled",
        ]
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
            editingMomentStatus: editingMomentStatus,
            status: status,
            onSuccess: { outcome in
                Task {
                    _ = try? await APIClient.shared.patchMomentNotificationPreferences(
                        momentId: outcome.momentId,
                        notifyOnChanges: notifyChanges,
                        reminderPreferences: reminderPreferences
                    )
                    await MainActor.run {
                        editingMomentStatus = outcome.status
                        onCreated(outcome)
                    }
                }
            }
        )
    }
}

private extension String {
    var nilIfBlank: String? {
        let trimmed = trimmingCharacters(in: .whitespacesAndNewlines)
        return trimmed.isEmpty ? nil : trimmed
    }
}

import SwiftUI
import UIKit

/// Figma 575:10567 — Shared Living setup (4 variants).
struct GroupLivingSetupView: View {
    @ObservedObject var createModel: MomentCreateModel
    var onBack: () -> Void
    var onCreated: (CreateMomentOutcome) -> Void
    var onSetupTypeChanged: (String) -> Void = { _ in }
    var editingMomentId: String? = nil
    var initialTitle: String? = nil
    var initialTypeCode: String? = nil

    var body: some View {
        GroupSectionSetupView(
            variant: GroupSetupCatalog.living,
            createModel: createModel,
            onBack: onBack,
            onCreated: onCreated,
            onSetupTypeChanged: onSetupTypeChanged,
            editingMomentId: editingMomentId,
            initialTitle: initialTitle,
            initialTypeCode: initialTypeCode
        )
    }
}

/// Shared Purchase + Living setup flow with per-type accent palette (Figma 575:9919 / 575:10567).
struct GroupSectionSetupView: View {
    let variant: GroupSetupVariant
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

    @State private var selectedCode: String
    @State private var name: String
    @State private var tagline: String
    @State private var profile: String
    @State private var itemOrGoal: String
    @State private var ownership: String
    @State private var targetDateIso: String?
    @State private var amount: String
    @State private var amountCustom = ""
    @State private var currency: String
    @State private var ownershipSplit: String
    @State private var paymentPlan: String
    @State private var deadline: String
    @State private var multiCurrency: String
    @State private var approvalRule: String
    @State private var paymentReminders: String
    @State private var decisionCheckIn: String
    @State private var reviewCadence: String
    @State private var residents: String
    @State private var moveInDateIso: String?
    @State private var rentSplit: String
    @State private var choreStyle: String
    @State private var billRhythm: String
    @State private var houseRules: String
    @State private var quietHours: String
    @State private var guestPolicy: String
    @State private var joinApproval: String
    @State private var billReminders: String
    @State private var choreReminders: String
    @State private var houseReview: String
    @State private var people: [DraftPerson]
    @State private var peopleEdited = false
    @State private var issuedInvite: GroupInvite?
    @State private var mintingInvite = false
    @State private var inviteError: String?

    init(
        variant: GroupSetupVariant,
        createModel: MomentCreateModel,
        onBack: @escaping () -> Void,
        onCreated: @escaping (CreateMomentOutcome) -> Void,
        onSetupTypeChanged: @escaping (String) -> Void = { _ in },
        editingMomentId: String? = nil,
        initialTitle: String? = nil,
        initialTypeCode: String? = nil
    ) {
        self.variant = variant
        self.createModel = createModel
        self.onBack = onBack
        self.onCreated = onCreated
        self.onSetupTypeChanged = onSetupTypeChanged
        self.editingMomentId = editingMomentId
        self.initialTitle = initialTitle
        self.initialTypeCode = initialTypeCode
        let first = variant.types.first(where: { $0.code == initialTypeCode }) ?? variant.types.first!
        let isPurchase = variant.section == "purchase"
        _selectedCode = State(initialValue: first.code)
        _name = State(initialValue: (initialTitle?.isEmpty == false) ? initialTitle! : first.defaultName)
        _tagline = State(initialValue: isPurchase ? "Buying together" : "Living well together")
        _profile = State(initialValue: isPurchase ? "Shared purchase" : "Shared home")
        _itemOrGoal = State(initialValue: isPurchase ? "What everyone is funding" : "What matters most at home")
        _ownership = State(initialValue: "Shared equally")
        _targetDateIso = State(initialValue: nil)
        _amount = State(initialValue: "₹25,000")
        _currency = State(initialValue: "INR")
        _ownershipSplit = State(initialValue: "Equal")
        _paymentPlan = State(initialValue: "Monthly")
        _deadline = State(initialValue: "Before target date")
        _multiCurrency = State(initialValue: "Enabled")
        _approvalRule = State(initialValue: "Admin confirms")
        _paymentReminders = State(initialValue: "Enabled")
        _decisionCheckIn = State(initialValue: "On major changes")
        _reviewCadence = State(initialValue: "Every week")
        _residents = State(initialValue: "4 people")
        _moveInDateIso = State(initialValue: nil)
        _rentSplit = State(initialValue: "Equal")
        _choreStyle = State(initialValue: "Rotate weekly")
        _billRhythm = State(initialValue: "Monthly")
        _houseRules = State(initialValue: "Consensus")
        _quietHours = State(initialValue: "10pm–7am")
        _guestPolicy = State(initialValue: "Ask first")
        _joinApproval = State(initialValue: "Admin approval")
        _billReminders = State(initialValue: "Enabled")
        _choreReminders = State(initialValue: "Enabled")
        _houseReview = State(initialValue: "Every month")
        _people = State(initialValue: Self.defaultPeople(for: first.code))
    }

    private var selected: GroupTypeOption {
        variant.types.first { $0.code == selectedCode } ?? variant.types[0]
    }

    private var palette: GroupTypePalette {
        GroupSetupTheme.palette(for: selectedCode)
    }

    private var isPurchase: Bool { variant.section == "purchase" }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 8) {
                headerRow
                GroupSetupHero(
                    title: isPurchase ? "Set up Shared Purchase" : "Set up Shared Living",
                    subtitle: isPurchase ? "Pool funds, buy together, and stay aligned on contributions." : "Coordinate home life — bills, chores, and house rhythm — together.",
                    accent: palette.accent,
                    iconName: selected.iconName ?? "ges_type_trip"
                )
                GroupLongFormTypeChipStrip(
                    title: isPurchase ? "Purchase setups" : "Living setups",
                    types: variant.types,
                    selectedCode: selectedCode,
                    shortLabel: isPurchase ? purchaseChipLabel : livingChipLabel,
                    onSelect: { opt in
                        selectedCode = opt.code
                        name = opt.defaultName
                        people = Self.defaultPeople(for: opt.code)
                        peopleEdited = false
                        amountCustom = ""
                    }
                )
                GroupLongFormDiamondDivider()
                if isPurchase {
                    purchaseSection01
                    GroupLongFormDiamondDivider()
                    purchaseSection02
                    GroupLongFormDiamondDivider()
                    purchaseSection03
                } else {
                    livingSection01
                    GroupLongFormDiamondDivider()
                    livingSection02
                    GroupLongFormDiamondDivider()
                    livingSection03
                }
                GroupLongFormDiamondDivider()
                section04Summary
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
            onSetupTypeChanged(selectedCode)
        }
        .onChange(of: selectedCode) { _, code in
            issuedInvite = nil
            inviteError = nil
            amountCustom = ""
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
            section: variant.section
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

    // MARK: - Purchase sections

    private var purchaseSection01: some View {
        GroupLongFormSectionCard(step: "01", title: "Purchase Basics", accent: palette.accent) {
            SetupTitleField(
                label: selected.nameLabel,
                value: $name,
                placeholder: selected.defaultName
            )
            subsectionTitle("Your Purchase")
            GroupLongFormPrefRow(
                label: "Purchase profile",
                hint: "What are you buying?",
                value: profile,
                options: ["Shared purchase", "Gift pool", "Shared asset", "Custom"],
                onValueChange: { profile = $0 },
                testTag: "setup.dropdown.purchaseProfile"
            )
            GroupLongFormPrefRow(
                label: "Item or goal",
                hint: "What is the purchase?",
                value: itemOrGoal,
                options: ["What everyone is funding", "Camera kit", "Sofa set", "Trip fund"],
                onValueChange: { itemOrGoal = $0 },
                testTag: "setup.dropdown.itemOrGoal"
            )
            subsectionTitle("Purchase Details")
            GroupLongFormPrefRow(
                label: "Ownership",
                hint: "How ownership starts",
                value: ownership,
                options: ["Shared equally", "By contribution", "Named owner"],
                onValueChange: { ownership = $0 },
                testTag: "setup.dropdown.ownership"
            )
            SetupDateField(
                label: "Target date",
                isoValue: $targetDateIso,
                testTag: "setup.date.targetDate"
            )
        }
    }

    private var purchaseSection02: some View {
        GroupLongFormSectionCard(step: "02", title: "Goal, Amount & Contributions", accent: palette.accent) {
            groupTitle("Funding")
            GroupLongFormPrefRow(
                label: "Item or goal",
                hint: "What everyone is funding",
                value: itemOrGoal,
                options: ["What everyone is funding", "Camera kit", "Sofa set"],
                onValueChange: { itemOrGoal = $0 },
                editableGlyph: true,
                testTag: "setup.dropdown.itemOrGoal"
            )
            GroupLongFormPrefRow(
                label: "Expected amount",
                hint: "Estimated total",
                value: amount,
                options: GroupBudgetUtils.purchaseAmountOptions,
                onValueChange: { amount = $0 },
                editableGlyph: true,
                testTag: "setup.dropdown.expectedAmount"
            )
            if amount == GroupBudgetUtils.customOption {
                GroupBudgetCustomField(value: $amountCustom, currencyCode: currency)
            }
            groupTitle("Money")
            GroupLongFormPrefRow(
                label: "Currency",
                hint: "Default currency",
                value: currency,
                options: ["INR", "USD", "EUR"],
                onValueChange: { currency = $0 },
                testTag: "setup.dropdown.currency"
            )
            GroupLongFormPrefRow(
                label: "Ownership split",
                hint: "How ownership is divided",
                value: ownershipSplit,
                options: ["Equal", "By %", "Custom"],
                onValueChange: { ownershipSplit = $0 },
                testTag: "setup.dropdown.ownershipSplit"
            )
            GroupLongFormPrefRow(
                label: "Payment plan",
                hint: "How people contribute",
                value: paymentPlan,
                options: ["Monthly", "One-time", "Flexible"],
                onValueChange: { paymentPlan = $0 },
                testTag: "setup.dropdown.paymentPlan"
            )
            groupTitle("Planning Preferences")
            GroupLongFormPrefRow(
                label: "Deadline",
                hint: "When the goal should be met",
                value: deadline,
                options: ["Before target date", "Flexible", "Hard deadline"],
                onValueChange: { deadline = $0 },
                testTag: "setup.dropdown.deadline"
            )
            GroupLongFormPrefRow(
                label: "Multi-currency",
                hint: "Allow other currencies",
                value: multiCurrency,
                options: ["Enabled", "Disabled"],
                onValueChange: { multiCurrency = $0 },
                testTag: "setup.dropdown.multiCurrency"
            )
            GroupLongFormLocalOnlyNote()
        }
    }

    private var purchaseSection03: some View {
        GroupLongFormSectionCard(step: "03", title: "Members & Ownership", accent: palette.accent) {
            groupTitle("Members")
            peopleCard
            Divider().overlay(GroupSetupTheme.border)
            groupTitle("Ownership rules")
            GroupLongFormPrefRow(
                label: "Approval rule",
                hint: "Who confirms changes",
                value: approvalRule,
                options: ["Admin confirms", "Majority", "Anyone"],
                onValueChange: { approvalRule = $0 },
                testTag: "setup.dropdown.approvalRule"
            )
            GroupLongFormPrefRow(
                label: "Payment reminders",
                hint: "Keep contributions visible",
                value: paymentReminders,
                options: ["Enabled", "Disabled"],
                onValueChange: { paymentReminders = $0 },
                testTag: "setup.dropdown.paymentReminders"
            )
            GroupLongFormPrefRow(
                label: "Decision check-in",
                hint: "Confirm major changes",
                value: decisionCheckIn,
                options: ["On major changes", "Always", "Never"],
                onValueChange: { decisionCheckIn = $0 },
                testTag: "setup.dropdown.decisionCheckIn"
            )
            GroupLongFormPrefRow(
                label: "Review cadence",
                hint: "How often to review",
                value: reviewCadence,
                options: ["Every week", "Every month", "On demand"],
                onValueChange: { reviewCadence = $0 },
                editableGlyph: true,
                testTag: "setup.dropdown.reviewCadence"
            )
            GroupLongFormLocalOnlyNote()
        }
    }

    // MARK: - Living sections

    private var livingSection01: some View {
        GroupLongFormSectionCard(step: "01", title: "Home Basics", accent: palette.accent) {
            SetupTitleField(
                label: selected.nameLabel,
                value: $name,
                placeholder: selected.defaultName
            )
            subsectionTitle("Your Home")
            GroupLongFormPrefRow(
                label: "Living type",
                hint: "What kind of home is this?",
                value: livingChipLabel(selected),
                options: variant.types.map(livingChipLabel),
                onValueChange: { label in
                    let next = variant.types.first { livingChipLabel($0) == label } ?? selected
                    selectedCode = next.code
                    name = next.defaultName
                    people = Self.defaultPeople(for: next.code)
                },
                testTag: "setup.dropdown.livingType"
            )
            GroupLongFormPrefRow(
                label: "Primary goal",
                hint: "What matters most at home?",
                value: itemOrGoal,
                options: ["What matters most at home", "Fair chores", "Shared bills", "Peaceful living"],
                onValueChange: { itemOrGoal = $0 },
                testTag: "setup.dropdown.primaryGoal"
            )
            subsectionTitle("Home Details")
            GroupLongFormPrefRow(
                label: "Residents",
                hint: "Who lives here",
                value: residents,
                options: ["2 people", "3 people", "4 people", "5+"],
                onValueChange: { residents = $0 },
                testTag: "setup.dropdown.residents"
            )
            SetupDateField(
                label: "Move-in date",
                isoValue: $moveInDateIso,
                testTag: "setup.date.moveIn"
            )
        }
    }

    private var livingSection02: some View {
        GroupLongFormSectionCard(step: "02", title: "Budget, Responsibilities & Preferences", accent: palette.accent) {
            groupTitle("Money")
            GroupLongFormPrefRow(
                label: "Monthly budget",
                hint: "Shared household spending",
                value: amount,
                options: GroupBudgetUtils.livingBudgetOptions,
                onValueChange: { amount = $0 },
                editableGlyph: true,
                testTag: "setup.dropdown.monthlyBudget"
            )
            if amount == GroupBudgetUtils.customOption {
                GroupBudgetCustomField(value: $amountCustom, currencyCode: currency)
            }
            GroupLongFormPrefRow(
                label: "Rent split",
                hint: "How rent is divided",
                value: rentSplit,
                options: ["Equal", "By room", "Custom"],
                onValueChange: { rentSplit = $0 },
                testTag: "setup.dropdown.rentSplit"
            )
            GroupLongFormPrefRow(
                label: "Bill rhythm",
                hint: "When shared bills settle",
                value: billRhythm,
                options: ["Monthly", "Weekly", "As due"],
                onValueChange: { billRhythm = $0 },
                testTag: "setup.dropdown.billRhythm"
            )
            groupTitle("Responsibilities")
            GroupLongFormPrefRow(
                label: "Chore style",
                hint: "How tasks are shared",
                value: choreStyle,
                options: ["Rotate weekly", "Assigned", "Volunteer"],
                onValueChange: { choreStyle = $0 },
                testTag: "setup.dropdown.choreStyle"
            )
            GroupLongFormPrefRow(
                label: "House rules",
                hint: "How agreements are made",
                value: houseRules,
                options: ["Consensus", "Majority", "Host decides"],
                onValueChange: { houseRules = $0 },
                testTag: "setup.dropdown.houseRules"
            )
            GroupLongFormPrefRow(
                label: "Quiet hours",
                hint: "Protect rest and focus",
                value: quietHours,
                options: ["10pm–7am", "11pm–8am", "None"],
                onValueChange: { quietHours = $0 },
                testTag: "setup.dropdown.quietHours"
            )
            GroupLongFormPrefRow(
                label: "Guest policy",
                hint: "How visits are handled",
                value: guestPolicy,
                options: ["Ask first", "Anytime", "Weekends only"],
                onValueChange: { guestPolicy = $0 },
                testTag: "setup.dropdown.guestPolicy"
            )
            GroupLongFormLocalOnlyNote()
        }
    }

    private var livingSection03: some View {
        GroupLongFormSectionCard(step: "03", title: "Residents & Invitations", accent: palette.accent) {
            groupTitle("Residents")
            peopleCard
            Divider().overlay(GroupSetupTheme.border)
            groupTitle("Invitations")
            GroupLongFormPrefRow(
                label: "Join approval",
                hint: "Who can join the home",
                value: joinApproval,
                options: ["Admin approval", "Anyone with link", "Invite only"],
                onValueChange: { joinApproval = $0 },
                testTag: "setup.dropdown.joinApproval"
            )
            GroupLongFormPrefRow(
                label: "Bill reminders",
                hint: "Keep shared costs visible",
                value: billReminders,
                options: ["Enabled", "Disabled"],
                onValueChange: { billReminders = $0 },
                testTag: "setup.dropdown.billReminders"
            )
            GroupLongFormPrefRow(
                label: "Chore reminders",
                hint: "Keep responsibilities fair",
                value: choreReminders,
                options: ["Enabled", "Disabled"],
                onValueChange: { choreReminders = $0 },
                testTag: "setup.dropdown.choreReminders"
            )
            GroupLongFormPrefRow(
                label: "House review",
                hint: "How often to check in",
                value: houseReview,
                options: ["Every month", "Every week", "On demand"],
                onValueChange: { houseReview = $0 },
                editableGlyph: true,
                testTag: "setup.dropdown.houseReview"
            )
            GroupLongFormLocalOnlyNote()
        }
    }

    // MARK: - Common section 04

    private var section04Summary: some View {
        GroupLongFormSectionCard(step: "04", title: isPurchase ? "Purchase Summary" : "Living Summary", accent: palette.accent) {
            VStack(alignment: .leading, spacing: 10) {
                summaryLine(isPurchase ? "Purchase" : "Home", name)
                summaryLine(isPurchase ? "Amount" : "Budget", GroupBudgetUtils.summaryLabel(displayBudget: amount, customAmount: amountCustom))
                summaryLine("Members", buildMemberSummary(people))
            }
            .padding(16)
            .background(GroupSetupTheme.card, in: RoundedRectangle(cornerRadius: 16))
            .overlay(RoundedRectangle(cornerRadius: 16).stroke(GroupSetupTheme.border, lineWidth: 1))
            GroupLongFormReadyBanner(message: isPurchase ? "Your shared purchase is ready" : "Your shared living space is ready")
            Button(action: activate) {
                ZStack {
                    if createModel.state.submitting { ProgressView().tint(GroupSetupTheme.ctaText) }
                    else { Text("\(variant.activateLabel) →").font(.plusJakarta(size: 16, weight: .heavy)).foregroundStyle(GroupSetupTheme.ctaText) }
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

    private static func defaultPeople(for code: String) -> [DraftPerson] {
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
        let trimmed = name.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }
        createModel.submitGroupMoment(
            section: variant.section,
            momentTypeCode: selected.code,
            title: trimmed,
            description: selected.defaultNotes.isEmpty ? nil : selected.defaultNotes,
            startAt: nil,
            endAt: nil,
            participants: people
                .filter { $0.roleCode != "ORGANIZER" }
                .map {
                    CreateMomentParticipantInput(
                        displayName: $0.name,
                        roleCode: $0.roleCode,
                        email: $0.contactEmail,
                        phone: $0.contactPhone
                    )
                },
            inviteCode: issuedInvite?.inviteCode,
            editingMomentId: editingMomentId,
            onSuccess: { outcome in
                Task {
                    _ = try? await APIClient.shared.patchMomentNotificationPreferences(
                        momentId: outcome.momentId,
                        reminderPreferences: [
                            "billReminders": billReminders.lowercased() == "enabled",
                            "choreReminders": choreReminders.lowercased() == "enabled",
                            "paymentReminders": paymentReminders.lowercased() == "enabled",
                        ]
                    )
                    await MainActor.run { onCreated(outcome) }
                }
            }
        )
    }
}

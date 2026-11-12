import Combine
import SwiftUI

/// `TRD-SETUP-v6` / `DST-SETUP-v6` — one scrolling document with six sections.
///
/// The document owns the draft it writes into: opening it creates a `DRAFT` moment so §04's
/// five rows can open the live masters sheet scoped to that moment, and §06 promotes the same
/// moment to `ACTIVE`. Every field edit is saved to the draft on a short debounce, so activation
/// only has to flip the status.
///
/// Both verticals render this one view. The kind chip re-themes the whole document, exactly as the
/// two Figma frames differ only in accent and copy.
struct BusinessSetupWizardView: View {
    let kind: BusinessSetupKind
    @ObservedObject var createModel: MomentCreateModel
    var companyId: String?
    var ownerName: String = ""
    var editingMomentId: String?
    var initialTitle: String?
    var onClose: () -> Void
    var onCreated: (CreateMomentOutcome) -> Void

    @State private var doc: BusinessSetupDocState
    @State private var draftMomentId: String?
    @State private var draftCompanyId: String?
    /// The status of the moment this document is writing into: `DRAFT` for a setup that has not
    /// started, `ACTIVE` when the same document is reopened to edit a live moment.
    @State private var momentStatus: String
    @State private var starting = false
    @State private var started = false
    @State private var closing = false
    /// Close can beat the draft POST. The moment is still created server-side, so the document
    /// remembers it was abandoned and discards the late arrival instead of orphaning a draft.
    @State private var abandoned = false
    @State private var counts = BusinessSetupMasterCounts()
    @State private var warehouseNames: [String] = []
    @State private var reloadToken = 0
    @State private var route: SetupRoute?
    @State private var saveTask: Task<Void, Never>?

    /// §02 mints a real invite in-setup. It is company-scoped, so the lazily created setup company
    /// is the first thing an invite touch needs; the code then rides the sheet and the card.
    @State private var setupInvite: CompanyInvite?
    @State private var mintingInvite = false
    @State private var inviteError: String?

    private let repo: BusinessOperatingGateway

    init(
        kind: BusinessSetupKind,
        createModel: MomentCreateModel,
        companyId: String? = nil,
        ownerName: String = "",
        editingMomentId: String? = nil,
        initialTitle: String? = nil,
        onClose: @escaping () -> Void,
        onCreated: @escaping (CreateMomentOutcome) -> Void,
        repo: BusinessOperatingGateway = BusinessOperatingRepository()
    ) {
        self.kind = kind
        self.createModel = createModel
        self.companyId = companyId
        self.ownerName = ownerName
        self.editingMomentId = editingMomentId
        self.initialTitle = initialTitle
        self.onClose = onClose
        self.onCreated = onCreated
        self.repo = repo
        _doc = State(initialValue: BusinessSetupDocState(
            vertical: kind.vertical ?? .trading,
            initialName: initialTitle ?? ""
        ))
        _draftMomentId = State(initialValue: editingMomentId)
        _draftCompanyId = State(initialValue: companyId.flatMap { $0.isEmpty ? nil : $0 })
        // A new document is a draft; reopening an existing moment edits it in place, and only a
        // draft can be discarded or promoted.
        _momentStatus = State(initialValue: editingMomentId == nil ? "DRAFT" : "ACTIVE")
    }

    /// The document's own palette. Every card, field, chip and pill in the six sections reads
    /// from this, never from `OperatingTheme` — that is what the Android `SetupDocumentKit` does,
    /// and what makes the two platforms render the same document.
    private var docTheme: BusinessSetupDocTheme {
        BusinessSetupDocTheme.theme(for: doc.vertical)
    }

    /// Only the sheets and the masters view are operating surfaces; the document itself does not
    /// use this.
    private var theme: OperatingTheme {
        OperatingTheme(vertical: doc.vertical)
    }

    var body: some View {
        ZStack(alignment: .bottom) {
            docTheme.canvas.ignoresSafeArea()
            ScrollView {
                VStack(alignment: .leading, spacing: SetupDocSectionGap.value) {
                    header
                    kindCard
                    ForEach(BusinessSetupDocSpec.sections(doc.vertical)) { section in
                        sectionView(section)
                    }
                    if let error = createModel.state.error, !error.isEmpty {
                        Text(error)
                            .font(.plusJakarta(size: 13))
                            .foregroundStyle(docTheme.negative)
                            .frame(maxWidth: .infinity, alignment: .leading)
                    }
                    Spacer(minLength: 28)
                }
                .padding(.horizontal, 17)
                .padding(.vertical, 16)
            }
            .scrollDismissesKeyboard(.interactively)
        }
        .task { await openDraft() }
        .onChange(of: doc) { _, _ in scheduleDraftSave() }
        .onChange(of: createModel.state) { _, newState in
            // A rejected discard or activation re-enables the affordance that started it, so the
            // operator can retry instead of facing a permanently disabled button.
            guard !newState.submitting else { return }
            if starting, !started { starting = false }
            if closing { closing = false }
        }
        .task(id: MasterReloadKey(companyId: draftCompanyId, momentId: draftMomentId, token: reloadToken)) {
            await loadCounts()
        }
        .sheet(item: $route) { route in
            switch route {
            case let .masters(tab, openCreate):
                mastersSheet(tab, openCreateForm: openCreate)
            case .invite:
                inviteSheet
            case let .picker(kind):
                setupPicker(kind)
                    .presentationDetents([.medium])
                    .presentationDragIndicator(.visible)
                    .presentationCornerRadius(24)
                    .preferredColorScheme(.dark)
            }
        }
        .interactiveDismissDisabled(starting)
    }

    // MARK: - Header

    private var header: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Button(action: close) {
                    Text("‹  Close")
                        .font(.plusJakarta(size: 14))
                        .foregroundStyle(docTheme.muted)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 6)
                        .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                .disabled(closing)
                Spacer(minLength: 4)
                Text(BusinessSetupDocSpec.kindLabel(doc.vertical))
                    .font(.plusJakarta(size: 11, weight: .semibold))
                    .foregroundStyle(docTheme.accent)
            }

            ZStack {
                if doc.vertical.isMarketing {
                    Circle()
                        .fill(
                            RadialGradient(
                                colors: [
                                    BusinessSetupDocTheme.emblemGlow.opacity(0.40),
                                    BusinessSetupDocTheme.emblemGlow.opacity(0.12),
                                    .clear,
                                ],
                                center: .center,
                                startRadius: 8,
                                endRadius: 60
                            )
                        )
                        .frame(width: 120, height: 120)
                }
                Circle()
                    .fill(docTheme.card)
                    .frame(width: 96, height: 96)
                    .shadow(
                        color: doc.vertical.isMarketing
                            ? BusinessSetupDocTheme.emblemGlow.opacity(0.40)
                            : .clear,
                        radius: 20
                    )
                Circle()
                    .stroke(docTheme.border, lineWidth: 1)
                    .frame(width: 96, height: 96)
                if doc.vertical.isMarketing {
                    Image("MarketingSetupMega")
                        .resizable()
                        .scaledToFit()
                        .frame(width: 40, height: 40)
                } else {
                    Text("◈")
                        .font(.plusJakarta(size: 44, weight: .semibold))
                        .foregroundStyle(docTheme.ink)
                }
            }
            .frame(width: 120, height: 120)
            .frame(maxWidth: .infinity)

            Text("\(displayName) Setup")
                .font(.plusJakarta(size: 32, weight: .semibold))
                .foregroundStyle(docTheme.ink)
                .fixedSize(horizontal: false, vertical: true)

            Text("Bring your business, people, and monthly plan together before you start.")
                .font(.plusJakarta(size: 14))
                .foregroundStyle(docTheme.muted)
                .fixedSize(horizontal: false, vertical: true)

            HStack {
                Spacer(minLength: 0)
                Text(BusinessSetupDocSpec.statusPillLabel(status: momentStatus))
                    .font(.plusJakarta(size: 11, weight: .semibold))
                    .foregroundStyle(docTheme.accent)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 6)
                    .background(docTheme.card)
                    .clipShape(Capsule())
                Spacer(minLength: 0)
            }
        }
    }

    private var displayName: String {
        let name = doc.businessName.trimmingCharacters(in: .whitespacesAndNewlines)
        return name.isEmpty ? "New business" : name
    }

    // MARK: - Kind switcher

    /// §00 kind tiles — Trading↔Distribution or Client↔Own for marketing.
    private var kindCard: some View {
        let options: [BusinessOperatingVertical] = {
            switch kind {
            case .marketing: return [.clientCampaign, .ownBusinessCampaign]
            case .manufacturing: return [.standardProduction, .madeToOrder]
            case .retail: return [.retailStore, .restaurantFood, .consumerService]
            default: return [.distribution, .trading]
            }
        }()
        return VStack(alignment: .leading, spacing: 10) {
            Text(kind == .marketing ? "Kind of campaign" : "Kind of business")
                .font(.plusJakarta(size: 11, weight: .semibold))
                .foregroundStyle(docTheme.muted)
            HStack(spacing: 10) {
                ForEach(options, id: \.rawValue) { vertical in
                    let on = doc.vertical == vertical
                    Button {
                        doc.switchVertical(vertical)
                    } label: {
                        VStack(spacing: 4) {
                            Text("◈")
                                .font(.plusJakarta(size: 20, weight: .semibold))
                                .foregroundStyle(on ? docTheme.ink : docTheme.muted)
                            Text(BusinessSetupDocSpec.kindLabel(vertical))
                                .font(.plusJakarta(size: 11, weight: .semibold))
                                .foregroundStyle(on ? docTheme.ink : docTheme.muted)
                        }
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 10)
                        .background(docTheme.card)
                        .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                        .overlay(
                            RoundedRectangle(cornerRadius: 10, style: .continuous)
                                .stroke(on ? docTheme.accent : docTheme.border, lineWidth: 1)
                        )
                    }
                    .buttonStyle(.plain)
                    .accessibilityIdentifier("setup.kind.\(vertical.storyNoun)")
                }
            }
            Text(
                kind == .marketing
                    ? "4 sections + GST & Review"
                    : "6 steps · \(BusinessSetupDocSpec.kindLabel(doc.vertical)) setup"
            )
                .font(.plusJakarta(size: 12))
                .foregroundStyle(docTheme.muted)
        }
        .padding(12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(docTheme.card)
        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .stroke(docTheme.border, lineWidth: 1)
        )
    }

    // MARK: - Sections

    @ViewBuilder
    private func sectionView(_ section: BusinessSetupSectionSpec) -> some View {
        SetupSectionCard(spec: section, theme: docTheme) {
            switch section.number {
            case "01": basicsSection
            case "02": peopleSection
            case "03":
                // Marketing stacks network before the monthly plan; trading keeps plan then masters.
                if doc.vertical.isMarketing { mastersSection } else { planSection }
            case "04":
                if doc.vertical.isMarketing { planSection } else { mastersSection }
            case "05": gstSection
            default: reviewSection
            }
        }
        .accessibilityIdentifier("setup.section.\(section.number)")
    }

    // MARK: §01

    private var basicsSection: some View {
        VStack(alignment: .leading, spacing: 14) {
            SetupPanelCard(title: "Identity", theme: docTheme) {
                if doc.vertical.isMarketing {
                    field(
                        "Campaign name",
                        text: $doc.businessName,
                        placeholder: doc.vertical == .ownBusinessCampaign ? "Spring launch" : "Brightline Q2"
                    )
                    field(
                        doc.vertical == .ownBusinessCampaign ? "Brand / business" : "Client",
                        text: $doc.location,
                        placeholder: doc.vertical == .ownBusinessCampaign ? "Your brand" : "Client name"
                    )
                    if doc.vertical == .ownBusinessCampaign {
                        field("What you promote", text: $doc.offering, placeholder: "Product, offer, or message")
                    } else {
                        field("Client fee", text: $doc.clientFee, hint: "Optional retainer / fee — not an invoice", placeholder: "₹1,50,000")
                        field("Client-approved media budget", text: $doc.clientApprovedMediaBudget, hint: "Separate from agency operating cost", placeholder: "₹5,00,000")
                        field("Agency operating budget", text: $doc.agencyOperatingBudget, hint: "Agency cost bucket", placeholder: "₹1,00,000")
                    }
                    field("Campaign purpose", text: $doc.campaignPurpose, placeholder: doc.vertical == .ownBusinessCampaign ? "Brand awareness / Leads / Sales" : "Awareness / Leads / Launch")
                    Toggle(isOn: $doc.creativeApprovalRequired) {
                        Text("Creative approval required")
                            .font(.plusJakarta(size: 13, weight: .semibold))
                            .foregroundStyle(docTheme.ink)
                    }
                    .tint(docTheme.accent)
                    if doc.vertical == .clientCampaign {
                        VStack(alignment: .leading, spacing: 8) {
                            Text("Reporting cadence")
                                .font(.plusJakarta(size: 12, weight: .semibold))
                                .foregroundStyle(docTheme.muted)
                            HStack(spacing: 8) {
                                ForEach(["WEEKLY", "MONTHLY", "CUSTOM"], id: \.self) { code in
                                    let on = doc.reportingCadence == code
                                    Button {
                                        doc.reportingCadence = on ? "" : code
                                    } label: {
                                        Text(code.capitalized)
                                            .font(.plusJakarta(size: 12, weight: .semibold))
                                            .foregroundStyle(on ? docTheme.ink : docTheme.muted)
                                            .padding(.horizontal, 10)
                                            .padding(.vertical, 7)
                                            .background(on ? docTheme.accent.opacity(0.18) : docTheme.inset)
                                            .clipShape(Capsule())
                                            .overlay(Capsule().stroke(on ? docTheme.accent : docTheme.border, lineWidth: 1))
                                    }
                                    .buttonStyle(.plain)
                                }
                            }
                        }
                    }
                } else {
                    field("Business name", text: $doc.businessName, placeholder: "Northstar Trading")
                    field("Company name", text: $doc.companyName, hint: "Registered company name", placeholder: displayName)
                    field(
                        "GSTIN",
                        text: Binding(
                            get: { doc.gstin },
                            set: { doc.gstin = $0.uppercased() }
                        ),
                        hint: IndiaStateCatalog.gstinStateHint(gstin: doc.gstin, state: doc.gstState)
                            ?? "15-digit GSTIN, if you have one",
                        placeholder: "29ABCDE4321T1Z5"
                    )
                }
                pickerRow(
                    "Currency",
                    value: doc.currency,
                    tag: "setup.field.currency"
                ) { sheetVerticalPicker() }
                HStack(alignment: .top, spacing: 10) {
                    dateSlot(
                        label: "Start date",
                        value: doc.startDate,
                        placeholder: doc.vertical.isMarketing ? "Required" : "Today",
                        tag: "setup.field.startDate",
                        onPick: { doc.startDate = $0 }
                    )
                    dateSlot(
                        label: "End date",
                        value: doc.endDate,
                        placeholder: "Still open",
                        tag: "setup.field.endDate",
                        onPick: { doc.endDate = $0 }
                    )
                }
            }
            if !doc.vertical.isMarketing {
                SetupPanelCard(title: "Dates & more", theme: docTheme) {
                    field(
                        "Description",
                        text: $doc.description,
                        placeholder: "Everyday notes about this business",
                        axis: .vertical
                    )
                    field("Location", text: $doc.location, placeholder: "Add your DC location")
                    footnote("You can fill in the extra details later.", size: 11)
                }
            }
        }
    }

    // MARK: §02

    private var peopleSection: some View {
        let owner = ownerName.trimmingCharacters(in: .whitespacesAndNewlines)
        let ownerDisplay = owner.isEmpty ? "You" : owner
        return VStack(alignment: .leading, spacing: 14) {
            // One card with the owner first and every invitee under it — the group setup people
            // card keeps one container and separates its rows with a hairline, so the team reads
            // as one list rather than a person plus a detached block of names.
            VStack(spacing: 10) {
                HStack(spacing: 10) {
                    SetupDocAvatar(initials: BusinessSetupDocSpec.initials(ownerDisplay), theme: docTheme, size: 44, tone: docTheme.accent)
                    VStack(alignment: .leading, spacing: 2) {
                        Text("\(ownerDisplay) · You")
                            .font(.plusJakarta(size: 14, weight: .bold))
                            .foregroundStyle(docTheme.ink)
                            .lineLimit(1)
                        Text("Owner · \(displayName)")
                            .font(.plusJakarta(size: 12))
                            .foregroundStyle(docTheme.muted)
                            .lineLimit(1)
                    }
                    Spacer(minLength: 4)
                    Text("You are owner")
                        .font(.plusJakarta(size: 10, weight: .bold))
                        .foregroundStyle(docTheme.accent)
                        .lineLimit(1)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 6)
                        .background(docTheme.accent.opacity(0.13))
                        .clipShape(Capsule())
                        .overlay(Capsule().stroke(docTheme.accent, lineWidth: 1))
                }

                // Everyone already invited, as live rows rather than a count — a name is what
                // makes a pending invite believable, exactly as the group card lists its people.
                ForEach(doc.invitees) { invitee in
                    Divider().overlay(docTheme.border)
                    inviteeRow(invitee)
                }
            }
            .padding(12)
            .frame(maxWidth: .infinity, alignment: .leading)
            .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .stroke(docTheme.border, lineWidth: 1)
            )

            SetupPanelCard(title: "Roles on invite", theme: docTheme) {
                chipRow(
                    doc.roles.map(\.label),
                    selected: [doc.inviteRoleLabel]
                ) { doc.inviteRole = BusinessSetupDocSpec.roleCode($0) }
                HStack(alignment: .top) {
                    footnote(BusinessSetupDocSpec.inviteAccessNote(doc.inviteRole), size: 11)
                    Spacer(minLength: 8)
                    // The group card shows the count beside the roles rather than in a second
                    // panel — a duplicate team panel would list the same people twice.
                    Text(doc.invitees.isEmpty ? "No invites yet" : "\(doc.invitees.count) pending")
                        .font(.plusJakarta(size: 11))
                        .foregroundStyle(docTheme.muted)
                        .lineLimit(1)
                }
            }

            HStack(spacing: 10) {
                // Enabled before the first mint on purpose: a share mints on demand, so it never
                // depends on the sheet having been opened first.
                SetupDocOutlineAction(title: "Share QR", theme: docTheme, busy: mintingInvite) { shareInviteQr() }
                SetupDocOutlineAction(title: "WhatsApp", theme: docTheme, busy: mintingInvite) { shareInviteWhatsApp() }
            }

            if let inviteError, !inviteError.isEmpty {
                Text(inviteError)
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(docTheme.negative)
                    .fixedSize(horizontal: false, vertical: true)
            }

            Button {
                openSetupInvite()
            } label: {
                Text("+ Invite people · \(doc.inviteRoleLabel)")
                    .font(.plusJakarta(size: 14, weight: .bold))
                    .foregroundStyle(docTheme.ink)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 14)
                    .overlay(
                        RoundedRectangle(cornerRadius: 14, style: .continuous)
                            .stroke(docTheme.accent.opacity(0.45), lineWidth: 1)
                    )
            }
            .buttonStyle(.plain)
            .accessibilityIdentifier("setup.invite")

            why(
                BusinessSetupDocSpec.approvalWhyTitle,
                BusinessSetupDocSpec.approvalWhyBody(owner: ownerDisplay, name: displayName)
            )
        }
    }

    /// One invited person: initials, name, and the role the pending invite grants.
    private func inviteeRow(_ invitee: BusinessSetupInvitee) -> some View {
        HStack(spacing: 10) {
            SetupDocAvatar(initials: BusinessSetupDocSpec.initials(invitee.name), theme: docTheme, size: 40)
            VStack(alignment: .leading, spacing: 2) {
                Text(invitee.name)
                    .font(.plusJakarta(size: 14, weight: .bold))
                    .foregroundStyle(docTheme.ink)
                    .lineLimit(1)
                Text(BusinessSetupDocSpec.teamMeta(status: "PENDING", role: invitee.roleLabel))
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(docTheme.accent)
                    .lineLimit(1)
            }
            Spacer(minLength: 4)
            Button {
                doc.invitees.removeAll { $0.id == invitee.id }
            } label: {
                Text("Remove")
                    .font(.plusJakarta(size: 12, weight: .semibold))
                    .foregroundStyle(docTheme.muted)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 6)
                    .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    // MARK: §03

    private var planSection: some View {
        let budgetOk = (Decimal(string: BusinessSetupDocSpec.plainAmount(doc.budget)) ?? 0) > 0
        let targetOk = (Decimal(string: BusinessSetupDocSpec.plainAmount(doc.target)) ?? 0) > 0
        return VStack(alignment: .leading, spacing: 14) {
            SetupPanelCard(title: "Channels", theme: docTheme) {
                chipRow(doc.channelChoices, selected: doc.channels) { doc.toggleChannel($0) }
                footnote(BusinessSetupDocSpec.channelHint(selected: doc.channels.count), size: 11)
            }
            pickerRow("Month", value: BusinessSetupDocSpec.monthLabel(doc.month), tag: "setup.field.month") {
                sheetMonthPicker()
            }
            field(
                "Monthly budget",
                text: Binding(
                    get: { doc.budget },
                    set: { doc.budget = BusinessSetupDocSpec.formatIndianAmount($0) }
                ),
                hint: "Amount",
                placeholder: "₹60,00,000",
                keyboard: .decimalPad
            )
            field(
                "Monthly sales target",
                text: Binding(
                    get: { doc.target },
                    set: { doc.target = BusinessSetupDocSpec.formatIndianAmount($0) }
                ),
                hint: "Amount",
                placeholder: "₹90,00,000",
                keyboard: .decimalPad
            )
            chipRow(
                BusinessSetupDocSpec.metricLabels(for: doc.vertical),
                selected: [doc.metric],
                label: "How the target is measured"
            ) { doc.metric = $0 }
            if doc.vertical.isMarketing {
                footnote(
                    "Unit · \(BusinessSetupDocSpec.marketingMetricUnit(BusinessSetupDocSpec.metricCode(doc.metric, vertical: doc.vertical)))",
                    size: 11
                )
            }
            why(BusinessSetupDocSpec.targetWhyTitle, BusinessSetupDocSpec.targetWhyBody)
            statusPill(
                budgetOk && targetOk ? "Budget and sales target look good" : "Add a budget and a sales target",
                good: budgetOk && targetOk
            )
        }
    }

    // MARK: §04

    private var mastersSection: some View {
        VStack(alignment: .leading, spacing: 14) {
            if doc.vertical.isMarketing {
                // BS-06 — Campaign Resources & Providers (no warehouses / separate price lists).
                masterRow(
                    glyph: "◇",
                    title: "Campaign Resources & Providers",
                    subtitle: counts.skus + counts.principals + counts.dealers == 0
                        ? "Register resources, providers, prices and tax in one place"
                        : "\(counts.skus) resources · \(counts.principals + counts.dealers) providers",
                    tag: "setup.master.campaignResources"
                ) {
                    openMasters(.catalogue, openCreateForm: true)
                }
                masterRow(
                    glyph: "◍",
                    title: BusinessSetupDocSpec.networkLabel(doc.vertical),
                    subtitle: BusinessSetupDocSpec.networkSubtitle(doc.vertical, counts: counts),
                    tag: "setup.master.network"
                ) {
                    openMasters(.principals, openCreateForm: false)
                }
            } else {
                masterRow(
                    glyph: "◇",
                    title: "Catalogue & SKU",
                    subtitle: BusinessSetupDocSpec.catalogueSubtitle(counts),
                    tag: "setup.master.catalogue"
                ) {
                    openMasters(.catalogue, openCreateForm: false)
                }
                masterRow(
                    glyph: "⊞",
                    title: "+ Create SKU",
                    subtitle: BusinessSetupDocSpec.catalogueInvite,
                    tag: "setup.master.createSku"
                ) {
                    openMasters(.catalogue, openCreateForm: true)
                }
                masterRow(
                    glyph: "▤",
                    title: "Warehouses & routes",
                    subtitle: BusinessSetupDocSpec.warehouseSubtitle(names: warehouseNames, counts: counts),
                    tag: "setup.master.warehouses"
                ) {
                    openMasters(.locations, openCreateForm: false)
                }
                masterRow(
                    glyph: "◍",
                    title: BusinessSetupDocSpec.networkLabel(doc.vertical),
                    subtitle: BusinessSetupDocSpec.networkSubtitle(doc.vertical, counts: counts),
                    tag: "setup.master.network"
                ) {
                    openMasters(.principals, openCreateForm: false)
                }
            }
            masterRow(
                glyph: "❐",
                title: "Team invite & QR",
                subtitle: BusinessSetupDocSpec.inviteSubtitle(doc.vertical),
                tag: "setup.master.invite"
            ) {
                openSetupInvite()
            }
            footnote(BusinessSetupDocSpec.networkFootnote(doc.vertical))
        }
    }

    // MARK: §05

    private var gstSection: some View {
        VStack(alignment: .leading, spacing: 14) {
            Button {
                doc.gstOpen.toggle()
            } label: {
                Text(doc.gstOpen ? "Hide GST setup" : "Show GST setup")
                    .font(.plusJakarta(size: 12, weight: .semibold))
                    .foregroundStyle(docTheme.accent)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .contentShape(Rectangle())
            }
            .buttonStyle(.plain)

            if doc.gstOpen {
                SetupPanelCard(title: "Tracking", theme: docTheme) {
                    chipRow(
                        BusinessSetupDocSpec.gstTrackingLabels,
                        selected: [doc.gstTracking],
                        label: "Do you want to track GST in this Moment?"
                    ) { doc.gstTracking = $0 }
                    if doc.gstTracking != "Yes" {
                        footnote(BusinessSetupDocSpec.gstSkipCaveat, size: 11)
                    }
                }
                if doc.gstTracking == "Yes" {
                    SetupPanelCard(title: "Registration", theme: docTheme) {
                        chipRow(
                            BusinessSetupDocSpec.gstRegistrationLabels,
                            selected: [doc.gstRegistration],
                            label: "How is your business registered for GST?"
                        ) { doc.gstRegistration = $0 }
                        pickerRow(
                            "Which state is your business registered in?",
                            value: doc.gstState,
                            placeholder: "Pick a state",
                            hint: IndiaStateCatalog.gstinStateHint(gstin: doc.gstin, state: doc.gstState)
                                ?? "Used to decide CGST + SGST or IGST on every sale.",
                            tag: "setup.field.gstState"
                        ) { route = .picker(.gstState) }
                    }
                    SetupPanelCard(title: "Pricing", theme: docTheme) {
                        chipRow(
                            BusinessSetupDocSpec.gstPricingLabels,
                            selected: [doc.gstPricing],
                            label: "Do prices include GST?"
                        ) { doc.gstPricing = $0 }
                        footnote(BusinessSetupDocSpec.gstNote, size: 11)
                        dateSlot(
                            label: "Effective from (optional)",
                            value: doc.gstEffectiveFrom,
                            placeholder: "YYYY-MM-DD",
                            tag: "setup.field.gstEffectiveFrom",
                            onPick: { doc.gstEffectiveFrom = $0 }
                        )
                    }
                }
            }
        }
    }

    // MARK: §06

    private var reviewSection: some View {
        let owner = ownerName.trimmingCharacters(in: .whitespacesAndNewlines)
        let month = BusinessSetupDocSpec.monthLabel(doc.month)
        return VStack(alignment: .leading, spacing: 14) {
            SetupPanelCard(title: "Moment", theme: docTheme) {
                Text(displayName)
                    .font(.plusJakarta(size: 15, weight: .bold))
                    .foregroundStyle(docTheme.ink)
                Text(BusinessSetupDocSpec.momentLine(
                    vertical: doc.vertical,
                    currency: BusinessSetupDocSpec.currencyCode(doc.currency),
                    startDate: doc.startDate,
                    endDate: doc.endDate
                ))
                .font(.plusJakarta(size: 12))
                .foregroundStyle(docTheme.muted)
            }
            SetupPanelCard(title: "Summary", theme: docTheme) {
                keyValue("Company", doc.companyName.isEmpty ? (doc.businessName.isEmpty ? "—" : doc.businessName) : doc.companyName)
                keyValue("GSTIN", doc.gstin.isEmpty ? "Not added" : doc.gstin)
                keyValue("People", "\(owner.isEmpty ? "Owner" : owner) · Owner")
                keyValue("\(month) budget", doc.budget.isEmpty ? "Not set" : doc.budget)
                keyValue("\(month) sales target", doc.target.isEmpty ? "Not set" : doc.target)
                keyValue("Target measure", doc.metric)
            }
            SetupPanelCard(title: "Before you start", theme: docTheme) {
                footnote(doc.hasChannels
                    ? "Sales channels · \(doc.channelChoices.filter { doc.channels.contains($0) }.joined(separator: ", "))"
                    : "Choose at least one sales channel in section 03.", size: 11)
                footnote("Name, currency, owner, and sales channels are set. Budget and sales target are filled in.", size: 11)
                footnote("This stays private to your business. Only the owner can change who has access. Nothing is counted until you start.", size: 11)
            }
            statusPill(
                doc.canStart ? "Ready to start" : "A few fields still need you",
                good: doc.canStart
            )
            SetupDocCta(
                label: starting ? startingLabel : activateLabel,
                theme: docTheme,
                enabled: doc.canStart && !starting,
                tag: "setup.submit"
            ) {
                start()
            }
        }
    }

    /// A draft is started; a live moment is saved. Printing `Start this business` over a moment
    /// that is already running would read like a second, duplicate business.
    private var activateLabel: String {
        BusinessSetupDocSpec.activateLabel(status: momentStatus)
    }

    private var startingLabel: String {
        BusinessSetupDocSpec.activatingLabel(status: momentStatus)
    }

    // MARK: - Small pieces

    /// The document's field: `SetupDocField` owns the label, frame, hint and cursor, so the
    /// wizard only decides what is typed into it.
    private func field(
        _ label: String,
        text: Binding<String>,
        hint: String? = nil,
        placeholder: String = "",
        keyboard: UIKeyboardType = .default,
        axis: Axis = .horizontal
    ) -> some View {
        SetupDocField(
            label: label,
            text: text,
            theme: docTheme,
            hint: hint,
            placeholder: placeholder,
            keyboard: keyboard,
            axis: axis,
            tag: "setup.field.\(label)"
        )
    }

    private func pickerRow(
        _ label: String,
        value: String,
        placeholder: String = "Select",
        hint: String? = nil,
        tag: String,
        onTap: @escaping () -> Void
    ) -> some View {
        SetupDocPickerField(
            label: label,
            value: value,
            theme: docTheme,
            placeholder: placeholder,
            hint: hint,
            tag: tag,
            onTap: onTap
        )
    }

    /// Start date, end date, and GST effective-from read their ISO strings through
    /// `SetupDateTimeUtils`, so the document never invents a second date format.
    private func dateSlot(
        label: String,
        value: String,
        placeholder: String,
        tag: String,
        onPick: @escaping (String) -> Void
    ) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(label)
                .font(.plusJakarta(size: 11, weight: .medium))
                .foregroundStyle(docTheme.muted)
                .lineLimit(1)
            SetupDateTap(
                theme: docTheme,
                value: value,
                placeholder: placeholder,
                tag: tag,
                onPick: onPick
            )
        }
        .frame(maxWidth: .infinity)
    }

    private func chipRow(
        _ options: [String],
        selected: Set<String>,
        label: String? = nil,
        onSelect: @escaping (String) -> Void
    ) -> some View {
        SetupDocChipRow(
            options: options,
            selected: selected,
            theme: docTheme,
            label: label,
            onSelect: onSelect
        )
    }

    private func keyValue(_ key: String, _ value: String) -> some View {
        SetupDocKeyValue(label: key, value: value, theme: docTheme)
    }

    private func footnote(_ text: String, size: CGFloat = 12) -> some View {
        SetupDocFootnote(text: text, theme: docTheme, size: size)
    }

    private func why(_ title: String, _ body: String) -> some View {
        SetupDocWhyCard(title: title, bodyText: body, theme: docTheme)
    }

    private func statusPill(_ text: String, good: Bool) -> some View {
        SetupDocStatusPill(
            text: text,
            theme: docTheme,
            tone: good ? docTheme.positive : docTheme.muted
        )
    }

    private func masterRow(
        glyph: String,
        title: String,
        subtitle: String,
        tag: String,
        onTap: @escaping () -> Void
    ) -> some View {
        SetupDocMasterRow(
            glyph: glyph,
            title: title,
            subtitle: subtitle,
            theme: docTheme,
            tag: tag,
            onTap: onTap
        )
    }

    // MARK: - Sheets

    @ViewBuilder
    private func setupPicker(_ kind: SetupPickerKind) -> some View {
        switch kind {
        case .currency:
            VStack(alignment: .leading, spacing: 12) {
                Text("Currency")
                    .font(.plusJakarta(size: 22, weight: .semibold))
                    .foregroundStyle(docTheme.ink)
                ForEach(BusinessSetupDocSpec.currencyLabels, id: \.self) { label in
                    let on = label == doc.currency
                    Button {
                        doc.currency = label
                        route = nil
                    } label: {
                        HStack {
                            Text(label)
                                .font(.plusJakarta(size: 14))
                                .foregroundStyle(docTheme.ink)
                            Spacer(minLength: 4)
                            if on {
                                Text("●")
                                    .font(.system(size: 10))
                                    .foregroundStyle(docTheme.accent)
                            }
                        }
                        .padding(.horizontal, 14)
                        .padding(.vertical, 12)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(on ? docTheme.accent.opacity(0.14) : docTheme.inset)
                        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                        .overlay(
                            RoundedRectangle(cornerRadius: 14, style: .continuous)
                                .stroke(on ? docTheme.accent : docTheme.panelStroke, lineWidth: 1)
                        )
                    }
                    .buttonStyle(.plain)
                }
                Spacer(minLength: 0)
            }
            .padding(18)
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
            .background(docTheme.canvas.ignoresSafeArea())
        case .month:
            SetupMonthPickerSheet(
                theme: docTheme,
                initial: doc.month,
                onPick: {
                    doc.month = $0
                    route = nil
                },
                onCancel: { route = nil }
            )
        case .gstState:
            SetupStatePickerSheet(
                theme: docTheme,
                selected: doc.gstState,
                onPick: {
                    // Store the canonical name; the code is derived on emit.
                    doc.gstState = $0.name
                    route = nil
                },
                onCancel: { route = nil }
            )
        }
    }

    /// Opening a §04 row. Masters are company-scoped, and a freshly created draft may not have a
    /// company yet, so one is created the first time a row is actually used — an untouched
    /// document never leaves a company behind. When that is needed the sheet waits for the id:
    /// its model is built once, and presenting it with no company would leave it permanently
    /// empty.
    private func openMasters(_ tab: OperatingMasterTab, openCreateForm: Bool) {
        guard draftCompanyId == nil, let momentId = draftMomentId else {
            route = .masters(tab, openCreateForm: openCreateForm)
            return
        }
        let name = doc.businessName.trimmingCharacters(in: .whitespacesAndNewlines)
        Task {
            let cid = await createModel.ensureSetupCompany(
                momentId: momentId,
                companyName: name.isEmpty ? kind.title : name,
                taxIdentifier: doc.gstin.isEmpty ? nil : doc.gstin
            )
            guard let cid else { return }
            draftCompanyId = cid
            route = .masters(tab, openCreateForm: openCreateForm)
        }
    }

    private func mastersSheet(_ tab: OperatingMasterTab, openCreateForm: Bool) -> some View {
        BusinessSetupMastersSheet(
            doc: doc,
            theme: theme,
            companyId: draftCompanyId,
            momentId: draftMomentId,
            initialTab: tab,
            openCreateForm: openCreateForm,
            repo: repo,
            onChanged: { reloadToken += 1 }
        )
        .presentationDetents([.large])
        .presentationDragIndicator(.visible)
        .presentationCornerRadius(24)
        .preferredColorScheme(.dark)
    }

    // MARK: - §02 invite

    /// The invite sheet, on demand. Opening it is the first touch that needs a code, so the mint
    /// starts here — the same shape the group setup uses.
    private var inviteSheet: some View {
        BusinessSetupInviteSheet(
            title: displayName,
            theme: theme,
            roleCode: doc.inviteRole,
            invite: setupInvite,
            minting: mintingInvite,
            error: inviteError,
            existingNames: doc.invitees.map(\.name),
            onRoleChange: { code in
                doc.inviteRole = code
                // A code grants the role it was minted with, so a role change must re-mint.
                Task { await ensureInvite(roleCode: code, force: true) }
            },
            onAddPerson: { invitee in
                guard !doc.invitees.contains(where: {
                    $0.name.compare(invitee.name, options: .caseInsensitive) == .orderedSame
                }) else { return }
                doc.invitees.append(invitee)
            },
            onRetry: { Task { await ensureInvite(force: true) } },
            onDismiss: { route = nil }
        )
        .presentationDetents([.large])
        .presentationDragIndicator(.visible)
        .presentationCornerRadius(24)
        .preferredColorScheme(.dark)
        .task { await ensureInvite() }
    }

    /// Mints a code for the selected role, creating the setup company first when the draft has
    /// none. A code already minted for this role is reused; only a role change needs a new one.
    private func ensureInvite(roleCode: String? = nil, force: Bool = false) async {
        let code = roleCode ?? doc.inviteRole
        // Another mint is in flight. Wait it out rather than firing a second request, so a role
        // change still lands on the code it asked for instead of racing the one before it.
        var waitedMs = 0
        while mintingInvite, waitedMs < 15_000 {
            try? await Task.sleep(nanoseconds: 120_000_000)
            waitedMs += 120
            if let settled = setupInvite, settled.operatingRole == code { return }
        }
        if !force, let current = setupInvite, current.operatingRole == code { return }
        let momentId = draftMomentId
        var company = draftCompanyId
        if company == nil, let momentId {
            let name = doc.businessName.trimmingCharacters(in: .whitespacesAndNewlines)
            company = await createModel.ensureSetupCompany(
                momentId: momentId,
                companyName: name.isEmpty ? kind.title : name,
                taxIdentifier: doc.gstin.isEmpty ? nil : doc.gstin
            )
            if let company { draftCompanyId = company }
        }
        guard let company else {
            inviteError = "Could not prepare the company for an invite. Try again."
            return
        }
        mintingInvite = true
        inviteError = nil
        let minted = await createModel.mintCompanyInvite(
            companyId: company,
            membershipType: BusinessSetupDocSpec.membershipType(for: code),
            operatingRole: code
        )
        if let minted {
            setupInvite = minted
        } else {
            inviteError = "Could not mint an invite. Try again."
        }
        mintingInvite = false
    }

    private var activeInvitePath: String? {
        // A code grants the role it was minted with, so a stale code is never shared as this
        // chip's link — the card re-mints first, exactly like the sheet does.
        guard let invite = setupInvite, invite.operatingRole == doc.inviteRole else { return nil }
        let code = invite.inviteCode.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        guard !code.isEmpty else { return nil }
        if !invite.invitePath.isEmpty { return invite.invitePath }
        if !invite.inviteUrl.isEmpty { return invite.inviteUrl }
        return CompanyJoinLink.displayPath(code: code)
    }

    private func shareInviteQr() {
        guard let path = activeInvitePath, let invite = setupInvite else {
            Task {
                await ensureInvite()
                // Re-entering the share only helps when the mint landed; on a failure the caller
                // would otherwise loop, so the error stays on the card instead.
                if activeInvitePath != nil {
                    await MainActor.run { shareInviteQr() }
                }
            }
            return
        }
        let code = invite.inviteCode.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        guard let qr = GroupQRCode.image(from: CompanyJoinLink.qrPayload(code: code), size: 1024) else { return }
        InviteOutboundShare.presentSystemShare(items: [qr, path])
    }

    private func shareInviteWhatsApp() {
        guard let path = activeInvitePath else {
            Task {
                await ensureInvite()
                if activeInvitePath != nil {
                    await MainActor.run { shareInviteWhatsApp() }
                }
            }
            return
        }
        let body = InviteOutboundShare.inviteMessage(title: displayName, url: path)
        InviteOutboundShare.sendWhatsApp(phone: nil, message: body)
    }

    /// Opens §02's invite sheet. The mint starts with the sheet, so an untouched document never
    /// creates a company or an invite it does not need.
    private func openSetupInvite() {
        route = .invite
    }

    private func sheetVerticalPicker() {
        route = .picker(.currency)
    }

    private func sheetMonthPicker() {
        route = .picker(.month)
    }
}

/// One sheet at a time. Chaining `.sheet` modifiers on the same view is unreliable — only the
/// last one wins on some iOS versions — so the masters sheet and the two pickers share a router.
private enum SetupRoute: Identifiable {
    case masters(OperatingMasterTab, openCreateForm: Bool)
    case invite
    case picker(SetupPickerKind)

    var id: String {
        switch self {
        case let .masters(tab, openCreate):
            return "masters:\(tab.rawValue):\(openCreate)"
        case .invite:
            return "invite"
        case let .picker(kind):
            return "picker:\(kind.rawValue)"
        }
    }
}

/// Which in-document picker is open. The date fields carry their own `SetupDateField` sheet, so
/// only the currency, month, and state need one here.
private enum SetupPickerKind: String, Identifiable {
    case currency
    case month
    case gstState

    var id: String { rawValue }
}

/// The state picker in the document's own palette: a searchable list over the canonical India
/// States & UTs, states first and union territories after.
private struct SetupStatePickerSheet: View {
    let theme: BusinessSetupDocTheme
    let selected: String
    let onPick: (IndiaState) -> Void
    let onCancel: () -> Void

    @State private var query = ""

    private var matches: [IndiaState] { IndiaStateCatalog.search(query) }
    private var chosenCode: String { IndiaStateCatalog.codeOf(selected) }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("State")
                .font(.plusJakarta(size: 22, weight: .semibold))
                .foregroundStyle(theme.ink)
            TextField("", text: $query, prompt: Text("Search a state or code").foregroundStyle(theme.muted.opacity(0.7)))
                .font(.plusJakarta(size: 14))
                .foregroundStyle(theme.ink)
                .autocorrectionDisabled()
                .textInputAutocapitalization(.words)
                .padding(.horizontal, 12)
                .padding(.vertical, 10)
                .background(theme.inset)
                .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                .overlay(
                    RoundedRectangle(cornerRadius: 10, style: .continuous)
                        .stroke(theme.panelStroke, lineWidth: 1)
                )
            if matches.isEmpty {
                Text("No state matches \"\(query)\".")
                    .font(.plusJakarta(size: 13))
                    .foregroundStyle(theme.muted)
            }
            ScrollView {
                LazyVStack(alignment: .leading, spacing: 8) {
                    ForEach(grouped(matches), id: \.title) { group in
                        if let title = group.title {
                            Text(title)
                                .font(.plusJakarta(size: 11, weight: .semibold))
                                .foregroundStyle(theme.muted)
                                .padding(.top, 6)
                        }
                        ForEach(group.items) { state in
                            stateRow(state)
                        }
                    }
                }
            }
            Button("Cancel", action: onCancel)
                .font(.plusJakarta(size: 14, weight: .semibold))
                .foregroundStyle(theme.muted)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 12)
        }
        .padding(18)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
        .background(theme.canvas.ignoresSafeArea())
    }

    private func stateRow(_ state: IndiaState) -> some View {
        let on = !chosenCode.isEmpty && state.code == chosenCode
        return Button {
            onPick(state)
        } label: {
            HStack(spacing: 8) {
                Text(state.name)
                    .font(.plusJakarta(size: 14))
                    .foregroundStyle(theme.ink)
                    .lineLimit(1)
                Spacer(minLength: 4)
                Text(state.code)
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(theme.muted)
                if on {
                    Text("●")
                        .font(.plusJakarta(size: 10))
                        .foregroundStyle(theme.accent)
                }
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 12)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(on ? theme.accent.opacity(0.14) : theme.inset)
            .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .stroke(on ? theme.accent : theme.panelStroke, lineWidth: 1)
            )
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }

    private func grouped(_ rows: [IndiaState]) -> [(title: String?, items: [IndiaState])] {
        var groups: [(title: String?, items: [IndiaState])] = []
        let states = rows.filter { !$0.isUnionTerritory }
        let uts = rows.filter(\.isUnionTerritory)
        if !states.isEmpty { groups.append((title: "States", items: states)) }
        if !uts.isEmpty { groups.append((title: "Union territories", items: uts)) }
        return groups
    }
}

/// A month picker in the document's own palette. `SetupDateField` picks a day; §03 asks for a
/// month, so this is the one place a month control is drawn.
private struct SetupMonthPickerSheet: View {
    let theme: BusinessSetupDocTheme
    let initial: String
    let onPick: (String) -> Void
    let onCancel: () -> Void

    @State private var date: Date

    private static let formatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.dateFormat = "yyyy-MM"
        return formatter
    }()

    init(
        theme: BusinessSetupDocTheme,
        initial: String,
        onPick: @escaping (String) -> Void,
        onCancel: @escaping () -> Void
    ) {
        self.theme = theme
        self.initial = initial
        self.onPick = onPick
        self.onCancel = onCancel
        _date = State(initialValue: Self.formatter.date(from: initial) ?? Date())
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Month")
                .font(.plusJakarta(size: 22, weight: .semibold))
                .foregroundStyle(theme.ink)
            DatePicker("", selection: $date, displayedComponents: .date)
                .datePickerStyle(.graphical)
                .labelsHidden()
                .tint(theme.accent)
            HStack(spacing: 10) {
                Button("Cancel", action: onCancel)
                    .font(.plusJakarta(size: 14, weight: .semibold))
                    .foregroundStyle(theme.muted)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 12)
                    .overlay(
                        RoundedRectangle(cornerRadius: 12, style: .continuous)
                            .stroke(theme.panelStroke, lineWidth: 1)
                    )
                    .buttonStyle(.plain)
                Button {
                    onPick(Self.formatter.string(from: date))
                } label: {
                    Text("Use month")
                        .font(.plusJakarta(size: 14, weight: .semibold))
                        .foregroundStyle(theme.ink)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 12)
                        .background(theme.accent)
                        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                }
                .buttonStyle(.plain)
            }
            Spacer(minLength: 0)
        }
        .padding(18)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
        .background(theme.canvas.ignoresSafeArea())
    }
}

/// A date that opens the same graphical `DatePicker` as `SetupDateField`, but in the document's
/// own palette and with the frame's placeholder words (`Today`, `Still open`). The ISO conversion
/// still goes through `SetupDateTimeUtils`, so there is one date format in the app.
private struct SetupDateTap: View {
    let theme: BusinessSetupDocTheme
    let value: String
    let placeholder: String
    let tag: String
    let onPick: (String) -> Void

    @State private var showPicker = false
    @State private var draft = Date()

    private var display: String {
        guard !value.isEmpty else { return placeholder }
        return SetupDateTimeUtils.formatDateDisplay(value)
    }

    var body: some View {
        Button {
            draft = SetupDateTimeUtils.dateFromIso(value.isEmpty ? nil : value)
            showPicker = true
        } label: {
            HStack(spacing: 8) {
                Text(display)
                    .font(.plusJakarta(size: 14, weight: .medium))
                    .foregroundStyle(value.isEmpty ? theme.faint : theme.ink)
                    .lineLimit(1)
                Spacer(minLength: 4)
                Text("›")
                    .font(.plusJakarta(size: 16))
                    .foregroundStyle(theme.muted)
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 10)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(theme.inset)
            .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 10, style: .continuous)
                    .stroke(theme.panelStroke, lineWidth: 1)
            )
        }
        .buttonStyle(.plain)
        .accessibilityIdentifier(tag)
        .sheet(isPresented: $showPicker) {
            NavigationStack {
                DatePicker("", selection: $draft, displayedComponents: .date)
                    .datePickerStyle(.graphical)
                    .labelsHidden()
                    .tint(theme.accent)
                    .padding()
                    .navigationTitle("Pick a date")
                    .navigationBarTitleDisplayMode(.inline)
                    .toolbar {
                        ToolbarItem(placement: .cancellationAction) {
                            Button("Cancel") { showPicker = false }
                        }
                        ToolbarItem(placement: .confirmationAction) {
                            Button("OK") {
                                onPick(SetupDateTimeUtils.localDateString(from: draft))
                                showPicker = false
                            }
                        }
                    }
            }
            .presentationDetents([.medium])
            .preferredColorScheme(.dark)
        }
    }
}

// MARK: - Masters sheet

/// Wraps `OperatingMastersView` around a `BusinessOperatingModel` bound to the setup's draft, and
/// hands the setup a count refresh whenever a write lands.
private struct BusinessSetupMastersSheet: View {
    let doc: BusinessSetupDocState
    let theme: OperatingTheme
    let companyId: String?
    let momentId: String?
    let initialTab: OperatingMasterTab
    let openCreateForm: Bool
    let repo: BusinessOperatingGateway
    let onChanged: () -> Void

    @StateObject private var model: BusinessOperatingModel
    @Environment(\.dismiss) private var dismiss

    init(
        doc: BusinessSetupDocState,
        theme: OperatingTheme,
        companyId: String?,
        momentId: String?,
        initialTab: OperatingMasterTab,
        openCreateForm: Bool,
        repo: BusinessOperatingGateway,
        onChanged: @escaping () -> Void
    ) {
        self.doc = doc
        self.theme = theme
        self.companyId = companyId
        self.momentId = momentId
        self.initialTab = initialTab
        self.openCreateForm = openCreateForm
        self.repo = repo
        self.onChanged = onChanged
        _model = StateObject(wrappedValue: BusinessOperatingModel(
            momentId: momentId ?? "draft",
            companyId: companyId,
            subtype: doc.subtypeCode,
            repo: repo
        ))
    }

    var body: some View {
        VStack(spacing: 0) {
            HStack {
                Text("Masters")
                    .font(.plusJakarta(size: 16, weight: .semibold))
                    .foregroundStyle(theme.ink)
                Spacer(minLength: 4)
                Button {
                    dismiss()
                } label: {
                    Text("Done")
                        .font(.plusJakarta(size: 14, weight: .semibold))
                        .foregroundStyle(theme.accent)
                        .padding(.horizontal, 6)
                        .padding(.vertical, 4)
                }
                .buttonStyle(.plain)
            }
            .padding(.horizontal, 18)
            .padding(.top, 16)
            OperatingMastersView(
                theme: theme,
                model: model,
                initialTab: initialTab,
                openCreateForm: openCreateForm
            )
        }
        .background(theme.canvas.ignoresSafeArea())
        .onDisappear { onChanged() }
    }
}

/// Identity for the count reload task: a tuple is not `Equatable` enough for `task(id:)` on every
/// Swift version this app builds with, so it gets its own value.
private struct MasterReloadKey: Equatable {
    let companyId: String?
    let momentId: String?
    let token: Int
}

// MARK: - Draft lifecycle

private extension BusinessSetupWizardView {
    func close() {
        guard !closing, !starting else { return }
        closing = true
        saveTask?.cancel()
        // A draft nobody starts is not left behind as an orphan; a live moment is simply closed,
        // because discarding it would delete a record the operator is still running.
        if let draft = draftMomentId, !started, BusinessSetupDocSpec.isDiscardable(momentStatus) {
            createModel.discardMomentDraft(momentId: draft) {
                closing = false
                onClose()
            }
        } else {
            // No draft yet means the create is still in flight; its result is discarded when
            // it lands.
            if draftMomentId == nil { abandoned = true }
            closing = false
            onClose()
        }
    }

    /// Opening the document creates its draft, so §04 has somewhere to write and §06 has
    /// something to promote. A resumed draft or an edited moment is prefilled instead.
    func openDraft() async {
        if let editingMomentId {
            await prefill(momentId: editingMomentId)
            return
        }
        guard draftMomentId == nil else { return }
        createModel.submitBusinessSetup(
            kind: kind,
            companyId: companyId,
            preferences: doc.preferences(ownerName: ownerName),
            title: titleForWire(),
            subtype: doc.subtypeCode,
            status: "DRAFT",
            onSuccess: { outcome in
                if abandoned {
                    // Nobody is looking at this draft any more; leaving it behind would show an
                    // empty setup in the moment directory.
                    createModel.discardMomentDraft(momentId: outcome.momentId) {}
                } else {
                    draftMomentId = outcome.momentId
                    Task { await loadCompany(for: outcome.momentId) }
                }
            }
        )
    }

    func prefill(momentId: String) async {
        guard let prefill = await createModel.getDomainSetupPrefill(momentId: momentId) else {
            await loadCompany(for: momentId)
            return
        }
        var next = doc
        if let preferences = prefill.preferences?.mapValues({ $0.value }) {
            next.apply(preferences: preferences)
        }
        if !prefill.title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty,
           next.businessName.isEmpty {
            next.businessName = prefill.title
        }
        doc = next
        // The wire's status is authoritative: a draft stays a draft, and a live moment keeps its
        // status so `Close` never tries to discard it.
        let status = prefill.status.trimmingCharacters(in: .whitespacesAndNewlines).uppercased()
        if !status.isEmpty {
            momentStatus = status
        }
        if let company = prefill.companyId?.trimmingCharacters(in: .whitespacesAndNewlines),
           !company.isEmpty {
            draftCompanyId = company
        }
    }

    /// The draft carries the company, and that is what the masters endpoints need.
    func loadCompany(for momentId: String) async {
        guard draftCompanyId == nil else { return }
        guard let prefill = await createModel.getDomainSetupPrefill(momentId: momentId) else { return }
        if let company = prefill.companyId?.trimmingCharacters(in: .whitespacesAndNewlines),
           !company.isEmpty {
            draftCompanyId = company
        }
    }

    /// A debounced save keeps the draft in step with the document, so closing mid-way does not
    /// lose §01-§05 and §06 only has to flip the status. A live moment has no draft row to patch,
    /// so its edits ride the §06 submit instead.
    func scheduleDraftSave() {
        guard let momentId = draftMomentId, !started,
              BusinessSetupDocSpec.isPromotable(momentStatus) else { return }
        saveTask?.cancel()
        let preferences = doc.preferences(ownerName: ownerName)
        let title = titleForWire()
        saveTask = Task {
            try? await Task.sleep(nanoseconds: 700_000_000)
            guard !Task.isCancelled else { return }
            await createModel.saveBusinessSetupDraft(
                momentId: momentId,
                title: title,
                preferences: preferences
            )
        }
    }

    func titleForWire() -> String {
        let name = doc.businessName.trimmingCharacters(in: .whitespacesAndNewlines)
        return name.isEmpty ? kind.title : name
    }

    func loadCounts() async {
        guard let companyId = draftCompanyId, !companyId.isEmpty else { return }
        do {
            let masters = try await repo.listMasters(
                companyId: companyId,
                kind: nil,
                momentId: draftMomentId
            )
            let byKind = Dictionary(grouping: masters) { $0.kind.uppercased() }
            func count(_ kinds: [String]) -> Int {
                kinds.reduce(0) { $0 + (byKind[$1]?.count ?? 0) }
            }
            let lists = (try? await repo.listPriceLists(companyId: companyId, momentId: draftMomentId))?.count ?? 0
            counts = BusinessSetupMasterCounts(
                skus: count(["SKU", "PRODUCT", "BATCH"]),
                priceLists: lists,
                warehouses: count(["WAREHOUSE", "LOCATION"]),
                routes: count(["ROUTE"]),
                principals: count(["PRINCIPAL", "SUPPLIER"]),
                dealers: count(["DEALER", "BUYER", "CUSTOMER"])
            )
            warehouseNames = masters
                .filter { ["WAREHOUSE", "LOCATION"].contains($0.kind.uppercased()) }
                .map(\.name)
        } catch {
            // A failed count read leaves the invitation copy in place — a zero is never invented.
            counts = BusinessSetupMasterCounts()
            warehouseNames = []
        }
    }

    func start() {
        guard !starting, let draft = draftMomentId else { return }
        starting = true
        saveTask?.cancel()
        createModel.submitBusinessSetup(
            kind: kind,
            companyId: draftCompanyId,
            preferences: doc.preferences(ownerName: ownerName),
            title: titleForWire(),
            subtype: doc.subtypeCode,
            editingMomentId: draft,
            editingMomentStatus: momentStatus,
            status: "ACTIVE",
            companyName: doc.companyName,
            taxIdentifier: doc.gstin,
            onSuccess: { outcome in
                starting = false
                started = true
                onCreated(outcome)
            }
        )
    }
}

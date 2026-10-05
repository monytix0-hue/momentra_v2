import SwiftUI
import FirebaseAuth

private enum CompanySheetPage {
    case switcher
    case settings
    case create
}

private let companyIndustries = [
    "Retail / Kirana",
    "Pet store / Specialty",
    "Manufacturing / Workshop",
    "Restaurant / F&B",
    "Fashion / Apparel",
    "Services",
    "Wholesale",
    "Technology & Software",
    "E-commerce",
    "Other",
]

private let companySizes = ["Solo (1)", "Small (2-25)", "Medium (26-100)"]

/// Figma 1687:21554 — switch, settings, and create company sheets.
struct CompanyFlowSheet: View {
    var companies: [CompanySummary]
    var selectedCompanyId: String?
    var startOnCreate: Bool = false
    var startOnSettings: Bool = false
    var onClose: () -> Void
    var onSelect: (CompanySummary) -> Void
    var onCreated: (CompanySummary) -> Void
    var onOpenLocations: () -> Void = {}
    var onCompaniesChanged: () -> Void = {}

    @State private var page: CompanySheetPage
    @State private var settingsCompanyId = ""
    @State private var settingsName = ""

    init(
        companies: [CompanySummary],
        selectedCompanyId: String?,
        startOnCreate: Bool = false,
        startOnSettings: Bool = false,
        onClose: @escaping () -> Void,
        onSelect: @escaping (CompanySummary) -> Void,
        onCreated: @escaping (CompanySummary) -> Void,
        onOpenLocations: @escaping () -> Void = {},
        onCompaniesChanged: @escaping () -> Void = {}
    ) {
        self.companies = companies
        self.selectedCompanyId = selectedCompanyId
        self.startOnCreate = startOnCreate
        self.startOnSettings = startOnSettings
        self.onClose = onClose
        self.onSelect = onSelect
        self.onCreated = onCreated
        self.onOpenLocations = onOpenLocations
        self.onCompaniesChanged = onCompaniesChanged
        let initial: CompanySheetPage = startOnCreate ? .create : (startOnSettings ? .settings : .switcher)
        _page = State(initialValue: initial)
        let selected = companies.first { $0.companyId == selectedCompanyId }
        _settingsCompanyId = State(initialValue: selected?.companyId ?? "")
        _settingsName = State(initialValue: selected?.displayName ?? "")
    }

    var body: some View {
        Group {
            if startOnCreate || page == .create {
                CompanySetupFlowView(
                    onClose: { startOnCreate ? onClose() : (page = .switcher) },
                    onActivated: { company in
                        onCreated(company)
                        onClose()
                    }
                )
            } else {
                ScrollView {
                    VStack(alignment: .leading, spacing: 12) {
                        switch page {
                        case .switcher:
                            switchPage
                        case .settings:
                            settingsPage
                        case .create:
                            EmptyView()
                        }
                    }
                    .padding(.bottom, 24)
                }
            }
        }
        .background(Color(hex: "#161B26").ignoresSafeArea())
    }

    private var switchPage: some View {
        VStack(alignment: .leading, spacing: 8) {
            sheetHeader("Switch Company", subtitle: nil, close: onClose)
            ForEach(companies) { company in
                HStack(spacing: 12) {
                    Button {
                        onSelect(company)
                        onClose()
                    } label: {
                        HStack(spacing: 12) {
                            initials(company.displayName)
                            Text(company.displayName)
                                .font(.system(size: 15, weight: .semibold))
                                .foregroundStyle(Color(hex: "#F1F5F9"))
                            Spacer()
                            if company.companyId == selectedCompanyId {
                                Image(systemName: "checkmark")
                                    .foregroundStyle(Color(hex: "#818CF8"))
                            }
                        }
                    }
                    .buttonStyle(.plain)
                    Button {
                        settingsCompanyId = company.companyId
                        settingsName = company.displayName
                        page = .settings
                    } label: {
                        Image(systemName: "gearshape")
                            .foregroundStyle(Color(hex: "#CBD5E1"))
                            .frame(width: 32, height: 32)
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel("Company settings")
                }
                .padding(12)
                .background(
                    company.companyId == selectedCompanyId
                        ? Color(hex: "#818CF8").opacity(0.12)
                        : Color.clear
                )
                .clipShape(RoundedRectangle(cornerRadius: 12))
            }
            .padding(.horizontal, 16)
            Divider().overlay(Color.white.opacity(0.10)).padding(.horizontal, 16)
            Button {
                page = .create
            } label: {
                HStack(spacing: 12) {
                    Image(systemName: "plus")
                        .foregroundStyle(Color(hex: "#818CF8"))
                        .frame(width: 36, height: 36)
                        .overlay(Circle().stroke(Color(hex: "#818CF8"), lineWidth: 1))
                    Text("Add New Company")
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundStyle(Color(hex: "#F1F5F9"))
                }
                .padding(.horizontal, 16)
            }
            .buttonStyle(.plain)
        }
    }

    private var settingsPage: some View {
        BusinessSettingsPage(
            companyId: settingsCompanyId,
            companyName: settingsName,
            onClose: { page = .switcher },
            onOpenLocations: onOpenLocations,
            onCompaniesChanged: {
                onCompaniesChanged()
                onClose()
            }
        )
    }
}

private let entityTypeOptions = ["Pvt Ltd", "LLP", "Partnership", "Sole Prop"]

private let currencyOptions = [
    "₹ INR — Indian Rupee",
    "$ USD — US Dollar",
    "€ EUR — Euro",
    "£ GBP — British Pound",
    "د.إ AED — UAE Dirham",
    "S$ SGD — Singapore Dollar",
]

private struct ModuleToggleDef {
    let label: String
    let key: String
    var smallShopHidden: Bool = false
}

private let moduleToggleDefs: [ModuleToggleDef] = [
    ModuleToggleDef(label: "Team & Work", key: "teamOps"),
    ModuleToggleDef(label: "Daily Business", key: "dailyBusiness"),
    ModuleToggleDef(label: "Money & Cash Flow", key: "money"),
    ModuleToggleDef(label: "Projects & Tasks", key: "projects", smallShopHidden: true),
    ModuleToggleDef(label: "Events & Plans", key: "events", smallShopHidden: true),
    ModuleToggleDef(label: "Suppliers & Vendors", key: "vendors"),
]

private let alertToggleDefs: [(String, String)] = [
    ("Purchase & Expense Alerts", "purchaseExpense"),
    ("Approval Requests", "approvals"),
    ("Issue / Risk Alerts", "issues"),
    ("Payment Reminders", "paymentReminders"),
    ("Important Business Updates", "importantUpdates"),
]

private struct CompanyCreateForm: View {
    var onCancel: () -> Void
    var onCreated: (CompanySummary) -> Void

    @State private var name = ""
    @State private var industry = ""
    @State private var size = "Solo (1)"
    @State private var saving = false
    @State private var error: String?

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            sheetHeader("Create Company", subtitle: nil, close: onCancel)
            HStack(spacing: 12) {
                Image(systemName: "plus")
                    .foregroundStyle(Color(hex: "#818CF8"))
                    .frame(width: 56, height: 56)
                    .overlay(Circle().stroke(Color(hex: "#818CF8"), lineWidth: 1))
                VStack(alignment: .leading, spacing: 2) {
                    Text("Company Logo")
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundStyle(Color(hex: "#F1F5F9"))
                    Text("Optional")
                        .font(.system(size: 12))
                        .foregroundStyle(Color(hex: "#64748B"))
                }
            }
            .padding(.horizontal, 16)
            fieldLabel("COMPANY NAME")
            TextField("e.g. Acme Corp", text: $name)
                .padding(14)
                .background(Color(hex: "#0C0F15"))
                .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color.white.opacity(0.10)))
                .clipShape(RoundedRectangle(cornerRadius: 12))
                .padding(.horizontal, 16)
            fieldLabel("INDUSTRY")
            Menu {
                ForEach(companyIndustries, id: \.self) { option in
                    Button(option) { industry = option }
                }
            } label: {
                HStack {
                    Text(industry.isEmpty ? "Select industry..." : industry)
                        .foregroundStyle(industry.isEmpty ? Color(hex: "#64748B") : Color(hex: "#F1F5F9"))
                    Spacer()
                    Image(systemName: "chevron.down").foregroundStyle(Color(hex: "#64748B"))
                }
                .padding(14)
                .background(Color(hex: "#0C0F15"))
                .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color.white.opacity(0.10)))
                .clipShape(RoundedRectangle(cornerRadius: 12))
            }
            .padding(.horizontal, 16)
            fieldLabel("COMPANY SIZE")
            FlexibleChipRow(options: companySizes, selected: size) { size = $0 }
                .padding(.horizontal, 16)
            if let error {
                Text(error)
                    .font(.system(size: 12))
                    .foregroundStyle(Color(hex: "#F87171"))
                    .padding(.horizontal, 16)
            }
            Button(action: submit) {
                Text(saving ? "Creating…" : "Create Company")
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(Color(hex: "#F1F5F9"))
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 14)
                    .background(Color(hex: "#818CF8").opacity(name.trimmingCharacters(in: .whitespaces).isEmpty || saving ? 0.35 : 0.9))
                    .clipShape(RoundedRectangle(cornerRadius: 12))
            }
            .buttonStyle(.plain)
            .disabled(name.trimmingCharacters(in: .whitespaces).isEmpty || saving)
            .padding(.horizontal, 16)
            Button("Cancel", action: onCancel)
                .font(.system(size: 15, weight: .semibold))
                .foregroundStyle(Color(hex: "#CBD5E1"))
                .frame(maxWidth: .infinity)
        }
    }

    private func submit() {
        let trimmed = name.trimmingCharacters(in: .whitespaces)
        guard !trimmed.isEmpty, !saving else { return }
        saving = true
        error = nil
        Task {
            do {
                var profile: [String: Any] = [
                    "companySize": size,
                    BusinessAudience.prefKey: BusinessAudience.smallShop,
                ]
                if !industry.isEmpty { profile["industry"] = industry }
                let created = try await APIClient.shared.createCompany(
                    displayName: trimmed,
                    legalName: trimmed,
                    timezone: TimeZone.current.identifier,
                    profileJson: profile
                )
                BusinessAudience.saveForCompany(companyId: created.companyId, audience: BusinessAudience.smallShop)
                await MainActor.run {
                    onCreated(CompanySummary(companyId: created.companyId, displayName: created.displayName))
                }
            } catch {
                await MainActor.run {
                    saving = false
                    self.error = "Could not create company"
                }
            }
        }
    }
}

private struct FlexibleChipRow: View {
    let options: [String]
    let selected: String
    var onSelect: (String) -> Void

    var body: some View {
        LazyVGrid(columns: [GridItem(.adaptive(minimum: 72), spacing: 8)], spacing: 8) {
            ForEach(options, id: \.self) { option in
                let on = option == selected
                Button(option) { onSelect(option) }
                    .font(.system(size: 13, weight: .medium))
                    .foregroundStyle(on ? Color(hex: "#F1F5F9") : Color(hex: "#CBD5E1"))
                    .padding(.horizontal, 12)
                    .padding(.vertical, 8)
                    .background(on ? Color(hex: "#818CF8").opacity(0.25) : Color.clear)
                    .overlay(Capsule().stroke(on ? Color(hex: "#818CF8") : Color.white.opacity(0.10)))
                    .clipShape(Capsule())
                    .buttonStyle(.plain)
            }
        }
    }
}

@ViewBuilder
private func sheetHeader(_ title: String, subtitle: String?, close: @escaping () -> Void) -> some View {
    HStack(alignment: .center) {
        VStack(alignment: .leading, spacing: 2) {
            Text(title)
                .font(.system(size: 18, weight: .semibold))
                .foregroundStyle(Color(hex: "#F1F5F9"))
            if let subtitle, !subtitle.isEmpty {
                Text(subtitle)
                    .font(.system(size: 13))
                    .foregroundStyle(Color(hex: "#CBD5E1"))
            }
        }
        Spacer()
        Button(action: close) {
            Image(systemName: "xmark")
                .foregroundStyle(Color(hex: "#CBD5E1"))
                .frame(width: 32, height: 32)
        }
        .buttonStyle(.plain)
        .accessibilityLabel("Close")
    }
    .padding(.horizontal, 16)
    .padding(.top, 12)
}

private func fieldLabel(_ text: String) -> some View {
    Text(text)
        .font(.system(size: 11, weight: .semibold))
        .foregroundStyle(Color(hex: "#64748B"))
        .padding(.horizontal, 16)
}

private func initials(_ name: String) -> some View {
    Text(companyInitials(name))
        .font(.system(size: 13, weight: .bold))
        .foregroundStyle(Color(hex: "#818CF8"))
        .frame(width: 40, height: 40)
        .background(Color(hex: "#818CF8").opacity(0.2), in: Circle())
}

func companyInitials(_ name: String) -> String {
    let parts = name.split(separator: " ").filter { !$0.isEmpty }
    let letters: String
    if parts.isEmpty {
        letters = "?"
    } else if parts.count == 1 {
        letters = String(parts[0].prefix(2))
    } else {
        letters = String(parts[0].prefix(1) + parts[1].prefix(1))
    }
    return letters.uppercased()
}

private struct BusinessSettingsPage: View {
    var companyId: String
    var companyName: String
    var onClose: () -> Void
    var onOpenLocations: () -> Void = {}
    var onCompaniesChanged: () -> Void = {}

    @State private var loading = true
    @State private var loadError: String?
    @State private var version: Int?

    @State private var displayName = ""
    @State private var legalName = ""
    @State private var taxIdentifier = ""
    @State private var companyType = entityTypeOptions[0]
    @State private var industry = ""
    @State private var companySize = companySizes[0]
    @State private var currency = currencyOptions[0]
    @State private var financialYear = "—"
    @State private var smallShop = false
    @State private var industryTemplateId: String?

    @State private var moduleToggles: [String: Bool] = [:]
    @State private var alertToggles: [String: Bool] = [:]
    @State private var memoryOn = true

    @State private var members: [APIClient.CompanyMemberPayload] = []
    @State private var defaultLocationName: String?
    @State private var showInviteSheet = false
    @State private var inviteBusy = false
    @State private var inviteError: String?
    @State private var inviteMessage: String?

    @State private var showTransferSheet = false
    @State private var confirmStatus: String?
    @State private var managementBusy = false
    @State private var managementError: String?
    @State private var managementOk: String?

    @State private var savingProfile = false
    @State private var profileSaveError: String?
    @State private var profileSaved = false
    @State private var settingsPatchBusy = false
    @State private var settingsPatchError: String?

    private var currentUserId: String {
        guard let uid = Auth.auth().currentUser?.uid else { return "" }
        return MomentraIdentityCache.load(firebaseUid: uid)?.userId ?? ""
    }

    private var isOwner: Bool {
        members.contains {
            $0.userId == currentUserId && $0.membershipType.uppercased() == "OWNER"
        }
    }

    private var transferCandidates: [APIClient.CompanyMemberPayload] {
        members.filter {
            $0.status.uppercased() == "ACTIVE" && $0.userId != currentUserId
        }
    }

    private var headerName: String {
        let name = displayName.isEmpty ? companyName : displayName
        return name.isEmpty ? "Company" : name
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            sheetHeader("Company Settings", subtitle: headerName, close: onClose)
            if loading {
                Text("Loading…").font(.system(size: 13)).foregroundStyle(Color(hex: "#64748B"))
            }
            if let loadError {
                Text(loadError).font(.system(size: 12)).foregroundStyle(Color(hex: "#F87171"))
            }
            if let settingsPatchError {
                Text(settingsPatchError).font(.system(size: 12)).foregroundStyle(Color(hex: "#F87171"))
            }
            settingsCard("Business Profile") {
                settingsTextField("Business Name", text: $displayName)
            }
            settingsCard("Business Details", note: "Optional formal business information.") {
                settingsTextField("Legal Business Name", text: $legalName)
                settingsMenuField("Business Registration Type", selection: $companyType, options: entityTypeOptions)
                settingsTextField("GSTIN / Tax ID", text: $taxIdentifier)
                settingsMenuField("Industry", selection: $industry, options: companyIndustries, placeholder: "Select industry…")
                fieldLabel("COMPANY SIZE")
                FlexibleChipRow(options: companySizes, selected: companySize) { companySize = $0 }
                settingsMenuField("Currency", selection: $currency, options: currencyOptions)
                if let profileSaveError {
                    Text(profileSaveError).font(.system(size: 12)).foregroundStyle(Color(hex: "#F87171"))
                }
                if profileSaved {
                    Text("Saved").font(.system(size: 12)).foregroundStyle(Color(hex: "#10B981"))
                }
                Button(action: saveProfile) {
                    Text(savingProfile ? "Saving…" : "Save profile")
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundStyle(Color(hex: "#F1F5F9"))
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 12)
                        .background(Color(hex: "#818CF8").opacity(savingProfile || companyId.isEmpty || version == nil ? 0.35 : 0.9))
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                }
                .buttonStyle(.plain)
                .disabled(savingProfile || companyId.isEmpty || version == nil)
            }
            settingsCard("Business Accounts") {
                Text("Accounts · Coming soon")
                    .font(.system(size: 12))
                    .foregroundStyle(Color(hex: "#64748B").opacity(0.85))
            }
            settingsCard("Money Settings") {
                valueRow("Currency", currency.isEmpty ? "—" : currency)
                valueRow("Financial Year", financialYear)
                valueRow("Default Expense Account", "—", muted: true)
                valueRow("Default Location", defaultLocationName ?? "—", muted: defaultLocationName == nil)
            }
            settingsCard("Locations") {
                Button(action: onOpenLocations) {
                    Text("Manage locations")
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundStyle(Color(hex: "#818CF8"))
                        .frame(maxWidth: .infinity, alignment: .leading)
                }
                .buttonStyle(.plain)
            }
            settingsCard("Team & Roles") {
                if members.isEmpty {
                    Text("No team members yet").font(.system(size: 13)).foregroundStyle(Color(hex: "#64748B"))
                } else {
                    ForEach(members) { member in
                        valueRow(
                            PersonLabel.personName(member.displayName),
                            member.membershipType
                        )
                    }
                }
                Button {
                    Task { await mintInvite() }
                } label: {
                    Text(inviteBusy ? "Preparing…" : "Invite")
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundStyle(Color(hex: "#F1F5F9"))
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 12)
                        .background(Color(hex: "#818CF8").opacity(companyId.isEmpty || inviteBusy ? 0.35 : 0.9))
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                }
                .buttonStyle(.plain)
                .disabled(companyId.isEmpty || inviteBusy)
                if let inviteError {
                    Text(inviteError).font(.system(size: 12)).foregroundStyle(Color(hex: "#F87171"))
                }
            }
            settingsCard("What do you use Momentra for?", note: "Turn on only the business moment types you want to use.") {
                if smallShop, let industryTemplateId, !industryTemplateId.isEmpty {
                    Text("Template: \(IndustryTemplateCatalog.labelFor(industryTemplateId))")
                        .font(.system(size: 13, weight: .medium))
                        .foregroundStyle(Color(hex: "#CBD5E1"))
                    Text("Change modules anytime — you are not locked to the template.")
                        .font(.system(size: 11))
                        .foregroundStyle(Color(hex: "#64748B").opacity(0.8))
                }
                ForEach(moduleToggleDefs.filter { !smallShop || !$0.smallShopHidden }, id: \.key) { def in
                    Toggle(def.label, isOn: moduleBinding(for: def.key))
                        .font(.system(size: 14))
                        .foregroundStyle(Color(hex: "#F1F5F9"))
                        .tint(Color(hex: "#818CF8"))
                }
            }
            settingsCard("Notifications & Approvals") {
                ForEach(alertToggleDefs, id: \.1) { label, key in
                    Toggle(label, isOn: alertBinding(for: key))
                        .font(.system(size: 14))
                        .foregroundStyle(Color(hex: "#F1F5F9"))
                        .tint(Color(hex: "#818CF8"))
                }
            }
            settingsCard("Business Memory & Data") {
                Toggle("Business Memory", isOn: memoryBinding)
                    .font(.system(size: 14))
                    .foregroundStyle(Color(hex: "#F1F5F9"))
                    .tint(Color(hex: "#818CF8"))
                Text("Archive, export, and retention options coming later")
                    .font(.system(size: 12))
                    .foregroundStyle(Color(hex: "#64748B").opacity(0.8))
            }
            settingsCard("Plan & Usage") {
                Text("Plan details · Coming soon")
                    .font(.system(size: 12))
                    .foregroundStyle(Color(hex: "#64748B").opacity(0.85))
            }
            settingsCard("Business Management") {
                if !isOwner {
                    Text("Owner actions only")
                        .font(.system(size: 12))
                        .foregroundStyle(Color(hex: "#64748B").opacity(0.8))
                } else {
                    Button {
                        showTransferSheet = true
                    } label: {
                        Text("Transfer Ownership")
                            .font(.system(size: 13, weight: .semibold))
                            .foregroundStyle(transferCandidates.isEmpty || managementBusy
                                             ? Color(hex: "#64748B")
                                             : Color(hex: "#818CF8"))
                            .frame(maxWidth: .infinity, alignment: .leading)
                    }
                    .buttonStyle(.plain)
                    .disabled(transferCandidates.isEmpty || managementBusy)
                    if transferCandidates.isEmpty {
                        Text("Invite another member before transferring")
                            .font(.system(size: 11))
                            .foregroundStyle(Color(hex: "#64748B").opacity(0.8))
                    }
                    Button {
                        confirmStatus = "ARCHIVED"
                    } label: {
                        Text("Archive Business")
                            .font(.system(size: 13, weight: .semibold))
                            .foregroundStyle(managementBusy || version == nil
                                             ? Color(hex: "#64748B")
                                             : Color(hex: "#818CF8"))
                            .frame(maxWidth: .infinity, alignment: .leading)
                    }
                    .buttonStyle(.plain)
                    .disabled(managementBusy || version == nil)
                    Button {
                        confirmStatus = "INACTIVE"
                    } label: {
                        Text("Deactivate Business")
                            .font(.system(size: 13, weight: .semibold))
                            .foregroundStyle(managementBusy || version == nil
                                             ? Color(hex: "#64748B")
                                             : Color(hex: "#818CF8"))
                            .frame(maxWidth: .infinity, alignment: .leading)
                    }
                    .buttonStyle(.plain)
                    .disabled(managementBusy || version == nil)
                }
                if let managementError {
                    Text(managementError).font(.system(size: 12)).foregroundStyle(Color(hex: "#F87171"))
                }
                if let managementOk {
                    Text(managementOk).font(.system(size: 12)).foregroundStyle(Color(hex: "#10B981"))
                }
            }
        }
        .padding(.horizontal, 16)
        .task(id: companyId) { await loadAll() }
        .onAppear {
            if displayName.isEmpty { displayName = companyName }
        }
        .confirmationDialog(
            confirmStatus == "ARCHIVED" ? "Archive business?" : "Deactivate business?",
            isPresented: Binding(
                get: { confirmStatus != nil },
                set: { if !$0 { confirmStatus = nil } }
            ),
            titleVisibility: .visible
        ) {
            Button(confirmStatus == "ARCHIVED" ? "Archive" : "Deactivate", role: .destructive) {
                Task { await applyStatus(confirmStatus ?? "INACTIVE") }
            }
            Button("Cancel", role: .cancel) { confirmStatus = nil }
        } message: {
            Text(
                confirmStatus == "ARCHIVED"
                    ? "The company will leave your switcher. Data is kept for later."
                    : "The company will be deactivated and leave your switcher."
            )
        }
        .sheet(isPresented: $showTransferSheet) {
            NavigationStack {
                List {
                    Section {
                        Text("Pick who becomes owner. You stay on the company as Admin.")
                            .font(.system(size: 13))
                            .foregroundStyle(Color(hex: "#94A3B8"))
                            .listRowBackground(Color.clear)
                    }
                    Section {
                        ForEach(transferCandidates) { member in
                            Button {
                                Task { await transferTo(member.userId) }
                            } label: {
                                Text(PersonLabel.personName(member.displayName))
                                    .foregroundStyle(Color(hex: "#818CF8"))
                            }
                            .disabled(managementBusy)
                        }
                    }
                }
                .navigationTitle("Transfer ownership")
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .cancellationAction) {
                        Button("Close") { showTransferSheet = false }
                    }
                }
            }
            .presentationDetents([.medium])
        }
        .sheet(isPresented: $showInviteSheet) {
            NavigationStack {
                VStack(alignment: .leading, spacing: 16) {
                    Text("Invite teammates")
                        .font(.system(size: 18, weight: .bold))
                        .foregroundStyle(Color(hex: "#F1F5F9"))
                    if let inviteMessage {
                        Text(inviteMessage)
                            .font(.system(size: 13))
                            .foregroundStyle(Color(hex: "#94A3B8"))
                            .textSelection(.enabled)
                        ShareLink(item: inviteMessage) {
                            Text("Share invite")
                                .font(.system(size: 14, weight: .semibold))
                                .foregroundStyle(Color(hex: "#F1F5F9"))
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 12)
                                .background(Color(hex: "#818CF8"))
                                .clipShape(RoundedRectangle(cornerRadius: 12))
                        }
                    } else {
                        ProgressView()
                    }
                    Spacer()
                }
                .padding(20)
                .background(Color(hex: "#0F172A").ignoresSafeArea())
                .toolbar {
                    ToolbarItem(placement: .cancellationAction) {
                        Button("Close") { showInviteSheet = false }
                    }
                }
            }
            .presentationDetents([.medium])
        }
    }

    private func transferTo(_ userId: String) async {
        guard !companyId.isEmpty else { return }
        managementBusy = true
        managementError = nil
        do {
            _ = try await APIClient.shared.transferCompanyOwnership(
                companyId: companyId,
                transferUserId: userId
            )
            await MainActor.run {
                managementBusy = false
                showTransferSheet = false
                managementOk = "Ownership transferred"
            }
            await loadAll()
        } catch {
            await MainActor.run {
                managementBusy = false
                managementError = error.localizedDescription
            }
        }
    }

    private func applyStatus(_ status: String) async {
        guard !companyId.isEmpty, let version else {
            confirmStatus = nil
            return
        }
        managementBusy = true
        managementError = nil
        do {
            _ = try await APIClient.shared.patchCompany(
                companyId: companyId,
                body: [
                    "expectedVersion": version,
                    "status": status,
                ]
            )
            await MainActor.run {
                managementBusy = false
                confirmStatus = nil
                onCompaniesChanged()
            }
        } catch {
            await MainActor.run {
                managementBusy = false
                confirmStatus = nil
                managementError = error.localizedDescription
            }
        }
    }

    private func mintInvite() async {
        guard !companyId.isEmpty else { return }
        inviteBusy = true
        inviteError = nil
        do {
            let invite = try await APIClient.shared.mintCompanyInvite(
                companyId: companyId,
                membershipType: "MEMBER",
                idempotencyKey: UUID().uuidString
            )
            let path = invite.invitePath.isEmpty
                ? CompanyJoinLink.displayPath(code: invite.inviteCode)
                : invite.invitePath
            let title = displayName.isEmpty ? headerName : displayName
            inviteMessage = InviteOutboundShare.inviteMessage(title: title, url: path)
            showInviteSheet = true
        } catch {
            inviteError = error.localizedDescription
        }
        inviteBusy = false
    }

    private func loadAll() async {
        guard !companyId.isEmpty else {
            loading = false
            loadError = "No company selected"
            return
        }
        loading = true
        loadError = nil
        do {
            async let companyTask = APIClient.shared.getCompany(companyId: companyId)
            async let membersTask = APIClient.shared.listCompanyMembers(companyId: companyId)
            async let locationsTask = APIClient.shared.listCompanyLocations(companyId: companyId)
            let company = try await companyTask
            let memberList = (try? await membersTask) ?? []
            let locations = (try? await locationsTask) ?? []
            await MainActor.run {
                applyCompany(company)
                members = memberList
                defaultLocationName = locations.first?.name
                loading = false
            }
        } catch {
            await MainActor.run {
                loadError = "Could not load company"
                loading = false
            }
        }
    }

    private func applyCompany(_ company: APIClient.CompanyDetailPayload) {
        version = company.version
        displayName = company.displayName
        legalName = company.legalName ?? ""
        taxIdentifier = company.taxIdentifier ?? ""
        if let type = company.companyType, !type.isEmpty { companyType = type }
        let profile = company.profileJson ?? [:]
        industry = profile["industry"]?.value as? String ?? ""
        if let size = profile["companySize"]?.value as? String, !size.isEmpty { companySize = size }
        if let cur = profile["currency"]?.value as? String, !cur.isEmpty { currency = cur }
        financialYear = (profile["financialYear"]?.value as? String)
            ?? (profile["fyCycle"]?.value as? String)
            ?? "—"
        smallShop = BusinessAudience.isSmallShop(profile[BusinessAudience.prefKey]?.value as? String)
        industryTemplateId = profile[IndustryTemplateCatalog.profileKey]?.value as? String
        let settings = companyNestedMap(profile["settings"])
        let modules = settings["modules"] as? [String: Any] ?? [:]
        moduleToggles = Dictionary(uniqueKeysWithValues: moduleToggleDefs.map { def in
            (def.key, companyBool(modules[def.key], default: true))
        })
        CompanyModules.saveModules(companyId: companyId, modules: moduleToggles)
        IndustryTemplateCatalog.rehydrateHubHint(companyId: companyId, profileJson: company.profileJson)
        let alertsMap = settings["alerts"] as? [String: Any] ?? [:]
        alertToggles = Dictionary(uniqueKeysWithValues: alertToggleDefs.map { (_, key) in
            (key, companyBool(alertsMap[key], default: true))
        })
        memoryOn = companyBool(settings["businessMemoryEnabled"], default: true)
    }

    private func saveProfile() {
        guard let version, !companyId.isEmpty, !savingProfile else { return }
        savingProfile = true
        profileSaveError = nil
        profileSaved = false
        Task {
            do {
                var profileJson: [String: Any] = ["companySize": companySize, "currency": currency]
                if !industry.isEmpty { profileJson["industry"] = industry }
                let body: [String: Any] = [
                    "expectedVersion": version,
                    "displayName": displayName.trimmingCharacters(in: .whitespaces),
                    "legalName": legalName.trimmingCharacters(in: .whitespaces).isEmpty
                        ? displayName.trimmingCharacters(in: .whitespaces) : legalName.trimmingCharacters(in: .whitespaces),
                    "taxIdentifier": taxIdentifier.trimmingCharacters(in: .whitespaces),
                    "companyType": companyType,
                    "profileJson": profileJson,
                ]
                let updated = try await APIClient.shared.patchCompany(companyId: companyId, body: body)
                await MainActor.run {
                    applyCompany(updated)
                    profileSaved = true
                    savingProfile = false
                }
            } catch {
                await MainActor.run {
                    profileSaveError = "Could not save"
                    savingProfile = false
                }
            }
        }
    }

    private func patchSettings(_ profilePatch: [String: Any], rollback: @escaping () -> Void) {
        guard let version, !settingsPatchBusy else { return }
        settingsPatchBusy = true
        settingsPatchError = nil
        Task {
            defer { Task { @MainActor in settingsPatchBusy = false } }
            do {
                let updated = try await APIClient.shared.patchCompany(
                    companyId: companyId,
                    body: ["expectedVersion": version, "profileJson": profilePatch]
                )
                await MainActor.run { applyCompany(updated) }
            } catch {
                await MainActor.run {
                    rollback()
                    settingsPatchError = "Could not update modules"
                }
            }
        }
    }

    private func moduleBinding(for key: String) -> Binding<Bool> {
        Binding(
            get: { moduleToggles[key] ?? true },
            set: { newValue in
                let previous = moduleToggles
                moduleToggles[key] = newValue
                patchSettings(["settings": ["modules": [key: newValue]]]) { moduleToggles = previous }
            }
        )
    }

    private func alertBinding(for key: String) -> Binding<Bool> {
        Binding(
            get: { alertToggles[key] ?? true },
            set: { newValue in
                let previous = alertToggles
                alertToggles[key] = newValue
                patchSettings(["settings": ["alerts": [key: newValue]]]) { alertToggles = previous }
            }
        )
    }

    private var memoryBinding: Binding<Bool> {
        Binding(
            get: { memoryOn },
            set: { newValue in
                let previous = memoryOn
                memoryOn = newValue
                patchSettings(["settings": ["businessMemoryEnabled": newValue]]) { memoryOn = previous }
            }
        )
    }

    private func settingsCard<Content: View>(_ title: String, note: String? = nil, @ViewBuilder content: () -> Content) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(title).font(.system(size: 15, weight: .semibold)).foregroundStyle(Color(hex: "#F1F5F9"))
            if let note {
                Text(note).font(.system(size: 12)).foregroundStyle(Color(hex: "#64748B"))
            }
            content()
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(16)
        .background(Color.white.opacity(0.06))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private func valueRow(_ label: String, _ value: String, muted: Bool = false) -> some View {
        HStack {
            Text(label).font(.system(size: 13)).foregroundStyle(Color(hex: "#CBD5E1"))
            Spacer()
            Text(value)
                .font(.system(size: 13, weight: .medium))
                .foregroundStyle(muted ? Color(hex: "#64748B") : Color(hex: "#F1F5F9"))
        }
    }

    private func settingsTextField(_ label: String, text: Binding<String>) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(label).font(.system(size: 13)).foregroundStyle(Color(hex: "#CBD5E1"))
            TextField("", text: text)
                .padding(10)
                .background(Color(hex: "#0C0F15"))
                .overlay(RoundedRectangle(cornerRadius: 10).stroke(Color.white.opacity(0.10)))
                .clipShape(RoundedRectangle(cornerRadius: 10))
                .foregroundStyle(Color(hex: "#F1F5F9"))
        }
    }

    private func settingsMenuField(_ label: String, selection: Binding<String>, options: [String], placeholder: String? = nil) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(label).font(.system(size: 13)).foregroundStyle(Color(hex: "#CBD5E1"))
            Menu {
                ForEach(options, id: \.self) { option in
                    Button(option) { selection.wrappedValue = option }
                }
            } label: {
                HStack {
                    Text(selection.wrappedValue.isEmpty ? (placeholder ?? "Select") : selection.wrappedValue)
                        .foregroundStyle(selection.wrappedValue.isEmpty ? Color(hex: "#64748B") : Color(hex: "#F1F5F9"))
                    Spacer()
                    Image(systemName: "chevron.down").foregroundStyle(Color(hex: "#64748B"))
                }
                .padding(10)
                .background(Color(hex: "#0C0F15"))
                .overlay(RoundedRectangle(cornerRadius: 10).stroke(Color.white.opacity(0.10)))
                .clipShape(RoundedRectangle(cornerRadius: 10))
            }
        }
    }
}

private func companyNestedMap(_ value: AnyDecodable?) -> [String: Any] {
    guard let dict = value?.value as? [String: Any] else { return [:] }
    return dict
}

private func companyBool(_ value: Any?, default defaultValue: Bool) -> Bool {
    guard let raw = value else { return defaultValue }
    if let b = raw as? Bool { return b }
    if let n = raw as? Int { return n != 0 }
    if let s = raw as? String { return s.lowercased() == "true" }
    return defaultValue
}

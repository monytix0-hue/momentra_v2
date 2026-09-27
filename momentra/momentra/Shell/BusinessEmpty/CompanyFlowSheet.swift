import SwiftUI

private enum CompanySheetPage {
    case switcher
    case settings
    case create
}

private let companyIndustries = [
    "Technology & Software",
    "E-commerce",
    "Retail",
    "Services",
    "Manufacturing",
    "Other",
]

private let companySizes = ["1-10", "11-50", "51-200", "201-500", "500+"]

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

    @State private var page: CompanySheetPage
    @State private var settingsName = ""

    init(
        companies: [CompanySummary],
        selectedCompanyId: String?,
        startOnCreate: Bool = false,
        startOnSettings: Bool = false,
        onClose: @escaping () -> Void,
        onSelect: @escaping (CompanySummary) -> Void,
        onCreated: @escaping (CompanySummary) -> Void,
        onOpenLocations: @escaping () -> Void = {}
    ) {
        self.companies = companies
        self.selectedCompanyId = selectedCompanyId
        self.startOnCreate = startOnCreate
        self.startOnSettings = startOnSettings
        self.onClose = onClose
        self.onSelect = onSelect
        self.onCreated = onCreated
        self.onOpenLocations = onOpenLocations
        let initial: CompanySheetPage = startOnCreate ? .create : (startOnSettings ? .settings : .switcher)
        _page = State(initialValue: initial)
        let selected = companies.first { $0.companyId == selectedCompanyId }
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
        BusinessSettingsPage(companyName: settingsName, onClose: { page = .switcher }, onOpenLocations: onOpenLocations)
    }
}

private struct CompanyCreateForm: View {
    var onCancel: () -> Void
    var onCreated: (CompanySummary) -> Void

    @State private var name = ""
    @State private var industry = ""
    @State private var size = "1-10"
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
                var profile: [String: String] = ["companySize": size]
                if !industry.isEmpty { profile["industry"] = industry }
                let created = try await APIClient.shared.createCompany(
                    displayName: trimmed,
                    legalName: trimmed,
                    timezone: TimeZone.current.identifier,
                    profileJson: profile
                )
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
    var companyName: String
    var onClose: () -> Void
    var onOpenLocations: () -> Void = {}

    @State private var momentUses: [(String, Bool)] = [
        ("Team & Work", true),
        ("Daily Business", true),
        ("Money & Cash Flow", true),
        ("Projects & Tasks", false),
        ("Events & Plans", false),
        ("Suppliers & Vendors", true),
    ]
    @State private var alerts: [(String, Bool)] = [
        ("Purchase & Expense Alerts", true),
        ("Approval Requests", true),
        ("Issue / Risk Alerts", true),
        ("Payment Reminders", true),
        ("Important Business Updates", true),
    ]
    @State private var memoryOn = true

    private var name: String { companyName.isEmpty ? "Company" : companyName }

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            sheetHeader("Company Settings", subtitle: name, close: onClose)
            settingsCard("Business Identity") {
                valueRow("Business", name)
            }
            settingsCard("Business Profile") {
                valueRow("Business Name", name)
            }
            settingsCard("Business Details", note: "Optional formal business information.") {
                valueRow("Legal Business Name", "—")
                valueRow("Business Registration Type", "—")
                valueRow("GSTIN / Tax ID", "—")
                valueRow("Registration Number", "Optional")
            }
            settingsCard("Business Accounts") {
                Text("No accounts yet").font(.system(size: 13)).foregroundStyle(Color(hex: "#64748B"))
            }
            settingsCard("Money Settings") {
                valueRow("Currency", "—")
                valueRow("Financial Year", "—")
                valueRow("Default Expense Account", "—")
                valueRow("Default Location", "—")
            }
            settingsCard("Locations") {
                Button(action: onOpenLocations) {
                    Text("Manage locations")
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundStyle(Color(hex: "#F1F5F9"))
                        .frame(maxWidth: .infinity, alignment: .leading)
                }
                .buttonStyle(.plain)
            }
            settingsCard("Team & Roles") {
                Text("No team members yet").font(.system(size: 13)).foregroundStyle(Color(hex: "#64748B"))
            }
            settingsCard("What do you use Momentra for?", note: "Turn on only the business moment types you want to use.") {
                ForEach(momentUses.indices, id: \.self) { index in
                    Toggle(momentUses[index].0, isOn: Binding(
                        get: { momentUses[index].1 },
                        set: { value in
                            var next = momentUses
                            next[index].1 = value
                            momentUses = next
                        }
                    ))
                    .font(.system(size: 14))
                    .foregroundStyle(Color(hex: "#F1F5F9"))
                    .tint(Color(hex: "#818CF8"))
                }
            }
            settingsCard("Notifications & Approvals") {
                ForEach(alerts.indices, id: \.self) { index in
                    Toggle(alerts[index].0, isOn: Binding(
                        get: { alerts[index].1 },
                        set: { value in
                            var next = alerts
                            next[index].1 = value
                            alerts = next
                        }
                    ))
                    .font(.system(size: 14))
                    .foregroundStyle(Color(hex: "#F1F5F9"))
                    .tint(Color(hex: "#818CF8"))
                }
            }
            settingsCard("Business Memory & Data") {
                Toggle("Business Memory", isOn: $memoryOn)
                    .font(.system(size: 14))
                    .foregroundStyle(Color(hex: "#F1F5F9"))
                    .tint(Color(hex: "#818CF8"))
                valueRow("Archived Moments", "—")
                valueRow("Export Business Data", "—")
                valueRow("Data Retention", "—")
            }
            settingsCard("Plan & Usage") {
                Text("Plan details aren't available yet")
                    .font(.system(size: 13))
                    .foregroundStyle(Color(hex: "#64748B"))
            }
            settingsCard("Business Management", note: "These actions affect your business setup and access.") {
                Text("Transfer Ownership").font(.system(size: 14, weight: .semibold)).foregroundStyle(Color(hex: "#F1F5F9"))
                Text("Archive Business").font(.system(size: 14, weight: .semibold)).foregroundStyle(Color(hex: "#F1F5F9"))
                Text("Deactivate Business").font(.system(size: 14, weight: .semibold)).foregroundStyle(Color(hex: "#F1F5F9"))
            }
        }
        .padding(.horizontal, 16)
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

    private func valueRow(_ label: String, _ value: String) -> some View {
        HStack {
            Text(label).font(.system(size: 13)).foregroundStyle(Color(hex: "#CBD5E1"))
            Spacer()
            Text(value).font(.system(size: 13, weight: .medium)).foregroundStyle(Color(hex: "#F1F5F9"))
        }
    }
}

import SwiftUI

enum BusinessGapPage: Equatable {
    case moments, companyPulse, finance, vendor
    case momentsSettings, milestone, visibility
    case locationPicker, locationDashboard, locationConfig, inheritance
}

struct PendingBusinessActivation: Equatable {
    let momentId: String
    let title: String
    let momentTypeCode: String?
    let status: String
}

private let gapBg = Color(hex: "#0C0F15")
private let gapCard = Color(hex: "#161B26")
private let gapAccent = Color(hex: "#818CF8")
private let gapText = Color(hex: "#F1F5F9")
private let gapMuted = Color(hex: "#64748B")

private func familyLabel(_ code: String?) -> String {
    let c = (code ?? "").uppercased()
    if c.contains("TEAM") { return "Team & Work" }
    if c.contains("RUNWAY") { return "Money & Cash Flow" }
    if c.contains("OPERATIONS") { return "Daily Business" }
    return c.isEmpty ? "Business" : c.replacingOccurrences(of: "_", with: " ")
}

struct BusinessGapHost: View {
    let page: BusinessGapPage
    let companyId: String?
    let companyName: String
    let moments: [MomentSummary]
    @Binding var locationName: String?
    var onSelectMoment: (MomentSummary) -> Void
    var onCreateMoment: () -> Void
    var onClose: () -> Void
    var onPage: (BusinessGapPage) -> Void
    var onBackToSettings: () -> Void

    var body: some View {
        Group {
            switch page {
            case .moments:
                BusinessMomentsDirectory(
                    moments: moments,
                    companyName: companyName,
                    onBack: onClose,
                    onSelect: onSelectMoment,
                    onCreate: onCreateMoment,
                    onSettings: { onPage(.momentsSettings) },
                    onPulse: { onPage(.companyPulse) }
                )
            case .companyPulse:
                BusinessCompanyPulseScreen(companyName: companyName, moments: moments, onBack: { onPage(.moments) }, onOpen: onSelectMoment)
            case .finance:
                BusinessFinanceScreen(companyName: companyName, moments: moments, onBack: onClose)
            case .vendor:
                VendorOperationsScreen(companyId: companyId, onBack: onClose)
            case .momentsSettings:
                MomentsSettingsScreen(onBack: { onPage(.moments) }, onMilestone: { onPage(.milestone) }, onVisibility: { onPage(.visibility) })
            case .milestone:
                MilestoneSettingsScreen(onBack: { onPage(.momentsSettings) })
            case .visibility:
                VisibilitySettingsScreen(onBack: { onPage(.momentsSettings) })
            case .locationPicker, .locationDashboard, .locationConfig, .inheritance:
                BusinessLocationFlow(
                    companyId: companyId,
                    page: page,
                    locationName: $locationName,
                    onPage: onPage,
                    onBackToSettings: onBackToSettings
                )
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(gapBg)
    }
}

private struct BusinessMomentsDirectory: View {
    let moments: [MomentSummary]
    let companyName: String
    var onBack: () -> Void
    var onSelect: (MomentSummary) -> Void
    var onCreate: () -> Void
    var onSettings: () -> Void
    var onPulse: () -> Void
    @State private var tab = "Active"

    private var filtered: [MomentSummary] {
        moments.filter { moment in
            let status = moment.status.uppercased()
            switch tab {
            case "Upcoming": return status == "UPCOMING" || status == "PLANNED" || status == "SCHEDULED"
            case "Completed": return moment.isCompletedStatus
            default: return moment.isActiveStatus
            }
        }
    }

    var body: some View {
        GapPage(title: "Business Moments", subtitle: companyName, onBack: onBack, trailing: onSettings) {
            HStack {
                ForEach(["Active", "Upcoming", "Completed"], id: \.self) { label in
                    Button(label) { tab = label }
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundStyle(tab == label ? gapBg : gapText)
                        .padding(.horizontal, 12).padding(.vertical, 8)
                        .background(tab == label ? gapAccent : gapCard, in: Capsule())
                }
            }
            Button("Company pulse", action: onPulse).foregroundStyle(gapAccent)
            if filtered.isEmpty {
                Text("No moments in this tab.").foregroundStyle(gapMuted)
            }
            ForEach(filtered) { moment in
                Button { onSelect(moment) } label: {
                    VStack(alignment: .leading, spacing: 4) {
                        Text(familyLabel(moment.momentTypeCode)).font(.system(size: 11, weight: .bold)).foregroundStyle(gapAccent)
                        Text(moment.title.isEmpty ? "Untitled moment" : moment.title).foregroundStyle(gapText)
                        Text(moment.status).font(.system(size: 12)).foregroundStyle(gapMuted)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(16)
                    .background(gapCard, in: RoundedRectangle(cornerRadius: 16))
                }
                .buttonStyle(.plain)
            }
            Button("+ Create Moment", action: onCreate)
                .font(.system(size: 14, weight: .bold))
                .foregroundStyle(gapBg)
                .padding(.horizontal, 16).padding(.vertical, 12)
                .background(gapAccent, in: Capsule())
        }
    }
}

private struct BusinessCompanyPulseScreen: View {
    let companyName: String
    let moments: [MomentSummary]
    var onBack: () -> Void
    var onOpen: (MomentSummary) -> Void
    @State private var lines: [(MomentSummary, String?, Int, String?)] = []
    @State private var loading = true

    var body: some View {
        GapPage(title: "Business Pulse", subtitle: companyName, onBack: onBack) {
            if loading { ProgressView().tint(gapAccent) }
            Text("Active \(moments.filter(\.isActiveStatus).count)").foregroundStyle(gapText)
            Text(lines.isEmpty ? "Needs attention —" : "Needs attention \(lines.map(\.2).reduce(0, +))").foregroundStyle(gapText)
            ForEach(lines, id: \.0.momentId) { line in
                Button { onOpen(line.0) } label: {
                    VStack(alignment: .leading) {
                        Text(familyLabel(line.0.momentTypeCode)).foregroundStyle(gapText)
                        Text([line.1.map { "Health \($0)" }, line.3.map { "Runway \($0)" }].compactMap { $0 }.joined(separator: " · ").ifEmpty("—"))
                            .foregroundStyle(gapMuted)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(16).background(gapCard, in: RoundedRectangle(cornerRadius: 16))
                }.buttonStyle(.plain)
            }
            Text("No insights yet").foregroundStyle(gapMuted).padding(16).frame(maxWidth: .infinity, alignment: .leading).background(gapCard, in: RoundedRectangle(cornerRadius: 16))
        }
        .task(id: moments.map(\.momentId).joined()) {
            loading = true
            var next: [(MomentSummary, String?, Int, String?)] = []
            for moment in moments.prefix(12) {
                let pulse = (try? await APIClient.shared.getBusinessPulse(momentId: moment.momentId))?.payload
                next.append((moment, pulse?.financialHealthScore, pulse?.attentionCount ?? 0, pulse?.runwayMonths))
            }
            lines = next
            loading = false
        }
    }
}

private struct BusinessFinanceScreen: View {
    let companyName: String
    let moments: [MomentSummary]
    var onBack: () -> Void
    @State private var expenses = "—"
    @State private var revenue = "—"
    @State private var outstanding = "—"
    @State private var activity: [APIClient.ActivityItemPayload] = []

    var body: some View {
        GapPage(title: "Finance", subtitle: companyName, onBack: onBack) {
            Text("Expenses \(expenses)").foregroundStyle(gapText)
            Text("Revenue \(revenue)").foregroundStyle(gapText)
            Text("Outstanding \(outstanding)").foregroundStyle(gapText)
            Text("Cash balance —").foregroundStyle(gapMuted)
            if activity.isEmpty { Text("No activity yet.").foregroundStyle(gapMuted) }
            ForEach(activity) { item in
                VStack(alignment: .leading) {
                    Text(item.title).foregroundStyle(gapText)
                    Text(item.occurredAt).font(.system(size: 12)).foregroundStyle(gapMuted)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(12).background(gapCard, in: RoundedRectangle(cornerRadius: 12))
            }
        }
        .task(id: moments.map(\.momentId).joined()) {
            var totals: [APIClient.BusinessFinanceTotalsPayload] = []
            var items: [APIClient.ActivityItemPayload] = []
            for moment in moments.prefix(12) {
                if let rows = (try? await APIClient.shared.getBusinessFinance(momentId: moment.momentId))?.payload?.totals {
                    totals.append(contentsOf: rows)
                }
                if let rows = try? await APIClient.shared.listBusinessActivity(momentId: moment.momentId, limit: 5) {
                    items.append(contentsOf: rows)
                }
            }
            let currency = totals.first?.currencyCode
            let same = currency != nil && totals.allSatisfy { $0.currencyCode == currency }
            func sum(_ pick: (APIClient.BusinessFinanceTotalsPayload) -> String?) -> String {
                guard same, let currency else { return "—" }
                let values = totals.compactMap { pick($0).flatMap(Double.init) }
                guard !values.isEmpty else { return "—" }
                return "\(currency) \(values.reduce(0, +))"
            }
            expenses = sum { $0.expenseTotal }
            revenue = sum { $0.revenueTotal }
            outstanding = sum { $0.invoiceOutstandingTotal }
            activity = Array(items.prefix(8))
        }
    }
}

private struct SessionContract: Identifiable { let id: String; let vendorId: String; let name: String }
private struct SessionSla: Identifiable { let id: String; let vendorId: String; let name: String }

private struct VendorOperationsScreen: View {
    let companyId: String?
    var onBack: () -> Void
    @State private var vendors: [APIClient.VendorItem] = []
    @State private var sheet: String?
    @State private var contracts: [SessionContract] = []
    @State private var slas: [SessionSla] = []

    var body: some View {
        GapPage(title: "Vendor Operations", subtitle: "Supply chain contracts and SLAs", onBack: onBack) {
            if vendors.isEmpty { Text("No vendors yet.").foregroundStyle(gapMuted) }
            ForEach(vendors, id: \.vendorId) { vendor in
                VStack(alignment: .leading) {
                    Text(vendor.name).foregroundStyle(gapText)
                    Text(vendor.status ?? "—").font(.system(size: 12)).foregroundStyle(gapMuted)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(16).background(gapCard, in: RoundedRectangle(cornerRadius: 16))
            }
            ForEach(["Add Contract", "Define SLA", "Record SLA Check"], id: \.self) { label in
                Button(label) { sheet = label }
                    .foregroundStyle(gapBg)
                    .padding(12)
                    .background(gapAccent, in: RoundedRectangle(cornerRadius: 12))
            }
        }
        .task(id: companyId) { await reload() }
        .sheet(item: Binding(get: { sheet.map(SheetToken.init) }, set: { sheet = $0?.value })) { token in
            VendorSheet(kind: token.value, companyId: companyId ?? "", vendors: vendors, contracts: contracts, slas: slas) { contract in
                contracts.append(contract)
                sheet = nil
                Task { await reload() }
            } onSla: { sla in
                slas.append(sla)
                sheet = nil
            } onCheck: {
                sheet = nil
            }
        }
    }

    private func reload() async {
        guard let companyId, !companyId.isEmpty else { vendors = []; return }
        vendors = (try? await APIClient.shared.listCompanyVendors(companyId: companyId).items) ?? []
    }
}

private struct SheetToken: Identifiable { let value: String; var id: String { value } }

private struct VendorSheet: View {
    let kind: String
    let companyId: String
    let vendors: [APIClient.VendorItem]
    let contracts: [SessionContract]
    let slas: [SessionSla]
    var onContract: (SessionContract) -> Void
    var onSla: (SessionSla) -> Void
    var onCheck: () -> Void
    @State private var vendorId = ""
    @State private var name = ""
    @State private var reference = ""
    @State private var start = ""
    @State private var end = ""
    @State private var value = ""
    @State private var currency = ""
    @State private var metric = ""
    @State private var period = ""
    @State private var comparator = ">="
    @State private var target = ""
    @State private var unit = ""
    @State private var contractId: String?
    @State private var slaId: String?
    @State private var observedAt = ""
    @State private var observed = ""
    @State private var result = "MET"
    @State private var note = ""
    @State private var error: String?
    @State private var showDate = false
    @State private var dateField = "start"
    @State private var showNote = false
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 10) {
                Text(kind).font(.title3.bold()).foregroundStyle(gapText)
                if vendors.isEmpty { Text("No vendors yet.").foregroundStyle(gapMuted) }
                ForEach(vendors, id: \.vendorId) { vendor in
                    Button(vendor.name) { vendorId = vendor.vendorId }
                        .foregroundStyle(vendor.vendorId == vendorId ? gapAccent : gapText)
                }
                if kind == "Add Contract" {
                    field("Contract Name", $name)
                    field("Reference", $reference)
                    Button(start.isEmpty ? "Start date" : start) { dateField = "start"; showDate = true }.foregroundStyle(gapAccent)
                    Button(end.isEmpty ? "End date" : end) { dateField = "end"; showDate = true }.foregroundStyle(gapAccent)
                    field("Value", $value)
                    field("Currency", $currency)
                } else if kind == "Define SLA" {
                    let mine = contracts.filter { $0.vendorId == vendorId }
                    if mine.isEmpty { Text("No contracts for this vendor yet.").foregroundStyle(gapMuted) }
                    ForEach(mine) { contract in
                        Button(contract.name) { contractId = contract.id }.foregroundStyle(contract.id == contractId ? gapAccent : gapText)
                    }
                    field("SLA Name", $name)
                    field("Metric", $metric)
                    field("Period", $period)
                    field("Comparator", $comparator)
                    field("Target", $target)
                    field("Unit", $unit)
                } else {
                    if slas.isEmpty { Text("Create an SLA in this hub before recording a check.").foregroundStyle(gapMuted) }
                    ForEach(slas) { sla in
                        Button(sla.name) { slaId = sla.id }.foregroundStyle(sla.id == slaId ? gapAccent : gapText)
                    }
                    Button(observedAt.isEmpty ? "Observation date" : observedAt) { dateField = "observed"; showDate = true }.foregroundStyle(gapAccent)
                    field("Observed value", $observed)
                    HStack {
                        ForEach(["MET", "BREACHED", "WAIVED"], id: \.self) { label in
                            Button(label) { result = label }.foregroundStyle(result == label ? gapBg : gapText).padding(8).background(result == label ? gapAccent : gapCard, in: Capsule())
                        }
                    }
                    Button(note.isEmpty ? "Add a note" : note) { showNote = true }.foregroundStyle(gapAccent)
                }
                if let error { Text(error).foregroundStyle(gapMuted) }
                Button("Save") { Task { await save() } }
                    .disabled(!canSave)
                    .foregroundStyle(gapBg).padding(12).background(canSave ? gapAccent : gapCard, in: RoundedRectangle(cornerRadius: 12))
                Button("Cancel") { dismiss() }.foregroundStyle(gapMuted)
            }
            .padding(20)
        }
        .background(gapBg)
        .sheet(isPresented: $showDate) {
            BusinessDateSheet { picked in
                if dateField == "end" { end = picked }
                else if kind == "Add Contract" { start = picked }
                else { observedAt = picked }
            }
        }
        .sheet(isPresented: $showNote) {
            BusinessNotesSheet(initial: note) { note = $0 }
        }
        .onAppear { vendorId = vendors.first?.vendorId ?? "" }
    }

    private var canSave: Bool {
        guard !companyId.isEmpty, !vendorId.isEmpty else { return false }
        if kind == "Add Contract" { return !name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty }
        if kind == "Define SLA" {
            return !name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty && !metric.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
        }
        return slaId != nil
    }

    private func save() async {
        guard !companyId.isEmpty, !vendorId.isEmpty else { error = "Choose a vendor."; return }
        do {
            if kind == "Add Contract" {
                let created = try await APIClient.shared.createVendorContract(companyId: companyId, vendorId: vendorId, contractName: name, contractReference: reference.nilIfBlank, startDate: start.nilIfBlank, endDate: end.nilIfBlank, contractValue: value.nilIfBlank, currencyCode: currency.nilIfBlank)
                onContract(SessionContract(id: created.vendorContractId, vendorId: vendorId, name: name))
            } else if kind == "Define SLA" {
                let created = try await APIClient.shared.createSlaDefinition(companyId: companyId, vendorId: vendorId, name: name, metricCode: metric, comparator: comparator, targetValue: Double(target), unitCode: unit.nilIfBlank, measurementPeriod: period.nilIfBlank, vendorContractId: contractId)
                onSla(SessionSla(id: created.slaDefinitionId, vendorId: vendorId, name: name))
            } else if let slaId {
                _ = try await APIClient.shared.createSlaCheck(companyId: companyId, slaDefinitionId: slaId, result: result, observedValue: Double(observed), note: note.nilIfBlank)
                onCheck()
            }
        } catch {
            self.error = error.localizedDescription
        }
    }

    private func field(_ label: String, _ text: Binding<String>) -> some View {
        VStack(alignment: .leading) {
            Text(label).font(.system(size: 12)).foregroundStyle(gapMuted)
            TextField(label, text: text).foregroundStyle(gapText).padding(10).background(gapCard, in: RoundedRectangle(cornerRadius: 10))
        }
    }
}

private struct BusinessLocationFlow: View {
    let companyId: String?
    let page: BusinessGapPage
    @Binding var locationName: String?
    var onPage: (BusinessGapPage) -> Void
    var onBackToSettings: () -> Void
    @State private var locations: [APIClient.CompanyLocationItemPayload] = []
    @State private var selected: APIClient.CompanyLocationItemPayload?
    @State private var adding = false

    var body: some View {
        let current = selected ?? locations.first { $0.name == locationName }
        Group {
            if page == .inheritance {
                GapPage(title: "Inheritance", subtitle: "Locations inherit company defaults.", onBack: { onPage(.locationConfig) }) {
                    Text("Currency, budget, and reporting stay with the company. This page does not change those rules.").foregroundStyle(gapMuted)
                }
            } else if page == .locationDashboard || page == .locationConfig {
                GapPage(title: page == .locationDashboard ? "Location" : "Location Config", subtitle: current?.name, onBack: { onPage(page == .locationDashboard ? .locationPicker : .locationDashboard) }) {
                    Text(current?.name ?? "—").foregroundStyle(gapText)
                    Text(current?.addressText ?? "—").foregroundStyle(gapMuted)
                    Text(current?.status ?? "—").foregroundStyle(gapMuted)
                    Button(page == .locationDashboard ? "Configuration" : "Inheritance") {
                        onPage(page == .locationDashboard ? .locationConfig : .inheritance)
                    }.foregroundStyle(gapAccent)
                }
            } else {
                GapPage(title: "Locations", onBack: onBackToSettings) {
                    if locations.isEmpty { Text("No locations yet.").foregroundStyle(gapMuted) }
                    ForEach(locations) { location in
                        Button {
                            selected = location
                            locationName = location.name
                            onPage(.locationDashboard)
                        } label: {
                            VStack(alignment: .leading) {
                                Text(location.name).foregroundStyle(gapText)
                                Text(location.addressText ?? "—").foregroundStyle(gapMuted)
                            }
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .padding(16).background(gapCard, in: RoundedRectangle(cornerRadius: 16))
                        }.buttonStyle(.plain)
                    }
                    Button("Add Location") { adding = true }.foregroundStyle(gapBg).padding(12).background(gapAccent, in: RoundedRectangle(cornerRadius: 12))
                }
            }
        }
        .task(id: companyId) {
            guard let companyId, !companyId.isEmpty else { return }
            locations = (try? await APIClient.shared.listCompanyLocations(companyId: companyId)) ?? []
        }
        .sheet(isPresented: $adding) {
            AddLocationSheet(companyId: companyId ?? "") { created in
                locations.append(created)
                selected = created
                locationName = created.name
                adding = false
            }
        }
    }
}

private struct AddLocationSheet: View {
    let companyId: String
    var onCreated: (APIClient.CompanyLocationItemPayload) -> Void
    @State private var name = ""
    @State private var address = ""
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Add Location").font(.title3.bold()).foregroundStyle(gapText)
            TextField("Name", text: $name).foregroundStyle(gapText)
            TextField("Address", text: $address).foregroundStyle(gapText)
            Button("Save") {
                Task {
                    let created = try? await APIClient.shared.createLocation(companyId: companyId, name: name, addressText: address.nilIfBlank, timezone: TimeZone.current.identifier)
                    if let created {
                        onCreated(APIClient.CompanyLocationItemPayload(locationId: created.locationId, name: created.name, addressText: address.nilIfBlank, timezone: TimeZone.current.identifier, status: "ACTIVE"))
                    }
                }
            }.foregroundStyle(gapBg).padding(12).background(gapAccent, in: RoundedRectangle(cornerRadius: 12))
            Button("Cancel") { dismiss() }.foregroundStyle(gapMuted)
        }
        .padding(20).frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top).background(gapBg)
    }
}

private struct MomentsSettingsScreen: View {
    var onBack: () -> Void
    var onMilestone: () -> Void
    var onVisibility: () -> Void
    @State private var track = true
    @State private var timeline = true
    @State private var decisions = false
    var body: some View {
        GapPage(title: "Moments Settings", subtitle: "Local preferences stay on this page.", onBack: onBack) {
            Toggle("Auto-track milestones", isOn: $track).tint(gapAccent)
            Toggle("Enable team timeline", isOn: $timeline).tint(gapAccent)
            Toggle("Show decision log", isOn: $decisions).tint(gapAccent)
            Text("Export moments data").foregroundStyle(gapMuted)
            Text("Clear history").foregroundStyle(gapMuted)
            Text("Retention policy").foregroundStyle(gapMuted)
            Button("Milestone tracking", action: onMilestone).foregroundStyle(gapAccent)
            Button("Visibility", action: onVisibility).foregroundStyle(gapAccent)
        }
    }
}

private struct MilestoneSettingsScreen: View {
    var onBack: () -> Void
    @State private var mode = "Hybrid"
    var body: some View {
        GapPage(title: "Milestone Tracking", onBack: onBack) {
            ForEach(["Automatic", "Manual", "Hybrid"], id: \.self) { option in
                Button(option) { mode = option }.foregroundStyle(mode == option ? gapAccent : gapText)
            }
        }
    }
}

private struct VisibilitySettingsScreen: View {
    var onBack: () -> Void
    @State private var owners = true
    @State private var team = true
    @State private var vendors = false
    var body: some View {
        GapPage(title: "Visibility", onBack: onBack) {
            Toggle("Owners", isOn: $owners)
            Toggle("Team", isOn: $team)
            Toggle("Vendors", isOn: $vendors)
        }
    }
}

struct BusinessDateSheet: View {
    var onConfirm: (String) -> Void
    @State private var date = Date()
    @Environment(\.dismiss) private var dismiss
    var body: some View {
        VStack {
            DatePicker("Date", selection: $date, displayedComponents: .date).datePickerStyle(.graphical)
            Button("Use date") {
                onConfirm(ISO8601DateFormatter().string(from: date).prefix(10).description)
                dismiss()
            }
            Button("Cancel") { dismiss() }
        }.padding()
    }
}

struct BusinessNotesSheet: View {
    var initial: String
    var onConfirm: (String) -> Void
    @State private var text = ""
    @Environment(\.dismiss) private var dismiss
    var body: some View {
        VStack(alignment: .leading) {
            Text("Notes").font(.title3.bold())
            TextField("Note", text: $text, axis: .vertical).lineLimit(4...8)
            Button("Save") { onConfirm(text.trimmingCharacters(in: .whitespacesAndNewlines)); dismiss() }
            Button("Cancel") { dismiss() }
        }.padding().onAppear { text = initial }
    }
}

struct BusinessAddPeopleSheet: View {
    var onConfirm: (String) -> Void
    @State private var name = ""
    @Environment(\.dismiss) private var dismiss
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Add people").font(.title3.bold()).foregroundStyle(gapText)
            TextField("Name or email", text: $name).foregroundStyle(gapText)
            Button("Add") { onConfirm(name.trimmingCharacters(in: .whitespacesAndNewlines)); dismiss() }
                .disabled(name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
            Button("Cancel") { dismiss() }.foregroundStyle(gapMuted)
        }.padding(20).frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top).background(gapBg)
    }
}

struct BusinessActivationSuccess: View {
    let title: String
    var onContinue: () -> Void
    var body: some View {
        VStack(spacing: 12) {
            Text("Moment ready").font(.title2.bold()).foregroundStyle(gapText)
            Text(title.isEmpty ? "Your business moment is active." : title).foregroundStyle(gapMuted)
            Button("Continue", action: onContinue).foregroundStyle(gapBg).padding(12).background(gapAccent, in: RoundedRectangle(cornerRadius: 12))
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(gapBg)
    }
}

private struct GapPage<Content: View>: View {
    let title: String
    var subtitle: String? = nil
    var onBack: (() -> Void)? = nil
    var trailing: (() -> Void)? = nil
    @ViewBuilder var content: () -> Content
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                if let onBack { Button("Back", action: onBack).foregroundStyle(gapText) }
                VStack(alignment: .leading) {
                    Text(title).font(.headline).foregroundStyle(gapText)
                    if let subtitle, !subtitle.isEmpty { Text(subtitle).font(.caption).foregroundStyle(gapMuted) }
                }
                Spacer()
                if let trailing { Button("Settings", action: trailing).foregroundStyle(gapMuted) }
            }
            ScrollView { VStack(alignment: .leading, spacing: 12) { content() } }
        }
        .padding(16)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
        .background(gapBg)
    }
}

private extension String {
    var nilIfBlank: String? { trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? nil : self }
    func ifEmpty(_ fallback: String) -> String { isEmpty ? fallback : self }
}

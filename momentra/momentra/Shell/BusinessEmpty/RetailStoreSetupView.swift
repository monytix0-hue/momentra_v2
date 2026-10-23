import SwiftUI

struct RetailStoreSetupView: View {
    let subtypes: [OperatingSubtype]
    let subtype: OperatingSubtype
    let ownerName: String
    @ObservedObject var createModel: MomentCreateModel
    var onSubtype: (OperatingSubtype) -> Void
    var onClose: () -> Void
    var onActivate: ([String: Any], String) -> Void
    var initialName: String = ""
    var initialCompanyName: String = ""
    var initialGstin: String = ""

    @State private var name = ""
    @State private var companyName = ""
    @State private var companyEdited = false
    @State private var gstin = ""
    @State private var currency = "INR"
    @State private var currencyOpen = false
    @State private var start = Date()
    @State private var end: Date? = nil
    @State private var dateOpen = false
    @State private var editingEnd = false
    @State private var details = ""
    @State private var location = ""
    @State private var invites: [StoreInvite] = []
    @State private var inviting = false
    @State private var inviteName = ""
    @State private var inviteRole = "OPERATIONS_LEAD"
    @State private var policy = "NONE"
    @State private var policyOpen = false
    @State private var selectedChannels: Set<String> = []
    @State private var tradeReach: Set<String> = []
    @State private var parties: [(String, String)] = []
    @State private var partyName = ""
    @State private var partyKind = "SUPPLIER"
    @State private var budget = ""
    @State private var target = ""
    @State private var metric = "REVENUE"
    @State private var metricOpen = false
    @State private var localError: String?

    private var spec: SetupSpec { SetupSpec.forCode(subtype.code) }
    private let currencies = [
        ("INR", "INR · Indian Rupee (₹)"),
        ("USD", "USD · US Dollar"),
        ("EUR", "EUR · Euro"),
        ("GBP", "GBP · British Pound"),
        ("AED", "AED · UAE Dirham"),
    ]
    private let policies = [
        ("NONE", "No approval needed"),
        ("APPROVAL_REQUIRED", "Someone must approve"),
    ]
    private var owner: String { PersonLabel.personName(ownerName, fallback: "You") }
    private var needsApprover: Bool { policy == "APPROVAL_REQUIRED" && !invites.contains { $0.role == "APPROVER" } }
    private var ready: Bool {
        !name.trimmingCharacters(in: .whitespaces).isEmpty &&
        !companyName.trimmingCharacters(in: .whitespaces).isEmpty &&
        (spec.channelsOptional || !selectedChannels.isEmpty) &&
        (Double(budget) ?? 0) > 0 &&
        (Double(target) ?? 0) > 0 &&
        !needsApprover &&
        !createModel.state.submitting
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 24) {
                header
                hero
                modelSelector
                HStack {
                    Text(spec.steps).font(.plusJakarta(size: 12)).foregroundStyle(spec.secondary)
                    Spacer()
                    Image("rs_setup_arrow_down").resizable().frame(width: 16, height: 16)
                }
                pill(spec.badge, fill: spec.draftFill, ink: spec.draftInk)
                basics
                people
                if !spec.partySection.isEmpty { counterparties }
                plan
                review
                if ready {
                    pill("Ready to start", fill: spec.statusFill, ink: spec.statusInk)
                    Text("Name, currency, owner, and sales channels are set. Budget and sales target are filled in.")
                        .font(.plusJakarta(size: 12)).foregroundStyle(spec.secondary)
                }
                if let message = plainSetupError(localError ?? createModel.state.error) {
                    Text(message).font(.plusJakarta(size: 13)).foregroundStyle(Color(hex: "#F87171"))
                }
                Button(createModel.state.submitting ? "Starting…" : spec.activate, action: activate)
                    .buttonStyle(.plain)
                    .font(.plusJakarta(size: 16, weight: .semibold))
                    .foregroundStyle(spec.actionInk)
                    .frame(maxWidth: .infinity)
                    .frame(height: 56)
                    .background(spec.brand.opacity(createModel.state.submitting ? 0.45 : 1))
                    .clipShape(RoundedRectangle(cornerRadius: 14))
                    .disabled(createModel.state.submitting)
                    .accessibilityIdentifier("business.setup.submit")
                Text(spec.footer)
                    .font(.plusJakarta(size: 12)).foregroundStyle(spec.secondary)
            }
            .padding(.horizontal, 17)
            .padding(.vertical, 20)
        }
        .background(spec.bg)
        .onChange(of: subtype.code) { _, _ in
            selectedChannels = selectedChannels.intersection(Set(spec.channels))
            tradeReach = tradeReach.intersection(Set(spec.tradeReach))
            if !spec.partyKinds.contains(where: { $0.0 == partyKind }) {
                partyKind = spec.partyKinds.first?.0 ?? "SUPPLIER"
            }
            if !spec.targetMetrics.contains(metric) {
                metric = spec.targetMetrics.first ?? "REVENUE"
            }
        }
        .onAppear { applyCompanySeed() }
        .onChange(of: initialCompanyName) { _, _ in applyCompanySeed() }
        .onChange(of: initialGstin) { _, _ in applyCompanySeed() }
        .onChange(of: initialName) { _, _ in applyCompanySeed() }
        .sheet(isPresented: $dateOpen) {
            VStack {
                DatePicker(
                    editingEnd ? "End date" : "Tracking start",
                    selection: Binding(
                        get: { editingEnd ? (end ?? start) : start },
                        set: { value in
                            if editingEnd { end = value } else { start = value }
                        }
                    ),
                    displayedComponents: .date
                )
                .datePickerStyle(.graphical)
                .padding()
                if editingEnd {
                    Button("Clear") { end = nil; dateOpen = false }
                }
                Button("Done") { dateOpen = false }.padding(.bottom, 20)
            }
            .presentationDetents([.medium])
        }
    }

    private var header: some View {
        HStack {
            Button(action: onClose) {
                HStack(spacing: 6) {
                    Image("rs_setup_close").resizable().frame(width: 14, height: 14)
                    Text("Close").font(.plusJakarta(size: 14)).foregroundStyle(spec.secondary)
                }
            }
            Spacer()
            Text(spec.mode).font(.plusJakarta(size: 10, weight: .semibold)).foregroundStyle(spec.accent)
        }
    }

    private var hero: some View {
        VStack(spacing: 16) {
            ZStack {
                Circle().fill(spec.notice).frame(width: 88, height: 88)
                    .overlay(Circle().stroke(spec.border, lineWidth: 1))
                Image(spec.hero).resizable().frame(width: 38, height: 38)
            }
            Text(spec.title)
                .font(.plusJakarta(size: 32, weight: .semibold))
                .foregroundStyle(spec.text)
                .multilineTextAlignment(.center)
            Text(spec.intro)
                .font(.plusJakarta(size: 14))
                .foregroundStyle(spec.secondary)
                .multilineTextAlignment(.center)
            pill(spec.badge, fill: spec.draftFill, ink: spec.draftInk)
        }
        .frame(maxWidth: .infinity)
    }

    private var modelSelector: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Kind of business").font(.plusJakarta(size: 10, weight: .semibold)).foregroundStyle(spec.secondary)
            HStack(spacing: 6) {
                ForEach(subtypes) { item in
                    let on = item.code == subtype.code
                    Button { onSubtype(item) } label: {
                        VStack(spacing: 7) {
                            Image(modelIcon(item.code)).resizable().frame(width: 20, height: 20)
                            Text(item.label).font(.plusJakarta(size: 10, weight: .semibold)).multilineTextAlignment(.center)
                        }
                        .foregroundStyle(on ? spec.chipOnLabel : spec.chipOffLabel)
                        .frame(maxWidth: .infinity)
                        .frame(height: 80)
                        .background(on ? spec.chipOnFill : spec.chipOffFill)
                        .overlay(RoundedRectangle(cornerRadius: 10).stroke(on ? spec.chipOnBorder : spec.border, lineWidth: 1))
                        .clipShape(RoundedRectangle(cornerRadius: 10))
                    }
                }
            }
        }
        .padding(12)
        .background(spec.card)
        .overlay(RoundedRectangle(cornerRadius: 18).stroke(spec.border, lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 18))
    }

    private var basics: some View {
        section("01", "Business Basics") {
            Text(spec.basicsLine).font(.plusJakarta(size: 14)).foregroundStyle(spec.secondary)
            editor(spec.nameHint, text: $name, placeholder: spec.nameHint, icon: "rs_setup_pencil")
                .onChange(of: name) { _, value in
                    if !companyEdited { companyName = value }
                }
            editor(
                "Company name",
                text: Binding(
                    get: { companyName },
                    set: {
                        companyEdited = true
                        companyName = $0
                    }
                ),
                placeholder: "Registered company name",
                icon: "rs_setup_pencil"
            )
            editor(
                "GSTIN",
                text: Binding(
                    get: { gstin },
                    set: { gstin = String($0.prefix(15)) }
                ),
                placeholder: "15-digit GSTIN, if you have one",
                icon: "rs_setup_pencil"
            )
            choice("Currency", currencies.first { $0.0 == currency }?.1 ?? currency, icon: "rs_setup_chevron") { currencyOpen.toggle() }
            if currencyOpen {
                ForEach(currencies, id: \.0) { item in
                    Button(item.1) { currency = item.0; currencyOpen = false }
                        .font(.plusJakarta(size: 14))
                        .foregroundStyle(item.0 == currency ? spec.accent : .white)
                }
            }
            choice("Start date", dayLabel(start), icon: "rs_setup_calendar") { editingEnd = false; dateOpen = true }
            choice("End date", end.map(dayLabel) ?? "Still open", icon: "rs_setup_calendar") { editingEnd = true; dateOpen = true }
            editor("Description", text: $details, placeholder: spec.descriptionHint, icon: "rs_setup_pencil")
            editor("Location", text: $location, placeholder: spec.locationHint, icon: "rs_setup_pin")
            if !spec.tradeReach.isEmpty {
                Text("Trade reach").font(.plusJakarta(size: 12)).foregroundStyle(spec.secondary)
                FlowLayout(spacing: 8) {
                    ForEach(spec.tradeReach, id: \.self) { reach in
                        let on = tradeReach.contains(reach)
                        Button(on ? "✓ \(reach)" : reach) {
                            if on { tradeReach.remove(reach) } else { tradeReach.insert(reach) }
                        }
                        .font(.plusJakarta(size: 11, weight: on ? .semibold : .regular))
                        .foregroundStyle(on ? spec.channelOnLabel : spec.channelOffLabel)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 6)
                        .background(on ? spec.channelOnFill : Color.clear)
                        .overlay(RoundedRectangle(cornerRadius: 8).stroke(on ? spec.chipOnBorder : spec.border, lineWidth: 1))
                        .clipShape(RoundedRectangle(cornerRadius: 8))
                    }
                }
            }
            Text("You can fill in the extra details later.")
                .font(.plusJakarta(size: 12)).foregroundStyle(spec.secondary)
        }
    }

    private var people: some View {
        section("02", "People & Roles") {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text("\(owner) · You").font(.plusJakarta(size: 14)).foregroundStyle(spec.text)
                    Text("Owner").font(.plusJakarta(size: 12)).foregroundStyle(spec.secondary)
                }
                Spacer()
                Text(initials(owner)).font(.plusJakarta(size: 14, weight: .semibold)).foregroundStyle(spec.accent)
            }
            pill("You are the owner", fill: spec.statusFill, ink: spec.statusInk)
            ForEach(invites) { person in
                Text("\(person.name) · \(roleLabel(person.role))").font(.plusJakarta(size: 14)).foregroundStyle(spec.text)
            }
            Button(inviting ? "Close invite" : "+ Invite or add people") { inviting.toggle() }
                .font(.plusJakarta(size: 14, weight: .semibold))
                .foregroundStyle(spec.actionInk)
                .frame(maxWidth: .infinity)
                .padding(14)
                .background(spec.inviteFill)
                .overlay(RoundedRectangle(cornerRadius: 14).stroke(spec.inviteBorder, lineWidth: 1))
                .clipShape(RoundedRectangle(cornerRadius: 14))
            if inviting {
                editor("Name", text: $inviteName, placeholder: "Person name", icon: "rs_setup_pencil")
                ForEach(OperatingMomentCatalog.roles.filter { $0.0 != "OWNER_ADMIN" }, id: \.0) { item in
                    Button(item.1) { inviteRole = item.0 }
                        .font(.plusJakarta(size: 11, weight: .semibold))
                        .foregroundStyle(inviteRole == item.0 ? spec.accent : spec.secondary)
                }
                Button("Add") {
                    let trimmed = inviteName.trimmingCharacters(in: .whitespaces)
                    guard !trimmed.isEmpty else { return }
                    invites.append(StoreInvite(name: trimmed, role: inviteRole))
                    inviteName = ""
                    inviting = false
                }
                .foregroundStyle(spec.text)
            }
            Text(spec.peopleNote)
                .font(.plusJakarta(size: 12)).foregroundStyle(spec.secondary)
            Text("Who can do what").font(.plusJakarta(size: 16)).foregroundStyle(spec.text)
            ForEach(spec.roles, id: \.0) { row in
                VStack(alignment: .leading, spacing: 4) {
                    Text(row.0).font(.plusJakarta(size: 14)).foregroundStyle(spec.text)
                    Text(row.1).font(.plusJakarta(size: 12)).foregroundStyle(spec.secondary)
                }
                .padding(.vertical, 8)
            }
            choice("Who needs to approve before this starts", policies.first { $0.0 == policy }?.1 ?? policy, icon: "rs_setup_chevron") { policyOpen.toggle() }
            if policyOpen {
                ForEach(policies, id: \.0) { item in
                    Button(item.1) { policy = item.0; policyOpen = false }
                        .font(.plusJakarta(size: 14))
                        .foregroundStyle(item.0 == policy ? spec.accent : .white)
                }
            }
            VStack(alignment: .leading, spacing: 6) {
                Text(needsApprover ? "Someone needs to approve" : "\(owner) can start this")
                    .font(.plusJakarta(size: 13, weight: .semibold)).foregroundStyle(spec.accent)
                Text(needsApprover
                     ? "Add a person who can approve, or choose no approval needed."
                     : "You only need someone else to approve if you turn that on. A manager or finance person is not required to start.")
                    .font(.plusJakarta(size: 12)).foregroundStyle(spec.secondary)
            }
            .padding(16)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(spec.notice)
            .clipShape(RoundedRectangle(cornerRadius: 12))
            Text("Only the owner can change who has access.")
                .font(.plusJakarta(size: 12)).foregroundStyle(spec.secondary)
        }
    }

    private var counterparties: some View {
        section("03", spec.partySection) {
            Text(spec.partyNote)
                .font(.plusJakarta(size: 12)).foregroundStyle(spec.secondary)
            ForEach(Array(parties.enumerated()), id: \.offset) { _, party in
                Text("\(party.1) · \(spec.partyKinds.first { $0.0 == party.0 }?.1 ?? party.0)")
                    .font(.plusJakarta(size: 14)).foregroundStyle(spec.text)
            }
            editor("Name", text: $partyName, placeholder: "Counterparty name", icon: "rs_setup_pencil")
            ForEach(spec.partyKinds, id: \.0) { item in
                Button(item.1) { partyKind = item.0 }
                    .font(.plusJakarta(size: 11, weight: .semibold))
                    .foregroundStyle(partyKind == item.0 ? spec.channelOnLabel : spec.channelOffLabel)
            }
            Button("Add") {
                let trimmed = partyName.trimmingCharacters(in: .whitespaces)
                guard !trimmed.isEmpty else { return }
                parties.append((partyKind, trimmed))
                partyName = ""
            }
            .foregroundStyle(spec.text)
        }
    }

    private var plan: some View {
        section(spec.partySection.isEmpty ? "03" : "04", "Channels & Monthly Plan") {
            Text(spec.channelPrompt).font(.plusJakarta(size: 14)).foregroundStyle(spec.text)
            FlowLayout(spacing: 8) {
                ForEach(spec.channels, id: \.self) { channel in
                    let on = selectedChannels.contains(channel)
                    Button(on ? "✓ \(channel)" : channel) {
                        if on { selectedChannels.remove(channel) } else { selectedChannels.insert(channel) }
                    }
                    .font(.plusJakarta(size: on ? 11 : 12, weight: on ? .semibold : .regular))
                    .foregroundStyle(on ? spec.channelOnLabel : spec.channelOffLabel)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 6)
                    .background(on ? spec.channelOnFill : Color.clear)
                    .overlay(RoundedRectangle(cornerRadius: on ? 999 : 8).stroke(spec.border, lineWidth: on ? 0 : 1))
                    .clipShape(RoundedRectangle(cornerRadius: on ? 999 : 8))
                }
            }
            Text(selectedChannels.isEmpty
                 ? (spec.channelsOptional ? spec.channelHelp : "Choose at least one.")
                 : "\(selectedChannels.count) selected. \(spec.channelHelp)")
                .font(.plusJakarta(size: 12)).foregroundStyle(spec.secondary)
            choice("Month", monthLabel(start), icon: "rs_setup_calendar") { editingEnd = false; dateOpen = true }
            editor("Monthly budget", text: $budget, placeholder: "Amount", icon: "rs_setup_pencil")
            editor("Monthly sales target", text: $target, placeholder: "Amount", icon: "rs_setup_pencil")
            if spec.targetMetrics.count > 1 {
                choice("How the target is measured", metricLabel(metric), icon: "rs_setup_chevron") { metricOpen.toggle() }
                if metricOpen {
                    ForEach(spec.targetMetrics, id: \.self) { code in
                        Button(metricLabel(code)) { metric = code; metricOpen = false }
                            .font(.plusJakarta(size: 14))
                            .foregroundStyle(code == metric ? spec.accent : spec.text)
                    }
                }
            }
            VStack(alignment: .leading, spacing: 6) {
                Text("How your target is counted").font(.plusJakarta(size: 13, weight: .semibold)).foregroundStyle(spec.accent)
                Text(spec.targetNote)
                    .font(.plusJakarta(size: 12)).foregroundStyle(spec.secondary)
            }
            .padding(16)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(spec.notice)
            .clipShape(RoundedRectangle(cornerRadius: 12))
            if ready { pill("Budget and sales target look good", fill: spec.statusFill, ink: spec.statusInk) }
        }
    }

    private var review: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Review your setup").font(.plusJakarta(size: 20, weight: .semibold)).foregroundStyle(spec.text)
            VStack(alignment: .leading, spacing: 4) {
                Text(name.isEmpty ? spec.nameHint : name).font(.plusJakarta(size: 14)).foregroundStyle(spec.text)
                Text("\(spec.reviewKind) · \(currency) · From \(dayLabel(start))").font(.plusJakarta(size: 12)).foregroundStyle(spec.secondary)
            }
            reviewLine("Company", companyName.trimmingCharacters(in: .whitespaces).isEmpty ? "Not set" : companyName)
            reviewLine("GSTIN", gstin.trimmingCharacters(in: .whitespaces).isEmpty ? "Not added" : gstin)
            if !tradeReach.isEmpty { reviewLine("Trade reach", tradeReach.sorted().joined(separator: ", ")) }
            if !parties.isEmpty { reviewLine("Counterparties", parties.map(\.1).joined(separator: ", ")) }
            reviewLine("People", ([ "\(owner) · Owner" ] + invites.map { "\($0.name) · \(roleLabel($0.role))" }).joined(separator: ", "))
            Text("Sales channels · \(selectedChannels.sorted().joined(separator: ", ").isEmpty ? "None yet" : selectedChannels.sorted().joined(separator: ", "))")
                .font(.plusJakarta(size: 12)).foregroundStyle(spec.secondary)
            reviewLine("\(monthLabel(start)) budget", money(budget))
            reviewLine("\(monthLabel(start)) sales target", money(target))
        }
        .padding(18)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(spec.notice)
        .overlay(RoundedRectangle(cornerRadius: 18).stroke(spec.border, lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 18))
    }

    private func section<Content: View>(_ number: String, _ title: String, @ViewBuilder content: () -> Content) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 14) {
                Text(number).font(.plusJakarta(size: 48, weight: .heavy)).foregroundStyle(spec.number)
                Text(title).font(.plusJakarta(size: 20, weight: .semibold)).foregroundStyle(spec.text)
            }
            content()
        }
        .padding(18)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(spec.card)
        .overlay(RoundedRectangle(cornerRadius: 18).stroke(spec.border, lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 18))
    }

    private func editor(_ label: String, text: Binding<String>, placeholder: String, icon: String) -> some View {
        HStack {
            VStack(alignment: .leading, spacing: 4) {
                Text(label).font(.plusJakarta(size: 12)).foregroundStyle(spec.secondary)
                TextField(placeholder, text: text).font(.plusJakarta(size: 14)).foregroundStyle(spec.text)
            }
            Spacer()
            Image(icon).resizable().frame(width: 16, height: 16)
        }
        .padding(14)
        .background(spec.field)
        .overlay(RoundedRectangle(cornerRadius: 8).stroke(spec.border, lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 8))
    }

    private func choice(_ label: String, _ value: String, icon: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text(label).font(.plusJakarta(size: 12)).foregroundStyle(spec.secondary)
                    Text(value).font(.plusJakarta(size: 14)).foregroundStyle(spec.text)
                }
                Spacer()
                Image(icon).resizable().frame(width: 16, height: 16)
            }
            .padding(14)
            .background(spec.field)
            .overlay(RoundedRectangle(cornerRadius: 8).stroke(spec.border, lineWidth: 1))
            .clipShape(RoundedRectangle(cornerRadius: 8))
        }
    }

    private func reviewLine(_ label: String, _ value: String) -> some View {
        HStack {
            Text(label).font(.plusJakarta(size: 14)).foregroundStyle(spec.text)
            Spacer()
            Text(value).font(.plusJakarta(size: 14, weight: .semibold)).foregroundStyle(spec.accent)
        }
        .padding(.vertical, 8)
    }

    private func pill(_ text: String, fill: Color, ink: Color) -> some View {
        Text(text)
            .font(.plusJakarta(size: 11, weight: .semibold))
            .foregroundStyle(ink)
            .padding(.horizontal, 10)
            .padding(.vertical, 6)
            .background(fill)
            .overlay(Capsule().stroke(spec.pillBorder, lineWidth: spec.pillBorder == .clear ? 0 : 1))
            .clipShape(Capsule())
    }

    private func applyCompanySeed() {
        if name.isEmpty && !initialName.isEmpty { name = initialName }
        if !companyEdited && !initialCompanyName.isEmpty {
            companyName = initialCompanyName
            companyEdited = true
        }
        if gstin.isEmpty && !initialGstin.isEmpty { gstin = initialGstin }
    }

    private func activate() {
        let trimmed = name.trimmingCharacters(in: .whitespaces)
        let company = companyName.trimmingCharacters(in: .whitespaces)
        if trimmed.isEmpty { localError = "Add a name."; return }
        if company.isEmpty { localError = "Add a company name."; return }
        if !spec.channelsOptional && selectedChannels.isEmpty { localError = "Choose at least one."; return }
        if (Double(budget) ?? 0) <= 0 { localError = "Enter a monthly budget above zero."; return }
        if (Double(target) ?? 0) <= 0 { localError = "Enter a monthly sales target above zero."; return }
        if needsApprover { localError = "Add someone who can approve, or choose no approval needed."; return }
        localError = nil
        let participants: [[String: String]] = invites.map { ["displayName": $0.name, "role": $0.role] }
        let prefs: [String: Any] = [
            "subtype": spec.code,
            "identityName": trimmed,
            "partyName": trimmed,
            "description": details.trimmingCharacters(in: .whitespaces),
            "currency": currency,
            "startDate": isoDay(start),
            "endDate": end.map(isoDay) ?? "",
            "location": location.trimmingCharacters(in: .whitespaces),
            "activationPolicy": policy,
            "participants": participants,
            "counterparties": parties.map { ["name": $0.1, "kind": $0.0] },
            "channels": Array(selectedChannels),
            "tradeReach": Array(tradeReach),
            "monthlyBudget": budget.trimmingCharacters(in: .whitespaces),
            "monthlyTarget": target.trimmingCharacters(in: .whitespaces),
            "targetMetric": metric,
            "companyName": company,
            "gstin": gstin.trimmingCharacters(in: .whitespaces),
        ]
        onActivate(prefs, trimmed)
    }

    private func metricLabel(_ code: String) -> String {
        let metrics = spec.targetMetrics
        switch code {
        case "REVENUE": return metrics.contains("PRODUCTION_OUTPUT") ? "Recognized sales" : (metrics.contains("VALUE_DISPATCHED") ? "Sales / Revenue" : "Revenue")
        case "LEADS": return "Leads"
        case "CONVERSIONS": return "Conversions"
        case "REACH": return "Reach"
        case "ENGAGEMENT": return "Engagement"
        case "ORDERS": return metrics.contains("PRODUCTION_OUTPUT") ? "Order intake" : (metrics.contains("DELIVERY_COUNT") ? "Order count" : "Orders")
        case "CUSTOM_COUNT": return metrics.contains("DELIVERY_COUNT") ? "Compatible units" : "Custom count"
        case "CLIENT_BILLING": return "Client billing"
        case "PRODUCTION_OUTPUT": return metrics.contains("DELIVERY_COUNT") ? "Accepted units" : "Production output"
        case "UNITS_PRODUCED": return "Units Produced"
        case "VALUE_DISPATCHED": return "Value Dispatched"
        case "DELIVERY_COUNT": return metrics.contains("PRODUCTION_OUTPUT") ? "Delivered units" : "Delivery count"
        default: return code
        }
    }

    private func modelIcon(_ code: String) -> String {
        switch code {
        case "RESTAURANT_FOOD": return "rs_setup_utensils"
        case "CONSUMER_SERVICE": return "rs_setup_handshake"
        case "TRADING_WHOLESALE": return "rs_setup_trading"
        case "CLIENT_CAMPAIGN", "OWN_BUSINESS_CAMPAIGN": return "rs_setup_campaign"
        case "STANDARD_PRODUCTION": return "rs_setup_standard"
        case "MADE_TO_ORDER": return "rs_setup_jobwork"
        case "DISTRIBUTION_DEALERSHIP": return "rs_setup_distribution"
        default: return "rs_setup_store_chip"
        }
    }

    private func plainSetupError(_ message: String?) -> String? {
        guard let message, !message.isEmpty else { return nil }
        let hasCode = message.range(of: #"\b[A-Z][A-Z0-9_]{2,}\b"#, options: .regularExpression) != nil
        if message.contains("\\n") || message.contains("{") || message.contains("/") || hasCode {
            return "We couldn't start this yet. Check the name, budget, and sales target, then try again."
        }
        return message
    }

    private func roleLabel(_ code: String) -> String {
        OperatingMomentCatalog.roles.first { $0.0 == code }?.1 ?? code
    }

    private func initials(_ name: String) -> String {
        let parts = name.split(separator: " ").prefix(2)
        let letters = parts.compactMap { $0.first }.map { String($0).uppercased() }.joined()
        return letters.isEmpty ? "Y" : letters
    }

    private func money(_ amount: String) -> String {
        let trimmed = amount.trimmingCharacters(in: .whitespaces)
        if trimmed.isEmpty { return "—" }
        return currency == "INR" ? "₹\(trimmed)" : "\(currency) \(trimmed)"
    }

    private func dayLabel(_ date: Date) -> String {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.dateFormat = "dd MMMM yyyy"
        return formatter.string(from: date)
    }

    private func monthLabel(_ date: Date) -> String {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.dateFormat = "MMMM yyyy"
        return formatter.string(from: date)
    }

    private func isoDay(_ date: Date) -> String {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.dateFormat = "yyyy-MM-dd"
        return formatter.string(from: date)
    }
}

private struct SetupSpec {
    let code: String
    let bg: Color
    let card: Color
    let field: Color
    let border: Color
    let notice: Color
    let brand: Color
    let accent: Color
    let number: Color
    let secondary: Color
    let text: Color
    let statusFill: Color
    let statusInk: Color
    let draftFill: Color
    let draftInk: Color
    let inviteFill: Color
    let inviteBorder: Color
    let actionInk: Color
    let pillBorder: Color
    let chipOnFill: Color
    let chipOnBorder: Color
    let chipOnLabel: Color
    let chipOffFill: Color
    let chipOffLabel: Color
    let channelOnFill: Color
    let channelOnLabel: Color
    let channelOffLabel: Color
    let hero: String
    let mode: String
    let title: String
    let badge: String
    let basicsLine: String
    let nameHint: String
    let descriptionHint: String
    let locationHint: String
    let ownerAccess: String
    let leadTitle: String
    let leadBody: String
    let peopleNote: String
    let channelPrompt: String
    let channels: [String]
    let activate: String
    let footer: String
    let reviewKind: String
    var tradeReach: [String] = []
    var partySection: String = ""
    var partyKinds: [(String, String)] = []
    var steps: String = "3 steps · One simple setup"
    var intro: String = "Bring your business, people, and monthly plan together before you start."
    var partyNote: String = "Optional. These are counterparties, not people who can sign in."
    var channelHelp: String = "These are the places you sell or serve. They are not a different kind of business."
    var targetNote: String = "Sales you record count toward the target. Money already collected does not. Purchases and expenses count once toward the budget."
    var targetMetrics: [String] = ["REVENUE"]
    var channelsOptional: Bool = false

    var roles: [(String, String)] {
        [
            ("Owner / Admin", ownerAccess),
            (leadTitle, leadBody),
            ("Finance", "Can see money records. Cannot approve unless you allow it."),
            ("Approver", "Can review and approve. Does not pay unless you allow it."),
            ("Contributor", "Can add updates for the work you assign."),
            ("Team member", "Can post updates. Money totals stay hidden."),
            ("Observer", "Can view what you assign. Money totals stay hidden."),
        ]
    }

    static func campaignSpec(
        code: String,
        title: String,
        badge: String,
        intro: String,
        basicsLine: String,
        channels: [String],
        partyKinds: [(String, String)],
        partyNote: String,
        targetMetrics: [String],
        reviewKind: String
    ) -> SetupSpec {
        SetupSpec(
            code: code,
            bg: Color(hex: "#09121F"), card: Color(hex: "#122235"), field: Color(hex: "#09121F"),
            border: Color(hex: "#2B4B63"), notice: Color(hex: "#16405C"), brand: Color(hex: "#6DDCFF"),
            accent: Color(hex: "#6DDCFF"), number: Color(hex: "#406582"), secondary: Color(hex: "#A5BBCE"),
            text: Color(hex: "#E5E0EE"), statusFill: Color(hex: "#16405C"), statusInk: Color(hex: "#6DDCFF"),
            draftFill: Color(hex: "#16405C"), draftInk: Color(hex: "#6DDCFF"),
            inviteFill: Color(hex: "#6DDCFF"), inviteBorder: Color(hex: "#6DDCFF"), actionInk: Color(hex: "#09121F"),
            pillBorder: Color(hex: "#2B4B63"),
            chipOnFill: Color(hex: "#16405C"), chipOnBorder: Color(hex: "#6DDCFF"), chipOnLabel: Color(hex: "#E5E0EE"),
            chipOffFill: Color(hex: "#09121F"), chipOffLabel: Color(hex: "#A5BBCE"),
            channelOnFill: Color(hex: "#16405C"), channelOnLabel: Color(hex: "#6DDCFF"), channelOffLabel: Color(hex: "#A5BBCE"),
            hero: "rs_setup_campaign", mode: "Marketing", title: title,
            badge: badge, basicsLine: basicsLine,
            nameHint: "Campaign name", descriptionHint: "Everyday notes about this campaign",
            locationHint: "Where this campaign runs",
            ownerAccess: "Full campaign access. Only the owner changes roles.",
            leadTitle: "Campaign lead", leadBody: "Runs day-to-day campaign work. Does not see full money records unless you allow it.",
            peopleNote: "You can start on your own. A campaign lead and finance person are optional.",
            channelPrompt: "Channels are optional.",
            channels: channels,
            activate: "Activate campaign",
            footer: "This stays private to your business. Only the owner can change who has access. Nothing is counted until the campaign is active.",
            reviewKind: reviewKind,
            partySection: "Network / Counterparties",
            partyKinds: partyKinds,
            steps: "4 steps · One simple setup",
            intro: intro,
            partyNote: partyNote,
            channelHelp: "These are the places this campaign runs. They are not a different kind of business.",
            targetNote: "Accepted results count toward the target. Money collected does not. Spend and vendor costs count once toward the budget.",
            targetMetrics: targetMetrics,
            channelsOptional: true
        )
    }

    static func productionSpec(
        code: String,
        mint: Bool,
        title: String,
        intro: String,
        basicsLine: String,
        partySection: String,
        partyKinds: [(String, String)],
        partyNote: String,
        targetMetrics: [String],
        reviewKind: String,
        steps: String,
        tradeReach: [String] = []
    ) -> SetupSpec {
        let accent = mint ? "#66C7A4" : "#6DDCFF"
        let page = mint ? "#071014" : "#09121F"
        let card = mint ? "#15262C" : "#122235"
        let border = mint ? "#345058" : "#2B4B63"
        let chip = mint ? "#15262C" : "#16405C"
        let ink = mint ? "#EFF5F2" : "#E5E0EE"
        let secondary = mint ? "#AFC1C3" : "#A5BBCE"
        let number = mint ? "#66C7A4" : "#406582"
        return SetupSpec(
            code: code,
            bg: Color(hex: page), card: Color(hex: card), field: Color(hex: page),
            border: Color(hex: border), notice: Color(hex: chip), brand: Color(hex: accent),
            accent: Color(hex: accent), number: Color(hex: number), secondary: Color(hex: secondary),
            text: Color(hex: ink), statusFill: Color(hex: chip), statusInk: Color(hex: accent),
            draftFill: Color(hex: chip), draftInk: Color(hex: accent),
            inviteFill: Color(hex: accent), inviteBorder: Color(hex: accent), actionInk: Color(hex: page),
            pillBorder: Color(hex: border),
            chipOnFill: Color(hex: chip), chipOnBorder: Color(hex: accent), chipOnLabel: Color(hex: ink),
            chipOffFill: Color(hex: page), chipOffLabel: Color(hex: secondary),
            channelOnFill: Color(hex: chip), channelOnLabel: Color(hex: accent), channelOffLabel: Color(hex: secondary),
            hero: mint ? "rs_setup_jobwork" : "rs_setup_standard", mode: "Manufacturing", title: title,
            badge: "Setup draft · Not active", basicsLine: basicsLine,
            nameHint: "Factory name", descriptionHint: "Everyday notes about this factory",
            locationHint: "Factory location",
            ownerAccess: "Full manufacturing access. Only the owner changes roles.",
            leadTitle: "Production / Order Lead",
            leadBody: "Runs day-to-day production. Does not see full money records unless you allow it.",
            peopleNote: "You can start on your own. A production lead and finance person are optional.",
            channelPrompt: "Channels are optional.",
            channels: [],
            activate: "Activate Manufacturing Moment",
            footer: "This stays private to your business. Only the owner can change who has access. Nothing is counted until the manufacturing business is active.",
            reviewKind: reviewKind,
            tradeReach: tradeReach,
            partySection: partySection,
            partyKinds: partyKinds,
            steps: steps,
            intro: intro,
            partyNote: partyNote,
            channelHelp: "Channels are optional. They are the places finished goods leave the factory.",
            targetNote: "Validated sales count toward a revenue target. Money collected does not. Production, orders, and deliveries count only for the measure you choose. Purchases and expenses count once toward the budget.",
            targetMetrics: targetMetrics,
            channelsOptional: true
        )
    }

    static func forCode(_ code: String) -> SetupSpec {
        switch code {
        case "STANDARD_PRODUCTION":
            return productionSpec(
                code: code, mint: false,
                title: "Standard Production Setup",
                intro: "Set up your manufacturing business, people, suppliers, buyers and monthly plan before activation.",
                basicsLine: "A home for this factory.",
                partySection: "Material Suppliers & Buyers",
                partyKinds: [("SUPPLIER", "Material supplier"), ("BUYER", "Buyer or group")],
                partyNote: "Material suppliers and buyers are optional. Counterparties stay separate from invited team roles.",
                targetMetrics: ["REVENUE", "PRODUCTION_OUTPUT", "DELIVERY_COUNT", "ORDERS"],
                reviewKind: "Standard Production",
                steps: "4 steps · Factory, people, suppliers, buyers and monthly plan"
            )
        case "MADE_TO_ORDER":
            return productionSpec(
                code: code, mint: true,
                title: "Made-to-Order Production Setup",
                intro: "Set up your job-work business, people, suppliers, customers and monthly plan.",
                basicsLine: "A home for this workshop.",
                partySection: "Material Suppliers & Customers",
                partyKinds: [("SUPPLIER", "Material supplier"), ("CUSTOMER", "Customer")],
                partyNote: "Material suppliers and customers are optional. They are counterparties, not people who can sign in.",
                targetMetrics: ["REVENUE", "PRODUCTION_OUTPUT", "DELIVERY_COUNT", "ORDERS"],
                reviewKind: "Made-to-Order Production",
                steps: "4 steps · Factory, people, suppliers, customers and monthly plan",
                tradeReach: ["Local", "Domestic", "International"]
            )
        case "CLIENT_CAMPAIGN":
            return campaignSpec(
                code: code,
                title: "Marketing Campaign Setup",
                badge: "Campaign draft · Not active",
                intro: "Run a marketing campaign with clear roles, counterparties and measurable targets before you activate.",
                basicsLine: "A home for this client campaign.",
                channels: ["Search Ads", "Paid Social", "Display / Video Ads", "Organic Social", "SEO / Content", "Email", "SMS / Messaging", "Cold Calling", "Field Sales / Visits", "Newspaper / Magazine", "Flyers / Brochures", "Radio / Podcast", "Billboard / Transit", "Exhibition / Roadshow", "Influencer", "Affiliate / Referral", "Custom Channel"],
                partyKinds: [("CLIENT", "Client contact"), ("VENDOR", "Vendor, creator, agency, or platform")],
                partyNote: "Client identity and optional vendors, creators, agencies, and platforms are separate from invited team roles.",
                targetMetrics: ["LEADS", "CONVERSIONS", "REVENUE", "ORDERS", "REACH", "ENGAGEMENT", "CUSTOM_COUNT", "CLIENT_BILLING"],
                reviewKind: "Client Campaign"
            )
        case "OWN_BUSINESS_CAMPAIGN":
            return campaignSpec(
                code: code,
                title: "Own Business Campaign Setup",
                badge: "Draft · Not active",
                intro: "You run campaigns for your own business or brand",
                basicsLine: "A home for this brand campaign.",
                channels: ["Search Ads", "Paid Social", "Display / Video Ads", "Organic Social", "SEO / Content", "Email", "SMS / Messaging", "Cold Calling", "Field Sales / Visits", "Newspaper / Magazine", "Flyers / Brochures", "Radio / Podcast", "Billboard / Transit", "Exhibition / Roadshow", "Influencer", "Affiliate / Referral", "Custom Channel"],
                partyKinds: [("VENDOR", "Vendor, creator, agency, or platform")],
                partyNote: "Vendors, creators, agencies and platforms are separate from invited team roles. No client is required.",
                targetMetrics: ["LEADS", "CONVERSIONS", "REVENUE", "ORDERS", "REACH", "ENGAGEMENT", "CUSTOM_COUNT"],
                reviewKind: "Own Business Campaign"
            )
        case "TRADING_WHOLESALE":
            return SetupSpec(
                code: code,
                bg: Color(hex: "#09121F"), card: Color(hex: "#122235"), field: Color(hex: "#09121F"),
                border: Color(hex: "#2B4B63"), notice: Color(hex: "#16405C"), brand: Color(hex: "#6DDCFF"),
                accent: Color(hex: "#6DDCFF"), number: Color(hex: "#406582"), secondary: Color(hex: "#A5BBCE"),
                text: Color(hex: "#E5E0EE"), statusFill: Color(hex: "#16405C"), statusInk: Color(hex: "#6DDCFF"),
                draftFill: Color(hex: "#16405C"), draftInk: Color(hex: "#6DDCFF"),
                inviteFill: Color(hex: "#6DDCFF"), inviteBorder: Color(hex: "#6DDCFF"), actionInk: Color(hex: "#09121F"),
                pillBorder: Color(hex: "#2B4B63"),
                chipOnFill: Color(hex: "#16405C"), chipOnBorder: Color(hex: "#6DDCFF"), chipOnLabel: Color(hex: "#E5E0EE"),
                chipOffFill: Color(hex: "#09121F"), chipOffLabel: Color(hex: "#A5BBCE"),
                channelOnFill: Color(hex: "#16405C"), channelOnLabel: Color(hex: "#6DDCFF"), channelOffLabel: Color(hex: "#A5BBCE"),
                hero: "rs_setup_trading", mode: "Trading", title: "Trading / Wholesale Setup",
                badge: "Not started yet", basicsLine: "A home for this trading business.",
                nameHint: "Business name", descriptionHint: "Everyday notes about this trade",
                locationHint: "Warehouse or business location",
                ownerAccess: "Full trading business access. Only the owner changes roles.",
                leadTitle: "Trading manager", leadBody: "Runs day-to-day work. Does not see full money records unless you allow it.",
                peopleNote: "You can start on your own. You can invite people after this starts.",
                channelPrompt: "Where do you sell? Choose at least one.",
                channels: ["Wholesale", "Direct", "Dealer", "Marketplace", "Export", "Other"],
                activate: "Activate trading moment",
                footer: "This stays private to your business. Only the owner can change who has access. Nothing is counted until you start.",
                reviewKind: "Trading / Wholesale",
                tradeReach: ["Local", "Domestic", "International"],
                partySection: "Suppliers, Buyers & Channels",
                partyKinds: [("SUPPLIER", "Supplier"), ("BUYER", "Buyer")],
                steps: "4 steps · One simple setup",
                intro: "Bring your business, people and monthly plan together. Make it yours before you activate."
            )
        case "DISTRIBUTION_DEALERSHIP":
            return SetupSpec(
                code: code,
                bg: Color(hex: "#071014"), card: Color(hex: "#0C171B"), field: Color(hex: "#0C171B"),
                border: Color(hex: "#345058"), notice: Color(hex: "#15262C"), brand: Color(hex: "#66C7A4"),
                accent: Color(hex: "#66C7A4"), number: Color(hex: "#66C7A4"), secondary: Color(hex: "#66C7A4"),
                text: Color(hex: "#EFF5F2"), statusFill: Color(hex: "#15262C"), statusInk: Color(hex: "#66C7A4"),
                draftFill: Color(hex: "#15262C"), draftInk: Color(hex: "#66C7A4"),
                inviteFill: Color(hex: "#66C7A4"), inviteBorder: Color(hex: "#66C7A4"), actionInk: Color(hex: "#071014"),
                pillBorder: Color(hex: "#345058"),
                chipOnFill: Color(hex: "#15262C"), chipOnBorder: Color(hex: "#66C7A4"), chipOnLabel: Color(hex: "#EFF5F2"),
                chipOffFill: Color(hex: "#0C171B"), chipOffLabel: Color(hex: "#66C7A4"),
                channelOnFill: Color(hex: "#15262C"), channelOnLabel: Color(hex: "#66C7A4"), channelOffLabel: Color(hex: "#66C7A4"),
                hero: "rs_setup_distribution", mode: "Distribution", title: "Distribution / Dealership Setup",
                badge: "Not started yet", basicsLine: "A home for this distribution business.",
                nameHint: "Business name", descriptionHint: "Everyday notes about this distribution",
                locationHint: "Depot or service territory",
                ownerAccess: "Full distribution business access. Only the owner changes roles.",
                leadTitle: "Distribution manager", leadBody: "Operations lead. Does not see full money records unless you allow it.",
                peopleNote: "You can start on your own. A distribution manager and finance person are optional.",
                channelPrompt: "Where do you sell? Choose at least one.",
                channels: ["Retailer", "Dealer", "Sub-distributor", "Institutional", "Direct", "Other"],
                activate: "Activate distribution moment",
                footer: "This stays private to your business. Only the owner can change who has access. Nothing is counted until the distribution business is active.",
                reviewKind: "Distribution / Dealership",
                tradeReach: ["Local", "Domestic", "International"],
                partySection: "Principals & Counterparties",
                partyKinds: [("PRINCIPAL", "Principal or supplier"), ("DEALER", "Dealer or buyer group")],
                steps: "4 steps · One simple setup",
                intro: "Bring your business, people and key counterparties together. Make it yours before you activate."
            )
        case "RESTAURANT_FOOD":
            return SetupSpec(
                code: code,
                bg: Color(hex: "#101412"), card: Color(hex: "#202A24"), field: Color(hex: "#1A211D"),
                border: Color(hex: "#344238"), notice: Color(hex: "#202A24"), brand: Color(hex: "#F2B544"),
                accent: Color(hex: "#F2B544"), number: Color(hex: "#F2B544"), secondary: Color(hex: "#B9C5BA"),
                text: Color(hex: "#F4F3EB"), statusFill: Color(hex: "#202A24"), statusInk: Color(hex: "#F2B544"),
                draftFill: Color(hex: "#202A24"), draftInk: Color(hex: "#F2B544"),
                inviteFill: Color(hex: "#F2B544"), inviteBorder: Color(hex: "#F2B544"), actionInk: Color(hex: "#101412"),
                pillBorder: Color(hex: "#344238"),
                chipOnFill: Color(hex: "#202A24"), chipOnBorder: Color(hex: "#F2B544"), chipOnLabel: Color(hex: "#F4F3EB"),
                chipOffFill: Color(hex: "#1A211D"), chipOffLabel: Color(hex: "#B9C5BA"),
                channelOnFill: Color(hex: "#F2B544"), channelOnLabel: Color(hex: "#101412"), channelOffLabel: Color(hex: "#F4F3EB"),
                hero: "rs_setup_restaurant", mode: "Restaurant", title: "Restaurant / Food Setup",
                badge: "Not started yet", basicsLine: "A home for this restaurant.",
                nameHint: "Restaurant name", descriptionHint: "Everyday notes about this restaurant",
                locationHint: "Add your restaurant location",
                ownerAccess: "Only the owner can change who has access.",
                leadTitle: "Restaurant manager", leadBody: "Runs day-to-day work. Does not see full money records unless you allow it.",
                peopleNote: "You can start on your own. You can invite people and add suppliers after this starts.",
                channelPrompt: "Where do you sell? Choose at least one.",
                channels: ["Dine-in", "Direct", "Zomato", "Takeaway", "Swiggy"],
                activate: "Start this restaurant",
                footer: "This stays private to your business. Only the owner can change who has access. Nothing is counted until you start.",
                reviewKind: "Restaurant / Food"
            )
        case "CONSUMER_SERVICE":
            return SetupSpec(
                code: code,
                bg: Color(hex: "#080F0E"), card: Color(hex: "#0F2320"), field: Color(hex: "#0B1917"),
                border: Color(hex: "#1E3530"), notice: Color(hex: "#0F2320"), brand: Color(hex: "#0ECBB0"),
                accent: Color(hex: "#0ECBB0"), number: Color(hex: "#0ECBB0"), secondary: Color(hex: "#8FBFBA"),
                text: Color(hex: "#D8F0ED"), statusFill: Color(hex: "#0F2320"), statusInk: Color(hex: "#D8F0ED"),
                draftFill: Color(hex: "#0F2320"), draftInk: Color(hex: "#D8F0ED"),
                inviteFill: Color(hex: "#0ECBB0"), inviteBorder: Color(hex: "#0ECBB0"), actionInk: Color(hex: "#080F0E"),
                pillBorder: Color(hex: "#1E3530"),
                chipOnFill: Color(hex: "#0ECBB0"), chipOnBorder: Color(hex: "#0ECBB0"), chipOnLabel: Color(hex: "#080F0E"),
                chipOffFill: Color(hex: "#0B1917"), chipOffLabel: Color(hex: "#8FBFBA"),
                channelOnFill: Color(hex: "#0ECBB0"), channelOnLabel: Color(hex: "#080F0E"), channelOffLabel: Color(hex: "#8FBFBA"),
                hero: "rs_setup_service", mode: "Service", title: "Consumer Service Setup",
                badge: "Not started yet", basicsLine: "A home for this service.",
                nameHint: "Service name", descriptionHint: "Everyday notes about this service",
                locationHint: "Add your service location",
                ownerAccess: "Only the owner can change who has access.",
                leadTitle: "Service manager", leadBody: "Runs day-to-day work. Does not see full money records unless you allow it.",
                peopleNote: "You can start on your own. Managers and finance people are optional. You can invite them after this starts.",
                channelPrompt: "How do customers reach you? Choose at least one.",
                channels: ["Walk-in", "Direct booking", "Platform", "Home service"],
                activate: "Start this service",
                footer: "This stays private to your business. Only the owner can change who has access. Nothing is counted until you start.",
                reviewKind: "Consumer Service"
            )
        default:
            return SetupSpec(
                code: "RETAIL_STORE",
                bg: Color(hex: "#0B1020"), card: Color(hex: "#161B2E"), field: Color(hex: "#0B1020"),
                border: Color(hex: "#2D3554"), notice: Color(hex: "#252654"), brand: Color(hex: "#5B5CEB"),
                accent: Color(hex: "#A5A7FF"), number: Color(hex: "#565A91"), secondary: Color(hex: "#A1A7C1"),
                text: .white, statusFill: Color(hex: "#102C30"), statusInk: Color(hex: "#10B981"),
                draftFill: Color(hex: "#252654"), draftInk: Color(hex: "#A5A7FF"),
                inviteFill: Color(hex: "#1E243D"), inviteBorder: Color(hex: "#2D3554"), actionInk: .white,
                pillBorder: .clear,
                chipOnFill: Color(hex: "#252654"), chipOnBorder: Color(hex: "#5B5CEB"), chipOnLabel: Color(hex: "#A5A7FF"),
                chipOffFill: Color(hex: "#1A2036"), chipOffLabel: Color(hex: "#A1A7C1"),
                channelOnFill: Color(hex: "#252654"), channelOnLabel: Color(hex: "#A5A7FF"), channelOffLabel: Color(hex: "#A1A7C1"),
                hero: "rs_setup_store", mode: "Retail store", title: "Retail Store Setup",
                badge: "Not started yet", basicsLine: "A home for this store.",
                nameHint: "Store name", descriptionHint: "Everyday notes about this store",
                locationHint: "Add your store location",
                ownerAccess: "Only the owner can change who has access.",
                leadTitle: "Store manager", leadBody: "Runs day-to-day work. Does not see full money records unless you allow it.",
                peopleNote: "You can start on your own. Managers and finance people are optional. You can invite them after this starts.",
                channelPrompt: "Where do you sell? Choose at least one.",
                channels: ["Store", "Website", "Marketplace", "Social", "Direct"],
                activate: "Start this store",
                footer: "This stays private to your business. Only the owner can change who has access. Nothing is counted until you start.",
                reviewKind: "Retail Store"
            )
        }
    }
}

private struct StoreInvite: Identifiable {
    let id = UUID()
    let name: String
    let role: String
}

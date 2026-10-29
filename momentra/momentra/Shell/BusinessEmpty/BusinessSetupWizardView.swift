import SwiftUI

struct BusinessSetupWizardView: View {
    let kind: BusinessSetupKind
    @ObservedObject var createModel: MomentCreateModel
    let companyId: String?
    var editingMomentId: String? = nil
    var initialTitle: String? = nil
    var ownerName: String = ""
    var onClose: () -> Void
    var onCreated: (CreateMomentOutcome) -> Void

    @State private var step = 0
    @State private var subtype: OperatingSubtype
    @State private var identity: String
    @State private var party = ""
    @State private var details = ""
    @State private var people = ""
    @State private var role = "OPERATIONS_LEAD"
    @State private var counterparties = ""
    @State private var channels = ""
    @State private var budget = ""
    @State private var target = ""
    @State private var metric: String
    @State private var localError: String?
    @State private var companySeed = ""
    @State private var gstSeed = ""

    init(
        kind: BusinessSetupKind,
        createModel: MomentCreateModel,
        companyId: String?,
        editingMomentId: String? = nil,
        initialTitle: String? = nil,
        ownerName: String = "",
        onClose: @escaping () -> Void,
        onCreated: @escaping (CreateMomentOutcome) -> Void
    ) {
        self.kind = kind
        self.createModel = createModel
        self.companyId = companyId
        self.editingMomentId = editingMomentId
        self.initialTitle = initialTitle
        self.ownerName = ownerName
        self.onClose = onClose
        self.onCreated = onCreated
        let first = OperatingMomentCatalog.subtypes(kind)[0]
        _subtype = State(initialValue: first)
        _identity = State(initialValue: initialTitle ?? "")
        _metric = State(initialValue: first.targetMetrics[0])
    }

    var body: some View {
        let dedicated = (kind == .retailConsumer && ["RETAIL_STORE", "RESTAURANT_FOOD", "CONSUMER_SERVICE"].contains(subtype.code))
            || (kind == .tradingDistribution && ["TRADING_WHOLESALE", "DISTRIBUTION_DEALERSHIP"].contains(subtype.code))
            || (kind == .marketingCampaigns && ["CLIENT_CAMPAIGN", "OWN_BUSINESS_CAMPAIGN"].contains(subtype.code))
            || (kind == .manufacturingProduction && ["STANDARD_PRODUCTION", "MADE_TO_ORDER"].contains(subtype.code))
        if dedicated {
            RetailStoreSetupView(
                ownerName: ownerName,
                createModel: createModel,
                onSubtype: { item in
                    subtype = item
                    metric = item.targetMetrics[0]
                },
                onClose: onClose,
                onActivate: { prefs, title in
                    createModel.submitBusinessSetup(
                        kind: kind,
                        companyId: companyId,
                        preferences: prefs,
                        title: title,
                        subtype: subtype.code,
                        editingMomentId: editingMomentId,
                        status: "ACTIVE",
                        companyName: prefs["companyName"] as? String,
                        taxIdentifier: prefs["gstin"] as? String,
                        onSuccess: onCreated
                    )
                },
                initialName: initialTitle ?? "",
                initialCompanyName: companySeed,
                initialGstin: gstSeed,
                companyId: companyId ?? ""
            )
            .task(id: editingMomentId) { await loadCompanySeed() }
            .accessibilityIdentifier(kind.maestroTag)
        } else {
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                HStack {
                    Button("Close", action: onClose).foregroundStyle(Color(hex: "#94A3B8"))
                    Spacer()
                    Text("Step \(step + 1) of 4").font(.caption).foregroundStyle(Color(hex: "#94A3B8"))
                }
                Text(kind.title).font(.title2.weight(.semibold)).foregroundStyle(.white)
                Text(kind.body).foregroundStyle(Color(hex: "#94A3B8"))
                if step == 0 { identityStep }
                if step == 1 { peopleStep }
                if step == 2 { field("Counterparties", "Optional. One name per line.", text: $counterparties) }
                if step == 3 { planStep }
                if let message = localError ?? createModel.state.error {
                    Text(message).foregroundStyle(Color(hex: "#F87171"))
                }
                HStack {
                    if step > 0 { button("Back", Color(hex: "#1E293B")) { step -= 1 } }
                    button(step < 3 ? "Continue" : "Activate", kind.activateColor) { advance() }
                }
            }
            .padding(20)
        }
        .background(Color(hex: "#0C0F15"))
        .accessibilityIdentifier(kind.maestroTag)
        }
    }

    private func loadCompanySeed() async {
        guard let editingMomentId else { return }
        let prefill = await createModel.getDomainSetupPrefill(momentId: editingMomentId)
        let existing = (companyId?.isEmpty == false ? companyId : nil) ?? prefill?.companyId
        if let existing, !existing.isEmpty {
            if let detail = try? await APIClient.shared.getCompany(companyId: existing) {
                companySeed = detail.displayName
                gstSeed = detail.taxIdentifier ?? ""
            }
        } else {
            companySeed = prefill?.title ?? initialTitle ?? ""
        }
    }

    private var identityStep: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Operating model").font(.caption).foregroundStyle(Color(hex: "#94A3B8"))
            ForEach(OperatingMomentCatalog.subtypes(kind)) { item in
                Button(item.label) {
                    subtype = item
                    metric = item.targetMetrics[0]
                }
                .buttonStyle(.bordered)
            }
            field("Moment name", "What you are running", text: $identity)
            field(subtype.partyLabel, "Who this moment is for", text: $party)
            field("Description", "Optional", text: $details)
        }
    }

    private var peopleStep: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("You are the Owner. Extra names share the role you pick.")
                .font(.caption).foregroundStyle(Color(hex: "#94A3B8"))
            ForEach(OperatingMomentCatalog.roles.prefix(4), id: \.0) { item in
                Button(item.1) { role = item.0 }.buttonStyle(.bordered)
            }
            field("Names", "Optional, one per line", text: $people)
        }
    }

    private var planStep: some View {
        VStack(alignment: .leading, spacing: 8) {
            if subtype.requiresChannel {
                field("Channels", "Comma separated", text: $channels)
            }
            field("Monthly budget", "Positive amount", text: Binding(get: { budget }, set: { budget = formatIndianAmount($0) }))
            field("Monthly target", "Positive amount or count", text: Binding(get: { target }, set: { target = formatIndianAmount($0) }))
            ForEach(subtype.targetMetrics, id: \.self) { item in
                Button(item) { metric = item }.buttonStyle(.bordered)
            }
        }
    }

    private func field(_ label: String, _ hint: String, text: Binding<String>) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label).foregroundStyle(.white)
            Text(hint).font(.caption).foregroundStyle(Color(hex: "#94A3B8"))
            TextField(label, text: text).textFieldStyle(.roundedBorder)
        }
    }

    private func button(_ label: String, _ color: Color, action: @escaping () -> Void) -> some View {
        Button(label, action: action)
            .padding(.horizontal, 16)
            .padding(.vertical, 10)
            .background(color)
            .foregroundStyle(Color(hex: "#0C0F15"))
            .clipShape(RoundedRectangle(cornerRadius: 12))
    }

    private func advance() {
        if step == 0 {
            if identity.trimmingCharacters(in: .whitespaces).isEmpty { localError = "Moment name is required."; return }
            if party.trimmingCharacters(in: .whitespaces).isEmpty { localError = "\(subtype.partyLabel) name is required."; return }
        }
        if step == 3 {
            if subtype.requiresChannel && channels.trimmingCharacters(in: .whitespaces).isEmpty {
                localError = "Select at least one channel."; return
            }
            if (Double(plainAmount(budget)) ?? 0) <= 0 { localError = "Monthly budget must be a positive amount."; return }
            if (Double(plainAmount(target)) ?? 0) <= 0 { localError = "Monthly target must be a positive amount."; return }
        }
        localError = nil
        if step < 3 {
            step += 1
            return
        }
        let participants: [[String: String]] = people.split(separator: "\n").map { String($0).trimmingCharacters(in: .whitespaces) }.filter { !$0.isEmpty }.map {
            ["displayName": $0, "role": role]
        }
        let parties: [[String: String]] = counterparties.split(separator: "\n").map { String($0).trimmingCharacters(in: .whitespaces) }.filter { !$0.isEmpty }.map {
            ["name": $0, "kind": "OTHER"]
        }
        let channelList = channels.split { $0 == "," || $0 == "\n" }.map { String($0).trimmingCharacters(in: .whitespaces) }.filter { !$0.isEmpty }
        let prefs: [String: Any] = [
            "subtype": subtype.code,
            "identityName": identity.trimmingCharacters(in: .whitespaces),
            "partyName": party.trimmingCharacters(in: .whitespaces),
            "description": details,
            "currency": "INR",
            "startDate": "",
            "participants": participants,
            "counterparties": parties,
            "channels": channelList,
            "monthlyBudget": plainAmount(budget),
            "monthlyTarget": plainAmount(target),
            "targetMetric": metric,
        ]
        createModel.submitBusinessSetup(
            kind: kind,
            companyId: companyId,
            preferences: prefs,
            title: identity.trimmingCharacters(in: .whitespaces),
            subtype: subtype.code,
            editingMomentId: editingMomentId,
            status: "ACTIVE",
            onSuccess: onCreated
        )
    }
}

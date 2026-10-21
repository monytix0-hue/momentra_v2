import SwiftUI

struct MarketingQuickAddHub: View {
    let model: OperatingProjectionPayload
    var onRecord: (MarketingEventDraft) async throws -> Void
    var onDecide: (String, String) async throws -> Void

    @State private var query = ""
    @State private var sheet: MarketingSheetKind?
    @State private var approvalOpen = false
    @State private var errorText: String?
    @State private var mastersOpen = false

    private var allowed: Set<String> { Set((model.quickAdds ?? []).map(\.code)) }
    private var owner: Bool { model.role == "OWNER_ADMIN" }
    private var approver: Bool { model.role == "APPROVER" || owner }
    private var client: Bool { model.subtype != "OWN_BUSINESS_CAMPAIGN" }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Quick Adds").font(.plusJakarta(size: 28, weight: .semibold)).foregroundStyle(MarketingChrome.ink)
            Text("Record what happened · \(model.title ?? "")").font(.plusJakarta(size: 13)).foregroundStyle(MarketingChrome.secondary)
            HStack {
                chip(OperatingMomentCatalog.roles.first { $0.0 == model.role }?.1 ?? "Marketing", filled: true)
                chip("Marketing", filled: false)
            }
            VStack(alignment: .leading, spacing: 8) {
                Text(client ? "Spend. Results. Approvals." : "Every marketing moment counts.")
                    .font(.plusJakarta(size: 18, weight: .semibold))
                    .foregroundStyle(client ? MarketingChrome.ink : Color(hex: "#EFF5F2"))
                Text(client ? "Record controlled spend, outcomes and handoffs." : "Capture spend, results, payments and approvals.")
                    .font(.plusJakarta(size: 13))
                    .foregroundStyle(client ? MarketingChrome.secondary : Color(hex: "#AFC1C3"))
                if client {
                    if let party = model.partyName, !party.isEmpty {
                        Text("Client · \(party)").font(.plusJakarta(size: 12)).foregroundStyle(MarketingChrome.secondary)
                    }
                    if let currency = model.currency {
                        Text("Currency · \(currency)").font(.plusJakarta(size: 12)).foregroundStyle(MarketingChrome.secondary)
                    }
                    let channels = model.life?.channels ?? []
                    if !channels.isEmpty {
                        Text("Channels · \(channels.joined(separator: " "))").font(.plusJakarta(size: 12)).foregroundStyle(MarketingChrome.secondary)
                    }
                    if let budget = model.budget?.amount {
                        Text("Budget · \(budget)").font(.plusJakarta(size: 12)).foregroundStyle(MarketingChrome.secondary)
                    }
                    if let target = model.target?.value {
                        Text("Target · \(target)").font(.plusJakarta(size: 12)).foregroundStyle(MarketingChrome.secondary)
                    }
                }
                if allowed.contains("SPEND") {
                    Button { sheet = .spend } label: {
                        HStack(spacing: 6) {
                            Image("qa_plus").renderingMode(.template).resizable().scaledToFit().frame(width: 14, height: 14)
                            Text("Record Spend")
                        }
                    }
                    .font(.plusJakarta(size: 14, weight: .semibold))
                    .foregroundStyle(client ? MarketingChrome.bg : Color(hex: "#66C7A4"))
                    .padding(.horizontal, 14)
                    .padding(.vertical, 8)
                    .background(client ? MarketingChrome.brand : Color(hex: "#EFF5F2"))
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                }
            }
            .padding(16)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(
                client
                    ? AnyShapeStyle(MarketingChrome.notice)
                    : AnyShapeStyle(LinearGradient(colors: [Color(hex: "#6C4EF2"), Color(hex: "#3A3842")], startPoint: .leading, endPoint: .trailing))
            )
            .clipShape(RoundedRectangle(cornerRadius: 18))
            HStack(spacing: 10) {
                Image("biz_qa_search").renderingMode(.template).resizable().scaledToFit().frame(width: 18, height: 18)
                    .foregroundStyle(MarketingChrome.ink)
                TextField("Search actions", text: $query).font(.plusJakarta(size: 14))
            }
            .padding(14)
            .background(MarketingChrome.field)
            .clipShape(RoundedRectangle(cornerRadius: 12))
            let core = MarketingSheetKind.core.filter { allowed.contains($0.eventType) && matches($0.label) }
            Text("Core actions · \(core.count)").font(.plusJakarta(size: 12)).foregroundStyle(MarketingChrome.secondary)
            LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible()), GridItem(.flexible())], spacing: 8) {
                ForEach(core) { kind in
                    Button { sheet = kind } label: {
                        VStack(spacing: 8) {
                            Image(kind.icon(client)).renderingMode(.template).resizable().scaledToFit().frame(width: 28, height: 28)
                            Text(kind.label).font(.plusJakarta(size: 13, weight: .semibold)).multilineTextAlignment(.center)
                        }
                        .foregroundStyle(MarketingChrome.ink)
                        .frame(maxWidth: .infinity, minHeight: 104)
                        .padding(12)
                        .background(MarketingChrome.card)
                        .overlay(RoundedRectangle(cornerRadius: 14).stroke(MarketingChrome.border, lineWidth: 1))
                        .clipShape(RoundedRectangle(cornerRadius: 14))
                    }
                }
            }
            Button("Masters") { mastersOpen = true }.font(.plusJakarta(size: 15, weight: .semibold))
            Text("Support & management").font(.plusJakarta(size: 12)).foregroundStyle(MarketingChrome.secondary)
            supportGrid
            if approver {
                Button("Review approvals") { approvalOpen = true }
                    .font(.plusJakarta(size: 15, weight: .semibold))
                    .foregroundStyle(MarketingChrome.accent)
            }
            if let errorText { Text(errorText).foregroundStyle(Color(hex: "#F87171")) }
        }
        .sheet(isPresented: $mastersOpen) {
            OperatingCaptureSheet(
                title: "Masters",
                icon: "qa_shield",
                eventType: "UPDATE",
                mode: "MASTERS",
                model: model,
                onSubmit: { draft in
                    try await onRecord(MarketingEventDraft(
                        eventType: draft.eventType,
                        title: draft.title,
                        amount: draft.amount,
                        note: draft.note,
                        channelName: draft.channelName,
                        counterpartyName: draft.counterpartyName,
                        paymentStatus: draft.paymentStatus,
                        occurredAt: draft.occurredAt,
                        quantity: draft.quantity,
                        unit: draft.unit,
                        unitPrice: draft.unitPrice,
                        itemName: draft.itemName,
                        parentEventId: draft.parentEventId,
                        revisesEventId: draft.revisesEventId,
                        masterKind: draft.masterKind,
                        dueAt: draft.dueAt,
                        contract: draft.contract,
                        lineItems: draft.lineItems
                    ))
                },
                onDecide: onDecide
            )
        }
        .sheet(item: $sheet) { kind in
            OperatingCaptureSheet(
                title: kind.sheetTitle(client),
                icon: kind.icon(client),
                eventType: kind.eventType,
                mode: kind.captureMode,
                model: model,
                channelLabel: "Channel",
                variant: client ? "client" : "own",
                onSubmit: { draft in
                    try await onRecord(MarketingEventDraft(
                        eventType: draft.eventType,
                        title: draft.title,
                        amount: draft.amount,
                        note: draft.note,
                        channelName: draft.channelName,
                        counterpartyName: draft.counterpartyName,
                        paymentStatus: draft.paymentStatus,
                        occurredAt: draft.occurredAt,
                        quantity: draft.quantity,
                        unit: draft.unit,
                        unitPrice: draft.unitPrice,
                        itemName: draft.itemName,
                        parentEventId: draft.parentEventId,
                        revisesEventId: draft.revisesEventId,
                        masterKind: draft.masterKind,
                        dueAt: draft.dueAt,
                        contract: draft.contract,
                        lineItems: draft.lineItems
                    ))
                },
                onDecide: onDecide
            )
        }
        .sheet(isPresented: $approvalOpen) {
            OperatingCaptureSheet(
                title: "Review Approval",
                icon: "qa_shield",
                eventType: "APPROVAL",
                mode: "APPROVAL",
                model: model,
                variant: client ? "client" : "own",
                onSubmit: { _ in },
                onDecide: onDecide
            )
        }
    }

    private var supportGrid: some View {
        let items: [(String, MarketingSheetKind?, String?)] = (client ? [
            ("People", MarketingSheetKind.person, owner ? nil : "Owner only"),
            ("Partners", .partner, owner ? nil : "Owner only"),
            ("Channels", .channels, owner ? nil : "Owner only"),
        ] : [
            ("People & Roles", .role, owner ? nil : "Owner only"),
            ("Partners & Channels", .partner, owner ? nil : "Owner only"),
        ]).filter { matches($0.0) }
        return LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible()), GridItem(.flexible())], spacing: 8) {
            ForEach(Array(items.enumerated()), id: \.offset) { _, item in
                Button {
                    if item.0 == "Approval review", item.2 == nil { approvalOpen = true }
                    else if let kind = item.1, item.2 == nil { sheet = kind }
                } label: {
                    VStack(spacing: 8) {
                        Image(item.1?.icon(client) ?? "qa_shield")
                            .renderingMode(.template).resizable().scaledToFit().frame(width: 28, height: 28)
                        Text(item.0).font(.plusJakarta(size: 12, weight: .semibold)).multilineTextAlignment(.center)
                        if let lock = item.2 {
                            Text(lock).font(.plusJakarta(size: 10))
                        }
                    }
                    .foregroundStyle(item.2 == nil ? MarketingChrome.ink : MarketingChrome.secondary)
                    .frame(maxWidth: .infinity, minHeight: 104)
                    .padding(12)
                    .background(MarketingChrome.card)
                    .overlay(RoundedRectangle(cornerRadius: 14).stroke(MarketingChrome.border, lineWidth: 1))
                    .clipShape(RoundedRectangle(cornerRadius: 14))
                }
                .disabled(item.2 != nil)
            }
        }
    }

    private func matches(_ label: String) -> Bool {
        query.isEmpty || label.localizedCaseInsensitiveContains(query)
    }

    private func chip(_ text: String, filled: Bool) -> some View {
        Text(text)
            .font(.plusJakarta(size: 12))
            .foregroundStyle(filled ? MarketingChrome.bg : MarketingChrome.secondary)
            .padding(.horizontal, 10)
            .padding(.vertical, 6)
            .background(filled ? MarketingChrome.brand : Color.clear)
            .overlay(Capsule().stroke(filled ? MarketingChrome.brand : MarketingChrome.border, lineWidth: 1))
            .clipShape(Capsule())
    }
}

enum MarketingSheetKind: String, Identifiable {
    case spend, result, payment, collection, budget, target, update, issue, approval, deliverable, vendorCost, complete, person, role, partner, channels
    var id: String { rawValue }
    var label: String {
        switch self {
        case .spend: return "Spend"
        case .result: return "Result"
        case .payment: return "Payment"
        case .collection: return "Collection"
        case .budget: return "Budget"
        case .target: return "Target"
        case .update: return "Update"
        case .issue: return "Issue"
        case .approval: return "Approval"
        case .deliverable: return "Deliverable"
        case .vendorCost: return "Vendor cost"
        case .complete: return "Complete"
        case .person: return "People"
        case .role: return "People & Roles"
        case .partner: return "Partners"
        case .channels: return "Channels"
        }
    }
    func icon(_ client: Bool) -> String {
        switch self {
        case .spend: return client ? "qa_receipt" : "biz_qa_wallet"
        case .result: return client ? "qa_message" : "qa_budget"
        case .payment: return "qa_credit_card"
        case .collection: return "qa_arrow_left_right"
        case .budget: return "qa_budget"
        case .target: return client ? "qa_alert" : "qa_budget"
        case .update, .channels: return "qa_message"
        case .issue: return "qa_alert"
        case .approval: return "qa_shield"
        case .deliverable: return client ? "qa_clipboard" : "qa_message"
        case .vendorCost: return client ? "biz_qa_wallet" : "qa_network"
        case .complete: return client ? "qa_check" : "qa_clipboard"
        case .person: return "qa_user_plus"
        case .role: return "biz_qa_users"
        case .partner: return "qa_network"
        }
    }
    func sheetTitle(_ client: Bool) -> String {
        switch self {
        case .spend: return "Add Spend / Expense"
        case .result: return "Add Result"
        case .payment: return "Add Payment"
        case .collection: return client ? "Client Billing / Collection" : "Add Collection"
        case .budget: return "Add Budget"
        case .target: return client ? "Target Revision" : "Add Target"
        case .update: return "Add Update"
        case .issue: return "Add Issue"
        case .approval: return "Request Approval"
        case .deliverable: return "Add Deliverable / Milestone"
        case .vendorCost: return "Add Vendor / Creator Cost"
        case .complete: return "Mark Result / Deliverable Complete"
        case .person, .role: return "Add or invite person"
        case .partner, .channels: return client ? "Client / Partners / Channels" : "Partners & Channels"
        }
    }
    var captureMode: String {
        switch self {
        case .spend: return "SPEND"
        case .result: return "RESULT"
        case .payment: return "PAYMENT"
        case .collection: return "COLLECTION"
        case .budget: return "BUDGET"
        case .target: return "TARGET"
        case .update, .approval: return "UPDATE"
        case .issue: return "ISSUE"
        case .deliverable: return "DELIVERABLE"
        case .vendorCost: return "VENDOR"
        case .complete: return "COMPLETE"
        case .person: return "PERSON"
        case .role: return "ROLE"
        case .partner, .channels: return "PARTNER"
        }
    }
    var title: String {
        switch self {
        case .spend: return "Add Spend"
        case .result: return "Add Result"
        case .payment: return "Add Payment"
        case .collection: return "Add Collection"
        case .budget: return "Add Budget"
        case .target: return "Add Target"
        case .update: return "Add Update"
        case .issue: return "Add Issue"
        case .approval: return "Request Approval"
        case .deliverable: return "Add Deliverable"
        case .vendorCost: return "Add Vendor Cost"
        case .complete: return "Mark Complete"
        case .person: return "People"
        case .role: return "People & Roles"
        case .partner: return "Partners"
        case .channels: return "Channels"
        }
    }
    var eventType: String {
        switch self {
        case .spend: return "SPEND"
        case .result: return "RESULT"
        case .payment: return "PAYMENT"
        case .collection: return "COLLECTION"
        case .budget: return "BUDGET"
        case .target: return "TARGET"
        case .issue: return "ISSUE"
        case .approval: return "APPROVAL"
        case .deliverable: return "DELIVERABLE"
        case .vendorCost: return "VENDOR_COST"
        case .complete: return "COMPLETE"
        default: return "UPDATE"
        }
    }
    var needsAmount: Bool {
        switch self {
        case .spend, .result, .payment, .collection, .budget, .target, .vendorCost: return true
        default: return false
        }
    }
    var needsChannel: Bool { self == .spend || self == .result || self == .collection }
    var needsParty: Bool { self == .payment || self == .vendorCost || self == .person || self == .role || self == .partner }
    var paymentStatus: String? {
        switch self {
        case .payment, .collection: return "PENDING"
        default: return nil
        }
    }
    static let core: [MarketingSheetKind] = [.spend, .result, .payment, .collection, .budget, .target, .update, .issue, .approval, .deliverable, .vendorCost, .complete]
}

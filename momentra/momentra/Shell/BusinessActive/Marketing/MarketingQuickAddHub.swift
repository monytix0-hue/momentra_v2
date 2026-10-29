import SwiftUI

struct MarketingQuickAddHub: View {
    let model: OperatingProjectionPayload
    var onRecord: (MarketingEventDraft) async throws -> Void
    var onDecide: (String, String) async throws -> Void

    @State private var query = ""
    @State private var presented: HubSheet?
    @State private var errorText: String?

    private enum HubSheet: Identifiable {
        case action(MarketingSheetKind)
        case masters
        case approval
        var id: String {
            switch self {
            case .action(let kind): return "action-\(kind.id)"
            case .masters: return "masters"
            case .approval: return "approval"
            }
        }
    }

    private var allowed: Set<String> { Set((model.quickAdds ?? []).map(\.code)) }
    private var owner: Bool { model.role == "OWNER_ADMIN" }
    private var approver: Bool { model.role == "APPROVER" || owner }
    private var client: Bool { model.subtype != "OWN_BUSINESS_CAMPAIGN" }

    var body: some View {
        if let presented {
            captureForm(presented)
        } else {
            tileList
        }
    }

    private var tileList: some View {
        ScrollView {
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
                    Button { presented = .action(.spend) } label: {
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
            .overlay(RoundedRectangle(cornerRadius: 18).stroke(MarketingChrome.brand.opacity(0.3), lineWidth: 1))
            .shadow(color: MarketingChrome.brand.opacity(0.4), radius: 24, y: 8)
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
            tileRows(core) { kind in
                actionTile(kind.label, icon: kind.icon(client), locked: nil) { presented = .action(kind) }
            }
            Button("Masters") { presented = .masters }.font(.plusJakarta(size: 15, weight: .semibold))
            Text("Support & management").font(.plusJakarta(size: 12)).foregroundStyle(MarketingChrome.secondary)
            supportGrid
            if approver {
                Button("Review approvals") { presented = .approval }
                    .font(.plusJakarta(size: 15, weight: .semibold))
                    .foregroundStyle(MarketingChrome.accent)
            }
            if let errorText { Text(errorText).foregroundStyle(Color(hex: "#F87171")) }
        }
        .padding(.horizontal, 17)
        .padding(.vertical, 20)
        }
    }

    @ViewBuilder private func captureForm(_ item: HubSheet) -> some View {
        switch item {
            case .masters:
                OperatingCaptureSheet(
                    title: "Masters",
                    icon: "qa_shield",
                    eventType: "UPDATE",
                    mode: "MASTERS",
                    model: model,
                    onSubmit: { draft in try await onRecord(marketingDraft(draft)) },
                    onDecide: onDecide,
                    onDismiss: { presented = nil }
                )
            case .action(let kind):
                OperatingCaptureSheet(
                    title: kind.sheetTitle(client),
                    icon: kind.icon(client),
                    eventType: kind.eventType,
                    mode: kind.captureMode,
                    model: model,
                    channelLabel: "Channel",
                    variant: client ? "client" : "own",
                    onSubmit: { draft in try await onRecord(marketingDraft(draft)) },
                    onDecide: onDecide,
                    onDismiss: { presented = nil }
                )
            case .approval:
                OperatingCaptureSheet(
                    title: "Review Approval",
                    icon: "qa_shield",
                    eventType: "APPROVAL",
                    mode: "APPROVAL",
                    model: model,
                    variant: client ? "client" : "own",
                    onSubmit: { _ in },
                    onDecide: onDecide,
                    onDismiss: { presented = nil }
                )
        }
    }

    private func marketingDraft(_ draft: OperatingCaptureDraft) -> MarketingEventDraft {
        MarketingEventDraft(
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
            lineItems: draft.lineItems,
            reportingMode: draft.reportingMode,
            metricCode: draft.metricCode,
            budgetCategory: draft.budgetCategory,
            activityStatus: draft.activityStatus,
            attributedSales: draft.attributedSales,
            revisionKind: draft.revisionKind,
            costBasis: draft.costBasis,
            overlapDeduped: draft.overlapDeduped,
            creativeVersion: draft.creativeVersion,
            resultSource: draft.resultSource,
            idempotencyKey: draft.idempotencyKey,
            effectiveFrom: draft.effectiveFrom,
            periodStart: draft.periodStart,
            periodEnd: draft.periodEnd,
            supersedesEventId: draft.supersedesEventId
        )
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
        return tileRows(items) { item in
            actionTile(item.0, icon: item.1?.icon(client) ?? "qa_shield", locked: item.2) {
                if item.0 == "Approval review", item.2 == nil { presented = .approval }
                else if let kind = item.1, item.2 == nil { presented = .action(kind) }
            }
        }
    }

    private func tileRows<Item>(_ items: [Item], tile: @escaping (Item) -> some View) -> some View {
        let rows = stride(from: 0, to: items.count, by: 3).map { start in
            Array(items[start..<min(start + 3, items.count)])
        }
        return VStack(spacing: 8) {
            ForEach(Array(rows.enumerated()), id: \.offset) { _, row in
                HStack(alignment: .top, spacing: 8) {
                    ForEach(Array(row.enumerated()), id: \.offset) { _, item in
                        tile(item)
                    }
                    if row.count < 3 {
                        ForEach(0..<(3 - row.count), id: \.self) { _ in
                            Color.clear.frame(maxWidth: .infinity)
                        }
                    }
                }
            }
        }
    }

    private func actionTile(_ title: String, icon: String, locked: String?, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            VStack(spacing: 8) {
                Image(icon).renderingMode(.template).resizable().scaledToFit().frame(width: 28, height: 28)
                Text(title).font(.plusJakarta(size: 13, weight: .semibold)).multilineTextAlignment(.center)
                if let locked {
                    Text(locked).font(.plusJakarta(size: 10))
                }
            }
            .foregroundStyle(locked == nil ? MarketingChrome.ink : MarketingChrome.secondary)
            .frame(maxWidth: .infinity, minHeight: 104)
            .padding(12)
            .background(MarketingChrome.card)
            .overlay(RoundedRectangle(cornerRadius: 14).stroke(MarketingChrome.border, lineWidth: 1))
            .clipShape(RoundedRectangle(cornerRadius: 14))
        }
        .buttonStyle(.plain)
        .disabled(locked != nil)
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
    case activity, progress, spend, result, payment, invoice, collection, revision, update, issue, approval, deliverable, vendorCost, complete, person, role, partner, channels, budget, target
    var id: String { rawValue }
    var label: String {
        switch self {
        case .activity: return "Add Activity"
        case .progress: return "Update Progress"
        case .invoice: return "Record Invoice"
        case .revision: return "Budget / Target Revision"
        case .spend: return "Record Spend"
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
        case .activity, .update, .channels: return "qa_message"
        case .progress, .result: return client ? "qa_message" : "qa_budget"
        case .invoice, .collection: return "qa_arrow_left_right"
        case .revision, .budget, .target: return "qa_budget"
        case .spend: return client ? "qa_receipt" : "biz_qa_wallet"
        case .payment: return "qa_credit_card"
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
        case .activity: return "Add Activity"
        case .progress: return "Update Progress"
        case .invoice: return "Record Invoice"
        case .revision: return "Budget / Target Revision"
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
        case .activity: return "UPDATE"
        case .progress, .result: return "RESULT"
        case .invoice: return "SALE"
        case .revision, .budget: return "BUDGET"
        case .spend: return "SPEND"
        case .payment: return "PAYMENT"
        case .collection: return "COLLECTION"
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
        case .activity: return "Add Activity"
        case .progress: return "Update Progress"
        case .invoice: return "Record Invoice"
        case .revision: return "Budget / Target Revision"
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
        case .activity: return "ACTIVITY"
        case .progress: return "PROGRESS"
        case .invoice: return "INVOICE"
        case .revision: return "REVISION"
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
        case .spend, .result, .payment, .invoice, .collection, .revision, .budget, .target, .vendorCost, .progress: return true
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
    static let core: [MarketingSheetKind] = [.activity, .progress, .spend, .payment, .invoice, .collection, .revision, .approval, .update, .issue, .result]
}

import SwiftUI

struct DistributionQuickAddHub: View {
    let model: OperatingProjectionPayload
    var onRecord: (DistributionEventDraft) async throws -> Void
    var onDecide: (String, String) async throws -> Void

    @State private var query = ""
    @State private var sheet: DistributionSheetKind?
    @State private var approvalOpen = false
    @State private var errorText: String?
    @State private var mastersOpen = false

    private var allowed: Set<String> { Set((model.quickAdds ?? []).map(\.code)) }
    private var owner: Bool { model.role == "OWNER_ADMIN" }
    private var approver: Bool { model.role == "APPROVER" || owner }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Quick Adds").font(.plusJakarta(size: 28, weight: .semibold)).foregroundStyle(DistributionChrome.ink)
            Text("Record what happened · \(model.title ?? "")").font(.plusJakarta(size: 13)).foregroundStyle(DistributionChrome.secondary)
            HStack {
                chip(OperatingMomentCatalog.roles.first { $0.0 == model.role }?.1 ?? "Distribution", filled: true)
                chip("Distribution", filled: false)
            }
            VStack(alignment: .leading, spacing: 8) {
                Text("Move. Sell. Collect.").font(.plusJakarta(size: 18, weight: .semibold)).foregroundStyle(DistributionChrome.ink)
                Text("Capture a sale, a cost, or a note while it is still clear.")
                    .font(.plusJakarta(size: 13)).foregroundStyle(DistributionChrome.secondary)
                if allowed.contains("SALE") {
                    Button { sheet = .sale } label: {
                        HStack(spacing: 6) {
                            Image("qa_plus").renderingMode(.template).resizable().scaledToFit().frame(width: 14, height: 14)
                            Text("Record Sale")
                        }
                    }
                    .font(.plusJakarta(size: 14, weight: .semibold))
                    .foregroundStyle(DistributionChrome.bg)
                    .padding(.horizontal, 14)
                    .padding(.vertical, 8)
                    .background(DistributionChrome.brand)
                    .clipShape(Capsule())
                }
            }
            .padding(16)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(DistributionChrome.notice)
            .clipShape(RoundedRectangle(cornerRadius: 18))
            HStack(spacing: 10) {
                Image("biz_qa_search").renderingMode(.template).resizable().scaledToFit().frame(width: 18, height: 18)
                    .foregroundStyle(DistributionChrome.secondary)
                TextField("Search actions", text: $query).font(.plusJakarta(size: 14))
            }
            .padding(14)
            .background(DistributionChrome.field)
            .clipShape(RoundedRectangle(cornerRadius: 12))
            let core = DistributionSheetKind.core.filter { allowed.contains($0.eventType) && matches($0.label) }
            Text("Core actions · \(core.count)").font(.plusJakarta(size: 12)).foregroundStyle(DistributionChrome.secondary)
            LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible()), GridItem(.flexible())], spacing: 8) {
                ForEach(core) { kind in
                    Button { sheet = kind } label: {
                        VStack(spacing: 8) {
                            Image(kind.icon).renderingMode(.template).resizable().scaledToFit().frame(width: 28, height: 28)
                            Text(kind.label).font(.plusJakarta(size: 13, weight: .semibold)).multilineTextAlignment(.center)
                        }
                        .foregroundStyle(DistributionChrome.ink)
                        .frame(maxWidth: .infinity, minHeight: 104)
                        .padding(12)
                        .background(DistributionChrome.card)
                        .overlay(RoundedRectangle(cornerRadius: 14).stroke(DistributionChrome.border, lineWidth: 1))
                        .clipShape(RoundedRectangle(cornerRadius: 14))
                    }
                }
            }
            Button("Masters") { mastersOpen = true }.font(.plusJakarta(size: 15, weight: .semibold))
            Text("Support & management").font(.plusJakarta(size: 12)).foregroundStyle(DistributionChrome.secondary)
            supportGrid
            if let errorText { Text(errorText).foregroundStyle(Color(hex: "#F87171")) }
        }
        .sheet(isPresented: $mastersOpen) {
            OperatingCaptureSheet(
                title: "Masters",
                icon: "qa_shield",
                eventType: "UPDATE",
                mode: "MASTERS",
                model: model,
                onSubmit: { draft in try await onRecord(draft.toDistribution()) },
                onDecide: onDecide
            )
        }
        .sheet(item: $sheet) { kind in
            OperatingCaptureSheet(
                title: kind.title,
                icon: kind.icon,
                eventType: kind.eventType,
                mode: operatingCaptureMode(eventType: kind.eventType, title: kind.title),
                model: model,
                channelLabel: kind == .sale ? "Sales channel" : (kind == .collection ? "Buyer / dealer" : "Channel"),
                variant: "distribution",
                onSubmit: { draft in try await onRecord(draft.toDistribution()) },
                onDecide: onDecide,
                onCreateMaster: { kind, name, unit in
                    try await onRecord(DistributionEventDraft(
                        eventType: "UPDATE",
                        title: name,
                        amount: nil,
                        note: nil,
                        channelName: nil,
                        counterpartyName: nil,
                        paymentStatus: nil,
                        unit: unit,
                        masterKind: kind
                    ))
                }
            )
        }
        .onAppear {
            switch OperatingQuickAddHint.value {
            case "shipment": sheet = .shipment
            case "stock": sheet = .stock
            case "budget": sheet = .budget
            default: break
            }
            OperatingQuickAddHint.value = nil
        }
        .sheet(isPresented: $approvalOpen) {
            OperatingCaptureSheet(
                title: "Review Approval",
                icon: "qa_shield",
                eventType: "APPROVAL",
                mode: "APPROVAL",
                model: model,
                variant: "distribution",
                onSubmit: { _ in },
                onDecide: onDecide
            )
        }
    }

    private var supportGrid: some View {
        let items: [(String, DistributionSheetKind?, String?)] = [
            ("Reimbursement", .reimburse, allowed.contains("PAYMENT") ? nil : "Finance only"),
            ("Approval review", nil, approver ? nil : "Approver only"),
            ("Add person", .person, owner && allowed.contains("UPDATE") ? nil : "Owner only"),
            ("Role & access", .role, owner && allowed.contains("UPDATE") ? nil : "Owner only"),
            ("Suppliers & channels", .supplier, allowed.contains("UPDATE") ? nil : "View only"),
            ("Invite & QR", .invite, owner && allowed.contains("UPDATE") ? nil : "Owner only"),
        ].filter { matches($0.0) }
        return LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible()), GridItem(.flexible())], spacing: 8) {
            ForEach(Array(items.enumerated()), id: \.offset) { _, item in
                Button {
                    if item.0 == "Approval review", item.2 == nil { approvalOpen = true }
                    else if let kind = item.1, item.2 == nil { sheet = kind }
                } label: {
                    VStack(spacing: 8) {
                        Image(item.0 == "Approval review" ? "qa_shield" : (item.1?.icon ?? "qa_shield"))
                            .renderingMode(.template).resizable().scaledToFit().frame(width: 28, height: 28)
                        Text(item.0).font(.plusJakarta(size: 12, weight: .semibold)).multilineTextAlignment(.center)
                        if let lock = item.2 {
                            Text(lock).font(.plusJakarta(size: 10))
                        }
                    }
                    .foregroundStyle(item.2 == nil ? DistributionChrome.ink : DistributionChrome.secondary)
                    .frame(maxWidth: .infinity, minHeight: 104)
                    .padding(12)
                    .background(DistributionChrome.card)
                    .overlay(RoundedRectangle(cornerRadius: 14).stroke(DistributionChrome.border, lineWidth: 1))
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
            .foregroundStyle(filled ? DistributionChrome.bg : DistributionChrome.secondary)
            .padding(.horizontal, 10)
            .padding(.vertical, 6)
            .background(filled ? DistributionChrome.brand : Color.clear)
            .overlay(Capsule().stroke(filled ? DistributionChrome.brand : DistributionChrome.border, lineWidth: 1))
            .clipShape(Capsule())
    }
}

enum DistributionSheetKind: String, Identifiable {
    case sale, purchase, spend, payment, collection, budget, update, issue, stock, order, shipment, returnNote, reimburse, person, role, invite, supplier
    var id: String { rawValue }
    var label: String {
        switch self {
        case .sale: return "Sale"
        case .purchase: return "Purchase"
        case .spend: return "Expense"
        case .payment: return "Payment"
        case .collection: return "Collection"
        case .budget: return "Budget"
        case .update: return "Update"
        case .issue: return "Issue"
        case .stock: return "Stock"
        case .order: return "Order"
        case .shipment: return "Shipment"
        case .returnNote: return "Return"
        case .reimburse: return "Reimbursement"
        case .person: return "Add person"
        case .role: return "Role & access"
        case .invite: return "Invite & QR"
        case .supplier: return "Add Supplier"
        }
    }
    var icon: String {
        switch self {
        case .sale, .spend, .returnNote: return "qa_receipt"
        case .purchase: return "biz_qa_wallet"
        case .payment, .reimburse: return "qa_credit_card"
        case .collection: return "qa_arrow_left_right"
        case .budget: return "qa_budget"
        case .update: return "qa_message"
        case .issue: return "qa_alert"
        case .stock: return "qa_package"
        case .order: return "qa_truck"
        case .shipment: return "qa_package"
        case .person: return "qa_user_plus"
        case .role: return "biz_qa_users"
        case .invite: return "qa_qr"
        case .supplier: return "qa_network"
        }
    }
    var title: String {
        switch self {
        case .sale: return "Record Sale"
        case .purchase: return "Record Purchase"
        case .spend: return "Record Expense"
        case .payment: return "Record Payment"
        case .collection: return "Record Collection"
        case .budget: return "Revise Budget"
        case .update: return "Post Update"
        case .issue: return "Report Issue"
        case .stock: return "Stock / Availability Update"
        case .order: return "Order / Dispatch Update"
        case .shipment: return "Delivery Update"
        case .returnNote: return "Return / Credit Note"
        case .reimburse: return "Record Reimbursement"
        case .person, .role, .invite: return "Add or invite person"
        case .supplier: return "Add Principal / Supplier"
        }
    }
    var eventType: String {
        switch self {
        case .sale: return "SALE"
        case .purchase: return "PURCHASE"
        case .spend: return "SPEND"
        case .payment, .reimburse: return "PAYMENT"
        case .collection: return "COLLECTION"
        case .budget: return "BUDGET"
        case .issue: return "ISSUE"
        case .stock: return "STOCK"
        case .order: return "ORDER"
        case .shipment: return "SHIPMENT"
        case .returnNote: return "RETURN"
        default: return "UPDATE"
        }
    }
    var needsAmount: Bool {
        switch self {
        case .update, .issue, .stock, .order, .shipment, .person, .role, .invite, .supplier: return false
        default: return true
        }
    }
    var needsChannel: Bool { self == .sale || self == .collection }
    var needsParty: Bool { self == .purchase || self == .payment || self == .reimburse || self == .person || self == .role || self == .invite || self == .supplier }
    var paymentStatus: String? {
        switch self {
        case .payment, .collection, .reimburse: return "PENDING"
        default: return nil
        }
    }
    static let core: [DistributionSheetKind] = [.sale, .purchase, .spend, .payment, .collection, .update, .issue, .stock, .order, .shipment, .returnNote]
}

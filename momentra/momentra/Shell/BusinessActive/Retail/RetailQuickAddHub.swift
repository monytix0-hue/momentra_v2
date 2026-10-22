import SwiftUI

struct RetailQuickAddHub: View {
    let model: OperatingProjectionPayload
    var onRecord: (RetailEventDraft) async throws -> Void
    var onDecide: (String, String) async throws -> Void

    @State private var query = ""
    @State private var sheet: RetailSheetKind?
    @State private var approvalOpen = false
    @State private var errorText: String?
    @State private var mastersOpen = false

    private var allowed: Set<String> { Set((model.quickAdds ?? []).map(\.code)) }
    private var owner: Bool { model.role == "OWNER_ADMIN" }
    private var approver: Bool { model.role == "APPROVER" || owner }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Quick Adds").font(.plusJakarta(size: 28, weight: .semibold)).foregroundStyle(.white)
            Text("Record what happened · \(model.title ?? "")").font(.plusJakarta(size: 13)).foregroundStyle(RetailStoreChrome.secondary)
            HStack {
                chip(OperatingMomentCatalog.roles.first { $0.0 == model.role }?.1 ?? "Retail Store", filled: true)
                chip("Retail Store", filled: false)
            }
            VStack(alignment: .leading, spacing: 8) {
                Text("Every store moment counts.").font(.plusJakarta(size: 18, weight: .semibold)).foregroundStyle(.white)
                Text("Capture a sale, a cost, or a note while it is still clear.")
                    .font(.plusJakarta(size: 13)).foregroundStyle(RetailStoreChrome.secondary)
                if allowed.contains("SALE") {
                    Button { sheet = .sale } label: {
                        HStack(spacing: 6) {
                            Image("qa_plus").renderingMode(.template).resizable().scaledToFit().frame(width: 14, height: 14)
                            Text("Record Sale")
                        }
                    }
                    .font(.plusJakarta(size: 14, weight: .semibold))
                    .foregroundStyle(RetailStoreChrome.bg)
                    .padding(.horizontal, 14)
                    .padding(.vertical, 8)
                    .background(.white)
                    .clipShape(Capsule())
                }
            }
            .padding(16)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(RetailStoreChrome.notice)
            .clipShape(RoundedRectangle(cornerRadius: 18))
            HStack(spacing: 10) {
                Image("biz_qa_search").renderingMode(.template).resizable().scaledToFit().frame(width: 18, height: 18)
                    .foregroundStyle(RetailStoreChrome.secondary)
                TextField("Search actions", text: $query).font(.plusJakarta(size: 14))
            }
            .padding(14)
            .background(RetailStoreChrome.field)
            .clipShape(RoundedRectangle(cornerRadius: 12))
            let core = RetailSheetKind.core.filter { allowed.contains($0.eventType) && matches($0.label) }
            Text("Core actions · \(core.count)").font(.plusJakarta(size: 12)).foregroundStyle(RetailStoreChrome.secondary)
            LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible()), GridItem(.flexible())], spacing: 8) {
                ForEach(core) { kind in
                    Button { sheet = kind } label: {
                        VStack(spacing: 8) {
                            Image(kind.icon).renderingMode(.template).resizable().scaledToFit().frame(width: 28, height: 28)
                            Text(kind.label).font(.plusJakarta(size: 13, weight: .semibold)).multilineTextAlignment(.center)
                        }
                        .foregroundStyle(.white)
                        .frame(maxWidth: .infinity, minHeight: 104)
                        .padding(12)
                        .background(RetailStoreChrome.card)
                        .overlay(RoundedRectangle(cornerRadius: 14).stroke(RetailStoreChrome.border, lineWidth: 1))
                        .clipShape(RoundedRectangle(cornerRadius: 14))
                    }
                }
            }
            Button("Masters") { mastersOpen = true }.font(.plusJakarta(size: 15, weight: .semibold))
            Text("Support & management").font(.plusJakarta(size: 12)).foregroundStyle(RetailStoreChrome.secondary)
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
                onSubmit: { draft in
                    try await onRecord(RetailEventDraft(
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
                title: kind.title,
                icon: kind.icon,
                eventType: kind.eventType,
                mode: operatingCaptureMode(eventType: kind.eventType, title: kind.title),
                model: model,
                channelLabel: "Sales channel",
                onSubmit: { draft in
                    try await onRecord(RetailEventDraft(
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
                onSubmit: { _ in },
                onDecide: onDecide
            )
        }
    }

    private var supportGrid: some View {
        let items: [(String, RetailSheetKind?, String?)] = [
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
                    .foregroundStyle(item.2 == nil ? Color.white : RetailStoreChrome.secondary)
                    .frame(maxWidth: .infinity, minHeight: 104)
                    .padding(12)
                    .background(RetailStoreChrome.card)
                    .overlay(RoundedRectangle(cornerRadius: 14).stroke(RetailStoreChrome.border, lineWidth: 1))
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
            .foregroundStyle(filled ? Color.white : RetailStoreChrome.secondary)
            .padding(.horizontal, 10)
            .padding(.vertical, 6)
            .background(filled ? RetailStoreChrome.brand : Color.clear)
            .overlay(Capsule().stroke(filled ? RetailStoreChrome.brand : RetailStoreChrome.border, lineWidth: 1))
            .clipShape(Capsule())
    }
}

enum RetailSheetKind: String, Identifiable {
    case sale, purchase, spend, payment, collection, budget, update, issue, stock, order, status, revision, reimburse, person, role, invite, supplier
    var id: String { rawValue }
    var label: String {
        switch self {
        case .sale: return "Record Sale"
        case .purchase: return "Purchase"
        case .spend: return "Expense"
        case .payment: return "Payment"
        case .collection: return "Collection"
        case .budget, .revision: return "Budget / Target"
        case .update: return "Issue / Update"
        case .issue: return "Issue"
        case .stock: return "Stock"
        case .order: return "Order"
        case .status: return "Status"
        case .reimburse: return "Reimbursement"
        case .person: return "Add person"
        case .role: return "Role & access"
        case .invite: return "Invite & QR"
        case .supplier: return "Add Supplier"
        }
    }
    var icon: String {
        switch self {
        case .sale, .spend: return "qa_receipt"
        case .purchase, .reimburse: return "biz_qa_wallet"
        case .payment: return "qa_credit_card"
        case .collection: return "qa_arrow_left_right"
        case .budget, .revision: return "qa_budget"
        case .update: return "qa_message"
        case .issue: return "qa_alert"
        case .stock, .order, .status: return "qa_package"
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
        case .budget, .revision: return "Budget / Target Revision"
        case .update: return "Issue / Approval / Update"
        case .issue: return "Report Issue"
        case .stock: return "Stock Update"
        case .order: return "Order"
        case .status: return "Status / Assignment"
        case .reimburse: return "Record Reimbursement"
        case .person: return "Add person"
        case .role: return "Role & access"
        case .invite: return "Invite & QR"
        case .supplier: return "Add supplier"
        }
    }
    var eventType: String {
        switch self {
        case .sale: return "SALE"
        case .purchase: return "PURCHASE"
        case .spend: return "SPEND"
        case .payment, .reimburse: return "PAYMENT"
        case .collection: return "COLLECTION"
        case .budget, .revision: return "BUDGET"
        case .issue: return "ISSUE"
        case .stock: return "STOCK"
        case .order, .status: return "ORDER"
        default: return "UPDATE"
        }
    }
    var needsAmount: Bool {
        switch self {
        case .update, .issue, .stock, .order, .status, .person, .role, .invite, .supplier: return false
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
    static let core: [RetailSheetKind] = [.sale, .purchase, .collection, .payment, .spend, .stock, .order, .status, .revision, .update]
}

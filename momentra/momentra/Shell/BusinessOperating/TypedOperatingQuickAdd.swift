import SwiftUI

/**
 Callers: OperatingMomentHubView.quickAddForm.
 No prior typed Quick Add form on iOS main path after V4 rebuild (confirmed via Glob).
 Reads: OperatingProjectionPayload.quickAdds + paymentReadiness/activity for parents.
 Writes: none locally — POST via APIClient.recordOperatingEvent + Idempotency-Key.
 User: "Build typed forms for all nine Business Moment types."
 User: "No generic fallback form for Quick Adds requiring typed data."
 User: "No unsupported Client-only actions in Own Campaign."
 User: "Validate required fields before submission." / "Use idempotency keys."
 */

// Callers: quickAddFieldSpec + TypedOperatingQuickAddForm. GST collected, not computed.
// User: "Support item lines, GST, parties... where required."
enum QuickAddField: String, CaseIterable {
    case title, amount, note, counterparty, channel, paymentStatus, dueAt, parent
    case quantity, unit, itemName, unitPrice, expenseCategory, stockAction, stockReason
    case returnReason, revisionKind, metricCode, periodStart, periodEnd, activityStatus
    case costBasis, rejectedQty, location, tableNumber, covers, attributedSales
    case taxMode, gstRate
}

struct QuickAddFieldSpec {
    let fields: Set<QuickAddField>
    let required: Set<QuickAddField>
}

func quickAddFieldSpec(eventType: String, subtype: String?) -> QuickAddFieldSpec {
    let retailish = ["RETAIL_STORE", "RESTAURANT_FOOD", "CONSUMER_SERVICE"].contains(subtype ?? "")
    let own = subtype == "OWN_BUSINESS_CAMPAIGN"
    let marketing = ["CLIENT_CAMPAIGN", "OWN_BUSINESS_CAMPAIGN"].contains(subtype ?? "")

    if own && ["INVOICE", "COLLECTION"].contains(eventType) {
        return QuickAddFieldSpec(fields: [], required: [])
    }

    switch eventType {
    case "SALE", "SERVICE":
        var fields: Set<QuickAddField> = [
            .title, .amount, .counterparty, .channel, .paymentStatus, .dueAt,
            .itemName, .quantity, .unit, .unitPrice, .taxMode, .gstRate, .note,
        ]
        if subtype == "RESTAURANT_FOOD" { fields.formUnion([.tableNumber, .covers]) }
        if subtype == "MADE_TO_ORDER" { fields.insert(.parent) }
        var required: Set<QuickAddField> = [.title, .amount]
        if retailish { required.insert(.channel) }
        return QuickAddFieldSpec(fields: fields, required: required)
    case "INVOICE":
        return QuickAddFieldSpec(
            fields: [.title, .amount, .counterparty, .dueAt, .taxMode, .gstRate, .note],
            required: [.title, .amount, .counterparty]
        )
    case "PURCHASE":
        return QuickAddFieldSpec(
            fields: [
                .title, .amount, .counterparty, .paymentStatus, .dueAt,
                .itemName, .quantity, .unit, .unitPrice, .taxMode, .gstRate, .note,
            ],
            required: [.title, .amount, .counterparty]
        )
    case "SPEND":
        var fields: Set<QuickAddField> = [
            .title, .amount, .counterparty, .expenseCategory, .paymentStatus, .note,
        ]
        if marketing && !own { fields.insert(.costBasis) }
        return QuickAddFieldSpec(fields: fields, required: [.title, .amount, .expenseCategory])
    case "PAYMENT", "COLLECTION":
        return QuickAddFieldSpec(
            fields: [.title, .amount, .parent, .counterparty, .note],
            required: [.title, .amount, .parent]
        )
    case "STOCK":
        return QuickAddFieldSpec(
            fields: [.title, .itemName, .quantity, .unit, .stockAction, .stockReason, .location, .note],
            required: [.title, .itemName, .quantity, .stockAction]
        )
    case "ORDER", "CUSTOMER_ORDER":
        var required: Set<QuickAddField> = [.title, .counterparty]
        if retailish { required.insert(.channel) }
        return QuickAddFieldSpec(
            fields: [
                .title, .amount, .counterparty, .channel, .dueAt, .itemName, .quantity, .unit, .note,
            ],
            required: required
        )
    case "SHIPMENT", "DELIVERY":
        return QuickAddFieldSpec(
            fields: [.title, .parent, .quantity, .unit, .counterparty, .note],
            required: [.title, .quantity, .parent]
        )
    case "RETURN":
        return QuickAddFieldSpec(
            fields: [.title, .amount, .parent, .returnReason, .quantity, .itemName, .taxMode, .gstRate, .note],
            required: [.title, .amount, .returnReason]
        )
    case "PRODUCTION":
        return QuickAddFieldSpec(
            fields: [.title, .itemName, .quantity, .unit, .rejectedQty, .parent, .note],
            required: [.title, .quantity]
        )
    case "APPOINTMENT":
        return QuickAddFieldSpec(fields: [.title, .counterparty, .note], required: [.title])
    case "BUDGET", "TARGET":
        return QuickAddFieldSpec(
            fields: [.title, .amount, .metricCode, .note],
            required: [.title, .amount]
        )
    case "REVISION":
        return QuickAddFieldSpec(
            fields: [.title, .amount, .revisionKind, .metricCode, .note],
            required: [.title, .revisionKind]
        )
    case "PROGRESS":
        return QuickAddFieldSpec(
            fields: [.title, .parent, .activityStatus, .amount, .quantity, .note],
            required: [.title, .parent]
        )
    case "RESULT":
        return QuickAddFieldSpec(
            fields: [.title, .amount, .attributedSales, .periodStart, .periodEnd, .metricCode, .note],
            required: [.title]
        )
    case "ACTIVITY", "UPDATE", "ISSUE", "APPROVAL":
        return QuickAddFieldSpec(fields: [.title, .note, .amount], required: [.title])
    default:
        return QuickAddFieldSpec(fields: [.title, .amount, .note], required: [.title])
    }
}

struct TypedQuickAddDraft {
    var title = ""
    var amount = ""
    var note = ""
    var counterparty = ""
    var channel = ""
    var paymentStatus = "UNPAID"
    var dueAt = ""
    var parentEventId = ""
    var quantity = ""
    var unit = "pc"
    var itemName = ""
    var unitPrice = ""
    var expenseCategory = ""
    var stockAction = "SET"
    var stockReason = ""
    var returnReason = ""
    var revisionKind = "BUDGET"
    var metricCode = ""
    var periodStart = ""
    var periodEnd = ""
    var activityStatus = ""
    var costBasis = "AGENCY"
    var rejectedQty = ""
    var location = ""
    var tableNumber = ""
    var covers = ""
    var attributedSales = ""
    var taxMode = "EXCLUSIVE"
    var gstRate = ""
}

func validateTypedQuickAdd(
    eventType: String,
    subtype: String?,
    needsAmount: Bool,
    draft: TypedQuickAddDraft
) -> [String] {
    let spec = quickAddFieldSpec(eventType: eventType, subtype: subtype)
    if spec.fields.isEmpty { return ["This Quick Add is not available for this Moment type."] }
    var errors: [String] = []
    func req(_ field: QuickAddField, _ value: String, _ label: String) {
        if spec.required.contains(field), value.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            errors.append("\(label) is required")
        }
    }
    req(.title, draft.title, "Title")
    if needsAmount || spec.required.contains(.amount) { req(.amount, draft.amount, "Amount") }
    req(.counterparty, draft.counterparty, "Party")
    req(.channel, draft.channel, "Channel")
    req(.parent, draft.parentEventId, "Linked source")
    req(.quantity, draft.quantity, "Quantity")
    req(.itemName, draft.itemName, "Item")
    req(.expenseCategory, draft.expenseCategory, "Expense category")
    req(.stockAction, draft.stockAction, "Stock action")
    req(.returnReason, draft.returnReason, "Return reason")
    req(.revisionKind, draft.revisionKind, "Revision kind")
    return errors
}

struct TypedOperatingQuickAddForm: View {
    let momentId: String
    let projection: OperatingProjectionPayload?
    var onSaved: () -> Void = {}

    @State private var selected = ""
    @State private var draft = TypedQuickAddDraft()
    @State private var errors: [String] = []
    @State private var confirm: String?
    @State private var submitting = false

    private let accent = Color(red: 0.20, green: 0.83, blue: 0.60)

    private var adds: [OperatingQuickAddPayload] {
        let raw = projection?.quickAdds ?? []
        if projection?.subtype == "OWN_BUSINESS_CAMPAIGN" {
            return raw.filter { $0.code != "INVOICE" && $0.code != "COLLECTION" }
        }
        return raw
    }

    private var spec: QuickAddFieldSpec {
        quickAddFieldSpec(eventType: selected, subtype: projection?.subtype)
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("Quick Add")
                .font(.system(size: 15, weight: .medium))
                .foregroundStyle(Color.white.opacity(0.9))
            if adds.isEmpty {
                Text("Your role cannot add to this moment.")
                    .font(.system(size: 13))
                    .foregroundStyle(Color.white.opacity(0.55))
            }
            ForEach(adds) { add in
                chip(add.label, on: add.code == selected) {
                    selected = add.code
                    errors = []
                    confirm = nil
                }
            }
            if selected.isEmpty, let first = adds.first {
                Color.clear.onAppear { selected = first.code }
            }

            if spec.fields.contains(.title) { field("Title *", text: $draft.title) }
            if spec.fields.contains(.amount) {
                let needs = adds.first(where: { $0.code == selected })?.needsAmount == true
                    || spec.required.contains(.amount)
                field(needs ? "Amount *" : "Amount", text: $draft.amount)
            }
            if spec.fields.contains(.counterparty) {
                field(spec.required.contains(.counterparty) ? "Party *" : "Party", text: $draft.counterparty)
            }
            if spec.fields.contains(.channel) {
                let channels = projection?.life?.channels ?? []
                if channels.isEmpty {
                    field(spec.required.contains(.channel) ? "Channel *" : "Channel", text: $draft.channel)
                } else {
                    Text("Channel").font(.system(size: 11)).foregroundStyle(Color.white.opacity(0.55))
                    ForEach(channels, id: \.self) { ch in
                        chip(ch, on: draft.channel == ch) { draft.channel = ch }
                    }
                }
            }
            if spec.fields.contains(.paymentStatus) {
                choice("Payment", ["PAID", "PARTIAL", "UNPAID"], draft.paymentStatus) {
                    draft.paymentStatus = $0
                }
            }
            if spec.fields.contains(.dueAt) { field("Due at (ISO date)", text: $draft.dueAt) }
            if spec.fields.contains(.parent) { parentPicker }
            if spec.fields.contains(.itemName) {
                field(spec.required.contains(.itemName) ? "Item *" : "Item", text: $draft.itemName)
            }
            if spec.fields.contains(.quantity) {
                field(spec.required.contains(.quantity) ? "Quantity *" : "Quantity", text: $draft.quantity)
            }
            if spec.fields.contains(.unit) { field("Unit", text: $draft.unit) }
            if spec.fields.contains(.unitPrice) { field("Unit price", text: $draft.unitPrice) }
            // Callers: SALE/PURCHASE/INVOICE/RETURN. Server applies GST; client collects only.
            // User: "Support item lines, GST... No client-side authoritative financial calculations."
            if spec.fields.contains(.taxMode) {
                choice("Tax mode", ["EXCLUSIVE", "INCLUSIVE", "NONE"], draft.taxMode) {
                    draft.taxMode = $0
                }
            }
            if spec.fields.contains(.gstRate) {
                field("GST rate % (server applies)", text: $draft.gstRate)
            }
            if spec.fields.contains(.expenseCategory) {
                choice(
                    "Expense category",
                    ["Rent", "Utilities", "Payroll", "Marketing", "Logistics", "Consumables", "Other"],
                    draft.expenseCategory.isEmpty ? "Other" : draft.expenseCategory
                ) { draft.expenseCategory = $0 }
            }
            if spec.fields.contains(.stockAction) {
                choice("Stock action", ["SET", "ADD", "REMOVE", "TRANSFER"], draft.stockAction) {
                    draft.stockAction = $0
                }
            }
            if spec.fields.contains(.stockReason) { field("Stock reason", text: $draft.stockReason) }
            if spec.fields.contains(.returnReason) { field("Return reason *", text: $draft.returnReason) }
            if spec.fields.contains(.revisionKind) {
                choice("Revision", ["BUDGET", "TARGET", "CATEGORY"], draft.revisionKind) {
                    draft.revisionKind = $0
                }
            }
            if spec.fields.contains(.metricCode) { field("Metric code", text: $draft.metricCode) }
            if spec.fields.contains(.periodStart) { field("Period start", text: $draft.periodStart) }
            if spec.fields.contains(.periodEnd) { field("Period end", text: $draft.periodEnd) }
            if spec.fields.contains(.activityStatus) {
                choice(
                    "Status",
                    ["PLANNED", "IN_PROGRESS", "DONE", "BLOCKED"],
                    draft.activityStatus.isEmpty ? "IN_PROGRESS" : draft.activityStatus
                ) { draft.activityStatus = $0 }
            }
            if spec.fields.contains(.costBasis) {
                choice("Cost basis", ["AGENCY", "REIMBURSABLE"], draft.costBasis) { draft.costBasis = $0 }
            }
            if spec.fields.contains(.rejectedQty) { field("Rejected quantity", text: $draft.rejectedQty) }
            if spec.fields.contains(.location) { field("Location", text: $draft.location) }
            if spec.fields.contains(.tableNumber) { field("Table", text: $draft.tableNumber) }
            if spec.fields.contains(.covers) { field("Covers", text: $draft.covers) }
            if spec.fields.contains(.attributedSales) { field("Attributed sales", text: $draft.attributedSales) }
            if spec.fields.contains(.note) { field("Note", text: $draft.note) }

            ForEach(errors, id: \.self) { err in
                Text(err).font(.system(size: 12)).foregroundStyle(Color.red.opacity(0.9))
            }
            if let confirm {
                Text(confirm).font(.system(size: 13)).foregroundStyle(accent)
            }

            Button(submitting ? "Saving…" : "Add") {
                Task { await submit() }
            }
            .disabled(submitting || selected.isEmpty)
            .font(.system(size: 15, weight: .semibold))
            .foregroundStyle(Color.black)
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
            .background(accent)
            .clipShape(RoundedRectangle(cornerRadius: 12))
        }
    }

    @ViewBuilder
    private var parentPicker: some View {
        Text(selected == "PAYMENT" || selected == "COLLECTION" ? "Linked source *" : "Linked source")
            .font(.system(size: 11))
            .foregroundStyle(Color.white.opacity(0.55))
        let parents: [(String, String)] = {
            var rows: [(String, String)] = []
            for p in projection?.paymentReadiness ?? [] {
                rows.append((p.id, "\(p.title) · \(p.amount) · \(p.status)"))
            }
            for a in projection?.activity ?? [] where [
                "SALE", "PURCHASE", "INVOICE", "ORDER", "CUSTOMER_ORDER", "SHIPMENT", "ACTIVITY",
            ].contains(a.eventType) {
                rows.append((a.id, "\(a.eventType) · \(a.title)"))
            }
            var seen = Set<String>()
            return rows.filter { seen.insert($0.0).inserted }
        }()
        if parents.isEmpty {
            Text("No eligible source records yet.")
                .font(.system(size: 12))
                .foregroundStyle(Color.white.opacity(0.55))
            field("Parent event id", text: $draft.parentEventId)
        } else {
            ForEach(Array(parents.prefix(12)), id: \.0) { id, label in
                chip(label, on: draft.parentEventId == id) { draft.parentEventId = id }
            }
        }
    }

    private func field(_ label: String, text: Binding<String>) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label).font(.system(size: 11)).foregroundStyle(Color.white.opacity(0.55))
            TextField(label, text: text)
                .textFieldStyle(.plain)
                .padding(12)
                .background(Color(red: 0.09, green: 0.11, blue: 0.15))
                .clipShape(RoundedRectangle(cornerRadius: 12))
                .foregroundStyle(Color.white)
        }
    }

    private func choice(
        _ label: String,
        _ options: [String],
        _ selected: String,
        onSelect: @escaping (String) -> Void
    ) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(label).font(.system(size: 11)).foregroundStyle(Color.white.opacity(0.55))
            HStack(spacing: 8) {
                ForEach(options, id: \.self) { opt in
                    chip(opt, on: opt == selected) { onSelect(opt) }
                }
            }
        }
    }

    private func chip(_ label: String, on: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(label)
                .font(.system(size: 12))
                .foregroundStyle(on ? Color.black : Color.white.opacity(0.9))
                .padding(.horizontal, 10)
                .padding(.vertical, 8)
                .background(on ? accent : Color(red: 0.09, green: 0.11, blue: 0.15))
                .clipShape(RoundedRectangle(cornerRadius: 10))
        }
    }

    private func submit() async {
        let needs = adds.first(where: { $0.code == selected })?.needsAmount == true
        let validation = validateTypedQuickAdd(
            eventType: selected,
            subtype: projection?.subtype,
            needsAmount: needs,
            draft: draft
        )
        if !validation.isEmpty {
            errors = validation
            return
        }
        errors = []
        submitting = true
        let idem = UUID().uuidString
        let title = draft.title.trimmingCharacters(in: .whitespacesAndNewlines)
        do {
            // Callers: recordOperatingEvent. GST rate operator-entered; server owns totals.
            // User: "No client-side authoritative financial calculations."
            let contract: OperatingContractInput? = {
                let has =
                    !draft.expenseCategory.isEmpty || !draft.stockAction.isEmpty
                    || !draft.returnReason.isEmpty || !draft.location.isEmpty
                    || !draft.tableNumber.isEmpty || !draft.covers.isEmpty
                    || !draft.gstRate.isEmpty || draft.taxMode != "EXCLUSIVE"
                guard has else { return nil }
                // Argument order matches OperatingContractInput member order.
                return OperatingContractInput(
                    stockAction: draft.stockAction.nilIfBlank,
                    stockReason: draft.stockReason.nilIfBlank,
                    expenseCategory: draft.expenseCategory.nilIfBlank,
                    taxMode: draft.taxMode.nilIfBlank,
                    returnReason: draft.returnReason.nilIfBlank,
                    gstRate: draft.gstRate.nilIfBlank,
                    locationName: draft.location.nilIfBlank,
                    tableNumber: draft.tableNumber.nilIfBlank,
                    covers: draft.covers.nilIfBlank
                )
            }()
            let lines: [(itemName: String, quantity: String?, unit: String?, unitPrice: String?, masterId: String?, taxMode: String?, gstRate: String?)]? = {
                let name = draft.itemName.trimmingCharacters(in: .whitespacesAndNewlines)
                guard !name.isEmpty else { return nil }
                return [(
                    itemName: name,
                    quantity: draft.quantity.nilIfBlank,
                    unit: draft.unit.nilIfBlank,
                    unitPrice: draft.unitPrice.nilIfBlank,
                    masterId: nil,
                    taxMode: draft.taxMode.nilIfBlank,
                    gstRate: draft.gstRate.nilIfBlank
                )]
            }()
            _ = try await APIClient.shared.recordOperatingEvent(
                momentId: momentId,
                eventType: selected,
                title: title,
                amount: draft.amount.nilIfBlank,
                note: draft.note.nilIfBlank,
                channelName: draft.channel.nilIfBlank,
                counterpartyName: draft.counterparty.nilIfBlank,
                paymentStatus: draft.paymentStatus.nilIfBlank,
                parentEventId: draft.parentEventId.nilIfBlank,
                quantity: draft.quantity.nilIfBlank,
                unit: draft.unit.nilIfBlank,
                dueAt: draft.dueAt.nilIfBlank,
                contract: contract,
                lineItems: lines,
                metricCode: draft.metricCode.nilIfBlank,
                activityStatus: draft.activityStatus.nilIfBlank,
                attributedSales: draft.attributedSales.nilIfBlank,
                revisionKind: draft.revisionKind.nilIfBlank,
                rejectedQuantity: draft.rejectedQty.nilIfBlank,
                costBasis: draft.costBasis.nilIfBlank,
                idempotencyKey: idem,
                periodStart: draft.periodStart.nilIfBlank,
                periodEnd: draft.periodEnd.nilIfBlank
            )
            confirm = "Saved \(title)"
            draft.title = ""
            draft.amount = ""
            draft.note = ""
            draft.quantity = ""
            draft.itemName = ""
            onSaved()
        } catch {
            // Preserve draft after validation / stale-source failures.
            errors = [error.localizedDescription]
        }
        submitting = false
    }
}

private extension String {
    var nilIfBlank: String? {
        let t = trimmingCharacters(in: .whitespacesAndNewlines)
        return t.isEmpty ? nil : t
    }
}

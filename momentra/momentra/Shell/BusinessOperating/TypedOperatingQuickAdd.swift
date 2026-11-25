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
    case "HOLD", "TRANSFER", "REPRINT":
        return QuickAddFieldSpec(fields: [.title, .note, .itemName, .quantity, .unit], required: [.title])
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
    /// Part-paid amount received/paid now (sticky + APK parity).
    var receivedAmount = ""
    /// PURCHASE goods: "Received now" | "Receive later".
    var receiptState = "Received now"
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
    var taxMode = "NONE"
    var gstRate = ""
    var supplyChoice = ""
    var highlightSupply = false
    var highlightGstRate = false
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
    /// When set (sheet host), preselect this event and hide the chip strip.
    var initialCode: String? = nil
    var accentOverride: Color? = nil
    /// When true, sticky meta + CTA sit below an internal ScrollView (sheet host).
    var pinStickyFooter: Bool = true
    var onSaved: () -> Void = {}

    @State private var selected = ""
    @State private var draft = TypedQuickAddDraft()
    @State private var errors: [String] = []
    @State private var confirm: String?
    @State private var submitting = false
    @State private var lovKind: String? = nil

    private var accent: Color {
        accentOverride ?? Color(red: 0.20, green: 0.83, blue: 0.60)
    }

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

    private var isTradingFamily: Bool {
        let code = (projection?.subtype ?? "").uppercased()
        return code == "TRADING_WHOLESALE" || code == "DISTRIBUTION_DEALERSHIP"
    }

    private var isDistribution: Bool {
        (projection?.subtype ?? "").uppercased() == "DISTRIBUTION_DEALERSHIP"
    }

    private var contextLine: String? {
        guard isTradingFamily, !selected.isEmpty else { return nil }
        return isDistribution
            ? DistributionQuickAddCopy.context(mode: selected, brand: projection?.title)
            : TradingQuickAddCopy.context(mode: selected, brand: projection?.title)
    }

    private var whyBody: String? {
        guard isTradingFamily, !selected.isEmpty else { return nil }
        return isDistribution
            ? DistributionQuickAddCopy.why(mode: selected)
            : TradingQuickAddCopy.why(mode: selected)
    }

    private var primaryCta: String {
        guard isTradingFamily, !selected.isEmpty else { return submitting ? "Saving…" : "Add" }
        if submitting { return "Saving…" }
        return isDistribution
            ? DistributionQuickAddCopy.cta(mode: selected)
            : TradingQuickAddCopy.cta(mode: selected)
    }

    // Callers: TradingAmountHero (L371) + stickyMeta. projection.currency is String?.
    // No alternate currency helper. Reads OperatingProjectionPayload.currency only.
    // User: "Value of optional type 'String?' must be unwrapped to a value of type 'String'"
    private var currencyCode: String {
        let trimmed = (projection?.currency ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
        return trimmed.isEmpty ? "INR" : trimmed
    }

    /// Prefer qty × unitPrice when both set; else typed amount.
    private var effectiveAmountRaw: String? {
        let qty = Decimal(string: draft.quantity.trimmingCharacters(in: .whitespacesAndNewlines))
        let price = Decimal(string: draft.unitPrice.trimmingCharacters(in: .whitespacesAndNewlines))
        if let qty, let price, qty > 0, price > 0 {
            return NSDecimalNumber(decimal: qty * price).stringValue
        }
        let typed = draft.amount.trimmingCharacters(in: .whitespacesAndNewlines)
        return typed.isEmpty ? nil : typed
    }

    private var stickyPaymentChoice: String? {
        let status = draft.paymentStatus.uppercased()
        let mode = selected.uppercased()
        switch status {
        case "PAID":
            return mode == "SALE" ? "Received" : "Paid"
        case "PARTIAL":
            return "Part paid"
        case "UNPAID":
            return "Pay later"
        default:
            return draft.paymentStatus.isEmpty ? nil : draft.paymentStatus
        }
    }

    private var stickyFooter: String? {
        guard isTradingFamily, !selected.isEmpty else { return nil }
        return TradingQuickAddCopy.stickyMeta(
            mode: selected,
            currency: currencyCode,
            amountRaw: effectiveAmountRaw,
            receivedRaw: draft.receivedAmount.nilIfBlank,
            paymentChoice: stickyPaymentChoice,
            receiptState: selected.uppercased() == "PURCHASE" ? draft.receiptState.nilIfBlank : nil,
            quantity: draft.quantity.nilIfBlank,
            unit: draft.unit.nilIfBlank,
            approvalChoice: nil
        )
    }

    private var showsQtyHero: Bool {
        guard isTradingFamily else { return false }
        switch selected.uppercased() {
        case "STOCK", "SHIPMENT", "DELIVERY": return true
        default: return false
        }
    }

    private var showsPartPaidField: Bool {
        guard isTradingFamily, draft.paymentStatus.uppercased() == "PARTIAL" else { return false }
        switch selected.uppercased() {
        case "SALE", "PURCHASE", "SPEND": return true
        default: return false
        }
    }

    /// Money modes that should show amount-first hero + ₹ sticky (not STOCK/SHIPMENT/ops).
    private var showsMoneyAmountHero: Bool {
        guard isTradingFamily, spec.fields.contains(.amount) else { return false }
        switch selected.uppercased() {
        case "SALE", "PURCHASE", "COLLECTION", "PAYMENT", "REIMBURSE",
             "SPEND", "RETURN", "ORDER", "CUSTOMER_ORDER",
             "BUDGET", "TARGET", "INVOICE":
            return true
        default:
            return false
        }
    }

    private var amountHeroLabel: String {
        switch selected.uppercased() {
        case "SALE": return "Sale amount (M)"
        case "PURCHASE": return "Purchase amount (M)"
        case "COLLECTION": return "Collection amount (M)"
        case "PAYMENT", "REIMBURSE": return "Payment amount (M)"
        case "SPEND": return "Spend amount (M)"
        case "RETURN": return "Credit amount (M)"
        case "ORDER", "CUSTOMER_ORDER": return "Commitment amount"
        case "BUDGET", "TARGET": return "New amount (M)"
        case "INVOICE": return "Invoice amount (M)"
        default: return "Amount"
        }
    }

    var body: some View {
        Group {
            if pinStickyFooter {
                VStack(spacing: 0) {
                    ScrollView {
                        formFields
                            .padding(16)
                    }
                    formFooter
                        .padding(.horizontal, 16)
                        .padding(.vertical, 12)
                        .background(Color(red: 0.04, green: 0.05, blue: 0.09))
                }
            } else {
                VStack(alignment: .leading, spacing: 10) {
                    formFields
                    formFooter
                }
            }
        }
        // Callers: TypedOperatingQuickAddForm.body. LOV types live in SearchableOperatingLovSheet.swift.
        // Extracted to help the type checker (avoids "unable to type-check" + cascading "not in scope").
        // User: "Cannot find 'SearchableOperatingLovSheet' in scope"
        .sheet(item: lovSheetItem, content: lovSheet)
    }

    private var lovSheetItem: Binding<CaptureSheetCode?> {
        Binding(
            get: { lovKind.map { CaptureSheetCode(code: $0) } },
            set: { lovKind = $0?.code }
        )
    }

    @ViewBuilder
    private func lovSheet(_ item: CaptureSheetCode) -> some View {
        let kind = item.code
        let masters = projection?.masters ?? []
        SearchableOperatingLovSheet(
            kind: kind,
            title: operatingLovTitle(for: kind),
            options: tradingLovOptions(kind: kind, masters: masters, local: []),
            canAdd: true,
            accent: accent,
            onSelect: { option in applyLovSelection(kind: kind, option: option) },
            onAdd: { name, unit, conversion in
                Task { await createLovMaster(kind: kind, name: name, unit: unit, conversion: conversion) }
            },
            onDismiss: { lovKind = nil }
        )
    }

    @ViewBuilder
    private var formFields: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("Quick Add")
                .font(.system(size: 15, weight: .medium))
                .foregroundStyle(Color.white.opacity(0.9))
            if let contextLine {
                Text(contextLine)
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundStyle(accent)
            }
            if adds.isEmpty {
                Text("Your role cannot add to this moment.")
                    .font(.system(size: 13))
                    .foregroundStyle(Color.white.opacity(0.55))
            }
            if initialCode == nil {
                ForEach(adds) { add in
                    chip(add.label, on: add.code == selected) {
                        selected = add.code
                        errors = []
                        confirm = nil
                    }
                }
            }
            Color.clear
                .frame(height: 0)
                .onAppear {
                    if selected.isEmpty {
                        if let initial = initialCode?.trimmingCharacters(in: .whitespacesAndNewlines), !initial.isEmpty {
                            selected = initial.uppercased()
                        } else if let first = adds.first {
                            selected = first.code
                        }
                    }
                }

            if showsMoneyAmountHero {
                let needs = adds.first(where: { $0.code == selected })?.needsAmount == true
                    || spec.required.contains(.amount)
                let lineTotal: String? = {
                    let qty = Decimal(string: draft.quantity.trimmingCharacters(in: .whitespacesAndNewlines))
                    let price = Decimal(string: draft.unitPrice.trimmingCharacters(in: .whitespacesAndNewlines))
                    if let qty, let price, qty > 0, price > 0 {
                        return NSDecimalNumber(decimal: qty * price).stringValue
                    }
                    return nil
                }()
                TradingAmountHero(
                    label: amountHeroLabel,
                    currency: currencyCode,
                    accent: accent,
                    text: $draft.amount,
                    hint: needs ? ">0" : "Optional",
                    readOnly: lineTotal != nil
                )
                .onChange(of: draft.quantity) { _, _ in syncAmountFromLine() }
                .onChange(of: draft.unitPrice) { _, _ in syncAmountFromLine() }
            }

            if showsQtyHero {
                let unitPill = draft.unit.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
                    ? "pc" : draft.unit.trimmingCharacters(in: .whitespacesAndNewlines)
                TradingAmountHero(
                    label: selected.uppercased() == "STOCK" ? "Quantity (M)" : "Qty delivered (M)",
                    currency: "Qty · \(unitPill)",
                    accent: accent,
                    text: $draft.quantity,
                    hint: selected.uppercased() == "STOCK" ? "Shows prior → delta → result" : "≤ remaining",
                    isQuantity: true,
                    unitOverride: "Qty · \(unitPill)"
                )
            }

            if spec.fields.contains(.title) { field("Title *", text: $draft.title) }
            if !showsMoneyAmountHero, spec.fields.contains(.amount) {
                let needs = adds.first(where: { $0.code == selected })?.needsAmount == true
                    || spec.required.contains(.amount)
                field(needs ? "Amount *" : "Amount", text: $draft.amount)
            }
            if spec.fields.contains(.counterparty) {
                if isTradingFamily {
                    lovField(
                        spec.required.contains(.counterparty) ? "Party *" : "Party",
                        value: draft.counterparty,
                        kind: ["PURCHASE", "PAYMENT", "SPEND"].contains(selected.uppercased())
                            ? "SUPPLIER" : "BUYER"
                    )
                } else {
                    field(spec.required.contains(.counterparty) ? "Party *" : "Party", text: $draft.counterparty)
                }
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
            if showsPartPaidField {
                field(
                    selected.uppercased() == "SALE" ? "Received now" : "Paid now",
                    text: $draft.receivedAmount
                )
            }
            if isTradingFamily, selected.uppercased() == "PURCHASE" {
                choice("Goods", ["Received now", "Receive later"], draft.receiptState) {
                    draft.receiptState = $0
                }
            }
            if spec.fields.contains(.dueAt) { field("Due at (ISO date)", text: $draft.dueAt) }
            if spec.fields.contains(.parent) { parentPicker }
            if spec.fields.contains(.itemName) {
                if isTradingFamily {
                    lovField(
                        spec.required.contains(.itemName) ? "Item *" : "Item",
                        value: draft.itemName,
                        kind: "PRODUCT"
                    )
                } else {
                    field(spec.required.contains(.itemName) ? "Item *" : "Item", text: $draft.itemName)
                }
            }
            if spec.fields.contains(.quantity), !showsQtyHero {
                field(spec.required.contains(.quantity) ? "Quantity *" : "Quantity", text: $draft.quantity)
            }
            if spec.fields.contains(.unit) {
                if isTradingFamily {
                    lovField("Unit", value: draft.unit, kind: "UOM")
                } else {
                    field("Unit", text: $draft.unit)
                }
            }
            if spec.fields.contains(.unitPrice) { field("Unit price", text: $draft.unitPrice) }
            if spec.fields.contains(.taxMode) {
                choice("Tax mode", ["NONE", "EXCLUSIVE", "INCLUSIVE"], draft.taxMode) {
                    draft.taxMode = $0
                    draft.highlightSupply = false
                    draft.highlightGstRate = false
                    if $0 == "NONE" { draft.supplyChoice = "" }
                }
            }
            if spec.fields.contains(.gstRate), draft.taxMode != "NONE" {
                field("GST rate % (server applies)", text: $draft.gstRate)
                choice(
                    "Supply",
                    ["Inside your state", "Outside your state"],
                    draft.supplyChoice == "INTRA"
                        ? "Inside your state"
                        : draft.supplyChoice == "INTER" ? "Outside your state" : ""
                ) {
                    draft.supplyChoice = $0.hasPrefix("Inside") ? "INTRA" : "INTER"
                    draft.highlightSupply = false
                }
                if draft.highlightSupply || draft.supplyChoice.isEmpty {
                    Text("Confirm whether this supply is inside your state or outside it.")
                        .font(.system(size: 12))
                        .foregroundStyle(draft.highlightSupply ? Color.red.opacity(0.9) : Color.white.opacity(0.45))
                }
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

            if let whyBody {
                VStack(alignment: .leading, spacing: 4) {
                    Text("WHY")
                        .font(.system(size: 10, weight: .bold))
                        .foregroundStyle(Color.white.opacity(0.45))
                    Text(whyBody)
                        .font(.system(size: 12))
                        .foregroundStyle(Color.white.opacity(0.7))
                }
                .padding(12)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(Color.white.opacity(0.04))
                .clipShape(RoundedRectangle(cornerRadius: 12))
            }

            ForEach(errors, id: \.self) { err in
                Text(err).font(.system(size: 12)).foregroundStyle(Color.red.opacity(0.9))
            }
            if let confirm {
                Text(confirm).font(.system(size: 13)).foregroundStyle(accent)
            }
        }
    }

    @ViewBuilder
    private var formFooter: some View {
        VStack(alignment: .leading, spacing: 10) {
            if let stickyFooter {
                Text(stickyFooter)
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundStyle(accent)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 10)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(accent.opacity(0.10))
                    .clipShape(RoundedRectangle(cornerRadius: 12))
            }

            Button(primaryCta) {
                Task { await submit() }
            }
            .disabled(submitting || selected.isEmpty)
            .font(.system(size: 15, weight: .semibold))
            .foregroundStyle(Color.black)
            .frame(maxWidth: .infinity)
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
            .background(accent)
            .clipShape(RoundedRectangle(cornerRadius: 12))
        }
    }

    private func lovField(_ label: String, value: String, kind: String) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label).font(.system(size: 11)).foregroundStyle(Color.white.opacity(0.55))
            Button {
                lovKind = kind
            } label: {
                HStack {
                    Text(value.isEmpty ? "Search \(operatingLovTitle(for: kind).lowercased())" : value)
                        .foregroundStyle(value.isEmpty ? Color.white.opacity(0.45) : Color.white)
                    Spacer()
                    Image(systemName: "chevron.down")
                        .font(.system(size: 11, weight: .semibold))
                        .foregroundStyle(Color.white.opacity(0.45))
                }
                .padding(12)
                .background(Color(red: 0.09, green: 0.11, blue: 0.15))
                .clipShape(RoundedRectangle(cornerRadius: 12))
            }
        }
    }

    private func applyLovSelection(kind: String, option: OperatingLovOption) {
        switch kind.uppercased() {
        case "PRODUCT":
            draft.itemName = option.name
            if let raw = option.unit, let unit = raw.nilIfBlank { draft.unit = unit }
            if draft.quantity.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                draft.quantity = "1"
            }
            if let master = projection?.masters?.first(where: { $0.id == option.id }) {
                if let rate = master.gstRate?.nilIfBlank { draft.gstRate = rate }
                let purchase = selected.uppercased() == "PURCHASE"
                let includes = (purchase ? master.purchasePriceIncludesGst : master.salePriceIncludesGst)?
                    .trimmingCharacters(in: .whitespacesAndNewlines)
                    .uppercased()
                switch includes {
                case "YES": draft.taxMode = "INCLUSIVE"
                case "NO": draft.taxMode = "EXCLUSIVE"
                default:
                    if draft.taxMode != "INCLUSIVE" && draft.taxMode != "EXCLUSIVE" {
                        draft.taxMode = "NONE"
                    }
                }
                if draft.unitPrice.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                    let source = purchase ? master.purchasePrice : (master.sellPrice ?? master.mrp)
                    let price = source?
                        .replacingOccurrences(of: "₹", with: "")
                        .trimmingCharacters(in: .whitespacesAndNewlines)
                    if let price, !price.isEmpty {
                        draft.unitPrice = BusinessSetupAmountFormatting.plainAmount(price)
                    }
                }
            }
            if draft.title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                draft.title = option.name
            }
            syncAmountFromLine()
        case "BUYER", "SUPPLIER", "CUSTOMER":
            draft.counterparty = option.name
        case "UOM":
            draft.unit = option.unit ?? option.name
        default:
            break
        }
        lovKind = nil
    }

    private func createLovMaster(kind: String, name: String, unit: String?, conversion: String?) async {
        let trimmed = name.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }
        let masterKind: String = {
            switch kind.uppercased() {
            case "BUYER", "CUSTOMER": return "BUYER"
            case "SUPPLIER": return "SUPPLIER"
            case "UOM": return "UOM"
            default: return "PRODUCT"
            }
        }()
        do {
            _ = try await APIClient.shared.recordOperatingEvent(
                momentId: momentId,
                eventType: "UPDATE",
                title: trimmed,
                amount: nil,
                note: nil,
                channelName: nil,
                counterpartyName: (masterKind == "BUYER" || masterKind == "SUPPLIER") ? trimmed : nil,
                paymentStatus: nil,
                occurredAt: nil,
                parentEventId: nil,
                quantity: nil,
                unit: unit,
                itemName: masterKind == "PRODUCT" ? trimmed : nil,
                masterKind: masterKind,
                conversionToBase: conversion,
                idempotencyKey: UUID().uuidString
            )
            applyLovSelection(
                kind: kind,
                option: OperatingLovOption(
                    id: "local-\(trimmed)",
                    name: trimmed,
                    kind: kind.uppercased(),
                    subtitle: unit.map { "Base unit: \($0)" },
                    unit: unit
                )
            )
        } catch {
            errors = [error.localizedDescription]
            lovKind = nil
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

    /// Keep draft.amount in sync with qty × unitPrice when both are set (Trading amount hero).
    private func syncAmountFromLine() {
        let qty = Decimal(string: draft.quantity.trimmingCharacters(in: .whitespacesAndNewlines))
        let price = Decimal(string: draft.unitPrice.trimmingCharacters(in: .whitespacesAndNewlines))
        guard let qty, let price, qty > 0, price > 0 else { return }
        draft.amount = NSDecimalNumber(decimal: qty * price).stringValue
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
        let taxOn = draft.taxMode == "INCLUSIVE" || draft.taxMode == "EXCLUSIVE"
        if taxOn, projection?.gstTracking == "YES" {
            var blocked: [String] = []
            if draft.supplyChoice != "INTRA" && draft.supplyChoice != "INTER" {
                draft.highlightSupply = true
                blocked.append("Confirm whether this supply is inside your state or outside it.")
            } else {
                draft.highlightSupply = false
            }
            let ratePlain = BusinessSetupAmountFormatting.plainAmount(draft.gstRate)
            if ratePlain.isEmpty || (Decimal(string: ratePlain) ?? 0) <= 0 {
                draft.highlightGstRate = true
                blocked.append("Choose a GST rate before saving.")
            } else {
                draft.highlightGstRate = false
            }
            if !blocked.isEmpty {
                errors = blocked
                return
            }
        }
        errors = []
        submitting = true
        let idem = UUID().uuidString
        let title = draft.title.trimmingCharacters(in: .whitespacesAndNewlines)
        do {
            // Callers: recordOperatingEvent. GST rate operator-entered; server owns totals.
            // User: "No client-side authoritative financial calculations."
            let taxWire: String = {
                switch draft.taxMode.uppercased() {
                case "INCLUSIVE": return "INCLUSIVE"
                case "EXCLUSIVE": return "EXCLUSIVE"
                default: return "NOT_CONFIGURED"
                }
            }()
            let contract: OperatingContractInput? = {
                let has =
                    !draft.expenseCategory.isEmpty || !draft.stockAction.isEmpty
                    || !draft.returnReason.isEmpty || !draft.location.isEmpty
                    || !draft.tableNumber.isEmpty || !draft.covers.isEmpty
                    || !draft.gstRate.isEmpty || draft.taxMode != "NONE"
                    || taxWire != "NOT_CONFIGURED"
                guard has else { return nil }
                var c = OperatingContractInput()
                c.stockAction = draft.stockAction.nilIfBlank
                c.stockReason = draft.stockReason.nilIfBlank
                c.expenseCategory = draft.expenseCategory.nilIfBlank
                c.taxMode = taxWire
                c.returnReason = draft.returnReason.nilIfBlank
                c.gstRate = taxOn ? draft.gstRate.nilIfBlank : nil
                c.taxJurisdiction = taxOn
                    ? (draft.supplyChoice == "INTRA" || draft.supplyChoice == "INTER" ? draft.supplyChoice : nil)
                    : nil
                c.locationName = draft.location.nilIfBlank
                c.tableNumber = draft.tableNumber.nilIfBlank
                c.covers = draft.covers.nilIfBlank
                return c
            }()
            let lines: [(itemName: String, quantity: String?, unit: String?, unitPrice: String?, masterId: String?, taxMode: String?, gstRate: String?)]? = {
                let name = draft.itemName.trimmingCharacters(in: .whitespacesAndNewlines)
                guard !name.isEmpty else { return nil }
                return [(
                    itemName: name,
                    quantity: draft.quantity.nilIfBlank,
                    unit: draft.unit.nilIfBlank,
                    unitPrice: BusinessSetupAmountFormatting.plainAmount(draft.unitPrice).nilIfBlank,
                    masterId: nil,
                    taxMode: taxOn ? taxWire : "NOT_CONFIGURED",
                    gstRate: taxOn ? draft.gstRate.nilIfBlank : nil
                )]
            }()
            _ = try await APIClient.shared.recordOperatingEvent(
                momentId: momentId,
                eventType: selected,
                title: title,
                amount: BusinessSetupAmountFormatting.plainAmount(draft.amount).nilIfBlank,
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

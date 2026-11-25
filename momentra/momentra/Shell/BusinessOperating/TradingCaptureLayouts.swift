import SwiftUI

/// One cart line for Itemized SALE / PURCHASE / ORDER (APK `savedLines` parity).
struct TradingCaptureLine: Identifiable, Equatable {
    let id: UUID
    var itemName: String
    var quantity: String
    var unit: String
    var unitPrice: String
    var masterId: String?

    init(
        id: UUID = UUID(),
        itemName: String,
        quantity: String,
        unit: String = "pc",
        unitPrice: String,
        masterId: String? = nil
    ) {
        self.id = id
        self.itemName = itemName
        self.quantity = quantity
        self.unit = unit
        self.unitPrice = unitPrice
        self.masterId = masterId
    }

    var lineTotal: Decimal? {
        guard let qty = Decimal(string: quantity.trimmingCharacters(in: .whitespacesAndNewlines)),
              let price = Decimal(string: unitPrice.trimmingCharacters(in: .whitespacesAndNewlines)),
              qty > 0, price >= 0 else { return nil }
        return qty * price
    }
}

/// Trading / Distribution capture draft — mode-shaped fields beyond the generic typed form.
struct TradingCaptureDraft {
    var title = ""
    var amount = ""
    var note = ""
    var counterparty = ""
    var channel = ""
    /// Display labels: Received/Paid / Part paid / Pay later
    var paymentChoice = "Pay later"
    var receivedAmount = ""
    var receiptState = "Received now"
    var dueAt = ""
    var occurredAt = ""
    var parentEventId = ""
    var quantity = ""
    var unit = "pc"
    var itemName = ""
    var unitPrice = ""
    var expenseGroup = "Premises"
    var expenseCategory = "Rent"
    var spendDetail = false
    var stockAction = "SET"
    var stockReason = "Stocktake"
    var returnReason = ""
    var returnDirection = "Customer return"
    var goodsMovement = "Returned to stock"
    /// QA-04 itemized SALE — Fully Delivered / Partially Delivered / Deliver Later
    var goodsStatusLabel = "Deliver Later"
    var quantityReceived = ""
    var goodsDueAt = ""
    var revisionKind = "BUDGET"
    var metricCode = ""
    var periodStart = ""
    var activityStatus = "Open confirmed"
    var location = ""
    var locationTo = ""
    var method = ""
    var carrier = ""
    var taxMode = "Not configured"
    var gstRate = ""
    /// INTRA | INTER — required only when tax is Inclusive/Exclusive.
    var supplyChoice = ""
    var highlightSupply = false
    var highlightGstRate = false
    var fxNote = ""
    var ownership = "Owned"
    var showMoreDetails = false
    var entryMode = "Quick amount"
    var orderSide = "Customer order"
    var shipmentDirection = "Outgoing"
    var holdTarget = "Order"
    var holdAction = "Hold order"
    var approvalChoice = "Approve"
    var updateKind = "Update"
    var reprintKind = "Invoice"
    var remainingHint = ""
    /// PRODUCT LOV master id for the pending itemized row (cleared when line is added).
    var productMasterId: String? = nil
    var savedLines: [TradingCaptureLine] = []
}

/// APK `ExpenseGroups` taxonomy for SPEND Detailed.
enum TradingExpenseGroups {
    static let all: [(String, [String])] = [
        ("Premises", ["Rent", "Utilities", "Maintenance"]),
        ("People", ["Salaries"]),
        ("Movement", ["Transport", "Freight"]),
        ("Selling", ["Packaging", "Marketing"]),
        ("Office", ["Office supplies", "Insurance", "Fees and charges"]),
        ("Other", ["Other"]),
    ]

    static func categories(for group: String) -> [String] {
        all.first(where: { $0.0 == group })?.1 ?? ["Other"]
    }
}

/// Mode-shaped Trading Quick Add capture (APK `OperatingCaptureSheet` trading branches).
struct TradingCaptureForm: View {
    let momentId: String
    let projection: OperatingProjectionPayload?
    let initialCode: String
    var accent: Color = Color(red: 0.06, green: 0.73, blue: 0.51)
    /// When set, Save PATCHes this event (Activity Edit / revise).
    var revisesEventId: String? = nil
    var seedActivity: OperatingActivityPayload? = nil
    var onSaved: () -> Void = {}

    @State private var mode: String = ""
    @State private var draft = TradingCaptureDraft()
    @State private var errors: [String] = []
    @State private var submitting = false
    @State private var lovKind: String? = nil
    @State private var confirm: String?
    @State private var obligationKinds: [String] = []
    @State private var obligationPrefillShipment = false
    @State private var seeded = false

    private var isDistribution: Bool {
        (projection?.subtype ?? "").uppercased() == "DISTRIBUTION_DEALERSHIP"
    }

    private var currency: String {
        projection?.currency?.trimmingCharacters(in: .whitespacesAndNewlines).nilIfBlank ?? "INR"
    }

    private var contextLine: String {
        isDistribution
            ? DistributionQuickAddCopy.context(mode: mode, brand: projection?.title)
            : TradingQuickAddCopy.context(mode: mode, brand: projection?.title)
    }

    private var whyBody: String {
        isDistribution
            ? DistributionQuickAddCopy.why(mode: mode)
            : TradingQuickAddCopy.why(mode: mode)
    }

    private var ctaLabel: String {
        if submitting { return "Saving…" }
        if mode.uppercased() == "REPRINT" { return "Close" }
        return isDistribution
            ? DistributionQuickAddCopy.cta(mode: mode)
            : TradingQuickAddCopy.cta(mode: mode)
    }

    private var linesTotalRaw: String? {
        var totals: [Decimal] = []
        for line in draft.savedLines {
            if let t = line.lineTotal { totals.append(t) }
        }
        let pendingName = draft.itemName.trimmingCharacters(in: .whitespacesAndNewlines)
        if !pendingName.isEmpty {
            let qty = draft.quantity.trimmingCharacters(in: .whitespacesAndNewlines)
            let price = BusinessSetupAmountFormatting.plainAmount(
                draft.unitPrice.trimmingCharacters(in: .whitespacesAndNewlines)
            )
            let q = qty.isEmpty && !price.isEmpty ? "1" : qty
            if let qd = Decimal(string: q), let pd = Decimal(string: price), qd > 0 {
                totals.append(qd * pd)
            }
        }
        guard !totals.isEmpty else { return nil }
        let sum = totals.reduce(0, +)
        return NSDecimalNumber(decimal: sum).stringValue
    }

    private var effectiveAmount: String? {
        if ["Itemized", "Itemised"].contains(where: { draft.entryMode.localizedCaseInsensitiveContains($0) }) {
            return linesTotalRaw ?? draft.amount.nilIfBlank
        }
        let typed = BusinessSetupAmountFormatting.plainAmount(
            draft.amount.trimmingCharacters(in: .whitespacesAndNewlines)
        )
        if !typed.isEmpty { return typed }
        return linesTotalRaw
    }

    private var stickyPaymentChoice: String? {
        draft.paymentChoice.nilIfBlank
    }

    private var stickyFooter: String {
        TradingQuickAddCopy.stickyMeta(
            mode: mode,
            currency: currency,
            amountRaw: effectiveAmount,
            receivedRaw: draft.receivedAmount.nilIfBlank,
            paymentChoice: stickyPaymentChoice,
            receiptState: mode.uppercased() == "PURCHASE" ? draft.receiptState.nilIfBlank : nil,
            quantity: draft.quantity.nilIfBlank,
            unit: draft.unit.nilIfBlank,
            approvalChoice: mode.uppercased() == "APPROVAL" ? draft.approvalChoice.nilIfBlank : nil
        )
    }

    private var paymentApiStatus: String {
        switch draft.paymentChoice.lowercased() {
        case "received", "paid": return "PAID"
        case "part paid": return "PARTIAL"
        default: return "UNPAID"
        }
    }

    /// Event type posted to the API (APK remap for utilities).
    private var postEventType: String {
        switch mode.uppercased() {
        case "HOLD": return "ORDER"
        case "TRANSFER": return "STOCK"
        case "REPRINT": return "SALE"
        case "TARGET": return "TARGET"
        case "ISSUE": return "ISSUE"
        case "DELIVERY": return "SHIPMENT"
        case "CUSTOMER_ORDER": return "ORDER"
        default: return mode.uppercased()
        }
    }

    var body: some View {
        VStack(spacing: 0) {
            ScrollView {
                VStack(alignment: .leading, spacing: 12) {
                    Text(contextLine)
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundStyle(accent)
                    modeBody
                    whyCard
                    ForEach(errors, id: \.self) { err in
                        Text(err).font(.system(size: 12)).foregroundStyle(Color.red.opacity(0.9))
                    }
                    if let confirm {
                        Text(confirm).font(.system(size: 13)).foregroundStyle(accent)
                    }
                }
                .padding(16)
            }
            footer
                .padding(.horizontal, 16)
                .padding(.vertical, 12)
                .background(Color(red: 0.04, green: 0.05, blue: 0.09))
        }
        .onAppear {
            if mode.isEmpty {
                mode = initialCode.uppercased()
                seedEntryMode()
            }
            if !seeded, let row = seedActivity {
                seeded = true
                applyActivitySeed(row)
            }
            applyBusinessTaxDefault()
            if draft.occurredAt.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                draft.occurredAt = Self.formatISODate(Date())
            }
            if draft.goodsDueAt.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                draft.goodsDueAt = Self.formatISODate(Date())
            }
            if draft.method.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                draft.method = "Cash"
            }
            if let firstChannel = projection?.life?.channels.first,
               draft.channel.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                draft.channel = firstChannel
            }
        }
        .sheet(item: Binding(
            get: { lovKind.map { CaptureSheetCode(code: $0) } },
            set: { lovKind = $0?.code }
        )) { item in
            SearchableOperatingLovSheet(
                kind: item.code,
                title: operatingLovTitle(for: item.code),
                options: lovOptions(for: item.code),
                canAdd: !["OBLIGATION", "SALES_ORDER", "PURCHASE_ORDER", "LINKED_SOURCE", "CHANNEL"].contains(item.code.uppercased()),
                accent: accent,
                onSelect: { applyLov(kind: item.code, option: $0) },
                onAdd: { name, unit, conversion in
                    Task { await createMaster(kind: item.code, name: name, unit: unit, conversion: conversion) }
                },
                onDismiss: { lovKind = nil }
            )
        }
    }

    // MARK: - Mode body

    @ViewBuilder
    private var modeBody: some View {
        switch mode.uppercased() {
        case "SALE": saleBody
        case "PURCHASE": purchaseBody
        case "COLLECTION": collectionBody
        case "PAYMENT", "REIMBURSE": paymentBody
        case "SPEND": spendBody
        case "STOCK": stockBody
        case "ORDER", "CUSTOMER_ORDER": orderBody
        case "SHIPMENT", "DELIVERY": shipmentBody
        case "RETURN": returnBody
        case "BUDGET", "TARGET": budgetBody
        case "UPDATE", "ISSUE": updateBody
        case "APPROVAL": approvalBody
        case "HOLD": holdBody
        case "TRANSFER": transferBody
        case "REPRINT": reprintBody
        default:
            field("Title *", text: $draft.title)
            TradingAmountHero(label: "Amount", currency: currency, accent: accent, text: $draft.amount)
        }
    }

    // MARK: Wave 1 — Money

    private var gstTrackingOn: Bool {
        (projection?.gstTracking ?? "").caseInsensitiveCompare("YES") == .orderedSame
    }

    @ViewBuilder
    private var saleBody: some View {
        chipRow(["Quick amount", "Itemized", "Return", "Reprint"], selected: draft.entryMode) { picked in
            if picked == "Return" { mode = "RETURN"; draft.entryMode = "Quick amount"; return }
            if picked == "Reprint" { mode = "REPRINT"; draft.entryMode = "Quick amount"; return }
            draft.entryMode = picked
        }
        if draft.entryMode == "Itemized" {
            itemizedEditor(addLabel: "+ Add item to sale")
            amountHero(label: "Sale total", readOnly: true, value: linesTotalRaw ?? draft.amount)
        } else {
            amountHero(label: "Sale amount (M)", readOnly: false, value: draft.amount)
        }
        if gstTrackingOn {
            gstSupplySection
        }
        field("Title", text: $draft.title)
        if draft.paymentChoice != "Received" {
            lovField("Customer", value: draft.counterparty, kind: "BUYER")
        }
        nativeDate("Sale date", iso: $draft.occurredAt)
        paymentChips(saleStyle: true)
        if draft.paymentChoice == "Part paid" {
            field("Received now", text: $draft.receivedAmount)
            lovField("Customer (part paid)", value: draft.counterparty, kind: "BUYER")
        }
        if draft.paymentChoice == "Pay later" || draft.paymentChoice == "Part paid" {
            nativeDate("Due date", iso: $draft.dueAt)
        }
        if let channels = projection?.life?.channels, !channels.isEmpty {
            Text("Channel").font(.system(size: 11)).foregroundStyle(Color.white.opacity(0.55))
            chipRow(channels, selected: draft.channel.isEmpty ? channels[0] : draft.channel) {
                draft.channel = $0
            }
        } else {
            lovField("Channel", value: draft.channel, kind: "CHANNEL")
        }
        if draft.entryMode == "Itemized" {
            Text("Delivery status").font(.system(size: 11)).foregroundStyle(Color.white.opacity(0.55))
            chipRow(
                ["Fully Delivered", "Partially Delivered", "Deliver Later"],
                selected: draft.goodsStatusLabel
            ) { draft.goodsStatusLabel = $0 }
            if draft.goodsStatusLabel.localizedCaseInsensitiveContains("Partial") {
                field("Quantity delivered now", text: $draft.quantityReceived)
            }
            if !draft.goodsStatusLabel.localizedCaseInsensitiveContains("Fully") {
                nativeDate("Remaining delivery date", iso: $draft.goodsDueAt)
            }
            Text("Delivery status does not post stock. Inventory follows fulfilment / Delivery Quick Add.")
                .font(.system(size: 12))
                .foregroundStyle(Color.white.opacity(0.45))
        }
        moreDetailsSection(includeTax: !gstTrackingOn)
    }

    @ViewBuilder
    private var purchaseBody: some View {
        chipRow(["Quick cost", "Itemized"], selected: draft.entryMode) { draft.entryMode = $0 }
        if draft.entryMode == "Itemized" {
            itemizedEditor(addLabel: "+ Add item to purchase")
            amountHero(label: "Purchase total", readOnly: true, value: linesTotalRaw ?? draft.amount)
        } else {
            amountHero(label: "Purchase amount (M)", readOnly: false, value: draft.amount)
        }
        if gstTrackingOn {
            gstSupplySection
        }
        field("Title", text: $draft.title)
        lovField("Supplier", value: draft.counterparty, kind: "SUPPLIER")
        nativeDate("Purchase date", iso: $draft.occurredAt)
        paymentChips(saleStyle: false)
        if draft.paymentChoice == "Part paid" {
            field("Paid now", text: $draft.receivedAmount)
        }
        chipRow(["Received now", "Receive later"], selected: draft.receiptState) { draft.receiptState = $0 }
        if gstTrackingOn {
            moreDetailsSection(includeTax: false, includeOwnership: true)
        } else {
            moreDetailsSection(includeTax: true, includeOwnership: true)
        }
    }

    @ViewBuilder
    private var collectionBody: some View {
        amountHero(label: "Collection amount (M)", readOnly: false, value: draft.amount)
        field("Title", text: $draft.title)
        lovField("Customer", value: draft.counterparty, kind: "BUYER")
        obligationPicker(label: "Eligible sale source *", kinds: ["SALE", "INVOICE", "ORDER"], prefillShipment: false)
        nativeDate("Collection date", iso: $draft.occurredAt)
        methodChips
        moreDetailsSection(includeTax: false)
    }

    @ViewBuilder
    private var paymentBody: some View {
        amountHero(label: "Payment amount (M)", readOnly: false, value: draft.amount)
        field("Title", text: $draft.title)
        lovField("Supplier / payee", value: draft.counterparty, kind: "SUPPLIER")
        obligationPicker(label: "Eligible payable *", kinds: ["PURCHASE", "SPEND", "INVOICE"], prefillShipment: false)
        nativeDate("Payment date", iso: $draft.occurredAt)
        methodChips
        moreDetailsSection(includeTax: false)
    }

    @ViewBuilder
    private var spendBody: some View {
        chipRow(["Quick", "Detailed"], selected: draft.spendDetail ? "Detailed" : "Quick") {
            draft.spendDetail = ($0 == "Detailed")
        }
        amountHero(label: "Spend amount (M)", readOnly: false, value: draft.amount)
        field("Title", text: $draft.title)
        if draft.spendDetail {
            Text("Expense type").font(.system(size: 11)).foregroundStyle(Color.white.opacity(0.55))
            chipRow(TradingExpenseGroups.all.map(\.0), selected: draft.expenseGroup) { group in
                draft.expenseGroup = group
                draft.expenseCategory = TradingExpenseGroups.categories(for: group).first ?? "Other"
            }
            chipRow(TradingExpenseGroups.categories(for: draft.expenseGroup), selected: draft.expenseCategory) {
                draft.expenseCategory = $0
            }
        } else {
            chipRow(["Rent", "Salaries", "Transport", "Other"], selected: draft.expenseCategory) {
                draft.expenseCategory = $0
            }
        }
        nativeDate("Spend date", iso: $draft.occurredAt)
        paymentChips(saleStyle: false)
        if draft.paymentChoice != "Paid" {
            lovField("Payee", value: draft.counterparty, kind: "SUPPLIER")
            nativeDate("Due date", iso: $draft.dueAt)
        }
        if draft.paymentChoice == "Part paid" {
            field("Paid now", text: $draft.receivedAmount)
        }
        moreDetailsSection(includeTax: false)
    }

    // MARK: Wave 2 — Ops

    @ViewBuilder
    private var stockBody: some View {
        chipRow(["Set current", "Add", "Reduce", "Transfer"], selected: stockChipLabel) { picked in
            if picked == "Transfer" { mode = "TRANSFER"; return }
            draft.stockAction = stockAction(from: picked)
        }
        TradingAmountHero(
            label: "Quantity (M)",
            currency: "Qty · \(draft.unit.ifBlankDefault("pc"))",
            accent: accent,
            text: $draft.quantity,
            hint: "Shows prior → delta → result",
            isQuantity: true,
            unitOverride: "Qty · \(draft.unit.ifBlankDefault("pc"))"
        )
        field("Title", text: $draft.title)
        lovField("Item / SKU", value: draft.itemName, kind: "PRODUCT")
        lovField("UOM", value: draft.unit, kind: "UOM")
        field("Location", text: $draft.location)
        chipRow(
            ["Opening count", "Stocktake", "Damage", "Waste", "Return", "Correction"],
            selected: draft.stockReason
        ) { draft.stockReason = $0 }
        nativeDate("Count date", iso: $draft.occurredAt)
        moreDetailsSection(includeTax: false, includeOwnership: true)
    }

    @ViewBuilder
    private var orderBody: some View {
        chipRow(["Customer order", "Supplier order", "Delivery"], selected: draft.orderSide) { picked in
            if picked == "Delivery" { mode = "SHIPMENT"; return }
            draft.orderSide = picked
        }
        amountHero(label: "Commitment amount", readOnly: false, value: draft.amount)
        field("Title *", text: $draft.title)
        lovField(
            draft.orderSide.hasPrefix("Supplier") ? "Supplier" : "Customer",
            value: draft.counterparty,
            kind: draft.orderSide.hasPrefix("Supplier") ? "SUPPLIER" : "BUYER"
        )
        nativeDate("Order date", iso: $draft.occurredAt)
        itemizedEditor(addLabel: "+ Add line")
        nativeDate("Expected delivery", iso: $draft.dueAt)
        chipRow(["Open confirmed", "Draft", "Cancelled"], selected: draft.activityStatus) {
            draft.activityStatus = $0
        }
        moreDetailsSection(includeTax: true)
    }

    @ViewBuilder
    private var shipmentBody: some View {
        chipRow(["Outgoing", "Incoming", "Hold DO"], selected: draft.shipmentDirection) { picked in
            if picked == "Hold DO" {
                mode = "HOLD"
                draft.holdTarget = "Shipment"
                return
            }
            draft.shipmentDirection = picked
        }
        TradingAmountHero(
            label: "Qty delivered (M)",
            currency: "Qty · \(draft.unit.ifBlankDefault("pc"))",
            accent: accent,
            text: $draft.quantity,
            hint: draft.remainingHint.isEmpty ? "≤ remaining" : "Remaining \(draft.remainingHint)",
            isQuantity: true,
            unitOverride: "Qty · \(draft.unit.ifBlankDefault("pc"))"
        )
        field("Title", text: $draft.title)
        obligationPicker(
            label: draft.shipmentDirection == "Incoming" ? "Purchase source *" : "Sale source *",
            kinds: draft.shipmentDirection == "Incoming" ? ["PURCHASE", "ORDER"] : ["SALE", "ORDER"],
            prefillShipment: true
        )
        lovField("Party", value: draft.counterparty, kind: draft.shipmentDirection == "Incoming" ? "SUPPLIER" : "BUYER")
        nativeDate("Delivery date", iso: $draft.occurredAt)
        field("Carrier / ref", text: $draft.carrier)
        lovField("UOM", value: draft.unit, kind: "UOM")
        moreDetailsSection(includeTax: false)
    }

    @ViewBuilder
    private var returnBody: some View {
        chipRow(["Customer return", "Supplier return"], selected: draft.returnDirection) {
            draft.returnDirection = $0
        }
        chipRow(["Quick amount", "Itemized"], selected: draft.entryMode) { draft.entryMode = $0 }
        if draft.entryMode == "Itemized" {
            itemizedEditor(addLabel: "+ Add returned item")
            amountHero(label: "Credit total", readOnly: true, value: linesTotalRaw ?? draft.amount)
        } else {
            amountHero(label: "Credit amount (M)", readOnly: false, value: draft.amount)
        }
        field("Title *", text: $draft.title)
        obligationPicker(
            label: "Source *",
            kinds: draft.returnDirection.hasPrefix("Supplier") ? ["PURCHASE"] : ["SALE", "INVOICE"],
            prefillShipment: false
        )
        lovField(
            "Party",
            value: draft.counterparty,
            kind: draft.returnDirection.hasPrefix("Supplier") ? "SUPPLIER" : "BUYER"
        )
        chipRow(["Returned to stock", "Pending inspection", "Not returned"], selected: draft.goodsMovement) {
            draft.goodsMovement = $0
        }
        field("Return reason *", text: $draft.returnReason)
        nativeDate("Return date", iso: $draft.occurredAt)
        moreDetailsSection(includeTax: false)
    }

    @ViewBuilder
    private var budgetBody: some View {
        chipRow(["Budget", "Sales target"], selected: draft.revisionKind == "TARGET" ? "Sales target" : "Budget") {
            draft.revisionKind = $0 == "Sales target" ? "TARGET" : "BUDGET"
            mode = draft.revisionKind == "TARGET" ? "TARGET" : "BUDGET"
        }
        amountHero(label: "New amount (M)", readOnly: false, value: draft.amount)
        field("Title *", text: $draft.title)
        nativeMonth("Period", iso: $draft.periodStart)
        nativeDate("Effective from", iso: $draft.occurredAt)
        field("Metric code", text: $draft.metricCode)
        field("Note / reason", text: $draft.note)
    }

    // MARK: Wave 3 — Utilities

    @ViewBuilder
    private var updateBody: some View {
        chipRow(["Update", "Issue", "Approval"], selected: draft.updateKind) { picked in
            if picked == "Approval" { mode = "APPROVAL"; return }
            draft.updateKind = picked
            mode = picked == "Issue" ? "ISSUE" : "UPDATE"
        }
        field("Title *", text: $draft.title)
        field("Description", text: $draft.note)
        obligationPicker(
            label: "Linked source (optional)",
            kinds: ["SALE", "PURCHASE", "ORDER", "STOCK", "ACTIVITY"],
            prefillShipment: false
        )
        if draft.updateKind == "Issue" {
            field("Owner", text: $draft.counterparty)
            nativeDate("Due date", iso: $draft.dueAt)
        } else {
            nativeDate("Date", iso: $draft.occurredAt)
        }
    }

    @ViewBuilder
    private var approvalBody: some View {
        chipRow(["Approve", "Reject", "Reimburse"], selected: draft.approvalChoice) { picked in
            if picked == "Reimburse" { mode = "PAYMENT"; return }
            draft.approvalChoice = picked
        }
        Text("Pending attention")
            .font(.system(size: 11))
            .foregroundStyle(Color.white.opacity(0.55))
        let rows = projection?.attention ?? []
        if rows.isEmpty {
            Text("Nothing waiting for a decision.")
                .font(.system(size: 12))
                .foregroundStyle(Color.white.opacity(0.45))
        } else {
            ForEach(rows.prefix(8)) { row in
                chip("\(row.title) · \(row.amount ?? "")", on: draft.parentEventId == row.id) {
                    draft.parentEventId = row.id
                    draft.title = row.title
                    if let amt = row.amount { draft.amount = amt }
                }
            }
        }
        if draft.approvalChoice == "Reject" {
            field("Reject reason *", text: $draft.note)
        } else {
            field("Note", text: $draft.note)
        }
        nativeDate("Decide-by", iso: $draft.dueAt)
    }

    @ViewBuilder
    private var holdBody: some View {
        chipRow(["Order", "Dispatch"], selected: draft.holdTarget) { picked in
            draft.holdTarget = picked
            draft.parentEventId = ""
        }
        chipRow(["No hold", "Hold order", "Hold dispatch"], selected: draft.holdAction) {
            draft.holdAction = $0
        }
        field("Title", text: $draft.title)
        obligationPicker(
            label: draft.holdTarget == "Dispatch" ? "Open shipment *" : "Open order *",
            kinds: draft.holdTarget == "Dispatch" ? ["SHIPMENT", "DELIVERY"] : ["ORDER", "CUSTOMER_ORDER"],
            prefillShipment: false
        )
        field("Hold reason *", text: $draft.note)
        Text("A hold freezes the record where it is. Release it before completing or delivering.")
            .font(.system(size: 12))
            .foregroundStyle(Color.white.opacity(0.45))
    }

    @ViewBuilder
    private var transferBody: some View {
        field("Title", text: $draft.title)
        lovField("Item", value: draft.itemName, kind: "PRODUCT")
        field("From location *", text: $draft.location)
        field("To location *", text: $draft.locationTo)
        TradingAmountHero(
            label: "Quantity (M)",
            currency: "Qty · \(draft.unit.ifBlankDefault("pc"))",
            accent: accent,
            text: $draft.quantity,
            isQuantity: true,
            unitOverride: "Qty · \(draft.unit.ifBlankDefault("pc"))"
        )
        lovField("UOM", value: draft.unit, kind: "UOM")
        chipRow(
            ["Stocktake", "Replenishment", "Damage", "Correction"],
            selected: ["Stocktake", "Replenishment", "Damage", "Correction"].contains(draft.stockReason)
                ? draft.stockReason
                : "Stocktake"
        ) { draft.stockReason = $0 }
        chipRow(["Owned", "Consignment"], selected: draft.ownership) { draft.ownership = $0 }
        field("Note", text: $draft.note)
        Text("A transfer moves stock between two places. Total on-hand does not change.")
            .font(.system(size: 12))
            .foregroundStyle(Color.white.opacity(0.45))
    }

    @ViewBuilder
    private var reprintBody: some View {
        chipRow(["Invoice", "Challan"], selected: draft.reprintKind) { draft.reprintKind = $0 }
        Text("Source document")
            .font(.system(size: 11))
            .foregroundStyle(Color.white.opacity(0.55))
        let sales = (projection?.activity ?? []).filter { $0.eventType == "SALE" || $0.eventType == "INVOICE" }
        if sales.isEmpty {
            Text("No invoices to reprint yet.")
                .font(.system(size: 12))
                .foregroundStyle(Color.white.opacity(0.45))
        } else {
            ForEach(sales.prefix(10)) { row in
                chip("\(row.title) · \(row.amount ?? "")", on: draft.parentEventId == row.id) {
                    draft.parentEventId = row.id
                    draft.title = row.title
                }
            }
        }
        Text("Reprint is read-only — nothing new is posted.")
            .font(.system(size: 12))
            .foregroundStyle(Color.white.opacity(0.55))
    }

    // MARK: Shared pieces

    private var footer: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(stickyFooter)
                .font(.system(size: 12, weight: .semibold))
                .foregroundStyle(accent)
                .padding(.horizontal, 12)
                .padding(.vertical, 10)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(accent.opacity(0.10))
                .clipShape(RoundedRectangle(cornerRadius: 12))
            Button(ctaLabel) {
                if mode.uppercased() == "REPRINT" {
                    onSaved()
                    return
                }
                Task { await submit() }
            }
            .disabled(submitting)
            .font(.system(size: 15, weight: .semibold))
            .foregroundStyle(Color.black)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 12)
            .background(accent)
            .clipShape(RoundedRectangle(cornerRadius: 12))
        }
    }

    private var whyCard: some View {
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

    private func amountHero(label: String, readOnly: Bool, value: String) -> some View {
        TradingAmountHero(
            label: label,
            currency: currency,
            accent: accent,
            text: Binding(
                get: { readOnly ? (value.isEmpty ? "0.00" : value) : draft.amount },
                set: { if !readOnly { draft.amount = $0 } }
            ),
            hint: readOnly ? "Sum of lines · read-only" : ">0",
            readOnly: readOnly
        )
        .onChange(of: linesTotalRaw) { _, next in
            if readOnly, let next { draft.amount = next }
        }
    }

    private func paymentChips(saleStyle: Bool) -> some View {
        let options = saleStyle
            ? ["Received", "Part paid", "Pay later"]
            : ["Paid", "Part paid", "Pay later"]
        return chipRow(options, selected: draft.paymentChoice) { draft.paymentChoice = $0 }
    }

    @ViewBuilder
    private func itemizedEditor(addLabel: String) -> some View {
        lovField("Item", value: draft.itemName, kind: "PRODUCT")
        HStack(spacing: 8) {
            field("Qty", text: $draft.quantity)
            lovField("Unit", value: draft.unit, kind: "UOM")
        }
        field("Unit price", text: $draft.unitPrice)
        if !draft.itemName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            Button(addLabel) { addPendingLine() }
                .font(.system(size: 13, weight: .semibold))
                .foregroundStyle(accent)
        }
        if draft.savedLines.isEmpty {
            Text("Add products to build the cart.")
                .font(.system(size: 12))
                .foregroundStyle(Color.white.opacity(0.45))
        } else {
            ForEach(draft.savedLines) { line in
                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(line.itemName).font(.system(size: 13, weight: .semibold)).foregroundStyle(.white)
                        Text("\(line.quantity) \(line.unit) × \(line.unitPrice)")
                            .font(.system(size: 11))
                            .foregroundStyle(Color.white.opacity(0.55))
                    }
                    Spacer()
                    Button {
                        draft.savedLines.removeAll { $0.id == line.id }
                        syncAmountFromLines()
                    } label: {
                        Image(systemName: "xmark.circle.fill")
                            .foregroundStyle(Color.white.opacity(0.45))
                    }
                }
                .padding(10)
                .background(Color(red: 0.09, green: 0.11, blue: 0.15))
                .clipShape(RoundedRectangle(cornerRadius: 10))
            }
        }
    }

    @ViewBuilder
    private func obligationPicker(label: String, kinds: [String], prefillShipment: Bool) -> some View {
        Text(label).font(.system(size: 11)).foregroundStyle(Color.white.opacity(0.55))
        let fromPayments = projection?.paymentReadiness ?? []
        let fromActivity = (projection?.activity ?? []).filter { kinds.contains($0.eventType) }
        Button {
            obligationKinds = kinds
            obligationPrefillShipment = prefillShipment
            lovKind = "OBLIGATION"
        } label: {
            HStack {
                Text(
                    draft.parentEventId.isEmpty
                        ? "Search open records…"
                        : (fromActivity.first(where: { $0.id == draft.parentEventId })?.title
                            ?? fromPayments.first(where: { $0.id == draft.parentEventId })?.title
                            ?? "Selected record")
                )
                .foregroundStyle(draft.parentEventId.isEmpty ? Color.white.opacity(0.45) : Color.white)
                Spacer()
                Image(systemName: "magnifyingglass")
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundStyle(Color.white.opacity(0.45))
            }
            .padding(12)
            .background(Color(red: 0.09, green: 0.11, blue: 0.15))
            .clipShape(RoundedRectangle(cornerRadius: 12))
        }
        .buttonStyle(.plain)
        if fromPayments.isEmpty && fromActivity.isEmpty {
            Text("Nothing is open yet.")
                .font(.system(size: 12))
                .foregroundStyle(Color.white.opacity(0.45))
        } else {
            ForEach(fromPayments.prefix(6)) { p in
                chip("\(p.title) · \(p.amount) · \(p.status)", on: draft.parentEventId == p.id) {
                    applyObligationPayment(p)
                }
            }
            ForEach(fromActivity.prefix(6)) { a in
                chip("\(a.eventType) · \(a.title)", on: draft.parentEventId == a.id) {
                    applyObligationActivity(a, prefillShipment: prefillShipment)
                }
            }
        }
        if prefillShipment, !draft.remainingHint.isEmpty {
            Text("Prefill remaining · \(draft.remainingHint)")
                .font(.system(size: 12))
                .foregroundStyle(Color.white.opacity(0.55))
        }
    }

    private func chipRow(_ options: [String], selected: String, onSelect: @escaping (String) -> Void) -> some View {
        ScrollView(.horizontal, showsIndicators: false) {
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
                .font(.system(size: 12, weight: .semibold))
                .foregroundStyle(on ? Color.black : Color.white.opacity(0.9))
                .padding(.horizontal, 10)
                .padding(.vertical, 8)
                .background(on ? accent : Color(red: 0.09, green: 0.11, blue: 0.15))
                .clipShape(RoundedRectangle(cornerRadius: 10))
        }
        .buttonStyle(.plain)
    }

    private func field(_ label: String, text: Binding<String>) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label).font(.system(size: 11)).foregroundStyle(Color.white.opacity(0.55))
            TextField(label, text: text)
                .textFieldStyle(.plain)
                .padding(12)
                .background(Color(red: 0.09, green: 0.11, blue: 0.15))
                .clipShape(RoundedRectangle(cornerRadius: 12))
                .foregroundStyle(.white)
        }
    }

    private func nativeDate(_ label: String, iso: Binding<String>) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label).font(.system(size: 11)).foregroundStyle(Color.white.opacity(0.55))
            DatePicker(
                label,
                selection: Binding(
                    get: { Self.parseISODate(iso.wrappedValue) ?? Date() },
                    set: { iso.wrappedValue = Self.formatISODate($0) }
                ),
                displayedComponents: .date
            )
            .labelsHidden()
            .datePickerStyle(.compact)
            .tint(accent)
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Color(red: 0.09, green: 0.11, blue: 0.15))
            .clipShape(RoundedRectangle(cornerRadius: 12))
            .colorScheme(.dark)
            .onAppear {
                if iso.wrappedValue.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                    iso.wrappedValue = Self.formatISODate(Date())
                }
            }
        }
    }

    private func nativeMonth(_ label: String, iso: Binding<String>) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label).font(.system(size: 11)).foregroundStyle(Color.white.opacity(0.55))
            DatePicker(
                label,
                selection: Binding(
                    get: { Self.parseISOMonth(iso.wrappedValue) ?? Date() },
                    set: { iso.wrappedValue = Self.formatISOMonth($0) }
                ),
                displayedComponents: [.date]
            )
            .labelsHidden()
            .datePickerStyle(.compact)
            .tint(accent)
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Color(red: 0.09, green: 0.11, blue: 0.15))
            .clipShape(RoundedRectangle(cornerRadius: 12))
            .colorScheme(.dark)
            .onAppear {
                if iso.wrappedValue.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                    iso.wrappedValue = Self.formatISOMonth(Date())
                }
            }
        }
    }

    private var methodChips: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text("Method").font(.system(size: 11)).foregroundStyle(Color.white.opacity(0.55))
            chipRow(["Cash", "UPI", "Bank", "Other"], selected: draft.method.isEmpty ? "Cash" : draft.method) {
                draft.method = $0
            }
        }
    }

    /// Tax mode + GST rate + Intra/Inter — always visible when GST tracking is on (APK GstPriceFields).
    @ViewBuilder
    private var gstSupplySection: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("Tax on price")
                .font(.system(size: 11))
                .foregroundStyle(Color.white.opacity(0.55))
            chipRow(["Not configured", "Exclusive", "Inclusive"], selected: draft.taxMode) {
                draft.taxMode = $0
                draft.highlightSupply = false
                draft.highlightGstRate = false
                if $0 == "Not configured" { draft.supplyChoice = "" }
            }
            if draft.taxMode != "Not configured" {
                field("GST rate %", text: $draft.gstRate)
                    .overlay(
                        RoundedRectangle(cornerRadius: 12)
                            .stroke(draft.highlightGstRate && draft.gstRate.isEmpty ? Color.red.opacity(0.8) : Color.clear, lineWidth: 1)
                    )
                Text("Supply")
                    .font(.system(size: 11))
                    .foregroundStyle(draft.highlightSupply ? Color.red.opacity(0.9) : Color.white.opacity(0.55))
                chipRow(
                    ["Inside your state", "Outside your state"],
                    selected: draft.supplyChoice == "INTRA"
                        ? "Inside your state"
                        : draft.supplyChoice == "INTER" ? "Outside your state" : ""
                ) {
                    draft.supplyChoice = $0.hasPrefix("Inside") ? "INTRA" : "INTER"
                    draft.highlightSupply = false
                }
                .overlay(
                    RoundedRectangle(cornerRadius: 12)
                        .stroke(draft.highlightSupply ? Color.red.opacity(0.8) : Color.clear, lineWidth: 1)
                )
                if draft.supplyChoice.isEmpty || draft.highlightSupply {
                    Text("Confirm whether this supply is inside your state or outside it.")
                        .font(.system(size: 12))
                        .foregroundStyle(draft.highlightSupply ? Color.red.opacity(0.9) : Color.white.opacity(0.45))
                }
            } else {
                Text("Tax is optional. Leave Not configured to save without GST.")
                    .font(.system(size: 12))
                    .foregroundStyle(Color.white.opacity(0.45))
            }
        }
    }

    private func applyBusinessTaxDefault() {
        guard gstTrackingOn else { return }
        guard draft.taxMode == "Not configured" || draft.taxMode.isEmpty else { return }
        let includes: String? = {
            switch mode.uppercased() {
            case "PURCHASE": return projection?.purchasePriceIncludesGst
            case "SALE", "SERVICE": return projection?.salePriceIncludesGst
            default: return projection?.salePriceIncludesGst
            }
        }()
        switch (includes ?? "").uppercased() {
        case "YES": draft.taxMode = "Inclusive"
        case "NO": draft.taxMode = "Exclusive"
        default: break
        }
    }

    @ViewBuilder
    private func moreDetailsSection(includeTax: Bool, includeOwnership: Bool = false) -> some View {
        Button {
            draft.showMoreDetails.toggle()
        } label: {
            HStack {
                Text(draft.showMoreDetails ? "More details  ∧" : "More details  ∨")
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundStyle(accent)
                Spacer()
            }
        }
        .buttonStyle(.plain)
        if draft.showMoreDetails {
            VStack(alignment: .leading, spacing: 10) {
                if includeOwnership {
                    chipRow(["Owned", "Consignment"], selected: draft.ownership) { draft.ownership = $0 }
                }
                if includeTax {
                    gstSupplySection
                    field("FX / original currency note", text: $draft.fxNote)
                } else if gstTrackingOn {
                    field("FX / original currency note", text: $draft.fxNote)
                }
                field("Note", text: $draft.note)
            }
            .padding(12)
            .background(Color.white.opacity(0.04))
            .clipShape(RoundedRectangle(cornerRadius: 12))
        }
    }

    private func lovField(_ label: String, value: String, kind: String) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label).font(.system(size: 11)).foregroundStyle(Color.white.opacity(0.55))
            Button { lovKind = kind } label: {
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
            .buttonStyle(.plain)
        }
    }

    // MARK: - Helpers

    private var stockChipLabel: String {
        switch draft.stockAction.uppercased() {
        case "ADD": return "Add"
        case "REMOVE": return "Reduce"
        case "TRANSFER": return "Transfer"
        default: return "Set current"
        }
    }

    private func stockAction(from chip: String) -> String {
        switch chip {
        case "Add": return "ADD"
        case "Reduce": return "REMOVE"
        case "Transfer": return "TRANSFER"
        default: return "SET"
        }
    }

    private func seedEntryMode() {
        switch mode.uppercased() {
        case "PURCHASE": draft.entryMode = "Quick cost"
        case "SALE": draft.entryMode = "Quick amount"
        case "SPEND": draft.spendDetail = false
        case "BUDGET": draft.revisionKind = "BUDGET"
        case "TARGET": draft.revisionKind = "TARGET"; draft.entryMode = "Sales target"
        default: break
        }
    }

    private func applyActivitySeed(_ row: OperatingActivityPayload) {
        draft.title = row.title
        draft.amount = row.amount ?? ""
        draft.note = row.note ?? ""
        draft.counterparty = row.counterpartyName ?? ""
        draft.channel = row.channelName ?? draft.channel
        draft.quantity = row.quantity ?? ""
        draft.unit = row.unit ?? draft.unit
        draft.parentEventId = row.parentEventId ?? ""
        if let occurred = row.occurredAt.nilIfBlank {
            draft.occurredAt = String(occurred.prefix(10))
        }
        if let status = row.paymentStatus?.uppercased() {
            switch status {
            case "PAID", "COLLECTED": draft.paymentChoice = "Received"
            case "PENDING", "READY": draft.paymentChoice = "Pay later"
            default: break
            }
        }
    }

    private func addPendingLine() {
        let name = draft.itemName.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !name.isEmpty else { return }
        let qty = draft.quantity.trimmingCharacters(in: .whitespacesAndNewlines).ifBlankDefault("1")
        draft.savedLines.append(
            TradingCaptureLine(
                itemName: name,
                quantity: qty,
                unit: draft.unit.ifBlankDefault("pc"),
                unitPrice: draft.unitPrice,
                masterId: draft.productMasterId
            )
        )
        draft.itemName = ""
        draft.quantity = ""
        draft.unitPrice = ""
        draft.productMasterId = nil
        syncAmountFromLines()
    }

    private func syncAmountFromLines() {
        if let total = linesTotalRaw { draft.amount = total }
    }

    private func lovOptions(for kind: String) -> [OperatingLovOption] {
        if kind.uppercased() == "OBLIGATION" {
            return obligationLovOptions(kinds: obligationKinds)
        }
        if kind.uppercased() == "CHANNEL" {
            let channels = projection?.life?.channels ?? []
            return channels.map {
                OperatingLovOption(id: "channel-\($0)", name: $0, kind: "CHANNEL", subtitle: nil, unit: nil)
            }
        }
        return tradingLovOptions(kind: kind, masters: projection?.masters ?? [], local: [])
    }

    private func obligationLovOptions(kinds: [String]) -> [OperatingLovOption] {
        var rows: [OperatingLovOption] = []
        var seen = Set<String>()
        for p in projection?.paymentReadiness ?? [] {
            guard seen.insert(p.id).inserted else { continue }
            var subtitleBits = [p.amount, p.status]
            if let party = p.counterpartyName?.nilIfBlank { subtitleBits.append(party) }
            rows.append(
                OperatingLovOption(
                    id: p.id,
                    name: p.title,
                    kind: "OBLIGATION",
                    subtitle: subtitleBits.joined(separator: " · "),
                    unit: nil
                )
            )
        }
        for a in projection?.activity ?? [] where kinds.contains(a.eventType) {
            guard seen.insert(a.id).inserted else { continue }
            let qtyBit: String? = {
                guard let q = a.quantity?.nilIfBlank else { return nil }
                return "\(q) \(a.unit ?? "")".trimmingCharacters(in: .whitespaces)
            }()
            let subtitle = [a.eventType, a.counterpartyName, a.amount, qtyBit]
                .compactMap { $0?.nilIfBlank }
                .joined(separator: " · ")
            rows.append(
                OperatingLovOption(
                    id: a.id,
                    name: a.title,
                    kind: "OBLIGATION",
                    subtitle: subtitle.nilIfBlank,
                    unit: a.unit
                )
            )
        }
        return rows
    }

    private func applyObligationPayment(_ p: OperatingPaymentPayload) {
        draft.parentEventId = p.id
        if draft.counterparty.isEmpty, let c = p.counterpartyName { draft.counterparty = c }
        if draft.amount.isEmpty { draft.amount = p.amount }
        if draft.title.isEmpty { draft.title = p.title }
        draft.remainingHint = ""
    }

    private func applyObligationActivity(_ a: OperatingActivityPayload, prefillShipment: Bool) {
        draft.parentEventId = a.id
        if draft.counterparty.isEmpty, let c = a.counterpartyName { draft.counterparty = c }
        if draft.title.isEmpty { draft.title = a.title }
        if let q = a.quantity {
            if draft.quantity.isEmpty || prefillShipment { draft.quantity = q }
            let unit = a.unit ?? draft.unit.ifBlankDefault("pc")
            draft.remainingHint = "\(q) \(unit) · \(a.title)"
        } else {
            draft.remainingHint = a.title
        }
        if let u = a.unit { draft.unit = u }
        if draft.amount.isEmpty, let amt = a.amount { draft.amount = amt }
    }

    private func applyLov(kind: String, option: OperatingLovOption) {
        switch kind.uppercased() {
        case "PRODUCT":
            draft.itemName = option.name
            let id = option.id
            draft.productMasterId = (id.hasPrefix("local-") || id.hasPrefix("seed-") || id.hasPrefix("channel-"))
                ? nil
                : id.nilIfBlank
            if let u = option.unit?.nilIfBlank { draft.unit = u }
            if draft.quantity.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                draft.quantity = "1"
            }
            if let master = projection?.masters?.first(where: { $0.id == option.id }) {
                applyProductMasterPrefill(master)
                draft.productMasterId = master.id.nilIfBlank
            }
            if draft.title.isEmpty { draft.title = option.name }
            syncAmountFromLines()
        case "BUYER", "CUSTOMER", "SUPPLIER":
            draft.counterparty = option.name
        case "CHANNEL":
            draft.channel = option.name
        case "UOM":
            draft.unit = option.unit ?? option.name
        case "OBLIGATION":
            if let pay = projection?.paymentReadiness?.first(where: { $0.id == option.id }) {
                applyObligationPayment(pay)
            } else if let act = projection?.activity?.first(where: { $0.id == option.id }) {
                applyObligationActivity(act, prefillShipment: obligationPrefillShipment)
            } else {
                draft.parentEventId = option.id
                if draft.title.isEmpty { draft.title = option.name }
            }
        default:
            break
        }
        lovKind = nil
    }

    private func applyProductMasterPrefill(_ master: OperatingMasterPayload) {
        if let rate = master.gstRate?.nilIfBlank {
            draft.gstRate = rate
        }
        let purchase = mode.uppercased() == "PURCHASE"
        let includes = (purchase ? master.purchasePriceIncludesGst : master.salePriceIncludesGst)?
            .trimmingCharacters(in: .whitespacesAndNewlines)
            .uppercased()
        switch includes {
        case "YES": draft.taxMode = "Inclusive"
        case "NO": draft.taxMode = "Exclusive"
        default:
            if draft.taxMode != "Inclusive" && draft.taxMode != "Exclusive" {
                draft.taxMode = "Not configured"
            }
        }
        if draft.unitPrice.isEmpty {
            let source = purchase
                ? master.purchasePrice
                : (master.sellPrice ?? master.mrp)
            let raw = source?
                .replacingOccurrences(of: "₹", with: "")
                .trimmingCharacters(in: .whitespacesAndNewlines)
            if let raw, !raw.isEmpty {
                let plain = BusinessSetupAmountFormatting.plainAmount(raw)
                draft.unitPrice = currency.uppercased() == "INR"
                    ? BusinessSetupAmountFormatting.formatIndianAmount(plain)
                    : plain
            }
        }
        draft.highlightSupply = false
        draft.highlightGstRate = false
    }

    private static let isoDayFormatter: DateFormatter = {
        let f = DateFormatter()
        f.calendar = Calendar(identifier: .gregorian)
        f.locale = Locale(identifier: "en_US_POSIX")
        f.timeZone = TimeZone(secondsFromGMT: 0)
        f.dateFormat = "yyyy-MM-dd"
        return f
    }()

    private static let isoMonthFormatter: DateFormatter = {
        let f = DateFormatter()
        f.calendar = Calendar(identifier: .gregorian)
        f.locale = Locale(identifier: "en_US_POSIX")
        f.timeZone = TimeZone(secondsFromGMT: 0)
        f.dateFormat = "yyyy-MM-01"
        return f
    }()

    private static func parseISODate(_ raw: String) -> Date? {
        let trimmed = raw.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return nil }
        let day = String(trimmed.prefix(10))
        return isoDayFormatter.date(from: day)
    }

    private static func formatISODate(_ date: Date) -> String {
        isoDayFormatter.string(from: date)
    }

    private static func parseISOMonth(_ raw: String) -> Date? {
        parseISODate(raw)
    }

    private static func formatISOMonth(_ date: Date) -> String {
        isoMonthFormatter.string(from: date)
    }

    private func createMaster(kind: String, name: String, unit: String?, conversion: String?) async {
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
                counterpartyName: (masterKind == "BUYER" || masterKind == "SUPPLIER") ? trimmed : nil,
                unit: unit,
                itemName: masterKind == "PRODUCT" ? trimmed : nil,
                masterKind: masterKind,
                conversionToBase: conversion,
                idempotencyKey: UUID().uuidString
            )
            applyLov(
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

    private func submit() async {
        errors = []
        let title = draft.title.trimmingCharacters(in: .whitespacesAndNewlines)
        let resolvedTitle: String = {
            if !title.isEmpty { return title }
            if !draft.itemName.isEmpty { return draft.itemName }
            if !draft.counterparty.isEmpty { return "\(mode.capitalized) · \(draft.counterparty)" }
            return mode.capitalized
        }()

        if mode.uppercased() == "RETURN",
           draft.returnReason.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            errors = ["Return reason is required"]
            return
        }
        if ["COLLECTION", "PAYMENT", "HOLD"].contains(mode.uppercased()),
           draft.parentEventId.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            errors = ["Linked source is required"]
            return
        }
        if mode.uppercased() == "HOLD",
           draft.note.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            errors = ["Hold reason is required"]
            return
        }
        if mode.uppercased() == "TRANSFER" {
            let from = draft.location.trimmingCharacters(in: .whitespacesAndNewlines)
            let to = draft.locationTo.trimmingCharacters(in: .whitespacesAndNewlines)
            if from.isEmpty || to.isEmpty {
                errors = ["From and To locations are required"]
                return
            }
            if from.caseInsensitiveCompare(to) == .orderedSame {
                errors = ["From and To must be different locations"]
                return
            }
            if draft.quantity.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                errors = ["Transfer quantity is required"]
                return
            }
        }
        if mode.uppercased() == "APPROVAL",
           draft.parentEventId.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            errors = ["Pick a pending item to decide"]
            return
        }
        if mode.uppercased() == "APPROVAL",
           draft.approvalChoice == "Reject",
           draft.note.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            errors = ["Reject reason is required"]
            return
        }
        if ["SALE", "PURCHASE"].contains(mode.uppercased()),
           projection?.gstTracking == "YES",
           draft.taxMode == "Inclusive" || draft.taxMode == "Exclusive" {
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

        if mode.uppercased() == "SALE" {
            let itemized = draft.entryMode.localizedCaseInsensitiveContains("Item")
            let pendingName = draft.itemName.trimmingCharacters(in: .whitespacesAndNewlines)
            if itemized {
                let hasLines = !draft.savedLines.isEmpty || !pendingName.isEmpty
                if !hasLines {
                    errors = ["Add at least one item."]
                    return
                }
                func lineOk(name: String, quantity: String, unit: String, unitPrice: String) -> Bool {
                    let qty = Decimal(string: quantity.trimmingCharacters(in: .whitespacesAndNewlines)) ?? 0
                    let price = Decimal(string: BusinessSetupAmountFormatting.plainAmount(unitPrice)) ?? 0
                    return !name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
                        && qty > 0
                        && !unit.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
                        && price > 0
                }
                let savedBad = draft.savedLines.contains {
                    !lineOk(name: $0.itemName, quantity: $0.quantity, unit: $0.unit, unitPrice: $0.unitPrice)
                }
                let pendingBad = !pendingName.isEmpty && !lineOk(
                    name: pendingName,
                    quantity: draft.quantity.ifBlankDefault("1"),
                    unit: draft.unit.ifBlankDefault("pc"),
                    unitPrice: draft.unitPrice
                )
                if savedBad || pendingBad {
                    errors = ["Each item needs a name, quantity, unit, and price."]
                    return
                }
            } else {
                let plain = BusinessSetupAmountFormatting.plainAmount(draft.amount)
                if plain.isEmpty || (Decimal(string: plain) ?? 0) <= 0 {
                    errors = ["Amount is required."]
                    return
                }
            }
            let channels = (projection?.life?.channels ?? []).filter { !$0.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty }
            let hasChannelMasters = (projection?.masters ?? []).contains {
                $0.kind.caseInsensitiveCompare("CHANNEL") == .orderedSame
                    && !$0.name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
            }
            if (channels.isEmpty == false || hasChannelMasters),
               draft.channel.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                errors = ["Sales channel is required."]
                return
            }
            let pay = draft.paymentChoice.lowercased()
            let settled = pay == "received" || pay == "paid"
            if !settled {
                if draft.counterparty.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                    errors = ["A named buyer is required when a balance remains."]
                    return
                }
                if draft.dueAt.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                    errors = ["Due date is required when a balance remains."]
                    return
                }
            }
        }

        submitting = true
        defer { submitting = false }

        if mode.uppercased() == "APPROVAL" {
            let decision = draft.approvalChoice.lowercased().hasPrefix("reject") ? "REJECTED" : "APPROVED"
            do {
                _ = try await APIClient.shared.decideOperatingEvent(
                    momentId: momentId,
                    eventId: draft.parentEventId,
                    decision: decision
                )
                confirm = "Decision saved"
                onSaved()
            } catch {
                errors = [error.localizedDescription]
            }
            return
        }

        if draft.entryMode.localizedCaseInsensitiveContains("Item"), let total = linesTotalRaw {
            draft.amount = total
        }

        let taxOnWire = taxModeWire == "INCLUSIVE" || taxModeWire == "EXCLUSIVE"
        var lines: [(itemName: String, quantity: String?, unit: String?, unitPrice: String?, masterId: String?, taxMode: String?, gstRate: String?)] = draft.savedLines.map {
            (
                $0.itemName,
                $0.quantity,
                $0.unit,
                BusinessSetupAmountFormatting.plainAmount($0.unitPrice).nilIfBlank,
                $0.masterId,
                taxOnWire ? taxModeWire : "NOT_CONFIGURED",
                taxOnWire ? draft.gstRate.nilIfBlank : nil
            )
        }
        let pending = draft.itemName.trimmingCharacters(in: .whitespacesAndNewlines)
        if !pending.isEmpty {
            lines.append((
                pending,
                draft.quantity.nilIfBlank ?? "1",
                draft.unit.nilIfBlank,
                BusinessSetupAmountFormatting.plainAmount(draft.unitPrice).nilIfBlank,
                draft.productMasterId,
                taxOnWire ? taxModeWire : "NOT_CONFIGURED",
                taxOnWire ? draft.gstRate.nilIfBlank : nil
            ))
        }

        let stockAction: String? = {
            switch mode.uppercased() {
            case "STOCK": return draft.stockAction
            case "TRANSFER": return "TRANSFER"
            default: return nil
            }
        }()

        let contract: OperatingContractInput? = {
            var c = OperatingContractInput()
            c.entryMode = Self.entryModeWire(draft.entryMode)
            c.paymentState = Self.paymentStateWire(
                choice: draft.paymentChoice,
                mode: mode,
                receiptState: draft.receiptState
            )
            c.receiptState = mode.uppercased() == "PURCHASE" ? Self.receiptStateWire(draft.receiptState) : nil
            c.receivedAmount = draft.receivedAmount.nilIfBlank
            c.stockAction = stockAction
            c.stockReason = stockReasonWire
            c.expenseCategory = mode.uppercased() == "SPEND" ? draft.expenseCategory.nilIfBlank : nil
            c.taxMode = taxModeWire ?? "NOT_CONFIGURED"
            c.returnReason = draft.returnReason.nilIfBlank
            c.returnDirection = mode.uppercased() == "RETURN" ? draft.returnDirection.nilIfBlank : nil
            c.goodsOutcome = mode.uppercased() == "RETURN" ? draft.goodsMovement.nilIfBlank : nil
            let taxOn = taxModeWire == "INCLUSIVE" || taxModeWire == "EXCLUSIVE"
            c.gstRate = taxOn ? draft.gstRate.nilIfBlank : nil
            c.taxJurisdiction = taxOn
                ? (draft.supplyChoice == "INTRA" || draft.supplyChoice == "INTER" ? draft.supplyChoice : nil)
                : nil
            c.ownership = ["STOCK", "TRANSFER", "PURCHASE"].contains(mode.uppercased())
                ? (draft.ownership.lowercased().hasPrefix("consign") ? "CONSIGNMENT" : "OWNED")
                : nil
            c.locationName = draft.location.nilIfBlank
            if mode.uppercased() == "TRANSFER" {
                c.transferFromName = draft.location.nilIfBlank
                c.transferToName = draft.locationTo.nilIfBlank
            }
            c.orderSide = mode.uppercased() == "ORDER" ? draft.orderSide.nilIfBlank : nil
            if mode.uppercased() == "HOLD" {
                c.holdStatus = draft.holdAction.lowercased().contains("no") ? "RELEASED" : "HELD"
                c.holdReason = draft.note.nilIfBlank
                if draft.holdTarget == "Dispatch" {
                    c.shipmentStatus = "HELD"
                }
            }
            // QA-04 — itemized SALE delivery signal (does not post stock).
            if mode.uppercased() == "SALE",
               draft.entryMode.localizedCaseInsensitiveContains("Item") {
                c.goodsStatus = saleGoodsStatusWire
                if saleGoodsStatusWire == "PARTIAL" {
                    c.quantityReceived = draft.quantityReceived.nilIfBlank
                }
                if saleGoodsStatusWire != "DELIVERED" {
                    c.promisedAt = draft.goodsDueAt.nilIfBlank ?? draft.occurredAt.nilIfBlank
                }
            }
            if !draft.fxNote.isEmpty {
                let note = draft.fxNote.trimmingCharacters(in: .whitespacesAndNewlines)
                // Currency code when 3 letters; otherwise leave as note-only (no FX rate required).
                if note.count == 3, note.allSatisfy(\.isLetter) {
                    c.originalCurrency = note.uppercased()
                }
            }
            return c
        }()

        let noteParts = [
            draft.note.nilIfBlank,
            draft.method.nilIfBlank.map { "Method: \($0)" },
            draft.carrier.nilIfBlank.map { "Carrier: \($0)" },
            mode.uppercased() == "HOLD" ? "\(draft.holdTarget) · \(draft.holdAction)" : nil,
            mode.uppercased() == "RETURN" ? "\(draft.returnDirection) · \(draft.goodsMovement)" : nil,
            mode.uppercased() == "SPEND" && draft.spendDetail
                ? "Group: \(draft.expenseGroup)"
                : nil,
        ].compactMap { $0 }

        let holdEventType = draft.holdTarget == "Dispatch" ? "SHIPMENT" : "ORDER"
        let revises: String? = {
            if let explicit = revisesEventId?.nilIfBlank { return explicit }
            if mode.uppercased() == "HOLD" { return draft.parentEventId.nilIfBlank }
            return nil
        }()
        let parentForPost: String? = mode.uppercased() == "HOLD" ? nil : draft.parentEventId.nilIfBlank

        do {
            _ = try await APIClient.shared.recordOperatingEvent(
                momentId: momentId,
                eventType: mode.uppercased() == "HOLD" ? holdEventType : postEventType,
                title: resolvedTitle,
                amount: effectiveAmount,
                note: noteParts.isEmpty ? nil : noteParts.joined(separator: " · "),
                channelName: draft.channel.nilIfBlank,
                counterpartyName: draft.counterparty.nilIfBlank,
                paymentStatus: needsPaymentStatus ? paymentApiStatus : nil,
                occurredAt: draft.occurredAt.nilIfBlank,
                parentEventId: parentForPost,
                quantity: draft.quantity.nilIfBlank,
                unit: draft.unit.nilIfBlank,
                itemName: draft.itemName.nilIfBlank ?? draft.savedLines.first?.itemName,
                unitPrice: draft.unitPrice.nilIfBlank,
                revisesEventId: revises,
                dueAt: draft.dueAt.nilIfBlank,
                contract: contract,
                lineItems: lines.isEmpty ? nil : lines,
                metricCode: draft.metricCode.nilIfBlank,
                activityStatus: mode.uppercased() == "ORDER" ? draft.activityStatus.nilIfBlank : nil,
                revisionKind: ["BUDGET", "TARGET"].contains(mode.uppercased()) ? draft.revisionKind.nilIfBlank : nil,
                idempotencyKey: UUID().uuidString,
                periodStart: draft.periodStart.nilIfBlank
            )
            confirm = "Saved \(resolvedTitle)"
            onSaved()
        } catch {
            errors = [error.localizedDescription]
        }
    }

    private var taxModeWire: String? {
        switch draft.taxMode.lowercased() {
        case "inclusive": return "INCLUSIVE"
        case "exclusive": return "EXCLUSIVE"
        case "not configured", "none": return "NOT_CONFIGURED"
        default: return draft.taxMode.nilIfBlank
        }
    }

    /// APK `buildDraft` — server only accepts QUICK | ITEMIZED.
    private static func entryModeWire(_ label: String) -> String? {
        let raw = label.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        if raw.isEmpty { return nil }
        if raw.contains("item") { return "ITEMIZED" }
        if raw.contains("quick") { return "QUICK" }
        if raw == "itemized" || raw == "itemised" { return "ITEMIZED" }
        if raw == "quick" || raw == "quick amount" || raw == "quick sale" { return "QUICK" }
        return nil
    }

    /// APK `buildDraft` — server only accepts RECEIVED | PART_PAID | PAY_LATER | PAID.
    private static func paymentStateWire(choice: String, mode: String, receiptState: String) -> String? {
        let raw = choice.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        if mode.uppercased() == "PURCHASE" {
            if raw == "paid" || raw == "received" { return "PAID" }
            if raw.contains("part") { return "PART_PAID" }
            if raw.contains("later") || raw.contains("pending") { return "PAY_LATER" }
            // Fall back from receipt chips when paymentChoice was left default.
            let receipt = receiptState.lowercased()
            if receipt.contains("fully") || receipt.contains("received now") { return "PAID" }
            if receipt.contains("part") { return "PART_PAID" }
            return "PAY_LATER"
        }
        if raw == "received" || raw == "paid" || raw == "walk-in" || raw == "fully paid" {
            return "RECEIVED"
        }
        if raw.contains("part") { return "PART_PAID" }
        if raw.contains("later") || raw.contains("credit") || raw.contains("pending") {
            return "PAY_LATER"
        }
        return raw.isEmpty ? nil : "PAY_LATER"
    }

    /// Server receiptState: RECEIVED | PART_RECEIVED | AWAITING.
    private static func receiptStateWire(_ label: String) -> String? {
        let raw = label.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        if raw.isEmpty { return nil }
        if raw.contains("fully") || raw == "received" || raw.contains("received now") {
            return "RECEIVED"
        }
        if raw.contains("part") { return "PART_RECEIVED" }
        if raw.contains("await") || raw.contains("later") { return "AWAITING" }
        return nil
    }

    /// QA-04 itemized SALE — DELIVERED | PARTIAL | LATER
    private var saleGoodsStatusWire: String {
        let label = draft.goodsStatusLabel.lowercased()
        if label.contains("fully") { return "DELIVERED" }
        if label.contains("partial") { return "PARTIAL" }
        return "LATER"
    }

    private var stockReasonWire: String? {
        let key = draft.stockReason.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        guard !key.isEmpty else { return nil }
        switch key {
        case "opening", "opening count": return "opening"
        case "stocktake", "count", "stock count", "cycle count": return "stocktake"
        case "damage", "damaged": return "damage"
        case "waste": return "waste"
        case "return", "returned": return "return"
        case "correction", "replenishment", "replenish": return "correction"
        case "transfer", "move": return "TRANSFER"
        default: return "correction"
        }
    }

    private var needsPaymentStatus: Bool {
        ["SALE", "PURCHASE", "SPEND"].contains(mode.uppercased())
    }
}

private extension String {
    var nilIfBlank: String? {
        let t = trimmingCharacters(in: .whitespacesAndNewlines)
        return t.isEmpty ? nil : t
    }

    func ifBlankDefault(_ fallback: String) -> String {
        let t = trimmingCharacters(in: .whitespacesAndNewlines)
        return t.isEmpty ? fallback : t
    }
}

import SwiftUI

struct OperatingCaptureDraft {
    var eventType: String
    var title: String
    var amount: String?
    var note: String?
    var channelName: String?
    var counterpartyName: String?
    var paymentStatus: String?
    var occurredAt: String?
    var quantity: String? = nil
    var unit: String? = nil
    var unitPrice: String? = nil
    var itemName: String? = nil
    var parentEventId: String? = nil
    var revisesEventId: String? = nil
    var masterKind: String? = nil
    var conversionToBase: String? = nil
    var dueAt: String? = nil
    var contract: OperatingContractInput? = nil
    var reportingMode: String? = nil
    var metricCode: String? = nil
    var budgetCategory: String? = nil
    var paymentState: String? = nil
    var activityStatus: String? = nil
    var attributedSales: String? = nil
    var revisionKind: String? = nil
    var costBasis: String? = nil
    var overlapDeduped: Bool? = nil
    var creativeVersion: String? = nil
    var resultSource: String? = nil
    var idempotencyKey: String? = nil
    var effectiveFrom: String? = nil
    var periodStart: String? = nil
    var periodEnd: String? = nil
    var supersedesEventId: String? = nil
    var lineItems: [(itemName: String, quantity: String?, unit: String?, unitPrice: String?, masterId: String?)]? = nil
    var rejectedQuantity: String? = nil
    var masterGstApplicable: String? = nil
    var masterGstClassification: String? = nil
    var masterGstRate: String? = nil
    var masterPriceIncludesGst: String? = nil
    var masterSalePriceIncludesGst: String? = nil
    var masterPurchasePriceIncludesGst: String? = nil
    var masterGstEffectiveFrom: String? = nil
    var masterHsn: String? = nil
    var masterGstRegistration: String? = nil
    var masterBusinessState: String? = nil
    var masterBillingState: String? = nil
    var masterDeliveryState: String? = nil
    var masterGstin: String? = nil
}

private struct RetailDraftLine: Identifiable {
    let id = UUID()
    var itemName: String
    var quantity: String
    var unit: String
    var unitPrice: String
    var masterId: String? = nil
}

func operatingCaptureMode(eventType: String, title: String) -> String {
    let label = title.lowercased()
    if label.contains("reimburse") { return "REIMBURSE" }
    if label.contains("invite") { return "INVITE" }
    if label.contains("role") { return "ROLE" }
    if label.contains("person") || label.contains("people") { return "PERSON" }
    if label.contains("supplier") || label.contains("partner") { return "SUPPLIER" }
    if eventType == "VENDOR_COST" { return "VENDOR" }
    if eventType == "CUSTOMER_ORDER" { return "ORDER" }
    if eventType == "APPROVAL", label.contains("request") { return "UPDATE" }
    return eventType
}

private enum CapturePicker: Identifiable {
    case date
    case due
    case goodsDue
    case lov(String)

    var id: String {
        switch self {
        case .date: return "date"
        case .due: return "due"
        case .goodsDue: return "goods"
        case .lov(let kind): return "lov-\(kind)"
        }
    }
}

struct OperatingCaptureSheet: View {
    let title: String
    let icon: String
    let eventType: String
    let mode: String
    let model: OperatingProjectionPayload
    var channelLabel: String = "Channel"
    var variant: String = ""
    var seed: OperatingActivityPayload? = nil
    var onSubmit: (OperatingCaptureDraft) async throws -> Void
    var onDecide: ((String, String) async throws -> Void)?
    var onDismiss: (() -> Void)? = nil
    /// kind, name, unit, optional conversionToBase
    var onCreateMaster: ((String, String, String?, String?) async throws -> Void)? = nil

    @Environment(\.dismiss) private var dismiss
    @State private var reportingMode = "INCREMENTAL"
    @State private var metricCode = "LEADS"
    @State private var budgetCategory = "DIGITAL_ADS"
    @State private var revisionKind = "BUDGET"
    @State private var activityStatus = "IN_PROGRESS"
    @State private var costBasis = "AGENCY"
    @State private var overlapDeduped = false
    @State private var creativeVersion = "1"
    @State private var resultSource = ""
    @State private var periodStart = ""
    @State private var periodEnd = ""
    @State private var supersedesId = ""
    @State private var idempotencyKey = UUID().uuidString
    @State private var attributedSales = ""
    @State private var amount = ""
    @State private var channel = ""
    @State private var party = ""
    @State private var purpose = ""
    @State private var note = ""
    @State private var link = ""
    @State private var detail = ""
    @State private var choice = ""
    @State private var noteKind = "Update"
    @State private var moreOpen = false
    @State private var errorText: String?
    @State private var eventDate = Date()
    @State private var picker: CapturePicker?
    @State private var dueDate = Date()
    @State private var goodsDueDate = Date()
    @State private var itemName = ""
    @State private var quantity = ""
    @State private var rejectedQuantity = ""
    @State private var scrapQuantity = ""
    @State private var quotation = "Accepted"
    @State private var quantityReceived = ""
    @State private var unit = "pc"
    @State private var unitPrice = ""
    @State private var parentId = ""
    @State private var masterKind = ""
    @State private var entryMode = "Quick amount"
    @State private var receivedAmount = ""
    @State private var receiptState = "Received"
    @State private var stockAction = "Set current stock"
    @State private var stockReason = "Stocktake"
    @State private var expenseCategory = "Miscellaneous"
    @State private var orderStatus = "Placed"
    @State private var bookingAction = "Book"
    @State private var ownership = "Owned"
    @State private var clientKey = UUID().uuidString
    @State private var savedLines: [RetailDraftLine] = []
    @State private var productMasterId: String?
    @State private var localMasters: [OperatingLovOption] = []
    @State private var taxMode = "Not configured"
    @State private var gstRate = ""
    @State private var supplyChoice = ""
    @State private var gstDetailsOpen = false
    @State private var gstApplicable = "Not sure"
    @State private var gstClassification = "Taxable"
    @State private var hsn = ""
    @State private var masterPriceIncludes = "Use business default"
    @State private var masterSaleIncludes = "Use business default"
    @State private var masterPurchaseIncludes = "Use business default"
    @State private var gstEffectiveFrom = ""
    @State private var gstPreviewOpen = false
    @State private var partyGstRegistration = "Not sure"
    @State private var businessState = ""
    @State private var billingState = ""
    @State private var deliveryState = ""
    @State private var partyGstin = ""
    @State private var taxAmount = ""
    @State private var originalCurrency = ""
    @State private var fxRate = ""
    @State private var returnDirection = "Customer"
    @State private var goodsOutcome = "To stock"
    @State private var goodsStatus = "Deliver later"
    @State private var orderSide = "Sales order"
    @State private var movement = "Outgoing sale"
    @State private var returnReason = ""

    private var restaurant: Bool { variant == "restaurant" }
    private var counterSaleWords: Bool { restaurant || model.subtype == "RETAIL_STORE" || model.subtype == "RESTAURANT_FOOD" }
    private var saleSettled: Bool { choice == "Received" || choice == "Walk-in" || choice == "Paid" }
    private var service: Bool { variant == "service" }
    private var trading: Bool { variant == "trading" }
    private var distribution: Bool { variant == "distribution" }
    private var client: Bool { variant == "client" }
    private var own: Bool { variant == "own" }
    private var marketing: Bool { client || own }
    private var standard: Bool { variant == "standard" }
    private var job: Bool { variant == "job" }
    private var production: Bool { standard || job }
    private var currency: String { model.currency?.isEmpty == false ? model.currency! : "INR" }
    private var dateText: String {
        let format = DateFormatter()
        format.dateFormat = "dd MMM yyyy"
        format.locale = Locale(identifier: "en_US_POSIX")
        return format.string(from: eventDate)
    }
    private var occurredAtValue: String? {
        if Calendar.current.isDateInToday(eventDate) { return nil }
        var parts = Calendar.current.dateComponents([.year, .month, .day], from: eventDate)
        parts.hour = 0
        parts.minute = 0
        parts.second = 0
        guard let start = Calendar.current.date(from: parts) else { return nil }
        let iso = ISO8601DateFormatter()
        iso.formatOptions = [.withInternetDateTime]
        return iso.string(from: start)
    }
    private var actor: String {
        let role = OperatingMomentCatalog.roles.first { $0.0 == model.role }?.1 ?? "Member"
        let name = model.title?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        return name.isEmpty ? role : "\(role) · \(name)"
    }
    private var channels: [String] { model.life?.channels ?? [] }
    private var retailFamily: Bool {
        ["RETAIL_STORE", "RESTAURANT_FOOD", "CONSUMER_SERVICE"].contains(model.subtype ?? "")
    }
    private var tradingFamily: Bool {
        ["TRADING_WHOLESALE", "DISTRIBUTION_DEALERSHIP"].contains(model.subtype ?? "") || trading || distribution
    }
    private var contractFamily: Bool { retailFamily || tradingFamily }
    private var retailModes: Set<String> { ["SALE", "SERVICE", "PURCHASE", "SPEND", "STOCK", "ORDER", "APPOINTMENT", "COLLECTION", "PAYMENT"] }
    private var tradingModes: Set<String> { ["SALE", "PURCHASE", "SPEND", "STOCK", "ORDER", "COLLECTION", "PAYMENT", "SHIPMENT", "DELIVERY", "RETURN", "UPDATE", "ISSUE"] }
    private var contractModes: Set<String> { retailFamily ? retailModes : tradingModes }
    private let expenseCategories = ["Rent/lease", "Utilities", "Wages/salaries", "Advertising", "Packaging", "Delivery/shipping", "Marketplace/platform fees", "Consumables", "Kitchen supplies", "Software/subscriptions", "Insurance", "Bank charges", "Gas/fuel", "Aggregator fees", "Miscellaneous"]
    private var canAddMaster: Bool {
        model.role == "OWNER_ADMIN" || (model.quickAdds ?? []).contains(where: { $0.code == "UPDATE" })
    }
    private var lovAccent: Color { trading || distribution ? Color(hex: trading ? "#6DDCFF" : "#5DDEA6") : Color(hex: "#B7B8FF") }
    private var allLovOptions: [OperatingLovOption] {
        let fromModel = (model.masters ?? []).map { master in
            let product = master.kind.uppercased() == "PRODUCT"
            let base = product || master.kind.uppercased() == "UOM" ? OperatingSeedUoms.canonical(master.unit) : (master.unit ?? "")
            let shown = base.isEmpty ? master.unit : base
            let subtitle = product && !(shown ?? "").isEmpty ? "Base unit: \(shown!)" : master.unit
            return OperatingLovOption(id: master.id, name: master.name, kind: master.kind, unit: shown, subtitle: subtitle, conversionToBase: master.conversionToBase)
        }
        let expenseSource = tradingFamily ? expenseCategories.filter { !campaignExpense($0) } : expenseCategories
        let expenseSeeds = expenseSource.map {
            OperatingLovOption(id: "seed-expense-\($0)", name: $0, kind: "EXPENSE_CATEGORY", unit: nil, subtitle: nil)
        }
        let channelSeeds = channels.map {
            OperatingLovOption(id: "seed-channel-\($0)", name: $0, kind: "CHANNEL", unit: nil, subtitle: nil)
        }
        let combined = localMasters + fromModel + OperatingSeedUoms.all + expenseSeeds + channelSeeds
        guard tradingFamily else { return combined }
        return combined.filter { $0.kind.uppercased() != "EXPENSE_CATEGORY" || !campaignExpense($0.name) }
    }

    private func campaignExpense(_ name: String) -> Bool {
        let key = name.trimmingCharacters(in: .whitespacesAndNewlines).uppercased().replacingOccurrences(of: " ", with: "_")
        return ["DIGITAL_ADS", "SOCIAL_CONTENT", "INFLUENCER", "EMAIL_DIRECT", "PRINT", "RADIO_AUDIO", "OUTDOOR", "EVENTS", "OUTREACH", "CREATIVE", "RESEARCH", "MARKETING", "ADVERTISING"].contains(key)
    }

    var body: some View {
        VStack(spacing: 0) {
            ScrollView {
                VStack(alignment: .leading, spacing: 6) {
                    Capsule().fill(Color(hex: "#C9C4D8")).frame(width: 36, height: 4).frame(maxWidth: .infinity)
                    HStack(spacing: 10) {
                        Image(icon).renderingMode(.template).resizable().scaledToFit().frame(width: 20, height: 20)
                            .padding(7)
                            .background(Color(hex: "#6C4EF2"))
                            .clipShape(RoundedRectangle(cornerRadius: 10))
                            .foregroundStyle(Color(hex: "#E5E0EE"))
                        Text(title).font(.plusJakarta(size: 20, weight: .semibold)).foregroundStyle(Color(hex: "#E5E0EE"))
                        Spacer()
                        Button("Close  ×") { close() }
                            .font(.plusJakarta(size: 12))
                            .foregroundStyle(Color(hex: "#C9C4D8"))
                            .padding(8)
                            .background(Color(hex: "#35333E"))
                            .clipShape(RoundedRectangle(cornerRadius: 10))
                    }
                    Text(actor).font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#B7B8FF"))
                    Text(statusLine).font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#C9C4D8"))
                    if contractFamily && contractModes.contains(mode) { retailFields } else { fields }
                    if marketing && !tradingFamily && ["ACTIVITY", "PROGRESS", "RESULT", "SPEND", "REVISION", "INVOICE"].contains(eventType) {
                        marketingFields
                    }
                    if let errorText { Text(errorText).font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#F87171")) }
                }
                .padding(.horizontal, 17)
                .padding(.bottom, 8)
            }
            VStack(alignment: .leading, spacing: 6) {
                Text(footerLine).font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#C9C4D8"))
                Button(actionLabel) { Task { await save() } }
                    .font(.plusJakarta(size: 14, weight: .semibold))
                    .foregroundStyle(canSave ? Color(hex: "#2F009C") : Color(hex: "#C9C4D8"))
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 15)
                    .background(canSave ? Color(hex: "#C9BFFF") : Color(hex: "#353A59"))
                    .clipShape(RoundedRectangle(cornerRadius: 14))
                    .disabled(!canSave)
            }
            .padding(.horizontal, 17)
            .padding(.vertical, 10)
            .overlay(alignment: .top) { Rectangle().fill(Color(hex: "#938EA1")).frame(height: 1) }
        }
        .background(Color(hex: "#3A3842"))
        .presentationDetents([.fraction(0.92)])
        .presentationDragIndicator(.hidden)
        .presentationCornerRadius(28)
        .presentationBackground(Color(hex: "#3A3842"))
        .sheet(item: $picker) { picker in
            switch picker {
            case .date:
                datePickerSheet("Date", selection: $eventDate)
            case .due:
                datePickerSheet("Due date", selection: $dueDate)
            case .goodsDue:
                datePickerSheet("Goods due", selection: $goodsDueDate)
            case .lov(let kind):
                SearchableOperatingLovSheet(
                    title: lovTitle(kind),
                    kind: kind,
                    options: lovOptions(kind),
                    canAdd: canAddMaster && !["UOM", "SALES_ORDER", "PURCHASE_ORDER", "OBLIGATION"].contains(kind.uppercased()),
                    accent: lovAccent,
                    unitChoices: kind.uppercased() == "PRODUCT" ? OperatingSeedUoms.all : [],
                    pageSize: ["SALES_ORDER", "PURCHASE_ORDER", "OBLIGATION"].contains(kind.uppercased()) ? 20 : nil,
                    onSelect: { applyLovSelection(kind, $0) },
                    onAdd: { name, unitValue, conversion in
                        Task {
                            do {
                                try await onCreateMaster?(kind, name, unitValue, conversion)
                                let option = OperatingLovOption(
                                    id: "local-\(UUID().uuidString)",
                                    name: name,
                                    kind: kind,
                                    unit: unitValue,
                                    subtitle: unitValue,
                                    conversionToBase: conversion
                                )
                                localMasters.insert(option, at: 0)
                                applyLovSelection(kind, option)
                            } catch {
                                errorText = error.localizedDescription
                            }
                        }
                    },
                    onDismiss: { self.picker = nil }
                )
                .presentationDetents([.medium, .large])
                .preferredColorScheme(.dark)
            }
        }
        .onAppear {
            if channel.trimmingCharacters(in: .whitespaces).isEmpty {
                channel = channels.first { !$0.trimmingCharacters(in: .whitespaces).isEmpty }
                    ?? (model.masters ?? []).first { $0.kind.uppercased() == "CHANNEL" && !$0.name.trimmingCharacters(in: .whitespaces).isEmpty }?.name
                    ?? ""
            }
            if marketing && mode == "DELIVERABLE" && detail.isEmpty { detail = "Due" }
            if production && mode == "PRODUCTION" { entryMode = "Quick output" }
            if production && mode == "DELIVERY" { entryMode = "Quick status" }
            if production && mode == "STOCK" && note.isEmpty { note = "opening" }
            choice = defaultChoice
            if (restaurant || model.subtype == "RESTAURANT_FOOD") && mode == "ORDER" { orderStatus = "Accepted" }
            if model.gstTracking == "YES" {
                let includes = mode == "PURCHASE" ? model.purchasePriceIncludesGst : model.salePriceIncludesGst
                if includes == "YES" { taxMode = "Inclusive" }
                if includes == "NO" { taxMode = "Exclusive" }
            }
            if let seed {
                amount = seed.amount ?? ""
                party = seed.counterpartyName ?? ""
                channel = seed.channelName ?? channel
                note = seed.note ?? ""
                purpose = seed.title
                itemName = seed.title
                quantity = seed.quantity ?? ""
                unit = seed.unit ?? unit
                parentId = seed.parentEventId ?? ""
            }
            if masterKind.isEmpty { masterKind = masterKinds.first ?? "PRODUCT" }
            if tradingFamily && mode == "PURCHASE" && receiptState == "Received" { receiptState = "Fully received" }
        }
    }

    private func datePickerSheet(_ title: String, selection: Binding<Date>) -> some View {
        NavigationStack {
            DatePicker(title, selection: selection, displayedComponents: .date)
                .datePickerStyle(.graphical)
                .labelsHidden()
                .padding()
                .navigationTitle(title)
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .confirmationAction) {
                        Button("Done") { picker = nil }
                    }
                }
        }
        .presentationDetents([.medium, .large])
        .presentationBackground(Color(hex: "#3A3842"))
        .preferredColorScheme(.dark)
    }

    private func lovOptions(_ kind: String) -> [OperatingLovOption] {
        let code = kind.uppercased()
        if code == "SALES_ORDER" || code == "PURCHASE_ORDER" { return orderRows(side: code) }
        if code == "OBLIGATION" { return obligationRows() }
        guard code == "UOM" else { return allLovOptions }
        let base = selectedProductUnit
        guard !base.isEmpty else { return allLovOptions }
        return allLovOptions.filter { option in
            option.kind.uppercased() != "UOM" || OperatingSeedUoms.compatible(option.name, base: base, conversion: selectedProductConversion)
        }
    }

    private var selectedProductUnit: String {
        if let id = productMasterId, let master = (model.masters ?? []).first(where: { $0.id == id }), let masterUnit = master.unit, !masterUnit.isEmpty {
            return OperatingSeedUoms.canonical(masterUnit)
        }
        if let local = localMasters.first(where: { $0.kind.uppercased() == "PRODUCT" && ($0.name == itemName || $0.name == purpose) }), let localUnit = local.unit, !localUnit.isEmpty {
            return OperatingSeedUoms.canonical(localUnit)
        }
        return ""
    }

    private var selectedProductConversion: String? {
        if let id = productMasterId, let master = (model.masters ?? []).first(where: { $0.id == id }) {
            return master.conversionToBase
        }
        return localMasters.first(where: { $0.kind.uppercased() == "PRODUCT" && ($0.name == itemName || $0.name == purpose) })?.conversionToBase
    }

    private func lovTitle(_ kind: String) -> String {
        switch kind.uppercased() {
        case "PRODUCT": return "Product"
        case "SALES_ORDER": return "Sales order"
        case "PURCHASE_ORDER": return "Purchase order"
        case "OBLIGATION": return mode == "COLLECTION" ? "Unpaid sale" : "Unpaid record"
        case "BUYER", "CUSTOMER": return distribution ? "Buyer / dealer" : "Buyer"
        case "SUPPLIER": return distribution ? "Principal / supplier" : "Supplier"
        case "CHANNEL": return "Channel"
        case "EXPENSE_CATEGORY": return "Expense category"
        case "UOM": return "Unit"
        default: return kind
        }
    }

    private func applyLovSelection(_ kind: String, _ option: OperatingLovOption) {
        switch kind.uppercased() {
        case "PRODUCT":
            itemName = option.name
            productMasterId = option.id.hasPrefix("seed-") || option.id.hasPrefix("local-") ? nil : option.id
            let baseUnit = OperatingSeedUoms.canonical(option.unit)
            if !baseUnit.isEmpty { unit = baseUnit }
            if mode == "STOCK" { purpose = option.name }
            if let master = (model.masters ?? []).first(where: { $0.id == option.id }) {
                if let rate = displayGstRate(master.gstRate) { gstRate = rate }
                if master.priceIncludesGst == "YES" { taxMode = "Inclusive" }
                if master.priceIncludesGst == "NO" { taxMode = "Exclusive" }
            }
        case "BUYER", "CUSTOMER", "SUPPLIER":
            party = option.name
            applyPartySupply(option.name)
        case "CHANNEL":
            channel = option.name
        case "EXPENSE_CATEGORY":
            expenseCategory = option.name
        case "UOM":
            unit = option.name
        case "SALES_ORDER", "PURCHASE_ORDER":
            parentId = option.id
            if let row = (model.activity ?? []).first(where: { $0.id == option.id }) {
                if let name = row.counterpartyName, !name.isEmpty { party = name }
                if let channelName = row.channelName, !channelName.isEmpty { channel = channelName }
                if let qty = row.quantity, !qty.isEmpty { quantity = qty }
                if let unitName = row.unit, !unitName.isEmpty { unit = unitName }
                if itemName.isEmpty { itemName = row.title }
            }
        case "OBLIGATION":
            parentId = option.id
            if let row = openObligations.first(where: { $0.id == option.id }) {
                if let name = row.counterpartyName, !name.isEmpty { party = name }
                amount = row.amount
            }
        default:
            break
        }
    }

    private func displayGstRate(_ raw: String?) -> String? {
        guard let raw, !raw.trimmingCharacters(in: .whitespaces).isEmpty else { return nil }
        guard let value = Decimal(string: raw) else { return raw }
        return NSDecimalNumber(decimal: value).stringValue
    }

    private func applyPartySupply(_ name: String) {
        guard let party = (model.masters ?? []).first(where: {
            $0.name.caseInsensitiveCompare(name) == .orderedSame && ["CUSTOMER", "BUYER", "SUPPLIER"].contains($0.kind.uppercased())
        }) else { return }
        let business = (model.gstState ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
        let supply = [party.deliveryState, party.billingState, party.businessState]
            .compactMap { $0?.trimmingCharacters(in: .whitespacesAndNewlines) }
            .first { !$0.isEmpty } ?? ""
        guard !business.isEmpty, !supply.isEmpty else { return }
        supplyChoice = supply.caseInsensitiveCompare(business) == .orderedSame ? "INTRA" : "INTER"
    }

    @ViewBuilder private func lovField(_ label: String, value: String, kind: String, placeholder: String) -> some View {
        Button { picker = .lov(kind) } label: {
            VStack(alignment: .leading, spacing: 4) {
                Text(label).font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#C9C4D8"))
                Text(value.isEmpty ? placeholder : value)
                    .font(.plusJakarta(size: 15))
                    .foregroundStyle(value.isEmpty ? Color(hex: "#9AA3B2") : Color(hex: "#E5E0EE"))
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(12)
                    .background(Color(hex: "#2A2832"))
                    .clipShape(RoundedRectangle(cornerRadius: 12))
            }
        }
        .buttonStyle(.plain)
    }

    @ViewBuilder private var fields: some View {
        switch mode {
        case "SALE", "SERVICE":
            saleServiceFields
        case "PURCHASE", "VENDOR":
            purchaseVendorFields
        case "SPEND":
            spendFields
        case "PAYMENT", "REIMBURSE":
            paymentFields
        case "COLLECTION":
            collectionFields
        case "BUDGET", "TARGET":
            budgetTargetFields
        case "ISSUE":
            issueFields
        case "STOCK":
            stockFields
        case "ORDER":
            orderFields
        case "SHIPMENT":
            shipmentFields
        case "APPOINTMENT":
            appointmentFields
        case "PERSON", "ROLE", "INVITE":
            personFields
        case "SUPPLIER", "PARTNER":
            supplierFields
        case "MASTERS":
            mastersFields
        case "APPROVAL":
            approvalFields
        default:
            fallbackFields
        }
    }

    @ViewBuilder private var saleServiceFields: some View {
        lineEditor
        if production && mode == "SALE" {
            amountBlock("Amount", hint: "Quick amount posts revenue and no SKU quantity")
            if !channels.isEmpty {
                choiceBlock("Buyer / channel", options: channels, selected: channel) { channel = $0 }
            } else {
                editField("Buyer", text: $party, placeholder: "Required when a balance remains")
            }
            dateField("Sale date")
            choiceBlock("Payment state", options: ["Received", "Part paid", "Pay later"], selected: choice) { choice = $0 }
            if choice == "Part paid" { editField("Received now", text: $receivedAmount, placeholder: "Required") }
            orderLink("Linked order · Optional")
            Text("A sale is not a delivery. Dispatch stays a separate record.")
                .font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#C9C4D8"))
            summary("After save", saleLines)
        } else {
        amountBlock("Amount", hint: "Positive amount · 2 decimal places")
        if !channels.isEmpty { choiceBlock(channelLabel, options: channels, selected: channel) { channel = $0 } }
        if distribution && mode == "SALE" { editField("Buyer / dealer", text: $party, placeholder: "Required") }
        HStack(spacing: 12) { dateField(restaurant || service || trading || distribution ? "Sale date" : "Date"); readField(restaurant || service || trading || distribution ? "Receipt actor" : "Recorded by", actorName) }
        choiceBlock(trading ? "Received status" : "Money received", options: ["Received", "Pending"], selected: choice) { choice = $0 }
        Text(distribution ? "One sale and its linked receipt. This is not a separate collection." : (trading ? "One sale and its linked receipt. This is not a second collection." : (service ? "One sale and its linked receipt. This is not a second sale or collection." : (restaurant ? "One sale and its linked receipt. This is not a separate collection." : "One sale and its receipt. A collection is a separate event."))))
            .font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#C9C4D8"))
        moreButton(distribution ? "Note and reference. Optional." : (trading ? "Buyer, reference, and shipment context. Optional." : (service ? "Customer, note and evidence. Optional." : "Note, reference and evidence. Optional.")))
        if moreOpen { editField("Note", text: $note, placeholder: "Optional") }
        summary("After save", saleLines)
        }
    }

    @ViewBuilder private var purchaseVendorFields: some View {
        lineEditor
        if production && mode == "PURCHASE" {
            amountBlock("Amount", hint: "Counted once toward budget and payables")
            HStack(spacing: 12) { editField("Supplier", text: $party, placeholder: "Required"); dateField("Purchase date") }
            editField("Material", text: $itemName, placeholder: "Optional item")
            HStack(spacing: 8) {
                editField("Qty", text: $quantity, placeholder: "Qty")
                editField("Unit", text: $unit, placeholder: "kg")
            }
            choiceBlock("Materials received", options: ["Received", "Part received", "Awaiting"], selected: receiptState) { receiptState = $0 }
            if receiptState == "Part received" { editField("Quantity received", text: $quantityReceived, placeholder: "Required") }
            choiceBlock("Payment state", options: ["Paid", "Part paid", "Pay later"], selected: choice) { choice = $0 }
            if choice == "Part paid" { editField("Paid now", text: $receivedAmount, placeholder: "Required") }
            Text("Stock increases only for the quantity you mark as received.")
                .font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#C9C4D8"))
        } else if marketing && mode == "VENDOR" {
            amountBlock("Amount", hint: "Positive amount · 2 decimal places")
            editField("Counterparty", text: $party, placeholder: "Required")
            editField("Purpose", text: $purpose, placeholder: "What this covers")
            choiceBlock("Paid / pending", options: ["Pending / unpaid", "Paid"], selected: choice) { choice = $0 }
            Text("Pending approval. Submission is not payment.").font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#C9C4D8"))
            summary("After approval", purchaseLines)
        } else {
        amountBlock("Amount", hint: "Positive amount · 2 decimal places")
        HStack(spacing: 12) { editField(distribution ? "Principal · Optional" : "Supplier · Optional", text: $party, placeholder: "Name"); dateField(restaurant || service || trading || distribution ? "Purchase date" : "Date") }
        editField("Purpose", text: $purpose, placeholder: "What was bought")
        choiceBlock("Payment state", options: ["Pending / unpaid", "Paid"], selected: choice) { choice = $0 }
        Text("Pending approval. Submission is not payment.").font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#C9C4D8"))
        moreButton("Due date, reference and evidence. Optional.")
        if moreOpen { editField("Note", text: $note, placeholder: "Optional") }
        summary("After approval", purchaseLines)
        }
    }

    @ViewBuilder private var spendFields: some View {
        if production {
            amountBlock("Amount", hint: "Eligible cost once")
            editField("Purpose", text: $purpose, placeholder: "What this was for")
            dateField("Expense date")
            choiceBlock("Payment state", options: ["Paid", "Pay later"], selected: choice) { choice = $0 }
            if choice != "Paid" { editField("Payee", text: $party, placeholder: "Required") }
            Text("A payment later is not a second expense.")
                .font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#C9C4D8"))
        } else if marketing {
            amountBlock("Amount", hint: "Positive amount · 2 decimal places")
            editField("Purpose", text: $purpose, placeholder: "What this was for")
            editField("Paid by", text: $party, placeholder: actorName)
            if !channels.isEmpty { choiceBlock(channelLabel, options: channels, selected: channel) { channel = $0 } }
            choiceBlock("Paid / pending", options: ["Pending / unpaid", "Paid"], selected: choice) { choice = $0 }
            summary("After approval", spendLines)
        } else {
        amountBlock("Amount", hint: "Positive amount · 2 decimal places")
        HStack(spacing: 12) { editField("Purpose", text: $purpose, placeholder: "What this was for"); dateField(restaurant || service || trading || distribution ? "Expense date" : "Date") }
        choiceBlock("Funding source", options: ["Business", "Personal"], selected: choice) { choice = $0 }
        editField("Paid by", text: $party, placeholder: actorName)
        if service || trading || distribution {
            Text("A personal claim is reimbursed later. It is not a second expense.")
                .font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#C9C4D8"))
        }
        moreButton("Reference and note. Optional.")
        if moreOpen { editField("Note", text: $note, placeholder: "Optional") }
        summary("After approval", spendLines)
        }
    }

    @ViewBuilder private var paymentFields: some View {
        obligationPicker
        if marketing && mode == "PAYMENT" {
            amountBlock("Payment amount", hint: "Positive amount · 2 decimal places")
            editField("Who paid", text: $detail, placeholder: "Payer")
            editField("Paid to", text: $party, placeholder: "Required")
            editField("Purpose / reference", text: $purpose, placeholder: "Required")
            dateField("Date")
            Text("Manual confirmation only. This is not a live transfer.").font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#C9C4D8"))
        } else {
        amountBlock(mode == "REIMBURSE" ? "Reimbursement amount" : "Amount", hint: "Positive amount · 2 decimal places")
        editField((service || trading || distribution) && mode == "REIMBURSE" ? "Business payer" : (mode == "REIMBURSE" ? "Payee" : "Paid to"), text: $party, placeholder: "Name")
        dateField((restaurant || service || trading || distribution) && mode == "REIMBURSE" ? "Recorded date" : "Date")
        editField("Reference · Optional", text: $note, placeholder: "Optional")
        summary("After save", ["Business paid moves by the amount you enter.", "This settles an existing claim. It is not a second expense."])
        }
    }

    @ViewBuilder private var collectionFields: some View {
        obligationPicker
        if production {
            if !channels.isEmpty {
                choiceBlock("Received from", options: channels, selected: channel) { channel = $0 }
            } else {
                editField("Received from", text: $party, placeholder: "Required")
            }
            amountBlock("Amount received", hint: "Positive amount")
            HStack(spacing: 12) { readField("Received by", actorName); dateField("Date") }
            editField("Note", text: $note, placeholder: "Optional")
            Text("Records the receipt. This is not a live transfer.").font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#C9C4D8"))
            summary("After mark received", collectionLines)
        } else if client {
            choiceBlock("Collection path", options: ["Mark Received / Collect", "Create Receivable"], selected: choice) { choice = $0 }
            Text("Mark Received records a receipt. Create Receivable records the obligation.")
                .font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#C9C4D8"))
            amountBlock("Amount received", hint: "Positive amount")
            editField("Received from", text: $party, placeholder: "Required")
            HStack(spacing: 12) { readField("Received by", actorName); dateField("Date") }
            editField("Purpose / reference", text: $purpose, placeholder: "Required")
            summary("After mark received", collectionLines)
        } else if own {
            choiceBlock("Received from", options: ["Linked campaign receipt", "Other campaign receipt"], selected: choice) { choice = $0 }
            Text("Campaign-linked receipt only. No client billing or receivable.")
                .font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#C9C4D8"))
            amountBlock("Amount received", hint: "Positive amount")
            HStack(spacing: 12) { readField("Received by", actorName); dateField("Date") }
            editField("Purpose / reference", text: $purpose, placeholder: "Required")
            summary("After mark received", collectionLines)
        } else {
        if !channels.isEmpty { choiceBlock(distribution ? "Buyer / dealer" : "Received from", options: channels, selected: channel) { channel = $0 } }
        amountBlock("Amount received", hint: "Positive amount")
        HStack(spacing: 12) { readField("Received by", actorName); dateField("Date") }
        editField("Note · Optional", text: $note, placeholder: "Optional")
        summary("After mark received", collectionLines)
        }
    }

    @ViewBuilder private var budgetTargetFields: some View {
        if production && mode == "BUDGET" {
            dateField("Plan month")
            choiceBlock("Revise", options: ["Budget", "Target"], selected: choice) { choice = $0 }
            summary("Current plan", [
                "Budget \(money(model.budget?.amount)) · Target \(money(model.target?.value))",
                "The current plan stays active until this revision is approved.",
            ])
            amountBlock(choice == "Target" ? "Proposed target" : "Proposed budget", hint: "Proposal only")
            editField("Revision reason", text: $purpose, placeholder: "Required")
            summary("After approval", budgetLines)
        } else if client && mode == "TARGET" {
            readField("Campaign", model.title?.isEmpty == false ? model.title! : "This campaign")
            editField("Metric", text: $purpose, placeholder: "Required")
            choiceBlock("Revision reason", options: ["Extend launch outcome", "Change launch scope", "Correct planning error"], selected: choice) { choice = $0 }
            editField("Revision note", text: $note, placeholder: "Required")
            dateField("Effective date")
            amountBlock("Proposed target", hint: "Proposal only")
            summary("Current plan", ["The current target stays active until this revision is approved."])
        } else if own && mode == "TARGET" {
            dateField("Effective date")
            choiceBlock("Target measure", options: ["Leads", "Revenue / Sales", "Orders / Conversions", "Custom Count"], selected: choice) { choice = $0 }
            amountBlock("Proposed target", hint: "Positive value")
            editField("Revision reason", text: $purpose, placeholder: "Required")
            summary("Current plan", ["The current target stays active until this revision is approved."])
        } else if marketing && mode == "BUDGET" {
            dateField("Plan month")
            summary("Current plan", [
                "Budget \(money(model.budget?.amount)) · Used \(money(model.budget?.spent))",
                "The current plan stays active until this revision is approved.",
            ])
            amountBlock("Proposed budget", hint: "Proposed only. The current plan stays active until approval.")
            editField("Revision reason", text: $purpose, placeholder: "Required")
            summary("After approval", budgetLines)
        } else if tradingFamily && mode == "BUDGET" {
            dateField("Plan month")
            if model.budget?.configured == true {
                summary("Current plan", [
                    "Budget \(money(model.budget?.amount)) · Used \(money(model.budget?.spent))",
                    "The current plan stays active until this revision is approved.",
                ])
            } else {
                Text("No budget yet. A proposal stays inactive until it is approved.")
                    .font(.plusJakarta(size: 13)).foregroundStyle(Color(hex: "#C9C4D8"))
            }
            amountBlock("Proposed budget", hint: "Proposal only")
            editField("Revision reason", text: $purpose, placeholder: "Why this changes")
            summary("After approval", budgetLines)
        } else {
        dateField(mode == "TARGET" ? "Plan" : "Plan month")
        summary("Current plan", [
            "Budget \(money(model.budget?.amount)) · Used \(money(model.budget?.spent))",
            "The current plan stays active until this revision is approved.",
        ])
        amountBlock(mode == "TARGET" ? "Proposed target" : "Proposed budget", hint: "Proposal only")
        editField("Revision reason", text: $purpose, placeholder: "Why this changes")
        summary("After approval", budgetLines)
        }
    }

    @ViewBuilder private var issueFields: some View {
        editField("Issue", text: $purpose, placeholder: "What needs attention")
        choiceBlock("Priority", options: ["Low", "Normal", "High"], selected: choice) { choice = $0 }
        editField("Owner", text: $party, placeholder: actorName)
        if restaurant || service || trading || distribution || marketing || production {
            dateField("Due date")
            editField("Operational context", text: $note, placeholder: "Optional")
        }
        summary("After save", ["Opens an attention item.", "Money totals stay unchanged."])
    }

    @ViewBuilder private var stockFields: some View {
        if production {
            editField("Item", text: $purpose, placeholder: "Required")
            choiceBlock("Action", options: ["Set", "Add", "Reduce"], selected: choice) { choice = $0 }
            choiceBlock("Reason", options: ["opening", "stocktake", "damage", "waste", "return", "correction"], selected: note) { note = $0 }
            HStack(spacing: 12) { editField("Quantity", text: $quantity, placeholder: "Required"); editField("Unit", text: $unit, placeholder: "kg") }
            Text("Damage is not an expense or a sale.")
                .font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#C9C4D8"))
        } else {
        editField(distribution ? "Availability context" : "Stock context", text: $purpose, placeholder: "What changed")
        choiceBlock(distribution ? "Availability status" : "Stock status", options: distribution ? ["Available", "Needs check", "Limited availability"] : (trading ? ["Available", "Needs check", "Low availability"] : ["In stock", "Needs check", "Low stock"]), selected: choice) { choice = $0 }
        editField(distribution ? "Availability note" : "Stock note", text: $note, placeholder: trading || distribution ? "Required" : "What you saw")
        if trading || distribution {
            HStack(spacing: 12) { readField("Recorded by", actorName); dateField("Draft time") }
        } else {
            readField("Recorded by", actorName)
        }
        summary("After save", ["Saves an operational signal.", "No purchase, sale, or money record is created."])
        }
    }

    @ViewBuilder private var orderFields: some View {
        if production {
            if job || model.subtype == "MADE_TO_ORDER" {
                choiceBlock("Quotation", options: ["Draft", "Sent", "Accepted", "Rejected", "Expired"], selected: quotation) { quotation = $0 }
                Text("Accepted confirms the order. It does not post revenue, a receivable, or GST.")
                    .font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#C9C4D8"))
            }
            editField("Buyer", text: $party, placeholder: "Required")
            editField("Quantity", text: $quantity, placeholder: "Required")
            editField("Unit", text: $unit, placeholder: "pcs")
            amountBlock("Order value · Optional", hint: "Leave blank when the order is unpriced")
            dateField("Order date")
            Text("An order is not revenue, cash, stock, or a receivable. A deposit is a separate collection.")
                .font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#C9C4D8"))
        } else if restaurant {
            editField("Order / context", text: $purpose, placeholder: "What changed")
            choiceBlock("Order status", options: ["Preparing", "Ready", "Handoff delayed"], selected: choice) { choice = $0 }
            editField("Operational note", text: $note, placeholder: "Optional")
            editField("Customer", text: $party, placeholder: "Optional")
            HStack(spacing: 12) { readField("Recorded by", actorName); dateField("Draft time") }
            summary("After save", ["Saves an operational note.", "This does not create a sale or a money record."])
        } else if distribution {
            editField("Buyer / dealer", text: $party, placeholder: "Required")
            editField("Order / dispatch reference", text: $purpose, placeholder: "Required")
            choiceBlock("Fulfillment status", options: ["Open", "In progress", "Dispatched", "Completed", "Cancelled", "Delayed", "Blocked"], selected: choice) { choice = $0 }
            editField("Fulfillment note", text: $note, placeholder: "Optional")
            amountBlock("Value · Optional", hint: "Leave blank if there is no amount")
            HStack(spacing: 12) { readField("Recorded by", actorName); dateField("Draft time") }
            summary("After save", ["Saves a dispatch note.", "This does not create a sale or a money record."])
        } else if trading {
            editField("Buyer / order reference", text: $purpose, placeholder: "Who and which order")
            choiceBlock("Dispatch status", options: ["Pending", "Ready for dispatch", "In progress", "Delayed", "Blocked", "Completed"], selected: choice) { choice = $0 }
            editField("Dispatch note", text: $note, placeholder: "What changed")
            HStack(spacing: 12) { readField("Recorded by", actorName); dateField("Draft time") }
            summary("After save", ["Saves an operational note.", "Money totals stay unchanged."])
        } else {
            editField("Order", text: $purpose, placeholder: "What happened")
            editField("Name", text: $party, placeholder: "Who this is for")
            HStack(spacing: 12) { readField("Recorded by", actorName); dateField("Draft time") }
            summary("After save", ["Saved as activity on this business.", "Money totals stay as they are unless this record carries an amount."])
        }
    }

    @ViewBuilder private var shipmentFields: some View {
        if distribution {
            editField("Shipment / delivery reference", text: $purpose, placeholder: "Required")
            choiceBlock("Delivery status", options: ["Delivered", "Out for delivery", "Delayed", "Blocked", "Cancelled"], selected: choice) { choice = $0 }
            editField("Delivery note", text: $note, placeholder: "Required")
            editField("Dealer", text: $party, placeholder: "Required")
            editField("Linked dispatch", text: $link, placeholder: "Optional")
            amountBlock("Value · Optional", hint: "Leave blank if there is no amount")
            summary("After save", ["Saves a delivery note.", "Money totals stay unchanged."])
        } else if trading {
            editField("Shipment / delivery reference", text: $purpose, placeholder: "Which shipment")
            choiceBlock("Delivery status", options: ["Pending", "Out for delivery", "Delayed", "Blocked", "Delivered"], selected: choice) { choice = $0 }
            editField("Delivery note", text: $note, placeholder: "What changed")
            HStack(spacing: 12) { readField("Recorded by", actorName); dateField("Draft time") }
            summary("After save", ["Saves a shipment note.", "Money totals stay unchanged."])
        } else {
            editField("Shipment", text: $purpose, placeholder: "What happened")
            editField("Name", text: $party, placeholder: "Who this is for")
            HStack(spacing: 12) { readField("Recorded by", actorName); dateField("Draft time") }
            summary("After save", ["Saved as activity on this business.", "Money totals stay as they are unless this record carries an amount."])
        }
    }

    @ViewBuilder private var appointmentFields: some View {
        if service {
            editField("Job / context", text: $purpose, placeholder: "What the job is")
            if !channels.isEmpty { choiceBlock(channelLabel, options: channels, selected: channel) { channel = $0 } }
            HStack(spacing: 12) { dateField("Appointment date"); editField("Professional", text: $party, placeholder: "Who is assigned") }
            choiceBlock("Job status", options: ["Scheduled", "Completed"], selected: choice) { choice = $0 }
            Text("Job status is operational only. Completing a job does not create revenue or a receivable.")
                .font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#C9C4D8"))
            moreButton("Customer. Optional.")
            if moreOpen { editField("Customer", text: $note, placeholder: "Optional") }
            summary("After save", ["Saves the job as activity.", "Revenue and collections stay unchanged."])
        } else {
            editField("Appointment", text: $purpose, placeholder: "What happened")
            editField("Name", text: $party, placeholder: "Who this is for")
            HStack(spacing: 12) { readField("Recorded by", actorName); dateField("Draft time") }
            summary("After save", ["Saved as activity on this business.", "Money totals stay as they are unless this record carries an amount."])
        }
    }

    @ViewBuilder private var personFields: some View {
        editField("Name", text: $party, placeholder: "Person")
        editField(restaurant ? "Restaurant-facing role" : (service ? "Service-facing role" : (trading ? "Trade-facing role" : (distribution ? "Distribution-facing role" : (marketing ? "Campaign-facing role" : (production ? "Manufacturing role" : "Role"))))), text: $purpose, placeholder: "How they work here")
        choiceBlock("Access", options: OperatingMomentCatalog.roles.map(\.1), selected: choice) { choice = $0 }
        if restaurant || service || trading || distribution || marketing || production { editField("Scope", text: $note, placeholder: "Optional") }
        Text("A job label is not a grant. Access applies after the invitation is accepted.")
            .font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#C9C4D8"))
    }

    @ViewBuilder private var supplierFields: some View {
        if production && mode == "PARTNER" {
            editField("Supplier name", text: $party, placeholder: "Required")
            Text("Enter a supplier name to enable Save.").font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#B7B8FF"))
            editField("What they supply", text: $purpose, placeholder: "Optional")
            editField("Payment terms", text: $note, placeholder: "Optional")
            amountBlock("Opening due · Optional", hint: "Leave blank if nothing is owed")
            if !channels.isEmpty { choiceBlock("Channel", options: channels, selected: channel) { channel = $0 } }
            Text("Saving creates no purchase or expense.").font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#C9C4D8"))
        } else if marketing && mode == "PARTNER" {
            editField(own ? "Partner / counterparty" : "Name", text: $party, placeholder: "Required")
            Text("Enter a name to enable Save.").font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#B7B8FF"))
            if client {
                choiceBlock("Relationship", options: ["Client", "Vendor", "Creator", "Platform", "Partner"], selected: choice) { choice = $0 }
                editField("Purpose", text: $purpose, placeholder: "Optional")
            }
            if !channels.isEmpty { choiceBlock(own ? "Accepted channel" : "Channel", options: channels, selected: channel) { channel = $0 } }
            Text(own ? "Saving creates no spend event." : "Saving creates no purchase or expense.")
                .font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#C9C4D8"))
        } else {
        editField(distribution ? "Principal / supplier name" : (restaurant || service || trading ? "Supplier name" : "Name"), text: $party, placeholder: "Name")
        if restaurant || service || trading || distribution {
            Text(service ? "A name is required to save this relationship." : (distribution ? "Enter principal / supplier name to enable Save." : "Enter a supplier name to enable Save."))
                .font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#E8C547"))
        }
        editField(distribution ? "Goods supplied" : "What they supply", text: $purpose, placeholder: restaurant ? "Ingredients or packaging" : (trading || distribution ? "Stock or packaging" : "Goods or service"))
        choiceBlock("Payment terms", options: ["Due on receipt", "7 days", "30 days"], selected: choice) { choice = $0 }
        amountBlock("Opening due", hint: "Leave at 0 if nothing is owed")
        if !channels.isEmpty { choiceBlock(restaurant || service || trading || distribution ? "Where you sell" : "Channel", options: channels, selected: channel) { channel = $0 } }
        Text("Saving creates the relationship. It does not create a purchase or an expense.")
            .font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#C9C4D8"))
        }
    }

    @ViewBuilder private var mastersFields: some View {
        choiceBlock("Master", options: masterKinds, selected: masterKind) { masterKind = $0 }
        editField("Name", text: $itemName, placeholder: "Required")
        if ["PRODUCT", "MATERIAL", "CONSUMABLE"].contains(masterKind) {
            if tradingFamily {
                choiceBlock("Base unit", options: OperatingSeedUoms.all.map(\.name), selected: unit) { unit = $0 }
            } else {
                choiceBlock("Unit", options: ["piece", "kg", "litre", "count"], selected: unit) { unit = $0 }
            }
        }
        editField("Detail", text: $purpose, placeholder: "Optional")
        masterGstFields
        Text("Saving adds a selectable record. It does not create a sale or a cost.")
            .font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#C9C4D8"))
    }

    @ViewBuilder private var fallbackFields: some View {
        if production && mode == "PRODUCTION" {
            choiceBlock(model.subtype == "MADE_TO_ORDER" ? "Job output" : "Standard output", options: ["Quick output", "Detailed batch"], selected: entryMode) { entryMode = $0 }
            amountBlock("Output quantity", hint: "Recorded output. This is not accepted stock.")
            HStack(spacing: 12) { editField("Unit", text: $detail, placeholder: "Required"); dateField("Date") }
            editField(entryMode == "Detailed batch" ? "Finished product" : "Output context", text: $purpose, placeholder: "Required for a detailed batch")
            if entryMode == "Detailed batch" {
                editField("Accepted quantity", text: $quantity, placeholder: "Required to add finished stock")
                editField("Rejected quantity", text: $rejectedQuantity, placeholder: "Optional")
                editField("Scrap quantity", text: $scrapQuantity, placeholder: "Does not become finished stock")
                choiceBlock("Status", options: ["Planned", "In progress", "Completed"], selected: choice) { choice = $0 }
            }
            orderLink(job ? "Linked order · Optional" : "Linked order · Optional")
            Text(model.subtype == "MADE_TO_ORDER"
                 ? "Job output does not create a finished SKU. Finished stock comes only from the accepted quantity."
                 : "Standard output does not create a finished SKU. Finished stock comes only from the accepted quantity.")
                .font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#C9C4D8"))
        } else if production && mode == "UPDATE" {
            choiceBlock("Record", options: ["Update", "Issue", "Approval", "Budget", "Target"], selected: noteKind) { noteKind = $0 }
            if noteKind == "Budget" || noteKind == "Target" {
                amountBlock(noteKind == "Target" ? "Proposed target" : "Proposed budget", hint: "Effective when approved")
                editField("Reason", text: $purpose, placeholder: "Required")
                dateField("Effective date")
            } else {
                editField("What happened", text: $purpose, placeholder: "Required")
                dateField("Date")
            }
        } else if production && mode == "DELIVERY" {
            orderLink("Customer order")
            choiceBlock("Delivery", options: ["Quick status", "Itemized"], selected: entryMode) { entryMode = $0 }
            choiceBlock("Status", options: ["Planned", "Dispatched", "Delivered", "Accepted", "Returned"], selected: choice) { choice = $0 }
            if entryMode == "Itemized" {
                editField("Delivered quantity", text: $quantity, placeholder: "Cannot exceed the open order")
                editField("Unit", text: $unit, placeholder: "pcs")
            }
            dateField("Delivery date")
            Text("A status-only delivery changes status and does not post a quantity or a sale.")
                .font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#C9C4D8"))
        } else if marketing && mode == "RESULT" && eventType == "PROGRESS" {
            amountBlock("Result quantity", hint: "Leave blank to update status only")
            dateField("Result date")
        } else if marketing && mode == "RESULT" {
            amountBlock("Value", hint: "Positive amount · 2 decimal places")
            if !channels.isEmpty { choiceBlock(channelLabel, options: channels, selected: channel) { channel = $0 } }
            dateField("Result date")
            choiceBlock("Status", options: ["Accepted", "Pending"], selected: choice) { choice = $0 }
            moreButton("Optional note, evidence link or source context.")
            if moreOpen { editField("Note", text: $note, placeholder: "Optional") }
            Text("Accepted Result counts toward target, not its Receipt.")
                .font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#C9C4D8"))
        } else if marketing && mode == "UPDATE" {
            editField("Update text", text: $purpose, placeholder: "Required")
            editField("Linked operational context", text: $note, placeholder: "Optional")
            HStack(spacing: 12) { readField("Recorded by", actorName); dateField("Draft time") }
            summary("After save", ["Saved as campaign activity.", "Money totals stay unchanged."])
        } else if marketing && mode == "DELIVERABLE" {
            editField("Deliverable / milestone name", text: $purpose, placeholder: "Required")
            choiceBlock("Type", options: ["Deliverable", "Milestone"], selected: choice) { choice = $0 }
            editField("Owner", text: $party, placeholder: "Required")
            HStack(spacing: 12) { dateField("Due date"); choiceBlock("Status", options: ["Launched", "Due", "Complete", "Blocked"], selected: detail) { detail = $0 } }
            if !channels.isEmpty { choiceBlock("Channel", options: channels, selected: channel) { channel = $0 } }
            editField("Dependency", text: $note, placeholder: "Optional")
            Text("Launched, Due, Complete, or Blocked. Closing retains obligations.")
                .font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#C9C4D8"))
        } else if marketing && mode == "COMPLETE" {
            editField("Result / deliverable reference", text: $purpose, placeholder: "Required")
            choiceBlock("Completion status", options: ["Assigned", "In review", "Blocked", "Complete", "Partial"], selected: choice) { choice = $0 }
            editField("Completion note", text: $note, placeholder: "Required")
            dateField("Completion date")
            Text("Completion does not add revenue or a receipt.")
                .font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#C9C4D8"))
        } else {
        if mode == "RESULT" || mode == "PRODUCTION" {
            amountBlock(mode == "PRODUCTION" ? "Output" : "Result", hint: "Amount for this record")
            if !channels.isEmpty { choiceBlock(channelLabel, options: channels, selected: channel) { channel = $0 } }
        }
        editField((service || trading || distribution) && mode == "UPDATE" ? "Update text" : (restaurant && mode == "UPDATE" ? "Update" : textLabel), text: $purpose, placeholder: "What happened")
        if (restaurant || service || trading || distribution) && mode == "UPDATE" {
            editField(service || trading || distribution ? "Linked operational context" : "Linked context", text: $note, placeholder: "Optional")
        }
        if ["DELIVERY"].contains(mode) {
            editField("Name", text: $party, placeholder: "Who this is for")
        }
        HStack(spacing: 12) { readField("Recorded by", actorName); dateField("Draft time") }
        summary(
            "After save",
            (trading || distribution) && mode == "UPDATE"
                ? ["Saved as activity on this business.", "Money totals stay unchanged."]
                : (service && mode == "UPDATE"
                ? ["Saved as activity on this service.", "Money totals stay unchanged."]
                : (restaurant && mode == "UPDATE"
                    ? ["Saved as activity on this restaurant.", "Money totals stay unchanged."]
                    : ["Saved as activity on this business.", "Money totals stay as they are unless this record carries an amount."]))
        )
        }
    }


    @ViewBuilder private var approvalFields: some View {
        let pending = (model.attention ?? []).filter { $0.kind == "APPROVAL" }
        if pending.isEmpty || onDecide == nil {
            Text("Nothing is waiting for a decision.").font(.plusJakarta(size: 14)).foregroundStyle(Color(hex: "#C9C4D8"))
        } else {
            ForEach(pending) { item in
                summary("Waiting", [item.title, "Still pending until you decide."])
            }
            choiceBlock("Decision", options: marketing ? ["Approve", "Reject", "Return"] : ["Approve", "Reject"], selected: choice) { choice = $0 }
            editField("Decision reason", text: $note, placeholder: marketing ? "Required for Reject or Return" : "Required when you reject")
            Text("Approval records the decision. It does not move money by itself.")
                .font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#C9C4D8"))
        }
    }

    private var actorName: String { actor.split(separator: "·").first.map { String($0).trimmingCharacters(in: .whitespaces) } ?? actor }
    private var defaultChoice: String {
        if counterSaleWords && mode == "SALE" { return "Walk-in" }
        if contractFamily && (mode == "SALE" || mode == "SERVICE") { return "Received" }
        if contractFamily && (mode == "PURCHASE" || mode == "SPEND") { return "Pay later" }
        if tradingFamily && (mode == "SHIPMENT" || mode == "DELIVERY") { return mode == "DELIVERY" ? "Delivered" : "In transit" }
        if production && mode == "SALE" { return "Received" }
        if production && mode == "ORDER" { return "Open" }
        if production && mode == "DELIVERY" { return "Delivered" }
        if production && mode == "STOCK" { return "Add" }
        if production && mode == "PURCHASE" { return "Pay later" }
        if production && mode == "SPEND" { return "Paid" }
        if production && mode == "BUDGET" { return "Budget" }
        if marketing && mode == "RESULT" { return "Accepted" }
        if marketing && mode == "SPEND" { return "Pending / unpaid" }
        if client && mode == "COLLECTION" { return "Mark Received / Collect" }
        if own && mode == "COLLECTION" { return "Linked campaign receipt" }
        if client && mode == "TARGET" { return "Extend launch outcome" }
        if own && mode == "TARGET" { return "Leads" }
        if marketing && mode == "DELIVERABLE" { return "Deliverable" }
        if marketing && mode == "COMPLETE" { return "Complete" }
        if marketing && mode == "PARTNER" { return "Partner" }
        switch mode {
        case "SALE", "SERVICE": return "Received"
        case "PURCHASE", "VENDOR": return "Pending / unpaid"
        case "SPEND": return "Business"
        case "ISSUE": return "Normal"
        case "STOCK": return "Needs check"
        case "ORDER": return restaurant ? "Preparing" : (distribution ? "In progress" : (trading ? "Ready for dispatch" : ""))
        case "SHIPMENT": return trading || distribution ? "Out for delivery" : ""
        case "APPOINTMENT": return service ? "Scheduled" : ""
        case "PERSON", "ROLE", "INVITE": return "Contributor"
        case "SUPPLIER", "PARTNER": return "Due on receipt"
        case "APPROVAL": return "Approve"
        default: return ""
        }
    }
    private var statusLine: String {
        switch mode {
        case "PURCHASE", "VENDOR", "SPEND", "BUDGET", "TARGET": return "Draft · Not yet submitted"
        case "APPROVAL": return "Decision draft · Still pending"
        default: return "Draft · Not yet saved"
        }
    }
    private var footerLine: String {
        switch mode {
        case "PURCHASE", "VENDOR", "SPEND": return "Submission is not approval or payment"
        case "COLLECTION": return marketing || production ? "Draft · No live transfer" : "Records the receipt. No live transfer."
        case "BUDGET", "TARGET": return marketing && mode == "TARGET" ? "Draft · Not yet submitted" : "Pending until approved. The current plan stays active."
        case "APPROVAL": return marketing ? "Decision only · No money execution · Audit preserved" : "Decision only · Audit is kept"
        case "SALE": return service || distribution ? "Draft · Sale entry granted · Not yet saved" : "Draft · Not yet saved"
        case "SUPPLIER", "PARTNER":
            if service { return "Ready to save. No purchase or expense is created." }
            if restaurant { return "Relationship only. Saving does not create a purchase or an expense." }
            if marketing && !client { return "Saving creates no spend event." }
            return trading || distribution || marketing || production ? "Saving creates no purchase or expense." : "A name is required"
        case "STOCK", "ORDER", "SHIPMENT":
            if production && job && mode == "ORDER" { return "Draft · Value / evidence optional · Not yet saved" }
            if production && (mode == "ORDER" || mode == "STOCK") { return "Draft · Operational signal, not an inventory ledger" }
            if restaurant && mode == "ORDER" { return "Draft · Operational note only" }
            if distribution && mode == "ORDER" { return "Draft · Value optional · Not yet saved" }
            if distribution && mode == "SHIPMENT" { return "Draft · Not yet saved" }
            return trading || (distribution && mode == "STOCK") ? "Draft · Operational signal, not an inventory ledger" : "Draft · Nothing saved yet"
        case "APPOINTMENT": return service ? "Draft · Assigned job scope · Not yet saved" : "Draft · Nothing saved yet"
        case "UPDATE": return production ? "Draft · Production activity only · Nothing saved yet" : (marketing ? "Draft · Campaign activity only · Nothing saved yet" : (service ? "Draft · Service activity only · Nothing saved yet" : (trading ? "Draft · Trade activity only · Nothing saved yet" : (distribution ? "Draft · Distribution activity only · Nothing saved yet" : "Draft · Nothing saved yet"))))
        case "ISSUE": return production || marketing || service || trading || distribution ? "Draft · Opens an attention item only after save" : "Draft · Nothing saved yet"
        case "REIMBURSE": return service || trading || distribution ? "Records the reimbursement. No live transfer." : "Draft · Nothing saved yet"
        case "PERSON", "ROLE", "INVITE": return production || marketing || service || trading || distribution ? "No access until the invitation is accepted." : "Draft · Nothing saved yet"
        case "PAYMENT": return marketing ? "Draft · Manual confirmation only · No live transfer" : "Draft · Nothing saved yet"
        case "RESULT", "COMPLETE": return "Draft · Not yet saved"
        case "DELIVERABLE": return marketing ? "Assigned work only" : "Draft · Nothing saved yet"
        case "PRODUCTION": return "Draft · Not yet saved"
        case "DELIVERY":
            if production && job { return "Draft · No evidence attached · Not yet saved" }
            return production ? "Draft · Operational signal, not an inventory ledger" : "Draft · Nothing saved yet"
        default: return "Draft · Nothing saved yet"
        }
    }
    private var actionLabel: String {
        switch mode {
        case "SALE": return service && choice == "Received" ? "Mark Received & Save Sale" : "Save Sale"
        case "SERVICE": return "Save Service"
        case "PURCHASE", "VENDOR", "SPEND": return service ? "Submit for Approval" : "Submit for approval"
        case "PAYMENT": return marketing ? "Mark Paid" : "Record Payment"
        case "REIMBURSE": return "Mark Reimbursed"
        case "COLLECTION": return "Mark Received"
        case "BUDGET": return marketing ? "Submit plan revision" : "Submit revision"
        case "TARGET":
            if marketing && ["Extend launch outcome", "Change launch scope", "Correct planning error"].contains(choice) { return "Submit target revision" }
            return marketing ? "Submit revision" : "Submit target"
        case "UPDATE": return "Save update"
        case "ISSUE": return "Save issue"
        case "STOCK": return distribution ? "Save availability update" : "Save stock update"
        case "PRODUCTION": return "Save production"
        case "RESULT": return marketing ? "Save Result" : "Save result"
        case "PERSON", "ROLE", "INVITE": return "Save person"
        case "SUPPLIER", "PARTNER":
            if distribution && mode == "SUPPLIER" { return "Save Principal / Supplier" }
            if (restaurant || service || trading) && mode == "SUPPLIER" { return "Save Supplier" }
            if marketing && mode == "PARTNER" { return client ? "Save partner" : "Save Partners & Channels" }
            if production && mode == "PARTNER" { return "Save Supplier" }
            return "Save"
        case "APPROVAL": return choice == "Reject" ? "Reject" : (choice == "Return" ? "Return" : "Approve")
        case "ORDER": return production ? "Save order" : (restaurant ? "Save order update" : (trading || distribution ? "Save dispatch update" : "Save order"))
        case "SHIPMENT": return distribution ? "Save delivery update" : (trading ? "Save Delivery Update" : "Save shipment")
        case "DELIVERY": return "Save delivery"
        case "APPOINTMENT": return service ? "Save job update" : "Save appointment"
        case "DELIVERABLE": return marketing ? "Add Deliverable" : "Save deliverable"
        case "COMPLETE": return marketing ? "Mark Complete" : "Mark complete"
        default: return "Save"
        }
    }
    private var textLabel: String {
        switch mode {
        case "PRODUCTION": return "Production note"
        case "RESULT": return "Result note"
        case "ORDER": return "Order"
        case "SHIPMENT": return "Shipment"
        case "DELIVERY": return "Delivery"
        case "APPOINTMENT": return "Appointment"
        case "DELIVERABLE": return "Deliverable"
        case "COMPLETE": return "What was completed"
        default: return "Update"
        }
    }
    private var canSave: Bool {
        let named = !party.trimmingCharacters(in: .whitespaces).isEmpty
        let described = !purpose.trimmingCharacters(in: .whitespaces).isEmpty
        let noted = !note.trimmingCharacters(in: .whitespaces).isEmpty
        if mode == "SUPPLIER" || mode == "PARTNER" { return named }
        if contractFamily && contractModes.contains(mode) {
            switch mode {
            case "SALE", "SERVICE":
                let amountOk = entryMode == "Itemized"
                    ? (!itemName.trimmingCharacters(in: .whitespaces).isEmpty || !savedLines.isEmpty)
                    : (Decimal(string: amount) ?? 0) > 0
                let buyerOk = saleSettled || named
                let knownChannel = !channels.isEmpty || (model.masters ?? []).contains { $0.kind.uppercased() == "CHANNEL" && !$0.name.trimmingCharacters(in: .whitespaces).isEmpty }
                let channelOk = !channel.trimmingCharacters(in: .whitespaces).isEmpty || !knownChannel
                return amountOk && channelOk && buyerOk
            case "PURCHASE":
                return named && (!itemName.trimmingCharacters(in: .whitespaces).isEmpty || (Decimal(string: amount) ?? 0) > 0)
            case "SPEND":
                let categoryOk = !tradingFamily || !expenseCategory.trimmingCharacters(in: .whitespaces).isEmpty
                return (Decimal(string: amount) ?? 0) > 0 && categoryOk && (choice == "Paid" || named)
            case "STOCK":
                return described && !quantity.trimmingCharacters(in: .whitespaces).isEmpty
            case "UPDATE", "ISSUE":
                return described
            case "ORDER":
                return !channel.trimmingCharacters(in: .whitespaces).isEmpty || named
            case "SHIPMENT", "DELIVERY":
                return !parentId.isEmpty && !quantity.trimmingCharacters(in: .whitespaces).isEmpty
            case "RETURN":
                return !parentId.isEmpty && !returnReason.trimmingCharacters(in: .whitespaces).isEmpty && (Decimal(string: amount) ?? 0) > 0
            case "COLLECTION", "PAYMENT":
                let entered = Decimal(string: amount) ?? 0
                let cap = openObligations.first { $0.id == parentId }.flatMap { Decimal(string: $0.amount) }
                let within = cap == nil || entered <= cap!
                return entered > 0 && !parentId.isEmpty && within
            default:
                break
            }
        }
        if marketing && mode == "VENDOR" { return named }
        if client && mode == "COLLECTION" { return named && described }
        if own && mode == "COLLECTION" { return described }
        if client && mode == "TARGET" { return described && noted }
        if own && mode == "TARGET" { return described }
        if marketing && mode == "BUDGET" { return described }
        if marketing && mode == "DELIVERABLE" { return described && named }
        if marketing && mode == "COMPLETE" { return described && noted }
        if marketing && mode == "PAYMENT" { return named && described }
        if marketing && mode == "ISSUE" { return described && named }
        let detailed = !detail.trimmingCharacters(in: .whitespaces).isEmpty
        if production && mode == "PRODUCTION" { return (Decimal(string: amount) ?? 0) > 0 && detailed && (entryMode != "Detailed batch" || described) }
        if production && mode == "SALE" { return (Decimal(string: amount) ?? 0) > 0 && (choice == "Received" || named || !channel.trimmingCharacters(in: .whitespaces).isEmpty) }
        if production && mode == "PURCHASE" { return named && (Decimal(string: amount) ?? 0) > 0 }
        if production && mode == "ORDER" { return named && !quantity.trimmingCharacters(in: .whitespaces).isEmpty }
        if production && mode == "DELIVERY" { return !parentId.isEmpty && (entryMode != "Itemized" || !quantity.trimmingCharacters(in: .whitespaces).isEmpty) }
        if production && mode == "STOCK" { return described && !quantity.trimmingCharacters(in: .whitespaces).isEmpty }
        if production && mode == "BUDGET" { return described }
        if production && mode == "ISSUE" { return described }
        if production && mode == "UPDATE" { return described || noteKind == "Budget" || noteKind == "Target" }
        if tradingFamily && mode == "BUDGET" { return described && (Decimal(string: amount) ?? 0) > 0 }
        return true
    }

    private func close() {
        if let onDismiss {
            onDismiss()
        } else {
            dismiss()
        }
    }

    private func save() async {
        if mode == "APPROVAL" {
            guard let item = (model.attention ?? []).first(where: { $0.kind == "APPROVAL" }), let onDecide else {
                errorText = "Nothing is waiting."
                return
            }
            if (choice == "Reject" || choice == "Return"), note.trimmingCharacters(in: .whitespaces).isEmpty {
                errorText = "A reason is required."
                return
            }
            do {
                try await onDecide(item.id, choice == "Approve" ? "APPROVED" : "REJECTED")
                close()
            } catch {
                errorText = error.localizedDescription
            }
            return
        }
        guard let draft = buildDraft() else {
            errorText = requiredMessage
            return
        }
        do {
            try await onSubmit(draft)
            close()
        } catch {
            errorText = error.localizedDescription
        }
    }

    private var requiredMessage: String {
        switch mode {
        case "SALE", "SERVICE", "PURCHASE", "VENDOR", "SPEND", "PAYMENT", "REIMBURSE", "COLLECTION", "BUDGET", "TARGET", "RESULT", "PRODUCTION":
            return "Amount must be positive."
        case "PERSON", "ROLE", "INVITE", "SUPPLIER", "PARTNER":
            return "A name is required."
        default:
            return "A description is required."
        }
    }

    private var masterKinds: [String] {
        switch model.subtype {
        case "CONSUMER_SERVICE": return ["SERVICE", "STAFF", "CUSTOMER", "SUPPLIER", "CONSUMABLE", "CHANNEL"]
        case "CLIENT_CAMPAIGN", "OWN_BUSINESS_CAMPAIGN": return ["METRIC", "CHANNEL", "BUDGET_CATEGORY", "PARTY"]
        case "STANDARD_PRODUCTION", "MADE_TO_ORDER": return ["MATERIAL", "PRODUCT", "SUPPLIER", "BUYER", "STAFF"]
        case "TRADING_WHOLESALE", "DISTRIBUTION_DEALERSHIP":
            return ["PRODUCT", "SUPPLIER", "BUYER", "CHANNEL", "EXPENSE_CATEGORY", "UOM"]
        default: return ["PRODUCT", "SUPPLIER", "CUSTOMER", "CHANNEL", "STAFF"]
        }
    }

    private var openObligations: [OperatingPaymentPayload] {
        let want = mode == "COLLECTION" ? "RECEIVABLE" : "PAYABLE"
        return (model.paymentReadiness ?? []).filter { row in
            if mode == "COLLECTION" { return row.relevance == "RECEIVABLE" }
            return row.relevance == want || row.relevance == "REIMBURSEMENT" || row.relevance == "SETTLEMENT"
        }
    }

    @ViewBuilder private var lineEditor: some View {
        if tradingFamily {
            lovField("Product", value: itemName, kind: "PRODUCT", placeholder: "Search products")
            HStack(spacing: 8) {
                editField("Qty", text: $quantity, placeholder: "1")
                lovField("Unit", value: unit, kind: "UOM", placeholder: "pc")
                editField("Price", text: $unitPrice, placeholder: "0")
            }
        } else {
            let saved = model.masters ?? []
            if !saved.isEmpty {
                Text("Saved items").font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#C9C4D8"))
                ForEach(Array(saved.enumerated()), id: \.offset) { _, master in
                    Button(master.name) {
                        itemName = master.name
                        if let unitName = master.unit, !unitName.isEmpty { unit = unitName }
                    }
                    .font(.plusJakarta(size: 13))
                    .foregroundStyle(Color(hex: "#E5E0EE"))
                    .padding(.horizontal, 10)
                    .padding(.vertical, 6)
                    .overlay(RoundedRectangle(cornerRadius: 8).stroke(Color(hex: "#938EA1"), lineWidth: 1))
                }
            }
            editField("Item", text: $itemName, placeholder: "Item name")
            HStack(spacing: 8) {
                editField("Qty", text: $quantity, placeholder: "1")
                editField("Unit", text: $unit, placeholder: "piece")
                editField("Price", text: $unitPrice, placeholder: "0")
            }
        }
        Text("Quantity, unit, and price stay on the sale or purchase. Stock changes only when you record that the goods were received or dispatched.")
            .font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#C9C4D8"))
    }

    @ViewBuilder private var taxFxMore: some View {
        if tradingFamily && model.gstTracking != "YES" {
            choiceBlock("Tax mode", options: ["Not configured", "Inclusive", "Exclusive"], selected: taxMode) { taxMode = $0 }
            if taxMode != "Not configured" {
                editField("Tax amount", text: $taxAmount, placeholder: taxMode == "Inclusive" ? "Optional · display only" : "Optional")
            } else {
                Text("No tax amount is stored until tax mode is configured.")
                    .font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#C9C4D8"))
            }
        }
        if tradingFamily {
            editField("Original currency", text: $originalCurrency, placeholder: "Leave blank for moment currency")
            if !originalCurrency.trimmingCharacters(in: .whitespaces).isEmpty {
                editField("FX rate", text: $fxRate, placeholder: "Required to convert")
            }
        }
    }

    @ViewBuilder private func gstPriceFields(purchase: Bool) -> some View {
        if model.gstTracking == "YES" && (tradingFamily || retailFamily || ["STANDARD_PRODUCTION", "MADE_TO_ORDER"].contains(model.subtype ?? "")) {
            let included = "GST included in price"
            let added = "GST added separately"
            choiceBlock(purchase ? "Purchase price" : "Sale price", options: [included, added], selected: taxMode == "Inclusive" ? included : (taxMode == "Exclusive" ? added : "")) {
                taxMode = $0 == included ? "Inclusive" : "Exclusive"
            }
            editField("GST rate %", text: $gstRate, placeholder: "From the product, or type a rate")
            choiceBlock("Supply", options: ["Inside your state", "Outside your state"], selected: supplyChoice == "INTRA" ? "Inside your state" : (supplyChoice == "INTER" ? "Outside your state" : "")) {
                supplyChoice = $0.hasPrefix("Inside") ? "INTRA" : "INTER"
            }
            if supplyChoice.isEmpty {
                Text("Confirm whether this supply is inside your state or outside it.")
                    .font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#C9C4D8"))
            }
            let preview = gstPreview()
            let modeLabel = taxMode == "Inclusive" ? "Included in price" : "Added separately"
            Button("GST \(gstRate.isEmpty ? "—" : gstRate)% · \(modeLabel) · \(gstPreviewOpen ? "Hide" : "View")") { gstPreviewOpen.toggle() }
                .font(.plusJakarta(size: 14, weight: .semibold)).foregroundStyle(Color(hex: "#E8C547"))
            if purchase, let preview {
                Text("Supplier invoice total \(moneyLabel(preview.invoice))")
                    .font(.plusJakarta(size: 14, weight: .semibold)).foregroundStyle(Color(hex: "#E5E0EE"))
                Text("Amount paid stays separate from this invoice total.")
                    .font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#C9C4D8"))
            }
            if gstPreviewOpen, let preview {
                Text("Taxable \(moneyLabel(preview.taxable))").font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#C9C4D8"))
                if supplyChoice == "INTER" {
                    Text("IGST \(moneyLabel(preview.igst))").font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#C9C4D8"))
                } else if supplyChoice == "INTRA" {
                    Text("CGST \(moneyLabel(preview.cgst)) · SGST \(moneyLabel(preview.sgst))").font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#C9C4D8"))
                }
                Text("GST \(moneyLabel(preview.gst)) · Invoice \(moneyLabel(preview.invoice))").font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#C9C4D8"))
            } else if preview == nil {
                Text("A missing rate is not treated as 0%.")
                    .font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#C9C4D8"))
            }
        }
    }

    private func moneyLabel(_ value: Decimal) -> String {
        let number = NSDecimalNumber(decimal: value)
        return String(format: "₹%.2f", number.doubleValue)
    }

    private func gstPreview() -> (taxable: Decimal, cgst: Decimal, sgst: Decimal, igst: Decimal, gst: Decimal, invoice: Decimal)? {
        guard let rate = Decimal(string: gstRate), rate > 0 else { return nil }
        let qty = Decimal(string: quantity).flatMap { $0 > 0 ? $0 : nil } ?? 1
        let price = Decimal(string: unitPrice).flatMap { $0 > 0 ? $0 : nil }
        let entered = price.map { qty * $0 } ?? Decimal(string: amount)
        guard let entered, entered > 0 else { return nil }
        var taxable = Decimal()
        var gst = Decimal()
        var invoice = Decimal()
        if taxMode == "Inclusive" {
            invoice = entered.roundMoney()
            let divisor = Decimal(1) + (rate / 100)
            var value = invoice / divisor
            var rounded = Decimal()
            NSDecimalRound(&rounded, &value, 2, .plain)
            taxable = rounded
            gst = invoice - taxable
        } else {
            taxable = entered.roundMoney()
            var raw = taxable * rate / 100
            var rounded = Decimal()
            NSDecimalRound(&rounded, &raw, 2, .plain)
            gst = rounded
            invoice = taxable + gst
        }
        let cgst: Decimal
        let sgst: Decimal
        let igst: Decimal
        if supplyChoice == "INTRA" {
            var half = gst / 2
            var rounded = Decimal()
            NSDecimalRound(&rounded, &half, 2, .plain)
            cgst = rounded
            sgst = gst - cgst
            igst = 0
        } else if supplyChoice == "INTER" {
            cgst = 0
            sgst = 0
            igst = gst
        } else {
            cgst = 0
            sgst = 0
            igst = 0
        }
        return (taxable, cgst, sgst, igst, gst, invoice)
    }

    @ViewBuilder private var masterGstFields: some View {
        let product = ["PRODUCT", "MATERIAL", "CONSUMABLE"].contains(masterKind)
        let party = ["CUSTOMER", "BUYER", "SUPPLIER"].contains(masterKind)
        let gstFamily = tradingFamily || retailFamily || ["STANDARD_PRODUCTION", "MADE_TO_ORDER"].contains(model.subtype ?? "")
        if gstFamily && model.gstTracking == "YES" && (product || party) {
            if party {
                editField("Delivery location", text: $deliveryState, placeholder: "Where goods usually go")
            }
            Button(gstDetailsOpen ? "Hide GST details" : (product ? "Add GST Details (optional)" : "GST & Business Location")) { gstDetailsOpen.toggle() }
                .font(.plusJakarta(size: 14, weight: .semibold)).foregroundStyle(Color(hex: "#E8C547"))
            if gstDetailsOpen && product {
                Text("Set the product’s GST rate for automatic calculations.").font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#C9C4D8"))
                choiceBlock("GST applicable", options: ["Yes", "No", "Not sure"], selected: gstApplicable) { gstApplicable = $0 }
                choiceBlock("Classification", options: ["Taxable", "Exempt", "Nil-rated", "Non-GST"], selected: gstClassification) { gstClassification = $0 }
                editField("GST rate %", text: $gstRate, placeholder: "Your rate")
                choiceBlock("Selling prices", options: ["Use business default", "GST included in price", "GST added separately"], selected: masterSaleIncludes) { masterSaleIncludes = $0 }
                choiceBlock("Purchase prices", options: ["Use business default", "GST included in price", "GST added separately"], selected: masterPurchaseIncludes) { masterPurchaseIncludes = $0 }
                editField("Effective from", text: $gstEffectiveFrom, placeholder: "YYYY-MM-DD optional")
                editField("HSN Code (Optional)", text: $hsn, placeholder: "Add the product’s HSN code if you know it")
            }
            if gstDetailsOpen && party {
                Text("Save their tax details for future transactions.").font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#C9C4D8"))
                choiceBlock("GST registration", options: ["Registered", "Unregistered", "Not sure"], selected: partyGstRegistration) { partyGstRegistration = $0 }
                editField("Business state", text: $businessState, placeholder: "State")
                editField("Billing state", text: $billingState, placeholder: "State")
                editField("GSTIN (Optional)", text: $partyGstin, placeholder: "Add their GST number if you have it")
            }
        }
    }

    @ViewBuilder private var obligationPicker: some View {
        if tradingFamily {
            let label = mode == "COLLECTION" ? "Unpaid sale" : "Unpaid purchase or expense"
            let selected = openObligations.first { $0.id == parentId }?.title ?? ""
            lovField(label, value: selected, kind: "OBLIGATION", placeholder: openObligations.isEmpty ? "Nothing is open" : "Search open records")
        } else if openObligations.isEmpty {
            Text(marketing && mode == "PAYMENT" ? "Record a spend before paying a vendor." : marketing && mode == "COLLECTION" ? "Record an invoice before collecting." : mode == "COLLECTION" ? "No unpaid sale is open." : "No unpaid purchase or expense is open.")
                .font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#C9C4D8"))
        } else {
            Text(marketing && mode == "COLLECTION" ? "Open client invoices" : marketing && mode == "PAYMENT" ? "Open payables" : mode == "COLLECTION" ? "Unpaid sales" : "Unpaid purchases and expenses")
                .font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#C9C4D8"))
            ForEach(Array(openObligations.enumerated()), id: \.offset) { _, row in
                Button {
                    parentId = row.id
                    if amount.trimmingCharacters(in: .whitespaces).isEmpty, !row.amount.isEmpty { amount = row.amount }
                    if let name = row.counterpartyName, !name.isEmpty { party = name }
                } label: {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(row.title).font(.plusJakarta(size: 14, weight: .semibold)).foregroundStyle(Color(hex: "#E5E0EE"))
                        Text([row.counterpartyName, row.amount, row.dueAt].compactMap { $0 }.filter { !$0.isEmpty }.joined(separator: " · "))
                            .font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#C9C4D8"))
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(10)
                    .background(parentId == row.id ? Color(hex: "#35333E") : Color.clear)
                    .overlay(RoundedRectangle(cornerRadius: 12).stroke(parentId == row.id ? Color(hex: "#B7B8FF") : Color(hex: "#938EA1"), lineWidth: 1))
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                }
                .buttonStyle(.plain)
            }
        }
    }

    @ViewBuilder private var retailFields: some View {
        switch mode {
        case "SALE", "SERVICE":
            retailSaleFields
        case "PURCHASE":
            retailPurchaseFields
        case "SPEND":
            retailSpendFields
        case "COLLECTION", "PAYMENT":
            retailCollectionFields
        case "STOCK":
            retailStockFields
        case "ORDER":
            retailOrderFields
        case "SHIPMENT", "DELIVERY":
            retailShipmentFields
        case "RETURN":
            retailReturnFields
        case "APPOINTMENT":
            retailAppointmentFields
        case "UPDATE":
            retailUpdateFields
        case "ISSUE":
            retailIssueFields
        default:
            EmptyView()
        }
    }

    @ViewBuilder private var retailSaleFields: some View {
        if tradingFamily { lovField("Sales order", value: linkedOrderTitle, kind: "SALES_ORDER", placeholder: "None · optional") }
        choiceBlock("Entry", options: ["Quick amount", "Itemized"], selected: entryMode) { entryMode = $0 }
        if entryMode == "Itemized" {
            lineEditor
            Button("Add another item") {
                let name = itemName.trimmingCharacters(in: .whitespaces)
                if !name.isEmpty {
                    savedLines.append(RetailDraftLine(itemName: name, quantity: quantity, unit: unit, unitPrice: unitPrice, masterId: productMasterId))
                    itemName = ""; quantity = ""; unitPrice = ""; productMasterId = nil
                }
            }
            .font(.plusJakarta(size: 13, weight: .semibold)).foregroundStyle(Color(hex: "#B7B8FF"))
            ForEach(savedLines) { line in
                Text("\(line.itemName) · \(line.quantity) \(line.unit)").font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#C9C4D8"))
            }
        }
        if entryMode == "Quick amount" { amountBlock("Sale amount", hint: "Positive amount · 2 decimal places") }
        gstPriceFields(purchase: false)
        if tradingFamily {
            lovField(mode == "SERVICE" ? "Service channel" : "Sales channel", value: channel, kind: "CHANNEL", placeholder: "Required")
        } else if channels.isEmpty {
            editField(mode == "SERVICE" ? "Service channel" : "Sales channel", text: $channel, placeholder: "Required")
        } else {
            choiceBlock(mode == "SERVICE" ? "Service channel" : "Sales channel", options: channels, selected: channel) { channel = $0 }
        }
        dateField(mode == "SERVICE" ? "Charge date" : "Sale date")
        choiceBlock("Payment state", options: counterSaleWords ? ["Walk-in", "Part paid", "Named credit"] : ["Received", "Part paid", "Pay later"], selected: choice) { choice = $0 }
        if choice == "Part paid" { editField("Received amount", text: $receivedAmount, placeholder: "Less than the total") }
        if !saleSettled {
            if tradingFamily {
                lovField(distribution ? "Buyer / dealer" : "Buyer", value: party, kind: "BUYER", placeholder: "Required when a balance remains")
            } else {
                editField("Customer", text: $party, placeholder: counterSaleWords ? "Required for named credit" : "Required when a balance remains")
            }
        }
        if mode == "SERVICE" {
            let bookings = (model.activity ?? []).filter { $0.eventType == "APPOINTMENT" }
            if !bookings.isEmpty {
                Text("Link a booking").font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#C9C4D8"))
                ForEach(Array(bookings.prefix(6).enumerated()), id: \.offset) { _, row in
                    Button(row.title) { parentId = row.id }
                        .font(.plusJakarta(size: 13))
                        .foregroundStyle(parentId == row.id ? Color(hex: "#2F009C") : Color(hex: "#E5E0EE"))
                }
            }
        }
        Text(mode == "SERVICE" ? "A finalized charge is the sale. A booking is not revenue." : (tradingFamily ? "A quick amount does not change stock. Itemized sales do not deduct stock until dispatch." : "A quick amount does not change stock. Itemized lines do."))
            .font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#C9C4D8"))
        if tradingFamily {
            choiceBlock("Goods", options: ["Fully delivered", "Partially delivered", "Deliver later"], selected: goodsStatus) { goodsStatus = $0 }
            if goodsStatus == "Partially delivered" {
                editField("Delivered quantity", text: $quantity, placeholder: "Does not change stock")
            }
            if goodsStatus != "Fully delivered" { goodsDueField("Delivery due date") }
            Text("Goods status does not post stock. Dispatch stays a delivery.")
                .font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#C9C4D8"))
        }
        if !saleSettled { dueDateField(tradingFamily ? "Payment due date" : "Due date") }
        moreButton("Tax, FX, and note. Optional.")
        if moreOpen {
            taxFxMore
            editField("Note", text: $note, placeholder: "Optional")
        }
    }

    @ViewBuilder private var retailPurchaseFields: some View {
        if tradingFamily { lovField("Purchase order", value: linkedOrderTitle, kind: "PURCHASE_ORDER", placeholder: "None · optional") }
        lineEditor
        amountBlock("Amount", hint: "Positive amount, or leave blank when items have prices")
        gstPriceFields(purchase: true)
        if tradingFamily {
            lovField(distribution ? "Principal / supplier" : "Supplier", value: party, kind: "SUPPLIER", placeholder: "Required")
        } else {
            editField("Supplier", text: $party, placeholder: "Required")
        }
        dateField("Purchase date")
        if tradingFamily {
            choiceBlock("Goods receipt", options: ["Fully received", "Partially received", "Receive later"], selected: receiptState) { receiptState = $0 }
            if receiptState != "Fully received" { goodsDueField("Receipt due date") }
        } else {
            choiceBlock("Goods receipt", options: ["Received", "Part received", "Awaiting receipt"], selected: receiptState) { receiptState = $0 }
        }
        if receiptState == "Part received" || receiptState == "Partially received" {
            editField("Quantity received", text: $quantityReceived, placeholder: "Less than ordered qty")
        }
        if tradingFamily {
            choiceBlock("Ownership", options: ["Owned", "Consignment"], selected: ownership) { ownership = $0 }
            Text("Consignment is not owned stock.")
                .font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#C9C4D8"))
        }
        choiceBlock("Payment state", options: ["Paid", "Part paid", "Pay later"], selected: choice) { choice = $0 }
        if choice == "Part paid" { editField("Amount paid", text: $receivedAmount, placeholder: "Less than the total") }
        if choice != "Paid" { dueDateField(tradingFamily ? "Payment due date" : "Due date") }
        moreButton("Tax, FX, and note. Optional.")
        if moreOpen {
            taxFxMore
            editField("Note", text: $note, placeholder: "Optional")
        }
    }

    @ViewBuilder private var retailSpendFields: some View {
        if tradingFamily {
            lovField("Category", value: expenseCategory, kind: "EXPENSE_CATEGORY", placeholder: "Required")
        } else {
            choiceBlock("Category", options: expenseCategories, selected: expenseCategory) { expenseCategory = $0 }
        }
        amountBlock("Amount", hint: "Positive amount")
        dateField("Expense date")
        choiceBlock("Payment state", options: ["Paid", "Part paid", "Pay later"], selected: choice) { choice = $0 }
        if choice != "Paid" {
            editField("Payee", text: $party, placeholder: "Required")
            dueDateField("Due date")
        }
        if choice == "Part paid" { editField("Amount paid", text: $receivedAmount, placeholder: "Less than the total") }
        moreButton("Note. Optional.")
        if moreOpen { editField("Note", text: $note, placeholder: "Optional") }
    }

    @ViewBuilder private var retailCollectionFields: some View {
        obligationPicker
        amountBlock(mode == "COLLECTION" ? "Collection amount" : "Payment amount", hint: "Cannot exceed the open balance")
        editField(mode == "COLLECTION" ? "Customer or platform" : "Supplier or payee", text: $party, placeholder: "Name")
        dateField(mode == "COLLECTION" ? "Collection date" : "Payment date")
        Text(mode == "COLLECTION" ? "This reduces what is owed. It does not increase sales." : "This reduces what you owe. It does not repeat the cost.")
            .font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#C9C4D8"))
        moreButton("Reference. Optional.")
        if moreOpen { editField("Reference", text: $note, placeholder: "Optional") }
    }

    @ViewBuilder private var retailStockFields: some View {
        if tradingFamily {
            lovField("Item", value: purpose, kind: "PRODUCT", placeholder: "Product or supply")
        } else {
            editField("Item", text: $purpose, placeholder: "Product, ingredient, or supply")
        }
        choiceBlock("Action", options: ["Set current stock", "Add", "Reduce"], selected: stockAction) { stockAction = $0 }
        editField("Quantity", text: $quantity, placeholder: "Required")
        if tradingFamily {
            lovField("Unit", value: unit, kind: "UOM", placeholder: "pc")
        } else {
            editField("Unit", text: $unit, placeholder: "pcs, kg, L")
        }
        choiceBlock("Reason", options: ["Opening count", "Stocktake", "Damage", "Waste", "Return", "Correction"], selected: stockReason) { stockReason = $0 }
        if tradingFamily {
            choiceBlock("Ownership", options: ["Owned", "Consignment"], selected: ownership) { ownership = $0 }
        }
        editField("Reorder point", text: $detail, placeholder: "Optional")
        Text("Set replaces the count. Add and reduce move it. This is not a purchase.")
            .font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#C9C4D8"))
    }

    @ViewBuilder private var retailOrderFields: some View {
        if tradingFamily {
            choiceBlock("Order", options: ["Sales order", "Purchase order"], selected: orderSide) { orderSide = $0 }
            lineEditor
            lovField(orderSide == "Purchase order" ? (distribution ? "Principal / supplier" : "Supplier") : (distribution ? "Buyer / dealer" : "Buyer"), value: party, kind: orderSide == "Purchase order" ? "SUPPLIER" : "BUYER", placeholder: "Required")
            lovField("Channel", value: channel, kind: "CHANNEL", placeholder: "Optional if the party is set")
            choiceBlock("Status", options: ["Placed", "Preparing", "Completed", "Cancelled"], selected: orderStatus) { orderStatus = $0 }
            editField("Estimated amount", text: $amount, placeholder: "Commitment only")
            editField("Promised date", text: $link, placeholder: "YYYY-MM-DD")
            Text("An order is not a sale or a purchase until you bill it. Ordered, executed, and remaining stay on the order.")
                .font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#C9C4D8"))
        } else {
        if channels.isEmpty {
            editField("Order channel", text: $channel, placeholder: "Required")
        } else {
            choiceBlock("Order channel", options: channels, selected: channel) { channel = $0 }
        }
        choiceBlock("Status", options: counterSaleWords && (restaurant || model.subtype == "RESTAURANT_FOOD") ? ["Accepted", "Preparing", "Ready", "Fulfilled", "Cancelled"] : ["Placed", "Preparing", "Completed", "Cancelled"], selected: orderStatus) { orderStatus = $0 }
        let openOrders = (model.activity ?? []).filter { $0.eventType == "ORDER" }
        if !openOrders.isEmpty {
            Text("Update an existing order").font(.plusJakarta(size: 12, weight: .semibold)).foregroundStyle(Color(hex: "#C9C4D8"))
            ForEach(Array(openOrders.enumerated()), id: \.offset) { _, row in
                Button(parentId == row.id ? "Updating \(row.title)" : row.title) { parentId = row.id }
                    .font(.plusJakarta(size: 13, weight: .semibold))
                    .foregroundStyle(Color(hex: "#F4F1FA"))
            }
        }
        editField("Guest or customer", text: $party, placeholder: "Optional")
        editField("Estimated amount", text: $amount, placeholder: "Pipeline only")
        dateField("Order time")
        Text("An order is not a sale until you bill it.")
            .font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#C9C4D8"))
        moreButton("Note. Optional.")
        if moreOpen { editField("Note", text: $note, placeholder: "Optional") }
        }
    }

    @ViewBuilder private var retailShipmentFields: some View {
        if tradingFamily {
            choiceBlock("Movement", options: ["Outgoing sale", "Incoming purchase"], selected: movement) {
                movement = $0
                parentId = ""
            }
            let sourceType = movement == "Incoming purchase" ? "PURCHASE" : "SALE"
            let sources = (model.activity ?? []).filter { $0.eventType == sourceType }
            if sources.isEmpty {
                Text(sourceType == "SALE" ? "No posted sale to deliver." : "No posted purchase to receive.")
                    .font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#C9C4D8"))
            } else {
                Text("Source").font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#C9C4D8"))
                ForEach(Array(sources.prefix(8).enumerated()), id: \.offset) { _, row in
                    Button {
                        parentId = row.id
                        purpose = row.title
                        if let name = row.counterpartyName, !name.isEmpty { party = name }
                        if let qty = row.quantity, !qty.isEmpty { quantity = qty }
                        if let unitName = row.unit, !unitName.isEmpty { unit = unitName }
                    } label: {
                        Text(parentId == row.id ? "Using \(row.title)" : row.title)
                            .font(.plusJakarta(size: 13, weight: .semibold))
                            .foregroundStyle(Color(hex: "#E5E0EE"))
                    }
                    .buttonStyle(.plain)
                }
            }
            editField("Quantity", text: $quantity, placeholder: "Required")
            lovField("Unit", value: unit, kind: "UOM", placeholder: "pc")
            choiceBlock("Status", options: ["Placed", "In transit", "Delivered", "Cancelled"], selected: choice) { choice = $0 }
            Text("Delivery moves goods against the source. It does not create another sale or purchase.")
                .font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#C9C4D8"))
        } else {
        editField(mode == "DELIVERY" ? "Delivery reference" : "Shipment reference", text: $purpose, placeholder: "Required")
        editField("Quantity", text: $quantity, placeholder: "Required")
        choiceBlock("Status", options: ["Placed", "In transit", "Delivered", "Cancelled"], selected: choice) {
            choice = $0
        }
        editField("Buyer or dealer", text: $party, placeholder: "Optional")
        editField("Linked order id", text: $parentId, placeholder: "Optional")
        editField("Promised date", text: $link, placeholder: "YYYY-MM-DD")
        moreButton("Note. Optional.")
        if moreOpen { editField("Note", text: $note, placeholder: "Optional") }
        Text("Shipment and delivery are fulfilment only. They are not sales.")
            .font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#C9C4D8"))
        }
    }

    @ViewBuilder private var retailReturnFields: some View {
        choiceBlock("Direction", options: ["Customer", "Supplier"], selected: returnDirection) { returnDirection = $0 }
        let sources = (model.activity ?? []).filter {
            ($0.eventType == "SALE" && returnDirection == "Customer") || ($0.eventType == "PURCHASE" && returnDirection == "Supplier")
        }
        if sources.isEmpty {
            Text(returnDirection == "Customer" ? "No open sale to credit." : "No open purchase to credit.")
                .font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#C9C4D8"))
        } else {
            Text("Source").font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#C9C4D8"))
            ForEach(Array(sources.prefix(8).enumerated()), id: \.offset) { _, row in
                Button {
                    parentId = row.id
                    if let name = row.counterpartyName, !name.isEmpty { party = name }
                } label: {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(row.title).font(.plusJakarta(size: 14, weight: .semibold)).foregroundStyle(Color(hex: "#E5E0EE"))
                        Text([row.counterpartyName, row.amount].compactMap { $0 }.joined(separator: " · "))
                            .font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#C9C4D8"))
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(10)
                    .background(parentId == row.id ? Color(hex: "#35333E") : Color.clear)
                    .overlay(RoundedRectangle(cornerRadius: 12).stroke(parentId == row.id ? lovAccent : Color(hex: "#938EA1"), lineWidth: 1))
                }
                .buttonStyle(.plain)
            }
        }
        lineEditor
        amountBlock("Credit amount", hint: "Cannot exceed the source amount")
        choiceBlock("Goods outcome", options: ["To stock", "Not returned", "Pending inspection"], selected: goodsOutcome) { goodsOutcome = $0 }
        editField("Reason", text: $returnReason, placeholder: "Required")
        Text("Only “To stock” increases owned on-hand. Returns do not invent revenue or cost.")
            .font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#C9C4D8"))
    }

    @ViewBuilder private var retailAppointmentFields: some View {
        editField("Service", text: $purpose, placeholder: "Required")
        dateField("Appointment time")
        editField("Guest or customer", text: $party, placeholder: "Optional for a walk-in")
        editField("Assigned person", text: $detail, placeholder: "Optional")
        let openBookings = (model.activity ?? []).filter { $0.eventType == "APPOINTMENT" }
        if !openBookings.isEmpty {
            Text("Update an existing booking").font(.plusJakarta(size: 12, weight: .semibold)).foregroundStyle(Color(hex: "#C9C4D8"))
            ForEach(Array(openBookings.enumerated()), id: \.offset) { _, row in
                Button(parentId == row.id ? "Updating \(row.title)" : row.title) { parentId = row.id }
                    .font(.plusJakarta(size: 13, weight: .semibold))
                    .foregroundStyle(Color(hex: "#F4F1FA"))
            }
        }
        if seed != nil || !parentId.isEmpty {
            choiceBlock("Action", options: ["Reschedule", "Cancel", "No-show", "Start", "Complete"], selected: bookingAction) { bookingAction = $0 }
        }
        Text("Booking, starting, and completing do not post revenue. Finalize a charge separately.")
            .font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#C9C4D8"))
        moreButton("Reason. Optional.")
        if moreOpen { editField("Reason", text: $note, placeholder: "Optional") }
    }

    @ViewBuilder private var retailUpdateFields: some View {
        editField("Update", text: $purpose, placeholder: "What happened")
        dateField("Date")
        Text("Saved as activity. Money totals stay unchanged.")
            .font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#C9C4D8"))
    }

    @ViewBuilder private var retailIssueFields: some View {
        editField("Issue", text: $purpose, placeholder: "What needs attention")
        choiceBlock("Priority", options: ["Low", "Normal", "High"], selected: choice) { choice = $0 }
        editField("Owner", text: $party, placeholder: actorName)
        dateField("Due date")
        Text("Opens an attention item. Money totals stay unchanged.")
            .font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#C9C4D8"))
    }


    private func buildDraft() -> OperatingCaptureDraft? {
        if contractFamily && contractModes.contains(mode) { return retailDraft() }
        if mode == "MASTERS" {
            let name = itemName.trimmingCharacters(in: .whitespaces)
            if name.isEmpty { return nil }
            if tradingFamily && ["PRODUCT", "MATERIAL", "CONSUMABLE"].contains(masterKind) && OperatingSeedUoms.canonical(unit).isEmpty { return nil }
            return captured(eventType: "UPDATE", title: name, amount: nil, note: purpose, channelName: nil, counterpartyName: nil, payment: nil)
        }
        if production && ["PRODUCTION", "PURCHASE", "SPEND", "SALE", "ORDER", "DELIVERY", "PAYMENT", "COLLECTION", "STOCK", "BUDGET", "UPDATE", "ISSUE"].contains(mode) {
            return productionGovernedDraft()
        }
        let statusOnlyProgress = marketing && eventType == "PROGRESS" && amount.trimmingCharacters(in: .whitespaces).isEmpty
        let needsAmount = ["SALE", "SERVICE", "PURCHASE", "VENDOR", "SPEND", "PAYMENT", "REIMBURSE", "COLLECTION", "BUDGET", "TARGET", "RESULT", "PRODUCTION"].contains(mode) && !statusOnlyProgress
        let value = Decimal(string: amount) ?? 0
        if needsAmount, value <= 0 { return nil }
        let needsText = ["UPDATE", "ISSUE", "STOCK", "ORDER", "SHIPMENT", "DELIVERY", "APPOINTMENT", "DELIVERABLE", "COMPLETE"].contains(mode)
        if needsText, !(production && mode == "ORDER"), purpose.trimmingCharacters(in: .whitespaces).isEmpty, note.trimmingCharacters(in: .whitespaces).isEmpty { return nil }
        if trading, ["STOCK", "ORDER", "SHIPMENT"].contains(mode), purpose.trimmingCharacters(in: .whitespaces).isEmpty || note.trimmingCharacters(in: .whitespaces).isEmpty { return nil }
        if distribution, mode == "STOCK", purpose.trimmingCharacters(in: .whitespaces).isEmpty || note.trimmingCharacters(in: .whitespaces).isEmpty { return nil }
        if distribution, mode == "SALE", party.trimmingCharacters(in: .whitespaces).isEmpty { return nil }
        if distribution, mode == "ORDER", purpose.trimmingCharacters(in: .whitespaces).isEmpty || party.trimmingCharacters(in: .whitespaces).isEmpty { return nil }
        if distribution, mode == "SHIPMENT", purpose.trimmingCharacters(in: .whitespaces).isEmpty || note.trimmingCharacters(in: .whitespaces).isEmpty || party.trimmingCharacters(in: .whitespaces).isEmpty { return nil }
        if marketing, mode == "RESULT", eventType != "PROGRESS", purpose.trimmingCharacters(in: .whitespaces).isEmpty { return nil }
        if marketing, eventType == "PROGRESS", parentId.isEmpty { return nil }
        if marketing, mode == "VENDOR", party.trimmingCharacters(in: .whitespaces).isEmpty { return nil }
        if client, mode == "COLLECTION", party.trimmingCharacters(in: .whitespaces).isEmpty || purpose.trimmingCharacters(in: .whitespaces).isEmpty { return nil }
        if own, mode == "COLLECTION", purpose.trimmingCharacters(in: .whitespaces).isEmpty { return nil }
        if client, mode == "TARGET", purpose.trimmingCharacters(in: .whitespaces).isEmpty || note.trimmingCharacters(in: .whitespaces).isEmpty { return nil }
        if own, mode == "TARGET", purpose.trimmingCharacters(in: .whitespaces).isEmpty { return nil }
        if marketing, mode == "BUDGET", purpose.trimmingCharacters(in: .whitespaces).isEmpty { return nil }
        if marketing, mode == "DELIVERABLE", purpose.trimmingCharacters(in: .whitespaces).isEmpty || party.trimmingCharacters(in: .whitespaces).isEmpty { return nil }
        if marketing, mode == "COMPLETE", purpose.trimmingCharacters(in: .whitespaces).isEmpty || note.trimmingCharacters(in: .whitespaces).isEmpty { return nil }
        if marketing, mode == "PAYMENT", party.trimmingCharacters(in: .whitespaces).isEmpty || purpose.trimmingCharacters(in: .whitespaces).isEmpty { return nil }
        if marketing, mode == "ISSUE", purpose.trimmingCharacters(in: .whitespaces).isEmpty || party.trimmingCharacters(in: .whitespaces).isEmpty { return nil }
        if production, mode == "PRODUCTION", detail.trimmingCharacters(in: .whitespaces).isEmpty || purpose.trimmingCharacters(in: .whitespaces).isEmpty { return nil }
        if production, mode == "SALE", channel.trimmingCharacters(in: .whitespaces).isEmpty && party.trimmingCharacters(in: .whitespaces).isEmpty { return nil }
        if production, mode == "PURCHASE", party.trimmingCharacters(in: .whitespaces).isEmpty { return nil }
        if production, mode == "ORDER", party.trimmingCharacters(in: .whitespaces).isEmpty || value <= 0 { return nil }
        if production, mode == "DELIVERY", purpose.trimmingCharacters(in: .whitespaces).isEmpty || note.trimmingCharacters(in: .whitespaces).isEmpty || (job && party.trimmingCharacters(in: .whitespaces).isEmpty) { return nil }
        if production, mode == "DELIVERY", !amount.trimmingCharacters(in: .whitespaces).isEmpty, value <= 0 { return nil }
        if production, mode == "STOCK", purpose.trimmingCharacters(in: .whitespaces).isEmpty || note.trimmingCharacters(in: .whitespaces).isEmpty { return nil }
        if production, mode == "BUDGET", purpose.trimmingCharacters(in: .whitespaces).isEmpty { return nil }
        if production, mode == "ISSUE", purpose.trimmingCharacters(in: .whitespaces).isEmpty || party.trimmingCharacters(in: .whitespaces).isEmpty { return nil }
        if ["PERSON", "ROLE", "INVITE", "SUPPLIER", "PARTNER"].contains(mode), party.trimmingCharacters(in: .whitespaces).isEmpty { return nil }
        if ["PAYMENT", "REIMBURSE", "COLLECTION"].contains(mode), !openObligations.isEmpty, parentId.isEmpty, !(client && mode == "COLLECTION" && choice == "Create Receivable") { return nil }
        let savedTitle: String = {
            switch mode {
            case "PERSON", "ROLE", "INVITE", "SUPPLIER", "PARTNER": return party.trimmingCharacters(in: .whitespaces)
            case "UPDATE", "ISSUE", "ORDER", "SHIPMENT", "DELIVERY", "APPOINTMENT", "DELIVERABLE", "COMPLETE":
                if production && mode == "ORDER" { return party.trimmingCharacters(in: .whitespaces) }
                return String(purpose.trimmingCharacters(in: .whitespaces).prefix(80))
            case "STOCK": return purpose.trimmingCharacters(in: .whitespaces).isEmpty ? title : purpose.trimmingCharacters(in: .whitespaces)
            case "PURCHASE", "VENDOR", "SPEND": return purpose.trimmingCharacters(in: .whitespaces).isEmpty ? title : purpose.trimmingCharacters(in: .whitespaces)
            default: return title
            }
        }()
        let payment: String? = {
            switch mode {
            case "SALE", "SERVICE":
                if production { return choice == "Paid" ? "COLLECTED" : "PENDING" }
                return choice == "Received" ? "COLLECTED" : "PENDING"
            case "PURCHASE", "VENDOR": return choice == "Paid" ? "PAID" : "PENDING"
            case "SPEND": return marketing ? (choice == "Paid" ? "PAID" : "PENDING") : nil
            case "PAYMENT", "REIMBURSE": return "PAID"
            case "COLLECTION": return client && choice == "Create Receivable" ? "PENDING" : "COLLECTED"
            default: return nil
            }
        }()
        if marketing {
            let parts: [String]?
            switch mode {
            case "RESULT": parts = [purpose, choice, note]
            case "COLLECTION": parts = [choice, purpose, note]
            case "TARGET": parts = client ? [purpose, choice, note] : [choice, purpose]
            case "DELIVERABLE": parts = [choice, detail, note]
            case "COMPLETE": parts = [choice, note]
            case "PAYMENT": parts = [purpose, detail]
            case "UPDATE": parts = [note]
            case "SPEND", "VENDOR": parts = [note, choice]
            case "BUDGET": parts = [purpose]
            case "PARTNER": parts = client ? [purpose, choice] : [purpose]
            default: parts = nil
            }
            if let parts {
                let saved = parts.map { $0.trimmingCharacters(in: .whitespaces) }.filter { !$0.isEmpty }.joined(separator: " · ")
                return captured(
                    eventType: eventType,
                    title: savedTitle.isEmpty ? title : savedTitle,
                    amount: amount.isEmpty ? nil : amount,
                    note: saved.isEmpty ? nil : saved,
                    channelName: channel.isEmpty ? nil : channel,
                    counterpartyName: party.trimmingCharacters(in: .whitespaces).isEmpty ? nil : party.trimmingCharacters(in: .whitespaces),
                    payment: payment
                )
            }
        }
        if production {
            let parts: [String]?
            switch mode {
            case "PRODUCTION": parts = [detail, purpose, link, party, note]
            case "SALE": parts = [detail, note]
            case "SPEND": parts = [note, choice, detail]
            case "ORDER": parts = [choice, purpose, note]
            case "DELIVERY": parts = [choice, note, job ? party : "", link]
            case "STOCK": parts = [choice, note]
            case "BUDGET": parts = [choice, purpose]
            case "PARTNER": parts = [purpose, note]
            case "UPDATE": parts = [note]
            default: parts = nil
            }
            if let parts {
                let saved = parts.map { $0.trimmingCharacters(in: .whitespaces) }.filter { !$0.isEmpty }.joined(separator: " · ")
                return captured(
                    eventType: mode == "BUDGET" && choice == "Target" ? "TARGET" : eventType,
                    title: savedTitle.isEmpty ? title : savedTitle,
                    amount: amount.isEmpty ? nil : amount,
                    note: saved.isEmpty ? nil : saved,
                    channelName: channel.isEmpty ? nil : channel,
                    counterpartyName: mode == "PRODUCTION" || party.trimmingCharacters(in: .whitespaces).isEmpty ? nil : party.trimmingCharacters(in: .whitespaces),
                    payment: payment
                )
            }
        }
        let noteParts = [purpose, note, distribution && mode == "SHIPMENT" ? link : "", ["ISSUE", "STOCK", "ORDER", "SHIPMENT", "APPOINTMENT", "SPEND", "PERSON", "ROLE", "INVITE", "SUPPLIER", "PARTNER"].contains(mode) ? choice : ""]
            .map { $0.trimmingCharacters(in: .whitespaces) }
            .filter { !$0.isEmpty }
        let skipPurpose = ["PURCHASE", "VENDOR", "SPEND", "UPDATE", "ISSUE"].contains(mode)
        let savedNote = (skipPurpose ? noteParts.filter { $0 != purpose.trimmingCharacters(in: .whitespaces) } : noteParts).joined(separator: " · ")
        return captured(
            eventType: eventType,
            title: savedTitle.isEmpty ? title : savedTitle,
            amount: amount.isEmpty ? nil : amount,
            note: savedNote.isEmpty ? nil : savedNote,
            channelName: channel.isEmpty ? nil : channel,
            counterpartyName: party.trimmingCharacters(in: .whitespaces).isEmpty ? nil : party.trimmingCharacters(in: .whitespaces),
            payment: payment
        )
    }

    private func retailDraft() -> OperatingCaptureDraft? {
        let rawAmount = amount.trimmingCharacters(in: .whitespaces)
        let lines = retailLines()
        switch mode {
        case "SALE", "SERVICE":
            let knownChannel = !channels.isEmpty || (model.masters ?? []).contains { $0.kind.uppercased() == "CHANNEL" && !$0.name.trimmingCharacters(in: .whitespaces).isEmpty }
            if knownChannel && channel.trimmingCharacters(in: .whitespaces).isEmpty { return nil }
            if entryMode == "Quick amount", (Decimal(string: rawAmount) ?? 0) <= 0 { return nil }
            if entryMode == "Itemized", lines.isEmpty { return nil }
            if choice == "Part paid", (Decimal(string: receivedAmount) ?? 0) <= 0 { return nil }
            if !saleSettled, party.trimmingCharacters(in: .whitespaces).isEmpty { return nil }
        case "PURCHASE":
            if party.trimmingCharacters(in: .whitespaces).isEmpty { return nil }
            if lines.isEmpty, (Decimal(string: rawAmount) ?? 0) <= 0 { return nil }
            if receiptState == "Part received" || receiptState == "Partially received", quantityReceived.trimmingCharacters(in: .whitespaces).isEmpty { return nil }
        case "SPEND":
            if (Decimal(string: rawAmount) ?? 0) <= 0 { return nil }
            if choice != "Paid", party.trimmingCharacters(in: .whitespaces).isEmpty { return nil }
        case "COLLECTION", "PAYMENT":
            if (Decimal(string: rawAmount) ?? 0) <= 0 || parentId.isEmpty { return nil }
        case "STOCK":
            if purpose.trimmingCharacters(in: .whitespaces).isEmpty || quantity.trimmingCharacters(in: .whitespaces).isEmpty { return nil }
        case "ORDER":
            if tradingFamily {
                if channel.trimmingCharacters(in: .whitespaces).isEmpty && party.trimmingCharacters(in: .whitespaces).isEmpty { return nil }
            } else if channel.trimmingCharacters(in: .whitespaces).isEmpty {
                return nil
            }
        case "SHIPMENT", "DELIVERY":
            if purpose.trimmingCharacters(in: .whitespaces).isEmpty || quantity.trimmingCharacters(in: .whitespaces).isEmpty { return nil }
        case "RETURN":
            if parentId.isEmpty || returnReason.trimmingCharacters(in: .whitespaces).isEmpty { return nil }
            if (Decimal(string: rawAmount) ?? 0) <= 0 { return nil }
            if goodsOutcome == "To stock", lines.isEmpty, quantity.trimmingCharacters(in: .whitespaces).isEmpty { return nil }
        case "APPOINTMENT":
            if purpose.trimmingCharacters(in: .whitespaces).isEmpty { return nil }
        case "UPDATE", "ISSUE":
            let text = purpose.trimmingCharacters(in: .whitespaces)
            if text.isEmpty { return nil }
            let priority = mode == "ISSUE" ? choice.trimmingCharacters(in: .whitespaces) : ""
            let detail = note.trimmingCharacters(in: .whitespaces)
            let combined = [priority.isEmpty ? nil : priority, detail.isEmpty ? nil : detail].compactMap { $0 }.joined(separator: " · ")
            let owner = party.trimmingCharacters(in: .whitespaces)
            return OperatingCaptureDraft(
                eventType: mode,
                title: text,
                amount: nil,
                note: combined.isEmpty ? nil : combined,
                channelName: nil,
                counterpartyName: mode == "ISSUE" && !owner.isEmpty ? owner : nil,
                paymentStatus: "NONE",
                occurredAt: occurredAtValue,
                dueAt: mode == "ISSUE" ? isoDay(eventDate) : nil
            )
        default:
            return nil
        }
        let paymentState: String? = {
            switch choice {
            case "Received", "Walk-in": return "RECEIVED"
            case "Part paid": return "PART_PAID"
            case "Pay later", "Named credit": return "PAY_LATER"
            case "Paid": return "PAID"
            default: return nil
            }
        }()
        let receipt: String? = {
            switch receiptState {
            case "Received", "Fully received": return "RECEIVED"
            case "Part received", "Partially received": return "PART_RECEIVED"
            case "Awaiting receipt", "Receive later": return "AWAITING"
            default: return nil
            }
        }()
        let stockCode: String? = stockAction == "Add" ? "ADD" : (stockAction == "Reduce" ? "REDUCE" : "SET")
        let reasonCode: String = {
            switch stockReason {
            case "Opening count": return "opening"
            case "Damage": return "damage"
            case "Waste": return "waste"
            case "Return": return "return"
            case "Correction": return "correction"
            default: return "stocktake"
            }
        }()
        let orderCode: String = {
            switch orderStatus {
            case "Preparing": return "PREPARING"
            case "Ready": return "READY"
            case "Completed", "Fulfilled": return "COMPLETED"
            case "Cancelled": return "CANCELLED"
            default: return "PLACED"
            }
        }()
        let actionCode: String? = {
            switch bookingAction {
            case "Reschedule": return "RESCHEDULE"
            case "Cancel": return "CANCEL"
            case "No-show": return "NO_SHOW"
            case "Start": return "START"
            case "Complete": return "COMPLETE"
            default: return nil
            }
        }()
        let titleText: String = {
            switch mode {
            case "STOCK", "APPOINTMENT", "SHIPMENT", "DELIVERY", "UPDATE", "ISSUE": return purpose.trimmingCharacters(in: .whitespaces)
            case "ORDER": return lines.first?.itemName ?? (party.trimmingCharacters(in: .whitespaces).isEmpty ? title : party.trimmingCharacters(in: .whitespaces))
            case "SALE", "SERVICE", "RETURN": return lines.first?.itemName ?? title
            default: return title
            }
        }()
        let shipmentCode: String? = {
            guard mode == "SHIPMENT" || mode == "DELIVERY" else { return nil }
            switch choice {
            case "Placed": return "PLACED"
            case "Delivered": return "DELIVERED"
            case "Cancelled": return "CANCELLED"
            default: return "IN_TRANSIT"
            }
        }()
        let ownershipCode: String? = {
            guard tradingFamily && (mode == "PURCHASE" || mode == "STOCK") else { return nil }
            return ownership == "Consignment" ? "CONSIGNMENT" : "OWNED"
        }()
        let taxModeCode: String? = {
            let gstFamily = tradingFamily || retailFamily || ["STANDARD_PRODUCTION", "MADE_TO_ORDER"].contains(model.subtype ?? "")
            guard gstFamily && ["SALE", "PURCHASE", "SERVICE", "SPEND"].contains(mode) else { return nil }
            switch taxMode {
            case "Inclusive": return "INCLUSIVE"
            case "Exclusive": return "EXCLUSIVE"
            default: return "NOT_CONFIGURED"
            }
        }()
        let goodsOutcomeCode: String? = {
            guard mode == "RETURN" else { return nil }
            switch goodsOutcome {
            case "Not returned": return "NOT_RETURNED"
            case "Pending inspection": return "PENDING_INSPECTION"
            default: return "TO_STOCK"
            }
        }()
        let goodsOpen = tradingFamily && (
            ((mode == "SALE" || mode == "SERVICE") && goodsStatus != "Fully delivered") ||
            (mode == "PURCHASE" && receiptState != "Fully received" && receiptState != "Received")
        )
        let goodsDeadline: String? = goodsOpen ? isoDay(goodsDueDate) : nil
        let promised = link.trimmingCharacters(in: .whitespaces)
        let fxCurrency = originalCurrency.trimmingCharacters(in: .whitespacesAndNewlines).uppercased()
        let allocation = mode == "ORDER" || mode == "APPOINTMENT" || parentId.isEmpty || rawAmount.isEmpty ? nil : [OperatingAllocationInput(sourceId: parentId, amount: rawAmount)]
        let contract = OperatingContractInput(
            entryMode: mode == "SALE" || mode == "SERVICE" ? (entryMode == "Itemized" ? "ITEMIZED" : "QUICK") : nil,
            paymentState: ["SALE", "SERVICE", "PURCHASE", "SPEND"].contains(mode) ? paymentState : nil,
            receiptState: mode == "PURCHASE" ? receipt : nil,
            receivedAmount: receivedAmount.isEmpty ? nil : receivedAmount,
            quantityReceived: mode == "PURCHASE" && (receiptState == "Part received" || receiptState == "Partially received") ? quantityReceived : nil,
            stockAction: mode == "STOCK" ? stockCode : nil,
            stockReason: mode == "STOCK" ? reasonCode : nil,
            expenseCategory: mode == "SPEND" ? expenseCategory : nil,
            appointmentAction: mode == "APPOINTMENT" && (seed != nil || !parentId.isEmpty) ? actionCode : nil,
            orderStatus: mode == "ORDER" ? orderCode : nil,
            clientKey: clientKey,
            reorderPoint: mode == "STOCK" && !detail.trimmingCharacters(in: .whitespaces).isEmpty ? detail : nil,
            staffName: mode == "APPOINTMENT" && !detail.trimmingCharacters(in: .whitespaces).isEmpty ? detail : nil,
            estimatedAmount: mode == "ORDER" && !rawAmount.isEmpty ? rawAmount : nil,
            allocations: allocation,
            ownership: ownershipCode,
            shipmentStatus: shipmentCode,
            promisedAt: goodsDeadline ?? ((mode == "ORDER" || mode == "SHIPMENT" || mode == "DELIVERY") && !promised.isEmpty ? promised : nil),
            taxMode: taxModeCode,
            taxAmount: model.gstTracking == "YES" || taxModeCode == "NOT_CONFIGURED" || taxAmount.isEmpty ? nil : taxAmount,
            originalCurrency: fxCurrency.isEmpty ? nil : fxCurrency,
            fxRate: fxCurrency.isEmpty || fxRate.isEmpty ? nil : fxRate,
            orderSide: mode == "ORDER" && tradingFamily ? (orderSide == "Purchase order" ? "PURCHASE" : "SALES") : nil,
            goodsStatus: tradingFamily && (mode == "SALE" || mode == "SERVICE") ? (goodsStatus == "Fully delivered" ? "DELIVERED" : (goodsStatus == "Partially delivered" ? "PARTIAL" : "LATER")) : nil,
            returnDirection: mode == "RETURN" ? (returnDirection == "Supplier" ? "SUPPLIER" : "CUSTOMER") : nil,
            goodsOutcome: goodsOutcomeCode,
            returnReason: mode == "RETURN" ? returnReason.trimmingCharacters(in: .whitespaces) : nil,
            gstRate: model.gstTracking == "YES" && taxModeCode != nil ? (gstRate.isEmpty ? nil : gstRate) : nil,
            taxJurisdiction: model.gstTracking == "YES" && taxModeCode != nil && (supplyChoice == "INTRA" || supplyChoice == "INTER") ? supplyChoice : nil
        )
        return OperatingCaptureDraft(
            eventType: mode == "UPDATE" && noteKind == "Issue" ? "ISSUE" : (mode == "UPDATE" && noteKind == "Approval" ? "APPROVAL" : eventType),
            title: titleText.isEmpty ? title : titleText,
            amount: rawAmount.isEmpty ? nil : rawAmount,
            note: note.trimmingCharacters(in: .whitespaces).isEmpty ? nil : note,
            channelName: channel.trimmingCharacters(in: .whitespaces).isEmpty ? nil : channel,
            counterpartyName: party.trimmingCharacters(in: .whitespaces).isEmpty ? nil : party,
            paymentStatus: nil,
            occurredAt: mode == "APPOINTMENT" || mode == "ORDER" ? isoDay(eventDate) : occurredAtValue,
            quantity: quantity.trimmingCharacters(in: .whitespaces).isEmpty ? nil : quantity,
            unit: {
                let raw = unit.trimmingCharacters(in: .whitespaces)
                if raw.isEmpty { return nil }
                if tradingFamily { let seeded = OperatingSeedUoms.canonical(raw); return seeded.isEmpty ? nil : seeded }
                return raw
            }(),
            parentEventId: mode == "ORDER" || mode == "APPOINTMENT" ? nil : (parentId.isEmpty ? nil : parentId),
            revisesEventId: (mode == "ORDER" || mode == "APPOINTMENT") && !parentId.isEmpty ? parentId : seed?.id,
            dueAt: mode == "ISSUE" ? isoDay(eventDate) : retailDueAt,
            contract: contract,
            lineItems: lines.isEmpty ? nil : lines,
            masterGstApplicable: mode == "MASTERS" ? (gstApplicable == "Yes" ? "YES" : (gstApplicable == "No" ? "NO" : (gstApplicable == "Not sure" ? "NOT_SURE" : nil))) : nil,
            masterGstClassification: mode == "MASTERS" ? (gstClassification == "Taxable" ? "TAXABLE" : (gstClassification == "Exempt" ? "EXEMPT" : (gstClassification == "Nil-rated" ? "NIL" : (gstClassification == "Non-GST" ? "NON_GST" : nil)))) : nil,
            masterGstRate: mode == "MASTERS" && !gstRate.isEmpty ? gstRate : nil,
            masterPriceIncludesGst: mode == "MASTERS" ? priceModeCode(masterSaleIncludes) : nil,
            masterSalePriceIncludesGst: mode == "MASTERS" ? priceModeCode(masterSaleIncludes) : nil,
            masterPurchasePriceIncludesGst: mode == "MASTERS" ? priceModeCode(masterPurchaseIncludes) : nil,
            masterGstEffectiveFrom: mode == "MASTERS" && gstEffectiveFrom.range(of: #"^\d{4}-\d{2}-\d{2}$"#, options: .regularExpression) != nil ? gstEffectiveFrom : nil,
            masterHsn: mode == "MASTERS" && !hsn.trimmingCharacters(in: .whitespaces).isEmpty ? hsn : nil,
            masterGstRegistration: mode == "MASTERS" ? (partyGstRegistration == "Registered" ? "REGISTERED" : (partyGstRegistration == "Unregistered" ? "UNREGISTERED" : (partyGstRegistration == "Not sure" ? "NOT_SURE" : nil))) : nil,
            masterBusinessState: mode == "MASTERS" ? businessState : nil,
            masterBillingState: mode == "MASTERS" ? billingState : nil,
            masterDeliveryState: mode == "MASTERS" ? deliveryState : nil,
            masterGstin: mode == "MASTERS" ? partyGstin : nil
        )
    }

    private var retailDueAt: String? {
        let unpaidSale = (mode == "SALE" || mode == "SERVICE") && !saleSettled
        let unpaidBuy = mode == "PURCHASE" && choice != "Paid"
        let unpaidSpend = mode == "SPEND" && choice != "Paid"
        if unpaidSale || unpaidBuy || unpaidSpend { return isoDay(dueDate) }
        let manual = link.trimmingCharacters(in: .whitespaces)
        return manual.isEmpty ? nil : manual
    }

    private func retailLines() -> [(itemName: String, quantity: String?, unit: String?, unitPrice: String?, masterId: String?)] {
        var rows = savedLines.map { (itemName: $0.itemName, quantity: $0.quantity, unit: $0.unit, unitPrice: $0.unitPrice, masterId: $0.masterId) }
        let name = itemName.trimmingCharacters(in: .whitespaces)
        if !name.isEmpty { rows.append((itemName: name, quantity: quantity, unit: unit, unitPrice: unitPrice, masterId: productMasterId)) }
        return rows
    }

    private func isoDay(_ date: Date) -> String {
        var parts = Calendar.current.dateComponents([.year, .month, .day], from: date)
        parts.hour = 12
        parts.minute = 0
        parts.second = 0
        let start = Calendar.current.date(from: parts) ?? date
        let iso = ISO8601DateFormatter()
        iso.formatOptions = [.withInternetDateTime]
        return iso.string(from: start)
    }

    @ViewBuilder private func orderLink(_ label: String) -> some View {
        let orders = (model.activity ?? []).filter { $0.eventType == "CUSTOMER_ORDER" && $0.approvalStatus != "REJECTED" }
        if orders.isEmpty {
            Text("No open customer order on this moment yet.").font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#C9C4D8"))
        } else {
            Text(label).font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#C9C4D8"))
            ForEach(Array(orders.enumerated()), id: \.offset) { _, row in
                Button(row.title) { parentId = row.id }
                    .font(.plusJakarta(size: 14, weight: .semibold))
                    .foregroundStyle(parentId == row.id ? Color(hex: "#0E2A22") : chromeInk)
                    .padding(.vertical, 8)
                    .frame(maxWidth: .infinity)
                    .background(parentId == row.id ? Color(hex: "#66C7A4") : Color(hex: "#35333E"))
                    .clipShape(RoundedRectangle(cornerRadius: 10))
            }
        }
    }

    private var chromeInk: Color { Color(hex: "#E5E0EE") }

    private func productionGovernedDraft() -> OperatingCaptureDraft? {
        let buyer = party.trimmingCharacters(in: .whitespaces)
        let named = !buyer.isEmpty
        let output = amount.trimmingCharacters(in: .whitespaces)
        let qty = quantity.trimmingCharacters(in: .whitespaces)
        func paymentCode(_ label: String) -> String {
            switch label {
            case "Received", "Walk-in": return "RECEIVED"
            case "Paid": return "PAID"
            case "Part paid": return "PART_PAID"
            case "Named credit": return "PAY_LATER"
            default: return "PAY_LATER"
            }
        }
        func deliveryCode(_ label: String) -> String {
            switch label {
            case "Planned": return "PLANNED"
            case "Dispatched": return "DISPATCHED"
            case "Accepted": return "ACCEPTED"
            case "Returned": return "RETURNED"
            default: return "DELIVERED"
            }
        }
        func productionCode(_ label: String) -> String {
            switch label {
            case "Planned": return "PLANNED"
            case "In progress": return "IN_PROGRESS"
            default: return "COMPLETED"
            }
        }
        switch mode {
        case "PRODUCTION":
            if (Decimal(string: output) ?? 0) <= 0 || detail.trimmingCharacters(in: .whitespaces).isEmpty { return nil }
            let detailed = entryMode == "Detailed batch"
            if detailed && purpose.trimmingCharacters(in: .whitespaces).isEmpty { return nil }
            let contract = OperatingContractInput(
                entryMode: detailed ? "ITEMIZED" : "QUICK",
                acceptedQuantity: detailed && !qty.isEmpty ? qty : nil,
                productionStatus: detailed ? productionCode(choice) : nil,
                scrapQuantity: detailed && !scrapQuantity.trimmingCharacters(in: .whitespaces).isEmpty ? scrapQuantity : nil
            )
            return captured(
                eventType: "PRODUCTION",
                title: detailed ? purpose : (purpose.isEmpty ? title : purpose),
                amount: output,
                note: nil,
                channelName: nil,
                counterpartyName: nil,
                payment: nil,
                quantity: output,
                unit: detail,
                parentEventId: parentId.isEmpty ? nil : parentId,
                contract: contract,
                rejectedQuantity: detailed && !rejectedQuantity.trimmingCharacters(in: .whitespaces).isEmpty ? rejectedQuantity : nil
            )
        case "PURCHASE":
            if !named || (Decimal(string: output) ?? 0) <= 0 { return nil }
            let item = itemName.trimmingCharacters(in: .whitespaces)
            let lines: [(itemName: String, quantity: String?, unit: String?, unitPrice: String?, masterId: String?)]? =
                item.isEmpty ? nil : [(itemName: item, quantity: qty.isEmpty ? nil : qty, unit: unit, unitPrice: nil, masterId: nil)]
            let receipt = receiptState == "Part received" ? "PART_RECEIVED" : (receiptState == "Awaiting" ? "AWAITING" : "RECEIVED")
            let contract = OperatingContractInput(
                entryMode: item.isEmpty ? "QUICK" : "ITEMIZED",
                paymentState: paymentCode(choice),
                receiptState: receipt,
                receivedAmount: choice == "Part paid" ? receivedAmount : nil,
                quantityReceived: receipt == "PART_RECEIVED" ? quantityReceived : (receipt == "RECEIVED" ? qty : nil)
            )
            return captured(
                eventType: "PURCHASE",
                title: item.isEmpty ? (purpose.isEmpty ? title : purpose) : item,
                amount: output,
                note: nil,
                channelName: nil,
                counterpartyName: buyer,
                payment: nil,
                quantity: qty.isEmpty ? nil : qty,
                unit: unit,
                contract: contract,
                lineItems: lines
            )
        case "SPEND":
            if (Decimal(string: output) ?? 0) <= 0 { return nil }
            if choice != "Paid" && !named { return nil }
            return captured(
                eventType: "SPEND",
                title: purpose.isEmpty ? title : purpose,
                amount: output,
                note: nil,
                channelName: nil,
                counterpartyName: named ? buyer : nil,
                payment: nil,
                contract: OperatingContractInput(paymentState: paymentCode(choice))
            )
        case "SALE":
            if (Decimal(string: output) ?? 0) <= 0 { return nil }
            if choice != "Received" && !named && channel.trimmingCharacters(in: .whitespaces).isEmpty { return nil }
            return captured(
                eventType: "SALE",
                title: title,
                amount: output,
                note: nil,
                channelName: channel.isEmpty ? nil : channel,
                counterpartyName: named ? buyer : nil,
                payment: nil,
                parentEventId: parentId.isEmpty ? nil : parentId,
                contract: OperatingContractInput(
                    entryMode: "QUICK",
                    paymentState: paymentCode(choice),
                    receivedAmount: choice == "Part paid" ? receivedAmount : nil
                )
            )
        case "ORDER":
            if !named || qty.isEmpty { return nil }
            let quote: String? = (job || model.subtype == "MADE_TO_ORDER") ? {
                switch quotation {
                case "Sent": return "SENT"
                case "Rejected": return "REJECTED"
                case "Expired": return "EXPIRED"
                case "Draft": return "DRAFT"
                default: return "ACCEPTED"
                }
            }() : nil
            return captured(
                eventType: "CUSTOMER_ORDER",
                title: buyer,
                amount: output.isEmpty ? nil : output,
                note: nil,
                channelName: nil,
                counterpartyName: buyer,
                payment: "NONE",
                quantity: qty,
                unit: unit,
                contract: OperatingContractInput(
                    orderStatus: quote == nil || quote == "ACCEPTED" ? "PLACED" : nil,
                    estimatedAmount: output.isEmpty ? nil : output,
                    quotationStatus: quote
                )
            )
        case "DELIVERY":
            if parentId.isEmpty { return nil }
            let itemized = entryMode == "Itemized"
            if itemized && (Decimal(string: qty) ?? 0) <= 0 { return nil }
            return captured(
                eventType: "DELIVERY",
                title: "Delivery",
                amount: nil,
                note: nil,
                channelName: nil,
                counterpartyName: nil,
                payment: nil,
                quantity: itemized ? qty : nil,
                unit: itemized ? unit : nil,
                parentEventId: parentId,
                contract: OperatingContractInput(entryMode: itemized ? "ITEMIZED" : "QUICK", deliveryStatus: deliveryCode(choice))
            )
        case "COLLECTION":
            if (Decimal(string: output) ?? 0) <= 0 || !named { return nil }
            return captured(eventType: "COLLECTION", title: "Collection", amount: output, note: nil, channelName: nil, counterpartyName: buyer, payment: "COLLECTED", parentEventId: parentId.isEmpty ? nil : parentId)
        case "PAYMENT":
            if (Decimal(string: output) ?? 0) <= 0 || parentId.isEmpty { return nil }
            return captured(eventType: "PAYMENT", title: "Payment", amount: output, note: nil, channelName: nil, counterpartyName: named ? buyer : nil, payment: "PAID", parentEventId: parentId)
        case "STOCK":
            if purpose.trimmingCharacters(in: .whitespaces).isEmpty || (choice != "Set" && (Decimal(string: qty) ?? 0) <= 0) { return nil }
            let action = choice == "Reduce" ? "REDUCE" : (choice == "Add" ? "ADD" : "SET")
            return captured(
                eventType: "STOCK",
                title: purpose,
                amount: nil,
                note: nil,
                channelName: nil,
                counterpartyName: nil,
                payment: nil,
                quantity: qty.isEmpty ? "0" : qty,
                unit: unit,
                contract: OperatingContractInput(stockAction: action, stockReason: note.isEmpty ? "correction" : note, declaredQuantity: choice == "Set" ? qty : nil)
            )
        case "BUDGET":
            if (Decimal(string: output) ?? 0) <= 0 || purpose.trimmingCharacters(in: .whitespaces).isEmpty { return nil }
            return captured(eventType: choice == "Target" ? "TARGET" : "BUDGET", title: purpose, amount: output, note: purpose, channelName: nil, counterpartyName: nil, payment: nil)
        case "UPDATE", "ISSUE":
            if noteKind == "Budget" || noteKind == "Target" {
                if (Decimal(string: output) ?? 0) <= 0 || purpose.trimmingCharacters(in: .whitespaces).isEmpty { return nil }
                return captured(eventType: noteKind == "Target" ? "TARGET" : "BUDGET", title: purpose, amount: output, note: purpose, channelName: nil, counterpartyName: nil, payment: nil)
            }
            if purpose.trimmingCharacters(in: .whitespaces).isEmpty { return nil }
            let code = noteKind == "Issue" ? "ISSUE" : (noteKind == "Approval" ? "APPROVAL" : "UPDATE")
            return captured(eventType: code, title: purpose, amount: nil, note: nil, channelName: nil, counterpartyName: nil, payment: nil)
        default:
            return nil
        }
    }

    private func captured(
        eventType: String,
        title: String,
        amount: String?,
        note: String?,
        channelName: String?,
        counterpartyName: String?,
        payment: String?,
        quantity overrideQuantity: String? = nil,
        unit overrideUnit: String? = nil,
        parentEventId: String? = nil,
        contract: OperatingContractInput? = nil,
        rejectedQuantity: String? = nil,
        lineItems: [(itemName: String, quantity: String?, unit: String?, unitPrice: String?, masterId: String?)]? = nil
    ) -> OperatingCaptureDraft {
        let item = itemName.trimmingCharacters(in: .whitespaces)
        let priced = ["SALE", "SERVICE", "PURCHASE", "VENDOR"].contains(mode) && !item.isEmpty && contract == nil
        let shown = priced ? item : title
        let qty = overrideQuantity ?? (self.quantity.trimmingCharacters(in: .whitespaces).isEmpty ? nil : self.quantity)
        let rawUnit = overrideUnit ?? (unit.trimmingCharacters(in: .whitespaces).isEmpty ? nil : unit)
        let seededUnit = OperatingSeedUoms.canonical(rawUnit)
        let unitValue = tradingFamily ? (seededUnit.isEmpty ? nil : seededUnit) : rawUnit
        return OperatingCaptureDraft(
            eventType: eventType,
            title: shown.isEmpty ? self.title : shown,
            amount: amount,
            note: note,
            channelName: channelName,
            counterpartyName: counterpartyName,
            paymentStatus: payment,
            occurredAt: occurredAtValue,
            quantity: qty,
            unit: unitValue,
            unitPrice: unitPrice.trimmingCharacters(in: .whitespaces).isEmpty ? nil : unitPrice,
            itemName: item.isEmpty ? nil : item,
            parentEventId: parentEventId ?? (parentId.isEmpty ? nil : parentId),
            revisesEventId: seed?.id.isEmpty == false ? seed?.id : nil,
            masterKind: mode == "MASTERS" ? masterKind : nil,
            dueAt: nil,
            contract: contract,
            reportingMode: reportingMode,
            metricCode: metricCode,
            budgetCategory: budgetCategory,
            activityStatus: activityStatus,
            attributedSales: attributedSales.isEmpty ? nil : attributedSales,
            revisionKind: revisionKind,
            costBasis: costBasis,
            overlapDeduped: overlapDeduped,
            creativeVersion: creativeVersion,
            resultSource: resultSource.isEmpty ? nil : resultSource,
            idempotencyKey: idempotencyKey,
            effectiveFrom: marketing && ["BUDGET", "TARGET", "REVISION"].contains(eventType) ? calendarDay(eventDate) : nil,
            periodStart: marketing && eventType == "RESULT" ? isoDayText(periodStart) : nil,
            periodEnd: marketing && eventType == "RESULT" ? isoDayText(periodEnd) : nil,
            supersedesEventId: marketing && eventType == "RESULT" && !supersedesId.isEmpty ? supersedesId : nil,
            lineItems: lineItems,
            rejectedQuantity: rejectedQuantity
        )
    }

    private func calendarDay(_ date: Date) -> String {
        let format = DateFormatter()
        format.calendar = Calendar(identifier: .gregorian)
        format.locale = Locale(identifier: "en_US_POSIX")
        format.timeZone = TimeZone(secondsFromGMT: 0)
        format.dateFormat = "yyyy-MM-dd"
        return format.string(from: date)
    }

    private func isoDayText(_ raw: String) -> String? {
        let text = raw.trimmingCharacters(in: .whitespaces)
        guard text.range(of: #"^\d{4}-\d{2}-\d{2}$"#, options: .regularExpression) != nil else { return nil }
        return text
    }

    private var marketingFields: some View {
        VStack(alignment: .leading, spacing: 8) {
            if eventType == "INVOICE" {
                Text("Client").font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#C9C4D8"))
                Text((model.partyName?.isEmpty == false) ? model.partyName! : "Set the client in setup")
                    .font(.plusJakarta(size: 16, weight: .semibold)).foregroundStyle(Color(hex: "#E5E0EE"))
                Text("This invoice uses the campaign client.")
                    .font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#C9C4D8"))
            }
            if eventType == "PROGRESS" {
                let open = (model.activity ?? []).filter { $0.eventType == "ACTIVITY" && $0.approvalStatus != "REJECTED" }
                Text(open.isEmpty ? "Add an activity before updating progress." : "Open activity").font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#C9C4D8"))
                Text("Leave the amount blank to update status only.").font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#C9C4D8"))
                ForEach(Array(open.enumerated()), id: \.offset) { _, row in
                    Button(row.title) { parentId = row.id }
                        .font(.plusJakarta(size: 14, weight: .semibold))
                        .foregroundStyle(parentId == row.id ? Color(hex: "#2F009C") : Color(hex: "#E5E0EE"))
                        .padding(.vertical, 8)
                        .frame(maxWidth: .infinity)
                        .background(parentId == row.id ? Color(hex: "#C9BFFF") : Color(hex: "#35333E"))
                        .clipShape(RoundedRectangle(cornerRadius: 10))
                }
            }
            if eventType == "PROGRESS" || eventType == "RESULT" {
                Text("Reporting").font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#C9C4D8"))
                Picker("Mode", selection: $reportingMode) {
                    Text("Incremental").tag("INCREMENTAL")
                    Text("Cumulative").tag("CUMULATIVE")
                }.pickerStyle(.segmented)
                Picker("Metric", selection: $metricCode) {
                    ForEach(["LEADS", "CONVERSIONS", "REVENUE", "ORDERS", "REACH", "ENGAGEMENT", "CUSTOM_COUNT"], id: \.self) { Text($0).tag($0) }
                }
            }
            if eventType == "SPEND" || eventType == "INVOICE" {
                Picker("Budget category", selection: $budgetCategory) {
                    ForEach(["DIGITAL_ADS", "SOCIAL_CONTENT", "INFLUENCER", "EMAIL_DIRECT", "PRINT", "RADIO_AUDIO", "OUTDOOR", "EVENTS", "OUTREACH", "CREATIVE", "RESEARCH", "OTHER"], id: \.self) { Text($0).tag($0) }
                }
            }
            if eventType == "ACTIVITY" || eventType == "PROGRESS" {
                Picker("Status", selection: $activityStatus) {
                    Text("In progress").tag("IN_PROGRESS")
                    Text("Blocked").tag("BLOCKED")
                    Text("Postponed").tag("POSTPONED")
                    Text("Completed").tag("COMPLETED")
                    Text("Cancelled").tag("CANCELLED")
                }
            }
            if eventType == "SPEND" && model.subtype == "CLIENT_CAMPAIGN" {
                Picker("Who bears the cost", selection: $costBasis) {
                    Text("Agency borne").tag("AGENCY")
                    Text("Client reimbursable").tag("REIMBURSABLE")
                }
            }
                if eventType == "RESULT" {
                TextField("Source", text: $resultSource)
                TextField("Period start YYYY-MM-DD", text: $periodStart)
                TextField("Period end YYYY-MM-DD", text: $periodEnd)
                let earlier = (model.activity ?? []).filter { $0.eventType == "RESULT" && $0.approvalStatus != "REJECTED" }
                if !earlier.isEmpty {
                    Text("Correct an earlier result").font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#C9C4D8"))
                    ForEach(Array(earlier.enumerated()), id: \.offset) { _, row in
                        Button(supersedesId == row.id ? "Replaces \(row.title)" : row.title) {
                            supersedesId = supersedesId == row.id ? "" : row.id
                        }
                        .font(.plusJakarta(size: 14, weight: .semibold))
                        .foregroundStyle(Color(hex: "#E5E0EE"))
                    }
                }
                Toggle("Deduplicated reach", isOn: $overlapDeduped)
                Text("Attributed sales stay an observation. They are not revenue.")
                    .font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#C9C4D8"))
            }
            if eventType == "APPROVAL" {
                TextField("Creative version", text: $creativeVersion)
            }
            if eventType == "REVISION" {
                Picker("Revision", selection: $revisionKind) {
                    Text("Budget").tag("BUDGET")
                    Text("Target").tag("TARGET")
                    Text("Category").tag("CATEGORY")
                }.pickerStyle(.segmented)
            }
            if eventType == "RESULT" && model.subtype == "OWN_BUSINESS_CAMPAIGN" {
                TextField("Attributed sales (not revenue)", text: $attributedSales)
                    .font(.plusJakarta(size: 14))
                    .foregroundStyle(Color(hex: "#E5E0EE"))
            }
        }
    }

    private func money(_ raw: String?) -> String {
        let value = Decimal(string: raw ?? "") ?? 0
        let format = NumberFormatter()
        format.numberStyle = .decimal
        format.minimumFractionDigits = 2
        format.maximumFractionDigits = 2
        let text = format.string(from: value as NSDecimalNumber) ?? "0.00"
        return currency.uppercased() == "INR" ? "₹\(text)" : "\(text) \(currency)"
    }
    private func add(_ raw: String?, _ extra: Decimal) -> Decimal { (Decimal(string: raw ?? "") ?? 0) + extra }
    private var typed: Decimal { Decimal(string: amount) ?? 0 }
    private var saleLines: [String] {
        var lines = ["Sales \(money(model.finance?.sales)) → \(money(add(model.finance?.sales, typed).description))"]
        if choice == "Received" || (production && choice == "Paid") {
            lines.append("Received \(money(model.finance?.received)) → \(money(add(model.finance?.received, typed).description))")
        } else {
            lines.append("Outstanding \(money(model.finance?.outstanding)) → \(money(add(model.finance?.outstanding, typed).description))")
        }
        return lines
    }
    private var purchaseLines: [String] {
        var lines = ["Used \(money(model.budget?.spent)) → \(money(add(model.budget?.spent, typed).description))"]
        if choice.hasPrefix("Pending") {
            lines.append("Payables \(money(model.finance?.outstanding)) → \(money(add(model.finance?.outstanding, typed).description))")
        }
        lines.append("Draft and pending records stay out of the totals until approved.")
        return lines
    }
    private var spendLines: [String] {
        ["Used \(money(model.budget?.spent)) → \(money(add(model.budget?.spent, typed).description))", "A personal claim is reimbursed later. That is not a second expense."]
    }
    private var collectionLines: [String] {
        let due = (Decimal(string: model.finance?.outstanding ?? "") ?? 0) - typed
        return [
            "Received \(money(model.finance?.received)) → \(money(add(model.finance?.received, typed).description))",
            "Outstanding \(money(model.finance?.outstanding)) → \(money((due < 0 ? 0 : due).description))",
            "Sales stay unchanged.",
        ]
    }
    private var budgetLines: [String] {
        let proposed = typed > 0 ? typed : (Decimal(string: model.budget?.amount ?? "") ?? 0)
        let used = Decimal(string: model.budget?.spent ?? "") ?? 0
        let remaining = proposed - used
        return [
            "Used \(money(model.budget?.spent)) of \(money(proposed.description))",
            "Remaining \(money((remaining < 0 ? 0 : remaining).description))",
        ]
    }

    private func amountBlock(_ label: String, hint: String) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack {
                Text(label).font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#C9C4D8"))
                Spacer()
                Text(currency).font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#B7B8FF"))
            }
            TextField("0.00", text: $amount)
                .keyboardType(.decimalPad)
                .font(.plusJakarta(size: 30, weight: .bold))
                .foregroundStyle(Color(hex: "#E5E0EE"))
            Text(hint).font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#C9C4D8"))
        }
        .padding(10)
        .background(Color(hex: "#201E28"))
        .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color(hex: "#C9BFFF"), lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }

    private func choiceBlock(_ label: String, options: [String], selected: String, onSelect: @escaping (String) -> Void) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(label).font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#C9C4D8"))
            FlowLayout(spacing: 6) {
                ForEach(Array(options.enumerated()), id: \.offset) { _, option in
                    let on = option == selected
                    Button(on ? "✓ \(option)" : option) { onSelect(option) }
                        .font(.plusJakarta(size: 12, weight: .medium))
                        .foregroundStyle(on ? Color(hex: "#B7B8FF") : Color(hex: "#C9C4D8"))
                        .padding(.horizontal, 10)
                        .padding(.vertical, 8)
                        .background(on ? Color(hex: "#6C4EF2") : Color(hex: "#201E28"))
                        .overlay(RoundedRectangle(cornerRadius: 10).stroke(on ? Color(hex: "#C9BFFF") : Color(hex: "#938EA1"), lineWidth: 1))
                        .clipShape(RoundedRectangle(cornerRadius: 10))
                }
            }
        }
    }

    private func dateField(_ label: String) -> some View {
        Button { picker = .date } label: { readField(label, dateText) }
            .buttonStyle(.plain)
    }

    private var dueDateText: String {
        let format = DateFormatter()
        format.dateFormat = "dd MMM yyyy"
        format.locale = Locale(identifier: "en_US_POSIX")
        return format.string(from: dueDate)
    }

    private func dueDateField(_ label: String) -> some View {
        Button { picker = .due } label: { readField(label, dueDateText) }
            .buttonStyle(.plain)
    }

    private var goodsDueText: String {
        let format = DateFormatter()
        format.dateFormat = "dd MMM yyyy"
        format.locale = Locale(identifier: "en_US_POSIX")
        return format.string(from: goodsDueDate)
    }

    private func goodsDueField(_ label: String) -> some View {
        Button { picker = .goodsDue } label: { readField(label, goodsDueText) }
            .buttonStyle(.plain)
    }

    private var linkedOrderTitle: String {
        (model.activity ?? []).first { $0.id == parentId }?.title ?? ""
    }

    private func plainAmount(_ value: Decimal) -> String {
        NSDecimalNumber(decimal: value).stringValue
    }

    private func orderRows(side: String) -> [OperatingLovOption] {
        let childType = side == "PURCHASE_ORDER" ? "PURCHASE" : "SALE"
        let activity = model.activity ?? []
        return activity.compactMap { row in
            guard row.eventType == "ORDER", row.status != "COMPLETED", row.status != "CANCELLED" else { return nil }
            if side == "PURCHASE_ORDER" {
                guard row.orderSide == "PURCHASE" else { return nil }
            } else if row.orderSide == "PURCHASE" {
                return nil
            }
            let estimated = Decimal(string: row.estimatedAmount ?? "") ?? 0
            let executed = activity
                .filter { $0.parentEventId == row.id && $0.eventType == childType }
                .reduce(Decimal(0)) { $0 + (Decimal(string: $1.amount ?? "") ?? 0) }
            if estimated > 0 && executed >= estimated { return nil }
            let remaining = estimated - executed
            let subtitle = [
                row.counterpartyName,
                estimated > 0 ? "Ordered \(plainAmount(estimated))" : nil,
                "Executed \(plainAmount(executed))",
                estimated > 0 ? "Remaining \(plainAmount(remaining))" : nil,
            ].compactMap { $0 }.filter { !$0.isEmpty }.joined(separator: " · ")
            return OperatingLovOption(id: row.id, name: row.title, kind: side, unit: nil, subtitle: subtitle.isEmpty ? nil : subtitle)
        }
    }

    private func obligationRows() -> [OperatingLovOption] {
        let activity = model.activity ?? []
        return openObligations.map { row in
            let source = activity.first { $0.id == row.id }
            let subtitle = [
                row.counterpartyName,
                source?.occurredAt.prefix(10).description,
                row.total.map { "Total \($0)" },
                "Open \(row.amount)",
                row.dueAt.map { String($0.prefix(10)) },
            ].compactMap { $0 }.filter { !$0.isEmpty }.joined(separator: " · ")
            return OperatingLovOption(id: row.id, name: row.title, kind: "OBLIGATION", unit: nil, subtitle: subtitle.isEmpty ? nil : subtitle, detailAmount: row.amount)
        }
    }

    private func readField(_ label: String, _ value: String) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(label).font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#C9C4D8"))
            Text(value).font(.plusJakarta(size: 14)).foregroundStyle(Color(hex: "#E5E0EE"))
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 10)
        .padding(.vertical, 7)
        .background(Color(hex: "#201E28"))
        .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color(hex: "#938EA1"), lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }

    private func editField(_ label: String, text: Binding<String>, placeholder: String) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(label).font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#C9C4D8"))
            TextField(placeholder, text: text)
                .font(.plusJakarta(size: 14))
                .foregroundStyle(Color(hex: "#E5E0EE"))
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 10)
        .padding(.vertical, 7)
        .background(Color(hex: "#201E28"))
        .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color(hex: "#938EA1"), lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }

    private func moreButton(_ hint: String) -> some View {
        Button { moreOpen.toggle() } label: {
            VStack(alignment: .leading, spacing: 2) {
                Text(moreOpen ? "More details  ∧" : "More details  ∨").font(.plusJakarta(size: 14)).foregroundStyle(Color(hex: "#B7B8FF"))
                Text(hint).font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#C9C4D8"))
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
    }

    private func summary(_ title: String, _ lines: [String]) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(title).font(.plusJakarta(size: 14)).foregroundStyle(Color(hex: "#B7B8FF"))
            ForEach(Array(lines.enumerated()), id: \.offset) { _, line in
                Text(line).font(.plusJakarta(size: 14)).foregroundStyle(Color(hex: "#E5E0EE"))
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(10)
        .background(Color(hex: "#35333E"))
        .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color(hex: "#938EA1"), lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }

    private func priceModeCode(_ label: String) -> String? {
        switch label {
        case "GST included in price", "Prices include GST": return "YES"
        case "GST added separately": return "NO"
        default: return nil
        }
    }
}

private extension Decimal {
    func roundMoney() -> Decimal {
        var input = self
        var output = Decimal()
        NSDecimalRound(&output, &input, 2, .plain)
        return output
    }
}

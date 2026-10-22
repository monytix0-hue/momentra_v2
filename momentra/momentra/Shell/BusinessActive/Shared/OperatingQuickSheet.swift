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
    var lineItems: [(itemName: String, quantity: String?, unit: String?, unitPrice: String?, masterId: String?)]? = nil
    var rejectedQuantity: String? = nil
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
    /// kind, name, unit, optional conversionToBase
    var onCreateMaster: ((String, String, String?, String?) async throws -> Void)? = nil

    @Environment(\.dismiss) private var dismiss
    @State private var reportingMode = "INCREMENTAL"
    @State private var metricCode = "LEADS"
    @State private var budgetCategory = "DIGITAL_ADS"
    @State private var revisionKind = "BUDGET"
    @State private var activityStatus = "OPEN"
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
    @State private var dateOpen = false
    @State private var dueDate = Date()
    @State private var dueDateOpen = false
    @State private var itemName = ""
    @State private var quantity = ""
    @State private var rejectedQuantity = ""
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
    @State private var lovKind: String?
    @State private var localMasters: [OperatingLovOption] = []
    @State private var taxMode = "Not configured"
    @State private var taxAmount = ""
    @State private var originalCurrency = ""
    @State private var fxRate = ""
    @State private var returnDirection = "Customer"
    @State private var goodsOutcome = "To stock"
    @State private var returnReason = ""

    private var restaurant: Bool { variant == "restaurant" }
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
    private var tradingModes: Set<String> { ["SALE", "PURCHASE", "SPEND", "STOCK", "ORDER", "COLLECTION", "PAYMENT", "SHIPMENT", "DELIVERY", "RETURN"] }
    private var contractModes: Set<String> { retailFamily ? retailModes : tradingModes }
    private let expenseCategories = ["Rent/lease", "Utilities", "Wages/salaries", "Advertising", "Packaging", "Delivery/shipping", "Marketplace/platform fees", "Consumables", "Kitchen supplies", "Software/subscriptions", "Insurance", "Bank charges", "Gas/fuel", "Aggregator fees", "Miscellaneous"]
    private var canAddMaster: Bool {
        model.role == "OWNER_ADMIN" || (model.quickAdds ?? []).contains(where: { $0.code == "UPDATE" })
    }
    private var lovAccent: Color { trading || distribution ? Color(hex: trading ? "#6DDCFF" : "#5DDEA6") : Color(hex: "#B7B8FF") }
    private var allLovOptions: [OperatingLovOption] {
        let fromModel = (model.masters ?? []).map {
            OperatingLovOption(id: $0.id, name: $0.name, kind: $0.kind, unit: $0.unit, subtitle: $0.unit)
        }
        let expenseSeeds = expenseCategories.map {
            OperatingLovOption(id: "seed-expense-\($0)", name: $0, kind: "EXPENSE_CATEGORY", unit: nil, subtitle: nil)
        }
        let channelSeeds = channels.map {
            OperatingLovOption(id: "seed-channel-\($0)", name: $0, kind: "CHANNEL", unit: nil, subtitle: nil)
        }
        return localMasters + fromModel + OperatingSeedUoms.all + expenseSeeds + channelSeeds
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
                        Button("Close  ×") { dismiss() }
                            .font(.plusJakarta(size: 12))
                            .foregroundStyle(Color(hex: "#C9C4D8"))
                            .padding(8)
                            .background(Color(hex: "#35333E"))
                            .clipShape(RoundedRectangle(cornerRadius: 10))
                    }
                    Text(actor).font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#B7B8FF"))
                    Text(statusLine).font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#C9C4D8"))
                    if contractFamily && contractModes.contains(mode) { retailFields } else { fields }
                    if ["ACTIVITY", "PROGRESS", "RESULT", "SPEND", "REVISION", "INVOICE"].contains(eventType) {
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
        .sheet(isPresented: $dateOpen) {
            NavigationStack {
                DatePicker("Date", selection: $eventDate, displayedComponents: .date)
                    .datePickerStyle(.graphical)
                    .labelsHidden()
                    .padding()
                    .navigationTitle("Date")
                    .navigationBarTitleDisplayMode(.inline)
                    .toolbar {
                        ToolbarItem(placement: .confirmationAction) {
                            Button("Done") { dateOpen = false }
                        }
                    }
            }
            .presentationDetents([.medium, .large])
            .presentationBackground(Color(hex: "#3A3842"))
            .preferredColorScheme(.dark)
        }
        .sheet(isPresented: $dueDateOpen) {
            NavigationStack {
                DatePicker("Due date", selection: $dueDate, displayedComponents: .date)
                    .datePickerStyle(.graphical)
                    .labelsHidden()
                    .padding()
                    .navigationTitle("Due date")
                    .navigationBarTitleDisplayMode(.inline)
                    .toolbar {
                        ToolbarItem(placement: .confirmationAction) {
                            Button("Done") { dueDateOpen = false }
                        }
                    }
            }
            .presentationDetents([.medium, .large])
            .presentationBackground(Color(hex: "#3A3842"))
            .preferredColorScheme(.dark)
        }
        .sheet(item: Binding(
            get: { lovKind.map { LovSheetToken(kind: $0) } },
            set: { lovKind = $0?.kind }
        )) { token in
            SearchableOperatingLovSheet(
                title: lovTitle(token.kind),
                kind: token.kind,
                options: allLovOptions,
                canAdd: canAddMaster && token.kind.uppercased() != "UOM",
                accent: lovAccent,
                onSelect: { applyLovSelection(token.kind, $0) },
                onAdd: { name, unitValue, conversion in
                    Task {
                        do {
                            try await onCreateMaster?(token.kind, name, unitValue, conversion)
                            let option = OperatingLovOption(
                                id: "local-\(UUID().uuidString)",
                                name: name,
                                kind: token.kind,
                                unit: unitValue,
                                subtitle: unitValue
                            )
                            localMasters.insert(option, at: 0)
                            applyLovSelection(token.kind, option)
                        } catch {
                            errorText = error.localizedDescription
                        }
                    }
                },
                onDismiss: { lovKind = nil }
            )
            .presentationDetents([.medium, .large])
            .preferredColorScheme(.dark)
        }
        .onAppear {
            channel = channels.first ?? ""
            if marketing && mode == "DELIVERABLE" && detail.isEmpty { detail = "Due" }
            if production && mode == "PRODUCTION" { entryMode = "Quick output" }
            if production && mode == "DELIVERY" { entryMode = "Quick status" }
            if production && mode == "STOCK" && note.isEmpty { note = "opening" }
            choice = defaultChoice
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
        }
    }

    private struct LovSheetToken: Identifiable {
        let kind: String
        var id: String { kind }
    }

    private func lovTitle(_ kind: String) -> String {
        switch kind.uppercased() {
        case "PRODUCT": return "Product"
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
            if let unitName = option.unit, !unitName.isEmpty { unit = unitName }
            if mode == "STOCK" { purpose = option.name }
        case "BUYER", "CUSTOMER", "SUPPLIER":
            party = option.name
        case "CHANNEL":
            channel = option.name
        case "EXPENSE_CATEGORY":
            expenseCategory = option.name
        case "UOM":
            unit = option.name
        default:
            break
        }
    }

    @ViewBuilder private func lovField(_ label: String, value: String, kind: String, placeholder: String) -> some View {
        Button { lovKind = kind } label: {
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
        case "PURCHASE", "VENDOR":
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
        case "SPEND":
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
                editField("Purpose / Category", text: $purpose, placeholder: "What this was for")
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
        case "PAYMENT", "REIMBURSE":
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
        case "COLLECTION":
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
        case "BUDGET", "TARGET":
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
        case "ISSUE":
            editField("Issue", text: $purpose, placeholder: "What needs attention")
            choiceBlock("Priority", options: ["Low", "Normal", "High"], selected: choice) { choice = $0 }
            editField("Owner", text: $party, placeholder: actorName)
            if restaurant || service || trading || distribution || marketing || production {
                dateField("Due date")
                editField("Operational context", text: $note, placeholder: "Optional")
            }
            summary("After save", ["Opens an attention item.", "Money totals stay unchanged."])
        case "STOCK":
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
        case "ORDER":
            if production {
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
        case "SHIPMENT":
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
        case "APPOINTMENT":
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
        case "PERSON", "ROLE", "INVITE":
            editField("Name", text: $party, placeholder: "Person")
            editField(restaurant ? "Restaurant-facing role" : (service ? "Service-facing role" : (trading ? "Trade-facing role" : (distribution ? "Distribution-facing role" : (marketing ? "Campaign-facing role" : (production ? "Manufacturing role" : "Role"))))), text: $purpose, placeholder: "How they work here")
            choiceBlock("Access", options: OperatingMomentCatalog.roles.map(\.1), selected: choice) { choice = $0 }
            if restaurant || service || trading || distribution || marketing || production { editField("Scope", text: $note, placeholder: "Optional") }
            Text("A job label is not a grant. Access applies after the invitation is accepted.")
                .font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#C9C4D8"))
        case "SUPPLIER", "PARTNER":
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
        case "MASTERS":
            choiceBlock("Master", options: masterKinds, selected: masterKind) { masterKind = $0 }
            editField("Name", text: $itemName, placeholder: "Required")
            if ["PRODUCT", "MATERIAL", "CONSUMABLE"].contains(masterKind) {
                choiceBlock("Unit", options: ["piece", "kg", "litre", "count"], selected: unit) { unit = $0 }
            }
            editField("Detail", text: $purpose, placeholder: "Optional")
            Text("Saving adds a selectable record. It does not create a sale or a cost.")
                .font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#C9C4D8"))
        case "APPROVAL":
            approvalFields
        default:
            if production && mode == "PRODUCTION" {
                choiceBlock("Output", options: ["Quick output", "Detailed batch"], selected: entryMode) { entryMode = $0 }
                amountBlock("Output quantity", hint: "Recorded output. This is not accepted stock.")
                HStack(spacing: 12) { editField("Unit", text: $detail, placeholder: "Required"); dateField("Date") }
                editField(entryMode == "Detailed batch" ? "Finished product" : "Output context", text: $purpose, placeholder: "Required for a detailed batch")
                if entryMode == "Detailed batch" {
                    editField("Accepted quantity", text: $quantity, placeholder: "Required to add finished stock")
                    editField("Rejected quantity", text: $rejectedQuantity, placeholder: "Optional")
                    choiceBlock("Status", options: ["Planned", "In progress", "Completed"], selected: choice) { choice = $0 }
                }
                orderLink(job ? "Linked order · Optional" : "Linked order · Optional")
                Text("Quick output does not create a finished SKU. Unlike units are never added.")
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
            } else if marketing && mode == "RESULT" {
                amountBlock("Value", hint: "Positive amount · 2 decimal places")
                if !channels.isEmpty { choiceBlock(channelLabel, options: channels, selected: channel) { channel = $0 } }
                HStack(spacing: 12) { dateField("Result date"); editField("Metric", text: $purpose, placeholder: "What was measured") }
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
        if distribution && mode == "SALE" { return named }
        if distribution && mode == "ORDER" { return named && described }
        if distribution && mode == "SHIPMENT" { return named && described && noted }
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
        return true
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
                dismiss()
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
            dismiss()
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
                ForEach(saved) { master in
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
        if tradingFamily {
            choiceBlock("Tax mode", options: ["Not configured", "Inclusive", "Exclusive"], selected: taxMode) { taxMode = $0 }
            if taxMode != "Not configured" {
                editField("Tax amount", text: $taxAmount, placeholder: taxMode == "Inclusive" ? "Optional · display only" : "Optional")
            } else {
                Text("No tax amount is stored until tax mode is configured.")
                    .font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#C9C4D8"))
            }
            editField("Original currency", text: $originalCurrency, placeholder: "Leave blank for moment currency")
            if !originalCurrency.trimmingCharacters(in: .whitespaces).isEmpty {
                editField("FX rate", text: $fxRate, placeholder: "Required to convert")
            }
        }
    }

    @ViewBuilder private var obligationPicker: some View {
        if openObligations.isEmpty {
            Text(mode == "COLLECTION" ? "No unpaid sale is open." : "No unpaid purchase or expense is open.")
                .font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#C9C4D8"))
        } else {
            Text(mode == "COLLECTION" ? "Unpaid sales" : "Unpaid purchases and expenses")
                .font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#C9C4D8"))
            ForEach(openObligations) { row in
                Button {
                    parentId = row.id
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
            if tradingFamily {
                lovField(mode == "SERVICE" ? "Service channel" : "Sales channel", value: channel, kind: "CHANNEL", placeholder: "Required")
            } else if channels.isEmpty {
                editField(mode == "SERVICE" ? "Service channel" : "Sales channel", text: $channel, placeholder: "Required")
            } else {
                choiceBlock(mode == "SERVICE" ? "Service channel" : "Sales channel", options: channels, selected: channel) { channel = $0 }
            }
            dateField(mode == "SERVICE" ? "Charge date" : "Sale date")
            choiceBlock("Payment state", options: ["Received", "Part paid", "Pay later"], selected: choice) { choice = $0 }
            if choice == "Part paid" { editField("Received amount", text: $receivedAmount, placeholder: "Less than the total") }
            if choice != "Received" {
                if tradingFamily {
                    lovField(distribution ? "Buyer / dealer" : "Buyer", value: party, kind: "BUYER", placeholder: "Required when a balance remains")
                } else {
                    editField("Customer", text: $party, placeholder: "Required when a balance remains")
                }
            }
            if mode == "SERVICE" {
                let bookings = (model.activity ?? []).filter { $0.eventType == "APPOINTMENT" }
                if !bookings.isEmpty {
                    Text("Link a booking").font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#C9C4D8"))
                    ForEach(bookings.prefix(6)) { row in
                        Button(row.title) { parentId = row.id }
                            .font(.plusJakarta(size: 13))
                            .foregroundStyle(parentId == row.id ? Color(hex: "#2F009C") : Color(hex: "#E5E0EE"))
                    }
                }
            }
            Text(mode == "SERVICE" ? "A finalized charge is the sale. A booking is not revenue." : (tradingFamily ? "A quick amount does not change stock. Itemized sales do not deduct stock until dispatch." : "A quick amount does not change stock. Itemized lines do."))
                .font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#C9C4D8"))
            if choice != "Received" { dueDateField("Due date") }
            moreButton("Tax, FX, and note. Optional.")
            if moreOpen {
                taxFxMore
                editField("Note", text: $note, placeholder: "Optional")
            }
        case "PURCHASE":
            lineEditor
            amountBlock("Amount", hint: "Positive amount, or leave blank when items have prices")
            if tradingFamily {
                lovField(distribution ? "Principal / supplier" : "Supplier", value: party, kind: "SUPPLIER", placeholder: "Required")
            } else {
                editField("Supplier", text: $party, placeholder: "Required")
            }
            dateField("Purchase date")
            choiceBlock("Goods receipt", options: ["Received", "Part received", "Awaiting receipt"], selected: receiptState) { receiptState = $0 }
            if receiptState == "Part received" {
                editField("Quantity received", text: $quantityReceived, placeholder: "Less than ordered qty")
            }
            if tradingFamily {
                choiceBlock("Ownership", options: ["Owned", "Consignment"], selected: ownership) { ownership = $0 }
                Text("Consignment is not owned stock.")
                    .font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#C9C4D8"))
            }
            choiceBlock("Payment state", options: ["Paid", "Part paid", "Pay later"], selected: choice) { choice = $0 }
            if choice == "Part paid" { editField("Amount paid", text: $receivedAmount, placeholder: "Less than the total") }
            if choice != "Paid" { dueDateField("Due date") }
            moreButton("Tax, FX, and note. Optional.")
            if moreOpen {
                taxFxMore
                editField("Note", text: $note, placeholder: "Optional")
            }
        case "SPEND":
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
        case "COLLECTION", "PAYMENT":
            obligationPicker
            amountBlock(mode == "COLLECTION" ? "Collection amount" : "Payment amount", hint: "Cannot exceed the open balance")
            editField(mode == "COLLECTION" ? "Customer or platform" : "Supplier or payee", text: $party, placeholder: "Name")
            dateField(mode == "COLLECTION" ? "Collection date" : "Payment date")
            Text(mode == "COLLECTION" ? "This reduces what is owed. It does not increase sales." : "This reduces what you owe. It does not repeat the cost.")
                .font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#C9C4D8"))
            moreButton("Reference. Optional.")
            if moreOpen { editField("Reference", text: $note, placeholder: "Optional") }
        case "STOCK":
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
        case "ORDER":
            if tradingFamily {
                lovField("Buyer or channel", value: channel, kind: "CHANNEL", placeholder: "Required when tracking channels")
            } else if channels.isEmpty {
                editField("Order channel", text: $channel, placeholder: "Required")
            } else {
                choiceBlock("Order channel", options: channels, selected: channel) { channel = $0 }
            }
            choiceBlock("Status", options: ["Placed", "Preparing", "Completed", "Cancelled"], selected: orderStatus) { orderStatus = $0 }
            let openOrders = (model.activity ?? []).filter { $0.eventType == "ORDER" }
            if !openOrders.isEmpty {
                Text("Update an existing order").font(.plusJakarta(size: 12, weight: .semibold)).foregroundStyle(Color(hex: "#C9C4D8"))
                ForEach(openOrders) { row in
                    Button(parentId == row.id ? "Updating \(row.title)" : row.title) { parentId = row.id }
                        .font(.plusJakarta(size: 13, weight: .semibold))
                        .foregroundStyle(Color(hex: "#F4F1FA"))
                }
            }
            if tradingFamily {
                lovField("Buyer", value: party, kind: "BUYER", placeholder: "Optional if channel is set")
            } else {
                editField("Guest or customer", text: $party, placeholder: "Optional")
            }
            editField("Estimated amount", text: $amount, placeholder: "Pipeline only")
            if tradingFamily {
                editField("Quantity", text: $quantity, placeholder: "Optional")
                editField("Promised date", text: $link, placeholder: "YYYY-MM-DD")
            }
            dateField("Order time")
            Text("An order is not a sale until you bill it.")
                .font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#C9C4D8"))
            moreButton("Note. Optional.")
            if moreOpen { editField("Note", text: $note, placeholder: "Optional") }
        case "SHIPMENT", "DELIVERY":
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
        case "RETURN":
            choiceBlock("Direction", options: ["Customer", "Supplier"], selected: returnDirection) { returnDirection = $0 }
            let sources = (model.activity ?? []).filter {
                ($0.eventType == "SALE" && returnDirection == "Customer") || ($0.eventType == "PURCHASE" && returnDirection == "Supplier")
            }
            if sources.isEmpty {
                Text(returnDirection == "Customer" ? "No open sale to credit." : "No open purchase to credit.")
                    .font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#C9C4D8"))
            } else {
                Text("Source").font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#C9C4D8"))
                ForEach(sources.prefix(8)) { row in
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
        case "APPOINTMENT":
            editField("Service", text: $purpose, placeholder: "Required")
            dateField("Appointment time")
            editField("Guest or customer", text: $party, placeholder: "Optional for a walk-in")
            editField("Assigned person", text: $detail, placeholder: "Optional")
            let openBookings = (model.activity ?? []).filter { $0.eventType == "APPOINTMENT" }
            if !openBookings.isEmpty {
                Text("Update an existing booking").font(.plusJakarta(size: 12, weight: .semibold)).foregroundStyle(Color(hex: "#C9C4D8"))
                ForEach(openBookings) { row in
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
        case "UPDATE", "ISSUE", "APPROVAL":
            choiceBlock("Record type", options: ["Update", "Issue", "Approval"], selected: noteKind) { noteKind = $0 }
            editField("What happened", text: $purpose, placeholder: "Required")
            moreButton("Note. Optional.")
            if moreOpen { editField("Note", text: $note, placeholder: "Optional") }
        default:
            EmptyView()
        }
    }

    private func buildDraft() -> OperatingCaptureDraft? {
        if contractFamily && contractModes.contains(mode) { return retailDraft() }
        if mode == "MASTERS" {
            let name = itemName.trimmingCharacters(in: .whitespaces)
            if name.isEmpty { return nil }
            return captured(eventType: "UPDATE", title: name, amount: nil, note: purpose, channelName: nil, counterpartyName: nil, payment: nil)
        }
        if production && ["PRODUCTION", "PURCHASE", "SPEND", "SALE", "ORDER", "DELIVERY", "PAYMENT", "COLLECTION", "STOCK", "BUDGET", "UPDATE", "ISSUE"].contains(mode) {
            return productionGovernedDraft()
        }
        let needsAmount = ["SALE", "SERVICE", "PURCHASE", "VENDOR", "SPEND", "PAYMENT", "REIMBURSE", "COLLECTION", "BUDGET", "TARGET", "RESULT", "PRODUCTION"].contains(mode)
        let value = Decimal(string: amount) ?? 0
        if needsAmount, value <= 0 { return nil }
        let needsText = ["UPDATE", "ISSUE", "STOCK", "ORDER", "SHIPMENT", "DELIVERY", "APPOINTMENT", "DELIVERABLE", "COMPLETE"].contains(mode)
        if needsText, !(production && mode == "ORDER"), purpose.trimmingCharacters(in: .whitespaces).isEmpty, note.trimmingCharacters(in: .whitespaces).isEmpty { return nil }
        if trading, ["STOCK", "ORDER", "SHIPMENT"].contains(mode), purpose.trimmingCharacters(in: .whitespaces).isEmpty || note.trimmingCharacters(in: .whitespaces).isEmpty { return nil }
        if distribution, mode == "STOCK", purpose.trimmingCharacters(in: .whitespaces).isEmpty || note.trimmingCharacters(in: .whitespaces).isEmpty { return nil }
        if distribution, mode == "SALE", party.trimmingCharacters(in: .whitespaces).isEmpty { return nil }
        if distribution, mode == "ORDER", purpose.trimmingCharacters(in: .whitespaces).isEmpty || party.trimmingCharacters(in: .whitespaces).isEmpty { return nil }
        if distribution, mode == "SHIPMENT", purpose.trimmingCharacters(in: .whitespaces).isEmpty || note.trimmingCharacters(in: .whitespaces).isEmpty || party.trimmingCharacters(in: .whitespaces).isEmpty { return nil }
        if marketing, mode == "RESULT", purpose.trimmingCharacters(in: .whitespaces).isEmpty { return nil }
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
            if channel.trimmingCharacters(in: .whitespaces).isEmpty { return nil }
            if entryMode == "Quick amount", (Decimal(string: rawAmount) ?? 0) <= 0 { return nil }
            if entryMode == "Itemized", lines.isEmpty { return nil }
            if choice == "Part paid", (Decimal(string: receivedAmount) ?? 0) <= 0 { return nil }
            if choice != "Received", party.trimmingCharacters(in: .whitespaces).isEmpty { return nil }
        case "PURCHASE":
            if party.trimmingCharacters(in: .whitespaces).isEmpty { return nil }
            if lines.isEmpty, (Decimal(string: rawAmount) ?? 0) <= 0 { return nil }
            if receiptState == "Part received", quantityReceived.trimmingCharacters(in: .whitespaces).isEmpty { return nil }
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
        default:
            return nil
        }
        let paymentState: String? = {
            switch choice {
            case "Received": return "RECEIVED"
            case "Part paid": return "PART_PAID"
            case "Pay later": return "PAY_LATER"
            case "Paid": return "PAID"
            default: return nil
            }
        }()
        let receipt: String? = {
            switch receiptState {
            case "Received": return "RECEIVED"
            case "Part received": return "PART_RECEIVED"
            case "Awaiting receipt": return "AWAITING"
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
            case "Completed": return "COMPLETED"
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
            case "STOCK", "APPOINTMENT", "SHIPMENT", "DELIVERY": return purpose.trimmingCharacters(in: .whitespaces)
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
            guard tradingFamily && ["SALE", "PURCHASE"].contains(mode) else { return nil }
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
        let promised = link.trimmingCharacters(in: .whitespaces)
        let fxCurrency = originalCurrency.trimmingCharacters(in: .whitespacesAndNewlines).uppercased()
        let allocation = mode == "ORDER" || mode == "APPOINTMENT" || parentId.isEmpty || rawAmount.isEmpty ? nil : [OperatingAllocationInput(sourceId: parentId, amount: rawAmount)]
        let contract = OperatingContractInput(
            entryMode: mode == "SALE" || mode == "SERVICE" ? (entryMode == "Itemized" ? "ITEMIZED" : "QUICK") : nil,
            paymentState: ["SALE", "SERVICE", "PURCHASE", "SPEND"].contains(mode) ? paymentState : nil,
            receiptState: mode == "PURCHASE" ? receipt : nil,
            receivedAmount: receivedAmount.isEmpty ? nil : receivedAmount,
            quantityReceived: mode == "PURCHASE" && receiptState == "Part received" ? quantityReceived : nil,
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
            promisedAt: (mode == "ORDER" || mode == "SHIPMENT" || mode == "DELIVERY") && !promised.isEmpty ? promised : nil,
            taxMode: taxModeCode,
            taxAmount: taxModeCode == "NOT_CONFIGURED" || taxAmount.isEmpty ? nil : taxAmount,
            originalCurrency: fxCurrency.isEmpty ? nil : fxCurrency,
            fxRate: fxCurrency.isEmpty || fxRate.isEmpty ? nil : fxRate,
            returnDirection: mode == "RETURN" ? (returnDirection == "Supplier" ? "SUPPLIER" : "CUSTOMER") : nil,
            goodsOutcome: goodsOutcomeCode,
            returnReason: mode == "RETURN" ? returnReason.trimmingCharacters(in: .whitespaces) : nil
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
            unit: unit.trimmingCharacters(in: .whitespaces).isEmpty ? nil : unit,
            parentEventId: mode == "ORDER" || mode == "APPOINTMENT" ? nil : (parentId.isEmpty ? nil : parentId),
            revisesEventId: (mode == "ORDER" || mode == "APPOINTMENT") && !parentId.isEmpty ? parentId : seed?.id,
            dueAt: retailDueAt,
            contract: contract,
            lineItems: lines.isEmpty ? nil : lines
        )
    }

    private var retailDueAt: String? {
        let unpaidSale = (mode == "SALE" || mode == "SERVICE") && choice != "Received"
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
            ForEach(orders) { row in
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
            case "Received": return "RECEIVED"
            case "Paid": return "PAID"
            case "Part paid": return "PART_PAID"
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
                productionStatus: detailed ? productionCode(choice) : nil
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
                contract: OperatingContractInput(orderStatus: "PLACED", estimatedAmount: output.isEmpty ? nil : output)
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
        let unitValue = overrideUnit ?? (unit.trimmingCharacters(in: .whitespaces).isEmpty ? nil : unit)
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
            lineItems: lineItems,
            rejectedQuantity: rejectedQuantity
        )
    }

    private var marketingFields: some View {
        VStack(alignment: .leading, spacing: 8) {
            if eventType == "PROGRESS" {
                let open = (model.activity ?? []).filter { $0.eventType == "ACTIVITY" && $0.approvalStatus != "REJECTED" }
                Text("Open activity").font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#C9C4D8"))
                ForEach(open) { row in
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
            if eventType == "ACTIVITY" {
                Picker("Status", selection: $activityStatus) {
                    Text("Open").tag("OPEN")
                    Text("Done").tag("DONE")
                }.pickerStyle(.segmented)
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
                ForEach(options, id: \.self) { option in
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
        Button { dateOpen = true } label: { readField(label, dateText) }
            .buttonStyle(.plain)
    }

    private var dueDateText: String {
        let format = DateFormatter()
        format.dateFormat = "dd MMM yyyy"
        format.locale = Locale(identifier: "en_US_POSIX")
        return format.string(from: dueDate)
    }

    private func dueDateField(_ label: String) -> some View {
        Button { dueDateOpen = true } label: { readField(label, dueDateText) }
            .buttonStyle(.plain)
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
            ForEach(lines, id: \.self) { line in
                Text(line).font(.plusJakarta(size: 14)).foregroundStyle(Color(hex: "#E5E0EE"))
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(10)
        .background(Color(hex: "#35333E"))
        .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color(hex: "#938EA1"), lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }
}

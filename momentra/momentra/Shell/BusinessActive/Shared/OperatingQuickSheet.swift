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
    var dueAt: String? = nil
    var contract: OperatingContractInput? = nil
    var lineItems: [(itemName: String, quantity: String?, unit: String?, unitPrice: String?)]? = nil
}

private struct RetailDraftLine: Identifiable {
    let id = UUID()
    var itemName: String
    var quantity: String
    var unit: String
    var unitPrice: String
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

    @Environment(\.dismiss) private var dismiss
    @State private var amount = ""
    @State private var channel = ""
    @State private var party = ""
    @State private var purpose = ""
    @State private var note = ""
    @State private var link = ""
    @State private var detail = ""
    @State private var choice = ""
    @State private var moreOpen = false
    @State private var errorText: String?
    @State private var eventDate = Date()
    @State private var dateOpen = false
    @State private var itemName = ""
    @State private var quantity = ""
    @State private var unit = "piece"
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
    private var tradingModes: Set<String> { ["SALE", "PURCHASE", "SPEND", "STOCK", "ORDER", "COLLECTION", "PAYMENT", "SHIPMENT", "DELIVERY"] }
    private var contractModes: Set<String> { retailFamily ? retailModes : tradingModes }
    private let expenseCategories = ["Rent/lease", "Utilities", "Wages/salaries", "Advertising", "Packaging", "Delivery/shipping", "Marketplace/platform fees", "Consumables", "Kitchen supplies", "Software/subscriptions", "Insurance", "Bank charges", "Gas/fuel", "Aggregator fees", "Miscellaneous"]

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
        .onAppear {
            channel = channels.first ?? ""
            if marketing && mode == "DELIVERABLE" && detail.isEmpty { detail = "Due" }
            if production && mode == "SPEND" && detail.isEmpty { detail = "No" }
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

    @ViewBuilder private var fields: some View {
        switch mode {
        case "SALE", "SERVICE":
            lineEditor
            if production && mode == "SALE" {
                amountBlock("Amount", hint: "Positive amount · 2 decimal places")
                if !channels.isEmpty {
                    choiceBlock("Buyer / channel", options: channels, selected: channel) { channel = $0 }
                } else {
                    editField("Buyer", text: $party, placeholder: "Required")
                }
                HStack(spacing: 12) { dateField("Sale date"); editField("Fulfilment status", text: $detail, placeholder: "Dispatched") }
                choiceBlock("Payment state", options: ["Paid", "Pending"], selected: choice) { choice = $0 }
                Text("Buyer and fulfilment status required. Reference and output quantity are optional.")
                    .font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#C9C4D8"))
                moreButton("Batch, output quantity, and reference. Optional.")
                if moreOpen { editField("Note", text: $note, placeholder: "Optional") }
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
                amountBlock("Amount", hint: "Positive amount · 2 decimal places")
                HStack(spacing: 12) { editField("Supplier", text: $party, placeholder: "Required"); dateField("Purchase date") }
                editField("Purpose", text: $purpose, placeholder: "What was bought")
                choiceBlock("Payment state", options: ["Pending / unpaid", "Paid"], selected: choice) { choice = $0 }
                Text("Pending approval. Submission is not payment.").font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#C9C4D8"))
                moreButton("Quantity, reference, and due date. Optional.")
                if moreOpen { editField("Note", text: $note, placeholder: "Optional") }
                summary("After approval", purchaseLines)
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
                amountBlock("Amount", hint: "Positive amount · 2 decimal places")
                HStack(spacing: 12) { editField("Purpose", text: $purpose, placeholder: "What this was for"); dateField("Expense date") }
                choiceBlock("Funding source", options: ["Business", "Personal"], selected: choice) { choice = $0 }
                editField("Paid by", text: $party, placeholder: actorName)
                choiceBlock("Claim after approval?", options: ["Yes · Reimburse", "No"], selected: detail) { detail = $0 }
                Text("A personal claim is reimbursed later. It is not a second expense.")
                    .font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#C9C4D8"))
                moreButton("Reference and note. Optional.")
                if moreOpen { editField("Note", text: $note, placeholder: "Optional") }
                summary("After approval", spendLines)
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
                editField("Material / finished-goods context", text: $purpose, placeholder: "Required")
                choiceBlock("Quantity / stock status", options: ["Available", "Needs check", "Low availability"], selected: choice) { choice = $0 }
                editField("Stock note", text: $note, placeholder: "Required")
                HStack(spacing: 12) { readField("Recorded by", actorName); dateField("Draft time") }
                Text("This is an operational signal. It creates no purchase, sale, or money record.")
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
                editField("Customer", text: $party, placeholder: "Required")
                choiceBlock(
                    "Order status",
                    options: job
                        ? ["Open", "In production", "Ready", "Delivered", "Cancelled", "Delayed", "Blocked"]
                        : ["Open", "In production", "Ready", "Delivered"],
                    selected: choice
                ) { choice = $0 }
                amountBlock("Value", hint: "Positive amount")
                dateField("Due date")
                editField("Reference", text: $purpose, placeholder: "Optional")
                editField("Specification note", text: $note, placeholder: "Optional")
                Text("An order is a commitment, not revenue.")
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
                amountBlock("Quantity / output", hint: "Positive output")
                HStack(spacing: 12) { editField("Unit", text: $detail, placeholder: "Required"); dateField("Date") }
                editField("Output context", text: $purpose, placeholder: "Required")
                Text(job
                     ? "Unit compatible with this job context. Custom jobs and unlike units are never aggregated."
                     : "Unit compatible with this output context. Unlike units are never aggregated.")
                    .font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#C9C4D8"))
                HStack(spacing: 12) {
                    editField(job ? "Linked order" : "Batch", text: $link, placeholder: "Optional")
                    editField(job ? "Batch / ref" : "Order / ref", text: $party, placeholder: "Optional")
                }
                editField("Note", text: $note, placeholder: "Optional")
                readField("Recorded by", actorName)
                Text("Catalogue, BOM, and machine setup are optional. Money totals stay unchanged.")
                    .font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#C9C4D8"))
            } else if production && mode == "UPDATE" {
                editField("Update text", text: $purpose, placeholder: "Required")
                editField("Linked operational context", text: $note, placeholder: "Optional")
                HStack(spacing: 12) { readField("Recorded by", actorName); dateField("Draft time") }
                summary("After save", ["Saved as production activity.", "Money totals stay unchanged."])
            } else if production && mode == "DELIVERY" {
                editField(job ? "Order" : "Order / buyer", text: $purpose, placeholder: "Required")
                choiceBlock("Delivery status", options: ["Pending", "Partial", "Delivered"], selected: choice) { choice = $0 }
                editField("Delivered quantity", text: $note, placeholder: "Required")
                amountBlock("Value · Optional", hint: "Leave blank if there is no amount")
                if job {
                    HStack(spacing: 12) {
                        editField("Customer", text: $party, placeholder: "Required")
                        editField("Linked order", text: $link, placeholder: "Optional")
                    }
                }
                dateField("Delivery date")
                Text("Delivered is not collected. It does not recognize the sale again.")
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
        if production && mode == "SALE" { return "Pending" }
        if production && mode == "ORDER" { return "In production" }
        if production && mode == "DELIVERY" { return "Delivered" }
        if production && mode == "STOCK" { return "Needs check" }
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
        if production && mode == "PRODUCTION" { return detailed && described }
        if production && mode == "SALE" { return !channel.trimmingCharacters(in: .whitespaces).isEmpty || named }
        if production && mode == "PURCHASE" { return named }
        if production && mode == "ORDER" { return named }
        if production && mode == "DELIVERY" { return described && noted && (!job || named) }
        if production && mode == "STOCK" { return described && noted }
        if production && mode == "BUDGET" { return described }
        if production && mode == "ISSUE" { return described && named }
        if production && mode == "UPDATE" { return described }
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
        case "TRADING_WHOLESALE", "DISTRIBUTION_DEALERSHIP": return ["PRODUCT", "SUPPLIER", "BUYER", "CHANNEL"]
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
        Text("Quantity, unit, and price stay on the sale or purchase. Stock changes only when you record that the goods were received or dispatched.")
            .font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#C9C4D8"))
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
                        savedLines.append(RetailDraftLine(itemName: name, quantity: quantity, unit: unit, unitPrice: unitPrice))
                        itemName = ""; quantity = ""; unitPrice = ""
                    }
                }
                .font(.plusJakarta(size: 13, weight: .semibold)).foregroundStyle(Color(hex: "#B7B8FF"))
                ForEach(savedLines) { line in
                    Text("\(line.itemName) · \(line.quantity) \(line.unit)").font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#C9C4D8"))
                }
            }
            if entryMode == "Quick amount" { amountBlock("Sale amount", hint: "Positive amount · 2 decimal places") }
            if channels.isEmpty { editField(mode == "SERVICE" ? "Service channel" : "Sales channel", text: $channel, placeholder: "Required") }
            else { choiceBlock(mode == "SERVICE" ? "Service channel" : "Sales channel", options: channels, selected: channel) { channel = $0 } }
            dateField(mode == "SERVICE" ? "Charge date" : "Sale date")
            choiceBlock("Payment state", options: ["Received", "Part paid", "Pay later"], selected: choice) { choice = $0 }
            if choice == "Part paid" { editField("Received amount", text: $receivedAmount, placeholder: "Less than the total") }
            if choice != "Received" { editField("Customer", text: $party, placeholder: "Required when a balance remains") }
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
            Text(mode == "SERVICE" ? "A finalized charge is the sale. A booking is not revenue." : "A quick amount does not change stock. Itemized lines do.")
                .font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#C9C4D8"))
            moreButton("Due date, note, and reference. Optional.")
            if moreOpen {
                editField("Due date", text: $link, placeholder: "YYYY-MM-DD")
                editField("Note", text: $note, placeholder: "Optional")
            }
        case "PURCHASE":
            lineEditor
            amountBlock("Amount", hint: "Positive amount, or leave blank when items have prices")
            editField("Supplier", text: $party, placeholder: "Required")
            dateField("Purchase date")
            choiceBlock("Goods receipt", options: ["Received", "Part received", "Awaiting receipt"], selected: receiptState) { receiptState = $0 }
            if receiptState == "Part received" { editField("Quantity received", text: $quantity, placeholder: "Required") }
            if tradingFamily {
                choiceBlock("Ownership", options: ["Owned", "Consignment"], selected: ownership) { ownership = $0 }
                Text("Consignment is not owned stock.")
                    .font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#C9C4D8"))
            }
            choiceBlock("Payment state", options: ["Paid", "Part paid", "Pay later"], selected: choice) { choice = $0 }
            if choice == "Part paid" { editField("Amount paid", text: $receivedAmount, placeholder: "Less than the total") }
            moreButton("Due date and note. Optional.")
            if moreOpen {
                editField("Due date", text: $link, placeholder: "YYYY-MM-DD")
                editField("Note", text: $note, placeholder: "Optional")
            }
        case "SPEND":
            choiceBlock("Category", options: expenseCategories, selected: expenseCategory) { expenseCategory = $0 }
            amountBlock("Amount", hint: "Positive amount")
            dateField("Expense date")
            choiceBlock("Payment state", options: ["Paid", "Part paid", "Pay later"], selected: choice) { choice = $0 }
            if choice != "Paid" { editField("Payee", text: $party, placeholder: "Required") }
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
            editField("Item", text: $purpose, placeholder: "Product, ingredient, or supply")
            choiceBlock("Action", options: ["Set current stock", "Add", "Reduce"], selected: stockAction) { stockAction = $0 }
            editField("Quantity", text: $quantity, placeholder: "Required")
            editField("Unit", text: $unit, placeholder: "pcs, kg, L")
            choiceBlock("Reason", options: ["Opening count", "Stocktake", "Damage", "Waste", "Return", "Correction"], selected: stockReason) { stockReason = $0 }
            if tradingFamily {
                choiceBlock("Ownership", options: ["Owned", "Consignment"], selected: ownership) { ownership = $0 }
            }
            editField("Reorder point", text: $detail, placeholder: "Optional")
            Text("Set replaces the count. Add and reduce move it. This is not a purchase.")
                .font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#C9C4D8"))
        case "ORDER":
            if channels.isEmpty { editField(tradingFamily ? "Buyer or channel" : "Order channel", text: $channel, placeholder: "Required") }
            else { choiceBlock(tradingFamily ? "Buyer channel" : "Order channel", options: channels, selected: channel) { channel = $0 } }
            choiceBlock("Status", options: ["Placed", "Preparing", "Completed", "Cancelled"], selected: orderStatus) { orderStatus = $0 }
            editField(tradingFamily ? "Buyer" : "Guest or customer", text: $party, placeholder: tradingFamily ? "Optional if channel is set" : "Optional")
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
        case "APPOINTMENT":
            editField("Service", text: $purpose, placeholder: "Required")
            dateField("Appointment time")
            editField("Guest or customer", text: $party, placeholder: "Optional for a walk-in")
            editField("Assigned person", text: $detail, placeholder: "Optional")
            if seed != nil {
                choiceBlock("Action", options: ["Reschedule", "Cancel", "No-show", "Start", "Complete"], selected: bookingAction) { bookingAction = $0 }
            }
            Text("Booking, starting, and completing do not post revenue. Finalize a charge separately.")
                .font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#C9C4D8"))
            moreButton("Reason. Optional.")
            if moreOpen { editField("Reason", text: $note, placeholder: "Optional") }
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
            if receiptState == "Part received", quantity.trimmingCharacters(in: .whitespaces).isEmpty { return nil }
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
            case "SALE", "SERVICE": return lines.first?.itemName ?? title
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
        let promised = link.trimmingCharacters(in: .whitespaces)
        let allocation = parentId.isEmpty || rawAmount.isEmpty ? nil : [OperatingAllocationInput(sourceId: parentId, amount: rawAmount)]
        let contract = OperatingContractInput(
            entryMode: mode == "SALE" || mode == "SERVICE" ? (entryMode == "Itemized" ? "ITEMIZED" : "QUICK") : nil,
            paymentState: ["SALE", "SERVICE", "PURCHASE", "SPEND"].contains(mode) ? paymentState : nil,
            receiptState: mode == "PURCHASE" ? receipt : nil,
            receivedAmount: receivedAmount.isEmpty ? nil : receivedAmount,
            quantityReceived: mode == "PURCHASE" && receiptState == "Part received" ? quantity : nil,
            stockAction: mode == "STOCK" ? stockCode : nil,
            stockReason: mode == "STOCK" ? reasonCode : nil,
            expenseCategory: mode == "SPEND" ? expenseCategory : nil,
            appointmentAction: mode == "APPOINTMENT" && seed != nil ? actionCode : nil,
            orderStatus: mode == "ORDER" ? orderCode : nil,
            clientKey: clientKey,
            reorderPoint: mode == "STOCK" && !detail.trimmingCharacters(in: .whitespaces).isEmpty ? detail : nil,
            staffName: mode == "APPOINTMENT" && !detail.trimmingCharacters(in: .whitespaces).isEmpty ? detail : nil,
            estimatedAmount: mode == "ORDER" && !rawAmount.isEmpty ? rawAmount : nil,
            allocations: allocation,
            ownership: ownershipCode,
            shipmentStatus: shipmentCode,
            promisedAt: (mode == "ORDER" || mode == "SHIPMENT" || mode == "DELIVERY") && !promised.isEmpty ? promised : nil
        )
        return OperatingCaptureDraft(
            eventType: eventType,
            title: titleText.isEmpty ? title : titleText,
            amount: rawAmount.isEmpty ? nil : rawAmount,
            note: note.trimmingCharacters(in: .whitespaces).isEmpty ? nil : note,
            channelName: channel.trimmingCharacters(in: .whitespaces).isEmpty ? nil : channel,
            counterpartyName: party.trimmingCharacters(in: .whitespaces).isEmpty ? nil : party,
            paymentStatus: nil,
            occurredAt: mode == "APPOINTMENT" || mode == "ORDER" ? isoDay(eventDate) : occurredAtValue,
            quantity: quantity.trimmingCharacters(in: .whitespaces).isEmpty ? nil : quantity,
            unit: unit.trimmingCharacters(in: .whitespaces).isEmpty ? nil : unit,
            parentEventId: parentId.isEmpty ? nil : parentId,
            revisesEventId: seed?.id,
            dueAt: link.trimmingCharacters(in: .whitespaces).isEmpty ? nil : link,
            contract: contract,
            lineItems: lines.isEmpty ? nil : lines
        )
    }

    private func retailLines() -> [(itemName: String, quantity: String?, unit: String?, unitPrice: String?)] {
        var rows = savedLines.map { (itemName: $0.itemName, quantity: $0.quantity, unit: $0.unit, unitPrice: $0.unitPrice) }
        let name = itemName.trimmingCharacters(in: .whitespaces)
        if !name.isEmpty { rows.append((itemName: name, quantity: quantity, unit: unit, unitPrice: unitPrice)) }
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

    private func captured(
        eventType: String,
        title: String,
        amount: String?,
        note: String?,
        channelName: String?,
        counterpartyName: String?,
        payment: String?
    ) -> OperatingCaptureDraft {
        let item = itemName.trimmingCharacters(in: .whitespaces)
        let priced = ["SALE", "SERVICE", "PURCHASE", "VENDOR"].contains(mode) && !item.isEmpty
        let shown = priced ? item : title
        return OperatingCaptureDraft(
            eventType: eventType,
            title: shown.isEmpty ? self.title : shown,
            amount: amount,
            note: note,
            channelName: channelName,
            counterpartyName: counterpartyName,
            paymentStatus: payment,
            occurredAt: occurredAtValue,
            quantity: quantity.trimmingCharacters(in: .whitespaces).isEmpty ? nil : quantity,
            unit: unit.trimmingCharacters(in: .whitespaces).isEmpty ? nil : unit,
            unitPrice: unitPrice.trimmingCharacters(in: .whitespaces).isEmpty ? nil : unitPrice,
            itemName: item.isEmpty ? nil : item,
            parentEventId: parentId.isEmpty ? nil : parentId,
            revisesEventId: seed?.id.isEmpty == false ? seed?.id : nil,
            masterKind: mode == "MASTERS" ? masterKind : nil
        )
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

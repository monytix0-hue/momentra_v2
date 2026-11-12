import SwiftUI

/// MTO journey / money / ops captures — Figma MTO-QA soft-amber chrome.
struct MtoCaptureSheet: View {
    let theme: OperatingTheme
    let mode: String
    @ObservedObject var model: BusinessOperatingModel
    var browserPrefill: OperatingBrowserItem? = nil
    var onRouteUtility: ((String) -> Void)? = nil

    var body: some View {
        MtoCaptureForm(
            theme: theme,
            mode: mode,
            model: model,
            browserPrefill: browserPrefill,
            onRouteUtility: onRouteUtility
        )
    }
}

private struct MtoCaptureForm: View {
    let theme: OperatingTheme
    let mode: String
    @ObservedObject var model: BusinessOperatingModel
    var browserPrefill: OperatingBrowserItem? = nil
    var onRouteUtility: ((String) -> Void)? = nil

    @Environment(\.dismiss) private var dismiss
    @State private var amount = ""
    @State private var party = ""
    @State private var channel = "Wholesale"
    @State private var payState = "Partial"
    @State private var saleMode = "From order"
    @State private var note = ""
    @State private var titleText = ""
    @State private var qty = ""
    @State private var rejected = "0"
    @State private var product = ""
    @State private var kindChip = ""
    @State private var saving = false
    @State private var showDatePicker = false
    @State private var showMonthPicker = false
    @State private var showDateTimePicker = false
    @State private var datePickerTarget: DatePickerTarget = .sale
    @State private var sourceInvoice = ""
    @State private var orderRef = ""
    @State private var reference = ""
    @State private var prefilledPart: String? = nil
    @State private var prefilledMasterId: String? = nil
    @State private var prefilledUnit: String? = nil
    @State private var didApplyBrowserPrefill = false
    @State private var actionTitle = ""
    @State private var approver = ""
    @State private var policy = "Finance ≥ ₹1L"
    @State private var scrapReason = ""
    @State private var materials = "Explicit consumption"
    @State private var jobLink = ""
    @State private var acceptedRemaining = "12"
    @State private var confirmPod = "Pending"
    @State private var jobTag = "J-07"
    @State private var owner = ""
    @State private var issueLink = ""
    @State private var returnReason = ""
    @State private var expensePayState = "Payable · 15 days"
    @State private var receiveNow = "Receive now"
    @State private var budgetReason = ""
    @State private var bomRev = "r1"
    @State private var plannedStart: String = Self.isoToday()
    @State private var confirmDue: String = {
        let d = Calendar.current.date(byAdding: .day, value: 21, to: Date()) ?? Date()
        return Self.iso(d)
    }()
    @State private var openJobDue: String = {
        let d = Calendar.current.date(byAdding: .day, value: 14, to: Date()) ?? Date()
        return Self.iso(d)
    }()
    @State private var creditNoteDate: String = Self.isoToday()
    @State private var issueDue: String = {
        let d = Calendar.current.date(byAdding: .day, value: 2, to: Date()) ?? Date()
        return Self.iso(d)
    }()
    @State private var receivedOn: String = Self.isoToday()
    @State private var saleDate: String = Self.isoToday()
    @State private var periodYm: String = {
        let f = DateFormatter()
        f.dateFormat = "yyyy-MM"
        return f.string(from: Date())
    }()
    @State private var dispatchSlot = "Morning · 09:00"
    @State private var dispatchIso: String = Self.isoToday()
    @State private var producedOn: String = Self.isoToday()
    @State private var expectedReturn: String = {
        let d = Calendar.current.date(byAdding: .day, value: 7, to: Date()) ?? Date()
        return Self.iso(d)
    }()
    @State private var decideBy: String = {
        let d = Calendar.current.date(byAdding: .day, value: 2, to: Date()) ?? Date()
        return Self.iso(d)
    }()

    private enum DatePickerTarget {
        case sale, received, decide, produced, issueDue, confirmDue, plannedStart, openJobDue, creditNote
    }

    private let expensePayOptions = ["Paid", "Payable · 7 days", "Payable · 15 days", "Payable · 30 days"]

    private static func isoToday() -> String { iso(Date()) }
    private static func iso(_ date: Date) -> String {
        let f = DateFormatter()
        f.calendar = Calendar(identifier: .gregorian)
        f.locale = Locale(identifier: "en_US_POSIX")
        f.timeZone = TimeZone.current
        f.dateFormat = "yyyy-MM-dd"
        return f.string(from: date)
    }

    private var openAr: [OperatingPaymentPayload] {
        (model.projection?.paymentReadiness ?? []).filter { $0.relevance.uppercased() == "RECEIVABLE" }
    }

    private var teamPeople: [String] {
        (model.projection?.participants ?? []).filter { !$0.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty }
    }

    private var datePickerBinding: Binding<String> {
        switch datePickerTarget {
        case .sale: return $saleDate
        case .received: return $receivedOn
        case .issueDue: return $issueDue
        case .decide: return $decideBy
        case .produced: return $producedOn
        case .confirmDue: return $confirmDue
        case .plannedStart: return $plannedStart
        case .openJobDue: return $openJobDue
        case .creditNote: return $creditNoteDate
        }
    }

    private var upper: String { mode.uppercased() }

    private var sheetTitle: String {
        switch upper {
        case "SALE", "INVOICE": return "Invoice against order"
        case "PURCHASE", "MATERIALS": return "Purchase job materials"
        case "COLLECTION": return "Collection / advance"
        case "PAYMENT": return "Record Payment"
        case "SPEND", "EXPENSE": return "Record Expense · job link"
        case "RETURN": return "Return / Credit Note"
        case "STOCK": return "Stock update (job-linked)"
        case "BUDGET", "TARGET": return "Budget / Target Revision"
        case "UPDATE", "ISSUE": return "Post Update / Issue"
        case "APPROVAL", "REIMBURSE": return "Approval / Reimbursement"
        case "ORDER", "CUSTOMER_ORDER", "QUOTATION", "QUOTE": return "Create Quotation"
        case "CONFIRM", "ACCEPT", "CONFIRM_ORDER": return "Accept / Confirm Order"
        case "OPEN_JOB", "JOB", "OPENJOB": return "Open Production Job"
        case "QUOTE_REVISE", "REVISE": return "Quote revise / expire"
        case "JOB_STATUS", "STATUS": return "Job status change"
        case "DELIVERY", "SHIPMENT", "DISPATCH": return "Delivery / dispatch against job"
        case "PRODUCTION": return "Record Production"
        case "REWORK", "SCRAP": return "Rework / Scrap / Job Work"
        default: return upper.capitalized
        }
    }

    private var why: String {
        switch upper {
        case "SALE", "INVOICE":
            return "Only a posted invoice owns revenue, AR and output GST. Quotes, confirms, advances and dispatch never invent GST math here."
        case "PURCHASE", "MATERIALS":
            return "Job materials post job cost vs stock. AP opens from posted purchase — GST posts on GRN/invoice only."
        case "COLLECTION":
            return "Advance on an uninvoiced order is held as liability — never revenue. Against INV, collection allocates open AR only and never creates a new SALE."
        case "PAYMENT":
            return "Payment allocates against open AP only — it never creates a new PURCHASE."
        case "SPEND", "EXPENSE":
            return "Expense posts spend for the period. Link a job when cost should follow the job — GST only if a tax invoice is attached server-side."
        case "RETURN":
            return "Credit note adjusts the source invoice. Restock to Job FG is explicit — never silent overwrite."
        case "STOCK":
            return "Stock updates are explicit Set / Add / Reduce / Transfer movements — never inferred from sale or job save."
        case "BUDGET", "TARGET":
            return "Revisions are versioned. Confirmed orders may inform targets — quotes never count as invoiced basis."
        case "UPDATE", "ISSUE":
            return "Updates and issues are job signals. They do not post revenue, stock, or GST by themselves."
        case "APPROVAL", "REIMBURSE":
            return "Submit for approval — approval is not disbursement. Money moves only when a Payment is posted."
        case "ORDER", "CUSTOMER_ORDER", "QUOTATION", "QUOTE":
            return "A quotation creates no revenue, AR or GST. Tax shown is a preview — only a posted invoice owns AR/GST."
        case "CONFIRM", "ACCEPT", "CONFIRM_ORDER":
            return "Confirm creates an SO commitment only — no revenue, AR, stock, or GST until a posted invoice."
        case "OPEN_JOB", "JOB", "OPENJOB":
            return "Open job creates the job card and optional material reserve only — no output, stock consume, or sale until production / invoice."
        case "QUOTE_REVISE", "REVISE":
            return "Quote revise / extend / expire updates the quote version only — never revenue, AR, or GST."
        case "JOB_STATUS", "STATUS":
            return "Job status is a status event only — Active / Pending / Blocked / On hold never move stock, cost, or sale."
        case "DELIVERY", "SHIPMENT", "DISPATCH":
            return "Dispatch moves accepted job units against the source SO/J/INV only — never re-books invoice revenue or exceeds accepted qty. Confirm/POD is custody chrome — POD photo stays locked without a camera stack."
        case "PRODUCTION":
            return "Only accepted qty becomes sellable FG. Rejected and rework are distinct — never silent overwrite of finished stock."
        case "REWORK", "SCRAP":
            return "Rework, scrap, and job work out are distinct events. They never silently overwrite accepted finished goods."
        default:
            return "Job capture posts through existing operating events — no invented GST or park APIs."
        }
    }

    private var postedInvoices: [OperatingActivityPayload] {
        (model.projection?.activity ?? []).filter {
            ["SALE", "INVOICE"].contains($0.eventType.uppercased()) ||
                $0.title.localizedCaseInsensitiveContains("INV")
        }
    }

    private var openJobs: [OperatingActivityPayload] {
        (model.projection?.activity ?? []).filter {
            ["UPDATE", "ORDER", "CUSTOMER_ORDER", "PRODUCTION"].contains($0.eventType.uppercased())
        }
    }

    private var budgetPreviousLabel: String {
        let configuredTarget = model.projection?.target
        let configuredBudget = model.projection?.budget
        let raw: String?
        if kindChip == "Confirmed orders" {
            raw = (configuredTarget?.configured == true) ? configuredTarget?.value : nil
        } else {
            raw = (configuredBudget?.configured == true) ? configuredBudget?.amount
                : ((configuredTarget?.configured == true) ? configuredTarget?.value : nil)
        }
        guard let raw, !raw.isEmpty else { return "Unavailable · no prior revision" }
        return "₹\(raw) · v1"
    }

    private var canSave: Bool {
        switch upper {
        case "SALE", "INVOICE":
            return (Decimal(string: amount).map { $0 > 0 } ?? false) && !party.isEmpty
        case "PURCHASE", "MATERIALS":
            let amountOk = Decimal(string: amount).map { $0 > 0 } ?? false
            let jobOk = kindChip != "Job materials" || !jobLink.isEmpty
            return amountOk && !party.isEmpty && jobOk
        case "COLLECTION", "PAYMENT", "SPEND", "EXPENSE", "RETURN", "REIMBURSE":
            return Decimal(string: amount).map { $0 > 0 } ?? false
        case "PRODUCTION":
            return !product.isEmpty && (Decimal(string: qty).map { $0 > 0 } ?? false)
        case "STOCK", "REWORK", "SCRAP", "DELIVERY", "SHIPMENT", "DISPATCH":
            return !product.isEmpty || !titleText.isEmpty || !party.isEmpty
        case "ORDER", "CUSTOMER_ORDER", "QUOTATION", "QUOTE":
            return (Decimal(string: amount).map { $0 > 0 } ?? false) && !party.isEmpty
        case "CONFIRM", "ACCEPT", "CONFIRM_ORDER", "OPEN_JOB", "JOB", "OPENJOB", "QUOTE_REVISE", "REVISE":
            return !titleText.isEmpty || (Decimal(string: amount).map { $0 > 0 } ?? false)
        case "JOB_STATUS", "STATUS":
            return !product.isEmpty
        case "UPDATE", "ISSUE":
            return !titleText.isEmpty
        case "APPROVAL":
            return !party.isEmpty && !actionTitle.isEmpty
        case "BUDGET", "TARGET":
            return Decimal(string: amount).map { $0 > 0 } ?? false
        default:
            return true
        }
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                MtoGrabber()
                MtoSheetHeader(theme: theme, title: sheetTitle, onClose: { dismiss() })
                modeBody
                MtoWhyNote(theme: theme, text: why)
                MtoStickyFooter(
                    theme: theme,
                    title: saving ? "Posting…" : ctaLabel,
                    disabled: !canSave || saving,
                    loading: saving,
                    action: { Task { await save() } }
                )
            }
            .padding(16)
            .padding(.bottom, 24)
        }
        .background(theme.canvas)
        .onAppear {
            seedDefaults()
            applyBrowserPrefillIfNeeded()
        }
        .sheet(isPresented: $showDatePicker) {
            MtoDatePickerModal(theme: theme, isoDate: datePickerBinding, isPresented: $showDatePicker)
                .presentationDetents([.medium])
                .presentationBackground(Color.black.opacity(0.01))
        }
        .sheet(isPresented: $showMonthPicker) {
            MtoMonthPickerModal(theme: theme, yearMonth: $periodYm, isPresented: $showMonthPicker)
                .presentationDetents([.medium])
                .presentationBackground(Color.black.opacity(0.01))
        }
        .sheet(isPresented: $showDateTimePicker) {
            MtoDateTimePickerModal(
                theme: theme,
                isoDate: $dispatchIso,
                slot: $dispatchSlot,
                isPresented: $showDateTimePicker
            )
            .presentationDetents([.medium])
            .presentationBackground(Color.black.opacity(0.01))
        }
        .task { if model.masters.isEmpty { await model.loadMasters() } }
    }

    private var ctaLabel: String {
        switch upper {
        case "SALE", "INVOICE": return "Post invoice"
        case "PURCHASE", "MATERIALS": return "Post purchase"
        case "COLLECTION": return sourceInvoice.isEmpty ? "Hold advance" : "Post collection"
        case "PAYMENT": return "Post payment"
        case "SPEND", "EXPENSE": return "Post expense"
        case "RETURN": return "Post credit note"
        case "STOCK": return "Post stock update"
        case "BUDGET", "TARGET": return "Save revision"
        case "UPDATE", "ISSUE": return "Post update"
        case "APPROVAL", "REIMBURSE": return "Submit for approval"
        case "ORDER", "CUSTOMER_ORDER", "QUOTATION", "QUOTE": return "Send quotation"
        case "CONFIRM", "ACCEPT", "CONFIRM_ORDER": return "Confirm order"
        case "OPEN_JOB", "JOB", "OPENJOB": return "Open job"
        case "QUOTE_REVISE", "REVISE": return "Send revision"
        case "JOB_STATUS", "STATUS": return "Set status"
        case "DELIVERY", "SHIPMENT", "DISPATCH": return "Post dispatch"
        case "PRODUCTION": return "Post production"
        case "REWORK", "SCRAP": return "Post action"
        default: return "Post"
        }
    }

    @ViewBuilder
    private var modeBody: some View {
        switch upper {
        case "SALE", "INVOICE":
            // Figma chips: From order | New sale | Return | Reprint — no Itemized mode.
            MtoChipRow(theme: theme, options: ["From order", "New sale", "Return", "Reprint"], selection: $saleMode)
                .onChange(of: saleMode) { _, next in
                    if next == "Reprint" { onRouteUtility?("REPRINT") }
                }
            if saleMode == "Return" {
                moneyHero(label: "Credit amount")
            } else {
                MtoAmountHero(
                    theme: theme,
                    currency: model.currency.isEmpty ? "INR" : model.currency,
                    amount: $amount,
                    label: "Invoice gross",
                    caption: [
                        orderRef.isEmpty ? nil : "Order \(orderRef)",
                        party.isEmpty ? nil : "Customer \(party)",
                        "INV draft",
                    ].compactMap { $0 }.joined(separator: " · ")
                )
            }
            MtoUnavailableGstRow(theme: theme, label: "Output GST · CGST+SGST/IGST · Unavailable")
            MtoPickerRow(
                theme: theme,
                label: "Order",
                value: orderRef.isEmpty ? "Select SO / confirmed order" : orderRef
            ) {
                orderRef = orderRef.isEmpty ? "SO · confirmed" : orderRef
            }
            partyField(label: "Customer")
            MtoPickerRow(theme: theme, label: "Sale date", value: saleDate) {
                datePickerTarget = .sale
                showDatePicker = true
            }
            MtoDateQuickPicker(theme: theme, isoDate: $saleDate)
            MtoChipRow(theme: theme, options: ["Wholesale", "Retail counter", "Export", "Job work"], selection: $channel)
            MtoChipRow(theme: theme, options: ["Paid", "Partial", "Open"], selection: $payState)
            MtoPickerRow(theme: theme, label: "GST", value: "Unavailable · stamped on invoice")
        case "PURCHASE", "MATERIALS":
            MtoChipRow(theme: theme, options: ["Job materials", "General stock"], selection: bindingKind("Job materials"))
            moneyHero(label: "Purchase amount")
            partyField(label: "Supplier")
            MtoUnavailableGstRow(theme: theme, label: "Input GST · CGST+SGST")
            MtoChipRow(theme: theme, options: ["Receive now", "Receive later"], selection: $receiveNow)
            MtoChipRow(theme: theme, options: ["Pay later", "Pay now", "Part"], selection: $payState)
            MtoPickerRow(
                theme: theme,
                label: kindChip == "Job materials" ? "Job (required)" : "Link job",
                value: jobLink.isEmpty
                    ? (openJobs.isEmpty ? "Unavailable · no open job" : (kindChip == "Job materials" ? "Select job" : "Optional · select job"))
                    : jobLink
            ) {
                if let first = openJobs.first { jobLink = first.title }
            }
            ForEach(openJobs.prefix(4), id: \.id) { row in
                MtoPickerRow(theme: theme, label: row.title, value: row.status ?? "Job") { jobLink = row.title }
            }
        case "COLLECTION":
            moneyHero(label: "Collect now", caption: sourceInvoice.isEmpty ? nil : "Against \(sourceInvoice)")
            partyField(label: "Customer")
            MtoPickerRow(
                theme: theme,
                label: "Source invoice",
                value: sourceInvoice.isEmpty
                    ? (openAr.isEmpty ? "Unavailable · no open AR" : "Select open invoice")
                    : sourceInvoice
            ) {
                if let first = openAr.first {
                    sourceInvoice = first.title
                    if party.isEmpty { party = first.counterpartyName ?? party }
                }
            }
            ForEach(openAr.prefix(4), id: \.id) { row in
                MtoPickerRow(theme: theme, label: row.title, value: row.amount) {
                    sourceInvoice = row.title
                    party = row.counterpartyName ?? party
                }
            }
            MtoChipRow(theme: theme, options: ["UPI", "NEFT", "Cash", "Cheque"], selection: $channel)
            MtoFieldLabel(text: "Reference / UTR")
            MtoTextField(theme: theme, placeholder: "UTR…", text: $reference)
            MtoPickerRow(theme: theme, label: "Received on", value: receivedOn) {
                datePickerTarget = .received
                showDatePicker = true
            }
        case "PAYMENT", "REIMBURSE":
            if upper == "REIMBURSE" {
                MtoChipRow(theme: theme, options: ["Approval", "Reimbursement"], selection: bindingKind("Reimbursement"))
            }
            moneyHero(label: upper == "REIMBURSE" ? "Claim amount" : "Paid")
            partyField(label: "Payee")
            MtoChipRow(theme: theme, options: ["NEFT", "UPI", "Cash", "Cheque"], selection: $channel)
            MtoFieldLabel(text: "Reference / UTR")
            MtoTextField(theme: theme, placeholder: "UTR…", text: $reference)
        case "SPEND", "EXPENSE":
            MtoChipRow(theme: theme, options: ["Utilities", "Freight", "Consumables", "Job work", "Other"], selection: bindingKind("Utilities"))
            moneyHero(label: "Expense")
            partyField(label: "Payee")
            MtoPickerRow(
                theme: theme,
                label: "Job link",
                value: jobLink.isEmpty
                    ? (openJobs.isEmpty ? "Unavailable · no open job" : "Select job (optional)")
                    : jobLink
            ) {
                if let first = openJobs.first { jobLink = first.title }
            }
            ForEach(openJobs.prefix(4), id: \.id) { row in
                MtoPickerRow(theme: theme, label: row.title, value: row.status ?? "Job") { jobLink = row.title }
            }
            MtoPickerRow(theme: theme, label: "Payment state", value: expensePayState) {
                let i = expensePayOptions.firstIndex(of: expensePayState) ?? 0
                expensePayState = expensePayOptions[(i + 1) % expensePayOptions.count]
            }
            MtoPickerRow(theme: theme, label: "Period", value: MtoMonthPicker.label(periodYm)) {
                showMonthPicker = true
            }
            MtoMonthPicker(theme: theme, yearMonth: $periodYm)
        case "RETURN":
            MtoChipRow(theme: theme, options: ["Customer return", "Supplier return"], selection: bindingKind("Customer return"))
            moneyHero(
                label: "Credit amount",
                caption: sourceInvoice.isEmpty ? nil : "Against \(sourceInvoice)"
            )
            partyField(label: "Party")
            MtoPickerRow(
                theme: theme,
                label: "Source",
                value: sourceInvoice.isEmpty
                    ? (postedInvoices.isEmpty ? "Unavailable · no posted INV" : "Select source INV")
                    : sourceInvoice
            ) {
                if let first = postedInvoices.first {
                    sourceInvoice = first.title
                    if party.isEmpty { party = first.counterpartyName ?? party }
                }
            }
            ForEach(postedInvoices.prefix(4), id: \.id) { row in
                MtoPickerRow(theme: theme, label: row.title, value: row.amount ?? "INV") {
                    sourceInvoice = row.title
                    party = row.counterpartyName ?? party
                }
            }
            MtoChipRow(theme: theme, options: ["Restock to Job FG", "Scrap", "No stock"], selection: $channel)
            MtoFieldLabel(text: "Reason")
            MtoTextField(theme: theme, placeholder: "Reason", text: $returnReason)
            MtoPickerRow(theme: theme, label: "Credit note date", value: creditNoteDate) {
                datePickerTarget = .creditNote
                showDatePicker = true
            }
            MtoDateQuickPicker(theme: theme, isoDate: $creditNoteDate)
        case "STOCK":
            MtoChipRow(theme: theme, options: ["Set current", "Add", "Reduce", "Transfer"], selection: bindingKind("Set current"))
                .onChange(of: kindChip) { _, next in
                    if next == "Transfer" { onRouteUtility?("TRANSFER") }
                }
            MtoFieldLabel(text: "Product")
            MtoTextField(theme: theme, placeholder: "Product / part", text: $product)
            MtoFieldLabel(text: "Qty")
            MtoTextField(theme: theme, placeholder: "0", text: $qty, keyboard: .decimalPad)
            MtoFieldLabel(text: "Job tag")
            MtoTextField(theme: theme, placeholder: "J-07", text: $jobTag)
            MtoPickerRow(
                theme: theme,
                label: "Location (job-tagged)",
                value: jobTag.isEmpty ? "Job FG bay · J-tag" : "Job FG bay · \(jobTag)"
            )
            MtoChipRow(theme: theme, options: ["Cycle count", "Damage", "Correction", "Custody"], selection: $channel)
        case "BUDGET", "TARGET":
            MtoChipRow(theme: theme, options: ["Invoiced basis", "Confirmed orders"], selection: bindingKind(upper == "TARGET" ? "Confirmed orders" : "Invoiced basis"))
            moneyHero(
                label: "New target",
                caption: kindChip == "Invoiced basis" ? "Effective · invoiced only" : "Effective · confirmed orders"
            )
            MtoPickerRow(theme: theme, label: "Period", value: MtoMonthPicker.label(periodYm)) {
                showMonthPicker = true
            }
            MtoMonthPicker(theme: theme, yearMonth: $periodYm)
            MtoPickerRow(theme: theme, label: "Previous", value: budgetPreviousLabel)
            MtoFieldLabel(text: "Reason")
            MtoTextField(theme: theme, placeholder: "Reason", text: $budgetReason)
        case "UPDATE", "ISSUE":
            MtoChipRow(theme: theme, options: ["Update", "Issue", "Risk", "Approval"], selection: bindingKind(upper == "ISSUE" ? "Issue" : "Update"))
            MtoFieldLabel(text: "Title")
            MtoTextField(theme: theme, placeholder: "Title", text: $titleText)
            MtoChipRow(theme: theme, options: ["Low", "Medium", "High"], selection: $channel)
            MtoFieldLabel(text: "Owner")
            MtoTextField(theme: theme, placeholder: "Owner", text: $owner)
            ForEach(teamPeople.prefix(3), id: \.self) { person in
                MtoPickerRow(theme: theme, label: person, value: "Owner") { owner = person }
            }
            MtoFieldLabel(text: "Link · J / SO")
            MtoTextField(theme: theme, placeholder: "J-07 / SO", text: $issueLink)
            MtoPickerRow(theme: theme, label: "Due", value: issueDue) {
                datePickerTarget = .issueDue
                showDatePicker = true
            }
            MtoDateQuickPicker(theme: theme, isoDate: $issueDue)
        case "APPROVAL":
            MtoChipRow(theme: theme, options: ["Approval", "Reimbursement"], selection: bindingKind("Approval"))
            MtoFieldLabel(text: "Action")
            MtoTextField(theme: theme, placeholder: "Action", text: $actionTitle)
            MtoFieldLabel(text: "Requester")
            MtoTextField(theme: theme, placeholder: "Requester", text: $party)
            ForEach(teamPeople.prefix(3), id: \.self) { person in
                MtoPickerRow(theme: theme, label: person, value: "Team") { party = person }
            }
            MtoPickerRow(
                theme: theme,
                label: "Approver",
                value: approver.isEmpty ? (teamPeople.first { $0 != party } ?? "Select approver") : approver
            ) {
                approver = teamPeople.first { $0 != party } ?? approver
            }
            ForEach(teamPeople.filter { $0 != party }.prefix(3), id: \.self) { person in
                MtoPickerRow(theme: theme, label: person, value: "Approver") { approver = person }
            }
            MtoChipRow(theme: theme, options: ["Finance ≥ ₹1L", "Manager any", "Owner only"], selection: $policy)
            MtoPickerRow(theme: theme, label: "Decide by", value: decideBy) {
                datePickerTarget = .decide
                showDatePicker = true
            }
            MtoDateQuickPicker(theme: theme, isoDate: $decideBy)
        case "ORDER", "CUSTOMER_ORDER", "QUOTATION", "QUOTE":
            MtoChipRow(theme: theme, options: ["Quotation", "Revise", "Accept", "Cancel"], selection: bindingKind("Quotation"))
            moneyHero(label: "Quote value (ex tax)")
            partyField(label: "Customer")
            MtoChipRow(theme: theme, options: ["Draft → Sent", "Sent", "Accepted"], selection: $payState)
            MtoFieldLabel(text: "Lines")
            MtoTextField(theme: theme, placeholder: "Item × qty @ rate", text: $titleText)
            MtoChipRow(theme: theme, options: ["7 days", "15 days", "30 days"], selection: $channel)
            MtoUnavailableGstRow(theme: theme, label: "GST on quote", caption: "Preview only · Unavailable for posting")
        case "CONFIRM", "ACCEPT", "CONFIRM_ORDER":
            MtoChipRow(theme: theme, options: ["Quotation", "Revise", "Accept", "Cancel"], selection: bindingKind("Accept"))
            moneyHero(label: "Confirmed value", caption: "Commitment · not revenue")
            MtoFieldLabel(text: "Accepted quote")
            MtoTextField(theme: theme, placeholder: "Quote / SO", text: $titleText)
            MtoChipRow(theme: theme, options: ["Owner", "Manager", "Estimator"], selection: $channel)
            MtoFieldLabel(text: "Spec / revision")
            MtoTextField(theme: theme, placeholder: "Spec", text: $note)
            MtoPickerRow(theme: theme, label: "Due", value: confirmDue) {
                datePickerTarget = .confirmDue
                showDatePicker = true
            }
            MtoDateQuickPicker(theme: theme, isoDate: $confirmDue)
        case "OPEN_JOB", "JOB", "OPENJOB":
            MtoChipRow(theme: theme, options: ["From order", "Job", "BOM", "Hold"], selection: bindingKind("From order"))
                .onChange(of: kindChip) { _, next in
                    if next == "Hold" { onRouteUtility?("HOLD") }
                }
            MtoFieldLabel(text: "Confirmed order")
            MtoTextField(theme: theme, placeholder: "SO / confirmed order", text: $titleText)
            MtoFieldLabel(text: "Job ID")
            MtoTextField(theme: theme, placeholder: "J-07", text: $jobTag)
            MtoFieldLabel(text: "Job target / finished part")
            MtoTextField(theme: theme, placeholder: "Finished part · qty", text: $product)
            MtoPickerRow(theme: theme, label: "Planned start", value: plannedStart) {
                datePickerTarget = .plannedStart
                showDatePicker = true
            }
            MtoDateQuickPicker(theme: theme, isoDate: $plannedStart)
            MtoFieldLabel(text: "BOM revision")
            MtoChipRow(theme: theme, options: ["r1", "r2", "r3"], selection: $bomRev)
            MtoChipRow(theme: theme, options: ["Reserve material", "No reserve"], selection: $materials)
            MtoPickerRow(theme: theme, label: "Due", value: openJobDue) {
                datePickerTarget = .openJobDue
                showDatePicker = true
            }
            MtoDateQuickPicker(theme: theme, isoDate: $openJobDue)
        case "QUOTE_REVISE", "REVISE":
            MtoChipRow(theme: theme, options: ["Q-id", "Revise", "Extend", "Expire"], selection: bindingKind("Revise"))
            moneyHero(label: "Versioned value")
            MtoFieldLabel(text: "Quote")
            MtoTextField(theme: theme, placeholder: "Q-id", text: $titleText)
            MtoChipRow(theme: theme, options: ["v2", "v3", "v4"], selection: $channel)
        case "JOB_STATUS", "STATUS":
            MtoChipRow(theme: theme, options: ["Active", "Pending", "Blocked", "On hold"], selection: bindingKind("Active"))
            MtoFieldLabel(text: "Job")
            MtoTextField(theme: theme, placeholder: "Job", text: $product)
            MtoFieldLabel(text: "Reason")
            MtoTextField(theme: theme, placeholder: "Reason", text: $note)
            MtoChipRow(theme: theme, options: ["None", "Delay", "Notify"], selection: $channel)
        case "DELIVERY", "SHIPMENT", "DISPATCH":
            MtoChipRow(theme: theme, options: ["Material in", "Dispatch to buyer"], selection: bindingKind("Dispatch to buyer"))
            MtoFieldLabel(text: "Job")
            MtoTextField(theme: theme, placeholder: "J-07 · finished part", text: $jobLink)
            MtoFieldLabel(text: "Source SO / J / INV")
            MtoTextField(theme: theme, placeholder: "Source", text: $product)
            MtoFieldLabel(text: "Accepted remaining")
            MtoTextField(theme: theme, placeholder: "12", text: $acceptedRemaining, keyboard: .decimalPad)
            MtoFieldLabel(text: "Qty · capped at accepted")
            MtoTextField(theme: theme, placeholder: "0", text: $qty, keyboard: .decimalPad)
            MtoPickerRow(
                theme: theme,
                label: "Qty remaining against accepted",
                value: {
                    let rem = Decimal(string: acceptedRemaining) ?? 0
                    let take = Decimal(string: qty) ?? 0
                    let left = rem > take ? rem - take : 0
                    return acceptedRemaining.isEmpty
                        ? "Unavailable · no accepted qty"
                        : "\(left) of \(acceptedRemaining) remaining · against accepted"
                }()
            )
            MtoChipRow(theme: theme, options: ["Pending", "Confirmed", "POD photo"], selection: $confirmPod)
                .onChange(of: confirmPod) { _, next in
                    if next == "POD photo" { confirmPod = "Confirmed" }
                }
            MtoLockedChip(theme: theme, label: "POD photo · Unavailable")
            MtoPickerRow(theme: theme, label: "Dispatch on", value: "\(dispatchIso) · \(dispatchSlot)") {
                showDateTimePicker = true
            }
            MtoDateTimeSlots(theme: theme, slot: $dispatchSlot)
        case "PRODUCTION":
            MtoChipRow(theme: theme, options: ["Against job", "Batch", "Rework", "WIP move"], selection: bindingKind("Against job"))
                .onChange(of: kindChip) { _, next in
                    if next == "WIP move" { onRouteUtility?("TRANSFER") }
                }
            MtoFieldLabel(text: "Finished product")
            MtoTextField(theme: theme, placeholder: "Finished product", text: $product)
            MtoFieldLabel(text: "Accepted qty")
            MtoTextField(theme: theme, placeholder: "0", text: $qty, keyboard: .decimalPad)
            MtoFieldLabel(text: "Rejected qty")
            MtoTextField(theme: theme, placeholder: "0", text: $rejected, keyboard: .decimalPad)
            MtoFieldLabel(text: "Scrap reason")
            MtoTextField(theme: theme, placeholder: "Scrap reason", text: $scrapReason)
            MtoChipRow(theme: theme, options: ["Explicit consumption", "Backflush", "None"], selection: $materials)
            MtoPickerRow(theme: theme, label: "Produced on", value: producedOn) {
                datePickerTarget = .produced
                showDatePicker = true
            }
            MtoDateQuickPicker(theme: theme, isoDate: $producedOn)
        case "REWORK":
            MtoChipRow(theme: theme, options: ["Rework", "Scrap", "Job work out"], selection: bindingKind("Rework"))
            MtoFieldLabel(text: "Source batch / WO")
            MtoTextField(theme: theme, placeholder: "Source", text: $product)
            MtoFieldLabel(text: "Qty")
            MtoTextField(theme: theme, placeholder: "0", text: $qty, keyboard: .decimalPad)
            if kindChip == "Job work out" {
                partyField(label: "Vendor")
                MtoLockedChip(theme: theme, label: "Challan · Unavailable")
                MtoPickerRow(theme: theme, label: "Expected return", value: expectedReturn)
                MtoDateQuickPicker(theme: theme, isoDate: $expectedReturn)
            }
        default:
            moneyHero(label: "Amount")
            partyField(label: "Party")
        }

        MtoMoreDetails(title: "More details") {
            MtoFieldLabel(text: "Note")
            MtoTextField(theme: theme, placeholder: "Notes · tags", text: $note)
        }
    }

    private func moneyHero(label: String, caption: String? = nil) -> some View {
        MtoAmountHero(
            theme: theme,
            currency: model.currency.isEmpty ? "INR" : model.currency,
            amount: $amount,
            label: label,
            caption: caption
        )
    }

    private func partyField(label: String) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            MtoFieldLabel(text: label)
            MtoTextField(theme: theme, placeholder: label, text: $party)
        }
    }

    private func bindingKind(_ fallback: String) -> Binding<String> {
        Binding(
            get: { kindChip.isEmpty ? fallback : kindChip },
            set: { kindChip = $0 }
        )
    }

    private func seedDefaults() {
        if kindChip.isEmpty {
            switch upper {
            case "PURCHASE", "MATERIALS": kindChip = "Job materials"
            case "STOCK": kindChip = "Set current"
            case "ORDER", "CUSTOMER_ORDER", "QUOTATION", "QUOTE": kindChip = "Quotation"
            case "PRODUCTION": kindChip = "Against job"
            case "REWORK": kindChip = "Rework"
            case "SPEND", "EXPENSE": kindChip = "Utilities"
            case "RETURN":
                kindChip = "Customer return"
                if channel == "Wholesale" { channel = "Restock to Job FG" }
            case "DELIVERY", "SHIPMENT", "DISPATCH": kindChip = "Dispatch to buyer"
            case "UPDATE": kindChip = "Update"
            case "ISSUE": kindChip = "Issue"
            case "APPROVAL", "REIMBURSE": kindChip = upper == "REIMBURSE" ? "Reimbursement" : "Approval"
            case "BUDGET": kindChip = "Invoiced basis"
            case "TARGET": kindChip = "Confirmed orders"
            default: break
            }
        }
    }

    private func save() async {
        saving = true
        defer { saving = false }
        var contract = OperatingContractInput()
        contract.taxMode = "UNAVAILABLE"
        let eventType: String
        let title: String
        var quantity: String? = nil
        var rejectedQuantity: String? = nil
        var itemName: String? = nil
        var paymentStatus: String? = nil
        var amountOut: String? = amount.isEmpty ? nil : amount

        switch upper {
        case "SALE", "INVOICE":
            eventType = saleMode == "Return" ? "RETURN" : "SALE"
            title = "\(eventType == "RETURN" ? "Credit" : "Sale") · \(party.isEmpty ? "Walk-in" : party)"
            paymentStatus = payState == "Paid" ? "PAID" : payState == "Partial" ? "PARTIAL" : "OPEN"
            contract.entryMode = "QUICK_AMOUNT"
            contract.paymentState = payState.uppercased()
            contract.taxMode = "UNAVAILABLE"
            if !orderRef.isEmpty { itemName = orderRef }
        case "PURCHASE", "MATERIALS":
            eventType = "PURCHASE"
            title = "Purchase · \(party.isEmpty ? "Vendor" : party)"
            paymentStatus = payState.contains("now") || payState == "Pay now" ? "PAID" : payState == "Part" ? "PARTIAL" : "OPEN"
            contract.receiptState = receiveNow == "Receive now" ? "RECEIVED" : "PENDING"
            contract.paymentState = payState.uppercased()
            contract.taxMode = "UNAVAILABLE"
        case "COLLECTION":
            eventType = "COLLECTION"
            title = "Collection · \(party)"
            paymentStatus = "PENDING"
        case "PAYMENT", "REIMBURSE":
            eventType = "PAYMENT"
            title = upper == "REIMBURSE" ? "Reimburse · \(party)" : "Payment · \(party)"
            paymentStatus = "PENDING"
        case "SPEND", "EXPENSE":
            eventType = "SPEND"
            title = "Expense · \(kindChip)"
            contract.expenseCategory = kindChip
            contract.paymentState = expensePayState
            paymentStatus = expensePayState == "Paid" ? "PAID" : "OPEN"
        case "RETURN":
            eventType = "RETURN"
            title = "Return · \(party)"
            contract.stockAction = channel == "Restock to Job FG"
                ? "RESTOCK_JOB_FG"
                : channel.uppercased().replacingOccurrences(of: " ", with: "_")
        case "STOCK":
            eventType = "STOCK"
            title = "Stock · \(kindChip) · \(product)"
            quantity = qty
            itemName = product
            amountOut = nil
            contract.stockAction = kindChip.uppercased().replacingOccurrences(of: " ", with: "_")
            contract.stockReason = channel
        case "BUDGET", "TARGET":
            eventType = kindChip == "Confirmed orders" || upper == "TARGET" ? "TARGET" : "BUDGET"
            title = "Revision · \(kindChip)"
        case "CONFIRM", "ACCEPT", "CONFIRM_ORDER":
            eventType = "CUSTOMER_ORDER"
            title = "Confirm · \(titleText.isEmpty ? "order" : titleText)"
            contract.orderStatus = "CONFIRMED"
            contract.entryMode = "CONFIRM"
        case "OPEN_JOB", "JOB", "OPENJOB":
            eventType = "UPDATE"
            title = "Open job · \(jobTag.isEmpty ? product : jobTag)"
            itemName = product.isEmpty ? nil : product
            contract.orderStatus = "OPEN_JOB"
            contract.entryMode = kindChip.uppercased().replacingOccurrences(of: " ", with: "_")
        case "UPDATE", "ISSUE":
            eventType = (kindChip == "Issue" || kindChip == "Risk") ? "ISSUE" : "UPDATE"
            title = titleText
            itemName = issueLink.isEmpty ? nil : issueLink
        case "APPROVAL":
            eventType = "UPDATE"
            title = "\(kindChip) · \(actionTitle.isEmpty ? party : actionTitle)"
        case "ORDER", "CUSTOMER_ORDER", "QUOTATION", "QUOTE":
            eventType = "ORDER"
            title = titleText
            amountOut = nil
            itemName = prefilledPart
            quantity = prefilledPart == nil ? nil : "1"
            contract.orderStatus = "OPEN"
            contract.entryMode = kindChip.uppercased().replacingOccurrences(of: " ", with: "_")
        case "DELIVERY", "SHIPMENT", "DISPATCH":
            eventType = "DELIVERY"
            title = "Delivery · \(jobLink.isEmpty ? product : jobLink)"
            quantity = qty
            itemName = product
            amountOut = nil
        case "PRODUCTION":
            eventType = "PRODUCTION"
            title = "Production · \(product)"
            quantity = qty
            rejectedQuantity = rejected
            itemName = product
            amountOut = nil
            contract.productionStatus = kindChip.uppercased().replacingOccurrences(of: " ", with: "_")
            contract.acceptedQuantity = qty
            if (Decimal(string: rejected) ?? 0) > 0 {
                contract.scrapQuantity = rejected
            }
        case "REWORK":
            eventType = "STOCK"
            title = "\(kindChip) · \(product)"
            quantity = qty
            itemName = product
            amountOut = nil
            contract.stockAction = kindChip.uppercased().replacingOccurrences(of: " ", with: "_")
        default:
            eventType = upper
            title = sheetTitle
        }

        var composedNote = note.isEmpty ? kindChip : "\(kindChip) · \(note)"
        switch upper {
        case "SALE", "INVOICE":
            composedNote = [
                orderRef.isEmpty ? nil : "order=\(orderRef)",
                note.isEmpty ? nil : note,
            ].compactMap { $0 }.joined(separator: " · ")
        case "PURCHASE", "MATERIALS":
            composedNote = [
                kindChip.isEmpty ? nil : kindChip,
                jobLink.isEmpty ? nil : "job=\(jobLink)",
                "receive=\(receiveNow)",
                note.isEmpty ? nil : note,
            ].compactMap { $0 }.joined(separator: " · ")
        case "COLLECTION":
            composedNote = [
                sourceInvoice.isEmpty ? nil : sourceInvoice,
                channel.isEmpty ? nil : channel,
                reference.isEmpty ? nil : "ref=\(reference)",
                note.isEmpty ? nil : note,
            ].compactMap { $0 }.joined(separator: " · ")
        case "PAYMENT", "REIMBURSE":
            composedNote = [
                channel.isEmpty ? nil : channel,
                reference.isEmpty ? nil : "ref=\(reference)",
                note.isEmpty ? nil : note,
            ].compactMap { $0 }.joined(separator: " · ")
        case "ORDER", "CUSTOMER_ORDER", "QUOTATION", "QUOTE":
            composedNote = [
                "commitment only · \(kindChip.isEmpty ? "Work order" : kindChip)",
                prefilledPart.map { "part=\($0)" },
                note.isEmpty ? nil : note,
            ].compactMap { $0 }.joined(separator: " · ")
        case "SPEND", "EXPENSE":
            composedNote = [
                "period=\(periodYm)",
                jobLink.isEmpty ? nil : "job=\(jobLink)",
                "pay=\(expensePayState)",
                note.isEmpty ? nil : note,
            ].compactMap { $0 }.joined(separator: " · ")
        case "RETURN":
            composedNote = [
                kindChip,
                channel,
                sourceInvoice.isEmpty ? nil : "source=\(sourceInvoice)",
                returnReason.isEmpty ? nil : "reason=\(returnReason)",
                note.isEmpty ? nil : note,
            ].compactMap { $0 }.joined(separator: " · ")
        case "BUDGET", "TARGET":
            composedNote = [
                "basis=\(kindChip)",
                "period=\(periodYm)",
                budgetReason.isEmpty ? nil : "reason=\(budgetReason)",
                note.isEmpty ? nil : note,
            ].compactMap { $0 }.joined(separator: " · ")
        case "CONFIRM", "ACCEPT", "CONFIRM_ORDER":
            composedNote = "CONFIRM · commitment only · approval=\(channel) · no revenue"
        case "OPEN_JOB", "JOB", "OPENJOB":
            composedNote = "OPEN_JOB · \(kindChip) · order=\(titleText) · bom=\(bomRev) · \(materials) · no output/stock/sale"
        case "STOCK":
            composedNote = [
                channel.isEmpty ? nil : channel,
                jobTag.isEmpty ? nil : "J-tag=\(jobTag)",
                note.isEmpty ? nil : note,
            ].compactMap { $0 }.joined(separator: " · ")
        case "DELIVERY", "SHIPMENT", "DISPATCH":
            composedNote = [
                kindChip,
                "confirm=\(confirmPod)",
                "acceptedRemaining=\(acceptedRemaining)",
                "\(dispatchIso) · \(dispatchSlot)",
                note.isEmpty ? nil : note,
            ].compactMap { $0 }.joined(separator: " · ")
        case "UPDATE", "ISSUE":
            composedNote = [
                "severity=\(channel)",
                owner.isEmpty ? nil : "owner=\(owner)",
                issueLink.isEmpty ? nil : "link=\(issueLink)",
                note.isEmpty ? nil : note,
            ].compactMap { $0 }.joined(separator: " · ")
        case "APPROVAL":
            composedNote = [
                "APPROVAL_REQUEST",
                "approver=\(approver.isEmpty ? "Unset" : approver)",
                "policy=\(policy)",
                "decideBy=\(decideBy)",
                note.isEmpty ? nil : note,
            ].compactMap { $0 }.joined(separator: " · ")
        case "PRODUCTION":
            composedNote = [
                "mode=\(kindChip)",
                "materials=\(materials)",
                scrapReason.isEmpty ? nil : "scrap=\(scrapReason)",
                note.isEmpty ? nil : note,
            ].compactMap { $0 }.joined(separator: " · ")
        case "REWORK" where kindChip == "Job work out":
            composedNote = "\(composedNote) · return=\(expectedReturn)"
        default:
            break
        }
        let occurred: String? = {
            switch upper {
            case "SALE", "INVOICE": return saleDate
            case "RETURN": return creditNoteDate
            case "COLLECTION": return receivedOn
            case "PRODUCTION": return producedOn
            case "OPEN_JOB", "JOB", "OPENJOB": return plannedStart
            case "DELIVERY", "SHIPMENT", "DISPATCH": return dispatchIso
            default: return nil
            }
        }()
        let dueAt: String? = {
            switch upper {
            case "APPROVAL": return decideBy
            case "UPDATE", "ISSUE": return issueDue
            case "CONFIRM", "ACCEPT", "CONFIRM_ORDER": return confirmDue
            case "OPEN_JOB", "JOB", "OPENJOB": return openJobDue
            case "DELIVERY", "SHIPMENT", "DISPATCH": return dispatchIso
            case "REWORK" where kindChip == "Job work out": return expectedReturn
            default: return nil
            }
        }()
        let postedLines: [OperatingEventInput.OperatingLineItem]? = {
            if upper == "ORDER" || upper == "CUSTOMER_ORDER" || upper == "QUOTATION" || upper == "QUOTE", let part = prefilledPart {
                return [
                    .init(
                        itemName: part,
                        quantity: "1",
                        unit: prefilledUnit ?? "nos",
                        masterId: prefilledMasterId
                    ),
                ]
            }
            return itemName.map { [.init(itemName: $0, quantity: quantity, unit: "nos")] }
        }()
        let ok = await model.record(OperatingEventInput(
            momentId: model.momentId,
            eventType: eventType,
            title: title,
            amount: amountOut,
            note: composedNote,
            channelName: {
                switch upper {
                case "APPROVAL": return policy
                case "PRODUCTION": return materials
                case "PURCHASE", "MATERIALS", "SPEND", "EXPENSE":
                    return jobLink.isEmpty ? nil : jobLink
                case "RETURN":
                    return sourceInvoice.isEmpty ? nil : sourceInvoice
                case "COLLECTION", "PAYMENT", "REIMBURSE", "SALE", "INVOICE":
                    return channel.isEmpty ? nil : channel
                case "OPEN_JOB", "JOB", "OPENJOB":
                    return materials
                default: return channel.isEmpty ? nil : channel
                }
            }(),
            counterpartyName: party.isEmpty ? nil : party,
            occurredAt: occurred,
            quantity: quantity,
            unit: quantity == nil ? nil : (prefilledUnit ?? "nos"),
            dueAt: dueAt,
            masterId: prefilledMasterId,
            lineItems: postedLines,
            contract: contract,
            paymentStatus: paymentStatus,
            rejectedQuantity: rejectedQuantity
        ))
        if ok {
            let entity = title.isEmpty
                ? (party.isEmpty ? model.title : party)
                : title
            MtoToastHost.shared.show(
                MtoToastCatalog.forCaptureSuccess(
                    mode: mode,
                    eventType: eventType,
                    entity: entity.isEmpty ? "this job" : entity
                )
            )
            dismiss()
        }
    }

    private func applyBrowserPrefillIfNeeded() {
        guard !didApplyBrowserPrefill, let item = browserPrefill else { return }
        didApplyBrowserPrefill = true
        prefilledPart = item.name
        prefilledMasterId = item.id
        prefilledUnit = item.unit
        if titleText.isEmpty { titleText = item.name }
        if upper == "ORDER" || upper == "CUSTOMER_ORDER" {
            if kindChip.isEmpty || kindChip == "Work order" {
                kindChip = "Sales order"
            }
        }
        if product.isEmpty { product = item.name }
    }
}

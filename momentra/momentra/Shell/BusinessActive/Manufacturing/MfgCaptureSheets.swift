import SwiftUI

/// MFG money / ops / production captures — Figma MFG-QA-01…14 with amber chrome.
struct MfgCaptureSheet: View {
    let theme: OperatingTheme
    let mode: String
    @ObservedObject var model: BusinessOperatingModel
    var browserPrefill: OperatingBrowserItem? = nil
    var onRouteUtility: ((String) -> Void)? = nil

    var body: some View {
        MfgCaptureForm(
            theme: theme,
            mode: mode,
            model: model,
            browserPrefill: browserPrefill,
            onRouteUtility: onRouteUtility
        )
    }
}

private struct MfgCaptureForm: View {
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
    @State private var saleMode = "Quick amount"
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
    @State private var itemizedLines: [(name: String, price: String)] = []
    @State private var sourceInvoice = ""
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

    private enum DatePickerTarget { case sale, received, decide, produced }

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
        case .decide: return $decideBy
        case .produced: return $producedOn
        }
    }

    private var upper: String { mode.uppercased() }

    private var sheetTitle: String {
        switch upper {
        case "SALE": return "Record Sale"
        case "PURCHASE": return "Record Purchase"
        case "COLLECTION": return "Record Collection"
        case "PAYMENT": return "Record Payment"
        case "SPEND": return "Record Expense"
        case "RETURN": return "Return / Credit Note"
        case "STOCK": return "Stock Update"
        case "BUDGET", "TARGET": return "Budget / Target Revision"
        case "UPDATE", "ISSUE": return "Post Update / Issue"
        case "APPROVAL", "REIMBURSE": return "Approval / Reimbursement"
        case "ORDER", "CUSTOMER_ORDER": return "Order"
        case "DELIVERY", "SHIPMENT": return "Delivery"
        case "PRODUCTION": return "Record Production"
        case "REWORK": return "Rework / Scrap / Job Work"
        default: return upper.capitalized
        }
    }

    private var why: String {
        switch upper {
        case "SALE":
            return "Posted invoice owns revenue and output GST once — CGST+SGST in MH, IGST when place of supply is another state. Collection later allocates."
        case "PURCHASE":
            return "AP opens from posted purchase. Stock += accepted qty on receipt. Payment allocates later — never double-cost. GST posts on GRN/invoice only."
        case "COLLECTION":
            return "Collection allocates against open invoices only — it never creates a new SALE or revenue line."
        case "PAYMENT":
            return "Payment allocates against open AP only — it never creates a new PURCHASE."
        case "SPEND":
            return "Expense posts spend for the period. Link a job when cost should follow the WO — GST only if a tax invoice is attached server-side."
        case "RETURN":
            return "Credit note adjusts the source invoice. Restock is explicit — never silent FG overwrite."
        case "STOCK":
            return "Stock updates are explicit Set / Add / Reduce / Transfer movements — never inferred from sale or WO save."
        case "BUDGET", "TARGET":
            return "Revisions are versioned and effective-dated. They do not rewrite posted production or sales history."
        case "UPDATE", "ISSUE":
            return "Updates and issues are plant signals. They do not post revenue, stock, or GST by themselves."
        case "APPROVAL", "REIMBURSE":
            return "Submit for approval — approval is not disbursement. Money moves only when a Payment is posted."
        case "ORDER", "CUSTOMER_ORDER":
            return "Orders are commitments only — SO / PO / WO / Delivery promises. Revenue, stock, and GST post on Sale, receipt, or accepted production."
        case "DELIVERY", "SHIPMENT":
            return "Delivery confirms movement against a commitment. It does not create a duplicate invoice or revenue."
        case "PRODUCTION":
            return "Only accepted qty becomes sellable FG. Rejected and rework are distinct — never silent overwrite of finished stock."
        case "REWORK":
            return "Rework, scrap, and job work out are distinct events. They never silently overwrite accepted finished goods."
        default:
            return "Plant capture posts through existing operating events — no invented GST or park APIs."
        }
    }

    private var finishedParts: [OperatingMasterPayload] {
        let source = model.masters.isEmpty ? (model.projection?.masters ?? []) : model.masters
        return source.filter { ["SKU", "PART", "PRODUCT"].contains($0.kind.uppercased()) }
    }

    private var itemizedTotal: Decimal {
        itemizedLines.reduce(Decimal.zero) { partial, row in
            partial + (Decimal(string: row.price) ?? 0)
        }
    }

    private var canSave: Bool {
        switch upper {
        case "SALE":
            if saleMode == "Itemized" {
                return !itemizedLines.isEmpty && !party.isEmpty
            }
            return Decimal(string: amount).map { $0 > 0 } ?? false
        case "PURCHASE", "COLLECTION", "PAYMENT", "SPEND", "RETURN", "REIMBURSE":
            return Decimal(string: amount).map { $0 > 0 } ?? false
        case "PRODUCTION":
            return !product.isEmpty && (Decimal(string: qty).map { $0 > 0 } ?? false)
        case "STOCK", "REWORK", "DELIVERY", "SHIPMENT":
            return !product.isEmpty || !titleText.isEmpty || !party.isEmpty
        case "ORDER", "CUSTOMER_ORDER":
            return !titleText.isEmpty
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
                MfgGrabber()
                MfgSheetHeader(theme: theme, title: sheetTitle, onClose: { dismiss() })
                modeBody
                MfgWhyBox(theme: theme, text: why)
                MfgStickyFooter(
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
            MfgDatePickerModal(theme: theme, isoDate: datePickerBinding, isPresented: $showDatePicker)
                .presentationDetents([.medium])
                .presentationBackground(Color.black.opacity(0.01))
        }
        .sheet(isPresented: $showMonthPicker) {
            MfgMonthPickerModal(theme: theme, yearMonth: $periodYm, isPresented: $showMonthPicker)
                .presentationDetents([.medium])
                .presentationBackground(Color.black.opacity(0.01))
        }
        .sheet(isPresented: $showDateTimePicker) {
            MfgDateTimePickerModal(
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
        case "SALE": return "Post sale"
        case "PURCHASE": return "Post purchase"
        case "COLLECTION": return "Post collection"
        case "PAYMENT": return "Post payment"
        case "SPEND": return "Post expense"
        case "RETURN": return "Post credit note"
        case "STOCK": return "Post stock update"
        case "BUDGET", "TARGET": return "Save revision"
        case "UPDATE", "ISSUE": return "Post update"
        case "APPROVAL", "REIMBURSE": return "Submit for approval"
        case "ORDER", "CUSTOMER_ORDER": return "Save order"
        case "DELIVERY", "SHIPMENT": return "Post delivery"
        case "PRODUCTION": return "Post production"
        case "REWORK": return "Post action"
        default: return "Post"
        }
    }

    @ViewBuilder
    private var modeBody: some View {
        switch upper {
        case "SALE":
            MfgChipRow(theme: theme, options: ["Quick amount", "Itemized", "Return", "Reprint"], selection: $saleMode)
                .onChange(of: saleMode) { _, next in
                    if next == "Reprint" { onRouteUtility?("REPRINT") }
                }
            if saleMode == "Itemized" {
                MfgAmountHero(
                    theme: theme,
                    label: "Itemized total",
                    amountText: itemizedLines.isEmpty ? "—" : "\(itemizedTotal)",
                    caption: itemizedLines.isEmpty
                        ? "Add finished parts below · GST Unavailable"
                        : "\(itemizedLines.count) lines · GST Unavailable"
                )
                MfgLinesCard(theme: theme, title: "Lines", trailing: "\(itemizedLines.count)") {
                    if itemizedLines.isEmpty {
                        Text(finishedParts.isEmpty
                             ? "Parts browser empty — Itemized stays locked until a finished part exists."
                             : "Tap a finished part below to add a real line. No pretend rows.")
                            .font(.plusJakarta(size: 12))
                            .foregroundStyle(MfgSheetChrome.textBody)
                    } else {
                        ForEach(Array(itemizedLines.enumerated()), id: \.offset) { idx, row in
                            MfgPickerRow(theme: theme, label: row.name, value: row.price) {
                                itemizedLines.remove(at: idx)
                            }
                        }
                    }
                    ForEach(finishedParts.prefix(8), id: \.id) { p in
                        let price = (p.sellPrice?.isEmpty == false) ? (p.sellPrice ?? "0") : "0"
                        MfgPickerRow(
                            theme: theme,
                            label: "+ \(p.name)",
                            value: [
                                (p.code?.isEmpty == false) ? p.code : nil,
                                (p.unit?.isEmpty == false) ? p.unit : nil,
                                price,
                            ]
                                .compactMap { $0 }
                                .joined(separator: " · ")
                        ) {
                            if !itemizedLines.contains(where: { $0.name == p.name }) {
                                itemizedLines.append((p.name, price))
                                amount = "\(itemizedTotal + (Decimal(string: price) ?? 0))"
                            }
                        }
                    }
                    if finishedParts.isEmpty {
                        MfgLockedChip(theme: theme, label: "Itemized locked · no parts in catalogue")
                    }
                }
            } else {
                MfgAmountHero(theme: theme, currency: model.currency.isEmpty ? "INR" : model.currency, amount: $amount, label: "Sale amount", caption: party.isEmpty ? "INV draft" : "Customer \(party) · INV draft")
            }
            partyField(label: "Customer")
            MfgPickerRow(theme: theme, label: "Sale date", value: saleDate) {
                datePickerTarget = .sale
                showDatePicker = true
            }
            MfgDateQuickPicker(theme: theme, isoDate: $saleDate)
            MfgChipRow(theme: theme, options: ["Wholesale", "Retail counter", "Export", "Job work"], selection: $channel)
            MfgChipRow(theme: theme, options: ["Paid", "Partial", "Open"], selection: $payState)
            MfgPickerRow(theme: theme, label: "GST", value: "Unavailable · stamped on invoice")
            MfgUnavailableGstRow(theme: theme, label: "Output GST · CGST+SGST/IGST")
        case "PURCHASE":
            MfgChipRow(theme: theme, options: ["Materials", "Consumables"], selection: bindingKind("Materials"))
            moneyHero(label: "Purchase amount")
            partyField(label: "Supplier")
            MfgUnavailableGstRow(theme: theme, label: "Input GST · CGST+SGST")
            MfgChipRow(theme: theme, options: ["Pay later", "Pay now", "Part"], selection: $payState)
        case "COLLECTION":
            moneyHero(label: "Collect now", caption: sourceInvoice.isEmpty ? nil : "Against \(sourceInvoice)")
            partyField(label: "Customer")
            MfgPickerRow(
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
                MfgPickerRow(theme: theme, label: row.title, value: row.amount) {
                    sourceInvoice = row.title
                    party = row.counterpartyName ?? party
                }
            }
            MfgChipRow(theme: theme, options: ["UPI", "NEFT", "Cash", "Cheque"], selection: $channel)
            MfgFieldLabel(text: "Reference / UTR")
            MfgTextField(theme: theme, placeholder: "UTR…", text: $reference)
            MfgPickerRow(theme: theme, label: "Received on", value: receivedOn) {
                datePickerTarget = .received
                showDatePicker = true
            }
        case "PAYMENT", "REIMBURSE":
            if upper == "REIMBURSE" {
                MfgChipRow(theme: theme, options: ["Approval", "Reimbursement"], selection: bindingKind("Reimbursement"))
            }
            moneyHero(label: upper == "REIMBURSE" ? "Claim amount" : "Paid")
            partyField(label: "Payee")
            MfgChipRow(theme: theme, options: ["NEFT", "UPI", "Cash", "Cheque"], selection: $channel)
            MfgFieldLabel(text: "Reference / UTR")
            MfgTextField(theme: theme, placeholder: "UTR…", text: $reference)
        case "SPEND":
            MfgChipRow(theme: theme, options: ["Utilities", "Freight", "Consumables", "Job work", "Other"], selection: bindingKind("Utilities"))
            moneyHero(label: "Expense")
            partyField(label: "Payee")
            MfgPickerRow(theme: theme, label: "Period", value: MfgMonthPicker.label(periodYm)) {
                showMonthPicker = true
            }
            MfgMonthPicker(theme: theme, yearMonth: $periodYm)
        case "RETURN":
            MfgChipRow(theme: theme, options: ["Customer return", "Supplier return"], selection: bindingKind("Customer return"))
            moneyHero(label: "Credit amount")
            partyField(label: "Party")
            MfgChipRow(theme: theme, options: ["Restock", "Scrap", "No stock"], selection: $channel)
        case "STOCK":
            MfgChipRow(theme: theme, options: ["Set current", "Add", "Reduce", "Transfer"], selection: bindingKind("Set current"))
                .onChange(of: kindChip) { _, next in
                    if next == "Transfer" { onRouteUtility?("TRANSFER") }
                }
            MfgFieldLabel(text: "Product")
            MfgTextField(theme: theme, placeholder: "Product / part", text: $product)
            MfgFieldLabel(text: "Qty")
            MfgTextField(theme: theme, placeholder: "0", text: $qty, keyboard: .decimalPad)
            MfgChipRow(theme: theme, options: ["Cycle count", "Damage", "Correction", "Custody"], selection: $channel)
        case "BUDGET", "TARGET":
            MfgChipRow(theme: theme, options: ["Sales basis", "Accepted units"], selection: bindingKind(upper == "TARGET" ? "Accepted units" : "Sales basis"))
            moneyHero(label: "New target")
            MfgPickerRow(theme: theme, label: "Period", value: MfgMonthPicker.label(periodYm)) {
                showMonthPicker = true
            }
            MfgMonthPicker(theme: theme, yearMonth: $periodYm)
        case "UPDATE", "ISSUE":
            MfgChipRow(theme: theme, options: ["Update", "Issue", "Risk", "Approval"], selection: bindingKind(upper == "ISSUE" ? "Issue" : "Update"))
            MfgFieldLabel(text: "Title")
            MfgTextField(theme: theme, placeholder: "Title", text: $titleText)
            MfgChipRow(theme: theme, options: ["Low", "Medium", "High"], selection: $channel)
        case "APPROVAL":
            MfgChipRow(theme: theme, options: ["Approval", "Reimbursement"], selection: bindingKind("Approval"))
            MfgFieldLabel(text: "Action")
            MfgTextField(theme: theme, placeholder: "Action", text: $actionTitle)
            MfgFieldLabel(text: "Requester")
            MfgTextField(theme: theme, placeholder: "Requester", text: $party)
            ForEach(teamPeople.prefix(3), id: \.self) { person in
                MfgPickerRow(theme: theme, label: person, value: "Team") { party = person }
            }
            MfgPickerRow(
                theme: theme,
                label: "Approver",
                value: approver.isEmpty ? (teamPeople.first { $0 != party } ?? "Select approver") : approver
            ) {
                approver = teamPeople.first { $0 != party } ?? approver
            }
            ForEach(teamPeople.filter { $0 != party }.prefix(3), id: \.self) { person in
                MfgPickerRow(theme: theme, label: person, value: "Approver") { approver = person }
            }
            MfgChipRow(theme: theme, options: ["Finance ≥ ₹1L", "Manager any", "Owner only"], selection: $policy)
            MfgPickerRow(theme: theme, label: "Decide by", value: decideBy) {
                datePickerTarget = .decide
                showDatePicker = true
            }
            MfgDateQuickPicker(theme: theme, isoDate: $decideBy)
        case "ORDER", "CUSTOMER_ORDER":
            MfgChipRow(theme: theme, options: ["Sales order", "Purchase order", "Work order", "Delivery"], selection: bindingKind("Work order"))
            MfgAmountHero(theme: theme, label: "Commitment", amountText: titleText.isEmpty ? "Untitled order" : titleText, caption: "No revenue · no stock · no GST on save")
            MfgFieldLabel(text: "Title")
            MfgTextField(theme: theme, placeholder: "Title", text: $titleText)
            if let prefilledPart {
                MfgPickerRow(
                    theme: theme,
                    label: "Part from browser",
                    value: [prefilledPart, prefilledUnit].compactMap { $0 }.joined(separator: " · ")
                )
            }
            partyField(label: "Party")
            MfgUnavailableGstRow(theme: theme, label: "GST on commitment", caption: "Est. on invoice/GRN only — Unavailable here")
        case "DELIVERY", "SHIPMENT":
            MfgChipRow(theme: theme, options: ["Incoming receipt", "Outgoing dispatch"], selection: bindingKind("Outgoing dispatch"))
            MfgFieldLabel(text: "Source SO / PO / WO")
            MfgTextField(theme: theme, placeholder: "Source", text: $product)
            MfgFieldLabel(text: "Qty")
            MfgTextField(theme: theme, placeholder: "0", text: $qty, keyboard: .decimalPad)
            MfgPickerRow(theme: theme, label: "Dispatch on", value: "\(dispatchIso) · \(dispatchSlot)") {
                showDateTimePicker = true
            }
            MfgDateTimeSlots(theme: theme, slot: $dispatchSlot)
        case "PRODUCTION":
            MfgChipRow(theme: theme, options: ["Batch/run", "MTO job", "Rework", "Hold WO"], selection: bindingKind("Batch/run"))
                .onChange(of: kindChip) { _, next in
                    if next == "Hold WO" { onRouteUtility?("HOLD") }
                }
            MfgFieldLabel(text: "Finished product")
            MfgTextField(theme: theme, placeholder: "Finished product", text: $product)
            MfgFieldLabel(text: "Accepted qty")
            MfgTextField(theme: theme, placeholder: "0", text: $qty, keyboard: .decimalPad)
            MfgFieldLabel(text: "Rejected qty")
            MfgTextField(theme: theme, placeholder: "0", text: $rejected, keyboard: .decimalPad)
            MfgFieldLabel(text: "Scrap reason")
            MfgTextField(theme: theme, placeholder: "Scrap reason", text: $scrapReason)
            MfgChipRow(theme: theme, options: ["Explicit consumption", "Backflush", "None"], selection: $materials)
            MfgPickerRow(theme: theme, label: "Produced on", value: producedOn) {
                datePickerTarget = .produced
                showDatePicker = true
            }
            MfgDateQuickPicker(theme: theme, isoDate: $producedOn)
        case "REWORK":
            MfgChipRow(theme: theme, options: ["Rework", "Scrap", "Job work out"], selection: bindingKind("Rework"))
            MfgFieldLabel(text: "Source batch / WO")
            MfgTextField(theme: theme, placeholder: "Source", text: $product)
            MfgFieldLabel(text: "Qty")
            MfgTextField(theme: theme, placeholder: "0", text: $qty, keyboard: .decimalPad)
            if kindChip == "Job work out" {
                partyField(label: "Vendor")
                MfgLockedChip(theme: theme, label: "Challan · Unavailable")
                MfgPickerRow(theme: theme, label: "Expected return", value: expectedReturn)
                MfgDateQuickPicker(theme: theme, isoDate: $expectedReturn)
            }
        default:
            moneyHero(label: "Amount")
            partyField(label: "Party")
        }

        MfgMoreDetails(title: "More details") {
            MfgFieldLabel(text: "Note")
            MfgTextField(theme: theme, placeholder: "Notes · tags", text: $note)
        }
    }

    private func moneyHero(label: String, caption: String? = nil) -> some View {
        MfgAmountHero(
            theme: theme,
            currency: model.currency.isEmpty ? "INR" : model.currency,
            amount: $amount,
            label: label,
            caption: caption
        )
    }

    private func partyField(label: String) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            MfgFieldLabel(text: label)
            MfgTextField(theme: theme, placeholder: label, text: $party)
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
            case "PURCHASE": kindChip = "Materials"
            case "STOCK": kindChip = "Set current"
            case "ORDER", "CUSTOMER_ORDER": kindChip = "Work order"
            case "PRODUCTION": kindChip = "Batch/run"
            case "REWORK": kindChip = "Rework"
            case "SPEND": kindChip = "Utilities"
            case "RETURN": kindChip = "Customer return"
            case "DELIVERY", "SHIPMENT": kindChip = "Outgoing dispatch"
            case "UPDATE": kindChip = "Update"
            case "ISSUE": kindChip = "Issue"
            case "APPROVAL", "REIMBURSE": kindChip = upper == "REIMBURSE" ? "Reimbursement" : "Approval"
            case "BUDGET": kindChip = "Sales basis"
            case "TARGET": kindChip = "Accepted units"
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
        case "SALE":
            eventType = saleMode == "Return" ? "RETURN" : "SALE"
            title = "\(eventType == "RETURN" ? "Credit" : "Sale") · \(party.isEmpty ? "Walk-in" : party)"
            paymentStatus = payState == "Paid" ? "PAID" : payState == "Partial" ? "PARTIAL" : "OPEN"
            contract.entryMode = saleMode == "Itemized" ? "ITEMIZED" : "QUICK_AMOUNT"
            contract.paymentState = payState.uppercased()
            if saleMode == "Itemized" {
                amountOut = "\(itemizedTotal)"
            }
        case "PURCHASE":
            eventType = "PURCHASE"
            title = "Purchase · \(party.isEmpty ? "Vendor" : party)"
            paymentStatus = payState.contains("now") || payState == "Pay now" ? "PAID" : payState == "Part" ? "PARTIAL" : "OPEN"
        case "COLLECTION":
            eventType = "COLLECTION"
            title = "Collection · \(party)"
            paymentStatus = "PENDING"
        case "PAYMENT", "REIMBURSE":
            eventType = "PAYMENT"
            title = upper == "REIMBURSE" ? "Reimburse · \(party)" : "Payment · \(party)"
            paymentStatus = "PENDING"
        case "SPEND":
            eventType = "SPEND"
            title = "Expense · \(kindChip)"
            contract.expenseCategory = kindChip
        case "RETURN":
            eventType = "RETURN"
            title = "Return · \(party)"
            contract.stockAction = channel.uppercased()
        case "STOCK":
            eventType = "STOCK"
            title = "Stock · \(kindChip) · \(product)"
            quantity = qty
            itemName = product
            amountOut = nil
            contract.stockAction = kindChip.uppercased().replacingOccurrences(of: " ", with: "_")
            contract.stockReason = channel
        case "BUDGET", "TARGET":
            eventType = kindChip == "Accepted units" || upper == "TARGET" ? "TARGET" : "BUDGET"
            title = "Revision · \(kindChip)"
            if kindChip == "Accepted units" {
                quantity = amount
                amountOut = nil
            }
        case "UPDATE", "ISSUE":
            eventType = (kindChip == "Issue" || kindChip == "Risk") ? "ISSUE" : "UPDATE"
            title = titleText
        case "APPROVAL":
            eventType = "UPDATE"
            title = "\(kindChip) · \(actionTitle.isEmpty ? party : actionTitle)"
        case "ORDER", "CUSTOMER_ORDER":
            eventType = "ORDER"
            title = titleText
            amountOut = nil
            itemName = prefilledPart
            quantity = prefilledPart == nil ? nil : "1"
            contract.orderStatus = "OPEN"
            contract.entryMode = kindChip.uppercased().replacingOccurrences(of: " ", with: "_")
        case "DELIVERY", "SHIPMENT":
            eventType = "DELIVERY"
            title = "Delivery · \(product)"
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
        case "ORDER", "CUSTOMER_ORDER":
            composedNote = [
                "commitment only · \(kindChip.isEmpty ? "Work order" : kindChip)",
                prefilledPart.map { "part=\($0)" },
                note.isEmpty ? nil : note,
            ].compactMap { $0 }.joined(separator: " · ")
        case "SPEND", "BUDGET", "TARGET":
            composedNote = "\(composedNote) · period=\(periodYm)"
        case "DELIVERY", "SHIPMENT":
            composedNote = "\(composedNote) · \(dispatchIso) · \(dispatchSlot)"
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
            case "SALE": return saleDate
            case "COLLECTION": return receivedOn
            case "PRODUCTION": return producedOn
            case "DELIVERY", "SHIPMENT": return dispatchIso
            default: return nil
            }
        }()
        let dueAt: String? = {
            switch upper {
            case "APPROVAL": return decideBy
            case "DELIVERY", "SHIPMENT": return dispatchIso
            case "REWORK" where kindChip == "Job work out": return expectedReturn
            default: return nil
            }
        }()
        let postedLines: [OperatingEventInput.OperatingLineItem]? = {
            if upper == "SALE", saleMode == "Itemized", !itemizedLines.isEmpty {
                return itemizedLines.map {
                    .init(itemName: $0.name, quantity: "1", unit: "nos", unitPrice: $0.price)
                }
            }
            if upper == "ORDER" || upper == "CUSTOMER_ORDER", let part = prefilledPart {
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
                case "COLLECTION", "PAYMENT", "REIMBURSE", "SALE":
                    return channel.isEmpty ? nil : channel
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
        if ok { dismiss() }
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

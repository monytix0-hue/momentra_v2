import SwiftUI

/// Figma-parity retail capture sheets (RTL-QA-02 … QA-12). Router for all non-POS retail captures.
struct RetailCaptureSheet: View {
    let theme: OperatingTheme
    let mode: String
    @ObservedObject var model: BusinessOperatingModel
    var browserPrefill: OperatingBrowserItem? = nil

    var body: some View {
        RetailCaptureForm(theme: theme, mode: mode, model: model, browserPrefill: browserPrefill)
    }
}

// MARK: - Shared form

private struct RetailCaptureForm: View {
    let theme: OperatingTheme
    let mode: String
    @ObservedObject var model: BusinessOperatingModel
    var browserPrefill: OperatingBrowserItem?

    @Environment(\.dismiss) private var dismiss
    @State private var draft = RetailCaptureDraft()
    @State private var saving = false
    @State private var didPrefill = false

    private var upper: String { mode.uppercased() }
    private var layout: OperatingCaptureLayout? { OperatingCaptureLayout.forMode(mode) }

    private var title: String {
        switch upper {
        case "STATUS": return "Status / Assignment"
        case "ISSUE": return "Issue / Approval / Update"
        case "SPEND": return "Record Expense"
        case "COLLECTION": return "Record Collection"
        case "PAYMENT": return "Record Payment"
        case "PURCHASE": return "Record Purchase"
        case "ORDER": return draft.orderSide == "Purchase order" ? "Purchase Order" : "Sales Order"
        case "BUDGET", "TARGET": return "Budget / Target Revision"
        default: return layout?.label ?? mode.capitalized
        }
    }

    private var qaCode: String {
        switch upper {
        case "STATUS": return "RTL-QA-08"
        case "ORDER": return "RTL-QA-07"
        case "PURCHASE": return "RTL-QA-02"
        case "COLLECTION": return "RTL-QA-03"
        case "PAYMENT": return "RTL-QA-04"
        case "SPEND": return "RTL-QA-05"
        case "BUDGET", "TARGET": return "RTL-QA-09"
        case "ISSUE", "UPDATE": return "RTL-QA-10"
        default: return layout?.qaCode ?? "RTL-QA"
        }
    }

    private var contextLine: String {
        let brand = model.title.isEmpty ? "Retail" : model.title
        switch upper {
        case "SALE": return "\(brand) · Sale ≠ receipt · stock only on dispatch"
        case "PURCHASE": return "\(draft.goodsMode) · \(brand) · GST on"
        case "COLLECTION": return "\(brand) · AR settle"
        case "PAYMENT": return "\(brand) · settle supplier"
        case "SPEND": return "Store ops · \(brand) · cost"
        case "STOCK": return "\(brand) · Reconcile qty · no invented buy/sell"
        case "ORDER":
            return draft.orderSide == "Purchase order"
                ? "Supplier commitment · \(brand) · not AP"
                : "Customer commitment · \(brand) · not revenue"
        case "STATUS": return "Floor board · \(brand) · ops only"
        case "BUDGET", "TARGET": return "Owner plan · \(brand) · versioned"
        case "ISSUE", "UPDATE": return "Register · \(brand) · audit trail"
        default: return "\(brand) · Retail Store"
        }
    }

    private var canSave: Bool {
        RetailCaptureEventBuilder(mode: mode, draft: draft, model: model).canSave
    }

    private var goodsTotal: Decimal {
        draft.lines.reduce(Decimal.zero) { $0 + $1.lineTotal }
    }

    private var goodsTotalString: String {
        NSDecimalNumber(decimal: goodsTotal).stringValue
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                RoseGrabber()
                RoseSheetHeader(
                    theme: theme,
                    title: title,
                    subtitle: stickyMeta,
                    qaCode: qaCode,
                    onClose: { dismiss() }
                )
                RoseContextLine(text: contextLine)

                modeBody

                if showsSharedMoreDetails {
                    RoseMoreDetails(title: "More details") {
                        SetupDateTimeField(label: "Recorded at", isoValue: $draft.occurredAt)
                        RoseFieldLabel(text: "Note")
                        RoseTextField(theme: theme, placeholder: "Optional reference", text: $draft.note)
                    }
                }

                RoseWhyBox(theme: theme, text: whyText)

                RoseStickyFooter(
                    theme: theme,
                    title: saving ? "Saving…" : saveLabel,
                    disabled: !canSave,
                    loading: saving,
                    action: { Task { await save() } }
                )
            }
            .padding(16)
            .padding(.bottom, 24)
        }
        .background(theme.canvas)
        .onAppear {
            applyPrefill()
            ensureSeedLines()
        }
        .task {
            if model.masters.isEmpty { await model.loadMasters() }
        }
    }

    private var showsSharedMoreDetails: Bool {
        !["PURCHASE", "COLLECTION", "PAYMENT", "SPEND", "ORDER", "STATUS", "BUDGET", "TARGET", "ISSUE", "UPDATE"].contains(upper)
    }

    private var saveLabel: String {
        let money = formattedMoney(draft.amount.isEmpty ? goodsTotalString : draft.amount)
        switch upper {
        case "STATUS": return "Update status"
        case "ORDER":
            if draft.orderSide == "Purchase order" {
                return canSave ? "Save purchase order · \(money)" : "Save purchase order"
            }
            return canSave ? "Save sales order · \(money)" : "Save sales order"
        case "BUDGET", "TARGET":
            return canSave ? "Save revision · \(money)" : "Save revision"
        case "PURCHASE":
            return canSave ? "Post purchase · \(money)" : "Post purchase"
        case "COLLECTION":
            return canSave ? "Post collection · \(money)" : "Post collection"
        case "PAYMENT":
            return canSave ? "Post payment · \(money)" : "Post payment"
        case "SPEND":
            return canSave ? "Post expense · \(money)" : "Post expense"
        case "ISSUE":
            return draft.revisionKind == "Issue" ? "Post issue" : "Submit \(draft.revisionKind)"
        default:
            return "Save · \(title)"
        }
    }

    private var stickyMeta: String {
        switch layout {
        case .sale: return draft.paymentChoice
        case .collection: return "Allocates to open AR"
        case .payment: return "Settles open AP"
        case .order: return "Commitment · no revenue"
        case .stock: return "Stock \(draft.stockAction)"
        case .purchase: return draft.tender
        case .spend: return draft.paymentChoice
        default: return "Draft · not yet saved"
        }
    }

    private var whyText: String {
        switch layout {
        case .purchase:
            return "Stock += accepted qty only on receipt. AP from posted purchase; payment allocates later — never double-cost."
        case .collection:
            return "Allocates open AR — never creates new sales. Receipt evidence ≠ separate revenue."
        case .payment:
            return "Settles open AP only — never posts new purchase cost."
        case .spend:
            return "Expense is cost; payment state is separate. Never invent stock from expense."
        case .stock:
            return "Set current reconciles on-hand. Quick sales never move stock alone."
        case .order:
            return draft.orderSide == "Purchase order"
                ? "Purchase order is commitment only — no AP, no stock, no GST posting. Cost + stock on goods receipt / posted Purchase."
                : "Sales order is commitment only — revenue + stock on posted Sale / POS Charge. Never posts GST as sale by itself."
        case .budget:
            return "Versioned + effective-dated plan. Never rewrite posted sales to match target."
        case .update:
            return "Audit / action / permission — approval ≠ disbursement. Shift variance opens Issue, not a sale edit."
        default:
            if upper == "STATUS" {
                return "Operational state only — does not post revenue or stock by itself. Sale / POS Charge posts facts."
            }
            return "One capture posts one event. Nothing here rewrites an approved record."
        }
    }

    @ViewBuilder
    private var modeBody: some View {
        switch upper {
        case "STATUS":
            statusBody
        case "ORDER":
            orderBody
        case "BUDGET", "TARGET":
            budgetBody
        case "ISSUE", "UPDATE":
            issueBody
        case "STOCK":
            stockBody
        case "COLLECTION":
            collectionBody
        case "PAYMENT":
            paymentBody
        case "PURCHASE":
            purchaseBody
        case "SPEND":
            expenseBody
        default:
            moneyBody
        }
    }

    // MARK: Purchase `2399:30130`

    @ViewBuilder
    private var purchaseBody: some View {
        RoseChipRow(theme: theme, options: ["Goods", "Supplies"], selection: $draft.goodsMode)
        RoseFieldLabel(text: "Scan supplier invoice / SKU")
        RoseScanField(theme: theme, placeholder: "INV / SKU · tap to scan", query: $draft.scanQuery, actionLabel: "Scan") {
            guard !draft.scanQuery.isEmpty else { return }
            if let idx = draft.lines.firstIndex(where: { $0.name.isEmpty }) {
                draft.lines[idx].name = draft.scanQuery
                draft.lines[idx].sku = String(draft.scanQuery.prefix(8)).uppercased()
            } else {
                draft.lines.append(RetailLineDraft(name: draft.scanQuery, sku: String(draft.scanQuery.prefix(8)).uppercased()))
            }
            draft.scanQuery = ""
            syncAmountFromLines()
        }
        RoseLinesCard(theme: theme, title: "Receive lines", trailing: "\(filledLineCount) lines") {
            ForEach($draft.lines) { $line in
                purchaseLineEditor(line: $line)
            }
            HStack {
                Button("+ Add line") {
                    draft.lines.append(RetailLineDraft())
                }
                .font(.plusJakarta(size: 13, weight: .semibold))
                .foregroundStyle(theme.accent)
                Spacer()
                Button("From PO · Favourites") {
                    seedFavouriteLines(purchase: true)
                }
                .font(.plusJakarta(size: 11))
                .foregroundStyle(OperatingV12.subtitle)
            }
            .buttonStyle(.plain)
        }
        purchaseTotals
        RoseFieldLabel(text: "Tender / method")
        RoseChipRow(theme: theme, options: ["Pay later", "Pay now", "Part"], selection: $draft.tender)
        RoseFieldLabel(text: "Supplier")
        RoseTextField(theme: theme, placeholder: "Supplier name", text: $draft.counterparty)
        partyChips(kind: "SUPPLIER")
        RosePickerRow(theme: theme, label: "Location", value: draft.location) {
            draft.location = locationMasters.first?.name ?? draft.location
        }
        RosePickerRow(theme: theme, label: "Linked PO", value: draft.linkedPo.isEmpty ? "Optional" : draft.linkedPo) {
            draft.linkedPo = openPurchaseOrders.first?.title ?? draft.linkedPo
        }
        RoseMoreDetails(title: "More details") {
            RoseFieldLabel(text: "Freight")
            RoseTextField(theme: theme, placeholder: "0", text: $draft.freight, keyboard: .decimalPad)
                .onChange(of: draft.freight) { _, _ in syncAmountFromLines() }
            RoseFieldLabel(text: "Note")
            RoseTextField(theme: theme, placeholder: "GRN note · supplier GSTIN", text: $draft.note)
        }
    }

    private var purchaseTotals: some View {
        VStack(spacing: 8) {
            roseMoneyRow("Goods", formattedMoney(goodsTotalString))
            RoseUnavailableGstRow(theme: theme, label: "Input GST · CGST+SGST")
            roseMoneyRow("Freight", formattedMoney(draft.freight.isEmpty ? "0" : draft.freight))
            Divider().overlay(theme.border.opacity(0.4))
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text("Purchase total")
                        .font(.plusJakarta(size: 10))
                        .foregroundStyle(OperatingV12.subtitle)
                    Text(formattedMoney(draft.amount))
                        .font(.plusJakarta(size: 24, weight: .bold))
                        .foregroundStyle(OperatingV12.title)
                }
                Spacer()
                VStack(alignment: .trailing, spacing: 2) {
                    Text(draft.tender)
                        .font(.plusJakarta(size: 10))
                        .foregroundStyle(OperatingV12.subtitle)
                    Text(draft.tender == "Pay later" ? "AP open" : draft.tender)
                        .font(.plusJakarta(size: 14, weight: .bold))
                        .foregroundStyle(theme.accent)
                }
            }
        }
        .padding(12)
        .background(Color.black.opacity(0.24))
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
    }

    @ViewBuilder
    private func purchaseLineEditor(line: Binding<RetailLineDraft>) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack {
                Text(line.wrappedValue.sku.isEmpty ? "SKU" : line.wrappedValue.sku)
                    .font(.plusJakarta(size: 10))
                    .foregroundStyle(theme.accent)
                Spacer()
                Text(formattedMoney(NSDecimalNumber(decimal: line.wrappedValue.lineTotal).stringValue))
                    .font(.plusJakarta(size: 13, weight: .semibold))
                    .foregroundStyle(theme.accent)
            }
            RoseTextField(theme: theme, placeholder: "Item name", text: line.name)
                .onChange(of: line.wrappedValue.name) { _, _ in syncAmountFromLines() }
            HStack(spacing: 8) {
                RoseTextField(theme: theme, placeholder: "Qty", text: line.qty, keyboard: .decimalPad)
                    .onChange(of: line.wrappedValue.qty) { _, _ in syncAmountFromLines() }
                RoseTextField(theme: theme, placeholder: "Rate", text: line.rate, keyboard: .decimalPad)
                    .onChange(of: line.wrappedValue.rate) { _, _ in syncAmountFromLines() }
                RoseTextField(theme: theme, placeholder: "HSN", text: line.hsn)
            }
        }
        .padding(10)
        .background(theme.inset)
        .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
    }

    // MARK: Collection `2399:30194`

    @ViewBuilder
    private var collectionBody: some View {
        openReceivables(title: "Allocate against")
        let openAr = Decimal(string: selectedReceivable?.amount ?? "0") ?? 0
        let receipt = Decimal(string: draft.amount) ?? 0
        let remaining = max(openAr - receipt, 0)
        VStack(spacing: 8) {
            roseMoneyRow("Open AR", formattedMoney(selectedReceivable?.amount))
            roseMoneyRow("This receipt", formattedMoney(draft.amount.isEmpty ? "0" : draft.amount))
            roseMoneyRow("Remaining", formattedMoney(NSDecimalNumber(decimal: remaining).stringValue))
            Divider().overlay(theme.border.opacity(0.4))
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text("Collect now")
                        .font(.plusJakarta(size: 10))
                        .foregroundStyle(OperatingV12.subtitle)
                    Text(formattedMoney(draft.amount))
                        .font(.plusJakarta(size: 24, weight: .bold))
                        .foregroundStyle(OperatingV12.title)
                }
                Spacer()
                VStack(alignment: .trailing, spacing: 2) {
                    Text("Method")
                        .font(.plusJakarta(size: 10))
                        .foregroundStyle(OperatingV12.subtitle)
                    Text(draft.tender)
                        .font(.plusJakarta(size: 14, weight: .bold))
                        .foregroundStyle(theme.accent)
                }
            }
        }
        .padding(12)
        .background(Color.black.opacity(0.24))
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))

        RoseFieldLabel(text: "Method")
        RoseChipRow(theme: theme, options: ["UPI", "Cash", "Card", "Bank"], selection: $draft.tender)
        RoseFieldLabel(text: "Customer")
        RoseTextField(theme: theme, placeholder: "Who paid", text: $draft.counterparty)
        RoseFieldLabel(text: "Reference")
        RoseTextField(theme: theme, placeholder: "UPI / NEFT ref", text: $draft.reference)
        RosePickerRow(theme: theme, label: "Receipt print", value: draft.receiptPrint) {
            draft.receiptPrint = draft.receiptPrint == "On" ? "Off" : "On"
        }
        RoseFieldLabel(text: "Collected")
        RoseTextField(theme: theme, placeholder: "0.00", text: $draft.amount, keyboard: .decimalPad)
        RoseMoreDetails(title: "More details") {
            Text("Notes · Proof · Tags")
                .font(.plusJakarta(size: 12))
                .foregroundStyle(OperatingV12.subtitle)
        }
    }

    // MARK: Payment `2399:30253`

    @ViewBuilder
    private var paymentBody: some View {
        openPayables(title: "Allocate against")
        let openAp = Decimal(string: selectedPayable?.amount ?? "0") ?? 0
        let paid = Decimal(string: draft.amount) ?? 0
        let remaining = max(openAp - paid, 0)
        VStack(spacing: 8) {
            roseMoneyRow("Open AP", formattedMoney(selectedPayable?.amount))
            roseMoneyRow("This payment", formattedMoney(draft.amount.isEmpty ? "0" : draft.amount))
            roseMoneyRow("Remaining", formattedMoney(NSDecimalNumber(decimal: remaining).stringValue))
            Divider().overlay(theme.border.opacity(0.4))
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text("Pay now")
                        .font(.plusJakarta(size: 10))
                        .foregroundStyle(OperatingV12.subtitle)
                    Text(formattedMoney(draft.amount))
                        .font(.plusJakarta(size: 24, weight: .bold))
                        .foregroundStyle(OperatingV12.title)
                }
                Spacer()
                VStack(alignment: .trailing, spacing: 2) {
                    Text("Method")
                        .font(.plusJakarta(size: 10))
                        .foregroundStyle(OperatingV12.subtitle)
                    Text(draft.tender)
                        .font(.plusJakarta(size: 14, weight: .bold))
                        .foregroundStyle(theme.accent)
                }
            }
        }
        .padding(12)
        .background(Color.black.opacity(0.24))
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))

        RoseFieldLabel(text: "Method")
        RoseChipRow(theme: theme, options: ["Bank", "UPI", "Cash", "Cheque"], selection: $draft.tender)
        RoseFieldLabel(text: "Payee")
        RoseTextField(theme: theme, placeholder: "Supplier", text: $draft.counterparty)
        RoseFieldLabel(text: "Reference")
        RoseTextField(theme: theme, placeholder: "NEFT / cheque no.", text: $draft.reference)
        RosePickerRow(theme: theme, label: "Linked purchase", value: selectedPayable?.title ?? "—")
        RoseFieldLabel(text: "Payment amount")
        RoseTextField(theme: theme, placeholder: "0.00", text: $draft.amount, keyboard: .decimalPad)
        RoseMoreDetails(title: "More details") {
            Text("Notes · Proof · Tags")
                .font(.plusJakarta(size: 12))
                .foregroundStyle(OperatingV12.subtitle)
        }
    }

    // MARK: Expense `2399:30306`

    @ViewBuilder
    private var expenseBody: some View {
        RoseChipRow(theme: theme, options: ["Store", "Ops", "Other"], selection: $draft.expenseMode)
        VStack(spacing: 8) {
            roseMoneyRow("Amount", formattedMoney(draft.amount.isEmpty ? "0" : draft.amount))
            RoseUnavailableGstRow(
                theme: theme,
                label: "GST (if any)",
                caption: "Unavailable / ₹0 — never invent GST on expense"
            )
            roseMoneyRow("Cash impact", draft.paymentChoice)
            Divider().overlay(theme.border.opacity(0.4))
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text("Expense")
                        .font(.plusJakarta(size: 10))
                        .foregroundStyle(OperatingV12.subtitle)
                    Text(formattedMoney(draft.amount))
                        .font(.plusJakarta(size: 24, weight: .bold))
                        .foregroundStyle(OperatingV12.title)
                }
                Spacer()
                VStack(alignment: .trailing, spacing: 2) {
                    Text("Type")
                        .font(.plusJakarta(size: 10))
                        .foregroundStyle(OperatingV12.subtitle)
                    Text(draft.expenseType.isEmpty ? draft.typeChip : draft.expenseType)
                        .font(.plusJakarta(size: 14, weight: .bold))
                        .foregroundStyle(theme.accent)
                }
            }
        }
        .padding(12)
        .background(Color.black.opacity(0.24))
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))

        RoseFieldLabel(text: "Payment state")
        RoseChipRow(theme: theme, options: ["Paid now", "Pay later"], selection: $draft.paymentChoice)
        RoseFieldLabel(text: "Type")
        RoseChipRow(
            theme: theme,
            options: ["Packaging", "Rent", "Utilities", "Transport", "Other"],
            selection: $draft.typeChip
        )
        if draft.typeChip == "Other" {
            RoseTextField(theme: theme, placeholder: "Custom type", text: $draft.expenseType)
        }
        RoseFieldLabel(text: "Payee")
        RoseTextField(theme: theme, placeholder: "Optional vendor", text: $draft.counterparty)
        RoseFieldLabel(text: "Period")
        RoseChipRow(
            theme: theme,
            options: [periodDefault, "This week", "Custom"],
            selection: $draft.periodChip
        )
        if draft.periodChip == "Custom" {
            RoseTextField(theme: theme, placeholder: "e.g. Nov 2026", text: $draft.periodCustom)
        }
        RoseAmountHero(
            theme: theme,
            label: "Expense",
            amountText: formattedMoney(draft.amount),
            caption: draft.expenseMode
        )
        RoseFieldLabel(text: "Amount")
        RoseTextField(theme: theme, placeholder: "0.00", text: $draft.amount, keyboard: .decimalPad)
        RoseMoreDetails(title: "More details") {
            RoseTextField(theme: theme, placeholder: "Notes · Bill photo · Tags", text: $draft.note)
        }
    }

    // MARK: Legacy money body (sale etc.)

    @ViewBuilder
    private var moneyBody: some View {
        if layout == .sale {
            RoseChipRow(theme: theme, options: ["Quick Sale", "Itemised Sale"], selection: $draft.entryMode)
        }
        RoseFieldLabel(text: fieldTitle)
        RoseTextField(theme: theme, placeholder: "What happened", text: $draft.title)
        if layout != .update {
            RoseFieldLabel(text: counterpartyLabel)
            RoseTextField(theme: theme, placeholder: counterpartyLabel, text: $draft.counterparty)
        }
        if needsAmount {
            RoseAmountHero(theme: theme, currency: model.currency, amount: $draft.amount)
        }
        if [.sale, .stock].contains(layout) {
            skuSection
        }
        if layout == .sale {
            RoseFieldLabel(text: "Payment")
            RoseChipRow(
                theme: theme,
                options: ["Fully Paid", "Partially Paid", "Pay Later"],
                selection: $draft.paymentChoice
            )
        }
        RoseFieldLabel(text: "Channel")
        RoseTextField(theme: theme, placeholder: "Counter · online", text: $draft.channel)
        if layout == .sale || layout == .collection || layout == .payment {
            RoseTenderChips(theme: theme, tender: $draft.tender, options: ["UPI", "Cash", "Card"])
        }
    }

    @ViewBuilder
    private var stockBody: some View {
        RoseFieldLabel(text: "Stock action")
        RoseChipRow(theme: theme, options: ["IN", "OUT", "COUNT"], selection: $draft.stockAction)
        RoseFieldLabel(text: "Item")
        RoseTextField(theme: theme, placeholder: "SKU or description", text: $draft.title)
        skuSection
        HStack(spacing: 8) {
            VStack(alignment: .leading, spacing: 4) {
                RoseFieldLabel(text: "Quantity")
                RoseTextField(theme: theme, placeholder: "0", text: $draft.quantity, keyboard: .decimalPad)
            }
            VStack(alignment: .leading, spacing: 4) {
                RoseFieldLabel(text: "Unit")
                RoseTextField(theme: theme, placeholder: "pc", text: $draft.unit)
            }
        }
        RoseFieldLabel(text: "Reason")
        RoseTextField(theme: theme, placeholder: "Count · damage · correction", text: $draft.stockReason)
    }

    // MARK: Order PO/SO `2443:29784` / `2399:30418` — D1-A

    @ViewBuilder
    private var orderBody: some View {
        let isPo = draft.orderSide == "Purchase order"
        RoseChipRow(theme: theme, options: ["Sales order", "Purchase order"], selection: $draft.orderSide)
            .onChange(of: draft.orderSide) { _, side in
                draft.stateChip = side == "Purchase order" ? "Sent" : "Confirmed"
                draft.location = side == "Purchase order" ? "Back store" : "Pickup · store"
            }
        RoseChipRow(
            theme: theme,
            options: isPo ? ["Sent", "Draft", "Partial"] : ["Confirmed", "Hold", "Draft"],
            selection: $draft.stateChip
        )

        RoseSectionCard(theme: theme, label: isPo ? "Stock items" : "Catalogue") {
            RoseScanField(
                theme: theme,
                placeholder: isPo ? "Search SKU / item" : "Search SKU, name or barcode",
                query: $draft.scanQuery,
                actionLabel: "Scan",
                action: {}
            )
            catalogueBrowser(purchase: isPo)
        }

        RoseLinesCard(
            theme: theme,
            title: isPo ? "PO lines" : "SO lines",
            trailing: "\(filledLineCount) SKUs"
        ) {
            ForEach($draft.lines) { $line in
                orderLineEditor(line: $line)
            }
            Button("+ Add line") { draft.lines.append(RetailLineDraft()) }
                .font(.plusJakarta(size: 13, weight: .semibold))
                .foregroundStyle(theme.accent)
                .buttonStyle(.plain)
        }

        VStack(spacing: 8) {
            if isPo {
                roseMoneyRow("Goods", formattedMoney(goodsTotalString))
                RoseUnavailableGstRow(
                    theme: theme,
                    label: "Est. input GST · on GRN",
                    caption: "Estimate label only — never posted from this sheet"
                )
                roseMoneyRow("Freight (est.)", "₹0")
            } else {
                roseMoneyRow("Lines value", formattedMoney(goodsTotalString))
                RoseUnavailableGstRow(
                    theme: theme,
                    label: "Est. GST · on bill",
                    caption: "Estimate label only — never posted from this sheet"
                )
                roseMoneyRow("Balance due on sale", formattedMoney(goodsTotalString))
            }
            Divider().overlay(theme.border.opacity(0.4))
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text(isPo ? "PO value" : "SO value")
                        .font(.plusJakarta(size: 10))
                        .foregroundStyle(OperatingV12.subtitle)
                    Text(formattedMoney(goodsTotalString))
                        .font(.plusJakarta(size: 24, weight: .bold))
                        .foregroundStyle(OperatingV12.title)
                }
                Spacer()
                VStack(alignment: .trailing, spacing: 2) {
                    Text("Status")
                        .font(.plusJakarta(size: 10))
                        .foregroundStyle(OperatingV12.subtitle)
                    Text(draft.stateChip)
                        .font(.plusJakarta(size: 14, weight: .bold))
                        .foregroundStyle(theme.accent)
                }
            }
        }
        .padding(12)
        .background(Color.black.opacity(0.24))
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))

        RoseFieldLabel(text: isPo ? "Supplier" : "Customer")
        RoseTextField(theme: theme, placeholder: "Optional", text: $draft.counterparty)
        partyChips(kind: isPo ? "SUPPLIER" : "CUSTOMER")
        RoseFieldLabel(text: isPo ? "Expect by" : "Fulfil by")
        SetupDateTimeField(label: isPo ? "Expect by" : "Fulfil by", isoValue: $draft.dueAt)
        RoseFieldLabel(text: isPo ? "Deliver to" : "Ship / pickup")
        RoseTextField(theme: theme, placeholder: isPo ? "Back store" : "Pickup · store", text: $draft.location)
        RosePickerRow(theme: theme, label: "Place of supply", value: draft.placeOfSupply) {
            draft.placeOfSupply = "Maharashtra (27) · intra-state"
        }
        RoseMoreDetails(title: "More details") {
            RoseTextField(theme: theme, placeholder: "Notes · Terms · Tags", text: $draft.note)
        }
    }

    @ViewBuilder
    private func catalogueBrowser(purchase: Bool) -> some View {
        let skus = catalogMasters.filter {
            draft.scanQuery.isEmpty
                || $0.name.localizedCaseInsensitiveContains(draft.scanQuery)
                || ($0.code?.localizedCaseInsensitiveContains(draft.scanQuery) ?? false)
        }
        if skus.isEmpty {
            Text("No catalogue rows yet — add lines manually.")
                .font(.plusJakarta(size: 12))
                .foregroundStyle(OperatingV12.subtitle)
        }
        ForEach(skus.prefix(6)) { sku in
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text(sku.name)
                        .font(.plusJakarta(size: 13, weight: .semibold))
                        .foregroundStyle(OperatingV12.title)
                    Text([sku.code, sku.unit, sku.hsn.map { "HSN \($0)" }].compactMap { $0 }.joined(separator: " · "))
                        .font(.plusJakarta(size: 10))
                        .foregroundStyle(OperatingV12.subtitle)
                }
                Spacer()
                Text(formattedMoney(purchase ? (sku.purchasePrice ?? sku.sellPrice) : (sku.sellPrice ?? sku.purchasePrice)))
                    .font(.plusJakarta(size: 13, weight: .semibold))
                Button("Add") {
                    addCatalogLine(sku, purchase: purchase)
                }
                .font(.plusJakarta(size: 12, weight: .bold))
                .foregroundStyle(theme.onAccent)
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
                .background(theme.accent)
                .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
            }
            .padding(10)
            .background(theme.canvas.opacity(0.35))
            .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
            .buttonStyle(.plain)
        }
    }

    @ViewBuilder
    private func orderLineEditor(line: Binding<RetailLineDraft>) -> some View {
        HStack(alignment: .center, spacing: 8) {
            VStack(alignment: .leading, spacing: 2) {
                Text(line.wrappedValue.sku.isEmpty ? "SKU" : line.wrappedValue.sku)
                    .font(.plusJakarta(size: 10))
                    .foregroundStyle(theme.accent)
                RoseTextField(theme: theme, placeholder: "Item", text: line.name)
                    .onChange(of: line.wrappedValue.name) { _, _ in syncAmountFromLines() }
                Text(
                    [
                        line.wrappedValue.rate.isEmpty ? nil : formattedMoney(line.wrappedValue.rate),
                        line.wrappedValue.unit,
                        line.wrappedValue.hsn.isEmpty ? nil : "HSN \(line.wrappedValue.hsn)",
                    ].compactMap { $0 }.joined(separator: " · ")
                )
                .font(.plusJakarta(size: 10))
                .foregroundStyle(OperatingV12.subtitle)
            }
            let qty = Int(line.wrappedValue.qty) ?? 1
            RoseStepper(theme: theme, quantity: qty, onDecrement: {
                if qty > 1 {
                    line.wrappedValue.qty = String(qty - 1)
                } else if draft.lines.count > 1 {
                    draft.lines.removeAll { $0.id == line.wrappedValue.id }
                }
                syncAmountFromLines()
            }, onIncrement: {
                line.wrappedValue.qty = String(qty + 1)
                syncAmountFromLines()
            })
            Text(formattedMoney(NSDecimalNumber(decimal: line.wrappedValue.lineTotal).stringValue))
                .font(.plusJakarta(size: 13, weight: .semibold))
                .foregroundStyle(theme.accent)
        }
        RoseTextField(theme: theme, placeholder: "Rate", text: line.rate, keyboard: .decimalPad)
            .onChange(of: line.wrappedValue.rate) { _, _ in syncAmountFromLines() }
    }

    // MARK: Status / Budget / Issue

    @ViewBuilder
    private var statusBody: some View {
        RoseChipRow(theme: theme, options: ["Dispatch", "Fulfil", "Pick"], selection: $draft.statusMode)
        openOrdersPicker
        RoseFieldLabel(text: "Next status")
        RoseChipRow(
            theme: theme,
            options: ["Ready to pick", "Picking", "Ready", "Dispatched"],
            selection: $draft.statusChoice
        )
        RoseFieldLabel(text: "Assignee")
        RoseTextField(theme: theme, placeholder: "Staff · floor · courier", text: $draft.staff)
        RoseFieldLabel(text: "Due")
        RoseTextField(theme: theme, placeholder: "Today · 6pm", text: $draft.dueLabel)
        RosePickerRow(theme: theme, label: "Notify", value: draft.notify) {
            draft.notify = draft.notify == "Customer WhatsApp" ? "None" : "Customer WhatsApp"
        }
        RoseFieldLabel(text: "Update note")
        RoseTextField(theme: theme, placeholder: "What changed", text: $draft.title)
        RoseMoreDetails(title: "More details") {
            Text("Notes · Photo · Tags")
                .font(.plusJakarta(size: 12))
                .foregroundStyle(OperatingV12.subtitle)
        }
    }

    @ViewBuilder
    private var openOrdersPicker: some View {
        let orders = (model.projection?.activity ?? []).filter { $0.eventType.uppercased() == "ORDER" }
        RoseLinesCard(theme: theme, title: "Source", trailing: "\(orders.count) open") {
            if orders.isEmpty {
                RoseEmptyCard(theme: theme, title: "No orders", bodyText: "Create a sales or purchase order first.")
            } else {
                ForEach(Array(orders.prefix(8))) { row in
                    Button {
                        draft.parentEventId = row.id
                        if draft.title.isEmpty { draft.title = row.title }
                    } label: {
                        HStack {
                            VStack(alignment: .leading, spacing: 2) {
                                Text(row.title)
                                    .font(.plusJakarta(size: 13, weight: .semibold))
                                    .foregroundStyle(OperatingV12.title)
                                Text(row.status ?? "OPEN")
                                    .font(.plusJakarta(size: 10))
                                    .foregroundStyle(OperatingV12.subtitle)
                            }
                            Spacer()
                            if draft.parentEventId == row.id {
                                Image(systemName: "checkmark.circle.fill").foregroundStyle(theme.accent)
                            }
                        }
                        .padding(10)
                        .background(draft.parentEventId == row.id ? theme.accent.opacity(0.14) : theme.canvas.opacity(0.3))
                        .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }

    @ViewBuilder
    private var budgetBody: some View {
        RoseChipRow(theme: theme, options: ["Sales target", "Budget"], selection: $draft.revisionKind)
        let previous = model.projection?.target?.value
        let prev = Decimal(string: previous ?? "")
        let next = Decimal(string: draft.amount)
        let delta: Decimal? = {
            guard let prev, let next else { return nil }
            return next - prev
        }()
        VStack(spacing: 8) {
            roseMoneyRow("Previous v1", formattedMoney(previous))
            roseMoneyRow(
                "Delta",
                delta.map {
                    let sign = $0 >= 0 ? "+ " : "− "
                    return sign + formattedMoney(NSDecimalNumber(decimal: $0.magnitude).stringValue)
                } ?? "—"
            )
            roseMoneyRow("Effective", "On save")
            Divider().overlay(theme.border.opacity(0.4))
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text("New target")
                        .font(.plusJakarta(size: 10))
                        .foregroundStyle(OperatingV12.subtitle)
                    Text(formattedMoney(draft.amount))
                        .font(.plusJakarta(size: 24, weight: .bold))
                        .foregroundStyle(OperatingV12.title)
                }
                Spacer()
                VStack(alignment: .trailing, spacing: 2) {
                    Text("Period")
                        .font(.plusJakarta(size: 10))
                        .foregroundStyle(OperatingV12.subtitle)
                    Text(draft.periodChip == "Custom" ? draft.periodCustom : draft.periodChip)
                        .font(.plusJakarta(size: 14, weight: .bold))
                        .foregroundStyle(theme.accent)
                }
            }
        }
        .padding(12)
        .background(Color.black.opacity(0.24))
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))

        RoseFieldLabel(text: "Period")
        RoseChipRow(theme: theme, options: [periodDefault, "Next month", "Custom"], selection: $draft.periodChip)
        if draft.periodChip == "Custom" {
            RoseTextField(theme: theme, placeholder: "e.g. Nov 2026", text: $draft.periodCustom)
        }
        RoseFieldLabel(text: "Amount")
        RoseTextField(theme: theme, placeholder: "0.00", text: $draft.amount, keyboard: .decimalPad)
        RoseFieldLabel(text: "Reason")
        RoseTextField(theme: theme, placeholder: "Festive demand", text: $draft.note)
        RoseFieldLabel(text: "Owner")
        RoseTextField(theme: theme, placeholder: "Optional", text: $draft.staff)
        RoseMoreDetails(title: "More details") {
            Text("Notes · Attachment · Tags")
                .font(.plusJakarta(size: 12))
                .foregroundStyle(OperatingV12.subtitle)
        }
    }

    @ViewBuilder
    private var issueBody: some View {
        HStack(spacing: 6) {
            RoseModeChip(theme: theme, text: "Issue", selected: draft.revisionKind == "Issue") {
                draft.revisionKind = "Issue"
            }
            RoseLockedChip(theme: theme, label: "Approval")
            RoseModeChip(theme: theme, text: "Update", selected: draft.revisionKind == "Update") {
                draft.revisionKind = "Update"
            }
        }
        RoseSectionCard(theme: theme, label: "Linked") {
            RoseTextField(theme: theme, placeholder: "Register · shift", text: $draft.link)
            Text("Attachment chrome only — no upload API")
                .font(.plusJakarta(size: 11))
                .foregroundStyle(OperatingV12.subtitle)
        }
        RoseFieldLabel(text: "Severity")
        RoseChipRow(theme: theme, options: ["Alert", "Warn", "Info"], selection: $draft.severity)
        RoseFieldLabel(text: "Title")
        RoseTextField(theme: theme, placeholder: "Short title", text: $draft.title)
        RoseFieldLabel(text: "Owner / assignee")
        RoseTextField(theme: theme, placeholder: "Staff", text: $draft.staff)
        RoseMoreDetails(title: "More details") {
            RoseTextField(theme: theme, placeholder: "Notes · Evidence · Tags", text: $draft.note)
            RosePickerRow(theme: theme, label: "Attachment", value: "Add photo (chrome only)")
        }
    }

    // MARK: Shared helpers

    @ViewBuilder
    private func openReceivables(title: String) -> some View {
        let open = (model.projection?.paymentReadiness ?? []).filter { $0.relevance.uppercased() == "RECEIVABLE" }
        RoseLinesCard(theme: theme, title: title, trailing: "\(open.count) open") {
            if open.isEmpty {
                RoseEmptyCard(theme: theme, title: "No open AR", bodyText: "Record a credit sale before collecting.")
            } else {
                ForEach(open.prefix(6)) { row in
                    Button {
                        draft.parentEventId = row.id
                        if draft.counterparty.isEmpty { draft.counterparty = row.counterpartyName ?? "" }
                        if draft.amount.isEmpty { draft.amount = row.amount }
                        if draft.title.isEmpty { draft.title = row.title }
                    } label: {
                        HStack {
                            VStack(alignment: .leading, spacing: 2) {
                                Text(row.title)
                                    .font(.plusJakarta(size: 13, weight: .semibold))
                                    .foregroundStyle(OperatingV12.title)
                                Text("Outstanding \(row.amount)")
                                    .font(.plusJakarta(size: 10))
                                    .foregroundStyle(OperatingV12.subtitle)
                            }
                            Spacer()
                            if draft.parentEventId == row.id {
                                Image(systemName: "checkmark.circle.fill").foregroundStyle(theme.accent)
                            }
                        }
                        .padding(10)
                        .background(draft.parentEventId == row.id ? theme.accent.opacity(0.14) : theme.canvas.opacity(0.3))
                        .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }

    @ViewBuilder
    private func openPayables(title: String) -> some View {
        let open = (model.projection?.paymentReadiness ?? []).filter { $0.relevance.uppercased() != "RECEIVABLE" }
        RoseLinesCard(theme: theme, title: title, trailing: "\(open.count) open") {
            if open.isEmpty {
                RoseEmptyCard(theme: theme, title: "Nothing to pay", bodyText: "Open purchases and expenses appear here.")
            } else {
                ForEach(open.prefix(6)) { row in
                    Button {
                        draft.parentEventId = row.id
                        if draft.counterparty.isEmpty { draft.counterparty = row.counterpartyName ?? "" }
                        if draft.amount.isEmpty { draft.amount = row.amount }
                        if draft.title.isEmpty { draft.title = row.title }
                    } label: {
                        HStack {
                            VStack(alignment: .leading, spacing: 2) {
                                Text(row.title)
                                    .font(.plusJakarta(size: 13, weight: .semibold))
                                    .foregroundStyle(OperatingV12.title)
                                Text(row.counterpartyName ?? "Payable")
                                    .font(.plusJakarta(size: 10))
                                    .foregroundStyle(OperatingV12.subtitle)
                            }
                            Spacer()
                            Text(formattedMoney(row.amount))
                                .font(.plusJakarta(size: 13, weight: .semibold))
                                .foregroundStyle(theme.accent)
                            if draft.parentEventId == row.id {
                                Image(systemName: "checkmark.circle.fill").foregroundStyle(theme.accent)
                            }
                        }
                        .padding(10)
                        .background(draft.parentEventId == row.id ? theme.accent.opacity(0.14) : theme.canvas.opacity(0.3))
                        .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }

    @ViewBuilder
    private var skuSection: some View {
        let skus = catalogMasters
        if !skus.isEmpty {
            RoseFieldLabel(text: "Catalogue")
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(skus.prefix(16)) { sku in
                        RoseModeChip(theme: theme, text: sku.name, selected: draft.masterId == sku.id) {
                            draft.masterId = sku.id
                            draft.title = sku.name
                            if draft.quantity.isEmpty { draft.quantity = "1" }
                            if draft.unit.isEmpty { draft.unit = sku.unit ?? "pc" }
                            if draft.itemPrice.isEmpty {
                                draft.itemPrice = sku.sellPrice ?? sku.purchasePrice ?? ""
                            }
                        }
                    }
                }
            }
        }
    }

    @ViewBuilder
    private func partyChips(kind: String) -> some View {
        let rows = model.masters.filter {
            if kind == "CUSTOMER" {
                return ["CUSTOMER", "BUYER"].contains($0.kind.uppercased())
            }
            return $0.kind.uppercased() == kind
        }
        ForEach(rows.prefix(4)) { row in
            RosePickerRow(theme: theme, label: row.name, value: row.gstin.map { "GSTIN \($0)" } ?? row.kind) {
                draft.counterparty = row.name
            }
        }
    }

    private func roseMoneyRow(_ label: String, _ value: String) -> some View {
        HStack {
            Text(label)
                .font(.plusJakarta(size: 12))
                .foregroundStyle(OperatingV12.subtitle)
            Spacer()
            Text(value)
                .font(.plusJakarta(size: 14, weight: .semibold))
                .foregroundStyle(OperatingV12.title)
        }
    }

    private var fieldTitle: String {
        switch layout {
        case .purchase: return "What was bought"
        case .payment: return "Paid to"
        case .spend: return "Spent on"
        default: return "What happened"
        }
    }

    private var counterpartyLabel: String {
        switch layout {
        case .purchase, .payment: return "Supplier"
        case .collection: return "Customer"
        default: return "Customer (optional)"
        }
    }

    private var needsAmount: Bool {
        guard let layout else { return false }
        return [.sale, .purchase, .collection, .payment, .spend, .budget].contains(layout)
    }

    private var catalogMasters: [OperatingMasterPayload] {
        model.masters.filter { ["SKU", "PRODUCT", "BATCH", "ITEM"].contains($0.kind.uppercased()) }
    }

    private var locationMasters: [OperatingMasterPayload] {
        model.masters.filter { ["LOCATION", "STORE", "CHANNEL"].contains($0.kind.uppercased()) }
    }

    private var openPurchaseOrders: [OperatingActivityPayload] {
        (model.projection?.activity ?? []).filter {
            $0.eventType.uppercased() == "ORDER" &&
                (($0.orderSide ?? "").uppercased() == "PURCHASE" || $0.title.localizedCaseInsensitiveContains("PO"))
        }
    }

    private var selectedReceivable: OperatingPaymentPayload? {
        let open = (model.projection?.paymentReadiness ?? []).filter { $0.relevance.uppercased() == "RECEIVABLE" }
        return open.first { $0.id == draft.parentEventId } ?? open.first
    }

    private var selectedPayable: OperatingPaymentPayload? {
        let open = (model.projection?.paymentReadiness ?? []).filter { $0.relevance.uppercased() != "RECEIVABLE" }
        return open.first { $0.id == draft.parentEventId } ?? open.first
    }

    private var filledLineCount: Int {
        draft.lines.filter { !$0.name.trimmingCharacters(in: .whitespaces).isEmpty }.count
    }

    private var periodDefault: String {
        let formatter = DateFormatter()
        formatter.dateFormat = "MMM yyyy"
        return formatter.string(from: Date())
    }

    private func formattedMoney(_ raw: String?) -> String {
        guard let raw, !raw.isEmpty, let value = Double(raw) else { return "—" }
        let formatter = NumberFormatter()
        formatter.numberStyle = .currency
        formatter.currencyCode = model.currency.isEmpty ? "INR" : model.currency
        formatter.maximumFractionDigits = 2
        formatter.minimumFractionDigits = 0
        if model.currency.isEmpty || model.currency == "INR" {
            formatter.currencySymbol = "₹"
            formatter.locale = Locale(identifier: "en_IN")
        }
        return formatter.string(from: NSNumber(value: value)) ?? raw
    }

    private func ensureSeedLines() {
        if draft.lines.count < 2 {
            while draft.lines.count < 2 { draft.lines.append(RetailLineDraft()) }
        }
        if upper == "COLLECTION", draft.tender == "UPI" || draft.tender.isEmpty {
            draft.tender = "UPI"
        }
        if upper == "PAYMENT", draft.tender == "UPI" {
            draft.tender = "Bank"
        }
        if upper == "SPEND", draft.paymentChoice == "Fully Paid" {
            draft.paymentChoice = "Paid now"
        }
        if upper == "BUDGET" || upper == "TARGET", draft.revisionKind == "Budget" {
            draft.revisionKind = "Sales target"
            if draft.amount.isEmpty {
                draft.amount = model.projection?.target?.value ?? ""
            }
        }
        if upper == "ISSUE" || upper == "UPDATE", draft.revisionKind == "Budget" {
            draft.revisionKind = "Issue"
        }
        if draft.periodChip.isEmpty { draft.periodChip = periodDefault }
    }

    private func syncAmountFromLines() {
        var total = goodsTotal
        if let freight = Decimal(string: draft.freight), upper == "PURCHASE" {
            total += freight
        }
        draft.amount = NSDecimalNumber(decimal: total).stringValue
    }

    private func seedFavouriteLines(purchase: Bool) {
        for (idx, sku) in catalogMasters.prefix(2).enumerated() {
            if idx < draft.lines.count {
                draft.lines[idx].sku = sku.code ?? String(sku.name.prefix(6)).uppercased()
                draft.lines[idx].name = sku.name
                draft.lines[idx].rate = (purchase ? sku.purchasePrice : sku.sellPrice)
                    ?? sku.purchasePrice ?? sku.sellPrice ?? ""
                draft.lines[idx].unit = sku.unit ?? "pcs"
                draft.lines[idx].hsn = sku.hsn ?? ""
            }
        }
        syncAmountFromLines()
    }

    private func addCatalogLine(_ sku: OperatingMasterPayload, purchase: Bool) {
        let line = RetailLineDraft(
            sku: sku.code ?? String(sku.name.prefix(6)).uppercased(),
            name: sku.name,
            qty: "1",
            rate: (purchase ? sku.purchasePrice : sku.sellPrice) ?? sku.purchasePrice ?? sku.sellPrice ?? "",
            hsn: sku.hsn ?? "",
            unit: sku.unit ?? "pcs"
        )
        if let idx = draft.lines.firstIndex(where: { $0.name.isEmpty }) {
            draft.lines[idx] = line
        } else {
            draft.lines.append(line)
        }
        syncAmountFromLines()
    }

    private func applyPrefill() {
        guard !didPrefill, let item = browserPrefill else { return }
        didPrefill = true
        draft.masterId = item.id
        draft.title = item.name
        draft.quantity = "1"
        if let unit = item.unit { draft.unit = unit }
        if let price = item.unitPrice { draft.itemPrice = price }
        if draft.lines.first?.name.isEmpty == true {
            draft.lines[0].name = item.name
            draft.lines[0].rate = item.unitPrice ?? ""
            draft.lines[0].unit = item.unit ?? "pcs"
            syncAmountFromLines()
        }
    }

    private func save() async {
        guard canSave else { return }
        saving = true
        defer { saving = false }
        if ["PURCHASE", "ORDER"].contains(upper) { syncAmountFromLines() }
        let input = RetailCaptureEventBuilder(mode: mode, draft: draft, model: model).build()
        let ok = await model.record(input)
        if ok { dismiss() }
    }
}

// MARK: - Draft + event builder

private struct RetailLineDraft: Identifiable, Equatable {
    var id = UUID()
    var sku = ""
    var name = ""
    var qty = "1"
    var rate = ""
    var hsn = ""
    var unit = "pcs"

    var lineTotal: Decimal {
        let q = Decimal(string: qty) ?? 0
        let r = Decimal(string: rate) ?? 0
        return q * r
    }
}

private struct RetailCaptureDraft {
    var title = ""
    var amount = ""
    var note = ""
    var counterparty = ""
    var channel = ""
    var quantity = ""
    var unit = ""
    var itemPrice = ""
    var masterId: String?
    var parentEventId: String?
    var occurredAt: String? = ISO8601DateFormatter().string(from: Date())
    var dueAt: String?
    var paymentChoice = "Fully Paid"
    var entryMode = "Quick receive"
    var goodsStatus = "Deliver Later"
    var stockAction = "IN"
    var stockReason = ""
    var orderSide = "Sales order"
    var statusChoice = "Ready to pick"
    var statusMode = "Dispatch"
    var staff = ""
    var revisionKind = "Budget"
    var tender = "UPI"
    var goodsMode = "Goods"
    var scanQuery = ""
    var freight = "0"
    var location = "Back store"
    var linkedPo = ""
    var reference = ""
    var receiptPrint = "On"
    var expenseMode = "Store"
    var typeChip = "Packaging"
    var expenseType = ""
    var periodChip = ""
    var periodCustom = ""
    var stateChip = "Confirmed"
    var placeOfSupply = "Maharashtra · intra-state"
    var dueLabel = "Today"
    var notify = "Customer WhatsApp"
    var severity = "Alert"
    var link = "Register · today"
    var lines: [RetailLineDraft] = [RetailLineDraft(), RetailLineDraft()]
}

private struct RetailCaptureEventBuilder {
    let mode: String
    let draft: RetailCaptureDraft
    let model: BusinessOperatingModel

    private var upper: String { mode.uppercased() }
    private var layout: OperatingCaptureLayout? { OperatingCaptureLayout.forMode(mode) }

    private var filledLines: [RetailLineDraft] {
        draft.lines.filter { !$0.name.trimmingCharacters(in: .whitespaces).isEmpty }
    }

    var canSave: Bool {
        switch upper {
        case "STATUS":
            return draft.parentEventId != nil
        case "ORDER":
            return !filledLines.isEmpty || !draft.amount.trimmingCharacters(in: .whitespaces).isEmpty
                || draft.masterId != nil || !draft.title.trimmingCharacters(in: .whitespaces).isEmpty
        case "ISSUE", "UPDATE":
            return !draft.title.trimmingCharacters(in: .whitespaces).isEmpty
        case "COLLECTION", "PAYMENT":
            return draft.parentEventId != nil && !draft.amount.trimmingCharacters(in: .whitespaces).isEmpty
        case "PURCHASE":
            return !draft.counterparty.trimmingCharacters(in: .whitespaces).isEmpty
                && !filledLines.isEmpty
                && (Decimal(string: draft.amount).map { $0 > 0 } ?? false)
        default:
            return !draft.title.trimmingCharacters(in: .whitespaces).isEmpty
                || !draft.amount.trimmingCharacters(in: .whitespaces).isEmpty
                || draft.masterId != nil
        }
    }

    func build() -> OperatingEventInput {
        var contract = OperatingContractInput()
        var input = OperatingEventInput(
            momentId: model.momentId,
            eventType: eventType,
            title: resolvedTitle,
            amount: draft.amount.isEmpty ? nil : draft.amount,
            note: combinedNote(),
            channelName: resolvedChannel,
            counterpartyName: draft.counterparty.isEmpty ? nil : draft.counterparty,
            occurredAt: draft.occurredAt,
            parentEventId: draft.parentEventId,
            quantity: draft.quantity.isEmpty ? filledLines.first?.qty : draft.quantity,
            unit: draft.unit.isEmpty ? filledLines.first?.unit : draft.unit,
            dueAt: draft.dueAt,
            masterId: draft.masterId,
            paymentStatus: paymentStatus,
            revisesEventId: upper == "STATUS" ? draft.parentEventId : nil
        )

        if layout == .stock {
            contract.stockAction = draft.stockAction == "OUT" ? "REDUCE" : "IN"
            contract.stockReason = draft.stockReason.isEmpty ? draft.stockAction : draft.stockReason
        }

        if layout == .sale {
            let itemized = draft.entryMode.localizedCaseInsensitiveContains("Item")
            contract.entryMode = itemized ? "ITEMIZED" : "QUICK"
            mapPayment(&contract)
            if itemized { mapGoods(&contract) }
        }

        if layout == .purchase {
            contract.entryMode = "ITEMIZED"
            contract.goodsStatus = "RECEIVED"
            contract.receiptState = "PARTIAL"
            contract.locationName = draft.location
            switch draft.tender {
            case "Pay now": contract.paymentState = "PAID"
            case "Part": contract.paymentState = "PART_PAID"
            default: contract.paymentState = "PAY_LATER"
            }
        }

        if layout == .spend {
            let type = draft.typeChip == "Other" ? (draft.expenseType.isEmpty ? "Other" : draft.expenseType) : draft.typeChip
            contract.expenseCategory = "\(draft.expenseMode)/\(type)"
            contract.paymentState = draft.paymentChoice == "Paid now" ? "PAID" : "PAY_LATER"
        }

        if upper == "ORDER" {
            contract.orderSide = draft.orderSide == "Purchase order" ? "PURCHASE" : "SALES"
            contract.orderStatus = orderStatusToken(draft.stateChip)
            contract.estimatedAmount = draft.amount.isEmpty ? nil : draft.amount
            contract.promisedAt = draft.dueAt
            contract.locationName = draft.location
            if draft.stateChip == "Hold" { contract.holdStatus = "HELD" }
        }

        if upper == "STATUS" {
            contract.orderStatus = statusToken(draft.statusChoice)
            contract.staffName = draft.staff.isEmpty ? nil : draft.staff
            contract.deliveryStatus = draft.statusMode
            contract.shipmentStatus = statusToken(draft.statusChoice)
        }

        if layout == .budget || upper == "TARGET" {
            input.effectiveFrom = draft.occurredAt
            contract.estimatedAmount = draft.amount.isEmpty ? nil : draft.amount
        }

        if upper == "ISSUE" || upper == "UPDATE" {
            contract.staffName = draft.staff.isEmpty ? nil : draft.staff
        }

        if layout == .collection || layout == .payment, let parentId = draft.parentEventId, !draft.amount.isEmpty {
            contract.paymentState = layout == .collection ? "RECEIVED" : "PAID"
            contract.receivedAmount = draft.amount
            contract.allocations = [OperatingAllocationInput(sourceId: parentId, amount: draft.amount)]
        }

        let lineItems: [OperatingEventInput.OperatingLineItem]
        if !filledLines.isEmpty {
            lineItems = filledLines.map {
                .init(
                    itemName: $0.name,
                    quantity: $0.qty,
                    unit: $0.unit,
                    unitPrice: $0.rate.isEmpty ? nil : $0.rate,
                    masterId: nil
                )
            }
        } else if let masterId = draft.masterId {
            lineItems = [
                .init(
                    itemName: draft.title,
                    quantity: draft.quantity.isEmpty ? nil : draft.quantity,
                    unit: draft.unit.isEmpty ? nil : draft.unit,
                    unitPrice: draft.itemPrice.isEmpty ? nil : draft.itemPrice,
                    masterId: masterId
                ),
            ]
        } else {
            lineItems = []
        }
        if !lineItems.isEmpty { input.lineItems = lineItems }

        input.contract = contract
        return input
    }

    private var resolvedTitle: String {
        let trimmed = draft.title.trimmingCharacters(in: .whitespaces)
        if !trimmed.isEmpty { return trimmed }
        switch upper {
        case "PURCHASE":
            return filledLines.first.map { "Purchase · \($0.name)" } ?? "Purchase · \(draft.counterparty)"
        case "ORDER":
            return draft.orderSide == "Purchase order"
                ? "PO · \(draft.counterparty.isEmpty ? "Supplier" : draft.counterparty)"
                : "Sales order · \(draft.counterparty.isEmpty ? "Customer" : draft.counterparty)"
        case "SPEND":
            let type = draft.typeChip == "Other" ? (draft.expenseType.isEmpty ? "Other" : draft.expenseType) : draft.typeChip
            return "\(draft.expenseMode) · \(type)"
        case "COLLECTION":
            return "Collection · \(draft.counterparty.isEmpty ? "AR" : draft.counterparty)"
        case "PAYMENT":
            return "Payment · \(draft.counterparty.isEmpty ? "AP" : draft.counterparty)"
        case "BUDGET", "TARGET":
            return "Budget · \(draft.revisionKind)"
        case "STATUS":
            return trimmed.isEmpty ? "Status update" : trimmed
        default:
            return trimmed
        }
    }

    private var resolvedChannel: String? {
        switch upper {
        case "PURCHASE": return draft.goodsMode
        case "COLLECTION", "PAYMENT": return draft.tender
        default: return draft.channel.isEmpty ? nil : draft.channel
        }
    }

    private var eventType: String {
        if upper == "STATUS" { return "ORDER" }
        if upper == "ISSUE" || upper == "UPDATE" { return "UPDATE" }
        if upper == "TARGET" { return "TARGET" }
        switch layout {
        case .sale: return "SALE"
        case .purchase: return "PURCHASE"
        case .collection: return "COLLECTION"
        case .payment: return "PAYMENT"
        case .spend: return "SPEND"
        case .stock: return "STOCK"
        case .budget: return "BUDGET"
        case .order: return "ORDER"
        default: return upper
        }
    }

    private var paymentStatus: String? {
        switch layout {
        case .collection:
            return "COLLECTED"
        case .payment:
            return "PAID"
        case .purchase:
            switch draft.tender {
            case "Pay now": return "PAID"
            case "Part": return "PARTIAL"
            default: return "OPEN"
            }
        case .spend:
            return draft.paymentChoice == "Paid now" ? "PAID" : "OPEN"
        default:
            return nil
        }
    }

    private func combinedNote() -> String? {
        var parts: [String] = []
        if !draft.note.isEmpty { parts.append(draft.note) }
        if upper == "PURCHASE" {
            if !draft.linkedPo.isEmpty { parts.append("Linked PO: \(draft.linkedPo)") }
            parts.append("Location: \(draft.location)")
        }
        if upper == "COLLECTION" || upper == "PAYMENT" {
            parts.append("Tender: \(draft.tender)")
            if !draft.reference.isEmpty { parts.append("Ref: \(draft.reference)") }
            if upper == "COLLECTION" { parts.append("Receipt print: \(draft.receiptPrint)") }
        }
        if upper == "SPEND" {
            let period = draft.periodChip == "Custom" ? draft.periodCustom : draft.periodChip
            if !period.isEmpty { parts.append("Period: \(period)") }
        }
        if upper == "ORDER" {
            parts.append("Deliver: \(draft.location)")
            parts.append("POS: \(draft.placeOfSupply)")
            parts.append("State: \(draft.stateChip)")
        }
        if upper == "STATUS" {
            if !draft.staff.isEmpty { parts.append("Assigned: \(draft.staff)") }
            parts.append("Mode: \(draft.statusMode)")
            parts.append("Due: \(draft.dueLabel)")
            parts.append("Notify: \(draft.notify)")
        }
        if upper == "BUDGET" || upper == "TARGET" {
            let period = draft.periodChip == "Custom" ? draft.periodCustom : draft.periodChip
            if !period.isEmpty { parts.append("Period: \(period)") }
            if !draft.staff.isEmpty { parts.append("Owner: \(draft.staff)") }
        }
        if upper == "ISSUE" || upper == "UPDATE" {
            parts.append("Severity: \(draft.severity)")
            if !draft.staff.isEmpty { parts.append("Owner: \(draft.staff)") }
            parts.append("Link: \(draft.link)")
        }
        if needsTenderLegacy, !draft.tender.isEmpty, !["PURCHASE", "COLLECTION", "PAYMENT", "SPEND"].contains(upper) {
            parts.append("Tender: \(draft.tender)")
        }
        return parts.isEmpty ? nil : parts.joined(separator: " · ")
    }

    private var needsTenderLegacy: Bool {
        [.sale].contains(layout)
    }

    private func mapPayment(_ contract: inout OperatingContractInput) {
        switch draft.paymentChoice {
        case "Fully Paid", "Paid", "Received", "Paid now":
            contract.paymentState = layout == .purchase ? "PAID" : "RECEIVED"
            contract.receivedAmount = draft.amount.isEmpty ? nil : draft.amount
        case "Partially Paid", "Part paid", "Part":
            contract.paymentState = "PART_PAID"
        default:
            contract.paymentState = "PAY_LATER"
        }
    }

    private func mapGoods(_ contract: inout OperatingContractInput) {
        switch draft.goodsStatus {
        case "Fully Delivered": contract.goodsStatus = "DELIVERED"
        case "Partially Delivered": contract.goodsStatus = "PARTIAL"
        default: contract.goodsStatus = "LATER"
        }
    }

    private func statusToken(_ choice: String) -> String {
        switch choice {
        case "Picking": return "PICKING"
        case "Ready": return "READY"
        case "Dispatched", "Dispatch": return "DISPATCHED"
        case "Fulfil": return "FULFILLED"
        case "Ready to pick": return "READY_TO_PICK"
        default: return "READY_TO_PICK"
        }
    }

    private func orderStatusToken(_ chip: String) -> String {
        switch chip {
        case "Hold": return "ON_HOLD"
        case "Draft": return "DRAFT"
        case "Partial": return "PARTIAL"
        default: return "OPEN"
        }
    }
}

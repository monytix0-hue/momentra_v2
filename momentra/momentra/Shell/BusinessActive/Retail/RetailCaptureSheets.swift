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
    @State private var createMasterKind: RetailMasterFormKind?
    @State private var showBarcodeScanner = false
    @State private var showStockSkuPicker = false
    @State private var scanBanner: String?

    private var upper: String { mode.uppercased() }
    private var layout: OperatingCaptureLayout? { OperatingCaptureLayout.forMode(mode) }

    private var title: String {
        if RetailCaptureCopy.moneyModes.contains(upper) || RetailCaptureCopy.opsModes.contains(upper) {
            return RetailCaptureCopy.sheetTitle(upper, orderSide: draft.orderSide)
        }
        switch upper {
        case "ISSUE", "UPDATE", "APPROVAL":
            return RetailCaptureCopy.sheetTitle("ISSUE")
        case "SPEND":
            return RetailCaptureCopy.sheetTitle("SPEND")
        case "BUDGET", "TARGET":
            return RetailCaptureCopy.sheetTitle("BUDGET")
        default:
            return layout?.label ?? mode.capitalized
        }
    }

    private var qaCode: String {
        if RetailCaptureCopy.moneyModes.contains(upper)
            || RetailCaptureCopy.opsModes.contains(upper)
            || ["SPEND", "BUDGET", "TARGET", "ISSUE", "UPDATE"].contains(upper)
        {
            return RetailCaptureCopy.qaCode(upper)
        }
        return layout?.qaCode ?? "RTL-QA"
    }

    private var contextLine: String {
        let brand = model.title.isEmpty ? "Harbor Mart" : model.title
        if RetailCaptureCopy.moneyModes.contains(upper) || RetailCaptureCopy.opsModes.contains(upper) {
            return RetailCaptureCopy.contextLine(
                upper,
                brand: brand,
                location: draft.location,
                orderSide: draft.orderSide
            )
        }
        switch upper {
        case "SALE": return "\(brand) · Sale ≠ receipt · stock only on dispatch"
        case "SPEND": return RetailCaptureCopy.contextLine("SPEND", brand: brand)
        case "BUDGET", "TARGET": return RetailCaptureCopy.contextLine("BUDGET", brand: brand)
        case "ISSUE", "UPDATE", "APPROVAL": return RetailCaptureCopy.contextLine("ISSUE", brand: brand)
        default: return "\(brand) · \(RetailCaptureCopy.tagline)"
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

                if let scanBanner {
                    Text(scanBanner)
                        .font(.plusJakarta(size: 12, weight: .medium))
                        .foregroundStyle(theme.negative)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }

                RoseWhyBox(theme: theme, text: whyText)

                if let writeError = model.writeError, ["COLLECTION", "PAYMENT"].contains(upper) {
                    Text(writeError)
                        .font(.plusJakarta(size: 12, weight: .medium))
                        .foregroundStyle(theme.negative)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }

                RoseStickyFooter(
                    theme: theme,
                    title: saving ? "Saving…" : saveLabel,
                    caption: footerCaption,
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
            applyPrefill()
            ensureSeedLines()
        }
        .task {
            if model.masters.isEmpty { await model.loadMasters() }
        }
        .sheet(item: $createMasterKind) { kind in
            RetailMasterFormSheet(
                theme: theme,
                kind: kind,
                model: model,
                onSave: { draft in
                    let created = await model.createMaster(draft)
                    let ok = created != nil && model.writeError == nil
                    if ok { await model.loadMasters() }
                    return ok
                },
                onUpdate: { id, draft, version in
                    let updated = await model.updateMaster(
                        id: id,
                        draft: draft,
                        expectedVersion: version
                    )
                    let ok = updated != nil && model.writeError == nil
                    if ok { await model.loadMasters() }
                    return ok
                }
            )
        }
        .fullScreenCover(isPresented: $showBarcodeScanner) {
            RetailBarcodeScanner(
                title: upper == "STOCK" ? "Scan SKU / barcode" : "Scan invoice / SKU",
                subtitle: upper == "STOCK"
                    ? "Point at a product barcode or QR"
                    : "Point at a supplier invoice QR or product barcode",
                onCode: { code in
                    showBarcodeScanner = false
                    if upper == "STOCK" {
                        draft.scanQuery = code
                        appendStockScanLine()
                    } else {
                        applyPurchaseScan(code)
                    }
                },
                onDismiss: { showBarcodeScanner = false }
            )
        }
    }

    private var showsSharedMoreDetails: Bool {
        !["PURCHASE", "COLLECTION", "PAYMENT", "SPEND", "ORDER", "STATUS", "BUDGET", "TARGET", "ISSUE", "UPDATE", "STOCK"].contains(upper)
    }

    private var saveLabel: String {
        let money = formattedMoney(draft.amount.isEmpty ? goodsTotalString : draft.amount)
        if RetailCaptureCopy.moneyModes.contains(upper) {
            return canSave ? RetailCaptureCopy.cta(upper, amount: money) : RetailCaptureCopy.cta(upper)
        }
        switch upper {
        case "STATUS":
            return RetailCaptureCopy.cta("STATUS")
        case "ORDER":
            return canSave
                ? RetailCaptureCopy.cta("ORDER", amount: money, orderSide: draft.orderSide)
                : RetailCaptureCopy.cta("ORDER", orderSide: draft.orderSide)
        case "BUDGET", "TARGET":
            return canSave
                ? RetailCaptureCopy.cta("BUDGET", amount: money)
                : RetailCaptureCopy.cta("BUDGET")
        case "SPEND":
            return canSave ? "Post expense · \(money)" : "Post expense"
        case "ISSUE", "UPDATE", "APPROVAL":
            return RetailCaptureCopy.cta("ISSUE", revisionKind: draft.revisionKind)
        case "STOCK":
            return RetailCaptureCopy.cta("STOCK")
        default:
            return "Save · \(title)"
        }
    }

    private var footerCaption: String {
        RetailCaptureCopy.footerCaption(upper, orderSide: draft.orderSide)
    }

    private var stickyMeta: String {
        switch layout {
        case .sale: return draft.paymentChoice
        case .collection: return "Allocates to open AR"
        case .payment: return "Settles open AP"
        case .order:
            return draft.orderSide == "Purchase order"
                ? "Commitment · no AP"
                : "Commitment · no revenue"
        case .stock: return "Floor count · \(draft.location.isEmpty ? "Register aisle" : draft.location)"
        case .purchase: return draft.tender
        case .spend: return draft.paymentChoice
        case .budget: return "Versioned · plan"
        case .update, .approval: return "Approval ≠ disbursement"
        default:
            if upper == "STATUS" { return "Ops only · no revenue" }
            return "Draft · not yet saved"
        }
    }

    private var whyText: String {
        if RetailCaptureCopy.moneyModes.contains(upper) || RetailCaptureCopy.opsModes.contains(upper) {
            return RetailCaptureCopy.why(upper, orderSide: draft.orderSide)
        }
        switch layout {
        case .spend:
            return RetailCaptureCopy.why("SPEND")
        case .budget:
            return RetailCaptureCopy.why("BUDGET")
        case .update:
            return RetailCaptureCopy.why("ISSUE")
        default:
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
        case "ISSUE", "UPDATE", "APPROVAL":
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

    // MARK: Purchase `2399:30130` — AP + receive · never double-cost

    @ViewBuilder
    private var purchaseBody: some View {
        RoseChipRow(
            theme: theme,
            options: RetailCaptureCopy.modeChips("PURCHASE"),
            selection: $draft.goodsMode
        )
        RoseFieldLabel(text: "Scan supplier invoice / SKU")
        RoseScanField(
            theme: theme,
            placeholder: RetailCaptureCopy.scanPlaceholder("PURCHASE"),
            query: $draft.scanQuery,
            actionLabel: "Scan"
        ) {
            let q = draft.scanQuery.trimmingCharacters(in: .whitespacesAndNewlines)
            if q.isEmpty {
                showBarcodeScanner = true
            } else {
                applyPurchaseScan(q)
            }
        }
        RoseLinesCard(
            theme: theme,
            title: "Receive lines",
            trailing: filledLineCount == 0
                ? "No lines yet"
                : "\(filledLineCount) line\(filledLineCount == 1 ? "" : "s") · full receive"
        ) {
            if draft.lines.isEmpty {
                RoseEmptyCard(
                    theme: theme,
                    title: "No receive lines",
                    bodyText: "Scan a barcode / invoice QR, or + Add line."
                )
            }
            ForEach($draft.lines) { $line in
                purchaseLineEditor(line: $line)
            }
            HStack {
                Button("+ Add line") {
                    draft.lines.append(RetailLineDraft())
                }
                .font(.plusJakarta(size: 13, weight: .bold))
                .foregroundStyle(RoseSheetChrome.label)
                Spacer()
                Button("From PO · Favourites") {
                    seedFavouriteLines(purchase: true)
                }
                .font(.plusJakarta(size: 11))
                .foregroundStyle(RoseSheetChrome.textBody)
            }
            .buttonStyle(.plain)
        }
        purchaseTotals
        RoseFieldLabel(text: "Tender / method")
        RoseChipRow(
            theme: theme,
            options: RetailCaptureCopy.tenderChips("PURCHASE"),
            selection: $draft.tender,
            style: .tender
        )
        RosePickerRow(
            theme: theme,
            label: "Supplier",
            value: draft.counterparty.isEmpty ? "Select supplier" : draft.counterparty
        ) {
            cycleSupplier()
        }
        RosePickerRow(theme: theme, label: "Location", value: draft.location) {
            cyclePurchaseLocation()
        }
        RosePickerRow(
            theme: theme,
            label: "Linked PO",
            value: draft.linkedPo.isEmpty ? "PO (optional)" : draft.linkedPo
        ) {
            draft.linkedPo = openPurchaseOrders.first?.title ?? (draft.linkedPo.isEmpty ? "PO-219 (optional)" : "")
        }
        RoseMoreDetails(title: "More details", hint: RetailCaptureCopy.purchaseMoreDetailsHint) {
            RoseFieldLabel(text: "Freight")
            RoseTextField(theme: theme, placeholder: "0", text: $draft.freight, keyboard: .decimalPad)
                .onChange(of: draft.freight) { _, _ in syncAmountFromLines() }
            RoseFieldLabel(text: "Note")
            RoseTextField(theme: theme, placeholder: "GRN note · supplier GSTIN", text: $draft.note)
        }
    }

    private var purchaseGstEstimate: Decimal? {
        RetailGstEstimate.from(
            amounts: draft.lines.map(\.lineTotal),
            rates: draft.lines.map { $0.gst.isEmpty ? nil : $0.gst },
            inclusive: false
        )
    }

    private var purchaseTotals: some View {
        RoseMoneyTotalsCard(
            theme: theme,
            heroLabel: RetailCaptureCopy.amountLabel("PURCHASE"),
            heroAmount: formattedMoney(draft.amount),
            statusEyebrow: draft.tender,
            statusValue: draft.tender == "Pay later" ? "AP open" : draft.tender,
            statusTone: draft.tender == "Pay later" ? .amber : .tender
        ) {
            RoseMoneyRow(label: "Goods", value: formattedMoney(goodsTotalString))
            RoseGstEstimateRow(
                theme: theme,
                label: "Input GST · CGST+SGST (est.)",
                estimate: purchaseGstEstimate,
                currency: model.currency,
                captionWhenKnown: "Estimate from line rates — GST posts only on Purchase post",
                captionWhenMissing: "Unavailable — rate missing on line / master"
            )
            RoseMoneyRow(
                label: "Freight",
                value: formattedMoney(draft.freight.isEmpty ? "0" : draft.freight)
            )
        }
    }

    @ViewBuilder
    private func purchaseLineEditor(line: Binding<RetailLineDraft>) -> some View {
        let unit = line.wrappedValue.unit.isEmpty ? "pcs" : line.wrappedValue.unit
        let meta = [
            "\(line.wrappedValue.qty) \(unit)",
            line.wrappedValue.rate.isEmpty ? nil : formattedMoney(line.wrappedValue.rate),
            line.wrappedValue.hsn.isEmpty ? nil : "HSN \(line.wrappedValue.hsn)",
        ].compactMap { $0 }.joined(separator: " · ")

        VStack(alignment: .leading, spacing: 6) {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 2) {
                    Text(line.wrappedValue.sku.isEmpty ? "SKU" : line.wrappedValue.sku)
                        .font(.plusJakarta(size: 10, weight: .bold))
                        .foregroundStyle(RoseSheetChrome.label)
                    RoseTextField(theme: theme, placeholder: "Item name", text: line.name)
                        .onChange(of: line.wrappedValue.name) { _, _ in syncAmountFromLines() }
                    if !line.wrappedValue.name.isEmpty {
                        Text(meta)
                            .font(.plusJakarta(size: 11))
                            .foregroundStyle(RoseSheetChrome.textBody)
                    }
                }
                Text(formattedMoney(NSDecimalNumber(decimal: line.wrappedValue.lineTotal).stringValue))
                    .font(.plusJakarta(size: 13, weight: .bold))
                    .foregroundStyle(OperatingV12.title)
            }
            HStack(spacing: 8) {
                RoseTextField(theme: theme, placeholder: "Qty", text: line.qty, keyboard: .decimalPad)
                    .onChange(of: line.wrappedValue.qty) { _, _ in syncAmountFromLines() }
                RoseTextField(theme: theme, placeholder: "Rate", text: line.rate, keyboard: .decimalPad)
                    .onChange(of: line.wrappedValue.rate) { _, _ in syncAmountFromLines() }
                RoseTextField(theme: theme, placeholder: "HSN", text: line.hsn)
            }
        }
        .padding(.vertical, 8)
        .overlay(alignment: .bottom) {
            Rectangle()
                .fill(Color(hex: "#4C0519").opacity(0.6))
                .frame(height: 1)
        }
    }

    // MARK: Collection `2399:30194` — allocate AR · never new revenue

    @ViewBuilder
    private var collectionBody: some View {
        openReceivables(title: "Allocate against")
        let openAr = Decimal(string: selectedReceivable?.amount ?? "0") ?? 0
        let receipt = Decimal(string: draft.amount) ?? 0
        let remaining = max(openAr - receipt, 0)

        RoseMoneyTotalsCard(
            theme: theme,
            heroLabel: RetailCaptureCopy.amountLabel("COLLECTION"),
            heroAmount: formattedMoney(draft.amount),
            statusEyebrow: "Method",
            statusValue: draft.tender,
            statusTone: .tender
        ) {
            RoseMoneyRow(
                label: "Open AR",
                value: formattedMoney(selectedReceivable?.amount),
                valueTone: .danger
            )
            RoseMoneyRow(
                label: "This receipt",
                value: formattedMoney(draft.amount.isEmpty ? "0" : draft.amount)
            )
            RoseMoneyRow(
                label: "Remaining",
                value: formattedMoney(NSDecimalNumber(decimal: remaining).stringValue)
            )
        }

        RoseFieldLabel(text: "Method")
        RoseChipRow(
            theme: theme,
            options: RetailCaptureCopy.tenderChips("COLLECTION"),
            selection: $draft.tender,
            style: .tender
        )
        RosePickerRow(
            theme: theme,
            label: "Customer",
            value: draft.counterparty.isEmpty ? "Select customer" : draft.counterparty
        ) {
            cycleCustomer()
        }
        RosePickerRow(
            theme: theme,
            label: "Reference",
            value: draft.reference.isEmpty ? "UPI / NEFT ref" : draft.reference
        ) {
            if draft.reference.isEmpty { draft.reference = "UPI/2810/9912" }
        }
        RosePickerRow(theme: theme, label: "Receipt print", value: draft.receiptPrint) {
            draft.receiptPrint = draft.receiptPrint == "On" ? "Off" : "On"
        }
        RoseFieldLabel(text: "Collect now")
        RoseTextField(theme: theme, placeholder: "0.00", text: $draft.amount, keyboard: .decimalPad)
        RoseMoreDetails(title: "More details", hint: RetailCaptureCopy.moreDetailsHint) {
            RoseTextField(theme: theme, placeholder: "Notes · Proof · Tags", text: $draft.note)
        }
    }

    // MARK: Payment `2399:30253` — settle AP · never new purchase cost

    @ViewBuilder
    private var paymentBody: some View {
        openPayables(title: "Allocate against")
        let openAp = Decimal(string: selectedPayable?.amount ?? "0") ?? 0
        let paid = Decimal(string: draft.amount) ?? 0
        let remaining = max(openAp - paid, 0)

        RoseMoneyTotalsCard(
            theme: theme,
            heroLabel: RetailCaptureCopy.amountLabel("PAYMENT"),
            heroAmount: formattedMoney(draft.amount),
            statusEyebrow: "Method",
            statusValue: draft.tender,
            statusTone: .tender
        ) {
            RoseMoneyRow(
                label: "Open AP",
                value: formattedMoney(selectedPayable?.amount),
                valueTone: .danger
            )
            RoseMoneyRow(
                label: "This payment",
                value: formattedMoney(draft.amount.isEmpty ? "0" : draft.amount)
            )
            RoseMoneyRow(
                label: "Remaining",
                value: formattedMoney(NSDecimalNumber(decimal: remaining).stringValue)
            )
        }

        RoseFieldLabel(text: "Method")
        RoseChipRow(
            theme: theme,
            options: RetailCaptureCopy.tenderChips("PAYMENT"),
            selection: $draft.tender,
            style: .tender
        )
        RosePickerRow(
            theme: theme,
            label: "Payee",
            value: draft.counterparty.isEmpty ? "Select payee" : draft.counterparty
        ) {
            cycleSupplier()
        }
        RosePickerRow(
            theme: theme,
            label: "Reference",
            value: draft.reference.isEmpty ? "NEFT/…" : draft.reference
        ) {
            if draft.reference.isEmpty { draft.reference = "NEFT/…" }
        }
        RosePickerRow(
            theme: theme,
            label: "Linked purchase",
            value: selectedPayable?.title ?? "—"
        ) {
            if let next = openPayablesList.first(where: { $0.id != draft.parentEventId }) ?? openPayablesList.first {
                draft.parentEventId = next.id
                draft.counterparty = next.counterpartyName ?? draft.counterparty
                draft.amount = next.amount
                draft.title = next.title
            }
        }
        RoseFieldLabel(text: "Pay now")
        RoseTextField(theme: theme, placeholder: "0.00", text: $draft.amount, keyboard: .decimalPad)
        RoseMoreDetails(title: "More details", hint: RetailCaptureCopy.moreDetailsHint) {
            RoseTextField(theme: theme, placeholder: "Notes · Proof · Tags", text: $draft.note)
        }
    }

    // MARK: Expense `2399:30306` — outside 23-node POS batch; rose money QA chrome + honesty Why

    @ViewBuilder
    private var expenseBody: some View {
        RoseChipRow(theme: theme, options: ["Store", "Ops", "Other"], selection: $draft.expenseMode)
        VStack(spacing: 8) {
            roseMoneyRow("Amount", formattedMoney(draft.amount.isEmpty ? "0" : draft.amount))
            RoseUnavailableGstRow(
                theme: theme,
                label: "GST (if any)",
                caption: "Unavailable — never invent GST on expense; payment allocates separately"
            )
            roseMoneyRow(
                "Cash impact",
                draft.paymentChoice == "Paid now" ? "Paid now" : "Pay later · AP later"
            )
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
                    Text(draft.paymentChoice == "Paid now" ? "Paid now" : "Pay later")
                        .font(.plusJakarta(size: 10, weight: .bold))
                        .foregroundStyle(RoseSheetChrome.label)
                    Text(draft.expenseType.isEmpty ? draft.typeChip : draft.expenseType)
                        .font(.plusJakarta(size: 14, weight: .bold))
                        .foregroundStyle(theme.accent)
                }
            }
        }
        .padding(12)
        .background(Color.black.opacity(0.24))
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .stroke(theme.accent.opacity(0.35), lineWidth: 1)
        )

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
            RoseTenderChips(
                theme: theme,
                tender: $draft.tender,
                splitTender: .constant(false),
                splitUpi: .constant(""),
                splitCash: .constant(""),
                splitCard: .constant(""),
                allowSplit: false,
                options: ["UPI", "Cash", "Card"]
            )
        }
    }

    // MARK: Stock Update `2399:30359` — Set current / Add / Reduce

    @ViewBuilder
    private var stockBody: some View {
        RoseChipRow(
            theme: theme,
            options: RetailCaptureCopy.modeChips("STOCK"),
            selection: $draft.stockAction
        )

        RoseFieldLabel(text: "Scan SKU / barcode")
        RoseScanField(
            theme: theme,
            placeholder: RetailCaptureCopy.scanPlaceholder("STOCK"),
            query: $draft.scanQuery,
            actionLabel: "Scan"
        ) {
            let q = draft.scanQuery.trimmingCharacters(in: .whitespacesAndNewlines)
            if q.isEmpty {
                showBarcodeScanner = true
            } else {
                appendStockScanLine()
            }
        }

        RoseLinesCard(
            theme: theme,
            title: "Count lines",
            trailing: "\(filledLineCount) SKU\(filledLineCount == 1 ? "" : "s") · \(draft.stockReason.isEmpty ? "cycle count" : draft.stockReason.lowercased())"
        ) {
            ForEach($draft.lines) { $line in
                stockLineEditor(line: $line)
            }
            HStack {
                Button("+ Add SKU") { showStockSkuPicker.toggle() }
                    .font(.plusJakarta(size: 13, weight: .bold))
                    .foregroundStyle(RoseSheetChrome.label)
                    .buttonStyle(.plain)
                Spacer()
                Button("Recent · Low stock") { showStockSkuPicker.toggle() }
                    .font(.plusJakarta(size: 11))
                    .foregroundStyle(RoseSheetChrome.textBody)
                    .buttonStyle(.plain)
            }
            .padding(.top, 4)

            if showStockSkuPicker {
                let usedCodes = Set(
                    draft.lines
                        .map { $0.sku.trimmingCharacters(in: .whitespacesAndNewlines).uppercased() }
                        .filter { !$0.isEmpty }
                )
                let usedNames = Set(
                    draft.lines
                        .map { $0.name.trimmingCharacters(in: .whitespacesAndNewlines).lowercased() }
                        .filter { !$0.isEmpty }
                )
                let picks = catalogMasters.filter { master in
                    let code = (master.code ?? "").trimmingCharacters(in: .whitespacesAndNewlines).uppercased()
                    let name = master.name.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
                    let codeFree = code.isEmpty || !usedCodes.contains(code)
                    return codeFree && !usedNames.contains(name)
                }.prefix(6)
                if picks.isEmpty {
                    Text(
                        catalogMasters.isEmpty
                            ? "No catalogue SKUs — add under Catalogue first."
                            : "No more catalogue SKUs to add."
                    )
                    .font(.plusJakarta(size: 11))
                    .foregroundStyle(OperatingV12.subtitle)
                } else {
                    VStack(spacing: 6) {
                        ForEach(Array(picks)) { sku in
                            RosePickerRow(
                                theme: theme,
                                label: stockSkuCodeLabel(sku),
                                value: stockSkuValueLabel(sku)
                            ) {
                                pickStockSku(sku)
                            }
                        }
                    }
                }
            }
        }

        RosePickerRow(theme: theme, label: "Reason", value: draft.stockReason.isEmpty ? "Cycle count" : draft.stockReason) {
            draft.stockReason = nextStockReason(draft.stockReason)
        }
        RosePickerRow(theme: theme, label: "Location", value: draft.location.isEmpty ? "Aisle 3" : draft.location) {
            draft.location = nextStockLocation(draft.location)
        }
        RosePickerRow(theme: theme, label: "Counted by", value: draft.staff.isEmpty ? "Ravi · floor" : draft.staff) {
            draft.staff = nextCountedBy(draft.staff)
        }

        RoseMoreDetails(title: "More details", hint: RetailCaptureCopy.stockMoreDetailsHint) {
            SetupDateTimeField(label: "Count date", isoValue: $draft.occurredAt)
            RoseFieldLabel(text: "Note")
            RoseTextField(theme: theme, placeholder: "Optional reference", text: $draft.note)
            RoseFieldLabel(text: "Counted by (custom)")
            RoseTextField(theme: theme, placeholder: "Name · role", text: $draft.staff)
        }
    }

    @ViewBuilder
    private func stockLineEditor(line: Binding<RetailLineDraft>) -> some View {
        // Name/SKU full-width above qty row — avoid crushing Item beside the stepper.
        VStack(alignment: .leading, spacing: 6) {
            Text(line.wrappedValue.sku.isEmpty ? "SKU" : line.wrappedValue.sku)
                .font(.plusJakarta(size: 10, weight: .bold))
                .foregroundStyle(RoseSheetChrome.label)
            if line.wrappedValue.name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                Text("Pick from + Add SKU or scan")
                    .font(.plusJakarta(size: 13))
                    .foregroundStyle(RoseSheetChrome.textBody)
            } else {
                Text(line.wrappedValue.name)
                    .font(.plusJakarta(size: 13, weight: .semibold))
                    .foregroundStyle(OperatingV12.title)
                    .lineLimit(2)
            }
            HStack(alignment: .center, spacing: 8) {
                Text(stockLineMeta(line.wrappedValue))
                    .font(.plusJakarta(size: 11))
                    .foregroundStyle(RoseSheetChrome.textBody)
                    .frame(maxWidth: .infinity, alignment: .leading)
                let qty = Int(line.wrappedValue.qty) ?? 0
                RoseStepper(theme: theme, quantity: qty, onDecrement: {
                    if qty > 0 {
                        line.wrappedValue.qty = String(qty - 1)
                    } else if draft.lines.count > 1 {
                        draft.lines.removeAll { $0.id == line.wrappedValue.id }
                    }
                }, onIncrement: {
                    line.wrappedValue.qty = String(qty + 1)
                })
                Text("→ \(line.wrappedValue.qty)")
                    .font(.plusJakarta(size: 13, weight: .bold))
                    .foregroundStyle(OperatingV12.title)
            }
        }
    }

    private func stockLineMeta(_ line: RetailLineDraft) -> String {
        let loc = draft.location.isEmpty ? "Aisle 3" : draft.location
        var bits: [String] = [loc]
        if !line.wasQty.isEmpty { bits.append("was \(line.wasQty)") }
        if !line.unit.isEmpty { bits.append(line.unit) }
        return bits.joined(separator: " · ")
    }

    private func pickStockSku(_ master: OperatingMasterPayload) {
        let code = (master.code ?? master.id).uppercased()
        let draftLine = RetailLineDraft(
            sku: code,
            name: master.name,
            unit: master.unit ?? "pcs"
        )
        if let idx = draft.lines.firstIndex(where: { $0.name.isEmpty }) {
            draft.lines[idx] = draftLine
        } else {
            draft.lines.append(draftLine)
        }
        showStockSkuPicker = false
    }

    private func appendStockScanLine() {
        let q = draft.scanQuery.trimmingCharacters(in: .whitespaces)
        guard !q.isEmpty else { return }
        guard let match = retailMatchCatalogSku(catalogMasters, query: q) else {
            scanBanner = "No product for this code — pick from catalogue or add under Catalogue."
            return
        }
        scanBanner = nil
        let sku = (match.code ?? String(q.prefix(8))).uppercased()
        let name = match.name
        let unit = match.unit ?? "pcs"
        if let idx = draft.lines.firstIndex(where: { $0.name.isEmpty }) {
            draft.lines[idx].name = name
            draft.lines[idx].sku = sku
            draft.lines[idx].unit = unit
        } else {
            draft.lines.append(RetailLineDraft(sku: sku, name: name, unit: unit))
        }
        draft.scanQuery = ""
    }

    private func nextStockReason(_ current: String) -> String {
        let options = ["Cycle count", "Opening count", "Stocktake", "Damage", "Waste", "Return", "Correction"]
        let idx = options.firstIndex(of: current.isEmpty ? "Cycle count" : current) ?? 0
        return options[(idx + 1) % options.count]
    }

    private func nextStockLocation(_ current: String) -> String {
        let options = ["Aisle 3", "Back store", "Register", "Floor"]
        let idx = options.firstIndex(of: current) ?? 0
        return options[(idx + 1) % options.count]
    }

    private func nextCountedBy(_ current: String) -> String {
        let options = ["Ravi · floor", "Counter · shift", "Owner"]
        let idx = options.firstIndex(of: current) ?? -1
        return options[(idx + 1) % options.count]
    }

    // MARK: Order PO/SO `2443:29784` / `2399:30418` — D1-A commitment only

    @ViewBuilder
    private var orderBody: some View {
        let isPo = draft.orderSide == "Purchase order"
        RoseChipRow(
            theme: theme,
            options: RetailCaptureCopy.modeChips("ORDER"),
            selection: $draft.orderSide
        )
        .onChange(of: draft.orderSide) { _, side in
            draft.stateChip = side == "Purchase order" ? "Sent" : "Confirmed"
            draft.location = side == "Purchase order" ? "Back store" : "Pickup · Andheri"
            draft.catalogCategory = "Staples"
            draft.placeOfSupply = side == "Purchase order"
                ? "Maharashtra (27) · intra-state"
                : "Maharashtra · CGST+SGST"
        }
        RoseChipRow(
            theme: theme,
            options: RetailCaptureCopy.orderStateChips(isPurchase: isPo),
            selection: $draft.stateChip
        )

        RoseSectionCard(
            theme: theme,
            label: isPo ? "Stock items" : "Catalogue",
            trailing: isPo ? "Master + on-hand" : "Tap Add · set qty on line"
        ) {
            RoseScanField(
                theme: theme,
                placeholder: isPo ? "Search SKU / item" : "Search SKU, name or barcode",
                query: $draft.scanQuery,
                actionLabel: "Scan",
                action: {}
            )
            RoseChipRow(
                theme: theme,
                options: isPo ? RetailCaptureCopy.poStockCategories : RetailCaptureCopy.soCatalogueCategories,
                selection: $draft.catalogCategory,
                style: .ticket
            )
            catalogueBrowser(purchase: isPo)
            RoseDenseCreateCta(
                theme: theme,
                title: isPo ? "+ New stock item" : "+ New product",
                hint: isPo ? "Name · unit · HSN · GST" : "Name · HSN · GST · MRP"
            ) {
                createMasterKind = isPo ? .stockItem : .product
            }
        }

        RoseLinesCard(
            theme: theme,
            title: isPo ? "PO lines" : "SO lines",
            trailing: orderLinesTrailing(isPo: isPo)
        ) {
            ForEach($draft.lines) { $line in
                orderLineEditor(line: $line)
            }
            HStack {
                Button("+ Add line") { draft.lines.append(RetailLineDraft()) }
                    .font(.plusJakarta(size: 13, weight: .bold))
                    .foregroundStyle(RoseSheetChrome.label)
                    .buttonStyle(.plain)
                Spacer()
                Text(isPo ? "From supplier catalogue" : "From favorites · Price list")
                    .font(.plusJakarta(size: 11))
                    .foregroundStyle(RoseSheetChrome.textBody)
            }
        }

        RoseMoneyTotalsCard(
            theme: theme,
            heroLabel: isPo ? "PO value" : "SO value",
            heroAmount: formattedMoney(goodsTotalString),
            statusEyebrow: "Status",
            statusValue: draft.stateChip,
            statusTone: .tender
        ) {
            let orderGst = RetailGstEstimate.from(
                amounts: draft.lines.map(\.lineTotal),
                rates: draft.lines.map { $0.gst.isEmpty ? nil : $0.gst },
                inclusive: !isPo
            )
            if isPo {
                RoseMoneyRow(label: "Goods", value: formattedMoney(goodsTotalString))
                RoseGstEstimateRow(
                    theme: theme,
                    label: "Est. input GST · on GRN",
                    estimate: orderGst,
                    currency: model.currency,
                    captionWhenKnown: "Estimate only — never posted from PO; GST on Purchase / GRN",
                    captionWhenMissing: "Unavailable — rate missing on line / master"
                )
                RoseMoneyRow(label: "Freight (est.)", value: "₹0")
            } else {
                RoseMoneyRow(label: "Lines value", value: formattedMoney(goodsTotalString))
                RoseGstEstimateRow(
                    theme: theme,
                    label: "Est. GST · on bill",
                    estimate: orderGst,
                    currency: model.currency,
                    captionWhenKnown: "Estimate only — GST posts on Sale / POS Charge",
                    captionWhenMissing: "Unavailable — rate missing on line / master"
                )
                RoseMoneyRow(
                    label: "Balance due on sale",
                    value: formattedMoney(goodsTotalString),
                    valueTone: .amber
                )
            }
        }

        RosePickerRow(
            theme: theme,
            label: isPo ? "Supplier" : "Customer",
            value: draft.counterparty.isEmpty ? (isPo ? "Select supplier" : "Select customer") : draft.counterparty
        ) {
            if isPo { cycleSupplier() } else { cycleCustomer() }
        }
        partyChips(kind: isPo ? "SUPPLIER" : "CUSTOMER")
        RoseFieldLabel(text: isPo ? "Expect by" : "Fulfil by")
        SetupDateTimeField(label: isPo ? "Expect by" : "Fulfil by", isoValue: $draft.dueAt)
        RosePickerRow(
            theme: theme,
            label: isPo ? "Deliver to" : "Ship / pickup",
            value: draft.location.isEmpty
                ? (isPo ? "Back store" : "Pickup · Andheri")
                : draft.location
        ) {
            draft.location = nextOrderLocation(isPo: isPo, current: draft.location)
        }
        RosePickerRow(theme: theme, label: "Place of supply", value: draft.placeOfSupply) {
            draft.placeOfSupply = isPo
                ? "Maharashtra (27) · intra-state"
                : "Maharashtra · CGST+SGST"
        }
        RoseMoreDetails(
            title: "More details",
            hint: isPo ? RetailCaptureCopy.orderPoMoreDetailsHint : RetailCaptureCopy.orderSoMoreDetailsHint
        ) {
            RoseTextField(
                theme: theme,
                placeholder: isPo ? "Notes · Terms · Tags" : "Notes · Delivery · Tags",
                text: $draft.note
            )
        }
    }

    private func orderLinesTrailing(isPo: Bool) -> String {
        let count = "\(filledLineCount) SKU\(filledLineCount == 1 ? "" : "s")"
        guard let due = draft.dueAt, !due.isEmpty else { return count }
        let stamp = shortDate(due)
        return isPo ? "\(count) · expect by \(stamp)" : "\(count) · fulfil by \(stamp)"
    }

    private func nextOrderLocation(isPo: Bool, current: String) -> String {
        let options = isPo
            ? ["Back store", "Andheri warehouse", "Floor"]
            : ["Pickup · Andheri", "Pickup · store", "Deliver · local"]
        let idx = options.firstIndex(of: current) ?? -1
        return options[(idx + 1) % options.count]
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
                .foregroundStyle(RoseSheetChrome.textBody)
        }
        ForEach(skus.prefix(6)) { sku in
            HStack(spacing: 8) {
                VStack(alignment: .leading, spacing: 2) {
                    Text(sku.name)
                        .font(.plusJakarta(size: 13, weight: .semibold))
                        .foregroundStyle(OperatingV12.title)
                    if purchase {
                        Text(
                            [
                                sku.unit,
                                "Back store",
                                "on-hand —",
                            ].compactMap { $0 }.joined(separator: " · ")
                        )
                        .font(.plusJakarta(size: 10))
                        .foregroundStyle(RoseSheetChrome.textMuted)
                    } else {
                        Text([sku.code, sku.unit, sku.hsn.map { "HSN \($0)" }].compactMap { $0 }.joined(separator: " · "))
                            .font(.plusJakarta(size: 10))
                            .foregroundStyle(RoseSheetChrome.textMuted)
                    }
                }
                Spacer(minLength: 4)
                Text(formattedMoney(purchase ? (sku.purchasePrice ?? sku.sellPrice) : (sku.sellPrice ?? sku.purchasePrice)))
                    .font(.plusJakarta(size: 13, weight: .semibold))
                    .foregroundStyle(RoseSheetChrome.label)
                Button("Add") {
                    addCatalogLine(sku, purchase: purchase)
                }
                .font(.plusJakarta(size: 12, weight: .bold))
                .foregroundStyle(Color.white)
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
                .background(theme.accent)
                .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
            }
            .padding(10)
            .background(RoseSheetChrome.field)
            .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
            .buttonStyle(.plain)
        }
    }

    @ViewBuilder
    private func orderLineEditor(line: Binding<RetailLineDraft>) -> some View {
        HStack(alignment: .center, spacing: 8) {
            VStack(alignment: .leading, spacing: 2) {
                Text(line.wrappedValue.sku.isEmpty ? "SKU" : line.wrappedValue.sku)
                    .font(.plusJakarta(size: 10, weight: .bold))
                    .foregroundStyle(RoseSheetChrome.label)
                RoseTextField(theme: theme, placeholder: "Item", text: line.name)
                    .onChange(of: line.wrappedValue.name) { _, _ in syncAmountFromLines() }
                Text(
                    [
                        line.wrappedValue.rate.isEmpty ? nil : formattedMoney(line.wrappedValue.rate),
                        line.wrappedValue.unit,
                        line.wrappedValue.hsn.isEmpty ? nil : "HSN \(line.wrappedValue.hsn)",
                        line.wrappedValue.hsn.isEmpty ? nil : "5%",
                    ].compactMap { $0 }.joined(separator: " · ")
                )
                .font(.plusJakarta(size: 10))
                .foregroundStyle(RoseSheetChrome.textBody)
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
                .font(.plusJakarta(size: 13, weight: .bold))
                .foregroundStyle(OperatingV12.title)
        }
        RoseTextField(theme: theme, placeholder: "Rate", text: line.rate, keyboard: .decimalPad)
            .onChange(of: line.wrappedValue.rate) { _, _ in syncAmountFromLines() }
    }

    // MARK: Status / Budget / Issue

    @ViewBuilder
    private var statusBody: some View {
        // Figma `2399:30482` — Dispatch / Fulfil / Pick are ops modes only; never posts SALE/STOCK.
        RoseChipRow(
            theme: theme,
            options: RetailCaptureCopy.modeChips("STATUS"),
            selection: $draft.statusMode
        )
        openOrdersPicker
        RoseFieldLabel(text: "Next status")
        RoseChipRow(
            theme: theme,
            options: RetailCaptureCopy.statusNextChips(),
            selection: $draft.statusChoice,
            style: .tender
        )
        RosePickerRow(
            theme: theme,
            label: "Assignee",
            value: draft.staff.isEmpty ? "Ravi · floor" : draft.staff
        ) {
            draft.staff = nextCountedBy(draft.staff)
        }
        RosePickerRow(
            theme: theme,
            label: "Due",
            value: draft.dueLabel.isEmpty ? "Today · 6pm" : draft.dueLabel
        ) {
            draft.dueLabel = nextStatusDue(draft.dueLabel)
        }
        RosePickerRow(theme: theme, label: "Notify", value: draft.notify) {
            draft.notify = draft.notify == "Customer WhatsApp" ? "None" : "Customer WhatsApp"
        }
        RoseMoreDetails(title: "More details", hint: RetailCaptureCopy.statusMoreDetailsHint) {
            RoseFieldLabel(text: "Update note")
            RoseTextField(theme: theme, placeholder: "What changed", text: $draft.title)
        }
    }

    @ViewBuilder
    private var openOrdersPicker: some View {
        let orders = (model.projection?.activity ?? []).filter { $0.eventType.uppercased() == "ORDER" }
        let trailing: String = {
            if let selected = orders.first(where: { $0.id == draft.parentEventId }) {
                let party = selected.counterpartyName ?? selected.status ?? "open"
                return "\(selected.title.prefix(12)) · \(party)"
            }
            return "\(orders.count) open"
        }()
        RoseLinesCard(theme: theme, title: "Source", trailing: trailing) {
            if orders.isEmpty {
                RoseEmptyCard(
                    theme: theme,
                    title: "No open orders",
                    bodyText: "Record a sales or purchase order first. Status never posts revenue or stock."
                )
            } else {
                ForEach(Array(orders.prefix(8))) { row in
                    let picked = draft.parentEventId == row.id
                    Button {
                        draft.parentEventId = row.id
                        if draft.title.isEmpty { draft.title = row.title }
                    } label: {
                        HStack(alignment: .center, spacing: 8) {
                            VStack(alignment: .leading, spacing: 2) {
                                Text(statusSourceCode(row))
                                    .font(.plusJakarta(size: 10, weight: .bold))
                                    .foregroundStyle(RoseSheetChrome.label)
                                Text(row.title)
                                    .font(.plusJakarta(size: 13, weight: .medium))
                                    .foregroundStyle(OperatingV12.title)
                                    .lineLimit(1)
                                Text(statusSourceMeta(row))
                                    .font(.plusJakarta(size: 11))
                                    .foregroundStyle(RoseSheetChrome.textBody)
                            }
                            Spacer(minLength: 4)
                            Text(row.status ?? "Ready")
                                .font(.plusJakarta(size: 13, weight: .bold))
                                .foregroundStyle(
                                    statusReadyTone(row.status)
                                        ? RoseSheetChrome.tenderSelected
                                        : RoseSheetChrome.label
                                )
                        }
                        .padding(10)
                        .background(picked ? theme.accent.opacity(0.16) : RoseSheetChrome.field)
                        .overlay(
                            RoundedRectangle(cornerRadius: 10, style: .continuous)
                                .strokeBorder(
                                    picked ? theme.accent : Color.white.opacity(0.06),
                                    lineWidth: 1
                                )
                        )
                        .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }

    private func statusSourceCode(_ row: OperatingActivityPayload) -> String {
        if let key = row.clientKey?.trimmingCharacters(in: .whitespaces), !key.isEmpty {
            return key
        }
        let side = row.orderSide?.uppercased()
        let prefix = side == "PURCHASE" ? "PO" : "SO"
        return "\(prefix)-\(String(row.id.suffix(3)).uppercased())"
    }

    private func statusSourceMeta(_ row: OperatingActivityPayload) -> String {
        let party = row.counterpartyName ?? row.status ?? "open"
        if let qty = row.quantity, !qty.isEmpty {
            return "\(qty) · \(party)"
        }
        return "Order · \(party)"
    }

    private func statusReadyTone(_ status: String?) -> Bool {
        guard let status else { return true }
        let upper = status.uppercased()
        return upper.contains("READY") || upper == "OPEN" || upper == "CONFIRMED"
    }

    private func nextStatusDue(_ current: String) -> String {
        let options = ["Today · 6pm", "Tomorrow · noon", "Today · close"]
        let idx = options.firstIndex(of: current.isEmpty ? "Today · 6pm" : current) ?? 0
        return options[(idx + 1) % options.count]
    }

    /// `2399:30539` Budget / Target Revision — versioned plan; never rewrites posted sales.
    @ViewBuilder
    private var budgetBody: some View {
        RoseChipRow(
            theme: theme,
            options: RetailCaptureCopy.modeChips("BUDGET"),
            selection: $draft.revisionKind
        )

        let previousRaw = budgetPreviousRaw
        let prev = Decimal(string: previousRaw)
        let next = Decimal(string: draft.amount)
        let delta: Decimal? = {
            guard let prev, let next else { return nil }
            return next - prev
        }()
        let periodLabel = draft.periodChip == "Custom"
            ? (draft.periodCustom.isEmpty ? "Custom" : draft.periodCustom)
            : (draft.periodChip.isEmpty ? periodDefault : draft.periodChip)

        VStack(spacing: 8) {
            roseMoneyRow("Previous v1", formattedMoney(previousRaw))
            roseMoneyRow(
                "Delta",
                delta.map {
                    let sign = $0 >= 0 ? "+ " : "− "
                    return sign + formattedMoney(NSDecimalNumber(decimal: $0.magnitude).stringValue)
                } ?? "—"
            )
            roseMoneyRow("Effective", budgetEffectiveLabel)
            Divider().overlay(RoseSheetChrome.roseBorder.opacity(0.85))
            HStack(alignment: .bottom) {
                VStack(alignment: .leading, spacing: 2) {
                    Text("New target")
                        .font(.plusJakarta(size: 11, weight: .medium))
                        .foregroundStyle(RoseSheetChrome.textBody)
                    TextField("0.00", text: $draft.amount)
                        .keyboardType(.decimalPad)
                        .font(.plusJakarta(size: 28, weight: .bold))
                        .foregroundStyle(OperatingV12.title)
                }
                Spacer(minLength: 8)
                VStack(alignment: .trailing, spacing: 2) {
                    Text("Period")
                        .font(.plusJakarta(size: 10, weight: .bold))
                        .foregroundStyle(RoseSheetChrome.label)
                    Text(periodLabel)
                        .font(.plusJakarta(size: 13, weight: .bold))
                        .foregroundStyle(RoseSheetChrome.label)
                }
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(RoseSheetChrome.sheet)
        .overlay(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .strokeBorder(theme.accent, lineWidth: 1.5)
        )
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
        .shadow(color: theme.accent.opacity(0.25), radius: 12, y: 4)

        RosePickerRow(theme: theme, label: "Period", value: periodLabel) {
            cycleBudgetPeriod()
        }
        RosePickerRow(
            theme: theme,
            label: "Reason",
            value: draft.note.isEmpty ? RetailCaptureCopy.budgetFixtureReason : draft.note
        ) {
            cycleBudgetReason()
        }
        RosePickerRow(
            theme: theme,
            label: "Owner",
            value: draft.staff.isEmpty ? RetailCaptureCopy.budgetFixtureOwner : draft.staff
        ) {
            cycleBudgetOwner()
        }
        RoseMoreDetails(title: "More details", hint: RetailCaptureCopy.budgetMoreDetailsHint) {
            SetupDateTimeField(label: "Effective from", isoValue: $draft.occurredAt)
            RoseFieldLabel(text: "Notes")
            RoseTextField(theme: theme, placeholder: "Optional note", text: $draft.reference)
        }
    }

    /// `2399:30597` Issue / Approval / Update — audit trail; approval ≠ disbursement.
    @ViewBuilder
    private var issueBody: some View {
        HStack(spacing: 6) {
            ForEach(RetailCaptureCopy.modeChips("ISSUE"), id: \.self) { chip in
                RoseModeChip(theme: theme, text: chip, selected: draft.revisionKind == chip) {
                    draft.revisionKind = chip
                }
            }
        }

        RoseLinesCard(theme: theme, title: "Linked", trailing: "Register · shift") {
            HStack(alignment: .top, spacing: 8) {
                VStack(alignment: .leading, spacing: 2) {
                    Text(RetailCaptureCopy.issueFixtureCode)
                        .font(.plusJakarta(size: 10, weight: .bold))
                        .foregroundStyle(RoseSheetChrome.label)
                    Text(
                        draft.title.isEmpty
                            ? RetailCaptureCopy.issueFixtureTitle
                            : draft.title
                    )
                    .font(.plusJakarta(size: 13, weight: .medium))
                    .foregroundStyle(OperatingV12.title)
                    Text(RetailCaptureCopy.issueFixtureDetail)
                        .font(.plusJakarta(size: 11))
                        .foregroundStyle(RoseSheetChrome.textBody)
                }
                Spacer(minLength: 8)
                Text(draft.severity.isEmpty ? "Alert" : draft.severity)
                    .font(.plusJakarta(size: 13, weight: .bold))
                    .foregroundStyle(theme.accent)
            }
            .padding(.vertical, 4)
        }

        RoseFieldLabel(text: "Severity")
        RoseChipRow(theme: theme, options: ["Alert", "Warn", "Info"], selection: $draft.severity)

        RosePickerRow(
            theme: theme,
            label: "Title",
            value: draft.title.isEmpty ? RetailCaptureCopy.issueFixtureTitle : draft.title
        ) {
            cycleIssueTitle()
        }
        RosePickerRow(
            theme: theme,
            label: "Owner",
            value: draft.staff.isEmpty ? RetailCaptureCopy.issueFixtureOwner : draft.staff
        ) {
            cycleIssueOwner()
        }
        RosePickerRow(
            theme: theme,
            label: "Link",
            value: draft.link.isEmpty ? RetailCaptureCopy.issueFixtureLink : draft.link
        ) {
            cycleIssueLink()
        }

        RoseMoreDetails(title: "More details", hint: RetailCaptureCopy.issueMoreDetailsHint) {
            RoseTextField(theme: theme, placeholder: "Notes · Evidence · Tags", text: $draft.note)
            RosePickerRow(theme: theme, label: "Attachment", value: "Add photo (chrome only)")
            Text("Evidence chrome only — no upload API · approval ≠ disbursement")
                .font(.plusJakarta(size: 11))
                .foregroundStyle(OperatingV12.subtitle)
        }
    }

    // MARK: Shared helpers

    private var openReceivablesList: [OperatingPaymentPayload] {
        (model.projection?.paymentReadiness ?? []).filter { $0.relevance.uppercased() == "RECEIVABLE" }
    }

    private var openPayablesList: [OperatingPaymentPayload] {
        (model.projection?.paymentReadiness ?? []).filter { $0.relevance.uppercased() == "PAYABLE" }
    }

    @ViewBuilder
    private func openReceivables(title: String) -> some View {
        let open = openReceivablesList
        let party = selectedReceivable?.counterpartyName ?? draft.counterparty
        RoseLinesCard(
            theme: theme,
            title: title,
            trailing: RetailCaptureCopy.allocateTrailing(openCount: open.count, party: party)
        ) {
            if open.isEmpty {
                RoseEmptyCard(
                    theme: theme,
                    title: "Nothing to collect",
                    bodyText: "Post a Pay later sale first — credit sales with open AR appear here."
                )
            } else {
                ForEach(open.prefix(6)) { row in
                    Button {
                        draft.parentEventId = row.id
                        draft.counterparty = row.counterpartyName ?? draft.counterparty
                        draft.amount = row.amount
                        draft.title = row.title
                    } label: {
                        allocateRow(
                            code: shortDocCode(row.title, fallback: "INV"),
                            title: row.title,
                            subtitle: dueSubtitle(row, receivable: true),
                            amount: formattedMoney(row.amount),
                            amountDanger: true,
                            selected: draft.parentEventId == row.id
                        )
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }

    @ViewBuilder
    private func openPayables(title: String) -> some View {
        let open = openPayablesList
        let party = selectedPayable?.counterpartyName ?? draft.counterparty
        RoseLinesCard(
            theme: theme,
            title: title,
            trailing: RetailCaptureCopy.allocateTrailing(openCount: open.count, party: party)
        ) {
            if open.isEmpty {
                RoseEmptyCard(
                    theme: theme,
                    title: "Nothing to pay",
                    bodyText: "Post an unpaid purchase first — open AP appears here."
                )
            } else {
                ForEach(open.prefix(6)) { row in
                    Button {
                        draft.parentEventId = row.id
                        draft.counterparty = row.counterpartyName ?? draft.counterparty
                        draft.amount = row.amount
                        draft.title = row.title
                    } label: {
                        allocateRow(
                            code: shortDocCode(row.title, fallback: "PI"),
                            title: row.title,
                            subtitle: dueSubtitle(row, receivable: false),
                            amount: formattedMoney(row.amount),
                            amountDanger: false,
                            selected: draft.parentEventId == row.id
                        )
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }

    @ViewBuilder
    private func allocateRow(
        code: String,
        title: String,
        subtitle: String,
        amount: String,
        amountDanger: Bool,
        selected: Bool
    ) -> some View {
        HStack(alignment: .top, spacing: 8) {
            VStack(alignment: .leading, spacing: 2) {
                Text(code)
                    .font(.plusJakarta(size: 10, weight: .bold))
                    .foregroundStyle(RoseSheetChrome.label)
                Text(title)
                    .font(.plusJakarta(size: 13, weight: .medium))
                    .foregroundStyle(OperatingV12.title)
                    .multilineTextAlignment(.leading)
                Text(subtitle)
                    .font(.plusJakarta(size: 11))
                    .foregroundStyle(amountDanger ? Color(hex: "#EF4444") : RoseSheetChrome.textBody)
            }
            Spacer(minLength: 8)
            Text(amount)
                .font(.plusJakarta(size: 13, weight: .bold))
                .foregroundStyle(amountDanger ? Color(hex: "#EF4444") : OperatingV12.title)
            if selected {
                Image(systemName: "checkmark.circle.fill")
                    .foregroundStyle(theme.accent)
            }
        }
        .padding(.vertical, 8)
        .overlay(alignment: .bottom) {
            Rectangle()
                .fill(Color(hex: "#4C0519").opacity(0.6))
                .frame(height: 1)
        }
        .opacity(selected ? 1 : 0.92)
    }

    private func shortDocCode(_ title: String, fallback: String) -> String {
        if let match = title.split(separator: " ").first(where: { $0.contains("-") }) {
            return String(match)
        }
        let compact = title.replacingOccurrences(of: " ", with: "")
        return compact.count > 3 ? String(compact.prefix(8)).uppercased() : "\(fallback)-…"
    }

    private func dueSubtitle(_ row: OperatingPaymentPayload, receivable: Bool) -> String {
        if let bucket = row.dueBucket, !bucket.isEmpty {
            return bucket.replacingOccurrences(of: "_", with: " ").capitalized
        }
        if let due = row.dueAt, !due.isEmpty {
            return receivable ? "Due \(shortDate(due))" : "Goods received"
        }
        if let occurred = row.occurredAt, !occurred.isEmpty {
            return receivable
                ? "Credit sale · \(shortDate(occurred))"
                : "Purchase · \(shortDate(occurred))"
        }
        return receivable ? "Open receivable" : "Goods received"
    }

    private func shortDate(_ iso: String) -> String {
        let parser = ISO8601DateFormatter()
        parser.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        let date = parser.date(from: iso)
            ?? ISO8601DateFormatter().date(from: iso)
        guard let date else { return iso }
        let formatter = DateFormatter()
        formatter.dateFormat = "d MMM"
        return formatter.string(from: date)
    }

    private func cycleSupplier() {
        let rows = model.masters.filter { $0.kind.uppercased() == "SUPPLIER" }
        guard !rows.isEmpty else {
            if draft.counterparty.isEmpty { draft.counterparty = "Wholesale Foods" }
            return
        }
        let names = rows.map(\.name)
        let idx = names.firstIndex(of: draft.counterparty) ?? -1
        draft.counterparty = names[(idx + 1) % names.count]
    }

    private func cycleCustomer() {
        let rows = model.masters.filter { ["CUSTOMER", "BUYER"].contains($0.kind.uppercased()) }
        guard !rows.isEmpty else {
            if draft.counterparty.isEmpty {
                draft.counterparty = selectedReceivable?.counterpartyName ?? "Mehta Traders"
            }
            return
        }
        let names = rows.map(\.name)
        let idx = names.firstIndex(of: draft.counterparty) ?? -1
        draft.counterparty = names[(idx + 1) % names.count]
    }

    private func cyclePurchaseLocation() {
        let options = locationMasters.map(\.name)
        let fallback = ["Back store", "Floor", "Register"]
        let pool = options.isEmpty ? fallback : options
        let idx = pool.firstIndex(of: draft.location) ?? 0
        draft.location = pool[(idx + 1) % pool.count]
    }

    private var budgetPreviousRaw: String {
        let live = model.projection?.target?.value?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        if !live.isEmpty, Decimal(string: live) != nil { return live }
        return RetailCaptureCopy.budgetFixturePrevious
    }

    private var budgetEffectiveLabel: String {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_IN")
        formatter.dateFormat = "dd MMM"
        if let iso = draft.occurredAt {
            let isoFull = ISO8601DateFormatter()
            if let date = isoFull.date(from: iso) {
                return formatter.string(from: date)
            }
            let dayOnly = DateFormatter()
            dayOnly.calendar = Calendar(identifier: .gregorian)
            dayOnly.locale = Locale(identifier: "en_US_POSIX")
            dayOnly.timeZone = TimeZone(secondsFromGMT: 0)
            dayOnly.dateFormat = "yyyy-MM-dd"
            if let date = dayOnly.date(from: String(iso.prefix(10))) {
                return formatter.string(from: date)
            }
        }
        return formatter.string(from: Date())
    }

    private func cycleBudgetPeriod() {
        let options = [periodDefault, nextMonthLabel, "Custom"]
        if draft.periodChip == "Custom" {
            draft.periodChip = periodDefault
            draft.periodCustom = ""
            return
        }
        let current = draft.periodChip.isEmpty ? periodDefault : draft.periodChip
        let idx = options.firstIndex(of: current) ?? 0
        let next = options[(idx + 1) % options.count]
        draft.periodChip = next
        if next == "Custom" {
            draft.periodCustom = periodDefault
        }
    }

    private var nextMonthLabel: String {
        let cal = Calendar.current
        let date = cal.date(byAdding: .month, value: 1, to: Date()) ?? Date()
        let formatter = DateFormatter()
        formatter.dateFormat = "MMM yyyy"
        return formatter.string(from: date)
    }

    private func cycleBudgetReason() {
        let options = [
            RetailCaptureCopy.budgetFixtureReason,
            "Seasonality",
            "Cost inflation",
            "New SKU mix",
        ]
        let current = draft.note.isEmpty ? options[0] : draft.note
        let idx = options.firstIndex(of: current) ?? -1
        draft.note = options[(idx + 1) % options.count]
    }

    private func cycleBudgetOwner() {
        let options = [
            RetailCaptureCopy.budgetFixtureOwner,
            "Ravi · floor",
            "Meera · ops",
            "Owner",
        ]
        let current = draft.staff.isEmpty ? options[0] : draft.staff
        let idx = options.firstIndex(of: current) ?? -1
        draft.staff = options[(idx + 1) % options.count]
    }

    private func cycleIssueTitle() {
        let options = [
            RetailCaptureCopy.issueFixtureTitle,
            "Register short · cash",
            "UPI mismatch · till",
            "Damaged goods · aisle",
        ]
        let current = draft.title.isEmpty ? options[0] : draft.title
        let idx = options.firstIndex(of: current) ?? -1
        draft.title = options[(idx + 1) % options.count]
    }

    private func cycleIssueOwner() {
        let options = [
            RetailCaptureCopy.issueFixtureOwner,
            "Ravi · floor",
            "Meera · ops",
            "Shift lead",
        ]
        let current = draft.staff.isEmpty ? options[0] : draft.staff
        let idx = options.firstIndex(of: current) ?? -1
        draft.staff = options[(idx + 1) % options.count]
    }

    private func cycleIssueLink() {
        let options = [
            RetailCaptureCopy.issueFixtureLink,
            "Register 1 · today",
            "Andheri · shift close",
            "Back store · count",
        ]
        let current = draft.link.isEmpty ? options[0] : draft.link
        let idx = options.firstIndex(of: current) ?? -1
        draft.link = options[(idx + 1) % options.count]
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
        let kinds: Set<String> = ["SKU", "PRODUCT", "BATCH", "ITEM", "MATERIAL", "CONSUMABLE"]
        return model.masters
            .filter { kinds.contains($0.kind.uppercased()) }
            .sorted { $0.name.localizedCaseInsensitiveCompare($1.name) == .orderedAscending }
    }

    private func stockSkuCodeLabel(_ sku: OperatingMasterPayload) -> String {
        let code = sku.code?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        return code.isEmpty ? "SKU" : code
    }

    private func stockSkuValueLabel(_ sku: OperatingMasterPayload) -> String {
        let unit = sku.unit?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        if unit.isEmpty { return sku.name }
        return "\(sku.name) · \(unit)"
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
        openReceivablesList.first { $0.id == draft.parentEventId } ?? openReceivablesList.first
    }

    private var selectedPayable: OperatingPaymentPayload? {
        openPayablesList.first { $0.id == draft.parentEventId } ?? openPayablesList.first
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
        // Purchase receive lines stay empty until Scan / + Add line — never seed blank cards.
        if RetailCaptureCopy.moneyModes.contains(upper) {
            let preferred = RetailCaptureCopy.defaultTender(upper)
            if draft.tender.isEmpty
                || (upper == "PURCHASE" && !RetailCaptureCopy.tenderChips("PURCHASE").contains(draft.tender))
                || (upper == "COLLECTION" && draft.tender == "Pay later")
                || (upper == "PAYMENT" && (draft.tender == "UPI" || draft.tender == "Pay later"))
            {
                draft.tender = preferred
            }
            if upper == "PURCHASE", draft.location.isEmpty {
                draft.location = "Back store"
            }
            if upper == "COLLECTION", draft.parentEventId == nil, let first = openReceivablesList.first {
                draft.parentEventId = first.id
                if draft.counterparty.isEmpty { draft.counterparty = first.counterpartyName ?? "" }
                if draft.amount.isEmpty { draft.amount = first.amount }
                if draft.title.isEmpty { draft.title = first.title }
            }
            if upper == "PAYMENT", draft.parentEventId == nil, let first = openPayablesList.first {
                draft.parentEventId = first.id
                if draft.counterparty.isEmpty { draft.counterparty = first.counterpartyName ?? "" }
                if draft.amount.isEmpty { draft.amount = first.amount }
                if draft.title.isEmpty { draft.title = first.title }
            }
        }
        if upper == "SPEND", draft.paymentChoice == "Fully Paid" {
            draft.paymentChoice = "Paid now"
        }
        if upper == "BUDGET" || upper == "TARGET" {
            if draft.revisionKind == "Budget"
                || draft.revisionKind == "Purchase budget"
                || !RetailCaptureCopy.modeChips("BUDGET").contains(draft.revisionKind)
            {
                draft.revisionKind = "Sales target"
            }
            if draft.amount.isEmpty {
                let live = model.projection?.target?.value?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
                draft.amount = live.isEmpty ? RetailCaptureCopy.budgetFixtureNew : live
            }
            if draft.note.isEmpty {
                draft.note = RetailCaptureCopy.budgetFixtureReason
            }
            if draft.staff.isEmpty {
                draft.staff = RetailCaptureCopy.budgetFixtureOwner
            }
            if draft.periodChip.isEmpty {
                draft.periodChip = periodDefault
            }
        }
        if upper == "ISSUE" || upper == "UPDATE" || upper == "APPROVAL" {
            if draft.revisionKind == "Budget"
                || !RetailCaptureCopy.modeChips("ISSUE").contains(draft.revisionKind)
            {
                draft.revisionKind = "Issue"
            }
            if draft.title.isEmpty {
                draft.title = RetailCaptureCopy.issueFixtureTitle
            }
            if draft.staff.isEmpty {
                draft.staff = RetailCaptureCopy.issueFixtureOwner
            }
            if draft.link.isEmpty || draft.link == "Register · today" {
                draft.link = RetailCaptureCopy.issueFixtureLink
            }
            if draft.severity.isEmpty {
                draft.severity = "Alert"
            }
        }
        if upper == "STOCK" {
            if draft.stockAction == "IN" { draft.stockAction = "Set current" }
            if draft.location == "Back store" || draft.location.isEmpty { draft.location = "Aisle 3" }
            if draft.stockReason.isEmpty { draft.stockReason = "Cycle count" }
            if draft.staff.isEmpty { draft.staff = "Ravi · floor" }
            // Lines stay empty until Scan / SKU pick — never seed Basmati / HM fixtures.
        }
        if upper == "ORDER" {
            if draft.orderSide != "Purchase order", draft.location == "Back store" {
                draft.location = "Pickup · Andheri"
            }
            if draft.orderSide == "Purchase order", draft.location == "Pickup · store" || draft.location == "Pickup · Andheri" {
                draft.location = "Back store"
            }
            if draft.catalogCategory.isEmpty { draft.catalogCategory = "Staples" }
            if draft.placeOfSupply.isEmpty || draft.placeOfSupply == "Maharashtra · intra-state" {
                draft.placeOfSupply = draft.orderSide == "Purchase order"
                    ? "Maharashtra (27) · intra-state"
                    : "Maharashtra · CGST+SGST"
            }
        }
        if upper == "STATUS" {
            // Ops-only sheet — never carry money/stock drafts into the post.
            draft.amount = ""
            draft.lines = []
            if draft.staff.isEmpty { draft.staff = "Ravi · floor" }
            if draft.dueLabel.isEmpty || draft.dueLabel == "Today" {
                draft.dueLabel = "Today · 6pm"
            }
            if draft.parentEventId == nil,
               let first = (model.projection?.activity ?? []).first(where: { $0.eventType.uppercased() == "ORDER" })
            {
                draft.parentEventId = first.id
            }
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
        for sku in catalogMasters.prefix(2) {
            addCatalogLine(sku, purchase: purchase)
        }
    }

    private func addCatalogLine(_ sku: OperatingMasterPayload, purchase: Bool) {
        let line = RetailLineDraft(
            sku: sku.code ?? String(sku.name.prefix(6)).uppercased(),
            name: sku.name,
            qty: "1",
            rate: (purchase ? sku.purchasePrice : sku.sellPrice) ?? sku.purchasePrice ?? sku.sellPrice ?? "",
            hsn: sku.hsn ?? "",
            unit: sku.unit ?? "pcs",
            gst: sku.gstRate ?? ""
        )
        if let idx = draft.lines.firstIndex(where: { $0.name.isEmpty }) {
            draft.lines[idx] = line
        } else {
            draft.lines.append(line)
        }
        syncAmountFromLines()
    }

    private func applyPurchaseScan(_ raw: String) {
        let q = raw.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !q.isEmpty else { return }
        guard let match = retailMatchCatalogSku(catalogMasters, query: q) else {
            scanBanner = "No product for this code — pick from catalogue or add under Catalogue."
            draft.scanQuery = q
            return
        }
        scanBanner = nil
        addCatalogLine(match, purchase: true)
        draft.scanQuery = ""
    }

    private func applyPrefill() {
        guard !didPrefill, let item = browserPrefill else { return }
        didPrefill = true
        draft.masterId = item.id
        draft.title = item.name
        draft.quantity = "1"
        if let unit = item.unit { draft.unit = unit }
        if let price = item.unitPrice { draft.itemPrice = price }
        if let idx = draft.lines.firstIndex(where: { $0.name.isEmpty }) {
            draft.lines[idx].name = item.name
            draft.lines[idx].rate = item.unitPrice ?? ""
            draft.lines[idx].unit = item.unit ?? "pcs"
            syncAmountFromLines()
        } else if draft.lines.isEmpty, upper == "PURCHASE" {
            draft.lines.append(
                RetailLineDraft(
                    name: item.name,
                    qty: "1",
                    rate: item.unitPrice ?? "",
                    unit: item.unit ?? "pcs"
                )
            )
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
    /// Master GST % for estimate rows — never posted as fabricated tax from SO/PO/Purchase UI.
    var gst = ""
    /// Prior on-hand shown as Figma “was N” on Stock Update count lines.
    var wasQty = ""

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
    var stockAction = "Set current"
    var stockReason = "Cycle count"
    var orderSide = "Sales order"
    var statusChoice = "Ready to pick"
    var statusMode = "Dispatch"
    var staff = ""
    var revisionKind = "Sales target"
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
    var placeOfSupply = "Maharashtra · CGST+SGST"
    var dueLabel = "Today · 6pm"
    var notify = "Customer WhatsApp"
    var severity = "Alert"
    var link = "Register 2 · today"
    var catalogCategory = "Staples"
    /// Purchase starts empty so Scan opens the camera instead of showing ghost receive cards.
    var lines: [RetailLineDraft] = []
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
        case "ISSUE", "UPDATE", "APPROVAL":
            return !draft.title.trimmingCharacters(in: .whitespaces).isEmpty
        case "BUDGET", "TARGET":
            return Decimal(string: draft.amount).map { $0 > 0 } ?? false
        case "COLLECTION", "PAYMENT":
            guard draft.parentEventId != nil else { return false }
            return Decimal(string: draft.amount).map { $0 > 0 } ?? false
        case "PURCHASE":
            return !draft.counterparty.trimmingCharacters(in: .whitespaces).isEmpty
                && !filledLines.isEmpty
                && (Decimal(string: draft.amount).map { $0 > 0 } ?? false)
        case "STOCK":
            // Contract validates a single qty + reason; multi-line UI posts the first filled line.
            guard let first = filledLines.first else { return false }
            let qty = Decimal(string: first.qty) ?? -1
            let action = OperatingContractVocabulary.stockAction(draft.stockAction)
            let reasonOk = OperatingContractVocabulary.stockReason(draft.stockReason) != nil
            if action == "SET" { return qty >= 0 && reasonOk }
            return qty > 0 && reasonOk
        default:
            return !draft.title.trimmingCharacters(in: .whitespaces).isEmpty
                || !draft.amount.trimmingCharacters(in: .whitespaces).isEmpty
                || draft.masterId != nil
        }
    }

    func build() -> OperatingEventInput {
        var contract = OperatingContractInput()
        // STATUS never posts revenue; STOCK posts qty delta only (no amount).
        let amount: String? = {
            if upper == "STATUS" || upper == "STOCK" { return nil }
            return draft.amount.isEmpty ? nil : draft.amount
        }()
        var input = OperatingEventInput(
            momentId: model.momentId,
            eventType: eventType,
            title: resolvedTitle,
            amount: amount,
            note: combinedNote(),
            channelName: resolvedChannel,
            counterpartyName: draft.counterparty.isEmpty ? nil : draft.counterparty,
            occurredAt: draft.occurredAt,
            parentEventId: draft.parentEventId,
            quantity: upper == "STATUS"
                ? nil
                : (draft.quantity.isEmpty ? filledLines.first?.qty : draft.quantity),
            unit: upper == "STATUS"
                ? nil
                : (draft.unit.isEmpty ? filledLines.first?.unit : draft.unit),
            dueAt: draft.dueAt,
            masterId: draft.masterId,
            paymentStatus: paymentStatus,
            revisesEventId: upper == "STATUS" ? draft.parentEventId : nil
        )

        if layout == .stock {
            let action = OperatingContractVocabulary.stockAction(draft.stockAction)
            contract.stockAction = action
            contract.stockReason = OperatingContractVocabulary.stockReason(
                draft.stockReason.isEmpty ? "Cycle count" : draft.stockReason
            )
            contract.locationName = draft.location.isEmpty ? nil : draft.location
            contract.staffName = draft.staff.isEmpty ? nil : draft.staff
            if let first = filledLines.first {
                input.title = first.name
                input.quantity = first.qty
                input.unit = first.unit.isEmpty ? nil : first.unit
                if action == "SET" { contract.declaredQuantity = first.qty }
            } else if action == "SET", !draft.quantity.isEmpty {
                contract.declaredQuantity = draft.quantity
            }
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
            // Commitment only — no AP, stock, GST, or revenue from this save.
            contract.orderSide = draft.orderSide == "Purchase order" ? "PURCHASE" : "SALES"
            contract.orderStatus = orderStatusToken(draft.stateChip)
            contract.estimatedAmount = draft.amount.isEmpty ? nil : draft.amount
            contract.promisedAt = draft.dueAt
            contract.locationName = draft.location
            if draft.stateChip == "Hold" { contract.holdStatus = "HELD" }
        }

        if upper == "STATUS" {
            // Operational state only — eventType stays ORDER; never SALE / STOCK.
            input.amount = nil
            input.quantity = nil
            contract.orderStatus = statusToken(draft.statusChoice)
            contract.staffName = draft.staff.isEmpty ? nil : draft.staff
            contract.deliveryStatus = draft.statusMode
            contract.shipmentStatus = statusToken(draft.statusChoice)
        }

        if layout == .budget || upper == "TARGET" {
            input.effectiveFrom = draft.occurredAt
            contract.estimatedAmount = draft.amount.isEmpty ? nil : draft.amount
        }

        if upper == "ISSUE" || upper == "UPDATE" || upper == "APPROVAL" {
            contract.staffName = draft.staff.isEmpty ? nil : draft.staff
            // Approval / Update never disburse — permission / audit only.
        }

        if layout == .collection || layout == .payment, let parentId = draft.parentEventId, !draft.amount.isEmpty {
            contract.paymentState = layout == .collection ? "RECEIVED" : "PAID"
            contract.receivedAmount = draft.amount
            contract.allocations = [OperatingAllocationInput(sourceId: parentId, amount: draft.amount)]
        }

        // STATUS is ops-only — never attach catalogue lines (would imply stock/sale movement).
        if upper != "STATUS" {
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
        }

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
        case "ISSUE", "UPDATE", "APPROVAL":
            return trimmed.isEmpty ? RetailCaptureCopy.issueFixtureTitle : trimmed
        case "STATUS":
            return trimmed.isEmpty ? "Status · \(draft.statusChoice)" : trimmed
        case "STOCK":
            if let first = filledLines.first { return first.name }
            return "Stock · \(draft.stockAction)"
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
        if upper == "ISSUE" || upper == "UPDATE" || upper == "APPROVAL" {
            // Issue posts ISSUE; Approval / Update are audit/permission only (≠ disbursement).
            return draft.revisionKind == "Issue" ? "ISSUE" : "UPDATE"
        }
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
        // Budget stores Reason in `note` — avoid duplicating the bare reason string.
        if !draft.note.isEmpty, upper != "BUDGET", upper != "TARGET" {
            parts.append(draft.note)
        }
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
        if upper == "STOCK" {
            parts.append("Action: \(draft.stockAction)")
            if !draft.location.isEmpty { parts.append("Location: \(draft.location)") }
            if !draft.staff.isEmpty { parts.append("Counted by: \(draft.staff)") }
            if filledLines.count > 1 {
                parts.append("SKUs: \(filledLines.map(\.name).joined(separator: ", "))")
            }
        }
        if upper == "BUDGET" || upper == "TARGET" {
            let period = draft.periodChip == "Custom" ? draft.periodCustom : draft.periodChip
            if !period.isEmpty { parts.append("Period: \(period)") }
            if !draft.note.isEmpty { parts.append("Reason: \(draft.note)") }
            if !draft.staff.isEmpty { parts.append("Owner: \(draft.staff)") }
            if !draft.reference.isEmpty { parts.append(draft.reference) }
            parts.append("Metric: \(draft.revisionKind)")
        }
        if upper == "ISSUE" || upper == "UPDATE" || upper == "APPROVAL" {
            parts.append("Kind: \(draft.revisionKind)")
            parts.append("Severity: \(draft.severity)")
            if !draft.staff.isEmpty { parts.append("Owner: \(draft.staff)") }
            if !draft.link.isEmpty { parts.append("Link: \(draft.link)") }
            if draft.revisionKind == "Approval" {
                parts.append("Approval ≠ disbursement")
            }
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

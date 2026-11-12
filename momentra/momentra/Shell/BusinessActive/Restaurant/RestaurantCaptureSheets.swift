import SwiftUI

/// Figma-parity restaurant capture sheets (RST-QA-02 … QA-07). Ember / Stone chrome only.
struct RestaurantCaptureSheet: View {
    let theme: OperatingTheme
    let mode: String
    @ObservedObject var model: BusinessOperatingModel

    var body: some View {
        RestaurantCaptureForm(theme: theme, mode: mode, model: model)
    }
}

// MARK: - Form

private struct RestaurantCaptureForm: View {
    let theme: OperatingTheme
    let mode: String
    @ObservedObject var model: BusinessOperatingModel

    @Environment(\.dismiss) private var dismiss
    @State private var draft = RestaurantCaptureDraft()
    @State private var saving = false
    @State private var moreOpen = false
    @State private var masterFormKind: RestaurantMasterFormKind?

    private var upper: String { mode.uppercased() }
    private var layout: OperatingCaptureLayout? { OperatingCaptureLayout.forMode(mode) }

    private var brand: String {
        model.title.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    private var kitchenLine: String {
        let raw = brand.isEmpty ? "Spice Route Kitchen" : brand
        return raw.localizedCaseInsensitiveContains("Kitchen") ? raw : "\(raw) Kitchen"
    }

    private var title: String {
        if RestaurantCaptureCopy.moneyModes.contains(upper)
            || RestaurantCaptureCopy.opsModes.contains(upper)
            || upper == "ORDER"
        {
            return RestaurantCaptureCopy.sheetTitle(upper, orderSide: draft.orderSide)
        }
        return layout?.label ?? mode.capitalized
    }

    private var qaCode: String {
        RestaurantCaptureCopy.qaCode(upper, orderSide: draft.orderSide)
    }

    private var headerSubtitle: String {
        if upper == "ORDER" {
            return RestaurantCaptureCopy.sheetSubtitle("ORDER", orderSide: draft.orderSide)
        }
        if upper == "PURCHASE" {
            return RestaurantCaptureCopy.sheetSubtitle("PURCHASE")
        }
        if ["BUDGET", "TARGET"].contains(upper) {
            let metric = draft.revisionKind.isEmpty ? "Sales" : draft.revisionKind
            let label = metric == "Covers" ? "Covers target" : "Sales target"
            return "\(kitchenLine) · Kiran Rao · \(label)"
        }
        if ["ISSUE", "UPDATE", "APPROVAL"].contains(upper) {
            return "\(kitchenLine) · Kiran Rao · Spoilage · dal"
        }
        let context = RestaurantCaptureCopy.contextLine(
            upper,
            brand: brand,
            kitchen: kitchenLine,
            party: draft.counterparty,
            detail: draft.goodsMode,
            orderSide: draft.orderSide
        )
        return context
    }

    private var canSave: Bool {
        RestaurantCaptureEventBuilder(mode: mode, draft: draft, model: model).canSave
    }

    private var filledLines: [RestaurantLineDraft] {
        draft.lines.filter { !$0.name.trimmingCharacters(in: .whitespaces).isEmpty }
    }

    private var filledLineCount: Int { filledLines.count }

    private var goodsTotal: Decimal {
        draft.lines.reduce(Decimal.zero) { $0 + $1.lineTotal }
    }

    private var goodsTotalString: String {
        NSDecimalNumber(decimal: goodsTotal).stringValue
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                EmberGrabber()
                EmberSheetHeader(title: title, subtitle: headerSubtitle, onClose: { dismiss() })
                modeBody

                if showsMoreDetails {
                    EmberMoreDetails(hint: RestaurantCaptureCopy.moreDetailsHint, open: $moreOpen) {
                        if upper != "STOCK" {
                            SetupDateTimeField(label: "Recorded at", isoValue: $draft.occurredAt)
                        }
                        EmberFieldLabel(text: "Note")
                        EmberTextField(placeholder: "Optional reference", text: $draft.note)
                    }
                }

                EmberWhyBox(text: whyText)

                EmberStickyFooter(
                    title: saving ? "Saving…" : saveLabel,
                    caption: footerCaption,
                    disabled: !canSave,
                    loading: saving,
                    action: { Task { await save() } }
                )
            }
            .padding(.horizontal, 20)
            .padding(.top, 12)
            .padding(.bottom, 28)
        }
        .background(EmberSheetChrome.sheet)
        .onAppear {
            ensureDefaults()
            syncAmountFromLines()
        }
        .task {
            if model.masters.isEmpty { await model.loadMasters() }
        }
        .sheet(item: $masterFormKind) { kind in
            RestaurantMasterFormSheet(theme: theme, kind: kind, model: model) { savedName in
                if kind == .vendor, !savedName.isEmpty {
                    draft.counterparty = savedName
                }
                masterFormKind = nil
                Task { await model.loadMasters() }
            }
            .presentationDetents([.large])
            .preferredColorScheme(.dark)
        }
    }

    private var showsMoreDetails: Bool {
        !["STOCK", "BUDGET", "TARGET", "ISSUE", "UPDATE", "APPROVAL", "HOLD", "TRANSFER", "REPRINT"].contains(upper)
    }

    private var saveLabel: String {
        let money = formattedMoney(draft.amount.isEmpty ? goodsTotalString : draft.amount)
        if RestaurantCaptureCopy.moneyModes.contains(upper) || upper == "ORDER" {
            return canSave
                ? RestaurantCaptureCopy.cta(upper, amount: upper == "STOCK" ? nil : money, orderSide: draft.orderSide)
                : RestaurantCaptureCopy.cta(upper, orderSide: draft.orderSide)
        }
        if ["BUDGET", "TARGET", "ISSUE", "UPDATE", "APPROVAL"].contains(upper) {
            return RestaurantCaptureCopy.cta(upper, revisionKind: draft.revisionKind)
        }
        return "Save · \(title)"
    }

    private var footerCaption: String {
        RestaurantCaptureCopy.footerCaption(upper, orderSide: draft.orderSide)
    }

    private var whyText: String {
        RestaurantCaptureCopy.why(upper, orderSide: draft.orderSide)
    }

    @ViewBuilder
    private var modeBody: some View {
        switch upper {
        case "PURCHASE": purchaseBody
        case "COLLECTION": collectionBody
        case "PAYMENT": paymentBody
        case "SPEND": expenseBody
        case "STOCK": stockBody
        case "ORDER": orderBody
        case "BUDGET", "TARGET": budgetBody
        case "ISSUE", "UPDATE", "APPROVAL": issueBody
        default: EmptyView()
        }
    }

    // MARK: Purchase · RST-QA-02

    @ViewBuilder
    private var purchaseBody: some View {
        EmberChipRow(
            options: RestaurantCaptureCopy.modeChips("PURCHASE"),
            selection: $draft.goodsMode
        )

        EmberFieldLabel(text: RestaurantCaptureCopy.scanLabel("PURCHASE"))
        EmberScanField(
            placeholder: RestaurantCaptureCopy.scanPlaceholder("PURCHASE"),
            query: $draft.scanQuery,
            actionLabel: "Scan",
            disabled: draft.scanQuery.isEmpty,
            action: { appendPurchaseScanLine() }
        )

        EmberLinesCard(
            title: "Receive lines",
            trailing: "\(filledLineCount) line\(filledLineCount == 1 ? "" : "s")"
        ) {
            ForEach($draft.lines) { $line in
                purchaseLineEditor(line: $line)
            }
            Button("+ Add line") { draft.lines.append(RestaurantLineDraft()) }
                .font(.plusJakarta(size: 13, weight: .bold))
                .foregroundStyle(EmberSheetChrome.gold)
                .buttonStyle(.plain)
        }

        purchaseTotals

        EmberFieldLabel(text: "Tender")
        EmberChipRow(
            options: RestaurantCaptureCopy.tenderChips("PURCHASE"),
            selection: $draft.tender,
            style: .tender
        )

        VStack(alignment: .leading, spacing: 6) {
            EmberFieldLabel(text: "Supplier")
            HStack(spacing: 8) {
                Button {
                    cycleSupplier()
                } label: {
                    HStack(spacing: 8) {
                        Text(draft.counterparty.isEmpty ? "Select supplier" : draft.counterparty)
                            .font(.plusJakarta(size: 14, weight: .medium))
                            .foregroundStyle(
                                draft.counterparty.isEmpty
                                    ? EmberSheetChrome.textFaint
                                    : EmberSheetChrome.textPrimary
                            )
                            .frame(maxWidth: .infinity, alignment: .leading)
                        Text("›")
                            .font(.plusJakarta(size: 16, weight: .bold))
                            .foregroundStyle(EmberSheetChrome.textBody)
                    }
                    .padding(12)
                    .background(EmberSheetChrome.chip)
                    .overlay(
                        RoundedRectangle(cornerRadius: 12, style: .continuous)
                            .strokeBorder(EmberSheetChrome.hairline, lineWidth: 1)
                    )
                    .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                }
                .buttonStyle(.plain)

                Button {
                    masterFormKind = .vendor
                } label: {
                    Text("+")
                        .font(.plusJakarta(size: 22, weight: .bold))
                        .foregroundStyle(EmberSheetChrome.ember)
                        .frame(width: 46, height: 46)
                        .background(EmberSheetChrome.chip)
                        .overlay(
                            RoundedRectangle(cornerRadius: 12, style: .continuous)
                                .strokeBorder(EmberSheetChrome.hairline, lineWidth: 1)
                        )
                        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                }
                .buttonStyle(.plain)
                .accessibilityLabel("New vendor")
            }
        }
        EmberPickerRow(
            label: "Deliver to",
            value: draft.location.isEmpty ? kitchenLine : draft.location,
            action: { draft.location = nextKitchenLocation(draft.location) }
        )
    }

    private var purchaseGstEstimate: Decimal? {
        RetailGstEstimate.from(
            amounts: draft.lines.map(\.lineTotal),
            rates: draft.lines.map { $0.gst.isEmpty ? nil : $0.gst },
            inclusive: false
        )
    }

    @ViewBuilder
    private var purchaseTotals: some View {
        let payable = formattedMoney(draft.amount.isEmpty ? goodsTotalString : draft.amount)
        let gstText: String = {
            guard let est = purchaseGstEstimate else { return "Unavailable" }
            return formattedMoney(NSDecimalNumber(decimal: est).stringValue)
        }()
        EmberTotalsCard(
            rows: [
                ("Subtotal", formattedMoney(goodsTotalString), false),
                ("GST", gstText, gstText == "Unavailable"),
            ],
            payableLabel: "Payable",
            payable: payable,
            sideLabel: "Tender",
            sideValue: draft.tender,
            sideWarning: draft.tender == "Pay later"
        )
    }

    @ViewBuilder
    private func purchaseLineEditor(line: Binding<RestaurantLineDraft>) -> some View {
        let qty = Int(line.wrappedValue.qty) ?? 0
        VStack(alignment: .leading, spacing: 8) {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 4) {
                    EmberTextField(placeholder: "Item", text: line.name)
                        .onChange(of: line.wrappedValue.name) { _, _ in syncAmountFromLines() }
                    Text(line.wrappedValue.unit.isEmpty ? "kg · pkg" : line.wrappedValue.unit)
                        .font(.plusJakarta(size: 11))
                        .foregroundStyle(EmberSheetChrome.textBody)
                }
                Spacer()
                Text(formattedMoney(NSDecimalNumber(decimal: line.wrappedValue.lineTotal).stringValue))
                    .font(.plusJakarta(size: 13, weight: .bold))
                    .foregroundStyle(EmberSheetChrome.textPrimary)
            }
            HStack {
                EmberStepper(quantity: max(qty, 0), onDecrement: {
                    if qty > 0 {
                        line.wrappedValue.qty = String(qty - 1)
                        syncAmountFromLines()
                    }
                }, onIncrement: {
                    line.wrappedValue.qty = String(qty + 1)
                    syncAmountFromLines()
                })
                EmberTextField(placeholder: "Rate", text: line.rate, keyboard: .decimalPad)
                    .onChange(of: line.wrappedValue.rate) { _, _ in syncAmountFromLines() }
            }
        }
        .padding(.vertical, 6)
    }

    // MARK: Collection · RST-QA-03

    @ViewBuilder
    private var collectionBody: some View {
        EmberChipRow(
            options: RestaurantCaptureCopy.modeChips("COLLECTION"),
            selection: $draft.goodsMode
        )

        openReceivablesBlock

        let openAr = Decimal(string: selectedReceivable?.amount ?? "0") ?? 0
        let receipt = Decimal(string: draft.amount) ?? 0
        let remaining = max(openAr - receipt, 0)

        EmberAmountHero(
            label: RestaurantCaptureCopy.amountLabel("COLLECTION"),
            amount: formattedMoney(draft.amount),
            caption: draft.tender
        )

        EmberBreakdownCard {
            EmberMoneyRow(label: "Open AR", value: formattedMoney(selectedReceivable?.amount), danger: true)
            EmberMoneyRow(label: "This receipt", value: formattedMoney(draft.amount.isEmpty ? "0" : draft.amount))
            EmberMoneyRow(
                label: "Remaining",
                value: formattedMoney(NSDecimalNumber(decimal: remaining).stringValue)
            )
        }

        EmberFieldLabel(text: "Method")
        EmberChipRow(
            options: RestaurantCaptureCopy.tenderChips("COLLECTION"),
            selection: $draft.tender,
            style: .tender
        )

        EmberPickerRow(
            label: "Settle",
            value: draft.counterparty.isEmpty ? "Platform AR" : draft.counterparty,
            action: { cycleReceivableParty() }
        )

        EmberFieldLabel(text: "Collect now")
        EmberTextField(placeholder: "0.00", text: $draft.amount, keyboard: .decimalPad)
    }

    // MARK: Payment · RST-QA-04

    @ViewBuilder
    private var paymentBody: some View {
        EmberChipRow(
            options: RestaurantCaptureCopy.modeChips("PAYMENT"),
            selection: $draft.goodsMode
        )

        openPayablesBlock

        EmberAmountHero(
            label: RestaurantCaptureCopy.amountLabel("PAYMENT"),
            amount: formattedMoney(draft.amount),
            caption: draft.tender
        )

        EmberFieldLabel(text: "Method")
        EmberChipRow(
            options: RestaurantCaptureCopy.tenderChips("PAYMENT"),
            selection: $draft.tender,
            style: .tender
        )

        EmberPickerRow(
            label: "Payee",
            value: draft.counterparty.isEmpty ? "Select supplier" : draft.counterparty,
            action: { cycleSupplier() }
        )

        EmberFieldLabel(text: "Pay now")
        EmberTextField(placeholder: "0.00", text: $draft.amount, keyboard: .decimalPad)
    }

    // MARK: Expense · RST-QA-05

    @ViewBuilder
    private var expenseBody: some View {
        EmberChipRow(
            options: RestaurantCaptureCopy.modeChips("SPEND"),
            selection: $draft.expenseMode
        )

        EmberAmountHero(
            label: RestaurantCaptureCopy.amountLabel("SPEND"),
            amount: formattedMoney(draft.amount.isEmpty ? "0" : draft.amount),
            caption: draft.paymentChoice
        )

        EmberFieldLabel(text: "Payment state")
        EmberChipRow(
            options: RestaurantCaptureCopy.tenderChips("SPEND"),
            selection: $draft.paymentChoice,
            style: .tender
        )

        EmberFieldLabel(text: "Amount")
        EmberTextField(placeholder: "0.00", text: $draft.amount, keyboard: .decimalPad)

        EmberBreakdownCard {
            EmberMoneyRow(label: "GST", value: "Unavailable", gold: true)
            EmberMoneyRow(
                label: "Cash impact",
                value: draft.paymentChoice == "Paid now" ? "Paid now" : "Pay later"
            )
        }
    }

    // MARK: Stock · RST-QA-06

    @ViewBuilder
    private var stockBody: some View {
        EmberChipRow(
            options: RestaurantCaptureCopy.modeChips("STOCK"),
            selection: $draft.goodsMode
        )
        .onChange(of: draft.goodsMode) { _, chip in
            syncStockModeChip(chip)
        }

        EmberFieldLabel(text: RestaurantCaptureCopy.scanLabel("STOCK"))
        EmberScanField(
            placeholder: RestaurantCaptureCopy.scanPlaceholder("STOCK"),
            query: $draft.scanQuery,
            actionLabel: "Add",
            disabled: draft.scanQuery.isEmpty,
            action: { appendStockScanLine() }
        )

        EmberLinesCard(
            title: "Qty lines",
            trailing: "\(filledLineCount) item\(filledLineCount == 1 ? "" : "s") · qty only"
        ) {
            ForEach($draft.lines) { $line in
                stockLineEditor(line: $line)
            }
            Button("+ Add item") { draft.lines.append(RestaurantLineDraft()) }
                .font(.plusJakarta(size: 13, weight: .bold))
                .foregroundStyle(EmberSheetChrome.gold)
                .buttonStyle(.plain)
        }

        EmberAmountHero(
            label: RestaurantCaptureCopy.amountLabel("STOCK"),
            amount: filledLineCount > 0 ? "\(filledLines.map(\.qty).joined(separator: " · "))" : "—",
            caption: "No unit cost · recipe excluded",
            amountSize: 24
        )

        EmberPickerRow(
            label: "Store",
            value: draft.location.isEmpty ? "Cold store" : draft.location,
            action: { draft.location = nextStockLocation(draft.location) }
        )
    }

    @ViewBuilder
    private func stockLineEditor(line: Binding<RestaurantLineDraft>) -> some View {
        let qty = Int(line.wrappedValue.qty) ?? 0
        HStack {
            VStack(alignment: .leading, spacing: 4) {
                EmberTextField(placeholder: "Ingredient", text: line.name)
                Text(draft.goodsMode)
                    .font(.plusJakarta(size: 10))
                    .foregroundStyle(EmberSheetChrome.textBody)
            }
            Spacer()
            EmberStepper(quantity: max(qty, 0), onDecrement: {
                if qty > 0 { line.wrappedValue.qty = String(qty - 1) }
            }, onIncrement: {
                line.wrappedValue.qty = String(qty + 1)
            })
        }
        .padding(.vertical, 6)
    }

    // MARK: Order · RST-QA-07 / 07-PO

    @ViewBuilder
    private var orderBody: some View {
        let isPo = draft.orderSide == "Purchase Order"

        EmberChipRow(
            options: RestaurantCaptureCopy.orderModeChips(),
            selection: $draft.orderSide
        )
        .onChange(of: draft.orderSide) { _, side in
            draft.stateChip = side == "Purchase Order" ? "Open" : "Placed"
            draft.catalogCategory = side == "Purchase Order"
                ? (RestaurantCaptureCopy.poRawCategories.first ?? "Dairy")
                : (RestaurantCaptureCopy.soMenuCategories.first ?? "Mains")
            draft.location = side == "Purchase Order" ? kitchenLine : "Floor"
            draft.channel = side == "Purchase Order" ? "" : "Dine-in"
            syncAmountFromLines()
        }

        if !isPo {
            EmberChipRow(
                options: RestaurantCaptureCopy.soChannelChips,
                selection: $draft.channel,
                style: .channel
            )
            EmberChipRow(
                options: RestaurantCaptureCopy.orderStateChips(isPurchase: false),
                selection: $draft.stateChip
            )
        } else {
            EmberChipRow(
                options: RestaurantCaptureCopy.modeChips("PURCHASE"),
                selection: $draft.goodsMode
            )
            EmberChipRow(
                options: RestaurantCaptureCopy.orderStateChips(isPurchase: true),
                selection: $draft.stateChip
            )
        }

        let heroCaption = isPo ? "Commitment only · not AP yet" : "Commitment only · not revenue"
        EmberAmountHero(
            label: RestaurantCaptureCopy.amountLabel("ORDER", orderSide: draft.orderSide),
            amount: formattedMoney(goodsTotalString),
            caption: heroCaption
        )

        EmberFieldLabel(text: isPo ? "Raw materials" : "Menu")
        EmberScanField(
            placeholder: isPo ? "Search ingredient" : "Search dish",
            query: $draft.scanQuery,
            actionLabel: "Add",
            disabled: false,
            action: {}
        )

        EmberChipRow(
            options: isPo ? RestaurantCaptureCopy.poRawCategories : RestaurantCaptureCopy.soMenuCategories,
            selection: $draft.catalogCategory,
            style: .channel
        )

        catalogueBrowser(isPo: isPo)
        Button(isPo ? "+ New raw" : "+ New dish") {
            masterFormKind = isPo ? .raw : .dish
        }
        .font(.plusJakarta(size: 12, weight: .bold))
        .foregroundStyle(EmberSheetChrome.gold)
        .buttonStyle(.plain)

        EmberLinesCard(
            title: isPo ? "PO lines" : "Order lines",
            trailing: "\(filledLineCount) line\(filledLineCount == 1 ? "" : "s")"
        ) {
            ForEach($draft.lines) { $line in
                orderLineEditor(line: $line, isPo: isPo)
            }
            Button("+ Add line") { draft.lines.append(RestaurantLineDraft()) }
                .font(.plusJakarta(size: 13, weight: .bold))
                .foregroundStyle(EmberSheetChrome.gold)
                .buttonStyle(.plain)
        }

        EmberBreakdownCard {
            if isPo {
                EmberMoneyRow(label: "Goods", value: formattedMoney(goodsTotalString))
                EmberMoneyRow(label: "Est. GST", value: "Unavailable — posts on Purchase", gold: true)
            } else {
                EmberMoneyRow(label: "Lines value", value: formattedMoney(goodsTotalString))
                EmberMoneyRow(label: "Est. GST", value: "Unavailable — posts on bill", gold: true)
            }
        }

        EmberPickerRow(
            label: isPo ? "Supplier" : "Customer",
            value: draft.counterparty.isEmpty
                ? (isPo ? "Select supplier" : "Walk-in / table")
                : draft.counterparty,
            action: { isPo ? cycleSupplier() : cycleCustomer() }
        )
    }

    @ViewBuilder
    private func catalogueBrowser(isPo: Bool) -> some View {
        let items = filteredCatalog(isPo: isPo)
        if items.isEmpty {
            Text("No masters yet — type lines manually.")
                .font(.plusJakarta(size: 12))
                .foregroundStyle(EmberSheetChrome.textBody)
        }
        ForEach(items.prefix(5)) { sku in
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text(sku.name)
                        .font(.plusJakarta(size: 13, weight: .semibold))
                        .foregroundStyle(EmberSheetChrome.textPrimary)
                    Text(sku.code ?? sku.unit ?? "—")
                        .font(.plusJakarta(size: 10))
                        .foregroundStyle(EmberSheetChrome.textBody)
                }
                Spacer()
                Button("Add") { addCatalogLine(sku, isPo: isPo) }
                    .font(.plusJakarta(size: 12, weight: .bold))
                    .foregroundStyle(EmberSheetChrome.inkOnEmber)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 6)
                    .background(EmberSheetChrome.ember)
                    .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
                    .buttonStyle(.plain)
            }
            .padding(10)
            .background(EmberSheetChrome.chip)
            .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
        }
    }

    @ViewBuilder
    private func orderLineEditor(line: Binding<RestaurantLineDraft>, isPo: Bool) -> some View {
        let qty = Int(line.wrappedValue.qty) ?? 1
        VStack(alignment: .leading, spacing: 6) {
            HStack {
                EmberTextField(placeholder: isPo ? "Ingredient" : "Dish", text: line.name)
                    .onChange(of: line.wrappedValue.name) { _, _ in syncAmountFromLines() }
                if !isPo {
                    Button(line.wrappedValue.holdForFire ? "Hold for fire" : "Fire now") {
                        line.wrappedValue.holdForFire.toggle()
                    }
                    .font(.plusJakarta(size: 10, weight: .bold))
                    .foregroundStyle(line.wrappedValue.holdForFire ? EmberSheetChrome.amber : EmberSheetChrome.tenderSelected)
                    .buttonStyle(.plain)
                }
            }
            HStack {
                EmberStepper(quantity: qty, onDecrement: {
                    if qty > 1 {
                        line.wrappedValue.qty = String(qty - 1)
                        syncAmountFromLines()
                    }
                }, onIncrement: {
                    line.wrappedValue.qty = String(qty + 1)
                    syncAmountFromLines()
                })
                EmberTextField(placeholder: "Rate", text: line.rate, keyboard: .decimalPad)
                    .onChange(of: line.wrappedValue.rate) { _, _ in syncAmountFromLines() }
                Text(formattedMoney(NSDecimalNumber(decimal: line.wrappedValue.lineTotal).stringValue))
                    .font(.plusJakarta(size: 12, weight: .bold))
                    .foregroundStyle(EmberSheetChrome.gold)
            }
        }
        .padding(.vertical, 6)
    }

    // MARK: AR / AP lists

    private var openReceivablesList: [OperatingPaymentPayload] {
        (model.projection?.paymentReadiness ?? []).filter { $0.relevance.uppercased() == "RECEIVABLE" }
    }

    private var openPayablesList: [OperatingPaymentPayload] {
        (model.projection?.paymentReadiness ?? []).filter { $0.relevance.uppercased() != "RECEIVABLE" }
    }

    private var selectedReceivable: OperatingPaymentPayload? {
        openReceivablesList.first { $0.id == draft.parentEventId } ?? openReceivablesList.first
    }

    private var selectedPayable: OperatingPaymentPayload? {
        openPayablesList.first { $0.id == draft.parentEventId } ?? openPayablesList.first
    }

    // MARK: Budget · RST-QA-09

    @ViewBuilder
    private var budgetBody: some View {
        EmberChipRow(
            options: RestaurantCaptureCopy.modeChips("BUDGET"),
            selection: $draft.revisionKind
        )
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 6) {
                EmberModeChip(text: brand.isEmpty ? "Spice Route" : brand, selected: true, style: .channel) {}
                EmberModeChip(text: draft.periodChip.isEmpty ? "Nov 2026" : draft.periodChip, selected: false) {}
                EmberModeChip(text: "v2 draft", selected: false) {}
            }
        }
        let previous = draft.budgetPrevious.isEmpty ? RestaurantCaptureCopy.budgetFixturePrevious : draft.budgetPrevious
        let prev = Decimal(string: previous) ?? 0
        let next = Decimal(string: draft.amount) ?? 0
        let delta = next - prev
        let pct: String = {
            guard prev > 0 else { return "+14%" }
            let p = NSDecimalNumber(decimal: delta * 100 / prev)
            return String(format: "%+d%%", p.intValue)
        }()
        EmberAmountHero(
            label: "New target",
            amount: formattedMoney(draft.amount.isEmpty ? RestaurantCaptureCopy.budgetFixtureNew : draft.amount),
            caption: "Effective 01 Nov · was ₹3.5L"
        )
        EmberLinesCard(title: "Revision lines", trailing: "v1 → v2") {
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text("Sales target").font(.plusJakarta(size: 14, weight: .semibold)).foregroundStyle(EmberSheetChrome.textPrimary)
                    Text("v1 → v2").font(.plusJakarta(size: 11)).foregroundStyle(EmberSheetChrome.textBody)
                }
                Spacer()
                VStack(alignment: .trailing, spacing: 2) {
                    Text("₹3.5L → ₹4.0L").font(.plusJakarta(size: 14, weight: .semibold)).foregroundStyle(EmberSheetChrome.textPrimary)
                    Text(pct).font(.plusJakarta(size: 11)).foregroundStyle(EmberSheetChrome.gold)
                }
            }
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text("Covers / day").font(.plusJakarta(size: 14, weight: .semibold)).foregroundStyle(EmberSheetChrome.textPrimary)
                    Text("avg target").font(.plusJakarta(size: 11)).foregroundStyle(EmberSheetChrome.textBody)
                }
                Spacer()
                VStack(alignment: .trailing, spacing: 2) {
                    Text("\(draft.coversPrev.isEmpty ? "86" : draft.coversPrev) → \(draft.coversNew.isEmpty ? "100" : draft.coversNew)")
                        .font(.plusJakarta(size: 14, weight: .semibold))
                        .foregroundStyle(EmberSheetChrome.textPrimary)
                    Text("+14").font(.plusJakarta(size: 11)).foregroundStyle(EmberSheetChrome.gold)
                }
            }
        }
        EmberMoreDetails(hint: "Edit amounts · period · covers", open: $moreOpen) {
            EmberFieldLabel(text: "New target")
            EmberTextField(placeholder: RestaurantCaptureCopy.budgetFixtureNew, text: $draft.amount, keyboard: .decimalPad)
            EmberFieldLabel(text: "Covers new")
            EmberTextField(placeholder: "100", text: $draft.coversNew, keyboard: .numberPad)
            EmberFieldLabel(text: "Period")
            EmberTextField(placeholder: "Nov 2026", text: $draft.periodChip)
            EmberFieldLabel(text: "Effective")
            EmberTextField(placeholder: "01 Nov 2026", text: $draft.effectiveFrom)
        }
    }

    // MARK: Issue · RST-QA-10

    @ViewBuilder
    private var issueBody: some View {
        EmberChipRow(
            options: RestaurantCaptureCopy.modeChips("ISSUE"),
            selection: $draft.revisionKind
        )
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 6) {
                EmberModeChip(text: brand.isEmpty ? "Spice Route" : brand, selected: true, style: .channel) {}
                EmberModeChip(text: "Kitchen ops", selected: false) {}
                EmberModeChip(text: "Audit", selected: false) {}
            }
        }
        EmberAmountHero(
            label: "Severity",
            amount: draft.severity.isEmpty ? "Alert" : draft.severity,
            caption: "Spoilage · dal 2kg"
        )
        EmberLinesCard(title: "Linked", trailing: "Register · shift") {
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text(draft.title.isEmpty ? RestaurantCaptureCopy.issueFixtureTitle : draft.title)
                        .font(.plusJakarta(size: 14, weight: .semibold))
                        .foregroundStyle(EmberSheetChrome.textPrimary)
                    Text("2 kg · Prep").font(.plusJakarta(size: 11)).foregroundStyle(EmberSheetChrome.textBody)
                }
                Spacer()
                VStack(alignment: .trailing, spacing: 2) {
                    Text(draft.severity.isEmpty ? "Alert" : draft.severity)
                        .font(.plusJakarta(size: 12, weight: .bold))
                        .foregroundStyle(EmberSheetChrome.negative)
                    Text(draft.link.isEmpty ? RestaurantCaptureCopy.issueFixtureLink : draft.link)
                        .font(.plusJakarta(size: 11))
                        .foregroundStyle(EmberSheetChrome.gold)
                }
            }
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text("Owner").font(.plusJakarta(size: 14, weight: .semibold)).foregroundStyle(EmberSheetChrome.textPrimary)
                    Text(draft.staff.isEmpty ? RestaurantCaptureCopy.issueFixtureOwner : draft.staff)
                        .font(.plusJakarta(size: 11)).foregroundStyle(EmberSheetChrome.textBody)
                }
                Spacer()
                Text("Open")
                    .font(.plusJakarta(size: 11, weight: .bold))
                    .foregroundStyle(EmberSheetChrome.amber)
            }
        }
        EmberChipRow(options: RestaurantCaptureCopy.issueSeverities, selection: $draft.severity)
        EmberMoreDetails(hint: "Notes · Evidence · Tags", open: $moreOpen) {
            EmberFieldLabel(text: "Title (edit)")
            EmberTextField(placeholder: RestaurantCaptureCopy.issueFixtureTitle, text: $draft.title)
            EmberFieldLabel(text: "Owner (edit)")
            EmberTextField(placeholder: RestaurantCaptureCopy.issueFixtureOwner, text: $draft.staff)
            EmberFieldLabel(text: "Link (edit)")
            EmberTextField(placeholder: RestaurantCaptureCopy.issueFixtureLink, text: $draft.link)
            EmberFieldLabel(text: "Notes · Evidence")
            EmberTextField(placeholder: "Notes · Evidence · Tags", text: $draft.note)
        }
    }

    @ViewBuilder
    private var openReceivablesBlock: some View {
        EmberLinesCard(
            title: "Allocate against",
            trailing: "\(openReceivablesList.count) open"
        ) {
            if openReceivablesList.isEmpty {
                EmberEmptyCard(title: "No open AR", bodyText: "Post a cloud bill before collecting.")
            } else {
                ForEach(openReceivablesList.prefix(5)) { row in
                    EmberAllocateRow(
                        title: row.title,
                        subtitle: row.counterpartyName ?? "Platform",
                        amount: formattedMoney(row.amount),
                        trailing: row.dueAt == nil ? "Open" : "Due",
                        selected: draft.parentEventId == row.id
                    ) {
                        draft.parentEventId = row.id
                        draft.counterparty = row.counterpartyName ?? draft.counterparty
                        if draft.amount.isEmpty { draft.amount = row.amount }
                        draft.title = row.title
                    }
                }
            }
        }
    }

    @ViewBuilder
    private var openPayablesBlock: some View {
        EmberLinesCard(title: "Allocate against", trailing: "\(openPayablesList.count) open") {
            if openPayablesList.isEmpty {
                EmberEmptyCard(title: "Nothing to pay", bodyText: "Open purchases appear here.")
            } else {
                ForEach(openPayablesList.prefix(5)) { row in
                    EmberAllocateRow(
                        title: row.title,
                        subtitle: row.counterpartyName ?? "Supplier",
                        amount: formattedMoney(row.amount),
                        trailing: "AP",
                        selected: draft.parentEventId == row.id
                    ) {
                        draft.parentEventId = row.id
                        draft.counterparty = row.counterpartyName ?? draft.counterparty
                        if draft.amount.isEmpty { draft.amount = row.amount }
                        draft.title = row.title
                    }
                }
            }
        }
    }

    // MARK: Catalog + cycles

    private func filteredCatalog(isPo: Bool) -> [OperatingMasterPayload] {
        catalogMasters(isPo: isPo).filter { sku in
            let q = draft.scanQuery.trimmingCharacters(in: .whitespaces)
            if q.isEmpty { return true }
            return sku.name.localizedCaseInsensitiveContains(q)
                || (sku.code?.localizedCaseInsensitiveContains(q) ?? false)
        }
    }

    private func catalogMasters(isPo: Bool) -> [OperatingMasterPayload] {
        let source = model.masters.isEmpty ? (model.projection?.masters ?? []) : model.masters
        return source.filter { master in
            let kind = master.kind.uppercased()
            if isPo {
                return ["SKU", "RAW_MATERIAL", "INGREDIENT", "PRODUCT"].contains(kind)
            }
            return ["MENU_DISH", "PRODUCT", "ITEM", "SKU"].contains(kind)
        }
        .sorted { $0.name.localizedCaseInsensitiveCompare($1.name) == .orderedAscending }
    }

    private func addCatalogLine(_ sku: OperatingMasterPayload, isPo: Bool) {
        let rate = (isPo ? sku.purchasePrice : sku.sellPrice) ?? sku.sellPrice ?? sku.purchasePrice ?? ""
        let line = RestaurantLineDraft(
            sku: sku.code ?? "",
            name: sku.name,
            rate: rate,
            unit: sku.unit ?? (isPo ? "kg" : "plate")
        )
        if let idx = draft.lines.firstIndex(where: { $0.name.isEmpty }) {
            draft.lines[idx] = line
        } else {
            draft.lines.append(line)
        }
        syncAmountFromLines()
    }

    private func cycleSupplier() {
        let kinds: Set<String> = ["VENDOR", "SUPPLIER", "PRINCIPAL"]
        let suppliers = model.masters.filter { kinds.contains($0.kind.uppercased()) }
        guard !suppliers.isEmpty else {
            draft.counterparty = draft.counterparty.isEmpty ? "Fresh Farm" : nextFixture(["Fresh Farm", "Dairy Co", "Spice Traders"], current: draft.counterparty)
            return
        }
        let names = suppliers.map(\.name)
        draft.counterparty = nextFixture(names, current: draft.counterparty)
    }

    private func cycleCustomer() {
        draft.counterparty = nextFixture(["Walk-in", "Table 12", "Catering · Rao", "Zomato guest"], current: draft.counterparty)
    }

    private func cycleReceivableParty() {
        draft.counterparty = nextFixture(["Zomato AR", "Swiggy AR", "Customer AR"], current: draft.counterparty)
    }

    private func nextFixture(_ options: [String], current: String) -> String {
        let trimmed = current.trimmingCharacters(in: .whitespaces)
        let idx = options.firstIndex(where: { $0 == trimmed }) ?? -1
        return options[(idx + 1) % options.count]
    }

    private func nextKitchenLocation(_ current: String) -> String {
        nextFixture([kitchenLine, "Cold store", "Dry store", "Pass"], current: current)
    }

    private func nextStockLocation(_ current: String) -> String {
        nextFixture(["Cold store", "Dry store", "Bar", "Pass"], current: current)
    }

    // MARK: Save + sync

    private func syncAmountFromLines() {
        if ["PURCHASE", "ORDER"].contains(upper), !filledLines.isEmpty {
            draft.amount = goodsTotalString
        }
    }

    private func syncStockModeChip(_ chip: String) {
        draft.stockReason = RestaurantCaptureCopy.stockReasonSeed(chip)
        switch chip {
        case "Count":
            draft.stockAction = "Set current"
        case "Waste", "Usage":
            draft.stockAction = "Reduce"
        default:
            draft.stockAction = "Set current"
        }
    }

    private func appendPurchaseScanLine() {
        let q = draft.scanQuery.trimmingCharacters(in: .whitespaces)
        guard !q.isEmpty else { return }
        if let idx = draft.lines.firstIndex(where: { $0.name.isEmpty }) {
            draft.lines[idx].name = q
        } else {
            draft.lines.append(RestaurantLineDraft(name: q, unit: "kg"))
        }
        draft.scanQuery = ""
        syncAmountFromLines()
    }

    private func appendStockScanLine() {
        let q = draft.scanQuery.trimmingCharacters(in: .whitespaces)
        guard !q.isEmpty else { return }
        if let match = catalogMasters(isPo: true).first(where: {
            $0.name.localizedCaseInsensitiveContains(q) || ($0.code?.localizedCaseInsensitiveContains(q) ?? false)
        }) {
            addCatalogLine(match, isPo: true)
        } else if let idx = draft.lines.firstIndex(where: { $0.name.isEmpty }) {
            draft.lines[idx].name = q
        } else {
            draft.lines.append(RestaurantLineDraft(name: q))
        }
        draft.scanQuery = ""
    }

    private func ensureDefaults() {
        if draft.lines.count < 2 {
            while draft.lines.count < 2 { draft.lines.append(RestaurantLineDraft()) }
        }
        if draft.goodsMode.isEmpty {
            draft.goodsMode = RestaurantCaptureCopy.defaultModeChip(upper)
        }
        if RestaurantCaptureCopy.moneyModes.contains(upper) {
            if draft.tender.isEmpty || !RestaurantCaptureCopy.tenderChips(upper).contains(draft.tender) {
                draft.tender = RestaurantCaptureCopy.defaultTender(upper)
            }
        }
        if upper == "COLLECTION", draft.parentEventId == nil, let first = openReceivablesList.first {
            draft.parentEventId = first.id
            draft.amount = first.amount
            draft.title = first.title
            if draft.counterparty.isEmpty { draft.counterparty = first.counterpartyName ?? "" }
        }
        if upper == "PAYMENT", draft.parentEventId == nil, let first = openPayablesList.first {
            draft.parentEventId = first.id
            draft.amount = first.amount
            draft.title = first.title
            if draft.counterparty.isEmpty { draft.counterparty = first.counterpartyName ?? "" }
        }
        if upper == "ORDER" {
            if draft.orderSide.isEmpty { draft.orderSide = "Sales Order" }
            if draft.channel.isEmpty { draft.channel = "Dine-in" }
            if draft.catalogCategory.isEmpty {
                draft.catalogCategory = RestaurantCaptureCopy.soMenuCategories.first ?? "Mains"
            }
            if draft.stateChip.isEmpty { draft.stateChip = "Placed" }
        }
        if upper == "STOCK" {
            syncStockModeChip(draft.goodsMode)
            if draft.location.isEmpty { draft.location = "Cold store" }
        }
        if ["BUDGET", "TARGET"].contains(upper) {
            if draft.revisionKind.isEmpty || !RestaurantCaptureCopy.modeChips("BUDGET").contains(draft.revisionKind) {
                draft.revisionKind = upper == "TARGET" ? "Covers" : "Sales"
            }
            if draft.amount.isEmpty { draft.amount = RestaurantCaptureCopy.budgetFixtureNew }
            if draft.budgetPrevious.isEmpty { draft.budgetPrevious = RestaurantCaptureCopy.budgetFixturePrevious }
            if draft.periodChip.isEmpty { draft.periodChip = "Nov 2026" }
            if draft.effectiveFrom.isEmpty { draft.effectiveFrom = "01 Nov 2026" }
        }
        if ["ISSUE", "UPDATE", "APPROVAL"].contains(upper) {
            if draft.revisionKind.isEmpty || !RestaurantCaptureCopy.modeChips("ISSUE").contains(draft.revisionKind) {
                draft.revisionKind = upper == "APPROVAL" ? "Approval" : (upper == "UPDATE" ? "Update" : "Issue")
            }
            if draft.title.isEmpty { draft.title = RestaurantCaptureCopy.issueFixtureTitle }
            if draft.staff.isEmpty { draft.staff = RestaurantCaptureCopy.issueFixtureOwner }
            if draft.link.isEmpty { draft.link = RestaurantCaptureCopy.issueFixtureLink }
            if draft.severity.isEmpty { draft.severity = "Alert" }
        }
        if upper == "SPEND" {
            if draft.expenseMode.isEmpty { draft.expenseMode = "Kitchen" }
            if draft.paymentChoice.isEmpty { draft.paymentChoice = "Paid now" }
        }
    }

    private func save() async {
        guard canSave else { return }
        saving = true
        defer { saving = false }
        if ["PURCHASE", "ORDER"].contains(upper) { syncAmountFromLines() }
        let input = RestaurantCaptureEventBuilder(mode: mode, draft: draft, model: model).build()
        let ok = await model.record(input)
        if ok { dismiss() }
    }

    private func formattedMoney(_ raw: String?) -> String {
        guard let raw, !raw.isEmpty else { return "—" }
        return OperatingV12Money.amount(raw, currency: model.currency)
    }
}

// MARK: - Draft

private struct RestaurantLineDraft: Identifiable, Equatable {
    var id = UUID()
    var sku = ""
    var name = ""
    var qty = "1"
    var rate = ""
    var unit = "kg"
    var gst = ""
    var holdForFire = false

    var lineTotal: Decimal {
        let q = Decimal(string: qty) ?? 0
        let r = Decimal(string: rate) ?? 0
        return q * r
    }
}

private struct RestaurantCaptureDraft {
    var title = ""
    var amount = ""
    var note = ""
    var counterparty = ""
    var channel = ""
    var goodsMode = ""
    var expenseMode = "Kitchen"
    var tender = ""
    var paymentChoice = "Paid now"
    var scanQuery = ""
    var location = ""
    var stockAction = "Set current"
    var stockReason = "Par / spoilage"
    var orderSide = "Sales Order"
    var stateChip = "Placed"
    var catalogCategory = "Mains"
    var revisionKind = ""
    var severity = "Alert"
    var staff = ""
    var link = ""
    var periodChip = "Nov 2026"
    var effectiveFrom = "01 Nov 2026"
    var budgetPrevious = RestaurantCaptureCopy.budgetFixturePrevious
    var coversPrev = "86"
    var coversNew = "100"
    var parentEventId: String?
    var occurredAt: String? = ISO8601DateFormatter().string(from: Date())
    var lines: [RestaurantLineDraft] = [RestaurantLineDraft(), RestaurantLineDraft()]
}

// MARK: - Event builder

private struct RestaurantCaptureEventBuilder {
    let mode: String
    let draft: RestaurantCaptureDraft
    let model: BusinessOperatingModel

    private var upper: String { mode.uppercased() }
    private var layout: OperatingCaptureLayout? { OperatingCaptureLayout.forMode(mode) }

    private var filledLines: [RestaurantLineDraft] {
        draft.lines.filter { !$0.name.trimmingCharacters(in: .whitespaces).isEmpty }
    }

    var canSave: Bool {
        switch upper {
        case "COLLECTION", "PAYMENT":
            return draft.parentEventId != nil && !draft.amount.trimmingCharacters(in: .whitespaces).isEmpty
        case "PURCHASE":
            return !draft.counterparty.trimmingCharacters(in: .whitespaces).isEmpty
                && !filledLines.isEmpty
                && (Decimal(string: draft.amount).map { $0 > 0 } ?? false)
        case "SPEND":
            return Decimal(string: draft.amount).map { $0 > 0 } ?? false
        case "STOCK":
            guard let first = filledLines.first else { return false }
            let qty = Decimal(string: first.qty) ?? -1
            let action = OperatingContractVocabulary.stockAction(draft.stockAction)
            let reasonOk = OperatingContractVocabulary.stockReason(draft.stockReason) != nil
            if action == "SET" { return qty >= 0 && reasonOk }
            return qty > 0 && reasonOk
        case "ORDER":
            return !filledLines.isEmpty || (Decimal(string: draft.amount).map { $0 > 0 } ?? false)
        case "BUDGET", "TARGET":
            return Decimal(string: draft.amount).map { $0 > 0 } ?? false
        case "ISSUE", "UPDATE", "APPROVAL":
            return !draft.title.trimmingCharacters(in: .whitespaces).isEmpty
        default:
            return false
        }
    }

    func build() -> OperatingEventInput {
        var contract = OperatingContractInput()
        let amount: String? = upper == "STOCK" ? nil : (draft.amount.isEmpty ? nil : draft.amount)

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
            quantity: filledLines.first?.qty,
            unit: filledLines.first?.unit,
            paymentStatus: paymentStatus
        )

        if layout == .purchase {
            contract.entryMode = "ITEMIZED"
            contract.goodsStatus = "RECEIVED"
            contract.receiptState = "PARTIAL"
            contract.locationName = draft.location.isEmpty ? nil : draft.location
            switch draft.tender {
            case "UPI now", "Cash": contract.paymentState = "PAID"
            default: contract.paymentState = "PAY_LATER"
            }
        }

        if layout == .spend {
            contract.expenseCategory = "\(draft.expenseMode)/\(draft.goodsMode.isEmpty ? "General" : draft.goodsMode)"
            contract.paymentState = draft.paymentChoice == "Paid now" ? "PAID" : "PAY_LATER"
        }

        if layout == .stock {
            let action = OperatingContractVocabulary.stockAction(draft.stockAction)
            contract.stockAction = action
            contract.stockReason = OperatingContractVocabulary.stockReason(draft.stockReason)
            contract.locationName = draft.location.isEmpty ? nil : draft.location
            if let first = filledLines.first {
                input.title = first.name
                input.quantity = first.qty
                input.unit = first.unit
                if action == "SET" { contract.declaredQuantity = first.qty }
            }
        }

        if upper == "ORDER" {
            contract.orderSide = draft.orderSide == "Purchase Order" ? "PURCHASE" : "SALES"
            contract.orderStatus = orderStatusToken(draft.stateChip)
            contract.estimatedAmount = draft.amount.isEmpty ? nil : draft.amount
            contract.locationName = draft.location.isEmpty ? nil : draft.location
            if draft.stateChip == "Hold" { contract.holdStatus = "HELD" }
        }

        if ["BUDGET", "TARGET"].contains(upper) {
            contract.estimatedAmount = draft.amount.isEmpty ? nil : draft.amount
        }

        if layout == .collection || layout == .payment,
           let parentId = draft.parentEventId,
           !draft.amount.isEmpty
        {
            contract.paymentState = layout == .collection ? "RECEIVED" : "PAID"
            contract.receivedAmount = draft.amount
            contract.allocations = [OperatingAllocationInput(sourceId: parentId, amount: draft.amount)]
        }

        if upper != "STOCK" || !filledLines.isEmpty {
            let lineItems = filledLines.map {
                OperatingEventInput.OperatingLineItem(
                    itemName: $0.name,
                    quantity: $0.qty,
                    unit: $0.unit,
                    unitPrice: $0.rate.isEmpty ? nil : $0.rate,
                    masterId: nil
                )
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
            return draft.orderSide == "Purchase Order"
                ? "PO · \(draft.counterparty.isEmpty ? "Supplier" : draft.counterparty)"
                : "Sales order · \(draft.channel.isEmpty ? "Dine-in" : draft.channel)"
        case "SPEND":
            return "\(draft.expenseMode) · expense"
        case "COLLECTION":
            return "Collection · \(draft.counterparty.isEmpty ? "Platform AR" : draft.counterparty)"
        case "PAYMENT":
            return "Payment · \(draft.counterparty.isEmpty ? "AP" : draft.counterparty)"
        case "STOCK":
            return filledLines.first?.name ?? "Stock · \(draft.goodsMode)"
        case "BUDGET", "TARGET":
            return "\(draft.revisionKind.isEmpty ? "Sales" : draft.revisionKind) · \(draft.periodChip)"
        case "ISSUE", "UPDATE", "APPROVAL":
            return draft.title.isEmpty ? RestaurantCaptureCopy.issueFixtureTitle : draft.title
        default:
            return trimmed
        }
    }

    private var resolvedChannel: String? {
        switch upper {
        case "PURCHASE", "SPEND": return draft.goodsMode.isEmpty ? nil : draft.goodsMode
        case "COLLECTION", "PAYMENT": return draft.tender
        case "ORDER": return draft.channel.isEmpty ? draft.orderSide : draft.channel
        default: return nil
        }
    }

    private var eventType: String {
        switch upper {
        case "ISSUE": return "ISSUE"
        case "APPROVAL": return "APPROVAL"
        case "UPDATE": return "UPDATE"
        default: break
        }
        if ["ISSUE", "UPDATE", "APPROVAL"].contains(upper) || draft.revisionKind == "Issue" || draft.revisionKind == "Approval" || draft.revisionKind == "Update" {
            switch draft.revisionKind {
            case "Issue": return "ISSUE"
            case "Approval": return "APPROVAL"
            case "Update": return "UPDATE"
            default: break
            }
        }
        switch layout {
        case .purchase: return "PURCHASE"
        case .collection: return "COLLECTION"
        case .payment: return "PAYMENT"
        case .spend: return "SPEND"
        case .stock: return "STOCK"
        case .order: return "ORDER"
        case .budget: return "BUDGET"
        case .update: return draft.revisionKind == "Issue" ? "ISSUE" : "UPDATE"
        case .approval: return "APPROVAL"
        default: return upper
        }
    }

    private var paymentStatus: String? {
        switch layout {
        case .collection: return "COLLECTED"
        case .payment: return "PAID"
        case .purchase:
            switch draft.tender {
            case "UPI now", "Cash": return "PAID"
            default: return "OPEN"
            }
        case .spend:
            return draft.paymentChoice == "Paid now" ? "PAID" : "OPEN"
        default:
            if draft.revisionKind == "Approval" { return "PENDING" }
            return nil
        }
    }

    private func orderStatusToken(_ chip: String) -> String {
        switch chip {
        case "Preparing": return "PREPARING"
        case "Ready": return "READY"
        case "Placed", "Open": return "PLACED"
        case "Partial": return "PARTIAL"
        case "Received": return "RECEIVED"
        default: return "PLACED"
        }
    }

    private func combinedNote() -> String? {
        var parts: [String] = []
        if !draft.note.isEmpty { parts.append(draft.note) }
        if upper == "PURCHASE" {
            parts.append("Mode: \(draft.goodsMode)")
            parts.append("Tender: \(draft.tender)")
        }
        if upper == "COLLECTION" || upper == "PAYMENT" {
            parts.append("Settle: \(draft.goodsMode)")
            parts.append("Tender: \(draft.tender)")
        }
        if upper == "STOCK" {
            parts.append("Mode: \(draft.goodsMode)")
            parts.append("Reason: \(draft.stockReason)")
        }
        if upper == "ORDER" {
            parts.append("Side: \(draft.orderSide)")
            parts.append("State: \(draft.stateChip)")
            let holds = filledLines.filter(\.holdForFire).map(\.name)
            if !holds.isEmpty { parts.append("Hold for fire: \(holds.joined(separator: ", "))") }
        }
        if ["BUDGET", "TARGET"].contains(upper) {
            parts.append("Metric: \(draft.revisionKind)")
            parts.append("Period: \(draft.periodChip)")
            parts.append("Effective: \(draft.effectiveFrom)")
            parts.append("Previous: \(draft.budgetPrevious)")
            parts.append("Covers: \(draft.coversPrev)→\(draft.coversNew)")
            parts.append("Versioned plan — achievement uses posted bills only")
        }
        if ["ISSUE", "UPDATE", "APPROVAL"].contains(upper) {
            parts.append("Severity: \(draft.severity)")
            parts.append("Owner: \(draft.staff)")
            parts.append("Link: \(draft.link)")
            parts.append("approval ≠ disbursement")
        }
        return parts.isEmpty ? nil : parts.joined(separator: " · ")
    }
}

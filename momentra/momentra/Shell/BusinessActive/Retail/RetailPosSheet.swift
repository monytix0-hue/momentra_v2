import SwiftUI
import UIKit

/// Retail POS — Figma RTL-QA-01 ALT: Scan cart · Hold recall · Quick amount.
/// Posts `SALE` via `BusinessOperatingModel.record`. Quick amount has no lines / no stock movement.
/// Hold recall restores a device-local park into Scan cart — Charge still posts from Scan cart.
struct RetailPosSheet: View {
    let theme: OperatingTheme
    @ObservedObject var model: BusinessOperatingModel

    @Environment(\.dismiss) private var dismiss

    private enum PosMode: String, CaseIterable {
        case scanCart = "Scan cart"
        case holdRecall = "Hold recall"
        case quickAmount = "Quick amount"
    }

    @State private var posMode: PosMode = .scanCart
    @State private var salesMode = ""
    @State private var tender = "UPI"
    @State private var splitTender = false
    @State private var splitUpi = ""
    @State private var splitCash = ""
    @State private var splitCard = ""
    @State private var discount = ""
    @State private var cessAmount = ""
    @State private var skuQuery = ""
    @State private var holdQuery = ""
    @State private var customer = ""
    @State private var note = ""
    @State private var quickAmount = ""
    @State private var cart: [RetailPosLine] = []
    @State private var saving = false
    @State private var recalledDraftId: String?
    @State private var selectedHoldId: String?
    @State private var showSkuPicker = false
    @State private var showBarcodeScanner = false
    @State private var gstPicker = "Inclusive · selection only"
    @State private var billPrint = "Thermal · share UPI QR"
    @State private var postedReceipt: RetailReceiptModel?
    @State private var shareItems: [Any] = []
    @State private var showShareSheet = false
    @State private var paper: RetailReceiptPaperSize = .default
    @State private var shareFormat: RetailReceiptShareFormat = .jpg
    @State private var logo: UIImage? = nil

    private var currency: String { model.currency }

    private var catalogue: [OperatingMasterPayload] {
        let source = model.masters.isEmpty ? (model.projection?.masters ?? []) : model.masters
        return source
            .filter { ["SKU", "PRODUCT", "ITEM", "BATCH"].contains($0.kind.uppercased()) }
            .sorted { $0.name.localizedCaseInsensitiveCompare($1.name) == .orderedAscending }
    }

    private var channels: [String] {
        let fromLife = model.projection?.life?.channels.filter { !$0.isEmpty } ?? []
        return fromLife.isEmpty ? ["Counter", "Online", "Delivery"] : Array(fromLife.prefix(3))
    }

    private var parked: [RetailParkedSale] {
        RetailParkedSaleStore.shared.list(momentId: model.momentId)
    }

    private var filteredParked: [RetailParkedSale] {
        let q = holdQuery.trimmingCharacters(in: .whitespaces)
        guard !q.isEmpty else { return parked }
        return parked.filter {
            $0.label.localizedCaseInsensitiveContains(q)
                || $0.id.localizedCaseInsensitiveContains(q)
                || $0.meta.localizedCaseInsensitiveContains(q)
                || $0.customer.localizedCaseInsensitiveContains(q)
        }
    }

    private var selectedHold: RetailParkedSale? {
        parked.first { $0.id == selectedHoldId }
    }

    private var cartUnits: Int { cart.reduce(0) { $0 + $1.quantity } }

    private var gross: Decimal {
        switch posMode {
        case .quickAmount:
            return PosSettlement.parseAmount(quickAmount)
        case .scanCart, .holdRecall:
            return cart.reduce(Decimal.zero) { $0 + $1.amount }
        }
    }

    private var subtotal: Decimal { gross }

    private var discountValue: Decimal { PosSettlement.parseAmount(discount) }
    private var cessValue: Decimal { PosSettlement.parseAmount(cessAmount) }
    private var payable: Decimal { PosSettlement.payable(gross: gross, discount: discountValue, cessAmount: cessValue) }
    private var splitAmounts: [String: String] {
        ["UPI": splitUpi, "Cash": splitCash, "Card": splitCard]
    }
    private var paymentsPreview: [OperatingPaymentInput]? {
        PosSettlement.buildPayments(
            split: splitTender,
            primary: tender,
            payable: payable,
            splitAmounts: splitAmounts
        )
    }
    private var splitOk: Bool {
        !splitTender || PosSettlement.splitSumsToPayable(splitAmounts, payable: payable)
    }
    private var discountOk: Bool { discountValue <= gross || gross <= 0 }
    private var tenderLabel: String {
        PosSettlement.tenderLabel(split: splitTender, primary: tender, payments: paymentsPreview)
    }

    private var subtotalText: String {
        guard gross > 0 else { return "—" }
        return OperatingV12Money.amount(NSDecimalNumber(decimal: gross).stringValue, currency: currency)
    }

    private var payableText: String {
        guard payable > 0 else { return "—" }
        return OperatingV12Money.amount(NSDecimalNumber(decimal: payable).stringValue, currency: currency)
    }

    private var discountText: String {
        guard discountValue > 0 else { return "—" }
        return "−" + OperatingV12Money.amount(NSDecimalNumber(decimal: discountValue).stringValue, currency: currency)
    }

    private var postable: Bool {
        switch posMode {
        case .quickAmount:
            return payable > 0 && discountOk && splitOk
        case .scanCart:
            return !cart.isEmpty
                && cart.allSatisfy { ($0.unitPrice as NSString).doubleValue > 0 }
                && discountOk
                && splitOk
                && payable > 0
        case .holdRecall:
            return false
        }
    }

    private var recallReady: Bool {
        posMode == .holdRecall && selectedHold != nil
    }

    var body: some View {
        Group {
            if let receipt = postedReceipt {
                postedReceiptBody(receipt)
            } else {
                ScrollView {
                    VStack(alignment: .leading, spacing: 14) {
                        RoseGrabber()
                        RoseSheetHeader(
                            theme: theme,
                            title: "POS Sale",
                            showAltBadge: posMode != .scanCart,
                            onClose: { dismiss() }
                        )
                        RoseRegisterContext(text: registerContext)

                        RoseChipRow(theme: theme, options: PosMode.allCases.map(\.rawValue), selection: Binding(
                            get: { posMode.rawValue },
                            set: { raw in
                                if let next = PosMode(rawValue: raw) {
                                    posMode = next
                                }
                            }
                        ))

                        switch posMode {
                        case .quickAmount:
                            quickBody
                        case .scanCart:
                            scanBody
                        case .holdRecall:
                            holdRecallBody
                        }
                    }
                    .padding(.horizontal, 20)
                    .padding(.top, 12)
                    .padding(.bottom, 28)
                }
            }
        }
        .background(RoseSheetChrome.sheet)
        .sheet(isPresented: $showShareSheet) {
            ActivityShareSheet(items: shareItems)
        }
        .fullScreenCover(isPresented: $showBarcodeScanner) {
            RetailBarcodeScanner(
                title: "Scan SKU",
                subtitle: "Point at a product barcode or QR",
                onCode: { code in
                    showBarcodeScanner = false
                    applyPosScan(code)
                },
                onDismiss: { showBarcodeScanner = false }
            )
        }
        .task {
            if salesMode.isEmpty || !channels.contains(salesMode) {
                salesMode = channels.first ?? "Counter"
            }
            if model.masters.isEmpty {
                await model.loadMasters()
            }
            logo = RetailReceiptLogoStore.loadImage(
                companyId: model.companyId,
                momentId: model.momentId
            )
        }
    }

    @ViewBuilder
    private func postedReceiptBody(_ receipt: RetailReceiptModel) -> some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                RoseGrabber()
                RoseSheetHeader(
                    theme: theme,
                    title: "Sale posted",
                    subtitle: "Customer copy · share or print",
                    onClose: { dismiss() }
                )
                RetailReceiptPreview(model: receipt, paper: paper, logo: logo)
                Text("Charge already posted. This slip is a client render — no second SALE, no fiscal bill allocation.")
                    .font(.plusJakarta(size: 11))
                    .foregroundStyle(RoseSheetChrome.textMuted)
                RetailReceiptShareControls(
                    theme: theme,
                    model: receipt,
                    companyId: model.companyId,
                    momentId: model.momentId,
                    paper: $paper,
                    format: $shareFormat,
                    logo: $logo
                ) { items in
                    shareItems = items
                    showShareSheet = true
                }
                RoseStickyFooter(
                    theme: theme,
                    title: "Done",
                    caption: "Receipt is client copy until receipt.pdf API exists",
                    action: { dismiss() }
                )
            }
            .padding(.horizontal, 20)
            .padding(.top, 12)
            .padding(.bottom, 28)
        }
    }

    /// Figma register strip — Register/Shift are chrome labels; place + title from projection.
    private var registerContext: String {
        let store = model.title.trimmingCharacters(in: .whitespaces).isEmpty ? "Harbor Mart" : model.title
        let trimmedLocation = model.projection?.location?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        let place: String = {
            if !trimmedLocation.isEmpty { return trimmedLocation }
            if let channel = model.projection?.life?.channels.first(where: {
                $0.localizedCaseInsensitiveContains("Andheri")
            }) {
                return channel
            }
            return "Andheri"
        }()
        return "Register 2 · Shift open · \(place) · \(store)"
    }

    private var footerTitle: String {
        if saving { return "Posting…" }
        switch posMode {
        case .quickAmount:
            return postable ? "Charge \(payableText) · Post" : "Charge · Post"
        case .scanCart:
            return postable ? "Charge \(payableText) · Post & print" : "Charge · Post & print"
        case .holdRecall:
            if let hold = selectedHold {
                return "Recall \(hold.parkDisplayId) → Scan cart"
            }
            return "Recall → Scan cart"
        }
    }

    private var footerCaption: String {
        switch posMode {
        case .scanCart:
            return "Idempotent post · draft kept on picker · original QA-01 untouched"
        case .quickAmount:
            return "No stock move · draft kept on mode switch"
        case .holdRecall:
            return "Does not post · Charge still required on Scan cart"
        }
    }

    // MARK: - Quick amount

    @ViewBuilder
    private var quickBody: some View {
        RoseAmountHero(
            theme: theme,
            label: "Sale amount",
            amountText: payableText,
            caption: "No SKU lines · revenue only · stock unchanged"
        )

        RoseSectionCard(theme: theme, label: "Keypad", trailing: "tap to enter") {
            RoseNumericKeypad(theme: theme, value: $quickAmount)
        }

        quickTotals

        RoseFieldLabel(text: "Discount")
        RoseTextField(theme: theme, placeholder: "0", text: $discount)
        RoseTenderChips(
            theme: theme,
            tender: $tender,
            splitTender: $splitTender,
            splitUpi: $splitUpi,
            splitCash: $splitCash,
            splitCard: $splitCard,
            splitOk: splitOk
        )

        RosePickerRow(
            theme: theme,
            label: "Customer",
            value: customer.isEmpty ? "Walk-in (cash/UPI)" : customer
        )
        RosePickerRow(
            theme: theme,
            label: "Channel",
            value: salesMode.isEmpty ? "In-store · Register 2" : "In-store · \(salesMode)"
        ) {
            cycleChannel()
        }
        RosePickerRow(theme: theme, label: "GST", value: gstPicker.hasPrefix("Inclusive") ? "Inclusive 5%" : "Exclusive · selection only") {
            gstPicker = gstPicker.hasPrefix("Inclusive")
                ? "Exclusive · selection only"
                : "Inclusive · selection only"
        }

        RoseMoreDetails(title: "More details", hint: "Notes · Attachment · Tags") {
            RoseFieldLabel(text: "Customer")
            RoseTextField(theme: theme, placeholder: "Walk-in or named credit buyer", text: $customer)
            RoseFieldLabel(text: "Note")
            RoseTextField(theme: theme, placeholder: "Notes · Attachment · Tags", text: $note)
            RoseFieldLabel(text: "Cess ₹ (optional · explicit only)")
            RoseTextField(theme: theme, placeholder: "0", text: $cessAmount)
            Text("Cess is operator-entered only — no HSN/CBIC schedule lookup.")
                .font(.plusJakarta(size: 10))
                .foregroundStyle(OperatingV12.subtitle.opacity(0.8))
        }

        RoseWhyBox(
            theme: theme,
            text: "Quick amount posts revenue + linked tender with zero SKU movement. Same rule as original QA-01 — kept inside POS so the counter never leaves this sheet. Credit still needs a named customer."
        )

        RoseStickyFooter(
            theme: theme,
            title: footerTitle,
            caption: footerCaption,
            disabled: !postable,
            loading: saving,
            action: { Task { await post() } }
        )
    }

    private var quickGstEstimate: Decimal? {
        guard gstPicker.hasPrefix("Inclusive") else { return nil }
        let rate = gstPicker.split(separator: " ").compactMap { token -> String? in
            let t = token.trimmingCharacters(in: CharacterSet(charactersIn: "%"))
            return Decimal(string: t) != nil ? t : nil
        }.first
        return RetailGstEstimate.onTotal(Decimal(string: quickAmount), ratePercent: rate ?? "5", inclusive: true)
    }

    private var scanGstEstimate: Decimal? {
        RetailGstEstimate.from(
            amounts: cart.map(\.amount),
            rates: cart.map(\.gstRate),
            inclusive: true
        )
    }

    private var quickTotals: some View {
        VStack(spacing: 6) {
            HStack {
                Text("Subtotal")
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(RoseSheetChrome.textBody)
                Spacer()
                Text(subtotalText)
                    .font(.plusJakarta(size: 12, weight: .medium))
                    .foregroundStyle(RoseSheetChrome.textBody)
            }
            if discountValue > 0 {
                HStack {
                    Text("Discount")
                        .font(.plusJakarta(size: 12))
                        .foregroundStyle(RoseSheetChrome.textBody)
                    Spacer()
                    Text(discountText)
                        .font(.plusJakarta(size: 12, weight: .medium))
                        .foregroundStyle(RoseSheetChrome.tenderSelected)
                }
            }
            if cessValue > 0 {
                HStack {
                    Text("Cess")
                        .font(.plusJakarta(size: 12))
                        .foregroundStyle(RoseSheetChrome.textBody)
                    Spacer()
                    Text(OperatingV12Money.amount(NSDecimalNumber(decimal: cessValue).stringValue, currency: currency))
                        .font(.plusJakarta(size: 12, weight: .medium))
                        .foregroundStyle(RoseSheetChrome.textBody)
                }
            }
            RoseGstEstimateRow(
                theme: theme,
                label: gstPicker.hasPrefix("Inclusive") ? "GST (inclusive est.)" : "GST (exclusive · selection)",
                estimate: quickGstEstimate,
                currency: model.currency,
                captionWhenKnown: "Estimate from keypad + rate — GST posts only on Charge",
                captionWhenMissing: "Unavailable — pick Inclusive rate or wait for tax config on post"
            )
            Rectangle()
                .fill(RoseSheetChrome.roseBorder)
                .frame(height: 1)
            HStack(alignment: .bottom) {
                VStack(alignment: .leading, spacing: 2) {
                    Text("Payable now")
                        .font(.plusJakarta(size: 11, weight: .medium))
                        .foregroundStyle(RoseSheetChrome.textBody)
                    Text(payableText)
                        .font(.plusJakarta(size: 24, weight: .bold))
                        .foregroundStyle(OperatingV12.title)
                }
                Spacer()
                VStack(alignment: .trailing, spacing: 2) {
                    Text("Tender")
                        .font(.plusJakarta(size: 10, weight: .bold))
                        .foregroundStyle(RoseSheetChrome.label)
                    Text(tenderLabel)
                        .font(.plusJakarta(size: 13, weight: .bold))
                        .foregroundStyle(RoseSheetChrome.tenderSelected)
                }
            }
        }
        .padding(16)
        .background(RoseSheetChrome.sheet)
        .overlay(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .strokeBorder(theme.accent, lineWidth: 1.5)
        )
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
    }

    // MARK: - Scan cart

    @ViewBuilder
    private var scanBody: some View {
        RoseFieldLabel(text: "Scan or search SKU")
        RoseScanField(
            theme: theme,
            placeholder: catalogue.isEmpty ? "Add SKU under Catalogue first" : "HM-044  ·  tap to scan",
            query: $skuQuery,
            actionLabel: "Scan",
            action: {
                let typed = skuQuery.trimmingCharacters(in: .whitespaces)
                if typed.isEmpty {
                    if !catalogue.isEmpty { showBarcodeScanner = true }
                } else {
                    addSku()
                }
            },
            disabled: catalogue.isEmpty
        )
        if catalogue.isEmpty {
            RoseEmptyCard(
                theme: theme,
                title: "Catalogue not set up",
                bodyText: "Add products under Catalogue. A sale line needs a real SKU to post itemized stock."
            )
        } else if !skuQuery.trimmingCharacters(in: .whitespaces).isEmpty {
            skuMatches
        }

        cartSection

        scanTotals

        RoseFieldLabel(text: "Discount")
        RoseTextField(theme: theme, placeholder: "0", text: $discount)
        RoseTenderChips(
            theme: theme,
            tender: $tender,
            splitTender: $splitTender,
            splitUpi: $splitUpi,
            splitCash: $splitCash,
            splitCard: $splitCard,
            splitOk: splitOk
        )

        RosePickerRow(
            theme: theme,
            label: "Customer",
            value: customer.isEmpty ? "Walk-in · B2C · MH intra-state" : customer
        )
        RosePickerRow(theme: theme, label: "Bill / print", value: billPrint) {
            billPrint = billPrint.hasPrefix("Thermal")
                ? "A4 invoice · email"
                : "Thermal · share UPI QR"
        }

        RoseMoreDetails(title: "More details", hint: "Notes · Loyalty · Hold bill") {
            RoseFieldLabel(text: "Customer")
            RoseTextField(theme: theme, placeholder: "Walk-in or credit buyer", text: $customer)
            RoseFieldLabel(text: "Sales channel")
            RoseChipRow(theme: theme, options: channels, selection: $salesMode)
            RoseFieldLabel(text: "Note")
            RoseTextField(theme: theme, placeholder: "Notes · Loyalty · Hold bill", text: $note)
            RoseFieldLabel(text: "Cess ₹ (optional · explicit only)")
            RoseTextField(theme: theme, placeholder: "0", text: $cessAmount)
            Text("Cess is operator-entered only — no HSN/CBIC schedule lookup.")
                .font(.plusJakarta(size: 10))
                .foregroundStyle(OperatingV12.subtitle.opacity(0.8))
            Button("Hold bill · park cart") {
                RetailParkedSaleStore.shared.parkCurrentCart(
                    momentId: model.momentId,
                    cart: cart,
                    channel: salesMode,
                    customer: customer,
                    note: note,
                    tender: tender,
                    currency: currency
                )
                posMode = .holdRecall
            }
            .font(.plusJakarta(size: 13, weight: .bold))
            .foregroundStyle(RoseSheetChrome.label)
            .disabled(cart.isEmpty)
        }

        RoseWhyBox(
            theme: theme,
            text: "POS posts one itemized sale + linked tender receipt atomically. Stock − on post when tracked. Hold is draft only — no revenue, no stock. Quick amount stays the original sheet."
        )

        RoseStickyFooter(
            theme: theme,
            title: footerTitle,
            caption: footerCaption,
            disabled: !postable,
            loading: saving,
            action: { Task { await post() } }
        )
    }

    private var scanTotals: some View {
        VStack(spacing: 6) {
            HStack {
                Text("Subtotal")
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(RoseSheetChrome.textBody)
                Spacer()
                Text(subtotalText)
                    .font(.plusJakarta(size: 12, weight: .medium))
                    .foregroundStyle(RoseSheetChrome.textBody)
            }
            if discountValue > 0 {
                HStack {
                    Text("Discount")
                        .font(.plusJakarta(size: 12))
                        .foregroundStyle(RoseSheetChrome.textBody)
                    Spacer()
                    Text(discountText)
                        .font(.plusJakarta(size: 12, weight: .medium))
                        .foregroundStyle(RoseSheetChrome.tenderSelected)
                }
            }
            if cessValue > 0 {
                HStack {
                    Text("Cess")
                        .font(.plusJakarta(size: 12))
                        .foregroundStyle(RoseSheetChrome.textBody)
                    Spacer()
                    Text(OperatingV12Money.amount(NSDecimalNumber(decimal: cessValue).stringValue, currency: currency))
                        .font(.plusJakarta(size: 12, weight: .medium))
                        .foregroundStyle(RoseSheetChrome.textBody)
                }
            }
            RoseGstEstimateRow(
                theme: theme,
                label: "CGST + SGST (incl. est.)",
                estimate: scanGstEstimate,
                currency: model.currency,
                captionWhenKnown: "Estimate from line rates — GST posts only on Charge",
                captionWhenMissing: "Unavailable — rate missing on line / master"
            )
            Rectangle()
                .fill(RoseSheetChrome.roseBorder)
                .frame(height: 1)
            HStack(alignment: .bottom) {
                VStack(alignment: .leading, spacing: 2) {
                    Text("Payable now")
                        .font(.plusJakarta(size: 11, weight: .medium))
                        .foregroundStyle(RoseSheetChrome.textBody)
                    Text(payableText)
                        .font(.plusJakarta(size: 28, weight: .bold))
                        .foregroundStyle(OperatingV12.title)
                }
                Spacer()
                VStack(alignment: .trailing, spacing: 2) {
                    Text("Tender")
                        .font(.plusJakarta(size: 10, weight: .bold))
                        .foregroundStyle(RoseSheetChrome.label)
                    Text(tenderLabel)
                        .font(.plusJakarta(size: 13, weight: .bold))
                        .foregroundStyle(RoseSheetChrome.tenderSelected)
                }
            }
        }
        .padding(16)
        .background(RoseSheetChrome.sheet)
        .overlay(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .strokeBorder(theme.accent, lineWidth: 1.5)
        )
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
    }

    // MARK: - Hold recall

    @ViewBuilder
    private var holdRecallBody: some View {
        RoseFieldLabel(text: "Find parked bill")
        RoseTextField(
            theme: theme,
            placeholder: "Name · table · bill #",
            text: $holdQuery,
            trailing: "⌕"
        )

        RoseSectionCard(
            theme: theme,
            label: "Parked on this register",
            trailing: parked.isEmpty ? "No holds · draft only" : "\(parked.count) holds · draft only"
        ) {
            if filteredParked.isEmpty {
                RoseEmptyCard(
                    theme: theme,
                    title: parked.isEmpty ? "No parked bills" : "No matches",
                    bodyText: parked.isEmpty
                        ? "Park from Scan cart → More details · Hold bill, or Utilities · Hold."
                        : "Try another name or bill #."
                )
            } else {
                VStack(spacing: 8) {
                    ForEach(filteredParked) { draft in
                        Button {
                            selectedHoldId = selectedHoldId == draft.id ? nil : draft.id
                        } label: {
                            HStack(alignment: .top, spacing: 8) {
                                VStack(alignment: .leading, spacing: 2) {
                                    Text(draft.parkDisplayId)
                                        .font(.plusJakarta(size: 10, weight: .bold))
                                        .foregroundStyle(RoseSheetChrome.label)
                                    Text(draft.label)
                                        .font(.plusJakarta(size: 14, weight: .bold))
                                        .foregroundStyle(OperatingV12.title)
                                        .lineLimit(1)
                                    Text(holdMeta(draft))
                                        .font(.plusJakarta(size: 11))
                                        .foregroundStyle(RoseSheetChrome.textBody)
                                        .lineLimit(1)
                                }
                                Spacer(minLength: 0)
                                VStack(alignment: .trailing, spacing: 2) {
                                    Text(OperatingV12Money.amount(draft.amount, currency: currency))
                                        .font(.plusJakarta(size: 14, weight: .bold))
                                        .foregroundStyle(OperatingV12.title)
                                    Text(draft.ageLabel)
                                        .font(.plusJakarta(size: 10, weight: .medium))
                                        .foregroundStyle(RoseSheetChrome.textBody)
                                }
                            }
                            .padding(12)
                            .background(RoseSheetChrome.sheet)
                            .overlay(
                                RoundedRectangle(cornerRadius: 12, style: .continuous)
                                    .strokeBorder(
                                        selectedHoldId == draft.id
                                            ? theme.accent
                                            : theme.accent.opacity(0.30),
                                        lineWidth: 1
                                    )
                            )
                            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                        }
                        .buttonStyle(.plain)
                    }
                }
                Text("Empty holds auto-expire end of shift")
                    .font(.plusJakarta(size: 11))
                    .foregroundStyle(RoseSheetChrome.textBody)
            }
        }

        VStack(alignment: .leading, spacing: 4) {
            HStack {
                Text("Selected")
                    .font(.plusJakarta(size: 11, weight: .medium))
                    .foregroundStyle(RoseSheetChrome.textBody)
                Spacer()
                Text(selectedHold?.parkDisplayId ?? "—")
                    .font(.plusJakarta(size: 11, weight: .bold))
                    .foregroundStyle(RoseSheetChrome.label)
            }
            Text(selectedHold.map { OperatingV12Money.amount($0.amount, currency: currency) } ?? "—")
                .font(.plusJakarta(size: 28, weight: .bold))
                .foregroundStyle(OperatingV12.title)
            Text("Tap Recall to refill Scan cart — still draft until Charge")
                .font(.plusJakarta(size: 11))
                .foregroundStyle(RoseSheetChrome.textBody)
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(RoseSheetChrome.sheet)
        .overlay(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .strokeBorder(theme.accent, lineWidth: 1.5)
        )
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))

        RoseMoreDetails(title: "More details", hint: "Void hold · Merge · Transfer register") {
            Text("Void / merge / transfer register — chrome only. Session parks are not server-backed yet.")
                .font(.plusJakarta(size: 11))
                .foregroundStyle(RoseSheetChrome.textBody)
            if let hold = selectedHold {
                Button("Void \(hold.parkDisplayId)") {
                    RetailParkedSaleStore.shared.remove(id: hold.id, momentId: model.momentId)
                    selectedHoldId = nil
                }
                .font(.plusJakarta(size: 13, weight: .bold))
                .foregroundStyle(theme.warning)
            }
            RoseFieldLabel(text: "Note")
            RoseTextField(theme: theme, placeholder: "Recall note", text: $note)
        }

        RoseWhyBox(
            theme: theme,
            text: "Hold is a device-local draft cart — no revenue, no stock, no tender. Survives app restart on this phone until recall/charge. Not shared across devices until a register API exists. Recall opens Scan cart; Charge clears the hold."
        )

        RoseStickyFooter(
            theme: theme,
            title: footerTitle,
            caption: footerCaption,
            disabled: !recallReady,
            loading: false,
            action: { recallSelectedToScanCart() }
        )
    }

    // MARK: - Shared cart / SKU

    @ViewBuilder
    private var skuMatches: some View {
        let matches = catalogue
            .filter {
                $0.name.localizedCaseInsensitiveContains(skuQuery.trimmingCharacters(in: .whitespaces))
                    || ($0.code ?? "").localizedCaseInsensitiveContains(skuQuery)
                    || ($0.barcode ?? "").localizedCaseInsensitiveContains(skuQuery)
            }
            .prefix(4)
        if !matches.isEmpty {
            VStack(spacing: 6) {
                ForEach(Array(matches)) { sku in
                    Button {
                        add(sku)
                        skuQuery = ""
                    } label: {
                        HStack {
                            VStack(alignment: .leading, spacing: 2) {
                                if let code = sku.code, !code.isEmpty {
                                    Text(code)
                                        .font(.plusJakarta(size: 10, weight: .bold))
                                        .foregroundStyle(theme.accent)
                                }
                                Text(sku.name)
                                    .font(.plusJakarta(size: 13, weight: .semibold))
                                    .foregroundStyle(OperatingV12.title)
                                    .lineLimit(1)
                            }
                            Spacer()
                            if let price = sku.sellPrice, !price.isEmpty {
                                Text(OperatingV12Money.amount(price, currency: currency))
                                    .font(.plusJakarta(size: 10, weight: .bold))
                                    .foregroundStyle(theme.accent)
                            }
                        }
                        .padding(.horizontal, 12)
                        .padding(.vertical, 9)
                        .background(theme.inset)
                        .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }

    @ViewBuilder
    private var cartSection: some View {
        RoseLinesCard(
            theme: theme,
            title: "Cart",
            trailing: cart.isEmpty ? "No items" : "\(cart.count) lines · \(cartUnits) units"
        ) {
            if cart.isEmpty {
                Text("Add SKU lines above — each needs a price before posting.")
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(OperatingV12.subtitle)
            } else {
                VStack(spacing: 0) {
                    ForEach(Array(cart.enumerated()), id: \.element.id) { index, line in
                        denseCartRow(index: index, line: line)
                        if index < cart.count - 1 {
                            Divider().overlay(theme.border.opacity(0.4))
                        }
                    }
                }
            }

            HStack {
                Button("+ Add line") { showSkuPicker.toggle() }
                    .font(.plusJakarta(size: 13, weight: .bold))
                    .foregroundStyle(RoseSheetChrome.label)
                Spacer()
                Button("Recent · Favourites") { showSkuPicker.toggle() }
                    .font(.plusJakarta(size: 11))
                    .foregroundStyle(RoseSheetChrome.textBody)
            }
            .padding(.top, 4)

            if showSkuPicker {
                let picks = catalogue.filter { sku in !cart.contains(where: { $0.masterId == sku.id }) }.prefix(6)
                if picks.isEmpty {
                    Text("No more catalogue SKUs to add.")
                        .font(.plusJakarta(size: 11))
                        .foregroundStyle(OperatingV12.subtitle)
                } else {
                    VStack(spacing: 6) {
                        ForEach(Array(picks)) { sku in
                            RosePickerRow(
                                theme: theme,
                                label: sku.code?.nilIfBlank ?? "SKU",
                                value: [
                                    sku.name,
                                    sku.sellPrice.flatMap { $0.isEmpty ? nil : OperatingV12Money.amount($0, currency: currency) },
                                ].compactMap { $0 }.joined(separator: " · ")
                            ) {
                                add(sku)
                                showSkuPicker = false
                            }
                        }
                    }
                }
            }
        }
        if !cart.isEmpty && !cart.allSatisfy({ ($0.unitPrice as NSString).doubleValue > 0 }) {
            Text("Every line needs a price above zero before the sale can post.")
                .font(.plusJakarta(size: 10))
                .foregroundStyle(theme.warning)
        }
    }

    private func denseCartRow(index: Int, line: RetailPosLine) -> some View {
        HStack(alignment: .center, spacing: 8) {
            VStack(alignment: .leading, spacing: 2) {
                if let code = line.skuCode, !code.isEmpty {
                    Text(code)
                        .font(.plusJakarta(size: 10, weight: .bold))
                        .foregroundStyle(RoseSheetChrome.label)
                }
                Text(line.name)
                    .font(.plusJakarta(size: 13, weight: .semibold))
                    .foregroundStyle(OperatingV12.title)
                    .lineLimit(1)
                Text(lineMeta(line))
                    .font(.plusJakarta(size: 11))
                    .foregroundStyle(OperatingV12.subtitle)
                    .lineLimit(1)
            }
            Spacer(minLength: 0)
            RoseStepper(
                theme: theme,
                quantity: line.quantity,
                onDecrement: { decrement(at: index) },
                onIncrement: { increment(at: index) }
            )
            Text(lineAmount(line))
                .font(.plusJakarta(size: 13, weight: .bold))
                .foregroundStyle(OperatingV12.title)
        }
        .padding(.vertical, 10)
        .contextMenu {
            Button("Remove", role: .destructive) { cart.remove(at: index) }
        }
    }

    private func lineMeta(_ line: RetailPosLine) -> String {
        var parts: [String] = []
        if line.unitPrice.isEmpty {
            parts.append("Price needed")
        } else {
            let unit = line.unit ?? "pc"
            parts.append("\(OperatingV12Money.amount(line.unitPrice, currency: currency)) / \(unit)")
        }
        if let hsn = line.hsn, !hsn.isEmpty {
            parts.append("HSN \(hsn)")
        }
        return parts.joined(separator: " · ")
    }

    private func holdMeta(_ draft: RetailParkedSale) -> String {
        let tenderBit = draft.tender.isEmpty ? "pending" : "\(draft.tender.lowercased()) pending"
        if draft.meta.trimmingCharacters(in: .whitespaces).isEmpty == false,
           draft.meta != " · " {
            return draft.meta
        }
        return [
            "\(draft.lines.count) line\(draft.lines.count == 1 ? "" : "s")",
            tenderBit,
        ].joined(separator: " · ")
    }

    private func cycleChannel() {
        guard !channels.isEmpty else { return }
        let idx = channels.firstIndex(of: salesMode) ?? -1
        salesMode = channels[(idx + 1) % channels.count]
    }

    private func addSku() {
        let typed = skuQuery.trimmingCharacters(in: .whitespaces)
        guard !catalogue.isEmpty, !typed.isEmpty else { return }
        let match = catalogue.first { ($0.code ?? "").caseInsensitiveCompare(typed) == .orderedSame }
            ?? catalogue.first { ($0.barcode ?? "").caseInsensitiveCompare(typed) == .orderedSame }
            ?? catalogue.first { $0.name.localizedCaseInsensitiveContains(typed) }
        if let match {
            add(match)
            skuQuery = ""
        }
    }

    private func applyPosScan(_ raw: String) {
        let typed = raw.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !typed.isEmpty else { return }
        let match = catalogue.first { ($0.code ?? "").caseInsensitiveCompare(typed) == .orderedSame }
            ?? catalogue.first { ($0.barcode ?? "").caseInsensitiveCompare(typed) == .orderedSame }
            ?? catalogue.first { $0.name.localizedCaseInsensitiveContains(typed) }
        if let match {
            add(match)
            skuQuery = ""
        } else {
            skuQuery = typed
        }
    }

    private func add(_ sku: OperatingMasterPayload) {
        if let index = cart.firstIndex(where: { $0.masterId == sku.id }) {
            guard cart[index].quantity < 99 else { return }
            cart[index].quantity += 1
            return
        }
        guard cart.count < 24 else { return }
        cart.append(RetailPosLine.of(sku))
    }

    private func recallSelectedToScanCart() {
        guard let draft = selectedHold else { return }
        recalledDraftId = draft.id
        cart = draft.lines
        salesMode = draft.channel.isEmpty ? salesMode : draft.channel
        if customer.isEmpty { customer = draft.customer }
        if note.isEmpty { note = draft.note }
        tender = draft.tender.isEmpty ? tender : draft.tender
        selectedHoldId = draft.id
        posMode = .scanCart
    }

    private func decrement(at index: Int) {
        guard cart.indices.contains(index) else { return }
        if cart[index].quantity <= 1 {
            cart.remove(at: index)
        } else {
            cart[index].quantity -= 1
        }
    }

    private func increment(at index: Int) {
        guard cart.indices.contains(index), cart[index].quantity < 99 else { return }
        cart[index].quantity += 1
    }

    private func lineAmount(_ line: RetailPosLine) -> String {
        OperatingV12Money.amount(NSDecimalNumber(decimal: line.amount).stringValue, currency: currency)
    }

    private func post() async {
        guard postable else { return }
        saving = true
        defer { saving = false }

        let guest = customer.trimmingCharacters(in: .whitespaces)
        let gstOn = (model.projection?.gstTracking ?? "").uppercased() == "YES"
            && (model.projection?.gstRegistration ?? "").uppercased() == "REGULAR"
        let taxMode: String = {
            guard gstOn else { return "NOT_CONFIGURED" }
            switch (model.projection?.salePriceIncludesGst ?? "").uppercased() {
            case "YES": return "INCLUSIVE"
            case "NO": return "EXCLUSIVE"
            default: return "INCLUSIVE"
            }
        }()
        var contract = OperatingContractInput()
        contract.taxMode = taxMode
        if gstOn {
            contract.placeOfSupply = model.projection?.gstState?.nilIfBlank
        }
        let payablePlain = PosSettlement.money(payable)
        contract.paymentState = "RECEIVED"
        contract.receivedAmount = payablePlain
        if discountValue > 0 { contract.discount = PosSettlement.money(discountValue) }
        if cessValue > 0 { contract.cessAmount = PosSettlement.money(cessValue) }
        contract.payments = PosSettlement.buildPayments(
            split: splitTender,
            primary: tender,
            payable: payable,
            splitAmounts: splitAmounts
        )

        let input: OperatingEventInput
        switch posMode {
        case .quickAmount:
            let amount = quickAmount.trimmingCharacters(in: .whitespaces)
            contract.entryMode = "QUICK"
            if gstOn, let rate = quickGstRateHint() {
                contract.gstRate = rate
            }
            input = OperatingEventInput(
                momentId: model.momentId,
                eventType: "SALE",
                title: guest.isEmpty ? "Quick sale" : "Quick sale · \(guest)",
                amount: amount,
                note: noteLine(),
                channelName: salesMode,
                counterpartyName: guest.isEmpty ? nil : guest,
                contract: contract,
                paymentStatus: "COLLECTED"
            )
        case .scanCart:
            contract.entryMode = "ITEMIZED"
            let title: String = {
                if !guest.isEmpty { return "Sale · \(guest)" }
                return "Sale · \(cart.count) item\(cart.count == 1 ? "" : "s")"
            }()
            input = OperatingEventInput(
                momentId: model.momentId,
                eventType: "SALE",
                title: title,
                amount: NSDecimalNumber(decimal: gross).stringValue,
                note: noteLine(),
                channelName: salesMode,
                counterpartyName: guest.isEmpty ? nil : guest,
                lineItems: cart.map {
                    .init(
                        itemName: $0.name,
                        quantity: "\($0.quantity)",
                        unit: $0.unit,
                        unitPrice: $0.unitPrice,
                        masterId: productionMasterId($0.masterId),
                        taxMode: taxMode == "NOT_CONFIGURED" ? nil : taxMode,
                        gstRate: $0.gstRate.flatMap { rate in
                            let trimmed = rate.trimmingCharacters(in: .whitespaces)
                            if trimmed.isEmpty || trimmed == "0" || trimmed.caseInsensitiveCompare("Exempt") == .orderedSame {
                                return nil
                            }
                            return trimmed
                        }
                    )
                },
                contract: contract,
                paymentStatus: "COLLECTED"
            )
        case .holdRecall:
            return
        }

        let wantsThermal = billPrint.hasPrefix("Thermal")
        let receiptSnapshot: RetailReceiptModel? = {
            guard wantsThermal, posMode == .scanCart else { return nil }
            let title: String = guest.isEmpty
                ? "Sale · \(cart.count) item\(cart.count == 1 ? "" : "s")"
                : "Sale · \(guest)"
            return RetailReceiptBuilder.fromPosCart(
                model: model,
                title: title,
                lines: cart,
                totalPlain: payablePlain,
                tender: tenderLabel,
                customer: guest.isEmpty ? nil : guest,
                quickAmountOnly: false,
                gstEstimatePlain: scanGstEstimate.map { NSDecimalNumber(decimal: $0).stringValue }
            )
        }()

        let ok = await model.record(input)
        if ok {
            if let id = recalledDraftId {
                RetailParkedSaleStore.shared.remove(id: id, momentId: model.momentId)
            }
            if let receiptSnapshot {
                postedReceipt = receiptSnapshot
            } else {
                dismiss()
            }
        }
    }

    private func noteLine() -> String? {
        let trimmed = note.trimmingCharacters(in: .whitespaces)
        return trimmed.isEmpty ? nil : trimmed
    }
}

// MARK: - Parked POS drafts (device-local durable)

struct RetailParkedSale: Identifiable, Equatable, Codable {
    let id: String
    /// Device-local slip id (`HOLD-22`) — not a fiscal bill number.
    let holdCode: String
    let label: String
    let meta: String
    let amount: String
    let channel: String
    let customer: String
    let note: String
    let tender: String
    let lines: [RetailPosLine]
    let parkedAt: Date

    var parkDisplayId: String { holdCode }

    var ageLabel: String {
        let mins = max(0, Int(Date().timeIntervalSince(parkedAt) / 60))
        if mins < 1 { return "just now" }
        if mins < 60 { return "\(mins)m ago" }
        return "\(mins / 60)h ago"
    }
}

/// Device-local park store (UserDefaults) — no register / holds API (`RETAIL_MISSING_APIS`).
/// Survives process death; scoped by moment. Not shared across devices.
final class RetailParkedSaleStore {
    static let shared = RetailParkedSaleStore()
    private var byMoment: [String: [RetailParkedSale]] = [:]
    private var nextHoldNumber = 1
    private let defaults = UserDefaults.standard
    private let holdsKey = "retail.parked.holds.v1"
    private let seqKey = "retail.parked.hold_seq.v1"

    private init() {
        load()
    }

    func nextHoldCode() -> String {
        let code = "HOLD-\(nextHoldNumber)"
        nextHoldNumber += 1
        defaults.set(nextHoldNumber, forKey: seqKey)
        return code
    }

    func list(momentId: String) -> [RetailParkedSale] {
        (byMoment[momentId] ?? []).sorted { $0.parkedAt > $1.parkedAt }
    }

    func park(momentId: String, draft: RetailParkedSale) {
        var list = byMoment[momentId] ?? []
        list.insert(draft, at: 0)
        byMoment[momentId] = Array(list.prefix(8))
        persist()
    }

    func remove(id: String, momentId: String) {
        byMoment[momentId] = (byMoment[momentId] ?? []).filter { $0.id != id }
        persist()
    }

    private func load() {
        nextHoldNumber = max(1, defaults.integer(forKey: seqKey))
        guard let data = defaults.data(forKey: holdsKey),
              let decoded = try? JSONDecoder().decode([String: [RetailParkedSale]].self, from: data)
        else { return }
        byMoment = decoded
        let maxCode = decoded.values.flatMap { $0 }
            .compactMap { Int($0.holdCode.replacingOccurrences(of: "HOLD-", with: "")) }
            .max() ?? 0
        if maxCode >= nextHoldNumber { nextHoldNumber = maxCode + 1 }
    }

    private func persist() {
        guard let data = try? JSONEncoder().encode(byMoment) else { return }
        defaults.set(data, forKey: holdsKey)
        defaults.set(nextHoldNumber, forKey: seqKey)
    }
}

extension RetailParkedSaleStore {
    func parkCurrentCart(
        momentId: String,
        cart: [RetailPosLine],
        channel: String,
        customer: String,
        note: String,
        tender: String,
        currency: String
    ) {
        guard !cart.isEmpty else { return }
        let total = cart.reduce(Decimal.zero) { $0 + $1.amount }
        let amount = NSDecimalNumber(decimal: total).stringValue
        let label = customer.isEmpty ? "Parked bill · \(cart.count) lines" : "Parked · \(customer)"
        let meta = [channel, "Tender \(tender)"].joined(separator: " · ")
        let draft = RetailParkedSale(
            id: UUID().uuidString,
            holdCode: nextHoldCode(),
            label: label,
            meta: meta,
            amount: amount,
            channel: channel,
            customer: customer,
            note: note,
            tender: tender,
            lines: cart,
            parkedAt: Date()
        )
        park(momentId: momentId, draft: draft)
    }
}

struct RetailPosLine: Identifiable, Equatable, Codable {
    let id: String
    let masterId: String?
    let name: String
    let unit: String?
    var quantity: Int
    var unitPrice: String
    var skuCode: String? = nil
    var hsn: String? = nil
    /// Master GST % for client estimate rows — never posted as fabricated tax.
    var gstRate: String? = nil

    var amount: Decimal {
        let price = Decimal(string: unitPrice) ?? 0
        return price * Decimal(quantity)
    }

    static func of(_ sku: OperatingMasterPayload) -> RetailPosLine {
        RetailPosLine(
            id: sku.id,
            masterId: sku.id,
            name: sku.name,
            unit: sku.unit ?? "pc",
            quantity: 1,
            unitPrice: sku.sellPrice ?? "",
            skuCode: sku.code?.nilIfBlank ?? sku.barcode?.nilIfBlank,
            hsn: sku.hsn?.nilIfBlank,
            gstRate: sku.gstRate?.nilIfBlank
        )
    }
}

/// Harbor fixture ids (`fx-*`) must never ride a production Charge.
private func productionMasterId(_ masterId: String?) -> String? {
    guard let id = masterId?.trimmingCharacters(in: .whitespaces), !id.isEmpty else { return nil }
    if id.lowercased().hasPrefix("fx-") { return nil }
    return id
}

private extension RetailPosSheet {
    func quickGstRateHint() -> String? {
        guard gstPicker.hasPrefix("Inclusive") || gstPicker.hasPrefix("Exclusive") else { return nil }
        let match = gstPicker.split(separator: " ").compactMap { token -> String? in
            let cleaned = token.replacingOccurrences(of: "%", with: "")
            return Decimal(string: cleaned) != nil ? cleaned : nil
        }.first
        return match ?? "5"
    }
}

// MARK: - Client GST estimate (UI only)

enum RetailGstEstimate {
    /// Returns estimate when every positive line has a rate (or Exempt/0); else nil → Unavailable.
    /// Matches APK HALF_UP: per-line scale-4, then total scale-2 (`.plain` == round half away from 0).
    static func from(amounts: [Decimal], rates: [String?], inclusive: Bool = true) -> Decimal? {
        guard !amounts.isEmpty else { return nil }
        var tax = Decimal.zero
        var sawPositive = false
        for i in amounts.indices {
            let amount = amounts[i]
            guard amount > 0 else { continue }
            sawPositive = true
            let raw = (rates.indices.contains(i) ? rates[i] : nil)?.trimmingCharacters(in: .whitespaces) ?? ""
            if raw.isEmpty { return nil }
            if raw.compare("Exempt", options: .caseInsensitive) == .orderedSame || raw == "0" { continue }
            let cleaned = raw.replacingOccurrences(of: "%", with: "")
            guard let rate = Decimal(string: cleaned), rate >= 0 else { return nil }
            var piece: Decimal
            if inclusive {
                let denom = Decimal(100) + rate
                guard denom != 0 else { return nil }
                piece = amount * rate / denom
            } else {
                piece = amount * rate / Decimal(100)
            }
            var pieceIn = piece
            var pieceOut = Decimal()
            NSDecimalRound(&pieceOut, &pieceIn, 4, .plain)
            tax += pieceOut
        }
        guard sawPositive else { return nil }
        var rounded = tax
        var result = Decimal()
        NSDecimalRound(&result, &rounded, 2, .plain)
        return result
    }

    static func onTotal(_ amount: Decimal?, ratePercent: String?, inclusive: Bool = true) -> Decimal? {
        guard let amount, amount > 0 else { return nil }
        return from(amounts: [amount], rates: [ratePercent], inclusive: inclusive)
    }
}

private extension String {
    var nilIfBlank: String? {
        let t = trimmingCharacters(in: .whitespacesAndNewlines)
        return t.isEmpty ? nil : t
    }
}

import SwiftUI

/// Retail POS — Figma RTL-QA-01 ALT: Scan cart · Hold recall · Quick amount.
/// Posts `SALE` via `BusinessOperatingModel.record`. Quick amount has no lines / no stock movement.
/// Hold recall restores a session park into Scan cart — Charge still posts from Scan cart.
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
    @State private var gstPicker = "Inclusive · selection only"
    @State private var billPrint = "Thermal · share UPI QR"

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

    private var subtotal: Decimal {
        cart.reduce(Decimal.zero) { $0 + $1.amount }
    }

    private var subtotalText: String {
        guard !cart.isEmpty else { return "—" }
        return OperatingV12Money.amount(NSDecimalNumber(decimal: subtotal).stringValue, currency: currency)
    }

    private var payableText: String {
        switch posMode {
        case .quickAmount:
            let amount = quickAmount.trimmingCharacters(in: .whitespaces)
            return amount.isEmpty ? "—" : OperatingV12Money.amount(amount, currency: currency)
        case .scanCart, .holdRecall:
            return cart.isEmpty ? "—" : subtotalText
        }
    }

    private var postable: Bool {
        switch posMode {
        case .quickAmount:
            return (Decimal(string: quickAmount) ?? 0) > 0
        case .scanCart:
            return !cart.isEmpty && cart.allSatisfy { ($0.unitPrice as NSString).doubleValue > 0 }
        case .holdRecall:
            return false
        }
    }

    private var recallReady: Bool {
        posMode == .holdRecall && selectedHold != nil
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                RoseGrabber()
                RoseSheetHeader(
                    theme: theme,
                    title: "POS Sale",
                    subtitle: headerSubtitle,
                    qaCode: "RTL-QA-01",
                    onClose: { dismiss() }
                )
                RoseContextLine(
                    text: "\(model.title) · Sale ≠ stock unless itemized · Quick amount never moves SKU"
                )

                RoseFieldLabel(text: "Mode")
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
            .padding(16)
            .padding(.bottom, 24)
        }
        .background(theme.canvas)
        .task {
            if salesMode.isEmpty || !channels.contains(salesMode) {
                salesMode = channels.first ?? "Counter"
            }
            if model.masters.isEmpty {
                await model.loadMasters()
            }
        }
    }

    private var headerSubtitle: String {
        switch posMode {
        case .quickAmount: return "Amount only · no stock"
        case .scanCart:
            if cart.isEmpty { return "Scan cart" }
            return "\(cart.count) lines · \(cartUnits) unit\(cartUnits == 1 ? "" : "s")"
        case .holdRecall:
            return "\(parked.count) hold\(parked.count == 1 ? "" : "s") · draft only"
        }
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

    // MARK: - Quick amount

    @ViewBuilder
    private var quickBody: some View {
        RoseAmountHero(
            theme: theme,
            label: "Sale amount · \(currency)",
            amountText: payableText,
            caption: "No SKU lines · revenue only · stock unchanged"
        )

        RoseSectionCard(theme: theme, label: "Keypad") {
            Text("tap to enter")
                .font(.plusJakarta(size: 11))
                .foregroundStyle(OperatingV12.subtitle)
            RoseNumericKeypad(theme: theme, value: $quickAmount)
        }

        quickTotals

        RoseTenderChips(theme: theme, tender: $tender, options: ["UPI", "Cash", "Card"])

        RosePickerRow(
            theme: theme,
            label: "Customer",
            value: customer.isEmpty ? "Walk-in (cash/UPI)" : customer
        )
        RosePickerRow(theme: theme, label: "Channel", value: salesMode.isEmpty ? "In-store" : salesMode) {
            cycleChannel()
        }
        RosePickerRow(theme: theme, label: "GST", value: gstPicker) {
            gstPicker = gstPicker.hasPrefix("Inclusive")
                ? "Exclusive · selection only"
                : "Inclusive · selection only"
        }

        RoseMoreDetails(title: "More details") {
            RoseFieldLabel(text: "Customer")
            RoseTextField(theme: theme, placeholder: "Walk-in or named credit buyer", text: $customer)
            RoseFieldLabel(text: "Note")
            RoseTextField(theme: theme, placeholder: "Notes · Attachment · Tags", text: $note)
        }

        RoseWhyBox(
            theme: theme,
            text: "Quick amount posts revenue + linked tender with zero SKU movement. Same rule as original QA-01 — kept inside POS so the counter never leaves this sheet. Credit still needs a named customer."
        )

        RoseStickyFooter(
            theme: theme,
            title: footerTitle,
            disabled: !postable,
            loading: saving,
            action: { Task { await post() } }
        )
    }

    private var quickTotals: some View {
        VStack(spacing: 8) {
            RoseUnavailableGstRow(
                theme: theme,
                label: "GST (inclusive)",
                caption: "Unavailable — no client tax engine"
            )
            HStack {
                Text("Discount")
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(OperatingV12.subtitle)
                Spacer()
                Text("− ₹0")
                    .font(.plusJakarta(size: 14, weight: .semibold))
                    .foregroundStyle(OperatingV12.title)
            }
            Divider().overlay(theme.border.opacity(0.4))
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text("Payable now")
                        .font(.plusJakarta(size: 10))
                        .foregroundStyle(OperatingV12.subtitle)
                    Text(payableText)
                        .font(.plusJakarta(size: 24, weight: .bold))
                        .foregroundStyle(OperatingV12.title)
                }
                Spacer()
                VStack(alignment: .trailing, spacing: 2) {
                    Text("Tender")
                        .font(.plusJakarta(size: 10))
                        .foregroundStyle(OperatingV12.subtitle)
                    Text(tender)
                        .font(.plusJakarta(size: 14, weight: .bold))
                        .foregroundStyle(Color(red: 0.13, green: 0.77, blue: 0.37))
                }
            }
        }
        .padding(12)
        .background(Color.black.opacity(0.24))
        .overlay(
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .strokeBorder(theme.accent.opacity(0.35), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
    }

    // MARK: - Scan cart

    @ViewBuilder
    private var scanBody: some View {
        RoseFieldLabel(text: "Scan or search SKU")
        RoseScanField(
            theme: theme,
            placeholder: catalogue.isEmpty ? "Add SKU under Catalogue first" : "Barcode or product name",
            query: $skuQuery,
            actionLabel: "Scan",
            action: addSku,
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

        RoseTenderChips(theme: theme, tender: $tender, options: ["UPI", "Cash", "Card"])

        RosePickerRow(
            theme: theme,
            label: "Customer",
            value: customer.isEmpty ? "Walk-in · B2C" : customer
        )
        RosePickerRow(theme: theme, label: "Bill / print", value: billPrint) {
            billPrint = billPrint.hasPrefix("Thermal")
                ? "A4 invoice · email"
                : "Thermal · share UPI QR"
        }

        RoseMoreDetails(title: "More details") {
            RoseFieldLabel(text: "Customer")
            RoseTextField(theme: theme, placeholder: "Walk-in or credit buyer", text: $customer)
            RoseFieldLabel(text: "Sales channel")
            RoseChipRow(theme: theme, options: channels, selection: $salesMode)
            RoseFieldLabel(text: "Note")
            RoseTextField(theme: theme, placeholder: "Notes · Loyalty · Hold bill", text: $note)
        }

        RoseWhyBox(
            theme: theme,
            text: "POS posts one itemized sale + linked tender receipt atomically. Stock − on post when tracked. Hold is draft only — no revenue, no stock. Quick amount stays amount-only."
        )

        RoseStickyFooter(
            theme: theme,
            title: footerTitle,
            disabled: !postable,
            loading: saving,
            action: { Task { await post() } }
        )
    }

    private var scanTotals: some View {
        VStack(spacing: 8) {
            HStack {
                Text("Subtotal")
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(OperatingV12.subtitle)
                Spacer()
                Text(payableText)
                    .font(.plusJakarta(size: 14, weight: .semibold))
                    .foregroundStyle(OperatingV12.title)
            }
            HStack {
                Text("Discount")
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(OperatingV12.subtitle)
                Spacer()
                Text("− ₹0")
                    .font(.plusJakarta(size: 14, weight: .semibold))
                    .foregroundStyle(OperatingV12.title)
            }
            RoseUnavailableGstRow(
                theme: theme,
                label: "CGST + SGST (incl.)",
                caption: "Unavailable — stamped server-side on bill"
            )
            Divider().overlay(theme.border.opacity(0.4))
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text("Payable now")
                        .font(.plusJakarta(size: 10))
                        .foregroundStyle(OperatingV12.subtitle)
                    Text(payableText)
                        .font(.plusJakarta(size: 24, weight: .bold))
                        .foregroundStyle(OperatingV12.title)
                }
                Spacer()
                VStack(alignment: .trailing, spacing: 2) {
                    Text("Tender")
                        .font(.plusJakarta(size: 10))
                        .foregroundStyle(OperatingV12.subtitle)
                    Text(tender)
                        .font(.plusJakarta(size: 14, weight: .bold))
                        .foregroundStyle(Color(red: 0.13, green: 0.77, blue: 0.37))
                }
            }
        }
        .padding(12)
        .background(Color.black.opacity(0.24))
        .overlay(
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .strokeBorder(theme.accent.opacity(0.35), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
    }

    // MARK: - Hold recall

    @ViewBuilder
    private var holdRecallBody: some View {
        RoseFieldLabel(text: "Find parked bill")
        RoseTextField(theme: theme, placeholder: "Name · table · bill #", text: $holdQuery)

        RoseSectionCard(theme: theme, label: "Parked on this register") {
            Text(parked.isEmpty ? "No holds · draft only" : "\(parked.count) holds · draft only")
                .font(.plusJakarta(size: 11))
                .foregroundStyle(OperatingV12.subtitle)

            if filteredParked.isEmpty {
                RoseEmptyCard(
                    theme: theme,
                    title: parked.isEmpty ? "No parked bills" : "No matches",
                    bodyText: parked.isEmpty
                        ? "Park a bill from Utilities → Hold, then recall here."
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
                                        .foregroundStyle(theme.accent)
                                    Text(draft.label)
                                        .font(.plusJakarta(size: 14, weight: .bold))
                                        .foregroundStyle(OperatingV12.title)
                                        .lineLimit(1)
                                    Text(holdMeta(draft))
                                        .font(.plusJakarta(size: 11))
                                        .foregroundStyle(OperatingV12.subtitle)
                                        .lineLimit(1)
                                }
                                Spacer(minLength: 0)
                                VStack(alignment: .trailing, spacing: 2) {
                                    Text(OperatingV12Money.amount(draft.amount, currency: currency))
                                        .font(.plusJakarta(size: 14, weight: .bold))
                                        .foregroundStyle(OperatingV12.title)
                                    Text(draft.ageLabel)
                                        .font(.plusJakarta(size: 10))
                                        .foregroundStyle(OperatingV12.subtitle)
                                }
                            }
                            .padding(12)
                            .background(selectedHoldId == draft.id ? theme.accent.opacity(0.16) : theme.inset)
                            .overlay(
                                RoundedRectangle(cornerRadius: 12, style: .continuous)
                                    .strokeBorder(
                                        selectedHoldId == draft.id ? theme.accent : theme.border.opacity(0.4),
                                        lineWidth: 1
                                    )
                            )
                            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                        }
                        .buttonStyle(.plain)
                    }
                }
                Text("Empty holds auto-expire end of shift · session parks only")
                    .font(.plusJakarta(size: 11))
                    .foregroundStyle(OperatingV12.subtitle)
            }
        }

        VStack(alignment: .leading, spacing: 4) {
            HStack {
                Text("Selected")
                    .font(.plusJakarta(size: 11))
                    .foregroundStyle(OperatingV12.subtitle)
                Spacer()
                Text(selectedHold?.parkDisplayId ?? "—")
                    .font(.plusJakarta(size: 11, weight: .bold))
                    .foregroundStyle(theme.accent)
            }
            Text(selectedHold.map { OperatingV12Money.amount($0.amount, currency: currency) } ?? "—")
                .font(.plusJakarta(size: 28, weight: .bold))
                .foregroundStyle(OperatingV12.title)
            Text("Tap Recall to refill Scan cart — still draft until Charge")
                .font(.plusJakarta(size: 11))
                .foregroundStyle(OperatingV12.subtitle)
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.black.opacity(0.24))
        .overlay(
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .strokeBorder(theme.accent.opacity(0.35), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))

        RoseMoreDetails(title: "More details") {
            Text("Void hold · Merge · Transfer register — chrome only on this sheet.")
                .font(.plusJakarta(size: 11))
                .foregroundStyle(OperatingV12.subtitle)
            RoseFieldLabel(text: "Note")
            RoseTextField(theme: theme, placeholder: "Recall note", text: $note)
        }

        RoseWhyBox(
            theme: theme,
            text: "Hold is draft cart JSON only — no revenue, no stock, no tender. Recall opens Scan cart with lines restored. Void discards draft."
        )

        RoseStickyFooter(
            theme: theme,
            title: footerTitle,
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
                    .foregroundStyle(theme.accent)
                Spacer()
                Button("Recent · Favourites") { showSkuPicker.toggle() }
                    .font(.plusJakarta(size: 11))
                    .foregroundStyle(OperatingV12.subtitle)
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
                        .foregroundStyle(theme.accent)
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
        [
            "\(draft.lines.count) line\(draft.lines.count == 1 ? "" : "s")",
            model.title,
            draft.channel.isEmpty ? "Session" : draft.channel,
        ].joined(separator: " · ")
    }

    private func cycleChannel() {
        guard !channels.isEmpty else { return }
        let idx = channels.firstIndex(of: salesMode) ?? -1
        salesMode = channels[(idx + 1) % channels.count]
    }

    private func addSku() {
        let typed = skuQuery.trimmingCharacters(in: .whitespaces)
        guard !catalogue.isEmpty else { return }
        let match = catalogue.first { ($0.code ?? "").caseInsensitiveCompare(typed) == .orderedSame }
            ?? catalogue.first { ($0.barcode ?? "").caseInsensitiveCompare(typed) == .orderedSame }
            ?? catalogue.first { typed.isEmpty == false && $0.name.localizedCaseInsensitiveContains(typed) }
            ?? catalogue.first
        if let match { add(match) }
        skuQuery = ""
    }

    private func add(_ sku: OperatingMasterPayload) {
        guard cart.count < 24, !cart.contains(where: { $0.masterId == sku.id }) else { return }
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
        var contract = OperatingContractInput()
        contract.taxMode = "NOT_CONFIGURED"

        let input: OperatingEventInput
        switch posMode {
        case .quickAmount:
            let amount = quickAmount.trimmingCharacters(in: .whitespaces)
            contract.entryMode = "QUICK"
            contract.paymentState = "RECEIVED"
            contract.receivedAmount = amount
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
            contract.paymentState = "RECEIVED"
            contract.receivedAmount = NSDecimalNumber(decimal: subtotal).stringValue
            let title: String = {
                if !guest.isEmpty { return "Sale · \(guest)" }
                return "Sale · \(cart.count) item\(cart.count == 1 ? "" : "s")"
            }()
            input = OperatingEventInput(
                momentId: model.momentId,
                eventType: "SALE",
                title: title,
                amount: NSDecimalNumber(decimal: subtotal).stringValue,
                note: noteLine(),
                channelName: salesMode,
                counterpartyName: guest.isEmpty ? nil : guest,
                lineItems: cart.map {
                    .init(
                        itemName: $0.name,
                        quantity: "\($0.quantity)",
                        unit: $0.unit,
                        unitPrice: $0.unitPrice,
                        masterId: $0.masterId
                    )
                },
                contract: contract,
                paymentStatus: "COLLECTED"
            )
        case .holdRecall:
            return
        }

        let ok = await model.record(input)
        if ok {
            if let id = recalledDraftId {
                RetailParkedSaleStore.shared.remove(id: id, momentId: model.momentId)
            }
            dismiss()
        }
    }

    private func noteLine() -> String? {
        let bits = [
            "Tender: \(tender)",
            note.trimmingCharacters(in: .whitespaces).isEmpty ? nil : note.trimmingCharacters(in: .whitespaces),
        ].compactMap { $0 }
        return bits.isEmpty ? nil : bits.joined(separator: " · ")
    }
}

// MARK: - Parked POS drafts (session / in-memory)

struct RetailParkedSale: Identifiable, Equatable {
    let id: String
    let label: String
    let meta: String
    let amount: String
    let channel: String
    let customer: String
    let note: String
    let tender: String
    let lines: [RetailPosLine]
    let parkedAt: Date

    var parkDisplayId: String {
        let short = id.replacingOccurrences(of: "-", with: "").prefix(4).uppercased()
        return short.isEmpty ? "HOLD" : "HOLD-\(short)"
    }

    var ageLabel: String {
        let mins = max(0, Int(Date().timeIntervalSince(parkedAt) / 60))
        if mins < 1 { return "just now" }
        if mins < 60 { return "\(mins)m ago" }
        return "\(mins / 60)h ago"
    }
}

final class RetailParkedSaleStore {
    static let shared = RetailParkedSaleStore()
    private var byMoment: [String: [RetailParkedSale]] = [:]

    func list(momentId: String) -> [RetailParkedSale] {
        (byMoment[momentId] ?? []).sorted { $0.parkedAt > $1.parkedAt }
    }

    func park(momentId: String, draft: RetailParkedSale) {
        var list = byMoment[momentId] ?? []
        list.insert(draft, at: 0)
        byMoment[momentId] = Array(list.prefix(8))
    }

    func remove(id: String, momentId: String) {
        byMoment[momentId] = (byMoment[momentId] ?? []).filter { $0.id != id }
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

struct RetailPosLine: Identifiable, Equatable {
    let id: String
    let masterId: String?
    let name: String
    let unit: String?
    var quantity: Int
    var unitPrice: String
    var skuCode: String? = nil
    var hsn: String? = nil

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
            hsn: sku.hsn?.nilIfBlank
        )
    }
}

private extension String {
    var nilIfBlank: String? {
        let t = trimmingCharacters(in: .whitespacesAndNewlines)
        return t.isEmpty ? nil : t
    }
}

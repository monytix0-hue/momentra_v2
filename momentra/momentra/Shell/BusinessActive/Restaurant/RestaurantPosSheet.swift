import SwiftUI

/// POS bill — Figma `RST-QA-01-ALT · POS · In-dining` `2446:29655`, `· Takeaway` `2448:29655`,
/// `· Cloud` `2446:29811`. Posts itemized `SALE`. Cloud Platform AR / Takeaway Pay on pickup open
/// AR as `PAY_LATER`. Discount + split tender ride `contract.discount` / `contract.payments`.
/// Explicit cess ₹ is optional (operator-entered only).
struct RestaurantPosSheet: View {
    let theme: OperatingTheme
    @ObservedObject var model: BusinessOperatingModel

    @Environment(\.dismiss) private var dismiss

    private enum PosMode: String, CaseIterable, Identifiable {
        case inDining = "In-dining"
        case cloud = "Cloud"
        case takeaway = "Takeaway"
        var id: String { rawValue }
    }

    @State private var mode: PosMode = .inDining
    @State private var tender = "UPI"
    @State private var splitTender = false
    @State private var splitUpi = ""
    @State private var splitCash = ""
    @State private var splitCard = ""
    @State private var discount = ""
    @State private var cessAmount = ""
    @State private var dishQuery = ""
    @State private var guest = ""
    @State private var phone = ""
    @State private var note = ""
    @State private var tableNumber = ""
    @State private var covers = ""
    @State private var platform = "Zomato"
    @State private var platformOrderId = ""
    @State private var readyBy = ""
    @State private var prepBy = ""
    @State private var packaging = "Standard"
    @State private var billPrint = "Thermal + KOT"
    @State private var parentKotId: String?
    @State private var cart: [RestaurantPosLine] = []
    @State private var saving = false
    @State private var moreOpen = false
    @State private var showKotPicker = false

    private var currency: String { model.currency }

    private var placeName: String {
        let title = model.title.trimmingCharacters(in: .whitespacesAndNewlines)
        return title.isEmpty ? "Spice Route BKC" : title
    }

    private var menuDishes: [OperatingMasterPayload] {
        let source = model.masters.isEmpty ? (model.projection?.masters ?? []) : model.masters
        return source
            .filter { ["MENU_DISH", "PRODUCT", "ITEM", "SKU"].contains($0.kind.uppercased()) }
            .sorted { $0.name.localizedCaseInsensitiveCompare($1.name) == .orderedAscending }
    }

    private var openKots: [OperatingActivityPayload] {
        (model.projection?.activity ?? [])
            .filter { $0.eventType.uppercased() == "ORDER" }
            .filter {
                let status = ($0.status ?? "").uppercased()
                return status != "COMPLETED" && status != "CANCELLED" && status != "DONE"
            }
            .sorted { $0.occurredAt > $1.occurredAt }
    }

    private var linkedKot: OperatingActivityPayload? {
        openKots.first { $0.id == parentKotId }
    }

    private var subtotal: Decimal {
        cart.reduce(Decimal.zero) { $0 + $1.amount }
    }

    private var discountValue: Decimal { PosSettlement.parseAmount(discount) }
    private var cessValue: Decimal { PosSettlement.parseAmount(cessAmount) }
    private var payable: Decimal { PosSettlement.payable(gross: subtotal, discount: discountValue, cessAmount: cessValue) }
    private var splitAmounts: [String: String] { ["UPI": splitUpi, "Cash": splitCash, "Card": splitCard] }
    private var splitOk: Bool {
        opensReceivable || !splitTender || PosSettlement.splitSumsToPayable(splitAmounts, payable: payable)
    }
    private var discountOk: Bool { discountValue <= subtotal || subtotal <= 0 }

    private var subtotalText: String {
        guard !cart.isEmpty else { return "—" }
        return OperatingV12Money.amount(NSDecimalNumber(decimal: subtotal).stringValue, currency: currency)
    }

    private var payableText: String {
        guard payable > 0 else { return "—" }
        return OperatingV12Money.amount(NSDecimalNumber(decimal: payable).stringValue, currency: currency)
    }

    private var postable: Bool {
        !cart.isEmpty
            && cart.allSatisfy { (Decimal(string: $0.unitPrice) ?? 0) > 0 }
            && discountOk
            && splitOk
            && payable > 0
    }

    private var gstOn: Bool {
        (model.projection?.gstTracking ?? "").uppercased() == "YES"
            && (model.projection?.gstRegistration ?? "").uppercased() == "REGULAR"
    }

    private var gstInclusive: Bool {
        (model.projection?.salePriceIncludesGst ?? "").uppercased() != "NO"
    }

    private var gstEstimate: Decimal? {
        guard gstOn, !cart.isEmpty else { return nil }
        return RetailGstEstimate.from(
            amounts: cart.map(\.amount),
            rates: cart.map(\.gstRate),
            inclusive: gstInclusive
        )
    }

    private var opensReceivable: Bool {
        switch mode {
        case .cloud:
            // Platform AR settles later via Collection. Prepaid / UPI close the bill now.
            return tender == "Platform AR"
        case .takeaway:
            return tender == "Pay on pickup"
        case .inDining:
            return false
        }
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                EmberGrabber()
                EmberSheetHeader(
                    title: "POS Bill",
                    subtitle: contextLine,
                    onClose: { dismiss() }
                )

                EmberChipRow(
                    options: PosMode.allCases.map(\.rawValue),
                    selection: Binding(
                        get: { mode.rawValue },
                        set: { raw in
                            if let next = PosMode(rawValue: raw) {
                                applyModeChange(next)
                            }
                        }
                    )
                )

                subChips

                scanSection

                if menuDishes.isEmpty {
                    EmberEmptyCard(
                        title: "Menu not set up",
                        bodyText: "Add dishes under Menu first. A bill line needs a real dish name to post."
                    )
                } else if !dishQuery.trimmingCharacters(in: .whitespaces).isEmpty {
                    dishMatches
                }

                ticketCard

                totalsCard

                tenderSection

                modeFields

                EmberMoreDetails(hint: moreHint, open: $moreOpen) {
                    VStack(alignment: .leading, spacing: 10) {
                        EmberFieldLabel(text: "Notes")
                        EmberTextField(placeholder: notePlaceholder, text: $note)
                        EmberFieldLabel(text: "Cess ₹ (optional · explicit only)")
                        EmberTextField(placeholder: "0", text: $cessAmount)
                        Text("Cess is operator-entered only — no HSN/CBIC schedule lookup.")
                            .font(.plusJakarta(size: 10))
                            .foregroundStyle(EmberSheetChrome.textFaint)
                        if mode == .inDining {
                            Text("Seat hold — locked until seat/hold contract lands.")
                                .font(.plusJakarta(size: 11))
                                .foregroundStyle(EmberSheetChrome.textFaint)
                        }
                    }
                }

                EmberWhyBox(text: whyCopy)

                EmberStickyFooter(
                    title: footerTitle,
                    disabled: !postable,
                    loading: saving,
                    action: { Task { await postBill() } }
                )
            }
            .padding(.horizontal, 20)
            .padding(.top, 12)
            .padding(.bottom, 28)
        }
        .background(EmberSheetChrome.sheet)
        .sheet(isPresented: $showKotPicker) {
            kotPickerSheet
        }
        .task {
            applyModeDefaults(mode)
            if model.masters.isEmpty {
                await model.loadMasters()
            }
        }
    }

    // MARK: - Mode chrome

    private var contextLine: String {
        switch mode {
        case .inDining:
            return "Floor · \(placeName) · Station open"
        case .cloud:
            return "Cloud / aggregator · \(placeName)"
        case .takeaway:
            return "Counter pickup · \(placeName) · Ready desk"
        }
    }

    private var moreHint: String {
        switch mode {
        case .inDining: return "Notes · Split seat · Hold table"
        case .cloud: return "Notes · Rider code · Tags"
        case .takeaway: return "Notes · Allergy · Hold pickup"
        }
    }

    private var notePlaceholder: String {
        switch mode {
        case .inDining: return "Course, kitchen note, reference"
        case .cloud: return "Rider code, platform tags"
        case .takeaway: return "Allergy, pickup note"
        }
    }

    private var whyCopy: String {
        switch mode {
        case .inDining:
            return "In-dining POS posts one bill + tender. KOT/prep status never posts revenue. Recipe stock deduction excluded in v5."
        case .cloud:
            return "Cloud bill posts revenue now; platform settlement is Collection later. Fees must not double-count cost. Prep/ready status ≠ revenue."
        case .takeaway:
            return "Takeaway posts bill + tender (or Pay on pickup → open AR until collect). Ready status is ops only — never posts revenue by itself."
        }
    }

    private var footerTitle: String {
        if saving { return "Posting…" }
        switch mode {
        case .inDining:
            return postable ? "Charge \(payableText) · Post & print" : "Charge · Post & print"
        case .cloud:
            return postable ? "Post cloud bill · \(payableText)" : "Post cloud bill"
        case .takeaway:
            return postable ? "Charge \(payableText) · Post & notify" : "Charge · Post & notify"
        }
    }

    private var scanLabel: String {
        switch mode {
        case .inDining: return "Add from menu / KOT"
        case .cloud: return "Confirm platform items"
        case .takeaway: return "Add from menu"
        }
    }

    private var scanPlaceholder: String {
        switch mode {
        case .inDining:
            return menuDishes.isEmpty ? "No saved dish — add one under Menu" : "Butter chicken · search menu"
        case .cloud:
            return platformOrderId.isEmpty ? "ZOM-9182 · synced ticket" : "\(platformOrderId) · synced ticket"
        case .takeaway:
            return menuDishes.isEmpty ? "No saved dish — add one under Menu" : "Search takeaway menu"
        }
    }

    private var scanActionLabel: String {
        mode == .cloud ? "Sync" : "Add"
    }

    private var ticketTitle: String {
        switch mode {
        case .inDining: return "Table ticket"
        case .cloud: return "Cloud ticket"
        case .takeaway: return "Pickup ticket"
        }
    }

    private var ticketTrailing: String {
        if cart.isEmpty { return "No items yet" }
        let count = "\(cart.count) item\(cart.count == 1 ? "" : "s")"
        if let kot = linkedKot {
            let key = kot.clientKey?.trimmingCharacters(in: .whitespacesAndNewlines)
            if let key, !key.isEmpty { return "\(count) · \(key)" }
            return "\(count) · KOT linked"
        }
        if mode == .cloud, !platformOrderId.trimmingCharacters(in: .whitespaces).isEmpty {
            return "\(count) · \(platformOrderId.trimmingCharacters(in: .whitespaces))"
        }
        return "\(count) of 12 lines"
    }

    private var addLineHint: String {
        switch mode {
        case .inDining: return "Courses · Favourites"
        case .cloud: return "Platform menu map"
        case .takeaway: return "Combos · Favourites"
        }
    }

    private var tenderOptions: [String] {
        switch mode {
        case .inDining: return ["UPI", "Cash", "Card"]
        case .cloud: return ["Platform AR", "Prepaid", "UPI"]
        case .takeaway: return ["UPI", "Cash", "Card", "Pay on pickup"]
        }
    }

    private var lockedTenders: Set<String> { [] }

    // MARK: - Sections

    @ViewBuilder
    private var subChips: some View {
        switch mode {
        case .inDining:
            HStack(spacing: 6) {
                EmberModeChip(
                    text: tableChipLabel,
                    selected: true,
                    style: .mode
                ) {}
                EmberModeChip(
                    text: coversChipLabel,
                    selected: false,
                    style: .mode
                ) {}
                EmberModeChip(
                    text: "KOT live",
                    selected: parentKotId != nil,
                    style: .mode
                ) {
                    showKotPicker = true
                }
                Spacer(minLength: 0)
            }
        case .cloud:
            HStack(spacing: 6) {
                EmberModeChip(text: platform, selected: true, style: .mode) {}
                EmberModeChip(text: "SLA 28m", selected: false, locked: true) {}
                EmberModeChip(text: "Prep", selected: false, locked: true) {}
                EmberModeChip(
                    text: "KOT live",
                    selected: parentKotId != nil,
                    style: .mode
                ) {
                    showKotPicker = true
                }
                Spacer(minLength: 0)
            }
        case .takeaway:
            HStack(spacing: 6) {
                EmberModeChip(text: "Pickup", selected: true, style: .mode) {}
                EmberModeChip(text: "Ready 15m", selected: false, locked: true) {}
                EmberModeChip(
                    text: tender == "Pay on pickup" ? "Open AR" : "Prepaid",
                    selected: false,
                    locked: true
                ) {}
                EmberModeChip(
                    text: "KOT live",
                    selected: parentKotId != nil,
                    style: .mode
                ) {
                    showKotPicker = true
                }
                Spacer(minLength: 0)
            }
        }
    }

    private var tableChipLabel: String {
        let table = tableNumber.trimmingCharacters(in: .whitespaces)
        return table.isEmpty ? "Table" : "Table \(table)"
    }

    private var coversChipLabel: String {
        let value = covers.trimmingCharacters(in: .whitespaces)
        return value.isEmpty ? "Covers" : "\(value) covers"
    }

    private var scanSection: some View {
        VStack(alignment: .leading, spacing: 6) {
            EmberFieldLabel(text: scanLabel)
            EmberScanField(
                placeholder: scanPlaceholder,
                query: $dishQuery,
                actionLabel: scanActionLabel,
                disabled: mode != .cloud && menuDishes.isEmpty,
                action: {
                    if mode == .cloud {
                        syncCloudTicket()
                    } else {
                        addDish()
                    }
                }
            )
        }
    }

    @ViewBuilder
    private var dishMatches: some View {
        let matches = menuDishes
            .filter { $0.name.localizedCaseInsensitiveContains(dishQuery.trimmingCharacters(in: .whitespaces)) }
            .prefix(4)
        if !matches.isEmpty {
            VStack(spacing: 6) {
                ForEach(Array(matches)) { dish in
                    Button {
                        add(dish)
                        dishQuery = ""
                    } label: {
                        HStack {
                            VStack(alignment: .leading, spacing: 2) {
                                if let code = dish.code, !code.isEmpty {
                                    Text(code)
                                        .font(.plusJakarta(size: 10, weight: .bold))
                                        .foregroundStyle(EmberSheetChrome.gold)
                                }
                                Text(dish.name)
                                    .font(.plusJakarta(size: 13, weight: .semibold))
                                    .foregroundStyle(EmberSheetChrome.textPrimary)
                                    .lineLimit(1)
                            }
                            Spacer()
                            if let price = dish.sellPrice, !price.isEmpty {
                                Text(OperatingV12Money.amount(price, currency: currency))
                                    .font(.plusJakarta(size: 10, weight: .bold))
                                    .foregroundStyle(EmberSheetChrome.gold)
                            }
                        }
                        .padding(.horizontal, 12)
                        .padding(.vertical, 9)
                        .background(EmberSheetChrome.chip)
                        .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }

    private var ticketCard: some View {
        EmberLinesCard(title: ticketTitle, trailing: ticketTrailing) {
            if cart.isEmpty {
                Text("Pick a saved dish above, then confirm its price. Every line posts as an itemized sale line.")
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(EmberSheetChrome.textBody)
                    .padding(.vertical, 8)
            } else {
                ForEach(Array(cart.enumerated()), id: \.element.id) { index, line in
                    cartLine(index: index, line: line)
                    if index < cart.count - 1 {
                        Divider().overlay(EmberSheetChrome.chip.opacity(0.6))
                    }
                }
            }
            HStack {
                Button {
                    // Focuses search — operator types or picks from matches.
                } label: {
                    Text("+ Add item")
                        .font(.plusJakarta(size: 13, weight: .bold))
                        .foregroundStyle(EmberSheetChrome.gold)
                }
                .buttonStyle(.plain)
                Spacer()
                Text(addLineHint)
                    .font(.plusJakarta(size: 11))
                    .foregroundStyle(EmberSheetChrome.textBody)
            }
            .padding(.top, 4)
            if !cart.isEmpty && !postable {
                Text("Every line needs a price above ₹0 before the bill can post.")
                    .font(.plusJakarta(size: 10))
                    .foregroundStyle(EmberSheetChrome.amber)
                    .padding(.top, 4)
            }
        }
    }

    private func cartLine(index: Int, line: RestaurantPosLine) -> some View {
        HStack(alignment: .center, spacing: 8) {
            VStack(alignment: .leading, spacing: 2) {
                if let code = line.code, !code.isEmpty {
                    Text(code)
                        .font(.plusJakarta(size: 10, weight: .bold))
                        .foregroundStyle(EmberSheetChrome.gold)
                }
                Text(line.name)
                    .font(.plusJakarta(size: 13, weight: .medium))
                    .foregroundStyle(EmberSheetChrome.textPrimary)
                    .lineLimit(1)
                if let meta = line.meta, !meta.isEmpty {
                    Text(meta)
                        .font(.plusJakarta(size: 11))
                        .foregroundStyle(EmberSheetChrome.textBody)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            EmberStepper(
                quantity: line.quantity,
                onDecrement: {
                    guard cart.indices.contains(index) else { return }
                    if cart[index].quantity <= 1 {
                        cart.remove(at: index)
                    } else {
                        cart[index].quantity -= 1
                    }
                },
                onIncrement: {
                    guard cart.indices.contains(index), cart[index].quantity < 99 else { return }
                    cart[index].quantity += 1
                }
            )

            TextField("0", text: bindingPrice(at: index))
                .keyboardType(.decimalPad)
                .multilineTextAlignment(.trailing)
                .font(.plusJakarta(size: 13, weight: .bold))
                .foregroundStyle(EmberSheetChrome.textPrimary)
                .frame(width: 64)
        }
        .padding(.vertical, 8)
    }

    private var tenderLabel: String {
        if opensReceivable { return mode == .cloud ? "Later AR" : tender }
        return PosSettlement.tenderLabel(
            split: splitTender,
            primary: tender,
            payments: PosSettlement.buildPayments(
                split: splitTender,
                primary: tender,
                payable: payable,
                splitAmounts: splitAmounts,
                openAr: opensReceivable
            )
        )
    }

    private var totalsCard: some View {
        EmberTotalsCard(
            rows: totalsRows,
            payableLabel: mode == .cloud ? "Bill value" : "Payable now",
            payable: cart.isEmpty ? "—" : payableText,
            sideLabel: mode == .cloud ? "Collect" : "Tender",
            sideValue: tenderLabel,
            sideWarning: mode == .cloud && opensReceivable
        )
    }

    private var gstRowLabel: String {
        guard gstOn else { return "GST" }
        return gstInclusive ? "GST (incl. est.)" : "GST (excl. est.)"
    }

    private var gstRowValue: String {
        if cart.isEmpty { return "—" }
        guard gstOn else { return "Unavailable" }
        if let gstEstimate {
            return OperatingV12Money.amount(NSDecimalNumber(decimal: gstEstimate).stringValue, currency: currency)
        }
        return "Unavailable"
    }

    private var totalsRows: [(label: String, value: String, warning: Bool)] {
        let gstWarning = gstEstimate == nil
        var rows: [(label: String, value: String, warning: Bool)] = [
            (mode == .cloud ? "Menu total" : "Subtotal", cart.isEmpty ? "—" : subtotalText, false),
        ]
        if discountValue > 0 {
            rows.append((
                "Discount",
                "−" + OperatingV12Money.amount(NSDecimalNumber(decimal: discountValue).stringValue, currency: currency),
                false
            ))
        }
        if cessValue > 0 {
            rows.append((
                "Cess",
                OperatingV12Money.amount(NSDecimalNumber(decimal: cessValue).stringValue, currency: currency),
                false
            ))
        }
        rows.append((gstRowLabel, gstRowValue, gstWarning))
        switch mode {
        case .inDining:
            rows.append(("Service", "Unavailable", true))
        case .cloud:
            rows.append(("Platform fee (info)", "₹0 on bill", false))
        case .takeaway:
            rows.append(("Packaging", "Unavailable", true))
        }
        return rows
    }

    private var tenderSection: some View {
        VStack(alignment: .leading, spacing: 6) {
            EmberFieldLabel(text: "Discount")
            EmberTextField(placeholder: "0", text: $discount)
            EmberFieldLabel(text: "Tender")
            EmberChipRow(
                options: tenderOptions,
                selection: Binding(
                    get: { tender },
                    set: { next in
                        splitTender = false
                        tender = next
                    }
                ),
                style: .tender,
                locked: lockedTenders
            )
            if mode == .inDining || (mode == .takeaway && tender != "Pay on pickup") {
                Button {
                    splitTender.toggle()
                } label: {
                    Text(splitTender ? "Split · on" : "Split tender")
                        .font(.plusJakarta(size: 12, weight: .bold))
                        .foregroundStyle(splitTender ? EmberSheetChrome.tenderSelected : EmberSheetChrome.textBody)
                }
            }
            if splitTender && !opensReceivable {
                EmberFieldLabel(text: "UPI")
                EmberTextField(placeholder: "0", text: $splitUpi)
                EmberFieldLabel(text: "Cash")
                EmberTextField(placeholder: "0", text: $splitCash)
                EmberFieldLabel(text: "Card")
                EmberTextField(placeholder: "0", text: $splitCard)
                Text(splitOk
                     ? "Split instruments post on the contract and must add up to payable."
                     : "Split amounts must add up to payable before Charge.")
                    .font(.plusJakarta(size: 10))
                    .foregroundStyle(splitOk ? EmberSheetChrome.textFaint : theme.warning)
            } else if mode == .inDining {
                Text("One instrument posts as contract.payments · use Split for UPI+Cash+Card.")
                    .font(.plusJakarta(size: 10))
                    .foregroundStyle(EmberSheetChrome.textFaint)
            }
        }
    }

    @ViewBuilder
    private var modeFields: some View {
        switch mode {
        case .inDining:
            EmberPickerRow(
                label: "Table / covers",
                value: tableCoversValue
            ) {
                // Inline edit via fields below when expanded; keep picker chrome for Figma parity.
                moreOpen = true
            }
            HStack(spacing: 8) {
                VStack(alignment: .leading, spacing: 6) {
                    EmberFieldLabel(text: "Table")
                    EmberTextField(placeholder: "7", text: $tableNumber)
                }
                VStack(alignment: .leading, spacing: 6) {
                    EmberFieldLabel(text: "Covers")
                    EmberTextField(placeholder: "4", text: $covers, keyboard: .numberPad)
                }
            }
            EmberPickerRow(label: "Bill / print", value: billPrint)
            EmberPickerRow(
                label: "Guest",
                value: guest.trimmingCharacters(in: .whitespaces).isEmpty ? "Walk-in" : guest
            )
            EmberTextField(placeholder: "Walk-in or guest name", text: $guest)
        case .cloud:
            EmberPickerRow(label: "Platform", value: platform) {
                cyclePlatform()
            }
            EmberFieldLabel(text: "Order id")
            EmberTextField(placeholder: "ZOM-9182", text: $platformOrderId)
            EmberFieldLabel(text: "Prep by")
            EmberTextField(placeholder: "12:48", text: $prepBy)
            EmberPickerRow(label: "Packaging", value: packaging) {
                packaging = packaging == "Standard" ? "No cutlery" : "Standard"
            }
        case .takeaway:
            EmberFieldLabel(text: "Guest name")
            EmberTextField(placeholder: "Priya", text: $guest)
            EmberFieldLabel(text: "Phone")
            EmberTextField(placeholder: "+91 …", text: $phone, keyboard: .phonePad)
            EmberFieldLabel(text: "Ready by")
            EmberTextField(placeholder: "12:55 · counter", text: $readyBy)
            EmberPickerRow(label: "Bill / print", value: billPrint)
        }
    }

    private var tableCoversValue: String {
        let table = tableNumber.trimmingCharacters(in: .whitespaces)
        let cover = covers.trimmingCharacters(in: .whitespaces)
        switch (table.isEmpty, cover.isEmpty) {
        case (true, true): return "Table · covers"
        case (false, true): return "Table \(table)"
        case (true, false): return "\(cover) covers"
        case (false, false): return "Table \(table) · \(cover)"
        }
    }

    private var kotPickerSheet: some View {
        NavigationStack {
            List {
                if openKots.isEmpty {
                    Text("No open kitchen tickets. Send a KOT first — linking never invents a ticket id.")
                        .font(.plusJakarta(size: 13))
                        .foregroundStyle(EmberSheetChrome.textBody)
                } else {
                    Button("Clear KOT link") {
                        parentKotId = nil
                        showKotPicker = false
                    }
                    ForEach(openKots) { ticket in
                        Button {
                            parentKotId = ticket.id
                            applyKot(ticket)
                            showKotPicker = false
                        } label: {
                            VStack(alignment: .leading, spacing: 4) {
                                Text(ticket.title.isEmpty ? "Kitchen ticket" : ticket.title)
                                    .font(.plusJakarta(size: 14, weight: .semibold))
                                Text(kotMeta(ticket))
                                    .font(.plusJakarta(size: 11))
                                    .foregroundStyle(.secondary)
                            }
                        }
                    }
                }
            }
            .navigationTitle("Bill from KOT")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Close") { showKotPicker = false }
                }
            }
        }
        .presentationDetents([.medium])
    }

    // MARK: - Actions

    private func applyModeChange(_ next: PosMode) {
        mode = next
        applyModeDefaults(next)
    }

    private func applyModeDefaults(_ next: PosMode) {
        switch next {
        case .inDining:
            tender = "UPI"
            billPrint = "Thermal + KOT"
        case .cloud:
            tender = "Platform AR"
            billPrint = "Platform slip"
            if platform.trimmingCharacters(in: .whitespaces).isEmpty {
                platform = "Zomato"
            }
        case .takeaway:
            tender = "UPI"
            billPrint = "Thermal · SMS link"
        }
    }

    private func cyclePlatform() {
        let options = ["Zomato", "Swiggy", "Other"]
        if let idx = options.firstIndex(of: platform) {
            platform = options[(idx + 1) % options.count]
        } else {
            platform = options[0]
        }
    }

    private func syncCloudTicket() {
        let typed = dishQuery.trimmingCharacters(in: .whitespaces)
        // Sync only binds a typed platform order id — never invent ZOM-9182 or auto-add dishes.
        if !typed.isEmpty {
            platformOrderId = typed
                .replacingOccurrences(of: "· synced ticket", with: "")
                .trimmingCharacters(in: .whitespaces)
        }
        dishQuery = ""
    }

    private func applyKot(_ ticket: OperatingActivityPayload) {
        if let channel = ticket.channelName {
            if let matched = PosMode(rawValue: channel) {
                mode = matched
                applyModeDefaults(matched)
            }
        }
        if let table = ticket.tableNumber, !table.isEmpty { tableNumber = table }
        if let cover = ticket.covers ?? ticket.quantity, !cover.isEmpty { covers = cover }
        if guest.trimmingCharacters(in: .whitespaces).isEmpty,
           let name = ticket.counterpartyName, !name.isEmpty {
            guest = name
        }
        if let key = ticket.clientKey, !key.isEmpty, mode == .cloud {
            platformOrderId = key
        }
        let seeded = seedPosCartFromKot(ticket, menu: menuDishes)
        if !seeded.isEmpty {
            cart = Array(seeded.prefix(12))
        }
    }

    private func seedPosCartFromKot(
        _ ticket: OperatingActivityPayload,
        menu: [OperatingMasterPayload]
    ) -> [RestaurantPosLine] {
        let rows = (ticket.lines ?? []).filter { !$0.itemName.trimmingCharacters(in: .whitespaces).isEmpty }
        guard !rows.isEmpty else { return [] }
        let byId = Dictionary(uniqueKeysWithValues: menu.map { ($0.id, $0) })
        let byName = Dictionary(uniqueKeysWithValues: menu.map { ($0.name.lowercased(), $0) })
        return rows.map { row -> RestaurantPosLine in
            let master = row.masterId.flatMap { byId[$0] }
                ?? byName[row.itemName.trimmingCharacters(in: .whitespaces).lowercased()]
            let qty = max(1, min(99, Int(row.quantity ?? "1") ?? 1))
            // Never drop a KOT line for a missing price — menu sellPrice, else "0" so cart hydrates.
            let price = row.unitPrice?.nilIfBlank
                ?? master?.sellPrice?.nilIfBlank
                ?? "0"
            let needsPrice = (Decimal(string: price) ?? 0) <= 0
            return RestaurantPosLine(
                id: master?.id ?? row.masterId ?? UUID().uuidString,
                masterId: productionMasterId(master?.id ?? row.masterId),
                code: master?.code,
                name: master?.name ?? row.itemName,
                meta: needsPrice
                    ? "Price needed"
                    : (mode == .cloud || mode == .takeaway ? "Pack" : (master?.unit ?? "KOT")),
                unit: row.unit?.nilIfBlank ?? master?.unit ?? "pc",
                quantity: qty,
                unitPrice: price,
                gstRate: master?.gstRate?.nilIfBlank
            )
        }
    }

    private func kotMeta(_ ticket: OperatingActivityPayload) -> String {
        [
            ticket.tableNumber.flatMap { $0.isEmpty ? nil : "Table \($0)" },
            ticket.covers.flatMap { $0.isEmpty ? nil : "\($0) covers" },
            ticket.channelName.flatMap { $0.isEmpty ? nil : $0 },
            ticket.clientKey.flatMap { $0.isEmpty ? nil : $0 },
            ticket.counterpartyName.flatMap { $0.isEmpty ? nil : $0 },
        ]
        .compactMap { $0 }
        .joined(separator: " · ")
        .ifEmpty("Open order")
    }

    private func addDish() {
        let typed = dishQuery.trimmingCharacters(in: .whitespaces)
        guard !menuDishes.isEmpty else { return }
        let match = menuDishes.first { $0.name.caseInsensitiveCompare(typed) == .orderedSame }
            ?? menuDishes.first { typed.isEmpty == false && $0.name.localizedCaseInsensitiveContains(typed) }
            ?? (typed.isEmpty ? nil : menuDishes.first)
        if let match { add(match) }
        dishQuery = ""
    }

    private func add(_ dish: OperatingMasterPayload) {
        if let index = cart.firstIndex(where: { $0.masterId == dish.id }) {
            guard cart[index].quantity < 99 else { return }
            cart[index].quantity += 1
            return
        }
        guard cart.count < 12 else { return }
        cart.append(RestaurantPosLine.of(dish, cloudPack: mode == .cloud || mode == .takeaway))
    }

    private func bindingPrice(at index: Int) -> Binding<String> {
        Binding(
            get: { cart.indices.contains(index) ? cart[index].unitPrice : "" },
            set: { value in
                guard cart.indices.contains(index) else { return }
                cart[index].unitPrice = value
            }
        )
    }

    private func postBill() async {
        guard postable else { return }
        saving = true
        defer { saving = false }

        let table = tableNumber.trimmingCharacters(in: .whitespaces)
        let coverCount = covers.trimmingCharacters(in: .whitespaces)
        let guestName = guest.trimmingCharacters(in: .whitespaces)
        let amount = NSDecimalNumber(decimal: subtotal).stringValue

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

        let payablePlain = PosSettlement.money(payable)
        var contract = OperatingContractInput()
        contract.entryMode = "ITEMIZED"
        contract.paymentState = opensReceivable ? "PAY_LATER" : "RECEIVED"
        contract.receivedAmount = opensReceivable ? nil : payablePlain
        if discountValue > 0 { contract.discount = PosSettlement.money(discountValue) }
        if cessValue > 0 { contract.cessAmount = PosSettlement.money(cessValue) }
        contract.payments = PosSettlement.buildPayments(
            split: splitTender && !opensReceivable,
            primary: tender,
            payable: payable,
            splitAmounts: splitAmounts,
            openAr: opensReceivable
        )
        contract.taxMode = taxMode
        if gstOn {
            contract.placeOfSupply = model.projection?.gstState?.nilIfBlank
        }
        contract.tableNumber = table.isEmpty ? nil : table
        contract.covers = coverCount.isEmpty ? nil : coverCount
        if mode == .cloud {
            let orderId = platformOrderId.trimmingCharacters(in: .whitespaces)
            contract.clientKey = orderId.isEmpty ? nil : orderId
        } else if let key = linkedKot?.clientKey, !key.isEmpty {
            contract.clientKey = key
        }

        let noteBits: [String?] = [
            mode == .cloud ? "Platform: \(platform)" : nil,
            mode == .cloud && !platformOrderId.isEmpty ? "Order: \(platformOrderId)" : nil,
            mode == .takeaway && !phone.isEmpty ? "Phone: \(phone)" : nil,
            mode == .takeaway && !readyBy.isEmpty ? "Ready: \(readyBy)" : nil,
            mode == .cloud && !prepBy.isEmpty ? "Prep by: \(prepBy)" : nil,
            note.trimmingCharacters(in: .whitespaces).isEmpty ? nil : note,
        ]

        let title: String = {
            switch mode {
            case .cloud:
                if !platformOrderId.isEmpty { return "Cloud bill · \(platformOrderId)" }
                return "Cloud bill · \(platform)"
            case .takeaway:
                if !guestName.isEmpty { return "Takeaway · \(guestName)" }
                return "Takeaway · \(cart.count) item\(cart.count == 1 ? "" : "s")"
            case .inDining:
                if !guestName.isEmpty { return "Bill · \(guestName)" }
                if !table.isEmpty { return "Bill · Table \(table)" }
                return "Bill · \(cart.count) item\(cart.count == 1 ? "" : "s")"
            }
        }()

        let ok = await model.record(
            OperatingEventInput(
                momentId: model.momentId,
                eventType: "SALE",
                title: title,
                amount: amount,
                note: noteBits.compactMap { $0 }.joined(separator: " · "),
                channelName: mode.rawValue,
                counterpartyName: guestName.isEmpty
                    ? (mode == .cloud ? platform : nil)
                    : guestName,
                parentEventId: parentKotId,
                quantity: coverCount.isEmpty ? nil : coverCount,
                lineItems: cart.map {
                    .init(
                        itemName: $0.name,
                        quantity: "\($0.quantity)",
                        unit: $0.unit,
                        unitPrice: $0.unitPrice,
                        masterId: productionMasterId($0.masterId),
                        taxMode: taxMode == "NOT_CONFIGURED" ? nil : taxMode,
                        gstRate: $0.gstRate
                    )
                },
                contract: contract,
                paymentStatus: opensReceivable ? "PENDING" : "COLLECTED"
            )
        )
        if ok { dismiss() }
    }
}

private func productionMasterId(_ masterId: String?) -> String? {
    guard let id = masterId?.trimmingCharacters(in: .whitespaces), !id.isEmpty else { return nil }
    if id.lowercased().hasPrefix("fx-") { return nil }
    return id
}

// MARK: - Models

private extension String {
    func ifEmpty(_ fallback: String) -> String { isEmpty ? fallback : self }
}

private struct RestaurantPosLine: Identifiable, Equatable {
    let id: String
    let masterId: String?
    let code: String?
    let name: String
    let meta: String?
    let unit: String?
    var quantity: Int
    var unitPrice: String
    var gstRate: String? = nil

    var amount: Decimal {
        let price = Decimal(string: unitPrice) ?? 0
        return price * Decimal(quantity)
    }

    static func of(_ dish: OperatingMasterPayload, cloudPack: Bool) -> RestaurantPosLine {
        RestaurantPosLine(
            id: dish.id,
            masterId: dish.id,
            code: dish.code,
            name: dish.name,
            meta: cloudPack ? "Pack" : dish.unit,
            unit: dish.unit ?? "pc",
            quantity: 1,
            unitPrice: dish.sellPrice ?? "",
            gstRate: dish.gstRate?.nilIfBlank
        )
    }
}

import SwiftUI

/// POS bill — Figma `RST-QA-01-ALT · POS · In-dining` `2446:29655`, `· Cloud` `2446:29802`,
/// `· Takeaway` `2448:29655`.
///
/// Honest mapping onto the strict operating contract:
/// - the three mode chips set `channelName`, which `validateSale` requires;
/// - each cart line posts as a real `lines[]` entry (`itemName`, `quantity`, `unit`,
///   `unitPrice`) and the bill posts `SALE` with `entryMode = ITEMIZED`;
/// - the tender chips collapse to the single `paymentState` one bill carries;
/// - `GST` and `Service` have no client-settable field here, so they read `Unavailable`
///   with the reason printed instead of a fabricated `₹0` line;
/// - Table / covers / KOT number have no field on `.strict()`, so they render disabled
///   with the reason printed rather than pretending to save.
///
/// `MENU_DISH` masters carry no price column, so the price is typed on the ticket line —
/// the backend derives the bill amount from `lines[]`.
struct RestaurantPosSheet: View {
    let model: OperatingProjectionPayload
    var onDismiss: () -> Void
    var onSubmit: (RestaurantEventDraft) -> Void

    @State private var mode: String = ""
    @State private var tender = "UPI"
    @State private var dishQuery = ""
    @State private var guest = ""
    @State private var note = ""
    @State private var cart: [PosLine] = []
    @State private var initialized = false

    private var currency: String {
        let code = (model.currency ?? "").trimmingCharacters(in: .whitespaces)
        return code.isEmpty ? "INR" : code
    }

    private var menuDishes: [OperatingMasterPayload] {
        (model.masters ?? [])
            .filter { $0.kind.uppercased() == "MENU_DISH" }
            .sorted { $0.name.lowercased() < $1.name.lowercased() }
    }

    private var channels: [String] {
        let live = (model.life?.channels ?? []).filter { !$0.trimmingCharacters(in: .whitespaces).isEmpty }
        return live.isEmpty ? ["In-dining", "Cloud", "Takeaway"] : live
    }

    private var subtotal: Decimal {
        cart.reduce(Decimal(0)) { $0 + $1.amount }
    }

    private var subtotalText: String {
        RestaurantFormat.exact("\(subtotal)", currency: model.currency, visible: true)
    }

    private var postable: Bool {
        !cart.isEmpty && cart.allSatisfy { (Decimal(string: $0.unitPrice) ?? 0) > 0 }
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                EmberSheetGrabber()
                EmberSheetHeader(
                    title: "POS Bill",
                    subtitle: [model.title, "\(cart.count) line\(cart.count == 1 ? "" : "s")"]
                        .compactMap { $0 }
                        .filter { !$0.isEmpty }
                        .joined(separator: " · "),
                    onClose: onDismiss
                )

                EmberFieldLabel("Mode")
                HStack(spacing: 6) {
                    ForEach(Array(channels.prefix(3).enumerated()), id: \.offset) { _, channel in
                        EmberModeChip(text: channel, selected: channel == mode) { mode = channel }
                    }
                    Spacer(minLength: 0)
                }

                HStack(spacing: 6) {
                    ForEach(["Table", "Covers", "KOT"], id: \.self) { LockedChip(text: $0) }
                    Spacer(minLength: 0)
                }

                EmberFieldLabel("Add from menu")
                HStack(spacing: 8) {
                    EmberTextField(
                        value: $dishQuery,
                        placeholder: menuDishes.isEmpty
                            ? "No saved dish — add one under Menu"
                            : "Search menu"
                    )
                    Button {
                        guard let match = bestMatch else { return }
                        if !cart.contains(where: { $0.masterId == match.id }) {
                            cart.append(PosLine.of(match))
                        }
                        dishQuery = ""
                    } label: {
                        Text("Add")
                            .font(.plusJakarta(size: 12, weight: .bold))
                            .foregroundStyle(menuDishes.isEmpty ? RestaurantEmber.textFaint : .white)
                            .padding(.horizontal, 14)
                            .padding(.vertical, 12)
                            .background(menuDishes.isEmpty ? RestaurantEmber.chip : RestaurantEmber.ember)
                            .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                    }
                    .buttonStyle(.plain)
                    .disabled(menuDishes.isEmpty)
                }

                if menuDishes.isEmpty {
                    EmberEmpty(
                        title: "Menu not set up",
                        body: "Add dishes under Menu first. A bill line needs a real dish name to post."
                    )
                } else if !dishQuery.trimmingCharacters(in: .whitespaces).isEmpty {
                    let matches = menuDishes
                        .filter { $0.name.localizedCaseInsensitiveContains(dishQuery.trimmingCharacters(in: .whitespaces)) }
                        .prefix(4)
                    if !matches.isEmpty {
                        VStack(spacing: 6) {
                            ForEach(matches) { dish in
                                Button {
                                    if !cart.contains(where: { $0.masterId == dish.id }) {
                                        cart.append(PosLine.of(dish))
                                    }
                                    dishQuery = ""
                                } label: {
                                    HStack(spacing: 8) {
                                        Text(dish.name)
                                            .font(.plusJakarta(size: 13, weight: .medium))
                                            .foregroundStyle(RestaurantEmber.textPrimary)
                                            .lineLimit(1)
                                            .frame(maxWidth: .infinity, alignment: .leading)
                                        if let rate = dish.gstRate, !rate.isEmpty {
                                            EmberSeverityChip(label: "GST \(rate)%", color: RestaurantEmber.gold, bordered: false)
                                        }
                                    }
                                    .padding(.horizontal, 12)
                                    .padding(.vertical, 9)
                                    .background(RestaurantEmber.surfaceWell)
                                    .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                                }
                                .buttonStyle(.plain)
                            }
                        }
                    }
                }

                HStack {
                    Text("Table ticket")
                        .font(.plusJakarta(size: 13, weight: .bold))
                        .foregroundStyle(RestaurantEmber.gold)
                    Spacer()
                    Text(cart.isEmpty ? "No items yet" : "\(cart.count) of 12 lines")
                        .font(.plusJakarta(size: 11))
                        .foregroundStyle(RestaurantEmber.textBody)
                }

                if cart.isEmpty {
                    EmberEmpty(
                        title: "Nothing on the bill",
                        body: "Pick a saved dish above, then type its price. Every line posts as an itemized sale line."
                    )
                } else {
                    VStack(spacing: 0) {
                        ForEach(Array(cart.enumerated()), id: \.element.masterId) { index, line in
                            PosLineRow(
                                line: line,
                                currency: currency,
                                onPrice: { cart[index].unitPrice = $0 },
                                onDecrement: {
                                    if cart[index].quantity <= 1 {
                                        cart.remove(at: index)
                                    } else {
                                        cart[index].quantity -= 1
                                    }
                                },
                                onIncrement: {
                                    if cart[index].quantity < 99 { cart[index].quantity += 1 }
                                },
                                onRemove: { cart.remove(at: index) }
                            )
                        }
                    }
                    .padding(.horizontal, 12)
                    .background(RestaurantEmber.chip)
                    .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))

                    if !postable {
                        Text("Every line needs a price above ₹0 before the bill can post.")
                            .font(.plusJakarta(size: 10))
                            .foregroundStyle(RestaurantEmber.warning)
                    }
                }

                totalsBlock

                EmberFieldLabel("Tender")
                HStack(spacing: 6) {
                    ForEach(["UPI", "Cash", "Card"], id: \.self) { option in
                        EmberModeChip(text: option, selected: option == tender) { tender = option }
                    }
                    LockedChip(text: "Split")
                    Spacer(minLength: 0)
                }
                Text("One bill carries one payment state — a split tender has nowhere to land.")
                    .font(.plusJakarta(size: 10))
                    .foregroundStyle(RestaurantEmber.textFaint)

                EmberLockedField(
                    label: "Table · covers",
                    value: "Not captured",
                    reason: "The strict contract has no table, section or covers field — the bill still posts."
                )

                EmberFieldLabel("Guest")
                EmberTextField(value: $guest, placeholder: "Walk-in or customer name")
                EmberFieldLabel("Note")
                EmberTextField(value: $note, placeholder: "Course, kitchen note, reference")

                EmberWhyNote(
                    body: "In-dining POS posts one bill + one tender. Prep status never posts revenue. "
                        + "Recipe stock deduction is excluded.",
                    tinted: true
                )

                EmberPrimaryButton(label: postable ? "Post bill · \(subtotalText)" : "Post bill", enabled: postable) {
                    submit()
                }
                Text("Draft preserved if you close or open a picker")
                    .font(.plusJakarta(size: 10))
                    .foregroundStyle(RestaurantEmber.textFaint)
                Spacer(minLength: 16)
            }
            .padding(.horizontal, RestaurantEmber.gutter)
            .padding(.vertical, 12)
        }
        .onAppear {
            guard !initialized else { return }
            initialized = true
            mode = channels.first ?? "In-dining"
        }
    }

    private var totalsBlock: some View {
        VStack(spacing: 8) {
            EmberTotalRow(label: "Subtotal", value: cart.isEmpty ? "—" : subtotalText)
            EmberTotalRow(
                label: "GST",
                value: "Unavailable",
                valueColor: RestaurantEmber.warning,
                caption: "Stamped server-side from each dish's rate"
            )
            EmberTotalRow(
                label: "Service",
                value: "Unavailable",
                valueColor: RestaurantEmber.warning,
                caption: "No service-charge field on the contract"
            )
            Rectangle().fill(RestaurantEmber.divider).frame(height: 1)
            HStack {
                VStack(alignment: .leading, spacing: 1) {
                    Text("Payable now")
                        .font(.plusJakarta(size: 10))
                        .foregroundStyle(RestaurantEmber.textBody)
                    Text(cart.isEmpty ? "—" : subtotalText)
                        .font(.plusJakarta(size: 26, weight: .bold))
                        .foregroundStyle(RestaurantEmber.textPrimary)
                        .lineLimit(1)
                        .minimumScaleFactor(0.7)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                VStack(alignment: .trailing, spacing: 1) {
                    Text("Tender")
                        .font(.plusJakarta(size: 10))
                        .foregroundStyle(RestaurantEmber.textBody)
                    Text(tender)
                        .font(.plusJakarta(size: 13, weight: .bold))
                        .foregroundStyle(RestaurantEmber.gold)
                }
            }
        }
        .padding(12)
        .background(Color.black.opacity(0.24))
        .overlay(
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .stroke(Color.white.opacity(0.06), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
    }

    private var bestMatch: OperatingMasterPayload? {
        let typed = dishQuery.trimmingCharacters(in: .whitespaces)
        return menuDishes.first { $0.name.compare(typed, options: .caseInsensitive) == .orderedSame }
            ?? menuDishes.first { !typed.isEmpty && $0.name.localizedCaseInsensitiveContains(typed) }
            ?? menuDishes.first
    }

    private func submit() {
        guard postable else { return }
        let amount = "\(subtotal)"
        let lineItems: [(itemName: String, quantity: String?, unit: String?, unitPrice: String?, masterId: String?)] =
            cart.map { line in
                (
                    itemName: line.name,
                    quantity: "\(line.quantity)" as String?,
                    unit: line.unit as String?,
                    unitPrice: line.unitPrice as String?,
                    masterId: line.masterId as String?
                )
            }
        var contract = OperatingContractInput()
        contract.entryMode = "ITEMIZED"
        contract.paymentState = "RECEIVED"
        contract.receivedAmount = amount
        contract.taxMode = "NOT_CONFIGURED"

        onSubmit(
            RestaurantEventDraft(
                eventType: "SALE",
                title: guest.trimmingCharacters(in: .whitespaces).isEmpty
                    ? "Bill · \(cart.count) item\(cart.count == 1 ? "" : "s")"
                    : "Bill · \(guest)",
                amount: amount,
                note: note.isEmpty ? nil : note,
                channelName: mode,
                counterpartyName: guest.isEmpty ? nil : guest,
                paymentStatus: "COLLECTED",
                contract: contract,
                lineItems: lineItems
            )
        )
    }
}

// MARK: - Rows

private struct LockedChip: View {
    let text: String

    var body: some View {
        Text(text)
            .font(.plusJakarta(size: 12, weight: .medium))
            .foregroundStyle(RestaurantEmber.textFaint)
            .lineLimit(1)
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
            .background(RestaurantEmber.chip.opacity(0.5))
            .overlay(
                Capsule().stroke(RestaurantEmber.hairline.opacity(0.5), lineWidth: 1)
            )
            .clipShape(Capsule())
    }
}

private struct PosLineRow: View {
    let line: PosLine
    let currency: String
    var onPrice: (String) -> Void
    var onDecrement: () -> Void
    var onIncrement: () -> Void
    var onRemove: () -> Void

    @State private var priceDraft: String = ""

    var body: some View {
        HStack(spacing: 8) {
            VStack(alignment: .leading, spacing: 3) {
                Text(line.name)
                    .font(.plusJakarta(size: 13, weight: .bold))
                    .foregroundStyle(RestaurantEmber.textPrimary)
                    .lineLimit(1)
                HStack(spacing: 6) {
                    Text(line.unit)
                        .font(.plusJakarta(size: 10))
                        .foregroundStyle(RestaurantEmber.textBody)
                    TextField("Price", text: $priceDraft)
                        .keyboardType(.decimalPad)
                        .font(.plusJakarta(size: 12))
                        .foregroundStyle(RestaurantEmber.textPrimary)
                        .frame(width: 64)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 5)
                        .background(RestaurantEmber.surfaceWell)
                        .overlay(
                            RoundedRectangle(cornerRadius: 8, style: .continuous)
                                .stroke(RestaurantEmber.hairline.opacity(0.6), lineWidth: 1)
                        )
                        .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
                        .onChange(of: priceDraft) { _, newValue in
                            let cleaned = String(newValue.filter { $0.isNumber || $0 == "." }.prefix(9))
                            if cleaned != newValue { priceDraft = cleaned }
                            onPrice(cleaned)
                        }
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            Button(action: onDecrement) {
                Text("−")
                    .font(.plusJakarta(size: 15, weight: .bold))
                    .foregroundStyle(RestaurantEmber.textPrimary)
                    .frame(width: 28, height: 28)
                    .background(RestaurantEmber.chip)
                    .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
            }
            .buttonStyle(.plain)

            Text("\(line.quantity)")
                .font(.plusJakarta(size: 13, weight: .bold))
                .foregroundStyle(RestaurantEmber.textPrimary)

            Button(action: onIncrement) {
                Text("+")
                    .font(.plusJakarta(size: 15, weight: .bold))
                    .foregroundStyle(RestaurantEmber.textPrimary)
                    .frame(width: 28, height: 28)
                    .background(RestaurantEmber.chip)
                    .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
            }
            .buttonStyle(.plain)

            Text(RestaurantFormat.exact("\(line.amount)", currency: currency, visible: true))
                .font(.plusJakarta(size: 13, weight: .bold))
                .foregroundStyle(RestaurantEmber.textPrimary)
                .lineLimit(1)

            Button(action: onRemove) {
                Text("✕")
                    .font(.plusJakarta(size: 11))
                    .foregroundStyle(RestaurantEmber.textFaint)
                    .padding(4)
            }
            .buttonStyle(.plain)
        }
        .padding(.vertical, 9)
        .onAppear { priceDraft = line.unitPrice }
    }
}

private struct PosLine: Identifiable {
    let masterId: String
    let name: String
    let unit: String
    var unitPrice: String
    var quantity: Int

    var id: String { masterId }

    var amount: Decimal {
        let price = Decimal(string: unitPrice) ?? 0
        let raw = price * Decimal(quantity)
        var rounded = Decimal()
        var value = raw
        NSDecimalRound(&rounded, &value, 2, .plain)
        return rounded
    }

    /// Dish masters carry no price, so the ticket line starts blank and is filled in.
    static func of(_ master: OperatingMasterPayload) -> PosLine {
        PosLine(
            masterId: master.id,
            name: master.name,
            unit: (master.unit?.isEmpty == false) ? master.unit! : "plate",
            unitPrice: "",
            quantity: 1
        )
    }
}

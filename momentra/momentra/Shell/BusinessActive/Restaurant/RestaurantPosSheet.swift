import SwiftUI

/// POS bill — Figma `RST-QA-01-ALT · POS · In-dining / Cloud / Takeaway`.
/// Port of Android `RestaurantPosSheet`. Posts itemized SALE with table/covers on contract.
/// GST / Service / Split stay Unavailable (no tax engine / split tender / PDF reprint).
struct RestaurantPosSheet: View {
    let theme: OperatingTheme
    @ObservedObject var model: BusinessOperatingModel

    @Environment(\.dismiss) private var dismiss
    @State private var mode = "In-dining"
    @State private var tender = "UPI"
    @State private var dishQuery = ""
    @State private var guest = ""
    @State private var note = ""
    @State private var tableNumber = ""
    @State private var covers = ""
    @State private var parentKotId: String?
    @State private var cart: [PosLine] = []
    @State private var saving = false

    private var currency: String { model.currency }

    private var menuDishes: [OperatingMasterPayload] {
        let source = model.masters.isEmpty ? (model.projection?.masters ?? []) : model.masters
        return source
            .filter { ["MENU_DISH", "PRODUCT", "ITEM", "SKU"].contains($0.kind.uppercased()) }
            .sorted { $0.name.localizedCaseInsensitiveCompare($1.name) == .orderedAscending }
    }

    private var channels: [String] {
        let fromLife = model.projection?.life?.channels.filter { !$0.isEmpty } ?? []
        return fromLife.isEmpty ? ["In-dining", "Cloud", "Takeaway"] : Array(fromLife.prefix(3))
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

    private var subtotal: Decimal {
        cart.reduce(Decimal.zero) { $0 + $1.amount }
    }

    private var subtotalText: String {
        guard !cart.isEmpty else { return "—" }
        return OperatingV12Money.amount(NSDecimalNumber(decimal: subtotal).stringValue, currency: currency)
    }

    private var postable: Bool {
        !cart.isEmpty && cart.allSatisfy { ($0.unitPrice as NSString).doubleValue > 0 }
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                header
                fieldLabel("Mode")
                chipRow(channels, selection: $mode)

                HStack(spacing: 8) {
                    VStack(alignment: .leading, spacing: 4) {
                        fieldLabel("Table")
                        textField("12", text: $tableNumber)
                    }
                    VStack(alignment: .leading, spacing: 4) {
                        fieldLabel("Covers")
                        textField("4", text: $covers)
                    }
                }

                if !openKots.isEmpty {
                    fieldLabel("Bill from KOT")
                    VStack(spacing: 6) {
                        ForEach(Array(openKots.prefix(4))) { ticket in
                            Button {
                                if parentKotId == ticket.id {
                                    parentKotId = nil
                                } else {
                                    parentKotId = ticket.id
                                    applyKot(ticket)
                                }
                            } label: {
                                HStack {
                                    VStack(alignment: .leading, spacing: 2) {
                                        Text(ticket.title.isEmpty ? "Kitchen ticket" : ticket.title)
                                            .font(.plusJakarta(size: 13, weight: .semibold))
                                            .foregroundStyle(OperatingV12.title)
                                            .lineLimit(1)
                                        Text(kotMeta(ticket))
                                            .font(.plusJakarta(size: 10))
                                            .foregroundStyle(OperatingV12.subtitle)
                                            .lineLimit(1)
                                    }
                                    Spacer()
                                    Text((ticket.status ?? "PLACED").uppercased())
                                        .font(.plusJakarta(size: 10, weight: .bold))
                                        .foregroundStyle(theme.accent)
                                }
                                .padding(.horizontal, 12)
                                .padding(.vertical, 9)
                                .background(parentKotId == ticket.id ? theme.accent.opacity(0.16) : theme.inset)
                                .overlay(
                                    RoundedRectangle(cornerRadius: 10, style: .continuous)
                                        .strokeBorder(
                                            parentKotId == ticket.id ? theme.accent : theme.border.opacity(0.4),
                                            lineWidth: 1
                                        )
                                )
                                .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                            }
                            .buttonStyle(.plain)
                        }
                    }
                }

                fieldLabel("Add from menu")
                HStack(spacing: 8) {
                    textField(
                        menuDishes.isEmpty ? "No saved dish — add one under Menu" : "Search menu",
                        text: $dishQuery
                    )
                    Button(action: addDish) {
                        Text("Add")
                            .font(.plusJakarta(size: 13, weight: .bold))
                            .foregroundStyle(menuDishes.isEmpty ? OperatingV12.subtitle : theme.onAccent)
                            .padding(.horizontal, 14)
                            .padding(.vertical, 12)
                            .background(menuDishes.isEmpty ? theme.inset : theme.accent)
                            .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                    }
                    .buttonStyle(.plain)
                    .disabled(menuDishes.isEmpty)
                }

                if menuDishes.isEmpty {
                    emptyCard(
                        "Menu not set up",
                        "Add dishes under Menu first. A bill line needs a real dish name to post."
                    )
                } else if !dishQuery.trimmingCharacters(in: .whitespaces).isEmpty {
                    dishMatches
                }

                HStack {
                    Text("Table ticket")
                        .font(.plusJakarta(size: 13, weight: .bold))
                        .foregroundStyle(theme.accent)
                    Spacer()
                    Text(cart.isEmpty ? "No items yet" : "\(cart.count) of 12 lines")
                        .font(.plusJakarta(size: 11))
                        .foregroundStyle(OperatingV12.subtitle)
                }

                if cart.isEmpty {
                    emptyCard(
                        "Nothing on the bill",
                        "Pick a saved dish above, then confirm its price. Every line posts as an itemized sale line."
                    )
                } else {
                    cartList
                    if !postable {
                        Text("Every line needs a price above ₹0 before the bill can post.")
                            .font(.plusJakarta(size: 10))
                            .foregroundStyle(theme.warning)
                    }
                }

                totalsCard

                fieldLabel("Tender")
                HStack(spacing: 6) {
                    ForEach(["UPI", "Cash", "Card"], id: \.self) { option in
                        Button { tender = option } label: {
                            Text(option)
                                .font(.plusJakarta(size: 12, weight: .semibold))
                                .foregroundStyle(tender == option ? theme.onAccent : OperatingV12.subtitle)
                                .padding(.horizontal, 12)
                                .padding(.vertical, 8)
                                .background(tender == option ? theme.accent : theme.inset)
                                .clipShape(Capsule())
                        }
                        .buttonStyle(.plain)
                    }
                    Text("Split")
                        .font(.plusJakarta(size: 12, weight: .medium))
                        .foregroundStyle(OperatingV12.subtitle.opacity(0.7))
                        .padding(.horizontal, 12)
                        .padding(.vertical, 8)
                        .background(theme.inset.opacity(0.5))
                        .overlay(Capsule().strokeBorder(theme.border.opacity(0.5), lineWidth: 1))
                        .clipShape(Capsule())
                    Spacer(minLength: 0)
                }
                Text("One bill carries one payment state — a split tender has nowhere to land.")
                    .font(.plusJakarta(size: 10))
                    .foregroundStyle(OperatingV12.subtitle)

                fieldLabel("Guest")
                textField("Walk-in or customer name", text: $guest)
                fieldLabel("Note")
                textField("Course, kitchen note, reference", text: $note)

                whyNote(
                    "In-dining POS posts one bill + one tender. Prep status never posts revenue. Recipe stock deduction is excluded."
                )

                Button {
                    Task { await postBill() }
                } label: {
                    Text(saving ? "Posting…" : (postable ? "Post bill · \(subtotalText)" : "Post bill"))
                        .font(.plusJakarta(size: 15, weight: .bold))
                        .foregroundStyle(theme.onAccent)
                        .frame(maxWidth: .infinity)
                        .frame(height: 52)
                        .background(postable && !saving ? theme.accent : theme.accent.opacity(0.4))
                        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                }
                .buttonStyle(.plain)
                .disabled(!postable || saving)

                Text("Draft preserved if you close or open a picker")
                    .font(.plusJakarta(size: 10))
                    .foregroundStyle(OperatingV12.subtitle)
            }
            .padding(16)
            .padding(.bottom, 24)
        }
        .background(theme.canvas)
        .task {
            if mode.isEmpty || !channels.contains(mode) {
                mode = channels.first ?? "In-dining"
            }
            if model.masters.isEmpty {
                await model.loadMasters()
            }
        }
    }

    private var header: some View {
        HStack {
            VStack(alignment: .leading, spacing: 4) {
                Text("POS Bill")
                    .font(.plusJakarta(size: 22, weight: .semibold))
                    .foregroundStyle(OperatingV12.title)
                Text("\(model.title) · \(cart.count) line\(cart.count == 1 ? "" : "s")")
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(OperatingV12.subtitle)
            }
            Spacer()
            Button("Close") { dismiss() }
                .font(.plusJakarta(size: 13, weight: .semibold))
                .foregroundStyle(OperatingV12.subtitle)
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
                            Text(dish.name)
                                .font(.plusJakarta(size: 13, weight: .semibold))
                                .foregroundStyle(OperatingV12.title)
                                .lineLimit(1)
                            Spacer()
                            if let price = dish.sellPrice, !price.isEmpty {
                                Text(OperatingV12Money.amount(price, currency: currency))
                                    .font(.plusJakarta(size: 10, weight: .bold))
                                    .foregroundStyle(theme.accent)
                            }
                            if let gst = dish.gstRate, !gst.isEmpty {
                                Text("GST \(gst)%")
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

    private var cartList: some View {
        VStack(spacing: 0) {
            ForEach(Array(cart.enumerated()), id: \.element.id) { index, line in
                HStack(alignment: .top, spacing: 8) {
                    VStack(alignment: .leading, spacing: 4) {
                        Text(line.name)
                            .font(.plusJakarta(size: 13, weight: .semibold))
                            .foregroundStyle(OperatingV12.title)
                        HStack(spacing: 8) {
                            stepper(index: index)
                            TextField("Price", text: bindingPrice(at: index))
                                .keyboardType(.decimalPad)
                                .font(.plusJakarta(size: 12))
                                .foregroundStyle(OperatingV12.title)
                                .padding(.horizontal, 8)
                                .padding(.vertical, 6)
                                .frame(width: 88)
                                .background(theme.canvas)
                                .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
                        }
                    }
                    Spacer(minLength: 0)
                    VStack(alignment: .trailing, spacing: 4) {
                        Text(lineAmount(line))
                            .font(.plusJakarta(size: 13, weight: .bold))
                            .foregroundStyle(OperatingV12.title)
                        Button("Remove") {
                            cart.remove(at: index)
                        }
                        .font(.plusJakarta(size: 10, weight: .semibold))
                        .foregroundStyle(theme.warning)
                    }
                }
                .padding(.vertical, 9)
                if index < cart.count - 1 {
                    Divider().overlay(theme.border.opacity(0.4))
                }
            }
        }
        .padding(.horizontal, 12)
        .background(theme.inset)
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
    }

    private var totalsCard: some View {
        VStack(spacing: 8) {
            totalRow("Subtotal", cart.isEmpty ? "—" : subtotalText)
            totalRow("GST", "Unavailable", caption: "Stamped server-side from each dish's rate", warning: true)
            totalRow("Service", "Unavailable", caption: "No service-charge field on the contract", warning: true)
            Divider().overlay(theme.border.opacity(0.4))
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text("Payable now")
                        .font(.plusJakarta(size: 10))
                        .foregroundStyle(OperatingV12.subtitle)
                    Text(cart.isEmpty ? "—" : subtotalText)
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
                        .foregroundStyle(theme.accent)
                }
            }
        }
        .padding(12)
        .background(Color.black.opacity(0.24))
        .overlay(
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .strokeBorder(Color.white.opacity(0.06), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
    }

    private func stepper(index: Int) -> some View {
        HStack(spacing: 6) {
            Button {
                guard cart.indices.contains(index) else { return }
                if cart[index].quantity <= 1 {
                    cart.remove(at: index)
                } else {
                    cart[index].quantity -= 1
                }
            } label: {
                Image(systemName: "minus")
                    .font(.system(size: 11, weight: .bold))
                    .frame(width: 24, height: 24)
                    .background(theme.canvas)
                    .clipShape(RoundedRectangle(cornerRadius: 6))
            }
            .buttonStyle(.plain)
            Text("\(cart.indices.contains(index) ? cart[index].quantity : 0)")
                .font(.plusJakarta(size: 12, weight: .bold))
                .foregroundStyle(OperatingV12.title)
                .frame(minWidth: 16)
            Button {
                guard cart.indices.contains(index), cart[index].quantity < 99 else { return }
                cart[index].quantity += 1
            } label: {
                Image(systemName: "plus")
                    .font(.system(size: 11, weight: .bold))
                    .frame(width: 24, height: 24)
                    .background(theme.canvas)
                    .clipShape(RoundedRectangle(cornerRadius: 6))
            }
            .buttonStyle(.plain)
        }
        .foregroundStyle(theme.accent)
    }

    private func applyKot(_ ticket: OperatingActivityPayload) {
        if let channel = ticket.channelName, channels.contains(channel) {
            mode = channel
        }
        if let table = ticket.tableNumber, !table.isEmpty { tableNumber = table }
        if let cover = ticket.covers ?? ticket.quantity, !cover.isEmpty { covers = cover }
        if guest.trimmingCharacters(in: .whitespaces).isEmpty,
           let name = ticket.counterpartyName, !name.isEmpty {
            guest = name
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
            ?? menuDishes.first
        if let match { add(match) }
        dishQuery = ""
    }

    private func add(_ dish: OperatingMasterPayload) {
        guard cart.count < 12, !cart.contains(where: { $0.masterId == dish.id }) else { return }
        cart.append(PosLine.of(dish))
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

    private func lineAmount(_ line: PosLine) -> String {
        OperatingV12Money.amount(NSDecimalNumber(decimal: line.amount).stringValue, currency: currency)
    }

    private func postBill() async {
        guard postable else { return }
        saving = true
        defer { saving = false }
        let table = tableNumber.trimmingCharacters(in: .whitespaces)
        let coverCount = covers.trimmingCharacters(in: .whitespaces)
        let guestName = guest.trimmingCharacters(in: .whitespaces)
        var contract = OperatingContractInput()
        contract.entryMode = "ITEMIZED"
        contract.paymentState = "RECEIVED"
        contract.receivedAmount = NSDecimalNumber(decimal: subtotal).stringValue
        contract.taxMode = "NOT_CONFIGURED"
        contract.tableNumber = table.isEmpty ? nil : table
        contract.covers = coverCount.isEmpty ? nil : coverCount
        let noteBits = [
            "Tender: \(tender)",
            note.trimmingCharacters(in: .whitespaces).isEmpty ? nil : note,
        ].compactMap { $0 }
        let title: String = {
            if !guestName.isEmpty { return "Bill · \(guestName)" }
            if !table.isEmpty { return "Bill · Table \(table)" }
            return "Bill · \(cart.count) item\(cart.count == 1 ? "" : "s")"
        }()
        let ok = await model.record(
            OperatingEventInput(
                momentId: model.momentId,
                eventType: "SALE",
                title: title,
                amount: NSDecimalNumber(decimal: subtotal).stringValue,
                note: noteBits.joined(separator: " · "),
                channelName: mode,
                counterpartyName: guestName.isEmpty ? nil : guestName,
                parentEventId: parentKotId,
                quantity: coverCount.isEmpty ? nil : coverCount,
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
        )
        if ok { dismiss() }
    }

    // MARK: - Chrome helpers

    private func fieldLabel(_ text: String) -> some View {
        Text(text.uppercased())
            .font(.plusJakarta(size: 10, weight: .semibold))
            .tracking(0.4)
            .foregroundStyle(OperatingV12.subtitle)
    }

    private func textField(_ placeholder: String, text: Binding<String>) -> some View {
        TextField(placeholder, text: text)
            .font(.plusJakarta(size: 14))
            .foregroundStyle(OperatingV12.title)
            .padding(.horizontal, 12)
            .padding(.vertical, 12)
            .background(theme.inset)
            .overlay(
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .strokeBorder(theme.border.opacity(0.6), lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
    }

    private func chipRow(_ options: [String], selection: Binding<String>) -> some View {
        HStack(spacing: 6) {
            ForEach(options, id: \.self) { option in
                Button {
                    selection.wrappedValue = option
                } label: {
                    Text(option)
                        .font(.plusJakarta(size: 12, weight: .semibold))
                        .foregroundStyle(selection.wrappedValue == option ? theme.onAccent : OperatingV12.subtitle)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 8)
                        .background(selection.wrappedValue == option ? theme.accent : theme.inset)
                        .clipShape(Capsule())
                }
                .buttonStyle(.plain)
            }
            Spacer(minLength: 0)
        }
    }

    private func emptyCard(_ title: String, _ body: String) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(title)
                .font(.plusJakarta(size: 13, weight: .bold))
                .foregroundStyle(OperatingV12.title)
            Text(body)
                .font(.plusJakarta(size: 12))
                .foregroundStyle(OperatingV12.subtitle)
        }
        .padding(12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(theme.inset)
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
    }

    private func totalRow(_ label: String, _ value: String, caption: String? = nil, warning: Bool = false) -> some View {
        HStack {
            VStack(alignment: .leading, spacing: 1) {
                Text(label)
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(OperatingV12.subtitle)
                if let caption {
                    Text(caption)
                        .font(.plusJakarta(size: 10))
                        .foregroundStyle(OperatingV12.subtitle.opacity(0.8))
                }
            }
            Spacer()
            Text(value)
                .font(.plusJakarta(size: 14, weight: .semibold))
                .foregroundStyle(warning ? theme.warning : OperatingV12.title)
        }
    }

    private func whyNote(_ text: String) -> some View {
        Text(text)
            .font(.plusJakarta(size: 11, weight: .medium))
            .foregroundStyle(Color(hex: "#CBD5E1"))
            .padding(12)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(theme.accent.opacity(0.08))
            .overlay(
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .strokeBorder(theme.accent.opacity(0.3), lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
    }
}

private extension String {
    func ifEmpty(_ fallback: String) -> String { isEmpty ? fallback : self }
}

private struct PosLine: Identifiable, Equatable {
    let id: String
    let masterId: String?
    let name: String
    let unit: String?
    var quantity: Int
    var unitPrice: String

    var amount: Decimal {
        let price = Decimal(string: unitPrice) ?? 0
        return price * Decimal(quantity)
    }

    static func of(_ dish: OperatingMasterPayload) -> PosLine {
        PosLine(
            id: dish.id,
            masterId: dish.id,
            name: dish.name,
            unit: dish.unit ?? "pc",
            quantity: 1,
            unitPrice: dish.sellPrice ?? ""
        )
    }
}

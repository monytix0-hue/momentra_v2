import SwiftUI

/// Retail utilities — Figma RTL-QA Hold / Transfer / Reprint.
struct RetailUtilitySheet: View {
    let theme: OperatingTheme
    let mode: String
    @ObservedObject var model: BusinessOperatingModel
    @State private var activeMode: String = ""

    private var current: String {
        let raw = activeMode.isEmpty ? mode : activeMode
        return raw.trimmingCharacters(in: .whitespacesAndNewlines).uppercased()
    }

    var body: some View {
        Group {
            switch current {
            case OperatingCaptureLayout.transferMode:
                RetailTransferSheet(theme: theme, model: model, onNavigate: navigate)
            case OperatingCaptureLayout.reprintMode:
                RetailReprintSheet(theme: theme, model: model, onNavigate: navigate)
            default:
                RetailHoldParkSheet(theme: theme, model: model, onNavigate: navigate)
            }
        }
    }

    private func navigate(_ dest: String) {
        activeMode = dest
    }
}

// MARK: - Shared chrome helpers (utility sheets only)

private enum RetailUtilityNav {
    static let hold = OperatingCaptureLayout.holdMode
    static let transfer = OperatingCaptureLayout.transferMode
    static let reprint = OperatingCaptureLayout.reprintMode
}

private struct RetailUtilityCrossNav: View {
    let theme: OperatingTheme
    let active: String
    let onNavigate: (String) -> Void

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 6) {
                chip("Hold", RetailUtilityNav.hold)
                chip("Transfer", RetailUtilityNav.transfer)
                chip("Reprint", RetailUtilityNav.reprint)
            }
        }
    }

    private func chip(_ label: String, _ dest: String) -> some View {
        RoseModeChip(theme: theme, text: label, selected: active == dest) {
            if active != dest { onNavigate(dest) }
        }
    }
}

private struct RetailRegisterChips: View {
    let theme: OperatingTheme
    let options: [String]
    @Binding var selection: String

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 6) {
                ForEach(options, id: \.self) { option in
                    RoseModeChip(theme: theme, text: option, selected: selection == option) {
                        selection = option
                    }
                }
            }
        }
    }
}

private struct RetailUtilityMetaRows: View {
    let theme: OperatingTheme
    let rows: [(String, String)]

    var body: some View {
        VStack(spacing: 8) {
            ForEach(Array(rows.enumerated()), id: \.offset) { _, row in
                HStack {
                    Text(row.0)
                        .font(.plusJakarta(size: 10, weight: .semibold))
                        .foregroundStyle(RoseSheetChrome.label)
                    Spacer(minLength: 8)
                    Text(row.1)
                        .font(.plusJakarta(size: 12, weight: .semibold))
                        .foregroundStyle(OperatingV12.title)
                        .lineLimit(1)
                }
            }
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 8)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(theme.inset)
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
    }
}

private struct RetailUtilityLineRow: View {
    let theme: OperatingTheme
    let title: String
    let qty: Int
    let meta: String
    let category: String
    let status: String
    var onDecrement: (() -> Void)? = nil
    var onIncrement: (() -> Void)? = nil
    var onRemove: (() -> Void)? = nil

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(alignment: .center, spacing: 8) {
                Text(title)
                    .font(.plusJakarta(size: 13, weight: .semibold))
                    .foregroundStyle(OperatingV12.title)
                    .lineLimit(1)
                Spacer(minLength: 4)
                if let onDecrement, let onIncrement {
                    RoseStepper(theme: theme, quantity: qty, onDecrement: onDecrement, onIncrement: onIncrement)
                } else {
                    Text("×\(qty)")
                        .font(.plusJakarta(size: 13, weight: .bold))
                        .foregroundStyle(OperatingV12.title)
                }
                if let onRemove {
                    Button(action: onRemove) {
                        Text("✕")
                            .font(.plusJakarta(size: 12))
                            .foregroundStyle(OperatingV12.subtitle)
                    }
                    .buttonStyle(.plain)
                }
            }
            HStack(spacing: 6) {
                Text(meta)
                    .font(.plusJakarta(size: 10))
                    .foregroundStyle(OperatingV12.subtitle)
                    .lineLimit(2)
                Spacer(minLength: 4)
                Text(category)
                    .font(.plusJakarta(size: 11, weight: .semibold))
                    .foregroundStyle(theme.accent)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 6)
                    .background(theme.inset)
                    .clipShape(Capsule())
                Text(status)
                    .font(.plusJakarta(size: 11, weight: .semibold))
                    .foregroundStyle(theme.onAccent)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 6)
                    .background(theme.accent)
                    .clipShape(Capsule())
            }
        }
        .padding(.vertical, 4)
    }
}

private func retailStation(for sku: OperatingMasterPayload?) -> String {
    let raw = sku?.dimension?.trimmingCharacters(in: .whitespacesAndNewlines)
    if let raw, !raw.isEmpty {
        return retailStationLabel(raw)
    }
    return retailStationLabel(sku?.name.split(separator: " ").first.map(String.init) ?? "Staples")
}

private func retailStationLabel(_ raw: String) -> String {
    let lower = raw.lowercased()
    if lower.contains("snack") { return "Snacks" }
    if lower.contains("dairy") || lower.contains("milk") { return "Dairy" }
    if lower.contains("beverage") || lower.contains("drink") { return "Beverages" }
    if lower.contains("personal") || lower.contains("care") { return "Personal care" }
    if lower.contains("home") { return "Home care" }
    return raw.prefix(16).capitalized
}

// MARK: - Hold / park bill (`2508:29333`)

struct RetailHoldParkSheet: View {
    let theme: OperatingTheme
    @ObservedObject var model: BusinessOperatingModel
    var onNavigate: (String) -> Void = { _ in }
    @Environment(\.dismiss) private var dismiss

    @State private var register = ""
    @State private var customer = ""
    @State private var parkNote = ""
    @State private var station = "All lines"
    @State private var query = ""
    @State private var slipTo = "Counter printer 2"
    @State private var parkCart: [RetailPosLine] = []
    @State private var pendingHoldId = UUID().uuidString

    private var registers: [String] {
        let fromLife = model.projection?.life?.channels.filter { !$0.isEmpty } ?? []
        let base = fromLife.isEmpty ? ["Register 1", "Register 2", "Online"] : Array(fromLife.prefix(3))
        return base
    }

    private var skus: [OperatingMasterPayload] {
        let source = model.masters.isEmpty ? (model.projection?.masters ?? []) : model.masters
        return source
            .filter { ["SKU", "PRODUCT", "ITEM", "MATERIAL"].contains($0.kind.uppercased()) }
            .sorted { $0.name.localizedCaseInsensitiveCompare($1.name) == .orderedAscending }
    }

    private var masterById: [String: OperatingMasterPayload] {
        Dictionary(uniqueKeysWithValues: skus.map { ($0.id, $0) })
    }

    private var nextHoldCode: String {
        let short = pendingHoldId.replacingOccurrences(of: "-", with: "").prefix(4).uppercased()
        return short.isEmpty ? "HOLD" : "HOLD-\(short)"
    }

    private var stations: [String] {
        let cats = parkCart.compactMap { line -> String? in
            guard let id = line.masterId else { return nil }
            return retailStation(for: masterById[id])
        }
        let unique = Array(Set(cats)).sorted()
        return ["All lines"] + (unique.isEmpty ? ["Staples", "Snacks", "Dairy", "Beverages"] : unique)
    }

    private var visibleCart: [RetailPosLine] {
        guard station != "All lines" else { return parkCart }
        return parkCart.filter { line in
            guard let id = line.masterId else { return false }
            return retailStation(for: masterById[id]) == station
        }
    }

    private var locationLabel: String {
        model.projection?.location?.trimmingCharacters(in: .whitespacesAndNewlines).nilIfBlank
            ?? model.title
    }

    private var canPark: Bool { !parkCart.isEmpty }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                RoseGrabber()
                RoseSheetHeader(
                    theme: theme,
                    title: "Hold / Park bill",
                    subtitle: "Parked cart · no revenue, no stock",
                    qaCode: "RTL-QA-HOLD",
                    onClose: { dismiss() }
                )

                RetailUtilityCrossNav(theme: theme, active: RetailUtilityNav.hold, onNavigate: onNavigate)
                RetailRegisterChips(theme: theme, options: registers, selection: registerBinding)

                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 6) {
                        RoseModeChip(theme: theme, text: "\(register.isEmpty ? "Register" : register) · Shift open", selected: true) {}
                        RoseModeChip(theme: theme, text: "Park · recall later", selected: false) {}
                        RoseModeChip(
                            theme: theme,
                            text: model.projection?.gstTracking?.uppercased() == "ON" || model.projection?.gstRegistration != nil
                                ? "GST on" : "GST off",
                            selected: false
                        ) {}
                    }
                }

                RoseAmountHero(
                    theme: theme,
                    label: "Hold slip · \(nextHoldCode)",
                    amountText: customer.isEmpty ? "Walk-in" : customer,
                    caption: "\(parkCart.count) line\(parkCart.count == 1 ? "" : "s") · \(locationLabel) · \(register.isEmpty ? "—" : register)"
                )

                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 6) {
                        ForEach(stations, id: \.self) { name in
                            RoseModeChip(theme: theme, text: name, selected: station == name) {
                                station = name
                            }
                        }
                    }
                }

                RoseLinesCard(theme: theme, title: "Parked lines", trailing: "\(parkCart.count)") {
                    RoseScanField(
                        theme: theme,
                        placeholder: skus.isEmpty ? "No SKU — add catalogue first" : "Scan or search SKU",
                        query: $query,
                        actionLabel: "Scan",
                        action: { pickFirstSku() },
                        disabled: skus.isEmpty
                    )
                    skuMatches
                    if visibleCart.isEmpty {
                        RoseEmptyCard(
                            theme: theme,
                            title: "Nothing on the hold slip",
                            bodyText: "Scan Harbor Mart SKUs to park a draft cart."
                        )
                    } else {
                        ForEach(visibleCart) { line in
                            lineRow(line)
                        }
                    }
                }

                RoseSectionCard(theme: theme, label: "Ticket") {
                    RoseFieldLabel(text: "Customer / hold label")
                    RoseTextField(theme: theme, placeholder: "Walk-in · Priya", text: $customer)
                    RoseFieldLabel(text: "Note")
                    RoseTextField(theme: theme, placeholder: "Customer stepped out", text: $parkNote)
                }

                RetailUtilityMetaRows(
                    theme: theme,
                    rows: [
                        ("Slip to", slipTo),
                        ("Cashier", model.projection?.participants?.first ?? "Cashier"),
                        ("Linked bill", "Not posted · draft"),
                    ]
                )

                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 6) {
                        ForEach(["Counter printer 1", "Counter printer 2", "Screen only"], id: \.self) { printer in
                            RoseModeChip(theme: theme, text: printer, selected: slipTo == printer) {
                                slipTo = printer
                            }
                        }
                        RoseLockedChip(theme: theme, label: "Thermal print")
                    }
                }
                Text("Slip to is chrome-only — no print stack.")
                    .font(.plusJakarta(size: 10))
                    .foregroundStyle(OperatingV12.subtitle)

                RoseWhyBox(
                    theme: theme,
                    text: "Hold parks a draft cart. No revenue, no stock, no tender and no GST until Charge on Scan cart."
                )

                RoseStickyFooter(
                    theme: theme,
                    title: canPark ? "Park bill · \(nextHoldCode)" : "Park bill",
                    disabled: !canPark,
                    action: parkBill
                )
            }
            .padding(16)
            .padding(.bottom, 24)
        }
        .background(theme.canvas)
        .task {
            if model.masters.isEmpty { await model.loadMasters() }
            if register.isEmpty {
                register = registers.count > 1 ? registers[1] : (registers.first ?? "Register 1")
            }
        }
    }

    private var registerBinding: Binding<String> {
        Binding(
            get: { register.isEmpty ? (registers.first ?? "") : register },
            set: { register = $0 }
        )
    }

    @ViewBuilder
    private var skuMatches: some View {
        let trimmed = query.trimmingCharacters(in: .whitespacesAndNewlines)
        let matches: [OperatingMasterPayload] = {
            guard !trimmed.isEmpty else { return [] }
            return skus.filter {
                $0.name.localizedCaseInsensitiveContains(trimmed)
                    || ($0.barcode?.localizedCaseInsensitiveContains(trimmed) == true)
                    || ($0.code?.localizedCaseInsensitiveContains(trimmed) == true)
            }
            .prefix(4)
            .map { $0 }
        }()
        if !matches.isEmpty {
            VStack(spacing: 6) {
                ForEach(matches) { sku in
                    Button {
                        addSku(sku)
                    } label: {
                        HStack {
                            Text(sku.name)
                                .font(.plusJakarta(size: 13, weight: .semibold))
                                .foregroundStyle(OperatingV12.title)
                                .lineLimit(1)
                            Spacer()
                            if let price = sku.sellPrice, !price.isEmpty {
                                Text(OperatingV12Money.amount(price, currency: model.currency))
                                    .font(.plusJakarta(size: 11))
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
    private func lineRow(_ line: RetailPosLine) -> some View {
        let master = line.masterId.flatMap { masterById[$0] }
        let cat = retailStation(for: master)
        let metaBits = [
            line.skuCode ?? master?.code ?? master?.barcode,
            line.unitPrice.isEmpty ? nil : OperatingV12Money.amount(line.unitPrice, currency: model.currency),
            line.hsn.map { "HSN \($0)" } ?? master?.hsn.map { "HSN \($0)" },
            master?.gstRate.map { "\($0)%" },
        ].compactMap { $0 }
        RetailUtilityLineRow(
            theme: theme,
            title: line.name,
            qty: line.quantity,
            meta: metaBits.isEmpty ? (line.unit ?? "pc") : metaBits.joined(separator: " · "),
            category: cat,
            status: "Parked",
            onDecrement: {
                updateLine(line) { current in
                    if current.quantity <= 1 {
                        parkCart.removeAll { $0.id == current.id }
                    } else {
                        guard let idx = parkCart.firstIndex(where: { $0.id == current.id }) else { return }
                        parkCart[idx].quantity -= 1
                    }
                }
            },
            onIncrement: {
                updateLine(line) { current in
                    guard let idx = parkCart.firstIndex(where: { $0.id == current.id }) else { return }
                    if parkCart[idx].quantity < 99 { parkCart[idx].quantity += 1 }
                }
            },
            onRemove: { parkCart.removeAll { $0.id == line.id } }
        )
    }

    private func updateLine(_ line: RetailPosLine, _ body: (RetailPosLine) -> Void) {
        body(line)
    }

    private func addSku(_ sku: OperatingMasterPayload) {
        if let idx = parkCart.firstIndex(where: { $0.masterId == sku.id }) {
            if parkCart[idx].quantity < 99 { parkCart[idx].quantity += 1 }
        } else {
            parkCart.append(RetailPosLine.of(sku))
        }
        query = ""
    }

    private func pickFirstSku() {
        let q = query.trimmingCharacters(in: .whitespacesAndNewlines)
        let match = skus.first { $0.name.localizedCaseInsensitiveContains(q) } ?? skus.first
        if let match { addSku(match) }
    }

    private func parkBill() {
        guard canPark else { return }
        let total = parkCart.reduce(Decimal.zero) { $0 + $1.amount }
        let amount = NSDecimalNumber(decimal: total).stringValue
        let channel = register.isEmpty ? "Counter" : register
        let label = customer.isEmpty ? "Parked bill · \(parkCart.count) lines" : "Parked · \(customer)"
        let draft = RetailParkedSale(
            id: pendingHoldId,
            label: label,
            meta: [channel, "Tender UPI"].joined(separator: " · "),
            amount: amount,
            channel: channel,
            customer: customer,
            note: parkNote,
            tender: "UPI",
            lines: parkCart,
            parkedAt: Date()
        )
        RetailParkedSaleStore.shared.park(momentId: model.momentId, draft: draft)
        dismiss()
    }
}

// MARK: - Transfer (`2508:29447`)

struct RetailTransferSheet: View {
    let theme: OperatingTheme
    @ObservedObject var model: BusinessOperatingModel
    var onNavigate: (String) -> Void = { _ in }
    @Environment(\.dismiss) private var dismiss

    @State private var register = ""
    @State private var skuQuery = ""
    @State private var fromId: String?
    @State private var toId: String?
    @State private var station = "All lines"
    @State private var cart: [RetailPosLine] = []
    @State private var note = ""
    @State private var saving = false
    @State private var transferCode = String(format: "TRF-%03d", Int.random(in: 100...999))

    private var registers: [String] {
        let fromLife = model.projection?.life?.channels.filter { !$0.isEmpty } ?? []
        return fromLife.isEmpty ? ["Register 1", "Register 2", "Online"] : Array(fromLife.prefix(3))
    }

    private var skus: [OperatingMasterPayload] {
        model.masters.filter { ["SKU", "PRODUCT", "ITEM", "MATERIAL"].contains($0.kind.uppercased()) }
    }

    private var locations: [OperatingMasterPayload] {
        model.masters.filter { ["WAREHOUSE", "LOCATION", "STORE", "CHANNEL"].contains($0.kind.uppercased()) }
    }

    private var masterById: [String: OperatingMasterPayload] {
        Dictionary(uniqueKeysWithValues: skus.map { ($0.id, $0) })
    }

    private var stations: [String] {
        let cats = cart.compactMap { line -> String? in
            guard let id = line.masterId else { return nil }
            return retailStation(for: masterById[id])
        }
        let unique = Array(Set(cats)).sorted()
        return ["All lines"] + (unique.isEmpty ? ["Staples", "Beverages"] : unique)
    }

    private var visibleCart: [RetailPosLine] {
        guard station != "All lines" else { return cart }
        return cart.filter { line in
            guard let id = line.masterId else { return false }
            return retailStation(for: masterById[id]) == station
        }
    }

    private var totalUnits: Int { cart.reduce(0) { $0 + $1.quantity } }

    private var routeLabel: String {
        let from = locationName(fromId)
        let to = locationName(toId)
        if from.isEmpty || to.isEmpty { return "Pick locations" }
        return "\(from) → \(to)"
    }

    private var canSave: Bool {
        guard let fromId, let toId, fromId != toId else { return false }
        return !cart.isEmpty
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                RoseGrabber()
                RoseSheetHeader(
                    theme: theme,
                    title: "Stock transfer",
                    subtitle: "Move stock between locations · no revenue",
                    qaCode: "RTL-QA-TRANSFER",
                    onClose: { dismiss() }
                )

                RetailUtilityCrossNav(theme: theme, active: RetailUtilityNav.transfer, onNavigate: onNavigate)
                RetailRegisterChips(theme: theme, options: registers, selection: registerBinding)

                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 6) {
                        RoseModeChip(theme: theme, text: routeLabel, selected: true) {}
                        RoseModeChip(theme: theme, text: "Same GSTIN", selected: false) {}
                        RoseModeChip(theme: theme, text: "GST on", selected: false) {}
                    }
                }

                RoseAmountHero(
                    theme: theme,
                    label: "Transfer slip · \(transferCode)",
                    amountText: routeLabel,
                    caption: "\(cart.count) line\(cart.count == 1 ? "" : "s") · dispatch now"
                )

                RoseSectionCard(theme: theme, label: "Route") {
                    if locations.count < 2 {
                        RoseEmptyCard(
                            theme: theme,
                            title: "Need two locations",
                            bodyText: "Add a second location under Masters before transferring."
                        )
                    }
                    RosePickerRow(theme: theme, label: "From location", value: locationName(fromId))
                    locationPicker(selection: $fromId, excluding: toId)
                    RosePickerRow(theme: theme, label: "To location", value: locationName(toId))
                    locationPicker(selection: $toId, excluding: fromId)
                }

                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 6) {
                        ForEach(stations, id: \.self) { name in
                            RoseModeChip(theme: theme, text: name, selected: station == name) {
                                station = name
                            }
                        }
                    }
                }

                RoseLinesCard(theme: theme, title: "Transfer lines", trailing: "\(cart.count)") {
                    RoseScanField(
                        theme: theme,
                        placeholder: skus.isEmpty ? "No SKU — add catalogue first" : "Scan or search SKU",
                        query: $skuQuery,
                        actionLabel: "Scan",
                        action: { pickFirstSku() },
                        disabled: skus.isEmpty
                    )
                    skuChips
                    if visibleCart.isEmpty {
                        RoseEmptyCard(
                            theme: theme,
                            title: "No lines to move",
                            bodyText: "Scan SKUs to build a multi-line transfer."
                        )
                    } else {
                        ForEach(visibleCart) { line in
                            transferLineRow(line)
                        }
                    }
                }

                RetailUtilityMetaRows(
                    theme: theme,
                    rows: [
                        ("Valuation", "At cost · no GST within the same GSTIN"),
                        ("Receiver", locationName(toId).isEmpty ? "—" : locationName(toId)),
                        ("Sale impact", "None"),
                    ]
                )

                RoseFieldLabel(text: "Note")
                RoseTextField(theme: theme, placeholder: "Dispatch note", text: $note)

                RoseWhyBox(
                    theme: theme,
                    text: "Transfer moves stock between locations at cost: − from, + to on confirm. No revenue, no AP, and no GST within the same GSTIN."
                )

                RoseStickyFooter(
                    theme: theme,
                    title: canSave ? "Confirm transfer · \(totalUnits) units" : "Confirm transfer",
                    disabled: !canSave,
                    loading: saving,
                    action: { Task { await post() } }
                )
            }
            .padding(16)
            .padding(.bottom, 24)
        }
        .background(theme.canvas)
        .task {
            if model.masters.isEmpty { await model.loadMasters() }
            if register.isEmpty {
                register = registers.count > 1 ? registers[1] : (registers.first ?? "Register 1")
            }
            if fromId == nil { fromId = locations.first?.id }
            if toId == nil { toId = locations.dropFirst().first?.id }
        }
    }

    private var registerBinding: Binding<String> {
        Binding(
            get: { register.isEmpty ? (registers.first ?? "") : register },
            set: { register = $0 }
        )
    }

    @ViewBuilder
    private var skuChips: some View {
        if !skus.isEmpty {
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(skus.prefix(12)) { sku in
                        RoseModeChip(
                            theme: theme,
                            text: sku.name,
                            selected: cart.contains(where: { $0.masterId == sku.id })
                        ) {
                            addSku(sku)
                        }
                    }
                }
            }
        }
    }

    @ViewBuilder
    private func locationPicker(selection: Binding<String?>, excluding other: String?) -> some View {
        let available = locations.filter { $0.id != other }
        if available.isEmpty {
            Text("Add a second location under Masters before transferring.")
                .font(.plusJakarta(size: 12))
                .foregroundStyle(theme.warning)
        } else {
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(available) { loc in
                        RoseModeChip(theme: theme, text: loc.name, selected: selection.wrappedValue == loc.id) {
                            selection.wrappedValue = selection.wrappedValue == loc.id ? nil : loc.id
                        }
                    }
                }
            }
        }
    }

    @ViewBuilder
    private func transferLineRow(_ line: RetailPosLine) -> some View {
        let master = line.masterId.flatMap { masterById[$0] }
        let cat = retailStation(for: master)
        let code = line.skuCode ?? master?.code ?? master?.barcode ?? "SKU"
        RetailUtilityLineRow(
            theme: theme,
            title: line.name,
            qty: line.quantity,
            meta: "\(code) · \(line.quantity) \(line.unit ?? "pc") · on hand —",
            category: cat,
            status: "Move",
            onDecrement: {
                if line.quantity <= 1 {
                    cart.removeAll { $0.id == line.id }
                } else if let idx = cart.firstIndex(where: { $0.id == line.id }) {
                    cart[idx].quantity -= 1
                }
            },
            onIncrement: {
                if let idx = cart.firstIndex(where: { $0.id == line.id }), cart[idx].quantity < 99 {
                    cart[idx].quantity += 1
                }
            },
            onRemove: { cart.removeAll { $0.id == line.id } }
        )
    }

    private func locationName(_ id: String?) -> String {
        guard let id else { return "" }
        return locations.first { $0.id == id }?.name ?? ""
    }

    private func addSku(_ sku: OperatingMasterPayload) {
        if let idx = cart.firstIndex(where: { $0.masterId == sku.id }) {
            if cart[idx].quantity < 99 { cart[idx].quantity += 1 }
        } else {
            cart.append(RetailPosLine.of(sku))
        }
        skuQuery = sku.name
    }

    private func pickFirstSku() {
        let q = skuQuery.trimmingCharacters(in: .whitespaces)
        let match = skus.first { $0.name.localizedCaseInsensitiveContains(q) } ?? skus.first
        if let match { addSku(match) }
    }

    private func post() async {
        guard canSave else { return }
        saving = true
        defer { saving = false }
        var contract = OperatingContractInput()
        contract.stockAction = "TRANSFER"
        contract.transferFromMasterId = fromId
        contract.transferToMasterId = toId
        contract.transferFromName = locationName(fromId)
        contract.transferToName = locationName(toId)
        contract.stockReason = "REPLENISHMENT"
        contract.ownership = "OWNED"
        let lines: [OperatingEventInput.OperatingLineItem] = cart.map {
            OperatingEventInput.OperatingLineItem(
                itemName: $0.name,
                quantity: "\($0.quantity)",
                unit: $0.unit,
                unitPrice: $0.unitPrice.isEmpty ? nil : $0.unitPrice,
                masterId: $0.masterId
            )
        }
        let first = cart.first
        let ok = await model.record(
            OperatingEventInput(
                momentId: model.momentId,
                eventType: "STOCK",
                title: cart.count == 1
                    ? (first?.name ?? "Transfer")
                    : "Transfer · \(cart.count) lines",
                note: note.isEmpty ? "Register \(register)" : note,
                quantity: "\(totalUnits)",
                unit: first?.unit,
                masterId: first?.masterId,
                lineItems: lines,
                contract: contract
            )
        )
        if ok { dismiss() }
    }
}

// MARK: - Reprint (`2508:29537`)

struct RetailReprintSheet: View {
    let theme: OperatingTheme
    @ObservedObject var model: BusinessOperatingModel
    var onNavigate: (String) -> Void = { _ in }
    @Environment(\.dismiss) private var dismiss

    @State private var register = ""
    @State private var sourceId: String?
    @State private var document: OperatingDocumentPayload?
    @State private var documentError: String?
    @State private var loading = false
    @State private var station = "All lines"
    @State private var reason = "Customer copy lost"

    private var registers: [String] {
        let fromLife = model.projection?.life?.channels.filter { !$0.isEmpty } ?? []
        return fromLife.isEmpty ? ["Register 1", "Register 2", "Online"] : Array(fromLife.prefix(3))
    }

    private var sources: [OperatingActivityPayload] {
        (model.projection?.activity ?? []).filter { $0.eventType.uppercased() == "SALE" }
    }

    private var selected: OperatingActivityPayload? {
        sources.first { $0.id == sourceId } ?? sources.first
    }

    private var billNo: String {
        if let key = selected?.clientKey, !key.isEmpty { return key }
        if let id = selected?.id {
            return "HM-B-\(id.suffix(5))"
        }
        return "—"
    }

    private var postedAt: String {
        guard let raw = selected?.occurredAt, !raw.isEmpty else { return "—" }
        if raw.count >= 16 { return String(raw.dropFirst(11).prefix(5)) }
        return String(raw.prefix(10))
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                RoseGrabber()
                RoseSheetHeader(
                    theme: theme,
                    title: "Reprint receipt",
                    subtitle: "Re-print posted bill · same bill no.",
                    qaCode: "RTL-QA-REPRINT",
                    onClose: { dismiss() }
                )

                RetailUtilityCrossNav(theme: theme, active: RetailUtilityNav.reprint, onNavigate: onNavigate)
                RetailRegisterChips(theme: theme, options: registers, selection: registerBinding)

                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 6) {
                        RoseModeChip(theme: theme, text: "Bill \(billNo)", selected: true) {}
                        RoseModeChip(
                            theme: theme,
                            text: "Posted \(postedAt) · \(selected?.paymentStatus ?? "Settled")",
                            selected: false
                        ) {}
                        RoseModeChip(theme: theme, text: "GST on", selected: false) {}
                    }
                }

                RoseAmountHero(
                    theme: theme,
                    label: "Tax invoice · \(billNo)",
                    amountText: heroAmountText,
                    caption: heroCaption
                )

                if sources.isEmpty {
                    RoseEmptyCard(
                        theme: theme,
                        title: "Nothing to reprint",
                        bodyText: "Post a sale on this moment first."
                    )
                } else {
                    RoseSectionCard(theme: theme, label: "Pick sale") {
                        VStack(spacing: 6) {
                            ForEach(Array(sources.prefix(10))) { row in
                                Button {
                                    sourceId = row.id
                                    Task { await loadDocument(row.id) }
                                } label: {
                                    HStack {
                                        VStack(alignment: .leading, spacing: 2) {
                                            Text(row.title)
                                                .font(.plusJakarta(size: 13, weight: .semibold))
                                                .foregroundStyle(OperatingV12.title)
                                                .lineLimit(1)
                                            Text(salePickMeta(row))
                                            .font(.plusJakarta(size: 10))
                                            .foregroundStyle(OperatingV12.subtitle)
                                        }
                                        Spacer()
                                        if let amount = row.amount {
                                            Text(OperatingV12Money.amount(amount, currency: model.currency))
                                                .font(.plusJakarta(size: 12, weight: .bold))
                                                .foregroundStyle(theme.accent)
                                        }
                                        if sourceId == row.id || (sourceId == nil && row.id == sources.first?.id) {
                                            Image(systemName: "checkmark.circle.fill")
                                                .foregroundStyle(theme.accent)
                                        }
                                    }
                                    .padding(.horizontal, 12)
                                    .padding(.vertical, 9)
                                    .background(
                                        (sourceId == row.id || (sourceId == nil && row.id == sources.first?.id))
                                            ? theme.accent.opacity(0.16)
                                            : theme.inset
                                    )
                                    .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                                }
                                .buttonStyle(.plain)
                            }
                        }
                    }
                }

                if selected != nil {
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 6) {
                            ForEach(["All lines", "Staples", "Dairy"], id: \.self) { name in
                                RoseModeChip(theme: theme, text: name, selected: station == name) {
                                    station = name
                                }
                            }
                        }
                    }

                    if loading {
                        ProgressView().tint(theme.accent)
                    } else if let document {
                        documentPreview(document)
                    } else {
                        RoseLinesCard(theme: theme, title: "Receipt preview", trailing: "1") {
                            RetailUtilityLineRow(
                                theme: theme,
                                title: selected?.title ?? "Posted sale",
                                qty: Int(selected?.quantity ?? "") ?? 1,
                                meta: selected?.subtitle?.nilIfBlank
                                    ?? selected?.note?.nilIfBlank
                                    ?? "Line detail not on activity row — amount only.",
                                category: selected?.channelName ?? "Posted",
                                status: "Posted"
                            )
                            Text("Preview chrome only — same bill number, no new SALE.")
                                .font(.plusJakarta(size: 10))
                                .foregroundStyle(OperatingV12.subtitle)
                        }
                    }

                    if let documentError {
                        Text(documentError)
                            .font(.plusJakarta(size: 12))
                            .foregroundStyle(theme.warning)
                    }

                    RetailUtilityMetaRows(
                        theme: theme,
                        rows: [
                            ("Original print", [postedAt, selected?.channelName ?? register].compactMap { $0 }.joined(separator: " · ")),
                            ("Reprint reason", reason),
                            (
                                "Payment",
                                [
                                    selected?.paymentStatus ?? "Settled",
                                    selected?.amount.map { OperatingV12Money.amount($0, currency: model.currency) },
                                ].compactMap { $0 }.joined(separator: " · ")
                            ),
                        ]
                    )

                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 6) {
                            ForEach(["Customer copy lost", "Printer jam", "Audit copy"], id: \.self) { option in
                                RoseModeChip(theme: theme, text: option, selected: reason == option) {
                                    reason = option
                                }
                            }
                            RoseLockedChip(theme: theme, label: "PDF / share")
                        }
                    }
                }

                RoseWhyBox(
                    theme: theme,
                    text: "Reprint re-renders the posted bill with the same number and GST split. No new sale, no stock or tender change."
                )

                RoseStickyFooter(
                    theme: theme,
                    title: "Reprint · duplicate copy",
                    disabled: selected == nil,
                    loading: loading,
                    action: {
                        // Read-only — never posts a new SALE. Optionally reloads document preview.
                        guard let id = selected?.id else { return }
                        Task { await loadDocument(id) }
                    }
                )
            }
            .padding(16)
            .padding(.bottom, 24)
        }
        .background(theme.canvas)
        .task {
            if register.isEmpty {
                register = registers.count > 1 ? registers[1] : (registers.first ?? "Register 1")
            }
            if sourceId == nil, let first = sources.first {
                sourceId = first.id
                await loadDocument(first.id)
            }
        }
    }

    private var registerBinding: Binding<String> {
        Binding(
            get: { register.isEmpty ? (registers.first ?? "") : register },
            set: { register = $0 }
        )
    }

    private var heroAmountText: String {
        guard let selected else { return "Nothing selected" }
        var parts: [String] = []
        if let name = selected.counterpartyName, !name.isEmpty {
            parts.append("B2C · \(name)")
        }
        if let state = model.projection?.gstState, !state.isEmpty {
            parts.append("\(state) intra-state")
        }
        if parts.isEmpty, let amount = selected.amount {
            return OperatingV12Money.amount(amount, currency: model.currency)
        }
        return parts.isEmpty ? "Posted sale" : parts.joined(separator: " · ")
    }

    private var heroCaption: String {
        guard let selected else { return "Pick a posted sale" }
        var parts: [String] = ["Posted sale"]
        if let amount = selected.amount {
            parts.append(OperatingV12Money.amount(amount, currency: model.currency))
        }
        parts.append("CGST + SGST stamped server-side")
        return parts.joined(separator: " · ")
    }

    @ViewBuilder
    private func documentPreview(_ doc: OperatingDocumentPayload) -> some View {
        RoseLinesCard(
            theme: theme,
            title: "Receipt preview",
            trailing: "\(doc.lines?.count ?? 0)"
        ) {
            Text("\(doc.kind) · \(doc.documentNumber)")
                .font(.plusJakarta(size: 14, weight: .bold))
                .foregroundStyle(OperatingV12.title)
            if let total = doc.grandTotal {
                Text("Total \(OperatingV12Money.amount(total, currency: doc.currency ?? model.currency))")
                    .font(.plusJakarta(size: 12, weight: .semibold))
                    .foregroundStyle(theme.accent)
            }
            if let lines = doc.lines, !lines.isEmpty {
                ForEach(Array(lines.prefix(6).enumerated()), id: \.offset) { _, line in
                    RetailUtilityLineRow(
                        theme: theme,
                        title: line.itemName ?? line.masterName ?? "Line",
                        qty: Int(line.quantity ?? "") ?? 1,
                        meta: [
                            line.unitPrice.map { OperatingV12Money.amount($0, currency: model.currency) },
                            line.gstRate.map { "GST \($0)%" },
                            line.unit,
                        ].compactMap { $0 }.joined(separator: " · ").nilIfBlank ?? "Posted",
                        category: "Posted",
                        status: "Posted"
                    )
                }
            } else if let notes = doc.notes, !notes.isEmpty {
                Text(notes)
                    .font(.plusJakarta(size: 11))
                    .foregroundStyle(OperatingV12.subtitle)
            }
        }
    }

    private func salePickMeta(_ row: OperatingActivityPayload) -> String {
        var bits = [String(row.occurredAt.prefix(10))]
        if let channel = row.channelName, !channel.isEmpty { bits.append(channel) }
        return bits.joined(separator: " · ")
    }

    private func loadDocument(_ eventId: String) async {
        loading = true
        documentError = nil
        defer { loading = false }
        // Server document kinds: INVOICE | CHALLAN (no separate RECEIPT endpoint).
        document = await model.document(eventId: eventId, kind: "INVOICE")
        if document == nil {
            // Soft fail — activity preview chrome still shows; never posts.
            documentError = nil
        }
    }
}

private extension Optional where Wrapped == String {
    var nilIfBlank: String? {
        guard let value = self?.trimmingCharacters(in: .whitespacesAndNewlines), !value.isEmpty else {
            return nil
        }
        return value
    }
}

private extension String {
    var nilIfBlank: String? {
        let t = trimmingCharacters(in: .whitespacesAndNewlines)
        return t.isEmpty ? nil : t
    }
}

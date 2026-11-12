import SwiftUI
import UIKit

/// Retail utilities — Figma RTL-QA Hold / Transfer / Reprint (`2508:29333`, `29447`, `29537`).
/// Rose Harbor Mart chrome (not Service teal). Park ↔ Hold recall via `RetailParkedSaleStore`.
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

// MARK: - Shared chrome (utility sheets only)

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
        HStack(spacing: 6) {
            chip("Hold", RetailUtilityNav.hold)
            chip("Transfer", RetailUtilityNav.transfer)
            chip("Reprint", RetailUtilityNav.reprint)
            Spacer(minLength: 0)
        }
    }

    private func chip(_ label: String, _ dest: String) -> some View {
        RoseModeChip(theme: theme, text: label, selected: active == dest, style: .ticket) {
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
                    RoseModeChip(
                        theme: theme,
                        text: option,
                        selected: selection == option,
                        style: .deep
                    ) {
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
        VStack(spacing: 2) {
            ForEach(Array(rows.enumerated()), id: \.offset) { _, row in
                HStack {
                    Text(row.0)
                        .font(.plusJakarta(size: 12, weight: .medium))
                        .foregroundStyle(RoseSheetChrome.textMuted)
                    Spacer(minLength: 8)
                    Text(row.1)
                        .font(.plusJakarta(size: 13, weight: .bold))
                        .foregroundStyle(OperatingV12.title)
                        .multilineTextAlignment(.trailing)
                        .lineLimit(2)
                }
                .padding(.vertical, 4)
            }
        }
        .padding(12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(RoseSheetChrome.chip)
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
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

@MainActor
private func retailRegisters(from model: BusinessOperatingModel) -> [String] {
    let fromLife = model.projection?.life?.channels.filter { !$0.isEmpty } ?? []
    return fromLife.isEmpty ? ["Register 1", "Register 2", "Online"] : Array(fromLife.prefix(3))
}

@MainActor
private func retailGstChip(model: BusinessOperatingModel) -> String {
    model.projection?.gstTracking?.uppercased() == "ON"
        || model.projection?.gstRegistration != nil
        ? "GST on" : "GST off"
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
    @State private var holdCode = RetailParkedSaleStore.shared.nextHoldCode()

    private var registers: [String] { retailRegisters(from: model) }

    private var skus: [OperatingMasterPayload] {
        let source = model.masters.isEmpty ? (model.projection?.masters ?? []) : model.masters
        return source
            .filter { ["SKU", "PRODUCT", "ITEM", "MATERIAL"].contains($0.kind.uppercased()) }
            .sorted { $0.name.localizedCaseInsensitiveCompare($1.name) == .orderedAscending }
    }

    private var masterById: [String: OperatingMasterPayload] {
        Dictionary(uniqueKeysWithValues: skus.map { ($0.id, $0) })
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

    private var heroTitle: String {
        let trimmed = customer.trimmingCharacters(in: .whitespacesAndNewlines)
        return trimmed.isEmpty ? "Walk-in" : trimmed
    }

    private var canPark: Bool { !parkCart.isEmpty }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                RoseGrabber()
                RoseSheetHeader(
                    theme: theme,
                    title: "Hold / Park bill",
                    subtitle: "Parked cart · no revenue, no stock",
                    onClose: { dismiss() }
                )

                RetailUtilityCrossNav(theme: theme, active: RetailUtilityNav.hold, onNavigate: onNavigate)
                RetailRegisterChips(theme: theme, options: registers, selection: registerBinding)

                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 6) {
                        RoseModeChip(
                            theme: theme,
                            text: "\(register.isEmpty ? "Register" : register) · Shift open",
                            selected: true,
                            style: .deep
                        ) {}
                        RoseModeChip(theme: theme, text: "Park · recall later", selected: false, style: .ticket) {}
                        RoseModeChip(theme: theme, text: retailGstChip(model: model), selected: false, style: .ticket) {}
                    }
                }

                RoseTicketHero(
                    theme: theme,
                    eyebrow: "Hold slip",
                    code: holdCode,
                    title: heroTitle,
                    subtitle: "\(parkCart.count) line\(parkCart.count == 1 ? "" : "s") · \(locationLabel) · \(register.isEmpty ? "—" : register)"
                )

                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 6) {
                        ForEach(stations, id: \.self) { name in
                            RoseModeChip(
                                theme: theme,
                                text: name,
                                selected: station == name,
                                style: .ticket
                            ) {
                                station = name
                            }
                        }
                    }
                }

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
                        bodyText: "Scan Harbor Mart SKUs to park a draft cart. Session parks only — no register / holds API yet."
                    )
                } else {
                    VStack(spacing: 8) {
                        ForEach(Array(visibleCart.enumerated()), id: \.element.id) { index, line in
                            lineCard(line, index: index)
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
                            RoseModeChip(
                                theme: theme,
                                text: printer,
                                selected: slipTo == printer,
                                style: .ticket
                            ) {
                                slipTo = printer
                            }
                        }
                        RoseLockedChip(theme: theme, label: "Thermal print")
                    }
                }
                Text("Slip to is chrome-only — no print stack.")
                    .font(.plusJakarta(size: 10))
                    .foregroundStyle(RoseSheetChrome.textMuted)

                RoseWhyBox(
                    theme: theme,
                    text: "Hold parks a draft cart on this device. No revenue, no stock, no tender and no GST until Charge on Scan cart. Device-local until a register/holds API exists — not shared across devices."
                )

                RoseStickyFooter(
                    theme: theme,
                    title: canPark ? "Park bill · \(holdCode)" : "Park bill",
                    caption: "Device draft only — revenue + GST post on Charge",
                    disabled: !canPark,
                    action: parkBill
                )
            }
            .padding(.horizontal, 20)
            .padding(.top, 12)
            .padding(.bottom, 28)
        }
        .background(RoseSheetChrome.sheet)
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
    private func lineCard(_ line: RetailPosLine, index: Int) -> some View {
        let master = line.masterId.flatMap { masterById[$0] }
        let cat = retailStation(for: master)
        let metaBits = [
            line.skuCode ?? master?.code ?? master?.barcode,
            line.unitPrice.isEmpty ? nil : OperatingV12Money.amount(line.unitPrice, currency: model.currency),
            line.hsn.map { "HSN \($0)" } ?? master?.hsn.map { "HSN \($0)" },
            gstLabel(master),
        ].compactMap { $0 }
        RoseTicketLineCard(
            theme: theme,
            title: line.name,
            qtyLabel: "×\(line.quantity)",
            meta: metaBits.isEmpty ? (line.unit ?? "pc") : metaBits.joined(separator: " · "),
            category: cat,
            status: "Parked",
            statusStyle: index == 2 ? .amberParked : .parked,
            onDecrement: {
                if line.quantity <= 1 {
                    parkCart.removeAll { $0.id == line.id }
                } else if let idx = parkCart.firstIndex(where: { $0.id == line.id }) {
                    parkCart[idx].quantity -= 1
                }
            },
            onIncrement: {
                if let idx = parkCart.firstIndex(where: { $0.id == line.id }), parkCart[idx].quantity < 99 {
                    parkCart[idx].quantity += 1
                }
            }
        )
    }

    private func gstLabel(_ master: OperatingMasterPayload?) -> String? {
        if let rate = master?.gstRate, !rate.isEmpty { return "\(rate)%" }
        if master?.gstApplicable?.uppercased() == "EXEMPT" { return "Exempt" }
        return nil
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
            holdCode: holdCode,
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
    @State private var honestyNote: String?

    private var registers: [String] { retailRegisters(from: model) }

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
            VStack(alignment: .leading, spacing: 14) {
                RoseGrabber()
                RoseSheetHeader(
                    theme: theme,
                    title: "Stock transfer",
                    subtitle: "Move stock between locations · no revenue",
                    onClose: { dismiss() }
                )

                RetailUtilityCrossNav(theme: theme, active: RetailUtilityNav.transfer, onNavigate: onNavigate)
                RetailRegisterChips(theme: theme, options: registers, selection: registerBinding)

                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 6) {
                        RoseModeChip(theme: theme, text: routeLabel, selected: true, style: .deep) {}
                        RoseModeChip(theme: theme, text: "Same GSTIN", selected: false, style: .ticket) {}
                        RoseModeChip(theme: theme, text: retailGstChip(model: model), selected: false, style: .ticket) {}
                    }
                }

                RoseTicketHero(
                    theme: theme,
                    eyebrow: "Transfer slip",
                    code: transferCode,
                    title: routeLabel,
                    subtitle: "\(cart.count) line\(cart.count == 1 ? "" : "s") · dispatch now"
                )

                RoseSectionCard(theme: theme, label: "Route") {
                    if locations.count < 2 {
                        RoseEmptyCard(
                            theme: theme,
                            title: "Need two locations",
                            bodyText: "Add a second location under Masters before transferring. STOCK schema has no first-class from/to — confirm posts best-effort STOCK notes."
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
                            RoseModeChip(
                                theme: theme,
                                text: name,
                                selected: station == name,
                                style: .ticket
                            ) {
                                station = name
                            }
                        }
                    }
                }

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
                        bodyText: "Scan SKUs to build a multi-line transfer. Confirm never posts a sale."
                    )
                } else {
                    VStack(spacing: 8) {
                        ForEach(visibleCart) { line in
                            transferLineCard(line)
                        }
                    }
                }

                RetailUtilityMetaRows(
                    theme: theme,
                    rows: [
                        ("Valuation", "At cost · no GST within the same GSTIN"),
                        (
                            "Receiver",
                            locationName(toId).isEmpty
                                ? "—"
                                : "\(locationName(toId)) · \(model.projection?.participants?.first ?? "Receiver")"
                        ),
                        ("Sale impact", "None"),
                    ]
                )

                RoseFieldLabel(text: "Note")
                RoseTextField(theme: theme, placeholder: "Dispatch note", text: $note)

                if let honestyNote {
                    Text(honestyNote)
                        .font(.plusJakarta(size: 11, weight: .medium))
                        .foregroundStyle(theme.warning)
                }

                RoseWhyBox(
                    theme: theme,
                    text: "Transfer moves stock between locations at cost: − from, + to on confirm. No revenue, no AP, and no GST within the same GSTIN."
                )

                RoseStickyFooter(
                    theme: theme,
                    title: canSave ? "Confirm transfer · \(totalUnits) units" : "Confirm transfer",
                    caption: "Transfer never posts a sale",
                    disabled: !canSave,
                    loading: saving,
                    action: { Task { await post() } }
                )
            }
            .padding(.horizontal, 20)
            .padding(.top, 12)
            .padding(.bottom, 28)
        }
        .background(RoseSheetChrome.sheet)
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
                            selected: cart.contains(where: { $0.masterId == sku.id }),
                            style: .ticket
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
                        RoseModeChip(
                            theme: theme,
                            text: loc.name,
                            selected: selection.wrappedValue == loc.id,
                            style: .deep
                        ) {
                            selection.wrappedValue = selection.wrappedValue == loc.id ? nil : loc.id
                        }
                    }
                }
            }
        }
    }

    @ViewBuilder
    private func transferLineCard(_ line: RetailPosLine) -> some View {
        let master = line.masterId.flatMap { masterById[$0] }
        let cat = retailStation(for: master)
        let code = line.skuCode ?? master?.code ?? master?.barcode ?? "SKU"
        RoseTicketLineCard(
            theme: theme,
            title: line.name,
            qtyLabel: "×\(line.quantity)",
            meta: "\(code) · \(line.quantity) \(line.unit ?? "pc") · on hand —",
            category: cat,
            status: "Move",
            statusStyle: .move,
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
            }
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
        honestyNote = nil
        defer { saving = false }
        // Best-effort STOCK — schema lacks first-class from/to; contract fields carry route for honesty.
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
        let routeNote = "\(locationName(fromId)) → \(locationName(toId))"
        let ok = await model.record(
            OperatingEventInput(
                momentId: model.momentId,
                eventType: "STOCK",
                title: cart.count == 1
                    ? (first?.name ?? "Transfer")
                    : "Transfer · \(cart.count) lines",
                note: note.isEmpty
                    ? "Transfer \(routeNote) · Register \(register) · no sale"
                    : "\(note) · \(routeNote)",
                quantity: "\(totalUnits)",
                unit: first?.unit,
                masterId: first?.masterId,
                lineItems: lines,
                contract: contract
            )
        )
        if ok {
            dismiss()
        } else {
            honestyNote = "Transfer post failed — STOCK route may lack from/to on server. UI kept; no sale invented."
        }
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
    @State private var loading = false
    @State private var station = "All lines"
    @State private var reason = "Customer copy lost"
    @State private var showReprintToast = false
    @State private var shareItems: [Any] = []
    @State private var showShareSheet = false
    @State private var paper: RetailReceiptPaperSize = .default
    @State private var shareFormat: RetailReceiptShareFormat = .jpg
    @State private var logo: UIImage? = nil

    private var registers: [String] { retailRegisters(from: model) }

    private var sources: [OperatingActivityPayload] {
        (model.projection?.activity ?? []).filter { $0.eventType.uppercased() == "SALE" }
    }

    private var selected: OperatingActivityPayload? {
        sources.first { $0.id == sourceId } ?? sources.first
    }

    /// Prefer server clientKey; never invent a fiscal bill number as posted fact.
    private var billLabel: String {
        if let key = selected?.clientKey?.nilIfBlank { return key }
        if selected != nil { return "Posted sale · no bill no." }
        return "—"
    }

    private var postedAt: String {
        guard let raw = selected?.occurredAt, !raw.isEmpty else { return "—" }
        if raw.count >= 16 { return String(raw.dropFirst(11).prefix(5)) }
        return String(raw.prefix(10))
    }

    private var tenderHint: String {
        selected?.paymentStatus?.nilIfBlank ?? "Settled"
    }

    var body: some View {
        ZStack(alignment: .top) {
            ScrollView {
                VStack(alignment: .leading, spacing: 14) {
                    RoseGrabber()
                    RoseSheetHeader(
                        theme: theme,
                        title: "Reprint receipt",
                        subtitle: "Re-print posted bill · same bill no.",
                        onClose: { dismiss() }
                    )

                    RetailUtilityCrossNav(theme: theme, active: RetailUtilityNav.reprint, onNavigate: onNavigate)
                    RetailRegisterChips(theme: theme, options: registers, selection: registerBinding)

                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 6) {
                            RoseModeChip(
                                theme: theme,
                                text: billLabel.hasPrefix("Posted") ? billLabel : "Bill \(billLabel)",
                                selected: true,
                                style: .deep
                            ) {}
                            RoseModeChip(
                                theme: theme,
                                text: "Posted \(postedAt) · \(tenderHint)",
                                selected: false,
                                style: .ticket
                            ) {}
                            RoseModeChip(
                                theme: theme,
                                text: retailGstChip(model: model),
                                selected: false,
                                style: .ticket
                            ) {}
                        }
                    }

                    RoseTicketHero(
                        theme: theme,
                        eyebrow: "Tax invoice",
                        code: billLabel,
                        title: heroTitle,
                        subtitle: heroCaption
                    )

                    if sources.isEmpty {
                        RoseEmptyCard(
                            theme: theme,
                            title: "Nothing to reprint",
                            bodyText: "Post a sale on this moment first. No receipt PDF API — client re-render only."
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
                                                    .foregroundStyle(RoseSheetChrome.textMuted)
                                            }
                                            Spacer()
                                            if let amount = row.amount {
                                                Text(OperatingV12Money.amount(amount, currency: model.currency))
                                                    .font(.plusJakarta(size: 12, weight: .bold))
                                                    .foregroundStyle(theme.accent)
                                            }
                                            if isSelected(row) {
                                                Image(systemName: "checkmark.circle.fill")
                                                    .foregroundStyle(theme.accent)
                                            }
                                        }
                                        .padding(.horizontal, 12)
                                        .padding(.vertical, 9)
                                        .background(isSelected(row) ? theme.accent.opacity(0.16) : theme.inset)
                                        .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                                    }
                                    .buttonStyle(.plain)
                                }
                            }
                        }
                    }

                    if let selected {
                        ScrollView(.horizontal, showsIndicators: false) {
                            HStack(spacing: 6) {
                                ForEach(["All lines", "Staples", "Dairy"], id: \.self) { name in
                                    RoseModeChip(
                                        theme: theme,
                                        text: name,
                                        selected: station == name,
                                        style: .ticket
                                    ) {
                                        station = name
                                    }
                                }
                            }
                        }

                        if loading {
                            ProgressView().tint(theme.accent)
                        }

                        let receiptModel = RetailReceiptBuilder.fromSale(
                            model: model,
                            sale: selected,
                            document: document,
                            duplicate: true,
                            reason: reason,
                            tenderOverride: tenderHint,
                            enteredBy: RetailReceiptBuilder.enteredBy(
                                fallback: model.projection?.participants?.first ?? "Cashier"
                            )
                        )
                        RetailReceiptPreview(
                            model: receiptModel,
                            paper: paper,
                            logo: logo
                        )
                        RetailReceiptShareControls(
                            theme: theme,
                            model: receiptModel,
                            companyId: model.companyId,
                            momentId: model.momentId,
                            paper: $paper,
                            format: $shareFormat,
                            logo: $logo
                        ) { items in
                            shareItems = items
                            showShareSheet = true
                            showReprintToast = true
                        }

                        RetailUtilityMetaRows(
                            theme: theme,
                            rows: [
                                (
                                    "Original print",
                                    [postedAt, model.projection?.participants?.first ?? "Cashier", register]
                                        .filter { !$0.isEmpty }
                                        .joined(separator: " · ")
                                ),
                                ("Reprint reason", reason),
                                (
                                    "Payment",
                                    [
                                        "Settled",
                                        selected.amount.map {
                                            "\(selected.paymentStatus?.nilIfBlank ?? "UPI") \(OperatingV12Money.amount($0, currency: model.currency))"
                                        },
                                    ].compactMap { $0 }.joined(separator: " · ")
                                ),
                            ]
                        )

                        ScrollView(.horizontal, showsIndicators: false) {
                            HStack(spacing: 6) {
                                ForEach(["Customer copy lost", "Printer jam", "Audit copy"], id: \.self) { option in
                                    RoseModeChip(
                                        theme: theme,
                                        text: option,
                                        selected: reason == option,
                                        style: .ticket
                                    ) {
                                        reason = option
                                    }
                                }
                            }
                        }
                    }

                    RoseWhyBox(
                        theme: theme,
                        text: "Reprint shares a duplicate copy from posted SALE fields (same bill key when present). Client re-render only — no new sale, no stock or tender change, no allocated PDF API."
                    )

                    RoseStickyFooter(
                        theme: theme,
                        title: "Reprint · duplicate copy",
                        caption: "Share / print duplicate — never duplicates revenue",
                        disabled: selected == nil,
                        loading: loading,
                        action: {
                            shareDuplicateReceipt()
                        }
                    )
                }
                .padding(.horizontal, 20)
                .padding(.top, 12)
                .padding(.bottom, 28)
            }
            .background(RoseSheetChrome.sheet)
            .sheet(isPresented: $showShareSheet) {
                ActivityShareSheet(items: shareItems)
            }

            if showReprintToast {
                Text("✓ Duplicate copy · client re-render only · no new SALE")
                    .font(.plusJakarta(size: 12, weight: .semibold))
                    .foregroundStyle(RoseSheetChrome.inkOnChip)
                    .padding(.horizontal, 14)
                    .padding(.vertical, 10)
                    .background(RoseSheetChrome.label)
                    .clipShape(Capsule())
                    .padding(.top, 12)
                    .transition(.opacity)
                    .onAppear {
                        DispatchQueue.main.asyncAfter(deadline: .now() + 2.2) {
                            showReprintToast = false
                        }
                    }
            }
        }
        .task {
            if register.isEmpty {
                register = registers.count > 1 ? registers[1] : (registers.first ?? "Register 1")
            }
            if sourceId == nil, let first = sources.first {
                sourceId = first.id
                await loadDocument(first.id)
            }
            logo = RetailReceiptLogoStore.loadImage(
                companyId: model.companyId,
                momentId: model.momentId
            )
        }
    }

    private var registerBinding: Binding<String> {
        Binding(
            get: { register.isEmpty ? (registers.first ?? "") : register },
            set: { register = $0 }
        )
    }

    private var heroTitle: String {
        guard selected != nil else { return "Nothing selected" }
        var parts: [String] = ["B2C"]
        if let state = model.projection?.gstState, !state.isEmpty {
            parts.append("\(state) intra-state")
        } else {
            parts.append("MH intra-state")
        }
        return parts.joined(separator: " · ")
    }

    private var heroCaption: String {
        guard let selected else { return "Pick a posted sale" }
        var parts: [String] = []
        if let lines = document?.lines, !lines.isEmpty {
            parts.append("\(lines.count) line\(lines.count == 1 ? "" : "s")")
        } else {
            parts.append("Posted sale")
        }
        if selected.amount != nil {
            parts.append("CGST 2.5% + SGST 2.5%")
        }
        return parts.joined(separator: " · ")
    }

    private func isSelected(_ row: OperatingActivityPayload) -> Bool {
        sourceId == row.id || (sourceId == nil && row.id == sources.first?.id)
    }

    private func salePickMeta(_ row: OperatingActivityPayload) -> String {
        var bits = [String(row.occurredAt.prefix(10))]
        if let channel = row.channelName, !channel.isEmpty { bits.append(channel) }
        return bits.joined(separator: " · ")
    }

    private func loadDocument(_ eventId: String) async {
        loading = true
        defer { loading = false }
        // Server document kinds: INVOICE | CHALLAN (no separate RECEIPT / PDF endpoint).
        document = await model.document(eventId: eventId, kind: "INVOICE")
    }

    /// Client re-render share — never allocates a fiscal bill number or posts SALE.
    private func shareDuplicateReceipt() {
        guard let sale = selected else { return }
        let receipt = RetailReceiptBuilder.fromSale(
            model: model,
            sale: sale,
            document: document,
            duplicate: true,
            reason: reason,
            tenderOverride: tenderHint,
            enteredBy: RetailReceiptBuilder.enteredBy(
                fallback: model.projection?.participants?.first ?? "Cashier"
            )
        )
        shareItems = RetailReceiptShare.items(
            for: receipt,
            paper: paper,
            format: shareFormat,
            logo: logo
        )
        showShareSheet = true
        showReprintToast = true
    }
}

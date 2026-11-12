import SwiftUI

/// Adjacent hub Hold · Transfer · Reprint — ember ticket chrome (not trading layouts).
enum RestaurantUtilityTab: String, CaseIterable, Identifiable {
    case hold, transfer, reprint

    var id: String { rawValue }

    var chipLabel: String {
        switch self {
        case .hold: return "Hold"
        case .transfer: return "Transfer"
        case .reprint: return "Reprint"
        }
    }

    var modeCode: String { rawValue.uppercased() }

    static func from(mode: String) -> RestaurantUtilityTab {
        switch mode.uppercased() {
        case "TRANSFER": return .transfer
        case "REPRINT": return .reprint
        default: return .hold
        }
    }
}

struct RestaurantUtilitySheet: View {
    let theme: OperatingTheme
    let mode: String
    @ObservedObject var model: BusinessOperatingModel
    var onRoute: ((String) -> Void)? = nil

    @Environment(\.dismiss) private var dismiss
    @State private var tab: RestaurantUtilityTab
    @State private var selectedOrderId: String?
    @State private var selectedBillId: String?
    @State private var fromLoc = "Cold store"
    @State private var toLoc = "Hot pass"
    @State private var item = "Paneer"
    @State private var qty = "2"
    @State private var reprintReason = "Guest copy · printer jam"
    @State private var saving = false

    private let holdCode: String
    private let transferCode: String

    init(
        theme: OperatingTheme,
        mode: String,
        model: BusinessOperatingModel,
        onRoute: ((String) -> Void)? = nil
    ) {
        self.theme = theme
        self.mode = mode
        self.model = model
        self.onRoute = onRoute
        _tab = State(initialValue: RestaurantUtilityTab.from(mode: mode))
        let stamp = Int(Date().timeIntervalSince1970) % 90
        holdCode = String(format: "HOLD-%02d", stamp + 10)
        transferCode = String(format: "TRF-%03d", stamp + 1)
    }

    private var brand: String {
        let t = model.title.trimmingCharacters(in: .whitespacesAndNewlines)
        return t.isEmpty ? "Spice Route" : t
    }

    private var openOrders: [OperatingActivityPayload] {
        (model.projection?.activity ?? []).filter {
            ["ORDER", "CUSTOMER_ORDER"].contains($0.eventType.uppercased())
        }
    }

    private var postedBills: [OperatingActivityPayload] {
        (model.projection?.activity ?? []).filter { $0.eventType.uppercased() == "SALE" }
    }

    private var stations: [String] {
        let source = model.masters.isEmpty ? (model.projection?.masters ?? []) : model.masters
        let names = source
            .filter { ["STATION", "KITCHEN_STATION", "LOCATION", "STORE"].contains($0.kind.uppercased()) }
            .map(\.name)
        return names.isEmpty ? ["Cold store", "Hot pass", "Bar", "Dry store"] : names
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                EmberGrabber()
                EmberSheetHeader(
                    title: RestaurantCaptureCopy.sheetTitle(tab.modeCode),
                    subtitle: headerSubtitle,
                    onClose: { dismiss() }
                )
                EmberChipRow(
                    options: RestaurantUtilityTab.allCases.map(\.chipLabel),
                    selection: Binding(
                        get: { tab.chipLabel },
                        set: { label in
                            if let next = RestaurantUtilityTab.allCases.first(where: { $0.chipLabel == label }) {
                                tab = next
                                onRoute?(next.modeCode)
                            }
                        }
                    )
                )
                contextChips
                switch tab {
                case .hold: holdBody
                case .transfer: transferBody
                case .reprint: reprintBody
                }
                EmberWhyBox(text: RestaurantCaptureCopy.why(tab.modeCode))
                EmberStickyFooter(
                    title: saving ? "Saving…" : footerTitle,
                    caption: footerCaption,
                    disabled: !canSave || saving,
                    loading: saving,
                    action: { Task { await save() } }
                )
            }
            .padding(16)
            .padding(.bottom, 24)
        }
        .background(EmberSheetChrome.sheet)
        .onAppear {
            if selectedOrderId == nil { selectedOrderId = openOrders.first?.id }
            if selectedBillId == nil { selectedBillId = postedBills.first?.id }
            if fromLoc.isEmpty { fromLoc = stations.first ?? "Cold store" }
            if toLoc.isEmpty || toLoc == fromLoc {
                toLoc = stations.dropFirst().first ?? stations.first ?? "Hot pass"
            }
        }
        .task { if model.masters.isEmpty { await model.loadMasters() } }
    }

    private var headerSubtitle: String {
        switch tab {
        case .hold: return "Park draft · no revenue · no KOT"
        case .transfer: return "Station → station · cost only"
        case .reprint: return "Same bill id · no new sale"
        }
    }

    private var footerTitle: String {
        switch tab {
        case .hold: return "Hold order · \(holdCode)"
        case .transfer: return "Confirm transfer · \(transferCode)"
        case .reprint: return "Reprint receipt"
        }
    }

    private var footerCaption: String {
        switch tab {
        case .hold: return "No revenue or stock while held"
        case .transfer: return "Transfer never posts a sale"
        case .reprint: return "Same bill id — never duplicates the sale"
        }
    }

    private var canSave: Bool {
        switch tab {
        case .hold: return selectedOrder != nil
        case .transfer:
            return fromLoc != toLoc && (Decimal(string: qty).map { $0 > 0 } ?? false)
        case .reprint: return selectedBill != nil
        }
    }

    private var selectedOrder: OperatingActivityPayload? {
        openOrders.first { $0.id == selectedOrderId } ?? openOrders.first
    }

    private var selectedBill: OperatingActivityPayload? {
        postedBills.first { $0.id == selectedBillId } ?? postedBills.first
    }

    private var contextChips: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 6) {
                EmberModeChip(text: brand, selected: true, style: .channel) {}
                EmberModeChip(
                    text: tab == .hold ? "Floor hold" : tab == .transfer ? "No sale" : "Posted bill",
                    selected: false
                ) {}
                EmberModeChip(
                    text: tab == .hold ? "No sale" : tab == .transfer ? "Same GSTIN" : "Share",
                    selected: false
                ) {}
            }
        }
    }

    @ViewBuilder
    private var holdBody: some View {
        EmberTicketHero(
            eyebrow: "Hold slip",
            ticketId: holdCode,
            headline: selectedOrder?.title.nilIfBlank ?? "Open ticket",
            detail: "\(selectedOrder?.note ?? "Park until guest ready") · \(holdCode)"
        )
        EmberFieldLabel(text: "Open tickets")
        if openOrders.isEmpty {
            EmberEmptyCard(title: "No open order", bodyText: "Send a KOT or place an order before parking.")
        } else {
            ForEach(openOrders.prefix(6), id: \.id) { ticket in
                let active = ticket.id == selectedOrder?.id
                Button {
                    selectedOrderId = ticket.id
                } label: {
                    HStack {
                        VStack(alignment: .leading, spacing: 2) {
                            Text(ticket.title.nilIfBlank ?? "Order")
                                .font(.plusJakarta(size: 13, weight: .semibold))
                                .foregroundStyle(EmberSheetChrome.textPrimary)
                            Text(ticket.note.map { String($0.prefix(48)) } ?? "Open")
                                .font(.plusJakarta(size: 10))
                                .foregroundStyle(EmberSheetChrome.textBody)
                                .lineLimit(1)
                        }
                        Spacer()
                        EmberModeChip(text: active ? "Hold" : "Park", selected: active) {
                            selectedOrderId = ticket.id
                        }
                    }
                    .padding(12)
                    .background(active ? EmberSheetChrome.ember.opacity(0.14) : EmberSheetChrome.chip)
                    .overlay(
                        RoundedRectangle(cornerRadius: 14, style: .continuous)
                            .strokeBorder(active ? EmberSheetChrome.ember : EmberSheetChrome.hairline, lineWidth: 1)
                    )
                    .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                }
                .buttonStyle(.plain)
            }
        }
    }

    @ViewBuilder
    private var transferBody: some View {
        EmberTicketHero(
            eyebrow: "Transfer slip",
            ticketId: transferCode,
            headline: "\(fromLoc) → \(toLoc)",
            detail: "\(item) · \(qty) · \(transferCode)"
        )
        EmberPickerRow(label: "From", value: fromLoc) {
            cycleStation(binding: $fromLoc)
        }
        EmberPickerRow(label: "To", value: toLoc) {
            cycleStation(binding: $toLoc)
        }
        EmberFieldLabel(text: "Item")
        EmberTextField(placeholder: "Paneer", text: $item)
        EmberFieldLabel(text: "Qty")
        EmberTextField(placeholder: "2", text: $qty, keyboard: .decimalPad)
    }

    @ViewBuilder
    private var reprintBody: some View {
        let billLabel = selectedBill.map { "BILL-\($0.id.suffix(6))" } ?? "—"
        let amount = selectedBill.flatMap(\.amount).map {
            OperatingV12Money.amount($0, currency: model.currency)
        } ?? "Unavailable"
        EmberTicketHero(
            eyebrow: "Receipt",
            ticketId: String(billLabel),
            headline: selectedBill?.title.nilIfBlank ?? "No posted bill",
            detail: amount
        )
        EmberFieldLabel(text: "Posted bills")
        if postedBills.isEmpty {
            EmberEmptyCard(title: "No posted bill", bodyText: "Charge a POS bill first — reprint never invents revenue.")
        } else {
            ForEach(postedBills.prefix(6), id: \.id) { bill in
                let active = bill.id == selectedBill?.id
                Button {
                    selectedBillId = bill.id
                } label: {
                    HStack {
                        VStack(alignment: .leading, spacing: 2) {
                            Text(bill.title.nilIfBlank ?? "Sale")
                                .font(.plusJakarta(size: 13, weight: .semibold))
                                .foregroundStyle(EmberSheetChrome.textPrimary)
                            Text(OperatingV12Money.amount(bill.amount, currency: model.currency))
                                .font(.plusJakarta(size: 10))
                                .foregroundStyle(EmberSheetChrome.textBody)
                        }
                        Spacer()
                        EmberModeChip(text: active ? "Reprint" : "Pick", selected: active) {
                            selectedBillId = bill.id
                        }
                    }
                    .padding(12)
                    .background(active ? EmberSheetChrome.ember.opacity(0.14) : EmberSheetChrome.chip)
                    .overlay(
                        RoundedRectangle(cornerRadius: 14, style: .continuous)
                            .strokeBorder(active ? EmberSheetChrome.ember : EmberSheetChrome.hairline, lineWidth: 1)
                    )
                    .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                }
                .buttonStyle(.plain)
            }
        }
        EmberFieldLabel(text: "Reprint reason")
        EmberTextField(placeholder: "Guest copy · printer jam", text: $reprintReason)
    }

    private func cycleStation(binding: Binding<String>) {
        let idx = stations.firstIndex(of: binding.wrappedValue) ?? -1
        binding.wrappedValue = stations[(idx + 1) % max(stations.count, 1)]
    }

    private func save() async {
        guard canSave else { return }
        saving = true
        defer { saving = false }
        var input: OperatingEventInput
        switch tab {
        case .hold:
            guard let ticket = selectedOrder else { return }
            var contract = OperatingContractInput()
            contract.orderStatus = "HOLD"
            contract.clientKey = holdCode
            input = OperatingEventInput(
                momentId: model.momentId,
                eventType: "ORDER",
                title: "Hold · \(holdCode) · \(ticket.title)",
                amount: nil,
                note: "HOLD · no revenue · no KOT · \(holdCode)",
                channelName: ticket.channelName,
                counterpartyName: ticket.counterpartyName
            )
            input.revisesEventId = ticket.id
            input.contract = contract
        case .transfer:
            var contract = OperatingContractInput()
            contract.stockAction = "TRANSFER"
            contract.locationName = fromLoc
            contract.clientKey = transferCode
            input = OperatingEventInput(
                momentId: model.momentId,
                eventType: "STOCK",
                title: "Transfer · \(item) · \(fromLoc) → \(toLoc)",
                amount: nil,
                note: "TRANSFER · \(transferCode) · from=\(fromLoc) · to=\(toLoc) · no sale",
                channelName: fromLoc,
                counterpartyName: toLoc,
                quantity: qty
            )
            input.contract = contract
        case .reprint:
            guard let bill = selectedBill else { return }
            input = OperatingEventInput(
                momentId: model.momentId,
                eventType: "UPDATE",
                title: "Reprint · \(bill.title)",
                amount: nil,
                note: "REPRINT receipt · same id \(bill.id) · \(reprintReason) · no new sale",
                channelName: bill.channelName,
                counterpartyName: bill.counterpartyName,
                parentEventId: bill.id
            )
        }
        if await model.record(input) { dismiss() }
    }
}

extension String {
    var nilIfBlank: String? {
        let t = trimmingCharacters(in: .whitespacesAndNewlines)
        return t.isEmpty ? nil : t
    }
}

extension Optional where Wrapped == String {
    var nilIfBlank: String? {
        self?.nilIfBlank
    }
}

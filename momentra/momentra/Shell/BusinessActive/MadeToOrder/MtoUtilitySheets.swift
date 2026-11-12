import SwiftUI

enum MtoUtilityTab: String, CaseIterable, Identifiable {
    case hold, transfer, reprint
    var id: String { rawValue }
    var chipLabel: String {
        switch self {
        case .hold: return "Hold"
        case .transfer: return "Transfer"
        case .reprint: return "Reprint"
        }
    }
    static func from(mode: String) -> MtoUtilityTab {
        switch mode.uppercased() {
        case "TRANSFER": return .transfer
        case "REPRINT": return .reprint
        default: return .hold
        }
    }
}

/// Figma MTO-QA Hold / TRANSFER / REPRINT.
struct MtoUtilitySheet: View {
    let theme: OperatingTheme
    let mode: String
    @ObservedObject var model: BusinessOperatingModel
    var onRoute: ((String) -> Void)? = nil

    @Environment(\.dismiss) private var dismiss
    @State private var tab: MtoUtilityTab
    @State private var line = "Line 1"
    @State private var station = "All items"
    @State private var fromLoc = "Raw store"
    @State private var toLoc = "Press line 1"
    @State private var material = ""
    @State private var qty = "1"
    @State private var reprintTitle = ""
    @State private var reprintStation = "All"
    @State private var reason = "Customer copy"
    @State private var saving = false

    private let wohCode: String

    init(theme: OperatingTheme, mode: String, model: BusinessOperatingModel, onRoute: ((String) -> Void)? = nil) {
        self.theme = theme
        self.mode = mode
        self.model = model
        self.onRoute = onRoute
        _tab = State(initialValue: MtoUtilityTab.from(mode: mode))
        wohCode = String(format: "JH-%02d", Int(Date().timeIntervalSince1970) % 90 + 10)
    }

    private var openOrders: [OperatingActivityPayload] {
        (model.projection?.activity ?? []).filter {
            ["ORDER", "CUSTOMER_ORDER"].contains($0.eventType.uppercased())
                || $0.title.localizedCaseInsensitiveContains("WO")
        }
    }

    private var sales: [OperatingActivityPayload] {
        (model.projection?.activity ?? []).filter { $0.eventType.uppercased() == "SALE" }
    }

    private var heldLines: [(name: String, meta: String, cat: String)] {
        let masters = model.masters.isEmpty ? (model.projection?.masters ?? []) : model.masters
        let parts = masters.filter { ["SKU", "PART", "PRODUCT"].contains($0.kind.uppercased()) }.prefix(4)
        if parts.isEmpty {
            return [
                ("CRC sheet 1.2mm", "RM-044 · 0.4 kg/unit · HSN 7209", "Raw"),
                ("M8 bolt set", "CS-112 · 4 nos/unit · HSN 7318", "Raw"),
                ("Paint · grey", "CS-031 · 20 ml/unit · HSN 3208", "WIP"),
                ("Packing carton", "PK-207 · 1/10 units · HSN 4819", "Consumables"),
            ]
        }
        let cats = ["Raw", "Raw", "WIP", "Consumables"]
        return parts.enumerated().map { i, m in
            let meta = [m.code, m.unit.map { "1 \($0)" }].compactMap { $0 }.joined(separator: " · ")
            return (m.name, meta.isEmpty ? m.kind : meta, cats[min(i, cats.count - 1)])
        }
    }

    private var visibleHeld: [(name: String, meta: String, cat: String)] {
        heldLines.filter { station == "All items" || $0.cat == station }
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                MtoGrabber()
                header
                crossNav
                switch tab {
                case .hold: holdBody
                case .transfer: transferBody
                case .reprint: reprintBody
                }
            }
            .padding(16)
            .padding(.bottom, 24)
        }
        .background(theme.canvas)
        .task { if model.masters.isEmpty { await model.loadMasters() } }
    }

    private var header: some View {
        MtoSheetHeader(
            theme: theme,
            title: tab == .hold ? "Job hold" : tab == .transfer ? "Stock / WIP transfer" : "Reprint invoice / job card",
            subtitle: tab == .hold
                ? "Paused WO · no stock move, no cost posting"
                : tab == .transfer
                    ? "Move at cost · no revenue · same GSTIN"
                    : "Duplicate copy · same number · no new revenue",
            onClose: { dismiss() }
        )
    }

    private var crossNav: some View {
        MtoChipRow(
            theme: theme,
            options: MtoUtilityTab.allCases.map(\.chipLabel),
            selection: Binding(
                get: { tab.chipLabel },
                set: { label in
                    if let next = MtoUtilityTab.allCases.first(where: { $0.chipLabel == label }) {
                        tab = next
                        onRoute?(next.rawValue.uppercased())
                    }
                }
            )
        )
    }

    private var holdBody: some View {
        let wo = openOrders.first?.title ?? "WO · open job"
        let plant = model.title.isEmpty ? "Plant" : model.title
        return VStack(alignment: .leading, spacing: 12) {
            MtoChipRow(theme: theme, options: ["Line 1", "Line 2", "QC bay"], selection: $line)
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 6) {
                    MtoModeChip(theme: theme, text: "\(line) · Shift A", selected: true) {}
                    MtoModeChip(theme: theme, text: "Hold · resume or release", selected: false) {}
                    MtoModeChip(theme: theme, text: "GST off", selected: false) {}
                }
            }
            MtoAmountHero(
                theme: theme,
                label: "Hold slip · \(wohCode)",
                amountText: wo,
                caption: "\(heldLines.count) components · \(plant) · \(line)"
            )
            MtoChipRow(
                theme: theme,
                options: ["All items", "Raw", "WIP", "Finished", "Consumables"],
                selection: $station
            )
            MtoLinesCard(theme: theme, title: "Held components", trailing: "\(visibleHeld.count)") {
                if visibleHeld.isEmpty {
                    Text("Nothing on hold slip")
                        .font(.plusJakarta(size: 13))
                        .foregroundStyle(MtoSheetChrome.textBody)
                } else {
                    ForEach(Array(visibleHeld.enumerated()), id: \.offset) { _, row in
                        VStack(alignment: .leading, spacing: 6) {
                            HStack {
                                Text(row.name)
                                    .font(.plusJakarta(size: 14, weight: .semibold))
                                    .foregroundStyle(OperatingV12.title)
                                Spacer()
                                Text("×1")
                                    .font(.plusJakarta(size: 13, weight: .semibold))
                                    .foregroundStyle(theme.accentSoft)
                            }
                            HStack(spacing: 6) {
                                Text(row.meta)
                                    .font(.plusJakarta(size: 11))
                                    .foregroundStyle(MtoSheetChrome.textBody)
                                    .lineLimit(2)
                                Spacer()
                                MtoModeChip(theme: theme, text: row.cat, selected: false) {}
                                MtoModeChip(theme: theme, text: "Held", selected: true, style: .tender) {}
                            }
                        }
                        .padding(12)
                        .background(theme.inset)
                        .overlay(
                            RoundedRectangle(cornerRadius: 14, style: .continuous)
                                .strokeBorder(theme.accent.opacity(0.3), lineWidth: 1)
                        )
                        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                    }
                }
            }
            VStack(alignment: .leading, spacing: 8) {
                metaRow("Notify", "Line supervisor")
                metaRow("Held by", "Operator")
                metaRow("Linked SO", openOrders.dropFirst().first?.title ?? "—")
            }
            .padding(12)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(theme.inset)
            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
            MtoWhyNote(
                theme: theme,
                text: "Hold pauses a job. No material issue, no production posting and no GST; resume continues the same job."
            )
            MtoStickyFooter(
                theme: theme,
                title: saving ? "Holding…" : "Hold job · \(wohCode)",
                caption: "No stock or cost moves while held",
                disabled: saving,
                loading: saving,
                action: { Task { await postHold(wo: wo) } }
            )
        }
    }

    private var transferBody: some View {
        VStack(alignment: .leading, spacing: 12) {
            MtoAmountHero(
                theme: theme,
                label: "Transfer slip",
                amountText: material.isEmpty ? "Select material" : material,
                caption: "\(fromLoc) → \(toLoc) · Sale impact · None"
            )
            MtoFieldLabel(text: "Material")
            MtoTextField(theme: theme, placeholder: "Part / WIP / FG", text: $material)
            MtoPickerRow(theme: theme, label: "From", value: fromLoc) { fromLoc = "Raw store" }
            MtoPickerRow(theme: theme, label: "To", value: toLoc) { toLoc = "Press line 1" }
            MtoFieldLabel(text: "Qty")
            MtoTextField(theme: theme, placeholder: "1", text: $qty, keyboard: .decimalPad)
            MtoLockedChip(theme: theme, label: "Job work out · challan locked")
            MtoWhyNote(
                theme: theme,
                text: "Transfer moves stock/WIP at cost inside the same GSTIN — never a sale. Job work out needs a challan (Unavailable until API)."
            )
            MtoStickyFooter(
                theme: theme,
                title: saving ? "Transferring…" : "Confirm transfer · 1 item",
                disabled: material.isEmpty || fromLoc == toLoc || saving,
                loading: saving,
                action: { Task { await postTransfer() } }
            )
        }
    }

    private var reprintBody: some View {
        let inv = reprintTitle.isEmpty ? (sales.first?.title ?? "No posted invoice") : reprintTitle
        let postedLines: [(name: String, meta: String, station: String)] = [
            ("Harbor flange set", "FG · J-07 · 12 nos", "Cut"),
            ("CRC sheet 2.0mm", "Raw · 48 kg", "Machine"),
            ("Packing crate · hardwood", "Consumable · 1 set", "Pack"),
            ("Plate finish · out", "Plate (out) · vendor", "Plate"),
        ]
        let visible = postedLines.filter { reprintStation == "All" || $0.station == reprintStation }
        return VStack(alignment: .leading, spacing: 12) {
            MtoAmountHero(
                theme: theme,
                label: "Tax invoice / job card",
                amountText: inv,
                caption: "Posted lines · Original print · Duplicate"
            )
            ForEach(sales.prefix(5), id: \.id) { row in
                MtoPickerRow(theme: theme, label: row.title, value: row.paymentStatus ?? "POSTED") {
                    reprintTitle = row.title
                }
            }
            MtoChipRow(
                theme: theme,
                options: ["All", "Cut", "Machine", "Plate", "QC", "Pack", "Dispatch"],
                selection: $reprintStation
            )
            MtoLinesCard(theme: theme, title: "Posted lines", trailing: "\(visible.count)") {
                if visible.isEmpty {
                    Text("No lines for \(reprintStation)")
                        .font(.plusJakarta(size: 12))
                        .foregroundStyle(MtoSheetChrome.textBody)
                } else {
                    ForEach(visible, id: \.name) { row in
                        MtoPickerRow(theme: theme, label: row.name, value: "\(row.meta) · \(row.station)")
                    }
                }
            }
            MtoChipRow(
                theme: theme,
                options: ["Customer copy", "Job card", "Lost print", "Audit"],
                selection: $reason
            )
            metaRow(
                "Original print",
                sales.first.flatMap { row -> String? in
                    guard let raw = row.occurredAt, raw.count >= 10 else { return nil }
                    return String(raw.prefix(10))
                } ?? "—"
            )
            metaRow("Payment", sales.first?.paymentStatus ?? "—")
            metaRow("Print", "Locked · chrome only")
            MtoLockedChip(theme: theme, label: "Thermal / PDF print · Unavailable")
            MtoWhyNote(
                theme: theme,
                text: "Reprint re-renders the same invoice or job card. It never creates a new SALE, AR, or invoice number."
            )
            MtoStickyFooter(
                theme: theme,
                title: "Reprint · duplicate copy",
                caption: "Same number · no new revenue",
                disabled: sales.isEmpty,
                action: { dismiss() }
            )
        }
    }

    private func metaRow(_ label: String, _ value: String) -> some View {
        HStack {
            Text(label)
                .font(.plusJakarta(size: 12, weight: .medium))
                .foregroundStyle(MtoSheetChrome.textBody)
            Spacer()
            Text(value)
                .font(.plusJakarta(size: 12, weight: .semibold))
                .foregroundStyle(OperatingV12.title)
        }
    }

    private func postHold(wo: String) async {
        saving = true
        defer { saving = false }
        var contract = OperatingContractInput()
        contract.orderStatus = "HOLD"
        contract.clientKey = wohCode
        contract.holdStatus = "HELD"
        let ok = await model.record(OperatingEventInput(
            momentId: model.momentId,
            eventType: "ORDER",
            title: "Hold · \(wohCode) · \(wo)",
            note: "HOLD · no stock · no cost · no GST · line=\(line) · \(wohCode)",
            channelName: line,
            contract: contract
        ))
        if ok { dismiss() }
    }

    private func postTransfer() async {
        saving = true
        defer { saving = false }
        var contract = OperatingContractInput()
        contract.stockAction = "TRANSFER"
        contract.stockReason = "TRANSFER"
        let ok = await model.record(OperatingEventInput(
            momentId: model.momentId,
            eventType: "STOCK",
            title: "Transfer · \(material)",
            note: "TRANSFER · \(fromLoc) → \(toLoc) · same-GSTIN · no revenue",
            channelName: toLoc,
            counterpartyName: fromLoc,
            quantity: qty,
            unit: "nos",
            lineItems: [.init(itemName: material, quantity: qty, unit: "nos")],
            contract: contract
        ))
        if ok { dismiss() }
    }
}

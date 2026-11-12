import SwiftUI

/// SVC-QA-HOLD / TRANSFER / REPRINT — Figma `2533:29334`, `2533:29448`, `2533:29538`.
/// Service ticket sheets (not trading Hold/Transfer/Reprint layouts).
/// Hold / Transfer never post revenue; Reprint re-renders a posted invoice.
enum ServiceUtilityTab: String, CaseIterable, Identifiable {
    case hold
    case transfer
    case reprint

    var id: String { rawValue }

    var chipLabel: String {
        switch self {
        case .hold: return "Hold"
        case .transfer: return "Transfer"
        case .reprint: return "Reprint"
        }
    }

    var modeCode: String { rawValue.uppercased() }

    static func from(mode: String) -> ServiceUtilityTab {
        switch mode.uppercased() {
        case "TRANSFER": return .transfer
        case "REPRINT": return .reprint
        default: return .hold
        }
    }
}

struct ServiceHoldTransferReprintSheet: View {
    let theme: OperatingTheme
    let initial: ServiceUtilityTab
    @ObservedObject var model: BusinessOperatingModel
    var onDismiss: () -> Void = {}

    @Environment(\.dismiss) private var dismiss
    @State private var tab: ServiceUtilityTab
    @State private var category = "All services"
    @State private var room = ""
    @State private var fromStaff = "Meera"
    @State private var toStaff = "Priya"
    @State private var toRoom = ""
    @State private var selectedHold: Set<String> = []
    @State private var selectedTransfer: Set<String> = []
    @State private var invoiceNo = ""
    @State private var saving = false
    @State private var showToast = false
    @State private var submitError: String?

    private let holdId: String
    private let transferId: String

    private var holdClient: String {
        appointments.first?.counterpartyName?.nilIfBlank.map { "\($0) · walk-in" }
            ?? "Ananya Mehta · walk-in"
    }

    private var appointments: [OperatingActivityPayload] {
        (model.projection?.activity ?? []).filter {
            let t = $0.eventType.uppercased()
            return t == "APPOINTMENT" || t == "BOOKING"
        }
    }

    private var postedCharges: [OperatingActivityPayload] {
        (model.projection?.activity ?? []).filter {
            let t = $0.eventType.uppercased()
            return t == "SERVICE" || t == "SALE"
        }
    }

    init(
        theme: OperatingTheme,
        mode: String,
        model: BusinessOperatingModel,
        onDismiss: @escaping () -> Void = {}
    ) {
        self.theme = theme
        self.initial = ServiceUtilityTab.from(mode: mode)
        self.model = model
        self.onDismiss = onDismiss
        _tab = State(initialValue: ServiceUtilityTab.from(mode: mode))
        let stamp = Int(Date().timeIntervalSince1970) % 100
        holdId = String(format: "HLD-%02d", stamp)
        transferId = String(format: "TRF-%03d", stamp)
    }

    private var rooms: [String] {
        let source = model.masters.isEmpty ? (model.projection?.masters ?? []) : model.masters
        let fromMasters = source
            .filter { ["LOCATION", "ROOM", "CHAIR", "CHANNEL"].contains($0.kind.uppercased()) }
            .map(\.name)
        let channels = model.projection?.life?.channels ?? []
        let merged = Array(Set(fromMasters + channels)).filter { !$0.isEmpty }.sorted()
        return merged.isEmpty ? ["Chair 1", "Chair 2", "Spa room"] : merged
    }

    private var staff: [String] {
        let source = model.masters.isEmpty ? (model.projection?.masters ?? []) : model.masters
        let names = source
            .filter { ["STAFF", "EMPLOYEE", "THERAPIST"].contains($0.kind.uppercased()) }
            .map(\.name)
        return names.isEmpty ? ["Meera", "Priya", "Ravi"] : Array(Set(names)).sorted()
    }

    private var categories: [String] {
        switch tab {
        case .hold: return ["All services", "Hair", "Skin", "Spa", "Wellness"]
        case .transfer: return ["All services", "Hair", "Wellness"]
        case .reprint: return ["All services", "Hair", "Spa"]
        }
    }

    private var holdLines: [ServiceTicketLine] { Self.demoHold }

    private var transferLines: [ServiceTicketLine] {
        if appointments.isEmpty { return Self.demoTransfer }
        return appointments.prefix(4).enumerated().map { index, row in
            let meta = [
                String(row.occurredAt.suffix(5)).count == 5 ? String(row.occurredAt.suffix(5)) : nil,
                row.counterpartyName?.nilIfBlank,
                row.quantity?.nilIfBlank.map { "\($0) min" },
            ].compactMap { $0 }.joined(separator: " · ")
            return ServiceTicketLine(
                id: row.id.isEmpty ? "xfer-\(index)" : row.id,
                title: row.title.isEmpty ? "Booking" : row.title,
                qtyLabel: "×1",
                meta: meta.isEmpty ? (row.subtitle ?? "") : meta,
                category: "Hair",
                badge: "Move",
                sourceEventId: row.id.nilIfBlank
            )
        }
    }

    private var reprintLines: [ServiceTicketLine] {
        guard let row = postedCharges.first else { return Self.demoReprint }
        let meta = [
            row.amount?.nilIfBlank.map { "₹\($0)" },
            gstOn ? "GST posted" : nil,
        ].compactMap { $0 }.joined(separator: " · ")
        return [
            ServiceTicketLine(
                id: row.id.isEmpty ? "inv-1" : row.id,
                title: row.title.isEmpty ? "Posted charge" : row.title,
                qtyLabel: "×\(row.quantity?.nilIfBlank ?? "1")",
                meta: meta.isEmpty ? "Posted · same invoice" : meta,
                category: "Hair",
                badge: "Posted",
                sourceEventId: row.id.nilIfBlank
            ),
        ]
    }

    private var lines: [ServiceTicketLine] {
        switch tab {
        case .hold: return holdLines
        case .transfer: return transferLines
        case .reprint: return reprintLines
        }
    }

    private var gstOn: Bool { model.projection?.gstTracking == "YES" }

    private var canSubmit: Bool {
        switch tab {
        case .hold: return !selectedHold.isEmpty && !room.isEmpty
        case .transfer: return !selectedTransfer.isEmpty && !toStaff.isEmpty && !toRoom.isEmpty && fromStaff != toStaff
        case .reprint: return !invoiceNo.isEmpty
        }
    }

    var body: some View {
        ZStack(alignment: .top) {
            ScrollView {
                VStack(alignment: .leading, spacing: 12) {
                    TealGrabber()
                    TealSheetHeader(
                        theme: theme,
                        title: ServiceCaptureCopy.sheetTitle(tab.modeCode),
                        subtitle: subtitle,
                        onClose: close
                    )

                    modeChips
                    roomChips
                    contextChips

                    TealTicketHero(
                        theme: theme,
                        eyebrow: slipLabel,
                        code: slipId,
                        title: slipTitle,
                        subtitle: slipSubtitle
                    )

                    TealChipRow(theme: theme, options: categories, selection: $category, style: .lime)

                    if tab == .transfer {
                        staffPickers
                    }

                    ForEach(filteredLines) { line in
                        lineRow(line)
                    }

                    TealMetaBlock(theme: theme, rows: metaRows)

                    TealWhyBox(theme: theme, text: ServiceCaptureCopy.why(tab.modeCode))
                    if let submitError, !submitError.isEmpty {
                        Text(submitError)
                            .font(.plusJakarta(size: 12, weight: .medium))
                            .foregroundStyle(theme.negative)
                    }
                    TealStickyFooter(
                        theme: theme,
                        title: saving ? "Saving…" : ctaLabel,
                        caption: caption,
                        disabled: !canSubmit,
                        loading: saving,
                        action: { Task { await submit() } }
                    )
                }
                .padding(16)
                .padding(.bottom, 28)
            }
            .background(theme.canvas)

            if showToast {
                TealSavedToast().padding(.top, 12)
            }
        }
        .onAppear {
            if room.isEmpty { room = rooms.first ?? "Chair 1" }
            if toRoom.isEmpty { toRoom = rooms.first ?? room }
            selectedHold = Set(holdLines.map(\.id))
            selectedTransfer = Set(transferLines.map(\.id))
            if invoiceNo.isEmpty {
                invoiceNo = postedInvoiceNumber
            }
            if staff.count >= 2 {
                fromStaff = staff[0]
                toStaff = staff[1]
            }
        }
    }

    private var subtitle: String {
        switch tab {
        case .hold: return "Held slot · no revenue, no charge"
        case .transfer: return "Move a booking to another pro or room · no revenue"
        case .reprint: return "Re-print posted charge · same invoice no."
        }
    }

    private var slipLabel: String {
        switch tab {
        case .hold: return "Hold slip"
        case .transfer: return "Transfer slip"
        case .reprint: return "Tax invoice"
        }
    }

    private var slipId: String {
        switch tab {
        case .hold: return holdId
        case .transfer: return transferId
        case .reprint: return invoiceNo.isEmpty ? "INV-—" : invoiceNo
        }
    }

    private var slipTitle: String {
        switch tab {
        case .hold: return holdClient
        case .transfer: return "\(fromStaff) → \(toStaff)"
        case .reprint: return invoiceNo.isEmpty ? "BC-INV-2048" : invoiceNo
        }
    }

    private var slipSubtitle: String {
        switch tab {
        case .hold:
            let count = max(selectedHold.count, 1)
            let brand = model.title.trimmingCharacters(in: .whitespacesAndNewlines)
            return "\(count) service\(count == 1 ? "" : "s") · \(brand.isEmpty ? "Bandra" : brand) · \(room) · 4:00 pm"
        case .transfer:
            let fromRoom = rooms.count > 1 ? rooms[1] : "Room 2"
            let n = max(selectedTransfer.count, 1)
            return "\(fromRoom) → \(toRoom) · \(n) booking\(n == 1 ? "" : "s") today"
        case .reprint:
            return gstOn
                ? "B2C · place of supply MH · CGST 9% + SGST 9%"
                : "B2C · posted charge · same invoice no."
        }
    }

    private var caption: String {
        switch tab {
        case .hold: return "Draft only — revenue + GST post on Finalize Charge"
        case .transfer: return "Transfer never posts a charge"
        case .reprint: return "Same invoice no. — never duplicates revenue"
        }
    }

    private var ctaLabel: String {
        switch tab {
        case .hold: return "Hold slot · \(holdId)"
        case .transfer: return "Confirm transfer · \(max(selectedTransfer.count, 1)) bookings"
        case .reprint: return ServiceCaptureCopy.cta("REPRINT")
        }
    }

    private var metaRows: [(String, String)] {
        switch tab {
        case .hold:
            return [
                ("Notify", "SMS + WhatsApp"),
                ("Held by", "Front desk · \(staff.first ?? "Ravi")"),
                ("Linked booking", "Not booked · draft"),
            ]
        case .transfer:
            return [
                ("Price", "Unchanged · senior rate needs approval"),
                ("New pro", "\(toStaff) · \(toRoom)"),
                ("Revenue impact", "None · charge posts once on Finalize"),
            ]
        case .reprint:
            return [
                ("Original print", "17:05 · \(staff.first ?? "Ravi") · Front desk"),
                ("Reprint reason", "Client asked for email copy"),
                ("Payment", "Settled · UPI"),
            ]
        }
    }

    private var modeChips: some View {
        // Figma utility trio uses lime fill when selected (Hold / Transfer / Reprint).
        HStack(spacing: 6) {
            ForEach(ServiceUtilityTab.allCases) { option in
                TealModeChip(
                    theme: theme,
                    text: option.chipLabel,
                    selected: tab == option,
                    style: .lime
                ) {
                    tab = option
                    category = "All services"
                }
            }
            Spacer(minLength: 0)
        }
    }

    private var roomChips: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 6) {
                ForEach(rooms, id: \.self) { label in
                    TealModeChip(
                        theme: theme,
                        text: label,
                        selected: room == label,
                        style: .accent
                    ) {
                        room = label
                        if tab == .transfer { toRoom = label }
                    }
                }
            }
        }
    }

    private var contextChips: some View {
        let gstChip = gstOn ? "GST on" : "GST off"
        let chips: [(String, Bool)] = {
            switch tab {
            case .hold:
                return [
                    ("\(room) · \(staff.first ?? "Priya") · 4:00 pm", true),
                    ("Hold 15 min · release or book", false),
                    (gstChip, false),
                ]
            case .transfer:
                return [
                    ("\(fromStaff) → \(toStaff)", true),
                    ("Same GSTIN", false),
                    (gstChip, false),
                ]
            case .reprint:
                return [
                    ("Invoice \(invoiceNo.isEmpty ? "—" : invoiceNo)", true),
                    ("Posted 17:05 · UPI", false),
                    (gstChip, false),
                ]
            }
        }()
        return ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 6) {
                ForEach(Array(chips.enumerated()), id: \.offset) { _, item in
                    contextChip(item.0, selected: item.1)
                }
            }
        }
    }

    private func contextChip(_ text: String, selected: Bool) -> some View {
        Text(text)
            .font(.plusJakarta(size: 12, weight: selected ? .bold : .medium))
            .foregroundStyle(selected ? TealSheetChrome.inkOnAccent : OperatingV12.subtitle)
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
            .background(theme.inset)
            .overlay(
                Capsule()
                    .strokeBorder(selected ? Color(hex: "#FEF3C7") : theme.accent, lineWidth: 1)
            )
            .clipShape(Capsule())
            .shadow(color: selected ? Color(hex: "#FEF3C7").opacity(0.25) : .clear, radius: 8, y: 0)
    }

    private var staffPickers: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 6) {
                ForEach(Array(staff.prefix(4)), id: \.self) { name in
                    TealModeChip(theme: theme, text: "From \(name)", selected: fromStaff == name) {
                        fromStaff = name
                        if toStaff == name {
                            toStaff = staff.first(where: { $0 != name }) ?? name
                        }
                    }
                }
                ForEach(Array(staff.prefix(4)), id: \.self) { name in
                    TealModeChip(theme: theme, text: "To \(name)", selected: toStaff == name) {
                        toStaff = name
                        if fromStaff == name {
                            fromStaff = staff.first(where: { $0 != name }) ?? name
                        }
                    }
                }
            }
        }
    }

    private var filteredLines: [ServiceTicketLine] {
        guard category != "All services" else { return lines }
        return lines.filter { $0.category.compare(category, options: .caseInsensitive) == .orderedSame }
    }

    private var postedInvoiceNumber: String {
        if let hit = postedCharges.first {
            if let key = hit.clientKey?.nilIfBlank { return key }
            let tail = String(hit.id.suffix(4)).uppercased()
            return "BC-INV-\(tail)"
        }
        return "BC-INV-2048"
    }

    private func lineRow(_ line: ServiceTicketLine) -> some View {
        let selected: Bool = {
            switch tab {
            case .hold: return selectedHold.contains(line.id)
            case .transfer: return selectedTransfer.contains(line.id)
            case .reprint: return true
            }
        }()
        let badge: String = {
            switch tab {
            case .hold: return selected ? "Held" : "Skip"
            case .transfer: return selected ? "Move" : "Skip"
            case .reprint: return "Posted"
            }
        }()
        let creamBadge = line.id == "hold-3" || tab == .transfer
        return Button {
            switch tab {
            case .hold:
                if selectedHold.contains(line.id) { selectedHold.remove(line.id) } else { selectedHold.insert(line.id) }
            case .transfer:
                if selectedTransfer.contains(line.id) { selectedTransfer.remove(line.id) } else { selectedTransfer.insert(line.id) }
            case .reprint:
                break
            }
        } label: {
            VStack(alignment: .leading, spacing: 6) {
                HStack {
                    Text(line.title)
                        .font(.plusJakarta(size: 14, weight: .bold))
                        .foregroundStyle(OperatingV12.title)
                        .frame(maxWidth: .infinity, alignment: .leading)
                    Text(line.qtyLabel)
                        .font(.plusJakarta(size: 14, weight: .bold))
                        .foregroundStyle(Color(hex: "#FEF3C7"))
                }
                HStack(spacing: 6) {
                    Text(line.meta)
                        .font(.plusJakarta(size: 11, weight: .medium))
                        .foregroundStyle(OperatingV12.subtitle)
                        .frame(maxWidth: .infinity, alignment: .leading)
                    Text(line.category)
                        .font(.plusJakarta(size: 12, weight: .medium))
                        .foregroundStyle(OperatingV12.subtitle)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 8)
                        .overlay(Capsule().strokeBorder(theme.accent, lineWidth: 1))
                    Text(badge)
                        .font(.plusJakarta(size: 12, weight: .bold))
                        .foregroundStyle(TealSheetChrome.inkOnAccent)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 8)
                        .background(
                            selected
                                ? (creamBadge ? Color(hex: "#FEF3C7") : Color(hex: "#22C55E"))
                                : theme.inset
                        )
                        .overlay(
                            Capsule().strokeBorder(
                                selected ? Color(hex: "#FEF3C7") : TealSheetChrome.chipBorder,
                                lineWidth: 1
                            )
                        )
                        .clipShape(Capsule())
                }
            }
            .padding(12)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(theme.inset)
            .overlay(
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .strokeBorder(
                        selected
                            ? (creamBadge ? Color(hex: "#FEF3C7").opacity(0.4) : TealSheetChrome.lime.opacity(0.3))
                            : theme.accent.opacity(0.25),
                        lineWidth: 1
                    )
            )
            .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
            .opacity(selected || tab == .reprint ? 1 : 0.55)
        }
        .buttonStyle(.plain)
        .disabled(tab == .reprint)
    }

    /// Mirror Android `ServiceHoldTransferReprintSheet` CTA:
    /// Hold / Transfer → `POST …/operating-events` with `amount: nil` (no revenue / GST).
    /// Reprint → local re-render only (no new charge).
    private func submit() async {
        saving = true
        submitError = nil
        defer { saving = false }

        switch tab {
        case .hold:
            let lines = holdLines.filter { selectedHold.contains($0.id) }.map {
                OperatingEventInput.OperatingLineItem(
                    itemName: $0.title,
                    quantity: $0.qtyLabel.replacingOccurrences(of: "×", with: "").nilIfBlank ?? "1",
                    unit: "session",
                    unitPrice: nil
                )
            }
            var contract = OperatingContractInput()
            contract.holdStatus = "HELD"
            contract.holdReason = "Hold 15 min · release or book"
            contract.staffName = staff.first
            contract.locationName = room.nilIfBlank
            contract.clientKey = holdId
            contract.taxMode = "NOT_CONFIGURED"
            let input = OperatingEventInput(
                momentId: model.momentId,
                eventType: "APPOINTMENT",
                title: "Hold · \(holdClient)",
                amount: nil,
                note: "Hold reserves a slot only · \(holdId)",
                channelName: room.nilIfBlank,
                counterpartyName: holdClient
                    .components(separatedBy: " ·")
                    .first?
                    .trimmingCharacters(in: .whitespacesAndNewlines)
                    .nilIfBlank,
                lineItems: lines.isEmpty ? nil : lines,
                contract: contract,
                paymentStatus: "NONE"
            )
            let ok = await model.record(input)
            guard ok else {
                submitError = model.writeError ?? "Could not hold the slot"
                return
            }

        case .transfer:
            let moving = transferLines.filter { selectedTransfer.contains($0.id) }
            let parent = moving.first?.sourceEventId
            var contract = OperatingContractInput()
            contract.staffName = toStaff
            contract.locationName = toRoom.nilIfBlank
            contract.clientKey = transferId
            contract.taxMode = "NOT_CONFIGURED"
            let input = OperatingEventInput(
                momentId: model.momentId,
                eventType: "APPOINTMENT",
                title: "Transfer · \(fromStaff) → \(toStaff)",
                amount: nil,
                note: "Staff/room transfer · \(transferId) · no revenue",
                channelName: toRoom.nilIfBlank,
                parentEventId: parent,
                lineItems: moving.map {
                    OperatingEventInput.OperatingLineItem(
                        itemName: $0.title,
                        quantity: "1",
                        unit: "booking",
                        unitPrice: nil
                    )
                }.nilIfEmpty,
                contract: contract,
                paymentStatus: "NONE",
                revisesEventId: parent
            )
            let ok = await model.record(input)
            guard ok else {
                submitError = model.writeError ?? "Could not transfer the booking"
                return
            }

        case .reprint:
            // Read-only: same invoice number — never posts a new SERVICE charge.
            break
        }

        withAnimation { showToast = true }
        try? await Task.sleep(nanoseconds: 700_000_000)
        close()
    }

    private func close() {
        onDismiss()
        dismiss()
    }

    private static let demoHold: [ServiceTicketLine] = [
        .init(id: "hold-1", title: "Haircut & style", qtyLabel: "×1", meta: "45 min · ₹899 · SAC 999721 · 18%", category: "Hair", badge: "Held"),
        .init(id: "hold-2", title: "Hair spa", qtyLabel: "×2", meta: "30 min · ₹1,200 · SAC 999721 · 18%", category: "Hair", badge: "Held"),
        .init(id: "hold-3", title: "Add-on: head massage", qtyLabel: "×1", meta: "15 min · ₹400 · SAC 999722 · 18%", category: "Skin", badge: "Held"),
        .init(id: "hold-4", title: "Package session 2/3", qtyLabel: "×2", meta: "Keratin pkg · prepaid · no new revenue", category: "Wellness", badge: "Held"),
    ]

    private static let demoTransfer: [ServiceTicketLine] = [
        .init(id: "xfer-1", title: "S-120 Signature facial", qtyLabel: "×1", meta: "4:00 pm · Ananya Mehta · 60 min", category: "Hair", badge: "Move"),
        .init(id: "xfer-2", title: "S-124 Haircut & style", qtyLabel: "×1", meta: "5:30 pm · Rohan Kapoor · 45 min", category: "Hair", badge: "Move"),
    ]

    private static let demoReprint: [ServiceTicketLine] = [
        .init(id: "inv-1", title: "Signature facial", qtyLabel: "×1", meta: "SAC 999721 · ₹4,500 · GST 18%", category: "Hair", badge: "Posted"),
        .init(id: "inv-2", title: "Head massage add-on", qtyLabel: "×1", meta: "SAC 999722 · ₹400 · GST 18%", category: "Spa", badge: "Posted"),
    ]
}

private struct ServiceTicketLine: Identifiable {
    let id: String
    let title: String
    let qtyLabel: String
    let meta: String
    let category: String
    let badge: String
    var sourceEventId: String? = nil
}

private extension Array {
    var nilIfEmpty: Self? { isEmpty ? nil : self }
}

import SwiftUI

/// Status / Assignment board — Figma `RST-QA-08` `2400:30488`.
/// Prepare / Ready / Fulfil modes bump kitchen state only — never posts SALE.
/// Updates the same ORDER via `revisesEventId` (ops PATCH), matching Fire semantics.
struct RestaurantStatusSheet: View {
    let theme: OperatingTheme
    @ObservedObject var model: BusinessOperatingModel

    @Environment(\.dismiss) private var dismiss

    private enum BoardMode: String, CaseIterable, Identifiable {
        case prepare = "Prepare"
        case ready = "Ready"
        case fulfil = "Fulfil"
        var id: String { rawValue }
    }

    private enum TargetStatus: String, CaseIterable, Identifiable {
        case preparing = "Preparing"
        case ready = "Ready"
        case served = "Served"
        var id: String { rawValue }

        var orderStatus: String {
            switch self {
            case .preparing: return "PREPARING"
            case .ready: return "READY"
            case .served: return "COMPLETED"
            }
        }

        var nextLabel: String {
            switch self {
            case .preparing: return "Ready at pass"
            case .ready: return "Fulfil / serve"
            case .served: return "Closed"
            }
        }
    }

    private struct BoardLine: Identifiable {
        let id: String
        let name: String
        let meta: String
        let statusLabel: String
        let isReady: Bool
        let assignee: String
    }

    @State private var mode: BoardMode = .ready
    @State private var target: TargetStatus = .ready
    @State private var ticketId: String?
    @State private var dueLabel = "Today · 7:30 pm"
    @State private var assignee = ""
    @State private var saving = false

    private var placeName: String {
        let title = model.title.trimmingCharacters(in: .whitespacesAndNewlines)
        if title.isEmpty { return "Kitchen" }
        if title.localizedCaseInsensitiveContains("POS") { return title }
        return "\(title) POS"
    }

    private var tickets: [OperatingActivityPayload] {
        (model.projection?.activity ?? [])
            .filter { $0.eventType.uppercased() == "ORDER" }
            .filter {
                let status = ($0.status ?? "").uppercased()
                return status != "CANCELLED"
            }
            .sorted { $0.occurredAt > $1.occurredAt }
    }

    private var filteredTickets: [OperatingActivityPayload] {
        tickets.filter { ticketMatchesMode($0) }
    }

    private var selectedTicket: OperatingActivityPayload? {
        if let ticketId, let match = tickets.first(where: { $0.id == ticketId }) {
            return match
        }
        return filteredTickets.first ?? tickets.first
    }

    private var kotLabel: String {
        if let key = selectedTicket?.clientKey?.trimmingCharacters(in: .whitespaces), !key.isEmpty {
            return key
        }
        return "KOT"
    }

    private var stationLabel: String {
        let loc = selectedTicket?.locationName?.trimmingCharacters(in: .whitespaces) ?? ""
        return loc.isEmpty ? "Hot pass" : loc
    }

    private var assigneeLabel: String {
        let typed = assignee.trimmingCharacters(in: .whitespaces)
        if !typed.isEmpty { return typed }
        if let fromNote = selectedTicket.flatMap({ extractAssignee(from: $0.note ?? "") }), !fromNote.isEmpty {
            return fromNote
        }
        // Display fixture only when no ticket-bound assignee exists — never invent on post.
        return selectedTicket == nil ? "Unavailable" : "Chef Ama"
    }

    private var assigneeForPost: String? {
        let typed = assignee.trimmingCharacters(in: .whitespaces)
        if !typed.isEmpty { return typed }
        return extractAssignee(from: selectedTicket?.note ?? "")
    }

    private var contextLine: String {
        let table = selectedTicket?.tableNumber?.trimmingCharacters(in: .whitespaces) ?? ""
        let tableBit = table.isEmpty ? nil : "Table \(table)"
        let readyBit: String = {
            switch target {
            case .preparing: return "Preparing · \(stationLabel)"
            case .ready: return "Ready at \(stationLabel.lowercased().contains("pass") ? stationLabel.lowercased() : "\(stationLabel) pass")"
            case .served: return "Served · \(stationLabel)"
            }
        }()
        let kitchen = model.title.trimmingCharacters(in: .whitespacesAndNewlines)
        let kitchenBit = kitchen.isEmpty ? "Kitchen" : "\(kitchen) Kitchen"
        let floor = selectedTicket?.channelName?.trimmingCharacters(in: .whitespaces)
        return [
            tableBit,
            readyBit,
            kitchenBit,
            floor.flatMap { $0.isEmpty ? nil : $0 },
        ]
        .compactMap { $0 }
        .joined(separator: " · ")
    }

    private var ticketDetail: String {
        let table = selectedTicket?.tableNumber?.trimmingCharacters(in: .whitespaces) ?? ""
        let tableBit = table.isEmpty ? nil : "Table \(table)"
        let stateBit: String = {
            switch target {
            case .preparing: return "Preparing at \(stationLabel.lowercased())"
            case .ready: return "Ready at \(stationLabel.lowercased())"
            case .served: return "Served · \(stationLabel)"
            }
        }()
        return [tableBit, stateBit].compactMap { $0 }.joined(separator: " · ")
    }

    private var boardLines: [BoardLine] {
        parseBoardLines(selectedTicket)
    }

    private var canSubmit: Bool {
        selectedTicket != nil && !saving
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                EmberGrabber()
                EmberSheetHeader(
                    title: "Status / Assignment",
                    subtitle: selectedTicket == nil ? "Prep · ready · fulfil · no revenue" : contextLine,
                    onClose: { dismiss() }
                )

                EmberChipRow(
                    options: BoardMode.allCases.map(\.rawValue),
                    selection: Binding(
                        get: { mode.rawValue },
                        set: { raw in
                            if let next = BoardMode(rawValue: raw) {
                                applyMode(next)
                            }
                        }
                    )
                )

                contextChips

                ticketHero

                ticketPicker

                linesSection

                metaCard

                EmberChipRow(
                    options: TargetStatus.allCases.map(\.rawValue),
                    selection: Binding(
                        get: { target.rawValue },
                        set: { raw in
                            if let next = TargetStatus(rawValue: raw) {
                                target = next
                                syncMode(from: next)
                            }
                        }
                    )
                )

                duePicker

                EmberWhyBox(text: "Operational state only — does not post revenue.")

                EmberStickyFooter(
                    title: saving ? "Updating…" : "Update status",
                    caption: "Saved as draft until posted",
                    disabled: !canSubmit,
                    loading: saving,
                    action: { Task { await submit() } }
                )
            }
            .padding(.horizontal, 20)
            .padding(.top, 12)
            .padding(.bottom, 28)
        }
        .background(EmberSheetChrome.sheet)
        .task {
            if ticketId == nil {
                ticketId = filteredTickets.first?.id ?? tickets.first?.id
            }
            hydrateAssignee()
        }
        .onChange(of: ticketId) { _, _ in
            hydrateAssignee()
        }
        .onChange(of: mode) { _, _ in
            if let selected = selectedTicket, !ticketMatchesMode(selected) {
                ticketId = filteredTickets.first?.id ?? tickets.first?.id
            }
        }
    }

    // MARK: - Sections

    @ViewBuilder
    private var contextChips: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 6) {
                EmberModeChip(text: placeName, selected: true, style: .channel) {}
                EmberModeChip(text: kotLabel, selected: false) {
                    cycleTicket()
                }
                EmberModeChip(text: stationLabel, selected: false) {}
            }
        }
    }

    private var ticketHero: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text("Ticket")
                .font(.plusJakarta(size: 12, weight: .medium))
                .foregroundStyle(EmberSheetChrome.textBody)
            Text(kotLabel)
                .font(.plusJakarta(size: 32, weight: .heavy))
                .foregroundStyle(EmberSheetChrome.textPrimary)
                .lineLimit(1)
                .minimumScaleFactor(0.7)
            Text(selectedTicket == nil ? "No open kitchen ticket" : ticketDetail)
                .font(.plusJakarta(size: 12, weight: .medium))
                .foregroundStyle(EmberSheetChrome.gold)
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(EmberSheetChrome.sheet)
        .overlay(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .strokeBorder(EmberSheetChrome.ember, lineWidth: 1.5)
        )
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
        .shadow(color: EmberSheetChrome.ember.opacity(0.33), radius: 12, y: 4)
    }

    @ViewBuilder
    private var ticketPicker: some View {
        let pool = filteredTickets.isEmpty ? tickets : filteredTickets
        if pool.isEmpty {
            EmberEmptyCard(
                title: "No open tickets",
                bodyText: "Send a KOT first. Status / Assignment never posts revenue."
            )
        } else if pool.count > 1 {
            EmberLinesCard(
                title: "Open tickets",
                trailing: "\(pool.count) open"
            ) {
                ForEach(pool.prefix(6)) { row in
                    Button {
                        ticketId = row.id
                    } label: {
                        HStack(spacing: 8) {
                            VStack(alignment: .leading, spacing: 2) {
                                Text(kotKey(for: row) ?? row.title)
                                    .font(.plusJakarta(size: 13, weight: .bold))
                                    .foregroundStyle(EmberSheetChrome.textPrimary)
                                Text(ticketPickerMeta(row))
                                    .font(.plusJakarta(size: 11))
                                    .foregroundStyle(EmberSheetChrome.textBody)
                                    .lineLimit(1)
                            }
                            Spacer(minLength: 0)
                            Text(orderStatusLabel(row.status))
                                .font(.plusJakarta(size: 11, weight: .bold))
                                .foregroundStyle(EmberSheetChrome.gold)
                        }
                        .padding(.vertical, 8)
                        .padding(.horizontal, 4)
                        .background(
                            row.id == selectedTicket?.id
                                ? EmberSheetChrome.ember.opacity(0.12)
                                : Color.clear
                        )
                        .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }

    @ViewBuilder
    private var linesSection: some View {
        if selectedTicket == nil {
            EmptyView()
        } else if boardLines.isEmpty {
            EmberEmptyCard(
                title: "No lines on ticket",
                bodyText: "Ticket state still updates — lines appear after Send KOT."
            )
        } else {
            VStack(alignment: .leading, spacing: 8) {
                ForEach(boardLines) { line in
                    boardLineRow(line)
                }
            }
        }
    }

    private func boardLineRow(_ line: BoardLine) -> some View {
        HStack(alignment: .center, spacing: 8) {
            VStack(alignment: .leading, spacing: 2) {
                Text(line.name)
                    .font(.plusJakarta(size: 14, weight: .bold))
                    .foregroundStyle(EmberSheetChrome.textPrimary)
                Text(line.meta)
                    .font(.plusJakarta(size: 11))
                    .foregroundStyle(EmberSheetChrome.textBody)
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            VStack(alignment: .trailing, spacing: 4) {
                Text(line.statusLabel)
                    .font(.plusJakarta(size: 11, weight: .bold))
                    .foregroundStyle(line.isReady ? EmberSheetChrome.tenderSelected : EmberSheetChrome.amber)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 4)
                    .background(
                        (line.isReady ? EmberSheetChrome.tenderSelected : EmberSheetChrome.amber)
                            .opacity(0.20)
                    )
                    .overlay(
                        Capsule().strokeBorder(
                            line.isReady ? EmberSheetChrome.tenderSelected : EmberSheetChrome.amber,
                            lineWidth: 1
                        )
                    )
                    .clipShape(Capsule())
                Text(line.assignee)
                    .font(.plusJakarta(size: 11, weight: .medium))
                    .foregroundStyle(EmberSheetChrome.gold)
            }
        }
        .padding(12)
        .background(EmberSheetChrome.sheet)
        .overlay(
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .strokeBorder(EmberSheetChrome.chip, lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
    }

    private var metaCard: some View {
        VStack(alignment: .leading, spacing: 8) {
            metaRow(label: "Station", value: stationLabel, valueGold: false, labelBold: false)
            metaRow(label: "Assignee", value: assigneeLabel, valueGold: false, labelBold: false)
            metaRow(label: "Next", value: target.nextLabel, valueGold: true, labelBold: true)
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(EmberSheetChrome.sheet)
        .overlay(
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .strokeBorder(EmberSheetChrome.ember.opacity(0.27), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
    }

    private func metaRow(label: String, value: String, valueGold: Bool, labelBold: Bool) -> some View {
        HStack(spacing: 8) {
            Text(label)
                .font(.plusJakarta(size: 13, weight: labelBold ? .bold : .regular))
                .foregroundStyle(labelBold ? EmberSheetChrome.textPrimary : EmberSheetChrome.textBody)
            Spacer(minLength: 0)
            Text(value)
                .font(.plusJakarta(size: 13, weight: .bold))
                .foregroundStyle(valueGold ? EmberSheetChrome.gold : EmberSheetChrome.textPrimary)
        }
    }

    private var duePicker: some View {
        VStack(alignment: .leading, spacing: 6) {
            EmberFieldLabel(text: "Due")
            Button {
                dueLabel = nextDueLabel(dueLabel)
            } label: {
                HStack(spacing: 8) {
                    Text(dueLabel)
                        .font(.plusJakarta(size: 14, weight: .medium))
                        .foregroundStyle(EmberSheetChrome.textPrimary)
                        .frame(maxWidth: .infinity, alignment: .leading)
                    Text("›")
                        .font(.plusJakarta(size: 16, weight: .bold))
                        .foregroundStyle(EmberSheetChrome.textBody)
                }
                .padding(12)
                .background(EmberSheetChrome.sheet)
                .overlay(
                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                        .strokeBorder(Color(hex: "#C2410C"), lineWidth: 1)
                )
                .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
            }
            .buttonStyle(.plain)
        }
    }

    // MARK: - Actions

    private func applyMode(_ next: BoardMode) {
        mode = next
        switch next {
        case .prepare: target = .preparing
        case .ready: target = .ready
        case .fulfil: target = .served
        }
    }

    private func syncMode(from status: TargetStatus) {
        switch status {
        case .preparing: mode = .prepare
        case .ready: mode = .ready
        case .served: mode = .fulfil
        }
    }

    private func cycleTicket() {
        let pool = filteredTickets.isEmpty ? tickets : filteredTickets
        guard !pool.isEmpty else { return }
        guard let current = selectedTicket,
              let index = pool.firstIndex(where: { $0.id == current.id })
        else {
            ticketId = pool.first?.id
            return
        }
        ticketId = pool[(index + 1) % pool.count].id
    }

    private func hydrateAssignee() {
        if let note = selectedTicket?.note,
           let extracted = extractAssignee(from: note),
           assignee.trimmingCharacters(in: .whitespaces).isEmpty {
            assignee = extracted
        }
    }

    private func nextDueLabel(_ current: String) -> String {
        let options = ["Today · 7:30 pm", "Today · 8:00 pm", "Today · ASAP", "Tomorrow · lunch"]
        let index = options.firstIndex(of: current) ?? 0
        return options[(index + 1) % options.count]
    }

    private func submit() async {
        guard let ticket = selectedTicket else { return }
        saving = true
        defer { saving = false }

        var contract = OperatingContractInput()
        contract.orderStatus = target.orderStatus
        contract.tableNumber = ticket.tableNumber
        contract.covers = ticket.covers
        contract.locationName = ticket.locationName
        contract.clientKey = ticket.clientKey
        contract.staffName = assigneeForPost
        contract.deliveryStatus = mode.rawValue

        let noteBits = [
            "Status · \(target.rawValue)",
            "Board · \(mode.rawValue)",
            assigneeForPost.map { "Assignee: \($0)" },
            "Due: \(dueLabel)",
            ticket.note.flatMap { $0.isEmpty ? nil : $0 },
        ].compactMap { $0 }

        let ok = await model.record(
            OperatingEventInput(
                momentId: model.momentId,
                eventType: "ORDER",
                title: ticket.title.isEmpty ? "Status · \(kotLabel)" : ticket.title,
                amount: nil,
                note: noteBits.joined(separator: " · "),
                channelName: ticket.channelName,
                counterpartyName: ticket.counterpartyName,
                quantity: ticket.quantity,
                contract: contract,
                paymentStatus: "NONE",
                revisesEventId: ticket.id
            )
        )
        if ok { dismiss() }
    }

    // MARK: - Helpers

    private func ticketMatchesMode(_ ticket: OperatingActivityPayload) -> Bool {
        let status = (ticket.status ?? "").uppercased()
        switch mode {
        case .prepare:
            return status.isEmpty || status == "PLACED" || status == "PREPARING" || status == "ACCEPTED"
        case .ready:
            return status == "PREPARING" || status == "READY" || status == "PLACED" || status.isEmpty
        case .fulfil:
            return status == "READY" || status == "COMPLETED" || status == "SERVED"
        }
    }

    private func ticketPickerMeta(_ row: OperatingActivityPayload) -> String {
        [
            row.tableNumber.flatMap { $0.isEmpty ? nil : "Table \($0)" },
            row.locationName,
            orderStatusLabel(row.status),
        ]
        .compactMap { $0?.trimmingCharacters(in: .whitespaces) }
        .filter { !$0.isEmpty }
        .joined(separator: " · ")
    }

    private func orderStatusLabel(_ status: String?) -> String {
        switch (status ?? "").uppercased() {
        case "PLACED": return "Sent"
        case "PREPARING": return "Preparing"
        case "READY": return "Ready"
        case "COMPLETED", "SERVED": return "Served"
        case "CANCELLED": return "Void"
        case "": return "Open"
        default:
            let raw = status?.trimmingCharacters(in: .whitespaces) ?? "Open"
            return raw.prefix(1).uppercased() + raw.dropFirst().lowercased()
        }
    }

    private func parseBoardLines(_ ticket: OperatingActivityPayload?) -> [BoardLine] {
        guard let ticket else { return [] }
        let station = ticket.locationName?.trimmingCharacters(in: .whitespaces)
        let stationBit = (station?.isEmpty == false ? station! : "Hot")
        let cook = extractAssignee(from: ticket.note ?? "") ?? assigneeLabel
        let status = (ticket.status ?? "").uppercased()
        let ready = status == "READY" || status == "COMPLETED" || status == "SERVED"
        let label = ready ? "Ready" : (status == "PREPARING" ? "Preparing" : orderStatusLabel(ticket.status))
        let metaSuffix = ready ? "done" : (status == "PREPARING" ? "fired" : "open")

        // Prefer note Hold / dish fragments when present; otherwise one honest ticket line.
        let held = ticket.note?
            .components(separatedBy: "Hold:")
            .dropFirst()
            .first?
            .components(separatedBy: " · ")
            .map { $0.trimmingCharacters(in: .whitespaces) }
            .filter { !$0.isEmpty } ?? []

        if !held.isEmpty {
            return held.enumerated().map { index, name in
                BoardLine(
                    id: "\(ticket.id)-hold-\(index)",
                    name: name,
                    meta: "\(stationBit) · held",
                    statusLabel: "Preparing",
                    isReady: false,
                    assignee: cook
                )
            }
        }

        let title = ticket.title.trimmingCharacters(in: .whitespaces)
        return [
            BoardLine(
                id: ticket.id,
                name: title.isEmpty ? "Kitchen ticket" : title,
                meta: "\(stationBit) · \(metaSuffix)",
                statusLabel: label,
                isReady: ready,
                assignee: cook
            ),
        ]
    }

    private func extractAssignee(from note: String) -> String? {
        guard let range = note.range(of: "Assignee:", options: .caseInsensitive) else { return nil }
        let after = note[range.upperBound...]
            .trimmingCharacters(in: .whitespaces)
        let token = after.split(separator: "·", maxSplits: 1, omittingEmptySubsequences: true)
            .first?
            .trimmingCharacters(in: .whitespaces)
        guard let token, !token.isEmpty else { return nil }
        return String(token)
    }

    private func kotKey(for row: OperatingActivityPayload) -> String? {
        let key = row.clientKey?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        return key.isEmpty ? nil : key
    }
}

import SwiftUI

/// Kitchen ticket — Figma `RST-QA-01-ALT · KOT · Send / Fire / Reprint`.
/// Port of Android `RestaurantKotSheet`. Send posts ORDER (PLACED); Fire revises
/// PLACED → PREPARING → READY. Reprint stays locked (no PDF endpoint).
struct RestaurantKotSheet: View {
    let theme: OperatingTheme
    @ObservedObject var model: BusinessOperatingModel

    @Environment(\.dismiss) private var dismiss
    @State private var tab: KotTab = .send
    @State private var ticketId: String?
    @State private var mode = "In-dining"
    @State private var station = ""
    @State private var dishQuery = ""
    @State private var guest = ""
    @State private var note = ""
    @State private var tableNumber = ""
    @State private var covers = ""
    @State private var lines: [KotLine] = []
    @State private var saving = false

    private enum KotTab: String { case send, fire }

    private var masterSource: [OperatingMasterPayload] {
        model.masters.isEmpty ? (model.projection?.masters ?? []) : model.masters
    }

    private var stations: [OperatingMasterPayload] {
        masterSource
            .filter { ["STATION", "LOCATION"].contains($0.kind.uppercased()) }
            .sorted { $0.name.localizedCaseInsensitiveCompare($1.name) == .orderedAscending }
    }

    private var menuDishes: [OperatingMasterPayload] {
        masterSource
            .filter { ["MENU_DISH", "PRODUCT", "ITEM", "SKU"].contains($0.kind.uppercased()) }
            .sorted { $0.name.localizedCaseInsensitiveCompare($1.name) == .orderedAscending }
    }

    private var channels: [String] {
        let fromLife = model.projection?.life?.channels.filter { !$0.isEmpty } ?? []
        return fromLife.isEmpty ? ["In-dining", "Cloud", "Takeaway"] : Array(fromLife.prefix(3))
    }

    private var tickets: [OperatingActivityPayload] {
        (model.projection?.activity ?? [])
            .filter { $0.eventType.uppercased() == "ORDER" }
            .filter {
                let status = ($0.status ?? "").uppercased()
                return status != "DONE" && status != "CANCELLED" && status != "COMPLETED"
            }
            .sorted { $0.occurredAt > $1.occurredAt }
    }

    private var selectedTicket: OperatingActivityPayload? {
        tickets.first { $0.id == ticketId }
    }

    private var fireTarget: String? {
        nextFireStatus(selectedTicket?.status)
    }

    private var canSubmit: Bool {
        switch tab {
        case .send:
            return !lines.isEmpty
                && !station.trimmingCharacters(in: .whitespaces).isEmpty
                && !mode.trimmingCharacters(in: .whitespaces).isEmpty
        case .fire:
            return selectedTicket != nil && fireTarget != nil
        }
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                header
                tabChips
                Text(tabCaption)
                    .font(.plusJakarta(size: 10))
                    .foregroundStyle(OperatingV12.subtitle)

                if tab == .send {
                    sendBody
                } else {
                    fireBody
                }

                whyNote(
                    tab == .send
                        ? "Station persists as locationName. Dining mode is the required channel."
                        : "Fire never reposts revenue. The bill posts separately once served."
                )

                Button {
                    Task { await submit() }
                } label: {
                    Text(saving ? "Saving…" : footerLabel)
                        .font(.plusJakarta(size: 15, weight: .bold))
                        .foregroundStyle(theme.onAccent)
                        .frame(maxWidth: .infinity)
                        .frame(height: 52)
                        .background(canSubmit && !saving ? theme.accent : theme.accent.opacity(0.4))
                        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                }
                .buttonStyle(.plain)
                .disabled(!canSubmit || saving)

                Text("Draft preserved if you close the sheet")
                    .font(.plusJakarta(size: 10))
                    .foregroundStyle(OperatingV12.subtitle)
            }
            .padding(16)
            .padding(.bottom, 24)
        }
        .background(theme.canvas)
        .task {
            if ticketId == nil { ticketId = tickets.first?.id }
            if station.isEmpty { station = stations.first?.name ?? "" }
            if mode.isEmpty || !channels.contains(mode) { mode = channels.first ?? "In-dining" }
            if model.masters.isEmpty { await model.loadMasters() }
        }
    }

    private var footerLabel: String {
        switch tab {
        case .send: return "Send ticket"
        case .fire:
            switch fireTarget {
            case "READY": return "Mark ready"
            default: return "Fire ticket"
            }
        }
    }

    private var header: some View {
        HStack {
            VStack(alignment: .leading, spacing: 4) {
                Text("Kitchen Ticket")
                    .font(.plusJakarta(size: 22, weight: .semibold))
                    .foregroundStyle(OperatingV12.title)
                Text(
                    "\(tickets.count) open ticket\(tickets.count == 1 ? "" : "s") · "
                        + (stations.isEmpty ? "no stations set" : "\(stations.count) station\(stations.count == 1 ? "" : "s")")
                )
                .font(.plusJakarta(size: 12))
                .foregroundStyle(OperatingV12.subtitle)
            }
            Spacer()
            Button("Close") { dismiss() }
                .font(.plusJakarta(size: 13, weight: .semibold))
                .foregroundStyle(OperatingV12.subtitle)
        }
    }

    private var tabChips: some View {
        HStack(spacing: 6) {
            modeChip("Send", selected: tab == .send) { tab = .send }
            modeChip("Fire", selected: tab == .fire) { tab = .fire }
            Text("Reprint")
                .font(.plusJakarta(size: 12, weight: .semibold))
                .foregroundStyle(OperatingV12.subtitle.opacity(0.6))
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
                .background(theme.inset.opacity(0.5))
                .overlay(Capsule().strokeBorder(theme.border.opacity(0.45), lineWidth: 1))
                .clipShape(Capsule())
                .overlay(alignment: .bottom) {
                    Text("No PDF endpoint")
                        .font(.plusJakarta(size: 8, weight: .bold))
                        .foregroundStyle(theme.warning)
                        .offset(y: 14)
                }
            Spacer(minLength: 0)
        }
        .padding(.bottom, 8)
    }

    private var tabCaption: String {
        switch tab {
        case .send: return "Send posts an order. An order is never revenue — only a posted bill is."
        case .fire: return "Fire revises that same order (Sent → Preparing → Ready), so the kitchen sees one ticket."
        }
    }

    private var sendBody: some View {
        VStack(alignment: .leading, spacing: 12) {
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

            fieldLabel("Station")
            if stations.isEmpty {
                Text("Add stations under Stations first.")
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(theme.warning)
            } else {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 6) {
                        ForEach(stations) { row in
                            modeChip(row.name, selected: station == row.name) { station = row.name }
                        }
                    }
                }
            }

            fieldLabel("Add from menu")
            HStack(spacing: 8) {
                textField(
                    menuDishes.isEmpty ? "No saved dish — add under Menu" : "Search menu",
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

            if !dishQuery.trimmingCharacters(in: .whitespaces).isEmpty {
                dishMatches
            }

            if lines.isEmpty {
                emptyCard("Nothing on the ticket", "Add dishes, then send to a station. This is not a bill.")
            } else {
                VStack(spacing: 0) {
                    ForEach(Array(lines.enumerated()), id: \.element.id) { index, line in
                        HStack {
                            Text(line.name)
                                .font(.plusJakarta(size: 13, weight: .semibold))
                                .foregroundStyle(OperatingV12.title)
                            Spacer()
                            Text("×\(line.quantity)")
                                .font(.plusJakarta(size: 12, weight: .bold))
                                .foregroundStyle(theme.accent)
                            Button {
                                lines.remove(at: index)
                            } label: {
                                Image(systemName: "xmark")
                                    .font(.system(size: 10, weight: .bold))
                                    .foregroundStyle(OperatingV12.subtitle)
                            }
                            .buttonStyle(.plain)
                        }
                        .padding(.vertical, 10)
                        if index < lines.count - 1 {
                            Divider().overlay(theme.border.opacity(0.4))
                        }
                    }
                }
                .padding(.horizontal, 12)
                .background(theme.inset)
                .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
            }

            fieldLabel("Guest")
            textField("Table guest or name", text: $guest)
            fieldLabel("Note")
            textField("Allergies, course, fire note", text: $note)
        }
    }

    private var fireBody: some View {
        VStack(alignment: .leading, spacing: 10) {
            fieldLabel("Open tickets")
            if tickets.isEmpty {
                emptyCard("No open tickets", "Send a kitchen ticket first. Fire only revises an existing ORDER.")
            } else {
                ForEach(tickets) { ticket in
                    Button {
                        ticketId = ticket.id
                    } label: {
                        HStack {
                            VStack(alignment: .leading, spacing: 2) {
                                Text(ticket.title.isEmpty ? "Kitchen ticket" : ticket.title)
                                    .font(.plusJakarta(size: 13, weight: .semibold))
                                    .foregroundStyle(OperatingV12.title)
                                    .lineLimit(1)
                                Text(ticketMeta(ticket))
                                    .font(.plusJakarta(size: 11))
                                    .foregroundStyle(OperatingV12.subtitle)
                                    .lineLimit(1)
                            }
                            Spacer()
                            if ticket.id == ticketId {
                                Image(systemName: "checkmark.circle.fill")
                                    .foregroundStyle(theme.accent)
                            }
                        }
                        .padding(12)
                        .background(ticket.id == ticketId ? theme.accent.opacity(0.12) : theme.inset)
                        .overlay(
                            RoundedRectangle(cornerRadius: 12, style: .continuous)
                                .strokeBorder(
                                    ticket.id == ticketId ? theme.accent.opacity(0.5) : theme.border.opacity(0.4),
                                    lineWidth: 1
                                )
                        )
                        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                    }
                    .buttonStyle(.plain)
                }
            }
            if let selectedTicket, fireTarget == nil {
                emptyCard(
                    "Already advanced",
                    "This ticket is \(selectedTicket.status ?? "ready"). Fire only moves Sent → Preparing → Ready."
                )
            }
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
                        Text(dish.name)
                            .font(.plusJakarta(size: 13, weight: .semibold))
                            .foregroundStyle(OperatingV12.title)
                            .frame(maxWidth: .infinity, alignment: .leading)
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

    private func ticketMeta(_ ticket: OperatingActivityPayload) -> String {
        [
            ticket.tableNumber.flatMap { $0.isEmpty ? nil : "Table \($0)" },
            ticket.covers.flatMap { $0.isEmpty ? nil : "\($0) covers" },
            ticket.locationName.flatMap { $0.isEmpty ? nil : $0 },
            ticket.channelName.flatMap { $0.isEmpty ? nil : $0 },
            ticket.status.flatMap { $0.isEmpty ? nil : $0 },
            ticket.clientKey.flatMap { $0.isEmpty ? nil : $0 },
            ticket.counterpartyName.flatMap { $0.isEmpty ? nil : $0 },
        ]
        .compactMap { $0 }
        .joined(separator: " · ")
    }

    private func nextFireStatus(_ status: String?) -> String? {
        switch (status ?? "").uppercased() {
        case "", "PLACED": return "PREPARING"
        case "PREPARING": return "READY"
        default: return nil
        }
    }

    private func addDish() {
        let typed = dishQuery.trimmingCharacters(in: .whitespaces)
        guard !menuDishes.isEmpty else { return }
        let match = menuDishes.first { $0.name.caseInsensitiveCompare(typed) == .orderedSame }
            ?? menuDishes.first { !typed.isEmpty && $0.name.localizedCaseInsensitiveContains(typed) }
            ?? menuDishes.first
        if let match { add(match) }
        dishQuery = ""
    }

    private func add(_ dish: OperatingMasterPayload) {
        if let index = lines.firstIndex(where: { $0.masterId == dish.id }) {
            lines[index].quantity += 1
            return
        }
        lines.append(KotLine.of(dish))
    }

    private func submit() async {
        guard canSubmit else { return }
        saving = true
        defer { saving = false }
        if tab == .send {
            var contract = OperatingContractInput()
            contract.orderStatus = "PLACED"
            contract.taxMode = "NOT_CONFIGURED"
            let stationName = station.trimmingCharacters(in: .whitespaces)
            let table = tableNumber.trimmingCharacters(in: .whitespaces)
            let coverCount = covers.trimmingCharacters(in: .whitespaces)
            contract.locationName = stationName.isEmpty ? nil : stationName
            contract.tableNumber = table.isEmpty ? nil : table
            contract.covers = coverCount.isEmpty ? nil : coverCount
            contract.clientKey = "KOT-\(Int(Date().timeIntervalSince1970) % 100_000)"
            let noteBits = [
                stationName.isEmpty ? nil : "Station: \(stationName)",
                note.trimmingCharacters(in: .whitespaces).isEmpty ? nil : note,
            ].compactMap { $0 }
            let guestName = guest.trimmingCharacters(in: .whitespaces)
            let title: String = {
                if !table.isEmpty { return "Table \(table)" }
                if !guestName.isEmpty { return "Ticket · \(guestName)" }
                return "Ticket · \(lines.count) item\(lines.count == 1 ? "" : "s")"
            }()
            let ok = await model.record(
                OperatingEventInput(
                    momentId: model.momentId,
                    eventType: "ORDER",
                    title: title,
                    note: noteBits.isEmpty ? nil : noteBits.joined(separator: " · "),
                    channelName: mode,
                    counterpartyName: guestName.isEmpty ? nil : guestName,
                    quantity: coverCount.isEmpty ? nil : coverCount,
                    lineItems: lines.map {
                        .init(
                            itemName: $0.name,
                            quantity: "\($0.quantity)",
                            unit: $0.unit,
                            unitPrice: nil,
                            masterId: $0.masterId
                        )
                    },
                    contract: contract,
                    paymentStatus: "NONE"
                )
            )
            if ok { dismiss() }
        } else if let ticket = selectedTicket, let next = fireTarget {
            var contract = OperatingContractInput()
            contract.orderStatus = next
            contract.tableNumber = ticket.tableNumber
            contract.covers = ticket.covers
            contract.locationName = ticket.locationName
            contract.clientKey = ticket.clientKey
            let ok = await model.record(
                OperatingEventInput(
                    momentId: model.momentId,
                    eventType: "ORDER",
                    title: ticket.title.isEmpty ? "Kitchen ticket" : ticket.title,
                    amount: ticket.amount,
                    note: ticket.note,
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
    }

    // MARK: - Chrome

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

    private func modeChip(_ text: String, selected: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(text)
                .font(.plusJakarta(size: 12, weight: .semibold))
                .foregroundStyle(selected ? theme.onAccent : OperatingV12.subtitle)
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
                .background(selected ? theme.accent : theme.inset)
                .clipShape(Capsule())
        }
        .buttonStyle(.plain)
    }

    private func chipRow(_ options: [String], selection: Binding<String>) -> some View {
        HStack(spacing: 6) {
            ForEach(options, id: \.self) { option in
                modeChip(option, selected: selection.wrappedValue == option) {
                    selection.wrappedValue = option
                }
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

private struct KotLine: Identifiable, Equatable {
    let id: String
    let masterId: String?
    let name: String
    let unit: String?
    var quantity: Int

    static func of(_ dish: OperatingMasterPayload) -> KotLine {
        KotLine(
            id: dish.id,
            masterId: dish.id,
            name: dish.name,
            unit: dish.unit ?? "pc",
            quantity: 1
        )
    }
}

import SwiftUI

/// Kitchen ticket — Figma `RST-QA-01-ALT · KOT · Send` `2449:29655`,
/// `· Fire` `2449:29769`, `· Reprint` `2449:29859`.
///
/// Send posts an `ORDER` with `orderStatus = PLACED` and real `lines[]` — an order is
/// never revenue, which is exactly what the frame says. Fire revises that same ORDER to
/// `PREPARING` through `PATCH …/operating-events/:eventId`, keeping the ticket's lines.
/// Reprint has no ticket-PDF endpoint, so it is absent rather than mocked.
///
/// Station routing (Tandoor / Hot / Cold / Bar) persists as `STATION` masters and is echoed
/// in the ticket note; there is no per-line station field on the contract.
struct RestaurantKotSheet: View {
    let model: OperatingProjectionPayload
    var onDismiss: () -> Void
    var onSubmit: (RestaurantEventDraft) -> Void

    @State private var tab = KotTab.send
    @State private var ticketId: String?
    @State private var station = ""
    @State private var dishQuery = ""
    @State private var guest = ""
    @State private var note = ""
    @State private var lines: [KotLine] = []

    private enum KotTab: String, CaseIterable {
        case send, fire
        var label: String { self == .send ? "Send" : "Fire" }
    }

    private var stations: [OperatingMasterPayload] {
        (model.masters ?? [])
            .filter { $0.kind.uppercased() == "STATION" }
            .sorted { $0.name.lowercased() < $1.name.lowercased() }
    }

    private var menuDishes: [OperatingMasterPayload] {
        (model.masters ?? [])
            .filter { $0.kind.uppercased() == "MENU_DISH" }
            .sorted { $0.name.lowercased() < $1.name.lowercased() }
    }

    private var tickets: [OperatingActivityPayload] {
        restaurantOpenOrders(model).sorted { $0.occurredAt > $1.occurredAt }
    }

    private var selectedTicket: OperatingActivityPayload? {
        tickets.first { $0.id == ticketId } ?? tickets.first
    }

    private var canPost: Bool {
        switch tab {
        case .send: return !lines.isEmpty && !station.isEmpty
        case .fire:
            guard let selectedTicket else { return false }
            return (selectedTicket.status ?? "").uppercased() != "PREPARING"
        }
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                EmberSheetGrabber()
                EmberSheetHeader(
                    title: "Kitchen Ticket",
                    subtitle: "\(tickets.count) open ticket\(tickets.count == 1 ? "" : "s") · "
                        + (stations.isEmpty
                            ? "no stations set"
                            : "\(stations.count) station\(stations.count == 1 ? "" : "s")"),
                    onClose: onDismiss
                )

                HStack(spacing: 6) {
                    ForEach(KotTab.allCases, id: \.rawValue) { option in
                        EmberModeChip(text: option.label, selected: tab == option) { tab = option }
                    }
                    EmberModeChip(text: "Reprint", selected: false, locked: "No PDF endpoint", onTap: nil)
                    Spacer(minLength: 0)
                }
                Text(tab == .send
                    ? "Send posts an order. An order is never revenue — only a posted bill is."
                    : "Fire revises that same order to Preparing, so the kitchen sees one ticket, not two.")
                    .font(.plusJakarta(size: 10))
                    .foregroundStyle(RestaurantEmber.textFaint)
                    .fixedSize(horizontal: false, vertical: true)

                if tab == .send { sendBody } else { fireBody }

                EmberWhyNote(
                    body: tab == .send
                        ? "Station rides on the ticket note. The contract has no per-line station field."
                        : "Fire never reposts revenue. The bill posts separately once served.",
                    tinted: true
                )

                EmberSheetFooter(
                    label: tab == .send ? "Send ticket" : "Fire ticket",
                    enabled: canPost,
                    caveat: "Draft preserved if you close the sheet"
                ) {
                    submit()
                }
                Spacer(minLength: 16)
            }
            .padding(.horizontal, RestaurantEmber.gutter)
            .padding(.vertical, 12)
        }
        .onAppear {
            if station.isEmpty { station = stations.first?.name ?? "" }
            if ticketId == nil { ticketId = tickets.first?.id }
        }
    }

    // MARK: - Send

    private var sendBody: some View {
        VStack(alignment: .leading, spacing: 12) {
            EmberFieldLabel("Station")
            if stations.isEmpty {
                EmberEmpty(
                    title: "No station yet",
                    body: "Add Tandoor / Hot / Cold / Bar under Stations so the pass knows where to send."
                )
            } else {
                HStack(spacing: 6) {
                    ForEach(Array(stations.prefix(4))) { master in
                        EmberModeChip(text: master.name, selected: master.name == station) {
                            station = master.name
                        }
                    }
                    Spacer(minLength: 0)
                }
            }

            EmberFieldLabel("Dish")
            if menuDishes.isEmpty {
                EmberEmpty(
                    title: "Menu not set up",
                    body: "Add dishes under Menu first — a ticket line needs a real dish."
                )
            } else {
                EmberTextField(value: $dishQuery, placeholder: "Search menu")
                if !dishQuery.trimmingCharacters(in: .whitespaces).isEmpty {
                    let matches = menuDishes
                        .filter { $0.name.localizedCaseInsensitiveContains(dishQuery.trimmingCharacters(in: .whitespaces)) }
                        .prefix(4)
                    if !matches.isEmpty {
                        VStack(spacing: 6) {
                            ForEach(matches) { dish in
                                Button {
                                    if !lines.contains(where: { $0.masterId == dish.id }) {
                                        lines.append(KotLine.of(dish))
                                    }
                                    dishQuery = ""
                                } label: {
                                    HStack(spacing: 8) {
                                        Text(dish.name)
                                            .font(.plusJakarta(size: 13, weight: .medium))
                                            .foregroundStyle(RestaurantEmber.textPrimary)
                                            .lineLimit(1)
                                            .frame(maxWidth: .infinity, alignment: .leading)
                                        Text("+")
                                            .font(.plusJakarta(size: 13, weight: .bold))
                                            .foregroundStyle(RestaurantEmber.ember)
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
            }

            if lines.isEmpty {
                EmberEmpty(
                    title: "No dish on the ticket",
                    body: "Pick from the menu above. Each line becomes a real order line."
                )
            } else {
                VStack(spacing: 0) {
                    ForEach(Array(lines.enumerated()), id: \.element.masterId) { index, line in
                        KotLineRow(
                            line: line,
                            onDecrement: {
                                if lines[index].quantity <= 1 { lines.remove(at: index) }
                                else { lines[index].quantity -= 1 }
                            },
                            onIncrement: {
                                if lines[index].quantity < 99 { lines[index].quantity += 1 }
                            },
                            onRemove: { lines.remove(at: index) }
                        )
                    }
                }
                .padding(.horizontal, 12)
                .background(RestaurantEmber.chip)
                .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
            }

            EmberFieldLabel("Guest / table reference")
            EmberTextField(value: $guest, placeholder: "Walk-in or covers reference")

            HStack(spacing: 6) {
                ForEach(["Table", "Covers", "KOT no."], id: \.self) { label in
                    Text(label)
                        .font(.plusJakarta(size: 12, weight: .medium))
                        .foregroundStyle(RestaurantEmber.textFaint)
                        .lineLimit(1)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 8)
                        .background(RestaurantEmber.chip.opacity(0.5))
                        .overlay(Capsule().stroke(RestaurantEmber.hairline.opacity(0.5), lineWidth: 1))
                        .clipShape(Capsule())
                }
                Spacer(minLength: 0)
            }

            EmberFieldLabel("Kitchen note")
            EmberTextField(value: $note, placeholder: "No onion, extra spice, allergy")
        }
    }

    // MARK: - Fire

    private var fireBody: some View {
        VStack(alignment: .leading, spacing: 12) {
            EmberFieldLabel("Open tickets")
            if tickets.isEmpty {
                EmberEmpty(
                    title: "Nothing in the pass",
                    body: "Send a ticket first — only a posted order can be fired."
                )
            } else {
                let activeId = ticketId ?? tickets.first?.id
                VStack(spacing: 8) {
                    ForEach(tickets.prefix(6)) { ticket in
                        let firing = (ticket.status ?? "").uppercased() == "PREPARING"
                        let selected = ticket.id == activeId
                        Button { ticketId = ticket.id } label: {
                            HStack(spacing: 10) {
                                VStack(alignment: .leading, spacing: 2) {
                                    Text(ticket.title.isEmpty ? "Kitchen ticket" : ticket.title)
                                        .font(.plusJakarta(size: 13, weight: .medium))
                                        .foregroundStyle(RestaurantEmber.textPrimary)
                                        .lineLimit(1)
                                    Text(ticketCaption(ticket))
                                        .font(.plusJakarta(size: 10))
                                        .foregroundStyle(RestaurantEmber.textBody)
                                        .lineLimit(1)
                                }
                                .frame(maxWidth: .infinity, alignment: .leading)
                                EmberSeverityChip(
                                    label: restaurantOrderStatusLabel(ticket.status),
                                    color: firing ? RestaurantEmber.amber : RestaurantEmber.ember
                                )
                            }
                            .padding(12)
                            .background(selected ? RestaurantEmber.ember.opacity(0.16) : RestaurantEmber.surfaceWell)
                            .overlay(
                                RoundedRectangle(cornerRadius: 12, style: .continuous)
                                    .stroke(selected ? RestaurantEmber.ember : Color.white.opacity(0.06), lineWidth: 1)
                            )
                            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                        }
                        .buttonStyle(.plain)
                    }
                }
                if (selectedTicket?.status ?? "").uppercased() == "PREPARING" {
                    EmberEmpty(
                        title: "Already firing",
                        body: "This ticket was fired. Nothing to do until the kitchen marks it ready."
                    )
                }
            }
        }
    }

    // MARK: - Submit

    /// Station · guest · sent-on, in one line. Falls back to `Order` when nothing is set.
    private func ticketCaption(_ ticket: OperatingActivityPayload) -> String {
        let parts = [
            ticket.channelName?.isEmpty == false ? ticket.channelName : nil,
            ticket.counterpartyName?.isEmpty == false ? ticket.counterpartyName : nil,
            RestaurantFormat.shortDay(ticket.occurredAt).map { "Sent \($0)" },
        ].compactMap { $0 }
        return parts.isEmpty ? "Order" : parts.joined(separator: " · ")
    }

    private func submit() {
        if tab == .send {
            let ticketLines: [(itemName: String, quantity: String?, unit: String?, unitPrice: String?, masterId: String?)] =
                lines.map { line in
                    (
                        itemName: line.name,
                        quantity: "\(line.quantity)" as String?,
                        unit: line.unit as String?,
                        unitPrice: nil,
                        masterId: line.masterId as String?
                    )
                }
            var contract = OperatingContractInput()
            contract.orderStatus = "PLACED"
            contract.taxMode = "NOT_CONFIGURED"
            let noteParts = [
                station.isEmpty ? nil : "Station: \(station)",
                note.isEmpty ? nil : note,
            ].compactMap { $0 }
            onSubmit(
                RestaurantEventDraft(
                    eventType: "ORDER",
                    title: guest.isEmpty
                        ? "Ticket · \(lines.count) item\(lines.count == 1 ? "" : "s")"
                        : "Ticket · \(guest)",
                    amount: nil,
                    note: noteParts.isEmpty ? nil : noteParts.joined(separator: " · "),
                    channelName: station.isEmpty ? nil : station,
                    counterpartyName: guest.isEmpty ? nil : guest,
                    paymentStatus: "NONE",
                    contract: contract,
                    lineItems: ticketLines
                )
            )
        } else {
            guard let ticket = selectedTicket else { return }
            var contract = OperatingContractInput()
            contract.orderStatus = "PREPARING"
            onSubmit(
                RestaurantEventDraft(
                    eventType: "ORDER",
                    title: ticket.title.isEmpty ? "Kitchen ticket" : ticket.title,
                    amount: ticket.amount,
                    note: ticket.note,
                    channelName: ticket.channelName,
                    counterpartyName: ticket.counterpartyName,
                    paymentStatus: "NONE",
                    revisesEventId: ticket.id,
                    contract: contract
                )
            )
        }
    }
}

// MARK: - Rows

private struct KotLine: Identifiable {
    let masterId: String
    let name: String
    let unit: String
    var quantity: Int

    var id: String { masterId }

    static func of(_ master: OperatingMasterPayload) -> KotLine {
        KotLine(
            masterId: master.id,
            name: master.name,
            unit: master.unit?.isEmpty == false ? master.unit! : "plate",
            quantity: 1
        )
    }
}

private struct KotLineRow: View {
    let line: KotLine
    var onDecrement: () -> Void
    var onIncrement: () -> Void
    var onRemove: () -> Void

    var body: some View {
        HStack(spacing: 10) {
            VStack(alignment: .leading, spacing: 2) {
                Text(line.name)
                    .font(.plusJakarta(size: 13, weight: .bold))
                    .foregroundStyle(RestaurantEmber.textPrimary)
                    .lineLimit(1)
                Text(line.unit)
                    .font(.plusJakarta(size: 10))
                    .foregroundStyle(RestaurantEmber.textBody)
                    .lineLimit(1)
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            KotStepper(glyph: "−", action: onDecrement)
            Text("\(line.quantity)")
                .font(.plusJakarta(size: 13, weight: .bold))
                .foregroundStyle(RestaurantEmber.textPrimary)
            KotStepper(glyph: "+", action: onIncrement)
            Button(action: onRemove) {
                Text("✕")
                    .font(.plusJakarta(size: 11))
                    .foregroundStyle(RestaurantEmber.textFaint)
                    .padding(4)
            }
            .buttonStyle(.plain)
        }
        .padding(.vertical, 9)
    }
}

private struct KotStepper: View {
    let glyph: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(glyph)
                .font(.plusJakarta(size: 15, weight: .bold))
                .foregroundStyle(RestaurantEmber.textPrimary)
                .frame(width: 28, height: 28)
                .background(RestaurantEmber.chip)
                .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
        }
        .buttonStyle(.plain)
    }
}

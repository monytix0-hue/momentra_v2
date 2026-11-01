import SwiftUI

/// Masters — Figma `RST-MASTER · Menu` `2465:30143`, `· Stations` `2465:30265`,
/// `· Vendors` `2465:30021`, `· Invite & QR` `2465:29655`.
///
/// Rows come from `model.masters`, which the projection derives from posted events that
/// carried a `master` payload. A master cannot be created on its own, so the create action
/// opens the existing capture sheet (`UPDATE` + `masterKind`) — the documented way to
/// introduce a master ahead of a transaction — instead of pretending a CRUD endpoint exists.
enum RestaurantMastersTab: String, Identifiable, CaseIterable {
    case menu, stations, vendors, invite

    var id: String { rawValue }

    var title: String {
        switch self {
        case .menu: return "Menu"
        case .stations: return "Stations"
        case .vendors: return "Vendors"
        case .invite: return "Invite & QR"
        }
    }

    var singular: String {
        switch self {
        case .menu: return "dish"
        case .stations: return "station"
        case .vendors: return "vendor"
        case .invite: return "invite"
        }
    }

    var icon: String {
        switch self {
        case .menu: return RestaurantTileIcons.menu
        case .stations: return RestaurantTileIcons.stations
        case .vendors: return RestaurantTileIcons.vendors
        case .invite: return RestaurantTileIcons.invite
        }
    }

    /// Master `kind` aliases the projection may report for this tab.
    var kinds: Set<String> {
        switch self {
        case .menu: return ["MENU_DISH", "DISH", "MENU_ITEM", "PRODUCT"]
        case .stations: return ["STATION", "KITCHEN_STATION"]
        case .vendors: return ["VENDOR", "SUPPLIER", "PRINCIPAL", "PARTNER"]
        case .invite: return []
        }
    }

    var note: String {
        switch self {
        case .menu:
            return "A dish row changes only on an explicit master record — never from a posted bill."
        case .stations:
            return "A station groups the pass. Adding one does not move an open ticket."
        case .vendors:
            return "Named vendors are required for credit and payable tracking."
        case .invite:
            return "Invite grants a role and its access. Joining ≠ approval ≠ payment."
        }
    }
}

struct RestaurantMastersSheetView: View {
    let tab: RestaurantMastersTab
    let model: OperatingProjectionPayload
    var momentId: String? = nil
    var onRecord: (RestaurantEventDraft) async throws -> Void
    var onError: (String) -> Void

    @Environment(\.dismiss) private var dismiss
    @State private var createOpen = false

    private var rows: [OperatingMasterPayload] {
        let aliases = tab.kinds
        guard !aliases.isEmpty else { return [] }
        return (model.masters ?? [])
            .filter { aliases.contains($0.kind.uppercased()) }
            .sorted { $0.name.lowercased() < $1.name.lowercased() }
    }

    private var canCreate: Bool { tab != .invite && model.role == "OWNER_ADMIN" }

    private var venueName: String {
        let title = (model.title ?? "").trimmingCharacters(in: .whitespaces)
        return title.isEmpty ? "Restaurant" : title
    }

    var body: some View {
        EmberScreenBackground {
            ScrollView {
                VStack(alignment: .leading, spacing: 12) {
                    EmberSheetGrabber()
                    EmberSheetHeader(
                        title: tab.title,
                        subtitle: tab == .invite
                            ? venueName
                            : "\(rows.count) record\(rows.count == 1 ? "" : "s") · \(venueName)",
                        onClose: { dismiss() }
                    )

                    if tab == .invite {
                        BusinessInviteQRView(
                            title: model.title ?? "",
                            role: model.role,
                            momentId: momentId,
                            palette: .ember,
                            icon: RestaurantTileIcons.invite,
                            noun: "restaurant",
                            embedded: true
                        )
                    } else {
                        if canCreate {
                            Button { createOpen = true } label: {
                                Text("New \(tab.singular)")
                                    .font(.plusJakarta(size: 15, weight: .bold))
                                    .foregroundStyle(.white)
                                    .frame(maxWidth: .infinity)
                                    .padding(.vertical, 14)
                                    .background(RestaurantEmber.ember)
                                    .clipShape(RoundedRectangle(cornerRadius: RestaurantEmber.rCardTight, style: .continuous))
                                    .shadow(color: RestaurantEmber.ember.opacity(0.45), radius: 12, x: 0, y: 8)
                            }
                            .buttonStyle(.plain)
                        }

                        switch tab {
                        case .menu: menuList
                        case .stations: stationList
                        default: vendorList
                        }

                        EmberWhyNote(body: tab.note, tinted: true)
                    }
                    Spacer(minLength: 20)
                }
                .padding(.horizontal, RestaurantEmber.gutter)
                .padding(.vertical, 12)
            }
        }
        .presentationDragIndicator(.hidden)
        .sheet(isPresented: $createOpen) {
            OperatingCaptureSheet(
                title: "New \(tab.singular)",
                icon: tab.icon,
                eventType: "UPDATE",
                mode: "MASTERS",
                model: model,
                variant: "restaurant",
                onSubmit: { draft in
                    do { try await onRecord(RestaurantEventDraft(draft)) }
                    catch { onError(error.localizedDescription) }
                },
                onDismiss: { createOpen = false }
            )
        }
    }

    // MARK: - Menu `2465:30143`

    @ViewBuilder
    private var menuList: some View {
        if rows.isEmpty {
            EmberEmpty(
                title: "Menu not set up",
                body: "Add a dish here and it becomes pickable on the POS bill and the kitchen ticket."
            )
        } else {
            VStack(spacing: 8) {
                ForEach(rows) { master in
                    let gst = master.gstRate?.trimmingCharacters(in: .whitespaces)
                    MasterRow(
                        glyph: "◈",
                        title: master.name.isEmpty ? "Unnamed dish" : master.name,
                        caption: menuCaption(master, gst: gst),
                        trailing: gst?.isEmpty == false ? "GST \(gst!)%" : "No GST",
                        trailingColor: gst?.isEmpty == false ? RestaurantEmber.gold : RestaurantEmber.warning
                    )
                }
            }
            Text("Dish price is typed on each bill line — the master carries a GST rate, not a rate card.")
                .font(.plusJakarta(size: 10))
                .foregroundStyle(RestaurantEmber.textFaint)
                .fixedSize(horizontal: false, vertical: true)
        }
    }

    private func menuCaption(_ master: OperatingMasterPayload, gst: String?) -> String {
        let parts = [
            master.unit?.isEmpty == false ? "Serves / \(master.unit!)" : nil,
            master.hsn?.isEmpty == false ? "HSN \(master.hsn!)" : nil,
            gst?.isEmpty != false ? "GST rate not set" : nil,
        ].compactMap { $0 }
        return parts.isEmpty ? "No unit recorded" : parts.joined(separator: " · ")
    }

    // MARK: - Stations `2465:30265`

    @ViewBuilder
    private var stationList: some View {
        let open = restaurantOpenOrders(model)
        if rows.isEmpty {
            EmberEmpty(
                title: "No station yet",
                body: "Add Tandoor / Hot / Cold / Bar so the pass knows where a ticket belongs."
            )
        } else {
            VStack(spacing: 8) {
                ForEach(rows) { master in
                    let tickets = open.filter { restaurantTicketCarriesStation($0, station: master.name) }.count
                    MasterRow(
                        glyph: "⌘",
                        title: master.name.isEmpty ? "Unnamed station" : master.name,
                        caption: tickets > 0
                            ? "\(tickets) open ticket\(tickets == 1 ? "" : "s")"
                            : "No open ticket",
                        trailing: tickets > 0 ? "Firing" : "Idle",
                        trailingColor: tickets > 0 ? RestaurantEmber.amber : RestaurantEmber.textMuted
                    )
                }
            }
            Text("Counted from posted orders whose status is not Completed or Void. "
                + "The contract has no per-line station field.")
                .font(.plusJakarta(size: 10))
                .foregroundStyle(RestaurantEmber.textFaint)
                .fixedSize(horizontal: false, vertical: true)
        }
    }

    // MARK: - Vendors `2465:30021`

    @ViewBuilder
    private var vendorList: some View {
        if rows.isEmpty {
            EmberEmpty(
                title: "No vendor yet",
                body: "Add the suppliers you buy ingredients or packaging from."
            )
        } else {
            let payables = (model.paymentReadiness ?? []).filter { $0.relevance != "RECEIVABLE" }
            VStack(spacing: 8) {
                ForEach(rows) { master in
                    let open = payables
                        .filter { ($0.counterpartyName ?? "").caseInsensitiveCompare(master.name) == .orderedSame }
                        .reduce(Decimal(0)) { $0 + (Decimal(string: $1.amount) ?? 0) }
                    MasterRow(
                        glyph: "◆",
                        title: master.name.isEmpty ? "Unnamed vendor" : master.name,
                        caption: master.gstin?.isEmpty == false ? "GSTIN \(master.gstin!)" : "GSTIN not recorded",
                        trailing: open > 0
                            ? RestaurantFormat.compact("\(open)", currency: model.currency, visible: model.financeVisible == true)
                            : "Nil",
                        trailingColor: open > 0 ? RestaurantEmber.amber : RestaurantEmber.textMuted
                    )
                }
            }
            Text("Open AP per vendor comes from unpaid purchase events — never estimated.")
                .font(.plusJakarta(size: 10))
                .foregroundStyle(RestaurantEmber.textFaint)
                .fixedSize(horizontal: false, vertical: true)
        }
    }
}

/// One masters row: 30pt ember icon well, name, caption, trailing value.
private struct MasterRow: View {
    let glyph: String
    let title: String
    let caption: String
    let trailing: String
    let trailingColor: Color

    var body: some View {
        HStack(spacing: 10) {
            Text(glyph)
                .font(.plusJakarta(size: 13))
                .foregroundStyle(RestaurantEmber.ember)
                .frame(width: 30, height: 30)
                .background(RestaurantEmber.ember.opacity(0.16))
                .clipShape(RoundedRectangle(cornerRadius: 9, style: .continuous))
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.plusJakarta(size: 13, weight: .bold))
                    .foregroundStyle(RestaurantEmber.textPrimary)
                    .lineLimit(1)
                Text(caption)
                    .font(.plusJakarta(size: 10))
                    .foregroundStyle(RestaurantEmber.textBody)
                    .lineLimit(1)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            Text(trailing)
                .font(.plusJakarta(size: 12, weight: .bold))
                .foregroundStyle(trailingColor)
                .lineLimit(1)
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 10)
        .background(RestaurantEmber.card)
        .overlay(
            RoundedRectangle(cornerRadius: RestaurantEmber.rRow, style: .continuous)
                .stroke(RestaurantEmber.ember.opacity(0.16), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: RestaurantEmber.rRow, style: .continuous))
    }
}

// MARK: - Derived helpers

/// Orders that are still in the kitchen: everything not served or voided.
func restaurantOpenOrders(_ model: OperatingProjectionPayload) -> [OperatingActivityPayload] {
    (model.activity ?? []).filter { row in
        guard row.eventType.uppercased() == "ORDER" else { return false }
        let status = (row.status ?? "").uppercased()
        return status != "COMPLETED" && status != "CANCELLED"
    }
}

/// A ticket belongs to a station when its note carries `Station: <name>`, as KOT Send writes it.
func restaurantTicketCarriesStation(_ ticket: OperatingActivityPayload, station: String) -> Bool {
    let note = ticket.note ?? ""
    if note.range(of: "Station: \(station)", options: .caseInsensitive) != nil { return true }
    return (ticket.channelName ?? "").caseInsensitiveCompare(station) == .orderedSame
}

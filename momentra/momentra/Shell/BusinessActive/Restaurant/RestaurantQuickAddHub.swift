import SwiftUI

/// Quick Add hub — Figma `RST-HUB-v5` `2400:29892`.
///
/// 9 core tiles (3 × 3) + 5 support tiles (3 + the 177pt `Invite & QR` / `Issue` pair).
/// A tile the role cannot submit stays in the grid and renders locked, so the layout keeps
/// its shape instead of silently shrinking.
///
/// `Sale` opens the POS bill and `Status` opens the kitchen ticket — the two surfaces that
/// need real itemized lines. Every other tile opens the shared capture sheet.
struct RestaurantQuickAddHub: View {
    let model: OperatingProjectionPayload
    var momentId: String? = nil
    var onRecord: (RestaurantEventDraft) async throws -> Void
    var onDecide: (String, String) async throws -> Void

    @State private var presented: HubSheet?
    @State private var errorText: String?

    private enum HubSheet: Identifiable {
        case pos
        case kot
        case capture(RestaurantSheetKind)
        case masters(RestaurantMastersTab)
        case approval

        var id: String {
            switch self {
            case .pos: return "pos"
            case .kot: return "kot"
            case .capture(let kind): return "capture-\(kind.id)"
            case .masters(let tab): return "masters-\(tab.rawValue)"
            case .approval: return "approval"
            }
        }
    }

    private var allowed: Set<String> { Set((model.quickAdds ?? []).map(\.code)) }
    private var owner: Bool { model.role == "OWNER_ADMIN" }
    private var approver: Bool { model.role == "APPROVER" || owner }

    private var venueName: String {
        let title = (model.title ?? "").trimmingCharacters(in: .whitespaces)
        return title.isEmpty ? "Restaurant" : title
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: RestaurantEmber.sectionGap) {
                EmberScreenHeader(
                    title: "Quick Adds",
                    roleLine: "\(venueName) · \(model.subtypeLabel ?? "Restaurant")"
                )
                EmberContextChips(
                    subtypeLabel: model.subtypeLabel ?? "Restaurant",
                    periodLine: RestaurantFormat.periodLine(model.startDate, currency: model.currency),
                    gstOn: model.gstTracking == "YES"
                )

                hubHero

                EmberGridSectionHeader(title: "Core · \(Self.coreTiles.count)", note: "Service journey")
                tileGrid(Self.coreTiles)

                EmberGridSectionHeader(title: "Support · \(Self.supportTiles.count)", note: "Kitchen · masters")
                tileGrid(Self.supportTiles)

                EmberWhyNote(
                    body: "A posted bill owns the sale once. An order is a commitment only — never revenue. "
                        + "Prep status never posts money. Collection and payment allocate; they never duplicate a fact. "
                        + "Empty analytics read Unavailable.",
                    tinted: true
                )

                if let errorText {
                    Text(errorText)
                        .font(.plusJakarta(size: 12))
                        .foregroundStyle(RestaurantEmber.negative)
                        .fixedSize(horizontal: false, vertical: true)
                }

                Spacer(minLength: 24)
            }
            .padding(.horizontal, RestaurantEmber.gutter)
            .padding(.vertical, 16)
        }
        .sheet(item: $presented) { route in
            switch route {
            case .pos:
                RestaurantPosSheet(model: model, onDismiss: { presented = nil }) { draft in
                    post(draft)
                }
            case .kot:
                RestaurantKotSheet(model: model, onDismiss: { presented = nil }) { draft in
                    post(draft)
                }
            case .capture(let kind):
                OperatingCaptureSheet(
                    title: kind.title,
                    icon: kind.icon,
                    eventType: kind.eventType,
                    mode: operatingCaptureMode(eventType: kind.eventType, title: kind.title),
                    model: model,
                    channelLabel: kind == .collection ? "Received from" : (kind == .sale ? "Sales channel" : "Channel"),
                    variant: "restaurant",
                    onSubmit: { draft in
                        do { try await onRecord(RestaurantEventDraft(draft)) }
                        catch { errorText = error.localizedDescription }
                    },
                    onDecide: { id, decision in try await onDecide(id, decision) },
                    onDismiss: { presented = nil }
                )
            case .masters(let tab):
                RestaurantMastersSheetView(
                    tab: tab,
                    model: model,
                    momentId: momentId,
                    onRecord: onRecord
                ) { message in
                    errorText = message
                }
            case .approval:
                OperatingCaptureSheet(
                    title: "Review Approval",
                    icon: RestaurantTileIcons.status,
                    eventType: "APPROVAL",
                    mode: "APPROVAL",
                    model: model,
                    variant: "restaurant",
                    onSubmit: { _ in },
                    onDecide: { id, decision in try await onDecide(id, decision) },
                    onDismiss: { presented = nil }
                )
            }
        }
    }

    /// Wine → ember hero with the white `+ Record Sale` pill.
    private var hubHero: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("RESTAURANT / FOOD")
                .font(.plusJakarta(size: 11, weight: .bold))
                .tracking(0.6)
                .foregroundStyle(RestaurantEmber.heroEyebrowTint)
                .lineLimit(1)
            Text("Order. Serve. Collect.")
                .font(.plusJakarta(size: 22, weight: .bold))
                .foregroundStyle(.white)
                .lineLimit(1)
                .minimumScaleFactor(0.8)
            Text("Order and preparation are not revenue. Posted bill owns the sale. "
                + "Platform AR needs an explicit settlement link.")
                .font(.plusJakarta(size: 12))
                .foregroundStyle(RestaurantEmber.heroBodyTint)
                .fixedSize(horizontal: false, vertical: true)
            if allowed.contains("SALE") {
                EmberCta(label: "+  Record Sale", plus: false) { presented = .pos }
                    .padding(.top, 4)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(20)
        .background(RestaurantEmber.heroGradient)
        .clipShape(RoundedRectangle(cornerRadius: RestaurantEmber.rHero, style: .continuous))
        .emberHeroShadow()
    }

    // MARK: - Tile grid

    private struct HubTile: Identifiable {
        let id: String
        let label: String
        let caption: String
        let asset: String
        var kind: RestaurantSheetKind? = nil
        var requires: String? = nil
        var master: RestaurantMastersTab? = nil
        var opensPos: Bool = false
        var opensKot: Bool = false
        var wide: Bool = false
    }

    private static let coreTiles: [HubTile] = [
        HubTile(id: "sale", label: "Sale", caption: "Bill / direct sale", asset: RestaurantTileIcons.sale, requires: "SALE", opensPos: true),
        HubTile(id: "purchase", label: "Purchase", caption: "Ingredients", asset: RestaurantTileIcons.purchase, kind: .purchase, requires: "PURCHASE"),
        HubTile(id: "collection", label: "Collection", caption: "AR · platform", asset: RestaurantTileIcons.collection, kind: .collection, requires: "COLLECTION"),
        HubTile(id: "payment", label: "Payment", caption: "Settle AP", asset: RestaurantTileIcons.payment, kind: .payment, requires: "PAYMENT"),
        HubTile(id: "expense", label: "Expense", caption: "Kitchen · platform", asset: RestaurantTileIcons.expense, kind: .spend, requires: "SPEND"),
        HubTile(id: "stock", label: "Stock", caption: "Count · waste", asset: RestaurantTileIcons.stock, kind: .stock, requires: "STOCK"),
        HubTile(id: "order", label: "Order", caption: "Food / catering", asset: RestaurantTileIcons.order, kind: .order, requires: "ORDER"),
        HubTile(id: "status", label: "Status", caption: "Prep · fulfil", asset: RestaurantTileIcons.status, requires: "ORDER", opensKot: true),
        HubTile(id: "budget", label: "Budget", caption: "Versioned revise", asset: RestaurantTileIcons.budget, kind: .budget, requires: "BUDGET"),
    ]

    private static let supportTiles: [HubTile] = [
        HubTile(id: "vendors", label: "Vendors", caption: "Suppliers", asset: RestaurantTileIcons.vendors, master: .vendors),
        HubTile(id: "menu", label: "Menu", caption: "Items · modifiers", asset: RestaurantTileIcons.menu, master: .menu),
        HubTile(id: "stations", label: "Stations", caption: "Prep · pass", asset: RestaurantTileIcons.stations, master: .stations),
        HubTile(id: "invite", label: "Invite & QR", caption: "Join Moment", asset: RestaurantTileIcons.invite, master: .invite, wide: true),
        HubTile(id: "issue", label: "Issue", caption: "Update · approval", asset: RestaurantTileIcons.issue, kind: .issue, requires: "UPDATE", wide: true),
    ]

    /// 3-column grid of 113 × 112 tiles; the two `wide` tiles share a 2-column row
    /// (177 wide, matching the Figma `Invite & QR` + `Issue` row).
    private func tileGrid(_ tiles: [HubTile]) -> some View {
        let rows = Self.rows(tiles)
        return VStack(spacing: 8) {
            ForEach(Array(rows.enumerated()), id: \.offset) { _, row in
                HStack(spacing: 8) {
                    ForEach(row) { tile in
                        EmberQuickAddTile(
                            asset: tile.asset,
                            label: tile.label,
                            caption: tile.caption,
                            locked: lockReason(tile),
                            onTap: { open(tile) }
                        )
                        .frame(maxWidth: .infinity)
                    }
                    if row.count < 3 && row.first?.wide != true {
                        ForEach(0..<(3 - row.count), id: \.self) { _ in
                            Color.clear.frame(maxWidth: .infinity)
                        }
                    }
                }
            }
        }
    }

    /// Packs tiles into rows of 3, except `wide` tiles which pair two-up.
    private static func rows(_ tiles: [HubTile]) -> [[HubTile]] {
        var rows: [[HubTile]] = []
        var row: [HubTile] = []
        func flush() {
            if !row.isEmpty { rows.append(row); row = [] }
        }
        for tile in tiles {
            if tile.wide {
                flush()
                if let last = rows.last, last.first?.wide == true, last.count < 2 {
                    rows[rows.count - 1].append(tile)
                } else {
                    rows.append([tile])
                }
            } else {
                row.append(tile)
                if row.count == 3 { flush() }
            }
        }
        flush()
        return rows
    }

    private func lockReason(_ tile: HubTile) -> String? {
        guard let requires = tile.requires else { return nil }
        return allowed.contains(requires) ? nil : "Role locked"
    }

    private func open(_ tile: HubTile) {
        if tile.opensPos {
            presented = .pos
        } else if tile.opensKot {
            presented = .kot
        } else if let master = tile.master {
            presented = .masters(master)
        } else if let kind = tile.kind {
            presented = .capture(kind)
        }
    }

    private func post(_ draft: RestaurantEventDraft) {
        presented = nil
        Task {
            do { try await onRecord(draft) }
            catch { errorText = error.localizedDescription }
        }
    }
}

/// The 10 capture sheets, named for their `RST-QA-*` frames.
enum RestaurantSheetKind: String, Identifiable {
    case sale, purchase, collection, payment, spend, stock, order, status, budget, issue

    var id: String { rawValue }

    var title: String {
        switch self {
        case .sale: return "Record Sale"
        case .purchase: return "Record Purchase"
        case .collection: return "Money Collection"
        case .payment: return "Record Payment"
        case .spend: return "Record Expense"
        case .stock: return "Stock Update"
        case .order: return "Record Order"
        case .status: return "Status / Assignment"
        case .budget: return "Revise Budget"
        case .issue: return "Issue / Update"
        }
    }

    var eventType: String {
        switch self {
        case .sale: return "SALE"
        case .purchase: return "PURCHASE"
        case .collection: return "COLLECTION"
        case .payment: return "PAYMENT"
        case .spend: return "SPEND"
        case .stock: return "STOCK"
        case .order, .status: return "ORDER"
        case .budget: return "BUDGET"
        case .issue: return "ISSUE"
        }
    }

    var icon: String {
        switch self {
        case .sale: return RestaurantTileIcons.sale
        case .purchase: return RestaurantTileIcons.purchase
        case .collection: return RestaurantTileIcons.collection
        case .payment: return RestaurantTileIcons.payment
        case .spend: return RestaurantTileIcons.expense
        case .stock: return RestaurantTileIcons.stock
        case .order, .status: return RestaurantTileIcons.order
        case .budget: return RestaurantTileIcons.budget
        case .issue: return RestaurantTileIcons.issue
        }
    }
}

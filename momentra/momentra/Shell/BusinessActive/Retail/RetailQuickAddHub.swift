import SwiftUI

/// Quick Add hub — Figma RTL-HUB-v5 `2399:29887`.
///
/// 9 core tiles (3 × 3) + 5 support tiles (3 + 2 wide). A tile the role cannot submit
/// stays in the grid and renders locked, so the layout keeps its shape instead of
/// silently shrinking.
struct RetailQuickAddHub: View {
    let model: OperatingProjectionPayload
    var momentId: String? = nil
    var onRecord: (RetailEventDraft) async throws -> Void
    var onDecide: (String, String) async throws -> Void

    @State private var presented: HubPresentation?
    @State private var errorText: String?

    /// One presentation slot for every hub route: a capture sheet, a masters surface, or
    /// the approval review. A single slot keeps SwiftUI to exactly one sheet per view.
    private enum HubPresentation: Identifiable {
        case capture(RetailSheetKind)
        case masters(RetailMastersTab)
        case approval

        var id: String {
            switch self {
            case .capture(let kind): return "capture-\(kind.rawValue)"
            case .masters(let tab): return "masters-\(tab.rawValue)"
            case .approval: return "approval"
            }
        }
    }

    private var allowed: Set<String> { Set((model.quickAdds ?? []).map(\.code)) }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: RetailRose.sectionGap) {
                RetailScreenHeader(
                    title: "Quick Adds",
                    roleLine: "\(subtypeName) · \(model.subtypeLabel ?? "Retail Store")"
                )
                RoseContextChips(
                    subtypeLabel: model.subtypeLabel ?? "Retail Store",
                    periodLine: RetailFormat.periodLine(model.startDate, currency: model.currency),
                    gstOn: model.gstTracking == "YES"
                )

                hubHero

                GridSectionHeader(title: "Core · \(Self.coreTiles.count)", note: "Retail journey")
                tileGrid(Self.coreTiles)

                GridSectionHeader(title: "Support · \(Self.supportTiles.count)", note: "Network · masters")
                tileGrid(Self.supportTiles)

                RoseWhyNote(
                    body: "Boundaries: a posted sale is revenue once. A quick amount never moves SKU stock. "
                        + "An order is a commitment only. Collection and payment allocate — they never duplicate a fact. "
                        + "Empty analytics read Unavailable.",
                    tinted: true
                )

                if let errorText {
                    Text(errorText)
                        .font(.plusJakarta(size: 12))
                        .foregroundStyle(RetailRose.negative)
                        .fixedSize(horizontal: false, vertical: true)
                }

                Spacer(minLength: 24)
            }
            .padding(.horizontal, RetailRose.gutter)
            .padding(.vertical, 16)
        }
        .sheet(item: $presented) { route in
            switch route {
            case .capture(let kind):
                captureSheet(
                    title: kind.title,
                    icon: kind.icon,
                    eventType: kind.eventType,
                    mode: kind.mode,
                    channelLabel: (kind == .sale || kind == .collection) ? "Sales channel" : "Channel"
                )
            case .masters(let tab):
                RetailMastersSheetView(
                    tab: tab,
                    model: model,
                    momentId: momentId,
                    onRecord: onRecord
                ) { message in
                    errorText = message
                }
            case .approval:
                captureSheet(
                    title: "Review Approval",
                    icon: RetailTileIcons.status,
                    eventType: "APPROVAL",
                    mode: "APPROVAL"
                )
            }
        }
    }

    private var subtypeName: String {
        let title = (model.title ?? "").trimmingCharacters(in: .whitespaces)
        return title.isEmpty ? "Retail Store" : title
    }

    /// Rose gradient hero with the white `+ Record Sale` pill.
    private var hubHero: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("RETAIL STORE")
                .font(.plusJakarta(size: 11, weight: .bold))
                .tracking(0.6)
                .foregroundStyle(RetailRose.roseMuted)
                .lineLimit(1)
            Text("Stock. Sell. Settle.")
                .font(.plusJakarta(size: 22, weight: .bold))
                .foregroundStyle(.white)
                .lineLimit(1)
                .minimumScaleFactor(0.8)
            Text("Quick amount never moves SKU stock. Credit sales need a named customer. "
                + "Settlement never reposts the sale.")
                .font(.plusJakarta(size: 12))
                .foregroundStyle(RetailRose.roseSoft)
                .fixedSize(horizontal: false, vertical: true)
            if allowed.contains("SALE") {
                RoseCta(label: "+  Record Sale", plus: false) { presented = .capture(.sale) }
                    .padding(.top, 4)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(20)
        .background(
            LinearGradient(
                colors: [RetailRose.rose, RetailRose.roseBorder],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        )
        .clipShape(RoundedRectangle(cornerRadius: RetailRose.rHero, style: .continuous))
        .shadow(color: RetailRose.indigo.opacity(0.5), radius: 16, x: 0, y: 12)
        .shadow(color: RetailRose.rose.opacity(0.3), radius: 32, x: 0, y: 24)
    }

    // MARK: - Tile grid

    private struct HubTile: Identifiable {
        let id: String
        let label: String
        let caption: String
        let asset: String
        var kind: RetailSheetKind? = nil
        var requires: String? = nil
        var master: RetailMastersTab? = nil
        var wide: Bool = false
    }

    private static let coreTiles: [HubTile] = [
        HubTile(id: "sale", label: "Sale", caption: "Quick or itemized", asset: RetailTileIcons.sale, kind: .sale, requires: "SALE"),
        HubTile(id: "purchase", label: "Purchase", caption: "Goods · receive", asset: RetailTileIcons.purchase, kind: .purchase, requires: "PURCHASE"),
        HubTile(id: "collection", label: "Collection", caption: "Allocate open AR", asset: RetailTileIcons.collection, kind: .collection, requires: "COLLECTION"),
        HubTile(id: "payment", label: "Payment", caption: "Settle open AP", asset: RetailTileIcons.payment, kind: .payment, requires: "PAYMENT"),
        HubTile(id: "expense", label: "Expense", caption: "Store running", asset: RetailTileIcons.expense, kind: .spend, requires: "SPEND"),
        HubTile(id: "stock", label: "Stock", caption: "Count · damage", asset: RetailTileIcons.stock, kind: .stock, requires: "STOCK"),
        HubTile(id: "order", label: "Order", caption: "Optional customer", asset: RetailTileIcons.order, kind: .order, requires: "ORDER"),
        HubTile(id: "status", label: "Status", caption: "Dispatch / fulfil", asset: RetailTileIcons.status, kind: .status, requires: "ORDER"),
        HubTile(id: "budget", label: "Budget", caption: "Versioned revise", asset: RetailTileIcons.budget, kind: .budget, requires: "BUDGET"),
    ]

    private static let supportTiles: [HubTile] = [
        HubTile(id: "suppliers", label: "Suppliers", caption: "Vendors", asset: RetailTileIcons.suppliers, master: .suppliers),
        HubTile(id: "catalogue", label: "Catalogue", caption: "SKU · UOM", asset: RetailTileIcons.catalogue, master: .catalogue),
        HubTile(id: "locations", label: "Locations", caption: "Stores · aisles", asset: RetailTileIcons.locations, master: .locations),
        HubTile(id: "invite", label: "Invite & QR", caption: "Join · people · roles", asset: RetailTileIcons.invite, master: .invite, wide: true),
        HubTile(id: "issue", label: "Issue", caption: "Update · approval", asset: RetailTileIcons.issue, kind: .issue, requires: "UPDATE", wide: true),
    ]

    /// 3-column grid of 113 × 112 tiles; the two `wide` tiles share a 2-column row
    /// (177 wide, matching the Figma `Invite & QR` + `Issue` row).
    private func tileGrid(_ tiles: [HubTile]) -> some View {
        let rows = Self.rows(tiles)
        return VStack(spacing: 8) {
            ForEach(Array(rows.enumerated()), id: \.offset) { _, row in
                HStack(spacing: 8) {
                    ForEach(row) { tile in
                        QuickAddTile(
                            asset: tile.asset,
                            label: tile.label,
                            caption: tile.caption,
                            locked: lockReason(tile),
                            onTap: { open(tile) }
                        )
                        .frame(maxWidth: .infinity)
                    }
                    // Keep a short final row aligned to the 3-column rhythm.
                    if row.count < 3 && row.first?.wide != true {
                        ForEach(0..<(3 - row.count), id: \.self) { _ in
                            Color.clear.frame(maxWidth: .infinity)
                        }
                    }
                }
            }
        }
    }

    /// Packs tiles into rows of 3, except `wide` tiles which pair two-up (177 wide each,
    /// exactly the Figma `Invite & QR` + `Issue` row).
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
        if let master = tile.master {
            presented = .masters(master)
        } else if let kind = tile.kind {
            presented = .capture(kind)
        }
    }

    // MARK: - Capture sheets

    private func captureSheet(
        title: String,
        icon: String,
        eventType: String,
        mode: String,
        channelLabel: String = "Channel"
    ) -> some View {
        OperatingCaptureSheet(
            title: title,
            icon: icon,
            eventType: eventType,
            mode: mode,
            model: model,
            channelLabel: channelLabel,
            onSubmit: { draft in
                do { try await onRecord(RetailEventDraft(draft)) } catch { errorText = error.localizedDescription }
            },
            onDecide: { id, decision in try await onDecide(id, decision) },
            onDismiss: { presented = nil }
        )
    }
}

/// The 10 capture sheets, named for their Figma frames.
enum RetailSheetKind: String, Identifiable {
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

    var mode: String {
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
        case .sale: return RetailTileIcons.sale
        case .purchase: return RetailTileIcons.purchase
        case .collection: return RetailTileIcons.collection
        case .payment: return RetailTileIcons.payment
        case .spend: return RetailTileIcons.expense
        case .stock: return RetailTileIcons.stock
        case .order: return RetailTileIcons.order
        case .status: return RetailTileIcons.status
        case .budget: return RetailTileIcons.budget
        case .issue: return RetailTileIcons.issue
        }
    }
}

/// Retail-side capture draft: exactly the fields the shared sheet produces, carried
/// across the module boundary so `POST …/operating-events` keeps its existing body.
struct RetailEventDraft {
    var eventType: String
    var title: String
    var amount: String?
    var note: String?
    var channelName: String?
    var counterpartyName: String?
    var paymentStatus: String?
    var occurredAt: String?
    var quantity: String? = nil
    var unit: String? = nil
    var unitPrice: String? = nil
    var itemName: String? = nil
    var parentEventId: String? = nil
    var revisesEventId: String? = nil
    var masterKind: String? = nil
    var dueAt: String? = nil
    var contract: OperatingContractInput? = nil
    var lineItems: [(itemName: String, quantity: String?, unit: String?, unitPrice: String?, masterId: String?)]? = nil

    init(_ draft: OperatingCaptureDraft) {
        eventType = draft.eventType
        title = draft.title
        amount = draft.amount
        note = draft.note
        channelName = draft.channelName
        counterpartyName = draft.counterpartyName
        paymentStatus = draft.paymentStatus
        occurredAt = draft.occurredAt
        quantity = draft.quantity
        unit = draft.unit
        unitPrice = draft.unitPrice
        itemName = draft.itemName
        parentEventId = draft.parentEventId
        revisesEventId = draft.revisesEventId
        masterKind = draft.masterKind
        dueAt = draft.dueAt
        contract = draft.contract
        lineItems = draft.lineItems
    }
}

import SwiftUI

/// `TRD-HUB-v11` / `DST-HUB-v11` — mirrors Android `OperatingV12HubScreen`.
struct OperatingV12HubScreen: View {
    let model: OperatingProjectionPayload
    let theme: OperatingV12Theme
    var onCapture: (String) -> Void
    var onOpenMasters: (String) -> Void = { _ in }

    @State private var query = ""

    private struct Tile: Identifiable {
        let id: String
        let title: String
        let subtitle: String
        let iconAsset: String
        let master: String?
        let requires: [String]

        init(
            id: String,
            title: String,
            subtitle: String,
            iconAsset: String? = nil,
            master: String? = nil,
            requires: [String] = []
        ) {
            self.id = id
            self.title = title
            self.subtitle = subtitle
            self.iconAsset = iconAsset ?? TradingChromeIcons.hubAsset(id)
            self.master = master
            self.requires = requires.map { $0.uppercased() }
        }
    }

    private var allowed: Set<String> {
        Set((model.quickAdds ?? []).map { $0.code.uppercased() })
    }

    private var isDistribution: Bool { theme.verticalKey == "distribution" }

    /// DST-HUB-v11 Money group.
    private var money: [Tile] {
        [
            Tile(id: "SALE", title: "Sale", subtitle: "Invoice · dealer · GST", requires: ["SALE"]),
            Tile(id: "PURCHASE", title: "Purchase", subtitle: "Supplier · GRN", requires: ["PURCHASE"]),
            Tile(id: "COLLECTION", title: "Collection", subtitle: "Allocate open AR", requires: ["COLLECTION"]),
            Tile(id: "PAYMENT", title: "Payment", subtitle: "Settle open AP", requires: ["PAYMENT"]),
            Tile(id: "SPEND", title: "Expense", subtitle: "Route · van · depot", requires: ["SPEND"]),
        ]
    }

    /// DST-HUB-v11 Operations group.
    private var operations: [Tile] {
        [
            Tile(id: "STOCK", title: "Stock", subtitle: "Adjust · count · batch", requires: ["STOCK"]),
            Tile(id: "RETURN", title: "Return", subtitle: "Credit · goods outcome", requires: ["RETURN"]),
            Tile(id: "ORDER", title: "Order", subtitle: "SO · PO commitment", requires: ["ORDER", "CUSTOMER_ORDER"]),
            Tile(id: "SHIPMENT", title: "Delivery", subtitle: "Dispatch · POD", requires: ["SHIPMENT", "DELIVERY"]),
            Tile(id: "BUDGET", title: "Budget", subtitle: "Versioned revise", requires: ["BUDGET"]),
        ]
    }

    /// DST-HUB-v11 People group.
    private var people: [Tile] {
        [
            Tile(id: "PRINCIPALS", title: "Principals & dealers", subtitle: "Principals · dealers · channels", master: "Principals"),
            Tile(id: "CATALOGUE", title: "Catalogue & SKU", subtitle: "SKU · batch · price", master: "Catalogue"),
            Tile(id: "LOCATIONS", title: "Warehouses & routes", subtitle: "Depots · beats", master: "Locations"),
            Tile(id: "INVITE", title: "Invite & QR", subtitle: "Join · people · roles", master: "Invite"),
            Tile(id: "UPDATE", title: "Update / Issue", subtitle: "Update · blocker · risk", requires: ["UPDATE", "ISSUE"]),
        ]
    }

    private var core: [Tile] {
        [
            Tile(id: "SALE", title: "Sale", subtitle: "TRD-QA-01 · amount · buyer", requires: ["SALE"]),
            Tile(id: "PURCHASE", title: "Purchase", subtitle: "TRD-QA-02 · supplier · goods", requires: ["PURCHASE"]),
            Tile(id: "COLLECTION", title: "Collection", subtitle: "TRD-QA-03 · allocate AR", requires: ["COLLECTION"]),
            Tile(id: "PAYMENT", title: "Payment", subtitle: "TRD-QA-04 · settle AP", requires: ["PAYMENT"]),
            Tile(id: "SPEND", title: "Expense", subtitle: "TRD-QA-05 · paid / later", requires: ["SPEND"]),
            Tile(id: "STOCK", title: "Stock", subtitle: "TRD-QA-06 · set · add · reduce", requires: ["STOCK"]),
            Tile(id: "RETURN", title: "Return", subtitle: "TRD-QA-07 · credit note", requires: ["RETURN"]),
            Tile(id: "BUDGET", title: "Budget", subtitle: "TRD-QA-08 · versioned revise", requires: ["BUDGET"]),
            Tile(id: "ORDER", title: "Order", subtitle: "TRD-QA-11 · commitment only", requires: ["ORDER", "CUSTOMER_ORDER"]),
            Tile(id: "SHIPMENT", title: "Delivery", subtitle: "TRD-QA-12 · goods move", requires: ["SHIPMENT", "DELIVERY"]),
        ]
    }

    private var support: [Tile] {
        [
            Tile(
                id: "PRINCIPALS",
                title: "Suppliers & buyers",
                subtitle: "Suppliers · retailers",
                master: "Principals"
            ),
            Tile(
                id: "CATALOGUE",
                title: "Catalogue & SKU",
                subtitle: "SKU · batch · price",
                master: "Catalogue"
            ),
            Tile(
                id: "LOCATIONS",
                title: "Godowns & branches",
                subtitle: "Depots · beats",
                master: "Locations"
            ),
            Tile(id: "INVITE", title: "Invite & QR", subtitle: "Join · people · roles", master: "Invite"),
            Tile(
                id: "UPDATE",
                title: "Update / Issue",
                subtitle: "TRD-QA-09 · ops signal",
                requires: ["UPDATE", "ISSUE"]
            ),
            Tile(
                id: "APPROVAL",
                title: "Approval",
                subtitle: "TRD-QA-10 · decide · reimburse",
                requires: ["UPDATE", "ISSUE", "PAYMENT"]
            ),
        ]
    }

    private var utilities: [Tile] {
        [
            Tile(id: "HOLD", title: "Hold", subtitle: "Freeze open order or DO", requires: ["ORDER", "CUSTOMER_ORDER"]),
            Tile(id: "TRANSFER", title: "Transfer", subtitle: "Move stock between locations", requires: ["STOCK"]),
            Tile(id: "REPRINT", title: "Reprint", subtitle: "Invoice · duplicate copy", requires: ["SALE"]),
        ]
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 24) {
            // Figma Hub Hero `5:16497` — Trading Quick Adds only.
            if !isDistribution {
                OperatingV12HubHero(model: model, theme: theme)
            }
            searchBar
            if isDistribution {
                tileGrid(
                    title: "Money · \(filtered(money).count)",
                    trailing: "Sale · settle · spend",
                    tiles: filtered(money)
                ) { tile in
                    onCapture(tile.id)
                }
                tileGrid(
                    title: "Operations · \(filtered(operations).count)",
                    trailing: "Stock · order · delivery",
                    tiles: filtered(operations)
                ) { tile in
                    onCapture(tile.id)
                }
                tileGrid(
                    title: "People · \(filtered(people).count)",
                    trailing: "Principals · catalogue · routes",
                    tiles: filtered(people)
                ) { tile in
                    if let master = tile.master {
                        onOpenMasters(master)
                    } else {
                        onCapture(tile.id)
                    }
                }
                tileGrid(
                    title: "Utilities · \(filtered(utilities).count)",
                    trailing: "Hold · transfer · reprint",
                    tiles: filtered(utilities)
                ) { tile in
                    onCapture(tile.id)
                }
            } else {
                tileGrid(
                    title: "Core actions · \(filtered(core).count)",
                    trailing: "Tap to record",
                    tiles: filtered(core)
                ) { tile in
                    onCapture(tile.id)
                }
                tileGrid(
                    title: "Support & management · \(filtered(support).count)",
                    trailing: "Role-gated",
                    tiles: filtered(support)
                ) { tile in
                    if let master = tile.master {
                        onOpenMasters(master)
                    } else {
                        onCapture(tile.id)
                    }
                }
                tileGrid(
                    title: "Utilities · \(filtered(utilities).count)",
                    trailing: "Hold · transfer · reprint",
                    tiles: filtered(utilities)
                ) { tile in
                    onCapture(tile.id)
                }
            }
            whyCard
        }
    }

    private var searchBar: some View {
        HStack(spacing: 8) {
            Image(TradingChromeIcons.hubAsset("SEARCH"))
                .renderingMode(.template)
                .resizable()
                .scaledToFit()
                .frame(width: 15, height: 15)
                .foregroundStyle(Color(hex: "#64748B"))
            TextField("Search actions...", text: $query)
                .font(v12Font(14, weight: .medium))
                .foregroundStyle(OperatingV12.title)
                .textFieldStyle(.plain)
        }
        .padding(.horizontal, 16)
        .frame(height: 48)
        .background(Color.white.opacity(0.04))
        .overlay(
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .stroke(Color.white.opacity(0.1), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
    }

    private func filtered(_ tiles: [Tile]) -> [Tile] {
        let q = query.trimmingCharacters(in: .whitespacesAndNewlines)
        if q.isEmpty { return tiles }
        return tiles.filter {
            $0.title.localizedCaseInsensitiveContains(q) || $0.subtitle.localizedCaseInsensitiveContains(q)
        }
    }

    private func lockedReason(_ tile: Tile) -> String? {
        if tile.master != nil || tile.requires.isEmpty || allowed.isEmpty { return nil }
        if tile.requires.contains(where: { allowed.contains($0) }) { return nil }
        return tile.id == "RETURN" ? "Unavailable" : "Role locked"
    }

    private func tileGrid(
        title: String,
        trailing: String,
        tiles: [Tile],
        onOpen: @escaping (Tile) -> Void
    ) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            OperatingV12SectionHead(title: title, trailing: trailing, trailingTone: OperatingV12.cyan)
            if tiles.isEmpty {
                Text("No action matches.")
                    .font(v12Font(12, weight: .medium))
                    .foregroundStyle(OperatingV12.subtitle)
            } else {
                let rows = stride(from: 0, to: tiles.count, by: 3).map { start in
                    Array(tiles[start..<min(start + 3, tiles.count)])
                }
                ForEach(Array(rows.enumerated()), id: \.offset) { _, row in
                    HStack(spacing: 8) {
                        ForEach(row) { tile in
                            let locked = lockedReason(tile)
                            OperatingV12ActionTile(
                                theme: theme,
                                title: tile.title,
                                subtitle: tile.subtitle,
                                iconAsset: tile.iconAsset,
                                lockedReason: locked
                            ) {
                                if locked == nil { onOpen(tile) }
                            }
                        }
                        if row.count < 3 {
                            ForEach(0..<(3 - row.count), id: \.self) { _ in
                                Color.clear.frame(maxWidth: .infinity)
                            }
                        }
                    }
                }
            }
        }
    }

    private var whyCard: some View {
        let rules = [
            "Sale ≠ receipt — cash lands only via Collection",
            "Order = commitment only, no stock or ledger move",
            "Delivery = physical movement only",
            "Submit ≠ approval ≠ payment",
            "Stock changes only on receipt / dispatch",
            isDistribution
                ? "Supplier stock in → dealer dispatch out → route settles collections"
                : "Supplier stock in → buyer dispatch out → route settles collections",
        ]
        return VStack(alignment: .leading, spacing: 8) {
            Text("Why these tiles save differently")
                .font(v12Font(13, weight: .bold))
                .foregroundStyle(OperatingV12.cyan)
            ForEach(rules, id: \.self) { rule in
                Text("• \(rule)")
                    .font(v12Font(12, weight: .medium))
                    .foregroundStyle(Color(hex: "#CBD5E1"))
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(OperatingV12.cyan.opacity(0.08))
        .overlay(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .stroke(OperatingV12.cyan.opacity(0.4), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
        .shadow(color: OperatingV12.cyan.opacity(0.25), radius: 24, y: 8)
    }
}

import SwiftUI

/// `TRD-HUB-v11` / `DST-HUB-v11`.
///
/// The hub is the one destination that is purely navigation: its hero states the moment's motion,
/// then every tile is a door to a capture or a master. Tiles keep the frame's order — the nine
/// core captures first, the five management surfaces after — because an operator learns a grid by
/// position, not by label, and a re-ordered grid would be a different grid.
struct OperatingHubScreen: View {
    @ObservedObject var model: BusinessOperatingModel
    let theme: OperatingTheme
    var onCapture: (String) -> Void
    var onOpenMasters: (OperatingMasterTab) -> Void
    var onOpenStory: () -> Void = {}

    @State private var query = ""

    private struct Tile: Identifiable {
        let id: String
        let title: String
        let subtitle: String
        let glyph: String
        /// `nil` for a capture; the masters tab for a management surface.
        let master: OperatingMasterTab?
        /// Projection `quickAdds` codes that unlock this tile. Empty = always open (masters).
        let requires: [String]

        init(
            id: String,
            title: String,
            subtitle: String,
            glyph: String,
            master: OperatingMasterTab? = nil,
            requires: [String] = []
        ) {
            self.id = id
            self.title = title
            self.subtitle = subtitle
            self.glyph = glyph
            self.master = master
            self.requires = requires.map { $0.uppercased() }
        }
    }

    /// Role capability codes from the projection. Empty ⇒ leave tiles open (still loading).
    private var allowed: Set<String> {
        Set((model.projection?.quickAdds ?? []).map { $0.code.uppercased() })
    }

    /// The nine core captures. Codes are the capture sheet's own modes, so a tile can never ask
    /// for a layout the sheet does not have. MFG uses plant `MFG-HUB-v3` order; MTO uses its own hub.
    private var core: [Tile] {
        if model.vertical == .standardProduction {
            return [
                Tile(id: "SALE", title: "Sale", subtitle: "Invoice · buyer · GST", glyph: "doc.text", requires: ["SALE"]),
                Tile(id: "PURCHASE", title: "Purchase", subtitle: "Material · GRN", glyph: "cart", requires: ["PURCHASE"]),
                Tile(id: "COLLECTION", title: "Collection", subtitle: "Allocate open AR", glyph: "arrow.down.circle", requires: ["COLLECTION"]),
                Tile(id: "PAYMENT", title: "Payment", subtitle: "Settle open AP", glyph: "creditcard", requires: ["PAYMENT"]),
                Tile(id: "SPEND", title: "Expense", subtitle: "Running plant cost", glyph: "wallet.pass", requires: ["SPEND"]),
                Tile(id: "STOCK", title: "Stock", subtitle: "Material · WIP · FG", glyph: "shippingbox", requires: ["STOCK"]),
                Tile(id: "ORDER", title: "Order", subtitle: "Customer · commitment", glyph: "list.bullet.rectangle", requires: ["ORDER", "CUSTOMER_ORDER"]),
                Tile(id: "PRODUCTION", title: "Production", subtitle: "Output · batch · scrap", glyph: "hammer", requires: ["PRODUCTION"]),
                Tile(id: "BUDGET", title: "Budget", subtitle: "Versioned revise", glyph: "target", requires: ["BUDGET"]),
            ]
        }
        return [
            Tile(id: "SALE", title: "Sale", subtitle: "Invoice · buyer · GST", glyph: "doc.text", requires: ["SALE"]),
            Tile(id: "PURCHASE", title: "Purchase", subtitle: "Supplier · GRN", glyph: "cart", requires: ["PURCHASE"]),
            Tile(id: "COLLECTION", title: "Collection", subtitle: "Allocate open AR", glyph: "arrow.down.circle", requires: ["COLLECTION"]),
            Tile(id: "PAYMENT", title: "Payment", subtitle: "Settle open AP", glyph: "creditcard", requires: ["PAYMENT"]),
            Tile(id: "SPEND", title: "Expense", subtitle: "Route · van · depot", glyph: "wallet.pass", requires: ["SPEND"]),
            Tile(id: "STOCK", title: "Stock", subtitle: "Adjust · count · batch", glyph: "shippingbox", requires: ["STOCK"]),
            Tile(id: "ORDER", title: "Order", subtitle: "SO · PO commitment", glyph: "list.bullet.rectangle", requires: ["ORDER", "CUSTOMER_ORDER"]),
            Tile(id: "SHIPMENT", title: "Delivery", subtitle: "Dispatch · POD", glyph: "truck.box", requires: ["SHIPMENT", "DELIVERY"]),
            Tile(id: "BUDGET", title: "Budget", subtitle: "Versioned revise", glyph: "target", requires: ["BUDGET"]),
        ]
    }

    private var utilities: [Tile] {
        guard model.vertical == .standardProduction else { return [] }
        return [
            Tile(id: "REWORK", title: "Rework / Scrap", subtitle: "Rework · scrap · job work", glyph: "wrench.and.screwdriver", requires: ["STOCK"]),
            Tile(id: "HOLD", title: "Hold", subtitle: "Park work order", glyph: "pause.circle", requires: ["ORDER", "CUSTOMER_ORDER"]),
            Tile(id: "TRANSFER", title: "WIP transfer", subtitle: "Between lines · stores", glyph: "arrow.left.arrow.right", requires: ["STOCK"]),
            Tile(id: "REPRINT", title: "Reprint", subtitle: "Invoice · job card", glyph: "printer", requires: ["SALE"]),
            Tile(id: "RETURN", title: "Return", subtitle: "Credit · goods outcome", glyph: "arrow.uturn.left", requires: ["RETURN"]),
            Tile(id: "DELIVERY", title: "Delivery", subtitle: "Dispatch · POD", glyph: "truck.box", requires: ["DELIVERY", "SHIPMENT"]),
        ]
    }

    /// The five management surfaces. The first four are masters tabs; `Issue` posts an update.
    private var support: [Tile] {
        let principalsTitle: String
        let principalsSubtitle: String
        let catalogueTitle: String
        let catalogueSubtitle: String
        let locationsTitle: String
        let locationsSubtitle: String
        if model.vertical == .standardProduction {
            principalsTitle = "Vendors & buyers"
            principalsSubtitle = "Vendors · customers"
            catalogueTitle = "Parts & BOM"
            catalogueSubtitle = "Part · SKU · BOM"
            locationsTitle = "Lines & stores"
            locationsSubtitle = "Lines · WIP · FG"
        } else if model.vertical == .trading {
            principalsTitle = "Suppliers & buyers"
            principalsSubtitle = "Suppliers · retailers"
            catalogueTitle = "Catalogue & SKU"
            catalogueSubtitle = "SKU · batch · price"
            locationsTitle = "Godowns & branches"
            locationsSubtitle = "Depots · beats"
        } else {
            principalsTitle = "Principals & dealers"
            principalsSubtitle = "Principals · dealers"
            catalogueTitle = "Catalogue & SKU"
            catalogueSubtitle = "SKU · batch · price"
            locationsTitle = "Warehouses & routes"
            locationsSubtitle = "Depots · beats"
        }
        return [
            Tile(
                id: OperatingMasterTab.principals.rawValue,
                title: principalsTitle,
                subtitle: principalsSubtitle,
                glyph: "person.2",
                master: .principals
            ),
            Tile(
                id: OperatingMasterTab.catalogue.rawValue,
                title: catalogueTitle,
                subtitle: catalogueSubtitle,
                glyph: "tag",
                master: .catalogue
            ),
            Tile(
                id: OperatingMasterTab.locations.rawValue,
                title: locationsTitle,
                subtitle: locationsSubtitle,
                glyph: "map",
                master: .locations
            ),
            Tile(
                id: OperatingMasterTab.invite.rawValue,
                title: "Invite & QR",
                subtitle: "Join · people · roles",
                glyph: "qrcode",
                master: .invite
            ),
            Tile(
                id: "UPDATE",
                title: "Issue",
                subtitle: "Update · blocker · risk",
                glyph: "exclamationmark.triangle",
                requires: ["UPDATE", "ISSUE"]
            ),
        ]
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 24) {
                hero
                searchBar
                grid(title: "Core actions · 9", trailing: "Tap to record", tiles: filtered(core))
                grid(title: "Support & management · 5", trailing: "Role-gated", tiles: filtered(support))
                if !utilities.isEmpty {
                    grid(title: "Utilities · \(utilities.count)", trailing: "Hold · rework · reprint", tiles: filtered(utilities))
                }
                whyCard
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 8)
            .padding(.bottom, 24)
        }
        .background(theme.canvas)
    }

    // MARK: Hero

    private var hero: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("QUICK ADDS · v11.1")
                .font(.plusJakarta(size: 11, weight: .bold))
                .tracking(1.2)
                .foregroundStyle(OperatingV12.cyan)
            Text("Quick Adds")
                .font(.plusJakarta(size: 28, weight: .bold))
                .foregroundStyle(.white)
            Text(model.vertical.motionLine)
                .font(.plusJakarta(size: 16, weight: .bold))
                .foregroundStyle(theme.accent)
            Text(identityLine)
                .font(.plusJakarta(size: 12, weight: .medium))
                .foregroundStyle(OperatingV12.subtitle)
            if model.vertical == .standardProduction {
                hubButton(
                    title: "Record Production",
                    glyph: "hammer",
                    filled: true,
                    locked: heroLocked(["PRODUCTION"])
                ) {
                    onCapture("PRODUCTION")
                }
                hubButton(
                    title: "Record Sale",
                    glyph: "doc.text",
                    filled: false,
                    locked: heroLocked(["SALE"])
                ) {
                    onCapture("SALE")
                }
                Text("Accepted production posts finished stock once. Sale raises the \(model.vertical.partyNoun.dropLast().lowercased()) invoice — scrap never becomes sellable qty.")
                    .font(.plusJakarta(size: 11, weight: .medium))
                    .foregroundStyle(OperatingV12.subtitle)
                    .frame(maxWidth: .infinity, alignment: .leading)
            } else {
                hubButton(
                    title: "Dispatch",
                    glyph: "truck.box",
                    filled: true,
                    locked: heroLocked(["SHIPMENT", "DELIVERY"])
                ) {
                    onCapture("SHIPMENT")
                }
                hubButton(
                    title: "Record Sale",
                    glyph: "doc.text",
                    filled: false,
                    locked: heroLocked(["SALE"])
                ) {
                    onCapture("SALE")
                }
                Text("Dispatch moves goods on a route; Record Sale raises the \(model.vertical.partyNoun.dropLast().lowercased()) invoice.")
                    .font(.plusJakarta(size: 11, weight: .medium))
                    .foregroundStyle(OperatingV12.subtitle)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
        }
        .padding(.horizontal, 20)
        .padding(.vertical, 24)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(OperatingV12.surface)
        .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 24, style: .continuous)
                .strokeBorder(OperatingV12.cyan.opacity(0.25), lineWidth: 1)
        )
        .shadow(color: .black.opacity(0.6), radius: 12, x: 0, y: 8)
        .shadow(color: theme.accent.opacity(0.12), radius: 80)
        .shadow(color: OperatingV12.cyan.opacity(0.18), radius: 40)
    }

    /// `Northstar Trading · FY 2026–27 · INR · GST on` — the four facts that change how a capture
    /// behaves, so they sit above the buttons rather than inside a settings sheet.
    private var identityLine: String {
        var parts = [model.title, fiscalYear, model.currency]
        parts.append(gstOn ? "GST on" : "GST off")
        return parts.joined(separator: " · ")
    }

    private var fiscalYear: String {
        guard let start = model.projection?.startDate, let year = Int(start.prefix(4)) else {
            return "FY 2026–27"
        }
        return "FY \(year)–\(String(format: "%02d", (year + 1) % 100))"
    }

    private var gstOn: Bool {
        (model.projection?.gstTracking ?? "").uppercased() == "YES"
    }

    private func heroLocked(_ codes: [String]) -> String? {
        if allowed.isEmpty { return nil }
        return codes.contains(where: { allowed.contains($0) }) ? nil : "Role locked"
    }

    private func lockedReason(_ tile: Tile) -> String? {
        guard tile.master == nil, !tile.requires.isEmpty, !allowed.isEmpty else { return nil }
        guard !tile.requires.contains(where: { allowed.contains($0) }) else { return nil }
        return tile.id == "RETURN" ? "Unavailable" : "Role locked"
    }

    private func hubButton(
        title: String,
        glyph: String,
        filled: Bool,
        locked: String? = nil,
        action: @escaping () -> Void
    ) -> some View {
        let enabled = locked == nil
        return Button(action: action) {
            HStack(spacing: 8) {
                Image(systemName: glyph)
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundStyle(enabled ? (filled ? Color.white : theme.accent) : OperatingV12.subtitle)
                Text(locked ?? title)
                    .font(.plusJakarta(size: 15, weight: .bold))
                    .foregroundStyle(enabled ? (filled ? Color.white : OperatingV12.title) : OperatingV12.subtitle)
            }
            .frame(maxWidth: .infinity)
            .frame(height: 52)
            .background(enabled ? (filled ? theme.accent : Color.white.opacity(0.06)) : Color.white.opacity(0.04))
            .overlay(
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .strokeBorder(
                        enabled ? (filled ? Color.clear : theme.accent.opacity(0.4)) : theme.accent.opacity(0.15),
                        lineWidth: 1
                    )
            )
            .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
            .shadow(color: filled && enabled ? theme.accent.opacity(0.5) : .clear, radius: 20)
        }
        .buttonStyle(.plain)
        .disabled(!enabled)
    }

    // MARK: Search

    private var searchBar: some View {
        HStack(spacing: 8) {
            Image(systemName: "magnifyingglass")
                .font(.system(size: 15, weight: .medium))
                .foregroundStyle(Color(hex: "#64748B"))
            TextField(
                "",
                text: $query,
                prompt: Text("Search actions...").foregroundStyle(Color(hex: "#64748B"))
            )
            .font(.plusJakarta(size: 14, weight: .medium))
            .foregroundStyle(OperatingV12.title)
            .textInputAutocapitalization(.never)
            .autocorrectionDisabled()
            .tint(theme.accent)
        }
        .padding(.horizontal, 16)
        .frame(height: 48)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.white.opacity(0.04))
        .overlay(
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .strokeBorder(Color.white.opacity(0.1), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
    }

    private func filtered(_ tiles: [Tile]) -> [Tile] {
        let trimmed = query.trimmingCharacters(in: .whitespaces)
        guard !trimmed.isEmpty else { return tiles }
        return tiles.filter {
            $0.title.localizedCaseInsensitiveContains(trimmed)
                || $0.subtitle.localizedCaseInsensitiveContains(trimmed)
        }
    }

    // MARK: Grids

    private func grid(title: String, trailing: String, tiles: [Tile]) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            OperatingV12SectionHead(title: title, trailing: trailing, trailingTone: OperatingV12.cyan)
            if tiles.isEmpty {
                Text("No action matches “\(query)”.")
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(OperatingV12.subtitle)
                    .padding(.vertical, 8)
            } else {
                LazyVGrid(
                    columns: Array(repeating: GridItem(.flexible(), spacing: 8), count: 3),
                    spacing: 8
                ) {
                    ForEach(tiles) { tile in
                        let locked = lockedReason(tile)
                        OperatingV12ActionTile(
                            theme: theme,
                            title: tile.title,
                            subtitle: tile.subtitle,
                            glyph: tile.glyph,
                            lockedReason: locked
                        ) {
                            guard locked == nil else { return }
                            open(tile)
                        }
                    }
                }
            }
        }
    }

    private func open(_ tile: Tile) {
        if let master = tile.master {
            onOpenMasters(master)
        } else {
            onCapture(tile.id)
        }
    }

    // MARK: Save rules

    /// The frame's closing card: six rules that say which tile posts what, because the common
    /// mistake in a wholesale trade is treating a delivery as a settlement.
    private var whyCard: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Why these tiles save differently")
                .font(.plusJakarta(size: 13, weight: .bold))
                .foregroundStyle(OperatingV12.cyan)
            ForEach(Self.saveRules, id: \.self) { rule in
                Text("• \(rule)")
                    .font(.plusJakarta(size: 12, weight: .medium))
                    .foregroundStyle(Color(hex: "#CBD5E1"))
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .multilineTextAlignment(.leading)
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(OperatingV12.cyan.opacity(0.08))
        .overlay(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .strokeBorder(OperatingV12.cyan.opacity(0.4), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
        .shadow(color: OperatingV12.cyan.opacity(0.25), radius: 24)
    }

    private static let saveRules = [
        "Sale ≠ receipt — cash lands only via Collection",
        "Order = commitment only, no stock or ledger move",
        "Delivery = physical movement only",
        "Submit ≠ approval ≠ payment",
        "Stock changes only on receipt / dispatch",
        "Supplier stock in → buyer dispatch out → route settles collections",
    ]
}

import SwiftUI

/// `MTO-HUB · Quick Adds` `2686:30690` — make-to-order journey, not MFG plant tiles.
struct MadeToOrderHubScreen: View {
    @ObservedObject var model: BusinessOperatingModel
    let theme: OperatingTheme
    var onCapture: (String) -> Void
    var onOpenMasters: (OperatingMasterTab) -> Void
    var onOpenStory: () -> Void = {}

    @State private var query = ""

    private var allowed: Set<String> {
        Set((model.projection?.quickAdds ?? []).map { $0.code.uppercased() })
    }

    private struct Tile: Identifiable {
        let id: String
        let title: String
        let subtitle: String
        let glyph: String
        let master: OperatingMasterTab?
        let wide: Bool
        /// Capability codes that unlock this capture. Empty = always available (masters / approval).
        let requires: [String]

        init(
            id: String,
            title: String,
            subtitle: String,
            glyph: String,
            master: OperatingMasterTab? = nil,
            wide: Bool = false,
            requires: [String] = []
        ) {
            self.id = id
            self.title = title
            self.subtitle = subtitle
            self.glyph = glyph
            self.master = master
            self.wide = wide
            self.requires = requires
        }
    }

    private var core: [Tile] {
        [
            Tile(id: "ORDER", title: "Quotation", subtitle: "Quote → accept → job", glyph: "doc.text", requires: ["ORDER", "CUSTOMER_ORDER"]),
            Tile(id: "CONFIRM", title: "Confirm", subtitle: "Accept order · no revenue", glyph: "checkmark.seal", requires: ["ORDER", "CUSTOMER_ORDER"]),
            Tile(id: "OPEN_JOB", title: "Open job", subtitle: "From confirmed order", glyph: "hammer", requires: ["UPDATE"]),
            Tile(id: "PRODUCTION", title: "Production", subtitle: "Against job · accept / reject", glyph: "gearshape.2", requires: ["PRODUCTION"]),
            Tile(id: "SALE", title: "Invoice", subtitle: "Against order · GST", glyph: "doc.plaintext", requires: ["SALE"]),
            Tile(id: "COLLECTION", title: "Collection", subtitle: "Allocate open AR", glyph: "arrow.down.circle", requires: ["COLLECTION"]),
            Tile(id: "PAYMENT", title: "Payment", subtitle: "Settle open AP", glyph: "creditcard", requires: ["PAYMENT"]),
            Tile(id: "PURCHASE", title: "Materials", subtitle: "Materials · supplier", glyph: "cart", requires: ["PURCHASE"]),
            Tile(id: "SPEND", title: "Expense", subtitle: "Running cost · job link", glyph: "wallet.pass", requires: ["SPEND"]),
        ]
    }

    private var support: [Tile] {
        [
            Tile(id: "PRINCIPALS", title: "Vendors", subtitle: "Suppliers · buyers", glyph: "person.2", master: .principals),
            Tile(id: "CATALOGUE", title: "Products & BOM", subtitle: "Parts · WIP · FG", glyph: "shippingbox", master: .catalogue),
            Tile(id: "LOCATIONS", title: "Lines & stores", subtitle: "Stations · floors", glyph: "map", master: .locations),
            Tile(id: "INVITE", title: "Invite & QR", subtitle: "Join · people · roles", glyph: "qrcode", master: .invite, wide: true),
            Tile(id: "ISSUE", title: "Issue", subtitle: "Update · blocker · risk", glyph: "exclamationmark.triangle", wide: true, requires: ["UPDATE"]),
        ]
    }

    private var utilities: [Tile] {
        [
            Tile(id: "RETURN", title: "Return", subtitle: "Credit · goods outcome", glyph: "arrow.uturn.left", requires: ["RETURN"]),
            Tile(id: "REWORK", title: "Rework", subtitle: "Rework · scrap · job work", glyph: "wrench.and.screwdriver", requires: ["STOCK"]),
            Tile(id: "STOCK", title: "Stock", subtitle: "Adjust · count · custody", glyph: "shippingbox", requires: ["STOCK"]),
            Tile(id: "TRANSFER", title: "WIP", subtitle: "Transfer between stations", glyph: "arrow.left.arrow.right", requires: ["STOCK"]),
            Tile(id: "HOLD", title: "Hold", subtitle: "Park job · no stock/cost", glyph: "pause.circle", requires: ["ORDER", "CUSTOMER_ORDER"]),
            Tile(id: "DELIVERY", title: "Delivery", subtitle: "Dispatch · accepted only", glyph: "box.truck", requires: ["DELIVERY", "SHIPMENT", "UPDATE"]),
            Tile(id: "REPRINT", title: "Reprint", subtitle: "Invoice · job card", glyph: "printer", requires: ["SALE"]),
            Tile(id: "APPROVAL", title: "Approval", subtitle: "Pending decisions", glyph: "checkmark.shield"),
            Tile(id: "UPDATE", title: "Update", subtitle: "Progress · note", glyph: "text.bubble", requires: ["UPDATE"]),
            Tile(id: "BUDGET", title: "Budget", subtitle: "Versioned revise", glyph: "target", requires: ["BUDGET"]),
            Tile(id: "QUOTE_REVISE", title: "Quote revise", subtitle: "Revise · expire", glyph: "doc.badge.gearshape", requires: ["ORDER", "CUSTOMER_ORDER"]),
            Tile(id: "JOB_STATUS", title: "Job status", subtitle: "Active · blocked · hold", glyph: "list.bullet.rectangle", requires: ["UPDATE"]),
        ]
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                Text("Quick Adds")
                    .font(.plusJakarta(size: 24, weight: .bold))
                    .foregroundStyle(Color(hex: "#F8F9FD"))
                Text("\(model.title) · Make to order")
                    .font(.plusJakarta(size: 12, weight: .medium))
                    .foregroundStyle(theme.accentSoft)
                chips
                hero
                section(title: "Core · 9", trailing: "Make-to-order journey", tiles: filtered(core))
                section(title: "Support · 5", trailing: "Network · masters", tiles: filtered(support))
                section(title: "Utilities · 12", trailing: "Job ops · hold · dispatch · revise", tiles: filtered(utilities))
                why
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
            .padding(.bottom, 24)
        }
        .background(theme.canvas)
    }

    private var chips: some View {
        HStack(spacing: 6) {
            chip("Make to order", accent: true)
            chip(periodLine, accent: false)
            chip(gstOn ? "GST on" : "GST off", accent: false)
        }
    }

    private func chip(_ label: String, accent: Bool) -> some View {
        Text(label)
            .font(.plusJakarta(size: 12, weight: accent ? .bold : .medium))
            .foregroundStyle(accent ? theme.accentSoft : Color(hex: "#A8A29E"))
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
            .background(accent ? theme.accentSoft.opacity(0.14) : Color(hex: "#2A2740"))
            .overlay(
                Capsule().strokeBorder(accent ? theme.accentSoft.opacity(0.45) : Color.white.opacity(0.08), lineWidth: 1)
            )
            .clipShape(Capsule())
    }

    private var hero: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("MAKE TO ORDER")
                .font(.plusJakarta(size: 11, weight: .bold))
                .foregroundStyle(theme.accentSoft)
            Text("Quote. Confirm. Make. Invoice.")
                .font(.plusJakarta(size: 22, weight: .bold))
                .foregroundStyle(.white)
            Text("Quote, confirm, make and invoice in one Moment — quotes and orders never become revenue until a posted invoice.")
                .font(.plusJakarta(size: 12))
                .foregroundStyle(Color(hex: "#F8F9FD"))
            Button { onCapture("ORDER") } label: {
                Text("+  Create Quotation")
                    .font(.plusJakarta(size: 15, weight: .bold))
                    .foregroundStyle(theme.accent)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 16)
                    .background(Color.white)
                    .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
            }
            .buttonStyle(.plain)
            Button { onCapture("CONFIRM") } label: {
                Text("Confirm Order")
                    .font(.plusJakarta(size: 15, weight: .bold))
                    .foregroundStyle(Color(hex: "#F8F9FD"))
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 16)
                    .background(Color.white.opacity(0.06))
                    .overlay(
                        RoundedRectangle(cornerRadius: 16, style: .continuous)
                            .strokeBorder(theme.accentSoft.opacity(0.45), lineWidth: 1)
                    )
                    .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
            }
            .buttonStyle(.plain)
        }
        .padding(20)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            LinearGradient(
                colors: [theme.accentSoft.opacity(0.22), theme.accent.opacity(0.14)],
                startPoint: .leading,
                endPoint: .trailing
            )
        )
        .overlay(
            RoundedRectangle(cornerRadius: 24, style: .continuous)
                .strokeBorder(theme.accentSoft.opacity(0.45), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
    }

    private func section(title: String, trailing: String, tiles: [Tile]) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text(title)
                    .font(.plusJakarta(size: 16, weight: .bold))
                    .foregroundStyle(Color(hex: "#F8F9FD"))
                Spacer()
                Text(trailing)
                    .font(.plusJakarta(size: 11, weight: .medium))
                    .foregroundStyle(Color(hex: "#A8A29E"))
            }
            let columns = [
                GridItem(.flexible(), spacing: 8),
                GridItem(.flexible(), spacing: 8),
                GridItem(.flexible(), spacing: 8),
            ]
            LazyVGrid(columns: columns, spacing: 8) {
                ForEach(tiles) { tile in
                    tileView(tile)
                }
            }
        }
    }

    private func lockedReason(_ tile: Tile) -> String? {
        guard tile.master == nil, !tile.requires.isEmpty, !allowed.isEmpty else { return nil }
        guard !tile.requires.contains(where: { allowed.contains($0) }) else { return nil }
        // RETURN has no manufacturing projection capability yet — honest Unavailable, not a fake sheet.
        return tile.id == "RETURN" ? "Unavailable" : "Role locked"
    }

    private func tileView(_ tile: Tile) -> some View {
        let locked = lockedReason(tile)
        return Button {
            guard locked == nil else { return }
            if let master = tile.master {
                onOpenMasters(master)
            } else {
                onCapture(tile.id)
            }
        } label: {
            VStack(alignment: .leading, spacing: 6) {
                Image(systemName: tile.glyph)
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundStyle(theme.accentSoft)
                    .frame(width: 32, height: 32)
                    .background(theme.accentSoft.opacity(0.16))
                    .overlay(
                        RoundedRectangle(cornerRadius: 10, style: .continuous)
                            .strokeBorder(theme.accentSoft.opacity(locked == nil ? 0.45 : 0.2), lineWidth: 1)
                    )
                    .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                Text(tile.title)
                    .font(.plusJakarta(size: 14, weight: .bold))
                    .foregroundStyle(Color(hex: "#F8F9FD").opacity(locked == nil ? 1 : 0.45))
                    .lineLimit(1)
                Text(locked ?? tile.subtitle)
                    .font(.plusJakarta(size: 10))
                    .foregroundStyle(Color(hex: "#A8A29E"))
                    .lineLimit(2)
                    .frame(maxWidth: .infinity, alignment: .leading)
                Spacer(minLength: 0)
            }
            .padding(12)
            .frame(maxWidth: .infinity, minHeight: 112, alignment: .topLeading)
            .background(Color(hex: "#1C1F28"))
            .overlay(
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .strokeBorder(theme.accentSoft.opacity(locked == nil ? 0.35 : 0.12), lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
        }
        .buttonStyle(.plain)
        .disabled(locked != nil)
    }

    private var why: some View {
        HStack(alignment: .top, spacing: 8) {
            Text("Why")
                .font(.plusJakarta(size: 11, weight: .bold))
                .foregroundStyle(theme.accentSoft)
            Text("Financial boundaries: Quote/order/advance ≠ revenue. Only posted invoice owns revenue/AR. Accepted production posts finished stock once — rejected/rework never inflate sellable qty. Empty analytics = Unavailable, never zero.")
                .font(.plusJakarta(size: 11))
                .foregroundStyle(theme.accentSoft)
                .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(10)
        .background(theme.accentSoft.opacity(0.12))
        .overlay(
            RoundedRectangle(cornerRadius: 10, style: .continuous)
                .strokeBorder(theme.accentSoft.opacity(0.4), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
    }

    private func filtered(_ tiles: [Tile]) -> [Tile] {
        let q = query.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !q.isEmpty else { return tiles }
        return tiles.filter {
            $0.title.localizedCaseInsensitiveContains(q) || $0.subtitle.localizedCaseInsensitiveContains(q)
        }
    }

    private var periodLine: String {
        guard let start = model.projection?.startDate, start.count >= 7 else {
            return model.currency
        }
        let year = String(start.prefix(4))
        let monthIdx = Int(start.dropFirst(5).prefix(2)) ?? 1
        let months = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"]
        let month = months[max(0, min(11, monthIdx - 1))]
        return "\(month) \(year) · \(model.currency)"
    }

    private var gstOn: Bool {
        (model.projection?.gstTracking ?? "").uppercased() == "YES"
    }
}

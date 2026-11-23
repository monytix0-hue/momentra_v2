import SwiftUI

enum DistributionDealershipHub {
    static let typeCode = "DISTRIBUTION_DEALERSHIP"

    static let pulseWidgetOrder = ["DD-ATTENTION", "DD-GST", "P01", "P02", "P03", "P04", "P05"]
    static let momentsWidgetOrder = ["DD-COMMITMENTS", "DD-ORDERS", "DD-STOCK", "DD-PEOPLE", "M01", "M02", "M03"]

  private static let widgetTitles: [String: String] = [
        "DD-ATTENTION": "Needs Attention",
        "DD-GST": "GST Position",
        "DD-COMMITMENTS": "Open Commitments",
        "DD-ORDERS": "Orders & Fulfilment",
        "DD-STOCK": "Stock & Consignment",
        "DD-PEOPLE": "People & Network",
    ]

    static func isDistribution(_ typeCode: String?) -> Bool {
        typeCode?.trimmingCharacters(in: .whitespacesAndNewlines).uppercased() == Self.typeCode
    }

    static func sortWidgets(screen: String, widgets: [OperatingWidgetPayload]) -> [OperatingWidgetPayload] {
        let order = screen.uppercased() == "PULSE" ? pulseWidgetOrder : momentsWidgetOrder
        guard !order.isEmpty else { return widgets }
        let rank = Dictionary(uniqueKeysWithValues: order.enumerated().map { ($1, $0) })
        return widgets.sorted {
            let l = rank[$0.widgetId] ?? Int.max
            let r = rank[$1.widgetId] ?? Int.max
            if l == r { return $0.widgetId < $1.widgetId }
            return l < r
        }
    }

    static func displayTitle(widgetId: String, serverTitle: String) -> String {
        widgetTitles[widgetId] ?? (serverTitle.isEmpty ? widgetId : serverTitle)
    }

    static let quickActionStrip: [(String, String)] = [
        ("SALE", "Sale"),
        ("PURCHASE", "Purchase"),
        ("COLLECTION", "Collection"),
        ("PAYMENT", "Payment"),
    ]

    static let quickAddCore: Set<String> = [
        "SALE", "PURCHASE", "COLLECTION", "PAYMENT", "ORDER", "SHIPMENT", "DELIVERY", "STOCK",
    ]
    static let quickAddSupport: Set<String> = [
        "SPEND", "BUDGET", "UPDATE", "ISSUE", "APPROVAL", "RETURN",
    ]

    static func formatMetricValue(_ raw: String?) -> String {
        let v = raw?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        if v.isEmpty || v == "0" || v == "0.0000" || v == "₹0" { return "Not recorded" }
        return v
    }
}

struct DistributionHeroHeader: View {
    let projection: OperatingProjectionPayload?
    let momentTitle: String?
    let companyId: String?
    let palette: HubPalette

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("DISTRIBUTION")
                .font(.system(size: 12, weight: .medium))
                .foregroundStyle(palette.accent)
            Text(
                projection?.title.flatMap { $0.isEmpty ? nil : $0 }
                    ?? momentTitle
                    ?? projection?.subtypeLabel
                    ?? "Distribution"
            )
            .font(.system(size: 22, weight: .semibold))
            .foregroundStyle(palette.text)
            FlowLayout(spacing: 8) {
                if let party = projection?.partyName, !party.isEmpty {
                    distributionChip(party)
                }
                if companyId?.isEmpty == false {
                    distributionChip("Company")
                }
                if let fy = fyLabel {
                    distributionChip(fy)
                }
                if booksOpen {
                    distributionChip("Books open", emphasized: true)
                }
            }
        }
    }

    private var fyLabel: String? {
        let start = projection?.startDate.map { String($0.prefix(4)) }
        let end = projection?.endDate.map { String($0.prefix(4)) }
        if let start, let end, start != end { return "FY \(start)–\(end)" }
        if let start { return "FY \(start)" }
        return nil
    }

    private var booksOpen: Bool {
        guard let projection else { return false }
        if projection.budget?.configured == true || projection.target?.configured == true { return true }
        return projection.financeVisible == true
    }

    private func distributionChip(_ label: String, emphasized: Bool = false) -> some View {
        Text(label)
            .font(.system(size: 11))
            .foregroundStyle(emphasized ? palette.onAccent : palette.text)
            .padding(.horizontal, 10)
            .padding(.vertical, 6)
            .background(emphasized ? palette.accent : palette.card)
            .overlay(RoundedRectangle(cornerRadius: 20).stroke(palette.border, lineWidth: 1))
            .clipShape(RoundedRectangle(cornerRadius: 20))
    }
}

struct DistributionQuickActionStrip: View {
    let palette: HubPalette
    let onCode: (String) -> Void

    var body: some View {
        FlowLayout(spacing: 8) {
            ForEach(DistributionDealershipHub.quickActionStrip, id: \.0) { code, label in
                Button(label) { onCode(code) }
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundStyle(palette.text)
                    .padding(.horizontal, 14)
                    .padding(.vertical, 10)
                    .background(palette.card)
                    .overlay(RoundedRectangle(cornerRadius: 20).stroke(palette.border, lineWidth: 1))
                    .clipShape(RoundedRectangle(cornerRadius: 20))
            }
        }
    }
}
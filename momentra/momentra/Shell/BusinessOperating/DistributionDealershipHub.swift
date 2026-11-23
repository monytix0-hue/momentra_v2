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

struct DistributionMomentsHero: View {
    let projection: OperatingProjectionPayload?
    let palette: HubPalette

    var body: some View {
        let routes = projection?.life?.channels.filter { !$0.isEmpty }.joined(separator: " · ")
            ?? {
                let detail = projection?.boards?.moments?.first?.detail ?? ""
                return detail.isEmpty ? nil : detail
            }()
        let people = projection?.participants?.count ?? 0
        FlowLayout(spacing: 8) {
            DistributionStatusChip(
                routes != nil ? "Routes live" : "Routes",
                palette: palette,
                emphasized: routes != nil
            )
            DistributionStatusChip(
                people > 0 ? "People · \(people)" : "People · Dealers",
                palette: palette
            )
        }
    }
}

struct DistributionLifeHeader: View {
    let palette: HubPalette

    var body: some View {
        Text("Today")
            .font(.system(size: 16, weight: .bold))
            .foregroundStyle(palette.text)
    }
}

struct DistributionPaymentChips: View {
    let projection: OperatingProjectionPayload?
    let palette: HubPalette

    var body: some View {
        let chips = (projection?.paymentReadiness ?? []).compactMap { row -> String? in
            switch row.dueBucket?.uppercased() {
            case "OVERDUE": return "Overdue · \(row.title)"
            case "DUE_SOON": return "Due soon · \(row.title)"
            default: return nil
            }
        }
        if chips.isEmpty { EmptyView() }
        else {
            FlowLayout(spacing: 8) {
                ForEach(chips, id: \.self) { chip in
                    DistributionStatusChip(chip, palette: palette)
                }
            }
        }
    }
}

struct DistributionMemoryHero: View {
    let projection: OperatingProjectionPayload?
    let palette: HubPalette

    var body: some View {
        FlowLayout(spacing: 8) {
            DistributionStatusChip("Closed period", palette: palette, emphasized: true)
            let revisions = projection?.memory?.revisions ?? []
            if !revisions.isEmpty {
                DistributionStatusChip("\(revisions.count) revisions", palette: palette)
            }
        }
    }
}

struct DistributionStatusChip: View {
    let label: String
    let palette: HubPalette
    var emphasized: Bool = false

    init(_ label: String, palette: HubPalette, emphasized: Bool = false) {
        self.label = label
        self.palette = palette
        self.emphasized = emphasized
    }

    var body: some View {
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

struct DistributionGenerateStoryButton: View {
    let palette: HubPalette
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text("Generate Business Story")
                .font(.system(size: 15, weight: .semibold))
                .foregroundStyle(palette.onAccent)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 14)
                .background(palette.accent)
                .clipShape(RoundedRectangle(cornerRadius: 12))
        }
    }
}

struct DistributionQuickAddHub: View {
    let adds: [OperatingQuickAddPayload]
    let palette: HubPalette
    @Binding var selectedCode: String
    @Binding var search: String
    @Binding var titleText: String
    @Binding var amountText: String
    var onSubmit: () -> Void

    private var filtered: [OperatingQuickAddPayload] {
        let q = search.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !q.isEmpty else { return adds }
        return adds.filter {
            $0.label.localizedCaseInsensitiveContains(q) || $0.code.localizedCaseInsensitiveContains(q)
        }
    }

    private var groups: [(String, [OperatingQuickAddPayload])] {
        let core = filtered.filter { DistributionDealershipHub.quickAddCore.contains($0.code.uppercased()) }
        let support = filtered.filter { DistributionDealershipHub.quickAddSupport.contains($0.code.uppercased()) }
        let utilities = filtered.filter {
            let c = $0.code.uppercased()
            return !DistributionDealershipHub.quickAddCore.contains(c) && !DistributionDealershipHub.quickAddSupport.contains(c)
        }
        return [
            ("Core", core),
            ("Support", support),
            ("Utilities", utilities),
        ].filter { !$1.isEmpty }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Source. Move. Settle.")
                .font(.system(size: 14, weight: .semibold))
                .foregroundStyle(palette.accent)
            TextField("Search quick adds", text: $search)
                .textFieldStyle(.plain)
                .padding(12)
                .background(palette.card)
                .overlay(RoundedRectangle(cornerRadius: 12).stroke(palette.border, lineWidth: 1))
                .clipShape(RoundedRectangle(cornerRadius: 12))
                .foregroundStyle(palette.text)
            HStack(spacing: 8) {
                ForEach([("SHIPMENT", "Dispatch"), ("SALE", "Record Sale")], id: \.0) { code, label in
                    if adds.contains(where: { $0.code.caseInsensitiveCompare(code) == .orderedSame }) {
                        Button(label) { selectedCode = code }
                            .font(.system(size: 12, weight: .semibold))
                            .foregroundStyle(palette.onAccent)
                            .padding(.horizontal, 14)
                            .padding(.vertical, 10)
                            .background(palette.accent)
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                    }
                }
            }
            ForEach(groups, id: \.0) { heading, items in
                Text(heading)
                    .font(.system(size: 15, weight: .medium))
                    .foregroundStyle(palette.text)
                FlowLayout(spacing: 10) {
                    ForEach(items) { add in
                        let on = add.code == selectedCode
                        Button {
                            selectedCode = add.code
                        } label: {
                            Text(add.label)
                                .font(.system(size: 12, weight: .bold))
                                .foregroundStyle(on ? palette.accent : palette.text)
                                .padding(.horizontal, 14)
                                .padding(.vertical, 14)
                                .background(on ? palette.accent.opacity(0.18) : palette.card)
                                .overlay(
                                    RoundedRectangle(cornerRadius: 16)
                                        .stroke(on ? palette.accent : palette.border, lineWidth: 1)
                                )
                                .clipShape(RoundedRectangle(cornerRadius: 16))
                        }
                    }
                }
            }
            Text("Every post becomes evidence on Pulse — nothing is invented until you add it.")
                .font(.system(size: 11))
                .italic()
                .foregroundStyle(palette.muted)
            TextField("Title", text: $titleText)
                .textFieldStyle(.plain)
                .padding(12)
                .background(palette.card)
                .clipShape(RoundedRectangle(cornerRadius: 12))
                .foregroundStyle(palette.text)
            if adds.first(where: { $0.code == selectedCode })?.needsAmount == true {
                TextField("Amount", text: $amountText)
                    .textFieldStyle(.plain)
                    .padding(12)
                    .background(palette.card)
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                    .foregroundStyle(palette.text)
            }
            Button("Add", action: onSubmit)
                .disabled(selectedCode.isEmpty || titleText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                .font(.system(size: 15, weight: .semibold))
                .foregroundStyle(palette.onAccent)
                .padding(.horizontal, 16)
                .padding(.vertical, 12)
                .background(palette.accent)
                .clipShape(RoundedRectangle(cornerRadius: 12))
        }
    }
}
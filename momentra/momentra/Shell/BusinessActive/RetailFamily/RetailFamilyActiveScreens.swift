import SwiftUI

// MARK: - Moments

/// Android `RetailMoments` / `RestaurantMoments` / `ServiceMoments` card IA.
/// Open Commitments → My Business → People → Network (not trading board slots).
struct RetailFamilyMomentsScreen: View {
    @ObservedObject var model: BusinessOperatingModel
    let theme: OperatingTheme
    var onCapture: (String) -> Void
    var onViewAll: (String) -> Void
    var onOpenRow: ((OperatingActivityPayload) -> Void)? = nil

    private var projection: OperatingProjectionPayload? { model.projection }
    private var commitments: OperatingBoardSectionPayload? { model.section(code: "M4") }

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            OperatingV12Hero(
                theme: theme,
                eyebrow: "ACTIVE MOMENT",
                title: model.title,
                chips: momentsChips,
                kpis: momentsHeroKpis
            )
            .padding(.horizontal, 16)

            OperatingV12QuickActions(theme: theme, onOpen: onCapture)
                .padding(.horizontal, 20)

            VStack(alignment: .leading, spacing: 16) {
                openCommitmentsCard
                myBusinessCard
                peopleCard
                networkCard
                recordNextCard
            }
            .padding(.horizontal, 16)
        }
        .padding(.top, 8)
        .padding(.bottom, 24)
    }

    private var momentsChips: [OperatingV12ChipSpec] {
        var chips: [OperatingV12ChipSpec] = [
            .init(label: projection?.subtypeLabel ?? model.vertical.label, tone: theme.accent),
        ]
        if let location = projection?.location, !location.isEmpty {
            chips.append(.init(label: location, tone: OperatingV12.green, dot: true))
        }
        return chips
    }

    private var momentsHeroKpis: [OperatingV12KpiSpec] {
        let staff = projection?.participants?.count ?? 0
        let suppliers = networkSuppliers.count
        return [
            .init(label: "People · Staff", value: "\(staff)", tone: OperatingV12.title),
            .init(label: "Network · Suppliers", value: "\(suppliers)", tone: OperatingV12.title),
            .init(
                label: "AP open",
                value: money(projection?.finance?.payable),
                tone: OperatingV12.amber
            ),
        ]
    }

    private var openCommitmentsCard: some View {
        let rows = Array((commitments?.rows ?? []).prefix(3))
        let openOrders = activity.filter { row in
            let type = row.eventType.uppercased()
            guard type == "ORDER" || type == "CUSTOMER_ORDER" else { return false }
            let status = (row.status ?? "").uppercased()
            return status != "COMPLETED" && status != "CANCELLED"
        }.count
        return boardCard {
            OperatingV12WidgetHeader(title: "Open Commitments", onViewAll: { onViewAll("M4") })
            kpiRow([
                ("AP", money(projection?.finance?.payable), OperatingV12.amber),
                (arLabel, money(projection?.finance?.outstanding), theme.accent),
                ("Orders", "\(openOrders)", OperatingV12.title),
            ])
            if rows.isEmpty {
                emptyBlock("Nothing open", commitmentsEmpty)
            } else {
                rowList(rows)
            }
            OperatingV12Why(text: commitmentsWhy)
        }
    }

    private var myBusinessCard: some View {
        let retired = projection?.retired == true
        let channel = projection?.life?.channels.first ?? "Unavailable"
        let targetConfigured = projection?.target?.configured == true
        return boardCard {
            OperatingV12WidgetHeader(title: myBusinessTitle, onViewAll: { onViewAll("ACTIVITY") })
            kpiRow(myBusinessKpis(retired: retired, channel: channel))
            detailLine(
                eyebrow: "Business",
                value: businessLine,
                trailing: retired ? closedLabel : openLabel,
                trailingTone: retired ? OperatingV12.subtitle : OperatingV12.green
            )
            detailLine(
                eyebrow: "Plan",
                value: targetConfigured
                    ? "Sales \(money(projection?.target?.value)) target"
                    : "No target set",
                trailing: targetConfigured ? "Active" : "Unavailable",
                trailingTone: targetConfigured ? OperatingV12.title : OperatingV12.amber
            )
            detailLine(
                eyebrow: "GST",
                value: gstValue,
                trailing: gstOn ? "On" : "Off",
                trailingTone: OperatingV12.amber
            )
            OperatingV12Why(text: myBusinessWhy)
        }
    }

    private var peopleCard: some View {
        let staff = projection?.participants ?? []
        return boardCard {
            OperatingV12WidgetHeader(title: "People", onViewAll: { onViewAll("M2") })
            kpiRow(peopleKpis(staffCount: staff.count))
            if staff.isEmpty {
                emptyBlock("No people yet", peopleEmpty)
            } else {
                ForEach(Array(staff.prefix(3)), id: \.self) { name in
                    let isOwner = name == projection?.partyName
                    detailLine(
                        eyebrow: name,
                        value: isOwner ? roleLabel(projection?.role) : "Member",
                        trailing: isOwner ? "Owner" : "Member",
                        trailingTone: isOwner ? OperatingV12.title : OperatingV12.rowText
                    )
                }
            }
            OperatingV12Why(text: peopleWhy)
        }
    }

    private var networkCard: some View {
        let suppliers = networkSuppliers
        let buyers = networkBuyers
        let combined: [(name: String, supplier: Bool)] = {
            var seen = Set<String>()
            var out: [(name: String, supplier: Bool)] = []
            for master in suppliers + buyers {
                if seen.insert(master.name).inserted {
                    out.append((
                        name: master.name,
                        supplier: suppliers.contains(where: { $0.id == master.id })
                    ))
                }
            }
            return out
        }()
        let payables = (projection?.paymentReadiness ?? []).filter { $0.relevance.uppercased() != "RECEIVABLE" }
        let receivables = (projection?.paymentReadiness ?? []).filter { $0.relevance.uppercased() == "RECEIVABLE" }
        return boardCard {
            OperatingV12WidgetHeader(title: networkTitle, onViewAll: { onViewAll(networkSheetCode) })
            kpiRow([
                ("Suppliers", "\(suppliers.count)", OperatingV12.title),
                (networkBuyerLabel, "\(buyers.count)", OperatingV12.title),
            ])
            if combined.isEmpty {
                emptyBlock("No network yet", networkEmpty)
            } else {
                ForEach(Array(combined.prefix(3).enumerated()), id: \.offset) { _, entry in
                    let openAp = payables
                        .filter { $0.counterpartyName?.caseInsensitiveCompare(entry.name) == .orderedSame }
                        .compactMap { OperatingV12Money.number($0.amount) }
                        .reduce(0, +)
                    let openAr = receivables
                        .filter { $0.counterpartyName?.caseInsensitiveCompare(entry.name) == .orderedSame }
                        .compactMap { OperatingV12Money.number($0.amount) }
                        .reduce(0, +)
                    let trailingTone: (String, Color) = {
                        if entry.supplier, openAp > 0 {
                            return (
                                "AP \(OperatingV12Money.compact(openAp, currency: model.currency))",
                                OperatingV12.amber
                            )
                        }
                        if !entry.supplier, openAr > 0 {
                            return (
                                "AR \(OperatingV12Money.compact(openAr, currency: model.currency))",
                                theme.accent
                            )
                        }
                        return ("No balance", OperatingV12.subtitle)
                    }()
                    detailLine(
                        eyebrow: entry.name,
                        value: entry.supplier ? "Supplier" : networkBuyerRole,
                        trailing: trailingTone.0,
                        trailingTone: trailingTone.1
                    )
                }
            }
            OperatingV12Why(text: networkWhy)
        }
    }

    private var recordNextCard: some View {
        Button {
            onCapture(primaryCapture)
        } label: {
            HStack(spacing: 10) {
                Image(systemName: primaryGlyph)
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundStyle(theme.accent)
                    .frame(width: 36, height: 36)
                    .background(theme.accent.opacity(0.14))
                    .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                VStack(alignment: .leading, spacing: 2) {
                    Text(recordNextTitle)
                        .font(.plusJakarta(size: 14, weight: .bold))
                        .foregroundStyle(OperatingV12.title)
                        .multilineTextAlignment(.leading)
                    Text(recordNextSubtitle)
                        .font(.plusJakarta(size: 12, weight: .medium))
                        .foregroundStyle(OperatingV12.subtitle)
                        .multilineTextAlignment(.leading)
                }
                Spacer(minLength: 0)
                Text("›")
                    .font(.plusJakarta(size: 18, weight: .bold))
                    .foregroundStyle(theme.accent)
            }
            .padding(16)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(OperatingV12.surface)
            .overlay(
                RoundedRectangle(cornerRadius: 20, style: .continuous)
                    .strokeBorder(theme.accent.opacity(0.28), lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
        }
        .buttonStyle(.plain)
    }

    // MARK: Moments copy

    private var myBusinessTitle: String {
        switch model.vertical {
        case .restaurantFood: return "My Restaurant"
        case .consumerService: return "My Service Business"
        default: return "My Retail Business"
        }
    }

    private var arLabel: String {
        model.vertical == .restaurantFood ? "Platform AR" : "AR"
    }

    private var commitmentsEmpty: String {
        switch model.vertical {
        case .restaurantFood: return "Kitchen tickets, payables and platform receivables will show here."
        default: return "Orders, payables and receivables will show here."
        }
    }

    private var commitmentsWhy: String {
        switch model.vertical {
        case .restaurantFood: return "Catering order ≠ revenue until billed."
        default: return "Orders are commitments — revenue only on posted sale."
        }
    }

    private var myBusinessWhy: String {
        switch model.vertical {
        case .restaurantFood: return "Order→prep→bill→collect. Posted bill owns revenue."
        default: return "One Moment holds catalogue, stock, sales and money. Quick amount never invents SKU movement."
        }
    }

    private var peopleEmpty: String {
        switch model.vertical {
        case .restaurantFood: return "Invite the kitchen, floor and billing team."
        default: return "Invite the shop floor, billing and approvers."
        }
    }

    private var peopleWhy: String {
        switch model.vertical {
        case .restaurantFood: return "Roles gate voids, comps and platform settlements."
        default: return "Roles gate credit sales, stock adjustments and payments."
        }
    }

    private var networkTitle: String {
        model.vertical == .consumerService ? "Clients & Services" : "Business Network"
    }

    private var networkSheetCode: String {
        model.vertical == .consumerService ? "customers" : "channels"
    }

    private var networkBuyerLabel: String {
        model.vertical == .restaurantFood ? "Platforms" : "Credit buyers"
    }

    private var networkBuyerRole: String {
        model.vertical == .restaurantFood ? "Platform / buyer" : "Credit buyer"
    }

    private var networkEmpty: String {
        switch model.vertical {
        case .restaurantFood: return "Suppliers, platforms and credit parties you record will show here."
        default: return "Suppliers and credit buyers you record will show here."
        }
    }

    private var networkWhy: String {
        switch model.vertical {
        case .restaurantFood: return "Platform debt only on explicit allocation."
        default: return "Named customer required for unpaid/credit charge."
        }
    }

    private var openLabel: String {
        model.vertical == .restaurantFood ? "Open" : "Live"
    }

    private var closedLabel: String {
        model.vertical == .restaurantFood ? "Closed" : "Closed"
    }

    private var recordNextTitle: String {
        switch model.vertical {
        case .restaurantFood: return "Record the next restaurant moment"
        case .consumerService: return "Record the next service moment"
        default: return "Record the next store moment"
        }
    }

    private var recordNextSubtitle: String {
        switch model.vertical {
        case .restaurantFood: return "Bill, kitchen ticket, purchase or stock count."
        case .consumerService: return "Charge, booking, collection or adjustment."
        default: return "Sale, purchase, collection or adjustment."
        }
    }

    private var primaryCapture: String {
        switch model.vertical {
        case .restaurantFood: return "POS"
        case .consumerService: return "SERVICE"
        default: return "SALE"
        }
    }

    private var primaryGlyph: String {
        switch model.vertical {
        case .restaurantFood: return "fork.knife"
        case .consumerService: return "checkmark.seal"
        default: return "bag"
        }
    }

    private func myBusinessKpis(retired: Bool, channel: String) -> [(String, String, Color)] {
        switch model.vertical {
        case .restaurantFood:
            return [
                ("Status", retired ? "Closed" : "Open", retired ? OperatingV12.subtitle : OperatingV12.green),
                ("Day", dayOfPeriod, OperatingV12.title),
                ("GST", gstOn ? "On" : "Off", OperatingV12.amber),
            ]
        default:
            return [
                ("Status", retired ? "Closed" : "Live", retired ? OperatingV12.subtitle : OperatingV12.green),
                ("Channel", channel, OperatingV12.title),
                ("Day", dayOfPeriod, OperatingV12.title),
            ]
        }
    }

    private func peopleKpis(staffCount: Int) -> [(String, String, Color)] {
        switch model.vertical {
        case .restaurantFood:
            let stations = (projection?.masters ?? model.masters)
                .filter { $0.kind.uppercased() == "STATION" }
                .count
            return [
                ("Staff", "\(staffCount)", OperatingV12.title),
                (
                    "Stations",
                    stations > 0 ? "\(stations)" : "Unavailable",
                    stations > 0 ? OperatingV12.title : OperatingV12.amber
                ),
            ]
        default:
            let role = (projection?.role ?? "").uppercased()
            let approver = role == "OWNER_ADMIN" || role == "APPROVER"
            return [
                ("Staff", "\(staffCount)", OperatingV12.title),
                ("Approvers", approver ? "1" : "Unavailable", approver ? OperatingV12.amber : OperatingV12.amber),
            ]
        }
    }

    private var projectionMasters: [OperatingMasterPayload] {
        projection?.masters ?? model.masters
    }

    private var networkSuppliers: [OperatingMasterPayload] {
        let kinds: Set<String>
        switch model.vertical {
        case .restaurantFood: kinds = ["SUPPLIER", "VENDOR", "PRINCIPAL"]
        default: kinds = ["SUPPLIER"]
        }
        return projectionMasters.filter { kinds.contains($0.kind.uppercased()) }
    }

    private var networkBuyers: [OperatingMasterPayload] {
        let kinds: Set<String>
        switch model.vertical {
        case .restaurantFood: kinds = ["CHANNEL", "PLATFORM", "AGGREGATOR", "CUSTOMER", "BUYER"]
        default: kinds = ["CUSTOMER", "BUYER"]
        }
        return projectionMasters.filter { kinds.contains($0.kind.uppercased()) }
    }
}

// MARK: - Life

/// Android Life cards: Current Business/Service → Sales & Orders / Bookings → Money Moving → Ops.
struct RetailFamilyLifeScreen: View {
    @ObservedObject var model: BusinessOperatingModel
    let theme: OperatingTheme
    var onCapture: (String) -> Void
    var onViewAll: (String) -> Void
    var onOpenRow: ((OperatingActivityPayload) -> Void)? = nil

    private var projection: OperatingProjectionPayload? { model.projection }
    private var life: [OperatingBoardSectionPayload] { projection?.boards?.life ?? [] }

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            OperatingV12QuickActions(theme: theme, onOpen: onCapture)
                .padding(.horizontal, 20)

            OperatingV12Hero(
                theme: theme,
                eyebrow: "TODAY",
                title: "\(model.title) · today",
                titleSize: 22,
                chips: [
                    .init(label: "Today · \(OperatingV12Date.today())", tone: theme.accent),
                    .init(
                        label: lifeOpenBadge,
                        tone: projection?.retired == true ? OperatingV12.subtitle : OperatingV12.green,
                        dot: true
                    ),
                ],
                kpis: [
                    .init(label: "Open", value: "\(openCount)", tone: theme.accent),
                    .init(label: "Done", value: "\(doneCount)", tone: OperatingV12.green),
                    .init(label: "Money in motion", value: money(projection?.finance?.received), tone: OperatingV12.title),
                ]
            )
            .padding(.horizontal, 16)

            VStack(alignment: .leading, spacing: 16) {
                currentCard
                salesCard
                moneyCard
                opsCard
            }
            .padding(.horizontal, 16)
        }
        .padding(.top, 8)
        .padding(.bottom, 24)
    }

    private var currentCard: some View {
        let section = life.first { $0.code == "L1" } ?? life.first { $0.code == "L2" }
        let rows = Array((section?.rows ?? []).prefix(3))
        return boardCard {
            OperatingV12WidgetHeader(title: currentTitle, onViewAll: { onViewAll("L1") })
            kpiRow(currentKpis)
            if rows.isEmpty {
                emptyBlock("Nothing recorded yet", section?.detail.nilIfBlank ?? currentEmpty)
            } else {
                rowList(rows, onOpenRow: onOpenRow)
            }
            if let coverage = section?.coverage?.nilIfBlank {
                Text(coverage)
                    .font(.plusJakarta(size: 11, weight: .semibold))
                    .foregroundStyle(OperatingV12.amber)
            }
            OperatingV12Why(text: currentWhy)
        }
    }

    private var salesCard: some View {
        let section = life.first { $0.code == "L2" }
        let rows = Array((section?.rows ?? []).prefix(3))
        return boardCard {
            OperatingV12WidgetHeader(title: salesTitle, onViewAll: { onViewAll("L2") })
            kpiRow(salesKpis)
            if !rows.isEmpty {
                ForEach(rows) { row in
                    detailLine(
                        eyebrow: row.channelName?.nilIfBlank ?? (row.title.nilIfBlank ?? "Channel"),
                        value: row.title.nilIfBlank ?? "Recorded",
                        trailing: model.financeVisible
                            ? OperatingV12Money.amount(row.amount, currency: model.currency)
                            : "—",
                        trailingTone: OperatingV12.title
                    )
                }
            } else if let detail = section?.detail.nilIfBlank {
                emptyBlock(model.vertical == .restaurantFood ? "Channel mix" : "Counter mix", detail)
            } else {
                emptyBlock("Nothing recorded yet", salesEmpty)
            }
            OperatingV12Why(text: "Order value stays separate from posted sale.")
        }
    }

    private var moneyCard: some View {
        let section = life.first { $0.code == "L3" }
        let rows = Array((section?.rows ?? []).prefix(3))
        return boardCard(borderOpacity: 0.45) {
            OperatingV12WidgetHeader(title: "Money Moving", onViewAll: { onViewAll("L3") })
            kpiRow([
                ("Collected", money(projection?.finance?.received), OperatingV12.green),
                ("Paid", money(projection?.finance?.cashPaid), OperatingV12.title),
                ("AR move", money(projection?.finance?.outstanding), theme.accent),
            ])
            if rows.isEmpty {
                emptyBlock("Nothing moved yet", section?.detail.nilIfBlank ?? moneyEmpty)
            } else {
                rowList(rows, trailingOverride: { row in
                    let status = (row.paymentStatus ?? "").uppercased()
                    return ["COLLECTED", "PAID", "SETTLED"].contains(status) ? "Done" : "Open"
                }, onOpenRow: onOpenRow)
            }
            OperatingV12Why(text: moneyWhy)
        }
    }

    private var opsCard: some View {
        let section = life.first { $0.code == "L4" }
        let rows = Array((section?.rows ?? []).prefix(3))
        return boardCard {
            OperatingV12WidgetHeader(title: opsTitle, onViewAll: { onViewAll("L4") })
            kpiRow(opsKpis)
            if rows.isEmpty {
                emptyBlock("Nothing moving", section?.detail.nilIfBlank ?? opsEmpty)
            } else {
                rowList(rows, trailingOverride: { _ in "Open" }, onOpenRow: onOpenRow)
            }
            OperatingV12Why(text: opsWhy)
        }
    }

    private var openCount: Int {
        if model.vertical == .restaurantFood {
            return openOrders.count
        }
        return projection?.life?.openCommitments ?? 0
    }

    private var doneCount: Int {
        activity.filter {
            ["COLLECTED", "PAID", "SETTLED"].contains(($0.paymentStatus ?? "").uppercased())
        }.count
    }

    private var openOrders: [OperatingActivityPayload] {
        activity.filter { row in
            let type = row.eventType.uppercased()
            guard type == "ORDER" || type == "CUSTOMER_ORDER" else { return false }
            let status = (row.status ?? "").uppercased()
            return status != "COMPLETED" && status != "CANCELLED"
        }
    }

    private var lifeOpenBadge: String {
        let retired = projection?.retired == true
        switch model.vertical {
        case .restaurantFood: return retired ? "Restaurant closed" : "Service live"
        default: return retired ? "Store closed" : "Store open"
        }
    }

    private var currentTitle: String {
        model.vertical == .restaurantFood ? "Current Service" : "Current Business"
    }

    private var currentEmpty: String {
        switch model.vertical {
        case .restaurantFood: return "Today's kitchen tickets and posted bills will show here."
        default: return "Today's posted sales and stock moves will show here."
        }
    }

    private var currentWhy: String {
        switch model.vertical {
        case .restaurantFood: return "Today tracks tickets and posted bills — not forecasts."
        default: return "Today shows posted sales and explicit stock moves only."
        }
    }

    private var currentKpis: [(String, String, Color)] {
        switch model.vertical {
        case .restaurantFood:
            let bills = activity.filter { $0.eventType.uppercased() == "SALE" }.count
            return [
                ("Open tickets", "\(openOrders.count)", theme.accent),
                ("Posted bills", "\(bills)", OperatingV12.green),
            ]
        default:
            let percent: String = {
                guard projection?.target?.configured == true else { return "Unavailable" }
                return targetPercent ?? "Unavailable"
            }()
            return [
                ("Sales today", money(projection?.finance?.sales), OperatingV12.green),
                ("Achv.", percent, percent == "Unavailable" ? OperatingV12.amber : OperatingV12.amber),
            ]
        }
    }

    private var salesTitle: String {
        model.vertical == .consumerService ? "Bookings & Services" : "Sales & Orders"
    }

    private var salesEmpty: String {
        switch model.vertical {
        case .restaurantFood: return "Dine-in, takeaway and platform bills will show here."
        default: return "Sales, credit and holds will show here."
        }
    }

    private var salesKpis: [(String, String, Color)] {
        let creditOpen = (projection?.paymentReadiness ?? []).filter {
            $0.relevance.uppercased() == "RECEIVABLE"
        }.count
        switch model.vertical {
        case .restaurantFood:
            let bills = activity.filter { $0.eventType.uppercased() == "SALE" }
            return [
                ("Bills", "\(bills.count)", OperatingV12.title),
                ("Avg ticket", averageTicket(bills), OperatingV12.title),
                ("Credit open", "\(creditOpen)", theme.accent),
            ]
        default:
            let tickets = activity.filter { $0.eventType.uppercased() == "SALE" }.count
            let holds = openOrders.count
            return [
                ("Tickets", "\(tickets)", OperatingV12.title),
                ("Credit open", "\(creditOpen)", theme.accent),
                ("Holds", "\(holds)", OperatingV12.amber),
            ]
        }
    }

    private var moneyEmpty: String {
        switch model.vertical {
        case .restaurantFood: return "Bill collections and supplier payments will show here."
        default: return "Collections and payments will show here."
        }
    }

    private var moneyWhy: String {
        switch model.vertical {
        case .restaurantFood: return "Settlement never reposts the bill."
        default: return "Collection/payment allocate — never repost sale or purchase."
        }
    }

    private var opsTitle: String {
        model.vertical == .consumerService ? "Rooms & Staff" : "Operations / Stock"
    }

    private var opsEmpty: String {
        switch model.vertical {
        case .restaurantFood: return "Ingredient receipts, counts and waste will show here."
        case .consumerService: return "Receipts, counts and damage will show here."
        default: return "Receipts, counts and damage will show here."
        }
    }

    private var opsWhy: String {
        "Stock += accepted receipt only; never infer from billing."
    }

    private var opsKpis: [(String, String, Color)] {
        let receipts = activity.filter { $0.eventType.uppercased() == "PURCHASE" }.count
        switch model.vertical {
        case .restaurantFood:
            let alerts = (projection?.attention ?? []).filter {
                $0.kind.uppercased().hasPrefix("LOW_STOCK")
            }.count
            return [
                ("Prep alerts", "\(alerts)", alerts > 0 ? theme.accent : OperatingV12.title),
                ("Receipts", "\(receipts)", OperatingV12.title),
            ]
        default:
            let adjustments = activity.filter { $0.eventType.uppercased() == "STOCK" }.count
            let alerts = projection?.attention?.count ?? 0
            return [
                ("Receipts", "\(receipts)", OperatingV12.title),
                ("Adjustments", "\(adjustments)", OperatingV12.title),
                ("Alerts", "\(alerts)", theme.accent),
            ]
        }
    }

    private var targetPercent: String? {
        guard let target = projection?.target, target.configured == true else { return nil }
        guard let progress = OperatingV12Money.number(target.progress),
              let value = OperatingV12Money.number(target.value),
              value > 0
        else { return nil }
        return "\(Int((progress / value * 100).rounded()))%"
    }

    private func averageTicket(_ bills: [OperatingActivityPayload]) -> String {
        let amounts = bills.compactMap { OperatingV12Money.number($0.amount) }
        guard !amounts.isEmpty, model.financeVisible else { return "Unavailable" }
        let avg = amounts.reduce(0, +) / Double(amounts.count)
        return OperatingV12Money.compact(avg, currency: model.currency)
    }

    private var activity: [OperatingActivityPayload] { projection?.activity ?? [] }

    private func money(_ raw: String?) -> String {
        model.financeVisible ? OperatingV12Money.amount(raw, currency: model.currency) : "—"
    }
}

// MARK: - Memory

/// Android Memory: Monthly → Money → People & Activity → Patterns & Decisions.
struct RetailFamilyMemoryScreen: View {
    @ObservedObject var model: BusinessOperatingModel
    let theme: OperatingTheme
    var onCapture: (String) -> Void
    var onStory: () -> Void
    var onViewAll: (String) -> Void
    var onOpenRow: ((OperatingActivityPayload) -> Void)? = nil

    private var projection: OperatingProjectionPayload? { model.projection }
    private var memory: [OperatingBoardSectionPayload] { projection?.boards?.memory ?? [] }
    private var period: String {
        OperatingV12Date.monthYear(projection?.startDate) ?? "This month"
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            OperatingV12QuickActions(theme: theme, onOpen: onCapture)
                .padding(.horizontal, 20)

            OperatingV12Hero(
                theme: theme,
                eyebrow: "MEMORY · THIS MONTH",
                title: "\(model.title) · memory",
                titleSize: 22,
                chips: memoryChips,
                kpis: [
                    .init(label: "Sales", value: money(projection?.finance?.sales), tone: .white),
                    .init(
                        label: "vs last period",
                        value: lastPeriodDelta,
                        tone: lastPeriodDelta == "Unavailable" ? OperatingV12.amber : OperatingV12.amber
                    ),
                    .init(
                        label: "Supplier paid",
                        value: money(projection?.finance?.cashPaid),
                        tone: theme.accent
                    ),
                ]
            )
            .padding(.horizontal, 16)

            VStack(alignment: .leading, spacing: 16) {
                monthlyCard
                if memory.contains(where: { $0.code == "ME2" }) { moneyMemoryCard }
                if memory.contains(where: { $0.code == "ME3" }) { peopleMemoryCard }
                if memory.contains(where: { $0.code == "ME4" }) { patternsCard }
                storyCTA
            }
            .padding(.horizontal, 16)
        }
        .padding(.top, 8)
        .padding(.bottom, 24)
    }

    private var memoryChips: [OperatingV12ChipSpec] {
        var chips: [OperatingV12ChipSpec] = [
            .init(label: "\(period) · memory", tone: theme.accent),
        ]
        let revisions = projection?.memory?.revisions.count ?? 0
        if revisions > 0 {
            chips.append(.init(label: "\(revisions) revisions", tone: OperatingV12.subtitle))
        }
        return chips
    }

    private var monthlyCard: some View {
        let section = memory.first { $0.code == "ME1" }
        let months = (section?.rows ?? []).filter { row in
            let type = row.eventType.uppercased()
            return type.isEmpty || type == "MONTH" || type == "REVISION"
        }
        return boardCard {
            OperatingV12WidgetHeader(title: "Monthly Memory", onViewAll: { onViewAll("ME1") })
            kpiRow([
                ("Months", "\(months.count)", OperatingV12.title),
                ("Avg achv.", averageAchievement(months), averageAchievement(months) == "Unavailable" ? OperatingV12.amber : OperatingV12.amber),
            ])
            if months.isEmpty {
                emptyBlock("No history yet", section?.detail.nilIfBlank ?? "Closed months will show here.")
            } else {
                ForEach(Array(months.prefix(3))) { row in
                    let parts = row.title.split(separator: "·").map { $0.trimmingCharacters(in: .whitespaces) }
                    detailLine(
                        eyebrow: parts.first?.nilIfBlank
                            ?? OperatingV12Date.monthYear(row.occurredAt)
                            ?? period,
                        value: parts.dropFirst().joined(separator: " · ").nilIfBlank
                            ?? (model.financeVisible
                                ? OperatingV12Money.amount(row.amount, currency: model.currency)
                                : "—"),
                        trailing: "Recorded",
                        trailingTone: OperatingV12.rowText
                    )
                }
            }
            if let coverage = section?.coverage?.nilIfBlank {
                Text(coverage)
                    .font(.plusJakarta(size: 11, weight: .semibold))
                    .foregroundStyle(OperatingV12.amber)
            }
            OperatingV12Why(text: "Missing coverage stays unavailable — never averaged as zero.")
        }
    }

    private var moneyMemoryCard: some View {
        let section = memory.first { $0.code == "ME2" }
        let rows = Array((section?.rows ?? []).prefix(3))
        let revisions = projection?.memory?.revisions ?? []
        return boardCard(borderOpacity: 0.50) {
            OperatingV12WidgetHeader(title: "Money Memory", onViewAll: { onViewAll("ME2") })
            kpiRow([
                ("Sales", money(projection?.finance?.sales), OperatingV12.title),
                ("Collected", money(projection?.finance?.received), OperatingV12.green),
                ("Paid", money(projection?.finance?.cashPaid), theme.accent),
            ])
            if let revision = revisions.first {
                detailLine(
                    eyebrow: "Plan revision",
                    value: revision.title.nilIfBlank ?? "Target revision",
                    trailing: OperatingV12Date.short(revision.occurredAt) ?? "Recorded",
                    trailingTone: OperatingV12.amber
                )
            }
            if rows.isEmpty {
                emptyBlock("Nothing settled yet", section?.detail.nilIfBlank ?? "Collections and payments will show here.")
            } else {
                ForEach(rows) { row in
                    detailLine(
                        eyebrow: row.title.nilIfBlank ?? eventLabel(row.eventType),
                        value: row.counterpartyName?.nilIfBlank ?? eventLabel(row.eventType),
                        trailing: model.financeVisible
                            ? OperatingV12Money.amount(row.amount, currency: model.currency)
                            : "—",
                        trailingTone: OperatingV12.title
                    )
                }
            }
            OperatingV12Why(text: "Plan revisions versioned; memory never overwrites prior targets.")
        }
    }

    private var peopleMemoryCard: some View {
        let section = memory.first { $0.code == "ME3" }
        let rows = Array((section?.rows ?? []).prefix(3))
        let shifts = activity.filter { $0.eventType.uppercased() == "SHIFT" }.count
        return boardCard {
            OperatingV12WidgetHeader(
                title: "People & Activity",
                subtitle: model.vertical == .restaurantFood ? "Shifts" : nil,
                onViewAll: { onViewAll("ME3") }
            )
            kpiRow([
                ("People", "\(projection?.participants?.count ?? 0)", OperatingV12.title),
                (
                    "Shifts",
                    shifts > 0 ? "\(shifts)" : "Unavailable",
                    shifts > 0 ? OperatingV12.title : OperatingV12.amber
                ),
            ])
            if rows.isEmpty {
                emptyBlock("Nothing recorded yet", section?.detail.nilIfBlank ?? "Who did what will show here.")
            } else {
                rowList(rows, trailingOverride: { row in
                    (row.approvalStatus ?? "").uppercased() == "PENDING" ? "Open" : "Settled"
                }, onOpenRow: onOpenRow)
            }
        }
    }

    private var patternsCard: some View {
        let section = memory.first { $0.code == "ME4" }
        let rows = Array((section?.rows ?? []).prefix(3))
        let decisions = activity.filter { $0.eventType.uppercased() == "APPROVAL" }.count
        let approved = activity.filter {
            $0.eventType.uppercased() == "APPROVAL" && ($0.approvalStatus ?? "").uppercased() == "APPROVED"
        }.count
        return boardCard {
            OperatingV12WidgetHeader(title: "Patterns & Decisions", onViewAll: { onViewAll("ME4") })
            kpiRow([
                ("Decisions", "\(decisions)", OperatingV12.title),
                ("Approved", "\(approved)", OperatingV12.green),
            ])
            if rows.isEmpty {
                emptyBlock("No patterns yet", section?.detail.nilIfBlank ?? "Approvals and repeated issues will show here.")
            } else {
                ForEach(rows) { row in
                    let status = (row.approvalStatus ?? "").uppercased()
                    let trailingBar: (String, Color) = {
                        switch status {
                        case "APPROVED": return ("Approved", OperatingV12.green)
                        case "REJECTED": return ("Denied", OperatingV12.red)
                        default: return ("Pending", OperatingV12.amber)
                        }
                    }()
                    Button {
                        onOpenRow?(row)
                    } label: {
                        OperatingV12Row(
                            title: row.title,
                            subtitle: row.subtitle,
                            trailing: trailingBar.0,
                            chip: row.badge,
                            bar: trailingBar.1,
                            accent: theme.accent
                        )
                    }
                    .buttonStyle(.plain)
                    .disabled(onOpenRow == nil)
                }
            }
        }
    }

    private var storyCTA: some View {
        Button(action: onStory) {
            VStack(alignment: .leading, spacing: 2) {
                Image(systemName: "sparkles")
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(OperatingV12.deepInk)
                Text("Generate Business Story")
                    .font(.plusJakarta(size: 15, weight: .heavy))
                    .foregroundStyle(OperatingV12.deepInk)
                Text("Eight pages from recorded activity · \(period) preselected")
                    .font(.plusJakarta(size: 11, weight: .medium))
                    .foregroundStyle(OperatingV12.deepInk.opacity(0.8))
            }
            .padding(.horizontal, 18)
            .padding(.vertical, 14)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(theme.accent)
            .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
            .shadow(color: theme.accent.opacity(0.35), radius: 12, x: 0, y: 8)
        }
        .buttonStyle(.plain)
    }

    private var lastPeriodDelta: String {
        let monthly = memory.first { $0.code == "ME1" }?.rows ?? []
        guard monthly.count >= 2,
              let current = OperatingV12Money.number(monthly[0].amount),
              let previous = OperatingV12Money.number(monthly[1].amount),
              previous > 0
        else { return "Unavailable" }
        let delta = ((current - previous) / previous * 100).rounded()
        let sign = delta < 0 ? "−" : "+"
        return "\(sign)\(Int(abs(delta)))%"
    }

    private func averageAchievement(_ rows: [OperatingActivityPayload]) -> String {
        let values = rows.compactMap { row -> Int? in
            guard let match = row.title.range(of: #"(\d+)%"#, options: .regularExpression) else {
                return nil
            }
            let token = String(row.title[match]).replacingOccurrences(of: "%", with: "")
            return Int(token)
        }
        guard !values.isEmpty else { return "Unavailable" }
        return "\(values.reduce(0, +) / values.count)%"
    }

    private var activity: [OperatingActivityPayload] { projection?.activity ?? [] }

    private func money(_ raw: String?) -> String {
        model.financeVisible ? OperatingV12Money.amount(raw, currency: model.currency) : "—"
    }

    private func eventLabel(_ type: String) -> String {
        switch type.uppercased() {
        case "SALE": return "Sale"
        case "PURCHASE": return "Purchase"
        case "COLLECTION": return "Collection"
        case "PAYMENT": return "Payment"
        case "APPROVAL": return "Decision"
        default: return type.isEmpty ? "Record" : type.capitalized
        }
    }
}

// MARK: - Shared chrome helpers

private extension RetailFamilyMomentsScreen {
    var activity: [OperatingActivityPayload] { projection?.activity ?? [] }
    var gstOn: Bool { (projection?.gstTracking ?? "").uppercased() == "YES" }
    var businessLine: String {
        [projection?.title, projection?.location?.nilIfBlank]
            .compactMap { $0?.nilIfBlank }
            .joined(separator: " · ")
            .nilIfBlank ?? "Unavailable"
    }
    var gstValue: String {
        if !gstOn { return "Not tracked" }
        if (projection?.gstRegistration ?? "").isEmpty { return "Registration incomplete" }
        if let state = projection?.gstState?.nilIfBlank {
            return "Registered · \(state)"
        }
        return "Registered"
    }
    var dayOfPeriod: String {
        RetailFamilyFormat.dayOfPeriod(startDate: projection?.startDate)
    }

    func money(_ raw: String?) -> String {
        model.financeVisible ? OperatingV12Money.amount(raw, currency: model.currency) : "—"
    }

    func roleLabel(_ code: String?) -> String {
        switch (code ?? "").uppercased() {
        case "OWNER_ADMIN": return "Owner"
        case "APPROVER": return "Approver"
        case "OPERATOR", "STAFF": return "Staff"
        case "VIEWER": return "Viewer"
        default: return model.vertical.label
        }
    }

    func boardCard<Content: View>(
        borderOpacity: Double = 0.28,
        @ViewBuilder content: @escaping () -> Content
    ) -> some View {
        OperatingV12Card(theme: theme, borderOpacity: borderOpacity, content: content)
    }

    func kpiRow(_ items: [(String, String, Color)]) -> some View {
        HStack(spacing: 8) {
            ForEach(Array(items.enumerated()), id: \.offset) { _, item in
                OperatingV12Tile(label: item.0, value: item.1, tone: item.2)
            }
        }
    }

    func emptyBlock(_ title: String, _ body: String) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(title)
                .font(.plusJakarta(size: 13, weight: .semibold))
                .foregroundStyle(OperatingV12.title)
            Text(body)
                .font(.plusJakarta(size: 12, weight: .medium))
                .foregroundStyle(OperatingV12.subtitle)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    func detailLine(eyebrow: String, value: String, trailing: String, trailingTone: Color) -> some View {
        HStack(alignment: .top, spacing: 10) {
            VStack(alignment: .leading, spacing: 2) {
                Text(eyebrow)
                    .font(.plusJakarta(size: 11, weight: .medium))
                    .foregroundStyle(OperatingV12.subtitle)
                Text(value)
                    .font(.plusJakarta(size: 13, weight: .semibold))
                    .foregroundStyle(OperatingV12.title)
                    .fixedSize(horizontal: false, vertical: true)
            }
            Spacer(minLength: 8)
            Text(trailing)
                .font(.plusJakarta(size: 12, weight: .bold))
                .foregroundStyle(trailingTone)
        }
        .padding(.vertical, 6)
    }

    func rowList(_ rows: [OperatingActivityPayload]) -> some View {
        VStack(spacing: 6) {
            ForEach(rows) { row in
                Button {
                    onOpenRow?(row)
                } label: {
                    OperatingV12Row(
                        title: row.title,
                        subtitle: row.subtitle,
                        trailing: rowTrailing(row),
                        chip: row.badge,
                        bar: OperatingV12.bar(for: row.badge),
                        accent: theme.accent
                    )
                }
                .buttonStyle(.plain)
                .disabled(onOpenRow == nil)
            }
        }
    }

    func rowTrailing(_ row: OperatingActivityPayload) -> String? {
        if model.financeVisible, let amount = row.amount, !amount.isEmpty {
            return OperatingV12Money.amount(amount, currency: model.currency)
        }
        if let quantity = row.quantity, !quantity.isEmpty {
            return [quantity, row.unit ?? ""].filter { !$0.isEmpty }.joined(separator: " ")
        }
        return nil
    }
}

private extension RetailFamilyLifeScreen {
    func boardCard<Content: View>(
        borderOpacity: Double = 0.28,
        @ViewBuilder content: @escaping () -> Content
    ) -> some View {
        OperatingV12Card(theme: theme, borderOpacity: borderOpacity, content: content)
    }

    func kpiRow(_ items: [(String, String, Color)]) -> some View {
        HStack(spacing: 8) {
            ForEach(Array(items.enumerated()), id: \.offset) { _, item in
                OperatingV12Tile(label: item.0, value: item.1, tone: item.2)
            }
        }
    }

    func emptyBlock(_ title: String, _ body: String) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(title)
                .font(.plusJakarta(size: 13, weight: .semibold))
                .foregroundStyle(OperatingV12.title)
            Text(body)
                .font(.plusJakarta(size: 12, weight: .medium))
                .foregroundStyle(OperatingV12.subtitle)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    func detailLine(eyebrow: String, value: String, trailing: String, trailingTone: Color) -> some View {
        HStack(alignment: .top, spacing: 10) {
            VStack(alignment: .leading, spacing: 2) {
                Text(eyebrow)
                    .font(.plusJakarta(size: 11, weight: .medium))
                    .foregroundStyle(OperatingV12.subtitle)
                Text(value)
                    .font(.plusJakarta(size: 13, weight: .semibold))
                    .foregroundStyle(OperatingV12.title)
                    .fixedSize(horizontal: false, vertical: true)
            }
            Spacer(minLength: 8)
            Text(trailing)
                .font(.plusJakarta(size: 12, weight: .bold))
                .foregroundStyle(trailingTone)
        }
        .padding(.vertical, 6)
    }

    func rowList(
        _ rows: [OperatingActivityPayload],
        trailingOverride: ((OperatingActivityPayload) -> String?)? = nil,
        onOpenRow: ((OperatingActivityPayload) -> Void)?
    ) -> some View {
        VStack(spacing: 6) {
            ForEach(rows) { row in
                Button {
                    onOpenRow?(row)
                } label: {
                    OperatingV12Row(
                        title: row.title,
                        subtitle: row.subtitle,
                        trailing: trailingOverride?(row) ?? rowTrailing(row),
                        chip: row.badge,
                        bar: OperatingV12.bar(for: row.badge),
                        accent: theme.accent
                    )
                }
                .buttonStyle(.plain)
                .disabled(onOpenRow == nil)
            }
        }
    }

    func rowTrailing(_ row: OperatingActivityPayload) -> String? {
        if model.financeVisible, let amount = row.amount, !amount.isEmpty {
            return OperatingV12Money.amount(amount, currency: model.currency)
        }
        return nil
    }
}

private extension RetailFamilyMemoryScreen {
    func boardCard<Content: View>(
        borderOpacity: Double = 0.28,
        @ViewBuilder content: @escaping () -> Content
    ) -> some View {
        OperatingV12Card(theme: theme, borderOpacity: borderOpacity, content: content)
    }

    func kpiRow(_ items: [(String, String, Color)]) -> some View {
        HStack(spacing: 8) {
            ForEach(Array(items.enumerated()), id: \.offset) { _, item in
                OperatingV12Tile(label: item.0, value: item.1, tone: item.2)
            }
        }
    }

    func emptyBlock(_ title: String, _ body: String) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(title)
                .font(.plusJakarta(size: 13, weight: .semibold))
                .foregroundStyle(OperatingV12.title)
            Text(body)
                .font(.plusJakarta(size: 12, weight: .medium))
                .foregroundStyle(OperatingV12.subtitle)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    func detailLine(eyebrow: String, value: String, trailing: String, trailingTone: Color) -> some View {
        HStack(alignment: .top, spacing: 10) {
            VStack(alignment: .leading, spacing: 2) {
                Text(eyebrow)
                    .font(.plusJakarta(size: 11, weight: .medium))
                    .foregroundStyle(OperatingV12.subtitle)
                Text(value)
                    .font(.plusJakarta(size: 13, weight: .semibold))
                    .foregroundStyle(OperatingV12.title)
                    .fixedSize(horizontal: false, vertical: true)
            }
            Spacer(minLength: 8)
            Text(trailing)
                .font(.plusJakarta(size: 12, weight: .bold))
                .foregroundStyle(trailingTone)
        }
        .padding(.vertical, 6)
    }

    func rowList(
        _ rows: [OperatingActivityPayload],
        trailingOverride: ((OperatingActivityPayload) -> String?)? = nil,
        onOpenRow: ((OperatingActivityPayload) -> Void)?
    ) -> some View {
        VStack(spacing: 6) {
            ForEach(rows) { row in
                Button {
                    onOpenRow?(row)
                } label: {
                    OperatingV12Row(
                        title: row.title,
                        subtitle: row.subtitle,
                        trailing: trailingOverride?(row),
                        chip: row.badge,
                        bar: OperatingV12.bar(for: row.badge),
                        accent: theme.accent
                    )
                }
                .buttonStyle(.plain)
                .disabled(onOpenRow == nil)
            }
        }
    }
}

private enum RetailFamilyFormat {
    static func dayOfPeriod(startDate: String?) -> String {
        guard let raw = startDate, raw.count >= 10 else { return "Unavailable" }
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        formatter.locale = Locale(identifier: "en_US_POSIX")
        guard let start = formatter.date(from: String(raw.prefix(10))) else { return "Unavailable" }
        let calendar = Calendar.current
        let today = calendar.startOfDay(for: Date())
        let from = calendar.startOfDay(for: start)
        let days = calendar.dateComponents([.day], from: from, to: today).day ?? 0
        if days < 0 { return "0" }
        return "\(days + 1)"
    }
}

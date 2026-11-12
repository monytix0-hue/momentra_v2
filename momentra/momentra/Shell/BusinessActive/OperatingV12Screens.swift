import SwiftUI

// MARK: - Shared plumbing

extension BusinessOperatingVertical {
    /// `TRD-PULSE-v12` / marketing hero gradient endpoints.
    var heroStart: Color {
        switch self {
        case .trading: return BusinessTokens.Colors.tradingHeroStart
        case .distribution: return BusinessTokens.Colors.distributionHeroStart
        // MFG solid amber lead; MTO soft-amber lead (`#FBBF24` → `#F59E0B`).
        case .standardProduction: return BusinessTokens.Colors.manufacturingHeroStart
        case .madeToOrder: return BusinessTokens.Colors.manufacturingAccentSoft
        case .clientCampaign: return BusinessTokens.Colors.marketingHeroStart
        case .ownBusinessCampaign: return BusinessTokens.Colors.marketingOwnHeroStart
        case .retailStore: return BusinessTokens.Colors.retailHeroStart
        case .restaurantFood: return BusinessTokens.Colors.restaurantHeroStart
        case .consumerService: return BusinessTokens.Colors.servicesHeroStart
        }
    }

    var heroEnd: Color {
        switch self {
        case .trading: return BusinessTokens.Colors.tradingHeroEnd
        case .distribution: return BusinessTokens.Colors.distributionHeroEnd
        case .standardProduction: return BusinessTokens.Colors.manufacturingHeroEnd
        case .madeToOrder: return BusinessTokens.Colors.manufacturingAccent
        case .clientCampaign: return BusinessTokens.Colors.marketingHeroEnd
        case .ownBusinessCampaign: return BusinessTokens.Colors.marketingOwnHeroEnd
        case .retailStore: return BusinessTokens.Colors.retailHeroEnd
        case .restaurantFood: return BusinessTokens.Colors.restaurantHeroEnd
        case .consumerService: return BusinessTokens.Colors.servicesHeroEnd
        }
    }

    var label: String {
        switch self {
        case .trading: return "Trading"
        case .distribution: return "Distribution"
        case .standardProduction: return "Manufacturing"
        case .madeToOrder: return "Make to order"
        case .clientCampaign: return "Client Campaign"
        case .ownBusinessCampaign: return "Own Business Campaign"
        case .retailStore: return "Retail Store"
        case .restaurantFood: return "Restaurant"
        case .consumerService: return "Service"
        }
    }
}

/// Dates arrive as `2026-10-28` or a full ISO stamp. The frames print `28 Oct`, `Oct 2026`,
/// and `29 Oct 2026`.
enum OperatingV12Date {
    private static let day: DateFormatter = {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        formatter.locale = Locale(identifier: "en_US_POSIX")
        return formatter
    }()

    private static func parse(_ raw: String?) -> Date? {
        guard let raw, raw.count >= 10 else { return nil }
        return day.date(from: String(raw.prefix(10)))
    }

    static func short(_ raw: String?) -> String? {
        guard let date = parse(raw) else { return nil }
        let out = DateFormatter()
        out.dateFormat = "d MMM"
        out.locale = Locale(identifier: "en_US")
        return out.string(from: date)
    }

    static func monthYear(_ raw: String?) -> String? {
        guard let date = parse(raw) else { return nil }
        let out = DateFormatter()
        out.dateFormat = "MMM yyyy"
        out.locale = Locale(identifier: "en_US")
        return out.string(from: date)
    }

    static func monthDayYear(_ raw: String?) -> String? {
        guard let date = parse(raw) else { return nil }
        let out = DateFormatter()
        out.dateFormat = "d MMM yyyy"
        out.locale = Locale(identifier: "en_US")
        return out.string(from: date)
    }

    static func today() -> String {
        let out = DateFormatter()
        out.dateFormat = "d MMM yyyy"
        out.locale = Locale(identifier: "en_US")
        return out.string(from: Date())
    }
}

/// The shared skeleton of a v12 tab: hero, quick actions, then a column of widgets.
///
/// The three sections have different horizontal rhythm — Pulse's hero is edge to edge, every
/// other hero is inset 16, and the quick actions are inset 20 — so the padding lives in each
/// screen rather than being applied once by the shell.
private struct OperatingV12Column<Content: View>: View {
    @ViewBuilder var content: () -> Content

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            content()
        }
        .padding(.top, 8)
        .padding(.bottom, 24)
    }
}

// MARK: - Pulse

/// `TRD-PULSE-v12` / `DST-PULSE-v12`.
///
/// Six widgets in the frame's order — attention, plan, payment, finance, activity, GST — each one
/// a projection figure rather than a board section. A surface the moment has switched off (finance
/// hidden, GST not tracked) is not drawn at all instead of drawn empty.
struct OperatingPulseScreen: View {
    @ObservedObject var model: BusinessOperatingModel
    let theme: OperatingTheme
    var onCapture: (String) -> Void
    var onViewAll: (String) -> Void
    var onOpenRow: ((OperatingActivityPayload) -> Void)? = nil

    private var projection: OperatingProjectionPayload? { model.projection }

    var body: some View {
        OperatingV12Column {
            OperatingV12PulseHero(
                theme: theme,
                chips: heroChips,
                title: model.title,
                subtitle: heroSubtitle,
                badge: projection?.retired == true ? "Books closed" : "Books open · live",
                glyph: "waveform.path.ecg"
            )
            OperatingV12QuickActions(theme: theme, tinted: true, radius: 16, onOpen: onCapture)
                .padding(.horizontal, 20)
            VStack(alignment: .leading, spacing: 16) {
                attentionCard
                targetCard
                paymentCard
                if model.financeVisible { financeCard }
                activityCard
                if gstEnabled { gstCard }
            }
            .padding(.horizontal, 16)
        }
    }

    // MARK: Hero

    private var heroChips: [String] {
        var chips = [model.title]
        let state = projection?.gstState ?? ""
        let gstin = projection?.gstRegistration ?? ""
        if !state.isEmpty || !gstin.isEmpty {
            chips.append("Channel / Route · \(state.isEmpty ? "State" : state) · GSTIN \(gstin.isEmpty ? "Not recorded" : gstin)")
        } else {
            chips.append("\(model.vertical.label) · \(model.currency)")
        }
        return chips
    }

    private var heroSubtitle: String {
        guard let start = projection?.startDate, let year = Int(start.prefix(4)) else {
            return "This period · \(model.currency)"
        }
        return "FY \(year)–\(String(format: "%02d", (year + 1) % 100)) · \(model.currency)"
    }

    private var gstEnabled: Bool {
        (projection?.gstTracking ?? "").uppercased() == "YES"
    }

    private var periodLabel: String {
        OperatingV12Date.monthYear(projection?.startDate) ?? "This month"
    }

    private func widget(_ code: String) -> OperatingPulseMetricGroupPayload? {
        model.pulseGroup(code: code)
    }

    // MARK: AN-P01 · Needs Attention

    private var attentionCard: some View {
        let group = widget("ATTENTION")
        let items = Array((projection?.attention ?? []).prefix(5))
        return OperatingV12Card(theme: theme, fill: OperatingV12.pulseSurface) {
            OperatingV12WidgetHeader(
                title: "Needs Attention",
                subtitle: "Severity then due · goods and money are separate axes",
                onViewAll: { onViewAll("ATTENTION") }
            )
            if items.isEmpty {
                OperatingV12QuietRow(title: "Nothing is waiting.")
            } else {
                VStack(spacing: 6) {
                    ForEach(items) { item in
                        OperatingV12Row(
                            title: item.title,
                            subtitle: attentionSubtitle(item),
                            chip: attentionChip(item),
                            bar: OperatingV12.bar(for: attentionChip(item)),
                            accent: theme.accent
                        )
                    }
                }
            }
            OperatingV12Why(text: group?.why ?? Self.attentionWhy, tone: .white)
        }
    }

    private func attentionSubtitle(_ item: OperatingAttentionPayload) -> String? {
        var parts: [String] = []
        if let name = item.counterpartyName, !name.isEmpty { parts.append(name) }
        if let due = OperatingV12Date.short(item.dueAt) { parts.append("due \(due)") }
        if let amount = item.amount, !amount.isEmpty {
            parts.append(OperatingV12Money.amount(amount, currency: model.currency))
        }
        return parts.isEmpty ? nil : parts.joined(separator: " · ")
    }

    /// `CRITICAL` reads as `Overdue`, `HIGH` as `Alert`, `MEDIUM` as `Unavailable` — the three
    /// words the frame uses, which say what is late rather than how badly.
    private func attentionChip(_ item: OperatingAttentionPayload) -> String? {
        switch (item.severity ?? "").uppercased() {
        case "CRITICAL": return "Overdue"
        case "HIGH": return "Alert"
        case "MEDIUM": return "Unavailable"
        default: return nil
        }
    }

    // MARK: AN-P02 · Target & Budget

    private var targetCard: some View {
        let target = projection?.target
        let budget = projection?.budget
        let configured = target?.configured ?? false
        let targetValue = OperatingV12Money.amount(target?.value, currency: model.currency)
        let achieved = OperatingV12Money.amount(target?.progress, currency: model.currency)
        let targetNumber = OperatingV12Money.number(target?.value) ?? 0
        let progressNumber = OperatingV12Money.number(target?.progress) ?? 0
        let fraction = targetNumber > 0 ? progressNumber / targetNumber : 0
        let onTrack = targetNumber > 0 && progressNumber >= targetNumber
        let spentNumber = OperatingV12Money.number(budget?.spent) ?? 0
        let amountNumber = OperatingV12Money.number(budget?.amount) ?? 0
        let usedPct = amountNumber > 0 ? Int(((spentNumber / amountNumber) * 100).rounded()) : nil

        return OperatingV12Card(theme: theme, fill: OperatingV12.pulseSurface) {
            OperatingV12WidgetHeader(
                title: "Target & Budget",
                subtitle: "Period · \(periodLabel) · as-of effective plan",
                onViewAll: { onViewAll("TARGET_BUDGET") }
            )
            HStack(spacing: 16) {
                OperatingV12Ring(
                    fraction: fraction,
                    caption: targetNumber > 0 ? "\(Int((fraction * 100).rounded()))%" : "—",
                    note: "of target",
                    accent: theme.accent
                )
                VStack(alignment: .leading, spacing: 6) {
                    Text("ACHIEVEMENT VS TARGET")
                        .font(.plusJakarta(size: 10, weight: .bold))
                        .tracking(0.6)
                        .foregroundStyle(theme.accent)
                    Text(configured && targetNumber > 0 ? "Sales \(achieved) of \(targetValue)" : "No target set")
                        .font(.plusJakarta(size: 15, weight: .bold))
                        .foregroundStyle(.white)
                        .lineLimit(2)
                        .minimumScaleFactor(0.75)
                    if configured && targetNumber > 0 {
                        Text(onTrack ? "● On track" : "● Behind plan")
                            .font(.plusJakarta(size: 10, weight: .bold))
                            .foregroundStyle(onTrack ? OperatingV12.green : OperatingV12.amber)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 3)
                            .background((onTrack ? OperatingV12.green : OperatingV12.amber).opacity(0.16))
                            .clipShape(Capsule())
                    }
                }
                Spacer(minLength: 0)
            }
            .padding(14)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(theme.canvas)
            .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .stroke(theme.accent.opacity(0.22), lineWidth: 1)
            )

            HStack(spacing: 8) {
                OperatingV12Tile(label: "Sales", value: achieved, tone: OperatingV12.title)
                OperatingV12Tile(label: "Target", value: targetValue, tone: OperatingV12.title)
            }
            HStack(spacing: 8) {
                OperatingV12Tile(
                    label: "Budget used",
                    value: usedPct.map { "\($0)%" } ?? "—",
                    tone: OperatingV12.cyan
                )
                OperatingV12Tile(
                    label: "Spend",
                    value: OperatingV12Money.amount(budget?.spent, currency: model.currency),
                    tone: OperatingV12.title
                )
                OperatingV12Tile(
                    label: "Budget",
                    value: OperatingV12Money.amount(budget?.amount, currency: model.currency),
                    tone: OperatingV12.title
                )
            }
            OperatingV12Why(
                text: widget("TARGET_BUDGET")?.why
                    ?? "sales ÷ effective target; spend ÷ effective budget. Empty ≠ zero — coverage noted when attribution is partial.",
                tone: .white
            )
        }
    }

    // MARK: AN-P03 · Payment Readiness

    /// The frame's four tiles are two sums and their overdue halves. They come from
    /// `paymentReadiness` rather than the metric group, because the group counts rows while the
    /// frame prints money.
    private var paymentCard: some View {
        let due = projection?.paymentReadiness ?? []
        let receivable = due.filter { $0.relevance.uppercased() == "RECEIVABLE" }
        let payable = due.filter { $0.relevance.uppercased() == "PAYABLE" }
        let receivableTotal = receivable.compactMap { OperatingV12Money.number($0.amount) }.reduce(0, +)
        let payableTotal = payable.compactMap { OperatingV12Money.number($0.amount) }.reduce(0, +)
        let overdueAr = receivable
            .filter { ($0.dueBucket ?? "").uppercased() == "OVERDUE" }
            .compactMap { OperatingV12Money.number($0.amount) }
            .reduce(0, +)
        let overdueAp = payable
            .filter { ($0.dueBucket ?? "").uppercased() == "OVERDUE" }
            .compactMap { OperatingV12Money.number($0.amount) }
            .reduce(0, +)
        let hasDues = !due.isEmpty

        return OperatingV12Card(theme: theme, fill: OperatingV12.pulseSurface) {
            OperatingV12WidgetHeader(
                title: "Payment Readiness",
                subtitle: "Invoice totals include GST · dues with due dates",
                onViewAll: { onViewAll("PAYMENT_READINESS") }
            )
            if hasDues {
                HStack(spacing: 8) {
                    OperatingV12Tile(
                        label: "Collections due",
                        value: OperatingV12Money.compact(receivableTotal, currency: model.currency),
                        tone: OperatingV12.red
                    )
                    OperatingV12Tile(
                        label: "Overdue AR",
                        value: OperatingV12Money.compact(overdueAr, currency: model.currency),
                        tone: OperatingV12.red
                    )
                }
                HStack(spacing: 8) {
                    OperatingV12Tile(
                        label: "Supplier dues",
                        value: OperatingV12Money.compact(payableTotal, currency: model.currency),
                        tone: OperatingV12.title
                    )
                    OperatingV12Tile(
                        label: "Overdue AP",
                        value: OperatingV12Money.compact(overdueAp, currency: model.currency),
                        tone: OperatingV12.red
                    )
                }
            } else {
                OperatingV12TileGrid(
                    theme: theme,
                    metrics: widget("PAYMENT_READINESS")?.metrics ?? [],
                    columns: 2,
                    currency: model.currency
                )
            }
            OperatingV12Why(
                text: widget("PAYMENT_READINESS")?.why
                    ?? "open payable/receivable = charge − allocations/credits. No overdue alert without a due date.",
                tone: .white
            )
        }
    }

    // MARK: AN-P04 · Finance

    /// The projection sends four finance figures (sales, received, outstanding, payable), so this
    /// widget lays them two up and never invents the frame's purchases or expense tiles.
    private var financeCard: some View {
        let metrics = widget("FINANCE")?.metrics ?? []
        return OperatingV12Card(theme: theme, fill: OperatingV12.pulseSurface) {
            OperatingV12WidgetHeader(
                title: "Finance",
                subtitle: "Flows by event date · balances as-of end",
                onViewAll: { onViewAll("FINANCE") }
            )
            OperatingV12TileGrid(
                theme: theme,
                metrics: metrics,
                columns: metrics.count >= 5 ? 3 : 2,
                currency: model.currency
            )
            OperatingV12Why(
                text: widget("FINANCE")?.why
                    ?? "revenue/purchase tax-exclusive; AR/AP GST-inclusive. Sale ≠ receipt — cash only from receipts/payments.",
                tone: .white
            )
        }
    }

    // MARK: AN-P05 · Activity

    private var activityCard: some View {
        let group = widget("ACTIVITY")
        let rows = Array((projection?.activity ?? []).prefix(4))
        return OperatingV12Card(theme: theme, fill: OperatingV12.pulseSurface) {
            OperatingV12WidgetHeader(
                title: "Activity",
                subtitle: "Today · posted events only",
                onViewAll: { onViewAll("ACTIVITY") }
            )
            OperatingV12TileGrid(
                theme: theme,
                metrics: group?.metrics ?? [],
                columns: 3,
                currency: model.currency
            )
            if rows.isEmpty {
                Text("Nothing recorded yet.")
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(OperatingV12.subtitle)
            } else {
                VStack(alignment: .leading, spacing: 6) {
                    ForEach(rows) { row in
                        Button {
                            onOpenRow?(row)
                        } label: {
                            Text(activityLine(row))
                                .font(.plusJakarta(size: 12, weight: .medium))
                                .foregroundStyle(OperatingV12.title)
                                .frame(maxWidth: .infinity, alignment: .leading)
                                .multilineTextAlignment(.leading)
                        }
                        .buttonStyle(.plain)
                        .disabled(onOpenRow == nil)
                    }
                }
            }
            OperatingV12Why(
                text: group?.why
                    ?? "chronological posted timeline; drafts/reversed distinguished; money vs operational events labeled.",
                tone: .white
            )
        }
    }

    private func activityLine(_ row: OperatingActivityPayload) -> String {
        var parts = ["· \(row.title.isEmpty ? row.eventType.capitalized : row.title)"]
        if model.financeVisible, let amount = row.amount, !amount.isEmpty {
            parts.append(OperatingV12Money.amount(amount, currency: model.currency))
        }
        if let name = row.counterpartyName, !name.isEmpty { parts.append(name) }
        return parts.joined(separator: " · ")
    }

    // MARK: AN-P06 · GST Position

    private var gstCard: some View {
        let gst = projection?.gstPosition
        let difference = OperatingV12Money.number(gst?.difference) ?? 0
        let differenceText = OperatingV12Money.amount(gst?.difference, currency: model.currency)

        return OperatingV12Card(theme: theme, fill: OperatingV12.pulseSurface) {
            OperatingV12WidgetHeader(
                title: "GST Position",
                subtitle: "Current month · As of \(OperatingV12Date.monthDayYear(projection?.endDate) ?? OperatingV12Date.today()) · GST enabled",
                onViewAll: { onViewAll("GST") }
            )
            HStack(spacing: 8) {
                OperatingV12Tile(
                    label: "GST on Sales",
                    value: OperatingV12Money.amount(gst?.outputGst, currency: model.currency),
                    tone: theme.accent
                )
                OperatingV12Tile(
                    label: "GST on Purchases",
                    value: OperatingV12Money.amount(gst?.inputGst, currency: model.currency),
                    tone: OperatingV12.cyan
                )
                OperatingV12Tile(
                    label: "Est. difference",
                    value: differenceText,
                    tone: difference == 0 ? OperatingV12.title : OperatingV12.red
                )
            }
            Text("Estimated difference: \(differenceText) payable before adjustments")
                .font(.plusJakarta(size: 12, weight: .medium))
                .foregroundStyle(OperatingV12.subtitle)
                .frame(maxWidth: .infinity, alignment: .leading)
            OperatingV12Why(
                text: widget("GST")?.why
                    ?? "Output GST − Input GST (indicative). Never label as government refund due. Hidden when GST disabled.",
                tone: .white
            )
        }
    }

    private static let attentionWhy = "open orders, deliveries/receipts due, collections/payments due, issues — never conflate delivery with settlement."
}

// MARK: - Moments

/// `TRD-MOMENTS-v12` / `DST-MOMENTS-v12`. Hero, captures, then the four commitment surfaces.
///
/// Retail-family kits bind `sales` / `purchases` / `stock` / `channels` (or service
/// `services` / `appointments` / `customers`) — never trading `orders` / `network`.
/// Manufacturing / MTO kits bind `orders` / `production` / `stock` — never trading `network`.
struct OperatingMomentsScreen: View {
    @ObservedObject var model: BusinessOperatingModel
    let theme: OperatingTheme
    var onCapture: (String) -> Void
    var onViewAll: (String) -> Void
    var onOpenRow: ((OperatingActivityPayload) -> Void)? = nil

    private var network: OperatingBoardSectionPayload? {
        if model.vertical.isRetailFamily {
            let code = model.vertical == .consumerService ? "customers" : "channels"
            return model.section(code: code) ?? model.section(code: "M3")
        }
        if model.vertical.isManufacturing {
            // Manufacturing boards emit `M3` (Business Network), not trading `network`.
            return model.section(code: "M3")
        }
        return model.section(code: "network")
    }
    private var commitments: OperatingBoardSectionPayload? { model.section(code: "M4") }

    var body: some View {
        OperatingV12Column {
            OperatingV12Hero(
                theme: theme,
                eyebrow: "ACTIVE MOMENT",
                title: model.title,
                chips: [
                    .init(label: model.vertical.label, tone: theme.accent),
                    .init(
                        label: model.projection?.retired == true ? "Books closed" : "Books open",
                        tone: OperatingV12.green,
                        dot: true
                    ),
                ],
                kpis: momentsHeroKpis
            )
            .padding(.horizontal, 16)

            OperatingV12QuickActions(theme: theme, onOpen: onCapture)
                .padding(.horizontal, 20)

            VStack(alignment: .leading, spacing: 16) {
                sectionCard(
                    code: "M4",
                    title: momentsOpenTitle,
                    subtitle: "What is still open · not alarms",
                    keys: ["toCollect", "toPay", "openOrders"],
                    labels: [
                        "AR": "AR",
                        "toCollect": "AR",
                        "toPay": "AP",
                        "openOrders": model.vertical.isMadeToOrder ? "Jobs" : "Open orders",
                    ],
                    fallbackMetrics: retailOpenCommitmentMetrics
                )
                ForEach(momentsBoardSlots, id: \.code) { slot in
                    sectionCard(
                        code: slot.code,
                        title: slot.title,
                        subtitle: slot.subtitle,
                        keys: slot.keys
                    )
                }
            }
            .padding(.horizontal, 16)
        }
    }

    private struct BoardSlot {
        let code: String
        let title: String
        let subtitle: String
        let keys: [String]
    }

    /// Projection section codes the Moments tab actually renders after Open Commitments.
    private var momentsBoardSlots: [BoardSlot] {
        switch model.vertical {
        case .retailStore, .restaurantFood:
            return [
                BoardSlot(code: "sales", title: "Sales & Orders", subtitle: "Posted sales · orders are commitment only", keys: ["netSales", "openOrders", "tickets"]),
                BoardSlot(code: "purchases", title: "Purchases", subtitle: "Goods received · payable until settled", keys: ["purchases", "received", "open"]),
                BoardSlot(code: "stock", title: "Stock & Supplies", subtitle: "On-hand · counts · low stock", keys: ["onHand", "stockMoves", "low"]),
                BoardSlot(code: "channels", title: "People & Network", subtitle: "Customers, suppliers & channels", keys: ["buyers", "suppliers", "channels"]),
            ]
        case .consumerService:
            return [
                BoardSlot(code: "services", title: "Services", subtitle: "Finalized charges · revenue once", keys: ["netSales", "finalized", "pending"]),
                BoardSlot(code: "appointments", title: "Appointments / Jobs", subtitle: "Bookings · not revenue until charged", keys: ["open", "completed", "today"]),
                BoardSlot(code: "sales", title: "Sales", subtitle: "Posted retail sales alongside service", keys: ["netSales", "tickets"]),
                BoardSlot(code: "customers", title: "Customers & Network", subtitle: "Named clients & channels", keys: ["buyers", "suppliers", "channels"]),
            ]
        case .standardProduction, .madeToOrder:
            // Backend `familyMomentSections(MANUFACTURING_PRODUCTION)` emits orders / production / stock.
            return [
                BoardSlot(code: "orders", title: momentsOrdersTitle, subtitle: momentsOrdersSubtitle, keys: ["openOrders", "inTransit", "netSales"]),
                BoardSlot(code: "production", title: momentsProductionTitle, subtitle: momentsProductionSubtitle, keys: ["recorded", "accepted", "pending"]),
                BoardSlot(code: "stock", title: momentsStockTitle, subtitle: momentsStockSubtitle, keys: ["onHand", "stockMoves", "low"]),
            ]
        default:
            return [
                BoardSlot(code: "orders", title: momentsOrdersTitle, subtitle: momentsOrdersSubtitle, keys: ["openOrders", "inTransit", "netSales"]),
                BoardSlot(code: "stock", title: momentsStockTitle, subtitle: momentsStockSubtitle, keys: ["onHand", "stockMoves", "low"]),
                BoardSlot(code: "network", title: momentsNetworkTitle, subtitle: momentsNetworkSubtitle, keys: ["buyers", "suppliers", "channels"]),
            ]
        }
    }

    private var momentsHeroKpis: [OperatingV12KpiSpec] {
        if model.vertical.isRetailFamily || model.vertical.isManufacturing {
            let people = model.projection?.participants?.count
                ?? network?.rows?.count
                ?? 0
            let peopleLabel = model.vertical.isManufacturing
                ? "People · \(model.vertical.partyNoun)"
                : "People · Staff"
            let ar = model.financeVisible
                ? (OperatingV12Metric.value(commitments, key: "toCollect", currency: model.currency)
                    ?? OperatingV12Money.amount(model.projection?.finance?.outstanding, currency: model.currency))
                : "—"
            return [
                OperatingV12KpiSpec(label: peopleLabel, value: "\(people)", tone: OperatingV12.title),
                OperatingV12KpiSpec(label: "AR open", value: ar, tone: theme.accent),
            ]
        }
        return [
            OperatingV12KpiSpec(
                label: "People · \(model.vertical.partyNoun)",
                value: OperatingV12Metric.value(network, key: "buyers", currency: model.currency) ?? "—",
                tone: OperatingV12.title
            ),
            OperatingV12KpiSpec(
                label: "AR open",
                value: OperatingV12Metric.value(commitments, key: "toCollect", currency: model.currency) ?? "—",
                tone: theme.accent
            ),
        ]
    }

    /// Retail boards rarely carry structured M4 metrics — fall back to finance figures.
    private var retailOpenCommitmentMetrics: [OperatingBoardMetricPayload]? {
        guard model.vertical.isRetailFamily else { return nil }
        let section = commitments
        if let metrics = section?.metrics, !metrics.isEmpty { return nil }
        let finance = model.projection?.finance
        return [
            OperatingBoardMetricPayload(
                key: "toPay",
                label: "AP",
                value: model.financeVisible
                    ? OperatingV12Money.amount(finance?.payable, currency: model.currency)
                    : "—",
                unit: nil,
                tone: "WARNING"
            ),
            OperatingBoardMetricPayload(
                key: "toCollect",
                label: "AR",
                value: model.financeVisible
                    ? OperatingV12Money.amount(finance?.outstanding, currency: model.currency)
                    : "—",
                unit: nil,
                tone: "POSITIVE"
            ),
            OperatingBoardMetricPayload(
                key: "openOrders",
                label: model.vertical == .consumerService ? "Bookings" : "Orders",
                value: "\(section?.rows?.count ?? 0)",
                unit: nil,
                tone: "NORMAL"
            ),
        ]
    }

    private var momentsOpenTitle: String {
        model.vertical.isMadeToOrder ? "Open Jobs" : "Open Commitments"
    }

    private var momentsOrdersTitle: String {
        switch model.vertical {
        case .madeToOrder: return "Current Jobs"
        case .standardProduction: return "Work Orders & Commitments"
        default: return "Orders & Commitments"
        }
    }

    private var momentsOrdersSubtitle: String {
        switch model.vertical {
        case .madeToOrder: return "Confirmed jobs · commitment only · not revenue"
        case .standardProduction: return "Plant work orders · commitment only"
        default: return "Open buyer & supplier orders · commitment only"
        }
    }

    private var momentsStockTitle: String {
        model.vertical.isMadeToOrder ? "Job Operations" : "Stock & Products"
    }

    private var momentsStockSubtitle: String {
        switch model.vertical {
        case .madeToOrder: return "WIP against open jobs · materials & FG"
        case .standardProduction: return "Material · WIP · finished goods"
        default: return "Physical movements · warehouse + routes"
        }
    }

    private var momentsProductionTitle: String {
        model.vertical.isMadeToOrder ? "Job Output" : "Production"
    }

    private var momentsProductionSubtitle: String {
        model.vertical.isMadeToOrder
            ? "Recorded vs accepted against open jobs"
            : "Recorded vs accepted plant output"
    }

    private var momentsNetworkTitle: String {
        "People & Network"
    }

    private var momentsNetworkSubtitle: String {
        "Bound suppliers, buyers & routes"
    }

    private func sectionCard(
        code: String,
        title: String,
        subtitle: String,
        keys: [String],
        labels: [String: String] = [:],
        fallbackMetrics: [OperatingBoardMetricPayload]? = nil
    ) -> some View {
        let section = model.section(code: code)
        var metrics = OperatingV12Tiles.ordered(section, keys: keys, limit: 3, currency: model.currency)
        if metrics.isEmpty, let fallbackMetrics, !fallbackMetrics.isEmpty {
            metrics = Array(fallbackMetrics.prefix(3))
        }
        let rows = Array((section?.rows ?? []).prefix(3))
        return OperatingV12Card(theme: theme) {
            OperatingV12WidgetHeader(
                title: title,
                subtitle: subtitle,
                onViewAll: { onViewAll(code) }
            )
            OperatingV12TileGrid(
                theme: theme,
                metrics: metrics.map { metric in
                    guard let label = labels[metric.key] else { return metric }
                    return OperatingBoardMetricPayload(
                        key: metric.key,
                        label: label,
                        value: metric.value,
                        unit: metric.unit,
                        tone: metric.tone
                    )
                },
                columns: 3,
                currency: model.currency
            )
            if rows.isEmpty {
                Text(quietLine(for: section))
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(OperatingV12.subtitle)
            } else {
                VStack(spacing: 6) {
                    ForEach(rows) { row in
                        Button {
                            onOpenRow?(row)
                        } label: {
                            OperatingV12Row(
                                title: rowTitle(row),
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
            OperatingV12Why(text: section?.why)
        }
    }

    private func quietLine(for section: OperatingBoardSectionPayload?) -> String {
        if let detail = section?.detail.trimmingCharacters(in: .whitespacesAndNewlines), !detail.isEmpty {
            return detail
        }
        return "Not recorded yet."
    }

    private func rowTitle(_ row: OperatingActivityPayload) -> String {
        if let subtitle = row.subtitle, !subtitle.isEmpty { return row.title }
        return row.title
    }

    private func rowTrailing(_ row: OperatingActivityPayload) -> String? {
        if model.financeVisible, let amount = row.amount, !amount.isEmpty {
            return OperatingV12Money.amount(amount, currency: model.currency)
        }
        if let quantity = row.quantity, !quantity.isEmpty {
            return [quantity, row.unit ?? ""].filter { !$0.isEmpty }.joined(separator: " ")
        }
        return nil
    }
}

// MARK: - Life

/// `TRD-LIFE-v12` / `DST-LIFE-v12`. The captures come first here, then today, then the four
/// surfaces — the frame's order, which puts the operator's next action above the read.
struct OperatingLifeScreen: View {
    @ObservedObject var model: BusinessOperatingModel
    let theme: OperatingTheme
    var onCapture: (String) -> Void
    var onViewAll: (String) -> Void
    var onOpenRow: ((OperatingActivityPayload) -> Void)? = nil

    private var money: OperatingBoardSectionPayload? { model.section(code: "L3") }

    var body: some View {
        OperatingV12Column {
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
                        label: model.projection?.retired == true ? "Books closed" : "Books open",
                        tone: OperatingV12.green,
                        dot: true
                    ),
                ],
                kpis: [
                    .init(label: "Open", value: "\(openCount)", tone: theme.accent),
                    .init(label: "Done", value: "\(doneCount)", tone: OperatingV12.green),
                    .init(
                        label: "Money in motion",
                        value: lifeMoneyInMotion,
                        tone: OperatingV12.title
                    ),
                ]
            )
            .padding(.horizontal, 16)

            VStack(alignment: .leading, spacing: 16) {
                sectionCard(
                    code: "L1",
                    title: lifeBusinessTitle,
                    subtitle: "As-of today · \(OperatingV12Date.today())",
                    keys: ["sales", "stockMoves", "issues"]
                )
                sectionCard(
                    code: "L2",
                    title: lifeL2Title,
                    subtitle: lifeL2Subtitle,
                    keys: ["channels", "buyers", "sales"]
                )
                sectionCard(
                    code: "L3",
                    title: "Money Moving",
                    subtitle: "Cash in / out · cash basis",
                    keys: ["collected", "paid", "net"]
                )
                sectionCard(
                    code: "L4",
                    title: lifePeopleTitle,
                    subtitle: "Assigned follow-ups",
                    keys: ["stockMoves", "issues", "updates"]
                )
            }
            .padding(.horizontal, 16)
        }
    }

    private var lifeBusinessTitle: String {
        switch model.vertical {
        case .madeToOrder: return "Current Jobs Today"
        // Projection widget label for MANUFACTURING_PRODUCTION L1.
        case .standardProduction: return "Current Production / Business"
        case .retailStore, .restaurantFood, .consumerService: return "Current Business"
        default: return "Current Business"
        }
    }

    private var lifeL2Title: String {
        switch model.vertical {
        case .madeToOrder: return "Jobs & Customers"
        case .standardProduction: return "Orders / Buyers"
        case .trading: return "Buyers & Channels"
        case .retailStore, .restaurantFood: return "Sales & Orders"
        case .consumerService: return "Channel Activity"
        default: return "Dealers & Channels"
        }
    }

    private var lifeL2Subtitle: String {
        switch model.vertical {
        case .madeToOrder: return "Attributed work by customer and channel"
        case .standardProduction: return "Attributed plant sales by buyer and channel"
        case .retailStore, .restaurantFood: return "Tickets · credit · holds · this period"
        case .consumerService: return "Attributed charges by channel"
        default: return "Channel / territory mix · this month"
        }
    }

    private var lifePeopleTitle: String {
        switch model.vertical {
        // Projection widget label for MANUFACTURING_PRODUCTION L4.
        case .standardProduction, .madeToOrder: return "Production Operations"
        case .retailStore, .restaurantFood, .consumerService: return "Operations"
        default: return "People & Activity"
        }
    }

    private var lifeMoneyInMotion: String {
        if let net = OperatingV12Metric.value(money, key: "net", currency: model.currency) {
            return net
        }
        if model.vertical.isRetailFamily, model.financeVisible {
            return OperatingV12Money.amount(model.projection?.finance?.received, currency: model.currency)
        }
        return "—"
    }

    /// `Open` and `Done` count the money rows the moment is actually carrying, so the hero never
    /// claims a task the projection did not send.
    private var openCount: Int {
        (money?.rows ?? []).filter { row in
            switch (row.badge ?? "").uppercased() {
            case "OPEN", "PART", "OVERDUE", "DUE", "TRANSIT": return true
            default: return false
            }
        }.count
    }

    private var doneCount: Int {
        (money?.rows ?? []).filter { row in
            switch (row.badge ?? "").uppercased() {
            case "DONE", "SETTLED", "DELIVERED": return true
            default: return false
            }
        }.count
    }

    private func sectionCard(code: String, title: String, subtitle: String, keys: [String]) -> some View {
        let section = model.section(code: code)
        let metrics = OperatingV12Tiles.ordered(section, keys: keys, limit: 3, currency: model.currency)
        let rows = Array((section?.rows ?? []).prefix(3))
        return OperatingV12Card(theme: theme) {
            OperatingV12WidgetHeader(
                title: title,
                subtitle: subtitle,
                onViewAll: { onViewAll(code) }
            )
            OperatingV12TileGrid(
                theme: theme,
                metrics: metrics,
                columns: 3,
                currency: model.currency
            )
            if rows.isEmpty {
                Text({
                    if let detail = section?.detail.trimmingCharacters(in: .whitespacesAndNewlines), !detail.isEmpty {
                        return detail
                    }
                    return "Not recorded yet."
                }())
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(OperatingV12.subtitle)
            } else {
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
            OperatingV12Why(text: section?.why)
        }
    }

    private func rowTrailing(_ row: OperatingActivityPayload) -> String? {
        if model.financeVisible, let amount = row.amount, !amount.isEmpty {
            return OperatingV12Money.amount(amount, currency: model.currency)
        }
        if let quantity = row.quantity, !quantity.isEmpty {
            return [quantity, row.unit ?? ""].filter { !$0.isEmpty }.joined(separator: " ")
        }
        return nil
    }
}

// MARK: - Memory

/// `TRD-MEMORY-v12` / `DST-MEMORY-v12`. The period hero and its one call to action, then the
/// four memory surfaces. Retail family has no `fulfilment` board — use People & Activity (ME3).
struct OperatingMemoryScreen: View {
    @ObservedObject var model: BusinessOperatingModel
    let theme: OperatingTheme
    var onCapture: (String) -> Void
    var onStory: () -> Void
    var onViewAll: (String) -> Void
    var onOpenRow: ((OperatingActivityPayload) -> Void)? = nil

    private var monthly: OperatingBoardSectionPayload? { model.section(code: "ME1") }
    private var money: OperatingBoardSectionPayload? { model.section(code: "ME2") }
    private var period: String {
        OperatingV12Date.monthYear(model.projection?.startDate) ?? "This month"
    }

    private var memoryMonthlySubtitle: String {
        switch model.vertical {
        case .madeToOrder: return "Closed periods · jobs & invoices (\(period))"
        case .standardProduction: return "Closed periods · plant target v2 (\(period))"
        case .retailStore, .restaurantFood, .consumerService:
            return "Closed periods · recorded activity (\(period))"
        default: return "Closed periods · target v2 (\(period))"
        }
    }

    private var memoryThirdCode: String {
        model.vertical.isRetailFamily ? "ME3" : "fulfilment"
    }

    private var memoryThirdTitle: String {
        if model.vertical.isRetailFamily { return "People & Activity" }
        return model.vertical.isMadeToOrder ? "Job Fulfilment Memory" : "Fulfilment Memory"
    }

    private var memoryThirdSubtitle: String {
        switch model.vertical {
        case .madeToOrder: return "Job → produce → invoice → dispatch"
        case .standardProduction: return "Order → produce → dispatch"
        case .retailStore, .restaurantFood, .consumerService:
            return "Who recorded what across the period"
        default: return "Order → dispatch → route delivery"
        }
    }

    private var memorySalesValue: String {
        if let value = OperatingV12Metric.value(monthly, key: "netSales", currency: model.currency) {
            return value
        }
        if model.vertical.isRetailFamily, model.financeVisible {
            return OperatingV12Money.amount(model.projection?.finance?.sales, currency: model.currency)
        }
        return "—"
    }

    private var memoryCollectedValue: String {
        if let value = OperatingV12Metric.value(money, key: "collected", currency: model.currency) {
            return value
        }
        if model.vertical.isRetailFamily, model.financeVisible {
            return OperatingV12Money.amount(model.projection?.finance?.received, currency: model.currency)
        }
        return "—"
    }

    var body: some View {
        OperatingV12Column {
            OperatingV12QuickActions(theme: theme, onOpen: onCapture)
                .padding(.horizontal, 20)

            OperatingV12Hero(
                theme: theme,
                eyebrow: "MEMORY · CLOSED PERIOD",
                title: "\(model.title) · memory",
                titleSize: 22,
                chips: [
                    .init(label: "\(period) · target v2", tone: theme.accent),
                    .init(label: "vs last period", tone: OperatingV12.subtitle),
                ],
                kpis: [
                    .init(
                        label: "Sales",
                        value: memorySalesValue,
                        tone: .white
                    ),
                    .init(
                        label: "Records",
                        value: OperatingV12Metric.value(monthly, key: "records", currency: model.currency)
                            ?? "\(monthly?.rows?.count ?? 0)",
                        tone: OperatingV12.cyan
                    ),
                    .init(
                        label: "Collected",
                        value: memoryCollectedValue,
                        tone: theme.accent
                    ),
                ]
            )
            .padding(.horizontal, 16)

            storyCTA
                .padding(.horizontal, 16)

            VStack(alignment: .leading, spacing: 16) {
                sectionCard(
                    code: "ME1",
                    title: "Monthly Memory",
                    subtitle: memoryMonthlySubtitle,
                    keys: ["netSales", "months", "records"]
                )
                sectionCard(
                    code: "ME2",
                    title: "Money Memory",
                    subtitle: model.vertical.isRetailFamily
                        ? "Collections, payments and returns"
                        : "Settled chains · FY 2026–27",
                    keys: ["collected", "paid", "credits"]
                )
                sectionCard(
                    code: memoryThirdCode,
                    title: memoryThirdTitle,
                    subtitle: memoryThirdSubtitle,
                    keys: model.vertical.isRetailFamily
                        ? ["issues", "updates", "records"]
                        : ["delivered", "received", "returns"]
                )
                sectionCard(
                    code: "ME4",
                    title: model.vertical.isRetailFamily ? "Patterns & Decisions" : "People & Patterns",
                    subtitle: model.vertical.isRetailFamily
                        ? "What repeats and what is waiting on approval"
                        : "Repeat behaviour · 6 months",
                    keys: ["issues", "approvals"]
                )
            }
            .padding(.horizontal, 16)
        }
    }

    /// The one call to action the Memory frame carries: accent fill, 18 pt corners, dark ink.
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

    private func sectionCard(code: String, title: String, subtitle: String, keys: [String]) -> some View {
        let section = model.section(code: code)
        let metrics = OperatingV12Tiles.ordered(section, keys: keys, limit: 3, currency: model.currency)
        let rows = Array((section?.rows ?? []).prefix(3))
        return OperatingV12Card(theme: theme) {
            OperatingV12WidgetHeader(
                title: title,
                subtitle: subtitle,
                onViewAll: { onViewAll(code) }
            )
            OperatingV12TileGrid(
                theme: theme,
                metrics: metrics,
                columns: metrics.count == 2 ? 2 : 3,
                currency: model.currency
            )
            if rows.isEmpty {
                Text({
                    if let detail = section?.detail.trimmingCharacters(in: .whitespacesAndNewlines), !detail.isEmpty {
                        return detail
                    }
                    return "Not recorded yet."
                }())
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(OperatingV12.subtitle)
            } else {
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
            OperatingV12Why(text: section?.why)
        }
    }

    private func rowTrailing(_ row: OperatingActivityPayload) -> String? {
        if model.financeVisible, let amount = row.amount, !amount.isEmpty {
            return OperatingV12Money.amount(amount, currency: model.currency)
        }
        if let quantity = row.quantity, !quantity.isEmpty {
            return [quantity, row.unit ?? ""].filter { !$0.isEmpty }.joined(separator: " ")
        }
        return nil
    }
}

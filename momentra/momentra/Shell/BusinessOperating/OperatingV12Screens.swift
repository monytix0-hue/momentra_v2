import SwiftUI

/// TRD/DST v12 tab bodies — Pulse, Moments, Life, Memory.
/// Mirrors Android `OperatingV12Screens.kt`.

struct OperatingV12PulseScreen: View {
    let model: OperatingProjectionPayload
    let theme: OperatingV12Theme
    var onCapture: (String) -> Void
    var onViewAll: (String) -> Void
    var onOpenRow: ((OperatingActivityPayload) -> Void)? = nil

    private var title: String {
        let t = model.title?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        return t.isEmpty ? theme.label : t
    }

    private var currency: String? { model.currency }
    private var period: String { OperatingV12Date.monthYear(model.startDate) ?? "This month" }
    private var gstOn: Bool { model.gstTracking?.caseInsensitiveCompare("YES") == .orderedSame }
    private var showFinance: Bool { model.financeVisible == true }

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            OperatingV12PulseHero(
                theme: theme,
                chips: pulseHeroChips,
                title: title,
                subtitle: "\(OperatingV12Date.fiscalYear(model.startDate)) · \(currency ?? "INR")",
                badge: (model.retired == true) ? "Books closed" : "Books open · live"
            )
            OperatingV12QuickActions(theme: theme, tinted: true, onOpen: onCapture)
            attentionCard
            targetCard
            paymentCard
            if showFinance { financeCard }
            activityCard
            if gstOn { gstCard }
        }
    }

    private var pulseHeroChips: [String] {
        var chips = [title]
        let state = model.gstState ?? ""
        let gstin = model.gstRegistration ?? ""
        if !state.isEmpty || !gstin.isEmpty {
            chips.append("Channel / Route · \(state.isEmpty ? "State" : state) · GSTIN \(gstin.isEmpty ? "Not recorded" : gstin)")
        } else {
            chips.append("\(theme.label) · \(currency ?? "INR")")
        }
        return chips
    }

    private var attentionCard: some View {
        let group = model.pulseGroup("ATTENTION")
        let items = Array((model.attention ?? []).prefix(5))
        return OperatingV12Card(theme: theme, fill: theme.surface) {
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
                        let chip = attentionChip(item)
                        OperatingV12Row(
                            title: item.title,
                            subtitle: attentionSubtitle(item),
                            chip: chip,
                            bar: OperatingV12.bar(chip),
                            accent: theme.accent
                        )
                    }
                }
            }
            OperatingV12Why(
                text: group?.why
                    ?? "open orders, deliveries/receipts due, collections/payments due, issues — never conflate delivery with settlement.",
                tone: .white
            )
        }
    }

    private func attentionSubtitle(_ item: OperatingAttentionPayload) -> String? {
        var parts: [String] = []
        if let n = item.counterpartyName, !n.isEmpty { parts.append(n) }
        if let due = OperatingV12Date.short(item.dueAt) { parts.append("due \(due)") }
        if model.financeVisible == true, let amount = item.amount, !amount.isEmpty {
            parts.append(OperatingV12Money.amount(amount, currency: currency))
        }
        return parts.isEmpty ? nil : parts.joined(separator: " · ")
    }

    private func attentionChip(_ item: OperatingAttentionPayload) -> String? {
        switch item.severity?.uppercased() {
        case "CRITICAL": return "Overdue"
        case "HIGH": return "Alert"
        case "MEDIUM": return "Unavailable"
        default: return nil
        }
    }

    private var targetCard: some View {
        let target = model.target
        let budget = model.budget
        let configured = target?.configured == true
        let targetValue = OperatingV12Money.amount(target?.value, currency: currency)
        let achieved = OperatingV12Money.amount(target?.progress, currency: currency)
        let targetNumber = OperatingV12Money.parse(target?.value) ?? 0
        let progressNumber = OperatingV12Money.parse(target?.progress) ?? 0
        let fraction = targetNumber > 0 ? progressNumber / targetNumber : 0
        let onTrack = targetNumber > 0 && progressNumber >= targetNumber * 0.7
        let spentNumber = OperatingV12Money.parse(budget?.spent) ?? 0
        let amountNumber = OperatingV12Money.parse(budget?.amount) ?? 0
        let usedPct: Int? = amountNumber > 0 ? Int(((spentNumber / amountNumber) * 100).rounded()) : nil

        return OperatingV12Card(theme: theme, fill: theme.surface) {
            OperatingV12WidgetHeader(
                title: "Target & Budget",
                subtitle: "Period · \(period) · as-of effective plan",
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
                        .font(.custom("PlusJakartaSans-Bold", size: 10))
                        .foregroundStyle(theme.accent)
                        .tracking(0.6)
                    Text(configured && targetNumber > 0 ? "Sales \(achieved) of \(targetValue)" : "No target set")
                        .font(.custom("PlusJakartaSans-Bold", size: 15))
                        .foregroundStyle(.white)
                        .lineLimit(2)
                    if configured && targetNumber > 0 {
                        let tone = onTrack ? OperatingV12.green : OperatingV12.amber
                        Text(onTrack ? "● On track" : "● Behind plan")
                            .font(.custom("PlusJakartaSans-Bold", size: 10))
                            .foregroundStyle(tone)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 3)
                            .background(tone.opacity(0.16))
                            .clipShape(Capsule())
                    }
                }
                Spacer(minLength: 0)
            }
            .padding(14)
            .background(theme.canvas)
            .overlay(
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .stroke(theme.accent.opacity(0.22), lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))

            HStack(spacing: 8) {
                OperatingV12Tile(label: "Sales", value: achieved)
                OperatingV12Tile(label: "Target", value: targetValue)
            }
            HStack(spacing: 8) {
                OperatingV12Tile(label: "Budget used", value: usedPct.map { "\($0)%" } ?? "—", tone: OperatingV12.cyan)
                OperatingV12Tile(label: "Spend", value: OperatingV12Money.amount(budget?.spent, currency: currency))
                OperatingV12Tile(label: "Budget", value: OperatingV12Money.amount(budget?.amount, currency: currency))
            }
            OperatingV12Why(
                text: model.pulseGroup("TARGET_BUDGET")?.why
                    ?? "sales ÷ effective target; spend ÷ effective budget. Empty ≠ zero — coverage noted when attribution is partial.",
                tone: .white
            )
        }
    }

    private var paymentCard: some View {
        let due = model.paymentReadiness ?? []
        let receivable = due.filter { $0.relevance.caseInsensitiveCompare("RECEIVABLE") == .orderedSame }
        let payable = due.filter { $0.relevance.caseInsensitiveCompare("PAYABLE") == .orderedSame }
        let receivableTotal = receivable.compactMap { OperatingV12Money.parse($0.amount) }.reduce(0, +)
        let payableTotal = payable.compactMap { OperatingV12Money.parse($0.amount) }.reduce(0, +)
        let overdueAr = receivable.filter { $0.dueBucket?.caseInsensitiveCompare("OVERDUE") == .orderedSame }
            .compactMap { OperatingV12Money.parse($0.amount) }.reduce(0, +)
        let overdueAp = payable.filter { $0.dueBucket?.caseInsensitiveCompare("OVERDUE") == .orderedSame }
            .compactMap { OperatingV12Money.parse($0.amount) }.reduce(0, +)

        return OperatingV12Card(theme: theme, fill: theme.surface) {
            OperatingV12WidgetHeader(
                title: "Payment Readiness",
                subtitle: "Invoice totals include GST · dues with due dates",
                onViewAll: { onViewAll("PAYMENT_READINESS") }
            )
            if !due.isEmpty {
                HStack(spacing: 8) {
                    OperatingV12Tile(
                        label: "Collections due",
                        value: OperatingV12Money.grouped(receivableTotal, currency: currency),
                        tone: OperatingV12.red
                    )
                    OperatingV12Tile(
                        label: "Overdue AR",
                        value: OperatingV12Money.grouped(overdueAr, currency: currency),
                        tone: OperatingV12.red
                    )
                }
                HStack(spacing: 8) {
                    OperatingV12Tile(
                        label: "Supplier dues",
                        value: OperatingV12Money.grouped(payableTotal, currency: currency)
                    )
                    OperatingV12Tile(
                        label: "Overdue AP",
                        value: OperatingV12Money.grouped(overdueAp, currency: currency),
                        tone: OperatingV12.red
                    )
                }
            } else {
                OperatingV12TileGrid(
                    theme: theme,
                    metrics: model.pulseGroup("PAYMENT_READINESS")?.metrics ?? [],
                    columns: 2,
                    currency: currency
                )
            }
            OperatingV12Why(
                text: model.pulseGroup("PAYMENT_READINESS")?.why
                    ?? "open payable/receivable = charge − allocations/credits. No overdue alert without a due date.",
                tone: .white
            )
        }
    }

    private var financeCard: some View {
        let metrics = model.pulseGroup("FINANCE")?.metrics ?? []
        return OperatingV12Card(theme: theme, fill: theme.surface) {
            OperatingV12WidgetHeader(
                title: "Finance",
                subtitle: "Flows by event date · balances as-of end",
                onViewAll: { onViewAll("FINANCE") }
            )
            OperatingV12TileGrid(
                theme: theme,
                metrics: metrics,
                columns: metrics.count >= 5 ? 3 : 2,
                currency: currency
            )
            OperatingV12Why(
                text: model.pulseGroup("FINANCE")?.why
                    ?? "revenue/purchase tax-exclusive; AR/AP GST-inclusive. Sale ≠ receipt — cash only from receipts/payments.",
                tone: .white
            )
        }
    }

    private var activityCard: some View {
        let group = model.pulseGroup("ACTIVITY")
        let rows = Array((model.activity ?? []).prefix(4))
        return OperatingV12Card(theme: theme, fill: theme.surface) {
            OperatingV12WidgetHeader(
                title: "Activity",
                subtitle: "Today · posted events only",
                onViewAll: { onViewAll("ACTIVITY") }
            )
            OperatingV12TileGrid(
                theme: theme,
                metrics: group?.metrics ?? [],
                columns: 3,
                currency: currency
            )
            if rows.isEmpty {
                Text("Nothing recorded yet.")
                    .font(.custom("PlusJakartaSans-Regular", size: 12))
                    .foregroundStyle(OperatingV12.subtitle)
            } else {
                VStack(alignment: .leading, spacing: 6) {
                    ForEach(rows) { row in
                        Button {
                            onOpenRow?(row)
                        } label: {
                            Text(activityLine(row))
                                .font(.custom("PlusJakartaSans-Medium", size: 12))
                                .foregroundStyle(OperatingV12.title)
                                .frame(maxWidth: .infinity, alignment: .leading)
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
        var parts = ["· \(row.title.isEmpty ? row.eventType : row.title)"]
        if model.financeVisible == true, let amount = row.amount, !amount.isEmpty {
            parts.append(OperatingV12Money.amount(amount, currency: currency))
        }
        if let cp = row.counterpartyName, !cp.isEmpty { parts.append(cp) }
        return parts.joined(separator: " · ")
    }

    private var gstCard: some View {
        let gst = model.gstPosition
        let difference = OperatingV12Money.parse(gst?.difference) ?? 0
        let differenceText = OperatingV12Money.amount(gst?.difference, currency: currency)
        return OperatingV12Card(theme: theme, fill: theme.surface) {
            OperatingV12WidgetHeader(
                title: "GST Position",
                subtitle: "Current month · As of \(OperatingV12Date.monthDayYear(model.endDate) ?? OperatingV12Date.today()) · GST enabled",
                onViewAll: { onViewAll("GST") }
            )
            HStack(spacing: 8) {
                OperatingV12Tile(
                    label: "GST on Sales",
                    value: OperatingV12Money.amount(gst?.outputGst, currency: currency),
                    tone: theme.accent
                )
                OperatingV12Tile(
                    label: "GST on Purchases",
                    value: OperatingV12Money.amount(gst?.inputGst, currency: currency),
                    tone: OperatingV12.cyan
                )
                OperatingV12Tile(
                    label: "Est. difference",
                    value: differenceText,
                    tone: difference == 0 ? OperatingV12.title : OperatingV12.red
                )
            }
            Text("Estimated difference: \(differenceText) payable before adjustments")
                .font(.custom("PlusJakartaSans-Medium", size: 12))
                .foregroundStyle(OperatingV12.subtitle)
            OperatingV12Why(
                text: model.pulseGroup("GST")?.why
                    ?? "Output GST − Input GST (indicative). Never label as government refund due. Hidden when GST disabled.",
                tone: .white
            )
        }
    }
}

// MARK: - Moments

struct OperatingV12MomentsScreen: View {
    let model: OperatingProjectionPayload
    let theme: OperatingV12Theme
    var onCapture: (String) -> Void
    var onViewAll: (String) -> Void
    var onOpenRow: ((OperatingActivityPayload) -> Void)? = nil

    private var title: String {
        let t = model.title?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        return t.isEmpty ? theme.label : t
    }

    private var network: OperatingBoardSectionPayload? {
        theme.isManufacturing ? model.sectionByCode("M3") : model.sectionByCode("network")
    }

    private var commitments: OperatingBoardSectionPayload? { model.sectionByCode("M4") }

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            OperatingV12Hero(
                theme: theme,
                eyebrow: "ACTIVE MOMENT",
                title: title,
                chips: [
                    OperatingV12ChipSpec(label: theme.label, tone: theme.accent),
                    OperatingV12ChipSpec(
                        label: (model.retired == true) ? "Books closed" : "Books open",
                        tone: OperatingV12.green,
                        dot: true
                    ),
                ],
                kpis: [
                    OperatingV12KpiSpec(
                        label: "People · \(theme.partyNoun)",
                        value: peopleCount ?? OperatingV12Metric.value(section: network, key: "buyers", currency: model.currency) ?? "—"
                    ),
                    OperatingV12KpiSpec(
                        label: "AR open",
                        value: OperatingV12Metric.value(section: commitments, key: "toCollect", currency: model.currency) ?? "—",
                        tone: theme.accent
                    ),
                ]
            )
            OperatingV12QuickActions(theme: theme, onOpen: onCapture)
            OperatingV12SectionCard(
                model: model, theme: theme, code: "M4",
                title: theme.isMadeToOrder ? "Open Jobs" : "Open Commitments",
                subtitle: "What is still open · not alarms",
                keys: ["toCollect", "toPay", "openOrders"],
                labels: [
                    "AR": "AR", "toCollect": "AR", "toPay": "AP",
                    "openOrders": theme.isMadeToOrder ? "Jobs" : "Open orders",
                ],
                onViewAll: onViewAll, onOpenRow: onOpenRow
            )
            OperatingV12SectionCard(
                model: model, theme: theme, code: "orders",
                title: theme.isMadeToOrder ? "Current Jobs" : (theme.isManufacturing ? "Work Orders & Commitments" : "Orders & Commitments"),
                subtitle: theme.isMadeToOrder
                    ? "Confirmed jobs · commitment only · not revenue"
                    : (theme.isManufacturing ? "Plant work orders · commitment only" : "Open buyer & supplier orders · commitment only"),
                keys: ["openOrders", "inTransit", "netSales"],
                onViewAll: onViewAll, onOpenRow: onOpenRow
            )
            if theme.isManufacturing {
                OperatingV12SectionCard(
                    model: model, theme: theme, code: "production",
                    title: theme.isMadeToOrder ? "Job Output" : "Production",
                    subtitle: theme.isMadeToOrder ? "Recorded vs accepted against open jobs" : "Recorded vs accepted plant output",
                    keys: ["recorded", "accepted", "pending"],
                    onViewAll: onViewAll, onOpenRow: onOpenRow
                )
            }
            OperatingV12SectionCard(
                model: model, theme: theme, code: "stock",
                title: theme.isMadeToOrder ? "Job Operations" : "Stock & Products",
                subtitle: theme.isMadeToOrder
                    ? "WIP against open jobs · materials & FG"
                    : (theme.isManufacturing ? "Material · WIP · finished goods" : "Physical movements · warehouse + routes"),
                keys: ["onHand", "stockMoves", "low"],
                onViewAll: onViewAll, onOpenRow: onOpenRow
            )
            if !theme.isManufacturing {
                OperatingV12SectionCard(
                    model: model, theme: theme, code: "network",
                    title: "People & Network",
                    subtitle: "Bound suppliers, buyers & routes",
                    keys: ["buyers", "suppliers", "channels"],
                    onViewAll: onViewAll, onOpenRow: onOpenRow
                )
            }
        }
    }

    private var peopleCount: String? {
        guard theme.isManufacturing else { return nil }
        let n = (model.participants ?? []).count
        if n > 0 { return "\(n)" }
        let rows = network?.rows?.count ?? 0
        return rows > 0 ? "\(rows)" : nil
    }
}

// MARK: - Life

struct OperatingV12LifeScreen: View {
    let model: OperatingProjectionPayload
    let theme: OperatingV12Theme
    var onCapture: (String) -> Void
    var onViewAll: (String) -> Void
    var onOpenRow: ((OperatingActivityPayload) -> Void)? = nil

    private var title: String {
        let t = model.title?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        return t.isEmpty ? theme.label : t
    }

    private var money: OperatingBoardSectionPayload? { model.sectionByCode("L3") }

    var body: some View {
        let openCount = (money?.rows ?? []).filter { row in
            ["OPEN", "PART", "OVERDUE", "DUE", "TRANSIT"].contains((row.badge ?? "").uppercased())
        }.count
        let doneCount = (money?.rows ?? []).filter { row in
            ["DONE", "SETTLED", "DELIVERED"].contains((row.badge ?? "").uppercased())
        }.count

        return VStack(alignment: .leading, spacing: 16) {
            OperatingV12QuickActions(theme: theme, onOpen: onCapture)
            OperatingV12Hero(
                theme: theme,
                eyebrow: "TODAY",
                title: "\(title) · today",
                titleSize: 22,
                chips: [
                    OperatingV12ChipSpec(label: "Today · \(OperatingV12Date.today())", tone: theme.accent),
                    OperatingV12ChipSpec(
                        label: (model.retired == true) ? "Books closed" : "Books open",
                        tone: OperatingV12.green,
                        dot: true
                    ),
                ],
                kpis: [
                    OperatingV12KpiSpec(label: "Open", value: "\(openCount)", tone: theme.accent),
                    OperatingV12KpiSpec(label: "Done", value: "\(doneCount)", tone: OperatingV12.green),
                    OperatingV12KpiSpec(
                        label: "Money in motion",
                        value: OperatingV12Metric.value(section: money, key: "net", currency: model.currency) ?? "—"
                    ),
                ]
            )
            OperatingV12SectionCard(
                model: model, theme: theme, code: "L1",
                title: theme.isMadeToOrder ? "Current Jobs Today" : (theme.isManufacturing ? "Current Production / Business" : "Current Business"),
                subtitle: "As-of today · \(OperatingV12Date.today())",
                keys: ["sales", "stockMoves", "issues"],
                onViewAll: onViewAll, onOpenRow: onOpenRow
            )
            OperatingV12SectionCard(
                model: model, theme: theme, code: "L2",
                title: theme.isMadeToOrder
                    ? "Jobs & Customers"
                    : (theme.isManufacturing ? "Orders / Buyers" : (theme.isTrading ? "Buyers & Channels" : "Dealers & Channels")),
                subtitle: theme.isMadeToOrder
                    ? "Attributed work by customer and channel"
                    : (theme.isManufacturing ? "Attributed plant sales by buyer and channel" : "Channel / territory mix · this month"),
                keys: ["channels", "buyers", "sales"],
                onViewAll: onViewAll, onOpenRow: onOpenRow
            )
            OperatingV12SectionCard(
                model: model, theme: theme, code: "L3",
                title: "Money Moving",
                subtitle: "Cash in / out · cash basis",
                keys: ["collected", "paid", "net"],
                onViewAll: onViewAll, onOpenRow: onOpenRow
            )
            OperatingV12SectionCard(
                model: model, theme: theme, code: "L4",
                title: theme.isManufacturing ? "Production Operations" : "People & Activity",
                subtitle: "Assigned follow-ups",
                keys: ["stockMoves", "issues", "updates"],
                onViewAll: onViewAll, onOpenRow: onOpenRow
            )
        }
    }
}

// MARK: - Memory

struct OperatingV12MemoryScreen: View {
    let model: OperatingProjectionPayload
    let theme: OperatingV12Theme
    var onCapture: (String) -> Void
    var onStory: () -> Void
    var onViewAll: (String) -> Void
    var onOpenRow: ((OperatingActivityPayload) -> Void)? = nil

    private var title: String {
        let t = model.title?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        return t.isEmpty ? theme.label : t
    }

    private var period: String { OperatingV12Date.monthYear(model.startDate) ?? "This month" }
    private var monthly: OperatingBoardSectionPayload? { model.sectionByCode("ME1") }
    private var money: OperatingBoardSectionPayload? { model.sectionByCode("ME2") }

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            OperatingV12QuickActions(theme: theme, onOpen: onCapture)
            OperatingV12Hero(
                theme: theme,
                eyebrow: "MEMORY · CLOSED PERIOD",
                title: "\(title) · memory",
                titleSize: 22,
                chips: [
                    OperatingV12ChipSpec(label: "\(period) · target v2", tone: theme.accent),
                    OperatingV12ChipSpec(label: "vs last period"),
                ],
                kpis: [
                    OperatingV12KpiSpec(
                        label: "Sales",
                        value: OperatingV12Metric.value(section: monthly, key: "netSales", currency: model.currency) ?? "—",
                        tone: .white
                    ),
                    OperatingV12KpiSpec(
                        label: "Records",
                        value: OperatingV12Metric.value(section: monthly, key: "records", currency: model.currency) ?? "—",
                        tone: OperatingV12.cyan
                    ),
                    OperatingV12KpiSpec(
                        label: "Collected",
                        value: OperatingV12Metric.value(section: money, key: "collected", currency: model.currency) ?? "—",
                        tone: theme.accent
                    ),
                ]
            )
            storyCta
            OperatingV12SectionCard(
                model: model, theme: theme, code: "ME1",
                title: "Monthly Memory",
                subtitle: theme.isMadeToOrder
                    ? "Closed periods · jobs & invoices (\(period))"
                    : (theme.isManufacturing ? "Closed periods · plant target v2 (\(period))" : "Closed periods · target v2 (\(period))"),
                keys: ["netSales", "months", "records"],
                onViewAll: onViewAll, onOpenRow: onOpenRow
            )
            OperatingV12SectionCard(
                model: model, theme: theme, code: "ME2",
                title: "Money Memory",
                subtitle: "Settled chains · FY 2026–27",
                keys: ["collected", "paid", "credits"],
                onViewAll: onViewAll, onOpenRow: onOpenRow
            )
            OperatingV12SectionCard(
                model: model, theme: theme, code: "fulfilment",
                title: theme.isMadeToOrder ? "Job Fulfilment Memory" : "Fulfilment Memory",
                subtitle: theme.isMadeToOrder
                    ? "Job → produce → invoice → dispatch"
                    : (theme.isManufacturing ? "Order → produce → dispatch" : "Order → dispatch → route delivery"),
                keys: ["delivered", "received", "returns"],
                onViewAll: onViewAll, onOpenRow: onOpenRow
            )
            OperatingV12SectionCard(
                model: model, theme: theme, code: "ME4",
                title: "People & Patterns",
                subtitle: "Repeat behaviour · 6 months",
                keys: ["issues", "approvals"],
                columns: 2,
                onViewAll: onViewAll, onOpenRow: onOpenRow
            )
        }
    }

    private var storyCta: some View {
        Button(action: onStory) {
            VStack(alignment: .leading, spacing: 2) {
                Image(systemName: "sparkles")
                    .foregroundStyle(OperatingV12.deepInk)
                    .padding(.bottom, 2)
                Text("Generate Business Story")
                    .font(.custom("PlusJakartaSans-ExtraBold", size: 15))
                    .foregroundStyle(OperatingV12.deepInk)
                Text("Eight pages from recorded activity · \(period) preselected")
                    .font(.custom("PlusJakartaSans-Medium", size: 11))
                    .foregroundStyle(OperatingV12.deepInk.opacity(0.8))
            }
            .padding(.horizontal, 18)
            .padding(.vertical, 14)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(theme.accent)
            .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
            .shadow(color: theme.accent.opacity(0.35), radius: 12, y: 4)
        }
        .buttonStyle(.plain)
    }
}

// MARK: - Shared section card

struct OperatingV12SectionCard: View {
    let model: OperatingProjectionPayload
    let theme: OperatingV12Theme
    let code: String
    let title: String
    let subtitle: String
    let keys: [String]
    var labels: [String: String] = [:]
    var columns: Int = 3
    var onViewAll: (String) -> Void
    var onOpenRow: ((OperatingActivityPayload) -> Void)?

    var body: some View {
        let section = model.sectionByCode(code)
        let ordered = OperatingV12Tiles.ordered(
            section: section, keys: keys, limit: columns, currency: model.currency
        )
        let rows = Array((section?.rows ?? []).prefix(3))
        let cardFill = theme.isTrading ? theme.surface : OperatingV12.surface
        let gridColumns = ordered.count == 2 ? 2 : columns

        return OperatingV12Card(theme: theme, fill: cardFill) {
            OperatingV12WidgetHeader(title: title, subtitle: subtitle, onViewAll: { onViewAll(code) })
            if !ordered.isEmpty {
                let width = max(gridColumns, 1)
                let chunked = stride(from: 0, to: ordered.count, by: width).map {
                    Array(ordered[$0..<min($0 + width, ordered.count)])
                }
                VStack(spacing: 8) {
                    ForEach(Array(chunked.enumerated()), id: \.offset) { _, row in
                        HStack(spacing: 8) {
                            ForEach(row) { metric in
                                OperatingV12Tile(
                                    label: labels[metric.key] ?? metric.label,
                                    value: OperatingV12Money.amount(metric.value, currency: model.currency),
                                    tone: OperatingV12.tone(metric.tone, key: metric.key, accent: theme.accent)
                                )
                            }
                            ForEach(0..<(width - row.count), id: \.self) { _ in
                                Color.clear.frame(maxWidth: .infinity)
                            }
                        }
                    }
                }
            }
            if rows.isEmpty {
                Text("Not recorded yet.")
                    .font(.custom("PlusJakartaSans-Regular", size: 12))
                    .foregroundStyle(OperatingV12.subtitle)
            } else {
                VStack(spacing: 6) {
                    ForEach(rows) { row in
                        OperatingV12Row(
                            title: row.title,
                            subtitle: row.subtitle,
                            trailing: rowTrailing(row),
                            chip: row.badge,
                            bar: OperatingV12.bar(row.badge),
                            accent: theme.accent,
                            onClick: onOpenRow.map { handler in { handler(row) } }
                        )
                    }
                }
            }
            OperatingV12Why(text: section?.why)
        }
    }

    private func rowTrailing(_ row: OperatingActivityPayload) -> String? {
        if model.financeVisible == true, let amount = row.amount, !amount.isEmpty {
            return OperatingV12Money.amount(amount, currency: model.currency)
        }
        if let qty = row.quantity, !qty.isEmpty {
            return [qty, row.unit].compactMap { $0 }.joined(separator: " ")
        }
        return nil
    }
}

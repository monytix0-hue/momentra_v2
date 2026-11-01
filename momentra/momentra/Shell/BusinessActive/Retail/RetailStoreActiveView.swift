import SwiftUI

/// Retail store active experience.
///
/// Layout + palette come from Figma `2487:37758`:
/// RTL-PULSE-v12 `2399:29655`, RTL-HUB-v5 `2399:29887`,
/// RTL-MOMENTS-v12 `2399:30656`, RTL-LIFE-v12 `2399:30816`, RTL-MEMORY-v12 `2399:30979`.
///
/// Every number is read from the operating projection. A metric the backend cannot supply
/// renders `Unavailable` — never a fabricated `0` or `0%` (honesty rules in the plan).
struct RetailStoreActiveView: View {
    let model: OperatingProjectionPayload
    let tab: OperatingTab
    var onOpenQuickAdd: () -> Void = {}
    var onRecord: (RetailEventDraft) async throws -> Void = { _ in }
    var onDecide: (String, String) async throws -> Void = { _, _ in }
    var momentId: String? = nil
    var onViewAllAttention: () -> Void = {}

    @State private var sheet: RetailSheetRoute?

    var body: some View {
        RoseScreenBackground {
            if tab == .quickAdd {
                RetailQuickAddHub(
                    model: model,
                    momentId: momentId,
                    onRecord: onRecord,
                    onDecide: onDecide
                )
            } else {
                ScrollView {
                    VStack(alignment: .leading, spacing: RetailRose.sectionGap) {
                        RetailScreenHeader(
                            title: tabTitle,
                            roleLine: roleLine
                        )
                        RoseContextChips(
                            subtypeLabel: model.subtypeLabel ?? "Retail Store",
                            periodLine: RetailFormat.periodLine(model.startDate, currency: model.currency),
                            gstOn: model.gstTracking == "YES"
                        )
                        switch tab {
                        case .pulse: pulse
                        case .moments: moments
                        case .life: life
                        case .memory: memory
                        case .quickAdd: EmptyView()
                        }
                        Spacer(minLength: 24)
                    }
                    .padding(.horizontal, RetailRose.gutter)
                    .padding(.vertical, 16)
                }
            }
        }
        .sheet(item: $sheet) { route in
            if route == .storyConfig, let momentId {
                RetailStoryConfigSheet(model: model, momentId: momentId) { sheet = nil }
            } else {
                RetailBoardSheetView(route: route, model: model) {
                    sheet = nil
                }
            }
        }
    }

    private var roleLine: String {
        let role = roleLabel(model.role)
        let party = (model.partyName ?? "").trimmingCharacters(in: .whitespaces)
        let subtype = model.subtypeLabel ?? "Retail Store"
        return [role, party].filter { !$0.isEmpty }.joined(separator: " ") + " · \(subtype)"
    }

    private var tabTitle: String {
        let name = (model.title ?? "").isEmpty ? "Retail Store" : model.title!
        switch tab {
        case .pulse: return "\(name) Pulse"
        case .moments: return "Moments"
        case .life: return "Life"
        case .memory: return "Memory"
        case .quickAdd: return "Quick Adds"
        }
    }

    // MARK: - Pulse — RTL-PULSE-v12 `2399:29655`

    private var pulse: some View {
        VStack(alignment: .leading, spacing: RetailRose.sectionGap) {
            pulseHero
            attentionCard
            targetBudgetCard
            paymentReadinessCard
            financeCard
            activityCard
            if model.gstTracking == "YES" { gstCard }
        }
    }

    /// Rose hero: eyebrow, promise line, one-line position + 40pt glyph well.
    private var pulseHero: some View {
        let alerts = (model.attention ?? []).count
        let receivable = RetailFormat.compact(model.finance?.outstanding, currency: model.currency, visible: model.financeVisible == true)
        let sales = RetailFormat.compact(
            model.finance?.sales ?? model.target?.progress,
            currency: model.currency,
            visible: model.financeVisible == true
        )
        let target = model.target?.configured == true
            ? RetailFormat.compact(model.target?.value, currency: model.currency, visible: model.financeVisible == true)
            : nil
        let summary = (target != nil ? "Sales \(sales) / \(target!)" : "Sales \(sales)")
            + " · Stock alerts \(alerts) · AR \(receivable) open"

        return ZStack(alignment: .topTrailing) {
            VStack(alignment: .leading, spacing: 6) {
                Text(
                    [(model.title ?? "Retail Store").uppercased(), model.location?.uppercased()]
                        .compactMap { $0 }
                        .filter { !$0.isEmpty }
                        .joined(separator: " · ")
                )
                .font(.plusJakarta(size: 11, weight: .bold))
                .foregroundStyle(RetailRose.roseMuted)
                .lineLimit(1)

                Text("Stock. Sell. Settle.")
                    .font(.plusJakarta(size: 22, weight: .bold))
                    .foregroundStyle(.white)
                    .lineLimit(1)

                Text(summary)
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(RetailRose.roseSoft)
                    .lineLimit(2)
                    .padding(.trailing, 56)

                if (model.quickAdds ?? []).contains(where: { $0.code == "SALE" }) {
                    RoseCta(label: "+  Record Sale", plus: false, onTap: onOpenQuickAdd)
                        .padding(.top, 4)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            heroGlyph
        }
        .padding(20)
        .background(RetailRose.rose)
        .clipShape(RoundedRectangle(cornerRadius: RetailRose.rHero, style: .continuous))
        .shadow(color: RetailRose.indigo.opacity(0.5), radius: 16, x: 0, y: 12)
        .shadow(color: RetailRose.rose.opacity(0.3), radius: 32, x: 0, y: 24)
    }

    /// 40pt rose-tinted glyph well pinned to the hero's top-right.
    private var heroGlyph: some View {
        ZStack {
            RoundedRectangle(cornerRadius: RetailRose.rHeroIcon, style: .continuous)
                .fill(RetailRose.surfaceRow.opacity(0.9))
            RoundedRectangle(cornerRadius: RetailRose.rHeroIcon, style: .continuous)
                .stroke(RetailRose.rose, lineWidth: 1)
            Image("RetailHeroGlyph")
                .renderingMode(.template)
                .resizable()
                .scaledToFit()
                .frame(width: 20, height: 20)
                .foregroundStyle(RetailRose.rose)
        }
        .frame(width: 40, height: 40)
        .shadow(color: RetailRose.rose.opacity(0.5), radius: 6, x: 0, y: 4)
    }

    /// AN-P01 · Needs Attention `2399:29710`
    private var attentionCard: some View {
        let items = model.attention ?? []
        let lowStock = items.filter { $0.kind.uppercased().hasPrefix("LOW_STOCK") }.count
        return RoseCard {
            RoseSectionHeader(
                title: "Needs Attention",
                chipText: items.isEmpty ? nil : "Alert",
                onViewAll: { sheet = .attention }
            )
            HStack(spacing: 8) {
                MetricChip(label: "Alerts", value: "\(items.count)", valueColor: RetailRose.rose)
                MetricChip(label: "Reorder", value: "\(lowStock)", valueColor: RetailRose.amber)
            }
            if items.isEmpty {
                RoseEmpty(title: "Nothing is waiting", body: "Approvals, overdue items and blockers will show here.")
            } else {
                VStack(spacing: 8) {
                    ForEach(items.prefix(3)) { item in
                        attentionRow(item)
                    }
                }
            }
            RoseWhyCaption(body: "Each alert links to Stock Update, Collection or Issue. Quick amount sales never invent stock movement.")
        }
    }

    private func attentionRow(_ item: OperatingAttentionPayload) -> some View {
        let hasSource = item.sourceEventId != nil || item.id.hasPrefix("open-") || item.cta != nil
        let label = retailSeverityLabel(kind: item.kind, severity: item.severity, hasSource: hasSource)
        let color = hasSource ? retailSeverityColor(item.severity) : RetailRose.warning
        let caption = [
            item.counterpartyName,
            RetailFormat.shortDay(item.dueAt).map { "Due \($0)" },
            RetailFormat.compact(item.amount, currency: model.currency, visible: model.financeVisible == true)
                .takeUnless("—")
        ]
        .compactMap { $0 }
        .filter { !$0.isEmpty }
        .joined(separator: " · ")

        return AccentRow(
            title: item.title.isEmpty ? "Attention item" : item.title,
            caption: caption.isEmpty ? "No detail recorded" : caption,
            accent: color,
            severityLabel: label,
            severityColor: color,
            onTap: {
                openOperatingAttention(item, model: model, onOpenQuickAdd: onOpenQuickAdd)
            }
        )
    }

    /// AN-P02 · Target & Budget `2399:29677`
    private var targetBudgetCard: some View {
        let target = model.target
        let budget = model.budget
        let configured = target?.configured == true
        let percentText = configured ? (RetailFormat.percent(target?.progress, target?.value) ?? "Unavailable") : "Unavailable"
        let fraction = configured ? RetailFormat.fraction(target?.progress, target?.value) : nil
        let sales = RetailFormat.compact(target?.progress ?? model.finance?.sales, currency: model.currency, visible: model.financeVisible == true)
        let targetValue = configured ? RetailFormat.compact(target?.value, currency: model.currency, visible: model.financeVisible == true) : nil
        let budgetPercent = budget?.configured == true
            ? (RetailFormat.percent(budget?.spent, budget?.amount) ?? "Unavailable")
            : "Unavailable"

        return RoseCard {
            RoseSectionHeader(title: "Target & Budget", onViewAll: { sheet = .target })
            HStack(spacing: 16) {
                HealthRing(percentLabel: percentText, caption: "of target", fraction: fraction)
                VStack(alignment: .leading, spacing: 6) {
                    Text("ACHIEVEMENT VS TARGET")
                        .font(.plusJakarta(size: 10, weight: .bold))
                        .tracking(0.6)
                        .foregroundStyle(RetailRose.rose)
                        .lineLimit(1)
                    Text(configured && targetValue != nil
                         ? "Sales \(sales) of \(targetValue!)"
                         : "Sales \(sales) · no target set")
                        .font(.plusJakarta(size: 15, weight: .bold))
                        .foregroundStyle(RetailRose.textPrimary)
                        .fixedSize(horizontal: false, vertical: true)
                    statusPillForTarget(fraction)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            }
            .padding(14)
            .background(RetailRose.surfaceRow)
            .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .stroke(RetailRose.rose.opacity(0.22), lineWidth: 1)
            )
            HStack(spacing: 8) {
                MetricChip(label: "Sales", value: sales)
                MetricChip(
                    label: "Budget use",
                    value: budgetPercent,
                    valueColor: budgetPercent == "Unavailable" ? RetailRose.warning : RetailRose.textPrimary
                )
            }
            VStack(spacing: 0) {
                DividedRow(
                    title: "Sales vs target",
                    caption: "Effective \(RetailFormat.month(model.startDate)) · store",
                    trailingValue: configured && targetValue != nil ? "\(sales) / \(targetValue!)" : "Unavailable",
                    trailingColor: configured ? RetailRose.textPrimary : RetailRose.warning
                )
                DividedRow(
                    title: "Eligible spend",
                    caption: "COGS + running",
                    trailingValue: RetailFormat.compact(model.finance?.eligibleCost, currency: model.currency, visible: model.financeVisible == true)
                )
                DividedRow(
                    title: "Footfall attributed",
                    caption: (target?.coverage?.isEmpty == false) ? target!.coverage! : "Coverage unavailable",
                    trailingValue: (target?.coverage?.isEmpty == false) ? target!.coverage! : "Unavailable",
                    trailingColor: (target?.coverage?.isEmpty == false) ? RetailRose.textPrimary : RetailRose.warning,
                    captionColor: (target?.coverage?.isEmpty == false) ? RetailRose.textBody : RetailRose.warning
                )
            }
            RoseWhyCaption(body: "Achievement = eligible actual ÷ effective target. Missing coverage shows Unavailable — never invent 0%.")
        }
    }

    @ViewBuilder
    private func statusPillForTarget(_ fraction: Double?) -> some View {
        if let fraction {
            if fraction < 0.5 {
                StatusPill(label: "● Behind", color: RetailRose.negative)
            } else if fraction >= 0.75 {
                StatusPill(label: "● On track", color: RetailRose.positive)
            } else {
                StatusPill(label: "● In progress", color: RetailRose.amber)
            }
        } else {
            StatusPill(label: "● Unavailable", color: RetailRose.warning)
        }
    }

    /// AN-P03 · Payment Readiness `2399:29742`
    private var paymentReadinessCard: some View {
        let all = model.paymentReadiness ?? []
        let payables = all.filter { $0.relevance != "RECEIVABLE" }
        let paid = all
            .filter { $0.relevance != "RECEIVABLE" && ["PAID", "SETTLED"].contains($0.status) }
            .reduce(Decimal(0)) { $0 + (Decimal(string: $1.amount) ?? 0) }
        let paidText = paid > 0
            ? RetailFormat.compact("\(paid)", currency: model.currency, visible: model.financeVisible == true)
            : RetailFormat.compact(model.finance?.cashPaid, currency: model.currency, visible: model.financeVisible == true)

        return RoseCard {
            RoseSectionHeader(title: "Payment Readiness", onViewAll: { sheet = .payment })
            HStack(spacing: 8) {
                MetricChip(
                    label: "AP open",
                    value: RetailFormat.compact(model.finance?.payable, currency: model.currency, visible: model.financeVisible == true),
                    valueColor: RetailRose.amber
                )
                MetricChip(label: "Paid", value: paidText, valueColor: RetailRose.positive)
            }
            if payables.isEmpty {
                RoseEmpty(title: "No open payments", body: "Purchases and expenses waiting to settle will show here.")
            } else {
                VStack(spacing: 0) {
                    ForEach(payables.prefix(3)) { row in
                        DividedRow(
                            title: row.title.isEmpty ? (row.counterpartyName ?? "Open payable") : row.title,
                            caption: paymentCaption(row),
                            trailingValue: RetailFormat.compact(row.amount, currency: model.currency, visible: model.financeVisible == true),
                            trailingColor: row.dueBucket == "OVERDUE" ? RetailRose.negative : RetailRose.amber,
                            captionColor: row.dueBucket == "OVERDUE" ? RetailRose.negative : RetailRose.textBody,
                            onTap: { OperatingChromeState.shared.settling = row }
                        )
                    }
                }
            }
            RoseWhyCaption(body: "Purchase ≠ payment. Payment settles open payable only.")
        }
    }

    private func paymentCaption(_ row: OperatingPaymentPayload) -> String {
        let parts = [
            row.counterpartyName,
            RetailFormat.shortDay(row.dueAt).map { "Due \($0)" }
        ].compactMap { $0 }.filter { !$0.isEmpty }
        if !parts.isEmpty { return parts.joined(separator: " · ") }
        return row.status.lowercased().capitalized
    }

    /// AN-P04 · Finance `2399:29772`
    private var financeCard: some View {
        let receivables = (model.paymentReadiness ?? []).filter { $0.relevance == "RECEIVABLE" }
        return RoseCard {
            RoseSectionHeader(title: "Finance", onViewAll: { sheet = .finance })
            HStack(spacing: 8) {
                MetricChip(
                    label: "Sales",
                    value: RetailFormat.compact(model.finance?.sales, currency: model.currency, visible: model.financeVisible == true)
                )
                MetricChip(
                    label: "Collected",
                    value: RetailFormat.compact(model.finance?.received, currency: model.currency, visible: model.financeVisible == true),
                    valueColor: RetailRose.positive
                )
                MetricChip(
                    label: "AR",
                    value: RetailFormat.compact(model.finance?.outstanding, currency: model.currency, visible: model.financeVisible == true),
                    valueColor: RetailRose.rose
                )
            }
            if model.financeVisible != true {
                RoseEmpty(title: "Totals stay private", body: "Finance, Approver and Owner see the money position.")
            } else if receivables.isEmpty {
                RoseEmpty(title: "Nothing to collect", body: "Credit sales waiting on collection will show here.")
            } else {
                VStack(spacing: 0) {
                    ForEach(receivables.prefix(3)) { row in
                        DividedRow(
                            title: (row.counterpartyName ?? row.title).isEmpty ? "Open receivable" : (row.counterpartyName ?? row.title),
                            caption: RetailFormat.shortDay(row.dueAt).map { "Due \($0)" } ?? "Still open",
                            trailingValue: RetailFormat.compact(row.amount, currency: model.currency, visible: true),
                            trailingColor: row.dueBucket == "OVERDUE" ? RetailRose.negative : RetailRose.rose,
                            captionColor: row.dueBucket == "OVERDUE" ? RetailRose.negative : RetailRose.textBody,
                            onTap: { OperatingChromeState.shared.settling = row }
                        )
                    }
                }
            }
            RoseWhyCaption(body: "Posted sale creates revenue. Credit needs a named customer. Collection allocates AR — never new sales.")
        }
    }

    /// AN-P05 · Activity `2399:29805`
    private var activityCard: some View {
        let activity = model.activity ?? []
        let sales = activity.filter { $0.eventType == "SALE" }.count
        let purchases = activity.filter { $0.eventType == "PURCHASE" }.count
        let stockMoves = activity.filter { $0.eventType == "STOCK" }.count
        return RoseCard {
            RoseSectionHeader(title: "Activity", onViewAll: { sheet = .activity })
            HStack(spacing: 8) {
                MetricChip(label: "Sales", value: "\(sales)")
                MetricChip(label: "Purchases", value: "\(purchases)")
                MetricChip(label: "Stock moves", value: "\(stockMoves)")
            }
            if activity.isEmpty {
                RoseEmpty(title: "Nothing recorded yet", body: "Sales, purchases and stock moves will show here.")
            } else {
                VStack(spacing: 0) {
                    ForEach(activity.prefix(3)) { row in
                        activityDividedRow(row)
                    }
                }
            }
            RoseWhyCaption(body: "Stock changes only on explicit receipt, sale fulfilment or adjustment — never from billing alone.")
        }
    }

    private func activityDividedRow(_ row: OperatingActivityPayload) -> some View {
        let caption = activityCaption(row)
        let trailing = activityTrailing(row)
        return DividedRow(
            title: row.title.isEmpty ? retailEventLabel(row.eventType) : row.title,
            caption: caption,
            trailingValue: trailing,
            trailingColor: activityTrailingColor(trailing),
            onTap: { OperatingChromeState.shared.editing = row }
        )
    }

    private func activityCaption(_ row: OperatingActivityPayload) -> String {
        var parts: [String] = []
        if let who = row.counterpartyName, !who.isEmpty {
            parts.append(who)
        } else if let channel = row.channelName, !channel.isEmpty {
            parts.append(channel)
        } else if let note = row.note, !note.isEmpty {
            parts.append(String(note.prefix(48)))
        } else {
            parts.append(retailEventLabel(row.eventType))
        }
        if let quantity = row.quantity, !quantity.isEmpty {
            parts.append(row.unit.map { "\(quantity) \($0)" } ?? quantity)
        }
        return parts.joined(separator: " · ")
    }

    private func activityTrailing(_ row: OperatingActivityPayload) -> String {
        if row.approvalStatus == "PENDING" { return "Pending" }
        if let status = row.paymentStatus, ["COLLECTED", "PAID", "SETTLED"].contains(status) { return "Done" }
        if let status = row.status, !status.isEmpty { return status.lowercased().capitalized }
        return "Open"
    }

    private func activityTrailingColor(_ trailing: String) -> Color {
        switch trailing {
        case "Done": return RetailRose.positive
        case "Pending": return RetailRose.amber
        default: return RetailRose.textBody
        }
    }

    /// AN-P06 · GST Position `2399:29838`
    private var gstCard: some View {
        let gst = model.gstPosition
        let complete = gst?.outputGst?.isEmpty == false && gst?.inputGst?.isEmpty == false
        return RoseCard {
            RoseSectionHeader(title: "GST Position", onViewAll: { sheet = .gst })
            HStack(spacing: 8) {
                MetricChip(label: "Output", value: RetailFormat.compact(gst?.outputGst, currency: model.currency))
                MetricChip(label: "ITC", value: RetailFormat.compact(gst?.inputGst, currency: model.currency), valueColor: RetailRose.positive)
                MetricChip(label: "Net", value: RetailFormat.compact(gst?.difference, currency: model.currency), valueColor: RetailRose.amber)
            }
            if !complete {
                RoseEmpty(title: "Registration incomplete", body: "GST stays Unavailable until the registration is complete.")
            } else {
                VStack(spacing: 0) {
                    let sources = gst?.sources ?? []
                    if sources.isEmpty {
                        DividedRow(
                            title: "Outward taxable",
                            caption: "Posted sales",
                            trailingValue: RetailFormat.compact(model.finance?.sales, currency: model.currency, visible: true)
                        )
                        DividedRow(
                            title: "Inward eligible",
                            caption: "Posted purchases",
                            trailingValue: RetailFormat.compact(model.finance?.eligibleCost, currency: model.currency, visible: true)
                        )
                    } else {
                        ForEach(sources.prefix(3)) { source in
                            DividedRow(
                                title: (source.title?.isEmpty == false) ? source.title! : retailEventLabel(source.eventType ?? ""),
                                caption: nil,
                                trailingValue: RetailFormat.compact(source.gstTotal, currency: model.currency),
                                trailingColor: RetailRose.positive
                            )
                        }
                    }
                    DividedRow(
                        title: "Net payable",
                        caption: RetailFormat.asOf(gst?.asOfMonth.map { "\($0)-01" }),
                        trailingValue: RetailFormat.compact(gst?.difference, currency: model.currency),
                        trailingColor: RetailRose.amber
                    )
                }
            }
            RoseWhyCaption(body: "GST only when enabled. Incomplete registration → Unavailable. Indicative only — not a filing.")
        }
    }

    // MARK: - Moments — RTL-MOMENTS-v12 `2399:30656`

    private var moments: some View {
        VStack(alignment: .leading, spacing: RetailRose.sectionGap) {
            momentsHero
            commitmentsCard
            myBusinessCard
            peopleCard
            networkCard
            RoseCard(tone: .board, onTap: onOpenQuickAdd) {
                HStack(spacing: 10) {
                    sheetIconWell(RetailTileIcons.sale)
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Record the next store moment")
                            .font(.plusJakarta(size: 16, weight: .bold))
                            .foregroundStyle(RetailRose.textPrimary)
                            .fixedSize(horizontal: false, vertical: true)
                        Text("Sale, purchase, collection or adjustment.")
                            .font(.plusJakarta(size: 11))
                            .foregroundStyle(RetailRose.textBody)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    Text("›")
                        .font(.plusJakarta(size: 16, weight: .bold))
                        .foregroundStyle(RetailRose.rose)
                }
            }
        }
    }

    private var momentsHero: some View {
        let staff = (model.participants ?? []).count
        let suppliers = (model.masters ?? []).filter { $0.kind == "SUPPLIER" }.count
        return BoardHero(
            eyebrow: "ACTIVE MOMENT",
            headline: (model.title?.isEmpty == false) ? model.title! : "Retail Store",
            chips: {
                HStack(spacing: 8) {
                    BoardHeroChip(text: model.subtypeLabel ?? "Retail", color: RetailRose.rose)
                    if let location = model.location, !location.isEmpty {
                        BoardHeroChip(text: location, color: RetailRose.positive, dot: true)
                    }
                }
            },
            kpis: {
                HStack(spacing: 8) {
                    KpiChip(label: "People · Staff", value: "\(staff)")
                    KpiChip(label: "Network · Suppliers", value: "\(suppliers)")
                    KpiChip(
                        label: "AP open",
                        value: RetailFormat.compact(model.finance?.payable, currency: model.currency, visible: model.financeVisible == true),
                        valueColor: RetailRose.amber
                    )
                }
            }
        )
    }

    /// AN-M04 · Open Commitments `2399:30767`
    private var commitmentsCard: some View {
        let openOrders = (model.activity ?? []).filter {
            ($0.eventType == "ORDER" || $0.eventType == "CUSTOMER_ORDER") &&
            !["COMPLETED", "CANCELLED"].contains($0.status ?? "")
        }.count
        let rows = ((model.boards?.moments ?? []).first { $0.code == "M4" }?.rows) ?? []
        return RoseCard(tone: .board, strokeAlpha: 0.40) {
            RoseSectionHeader(title: "Open Commitments", onViewAll: { sheet = .commitments })
            HStack(spacing: 8) {
                KpiChip(
                    label: "AP",
                    value: RetailFormat.compact(model.finance?.payable, currency: model.currency, visible: model.financeVisible == true),
                    valueColor: RetailRose.amber
                )
                KpiChip(
                    label: "AR",
                    value: RetailFormat.compact(model.finance?.outstanding, currency: model.currency, visible: model.financeVisible == true),
                    valueColor: RetailRose.rose
                )
                KpiChip(label: "Orders", value: "\(openOrders)")
            }
            if rows.isEmpty {
                RoseEmpty(title: "Nothing open", body: "Orders, payables and receivables will show here.")
            } else {
                VStack(spacing: 8) {
                    ForEach(rows.prefix(3)) { row in
                        boardRowFor(row)
                    }
                }
            }
            RoseWhyNote(body: "Orders are commitments — revenue only on posted sale.")
        }
    }

    /// AN-M01 · My Retail Business `2399:30674`
    private var myBusinessCard: some View {
        let retired = model.retired == true
        let targetConfigured = model.target?.configured == true
        let gstValue: String
        if model.gstTracking != "YES" {
            gstValue = "Not tracked"
        } else if (model.gstRegistration ?? "").isEmpty {
            gstValue = "Registration incomplete"
        } else {
            gstValue = "Registered\((model.gstState?.isEmpty == false) ? " · \(model.gstState!)" : "")"
        }
        return RoseCard(tone: .board) {
            RoseSectionHeader(title: "My Retail Business", onViewAll: { sheet = .momentDetail })
            HStack(spacing: 8) {
                KpiChip(label: "Status", value: retired ? "Closed" : "Live", valueColor: retired ? RetailRose.textMuted : RetailRose.positive)
                KpiChip(
                    label: "Channel",
                    value: (model.life?.channels.first).flatMap { $0.isEmpty ? nil : $0 } ?? "Unavailable"
                )
                KpiChip(label: "Day", value: dayOfPeriod)
            }
            VStack(spacing: 0) {
                detailRow(
                    eyebrow: "Business",
                    value: [model.title, model.location].compactMap { $0 }.filter { !$0.isEmpty }.joined(separator: " · "),
                    trailing: retired ? "Closed" : "Live",
                    trailingColor: retired ? RetailRose.textMuted : RetailRose.positive
                )
                detailRow(
                    eyebrow: "Plan",
                    value: targetConfigured
                        ? "Sales \(RetailFormat.compact(model.target?.value, currency: model.currency, visible: model.financeVisible == true)) target"
                        : "No target set",
                    trailing: targetConfigured ? "Active" : "Unavailable",
                    trailingColor: targetConfigured ? RetailRose.textPrimary : RetailRose.warning
                )
                detailRow(
                    eyebrow: "GST",
                    value: gstValue,
                    trailing: model.gstTracking == "YES" ? "On" : "Off",
                    trailingColor: RetailRose.amber
                )
            }
            RoseWhyNote(body: "One Moment holds catalogue, stock, sales and money. Quick amount never invents SKU movement.")
        }
    }

    /// AN-M02 · People `2399:30707`
    private var peopleCard: some View {
        let staff = model.participants ?? []
        let canApprove = model.role == "OWNER_ADMIN" || model.role == "APPROVER"
        return RoseCard(tone: .board) {
            RoseSectionHeader(title: "People", onViewAll: { sheet = .people })
            HStack(spacing: 8) {
                KpiChip(label: "Staff", value: "\(staff.count)")
                KpiChip(
                    label: "Approvers",
                    value: canApprove ? "1" : "Unavailable",
                    valueColor: canApprove ? RetailRose.amber : RetailRose.warning
                )
            }
            if staff.isEmpty {
                RoseEmpty(title: "No people yet", body: "Invite the shop floor, billing and approvers.")
            } else {
                VStack(spacing: 0) {
                    ForEach(staff.prefix(3), id: \.self) { name in
                        detailRow(
                            eyebrow: name,
                            value: name == model.partyName ? roleLabel(model.role) : "Member",
                            trailing: name == model.partyName ? "Owner" : "Member",
                            trailingColor: name == model.partyName ? RetailRose.textPrimary : RetailRose.textBody
                        )
                    }
                }
            }
            RoseWhyNote(body: "Roles gate credit sales, stock adjustments and payments.")
        }
    }

    /// AN-M03 · Business Network `2399:30737`
    private var networkCard: some View {
        let masters = model.masters ?? []
        let suppliers = masters.filter { $0.kind == "SUPPLIER" }
        let buyers = masters.filter { ["CUSTOMER", "BUYER"].contains($0.kind) }
        let payables = (model.paymentReadiness ?? []).filter { $0.relevance != "RECEIVABLE" }
        let receivables = (model.paymentReadiness ?? []).filter { $0.relevance == "RECEIVABLE" }
        var combined: [(String, Bool)] = []
        for supplier in suppliers where !combined.contains(where: { $0.0 == supplier.name }) {
            combined.append((supplier.name, true))
        }
        for buyer in buyers where !combined.contains(where: { $0.0 == buyer.name }) {
            combined.append((buyer.name, false))
        }

        return RoseCard(tone: .board) {
            RoseSectionHeader(title: "Business Network", onViewAll: { sheet = .network })
            HStack(spacing: 8) {
                KpiChip(label: "Suppliers", value: "\(suppliers.count)")
                KpiChip(label: "Credit buyers", value: "\(buyers.count)")
            }
            if combined.isEmpty {
                RoseEmpty(title: "No network yet", body: "Suppliers and credit buyers you record will show here.")
            } else {
                VStack(spacing: 0) {
                    ForEach(combined.prefix(3), id: \.0) { pair in
                        let openAp = payables.filter { $0.counterpartyName?.caseInsensitiveCompare(pair.0) == .orderedSame }
                            .reduce(Decimal(0)) { $0 + (Decimal(string: $1.amount) ?? 0) }
                        let openAr = receivables.filter { $0.counterpartyName?.caseInsensitiveCompare(pair.0) == .orderedSame }
                            .reduce(Decimal(0)) { $0 + (Decimal(string: $1.amount) ?? 0) }
                        let trailing: String
                        let color: Color
                        if pair.1 && openAp > 0 {
                            trailing = "AP \(RetailFormat.compact("\(openAp)", currency: model.currency, visible: model.financeVisible == true))"
                            color = RetailRose.amber
                        } else if !pair.1 && openAr > 0 {
                            trailing = "AR \(RetailFormat.compact("\(openAr)", currency: model.currency, visible: model.financeVisible == true))"
                            color = RetailRose.rose
                        } else {
                            trailing = "No balance"
                            color = RetailRose.textMuted
                        }
                        detailRow(
                            eyebrow: pair.0,
                            value: pair.1 ? "Supplier" : "Credit buyer",
                            trailing: trailing,
                            trailingColor: color
                        )
                    }
                }
            }
            RoseWhyNote(body: "Named customer required for unpaid/credit charge.")
        }
    }

    // MARK: - Life — RTL-LIFE-v12 `2399:30816`

    private var life: some View {
        let open = model.life?.openCommitments ?? 0
        let done = (model.activity ?? []).filter { ["COLLECTED", "PAID", "SETTLED"].contains($0.paymentStatus ?? "") }.count
        let sections = model.boards?.life ?? []
        return VStack(alignment: .leading, spacing: RetailRose.sectionGap) {
            BoardHero(
                eyebrow: "TODAY",
                headline: "\((model.title?.isEmpty == false) ? model.title! : "Retail Store") · today",
                strokeAlpha: 0.45,
                chips: {
                    HStack(spacing: 8) {
                        BoardHeroChip(text: RetailFormat.todayLine(), color: RetailRose.rose)
                        BoardHeroChip(
                            text: model.retired == true ? "Store closed" : "Store open",
                            color: model.retired == true ? RetailRose.textMuted : RetailRose.positive,
                            dot: true
                        )
                    }
                },
                kpis: {
                    HStack(spacing: 8) {
                        KpiChip(label: "Open", value: "\(open)", valueColor: RetailRose.rose)
                        KpiChip(label: "Done", value: "\(done)", valueColor: RetailRose.positive)
                        KpiChip(
                            label: "Money in motion",
                            value: RetailFormat.compact(model.finance?.received, currency: model.currency, visible: model.financeVisible == true)
                        )
                    }
                }
            )
            lifeCurrentCard(sections.first { $0.code == "L1" || $0.code == "L2" })
            lifeSalesCard(sections)
            lifeMoneyCard(sections.first { $0.code == "L3" })
            lifeOpsCard(sections)
        }
    }

    /// AN-L01 · Current Business `2399:30834`
    private func lifeCurrentCard(_ section: OperatingBoardSectionPayload?) -> some View {
        let target = model.target
        let percent = target?.configured == true
            ? (RetailFormat.percent(target?.progress, target?.value) ?? "Unavailable")
            : "Unavailable"
        let rows = section?.rows ?? []
        return RoseCard(tone: .board) {
            RoseSectionHeader(title: "Current Business", onViewAll: { sheet = .lifeCurrent })
            HStack(spacing: 8) {
                KpiChip(
                    label: "Sales today",
                    value: RetailFormat.compact(model.finance?.sales, currency: model.currency, visible: model.financeVisible == true),
                    valueColor: RetailRose.positive
                )
                KpiChip(
                    label: "Achv.",
                    value: percent,
                    valueColor: percent == "Unavailable" ? RetailRose.warning : RetailRose.amber
                )
            }
            if rows.isEmpty {
                RoseEmpty(
                    title: "Nothing recorded yet",
                    body: (section?.detail?.isEmpty == false) ? section!.detail! : "Today's posted sales and stock moves will show here."
                )
            } else {
                VStack(spacing: 8) {
                    ForEach(rows.prefix(3)) { row in
                        boardRowFor(row, trailingOverride: row.approvalStatus == "PENDING" ? "Open" : nil)
                    }
                }
            }
            if let coverage = section?.coverage, !coverage.isEmpty {
                SeverityChip(label: coverage, color: RetailRose.warning)
            }
            RoseWhyNote(body: "Today shows posted sales and explicit stock moves only.")
        }
    }

    /// AN-L02 · Sales & Orders `2399:30864`
    private func lifeSalesCard(_ sections: [OperatingBoardSectionPayload]) -> some View {
        let activity = model.activity ?? []
        let tickets = activity.filter { $0.eventType == "SALE" }.count
        let creditOpen = (model.paymentReadiness ?? []).filter { $0.relevance == "RECEIVABLE" }.count
        let holds = activity.filter {
            ($0.eventType == "ORDER" || $0.eventType == "CUSTOMER_ORDER") &&
            !["COMPLETED", "CANCELLED"].contains($0.status ?? "")
        }.count
        let channel = sections.first { $0.code == "L2" }
        return RoseCard(tone: .board) {
            RoseSectionHeader(title: "Sales & Orders", onViewAll: { sheet = .lifeSales })
            HStack(spacing: 8) {
                KpiChip(label: "Tickets", value: "\(tickets)")
                KpiChip(label: "Credit open", value: "\(creditOpen)", valueColor: RetailRose.rose)
                KpiChip(label: "Holds", value: "\(holds)", valueColor: RetailRose.amber)
            }
            if let rows = channel?.rows, !rows.isEmpty {
                VStack(spacing: 0) {
                    ForEach(rows.prefix(3)) { row in
                        detailRow(
                            eyebrow: row.channelName?.isEmpty == false ? row.channelName! : (row.title.isEmpty ? "Channel" : row.title),
                            value: row.title.isEmpty ? "Recorded" : row.title,
                            trailing: RetailFormat.compact(row.amount, currency: model.currency, visible: model.financeVisible == true),
                            trailingColor: RetailRose.textPrimary
                        )
                    }
                }
            } else if let detail = channel?.detail, !detail.isEmpty {
                RoseEmpty(title: "Counter mix", body: detail)
            } else {
                RoseEmpty(title: "Nothing recorded yet", body: "Sales, credit and holds will show here.")
            }
            RoseWhyNote(body: "Order value stays separate from posted sale.")
        }
    }

    /// AN-L03 · Money Moving `2399:30897`
    private func lifeMoneyCard(_ section: OperatingBoardSectionPayload?) -> some View {
        let rows = section?.rows ?? []
        return RoseCard(tone: .board, strokeAlpha: 0.45) {
            RoseSectionHeader(title: "Money Moving", onViewAll: { sheet = .lifeMoney })
            HStack(spacing: 8) {
                KpiChip(
                    label: "Collected",
                    value: RetailFormat.compact(model.finance?.received, currency: model.currency, visible: model.financeVisible == true),
                    valueColor: RetailRose.positive
                )
                KpiChip(
                    label: "Paid",
                    value: RetailFormat.compact(model.finance?.cashPaid, currency: model.currency, visible: model.financeVisible == true)
                )
                KpiChip(
                    label: "AR move",
                    value: RetailFormat.compact(model.finance?.outstanding, currency: model.currency, visible: model.financeVisible == true),
                    valueColor: RetailRose.rose
                )
            }
            if rows.isEmpty {
                RoseEmpty(
                    title: "Nothing moved yet",
                    body: (section?.detail?.isEmpty == false) ? section!.detail! : "Collections and payments will show here."
                )
            } else {
                VStack(spacing: 8) {
                    ForEach(rows.prefix(3)) { row in
                        boardRowFor(
                            row,
                            trailingOverride: ["COLLECTED", "PAID", "SETTLED"].contains(row.paymentStatus ?? "") ? "Done" : "Open"
                        )
                    }
                }
            }
            RoseWhyNote(body: "Collection/payment allocate — never repost sale or purchase.")
        }
    }

    /// AN-L04 · Operations / Stock `2399:30930`
    private func lifeOpsCard(_ sections: [OperatingBoardSectionPayload]) -> some View {
        let activity = model.activity ?? []
        let receipts = activity.filter { $0.eventType == "PURCHASE" }.count
        let adjustments = activity.filter { $0.eventType == "STOCK" }.count
        let alerts = (model.attention ?? []).count
        let section = sections.first { $0.code == "L4" }
        let rows = section?.rows ?? []
        return RoseCard(tone: .board) {
            RoseSectionHeader(title: "Operations / Stock", onViewAll: { sheet = .lifeOps })
            HStack(spacing: 8) {
                KpiChip(label: "Receipts", value: "\(receipts)")
                KpiChip(label: "Adjustments", value: "\(adjustments)")
                KpiChip(label: "Alerts", value: "\(alerts)", valueColor: RetailRose.rose)
            }
            if rows.isEmpty {
                RoseEmpty(
                    title: "Nothing moving",
                    body: (section?.detail?.isEmpty == false) ? section!.detail! : "Receipts, counts and damage will show here."
                )
            } else {
                VStack(spacing: 8) {
                    ForEach(rows.prefix(3)) { row in
                        boardRowFor(row, trailingOverride: "Open")
                    }
                }
            }
            RoseWhyNote(body: "Stock += accepted receipt only; never infer from billing.")
        }
    }

    // MARK: - Memory — RTL-MEMORY-v12 `2399:30979`

    private var memory: some View {
        let target = model.target
        let percent = target?.configured == true ? RetailFormat.percent(target?.progress, target?.value) : nil
        let period = RetailFormat.month(model.startDate)
        let sections = model.boards?.memory ?? []
        let revisions = model.memory?.revisions ?? []

        return VStack(alignment: .leading, spacing: RetailRose.sectionGap) {
            BoardHero(
                eyebrow: "MEMORY · THIS MONTH",
                headline: "\((model.title?.isEmpty == false) ? model.title! : "Retail Store") · memory",
                strokeAlpha: 0.45,
                chips: {
                    HStack(spacing: 8) {
                        BoardHeroChip(
                            text: percent != nil
                                ? "\(period) · \(percent!) of \(RetailFormat.compact(target?.value, currency: model.currency, visible: model.financeVisible == true))"
                                : "\(period) · memory",
                            color: RetailRose.rose
                        )
                        if !revisions.isEmpty {
                            BoardHeroChip(text: "\(revisions.count) revisions", color: RetailRose.textMuted)
                        }
                    }
                },
                kpis: {
                    HStack(spacing: 8) {
                        KpiChip(
                            label: "Sales",
                            value: RetailFormat.compact(model.finance?.sales, currency: model.currency, visible: model.financeVisible == true)
                        )
                        KpiChip(
                            label: "vs last period",
                            value: lastPeriodDelta(sections),
                            valueColor: RetailRose.warning
                        )
                        KpiChip(
                            label: "Supplier paid",
                            value: RetailFormat.compact(model.finance?.cashPaid, currency: model.currency, visible: model.financeVisible == true),
                            valueColor: RetailRose.rose
                        )
                    }
                }
            )
            monthlyMemoryCard(sections.first { $0.code == "ME1" })
            if let money = sections.first(where: { $0.code == "ME2" }) {
                moneyMemoryCard(money)
            }
            if let people = sections.first(where: { $0.code == "ME3" }) {
                peopleMemoryCard(people)
            }
            if let patterns = sections.first(where: { $0.code == "ME4" }) {
                patternsCard(patterns)
            }
            if let momentId {
                storyCta(momentId: momentId, month: period)
            }
        }
    }

    /// AN-H01 · Monthly Memory `2399:30997`
    private func monthlyMemoryCard(_ section: OperatingBoardSectionPayload?) -> some View {
        let months = (section?.rows ?? []).filter {
            $0.eventType == "MONTH" || $0.eventType.isEmpty || $0.eventType == "REVISION"
        }
        return RoseCard(tone: .board) {
            RoseSectionHeader(title: "Monthly Memory", onViewAll: { sheet = .monthly })
            HStack(spacing: 8) {
                KpiChip(label: "Months", value: "\(months.count)")
                KpiChip(
                    label: "Avg achv.",
                    value: averageAchievement(months),
                    valueColor: averageAchievement(months) == "Unavailable" ? RetailRose.warning : RetailRose.amber
                )
            }
            if months.isEmpty {
                RoseEmpty(
                    title: "No history yet",
                    body: (section?.detail?.isEmpty == false) ? section!.detail! : "Closed months will show here."
                )
            } else {
                VStack(spacing: 0) {
                    ForEach(months.prefix(3)) { row in
                        let parts = row.title.components(separatedBy: "·").map { $0.trimmingCharacters(in: .whitespaces) }
                        detailRow(
                            eyebrow: parts.first.flatMap { $0.isEmpty ? nil : $0 } ?? RetailFormat.month(row.occurredAt),
                            value: parts.dropFirst().joined(separator: " · "),
                            trailing: "Recorded",
                            trailingColor: RetailRose.textBody
                        )
                    }
                }
            }
            if let coverage = section?.coverage, !coverage.isEmpty {
                SeverityChip(label: coverage, color: RetailRose.warning)
            }
            RoseWhyNote(body: "Missing coverage stays unavailable — never averaged as zero.")
        }
    }

    /// AN-H02 · Money Memory `2399:31027`
    private func moneyMemoryCard(_ section: OperatingBoardSectionPayload) -> some View {
        let revisions = model.memory?.revisions ?? []
        let rows = section.rows ?? []
        return RoseCard(tone: .board, strokeAlpha: 0.50) {
            RoseSectionHeader(title: "Money Memory", onViewAll: { sheet = .moneyMemory })
            HStack(spacing: 8) {
                KpiChip(
                    label: "Sales",
                    value: RetailFormat.compact(model.finance?.sales, currency: model.currency, visible: model.financeVisible == true)
                )
                KpiChip(
                    label: "Collected",
                    value: RetailFormat.compact(model.finance?.received, currency: model.currency, visible: model.financeVisible == true),
                    valueColor: RetailRose.positive
                )
                KpiChip(
                    label: "Paid",
                    value: RetailFormat.compact(model.finance?.cashPaid, currency: model.currency, visible: model.financeVisible == true),
                    valueColor: RetailRose.rose
                )
            }
            if let revision = revisions.first {
                detailRow(
                    eyebrow: "Plan revision",
                    value: revision.title.isEmpty ? "Target revision" : revision.title,
                    trailing: RetailFormat.shortDay(revision.occurredAt) ?? "Recorded",
                    trailingColor: RetailRose.amber
                )
            }
            if rows.isEmpty {
                RoseEmpty(
                    title: "Nothing settled yet",
                    body: section.detail.isEmpty ? "Collections and payments will show here." : section.detail
                )
            } else {
                VStack(spacing: 0) {
                    ForEach(rows.prefix(3)) { row in
                        detailRow(
                            eyebrow: row.title.isEmpty ? retailEventLabel(row.eventType) : row.title,
                            value: row.counterpartyName?.isEmpty == false ? row.counterpartyName! : retailEventLabel(row.eventType),
                            trailing: RetailFormat.compact(row.amount, currency: model.currency, visible: model.financeVisible == true),
                            trailingColor: RetailRose.textPrimary
                        )
                    }
                }
            }
            RoseWhyNote(body: "Plan revisions versioned; memory never overwrites prior targets.")
        }
    }

    /// AN-H03 · People & Activity `2399:31060`
    private func peopleMemoryCard(_ section: OperatingBoardSectionPayload) -> some View {
        let shifts = (model.activity ?? []).filter { $0.eventType == "SHIFT" }.count
        let rows = section.rows ?? []
        return RoseCard(tone: .board) {
            RoseSectionHeader(title: "People & Activity", onViewAll: { sheet = .peopleMemory })
            HStack(spacing: 8) {
                KpiChip(label: "People", value: "\((model.participants ?? []).count)")
                KpiChip(
                    label: "Shifts",
                    value: shifts > 0 ? "\(shifts)" : "Unavailable",
                    valueColor: shifts > 0 ? RetailRose.textPrimary : RetailRose.warning
                )
            }
            if rows.isEmpty {
                RoseEmpty(
                    title: "Nothing recorded yet",
                    body: section.detail.isEmpty ? "Who did what will show here." : section.detail
                )
            } else {
                VStack(spacing: 8) {
                    ForEach(rows.prefix(3)) { row in
                        boardRowFor(row, trailingOverride: row.approvalStatus == "PENDING" ? "Open" : "Settled")
                    }
                }
            }
        }
    }

    /// AN-H04 · Patterns & Decisions `2399:31087`
    private func patternsCard(_ section: OperatingBoardSectionPayload) -> some View {
        let activity = model.activity ?? []
        let decisions = activity.filter { $0.eventType == "APPROVAL" }.count
        let approved = activity.filter { $0.eventType == "APPROVAL" && $0.approvalStatus == "APPROVED" }.count
        let rows = section.rows ?? []
        return RoseCard(tone: .board) {
            RoseSectionHeader(title: "Patterns & Decisions", onViewAll: { sheet = .patterns })
            HStack(spacing: 8) {
                KpiChip(label: "Decisions", value: "\(decisions)")
                KpiChip(label: "Approved", value: "\(approved)", valueColor: RetailRose.positive)
            }
            if rows.isEmpty {
                RoseEmpty(
                    title: "No patterns yet",
                    body: section.detail.isEmpty ? "Approvals and repeated issues will show here." : section.detail
                )
            } else {
                VStack(spacing: 8) {
                    ForEach(rows.prefix(3)) { row in
                        let approvedRow = row.approvalStatus == "APPROVED"
                        let denied = row.approvalStatus == "REJECTED"
                        boardRowFor(
                            row,
                            accent: approvedRow ? RetailRose.positive : (denied ? RetailRose.negative : RetailRose.amber),
                            trailingOverride: approvedRow ? "Approved" : (denied ? "Denied" : "Open")
                        )
                    }
                }
            }
            RoseWhyNote(body: "Approval ≠ disbursement.")
        }
    }

    /// Rose story CTA `2670:34495` — opens the Figma create-story sheet `2670:33783`.
    private func storyCta(momentId: String, month: String) -> some View {
        Button {
            sheet = .storyConfig
        } label: {
            VStack(alignment: .leading, spacing: 2) {
                HStack(spacing: 6) {
                    Image("RetailPlus")
                        .renderingMode(.template)
                        .resizable()
                        .scaledToFit()
                        .frame(width: 15, height: 15)
                        .foregroundStyle(RetailRose.inkOnRose)
                    Text("Generate Business Story")
                        .font(.plusJakarta(size: 15, weight: .heavy))
                        .foregroundStyle(RetailRose.inkOnRose)
                        .lineLimit(1)
                }
                Text("Eight pages from recorded activity · \(month) preselected")
                    .font(.plusJakarta(size: 11, weight: .medium))
                    .foregroundStyle(RetailRose.inkOnRose.opacity(0.8))
                    .lineLimit(1)
                    .minimumScaleFactor(0.8)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, 18)
            .padding(.vertical, 14)
            .background(RetailRose.rose)
            .clipShape(RoundedRectangle(cornerRadius: RetailRose.rStoryCta, style: .continuous))
            .shadow(color: RetailRose.positive.opacity(0.35), radius: 12, x: 0, y: 8)
        }
        .buttonStyle(.plain)
    }

    // MARK: - Shared row shapes

    /// Board list row with the inner `rgba(0,0,0,.28)` KPI well.
    private func boardRowFor(
        _ row: OperatingActivityPayload,
        accent: Color = RetailRose.rose,
        trailingOverride: String? = nil
    ) -> some View {
        let trailing = trailingOverride ?? row.paymentStatus.map(retailPrettyStatus) ?? "Open"
        let color: Color
        switch trailing {
        case "Done", "Settled", "Approved": color = RetailRose.positive
        case "Denied": color = RetailRose.negative
        case "Open": color = RetailRose.amber
        default:
            color = ["COLLECTED", "PAID", "SETTLED"].contains(row.paymentStatus ?? "")
                ? RetailRose.positive
                : RetailRose.textBody
        }
        var captionParts: [String] = []
        if let name = row.counterpartyName, !name.isEmpty { captionParts.append(name) }
        if let channel = row.channelName, !channel.isEmpty { captionParts.append(channel) }
        if let quantity = row.quantity, !quantity.isEmpty {
            captionParts.append(row.unit.map { "\(quantity) \($0)" } ?? quantity)
        }
        if captionParts.isEmpty { captionParts.append(retailEventLabel(row.eventType)) }

        return BoardRow(
            eyebrow: row.title.isEmpty ? retailEventLabel(row.eventType) : row.title,
            value: captionParts.joined(separator: " · "),
            trailing: trailing,
            trailingColor: color,
            accent: accent,
            onTap: { OperatingChromeState.shared.editing = row }
        )
    }

    /// Board row in the `[eyebrow / value / trailing]` form used inside slate cards.
    private func detailRow(
        eyebrow: String,
        value: String,
        trailing: String,
        trailingColor: Color = RetailRose.textPrimary,
        accent: Color = RetailRose.rose
    ) -> some View {
        BoardRow(
            eyebrow: eyebrow,
            value: value,
            trailing: trailing,
            trailingColor: trailingColor,
            accent: accent
        )
    }

    /// 36pt rounded rose icon well used by sheet headers.
    private func sheetIconWell(_ asset: String) -> some View {
        ZStack {
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .fill(RetailRose.tone(RetailRose.rose))
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .stroke(RetailRose.toneStroke(RetailRose.rose), lineWidth: 1)
            Image(asset)
                .renderingMode(.template)
                .resizable()
                .scaledToFit()
                .frame(width: 20, height: 20)
                .foregroundStyle(RetailRose.rose)
        }
        .frame(width: 36, height: 36)
    }

    // MARK: - Derived facts

    private var dayOfPeriod: String {
        guard let start = RetailFormat.parse(model.startDate) else { return "Unavailable" }
        let today = Calendar.current.startOfDay(for: Date())
        guard today >= start else { return "0" }
        let days = Calendar.current.dateComponents([.day], from: start, to: today).day ?? 0
        return "\(days + 1)"
    }

    /// `−6%` when the previous period is known, otherwise `Unavailable`.
    private func lastPeriodDelta(_ sections: [OperatingBoardSectionPayload]) -> String {
        let monthly = (sections.first { $0.code == "ME1" }?.rows) ?? []
        guard monthly.count >= 2,
              let current = monthly[0].amount.flatMap({ Decimal(string: $0) }),
              let previous = monthly[1].amount.flatMap({ Decimal(string: $0) }),
              previous > 0 else { return "Unavailable" }
        let delta = (current - previous) / previous * 100
        var rounded = Decimal()
        var value = delta
        NSDecimalRound(&rounded, &value, 0, .plain)
        let sign = rounded < 0 ? "−" : "+"
        let magnitude = rounded < 0 ? -rounded : rounded
        return "\(sign)\(magnitude)%"
    }

    private func averageAchievement(_ rows: [OperatingActivityPayload]) -> String {
        let values: [Int] = rows.compactMap { row in
            guard let regex = try? NSRegularExpression(pattern: #"(\d+)%"#) else { return nil }
            let text = row.title
            guard let match = regex.firstMatch(in: text, range: NSRange(text.startIndex..., in: text)),
                  let range = Range(match.range(at: 1), in: text) else { return nil }
            return Int(text[range])
        }
        guard !values.isEmpty else { return "Unavailable" }
        return "\(values.reduce(0, +) / values.count)%"
    }

    private func roleLabel(_ code: String?) -> String {
        OperatingMomentCatalog.roles.first { $0.0 == code }?.1 ?? "Retail Store"
    }
}

private extension String {
    func takeUnless(_ other: String) -> String? {
        self == other ? nil : self
    }
}

// MARK: - View-all route

/// Which board a `View all ›` opens (16 sheets from the plan).
enum RetailSheetRoute: String, Identifiable {
    case attention, target, payment, finance, activity, gst
    case storyConfig
    case commitments, momentDetail, people, network
    case lifeCurrent, lifeSales, lifeMoney, lifeOps
    case monthly, moneyMemory, peopleMemory, patterns

    var id: String { rawValue }

    var title: String {
        switch self {
        case .attention: return "Needs Attention"
        case .target: return "Target & Budget"
        case .payment: return "Payment Readiness"
        case .finance: return "Finance"
        case .activity: return "Activity"
        case .gst: return "GST Position"
        case .commitments: return "Open Commitments"
        case .momentDetail: return "My Retail Business"
        case .people: return "People"
        case .network: return "Business Network"
        case .lifeCurrent: return "Current Business"
        case .lifeSales: return "Sales & Orders"
        case .lifeMoney: return "Money Moving"
        case .lifeOps: return "Operations / Stock"
        case .monthly: return "Monthly Memory"
        case .moneyMemory: return "Money Memory"
        case .peopleMemory: return "People & Activity"
        case .patterns: return "Patterns & Decisions"
        case .storyConfig: return "Create Business Story"
        }
    }

    /// Board section code shown by this route, when it maps to one.
    var boardCode: String? {
        switch self {
        case .commitments: return "M4"
        case .momentDetail: return "M1"
        case .network: return "M3"
        case .lifeCurrent: return "L1"
        case .lifeMoney: return "L3"
        case .lifeOps: return "L4"
        case .monthly: return "ME1"
        case .moneyMemory: return "ME2"
        case .peopleMemory: return "ME3"
        case .patterns: return "ME4"
        default: return nil
        }
    }
}

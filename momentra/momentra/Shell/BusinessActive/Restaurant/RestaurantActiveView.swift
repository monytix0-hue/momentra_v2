import SwiftUI

/// Restaurant active experience — Figma `2487:37611` (`resuarant`).
///
/// Frames: `RST-PULSE-v12` `2400:29655`, `RST-MOMENTS-v12` `2400:30664`,
/// `RST-LIFE-v12` `2400:30824`, `RST-MEMORY-v12` `2400:30984`.
///
/// Every number is read from the operating projection. A metric the backend cannot
/// supply renders `Unavailable` — never a fabricated `0`, `0%` or `—`-as-zero.
/// Covers, tables and KOT ticket ids have no field on the strict operating contract,
/// so those frame values are replaced by the closest recorded fact instead of invented.
struct RestaurantActiveView: View {
    let model: OperatingProjectionPayload
    let tab: OperatingTab
    var onOpenQuickAdd: () -> Void = {}
    var onRecord: (RestaurantEventDraft) async throws -> Void = { _ in }
    var onDecide: (String, String) async throws -> Void = { _, _ in }
    var momentId: String? = nil
    var onViewAllAttention: () -> Void = {}

    @State private var sheet: RestaurantSheetRoute?
    @State private var picker: RestaurantSheetRoute?
    @State private var pickerDate = Date()

    var body: some View {
        EmberScreenBackground {
            if tab == .quickAdd {
                RestaurantQuickAddHub(
                    model: model,
                    momentId: momentId,
                    onRecord: onRecord,
                    onDecide: onDecide
                )
            } else {
                ScrollView {
                    VStack(alignment: .leading, spacing: RestaurantEmber.sectionGap) {
                        EmberScreenHeader(title: tabTitle, roleLine: roleLine)
                        EmberContextChips(
                            subtypeLabel: model.subtypeLabel ?? "Restaurant",
                            periodLine: RestaurantFormat.periodLine(model.startDate, currency: model.currency),
                            gstOn: model.gstTracking == "YES",
                            quickAdds: tab != .quickAdd,
                            onQuickAdds: onOpenQuickAdd
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
                    .padding(.horizontal, RestaurantEmber.gutter)
                    .padding(.vertical, 16)
                }
            }
        }
        .sheet(item: $sheet) { route in
            if route == .storyConfig, let momentId {
                RestaurantStoryConfigSheet(model: model, momentId: momentId) { sheet = nil }
            } else if let mode = route.pickerMode {
                EmberPickerSheet(
                    mode: mode,
                    venue: model.title ?? "",
                    date: $pickerDate,
                    onDismiss: { sheet = nil }
                )
            } else {
                RestaurantBoardSheetView(route: route, model: model, onRange: { picker = $0 }) { sheet = nil }
            }
        }
        .sheet(item: $picker) { route in
            if let mode = route.pickerMode {
                EmberPickerSheet(
                    mode: mode,
                    venue: model.title ?? "",
                    date: $pickerDate,
                    onDismiss: { picker = nil }
                )
            }
        }
    }

    // MARK: - Header

    private var roleLine: String {
        let role = roleLabel(model.role)
        let party = (model.partyName ?? "").trimmingCharacters(in: .whitespaces)
        let subtype = model.subtypeLabel ?? "Restaurant"
        return [role, party].filter { !$0.isEmpty }.joined(separator: " ") + " · \(subtype)"
    }

    private var tabTitle: String {
        let name = (model.title ?? "").isEmpty ? "Restaurant" : model.title!
        switch tab {
        case .pulse: return "\(name) Pulse"
        case .moments: return "Moments"
        case .life: return "Life"
        case .memory: return "Memory"
        case .quickAdd: return "Quick Adds"
        }
    }

    // MARK: - Pulse — RST-PULSE-v12 `2400:29655`

    private var pulse: some View {
        VStack(alignment: .leading, spacing: RestaurantEmber.sectionGap) {
            pulseHero
            attentionCard
            targetBudgetCard
            paymentReadinessCard
            financeCard
            activityCard
            if model.gstTracking == "YES" { gstCard }
        }
    }

    /// Ember hero: eyebrow, promise line, one-line position + 40pt glyph well.
    /// `Covers 86` from the frame is omitted — the strict contract carries no covers field.
    private var pulseHero: some View {
        let sales = RestaurantFormat.compact(
            model.finance?.sales ?? model.target?.progress,
            currency: model.currency,
            visible: model.financeVisible == true
        )
        let targetValue = model.target?.configured == true
            ? RestaurantFormat.compact(model.target?.value, currency: model.currency, visible: model.financeVisible == true)
            : nil
        let receivable = RestaurantFormat.compact(model.finance?.outstanding, currency: model.currency, visible: model.financeVisible == true)
        let summary = (targetValue != nil ? "Sales \(sales) / \(targetValue!)" : "Sales \(sales)")
            + " · Open tickets \(openOrders.count) · AR \(receivable)"

        return ZStack(alignment: .topTrailing) {
            VStack(alignment: .leading, spacing: 6) {
                Text(
                    [(model.title ?? "Restaurant").uppercased(), model.location?.uppercased()]
                        .compactMap { $0 }
                        .filter { !$0.isEmpty }
                        .joined(separator: " · ")
                )
                .font(.plusJakarta(size: 11, weight: .bold))
                .tracking(0.6)
                .foregroundStyle(RestaurantEmber.heroEyebrowTint)
                .lineLimit(1)
                .padding(.trailing, 52)

                Text("Order. Serve. Collect.")
                    .font(.plusJakarta(size: 22, weight: .bold))
                    .foregroundStyle(.white)
                    .lineLimit(1)
                    .minimumScaleFactor(0.8)

                Text(summary)
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(RestaurantEmber.heroBodyTint)
                    .lineLimit(2)
                    .padding(.trailing, 56)

                if (model.quickAdds ?? []).contains(where: { $0.code == "SALE" }) {
                    EmberCta(label: "Record Sale", plus: true, onTap: onOpenQuickAdd)
                        .padding(.top, 4)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            heroGlyph
        }
        .padding(20)
        .background(RestaurantEmber.heroGradient)
        .clipShape(RoundedRectangle(cornerRadius: RestaurantEmber.rHero, style: .continuous))
        .emberHeroShadow()
    }

    /// 40pt `#1c1917` glyph well pinned to the hero's top-right.
    private var heroGlyph: some View {
        ZStack {
            RoundedRectangle(cornerRadius: RestaurantEmber.rHeroIcon, style: .continuous)
                .fill(RestaurantEmber.surfaceWell)
            Image(RestaurantTileIcons.heroGlyph)
                .renderingMode(.template)
                .resizable()
                .scaledToFit()
                .frame(width: 20, height: 20)
                .foregroundStyle(.white)
        }
        .frame(width: 40, height: 40)
        .shadow(color: RestaurantEmber.ember.opacity(0.5), radius: 6, x: 0, y: 4)
    }

    /// AN-P01 · Needs Attention `2400:29711`
    private var attentionCard: some View {
        let items = model.attention ?? []
        let reorders = items.filter { $0.kind.uppercased().hasPrefix("LOW_STOCK") }.count
        return EmberCard {
            EmberCardHeader(
                asset: RestaurantTileIcons.status,
                title: "Needs Attention",
                chipText: items.isEmpty ? nil : "Alert",
                chipColor: RestaurantEmber.negative,
                onViewAll: { sheet = .attention }
            )
            HStack(spacing: 8) {
                EmberMetricChip(label: "Alerts", value: "\(items.count)", valueColor: RestaurantEmber.ember)
                EmberMetricChip(label: "Tickets", value: "\(openOrders.count)")
            }
            if items.isEmpty {
                EmberEmpty(
                    title: "Nothing is waiting",
                    body: "Approvals, overdue bills and blockers will show here."
                )
            } else {
                VStack(spacing: 8) {
                    ForEach(items.prefix(3)) { item in
                        attentionRow(item)
                    }
                }
            }
            EmberWhyCaption(body: "Platform receivable needs explicit settlement link — never invent collection from order complete.")
        }
    }

    private func attentionRow(_ item: OperatingAttentionPayload) -> some View {
        let hasSource = item.sourceEventId != nil || item.id.hasPrefix("open-") || item.cta != nil
        let label = restaurantSeverityLabel(kind: item.kind, severity: item.severity, hasSource: hasSource)
        let color = hasSource ? restaurantSeverityColor(item.severity) : RestaurantEmber.warning
        let caption = [
            item.counterpartyName,
            RestaurantFormat.shortDay(item.dueAt).map { "Due \($0)" },
            RestaurantFormat.compact(item.amount, currency: model.currency, visible: model.financeVisible == true)
                .takeUnlessRestaurant("—"),
        ]
        .compactMap { $0 }
        .filter { !$0.isEmpty }
        .joined(separator: " · ")

        return EmberAccentRow(
            title: item.title.isEmpty ? "Attention item" : item.title,
            caption: caption.isEmpty ? "No detail recorded" : caption,
            accent: color,
            severityLabel: label,
            severityColor: color,
            onTap: { openOperatingAttention(item, model: model, onOpenQuickAdd: onOpenQuickAdd) }
        )
    }

    /// AN-P02 · Target & Budget `2400:29677`
    private var targetBudgetCard: some View {
        let target = model.target
        let budget = model.budget
        let configured = target?.configured == true
        let percentText = configured ? (RestaurantFormat.percent(target?.progress, target?.value) ?? "Unavailable") : "Unavailable"
        let fraction = configured ? RestaurantFormat.fraction(target?.progress, target?.value) : nil
        let sales = RestaurantFormat.compact(
            target?.progress ?? model.finance?.sales,
            currency: model.currency,
            visible: model.financeVisible == true
        )
        let targetValue = configured
            ? RestaurantFormat.compact(target?.value, currency: model.currency, visible: model.financeVisible == true)
            : nil
        let budgetPercent = budget?.configured == true
            ? (RestaurantFormat.percent(budget?.spent, budget?.amount) ?? "Unavailable")
            : "Unavailable"

        return EmberCard {
            EmberCardHeader(
                asset: RestaurantTileIcons.budget,
                title: "Target & Budget",
                onViewAll: { sheet = .target }
            )
            HStack(spacing: 16) {
                EmberHealthRing(percentLabel: percentText, caption: "of target", fraction: fraction)
                VStack(alignment: .leading, spacing: 6) {
                    Text("ACHIEVEMENT VS TARGET")
                        .font(.plusJakarta(size: 10, weight: .bold))
                        .tracking(0.6)
                        .foregroundStyle(RestaurantEmber.ember)
                        .lineLimit(1)
                    Text(configured && targetValue != nil
                         ? "Sales \(sales) of \(targetValue!)"
                         : "Sales \(sales) · no target set")
                        .font(.plusJakarta(size: 15, weight: .bold))
                        .foregroundStyle(RestaurantEmber.textPrimary)
                        .fixedSize(horizontal: false, vertical: true)
                    targetStatusPill(fraction)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            }
            .padding(14)
            .background(RestaurantEmber.surfaceWell)
            .clipShape(RoundedRectangle(cornerRadius: RestaurantEmber.rCardTight, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: RestaurantEmber.rCardTight, style: .continuous)
                    .stroke(RestaurantEmber.ember.opacity(0.22), lineWidth: 1)
            )
            HStack(spacing: 8) {
                EmberMetricChip(
                    label: "Achievement",
                    value: percentText,
                    valueColor: percentText == "Unavailable" ? RestaurantEmber.warning : RestaurantEmber.textPrimary
                )
                EmberMetricChip(label: "Sales", value: sales)
                EmberMetricChip(
                    label: "Budget use",
                    value: budgetPercent,
                    valueColor: budgetPercent == "Unavailable" ? RestaurantEmber.warning : RestaurantEmber.textPrimary
                )
            }
            VStack(spacing: 0) {
                EmberDividedRow(
                    title: "Sales vs target",
                    caption: "Effective \(RestaurantFormat.month(model.startDate)) · restaurant",
                    trailingValue: configured && targetValue != nil ? "\(sales) / \(targetValue!)" : "Unavailable",
                    trailingColor: configured ? RestaurantEmber.textPrimary : RestaurantEmber.warning
                )
                EmberDividedRow(
                    title: "Eligible spend",
                    caption: "Ingredients + running",
                    trailingValue: RestaurantFormat.compact(model.finance?.eligibleCost, currency: model.currency, visible: model.financeVisible == true)
                )
                EmberDividedRow(
                    title: "Coverage",
                    caption: (target?.coverage?.isEmpty == false) ? target!.coverage! : "Coverage unavailable",
                    trailingValue: (target?.coverage?.isEmpty == false) ? "Recorded" : "Unavailable",
                    trailingColor: (target?.coverage?.isEmpty == false) ? RestaurantEmber.positive : RestaurantEmber.warning,
                    captionColor: (target?.coverage?.isEmpty == false) ? RestaurantEmber.textBody : RestaurantEmber.warning
                )
            }
            EmberWhyCaption(body: "Order and prep are not revenue — achievement uses posted bills / finalized sales only.")
        }
    }

    @ViewBuilder
    private func targetStatusPill(_ fraction: Double?) -> some View {
        if let fraction {
            if fraction < 0.5 {
                EmberStatusPill(label: "● Behind", color: RestaurantEmber.negative)
            } else if fraction >= 0.75 {
                EmberStatusPill(label: "● On track", color: RestaurantEmber.positive)
            } else {
                EmberStatusPill(label: "● In progress", color: RestaurantEmber.amber)
            }
        } else {
            EmberStatusPill(label: "● Unavailable", color: RestaurantEmber.warning)
        }
    }

    /// AN-P03 · Payment Readiness `2400:29742`
    private var paymentReadinessCard: some View {
        let all = model.paymentReadiness ?? []
        let payables = all.filter { $0.relevance != "RECEIVABLE" }
        let settled = all
            .filter { $0.relevance != "RECEIVABLE" && ["PAID", "SETTLED"].contains($0.status) }
            .reduce(Decimal(0)) { $0 + (Decimal(string: $1.amount) ?? 0) }
        let paidText = settled > 0
            ? RestaurantFormat.compact("\(settled)", currency: model.currency, visible: model.financeVisible == true)
            : RestaurantFormat.compact(model.finance?.cashPaid, currency: model.currency, visible: model.financeVisible == true)

        return EmberCard {
            EmberCardHeader(
                asset: RestaurantTileIcons.payment,
                title: "Payment Readiness",
                onViewAll: { sheet = .payment }
            )
            HStack(spacing: 8) {
                EmberMetricChip(
                    label: "AP open",
                    value: RestaurantFormat.compact(model.finance?.payable, currency: model.currency, visible: model.financeVisible == true),
                    valueColor: RestaurantEmber.amber
                )
                EmberMetricChip(label: "Paid", value: paidText, valueColor: RestaurantEmber.positive)
            }
            if payables.isEmpty {
                EmberEmpty(title: "No open payments", body: "Ingredient bills and kitchen expenses waiting to settle will show here.")
            } else {
                VStack(spacing: 0) {
                    ForEach(payables.prefix(3)) { row in
                        EmberDividedRow(
                            title: row.title.isEmpty ? (row.counterpartyName ?? "Open payable") : row.title,
                            caption: paymentCaption(row),
                            trailingValue: RestaurantFormat.compact(row.amount, currency: model.currency, visible: model.financeVisible == true),
                            trailingColor: row.dueBucket == "OVERDUE" ? RestaurantEmber.negative : RestaurantEmber.amber,
                            captionColor: row.dueBucket == "OVERDUE" ? RestaurantEmber.negative : RestaurantEmber.textBody,
                            onTap: { OperatingChromeState.shared.settling = row }
                        )
                    }
                }
            }
            EmberWhyCaption(body: "Purchase ≠ payment. Aggregator fees must not duplicate cost facts.")
        }
    }

    private func paymentCaption(_ row: OperatingPaymentPayload) -> String {
        let parts = [
            row.counterpartyName,
            RestaurantFormat.shortDay(row.dueAt).map { "Due \($0)" },
        ].compactMap { $0 }.filter { !$0.isEmpty }
        if !parts.isEmpty { return parts.joined(separator: " · ") }
        return row.status.lowercased().capitalized
    }

    /// AN-P04 · Finance `2400:29772`
    private var financeCard: some View {
        let receivables = (model.paymentReadiness ?? []).filter { $0.relevance == "RECEIVABLE" }
        return EmberCard {
            EmberCardHeader(
                asset: RestaurantTileIcons.collection,
                title: "Finance",
                onViewAll: { sheet = .finance }
            )
            HStack(spacing: 8) {
                EmberMetricChip(
                    label: "Billed",
                    value: RestaurantFormat.compact(model.finance?.sales, currency: model.currency, visible: model.financeVisible == true)
                )
                EmberMetricChip(
                    label: "Collected",
                    value: RestaurantFormat.compact(model.finance?.received, currency: model.currency, visible: model.financeVisible == true),
                    valueColor: RestaurantEmber.positive
                )
                EmberMetricChip(
                    label: "Platform AR",
                    value: RestaurantFormat.compact(model.finance?.outstanding, currency: model.currency, visible: model.financeVisible == true),
                    valueColor: RestaurantEmber.ember
                )
            }
            if model.financeVisible != true {
                EmberEmpty(title: "Totals stay private", body: "Finance, Approver and Owner see the money position.")
            } else if receivables.isEmpty {
                EmberEmpty(title: "Nothing to collect", body: "Platform and credit bills waiting on settlement will show here.")
            } else {
                VStack(spacing: 0) {
                    ForEach(receivables.prefix(3)) { row in
                        EmberDividedRow(
                            title: (row.counterpartyName ?? row.title).isEmpty ? "Open receivable" : (row.counterpartyName ?? row.title),
                            caption: RestaurantFormat.shortDay(row.dueAt).map { "Due \($0)" } ?? "Awaiting settle",
                            trailingValue: RestaurantFormat.compact(row.amount, currency: model.currency, visible: true),
                            trailingColor: row.dueBucket == "OVERDUE" ? RestaurantEmber.negative : RestaurantEmber.ember,
                            captionColor: row.dueBucket == "OVERDUE" ? RestaurantEmber.negative : RestaurantEmber.textBody,
                            onTap: { OperatingChromeState.shared.settling = row }
                        )
                    }
                }
            }
            EmberWhyCaption(body: "Posted bill creates revenue. Prep/fulfil do not. Platform AR only with explicit allocation.")
        }
    }

    /// AN-P05 · Activity `2400:29805`
    private var activityCard: some View {
        let activity = model.activity ?? []
        let stockMoves = activity.filter { $0.eventType == "STOCK" }.count
        return EmberCard {
            EmberCardHeader(
                asset: RestaurantTileIcons.order,
                title: "Activity",
                viewAllLabel: "View activity ›",
                onViewAll: { sheet = .activity }
            )
            HStack(spacing: 8) {
                EmberMetricChip(label: "Orders", value: "\(orderRows.count)")
                EmberMetricChip(label: "Firing", value: "\(firingOrders.count)", valueColor: RestaurantEmber.amber)
                EmberMetricChip(label: "Stock moves", value: "\(stockMoves)")
            }
            if activity.isEmpty {
                EmberEmpty(title: "Nothing recorded yet", body: "Bills, kitchen tickets and stock moves will show here.")
            } else {
                VStack(spacing: 0) {
                    ForEach(activity.prefix(3)) { row in
                        activityDividedRow(row)
                    }
                }
            }
            EmberWhyCaption(body: "Cancel, amend, spoilage and split tender must not duplicate facts.")
        }
    }

    private func activityDividedRow(_ row: OperatingActivityPayload) -> some View {
        let trailing = activityTrailing(row)
        return EmberDividedRow(
            title: row.title.isEmpty ? restaurantEventLabel(row.eventType) : row.title,
            caption: activityCaption(row),
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
            parts.append(restaurantEventLabel(row.eventType))
        }
        if let quantity = row.quantity, !quantity.isEmpty {
            parts.append(row.unit.map { "\(quantity) \($0)" } ?? quantity)
        }
        return parts.joined(separator: " · ")
    }

    private func activityTrailing(_ row: OperatingActivityPayload) -> String {
        if row.eventType == "ORDER" || row.eventType == "CUSTOMER_ORDER" {
            return restaurantOrderStatusLabel(row.status)
        }
        if row.approvalStatus == "PENDING" { return "Pending" }
        if let status = row.paymentStatus, ["COLLECTED", "PAID", "SETTLED"].contains(status) { return "Done" }
        if let status = row.status, !status.isEmpty { return status.lowercased().capitalized }
        return "Open"
    }

    private func activityTrailingColor(_ trailing: String) -> Color {
        switch trailing {
        case "Done", "Served", "Ready", "Approved": return RestaurantEmber.positive
        case "Pending", "Firing", "Sent": return RestaurantEmber.amber
        case "Void": return RestaurantEmber.negative
        default: return RestaurantEmber.textBody
        }
    }

    /// AN-P06 · GST Position `2400:29838`
    private var gstCard: some View {
        let gst = model.gstPosition
        let complete = gst?.outputGst?.isEmpty == false && gst?.inputGst?.isEmpty == false
        return EmberCard {
            EmberCardHeader(
                asset: RestaurantTileIcons.status,
                title: "GST Position",
                onViewAll: { sheet = .gst }
            )
            HStack(spacing: 8) {
                EmberMetricChip(label: "Output", value: RestaurantFormat.compact(gst?.outputGst, currency: model.currency))
                EmberMetricChip(label: "ITC", value: RestaurantFormat.compact(gst?.inputGst, currency: model.currency), valueColor: RestaurantEmber.positive)
                EmberMetricChip(label: "Net", value: RestaurantFormat.compact(gst?.difference, currency: model.currency), valueColor: RestaurantEmber.amber)
            }
            if !complete {
                EmberEmpty(title: "Registration incomplete", body: "GST stays Unavailable until the registration is complete.")
            } else {
                VStack(spacing: 0) {
                    EmberDividedRow(
                        title: "Outward taxable",
                        caption: "Posted bills \(RestaurantFormat.month(model.startDate))",
                        trailingValue: RestaurantFormat.compact(model.finance?.sales, currency: model.currency, visible: true)
                    )
                    EmberDividedRow(
                        title: "Inward eligible",
                        caption: "Posted purchases",
                        trailingValue: RestaurantFormat.compact(model.finance?.eligibleCost, currency: model.currency, visible: true),
                        trailingColor: RestaurantEmber.positive
                    )
                    EmberDividedRow(
                        title: "Net payable",
                        caption: RestaurantFormat.asOf(gst?.asOfMonth.map { "\($0)-01" }),
                        trailingValue: RestaurantFormat.compact(gst?.difference, currency: model.currency),
                        trailingColor: RestaurantEmber.amber
                    )
                }
            }
            EmberWhyCaption(body: "GST only when enabled. Incomplete registration → Unavailable. Indicative only — not a filing.")
        }
    }

    // MARK: - Moments — RST-MOMENTS-v12 `2400:30664`

    private var moments: some View {
        VStack(alignment: .leading, spacing: RestaurantEmber.sectionGap) {
            momentsHero
            commitmentsCard
            myRestaurantCard
            peopleCard
            networkCard
            EmberCard(tone: .board, onTap: onOpenQuickAdd) {
                HStack(spacing: 10) {
                    EmberIconWell(asset: RestaurantTileIcons.sale)
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Record the next restaurant moment")
                            .font(.plusJakarta(size: 16, weight: .bold))
                            .foregroundStyle(RestaurantEmber.textPrimary)
                            .fixedSize(horizontal: false, vertical: true)
                        Text("Bill, kitchen ticket, purchase or stock count.")
                            .font(.plusJakarta(size: 11))
                            .foregroundStyle(RestaurantEmber.textBody)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    Text("›")
                        .font(.plusJakarta(size: 16, weight: .bold))
                        .foregroundStyle(RestaurantEmber.ember)
                }
            }
        }
    }

    private var momentsHero: some View {
        let staff = (model.participants ?? []).count
        let suppliers = (model.masters ?? []).filter { ["SUPPLIER", "VENDOR", "PRINCIPAL"].contains($0.kind.uppercased()) }.count
        return EmberBoardHero(
            eyebrow: "ACTIVE MOMENT",
            headline: (model.title?.isEmpty == false) ? model.title! : "Restaurant",
            chips: {
                HStack(spacing: 8) {
                    EmberBoardHeroChip(text: model.subtypeLabel ?? "Restaurant", color: RestaurantEmber.ember)
                    if let location = model.location, !location.isEmpty {
                        EmberBoardHeroChip(text: location, color: RestaurantEmber.positive, dot: true)
                    }
                }
            },
            kpis: {
                HStack(spacing: 8) {
                    EmberKpiChip(label: "People · Staff", value: "\(staff)")
                    EmberKpiChip(label: "Network · Suppliers", value: "\(suppliers)")
                    EmberKpiChip(
                        label: "AP open",
                        value: RestaurantFormat.compact(model.finance?.payable, currency: model.currency, visible: model.financeVisible == true),
                        valueColor: RestaurantEmber.amber
                    )
                }
            }
        )
    }

    /// AN-M04 · Open Commitments `2400:30767`
    private var commitmentsCard: some View {
        let rows = ((model.boards?.moments ?? []).first { $0.code == "M4" }?.rows) ?? []
        return EmberCard(tone: .board, strokeAlpha: 0.40) {
            EmberBoardHeader(title: "Open Commitments", onViewAll: { sheet = .commitments })
            HStack(spacing: 8) {
                EmberKpiChip(
                    label: "AP",
                    value: RestaurantFormat.compact(model.finance?.payable, currency: model.currency, visible: model.financeVisible == true),
                    valueColor: RestaurantEmber.amber
                )
                EmberKpiChip(
                    label: "Platform AR",
                    value: RestaurantFormat.compact(model.finance?.outstanding, currency: model.currency, visible: model.financeVisible == true),
                    valueColor: RestaurantEmber.ember
                )
                EmberKpiChip(label: "Orders", value: "\(openOrders.count)")
            }
            if rows.isEmpty {
                EmberEmpty(title: "Nothing open", body: "Kitchen tickets, payables and platform receivables will show here.")
            } else {
                VStack(spacing: 8) {
                    ForEach(rows.prefix(3)) { row in
                        boardRowFor(row)
                    }
                }
            }
            EmberWhyNote(body: "Catering order ≠ revenue until billed.")
        }
    }

    /// AN-M01 · My Restaurant `2400:30674`
    private var myRestaurantCard: some View {
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
        return EmberCard(tone: .board) {
            EmberBoardHeader(title: "My Restaurant", onViewAll: { sheet = .momentDetail })
            HStack(spacing: 8) {
                EmberKpiChip(label: "Status", value: retired ? "Closed" : "Open", valueColor: retired ? RestaurantEmber.textMuted : RestaurantEmber.positive)
                EmberKpiChip(label: "Day", value: dayOfPeriod)
                EmberKpiChip(
                    label: "GST",
                    value: model.gstTracking == "YES" ? "On" : "Off",
                    valueColor: RestaurantEmber.amber
                )
            }
            VStack(spacing: 0) {
                detailRow(
                    eyebrow: "Business",
                    value: [model.title, model.location].compactMap { $0 }.filter { !$0.isEmpty }.joined(separator: " · "),
                    trailing: retired ? "Closed" : "Open",
                    trailingColor: retired ? RestaurantEmber.textMuted : RestaurantEmber.positive
                )
                detailRow(
                    eyebrow: "Plan",
                    value: targetConfigured
                        ? "Sales \(RestaurantFormat.compact(model.target?.value, currency: model.currency, visible: model.financeVisible == true)) target"
                        : "No target set",
                    trailing: targetConfigured ? "Active" : "Unavailable",
                    trailingColor: targetConfigured ? RestaurantEmber.textPrimary : RestaurantEmber.warning
                )
                detailRow(
                    eyebrow: "GST",
                    value: gstValue,
                    trailing: model.gstTracking == "YES" ? "On" : "Off",
                    trailingColor: RestaurantEmber.amber
                )
            }
            EmberWhyNote(body: "Order→prep→bill→collect. Posted bill owns revenue.")
        }
    }

    /// AN-M02 · People `2400:30707`
    private var peopleCard: some View {
        let staff = model.participants ?? []
        let stations = stationMasters.count
        return EmberCard(tone: .board) {
            EmberBoardHeader(title: "People", onViewAll: { sheet = .people })
            HStack(spacing: 8) {
                EmberKpiChip(label: "Staff", value: "\(staff.count)")
                EmberKpiChip(
                    label: "Stations",
                    value: stations > 0 ? "\(stations)" : "Unavailable",
                    valueColor: stations > 0 ? RestaurantEmber.textPrimary : RestaurantEmber.warning
                )
            }
            if staff.isEmpty {
                EmberEmpty(title: "No people yet", body: "Invite the kitchen, floor and billing team.")
            } else {
                VStack(spacing: 0) {
                    ForEach(staff.prefix(3), id: \.self) { name in
                        detailRow(
                            eyebrow: name,
                            value: name == model.partyName ? roleLabel(model.role) : "Member",
                            trailing: name == model.partyName ? "Owner" : "Member",
                            trailingColor: name == model.partyName ? RestaurantEmber.textPrimary : RestaurantEmber.textBody
                        )
                    }
                }
            }
            EmberWhyNote(body: "Roles gate voids, comps and platform settlements.")
        }
    }

    /// AN-M03 · Business Network `2400:30737`
    private var networkCard: some View {
        let masters = model.masters ?? []
        let suppliers = masters.filter { ["SUPPLIER", "VENDOR", "PRINCIPAL"].contains($0.kind.uppercased()) }
        let platforms = masters.filter { ["CHANNEL", "PLATFORM", "AGGREGATOR", "CUSTOMER", "BUYER"].contains($0.kind.uppercased()) }
        let payables = (model.paymentReadiness ?? []).filter { $0.relevance != "RECEIVABLE" }
        let receivables = (model.paymentReadiness ?? []).filter { $0.relevance == "RECEIVABLE" }
        var combined: [(String, Bool)] = []
        for supplier in suppliers where !combined.contains(where: { $0.0 == supplier.name }) {
            combined.append((supplier.name, true))
        }
        for party in platforms where !combined.contains(where: { $0.0 == party.name }) {
            combined.append((party.name, false))
        }

        return EmberCard(tone: .board) {
            EmberBoardHeader(title: "Business Network", onViewAll: { sheet = .network })
            HStack(spacing: 8) {
                EmberKpiChip(label: "Suppliers", value: "\(suppliers.count)")
                EmberKpiChip(label: "Platforms", value: "\(platforms.count)")
            }
            if combined.isEmpty {
                EmberEmpty(title: "No network yet", body: "Suppliers, platforms and credit parties you record will show here.")
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
                            trailing = "AP \(RestaurantFormat.compact("\(openAp)", currency: model.currency, visible: model.financeVisible == true))"
                            color = RestaurantEmber.amber
                        } else if !pair.1 && openAr > 0 {
                            trailing = "AR \(RestaurantFormat.compact("\(openAr)", currency: model.currency, visible: model.financeVisible == true))"
                            color = RestaurantEmber.ember
                        } else {
                            trailing = "No balance"
                            color = RestaurantEmber.textMuted
                        }
                        detailRow(
                            eyebrow: pair.0,
                            value: pair.1 ? "Supplier" : "Platform / buyer",
                            trailing: trailing,
                            trailingColor: color
                        )
                    }
                }
            }
            EmberWhyNote(body: "Platform debt only on explicit allocation.")
        }
    }

    // MARK: - Life — RST-LIFE-v12 `2400:30824`

    private var life: some View {
        let open = openOrders.count
        let done = (model.activity ?? []).filter { ["COLLECTED", "PAID", "SETTLED"].contains($0.paymentStatus ?? "") }.count
        let sections = model.boards?.life ?? []
        return VStack(alignment: .leading, spacing: RestaurantEmber.sectionGap) {
            EmberBoardHero(
                eyebrow: "TODAY",
                headline: "\((model.title?.isEmpty == false) ? model.title! : "Restaurant") · today",
                strokeAlpha: 0.45,
                chips: {
                    HStack(spacing: 8) {
                        EmberBoardHeroChip(text: RestaurantFormat.todayLine(), color: RestaurantEmber.ember)
                        EmberBoardHeroChip(
                            text: model.retired == true ? "Restaurant closed" : "Service live",
                            color: model.retired == true ? RestaurantEmber.textMuted : RestaurantEmber.positive,
                            dot: true
                        )
                    }
                },
                kpis: {
                    HStack(spacing: 8) {
                        EmberKpiChip(label: "Open", value: "\(open)", valueColor: RestaurantEmber.ember)
                        EmberKpiChip(label: "Done", value: "\(done)", valueColor: RestaurantEmber.positive)
                        EmberKpiChip(
                            label: "Money in motion",
                            value: RestaurantFormat.compact(model.finance?.received, currency: model.currency, visible: model.financeVisible == true)
                        )
                    }
                }
            )
            lifeCurrentCard(sections.first { $0.code == "L1" })
            lifeSalesCard(sections)
            lifeMoneyCard(sections.first { $0.code == "L3" })
            lifeOpsCard(sections.first { $0.code == "L4" })
        }
    }

    /// AN-L01 · Current Service `2400:30834`
    /// The frame's `Covers 86` has no contract field, so the second chip reports posted bills.
    private func lifeCurrentCard(_ section: OperatingBoardSectionPayload?) -> some View {
        let rows = section?.rows ?? []
        let bills = (model.activity ?? []).filter { $0.eventType == "SALE" }.count
        return EmberCard(tone: .board) {
            EmberBoardHeader(title: "Current Service", onViewAll: { sheet = .lifeCurrent })
            HStack(spacing: 8) {
                EmberKpiChip(label: "Open tickets", value: "\(openOrders.count)", valueColor: RestaurantEmber.ember)
                EmberKpiChip(label: "Posted bills", value: "\(bills)", valueColor: RestaurantEmber.positive)
            }
            if rows.isEmpty {
                EmberEmpty(
                    title: "Nothing recorded yet",
                    body: (section?.detail?.isEmpty == false) ? section!.detail! : "Today's kitchen tickets and posted bills will show here."
                )
            } else {
                VStack(spacing: 8) {
                    ForEach(rows.prefix(3)) { row in
                        boardRowFor(row, trailingOverride: row.eventType == "ORDER" ? restaurantOrderStatusLabel(row.status) : nil)
                    }
                }
            }
            if let coverage = section?.coverage, !coverage.isEmpty {
                EmberSeverityChip(label: coverage, color: RestaurantEmber.warning)
            }
            EmberWhyNote(body: "Today tracks tickets and posted bills — not forecasts.")
        }
    }

    /// AN-L02 · Sales & Orders `2400:30864`
    private func lifeSalesCard(_ sections: [OperatingBoardSectionPayload]) -> some View {
        let activity = model.activity ?? []
        let bills = activity.filter { $0.eventType == "SALE" }
        let creditOpen = (model.paymentReadiness ?? []).filter { $0.relevance == "RECEIVABLE" }.count
        let section = sections.first { $0.code == "L2" }
        return EmberCard(tone: .board) {
            EmberBoardHeader(title: "Sales & Orders", onViewAll: { sheet = .lifeSales })
            HStack(spacing: 8) {
                EmberKpiChip(label: "Bills", value: "\(bills.count)")
                EmberKpiChip(label: "Avg ticket", value: averageTicket(bills), valueColor: RestaurantEmber.textPrimary)
                EmberKpiChip(label: "Credit open", value: "\(creditOpen)", valueColor: RestaurantEmber.ember)
            }
            if let rows = section?.rows, !rows.isEmpty {
                VStack(spacing: 0) {
                    ForEach(rows.prefix(3)) { row in
                        detailRow(
                            eyebrow: row.channelName?.isEmpty == false ? row.channelName! : (row.title.isEmpty ? "Channel" : row.title),
                            value: row.title.isEmpty ? "Recorded" : row.title,
                            trailing: RestaurantFormat.compact(row.amount, currency: model.currency, visible: model.financeVisible == true),
                            trailingColor: RestaurantEmber.textPrimary
                        )
                    }
                }
            } else if let detail = section?.detail, !detail.isEmpty {
                EmberEmpty(title: "Channel mix", body: detail)
            } else {
                EmberEmpty(title: "Nothing recorded yet", body: "Dine-in, takeaway and platform bills will show here.")
            }
            EmberWhyNote(body: "Order value separate from posted sale.")
        }
    }

    /// AN-L03 · Money Moving `2400:30897`
    private func lifeMoneyCard(_ section: OperatingBoardSectionPayload?) -> some View {
        let rows = section?.rows ?? []
        return EmberCard(tone: .board, strokeAlpha: 0.45) {
            EmberBoardHeader(title: "Money Moving", onViewAll: { sheet = .lifeMoney })
            HStack(spacing: 8) {
                EmberKpiChip(
                    label: "Collected",
                    value: RestaurantFormat.compact(model.finance?.received, currency: model.currency, visible: model.financeVisible == true),
                    valueColor: RestaurantEmber.positive
                )
                EmberKpiChip(
                    label: "Paid",
                    value: RestaurantFormat.compact(model.finance?.cashPaid, currency: model.currency, visible: model.financeVisible == true)
                )
                EmberKpiChip(
                    label: "AR move",
                    value: RestaurantFormat.compact(model.finance?.outstanding, currency: model.currency, visible: model.financeVisible == true),
                    valueColor: RestaurantEmber.ember
                )
            }
            if rows.isEmpty {
                EmberEmpty(
                    title: "Nothing moved yet",
                    body: (section?.detail?.isEmpty == false) ? section!.detail! : "Bill collections and supplier payments will show here."
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
            EmberWhyNote(body: "Settlement never reposts the bill.")
        }
    }

    /// AN-L04 · Operations / Stock `2400:30930`
    private func lifeOpsCard(_ section: OperatingBoardSectionPayload?) -> some View {
        let activity = model.activity ?? []
        let receipts = activity.filter { $0.eventType == "PURCHASE" }.count
        let alerts = (model.attention ?? []).filter { $0.kind.uppercased().hasPrefix("LOW_STOCK") }.count
        let rows = section?.rows ?? []
        return EmberCard(tone: .board) {
            EmberBoardHeader(title: "Operations / Stock", onViewAll: { sheet = .lifeOps })
            HStack(spacing: 8) {
                EmberKpiChip(label: "Prep alerts", value: "\(alerts)", valueColor: alerts > 0 ? RestaurantEmber.ember : RestaurantEmber.textPrimary)
                EmberKpiChip(label: "Receipts", value: "\(receipts)")
            }
            if rows.isEmpty {
                EmberEmpty(
                    title: "Nothing moving",
                    body: (section?.detail?.isEmpty == false) ? section!.detail! : "Ingredient receipts, counts and waste will show here."
                )
            } else {
                VStack(spacing: 8) {
                    ForEach(rows.prefix(3)) { row in
                        boardRowFor(row, trailingOverride: row.eventType == "STOCK" ? "Logged" : "Open")
                    }
                }
            }
            EmberWhyNote(body: "No automatic recipe deduction.")
        }
    }

    // MARK: - Memory — RST-MEMORY-v12 `2400:30984`

    private var memory: some View {
        let target = model.target
        let percent = target?.configured == true ? RestaurantFormat.percent(target?.progress, target?.value) : nil
        let period = RestaurantFormat.month(model.startDate)
        let sections = model.boards?.memory ?? []
        let revisions = model.memory?.revisions ?? []
        let bills = (model.activity ?? []).filter { $0.eventType == "SALE" }.count

        return VStack(alignment: .leading, spacing: RestaurantEmber.sectionGap) {
            EmberBoardHero(
                eyebrow: "MEMORY · THIS MONTH",
                headline: "\((model.title?.isEmpty == false) ? model.title! : "Restaurant") · memory",
                strokeAlpha: 0.45,
                chips: {
                    HStack(spacing: 8) {
                        EmberBoardHeroChip(
                            text: percent != nil
                                ? "\(period) · \(percent!) of \(RestaurantFormat.compact(target?.value, currency: model.currency, visible: model.financeVisible == true))"
                                : "\(period) · memory",
                            color: RestaurantEmber.ember
                        )
                        if !revisions.isEmpty {
                            EmberBoardHeroChip(text: "\(revisions.count) revisions", color: RestaurantEmber.textMuted)
                        }
                    }
                },
                kpis: {
                    HStack(spacing: 8) {
                        EmberKpiChip(
                            label: "Revenue",
                            value: RestaurantFormat.compact(model.finance?.sales, currency: model.currency, visible: model.financeVisible == true)
                        )
                        EmberKpiChip(
                            label: "vs last period",
                            value: lastPeriodDelta(sections),
                            valueColor: RestaurantEmber.warning
                        )
                        EmberKpiChip(label: "Bills", value: "\(bills)")
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
            if momentId != nil {
                storyCta(month: period)
            }
        }
    }

    /// AN-H01 · Monthly Memory `2400:30997`
    private func monthlyMemoryCard(_ section: OperatingBoardSectionPayload?) -> some View {
        let months = (section?.rows ?? []).filter {
            $0.eventType == "MONTH" || $0.eventType.isEmpty || $0.eventType == "REVISION"
        }
        let average = averageAchievement(months)
        return EmberCard(tone: .board) {
            EmberBoardHeader(title: "Monthly Memory", onViewAll: { sheet = .monthly })
            HStack(spacing: 8) {
                EmberKpiChip(label: "Months", value: "\(months.count)")
                EmberKpiChip(
                    label: "Avg achv.",
                    value: average,
                    valueColor: average == "Unavailable" ? RestaurantEmber.warning : RestaurantEmber.amber
                )
            }
            if months.isEmpty {
                EmberEmpty(
                    title: "No history yet",
                    body: (section?.detail?.isEmpty == false) ? section!.detail! : "Closed months will show here."
                )
            } else {
                VStack(spacing: 0) {
                    ForEach(months.prefix(3)) { row in
                        let parts = row.title.components(separatedBy: "·").map { $0.trimmingCharacters(in: .whitespaces) }
                        detailRow(
                            eyebrow: parts.first.flatMap { $0.isEmpty ? nil : $0 } ?? RestaurantFormat.month(row.occurredAt),
                            value: parts.dropFirst().joined(separator: " · "),
                            trailing: "Recorded",
                            trailingColor: RestaurantEmber.textBody
                        )
                    }
                }
            }
            if let coverage = section?.coverage, !coverage.isEmpty {
                EmberSeverityChip(label: coverage, color: RestaurantEmber.warning)
            }
            EmberWhyNote(body: "Missing coverage stays unavailable — never averaged as zero.")
        }
    }

    /// AN-H02 · Money Memory `2400:31027`
    private func moneyMemoryCard(_ section: OperatingBoardSectionPayload) -> some View {
        let revisions = model.memory?.revisions ?? []
        let rows = section.rows ?? []
        return EmberCard(tone: .board, strokeAlpha: 0.50) {
            EmberBoardHeader(title: "Money Memory", onViewAll: { sheet = .moneyMemory })
            HStack(spacing: 8) {
                EmberKpiChip(
                    label: "Billed",
                    value: RestaurantFormat.compact(model.finance?.sales, currency: model.currency, visible: model.financeVisible == true)
                )
                EmberKpiChip(
                    label: "Collected",
                    value: RestaurantFormat.compact(model.finance?.received, currency: model.currency, visible: model.financeVisible == true),
                    valueColor: RestaurantEmber.positive
                )
                EmberKpiChip(
                    label: "Paid",
                    value: RestaurantFormat.compact(model.finance?.cashPaid, currency: model.currency, visible: model.financeVisible == true),
                    valueColor: RestaurantEmber.ember
                )
            }
            if let revision = revisions.first {
                detailRow(
                    eyebrow: "Plan revision",
                    value: revision.title.isEmpty ? "Target revision" : revision.title,
                    trailing: RestaurantFormat.shortDay(revision.occurredAt) ?? "Recorded",
                    trailingColor: RestaurantEmber.amber
                )
            }
            if rows.isEmpty {
                EmberEmpty(
                    title: "Nothing settled yet",
                    body: section.detail.isEmpty ? "Platform settlements and supplier payments will show here." : section.detail
                )
            } else {
                VStack(spacing: 0) {
                    ForEach(rows.prefix(3)) { row in
                        detailRow(
                            eyebrow: row.title.isEmpty ? restaurantEventLabel(row.eventType) : row.title,
                            value: row.counterpartyName?.isEmpty == false ? row.counterpartyName! : restaurantEventLabel(row.eventType),
                            trailing: RestaurantFormat.compact(row.amount, currency: model.currency, visible: model.financeVisible == true),
                            trailingColor: RestaurantEmber.textPrimary
                        )
                    }
                }
            }
            EmberWhyNote(body: "Reversals versioned — never silent overwrite.")
        }
    }

    /// AN-H03 · People & Activity `2400:31060`
    private func peopleMemoryCard(_ section: OperatingBoardSectionPayload) -> some View {
        let shifts = (model.activity ?? []).filter { $0.eventType == "SHIFT" }.count
        let rows = section.rows ?? []
        return EmberCard(tone: .board) {
            EmberBoardHeader(title: "People & Activity", trailingNote: "Shifts", onViewAll: { sheet = .peopleMemory })
            HStack(spacing: 8) {
                EmberKpiChip(label: "People", value: "\((model.participants ?? []).count)")
                EmberKpiChip(
                    label: "Shifts",
                    value: shifts > 0 ? "\(shifts)" : "Unavailable",
                    valueColor: shifts > 0 ? RestaurantEmber.textPrimary : RestaurantEmber.warning
                )
            }
            if rows.isEmpty {
                EmberEmpty(
                    title: "Nothing recorded yet",
                    body: section.detail.isEmpty ? "Who worked which station will show here." : section.detail
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

    /// AN-H04 · Patterns & Decisions `2400:31087`
    private func patternsCard(_ section: OperatingBoardSectionPayload) -> some View {
        let activity = model.activity ?? []
        let decisions = activity.filter { $0.eventType == "APPROVAL" }.count
        let approved = activity.filter { $0.eventType == "APPROVAL" && $0.approvalStatus == "APPROVED" }.count
        let rows = section.rows ?? []
        return EmberCard(tone: .board) {
            EmberBoardHeader(title: "Patterns & Decisions", onViewAll: { sheet = .patterns })
            HStack(spacing: 8) {
                EmberKpiChip(label: "Decisions", value: "\(decisions)")
                EmberKpiChip(label: "Approved", value: "\(approved)", valueColor: RestaurantEmber.positive)
            }
            if rows.isEmpty {
                EmberEmpty(
                    title: "No patterns yet",
                    body: section.detail.isEmpty ? "Approvals and repeated kitchen issues will show here." : section.detail
                )
            } else {
                VStack(spacing: 8) {
                    ForEach(rows.prefix(3)) { row in
                        let isApproved = row.approvalStatus == "APPROVED"
                        let denied = row.approvalStatus == "REJECTED"
                        boardRowFor(
                            row,
                            accent: isApproved ? RestaurantEmber.positive : (denied ? RestaurantEmber.negative : RestaurantEmber.amber),
                            trailingOverride: isApproved ? "Approved" : (denied ? "Denied" : "Open")
                        )
                    }
                }
            }
            EmberWhyNote(body: "Approval ≠ disbursement.")
        }
    }

    /// Ember story CTA — opens `RST-STORY-CONFIG` `2671:32352`.
    private func storyCta(month: String) -> some View {
        Button { sheet = .storyConfig } label: {
            VStack(alignment: .leading, spacing: 2) {
                HStack(spacing: 6) {
                    Image(RestaurantTileIcons.plus)
                        .renderingMode(.template)
                        .resizable()
                        .scaledToFit()
                        .frame(width: 15, height: 15)
                        .foregroundStyle(RestaurantEmber.inkOnEmber)
                    Text("Generate Business Story")
                        .font(.plusJakarta(size: 15, weight: .heavy))
                        .foregroundStyle(RestaurantEmber.inkOnEmber)
                        .lineLimit(1)
                }
                Text("Eight pages from recorded activity · \(month) preselected")
                    .font(.plusJakarta(size: 11, weight: .medium))
                    .foregroundStyle(RestaurantEmber.inkOnEmber.opacity(0.8))
                    .lineLimit(1)
                    .minimumScaleFactor(0.8)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, 18)
            .padding(.vertical, 14)
            .background(RestaurantEmber.ember)
            .clipShape(RoundedRectangle(cornerRadius: RestaurantEmber.rStoryCta, style: .continuous))
            .shadow(color: RestaurantEmber.ember.opacity(0.4), radius: 12, x: 0, y: 8)
        }
        .buttonStyle(.plain)
    }

    // MARK: - Shared row shapes

    private func boardRowFor(
        _ row: OperatingActivityPayload,
        accent: Color = RestaurantEmber.ember,
        trailingOverride: String? = nil
    ) -> some View {
        let trailing = trailingOverride ?? row.paymentStatus.map(restaurantPrettyStatus) ?? "Open"
        let color: Color
        switch trailing {
        case "Done", "Settled", "Approved", "Served", "Ready": color = RestaurantEmber.positive
        case "Denied", "Void": color = RestaurantEmber.negative
        case "Open", "Pending", "Firing", "Sent": color = RestaurantEmber.amber
        default:
            color = ["COLLECTED", "PAID", "SETTLED"].contains(row.paymentStatus ?? "")
                ? RestaurantEmber.positive
                : RestaurantEmber.textBody
        }
        var captionParts: [String] = []
        if let name = row.counterpartyName, !name.isEmpty { captionParts.append(name) }
        if let channel = row.channelName, !channel.isEmpty { captionParts.append(channel) }
        if let quantity = row.quantity, !quantity.isEmpty {
            captionParts.append(row.unit.map { "\(quantity) \($0)" } ?? quantity)
        }
        if captionParts.isEmpty { captionParts.append(restaurantEventLabel(row.eventType)) }

        return EmberBoardRow(
            eyebrow: row.title.isEmpty ? restaurantEventLabel(row.eventType) : row.title,
            value: captionParts.joined(separator: " · "),
            trailing: trailing,
            trailingColor: color,
            accent: accent,
            onTap: { OperatingChromeState.shared.editing = row }
        )
    }

    private func detailRow(
        eyebrow: String,
        value: String,
        trailing: String,
        trailingColor: Color = RestaurantEmber.textPrimary,
        accent: Color = RestaurantEmber.ember
    ) -> some View {
        EmberBoardRow(
            eyebrow: eyebrow,
            value: value,
            trailing: trailing,
            trailingColor: trailingColor,
            accent: accent
        )
    }

    // MARK: - Derived facts

    private var orderRows: [OperatingActivityPayload] {
        (model.activity ?? []).filter { $0.eventType == "ORDER" || $0.eventType == "CUSTOMER_ORDER" }
    }

    private var openOrders: [OperatingActivityPayload] {
        orderRows.filter { !["COMPLETED", "CANCELLED"].contains(($0.status ?? "").uppercased()) }
    }

    private var firingOrders: [OperatingActivityPayload] {
        orderRows.filter { ["PREPARING", "READY"].contains(($0.status ?? "").uppercased()) }
    }

    private var stationMasters: [OperatingMasterPayload] {
        (model.masters ?? []).filter { $0.kind.uppercased() == "STATION" }
    }

    /// `₹3,265` when at least one bill carries an amount, otherwise `Unavailable`.
    private func averageTicket(_ bills: [OperatingActivityPayload]) -> String {
        let amounts = bills.compactMap { $0.amount.flatMap { Decimal(string: $0) } }
        guard !amounts.isEmpty else { return "Unavailable" }
        let total = amounts.reduce(Decimal(0), +)
        let average = total / Decimal(amounts.count)
        return RestaurantFormat.exact("\(average)", currency: model.currency, visible: model.financeVisible == true)
    }

    private var dayOfPeriod: String {
        guard let start = RestaurantFormat.parse(model.startDate) else { return "Unavailable" }
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
        OperatingMomentCatalog.roles.first { $0.0 == code }?.1 ?? "Restaurant"
    }
}

private extension String {
    func takeUnlessRestaurant(_ other: String) -> String? {
        self == other ? nil : self
    }
}

import SwiftUI

/// Ember bottom sheet for every `View all ›` route — the 16 `RST-SHEET · View all · *`
/// frames under `2487:37611` (e.g. `2482:29487`, `2482:29639`, `2482:30978`, `2482:31573`).
///
/// Rows come straight from the projection. A board with no rows says so instead of
/// inventing a placeholder metric, and money the role cannot see stays `—`.
struct RestaurantBoardSheetView: View {
    let route: RestaurantSheetRoute
    let model: OperatingProjectionPayload
    var onRange: (RestaurantSheetRoute) -> Void = { _ in }
    let onClose: () -> Void

    var body: some View {
        EmberScreenBackground {
            ScrollView {
                VStack(alignment: .leading, spacing: 12) {
                    HStack {
                        Spacer()
                        Capsule()
                            .fill(RestaurantEmber.ember.opacity(0.55))
                            .frame(width: 44, height: 4)
                        Spacer()
                    }
                    .padding(.top, 8)

                    HStack(spacing: 10) {
                        Text(route.title)
                            .font(.plusJakarta(size: 20, weight: .bold))
                            .foregroundStyle(RestaurantEmber.textPrimary)
                            .lineLimit(1)
                            .minimumScaleFactor(0.7)
                            .frame(maxWidth: .infinity, alignment: .leading)
                        Button(action: onClose) {
                            Text("✕")
                                .font(.plusJakarta(size: 14, weight: .bold))
                                .foregroundStyle(RestaurantEmber.textBody)
                                .frame(width: 35, height: 32)
                                .background(Color.white.opacity(0.06))
                                .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                        }
                        .buttonStyle(.plain)
                        .accessibilityLabel("Close")
                    }

                    Text("\(model.title ?? "Restaurant") · \(RestaurantFormat.month(model.startDate)) · full activity, newest first")
                        .font(.plusJakarta(size: 12))
                        .foregroundStyle(RestaurantEmber.textBody)
                        .lineLimit(2)

                    HStack(spacing: 6) {
                        Button { onRange(.monthPicker) } label: { OutlineEmberChip(text: "This month") }
                            .buttonStyle(.plain)
                        OutlineEmberChip(text: "Last month")
                        Button { onRange(.datePicker) } label: { OutlineEmberChip(text: "Custom range ›") }
                            .buttonStyle(.plain)
                        Spacer(minLength: 0)
                    }

                    content
                    Spacer(minLength: 24)
                }
                .padding(.horizontal, RestaurantEmber.gutter)
                .padding(.bottom, 24)
            }
        }
        .presentationDragIndicator(.hidden)
    }

    @ViewBuilder
    private var content: some View {
        switch route {
        case .attention:
            attentionRows
        case .payment:
            paymentRows(receivables: false)
        case .finance:
            paymentRows(receivables: true)
        case .activity, .lifeSales:
            activityRows
        case .people:
            peopleRows
        case .target:
            targetRows
        case .gst:
            gstRows
        case .storyConfig:
            EmberEmpty(title: "Open from Memory", body: "Generate the story from the Memory tab.")
        case .datePicker, .monthPicker, .timePicker:
            EmberEmpty(title: "Picker", body: "This range chip opens the picker sheet.")
        default:
            if let code = route.boardCode {
                sectionRows(code: code)
            } else {
                EmberEmpty(title: "Not available", body: "This board has not been recorded yet.")
            }
        }
    }

    private var attentionRows: some View {
        let items = model.attention ?? []
        return Group {
            if items.isEmpty {
                EmberEmpty(title: "Nothing is waiting", body: "Approvals, overdue bills and blockers will show here.")
            } else {
                VStack(spacing: 8) {
                    ForEach(items) { item in
                        let hasSource = item.sourceEventId != nil || item.id.hasPrefix("open-") || item.cta != nil
                        let label = restaurantSeverityLabel(kind: item.kind, severity: item.severity, hasSource: hasSource)
                        let color = hasSource ? restaurantSeverityColor(item.severity) : RestaurantEmber.warning
                        EmberAccentRow(
                            title: item.title,
                            caption: [
                                item.counterpartyName,
                                RestaurantFormat.shortDay(item.dueAt).map { "Due \($0)" },
                                RestaurantFormat.compact(item.amount, currency: model.currency, visible: model.financeVisible == true),
                            ]
                            .compactMap { $0 }
                            .filter { !$0.isEmpty && $0 != "—" }
                            .joined(separator: " · "),
                            accent: color,
                            severityLabel: label,
                            severityColor: color,
                            onTap: { openOperatingAttention(item, model: model, onOpenQuickAdd: {}) }
                        )
                    }
                }
            }
        }
    }

    private func paymentRows(receivables: Bool) -> some View {
        let rows = (model.paymentReadiness ?? []).filter {
            receivables ? $0.relevance == "RECEIVABLE" : $0.relevance != "RECEIVABLE"
        }
        return Group {
            if rows.isEmpty {
                EmberEmpty(
                    title: receivables ? "Nothing to collect" : "Nothing to pay",
                    body: receivables
                        ? "Platform and credit bills waiting on settlement will show here."
                        : "Ingredient bills waiting to settle will show here."
                )
            } else {
                VStack(spacing: 8) {
                    ForEach(rows) { row in
                        EmberAccentRow(
                            title: row.title.isEmpty ? (row.counterpartyName ?? "Open balance") : row.title,
                            caption: [
                                row.counterpartyName,
                                RestaurantFormat.shortDay(row.dueAt).map { "Due \($0)" },
                                row.status.lowercased().capitalized,
                            ]
                            .compactMap { $0 }
                            .filter { !$0.isEmpty }
                            .joined(separator: " · "),
                            accent: dueColor(row.dueBucket),
                            trailingValue: RestaurantFormat.compact(row.amount, currency: model.currency, visible: model.financeVisible == true),
                            trailingColor: row.dueBucket == "OVERDUE" ? RestaurantEmber.negative : RestaurantEmber.textPrimary,
                            onTap: { OperatingChromeState.shared.settling = row }
                        )
                    }
                }
            }
        }
    }

    private func dueColor(_ bucket: String?) -> Color {
        switch bucket {
        case "OVERDUE": return RestaurantEmber.negative
        case "DUE_SOON": return RestaurantEmber.warning
        default: return RestaurantEmber.ember
        }
    }

    private var activityRows: some View {
        let rows = model.activity ?? []
        return Group {
            if rows.isEmpty {
                EmberEmpty(title: "Nothing recorded yet", body: "Bills, kitchen tickets and settlement will show here.")
            } else {
                VStack(spacing: 0) {
                    ForEach(rows) { row in
                        EmberDividedRow(
                            title: row.title.isEmpty ? restaurantEventLabel(row.eventType) : row.title,
                            caption: [
                                row.counterpartyName ?? row.channelName,
                                row.quantity.map { qty in row.unit.map { u in "\(qty) \(u)" } ?? qty },
                            ]
                            .compactMap { $0 }
                            .filter { !$0.isEmpty }
                            .joined(separator: " · "),
                            trailingValue: (row.eventType == "ORDER" || row.eventType == "CUSTOMER_ORDER")
                                ? restaurantOrderStatusLabel(row.status)
                                : (row.paymentStatus.map(restaurantPrettyStatus) ?? (row.approvalStatus == "PENDING" ? "Pending" : "Open")),
                            trailingColor: ["COLLECTED", "PAID", "SETTLED"].contains(row.paymentStatus ?? "")
                                ? RestaurantEmber.positive
                                : RestaurantEmber.textBody,
                            onTap: { OperatingChromeState.shared.editing = row }
                        )
                    }
                }
            }
        }
    }

    private var peopleRows: some View {
        let names = model.participants ?? []
        return Group {
            if names.isEmpty {
                EmberEmpty(title: "No people yet", body: "Invite the kitchen, floor and billing team.")
            } else {
                VStack(spacing: 0) {
                    ForEach(names, id: \.self) { name in
                        EmberDividedRow(
                            title: name,
                            caption: name == model.partyName
                                ? (OperatingMomentCatalog.roles.first { $0.0 == model.role }?.1 ?? "Owner / Admin")
                                : "Member",
                            trailingValue: name == model.partyName ? "Owner" : "Member",
                            trailingColor: RestaurantEmber.textBody
                        )
                    }
                }
            }
        }
    }

    private var targetRows: some View {
        let target = model.target
        let budget = model.budget
        let targetConfigured = target?.configured == true
        let budgetConfigured = budget?.configured == true
        return VStack(spacing: 8) {
            EmberBoardRow(
                eyebrow: "Sales vs target",
                value: targetConfigured
                    ? "\(RestaurantFormat.compact(target?.progress, currency: model.currency, visible: model.financeVisible == true)) of \(RestaurantFormat.compact(target?.value, currency: model.currency, visible: model.financeVisible == true))"
                    : "No target set",
                trailing: targetConfigured ? (RestaurantFormat.percent(target?.progress, target?.value) ?? "Unavailable") : "Unavailable",
                trailingColor: RestaurantEmber.amber
            )
            EmberBoardRow(
                eyebrow: "Eligible spend",
                value: "Ingredients + running",
                trailing: RestaurantFormat.compact(model.finance?.eligibleCost, currency: model.currency, visible: model.financeVisible == true)
            )
            EmberBoardRow(
                eyebrow: "Budget",
                value: budgetConfigured
                    ? "\(RestaurantFormat.compact(budget?.spent, currency: model.currency, visible: true)) of \(RestaurantFormat.compact(budget?.amount, currency: model.currency, visible: true))"
                    : "No period budget",
                trailing: budgetConfigured ? (RestaurantFormat.percent(budget?.spent, budget?.amount) ?? "Unavailable") : "Unavailable"
            )
            EmberBoardRow(
                eyebrow: "Budget remaining",
                value: "After eligible spend",
                trailing: budgetConfigured
                    ? RestaurantFormat.compact(budget?.remaining, currency: model.currency, visible: true)
                    : "Unavailable",
                trailingColor: budgetConfigured ? RestaurantEmber.positive : RestaurantEmber.warning
            )
            EmberWhyCaption(body: "Achievement = posted bills ÷ effective target. Missing coverage shows Unavailable — never invent 0%.")
        }
    }

    private var gstRows: some View {
        let gst = model.gstPosition
        return VStack(spacing: 0) {
            EmberDividedRow(
                title: "GST on sales",
                caption: "Outward taxable",
                trailingValue: RestaurantFormat.compact(gst?.outputGst, currency: model.currency)
            )
            EmberDividedRow(
                title: "GST on purchases",
                caption: "Inward eligible",
                trailingValue: RestaurantFormat.compact(gst?.inputGst, currency: model.currency),
                trailingColor: RestaurantEmber.positive
            )
            EmberDividedRow(
                title: "Estimated difference",
                caption: RestaurantFormat.asOf(gst?.asOfMonth.map { "\($0)-01" }),
                trailingValue: RestaurantFormat.compact(gst?.difference, currency: model.currency),
                trailingColor: RestaurantEmber.amber
            )
            EmberWhyCaption(body: "Indicative only. This is not a tax filing or an ITC claim.")
        }
    }

    private func sectionRows(code: String) -> some View {
        let sections = (model.boards?.moments ?? []) + (model.boards?.life ?? []) + (model.boards?.memory ?? [])
        guard let section = sections.first(where: { $0.code == code }) else {
            return AnyView(EmberEmpty(title: "Not available", body: "This board has not been recorded yet."))
        }
        return AnyView(
            VStack(alignment: .leading, spacing: 10) {
                if !section.detail.isEmpty {
                    EmberCard(tone: .board) {
                        Text(RestaurantFormat.detail(section.detail, currency: model.currency))
                            .font(.plusJakarta(size: 11))
                            .foregroundStyle(RestaurantEmber.textBody)
                            .fixedSize(horizontal: false, vertical: true)
                        if let coverage = section.coverage, !coverage.isEmpty {
                            EmberSeverityChip(label: coverage, color: RestaurantEmber.warning)
                        }
                    }
                }
                if let rows = section.rows, !rows.isEmpty {
                    VStack(spacing: 8) {
                        ForEach(rows) { row in
                            EmberBoardRow(
                                eyebrow: row.title.isEmpty ? restaurantEventLabel(row.eventType) : row.title,
                                value: [
                                    row.counterpartyName,
                                    row.channelName,
                                    row.quantity.map { qty in row.unit.map { u in "\(qty) \(u)" } ?? qty },
                                ]
                                .compactMap { $0 }
                                .filter { !$0.isEmpty }
                                .joined(separator: " · "),
                                trailing: (row.eventType == "ORDER" || row.eventType == "CUSTOMER_ORDER")
                                    ? restaurantOrderStatusLabel(row.status)
                                    : RestaurantFormat.compact(row.amount, currency: model.currency, visible: model.financeVisible == true),
                                trailingColor: RestaurantEmber.textPrimary,
                                onTap: { OperatingChromeState.shared.editing = row }
                            )
                        }
                    }
                } else {
                    EmberEmpty(title: "Nothing recorded yet", body: "Rows will appear as activity is recorded.")
                }
            }
        )
    }
}

// MARK: - Route

/// Which board a `View all ›` opens (the 16 `RST-SHEET · View all · *` frames).
enum RestaurantSheetRoute: String, Identifiable {
    case attention, target, payment, finance, activity, gst
    case storyConfig
    case commitments, momentDetail, people, network
    case lifeCurrent, lifeSales, lifeMoney, lifeOps
    case monthly, moneyMemory, peopleMemory, patterns
    case datePicker, monthPicker, timePicker

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
        case .momentDetail: return "My Restaurant"
        case .people: return "People"
        case .network: return "Business Network"
        case .lifeCurrent: return "Current Service"
        case .lifeSales: return "Sales & Orders"
        case .lifeMoney: return "Money Moving"
        case .lifeOps: return "Operations / Stock"
        case .monthly: return "Monthly Memory"
        case .moneyMemory: return "Money Memory"
        case .peopleMemory: return "People & Activity"
        case .patterns: return "Patterns & Decisions"
        case .storyConfig: return "Create Business Story"
        case .datePicker: return "Select date"
        case .monthPicker: return "Select month"
        case .timePicker: return "Due date & time"
        }
    }

    /// Picker frame this route opens, when it is one of the three `2521:*` frames.
    var pickerMode: EmberPickerSheet.Mode? {
        switch self {
        case .datePicker: return .date
        case .monthPicker: return .month
        case .timePicker: return .dateTime
        default: return nil
        }
    }

    /// Board section code shown by this route, when it maps to one.
    var boardCode: String? {
        switch self {
        case .commitments: return "M4"
        case .momentDetail: return "M1"
        case .network: return "M3"
        case .lifeCurrent: return "L1"
        case .lifeSales: return "L2"
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

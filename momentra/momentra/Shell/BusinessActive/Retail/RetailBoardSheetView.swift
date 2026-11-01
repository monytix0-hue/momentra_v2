import SwiftUI

/// Rose bottom sheet for every `View all ›` route (the 16 boards from the plan).
///
/// Rows come straight from the projection — a board with no rows says so instead of
/// inventing a placeholder metric.
struct RetailBoardSheetView: View {
    let route: RetailSheetRoute
    let model: OperatingProjectionPayload
    let onClose: () -> Void

    var body: some View {
        RoseScreenBackground {
            ScrollView {
                VStack(alignment: .leading, spacing: 12) {
                    HStack {
                        Spacer()
                        Capsule()
                            .fill(Color.white.opacity(0.2))
                            .frame(width: 40, height: 5)
                        Spacer()
                    }
                    .padding(.top, 8)

                    HStack(spacing: 10) {
                        Text(route.title)
                            .font(.plusJakarta(size: 24, weight: .bold))
                            .foregroundStyle(RetailRose.textPrimary)
                            .lineLimit(1)
                            .minimumScaleFactor(0.7)
                            .frame(maxWidth: .infinity, alignment: .leading)
                        Button("Close", action: onClose)
                            .font(.plusJakarta(size: 12, weight: .bold))
                            .foregroundStyle(RetailRose.textBody)
                            .padding(.horizontal, 10)
                            .padding(.vertical, 6)
                    }

                    Text(RetailFormat.periodLine(model.startDate, currency: model.currency))
                        .font(.plusJakarta(size: 12, weight: .medium))
                        .foregroundStyle(RetailRose.amber)
                        .lineLimit(1)

                    content
                    Spacer(minLength: 24)
                }
                .padding(.horizontal, RetailRose.gutter)
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
        default:
            if let code = route.boardCode {
                sectionRows(code: code)
            } else {
                RoseEmpty(title: "Not available", body: "This board has not been recorded yet.")
            }
        }
    }

    private var attentionRows: some View {
        let items = model.attention ?? []
        return Group {
            if items.isEmpty {
                RoseEmpty(title: "Nothing is waiting", body: "Approvals, overdue items and blockers will show here.")
            } else {
                VStack(spacing: 8) {
                    ForEach(items) { item in
                        let hasSource = item.sourceEventId != nil || item.id.hasPrefix("open-") || item.cta != nil
                        let label = retailSeverityLabel(kind: item.kind, severity: item.severity, hasSource: hasSource)
                        let color = hasSource ? retailSeverityColor(item.severity) : RetailRose.warning
                        AccentRow(
                            title: item.title,
                            caption: [
                                item.counterpartyName,
                                RetailFormat.shortDay(item.dueAt).map { "Due \($0)" },
                                RetailFormat.compact(item.amount, currency: model.currency, visible: model.financeVisible == true)
                            ]
                            .compactMap { $0 }
                            .filter { !$0.isEmpty && $0 != "—" }
                            .joined(separator: " · "),
                            accent: color,
                            severityLabel: label,
                            severityColor: color,
                            onTap: {
                                openOperatingAttention(item, model: model, onOpenQuickAdd: {})
                            }
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
                RoseEmpty(
                    title: receivables ? "Nothing to collect" : "Nothing to pay",
                    body: receivables
                        ? "Credit sales waiting on collection will show here."
                        : "Purchases waiting to settle will show here."
                )
            } else {
                VStack(spacing: 8) {
                    ForEach(rows) { row in
                        AccentRow(
                            title: row.title.isEmpty ? (row.counterpartyName ?? "Open balance") : row.title,
                            caption: [
                                row.counterpartyName,
                                RetailFormat.shortDay(row.dueAt).map { "Due \($0)" },
                                row.status.lowercased().capitalized
                            ]
                            .compactMap { $0 }
                            .filter { !$0.isEmpty }
                            .joined(separator: " · "),
                            accent: dueColor(row.dueBucket),
                            trailingValue: RetailFormat.compact(row.amount, currency: model.currency, visible: model.financeVisible == true),
                            trailingColor: row.dueBucket == "OVERDUE" ? RetailRose.negative : RetailRose.textPrimary,
                            onTap: { OperatingChromeState.shared.settling = row }
                        )
                    }
                }
            }
        }
    }

    private func dueColor(_ bucket: String?) -> Color {
        switch bucket {
        case "OVERDUE": return RetailRose.negative
        case "DUE_SOON": return RetailRose.warning
        default: return RetailRose.rose
        }
    }

    private var activityRows: some View {
        let rows = model.activity ?? []
        return Group {
            if rows.isEmpty {
                RoseEmpty(title: "Nothing recorded yet", body: "Sales, spend and settlement will show here.")
            } else {
                VStack(spacing: 0) {
                    ForEach(rows) { row in
                        DividedRow(
                            title: row.title.isEmpty ? retailEventLabel(row.eventType) : row.title,
                            caption: [
                                row.counterpartyName ?? row.channelName,
                                row.quantity.map { qty in row.unit.map { u in "\(qty) \(u)" } ?? qty }
                            ]
                            .compactMap { $0 }
                            .filter { !$0.isEmpty }
                            .joined(separator: " · "),
                            trailingValue: row.paymentStatus.map(retailPrettyStatus) ?? (row.approvalStatus == "PENDING" ? "Pending" : "Open"),
                            trailingColor: ["COLLECTED", "PAID", "SETTLED"].contains(row.paymentStatus ?? "")
                                ? RetailRose.positive
                                : RetailRose.textBody,
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
                RoseEmpty(title: "No people yet", body: "Invite the shop floor, billing and approvers.")
            } else {
                VStack(spacing: 0) {
                    ForEach(names, id: \.self) { name in
                        DividedRow(
                            title: name,
                            caption: name == model.partyName
                                ? (OperatingMomentCatalog.roles.first { $0.0 == model.role }?.1 ?? "Owner / Admin")
                                : "Member",
                            trailingValue: name == model.partyName ? "Owner" : "Member",
                            trailingColor: RetailRose.textBody
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
            BoardRow(
                eyebrow: "Sales vs target",
                value: targetConfigured
                    ? "\(RetailFormat.compact(target?.progress, currency: model.currency, visible: model.financeVisible == true)) of \(RetailFormat.compact(target?.value, currency: model.currency, visible: model.financeVisible == true))"
                    : "No target set",
                trailing: targetConfigured ? (RetailFormat.percent(target?.progress, target?.value) ?? "Unavailable") : "Unavailable",
                trailingColor: RetailRose.amber
            )
            BoardRow(
                eyebrow: "Budget",
                value: budgetConfigured
                    ? "\(RetailFormat.compact(budget?.spent, currency: model.currency, visible: true)) of \(RetailFormat.compact(budget?.amount, currency: model.currency, visible: true))"
                    : "No period budget",
                trailing: budgetConfigured ? (RetailFormat.percent(budget?.spent, budget?.amount) ?? "Unavailable") : "Unavailable"
            )
            BoardRow(
                eyebrow: "Budget remaining",
                value: "After eligible spend",
                trailing: budgetConfigured
                    ? RetailFormat.compact(budget?.remaining, currency: model.currency, visible: true)
                    : "Unavailable",
                trailingColor: budgetConfigured ? RetailRose.positive : RetailRose.warning
            )
            RoseWhyCaption(body: "Achievement = eligible actual ÷ effective target. Missing coverage shows Unavailable — never invent 0%.")
        }
    }

    private var gstRows: some View {
        let gst = model.gstPosition
        return VStack(spacing: 0) {
            DividedRow(
                title: "GST on sales",
                caption: "Outward taxable",
                trailingValue: RetailFormat.compact(gst?.outputGst, currency: model.currency)
            )
            DividedRow(
                title: "GST on purchases",
                caption: "Inward eligible",
                trailingValue: RetailFormat.compact(gst?.inputGst, currency: model.currency),
                trailingColor: RetailRose.positive
            )
            DividedRow(
                title: "Estimated difference",
                caption: RetailFormat.asOf(gst?.asOfMonth.map { "\($0)-01" }),
                trailingValue: RetailFormat.compact(gst?.difference, currency: model.currency),
                trailingColor: RetailRose.amber
            )
            RoseWhyCaption(body: "Indicative only. This is not a tax filing or an ITC claim.")
        }
    }

    private func sectionRows(code: String) -> some View {
        let sections = (model.boards?.moments ?? []) + (model.boards?.life ?? []) + (model.boards?.memory ?? [])
        guard let section = sections.first(where: { $0.code == code }) else {
            return AnyView(RoseEmpty(title: "Not available", body: "This board has not been recorded yet."))
        }
        return AnyView(
            VStack(alignment: .leading, spacing: 10) {
                if !section.detail.isEmpty {
                    RoseCard(tone: .board) {
                        Text(RetailFormat.detail(section.detail, currency: model.currency))
                            .font(.plusJakarta(size: 11))
                            .foregroundStyle(RetailRose.textBody)
                            .fixedSize(horizontal: false, vertical: true)
                        if let coverage = section.coverage, !coverage.isEmpty {
                            SeverityChip(label: coverage, color: RetailRose.warning)
                        }
                    }
                }
                if let rows = section.rows, !rows.isEmpty {
                    VStack(spacing: 8) {
                        ForEach(rows) { row in
                            BoardRow(
                                eyebrow: row.title.isEmpty ? retailEventLabel(row.eventType) : row.title,
                                value: [
                                    row.counterpartyName,
                                    row.channelName,
                                    row.quantity.map { qty in row.unit.map { u in "\(qty) \(u)" } ?? qty }
                                ]
                                .compactMap { $0 }
                                .filter { !$0.isEmpty }
                                .joined(separator: " · "),
                                trailing: RetailFormat.compact(row.amount, currency: model.currency, visible: model.financeVisible == true),
                                trailingColor: RetailRose.textPrimary,
                                onTap: { OperatingChromeState.shared.editing = row }
                            )
                        }
                    }
                } else {
                    RoseEmpty(title: "Nothing recorded yet", body: "Rows will appear as activity is recorded.")
                }
            }
        )
    }
}

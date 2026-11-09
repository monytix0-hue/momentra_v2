import SwiftUI

/// One data-driven `View all ›` sheet for all eighteen surfaces of a vertical.
///
/// The Pulse surfaces read a metric group; the twelve board surfaces read their section. Both
/// come from the same projection, so this sheet never holds state of its own beyond the load.
struct OperatingViewAllSheet: View {
    let spec: OperatingSheetSpec
    let theme: OperatingTheme
    @ObservedObject var model: BusinessOperatingModel
    var onOpenRow: ((OperatingActivityPayload) -> Void)? = nil

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                HStack(spacing: 10) {
                    Text(spec.title)
                        .font(.plusJakarta(size: 24, weight: .semibold))
                        .foregroundStyle(theme.ink)
                    Spacer()
                }
                Text(spec.eyebrow)
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(theme.body)

                if spec.pulseSurface {
                    let group = model.pulseGroup(code: spec.code)
                    OperatingMetricGrid(theme: theme, metrics: group?.metrics ?? [])
                    OperatingWhyFootnote(theme: theme, text: group?.why)
                    if (group?.metrics ?? []).isEmpty {
                        OperatingEmptyState(
                            theme: theme,
                            title: spec.emptyTitle,
                            bodyText: spec.emptyBody
                        )
                    }
                    if spec.code == "ATTENTION" {
                        attentionRows
                    }
                    if spec.code == "PAYMENT_READINESS" {
                        paymentRows
                    }
                } else {
                    OperatingBoardSectionBody(
                        theme: theme,
                        spec: spec,
                        section: model.section(code: spec.code),
                        currency: model.currency,
                        financeVisible: model.financeVisible,
                        onOpenRow: onOpenRow
                    )
                }
            }
            .padding(18)
        }
        .background(theme.canvas.ignoresSafeArea())
    }

    private var attentionRows: some View {
        let items = model.projection?.attention ?? []
        return Group {
            if !items.isEmpty {
                VStack(spacing: 0) {
                    ForEach(items) { item in
                        VStack(alignment: .leading, spacing: 4) {
                            HStack(spacing: 8) {
                                Text(item.title)
                                    .font(.plusJakarta(size: 14))
                                    .foregroundStyle(theme.ink)
                                Spacer(minLength: 4)
                                OperatingBadgeChip(
                                    theme: theme,
                                    badge: item.severity == "CRITICAL" ? "Overdue" : "Open"
                                )
                            }
                            if let due = item.dueAt, !due.isEmpty {
                                Text("Due \(String(due.prefix(10)))")
                                    .font(.plusJakarta(size: 12))
                                    .foregroundStyle(theme.body)
                            }
                            if model.financeVisible, let amount = item.amount, !amount.isEmpty {
                                Text(OperatingMoneyFormat.format(model.currency, amount))
                                    .font(.plusJakarta(size: 13, weight: .semibold))
                                    .foregroundStyle(theme.accent)
                            }
                        }
                        .padding(.vertical, 8)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        Divider().overlay(theme.border)
                    }
                }
            }
        }
    }

    private var paymentRows: some View {
        let items = model.projection?.paymentReadiness ?? []
        return Group {
            if !items.isEmpty {
                VStack(spacing: 0) {
                    ForEach(items) { item in
                        VStack(alignment: .leading, spacing: 4) {
                            HStack(spacing: 8) {
                                Text(item.title)
                                    .font(.plusJakarta(size: 14))
                                    .foregroundStyle(theme.ink)
                                Spacer(minLength: 4)
                                OperatingBadgeChip(theme: theme, badge: dueBadge(item.dueBucket))
                            }
                            if let name = item.counterpartyName, !name.isEmpty {
                                Text(name)
                                    .font(.plusJakarta(size: 12))
                                    .foregroundStyle(theme.body)
                            }
                            if model.financeVisible {
                                Text(OperatingMoneyFormat.format(model.currency, item.amount))
                                    .font(.plusJakarta(size: 13, weight: .semibold))
                                    .foregroundStyle(theme.accent)
                            }
                        }
                        .padding(.vertical, 8)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        Divider().overlay(theme.border)
                    }
                }
            }
        }
    }

    private func dueBadge(_ bucket: String?) -> String? {
        switch bucket {
        case "OVERDUE": return "Overdue"
        case "DUE_SOON": return "Due soon"
        case "OPEN": return "Open"
        default: return nil
        }
    }
}

/// The read of one recorded event, with the decision actions that apply to it.
struct OperatingRecordSheet: View {
    let theme: OperatingTheme
    let row: OperatingActivityPayload
    @ObservedObject var model: BusinessOperatingModel

    @Environment(\.dismiss) private var dismiss
    @State private var deciding = false
    @State private var document: OperatingDocumentPayload?
    @State private var documentKind = "INVOICE"
    @State private var showReprint = false

    private var canDecide: Bool {
        row.approvalStatus == "PENDING" || row.approvalStatus == "PENDING_APPROVAL"
    }

    private var canReprint: Bool {
        ["SALE", "PURCHASE", "SERVICE"].contains(row.eventType)
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                Text(row.title.isEmpty ? row.eventType.capitalized : row.title)
                    .font(.plusJakarta(size: 22, weight: .semibold))
                    .foregroundStyle(theme.ink)
                HStack(spacing: 8) {
                    OperatingPill(theme: theme, label: row.eventType.capitalized, accent: true)
                    OperatingBadgeChip(theme: theme, badge: row.badge)
                    if let status = row.approvalStatus {
                        OperatingPill(theme: theme, label: status.replacingOccurrences(of: "_", with: " ").capitalized)
                    }
                }
                facts

                if canDecide {
                    HStack(spacing: 10) {
                        decisionButton("Approve", decision: "APPROVE", filled: true)
                        decisionButton("Reject", decision: "REJECT", filled: false)
                    }
                }
                if canReprint {
                    reprintBlock
                }
                if let error = model.writeError {
                    Text(error)
                        .font(.plusJakarta(size: 12))
                        .foregroundStyle(theme.negative)
                }
                OperatingWhyFootnote(
                    theme: theme,
                    text: "This is the recorded row. Nothing here can rewrite an approved record; a change is a new revision."
                )
            }
            .padding(18)
        }
        .background(theme.canvas.ignoresSafeArea())
    }

    private var facts: some View {
        OperatingCard(theme: theme) {
            factRow("Recorded", String(row.occurredAt.prefix(10)))
            if let name = row.counterpartyName, !name.isEmpty {
                factRow(model.vertical == .trading ? "Buyer" : "Dealer", name)
            }
            if let channel = row.channelName, !channel.isEmpty {
                factRow("Channel", channel)
            }
            if let quantity = row.quantity, !quantity.isEmpty {
                factRow("Quantity", [quantity, row.unit ?? ""].filter { !$0.isEmpty }.joined(separator: " "))
            }
            if model.financeVisible, let amount = row.amount, !amount.isEmpty {
                factRow("Amount", OperatingMoneyFormat.format(model.currency, amount))
            }
            if let paymentStatus = row.paymentStatus, !paymentStatus.isEmpty {
                factRow("Payment", paymentStatus.replacingOccurrences(of: "_", with: " ").capitalized)
            }
            if let note = row.note, !note.isEmpty {
                factRow("Note", note)
            }
        }
    }

    private func factRow(_ label: String, _ value: String) -> some View {
        HStack(alignment: .top, spacing: 12) {
            Text(label)
                .font(.plusJakarta(size: 12))
                .foregroundStyle(theme.muted)
            Spacer(minLength: 8)
            Text(value)
                .font(.plusJakarta(size: 13))
                .foregroundStyle(theme.ink)
                .multilineTextAlignment(.trailing)
        }
    }

    private func decisionButton(_ label: String, decision: String, filled: Bool) -> some View {
        Button {
            Task {
                deciding = true
                _ = await model.decide(eventId: row.id, decision: decision)
                deciding = false
                if model.writeError == nil { dismiss() }
            }
        } label: {
            Text(deciding ? "Saving…" : label)
                .font(.plusJakarta(size: 14, weight: .semibold))
                .foregroundStyle(filled ? theme.onAccent : theme.accent)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 12)
                .background(filled ? theme.accent : Color.clear)
                .overlay(
                    RoundedRectangle(cornerRadius: 12)
                        .stroke(theme.accent, lineWidth: 1)
                )
                .clipShape(RoundedRectangle(cornerRadius: 12))
        }
        .buttonStyle(.plain)
        .disabled(deciding)
    }

    /// `TRD-QA-REPRINT` / `DST-QA-REPRINT` — a read, never a new post.
    private var reprintBlock: some View {
        OperatingCard(theme: theme) {
            HStack {
                Text("Reprint")
                    .font(.plusJakarta(size: 15, weight: .semibold))
                    .foregroundStyle(theme.ink)
                Spacer()
                Picker("Document", selection: $documentKind) {
                    ForEach(OperatingCaptureLayout.reprintKinds, id: \.self) { kind in
                        Text(kind.capitalized).tag(kind)
                    }
                }
                .pickerStyle(.segmented)
                .frame(width: 160)
            }
            Button {
                Task {
                    showReprint = true
                    document = await model.document(eventId: row.id, kind: documentKind)
                }
            } label: {
                Text(document == nil ? "Load document" : "Reload document")
                    .font(.plusJakarta(size: 13, weight: .semibold))
                    .foregroundStyle(theme.accent)
            }
            .buttonStyle(.plain)
            if let document, showReprint {
                documentSummary(document)
            }
            OperatingWhyFootnote(
                theme: theme,
                text: "A reprint draws the invoice or challan from the recorded row. Nothing is posted."
            )
        }
    }

    private func documentSummary(_ document: OperatingDocumentPayload) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(document.documentNumber.isEmpty ? document.kind.capitalized : document.documentNumber)
                .font(.plusJakarta(size: 15, weight: .semibold))
                .foregroundStyle(theme.ink)
            if let buyer = document.buyer?.name, !buyer.isEmpty {
                Text("To \(buyer)")
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(theme.body)
            }
            let lines = document.lines ?? []
            if !lines.isEmpty {
                Text(lines.count == 1 ? "1 line" : "\(lines.count) lines")
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(theme.body)
            }
            if let total = document.grandTotal, !total.isEmpty {
                Text("Total \(OperatingMoneyFormat.format(document.currency, total))")
                    .font(.plusJakarta(size: 13, weight: .semibold))
                    .foregroundStyle(theme.accent)
            }
        }
    }
}

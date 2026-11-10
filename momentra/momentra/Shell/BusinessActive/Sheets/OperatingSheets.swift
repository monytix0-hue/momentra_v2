import SwiftUI

/// One data-driven `View all ›` sheet for all eighteen surfaces of a vertical.
///
/// Sticky header carries period + status chips (`DST-SHEET`); the body scrolls; the footer
/// CTA deep-links into Quick Add, payment capture, attention, or Memory.
struct OperatingViewAllSheet: View {
    let spec: OperatingSheetSpec
    let theme: OperatingTheme
    @ObservedObject var model: BusinessOperatingModel
    var onOpenRow: ((OperatingActivityPayload) -> Void)? = nil
    /// Host handles footer actions: `QUICK_ADD`, `PAYMENT`, `ATTENTION`, `MEMORY`, or a capture code.
    var onFooterAction: ((String) -> Void)? = nil

    @State private var period = OperatingSheetChrome.periodFilters[0]
    @State private var statusFilter: String = ""
    @State private var pickingCustomBound: CustomPeriodBound?

    private enum CustomPeriodBound: String, Identifiable {
        case from
        case to
        var id: String { rawValue }
        var title: String {
            switch self {
            case .from: return "Custom range · from"
            case .to: return "Custom range · to"
            }
        }
    }

    private var filters: [String] { spec.statusFilters }
    private var selectedStatus: String {
        statusFilter.isEmpty ? (filters.first ?? "All") : statusFilter
    }

    private var contextLine: String {
        let title = model.title.isEmpty ? model.vertical.label : model.title
        let month = OperatingV12Date.monthYear(model.projection?.startDate) ?? "This month"
        return "\(title) · \(month) · full activity, newest first"
    }

    var body: some View {
        VStack(spacing: 0) {
            stickyHeader
            ScrollView {
                VStack(alignment: .leading, spacing: 14) {
                    sheetBody
                }
                .padding(.horizontal, 18)
                .padding(.vertical, 12)
            }
            stickyFooter
        }
        .background(theme.canvas.ignoresSafeArea())
        .onAppear {
            if statusFilter.isEmpty { statusFilter = filters.first ?? "All" }
            period = Self.periodLabel(for: model.periodFilter)
        }
        .sheet(item: $pickingCustomBound) { bound in
            OperatingCustomPeriodPicker(
                theme: theme,
                title: bound.title,
                initial: bound == .from ? model.periodFrom : model.periodTo
            ) { day in
                pickingCustomBound = nil
                Task {
                    if bound == .from {
                        await model.applyCustomPeriod(from: day)
                    } else {
                        await model.applyCustomPeriod(to: day)
                    }
                }
            } onCancel: {
                pickingCustomBound = nil
            }
            .presentationDetents([.medium])
            .preferredColorScheme(.dark)
        }
    }

    private var stickyHeader: some View {
        VStack(alignment: .leading, spacing: 10) {
            Capsule()
                .fill(Color.white.opacity(0.2))
                .frame(width: 40, height: 4)
                .frame(maxWidth: .infinity)

            HStack(spacing: 10) {
                Text(spec.title)
                    .font(.plusJakarta(size: 24, weight: .semibold))
                    .foregroundStyle(theme.ink)
                Spacer(minLength: 4)
            }

            Text(contextLine)
                .font(.plusJakarta(size: 12))
                .foregroundStyle(theme.body)
                .lineLimit(2)

            chipRow(OperatingSheetChrome.periodFilters, selection: period) { next in
                period = next
                Task { await model.applySheetPeriod(next) }
            }
            if period == "Custom range ›" {
                customRangeRow
            }
            chipRow(filters, selection: selectedStatus) { next in
                // Status chips filter the already-loaded sheet body locally.
                statusFilter = next
            }
        }
        .padding(.horizontal, 18)
        .padding(.top, 10)
        .padding(.bottom, 12)
        .background(theme.canvas)
    }

    private var customRangeRow: some View {
        HStack(spacing: 8) {
            customBoundChip(label: "From", value: model.periodFrom, bound: .from)
            customBoundChip(label: "To", value: model.periodTo, bound: .to)
        }
    }

    private func customBoundChip(label: String, value: String?, bound: CustomPeriodBound) -> some View {
        Button {
            pickingCustomBound = bound
        } label: {
            Text("\(label) \(value ?? "…")")
                .font(.plusJakarta(size: 12, weight: .semibold))
                .foregroundStyle(theme.ink)
                .lineLimit(1)
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.horizontal, 12)
                .padding(.vertical, 7)
                .background(theme.inset)
                .clipShape(Capsule())
                .overlay(Capsule().stroke(theme.border, lineWidth: 1))
        }
        .buttonStyle(.plain)
    }

    private static func periodLabel(for filter: String?) -> String {
        switch filter {
        case "last_month": return "Last month"
        case "custom": return "Custom range ›"
        default: return OperatingSheetChrome.periodFilters[0]
        }
    }

    private func chipRow(_ options: [String], selection: String, onSelect: @escaping (String) -> Void) -> some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(options, id: \.self) { option in
                    let on = option == selection
                    Text(option)
                        .font(.plusJakarta(size: 12, weight: .semibold))
                        .foregroundStyle(on ? theme.onAccent : theme.body)
                        .padding(.horizontal, 13)
                        .padding(.vertical, 7)
                        .background(on ? theme.accent : theme.inset)
                        .clipShape(Capsule())
                        .overlay(Capsule().stroke(theme.border, lineWidth: on ? 0 : 1))
                        .onTapGesture { onSelect(option) }
                }
            }
        }
    }

    @ViewBuilder
    private var sheetBody: some View {
        if spec.pulseSurface {
            let group = model.pulseGroup(code: spec.code)
            OperatingMetricGrid(theme: theme, metrics: group?.metrics ?? [])
            OperatingWhyFootnote(theme: theme, text: group?.why)
            if (group?.metrics ?? []).isEmpty && rawRowCount == 0 {
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
            if spec.code == "ACTIVITY" {
                activityRows
            }
        } else if let section = model.section(code: spec.code) {
            OperatingMetricGrid(theme: theme, metrics: section.metrics ?? [])
            OperatingWhyFootnote(theme: theme, text: section.why)
            let rows = filteredBoardRows(section.rows ?? [])
            if rows.isEmpty {
                OperatingEmptyState(
                    theme: theme,
                    title: spec.emptyTitle,
                    bodyText: spec.emptyBody,
                    glyph: "◻︎"
                )
            } else {
                VStack(spacing: 0) {
                    ForEach(rows) { row in
                        HStack(alignment: .center, spacing: 10) {
                            priorityBar(for: row.badge)
                            OperatingActivityRow(
                                theme: theme,
                                row: row,
                                currency: model.currency,
                                financeVisible: model.financeVisible,
                                onTap: onOpenRow.map { open in { open(row) } }
                            )
                        }
                        Divider().overlay(theme.border)
                    }
                }
            }
        } else {
            OperatingEmptyState(theme: theme, title: spec.emptyTitle, bodyText: spec.emptyBody)
        }
    }

    private var stickyFooter: some View {
        VStack(spacing: 8) {
            Text("Showing \(visibleCount) of \(totalCount) · tap a row for detail")
                .font(.plusJakarta(size: 12))
                .foregroundStyle(theme.body)
                .frame(maxWidth: .infinity)
            Button {
                onFooterAction?(OperatingSheetChrome.footerAction(for: spec.code))
            } label: {
                Text(spec.footerCta)
                    .font(.plusJakarta(size: 15, weight: .semibold))
                    .foregroundStyle(theme.onAccent)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 14)
                    .background(theme.accent)
                    .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
            }
            .buttonStyle(.plain)
        }
        .padding(.horizontal, 18)
        .padding(.top, 10)
        .padding(.bottom, 18)
        .background(theme.canvas)
    }

    private var rawRowCount: Int {
        if spec.code == "ATTENTION" { return model.projection?.attention?.count ?? 0 }
        if spec.code == "PAYMENT_READINESS" { return model.projection?.paymentReadiness?.count ?? 0 }
        if spec.code == "ACTIVITY" { return model.projection?.activity?.count ?? 0 }
        if spec.pulseSurface { return model.pulseGroup(code: spec.code)?.metrics?.count ?? 0 }
        return model.section(code: spec.code)?.rows?.count ?? 0
    }

    private var totalCount: Int { rawRowCount }
    private var visibleCount: Int {
        if spec.code == "ATTENTION" { return filteredAttention.count }
        if spec.code == "PAYMENT_READINESS" { return filteredPayments.count }
        if spec.code == "ACTIVITY" { return filteredActivity.count }
        if spec.pulseSurface { return model.pulseGroup(code: spec.code)?.metrics?.count ?? 0 }
        return filteredBoardRows(model.section(code: spec.code)?.rows ?? []).count
    }

    private func matchesStatus(_ badge: String?) -> Bool {
        let filter = selectedStatus
        if filter == "All" { return true }
        let b = (badge ?? "").lowercased()
        switch filter {
        case "Open":
            return b.contains("open") || b.contains("alert") || b.contains("part")
                || b.contains("transit") || b.contains("unavailable") || b.isEmpty
        case "Done":
            return b.contains("done") || b.contains("delivered")
        case "Settled":
            return b.contains("settled") || b.contains("paid") || b.contains("done")
        case "Overdue":
            return b.contains("overdue") || b.contains("critical")
        default:
            return true
        }
    }

    private var filteredAttention: [OperatingAttentionPayload] {
        (model.projection?.attention ?? []).filter { item in
            matchesStatus(attentionChip(item))
        }
    }

    private var filteredPayments: [OperatingPaymentPayload] {
        (model.projection?.paymentReadiness ?? []).filter { item in
            matchesStatus(dueBadge(item.dueBucket))
        }
    }

    private var filteredActivity: [OperatingActivityPayload] {
        (model.projection?.activity ?? []).filter { matchesStatus($0.badge) }
    }

    private func filteredBoardRows(_ rows: [OperatingActivityPayload]) -> [OperatingActivityPayload] {
        rows.filter { matchesStatus($0.badge) }
    }

    private var attentionRows: some View {
        Group {
            if !filteredAttention.isEmpty {
                VStack(spacing: 0) {
                    ForEach(filteredAttention) { item in
                        HStack(alignment: .center, spacing: 10) {
                            priorityBar(for: attentionChip(item))
                            VStack(alignment: .leading, spacing: 4) {
                                HStack(spacing: 8) {
                                    Text(item.title)
                                        .font(.plusJakarta(size: 14))
                                        .foregroundStyle(theme.ink)
                                    Spacer(minLength: 4)
                                    OperatingBadgeChip(theme: theme, badge: attentionChip(item))
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
        Group {
            if !filteredPayments.isEmpty {
                VStack(spacing: 0) {
                    ForEach(filteredPayments) { item in
                        HStack(alignment: .center, spacing: 10) {
                            priorityBar(for: dueBadge(item.dueBucket))
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
                        }
                        .padding(.vertical, 8)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        Divider().overlay(theme.border)
                    }
                }
            }
        }
    }

    private var activityRows: some View {
        Group {
            if !filteredActivity.isEmpty {
                VStack(spacing: 0) {
                    ForEach(filteredActivity) { row in
                        HStack(alignment: .center, spacing: 10) {
                            priorityBar(for: row.badge)
                            OperatingActivityRow(
                                theme: theme,
                                row: row,
                                currency: model.currency,
                                financeVisible: model.financeVisible,
                                onTap: onOpenRow.map { open in { open(row) } }
                            )
                        }
                        Divider().overlay(theme.border)
                    }
                }
            }
        }
    }

    private func priorityBar(for badge: String?) -> some View {
        let tone: Color = {
            switch (badge ?? "").lowercased() {
            case let b where b.contains("overdue") || b.contains("critical"):
                return Color(red: 0.97, green: 0.44, blue: 0.44)
            case let b where b.contains("alert") || b.contains("open") || b.contains("unavailable"):
                return Color(red: 0.96, green: 0.62, blue: 0.04)
            default:
                return theme.accent.opacity(0.55)
            }
        }()
        return RoundedRectangle(cornerRadius: 2)
            .fill(tone)
            .frame(width: 3, height: 32)
    }

    private func attentionChip(_ item: OperatingAttentionPayload) -> String? {
        switch (item.severity ?? "").uppercased() {
        case "CRITICAL": return "Overdue"
        case "HIGH": return "Alert"
        case "MEDIUM": return "Unavailable"
        default: return "Open"
        }
    }

    private func dueBadge(_ bucket: String?) -> String? {
        switch bucket {
        case "OVERDUE": return "Overdue"
        case "DUE_SOON": return "Due soon"
        case "OPEN": return "Open"
        case "SETTLED": return "Settled"
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

/// Day picker for view-all `period=custom` From / To chips (mirrors Android `OperatingPickerSheet`).
private struct OperatingCustomPeriodPicker: View {
    let theme: OperatingTheme
    let title: String
    let initial: String?
    let onPick: (String) -> Void
    let onCancel: () -> Void

    @State private var draft: Date

    init(
        theme: OperatingTheme,
        title: String,
        initial: String?,
        onPick: @escaping (String) -> Void,
        onCancel: @escaping () -> Void
    ) {
        self.theme = theme
        self.title = title
        self.initial = initial
        self.onPick = onPick
        self.onCancel = onCancel
        _draft = State(initialValue: SetupDateTimeUtils.dateFromIso(initial))
    }

    var body: some View {
        NavigationStack {
            DatePicker("", selection: $draft, displayedComponents: .date)
                .datePickerStyle(.graphical)
                .labelsHidden()
                .tint(theme.accent)
                .padding()
                .navigationTitle(title)
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .cancellationAction) {
                        Button("Cancel", action: onCancel)
                    }
                    ToolbarItem(placement: .confirmationAction) {
                        Button("OK") {
                            onPick(SetupDateTimeUtils.localDateString(from: draft))
                        }
                    }
                }
        }
        .background(theme.canvas.ignoresSafeArea())
    }
}

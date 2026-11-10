import SwiftUI

/// Marketing capture modes — mirrors Android `MarketingSheet` / catalog quick-adds.
enum MarketingCaptureMode: String, CaseIterable, Identifiable {
    case activity = "ACTIVITY"
    case progress = "PROGRESS"
    case spend = "SPEND"
    case result = "RESULT"
    case payment = "PAYMENT"
    case invoice = "INVOICE"
    case collection = "COLLECTION"
    case revision = "REVISION"
    case budget = "BUDGET"
    case target = "TARGET"
    case update = "UPDATE"
    case issue = "ISSUE"
    case approval = "APPROVAL"
    case deliverable = "DELIVERABLE"
    case vendorCost = "VENDOR_COST"
    case complete = "COMPLETE"

    var id: String { rawValue }

    var eventType: String { rawValue }

    var needsAmount: Bool {
        switch self {
        case .progress, .spend, .result, .payment, .invoice, .collection, .revision, .budget, .target, .vendorCost:
            return true
        default:
            return false
        }
    }

    var needsChannel: Bool {
        switch self {
        case .spend, .result, .collection, .progress: return true
        default: return false
        }
    }

    var needsParty: Bool {
        switch self {
        case .payment, .invoice, .vendorCost, .collection: return true
        default: return false
        }
    }

    var needsDueDate: Bool {
        switch self {
        case .payment, .invoice, .collection, .approval, .activity: return true
        default: return false
        }
    }

    func title(client: Bool) -> String {
        switch self {
        case .activity: return "Add Activity"
        case .progress: return "Update Progress"
        case .spend: return "Add Spend / Expense"
        case .result: return "Add Result"
        case .payment: return "Add Payment"
        case .invoice: return "Record Invoice"
        case .collection: return client ? "Client Billing / Collection" : "Add Collection"
        case .revision: return "Budget / Target Revision"
        case .budget: return "Add Budget"
        case .target: return client ? "Target Revision" : "Add Target"
        case .update: return "Add Update"
        case .issue: return "Add Issue"
        case .approval: return "Request Approval"
        case .deliverable: return "Add Deliverable / Milestone"
        case .vendorCost: return "Add Vendor / Creator Cost"
        case .complete: return "Mark Result / Deliverable Complete"
        }
    }

    var glyph: String {
        switch self {
        case .activity, .update: return "text.bubble"
        case .progress, .result: return "chart.line.uptrend.xyaxis"
        case .spend: return "wallet.pass"
        case .payment: return "creditcard"
        case .invoice, .collection: return "arrow.left.arrow.right"
        case .revision, .budget, .target: return "target"
        case .issue: return "exclamationmark.triangle"
        case .approval: return "checkmark.shield"
        case .deliverable, .complete: return "checklist"
        case .vendorCost: return "person.2"
        }
    }

    /// Client-only captures the Own Business campaign must never offer.
    var clientOnly: Bool {
        self == .invoice || self == .collection
    }

    var whyNote: String {
        switch self {
        case .spend, .vendorCost:
            return "Spend counts once toward the effective budget after approval."
        case .result, .progress:
            return "Accepted results count toward the target. Money collected does not."
        case .invoice:
            return "Invoice raises client AR. Collection settles it — Client Campaign only."
        case .collection:
            return "Collection allocates against open client invoices — Client Campaign only."
        case .payment:
            return "Payment settles open vendor / creator dues. Submit ≠ payment."
        case .approval:
            return "Approval freezes a creative or spend decision — it does not move money."
        case .revision, .budget, .target:
            return "Revisions version the plan; only the effective-from date is in force."
        default:
            return "Posted events appear on Pulse Activity once saved."
        }
    }
}

/// Quick-add hub for Client / Own campaigns. Filters tiles from the projection; capture and
/// masters open through the host sheet router (same pattern as Trading's hub).
struct MarketingQuickAddHubView: View {
    let theme: OperatingTheme
    @ObservedObject var model: BusinessOperatingModel
    var onCapture: (MarketingCaptureMode) -> Void
    var onOpenMasters: () -> Void = {}
    var onOpenStory: () -> Void = {}

    @State private var query = ""

    private var client: Bool { model.vertical == .clientCampaign }
    private var allowed: Set<String> {
        Set((model.projection?.quickAdds ?? []).map { $0.code.uppercased() })
    }

    private var core: [MarketingCaptureMode] {
        let base: [MarketingCaptureMode] = [
            .activity, .progress, .spend, .payment, .invoice, .collection,
            .revision, .approval, .update, .issue, .result,
        ]
        return base.filter { mode in
            if mode.clientOnly && !client { return false }
            guard allowed.isEmpty || allowed.contains(mode.rawValue) else { return false }
            if query.isEmpty { return true }
            return mode.title(client: client).localizedCaseInsensitiveContains(query)
        }
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                Text("QUICK ADDS")
                    .font(.plusJakarta(size: 11, weight: .bold))
                    .tracking(1.2)
                    .foregroundStyle(OperatingV12.cyan)
                Text("Quick Adds")
                    .font(.plusJakarta(size: 28, weight: .semibold))
                    .foregroundStyle(OperatingV12.title)
                Text("Record what happened · \(model.title)")
                    .font(.plusJakarta(size: 13))
                    .foregroundStyle(theme.body)

                HStack(spacing: 8) {
                    pill(model.projection?.role?.replacingOccurrences(of: "_", with: " ") ?? "Marketing", filled: true)
                    pill(client ? "Client Campaign" : "Own Business", filled: false)
                }

                heroBanner
                searchField

                Text("Core actions · \(core.count)")
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(OperatingV12.subtitle)

                LazyVGrid(
                    columns: [GridItem(.flexible()), GridItem(.flexible()), GridItem(.flexible())],
                    spacing: 8
                ) {
                    ForEach(core) { mode in
                        OperatingV12ActionTile(
                            theme: theme,
                            title: mode.title(client: client),
                            subtitle: mode.rawValue.capitalized,
                            glyph: mode.glyph
                        ) {
                            onCapture(mode)
                        }
                    }
                }

                Text("Support")
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(OperatingV12.subtitle)

                HStack(spacing: 10) {
                    supportTile("Network", "person.2", action: onOpenMasters)
                    supportTile("Invite & QR", "qrcode", action: onOpenMasters)
                    supportTile("Story", "book", action: onOpenStory)
                }

                Text("Utilities · \(utilities.count)")
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(OperatingV12.subtitle)

                LazyVGrid(
                    columns: [GridItem(.flexible()), GridItem(.flexible()), GridItem(.flexible())],
                    spacing: 8
                ) {
                    ForEach(utilities, id: \.title) { tile in
                        OperatingV12ActionTile(
                            theme: theme,
                            title: tile.title,
                            subtitle: tile.caption,
                            glyph: tile.glyph,
                            unavailable: true
                        ) {}
                    }
                }

                whyCard
            }
            .padding(18)
        }
        .background(theme.canvas.ignoresSafeArea())
    }

    /// Honest non-actionable tiles — Marketing has no hold / budget-transfer / reprint writes.
    private var utilities: [(title: String, caption: String, glyph: String)] {
        var tiles: [(title: String, caption: String, glyph: String)] = [
            ("Hold", "Park campaign work", "pause.circle"),
            ("Transfer", "Budget move", "arrow.left.arrow.right"),
        ]
        if client {
            tiles.append(("Reprint", "Invoice · receipt", "printer"))
        }
        if query.isEmpty { return tiles }
        return tiles.filter {
            $0.title.localizedCaseInsensitiveContains(query)
                || $0.caption.localizedCaseInsensitiveContains(query)
        }
    }

    private var heroBanner: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(client ? "Spend. Results. Approvals." : "Every marketing moment counts.")
                .font(.plusJakarta(size: 18, weight: .semibold))
                .foregroundStyle(OperatingV12.title)
            Text(client
                 ? "Record controlled spend, outcomes and handoffs."
                 : "Capture spend, results, payments and approvals.")
                .font(.plusJakarta(size: 13))
                .foregroundStyle(theme.body)
            if let party = model.projection?.partyName, !party.isEmpty {
                Text(client ? "Client · \(party)" : "Brand · \(party)")
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(OperatingV12.subtitle)
            }
            if allowed.isEmpty || allowed.contains("SPEND") {
                Button {
                    onCapture(.spend)
                } label: {
                    Label("Record Spend", systemImage: "plus")
                        .font(.plusJakarta(size: 13, weight: .semibold))
                        .foregroundStyle(theme.onAccent)
                        .padding(.horizontal, 14)
                        .padding(.vertical, 8)
                        .background(theme.accent)
                        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                }
                .buttonStyle(.plain)
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            LinearGradient(
                colors: [theme.accent.opacity(0.28), OperatingV12.surface],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        )
        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .strokeBorder(theme.accent.opacity(0.35), lineWidth: 1)
        )
    }

    private var searchField: some View {
        HStack(spacing: 10) {
            Image(systemName: "magnifyingglass")
                .foregroundStyle(OperatingV12.subtitle)
            TextField("Search actions", text: $query)
                .font(.plusJakarta(size: 14))
                .foregroundStyle(OperatingV12.title)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
        .background(OperatingV12.inset)
        .overlay(
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .stroke(theme.border, lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
    }

    private func supportTile(_ title: String, _ glyph: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            VStack(spacing: 6) {
                Image(systemName: glyph).foregroundStyle(theme.accent)
                Text(title)
                    .font(.plusJakarta(size: 12, weight: .semibold))
                    .foregroundStyle(OperatingV12.title)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 14)
            .background(OperatingV12.inset)
            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
        }
        .buttonStyle(.plain)
    }

    private var whyCard: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Why these tiles save differently")
                .font(.plusJakarta(size: 13, weight: .bold))
                .foregroundStyle(OperatingV12.cyan)
            Text(client
                 ? "• Invoice and collection stay Client-only\n• Own Business posts spend and results instead\n• Accepted results count toward target — cash does not\n• Submit ≠ approval ≠ payment"
                 : "• Own Business has no client invoice or collection\n• Spend, results, and payments only\n• ROAS uses attributed sales ÷ eligible spend\n• Submit ≠ approval ≠ payment")
                .font(.plusJakarta(size: 12, weight: .medium))
                .foregroundStyle(Color(hex: "#CBD5E1"))
                .fixedSize(horizontal: false, vertical: true)
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(OperatingV12.cyan.opacity(0.08))
        .overlay(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .strokeBorder(OperatingV12.cyan.opacity(0.4), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
    }

    private func pill(_ text: String, filled: Bool) -> some View {
        Text(text)
            .font(.plusJakarta(size: 12))
            .foregroundStyle(filled ? theme.onAccent : OperatingV12.subtitle)
            .padding(.horizontal, 10)
            .padding(.vertical, 6)
            .background(filled ? theme.accent : Color.clear)
            .overlay(
                Capsule().stroke(filled ? theme.accent : theme.border, lineWidth: 1)
            )
            .clipShape(Capsule())
    }
}

/// Capture sheet for marketing event types — due dates, mode chips, sticky footer, why-note.
/// Only fields already on `OperatingEventInput` / `recordOperatingEvent` are posted.
struct MarketingCaptureSheet: View {
    let theme: OperatingTheme
    let mode: MarketingCaptureMode
    @ObservedObject var model: BusinessOperatingModel

    @Environment(\.dismiss) private var dismiss
    @State private var title = ""
    @State private var amount = ""
    @State private var note = ""
    @State private var channel = ""
    @State private var party = ""
    @State private var metric = "LEADS"
    @State private var reportingMode = "INCREMENTAL"
    @State private var budgetCategory = "DIGITAL_ADS"
    @State private var revisionKind = "BUDGET"
    @State private var activityStatus = "IN_PROGRESS"
    @State private var creativeVersion = ""
    @State private var paymentStatus = "PENDING"
    @State private var attributedSales = ""
    @State private var resultSource = "MANUAL"
    @State private var dueDate = Date()
    @State private var includeDueDate = false
    @State private var saving = false

    private var client: Bool { model.vertical == .clientCampaign }

    private var channels: [String] {
        model.projection?.life?.channels ?? BusinessSetupDocSpec.marketingChannels
    }

    private var metrics: [String] {
        client
            ? ["LEADS", "CONVERSIONS", "REVENUE", "ORDERS", "REACH", "ENGAGEMENT", "CUSTOM_COUNT", "CLIENT_BILLING"]
            : ["LEADS", "CONVERSIONS", "REVENUE", "ORDERS", "REACH", "ENGAGEMENT", "CUSTOM_COUNT"]
    }

    private var paymentStatuses: [String] {
        switch mode {
        case .invoice: return ["PENDING", "PARTIAL", "PAID"]
        case .collection, .payment: return ["PENDING", "SETTLED", "PARTIAL"]
        default: return ["PENDING"]
        }
    }

    private var canSave: Bool {
        !title.trimmingCharacters(in: .whitespaces).isEmpty
            && (!mode.needsAmount || !amount.trimmingCharacters(in: .whitespaces).isEmpty)
            && (!mode.clientOnly || client)
    }

    var body: some View {
        VStack(spacing: 0) {
            ScrollView {
                VStack(alignment: .leading, spacing: 12) {
                    Text(mode.title(client: client))
                        .font(.plusJakarta(size: 22, weight: .semibold))
                        .foregroundStyle(OperatingV12.title)

                    Text("\(model.title) · \(client ? "Client Campaign" : "Own Business")")
                        .font(.plusJakarta(size: 12))
                        .foregroundStyle(OperatingV12.subtitle)

                    HStack(spacing: 8) {
                        OperatingPill(theme: theme, label: mode.rawValue, accent: true)
                        OperatingPill(
                            theme: theme,
                            label: client ? "CLIENT" : "OWN",
                            accent: false
                        )
                    }

                    if mode.clientOnly && !client {
                        Text("Invoice and collection are Client Campaign only.")
                            .font(.plusJakarta(size: 13))
                            .foregroundStyle(theme.warning)
                            .padding(.bottom, 4)
                    }

                    field("Title", text: $title)
                    if mode.needsAmount {
                        field(amountLabel, text: $amount, decimal: true)
                    }
                    if mode.needsChannel {
                        chipRow("Channel", options: channels, selection: $channel)
                    }
                    if mode.needsParty {
                        field(client ? "Client / vendor" : "Vendor / creator", text: $party)
                    }
                    if mode == .result || mode == .progress {
                        chipRow("Metric", options: metrics, selection: $metric)
                        chipRow("Reporting", options: ["INCREMENTAL", "CUMULATIVE"], selection: $reportingMode)
                        if !client, mode == .result {
                            field("Attributed sales (optional)", text: $attributedSales, decimal: true)
                            chipRow("Source", options: ["MANUAL", "IMPORT", "PIXEL"], selection: $resultSource)
                        }
                    }
                    if mode == .spend || mode == .invoice || mode == .vendorCost {
                        chipRow(
                            "Budget category",
                            options: ["DIGITAL_ADS", "CREATIVE", "AGENCY", "TOOLS", "OTHER"],
                            selection: $budgetCategory
                        )
                    }
                    if mode == .revision {
                        chipRow("Revision", options: ["BUDGET", "TARGET"], selection: $revisionKind)
                    }
                    if mode == .activity {
                        chipRow(
                            "Status",
                            options: ["PLANNED", "IN_PROGRESS", "BLOCKED", "DONE"],
                            selection: $activityStatus
                        )
                    }
                    if mode == .payment || mode == .invoice || mode == .collection {
                        chipRow("Payment status", options: paymentStatuses, selection: $paymentStatus)
                    }
                    if mode == .approval {
                        field("Creative version (optional)", text: $creativeVersion)
                    }
                    if mode.needsDueDate {
                        Toggle(isOn: $includeDueDate) {
                            Text("Set due date")
                                .font(.plusJakarta(size: 13))
                                .foregroundStyle(OperatingV12.title)
                        }
                        .tint(theme.accent)
                        if includeDueDate {
                            DatePicker(
                                "Due",
                                selection: $dueDate,
                                displayedComponents: .date
                            )
                            .datePickerStyle(.compact)
                            .font(.plusJakarta(size: 13))
                            .foregroundStyle(OperatingV12.title)
                        }
                    }
                    field("Note", text: $note)

                    Text(mode.whyNote)
                        .font(.plusJakarta(size: 12))
                        .foregroundStyle(OperatingV12.subtitle)
                        .padding(12)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(OperatingV12.inset)
                        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))

                    if let err = model.writeError, !err.isEmpty {
                        Text(err)
                            .font(.plusJakarta(size: 12))
                            .foregroundStyle(theme.negative)
                    }
                }
                .padding(18)
                .padding(.bottom, 8)
            }

            stickyFooter
        }
        .background(theme.canvas.ignoresSafeArea())
        .onAppear {
            if channel.isEmpty { channel = channels.first ?? "" }
            if title.isEmpty {
                title = mode.title(client: client)
            }
            if mode == .invoice || mode == .payment || mode == .collection {
                includeDueDate = true
            }
        }
    }

    private var stickyFooter: some View {
        VStack(spacing: 8) {
            Text(mode.whyNote)
                .font(.plusJakarta(size: 11))
                .foregroundStyle(OperatingV12.subtitle)
                .lineLimit(2)
                .frame(maxWidth: .infinity, alignment: .leading)
            Button {
                Task { await save() }
            } label: {
                Text(saving ? "Saving…" : "Save")
                    .font(.plusJakarta(size: 15, weight: .semibold))
                    .foregroundStyle(theme.onAccent)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 14)
                    .background(canSave && !saving ? theme.accent : theme.accent.opacity(0.35))
                    .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                    .shadow(color: theme.accent.opacity(canSave ? 0.18 : 0), radius: 14, y: 8)
            }
            .buttonStyle(.plain)
            .disabled(!canSave || saving)
        }
        .padding(.horizontal, 18)
        .padding(.top, 10)
        .padding(.bottom, 18)
        .background(theme.canvas)
    }

    private var amountLabel: String {
        switch mode {
        case .spend, .vendorCost: return "Spent"
        case .payment: return "Paid"
        case .collection: return "Collected"
        case .invoice: return "Invoice amount"
        case .result, .progress: return "Value"
        case .budget, .revision: return "Budget"
        case .target: return "Target"
        default: return "Amount"
        }
    }

    private func save() async {
        guard canSave else { return }
        if mode.clientOnly && !client {
            return
        }
        saving = true
        defer { saving = false }

        let dueAt: String? = {
            guard includeDueDate, mode.needsDueDate else { return nil }
            let formatter = DateFormatter()
            formatter.dateFormat = "yyyy-MM-dd"
            formatter.locale = Locale(identifier: "en_US_POSIX")
            return formatter.string(from: dueDate)
        }()

        var input = OperatingEventInput(
            momentId: model.momentId,
            eventType: mode.eventType,
            title: title.trimmingCharacters(in: .whitespaces),
            amount: amount.isEmpty ? nil : amount,
            note: note.isEmpty ? nil : note,
            channelName: channel.isEmpty ? nil : channel,
            counterpartyName: party.isEmpty ? nil : party,
            occurredAt: ISO8601DateFormatter().string(from: Date()),
            dueAt: dueAt,
            paymentStatus: (mode == .payment || mode == .collection || mode == .invoice)
                ? paymentStatus
                : nil
        )
        if mode == .result || mode == .progress {
            input.metricCode = metric
            input.reportingMode = reportingMode
        }
        if mode == .result, !client {
            if !attributedSales.isEmpty { input.attributedSales = attributedSales }
            input.resultSource = resultSource
        }
        if mode == .spend || mode == .invoice || mode == .vendorCost {
            input.budgetCategory = budgetCategory
        }
        if mode == .revision {
            input.revisionKind = revisionKind
        }
        if mode == .activity {
            input.activityStatus = activityStatus
        }
        if mode == .approval, !creativeVersion.isEmpty {
            input.creativeVersion = creativeVersion
        }
        let ok = await model.record(input)
        if ok { dismiss() }
    }

    private func field(_ label: String, text: Binding<String>, decimal: Bool = false) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label)
                .font(.plusJakarta(size: 11))
                .foregroundStyle(OperatingV12.subtitle)
            TextField("", text: text)
                .keyboardType(decimal ? .decimalPad : .default)
                .font(.plusJakarta(size: 14))
                .foregroundStyle(OperatingV12.title)
                .padding(.horizontal, 12)
                .padding(.vertical, 11)
                .background(OperatingV12.inset)
                .overlay(
                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                        .stroke(theme.border, lineWidth: 1)
                )
                .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
        }
    }

    private func chipRow(_ label: String, options: [String], selection: Binding<String>) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label)
                .font(.plusJakarta(size: 11))
                .foregroundStyle(OperatingV12.subtitle)
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(options, id: \.self) { option in
                        Button {
                            selection.wrappedValue = option
                        } label: {
                            OperatingPill(theme: theme, label: option, accent: selection.wrappedValue == option)
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
        }
    }
}

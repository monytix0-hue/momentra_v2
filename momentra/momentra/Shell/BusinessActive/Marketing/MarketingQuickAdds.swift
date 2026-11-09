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
        case .spend, .result, .collection: return true
        default: return false
        }
    }

    var needsParty: Bool {
        switch self {
        case .payment, .invoice, .vendorCost: return true
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
}

/// Quick-add hub for Client / Own campaigns. Filters tiles from the projection; capture and
/// masters open through the host sheet router (same pattern as Trading's hub).
struct MarketingQuickAddHubView: View {
    let theme: OperatingTheme
    @ObservedObject var model: BusinessOperatingModel
    var onCapture: (MarketingCaptureMode) -> Void
    var onOpenMasters: () -> Void = {}

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
                Text("Quick Adds")
                    .font(.plusJakarta(size: 28, weight: .semibold))
                    .foregroundStyle(theme.ink)
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
                    .foregroundStyle(theme.muted)

                LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible()), GridItem(.flexible())], spacing: 8) {
                    ForEach(core) { mode in
                        Button {
                            onCapture(mode)
                        } label: {
                            VStack(spacing: 8) {
                                Image(systemName: mode.glyph)
                                    .font(.system(size: 22))
                                    .foregroundStyle(theme.ink)
                                Text(mode.title(client: client))
                                    .font(.plusJakarta(size: 12, weight: .semibold))
                                    .foregroundStyle(theme.ink)
                                    .multilineTextAlignment(.center)
                                    .lineLimit(2)
                                    .minimumScaleFactor(0.8)
                            }
                            .padding(12)
                            .frame(maxWidth: .infinity, minHeight: 96)
                            .background(theme.surface)
                            .overlay(
                                RoundedRectangle(cornerRadius: 14, style: .continuous)
                                    .stroke(theme.border, lineWidth: 1)
                            )
                            .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                        }
                        .buttonStyle(.plain)
                    }
                }

                Button(action: onOpenMasters) {
                    Text("Masters ›")
                        .font(.plusJakarta(size: 14, weight: .semibold))
                        .foregroundStyle(theme.accent)
                }
                .buttonStyle(.plain)

                Text(client
                     ? "Invoice and collection stay Client-only. Own Business posts spend and results instead."
                     : "Own Business has no client invoice or collection — spend, results, and payments only.")
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(theme.muted)
                    .fixedSize(horizontal: false, vertical: true)
            }
            .padding(18)
        }
        .background(theme.canvas.ignoresSafeArea())
    }

    private var heroBanner: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(client ? "Spend. Results. Approvals." : "Every marketing moment counts.")
                .font(.plusJakarta(size: 18, weight: .semibold))
                .foregroundStyle(theme.ink)
            Text(client
                 ? "Record controlled spend, outcomes and handoffs."
                 : "Capture spend, results, payments and approvals.")
                .font(.plusJakarta(size: 13))
                .foregroundStyle(theme.body)
            if let party = model.projection?.partyName, !party.isEmpty {
                Text(client ? "Client · \(party)" : "Brand · \(party)")
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(theme.muted)
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
                colors: [theme.accent.opacity(0.28), theme.surface],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        )
        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
    }

    private var searchField: some View {
        HStack(spacing: 10) {
            Image(systemName: "magnifyingglass")
                .foregroundStyle(theme.muted)
            TextField("Search actions", text: $query)
                .font(.plusJakarta(size: 14))
                .foregroundStyle(theme.ink)
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
        .background(theme.inset)
        .overlay(
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .stroke(theme.border, lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
    }

    private func pill(_ text: String, filled: Bool) -> some View {
        Text(text)
            .font(.plusJakarta(size: 12))
            .foregroundStyle(filled ? theme.onAccent : theme.muted)
            .padding(.horizontal, 10)
            .padding(.vertical, 6)
            .background(filled ? theme.accent : Color.clear)
            .overlay(
                Capsule().stroke(filled ? theme.accent : theme.border, lineWidth: 1)
            )
            .clipShape(Capsule())
    }
}

/// Capture sheet for marketing event types, including metric / budget / revision fields.
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

    private var canSave: Bool {
        !title.trimmingCharacters(in: .whitespaces).isEmpty
            && (!mode.needsAmount || !amount.trimmingCharacters(in: .whitespaces).isEmpty)
            && (!mode.clientOnly || client)
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                HStack {
                    Text(mode.title(client: client))
                        .font(.plusJakarta(size: 22, weight: .semibold))
                        .foregroundStyle(theme.ink)
                    Spacer()
                    OperatingPill(theme: theme, label: mode.rawValue, accent: true)
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
                }
                if mode == .spend || mode == .invoice || mode == .vendorCost {
                    chipRow("Budget category", options: ["DIGITAL_ADS", "CREATIVE", "AGENCY", "TOOLS", "OTHER"], selection: $budgetCategory)
                }
                if mode == .revision {
                    chipRow("Revision", options: ["BUDGET", "TARGET"], selection: $revisionKind)
                }
                if mode == .activity {
                    chipRow("Status", options: ["PLANNED", "IN_PROGRESS", "BLOCKED", "DONE"], selection: $activityStatus)
                }
                if mode == .approval {
                    field("Creative version (optional)", text: $creativeVersion)
                }
                field("Note", text: $note)

                if let err = model.writeError, !err.isEmpty {
                    Text(err)
                        .font(.plusJakarta(size: 12))
                        .foregroundStyle(theme.negative)
                }

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
            .padding(18)
        }
        .background(theme.canvas.ignoresSafeArea())
        .onAppear {
            if channel.isEmpty { channel = channels.first ?? "" }
            if title.isEmpty {
                title = mode.title(client: client)
            }
        }
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
        var input = OperatingEventInput(
            momentId: model.momentId,
            eventType: mode.eventType,
            title: title.trimmingCharacters(in: .whitespaces),
            amount: amount.isEmpty ? nil : amount,
            note: note.isEmpty ? nil : note,
            channelName: channel.isEmpty ? nil : channel,
            counterpartyName: party.isEmpty ? nil : party,
            occurredAt: ISO8601DateFormatter().string(from: Date()),
            paymentStatus: (mode == .payment || mode == .collection || mode == .invoice) ? "PENDING" : nil
        )
        if mode == .result || mode == .progress {
            input.metricCode = metric
            input.reportingMode = reportingMode
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
                .foregroundStyle(theme.muted)
            TextField("", text: text)
                .keyboardType(decimal ? .decimalPad : .default)
                .font(.plusJakarta(size: 14))
                .foregroundStyle(theme.ink)
                .padding(.horizontal, 12)
                .padding(.vertical, 11)
                .background(theme.inset)
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
                .foregroundStyle(theme.muted)
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

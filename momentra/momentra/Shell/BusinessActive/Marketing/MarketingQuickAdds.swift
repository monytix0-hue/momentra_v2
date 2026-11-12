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
    /// Chrome-only — no campaign-hold / budget-transfer / invoice-reprint write APIs.
    case hold = "HOLD"
    case transfer = "TRANSFER"
    case reprint = "REPRINT"

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

    func title(client: Bool, own: Bool = false) -> String {
        if client {
            let key = MarketingClientQuickAddCopy.catalogMode(eventType: eventType, mode: rawValue)
            let titled = MarketingClientQuickAddCopy.title(mode: key)
            if titled != "Quick Add" { return titled }
        }
        if own {
            let key = MarketingOwnQuickAddCopy.catalogMode(eventType: eventType, mode: rawValue)
            let titled = MarketingOwnQuickAddCopy.title(mode: key)
            if titled != "Quick Add" { return titled }
        }
        switch self {
        case .activity: return "Add Activity"
        case .progress: return "Update Progress"
        case .spend: return "Add Spend / Expense"
        case .result: return "Add Result"
        case .payment: return "Add Payment"
        case .invoice: return "Record Invoice"
        case .collection: return client ? "Record Collection" : "Add Collection"
        case .revision: return "Budget / Target Revision"
        case .budget: return "Add Budget"
        case .target: return client ? "Target Revision" : "Add Target"
        case .update: return "Team Update / Issue"
        case .issue: return "Add Issue"
        case .approval: return "Approval / Decision"
        case .deliverable: return "Add Deliverable / Milestone"
        case .vendorCost: return "Add Vendor / Creator Cost"
        case .complete: return "Mark Result / Deliverable Complete"
        case .hold: return "Campaign hold"
        case .transfer: return "Budget transfer"
        case .reprint: return "Reprint invoice"
        }
    }

    /// Catalog key used for Client/Own Figma copy (context / WHY / CTA).
    func catalogKey(own: Bool = false) -> String {
        if own {
            return MarketingOwnQuickAddCopy.catalogMode(eventType: eventType, mode: rawValue)
        }
        return MarketingClientQuickAddCopy.catalogMode(eventType: eventType, mode: rawValue)
    }

    var catalogKey: String { catalogKey(own: false) }

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
        case .hold: return "pause.circle"
        case .transfer: return "arrow.left.arrow.right"
        case .reprint: return "printer"
        }
    }

    /// Client-only captures the Own Business campaign must never offer.
    var clientOnly: Bool {
        self == .invoice || self == .collection
    }

    func whyNote(client: Bool, own: Bool = false) -> String {
        if client {
            return MarketingClientQuickAddCopy.why(mode: catalogKey)
        }
        if own {
            return MarketingOwnQuickAddCopy.why(mode: catalogKey(own: true))
        }
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

    /// Mode chips shown under the title for Client / Own Figma parity.
    var clientModeChips: [String] {
        switch self {
        case .activity: return ["Creative", "Publish", "Booking", "Event", "Hold"]
        case .progress: return ["Incremental", "Cumulative"]
        case .spend: return ["Incurred", "Accrued", "Reimbursable", "Payment"]
        case .payment: return ["Partial", "Full"]
        case .invoice: return ["Retainer", "Deliverable", "Pass-through", "Reprint"]
        case .collection: return ["Partial", "Full"]
        case .revision, .budget, .target: return ["Budget", "Target", "Transfer"]
        case .approval: return ["Approve", "Reject", "Changes"]
        case .update, .issue: return ["Update", "Issue", "Risk", "Approval"]
        case .result: return ["Leads", "Reach", "Clicks", "Orders", "Sales"]
        default: return []
        }
    }
}

/// Quick-add hub for Client / Own campaigns. Filters tiles from the projection; capture and
/// masters open through the host sheet router (same pattern as Trading's hub).
struct MarketingQuickAddHubView: View {
    let theme: OperatingTheme
    @ObservedObject var model: BusinessOperatingModel
    var onCapture: (MarketingCaptureMode) -> Void
    var onOpenMasters: (OperatingMasterTab) -> Void = { _ in }
    var onOpenStory: () -> Void = {}

    @State private var query = ""

    private var client: Bool { model.vertical == .clientCampaign }
    private var own: Bool { model.vertical == .ownBusinessCampaign }
    private var allowed: Set<String> {
        Set((model.projection?.quickAdds ?? []).map { $0.code.uppercased() })
    }

    private var core: [MarketingCaptureMode] {
        // v5.1 AT-01: 10 client / 8 own — QA-09 is a single Team Update / Issue tile (UPDATE).
        let base: [MarketingCaptureMode] = [
            .activity, .progress, .spend, .payment, .invoice, .collection,
            .revision, .approval, .update, .result,
        ]
        return base.filter { mode in
            if mode.clientOnly && !client { return false }
            guard allowed.isEmpty || allowed.contains(mode.rawValue) else { return false }
            if query.isEmpty { return true }
            return mode.title(client: client, own: own).localizedCaseInsensitiveContains(query)
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
                            title: mode.title(client: client, own: own),
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

                if own {
                    // Own Figma hub masters: Vendors & creators · Services & rate card · Channels & teams · Invite
                    LazyVGrid(
                        columns: [GridItem(.flexible()), GridItem(.flexible())],
                        spacing: 8
                    ) {
                        supportTile("Vendors & creators", "person.2", action: { onOpenMasters(.principals) })
                        supportTile("Services & rate card", "tag", action: { onOpenMasters(.priceLists) })
                        supportTile("Channels & teams", "antenna.radiowaves.left.and.right", action: { onOpenMasters(.locations) })
                        supportTile("Invite & QR", "qrcode", action: { onOpenMasters(.invite) })
                    }
                } else {
                    HStack(spacing: 10) {
                        supportTile("Network", "person.2", action: { onOpenMasters(.principals) })
                        supportTile("Invite & QR", "qrcode", action: { onOpenMasters(.invite) })
                        supportTile("Story", "book", action: onOpenStory)
                    }
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
/// Client path uses MKT-QA indigo/violet chrome + `MarketingClientQuickAddCopy`.
/// Own path reuses the same field registry with magenta/amber chrome + `MarketingOwnQuickAddCopy`.
/// Invoice / Collection remain Client-only. Only fields already on
/// `OperatingEventInput` / `recordOperatingEvent` are posted.
struct MarketingCaptureSheet: View {
    let theme: OperatingTheme
    let mode: MarketingCaptureMode
    @ObservedObject var model: BusinessOperatingModel
    var onRouteToMode: ((MarketingCaptureMode) -> Void)? = nil

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
    @State private var modeChip = ""
    @State private var moreOpen = false
    @State private var dueDate = Date()
    @State private var includeDueDate = false
    @State private var asOfDate = Date()
    @State private var saving = false
    // Client field registry (MKT-QA-01…10 + 08)
    @State private var activityType = ""
    @State private var milestone = ""
    @State private var parentId = ""
    @State private var method = ""
    @State private var reference = ""
    @State private var deliverable = ""
    @State private var invoiceNo = ""
    @State private var tax = ""
    @State private var tds = ""
    @State private var severity = "Normal"
    @State private var assignee = ""
    @State private var periodStart = ""
    @State private var periodEnd = ""
    @State private var reason = ""
    @State private var approver = ""

    private var client: Bool { model.vertical == .clientCampaign }
    private var own: Bool { model.vertical == .ownBusinessCampaign }
    private var marketingOps: Bool { client || own }

    /// Compact open AP for Own Payment context — nil when missing / zero (catalog fallback).
    private var ownContextApAmount: String? {
        guard model.financeVisible,
              let payable = OperatingV12Money.number(model.projection?.finance?.payable),
              payable > 0
        else { return nil }
        return OperatingV12Money.compact(payable, currency: model.currency)
    }

    private var ownContextBudgetAmount: String? {
        guard let amount = OperatingV12Money.number(model.projection?.budget?.amount), amount > 0
        else { return nil }
        return OperatingV12Money.compact(amount, currency: model.currency)
    }
    private let rose = Color(hex: "#F43F5E")

    private var channels: [String] {
        model.projection?.life?.channels ?? BusinessSetupDocSpec.marketingChannels
    }

    private var openActivities: [OperatingActivityPayload] {
        (model.projection?.activity ?? []).filter {
            $0.eventType.uppercased() == "ACTIVITY"
                && ($0.approvalStatus?.uppercased() != "REJECTED")
                && ($0.approvalStatus?.uppercased() != "PENDING")
        }
    }

    private var openPayables: [OperatingPaymentPayload] {
        (model.projection?.paymentReadiness ?? []).filter {
            $0.relevance.uppercased() != "RECEIVABLE"
        }
    }

    private var openInvoices: [OperatingPaymentPayload] {
        (model.projection?.paymentReadiness ?? []).filter {
            $0.relevance.uppercased() == "RECEIVABLE"
        }
    }

    private var pendingApprovals: [OperatingAttentionPayload] {
        (model.projection?.attention ?? []).filter { $0.kind.uppercased() == "APPROVAL" }
    }

    private var metrics: [String] {
        marketingOps
            ? ["LEADS", "REACH", "CLICKS", "ORDERS", "SALES", "CONVERSIONS", "ENGAGEMENT"]
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
        guard !mode.clientOnly || client else { return false }
        if marketingOps {
            switch mode {
            case .activity:
                return !title.trimmingCharacters(in: .whitespaces).isEmpty
                    && !activityType.trimmingCharacters(in: .whitespaces).isEmpty
                    && !party.trimmingCharacters(in: .whitespaces).isEmpty
            case .progress:
                return !amount.trimmingCharacters(in: .whitespaces).isEmpty
            case .spend, .invoice, .collection, .payment, .result, .budget, .revision, .target, .vendorCost:
                return !amount.trimmingCharacters(in: .whitespaces).isEmpty
            case .update, .issue:
                return !title.trimmingCharacters(in: .whitespaces).isEmpty
            case .approval:
                if modeChip == "Reject" || modeChip == "Changes" {
                    return !reason.trimmingCharacters(in: .whitespaces).isEmpty
                }
                return !pendingApprovals.isEmpty
            default:
                return !title.trimmingCharacters(in: .whitespaces).isEmpty
            }
        }
        return !title.trimmingCharacters(in: .whitespaces).isEmpty
            && (!mode.needsAmount || !amount.trimmingCharacters(in: .whitespaces).isEmpty)
    }

    private var ctaLabel: String {
        if client {
            return MarketingClientQuickAddCopy.cta(mode: mode.catalogKey)
        }
        if own {
            return MarketingOwnQuickAddCopy.cta(mode: mode.catalogKey(own: true))
        }
        return "Save"
    }

    var body: some View {
        VStack(spacing: 0) {
            Capsule()
                .fill(Color.white.opacity(0.2))
                .frame(width: 40, height: 4)
                .padding(.top, 10)
                .padding(.bottom, 6)

            ScrollView {
                VStack(alignment: .leading, spacing: 12) {
                    Text(mode.title(client: client, own: own))
                        .font(.plusJakarta(size: 22, weight: .bold))
                        .foregroundStyle(marketingOps ? theme.captureValue : OperatingV12.title)

                    Text(
                        client
                            ? MarketingClientQuickAddCopy.context(mode: mode.catalogKey, brand: model.title)
                            : own
                                ? MarketingOwnQuickAddCopy.context(
                                    mode: mode.catalogKey(own: true),
                                    brand: model.title,
                                    period: OperatingV12Date.monthYear(model.projection?.startDate),
                                    apAmount: ownContextApAmount,
                                    budgetAmount: ownContextBudgetAmount,
                                    budgetPacing: model.projection?.marketing?.pacing
                                  )
                                : "\(model.title) · Own Business"
                    )
                    .font(.plusJakarta(size: 12, weight: .medium))
                    .foregroundStyle(client ? theme.cyan : (own ? theme.captureLabel : OperatingV12.subtitle))

                    if marketingOps, !mode.clientModeChips.isEmpty {
                        chipRow("", options: mode.clientModeChips.filter { client || $0 != "Reprint" }, selection: $modeChip)
                    } else {
                        HStack(spacing: 8) {
                            OperatingPill(theme: theme, label: mode.rawValue, accent: true)
                            OperatingPill(
                                theme: theme,
                                label: client ? "CLIENT" : "OWN",
                                accent: false
                            )
                        }
                    }

                    if mode.clientOnly {
                        clientOnlyRoseGate
                    }

                    if mode.clientOnly && !client {
                        Text("Invoice and collection are Client Campaign only.")
                            .font(.plusJakarta(size: 13))
                            .foregroundStyle(theme.warning)
                            .padding(.bottom, 4)
                    }

                    if marketingOps {
                        clientFields
                        TradingMoreDetails(theme: theme, open: $moreOpen, note: $note)
                        if mode == .approval {
                            dueToggle(label: "Decide by (C)", required: false)
                        }
                    } else {
                        ownFields
                        field("Note", text: $note)
                    }

                    OperatingWhyFootnote(theme: theme, text: mode.whyNote(client: client, own: own))

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
        .background((marketingOps ? theme.captureSheet : theme.canvas).ignoresSafeArea())
        .onAppear(perform: bootstrap)
        .onChange(of: modeChip) { _, chip in
            guard marketingOps else { return }
            routeChipIfNeeded(chip)
        }
    }

    private var clientOnlyRoseGate: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(MarketingClientQuickAddCopy.clientOnlyChip)
                .font(.plusJakarta(size: 12, weight: .semibold))
                .foregroundStyle(rose)
                .padding(.horizontal, 12)
                .padding(.vertical, 6)
                .background(rose.opacity(0.14))
                .overlay(
                    Capsule().stroke(rose.opacity(0.55), lineWidth: 1)
                )
                .clipShape(Capsule())
            Text(MarketingClientQuickAddCopy.clientOnlyGate)
                .font(.plusJakarta(size: 12, weight: .medium))
                .foregroundStyle(rose.opacity(0.92))
        }
    }

    @ViewBuilder
    private var clientFields: some View {
        switch mode {
        case .activity:
            field("Activity name (M)", text: $title)
            field(own ? "Activity type (M)" : "Type (M)", text: $activityType)
            field("Owner (M)", text: $party)
            dueToggle(label: own ? "Due date (M)" : "Due (M)", required: true)
            field("Milestone (O)", text: $milestone)
            let activityChannel = own ? "Channel (C · media type)" : "Channel (C)"
            if !channels.isEmpty {
                chipRow(activityChannel, options: channels, selection: $channel)
            } else {
                field(activityChannel, text: $channel)
            }

        case .progress:
            TradingAmountHero(
                theme: theme,
                label: own ? "Progress (M)" : "Progress % (M)",
                currency: model.currency.isEmpty ? "INR" : model.currency,
                text: $amount,
                hint: own ? "Incremental adds · never exceeds 100%" : "0–100",
                unitOverride: own ? "%" : nil
            )
            openRecordPicker(
                title: openActivities.isEmpty ? "Add an activity before updating progress." : "Activity (M)",
                empty: "No open activities",
                rows: openActivities.map { ($0.id, $0.title) }
            )
            DatePicker("As of (M)", selection: $asOfDate, displayedComponents: .date)
                .datePickerStyle(.compact)
                .font(.plusJakarta(size: 13))
                .foregroundStyle(theme.captureValue)
            // Own Figma: Planned · In progress · Blocked · Done · Cancelled (map to API codes on save).
            chipRow(
                "Status (M)",
                options: own
                    ? ["Planned", "In progress", "Blocked", "Done", "Cancelled"]
                    : ["IN_PROGRESS", "BLOCKED", "POSTPONED", "DONE", "CANCELLED"],
                selection: $activityStatus
            )
            field("Note (O)", text: $note)

        case .spend:
            TradingAmountHero(
                theme: theme,
                label: "Spend amount (M)",
                currency: model.currency.isEmpty ? "INR" : model.currency,
                text: $amount,
                hint: ">0 · Spend ≠ payment"
            )
            field("Vendor (M)", text: $party)
            field("Activity (M)", text: $deliverable)
            DatePicker("Spend date (M)", selection: $asOfDate, displayedComponents: .date)
                .datePickerStyle(.compact)
                .font(.plusJakarta(size: 13))
                .foregroundStyle(theme.captureValue)
            if !channels.isEmpty {
                chipRow("Channel (C)", options: channels, selection: $channel)
            } else {
                field("Channel (C)", text: $channel)
            }
            dueToggle(label: own ? "Due date (C · payable)" : "Due (C)", required: false)

        case .payment:
            TradingAmountHero(
                theme: theme,
                label: "Payment amount (M)",
                currency: model.currency.isEmpty ? "INR" : model.currency,
                text: $amount,
                hint: "≤ outstanding"
            )
            openRecordPicker(
                title: "Open payable (M)",
                empty: "Record a spend before paying a vendor.",
                rows: openPayables.map { ($0.id, $0.title) }
            )
            DatePicker("Paid on (M)", selection: $asOfDate, displayedComponents: .date)
                .datePickerStyle(.compact)
                .font(.plusJakarta(size: 13))
                .foregroundStyle(theme.captureValue)
            field("Method (M)", text: $method)
            field("Reference (O)", text: $reference)

        case .invoice:
            TradingAmountHero(
                theme: theme,
                label: "Invoice amount (M)",
                currency: model.currency.isEmpty ? "INR" : model.currency,
                text: $amount,
                hint: ">0"
            )
            field(
                "Client (M)",
                text: Binding(
                    get: { party.isEmpty ? (model.projection?.partyName ?? "") : party },
                    set: { party = $0 }
                )
            )
            field("Deliverable (M)", text: $deliverable)
            field("Invoice no. (M)", text: $invoiceNo)
            dueToggle(label: "Due (M)", required: true)
            field("Tax (C)", text: $tax)

        case .collection:
            TradingAmountHero(
                theme: theme,
                label: "Collection amount (M)",
                currency: model.currency.isEmpty ? "INR" : model.currency,
                text: $amount,
                hint: "≤ outstanding"
            )
            openRecordPicker(
                title: "Open invoice (M)",
                empty: "Record an invoice before collecting.",
                rows: openInvoices.map { ($0.id, $0.title) }
            )
            DatePicker("Received on (M)", selection: $asOfDate, displayedComponents: .date)
                .datePickerStyle(.compact)
                .font(.plusJakarta(size: 13))
                .foregroundStyle(theme.captureValue)
            field("Method (M)", text: $method)
            field("TDS (C)", text: $tds)

        case .revision, .budget, .target:
            Text("Current: Budget · Target — revisions are versioned and effective-dated.")
                .font(.plusJakarta(size: 12, weight: .medium))
                .foregroundStyle(theme.captureLabel)
            TradingAmountHero(
                theme: theme,
                label: modeChip == "Target" || mode == .target ? "New target (M)" : "New budget (M)",
                currency: model.currency.isEmpty ? "INR" : model.currency,
                text: $amount,
                hint: "≥ 0"
            )
            DatePicker("Effective from (M)", selection: $asOfDate, displayedComponents: .date)
                .datePickerStyle(.compact)
                .font(.plusJakarta(size: 13))
                .foregroundStyle(theme.captureValue)
            field("Reason (M)", text: $reason)
            field("Approver (C)", text: $approver)

        case .approval:
            // MKT-QA-08 — Item, Requested by, Decision note, Effective (O), Decide by (C).
            openRecordPicker(
                title: pendingApprovals.isEmpty ? "Nothing is waiting for a decision." : "Item (M)",
                empty: "No pending approvals",
                rows: pendingApprovals.map { item in
                    let amountBit = item.amount.flatMap { $0.isEmpty ? nil : " · ₹\($0)" } ?? ""
                    return (item.id, item.title + amountBit)
                }
            )
            let requestedRaw = pendingApprovals
                .first(where: { $0.id == parentId })?
                .counterpartyName?
                .trimmingCharacters(in: .whitespacesAndNewlines)
            let requestedLabel = (requestedRaw?.isEmpty == false) ? (requestedRaw ?? "—") : "—"
            VStack(alignment: .leading, spacing: 4) {
                Text("Requested by")
                    .font(.plusJakarta(size: 12, weight: .medium))
                    .foregroundStyle(theme.captureLabel)
                Text(requestedLabel)
                    .font(.plusJakarta(size: 15))
                    .foregroundStyle(theme.captureValue)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 11)
                    .background(theme.captureField)
                    .overlay(
                        RoundedRectangle(cornerRadius: 12, style: .continuous)
                            .stroke(Color(hex: "#475569"), lineWidth: 1)
                    )
                    .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
            }
            field("Decision note (C · reject/hold)", text: $reason)
            DatePicker("Effective (O)", selection: $asOfDate, displayedComponents: .date)
                .datePickerStyle(.compact)
                .font(.plusJakarta(size: 13))
                .foregroundStyle(theme.captureValue)
            // Decide by (C) renders after More details (Figma order).

        case .update, .issue:
            field("Title (M)", text: $title)
            field("Activity (C)", text: $deliverable)
            if modeChip == "Issue" || modeChip == "Risk" {
                chipRow("Severity (C)", options: ["Low", "Normal", "High"], selection: $severity)
                field("Assignee (O)", text: $assignee)
                dueToggle(label: "Due (O)", required: false)
            }

        case .result:
            TradingAmountHero(
                theme: theme,
                label: "Value (M)",
                currency: model.currency.isEmpty ? "INR" : model.currency,
                text: $amount,
                hint: "Leave blank when not measured"
            )
            chipRow("Metric (M)", options: metrics, selection: $metric)
            if !channels.isEmpty {
                chipRow("Channel (M)", options: channels, selection: $channel)
            } else {
                field("Channel (M)", text: $channel)
            }
            field("Period (M)", text: $periodStart)
            field("Coverage (M)", text: $periodEnd)
            field("Attributed sales (O)", text: $attributedSales, decimal: true)

        default:
            if mode.needsAmount {
                TradingAmountHero(
                    theme: theme,
                    label: amountLabel,
                    currency: model.currency.isEmpty ? "INR" : model.currency,
                    text: $amount,
                    hint: ">0"
                )
            }
            field("Title (M)", text: $title)
        }
    }

    @ViewBuilder
    private var ownFields: some View {
        if mode.needsAmount {
            field(amountLabel, text: $amount, decimal: true)
        }
        field("Title", text: $title)
        if mode.needsChannel {
            chipRow("Channel", options: channels, selection: $channel)
        }
        if mode.needsParty {
            field("Vendor / creator", text: $party)
        }
        if mode == .result || mode == .progress {
            chipRow("Metric", options: metrics, selection: $metric)
            chipRow("Reporting", options: ["INCREMENTAL", "CUMULATIVE"], selection: $reportingMode)
            if mode == .result {
                field("Attributed sales (O)", text: $attributedSales, decimal: true)
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
                DatePicker("Due", selection: $dueDate, displayedComponents: .date)
                    .datePickerStyle(.compact)
                    .font(.plusJakarta(size: 13))
                    .foregroundStyle(OperatingV12.title)
            }
        }
    }

    private func openRecordPicker(title: String, empty: String, rows: [(String, String)]) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(title)
                .font(.plusJakarta(size: 12, weight: .medium))
                .foregroundStyle(theme.captureLabel)
            if rows.isEmpty {
                Text(empty)
                    .font(.plusJakarta(size: 13))
                    .foregroundStyle(theme.captureHint)
            } else {
                ForEach(rows, id: \.0) { row in
                    Button {
                        parentId = row.0
                        if title.hasPrefix("Open"), amount.isEmpty,
                           let pay = (model.projection?.paymentReadiness ?? []).first(where: { $0.id == row.0 }) {
                            amount = pay.amount
                            if party.isEmpty { party = pay.counterpartyName ?? "" }
                        }
                    } label: {
                        Text(parentId == row.0 ? "Selected \(row.1)" : row.1)
                            .font(.plusJakarta(size: 14, weight: .semibold))
                            .foregroundStyle(theme.captureValue)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .padding(10)
                            .background(
                                parentId == row.0
                                    ? theme.accent.opacity(0.15)
                                    : Color.clear
                            )
                            .overlay(
                                RoundedRectangle(cornerRadius: 12, style: .continuous)
                                    .stroke(
                                        parentId == row.0 ? theme.accent : Color(hex: "#475569"),
                                        lineWidth: 1
                                    )
                            )
                            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }

    private func dueToggle(label: String, required: Bool) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Toggle(isOn: $includeDueDate) {
                Text(label)
                    .font(.plusJakarta(size: 13))
                    .foregroundStyle(theme.captureValue)
            }
            .tint(theme.accent)
            .onAppear {
                if required { includeDueDate = true }
            }
            if includeDueDate {
                DatePicker("Due", selection: $dueDate, displayedComponents: .date)
                    .datePickerStyle(.compact)
                    .font(.plusJakarta(size: 13))
                    .foregroundStyle(theme.captureValue)
            }
        }
    }

    private func bootstrap() {
        if channel.isEmpty { channel = channels.first ?? "" }
        if title.isEmpty, !client || (mode != .activity && mode != .update && mode != .issue) {
            title = mode.title(client: client, own: own)
        }
        if modeChip.isEmpty {
            modeChip = mode.clientModeChips.filter { client || $0 != "Reprint" }.first ?? ""
        }
        if mode == .invoice || mode == .payment || mode == .collection || mode == .activity {
            includeDueDate = true
        }
        if client, mode == .invoice, party.isEmpty {
            party = model.projection?.partyName ?? ""
        }
        if marketingOps, mode == .progress {
            activityStatus = own ? "In progress" : "IN_PROGRESS"
            if !modeChip.isEmpty { reportingMode = modeChip.uppercased() }
        }
        if marketingOps, mode == .result, ["Leads", "Reach", "Clicks", "Orders", "Sales"].contains(modeChip) {
            metric = modeChip.uppercased()
        }
        if marketingOps, mode == .revision || mode == .budget || mode == .target {
            revisionKind = modeChip == "Target" ? "TARGET" : "BUDGET"
        }
    }

    private func routeChipIfNeeded(_ chip: String) {
        switch (mode, chip) {
        case (.activity, "Hold"):
            // Chrome-only HOLD sheet — never posts; never Trading stock layouts.
            onRouteToMode?(.hold) ?? dismiss()
        case (.spend, "Payment"):
            onRouteToMode?(.payment) ?? dismiss()
        case (.revision, "Transfer"), (.budget, "Transfer"), (.target, "Transfer"):
            onRouteToMode?(.transfer) ?? dismiss()
        case (.update, "Approval"), (.issue, "Approval"):
            onRouteToMode?(.approval) ?? dismiss()
        case (.invoice, "Reprint") where client:
            onRouteToMode?(.reprint) ?? dismiss()
        case (.progress, "Incremental"), (.progress, "Cumulative"):
            reportingMode = chip.uppercased()
        case (.result, _):
            if ["Leads", "Reach", "Clicks", "Orders", "Sales"].contains(chip) {
                metric = chip.uppercased()
            }
        case (.revision, "Budget"), (.budget, "Budget"), (.target, "Budget"):
            revisionKind = "BUDGET"
        case (.revision, "Target"), (.budget, "Target"), (.target, "Target"):
            revisionKind = "TARGET"
        default:
            break
        }
    }

    private var stickyFooter: some View {
        VStack(spacing: 8) {
            // Figma sticky footer: CTA then draft meta (Client + Own).
            if !marketingOps {
                Text(mode.whyNote(client: client, own: own))
                    .font(.plusJakarta(size: 11, weight: .medium))
                    .foregroundStyle(OperatingV12.subtitle)
                    .lineLimit(2)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
            Button {
                Task { await save() }
            } label: {
                Text(saving ? "Saving…" : ctaLabel)
                    .font(.plusJakarta(size: 15, weight: .semibold))
                    .foregroundStyle(theme.onAccent)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 14)
                    .background(
                        Group {
                            if canSave && !saving {
                                if marketingOps {
                                    LinearGradient(
                                        colors: [theme.accent, theme.accentSoft],
                                        startPoint: .leading,
                                        endPoint: .trailing
                                    )
                                } else {
                                    theme.accent
                                }
                            } else {
                                theme.accent.opacity(0.35)
                            }
                        }
                    )
                    .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                    .shadow(color: theme.accent.opacity(canSave ? 0.18 : 0), radius: 14, y: 8)
            }
            .buttonStyle(.plain)
            .disabled(!canSave || saving)
            if client {
                Text(MarketingClientQuickAddCopy.stickyMeta)
                    .font(.plusJakarta(size: 10))
                    .foregroundStyle(theme.captureHint)
                    .multilineTextAlignment(.center)
                    .frame(maxWidth: .infinity)
            } else if own {
                Text(MarketingOwnQuickAddCopy.stickyMeta)
                    .font(.plusJakarta(size: 10))
                    .foregroundStyle(theme.captureHint)
                    .multilineTextAlignment(.center)
                    .frame(maxWidth: .infinity)
            }
        }
        .padding(.horizontal, 18)
        .padding(.top, 10)
        .padding(.bottom, 18)
        .background(marketingOps ? theme.captureSheet : theme.canvas)
    }

    private var amountLabel: String {
        switch mode {
        case .spend, .vendorCost: return marketingOps ? "Spend amount (M)" : "Spent"
        case .payment: return marketingOps ? "Payment amount (M)" : "Paid"
        case .collection: return client ? "Collection amount (M)" : "Collected"
        case .invoice: return client ? "Invoice amount (M)" : "Invoice amount"
        case .result, .progress: return marketingOps ? (mode == .progress ? "Progress % (M)" : "Value (M)") : "Value"
        case .budget, .revision: return marketingOps ? (modeChip == "Target" ? "New target (M)" : "New budget (M)") : "Budget"
        case .target: return marketingOps ? "New target (M)" : "Target"
        default: return "Amount"
        }
    }

    private func save() async {
        guard canSave else { return }
        if mode.clientOnly && !client { return }
        saving = true
        defer { saving = false }

        let dayFormatter = DateFormatter()
        dayFormatter.dateFormat = "yyyy-MM-dd"
        dayFormatter.locale = Locale(identifier: "en_US_POSIX")

        let dueAt: String? = {
            guard includeDueDate else { return nil }
            if marketingOps {
                switch mode {
                case .activity, .invoice, .spend, .update, .issue, .approval: break
                default: return nil
                }
            } else if !mode.needsDueDate {
                return nil
            }
            return dayFormatter.string(from: dueDate)
        }()

        var saveTitle = title.trimmingCharacters(in: .whitespaces)
        if marketingOps {
            switch mode {
            case .activity:
                saveTitle = title.trimmingCharacters(in: .whitespaces)
            case .spend:
                saveTitle = deliverable.isEmpty ? mode.title(client: client, own: own) : deliverable
            case .invoice:
                saveTitle = invoiceNo.isEmpty
                    ? (deliverable.isEmpty ? mode.title(client: true) : deliverable)
                    : "Invoice \(invoiceNo)"
            case .payment, .collection:
                saveTitle = mode.title(client: client, own: own)
            case .progress:
                saveTitle = mode.title(client: client, own: own)
            case .revision, .budget, .target:
                saveTitle = reason.isEmpty ? mode.title(client: client, own: own) : reason
            case .approval:
                saveTitle = mode.title(client: client, own: own)
            case .result:
                saveTitle = "\(metric) result"
            default:
                break
            }
        }

        var composedNote = note
        if marketingOps {
            var bits: [String] = []
            if !note.isEmpty { bits.append(note) }
            switch mode {
            case .activity:
                if !activityType.isEmpty { bits.append("Type: \(activityType)") }
                if !milestone.isEmpty { bits.append("Milestone: \(milestone)") }
            case .payment:
                if !method.isEmpty { bits.append("Method: \(method)") }
                if !reference.isEmpty { bits.append("Ref: \(reference)") }
            case .invoice:
                if !deliverable.isEmpty { bits.append("Deliverable: \(deliverable)") }
                if !tax.isEmpty { bits.append("Tax: \(tax)") }
            case .collection:
                if !method.isEmpty { bits.append("Method: \(method)") }
                if !tds.isEmpty { bits.append("TDS: \(tds)") }
            case .update, .issue:
                if !deliverable.isEmpty { bits.append("Activity: \(deliverable)") }
                if modeChip == "Issue" || modeChip == "Risk" {
                    bits.append("Severity: \(severity)")
                    if !assignee.isEmpty { bits.append("Assignee: \(assignee)") }
                }
            case .approval:
                if !reason.isEmpty { bits.append(reason) }
            case .result:
                if !periodStart.isEmpty { bits.append("Period: \(periodStart)") }
                if !periodEnd.isEmpty { bits.append("Coverage: \(periodEnd)") }
            case .revision, .budget, .target:
                if !approver.isEmpty { bits.append("Approver: \(approver)") }
            default:
                break
            }
            composedNote = bits.joined(separator: " · ")
        }

        var input = OperatingEventInput(
            momentId: model.momentId,
            eventType: mode.eventType,
            title: saveTitle.isEmpty ? mode.title(client: client, own: own) : saveTitle,
            amount: amount.isEmpty ? nil : amount,
            note: composedNote.isEmpty ? nil : composedNote,
            channelName: channel.isEmpty ? nil : channel,
            counterpartyName: party.isEmpty ? nil : party,
            occurredAt: ISO8601DateFormatter().string(from: Date()),
            parentEventId: parentId.isEmpty ? nil : parentId,
            dueAt: dueAt,
            paymentStatus: (mode == .payment || mode == .collection || mode == .invoice)
                ? paymentStatus
                : nil
        )
        if mode == .result || mode == .progress {
            input.metricCode = metric
            input.reportingMode = client
                ? (modeChip.uppercased().hasPrefix("CUMUL") ? "CUMULATIVE" : "INCREMENTAL")
                : reportingMode
        }
        if mode == .result {
            if !attributedSales.isEmpty { input.attributedSales = attributedSales }
            if !client { input.resultSource = resultSource }
        }
        if mode == .spend || mode == .invoice || mode == .vendorCost {
            input.budgetCategory = budgetCategory
        }
        if mode == .revision || mode == .budget || mode == .target {
            input.revisionKind = client
                ? (modeChip == "Target" || mode == .target ? "TARGET" : "BUDGET")
                : revisionKind
            input.effectiveFrom = dayFormatter.string(from: asOfDate)
        }
        if mode == .activity {
            input.activityStatus = marketingOps ? "IN_PROGRESS" : activityStatus
        }
        if mode == .progress {
            input.activityStatus = Self.mapProgressStatus(activityStatus, own: own)
            input.occurredAt = ISO8601DateFormatter().string(from: asOfDate)
        }
        if mode == .approval, !creativeVersion.isEmpty {
            input.creativeVersion = creativeVersion
        }
        if mode == .spend || mode == .payment || mode == .collection {
            input.occurredAt = ISO8601DateFormatter().string(from: asOfDate)
        }
        let ok = await model.record(input)
        if ok { dismiss() }
    }

    /// Own Figma human chips → API activityStatus codes.
    private static func mapProgressStatus(_ raw: String, own: Bool) -> String {
        if !own { return raw }
        switch raw {
        case "Planned": return "OPEN"
        case "Blocked": return "BLOCKED"
        case "Done": return "DONE"
        case "Cancelled": return "CANCELLED"
        default: return "IN_PROGRESS"
        }
    }

    private func field(_ label: String, text: Binding<String>, decimal: Bool = false) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label)
                .font(.plusJakarta(size: 12, weight: .medium))
                .foregroundStyle(marketingOps ? theme.captureLabel : OperatingV12.subtitle)
            TextField("", text: text)
                .keyboardType(decimal ? .decimalPad : .default)
                .font(.plusJakarta(size: 15))
                .foregroundStyle(marketingOps ? theme.captureValue : OperatingV12.title)
                .padding(.horizontal, 12)
                .padding(.vertical, 11)
                .background(marketingOps ? theme.captureField : OperatingV12.inset)
                .overlay(
                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                        .stroke(marketingOps ? Color(hex: "#475569") : theme.border, lineWidth: 1)
                )
                .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
        }
    }

    private func chipRow(_ label: String, options: [String], selection: Binding<String>) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            if !label.isEmpty {
                Text(label)
                    .font(.plusJakarta(size: 12, weight: .medium))
                    .foregroundStyle(marketingOps ? theme.captureLabel : OperatingV12.subtitle)
            }
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(options, id: \.self) { option in
                        Button {
                            selection.wrappedValue = option
                        } label: {
                            Text(option)
                                .font(.plusJakarta(size: 13, weight: selection.wrappedValue == option ? .bold : .medium))
                                .foregroundStyle(selection.wrappedValue == option ? theme.onAccent : (marketingOps ? theme.captureLabel : theme.muted))
                                .padding(.horizontal, 14)
                                .padding(.vertical, 8)
                                .background(selection.wrappedValue == option ? theme.accent : (marketingOps ? theme.captureField : theme.inset))
                                .overlay(
                                    Capsule()
                                        .stroke(
                                            selection.wrappedValue == option
                                                ? theme.accentSoft
                                                : (marketingOps ? Color(hex: "#475569") : theme.border),
                                            lineWidth: 1
                                        )
                                )
                                .clipShape(Capsule())
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
        }
    }
}

/// Figma MKT-QA-HOLD / TRANSFER / REPRINT — titles, WHY, CTA chrome only; CTA dismisses, never posts.
struct MarketingClientChromeOnlySheet: View {
    let theme: OperatingTheme
    let mode: String
    let brand: String
    var onNavigate: (String) -> Void
    var onDismiss: () -> Void

    private var own: Bool { theme.isMarketingOwnCapture }
    private var key: String { mode.uppercased() }
    private var title: String {
        own ? MarketingOwnQuickAddCopy.title(mode: key) : MarketingClientQuickAddCopy.title(mode: key)
    }
    private var contextLine: String {
        switch key {
        case "HOLD": return "Paused campaign · no spend, no billing"
        case "TRANSFER": return "Move budget between channels · no revenue, no spend"
        case "REPRINT": return "Same invoice no. · no new sale / AR"
        default:
            return own
                ? MarketingOwnQuickAddCopy.context(mode: key, brand: brand)
                : MarketingClientQuickAddCopy.context(mode: key, brand: brand)
        }
    }
    // Chrome-only Hold/Transfer/Reprint sheets have no live projection — catalog strings only.
    private var cta: String {
        switch key {
        case "HOLD": return "Hold campaign · preview"
        case "TRANSFER": return "Confirm transfer · preview"
        default:
            return own
                ? MarketingOwnQuickAddCopy.cta(mode: key)
                : MarketingClientQuickAddCopy.cta(mode: key)
        }
    }
    private var sticky: String {
        switch key {
        case "HOLD": return "No spend or billing while held · write API not available"
        case "TRANSFER": return "Transfer never posts spend · write API not available"
        case "REPRINT": return "Reprint never posts a new invoice · write API not available"
        default:
            return own ? MarketingOwnQuickAddCopy.stickyMeta : MarketingClientQuickAddCopy.stickyMeta
        }
    }
    private var navChips: [String] { own ? ["Hold", "Transfer"] : ["Hold", "Transfer", "Reprint"] }

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            Capsule()
                .fill(Color(hex: "#64748B"))
                .frame(width: 40, height: 4)
                .frame(maxWidth: .infinity)
            HStack {
                Text(title)
                    .font(.plusJakarta(size: 22, weight: .bold))
                    .foregroundStyle(theme.captureValue)
                Spacer()
                Button(action: onDismiss) {
                    Text("✕")
                        .font(.plusJakarta(size: 13, weight: .bold))
                        .foregroundStyle(theme.captureLabel)
                        .frame(width: 32, height: 32)
                        .background(theme.captureField, in: RoundedRectangle(cornerRadius: 16))
                }
                .buttonStyle(.plain)
            }
            Text(contextLine)
                .font(.plusJakarta(size: 12, weight: .medium))
                .foregroundStyle(theme.captureLabel)
            FlowLayout(spacing: 6) {
                ForEach(navChips, id: \.self) { chip in
                    let selected = (chip == "Hold" && key == "HOLD")
                        || (chip == "Transfer" && key == "TRANSFER")
                        || (chip == "Reprint" && key == "REPRINT")
                    Button {
                        onNavigate(chip.uppercased())
                    } label: {
                        Text(chip)
                            .font(.plusJakarta(size: 12, weight: selected ? .bold : .medium))
                            .foregroundStyle(selected ? theme.onAccent : theme.captureLabel)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 8)
                            .background(selected ? theme.accent : theme.captureField, in: Capsule())
                            .overlay(
                                Capsule().stroke(selected ? theme.accentSoft : Color(hex: "#475569"), lineWidth: 1)
                            )
                    }
                    .buttonStyle(.plain)
                }
            }
            VStack(alignment: .leading, spacing: 4) {
                HStack {
                    Text(key == "HOLD" ? "Hold slip" : key == "TRANSFER" ? "Transfer slip" : "Invoice copy")
                        .font(.plusJakarta(size: 11, weight: .medium))
                        .foregroundStyle(theme.captureLabel)
                    Spacer()
                    Text(key == "HOLD" ? "CH-…" : key == "TRANSFER" ? "TRF-…" : "INV-…")
                        .font(.plusJakarta(size: 12, weight: .bold))
                        .foregroundStyle(theme.accentSoft)
                }
                Text(brand.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? "Campaign" : brand)
                    .font(.plusJakarta(size: 22, weight: .bold))
                    .foregroundStyle(theme.captureValue)
                Text("Preview only · no write API on this surface")
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(theme.captureLabel)
            }
            .padding(16)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(theme.captureField, in: RoundedRectangle(cornerRadius: 16))
            .overlay(RoundedRectangle(cornerRadius: 16).stroke(theme.accent, lineWidth: 1.5))

            HStack(alignment: .top, spacing: 8) {
                Text("Why")
                    .font(.plusJakarta(size: 11, weight: .bold))
                    .foregroundStyle(theme.accentSoft)
                Text(own ? MarketingOwnQuickAddCopy.why(mode: key) : MarketingClientQuickAddCopy.why(mode: key))
                    .font(.plusJakarta(size: 11))
                    .foregroundStyle(theme.whyBody)
                    .fixedSize(horizontal: false, vertical: true)
            }
            .padding(10)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(theme.whyFill, in: RoundedRectangle(cornerRadius: 10))
            .overlay(RoundedRectangle(cornerRadius: 10).stroke(theme.whyBorder, lineWidth: 1))

            Button(action: onDismiss) {
                Text(cta)
                    .font(.plusJakarta(size: 15, weight: .bold))
                    .foregroundStyle(theme.captureValue)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 16)
                    .background(
                        LinearGradient(colors: [theme.accent, theme.accentSoft], startPoint: .leading, endPoint: .trailing),
                        in: RoundedRectangle(cornerRadius: 16)
                    )
            }
            .buttonStyle(.plain)
            Text(sticky)
                .font(.plusJakarta(size: 10))
                .foregroundStyle(theme.captureLabel)
                .frame(maxWidth: .infinity)
        }
        .padding(.horizontal, 20)
        .padding(.top, 12)
        .padding(.bottom, 28)
        .background(theme.captureSheet.ignoresSafeArea())
    }
}

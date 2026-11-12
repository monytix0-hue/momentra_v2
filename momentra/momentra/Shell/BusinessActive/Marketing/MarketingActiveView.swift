import SwiftUI

/// Client / Own campaign populated tabs — Pulse, Moments, Life, Memory, and the Create hub.
///
/// Mirrors `OperatingActiveView` (Trading v12): one sheet router for View-all + row detail,
/// OperatingV12 chrome, and Client indigo / Own magenta via `OperatingTheme`.
struct MarketingActiveView: View {
    @ObservedObject var model: BusinessOperatingModel
    let destination: BottomDestination
    let refreshToken: UInt64
    var onOpenQuickAdd: () -> Void = {}
    var onOpenStory: () -> Void = {}
    var onOpenMasters: (OperatingMasterTab) -> Void = { _ in }
    var onCapture: ((String) -> Void)? = nil

    @State private var activeSheet: MarketingActiveSheet?

    private var theme: OperatingTheme { OperatingTheme(vertical: model.vertical) }
    private var client: Bool { model.vertical == .clientCampaign }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                phaseNotice
                switch destination {
                case .pulse:
                    MarketingPulseScreen(
                        model: model,
                        theme: theme,
                        client: client,
                        onCapture: capture,
                        onViewAll: openSheet,
                        onOpenRow: openRow
                    )
                case .moments:
                    MarketingMomentsScreen(
                        model: model,
                        theme: theme,
                        client: client,
                        onCapture: capture,
                        onViewAll: openSheet,
                        onOpenRow: openRow
                    )
                case .life:
                    MarketingLifeScreen(
                        model: model,
                        theme: theme,
                        client: client,
                        onCapture: capture,
                        onViewAll: openSheet,
                        onOpenRow: openRow
                    )
                case .memory:
                    MarketingMemoryScreen(
                        model: model,
                        theme: theme,
                        client: client,
                        onCapture: capture,
                        onStory: onOpenStory,
                        onViewAll: openSheet,
                        onOpenRow: openRow
                    )
                case .create:
                    MarketingHubScreen(
                        model: model,
                        theme: theme,
                        client: client,
                        onCapture: capture,
                        onOpenMasters: onOpenMasters,
                        onOpenStory: onOpenStory
                    )
                }
            }
            .padding(.bottom, 32)
        }
        .background(
            LinearGradient(
                colors: [theme.canvasTop, theme.canvas],
                startPoint: .top,
                endPoint: .bottom
            )
        )
        .task { await model.load() }
        .onChange(of: refreshToken) { _, _ in
            Task { await model.load() }
        }
        .sheet(item: $activeSheet) { sheet in
            Group {
                switch sheet {
                case let .viewAll(spec):
                    OperatingViewAllSheet(
                        spec: spec,
                        theme: theme,
                        model: model,
                        onOpenRow: { activeSheet = .row($0) },
                        onFooterAction: { action in
                            activeSheet = nil
                            model.periodFilter = nil
                            model.statusFilter = nil
                            model.periodFrom = nil
                            model.periodTo = nil
                            switch action {
                            case "MEMORY":
                                onOpenStory()
                            case "ATTENTION", "QUICK_ADD":
                                onOpenQuickAdd()
                            default:
                                capture(action)
                            }
                        }
                    )
                case let .row(row):
                    OperatingRecordSheet(theme: theme, row: row, model: model)
                }
            }
            .presentationDetents([.large])
            .presentationDragIndicator(.visible)
            .presentationCornerRadius(24)
            .preferredColorScheme(.dark)
        }
    }

    private func capture(_ code: String) {
        if let onCapture {
            onCapture(code)
        } else {
            onOpenQuickAdd()
        }
    }

    private func openRow(_ row: OperatingActivityPayload) {
        activeSheet = .row(row)
    }

    private func openSheet(_ code: String) {
        if let spec = OperatingSheetCatalog.spec(forCode: code, vertical: model.vertical) {
            activeSheet = .viewAll(spec)
            return
        }
        // Board codes the shared catalogue has not yet named (M1/M2/M3) still open from title.
        if let section = model.section(code: code) {
            activeSheet = .viewAll(
                OperatingSheetSpec(
                    code: section.code,
                    title: section.title,
                    eyebrow: section.detail,
                    emptyTitle: "Nothing recorded yet",
                    emptyBody: section.detail.isEmpty
                        ? "Board updates will show here."
                        : section.detail
                )
            )
        }
    }

    @ViewBuilder
    private var phaseNotice: some View {
        switch model.phase {
        case .loading where model.projection == nil:
            HStack(spacing: 8) {
                ProgressView().tint(theme.accent)
                Text("Reading what was recorded…")
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(OperatingV12.subtitle)
            }
            .padding(.horizontal, 16)
        case let .error(message):
            OperatingV12Card(theme: theme, fill: theme.isMarketingOwnCapture ? theme.surface : OperatingV12.surface) {
                Text("Couldn't load this campaign")
                    .font(.plusJakarta(size: 14, weight: .bold))
                    .foregroundStyle(OperatingV12.title)
                Text(message)
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(OperatingV12.subtitle)
                Button {
                    Task { await model.load() }
                } label: {
                    Text("Retry")
                        .font(.plusJakarta(size: 14, weight: .bold))
                        .foregroundStyle(OperatingV12.title)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 12)
                        .background(theme.inset)
                        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                }
                .buttonStyle(.plain)
            }
            .padding(.horizontal, 16)
        case .empty:
            OperatingV12Card(theme: theme, fill: theme.isMarketingOwnCapture ? theme.surface : OperatingV12.surface) {
                Text("This campaign is closed")
                    .font(.plusJakarta(size: 14, weight: .bold))
                    .foregroundStyle(OperatingV12.title)
                Text("A closed moment is read-only. Start a new one to record again.")
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(OperatingV12.subtitle)
            }
            .padding(.horizontal, 16)
        default:
            EmptyView()
        }
    }
}

private enum MarketingActiveSheet: Identifiable {
    case viewAll(OperatingSheetSpec)
    case row(OperatingActivityPayload)

    var id: String {
        switch self {
        case let .viewAll(spec): return "viewAll:\(spec.code)"
        case let .row(row): return "row:\(row.id)"
        }
    }
}

// MARK: - Shared context chips

private struct MarketingContextChips: View {
    let theme: OperatingTheme
    let client: Bool
    let model: BusinessOperatingModel

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                OperatingV12Chip(
                    label: client ? "Client Campaign" : "Own Business",
                    tone: theme.accent
                )
                if let party = model.projection?.partyName, !party.isEmpty {
                    OperatingV12Chip(label: party, tone: OperatingV12.subtitle)
                }
                OperatingV12Chip(
                    label: "\(OperatingV12Date.monthYear(model.projection?.startDate) ?? "This month") · \(model.currency)",
                    tone: OperatingV12.subtitle
                )
            }
        }
        .scrollClipDisabled()
        .padding(.horizontal, 16)
    }
}

// MARK: - Marketing quick actions (Spend / Result / Payment / Invoice|Approval)

private struct MarketingV12QuickActions: View {
    let theme: OperatingTheme
    let client: Bool
    var tinted: Bool = false
    var onOpen: (String) -> Void

    private struct Action: Identifiable {
        let code: String
        let systemImage: String
        let label: String
        var id: String { code }
    }

    private var actions: [Action] {
        var list = [
            Action(code: "SPEND", systemImage: "wallet.pass", label: "Spend"),
            Action(code: "RESULT", systemImage: "chart.line.uptrend.xyaxis", label: "Result"),
            Action(code: "PAYMENT", systemImage: "creditcard", label: "Payment"),
        ]
        if client {
            list.append(Action(code: "INVOICE", systemImage: "doc.text", label: "Invoice"))
        } else {
            list.append(Action(code: "APPROVAL", systemImage: "checkmark.shield", label: "Approval"))
        }
        return list
    }

    var body: some View {
        HStack(spacing: 8) {
            ForEach(Array(actions.enumerated()), id: \.element.id) { index, action in
                let selected = tinted && index == 0
                Button {
                    onOpen(action.code)
                } label: {
                    VStack(spacing: 6) {
                        if selected {
                            Circle()
                                .fill(theme.accent)
                                .frame(width: 4, height: 4)
                        }
                        Image(systemName: action.systemImage)
                            .font(.system(size: 15, weight: .medium))
                            .foregroundStyle(selected ? theme.accent : theme.accent)
                        Text(action.label)
                            .font(.plusJakarta(size: 11, weight: .bold))
                            .foregroundStyle(OperatingV12.title)
                            .lineLimit(1)
                            .minimumScaleFactor(0.75)
                    }
                    .padding(.horizontal, 10)
                    .padding(.vertical, 10)
                    .frame(maxWidth: .infinity)
                    .background(tinted ? theme.accent.opacity(0.12) : OperatingV12.inset)
                    .overlay(
                        RoundedRectangle(cornerRadius: 14, style: .continuous)
                            .stroke(
                                selected ? theme.accent.opacity(0.55) : theme.accent.opacity(0.28),
                                lineWidth: 1
                            )
                    )
                    .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                }
                .buttonStyle(.plain)
            }
        }
    }
}

// MARK: - Pulse (`MKT-PULSE-v12` / `MKT-OWN-PULSE-v12`)

/// Marketing Pulse — GST Position is an intentional omit vs Retail/Restaurant/Service/MFG:
/// MKT-PULSE-v12 tracks leads/budget/AR, not tax filing position.
private struct MarketingPulseScreen: View {
    @ObservedObject var model: BusinessOperatingModel
    let theme: OperatingTheme
    let client: Bool
    var onCapture: (String) -> Void
    var onViewAll: (String) -> Void
    var onOpenRow: ((OperatingActivityPayload) -> Void)? = nil

    private var projection: OperatingProjectionPayload? { model.projection }

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            MarketingContextChips(theme: theme, client: client, model: model)

            OperatingV12PulseHero(
                theme: theme,
                chips: heroChips,
                title: model.title,
                subtitle: heroSubtitle,
                badge: projection?.retired == true ? "Campaign closed" : "Campaign open · live",
                glyph: "megaphone"
            )

            MarketingV12QuickActions(theme: theme, client: client, tinted: true, onOpen: onCapture)
                .padding(.horizontal, 20)

            VStack(alignment: .leading, spacing: 16) {
                if model.financeVisible {
                    attentionCard
                    targetCard
                    paymentCard
                    financeCard
                } else {
                    restrictedPulse
                    attentionCard
                }
                activityCard
            }
            .padding(.horizontal, 16)
        }
        .padding(.top, 8)
        .padding(.bottom, 24)
    }

    private var heroChips: [String] {
        var chips = [client ? "Client Campaign" : "Own Business"]
        if let party = projection?.partyName, !party.isEmpty {
            chips.append(party)
        }
        chips.append("\(OperatingV12Date.monthYear(projection?.startDate) ?? "This month") · \(model.currency)")
        return chips
    }

    private var heroSubtitle: String {
        let mkt = projection?.marketing
        var parts: [String] = []
        if let pacing = mkt?.pacing, !pacing.isEmpty { parts.append(pacing) }
        if let cpl = mkt?.cpl, !cpl.isEmpty { parts.append("CPL \(cpl)") }
        if parts.isEmpty {
            return "Plan. Launch. Measure."
        }
        return parts.joined(separator: " · ")
    }

    private var periodLabel: String {
        OperatingV12Date.monthYear(projection?.startDate) ?? "This month"
    }

    private func widget(_ code: String) -> OperatingPulseMetricGroupPayload? {
        model.pulseGroup(code: code)
    }

    // MARK: AN-P01 · Needs Attention

    private var attentionCard: some View {
        let group = widget("ATTENTION")
        let items = Array((projection?.attention ?? []).prefix(5))
        let alerts = (projection?.attention ?? []).filter {
            ($0.severity ?? "").uppercased() == "HIGH" || ($0.severity ?? "").uppercased() == "CRITICAL"
        }.count
        let overdue = (projection?.attention ?? []).filter {
            ($0.severity ?? "").uppercased() == "CRITICAL"
        }.count

        return OperatingV12Card(theme: theme, fill: theme.pulseCardFill) {
            OperatingV12WidgetHeader(
                title: "Needs Attention",
                subtitle: "Severity then due · spend and money are separate axes",
                viewAllColor: theme.softLink,
                onViewAll: { onViewAll("ATTENTION") }
            )
            HStack(spacing: 8) {
                OperatingV12Tile(
                    label: "Alerts",
                    value: "\(alerts)",
                    tone: OperatingV12.red
                )
                OperatingV12Tile(
                    label: "Overdue",
                    value: "\(overdue)",
                    tone: OperatingV12.amber
                )
            }
            if items.isEmpty {
                OperatingV12QuietRow(title: "Nothing is waiting.")
            } else {
                VStack(spacing: 6) {
                    ForEach(items) { item in
                        VStack(alignment: .leading, spacing: 6) {
                            OperatingV12Row(
                                title: item.title,
                                subtitle: attentionSubtitle(item),
                                chip: attentionChip(item),
                                bar: OperatingV12.bar(for: attentionChip(item)),
                                accent: theme.accent
                            )
                            attentionActions(item)
                        }
                    }
                }
            }
            OperatingV12Why(
                text: group?.why
                    ?? "Approvals, overdue payments, blockers, and behind-pace alerts — never conflate spend with settlement.",
                tone: .white
            )
        }
    }

    @ViewBuilder
    private func attentionActions(_ item: OperatingAttentionPayload) -> some View {
        if canDecide(item) {
            HStack(spacing: 12) {
                Button("Approve") {
                    Task { _ = await model.decide(eventId: item.id, decision: "APPROVED") }
                }
                .font(.plusJakarta(size: 13, weight: .semibold))
                .foregroundStyle(OperatingV12.green)
                Button("Reject") {
                    Task { _ = await model.decide(eventId: item.id, decision: "REJECTED") }
                }
                .font(.plusJakarta(size: 13, weight: .semibold))
                .foregroundStyle(OperatingV12.red)
            }
        } else if item.id.hasPrefix("open-") {
            let paymentId = String(item.id.dropFirst("open-".count))
            if let payment = projection?.paymentReadiness?.first(where: { $0.id == paymentId }) {
                Button(payment.relevance.uppercased() == "RECEIVABLE" ? "Collect" : "Settle") {
                    onCapture(payment.relevance.uppercased() == "RECEIVABLE" ? "COLLECTION" : "PAYMENT")
                }
                .font(.plusJakarta(size: 13, weight: .semibold))
                .foregroundStyle(theme.accent)
            }
        } else if let cta = item.cta, !cta.isEmpty, cta != "approval" {
            Button(cta.replacingOccurrences(of: "_", with: " ").capitalized) {
                onCapture(cta.uppercased())
            }
            .font(.plusJakarta(size: 13, weight: .semibold))
            .foregroundStyle(theme.accent)
        }
    }

    private func canDecide(_ item: OperatingAttentionPayload) -> Bool {
        let role = projection?.role ?? ""
        let allowed = role == "OWNER_ADMIN" || role == "APPROVER"
        let kind = item.kind.uppercased()
        return allowed && (kind == "APPROVAL" || kind == "ATT-03" || item.cta == "approval")
    }

    private func attentionSubtitle(_ item: OperatingAttentionPayload) -> String? {
        var parts: [String] = []
        if let name = item.counterpartyName, !name.isEmpty { parts.append(name) }
        if let due = OperatingV12Date.short(item.dueAt) { parts.append("due \(due)") }
        if let amount = item.amount, !amount.isEmpty {
            parts.append(OperatingV12Money.amount(amount, currency: model.currency))
        }
        return parts.isEmpty ? nil : parts.joined(separator: " · ")
    }

    private func attentionChip(_ item: OperatingAttentionPayload) -> String? {
        switch (item.severity ?? "").uppercased() {
        case "CRITICAL": return "Overdue"
        case "HIGH": return "Alert"
        case "MEDIUM": return "Unavailable"
        default: return nil
        }
    }

    // MARK: AN-P02 · Target & Budget

    private var targetCard: some View {
        let target = projection?.target
        let budget = projection?.budget
        let mkt = projection?.marketing
        let configured = target?.configured ?? false
        let measured = (projection?.activity ?? []).contains {
            $0.eventType == "RESULT" || $0.eventType == "PROGRESS"
        }
        let targetValue = OperatingV12Money.amount(target?.value, currency: model.currency)
        let achieved = measured
            ? OperatingV12Money.amount(target?.progress, currency: model.currency)
            : "N/A"
        let targetNumber = OperatingV12Money.number(target?.value) ?? 0
        let progressNumber = measured ? (OperatingV12Money.number(target?.progress) ?? 0) : 0
        let fraction = targetNumber > 0 ? progressNumber / targetNumber : 0
        let onTrack = targetNumber > 0 && progressNumber >= targetNumber * 0.9
        let spentNumber = OperatingV12Money.number(budget?.spent) ?? 0
        let amountNumber = OperatingV12Money.number(budget?.amount) ?? 0
        let usedPct = amountNumber > 0 ? Int(((spentNumber / amountNumber) * 100).rounded()) : nil

        return OperatingV12Card(theme: theme, fill: theme.pulseCardFill) {
            OperatingV12WidgetHeader(
                title: "Target & Budget",
                subtitle: "Period · \(periodLabel) · as-of effective plan",
                viewAllColor: theme.softLink,
                onViewAll: { onViewAll("TARGET_BUDGET") }
            )
            HStack(spacing: 16) {
                OperatingV12Ring(
                    fraction: fraction,
                    caption: targetNumber > 0 && measured ? "\(Int((fraction * 100).rounded()))%" : "—",
                    note: "of target",
                    accent: theme.accent
                )
                VStack(alignment: .leading, spacing: 6) {
                    Text("RESULTS TOWARD TARGET")
                        .font(.plusJakarta(size: 10, weight: .bold))
                        .tracking(0.6)
                        .foregroundStyle(theme.accent)
                    Text(configured && measured
                         ? "Results \(achieved) of \(targetValue)"
                         : configured ? "No accepted results yet" : "No target set")
                        .font(.plusJakarta(size: 15, weight: .bold))
                        .foregroundStyle(.white)
                        .lineLimit(2)
                        .minimumScaleFactor(0.75)
                    if configured && measured && targetNumber > 0 {
                        Text(onTrack ? "● On track" : "● Behind pace")
                            .font(.plusJakarta(size: 10, weight: .bold))
                            .foregroundStyle(onTrack ? OperatingV12.green : OperatingV12.amber)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 3)
                            .background((onTrack ? OperatingV12.green : OperatingV12.amber).opacity(0.16))
                            .clipShape(Capsule())
                    }
                    if let pacing = mkt?.pacing, !pacing.isEmpty {
                        Text(pacing)
                            .font(.plusJakarta(size: 11))
                            .foregroundStyle(OperatingV12.subtitle)
                    }
                }
                Spacer(minLength: 0)
            }
            .padding(14)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(theme.canvas)
            .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .stroke(theme.accent.opacity(0.22), lineWidth: 1)
            )

            HStack(spacing: 8) {
                OperatingV12Tile(
                    label: "Budget use",
                    value: usedPct.map { "\($0)%" } ?? "—",
                    tone: OperatingV12.cyan
                )
                OperatingV12Tile(
                    label: "Results",
                    value: measured && configured ? "\(achieved)/\(targetValue)" : "—",
                    tone: OperatingV12.title
                )
            }
            HStack(spacing: 8) {
                OperatingV12Tile(
                    label: "Spend",
                    value: OperatingV12Money.amount(budget?.spent, currency: model.currency),
                    tone: OperatingV12.title
                )
                OperatingV12Tile(
                    label: "Budget",
                    value: OperatingV12Money.amount(budget?.amount, currency: model.currency),
                    tone: OperatingV12.title
                )
                OperatingV12Tile(
                    label: "CPL",
                    value: mkt?.cpl ?? "N/A",
                    tone: OperatingV12.subtitle
                )
            }
            if !client {
                HStack(spacing: 8) {
                    if let sales = mkt?.salesAchievement {
                        OperatingV12Tile(label: "Attr. sales", value: "\(sales)%", tone: theme.accent)
                    }
                    if let roas = mkt?.roas {
                        OperatingV12Tile(label: "ROAS", value: "\(roas)x", tone: OperatingV12.green)
                    }
                    if let orders = mkt?.orderAchievement {
                        OperatingV12Tile(label: "Orders", value: "\(orders)%", tone: OperatingV12.title)
                    }
                }
            }
            channelTotals(
                title: "Results by channel",
                rows: (projection?.activity ?? []).filter {
                    $0.eventType == "RESULT" || $0.eventType == "PROGRESS"
                }
            )
            channelTotals(
                title: "Spend by channel",
                rows: (projection?.activity ?? []).filter {
                    $0.eventType == "SPEND" || $0.eventType == "VENDOR_COST"
                }
            )
            resultsMeter(fraction: fraction, measured: measured)
            OperatingV12Why(
                text: widget("TARGET_BUDGET")?.why
                    ?? "Accepted results ÷ effective target; eligible spend ÷ effective budget. Money collected does not count as results.",
                tone: .white
            )
        }
    }

    private func resultsMeter(fraction: Double, measured: Bool) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Text("Results toward target")
                .font(.plusJakarta(size: 11, weight: .semibold))
                .foregroundStyle(OperatingV12.subtitle)
            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    Capsule().fill(Color.white.opacity(0.08))
                    Capsule()
                        .fill(theme.accent)
                        .frame(width: geo.size.width * (measured ? min(1, fraction) : 0))
                }
            }
            .frame(height: 8)
        }
    }

    @ViewBuilder
    private func channelTotals(title: String, rows: [OperatingActivityPayload]) -> some View {
        let grouped = Dictionary(
            grouping: rows.filter {
                let status = ($0.approvalStatus ?? "").uppercased()
                return status != "REJECTED" && status != "PENDING"
            }
        ) { row -> String in
            let name = row.channelName?.trimmingCharacters(in: .whitespaces) ?? ""
            return name.isEmpty ? "Unassigned" : name
        }
        if !grouped.isEmpty {
            VStack(alignment: .leading, spacing: 4) {
                Text(title)
                    .font(.plusJakarta(size: 11, weight: .semibold))
                    .foregroundStyle(OperatingV12.subtitle)
                ForEach(grouped.keys.sorted(), id: \.self) { channel in
                    let total = (grouped[channel] ?? [])
                        .compactMap { OperatingV12Money.number($0.amount) }
                        .reduce(0, +)
                    HStack {
                        Text(channel)
                            .font(.plusJakarta(size: 12))
                            .foregroundStyle(OperatingV12.rowText)
                        Spacer()
                        Text(OperatingV12Money.compact(total, currency: model.currency))
                            .font(.plusJakarta(size: 12, weight: .semibold))
                            .foregroundStyle(OperatingV12.title)
                    }
                }
            }
        }
    }

    // MARK: AN-P03 · Payment Readiness

    private var paymentCard: some View {
        let due = projection?.paymentReadiness ?? []
        let receivable = client
            ? due.filter { ($0.relevance ?? "").uppercased() == "RECEIVABLE" }
            : []
        let payable = due.filter { ($0.relevance ?? "").uppercased() != "RECEIVABLE" }
        let receivableTotal = receivable.compactMap { OperatingV12Money.number($0.amount) }.reduce(0, +)
        let payableTotal = payable.compactMap { OperatingV12Money.number($0.amount) }.reduce(0, +)

        return OperatingV12Card(theme: theme, fill: theme.pulseCardFill) {
            OperatingV12WidgetHeader(
                title: "Payment Readiness",
                subtitle: client
                    ? "Vendor AP · Client AR · dues with due dates"
                    : "Vendor / creator dues · Own Business has no client AR",
                viewAllColor: theme.softLink,
                onViewAll: { onViewAll("PAYMENT_READINESS") }
            )
            if client {
                HStack(spacing: 8) {
                    OperatingV12Tile(
                        label: "Vendor dues (AP)",
                        value: OperatingV12Money.compact(payableTotal, currency: model.currency),
                        tone: OperatingV12.red
                    )
                    OperatingV12Tile(
                        label: "Client AR due",
                        value: OperatingV12Money.compact(receivableTotal, currency: model.currency),
                        tone: theme.accent
                    )
                }
            } else {
                HStack(spacing: 8) {
                    OperatingV12Tile(
                        label: "Vendor dues",
                        value: OperatingV12Money.compact(payableTotal, currency: model.currency),
                        tone: OperatingV12.red
                    )
                    OperatingV12Tile(
                        label: "Ready to pay",
                        value: "\(payable.filter { ($0.status ?? "").uppercased().contains("APPROV") || ($0.status ?? "").uppercased() == "READY" }.count)",
                        tone: OperatingV12.green
                    )
                }
            }
            let preview = Array((client ? receivable + payable : payable).prefix(3))
            if preview.isEmpty {
                OperatingV12QuietRow(title: "No open payments or collections.")
            } else {
                VStack(spacing: 6) {
                    ForEach(preview) { row in
                        OperatingV12Row(
                            title: row.counterpartyName?.isEmpty == false
                                ? (row.counterpartyName ?? row.title)
                                : row.title,
                            subtitle: row.status.replacingOccurrences(of: "_", with: " "),
                            trailing: OperatingV12Money.amount(row.amount, currency: model.currency),
                            chip: (row.dueBucket ?? "").uppercased() == "OVERDUE" ? "Overdue" : nil,
                            bar: OperatingV12.bar(
                                for: (row.dueBucket ?? "").uppercased() == "OVERDUE" ? "Overdue" : "Open"
                            ),
                            accent: theme.accent
                        )
                    }
                }
            }
            OperatingV12Why(
                text: widget("PAYMENT_READINESS")?.why
                    ?? (client
                        ? "Open AP/AR = charge − allocations. Invoice/Collection are Client-only."
                        : "Own Business settles vendor spend only — no client invoice or collection."),
                tone: .white
            )
        }
    }

    // MARK: AN-P04 · Finance

    private var financeCard: some View {
        let finance = projection?.finance
        let measured = (projection?.activity ?? []).contains {
            $0.eventType == "RESULT" || $0.eventType == "PROGRESS"
        }
        let mkt = projection?.marketing

        return OperatingV12Card(theme: theme, fill: theme.pulseCardFill) {
            OperatingV12WidgetHeader(
                title: "Finance",
                subtitle: client
                    ? "Invoices · collections · eligible cost"
                    : "Attributed sales · spend · ROAS",
                viewAllColor: theme.softLink,
                onViewAll: { onViewAll("FINANCE") }
            )
            if client {
                HStack(spacing: 8) {
                    OperatingV12Tile(
                        label: "Invoiced",
                        value: OperatingV12Money.amount(finance?.sales, currency: model.currency),
                        tone: OperatingV12.title
                    )
                    OperatingV12Tile(
                        label: "Collected",
                        value: OperatingV12Money.amount(finance?.received, currency: model.currency),
                        tone: OperatingV12.green
                    )
                    OperatingV12Tile(
                        label: "AR",
                        value: OperatingV12Money.amount(finance?.outstanding, currency: model.currency),
                        tone: OperatingV12.red
                    )
                }
            } else {
                HStack(spacing: 8) {
                    OperatingV12Tile(
                        label: "Accepted results",
                        value: measured
                            ? OperatingV12Money.amount(projection?.target?.progress, currency: model.currency)
                            : "N/A",
                        tone: theme.accent
                    )
                    if let sales = mkt?.salesAchievement {
                        OperatingV12Tile(label: "Attr. sales", value: "\(sales)%", tone: OperatingV12.green)
                    }
                    if let roas = mkt?.roas {
                        OperatingV12Tile(label: "ROAS", value: "\(roas)x", tone: OperatingV12.cyan)
                    }
                }
            }
            HStack(spacing: 8) {
                OperatingV12Tile(
                    label: "Eligible cost",
                    value: OperatingV12Money.amount(finance?.eligibleCost, currency: model.currency),
                    tone: OperatingV12.title
                )
                OperatingV12Tile(
                    label: "Cash paid",
                    value: OperatingV12Money.amount(finance?.cashPaid, currency: model.currency),
                    tone: OperatingV12.title
                )
                OperatingV12Tile(
                    label: "Still to pay",
                    value: OperatingV12Money.amount(finance?.payable, currency: model.currency),
                    tone: OperatingV12.amber
                )
            }
            OperatingV12Why(
                text: widget("FINANCE")?.why
                    ?? (client
                        ? "Invoice/Collection are Client-only. Sale ≠ receipt — cash only from collections."
                        : "Own Business has no client invoices. Attributed sales and ROAS come from accepted results."),
                tone: .white
            )
        }
    }

    // MARK: AN-P05 · Activity

    private var activityCard: some View {
        let group = widget("ACTIVITY")
        let rows = Array((projection?.activity ?? []).prefix(4))
        let open = (projection?.activity ?? []).filter {
            let b = ($0.badge ?? $0.status ?? "").uppercased()
            return ["OPEN", "PART", "OVERDUE", "DUE", "IN_PROGRESS", "PLANNED"].contains(b)
                || ($0.approvalStatus ?? "").uppercased() == "PENDING"
        }.count
        let done = (projection?.activity ?? []).filter {
            let b = ($0.badge ?? $0.status ?? "").uppercased()
            return ["DONE", "SETTLED", "DELIVERED"].contains(b)
                || ($0.approvalStatus ?? "").uppercased() == "APPROVED"
        }.count

        return OperatingV12Card(theme: theme, fill: theme.pulseCardFill) {
            OperatingV12WidgetHeader(
                title: "Activity",
                subtitle: "Today · posted events only",
                viewAllColor: theme.softLink,
                onViewAll: { onViewAll("ACTIVITY") }
            )
            HStack(spacing: 8) {
                OperatingV12Tile(label: "Open", value: "\(open)", tone: theme.accent)
                OperatingV12Tile(label: "Done", value: "\(done)", tone: OperatingV12.green)
                OperatingV12Tile(
                    label: "Events",
                    value: "\(projection?.activity?.count ?? 0)",
                    tone: OperatingV12.title
                )
            }
            if rows.isEmpty {
                VStack(alignment: .leading, spacing: 8) {
                    Text("Nothing recorded yet.")
                        .font(.plusJakarta(size: 12))
                        .foregroundStyle(OperatingV12.subtitle)
                    Button("Use quick add") { onCapture("SPEND") }
                        .font(.plusJakarta(size: 13, weight: .semibold))
                        .foregroundStyle(theme.accent)
                }
            } else {
                VStack(alignment: .leading, spacing: 6) {
                    ForEach(rows) { row in
                        Button {
                            onOpenRow?(row)
                        } label: {
                            OperatingV12Row(
                                title: row.title.isEmpty ? row.eventType.capitalized : row.title,
                                subtitle: activitySubtitle(row),
                                trailing: model.financeVisible
                                    ? (row.amount.flatMap { $0.isEmpty ? nil : OperatingV12Money.amount($0, currency: model.currency) })
                                    : nil,
                                chip: row.badge ?? row.status,
                                bar: OperatingV12.bar(for: row.badge ?? row.status),
                                accent: theme.accent
                            )
                        }
                        .buttonStyle(.plain)
                        .disabled(onOpenRow == nil)
                    }
                }
            }
            OperatingV12Why(
                text: group?.why
                    ?? "Chronological posted timeline; drafts/reversed distinguished; incremental progress labeled.",
                tone: .white
            )
        }
    }

    private func activitySubtitle(_ row: OperatingActivityPayload) -> String? {
        var parts: [String] = [row.eventType.capitalized]
        if let channel = row.channelName, !channel.isEmpty { parts.append(channel) }
        if let name = row.counterpartyName, !name.isEmpty { parts.append(name) }
        return parts.joined(separator: " · ")
    }

    private var restrictedPulse: some View {
        OperatingV12Card(theme: theme, fill: theme.pulseCardFill) {
            Text(model.title)
                .font(.plusJakarta(size: 18, weight: .semibold))
                .foregroundStyle(OperatingV12.title)
            if let party = projection?.partyName, !party.isEmpty {
                Text(party)
                    .font(.plusJakarta(size: 13))
                    .foregroundStyle(OperatingV12.subtitle)
            }
            let channels = projection?.life?.channels ?? []
            if !channels.isEmpty {
                Text("Channels · \(channels.joined(separator: " · "))")
                    .font(.plusJakarta(size: 13))
                    .foregroundStyle(OperatingV12.subtitle)
            }
            Text("Financial totals stay hidden in this view.")
                .font(.plusJakarta(size: 12))
                .foregroundStyle(OperatingV12.subtitle)
        }
    }
}

// MARK: - Moments (`MKT-MOMENTS-v12`)

private struct MarketingMomentsScreen: View {
    @ObservedObject var model: BusinessOperatingModel
    let theme: OperatingTheme
    let client: Bool
    var onCapture: (String) -> Void
    var onViewAll: (String) -> Void
    var onOpenRow: ((OperatingActivityPayload) -> Void)? = nil

    private var target: OperatingTargetPayload? { model.projection?.target }
    private var budget: OperatingMoneyPayload? { model.projection?.budget }

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            MarketingContextChips(theme: theme, client: client, model: model)

            OperatingV12Hero(
                theme: theme,
                eyebrow: "ACTIVE MOMENT",
                title: model.title,
                chips: [
                    .init(label: client ? "Client Campaign" : "Own Business", tone: theme.accent),
                    .init(
                        label: model.projection?.retired == true ? "Campaign closed" : "Campaign open",
                        tone: OperatingV12.green,
                        dot: true
                    ),
                ],
                kpis: [
                    .init(
                        label: "Results",
                        value: OperatingV12Money.amount(target?.progress, currency: model.currency),
                        tone: OperatingV12.title
                    ),
                    .init(
                        label: "Target",
                        value: OperatingV12Money.amount(target?.value, currency: model.currency),
                        tone: theme.accent
                    ),
                    .init(
                        label: "Remaining",
                        value: OperatingV12Money.amount(budget?.remaining, currency: model.currency),
                        tone: OperatingV12.cyan
                    ),
                ]
            )
            .padding(.horizontal, 16)

            resultsMeter
                .padding(.horizontal, 16)

            MarketingV12QuickActions(theme: theme, client: client, onOpen: onCapture)
                .padding(.horizontal, 20)

            VStack(alignment: .leading, spacing: 16) {
                ForEach(model.projection?.boards?.moments ?? []) { section in
                    marketingSectionCard(section)
                }
                if (model.projection?.boards?.moments ?? []).isEmpty {
                    OperatingV12Card(theme: theme, fill: theme.isMarketingOwnCapture ? theme.surface : OperatingV12.surface) {
                        Text("Nothing on the campaign board yet")
                            .font(.plusJakarta(size: 14, weight: .bold))
                            .foregroundStyle(OperatingV12.title)
                        Text("Deliverables, people, channels, and commitments will show here.")
                            .font(.plusJakarta(size: 12))
                            .foregroundStyle(OperatingV12.subtitle)
                    }
                }
            }
            .padding(.horizontal, 16)
        }
        .padding(.top, 8)
        .padding(.bottom, 24)
    }

    private var resultsMeter: some View {
        let targetNumber = OperatingV12Money.number(target?.value) ?? 0
        let progressNumber = OperatingV12Money.number(target?.progress) ?? 0
        let fraction = targetNumber > 0 ? min(1, progressNumber / targetNumber) : 0
        return OperatingV12Card(theme: theme, fill: theme.isMarketingOwnCapture ? theme.surface : OperatingV12.surface) {
            Text("Results toward target")
                .font(.plusJakarta(size: 12, weight: .semibold))
                .foregroundStyle(OperatingV12.subtitle)
            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    Capsule().fill(Color.white.opacity(0.08))
                    Capsule()
                        .fill(theme.accent)
                        .frame(width: geo.size.width * fraction)
                }
            }
            .frame(height: 10)
            HStack {
                Text(OperatingV12Money.amount(target?.progress, currency: model.currency))
                    .font(.plusJakarta(size: 13, weight: .bold))
                    .foregroundStyle(OperatingV12.title)
                Spacer()
                Text("of \(OperatingV12Money.amount(target?.value, currency: model.currency))")
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(OperatingV12.subtitle)
            }
        }
    }

    private func marketingSectionCard(_ section: OperatingBoardSectionPayload) -> some View {
        let rows = Array((section.rows ?? []).prefix(3))
        return OperatingV12Card(theme: theme, fill: theme.isMarketingOwnCapture ? theme.surface : OperatingV12.surface) {
            OperatingV12WidgetHeader(
                title: section.title,
                subtitle: section.detail.isEmpty
                    ? (section.coverage ?? "Campaign board")
                    : section.detail,
                viewAllColor: theme.softLink,
                onViewAll: { onViewAll(section.code) }
            )
            if let metrics = section.metrics, !metrics.isEmpty {
                OperatingV12TileGrid(
                    theme: theme,
                    metrics: metrics,
                    columns: min(3, metrics.count),
                    currency: model.currency
                )
            }
            if rows.isEmpty {
                Text("Not recorded yet.")
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(OperatingV12.subtitle)
            } else {
                VStack(spacing: 6) {
                    ForEach(rows) { row in
                        Button { onOpenRow?(row) } label: {
                            OperatingV12Row(
                                title: row.title,
                                subtitle: row.subtitle ?? row.channelName,
                                trailing: rowTrailing(row),
                                chip: row.badge,
                                bar: OperatingV12.bar(for: row.badge),
                                accent: theme.accent
                            )
                        }
                        .buttonStyle(.plain)
                        .disabled(onOpenRow == nil)
                    }
                }
            }
            OperatingV12Why(text: section.why)
        }
    }

    private func rowTrailing(_ row: OperatingActivityPayload) -> String? {
        if model.financeVisible, let amount = row.amount, !amount.isEmpty {
            return OperatingV12Money.amount(amount, currency: model.currency)
        }
        return nil
    }
}

// MARK: - Life (`MKT-LIFE-v12`)

private struct MarketingLifeScreen: View {
    @ObservedObject var model: BusinessOperatingModel
    let theme: OperatingTheme
    let client: Bool
    var onCapture: (String) -> Void
    var onViewAll: (String) -> Void
    var onOpenRow: ((OperatingActivityPayload) -> Void)? = nil

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            MarketingContextChips(theme: theme, client: client, model: model)

            MarketingV12QuickActions(theme: theme, client: client, onOpen: onCapture)
                .padding(.horizontal, 20)

            OperatingV12Hero(
                theme: theme,
                eyebrow: "TODAY",
                title: "\(model.title) · today",
                titleSize: 22,
                chips: [
                    .init(label: "Today · \(OperatingV12Date.today())", tone: theme.accent),
                    .init(
                        label: model.projection?.retired == true ? "Campaign closed" : "Campaign open",
                        tone: OperatingV12.green,
                        dot: true
                    ),
                ],
                kpis: lifeKpis
            )
            .padding(.horizontal, 16)

            VStack(alignment: .leading, spacing: 16) {
                let sections = model.projection?.boards?.life ?? []
                if sections.isEmpty {
                    OperatingV12Card(theme: theme, fill: theme.isMarketingOwnCapture ? theme.surface : OperatingV12.surface) {
                        Text("Nothing recorded yet")
                            .font(.plusJakarta(size: 14, weight: .bold))
                            .foregroundStyle(OperatingV12.title)
                        Text("Board updates and the live position will show here.")
                            .font(.plusJakarta(size: 12))
                            .foregroundStyle(OperatingV12.subtitle)
                    }
                } else {
                    ForEach(sections) { section in
                        lifeSectionCard(section)
                    }
                }
            }
            .padding(.horizontal, 16)
        }
        .padding(.top, 8)
        .padding(.bottom, 24)
    }

    private var lifeKpis: [OperatingV12KpiSpec] {
        let life = model.projection?.life
        return [
            .init(label: "Open", value: "\(life?.openCommitments ?? 0)", tone: theme.accent),
            .init(
                label: "Progress",
                value: life?.resultProgress ?? "—",
                tone: OperatingV12.green
            ),
            .init(
                label: "Channels",
                value: "\(life?.channels.count ?? 0)",
                tone: OperatingV12.title
            ),
        ]
    }

    private func lifeSectionCard(_ section: OperatingBoardSectionPayload) -> some View {
        let rows = Array((section.rows ?? []).prefix(3))
        return OperatingV12Card(theme: theme, fill: theme.isMarketingOwnCapture ? theme.surface : OperatingV12.surface) {
            OperatingV12WidgetHeader(
                title: section.title,
                subtitle: section.coverage ?? section.detail,
                viewAllColor: theme.softLink,
                onViewAll: { onViewAll(section.code) }
            )
            if let metrics = section.metrics, !metrics.isEmpty {
                OperatingV12TileGrid(
                    theme: theme,
                    metrics: metrics,
                    columns: min(3, metrics.count),
                    currency: model.currency
                )
            }
            if rows.isEmpty {
                Text("Not recorded yet.")
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(OperatingV12.subtitle)
            } else {
                VStack(spacing: 6) {
                    ForEach(rows) { row in
                        Button { onOpenRow?(row) } label: {
                            OperatingV12Row(
                                title: row.title,
                                subtitle: row.subtitle ?? row.channelName,
                                trailing: model.financeVisible && row.amount?.isEmpty == false
                                    ? OperatingV12Money.amount(row.amount, currency: model.currency)
                                    : nil,
                                chip: row.badge,
                                bar: OperatingV12.bar(for: row.badge),
                                accent: theme.accent
                            )
                        }
                        .buttonStyle(.plain)
                        .disabled(onOpenRow == nil)
                    }
                }
            }
            OperatingV12Why(text: section.why)
        }
    }
}

// MARK: - Memory (`MKT-MEMORY-v12`)

private struct MarketingMemoryScreen: View {
    @ObservedObject var model: BusinessOperatingModel
    let theme: OperatingTheme
    let client: Bool
    var onCapture: (String) -> Void
    var onStory: () -> Void
    var onViewAll: (String) -> Void
    var onOpenRow: ((OperatingActivityPayload) -> Void)? = nil

    @State private var period = OperatingSheetChrome.periodFilters[0]

    private var periodLabel: String {
        OperatingV12Date.monthYear(model.projection?.startDate) ?? "This month"
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            MarketingContextChips(theme: theme, client: client, model: model)

            MarketingV12QuickActions(theme: theme, client: client, onOpen: onCapture)
                .padding(.horizontal, 20)

            OperatingV12Hero(
                theme: theme,
                eyebrow: "MEMORY · CLOSED PERIOD",
                title: "\(model.title) · memory",
                titleSize: 22,
                chips: [
                    .init(label: periodLabel, tone: theme.accent),
                    .init(label: "vs last period", tone: OperatingV12.subtitle),
                ],
                kpis: [
                    .init(
                        label: "Results",
                        value: OperatingV12Money.amount(
                            model.projection?.target?.progress,
                            currency: model.currency
                        ),
                        tone: .white
                    ),
                    .init(
                        label: "Spend",
                        value: OperatingV12Money.amount(
                            model.projection?.budget?.spent,
                            currency: model.currency
                        ),
                        tone: OperatingV12.cyan
                    ),
                    .init(
                        label: client ? "Collected" : "ROAS",
                        value: client
                            ? OperatingV12Money.amount(
                                model.projection?.finance?.received,
                                currency: model.currency
                            )
                            : (model.projection?.marketing?.roas.map { "\($0)x" } ?? "—"),
                        tone: theme.accent
                    ),
                ]
            )
            .padding(.horizontal, 16)

            periodPills
                .padding(.horizontal, 16)

            storyCTA
                .padding(.horizontal, 16)

            VStack(alignment: .leading, spacing: 16) {
                ForEach(model.projection?.boards?.memory ?? []) { section in
                    memorySectionCard(section)
                }
                if (model.projection?.boards?.memory ?? []).isEmpty {
                    OperatingV12Card(theme: theme, fill: theme.isMarketingOwnCapture ? theme.surface : OperatingV12.surface) {
                        Text("No memory recorded yet")
                            .font(.plusJakarta(size: 14, weight: .bold))
                            .foregroundStyle(OperatingV12.title)
                        Text("Months, money, people, and patterns will show here.")
                            .font(.plusJakarta(size: 12))
                            .foregroundStyle(OperatingV12.subtitle)
                    }
                }
            }
            .padding(.horizontal, 16)
        }
        .padding(.top, 8)
        .padding(.bottom, 24)
    }

    private var periodPills: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(OperatingSheetChrome.periodFilters, id: \.self) { option in
                    let on = option == period
                    Button {
                        period = option
                        Task { await model.applySheetPeriod(option) }
                    } label: {
                        Text(option)
                            .font(.plusJakarta(size: 12, weight: .semibold))
                            .foregroundStyle(on ? theme.onAccent : OperatingV12.subtitle)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 7)
                            .background(on ? theme.accent : OperatingV12.inset)
                            .clipShape(Capsule())
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }

    private var storyCTA: some View {
        Button(action: onStory) {
            VStack(alignment: .leading, spacing: 2) {
                Image(systemName: "sparkles")
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(OperatingV12.deepInk)
                Text("Generate Business Story")
                    .font(.plusJakarta(size: 15, weight: .heavy))
                    .foregroundStyle(OperatingV12.deepInk)
                Text("Eight pages from recorded activity · \(periodLabel) preselected")
                    .font(.plusJakarta(size: 11, weight: .medium))
                    .foregroundStyle(OperatingV12.deepInk.opacity(0.8))
            }
            .padding(.horizontal, 18)
            .padding(.vertical, 14)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(theme.accent)
            .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
            .shadow(color: theme.accent.opacity(0.35), radius: 12, x: 0, y: 8)
        }
        .buttonStyle(.plain)
    }

    private func memorySectionCard(_ section: OperatingBoardSectionPayload) -> some View {
        let rows = Array((section.rows ?? []).prefix(3))
        return OperatingV12Card(theme: theme, fill: theme.isMarketingOwnCapture ? theme.surface : OperatingV12.surface) {
            OperatingV12WidgetHeader(
                title: section.title,
                subtitle: section.detail,
                viewAllColor: theme.softLink,
                onViewAll: { onViewAll(section.code) }
            )
            if let metrics = section.metrics, !metrics.isEmpty {
                OperatingV12TileGrid(
                    theme: theme,
                    metrics: metrics,
                    columns: metrics.count == 2 ? 2 : 3,
                    currency: model.currency
                )
            }
            if rows.isEmpty {
                Text("Not recorded yet.")
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(OperatingV12.subtitle)
            } else {
                VStack(spacing: 6) {
                    ForEach(rows) { row in
                        Button { onOpenRow?(row) } label: {
                            OperatingV12Row(
                                title: row.title,
                                subtitle: row.subtitle,
                                trailing: model.financeVisible && row.amount?.isEmpty == false
                                    ? OperatingV12Money.amount(row.amount, currency: model.currency)
                                    : nil,
                                chip: row.badge,
                                bar: OperatingV12.bar(for: row.badge),
                                accent: theme.accent
                            )
                        }
                        .buttonStyle(.plain)
                        .disabled(onOpenRow == nil)
                    }
                }
            }
            OperatingV12Why(text: section.why)
        }
    }
}

// MARK: - Hub (Create tab) — search + support + why

private struct MarketingHubScreen: View {
    @ObservedObject var model: BusinessOperatingModel
    let theme: OperatingTheme
    let client: Bool
    var onCapture: (String) -> Void
    var onOpenMasters: (OperatingMasterTab) -> Void
    var onOpenStory: () -> Void

    @State private var query = ""

    private var allowed: Set<String> {
        Set((model.projection?.quickAdds ?? []).map { $0.code.uppercased() })
    }

    private var core: [MarketingCaptureMode] {
        // v5.1 AT-01: 10 client / 8 own — QA-09 is a single Team Update / Issue tile.
        [
            .activity, .progress, .spend, .result, .payment,
            .invoice, .collection, .revision, .approval, .update,
        ].filter { mode in
            if mode.clientOnly && !client { return false }
            guard allowed.isEmpty || allowed.contains(mode.rawValue) else { return false }
            if query.isEmpty { return true }
            return mode.title(client: client).localizedCaseInsensitiveContains(query)
        }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 20) {
            MarketingContextChips(theme: theme, client: client, model: model)

            VStack(alignment: .leading, spacing: 8) {
                Text("QUICK ADDS")
                    .font(.plusJakarta(size: 11, weight: .bold))
                    .tracking(1.2)
                    .foregroundStyle(OperatingV12.cyan)
                Text(model.title)
                    .font(.plusJakarta(size: 28, weight: .bold))
                    .foregroundStyle(OperatingV12.title)
                Text(model.vertical.motionLine)
                    .font(.plusJakarta(size: 14, weight: .bold))
                    .foregroundStyle(theme.accent)
            }
            .padding(.horizontal, 16)

            searchBar
                .padding(.horizontal, 16)

            Text("Core actions · \(core.count)")
                .font(.plusJakarta(size: 12))
                .foregroundStyle(OperatingV12.subtitle)
                .padding(.horizontal, 16)

            LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible()), GridItem(.flexible())], spacing: 8) {
                ForEach(core) { mode in
                    OperatingV12ActionTile(
                        theme: theme,
                        title: mode.title(client: client),
                        subtitle: mode.rawValue.capitalized,
                        glyph: mode.glyph
                    ) {
                        onCapture(mode.rawValue)
                    }
                }
            }
            .padding(.horizontal, 16)

            Text("Support")
                .font(.plusJakarta(size: 12))
                .foregroundStyle(OperatingV12.subtitle)
                .padding(.horizontal, 16)

            if model.vertical == .ownBusinessCampaign {
                LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 8) {
                    supportTile("Vendors & creators", "person.2") { onOpenMasters(.principals) }
                    supportTile("Services & rate card", "tag") { onOpenMasters(.priceLists) }
                    supportTile("Channels & teams", "antenna.radiowaves.left.and.right") { onOpenMasters(.locations) }
                    supportTile("Invite & QR", "qrcode") { onOpenMasters(.invite) }
                    supportTile("Story", "book") { onOpenStory() }
                }
                .padding(.horizontal, 16)
            } else {
                HStack(spacing: 10) {
                    supportTile("Network", "person.2") { onOpenMasters(.principals) }
                    supportTile("Invite & QR", "qrcode") { onOpenMasters(.invite) }
                    supportTile("Story", "book") { onOpenStory() }
                }
                .padding(.horizontal, 16)
            }

            Text("Utilities · \(utilities.count)")
                .font(.plusJakarta(size: 12))
                .foregroundStyle(OperatingV12.subtitle)
                .padding(.horizontal, 16)

            LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible()), GridItem(.flexible())], spacing: 8) {
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
            .padding(.horizontal, 16)

            whyCard
                .padding(.horizontal, 16)
        }
        .padding(.top, 8)
        .padding(.bottom, 24)
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

    private var searchBar: some View {
        HStack(spacing: 8) {
            Image(systemName: "magnifyingglass")
                .foregroundStyle(Color(hex: "#64748B"))
            TextField(
                "",
                text: $query,
                prompt: Text("Search actions…").foregroundStyle(Color(hex: "#64748B"))
            )
            .font(.plusJakarta(size: 14, weight: .medium))
            .foregroundStyle(OperatingV12.title)
            .textInputAutocapitalization(.never)
            .autocorrectionDisabled()
        }
        .padding(.horizontal, 16)
        .frame(height: 48)
        .background(Color.white.opacity(0.04))
        .overlay(
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .strokeBorder(Color.white.opacity(0.1), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
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
            ForEach(whyRules, id: \.self) { rule in
                Text("• \(rule)")
                    .font(.plusJakarta(size: 12, weight: .medium))
                    .foregroundStyle(Color(hex: "#CBD5E1"))
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
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

    private var whyRules: [String] {
        if client {
            return [
                "Invoice / Collection are Client-only",
                "Accepted results count toward target — money collected does not",
                "Spend and vendor costs count once toward budget",
                "Submit ≠ approval ≠ payment",
                "Own Business posts spend and results instead of client billing",
            ]
        }
        return [
            "Own Business has no client invoice or collection",
            "Accepted results and attributed sales drive the plan",
            "Spend and vendor costs count once toward budget",
            "Submit ≠ approval ≠ payment",
            "ROAS uses attributed sales ÷ eligible spend",
        ]
    }
}

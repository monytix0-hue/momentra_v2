import SwiftUI

/// Client / Own campaign populated tabs — Pulse, Moments, Life, Memory, and the Create hub.
///
/// Uses the same operating projection as Trading; copy, payment readiness, and finance rows
/// branch on `CLIENT_CAMPAIGN` vs `OWN_BUSINESS_CAMPAIGN`.
struct MarketingActiveView: View {
    @ObservedObject var model: BusinessOperatingModel
    let destination: BottomDestination
    let refreshToken: UInt64
    var onOpenQuickAdd: () -> Void = {}
    var onOpenStory: () -> Void = {}
    var onOpenMasters: (OperatingMasterTab) -> Void = { _ in }
    var onCapture: ((String) -> Void)? = nil

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
                        onOpenQuickAdd: onOpenQuickAdd
                    )
                case .moments:
                    MarketingMomentsScreen(
                        model: model,
                        theme: theme,
                        onOpenQuickAdd: onOpenQuickAdd
                    )
                case .life:
                    MarketingLifeScreen(model: model, theme: theme)
                case .memory:
                    MarketingMemoryScreen(
                        model: model,
                        theme: theme,
                        onOpenStory: onOpenStory
                    )
                case .create:
                    MarketingHubScreen(
                        model: model,
                        theme: theme,
                        client: client,
                        onCapture: { code in
                            if let onCapture {
                                onCapture(code)
                            } else {
                                onOpenQuickAdd()
                            }
                        },
                        onOpenMasters: onOpenMasters,
                        onOpenStory: onOpenStory
                    )
                }
            }
            .padding(.bottom, 32)
        }
        .background(theme.canvas)
        .task { await model.load() }
        .onChange(of: refreshToken) { _, _ in
            Task { await model.load() }
        }
    }

    @ViewBuilder
    private var phaseNotice: some View {
        switch model.phase {
        case .loading where model.projection == nil:
            ProgressView()
                .tint(theme.accent)
                .frame(maxWidth: .infinity)
                .padding(.top, 40)
        case let .error(message):
            OperatingCard(theme: theme) {
                Text("Could not load campaign")
                    .font(.plusJakarta(size: 14, weight: .bold))
                    .foregroundStyle(theme.ink)
                Text(message)
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(theme.body)
            }
            .padding(.horizontal, 16)
        case .empty:
            OperatingCard(theme: theme) {
                Text("This campaign is closed")
                    .font(.plusJakarta(size: 14, weight: .bold))
                    .foregroundStyle(theme.ink)
                Text("A closed moment is read-only. Start a new one to record again.")
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(theme.body)
            }
            .padding(.horizontal, 16)
        default:
            EmptyView()
        }
    }
}

// MARK: - Pulse

private struct MarketingPulseScreen: View {
    @ObservedObject var model: BusinessOperatingModel
    let theme: OperatingTheme
    let client: Bool
    var onOpenQuickAdd: () -> Void

    private var projection: OperatingProjectionPayload? { model.projection }

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            header(
                "Pulse",
                client
                    ? "Your campaign revenue plan, approved spend and ready money."
                    : "Your campaign plan, approved obligations and ready money.",
                theme: theme
            )

            if model.financeVisible {
                section("Target & Budget", theme: theme) {
                    let measured = (projection?.activity ?? []).contains {
                        $0.eventType == "RESULT" || $0.eventType == "PROGRESS"
                    }
                    metricRow("Accepted results", measured
                        ? money(model.currency, projection?.target?.progress)
                        : "N/A", theme: theme)
                    Text(projection?.target?.configured == false
                         ? "Target not set"
                         : "of \(money(model.currency, projection?.target?.value)) target")
                        .font(.plusJakarta(size: 13))
                        .foregroundStyle(theme.muted)
                    if let pacing = projection?.marketing?.pacing {
                        Text(pacing).font(.plusJakarta(size: 12)).foregroundStyle(theme.muted)
                    }
                    Text("CPL \(projection?.marketing?.cpl ?? "N/A")")
                        .font(.plusJakarta(size: 12))
                        .foregroundStyle(theme.muted)
                    if let orders = projection?.marketing?.orderAchievement {
                        Text("Orders \(orders)%").font(.plusJakarta(size: 12)).foregroundStyle(theme.muted)
                    }
                    if let sales = projection?.marketing?.salesAchievement {
                        Text("Attributed sales \(sales)%").font(.plusJakarta(size: 12)).foregroundStyle(theme.muted)
                    }
                    if let roas = projection?.marketing?.roas {
                        Text("ROAS \(roas)x").font(.plusJakarta(size: 12)).foregroundStyle(theme.muted)
                    }
                    meter("Budget used", spent: projection?.budget?.spent, total: projection?.budget?.amount, theme: theme)
                    metricRow(
                        "Budget remaining",
                        money(model.currency, projection?.budget?.remaining),
                        theme: theme,
                        tone: theme.positive
                    )
                }

                section("Needs Attention", theme: theme) {
                    let items = projection?.attention ?? []
                    if items.isEmpty {
                        Text("Nothing is waiting")
                            .font(.plusJakarta(size: 14))
                            .foregroundStyle(theme.muted)
                    } else {
                        ForEach(items) { item in
                            VStack(alignment: .leading, spacing: 6) {
                                Text(item.kind)
                                    .font(.plusJakarta(size: 11, weight: .semibold))
                                    .foregroundStyle(theme.accent)
                                Text(item.title)
                                    .font(.plusJakarta(size: 15))
                                    .foregroundStyle(theme.ink)
                                if canDecide(item) {
                                    HStack(spacing: 12) {
                                        Button("Approve") {
                                            Task { _ = await model.decide(eventId: item.id, decision: "APPROVED") }
                                        }
                                        .foregroundStyle(theme.positive)
                                        Button("Reject") {
                                            Task { _ = await model.decide(eventId: item.id, decision: "REJECTED") }
                                        }
                                        .foregroundStyle(theme.negative)
                                    }
                                    .font(.plusJakarta(size: 13, weight: .semibold))
                                }
                            }
                            .padding(.vertical, 4)
                        }
                    }
                }

                section("Payment Readiness", theme: theme) {
                    let readiness = projection?.paymentReadiness ?? []
                    let toCollect = client ? readiness.filter { $0.relevance == "RECEIVABLE" } : []
                    let toPay = readiness.filter { $0.relevance != "RECEIVABLE" }
                    if client {
                        readinessGroup("To collect", toCollect)
                    }
                    readinessGroup("To pay", toPay)
                    if readiness.isEmpty {
                        Text("No open payments or collections.")
                            .font(.plusJakarta(size: 14))
                            .foregroundStyle(theme.muted)
                    }
                }

                section("Finance", theme: theme) {
                    let finance = projection?.finance
                    let measured = (projection?.activity ?? []).contains {
                        $0.eventType == "RESULT" || $0.eventType == "PROGRESS"
                    }
                    metricRow(
                        "Accepted results",
                        measured ? money(model.currency, projection?.target?.progress) : "N/A",
                        theme: theme
                    )
                    if client {
                        metricRow("Client invoices", money(model.currency, finance?.sales), theme: theme)
                        metricRow("Received", money(model.currency, finance?.received), theme: theme, tone: theme.positive)
                        metricRow("Still to collect", money(model.currency, finance?.outstanding), theme: theme, tone: theme.warning)
                    }
                    metricRow("Eligible cost", money(model.currency, finance?.eligibleCost), theme: theme)
                    metricRow("Business cash paid", money(model.currency, finance?.cashPaid), theme: theme)
                    metricRow("Still to pay", money(model.currency, finance?.payable), theme: theme, tone: theme.warning)
                }
            } else {
                restrictedPulse
            }

            section("Activity", theme: theme) {
                let recent = Array((projection?.activity ?? []).prefix(5))
                if recent.isEmpty {
                    VStack(alignment: .leading, spacing: 10) {
                        Text("Nothing recorded yet")
                            .font(.plusJakarta(size: 15, weight: .semibold))
                            .foregroundStyle(theme.ink)
                        Text("Campaigns, spend, and results will show here.")
                            .font(.plusJakarta(size: 13))
                            .foregroundStyle(theme.muted)
                        Button("Use quick add", action: onOpenQuickAdd)
                            .font(.plusJakarta(size: 13, weight: .semibold))
                            .foregroundStyle(theme.accent)
                    }
                } else {
                    ForEach(recent) { row in
                        HStack {
                            VStack(alignment: .leading, spacing: 2) {
                                Text(row.title)
                                    .font(.plusJakarta(size: 14, weight: .semibold))
                                    .foregroundStyle(theme.ink)
                                Text(row.eventType)
                                    .font(.plusJakarta(size: 11))
                                    .foregroundStyle(theme.muted)
                            }
                            Spacer()
                            if model.financeVisible, let amount = row.amount, !amount.isEmpty {
                                Text(OperatingMoneyFormat.format(model.currency, amount))
                                    .font(.plusJakarta(size: 13, weight: .semibold))
                                    .foregroundStyle(theme.ink)
                            }
                        }
                        .padding(.vertical, 4)
                    }
                }
            }
        }
        .padding(.horizontal, 16)
    }

    private var restrictedPulse: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("The campaign today, with the work you're allowed to see.")
                .font(.plusJakarta(size: 14))
                .foregroundStyle(theme.body)
            card(theme: theme) {
                Text(model.title)
                    .font(.plusJakarta(size: 18, weight: .semibold))
                    .foregroundStyle(theme.ink)
                if let party = projection?.partyName, !party.isEmpty {
                    Text(party).font(.plusJakarta(size: 13)).foregroundStyle(theme.muted)
                }
                Text("Financial totals stay hidden in this view.")
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(theme.muted)
            }
        }
    }

    private func canDecide(_ item: OperatingAttentionPayload) -> Bool {
        let role = projection?.role ?? ""
        let allowed = role == "OWNER_ADMIN" || role == "APPROVER"
        let kind = item.kind.uppercased()
        return allowed && (kind == "APPROVAL" || kind == "ATT-03" || item.cta == "approval")
    }

    private func readinessGroup(_ title: String, _ rows: [OperatingPaymentPayload]) -> some View {
        Group {
            if !rows.isEmpty {
                Text(title)
                    .font(.plusJakarta(size: 12, weight: .semibold))
                    .foregroundStyle(theme.muted)
                ForEach(rows) { row in
                    HStack {
                        Text(row.counterpartyName?.isEmpty == false ? (row.counterpartyName ?? row.title) : row.title)
                            .font(.plusJakarta(size: 14))
                            .foregroundStyle(theme.ink)
                        Spacer()
                        Text(OperatingMoneyFormat.format(model.currency, row.amount))
                            .font(.plusJakarta(size: 13, weight: .semibold))
                            .foregroundStyle(theme.ink)
                    }
                }
            }
        }
    }
}

// MARK: - Moments / Life / Memory

private struct MarketingMomentsScreen: View {
    @ObservedObject var model: BusinessOperatingModel
    let theme: OperatingTheme
    var onOpenQuickAdd: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            header("Moments", "Deliverables, results, and the campaign this month.", theme: theme)
            card(theme: theme) {
                Text(periodLabel(model.projection?.startDate))
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(theme.accent)
                Text("This month, in motion")
                    .font(.plusJakarta(size: 22, weight: .semibold))
                    .foregroundStyle(theme.ink)
                HStack(spacing: 8) {
                    metricTile("Results", money(model.currency, model.projection?.target?.progress))
                    metricTile("Target", money(model.currency, model.projection?.target?.value))
                }
                HStack(spacing: 8) {
                    metricTile("Spent", money(model.currency, model.projection?.budget?.spent))
                    metricTile("Remaining", money(model.currency, model.projection?.budget?.remaining))
                }
            }
            boardSections(model.projection?.boards?.moments ?? [], theme: theme, model: model)
            card(theme: theme) {
                Text("Create the next campaign moment")
                    .font(.plusJakarta(size: 16, weight: .semibold))
                    .foregroundStyle(theme.ink)
                Button(action: onOpenQuickAdd) {
                    Text("Open Quick Add")
                        .font(.plusJakarta(size: 14, weight: .semibold))
                        .foregroundStyle(theme.onAccent)
                        .padding(.horizontal, 16)
                        .padding(.vertical, 12)
                        .background(theme.accent)
                        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                }
                .buttonStyle(.plain)
            }
        }
        .padding(.horizontal, 16)
    }

    private func metricTile(_ label: String, _ value: String) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label).font(.plusJakarta(size: 11)).foregroundStyle(theme.muted)
            Text(value).font(.plusJakarta(size: 16, weight: .semibold)).foregroundStyle(theme.ink)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(12)
        .background(theme.inset)
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
    }
}

private struct MarketingLifeScreen: View {
    @ObservedObject var model: BusinessOperatingModel
    let theme: OperatingTheme

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            header("Life", "How the campaign is moving today.", theme: theme)
            let sections = model.projection?.boards?.life ?? []
            if sections.isEmpty {
                card(theme: theme) {
                    Text("Nothing recorded yet")
                        .font(.plusJakarta(size: 15, weight: .semibold))
                        .foregroundStyle(theme.ink)
                    Text("Board updates and the live position will show here.")
                        .font(.plusJakarta(size: 13))
                        .foregroundStyle(theme.muted)
                }
            } else {
                boardSections(sections, theme: theme, model: model)
            }
        }
        .padding(.horizontal, 16)
    }
}

private struct MarketingMemoryScreen: View {
    @ObservedObject var model: BusinessOperatingModel
    let theme: OperatingTheme
    var onOpenStory: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            header("Memory", "What this campaign keeps from the month.", theme: theme)
            Button(action: onOpenStory) {
                Text("Preview story")
                    .font(.plusJakarta(size: 14, weight: .semibold))
                    .foregroundStyle(theme.onAccent)
                    .padding(.horizontal, 16)
                    .padding(.vertical, 12)
                    .background(theme.accent)
                    .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
            }
            .buttonStyle(.plain)
            boardSections(model.projection?.boards?.memory ?? [], theme: theme, model: model)
        }
        .padding(.horizontal, 16)
    }
}

// MARK: - Hub (Create tab)

private struct MarketingHubScreen: View {
    @ObservedObject var model: BusinessOperatingModel
    let theme: OperatingTheme
    let client: Bool
    var onCapture: (String) -> Void
    var onOpenMasters: (OperatingMasterTab) -> Void
    var onOpenStory: () -> Void

    private var allowed: Set<String> {
        Set((model.projection?.quickAdds ?? []).map { $0.code.uppercased() })
    }

    private var core: [MarketingCaptureMode] {
        [
            .activity, .progress, .spend, .result, .payment,
            .invoice, .collection, .revision, .approval, .update, .issue,
        ].filter { mode in
            if mode.clientOnly && !client { return false }
            return allowed.isEmpty || allowed.contains(mode.rawValue)
        }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 20) {
            VStack(alignment: .leading, spacing: 6) {
                Text(model.title)
                    .font(.plusJakarta(size: 28, weight: .bold))
                    .foregroundStyle(theme.ink)
                Text(model.vertical.motionLine)
                    .font(.plusJakarta(size: 14))
                    .foregroundStyle(theme.body)
            }

            Text("Core actions · \(core.count)")
                .font(.plusJakarta(size: 12))
                .foregroundStyle(theme.muted)

            LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 10) {
                ForEach(core) { mode in
                    Button {
                        onCapture(mode.rawValue)
                    } label: {
                        VStack(alignment: .leading, spacing: 6) {
                            Image(systemName: mode.glyph)
                                .foregroundStyle(theme.accent)
                            Text(mode.title(client: client))
                                .font(.plusJakarta(size: 14, weight: .semibold))
                                .foregroundStyle(theme.ink)
                                .multilineTextAlignment(.leading)
                        }
                        .padding(14)
                        .frame(maxWidth: .infinity, minHeight: 88, alignment: .leading)
                        .background(theme.surface)
                        .overlay(
                            RoundedRectangle(cornerRadius: 14, style: .continuous)
                                .stroke(theme.border, lineWidth: 1)
                        )
                        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                        .shadow(color: theme.accent.opacity(0.18), radius: 8, y: 4)
                    }
                    .buttonStyle(.plain)
                }
            }

            Text("Support")
                .font(.plusJakarta(size: 12))
                .foregroundStyle(theme.muted)

            HStack(spacing: 10) {
                supportTile("Network", "person.2") { onOpenMasters(.principals) }
                supportTile("Invite & QR", "qrcode") { onOpenMasters(.invite) }
                supportTile("Story", "book") { onOpenStory() }
            }
        }
        .padding(.horizontal, 16)
    }

    private func supportTile(_ title: String, _ glyph: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            VStack(spacing: 6) {
                Image(systemName: glyph).foregroundStyle(theme.accent)
                Text(title)
                    .font(.plusJakarta(size: 12, weight: .semibold))
                    .foregroundStyle(theme.ink)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 14)
            .background(theme.inset)
            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
        }
        .buttonStyle(.plain)
    }
}

// MARK: - Shared chrome helpers

private func header(_ title: String, _ subtitle: String, theme: OperatingTheme) -> some View {
    OperatingHeader(theme: theme, title: title, bodyText: subtitle)
}

@ViewBuilder
private func section<Content: View>(
    _ title: String,
    theme: OperatingTheme,
    @ViewBuilder content: () -> Content
) -> some View {
    VStack(alignment: .leading, spacing: 10) {
        Text(title)
            .font(.plusJakarta(size: 13, weight: .semibold))
            .foregroundStyle(theme.muted)
        card(theme: theme, content: content)
    }
}

@ViewBuilder
private func card<Content: View>(
    theme: OperatingTheme,
    @ViewBuilder content: () -> Content
) -> some View {
    OperatingCard(theme: theme, content: content)
}

private func metricRow(
    _ label: String,
    _ value: String,
    theme: OperatingTheme,
    tone: Color? = nil
) -> some View {
    HStack {
        Text(label)
            .font(.plusJakarta(size: 13))
            .foregroundStyle(theme.muted)
        Spacer()
        Text(value)
            .font(.plusJakarta(size: 14, weight: .semibold))
            .foregroundStyle(tone ?? theme.ink)
    }
}

private func meter(
    _ label: String,
    spent: String?,
    total: String?,
    theme: OperatingTheme
) -> some View {
    let s = Double(spent ?? "") ?? 0
    let t = Double(total ?? "") ?? 0
    let fraction = t > 0 ? min(1, s / t) : 0
    return VStack(alignment: .leading, spacing: 6) {
        Text(label)
            .font(.plusJakarta(size: 12))
            .foregroundStyle(theme.muted)
        GeometryReader { geo in
            ZStack(alignment: .leading) {
                Capsule().fill(theme.border)
                Capsule()
                    .fill(theme.accent)
                    .frame(width: geo.size.width * fraction)
            }
        }
        .frame(height: 8)
    }
}

private func boardSections(
    _ sections: [OperatingBoardSectionPayload],
    theme: OperatingTheme,
    model: BusinessOperatingModel
) -> some View {
    ForEach(sections) { section in
        VStack(alignment: .leading, spacing: 8) {
            Text(section.title)
                .font(.plusJakarta(size: 16, weight: .semibold))
                .foregroundStyle(theme.ink)
            if !section.detail.isEmpty {
                Text(section.detail)
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(theme.muted)
            }
            if let coverage = section.coverage, !coverage.isEmpty {
                Text(coverage)
                    .font(.plusJakarta(size: 12, weight: .semibold))
                    .foregroundStyle(theme.accent)
            }
            ForEach(section.rows ?? []) { row in
                VStack(alignment: .leading, spacing: 2) {
                    HStack {
                        Text(row.title)
                            .font(.plusJakarta(size: 13))
                            .foregroundStyle(theme.ink)
                        Spacer()
                        if model.financeVisible, let amount = row.amount, !amount.isEmpty {
                            Text(OperatingMoneyFormat.format(model.currency, amount))
                                .font(.plusJakarta(size: 12, weight: .semibold))
                                .foregroundStyle(theme.accent)
                        }
                    }
                    let meta = [row.channelName, row.counterpartyName, row.paymentStatus]
                        .compactMap { $0 }
                        .filter { !$0.isEmpty && $0 != "NONE" }
                        .joined(separator: " · ")
                    if !meta.isEmpty {
                        Text(meta)
                            .font(.plusJakarta(size: 11))
                            .foregroundStyle(theme.muted)
                    }
                }
                .padding(.vertical, 4)
            }
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(theme.surface)
        .overlay(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .stroke(theme.border, lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
    }
}

private func money(_ currency: String, _ raw: String?) -> String {
    OperatingMoneyFormat.format(currency, raw)
}

private func periodLabel(_ iso: String?) -> String {
    guard let iso, iso.count >= 7 else { return "This month" }
    let parts = iso.prefix(7).split(separator: "-")
    guard parts.count == 2, let m = Int(parts[1]) else { return "This month" }
    let months = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"]
    return "\(months[max(0, min(11, m - 1))]) \(parts[0])"
}

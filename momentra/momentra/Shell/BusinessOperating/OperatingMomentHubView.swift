import SwiftUI

/**
 Callers: AppShellView.businessDestinationContent (Ready + company + selected moment).
 No prior iOS operating hub remained after Business V4 rebuild placeholders.
 Reads: OperatingProjectionPayload + WidgetsProjectionPayload from APIClient.
 Writes: none locally; Quick Adds POST via recordOperatingEvent.
 User: "Wire Pulse, Moments, Life and Memory to real backend projections." +
 "Ensure every widget can navigate to its eligible source records."
 TRD-v12: trading amber palette + pulseMetrics groups + Quick Adds tile grid.
 */

enum OperatingHubTab: String {
    case pulse, moments, life, memory, quickAdd

    static func from(_ destination: BottomDestination) -> OperatingHubTab {
        switch destination {
        case .moments: return .moments
        case .life: return .life
        case .memory: return .memory
        case .create: return .quickAdd
        default: return .pulse
        }
    }

    var screenCode: String? {
        switch self {
        case .pulse: return "PULSE"
        case .moments: return "MOMENTS"
        case .life: return "LIFE"
        case .memory: return "MEMORY"
        case .quickAdd: return nil
        }
    }
}

enum WidgetCoveragePresentation: String {
    case complete = "COMPLETE"
    case partial = "PARTIAL"
    case unavailable = "UNAVAILABLE"
    case zero = "ZERO"
    case unknown = ""

    var label: String {
        switch self {
        case .complete: return "Complete"
        case .partial: return "Partial — incomplete history"
        case .unavailable: return "Unavailable"
        case .zero: return "Zero"
        case .unknown: return "Unknown coverage"
        }
    }

    var showValue: Bool { self == .complete || self == .zero }

    var showSources: Bool {
        self == .complete || self == .partial || self == .zero
    }

    static func fromServer(_ code: String?) -> WidgetCoveragePresentation {
        guard let raw = code?.trimmingCharacters(in: .whitespacesAndNewlines).uppercased(),
              let match = WidgetCoveragePresentation(rawValue: raw) else {
            return .unknown
        }
        return match
    }
}

struct HubPalette {
    let bg: Color
    let card: Color
    let border: Color
    let muted: Color
    let text: Color
    let accent: Color
    let onAccent: Color

    static func forType(subtype: String?, family: String?) -> HubPalette {
        let code = subtype?.trimmingCharacters(in: .whitespacesAndNewlines).uppercased()
        if code == DistributionDealershipHub.typeCode {
            return HubPalette(
                bg: BusinessTokens.Colors.distributionCanvas,
                card: BusinessTokens.Colors.distributionSurface,
                border: BusinessTokens.Colors.distributionAccent.opacity(0.28),
                muted: BusinessTokens.Colors.distributionHint,
                text: BusinessTokens.Colors.distributionLabel,
                accent: BusinessTokens.Colors.distributionAccent,
                onAccent: BusinessTokens.Colors.distributionOnAccent
            )
        }
        let tradingWholesale = code == "TRADING_WHOLESALE" ||
            (family == "TRADING_DISTRIBUTION" && code != DistributionDealershipHub.typeCode)
        if tradingWholesale {
            return HubPalette(
                bg: BusinessTokens.Colors.tradingCanvas,
                card: Color(hex: "#111827"),
                border: BusinessTokens.Colors.tradingAccent.opacity(0.28),
                muted: Color(hex: "#94A3B8"),
                text: Color(hex: "#F1F5F9"),
                accent: BusinessTokens.Colors.tradingAccent,
                onAccent: BusinessTokens.Colors.tradingOnAccent
            )
        }
        let accent = BusinessAppStateAccent.forTypeCode(subtype).color
        return HubPalette(
            bg: Color(hex: "#0C0F15"),
            card: Color(hex: "#161B26"),
            border: Color(hex: "#1E293B"),
            muted: Color(hex: "#94A3B8"),
            text: Color(hex: "#E5E0EE"),
            accent: accent,
            onAccent: Color(hex: "#0C0F15")
        )
    }
}

private func pulseGroupTitle(_ code: String) -> String {
    switch code.uppercased() {
    case "TARGET_BUDGET": return "Target & budget"
    case "FINANCE": return "Finance"
    case "PAYMENT_READINESS": return "Payment readiness"
    case "ATTENTION": return "Needs Attention"
    case "ACTIVITY": return "Activity"
    case "GST": return "GST"
    default: return code.replacingOccurrences(of: "_", with: " ")
    }
}

struct OperatingMomentHubView: View {
    let momentId: String
    let momentTitle: String?
    var momentTypeCode: String? = nil
    var companyId: String? = nil
    let tab: OperatingHubTab
    let refreshToken: Int
    var onCreateMoment: () -> Void = {}
    var onOpenQuickAdd: () -> Void = {}
    var onViewAllAttention: () -> Void = {}
    var onQuickAddCode: (String) -> Void = { _ in }
    var onGenerateStory: () -> Void = {}

    @State private var projection: OperatingProjectionPayload?
    @State private var widgets: WidgetsProjectionPayload?
    @State private var sources: WidgetSourcesPayload?
    @State private var drillWidgetId: String?
    @State private var error: String?
    @State private var confirm: String?
    @State private var selectedQuickAdd: String = ""
    @State private var titleText = ""
    @State private var amountText = ""
    @State private var tick = 0
    @State private var loading = true

    private var palette: HubPalette {
        HubPalette.forType(subtype: momentTypeCode ?? projection?.subtype, family: projection?.familyCode)
    }

    private var typeCode: String? {
        (momentTypeCode ?? projection?.subtype)?.uppercased()
    }

    private var isDistribution: Bool {
        DistributionDealershipHub.isDistribution(typeCode)
    }

    private var isTradingWholesale: Bool {
        typeCode == "TRADING_WHOLESALE"
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                if isDistribution, projection != nil {
                    DistributionHeroHeader(
                        projection: projection,
                        momentTitle: momentTitle,
                        companyId: companyId ?? projection?.companyId,
                        palette: palette
                    )
                } else {
                    Text(familyKicker(projection?.familyCode))
                        .font(.system(size: 12, weight: .medium))
                        .foregroundStyle(palette.accent)
                    Text(
                        projection?.title.flatMap { $0.isEmpty ? nil : $0 }
                            ?? momentTitle
                            ?? projection?.subtypeLabel
                            ?? "Business moment"
                    )
                    .font(.system(size: 22, weight: .semibold))
                    .foregroundStyle(palette.text)
                    if typeCode == "CLIENT_CAMPAIGN" {
                        Text("Plan. Launch. Measure.")
                            .font(.system(size: 14, weight: .semibold))
                            .foregroundStyle(palette.accent)
                    }
                    if let party = projection?.partyName, !party.isEmpty {
                        Text(party).font(.system(size: 13)).foregroundStyle(palette.muted)
                    }
                    if let role = projection?.role, !role.isEmpty {
                        Text(role.replacingOccurrences(of: "_", with: " "))
                            .font(.system(size: 12))
                            .foregroundStyle(palette.accent)
                    }
                }

                if tab != .quickAdd && (isDistribution || isTradingWholesale) {
                    DistributionQuickActionStrip(palette: palette) { code in
                        BusinessMomentFamilyConfig.setPendingQuickAdd(momentId: momentId, code: code)
                        onQuickAddCode(code)
                    }
                }

                if loading && projection == nil {
                    ProgressView().tint(palette.accent).frame(maxWidth: .infinity).padding()
                }

                if let error, projection == nil {
                    infoCard(label: "Couldn't load", body: error)
                    Button("Retry") { tick += 1 }.foregroundStyle(palette.accent)
                }

                if let confirm {
                    infoCard(label: "Saved", body: confirm)
                }

                switch tab {
                case .pulse, .moments, .life, .memory:
                    widgetBlocks(for: tab.screenCode ?? "PULSE")
                    boardFallback
                case .quickAdd:
                    quickAddForm
                }

                if let sources {
                    sourceSection(sources)
                }

                if tab != .quickAdd {
                    Button("Quick Adds", action: onOpenQuickAdd).foregroundStyle(palette.accent)
                }
            }
            .padding(20)
        }
        .background(palette.bg)
        .task(id: "\(momentId)-\(refreshToken)-\(tick)") {
            await reload()
        }
    }

    @ViewBuilder
    private func sourceSection(_ sources: WidgetSourcesPayload) -> some View {
        let cov = WidgetCoveragePresentation.fromServer(sources.coverage)
        let totalPart: String = {
            guard cov.showValue, let total = sources.reconciledTotal else { return "" }
            return " · server total \(total)"
        }()
        let header = "\(cov.label)\(totalPart) · \(sources.formula)"
        infoCard(label: "Sources · \(sources.widgetId)", body: header)
        ForEach(sources.sources, id: \.sourceEventId) { row in
            infoCard(
                label: row.eventType.replacingOccurrences(of: "_", with: " "),
                body: [row.title, row.amount, row.occurredAt].compactMap { $0 }.joined(separator: " · ")
            )
        }
        if sources.sources.isEmpty {
            infoCard(label: "Sources", body: "No eligible source records for this widget.")
        }
        Button("Close sources") {
            drillWidgetId = nil
            self.sources = nil
        }
        .foregroundStyle(palette.accent)
    }

    private func widgetTitle(_ w: OperatingWidgetPayload, clientCampaign: Bool) -> String {
        let server = w.title ?? ""
        if clientCampaign {
            return ClientCampaignHubWidgets.displayTitle(widgetId: w.widgetId, serverTitle: server)
        }
        if DistributionDealershipHub.isDistribution(typeCode) {
            return DistributionDealershipHub.displayTitle(widgetId: w.widgetId, serverTitle: server)
        }
        return server.isEmpty ? w.widgetId : server
    }

    private var clientCampaignPulseWidgets: [OperatingWidgetPayload] {
        guard ClientCampaignHubWidgets.isClientCampaign(momentTypeCode ?? projection?.subtype) else { return [] }
        let screen = tab.screenCode ?? "PULSE"
        let filtered = (widgets?.widgets ?? []).filter {
            ($0.screen ?? "").caseInsensitiveCompare(screen) == .orderedSame
        }
        return ClientCampaignHubWidgets.sortWidgets(screen: screen, widgets: filtered)
    }

    @ViewBuilder
    private var boardFallback: some View {
        if tab == .pulse {
            let skipPulseFallback = ClientCampaignHubWidgets.isClientCampaign(momentTypeCode ?? projection?.subtype)
                && !clientCampaignPulseWidgets.isEmpty
            if skipPulseFallback {
                EmptyView()
            } else if isDistribution {
                ForEach(projection?.pulseMetrics ?? []) { group in
                    pulseGroupCard(group, distribution: true)
                }
            } else {
                let groups = projection?.pulseMetrics ?? []
                if !groups.isEmpty {
                    ForEach(groups) { group in
                        pulseGroupCard(group)
                    }
                } else {
                    legacyPulseFallback
                }
            }
        } else {
            let sections: [OperatingBoardSectionPayload] = {
                switch tab {
                case .moments: return projection?.boards?.moments ?? []
                case .life: return projection?.boards?.life ?? []
                case .memory: return projection?.boards?.memory ?? []
                default: return []
                }
            }()
            Text(boardHeading)
                .font(.system(size: 15, weight: .medium))
                .foregroundStyle(palette.text)
            if sections.isEmpty {
                infoCard(label: "This moment", body: "Nothing recorded yet.")
            }
            ForEach(sections) { section in
                sectionCard(section)
            }
        }
    }

    @ViewBuilder
    private var legacyPulseFallback: some View {
        if projection?.financeVisible == true {
            if let budget = projection?.budget {
                let body = budget.configured == true
                    ? "\(budget.remaining ?? "0") left of \(budget.amount ?? "0") \(projection?.currency ?? "")"
                    : "Not recorded"
                infoCard(label: "Budget", body: body)
            }
            if let target = projection?.target {
                let body = target.configured == true
                    ? "\(target.progress ?? "0") of \(target.value ?? "0") (\(target.metric ?? ""))"
                    : "Not recorded"
                infoCard(label: "Target", body: body)
            }
        } else {
            infoCard(label: "Your view", body: "Finance totals stay with Finance, Approver, and Owner.")
        }
        let attention = projection?.attention ?? []
        if attention.isEmpty {
            infoCard(label: "Needs Attention", body: "Nothing is waiting.")
        } else {
            ForEach(attention) { item in
                attentionRow(title: item.title, badge: item.kind)
            }
        }
    }

    private var boardHeading: String {
        if ClientCampaignHubWidgets.isClientCampaign(momentTypeCode ?? projection?.subtype) {
            switch tab {
            case .moments: return "Plan, people, and channels"
            case .life: return "How this campaign connects"
            case .memory: return "What stays on record"
            default: return "Campaign structure"
            }
        }
        switch tab {
        case .life: return "How this moment connects"
        case .memory: return "What stays"
        default:
            switch projection?.familyCode {
            case "TRADING_DISTRIBUTION": return "Sales, purchases, and movement"
            case "MARKETING_CAMPAIGNS": return "Spend, results, and delivery"
            case "MANUFACTURING_PRODUCTION": return "Production, orders, and delivery"
            default: return "Sales, stock, and service"
            }
        }
    }

    @ViewBuilder
    private func pulseGroupCard(_ group: OperatingPulseMetricGroupPayload, distribution: Bool = false) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text(pulseGroupTitle(group.code))
                    .font(.system(size: 16, weight: .bold))
                    .foregroundStyle(palette.text)
                Spacer()
                if distribution && group.code.uppercased() == "ATTENTION" {
                    Button("View all ›", action: onViewAllAttention)
                        .font(.system(size: 12))
                        .foregroundStyle(palette.accent)
                }
            }
            metricTiles(group.metrics ?? [])
            if let why = group.why, !why.isEmpty {
                Text(why)
                    .font(.system(size: 12))
                    .italic(distribution)
                    .foregroundStyle(palette.muted)
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(palette.card)
        .overlay(
            RoundedRectangle(cornerRadius: 20).stroke(palette.border, lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 20))
    }

    @ViewBuilder
    private func sectionCard(_ section: OperatingBoardSectionPayload) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(section.title)
                .font(.system(size: 16, weight: .bold))
                .foregroundStyle(palette.text)
            Text(section.detail.isEmpty ? "Nothing recorded yet." : section.detail)
                .font(.system(size: 12))
                .foregroundStyle(palette.muted)
            if let metrics = section.metrics, !metrics.isEmpty {
                metricTiles(metrics)
            }
            ForEach(section.rows ?? []) { row in
                let amount = row.amount.map { " · \($0)" } ?? ""
                attentionRow(
                    title: row.title + amount,
                    badge: row.badge ?? row.eventType.replacingOccurrences(of: "_", with: " ")
                )
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(palette.card)
        .overlay(
            RoundedRectangle(cornerRadius: 20).stroke(palette.border, lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 20))
    }

    @ViewBuilder
    private func metricTiles(_ metrics: [OperatingBoardMetricPayload]) -> some View {
        FlowLayout(spacing: 8) {
            ForEach(metrics) { m in
                VStack(alignment: .leading, spacing: 2) {
                    Text(m.label.isEmpty ? m.key : m.label)
                        .font(.system(size: 11))
                        .foregroundStyle(palette.muted)
                    Text(
                        isDistribution
                            ? DistributionDealershipHub.formatMetricValue(m.value)
                            : (m.value.isEmpty ? "Not recorded" : m.value)
                    )
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundStyle(palette.text)
                }
                .padding(.horizontal, 12)
                .padding(.vertical, 10)
                .background(palette.bg)
                .overlay(
                    RoundedRectangle(cornerRadius: 12).stroke(palette.border.opacity(0.6), lineWidth: 1)
                )
                .clipShape(RoundedRectangle(cornerRadius: 12))
            }
        }
    }

    private func attentionRow(title: String, badge: String) -> some View {
        HStack {
            Rectangle()
                .fill(palette.accent)
                .frame(width: 3)
            Text(title)
                .font(.system(size: 13))
                .foregroundStyle(palette.text)
            Spacer()
            Text(badge)
                .font(.system(size: 10, weight: .bold))
                .foregroundStyle(palette.accent)
        }
        .padding(.vertical, 10)
        .padding(.trailing, 10)
        .background(palette.bg)
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .overlay(
            RoundedRectangle(cornerRadius: 12).stroke(palette.border, lineWidth: 1)
        )
    }

    @ViewBuilder
    private func widgetBlocks(for screen: String) -> some View {
        let subtype = (momentTypeCode ?? projection?.subtype)?.uppercased()
        let filtered = (widgets?.widgets ?? []).filter {
            ($0.screen ?? "").caseInsensitiveCompare(screen) == .orderedSame
        }
        let list: [OperatingWidgetPayload] = {
            if subtype == ClientCampaignHubWidgets.typeCode {
                return ClientCampaignHubWidgets.sortWidgets(screen: screen, widgets: filtered)
            }
            if DistributionDealershipHub.isDistribution(subtype) {
                return DistributionDealershipHub.sortWidgets(screen: screen, widgets: filtered)
            }
            return filtered
        }()
        if !list.isEmpty {
            Text("Widgets")
                .font(.system(size: 15, weight: .medium))
                .foregroundStyle(palette.text)
            ForEach(list, id: \.widgetId) { w in
                let cov = WidgetCoveragePresentation.fromServer(w.coverage)
                let financeVisible = widgets?.financeVisible == true
                Button {
                    Task { await openSources(w.widgetId) }
                } label: {
                    VStack(alignment: .leading, spacing: 4) {
                        HStack {
                            Text(widgetTitle(w, clientCampaign: subtype == ClientCampaignHubWidgets.typeCode))
                                .font(.system(size: 11))
                                .foregroundStyle(palette.muted)
                            Spacer()
                            Text(cov.label).font(.system(size: 11)).foregroundStyle(palette.accent)
                        }
                        Text(
                            (!financeVisible && (w.financeOnly == true))
                                ? "Finance redacted for your role"
                                : metricLine(w, cov: cov)
                        )
                        .font(.system(size: 14))
                        .foregroundStyle(palette.text)
                        if let why = w.why, !why.isEmpty {
                            Text(why).font(.system(size: 12)).foregroundStyle(palette.muted)
                        }
                    }
                    .padding(14)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(palette.card)
                    .clipShape(RoundedRectangle(cornerRadius: 16))
                    .overlay(
                        RoundedRectangle(cornerRadius: 16).stroke(palette.border, lineWidth: 1)
                    )
                }
                .disabled(!(cov.showSources || !(w.sourceEventIds ?? []).isEmpty))
            }
        }
    }

    private func metricLine(_ w: OperatingWidgetPayload, cov: WidgetCoveragePresentation) -> String {
        let parts = (w.metrics ?? []).map { m -> String in
            let mCov = WidgetCoveragePresentation.fromServer(m.coverage)
            let valueText: String
            if !mCov.showValue || m.valueString == nil {
                valueText = mCov.label
            } else {
                valueText = "\(m.valueString!)\(m.unit.map { " \($0)" } ?? "")"
            }
            return "\(m.label ?? m.key ?? "metric"): \(valueText)"
        }
        return parts.isEmpty ? cov.label : parts.joined(separator: " · ")
    }

    @ViewBuilder
    private var quickAddForm: some View {
        let adds = projection?.quickAdds ?? []
        Text("Quick Adds")
            .font(.system(size: 18, weight: .semibold))
            .foregroundStyle(palette.text)
        Text("Post a real capture — empty tiles stay honest until you add.")
            .font(.system(size: 12))
            .foregroundStyle(palette.muted)
        if adds.isEmpty {
            Text("Your role cannot add to this moment.")
                .font(.system(size: 13))
                .foregroundStyle(palette.muted)
        }
        FlowLayout(spacing: 10) {
            ForEach(adds) { add in
                let on = add.code == selectedQuickAdd
                Button {
                    selectedQuickAdd = add.code
                } label: {
                    Text(add.label)
                        .font(.system(size: 12, weight: .bold))
                        .foregroundStyle(on ? palette.accent : palette.text)
                        .padding(.horizontal, 14)
                        .padding(.vertical, 14)
                        .background(on ? palette.accent.opacity(0.18) : palette.card)
                        .overlay(
                            RoundedRectangle(cornerRadius: 16)
                                .stroke(on ? palette.accent : palette.border, lineWidth: 1)
                        )
                        .clipShape(RoundedRectangle(cornerRadius: 16))
                }
            }
        }
        TextField("Title", text: $titleText)
            .textFieldStyle(.plain)
            .padding(12)
            .background(palette.card)
            .clipShape(RoundedRectangle(cornerRadius: 12))
            .foregroundStyle(palette.text)
        if adds.first(where: { $0.code == selectedQuickAdd })?.needsAmount == true {
            TextField("Amount", text: $amountText)
                .textFieldStyle(.plain)
                .padding(12)
                .background(palette.card)
                .clipShape(RoundedRectangle(cornerRadius: 12))
                .foregroundStyle(palette.text)
        }
        Button("Add") {
            Task { await submitQuickAdd() }
        }
        .disabled(selectedQuickAdd.isEmpty || titleText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
        .font(.system(size: 15, weight: .semibold))
        .foregroundStyle(palette.onAccent)
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .background(palette.accent)
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }

    private func infoCard(label: String, body: String) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label).font(.system(size: 11)).foregroundStyle(palette.muted)
            Text(body).font(.system(size: 14)).foregroundStyle(palette.text)
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(palette.card)
        .overlay(
            RoundedRectangle(cornerRadius: 16).stroke(palette.border, lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private func familyKicker(_ family: String?) -> String {
        switch family {
        case "TRADING_DISTRIBUTION": return "TRADING & DISTRIBUTION"
        case "MARKETING_CAMPAIGNS": return "MARKETING & CAMPAIGNS"
        case "MANUFACTURING_PRODUCTION": return "MANUFACTURING & PRODUCTION"
        default: return "RETAIL & CONSUMER"
        }
    }

    private func reload() async {
        loading = projection == nil
        do {
            let op = try await APIClient.shared.getOperatingMoment(momentId: momentId)
            projection = op
            if selectedQuickAdd.isEmpty {
                selectedQuickAdd = BusinessMomentFamilyConfig.consumePendingQuickAdd(momentId: momentId)
                    ?? op.quickAdds?.first?.code
                    ?? ""
            }
            error = nil
        } catch {
            if projection == nil {
                self.error = error.localizedDescription
            }
        }
        do {
            widgets = try await APIClient.shared.getBusinessMomentWidgets(momentId: momentId)
        } catch {
            // Hub still paints boards from operating projection.
        }
        if let id = drillWidgetId {
            sources = try? await APIClient.shared.getBusinessMomentWidgetSources(
                momentId: momentId,
                widgetId: id
            )
        }
        loading = false
    }

    private func openSources(_ widgetId: String) async {
        drillWidgetId = widgetId
        sources = try? await APIClient.shared.getBusinessMomentWidgetSources(
            momentId: momentId,
            widgetId: widgetId
        )
    }

    private func submitQuickAdd() async {
        let title = titleText.trimmingCharacters(in: .whitespacesAndNewlines)
        let amount = amountText.trimmingCharacters(in: .whitespacesAndNewlines)
        do {
            _ = try await APIClient.shared.recordOperatingEvent(
                momentId: momentId,
                eventType: selectedQuickAdd,
                title: title,
                amount: amount.isEmpty ? nil : amount
            )
            confirm = "Saved \(title)"
            titleText = ""
            amountText = ""
            tick += 1
        } catch {
            self.error = error.localizedDescription
        }
    }
}

/// Simple wrapping layout for metric tiles / quick-add chips.
private struct FlowLayout: Layout {
    var spacing: CGFloat = 8

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let maxWidth = proposal.width ?? .infinity
        var x: CGFloat = 0
        var y: CGFloat = 0
        var rowHeight: CGFloat = 0
        var height: CGFloat = 0
        for sub in subviews {
            let size = sub.sizeThatFits(.unspecified)
            if x + size.width > maxWidth, x > 0 {
                x = 0
                y += rowHeight + spacing
                rowHeight = 0
            }
            rowHeight = max(rowHeight, size.height)
            x += size.width + spacing
            height = y + rowHeight
        }
        return CGSize(width: maxWidth.isFinite ? maxWidth : x, height: height)
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        var x = bounds.minX
        var y = bounds.minY
        var rowHeight: CGFloat = 0
        for sub in subviews {
            let size = sub.sizeThatFits(.unspecified)
            if x + size.width > bounds.maxX, x > bounds.minX {
                x = bounds.minX
                y += rowHeight + spacing
                rowHeight = 0
            }
            sub.place(at: CGPoint(x: x, y: y), proposal: ProposedViewSize(size))
            rowHeight = max(rowHeight, size.height)
            x += size.width + spacing
        }
    }
}

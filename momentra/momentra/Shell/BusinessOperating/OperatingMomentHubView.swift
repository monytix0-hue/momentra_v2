import SwiftUI

/**
 Callers: AppShellView.businessDestinationContent (Ready + company + selected moment).
 No prior iOS operating hub remained after Business V4 rebuild placeholders.
 Reads: OperatingProjectionPayload + WidgetsProjectionPayload from APIClient.
 Writes: none locally; Quick Adds POST via recordOperatingEvent.
 User: "Wire Pulse, Moments, Life and Memory to real backend projections." +
 "Ensure every widget can navigate to its eligible source records."
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

struct OperatingMomentHubView: View {
    let momentId: String
    let momentTitle: String?
    let tab: OperatingHubTab
    let refreshToken: Int
    var onCreateMoment: () -> Void = {}
    var onOpenQuickAdd: () -> Void = {}

    @State private var projection: OperatingProjectionPayload?
    @State private var widgets: WidgetsProjectionPayload?
    @State private var sources: WidgetSourcesPayload?
    @State private var drillWidgetId: String?
    @State private var error: String?
    @State private var confirm: String?
    @State private var tick = 0
    @State private var loading = true

    private let accent = Color(red: 0.20, green: 0.83, blue: 0.60)

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                Text(familyKicker(projection?.familyCode))
                    .font(.system(size: 12, weight: .medium))
                    .foregroundStyle(accent)
                Text(projection?.subtypeLabel ?? momentTitle ?? "Business moment")
                    .font(.system(size: 22, weight: .semibold))
                    .foregroundStyle(Color.white.opacity(0.92))
                if let party = projection?.partyName, !party.isEmpty {
                    Text(party).font(.system(size: 13)).foregroundStyle(Color.white.opacity(0.55))
                }
                if let role = projection?.role, !role.isEmpty {
                    Text(role.replacingOccurrences(of: "_", with: " "))
                        .font(.system(size: 12))
                        .foregroundStyle(accent)
                }

                if loading && projection == nil {
                    ProgressView().tint(accent).frame(maxWidth: .infinity).padding()
                }

                if let error, projection == nil {
                    infoCard(label: "Couldn't load", body: error)
                    Button("Retry") { tick += 1 }.foregroundStyle(accent)
                }

                if let confirm {
                    infoCard(label: "Saved", body: confirm)
                }

                switch tab {
                case .pulse, .moments, .life, .memory:
                    widgetBlocks(for: tab.screenCode ?? "PULSE")
                    boardFallback
                // Callers: CREATE tab. Typed forms replace generic title/amount.
                // User: "No generic fallback form for Quick Adds requiring typed data."
                case .quickAdd:
                    TypedOperatingQuickAddForm(
                        momentId: momentId,
                        projection: projection,
                        onSaved: { tick += 1 }
                    )
                }

                if let sources {
                    sourceSection(sources)
                }

                if tab != .quickAdd {
                    Button("Quick Adds", action: onOpenQuickAdd).foregroundStyle(accent)
                }
            }
            .padding(20)
        }
        .background(Color(red: 0.05, green: 0.06, blue: 0.08))
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
        .foregroundStyle(accent)
    }

    @ViewBuilder
    private var boardFallback: some View {
        if tab == .pulse {
            if projection?.financeVisible == true {
                if let budget = projection?.budget {
                    infoCard(
                        label: "Budget",
                        body: "\(budget.remaining) left of \(budget.amount) \(projection?.currency ?? "")"
                    )
                }
            } else {
                infoCard(label: "Your view", body: "Finance totals stay with Finance, Approver, and Owner.")
            }
            let attention = projection?.attention ?? []
            if attention.isEmpty {
                infoCard(label: "Needs Attention", body: "Nothing is waiting.")
            } else {
                ForEach(attention) { item in
                    infoCard(label: item.kind, body: item.title)
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
                .foregroundStyle(Color.white.opacity(0.9))
            if sections.isEmpty {
                infoCard(label: "This moment", body: "Nothing recorded yet.")
            }
            ForEach(sections) { section in
                infoCard(
                    label: section.title,
                    body: section.detail.isEmpty ? "Nothing recorded yet." : section.detail
                )
            }
        }
    }

    private var boardHeading: String {
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
    private func widgetBlocks(for screen: String) -> some View {
        let list = (widgets?.widgets ?? []).filter {
            ($0.screen ?? "").caseInsensitiveCompare(screen) == .orderedSame
        }
        if !list.isEmpty {
            Text("Widgets")
                .font(.system(size: 15, weight: .medium))
                .foregroundStyle(Color.white.opacity(0.9))
            ForEach(list, id: \.widgetId) { w in
                let cov = WidgetCoveragePresentation.fromServer(w.coverage)
                let financeVisible = widgets?.financeVisible == true
                Button {
                    Task { await openSources(w.widgetId) }
                } label: {
                    VStack(alignment: .leading, spacing: 4) {
                        HStack {
                            Text(w.title?.isEmpty == false ? w.title! : w.widgetId)
                                .font(.system(size: 11))
                                .foregroundStyle(Color.white.opacity(0.55))
                            Spacer()
                            Text(cov.label).font(.system(size: 11)).foregroundStyle(accent)
                        }
                        Text(
                            (!financeVisible && (w.financeOnly == true))
                                ? "Finance redacted for your role"
                                : metricLine(w, cov: cov)
                        )
                        .font(.system(size: 14))
                        .foregroundStyle(Color.white.opacity(0.9))
                        if let why = w.why, !why.isEmpty {
                            Text(why).font(.system(size: 12)).foregroundStyle(Color.white.opacity(0.55))
                        }
                    }
                    .padding(14)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(Color(red: 0.09, green: 0.11, blue: 0.15))
                    .clipShape(RoundedRectangle(cornerRadius: 16))
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

    private func infoCard(label: String, body: String) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label).font(.system(size: 11)).foregroundStyle(Color.white.opacity(0.55))
            Text(body).font(.system(size: 14)).foregroundStyle(Color.white.opacity(0.9))
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(red: 0.09, green: 0.11, blue: 0.15))
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
}

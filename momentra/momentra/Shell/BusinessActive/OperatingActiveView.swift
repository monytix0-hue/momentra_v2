import SwiftUI

/// The active surface for Trading and Distribution. Both verticals render the same five
/// destinations from the same projection; they differ only in accent and copy, which is why
/// there is one view and two thin wrappers (`TradingActiveView`, `DistributionActiveView`).
struct OperatingActiveView: View {
    @ObservedObject var model: BusinessOperatingModel
    let destination: BottomDestination
    let refreshToken: UInt64
    var onOpenQuickAdd: () -> Void = {}
    var onOpenStory: () -> Void = {}
    var onOpenMasters: (OperatingMasterTab) -> Void = { _ in }

    @State private var activeSheet: OperatingActiveSheet?

    private var theme: OperatingTheme { OperatingTheme(vertical: model.vertical) }

    var body: some View {
        ZStack {
            theme.canvas.ignoresSafeArea()
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    phaseNotice
                    switch destination {
                    case .pulse:
                        pulseTab
                    case .moments:
                        momentsTab
                    case .life:
                        lifeTab
                    case .memory:
                        memoryTab
                    case .create:
                        quickAddTab
                    }
                }
                .padding(.horizontal, 16)
                .padding(.top, 12)
                .padding(.bottom, 32)
            }
        }
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
                        onOpenRow: { activeSheet = .row($0) }
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

    /// One sheet at a time: opening a record from a view-all replaces it, which is what the
    /// Figma flow does and what a single `.sheet` can actually express.
    private func openRow(_ row: OperatingActivityPayload) {
        activeSheet = .row(row)
    }

    private func openSheet(_ code: String) {
        guard let spec = OperatingSheetCatalog.spec(forCode: code) else { return }
        activeSheet = .viewAll(spec)
    }

    // MARK: - Load phase

    /// The projection is the only source of truth, so a failed read is stated rather than
    /// rendered as a page of zeros, and a retired moment is stated as retired.
    @ViewBuilder
    private var phaseNotice: some View {
        switch model.phase {
        case .loading:
            HStack(spacing: 8) {
                ProgressView().tint(theme.accent)
                Text("Reading what was recorded…")
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(theme.body)
            }
        case let .error(message):
            OperatingCard(theme: theme) {
                Text("Could not read this moment")
                    .font(.plusJakarta(size: 14, weight: .semibold))
                    .foregroundStyle(theme.ink)
                Text(message)
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(theme.body)
                Button {
                    Task { await model.load() }
                } label: {
                    Text("Try again")
                        .font(.plusJakarta(size: 13, weight: .semibold))
                        .foregroundStyle(theme.accent)
                }
                .buttonStyle(.plain)
            }
        case .empty:
            OperatingEmptyState(
                theme: theme,
                title: "This moment is closed",
                bodyText: "A closed moment is read-only. Start a new one to record again.",
                glyph: "◻︎"
            )
        case .ready:
            EmptyView()
        }
    }

    // MARK: - Pulse

    private var pulseTab: some View {
        VStack(alignment: .leading, spacing: 14) {
            OperatingHeader(
                theme: theme,
                title: "Pulse",
                bodyText: model.vertical == .trading
                    ? "Your sales plan, trade commitments and ready money."
                    : "Your sales plan, distribution commitments and ready money."
            )
            HStack(spacing: 8) {
                OperatingPill(theme: theme, label: periodLabel, accent: true)
                OperatingPill(theme: theme, label: model.vertical.partyNoun)
                OperatingPill(theme: theme, label: model.vertical.motionLine)
            }
            if model.financeVisible {
                financeHero
            }
            pulseSurface("TARGET_BUDGET", title: "Target & Budget")
            pulseSurface("FINANCE", title: "Finance")
            pulseSurface("PAYMENT_READINESS", title: "Payment Readiness")
            pulseSurface("ATTENTION", title: "Needs Attention", attentionRows: true)
            pulseSurface("ACTIVITY", title: "Activity")
            pulseSurface("GST", title: "GST Position")
        }
    }

    private var financeHero: some View {
        OperatingCard(theme: theme) {
            Text("Recorded sales")
                .font(.plusJakarta(size: 11))
                .foregroundStyle(theme.body)
            Text(OperatingMoneyFormat.format(model.currency, model.projection?.finance?.sales))
                .font(.plusJakarta(size: 32, weight: .bold))
                .foregroundStyle(theme.ink)
            HStack(spacing: 16) {
                heroFact("Received", model.projection?.finance?.received)
                heroFact("Outstanding", model.projection?.finance?.outstanding)
                heroFact("Payable", model.projection?.finance?.payable)
            }
            OperatingWhyFootnote(
                theme: theme,
                text: "Only recorded money counts. A commitment is not revenue until it is billed."
            )
        }
    }

    private func heroFact(_ label: String, _ value: String?) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(label)
                .font(.plusJakarta(size: 10))
                .foregroundStyle(theme.muted)
            Text(OperatingMoneyFormat.format(model.currency, value))
                .font(.plusJakarta(size: 13, weight: .semibold))
                .foregroundStyle(theme.body)
        }
    }

    private func pulseSurface(
        _ code: String,
        title: String,
        attentionRows: Bool = false
    ) -> some View {
        let group = model.pulseGroup(code: code)
        let metrics = group?.metrics ?? []
        return OperatingCard(theme: theme) {
            HStack {
                Text(title)
                    .font(.plusJakarta(size: 15, weight: .semibold))
                    .foregroundStyle(theme.ink)
                Spacer()
                Button {
                    openSheet(code)
                } label: {
                    Text("View all ›")
                        .font(.plusJakarta(size: 11, weight: .semibold))
                        .foregroundStyle(theme.accent)
                }
                .buttonStyle(.plain)
            }
            if metrics.isEmpty {
                Text("Not recorded yet")
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(theme.warning)
            } else {
                OperatingMetricGrid(theme: theme, metrics: metrics)
            }
            if attentionRows, let attention = model.projection?.attention, !attention.isEmpty {
                VStack(spacing: 0) {
                    ForEach(attention.prefix(3)) { item in
                        HStack(spacing: 8) {
                            Text(item.title)
                                .font(.plusJakarta(size: 13))
                                .foregroundStyle(theme.ink)
                                .lineLimit(1)
                            Spacer(minLength: 4)
                            OperatingBadgeChip(theme: theme, badge: badgeFor(item))
                        }
                        .padding(.vertical, 6)
                    }
                }
            }
            OperatingWhyFootnote(theme: theme, text: group?.why)
        }
    }

    private func badgeFor(_ item: OperatingAttentionPayload) -> String? {
        switch item.severity {
        case "CRITICAL", "OVERDUE": return "Overdue"
        case "WARNING": return "Open"
        default: return nil
        }
    }

    // MARK: - Moments

    private var momentsTab: some View {
        VStack(alignment: .leading, spacing: 14) {
            OperatingHeader(
                theme: theme,
                title: "Moments",
                bodyText: model.vertical == .trading
                    ? "Orders, stock, buyers and open commitments."
                    : "Orders, stock, dealers and open commitments."
            )
            boardSection("orders", title: "This month, in motion")
            boardSection("stock", title: "Stock & products")
            boardSection("network", title: model.vertical == .trading ? "Buyers, suppliers & channels" : "Dealers, principals & channels")
            boardSection("M4", title: "Open commitments")
        }
    }

    // MARK: - Life

    private var lifeTab: some View {
        VStack(alignment: .leading, spacing: 14) {
            OperatingHeader(
                theme: theme,
                title: "Life",
                bodyText: "What this business is doing right now."
            )
            boardSection("L1", title: "Current business")
            boardSection("L2", title: model.vertical == .trading ? "Buyers & channels" : "Dealers & channels")
            boardSection("L3", title: "Money moving")
            boardSection("L4", title: "People & activity")
        }
    }

    // MARK: - Memory

    private var memoryTab: some View {
        VStack(alignment: .leading, spacing: 14) {
            OperatingHeader(
                theme: theme,
                title: "Memory",
                bodyText: model.vertical == .trading
                    ? "What this trade keeps from the month."
                    : "What this dealership keeps from the month."
            )
            HStack(spacing: 8) {
                OperatingPill(theme: theme, label: periodLabel)
                OperatingPill(theme: theme, label: "Current")
            }
            boardSection("ME1", title: "Monthly memory")
            boardSection("ME2", title: "Money memory")
            boardSection("fulfilment", title: "Fulfilment memory")
            boardSection("ME4", title: "Patterns & decisions")
            storyCta
        }
    }

    private var storyCta: some View {
        OperatingCard(theme: theme) {
            Text("Business story")
                .font(.plusJakarta(size: 16, weight: .semibold))
                .foregroundStyle(theme.ink)
            Text(
                "Eight pages from the month — moment, motion, "
                    + (model.vertical == .trading ? "orders" : "dealers")
                    + ", money, stock, network, attention, and so far."
            )
            .font(.plusJakarta(size: 12))
            .foregroundStyle(theme.body)
            Button(action: onOpenStory) {
                Text("Generate business story")
                    .font(.plusJakarta(size: 14, weight: .semibold))
                    .foregroundStyle(theme.onAccent)
                    .padding(.horizontal, 16)
                    .padding(.vertical, 12)
                    .background(theme.accent)
                    .clipShape(RoundedRectangle(cornerRadius: 12))
            }
            .buttonStyle(.plain)
            OperatingWhyFootnote(
                theme: theme,
                text: "Every page is built from recorded activity. An empty page says so rather than estimating."
            )
        }
    }

    // MARK: - Quick adds

    private var quickAddTab: some View {
        VStack(alignment: .leading, spacing: 14) {
            OperatingHeader(
                theme: theme,
                title: "Quick adds",
                bodyText: "Record what happened. Each one posts a single event."
            )
            LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 10) {
                ForEach(model.projection?.quickAdds ?? [], id: \.id) { add in
                    Button(action: onOpenQuickAdd) {
                        VStack(alignment: .leading, spacing: 6) {
                            Text(add.label)
                                .font(.plusJakarta(size: 14, weight: .semibold))
                                .foregroundStyle(theme.ink)
                                .multilineTextAlignment(.leading)
                            Text(add.needsAmount ? "Needs an amount" : "Recorded with detail")
                                .font(.plusJakarta(size: 11))
                                .foregroundStyle(theme.muted)
                        }
                        .padding(12)
                        .frame(maxWidth: .infinity, minHeight: 78, alignment: .leading)
                        .background(theme.inset)
                        .overlay(
                            RoundedRectangle(cornerRadius: 14)
                                .stroke(theme.border, lineWidth: 1)
                        )
                        .clipShape(RoundedRectangle(cornerRadius: 14))
                    }
                    .buttonStyle(.plain)
                }
            }
            if (model.projection?.quickAdds ?? []).isEmpty {
                OperatingEmptyState(
                    theme: theme,
                    title: "No captures available",
                    bodyText: "Quick adds appear once this moment knows its vertical."
                )
            }
            Button {
                onOpenMasters(.catalogue)
            } label: {
                Text("Masters & price lists ›")
                    .font(.plusJakarta(size: 13, weight: .semibold))
                    .foregroundStyle(theme.accent)
            }
            .buttonStyle(.plain)
        }
    }

    // MARK: - Board section

    private func boardSection(_ code: String, title: String) -> some View {
        let section = model.section(code: code)
        return OperatingCard(theme: theme) {
            HStack {
                Text(title)
                    .font(.plusJakarta(size: 15, weight: .semibold))
                    .foregroundStyle(theme.ink)
                Spacer()
                Button {
                    openSheet(code)
                } label: {
                    Text("View all ›")
                        .font(.plusJakarta(size: 11, weight: .semibold))
                        .foregroundStyle(theme.accent)
                }
                .buttonStyle(.plain)
            }
            if let metrics = section?.metrics, !metrics.isEmpty {
                OperatingMetricGrid(theme: theme, metrics: Array(metrics.prefix(4)))
            }
            let rows = Array((section?.rows ?? []).prefix(3))
            if rows.isEmpty {
                Text("Not recorded yet")
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(theme.warning)
            } else {
                VStack(spacing: 0) {
                    ForEach(rows) { row in
                        OperatingActivityRow(
                            theme: theme,
                            row: row,
                            currency: model.currency,
                            financeVisible: model.financeVisible,
                            onTap: { openRow(row) }
                        )
                        Divider().overlay(theme.border)
                    }
                }
            }
            OperatingWhyFootnote(theme: theme, text: section?.why)
        }
    }

    private var periodLabel: String {
        let start = model.projection?.startDate?.prefix(10).description
        let end = model.projection?.endDate?.prefix(10).description
        switch (start, end) {
        case let (start?, end?): return "\(start) – \(end)"
        case let (start?, nil): return start
        default: return "This month"
        }
    }
}

/// `TRD-*-v12`.
struct TradingActiveView: View {
    @ObservedObject var model: BusinessOperatingModel
    let destination: BottomDestination
    let refreshToken: UInt64
    var onOpenQuickAdd: () -> Void = {}
    var onOpenStory: () -> Void = {}
    var onOpenMasters: (OperatingMasterTab) -> Void = { _ in }

    var body: some View {
        OperatingActiveView(
            model: model,
            destination: destination,
            refreshToken: refreshToken,
            onOpenQuickAdd: onOpenQuickAdd,
            onOpenStory: onOpenStory,
            onOpenMasters: onOpenMasters
        )
    }
}

/// `DST-*-v12`.
struct DistributionActiveView: View {
    @ObservedObject var model: BusinessOperatingModel
    let destination: BottomDestination
    let refreshToken: UInt64
    var onOpenQuickAdd: () -> Void = {}
    var onOpenStory: () -> Void = {}
    var onOpenMasters: (OperatingMasterTab) -> Void = { _ in }

    var body: some View {
        OperatingActiveView(
            model: model,
            destination: destination,
            refreshToken: refreshToken,
            onOpenQuickAdd: onOpenQuickAdd,
            onOpenStory: onOpenStory,
            onOpenMasters: onOpenMasters
        )
    }
}

/// The one sheet the active view can show, so a view-all can hand off to a record without two
/// competing `.sheet` modifiers.
private enum OperatingActiveSheet: Identifiable {
    case viewAll(OperatingSheetSpec)
    case row(OperatingActivityPayload)

    var id: String {
        switch self {
        case let .viewAll(spec): return "viewAll:\(spec.code)"
        case let .row(row): return "row:\(row.id)"
        }
    }
}

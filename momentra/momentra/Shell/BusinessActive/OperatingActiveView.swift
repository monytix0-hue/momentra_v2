import SwiftUI

/// The active surface for Trading and Distribution. Both verticals render the same five
/// destinations from the same projection; they differ only in accent and copy, which is why
/// there is one view and two thin wrappers (`TradingActiveView`, `DistributionActiveView`).
///
/// Each destination is its own v12 screen (`OperatingV12Screens.swift`), and the hub is the v11
/// grid (`OperatingV12HubScreen.swift`). This view owns only the frame around them: the load
/// phase, the one-sheet router, and the refresh trigger.
struct OperatingActiveView: View {
    @ObservedObject var model: BusinessOperatingModel
    let destination: BottomDestination
    let refreshToken: UInt64
    var onOpenQuickAdd: () -> Void = {}
    var onOpenStory: () -> Void = {}
    var onOpenMasters: (OperatingMasterTab) -> Void = { _ in }
    /// A capture the operator picked from a tile or a quick action. The host presents the sheet;
    /// the fallback opens the hub when the shell has no capture route of its own.
    var onCapture: ((String) -> Void)? = nil

    @State private var activeSheet: OperatingActiveSheet?

    private var theme: OperatingTheme { OperatingTheme(vertical: model.vertical) }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                phaseNotice
                switch destination {
                case .pulse:
                    OperatingPulseScreen(
                        model: model,
                        theme: theme,
                        onCapture: capture,
                        onViewAll: openSheet,
                        onOpenRow: openRow
                    )
                case .moments:
                    OperatingMomentsScreen(
                        model: model,
                        theme: theme,
                        onCapture: capture,
                        onViewAll: openSheet,
                        onOpenRow: openRow
                    )
                case .life:
                    OperatingLifeScreen(
                        model: model,
                        theme: theme,
                        onCapture: capture,
                        onViewAll: openSheet,
                        onOpenRow: openRow
                    )
                case .memory:
                    OperatingMemoryScreen(
                        model: model,
                        theme: theme,
                        onCapture: capture,
                        onStory: onOpenStory,
                        onViewAll: openSheet,
                        onOpenRow: openRow
                    )
                case .create:
                    // The hub lives in a sheet, so a tab that lands here shows the same grid
                    // inline rather than a second, shallower quick-add list.
                    OperatingHubScreen(
                        model: model,
                        theme: theme,
                        onCapture: capture,
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
                            switch action {
                            case "MEMORY":
                                onOpenStory()
                            case "ATTENTION":
                                onOpenQuickAdd()
                            case "QUICK_ADD":
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

    /// A tile asks for a capture. When the host has no route of its own — the plain tab bar
    /// mounting this view — the hub opens, which is the only honest place to choose one.
    private func capture(_ code: String) {
        if let onCapture {
            onCapture(code)
        } else {
            onOpenQuickAdd()
        }
    }

    /// One sheet at a time: opening a record from a view-all replaces it, which is what the
    /// Figma flow does and what a single `.sheet` can actually express.
    private func openRow(_ row: OperatingActivityPayload) {
        activeSheet = .row(row)
    }

    private func openSheet(_ code: String) {
        guard let spec = OperatingSheetCatalog.spec(forCode: code, vertical: model.vertical) else { return }
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
                    .foregroundStyle(OperatingV12.subtitle)
            }
            .padding(.horizontal, 16)
        case let .error(message):
            OperatingV12Card(theme: theme) {
                Text("Couldn't load this moment")
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
                        .overlay(
                            RoundedRectangle(cornerRadius: 12, style: .continuous)
                                .stroke(theme.accent.opacity(0.35), lineWidth: 1)
                        )
                        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                }
                .buttonStyle(.plain)
            }
            .padding(.horizontal, 16)
        case .empty:
            OperatingV12Card(theme: theme) {
                Text("This moment is closed")
                    .font(.plusJakarta(size: 14, weight: .bold))
                    .foregroundStyle(OperatingV12.title)
                Text("A closed moment is read-only. Start a new one to record again.")
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(OperatingV12.subtitle)
            }
            .padding(.horizontal, 16)
        case .ready:
            EmptyView()
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
    var onCapture: ((String) -> Void)? = nil

    var body: some View {
        OperatingActiveView(
            model: model,
            destination: destination,
            refreshToken: refreshToken,
            onOpenQuickAdd: onOpenQuickAdd,
            onOpenStory: onOpenStory,
            onOpenMasters: onOpenMasters,
            onCapture: onCapture
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
    var onCapture: ((String) -> Void)? = nil

    var body: some View {
        OperatingActiveView(
            model: model,
            destination: destination,
            refreshToken: refreshToken,
            onOpenQuickAdd: onOpenQuickAdd,
            onOpenStory: onOpenStory,
            onOpenMasters: onOpenMasters,
            onCapture: onCapture
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

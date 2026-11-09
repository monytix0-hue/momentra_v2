import SwiftUI

/// Mounts Trading / Distribution / Manufacturing / MTO / Marketing / Retail / Restaurant / Service for the selected business moment.
///
/// The host owns the model so the shell never has to know which vertical is on screen: it reads
/// the moment's subtype once, then every surface below is shared. Sheets live here too, so the
/// shell only forwards the destination and the refresh token.
///
/// Every sheet goes through one router. Chaining several `.sheet` modifiers on the same view is
/// unreliable — only the last one wins on some iOS versions — so the host keeps a single sheet and
/// a case for each surface.
struct OperatingExperienceHost: View {
    let momentId: String
    let companyId: String?
    let subtype: String?
    let destination: BottomDestination
    let refreshToken: UInt64
    /// Called with the moment id when a story has been composed, so the shell can present it.
    var onStoryReady: (String) -> Void = { _ in }

    @StateObject private var model: BusinessOperatingModel
    @State private var sheet: OperatingHostSheet?
    /// A capture chosen from a tile before the sheet router has finished presenting the hub.
    @State private var pendingCapture: String?

    init(
        momentId: String,
        companyId: String?,
        subtype: String?,
        destination: BottomDestination,
        refreshToken: UInt64,
        onStoryReady: @escaping (String) -> Void = { _ in }
    ) {
        self.momentId = momentId
        self.companyId = companyId
        self.subtype = subtype
        self.destination = destination
        self.refreshToken = refreshToken
        self.onStoryReady = onStoryReady
        _model = StateObject(wrappedValue: BusinessOperatingModel(
            momentId: momentId,
            companyId: companyId,
            subtype: subtype
        ))
    }

    private var theme: OperatingTheme { OperatingTheme(vertical: model.vertical) }
    private var isMarketing: Bool { model.vertical.isMarketing }

    var body: some View {
        Group {
            if isMarketing {
                MarketingActiveView(
                    model: model,
                    destination: destination,
                    refreshToken: refreshToken,
                    onOpenQuickAdd: { sheet = .hub },
                    onOpenStory: { sheet = .story },
                    onOpenMasters: { sheet = .masters($0) },
                    onCapture: { code in sheet = .capture(code) }
                )
            } else {
                OperatingActiveView(
                    model: model,
                    destination: destination,
                    refreshToken: refreshToken,
                    onOpenQuickAdd: { sheet = .hub },
                    onOpenStory: { sheet = .story },
                    onOpenMasters: { sheet = .masters($0) },
                    onCapture: { code in sheet = .capture(code) }
                )
            }
        }
        .sheet(item: $sheet, onDismiss: reopenPendingCapture) { route in
            Group {
                switch route {
                case .hub:
                    // The hub is a sheet, but its tiles still open captures — so it gets the same
                    // capture route, and the router replaces itself once this sheet is down.
                    if isMarketing {
                        MarketingQuickAddHubView(
                            theme: theme,
                            model: model,
                            onCapture: { mode in
                                pendingCapture = mode.rawValue
                                sheet = nil
                            },
                            onOpenMasters: { sheet = .masters(.principals) }
                        )
                    } else {
                        OperatingHubScreen(
                            model: model,
                            theme: theme,
                            onCapture: { code in
                                pendingCapture = code
                                sheet = nil
                            },
                            onOpenMasters: { tab in sheet = .masters(tab) },
                            onOpenStory: { sheet = .story }
                        )
                    }
                case let .capture(code):
                    if isMarketing, let mode = MarketingCaptureMode(rawValue: code.uppercased()) {
                        MarketingCaptureSheet(theme: theme, mode: mode, model: model)
                    } else {
                        OperatingCaptureSheet(theme: theme, mode: code, model: model)
                    }
                case let .masters(tab):
                    OperatingMastersView(theme: theme, model: model, initialTab: tab)
                        .task { await model.loadPriceLists() }
                case .story:
                    OperatingStoryConfigSheet(theme: theme, model: model) { id in
                        sheet = nil
                        onStoryReady(id)
                    }
                }
            }
            .presentationDetents([.large])
            .presentationDragIndicator(.visible)
            .presentationCornerRadius(24)
            .preferredColorScheme(.dark)
        }
    }

    /// One sheet at a time means a capture picked from the hub has to wait for the hub to go away
    /// before it can present. A short hop through the run loop is enough — and it keeps the
    /// capture sheet's own detents intact.
    private func reopenPendingCapture() {
        guard let code = pendingCapture else { return }
        pendingCapture = nil
        DispatchQueue.main.async {
            sheet = .capture(code)
        }
    }
}

/// The one sheet the host can show at a time.
private enum OperatingHostSheet: Identifiable {
    case hub
    case capture(String)
    case masters(OperatingMasterTab)
    case story

    var id: String {
        switch self {
        case .hub: return "hub"
        case let .capture(code): return "capture:\(code)"
        case let .masters(tab): return "masters:\(tab.rawValue)"
        case .story: return "story"
        }
    }
}

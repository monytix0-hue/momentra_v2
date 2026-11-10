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
    /// Master chosen in Parts/Materials browser — applied when the capture sheet opens.
    @State private var pendingBrowserPrefill: OperatingBrowserItem?

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
    private var kit: OperatingKit { model.vertical.kit }

    var body: some View {
        Group {
            switch kit {
            case .marketing:
                MarketingActiveView(
                    model: model,
                    destination: destination,
                    refreshToken: refreshToken,
                    onOpenQuickAdd: { sheet = .hub },
                    onOpenStory: { sheet = .story },
                    onOpenMasters: { sheet = .masters($0) },
                    onCapture: { code in sheet = .capture(code) }
                )
            case .retail:
                RetailActiveView(
                    model: model,
                    destination: destination,
                    refreshToken: refreshToken,
                    onOpenQuickAdd: { sheet = .hub },
                    onOpenStory: { sheet = .story },
                    onOpenMasters: { sheet = .masters($0) },
                    onCapture: { code in sheet = .capture(code) }
                )
            case .restaurant:
                RestaurantActiveView(
                    model: model,
                    destination: destination,
                    refreshToken: refreshToken,
                    onOpenQuickAdd: { sheet = .hub },
                    onOpenStory: { sheet = .story },
                    onOpenMasters: { sheet = .masters($0) },
                    onCapture: { code in sheet = .capture(code) }
                )
            case .service:
                ServiceActiveView(
                    model: model,
                    destination: destination,
                    refreshToken: refreshToken,
                    onOpenQuickAdd: { sheet = .hub },
                    onOpenStory: { sheet = .story },
                    onOpenMasters: { sheet = .masters($0) },
                    onCapture: { code in sheet = .capture(code) }
                )
            case .tradingV12, .distributionV12, .manufacturing, .madeToOrder:
                // MTO shares Pulse/Moments/Life/Memory shells (job copy via theme); hub is dedicated.
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
                    hubSheet
                case let .capture(code):
                    captureSheet(code)
                case let .masters(tab):
                    OperatingMastersView(theme: theme, model: model, initialTab: tab)
                        .task { await model.loadPriceLists() }
                case let .partsBrowser(kind):
                    OperatingPartsBrowserView(
                        theme: theme,
                        kind: kind,
                        items: model.browserItems(kind: kind),
                        contextLabel: model.title,
                        onAdd: { item in
                            pendingBrowserPrefill = item
                            pendingCapture = captureHintForBrowser(
                                kind: kind,
                                subtype: model.projection?.subtype ?? model.vertical.rawValue
                            )
                            sheet = nil
                        },
                        onCreateNew: { sheet = .masters(.catalogue) },
                        onDismiss: { sheet = nil }
                    )
                    .padding(16)
                    .background(theme.canvas)
                    .task { if model.masters.isEmpty { await model.loadMasters() } }
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
        .overlay(alignment: .top) {
            if kit == .madeToOrder {
                MtoToastOverlay()
            }
        }
        .onAppear {
            guard kit == .madeToOrder else { return }
            MtoToastHost.shared.onAction = { action in
                switch action {
                case .openPulse, .goBack, .dismiss: break
                case .confirmOrder: sheet = .capture("CONFIRM")
                case .openCustomers: sheet = .masters(.principals)
                case .openLines: sheet = .partsBrowser(.materials) // Job PO materials
                case .openProducts: sheet = .partsBrowser(.parts)
                }
            }
        }
    }

    @ViewBuilder
    private var hubSheet: some View {
        switch kit {
        case .marketing:
            MarketingQuickAddHubView(
                theme: theme,
                model: model,
                onCapture: { mode in
                    pendingCapture = mode.rawValue
                    sheet = nil
                },
                onOpenMasters: { sheet = .masters(.principals) },
                onOpenStory: { sheet = .story }
            )
        case .retail:
            RetailHubScreen(
                model: model,
                theme: theme,
                onCapture: { code in
                    pendingCapture = code
                    sheet = nil
                },
                onOpenMasters: { tab in sheet = .masters(tab) },
                onOpenStory: { sheet = .story }
            )
        case .restaurant:
            RestaurantHubScreen(
                model: model,
                theme: theme,
                onCapture: { code in
                    pendingCapture = code
                    sheet = nil
                },
                onOpenMasters: { tab in sheet = .masters(tab) },
                onOpenStory: { sheet = .story }
            )
        case .service:
            ServiceHubScreen(
                model: model,
                theme: theme,
                onCapture: { code in
                    pendingCapture = code
                    sheet = nil
                },
                onOpenMasters: { tab in sheet = .masters(tab) },
                onOpenStory: { sheet = .story }
            )
        case .madeToOrder:
            MadeToOrderHubScreen(
                model: model,
                theme: theme,
                onCapture: { code in
                    pendingCapture = code
                    sheet = nil
                },
                onOpenMasters: { tab in
                    switch tab {
                    case .catalogue: sheet = .partsBrowser(.parts)
                    case .locations: sheet = .partsBrowser(.materials)
                    default: sheet = .masters(tab)
                    }
                },
                onOpenStory: { sheet = .story }
            )
        case .tradingV12, .distributionV12, .manufacturing:
            OperatingHubScreen(
                model: model,
                theme: theme,
                onCapture: { code in
                    pendingCapture = code
                    sheet = nil
                },
                onOpenMasters: { tab in
                    // MFG Parts & BOM opens the Parts browser (parity with Android hub).
                    // Trading/Dist keep the masters sheet — SKU pick happens via capture LOV.
                    if kit == .manufacturing, tab == .catalogue {
                        sheet = .partsBrowser(.parts)
                    } else {
                        sheet = .masters(tab)
                    }
                },
                onOpenStory: { sheet = .story }
            )
        }
    }

    @ViewBuilder
    private func captureSheet(_ code: String) -> some View {
        let upper = code.uppercased()
        let prefill = pendingBrowserPrefill
        if kit == .marketing, let mode = MarketingCaptureMode(rawValue: upper) {
            MarketingCaptureSheet(theme: theme, mode: mode, model: model)
        } else if kit == .restaurant, upper == "POS" {
            RestaurantPosSheet(theme: theme, model: model)
        } else if kit == .restaurant, upper == "KOT" {
            RestaurantKotSheet(theme: theme, model: model)
        } else if kit == .retail, upper == "SALE" {
            RetailPosSheet(theme: theme, model: model)
        } else if kit == .retail, [OperatingCaptureLayout.holdMode, OperatingCaptureLayout.transferMode, OperatingCaptureLayout.reprintMode].contains(upper) {
            RetailUtilitySheet(theme: theme, mode: code, model: model)
        } else if kit == .retail {
            RetailCaptureSheet(theme: theme, mode: code, model: model, browserPrefill: prefill)
                .onAppear { pendingBrowserPrefill = nil }
        } else {
            OperatingCaptureSheet(
                theme: theme,
                mode: code,
                model: model,
                browserPrefill: prefill,
                onRouteToMode: { target in
                    sheet = .capture(target.uppercased())
                }
            )
                .onAppear { pendingBrowserPrefill = nil }
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
    case partsBrowser(OperatingBrowserKind)
    case story

    var id: String {
        switch self {
        case .hub: return "hub"
        case let .capture(code): return "capture:\(code)"
        case let .masters(tab): return "masters:\(tab.rawValue)"
        case let .partsBrowser(kind): return "browser:\(kind)"
        case .story: return "story"
        }
    }
}

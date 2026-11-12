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
                    if kit == .service {
                        ServiceMastersView(theme: theme, model: model, initialTab: tab)
                    } else if kit == .retail {
                        RetailMastersView(theme: theme, model: model, initialTab: tab)
                    } else if kit == .manufacturing {
                        MfgMastersSheet(
                            theme: theme,
                            tab: mfgMasterTab(from: tab),
                            model: model,
                            onBrowseParts: { sheet = .partsBrowser(.parts) }
                        )
                    } else if kit == .madeToOrder {
                        MtoMastersSheet(
                            theme: theme,
                            tab: mtoMasterTab(from: tab),
                            model: model,
                            onOpenForm: { sheet = .mtoForm($0) },
                            onBrowseParts: { sheet = .partsBrowser(.parts) }
                        )
                    } else if kit == .restaurant {
                        RestaurantMastersView(theme: theme, model: model, initialTab: tab)
                    } else {
                        OperatingMastersView(theme: theme, model: model, initialTab: tab)
                            .task { await model.loadPriceLists() }
                    }
                case let .partsBrowser(kind):
                    if kit == .manufacturing {
                        if kind == .materials {
                            MfgMaterialsBrowserSheet(
                                theme: theme,
                                model: model,
                                onAdd: { master in
                                    pendingBrowserPrefill = OperatingBrowserItem(
                                        id: master.id,
                                        name: master.name,
                                        meta: [master.code, master.unit].compactMap { $0 }.joined(separator: " · "),
                                        priceLabel: master.purchasePrice ?? master.sellPrice,
                                        category: mfgPartCategory(master),
                                        unit: master.unit,
                                        unitPrice: master.purchasePrice ?? master.sellPrice,
                                        masterKind: master.kind,
                                        code: master.code
                                    )
                                    // Must use PURCHASE_CAPTURE — bare PURCHASE reopens this browser.
                                    pendingCapture = "PURCHASE_CAPTURE"
                                    sheet = nil
                                },
                                onNewBom: { sheet = .mfgForm(.bom) },
                                onDismiss: { sheet = nil }
                            )
                        } else {
                            MfgPartsBrowserSheet(
                                theme: theme,
                                model: model,
                                onAdd: { master in
                                    pendingBrowserPrefill = OperatingBrowserItem(
                                        id: master.id,
                                        name: master.name,
                                        meta: [master.code, master.unit].compactMap { $0 }.joined(separator: " · "),
                                        priceLabel: master.sellPrice,
                                        category: mfgPartCategory(master),
                                        unit: master.unit,
                                        unitPrice: master.sellPrice,
                                        masterKind: master.kind,
                                        code: master.code
                                    )
                                    pendingCapture = "ORDER"
                                    sheet = nil
                                },
                                onNewPart: { sheet = .mfgForm(.part) },
                                onDismiss: { sheet = nil }
                            )
                        }
                    } else if kit == .madeToOrder {
                        if kind == .materials {
                            MtoMaterialsBrowserSheet(
                                theme: theme,
                                model: model,
                                onAdd: { master in
                                    pendingBrowserPrefill = OperatingBrowserItem(
                                        id: master.id,
                                        name: master.name,
                                        meta: [master.code, master.unit].compactMap { $0 }.joined(separator: " · "),
                                        priceLabel: master.purchasePrice ?? master.sellPrice,
                                        category: mtoPartCategory(master),
                                        unit: master.unit,
                                        unitPrice: master.purchasePrice ?? master.sellPrice,
                                        masterKind: master.kind,
                                        code: master.code
                                    )
                                    pendingCapture = "MATERIALS"
                                    sheet = nil
                                },
                                onNewBom: { sheet = .mtoForm(.bom) },
                                onDismiss: { sheet = nil }
                            )
                        } else {
                            MtoPartsBrowserSheet(
                                theme: theme,
                                model: model,
                                onAdd: { master in
                                    pendingBrowserPrefill = OperatingBrowserItem(
                                        id: master.id,
                                        name: master.name,
                                        meta: [master.code, master.unit].compactMap { $0 }.joined(separator: " · "),
                                        priceLabel: master.sellPrice,
                                        category: mtoPartCategory(master),
                                        unit: master.unit,
                                        unitPrice: master.sellPrice,
                                        masterKind: master.kind,
                                        code: master.code
                                    )
                                    pendingCapture = "QUOTATION"
                                    sheet = nil
                                },
                                onNewPart: { sheet = .mtoForm(.part) },
                                onDismiss: { sheet = nil }
                            )
                        }
                    } else {
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
                    }
                case let .mfgForm(kind):
                    MfgMasterFormSheet(theme: theme, kind: kind, model: model)
                case let .mtoForm(kind):
                    MtoMasterFormSheet(theme: theme, kind: kind, model: model)
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
                onOpenMasters: { tab in sheet = .masters(tab) },
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
                    // MTO Products & BOM / Lines & stores → dedicated masters (Figma MTO-MASTER).
                    // Parts/materials browsers stay reachable from New part / New BOM CTAs.
                    sheet = .masters(tab)
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
                    // MFG Parts & BOM → Products & BOM masters (Figma 2545:29477).
                    // Parts browser stays reachable from New part CTAs / SO·WO add paths.
                    sheet = .masters(tab)
                },
                onOpenStory: { sheet = .story }
            )
        }
    }

    @ViewBuilder
    private func captureSheet(_ code: String) -> some View {
        let upper = code.uppercased()
        let prefill = pendingBrowserPrefill
        if kit == .marketing, ["HOLD", "TRANSFER", "REPRINT"].contains(upper) {
            // Figma MKT-QA chrome-only — never posts; never Trading stock layouts.
            MarketingClientChromeOnlySheet(theme: theme, mode: upper, brand: model.title) { target in
                sheet = .capture(target)
            } onDismiss: {
                sheet = nil
            }
        } else if kit == .marketing, let mode = MarketingCaptureMode(rawValue: upper) {
            MarketingCaptureSheet(theme: theme, mode: mode, model: model) { target in
                sheet = .capture(target.rawValue)
            }
        } else if kit == .restaurant, upper == "POS" {
            RestaurantPosSheet(theme: theme, model: model)
        } else if kit == .restaurant, upper == "KOT" {
            RestaurantKotSheet(theme: theme, model: model)
        } else if kit == .restaurant, upper == "STATUS" {
            RestaurantStatusSheet(theme: theme, model: model)
        } else if kit == .restaurant, ["HOLD", "TRANSFER", "REPRINT"].contains(upper) {
            RestaurantUtilitySheet(theme: theme, mode: code, model: model) { target in
                sheet = .capture(target)
            }
        } else if kit == .restaurant,
                  RestaurantCaptureCopy.moneyModes.contains(upper)
                    || ["BUDGET", "TARGET", "ISSUE", "UPDATE", "APPROVAL"].contains(upper)
                    || upper == "ORDER"
        {
            RestaurantCaptureSheet(theme: theme, mode: code, model: model)
        } else if kit == .retail, upper == "SALE" {
            RetailPosSheet(theme: theme, model: model)
        } else if kit == .retail, [OperatingCaptureLayout.holdMode, OperatingCaptureLayout.transferMode, OperatingCaptureLayout.reprintMode].contains(upper) {
            RetailUtilitySheet(theme: theme, mode: code, model: model)
        } else if kit == .retail {
            RetailCaptureSheet(theme: theme, mode: code, model: model, browserPrefill: prefill)
                .onAppear { pendingBrowserPrefill = nil }
        } else if kit == .service, ["HOLD", "TRANSFER", "REPRINT"].contains(upper) {
            ServiceHoldTransferReprintSheet(theme: theme, mode: code, model: model) {
                sheet = nil
            }
        } else if kit == .service, upper == "SERVICES_BROWSER" {
            ServiceServicesBrowserSheet(
                theme: theme,
                model: model,
                onAdd: { item in
                    pendingBrowserPrefill = item
                    pendingCapture = "SERVICE"
                    sheet = nil
                },
                onDismiss: { sheet = nil }
            )
        } else if kit == .manufacturing, ["HOLD", "TRANSFER", "REPRINT"].contains(upper) {
            MfgUtilitySheet(theme: theme, mode: code, model: model) { target in
                sheet = .capture(target.uppercased())
            }
        } else if kit == .manufacturing, ["INVITE", "PERSON", "ROLE"].contains(upper) {
            MfgMastersSheet(theme: theme, tab: .invite, model: model)
        } else if kit == .manufacturing, upper == "SUPPLIER" {
            MfgMastersSheet(theme: theme, tab: .vendors, model: model)
        } else if kit == .manufacturing, upper == "PURCHASE" {
            // PO materials entry — Figma Materials browser before bare Purchase capture.
            MfgMaterialsBrowserSheet(
                theme: theme,
                model: model,
                onAdd: { _ in
                    pendingCapture = "PURCHASE_CAPTURE"
                    sheet = nil
                },
                onNewBom: { sheet = .mfgForm(.bom) },
                onDismiss: { sheet = nil }
            )
        } else if kit == .manufacturing, upper == "PURCHASE_CAPTURE" {
            MfgCaptureSheet(theme: theme, mode: "PURCHASE", model: model, browserPrefill: prefill) { target in
                sheet = .capture(target.uppercased())
            }
            .onAppear { pendingBrowserPrefill = nil }
        } else if kit == .manufacturing {
            MfgCaptureSheet(theme: theme, mode: code, model: model, browserPrefill: prefill) { target in
                sheet = .capture(target.uppercased())
            }
            .onAppear { pendingBrowserPrefill = nil }
        } else if kit == .madeToOrder, ["HOLD", "TRANSFER", "REPRINT"].contains(upper) {
            MtoUtilitySheet(theme: theme, mode: code, model: model) { target in
                sheet = .capture(target.uppercased())
            }
        } else if kit == .madeToOrder, ["INVITE", "PERSON", "ROLE"].contains(upper) {
            MtoMastersSheet(theme: theme, tab: .invite, model: model)
        } else if kit == .madeToOrder, ["SUPPLIER", "VENDORS", "PRINCIPALS"].contains(upper) {
            MtoMastersSheet(theme: theme, tab: .vendors, model: model)
        } else if kit == .madeToOrder {
            MtoCaptureSheet(theme: theme, mode: code, model: model, browserPrefill: prefill) { target in
                sheet = .capture(target.uppercased())
            }
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

    private func mfgMasterTab(from tab: OperatingMasterTab) -> MfgMasterListTab {
        switch tab {
        case .invite: return .invite
        case .principals: return .vendors
        case .catalogue: return .productsBom
        case .locations: return .linesStores
        case .priceLists: return .productsBom
        }
    }
}

/// The one sheet the host can show at a time.
private enum OperatingHostSheet: Identifiable {
    case hub
    case capture(String)
    case masters(OperatingMasterTab)
    case partsBrowser(OperatingBrowserKind)
    case mfgForm(MfgMasterFormKind)
    case mtoForm(MtoMasterFormKind)
    case story

    var id: String {
        switch self {
        case .hub: return "hub"
        case let .capture(code): return "capture:\(code)"
        case let .masters(tab): return "masters:\(tab.rawValue)"
        case let .partsBrowser(kind): return "browser:\(kind)"
        case let .mfgForm(kind): return "mfgForm:\(kind.rawValue)"
        case let .mtoForm(kind): return "mtoForm:\(kind.rawValue)"
        case .story: return "story"
        }
    }
}

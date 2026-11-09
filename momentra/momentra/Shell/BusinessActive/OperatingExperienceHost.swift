import SwiftUI

/// Mounts the Trading / Distribution experience for the selected business moment.
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

    var body: some View {
        OperatingActiveView(
            model: model,
            destination: destination,
            refreshToken: refreshToken,
            onOpenQuickAdd: { sheet = .quickAdd },
            onOpenStory: { sheet = .story },
            onOpenMasters: { sheet = .masters($0) }
        )
        .sheet(item: $sheet) { route in
            Group {
                switch route {
                case .quickAdd:
                    OperatingQuickAddHubView(theme: theme, model: model)
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
}

/// The one sheet the host can show at a time.
private enum OperatingHostSheet: Identifiable {
    case quickAdd
    case masters(OperatingMasterTab)
    case story

    var id: String {
        switch self {
        case .quickAdd: return "quickAdd"
        case let .masters(tab): return "masters:\(tab.rawValue)"
        case .story: return "story"
        }
    }
}

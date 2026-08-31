import Foundation

enum ShellVisibilityPolicy {
    static func showMomentSwitcher(
        context: AppContextKind,
        content: ShellContentState,
        destination: BottomDestination,
        activeMomentCount: Int,
        authReady: Bool
    ) -> Bool {
        guard authReady else { return false }
        guard context != .circle else { return false }
        guard destination != .create else { return false }
        switch content {
        case .ready, .empty: break
        default: return false
        }
        return activeMomentCount > 0 && (context == .personal || context == .group || context == .business)
    }

    static func showCompanySwitcher(context: AppContextKind, companies: [CompanySummary]) -> Bool {
        context == .business && !companies.isEmpty
    }
}

struct ShellInvariantInput {
    var supportedContexts: [AppContextKind]
    var selectedContext: AppContextKind
    var selectedCompanyId: String?
    var companies: [CompanySummary]
    var moments: [MomentSummary]
    var selectedMomentId: String?
    var selectedTabByContext: [AppContextKind: BottomDestination]
    var currentlySelectedContextDefault: AppContextKind = .personal
}

struct ShellInvariantResult {
    var selectedContext: AppContextKind
    var selectedCompanyId: String?
    var selectedMomentId: String?
    var moments: [MomentSummary]
    var selectedTabByContext: [AppContextKind: BottomDestination]
    var healed: Bool
}

enum ShellStateInvariants {
    static func heal(_ input: ShellInvariantInput) -> ShellInvariantResult {
        var healed = false
        let supported = input.supportedContexts.isEmpty ? [.personal] : input.supportedContexts

        var context = input.selectedContext
        if !supported.contains(context) {
            context = supported.contains(input.currentlySelectedContextDefault)
                ? input.currentlySelectedContextDefault
                : supported[0]
            healed = true
        }

        var companyId = input.selectedCompanyId
        if context != .business {
            if companyId != nil {
                companyId = nil
                healed = true
            }
        } else {
            let valid = Set(input.companies.map(\.companyId))
            if companyId == nil || !(valid.contains(companyId!)) {
                companyId = input.companies.first?.companyId
                healed = true
            }
        }

        let scoped: [MomentSummary]
        switch context {
        case .business:
            scoped = input.moments.filter { $0.companyId == nil || $0.companyId == companyId }
        case .circle:
            scoped = []
        default:
            scoped = input.moments
        }

        var momentId = input.selectedMomentId
        let momentOk = scoped.contains { $0.momentId == momentId }
        if momentId != nil && !momentOk {
            momentId = nil
            healed = true
        }
        if momentId == nil {
            momentId = scoped.first(where: \.isActiveStatus)?.momentId ?? scoped.first?.momentId
        }
        if context == .business, let mid = momentId,
           let m = scoped.first(where: { $0.momentId == mid }),
           let mc = m.companyId, mc != companyId {
            momentId = scoped.first(where: \.isActiveStatus)?.momentId
            healed = true
        }

        var tabs = input.selectedTabByContext.filter { supported.contains($0.key) }
        for c in supported where tabs[c] == nil {
            tabs[c] = .pulse
        }

        return ShellInvariantResult(
            selectedContext: context,
            selectedCompanyId: companyId,
            selectedMomentId: momentId,
            moments: scoped,
            selectedTabByContext: tabs,
            healed: healed
        )
    }
}

enum ShellScreenSlot {
    case loading, empty, error, offline, unauthorized, deferred, product, life360Global, profile
}

enum ShellScreenResolver {
    static func resolve(content: ShellContentState, life360Open: Bool, profileOpen: Bool) -> ShellScreenSlot {
        if profileOpen { return .profile }
        if life360Open { return .life360Global }
        switch content {
        case .loading, .idle: return .loading
        case .offline: return .offline
        case .forbidden: return .unauthorized
        case .error(let code, _):
            return code == "UNAUTHORIZED" ? .unauthorized : .error
        case .deferred: return .deferred
        case .empty: return .empty
        case .ready: return .product
        }
    }
}

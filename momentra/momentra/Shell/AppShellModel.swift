import Combine
import Foundation

@MainActor
final class AppShellModel: ObservableObject {
    @Published private(set) var identity: ShellIdentity?
    @Published var selectedContext: AppContextKind = .personal
    @Published private(set) var supportedContexts: [AppContextKind] = [.personal]
    @Published var selectedCompany: CompanySummary?
    @Published private(set) var companies: [CompanySummary] = []
    @Published var selectedMomentTitle: String?
    @Published var selectedMomentId: String?
    @Published var selectedMomentTypeCode: String?
    @Published var showMomentSwitcher = false
    @Published var bottomDestination: BottomDestination = .pulse
    private(set) var lastNonCreateDestination: BottomDestination = .moments
    @Published var companyMenuOpen = false
    @Published var life360Open = false
    @Published var profileOpen = false
    @Published private(set) var contextContent: ShellContentState = .idle
    @Published private(set) var momentExperience: MomentExperienceKind = .loading
    @Published private(set) var moments: [MomentSummary] = []
    @Published private(set) var personalTabRefreshToken: UInt64 = 0
    @Published private(set) var groupTabRefreshToken: UInt64 = 0
    @Published private(set) var businessTabRefreshToken: UInt64 = 0
    @Published private(set) var capabilities: [String] = []
    @Published private(set) var ttcsMs: Int64?

    private var tabByContext: [AppContextKind: BottomDestination] = [:]
    private var selectedMomentByContext: [AppContextKind: String?] = [:]
    private var generation: UInt64 = 0
    private var loadTask: Task<Void, Never>?
    private var bootstrap: ShellBootstrap?
    private var bindStartedAt: Date?
    private var bootstrapRefreshTask: Task<Void, Never>?
    private var groupPrefetchTask: Task<Void, Never>?
    private var businessPrefetchTask: Task<Void, Never>?
    private let gateway: ShellMeGatewaying

    init(gateway: ShellMeGatewaying? = nil) {
        self.gateway = gateway ?? ShellMeGateway()
    }

    func bindIdentity(_ identity: ShellIdentity) {
        bindStartedAt = Date()
        self.identity = identity
        if let cached = gateway.cachedBootstrap(userId: identity.userId) {
            bootstrap = cached
            applyBootstrapInventory(cached, networkRefresh: false)
            if let start = bindStartedAt {
                let ms = Int64(Date().timeIntervalSince(start) * 1000)
                ttcsMs = ms
                ShellPerf.instant("ttcs_cache_paint", extras: ["ttcsMs": ms])
            }
        }
        if bootstrap != nil, gateway.isBootstrapCacheFresh(userId: identity.userId, maxAgeMs: 30_000) {
            ensureContextContent()
            scheduleDeferredBootstrapRefresh()
        } else {
            refreshBootstrap()
        }
    }

    func clearForLogout() {
        loadTask?.cancel()
        bootstrapRefreshTask?.cancel()
        gateway.clearBootstrapCache(userId: identity?.userId)
        bootstrap = nil
        identity = nil
        selectedContext = .personal
        supportedContexts = [.personal]
        selectedCompany = nil
        companies = []
        selectedMomentTitle = nil
        selectedMomentId = nil
        selectedMomentTypeCode = nil
        showMomentSwitcher = false
        bottomDestination = .pulse
        lastNonCreateDestination = .moments
        tabByContext = [:]
        selectedMomentByContext = [:]
        companyMenuOpen = false
        life360Open = false
        profileOpen = false
        contextContent = .idle
        momentExperience = .loading
        moments = []
        capabilities = []
        generation = 0
        ttcsMs = nil
        PersonalTabDataCache.clear()
        GroupTabDataCache.clear()
        BusinessTabDataCache.clear()
        groupPrefetchTask?.cancel()
        businessPrefetchTask?.cancel()
        APIClient.shared.clearAuthTokenCache()
    }

    private func scheduleDeferredBootstrapRefresh(delaySeconds: Double = 15) {
        bootstrapRefreshTask?.cancel()
        bootstrapRefreshTask = Task {
            try? await Task.sleep(nanoseconds: UInt64(delaySeconds * 1_000_000_000))
            guard !Task.isCancelled else { return }
            refreshBootstrap()
        }
    }

    func selectContext(_ context: AppContextKind) {
        let mark = ShellPerf.start("context_switch")
        // Until bootstrap arrives, allow optimistic selection; heal enforces after merge.
        if bootstrap != nil && !supportedContexts.contains(context) { return }
        if selectedContext == context {
            switch context {
            case .personal: refreshVisiblePersonalTab()
            case .group: refreshVisibleGroupTab()
            case .business: refreshVisibleBusinessTab()
            case .circle: break
            }
            ShellPerf.end(mark, extras: ["sameContext": true, "context": "\(context)"])
            return
        }
        let previous = selectedContext
        tabByContext[previous] = bottomDestination
        selectedMomentByContext[previous] = selectedMomentId
        selectedContext = context
        bottomDestination = tabByContext[context] ?? .pulse
        selectedMomentId = selectedMomentByContext[context].flatMap { $0 }
        selectedMomentTitle = nil
        selectedMomentTypeCode = nil
        showMomentSwitcher = false
        companyMenuOpen = false
        moments = []
        momentExperience = .loading
        if context != .business {
            selectedCompany = nil
        }
        generation &+= 1
        contextContent = .loading
        ensureContextContent()
        ShellPerf.end(mark, extras: ["from": "\(previous)", "to": "\(context)"])
    }

    func openLife360(_ open: Bool = true) {
        life360Open = open
        if open { profileOpen = false }
    }

    func openProfile(_ open: Bool = true) {
        profileOpen = open
        if open { life360Open = false }
    }

    func reloadCurrentContext() {
        refreshBootstrap()
    }

    /// After leaving a Group/Business moment or company, drop selection and reload inventory.
    func clearSelectedMomentAfterLeave() {
        selectedMomentId = nil
        selectedMomentTitle = nil
        selectedMomentTypeCode = nil
        selectedMomentByContext[selectedContext] = nil
        momentExperience = .firstMoment
        contextContent = .loading
        refreshBootstrap()
    }

    private func refreshBootstrap() {
        loadTask?.cancel()
        loadTask = Task {
            let hadCache = bootstrap != nil
            if !hadCache {
                contextContent = .loading
                momentExperience = .loading
            }
            do {
                let boot = try await gateway.getBootstrap()
                guard !Task.isCancelled else { return }
                bootstrap = boot
                applyBootstrapInventory(boot, networkRefresh: true)
            } catch {
                guard !Task.isCancelled else { return }
                if bootstrap != nil {
                    ensureContextContent()
                } else {
                    applyError(generation, error)
                }
            }
        }
    }

    private func applyBootstrapInventory(
        _ boot: ShellBootstrap,
        networkRefresh: Bool,
        preserveMomentId: String? = nil
    ) {
        let previousMomentIds = moments.map(\.momentId)
        let previousSelection = selectedMomentId
        identity = boot.identity
        companies = boot.companies
        capabilities = boot.capabilities
        var rawMoments: [MomentSummary]
        switch selectedContext {
        case .personal: rawMoments = boot.personalMoments
        case .group: rawMoments = boot.groupMoments
        case .business: rawMoments = boot.businessMoments
        case .circle: rawMoments = []
        }
        let preferredMomentId = preserveMomentId
            ?? selectedMomentId
            ?? selectedMomentByContext[selectedContext].flatMap { $0 }
        if let preserve = preserveMomentId,
           !preserve.isEmpty,
           !rawMoments.contains(where: { $0.momentId == preserve }) {
            rawMoments.append(
                MomentSummary(
                    momentId: preserve,
                    title: (selectedMomentTitle?.isEmpty == false) ? (selectedMomentTitle ?? "Group") : "Group",
                    status: moments.first(where: { $0.momentId == preserve })?.status ?? "ACTIVE",
                    momentTypeCode: selectedMomentTypeCode
                )
            )
        }
        let healed = ShellStateInvariants.heal(
            ShellInvariantInput(
                supportedContexts: boot.supportedContexts.isEmpty
                    ? [.personal, .group, .business, .circle]
                    : boot.supportedContexts,
                selectedContext: selectedContext,
                selectedCompanyId: selectedCompany?.companyId ?? boot.selectedCompany?.companyId,
                companies: boot.companies,
                moments: rawMoments,
                selectedMomentId: preferredMomentId,
                selectedTabByContext: tabByContext,
                currentlySelectedContextDefault: boot.currentlySelectedContext
            )
        )
        supportedContexts = boot.supportedContexts.isEmpty
            ? [.personal, .group, .business, .circle]
            : boot.supportedContexts
        selectedContext = healed.selectedContext
        selectedCompany = boot.companies.first { $0.companyId == healed.selectedCompanyId }
        let previousById = Dictionary(uniqueKeysWithValues: moments.map { ($0.momentId, $0) })
        // Preserve known type codes when bootstrap omits them (legacy group inventory).
        moments = healed.moments.map { m in
            if let code = m.momentTypeCode, !code.isEmpty { return m }
            if let prev = previousById[m.momentId]?.momentTypeCode, !prev.isEmpty {
                return MomentSummary(
                    momentId: m.momentId,
                    title: m.title,
                    status: m.status,
                    momentTypeCode: prev,
                    companyId: m.companyId
                )
            }
            return m
        }
        selectedMomentId = healed.selectedMomentId
        let selected = moments.first { $0.momentId == healed.selectedMomentId }
        selectedMomentTitle = selected?.title
        selectedMomentTypeCode = selected?.momentTypeCode
        selectedMomentByContext[healed.selectedContext] = healed.selectedMomentId
        tabByContext = healed.selectedTabByContext
        bottomDestination = healed.selectedTabByContext[healed.selectedContext] ?? bottomDestination

        if healed.selectedContext == .circle {
            // S6: Circle Coming Soon — empty (not deferred); no Circle API fetch.
            momentExperience = .firstMoment
            contextContent = .empty
            showMomentSwitcher = false
        } else {
            let experience = resolveMomentExperience(healed.moments)
            momentExperience = experience
            switch experience {
            case .active: contextContent = .ready(detail: nil)
            case .firstMoment, .betweenMoments, .pausedOnly: contextContent = .empty
            case .loading, .error: contextContent = .loading
            }
            showMomentSwitcher = ShellVisibilityPolicy.showMomentSwitcher(
                context: healed.selectedContext,
                content: contextContent,
                destination: bottomDestination,
                activeMomentCount: activeMomentCount(healed.moments),
                authReady: true
            )
        }

        if networkRefresh, momentExperience == .active {
            let inventoryChanged = previousMomentIds != healed.moments.map(\.momentId)
            let selectionChanged = previousSelection != healed.selectedMomentId
            if inventoryChanged || selectionChanged {
                switch selectedContext {
                case .personal: refreshVisiblePersonalTab()
                case .group: refreshVisibleGroupTab()
                case .business: refreshVisibleBusinessTab()
                case .circle: break
                }
            }
        }
        if healed.selectedContext == .group,
           case .ready = contextContent,
           let momentId = healed.selectedMomentId,
           !momentId.isEmpty {
            prefetchGroupTabs(for: momentId)
        }
        if healed.selectedContext == .business,
           case .ready = contextContent,
           let momentId = healed.selectedMomentId,
           !momentId.isEmpty {
            prefetchBusinessTabs(for: momentId)
        }
    }

    func selectBottomDestination(_ destination: BottomDestination) {
        let mark = ShellPerf.start("tab_switch")
        let remembered: BottomDestination = {
            if destination != .create { return destination }
            if bottomDestination != .create { return bottomDestination }
            return lastNonCreateDestination == .create ? .moments : lastNonCreateDestination
        }()
        lastNonCreateDestination = remembered == .create ? .moments : remembered
        bottomDestination = destination
        tabByContext[selectedContext] = destination
        showMomentSwitcher = ShellVisibilityPolicy.showMomentSwitcher(
            context: selectedContext,
            content: contextContent,
            destination: destination,
            activeMomentCount: activeMomentCount(moments),
            authReady: true
        )
        if destination == .create {
            ShellPerf.instant("quick_add_presentation", extras: ["context": "\(selectedContext)"])
        }
        ShellPerf.end(mark, extras: ["destination": "\(destination)"])
    }

    func selectMoment(id: String) {
        let mark = ShellPerf.start("moment_switch")
        guard let moment = moments.first(where: { $0.momentId == id }) else { return }
        selectedMomentId = moment.momentId
        selectedMomentTitle = moment.title
        selectedMomentTypeCode = moment.momentTypeCode
        selectedMomentByContext[selectedContext] = moment.momentId
        if selectedContext == .personal {
            refreshVisiblePersonalTab()
        }
        if selectedContext == .group {
            refreshVisibleGroupTab()
        }
        if selectedContext == .business {
            refreshVisibleBusinessTab()
        }
        ShellPerf.end(mark, extras: ["momentId": String(id.prefix(8))])
    }

    func onMomentCreated(momentId: String, title: String, momentTypeCode: String? = nil, status: String = "ACTIVE") {
        selectedMomentId = momentId
        selectedMomentTitle = title
        selectedMomentTypeCode = momentTypeCode ?? selectedMomentTypeCode
        if let idx = moments.firstIndex(where: { $0.momentId == momentId }) {
            moments[idx] = MomentSummary(
                momentId: momentId,
                title: title,
                status: status,
                momentTypeCode: momentTypeCode ?? moments[idx].momentTypeCode,
                companyId: moments[idx].companyId
            )
        } else {
            moments.append(MomentSummary(momentId: momentId, title: title, status: status, momentTypeCode: momentTypeCode))
        }
        bottomDestination = .pulse
        lastNonCreateDestination = .pulse
        tabByContext[selectedContext] = .pulse
        selectedMomentByContext[selectedContext] = momentId
        reloadCurrentContext()
        if selectedContext == .group {
            refreshVisibleGroupTab()
        }
        if selectedContext == .business {
            refreshVisibleBusinessTab()
        }
    }

    func refreshVisiblePersonalTab() {
        personalTabRefreshToken &+= 1
        ShellPerf.instant("scoped_refresh_personal", extras: ["token": personalTabRefreshToken])
    }

    func refreshVisibleGroupTab() {
        prefetchGroupTabs(for: selectedMomentId)
        groupTabRefreshToken &+= 1
    }

    /// Warm pulse+finance+activity cache so Moments/Memory/Life paint without spinners.
    private func prefetchGroupTabs(for momentId: String?) {
        guard let momentId, !momentId.isEmpty else { return }
        groupPrefetchTask?.cancel()
        groupPrefetchTask = Task {
            await GroupTabPrefetch.run(momentId: momentId)
        }
    }

    func refreshVisibleBusinessTab() {
        prefetchBusinessTabs(for: selectedMomentId)
        businessTabRefreshToken &+= 1
    }

    /// Warm bundled pulse so Business tabs paint without spinners.
    private func prefetchBusinessTabs(for momentId: String?) {
        guard let momentId, !momentId.isEmpty else { return }
        businessPrefetchTask?.cancel()
        businessPrefetchTask = Task {
            await BusinessTabPrefetch.run(momentId: momentId)
        }
    }

    /// Redeem invite code then select the joined Moment on Pulse.
    /// Returns the redeem result so UI can show PENDING vs joined messaging.
    @discardableResult
    func redeemJoinCode(_ code: String, using createModel: MomentCreateModel) async -> RedeemGroupInviteResult? {
        guard let result = await createModel.redeemGroupInvite(code: code) else { return nil }
        guard let momentId = result.momentId, !momentId.isEmpty else {
            // PENDING claim — stay put; caller shows honest messaging.
            return result
        }
        if selectedContext != .group {
            selectContext(.group)
        }
        let title = moments.first(where: { $0.momentId == momentId })?.title
            ?? selectedMomentTitle
            ?? "Group Moment"
        selectedMomentId = momentId
        selectedMomentTitle = title
        bottomDestination = .pulse
        lastNonCreateDestination = .pulse
        tabByContext[.group] = .pulse
        selectedMomentByContext[.group] = momentId
        if !moments.contains(where: { $0.momentId == momentId }) {
            moments.append(MomentSummary(momentId: momentId, title: title, status: "ACTIVE"))
        }
        momentExperience = .active
        contextContent = .ready(detail: nil)

        var appeared = false
        for attempt in 0..<4 {
            do {
                let boot = try await gateway.getBootstrap()
                bootstrap = boot
                if boot.groupMoments.contains(where: { $0.momentId == momentId }) {
                    appeared = true
                }
                applyBootstrapInventory(boot, networkRefresh: true, preserveMomentId: momentId)
            } catch {
                break
            }
            if appeared { break }
            try? await Task.sleep(nanoseconds: UInt64(350_000_000 * (attempt + 1)))
        }
        refreshVisibleGroupTab()
        return result
    }

    /// Redeem company invite then select the joined company in Business.
    func redeemCompanyInviteCode(_ code: String, using createModel: MomentCreateModel) async {
        guard let result = await createModel.redeemCompanyInvite(code: code) else { return }
        if selectedContext != .business {
            selectContext(.business)
        }
        let companies = await createModel.listCompanies()
        self.companies = companies
        if let company = companies.first(where: { $0.companyId == result.companyId }) {
            onCompanyCreated(company)
        } else {
            onCompanyCreated(CompanySummary(companyId: result.companyId, displayName: "Company"))
        }
    }

    func exitCreateDestination() {
        selectBottomDestination(lastNonCreateDestination)
    }

    func toggleCompanyMenu(_ open: Bool? = nil) {
        companyMenuOpen = open ?? !companyMenuOpen
    }

    func selectCompany(_ company: CompanySummary?) {
        // Atomic company switch: clear invalid Moment, re-filter inventory by companyId, bump refresh.
        selectedCompany = company
        selectedMomentId = nil
        selectedMomentTitle = nil
        selectedMomentTypeCode = nil
        selectedMomentByContext[.business] = nil
        showMomentSwitcher = false
        companyMenuOpen = false
        moments = []
        generation &+= 1
        if company == nil {
            momentExperience = .firstMoment
            contextContent = .empty
        } else {
            // Heal scopes business moments to selectedCompany.companyId and may pick a valid Moment.
            ensureContextContent()
        }
        refreshVisibleBusinessTab()
    }

    func onCompanyCreated(_ company: CompanySummary) {
        if companies.contains(where: { $0.companyId == company.companyId }) {
            companies = [company] + companies.filter { $0.companyId != company.companyId }
        } else {
            companies = [company] + companies
        }
        selectedCompany = company
        selectedMomentId = nil
        selectedMomentTitle = nil
        selectedMomentTypeCode = nil
        selectedMomentByContext[.business] = nil
        bottomDestination = .create
        contextContent = .empty
        momentExperience = .firstMoment
        companyMenuOpen = false
        refreshVisibleBusinessTab()
        Task { _ = try? await gateway.getBootstrap() }
    }

    private func ensureContextContent() {
        guard let boot = bootstrap else {
            contextContent = .loading
            momentExperience = .loading
            return
        }
        applyBootstrapInventory(boot, networkRefresh: false)
    }

    private func applyError(_ gen: UInt64, _ error: Error) {
        guard generation == gen else { return }
        momentExperience = .error
        showMomentSwitcher = false
        moments = []
        if let kind = error as? APIErrorKind {
            switch kind {
            case .network:
                contextContent = .offline
            case .forbidden:
                contextContent = .forbidden
            case .unauthenticated(let code):
                contextContent = .error(code: code, message: "Unauthorized")
            default:
                contextContent = .error(code: nil, message: String(describing: kind))
            }
        } else {
            contextContent = .error(code: nil, message: error.localizedDescription)
        }
    }
}

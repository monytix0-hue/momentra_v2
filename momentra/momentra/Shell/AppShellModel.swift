import Combine
import Foundation
import Network

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
    @Published private(set) var offlineNotice: String?
    /// Group inventory empty of ACTIVE/DRAFT but user has COMPLETED membership.
    @Published private(set) var groupHasCompletedHistory = false

    private var tabByContext: [AppContextKind: BottomDestination] = [:]
    private var selectedMomentByContext: [AppContextKind: String?] = [:]
    private var generation: UInt64 = 0
    private var loadTask: Task<Void, Never>?
    private var bootstrap: ShellBootstrap?
    private var preferredPersonalMomentId: String?
    private var personalDeepLinkHold = false
    private var bindStartedAt: Date?
    private var bootstrapRefreshTask: Task<Void, Never>?
    private var groupPrefetchTask: Task<Void, Never>?
    private var businessPrefetchTask: Task<Void, Never>?
    private var sseDebounceTask: Task<Void, Never>?
    private let pathMonitor = NWPathMonitor()
    private var pathMonitorStarted = false
    private let gateway: ShellMeGatewaying

    var isPersonalDeepLinkHold: Bool { personalDeepLinkHold }

    func clearPersonalDeepLinkHold() {
        personalDeepLinkHold = false
    }

    func applyPreferredPersonalLock() {
        guard selectedContext == .personal else { return }
        guard !personalDeepLinkHold else { return }
        guard let preferred = PersonalUnified.resolvePreferred(
            moments: moments,
            currentSelectedId: selectedMomentId
        ) else { return }
        guard preferred.momentId != selectedMomentId else { return }
        preferredPersonalMomentId = preferred.momentId
        selectMoment(id: preferred.momentId)
    }

    init(gateway: ShellMeGatewaying? = nil) {
        self.gateway = gateway ?? ShellMeGateway()
    }

    func bindIdentity(_ identity: ShellIdentity) {
        bindStartedAt = Date()
        self.identity = identity
        OfflineOutbox.shared.bindUser(identity.userId)
        OfflineOutbox.shared.onChanged = { [weak self] in
            Task { @MainActor in
                self?.publishOfflineNotice(networkDown: false)
            }
        }
        startOfflineMonitor()
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
        startRealtime()
    }

    func clearForLogout() {
        stopRealtime()
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

    private func startRealtime() {
        stopRealtime()
        SseClient.shared.connect { [weak self] event in
            Task { @MainActor in
                self?.scheduleProjectionRefresh(event)
            }
        }
    }

    private func stopRealtime() {
        sseDebounceTask?.cancel()
        sseDebounceTask = nil
        SseClient.shared.disconnect()
    }

    private func scheduleProjectionRefresh(_ event: ProjectionUpdatedEvent) {
        sseDebounceTask?.cancel()
        sseDebounceTask = Task {
            try? await Task.sleep(nanoseconds: 400_000_000)
            guard !Task.isCancelled else { return }
            onProjectionUpdated(event)
        }
    }

    private func onProjectionUpdated(_ event: ProjectionUpdatedEvent) {
        ShellPerf.instant(
            "sse_projection_updated",
            extras: [
                "scopeType": event.scopeType ?? "",
                "scopeId": String((event.scopeId ?? "").prefix(8)),
            ]
        )
        if let scopeId = event.scopeId, !scopeId.isEmpty, scopeId == selectedMomentId {
            switch selectedContext {
            case .personal: refreshVisiblePersonalTab()
            case .group: refreshVisibleGroupTab()
            case .business: refreshVisibleBusinessTab()
            case .circle: break
            }
            return
        }
        // Off-scope: skip full bootstrap; soft-defer inventory refresh for moment/company events.
        let scopeType = (event.scopeType ?? "").uppercased()
        if scopeType == "MOMENT" || scopeType == "COMPANY" {
            ShellPerf.instant(
                "sse_off_scope_soft",
                extras: [
                    "scopeType": scopeType,
                    "scopeId": String((event.scopeId ?? "").prefix(8)),
                ]
            )
            scheduleDeferredBootstrapRefresh()
            return
        }
        reloadCurrentContext()
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
            case .personal:
                if !personalDeepLinkHold { applyPreferredPersonalLock() }
                refreshVisiblePersonalTab()
            case .group: refreshVisibleGroupTab()
            case .business: refreshVisibleBusinessTab()
            case .circle: break
            }
            ShellPerf.end(mark, extras: ["sameContext": true, "context": "\(context)"])
            return
        }
        let previous = selectedContext
        if previous == .personal {
            personalDeepLinkHold = false
        }
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
                publishOfflineNotice(networkDown: false)
            } catch {
                guard !Task.isCancelled else { return }
                if bootstrap != nil {
                    ensureContextContent()
                    publishOfflineNotice(networkDown: {
                        if case .network = error as? APIErrorKind { return true }
                        return false
                    }())
                } else {
                    applyError(generation, error)
                }
            }
        }
    }

    /// A missing moment stays in this context only when its type belongs here. Blank types stay for optimistic creates.
    private func momentTypeFits(_ context: AppContextKind, _ momentTypeCode: String?) -> Bool {
        let code = (momentTypeCode ?? "").trimmingCharacters(in: .whitespacesAndNewlines).uppercased()
        if code.isEmpty { return context != .circle }
        let personal = code == "LIFE_RHYTHM"
            || code == "LIFE_OPERATIONS"
            || code == "LIFESTYLE"
            || code == "FUTURE_GOAL"
            || code == "FUTURE_BUILDING"
            || code == "RELATIONSHIP_CONNECTION"
            || code == "RELATIONSHIPS"
            || code.hasPrefix("LIFE_")
            || code.hasPrefix("FUTURE_")
            || code.hasPrefix("LIFESTYLE")
            || code.hasPrefix("RELATIONSHIP")
        let business = code == "TEAM_OPERATIONS"
            || code == "BUSINESS_RUNWAY"
            || code == "BUSINESS_OPERATIONS"
        switch context {
        case .personal: return personal
        case .group: return !personal && !business
        case .business: return business
        case .circle: return false
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
        let preferredMomentId: String?
        if let preserveMomentId {
            preferredMomentId = preserveMomentId
        } else if selectedContext == .personal && !personalDeepLinkHold {
            preferredMomentId = PersonalUnified.resolvePreferred(
                moments: rawMoments,
                currentSelectedId: selectedMomentId
                    ?? selectedMomentByContext[.personal].flatMap { $0 }
                    ?? preferredPersonalMomentId
            )?.momentId
                ?? selectedMomentId
                ?? selectedMomentByContext[.personal].flatMap { $0 }
                ?? preferredPersonalMomentId
        } else {
            preferredMomentId = selectedMomentId
                ?? selectedMomentByContext[selectedContext].flatMap { $0 }
                ?? (selectedContext == .personal ? preferredPersonalMomentId : nil)
        }
        if let preserve = preserveMomentId,
           !preserve.isEmpty,
           !rawMoments.contains(where: { $0.momentId == preserve }),
           momentTypeFits(
               selectedContext,
               moments.first(where: { $0.momentId == preserve })?.momentTypeCode ?? selectedMomentTypeCode
           ) {
            let existing = moments.first(where: { $0.momentId == preserve })
            rawMoments.append(
                MomentSummary(
                    momentId: preserve,
                    title: existing?.title
                        ?? ((selectedMomentTitle?.isEmpty == false) ? (selectedMomentTitle ?? "Group") : "Group"),
                    status: existing?.status ?? "ACTIVE",
                    momentTypeCode: existing?.momentTypeCode ?? selectedMomentTypeCode,
                    participantCount: existing?.participantCount ?? 0
                )
            )
        } else if let preferred = preferredMomentId,
                  !rawMoments.contains(where: { $0.momentId == preferred }),
                  let preserved = moments.first(where: { $0.momentId == preferred }),
                  preserved.isCompletedStatus || selectedContext == .group,
                  momentTypeFits(selectedContext, preserved.momentTypeCode) {
            rawMoments.append(preserved)
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
                    companyId: m.companyId,
                    participantCount: m.participantCount
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
            groupHasCompletedHistory = false
        } else {
            // Personal moments are active by default; never land on the empty shell.
            let experience = healed.selectedContext == .personal
                ? MomentExperienceKind.active
                : resolveMomentExperience(healed.moments)
            momentExperience = experience
            switch experience {
            case .active: contextContent = .ready(detail: nil)
            case .firstMoment, .betweenMoments, .pausedOnly: contextContent = .empty
            case .loading, .error: contextContent = .loading
            }
            let switcherCount = max(
                activeMomentCount(healed.moments),
                (selected?.isCompletedStatus == true) ? 1 : 0
            )
            showMomentSwitcher = ShellVisibilityPolicy.showMomentSwitcher(
                context: healed.selectedContext,
                content: contextContent,
                destination: bottomDestination,
                activeMomentCount: switcherCount,
                authReady: true
            )
            if healed.selectedContext != .group {
                groupHasCompletedHistory = false
            }
        }

        if healed.selectedContext == .group, momentExperience == .firstMoment, networkRefresh {
            Task { await probeGroupCompletedHistory() }
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
            refreshVisibleGroupTab(forcePrefetch: true)
        }
        if selectedContext == .business {
            refreshVisibleBusinessTab(forcePrefetch: true)
        }
        ShellPerf.end(mark, extras: ["momentId": String(id.prefix(8))])
    }

    private func probeGroupCompletedHistory() async {
        let hasCompleted = ((try? await gateway.listGroupMoments(limit: 1, lifecycle: "completed")) ?? []).isEmpty == false
        guard selectedContext == .group else { return }
        if momentExperience != .firstMoment && momentExperience != .betweenMoments {
            groupHasCompletedHistory = hasCompleted
            return
        }
        guard hasCompleted else {
            groupHasCompletedHistory = false
            return
        }
        groupHasCompletedHistory = true
        momentExperience = .betweenMoments
        contextContent = .empty
        showMomentSwitcher = true
    }

    /// Open a COMPLETED Group moment into the live shell so members can settle expenses.
    func selectCompletedGroupMoment(_ moment: MomentSummary) {
        let status = moment.status.isEmpty ? "COMPLETED" : moment.status
        let summary = MomentSummary(
            momentId: moment.momentId,
            title: moment.title,
            status: status,
            momentTypeCode: moment.momentTypeCode,
            companyId: moment.companyId,
            participantCount: moment.participantCount
        )
        let baseMoments = selectedContext == .group ? moments : []
        if let idx = baseMoments.firstIndex(where: { $0.momentId == summary.momentId }) {
            var next = baseMoments
            next[idx] = summary
            moments = next
        } else {
            moments = baseMoments + [summary]
        }
        selectedContext = .group
        selectedMomentId = summary.momentId
        selectedMomentTitle = summary.title
        selectedMomentTypeCode = summary.momentTypeCode
        selectedMomentByContext[.group] = summary.momentId
        bottomDestination = .pulse
        lastNonCreateDestination = .pulse
        tabByContext[.group] = .pulse
        momentExperience = .active
        contextContent = .ready(detail: nil)
        showMomentSwitcher = true
        Task {
            do {
                let boot = try await gateway.getBootstrap()
                bootstrap = boot
                applyBootstrapInventory(boot, networkRefresh: true, preserveMomentId: summary.momentId)
            } catch {
                // Keep optimistic completed selection if bootstrap fails.
            }
            refreshVisibleGroupTab(forcePrefetch: true)
        }
    }

    /// Opens a moment from a push/inbox deep link, switching Personal/Group/Business if needed.
    /// Returns false when bootstrap inventory is not ready or the moment is not found yet
    /// (caller should keep pending deep link / retry). Kicks off an async completed-inventory lookup.
    @discardableResult
    func openMomentFromDeepLink(momentId: String) -> Bool {
        if moments.contains(where: { $0.momentId == momentId }) {
            if selectedContext == .personal {
                personalDeepLinkHold = true
            }
            selectMoment(id: momentId)
            return true
        }
        if let boot = bootstrap {
            let candidates: [(AppContextKind, [MomentSummary])] = [
                (.group, boot.groupMoments),
                (.business, boot.businessMoments),
                (.personal, boot.personalMoments),
            ]
            for (ctx, list) in candidates {
                guard list.contains(where: { $0.momentId == momentId }) else { continue }
                if ctx == .personal {
                    personalDeepLinkHold = true
                }
                selectedMomentByContext[ctx] = momentId
                if selectedContext != ctx {
                    selectContext(ctx)
                } else {
                    ensureContextContent()
                }
                if moments.contains(where: { $0.momentId == momentId }) {
                    selectMoment(id: momentId)
                    return true
                }
                return selectedMomentId == momentId
            }
        }
        Task {
            await resolveDeepLinkFromCompletedGroup(momentId: momentId)
        }
        return false
    }

    @discardableResult
    private func resolveDeepLinkFromCompletedGroup(momentId: String) async -> Bool {
        if moments.contains(where: { $0.momentId == momentId }) {
            selectMoment(id: momentId)
            return true
        }
        guard let hit = try? await gateway.listGroupMoments(limit: 50, lifecycle: "completed")
            .first(where: { $0.momentId == momentId }) else {
            return false
        }
        selectCompletedGroupMoment(hit)
        return true
    }

    func onMomentCreated(momentId: String, title: String, momentTypeCode: String? = nil, status: String = "ACTIVE") {
        if momentId == OfflineOutbox.localId { return }
        selectedMomentId = momentId
        selectedMomentTitle = title
        selectedMomentTypeCode = momentTypeCode ?? selectedMomentTypeCode
        if let idx = moments.firstIndex(where: { $0.momentId == momentId }) {
            moments[idx] = MomentSummary(
                momentId: momentId,
                title: title,
                status: status,
                momentTypeCode: momentTypeCode ?? moments[idx].momentTypeCode,
                companyId: moments[idx].companyId,
                participantCount: moments[idx].participantCount
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

    func flushOfflineQueue() {
        guard identity != nil else { return }
        Task {
            let synced = await OfflineOutbox.shared.flush()
            let refreshRoot = OfflineOutbox.shared.takeBootstrapRefresh()
            publishOfflineNotice(networkDown: false)
            if refreshRoot {
                refreshBootstrap()
            }
            guard synced > 0 else { return }
            switch selectedContext {
            case .personal: refreshVisiblePersonalTab()
            case .group: refreshVisibleGroupTab(forcePrefetch: true)
            case .business: refreshVisibleBusinessTab(forcePrefetch: true)
            case .circle: break
            }
        }
    }

    private func startOfflineMonitor() {
        guard !pathMonitorStarted else { return }
        pathMonitorStarted = true
        pathMonitor.pathUpdateHandler = { [weak self] path in
            guard path.status == .satisfied else { return }
            Task { @MainActor in
                self?.flushOfflineQueue()
            }
        }
        pathMonitor.start(queue: DispatchQueue(label: "momentra.offline-sync"))
    }

    private func publishOfflineNotice(networkDown: Bool) {
        let failed = OfflineOutbox.shared.failedMessage()
        let pending = OfflineOutbox.shared.pendingCount()
        if let failed {
            offlineNotice = failed
        } else if pending > 0 {
            offlineNotice = "Saved on this device. Changes will sync when you reconnect."
        } else if networkDown {
            offlineNotice = "You're offline. Saved changes will sync when you reconnect."
        } else {
            offlineNotice = nil
        }
    }

    func refreshVisiblePersonalTab() {
        personalTabRefreshToken &+= 1
        ShellPerf.instant("scoped_refresh_personal", extras: ["token": personalTabRefreshToken])
    }

    func refreshVisibleGroupTab(forcePrefetch: Bool = false) {
        let warm = selectedMomentId.map { GroupTabDataCache.peekPulse($0) != nil } ?? false
        if forcePrefetch || !warm {
            prefetchGroupTabs(for: selectedMomentId)
        }
        groupTabRefreshToken &+= 1
        ShellPerf.instant(
            "scoped_refresh_group",
            extras: [
                "token": groupTabRefreshToken,
                "warm": warm,
                "prefetch": forcePrefetch || !warm,
            ]
        )
    }

    /// Warm pulse+finance+activity cache so Moments/Memory/Life paint without spinners.
    private func prefetchGroupTabs(for momentId: String?) {
        guard let momentId, !momentId.isEmpty else { return }
        groupPrefetchTask?.cancel()
        groupPrefetchTask = Task {
            await GroupTabPrefetch.run(momentId: momentId)
        }
    }

    func refreshVisibleBusinessTab(forcePrefetch: Bool = false) {
        let warm = selectedMomentId.map { BusinessTabDataCache.peekPulse($0) != nil } ?? false
        if forcePrefetch || !warm {
            prefetchBusinessTabs(for: selectedMomentId)
        }
        businessTabRefreshToken &+= 1
        ShellPerf.instant(
            "scoped_refresh_business",
            extras: [
                "token": businessTabRefreshToken,
                "warm": warm,
                "prefetch": forcePrefetch || !warm,
            ]
        )
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
    /// Throws when the API rejects the invite so the sheet can show an error.
    @discardableResult
    func redeemJoinCode(_ code: String, using createModel: MomentCreateModel) async throws -> RedeemGroupInviteResult {
        let result = try await createModel.redeemGroupInvite(code: code)
        if result.momentId == OfflineOutbox.localId { return result }
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
    @discardableResult
    func redeemCompanyInviteCode(_ code: String, using createModel: MomentCreateModel) async -> Bool {
        guard let result = await createModel.redeemCompanyInvite(code: code) else { return false }
        if result.companyId == OfflineOutbox.localId { return true }
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
        return true
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

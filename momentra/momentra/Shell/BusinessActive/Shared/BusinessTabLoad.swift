import Foundation

enum BusinessTabLoad {
    private final class Gate: @unchecked Sendable {
        private let lock = NSLock()
        private var tasks: [String: Task<BusinessTabDataCache.PulseTab, Error>] = [:]
        private var generation: [String: Int] = [:]

        func invalidate(momentId: String) {
            lock.lock()
            generation[momentId, default: 0] += 1
            tasks[momentId]?.cancel()
            tasks.removeValue(forKey: momentId)
            lock.unlock()
            BusinessTabDataCache.invalidateMoment(momentId)
        }

        func currentGeneration(_ momentId: String) -> Int {
            lock.lock()
            defer { lock.unlock() }
            return generation[momentId] ?? 0
        }

        func begin(
            momentId: String,
            create: () -> Task<BusinessTabDataCache.PulseTab, Error>
        ) -> (task: Task<BusinessTabDataCache.PulseTab, Error>, owner: Bool, generation: Int) {
            lock.lock()
            defer { lock.unlock() }
            let generation = generation[momentId] ?? 0
            if let existing = tasks[momentId] {
                return (existing, false, generation)
            }
            let task = create()
            tasks[momentId] = task
            return (task, true, generation)
        }

        func end(momentId: String) {
            lock.lock()
            tasks.removeValue(forKey: momentId)
            lock.unlock()
        }
    }

    private static let gate = Gate()

    static func loadFamily(for momentTypeCode: String?) -> BusinessMomentFamily {
        let code = (momentTypeCode ?? "").uppercased()
        if code.contains("RUNWAY") { return .money }
        if code.contains("OPERATIONS") && !code.contains("TEAM") { return .daily }
        return .team
    }

    /// Invalidate cache and drop in-flight work before a refresh token bump.
    static func invalidate(momentId: String) {
        gate.invalidate(momentId: momentId)
    }

    static func loadPulseTab(
        momentId: String,
        fetchTeamOpsMetrics: Bool = false,
        family: BusinessMomentFamily = .team
    ) async throws -> BusinessTabDataCache.PulseTab {
        let owned = gate.begin(momentId: momentId) {
            Task { try await fetchPulseTab(momentId: momentId, family: family) }
        }
        do {
            var tab = try await owned.task.value
            if gate.currentGeneration(momentId) != owned.generation {
                throw CancellationError()
            }
            if fetchTeamOpsMetrics {
                tab = await enrichTeamOps(momentId: momentId, tab: tab)
            }
            if owned.owner {
                gate.end(momentId: momentId)
            }
            return tab
        } catch {
            if owned.owner {
                gate.end(momentId: momentId)
            }
            throw error
        }
    }

    private static func fetchPulseTab(
        momentId: String,
        family: BusinessMomentFamily
    ) async throws -> BusinessTabDataCache.PulseTab {
        let mark = ShellPerf.start("pulse_tab_ready")
        let started = gate.currentGeneration(momentId)
        let lifeTask: Task<APIClient.BusinessLifePayload, Error>? = family == .money
            ? Task { try await APIClient.shared.getBusinessLife(momentId: momentId) }
            : nil
        let pulse = try await APIClient.shared.getBusinessPulse(momentId: momentId)
        let activities: [APIClient.ActivityItemPayload]
        if let bundled = pulse.payload?.activity {
            activities = bundled
        } else {
            activities = try await APIClient.shared.listBusinessActivity(
                momentId: momentId,
                limit: BusinessTabPrefetch.activityLimit
            )
        }
        var life: APIClient.BusinessLifePayload?
        var lifeFailed = false
        var approvals: [BusinessTabDataCache.PulseNamedRow]?
        var issues: [BusinessTabDataCache.PulseNamedRow]?
        var rosterCount: Int?
        if family == .money {
            if let lifeTask {
                do {
                    life = try await lifeTask.value
                } catch {
                    lifeFailed = true
                }
            }
            approvals = try? await approvalRows(momentId)
        } else if family == .daily {
            issues = try? await issueRows(momentId)
        } else {
            async let approvalTask = approvalRows(momentId)
            async let issueTask = issueRows(momentId)
            async let rosterTask = APIClient.shared.getBusinessRoster(momentId: momentId)
            approvals = try? await approvalTask
            issues = try? await issueTask
            if let roster = try? await rosterTask {
                rosterCount = roster.members.count
            }
        }
        let ops = pulse.payload?.operations
        let tab = BusinessTabDataCache.PulseTab(
            pulse: pulse,
            finance: pulse.payload?.finance,
            life: life,
            activities: activities,
            businessFamily: pulse.businessFamily,
            facetStatus: pulse.status,
            capacity: nil,
            workload: nil,
            approvalTitles: approvals,
            issueTitles: issues,
            needsAttention: (ops?.needsAttention ?? []).map {
                BusinessTabDataCache.PulseNamedRow(id: $0.issueId, title: $0.title)
            },
            rosterCount: rosterCount,
            lifeFailed: lifeFailed
        )
        if gate.currentGeneration(momentId) == started {
            BusinessTabDataCache.putPulse(momentId, tab)
        }
        ShellPerf.end(mark, extras: ["context": "BUSINESS", "bundled": true])
        return tab
    }

    private static func approvalRows(_ momentId: String) async throws -> [BusinessTabDataCache.PulseNamedRow] {
        let body = try await APIClient.shared.listBusinessApprovals(momentId: momentId)
        return body.items.map {
            BusinessTabDataCache.PulseNamedRow(
                id: $0.approvalRequestId,
                title: ($0.title?.isEmpty == false) ? $0.title! : "Approval waiting"
            )
        }
    }

    private static func issueRows(_ momentId: String) async throws -> [BusinessTabDataCache.PulseNamedRow] {
        let body = try await APIClient.shared.listBusinessIssues(momentId: momentId)
        return body.items.compactMap { item in
            let status = item.status?.uppercased()
            if let status, status != "OPEN", status != "IN_PROGRESS", status != "BLOCKED" {
                return nil
            }
            return BusinessTabDataCache.PulseNamedRow(id: item.issueId, title: item.title)
        }
    }

    private static func enrichTeamOps(
        momentId: String,
        tab: BusinessTabDataCache.PulseTab
    ) async -> BusinessTabDataCache.PulseTab {
        if tab.capacity != nil && tab.workload != nil { return tab }
        async let capTask = APIClient.shared.getBusinessCapacity(momentId: momentId)
        async let workTask = APIClient.shared.getBusinessWorkload(momentId: momentId)
        let enriched = BusinessTabDataCache.PulseTab(
            pulse: tab.pulse,
            finance: tab.finance,
            life: tab.life,
            activities: tab.activities,
            businessFamily: tab.businessFamily,
            facetStatus: tab.facetStatus,
            capacity: (try? await capTask) ?? tab.capacity,
            workload: (try? await workTask) ?? tab.workload,
            approvalTitles: tab.approvalTitles,
            issueTitles: tab.issueTitles,
            needsAttention: tab.needsAttention,
            rosterCount: tab.rosterCount,
            lifeFailed: tab.lifeFailed
        )
        BusinessTabDataCache.putPulse(momentId, enriched)
        return enriched
    }

    static func loadMemoryTab(momentId: String) async throws -> BusinessTabDataCache.MemoryTab {
        let cached = BusinessTabDataCache.peekPulse(momentId)
        async let memoryTask = APIClient.shared.getBusinessMemory(momentId: momentId)
        let memory = try await memoryTask
        let pulse: APIClient.BusinessPulsePayload?
        let finance: APIClient.BusinessFinancePayload?
        if let cachedPulse = cached?.pulse {
            pulse = cachedPulse
            finance = cached?.finance ?? cachedPulse.payload?.finance
        } else {
            let loaded = try await APIClient.shared.getBusinessPulse(momentId: momentId)
            pulse = loaded
            finance = loaded.payload?.finance
        }
        let tab = BusinessTabDataCache.MemoryTab(
            memory: memory,
            pulse: pulse,
            finance: finance,
            life: cached?.life
        )
        BusinessTabDataCache.putMemory(momentId, tab)
        return tab
    }
}

import Foundation

enum BusinessTabLoad {
    private static let lock = NSLock()
    private static var inflight: [String: Task<BusinessTabDataCache.PulseTab, Error>] = [:]

    /// Personal-parity pulse load: one bundled /pulse GET (finance + activity preview).
    /// Team Ops capacity/workload are a cheap follow-up and do not block first paint.
    static func loadPulseTab(
        momentId: String,
        fetchTeamOpsMetrics: Bool = false
    ) async throws -> BusinessTabDataCache.PulseTab {
        let owned: (task: Task<BusinessTabDataCache.PulseTab, Error>, owner: Bool) = {
            lock.lock()
            defer { lock.unlock() }
            if let existing = inflight[momentId] { return (existing, false) }
            let task = Task { try await fetchPulseTab(momentId: momentId) }
            inflight[momentId] = task
            return (task, true)
        }()
        defer {
            if owned.owner {
                lock.lock()
                inflight.removeValue(forKey: momentId)
                lock.unlock()
            }
        }
        var tab = try await owned.task.value
        if fetchTeamOpsMetrics {
            tab = await enrichTeamOps(momentId: momentId, tab: tab)
        }
        return tab
    }

    private static func fetchPulseTab(momentId: String) async throws -> BusinessTabDataCache.PulseTab {
        let mark = ShellPerf.start("pulse_tab_ready")
        let pulse = try await APIClient.shared.getBusinessPulse(momentId: momentId)
        let finance = pulse.payload?.finance
        let activities: [APIClient.ActivityItemPayload]
        if let bundled = pulse.payload?.activity {
            activities = bundled
        } else {
            activities = try await APIClient.shared.listBusinessActivity(
                momentId: momentId,
                limit: BusinessTabPrefetch.activityLimit
            )
        }
        let previous = BusinessTabDataCache.peekPulse(momentId)
        let tab = BusinessTabDataCache.PulseTab(
            pulse: pulse,
            finance: finance ?? previous?.finance,
            life: previous?.life,
            activities: activities,
            businessFamily: pulse.businessFamily,
            facetStatus: pulse.status,
            capacity: previous?.capacity,
            workload: previous?.workload
        )
        BusinessTabDataCache.putPulse(momentId, tab)
        ShellPerf.end(mark, extras: ["context": "BUSINESS", "bundled": true])
        return tab
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
            workload: (try? await workTask) ?? tab.workload
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

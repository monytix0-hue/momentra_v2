import Foundation

/// In-memory SWR cache for Business tab datasets keyed by momentId.
enum BusinessTabDataCache {
    struct PulseTab {
        let pulse: APIClient.BusinessPulsePayload
        let finance: APIClient.BusinessFinancePayload?
        let life: APIClient.BusinessLifePayload?
        let activities: [APIClient.ActivityItemPayload]
        let businessFamily: String?
        let facetStatus: String?
        let capacity: APIClient.BusinessCapacityPayload?
        let workload: APIClient.BusinessWorkloadPayload?
    }

    struct MemoryTab {
        let memory: APIClient.BusinessMemoryPayload?
        let pulse: APIClient.BusinessPulsePayload?
        let finance: APIClient.BusinessFinancePayload?
        let life: APIClient.BusinessLifePayload?
    }

    private static var pulseByMoment: [String: PulseTab] = [:]
    private static var memoryByMoment: [String: MemoryTab] = [:]

    static func peekPulse(_ momentId: String) -> PulseTab? {
        pulseByMoment[momentId]
    }

    static func putPulse(_ momentId: String, _ tab: PulseTab) {
        pulseByMoment[momentId] = tab
    }

    static func peekMemory(_ momentId: String) -> MemoryTab? {
        memoryByMoment[momentId]
    }

    static func putMemory(_ momentId: String, _ tab: MemoryTab) {
        memoryByMoment[momentId] = tab
    }

    /// Merge life facet into warm pulse cache so Pulse/Moments can reuse it.
    static func putLife(_ momentId: String, _ life: APIClient.BusinessLifePayload) {
        guard let previous = pulseByMoment[momentId] else { return }
        putPulse(momentId, PulseTab(
            pulse: previous.pulse,
            finance: previous.finance,
            life: life,
            activities: previous.activities,
            businessFamily: previous.businessFamily,
            facetStatus: previous.facetStatus,
            capacity: previous.capacity,
            workload: previous.workload
        ))
        if let mem = memoryByMoment[momentId] {
            putMemory(momentId, MemoryTab(
                memory: mem.memory,
                pulse: mem.pulse,
                finance: mem.finance,
                life: life
            ))
        }
    }

    /// Drop cached facets for one moment after a write so SWR reloads fresh data.
    static func invalidateMoment(_ momentId: String) {
        pulseByMoment.removeValue(forKey: momentId)
        memoryByMoment.removeValue(forKey: momentId)
    }

    static func clear() {
        pulseByMoment.removeAll()
        memoryByMoment.removeAll()
    }
}

enum BusinessTabPrefetch {
    static let activityLimit = 5

    /// Warm bundled pulse (finance + activity preview) so Business tabs paint without spinners.
    static func run(momentId: String) async {
        guard !momentId.isEmpty else { return }
        do {
            _ = try await BusinessTabLoad.loadPulseTab(momentId: momentId)
        } catch {
            // Prefetch is best-effort; visible tabs retry on their own .task.
        }
    }
}

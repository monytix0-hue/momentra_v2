import Foundation

/// In-memory SWR cache for Group tab datasets keyed by momentId.
enum GroupTabDataCache {
    struct PulseTab {
        let title: String?
        let pulse: APIClient.GroupPulsePayload?
        let finance: APIClient.GroupFinancePayload?
        let activities: [APIClient.ActivityItemPayload]
    }

    struct MemoryTab {
        let memory: APIClient.GroupMemoryPayload?
        let finance: APIClient.GroupFinancePayload?
        let pulse: APIClient.GroupPulsePayload?
        var participants: [APIClient.GroupParticipantPayload] = []
    }

    private static var pulseByMoment: [String: PulseTab] = [:]
    private static var memoryByMoment: [String: MemoryTab] = [:]
    private static var lifeByMoment: [String: APIClient.GroupLifePayload] = [:]

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

    static func peekLife(_ momentId: String) -> APIClient.GroupLifePayload? {
        lifeByMoment[momentId]
    }

    static func putLife(_ momentId: String, _ payload: APIClient.GroupLifePayload) {
        lifeByMoment[momentId] = payload
    }

    /// Drop cached facets for one moment after a write so SWR reloads fresh data.
    static func invalidateMoment(_ momentId: String) {
        pulseByMoment.removeValue(forKey: momentId)
        memoryByMoment.removeValue(forKey: momentId)
        lifeByMoment.removeValue(forKey: momentId)
    }

    static func clear() {
        pulseByMoment.removeAll()
        memoryByMoment.removeAll()
        lifeByMoment.removeAll()
    }
}

enum GroupTabPrefetch {
    static let activityLimit = 5

    /// Warm pulse+finance+activity so Moments/Memory/Life paint without spinners.
    static func run(momentId: String) async {
        guard !momentId.isEmpty else { return }
        do {
            async let pulseResult = APIClient.shared.getGroupPulse(momentId: momentId)
            async let financeResult = APIClient.shared.getGroupFinance(momentId: momentId)
            async let activityResult = APIClient.shared.listGroupActivity(momentId: momentId, limit: activityLimit)
            let loadedPulse = try await pulseResult
            let finFacet = try await financeResult
            let loadedActivity = try await activityResult
            let loadedFinance = finFacet.payload ?? loadedPulse.payload?.finance
            GroupTabDataCache.putPulse(momentId, .init(
                title: loadedPulse.title,
                pulse: loadedPulse,
                finance: loadedFinance,
                activities: loadedActivity
            ))
        } catch {
            // Prefetch is best-effort; visible tabs retry on their own .task.
        }
    }
}

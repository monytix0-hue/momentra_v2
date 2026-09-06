import Foundation

/// In-memory SWR cache for Group tab datasets keyed by momentId.
enum GroupTabDataCache {
    struct PulseTab {
        let title: String?
        let pulse: APIClient.GroupPulsePayload?
        let finance: APIClient.GroupFinancePayload?
        let activities: [APIClient.ActivityItemPayload]
        var insights: [AnalyticsInsightItemPayload]

        init(
            title: String?,
            pulse: APIClient.GroupPulsePayload?,
            finance: APIClient.GroupFinancePayload?,
            activities: [APIClient.ActivityItemPayload],
            insights: [AnalyticsInsightItemPayload] = []
        ) {
            self.title = title
            self.pulse = pulse
            self.finance = finance
            self.activities = activities
            self.insights = insights
        }
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

    /// Warm critical-path pulse+finance+activity (inflight-deduped via GroupTabLoad).
    static func run(momentId: String) async {
        guard !momentId.isEmpty else { return }
        _ = try? await GroupTabLoad.loadPulseTab(momentId: momentId)
    }
}

import Foundation

/// In-memory SWR cache for Personal tab datasets (pulse + activity preview).
enum PersonalTabDataCache {
    struct PulseEntry {
        let pulse: APIClient.PersonalPulsePayload
        let activities: [APIClient.ActivityItemPayload]
        let savedAtMs: TimeInterval
    }

    private static var pulseStore: [String: PulseEntry] = [:]
    private static var lifePayload: APIClient.PersonalLifePayload?

    static func cacheKey(momentId: String?) -> String {
        guard let momentId, !momentId.isEmpty else { return "__personal_default__" }
        return momentId
    }

    static func peekPulse(momentId: String?) -> PulseEntry? {
        pulseStore[cacheKey(momentId: momentId)]
    }

    static func putPulse(momentId: String?, pulse: APIClient.PersonalPulsePayload, activities: [APIClient.ActivityItemPayload]) {
        pulseStore[cacheKey(momentId: momentId)] = PulseEntry(
            pulse: pulse,
            activities: activities,
            savedAtMs: Date().timeIntervalSince1970 * 1000
        )
    }

    static func peekLife() -> APIClient.PersonalLifePayload? {
        lifePayload
    }

    static func putLife(_ payload: APIClient.PersonalLifePayload) {
        lifePayload = payload
    }

    static func clear() {
        pulseStore.removeAll()
        lifePayload = nil
    }
}

struct PersonalPulseTabData {
    let pulse: APIClient.PersonalPulsePayload
    let activities: [APIClient.ActivityItemPayload]
}

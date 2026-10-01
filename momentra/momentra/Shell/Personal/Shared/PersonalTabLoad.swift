import Foundation

enum PersonalTabLoad {
    static let pulseActivityLimit = 5
    /// Moments first page — UI must not assume this is the full history.
    static let momentsActivityLimit = 30

    struct MomentsPage {
        let pulse: APIClient.PersonalPulsePayload?
        let activities: [APIClient.ActivityItemPayload]
        /// True when the page was filled to the limit (more may exist).
        let mayHaveMore: Bool
    }

    /// Parallel pulse + activity fetch with in-memory SWR cache write.
    static func loadPulseTab(momentId: String?) async throws -> PersonalPulseTabData {
        let mark = ShellPerf.start("pulse_tab_ready")
        async let pulseTask = APIClient.shared.getPersonalPulse(momentId: momentId)
        async let activityTask = APIClient.shared.listPersonalActivity(momentId: momentId, limit: pulseActivityLimit)
        let pulse = try await pulseTask
        let activities = try await activityTask
        PersonalTabDataCache.putPulse(momentId: momentId, pulse: pulse, activities: activities)
        ShellPerf.end(mark, extras: ["context": "PERSONAL", "parallel": true])
        return PersonalPulseTabData(pulse: pulse, activities: activities)
    }

    static func loadMomentsTab(momentId: String?) async throws -> MomentsPage {
        let mark = ShellPerf.start("moments_tab_ready")
        async let pulseTask = APIClient.shared.getPersonalPulse(momentId: momentId)
        async let activityTask = APIClient.shared.listPersonalActivity(momentId: momentId, limit: momentsActivityLimit)
        let pulse = try? await pulseTask
        let activities = (try? await activityTask) ?? []
        ShellPerf.end(mark, extras: ["context": "PERSONAL", "moments": true])
        return MomentsPage(
            pulse: pulse,
            activities: activities,
            mayHaveMore: activities.count >= momentsActivityLimit
        )
    }
}

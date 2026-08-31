import Foundation

enum PersonalTabLoad {
    static let pulseActivityLimit = 5

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
}

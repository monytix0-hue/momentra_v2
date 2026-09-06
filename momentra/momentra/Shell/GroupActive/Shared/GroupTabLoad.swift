import Foundation

/// Paint-first Group pulse load with inflight dedupe (mirrors APK GroupTabLoad).
enum GroupTabLoad {
    private actor InflightGate {
        var tasks: [String: Task<GroupTabDataCache.PulseTab, Error>] = [:]

        func begin(
            momentId: String,
            create: () -> Task<GroupTabDataCache.PulseTab, Error>
        ) -> (task: Task<GroupTabDataCache.PulseTab, Error>, owner: Bool) {
            if let existing = tasks[momentId] { return (existing, false) }
            let task = create()
            tasks[momentId] = task
            return (task, true)
        }

        func end(momentId: String) {
            tasks.removeValue(forKey: momentId)
        }
    }

    private static let activityLimit = GroupTabPrefetch.activityLimit
    private static let gate = InflightGate()

    /// Critical path: pulse + finance + activity. Call [enrich] after paint for analytics.
    static func loadPulseTab(momentId: String) async throws -> GroupTabDataCache.PulseTab {
        let owned = await gate.begin(momentId: momentId) {
            Task { try await fetchCritical(momentId: momentId) }
        }
        do {
            let tab = try await owned.task.value
            if owned.owner {
                await gate.end(momentId: momentId)
            }
            return tab
        } catch {
            if owned.owner {
                await gate.end(momentId: momentId)
            }
            throw error
        }
    }

    private static func fetchCritical(momentId: String) async throws -> GroupTabDataCache.PulseTab {
        let mark = ShellPerf.start("pulse_tab_ready")
        async let pulseResult = APIClient.shared.getGroupPulse(momentId: momentId)
        async let financeResult = APIClient.shared.getGroupFinance(momentId: momentId)
        async let activityResult = APIClient.shared.listGroupActivity(momentId: momentId, limit: activityLimit)
        let loadedPulse = try await pulseResult
        let finFacet = try await financeResult
        let loadedActivity = try await activityResult
        let loadedFinance = finFacet.payload ?? loadedPulse.payload?.finance
        let previous = GroupTabDataCache.peekPulse(momentId)
        let tab = GroupTabDataCache.PulseTab(
            title: loadedPulse.title,
            pulse: loadedPulse,
            finance: loadedFinance,
            activities: loadedActivity,
            insights: previous?.insights ?? []
        )
        GroupTabDataCache.putPulse(momentId, tab)
        ShellPerf.end(mark, extras: ["context": "GROUP", "paint": true, "parallel": true])
        return tab
    }

    /// Deferred analytics — does not block first paint.
    @discardableResult
    static func enrich(momentId: String) async -> GroupTabDataCache.PulseTab? {
        do {
            async let insightsResult = APIClient.shared.listAnalyticsInsights(scopeType: "MOMENT", scopeId: momentId)
            async let metricsResult = APIClient.shared.listAnalyticsMetrics(scopeType: "MOMENT", scopeId: momentId)
            async let refreshResult = APIClient.shared.refreshAnalytics(context: "GROUP_PULSE", momentId: momentId)
            let insights = (try? await insightsResult)?.items ?? []
            _ = try? await metricsResult
            _ = try? await refreshResult
            guard let previous = GroupTabDataCache.peekPulse(momentId) else { return nil }
            let tab = GroupTabDataCache.PulseTab(
                title: previous.title,
                pulse: previous.pulse,
                finance: previous.finance,
                activities: previous.activities,
                insights: insights
            )
            GroupTabDataCache.putPulse(momentId, tab)
            ShellPerf.instant("group_pulse_enrich", extras: [
                "momentId": String(momentId.prefix(8)),
                "insights": insights.count,
            ])
            return tab
        } catch {
            return nil
        }
    }
}

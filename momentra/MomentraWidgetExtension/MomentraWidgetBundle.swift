import WidgetKit
import SwiftUI

@main
struct MomentraWidgetBundle: WidgetBundle {
    var body: some Widget {
        // Phase 1: Status Glance
        PersonalPulseWidget()
        PersonalLifeWidget()
        GroupPulseWidget()
        GroupFinanceWidget()
        BusinessPulseWidget()
        BusinessFinanceWidget()
        BusinessLifeWidget()

        // Phase 2: Content Feeds
        GroupActivityWidget()
        GroupMemoryWidget()
        PersonalActivityWidget()
        BusinessTimelineWidget()
        BusinessWeeklyWidget()

        // Phase 3: Roster & People
        GroupRosterWidget()
        GroupAttendanceWidget()
        BusinessRosterWidget()
        KhataWidget()

        // Phase 4: Outbox + Quick Actions
        OutboxMonitorWidget()
        QuickActionsWidget()

        // Phase 5: Live Activity
        if #available(iOS 16.1, *) {
            OutboxFlushWidgetLiveActivity()
        }
    }
}

import Foundation
import UserNotifications

/// Local 20:00 Personal evening check-in — mirrors Android PersonalEveningNudgeScheduler.
enum PersonalEveningNudgeScheduler {
    private static let requestId = "personal.evening.nudge"

    static func sync(enabled: Bool) {
        let center = UNUserNotificationCenter.current()
        center.removePendingNotificationRequests(withIdentifiers: [requestId])
        guard enabled else { return }
        center.requestAuthorization(options: [.alert, .sound]) { granted, _ in
            guard granted else { return }
            var comps = DateComponents()
            comps.hour = 20
            comps.minute = 0
            let content = UNMutableNotificationContent()
            content.title = "How was today?"
            content.body = "Log a mood or rest in a tap."
            content.userInfo = ["openPersonalPulse": true]
            let trigger = UNCalendarNotificationTrigger(dateMatching: comps, repeats: true)
            let req = UNNotificationRequest(identifier: requestId, content: content, trigger: trigger)
            center.add(req)
        }
    }

    static func onTodaySave() {
        PersonalHabitPreferences.markTodaySave()
        if !PersonalHabitPreferences.eveningNudgeEnabled {
            PersonalHabitPreferences.eveningNudgeEnabled = true
        }
        sync(enabled: PersonalHabitPreferences.eveningNudgeEnabled)
    }
}

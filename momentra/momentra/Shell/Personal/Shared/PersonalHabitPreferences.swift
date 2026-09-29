import Foundation

/// Local Personal habit prefs (Simple mode + evening nudge) — mirrors Android AppPreferences.
enum PersonalHabitPreferences {
    private static let simpleModeKey = "personal_simple_mode"
    private static let eveningNudgeKey = "personal_evening_nudge_enabled"
    private static let todaySaveKey = "personal_today_save_seen"
    private static let simpleCtaDismissedKey = "personal_simple_cta_dismissed"

    static var simpleMode: Bool {
        get { UserDefaults.standard.bool(forKey: simpleModeKey) }
        set { UserDefaults.standard.set(newValue, forKey: simpleModeKey) }
    }

    static var eveningNudgeEnabled: Bool {
        get { UserDefaults.standard.bool(forKey: eveningNudgeKey) }
        set { UserDefaults.standard.set(newValue, forKey: eveningNudgeKey) }
    }

    static var hasTodaySave: Bool {
        get { UserDefaults.standard.bool(forKey: todaySaveKey) }
        set { UserDefaults.standard.set(newValue, forKey: todaySaveKey) }
    }

    static var simpleCtaDismissed: Bool {
        get { UserDefaults.standard.bool(forKey: simpleCtaDismissedKey) }
        set { UserDefaults.standard.set(newValue, forKey: simpleCtaDismissedKey) }
    }

    static func markTodaySave() {
        hasTodaySave = true
    }
}

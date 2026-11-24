import Foundation
import os

/// Publishes the "current context" the widgets build their file names from.
///
/// Providers cannot receive parameters from the app, so each one looks up its id in the shared App
/// Group defaults:
///
///   `widget_last_user_id`     — Personal*, OutboxMonitor
///   `widget_last_moment_id`   — Group*, Business*
///   `widget_last_company_id`  — Khata
///
/// Snapshot files are named `<kind>-<id>.json`, so an empty id resolves to `<kind>-.json`, a file no
/// writer ever creates. Publishing these is what lets a written snapshot and the provider that reads
/// it agree on a filename.
enum WidgetContext {
    static let appGroupId = "group.resolvingpoint.momentra"

    enum Key: String {
        case userId = "widget_last_user_id"
        case momentId = "widget_last_moment_id"
        case companyId = "widget_last_company_id"
    }

    private static let logger = Logger(subsystem: "com.momentra.app", category: "WidgetContext")

    private static var defaults: UserDefaults? {
        UserDefaults(suiteName: appGroupId)
    }

    /// Publish one id. Idempotent — repeated calls with the same value do not rewrite.
    static func set(_ key: Key, _ value: String?) {
        guard let defaults else {
            logger.error("App Group defaults unavailable (\(appGroupId, privacy: .public))")
            return
        }
        let trimmed = value?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        guard !trimmed.isEmpty else { return }
        guard defaults.string(forKey: key.rawValue) != trimmed else { return }
        defaults.set(trimmed, forKey: key.rawValue)
    }

    static func current(_ key: Key) -> String? {
        guard let value = defaults?.string(forKey: key.rawValue), !value.isEmpty else { return nil }
        return value
    }

    /// The signed-in user, or nil if the app hasn't published one yet.
    static var userId: String? { current(.userId) }
    /// The moment the user is currently in, or nil.
    static var momentId: String? { current(.momentId) }
    /// The company scoping Khata, or nil.
    static var companyId: String? { current(.companyId) }
}

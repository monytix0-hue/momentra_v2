import Foundation
import os
import WidgetKit

/// Pushes a WidgetKit reload after a snapshot write.
///
/// Every provider schedules with `policy: .after(30 * 60)`, which is a *minimum* interval — nothing
/// is pushed. Without this, a write lands, the JSON is fresh, and the tile still shows old data
/// until the next scheduled refresh. This makes each write visible immediately instead.
///
/// `reloadTimelines` has a daily budget per widget and the write paths fire on every projection
/// load, so reloads are debounced. A burst of writes for one widget produces one reload; the
/// leading call goes out at once so the first write is immediate.
///
/// `Kind` and the debounce policy live here next to their only caller
/// (`WidgetSnapshotManager.write*`); the single `WidgetCenter` call below is the whole reason this
/// type exists.
enum WidgetReloader {
    private static let logger = Logger(subsystem: "com.momentra.app", category: "WidgetReload")

    /// Kinds are the `kind` strings declared by each widget provider, so a reload can never drift
    /// from the widget it targets.
    enum Kind: String, CaseIterable {
        // Personal
        case personalPulse = "PersonalPulseWidget"
        case personalLife = "PersonalLifeWidget"
        case personalActivity = "PersonalActivityWidget"

        // Group
        case groupPulse = "GroupPulseWidget"
        case groupFinance = "GroupFinanceWidget"
        case groupActivity = "GroupActivityWidget"
        case groupMemory = "GroupMemoryWidget"
        case groupAttendance = "GroupAttendanceWidget"
        case groupRoster = "GroupRosterWidget"

        // Business
        case businessPulse = "BusinessPulseWidget"
        case businessFinance = "BusinessFinanceWidget"
        case businessLife = "BusinessLifeWidget"
        case businessTimeline = "BusinessTimelineWidget"
        case businessWeekly = "BusinessWeeklyWidget"
        case businessRoster = "BusinessRosterWidget"
        case khata = "KhataWidget"

        // Outbox
        case outboxMonitor = "OutboxMonitorWidget"
    }

    /// Minimum gap between reloads of the same kind.
    static let debounceInterval: TimeInterval = 60

    private static let lock = NSLock()
    /// Thread-safe last-fire table; `WidgetSnapshotManager.write` runs off the main actor.
    private static var lastFired: [Kind: TimeInterval] = [:]

    /// Reload one widget, at most once per `debounceInterval`.
    static func reload(_ kind: Kind) {
        let now = Date().timeIntervalSince1970

        lock.lock()
        let previous = lastFired[kind]
        let shouldFire = previous.map { now - $0 >= debounceInterval } ?? true
        if shouldFire { lastFired[kind] = now }
        lock.unlock()

        guard shouldFire else { return }
        WidgetCenter.shared.reloadTimelines(ofKind: kind.rawValue)
        logger.debug("Reloaded widget \(kind.rawValue, privacy: .public)")
    }

    /// Reload several widgets in one pass — one debounce table, N budget checks.
    static func reload(_ kinds: [Kind]) {
        for kind in kinds { reload(kind) }
    }

    /// Clear the debounce table. Does not itself push a reload.
    static func resetDebounce() {
        lock.lock()
        lastFired.removeAll()
        lock.unlock()
    }
}

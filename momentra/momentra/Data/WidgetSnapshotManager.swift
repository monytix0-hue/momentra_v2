import Foundation
import os

/// Writes snapshot JSONs to the shared App Group so widgets can read them.
///
/// Storage (container resolution, atomic writes, protection, backup exclusion) lives in
/// `WidgetSnapshotStore`, which the widget extension compiles too — see that file for the format
/// contract. This type adds the per-widget `write*`/`read*` namespacing and, on every write, the
/// `WidgetReloader` push that makes the change visible without waiting for the 30-minute timeline.
enum WidgetSnapshotManager {
    static let appGroupId = WidgetSnapshotStore.appGroupId

    // MARK: - Storage

    private static func write<T: Encodable>(_ value: T, filename: String) {
        WidgetSnapshotStore.write(value, filename: filename)
    }

    private static func read<T: Decodable>(filename: String) -> T? {
        WidgetSnapshotStore.read(filename: filename)
    }

    // MARK: - Personal

    static func writePersonalPulse(userId: String, payload: PersonalPulseWidgetSnapshot) {
        WidgetContext.set(.userId, userId)
        write(payload, filename: "personal-pulse-\(userId).json")
        WidgetReloader.reload(.personalPulse)
    }

    static func readPersonalPulse(userId: String) -> PersonalPulseWidgetSnapshot? {
        read(filename: "personal-pulse-\(userId).json")
    }

    static func writePersonalLife(userId: String, payload: PersonalLifeWidgetSnapshot) {
        WidgetContext.set(.userId, userId)
        write(payload, filename: "personal-life-\(userId).json")
        WidgetReloader.reload(.personalLife)
    }

    static func readPersonalLife(userId: String) -> PersonalLifeWidgetSnapshot? {
        read(filename: "personal-life-\(userId).json")
    }

    static func writePersonalActivity(userId: String, payload: ActivityFeedWidgetSnapshot) {
        WidgetContext.set(.userId, userId)
        write(payload, filename: "personal-activity-\(userId).json")
        WidgetReloader.reload(.personalActivity)
    }

    static func readPersonalActivity(userId: String) -> ActivityFeedWidgetSnapshot? {
        read(filename: "personal-activity-\(userId).json")
    }

    // MARK: - Group

    static func writeGroupPulse(momentId: String, payload: GroupPulseWidgetSnapshot) {
        WidgetContext.set(.momentId, momentId)
        write(payload, filename: "group-pulse-\(momentId).json")
        WidgetReloader.reload(.groupPulse)
    }

    static func readGroupPulse(momentId: String) -> GroupPulseWidgetSnapshot? {
        read(filename: "group-pulse-\(momentId).json")
    }

    static func writeGroupFinance(momentId: String, payload: GroupFinanceWidgetSnapshot) {
        WidgetContext.set(.momentId, momentId)
        write(payload, filename: "group-finance-\(momentId).json")
        WidgetReloader.reload(.groupFinance)
    }

    static func readGroupFinance(momentId: String) -> GroupFinanceWidgetSnapshot? {
        read(filename: "group-finance-\(momentId).json")
    }

    static func writeGroupActivity(momentId: String, payload: ActivityFeedWidgetSnapshot) {
        WidgetContext.set(.momentId, momentId)
        write(payload, filename: "group-activity-\(momentId).json")
        WidgetReloader.reload(.groupActivity)
    }

    static func readGroupActivity(momentId: String) -> ActivityFeedWidgetSnapshot? {
        read(filename: "group-activity-\(momentId).json")
    }

    static func writeGroupMemory(momentId: String, payload: GroupMemoryWidgetSnapshot) {
        WidgetContext.set(.momentId, momentId)
        write(payload, filename: "group-memory-\(momentId).json")
        WidgetReloader.reload(.groupMemory)
    }

    static func readGroupMemory(momentId: String) -> GroupMemoryWidgetSnapshot? {
        read(filename: "group-memory-\(momentId).json")
    }

    static func writeGroupAttendance(momentId: String, payload: GroupAttendanceWidgetSnapshot) {
        WidgetContext.set(.momentId, momentId)
        write(payload, filename: "group-attendance-\(momentId).json")
        WidgetReloader.reload(.groupAttendance)
    }

    static func readGroupAttendance(momentId: String) -> GroupAttendanceWidgetSnapshot? {
        read(filename: "group-attendance-\(momentId).json")
    }

    static func writeGroupRoster(momentId: String, payload: GroupRosterWidgetSnapshot) {
        WidgetContext.set(.momentId, momentId)
        write(payload, filename: "group-roster-\(momentId).json")
        WidgetReloader.reload(.groupRoster)
    }

    static func readGroupRoster(momentId: String) -> GroupRosterWidgetSnapshot? {
        read(filename: "group-roster-\(momentId).json")
    }

    // MARK: - Business

    static func writeBusinessPulse(momentId: String, payload: BusinessPulseWidgetSnapshot) {
        WidgetContext.set(.momentId, momentId)
        write(payload, filename: "business-pulse-\(momentId).json")
        WidgetReloader.reload(.businessPulse)
    }

    static func readBusinessPulse(momentId: String) -> BusinessPulseWidgetSnapshot? {
        read(filename: "business-pulse-\(momentId).json")
    }

    static func writeBusinessFinance(momentId: String, payload: BusinessFinanceWidgetSnapshot) {
        WidgetContext.set(.momentId, momentId)
        write(payload, filename: "business-finance-\(momentId).json")
        WidgetReloader.reload(.businessFinance)
    }

    static func readBusinessFinance(momentId: String) -> BusinessFinanceWidgetSnapshot? {
        read(filename: "business-finance-\(momentId).json")
    }

    static func writeBusinessLife(momentId: String, payload: BusinessLifeWidgetSnapshot) {
        WidgetContext.set(.momentId, momentId)
        write(payload, filename: "business-life-\(momentId).json")
        WidgetReloader.reload(.businessLife)
    }

    static func readBusinessLife(momentId: String) -> BusinessLifeWidgetSnapshot? {
        read(filename: "business-life-\(momentId).json")
    }

    static func writeBusinessTimeline(momentId: String, payload: BusinessTimelineWidgetSnapshot) {
        WidgetContext.set(.momentId, momentId)
        write(payload, filename: "business-timeline-\(momentId).json")
        WidgetReloader.reload(.businessTimeline)
    }

    static func readBusinessTimeline(momentId: String) -> BusinessTimelineWidgetSnapshot? {
        read(filename: "business-timeline-\(momentId).json")
    }

    static func writeBusinessWeekly(momentId: String, period: String, payload: BusinessWeeklyWidgetSnapshot) {
        WidgetContext.set(.momentId, momentId)
        write(payload, filename: "business-weekly-\(momentId)-\(period).json")
        WidgetReloader.reload(.businessWeekly)
    }

    static func readBusinessWeekly(momentId: String, period: String) -> BusinessWeeklyWidgetSnapshot? {
        read(filename: "business-weekly-\(momentId)-\(period).json")
    }

    // MARK: - Roster / Khata

    static func writeBusinessRoster(momentId: String, payload: BusinessRosterWidgetSnapshot) {
        WidgetContext.set(.momentId, momentId)
        write(payload, filename: "business-roster-\(momentId).json")
        WidgetReloader.reload(.businessRoster)
    }

    static func readBusinessRoster(momentId: String) -> BusinessRosterWidgetSnapshot? {
        read(filename: "business-roster-\(momentId).json")
    }

    static func writeKhata(companyId: String, payload: KhataWidgetSnapshot) {
        WidgetContext.set(.companyId, companyId)
        write(payload, filename: "khata-\(companyId).json")
        WidgetReloader.reload(.khata)
    }

    static func readKhata(companyId: String) -> KhataWidgetSnapshot? {
        read(filename: "khata-\(companyId).json")
    }

    // MARK: - Outbox

    static func readOutbox(userId: String) -> OutboxWidgetSnapshot? {
        guard let container = WidgetSnapshotStore.containerURL else { return nil }
        let url = container
            .appendingPathComponent("OfflineOutbox", isDirectory: true)
            .appendingPathComponent("outbox-snapshot-\(userId).json")
        do {
            let data = try Data(contentsOf: url)
            return try JSONDecoder().decode(OutboxWidgetSnapshot.self, from: data)
        } catch let error as NSError
            where error.domain == NSCocoaErrorDomain && error.code == NSFileReadNoSuchFileError
        {
            return nil
        } catch {
            WidgetSnapshotStore.logReadFailure(error, what: "outbox snapshot")
            return nil
        }
    }
}


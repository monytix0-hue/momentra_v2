import Foundation

enum BusinessMomentFamilyConfig {
    private static var pendingQuickAddByMoment: [String: String] = [:]
    private static let lock = NSLock()

    static func setPendingQuickAdd(momentId: String, code: String) {
        let trimmed = code.trimmingCharacters(in: .whitespacesAndNewlines).uppercased()
        guard !trimmed.isEmpty else { return }
        lock.lock()
        pendingQuickAddByMoment[momentId] = trimmed
        lock.unlock()
    }

    static func consumePendingQuickAdd(momentId: String) -> String? {
        lock.lock()
        defer { lock.unlock() }
        return pendingQuickAddByMoment.removeValue(forKey: momentId)
    }
}
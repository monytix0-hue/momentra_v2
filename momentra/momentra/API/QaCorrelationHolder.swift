import Foundation

/// Debug/QA correlation override for S9 Master Certification (mirrors Android QaCorrelationHolder).
/// Set via debug URL `momentra-qa://correlate?id=qa-…&run=QA-…` or test helpers.
enum QaCorrelationHolder {
    private static let lock = NSLock()
    private static var nextCorrelationId: String?
    private static var runId: String?

    static func setNextCorrelationId(_ id: String?) {
        lock.lock()
        defer { lock.unlock() }
        nextCorrelationId = id?.trimmingCharacters(in: .whitespacesAndNewlines).nilIfEmpty
    }

    static func setRunId(_ id: String?) {
        lock.lock()
        defer { lock.unlock() }
        runId = id?.trimmingCharacters(in: .whitespacesAndNewlines).nilIfEmpty
    }

    static func peekRunId() -> String? {
        lock.lock()
        defer { lock.unlock() }
        return runId
    }

    static func takeCorrelationId() -> String {
        lock.lock()
        defer { lock.unlock() }
        if let override = nextCorrelationId {
            nextCorrelationId = nil
            return override
        }
        return UUID().uuidString
    }
}

private extension String {
    var nilIfEmpty: String? { isEmpty ? nil : self }
}

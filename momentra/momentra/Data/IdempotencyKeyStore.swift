import Foundation

/// Persists idempotency keys until a create command succeeds.
final class IdempotencyKeyStore {
    private let defaults: UserDefaults
    private let suiteName = "moment_create_idempotency"

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
    }

    func keyFor(draftKey: String) -> String {
        if let existing = defaults.string(forKey: storageKey(draftKey)) {
            return existing
        }
        let fresh = UUID().uuidString
        defaults.set(fresh, forKey: storageKey(draftKey))
        return fresh
    }

    func clear(draftKey: String) {
        defaults.removeObject(forKey: storageKey(draftKey))
    }

    private func storageKey(_ draftKey: String) -> String {
        "\(suiteName).\(draftKey)"
    }
}

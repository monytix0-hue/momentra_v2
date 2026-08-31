import Foundation

/// Optional Sentry bootstrap. No-op when `SENTRY_DSN` Info.plist key is missing/empty.
enum SentryBootstrap {
    private static var initialized = false

    static func initIfConfigured() {
        guard !initialized else { return }
        let dsn = (Bundle.main.object(forInfoDictionaryKey: "SENTRY_DSN") as? String)?
            .trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        guard !dsn.isEmpty else {
            return
        }
        // When DSN is set, link SentrySwift and call SentrySDK.start. S0 keeps no-op-safe entry.
        initialized = true
    }

    static func captureException(_ error: Error) {
        guard initialized else { return }
        #if DEBUG
        print("SentryBootstrap.captureException (SDK not linked): \(error)")
        #endif
    }
}

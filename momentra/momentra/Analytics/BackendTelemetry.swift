import Foundation
#if canImport(UIKit)
import UIKit
#endif

struct TelemetryUserSnapshot: Encodable {
    var userName: String?
    var userEmail: String?
    var userPhone: String?
    var userAge: String?
    var userSex: String?
    var hasPhoto: Bool?
    var photoUrl: String?
    var authProviders: String?
}

struct TelemetryEventPayload: Encodable {
    let eventName: String
    var screenName: String?
    var widgetName: String?
    let clientOccurredAt: String
    var properties: [String: String] = [:]
}

struct TelemetryIngestPayload: Encodable {
    let sessionId: String
    let anonymousId: String
    let platform: String
    var appVersion: String?
    var deviceModel: String?
    var sessionEndedAt: String?
    var userSnapshot: TelemetryUserSnapshot?
    let events: [TelemetryEventPayload]
}

/// First-party telemetry — batches to POST /v1/telemetry/events (PostgreSQL).
/// Mutable state is confined to `queue`; safe to share across concurrency domains.
///
/// `userSnapshot` carries an account's name, email and phone. It is only ever populated when
/// `MomentraAnalytics.sendsAccountSnapshotToBackend` is on, which is off by default — see the note
/// there. Nothing in this type decides that policy; it only transports whatever it is handed.
final class BackendTelemetry: @unchecked Sendable {
    static let shared = BackendTelemetry()

    /// A ceiling on the retry queue. Without one, a persistent 5xx that is not in the drop list
    /// re-inserts every batch forever and `pending` grows unbounded in a long-lived foreground.
    private static let maxPendingEvents = 500

    private let defaults = UserDefaults.standard
    private let queue = DispatchQueue(label: "momentra.backend-telemetry", qos: .utility)
    private var pending: [TelemetryEventPayload] = []
    private var userSnapshot: TelemetryUserSnapshot?
    private var flushTimer: Timer?

    private lazy var anonymousId: String = {
        if let existing = defaults.string(forKey: Keys.anonymousId) { return existing }
        let created = UUID().uuidString
        defaults.set(created, forKey: Keys.anonymousId)
        return created
    }()

    private var sessionId: String {
        get {
            defaults.string(forKey: Keys.sessionId) ?? resetSessionId()
        }
        set {
            defaults.set(newValue, forKey: Keys.sessionId)
        }
    }

    private init() {
        // Created on the main queue so the timer lands on the main run loop; a timer built from a
        // background caller's run loop never fires, and the periodic flush would silently stop.
        DispatchQueue.main.async { [weak self] in
            guard let self else { return }
            let timer = Timer(timeInterval: 15, repeats: true) { [weak self] _ in
                self?.flush()
            }
            RunLoop.main.add(timer, forMode: .common)
            self.flushTimer = timer
        }
    }

    func onSessionStart() {
        sessionId = UUID().uuidString
    }

    func onSessionEnd() {
        enqueue(name: "session_end", params: ["platform": "ios"])
        flush(sessionEnded: true)
    }

    func updateUserSnapshot(_ snapshot: TelemetryUserSnapshot) {
        queue.async { self.userSnapshot = snapshot }
    }

    private static let telemetryInstantFormatter: ISO8601DateFormatter = {
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        return formatter
    }()

    func enqueue(name: String, params: [String: Any]) {
        queue.async {
            var screenName: String?
            var widgetName: String?
            var properties: [String: String] = [:]
            for (key, value) in params {
                switch key {
                case "screen_name": screenName = "\(value)"
                case "widget_name": widgetName = "\(value)"
                default: properties[key] = "\(value)"
                }
            }
            self.pending.append(
                TelemetryEventPayload(
                    eventName: name,
                    screenName: screenName,
                    widgetName: widgetName,
                    clientOccurredAt: Self.telemetryInstantFormatter.string(from: Date()),
                    properties: properties
                )
            )
            if self.pending.count >= 100 {
                self.flush()
            }
        }
    }

    func flush(sessionEnded: Bool = false) {
        queue.async {
            #if DEBUG
            self.pending.removeAll()
            #else
            guard !self.pending.isEmpty || sessionEnded else { return }
            let batch = self.pending
            self.pending = []
            guard !batch.isEmpty else { return }

            let payload = TelemetryIngestPayload(
                sessionId: self.sessionId,
                anonymousId: self.anonymousId,
                platform: "ios",
                appVersion: Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String,
                deviceModel: Self.currentDeviceModel,
                sessionEndedAt: sessionEnded ? Self.telemetryInstantFormatter.string(from: Date()) : nil,
                userSnapshot: self.userSnapshot,
                events: batch
            )
            let queue = self.queue
            // The session-end flush is started from `onAppBackground`, where the process is about
            // to be suspended; without an assertion the request never leaves the device.
            let assertion = Self.beginBackgroundAssertion()
            Task {
                defer { Self.endBackgroundAssertion(assertion) }
                do {
                    try await APIClient.shared.ingestTelemetry(payload)
                } catch {
                    let dropBatch: Bool = {
                        if case APIErrorKind.validation = error { return true }
                        if case APIErrorKind.unauthenticated = error { return true }
                        if case APIErrorKind.forbidden = error { return true }
                        if case APIErrorKind.notFound = error { return true }
                        return false
                    }()
                    if !dropBatch {
                        queue.async {
                            self.pending.insert(contentsOf: batch, at: 0)
                            if self.pending.count > Self.maxPendingEvents {
                                // Oldest first: the newest events are the ones an operator is most
                                // likely to be looking for.
                                self.pending.removeFirst(self.pending.count - Self.maxPendingEvents)
                            }
                        }
                    }
                }
            }
            #endif
        }
    }

    private func resetSessionId() -> String {
        let id = UUID().uuidString
        defaults.set(id, forKey: Keys.sessionId)
        return id
    }

    private static func beginBackgroundAssertion() -> UIBackgroundTaskIdentifier {
        #if canImport(UIKit)
        // Hold the task ID in a reference so the expiration handler can read it
        // without capturing a mutable local that is reassigned after capture.
        final class TaskIDBox {
            var value: UIBackgroundTaskIdentifier = .invalid
        }
        let box = TaskIDBox()
        box.value = UIApplication.shared.beginBackgroundTask(withName: "telemetry.flush") {
            let identifier = box.value
            if identifier != .invalid { UIApplication.shared.endBackgroundTask(identifier) }
        }
        return box.value
        #else
        return .invalid
        #endif
    }

    private static func endBackgroundAssertion(_ identifier: UIBackgroundTaskIdentifier) {
        #if canImport(UIKit)
        if identifier != .invalid { UIApplication.shared.endBackgroundTask(identifier) }
        #endif
    }

    private static var currentDeviceModel: String {
#if canImport(UIKit)
        UIDevice.current.model
#else
        ProcessInfo.processInfo.hostName
#endif
    }

    private enum Keys {
        static let anonymousId = "telemetry_anonymous_id"
        static let sessionId = "telemetry_session_id"
    }
}

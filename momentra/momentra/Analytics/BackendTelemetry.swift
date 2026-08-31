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
final class BackendTelemetry: @unchecked Sendable {
    static let shared = BackendTelemetry()

    private let defaults = UserDefaults.standard
    private let queue = DispatchQueue(label: "momentra.backend-telemetry", qos: .utility)
    private var pending: [TelemetryEventPayload] = []
    private var userSnapshot: TelemetryUserSnapshot?

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
        Timer.scheduledTimer(withTimeInterval: 15, repeats: true) { [weak self] _ in
            self?.flush()
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
            Task {
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

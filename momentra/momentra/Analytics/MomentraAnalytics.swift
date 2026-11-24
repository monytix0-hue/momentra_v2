import FirebaseAnalytics
import FirebaseAuth
import Foundation
#if canImport(UIKit)
import UIKit
#endif

/// Screen timing, session tracking, and widget taps via Firebase Analytics.
///
/// Account identity is deliberately kept out of Firebase Analytics. GA4 forbids personally
/// identifiable data in user properties and event parameters, and that applies to email, phone,
/// name and photo URL as much as to anything else. The non-identifying properties that remain —
/// `has_photo`, `auth_providers` — are enough to segment without carrying a person's contact
/// details into a third-party pipeline.
final class MomentraAnalytics {
    static let shared = MomentraAnalytics()

    /// Off by default. When false, `syncUserDemographics` sends only non-identifying properties to
    /// Firebase and skips `BackendTelemetry` entirely. Turn it on only where there is a documented
    /// consent basis for storing an account's name, email and phone server-side.
    private let sendsAccountSnapshotToBackend = false

    private var sessionStartedAt = Date()
    private var sessionActive = true
    private var tickTimer: Timer?

    private var currentScreen: String?
    private var currentScreenEnteredAt = Date()
    private var lastInteractionAt = Date()
    private var stuckReported = false

    /// Backend-only cadence. One second was producing a Firebase event per second — several
    /// thousand an hour of foreground use — for a duration figure the backend already receives.
    private static let tickInterval: TimeInterval = 15

    private init() {
        logEvent("app_session_start", ["platform": "ios"])
    }

    deinit {
        tickTimer?.invalidate()
    }

    func onAppForeground() {
        guard !sessionActive else { return }
        sessionActive = true
        sessionStartedAt = Date()
        BackendTelemetry.shared.onSessionStart()
        logEvent("app_session_start", ["platform": "ios"])
        if let screen = currentScreen {
            resumeScreenTicks(screenName: screen)
        }
    }

    func onAppBackground() {
        guard sessionActive else { return }
        let foregroundSec = Int(Date().timeIntervalSince(sessionStartedAt))
        // `onSessionEnd` enqueues the backend's `session_end` and flushes it. Routing the same
        // event through `logEvent` first would deliver it twice — once with `duration_sec`, once
        // without.
        BackendTelemetry.shared.enqueue(
            name: "session_end",
            params: ["platform": "ios", "duration_sec": foregroundSec]
        )
        BackendTelemetry.shared.onSessionEnd()
        sessionActive = false
        stopScreenTicks()
        if let screen = currentScreen {
            logScreenExit(screenName: screen, reason: "app_background")
        }
    }

    func onScreenEnter(_ screenName: String, screenClass: String = "") {
        let screenClassName = screenClass.isEmpty ? screenName : screenClass
        if let previous = currentScreen, previous != screenName {
            logScreenExit(screenName: previous, reason: "navigate")
        }
        currentScreen = screenName
        currentScreenEnteredAt = Date()
        stuckReported = false
        markInteraction()

        Analytics.logEvent(AnalyticsEventScreenView, parameters: [
            AnalyticsParameterScreenName: screenName,
            AnalyticsParameterScreenClass: screenClassName,
        ])
        logEvent("screen_enter", [
            "screen_name": screenName,
            "screen_class": screenClassName,
        ])
        resumeScreenTicks(screenName: screenName)
    }

    func onScreenExit(_ screenName: String) {
        guard currentScreen == screenName else { return }
        logScreenExit(screenName: screenName, reason: "dispose")
        currentScreen = nil
        stopScreenTicks()
    }

    func trackWidget(screenName: String, widgetName: String, action: String = "tap") {
        markInteraction()
        logEvent("widget_interaction", [
            "screen_name": screenName,
            "widget_name": widgetName,
            "action": action,
        ])
    }

    func trackAuthResult(method: String, success: Bool, errorCode: String? = nil) {
        logEvent(success ? "auth_success" : "auth_error", [
            "method": method,
            "success": success ? 1 : 0,
            "error_code": errorCode ?? "",
        ])
        if success {
            syncUserDemographics(user: Auth.auth().currentUser)
        }
    }

    func syncUserDemographics(
        user: User?,
        profileDisplayName: String? = nil,
        profileEmail: String? = nil,
        profileAge: String? = nil,
        profileSex: String? = nil
    ) {
        guard let user else {
            Analytics.setUserID(nil)
            return
        }
        Analytics.setUserID(user.uid)

        let photoUrl = user.photoURL?.absoluteString ?? ""
        setUserProperty("has_photo", value: photoUrl.isEmpty ? "no" : "yes")
        let providers = user.providerData.map(\.providerID).joined(separator: ",")
        setUserProperty("auth_providers", value: String(providers.prefix(100)))

        // No email, phone, name, age, sex or photo URL is written as a Firebase user property:
        // GA4's terms exclude personal data from that surface regardless of consent.
        guard sendsAccountSnapshotToBackend else { return }

        let name = profileDisplayName ?? user.displayName ?? ""
        let email = profileEmail ?? user.email ?? ""
        let phone = user.phoneNumber ?? ""
        BackendTelemetry.shared.updateUserSnapshot(
            TelemetryUserSnapshot(
                userName: name.isEmpty ? nil : String(name.prefix(100)),
                userEmail: email.isEmpty ? nil : String(email.prefix(100)),
                userPhone: phone.isEmpty ? nil : String(phone.prefix(100)),
                userAge: profileAge ?? "unknown",
                userSex: profileSex ?? "unknown",
                hasPhoto: !photoUrl.isEmpty,
                photoUrl: photoUrl.isEmpty ? nil : String(photoUrl.prefix(100)),
                authProviders: providers.isEmpty ? nil : String(providers.prefix(100))
            )
        )
    }

    func markInteraction() {
        lastInteractionAt = Date()
        stuckReported = false
    }

    private func logScreenExit(screenName: String, reason: String) {
        let durationMs = Int(Date().timeIntervalSince(currentScreenEnteredAt) * 1000)
        let durationSec = max(durationMs / 1000, 0)
        logEvent("screen_exit", [
            "screen_name": screenName,
            "duration_ms": durationMs,
            "duration_sec": durationSec,
            "reason": reason,
        ])
    }

    private func resumeScreenTicks(screenName: String) {
        stopScreenTicks()
        let timer = Timer(timeInterval: Self.tickInterval, repeats: true) { [weak self] _ in
            guard let self, self.sessionActive, self.currentScreen == screenName else { return }
            let now = Date()
            let screenElapsedSec = max(Int(now.timeIntervalSince(self.currentScreenEnteredAt)), 0)
            let sessionElapsedSec = max(Int(now.timeIntervalSince(self.sessionStartedAt)), 0)
            let idleSec = max(Int(now.timeIntervalSince(self.lastInteractionAt)), 0)
            // Backend only. This used to reach Firebase once per second; a duration sample does
            // not need that granularity and the volume is not free.
            BackendTelemetry.shared.enqueue(name: "screen_tick", params: [
                "screen_name": screenName,
                "screen_elapsed_sec": screenElapsedSec,
                "session_elapsed_sec": sessionElapsedSec,
                "idle_sec": idleSec,
            ])
            if idleSec >= 30, !self.stuckReported {
                self.stuckReported = true
                BackendTelemetry.shared.enqueue(name: "screen_stuck", params: [
                    "screen_name": screenName,
                    "idle_sec": idleSec,
                    "screen_elapsed_sec": screenElapsedSec,
                ])
            }
        }
        // Scheduled on the main run loop explicitly: a timer created on a background queue's run
        // loop never fires.
        RunLoop.main.add(timer, forMode: .common)
        tickTimer = timer
    }

    private func stopScreenTicks() {
        tickTimer?.invalidate()
        tickTimer = nil
    }

    private func setUserProperty(_ name: String, value: String) {
        guard !value.isEmpty else { return }
        Analytics.setUserProperty(value, forName: name)
    }

    private func logEvent(_ name: String, _ params: [String: Any]) {
        Analytics.logEvent(name, parameters: params)
        if name == "screen_tick" { return }
        BackendTelemetry.shared.enqueue(name: name, params: params)
    }
}

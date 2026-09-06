import FirebaseAuth
import FirebaseMessaging
import Foundation
import UIKit
import UserNotifications

/// Requests notification permission, syncs APNs→FCM, and POSTs `/me/devices` with the push credential.
@MainActor
enum PushNotifications {
    private static var deviceId: String {
        UIDevice.current.identifierForVendor?.uuidString ?? UUID().uuidString
    }

    /// Latest FCM token or FID delivered by `MessagingDelegate`.
    private static var cachedPushCredential: String?

    static func configure(delegate: UNUserNotificationCenterDelegate & MessagingDelegate) {
        UNUserNotificationCenter.current().delegate = delegate
        Messaging.messaging().delegate = delegate
    }

    static func requestPermissionAndRegister() {
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .badge, .sound]) { granted, error in
            if let error {
                NSLog("Push permission error: \(error.localizedDescription)")
            }
            guard granted else { return }
            DispatchQueue.main.async {
                UIApplication.shared.registerForRemoteNotifications()
            }
        }
    }

    static func handleApnsToken(_ deviceToken: Data) {
        Auth.auth().setAPNSToken(deviceToken, type: .unknown)
        Messaging.messaging().apnsToken = deviceToken
        Task { await ensureRegisteredAndSync() }
    }

    /// Called from MessagingDelegate when FCM/FID registration is available.
    static func notePushCredential(_ credential: String?) {
        guard let credential, !credential.isEmpty else { return }
        cachedPushCredential = credential
    }

    static func syncDeviceWithBackend(explicitToken: String? = nil) async {
        if let explicitToken, !explicitToken.isEmpty {
            cachedPushCredential = explicitToken
        } else if cachedPushCredential == nil {
            await withCheckedContinuation { (continuation: CheckedContinuation<Void, Never>) in
                Messaging.messaging().register { _ in
                    continuation.resume()
                }
            }
        }
        let token = cachedPushCredential
        _ = try? await APIClient.shared.registerDevice(
            deviceId: deviceId,
            platform: "IOS",
            pushToken: token
        )
    }

    private static func ensureRegisteredAndSync() async {
        await withCheckedContinuation { (continuation: CheckedContinuation<Void, Never>) in
            Messaging.messaging().register { error in
                if let error {
                    NSLog("FCM register error: \(error.localizedDescription)")
                }
                continuation.resume()
            }
        }
        await syncDeviceWithBackend()
    }
}

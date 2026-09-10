import FirebaseAuth
import FirebaseMessaging
import Foundation
import UIKit
import UserNotifications

/// Requests notification permission, syncs APNs→FCM, and POSTs `/me/devices` with the FCM token.
@MainActor
enum PushNotifications {
    private static var deviceId: String {
        UIDevice.current.identifierForVendor?.uuidString ?? UUID().uuidString
    }

    /// Latest FCM registration token. Never an installation id — FCM cannot send to an FID.
    private static var cachedFcmToken: String?

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
        Task { await syncDeviceWithBackend() }
    }

    /// Called from `MessagingDelegate` when an FCM registration token is issued or refreshed.
    static func noteFcmToken(_ token: String?) {
        guard let token, !token.isEmpty else { return }
        cachedFcmToken = token
    }

    static func syncDeviceWithBackend(explicitToken: String? = nil) async {
        if let explicitToken, !explicitToken.isEmpty {
            cachedFcmToken = explicitToken
        }
        // A nil token still registers the device for the Devices list; the backend keeps
        // any token it already holds rather than overwriting it with a blank.
        // FCM token arrives via MessagingDelegate → noteFcmToken (token(completion:) is deprecated).
        _ = try? await APIClient.shared.registerDevice(
            deviceId: deviceId,
            platform: "IOS",
            pushToken: cachedFcmToken
        )
    }
}

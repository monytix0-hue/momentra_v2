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

    static func syncDeviceWithBackend(explicitToken: String? = nil) async {
        let token: String?
        if let explicitToken, !explicitToken.isEmpty {
            token = explicitToken
        } else {
            token = await fetchFcmToken()
        }
        _ = try? await APIClient.shared.registerDevice(
            deviceId: deviceId,
            platform: "IOS",
            pushToken: token
        )
    }

    private static func fetchFcmToken() async -> String? {
        await withCheckedContinuation { continuation in
            Messaging.messaging().token { token, error in
                if let error {
                    NSLog("FCM token fetch failed: \(error.localizedDescription)")
                }
                continuation.resume(returning: token)
            }
        }
    }
}

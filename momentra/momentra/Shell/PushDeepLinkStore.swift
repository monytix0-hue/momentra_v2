import Combine
import Foundation

/// Pending push deep link (`momentra://moment/{id}` or `momentra://inbox`) + open attribution id.
@MainActor
final class PushDeepLinkStore: ObservableObject {
    static let shared = PushDeepLinkStore()

    private static let prefsKey = "momentra_pending_push_deep_link"
    private static let notifIdKey = "momentra_pending_user_notification_id"

    @Published private(set) var pendingLink: String?
    @Published private(set) var pendingUserNotificationId: String?

    private init() {
        pendingLink = UserDefaults.standard.string(forKey: Self.prefsKey)
        pendingUserNotificationId = UserDefaults.standard.string(forKey: Self.notifIdKey)
    }

    func offer(_ raw: String, userNotificationId: String? = nil) {
        let trimmed = raw.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }
        pendingLink = trimmed
        UserDefaults.standard.set(trimmed, forKey: Self.prefsKey)
        if let id = userNotificationId?.trimmingCharacters(in: .whitespacesAndNewlines), !id.isEmpty {
            pendingUserNotificationId = id
            UserDefaults.standard.set(id, forKey: Self.notifIdKey)
        }
    }

    func offer(userInfo: [AnyHashable: Any]) {
        let notifId =
            (userInfo["userNotificationId"] as? String)
            ?? (userInfo["user_notification_id"] as? String)
        if let deepLink = userInfo["deepLink"] as? String {
            offer(deepLink, userNotificationId: notifId)
            return
        }
        if let momentId = userInfo["momentId"] as? String, !momentId.isEmpty {
            offer("momentra://moment/\(momentId)", userNotificationId: notifId)
        }
    }

    /// Returns (link, userNotificationId) and clears both.
    func consume() -> (link: String, userNotificationId: String?)? {
        let link = pendingLink ?? UserDefaults.standard.string(forKey: Self.prefsKey)
        let notifId = pendingUserNotificationId ?? UserDefaults.standard.string(forKey: Self.notifIdKey)
        pendingLink = nil
        pendingUserNotificationId = nil
        UserDefaults.standard.removeObject(forKey: Self.prefsKey)
        UserDefaults.standard.removeObject(forKey: Self.notifIdKey)
        guard let link, !link.isEmpty else { return nil }
        return (link, notifId)
    }

    static func isInboxLink(_ raw: String) -> Bool {
        guard let url = URL(string: raw), url.scheme?.lowercased() == "momentra" else { return false }
        return url.host?.lowercased() == "inbox"
    }

    static func parseMomentId(_ raw: String) -> String? {
        guard let url = URL(string: raw) else { return nil }
        let scheme = url.scheme?.lowercased() ?? ""
        guard scheme == "momentra" else { return nil }
        let host = url.host?.lowercased() ?? ""
        let parts = url.path.split(separator: "/").map(String.init)
        if host == "moment", let id = parts.first, !id.isEmpty { return id }
        if parts.first == "moment", let id = parts.dropFirst().first, !id.isEmpty { return id }
        return nil
    }
}

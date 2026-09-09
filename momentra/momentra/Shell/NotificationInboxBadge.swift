import Combine
import Foundation

/// Unread count for the shell bell. Refreshed on foreground and after inbox reads.
@MainActor
final class NotificationInboxBadge: ObservableObject {
    static let shared = NotificationInboxBadge()

    @Published private(set) var unreadCount: Int = 0

    private init() {}

    func set(_ count: Int) {
        unreadCount = max(0, count)
    }

    func refresh() async {
        guard let payload = try? await APIClient.shared.listMyNotifications(limit: 1, unreadOnly: true) else {
            return
        }
        set(payload.unreadCount)
    }
}

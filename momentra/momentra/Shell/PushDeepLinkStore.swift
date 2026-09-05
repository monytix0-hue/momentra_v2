import Combine
import Foundation

/// Pending push deep link (`momentra://moment/{id}` or `momentra://inbox`).
@MainActor
final class PushDeepLinkStore: ObservableObject {
    static let shared = PushDeepLinkStore()

    private static let prefsKey = "momentra_pending_push_deep_link"

    @Published private(set) var pendingLink: String?

    private init() {
        pendingLink = UserDefaults.standard.string(forKey: Self.prefsKey)
    }

    func offer(_ raw: String) {
        let trimmed = raw.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }
        pendingLink = trimmed
        UserDefaults.standard.set(trimmed, forKey: Self.prefsKey)
    }

    func offer(userInfo: [AnyHashable: Any]) {
        if let deepLink = userInfo["deepLink"] as? String {
            offer(deepLink)
            return
        }
        if let momentId = userInfo["momentId"] as? String, !momentId.isEmpty {
            offer("momentra://moment/\(momentId)")
        }
    }

    func consume() -> String? {
        let link = pendingLink ?? UserDefaults.standard.string(forKey: Self.prefsKey)
        pendingLink = nil
        UserDefaults.standard.removeObject(forKey: Self.prefsKey)
        return link?.isEmpty == false ? link : nil
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

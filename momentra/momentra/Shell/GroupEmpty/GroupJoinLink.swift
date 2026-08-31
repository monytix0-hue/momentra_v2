import Combine
import Foundation

enum GroupJoinLink {
    private static let shortPattern = #"^[a-hj-np-z2-9]{8}$"#
    private static let legacyPattern = #"^[a-z0-9]+-[a-z0-9-]+-[a-f0-9]{8}$"#
    private static let jwtish = #"eyJ[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+"#

    static func parse(_ url: URL) -> String? {
        parse(url.absoluteString)
    }

    static func parse(_ raw: String) -> String? {
        let trimmed = raw.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return nil }
        if trimmed.range(of: jwtish, options: .regularExpression) != nil { return nil }
        if trimmed.filter({ $0 == "." }).count >= 2, trimmed.count > 60 { return nil }
        if let slashed = extractFromSlashes(trimmed) { return slashed }
        guard let url = URL(string: trimmed) else { return nil }
        let scheme = url.scheme?.lowercased() ?? ""
        let host = url.host?.lowercased() ?? ""
        let parts = url.path.split(separator: "/").map(String.init)

        if scheme == "momentra" {
            if host == "j" || host == "join" {
                return sanitize(parts.last)
            }
            if parts.first == "j" || parts.first == "join" {
                return sanitize(parts.dropFirst().first)
            }
        }
        if host == "momentra.app" || host == "www.momentra.app",
           let first = parts.first, first == "j" || first == "join" {
            return sanitize(parts.dropFirst().first)
        }
        return sanitize(url.lastPathComponent) ?? extractFromSlashes(trimmed)
    }

    private static func extractFromSlashes(_ raw: String) -> String? {
        let parts = raw.split(separator: "?").first.map(String.init) ?? raw
        let segs = parts.split(separator: "#").first.map(String.init) ?? parts
        let pieces = segs.split(separator: "/").map(String.init)
        if let marker = pieces.firstIndex(where: { $0.lowercased() == "j" || $0.lowercased() == "join" }) {
            return sanitize(pieces.dropFirst(marker + 1).first)
        }
        return sanitize(pieces.last)
    }

    private static func sanitize(_ raw: String?) -> String? {
        guard let raw else { return nil }
        let value = raw.trimmingCharacters(in: CharacterSet(charactersIn: "/")).lowercased()
        if value.range(of: shortPattern, options: [.regularExpression, .caseInsensitive]) != nil { return value }
        if value.range(of: legacyPattern, options: [.regularExpression, .caseInsensitive]) != nil { return value }
        return nil
    }
}

@MainActor
final class JoinInviteStore: ObservableObject {
    static let shared = JoinInviteStore()

    private static let prefsKey = "momentra_pending_join_code"

    @Published private(set) var pendingCode: String?

    private init() {
        pendingCode = UserDefaults.standard.string(forKey: Self.prefsKey)
    }

    func offer(_ code: String) {
        pendingCode = code
        UserDefaults.standard.set(code, forKey: Self.prefsKey)
    }

    func consume() -> String? {
        let code = pendingCode ?? UserDefaults.standard.string(forKey: Self.prefsKey)
        pendingCode = nil
        UserDefaults.standard.removeObject(forKey: Self.prefsKey)
        return code?.isEmpty == false ? code : nil
    }
}

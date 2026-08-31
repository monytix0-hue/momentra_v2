import Foundation

/// Company invite deeplinks — `momentra://c/{code}` / `momentra.app/c/{code}`.
/// Does not accept bare codes (those stay GroupJoinLink) to avoid colliding with group invites.
enum CompanyJoinLink {
    private static let shortPattern = #"^[a-hj-np-z2-9]{8}$"#

    static func displayPath(code: String) -> String {
        "momentra.app/c/\(code.trimmingCharacters(in: .whitespacesAndNewlines).lowercased())"
    }

    static func qrPayload(code: String) -> String {
        "momentra://c/\(code.trimmingCharacters(in: .whitespacesAndNewlines).lowercased())"
    }

    static func parse(_ raw: String) -> String? {
        let trimmed = raw.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return nil }
        if let fromSlash = extractFromSlashes(trimmed) { return fromSlash }
        guard let url = URL(string: trimmed) else { return nil }
        let scheme = url.scheme?.lowercased() ?? ""
        let host = url.host?.lowercased() ?? ""
        let parts = url.path.split(separator: "/").map(String.init)

        if scheme == "momentra" {
            if host == "c" || host == "company" {
                return sanitize(parts.last)
            }
            if parts.first == "c" || parts.first == "company" {
                return sanitize(parts.dropFirst().first)
            }
        }
        if host == "momentra.app" || host == "www.momentra.app",
           let first = parts.first, first == "c" || first == "company" {
            return sanitize(parts.dropFirst().first)
        }
        return nil
    }

    /// Accept bare 8-char codes when the user typed them into Company Setup.
    static func parseTyped(_ raw: String) -> String? {
        if let linked = parse(raw) { return linked }
        return sanitize(raw.trimmingCharacters(in: .whitespacesAndNewlines).lowercased())
    }

    private static func extractFromSlashes(_ raw: String) -> String? {
        let parts = raw.split(separator: "?").first.map(String.init) ?? raw
        let segs = parts.split(separator: "#").first.map(String.init) ?? parts
        let pieces = segs.split(separator: "/").map(String.init)
        if let marker = pieces.firstIndex(where: { $0.lowercased() == "c" || $0.lowercased() == "company" }) {
            return sanitize(pieces.dropFirst(marker + 1).first)
        }
        return nil
    }

    private static func sanitize(_ raw: String?) -> String? {
        guard let raw else { return nil }
        let value = raw.trimmingCharacters(in: CharacterSet(charactersIn: "/")).lowercased()
        if value.range(of: shortPattern, options: [.regularExpression, .caseInsensitive]) != nil { return value }
        return nil
    }
}

enum InviteJoinKind {
    case group(String)
    case company(String)
}

enum InviteJoinLink {
    /// Prefer company `/c/` markers, then group `/j/` / bare short codes.
    static func parse(_ raw: String) -> InviteJoinKind? {
        if let company = CompanyJoinLink.parse(raw) { return .company(company) }
        if let group = GroupJoinLink.parse(raw) { return .group(group) }
        return nil
    }
}

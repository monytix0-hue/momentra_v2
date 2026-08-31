import Foundation

enum GroupInviteLink {
    static func displayPath(code: String) -> String {
        "momentra.app/j/\(code.trimmingCharacters(in: .whitespacesAndNewlines).lowercased())"
    }

    static func copyText(code: String) -> String {
        displayPath(code: code)
    }

    static func qrPayload(code: String) -> String {
        "momentra://j/\(code.trimmingCharacters(in: .whitespacesAndNewlines).lowercased())"
    }
}

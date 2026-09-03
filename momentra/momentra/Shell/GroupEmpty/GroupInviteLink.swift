import Foundation

enum GroupInviteLink {
    private static let httpsBase = "https://momentra.app/j"

    static func displayPath(code: String) -> String {
        "\(httpsBase)/\(code.trimmingCharacters(in: .whitespacesAndNewlines).lowercased())"
    }

    static func copyText(code: String) -> String {
        displayPath(code: code)
    }

    /// Same HTTPS URL as share/copy so camera apps and messengers hit the invite landing.
    static func qrPayload(code: String) -> String {
        displayPath(code: code)
    }
}

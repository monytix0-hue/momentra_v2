import Foundation

/// Visible name for a person. A real email may be shown separately as contact text.
/// Never print a user id, an id prefix, or the synthetic sign-in address as a name.
enum PersonLabel {
    private static let placeholderSuffix = "@users.momentra.local"
    private static let uuidPattern =
        #"^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"#

    static func personName(_ displayName: String?, fallback: String = "Member") -> String {
        let name = displayName?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        if name.isEmpty || isPlaceholder(name) { return fallback }
        return name
    }

    /// Signed-in account line. Empty when there is no display name.
    static func accountName(_ displayName: String?) -> String {
        personName(displayName, fallback: "")
    }

    static func contactEmail(_ email: String?) -> String? {
        let value = email?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        if value.isEmpty || isPlaceholder(value) { return nil }
        return value
    }

    private static func isPlaceholder(_ value: String) -> Bool {
        if value.lowercased().hasSuffix(placeholderSuffix) { return true }
        return value.range(of: uuidPattern, options: .regularExpression) != nil
    }
}

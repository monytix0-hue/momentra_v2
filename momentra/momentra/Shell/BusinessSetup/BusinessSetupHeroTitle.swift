import Foundation

enum BusinessSetupHeroTitle {
    static func format(name: String?, placeholder: String) -> String {
        let trimmed = (name ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
        if trimmed.isEmpty { return placeholder }
        if trimmed.lowercased().hasSuffix(" setup") {
            let prefix = String(trimmed.dropLast(6)).trimmingCharacters(in: .whitespacesAndNewlines)
            let normalized = prefix.prefix(1).uppercased() + prefix.dropFirst()
            return "\(normalized) Setup"
        }
        return "\(trimmed) Setup"
    }
}

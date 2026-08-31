import Foundation

/// Filters wizard state to keys accepted by backend catalog defaults.
enum SetupPreferenceFilter {
    static func filterToCatalogKeys(
        _ preferences: [String: Any],
        allowedKeys: Set<String>
    ) -> [String: Any] {
        preferences.filter { allowedKeys.contains($0.key) }
    }

    static func filterLocalOnly(
        _ preferences: [String: Any],
        localOnlyKeys: Set<String>
    ) -> [String: Any] {
        preferences.filter { !localOnlyKeys.contains($0.key) }
    }
}

import Foundation

/// Guard for async draft-preference prefill.
///
/// `BusinessSetupShell` loads saved draft preferences after appear. The user can start typing
/// before that load resolves, so the prefill loop asks this helper per key instead of assigning
/// unconditionally — a stale draft value must never clobber what the user already typed.
///
/// Callers: `BusinessSetupShell.loadDraftPreferences`; assertions in `BusinessSetupPrefillTests`.
enum BusinessSetupPrefill {
    /// - Parameters:
    ///   - key: preference key being prefilled (e.g. `businessName`) — never renamed, stored prefs depend on it.
    ///   - dirty: keys the user has already edited through a field control.
    ///   - loaded: the value returned by the draft load.
    ///   - current: the in-memory value for `key` right now.
    /// - Returns: `true` only when the field is untouched and empty and the loaded value is non-blank.
    static func shouldApply(
        key: String,
        dirty: Set<String>,
        loaded: String?,
        current: String? = nil
    ) -> Bool {
        if dirty.contains(key) { return false }
        let incoming = (loaded ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
        // Blank / whitespace-only draft values are not worth writing.
        if incoming.isEmpty { return false }
        let existing = (current ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
        // A non-empty current value is never replaced (this also covers incoming == existing).
        if !existing.isEmpty { return false }
        return true
    }
}

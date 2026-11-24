package com.example.momentra.ui.shell.business.setup

/**
 * Guard for async draft-preference prefill.
 *
 * `BusinessSetupShell` loads saved draft preferences after first composition. The user can start
 * typing before that load resolves, so the prefill loop asks this helper per key instead of
 * assigning unconditionally — a stale draft value must never clobber what the user already typed.
 *
 * Callers: the `LaunchedEffect(Unit)` prefill block in `BusinessSetupShell`; assertions in
 * `BusinessSetupPrefillTest`.
 */
object BusinessSetupPrefill {
    /**
     * @param key preference key being prefilled (e.g. `businessName`) — never renamed, stored prefs depend on it.
     * @param dirty keys the user has already edited through a field control.
     * @param loaded the value returned by the draft load.
     * @param current the in-memory value for [key] right now.
     * @return `true` only when the field is untouched and empty and the loaded value is non-blank.
     */
    fun shouldApply(
        key: String,
        dirty: Set<String>,
        loaded: String?,
        current: String? = null,
    ): Boolean {
        if (key in dirty) return false
        val incoming = loaded?.trim().orEmpty()
        // Blank / whitespace-only draft values are not worth writing.
        if (incoming.isEmpty()) return false
        val existing = current?.trim().orEmpty()
        // A non-empty current value is never replaced (this also covers incoming == existing).
        if (existing.isNotEmpty()) return false
        return true
    }
}

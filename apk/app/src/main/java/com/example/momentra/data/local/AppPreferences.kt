package com.example.momentra.data.local

import android.content.Context

class AppPreferences(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isOnboardingSeen(): Boolean =
        prefs.getBoolean(KEY_ONBOARDING_SEEN, false)

    fun setOnboardingSeen(seen: Boolean) {
        prefs.edit().putBoolean(KEY_ONBOARDING_SEEN, seen).apply()
    }

    fun isConsentGateSeen(): Boolean =
        prefs.getBoolean(KEY_CONSENT_GATE_SEEN, false)

    fun setConsentGateSeen(seen: Boolean) {
        prefs.edit().putBoolean(KEY_CONSENT_GATE_SEEN, seen).apply()
    }

    fun getOrCreateTelemetryAnonymousId(): String {
        val existing = prefs.getString(KEY_TELEMETRY_ANON_ID, null)
        if (existing != null) return existing
        val created = java.util.UUID.randomUUID().toString()
        prefs.edit().putString(KEY_TELEMETRY_ANON_ID, created).apply()
        return created
    }

    fun getOrCreateTelemetrySessionId(): String {
        val existing = prefs.getString(KEY_TELEMETRY_SESSION_ID, null)
        if (existing != null) return existing
        return setTelemetrySessionId(java.util.UUID.randomUUID().toString())
    }

    fun setTelemetrySessionId(sessionId: String): String {
        prefs.edit().putString(KEY_TELEMETRY_SESSION_ID, sessionId).apply()
        return sessionId
    }

    fun getSelectedPersonalMomentId(userId: String): String? =
        prefs.getString(selectedPersonalMomentKey(userId), null)

    fun setSelectedPersonalMomentId(userId: String, momentId: String?) {
        prefs.edit().apply {
            if (momentId.isNullOrBlank()) {
                remove(selectedPersonalMomentKey(userId))
            } else {
                putString(selectedPersonalMomentKey(userId), momentId)
            }
        }.apply()
    }

    fun getPendingJoinCode(): String? =
        prefs.getString(KEY_PENDING_JOIN_CODE, null)

    fun setPendingJoinCode(code: String) {
        prefs.edit().putString(KEY_PENDING_JOIN_CODE, code).apply()
    }

    fun clearPendingJoinCode() {
        prefs.edit().remove(KEY_PENDING_JOIN_CODE).apply()
    }

    /**
     * Last known Momentra identity for a Firebase UID.
     * Never store Firebase UID as Momentra userId — only cache server-issued UUIDv5.
     */
    fun saveCachedIdentity(
        firebaseUid: String,
        userId: String,
        displayName: String?,
        email: String?,
    ) {
        if (userId.isBlank() || userId == firebaseUid) return
        prefs.edit()
            .putString(identityUserIdKey(firebaseUid), userId)
            .putString(identityDisplayNameKey(firebaseUid), displayName)
            .putString(identityEmailKey(firebaseUid), email)
            .apply()
    }

    fun getCachedIdentity(firebaseUid: String): Triple<String, String?, String?>? {
        val userId = prefs.getString(identityUserIdKey(firebaseUid), null) ?: return null
        if (userId.isBlank() || userId == firebaseUid) return null
        return Triple(
            userId,
            prefs.getString(identityDisplayNameKey(firebaseUid), null),
            prefs.getString(identityEmailKey(firebaseUid), null),
        )
    }

    fun clearCachedIdentity(firebaseUid: String?) {
        if (firebaseUid.isNullOrBlank()) return
        prefs.edit()
            .remove(identityUserIdKey(firebaseUid))
            .remove(identityDisplayNameKey(firebaseUid))
            .remove(identityEmailKey(firebaseUid))
            .apply()
    }

    fun getShellContext(userId: String): String? =
        prefs.getString(shellContextKey(userId), null)

    fun setShellContext(userId: String, context: String) {
        prefs.edit().putString(shellContextKey(userId), context).apply()
    }

    fun getShellCompanyId(userId: String): String? =
        prefs.getString(shellCompanyKey(userId), null)

    fun setShellCompanyId(userId: String, companyId: String?) {
        prefs.edit().apply {
            if (companyId.isNullOrBlank()) remove(shellCompanyKey(userId))
            else putString(shellCompanyKey(userId), companyId)
        }.apply()
    }

    fun clearUserScopedShell(userId: String?) {
        if (userId.isNullOrBlank()) return
        prefs.edit()
            .remove(shellContextKey(userId))
            .remove(shellCompanyKey(userId))
            .remove(selectedPersonalMomentKey(userId))
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "momentra_app_prefs"
        private const val KEY_ONBOARDING_SEEN = "momentra_onboarding_seen"
        private const val KEY_CONSENT_GATE_SEEN = "momentra_consent_gate_seen"
        private const val KEY_TELEMETRY_ANON_ID = "telemetry_anonymous_id"
        private const val KEY_TELEMETRY_SESSION_ID = "telemetry_session_id"
        private const val KEY_PENDING_JOIN_CODE = "pending_join_code"

        private fun selectedPersonalMomentKey(userId: String): String =
            "selected_personal_moment_$userId"

        private fun identityUserIdKey(firebaseUid: String) = "identity_user_id_$firebaseUid"
        private fun identityDisplayNameKey(firebaseUid: String) = "identity_display_name_$firebaseUid"
        private fun identityEmailKey(firebaseUid: String) = "identity_email_$firebaseUid"
        private fun shellContextKey(userId: String) = "shell_context_$userId"
        private fun shellCompanyKey(userId: String) = "shell_company_$userId"
    }
}

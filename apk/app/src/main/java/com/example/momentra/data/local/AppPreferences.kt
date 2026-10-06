package com.example.momentra.data.local

import android.content.Context
import com.example.momentra.ui.shell.policy.decodeBusinessMoments
import com.example.momentra.ui.shell.policy.encodeBusinessMoments

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

    fun isAgeGateAccepted(): Boolean =
        prefs.getBoolean(KEY_AGE_GATE_ACCEPTED, false)

    fun setAgeGateAccepted(accepted: Boolean) {
        prefs.edit().putBoolean(KEY_AGE_GATE_ACCEPTED, accepted).apply()
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

    fun getPendingCompanyJoinCode(): String? =
        prefs.getString(KEY_PENDING_COMPANY_JOIN_CODE, null)

    fun setPendingCompanyJoinCode(code: String) {
        prefs.edit().putString(KEY_PENDING_COMPANY_JOIN_CODE, code).apply()
    }

    fun clearPendingCompanyJoinCode() {
        prefs.edit().remove(KEY_PENDING_COMPANY_JOIN_CODE).apply()
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

    fun getShellBusinessMomentId(userId: String, companyId: String): String? {
        if (userId.isBlank() || companyId.isBlank()) return null
        return decodeBusinessMoments(prefs.getString(shellBusinessMomentsKey(userId), null))[companyId]
    }

    fun setShellBusinessMomentId(userId: String, companyId: String, momentId: String?) {
        if (userId.isBlank() || companyId.isBlank()) return
        val next = decodeBusinessMoments(prefs.getString(shellBusinessMomentsKey(userId), null)).toMutableMap()
        if (momentId.isNullOrBlank()) next.remove(companyId) else next[companyId] = momentId
        prefs.edit().apply {
            if (next.isEmpty()) remove(shellBusinessMomentsKey(userId))
            else putString(shellBusinessMomentsKey(userId), encodeBusinessMoments(next))
        }.apply()
    }

    fun clearUserScopedShell(userId: String?) {
        if (userId.isNullOrBlank()) return
        prefs.edit()
            .remove(shellContextKey(userId))
            .remove(shellCompanyKey(userId))
            .remove(shellBusinessMomentsKey(userId))
            .remove(selectedPersonalMomentKey(userId))
            .apply()
    }

    fun isTourPersonalDone(): Boolean =
        prefs.getBoolean(KEY_TOUR_PERSONAL_V1, false)

    fun setTourPersonalDone(done: Boolean) {
        prefs.edit().putBoolean(KEY_TOUR_PERSONAL_V1, done).apply()
    }

    fun isTourGroupMiniDone(): Boolean =
        prefs.getBoolean(KEY_TOUR_GROUP_MINI_V1, false)

    fun setTourGroupMiniDone(done: Boolean) {
        prefs.edit().putBoolean(KEY_TOUR_GROUP_MINI_V1, done).apply()
    }

    fun isTourBusinessMiniDone(): Boolean =
        prefs.getBoolean(KEY_TOUR_BUSINESS_MINI_V1, false)

    fun setTourBusinessMiniDone(done: Boolean) {
        prefs.edit().putBoolean(KEY_TOUR_BUSINESS_MINI_V1, done).apply()
    }

    fun isTourPostQaHintSeen(): Boolean =
        prefs.getBoolean(KEY_TOUR_POST_QA_HINT, false)

    fun setTourPostQaHintSeen(seen: Boolean) {
        prefs.edit().putBoolean(KEY_TOUR_POST_QA_HINT, seen).apply()
    }

    fun isPersonalSimpleMode(): Boolean =
        prefs.getBoolean(KEY_PERSONAL_SIMPLE_MODE, false)

    fun setPersonalSimpleMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_PERSONAL_SIMPLE_MODE, enabled).apply()
    }

    fun isPersonalEveningNudgeEnabled(): Boolean =
        prefs.getBoolean(KEY_PERSONAL_EVENING_NUDGE, false)

    fun setPersonalEveningNudgeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_PERSONAL_EVENING_NUDGE, enabled).apply()
    }

    fun hasPersonalTodaySave(): Boolean =
        prefs.getBoolean(KEY_PERSONAL_TODAY_SAVE, false)

    fun markPersonalTodaySave() {
        prefs.edit().putBoolean(KEY_PERSONAL_TODAY_SAVE, true).apply()
    }

    fun isPersonalSimpleCtaDismissed(): Boolean =
        prefs.getBoolean(KEY_PERSONAL_SIMPLE_CTA_DISMISSED, false)

    fun setPersonalSimpleCtaDismissed(dismissed: Boolean) {
        prefs.edit().putBoolean(KEY_PERSONAL_SIMPLE_CTA_DISMISSED, dismissed).apply()
    }

    fun resetShellTours() {
        prefs.edit()
            .putBoolean(KEY_TOUR_PERSONAL_V1, false)
            .putBoolean(KEY_TOUR_GROUP_MINI_V1, false)
            .putBoolean(KEY_TOUR_BUSINESS_MINI_V1, false)
            .putBoolean(KEY_TOUR_POST_QA_HINT, false)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "momentra_app_prefs"
        private const val KEY_ONBOARDING_SEEN = "momentra_onboarding_seen"
        private const val KEY_CONSENT_GATE_SEEN = "momentra_consent_gate_seen"
        private const val KEY_AGE_GATE_ACCEPTED = "momentra_age_gate_accepted_13plus"
        private const val KEY_TELEMETRY_ANON_ID = "telemetry_anonymous_id"
        private const val KEY_TELEMETRY_SESSION_ID = "telemetry_session_id"
        private const val KEY_PENDING_JOIN_CODE = "pending_join_code"
        private const val KEY_PENDING_COMPANY_JOIN_CODE = "pending_company_join_code"
        private const val KEY_TOUR_PERSONAL_V1 = "tour_personal_v1_done"
        private const val KEY_TOUR_GROUP_MINI_V1 = "tour_group_mini_v1_done"
        private const val KEY_TOUR_BUSINESS_MINI_V1 = "tour_business_mini_v1_done"
        private const val KEY_TOUR_POST_QA_HINT = "tour_post_qa_hint_seen"
        private const val KEY_PERSONAL_SIMPLE_MODE = "personal_simple_mode"
        private const val KEY_PERSONAL_EVENING_NUDGE = "personal_evening_nudge_enabled"
        private const val KEY_PERSONAL_TODAY_SAVE = "personal_today_save_seen"
        private const val KEY_PERSONAL_SIMPLE_CTA_DISMISSED = "personal_simple_cta_dismissed"

        private fun selectedPersonalMomentKey(userId: String): String =
            "selected_personal_moment_$userId"

        private fun identityUserIdKey(firebaseUid: String) = "identity_user_id_$firebaseUid"
        private fun identityDisplayNameKey(firebaseUid: String) = "identity_display_name_$firebaseUid"
        private fun identityEmailKey(firebaseUid: String) = "identity_email_$firebaseUid"
        private fun shellContextKey(userId: String) = "shell_context_$userId"
        private fun shellCompanyKey(userId: String) = "shell_company_$userId"
        private fun shellBusinessMomentsKey(userId: String) = "shell_business_moments_$userId"
    }
}

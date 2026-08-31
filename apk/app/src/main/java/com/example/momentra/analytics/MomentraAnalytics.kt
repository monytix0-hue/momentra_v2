package com.example.momentra.analytics

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.example.momentra.data.api.TelemetryUserSnapshotDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

/**
 * Screen timing, session tracking, demographics, and widget taps via Firebase Analytics.
 * Emits [screen_tick] every second while a screen is foregrounded.
 */
class MomentraAnalytics private constructor(
    private val firebaseAnalytics: FirebaseAnalytics,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val sessionStartedAtMs = AtomicLong(System.currentTimeMillis())
    private val sessionForegroundMs = AtomicLong(0L)
    private var sessionActive = true
    private var tickJob: Job? = null

    private val currentScreen = AtomicReference<String?>(null)
    private val currentScreenEnteredAtMs = AtomicLong(0L)
    private val lastInteractionAtMs = AtomicLong(System.currentTimeMillis())
    private var stuckReported = false

    fun onAppForeground() {
        if (sessionActive) return
        sessionActive = true
        sessionStartedAtMs.set(System.currentTimeMillis())
        BackendTelemetry.get().onSessionStart()
        logEvent("session_start", bundleOf("platform" to "android"))
        currentScreen.get()?.let { resumeScreenTicks(it) }
    }

    fun onAppBackground() {
        if (!sessionActive) return
        val foregroundSec = foregroundSeconds()
        logEvent(
            "session_end",
            bundleOf(
                "platform" to "android",
                "duration_sec" to foregroundSec,
            ),
        )
        BackendTelemetry.get().onSessionEnd()
        sessionActive = false
        stopScreenTicks()
        currentScreen.get()?.let { screen ->
            logScreenExit(screen, reason = "app_background")
        }
    }

    fun onScreenEnter(screenName: String, screenClass: String = screenName) {
        currentScreen.get()?.let { previous ->
            if (previous != screenName) logScreenExit(previous, reason = "navigate")
        }
        currentScreen.set(screenName)
        currentScreenEnteredAtMs.set(System.currentTimeMillis())
        stuckReported = false
        markInteraction()
        firebaseAnalytics.logEvent(
            FirebaseAnalytics.Event.SCREEN_VIEW,
            bundleOf(
                FirebaseAnalytics.Param.SCREEN_NAME to screenName,
                FirebaseAnalytics.Param.SCREEN_CLASS to screenClass,
            ),
        )
        logEvent(
            "screen_enter",
            bundleOf(
                "screen_name" to screenName,
                "screen_class" to screenClass,
            ),
        )
        resumeScreenTicks(screenName)
    }

    fun onScreenExit(screenName: String) {
        if (currentScreen.get() != screenName) return
        logScreenExit(screenName, reason = "dispose")
        currentScreen.set(null)
        stopScreenTicks()
    }

    fun trackWidget(screenName: String, widgetName: String, action: String = "tap") {
        markInteraction()
        logEvent(
            "widget_interaction",
            bundleOf(
                "screen_name" to screenName,
                "widget_name" to widgetName,
                "action" to action,
            ),
        )
    }

    fun trackAuthResult(method: String, success: Boolean, errorCode: String? = null) {
        logEvent(
            if (success) "auth_success" else "auth_error",
            bundleOf(
                "method" to method,
                "success" to if (success) 1L else 0L,
                "error_code" to (errorCode ?: ""),
            ),
        )
        if (success) syncUserDemographics(FirebaseAuth.getInstance().currentUser)
    }

    fun syncUserDemographics(
        user: FirebaseUser?,
        profileDisplayName: String? = null,
        profileEmail: String? = null,
        profileAge: String? = null,
        profileSex: String? = null,
    ) {
        if (user == null) {
            firebaseAnalytics.setUserId(null)
            return
        }
        firebaseAnalytics.setUserId(user.uid)
        val name = profileDisplayName ?: user.displayName ?: ""
        val email = profileEmail ?: user.email ?: ""
        val phone = user.phoneNumber ?: ""
        val photoUrl = user.photoUrl?.toString() ?: ""
        setUserProperty("user_name", name.take(100))
        setUserProperty("user_email", email.take(100))
        setUserProperty("user_phone", phone.take(100))
        setUserProperty("has_photo", if (photoUrl.isNotBlank()) "yes" else "no")
        setUserProperty("photo_url", photoUrl.take(100))
        setUserProperty("user_sex", (profileSex ?: "unknown").take(100))
        setUserProperty("auth_providers", user.providerData.joinToString(",") { it.providerId }.take(100))
        setUserProperty("user_age", (profileAge ?: "unknown").take(100))
        BackendTelemetry.get().updateUserSnapshot(
            TelemetryUserSnapshotDto(
                userName = name.takeIf { it.isNotBlank() },
                userEmail = email.takeIf { it.isNotBlank() },
                userPhone = phone.takeIf { it.isNotBlank() },
                userAge = profileAge ?: "unknown",
                userSex = profileSex ?: "unknown",
                hasPhoto = photoUrl.isNotBlank(),
                photoUrl = photoUrl.takeIf { it.isNotBlank() },
                authProviders = user.providerData.joinToString(",") { it.providerId }.takeIf { it.isNotBlank() },
            ),
        )
    }

    fun markInteraction() {
        lastInteractionAtMs.set(System.currentTimeMillis())
        stuckReported = false
    }

    private fun logScreenExit(screenName: String, reason: String) {
        val durationMs = System.currentTimeMillis() - currentScreenEnteredAtMs.get()
        val durationSec = (durationMs / 1000).coerceAtLeast(0)
        logEvent(
            "screen_exit",
            bundleOf(
                "screen_name" to screenName,
                "duration_ms" to durationMs,
                "duration_sec" to durationSec,
                "reason" to reason,
            ),
        )
    }

    private fun resumeScreenTicks(screenName: String) {
        stopScreenTicks()
        tickJob = scope.launch {
            while (isActive && currentScreen.get() == screenName && sessionActive) {
                delay(TICK_MS)
                val now = System.currentTimeMillis()
                val screenElapsedSec = ((now - currentScreenEnteredAtMs.get()) / 1000).coerceAtLeast(0)
                val sessionElapsedSec = ((now - sessionStartedAtMs.get()) / 1000).coerceAtLeast(0)
                val idleSec = ((now - lastInteractionAtMs.get()) / 1000).coerceAtLeast(0)
                logEvent(
                    "screen_tick",
                    bundleOf(
                        "screen_name" to screenName,
                        "screen_elapsed_sec" to screenElapsedSec,
                        "session_elapsed_sec" to sessionElapsedSec,
                        "idle_sec" to idleSec,
                    ),
                )
                if (idleSec >= STUCK_IDLE_SEC && !stuckReported) {
                    stuckReported = true
                    logEvent(
                        "screen_stuck",
                        bundleOf(
                            "screen_name" to screenName,
                            "idle_sec" to idleSec,
                            "screen_elapsed_sec" to screenElapsedSec,
                        ),
                    )
                }
            }
        }
    }

    private fun stopScreenTicks() {
        tickJob?.cancel()
        tickJob = null
    }

    private fun foregroundSeconds(): Long =
        ((System.currentTimeMillis() - sessionStartedAtMs.get()) / 1000).coerceAtLeast(0)

    private fun setUserProperty(name: String, value: String) {
        if (value.isBlank()) return
        firebaseAnalytics.setUserProperty(name, value)
    }

    private fun logSessionStart() {
        logEvent("session_start", bundleOf("platform" to "android"))
    }

    private fun logEvent(name: String, params: Bundle) {
        firebaseAnalytics.logEvent(name, params)
        if (name == "screen_tick") return
        runCatching {
            BackendTelemetry.get().enqueue(name, bundleToMap(params))
        }
    }

    private fun bundleToMap(bundle: Bundle): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()
        for (key in bundle.keySet()) {
            map[key] = when (val value = bundle.get(key)) {
                is String, is Long, is Int, is Double, is Boolean -> value
                else -> value?.toString()
            }
        }
        return map
    }

    internal fun bundleOf(vararg pairs: Pair<String, Any>): Bundle {
        val bundle = Bundle()
        for ((key, value) in pairs) {
            when (value) {
                is String -> bundle.putString(key, value)
                is Long -> bundle.putLong(key, value)
                is Int -> bundle.putInt(key, value)
                is Double -> bundle.putDouble(key, value)
                is Boolean -> bundle.putInt(key, if (value) 1 else 0)
            }
        }
        return bundle
    }

    companion object {
        private const val TICK_MS = 1_000L
        private const val STUCK_IDLE_SEC = 30L

        @Volatile
        private var instance: MomentraAnalytics? = null

        fun init(context: Context): MomentraAnalytics {
            return instance ?: synchronized(this) {
                instance ?: MomentraAnalytics(FirebaseAnalytics.getInstance(context.applicationContext))
                    .also { created ->
                        created.logSessionStart()
                        instance = created
                    }
            }
        }

        fun get(): MomentraAnalytics =
            instance ?: error("MomentraAnalytics.init() must be called from Application.onCreate")
    }
}

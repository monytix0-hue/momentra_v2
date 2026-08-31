package com.example.momentra.observability

import android.util.Log
import com.example.momentra.BuildConfig

/**
 * Optional Sentry bootstrap. No-op when [BuildConfig.SENTRY_DSN] is blank.
 * Full SDK wiring can replace the log path without changing call sites.
 */
object SentryBootstrap {
    private const val TAG = "SentryBootstrap"
    private var initialized = false

    fun init() {
        if (initialized) return
        val dsn = BuildConfig.SENTRY_DSN.trim()
        if (dsn.isEmpty()) {
            Log.d(TAG, "SENTRY_DSN unset — Sentry no-op")
            return
        }
        // SDK dependency is optional in S0; when DSN is set, install io.sentry:sentry-android
        // and replace this branch with SentryAndroid.init. Until then, record intent only.
        Log.i(TAG, "SENTRY_DSN present — install sentry-android to enable capture")
        initialized = true
    }

    fun captureException(throwable: Throwable) {
        if (!initialized) return
        Log.e(TAG, "captureException (SDK not linked)", throwable)
    }
}

package com.example.momentra.data.local

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Persisted push deep link + optional userNotificationId for open attribution. */
object PendingDeepLink {
    private const val PREFS = "momentra_pending_deeplink"
    private const val KEY = "link"
    private const val KEY_NOTIF = "user_notification_id"

    data class Pending(val link: String, val userNotificationId: String?)

    private val _link = MutableStateFlow<String?>(null)
    val link: StateFlow<String?> = _link.asStateFlow()

    private var pendingNotificationId: String? = null

    fun offer(link: String, context: Context? = null, userNotificationId: String? = null) {
        val trimmed = link.trim()
        if (trimmed.isEmpty()) return
        _link.value = trimmed
        if (!userNotificationId.isNullOrBlank()) {
            pendingNotificationId = userNotificationId.trim()
        }
        context?.applicationContext
            ?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            ?.edit()
            ?.putString(KEY, trimmed)
            ?.also { editor ->
                if (!userNotificationId.isNullOrBlank()) {
                    editor.putString(KEY_NOTIF, userNotificationId.trim())
                }
            }
            ?.apply()
    }

    fun hydrate(link: String?) {
        if (_link.value.isNullOrBlank() && !link.isNullOrBlank()) {
            _link.value = link
        }
    }

    fun hydrateFromDisk(context: Context) {
        if (!_link.value.isNullOrBlank()) return
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val stored = prefs.getString(KEY, null)
        if (!stored.isNullOrBlank()) _link.value = stored
        if (pendingNotificationId.isNullOrBlank()) {
            pendingNotificationId = prefs.getString(KEY_NOTIF, null)
        }
    }

    fun consume(context: Context? = null): Pending? {
        val next = _link.value
        val notifId = pendingNotificationId
            ?: context?.applicationContext
                ?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                ?.getString(KEY_NOTIF, null)
        _link.value = null
        pendingNotificationId = null
        context?.applicationContext
            ?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            ?.edit()
            ?.remove(KEY)
            ?.remove(KEY_NOTIF)
            ?.apply()
        val link = next?.takeIf { it.isNotBlank() } ?: return null
        return Pending(link, notifId?.takeIf { it.isNotBlank() })
    }

    fun parseMomentId(raw: String): String? {
        val uri = android.net.Uri.parse(raw)
        if (uri.scheme?.equals("momentra", ignoreCase = true) != true) return null
        val host = uri.host?.lowercase().orEmpty()
        val segments = uri.pathSegments.orEmpty()
        if (host == "moment" && segments.isNotEmpty()) return segments.first()
        if (segments.firstOrNull() == "moment" && segments.size > 1) return segments[1]
        return null
    }

    fun parseCategory(raw: String): String? =
        android.net.Uri.parse(raw).getQueryParameter("category")

    fun parseEvent(raw: String): String? =
        android.net.Uri.parse(raw).getQueryParameter("event")
}

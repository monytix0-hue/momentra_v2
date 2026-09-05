package com.example.momentra.data.local

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** In-memory push deep link so a live session can open the target moment. */
object PendingDeepLink {
    private val _link = MutableStateFlow<String?>(null)
    val link: StateFlow<String?> = _link.asStateFlow()

    fun offer(link: String) {
        val trimmed = link.trim()
        if (trimmed.isEmpty()) return
        _link.value = trimmed
    }

    fun hydrate(link: String?) {
        if (_link.value.isNullOrBlank() && !link.isNullOrBlank()) {
            _link.value = link
        }
    }

    fun consume(): String? {
        val next = _link.value
        _link.value = null
        return next?.takeIf { it.isNotBlank() }
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
}

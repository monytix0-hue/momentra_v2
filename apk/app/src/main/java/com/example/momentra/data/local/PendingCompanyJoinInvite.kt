package com.example.momentra.data.local

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** In-memory company invite code so a live session can redeem a newly opened invite URL. */
object PendingCompanyJoinInvite {
    private val _code = MutableStateFlow<String?>(null)
    val code: StateFlow<String?> = _code.asStateFlow()

    fun offer(prefs: AppPreferences, code: String) {
        prefs.setPendingCompanyJoinCode(code)
        _code.value = code
    }

    fun hydrate(code: String) {
        if (_code.value.isNullOrBlank()) {
            _code.value = code
        }
    }

    fun consume(prefs: AppPreferences): String? {
        val next = _code.value ?: prefs.getPendingCompanyJoinCode()
        _code.value = null
        prefs.clearPendingCompanyJoinCode()
        return next?.takeIf { it.isNotBlank() }
    }
}

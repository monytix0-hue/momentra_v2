package com.example.momentra.data.local

import android.content.Context
import com.example.momentra.data.api.MeBootstrapDto
import com.google.gson.Gson

/** Stale-while-revalidate cache for GET /v1/me shell inventory, keyed by Momentra userId. */
class BootstrapCache(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val gson = Gson()

    fun save(userId: String, dto: MeBootstrapDto) {
        if (userId.isBlank()) return
        prefs.edit()
            .putString(key(userId), gson.toJson(dto))
            .putLong(savedAtKey(userId), System.currentTimeMillis())
            .apply()
    }

    fun savedAtMs(userId: String): Long? {
        if (userId.isBlank()) return null
        val at = prefs.getLong(savedAtKey(userId), -1L)
        return at.takeIf { it > 0L }
    }

    fun isFresh(userId: String, maxAgeMs: Long = 30_000L): Boolean {
        val at = savedAtMs(userId) ?: return false
        return System.currentTimeMillis() - at < maxAgeMs
    }

    fun load(userId: String): MeBootstrapDto? {
        if (userId.isBlank()) return null
        val raw = prefs.getString(key(userId), null) ?: return null
        return runCatching { gson.fromJson(raw, MeBootstrapDto::class.java) }.getOrNull()
    }

    fun clear(userId: String?) {
        if (userId.isNullOrBlank()) return
        prefs.edit().remove(key(userId)).apply()
    }

    companion object {
        private const val PREFS = "momentra_bootstrap_cache"
        private fun key(userId: String) = "bootstrap_$userId"
        private fun savedAtKey(userId: String) = "bootstrap_saved_at_$userId"
    }
}

package com.example.momentra.data.create

import android.content.Context
import java.util.UUID

/** Persists idempotency keys until a create command succeeds. */
class IdempotencyKeyStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun keyFor(draftKey: String): String =
        prefs.getString(draftKey, null) ?: UUID.randomUUID().toString().also { fresh ->
            prefs.edit().putString(draftKey, fresh).apply()
        }

    fun clear(draftKey: String) {
        prefs.edit().remove(draftKey).apply()
    }

    companion object {
        private const val PREFS = "moment_create_idempotency"
    }
}

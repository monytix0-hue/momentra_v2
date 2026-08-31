package com.example.momentra.data.operation

import android.content.Context
import java.util.UUID

class IdempotencyStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun nextClientOperationId(): String {
        val existing = prefs.getString(KEY_CLIENT_OPERATION_ID, null)
        if (!existing.isNullOrBlank()) return existing

        val generated = UUID.randomUUID().toString()
        prefs.edit().putString(KEY_CLIENT_OPERATION_ID, generated).apply()
        return generated
    }

    fun clearClientOperationId() {
        prefs.edit().remove(KEY_CLIENT_OPERATION_ID).apply()
    }

    companion object {
        private const val PREFS_NAME = "momentra_idempotency"
        private const val KEY_CLIENT_OPERATION_ID = "clientOperationId"
    }
}

package com.example.momentra.data.device

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.example.momentra.data.api.ApiClient
import com.example.momentra.data.api.RegisterDeviceBody
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import java.util.UUID

object DeviceRegistrar {
    private const val PREFS = "momentra_device"
    private const val KEY_DEVICE_ID = "device_id"
    private const val TAG = "DeviceRegistrar"

    fun deviceId(context: Context): String {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val existing = prefs.getString(KEY_DEVICE_ID, null)
        if (!existing.isNullOrBlank()) return existing
        val androidId = Settings.Secure.getString(
            context.applicationContext.contentResolver,
            Settings.Secure.ANDROID_ID,
        )
        val id = androidId?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
        prefs.edit().putString(KEY_DEVICE_ID, id).apply()
        return id
    }

    /**
     * Resolves the FCM token when [pushToken] is null, then POSTs `/me/devices`.
     * A tokenless registration is skipped — `onNewToken` re-registers once FCM issues one.
     */
    suspend fun register(context: Context, pushToken: String? = null): Result<Unit> = runCatching {
        val token = pushToken?.takeIf { it.isNotBlank() } ?: fetchFcmToken()
        if (token.isNullOrBlank()) {
            Log.w(TAG, "No FCM token yet; skipping device registration")
            return@runCatching
        }
        val id = deviceId(context)
        ApiClient.apiService.registerDevice(
            idempotencyKey = UUID.randomUUID().toString(),
            body = RegisterDeviceBody(
                deviceId = id,
                platform = "ANDROID",
                pushToken = token,
                appVersion = Build.VERSION.RELEASE,
            ),
        )
        Log.i(TAG, "Registered device with FCM token")
    }

    private suspend fun fetchFcmToken(): String? = runCatching {
        FirebaseMessaging.getInstance().token.await()
    }.onFailure {
        Log.w(TAG, "FCM token fetch failed", it)
    }.getOrNull()
}

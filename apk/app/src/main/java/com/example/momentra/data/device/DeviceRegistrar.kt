package com.example.momentra.data.device

import android.content.Context
import android.os.Build
import android.provider.Settings
import com.example.momentra.data.api.ApiClient
import com.example.momentra.data.api.RegisterDeviceBody
import java.util.UUID

object DeviceRegistrar {
    private const val PREFS = "momentra_device"
    private const val KEY_DEVICE_ID = "device_id"

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

    suspend fun register(context: Context, pushToken: String? = null): Result<Unit> = runCatching {
        val id = deviceId(context)
        ApiClient.apiService.registerDevice(
            idempotencyKey = UUID.randomUUID().toString(),
            body = RegisterDeviceBody(
                deviceId = id,
                platform = "ANDROID",
                pushToken = pushToken,
                appVersion = Build.VERSION.RELEASE,
            ),
        )
        Unit
    }
}

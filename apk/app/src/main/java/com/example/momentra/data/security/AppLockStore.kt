package com.example.momentra.data.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.MessageDigest
import java.util.UUID

/**
 * Local App Lock — PIN verifier never leaves the device.
 * Stores only a salted hash in EncryptedSharedPreferences (Keystore-backed master key).
 */
class AppLockStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs by lazy {
        val masterKey = MasterKey.Builder(appContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            appContext,
            "momentra_app_lock",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    fun isPinEnabled(): Boolean = !prefs.getString(KEY_PIN_HASH, null).isNullOrBlank()

    fun biometricsEnabled(): Boolean = prefs.getBoolean(KEY_BIOMETRICS, false)

    fun setBiometricsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRICS, enabled).apply()
    }

    fun autoLockSeconds(): Int = prefs.getInt(KEY_AUTO_LOCK_SEC, 60)

    fun setAutoLockSeconds(seconds: Int) {
        prefs.edit().putInt(KEY_AUTO_LOCK_SEC, seconds.coerceIn(0, 3600)).apply()
    }

    fun setPin(pin: String) {
        require(pin.length in 4..8 && pin.all { it.isDigit() }) { "PIN must be 4–8 digits" }
        val salt = prefs.getString(KEY_SALT, null) ?: UUID.randomUUID().toString().also {
            prefs.edit().putString(KEY_SALT, it).apply()
        }
        prefs.edit().putString(KEY_PIN_HASH, hash(pin, salt)).apply()
    }

    fun verifyPin(pin: String): Boolean {
        val salt = prefs.getString(KEY_SALT, null) ?: return false
        val expected = prefs.getString(KEY_PIN_HASH, null) ?: return false
        return expected == hash(pin, salt)
    }

    fun clearPin() {
        prefs.edit()
            .remove(KEY_PIN_HASH)
            .remove(KEY_SALT)
            .putBoolean(KEY_BIOMETRICS, false)
            .apply()
    }

    fun clearForLogout() {
        // Keep lock settings device-local; clear unlock session only via AppLockSession.
    }

    private fun hash(pin: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest("$salt:$pin".toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_SALT = "pin_salt"
        private const val KEY_BIOMETRICS = "biometrics_enabled"
        private const val KEY_AUTO_LOCK_SEC = "auto_lock_sec"
    }
}

/** In-memory unlock state for the process. */
object AppLockSession {
    @Volatile
    var unlocked: Boolean = false
        private set

    @Volatile
    var lastBackgroundAtMs: Long = 0L
        private set

    fun markUnlocked() {
        unlocked = true
    }

    fun markLocked() {
        unlocked = false
    }

    fun onBackground() {
        lastBackgroundAtMs = System.currentTimeMillis()
    }

    fun shouldRelock(autoLockSeconds: Int): Boolean {
        if (!unlocked) return true
        if (autoLockSeconds <= 0) return true
        val elapsed = System.currentTimeMillis() - lastBackgroundAtMs
        return elapsed >= autoLockSeconds * 1000L
    }
}

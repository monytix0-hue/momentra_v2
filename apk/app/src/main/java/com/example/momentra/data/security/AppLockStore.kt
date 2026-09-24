package com.example.momentra.data.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.KeyStore
import java.security.MessageDigest
import java.util.UUID
import javax.crypto.AEADBadTagException

/**
 * Local App Lock — PIN verifier never leaves the device.
 * Stores only a salted hash in EncryptedSharedPreferences (Keystore-backed master key).
 *
 * If Keystore/master-key decryption fails (reinstall, OEM keystore wipe, AEADBadTag),
 * keep the encrypted prefs and stay locked. Sign-out is what clears the store.
 */
class AppLockStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs: SharedPreferences by lazy { openOrRecoverPrefs() }

    val lockUnavailable: Boolean
        get() = AppLockSession.lockUnavailable

    private fun openOrRecoverPrefs(): SharedPreferences {
        return try {
            createEncryptedPrefs()
        } catch (e: Throwable) {
            if (!isCorruptCrypto(e)) throw e
            Log.w(TAG, "Encrypted app-lock prefs unreadable; staying locked. ${e.javaClass.simpleName}: ${e.message}")
            AppLockSession.markLockUnavailable()
            UnavailableLockPrefs
        }
    }

    private fun createEncryptedPrefs(): SharedPreferences {
        val masterKey = MasterKey.Builder(appContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            appContext,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    private fun wipeCorruptLockStorage() {
        // Delete encrypted prefs file(s) under the app's shared_prefs.
        runCatching {
            appContext.deleteSharedPreferences(PREFS_NAME)
        }
        runCatching {
            val dir = java.io.File(appContext.applicationInfo.dataDir, "shared_prefs")
            listOf(
                "$PREFS_NAME.xml",
                "__androidx_security_crypto_encrypted_prefs_key_keyset__$PREFS_NAME.xml",
                "__androidx_security_crypto_encrypted_prefs_value_keyset__$PREFS_NAME.xml",
            ).forEach { name ->
                java.io.File(dir, name).delete()
            }
        }
        // Drop the default MasterKey alias so a fresh keyset can be minted.
        runCatching {
            val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            if (keyStore.containsAlias(MasterKey.DEFAULT_MASTER_KEY_ALIAS)) {
                keyStore.deleteEntry(MasterKey.DEFAULT_MASTER_KEY_ALIAS)
            }
        }
        // Also clear any leftover MasterKey prefs used by security-crypto.
        runCatching {
            appContext.getSharedPreferences(
                MasterKey.DEFAULT_MASTER_KEY_ALIAS,
                Context.MODE_PRIVATE,
            ).edit().clear().commit()
            appContext.deleteSharedPreferences(MasterKey.DEFAULT_MASTER_KEY_ALIAS)
        }
    }

    private fun isCorruptCrypto(t: Throwable): Boolean {
        var cur: Throwable? = t
        while (cur != null) {
            when (cur) {
                is AEADBadTagException,
                is javax.crypto.BadPaddingException,
                is java.security.GeneralSecurityException,
                -> return true
            }
            val msg = cur.message.orEmpty()
            if (msg.contains("VERIFICATION_FAILED", ignoreCase = true) ||
                msg.contains("AEADBadTag", ignoreCase = true) ||
                msg.contains("KeystoreException", ignoreCase = true) ||
                msg.contains("Signature/MAC verification failed", ignoreCase = true)
            ) {
                return true
            }
            cur = cur.cause
        }
        return false
    }

    fun isPinEnabled(): Boolean {
        if (AppLockSession.lockUnavailable) return true
        return !prefs.getString(KEY_PIN_HASH, null).isNullOrBlank()
    }

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
        // Keep a healthy lock device-local. A broken store is wiped only after sign-out.
    }

    /** Sign-out is the only way past an unreadable PIN store. */
    fun wipeAfterFailedLockSignOut() {
        wipeCorruptLockStorage()
        AppLockSession.clearLockUnavailable()
    }

    private fun hash(pin: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest("$salt:$pin".toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val TAG = "AppLockStore"
        private const val PREFS_NAME = "momentra_app_lock"
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_SALT = "pin_salt"
        private const val KEY_BIOMETRICS = "biometrics_enabled"
        private const val KEY_AUTO_LOCK_SEC = "auto_lock_sec"
    }
}

/** True when an unreadable PIN store must keep the signed-in shell closed. */
fun requiresAppLock(lockUnavailable: Boolean, pinEnabled: Boolean, unlocked: Boolean): Boolean =
    lockUnavailable || (pinEnabled && !unlocked)

/** In-memory unlock state for the process. */
object AppLockSession {
    @Volatile
    var unlocked: Boolean = false
        private set

    @Volatile
    var lockUnavailable: Boolean = false
        private set

    fun markLockUnavailable() {
        lockUnavailable = true
        unlocked = false
    }

    fun clearLockUnavailable() {
        lockUnavailable = false
    }

    @Volatile
    var lastBackgroundAtMs: Long = 0L
        private set

    fun markUnlocked() {
        if (lockUnavailable) return
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

/** Empty prefs used only while the encrypted store cannot be read. Nothing is written. */
private object UnavailableLockPrefs : SharedPreferences {
    override fun getAll(): MutableMap<String, *> = mutableMapOf<String, Any?>()
    override fun getString(key: String?, defValue: String?): String? = defValue
    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? = defValues
    override fun getInt(key: String?, defValue: Int): Int = defValue
    override fun getLong(key: String?, defValue: Long): Long = defValue
    override fun getFloat(key: String?, defValue: Float): Float = defValue
    override fun getBoolean(key: String?, defValue: Boolean): Boolean = defValue
    override fun contains(key: String?): Boolean = false
    override fun edit(): SharedPreferences.Editor = Editor
    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit
    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit

    private object Editor : SharedPreferences.Editor {
        override fun putString(key: String?, value: String?) = this
        override fun putStringSet(key: String?, values: MutableSet<String>?) = this
        override fun putInt(key: String?, value: Int) = this
        override fun putLong(key: String?, value: Long) = this
        override fun putFloat(key: String?, value: Float) = this
        override fun putBoolean(key: String?, value: Boolean) = this
        override fun remove(key: String?) = this
        override fun clear() = this
        override fun commit(): Boolean = false
        override fun apply() = Unit
    }
}

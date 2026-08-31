package com.example.momentra.data.security

import android.content.Context

/** Local presentation preferences — not AuthZ. */
class SecurityPreferences(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("momentra_security_prefs", Context.MODE_PRIVATE)

    fun hideBalances(): Boolean = prefs.getBoolean(KEY_HIDE_BALANCES, false)

    fun setHideBalances(hide: Boolean) {
        prefs.edit().putBoolean(KEY_HIDE_BALANCES, hide).apply()
    }

    fun notificationsEnabledLocal(): Boolean = prefs.getBoolean(KEY_NOTIF_LOCAL, true)

    fun setNotificationsEnabledLocal(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIF_LOCAL, enabled).apply()
    }

    fun clearUserScoped(userId: String?) {
        // Device-level prefs intentionally retained across accounts except hide-balances mask.
        prefs.edit().remove(KEY_HIDE_BALANCES).apply()
    }

    companion object {
        private const val KEY_HIDE_BALANCES = "hide_balances"
        private const val KEY_NOTIF_LOCAL = "notifications_local"
    }
}

object BalanceMask {
    fun mask(amountText: String, hide: Boolean): String =
        if (hide) "••••" else amountText
}

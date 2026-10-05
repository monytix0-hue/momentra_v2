package com.example.momentra.ui.shell.personal.shared

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.momentra.MainActivity
import com.example.momentra.R
import com.example.momentra.data.local.AppPreferences
import java.util.Calendar

/** Local 20:00 Personal evening check-in — no server. */
object PersonalEveningNudgeScheduler {
    const val CHANNEL_ID = "personal_evening_nudge"
    const val NOTIFICATION_ID = 4201
    const val ACTION_FIRE = "resolvingpoint.momentra.PERSONAL_EVENING_NUDGE"
    const val EXTRA_OPEN_PERSONAL_PULSE = "open_personal_pulse"

    fun sync(context: Context, enabled: Boolean) {
        val app = context.applicationContext
        ensureChannel(app)
        val am = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = pendingFire(app)
        am.cancel(pi)
        if (!enabled) return
        val trigger = nextEightPmMillis()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi)
        } else {
            @Suppress("DEPRECATION")
            am.set(AlarmManager.RTC_WAKEUP, trigger, pi)
        }
    }

    fun onTodaySave(context: Context) {
        val prefs = AppPreferences(context)
        prefs.markPersonalTodaySave()
        if (!prefs.isPersonalEveningNudgeEnabled()) {
            prefs.setPersonalEveningNudgeEnabled(true)
        }
        sync(context, prefs.isPersonalEveningNudgeEnabled())
    }

    fun showNotification(context: Context) {
        ensureChannel(context)
        val open = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_PERSONAL_PULSE, true)
        }
        val contentPi = PendingIntent.getActivity(
            context,
            0,
            open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("How was today?")
            .setContentText("Log a mood or rest in a tap.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(contentPi)
            .build()
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(NOTIFICATION_ID, notification)
        // Reschedule next day while still enabled
        if (AppPreferences(context).isPersonalEveningNudgeEnabled()) {
            sync(context, true)
        }
    }

    private fun pendingFire(context: Context): PendingIntent {
        val intent = Intent(context, PersonalEveningNudgeReceiver::class.java).setAction(ACTION_FIRE)
        return PendingIntent.getBroadcast(
            context,
            NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun nextEightPmMillis(): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 20)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        return cal.timeInMillis
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val mgr = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (mgr.getNotificationChannel(CHANNEL_ID) != null) return
        mgr.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Personal evening check-in",
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )
    }
}

class PersonalEveningNudgeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        when (action) {
            PersonalEveningNudgeScheduler.ACTION_FIRE -> {
                if (!AppPreferences(context).isPersonalEveningNudgeEnabled()) return
                PersonalEveningNudgeScheduler.showNotification(context)
            }
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            -> {
                if (AppPreferences(context).isPersonalEveningNudgeEnabled()) {
                    PersonalEveningNudgeScheduler.sync(context, true)
                }
            }
        }
    }
}

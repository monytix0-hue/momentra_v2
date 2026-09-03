package com.example.momentra.ui.shell.maestro

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import com.example.momentra.BuildConfig

/**
 * Debug-only receiver so Maestro can pin correlation / run ids before a write.
 * Action: com.example.momentra.QA_SET_CORRELATION
 */
class QaCorrelationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (!BuildConfig.DEBUG) return
        if (intent?.action != ACTION) return
        QaCorrelationHolder.setNextCorrelationId(intent.getStringExtra(EXTRA_CORRELATION))
        QaCorrelationHolder.setRunId(intent.getStringExtra(EXTRA_RUN_ID))
    }

    companion object {
        const val ACTION = "com.example.momentra.QA_SET_CORRELATION"
        const val EXTRA_CORRELATION = "correlation_id"
        const val EXTRA_RUN_ID = "run_id"

        fun register(context: Context) {
            if (!BuildConfig.DEBUG) return
            val filter = IntentFilter(ACTION)
            if (Build.VERSION.SDK_INT >= 33) {
                context.registerReceiver(QaCorrelationReceiver(), filter, Context.RECEIVER_EXPORTED)
            } else {
                @Suppress("UnspecifiedRegisterReceiverFlag")
                context.registerReceiver(QaCorrelationReceiver(), filter)
            }
        }
    }
}

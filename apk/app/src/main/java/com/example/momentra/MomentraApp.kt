package com.example.momentra

import android.app.Application
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.momentra.analytics.BackendTelemetry
import com.example.momentra.analytics.MomentraAnalytics
import com.example.momentra.observability.SentryBootstrap
import com.example.momentra.ui.shell.maestro.QaCorrelationReceiver
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseNetworkException

class MomentraApp : Application() {
    override fun onCreate() {
        super.onCreate()
        installFirebaseNetworkGuard()
        SentryBootstrap.init()
        FirebaseApp.initializeApp(this)
        QaCorrelationReceiver.register(this)
        BackendTelemetry.init(this)
        MomentraAnalytics.init(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                MomentraAnalytics.get().onAppForeground()
            }

            override fun onStop(owner: LifecycleOwner) {
                MomentraAnalytics.get().onAppBackground()
            }
        })
    }

    /**
     * Firebase Auth can throw [FirebaseNetworkException] on OkHttp's dispatcher thread
     * (timeout / unreachable host) instead of completing the Task as a failure.
     * That becomes a process-killing uncaught exception; swallow it off the main thread.
     */
    private fun installFirebaseNetworkGuard() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            val network = throwable is FirebaseNetworkException ||
                throwable.cause is FirebaseNetworkException
            if (network && thread.name != "main") {
                Log.w(TAG, "Firebase network error on ${thread.name}", throwable)
                return@setDefaultUncaughtExceptionHandler
            }
            previous?.uncaughtException(thread, throwable)
        }
    }

    companion object {
        private const val TAG = "MomentraApp"
    }
}

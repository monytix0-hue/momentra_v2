package com.example.momentra

import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.momentra.data.local.AppPreferences
import com.example.momentra.data.local.PendingJoinInvite
import com.example.momentra.data.security.AppLockSession
import com.example.momentra.data.security.AppLockStore
import com.example.momentra.ui.AppRoot
import com.example.momentra.ui.shell.empty.group.GroupJoinLink
import com.example.momentra.ui.shell.perf.ShellPerf
import com.example.momentra.ui.theme.MomentraTheme

class MainActivity : FragmentActivity() {
    private val lockStore by lazy { AppLockStore(this) }
    private var coldLaunchMark: ShellPerf.Mark? = null

    @OptIn(ExperimentalComposeUiApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        coldLaunchMark = ShellPerf.start(if (savedInstanceState == null) "cold_launch" else "warm_process_recreate")
        super.onCreate(savedInstanceState)
        handleJoinIntent(intent)
        ProcessLifecycleOwner.get().lifecycle.addObserver(
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_STOP -> AppLockSession.onBackground()
                    Lifecycle.Event.ON_START -> {
                        ShellPerf.instant("foreground_resume", mapOf("uptimeMs" to SystemClock.uptimeMillis()))
                        if (lockStore.isPinEnabled() &&
                            AppLockSession.shouldRelock(lockStore.autoLockSeconds())
                        ) {
                            AppLockSession.markLocked()
                        }
                    }
                    else -> Unit
                }
            },
        )
        enableEdgeToEdge()
        setContent {
            MomentraTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .semantics {
                            testTagsAsResourceId = true
                        },
                ) {
                    AppRoot()
                }
            }
        }
        coldLaunchMark?.let { ShellPerf.end(it, mapOf("phase" to "setContent")) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleJoinIntent(intent)
    }

    private fun handleJoinIntent(intent: Intent?) {
        val uri = intent?.data ?: return
        val code = GroupJoinLink.parse(uri.toString()) ?: return
        PendingJoinInvite.offer(AppPreferences(this), code)
    }
}

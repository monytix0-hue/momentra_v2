package com.example.momentra.ui

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.momentra.analytics.AnalyticsScreens
import com.example.momentra.analytics.AnalyticsWidgets
import com.example.momentra.analytics.TrackScreen
import com.example.momentra.analytics.trackWidget
import com.example.momentra.data.local.AppPreferences
import com.example.momentra.data.local.PendingJoinInvite
import com.example.momentra.data.device.DeviceRegistrar
import com.example.momentra.data.repository.MeRepository
import com.example.momentra.data.security.AppLockSession
import com.example.momentra.data.security.AppLockStore
import com.example.momentra.domain.AuthPhase
import com.example.momentra.ui.auth.AuthViewModel
import com.example.momentra.ui.auth.BrandAuthScreen
import com.example.momentra.ui.auth.LoginScreen
import com.example.momentra.ui.onboarding.ConsentGateScreen
import com.example.momentra.ui.onboarding.OnboardingMode
import com.example.momentra.ui.onboarding.OnboardingScreen
import com.example.momentra.ui.security.AppLockGate
import com.example.momentra.ui.shell.AppShellScreen
import com.example.momentra.ui.shell.AppShellViewModel
import com.example.momentra.ui.splash.SplashScreen
import com.example.momentra.ui.theme.MomentraBrandColors
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@Composable
fun AppRoot() {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val authViewModel: AuthViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AuthViewModel(application) as T
            }
        },
    )
    val prefs = remember { AppPreferences(context) }
    val lockStore = remember { AppLockStore(context) }
    var lockTick by remember { mutableStateOf(0) }
    val shellViewModel: AppShellViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AppShellViewModel(
                    meRepository = MeRepository(context.applicationContext),
                    prefs = AppPreferences(context.applicationContext),
                ) as T
            }
        },
    )
    var onboardingDone by remember { mutableStateOf(prefs.isOnboardingSeen()) }
    var consentAck by remember { mutableStateOf(prefs.isConsentGateSeen()) }
    val authState by authViewModel.state.collectAsState()

    var showSplash by remember { mutableStateOf(true) }
    var splashAnimationDone by remember { mutableStateOf(false) }
    val hadSessionOnLaunch = remember { FirebaseAuth.getInstance().currentUser != null }
    val scope = rememberCoroutineScope()

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            scope.launch { DeviceRegistrar.register(context.applicationContext) }
        }
    }

    LaunchedEffect(Unit) {
        prefs.getPendingJoinCode()?.let { PendingJoinInvite.hydrate(it) }
    }

    LaunchedEffect(authState.isLoggedIn) {
        if (!authState.isLoggedIn) return@LaunchedEffect
        if (Build.VERSION.SDK_INT >= 33) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                scope.launch { DeviceRegistrar.register(context.applicationContext) }
            }
        }
    }

    LaunchedEffect(authState.isRestoring, hadSessionOnLaunch) {
        if (hadSessionOnLaunch && !authState.isRestoring) {
            showSplash = false
        }
    }

    LaunchedEffect(splashAnimationDone, hadSessionOnLaunch) {
        if (splashAnimationDone && !hadSessionOnLaunch) {
            showSplash = false
        }
    }

    val needsLock = authState.isLoggedIn &&
        lockStore.isPinEnabled() &&
        !AppLockSession.unlocked

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            authState.isLoggedIn && authState.identity != null && needsLock -> {
                @Suppress("UNUSED_EXPRESSION")
                lockTick
                AppLockGate(lockStore = lockStore, onUnlocked = { lockTick++ })
            }
            authState.isLoggedIn && authState.identity != null -> {
                TrackScreen(AnalyticsScreens.HOME) {
                    AppShellScreen(
                        identity = authState.identity!!,
                        shellViewModel = shellViewModel,
                        onSignOut = {
                            shellViewModel.clearForLogout()
                            AppLockSession.markLocked()
                            authViewModel.signOut()
                        },
                        onSessionExpired = {
                            shellViewModel.clearForLogout()
                            authViewModel.onSessionExpired()
                        },
                    )
                }
            }
            authState.phase == AuthPhase.RestoringSession ||
                authState.phase == AuthPhase.AuthenticatedBootstrapping -> {
                TrackScreen(AnalyticsScreens.SESSION_RESTORE) {
                    BrandAuthScreen {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(color = MomentraBrandColors.Cta)
                        }
                    }
                }
            }
            !onboardingDone -> {
                OnboardingScreen(
                    mode = OnboardingMode.FirstRun,
                    onFinished = { onboardingDone = true },
                )
            }
            !consentAck -> {
                ConsentGateScreen(
                    onContinue = {
                        prefs.setConsentGateSeen(true)
                        consentAck = true
                    },
                )
            }
            else -> {
                LoginScreen(
                    authViewModel = authViewModel,
                    onLoggedIn = { },
                )
            }
        }

        AnimatedVisibility(
            visible = showSplash,
            exit = fadeOut(),
        ) {
            TrackScreen(AnalyticsScreens.SPLASH) {
                SplashScreen(
                    onFinish = {
                        splashAnimationDone = true
                        trackWidget(AnalyticsScreens.SPLASH, AnalyticsWidgets.SPLASH_COMPLETE, "auto")
                        if (!hadSessionOnLaunch) {
                            showSplash = false
                        }
                    },
                )
            }
        }
    }
}

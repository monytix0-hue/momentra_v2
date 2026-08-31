package com.example.momentra.ui.auth

import android.app.Activity
import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.momentra.analytics.MomentraAnalytics
import com.example.momentra.data.api.ApiClient
import com.example.momentra.data.api.ApiResultException
import com.example.momentra.data.auth.AuthErrorMapper
import com.example.momentra.data.auth.FirebaseAuthRepository
import com.example.momentra.data.auth.PhoneSendResult
import com.example.momentra.data.device.DeviceRegistrar
import com.example.momentra.data.local.AppPreferences
import com.example.momentra.data.repository.MeRepository
import com.example.momentra.data.security.SecurityPreferences
import com.example.momentra.domain.AuthPhase
import com.example.momentra.domain.ShellIdentity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val phase: AuthPhase = AuthPhase.Launching,
    val identity: ShellIdentity? = null,
    val phoneCodeSent: Boolean = false,
    val error: String? = null,
) {
    val isLoggedIn: Boolean get() = phase == AuthPhase.Authenticated && identity != null
    val isRestoring: Boolean get() = phase == AuthPhase.RestoringSession || phase == AuthPhase.AuthenticatedBootstrapping
    val isLoading: Boolean get() = phase == AuthPhase.Authenticating || phase == AuthPhase.AuthenticatedBootstrapping
}

class AuthViewModel @JvmOverloads constructor(
    application: Application,
    private val authRepository: FirebaseAuthRepository = FirebaseAuthRepository(),
    private val meRepository: MeRepository = MeRepository(application),
) : AndroidViewModel(application) {

    private val prefs = AppPreferences(application)
    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    init {
        try {
            restoreSessionIfPossible()
        } catch (e: Exception) {
            _state.update {
                it.copy(phase = AuthPhase.SignedOut, error = e.message)
            }
        }
    }

    private fun restoreSessionIfPossible() {
        if (authRepository.currentUser == null) {
            _state.update { it.copy(phase = AuthPhase.SignedOut) }
            return
        }
        _state.update { it.copy(phase = AuthPhase.RestoringSession) }
        viewModelScope.launch { bootstrapAfterAuth(restore = true) }
    }

    private suspend fun bootstrapAfterAuth(restore: Boolean = false) {
        _state.update {
            it.copy(
                phase = if (restore) AuthPhase.RestoringSession else AuthPhase.AuthenticatedBootstrapping,
                error = null,
            )
        }
        meRepository.getMe().fold(
            onSuccess = { identity ->
                authRepository.currentUser?.uid?.let { firebaseUid ->
                    prefs.saveCachedIdentity(
                        firebaseUid = firebaseUid,
                        userId = identity.userId,
                        displayName = identity.displayName,
                        email = identity.email,
                    )
                }
                MomentraAnalytics.get().syncUserDemographics(
                    user = authRepository.currentUser,
                    profileDisplayName = identity.displayName,
                    profileEmail = identity.email,
                )
                if (!restore) {
                    MomentraAnalytics.get().trackAuthResult("bootstrap", success = true)
                }
                ApiClient.warmAuthToken()
                viewModelScope.launch { DeviceRegistrar.register(getApplication()) }
                _state.update {
                    AuthUiState(
                        phase = AuthPhase.Authenticated,
                        identity = identity,
                        phoneCodeSent = false,
                        error = null,
                    )
                }
            },
            onFailure = { e ->
                when (e) {
                    is ApiResultException.Network -> {
                        // Offline: keep Firebase session; never substitute Firebase UID for Momentra userId.
                        if (restore && authRepository.currentUser != null) {
                            val firebaseUid = authRepository.currentUser?.uid
                            val cached = firebaseUid?.let { prefs.getCachedIdentity(it) }
                            if (cached != null) {
                                _state.update {
                                    AuthUiState(
                                        phase = AuthPhase.Authenticated,
                                        identity = ShellIdentity(
                                            userId = cached.first,
                                            displayName = cached.second,
                                            email = cached.third,
                                            firebaseUid = firebaseUid,
                                        ),
                                        error = "NETWORK_UNAVAILABLE",
                                    )
                                }
                            } else {
                                _state.update {
                                    AuthUiState(
                                        phase = AuthPhase.RestoringSession,
                                        identity = null,
                                        error = "NETWORK_UNAVAILABLE",
                                    )
                                }
                            }
                        } else {
                            failBootstrap(e, restore)
                        }
                    }
                    is ApiResultException.Unauthenticated -> {
                        authRepository.signOut()
                        _state.update {
                            AuthUiState(phase = AuthPhase.SessionExpired, error = e.message)
                        }
                    }
                    else -> failBootstrap(e, restore)
                }
            },
        )
    }

    private fun failBootstrap(e: Throwable, restore: Boolean) {
        authRepository.signOut()
        if (!restore) {
            MomentraAnalytics.get().trackAuthResult("bootstrap", success = false, errorCode = e.message)
        }
        _state.update {
            AuthUiState(
                phase = if (restore) AuthPhase.SignedOut else AuthPhase.AuthError,
                error = if (restore) null else (e.message ?: "Bootstrap failed"),
            )
        }
    }

    fun onFirebaseLoggedIn() {
        _state.update { it.copy(phase = AuthPhase.AuthenticatedBootstrapping, error = null) }
        viewModelScope.launch { bootstrapAfterAuth() }
    }

    fun onAuthStarted() {
        _state.update { it.copy(phase = AuthPhase.Authenticating, error = null) }
    }

    fun onAuthError(message: String) {
        _state.update {
            it.copy(phase = AuthPhase.AuthError, error = AuthErrorMapper.userMessage(message))
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    fun signInWithEmail(email: String, password: String) {
        _state.update { it.copy(phase = AuthPhase.Authenticating, error = null) }
        viewModelScope.launch {
            authRepository.signIn(email, password).fold(
                onSuccess = {
                    MomentraAnalytics.get().trackAuthResult("email_sign_in", success = true)
                    onFirebaseLoggedIn()
                },
                onFailure = { e ->
                    MomentraAnalytics.get().trackAuthResult("email_sign_in", success = false, errorCode = e.message)
                    _state.update {
                        it.copy(
                            phase = AuthPhase.AuthError,
                            error = AuthErrorMapper.userMessage(e.message ?: "Sign-in failed"),
                        )
                    }
                },
            )
        }
    }

    fun registerWithEmail(email: String, password: String, confirmPassword: String) {
        if (password != confirmPassword) {
            _state.update { it.copy(error = "Passwords do not match.") }
            return
        }
        _state.update { it.copy(phase = AuthPhase.Authenticating, error = null) }
        viewModelScope.launch {
            authRepository.register(email, password).fold(
                onSuccess = {
                    MomentraAnalytics.get().trackAuthResult("email_register", success = true)
                    onFirebaseLoggedIn()
                },
                onFailure = { e ->
                    MomentraAnalytics.get().trackAuthResult("email_register", success = false, errorCode = e.message)
                    _state.update {
                        it.copy(
                            phase = AuthPhase.AuthError,
                            error = AuthErrorMapper.userMessage(e.message ?: "Registration failed"),
                        )
                    }
                },
            )
        }
    }

    fun sendPhoneCode(activity: Activity, phone: String) {
        _state.update { it.copy(phase = AuthPhase.Authenticating, error = null) }
        viewModelScope.launch {
            authRepository.sendPhoneCode(activity, phone).fold(
                onSuccess = { result ->
                    when (result) {
                        is PhoneSendResult.AutoVerified -> {
                            MomentraAnalytics.get().trackAuthResult("phone_auto", success = true)
                            onFirebaseLoggedIn()
                        }
                        PhoneSendResult.CodeSent -> {
                            MomentraAnalytics.get().trackAuthResult("phone_sms_sent", success = true)
                            _state.update {
                                it.copy(phase = AuthPhase.SignedOut, phoneCodeSent = true, error = null)
                            }
                        }
                    }
                },
                onFailure = { e ->
                    MomentraAnalytics.get().trackAuthResult("phone_sms_sent", success = false, errorCode = e.message)
                    _state.update {
                        it.copy(
                            phase = AuthPhase.AuthError,
                            error = AuthErrorMapper.userMessage(e.message ?: "Could not send SMS code"),
                        )
                    }
                },
            )
        }
    }

    fun confirmPhoneCode(code: String) {
        if (code.trim().length < 6) {
            _state.update { it.copy(error = "Enter the 6-digit code from SMS.") }
            return
        }
        _state.update { it.copy(phase = AuthPhase.Authenticating, error = null) }
        viewModelScope.launch {
            authRepository.confirmPhoneCode(code).fold(
                onSuccess = {
                    MomentraAnalytics.get().trackAuthResult("phone_verify", success = true)
                    onFirebaseLoggedIn()
                },
                onFailure = { e ->
                    MomentraAnalytics.get().trackAuthResult("phone_verify", success = false, errorCode = e.message)
                    _state.update {
                        it.copy(
                            phase = AuthPhase.AuthError,
                            error = AuthErrorMapper.userMessage(e.message ?: "Phone verification failed"),
                        )
                    }
                },
            )
        }
    }

    fun resetPhoneFlow() {
        authRepository.resetPhoneFlow()
        _state.update { it.copy(phoneCodeSent = false, error = null, phase = AuthPhase.SignedOut) }
    }

    fun sendPasswordReset(email: String) {
        if (email.isBlank()) {
            _state.update { it.copy(error = "Enter your email to reset password.") }
            return
        }
        _state.update { it.copy(phase = AuthPhase.Authenticating, error = null) }
        viewModelScope.launch {
            authRepository.sendPasswordReset(email).fold(
                onSuccess = {
                    _state.update {
                        it.copy(
                            phase = AuthPhase.SignedOut,
                            error = "Password reset email sent. Check your inbox.",
                        )
                    }
                },
                onFailure = { e ->
                    _state.update {
                        it.copy(
                            phase = AuthPhase.AuthError,
                            error = AuthErrorMapper.userMessage(e.message ?: "Could not send reset email"),
                        )
                    }
                },
            )
        }
    }

    fun signInWithGoogle(context: Context) {
        _state.update { it.copy(phase = AuthPhase.Authenticating, error = null) }
        viewModelScope.launch {
            authRepository.signInWithGoogle(context).fold(
                onSuccess = {
                    MomentraAnalytics.get().trackAuthResult("google", success = true)
                    onFirebaseLoggedIn()
                },
                onFailure = { e ->
                    MomentraAnalytics.get().trackAuthResult("google", success = false, errorCode = e.message)
                    _state.update {
                        it.copy(
                            phase = AuthPhase.AuthError,
                            error = AuthErrorMapper.userMessage(e.message ?: "Google sign-in failed"),
                        )
                    }
                },
            )
        }
    }

    fun signOut() {
        val firebaseUid = authRepository.currentUser?.uid
        val momentraUserId = _state.value.identity?.userId
        prefs.clearCachedIdentity(firebaseUid)
        prefs.clearUserScopedShell(momentraUserId)
        meRepository.clearBootstrapCache(momentraUserId)
        SecurityPreferences(getApplication()).clearUserScoped(momentraUserId)
        authRepository.signOut()
        _state.value = AuthUiState(phase = AuthPhase.SignedOut)
    }

    fun onSessionExpired() {
        val firebaseUid = authRepository.currentUser?.uid
        val momentraUserId = _state.value.identity?.userId
        prefs.clearCachedIdentity(firebaseUid)
        meRepository.clearBootstrapCache(momentraUserId)
        authRepository.signOut()
        _state.value = AuthUiState(phase = AuthPhase.SessionExpired, error = "Session expired")
    }
}

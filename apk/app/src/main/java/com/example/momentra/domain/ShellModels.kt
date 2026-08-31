package com.example.momentra.domain

/** Top-level Momentra application context — UI selection only; server Governance is authoritative. */
enum class AppContext {
    PERSONAL,
    GROUP,
    BUSINESS,
    CIRCLE,
}

/** Canonical bottom navigation destinations. */
enum class BottomDestination {
    PULSE,
    MOMENTS,
    CREATE,
    LIFE,
    MEMORY,
}

enum class AuthPhase {
    Launching,
    RestoringSession,
    SignedOut,
    Authenticating,
    AuthenticatedBootstrapping,
    Authenticated,
    SessionExpired,
    AuthError,
}

data class ShellIdentity(
    val userId: String,
    val displayName: String?,
    val email: String?,
    val firebaseUid: String?,
)

data class CompanySummary(
    val companyId: String,
    val displayName: String,
)

sealed class ShellContentState {
    data object Idle : ShellContentState()
    data object Loading : ShellContentState()
    data object Empty : ShellContentState()
    data class Ready(val detail: String? = null) : ShellContentState()
    data class Error(val code: String?, val message: String) : ShellContentState()
    data object Forbidden : ShellContentState()
    data object Offline : ShellContentState()
    data object Deferred : ShellContentState()
}

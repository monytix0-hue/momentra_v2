package com.example.momentra.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class AuthPhaseTest {
    @Test
    fun phasesCoverLifecycle() {
        val phases = AuthPhase.entries.map { it.name }.toSet()
        assertEquals(
            setOf(
                "Launching",
                "RestoringSession",
                "SignedOut",
                "Authenticating",
                "AuthenticatedBootstrapping",
                "Authenticated",
                "SessionExpired",
                "AuthError",
            ),
            phases,
        )
    }

    @Test
    fun bottomNavCanonicalNames() {
        assertEquals(
            listOf("PULSE", "MOMENTS", "CREATE", "LIFE", "MEMORY"),
            BottomDestination.entries.map { it.name },
        )
    }

    @Test
    fun contextsCanonicalNames() {
        assertEquals(
            listOf("PERSONAL", "GROUP", "BUSINESS", "CIRCLE"),
            AppContext.entries.map { it.name },
        )
    }
}

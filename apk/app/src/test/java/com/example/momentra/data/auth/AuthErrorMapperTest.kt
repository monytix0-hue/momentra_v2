package com.example.momentra.data.auth

import org.junit.Assert.assertTrue
import org.junit.Test

class AuthErrorMapperTest {
    @Test
    fun mapsBillingNotEnabled() {
        val msg = AuthErrorMapper.userMessage("An internal error has occurred. [ BILLING_NOT_ENABLED ]")
        assertTrue(msg.contains("Blaze") || msg.contains("billing", ignoreCase = true))
    }

    @Test
    fun mapsDeveloperConsole28444() {
        val msg = AuthErrorMapper.userMessage("[28444] Developer console is not set up correctly.")
        assertTrue(msg.contains("SHA", ignoreCase = true))
    }

    @Test
    fun mapsSignInCancelled() {
        val msg = AuthErrorMapper.userMessage("Sign-in cancelled.")
        assertTrue(msg.contains("cancelled", ignoreCase = true))
    }

    @Test
    fun mapsNoGoogleAccountsAsSetupHint() {
        val msg = AuthErrorMapper.userMessage(
            "No Google accounts available. Add a Google account on this device, or check that Firebase Android SHA fingerprints are configured (error 28444).",
        )
        assertTrue(msg.contains("SHA", ignoreCase = true))
    }
}

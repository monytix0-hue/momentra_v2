package com.example.momentra.data.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BalanceMaskTest {
    @Test
    fun masksWhenHideEnabled() {
        assertEquals("••••", BalanceMask.mask("1,234.50", hide = true))
        assertEquals("1,234.50", BalanceMask.mask("1,234.50", hide = false))
    }
}

class AppLockSessionTest {
    @Test
    fun lockUnlockCycle() {
        AppLockSession.markUnlocked()
        assertTrue(AppLockSession.unlocked)
        AppLockSession.markLocked()
        assertFalse(AppLockSession.unlocked)
    }

    @Test
    fun shouldRelockAfterTimeout() {
        AppLockSession.markUnlocked()
        AppLockSession.onBackground()
        // 0 seconds means immediate relock
        assertTrue(AppLockSession.shouldRelock(0))
    }
}

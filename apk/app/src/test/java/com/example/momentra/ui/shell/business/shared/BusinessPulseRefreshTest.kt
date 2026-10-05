package com.example.momentra.ui.shell.business.shared

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BusinessPulseRefreshTest {
    @Test
    fun saveRefreshesPulseOnceThroughTheVisibleScreen() {
        assertFalse(businessRefreshPrefetchesPulse(forcePrefetch = false))
    }

    @Test
    fun momentSelectStillPrefetchesPulse() {
        assertTrue(businessRefreshPrefetchesPulse(forcePrefetch = true))
    }
}

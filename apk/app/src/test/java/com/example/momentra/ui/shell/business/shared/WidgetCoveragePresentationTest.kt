package com.example.momentra.ui.shell.business.shared

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Behaviour under test: server coverage → mobile presentation (showValue/showSources).
 * Module: WidgetCoveragePresentation.kt. Existing tests: none.
 * User: "Map the backend's COMPLETE, PARTIAL, UNAVAILABLE and ZERO states into explicit
 * mobile presentation states." + "Do not allow mobile clients to calculate authoritative
 * finance totals independently."
 */
class WidgetCoveragePresentationTest {
    @Test
    fun completeShowsValueAndSources() {
        val p = WidgetCoveragePresentation.fromServer("COMPLETE")
        assertTrue(p.showValue)
        assertTrue(p.showSources)
        assertEquals("Complete", p.label)
    }

    @Test
    fun partialHidesValueKeepsSources() {
        val p = WidgetCoveragePresentation.fromServer("partial")
        assertFalse(p.showValue)
        assertTrue(p.showSources)
    }

    @Test
    fun unavailableHidesValueAndSources() {
        val p = WidgetCoveragePresentation.fromServer("UNAVAILABLE")
        assertFalse(p.showValue)
        assertFalse(p.showSources)
    }

    @Test
    fun zeroShowsServerZeroNotClientMath() {
        val p = WidgetCoveragePresentation.fromServer("ZERO")
        assertTrue(p.showValue)
        assertTrue(p.showSources)
        assertEquals("Zero", p.label)
    }
}

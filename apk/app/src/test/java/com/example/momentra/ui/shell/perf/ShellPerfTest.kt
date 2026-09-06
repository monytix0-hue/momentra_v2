package com.example.momentra.ui.shell.perf

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Contract smoke for paint-first Pulse marks after Personal vs Group/Business sync audit.
 *
 * On device: `adb logcat -s MomentraPerf | findstr pulse_tab_ready`
 * Expect Group cold paint within ~2× Personal (not 5–6×), with Group extras including paint=true.
 */
class ShellPerfTest {
    @Before
    fun clearMarks() {
        ShellPerf.clear()
    }

    @Test
    fun recordsElapsedForNamedMarks() {
        val mark = ShellPerf.start("moment_switch")
        Thread.sleep(5)
        val elapsed = ShellPerf.end(mark, mapOf("momentId" to "abc"))
        assertTrue(elapsed >= 5)
        assertTrue((ShellPerf.last["moment_switch"] ?: 0) >= 5)
    }

    @Test
    fun instantEventsAreStored() {
        ShellPerf.instant("quick_add_presentation", mapOf("context" to "PERSONAL"))
        assertTrue(ShellPerf.last.containsKey("quick_add_presentation"))
    }

    @Test
    fun pulseTabReadyMarksRecordPerContext() {
        val personal = ShellPerf.start("pulse_tab_ready")
        ShellPerf.end(personal, mapOf("context" to "PERSONAL", "parallel" to true))
        val personalMs = ShellPerf.last["pulse_tab_ready"]!!

        val group = ShellPerf.start("pulse_tab_ready")
        ShellPerf.end(group, mapOf("context" to "GROUP", "paint" to true, "parallel" to true))
        val groupMs = ShellPerf.last["pulse_tab_ready"]!!

        val business = ShellPerf.start("pulse_tab_ready")
        ShellPerf.end(business, mapOf("context" to "BUSINESS", "bundled" to true))
        val businessMs = ShellPerf.last["pulse_tab_ready"]!!

        assertEquals(businessMs, ShellPerf.last["pulse_tab_ready"])
        assertTrue(personalMs >= 0)
        assertTrue(groupMs >= 0)
        assertTrue(businessMs >= 0)

        ShellPerf.instant("group_pulse_enrich", mapOf("momentId" to "deadbeef"))
        assertTrue(ShellPerf.last.containsKey("group_pulse_enrich"))

        ShellPerf.instant("sse_off_scope_soft", mapOf("scopeType" to "MOMENT"))
        assertTrue(ShellPerf.last.containsKey("sse_off_scope_soft"))

        ShellPerf.instant(
            "scoped_refresh_group",
            mapOf("warm" to true, "prefetch" to false),
        )
        assertTrue(ShellPerf.last.containsKey("scoped_refresh_group"))
    }
}

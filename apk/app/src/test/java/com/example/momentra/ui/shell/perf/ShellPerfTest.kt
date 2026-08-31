package com.example.momentra.ui.shell.perf

import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ShellPerfTest {
    @Before
    fun clear() {
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
}

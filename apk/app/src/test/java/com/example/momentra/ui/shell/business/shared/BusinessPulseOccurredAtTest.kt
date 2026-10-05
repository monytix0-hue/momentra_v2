package com.example.momentra.ui.shell.business.shared

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class BusinessPulseOccurredAtTest {
    @Test
    fun isoTimestampBecomesAKolkataLabel() {
        val label = formatBusinessPulseOccurredAt("2026-10-01T08:53:15.883Z")
        assertEquals("1 Oct, 2:23 PM", label)
        assertFalse(label.contains("T"))
        assertFalse(label.endsWith("Z"))
    }

    @Test
    fun unparseableTimestampIsOmitted() {
        assertEquals("", formatBusinessPulseOccurredAt("not-a-timestamp"))
    }
}

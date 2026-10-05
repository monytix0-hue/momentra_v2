package com.example.momentra.ui.shell.business.teamops.create

import org.junit.Assert.assertEquals
import org.junit.Test

class TeamMemoryTypeTest {
    @Test
    fun noteMilestoneAndDecisionUseTheServerEnum() {
        assertEquals("GENERAL", teamMemoryTypeCode("Note"))
        assertEquals("MILESTONE", teamMemoryTypeCode("Milestone"))
        assertEquals("DECISION", teamMemoryTypeCode("Decision"))
        assertEquals("GENERAL", teamMemoryTypeCode("NOTE"))
    }
}

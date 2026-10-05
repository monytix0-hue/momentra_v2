package com.example.momentra.ui.shell.personal.shared

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PersonalPulseNudgeTest {

    @Test
    fun everydayNudgeIsOmittedWhenTodayHasNoLogs() {
        assertNull(PersonalPulseFamily.LIFE_OPERATIONS.visibleNudge(0))
    }

    @Test
    fun everydayNudgeWithLogsKeepsRecoveryCtaAndDoesNotClaimBusy() {
        val nudge = PersonalPulseFamily.LIFE_OPERATIONS.visibleNudge(1)
        assertNotNull(nudge)
        assertEquals("Log Recovery", nudge!!.cta)
        val text = listOfNotNull(nudge.title, nudge.body).joinToString(" ")
        assertFalse(text.contains("busy", ignoreCase = true))
    }

    @Test
    fun otherFamiliesKeepTheirInstructionCopy() {
        val future = PersonalPulseFamily.FUTURE_BUILDING.visibleNudge(0)
        assertNotNull(future)
        assertEquals("Log a milestone to keep momentum compounding.", future!!.body)
    }
}

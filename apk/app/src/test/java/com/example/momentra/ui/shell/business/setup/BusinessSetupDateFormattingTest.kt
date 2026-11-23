package com.example.momentra.ui.shell.business.setup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Behaviour under test: BusinessSetupDateFormatting day/month storage + GSTIN uppercase.
 * Existing tests: BusinessSetupFieldModelTest, OperatingSetupPayloadBuilderTest (no prior DateRow helper tests).
 * User: "Implement the plan as specified" — Task 1 date helpers + Task 3 uppercase helper.
 */
class BusinessSetupDateFormattingTest {
    @Test
    fun dayModeWritesIsoAndDisplaysHuman() {
        val stored = BusinessSetupDateFormatting.toStorage(
            LocalDate.of(2026, 10, 12),
            BusinessSetupDateFormatting.Mode.Day,
        )
        assertEquals("2026-10-12", stored)
        assertEquals(
            "12 October 2026",
            BusinessSetupDateFormatting.formatDisplay(stored, BusinessSetupDateFormatting.Mode.Day, "Pick"),
        )
    }

    @Test
    fun monthModeWritesYearMonth() {
        val stored = BusinessSetupDateFormatting.toStorage(
            LocalDate.of(2026, 10, 12),
            BusinessSetupDateFormatting.Mode.Month,
        )
        assertEquals("2026-10", stored)
        assertEquals(
            "October 2026",
            BusinessSetupDateFormatting.formatDisplay(stored, BusinessSetupDateFormatting.Mode.Month, "Pick"),
        )
    }

    @Test
    fun parseHumanAndIso() {
        assertEquals(LocalDate.of(2026, 10, 12), BusinessSetupDateFormatting.parseToLocalDate("2026-10-12"))
        assertEquals(LocalDate.of(2026, 10, 1), BusinessSetupDateFormatting.parseToLocalDate("2026-10"))
        assertEquals(LocalDate.of(2026, 10, 12), BusinessSetupDateFormatting.parseToLocalDate("12 October 2026"))
        assertNull(BusinessSetupDateFormatting.parseToLocalDate(""))
    }

    @Test
    fun modeAndClearFromKey() {
        assertEquals(BusinessSetupDateFormatting.Mode.Month, BusinessSetupDateFormatting.modeForKey("planMonth"))
        assertEquals(BusinessSetupDateFormatting.Mode.Day, BusinessSetupDateFormatting.modeForKey("startDate"))
        assertTrue(BusinessSetupDateFormatting.allowClearForKey("endDate"))
        assertEquals(false, BusinessSetupDateFormatting.allowClearForKey("startDate"))
    }

    @Test
    fun forceUppercaseInput() {
        assertEquals("27AAFCF4321M1Z6", BusinessSetupDateFormatting.forceUppercaseInput("27aafcf4321m1z6"))
    }
}

package com.example.momentra.ui.shell.business.setup

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BusinessSetupPrefillTest {

    @Test
    fun dirtyKeyIsSkipped() {
        assertFalse(
            BusinessSetupPrefill.shouldApply(
                key = "businessName",
                dirty = setOf("businessName"),
                loaded = "Northstar Trading",
                current = "enterprises",
            ),
        )
    }

    @Test
    fun cleanEmptyKeyIsFilled() {
        assertTrue(
            BusinessSetupPrefill.shouldApply(
                key = "businessName",
                dirty = emptySet(),
                loaded = "Northstar Trading",
                current = "",
            ),
        )
    }

    @Test
    fun cleanKeyWithExistingValueIsNotClobbered() {
        assertFalse(
            BusinessSetupPrefill.shouldApply(
                key = "businessName",
                dirty = emptySet(),
                loaded = "Northstar Trading",
                current = "enterprises",
            ),
        )
    }

    @Test
    fun whitespaceOnlyLoadedValueIsSkipped() {
        assertFalse(
            BusinessSetupPrefill.shouldApply(
                key = "businessName",
                dirty = emptySet(),
                loaded = "   ",
                current = "",
            ),
        )
        assertFalse(
            BusinessSetupPrefill.shouldApply(
                key = "businessName",
                dirty = emptySet(),
                loaded = null,
                current = "",
            ),
        )
    }

    @Test
    fun untouchedKeyWithIdenticalValueIsSkipped() {
        assertFalse(
            BusinessSetupPrefill.shouldApply(
                key = "businessName",
                dirty = emptySet(),
                loaded = "Northstar Trading",
                current = "Northstar Trading",
            ),
        )
    }

    @Test
    fun expandedFigmaKeysArePrefillable() {
        // Backend expand writes businessName/companyName/teamMembers alongside closed keys.
        assertTrue(
            BusinessSetupPrefill.shouldApply(
                key = "businessName",
                dirty = emptySet(),
                loaded = "Northstar Trading",
                current = "",
            ),
        )
        assertTrue(
            BusinessSetupPrefill.shouldApply(
                key = "companyName",
                dirty = emptySet(),
                loaded = "Northstar",
                current = null,
            ),
        )
        assertTrue(
            BusinessSetupPrefill.shouldApply(
                key = "teamMembers",
                dirty = emptySet(),
                loaded = "Asha | Sam",
                current = "",
            ),
        )
        assertTrue(
            BusinessSetupPrefill.shouldApply(
                key = "gstTracking",
                dirty = emptySet(),
                loaded = "Yes",
                current = "",
            ),
        )
    }

    @Test
    fun logoMediaIdPrefillIsAllowedForUntouchedEmptyField() {
        assertTrue(
            BusinessSetupPrefill.shouldApply(
                key = "logoMediaId",
                dirty = emptySet(),
                loaded = "media_123",
                current = "",
            ),
        )
    }
}

package com.example.momentra.ui.shell.empty.business

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BusinessNoMomentEmptyCopyTest {
    @Test
    fun noMomentEmptyDoesNotLookLikeLiveData() {
        assertTrue(BusinessNoMomentEmptyCopy.momentsSampleRows.isEmpty())
        BusinessNoMomentEmptyCopy.forbiddenSampleTitles.forEach { title ->
            assertFalse(BusinessNoMomentEmptyCopy.momentsSampleRows.any { it.first == title })
        }
        assertEquals("Not set up", BusinessNoMomentEmptyCopy.ABSENT)
        assertFalse(BusinessNoMomentEmptyCopy.ABSENT == "-")
        assertFalse(BusinessNoMomentEmptyCopy.ABSENT == "0")
    }
}

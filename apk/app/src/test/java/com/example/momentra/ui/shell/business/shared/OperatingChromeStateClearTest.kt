package com.example.momentra.ui.shell.business.shared

import com.example.momentra.data.api.OperatingActivityDto
import com.example.momentra.data.api.OperatingPaymentDto
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class OperatingChromeStateClearTest {
    @Test
    fun clearTransientUi_nullsHintsAndSheets() {
        OperatingChromeState.quickAddHint = "sale"
        OperatingChromeState.mastersOpen = "Catalogue"
        OperatingChromeState.browserSelection = OperatingBrowserSelection(
            id = "m1",
            name = "Widget",
            browserKind = OperatingBrowserKind.Parts,
            captureHint = "ORDER",
        )
        OperatingChromeState.editing = OperatingActivityDto(id = "a1", title = "t")
        OperatingChromeState.settling = OperatingPaymentDto(id = "p1", title = "t")
        OperatingChromeState.storyConfigOpen = true
        OperatingChromeState.captureOpen = true

        OperatingChromeState.clearTransientUi()

        assertNull(OperatingChromeState.quickAddHint)
        assertNull(OperatingChromeState.mastersOpen)
        assertNull(OperatingChromeState.browserSelection)
        assertNull(OperatingChromeState.editing)
        assertNull(OperatingChromeState.settling)
        assertFalse(OperatingChromeState.storyConfigOpen)
        assertFalse(OperatingChromeState.captureOpen)
    }
}
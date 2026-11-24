package com.example.momentra.ui.shell.business.shared

import com.example.momentra.ui.theme.generated.BusinessTokens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OperatingV12TradingThemeTest {

    @Test
    fun tradingForScreenUsesPerTabSurfaces() {
        val pulse = OperatingV12Theme.tradingForScreen(OperatingTab.PULSE)
        val moments = OperatingV12Theme.tradingForScreen(OperatingTab.MOMENTS)
        val quickAdd = OperatingV12Theme.tradingForScreen(OperatingTab.QUICK_ADD)

        assertEquals(BusinessTokens.Colors.TRADINGPulseSurface, pulse.surface)
        assertEquals(BusinessTokens.Colors.TRADINGMomentsSurface, moments.surface)
        assertEquals(BusinessTokens.Colors.TRADINGQuickAddSurface, quickAdd.surface)
        assertNotEquals(pulse.surface, moments.surface)
        assertTrue(pulse.isTrading)
        assertEquals(BusinessTokens.Colors.TRADINGAccent, pulse.accent)
    }
}

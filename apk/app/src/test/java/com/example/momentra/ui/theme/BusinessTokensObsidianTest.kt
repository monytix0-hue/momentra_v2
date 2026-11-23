package com.example.momentra.ui.theme

import androidx.compose.ui.graphics.toArgb
import com.example.momentra.ui.theme.generated.BusinessTokens
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pins BusinessTokens to the Obsidian teal palette from Figma rknf `2:67`.
 */
class BusinessTokensObsidianTest {

    @Test
    fun bgCanvasIsObsidian() {
        assertEquals(0xFF05070B.toInt(), BusinessTokens.Colors.BgCanvas.toArgb())
    }

    @Test
    fun ctaGradientIsTeal() {
        assertEquals(0xFF059669.toInt(), BusinessTokens.Colors.CtaStart.toArgb())
        assertEquals(0xFF34D399.toInt(), BusinessTokens.Colors.CtaEnd.toArgb())
    }

    @Test
    fun tradingAccentIsEmerald() {
        assertEquals(0xFF10B981.toInt(), BusinessTokens.Colors.TRADINGAccent.toArgb())
    }

    @Test
    fun madeToOrderDomainIsPresent() {
        assertEquals(0xFFEA580C.toInt(), BusinessTokens.Colors.MADE_TO_ORDERAccent.toArgb())
        assertEquals(0xFF101818.toInt(), BusinessTokens.Colors.MADE_TO_ORDERCanvas.toArgb())
    }

    @Test
    fun signalPaletteMatchesObsidian() {
        assertEquals(0xFFF5A624.toInt(), BusinessTokens.Colors.SignalWarning.toArgb())
        assertEquals(0xFFFF5C38.toInt(), BusinessTokens.Colors.SignalDanger.toArgb())
        assertEquals(0xFF94A4B8.toInt(), BusinessTokens.Colors.TextMuted.toArgb())
    }
}

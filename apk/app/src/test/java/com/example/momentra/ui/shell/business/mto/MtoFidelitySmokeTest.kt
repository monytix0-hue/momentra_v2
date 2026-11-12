package com.example.momentra.ui.shell.business.mto

import com.example.momentra.ui.theme.MadeToOrderAmber
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MtoFidelitySmokeTest {
    @Test
    fun softAmberHeroLeadsSoftThenSolid() {
        // Soft lead (#FBBF24) differs from deep (#B45309); solid accent/end is #F59E0B.
        assertNotEquals(MadeToOrderAmber.heroStart, MadeToOrderAmber.accentDeep)
        assertEquals(MadeToOrderAmber.accentSoft, MadeToOrderAmber.heroStart)
        assertEquals(MadeToOrderAmber.accent, MadeToOrderAmber.heroEnd)
        assertTrue(MadeToOrderAmber.canvas.red < 0.2f)
        assertTrue(MadeToOrderAmber.accent.red > 0.8f)
        assertTrue(MadeToOrderAmber.accentSoft.green >= MadeToOrderAmber.accent.green - 0.05f)
    }

    @Test
    fun deliveryAndHoldAreFirstClassCaptures() {
        val names = MtoCapture.entries.map { it.name }.toSet()
        assertTrue(names.contains("DELIVERY"))
        assertTrue(names.contains("HOLD"))
        assertEquals("DELIVERY", MtoCapture.DELIVERY.captureMode)
        assertEquals("HOLD", MtoCapture.HOLD.captureMode)
        assertEquals("Collection / advance", MtoCapture.COLLECTION.title)
        assertEquals("Delivery / dispatch", MtoCapture.DELIVERY.title)
    }

    @Test
    fun holdAndReprintDoNotPostStockOrNewSale() {
        // Hold parks a job (ORDER/HOLD) — never STOCK write.
        assertEquals("ORDER", MtoCapture.HOLD.eventType)
        assertEquals("HOLD", MtoCapture.HOLD.captureMode)
        assertTrue(MtoCapture.HOLD.eventType != "STOCK")
        assertTrue(MtoCapture.HOLD.eventType != "SALE")
        // Reprint re-renders an existing invoice/job card (mode REPRINT), not a new SALE write path.
        assertEquals("REPRINT", MtoCapture.REPRINT.captureMode)
        assertEquals("SALE", MtoCapture.REPRINT.eventType) // document lookup family
        val modes = MtoCapture.entries.map { it.captureMode }.toSet()
        assertTrue(modes.contains("HOLD"))
        assertTrue(modes.contains("REPRINT"))
        assertTrue(modes.contains("DELIVERY"))
        assertTrue(modes.contains("TRANSFER"))
    }
}

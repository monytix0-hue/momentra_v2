package com.example.momentra.ui.shell.business.manufacturing

import com.example.momentra.ui.theme.ManufacturingAmber
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Hub wiring smoke — no Compose / device. Covers MFG capture enum + utility nav
 * (HOLD / TRANSFER / REPRINT) used by ManufacturingQuickAddHub.openCapture.
 */
class MfgFidelitySmokeTest {
    @Test
    fun amberCanvasDistinctFromAccent() {
        assertNotEquals(ManufacturingAmber.accent, ManufacturingAmber.canvas)
        assertTrue(ManufacturingAmber.accent.red > 0.7f || ManufacturingAmber.accent.green > 0.4f)
    }

    @Test
    fun coreCaptureEventTypesArePresent() {
        val byName = ManufacturingCapture.entries.map { it.name }.toSet()
        val types = ManufacturingCapture.entries.map { it.eventType }.toSet()
        for (name in listOf(
            "PRODUCTION", "SALE", "PURCHASE", "SPEND", "PAYMENT", "COLLECTION",
            "CUSTOMER_ORDER", "DELIVERY", "BUDGET", "STOCK", "ORDER", "RETURN",
            "HOLD", "TRANSFER", "REPRINT", "REWORK",
        )) {
            assertTrue("MFG missing capture $name", byName.contains(name))
        }
        assertTrue(types.contains("PRODUCTION"))
        assertTrue(types.contains("CUSTOMER_ORDER"))
        assertTrue(types.contains("DELIVERY"))
    }

    @Test
    fun holdTransferReprintHonestyAndUtilityNav() {
        assertEquals("ORDER", ManufacturingCapture.HOLD.eventType)
        assertEquals("STOCK", ManufacturingCapture.TRANSFER.eventType)
        assertEquals("SALE", ManufacturingCapture.REPRINT.eventType)
        assertNotEquals("SALE", ManufacturingCapture.HOLD.eventType)
        assertNotEquals("STOCK", ManufacturingCapture.HOLD.eventType)

        assertEquals(
            setOf(MfgUtilityNav.HOLD, MfgUtilityNav.TRANSFER, MfgUtilityNav.REPRINT),
            MfgUtilityNav.entries.toSet(),
        )
    }
}

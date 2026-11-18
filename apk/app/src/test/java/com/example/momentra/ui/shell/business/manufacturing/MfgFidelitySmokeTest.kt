package com.example.momentra.ui.shell.business.manufacturing

import com.example.momentra.ui.theme.ManufacturingAmber
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Hub wiring smoke — no Compose / device. Covers MFG hub taxonomy `5:47223`
 * (Core·9 / Support·5 / Fulfilment·4 / Utilities·3) + capture/HTR vocabulary.
 */
class MfgFidelitySmokeTest {
    @Test
    fun amberCanvasDistinctFromAccent() {
        assertNotEquals(ManufacturingAmber.accent, ManufacturingAmber.canvas)
        assertTrue(ManufacturingAmber.accent.red > 0.7f || ManufacturingAmber.accent.green > 0.4f)
    }

    @Test
    fun hubSectionsMatchFigma47223() {
        assertEquals(9, MfgHubCatalog.core.size)
        assertEquals(5, MfgHubCatalog.support.size)
        assertEquals(4, MfgHubCatalog.fulfilment.size)
        assertEquals(3, MfgHubCatalog.utilities.size)
        assertEquals(MfgCaptureCopy.CORE_SECTION, "Core · 9")
        assertEquals(MfgCaptureCopy.SUPPORT_SECTION, "Support · 5")
        assertEquals(MfgCaptureCopy.FULFILMENT_SECTION, "Fulfilment · 4")
        assertEquals(MfgCaptureCopy.UTILITIES_SECTION, "Utilities · 3")
        assertEquals(MfgCaptureCopy.HUB_SEARCH_HINT, "Search actions…")
        assertTrue(MfgCaptureCopy.HUB_HERO_HEADLINE.contains("MAKE TO STOCK"))
    }

    @Test
    fun fulfilmentAndUtilitiesCaptions() {
        assertEquals(
            listOf("Credit note · restock", "Receipt in · dispatch out", "Governed decision", "Scrap · job work"),
            MfgCaptureCopy.HUB_FULFILMENT_CAPTIONS,
        )
        assertEquals(
            listOf("Work order pause", "Stock · WIP move", "Invoice · job card"),
            MfgCaptureCopy.HUB_UTILITY_CAPTIONS,
        )
        assertEquals("RETURN", MfgHubCatalog.fulfilment[0].id)
        assertEquals("DELIVERY", MfgHubCatalog.fulfilment[1].id)
        assertEquals("APPROVAL", MfgHubCatalog.fulfilment[2].id)
        assertEquals("REWORK", MfgHubCatalog.fulfilment[3].id)
        assertEquals(
            listOf("HOLD", "TRANSFER", "REPRINT"),
            MfgHubCatalog.utilities.map { it.id },
        )
    }

    @Test
    fun coreCaptureEventTypesArePresent() {
        val byName = ManufacturingCapture.entries.map { it.name }.toSet()
        val types = ManufacturingCapture.entries.map { it.eventType }.toSet()
        for (name in listOf(
            "PRODUCTION", "SALE", "PURCHASE", "SPEND", "PAYMENT", "COLLECTION",
            "CUSTOMER_ORDER", "DELIVERY", "BUDGET", "STOCK", "ORDER", "RETURN",
            "HOLD", "TRANSFER", "REPRINT", "REWORK", "APPROVAL",
        )) {
            assertTrue("MFG missing capture $name", byName.contains(name))
        }
        assertTrue(types.contains("PRODUCTION"))
        assertTrue(types.contains("CUSTOMER_ORDER"))
        assertTrue(types.contains("DELIVERY"))
        assertTrue(types.contains("RETURN"))
    }

    @Test
    fun holdTransferReprintHonestyAndUtilityNav() {
        assertEquals("ORDER", ManufacturingCapture.HOLD.eventType)
        assertEquals("STOCK", ManufacturingCapture.TRANSFER.eventType)
        assertEquals("SALE", ManufacturingCapture.REPRINT.eventType)
        assertNotEquals("SALE", ManufacturingCapture.HOLD.eventType)
        assertNotEquals("STOCK", ManufacturingCapture.HOLD.eventType)
        assertTrue(MfgCaptureCopy.HOLD_WHY.contains("device"))
        assertTrue(MfgCaptureCopy.TRANSFER_WHY.contains("No revenue"))
        assertTrue(MfgCaptureCopy.REPRINT_WHY.contains("Never"))

        assertEquals(
            setOf(MfgUtilityNav.HOLD, MfgUtilityNav.TRANSFER, MfgUtilityNav.REPRINT),
            MfgUtilityNav.entries.toSet(),
        )
    }

    @Test
    fun orderProductionReworkCopyParity() {
        assertEquals(
            listOf("Sales order", "Purchase order", "Work order", "Delivery"),
            MfgCaptureCopy.ORDER_KIND_CHIPS,
        )
        assertTrue(MfgCaptureCopy.ORDER_WHY.contains("commitments"))
        assertTrue(MfgCaptureCopy.PRODUCTION_WHY.contains("accepted"))
        assertTrue(MfgCaptureCopy.REWORK_WHY.contains("never silently"))
    }
}

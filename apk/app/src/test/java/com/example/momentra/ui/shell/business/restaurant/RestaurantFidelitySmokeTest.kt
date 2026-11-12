package com.example.momentra.ui.shell.business.restaurant

import com.example.momentra.ui.theme.RestaurantEmber
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Hub wiring smoke — no Compose / device. Asserts Restaurant sheet enum codes
 * stay openable and HOLD/REPRINT honesty modes do not collapse into SALE/STOCK.
 */
class RestaurantFidelitySmokeTest {
    @Test
    fun emberHeroDistinctFromCanvas() {
        assertNotEquals(RestaurantEmber.wine, RestaurantEmber.ember)
        assertTrue(RestaurantEmber.screenTop.red >= RestaurantEmber.screenBottom.red)
    }

    @Test
    fun coreCaptureModesAreOpenable() {
        val modes = RestaurantSheetKind.entries.map { it.mode }.toSet()
        for (code in listOf(
            "SALE", "PURCHASE", "COLLECTION", "PAYMENT", "SPEND", "STOCK",
            "ORDER", "BUDGET", "ISSUE", "HOLD", "TRANSFER", "REPRINT",
        )) {
            assertTrue("Restaurant missing capture mode $code", modes.contains(code))
        }
    }

    @Test
    fun holdAndReprintHonesty() {
        assertEquals("ORDER", RestaurantSheetKind.Hold.eventType)
        assertEquals("HOLD", RestaurantSheetKind.Hold.mode)
        assertNotEquals("SALE", RestaurantSheetKind.Hold.eventType)
        assertNotEquals("STOCK", RestaurantSheetKind.Hold.eventType)
        assertEquals("REPRINT", RestaurantSheetKind.Reprint.mode)
        assertEquals("SALE", RestaurantSheetKind.Reprint.eventType)
        assertEquals("TRANSFER", RestaurantSheetKind.Transfer.mode)
        assertEquals("STOCK", RestaurantSheetKind.Transfer.eventType)
    }
}

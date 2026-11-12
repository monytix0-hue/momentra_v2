package com.example.momentra.ui.shell.business.retail

import com.example.momentra.ui.theme.RetailRose
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Hub wiring smoke — no Compose / device. Catches missing capture modes and
 * honesty slips (HOLD must not be SALE/STOCK; REPRINT is mode REPRINT).
 */
class RetailFidelitySmokeTest {
    @Test
    fun roseHeroLeadsDistinctFromCanvas() {
        assertNotEquals(RetailRose.screenTop, RetailRose.screenBottom)
        assertTrue(RetailRose.screenTop.red > RetailRose.screenMid.red)
    }

    @Test
    fun coreCaptureModesAreOpenable() {
        val modes = RetailSheet.entries.map { it.mode }.toSet()
        for (code in listOf(
            "SALE", "PURCHASE", "COLLECTION", "PAYMENT", "SPEND", "STOCK",
            "ORDER", "BUDGET", "ISSUE", "HOLD", "TRANSFER", "REPRINT",
        )) {
            assertTrue("Retail missing capture mode $code", modes.contains(code))
        }
    }

    @Test
    fun holdParksBillWithoutStockOrSaleWrite() {
        assertEquals("ORDER", RetailSheet.HOLD.eventType)
        assertEquals("HOLD", RetailSheet.HOLD.mode)
        assertNotEquals("SALE", RetailSheet.HOLD.eventType)
        assertNotEquals("STOCK", RetailSheet.HOLD.eventType)
    }

    @Test
    fun reprintIsModeNotNewSalePath() {
        assertEquals("REPRINT", RetailSheet.REPRINT.mode)
        assertEquals("SALE", RetailSheet.REPRINT.eventType) // receipt document family
        assertEquals("TRANSFER", RetailSheet.TRANSFER.mode)
        assertEquals("STOCK", RetailSheet.TRANSFER.eventType)
    }
}

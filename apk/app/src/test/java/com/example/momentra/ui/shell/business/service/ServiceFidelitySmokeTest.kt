package com.example.momentra.ui.shell.business.service

import com.example.momentra.ui.theme.ServicesTeal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Hub wiring smoke — no Compose / device. Service HOLD/TRANSFER park or reassign
 * appointments; REPRINT re-renders an invoice without a new charge.
 */
class ServiceFidelitySmokeTest {
    @Test
    fun tealHeroLeadsSoftThenSolid() {
        assertNotEquals(ServicesTeal.heroStart, ServicesTeal.heroEnd)
        assertEquals(ServicesTeal.teal, ServicesTeal.heroStart)
        assertEquals(ServicesTeal.lime, ServicesTeal.heroEnd)
    }

    @Test
    fun coreCaptureModesAreOpenable() {
        val modes = ServiceCaptureSheet.entries.map { it.mode }.toSet()
        for (code in listOf(
            "SERVICE", "PURCHASE", "COLLECTION", "PAYMENT", "SPEND", "STOCK",
            "APPOINTMENT", "STATUS", "BUDGET", "ISSUE", "HOLD", "TRANSFER", "REPRINT",
        )) {
            assertTrue("Service missing capture mode $code", modes.contains(code))
        }
    }

    @Test
    fun holdTransferReprintHonesty() {
        assertEquals("APPOINTMENT", ServiceCaptureSheet.HOLD.eventType)
        assertEquals("HOLD", ServiceCaptureSheet.HOLD.mode)
        assertNotEquals("SERVICE", ServiceCaptureSheet.HOLD.eventType)
        assertNotEquals("STOCK", ServiceCaptureSheet.HOLD.eventType)

        assertEquals("APPOINTMENT", ServiceCaptureSheet.TRANSFER.eventType)
        assertEquals("TRANSFER", ServiceCaptureSheet.TRANSFER.mode)
        assertNotEquals("STOCK", ServiceCaptureSheet.TRANSFER.eventType)

        assertEquals("REPRINT", ServiceCaptureSheet.REPRINT.mode)
        assertEquals("SERVICE", ServiceCaptureSheet.REPRINT.eventType)
        assertEquals("STATUS", ServiceCaptureSheet.STATUS.mode)
        assertEquals("APPOINTMENT", ServiceCaptureSheet.STATUS.eventType)
    }
}

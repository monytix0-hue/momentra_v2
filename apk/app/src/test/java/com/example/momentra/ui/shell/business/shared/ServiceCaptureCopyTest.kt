package com.example.momentra.ui.shell.business.shared

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ServiceCaptureCopyTest {

    @Test
    fun ctasMatchFigmaTable() {
        val expected = mapOf(
            "SERVICE" to "Post charge",
            "PURCHASE" to "Post purchase",
            "COLLECTION" to "Post collection",
            "PAYMENT" to "Post payment",
            "SPEND" to "Post expense",
            "STOCK" to "Post stock update",
            "APPOINTMENT" to "Save booking",
            "STATUS" to "Update status",
            "BUDGET" to "Save revision",
            "ISSUE" to "Post",
        )
        expected.forEach { (mode, cta) ->
            assertEquals(mode, cta, ServiceCaptureCopy.cta(mode))
        }
    }

    @Test
    fun whyBodiesMatchFigmaGstBoundaries() {
        assertTrue(ServiceCaptureCopy.why("SERVICE").contains("revenue + output GST once"))
        assertTrue(ServiceCaptureCopy.why("APPOINTMENT").contains("no revenue, no GST"))
        assertTrue(ServiceCaptureCopy.why("STATUS").contains("does not post charge"))
        assertTrue(ServiceCaptureCopy.why("COLLECTION").contains("never creates new revenue"))
        assertEquals("Settles open AP only.", ServiceCaptureCopy.why("PAYMENT"))
        assertEquals("Versioned + effective-dated.", ServiceCaptureCopy.why("BUDGET"))
        assertEquals("Audit / action — approval ≠ disbursement.", ServiceCaptureCopy.why("ISSUE"))
    }

    @Test
    fun modeChipsMatchFigma() {
        assertEquals(
            listOf("From booking", "Walk-in charge", "Reprint"),
            ServiceCaptureCopy.modeChips("SERVICE"),
        )
        assertEquals(listOf("Consumables", "Equipment"), ServiceCaptureCopy.modeChips("PURCHASE"))
        assertEquals(listOf("Set current", "Add", "Reduce"), ServiceCaptureCopy.modeChips("STOCK"))
        assertEquals(listOf("Appointment", "Walk-in"), ServiceCaptureCopy.modeChips("APPOINTMENT"))
        assertEquals(listOf("Start", "Complete", "Reschedule"), ServiceCaptureCopy.modeChips("STATUS"))
        assertEquals(listOf("Charges", "Sessions"), ServiceCaptureCopy.modeChips("BUDGET"))
        assertEquals(listOf("Issue", "Approval", "Update"), ServiceCaptureCopy.modeChips("ISSUE"))
        assertTrue(ServiceCaptureCopy.modeChips("COLLECTION").isEmpty())
    }

    @Test
    fun draftFooterAndToastLocked() {
        assertEquals("Draft preserved if you close or open a picker", ServiceCaptureCopy.DRAFT_FOOTER)
        assertEquals("✓ Saved · sheet closes and returns back", ServiceCaptureCopy.SAVED_TOAST)
        assertEquals("Book. Deliver. Charge.", ServiceCaptureCopy.TAGLINE)
    }

    @Test
    fun sheetTitlesMatchFigma() {
        assertEquals("Finalize Charge", ServiceCaptureCopy.sheetTitle("SERVICE"))
        assertEquals("Record Collection", ServiceCaptureCopy.sheetTitle("COLLECTION"))
        assertEquals("Budget / Target Revision", ServiceCaptureCopy.sheetTitle("BUDGET"))
        assertEquals("Issue / Approval / Update", ServiceCaptureCopy.sheetTitle("ISSUE"))
        assertEquals("Status / Assignment", ServiceCaptureCopy.sheetTitle("STATUS"))
        assertEquals("Appointment hold", ServiceCaptureCopy.sheetTitle("HOLD"))
        assertEquals("Staff / room transfer", ServiceCaptureCopy.sheetTitle("TRANSFER"))
        assertEquals("Reprint invoice", ServiceCaptureCopy.sheetTitle("REPRINT"))
    }

    @Test
    fun utilityHoldTransferReprintCopyLocked() {
        assertEquals("Hold slot · HLD-22", ServiceCaptureCopy.cta("HOLD"))
        assertEquals("Confirm transfer · 2 bookings", ServiceCaptureCopy.cta("TRANSFER"))
        assertEquals("Reprint · duplicate copy", ServiceCaptureCopy.cta("REPRINT"))
        assertTrue(ServiceCaptureCopy.why("HOLD").contains("no revenue, no GST"))
        assertTrue(ServiceCaptureCopy.why("TRANSFER").contains("No revenue, no charge, no GST"))
        assertTrue(ServiceCaptureCopy.why("REPRINT").contains("No new charge"))
    }
}

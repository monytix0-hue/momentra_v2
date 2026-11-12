package com.example.momentra.ui.shell.business.restaurant

import com.example.momentra.data.api.OperatingActivityDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards Phase 2 Full KOT semantics:
 * Send → ORDER PLACED (never SALE); Fire → revises same id; Reprint keeps same clientKey.
 */
class RestaurantKotSemanticsTest {

    @Test
    fun sendPostsOrderPlacedNeverSale() {
        val draft = buildKotSendDraft(
            channel = KotChannel.InDining,
            lines = listOf(
                KotLine(masterId = "d1", name = "Butter chicken", unit = "plate", quantity = 1),
                KotLine(
                    masterId = "d2",
                    name = "Dal makhani",
                    unit = "bowl",
                    quantity = 1,
                    course = KotCourse.Hold,
                    station = "Hot",
                ),
            ),
            stationFilter = STATION_ALL,
            stations = listOf("Tandoor", "Hot"),
            tableNumber = "12",
            covers = "4",
            guest = "",
            note = "less spice",
            platform = "",
            platformOrder = "",
            assignee = "Chef Ama",
            clientKey = "KOT-442",
        )
        assertEquals("ORDER", draft.eventType)
        assertNotEquals("SALE", draft.eventType)
        assertEquals("PLACED", draft.contract?.orderStatus)
        assertEquals("NONE", draft.paymentStatus)
        assertNull(draft.revisesEventId)
        assertEquals("KOT-442", draft.contract?.clientKey)
        assertEquals("In-dining", draft.channelName)
        assertEquals("Table 12", draft.title)
        assertEquals("12", draft.contract?.tableNumber)
        assertEquals("4", draft.contract?.covers)
        assertTrue(draft.note.orEmpty().contains("Hold: Dal makhani"))
        assertTrue(draft.note.orEmpty().contains("Station:"))
        assertEquals(2, draft.lines?.size)
    }

    @Test
    fun firePatchesSameEventNeverSale() {
        val ticket = OperatingActivityDto(
            id = "evt-99",
            title = "Table 12",
            eventType = "ORDER",
            status = "PLACED",
            channelName = "In-dining",
            tableNumber = "12",
            covers = "4",
            locationName = "Hot",
            clientKey = "KOT-442",
            note = "Station: Hot · Hold: Dal makhani · less spice",
        )
        val draft = buildKotFireDraft(
            ticket = ticket,
            nextStatus = nextFireStatus(ticket.status)!!,
            heldNames = parseHeldLineNames(ticket.note),
        )
        assertEquals("ORDER", draft.eventType)
        assertNotEquals("SALE", draft.eventType)
        assertEquals("evt-99", draft.revisesEventId)
        assertEquals("PREPARING", draft.contract?.orderStatus)
        assertEquals("KOT-442", draft.contract?.clientKey)
        assertEquals("12", draft.contract?.tableNumber)
        assertFalse(draft.note.orEmpty().contains("Hold:"))
        assertTrue(draft.note.orEmpty().contains("Fired: Dal makhani"))
    }

    @Test
    fun fireAdvancesPreparingToReady() {
        assertEquals("READY", nextFireStatus("PREPARING"))
        assertNull(nextFireStatus("READY"))
        assertNull(nextFireStatus("COMPLETED"))
    }

    @Test
    fun reprintKeepsSameKotIdAndNeverAllocates() {
        val ticket = OperatingActivityDto(
            id = "evt-51",
            title = "ZOM-90821",
            eventType = "ORDER",
            status = "PREPARING",
            channelName = "Cloud",
            clientKey = "KOT-451",
            note = "Station: Cloud queue",
        )
        val share = formatKotReprintShare(ticket, reason = "Kitchen printer jam")
        assertTrue(share.contains("KOT-451"))
        assertTrue(share.contains("REPRINT · KOT-451"))
        assertTrue(share.contains("Same KOT id"))
        assertFalse(share.contains("KOT-452"))
        assertEquals("KOT-451", kotDisplayId(ticket))
        // Display never invents a fresh id when clientKey exists.
        assertEquals(ticket.clientKey, kotDisplayId(ticket))
    }

    @Test
    fun parseHeldLinesFromNote() {
        assertEquals(
            listOf("Dal makhani", "Jeera rice"),
            parseHeldLineNames("Station: Hot · Hold: Dal makhani, Jeera rice · note"),
        )
        assertEquals(emptyList<String>(), parseHeldLineNames("Station: Hot · all now"))
    }
}

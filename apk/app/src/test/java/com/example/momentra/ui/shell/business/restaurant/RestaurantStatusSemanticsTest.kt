package com.example.momentra.ui.shell.business.restaurant

import com.example.momentra.data.api.OperatingActivityDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards Phase 3 Status / Assignment semantics:
 * Update status → ORDER PATCH via revisesEventId; never SALE; Prepare/Ready/Fulfil map.
 */
class RestaurantStatusSemanticsTest {

    private val ticket = OperatingActivityDto(
        id = "evt-441",
        title = "Table 12",
        eventType = "ORDER",
        status = "PREPARING",
        channelName = "In-dining",
        tableNumber = "12",
        covers = "4",
        locationName = "Hot pass",
        clientKey = "KOT-441",
        note = "Station: Hot pass · Hold: Dal makhani · Assignee: Chef Ama · Fired: Butter chicken, Garlic naan",
        counterpartyName = "Kiran Rao",
    )

    @Test
    fun updateStatusNeverPostsSale() {
        val draft = buildStatusUpdateDraft(
            ticket = ticket,
            mode = StatusBoardMode.Ready,
            status = StatusBoardChip.Ready,
            station = "Hot pass",
            assignee = "Chef Ama",
            due = "Today · 7:30 pm",
        )
        assertEquals("ORDER", draft.eventType)
        assertNotEquals("SALE", draft.eventType)
        assertNull(draft.amount)
        assertEquals("NONE", draft.paymentStatus)
        assertEquals("evt-441", draft.revisesEventId)
        assertEquals("READY", draft.contract?.orderStatus)
        assertEquals("KOT-441", draft.contract?.clientKey)
        assertEquals("12", draft.contract?.tableNumber)
        assertEquals("Chef Ama", draft.contract?.staffName)
        assertEquals("Ready", draft.contract?.deliveryStatus)
        assertTrue(draft.note.orEmpty().contains("Mode: Ready"))
        assertTrue(draft.note.orEmpty().contains("Due: Today · 7:30 pm"))
    }

    @Test
    fun prepareMapsToPreparing() {
        assertEquals("PREPARING", StatusBoardChip.Preparing.orderStatus)
        assertEquals(StatusBoardChip.Preparing, defaultChipForMode(StatusBoardMode.Prepare))
        val draft = buildStatusUpdateDraft(
            ticket = ticket,
            mode = StatusBoardMode.Prepare,
            status = defaultChipForMode(StatusBoardMode.Prepare),
            station = "Hot pass",
            assignee = "Chef Ama",
            due = "Today · 7:30 pm",
        )
        assertEquals("PREPARING", draft.contract?.orderStatus)
        assertEquals("Fulfil / serve", statusNextLabel(StatusBoardMode.Ready, StatusBoardChip.Ready))
        assertEquals("Ready at pass", statusNextLabel(StatusBoardMode.Prepare, StatusBoardChip.Preparing))
        assertEquals("Close ticket", statusNextLabel(StatusBoardMode.Fulfil, StatusBoardChip.Served))
    }

    @Test
    fun fulfilMapsToCompletedNeverSale() {
        val draft = buildStatusUpdateDraft(
            ticket = ticket,
            mode = StatusBoardMode.Fulfil,
            status = StatusBoardChip.Served,
            station = "Hot pass",
            assignee = "Chef Ama",
            due = "Today · 8:00 pm",
        )
        assertEquals("COMPLETED", draft.contract?.orderStatus)
        assertNotEquals("SALE", draft.eventType)
        assertNull(draft.amount)
    }

    @Test
    fun boardLinesPreferFiredAndHoldMarkers() {
        val lines = statusBoardLines(ticket, assignee = "Chef Ama", station = "Hot pass")
        assertEquals(3, lines.size)
        assertTrue(lines.any { it.name == "Butter chicken" && it.chip == "Ready" })
        assertTrue(lines.any { it.name == "Garlic naan" && it.chipPositive })
        assertTrue(lines.any { it.name == "Dal makhani" && it.chip == "Preparing" })
        assertFalse(lines.any { it.name.contains("invent", ignoreCase = true) })
    }

    @Test
    fun boardLinesFallBackToTicketSummaryWithoutInventingDishes() {
        val bare = ticket.copy(note = "Station: Tandoor · Assignee: Chef Ama")
        val lines = statusBoardLines(bare, assignee = "Chef Ama", station = "Tandoor")
        assertEquals(1, lines.size)
        assertEquals("Table 12", lines.single().name)
        assertEquals("Preparing", lines.single().chip)
    }
}

package com.example.momentra.ui.shell.group

import com.example.momentra.data.api.GroupExpenseSplitInputDto
import com.example.momentra.data.repository.GroupExpenseSplitBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GroupActionRegistryTest {

    @Test
    fun mapsV019CapabilityCodesToDestinations() {
        assertEquals(
            GroupActionRegistry.Destination.EXPENSE,
            GroupActionRegistry.destinationFor(GroupActionRegistry.EXPENSE_CREATE),
        )
        assertEquals(
            GroupActionRegistry.Destination.CONTRIBUTION,
            GroupActionRegistry.destinationFor(GroupActionRegistry.CONTRIBUTION_RECORD),
        )
        assertEquals(
            GroupActionRegistry.Destination.SETTLEMENT,
            GroupActionRegistry.destinationFor(GroupActionRegistry.SETTLEMENT_RECORD),
        )
        assertEquals(
            GroupActionRegistry.Destination.PARTICIPANTS,
            GroupActionRegistry.destinationFor(GroupActionRegistry.PARTICIPANT_MANAGE),
        )
        assertNull(GroupActionRegistry.destinationFor("UNKNOWN_CODE"))
    }

    @Test
    fun emptyCapabilitiesEnableLiveTilesIncludingSettle() {
        assertTrue(
            GroupActionRegistry.isDestinationEnabled(emptyList(), GroupActionRegistry.Destination.EXPENSE),
        )
        assertTrue(
            GroupActionRegistry.isDestinationEnabled(emptyList(), GroupActionRegistry.Destination.CONTRIBUTION),
        )
        assertTrue(
            GroupActionRegistry.isDestinationEnabled(emptyList(), GroupActionRegistry.Destination.SETTLEMENT),
        )
        assertTrue(
            GroupActionRegistry.isDestinationEnabled(emptyList(), GroupActionRegistry.Destination.PLANNING),
        )
    }

    @Test
    fun nonEmptyCapabilitiesFilterDestinations() {
        val caps = listOf(GroupActionRegistry.EXPENSE_CREATE)
        assertTrue(
            GroupActionRegistry.isDestinationEnabled(caps, GroupActionRegistry.Destination.EXPENSE),
        )
        assertFalse(
            GroupActionRegistry.isDestinationEnabled(caps, GroupActionRegistry.Destination.CONTRIBUTION),
        )
        assertTrue(
            GroupActionRegistry.isDestinationEnabled(
                listOf(GroupActionRegistry.SETTLEMENT_RECORD),
                GroupActionRegistry.Destination.SETTLEMENT,
            ),
        )
    }

    @Test
    fun settleHubTilePresentAndEnabledWithCapability() {
        val settle = GroupActionRegistry.figmaHubTiles.find { it.id == "settle" }
        assertEquals(GroupActionRegistry.Destination.SETTLEMENT, settle!!.destination)
        assertEquals(GroupActionRegistry.SETTLEMENT_RECORD, settle.capabilityCode)
        assertTrue(
            GroupActionRegistry.hubTileEnabled(
                hasActiveMoment = true,
                capabilities = listOf(GroupActionRegistry.SETTLEMENT_RECORD),
                tile = settle,
            ),
        )
    }

    @Test
    fun planningHubTileLiveWithoutApiGap() {
        val planning = GroupActionRegistry.figmaHubTiles.find { it.id == "planning" }
        assertEquals(false, planning!!.apiGap)
        assertEquals(GroupActionRegistry.PLANNING_ITEM_CREATE, planning.capabilityCode)
        assertTrue(
            GroupActionRegistry.hubTileEnabled(
                hasActiveMoment = true,
                capabilities = listOf(GroupActionRegistry.PLANNING_ITEM_CREATE),
                tile = planning,
            ),
        )
    }
}

class GroupExpenseSplitBuilderTest {

    @Test
    fun equalSplitRequestBodyUsesParticipantIdsOnly() {
        val body = GroupExpenseSplitBuilder.equalSplit(
            amount = "100.00",
            currencyCode = "inr",
            paidByParticipantId = "payer-1",
            participantIds = listOf("payer-1", "member-2"),
            description = "Dinner",
        )
        assertEquals("100.00", body.amount)
        assertEquals("INR", body.currencyCode)
        assertEquals("Dinner", body.description)
        assertEquals("payer-1", body.paidByParticipantId)
        assertEquals("EQUAL", body.splitStrategy)
        assertEquals(2, body.splitInputs.size)
        assertEquals("payer-1", body.splitInputs[0].participantId)
        assertNull(body.splitInputs[0].amount)
        assertNull(body.splitInputs[0].percent)
        assertNull(body.splitInputs[0].shares)
        assertEquals("member-2", body.splitInputs[1].participantId)
    }

    @Test
    fun percentageSplitSendsPercentInputs() {
        val body = GroupExpenseSplitBuilder.build(
            amount = "200.00",
            currencyCode = "INR",
            paidByParticipantId = "payer-1",
            splitStrategy = "PERCENTAGE",
            splitInputs = listOf(
                GroupExpenseSplitInputDto(participantId = "payer-1", percent = "60"),
                GroupExpenseSplitInputDto(participantId = "member-2", percent = "40"),
            ),
        )
        assertEquals("PERCENTAGE", body.splitStrategy)
        assertEquals("60", body.splitInputs[0].percent)
        assertEquals("40", body.splitInputs[1].percent)
    }
}

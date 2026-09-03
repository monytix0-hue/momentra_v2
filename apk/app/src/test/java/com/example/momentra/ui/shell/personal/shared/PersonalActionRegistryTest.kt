package com.example.momentra.ui.shell.personal.shared

import com.example.momentra.data.api.ActivityItemDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import com.example.momentra.ui.shell.personal.relationships.create.mapActivityDtosToRelationships
import com.example.momentra.ui.shell.personal.relationships.create.RelationshipsActivityItem

class PersonalActionRegistryTest {

    @Test
    fun mapsV019CapabilityCodesToDestinations() {
        assertEquals(
            PersonalActionRegistry.Destination.EXPENSE,
            PersonalActionRegistry.destinationFor(PersonalActionRegistry.EXPENSE_CREATE),
        )
        assertEquals(
            PersonalActionRegistry.Destination.LIFE_OPS,
            PersonalActionRegistry.destinationFor(PersonalActionRegistry.LIFE_OBSERVATION_RECORD),
        )
        assertEquals(
            PersonalActionRegistry.Destination.FUTURE,
            PersonalActionRegistry.destinationFor(PersonalActionRegistry.GOAL_CREATE),
        )
        assertEquals(
            PersonalActionRegistry.Destination.FUTURE,
            PersonalActionRegistry.destinationFor(PersonalActionRegistry.MILESTONE_CREATE),
        )
        assertEquals(
            PersonalActionRegistry.Destination.LIFESTYLE,
            PersonalActionRegistry.destinationFor(PersonalActionRegistry.LIFESTYLE_ACTIVITY_CREATE),
        )
        assertEquals(
            PersonalActionRegistry.Destination.RELATIONSHIPS,
            PersonalActionRegistry.destinationFor(PersonalActionRegistry.RELATIONSHIP_ACTIVITY_RECORD),
        )
        assertEquals(
            PersonalActionRegistry.Destination.MOVEMENT,
            PersonalActionRegistry.destinationFor(PersonalActionRegistry.MOVEMENT_RECORD),
        )
    }

    @Test
    fun emptyCapabilitiesFailClosed() {
        assertFalse(
            PersonalActionRegistry.isDestinationEnabled(emptyList(), PersonalActionRegistry.Destination.EXPENSE),
        )
        assertFalse(
            PersonalActionRegistry.isDestinationEnabled(emptyList(), PersonalActionRegistry.Destination.RELATIONSHIPS),
        )
        assertFalse(
            PersonalActionRegistry.isDestinationEnabled(emptyList(), PersonalActionRegistry.Destination.MOVEMENT),
        )
        assertTrue(PersonalActionRegistry.enabledDestinations(emptyList()).isEmpty())
    }

    @Test
    fun nonEmptyCapabilitiesFilterDestinations() {
        val caps = listOf(PersonalActionRegistry.EXPENSE_CREATE, PersonalActionRegistry.RELATIONSHIP_ACTIVITY_RECORD)
        assertTrue(
            PersonalActionRegistry.isDestinationEnabled(caps, PersonalActionRegistry.Destination.EXPENSE),
        )
        assertTrue(
            PersonalActionRegistry.isDestinationEnabled(caps, PersonalActionRegistry.Destination.RELATIONSHIPS),
        )
        assertFalse(
            PersonalActionRegistry.isDestinationEnabled(caps, PersonalActionRegistry.Destination.LIFESTYLE),
        )
    }
}

class RelationshipsActivityModelsTest {

    @Test
    fun mapActivityDtosEmptyReturnsEmpty() {
        assertEquals(emptyList<RelationshipsActivityItem>(), mapActivityDtosToRelationships(emptyList()))
    }

    @Test
    fun mapActivityDtosMapsTitles() {
        val mapped = mapActivityDtosToRelationships(
            listOf(
                ActivityItemDto(
                    activityCode = "RELATIONSHIP_CONNECTION",
                    title = "Morning check-in",
                    occurredAt = "2026-08-26T10:00:00Z",
                ),
            ),
        )
        assertEquals(1, mapped.size)
        assertEquals("Morning check-in", mapped[0].title)
    }
}

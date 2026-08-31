package com.example.momentra.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MomentExperienceTest {

    @Test
    fun emptyListIsFirstMoment() {
        assertEquals(MomentExperienceKind.FIRST_MOMENT, resolveMomentExperience(emptyList()))
    }

    @Test
    fun activeWins() {
        val moments = listOf(
            MomentSummary("1", "Trip", "COMPLETED"),
            MomentSummary("2", "Now", "ACTIVE"),
        )
        assertEquals(MomentExperienceKind.ACTIVE, resolveMomentExperience(moments))
        assertEquals(1, activeMomentCount(moments))
    }

    @Test
    fun draftCountsAsActive() {
        assertEquals(
            MomentExperienceKind.ACTIVE,
            resolveMomentExperience(listOf(MomentSummary("1", "Draft", "DRAFT"))),
        )
    }

    @Test
    fun completedOnlyIsBetweenMoments() {
        assertEquals(
            MomentExperienceKind.BETWEEN_MOMENTS,
            resolveMomentExperience(listOf(MomentSummary("1", "Done", "COMPLETED"))),
        )
    }

    @Test
    fun pausedOnly() {
        assertEquals(
            MomentExperienceKind.PAUSED_ONLY,
            resolveMomentExperience(listOf(MomentSummary("1", "Hold", "PAUSED"))),
        )
    }

    @Test
    fun recentHistoryExcludesActiveAndArchived() {
        val moments = listOf(
            MomentSummary("1", "A", "ACTIVE"),
            MomentSummary("2", "B", "COMPLETED"),
            MomentSummary("3", "C", "ARCHIVED"),
            MomentSummary("4", "D", "CANCELLED"),
        )
        val recent = recentHistoryMoments(moments, limit = 5)
        assertEquals(2, recent.size)
        assertTrue(recent.none { it.status == "ACTIVE" || it.status == "ARCHIVED" })
    }
}

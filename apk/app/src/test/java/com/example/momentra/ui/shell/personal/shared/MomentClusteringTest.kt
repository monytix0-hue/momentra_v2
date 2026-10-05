package com.example.momentra.ui.shell.personal.shared

import com.example.momentra.data.api.ActivityItemDto
import com.example.momentra.data.api.ActivityPayloadDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MomentClusteringTest {

    @Test
    fun complementarySpendMoodAndRecoveryClusterAndTapOpensTheSpend() {
        val cards = MomentClustering.cards(
            listOf(
                activity("RECOVERY", "Walk", "2026-10-01T14:35:00Z", activityId = "rec-1"),
                activity("MOOD", "Calm", "2026-10-01T14:32:00Z", activityId = "mood-1"),
                activity("EXPENSE", "GateCheck1111", "2026-10-01T14:31:00Z", expenseId = "exp-1"),
            ),
            PersonalPulseFamily.LIFE_OPERATIONS,
        )
        assertEquals(1, cards.size)
        val card = cards.single()
        assertEquals(listOf("exp-1", "rec-1", "mood-1"), card.sourceActivityIds)
        assertTrue(card.title.contains("GateCheck1111"))
        assertEquals("EXPENSE", card.activityCode)
    }

    @Test
    fun sameWindowUnrelatedActivitiesStaySeparate() {
        val cards = MomentClustering.cards(
            listOf(
                activity("LEARNING", "GateLearningGamma", "2026-10-01T15:00:00Z", activityId = "learn-1"),
                activity("MILESTONE", "GateMilestoneAlpha", "2026-10-01T15:05:00Z", activityId = "mile-1"),
            ),
            PersonalPulseFamily.FUTURE_BUILDING,
        )
        assertEquals(2, cards.size)
    }

    @Test
    fun repeatedSpendsDoNotClusterUnlessTheyAreTheSameMerchant() {
        val cards = MomentClustering.cards(
            listOf(
                activity("EXPENSE", "Coffee", "2026-10-01T16:00:00Z", expenseId = "a", merchant = "Cafe"),
                activity("EXPENSE", "Groceries", "2026-10-01T16:10:00Z", expenseId = "b", merchant = "Market"),
            ),
            PersonalPulseFamily.LIFE_OPERATIONS,
        )
        assertEquals(2, cards.size)
    }

    private fun activity(
        code: String,
        title: String,
        at: String,
        activityId: String? = null,
        expenseId: String? = null,
        merchant: String? = null,
    ) = ActivityItemDto(
        activityCode = code,
        title = title,
        occurredAt = at,
        activityPayload = ActivityPayloadDto(
            activityId = activityId,
            expenseId = expenseId,
            merchantName = merchant ?: title,
        ),
    )
}

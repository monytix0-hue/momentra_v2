package com.example.momentra.ui.shell.business.shared

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class BusinessLifePresentationTest {
    private val zone = ZoneId.of("Asia/Kolkata")
    private val now = Instant.parse("2026-09-30T12:00:00Z")

    @Test
    fun lensesShareCompanyStateAndOnlyTheMovementTabChanges() {
        val facts = sample()
        val state = buildBusinessLifeCompanyState(facts, now, zone)
        assertEquals(state, buildBusinessLifeCompanyState(facts, now, zone))
        assertEquals(BusinessLifeMovementTab.ACTIVITY, movementTabFor(BusinessLifeLens.OVERVIEW))
        assertEquals(BusinessLifeMovementTab.ACTIVITY, movementTabFor(BusinessLifeLens.DAILY))
        assertEquals(BusinessLifeMovementTab.ACTIVITY, movementTabFor(BusinessLifeLens.TEAM))
        assertEquals(BusinessLifeMovementTab.MONEY, movementTabFor(BusinessLifeLens.MONEY))
        assertEquals(BusinessLifeLens.MONEY, businessLifeLens("BUSINESS_RUNWAY"))
        assertEquals(BusinessLifeLens.DAILY, businessLifeLens("BUSINESS_OPERATIONS"))
        assertEquals(BusinessLifeLens.TEAM, businessLifeLens("TEAM_OPERATIONS"))
        assertEquals(BusinessLifeLens.OVERVIEW, businessLifeLens(null))
        assertEquals("Money", state.overview[0].title)
        assertEquals("Needs attention", state.overview[0].state)
        assertEquals("Watch", state.overview[1].state)
        assertEquals("Not set up", state.overview[2].state)
    }

    @Test
    fun thisWeekCountsOnlyTheRecentWindow() {
        val state = buildBusinessLifeCompanyState(sample(), now, zone)
        assertEquals("1 recent company event is from this week.", state.thisWeek)
        assertEquals(2, state.activity.size)
    }

    @Test
    fun zeroCashStaysAndMissingRevenueIsOmitted() {
        val state = buildBusinessLifeCompanyState(
            sample().copy(
                runway = mapOf("availableCash" to "0", "monthlyRevenue" to ""),
                runwayStatus = null,
                runwayMonths = null,
                signals = emptyList(),
                sections = mapOf("runway" to "REAL_DATA"),
            ),
            now,
            zone,
        )
        assertEquals(listOf(BusinessLifeValueLine("Available cash", "0")), state.financialPosition)
        assertFalse(state.financialPosition.any { it.label == "Revenue" })
        assertEquals(BUSINESS_LIFE_FINANCE_SCOPE, "Company totals")
        assertTrue(state.needsAttention.isEmpty())
        assertEquals("Recorded", state.overview[0].state)
    }

    @Test
    fun attentionAndWorkingStayCompanyWide() {
        val state = buildBusinessLifeCompanyState(sample(), now, zone)
        assertEquals(listOf("Cash is tight", "Spend is high"), state.needsAttention)
        assertEquals(listOf("Invoice collected"), state.working)
    }

    private fun sample() = BusinessLifeFacts(
        sections = mapOf(
            "runway" to "REAL_DATA",
            "businessOperations" to "REAL_DATA",
            "teamOperations" to "EMPTY_SUPPORTED",
        ),
        runway = mapOf(
            "statusLabel" to "Scaling",
            "availableCash" to "50000",
            "revenueTotal" to "8000",
            "expenseTotal" to "10000",
        ),
        daily = mapOf("statusLabel" to "Daily Business", "monthlyBudget" to "80000"),
        team = emptyMap(),
        runwayStatus = "Scaling",
        dailyStatus = "Daily Business",
        runwayMonths = "5.0",
        signals = listOf(
            BusinessLifeSignalFact("Cash is tight", "RUNWAY", "Action"),
            BusinessLifeSignalFact("Spend is high", "OPERATIONS", "Watch"),
            BusinessLifeSignalFact("Invoice collected", "RUNWAY", "Healthy"),
        ),
        activity = listOf(
            BusinessLifeActivityFact("Till sale", "2026-09-30T05:12:00Z", "BUSINESS_REVENUE"),
            BusinessLifeActivityFact("Old expense", "2026-09-22T05:12:00Z", "BUSINESS_EXPENSE"),
        ),
    )
}

package com.example.momentra.ui.shell.business.shared

import com.example.momentra.data.api.ActivityItemDto
import com.example.momentra.data.api.BusinessFinancePayloadDto
import com.example.momentra.data.api.BusinessFinanceTotalDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BusinessPulsePresentationTest {

    @Test
    fun emptyFinanceIsUnavailableNotZero() {
        val model = buildBusinessPulsePresentation(
            BusinessPulseFactsInput(
                family = BusinessMomentFamilyConfig.Family.MONEY,
                smallShop = false,
                capabilities = emptyList(),
                momentTypeCode = "BUSINESS_RUNWAY",
                finance = BusinessFinancePayloadDto(
                    dataQuality = "EMPTY",
                    totals = listOf(BusinessFinanceTotalDto(currencyCode = "INR", expenseTotal = "0", revenueTotal = "0")),
                ),
            ),
        )
        val revenue = model.facts.first { it.label == "Revenue" }
        assertTrue(revenue.metric is PulseMetric.Unavailable)
        assertFalse(model.displayedFacts().any { it.label == "Revenue" })
    }

    @Test
    fun realZeroStaysZero() {
        val model = money("REAL_DATA", revenue = "0", expense = "120")
        val revenue = model.facts.first { it.label == "Revenue" }
        assertEquals(PulseMetric.Zero, revenue.metric)
        assertTrue(model.displayedFacts().any { it.label == "Revenue" && it.metric == PulseMetric.Zero })
    }

    @Test
    fun heroAndSnapshotShareDisplayedFacts() {
        val model = money("REAL_DATA", revenue = "480000", expense = "310000")
        assertEquals(model.displayedFacts(), model.displayedFacts())
        assertTrue(model.showsCompanyTotals())
        assertFalse(model.facts.any { it.label.contains("Strong") || it.label.contains("health", ignoreCase = true) })
    }

    @Test
    fun teamFactsDoNotUseFinanceScore() {
        val model = buildBusinessPulsePresentation(
            BusinessPulseFactsInput(
                family = BusinessMomentFamilyConfig.Family.TEAM,
                smallShop = false,
                capabilities = emptyList(),
                momentTypeCode = "TEAM_OPERATIONS",
                finance = BusinessFinancePayloadDto(
                    dataQuality = "REAL_DATA",
                    totals = listOf(BusinessFinanceTotalDto(currencyCode = "INR", revenueTotal = "999")),
                ),
                rosterCount = 8,
                approvals = emptyList(),
            ),
        )
        assertTrue(model.facts.none { it.label.contains("health", ignoreCase = true) })
        assertTrue(model.facts.none { it.label == "Revenue" })
        assertEquals(PulseMetric.Available("8"), model.facts.first { it.label == "People" }.metric)
    }

    @Test
    fun lifeFailureOmitsCashAndRunwayButKeepsRevenue() {
        val model = buildBusinessPulsePresentation(
            BusinessPulseFactsInput(
                family = BusinessMomentFamilyConfig.Family.MONEY,
                smallShop = false,
                capabilities = emptyList(),
                momentTypeCode = "BUSINESS_RUNWAY",
                finance = BusinessFinancePayloadDto(
                    dataQuality = "REAL_DATA",
                    totals = listOf(BusinessFinanceTotalDto(currencyCode = "INR", revenueTotal = "10", expenseTotal = "4")),
                ),
                runwayMonths = "6",
                availableCash = "500",
                lifeFailed = true,
            ),
        )
        assertTrue(model.facts.first { it.label == "Cash" }.metric is PulseMetric.Unavailable)
        assertTrue(model.facts.first { it.label == "Runway" }.metric is PulseMetric.Unavailable)
        assertTrue(model.displayedFacts().any { it.label == "Revenue" })
    }

    @Test
    fun missingSectionQualityIsUnavailable() {
        val model = buildBusinessPulsePresentation(
            BusinessPulseFactsInput(
                family = BusinessMomentFamilyConfig.Family.DAILY,
                smallShop = true,
                capabilities = emptyList(),
                momentTypeCode = "BUSINESS_OPERATIONS",
                operations = com.example.momentra.data.api.OpsPulseExtrasDto(
                    openIssueCount = 0,
                    activeVendorCount = 0,
                    monthlySpend = "0",
                ),
            ),
        )
        assertTrue(model.facts.all { it.metric is PulseMetric.Unavailable })
        assertTrue(model.displayedFacts().isEmpty())
    }

    @Test
    fun emptyAttentionUsesCalmLineOnlyWhenLoaded() {
        val loaded = buildBusinessPulsePresentation(
            BusinessPulseFactsInput(
                family = BusinessMomentFamilyConfig.Family.DAILY,
                smallShop = false,
                capabilities = emptyList(),
                momentTypeCode = "BUSINESS_OPERATIONS",
                issues = emptyList(),
            ),
        )
        assertEquals(BusinessPulsePresentation.ATTENTION_EMPTY, loaded.attentionEmptyLine)
        val missing = buildBusinessPulsePresentation(
            BusinessPulseFactsInput(
                family = BusinessMomentFamilyConfig.Family.DAILY,
                smallShop = false,
                capabilities = emptyList(),
                momentTypeCode = "BUSINESS_OPERATIONS",
                issues = null,
            ),
        )
        assertNull(missing.attentionEmptyLine)
    }

    @Test
    fun recentHeadingAndOtherActorCards() {
        assertEquals("Recent activity", BusinessPulsePresentation.RECENT_TITLE)
        val named = ActivityItemDto(
            activityCode = "EXPENSE_RECORDED",
            title = "Shop expense",
            occurredAt = "2026-09-30T08:00:00.000Z",
            actorDisplayName = "Member B",
        )
        val unnamed = named.copy(actorDisplayName = "  ")
        val model = buildBusinessPulsePresentation(
            BusinessPulseFactsInput(
                family = BusinessMomentFamilyConfig.Family.MONEY,
                smallShop = false,
                capabilities = emptyList(),
                momentTypeCode = "BUSINESS_RUNWAY",
                activities = listOf(named, unnamed),
            ),
        )
        assertEquals(2, model.recent.size)
        assertEquals("Shop expense", model.recent[0].title)
        assertEquals("Member B", model.recent[0].actorDisplayName)
        assertEquals("Shop expense", model.recent[1].title)
        assertNull(model.recent[1].actorDisplayName)
        assertEquals(model.recent[0].id, model.recent[1].id)
    }

    @Test
    fun invalidateDropsOnlyThatMoment() {
        val id = "b1-cache-${System.nanoTime()}"
        val other = "$id-other"
        val tab = BusinessTabDataCache.PulseTab(
            pulse = null,
            finance = null,
            life = null,
            activities = emptyList(),
            businessFamily = null,
            facetStatus = null,
        )
        BusinessTabDataCache.putPulse(id, tab)
        BusinessTabDataCache.putPulse(other, tab)
        invalidateBusinessPulseLoad(id)
        assertNull(BusinessTabDataCache.peekPulse(id))
        assertTrue(BusinessTabDataCache.peekPulse(other) != null)
        BusinessTabDataCache.invalidateMoment(other)
    }

    private fun money(quality: String, revenue: String, expense: String) = buildBusinessPulsePresentation(
        BusinessPulseFactsInput(
            family = BusinessMomentFamilyConfig.Family.MONEY,
            smallShop = false,
            capabilities = emptyList(),
            momentTypeCode = "BUSINESS_RUNWAY",
            finance = BusinessFinancePayloadDto(
                dataQuality = quality,
                totals = listOf(
                    BusinessFinanceTotalDto(
                        currencyCode = "INR",
                        expenseTotal = expense,
                        revenueTotal = revenue,
                    ),
                ),
            ),
        ),
    )
}

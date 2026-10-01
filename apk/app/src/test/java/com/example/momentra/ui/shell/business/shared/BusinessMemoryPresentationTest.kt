package com.example.momentra.ui.shell.business.shared

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class BusinessMemoryPresentationTest {
    private val zone = ZoneId.of("Asia/Kolkata")
    private val now = Instant.parse("2026-09-30T12:00:00Z")

    @Test
    fun mapperIgnoresLensAndChipsFilterOnlyWorthRemembering() {
        val items = listOf(
            fact("Cash risk", family = "BUSINESS_RUNWAY", at = "2026-09-28T04:00:00Z", type = "DECISION"),
            fact("Vendor issue", family = "BUSINESS_OPERATIONS", at = "2026-09-30T05:12:00Z", type = "NOTE"),
            fact("Shift plan", family = "TEAM_OPERATIONS", at = "2026-09-30T05:12:00Z", type = "NOTE"),
        )
        val company = buildBusinessMemoryPresentation(items, now, zone)
        val again = buildBusinessMemoryPresentation(items, now, zone)
        val money = filterWorthRemembering(company.worthRemembering, BusinessLifeLens.MONEY)
        val daily = filterWorthRemembering(company.worthRemembering, BusinessLifeLens.DAILY)
        assertEquals(listOf("Cash risk"), money.map { it.title })
        assertEquals(listOf("Vendor issue"), daily.map { it.title })
        assertEquals(3, filterWorthRemembering(company.worthRemembering, BusinessLifeLens.OVERVIEW).size)
        assertEquals(company.heroPeriod, again.heroPeriod)
        assertEquals(company.heroSentence, again.heroSentence)
        assertEquals(company.pattern, again.pattern)
        assertEquals(company.worked, again.worked)
        assertEquals(company.didnt, again.didnt)
        assertEquals(company.thenNow, again.thenNow)
        assertEquals("2 memories mention risk", company.pattern?.title)
        assertEquals(listOf("Shift plan"), company.worked)
        assertEquals(2, company.didnt.size)
    }

    @Test
    fun delayAndOverdueAreNotRisk() {
        assertFalse(businessMemoryIsRisk("Shipment delay", "overdue follow-up"))
        assertFalse(businessMemoryIsRisk("Payment overdue", null))
        assertTrue(businessMemoryIsRisk("Margin risk", null))
        assertTrue(businessMemoryIsRisk("Open issue", null))
        assertTrue(businessMemoryIsRisk("Safety incident", null))
        val built = buildBusinessMemoryPresentation(
            listOf(
                fact("Shipment delay", body = "overdue follow-up"),
                fact("Payment overdue"),
            ),
            now,
            zone,
        )
        assertEquals(listOf("Shipment delay", "Payment overdue"), built.worked)
        assertTrue(built.didnt.isEmpty())
        assertNull(built.pattern)
    }

    @Test
    fun repeatedTypeIsTheOnePatternAndASingleMemoryIsNot() {
        val repeated = buildBusinessMemoryPresentation(
            listOf(
                fact("First close", type = "DECISION"),
                fact("Second close", type = "DECISION"),
            ),
            now,
            zone,
        )
        assertEquals("2 Decision memories", repeated.pattern?.title)
        assertEquals(listOf("First close", "Second close"), repeated.pattern?.evidence)
        val single = buildBusinessMemoryPresentation(listOf(fact("Only one", type = "DECISION")), now, zone)
        assertNull(single.pattern)
        val tie = buildBusinessMemoryPresentation(
            listOf(
                fact("Cash risk", type = "DECISION"),
                fact("Tax risk", type = "DECISION"),
            ),
            now,
            zone,
        )
        assertEquals("2 memories mention risk", tie.pattern?.title)
    }

    @Test
    fun thenNowUsesTwoDifferentLocalDates() {
        val built = buildBusinessMemoryPresentation(
            listOf(
                fact("Opened the books", at = "2026-09-28T04:00:00Z"),
                fact("Closed the week", at = "2026-09-30T05:12:00Z"),
            ),
            now,
            zone,
        )
        assertEquals("28 Sep 2026 – 30 Sep 2026", built.heroPeriod)
        assertEquals("Opened the books", built.thenNow?.thenTitle)
        assertEquals("28 Sep 2026", built.thenNow?.thenDate)
        assertEquals("Closed the week", built.thenNow?.nowTitle)
        assertEquals("30 Sep 2026", built.thenNow?.nowDate)
    }

    @Test
    fun sameKolkataDayAcrossUtcMidnightOmitsThenNow() {
        val built = buildBusinessMemoryPresentation(
            listOf(
                fact("After midnight", at = "2026-09-29T20:30:00Z"),
                fact("Morning", at = "2026-09-30T02:00:00Z"),
            ),
            now,
            zone,
        )
        assertEquals("30 Sep 2026", built.heroPeriod)
        assertEquals("30 Sep 2026", built.worthRemembering[0].dateLabel)
        assertEquals("30 Sep 2026", built.worthRemembering[1].dateLabel)
        assertNull(built.thenNow)
    }

    @Test
    fun emptyCompanyOmitsPatternAndThenNowAndEmptyBodyIsNotALearning() {
        val empty = buildBusinessMemoryPresentation(emptyList(), now, zone)
        assertNull(empty.pattern)
        assertNull(empty.thenNow)
        assertEquals("Nothing saved yet.", empty.heroSentence)
        assertEquals("Company memory", empty.heroPeriod)
        val blank = buildBusinessMemoryPresentation(
            listOf(fact("Cash note", body = "")),
            now,
            zone,
        )
        assertNull(blank.pattern)
        assertNull(blank.thenNow)
        assertEquals("1 saved memories.", blank.heroSentence)
        assertEquals(listOf("Cash note"), blank.worked)
        assertEquals("Cash note", blank.worthRemembering.single().title)
        assertFalse(blank.heroSentence.contains("Cash note"))
    }

    private fun fact(
        title: String,
        body: String? = null,
        family: String? = null,
        at: String? = null,
        type: String? = null,
    ) = BusinessMemoryItemFact(
        title = title,
        body = body,
        occurredAt = at,
        memoryType = type,
        businessFamily = family,
    )
}

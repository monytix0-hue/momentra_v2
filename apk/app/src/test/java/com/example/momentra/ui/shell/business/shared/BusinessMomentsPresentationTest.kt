package com.example.momentra.ui.shell.business.shared

import com.example.momentra.data.api.ActivityItemDto
import com.example.momentra.data.api.ActivityPayloadDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class BusinessMomentsPresentationTest {
    private val zone = ZoneId.of("Asia/Kolkata")
    private val now = Instant.parse("2026-09-30T12:00:00Z")

    @Test
    fun moneyMapsRevenueExpenseAndInvoice() {
        val model = present(
            BusinessMomentFamilyConfig.Family.MONEY,
            listOf(
                item(
                    "BUSINESS_REVENUE",
                    "Till sale",
                    "2026-09-30T05:12:00Z",
                    ActivityPayloadDto(revenueId = "rev-1", amount = "12500.0000", currencyCode = "INR", status = "POSTED"),
                    actor = "Santosh",
                ),
                item(
                    "BUSINESS_EXPENSE",
                    "Shop expense",
                    "2026-09-30T04:00:00Z",
                    ActivityPayloadDto(expenseId = "exp-1", amount = "3250.0000", currencyCode = "INR", categoryCode = "RENT"),
                    actor = "  ",
                ),
                item(
                    "BUSINESS_INVOICE",
                    "Invoice INV-9",
                    "2026-09-29T03:48:00Z",
                    ActivityPayloadDto(invoiceId = "inv-1", invoiceNumber = "INV-9", totalAmount = "18000.0000", merchantName = "ABC Traders"),
                ),
            ),
        )
        val cards = model.groups.flatMap { it.entries }.map { it.card }
        assertEquals("₹12,500", cards[0].amountLabel)
        assertEquals("by Santosh", model.groups[0].entries[0].actorLine)
        assertEquals("₹3,250", cards[1].amountLabel)
        assertEquals("Rent", cards[1].subtitle)
        assertNull(cards[1].actorDisplayName)
        assertNull(model.groups.flatMap { it.entries }[1].actorLine)
        assertEquals("18,000", cards[2].amountLabel)
        assertNull(cards[2].subtitle)
        assertFalse(cards[2].title.contains("ABC"))
        assertEquals(listOf("rev-1"), cards[0].sourceActivityIds)
        assertTrue(model.chips.none { it.label.contains("Vendor") || it.label.contains("Khata") })
    }

    @Test
    fun dailyMapsIssueAndUpdateAndDoesNotInventVendor() {
        val model = present(
            BusinessMomentFamilyConfig.Family.DAILY,
            listOf(
                item(
                    "ISSUE_REPORTED",
                    "Delivery delayed",
                    "2026-09-30T05:12:00Z",
                    ActivityPayloadDto(issueId = "iss-1", severity = "HIGH"),
                    actor = "Priya",
                ),
                item(
                    "BUSINESS_UPDATE",
                    "Shift note",
                    "2026-09-30T04:00:00Z",
                    ActivityPayloadDto(updateId = "upd-1"),
                ),
                item(
                    "VENDOR_UPDATED",
                    "",
                    "2026-09-30T03:00:00Z",
                    ActivityPayloadDto(merchantName = "ABC Packaging"),
                ),
            ),
        )
        val entries = model.groups.flatMap { it.entries }
        assertEquals("Issue", entries[0].eyebrow)
        assertEquals("High", entries[0].card.statusLabel)
        assertNull(entries[0].card.amountLabel)
        assertEquals("by Priya", entries[0].actorLine)
        assertEquals("Update", entries[1].eyebrow)
        assertEquals("Activity", entries[2].card.title)
        assertNull(entries[2].card.subtitle)
        assertTrue(model.chips.none { it.label == "Vendors" || it.label == "Vendor" })
    }

    @Test
    fun teamMapsUpdateDecisionAndApproval() {
        val model = present(
            BusinessMomentFamilyConfig.Family.TEAM,
            listOf(
                item("BUSINESS_UPDATE", "Standup note", "2026-09-30T05:12:00Z", ActivityPayloadDto(updateId = "u1")),
                item("DECISION_RECORDED", "Launch moved to Friday", "2026-09-30T04:00:00Z", null, actor = "Santosh"),
                item("APPROVAL_APPROVED", "Purchase request", "2026-09-29T03:00:00Z", ActivityPayloadDto(approvalRequestId = "a1"), actor = "Priya"),
            ),
        )
        val entries = model.groups.flatMap { it.entries }
        assertEquals(listOf("Update", "Decision", "Approval"), entries.map { it.eyebrow })
        assertEquals("Launch moved to Friday", entries[1].card.title)
        assertEquals("by Priya", entries[2].actorLine)
        assertTrue(model.chips.none { it.label.contains("Milestone") })
    }

    @Test
    fun filtersUseActivityCodes() {
        val items = listOf(
            item("BUSINESS_REVENUE", "Sale", "2026-09-30T05:00:00Z", ActivityPayloadDto(amount = "1", currencyCode = "INR")),
            item("BUSINESS_EXPENSE", "Rent", "2026-09-30T04:00:00Z", ActivityPayloadDto(amount = "2", currencyCode = "INR")),
            item("ISSUE_REPORTED", "Delay", "2026-09-30T03:00:00Z", null),
            item("BUSINESS_UPDATE", "Note", "2026-09-30T02:00:00Z", null),
            item("DECISION_RECORDED", "Hold", "2026-09-30T01:00:00Z", null),
            item("APPROVAL_REQUESTED", "Buy", "2026-09-29T01:00:00Z", null),
        )
        assertEquals(6, present(BusinessMomentFamilyConfig.Family.MONEY, items).groups.flatMap { it.entries }.size)
        val expenses = present(BusinessMomentFamilyConfig.Family.MONEY, items, BusinessMomentFilter.EXPENSE)
        assertEquals(listOf("Rent"), expenses.groups.flatMap { it.entries }.map { it.card.title })
        val issues = present(BusinessMomentFamilyConfig.Family.DAILY, items, BusinessMomentFilter.ISSUE)
        assertEquals(listOf("Delay"), issues.groups.flatMap { it.entries }.map { it.card.title })
        val decisions = present(BusinessMomentFamilyConfig.Family.TEAM, items, BusinessMomentFilter.DECISION)
        assertEquals(listOf("Hold"), decisions.groups.flatMap { it.entries }.map { it.card.title })
    }

    @Test
    fun keepsServerOrderIncludingEqualTimestamps() {
        val same = "2026-09-30T05:12:00Z"
        val model = present(
            BusinessMomentFamilyConfig.Family.MONEY,
            listOf(
                item("BUSINESS_REVENUE", "First", same, null),
                item("BUSINESS_EXPENSE", "Second", same, null),
            ),
        )
        assertEquals(listOf("First", "Second"), model.groups.single().entries.map { it.card.title })
    }

    @Test
    fun groupsTodayYesterdayAndOlder() {
        val model = present(
            BusinessMomentFamilyConfig.Family.MONEY,
            listOf(
                item("BUSINESS_REVENUE", "Today row", "2026-09-30T05:12:00Z", null),
                item("BUSINESS_EXPENSE", "Yesterday row", "2026-09-29T03:48:00Z", null),
                item("BUSINESS_INVOICE", "Older row", "2026-09-28T04:00:00Z", ActivityPayloadDto(totalAmount = "1")),
            ),
        )
        assertEquals(listOf("TODAY", "YESTERDAY", "28 SEP"), model.groups.map { it.label })
        assertEquals("10:42 AM", model.groups[0].entries.single().timeLabel)
    }

    @Test
    fun emptyFilterAndErrorStayDistinct() {
        val empty = present(BusinessMomentFamilyConfig.Family.MONEY, emptyList())
        assertEquals(BusinessMomentsContent.FEED_EMPTY, empty.content)
        assertEquals("Nothing recorded yet", empty.emptyTitle)
        assertEquals("Revenue, expenses and other money activity will appear here.", empty.emptyBody)

        val dailyEmpty = present(BusinessMomentFamilyConfig.Family.DAILY, emptyList())
        assertTrue(dailyEmpty.emptyBody!!.contains("issues"))

        val filtered = present(
            BusinessMomentFamilyConfig.Family.MONEY,
            listOf(item("BUSINESS_REVENUE", "Sale", "2026-09-30T05:00:00Z", null)),
            BusinessMomentFilter.EXPENSE,
        )
        assertEquals(BusinessMomentsContent.FILTER_EMPTY, filtered.content)
        assertEquals("No expenses yet", filtered.emptyTitle)
        assertNull(filtered.emptyBody)

        val failed = buildBusinessMomentsPresentation(
            family = BusinessMomentFamilyConfig.Family.MONEY,
            items = emptyList(),
            filter = BusinessMomentFilter.ALL,
            loading = false,
            error = "Network",
            now = now,
            zone = zone,
        )
        assertEquals(BusinessMomentsContent.ERROR, failed.content)
        assertEquals("Network", failed.errorMessage)
        assertNull(failed.emptyTitle)
    }

    @Test
    fun switchingClearsThePreviousMomentAndDropsStalePages() {
        val money = item("BUSINESS_REVENUE", "Money row", "2026-09-30T05:00:00Z", null)
        val daily = item("ISSUE_REPORTED", "Daily row", "2026-09-30T05:00:00Z", null)
        var feed = BusinessMomentsFeed()
        feed = feed.begin("money")
        val moneyGen = feed.generation
        feed = feed.applyPage("money", moneyGen, listOf(money), null, append = false)
        assertEquals("Money & Cash Flow", present(BusinessMomentFamilyConfig.Family.MONEY, feed.items).familyTitle)

        feed = feed.begin("daily")
        assertTrue(feed.items.isEmpty())
        assertEquals("Daily Business", present(BusinessMomentFamilyConfig.Family.DAILY, feed.items).familyTitle)
        assertEquals(
            listOf("All", "Spend", "Issues", "Updates", "Approvals"),
            present(BusinessMomentFamilyConfig.Family.DAILY, feed.items).chips.map { it.label },
        )
        feed = feed.applyPage("money", moneyGen, listOf(money), null, append = false)
        assertTrue(feed.items.isEmpty())
        feed = feed.applyPage("daily", feed.generation, listOf(daily), null, append = false)
        assertEquals(listOf("Daily row"), feed.items.map { it.title })

        feed = feed.begin("company-b")
        assertTrue(feed.items.isEmpty())
        feed = feed.applyPage("money", moneyGen, listOf(money), null, append = false)
        assertTrue(feed.items.isEmpty())
    }

    @Test
    fun otherMemberAppearsWhenTheRefreshedFeedIncludesThem() {
        val before = present(BusinessMomentFamilyConfig.Family.MONEY, emptyList())
        assertEquals(BusinessMomentsContent.FEED_EMPTY, before.content)
        val after = present(
            BusinessMomentFamilyConfig.Family.MONEY,
            listOf(
                item(
                    "BUSINESS_EXPENSE",
                    "Member expense",
                    "2026-09-30T05:12:00Z",
                    ActivityPayloadDto(expenseId = "exp-b", amount = "10", currencyCode = "INR"),
                    actor = "Member B",
                ),
            ),
        )
        val entry = after.groups.single().entries.single()
        assertEquals("Member expense", entry.card.title)
        assertEquals("by Member B", entry.actorLine)
    }

    private fun present(
        family: BusinessMomentFamilyConfig.Family,
        items: List<ActivityItemDto>,
        filter: BusinessMomentFilter = BusinessMomentFilter.ALL,
    ) = buildBusinessMomentsPresentation(
        family = family,
        items = items,
        filter = filter,
        loading = false,
        error = null,
        now = now,
        zone = zone,
    )

    private fun item(
        code: String,
        title: String,
        occurredAt: String,
        payload: ActivityPayloadDto?,
        actor: String? = null,
    ) = ActivityItemDto(
        activityCode = code,
        title = title,
        occurredAt = occurredAt,
        activityPayload = payload,
        actorDisplayName = actor,
    )
}

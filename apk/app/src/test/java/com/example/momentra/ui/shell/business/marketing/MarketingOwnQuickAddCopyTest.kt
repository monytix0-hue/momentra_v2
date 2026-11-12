package com.example.momentra.ui.shell.business.marketing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarketingOwnQuickAddCopyTest {

    @Test
    fun activityContextUsesOwnBusinessCampaignPrefix() {
        val line = MarketingOwnQuickAddCopy.context("ACTIVITY", "Harbor & Loom", period = "Oct 2026")
        assertEquals(
            "Harbor & Loom · Own Business Campaign · Oct 2026",
            line,
        )
    }

    @Test
    fun progressContextMatchesFigmaStandaloneLine() {
        assertEquals(
            "Pick an open activity in this Moment",
            MarketingOwnQuickAddCopy.context("PROGRESS", "Harbor & Loom"),
        )
    }

    @Test
    fun paymentContextMatchesFigmaStandaloneLine() {
        assertEquals(
            "Settle an open payable · AP",
            MarketingOwnQuickAddCopy.context("PAYMENT", "Harbor & Loom"),
        )
    }

    @Test
    fun paymentContextInterpolatesApAmountWhenPresent() {
        assertEquals(
            "Settle an open payable · AP ₹40K",
            MarketingOwnQuickAddCopy.context("PAYMENT", "Harbor & Loom", apAmount = "₹40K"),
        )
    }

    @Test
    fun budgetContextAppendsPacingWhenPresent() {
        assertEquals(
            "Harbor & Loom · Own Business Campaign · Oct 2026 · Versioned plan · effective-dated · 64% used",
            MarketingOwnQuickAddCopy.context(
                "BUDGET",
                "Harbor & Loom",
                period = "Oct 2026",
                budgetPacing = "64% used",
            ),
        )
    }

    @Test
    fun budgetContextFallsBackWithoutProjectionFigures() {
        assertEquals(
            "Harbor & Loom · Own Business Campaign · Versioned plan · effective-dated",
            MarketingOwnQuickAddCopy.context("BUDGET", "Harbor & Loom"),
        )
    }

    @Test
    fun inviteWhyMatchesFigmaMastersPanel() {
        assertEquals(
            "Invite grants a role and its access. Joining ≠ approval ≠ payment.",
            MarketingOwnQuickAddCopy.INVITE_WHY,
        )
    }

    @Test
    fun blankBrandFallsBackToCampaign() {
        assertEquals(
            "Campaign · Own Business Campaign · Spend ≠ payment · creates payable",
            MarketingOwnQuickAddCopy.context("SPEND", "  "),
        )
    }

    @Test
    fun catalogModeMapsSharedEventTypes() {
        assertEquals("ACTIVITY", MarketingOwnQuickAddCopy.catalogMode("ACTIVITY", "UPDATE"))
        assertEquals("PROGRESS", MarketingOwnQuickAddCopy.catalogMode("PROGRESS", "RESULT"))
        assertEquals("APPROVAL", MarketingOwnQuickAddCopy.catalogMode("APPROVAL", "UPDATE"))
        assertEquals("BUDGET", MarketingOwnQuickAddCopy.catalogMode("REVISION", "BUDGET"))
        assertEquals("PAYMENT", MarketingOwnQuickAddCopy.catalogMode("PAYMENT", "PAYMENT"))
    }

    @Test
    fun whyBodiesMatchFigmaAppendix() {
        assertEquals(
            "An activity is a plan item — it carries no money. Spend and results are recorded against it later.",
            MarketingOwnQuickAddCopy.why("ACTIVITY"),
        )
        assertEquals(
            "Spend ≠ payment. Recording spend creates a payable; cash leaves only when you record a Payment.",
            MarketingOwnQuickAddCopy.why("SPEND"),
        )
        assertEquals(
            "Approval ≠ disbursement — paying is a separate Payment. You cannot approve your own controlled action.",
            MarketingOwnQuickAddCopy.why("APPROVAL"),
        )
        assertEquals(
            "Empty ≠ zero: leave blank when not measured. ROAS is not profit; attributed sales ≠ booked revenue.",
            MarketingOwnQuickAddCopy.why("RESULT"),
        )
    }

    @Test
    fun ctasMatchFigmaAppendix() {
        assertEquals("Save activity", MarketingOwnQuickAddCopy.cta("ACTIVITY"))
        assertEquals("Save progress", MarketingOwnQuickAddCopy.cta("PROGRESS"))
        assertEquals("Save spend", MarketingOwnQuickAddCopy.cta("SPEND"))
        assertEquals("Save payment", MarketingOwnQuickAddCopy.cta("PAYMENT"))
        assertEquals("Save revision v2", MarketingOwnQuickAddCopy.cta("BUDGET", budgetVersion = 2))
        assertEquals("Save decision", MarketingOwnQuickAddCopy.cta("APPROVAL"))
        assertEquals("Post update", MarketingOwnQuickAddCopy.cta("UPDATE"))
        assertEquals("Save result", MarketingOwnQuickAddCopy.cta("RESULT"))
    }

    @Test
    fun holdTransferWhyAreMarketingSpecific() {
        assertTrue(MarketingOwnQuickAddCopy.why("HOLD").contains("Hold pauses a campaign"))
        assertTrue(MarketingOwnQuickAddCopy.why("TRANSFER").contains("planned budget between channels"))
    }

    @Test
    fun moreDetailsAndStickyMetaMatchFigma() {
        assertEquals("More details", MarketingOwnQuickAddCopy.MORE_DETAILS_LABEL)
        assertEquals("Notes · Attachment · Tags", MarketingOwnQuickAddCopy.MORE_DETAILS_HINT)
        assertEquals(
            "Draft preserved if you close or open a picker",
            MarketingOwnQuickAddCopy.STICKY_META,
        )
    }

    @Test
    fun titlesMatchOwnFigma() {
        assertEquals("Add Activity", MarketingOwnQuickAddCopy.title("ACTIVITY"))
        assertEquals("Record Spend", MarketingOwnQuickAddCopy.title("SPEND"))
        assertEquals("Budget / Target Revision", MarketingOwnQuickAddCopy.title("BUDGET"))
        assertEquals("Campaign Result", MarketingOwnQuickAddCopy.title("RESULT"))
    }
}

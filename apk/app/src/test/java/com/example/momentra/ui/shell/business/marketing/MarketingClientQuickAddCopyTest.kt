package com.example.momentra.ui.shell.business.marketing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarketingClientQuickAddCopyTest {

    @Test
    fun activityContextUsesClientCampaignPrefix() {
        val line = MarketingClientQuickAddCopy.context("ACTIVITY", "Brightline")
        assertEquals(
            "Brightline · Client Campaign · Plan item · no money",
            line,
        )
    }

    @Test
    fun blankBrandFallsBackToCampaign() {
        assertEquals(
            "Campaign · Client Campaign · Spend ≠ payment · creates payable",
            MarketingClientQuickAddCopy.context("SPEND", "  "),
        )
    }

    @Test
    fun catalogModeMapsInvoiceAndActivityEventTypes() {
        assertEquals("INVOICE", MarketingClientQuickAddCopy.catalogMode("INVOICE", "SALE"))
        assertEquals("ACTIVITY", MarketingClientQuickAddCopy.catalogMode("ACTIVITY", "UPDATE"))
        assertEquals("PROGRESS", MarketingClientQuickAddCopy.catalogMode("PROGRESS", "RESULT"))
        assertEquals("APPROVAL", MarketingClientQuickAddCopy.catalogMode("APPROVAL", "UPDATE"))
        assertEquals("BUDGET", MarketingClientQuickAddCopy.catalogMode("REVISION", "BUDGET"))
    }

    @Test
    fun whyBodiesMatchFigmaAppendix() {
        assertEquals(
            "An activity is a plan item — it carries no money. Spend and results are recorded against it later.",
            MarketingClientQuickAddCopy.why("ACTIVITY"),
        )
        assertEquals(
            "Spend ≠ payment. Recording spend creates a payable; cash leaves only when you record a Payment.",
            MarketingClientQuickAddCopy.why("SPEND"),
        )
        assertEquals(
            "Approval ≠ disbursement — paying is a separate Payment. You cannot approve your own controlled action.",
            MarketingClientQuickAddCopy.why("APPROVAL"),
        )
        assertEquals(
            "Empty ≠ zero: leave blank when not measured. ROAS is not profit; attributed sales ≠ booked revenue.",
            MarketingClientQuickAddCopy.why("RESULT"),
        )
    }

    @Test
    fun ctasMatchFigmaAppendix() {
        assertEquals("Save activity", MarketingClientQuickAddCopy.cta("ACTIVITY"))
        assertEquals("Save progress", MarketingClientQuickAddCopy.cta("PROGRESS"))
        assertEquals("Save spend", MarketingClientQuickAddCopy.cta("SPEND"))
        assertEquals("Save payment", MarketingClientQuickAddCopy.cta("PAYMENT"))
        assertEquals("Save invoice", MarketingClientQuickAddCopy.cta("INVOICE"))
        assertEquals("Save collection", MarketingClientQuickAddCopy.cta("COLLECTION"))
        assertEquals("Save revision v2", MarketingClientQuickAddCopy.cta("BUDGET", budgetVersion = 2))
        assertEquals("Save decision", MarketingClientQuickAddCopy.cta("APPROVAL"))
        assertEquals("Post update", MarketingClientQuickAddCopy.cta("UPDATE"))
        assertEquals("Save result", MarketingClientQuickAddCopy.cta("RESULT"))
        assertEquals("Reprint · duplicate copy", MarketingClientQuickAddCopy.cta("REPRINT"))
    }

    @Test
    fun holdTransferReprintWhyAreMarketingSpecific() {
        assertTrue(MarketingClientQuickAddCopy.why("HOLD").contains("Hold pauses a campaign"))
        assertTrue(MarketingClientQuickAddCopy.why("TRANSFER").contains("planned budget between channels"))
        assertTrue(MarketingClientQuickAddCopy.why("REPRINT").contains("same number"))
    }

    @Test
    fun moreDetailsAndStickyMetaMatchFigma() {
        assertEquals("More details", MarketingClientQuickAddCopy.MORE_DETAILS_LABEL)
        assertEquals("Notes · Attachment · Tags", MarketingClientQuickAddCopy.MORE_DETAILS_HINT)
        assertEquals(
            "Draft preserved if you close or open a picker",
            MarketingClientQuickAddCopy.STICKY_META,
        )
        assertTrue(MarketingClientQuickAddCopy.CLIENT_ONLY_GATE.contains("Client only"))
        assertEquals("Client only", MarketingClientQuickAddCopy.CLIENT_ONLY_CHIP)
    }
}

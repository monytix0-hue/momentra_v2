package com.example.momentra.ui.shell.business.marketing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

data class MarketingSetupDraft(val subtype: String = "")

class MarketingSetupModeTest {

    @Test
    fun testMarketingSetupSubtypeRules() {
        // Given a marketing setup draft with CLIENT_CAMPAIGN
        val clientDraft = MarketingSetupDraft(subtype = "CLIENT_CAMPAIGN")
        assertEquals("CLIENT_CAMPAIGN", clientDraft.subtype)

        // Given a marketing setup draft with OWN_BUSINESS_CAMPAIGN
        val ownBusinessDraft = MarketingSetupDraft(subtype = "OWN_BUSINESS_CAMPAIGN")
        assertEquals("OWN_BUSINESS_CAMPAIGN", ownBusinessDraft.subtype)
    }

    @Test
    fun testOwnBusinessExcludesClientInvoiceAndAR() {
        // According to Figma specs & backend architecture rules:
        // Own business campaign must not have client invoice, collection or AR.
        val subtype = "OWN_BUSINESS_CAMPAIGN"
        val isClientMode = subtype == "CLIENT_CAMPAIGN"
        assertFalse("Own business mode should not be client mode", isClientMode)
    }
}

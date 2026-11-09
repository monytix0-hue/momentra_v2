package com.example.momentra.ui.shell.business.marketing

import com.example.momentra.ui.shell.empty.business.MarketingSetupDocState
import com.example.momentra.ui.shell.empty.business.MarketingSetupDocumentSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MarketingSetupModeTest {

    @Test
    fun tileFlipSwitchesSubtypeAndClearsClientFeeOnOwn() {
        val state = MarketingSetupDocState(MarketingSetupDocumentSpec.CLIENT)
        state.clientFee = "15000"
        state.partyName = "Brightline"
        state.campaignName = "Q2"
        state.budget = "200000"
        state.channels = setOf("Paid Social")

        state.switchSubtype(MarketingSetupDocumentSpec.OWN)

        assertEquals(MarketingSetupDocumentSpec.OWN, state.subtype)
        assertEquals("", state.clientFee)
        assertTrue(MarketingSetupDocumentSpec.isOwn(state.subtype))
        assertFalse(MarketingSetupDocumentSpec.isClient(state.subtype))
    }

    @Test
    fun preferencesEmitClientFeeOnlyForClient() {
        val client = MarketingSetupDocumentSpec.preferences(
            subtypeCode = MarketingSetupDocumentSpec.CLIENT,
            identityName = "Brightline Q2",
            partyName = "Brightline",
            startDate = "2026-11-01",
            monthlyBudget = "200000",
            clientFee = "15000",
            channels = listOf("Paid Social"),
        )
        assertEquals("15000", client["clientFee"])

        val own = MarketingSetupDocumentSpec.preferences(
            subtypeCode = MarketingSetupDocumentSpec.OWN,
            identityName = "Spring launch",
            partyName = "Northstar",
            startDate = "2026-11-01",
            monthlyBudget = "50000",
            clientFee = "15000",
            channels = listOf("Organic Social"),
        )
        assertEquals("", own["clientFee"])
        assertEquals(MarketingSetupDocumentSpec.OWN, own["subtype"])
    }

    @Test
    fun ownBusinessExcludesClientInvoiceAndArQuickAdds() {
        val own = MarketingSetupDocumentSpec.subtype(MarketingSetupDocumentSpec.OWN)
        assertFalse(own.quickAdds.contains("INVOICE"))
        assertFalse(own.quickAdds.contains("COLLECTION"))

        val client = MarketingSetupDocumentSpec.subtype(MarketingSetupDocumentSpec.CLIENT)
        assertTrue(client.quickAdds.contains("INVOICE"))
        assertTrue(client.quickAdds.contains("COLLECTION"))
    }
}

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

    @Test
    fun preferencesEmitEconomicsBucketsForClientOnly() {
        val client = MarketingSetupDocumentSpec.preferences(
            subtypeCode = MarketingSetupDocumentSpec.CLIENT,
            identityName = "Brightline Q2",
            partyName = "Brightline",
            startDate = "2026-11-01",
            monthlyBudget = "200000",
            clientFee = "15000",
            clientApprovedMediaBudget = "500000",
            agencyOperatingBudget = "80000",
            campaignPurpose = "Leads",
            creativeApprovalRequired = "YES",
            reportingCadence = "MONTHLY",
            channels = listOf("Paid Social"),
        )
        assertEquals("15000", client["clientFee"])
        assertEquals("500000", client["clientApprovedMediaBudget"])
        assertEquals("80000", client["agencyOperatingBudget"])
        assertEquals("Leads", client["campaignPurpose"])
        assertEquals("YES", client["creativeApprovalRequired"])
        assertEquals("MONTHLY", client["reportingCadence"])

        val own = MarketingSetupDocumentSpec.preferences(
            subtypeCode = MarketingSetupDocumentSpec.OWN,
            identityName = "Spring launch",
            partyName = "Northstar",
            startDate = "2026-11-01",
            monthlyBudget = "50000",
            clientFee = "15000",
            clientApprovedMediaBudget = "500000",
            agencyOperatingBudget = "80000",
            reportingCadence = "WEEKLY",
            channels = listOf("Organic Social"),
        )
        assertEquals("", own["clientFee"])
        assertEquals("", own["clientApprovedMediaBudget"])
        assertEquals("", own["agencyOperatingBudget"])
        assertEquals("", own["reportingCadence"])
    }

    @Test
    fun switchSubtypeClearsClientEconomics() {
        val state = MarketingSetupDocState(MarketingSetupDocumentSpec.CLIENT)
        state.clientFee = "15000"
        state.clientApprovedMediaBudget = "500000"
        state.agencyOperatingBudget = "80000"
        state.reportingCadence = "MONTHLY"
        state.switchSubtype(MarketingSetupDocumentSpec.OWN)
        assertEquals("", state.clientFee)
        assertEquals("", state.clientApprovedMediaBudget)
        assertEquals("", state.agencyOperatingBudget)
        assertEquals("", state.reportingCadence)
    }

    @Test
    fun quickAddSurfaceIsTenClientEightOwn() {
        val client = MarketingSetupDocumentSpec.subtype(MarketingSetupDocumentSpec.CLIENT).quickAdds
        val own = MarketingSetupDocumentSpec.subtype(MarketingSetupDocumentSpec.OWN).quickAdds
        assertEquals(10, client.size)
        assertEquals(8, own.size)
        assertTrue(client.contains("UPDATE"))
        assertFalse(client.contains("ISSUE"))
        assertFalse(own.contains("ISSUE"))
        assertFalse(own.contains("INVOICE"))
        assertFalse(own.contains("COLLECTION"))
    }
}

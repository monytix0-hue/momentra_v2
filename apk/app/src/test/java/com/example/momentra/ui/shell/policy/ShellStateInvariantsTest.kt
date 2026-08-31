package com.example.momentra.ui.shell.policy

import com.example.momentra.domain.AppContext
import com.example.momentra.domain.BottomDestination
import com.example.momentra.domain.CompanySummary
import com.example.momentra.domain.MomentSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShellStateInvariantsTest {
    @Test
    fun healsUnsupportedContext() {
        val result = ShellStateInvariants.heal(
            ShellInvariantInput(
                supportedContexts = listOf(AppContext.PERSONAL, AppContext.GROUP),
                selectedContext = AppContext.BUSINESS,
                selectedCompanyId = "c1",
                companies = emptyList(),
                moments = emptyList(),
                selectedMomentId = null,
                selectedTabByContext = emptyMap(),
            ),
        )
        assertEquals(AppContext.PERSONAL, result.selectedContext)
        assertNull(result.selectedCompanyId)
        assertTrue(result.healed)
    }

    @Test
    fun businessScopesMomentsToCompany() {
        val result = ShellStateInvariants.heal(
            ShellInvariantInput(
                supportedContexts = listOf(AppContext.PERSONAL, AppContext.BUSINESS),
                selectedContext = AppContext.BUSINESS,
                selectedCompanyId = "c1",
                companies = listOf(CompanySummary("c1", "A"), CompanySummary("c2", "B")),
                moments = listOf(
                    MomentSummary("m1", "A1", "ACTIVE", companyId = "c1"),
                    MomentSummary("m2", "B1", "ACTIVE", companyId = "c2"),
                ),
                selectedMomentId = "m2",
                selectedTabByContext = mapOf(AppContext.BUSINESS to BottomDestination.MEMORY),
            ),
        )
        assertEquals("c1", result.selectedCompanyId)
        assertEquals(listOf("m1"), result.moments.map { it.momentId })
        assertEquals("m1", result.selectedMomentId)
        assertEquals(BottomDestination.MEMORY, result.selectedTabByContext[AppContext.BUSINESS])
    }

    @Test
    fun clearsCompanyOutsideBusiness() {
        val result = ShellStateInvariants.heal(
            ShellInvariantInput(
                supportedContexts = listOf(AppContext.PERSONAL, AppContext.BUSINESS),
                selectedContext = AppContext.PERSONAL,
                selectedCompanyId = "c1",
                companies = listOf(CompanySummary("c1", "A")),
                moments = listOf(MomentSummary("p1", "Life", "ACTIVE")),
                selectedMomentId = "p1",
                selectedTabByContext = emptyMap(),
            ),
        )
        assertNull(result.selectedCompanyId)
        assertEquals("p1", result.selectedMomentId)
    }
}

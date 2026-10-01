package com.example.momentra.ui.shell.business.shared

import com.example.momentra.domain.MomentSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BusinessActionRegistryTest {

    @Test
    fun mapsCapabilityCodesToDestinations() {
        assertEquals(
            BusinessActionRegistry.Destination.SPEND,
            BusinessActionRegistry.destinationFor(BusinessActionRegistry.EXPENSE_CREATE),
        )
        assertEquals(
            BusinessActionRegistry.Destination.VENDOR,
            BusinessActionRegistry.destinationFor(BusinessActionRegistry.VENDOR_MANAGE),
        )
        assertEquals(
            BusinessActionRegistry.Destination.ISSUE,
            BusinessActionRegistry.destinationFor(BusinessActionRegistry.ISSUE_CREATE),
        )
        assertEquals(
            BusinessActionRegistry.Destination.SLA,
            BusinessActionRegistry.destinationFor(BusinessActionRegistry.SLA_MANAGE),
        )
        assertEquals(
            BusinessActionRegistry.Destination.REVENUE,
            BusinessActionRegistry.destinationFor(BusinessActionRegistry.REVENUE_RECORD),
        )
        assertEquals(
            BusinessActionRegistry.Destination.INVOICE,
            BusinessActionRegistry.destinationFor(BusinessActionRegistry.INVOICE_CREATE),
        )
        assertNull(BusinessActionRegistry.destinationFor("UNKNOWN_CODE"))
        assertEquals(
            BusinessActionRegistry.Destination.MEMBERS,
            BusinessActionRegistry.destinationFor(BusinessActionRegistry.MEMBER_MANAGE),
        )
    }

    @Test
    fun emptyCapabilitiesFailOpenWithDefaults() {
        assertTrue(
            BusinessActionRegistry.isDestinationEnabled(emptyList(), BusinessActionRegistry.Destination.SPEND),
        )
        assertTrue(
            BusinessActionRegistry.isDestinationEnabled(emptyList(), BusinessActionRegistry.Destination.REVENUE),
        )
        assertTrue(
            BusinessActionRegistry.isDestinationEnabled(emptyList(), BusinessActionRegistry.Destination.INVOICE),
        )
        assertTrue(
            BusinessActionRegistry.enabledDestinations(emptyList()).containsAll(
                listOf(
                    BusinessActionRegistry.Destination.SPEND,
                    BusinessActionRegistry.Destination.REVENUE,
                    BusinessActionRegistry.Destination.INVOICE,
                ),
            ),
        )
        assertTrue(BusinessQuickAddKind.REVENUE.isCapabilityEnabled(emptyList(), "BUSINESS_RUNWAY"))
        assertFalse(BusinessQuickAddKind.REVENUE.isCapabilityEnabled(emptyList(), "TEAM_OPERATIONS"))
    }

    @Test
    fun nonEmptyCapabilitiesFilterDestinations() {
        val caps = listOf(BusinessActionRegistry.EXPENSE_CREATE)
        assertTrue(
            BusinessActionRegistry.isDestinationEnabled(caps, BusinessActionRegistry.Destination.SPEND),
        )
        assertFalse(
            BusinessActionRegistry.isDestinationEnabled(caps, BusinessActionRegistry.Destination.REVENUE),
        )
    }

    @Test
    fun runwayFinanceOnlyOnRunwayMomentType() {
        assertTrue(BusinessActionRegistry.isRunwayFinanceEnabled("BUSINESS_RUNWAY"))
        assertFalse(BusinessActionRegistry.isRunwayFinanceEnabled("TEAM_OPERATIONS"))
        assertFalse(BusinessActionRegistry.isRunwayFinanceEnabled("BUSINESS_OPERATIONS"))
    }

    @Test
    fun revenueInvoiceCapabilityRequiresRunwayMomentType() {
        val caps = listOf(BusinessActionRegistry.REVENUE_RECORD, BusinessActionRegistry.INVOICE_CREATE)
        assertFalse(BusinessQuickAddKind.REVENUE.isCapabilityEnabled(caps, "TEAM_OPERATIONS"))
        assertFalse(BusinessQuickAddKind.INVOICE.isCapabilityEnabled(caps, "BUSINESS_OPERATIONS"))
        assertTrue(BusinessQuickAddKind.REVENUE.isCapabilityEnabled(caps, "BUSINESS_RUNWAY"))
        assertTrue(BusinessQuickAddKind.INVOICE.isCapabilityEnabled(caps, "BUSINESS_RUNWAY"))
    }

    @Test
    fun growingMoneyPrimaryIsRevenueExpenseInvoice() {
        val spec = BusinessMomentFamilyConfig.forFamily(BusinessMomentFamilyConfig.Family.MONEY, smallShop = false)
        assertEquals(
            listOf(BusinessQuickAddKind.REVENUE, BusinessQuickAddKind.EXPENSE, BusinessQuickAddKind.INVOICE),
            spec.primary,
        )
    }

    @Test
    fun shopMoneyPrimaryIsKhataRevenueExpense() {
        val spec = BusinessMomentFamilyConfig.forFamily(BusinessMomentFamilyConfig.Family.MONEY, smallShop = true)
        assertEquals(
            listOf(BusinessQuickAddKind.KHATA, BusinessQuickAddKind.REVENUE, BusinessQuickAddKind.EXPENSE),
            spec.primary,
        )
        assertEquals("Shop expense", BusinessQuickAddKind.EXPENSE.label(smallShop = true))
    }

    @Test
    fun dailyPrimaryIsSpendVendorIssue() {
        val growing = BusinessMomentFamilyConfig.forFamily(BusinessMomentFamilyConfig.Family.DAILY, smallShop = false)
        val shop = BusinessMomentFamilyConfig.forFamily(BusinessMomentFamilyConfig.Family.DAILY, smallShop = true)
        val expected = listOf(
            BusinessQuickAddKind.SPEND_ENTRY,
            BusinessQuickAddKind.UPDATE_VENDOR,
            BusinessQuickAddKind.REPORT_ISSUE,
        )
        assertEquals(expected, growing.primary)
        assertEquals(expected, shop.primary)
        assertFalse(shop.secondary.contains(BusinessQuickAddKind.SLA_CHECK))
    }

    @Test
    fun teamPrimaryAndShopOmitsDecision() {
        val growing = BusinessMomentFamilyConfig.forFamily(BusinessMomentFamilyConfig.Family.TEAM, smallShop = false)
        assertEquals(
            listOf(BusinessQuickAddKind.TEAM_UPDATE, BusinessQuickAddKind.APPROVAL, BusinessQuickAddKind.DECISION),
            growing.primary,
        )
        val shop = BusinessMomentFamilyConfig.forFamily(BusinessMomentFamilyConfig.Family.TEAM, smallShop = true)
        assertFalse(shop.primary.contains(BusinessQuickAddKind.DECISION))
        assertFalse(shop.secondary.contains(BusinessQuickAddKind.DECISION))
    }

    @Test
    fun capabilityFilterDoesNotBackfillPrimary() {
        val spec = BusinessMomentFamilyConfig.forFamily(BusinessMomentFamilyConfig.Family.MONEY, smallShop = false)
        val visible = BusinessMomentFamilyConfig.visibleActions(
            spec.primary,
            listOf(BusinessActionRegistry.EXPENSE_CREATE, BusinessActionRegistry.REVENUE_RECORD),
            "BUSINESS_RUNWAY",
        )
        assertEquals(listOf(BusinessQuickAddKind.REVENUE, BusinessQuickAddKind.EXPENSE), visible)
        assertFalse(visible.contains(BusinessQuickAddKind.TAX_ENTRY))
    }

    @Test
    fun eachFamilyQuickAddPrimaryDiffers() {
        assertEquals(
            listOf(BusinessQuickAddKind.REVENUE, BusinessQuickAddKind.EXPENSE, BusinessQuickAddKind.INVOICE),
            BusinessMomentFamilyConfig.quickAddPrimary("BUSINESS_RUNWAY", false, emptyList()),
        )
        assertEquals(
            listOf(BusinessQuickAddKind.KHATA, BusinessQuickAddKind.REVENUE, BusinessQuickAddKind.EXPENSE),
            BusinessMomentFamilyConfig.quickAddPrimary("BUSINESS_RUNWAY", true, emptyList()),
        )
        assertEquals(
            listOf(
                BusinessQuickAddKind.SPEND_ENTRY,
                BusinessQuickAddKind.UPDATE_VENDOR,
                BusinessQuickAddKind.REPORT_ISSUE,
            ),
            BusinessMomentFamilyConfig.quickAddPrimary("BUSINESS_OPERATIONS", false, emptyList()),
        )
        assertEquals(
            listOf(BusinessQuickAddKind.TEAM_UPDATE, BusinessQuickAddKind.APPROVAL, BusinessQuickAddKind.DECISION),
            BusinessMomentFamilyConfig.quickAddPrimary("TEAM_OPERATIONS", false, emptyList()),
        )
        assertEquals(
            listOf(BusinessQuickAddKind.KHATA, BusinessQuickAddKind.TEAM_UPDATE, BusinessQuickAddKind.APPROVAL),
            BusinessMomentFamilyConfig.quickAddPrimary("TEAM_OPERATIONS", true, emptyList()),
        )
        assertFalse(
            BusinessMomentFamilyConfig.quickAddPrimary("BUSINESS_OPERATIONS", false, emptyList())
                .contains(BusinessQuickAddKind.REVENUE),
        )
        assertTrue(BusinessMomentFamilyConfig.quickAddPrimary(null, false, emptyList()).isEmpty())
    }

    @Test
    fun switchingMomentsChangesQuickAddWithoutRestart() {
        val moments = listOf(
            MomentSummary("money", "Money & Cash Flow", "ACTIVE", "BUSINESS_RUNWAY"),
            MomentSummary("daily", "Daily Business", "ACTIVE", "BUSINESS_OPERATIONS"),
            MomentSummary("team", "Team & Work", "ACTIVE", "TEAM_OPERATIONS"),
            MomentSummary("blank", "Untitled", "ACTIVE", null),
        )
        fun primary(id: String) = BusinessMomentFamilyConfig.quickAddPrimary(
            BusinessMomentFamilyConfig.selectedQuickAddTypeCode(id, moments, "TEAM_OPERATIONS"),
            smallShop = false,
            capabilities = emptyList(),
        )
        val money = primary("money")
        val daily = primary("daily")
        val team = primary("team")
        assertEquals(
            listOf(BusinessQuickAddKind.REVENUE, BusinessQuickAddKind.EXPENSE, BusinessQuickAddKind.INVOICE),
            money,
        )
        assertEquals(
            listOf(
                BusinessQuickAddKind.SPEND_ENTRY,
                BusinessQuickAddKind.UPDATE_VENDOR,
                BusinessQuickAddKind.REPORT_ISSUE,
            ),
            daily,
        )
        assertEquals(
            listOf(BusinessQuickAddKind.TEAM_UPDATE, BusinessQuickAddKind.APPROVAL, BusinessQuickAddKind.DECISION),
            team,
        )
        assertEquals(money, primary("money"))
        assertNull(BusinessMomentFamilyConfig.selectedQuickAddTypeCode("blank", moments, "TEAM_OPERATIONS"))
        assertTrue(primary("blank").isEmpty())
    }
}

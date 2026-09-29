package com.example.momentra.ui.shell.business.shared

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
}

package com.example.momentra.ui.shell.business.setup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OperatingSetupPayloadBuilderTest {

    @Test
    fun activateMissingChannelsSetsFieldError() {
        val result = OperatingSetupPayloadBuilder.build(
            preferences = mapOf(
                "serviceName" to "BrightCare",
                "companyName" to "BrightCare",
            ),
            momentTypeCode = "CONSUMER_SERVICE",
            title = "BrightCare",
            activating = true,
        )
        assertEquals("Select at least one channel.", result.fieldErrors["channels"])
        assertEquals("Select at least one channel.", result.validationError)
    }

    @Test
    fun draftNeverReturnsFieldErrors() {
        val result = OperatingSetupPayloadBuilder.build(
            preferences = emptyMap(),
            momentTypeCode = "CONSUMER_SERVICE",
            title = "BrightCare",
            activating = false,
        )
        assertNull(result.validationError)
        assertTrue(result.fieldErrors.isEmpty())
    }

    @Test
    fun monthlyChargeTargetAliasesToMonthlyTarget() {
        val result = OperatingSetupPayloadBuilder.build(
            preferences = mapOf(
                "serviceName" to "BrightCare",
                "companyName" to "BrightCare",
                "channels" to "Studio",
                "monthlyChargeTarget" to "₹6,50,000",
            ),
            momentTypeCode = "CONSUMER_SERVICE",
            title = "BrightCare",
            activating = false,
        )
        assertEquals("650000.0000", result.preferences["monthlyTarget"])
    }

    @Test
    fun invalidGstinHighlightsFieldOnActivate() {
        val result = OperatingSetupPayloadBuilder.build(
            preferences = mapOf(
                "serviceName" to "BrightCare",
                "companyName" to "BrightCare",
                "channels" to "Studio",
                "gstTracking" to "Yes",
                "gstin" to "BAD",
            ),
            momentTypeCode = "CONSUMER_SERVICE",
            title = "BrightCare",
            activating = true,
        )
        assertEquals("Enter a valid 15-digit GSTIN.", result.fieldErrors["gstin"])
    }

    @Test
    fun emptyServiceNameHighlightsServiceName() {
        val result = OperatingSetupPayloadBuilder.build(
            preferences = mapOf("channels" to "Studio"),
            momentTypeCode = "CONSUMER_SERVICE",
            title = "",
            activating = true,
        )
        assertEquals("Moment name is required.", result.fieldErrors["serviceName"])
    }

    @Test
    fun retailAliasesCompanyNameAndIgnoresFormat() {
        val result = OperatingSetupPayloadBuilder.build(
            preferences = mapOf(
                "storeName" to "Harbor Mart",
                "companyName" to "Harbor Mart Pvt",
                "format" to "Kirana",
                "locality" to "Andheri West",
                "channels" to "Counter|Online",
                "planMonth" to "2026-10",
                "salePriceIncludesGst" to "GST is added separately",
                "monthlyBudget" to "₹8,40,000",
                "monthlyTarget" to "₹12,00,000",
            ),
            momentTypeCode = "RETAIL_STORE",
            title = "Harbor Mart",
            activating = false,
        )
        assertEquals("Harbor Mart", result.preferences["identityName"])
        assertEquals("Harbor Mart Pvt", result.preferences["partyName"])
        assertEquals("Andheri West", result.preferences["location"])
        assertEquals("2026-10-01", result.preferences["startDate"])
        assertEquals("NO", result.preferences["salePriceIncludesGst"])
        @Suppress("UNCHECKED_CAST")
        assertEquals(listOf("Counter", "Online"), result.preferences["channels"] as List<*>)
        val status = OperatingSetupPayloadBuilder.planStatusMessage(result.preferences)
        assertTrue(status.first)
        assertEquals("Budget and sales target look good", status.second)
    }

    @Test
    fun retailActivateMissingChannelsHighlightsChannels() {
        val result = OperatingSetupPayloadBuilder.build(
            preferences = mapOf(
                "storeName" to "Harbor Mart",
                "companyName" to "Harbor Mart",
            ),
            momentTypeCode = "RETAIL_STORE",
            title = "Harbor Mart",
            activating = true,
        )
        assertEquals("Select at least one channel.", result.fieldErrors["channels"])
    }

    @Test
    fun tradingFigmaAliasesRoundTrip() {
        val result = OperatingSetupPayloadBuilder.build(
            preferences = mapOf(
                "businessName" to "Northstar Trading",
                "companyName" to "Northstar Trading",
                "channels" to "Retail buyers|Wholesale",
                "monthlyBudget" to "₹60,00,000",
                "monthlySalesTarget" to "₹90,00,000",
                "roles" to "Sales exec|Sales rep",
            ),
            momentTypeCode = "TRADING_WHOLESALE",
            title = "Fallback",
            activating = false,
        )
        assertEquals("Northstar Trading", result.preferences["identityName"])
        assertEquals("Northstar Trading", result.preferences["partyName"])
        assertEquals("9000000.0000", result.preferences["monthlyTarget"])
        assertEquals("OPERATIONS_LEAD", OperatingSetupPayloadBuilder.mapRole("Sales exec"))
        assertEquals("TEAM_MEMBER", OperatingSetupPayloadBuilder.mapRole("Sales rep"))
    }

    @Test
    fun tradingActivateMissingBudgetSetsFieldError() {
        val result = OperatingSetupPayloadBuilder.build(
            preferences = mapOf(
                "businessName" to "Northstar Trading",
                "companyName" to "Northstar Trading",
            ),
            momentTypeCode = "TRADING_WHOLESALE",
            title = "Northstar Trading",
            activating = true,
        )
        assertEquals("Monthly budget must be a positive amount.", result.fieldErrors["monthlyBudget"])
        assertEquals("Monthly target must be a positive amount.", result.fieldErrors["monthlyTarget"])
    }

    @Test
    fun restaurantFigmaAliasesRoundTrip() {
        val result = OperatingSetupPayloadBuilder.build(
            preferences = mapOf(
                "restaurantName" to "Spice Route Kitchen",
                "companyName" to "Spice Route Kitchen",
                "channels" to "Dine-in|Swiggy",
                "monthlyBudget" to "₹5,20,000",
                "monthlySalesTarget" to "₹6,50,000",
                "roles" to "Cashier|Waiter",
            ),
            momentTypeCode = "RESTAURANT_FOOD",
            title = "Fallback",
            activating = false,
        )
        assertEquals("Spice Route Kitchen", result.preferences["outletName"])
        assertEquals("Spice Route Kitchen", result.preferences["identityName"])
        assertEquals("650000.0000", result.preferences["monthlyTarget"])
        assertEquals("TEAM_MEMBER", OperatingSetupPayloadBuilder.mapRole("Waiter"))
    }
}

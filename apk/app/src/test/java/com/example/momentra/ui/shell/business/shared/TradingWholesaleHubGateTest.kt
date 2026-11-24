package com.example.momentra.ui.shell.business.shared

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TradingWholesaleHubGateTest {

    @Test
    fun exactWholesaleSubtypeRoutesToV12() {
        assertTrue(isTradingWholesaleHub("TRADING_WHOLESALE", null))
        assertTrue(isTradingWholesaleHub("trading_wholesale", "TRADING_DISTRIBUTION"))
    }

    @Test
    fun tradingFamilyWithoutDealershipRoutesToV12() {
        assertTrue(isTradingWholesaleHub(null, "TRADING_DISTRIBUTION"))
        assertTrue(isTradingWholesaleHub("SOME_OTHER", "trading_distribution"))
    }

    @Test
    fun dealershipNeverRoutesToTradingV12() {
        assertFalse(isTradingWholesaleHub("DISTRIBUTION_DEALERSHIP", "TRADING_DISTRIBUTION"))
        assertFalse(isTradingWholesaleHub("DISTRIBUTION_DEALERSHIP", null))
    }

    @Test
    fun otherFamiliesStayOnGenericWidgets() {
        assertFalse(isTradingWholesaleHub("RETAIL_STORE", "RETAIL"))
        assertFalse(isTradingWholesaleHub(null, null))
    }
}

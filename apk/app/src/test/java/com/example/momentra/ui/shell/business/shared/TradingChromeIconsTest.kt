package com.example.momentra.ui.shell.business.shared

import com.example.momentra.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TradingChromeIconsTest {

    @Test
    fun pulseActionsMapToRestTileDrawables() {
        assertEquals(R.drawable.ic_rest_tile_sale, TradingChromeIcons.pulseActionDrawable("SALE"))
        assertEquals(R.drawable.ic_rest_tile_purchase, TradingChromeIcons.pulseActionDrawable("purchase"))
        assertEquals(R.drawable.ic_rest_tile_collection, TradingChromeIcons.pulseActionDrawable("COLLECTION"))
        assertEquals(R.drawable.ic_rest_tile_payment, TradingChromeIcons.pulseActionDrawable("PAYMENT"))
        assertEquals(4, TradingChromeIcons.pulseActionCodes.size)
    }

    @Test
    fun tradingHubCoreAndSupportHaveDrawableEntries() {
        TradingChromeIcons.tradingHubCoreIds.forEach { id ->
            assertTrue(TradingChromeIcons.hubDrawable(id) != 0)
        }
        TradingChromeIcons.tradingHubSupportIds.forEach { id ->
            assertTrue(TradingChromeIcons.hubDrawable(id) != 0)
        }
    }

    @Test(expected = IllegalStateException::class)
    fun unknownPulseCodeFailsLoudly() {
        TradingChromeIcons.pulseActionDrawable("UNKNOWN")
    }

    @Test(expected = IllegalStateException::class)
    fun unknownHubTileFailsLoudly() {
        TradingChromeIcons.hubDrawable("NOT_A_TILE")
    }
}

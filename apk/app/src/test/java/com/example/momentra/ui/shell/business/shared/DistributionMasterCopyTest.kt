package com.example.momentra.ui.shell.business.shared

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DistributionMasterCopyTest {
    @Test
    fun principalsTitleMatchesFigma() {
        assertEquals("Principals & dealers", DistributionMasterCopy.tabTitle(OperatingMasterTab.Principals))
    }

    @Test
    fun skuBrowserCopyMatchesFigma() {
        assertEquals("Catalogue", DistributionMasterCopy.SKU_BROWSER_TITLE)
        assertEquals("Search SKU, name or barcode", DistributionMasterCopy.SKU_BROWSER_SEARCH)
        assertEquals("+ New SKU", DistributionMasterCopy.SKU_BROWSER_CREATE)
        assertTrue(DistributionMasterCopy.skuCategories.contains("Staples"))
    }

    @Test
    fun switchMomentCopyMatchesFigma() {
        assertEquals("Switch Moment", DistributionMasterCopy.SWITCH_TITLE)
        assertEquals("Your Business moments · tap to open", DistributionMasterCopy.SWITCH_SUBTITLE)
    }

    @Test
    fun savePartyCtaMatchesFigma() {
        assertEquals("Save party", DistributionMasterCopy.saveCta(OperatingMasterTab.Principals, false))
        assertEquals("Save SKU", DistributionMasterCopy.saveCta(OperatingMasterTab.Catalogue, false))
    }
}

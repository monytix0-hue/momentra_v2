package com.example.momentra.ui.shell.business.shared

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OperatingLovKindMatchTest {

    @Test
    fun principalAppearsInSupplierAndBuyerLovs() {
        assertTrue(OperatingLovKindMatch.masterMatchesLovKind("SUPPLIER", "PRINCIPAL"))
        assertTrue(OperatingLovKindMatch.masterMatchesLovKind("BUYER", "PRINCIPAL"))
        assertTrue(OperatingLovKindMatch.masterMatchesLovKind("SUPPLIER", "DEALER"))
        assertTrue(OperatingLovKindMatch.masterMatchesLovKind("BUYER", "CUSTOMER"))
        assertTrue(OperatingLovKindMatch.masterMatchesLovKind("PRODUCT", "SKU"))
        assertFalse(OperatingLovKindMatch.masterMatchesLovKind("SUPPLIER", "SKU"))
        assertFalse(OperatingLovKindMatch.masterMatchesLovKind("PRODUCT", "CUSTOMER"))
    }

    @Test
    fun partyKindsForSupplyIncludePrincipal() {
        assertTrue(
            OperatingLovKindMatch.partyKindsForSupply().containsAll(
                setOf("CUSTOMER", "BUYER", "SUPPLIER", "PRINCIPAL", "DEALER", "VENDOR"),
            ),
        )
    }

    @Test
    fun prefersSelectedThenMomentThenProjection() {
        assertEquals("sel", resolveOperatingCompanyId("sel", "mom", "proj"))
        assertEquals("mom", resolveOperatingCompanyId(null, "mom", "proj"))
        assertEquals("proj", resolveOperatingCompanyId(null, null, "proj"))
        assertNull(resolveOperatingCompanyId(null, null, null))
    }

    @Test
    fun blankProductKindAndPartyVendorEdgeCases() {
        assertTrue(OperatingLovKindMatch.masterMatchesLovKind("PRODUCT", ""))
        assertFalse(OperatingLovKindMatch.masterMatchesLovKind("UOM", ""))
        assertTrue(OperatingLovKindMatch.masterMatchesLovKind("UOM", "SKU"))
        assertTrue(OperatingLovKindMatch.masterMatchesLovKind("BUYER", "PARTY"))
        assertTrue(OperatingLovKindMatch.masterMatchesLovKind("SUPPLIER", "PARTY"))
        assertTrue(OperatingLovKindMatch.masterMatchesLovKind("SUPPLIER", "VENDOR"))
    }
}
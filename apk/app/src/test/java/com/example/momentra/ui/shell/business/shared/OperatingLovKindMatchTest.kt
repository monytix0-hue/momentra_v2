package com.example.momentra.ui.shell.business.shared

import org.junit.Assert.assertFalse
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
}
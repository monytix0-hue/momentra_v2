package com.example.momentra.ui.shell.business.retail

import com.example.momentra.ui.shell.business.shared.RetailInviteRoles
import com.example.momentra.ui.shell.business.shared.retailInviteRoleAccess
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RetailMastersCopyTest {

    @Test
    fun inviteRolesMatchFigmaLabels() {
        assertEquals(
            listOf("Owner", "Manager", "Cashier", "Stock", "Floor"),
            RetailInviteRoles.map { it.second },
        )
        assertEquals(
            setOf("FINANCE", "APPROVER", "OPERATIONS_LEAD", "CONTRIBUTOR"),
            RetailInviteRoles.map { it.first }.toSet(),
        )
    }

    @Test
    fun cashierAccessMentionsPos() {
        val access = retailInviteRoleAccess("OPERATIONS_LEAD")
        assertTrue(access.contains("Cashier"))
        assertTrue(access.contains("POS"))
    }

    @Test
    fun mastersWhyNotesLockFigmaCopy() {
        assertTrue(MastersTab.Invite.why.contains("Joining ≠ approval ≠ payment"))
        assertTrue(MastersTab.Suppliers.why.contains("Purchase Order never posts GST"))
        assertTrue(MastersTab.Catalogue.why.contains("never on Sales Order save"))
        assertTrue(MastersTab.Locations.why.contains("never on order save"))
    }

    @Test
    fun formCtasMatchFigma() {
        assertEquals("Save supplier", RetailMasterFormKind.Supplier.cta)
        assertEquals("Save location", RetailMasterFormKind.Location.cta)
        assertEquals("Save product", RetailMasterFormKind.Product.cta)
        assertEquals("Save stock item", RetailMasterFormKind.StockItem.cta)
        assertEquals("✓ Saved · sheet closes and returns back", RetailCaptureCopy.SAVED_TOAST)
        assertEquals("Stock. Sell. Settle.", RetailCaptureCopy.TAGLINE)
    }
}

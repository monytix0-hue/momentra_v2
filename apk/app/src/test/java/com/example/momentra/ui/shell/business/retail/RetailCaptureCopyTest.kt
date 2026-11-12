package com.example.momentra.ui.shell.business.retail

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RetailCaptureCopyTest {

    @Test
    fun moneyCtasMatchFigma() {
        assertEquals("Post purchase", RetailCaptureCopy.cta("PURCHASE"))
        assertEquals("Post purchase · ₹65,000", RetailCaptureCopy.cta("PURCHASE", "₹65,000"))
        assertEquals("Post collection · ₹18,000", RetailCaptureCopy.cta("COLLECTION", "₹18,000"))
        assertEquals("Post payment · ₹65,000", RetailCaptureCopy.cta("PAYMENT", "₹65,000"))
    }

    @Test
    fun moneyWhyBodiesMatchFigmaSemantics() {
        assertEquals(
            "Stock += accepted qty only on receipt. AP from posted purchase; payment allocates later — never double-cost.",
            RetailCaptureCopy.why("PURCHASE"),
        )
        assertEquals(
            "Allocates open AR — never creates new sales. Receipt evidence ≠ separate revenue.",
            RetailCaptureCopy.why("COLLECTION"),
        )
        assertEquals(
            "Settles open AP only — never posts new purchase cost.",
            RetailCaptureCopy.why("PAYMENT"),
        )
        assertTrue(RetailCaptureCopy.why("COLLECTION").contains("never creates new sales"))
        assertTrue(RetailCaptureCopy.why("PAYMENT").contains("never posts new purchase cost"))
    }

    @Test
    fun titlesChipsFooterToastLocked() {
        assertEquals("Record Purchase", RetailCaptureCopy.sheetTitle("PURCHASE"))
        assertEquals("Record Collection", RetailCaptureCopy.sheetTitle("COLLECTION"))
        assertEquals("Record Payment", RetailCaptureCopy.sheetTitle("PAYMENT"))
        assertEquals(listOf("Goods", "Supplies"), RetailCaptureCopy.modeChips("PURCHASE"))
        assertTrue(RetailCaptureCopy.modeChips("COLLECTION").isEmpty())
        assertTrue(RetailCaptureCopy.modeChips("PAYMENT").isEmpty())
        assertEquals("Draft preserved if you close or open a picker", RetailCaptureCopy.DRAFT_FOOTER)
        assertEquals("✓ Saved · sheet closes and returns back", RetailCaptureCopy.SAVED_TOAST)
        assertEquals("Stock. Sell. Settle.", RetailCaptureCopy.TAGLINE)
        assertEquals("Notes · Proof · Tags", RetailCaptureCopy.MORE_DETAILS_HINT)
    }

    @Test
    fun subtitlesMatchHarborMartChrome() {
        assertEquals(
            "Back store · Harbor Mart Andheri · GST on",
            RetailCaptureCopy.purchaseSubtitle("Back store", "Harbor Mart Andheri", gstOn = true),
        )
        assertEquals(
            "Register 2 · Harbor Mart Andheri · AR settle",
            RetailCaptureCopy.collectionSubtitle("Harbor Mart Andheri"),
        )
        assertEquals(
            "AP desk · Harbor Mart Andheri · settle supplier",
            RetailCaptureCopy.paymentSubtitle("Harbor Mart Andheri"),
        )
        assertEquals("Harbor Mart Andheri", RetailCaptureCopy.storeLine(null))
        assertEquals("Harbor Mart Andheri", RetailCaptureCopy.storeLine("Harbor Mart"))
    }

    @Test
    fun retailCopyIsNotServiceTeal() {
        // Guard: money Why must stay rose-retail wording, not Service teal variants.
        assertFalse(RetailCaptureCopy.why("PURCHASE").contains("Consumables"))
        assertFalse(RetailCaptureCopy.why("COLLECTION").contains("never creates a new SERVICE"))
        assertFalse(RetailCaptureCopy.why("PAYMENT").equals("Settles open AP only."))
    }

    @Test
    fun catalogueSubcategoriesMatchLockedTaxonomy() {
        assertEquals(
            listOf("Grains", "Oils", "Pulses", "Spices", "Salt and sugar"),
            RetailCaptureCopy.catalogueSubcategories("Staples"),
        )
        assertEquals("Grains", RetailCaptureCopy.defaultSubcategory("Staples"))
        assertEquals(
            listOf("Biscuits", "Namkeen", "Chips", "Sweets"),
            RetailCaptureCopy.catalogueSubcategories("Snacks"),
        )
        assertTrue(RetailCaptureCopy.catalogueSubcategories("Unknown").isEmpty())
    }

    @Test
    fun phase4OrderStockStatusCopyMatchesFigma() {
        assertEquals("Sales Order", RetailCaptureCopy.sheetTitle("ORDER"))
        assertEquals("Purchase Order", RetailCaptureCopy.sheetTitle("PURCHASE_ORDER"))
        assertEquals("Stock Update", RetailCaptureCopy.sheetTitle("STOCK"))
        assertEquals("Status / Assignment", RetailCaptureCopy.sheetTitle("STATUS"))

        assertEquals(listOf("Sales order", "Purchase order"), RetailCaptureCopy.modeChips("ORDER"))
        assertEquals(listOf("Set current", "Add", "Reduce"), RetailCaptureCopy.modeChips("STOCK"))
        assertEquals(listOf("Dispatch", "Fulfil", "Pick"), RetailCaptureCopy.modeChips("STATUS"))

        assertEquals(
            "Sales order is commitment only — revenue + stock on posted Sale / POS Charge. Hold parks draft. Never posts GST as sale by itself.",
            RetailCaptureCopy.why("ORDER"),
        )
        assertEquals(
            "Purchase order is commitment only — no AP, no stock, no GST posting. Cost + stock on goods receipt / posted Purchase. Partial receive stays on PO.",
            RetailCaptureCopy.why("PURCHASE_ORDER"),
        )
        assertEquals(
            "Quantity delta only — never invent unit cost or BOM. Damage uses Reduce + reason.",
            RetailCaptureCopy.why("STOCK"),
        )
        assertEquals(
            "Operational state only — does not post revenue or stock by itself. Sale / POS Charge posts facts.",
            RetailCaptureCopy.why("STATUS"),
        )

        assertEquals("Save sales order · ₹5,180", RetailCaptureCopy.cta("ORDER", "₹5,180"))
        assertEquals("Save purchase order · ₹25,340", RetailCaptureCopy.cta("PURCHASE_ORDER", "₹25,340"))
        assertEquals("Post stock update", RetailCaptureCopy.cta("STOCK"))
        assertEquals("Update status", RetailCaptureCopy.cta("STATUS"))

        assertEquals(
            "Customer commitment · Harbor Mart Andheri · not revenue",
            RetailCaptureCopy.salesOrderSubtitle("Harbor Mart Andheri"),
        )
        assertEquals(
            "Supplier commitment · Harbor Mart Andheri · not AP",
            RetailCaptureCopy.purchaseOrderSubtitle("Harbor Mart Andheri"),
        )
        assertEquals("Floor count · Register aisle · Andheri", RetailCaptureCopy.stockSubtitle())
        assertEquals(
            "Floor board · Harbor Mart · ops only",
            RetailCaptureCopy.statusSubtitle("Harbor Mart Andheri"),
        )

        assertEquals(listOf("Staples", "Snacks", "Personal care", "Beverages", "Home care"), RetailCaptureCopy.soCategories)
        assertTrue(RetailCaptureCopy.poCategories.contains("Packaging"))
        assertTrue(RetailCaptureCopy.why("STATUS").contains("does not post revenue or stock"))
        assertFalse(RetailCaptureCopy.why("ORDER").contains("posts one itemized sale"))
        assertEquals(
            "Commitment only · revenue, stock & GST post on Sale / POS",
            RetailCaptureCopy.SO_COMMITMENT_FOOTER,
        )
        assertEquals(
            "Commitment only · AP, stock & input GST post on Purchase / GRN",
            RetailCaptureCopy.PO_COMMITMENT_FOOTER,
        )
    }

    @Test
    fun phase6BudgetIssueCopyMatchesFigma() {
        assertEquals("Budget / Target Revision", RetailCaptureCopy.sheetTitle("BUDGET"))
        assertEquals("Budget / Target Revision", RetailCaptureCopy.sheetTitle("TARGET"))
        assertEquals("Issue / Approval / Update", RetailCaptureCopy.sheetTitle("ISSUE"))
        assertEquals("Issue / Approval / Update", RetailCaptureCopy.sheetTitle("UPDATE"))
        assertEquals("Issue / Approval / Update", RetailCaptureCopy.sheetTitle("APPROVAL"))

        assertEquals(listOf("Sales target", "Budget"), RetailCaptureCopy.modeChips("BUDGET"))
        assertEquals(listOf("Issue", "Approval", "Update"), RetailCaptureCopy.modeChips("ISSUE"))
        assertEquals(listOf("Alert", "Warn", "Info"), RetailCaptureCopy.issueSeverities)

        assertEquals(
            "Versioned + effective-dated plan. Never rewrite posted sales to match target.",
            RetailCaptureCopy.why("BUDGET"),
        )
        assertEquals(
            "Audit / action / permission — approval ≠ disbursement. Shift variance opens Issue, not a sale edit.",
            RetailCaptureCopy.why("ISSUE"),
        )
        assertTrue(RetailCaptureCopy.why("ISSUE").contains("approval ≠ disbursement"))
        assertTrue(RetailCaptureCopy.why("BUDGET").contains("Never rewrite posted sales"))
        // Not Service teal / shortened variants.
        assertFalse(RetailCaptureCopy.why("BUDGET").equals("Versioned + effective-dated."))
        assertFalse(RetailCaptureCopy.why("ISSUE").equals("Audit / action — approval ≠ disbursement."))

        assertEquals("Save revision · ₹6,00,000", RetailCaptureCopy.cta("BUDGET", "₹6,00,000"))
        assertEquals("Save revision", RetailCaptureCopy.cta("BUDGET"))
        assertEquals("Post issue", RetailCaptureCopy.cta("ISSUE"))
        assertEquals("Post issue", RetailCaptureCopy.issueCta("Issue"))
        assertEquals("Post approval", RetailCaptureCopy.issueCta("Approval"))
        assertEquals("Post update", RetailCaptureCopy.issueCta("Update"))

        assertEquals("New target", RetailCaptureCopy.amountLabel("BUDGET"))
        assertEquals("New target", RetailCaptureCopy.budgetPayableLabel("Sales target"))
        assertEquals("New budget", RetailCaptureCopy.budgetPayableLabel("Budget"))

        assertEquals(
            "Owner plan · Harbor Mart · versioned",
            RetailCaptureCopy.budgetSubtitle("Harbor Mart Andheri"),
        )
        assertEquals(
            "Register 2 · Harbor Mart · audit trail",
            RetailCaptureCopy.issueSubtitle("Harbor Mart Andheri"),
        )
        assertEquals("Notes · Attachment · Tags", RetailCaptureCopy.BUDGET_MORE_HINT)
        assertEquals("Notes · Evidence · Tags", RetailCaptureCopy.ISSUE_MORE_HINT)
        assertEquals("Stock. Sell. Settle.", RetailCaptureCopy.TAGLINE)
        assertEquals("Festive demand", RetailCaptureCopy.BUDGET_FIXTURE_REASON)
        assertEquals("POS shift unreconciled", RetailCaptureCopy.ISSUE_FIXTURE_TITLE)
        assertEquals("REG-2", RetailCaptureCopy.ISSUE_FIXTURE_CODE)
    }
}

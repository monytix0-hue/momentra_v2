package com.example.momentra.ui.shell.business.shared

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DistributionQuickAddCopyTest {

    @Test
    fun purchaseContextContainsStockOnlyWhenReceived() {
        val line = DistributionQuickAddCopy.context("PURCHASE", "Metro")
        assertEquals(
            "Metro Distribution · Purchase ≠ payment · stock only when received",
            line,
        )
        assertTrue(line.contains("stock only when received"))
    }

    @Test
    fun blankBrandFallsBackToDistribution() {
        assertEquals(
            "Distribution Distribution · Sale ≠ receipt · stock only on dispatch",
            DistributionQuickAddCopy.context("SALE", "  "),
        )
    }

    @Test
    fun contextSuffixesMatchFigmaTable() {
        val expected = mapOf(
            "SALE" to "Sale ≠ receipt · stock only on dispatch",
            "PURCHASE" to "Purchase ≠ payment · stock only when received",
            "COLLECTION" to "Never posts a new sale · allocates to open AR",
            "PAYMENT" to "Never posts a new purchase · allocates to open AP",
            "SPEND" to "Expense ≠ payment · payable if unpaid",
            "STOCK" to "Reconciliation/adjustment — not a purchase",
            "RETURN" to "Reversal only · stock changes need explicit goods movement",
            "BUDGET" to "Versioned plan · never mutates past actuals",
            "UPDATE" to "Operational only — never money or stock by inference",
            "APPROVAL" to "Approval ≠ disbursement · maker-checker aware",
            "ORDER" to "Commitment only — no sale, purchase, stock, or money",
            "SHIPMENT" to "Physical movement only — not a new sale or purchase",
            "HOLD" to "Freeze open order or dispatch",
            "TRANSFER" to "Move qty between locations",
            "REPRINT" to "Read only · never posts",
        )
        expected.forEach { (mode, suffix) ->
            assertEquals(mode, suffix, DistributionQuickAddCopy.contextSuffix(mode))
        }
    }

    @Test
    fun holdTransferReprintWhyAreDistributionSpecific() {
        assertTrue(DistributionQuickAddCopy.why("HOLD").contains("Hold pauses a delivery order"))
        assertTrue(DistributionQuickAddCopy.why("TRANSFER").contains("warehouses at cost"))
        assertTrue(DistributionQuickAddCopy.why("REPRINT").contains("same number"))
    }

    @Test
    fun approvalCtaIsConfirmAction() {
        assertEquals("Confirm action", DistributionQuickAddCopy.cta("APPROVAL"))
    }

    @Test
    fun moreDetailsLabelMatchesFigma() {
        assertEquals("More details · notes, attachments, tags", DistributionQuickAddCopy.MORE_DETAILS_LABEL)
    }
}

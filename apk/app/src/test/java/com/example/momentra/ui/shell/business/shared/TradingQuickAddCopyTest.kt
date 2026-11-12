package com.example.momentra.ui.shell.business.shared

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TradingQuickAddCopyTest {

    @Test
    fun purchaseContextContainsStockOnlyWhenReceived() {
        val line = TradingQuickAddCopy.context("PURCHASE", "Northstar")
        assertEquals(
            "Northstar Trading · Purchase ≠ payment · stock only when received",
            line,
        )
        assertTrue(line.contains("stock only when received"))
    }

    @Test
    fun blankBrandFallsBackToTrading() {
        assertEquals(
            "Trading Trading · Sale ≠ receipt · stock only on dispatch",
            TradingQuickAddCopy.context("SALE", "  "),
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
            "ISSUE" to "Operational only — never money or stock by inference",
            "APPROVAL" to "Approval ≠ disbursement · maker-checker aware",
            "ORDER" to "Commitment only — no sale, purchase, stock, or money",
            "SHIPMENT" to "Physical movement only — not a new sale or purchase",
            "HOLD" to "Freeze open order or dispatch",
            "TRANSFER" to "Move qty between locations",
            "REPRINT" to "Read only · never posts",
        )
        expected.forEach { (mode, suffix) ->
            assertEquals(mode, suffix, TradingQuickAddCopy.contextSuffix(mode))
        }
    }

    @Test
    fun whyBodiesMatchFigmaTable() {
        assertEquals(
            "Mode chips re-render the form — item fields hidden in Quick amount. Part paid posts one sale + linked partial receipt + remaining AR. Itemized total is read-only sum of lines.",
            TradingQuickAddCopy.why("SALE"),
        )
        assertEquals(
            "Issue fields appear only for Issue mode. Creates Needs Attention + Activity entries, not financial facts.",
            TradingQuickAddCopy.why("UPDATE"),
        )
        assertEquals(
            "Only role-authorized actions shown. Self-approval blocked when maker-checker enabled. Reimburse posts payment separately from approval.",
            TradingQuickAddCopy.why("APPROVAL"),
        )
    }

    @Test
    fun approvalCtaIsConfirmAction() {
        assertEquals("Confirm action", TradingQuickAddCopy.cta("APPROVAL"))
    }

    @Test
    fun moreDetailsLabelMatchesFigma() {
        assertEquals("More details · notes, attachments, tags", TradingQuickAddCopy.MORE_DETAILS_LABEL)
    }
}

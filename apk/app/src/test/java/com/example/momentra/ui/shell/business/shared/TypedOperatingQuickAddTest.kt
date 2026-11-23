package com.example.momentra.ui.shell.business.shared

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Behaviour under test: typed Quick Add field specs for nine subtypes, Own Campaign
 * exclusions, and client-side required-field validation before POST.
 * Module: TypedOperatingQuickAdd.kt (`quickAddFieldSpec`, `validateTypedQuickAdd`).
 * Existing test files for this module: none (sibling: WidgetCoveragePresentationTest.kt).
 * User instruction: "Build typed forms for all nine Business Moment types."
 * User instruction: "No unsupported Client-only actions in Own Campaign."
 * User instruction: "Validate required fields before submission."
 */
class TypedOperatingQuickAddTest {
    @Test
    fun ownCampaignExcludesInvoiceAndCollection() {
        val invoice = quickAddFieldSpec("INVOICE", "OWN_BUSINESS_CAMPAIGN")
        val collection = quickAddFieldSpec("COLLECTION", "OWN_BUSINESS_CAMPAIGN")
        assertTrue(invoice.fields.isEmpty())
        assertTrue(collection.fields.isEmpty())
        assertTrue(
            validateTypedQuickAdd(
                "INVOICE",
                "OWN_BUSINESS_CAMPAIGN",
                needsAmount = true,
                draft = TypedQuickAddDraft(title = "x", amount = "1"),
            ).isNotEmpty(),
        )
    }

    @Test
    fun clientCampaignKeepsInvoice() {
        val invoice = quickAddFieldSpec("INVOICE", "CLIENT_CAMPAIGN")
        assertTrue(QuickAddField.TITLE in invoice.required)
        assertTrue(QuickAddField.COUNTERPARTY in invoice.required)
    }

    @Test
    fun retailSaleRequiresChannel() {
        val spec = quickAddFieldSpec("SALE", "RETAIL_STORE")
        assertTrue(QuickAddField.CHANNEL in spec.required)
        val errors = validateTypedQuickAdd(
            "SALE",
            "RETAIL_STORE",
            needsAmount = true,
            draft = TypedQuickAddDraft(title = "Sale", amount = "100"),
        )
        assertTrue(errors.any { it.contains("Channel") })
    }

    @Test
    fun tradingSaleDoesNotRequireChannel() {
        val spec = quickAddFieldSpec("SALE", "TRADING_WHOLESALE")
        assertFalse(QuickAddField.CHANNEL in spec.required)
    }

    @Test
    fun shipmentRequiresParentAndQuantity() {
        val spec = quickAddFieldSpec("SHIPMENT", "DISTRIBUTION_DEALERSHIP")
        assertTrue(QuickAddField.PARENT in spec.required)
        assertTrue(QuickAddField.QUANTITY in spec.required)
    }

    @Test
    fun productionIncludesRejectedQtyAndParent() {
        val spec = quickAddFieldSpec("PRODUCTION", "MADE_TO_ORDER")
        assertTrue(QuickAddField.REJECTED_QTY in spec.fields)
        assertTrue(QuickAddField.PARENT in spec.fields)
        assertTrue(QuickAddField.QUANTITY in spec.required)
    }

    @Test
    fun saleExposesGstFieldsWithoutClientMath() {
        val spec = quickAddFieldSpec("SALE", "TRADING_WHOLESALE")
        assertTrue(QuickAddField.TAX_MODE in spec.fields)
        assertTrue(QuickAddField.GST_RATE in spec.fields)
    }

    @Test
    fun paymentRequiresParent() {
        val errors = validateTypedQuickAdd(
            "PAYMENT",
            "TRADING_WHOLESALE",
            needsAmount = true,
            draft = TypedQuickAddDraft(title = "Pay", amount = "50"),
        )
        assertTrue(errors.any { it.contains("Linked source") })
    }

    @Test
    fun spendRequiresExpenseCategory() {
        val errors = validateTypedQuickAdd(
            "SPEND",
            "CLIENT_CAMPAIGN",
            needsAmount = true,
            draft = TypedQuickAddDraft(title = "Ad spend", amount = "200"),
        )
        assertTrue(errors.any { it.contains("Expense category") })
        val ok = validateTypedQuickAdd(
            "SPEND",
            "CLIENT_CAMPAIGN",
            needsAmount = true,
            draft = TypedQuickAddDraft(
                title = "Ad spend",
                amount = "200",
                expenseCategory = "Marketing",
            ),
        )
        assertTrue(ok.isEmpty())
    }
}

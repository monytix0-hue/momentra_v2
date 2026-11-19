package com.example.momentra.ui.shell.business.shared

import com.example.momentra.data.api.OperatingMasterDto
import com.example.momentra.data.api.OperatingProjectionDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Itemized Sale/Purchase amount auto-fill (Figma TRD-QA-01): price × qty unless dirty.
 */
class CaptureFormLineAmountTest {

    private fun form() = CaptureForm(
        channel = "Counter",
        choice = "Received",
        unit = "pc",
        masterKind = "PRODUCT",
        receiptState = "Fully Delivered",
        detail = "",
    )

    private fun projectionWithProduct(
        sellPrice: String = "2400",
        purchasePrice: String? = null,
    ): OperatingProjectionDto {
        val master = OperatingMasterDto(
            id = "m-rice",
            kind = "PRODUCT",
            name = "Basmati Rice 25kg",
            sellPrice = sellPrice,
            purchasePrice = purchasePrice,
            unit = "bags",
        )
        return OperatingProjectionDto(
            subtype = "TRADING_WHOLESALE",
            masters = listOf(master),
        )
    }

    @Test
    fun applyLovProduct_syncsLineAmountFromSellPriceAndDefaultQty() {
        val form = form()
        val model = projectionWithProduct("2400")
        form.applyLov(
            kind = "PRODUCT",
            option = OperatingLovOption(
                id = "m-rice",
                name = "Basmati Rice 25kg",
                kind = "PRODUCT",
                unit = "bags",
            ),
            mode = "SALE",
            model = model,
        )
        assertEquals("1", form.quantity)
        assertEquals("2400", form.unitPrice)
        assertFalse(form.lineAmountEdited)
        assertEquals("2400.00", form.lineAmount)
    }

    @Test
    fun syncLineAmountFromQtyPrice_multipliesQtyAndPrice() {
        val form = form()
        form.quantity = "12"
        form.unitPrice = "2400"
        form.lineAmountEdited = false
        form.syncLineAmountFromQtyPrice()
        assertEquals("28800.00", form.lineAmount)
    }

    @Test
    fun syncLineAmountFromQtyPrice_skipsWhenDirty() {
        val form = form()
        form.quantity = "12"
        form.unitPrice = "2400"
        form.lineAmount = "5000.00"
        form.lineAmountEdited = true
        form.syncLineAmountFromQtyPrice()
        assertEquals("5000.00", form.lineAmount)
    }

    @Test
    fun resolvedEditorUnitPrice_backSolvesFromDirtyLineAmount() {
        val form = form()
        form.quantity = "2"
        form.unitPrice = "100"
        form.lineAmount = "5000"
        form.lineAmountEdited = true
        val resolved = form.resolvedEditorUnitPrice()
        assertEquals(
            BigDecimal("2500").setScale(4, RoundingMode.HALF_UP),
            BigDecimal(resolved!!).setScale(4, RoundingMode.HALF_UP),
        )
    }

    @Test
    fun itemizedSaleTotal_sumsCartAndPendingEditor() {
        val form = form()
        form.savedLines = listOf(
            com.example.momentra.data.api.OperatingLineBody(
                itemName = "A",
                quantity = "12",
                unit = "bags",
                unitPrice = "2400",
            ),
        )
        form.itemName = "B"
        form.quantity = "8"
        form.unitPrice = "1850"
        form.lineAmountEdited = false
        form.syncLineAmountFromQtyPrice()
        val total = form.itemizedSaleTotal(includePendingEditor = true)
        assertEquals(BigDecimal("43600.00"), total!!.setScale(2, RoundingMode.HALF_UP))
    }

    @Test
    fun itemizedTotalCaption_matchesFigma() {
        assertEquals("Itemized total is read-only · sum of lines", ITEMIZED_TOTAL_CAPTION)
    }
}

package com.example.momentra.ui.shell.business

import com.example.momentra.data.api.OperatingMasterDto
import com.example.momentra.ui.shell.business.shared.OperatingCaptureLayout
import com.example.momentra.ui.shell.business.shared.OperatingMasterDraft
import com.example.momentra.ui.shell.business.shared.OperatingMasterTab
import com.example.momentra.ui.shell.business.shared.OperatingStockReasons
import com.example.momentra.ui.shell.business.shared.defaultKindFor
import com.example.momentra.ui.shell.business.shared.kinds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure mapping for the masters surface: which kinds a tab lists, which body the form
 * sends, and which QA layout a capture mode renders. No Compose, no network.
 */
class OperatingMasterMappingTest {

    @Test
    fun tabKindsPartitionTheMasterKinds() {
        val principals = OperatingMasterTab.Principals.kinds()
        val catalogue = OperatingMasterTab.Catalogue.kinds()
        val locations = OperatingMasterTab.Locations.kinds()

        assertTrue(principals.contains("DEALER"))
        assertTrue(principals.contains("PRINCIPAL"))
        assertTrue(principals.contains("BUYER"))
        assertTrue(catalogue.contains("SKU"))
        assertTrue(catalogue.contains("BATCH"))
        assertTrue(locations.contains("WAREHOUSE"))
        assertTrue(locations.contains("ROUTE"))

        assertEquals(emptySet<String>(), principals intersect catalogue)
        assertEquals(emptySet<String>(), catalogue intersect locations)
        // Price lists and invites are not master rows at all.
        assertTrue(OperatingMasterTab.PriceLists.kinds().isEmpty())
        assertTrue(OperatingMasterTab.Invite.kinds().isEmpty())
    }

    @Test
    fun defaultKindMatchesTheTab() {
        assertEquals("DEALER", defaultKindFor(OperatingMasterTab.Principals))
        assertEquals("SKU", defaultKindFor(OperatingMasterTab.Catalogue))
        assertEquals("WAREHOUSE", defaultKindFor(OperatingMasterTab.Locations))
    }

    @Test
    fun draftTrimsAndBlanksToNullOnTheWire() {
        val body = OperatingMasterDraft(
            kind = "SKU",
            name = "  Basmati 25kg  ",
            code = "   ",
            unit = "BAG",
            sellPrice = " 1850 ",
        ).toCreateBody()

        assertEquals("Basmati 25kg", body.name)
        assertEquals("SKU", body.kind)
        assertEquals("BAG", body.unit)
        assertEquals("1850", body.sellPrice)
        assertNull("A blank code must not be sent as an empty string", body.code)
        assertNull(body.purchasePrice)
        assertNull(body.parentMasterId)
    }

    @Test
    fun gstApplicabilityFollowsTheRate() {
        val withRate = OperatingMasterDraft(kind = "SKU", name = "Tea", gstRate = "5").toCreateBody()
        assertEquals("YES", withRate.gstApplicable)
        assertEquals("5", withRate.gstRate)

        val withoutRate = OperatingMasterDraft(kind = "SKU", name = "Tea", gstRate = " ").toCreateBody()
        assertNull("No rate means the master stays out of the GST position", withoutRate.gstApplicable)
        assertNull(withoutRate.gstRate)
    }

    @Test
    fun updateBodyCarriesTheVersionItSaw() {
        val update = OperatingMasterDraft(kind = "DEALER", name = "Sharma Traders", billingState = "MH")
            .toUpdateBody(expectedVersion = 7)
        assertEquals(7, update.expectedVersion)
        assertEquals("MH", update.billingState)
        assertEquals("Sharma Traders", update.name)
    }

    @Test
    fun aMasterCannotBeSavedWithoutAName() {
        assertFalse(OperatingMasterDraft(kind = "SKU", name = "   ").canSave)
        assertTrue(OperatingMasterDraft(kind = "SKU", name = "Rice").canSave)
    }

    @Test
    fun masterRowsCarryStableIdentityAndSource() {
        val row = OperatingMasterDto(id = "m-1", source = "MASTER", kind = "DEALER", name = "Sharma", version = 3)
        assertEquals("m-1", row.id)
        assertEquals("MASTER", row.source)
        assertEquals(3, row.version)
    }

    @Test
    fun everyCaptureModeMapsToATwelveQaLayout() {
        val expected = mapOf(
            "SALE" to OperatingCaptureLayout.SALE,
            "PURCHASE" to OperatingCaptureLayout.PURCHASE,
            "COLLECTION" to OperatingCaptureLayout.COLLECTION,
            "PAYMENT" to OperatingCaptureLayout.PAYMENT,
            "SPEND" to OperatingCaptureLayout.SPEND,
            "STOCK" to OperatingCaptureLayout.STOCK,
            "RETURN" to OperatingCaptureLayout.RETURN,
            "BUDGET" to OperatingCaptureLayout.BUDGET,
            "UPDATE" to OperatingCaptureLayout.UPDATE,
            "APPROVAL" to OperatingCaptureLayout.APPROVAL,
            "ORDER" to OperatingCaptureLayout.ORDER,
            "SHIPMENT" to OperatingCaptureLayout.SHIPMENT,
        )
        expected.forEach { (mode, layout) ->
            assertEquals(mode, layout, OperatingCaptureLayout.forMode(mode))
        }
        assertEquals(12, OperatingCaptureLayout.entries.size)
    }

    @Test
    fun aliasesReuseTheirLayout() {
        assertEquals(OperatingCaptureLayout.SALE, OperatingCaptureLayout.forMode("SERVICE"))
        assertEquals(OperatingCaptureLayout.PURCHASE, OperatingCaptureLayout.forMode("VENDOR"))
        assertEquals(OperatingCaptureLayout.SHIPMENT, OperatingCaptureLayout.forMode("DELIVERY"))
        assertEquals(OperatingCaptureLayout.PAYMENT, OperatingCaptureLayout.forMode("REIMBURSE"))
        assertEquals(OperatingCaptureLayout.BUDGET, OperatingCaptureLayout.forMode("TARGET"))
        assertEquals(OperatingCaptureLayout.STOCK, OperatingCaptureLayout.forMode("TRANSFER"))
        assertEquals(OperatingCaptureLayout.STOCK, OperatingCaptureLayout.forMode("HOLD"))
        assertNull(OperatingCaptureLayout.forMode("REPRINT"))
    }

    @Test
    fun qaCodesAreUniqueAndSequential() {
        assertEquals(
            (1..12).map { "QA-%02d".format(it) },
            OperatingCaptureLayout.entries.map { it.qaCode },
        )
    }

    @Test
    fun stockReasonsOnlySendWordsTheContractAccepts() {
        listOf("Opening count", "Stocktake", "Damage", "Waste", "Return", "Correction", "Replenishment", "")
            .forEach { label ->
                val code = OperatingStockReasons.reasonCode(label)
                if (code != null) {
                    assertTrue(
                        "A label must never reach the wire as itself: $label -> $code",
                        OperatingStockReasons.accepted.contains(code),
                    )
                }
            }
        assertNull(OperatingStockReasons.reasonCode("  "))
        // A transfer's reason is the transfer itself, not a made-up sentence.
        assertEquals("TRANSFER", OperatingStockReasons.reasonCode("Transfer"))
        assertEquals("correction", OperatingStockReasons.reasonCode("Replenishment"))
    }

    @Test
    fun stockActionsAndOwnershipUseTheContractWords() {
        assertEquals("ADD", OperatingStockReasons.stockAction("In"))
        assertEquals("REDUCE", OperatingStockReasons.stockAction("Out"))
        assertEquals("SET", OperatingStockReasons.stockAction("Set"))
        assertEquals("TRANSFER", OperatingStockReasons.stockAction("Transfer"))
        assertEquals("SET", OperatingStockReasons.stockAction("Set current"))
        assertEquals("ADD", OperatingStockReasons.stockAction("Add"))
        assertEquals("REDUCE", OperatingStockReasons.stockAction("Reduce"))
        assertEquals("SET", OperatingStockReasons.stockAction("COUNT"))
        assertEquals("OWNED", OperatingStockReasons.ownership("Owned"))
        assertEquals("CONSIGNMENT", OperatingStockReasons.ownership("Consignment"))
        assertEquals("RELEASED", OperatingStockReasons.holdStatus("No hold"))
        assertEquals("HELD", OperatingStockReasons.holdStatus("Hold order"))
    }
}

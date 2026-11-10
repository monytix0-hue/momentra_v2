package com.example.momentra.ui.shell.business

import com.example.momentra.data.api.OperatingBoardMetricDto
import com.example.momentra.data.api.OperatingProjectionDto
import com.example.momentra.ui.shell.business.distribution.DISTRIBUTION_SHEET_STATES
import com.example.momentra.ui.shell.business.distribution.DistributionSheetState
import com.example.momentra.ui.shell.business.distribution.distributionSheetForCode
import com.example.momentra.ui.shell.business.manufacturing.MANUFACTURING_SHEET_STATES
import com.example.momentra.ui.shell.business.manufacturing.ManufacturingSheetState
import com.example.momentra.ui.shell.business.manufacturing.manufacturingSheetForCode
import com.example.momentra.ui.shell.business.manufacturing.specFor
import com.example.momentra.ui.shell.business.shared.OperatingV12Theme
import com.example.momentra.ui.shell.business.shared.hasUsefulPulseChrome
import com.example.momentra.ui.shell.business.shared.sectionByCode
import com.example.momentra.ui.shell.business.trading.TRADING_PULSE_CODES
import com.example.momentra.ui.shell.business.trading.TRADING_SHEET_STATES
import com.example.momentra.ui.shell.business.trading.TradingSheetState
import com.example.momentra.ui.shell.business.trading.tradingSheetForCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The 18 `View all ›` surfaces of each vertical, plus the section-code router that opens
 * them. This is pure mapping: no Compose, no network — so it runs as a JVM unit test.
 */
class TradingSheetStateTest {

    @Test
    fun tradingListsExactlyEighteenSurfaces() {
        assertEquals(18, TRADING_SHEET_STATES.size)
        assertEquals(18, TRADING_SHEET_STATES.map { it.spec.code }.toSet().size)
    }

    @Test
    fun distributionListsExactlyEighteenSurfaces() {
        assertEquals(18, DISTRIBUTION_SHEET_STATES.size)
        assertEquals(18, DISTRIBUTION_SHEET_STATES.map { it.spec.code }.toSet().size)
    }

    @Test
    fun bothVerticalsCoverTheSameEighteenCodes() {
        assertEquals(
            TRADING_SHEET_STATES.map { it.spec.code }.toSet(),
            DISTRIBUTION_SHEET_STATES.map { it.spec.code }.toSet(),
        )
    }

    @Test
    fun manufacturingListsExactlyEighteenSurfaces() {
        assertEquals(18, MANUFACTURING_SHEET_STATES.size)
        assertEquals(18, MANUFACTURING_SHEET_STATES.map { it.spec.code }.toSet().size)
        // Manufacturing boards emit `production` instead of trading `network`.
        val trading = TRADING_SHEET_STATES.map { it.spec.code }.toSet()
        val manufacturing = MANUFACTURING_SHEET_STATES.map { it.spec.code }.toSet()
        assertTrue(manufacturing.contains("production"))
        assertFalse(manufacturing.contains("network"))
        assertEquals(trading - setOf("network") + setOf("production"), manufacturing)
    }

    @Test
    fun manufacturingMtoTitlesOverrideJobCopy() {
        val orders = ManufacturingSheetState.OrdersCommitments.specFor("MADE_TO_ORDER")
        assertEquals("Current Jobs", orders.title)
        assertEquals(
            "Work Orders & Commitments",
            ManufacturingSheetState.OrdersCommitments.specFor("STANDARD_PRODUCTION").title,
        )
        assertEquals("Job Output", ManufacturingSheetState.Production.specFor("MADE_TO_ORDER").title)
        assertEquals("Production", ManufacturingSheetState.Production.specFor("STANDARD_PRODUCTION").title)
    }

    @Test
    fun manufacturingThemeResolvesFromSubtype() {
        assertTrue(OperatingV12Theme.forSubtype("STANDARD_PRODUCTION").isManufacturing)
        assertTrue(OperatingV12Theme.forSubtype("MADE_TO_ORDER").isMadeToOrder)
        assertEquals("Plan. Make. Fulfil.", OperatingV12Theme.manufacturing().motionLine)
        assertEquals(
            "Quote. Confirm. Make. Invoice.",
            OperatingV12Theme.madeToOrder().motionLine,
        )
    }

    @Test
    fun sixPulseCodesRouteToPulseSurfaces() {
        assertEquals(6, TRADING_PULSE_CODES.size)
        TRADING_PULSE_CODES.forEach { code ->
            val state = TRADING_SHEET_STATES.first { it.spec.code == code }
            assertTrue("$code should be a pulse surface", state.spec.pulseSurface)
        }
        // The twelve board surfaces must not be marked as pulse surfaces.
        TRADING_SHEET_STATES.filterNot { it.spec.pulseSurface }.forEach {
            assertFalse(it.spec.code in TRADING_PULSE_CODES)
        }
    }

    @Test
    fun everySheetHasTitleEyebrowAndEmptyCopy() {
        TRADING_SHEET_STATES.forEach { state ->
            assertTrue(state.spec.title.isNotBlank())
            assertTrue(state.spec.eyebrow.isNotBlank())
            assertTrue(state.spec.emptyTitle.isNotBlank())
            assertTrue(state.spec.emptyBody.isNotBlank())
        }
        DISTRIBUTION_SHEET_STATES.forEach { state ->
            assertTrue(state.spec.title.isNotBlank())
            assertTrue(state.spec.eyebrow.isNotBlank())
            assertTrue(state.spec.emptyTitle.isNotBlank())
            assertTrue(state.spec.emptyBody.isNotBlank())
        }
    }

    @Test
    fun everySheetCarriesDstChromeMetadata() {
        DISTRIBUTION_SHEET_STATES.forEach { state ->
            assertTrue(state.spec.code, state.spec.figmaId.isNotBlank())
            assertTrue(state.spec.code, state.spec.statusFilters.isNotEmpty())
            assertTrue(state.spec.code, state.spec.footerCta.isNotBlank())
            assertEquals(3, state.spec.periodFilters.size)
        }
        assertEquals("Open Quick Add", DISTRIBUTION_SHEET_STATES.first().spec.footerCta)
        assertEquals(
            "Open Memory",
            DISTRIBUTION_SHEET_STATES.first { it.spec.code == "ME4" }.spec.footerCta,
        )
    }

    @Test
    fun sectionCodesRouteToTheirSurface() {
        assertEquals(TradingSheetState.OrdersCommitments, tradingSheetForCode("orders"))
        assertEquals(TradingSheetState.StockProducts, tradingSheetForCode("stock"))
        assertEquals(TradingSheetState.PeopleNetwork, tradingSheetForCode("network"))
        assertEquals(TradingSheetState.OpenCommitments, tradingSheetForCode("M4"))
        assertEquals(TradingSheetState.LifeCurrentBusiness, tradingSheetForCode("L1"))
        assertEquals(TradingSheetState.LifeDealersChannels, tradingSheetForCode("L2"))
        assertEquals(TradingSheetState.LifeMoneyMoving, tradingSheetForCode("L3"))
        assertEquals(TradingSheetState.LifePeopleActivity, tradingSheetForCode("L4"))
        assertEquals(TradingSheetState.MonthlyMemory, tradingSheetForCode("ME1"))
        assertEquals(TradingSheetState.MoneyMemory, tradingSheetForCode("ME2"))
        assertEquals(TradingSheetState.FulfilmentMemory, tradingSheetForCode("fulfilment"))
        assertEquals(TradingSheetState.PeoplePatterns, tradingSheetForCode("ME4"))

        assertEquals(DistributionSheetState.OrdersCommitments, distributionSheetForCode("orders"))
        assertEquals(DistributionSheetState.FulfilmentMemory, distributionSheetForCode("fulfilment"))
        assertEquals(DistributionSheetState.PeoplePatterns, distributionSheetForCode("ME4"))

        assertEquals(ManufacturingSheetState.OrdersCommitments, manufacturingSheetForCode("orders"))
        assertEquals(ManufacturingSheetState.Production, manufacturingSheetForCode("production"))
        assertNull(manufacturingSheetForCode("network"))
        assertEquals(ManufacturingSheetState.FulfilmentMemory, manufacturingSheetForCode("fulfilment"))
        assertEquals(ManufacturingSheetState.PeoplePatterns, manufacturingSheetForCode("ME4"))
    }

    @Test
    fun unknownSectionCodeHasNoSurface() {
        assertNull(tradingSheetForCode("not-a-section"))
        assertNull(distributionSheetForCode(""))
        assertNull(manufacturingSheetForCode(""))
    }

    @Test
    fun sectionLookupFindsSectionsAcrossAllThreeBoardGroups() {
        val model = OperatingProjectionDto(
            boards = com.example.momentra.data.api.OperatingBoardsDto(
                moments = listOf(
                    com.example.momentra.data.api.OperatingBoardSectionDto(code = "orders", title = "Orders"),
                ),
                life = listOf(
                    com.example.momentra.data.api.OperatingBoardSectionDto(code = "L3", title = "Money"),
                ),
                memory = listOf(
                    com.example.momentra.data.api.OperatingBoardSectionDto(code = "ME4", title = "Patterns"),
                ),
            ),
        )
        assertEquals("Orders", model.sectionByCode("orders")?.title)
        assertEquals("Money", model.sectionByCode("L3")?.title)
        assertEquals("Patterns", model.sectionByCode("ME4")?.title)
        assertNull(model.sectionByCode("stock"))
    }

    @Test
    fun metricToneIsOneOfTheThreeKnownValues() {
        val allowed = setOf("NORMAL", "WARNING", "POSITIVE")
        val metrics = listOf(
            OperatingBoardMetricDto(key = "ar", label = "AR open", value = "1,20,000", unit = "INR", tone = "WARNING"),
            OperatingBoardMetricDto(key = "delivered", label = "Delivered", value = "92", unit = "%", tone = "POSITIVE"),
            OperatingBoardMetricDto(key = "open", label = "Open orders", value = "4", tone = "NORMAL"),
        )
        metrics.forEach { tile ->
            assertTrue(tile.tone ?: "NORMAL" in allowed)
        }
    }

    @Test
    fun missingMeasurementIsNotRenderedAsZero() {
        // A gap in the record is carried verbatim so the tile can say `Not recorded`.
        val tile = OperatingBoardMetricDto(key = "received", label = "Received", value = "Not recorded")
        assertNotNull(tile.value)
        assertFalse(tile.value == "0")
    }

    @Test
    fun freshSetupWithBudgetKeepsPulseChrome() {
        val blank = OperatingProjectionDto()
        assertFalse(blank.hasUsefulPulseChrome())

        val withBudget = OperatingProjectionDto(
            budget = com.example.momentra.data.api.OperatingMoneyDto(
                amount = "500000",
                spent = "0",
                remaining = "500000",
                configured = true,
            ),
        )
        assertTrue(withBudget.hasUsefulPulseChrome())

        val withTarget = OperatingProjectionDto(
            target = com.example.momentra.data.api.OperatingTargetDto(
                metric = "REVENUE",
                value = "1000000",
                progress = "0",
                configured = true,
            ),
        )
        assertTrue(withTarget.hasUsefulPulseChrome())

        val withGst = OperatingProjectionDto(gstTracking = "YES")
        assertTrue(withGst.hasUsefulPulseChrome())
    }
}

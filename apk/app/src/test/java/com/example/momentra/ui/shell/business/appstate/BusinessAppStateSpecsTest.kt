package com.example.momentra.ui.shell.business.appstate

import androidx.compose.ui.graphics.toArgb
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.GregorianCalendar

/**
 * Guards the Figma "App States · Shared" contract:
 * every (screen, phase) resolves, error states carry a diagnostic code, Hub labels are
 * per-vertical, and — the kit's governing rule — a missing figure is never rendered as 0.
 */
class BusinessAppStateSpecsTest {

    private val phases = listOf(
        BusinessAppStatePhase.Empty,
        BusinessAppStatePhase.Loading,
        BusinessAppStatePhase.Error,
        BusinessAppStatePhase.Setup,
    )

    @Test
    fun everyScreenAndPhaseResolvesWithATitle() {
        BusinessAppStateScreen.entries.forEach { screen ->
            phases.forEach { phase ->
                val spec = BusinessAppStateSpecs.spec(screen, phase)
                assertEquals(screen, spec.screen)
                assertTrue(
                    "$screen/$phase must have a title",
                    spec.title.isNotBlank(),
                )
            }
        }
    }

    @Test
    fun errorVariantsCarryADiagnosticCode() {
        val errorScreens = listOf(
            BusinessAppStateScreen.Pulse,
            BusinessAppStateScreen.Moments,
            BusinessAppStateScreen.Life,
            BusinessAppStateScreen.Memory,
            BusinessAppStateScreen.Hub,
        )
        errorScreens.forEach { screen ->
            val spec = BusinessAppStateSpecs.spec(screen, BusinessAppStatePhase.Error)
            assertNotNull("$screen error needs a code", spec.errorCode)
            assertTrue(spec.errorCode!!.matches(Regex("[A-Z0-9_]+")))
        }
        assertEquals("PULSE_FETCH_FAILED", BusinessAppStateSpecs.spec(BusinessAppStateScreen.Pulse, BusinessAppStatePhase.Error).errorCode)
        assertEquals("MOMENT_DETAIL_FAILED", BusinessAppStateSpecs.spec(BusinessAppStateScreen.Moments, BusinessAppStatePhase.Error).errorCode)
        assertEquals("LIFE_FEED_FAILED", BusinessAppStateSpecs.spec(BusinessAppStateScreen.Life, BusinessAppStatePhase.Error).errorCode)
        assertEquals("MEMORY_FETCH_FAILED", BusinessAppStateSpecs.spec(BusinessAppStateScreen.Memory, BusinessAppStatePhase.Error).errorCode)
        assertEquals("HUB_FETCH_FAILED", BusinessAppStateSpecs.spec(BusinessAppStateScreen.Hub, BusinessAppStatePhase.Error).errorCode)
    }

    @Test
    fun offlineStateUsesABannerNotADiagnosticCode() {
        // Figma's Offline variant has no error-code pill: it leads with the amber offline banner
        // and a single Retry, and hides live figures rather than reporting a failure code.
        val offline = BusinessAppStateSpecs.spec(BusinessAppStateScreen.Offline, BusinessAppStatePhase.Error)
        assertEquals(BusinessAppStatePhase.Error, offline.phase)
        assertNull(offline.errorCode)
        assertEquals("Retry", offline.primaryLabel)
        assertNull(offline.secondaryLabel)
        assertNotNull(offline.eyebrow)
        assertEquals("You're offline · showing last known", BusinessAppStateSpecs.OFFLINE_BANNER)
    }

    @Test
    fun hubLabelsOverrideForServicesAndMarketing() {
        val defaults = BusinessAppStateSpecs.hubTileLabels(BusinessAppStateAccent.Trading).map { it.second }
        val services = BusinessAppStateSpecs.hubTileLabels(BusinessAppStateAccent.Services).map { it.second }
        val marketing = BusinessAppStateSpecs.hubTileLabels(BusinessAppStateAccent.Marketing).map { it.second }

        assertTrue(defaults.contains("Sale"))
        assertTrue(services.contains("Invoice"))
        assertTrue(services.contains("Appointment"))
        assertTrue(marketing.contains("Client invoice"))
        assertTrue(marketing.contains("Campaign"))

        assertTrue(services != defaults)
        assertTrue(marketing != services)
        assertEquals(6, defaults.size)
        assertEquals(6, services.size)
        assertEquals(6, marketing.size)
    }

    @Test
    fun noRowEverRendersZero() {
        BusinessAppStateScreen.entries.forEach { screen ->
            phases.forEach { phase ->
                BusinessAppStateSpecs.spec(screen, phase).rows.forEach { row ->
                    assertTrue(
                        "$screen/$phase row '${row.label}' must not render a zero placeholder",
                        row.value.trim() != "0",
                    )
                    assertTrue(row.label.isNotBlank())
                    assertTrue(row.value.isNotBlank())
                }
            }
        }
    }

    @Test
    fun warningToneMarksUnrecordedFigures() {
        val memory = BusinessAppStateSpecs.spec(BusinessAppStateScreen.Memory, BusinessAppStatePhase.Empty)
        val warningRows = memory.rows.filter { it.tone == ValueTone.Warning }
        assertTrue("Memory empty should mark unrecorded figures", warningRows.isNotEmpty())
        assertTrue(warningRows.any { it.value == "Not recorded" })

        val offline = BusinessAppStateSpecs.spec(BusinessAppStateScreen.Offline, BusinessAppStatePhase.Error)
        assertTrue(offline.rows.any { it.value == "Hidden until online" && it.tone == ValueTone.Warning })
    }

    @Test
    fun memoryAndLifeEyebrowsAreDerivedFromTheDate() {
        val now = GregorianCalendar(2026, Calendar.OCTOBER, 31).time
        assertEquals("OCTOBER 2026", BusinessAppStateSpecs.monthLabel(now))
        assertEquals("TODAY · 31 OCT", BusinessAppStateSpecs.todayLabel(now))

        val memory = BusinessAppStateSpecs.spec(BusinessAppStateScreen.Memory, BusinessAppStatePhase.Empty, now)
        assertEquals("OCTOBER 2026", memory.eyebrow)

        val life = BusinessAppStateSpecs.spec(BusinessAppStateScreen.Life, BusinessAppStatePhase.Empty, now)
        assertEquals("TODAY · 31 OCT", life.eyebrow)
    }

    @Test
    fun loadingVariantsAreLabelledPerScreen() {
        assertEquals(
            "Loading Pulse…",
            BusinessAppStateSpecs.spec(BusinessAppStateScreen.Pulse, BusinessAppStatePhase.Loading).title,
        )
        assertEquals(
            "Loading Moments…",
            BusinessAppStateSpecs.spec(BusinessAppStateScreen.Moments, BusinessAppStatePhase.Loading).title,
        )
        assertEquals(
            "Loading Life…",
            BusinessAppStateSpecs.spec(BusinessAppStateScreen.Life, BusinessAppStatePhase.Loading).title,
        )
        assertEquals(
            "Loading Memory…",
            BusinessAppStateSpecs.spec(BusinessAppStateScreen.Memory, BusinessAppStatePhase.Loading).title,
        )
    }

    @Test
    fun accentMappingIsStablePerVertical() {
        assertEquals(BusinessAppStateAccent.Trading, BusinessAppStateAccent.forTypeCode("TRADING_WHOLESALE"))
        assertEquals(BusinessAppStateAccent.Retail, BusinessAppStateAccent.forTypeCode("RETAIL_STORE"))
        assertEquals(BusinessAppStateAccent.Rst, BusinessAppStateAccent.forTypeCode("RESTAURANT_FOOD"))
        assertEquals(BusinessAppStateAccent.Services, BusinessAppStateAccent.forTypeCode("CONSUMER_SERVICE"))
        assertEquals(BusinessAppStateAccent.Mfg, BusinessAppStateAccent.forTypeCode("STANDARD_PRODUCTION"))
        assertEquals(BusinessAppStateAccent.Marketing, BusinessAppStateAccent.forTypeCode("CLIENT_CAMPAIGN"))
        assertEquals(BusinessAppStateAccent.Marketing, BusinessAppStateAccent.forTypeCode("OWN_BUSINESS_CAMPAIGN"))
        assertEquals(BusinessAppStateAccent.Distribution, BusinessAppStateAccent.forTypeCode("DISTRIBUTION_DEALERSHIP"))
        assertEquals(BusinessAppStateAccent.Trading, BusinessAppStateAccent.forTypeCode(null))
        assertEquals(BusinessAppStateAccent.Trading, BusinessAppStateAccent.forTypeCode("SOMETHING_NEW"))
    }

    @Test
    fun figmaAccentValuesAreExact() {
        assertEquals(0xFF10B981.toInt(), BusinessAppStateAccent.Trading.color.toArgb())
        assertEquals(0xFFF43F5E.toInt(), BusinessAppStateAccent.Retail.color.toArgb())
        assertEquals(0xFFEA580C.toInt(), BusinessAppStateAccent.Rst.color.toArgb())
        assertEquals(0xFF84CC16.toInt(), BusinessAppStateAccent.Services.color.toArgb())
        assertEquals(0xFFF59E0B.toInt(), BusinessAppStateAccent.Mfg.color.toArgb())
        assertEquals(0xFF6366F1.toInt(), BusinessAppStateAccent.Marketing.color.toArgb())
        assertEquals(0xFFEA580C.toInt(), BusinessAppStateAccent.Distribution.color.toArgb())
        BusinessAppStateAccent.entries.forEach { accent ->
            assertEquals("${accent.name} must be fully opaque", 1f, accent.color.alpha)
        }
    }
}

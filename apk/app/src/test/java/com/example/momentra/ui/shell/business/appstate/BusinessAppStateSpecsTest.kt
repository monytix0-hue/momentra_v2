package com.example.momentra.ui.shell.business.appstate

import androidx.compose.ui.graphics.toArgb
import com.example.momentra.ui.shell.momentswitcher.businessDirectoryBucketFor
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
 *
 * Mirrors `BusinessAppStateSpecsTests` on iOS.
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
        // Figma's Offline variant has no error-code pill: it leads with the emerald-stroked offline
        // pill and a single Retry, and hides live figures rather than reporting a failure code.
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
        val defaults = BusinessAppStateSpecs.hubTileLabels(BusinessAppStateVertical.Trading).map { it.second }
        val services = BusinessAppStateSpecs.hubTileLabels(BusinessAppStateVertical.Services).map { it.second }
        val marketing = BusinessAppStateSpecs.hubTileLabels(BusinessAppStateVertical.Marketing).map { it.second }

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
    fun hubTilesAreTheSixLucideCaptureGlyphs() {
        // The glyph set is fixed and identical across verticals; only the copy overrides relabel it.
        val expected = listOf("camera", "receipt", "clipboard-list", "banknote", "calculator", "package")
        BusinessAppStateVertical.entries.forEach { vertical ->
            val tiles = BusinessAppStateSpecs.hubTileLabels(vertical)
            assertEquals(6, tiles.size)
            assertEquals("$vertical glyph set must match Figma", expected, tiles.map { it.first })
        }
    }

    @Test
    fun hubGridMatchesTheFigmaMetrics() {
        // Figma: 3 * 111 + 2 * 10 = 353, which fits inside the kit's 354 body width.
        val three = BusinessAppStateTokens.HubTileWidth * 3 + BusinessAppStateTokens.HubColumnGap * 2
        assertEquals(353f, three.value)
        assertTrue(three <= BusinessAppStateTokens.BodyWidth)
        assertEquals(67f, BusinessAppStateTokens.HubTileHeight.value)
        assertEquals(10f, BusinessAppStateTokens.HubRowGap.value)
        assertEquals(18f, BusinessAppStateTokens.HubIconSize.value)
        assertEquals(16f, BusinessAppStateTokens.HubTileRadius.value)
        assertEquals(11.5f, BusinessAppStateTokens.HubLabelSize)
        assertEquals(
            BusinessAppStateTokens.Emerald.copy(alpha = 0.25f).toArgb(),
            BusinessAppStateTokens.HubTileStroke.toArgb(),
        )
        assertEquals(0x0DFFFFFF.toInt(), BusinessAppStateTokens.HubTileFill.toArgb())
    }

    @Test
    fun hubCarriesTheSolidFootnote() {
        // Every tile is equal weight in the Obsidian frame: the amber badge and the 60% muting
        // are gone, and the footnote renders solid `#738099`.
        assertNotNull(BusinessAppStateSpecs.spec(BusinessAppStateScreen.Hub, BusinessAppStatePhase.Empty).footnote)
        assertEquals(0xFF738099.toInt(), BusinessAppStateTokens.TextSubtle.toArgb())
        assertEquals(1f, BusinessAppStateTokens.FootnoteMutedOpacity)
        assertEquals(0xFF10B981.toInt(), BusinessAppStateAccent.Emerald.color.toArgb())
    }

    @Test
    fun pageAndCtaMatchTheFigmaFrame() {
        // Figma root background and the `0 4px 12px rgba(16,185,129,0.35)` CTA shadow.
        assertEquals(0xFF05070B.toInt(), BusinessAppStateTokens.Page.toArgb())
        assertEquals(0xFF059669.toInt(), BusinessAppStateTokens.CtaStart.toArgb())
        assertEquals(0xFF34D399.toInt(), BusinessAppStateTokens.CtaEnd.toArgb())
        assertEquals(0xFF10B981.toInt(), BusinessAppStateTokens.CtaShadow.toArgb())
        assertEquals(12f, BusinessAppStateTokens.CtaShadowRadius.value)
        assertEquals(4f, BusinessAppStateTokens.CtaShadowY.value)
    }

    // Obsidian & neon refraction

    @Test
    fun glassCardMatchesTheRefractionSpec() {
        // Flat translucent fill — the Figma effect is the glass over the glow, not a sheen.
        assertEquals(0xB8101623.toInt(), BusinessAppStateTokens.GlassCardFill.toArgb())
        assertEquals(
            BusinessAppStateTokens.Emerald.copy(alpha = 0.35f).toArgb(),
            BusinessAppStateTokens.GlassCardStroke.toArgb(),
        )
        assertEquals(1.25f, BusinessAppStateTokens.GlassCardStrokeWidth.value)
        assertEquals(22f, BusinessAppStateTokens.CardRadius.value)
        assertEquals(20f, BusinessAppStateTokens.CardPaddingHorizontal.value)
        assertEquals(18f, BusinessAppStateTokens.CardPaddingVertical.value)
        assertEquals(12f, BusinessAppStateTokens.CardRowGap.value)
        assertEquals(46f, BusinessAppStateTokens.SecondaryButtonHeight.value)
        assertEquals(50f, BusinessAppStateTokens.PrimaryButtonHeight.value)
    }

    @Test
    fun glowMeshCarriesTheTwoOrbRamp() {
        assertEquals(0xFF10B981.toInt(), BusinessAppStateTokens.GlowAccent.toArgb())
        assertEquals(360f, BusinessAppStateTokens.GlowAccentSize.value)
        assertEquals(0.28f, BusinessAppStateTokens.GlowAccentAlpha)
        assertEquals(0.08f, BusinessAppStateTokens.GlowAccentMidAlpha)
        assertEquals(0.60f, BusinessAppStateTokens.GlowAccentMidStop)
        assertEquals(0xFF059669.toInt(), BusinessAppStateTokens.GlowWarm.toArgb())
        assertEquals(280f, BusinessAppStateTokens.GlowWarmSize.value)
        assertEquals(0.15f, BusinessAppStateTokens.GlowWarmAlpha)
        assertEquals(0xFFFF5C38.toInt(), BusinessAppStateTokens.GlowError.toArgb())
        assertEquals(0.25f, BusinessAppStateTokens.GlowErrorAlpha)
    }

    @Test
    fun heroBadgeUsesTheFloatingIndicatorGeometry() {
        assertEquals(112f, BusinessAppStateTokens.HeroSize.value)
        assertEquals(96f, BusinessAppStateTokens.HeroRingSize.value)
        assertEquals(8f, BusinessAppStateTokens.HeroRingInset.value)
        assertEquals(2f, BusinessAppStateTokens.HeroRingStroke.value)
        assertEquals(0.08f, BusinessAppStateTokens.HeroRingFillAlpha)
        assertEquals(0.45f, BusinessAppStateTokens.HeroRingStrokeAlpha)
        assertEquals(76f, BusinessAppStateTokens.HeroCoreSize.value)
        assertEquals(18f, BusinessAppStateTokens.HeroCoreInset.value)
        assertEquals(1.5f, BusinessAppStateTokens.HeroCoreStroke.value)
        assertEquals(0xFF0F141F.toInt(), BusinessAppStateTokens.HeroCoreFill.toArgb())
        assertEquals(0.18f, BusinessAppStateTokens.HeroCoreStrokeAlpha)
        assertEquals(36f, BusinessAppStateTokens.HeroDotSize.value)
        assertEquals(72f, BusinessAppStateTokens.HeroDotInset.value)
        assertEquals(3.5f, BusinessAppStateTokens.HeroDotStroke.value)
        assertEquals(28f, BusinessAppStateTokens.HeroGlyphSize.value)
        assertEquals(17f, BusinessAppStateTokens.HeroDotGlyphSize.value)
        assertEquals(0xFF080A0F.toInt(), BusinessAppStateTokens.HeroDotGlyphColor.toArgb())
    }

    @Test
    fun pillsMatchTheObsidianSpec() {
        assertEquals(
            BusinessAppStateTokens.ErrorRing.copy(alpha = 0.15f).toArgb(),
            BusinessAppStateTokens.ErrorPillFill.toArgb(),
        )
        assertEquals(
            BusinessAppStateTokens.Emerald.copy(alpha = 0.45f).toArgb(),
            BusinessAppStateTokens.ErrorPillStroke.toArgb(),
        )
        assertEquals(0xFF262E3D.toInt(), BusinessAppStateTokens.OfflineBannerFill.copy(alpha = 1f).toArgb())
        assertEquals(
            BusinessAppStateTokens.Emerald.copy(alpha = 0.40f).toArgb(),
            BusinessAppStateTokens.OfflineBannerStroke.toArgb(),
        )
        assertEquals(34f, BusinessAppStateTokens.OfflineBannerHeight.value)
        assertEquals(11f, BusinessAppStateTokens.ErrorCodeSize)
        assertEquals(1.5f, BusinessAppStateTokens.ErrorCodeTracking)
    }

    @Test
    fun typeRampMatchesTheFigmaSpec() {
        assertEquals(24f, BusinessAppStateTokens.TitleSize)
        assertEquals(13.5f, BusinessAppStateTokens.BodySize)
        assertEquals(11f, BusinessAppStateTokens.EyebrowSize)
        assertEquals(1.5f, BusinessAppStateTokens.EyebrowTracking)
        assertEquals(13f, BusinessAppStateTokens.RowLabelSize)
        assertEquals(13f, BusinessAppStateTokens.RowValueSize)
        assertEquals(13.5f, BusinessAppStateTokens.ButtonLabelSize)
        assertEquals(11f, BusinessAppStateTokens.FootnoteSize)
        assertEquals(0xFF94A4B8.toInt(), BusinessAppStateTokens.TextMuted.toArgb())
        assertEquals(0xFFC7D1E0.toInt(), BusinessAppStateTokens.RowLabelColor.toArgb())
        assertEquals(0xFFF5A624.toInt(), BusinessAppStateTokens.Warning.toArgb())
    }

    @Test
    fun everyEmptyScreenExposesAFullStateBody() {
        listOf(
            BusinessAppStateScreen.Pulse,
            BusinessAppStateScreen.Moments,
            BusinessAppStateScreen.Life,
            BusinessAppStateScreen.Memory,
        ).forEach { screen ->
            val spec = BusinessAppStateSpecs.spec(screen, BusinessAppStatePhase.Empty)
            assertEquals(screen, spec.screen)
            assertTrue("$screen empty state needs a title", spec.title.isNotBlank())
            assertNotNull("$screen empty state lost its primary CTA", spec.primaryLabel)
        }
        val hub = BusinessAppStateSpecs.spec(BusinessAppStateScreen.Hub, BusinessAppStatePhase.Empty)
        assertTrue(hub.title.isNotBlank())
        assertNotNull(hub.footnote)
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
    fun loadingSkeletonIsASingleGlassBox() {
        assertEquals(150f, BusinessAppStateTokens.SkeletonBoxHeight.value)
        assertEquals(20f, BusinessAppStateTokens.SkeletonPadHorizontal.value)
        assertEquals(22f, BusinessAppStateTokens.SkeletonPadVertical.value)
        assertEquals(14f, BusinessAppStateTokens.SkeletonGap.value)
        assertEquals(16f, BusinessAppStateTokens.SkeletonBarHeight.value)
        assertEquals(8f, BusinessAppStateTokens.SkeletonBarRadius.value)
        assertEquals(110f, BusinessAppStateTokens.SkeletonTrailingSpace.value)
    }

    @Test
    fun accentMappingIsUnifiedAcrossEveryVertical() {
        // The accent is unified across every Business vertical, so every code resolves to Emerald.
        // Per-vertical *copy* is keyed on the vertical instead — see `verticalMappingIsStablePerVertical`.
        assertEquals(BusinessAppStateAccent.Emerald, BusinessAppStateAccent.forTypeCode("TRADING_WHOLESALE"))
        assertEquals(BusinessAppStateAccent.Emerald, BusinessAppStateAccent.forTypeCode("RETAIL_STORE"))
        assertEquals(BusinessAppStateAccent.Emerald, BusinessAppStateAccent.forTypeCode("RESTAURANT_FOOD"))
        assertEquals(BusinessAppStateAccent.Emerald, BusinessAppStateAccent.forTypeCode("CONSUMER_SERVICE"))
        assertEquals(BusinessAppStateAccent.Emerald, BusinessAppStateAccent.forTypeCode("STANDARD_PRODUCTION"))
        assertEquals(BusinessAppStateAccent.Emerald, BusinessAppStateAccent.forTypeCode("MADE_TO_ORDER"))
        assertEquals(BusinessAppStateAccent.Emerald, BusinessAppStateAccent.forTypeCode("CLIENT_CAMPAIGN"))
        assertEquals(BusinessAppStateAccent.Emerald, BusinessAppStateAccent.forTypeCode("OWN_BUSINESS_CAMPAIGN"))
        assertEquals(BusinessAppStateAccent.Emerald, BusinessAppStateAccent.forTypeCode("DISTRIBUTION_DEALERSHIP"))
        assertEquals(BusinessAppStateAccent.Emerald, BusinessAppStateAccent.forTypeCode(null))
        assertEquals(BusinessAppStateAccent.Emerald, BusinessAppStateAccent.forTypeCode("SOMETHING_NEW"))
    }

    @Test
    fun verticalMappingIsStablePerVertical() {
        assertEquals(BusinessAppStateVertical.Trading, BusinessAppStateVertical.forTypeCode("TRADING_WHOLESALE"))
        assertEquals(BusinessAppStateVertical.Retail, BusinessAppStateVertical.forTypeCode("RETAIL_STORE"))
        assertEquals(BusinessAppStateVertical.Rst, BusinessAppStateVertical.forTypeCode("RESTAURANT_FOOD"))
        assertEquals(BusinessAppStateVertical.Services, BusinessAppStateVertical.forTypeCode("CONSUMER_SERVICE"))
        assertEquals(BusinessAppStateVertical.Mfg, BusinessAppStateVertical.forTypeCode("STANDARD_PRODUCTION"))
        assertEquals(BusinessAppStateVertical.Mfg, BusinessAppStateVertical.forTypeCode("MADE_TO_ORDER"))
        assertEquals(BusinessAppStateVertical.Marketing, BusinessAppStateVertical.forTypeCode("CLIENT_CAMPAIGN"))
        assertEquals(BusinessAppStateVertical.Marketing, BusinessAppStateVertical.forTypeCode("OWN_BUSINESS_CAMPAIGN"))
        assertEquals(BusinessAppStateVertical.Distribution, BusinessAppStateVertical.forTypeCode("DISTRIBUTION_DEALERSHIP"))
        assertEquals(BusinessAppStateVertical.Trading, BusinessAppStateVertical.forTypeCode(null))
        assertEquals(BusinessAppStateVertical.Trading, BusinessAppStateVertical.forTypeCode("SOMETHING_NEW"))
    }

    @Test
    fun figmaAccentValuesAreExact() {
        // The kit's single accent variable — `appstate/accent` — is emerald 500.
        assertEquals(listOf(BusinessAppStateAccent.Emerald), BusinessAppStateAccent.entries.toList())
        assertEquals(0xFF10B981.toInt(), BusinessAppStateAccent.Emerald.color.toArgb())
        BusinessAppStateAccent.entries.forEach { accent ->
            assertEquals("${accent.name} must be fully opaque", 1f, accent.color.alpha)
        }
    }

    @Test
    fun directoryBucketsCarryTheVerticalAndTheUnifiedAccent() {
        assertEquals("retail", businessDirectoryBucketFor("RETAIL_STORE").id)
        assertEquals("trading", businessDirectoryBucketFor("TRADING_WHOLESALE").id)
        assertEquals("food", businessDirectoryBucketFor("RESTAURANT_FOOD").id)
        assertEquals("service", businessDirectoryBucketFor("CONSUMER_SERVICE").id)
        assertEquals("manufacturing", businessDirectoryBucketFor("MADE_TO_ORDER").id)
        assertEquals("marketing", businessDirectoryBucketFor("OWN_BUSINESS_CAMPAIGN").id)
        assertEquals("distribution", businessDirectoryBucketFor("DISTRIBUTION_DEALERSHIP").id)
        assertEquals("trading", businessDirectoryBucketFor(null).id)
        assertEquals("trading", businessDirectoryBucketFor("SOMETHING_NEW").id)

        assertEquals(BusinessAppStateVertical.Retail, businessDirectoryBucketFor("RETAIL_STORE").vertical)
        assertEquals(BusinessAppStateVertical.Services, businessDirectoryBucketFor("CONSUMER_SERVICE").vertical)
        assertEquals(BusinessAppStateVertical.Marketing, businessDirectoryBucketFor("OWN_BUSINESS_CAMPAIGN").vertical)
        assertEquals(BusinessAppStateVertical.Distribution, businessDirectoryBucketFor("DISTRIBUTION_DEALERSHIP").vertical)
        assertEquals(BusinessAppStateVertical.Mfg, businessDirectoryBucketFor("MADE_TO_ORDER").vertical)
        assertEquals(BusinessAppStateVertical.Rst, businessDirectoryBucketFor("RESTAURANT_FOOD").vertical)
        assertEquals(BusinessAppStateVertical.Trading, businessDirectoryBucketFor("TRADING_WHOLESALE").vertical)

        // Every bucket renders with the same emerald accent, regardless of vertical.
        listOf(
            "RETAIL_STORE", "TRADING_WHOLESALE", "RESTAURANT_FOOD", "CONSUMER_SERVICE",
            "MADE_TO_ORDER", "OWN_BUSINESS_CAMPAIGN", "DISTRIBUTION_DEALERSHIP",
        ).forEach { code ->
            assertEquals(
                "$code bucket must use the unified emerald accent",
                BusinessAppStateAccent.Emerald,
                businessDirectoryBucketFor(code).accent,
            )
        }
    }

    // MARK: Directory switcher row (Figma 2871:39707)

    @Test
    fun directorySwitcherRowUsesTheObsidianCardGeometry() {
        // Radius 16, `rgba(16,22,35,0.95)` when selected and `0.65` otherwise, emerald strokes.
        assertEquals(16f, BusinessAppStateTokens.HubTileRadius.value)
        assertEquals(0xB8101623.toInt(), BusinessAppStateTokens.GlassCardFill.toArgb())
        // Unselected switcher rows and chips use the emerald 18% hairline.
        assertEquals(0x2E10B981.toInt(), BusinessAppStateTokens.HairlineStroke.toArgb())
        assertEquals(0xFFC7D1E0.toInt(), BusinessAppStateTokens.RowLabelColor.toArgb())
    }
}

import Foundation
import SwiftUI
import Testing
@testable import momentra

/// Mirrors `BusinessAppStateSpecsTest` on Android — the two kits must not drift.
struct BusinessAppStateSpecsTests {

    // MARK: Coverage

    @Test func everyScreenPhaseResolvesWithoutCrashing() {
        for screen in BusinessAppStateScreen.allCases {
            for phase in [BusinessAppStatePhase.empty, .loading, .error, .setup] {
                let spec = BusinessAppStateSpecs.spec(screen, phase)
                #expect(spec.screen == screen)
                #expect(!spec.title.isEmpty, "\(screen) \(phase) has no title")
            }
        }
    }

    // MARK: Missing is never zero

    @Test func noRowEverRendersZero() {
        for screen in BusinessAppStateScreen.allCases {
            for phase in [BusinessAppStatePhase.empty, .loading, .error, .setup] {
                for row in BusinessAppStateSpecs.spec(screen, phase).rows {
                    #expect(row.value.trimmingCharacters(in: .whitespaces) != "0")
                    #expect(row.value.trimmingCharacters(in: .whitespaces) != "₹0")
                    #expect(row.value.trimmingCharacters(in: .whitespaces).isEmpty == false)
                }
            }
        }
    }

    @Test func warningToneIsUsedForMissingFigures() {
        let pulse = BusinessAppStateSpecs.spec(.pulse, .empty)
        let warnings = pulse.rows.filter { $0.tone == .warning }.map(\.value)
        #expect(warnings.contains("Not recorded"))

        let memory = BusinessAppStateSpecs.spec(.memory, .empty)
        #expect(memory.rows.contains { $0.value == "None yet" && $0.tone == .warning })

        let offline = BusinessAppStateSpecs.spec(.offline, .error)
        #expect(offline.rows.contains { $0.value == "Hidden until online" && $0.tone == .warning })
    }

    // MARK: Error codes

    @Test func errorStatesCarryDiagnosticCodes() {
        #expect(BusinessAppStateSpecs.spec(.pulse, .error).errorCode == "PULSE_FETCH_FAILED")
        #expect(BusinessAppStateSpecs.spec(.moments, .error).errorCode == "MOMENT_DETAIL_FAILED")
        #expect(BusinessAppStateSpecs.spec(.life, .error).errorCode == "LIFE_FEED_FAILED")
        #expect(BusinessAppStateSpecs.spec(.memory, .error).errorCode == "MEMORY_FETCH_FAILED")
        #expect(BusinessAppStateSpecs.spec(.hub, .error).errorCode == "HUB_FETCH_FAILED")
        // Offline leads with the status pill instead of a code pill.
        #expect(BusinessAppStateSpecs.spec(.offline, .error).errorCode == nil)
    }

    @Test func loadingStatesHaveNoActionsOrRows() {
        for screen in [BusinessAppStateScreen.pulse, .moments, .life, .memory] {
            let spec = BusinessAppStateSpecs.spec(screen, .loading)
            #expect(spec.rows.isEmpty)
            #expect(spec.primaryLabel == nil)
            #expect(spec.title.hasPrefix("Loading"))
        }
    }

    // MARK: Unified emerald accent

    @Test func everyMomentTypeCodeResolvesToEmerald() {
        // The accent is unified across every Business vertical, so emerald is the only accent the
        // kit knows about. Per-vertical *copy* is keyed on the vertical instead.
        #expect(BusinessAppStateAccent.forTypeCode("TRADING_WHOLESALE") == .emerald)
        #expect(BusinessAppStateAccent.forTypeCode("RETAIL_STORE") == .emerald)
        #expect(BusinessAppStateAccent.forTypeCode("RESTAURANT_FOOD") == .emerald)
        #expect(BusinessAppStateAccent.forTypeCode("CONSUMER_SERVICE") == .emerald)
        #expect(BusinessAppStateAccent.forTypeCode("STANDARD_PRODUCTION") == .emerald)
        #expect(BusinessAppStateAccent.forTypeCode("MADE_TO_ORDER") == .emerald)
        #expect(BusinessAppStateAccent.forTypeCode("CLIENT_CAMPAIGN") == .emerald)
        #expect(BusinessAppStateAccent.forTypeCode("OWN_BUSINESS_CAMPAIGN") == .emerald)
        #expect(BusinessAppStateAccent.forTypeCode("DISTRIBUTION_DEALERSHIP") == .emerald)
        #expect(BusinessAppStateAccent.forTypeCode(nil) == .emerald)
        #expect(BusinessAppStateAccent.forTypeCode("  retail_store  ") == .emerald)
    }

    @Test func accentIsTheUnifiedEmeraldValue() {
        #expect(BusinessAppStateAccent.allCases == [.emerald])
        #expect(BusinessAppStateAccent.emerald.color == Color(hex: "#10B981"))
    }

    @Test func verticalStillResolvesPerMomentTypeCode() {
        #expect(BusinessAppStateVertical.forTypeCode("TRADING_WHOLESALE") == .trading)
        #expect(BusinessAppStateVertical.forTypeCode("RETAIL_STORE") == .retail)
        #expect(BusinessAppStateVertical.forTypeCode("RESTAURANT_FOOD") == .rst)
        #expect(BusinessAppStateVertical.forTypeCode("CONSUMER_SERVICE") == .services)
        #expect(BusinessAppStateVertical.forTypeCode("STANDARD_PRODUCTION") == .mfg)
        #expect(BusinessAppStateVertical.forTypeCode("MADE_TO_ORDER") == .mfg)
        #expect(BusinessAppStateVertical.forTypeCode("CLIENT_CAMPAIGN") == .marketing)
        #expect(BusinessAppStateVertical.forTypeCode("OWN_BUSINESS_CAMPAIGN") == .marketing)
        #expect(BusinessAppStateVertical.forTypeCode("DISTRIBUTION_DEALERSHIP") == .distribution)
        #expect(BusinessAppStateVertical.forTypeCode(nil) == .trading)
        #expect(BusinessAppStateVertical.forTypeCode("  retail_store  ") == .retail)
    }

    // MARK: Hub label overrides

    @Test func hubTilesUseVerticalOverrides() {
        #expect(BusinessAppStateSpecs.hubTileLabels(vertical: .services).map(\.label).contains("Invoice"))
        #expect(BusinessAppStateSpecs.hubTileLabels(vertical: .services).map(\.label).contains("Appointment"))
        #expect(BusinessAppStateSpecs.hubTileLabels(vertical: .marketing).map(\.label).contains("Client invoice"))
        #expect(BusinessAppStateSpecs.hubTileLabels(vertical: .marketing).map(\.label).contains("Campaign"))
        #expect(BusinessAppStateSpecs.hubTileLabels(vertical: .trading).map(\.label) == [
            "Sale", "Purchase", "Collection", "Payment", "Expense", "Stock",
        ])
        #expect(BusinessAppStateSpecs.hubTileLabels(vertical: .retail).count == 6)
    }

    @Test func hubTilesDifferBetweenServicesAndMarketing() {
        let services = BusinessAppStateSpecs.hubTileLabels(vertical: .services).map(\.label)
        let marketing = BusinessAppStateSpecs.hubTileLabels(vertical: .marketing).map(\.label)
        #expect(services != marketing)
        #expect(services.first == "Invoice")
        #expect(marketing.first == "Client invoice")
    }

    // MARK: Hub grid (Figma 2666:31852)

    @Test func hubTilesAreTheSixLucideCaptureGlyphs() {
        // The glyph set is fixed and identical across verticals; only the copy overrides relabel it.
        let expected: [BusinessIcon] = [.camera, .receipt, .clipboardList, .banknote, .calculator, .package]
        for vertical in BusinessAppStateVertical.allCases {
            let tiles = BusinessAppStateSpecs.hubTileLabels(vertical: vertical)
            #expect(tiles.count == 6)
            #expect(tiles.map(\.icon) == expected)
        }
        // Every glyph resolves to a bundled BusinessIcons asset.
        for icon in expected {
            #expect(icon.assetName == "BusinessIcons/" + icon.rawValue)
        }
    }

    @Test func hubTilesSplitIntoTwoRowsOfThreeAcrossTheBodyWidth() {
        // Figma: 3 * 111 + 2 * 10 = 353 inside the 354 body width.
        let three = 3 * BusinessAppStateTokens.hubTileWidth + 2 * BusinessAppStateTokens.hubColumnGap
        #expect(three == 353)
        #expect(three <= BusinessAppStateTokens.bodyWidth)
        #expect(BusinessAppStateTokens.hubTileHeight == 67)
        #expect(BusinessAppStateTokens.hubRowGap == 10)
        #expect(BusinessAppStateTokens.hubIconSize == 18)
        #expect(BusinessAppStateTokens.hubTileRadius == 16)
        #expect(BusinessAppStateTokens.hubLabelSize == 11.5)
        #expect(BusinessAppStateTokens.hubTileStroke == BusinessAppStateTokens.emerald.opacity(0.25))
    }

    @Test func hubOnlyScreenCarriesTheMutedFootnote() {
        // Every tile is equal weight in the Obsidian frame; the amber badge and the 60% muting
        // are gone, and the footnote renders solid `#738099`.
        #expect(BusinessAppStateSpecs.spec(.hub, .empty).footnote != nil)
        #expect(BusinessAppStateTokens.textSubtle == Color(hex: "#738099"))
        #expect(BusinessAppStateTokens.footnoteMutedOpacity == 1)
        #expect(BusinessAppStateAccent.emerald.color == Color(hex: "#10B981"))
    }

    @Test func pageAndCtaMatchTheFigmaFrame() {
        // Figma root background and the `0 4px 12px rgba(16,185,129,0.35)` CTA shadow.
        #expect(BusinessAppStateTokens.page == Color(hex: "#05070B"))
        #expect(BusinessAppStateTokens.ctaStart == Color(hex: "#059669"))
        #expect(BusinessAppStateTokens.ctaEnd == Color(hex: "#34D399"))
        #expect(BusinessAppStateTokens.ctaShadow == BusinessAppStateTokens.emerald.opacity(0.35))
        #expect(BusinessAppStateTokens.ctaShadowRadius == 12)
        #expect(BusinessAppStateTokens.ctaShadowY == 4)
    }

    // MARK: Obsidian & neon refraction

    @Test func glassCardMatchesTheRefractionSpec() {
        // Flat translucent fill — the Figma effect is the glass over the glow, not a sheen.
        #expect(
            BusinessAppStateTokens.glassCardFill
                == Color(red: 16.0 / 255, green: 22.0 / 255, blue: 35.0 / 255).opacity(0.72)
        )
        #expect(BusinessAppStateTokens.glassCardStroke == BusinessAppStateTokens.emerald.opacity(0.35))
        #expect(BusinessAppStateTokens.glassCardStrokeWidth == 1.25)
        #expect(BusinessAppStateTokens.cardRadius == 22)
        #expect(BusinessAppStateTokens.cardPaddingHorizontal == 20)
        #expect(BusinessAppStateTokens.cardPaddingVertical == 18)
        #expect(BusinessAppStateTokens.cardRowGap == 12)
        #expect(BusinessAppStateTokens.secondaryButtonHeight == 46)
        #expect(BusinessAppStateTokens.primaryButtonHeight == 50)
    }

    @Test func glowMeshCarriesTheTwoOrbRamp() {
        #expect(BusinessAppStateTokens.glowAccent == Color(hex: "#10B981"))
        #expect(BusinessAppStateTokens.glowAccentSize == 360)
        #expect(BusinessAppStateTokens.glowAccentAlpha == 0.28)
        #expect(BusinessAppStateTokens.glowAccentMidAlpha == 0.08)
        #expect(BusinessAppStateTokens.glowAccentMidStop == 0.60)
        #expect(BusinessAppStateTokens.glowWarm == Color(hex: "#059669"))
        #expect(BusinessAppStateTokens.glowWarmSize == 280)
        #expect(BusinessAppStateTokens.glowWarmAlpha == 0.15)
        #expect(BusinessAppStateTokens.glowError == Color(hex: "#FF5C38"))
        #expect(BusinessAppStateTokens.glowErrorAlpha == 0.25)
    }

    @Test func heroBadgeUsesTheFloatingIndicatorGeometry() {
        #expect(BusinessAppStateTokens.heroSize == 112)
        #expect(BusinessAppStateTokens.heroRingSize == 96)
        #expect(BusinessAppStateTokens.heroRingInset == 8)
        #expect(BusinessAppStateTokens.heroRingStroke == 2)
        #expect(BusinessAppStateTokens.heroRingFillAlpha == 0.08)
        #expect(BusinessAppStateTokens.heroRingStrokeAlpha == 0.45)
        #expect(BusinessAppStateTokens.heroCoreSize == 76)
        #expect(BusinessAppStateTokens.heroCoreInset == 18)
        #expect(BusinessAppStateTokens.heroCoreStroke == 1.5)
        #expect(BusinessAppStateTokens.heroCoreFill == Color(hex: "#0F141F"))
        #expect(BusinessAppStateTokens.heroCoreStrokeAlpha == 0.18)
        #expect(BusinessAppStateTokens.heroDotSize == 36)
        #expect(BusinessAppStateTokens.heroDotInset == 72)
        #expect(BusinessAppStateTokens.heroDotStroke == 3.5)
        #expect(BusinessAppStateTokens.heroGlyphSize == 28)
        #expect(BusinessAppStateTokens.heroDotGlyphSize == 17)
        #expect(BusinessAppStateTokens.heroDotGlyphColor == Color(hex: "#080A0F"))
    }

    @Test func pillsMatchTheObsidianSpec() {
        #expect(BusinessAppStateTokens.errorPillFill == BusinessAppStateTokens.errorRing.opacity(0.15))
        #expect(BusinessAppStateTokens.errorPillStroke == BusinessAppStateTokens.emerald.opacity(0.45))
        #expect(BusinessAppStateTokens.offlineBannerHeight == 34)
        #expect(BusinessAppStateTokens.errorCodeSize == 11)
        #expect(BusinessAppStateTokens.errorCodeTracking == 1.5)
    }

    @Test func typeRampMatchesTheFigmaSpec() {
        #expect(BusinessAppStateTokens.titleSize == 24)
        #expect(BusinessAppStateTokens.bodySize == 13.5)
        #expect(BusinessAppStateTokens.eyebrowSize == 11)
        #expect(BusinessAppStateTokens.eyebrowTracking == 1.5)
        #expect(BusinessAppStateTokens.rowLabelSize == 13)
        #expect(BusinessAppStateTokens.rowValueSize == 13)
        #expect(BusinessAppStateTokens.buttonLabelSize == 13.5)
        #expect(BusinessAppStateTokens.footnoteSize == 11)
        #expect(BusinessAppStateTokens.textMuted == Color(hex: "#94A4B8"))
        #expect(BusinessAppStateTokens.rowLabelColor == Color(hex: "#C7D1E0"))
        #expect(BusinessAppStateTokens.warning == Color(hex: "#F5A624"))
    }

    @Test func loadingSkeletonIsASingleGlassBox() {
        #expect(BusinessAppStateTokens.skeletonBoxHeight == 150)
        #expect(BusinessAppStateTokens.skeletonPadHorizontal == 20)
        #expect(BusinessAppStateTokens.skeletonPadVertical == 22)
        #expect(BusinessAppStateTokens.skeletonGap == 14)
        #expect(BusinessAppStateTokens.skeletonBarHeight == 16)
        #expect(BusinessAppStateTokens.skeletonBarRadius == 8)
        #expect(BusinessAppStateTokens.skeletonTrailingSpace == 110)
    }

    @Test func everyEmptyScreenExposesAFullStateBody() {
        for screen in [BusinessAppStateScreen.pulse, .moments, .life, .memory] {
            let spec = BusinessAppStateSpecs.spec(screen, .empty)
            #expect(spec.screen == screen)
            #expect(!spec.title.isEmpty)
            #expect(spec.primaryLabel != nil, "\(screen) empty state lost its primary CTA")
        }
        let hub = BusinessAppStateSpecs.spec(.hub, .empty)
        #expect(!hub.title.isEmpty)
        #expect(hub.footnote != nil)
    }

    // MARK: Dynamic labels

    @Test func monthLabelMatchesFigmaCasing() {
        var components = DateComponents()
        components.year = 2026
        components.month = 10
        components.day = 31
        let now = Calendar(identifier: .gregorian).date(from: components)!
        #expect(BusinessAppStateSpecs.monthLabel(now) == "OCTOBER 2026")

        let memory = BusinessAppStateSpecs.spec(.memory, .empty, now: now)
        #expect(memory.eyebrow == "OCTOBER 2026")
        #expect(memory.body.contains("OCTOBER 2026"))
    }

    @Test func todayLabelMatchesFigmaCasing() {
        var components = DateComponents()
        components.year = 2026
        components.month = 10
        components.day = 31
        let now = Calendar(identifier: .gregorian).date(from: components)!
        #expect(BusinessAppStateSpecs.todayLabel(now) == "TODAY · 31 OCT")
        #expect(BusinessAppStateSpecs.spec(.life, .empty, now: now).eyebrow == "TODAY · 31 OCT")
    }

    // MARK: Directory

    @Test func directoryBucketsClassifyEveryVertical() {
        #expect(businessDirectoryBucketFor("RETAIL_STORE").id == "retail")
        #expect(businessDirectoryBucketFor("TRADING_WHOLESALE").id == "trading")
        #expect(businessDirectoryBucketFor("RESTAURANT_FOOD").id == "food")
        #expect(businessDirectoryBucketFor("CONSUMER_SERVICE").id == "service")
        #expect(businessDirectoryBucketFor("MADE_TO_ORDER").id == "manufacturing")
        #expect(businessDirectoryBucketFor("OWN_BUSINESS_CAMPAIGN").id == "marketing")
        #expect(businessDirectoryBucketFor("DISTRIBUTION_DEALERSHIP").id == "distribution")
        // Unknown codes fall back rather than dropping the moment.
        #expect(businessDirectoryBucketFor(nil).id == "trading")
        #expect(businessDirectoryBucketFor("SOMETHING_NEW").id == "trading")
    }

    @Test func directoryBucketsCarryTheVerticalAndTheUnifiedAccent() {
        #expect(businessDirectoryBucketFor("RETAIL_STORE").vertical == .retail)
        #expect(businessDirectoryBucketFor("CONSUMER_SERVICE").vertical == .services)
        #expect(businessDirectoryBucketFor("OWN_BUSINESS_CAMPAIGN").vertical == .marketing)
        #expect(businessDirectoryBucketFor("DISTRIBUTION_DEALERSHIP").vertical == .distribution)
        #expect(businessDirectoryBucketFor("MADE_TO_ORDER").vertical == .mfg)
        #expect(businessDirectoryBucketFor("RESTAURANT_FOOD").vertical == .rst)
        #expect(businessDirectoryBucketFor("TRADING_WHOLESALE").vertical == .trading)

        // Every bucket renders with the same emerald accent, regardless of vertical.
        for code in [
            "RETAIL_STORE", "TRADING_WHOLESALE", "RESTAURANT_FOOD", "CONSUMER_SERVICE",
            "MADE_TO_ORDER", "OWN_BUSINESS_CAMPAIGN", "DISTRIBUTION_DEALERSHIP",
        ] {
            #expect(businessDirectoryBucketFor(code).accent == .emerald)
        }
    }

    // MARK: Directory switcher row (Figma 2871:39707)

    @Test func directorySwitcherRowUsesTheObsidianCardGeometry() {
        // Radius 16, `rgba(16,22,35,0.95)` when selected and `0.65` otherwise, emerald strokes.
        #expect(BusinessAppStateTokens.hubTileRadius == 16)
        #expect(BusinessAppStateTokens.glassCardFill == Color(red: 16.0 / 255, green: 22.0 / 255, blue: 35.0 / 255).opacity(0.72))
        #expect(BusinessAppStateTokens.hairlineStroke == BusinessAppStateTokens.emerald.opacity(0.18))
        #expect(BusinessAppStateTokens.rowLabelColor == Color(hex: "#C7D1E0"))
    }
}

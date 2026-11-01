import Foundation
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
        // Offline uses an amber banner instead of a code pill.
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

    // MARK: Vertical accents

    @Test func accentResolvesPerMomentTypeCode() {
        #expect(BusinessAppStateAccent.forTypeCode("TRADING_WHOLESALE") == .trading)
        #expect(BusinessAppStateAccent.forTypeCode("RETAIL_STORE") == .retail)
        #expect(BusinessAppStateAccent.forTypeCode("RESTAURANT_FOOD") == .rst)
        #expect(BusinessAppStateAccent.forTypeCode("CONSUMER_SERVICE") == .services)
        #expect(BusinessAppStateAccent.forTypeCode("STANDARD_PRODUCTION") == .mfg)
        #expect(BusinessAppStateAccent.forTypeCode("MADE_TO_ORDER") == .mfg)
        #expect(BusinessAppStateAccent.forTypeCode("CLIENT_CAMPAIGN") == .marketing)
        #expect(BusinessAppStateAccent.forTypeCode("DISTRIBUTION_DEALERSHIP") == .distribution)
        #expect(BusinessAppStateAccent.forTypeCode(nil) == .trading)
        #expect(BusinessAppStateAccent.forTypeCode("  retail_store  ") == .retail)
    }

    @Test func hubTilesUseVerticalOverrides() {
        #expect(BusinessAppStateSpecs.hubTileLabels(accent: .services).map(\.label).contains("Invoice"))
        #expect(BusinessAppStateSpecs.hubTileLabels(accent: .marketing).map(\.label).contains("Client invoice"))
        #expect(BusinessAppStateSpecs.hubTileLabels(accent: .trading).map(\.label) == [
            "Sale", "Purchase", "Collection", "Payment", "Expense", "Stock",
        ])
        #expect(BusinessAppStateSpecs.hubTileLabels(accent: .retail).count == 6)
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
}

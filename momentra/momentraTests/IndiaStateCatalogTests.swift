import Foundation
import Testing
@testable import momentra

/// The bundled India States & UTs registry.
///
/// The backend `india-states-parity.test.ts` asserts this table matches the server and the
/// Android copy row for row, so these tests only pin the iOS resolution behaviour: name, code,
/// abbreviation, legacy spelling, jurisdiction, and the GSTIN prefix hint.
struct IndiaStateCatalogTests {

    @Test func carriesAllStatesAndUnionTerritories() {
        #expect(IndiaStateCatalog.all.count == 37)
        #expect(IndiaStateCatalog.states.count == 28)
        #expect(IndiaStateCatalog.unionTerritories.count == 9)
        #expect(IndiaStateCatalog.states.count + IndiaStateCatalog.unionTerritories.count == IndiaStateCatalog.all.count)
    }

    @Test func codesAreUniqueTwoDigitStrings() {
        let codes = IndiaStateCatalog.all.map(\.code)
        #expect(Set(codes).count == codes.count)
        for code in codes {
            #expect(code.range(of: "^\\d{2}$", options: .regularExpression) != nil, "\(code) must be two digits")
        }
    }

    @Test func keepsTheRetiredDamanAndDiuCodeOutOfTheList() {
        // 25 was Daman & Diu before the merger. It resolves forward to 26, never to itself.
        #expect(!IndiaStateCatalog.all.contains { $0.code == "25" })
        #expect(IndiaStateCatalog.resolve("25")?.code == "26")
        #expect(IndiaStateCatalog.codeOf("25") == "26")
        #expect(IndiaStateCatalog.canonical("25") == "Dadra & Nagar Haveli and Daman & Diu")
    }

    @Test func resolvesFullNamesInAnyCasing() {
        #expect(IndiaStateCatalog.codeOf("Maharashtra") == "27")
        #expect(IndiaStateCatalog.codeOf("  maharashtra  ") == "27")
        #expect(IndiaStateCatalog.codeOf("MAHARASHTRA") == "27")
        #expect(IndiaStateCatalog.canonical("karnataka") == "Karnataka")
        #expect(IndiaStateCatalog.canonical("tamil nadu") == "Tamil Nadu")
    }

    @Test func resolvesTwoDigitCodes() {
        #expect(IndiaStateCatalog.canonical("27") == "Maharashtra")
        #expect(IndiaStateCatalog.canonical("30") == "Goa")
        #expect(IndiaStateCatalog.canonical("97") == "Other Territory")
    }

    @Test func resolvesPostalAbbreviationsAndLegacySpellings() {
        #expect(IndiaStateCatalog.codeOf("MH") == "27")
        #expect(IndiaStateCatalog.codeOf("KA") == "29")
        #expect(IndiaStateCatalog.codeOf("J&K") == "01")
        #expect(IndiaStateCatalog.codeOf("Orissa") == "21")
        #expect(IndiaStateCatalog.codeOf("Pondicherry") == "34")
        #expect(IndiaStateCatalog.codeOf("Uttaranchal") == "05")
        #expect(IndiaStateCatalog.codeOf("Bangalore") == "29")
        #expect(IndiaStateCatalog.codeOf("Hyderabad") == "36")
        #expect(IndiaStateCatalog.codeOf("New Delhi") == "07")
    }

    @Test func leavesUnresolvableTextAlone() {
        #expect(IndiaStateCatalog.resolve("") == nil)
        #expect(IndiaStateCatalog.resolve(nil) == nil)
        #expect(IndiaStateCatalog.resolve("Atlantis") == nil)
        #expect(IndiaStateCatalog.codeOf("Atlantis") == "")
        #expect(IndiaStateCatalog.canonical("Atlantis") == "")
        // Display falls back to the raw text so an old row never renders blank.
        #expect(IndiaStateCatalog.displayName("Atlantis") == "Atlantis")
        #expect(IndiaStateCatalog.displayName("mh") == "Maharashtra")
    }

    @Test func jurisdictionComparesCodesNotSpellings() {
        #expect(IndiaStateCatalog.jurisdiction(businessState: "Maharashtra", supplyState: "MH") == "INTRA")
        #expect(IndiaStateCatalog.jurisdiction(businessState: "maharashtra", supplyState: "Maharashtra") == "INTRA")
        #expect(IndiaStateCatalog.jurisdiction(businessState: "Maharashtra", supplyState: "KA") == "INTER")
        #expect(IndiaStateCatalog.jurisdiction(businessState: "27", supplyState: "29") == "INTER")
        // Unresolvable on both sides but identical text: still intra-state.
        #expect(IndiaStateCatalog.jurisdiction(businessState: "Atlantis", supplyState: "Atlantis") == "INTRA")
        // Unresolvable and different: inter-state, matching the old string compare.
        #expect(IndiaStateCatalog.jurisdiction(businessState: "Atlantis", supplyState: "El Dorado") == "INTER")
        #expect(IndiaStateCatalog.jurisdiction(businessState: "", supplyState: "Maharashtra") == nil)
        #expect(IndiaStateCatalog.jurisdiction(businessState: "Maharashtra", supplyState: nil) == nil)
    }

    @Test func gstinHintExplainsThePrefix() {
        #expect(
            IndiaStateCatalog.gstinStateHint(gstin: "27AAPFU0939F1ZV", state: "Maharashtra")
                == "GSTIN starts with 27, which is Maharashtra."
        )
        #expect(
            IndiaStateCatalog.gstinStateHint(gstin: "29AAGCB7383J1Z4", state: "Maharashtra")
                == "GSTIN starts with 29 (Karnataka) but the state is Maharashtra."
        )
        // Too short, non-numeric, no state selected, or a prefix naming no jurisdiction: no hint.
        #expect(IndiaStateCatalog.gstinStateHint(gstin: "2", state: "Maharashtra") == nil)
        #expect(IndiaStateCatalog.gstinStateHint(gstin: "", state: "Maharashtra") == nil)
        #expect(IndiaStateCatalog.gstinStateHint(gstin: "XXAAAAA0000A1Z5", state: "Maharashtra") == nil)
        #expect(IndiaStateCatalog.gstinStateHint(gstin: "27AAPFU0939F1ZV", state: "") == nil)
        #expect(IndiaStateCatalog.gstinStateHint(gstin: "99AAAAA0000A1Z5", state: "Maharashtra") == nil)
    }

    @Test func searchGroupsStatesBeforeUnionTerritories() {
        let all = IndiaStateCatalog.search(nil)
        #expect(all.count == IndiaStateCatalog.all.count)
        let firstTerritory = all.firstIndex { $0.isUnionTerritory }
        #expect(firstTerritory! >= IndiaStateCatalog.states.count)
        #expect(all[firstTerritory!...].allSatisfy(\.isUnionTerritory))

        #expect(IndiaStateCatalog.search("mahar").map(\.name) == ["Maharashtra"])
        #expect(IndiaStateCatalog.search("27").map(\.name) == ["Maharashtra"])
        #expect(IndiaStateCatalog.search("Atlantis").isEmpty)
    }
}

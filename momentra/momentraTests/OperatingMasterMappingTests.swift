import Foundation
import Testing
@testable import momentra

/// Mirrors `OperatingSheetCatalogTest` / `OperatingCaptureLayoutTest` on Android — the two
/// platforms read the same V099 contract, so the catalogues must not drift.
struct OperatingCatalogTests {

    // MARK: Surface catalogue

    @Test func catalogueHasTheEighteenSurfaces() {
        #expect(OperatingSheetCatalog.all.count == 18)
    }

    @Test func everySurfaceCodeIsUnique() {
        let codes = OperatingSheetCatalog.all.map(\.code)
        #expect(Set(codes).count == codes.count)
    }

    @Test func pulseSurfacesAreExactlyTheSixPulseGroups() {
        let pulse = OperatingSheetCatalog.all.filter(\.pulseSurface).map(\.code)
        #expect(Set(pulse) == OperatingSheetCatalog.pulseCodes)
        #expect(pulse.count == 6)
        #expect(OperatingSheetCatalog.all.filter { !$0.pulseSurface }.count == 12)
    }

    @Test func everySurfaceCarriesCopyAndAnEmptyState() {
        for spec in OperatingSheetCatalog.all {
            #expect(!spec.title.isEmpty, "\(spec.code) has no title")
            #expect(!spec.eyebrow.isEmpty, "\(spec.code) has no eyebrow")
            #expect(!spec.emptyTitle.isEmpty, "\(spec.code) has no empty title")
            #expect(!spec.emptyBody.isEmpty, "\(spec.code) has no empty body")
        }
    }

    @Test func boardCodesRouteBackToThemselves() {
        for code in ["orders", "stock", "network", "M4", "L1", "L2", "L3", "L4", "ME1", "ME2", "fulfilment", "ME4"] {
            #expect(OperatingSheetCatalog.code(forSection: code) == code)
            #expect(OperatingSheetCatalog.spec(forCode: code) != nil)
        }
    }

    @Test func unknownCodeHasNoSheet() {
        #expect(OperatingSheetCatalog.code(forSection: "NOT_A_SECTION") == nil)
        #expect(OperatingSheetCatalog.spec(forCode: "NOT_A_SECTION") == nil)
    }

    // MARK: Capture layouts

    @Test func twelveQaLayoutsWithSequentialCodes() {
        #expect(OperatingCaptureLayout.allCases.count == 12)
        let codes = OperatingCaptureLayout.allCases.map(\.qaCode)
        #expect(codes.first == "QA-01")
        #expect(codes.last == "QA-12")
        #expect(Set(codes).count == 12)
    }

    @Test func everyQuickAddModeResolvesToALayout() {
        for mode in ["SALE", "PURCHASE", "COLLECTION", "PAYMENT", "SPEND", "STOCK", "RETURN", "BUDGET", "UPDATE", "APPROVAL", "ORDER", "SHIPMENT"] {
            #expect(OperatingCaptureLayout.forMode(mode) != nil, "\(mode) has no layout")
        }
    }

    @Test func qaModesMapToTheirOwnFrames() {
        // The three modes with dedicated TRD-QA frames reuse the closest capture layout.
        #expect(OperatingCaptureLayout.forMode(OperatingCaptureLayout.holdMode) == .stock)
        #expect(OperatingCaptureLayout.forMode(OperatingCaptureLayout.transferMode) == .stock)
        #expect(OperatingCaptureLayout.forMode(OperatingCaptureLayout.reprintMode) == nil)
        #expect(OperatingCaptureLayout.holdChoices.count == 3)
        #expect(OperatingCaptureLayout.reprintKinds == ["INVOICE", "CHALLAN"])
    }

    @Test func layoutLookupIsCaseInsensitive() {
        #expect(OperatingCaptureLayout.forMode("sale") == .sale)
        #expect(OperatingCaptureLayout.forMode("  delivery  ") == .shipment)
    }

    // MARK: Verticals

    @Test func operatingSubtypesResolveIncludingManufacturing() {
        #expect(BusinessOperatingVertical.forSubtype("TRADING_WHOLESALE") == .trading)
        #expect(BusinessOperatingVertical.forSubtype("DISTRIBUTION_DEALERSHIP") == .distribution)
        #expect(BusinessOperatingVertical.forSubtype("trading_wholesale") == .trading)
        #expect(BusinessOperatingVertical.forSubtype("STANDARD_PRODUCTION") == .standardProduction)
        #expect(BusinessOperatingVertical.forSubtype("MADE_TO_ORDER") == .madeToOrder)
        #expect(BusinessOperatingVertical.forSubtype("RETAIL_STORE") == .retailStore)
        #expect(BusinessOperatingVertical.forSubtype(nil) == nil)
        #expect(BusinessOperatingVertical.forSubtype("SOMETHING_ELSE") == nil)
    }

    @Test func verticalsUseTheirOwnNoun() {
        #expect(BusinessOperatingVertical.trading.partyNoun == "Buyers")
        #expect(BusinessOperatingVertical.distribution.partyNoun == "Dealers")
        #expect(BusinessOperatingVertical.standardProduction.partyNoun == "Buyers")
        #expect(BusinessOperatingVertical.madeToOrder.partyNoun == "Customers")
        #expect(BusinessOperatingVertical.trading.storyPages.count == 8)
        #expect(BusinessOperatingVertical.distribution.storyPages.count == 8)
        #expect(BusinessOperatingVertical.standardProduction.storyPages.count == 8)
        #expect(BusinessOperatingVertical.madeToOrder.storyPages.count == 8)
        #expect(BusinessOperatingVertical.standardProduction.isManufacturing)
        #expect(BusinessOperatingVertical.madeToOrder.isManufacturing)
    }

    // MARK: Masters

    @Test func masterTabsMatchTheKindsTheyOwn() {
        #expect(OperatingMasterTab.principals.kinds.contains("DEALER"))
        #expect(OperatingMasterTab.principals.kinds.contains("PRINCIPAL"))
        #expect(OperatingMasterTab.catalogue.kinds.contains("SKU"))
        #expect(OperatingMasterTab.locations.kinds.contains("WAREHOUSE"))
        #expect(OperatingMasterTab.locations.kinds.contains("ROUTE"))
        // Price lists and invites are not master rows.
        #expect(OperatingMasterTab.priceLists.kinds.isEmpty)
        #expect(OperatingMasterTab.invite.kinds.isEmpty)
    }

    @Test func everyMasterTabExplainsItself() {
        for tab in OperatingMasterTab.allCases {
            #expect(!tab.title.isEmpty)
            #expect(!tab.note.isEmpty, "\(tab.rawValue) has no note")
        }
    }
}

/// The draft → wire body mapping, tested without SwiftUI.
struct OperatingMasterMappingTests {

    @Test func blankFieldsNeverReachTheWire() {
        var draft = OperatingMasterDraft(kind: "SKU", name: "  Basmati 25kg  ")
        draft.code = "   "
        draft.unit = ""
        draft.gstRate = " "
        draft.sellPrice = ""

        let body = draft.toCreateBody()
        #expect(body.name == "Basmati 25kg")
        #expect(body.code == nil)
        #expect(body.unit == nil)
        #expect(body.gstRate == nil)
        #expect(body.sellPrice == nil)
        #expect(body.gstApplicable == nil)
    }

    @Test func aRateIsTheOnlyThingThatMakesAMasterTaxable() {
        var draft = OperatingMasterDraft(kind: "SKU", name: "Rice")
        #expect(draft.gstApplicable == nil)

        draft.gstRate = "5"
        #expect(draft.gstApplicable == "YES")
        #expect(draft.toCreateBody().gstRate == "5")
        #expect(draft.toCreateBody().gstApplicable == "YES")

        draft.gstRate = ""
        #expect(draft.gstApplicable == nil)
    }

    @Test func updateBodyCarriesTheVersionStamp() {
        var draft = OperatingMasterDraft(kind: "DEALER", name: "Sharma Traders")
        draft.code = "DL-01"
        draft.gstin = "27ABCDE1234F1Z5"

        let body = draft.toUpdateBody(expectedVersion: 7)
        #expect(body.expectedVersion == 7)
        #expect(body.name == "Sharma Traders")
        #expect(body.code == "DL-01")
        #expect(body.gstin == "27ABCDE1234F1Z5")
    }

    @Test func aParentLinksACatalogueRowToItsMaster() {
        var draft = OperatingMasterDraft(kind: "BATCH", name: "Lot 12")
        draft.parentMasterId = "master-1"
        let body = draft.toCreateBody()
        #expect(body.parentMasterId == "master-1")
        #expect(body.kind == "BATCH")
    }

    @Test func saveRequiresAName() {
        var draft = OperatingMasterDraft(kind: "SKU", name: "   ")
        #expect(!draft.canSave)
        draft.name = "Sugar"
        #expect(draft.canSave)
    }

    @Test func masterDraftCarriesTheMomentScope() {
        var draft = OperatingMasterDraft(kind: "SKU", name: "Sugar")
        draft.momentId = "moment-1"
        #expect(draft.toCreateBody().momentId == "moment-1")
    }
}

/// The operator-facing words must map onto the contract's fixed vocabulary, because a stray
/// literal is a rejected write rather than a validation message the operator can act on.
struct OperatingContractVocabularyTests {

    @Test func stockActionsUseTheContractWords() {
        #expect(OperatingContractVocabulary.stockAction("In") == "ADD")
        #expect(OperatingContractVocabulary.stockAction("Out") == "REDUCE")
        #expect(OperatingContractVocabulary.stockAction("Set") == "SET")
        #expect(OperatingContractVocabulary.stockAction("Transfer") == "TRANSFER")
        // An unknown word must not reach the wire as itself.
        #expect(OperatingContractVocabulary.stockAction("Whatever") == "ADD")
    }

    @Test func stockReasonsFoldIntoTheAcceptedList() {
        let accepted: Set<String> = ["opening", "stocktake", "damage", "waste", "return", "correction", "TRANSFER"]
        for reason in ["Stocktake", "Damage", "Replenishment", "Correction", "Transfer", "", "Something else"] {
            let mapped = OperatingContractVocabulary.stockReason(reason)
            if let mapped {
                #expect(accepted.contains(mapped), "\(reason) mapped to \(mapped)")
            }
        }
        #expect(OperatingContractVocabulary.stockReason("") == nil)
        #expect(OperatingContractVocabulary.stockReason("Replenishment") == "correction")
        #expect(OperatingContractVocabulary.stockReason("Move") == "TRANSFER")
    }

    @Test func ownershipIsEitherOwnedOrConsignment() {
        #expect(OperatingContractVocabulary.ownership("Owned") == "OWNED")
        #expect(OperatingContractVocabulary.ownership("Consignment") == "CONSIGNMENT")
        #expect(OperatingContractVocabulary.ownership("consignment") == "CONSIGNMENT")
        #expect(OperatingContractVocabulary.ownership("") == "OWNED")
    }

    @Test func noHoldIsAReleaseNotAnAbsence() {
        #expect(OperatingContractVocabulary.holdStatus("No hold") == "RELEASED")
        #expect(OperatingContractVocabulary.holdStatus("Hold order") == "HELD")
        #expect(OperatingContractVocabulary.holdStatus("Hold dispatch") == "HELD")
    }
}

/// Money formatting is locale-aware and never invents a zero.
struct OperatingMoneyFormatTests {

    @Test func missingMoneyIsAnEmDash() {
        #expect(OperatingMoneyFormat.format("INR", nil) == "—")
        #expect(OperatingMoneyFormat.format("INR", "") == "—")
        #expect(OperatingMoneyFormat.format("INR", "0", visible: false) == "—")
    }

    @Test func inrUsesTheRupeeMark() {
        #expect(OperatingMoneyFormat.format("INR", "1234") == "₹1,234")
        #expect(OperatingMoneyFormat.format("INR", "1234.5") == "₹1,234.5")
    }

    @Test func otherCurrenciesTrailTheCode() {
        let formatted = OperatingMoneyFormat.format("USD", "1234")
        #expect(formatted.hasSuffix("USD"))
        #expect(formatted.contains("1,234"))
    }

    @Test func tileValueAppendsTheUnitExceptForProse() {
        #expect(OperatingMoneyFormat.tileValue("12", unit: "bags") == "12 bags")
        #expect(OperatingMoneyFormat.tileValue("12", unit: nil) == "12")
        // A prose answer already reads as a sentence and must stand alone.
        #expect(OperatingMoneyFormat.tileValue("Not recorded", unit: "bags") == "Not recorded")
    }
}

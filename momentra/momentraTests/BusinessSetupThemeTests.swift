import Foundation
import Testing
@testable import momentra

struct BusinessSetupThemeTests {

    @Test func nineTypeCodesResolveDistinctDomains() {
        let mapping: [(String, BusinessDomain)] = [
            ("CLIENT_CAMPAIGN", .marketing),
            ("OWN_BUSINESS_CAMPAIGN", .marketingOwn),
            ("TRADING_WHOLESALE", .trading),
            ("DISTRIBUTION_DEALERSHIP", .distribution),
            ("RETAIL_STORE", .retail),
            ("RESTAURANT_FOOD", .restaurant),
            ("CONSUMER_SERVICE", .services),
            ("STANDARD_PRODUCTION", .manufacturing),
            ("MADE_TO_ORDER", .madeToOrder),
        ]
        for (code, domain) in mapping {
            #expect(BusinessSetupTheme.domain(for: code) == domain)
            #expect(BusinessSetupTheme.forTypeCode(code).domain == domain)
        }
    }

    @Test func familyChipsCoverNineSubtypes() {
        let codes = Set(BusinessSetupFamily.allCases.flatMap { $0.chips.map(\.typeCode) })
        #expect(codes.count == 9)
        #expect(codes.contains("OWN_BUSINESS_CAMPAIGN"))
        #expect(codes.contains("MADE_TO_ORDER"))
    }

    @Test func retailKindOfBusinessLabelsMatchFigma() {
        let labels = BusinessSetupFamily.retail.chips.map(\.label)
        let codes = BusinessSetupFamily.retail.chips.map(\.typeCode)
        #expect(labels == ["Restaurant / Food", "Consumer Service", "Retail Store"])
        #expect(codes == ["RESTAURANT_FOOD", "CONSUMER_SERVICE", "RETAIL_STORE"])
    }

    @Test func marketingChipLabelsIncludeClientCampaign() {
        #expect(BusinessSetupFamily.marketing.chips.first?.label == "Client campaign")
        #expect(BusinessSetupFamily.marketing.chips.last?.typeCode == "OWN_BUSINESS_CAMPAIGN")
    }

    @Test func documentsExistForEveryChip() {
        for family in BusinessSetupFamily.allCases {
            for chip in family.chips {
                let spec = BusinessSetupDocuments.spec(for: chip.typeCode)
                #expect(spec.typeCode == chip.typeCode)
                #expect(!spec.sections.isEmpty)
            }
        }
    }

    @Test func setupKindIncludesOwnMarketingAndMadeToOrder() {
        #expect(BusinessSetupKind.marketingOwn.subtype == "OWN_BUSINESS_CAMPAIGN")
        #expect(BusinessSetupKind.madeToOrder.subtype == "MADE_TO_ORDER")
        #expect(BusinessSetupKind.from(typeCode: "OWN_BUSINESS_CAMPAIGN") == .marketingOwn)
        #expect(BusinessSetupKind.from(typeCode: "MADE_TO_ORDER") == .madeToOrder)
    }
}

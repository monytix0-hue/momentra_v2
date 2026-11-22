import Foundation
import Testing
@testable import momentra

struct OperatingSetupPayloadBuilderTests {

    @Test func coercesMoneyChannelsGstAndDatesForMarketing() {
        let built = OperatingSetupPayloadBuilder.build(
            preferences: [
                "campaignName": "Summer launch",
                "clientName": "Northwind",
                "channels": "Instagram|WhatsApp",
                "monthlyBudget": "₹2,00,000",
                "startDate": "01 Nov 2024",
                "endDate": "30 Nov 2024",
                "currency": "inr",
                "gstRegistered": "Yes",
                "gstType": "Regular",
                "gstin": "27aaaaa0000a1z2",
                "roles": "Owner|Finance",
                "teamMembers": "Asha|Ravi",
            ],
            momentTypeCode: "CLIENT_CAMPAIGN",
            title: "Fallback"
        )
        #expect(built.validationError == nil)
        #expect(built.preferences["identityName"] as? String == "Summer launch")
        #expect(built.preferences["partyName"] as? String == "Northwind")
        #expect(built.preferences["monthlyBudget"] as? String == "200000.0000")
        #expect(built.preferences["startDate"] as? String == "2024-11-01")
        #expect(built.preferences["currency"] as? String == "INR")
        #expect(built.preferences["gstTracking"] as? String == "YES")
        #expect(built.preferences["gstRegistration"] as? String == "REGULAR")
        #expect(built.preferences["gstin"] as? String == "27AAAAA0000A1Z2")
        let channels = built.preferences["channels"] as? [String]
        #expect(channels == ["Instagram", "WhatsApp"])
        #expect(built.masterSeeds.contains { $0.kind == "CHANNEL" && $0.name == "Instagram" })
        #expect(built.masterSeeds.contains { $0.kind == "CLIENT" && $0.name == "Northwind" })
    }

    @Test func mapsAllNineSubtypeIdentityKeys() {
        let cases: [(String, String, String)] = [
            ("CLIENT_CAMPAIGN", "campaignName", "C1"),
            ("OWN_BUSINESS_CAMPAIGN", "campaignName", "C2"),
            ("TRADING_WHOLESALE", "businessName", "T1"),
            ("DISTRIBUTION_DEALERSHIP", "businessName", "D1"),
            ("RETAIL_STORE", "storeName", "R1"),
            ("RESTAURANT_FOOD", "outletName", "F1"),
            ("CONSUMER_SERVICE", "serviceName", "S1"),
            ("STANDARD_PRODUCTION", "plantName", "P1"),
            ("MADE_TO_ORDER", "shopName", "M1"),
        ]
        for (code, key, name) in cases {
            var prefs: [String: Any] = [
                key: name,
                "companyName": "Party",
                "monthlyBudget": "1000",
                "monthlyTarget": "1000",
                "channels": "Walk-in",
                "startDate": "2024-11-01",
                "posStyle": "Counter POS",
                "bookingStyle": "Walk-in",
                "brandName": "Brand",
                "clientName": "Client",
                "principalBrand": "Principal",
                "format": "Kirana",
                "serviceStyle": "Dine-in",
            ]
            let built = OperatingSetupPayloadBuilder.build(
                preferences: prefs,
                momentTypeCode: code,
                title: "Title"
            )
            #expect(built.preferences["identityName"] as? String == name, "identity for \(code)")
            #expect(built.validationError == nil, "validation for \(code): \(built.validationError ?? "")")
        }
    }

    @Test func mapsRoleLabelsAndRegularGst() {
        #expect(OperatingSetupPayloadBuilder.mapRole("Owner") == "OWNER_ADMIN")
        #expect(OperatingSetupPayloadBuilder.mapRole("Finance") == "FINANCE")
        #expect(OperatingSetupPayloadBuilder.mapRole("Stylist") == "TEAM_MEMBER")
        #expect(OperatingSetupPayloadBuilder.mapGstRegistration("Regular") == "REGULAR")
        #expect(OperatingSetupPayloadBuilder.positiveAmount("₹2,00,000") == "200000.0000")
    }

    @Test func activateMissingChannelsSetsFieldError() {
        let built = OperatingSetupPayloadBuilder.build(
            preferences: [
                "serviceName": "BrightCare",
                "companyName": "BrightCare",
            ],
            momentTypeCode: "CONSUMER_SERVICE",
            title: "BrightCare",
            activating: true
        )
        #expect(built.fieldErrors["channels"] == "Select at least one channel.")
        #expect(built.validationError == "Select at least one channel.")
    }

    @Test func draftNeverReturnsFieldErrors() {
        let built = OperatingSetupPayloadBuilder.build(
            preferences: [:],
            momentTypeCode: "CONSUMER_SERVICE",
            title: "BrightCare",
            activating: false
        )
        #expect(built.validationError == nil)
        #expect(built.fieldErrors.isEmpty)
    }

    @Test func monthlyChargeTargetAliasesToMonthlyTarget() {
        let built = OperatingSetupPayloadBuilder.build(
            preferences: [
                "serviceName": "BrightCare",
                "companyName": "BrightCare",
                "channels": "Studio",
                "monthlyChargeTarget": "₹6,50,000",
            ],
            momentTypeCode: "CONSUMER_SERVICE",
            title: "BrightCare",
            activating: false
        )
        #expect(built.preferences["monthlyTarget"] as? String == "650000.0000")
    }

    @Test func invalidGstinHighlightsFieldOnActivate() {
        let built = OperatingSetupPayloadBuilder.build(
            preferences: [
                "serviceName": "BrightCare",
                "companyName": "BrightCare",
                "channels": "Studio",
                "gstTracking": "Yes",
                "gstin": "BAD",
            ],
            momentTypeCode: "CONSUMER_SERVICE",
            title: "BrightCare",
            activating: true
        )
        #expect(built.fieldErrors["gstin"] == "Enter a valid 15-digit GSTIN.")
    }

    @Test func retailAliasesCompanyNameAndIgnoresFormat() {
        let built = OperatingSetupPayloadBuilder.build(
            preferences: [
                "storeName": "Harbor Mart",
                "companyName": "Harbor Mart Pvt",
                "format": "Kirana",
                "locality": "Andheri West",
                "channels": "Counter|Online",
                "planMonth": "2026-10",
                "salePriceIncludesGst": "GST is added separately",
                "monthlyBudget": "₹8,40,000",
                "monthlyTarget": "₹12,00,000",
            ],
            momentTypeCode: "RETAIL_STORE",
            title: "Harbor Mart",
            activating: false
        )
        #expect(built.preferences["identityName"] as? String == "Harbor Mart")
        #expect(built.preferences["partyName"] as? String == "Harbor Mart Pvt")
        #expect(built.preferences["location"] as? String == "Andheri West")
        #expect(built.preferences["startDate"] as? String == "2026-10-01")
        #expect(built.preferences["salePriceIncludesGst"] as? String == "NO")
        #expect(built.preferences["channels"] as? [String] == ["Counter", "Online"])
        let status = OperatingSetupPayloadBuilder.planStatusMessage(preferences: built.preferences)
        #expect(status.ok)
        #expect(status.message == "Budget and sales target look good")
    }

    @Test func retailActivateMissingChannelsHighlightsChannels() {
        let built = OperatingSetupPayloadBuilder.build(
            preferences: [
                "storeName": "Harbor Mart",
                "companyName": "Harbor Mart",
            ],
            momentTypeCode: "RETAIL_STORE",
            title: "Harbor Mart",
            activating: true
        )
        #expect(built.fieldErrors["channels"] == "Select at least one channel.")
    }
}

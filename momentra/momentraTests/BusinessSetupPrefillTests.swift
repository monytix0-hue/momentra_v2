import Foundation
import Testing
@testable import momentra

struct BusinessSetupPrefillTests {

    @Test func dirtyKeyIsSkipped() {
        #expect(
            BusinessSetupPrefill.shouldApply(
                key: "businessName",
                dirty: ["businessName"],
                loaded: "Northstar Trading",
                current: "enterprises"
            ) == false
        )
    }

    @Test func cleanEmptyKeyIsFilled() {
        #expect(
            BusinessSetupPrefill.shouldApply(
                key: "businessName",
                dirty: [],
                loaded: "Northstar Trading",
                current: ""
            ) == true
        )
    }

    @Test func cleanKeyWithExistingValueIsNotClobbered() {
        #expect(
            BusinessSetupPrefill.shouldApply(
                key: "businessName",
                dirty: [],
                loaded: "Northstar Trading",
                current: "enterprises"
            ) == false
        )
    }

    @Test func whitespaceOnlyLoadedValueIsSkipped() {
        #expect(
            BusinessSetupPrefill.shouldApply(
                key: "businessName",
                dirty: [],
                loaded: "   ",
                current: ""
            ) == false
        )
        #expect(
            BusinessSetupPrefill.shouldApply(
                key: "businessName",
                dirty: [],
                loaded: nil,
                current: ""
            ) == false
        )
    }

    @Test func untouchedKeyWithIdenticalValueIsSkipped() {
        #expect(
            BusinessSetupPrefill.shouldApply(
                key: "businessName",
                dirty: [],
                loaded: "Northstar Trading",
                current: "Northstar Trading"
            ) == false
        )
    }

    @Test func logoMediaIdPrefillIsAllowedForUntouchedEmptyField() {
        #expect(
            BusinessSetupPrefill.shouldApply(
                key: "logoMediaId",
                dirty: [],
                loaded: "media_123",
                current: ""
            ) == true
        )
    }
}

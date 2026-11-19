import Foundation
import Testing
@testable import momentra

struct OperatingLovKindMatchTests {

    @Test func principalAppearsInSupplierAndBuyerLovs() {
        #expect(OperatingLovKindMatch.masterMatches(pickerKind: "SUPPLIER", masterKind: "PRINCIPAL"))
        #expect(OperatingLovKindMatch.masterMatches(pickerKind: "BUYER", masterKind: "PRINCIPAL"))
        #expect(OperatingLovKindMatch.masterMatches(pickerKind: "SUPPLIER", masterKind: "DEALER"))
        #expect(OperatingLovKindMatch.masterMatches(pickerKind: "BUYER", masterKind: "CUSTOMER"))
        #expect(OperatingLovKindMatch.masterMatches(pickerKind: "PRODUCT", masterKind: "SKU"))
        #expect(!OperatingLovKindMatch.masterMatches(pickerKind: "SUPPLIER", masterKind: "SKU"))
        #expect(!OperatingLovKindMatch.masterMatches(pickerKind: "PRODUCT", masterKind: "CUSTOMER"))
    }

    @Test func partyKindsForSupplyIncludePrincipal() {
        let kinds = OperatingLovKindMatch.partyKindsForSupply()
        #expect(kinds.isSuperset(of: ["CUSTOMER", "BUYER", "SUPPLIER", "PRINCIPAL", "DEALER", "VENDOR"]))
    }

    @Test func blankProductKindAndPartyVendorEdgeCases() {
        #expect(OperatingLovKindMatch.masterMatches(pickerKind: "PRODUCT", masterKind: ""))
        #expect(!OperatingLovKindMatch.masterMatches(pickerKind: "UOM", masterKind: ""))
        #expect(OperatingLovKindMatch.masterMatches(pickerKind: "UOM", masterKind: "SKU"))
        #expect(OperatingLovKindMatch.masterMatches(pickerKind: "BUYER", masterKind: "PARTY"))
        #expect(OperatingLovKindMatch.masterMatches(pickerKind: "SUPPLIER", masterKind: "PARTY"))
        #expect(OperatingLovKindMatch.masterMatches(pickerKind: "SUPPLIER", masterKind: "VENDOR"))
    }
}
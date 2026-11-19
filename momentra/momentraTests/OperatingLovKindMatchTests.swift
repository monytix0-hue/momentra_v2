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

    @Test func tradingLovOptionsIncludesPrincipalForSupplier() {
        let masters = [Self.master(id: "p1", kind: "PRINCIPAL", name: "Acme")]
        let opts = tradingLovOptions(kind: "SUPPLIER", masters: masters)
        #expect(opts.contains { $0.name == "Acme" })
    }

    @Test func tradingLovOptionsIncludesPrincipalForBuyer() {
        let masters = [Self.master(id: "p1", kind: "PRINCIPAL", name: "Both Co")]
        let opts = tradingLovOptions(kind: "BUYER", masters: masters)
        #expect(opts.contains { $0.name == "Both Co" })
    }

    @Test func tradingLovOptionsMapsSkuToProductPicker() {
        let masters = [Self.master(id: "s1", kind: "SKU", name: "Basmati", unit: "kg")]
        let opts = tradingLovOptions(kind: "PRODUCT", masters: masters)
        #expect(opts.count == 1)
        #expect(opts[0].name == "Basmati")
        #expect(opts[0].kind == "PRODUCT")
        #expect(opts[0].unit == "kg")
    }

    @Test func filterOperatingLovOptionsMatchesNameAndSubtitle() {
        let options = [
            OperatingLovOption(id: "1", name: "Acme", kind: "SUPPLIER", subtitle: "Mumbai"),
            OperatingLovOption(id: "2", name: "Beta Traders", kind: "SUPPLIER"),
        ]
        #expect(filterOperatingLovOptions(query: "mum", kind: "SUPPLIER", options: options).map(\.name) == ["Acme"])
        #expect(filterOperatingLovOptions(query: "", kind: "SUPPLIER", options: options).count == 2)
        #expect(filterOperatingLovOptions(query: "beta", kind: "SUPPLIER", options: options).map(\.name) == ["Beta Traders"])
    }

    private static func master(id: String, kind: String, name: String, unit: String? = nil) -> OperatingMasterPayload {
        let unitField = unit.map { ",\"unit\":\"\($0)\"" } ?? ""
        let json = """
        {"id":"\(id)","source":"MASTER","kind":"\(kind)","name":"\(name)","status":"ACTIVE","version":1\(unitField)}
        """
        // swiftlint:disable:next force_try
        return try! JSONDecoder().decode(OperatingMasterPayload.self, from: Data(json.utf8))
    }
}
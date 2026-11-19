import Foundation

/// LOV picker kind vs master row kind — mirrors Android `OperatingLovKindMatch`.
enum OperatingLovKindMatch {
    private static let productKinds: Set<String> = [
        "PRODUCT", "SKU", "BATCH", "PART", "MATERIAL", "RAW", "BOM",
    ]

    private static let buyerKinds: Set<String> = [
        "BUYER", "CUSTOMER", "PRINCIPAL", "DEALER", "PARTY",
    ]

    private static let supplierKinds: Set<String> = [
        "SUPPLIER", "VENDOR", "PRINCIPAL", "DEALER", "PARTY",
    ]

    private static let partyKindsForSupplySet: Set<String> = [
        "CUSTOMER", "BUYER", "SUPPLIER", "PRINCIPAL", "DEALER", "VENDOR",
    ]

    static func masterMatches(pickerKind: String, masterKind: String) -> Bool {
        let picker = pickerKind.uppercased()
        let master = masterKind.uppercased()
        switch picker {
        case "PRODUCT":
            return productKinds.contains(master) || masterKind.isEmpty
        case "UOM":
            return productKinds.contains(master)
        case "BUYER":
            return buyerKinds.contains(master)
        case "SUPPLIER":
            return supplierKinds.contains(master)
        default:
            return master == picker
        }
    }

    static func partyKindsForSupply() -> Set<String> {
        partyKindsForSupplySet
    }
}
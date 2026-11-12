import Foundation

/// Dist-specific masters / picker / Switch Moment copy (DST-MASTER / DST-PICKER / DST-SHEET).
/// Mirrored with APK `DistributionMasterCopy.kt`. Trading keeps generic `OperatingMasterTab` titles.
enum DistributionMasterCopy {
    static func tabTitle(_ tab: OperatingMasterTab) -> String {
        switch tab {
        case .principals: return "Principals & dealers"
        case .catalogue: return "Catalogue & SKU"
        case .locations: return "Warehouses & routes"
        case .priceLists: return "Price lists"
        case .invite: return "Invite & QR"
        }
    }

    static func tabSubtitle(_ tab: OperatingMasterTab) -> String {
        switch tab {
        case .principals:
            return "Principal & dealer master · GSTIN drives ITC on GRN and place of supply on invoice"
        case .catalogue:
            return "SKU master · principal, HSN, GST, MRP + case size before it appears in SO / PO"
        case .locations:
            return "Warehouse & route master · stock moves between them are transfers, not sales"
        case .priceLists:
            return "Effective-dated dealer price · scheme override never rewrites a posted invoice"
        case .invite:
            return "Invite grants a role and its access. Joining ≠ approval ≠ payment."
        }
    }

    static func tabNote(_ tab: OperatingMasterTab) -> String {
        switch tab {
        case .principals:
            return "Principal/dealer is a master write. GSTIN drives ITC on GRN and output GST on invoice — PO/SO save never posts GST."
        case .catalogue:
            return "Create SKU is a master write. SO and PO only pick saved SKUs; GST posts on invoice / GRN, not on save."
        case .locations:
            return "A route belongs to a warehouse. Moving stock between them is a transfer, not a sale."
        case .priceLists:
            return "A price list is effective-dated. Two lists may overlap in name but never in a day."
        case .invite:
            return "Invite grants a role and its access. Joining ≠ approval ≠ payment."
        }
    }

    static func newCta(_ tab: OperatingMasterTab) -> String {
        switch tab {
        case .principals: return "+ New principal / dealer"
        case .catalogue: return "+ New SKU"
        case .locations: return "+ New warehouse or route"
        case .priceLists: return "+ New price list"
        case .invite: return "Done"
        }
    }

    static func formTitle(_ tab: OperatingMasterTab, editingName: String?) -> String {
        if let editingName { return "Edit \(editingName)" }
        switch tab {
        case .principals: return "New / Edit Principal or Dealer"
        case .catalogue: return "Create SKU"
        case .locations: return "Add / Edit Warehouse or Route"
        case .priceLists: return "Create price list"
        case .invite: return "Invite & QR"
        }
    }

    static func saveCta(_ tab: OperatingMasterTab) -> String {
        switch tab {
        case .principals: return "Save party"
        case .catalogue: return "Save SKU"
        case .locations: return "Save warehouse / route"
        case .priceLists: return "Save price list"
        case .invite: return "Done"
        }
    }

    static func saveFootnote(_ tab: OperatingMasterTab) -> String {
        switch tab {
        case .principals: return "Party saved · available in PO / SO"
        case .catalogue: return "Master saved · available in browser on next open"
        case .locations: return "Location saved · available on stock / transfer"
        case .priceLists: return "Price list saved · effective from valid-from"
        case .invite: return "3 invites pending · roles apply on join"
        }
    }

    static let switchTitle = "Switch Moment"
    static let switchSubtitle = "Your Business moments · tap to open"
    static let switchNew = "+ New Business moment"

    static let skuBrowserTitle = "Catalogue"
    static let skuBrowserHint = "Tap Add · qty per line"
    static let skuBrowserSearch = "Search SKU, name or barcode"
    static let skuBrowserCreate = "+ New SKU"
    static let skuBrowserCreateHint = "Name · HSN · GST · MRP"

    static let skuCategories = ["Staples", "Beverages", "Personal care", "Home care", "Snacks"]
}

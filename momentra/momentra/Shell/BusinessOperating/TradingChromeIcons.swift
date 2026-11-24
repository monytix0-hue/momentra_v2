import Foundation

/// Locked TRD-v12 pulse + Quick Add hub icon catalog (Figma chrome parity).
/// Keep in sync with Android `TradingChromeIcons`.
enum TradingChromeIcons {
    /// Pulse quick-action row — Sale / Purchase / Collection / Payment.
    static let pulseActionCodes: [String] = ["SALE", "PURCHASE", "COLLECTION", "PAYMENT"]

    static func pulseActionAsset(_ code: String) -> String {
        switch code.uppercased() {
        case "SALE": return "ic_rest_tile_sale"
        case "PURCHASE": return "ic_rest_tile_purchase"
        case "COLLECTION": return "ic_rest_tile_collection"
        case "PAYMENT": return "ic_rest_tile_payment"
        default:
            preconditionFailure("Unknown Trading pulse action icon code: \(code)")
        }
    }

    /// Hub tile id → Asset Catalog name (`qa_*` / `biz_qa_*`).
    static func hubAsset(_ tileId: String) -> String {
        switch tileId.uppercased() {
        case "SALE", "REPRINT": return "qa_receipt"
        case "PURCHASE": return "biz_qa_wallet"
        case "COLLECTION", "RETURN": return "qa_arrow_left_right"
        case "PAYMENT": return "qa_credit_card"
        case "SPEND", "BUDGET": return "qa_budget"
        case "STOCK", "CATALOGUE": return "qa_package"
        case "ORDER": return "qa_clipboard"
        case "SHIPMENT", "DELIVERY", "TRANSFER": return "qa_shipment"
        case "PRINCIPALS": return "biz_qa_users"
        case "LOCATIONS": return "qa_truck"
        case "INVITE": return "qa_qr"
        case "UPDATE": return "qa_alert"
        case "APPROVAL", "HOLD": return "qa_shield"
        case "PRODUCTION": return "qa_factory"
        case "SEARCH": return "biz_qa_search"
        default:
            preconditionFailure("Unknown Trading hub icon tile id: \(tileId)")
        }
    }

    static let tradingHubCoreIds: [String] = [
        "SALE", "PURCHASE", "COLLECTION", "PAYMENT", "SPEND",
        "STOCK", "RETURN", "BUDGET", "ORDER", "SHIPMENT",
    ]

    static let tradingHubSupportIds: [String] = [
        "PRINCIPALS", "CATALOGUE", "LOCATIONS", "INVITE", "UPDATE", "APPROVAL",
    ]
}

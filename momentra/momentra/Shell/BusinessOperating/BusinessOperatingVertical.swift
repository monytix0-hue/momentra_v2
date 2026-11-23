import Foundation

/// Routes business Ready tabs to the shared operating hub (trading, distribution, retail-family stubs).
enum BusinessOperatingVertical: String, CaseIterable, Sendable {
    case tradingWholesale = "TRADING_WHOLESALE"
    case distributionDealership = "DISTRIBUTION_DEALERSHIP"
    case clientCampaign = "CLIENT_CAMPAIGN"
    case ownBusinessCampaign = "OWN_BUSINESS_CAMPAIGN"
    case retailStore = "RETAIL_STORE"
    case restaurantFood = "RESTAURANT_FOOD"
    case consumerService = "CONSUMER_SERVICE"
    case standardProduction = "STANDARD_PRODUCTION"
    case madeToOrder = "MADE_TO_ORDER"

    static func forSubtype(_ code: String?) -> BusinessOperatingVertical? {
        let raw = code?.trimmingCharacters(in: .whitespacesAndNewlines).uppercased() ?? ""
        guard !raw.isEmpty else { return nil }
        return BusinessOperatingVertical(rawValue: raw)
    }
}
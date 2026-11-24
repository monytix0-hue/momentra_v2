import Foundation

/// Nine Business setup subtypes keyed by type code, grouped into the four guided-create families.
enum BusinessSetupFamily: String, CaseIterable, Identifiable {
    case marketing = "MARKETING_CAMPAIGNS"
    case trading = "TRADING_DISTRIBUTION"
    case retail = "RETAIL_CONSUMER"
    case manufacturing = "MANUFACTURING_PRODUCTION"

    var id: String { rawValue }

    var title: String {
        switch self {
        case .marketing: return "Marketing"
        case .trading: return "Trading"
        case .retail: return "Retail"
        case .manufacturing: return "Manufacturing"
        }
    }

    var chips: [BusinessSetupChip] {
        switch self {
        case .marketing:
            return [
                .init(label: "Client campaign", typeCode: "CLIENT_CAMPAIGN"),
                .init(label: "Own business", typeCode: "OWN_BUSINESS_CAMPAIGN"),
            ]
        case .trading:
            return [
                .init(label: "Trading", typeCode: "TRADING_WHOLESALE"),
                .init(label: "Distribution", typeCode: "DISTRIBUTION_DEALERSHIP"),
            ]
        case .retail:
            // Figma Kind of business order (`5:71178`): Restaurant → Consumer Service → Retail Store
            return [
                .init(label: "Restaurant / Food", typeCode: "RESTAURANT_FOOD"),
                .init(label: "Consumer Service", typeCode: "CONSUMER_SERVICE"),
                .init(label: "Retail Store", typeCode: "RETAIL_STORE"),
            ]
        case .manufacturing:
            return [
                .init(label: "Manufacturing", typeCode: "STANDARD_PRODUCTION"),
                .init(label: "Job order", typeCode: "MADE_TO_ORDER"),
            ]
        }
    }

    var defaultTypeCode: String { chips[0].typeCode }

    static func forFamilyCode(_ code: String?) -> BusinessSetupFamily {
        switch (code ?? "").uppercased() {
        case "MARKETING_CAMPAIGNS": return .marketing
        case "TRADING_DISTRIBUTION": return .trading
        case "RETAIL_CONSUMER": return .retail
        case "MANUFACTURING_PRODUCTION": return .manufacturing
        default: return .forTypeCode(code)
        }
    }

    static func forTypeCode(_ code: String?) -> BusinessSetupFamily {
        switch BusinessSetupTheme.domain(for: code ?? "") {
        case .marketing, .marketingOwn: return .marketing
        case .trading, .distribution: return .trading
        case .retail, .restaurant, .services: return .retail
        case .manufacturing, .madeToOrder: return .manufacturing
        case .corporate: return .trading
        }
    }
}

struct BusinessSetupChip: Identifiable, Equatable {
    let label: String
    let typeCode: String
    var id: String { typeCode }
}

extension BusinessSetupKind {
    static func from(typeCode: String?) -> BusinessSetupKind {
        switch (typeCode ?? "").uppercased() {
        case "CLIENT_CAMPAIGN": return .marketing
        case "OWN_BUSINESS_CAMPAIGN": return .marketingOwn
        case "TRADING_WHOLESALE": return .trading
        case "DISTRIBUTION_DEALERSHIP": return .distribution
        case "RETAIL_STORE": return .retail
        case "RESTAURANT_FOOD": return .restaurant
        case "CONSUMER_SERVICE": return .service
        case "STANDARD_PRODUCTION": return .manufacturing
        case "MADE_TO_ORDER": return .madeToOrder
        default:
            return BusinessSetupKind.allCases.first { $0.subtype == (typeCode ?? "").uppercased() } ?? .manufacturing
        }
    }
}

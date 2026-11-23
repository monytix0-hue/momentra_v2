import SwiftUI

/// Per-moment setup palette resolved from `BusinessTokens` domain colors.
/// Chip swaps retheme the whole document from the new type code.
struct BusinessSetupTheme: Equatable {
    let typeCode: String
    let domain: BusinessDomain
    let canvas: Color
    let canvasTop: Color
    let surface: Color
    let inset: Color
    let accent: Color
    let accentSoft: Color
    let accentDeep: Color
    let onAccent: Color
    let heroStart: Color
    let heroEnd: Color
    let label: Color
    let hint: Color

    var heroGradient: LinearGradient {
        LinearGradient(colors: [heroStart, heroEnd], startPoint: .topLeading, endPoint: .bottomTrailing)
    }

    var accentGradient: LinearGradient {
        LinearGradient(colors: [accent, accentSoft], startPoint: .leading, endPoint: .trailing)
    }

    static func forTypeCode(_ raw: String?) -> BusinessSetupTheme {
        let code = (raw ?? "").uppercased()
        let domain = domain(for: code)
        let c = BusinessTokens.Colors.self
        switch domain {
        case .marketing:
            return theme(code, .marketing, c.marketingCanvas, c.marketingCanvasTop, c.marketingSurface, c.marketingInset,
                         c.marketingAccent, c.marketingAccentSoft, c.marketingAccentDeep, c.marketingOnAccent,
                         c.marketingHeroStart, c.marketingHeroEnd, c.marketingLabel, c.marketingHint)
        case .marketingOwn:
            return theme(code, .marketingOwn, c.marketingOwnCanvas, c.marketingOwnCanvasTop, c.marketingOwnSurface, c.marketingOwnInset,
                         c.marketingOwnAccent, c.marketingOwnAccentSoft, c.marketingOwnAccentDeep, c.marketingOwnOnAccent,
                         c.marketingOwnHeroStart, c.marketingOwnHeroEnd, c.marketingOwnLabel, c.marketingOwnHint)
        case .trading:
            return theme(code, .trading, c.tradingCanvas, c.tradingCanvasTop, c.tradingSurface, c.tradingInset,
                         c.tradingAccent, c.tradingAccentSoft, c.tradingAccentDeep, c.tradingOnAccent,
                         c.tradingHeroStart, c.tradingHeroEnd, c.tradingLabel, c.tradingHint)
        case .distribution:
            return theme(code, .distribution, c.distributionCanvas, c.distributionCanvasTop, c.distributionSurface, c.distributionInset,
                         c.distributionAccent, c.distributionAccentSoft, c.distributionAccentDeep, c.distributionOnAccent,
                         c.distributionHeroStart, c.distributionHeroEnd, c.distributionLabel, c.distributionHint)
        case .retail:
            return theme(code, .retail, c.retailCanvas, c.retailCanvasTop, c.retailSurface, c.retailInset,
                         c.retailAccent, c.retailAccentSoft, c.retailAccentDeep, c.retailOnAccent,
                         c.retailHeroStart, c.retailHeroEnd, c.retailLabel, c.retailHint)
        case .restaurant:
            return theme(code, .restaurant, c.restaurantCanvas, c.restaurantCanvasTop, c.restaurantSurface, c.restaurantInset,
                         c.restaurantAccent, c.restaurantAccentSoft, c.restaurantAccentDeep, c.restaurantOnAccent,
                         c.restaurantHeroStart, c.restaurantHeroEnd, c.restaurantLabel, c.restaurantHint)
        case .services:
            return theme(code, .services, c.servicesCanvas, c.servicesCanvasTop, c.servicesSurface, c.servicesInset,
                         c.servicesAccent, c.servicesAccentSoft, c.servicesAccentDeep, c.servicesOnAccent,
                         c.servicesHeroStart, c.servicesHeroEnd, c.servicesLabel, c.servicesHint)
        case .manufacturing:
            return theme(code, .manufacturing, c.manufacturingCanvas, c.manufacturingCanvasTop, c.manufacturingSurface, c.manufacturingInset,
                         c.manufacturingAccent, c.manufacturingAccentSoft, c.manufacturingAccentDeep, c.manufacturingOnAccent,
                         c.manufacturingHeroStart, c.manufacturingHeroEnd, c.manufacturingLabel, c.manufacturingHint)
        case .madeToOrder:
            return theme(code, .madeToOrder, c.madeToOrderCanvas, c.madeToOrderCanvasTop, c.madeToOrderSurface, c.madeToOrderInset,
                         c.madeToOrderAccent, c.madeToOrderAccentSoft, c.madeToOrderAccentDeep, c.madeToOrderOnAccent,
                         c.madeToOrderHeroStart, c.madeToOrderHeroEnd, c.madeToOrderLabel, c.madeToOrderHint)
        case .corporate:
            return theme(code, .corporate, c.corporateCanvas, c.corporateCanvasTop, c.corporateSurface, c.corporateInset,
                         c.corporateAccent, c.corporateAccentSoft, c.corporateAccentDeep, c.corporateOnAccent,
                         c.corporateHeroStart, c.corporateHeroEnd, c.corporateLabel, c.corporateHint)
        }
    }

    static func domain(for typeCode: String) -> BusinessDomain {
        switch typeCode.uppercased() {
        case "CLIENT_CAMPAIGN": return .marketing
        case "OWN_BUSINESS_CAMPAIGN": return .marketingOwn
        case "TRADING_WHOLESALE": return .trading
        case "DISTRIBUTION_DEALERSHIP": return .distribution
        case "RETAIL_STORE": return .retail
        case "RESTAURANT_FOOD": return .restaurant
        case "CONSUMER_SERVICE": return .services
        case "STANDARD_PRODUCTION": return .manufacturing
        case "MADE_TO_ORDER": return .madeToOrder
        default:
            if typeCode.contains("OWN") && typeCode.contains("CAMPAIGN") { return .marketingOwn }
            if typeCode.contains("MARKETING") || typeCode.contains("CAMPAIGN") { return .marketing }
            if typeCode.contains("DISTRIBUTION") { return .distribution }
            if typeCode.contains("TRADING") { return .trading }
            if typeCode.contains("RESTAURANT") { return .restaurant }
            if typeCode.contains("RETAIL") { return .retail }
            if typeCode.contains("SERVICE") { return .services }
            if typeCode.contains("MADE") || typeCode.contains("ORDER") { return .madeToOrder }
            if typeCode.contains("MANUFACTUR") || typeCode.contains("PRODUCTION") { return .manufacturing }
            return .trading
        }
    }

    private static func theme(
        _ code: String,
        _ domain: BusinessDomain,
        _ canvas: Color, _ canvasTop: Color, _ surface: Color, _ inset: Color,
        _ accent: Color, _ soft: Color, _ deep: Color, _ onAccent: Color,
        _ heroStart: Color, _ heroEnd: Color, _ label: Color, _ hint: Color
    ) -> BusinessSetupTheme {
        BusinessSetupTheme(
            typeCode: code.isEmpty ? domain.rawValue.uppercased() : code,
            domain: domain,
            canvas: canvas,
            canvasTop: canvasTop,
            surface: surface,
            inset: inset,
            accent: accent,
            accentSoft: soft,
            accentDeep: deep,
            onAccent: onAccent,
            heroStart: heroStart,
            heroEnd: heroEnd,
            label: label,
            hint: hint
        )
    }
}

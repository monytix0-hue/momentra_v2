import Foundation

/// Figma CLIENT_CAMPAIGN hub widget order + display titles (nodes 5:29386–5:30195).
enum ClientCampaignHubWidgets {
    static let typeCode = "CLIENT_CAMPAIGN"

    private static let pulseOrder = ["P04", "P01", "P03", "P02", "P05"]
    private static let momentsOrder = ["M01", "M02", "M03", "M04", "MKT-RESULTS"]
    private static let lifeOrder = ["L01", "L02", "L03", "L04"]
    private static let memoryOrder = ["ME01", "ME02", "ME03", "ME04"]

    private static let titles: [String: String] = [
        "P04": "Needs Attention",
        "P01": "Target & Budget",
        "P03": "Payment Readiness",
        "P02": "Finance",
        "P05": "Activity",
        "M01": "Campaign Overview",
        "M02": "People",
        "M03": "Channels & Network",
        "M04": "Open Commitments",
        "MKT-RESULTS": "Campaign Results",
        "L01": "Current Campaign",
        "L02": "Channel Activity",
        "L03": "Money Moving",
        "L04": "People Today",
        "ME01": "Monthly History",
        "ME02": "Money History",
        "ME03": "People & Activity",
        "ME04": "Patterns & Decisions",
    ]

    static func isClientCampaign(_ typeCode: String?) -> Bool {
        typeCode?.trimmingCharacters(in: .whitespacesAndNewlines).uppercased() == Self.typeCode
    }

    static func order(for screen: String) -> [String]? {
        switch screen.uppercased() {
        case "PULSE": return pulseOrder
        case "MOMENTS": return momentsOrder
        case "LIFE": return lifeOrder
        case "MEMORY": return memoryOrder
        default: return nil
        }
    }

    static func sortWidgets(screen: String, widgets: [OperatingWidgetPayload]) -> [OperatingWidgetPayload] {
        guard let order = order(for: screen) else { return widgets }
        let rank = Dictionary(uniqueKeysWithValues: order.enumerated().map { ($0.element, $0.offset) })
        return widgets.sorted {
            let left = rank[$0.widgetId] ?? Int.max
            let right = rank[$1.widgetId] ?? Int.max
            if left != right { return left < right }
            return $0.widgetId < $1.widgetId
        }
    }

    static func displayTitle(widgetId: String, serverTitle: String) -> String {
        titles[widgetId] ?? (serverTitle.isEmpty ? widgetId : serverTitle)
    }
}
import Foundation
import Testing
@testable import momentra

struct ClientCampaignHubWidgetsTests {

    private func widget(_ id: String, screen: String, title: String = "Server") -> OperatingWidgetPayload {
        let json = """
        {"widgetId":"\(id)","screen":"\(screen)","title":"\(title)"}
        """
        return try! JSONDecoder().decode(OperatingWidgetPayload.self, from: json.data(using: .utf8)!)
    }

    @Test func pulseSortOrderAndTitlesMatchFigma() {
        let incoming = ["P01", "P02", "P03", "P04", "P05"].map { widget($0, screen: "PULSE") }
        let sorted = ClientCampaignHubWidgets.sortWidgets(screen: "PULSE", widgets: incoming)
        #expect(sorted.map(\.widgetId) == ["P04", "P01", "P03", "P02", "P05"])
        #expect(ClientCampaignHubWidgets.displayTitle(widgetId: "P04", serverTitle: "Attention") == "Needs Attention")
        #expect(ClientCampaignHubWidgets.displayTitle(widgetId: "P01", serverTitle: "Target & Budget") == "Target & Budget")
    }
}
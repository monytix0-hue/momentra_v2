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

    @Test func momentsSortOrderAndTitlesMatchFigma() {
        let incoming = ["M04", "MKT-RESULTS", "M01", "M03", "M02"].map { widget($0, screen: "MOMENTS") }
        let sorted = ClientCampaignHubWidgets.sortWidgets(screen: "MOMENTS", widgets: incoming)
        #expect(sorted.map(\.widgetId) == ["M01", "M02", "M03", "M04", "MKT-RESULTS"])
        #expect(ClientCampaignHubWidgets.displayTitle(widgetId: "MKT-RESULTS", serverTitle: "Campaign Results") == "Campaign Results")
    }

    @Test func lifeSortOrderAndTitlesMatchFigma() {
        let incoming = ["L04", "L01", "L03", "L02"].map { widget($0, screen: "LIFE") }
        let sorted = ClientCampaignHubWidgets.sortWidgets(screen: "LIFE", widgets: incoming)
        #expect(sorted.map(\.widgetId) == ["L01", "L02", "L03", "L04"])
        #expect(ClientCampaignHubWidgets.displayTitle(widgetId: "L04", serverTitle: "Operations / Execution") == "People Today")
    }
}
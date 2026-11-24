import Foundation
import SwiftUI
import Testing
@testable import momentra

struct OwnMarketingHubTests {

    private func widget(_ id: String, screen: String, title: String = "Server") -> OperatingWidgetPayload {
        let json = """
        {"widgetId":"\(id)","screen":"\(screen)","title":"\(title)"}
        """
        return try! JSONDecoder().decode(OperatingWidgetPayload.self, from: json.data(using: .utf8)!)
    }

    @Test func detectsOwnSubtype() {
        #expect(OwnMarketingHub.isOwn("OWN_BUSINESS_CAMPAIGN"))
        #expect(OwnMarketingHub.isOwn(" own_business_campaign "))
        #expect(!OwnMarketingHub.isOwn("CLIENT_CAMPAIGN"))
        #expect(!OwnMarketingHub.isOwn(nil))
    }

    @Test func pulseOrderMatchesFigmaNeedsAttentionFirst() {
        let incoming = [
            widget("P01", screen: "PULSE", title: "Target"),
            widget("P04", screen: "PULSE", title: "Attention"),
            widget("P05", screen: "PULSE", title: "Activity"),
        ]
        let sorted = OwnMarketingHub.sortWidgets(screen: "PULSE", widgets: incoming)
        #expect(sorted.map(\.widgetId) == ["P04", "P01", "P05"])
        #expect(OwnMarketingHub.displayTitle(widgetId: "P04", serverTitle: "Attention") == "Needs Attention")
        #expect(OwnMarketingHub.displayCode(widgetId: "P04", serverDisplayCode: nil) == "AN-P01")
        #expect(OwnMarketingHub.displayCode(widgetId: "P01", serverDisplayCode: nil) == "AN-P02")
        #expect(OwnMarketingHub.displayCode(widgetId: "P04", serverDisplayCode: "AN-CUSTOM") == "AN-CUSTOM")
    }

    @Test func momentsIncludesAttributionSlot() {
        let incoming = ["MKT-ATTRIBUTION", "M01", "MKT-RESULTS", "M02"].map { widget($0, screen: "MOMENTS") }
        let sorted = OwnMarketingHub.sortWidgets(screen: "MOMENTS", widgets: incoming)
        #expect(sorted.map(\.widgetId) == ["M01", "M02", "MKT-RESULTS", "MKT-ATTRIBUTION"])
        #expect(OwnMarketingHub.displayTitle(widgetId: "M01", serverTitle: "Overview") == "Campaign / Business Moment")
        #expect(OwnMarketingHub.displayTitle(widgetId: "MKT-ATTRIBUTION", serverTitle: "Attr") == "Attribution")
    }

    @Test func ownAccentAndActivateCopy() {
        #expect(BusinessAppStateAccent.forTypeCode("OWN_BUSINESS_CAMPAIGN") == .magenta)
        #expect(BusinessSetupActivateToastCopy.defaultTitle == "Moment activated")
        #expect(
            BusinessSetupActivateToastCopy.subtitle(
                momentTitle: "Harbor & Loom",
                momentTypeCode: "OWN_BUSINESS_CAMPAIGN"
            ) == "Harbor & Loom · Campaign is live · opening Pulse"
        )
    }

    @Test func hubPaletteUsesOwnMagentaSurface() {
        let palette = HubPalette.forType(subtype: "OWN_BUSINESS_CAMPAIGN", family: "MARKETING_CAMPAIGNS")
        #expect(palette.accent == BusinessTokens.Colors.marketingOwnAccent)
        #expect(palette.bg == BusinessTokens.Colors.marketingOwnCanvas)
        #expect(palette.card == BusinessTokens.Colors.marketingOwnSurface)
        #expect(palette.onAccent == BusinessTokens.Colors.marketingOwnOnAccent)
    }

    @Test func clientIndigoPathUntouched() {
        #expect(BusinessAppStateAccent.forTypeCode("CLIENT_CAMPAIGN") == .indigo)
        let palette = HubPalette.forType(subtype: "CLIENT_CAMPAIGN", family: "MARKETING_CAMPAIGNS")
        #expect(palette.accent == BusinessAppStateAccent.indigo.color)
        #expect(palette.bg == Color(hex: "#0C0F15"))
    }
}

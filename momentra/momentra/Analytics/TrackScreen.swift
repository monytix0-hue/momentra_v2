import SwiftUI

private struct TrackScreenModifier: ViewModifier {
    let screenName: String
    let screenClass: String

    func body(content: Content) -> some View {
        content
            .onAppear {
                MomentraAnalytics.shared.onScreenEnter(screenName, screenClass: screenClass)
            }
            .onDisappear {
                MomentraAnalytics.shared.onScreenExit(screenName)
            }
    }
}

extension View {
    func trackScreen(_ screenName: String, screenClass: String? = nil) -> some View {
        modifier(TrackScreenModifier(
            screenName: screenName,
            screenClass: screenClass ?? screenName
        ))
    }
}

func trackWidget(screenName: String, widgetName: String, action: String = "tap") {
    MomentraAnalytics.shared.trackWidget(screenName: screenName, widgetName: widgetName, action: action)
}

import SwiftUI

struct TourStep: Identifiable {
    let id = UUID()
    let accessibilityID: String
    let title: String
    let message: String
    let action: (() -> Void)?
    var overlayColor: Color = Color.black.opacity(0.4)
    var arrowPosition: Edge = .top
}

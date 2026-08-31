import Foundation
import SwiftUI

/// Global brand tokens from `design/momentra_theme.css` v1.1.
enum MomentraBrandTokens {
    static let indigo50 = Color(hex: "#EEE9FF")
    static let indigo100 = Color(hex: "#C4BDEE")
    static let indigo300 = Color(hex: "#8C83D4")
    static let indigo500 = Color(hex: "#4B3EA8")
    static let indigo700 = Color(hex: "#2D1F5E")
    static let indigo900 = Color(hex: "#1A0F3D")

    static let ember300 = Color(hex: "#F59060")
    static let ember500 = Color(hex: "#E8621A")

    static let amber500 = Color(hex: "#F5A623")

    static let teal500 = Color(hex: "#1D9E75")

    static let brand = indigo700
    static let cta = ember500
    static let progress = amber500
    static let safe = teal500
    static let textOnDark = Color(hex: "#F5F0FF")
    static let textOnEmber = Color(hex: "#FFF5F0")

    static let space3: CGFloat = 12
    static let space4: CGFloat = 16
    static let space6: CGFloat = 24
    static let space8: CGFloat = 32
    static let radiusCard: CGFloat = 12
}

extension Color {
    init(hex: String) {
        let hex = hex.trimmingCharacters(in: CharacterSet.alphanumerics.inverted)
        var int: UInt64 = 0
        Scanner(string: hex).scanHexInt64(&int)
        let r, g, b: UInt64
        switch hex.count {
        case 6:
            (r, g, b) = ((int >> 16) & 0xFF, (int >> 8) & 0xFF, int & 0xFF)
        default:
            (r, g, b) = (0, 0, 0)
        }
        self.init(
            .sRGB,
            red: Double(r) / 255,
            green: Double(g) / 255,
            blue: Double(b) / 255,
            opacity: 1
        )
    }
}

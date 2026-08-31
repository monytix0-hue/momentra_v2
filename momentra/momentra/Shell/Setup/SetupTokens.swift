import SwiftUI

/// Figma setup wizard tokens (Personal / Group / Business).
enum SetupTokens {
    static let bgPrimary = Color(hex: "#14121B")
    static let bgShell = Color(hex: "#0C0F15")
    static let textPrimary = Color(hex: "#E5E0EE")
    static let textSecondary = Color(hex: "#C9C4D8")
    static let brandPrimary = Color(hex: "#C9BFFF")
    static let accentPurple = Color(hex: "#7C5CFC")
    static let surfaceCard = Color.white.opacity(0.04)
    static let borderSubtle = Color.white.opacity(0.08)
    static let chipSelected = Color(hex: "#7C5CFC")
    static let chipUnselected = Color.white.opacity(0.04)
    static let groupOrange = Color(hex: "#FF7A3D")
    static let groupText = Color(hex: "#E5E2E1")
    static let groupSecondary = Color(hex: "#DFC0B4")
    static let groupCard = Color(hex: "#201F1F")
    static let groupBorder = Color(hex: "#3A3838")
    static let groupCtaText = Color(hex: "#591C00")
    static let bizAccent = Color(hex: "#818CF8")
    static let bizBg = Color(hex: "#0C0F15")
    static let bizCard = Color(hex: "#161B26")
    static let error = Color(hex: "#FF8A80")
    static let savedGreen = Color(hex: "#10B981")
    static let pressureBar = Color(hex: "#E8621A")
    static let recoveryBar = Color(hex: "#60A5FA")

    static var personalCtaGradient: LinearGradient {
        LinearGradient(
            colors: [accentPurple, Color(hex: "#E91E63")],
            startPoint: .leading,
            endPoint: .trailing
        )
    }

    static var groupCtaGradient: LinearGradient {
        LinearGradient(
            colors: [groupOrange, Color(hex: "#FFB598")],
            startPoint: .leading,
            endPoint: .trailing
        )
    }
}

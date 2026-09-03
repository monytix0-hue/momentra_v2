import SwiftUI

/// Figma 575:14655 / 575:15497 Group Trip Quick Add icons.
enum GroupQaIcons {
    static func close(size: CGFloat = 14) -> some View {
        Image("GroupQaClose")
            .renderingMode(.template)
            .resizable()
            .scaledToFit()
            .frame(width: size, height: size)
            .foregroundStyle(Color(hex: "#E5E0EE"))
    }

    static func search(size: CGFloat = 18) -> some View {
        Image("GroupQaSearch")
            .renderingMode(.template)
            .resizable()
            .scaledToFit()
            .frame(width: size, height: size)
    }

    static func qrCode(size: CGFloat = 18) -> some View {
        Image(systemName: "qrcode.viewfinder")
            .font(.system(size: size, weight: .semibold))
            .frame(width: size, height: size)
    }

    @ViewBuilder
    static func tileIcon(asset: String, size: CGFloat = 28) -> some View {
        Image(asset)
            .renderingMode(.template)
            .resizable()
            .scaledToFit()
            .frame(width: size, height: size)
            .foregroundStyle(.white)
    }
}

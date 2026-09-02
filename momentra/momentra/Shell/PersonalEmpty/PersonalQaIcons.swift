import SwiftUI

/// Figma / APK `ic_qa_*` assets for Personal Quick Add hub chrome and tiles.
enum PersonalQaIcons {
    static func close(size: CGFloat = 12) -> some View {
        Image("QaClose")
            .renderingMode(.template)
            .resizable()
            .scaledToFit()
            .frame(width: size, height: size)
            .foregroundStyle(Color(hex: "#E5E0EE"))
    }

    static func search(size: CGFloat = 15) -> some View {
        Image("QaSearch")
            .renderingMode(.template)
            .resizable()
            .scaledToFit()
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

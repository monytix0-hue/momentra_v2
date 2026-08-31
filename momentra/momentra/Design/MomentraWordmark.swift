import SwiftUI

struct MomentraWordmark: View {
    var showTagline: Bool = false
    var titleSize: CGFloat = 32
    var taglineSize: CGFloat = 9
    var alignStart: Bool = false

    var body: some View {
        VStack(alignment: alignStart ? .leading : .center, spacing: 4) {
            HStack(alignment: .top, spacing: 0) {
                Text("momentr")
                    .font(.system(size: titleSize, weight: .medium))
                    .foregroundColor(MomentraBrandTokens.textOnDark)
                    .kerning(-0.5)

                ZStack(alignment: .topTrailing) {
                    Text("a")
                        .font(.system(size: titleSize, weight: .medium))
                        .foregroundColor(MomentraBrandTokens.cta)
                        .kerning(-0.5)

                    Circle()
                        .fill(MomentraBrandTokens.progress)
                        .frame(width: max(5, titleSize * 0.22), height: max(5, titleSize * 0.22))
                        .offset(x: 2, y: -(titleSize * 0.28))
                }
            }

            if showTagline {
                Text("TOGETHER · FORWARD")
                    .font(.system(size: taglineSize, weight: .regular))
                    .tracking(2)
                    .foregroundColor(MomentraBrandTokens.textOnDark.opacity(0.38))
            }
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("Momentra")
    }
}

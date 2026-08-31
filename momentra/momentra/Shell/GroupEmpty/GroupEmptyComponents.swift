import SwiftUI

enum GroupEmptyTokens {
    static let bg = Color(hex: "#131313")
    static let text = Color(hex: "#E5E2E1")
    static let secondary = Color(hex: "#DFC0B4")
    static let orange = Color(hex: "#FF7A3D")
    static let orangeSoft = Color(hex: "#FFB598")
    static let ctaText = Color(hex: "#591C00")
    static let card = Color(hex: "#201F1F")
    static let border = Color.white.opacity(0.10)
    static let surfaceHigh = Color(hex: "#2A2A2A")
    static let heroOverlay = Color(hex: "#14121B")
}

struct GroupEmptyChapterLabel: View {
    let text: String

    var body: some View {
        Text(text.uppercased())
            .font(.system(size: 12, weight: .heavy))
            .kerning(2)
            .foregroundStyle(GroupEmptyTokens.orange)
    }
}

struct GroupEmptyOrangeCta: View {
    let label: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(label.uppercased())
                .font(.system(size: 14, weight: .bold))
                .kerning(0.5)
                .foregroundStyle(GroupEmptyTokens.ctaText)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 16)
                .background(
                    LinearGradient(
                        colors: [GroupEmptyTokens.orange, GroupEmptyTokens.orangeSoft],
                        startPoint: .leading,
                        endPoint: .trailing
                    ),
                    in: RoundedRectangle(cornerRadius: 12)
                )
        }
        .buttonStyle(.plain)
        .accessibilityLabel(label)
    }
}

struct GroupEmptyScanJoinButton: View {
    var action: () -> Void

    var body: some View {
        Button(action: action) {
            Text("Scan QR to join")
                .font(.plusJakarta(size: 14, weight: .bold))
                .foregroundStyle(GroupEmptyTokens.orange)
                .frame(maxWidth: .infinity)
                .frame(height: 48)
                .overlay(
                    RoundedRectangle(cornerRadius: 14)
                        .stroke(GroupEmptyTokens.orange, lineWidth: 1)
                )
        }
        .buttonStyle(.plain)
        .accessibilityLabel("Scan QR to join")
    }
}

/// Pixel-faithful Figma hero export (copy + CTA baked in).
struct GroupEmptyFigmaHeroExport: View {
    let imageName: String
    let aspectRatio: CGFloat
    let ctaLabel: String
    let action: () -> Void

    var body: some View {
        ZStack(alignment: .bottom) {
            Image(imageName)
                .resizable()
                .scaledToFill()
                .frame(maxWidth: .infinity)
                .aspectRatio(aspectRatio, contentMode: .fit)
                .clipped()

            Button(action: action) {
                Color.clear
                    .frame(height: 56)
                    .frame(maxWidth: .infinity)
                    .padding(.horizontal, 24)
                    .padding(.bottom, 24)
            }
            .buttonStyle(.plain)
            .accessibilityLabel(ctaLabel)
        }
    }
}

struct GroupEmptyHeroImage<Content: View>: View {
    let imageName: String
    var height: CGFloat = 420
    var overlayAlpha: CGFloat = 0.5
    @ViewBuilder let content: () -> Content

    var body: some View {
        ZStack(alignment: .bottomLeading) {
            Image(imageName)
                .resizable()
                .scaledToFill()
                .frame(maxWidth: .infinity)
                .frame(height: height)
                .clipped()

            GroupEmptyTokens.heroOverlay
                .opacity(overlayAlpha)

            VStack(alignment: .leading, spacing: 12) {
                content()
            }
            .padding(.horizontal, 24)
            .padding(.vertical, 32)
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .frame(height: height)
    }
}

struct GroupEmptyTypeCard: View {
    let imageName: String
    var badgeName: String? = "group_moments_type_badge"
    let title: String
    let bodyText: String
    var imageHeight: CGFloat = 192
    var comingSoon: Bool = false
    var action: (() -> Void)? = nil

    var body: some View {
        let card = VStack(alignment: .leading, spacing: 0) {
            ZStack(alignment: .topLeading) {
                Image(imageName)
                    .resizable()
                    .scaledToFill()
                    .frame(maxWidth: .infinity)
                    .frame(height: comingSoon ? 170 : imageHeight)
                    .clipped()
                    .opacity(comingSoon ? 0.5 : 1)

                if let badgeName, !comingSoon {
                    ZStack {
                        RoundedRectangle(cornerRadius: 8)
                            .fill(GroupEmptyTokens.orange.opacity(0.2))
                            .overlay(
                                RoundedRectangle(cornerRadius: 8)
                                    .stroke(GroupEmptyTokens.border, lineWidth: 1)
                            )
                        GroupEmptyAssetIcon(name: badgeName, size: 20)
                    }
                    .frame(width: 38, height: 38)
                    .padding(16)
                }
            }

            VStack(alignment: .leading, spacing: 8) {
                HStack(alignment: .top, spacing: 8) {
                    Text(title)
                        .font(.system(size: 18, weight: .semibold))
                        .foregroundStyle(GroupEmptyTokens.text)
                        .frame(maxWidth: .infinity, alignment: .leading)
                    if comingSoon {
                        Text("Coming Soon")
                            .font(.system(size: 11, weight: .semibold))
                            .foregroundStyle(Color(hex: "#9CA3AF"))
                    }
                }
                Text(bodyText)
                    .font(.system(size: 13))
                    .foregroundStyle(GroupEmptyTokens.secondary)
                    .fixedSize(horizontal: false, vertical: true)
            }
            .padding(comingSoon ? 20 : 24)
        }
        .background(GroupEmptyTokens.card, in: RoundedRectangle(cornerRadius: comingSoon ? 22 : 24))
        .overlay(
            RoundedRectangle(cornerRadius: comingSoon ? 22 : 24)
                .stroke(GroupEmptyTokens.border, lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: comingSoon ? 22 : 24))

        if let action {
            Button(action: action) { card }
                .buttonStyle(.plain)
        } else {
            card
        }
    }
}

struct GroupEmptyMomentTypeGrid: View {
    var onSelectExperience: (() -> Void)? = nil
    var onSelectPurchase: (() -> Void)? = nil
    var onSelectLiving: (() -> Void)? = nil

    var body: some View {
        VStack(spacing: 12) {
            HStack(alignment: .top, spacing: 12) {
                GroupEmptyTypeCard(
                    imageName: "group_moments_type_experience",
                    title: "Experience",
                    bodyText: "Trips, weddings, celebrations, outings and events.",
                    action: onSelectExperience
                )
                GroupEmptyTypeCard(
                    imageName: "group_moments_type_purchase",
                    title: "Purchase",
                    bodyText: "Plan, fund and track something together.",
                    action: onSelectPurchase
                )
            }
            HStack(alignment: .top, spacing: 12) {
                GroupEmptyTypeCard(
                    imageName: "group_moments_type_living",
                    title: "Living",
                    bodyText: "Coordinate a home, routine or shared space.",
                    action: onSelectLiving
                )
                GroupEmptyTypeCard(
                    imageName: "group_moments_type_goal",
                    badgeName: nil,
                    title: "Goal",
                    bodyText: "Turn a shared ambition into steady progress.",
                    comingSoon: true
                )
            }
            GroupEmptyTypeCard(
                imageName: "group_moments_type_community",
                title: "Community",
                bodyText: "Bring a wider circle around a common purpose.",
                comingSoon: true
            )
        }
    }
}

struct GroupEmptyAppear: ViewModifier {
    var delay: Double = 0
    @State private var shown = false

    func body(content: Content) -> some View {
        content
            .opacity(shown ? 1 : 0)
            .offset(y: shown ? 0 : 18)
            .onAppear {
                withAnimation(.easeOut(duration: 0.42).delay(delay)) {
                    shown = true
                }
            }
    }
}

extension View {
    func groupEmptyAppear(delay: Double = 0) -> some View {
        modifier(GroupEmptyAppear(delay: delay))
    }
}

struct GroupEmptyShimmerBar: View {
    let accent: Color
    @State private var phase: CGFloat = -1

    var body: some View {
        GeometryReader { geo in
            Capsule()
                .fill(accent.opacity(0.22))
                .overlay(alignment: .leading) {
                    Capsule()
                        .fill(
                            LinearGradient(
                                colors: [.clear, accent.opacity(0.85), .clear],
                                startPoint: .leading,
                                endPoint: .trailing
                            )
                        )
                        .frame(width: geo.size.width * 0.4)
                        .offset(x: phase * geo.size.width)
                }
                .clipShape(Capsule())
        }
        .frame(height: 6)
        .onAppear {
            withAnimation(.linear(duration: 1.6).repeatForever(autoreverses: false)) {
                phase = 1.2
            }
        }
    }
}

struct GroupEmptyFigmaCardExport: View {
    let imageName: String
    let aspectRatio: CGFloat

    var body: some View {
        Image(imageName)
            .resizable()
            .scaledToFill()
            .aspectRatio(aspectRatio, contentMode: .fit)
            .clipShape(RoundedRectangle(cornerRadius: 24))
    }
}

struct GroupEmptyFeatureRow: View {
    let iconName: String
    let title: String
    let bodyText: String

    var body: some View {
        HStack(alignment: .top, spacing: 16) {
            ZStack {
                RoundedRectangle(cornerRadius: 12)
                    .fill(GroupEmptyTokens.orange.opacity(0.1))
                    .frame(width: 48, height: 48)
                GroupEmptyAssetIcon(name: iconName, size: 22)
            }

            VStack(alignment: .leading, spacing: 4) {
                Text(title.uppercased())
                    .font(.system(size: 14, weight: .bold))
                    .kerning(0.5)
                    .foregroundStyle(GroupEmptyTokens.text)
                Text(bodyText)
                    .font(.system(size: 16))
                    .foregroundStyle(GroupEmptyTokens.secondary)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
        .padding(24)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(GroupEmptyTokens.card, in: RoundedRectangle(cornerRadius: 16))
        .overlay(
            RoundedRectangle(cornerRadius: 16)
                .stroke(GroupEmptyTokens.border, lineWidth: 1)
        )
    }
}

struct GroupEmptyAssetIcon: View {
    let name: String
    var size: CGFloat = 24

    var body: some View {
        Image(name)
            .resizable()
            .scaledToFit()
            .frame(width: size, height: size)
    }
}

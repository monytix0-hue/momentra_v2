import SwiftUI

/// Figma frame Android / Onboarding / Product * — 402 × 874
private let designW: CGFloat = 402
private let designH: CGFloat = 874
private let productMidnight = Color(red: 5 / 255, green: 8 / 255, blue: 22 / 255)
private let ctaOrange = Color(hex: "#FF7A3D")

private struct ProductPageModel: Identifiable {
    let id: Int
    let imageName: String
    let illustrationTop: CGFloat
    let illustrationSize: CGFloat
    let copyTop: CGFloat
    let eyebrow: String
    let eyebrowColor: Color
    let title: String
    let body: String
    let analyticsScreen: String
    let cta: String
}

private let productPages: [ProductPageModel] = [
    .init(
        id: 0,
        imageName: "OnboardingIllPulse",
        illustrationTop: 118,
        illustrationSize: 340,
        copyTop: 492,
        eyebrow: "YOUR DAILY PULSE",
        eyebrowColor: MomentraBrandTokens.amber500,
        title: "Everything important,\nat a glance.",
        body: "Money, people, goals, and work come together in one calm view.",
        analyticsScreen: AnalyticsScreens.onboardingProduct1,
        cta: "Next"
    ),
    .init(
        id: 1,
        imageName: "OnboardingIllMoments",
        illustrationTop: 112,
        illustrationSize: 340,
        copyTop: 488,
        eyebrow: "CAPTURE THE MOMENT",
        eyebrowColor: MomentraBrandTokens.teal500,
        title: "Add it once.\nRemember it better.",
        body: "A spend, a feeling, a task, or a memory—Momentra keeps the context with it.",
        analyticsScreen: AnalyticsScreens.onboardingProduct2,
        cta: "Next"
    ),
    .init(
        id: 2,
        imageName: "OnboardingIllLifeMemory",
        illustrationTop: 106,
        illustrationSize: 350,
        copyTop: 488,
        eyebrow: "SEE WHAT'S NEXT",
        eyebrowColor: MomentraBrandTokens.amber500,
        title: "Your story\nbecomes insight.",
        body: "Life shows the shape of today. Memory learns the patterns that help tomorrow.",
        analyticsScreen: AnalyticsScreens.onboardingProduct3,
        cta: "Get Started"
    ),
]

/// Figma product education carousel before the cinematic particle welcome.
struct ProductOnboardingView: View {
    var onContinueToCinematic: () -> Void
    @State private var page = 0

    private var current: ProductPageModel { productPages[page] }

    var body: some View {
        GeometryReader { geo in
            let scale = min(geo.size.width / designW, geo.size.height / designH)
            let s: (CGFloat) -> CGFloat = { $0 * scale }

            ZStack(alignment: .topLeading) {
                productMidnight.ignoresSafeArea()

                // Atmosphere
                Circle()
                    .fill(MomentraBrandTokens.indigo700.opacity(0.55))
                    .frame(width: s(300), height: s(300))
                    .blur(radius: s(40))
                    .position(
                        x: geo.size.width / 2,
                        y: s(current.illustrationTop - 20) + s(150)
                    )

                // Illustration — Figma centered horizontally
                Image(current.imageName)
                    .resizable()
                    .scaledToFit()
                    .frame(width: s(current.illustrationSize), height: s(current.illustrationSize))
                    .position(
                        x: geo.size.width / 2,
                        y: s(current.illustrationTop) + s(current.illustrationSize) / 2
                    )

                // Copy block
                VStack(spacing: s(10)) {
                    Text(current.eyebrow)
                        .font(.system(size: s(11), weight: .semibold))
                        .tracking(1.4 * scale)
                        .foregroundStyle(current.eyebrowColor)
                    Text(current.title)
                        .font(.system(size: s(30), weight: .bold))
                        .foregroundStyle(MomentraBrandTokens.textOnDark)
                        .multilineTextAlignment(.center)
                        .lineSpacing(s(6))
                        .frame(width: s(354))
                    Text(current.body)
                        .font(.system(size: s(15), weight: .regular))
                        .foregroundStyle(MomentraBrandTokens.textOnDark.opacity(0.62))
                        .multilineTextAlignment(.center)
                        .lineSpacing(s(4))
                        .frame(width: s(330))
                }
                .position(
                    x: geo.size.width / 2,
                    y: s(current.copyTop) + s(95)
                )

                // Brand mark — left 24, top 40
                Image("OnboardingBrandMark")
                    .resizable()
                    .scaledToFit()
                    .frame(width: s(28), height: s(28))
                    .position(x: s(24) + s(14), y: s(40) + s(14))

                // Skip
                Button("Skip") {
                    trackWidget(
                        screenName: AnalyticsScreens.onboarding,
                        widgetName: AnalyticsWidgets.onboardingSkip,
                        action: "tap"
                    )
                    onContinueToCinematic()
                }
                .font(.system(size: s(13), weight: .medium))
                .foregroundStyle(MomentraBrandTokens.textOnDark.opacity(0.58))
                .position(x: geo.size.width - s(24) - s(20), y: s(43) + s(12))
                .accessibilityIdentifier("onboarding.skip")
            }
            .frame(width: geo.size.width, height: geo.size.height)
            .safeAreaInset(edge: .bottom) {
                VStack(spacing: s(16)) {
                    HStack(spacing: s(8)) {
                        ForEach(0..<productPages.count, id: \.self) { i in
                            if i == page {
                                Capsule()
                                    .fill(ctaOrange)
                                    .frame(width: s(22), height: s(8))
                            } else {
                                Circle()
                                    .fill(Color(hex: "#3A3558"))
                                    .frame(width: s(8), height: s(8))
                            }
                        }
                    }
                    .frame(height: s(8))

                    Button {
                        trackWidget(
                            screenName: AnalyticsScreens.onboarding,
                            widgetName: page == productPages.count - 1
                                ? AnalyticsWidgets.onboardingGetStarted
                                : AnalyticsWidgets.onboardingNext,
                            action: "tap"
                        )
                        if page < productPages.count - 1 {
                            withAnimation(.easeInOut(duration: 0.2)) { page += 1 }
                        } else {
                            onContinueToCinematic()
                        }
                    } label: {
                        Text(current.cta)
                            .font(.system(size: s(17), weight: .semibold))
                            .foregroundStyle(.white)
                            .frame(width: s(320), height: s(52))
                            .background(ctaOrange, in: Capsule())
                    }
                    .accessibilityLabel(current.cta)
                }
                .padding(.bottom, s(24))
                .frame(maxWidth: .infinity)
            }
        }
        .preferredColorScheme(.dark)
        .id(current.analyticsScreen)
        .trackScreen(current.analyticsScreen)
    }
}

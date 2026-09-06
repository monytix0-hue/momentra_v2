import SwiftUI

struct QuickTourView: View {
    @StateObject private var tourGuide: TourGuide

    init() {
        let guide = TourGuide()
        guide.steps = [
            .createMoment,
            .activateMoment,
            .quickaddsTab,
            .personalQuickadds,
            .businessQuickadds,
            .groupQuickadds,
        ]
        guide.isPresented = true
        _tourGuide = StateObject(wrappedValue: guide)
    }

    var body: some View {
        NavigationStack {
            ZStack(alignment: .topTrailing) {
                Color(hex: "#1C233D")
                    .ignoresSafeArea()

                VStack(spacing: 24) {
                    HStack {
                        Spacer()
                        Button("Done") {
                            tourGuide.skip()
                        }
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundStyle(.white)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 6)
                        .background(Color.gray.opacity(0.3))
                        .cornerRadius(20)
                    }

                    if let step = tourGuide.currentStep {
                        VStack(spacing: 16) {
                            Text(step.title)
                                .font(.system(size: 22, weight: .bold))
                                .foregroundStyle(.white)
                                .multilineTextAlignment(.center)

                            Text(step.message)
                                .font(.system(size: 15, weight: .regular))
                                .foregroundStyle(.white.opacity(0.85))
                                .multilineTextAlignment(.center)
                                .fixedSize(horizontal: false, vertical: true)
                                .padding(.horizontal, 24)

                            if let action = step.action {
                                Button("Let's Go") {
                                    action()
                                    tourGuide.next()
                                }
                                .font(.system(size: 16, weight: .semibold))
                                .foregroundStyle(.white)
                                .frame(width: 200, height: 44)
                                .background(MomentraBrandTokens.ember500, in: Capsule())
                                .padding(.top, 8)
                            }
                        }
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                        .overlay(alignment: .topTrailing) {
                            if tourGuide.isPresented && !tourGuide.isFinished {
                                QuickTourHighlightOverlay(step: step)
                            }
                        }
                    }
                }
                .padding(24)
                .background(Color(hex: "#1C233D"))
                .cornerRadius(20)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            }
            .navigationBarTitleDisplayMode(.inline)
            .toolbar(.hidden, for: .navigationBar)
        }
        .onAppear {
            trackWidget(
                screenName: AnalyticsScreens.onboarding,
                widgetName: AnalyticsWidgets.onboardingQuickTour,
                action: "view"
            )
        }
        .onDisappear {
            trackWidget(
                screenName: AnalyticsScreens.onboarding,
                widgetName: AnalyticsWidgets.onboardingQuickTour,
                action: "dismiss"
            )
        }
    }
}

private struct QuickTourHighlightOverlay: View {
    let step: TourStep

    var body: some View {
        GeometryReader { geo in
            ZStack {
                Color.black.opacity(0.4)
                    .ignoresSafeArea()
                    .mask(
                        Group {
                            Rectangle()
                                .frame(height: 80)
                                .offset(y: -geo.safeAreaInsets.top)
                        }
                    )

                TourArrowTriangle()
                    .fill(Color.white)
                    .frame(width: 20, height: 20)
                    .offset(arrowOffset(for: step.arrowPosition))
            }
        }
    }

    private func arrowOffset(for edge: Edge) -> CGSize {
        switch edge {
        case .top: return CGSize(width: 0, height: -10)
        case .bottom: return CGSize(width: 0, height: 10)
        case .leading: return CGSize(width: -10, height: 0)
        case .trailing: return CGSize(width: 10, height: 0)
        @unknown default: return .zero
        }
    }
}

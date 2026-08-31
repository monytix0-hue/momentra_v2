import SwiftUI

/// Figma 575:8967 — Group / Pulse empty
struct GroupPulseEmptyView: View {
    var onStartCta: () -> Void
    var onSelectExperience: () -> Void = {}
    var onSelectPurchase: () -> Void = {}
    var onSelectLiving: () -> Void = {}
    var onJoinCode: (String) -> Void = { _ in }

    @State private var showScanner = false

    var body: some View {
        ScrollView {
            VStack(spacing: 0) {
                GroupEmptyFigmaHeroExport(
                    imageName: "group_pulse_hero",
                    aspectRatio: 402 / 560,
                    ctaLabel: "Begin Story",
                    action: onStartCta
                )
                .groupEmptyAppear()

                VStack(alignment: .leading, spacing: 16) {
                    GroupEmptyChapterLabel(text: "Chapter 02 / The Matrix")
                    Text("Select Your Arena")
                        .font(.system(size: 24, weight: .bold))
                        .foregroundStyle(GroupEmptyTokens.text)
                    GroupEmptyMomentTypeGrid(
                        onSelectExperience: onSelectExperience,
                        onSelectPurchase: onSelectPurchase,
                        onSelectLiving: onSelectLiving
                    )
                    GroupEmptyScanJoinButton { showScanner = true }
                }
                .padding(.horizontal, 24)
                .padding(.vertical, 32)
                .groupEmptyAppear(delay: 0.09)

                VStack(alignment: .leading, spacing: 24) {
                    GroupEmptyChapterLabel(text: "Chapter 03 / Why Momentra")
                    Text("Why Groups Use Momentra")
                        .font(.system(size: 26, weight: .heavy))
                        .foregroundStyle(GroupEmptyTokens.text)
                        .fixedSize(horizontal: false, vertical: true)

                    GroupEmptyFeatureRow(
                        iconName: "group_pulse_feature_icon",
                        title: "Coordinate Together",
                        bodyText: "Keep people, plans and money aligned in real-time."
                    )
                    GroupEmptyFeatureRow(
                        iconName: "group_pulse_feature_icon",
                        title: "Manage Shared Money",
                        bodyText: "Track contributions, spending and settlements effortlessly."
                    )
                    GroupEmptyFeatureRow(
                        iconName: "group_pulse_feature_icon",
                        title: "Stay Organized",
                        bodyText: "Plans, tasks and updates all in one unified dashboard."
                    )
                    GroupEmptyFeatureRow(
                        iconName: "group_pulse_feature_icon",
                        title: "Remember Together",
                        bodyText: "Capture milestones, updates and memories as they happen."
                    )
                }
                .padding(.horizontal, 24)
                .padding(.vertical, 48)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(GroupEmptyTokens.card)
                .groupEmptyAppear(delay: 0.18)
            }
        }
        .background(GroupEmptyTokens.bg.ignoresSafeArea())
        .fullScreenCover(isPresented: $showScanner) {
            GroupJoinQrScanner(
                onCode: { code in
                    showScanner = false
                    onJoinCode(code)
                },
                onDismiss: { showScanner = false }
            )
        }
    }
}

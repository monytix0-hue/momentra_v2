import SwiftUI

/// Figma 575:8553 — Group / Moments empty
struct GroupMomentsEmptyView: View {
    var onStartCta: () -> Void

    private let timelineSteps: [(Int, String, String, Bool)] = [
        (1, "Create a Moment", "Set your objective with friends or family.", true),
        (2, "Invite Your People", "Share the link to bring everyone on board.", false),
        (3, "Plan & Contribute", "Pool money, schedule tasks, and lock dates.", true),
        (4, "Stay In Sync", "Evolve plans fluidly as life happens.", false),
        (5, "Keep Memories", "Every shared journey archives naturally.", true),
    ]

    var body: some View {
        ScrollView {
            VStack(spacing: 0) {
                GroupEmptyFigmaHeroExport(
                    imageName: "group_moments_hero",
                    aspectRatio: 402 / 520,
                    ctaLabel: "Start Your First Moment",
                    action: onStartCta
                )
                .groupEmptyAppear()

                VStack(alignment: .leading, spacing: 16) {
                    GroupEmptyChapterLabel(text: "Chapter 02 / Type Selection")
                    Text("Moment Types")
                        .font(.system(size: 24, weight: .bold))
                        .foregroundStyle(GroupEmptyTokens.text)
                    GroupEmptyMomentTypeGrid()
                }
                .padding(.horizontal, 24)
                .padding(.vertical, 40)
                .groupEmptyAppear(delay: 0.09)

                VStack(alignment: .leading, spacing: 16) {
                    GroupEmptyChapterLabel(text: "Chapter 03 / How It Works")
                    Text("Your journey, mapped.")
                        .font(.system(size: 24, weight: .bold))
                        .foregroundStyle(GroupEmptyTokens.text)
                    Text("Follow the path from idea to memory - with a little magic along the way.")
                        .font(.system(size: 15))
                        .foregroundStyle(GroupEmptyTokens.secondary)
                        .fixedSize(horizontal: false, vertical: true)

                    ForEach(timelineSteps, id: \.0) { step in
                        timelineStep(number: step.0, title: step.1, body: step.2, leftAligned: step.3)
                    }

                    Text("Every shared story starts with a single moment.")
                        .font(.system(size: 14))
                        .foregroundStyle(GroupEmptyTokens.secondary)
                        .multilineTextAlignment(.center)
                        .frame(maxWidth: .infinity)
                        .padding(.top, 16)
                }
                .padding(.horizontal, 24)
                .padding(.bottom, 48)
                .groupEmptyAppear(delay: 0.18)
            }
        }
        .background(GroupEmptyTokens.bg.ignoresSafeArea())
    }

    @ViewBuilder
    private func timelineStep(number: Int, title: String, body: String, leftAligned: Bool) -> some View {
        HStack(alignment: .center, spacing: 16) {
            if leftAligned {
                stepBadge(number)
                stepCard(title: title, body: body)
            } else {
                stepCard(title: title, body: body)
                stepBadge(number)
            }
        }
        .frame(maxWidth: .infinity, alignment: leftAligned ? .leading : .trailing)
    }

    private func stepBadge(_ number: Int) -> some View {
        Text("\(number)")
            .font(.system(size: 14, weight: .bold))
            .foregroundStyle(GroupEmptyTokens.ctaText)
            .frame(width: 36, height: 36)
            .background(GroupEmptyTokens.orange, in: Circle())
    }

    private func stepCard(title: String, body: String) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title.uppercased())
                .font(.system(size: 13, weight: .bold))
                .kerning(0.5)
                .foregroundStyle(GroupEmptyTokens.text)
            Text(body)
                .font(.system(size: 14))
                .foregroundStyle(GroupEmptyTokens.secondary)
                .fixedSize(horizontal: false, vertical: true)
        }
        .padding(20)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(GroupEmptyTokens.card, in: RoundedRectangle(cornerRadius: 16))
        .overlay(
            RoundedRectangle(cornerRadius: 16)
                .stroke(GroupEmptyTokens.border, lineWidth: 1)
        )
    }
}

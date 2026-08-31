import SwiftUI

/// Figma 575:8838 — Group / Memory empty
struct GroupMemoryEmptyView: View {
    var onStartCta: () -> Void

    private struct LearnItem {
        let iconName: String
        let gradient: [Color]
        let cardTint: Color
        let title: String
        let body: String
    }

    private let learnItems: [LearnItem] = [
        LearnItem(
            iconName: "group_memory_icon_calendar",
            gradient: [Color(hex: "#FFD8A8"), Color(hex: "#FFB598")],
            cardTint: Color(hex: "#FFB598").opacity(0.2),
            title: "Shared rituals",
            body: "The moments and recurring plans your group returns to."
        ),
        LearnItem(
            iconName: "group_memory_icon_award",
            gradient: [Color(hex: "#F2CA50"), Color(hex: "#F2CA50")],
            cardTint: Color(hex: "#F2CA50").opacity(0.2),
            title: "Milestones",
            body: "The landmark achievements that define your journey."
        ),
        LearnItem(
            iconName: "group_memory_icon_users",
            gradient: [Color(hex: "#FFB598"), Color(hex: "#FF7A3D")],
            cardTint: Color(hex: "#FF7A3D").opacity(0.2),
            title: "People and roles",
            body: "Reveal how everyone shows up and supports each other."
        ),
        LearnItem(
            iconName: "group_memory_icon_map_pin",
            gradient: [Color(hex: "#FFB598"), Color(hex: "#FFD8A8")],
            cardTint: Color(hex: "#FFD8A8").opacity(0.2),
            title: "Places and patterns",
            body: "Map where your group's strongest memories are made."
        ),
    ]

    var body: some View {
        ScrollView {
            VStack(spacing: 0) {
                GroupEmptyFigmaHeroExport(
                    imageName: "group_memory_hero",
                    aspectRatio: 402 / 550,
                    ctaLabel: "Create Your First Memory",
                    action: onStartCta
                )
                .groupEmptyAppear()

                VStack(alignment: .leading, spacing: 12) {
                    GroupEmptyChapterLabel(text: "Chapter 02 / Preservation")
                    Text("What We Learn")
                        .font(.system(size: 24, weight: .bold))
                        .foregroundStyle(GroupEmptyTokens.text)

                    ForEach(learnItems.indices, id: \.self) { index in
                        learnCard(learnItems[index])
                    }

                    VStack(spacing: 12) {
                        Text("Your memory begins with the first shared moment.")
                            .font(.system(size: 18, weight: .semibold))
                            .foregroundStyle(GroupEmptyTokens.text)
                            .multilineTextAlignment(.center)
                            .frame(maxWidth: .infinity)
                        Text("Create something together and Momentra will preserve what matters.")
                            .font(.system(size: 14))
                            .foregroundStyle(GroupEmptyTokens.secondary)
                            .multilineTextAlignment(.center)
                            .frame(maxWidth: .infinity)
                    }
                    .padding(.top, 24)
                }
                .padding(.horizontal, 24)
                .padding(.vertical, 40)
                .groupEmptyAppear(delay: 0.12)
            }
        }
        .background(GroupEmptyTokens.bg.ignoresSafeArea())
    }

    private func learnCard(_ item: LearnItem) -> some View {
        HStack(alignment: .top, spacing: 16) {
            ZStack {
                RoundedRectangle(cornerRadius: 16)
                    .fill(
                        LinearGradient(
                            colors: item.gradient,
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                    )
                    .frame(width: 56, height: 56)
                GroupEmptyAssetIcon(name: item.iconName, size: 24)
            }

            VStack(alignment: .leading, spacing: 6) {
                Text(item.title)
                    .font(.system(size: 14, weight: .bold))
                    .kerning(0.5)
                    .foregroundStyle(GroupEmptyTokens.text)
                Text(item.body)
                    .font(.system(size: 16))
                    .foregroundStyle(GroupEmptyTokens.secondary)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
        .padding(20)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            LinearGradient(
                colors: [item.cardTint, GroupEmptyTokens.card],
                startPoint: .leading,
                endPoint: .trailing
            ),
            in: RoundedRectangle(cornerRadius: 18)
        )
        .overlay(
            RoundedRectangle(cornerRadius: 18)
                .stroke(GroupEmptyTokens.border, lineWidth: 1)
        )
    }
}

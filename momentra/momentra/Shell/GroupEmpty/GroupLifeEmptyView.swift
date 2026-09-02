import SwiftUI

/// Figma 575:8660 — Group / Life empty
struct GroupLifeEmptyView: View {
    var onStartCta: () -> Void

    private struct DimensionItem {
        let iconName: String
        let accent: Color
        let title: String
        let fullWidth: Bool
    }

    private struct UnlockItem {
        let iconName: String
        let accent: Color
        let title: String
        let body: String
    }

    private struct WhyItem {
        let badge: String
        let title: String
        let body: String
    }

    private let dimensions: [DimensionItem] = [
        DimensionItem(iconName: "group_life_sparkles", accent: GroupEmptyTokens.orange, title: "Experience", fullWidth: false),
        DimensionItem(iconName: "group_life_icon_wallet", accent: Color(hex: "#60A5FA"), title: "Purchase", fullWidth: false),
        DimensionItem(iconName: "group_life_icon_calendar", accent: Color(hex: "#10B981"), title: "Living", fullWidth: false),
        DimensionItem(iconName: "group_life_icon_award", accent: Color(hex: "#EC4899"), title: "Goal", fullWidth: false),
        DimensionItem(iconName: "group_life_icon_users", accent: Color(hex: "#8B5CF6"), title: "Community", fullWidth: true),
    ]

    private let unlocks: [UnlockItem] = [
        UnlockItem(iconName: "group_life_icon_users", accent: GroupEmptyTokens.orange, title: "Participation patterns", body: "Who powers the pulse and drives consistent action."),
        UnlockItem(iconName: "group_life_icon_wallet", accent: Color(hex: "#14B8A6"), title: "Contribution balance", body: "Keep spending, funding, and splitting completely fair."),
        UnlockItem(iconName: "group_life_icon_calendar", accent: Color(hex: "#F2CA50"), title: "Coordination health", body: "Real-time updates and seamless timeline synchronization."),
        UnlockItem(iconName: "group_life_icon_award", accent: Color(hex: "#8B5CF6"), title: "Shared achievement", body: "Milestones unlocked together as a united group."),
        UnlockItem(iconName: "group_life_icon_sparkles", accent: Color(hex: "#60A5FA"), title: "Memory evolution", body: "Watch how your shared traditions and rituals grow."),
    ]

    private let whyItems: [WhyItem] = [
        WhyItem(badge: "01", title: "Experience", body: "Builds connection through shared time."),
        WhyItem(badge: "02", title: "Purchase", body: "Makes collective decisions and money visible."),
        WhyItem(badge: "03", title: "Living", body: "Turns everyday coordination into clarity."),
        WhyItem(badge: "04", title: "Goal", body: "Keeps ambition measurable and shared."),
        WhyItem(badge: "05", title: "Community", body: "Shows how your group contributes beyond itself."),
    ]

    var body: some View {
        NativeDashboardScaffold(background: GroupEmptyTokens.bg) {
            NativeListSection(insets: EdgeInsets()) {
                GroupEmptyFigmaHeroExport(
                    imageName: "group_life_hero",
                    aspectRatio: 402 / 351,
                    ctaLabel: "Create First Group Moment",
                    action: onStartCta
                )
                .groupEmptyAppear()
            }

            NativeListSection(insets: EdgeInsets(top: 4, leading: 24, bottom: 4, trailing: 24)) {
                VStack(alignment: .leading, spacing: 16) {
                    GroupEmptyChapterLabel(text: "Chapter 02 / Lifeline Dimensions")
                    Text("Activate Your Dimensions")
                        .font(.system(size: 24, weight: .bold))
                        .foregroundStyle(GroupEmptyTokens.text)

                    HStack(alignment: .top, spacing: 12) {
                        dimensionCard(dimensions[0], action: onStartCta)
                        dimensionCard(dimensions[1], action: onStartCta)
                    }
                    HStack(alignment: .top, spacing: 12) {
                        dimensionCard(dimensions[2], action: onStartCta)
                        dimensionCard(dimensions[3], action: onStartCta)
                    }
                    dimensionCard(dimensions[4], action: onStartCta)
                }
                .padding(.vertical, 32)
                .groupEmptyAppear(delay: 0.08)
            }

            NativeListSection(insets: EdgeInsets(top: 4, leading: 24, bottom: 4, trailing: 24)) {
                VStack(alignment: .leading, spacing: 12) {
                    GroupEmptyChapterLabel(text: "Chapter 03 / System Secrets")
                    Text("Intelligence Unlocks")
                        .font(.system(size: 24, weight: .bold))
                        .foregroundStyle(GroupEmptyTokens.text)

                    ForEach(unlocks.indices, id: \.self) { index in
                        unlockCard(unlocks[index])
                    }

                    Text("Insights unlock naturally as your group creates and completes moments together.")
                        .font(.system(size: 13))
                        .foregroundStyle(GroupEmptyTokens.secondary)
                        .padding(.top, 8)
                }
                .padding(.bottom, 24)
            }

            NativeListSection(insets: EdgeInsets(top: 4, leading: 24, bottom: 48, trailing: 24)) {
                VStack(alignment: .leading, spacing: 12) {
                    GroupEmptyChapterLabel(text: "Chapter 04 / Philosophy")
                    Text("Why These Matter")
                        .font(.system(size: 24, weight: .bold))
                        .foregroundStyle(GroupEmptyTokens.text)

                    ForEach(whyItems.indices, id: \.self) { index in
                        whyRow(whyItems[index])
                    }

                    Button(action: onStartCta) {
                        HStack(spacing: 10) {
                            Text("Explore Group Types")
                                .font(.system(size: 15, weight: .bold))
                                .foregroundStyle(GroupEmptyTokens.ctaText)
                            GroupEmptyAssetIcon(name: "group_life_arrow_right", size: 18)
                        }
                        .frame(maxWidth: .infinity)
                        .frame(height: 56)
                        .background(
                            LinearGradient(
                                colors: [GroupEmptyTokens.orange, GroupEmptyTokens.orangeSoft],
                                startPoint: .leading,
                                endPoint: .trailing
                            ),
                            in: RoundedRectangle(cornerRadius: 18)
                        )
                    }
                    .buttonStyle(.plain)
                    .padding(.top, 12)
                    .accessibilityLabel("Explore Group Types")
                }
            }
        }
        .background(GroupEmptyTokens.bg.ignoresSafeArea())
    }

    private func dimensionCard(_ item: DimensionItem, action: @escaping () -> Void) -> some View {
        VStack(spacing: 0) {
            ZStack {
                RoundedRectangle(cornerRadius: 12)
                    .fill(item.accent.opacity(0.18))
                    .frame(width: 44, height: 44)
                GroupEmptyAssetIcon(name: item.iconName, size: 22)
            }
            .padding(.top, 4)

            Spacer(minLength: 8)

            VStack(spacing: 2) {
                Text(item.title)
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(GroupEmptyTokens.text)
                Text("Inactive")
                    .font(.system(size: 12))
                    .foregroundStyle(GroupEmptyTokens.secondary.opacity(0.7))
            }

            Spacer(minLength: 8)

            Button(action: action) {
                HStack(spacing: 6) {
                    GroupEmptyAssetIcon(name: "group_life_sparkles", size: 14)
                    Text("Set Up")
                        .font(.system(size: 12, weight: .semibold))
                }
                .foregroundStyle(item.accent)
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
                .background(item.accent.opacity(0.2), in: RoundedRectangle(cornerRadius: 16))
            }
            .buttonStyle(.plain)
            .padding(.bottom, 4)
        }
        .frame(maxWidth: .infinity)
        .frame(height: 160)
        .padding(16)
        .background(GroupEmptyTokens.card, in: RoundedRectangle(cornerRadius: 18))
        .overlay(
            RoundedRectangle(cornerRadius: 18)
                .stroke(GroupEmptyTokens.border, lineWidth: 1)
        )
    }

    private func unlockCard(_ item: UnlockItem) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 12) {
                ZStack {
                    RoundedRectangle(cornerRadius: 12)
                        .fill(item.accent.opacity(0.2))
                        .frame(width: 48, height: 48)
                    GroupEmptyAssetIcon(name: item.iconName, size: 22)
                }
                ZStack {
                    Circle()
                        .fill(Color.white.opacity(0.08))
                        .frame(width: 36, height: 36)
                    GroupEmptyAssetIcon(name: "group_life_icon_lock", size: 18)
                }
            }
            Text(item.title)
                .font(.system(size: 15, weight: .bold))
                .foregroundStyle(GroupEmptyTokens.text)
            Text(item.body)
                .font(.system(size: 14))
                .foregroundStyle(GroupEmptyTokens.secondary)
                .fixedSize(horizontal: false, vertical: true)
            GroupEmptyShimmerBar(accent: item.accent)
        }
        .padding(20)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(GroupEmptyTokens.card, in: RoundedRectangle(cornerRadius: 20))
        .overlay(
            RoundedRectangle(cornerRadius: 20)
                .stroke(item.accent.opacity(0.35), lineWidth: 1)
        )
    }

    private func whyRow(_ item: WhyItem) -> some View {
        HStack(alignment: .center, spacing: 12) {
            Text(item.badge)
                .font(.system(size: 11, weight: .bold))
                .foregroundStyle(GroupEmptyTokens.orange)
                .frame(width: 32, height: 32)
                .background(GroupEmptyTokens.orange.opacity(0.2), in: Circle())

            VStack(alignment: .leading, spacing: 4) {
                Text(item.title)
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(GroupEmptyTokens.text)
                Text(item.body)
                    .font(.system(size: 13))
                    .foregroundStyle(GroupEmptyTokens.secondary)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(GroupEmptyTokens.card, in: RoundedRectangle(cornerRadius: 18))
        .overlay(
            RoundedRectangle(cornerRadius: 18)
                .stroke(GroupEmptyTokens.orangeSoft.opacity(0.2), lineWidth: 1)
        )
    }
}

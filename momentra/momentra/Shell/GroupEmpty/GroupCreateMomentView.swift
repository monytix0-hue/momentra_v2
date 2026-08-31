import SwiftUI

/// Figma 575:8894 — Group Create / Choose a Moment
struct GroupCreateMomentView: View {
    var onBack: () -> Void
    var onSelectExperience: () -> Void = {}
    var onSelectPurchase: () -> Void = {}
    var onSelectLiving: () -> Void = {}
    var onJoinCode: (String) -> Void = { _ in }

    @State private var showScanner = false

    private struct BenefitItem {
        let iconName: String
        let border: Color
        let label: String
    }

    private let benefits: [BenefitItem] = [
        BenefitItem(iconName: "group_create_benefit_plan", border: Color(hex: "#F59E0B"), label: "Plan"),
        BenefitItem(iconName: "group_create_benefit_contribute", border: Color(hex: "#FB7185"), label: "Contribute"),
        BenefitItem(iconName: "group_create_benefit_coordinate", border: Color(hex: "#14B8A6"), label: "Coordinate"),
        BenefitItem(iconName: "group_create_benefit_remember", border: Color(hex: "#8B5CF6"), label: "Remember"),
    ]

    var body: some View {
        ScrollView {
            VStack(spacing: 0) {
                Button(action: onBack) {
                    HStack(spacing: 12) {
                        Text("‹")
                            .font(.system(size: 32))
                            .foregroundStyle(GroupEmptyTokens.text)
                        Text("Choose a Moment")
                            .font(.system(size: 17, weight: .semibold))
                            .foregroundStyle(GroupEmptyTokens.text)
                        Spacer(minLength: 0)
                    }
                    .padding(.horizontal, 20)
                    .padding(.top, 18)
                    .padding(.bottom, 16)
                }
                .buttonStyle(.plain)
                .accessibilityLabel("Back")

                VStack(alignment: .leading, spacing: 24) {
                    VStack(alignment: .leading, spacing: 10) {
                        Text("Plan life together, in one place.")
                            .font(.system(size: 30, weight: .bold))
                            .foregroundStyle(GroupEmptyTokens.text)
                            .fixedSize(horizontal: false, vertical: true)
                        Text("Create a moment to plan, contribute, coordinate and stay in sync.")
                            .font(.system(size: 15))
                            .foregroundStyle(GroupEmptyTokens.secondary)
                            .fixedSize(horizontal: false, vertical: true)
                        Image("group_create_hero")
                            .resizable()
                            .scaledToFill()
                            .frame(maxWidth: .infinity)
                            .frame(height: 220)
                            .clipShape(RoundedRectangle(cornerRadius: 24))
                    }
                    .groupEmptyAppear()

                    HStack(spacing: 0) {
                        ForEach(benefits.indices, id: \.self) { index in
                            benefitChip(benefits[index])
                                .frame(maxWidth: .infinity)
                        }
                    }
                    .groupEmptyAppear(delay: 0.08)

                    VStack(alignment: .leading, spacing: 16) {
                        Text("Choose what you want to do together")
                            .font(.system(size: 21, weight: .semibold))
                            .foregroundStyle(GroupEmptyTokens.text)
                        GroupEmptyMomentTypeGrid(
                            onSelectExperience: onSelectExperience,
                            onSelectPurchase: onSelectPurchase,
                            onSelectLiving: onSelectLiving
                        )
                        GroupEmptyScanJoinButton {
                            showScanner = true
                        }
                        Text("You can always add more moments later.")
                            .font(.system(size: 13))
                            .foregroundStyle(GroupEmptyTokens.secondary)
                            .multilineTextAlignment(.center)
                            .frame(maxWidth: .infinity)
                    }
                    .groupEmptyAppear(delay: 0.14)
                }
                .padding(.horizontal, 20)
                .padding(.bottom, 36)
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

    private func benefitChip(_ item: BenefitItem) -> some View {
        VStack(spacing: 6) {
            ZStack {
                RoundedRectangle(cornerRadius: 14)
                    .fill(GroupEmptyTokens.surfaceHigh)
                    .overlay(
                        RoundedRectangle(cornerRadius: 14)
                            .stroke(item.border, lineWidth: 1)
                    )
                    .frame(width: 28, height: 28)
                GroupEmptyAssetIcon(name: item.iconName, size: 16)
            }
            Text(item.label)
                .font(.system(size: 11, weight: .semibold))
                .foregroundStyle(GroupEmptyTokens.secondary)
        }
    }
}

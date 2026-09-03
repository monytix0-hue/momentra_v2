import SwiftUI

/// Figma `353:5783` — Personal Life empty body.
struct PersonalLifeEmptyView: View {
    var onStartCta: () -> Void
    var history: [MomentSummary] = []

    var body: some View {
        NativeDashboardScaffold(background: PersonalEmptyTokens.bg) {
            NativeListSection(insets: EdgeInsets(top: 28, leading: 20, bottom: 34, trailing: 20)) {
                VStack(spacing: 24) {
                    hero
                    howItWorks
                    fourPillars
                    whatYoullDiscover
                    socialProof
                    PersonalGradientCta(title: "✨ Start Building Your Life Graph", onTap: onStartCta)
                    Text("Takes less than a minute to begin")
                        .font(.system(size: 11))
                        .foregroundStyle(PersonalEmptyTokens.subtle)
                        .frame(maxWidth: .infinity)
                    PersonalHistorySection(history: history)
                }
            }
        }
        .background(PersonalEmptyTokens.bg.ignoresSafeArea())
        .personalAppear()
    }

    private var hero: some View {
        VStack(spacing: 12) {
            Text("See Your Whole Life in One Place")
                .font(.system(size: 24, weight: .heavy))
                .foregroundStyle(PersonalEmptyTokens.text)
                .multilineTextAlignment(.center)
                .shadow(color: PersonalEmptyTokens.purple.opacity(0.2), radius: 6)
            Text("Momentra connects your daily moments across all life areas to reveal the bigger picture - your energy, growth, balance, and trajectory.")
                .font(.system(size: 13))
                .foregroundStyle(PersonalEmptyTokens.secondary)
                .multilineTextAlignment(.center)
                .lineSpacing(4)
        }
        .frame(maxWidth: .infinity)
    }

    private var howItWorks: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack(spacing: 12) {
                Text("✨")
                    .font(.system(size: 18))
                    .frame(width: 36, height: 36)
                    .background(PersonalEmptyTokens.purple, in: Circle())
                Text("How Life Intelligence Works")
                    .font(.system(size: 18, weight: .bold))
                    .foregroundStyle(PersonalEmptyTokens.text)
            }

            HStack(alignment: .top, spacing: 8) {
                stepCard("📝", "Log", "Capture moments across all 4 life areas", PersonalEmptyTokens.purple)
                Image("personal_life_arrow_right")
                    .resizable()
                    .scaledToFit()
                    .frame(width: 20, height: 20)
                    .padding(.top, 36)
                stepCard("🔗", "Connect", "Momentra finds patterns between your areas", PersonalEmptyTokens.green)
                Image("personal_life_arrow_right1")
                    .resizable()
                    .scaledToFit()
                    .frame(width: 20, height: 20)
                    .padding(.top, 36)
                stepCard("💡", "Reveal", "See your complete life intelligence graph", PersonalEmptyTokens.amber)
            }
        }
        .padding(20)
        .background(Color.white.opacity(0.04))
        .overlay(RoundedRectangle(cornerRadius: 20).stroke(Color.white.opacity(0.08), lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 20))
    }

    private func stepCard(_ emoji: String, _ title: String, _ body: String, _ accent: Color) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(emoji)
                .font(.system(size: 16))
                .frame(width: 32, height: 32)
                .background(accent, in: Circle())
            Text(title)
                .font(.system(size: 12, weight: .bold))
                .foregroundStyle(PersonalEmptyTokens.text)
            Text(body)
                .font(.system(size: 10))
                .foregroundStyle(PersonalEmptyTokens.secondary)
                .fixedSize(horizontal: false, vertical: true)
        }
        .padding(12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(accent.opacity(0.1))
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(accent, lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private var fourPillars: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack(spacing: 12) {
                PersonalIconCircle(
                    glyph: "◎",
                    accent: PersonalEmptyTokens.purple,
                    deep: PersonalEmptyTokens.deepIndigo,
                    size: 36
                )
                Text("Your 4 Life Pillars")
                    .font(.system(size: 18, weight: .bold))
                    .foregroundStyle(PersonalEmptyTokens.text)
            }

            VStack(spacing: 12) {
                pillarRow("▣", "Life Operations", "Money, routines, commitments", PersonalEmptyTokens.purple, PersonalEmptyTokens.deepIndigo)
                pillarRow("↗", "Future Building", "Goals, growth, milestones", PersonalEmptyTokens.green, PersonalEmptyTokens.tealDeep)
                pillarRow("◇", "Lifestyle", "Experiences, wellbeing, creativity", PersonalEmptyTokens.amber, PersonalEmptyTokens.orangeDeep)
                pillarRow("♡", "Relationships", "Connections, care, shared moments", PersonalEmptyTokens.pink, PersonalEmptyTokens.pinkDeep)
            }

            HStack {
                Text("0 of 4 areas active")
                    .font(.system(size: 11, weight: .bold))
                    .foregroundStyle(Color(hex: "#C9BFFF"))
                Spacer()
                RoundedRectangle(cornerRadius: 2)
                    .stroke(PersonalEmptyTokens.purple, lineWidth: 1)
                    .background(Color.white.opacity(0.04))
                    .frame(width: 120, height: 4)
            }
        }
        .padding(20)
        .background(Color(hex: "#3A3842").opacity(0.55))
        .overlay(RoundedRectangle(cornerRadius: 20).stroke(Color.white.opacity(0.08), lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 20))
    }

    private func pillarRow(
        _ glyph: String,
        _ title: String,
        _ subtitle: String,
        _ accent: Color,
        _ deep: Color
    ) -> some View {
        HStack(spacing: 12) {
            PersonalIconCircle(glyph: glyph, accent: accent, deep: deep, size: 32)
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.system(size: 14, weight: .bold))
                    .foregroundStyle(PersonalEmptyTokens.text)
                Text(subtitle)
                    .font(.system(size: 11))
                    .foregroundStyle(PersonalEmptyTokens.subtle)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            RoundedRectangle(cornerRadius: 2)
                .stroke(accent, lineWidth: 1)
                .background(Color.white.opacity(0.04))
                .frame(width: 80, height: 4)
        }
        .padding(12)
        .background(Color.white.opacity(0.02))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private var whatYoullDiscover: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack(spacing: 12) {
                PersonalIconCircle(
                    glyph: "✦",
                    accent: PersonalEmptyTokens.pink,
                    deep: PersonalEmptyTokens.pinkDeep,
                    size: 36
                )
                Text("What You'll Discover")
                    .font(.system(size: 18, weight: .bold))
                    .foregroundStyle(PersonalEmptyTokens.text)
            }

            VStack(spacing: 12) {
                unlockRow(
                    accent: PersonalEmptyTokens.purple,
                    icon: {
                        Image("personal_life_icon")
                            .resizable()
                            .scaledToFit()
                            .frame(width: 40, height: 40)
                    },
                    title: "Life Health Score",
                    body: "A real-time composite of your wellbeing across all areas"
                )
                unlockRow(
                    accent: PersonalEmptyTokens.green,
                    icon: {
                        ZStack {
                            PersonalIconCircle(
                                glyph: "◈",
                                accent: PersonalEmptyTokens.green,
                                deep: PersonalEmptyTokens.tealDeep,
                                size: 40
                            )
                            Image("personal_life_grid")
                                .resizable()
                                .scaledToFit()
                                .frame(width: 22, height: 22)
                                .opacity(0.35)
                        }
                    },
                    title: "Cross-Area Patterns",
                    body: "How your finances affect your relationships, how goals impact energy"
                )
                unlockRow(
                    accent: PersonalEmptyTokens.amber,
                    icon: {
                        PersonalIconCircle(
                            glyph: "↗",
                            accent: PersonalEmptyTokens.amber,
                            deep: PersonalEmptyTokens.orangeDeep,
                            size: 40
                        )
                    },
                    title: "Trajectory Forecast",
                    body: "Where your life is heading based on current momentum"
                )
            }
        }
        .padding(20)
        .background(Color(hex: "#3A3842").opacity(0.55))
        .overlay(RoundedRectangle(cornerRadius: 20).stroke(Color.white.opacity(0.08), lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 20))
    }

    private func unlockRow<Icon: View>(
        accent: Color,
        @ViewBuilder icon: () -> Icon,
        title: String,
        body: String
    ) -> some View {
        HStack(spacing: 12) {
            RoundedRectangle(cornerRadius: 2)
                .fill(accent)
                .frame(width: 4)
                .frame(maxHeight: .infinity)
            icon()
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.system(size: 13, weight: .bold))
                    .foregroundStyle(PersonalEmptyTokens.text)
                Text(body)
                    .font(.system(size: 11))
                    .foregroundStyle(PersonalEmptyTokens.secondary)
                    .fixedSize(horizontal: false, vertical: true)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            VStack(spacing: 2) {
                Image("personal_moments_lock")
                    .resizable()
                    .scaledToFit()
                    .frame(width: 12, height: 12)
                    .opacity(0.5)
                Text("Unlocks after 7 moments")
                    .font(.system(size: 9))
                    .foregroundStyle(PersonalEmptyTokens.subtle)
                    .multilineTextAlignment(.trailing)
                    .frame(width: 72, alignment: .trailing)
            }
        }
        .padding(.vertical, 12)
        .padding(.trailing, 12)
        .padding(.leading, 8)
        .background(Color.white.opacity(0.02))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private var socialProof: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("\"")
                .font(.system(size: 18))
                .foregroundStyle(.white)
                .frame(width: 32, height: 32)
                .background(PersonalEmptyTokens.purple, in: RoundedRectangle(cornerRadius: 16))
            Text("\"After 2 weeks, I finally understood why I felt drained every Thursday.\" - Sandeep")
                .font(.system(size: 13).italic())
                .foregroundStyle(PersonalEmptyTokens.text)
                .lineSpacing(4)
            HStack(spacing: 2) {
                ForEach(0..<5, id: \.self) { _ in
                    Image("personal_life_star")
                        .resizable()
                        .scaledToFit()
                        .frame(width: 12, height: 12)
                }
            }
        }
        .padding(20)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.white.opacity(0.04))
        .overlay(RoundedRectangle(cornerRadius: 20).stroke(PersonalEmptyTokens.purple, lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 20))
    }
}

import SwiftUI

/// Figma `353:394` — Personal Moments empty body.
struct PersonalMomentsEmptyView: View {
    var onCreateMoment: () -> Void
    var history: [MomentSummary] = []

    var body: some View {
        NativeDashboardScaffold(background: Color.clear) {
            NativeListSection(insets: EdgeInsets(top: 24, leading: 20, bottom: 34, trailing: 20)) {
                VStack(spacing: 32) {
                    hero
                    journeyTimeline
                    testimonial
                    whatAwaitsYou
                    milestones
                    ctaSection
                    PersonalHistorySection(history: history)
                }
            }
        }
        .background(
            LinearGradient(
                colors: [Color(hex: "#0F0D15"), Color(hex: "#191622")],
                startPoint: .top,
                endPoint: .bottom
            )
            .ignoresSafeArea()
        )
        .personalAppear()
    }

    private var hero: some View {
        VStack(spacing: 12) {
            Text("Your Story Starts Here")
                .font(.plusJakarta(size: 26, weight: .heavy))
                .foregroundStyle(PersonalEmptyTokens.text)
                .multilineTextAlignment(.center)
            Text("Every moment you capture becomes part of your personal narrative.")
                .font(.plusJakarta(size: 14))
                .foregroundStyle(PersonalEmptyTokens.muted)
                .multilineTextAlignment(.center)
                .lineSpacing(4)
        }
        .frame(maxWidth: .infinity)
    }

    private var journeyTimeline: some View {
        HStack(alignment: .top, spacing: 16) {
            ZStack(alignment: .top) {
                RoundedRectangle(cornerRadius: 2)
                    .fill(
                        LinearGradient(
                            colors: [PersonalEmptyTokens.purple, PersonalEmptyTokens.green],
                            startPoint: .top,
                            endPoint: .bottom
                        )
                    )
                    .frame(width: 4)
                    .padding(.top, 20)
                    .padding(.bottom, 20)

                VStack(spacing: 48) {
                    PersonalIconCircle(
                        glyph: "◎",
                        accent: PersonalEmptyTokens.deepIndigo,
                        deep: Color(hex: "#2E26A8"),
                        size: 40
                    )
                    PersonalIconCircle(
                        glyph: "⚡",
                        accent: PersonalEmptyTokens.deepIndigo,
                        deep: Color(hex: "#2E26A8"),
                        size: 44
                    )
                    PersonalIconCircle(
                        glyph: "☆",
                        accent: PersonalEmptyTokens.tealDeep,
                        deep: Color(hex: "#0A5A4C"),
                        size: 40
                    )
                }
            }
            .frame(width: 44)

            VStack(spacing: 24) {
                timelineCard(
                    title: "Where you've been",
                    subtitle: "Patterns will emerge from your history",
                    highlighted: false
                )
                timelineCard(
                    title: "Where you are now",
                    subtitle: "Start capturing this moment",
                    highlighted: true,
                    showCapture: true
                )
                timelineCard(
                    title: "Where you're going",
                    subtitle: "Your future self will thank you",
                    highlighted: false
                )
            }
        }
    }

    private func timelineCard(
        title: String,
        subtitle: String,
        highlighted: Bool,
        showCapture: Bool = false
    ) -> some View {
        VStack(alignment: .leading, spacing: highlighted ? 12 : 8) {
            Text(title)
                .font(.plusJakarta(size: 14, weight: .bold))
                .foregroundStyle(PersonalEmptyTokens.text)
            Text(subtitle)
                .font(.plusJakarta(size: 12))
                .foregroundStyle(PersonalEmptyTokens.muted)
            if showCapture {
                Button(action: onCreateMoment) {
                    Text("Capture Now →")
                        .font(.plusJakarta(size: 11, weight: .bold))
                        .foregroundStyle(.white)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 6)
                        .background(PersonalEmptyTokens.purple, in: Capsule())
                }
                .buttonStyle(.plain)
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.white.opacity(0.04))
        .overlay(
            RoundedRectangle(cornerRadius: 16)
                .stroke(
                    highlighted ? PersonalEmptyTokens.purple : Color.white.opacity(0.08),
                    lineWidth: highlighted ? 2 : 1
                )
        )
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private var testimonial: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("\"")
                .font(.plusJakarta(size: 18))
                .foregroundStyle(.white)
                .frame(width: 32, height: 32)
                .background(PersonalEmptyTokens.purple, in: RoundedRectangle(cornerRadius: 16))
            Text("After 30 days, I noticed patterns I never saw before. Momentra helped me understand my energy cycles and relationship dynamics.")
                .font(.plusJakarta(size: 13).italic())
                .foregroundStyle(PersonalEmptyTokens.text)
                .lineSpacing(4)
            HStack(spacing: 8) {
                Text("- Santosh, using Momentra for 3 months")
                    .font(.plusJakarta(size: 12, weight: .semibold))
                    .foregroundStyle(PersonalEmptyTokens.muted)
                HStack(spacing: 2) {
                    ForEach(0..<5, id: \.self) { _ in
                        Image("personal_moments_star")
                            .resizable()
                            .scaledToFit()
                            .frame(width: 12, height: 12)
                    }
                }
            }
        }
        .padding(20)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.white.opacity(0.04))
        .overlay(RoundedRectangle(cornerRadius: 20).stroke(PersonalEmptyTokens.purple, lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 20))
    }

    private var whatAwaitsYou: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack(spacing: 8) {
                Text("What awaits you")
                    .font(.plusJakarta(size: 16, weight: .bold))
                    .foregroundStyle(PersonalEmptyTokens.text)
                Text("✦")
                    .font(.plusJakarta(size: 16, weight: .bold))
                    .foregroundStyle(PersonalEmptyTokens.deepIndigo)
            }

            HStack(spacing: 12) {
                previewCard("📊", "Your Pulse", "Real-time insights", PersonalEmptyTokens.purple)
                previewCard("🧠", "Your Memory", "Deep patterns", PersonalEmptyTokens.green)
                previewCard("🌊", "Your Life", "Full picture", PersonalEmptyTokens.blue)
            }
        }
    }

    private func previewCard(_ emoji: String, _ title: String, _ subtitle: String, _ accent: Color) -> some View {
        VStack(spacing: 8) {
            Text(emoji)
                .font(.plusJakarta(size: 16))
                .frame(width: 32, height: 32)
                .background(accent.opacity(0.15), in: Circle())
            Text(title)
                .font(.plusJakarta(size: 12, weight: .semibold))
                .foregroundStyle(PersonalEmptyTokens.text)
                .lineLimit(1)
                .minimumScaleFactor(0.8)
            Text(subtitle)
                .font(.plusJakarta(size: 10))
                .foregroundStyle(PersonalEmptyTokens.muted)
                .lineLimit(1)
                .minimumScaleFactor(0.8)
            HStack(spacing: 4) {
                Image("personal_moments_lock")
                    .resizable()
                    .scaledToFit()
                    .frame(width: 10, height: 10)
                Text("Unlock with first moment")
                    .font(.plusJakarta(size: 8))
                    .foregroundStyle(PersonalEmptyTokens.subtle)
                    .lineLimit(1)
                    .minimumScaleFactor(0.7)
            }
        }
        .padding(12)
        .frame(maxWidth: .infinity)
        .frame(height: 118)
        .background(Color.white.opacity(0.04))
        .overlay(alignment: .top) {
            Rectangle()
                .fill(accent)
                .frame(height: 2)
        }
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private var milestones: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack(spacing: 8) {
                Text("Milestones to unlock")
                    .font(.plusJakarta(size: 16, weight: .bold))
                    .foregroundStyle(PersonalEmptyTokens.text)
                Text("✦")
                    .font(.plusJakarta(size: 16, weight: .bold))
                    .foregroundStyle(PersonalEmptyTokens.orangeDeep)
            }

            HStack(spacing: 12) {
                milestone("◉", "First Moment", PersonalEmptyTokens.deepIndigo, Color(hex: "#2E26A8"), dimmed: false)
                milestone("▲", "7-Day Streak", PersonalEmptyTokens.orangeDeep, Color(hex: "#B45309"), dimmed: true)
                milestone("◈", "Pattern Found", PersonalEmptyTokens.tealDeep, Color(hex: "#0A5A4C"), dimmed: true)
                milestone("◇", "30 Days", PersonalEmptyTokens.blueDeep, Color(hex: "#1D4ED8"), dimmed: true)
            }
        }
    }

    private func milestone(
        _ glyph: String,
        _ label: String,
        _ accent: Color,
        _ deep: Color,
        dimmed: Bool
    ) -> some View {
        VStack(spacing: 8) {
            PersonalIconCircle(glyph: glyph, accent: accent, deep: deep, size: 44)
            Text(label)
                .font(.plusJakarta(size: 10, weight: .semibold))
                .foregroundStyle(dimmed ? PersonalEmptyTokens.subtle : PersonalEmptyTokens.text)
                .multilineTextAlignment(.center)
                .lineLimit(2)
                .minimumScaleFactor(0.8)
        }
        .frame(maxWidth: .infinity)
        .opacity(dimmed ? 0.5 : 1)
    }

    private var ctaSection: some View {
        VStack(spacing: 8) {
            PersonalGradientCta(title: "✨ Create Your First Moment", onTap: onCreateMoment)
            Text("It only takes 30 seconds")
                .font(.plusJakarta(size: 11))
                .foregroundStyle(PersonalEmptyTokens.subtle)
        }
    }
}

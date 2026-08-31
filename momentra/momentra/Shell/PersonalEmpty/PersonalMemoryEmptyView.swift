import SwiftUI

/// Figma `353:5878` — Personal Memory empty body.
struct PersonalMemoryEmptyView: View {
    var onCreateMoment: () -> Void
    var history: [MomentSummary] = []

    var body: some View {
        ScrollView {
            VStack(spacing: 24) {
                hero
                howMemoryWorks
                modulesAwaiting
                whatYoullUnlock
                ctaBlock
                PersonalQuoteBar(text: "\"After 2 weeks, Memory showed me patterns I'd been blind to for years.\" - Alex, 28")
                PersonalHistorySection(history: history)
            }
            .padding(.horizontal, 20)
            .padding(.top, 28)
            .padding(.bottom, 34)
        }
        .background(PersonalEmptyTokens.bg.ignoresSafeArea())
        .personalAppear()
    }

    private var hero: some View {
        VStack(spacing: 16) {
            VStack(spacing: 8) {
                Text("Your Memory Is Forming")
                    .font(.system(size: 24, weight: .heavy))
                    .foregroundStyle(PersonalEmptyTokens.text)
                    .multilineTextAlignment(.center)
                Text("As you log moments, Momentra builds a deep intelligence layer - discovering who you are, how you grow, and what drives you.")
                    .font(.system(size: 13))
                    .foregroundStyle(PersonalEmptyTokens.secondary)
                    .multilineTextAlignment(.center)
                    .lineSpacing(4)
            }

            memoryVisualization
        }
    }

    private var memoryVisualization: some View {
        ZStack {
            LinearGradient(
                colors: [Color(hex: "#0F0D15"), Color(hex: "#191622")],
                startPoint: .leading,
                endPoint: .trailing
            )

            Image("personal_memory_bg_glow")
                .resizable()
                .scaledToFit()
                .frame(width: 220, height: 220)
                .opacity(0.85)

            memoryMiniCard(glyph: "♡", tag: "Relationships", lineLabel: "Patterns", border: PersonalEmptyTokens.purple.opacity(0.4))
                .rotationEffect(.degrees(-6))
                .offset(x: -72, y: -42)

            memoryMiniCard(glyph: "☆", tag: "Emotions", lineLabel: "Growth", border: PersonalEmptyTokens.pink.opacity(0.25), width: 190, height: 120)
                .rotationEffect(.degrees(4))
                .offset(x: -4, y: 8)

            memoryMiniCard(glyph: "◇", tag: "Experiences", lineLabel: "Story", border: PersonalEmptyTokens.purple.opacity(0.25), width: 160, height: 100)
                .rotationEffect(.degrees(-8))
                .offset(x: 78, y: 48)

            Image("personal_memory_conn_dot1")
                .resizable()
                .scaledToFit()
                .frame(width: 6, height: 6)
                .offset(x: -22, y: 18)
            Image("personal_memory_conn_dot2")
                .resizable()
                .scaledToFit()
                .frame(width: 6, height: 6)
                .offset(x: 22, y: -14)
            Image("personal_memory_conn_dot3")
                .resizable()
                .scaledToFit()
                .frame(width: 6, height: 6)
            Image("personal_memory_center_glow")
                .resizable()
                .scaledToFit()
                .frame(width: 44, height: 44)
        }
        .frame(height: 220)
        .clipShape(RoundedRectangle(cornerRadius: 14))
        .shadow(color: PersonalEmptyTokens.purple.opacity(0.25), radius: 12, y: 4)
    }

    private func memoryMiniCard(
        glyph: String,
        tag: String,
        lineLabel: String,
        border: Color,
        width: CGFloat = 170,
        height: CGFloat = 110
    ) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text(glyph)
                    .font(.system(size: 12, weight: .bold))
                    .foregroundStyle(Color.white.opacity(0.35))
                    .frame(width: 18, height: 18)
                    .background(Color.white.opacity(0.04))
                    .overlay(RoundedRectangle(cornerRadius: 6).stroke(Color.white.opacity(0.08), lineWidth: 1))
                    .clipShape(RoundedRectangle(cornerRadius: 6))
                Spacer()
                Text(tag)
                    .font(.system(size: 10, weight: .bold))
                    .foregroundStyle(Color.white.opacity(0.35))
                    .padding(.horizontal, 8)
                    .frame(height: 18)
                    .background(Color.white.opacity(0.04), in: Capsule())
                    .overlay(Capsule().stroke(Color.white.opacity(0.08), lineWidth: 1))
            }
            HStack {
                RoundedRectangle(cornerRadius: 2)
                    .fill(Color.white.opacity(0.08))
                    .frame(width: 44, height: 2)
                Spacer()
                Text(lineLabel)
                    .font(.system(size: 10, weight: .bold))
                    .foregroundStyle(Color.white.opacity(0.35))
            }
            Spacer(minLength: 0)
        }
        .padding(12)
        .frame(width: width, height: height, alignment: .topLeading)
        .background(
            LinearGradient(
                colors: [PersonalEmptyTokens.purple.opacity(0.08), PersonalEmptyTokens.pink.opacity(0.06)],
                startPoint: .leading,
                endPoint: .trailing
            )
        )
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(border, lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .shadow(color: .black.opacity(0.2), radius: 8, y: 6)
    }

    private var howMemoryWorks: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack(spacing: 12) {
                PersonalIconCircle(
                    glyph: "◎",
                    accent: PersonalEmptyTokens.purple,
                    deep: PersonalEmptyTokens.deepIndigo,
                    size: 36
                )
                Text("How Memory Intelligence Works")
                    .font(.system(size: 18, weight: .bold))
                    .foregroundStyle(PersonalEmptyTokens.text)
            }

            HStack(alignment: .top, spacing: 8) {
                memStep("◉", "Capture", "Log moments daily", PersonalEmptyTokens.purple, PersonalEmptyTokens.deepIndigo)
                Image("personal_memory_arrow_right")
                    .resizable()
                    .scaledToFit()
                    .frame(width: 20, height: 20)
                    .padding(.top, 36)
                memStep("◈", "Analyze", "AI finds your patterns", PersonalEmptyTokens.green, PersonalEmptyTokens.tealDeep)
                Image("personal_memory_arrow_right1")
                    .resizable()
                    .scaledToFit()
                    .frame(width: 20, height: 20)
                    .padding(.top, 36)
                memStep("✦", "Remember", "Deep insights emerge", PersonalEmptyTokens.amber, PersonalEmptyTokens.orangeDeep)
            }
        }
        .padding(20)
        .background(Color.white.opacity(0.03))
        .overlay(RoundedRectangle(cornerRadius: 20).stroke(Color.white.opacity(0.08), lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 20))
    }

    private func memStep(
        _ glyph: String,
        _ title: String,
        _ body: String,
        _ accent: Color,
        _ deep: Color
    ) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            PersonalIconCircle(glyph: glyph, accent: accent, deep: deep, size: 32)
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

    private var modulesAwaiting: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack(spacing: 12) {
                PersonalIconCircle(
                    glyph: "▣",
                    accent: PersonalEmptyTokens.purple,
                    deep: PersonalEmptyTokens.deepIndigo,
                    size: 36
                )
                Text("Modules Awaiting Data")
                    .font(.system(size: 18, weight: .bold))
                    .foregroundStyle(PersonalEmptyTokens.text)
            }

            VStack(spacing: 12) {
                moduleRow("◈", "Patterns", "Analyzing recurring behaviors", PersonalEmptyTokens.purple, PersonalEmptyTokens.deepIndigo)
                moduleRow("◎", "Recovery Anchors", "Identifying reset patterns", PersonalEmptyTokens.green, PersonalEmptyTokens.tealDeep)
                moduleRow("/", "Progress Signals", "Mapping momentum", PersonalEmptyTokens.blue, PersonalEmptyTokens.blueDeep)
                moduleRow("✦", "Experience Highlights", "Extracting emotional resonance", PersonalEmptyTokens.amber, PersonalEmptyTokens.orangeDeep)
                moduleRow("♡", "Relationship Learning", "Synthesizing connection lessons", PersonalEmptyTokens.pink, PersonalEmptyTokens.pinkDeep)
            }
        }
        .padding(20)
        .background(Color.white.opacity(0.03))
        .overlay(RoundedRectangle(cornerRadius: 20).stroke(Color.white.opacity(0.08), lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 20))
    }

    private func moduleRow(
        _ glyph: String,
        _ title: String,
        _ subtitle: String,
        _ accent: Color,
        _ deep: Color
    ) -> some View {
        HStack(spacing: 12) {
            PersonalIconCircle(glyph: glyph, accent: accent, deep: deep, size: 28)
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.system(size: 13, weight: .bold))
                    .foregroundStyle(PersonalEmptyTokens.text)
                Text(subtitle)
                    .font(.system(size: 11))
                    .foregroundStyle(PersonalEmptyTokens.subtle)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            Text("PENDING")
                .font(.system(size: 9, weight: .bold))
                .foregroundStyle(PersonalEmptyTokens.subtle)
                .padding(.horizontal, 6)
                .padding(.vertical, 2)
                .background(Color.white.opacity(0.03))
                .overlay(RoundedRectangle(cornerRadius: 6).stroke(Color.white.opacity(0.08), lineWidth: 1))
                .clipShape(RoundedRectangle(cornerRadius: 6))
        }
        .padding(12)
        .background(Color.white.opacity(0.02))
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color.white.opacity(0.08), lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private var whatYoullUnlock: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("What You'll Unlock")
                .font(.system(size: 16, weight: .bold))
                .foregroundStyle(PersonalEmptyTokens.text)

            VStack(spacing: 12) {
                unlockCard(
                    "◎",
                    "Identity Snapshot",
                    "A real-time portrait of who you are",
                    PersonalEmptyTokens.purple,
                    PersonalEmptyTokens.deepIndigo
                )
                unlockCard(
                    "◈",
                    "Pattern Detection",
                    "Recurring behaviors and cycles revealed",
                    PersonalEmptyTokens.green,
                    PersonalEmptyTokens.tealDeep
                )
                unlockCard(
                    "↗",
                    "Growth Trajectory",
                    "Where you're heading and what's changing",
                    PersonalEmptyTokens.amber,
                    PersonalEmptyTokens.orangeDeep
                )
            }
        }
        .padding(20)
        .background(Color.white.opacity(0.03))
        .overlay(RoundedRectangle(cornerRadius: 20).stroke(Color.white.opacity(0.08), lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 20))
    }

    private func unlockCard(
        _ glyph: String,
        _ title: String,
        _ body: String,
        _ accent: Color,
        _ deep: Color
    ) -> some View {
        HStack(spacing: 12) {
            RoundedRectangle(cornerRadius: 2)
                .fill(accent)
                .frame(width: 4)
                .frame(maxHeight: .infinity)
            PersonalIconCircle(glyph: glyph, accent: accent, deep: deep, size: 40)
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

    private var ctaBlock: some View {
        VStack(spacing: 12) {
            PersonalGradientCta(title: "Initialize Your Memory", onTap: onCreateMoment)
            Text("Every moment makes your memory smarter")
                .font(.system(size: 11))
                .foregroundStyle(PersonalEmptyTokens.subtle)
                .multilineTextAlignment(.center)
                .frame(maxWidth: .infinity)
        }
    }
}

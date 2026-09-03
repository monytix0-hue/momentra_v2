import SwiftUI

/// Figma `353:320` — Personal Pulse empty body.
struct PersonalPulseEmptyView: View {
    var onCreateMoment: () -> Void
    var history: [MomentSummary] = []

    private let blue = PersonalEmptyTokens.blue
    private let blueDeep = PersonalEmptyTokens.blueDeep

    var body: some View {
        List {
            Section { hero }
                .listRowInsets(EdgeInsets(top: 28, leading: 20, bottom: 0, trailing: 20))
                .listRowBackground(Color.clear)
                .listRowSeparator(.hidden)
            Section { startJourneyRow }
                .listRowInsets(EdgeInsets(top: 28, leading: 20, bottom: 0, trailing: 20))
                .listRowBackground(Color.clear)
                .listRowSeparator(.hidden)
            Section { futurePulsePreview }
                .listRowInsets(EdgeInsets(top: 28, leading: 20, bottom: 0, trailing: 20))
                .listRowBackground(Color.clear)
                .listRowSeparator(.hidden)
            Section { operationalSignals }
                .listRowBackground(Color.clear)
                .listRowSeparator(.hidden)
            Section { whatMomentraLearns }
                .listRowBackground(Color.clear)
                .listRowSeparator(.hidden)
            Section {
                PersonalQuoteBar(text: "\"Start your journey to build a system that grows with you.\"")
                PersonalHistorySection(title: "Recent moments", history: history)
            }
            .listRowInsets(EdgeInsets(top: 0, leading: 20, bottom: 34, trailing: 20))
            .listRowBackground(Color.clear)
            .listRowSeparator(.hidden)
        }
        .listStyle(.plain)
        .scrollContentBackground(.hidden)
        .background(
            LinearGradient(
                colors: [Color(hex: "#1A1726"), Color(hex: "#0F0D15")],
                startPoint: .top,
                endPoint: .bottom
            )
            .ignoresSafeArea()
        )
        .personalAppear()
    }

    private var hero: some View {
        VStack(spacing: 14) {
            Text("Your Personal Operating System")
                .font(.plusJakarta(size: 28, weight: .heavy))
                .foregroundStyle(PersonalEmptyTokens.text)
                .multilineTextAlignment(.center)
                .shadow(color: PersonalEmptyTokens.purple.opacity(0.3), radius: 6)
            Text("Life moves through commitments, money, energy, experiences, and relationships.")
                .font(.plusJakarta(size: 15))
                .foregroundStyle(PersonalEmptyTokens.muted)
                .multilineTextAlignment(.center)
                .lineSpacing(6)
        }
        .frame(maxWidth: .infinity)
    }

    private var startJourneyRow: some View {
        Button(action: onCreateMoment) {
            HStack(spacing: 12) {
                RoundedRectangle(cornerRadius: 2)
                    .fill(PersonalEmptyTokens.purple)
                    .frame(width: 4, height: 48)
                VStack(alignment: .leading, spacing: 5) {
                    Text("Start Your Journey")
                        .font(.plusJakarta(size: 19, weight: .bold))
                        .foregroundStyle(PersonalEmptyTokens.text)
                    Text("Activate your personal operating system.")
                        .font(.plusJakarta(size: 14))
                        .foregroundStyle(PersonalEmptyTokens.secondary)
                }
                Spacer(minLength: 8)
                PersonalIconCircle(
                    glyph: "↗",
                    accent: PersonalEmptyTokens.purple,
                    deep: PersonalEmptyTokens.deepIndigo,
                    size: 48
                )
                Image("personal_pulse_chevron_right")
                    .resizable()
                    .scaledToFit()
                    .frame(width: 20, height: 20)
                    .opacity(0.7)
            }
            .padding(20)
            .personalEmptyCard(radius: 24, fillOpacity: 0.12)
        }
        .buttonStyle(.plain)
    }

    private var futurePulsePreview: some View {
        VStack(spacing: 18) {
            ZStack {
                LinearGradient(
                    colors: [Color(hex: "#0F0D15"), Color(hex: "#1A1726")],
                    startPoint: .top,
                    endPoint: .bottom
                )
                Circle()
                    .stroke(PersonalEmptyTokens.purple.opacity(0.2), lineWidth: 1)
                    .frame(width: 180, height: 180)
                Circle()
                    .stroke(PersonalEmptyTokens.purple.opacity(0.12), lineWidth: 1)
                    .frame(width: 220, height: 220)

                miniCard(
                    title: "Activity",
                    chip: "Live",
                    chipColor: PersonalEmptyTokens.green,
                    border: PersonalEmptyTokens.green
                ) {
                    VStack(alignment: .leading, spacing: 6) {
                        GeometryReader { geo in
                            ZStack(alignment: .leading) {
                                Capsule().fill(Color.white.opacity(0.08))
                                Capsule()
                                    .fill(PersonalEmptyTokens.green)
                                    .frame(width: geo.size.width * 0.68)
                            }
                        }
                        .frame(height: 6)
                        Text("68% analyzed")
                            .font(.plusJakarta(size: 11))
                            .foregroundStyle(Color(hex: "#E5E7EB").opacity(0.75))
                    }
                }
                .rotationEffect(.degrees(-6))
                .offset(x: -56, y: -36)

                miniCard(
                    title: "Patterns",
                    chip: "Trend",
                    chipColor: PersonalEmptyTokens.purple,
                    border: PersonalEmptyTokens.purple
                ) {
                    Image("personal_pulse_line")
                        .resizable()
                        .scaledToFit()
                        .frame(height: 28)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }
                .rotationEffect(.degrees(6))
                .offset(x: 54, y: -8)

                miniCard(
                    title: "Signals",
                    chip: "New",
                    chipColor: PersonalEmptyTokens.amber,
                    border: PersonalEmptyTokens.amber
                ) {
                    HStack(spacing: 8) {
                        Circle().fill(PersonalEmptyTokens.amber).frame(width: 8, height: 8)
                        Circle().fill(PersonalEmptyTokens.purple).frame(width: 8, height: 8)
                        Circle().fill(PersonalEmptyTokens.green).frame(width: 8, height: 8)
                    }
                }
                .rotationEffect(.degrees(-4))
                .offset(y: 52)
            }
            .frame(height: 240)
            .clipShape(RoundedRectangle(cornerRadius: 20))

            Text("Your future pulse will form here.")
                .font(.plusJakarta(size: 17, weight: .semibold))
                .foregroundStyle(PersonalEmptyTokens.muted)
                .multilineTextAlignment(.center)

            PersonalGradientCta(title: "Create My First Moment", onTap: onCreateMoment)
        }
        .padding(.horizontal, 20)
        .padding(.vertical, 32)
        .personalEmptyCard(radius: 24, fillOpacity: 0.08)
    }

    private func miniCard<Content: View>(
        title: String,
        chip: String,
        chipColor: Color,
        border: Color,
        @ViewBuilder content: () -> Content
    ) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Text(title)
                    .font(.plusJakarta(size: 12, weight: .bold))
                    .foregroundStyle(Color(hex: "#E5E7EB").opacity(0.9))
                Spacer()
                Text(chip)
                    .font(.plusJakarta(size: 11, weight: .bold))
                    .foregroundStyle(chipColor)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(chipColor.opacity(0.1), in: Capsule())
                    .overlay(Capsule().stroke(chipColor.opacity(0.2), lineWidth: 1))
            }
            content()
        }
        .padding(12)
        .frame(width: 170, height: 92, alignment: .topLeading)
        .background(Color.white.opacity(0.06))
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(border, lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .shadow(color: .black.opacity(0.4), radius: 10, y: 6)
    }

    private var operationalSignals: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("OPERATIONAL SIGNALS")
                .font(.plusJakarta(size: 12, weight: .bold))
                .tracking(1.4)
                .foregroundStyle(PersonalEmptyTokens.purple)

            VStack(spacing: 12) {
                HStack(spacing: 12) {
                    signalCard("◉", "Moments", PersonalEmptyTokens.purple, PersonalEmptyTokens.deepIndigo)
                    signalCard("/", "Runtimes", blue, blueDeep)
                }
                HStack(spacing: 12) {
                    signalCard("◇", "Patterns", PersonalEmptyTokens.amber, PersonalEmptyTokens.orangeDeep)
                    signalCard("✦", "Guidance", PersonalEmptyTokens.green, PersonalEmptyTokens.tealDeep)
                }
            }
        }
        .padding(20)
        .personalEmptyCard(radius: 24, fillOpacity: 0.08)
    }

    private func signalCard(_ glyph: String, _ title: String, _ accent: Color, _ deep: Color) -> some View {
        VStack(spacing: 8) {
            PersonalIconCircle(glyph: glyph, accent: accent, deep: deep, size: 32)
            Text(title)
                .font(.plusJakarta(size: 11, weight: .semibold))
                .foregroundStyle(PersonalEmptyTokens.text)
            HStack(spacing: 4) {
                Text("-")
                    .font(.plusJakarta(size: 10))
                    .foregroundStyle(PersonalEmptyTokens.subtle)
                Circle()
                    .fill(accent)
                    .frame(width: 4, height: 4)
            }
        }
        .padding(12)
        .frame(maxWidth: .infinity)
        .background(Color.white.opacity(0.02))
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color.white.opacity(0.08), lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private var whatMomentraLearns: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack(spacing: 8) {
                PersonalIconCircle(
                    glyph: "◎",
                    accent: PersonalEmptyTokens.purple,
                    deep: PersonalEmptyTokens.deepIndigo,
                    size: 36
                )
                Text("WHAT MOMENTRA LEARNS")
                    .font(.plusJakarta(size: 12, weight: .bold))
                    .tracking(1.4)
                    .foregroundStyle(PersonalEmptyTokens.secondary)
            }

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 0) {
                    learnStep("◎", "1", "Stability", PersonalEmptyTokens.purple, PersonalEmptyTokens.deepIndigo)
                    connector
                    learnStep("/", "2", "Recovery", PersonalEmptyTokens.green, PersonalEmptyTokens.tealDeep)
                    connector
                    learnStep("/", "3", "Progress", blue, blueDeep)
                    connector
                    learnStep("↗", "4", "Momentum", PersonalEmptyTokens.amber, PersonalEmptyTokens.orangeDeep)
                }
            }

            Text("Start your journey to build a system that grows with you.")
                .font(.plusJakarta(size: 13))
                .foregroundStyle(PersonalEmptyTokens.muted)
        }
        .padding(20)
        .personalEmptyCard(radius: 24, fillOpacity: 0.08)
    }

    private var connector: some View {
        Rectangle()
            .fill(PersonalEmptyTokens.purple.opacity(0.3))
            .frame(width: 24, height: 1)
            .padding(.horizontal, 4)
    }

    private func learnStep(
        _ glyph: String,
        _ number: String,
        _ label: String,
        _ accent: Color,
        _ deep: Color
    ) -> some View {
        VStack(spacing: 8) {
            PersonalIconCircle(glyph: glyph, accent: accent, deep: deep, size: 32)
            Text(number)
                .font(.plusJakarta(size: 9, weight: .semibold))
                .foregroundStyle(PersonalEmptyTokens.text)
            Text(label)
                .font(.plusJakarta(size: 10, weight: .semibold))
                .foregroundStyle(PersonalEmptyTokens.text)
        }
        .padding(12)
        .frame(width: 72)
        .background(Color.white.opacity(0.02))
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color.white.opacity(0.08), lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }
}

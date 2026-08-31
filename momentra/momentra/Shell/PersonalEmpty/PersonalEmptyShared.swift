import SwiftUI

enum PersonalEmptyTokens {
    static let bg = Color(hex: "#14121B")
    static let text = Color(hex: "#E5E0EE")
    static let subtle = Color(hex: "#938EA1")
    static let secondary = Color(hex: "#C9C4D8")
    static let purple = Color(hex: "#7C5CFC")
    static let green = Color(hex: "#10B981")
    static let amber = Color(hex: "#F59E0B")
    static let card = Color(hex: "#1A1726")
    static let muted = Color(hex: "#B8B0C8")
    static let pink = Color(hex: "#E91E63")
    static let blue = Color(hex: "#3B82F6")
    static let deepIndigo = Color(hex: "#4F46E5")
    static let tealDeep = Color(hex: "#0F766E")
    static let orangeDeep = Color(hex: "#EA580C")
    static let pinkDeep = Color(hex: "#BE185D")
    static let blueDeep = Color(hex: "#2563EB")
}

private struct PersonalAppearModifier: ViewModifier {
    @State private var shown = false

    func body(content: Content) -> some View {
        content
            .opacity(shown ? 1 : 0)
            .offset(y: shown ? 0 : 16)
            .scaleEffect(shown ? 1 : 0.985)
            .onAppear {
                withAnimation(.easeOut(duration: 0.42)) {
                    shown = true
                }
            }
    }
}

extension View {
    func personalAppear() -> some View {
        modifier(PersonalAppearModifier())
    }
}

struct PersonalGradientCta: View {
    var title: String
    var onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            Text(title)
                .font(.system(size: 16, weight: .heavy))
                .foregroundStyle(.white)
                .frame(maxWidth: .infinity)
                .frame(height: 52)
                .background(
                    LinearGradient(
                        colors: [PersonalEmptyTokens.purple, PersonalEmptyTokens.pink],
                        startPoint: .leading,
                        endPoint: .trailing
                    ),
                    in: Capsule()
                )
                .shadow(color: PersonalEmptyTokens.purple.opacity(0.25), radius: 10, y: 4)
                .shadow(color: PersonalEmptyTokens.pink.opacity(0.2), radius: 6, y: 2)
        }
        .buttonStyle(.plain)
    }
}

struct PersonalHistorySection: View {
    var title: String = "Past moments"
    var history: [MomentSummary]

    var body: some View {
        if !history.isEmpty {
            VStack(alignment: .leading, spacing: 8) {
                Text(title)
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(PersonalEmptyTokens.text)
                ForEach(history) { moment in
                    VStack(alignment: .leading, spacing: 4) {
                        Text(moment.title)
                            .font(.system(size: 14, weight: .semibold))
                            .foregroundStyle(PersonalEmptyTokens.text)
                        Text(moment.status.capitalized)
                            .font(.system(size: 12))
                            .foregroundStyle(PersonalEmptyTokens.subtle)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(14)
                    .background(Color.white.opacity(0.06))
                    .clipShape(RoundedRectangle(cornerRadius: 14))
                }
            }
        }
    }
}

struct PersonalQuoteBar: View {
    var text: String

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            RoundedRectangle(cornerRadius: 2)
                .fill(PersonalEmptyTokens.purple.opacity(0.9))
                .frame(width: 4, height: 44)
            Text(text)
                .font(.system(size: 14).italic())
                .foregroundStyle(PersonalEmptyTokens.text)
                .lineSpacing(4)
                .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(20)
        .background(
            LinearGradient(
                colors: [Color.white.opacity(0.06), Color.clear],
                startPoint: .leading,
                endPoint: .trailing
            )
        )
        .overlay(RoundedRectangle(cornerRadius: 24).stroke(PersonalEmptyTokens.purple.opacity(0.85), lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 24))
    }
}

struct PersonalIconCircle: View {
    var glyph: String
    var accent: Color
    var deep: Color
    var size: CGFloat = 32

    var body: some View {
        Text(glyph)
            .font(.system(size: max(12, size * 0.44), weight: .heavy))
            .foregroundStyle(.white)
            .frame(width: size, height: size)
            .background(
                RadialGradient(
                    colors: [accent, deep],
                    center: .center,
                    startRadius: 0,
                    endRadius: size * 0.62
                ),
                in: Circle()
            )
            .shadow(color: deep.opacity(0.35), radius: 3, y: 2)
    }
}

struct PersonalEmptyCardBackground: ViewModifier {
    var radius: CGFloat = 24
    var border: Color = PersonalEmptyTokens.purple.opacity(0.85)
    var fillOpacity: Double = 0.08

    func body(content: Content) -> some View {
        content
            .background(
                LinearGradient(
                    colors: [Color.white.opacity(fillOpacity), Color.clear],
                    startPoint: .leading,
                    endPoint: .trailing
                )
            )
            .overlay(RoundedRectangle(cornerRadius: radius).stroke(border, lineWidth: 1))
            .clipShape(RoundedRectangle(cornerRadius: radius))
    }
}

extension View {
    func personalEmptyCard(
        radius: CGFloat = 24,
        border: Color = PersonalEmptyTokens.purple.opacity(0.85),
        fillOpacity: Double = 0.08
    ) -> some View {
        modifier(PersonalEmptyCardBackground(radius: radius, border: border, fillOpacity: fillOpacity))
    }
}

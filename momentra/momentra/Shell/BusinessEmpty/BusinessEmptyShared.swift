import SwiftUI

// Domain-aware Business empty screen tokens
struct BusinessEmptyTokens {
    let accent: Color
    let textPrimary: Color
    let textSecondary: Color
    let textMuted: Color
    let cardFill: Color
    let cardStroke: Color
    let iconWell: Color

    init(domain: BusinessDomain = .corporate) {
        let domainAccent = BusinessTokens.Colors.accent(for: domain)
        self.accent = domainAccent
        self.textPrimary = BusinessTokens.Colors.textPrimary
        self.textSecondary = BusinessTokens.Colors.textSecondary
        self.textMuted = BusinessTokens.Colors.textMuted
        self.cardFill = Color.white.opacity(0.08)
        self.cardStroke = Color.white.opacity(0.10)
        self.iconWell = domainAccent.opacity(0.10)
    }

    // MARK: - Static compatibility accessors
    //
    // The token set was a `static enum` before it became a domain-aware instance struct. Call
    // sites that have no domain context (the Business create flow) still read the corporate
    // palette statically, so these forward to a shared corporate instance. Instance members keep
    // precedence at type scope, so `tokens.accent` and `BusinessEmptyTokens.accent` coexist.

    static let corporate = BusinessEmptyTokens(domain: .corporate)

    static var accent: Color { corporate.accent }
    static var textPrimary: Color { corporate.textPrimary }
    static var textSecondary: Color { corporate.textSecondary }
    static var textMuted: Color { corporate.textMuted }
    static var cardFill: Color { corporate.cardFill }
    static var cardStroke: Color { corporate.cardStroke }
    static var iconWell: Color { corporate.iconWell }
}

struct BusinessEmptyPill: View {
    let label: String
    var tokens: BusinessEmptyTokens = .corporate

    var body: some View {
        Text(label.uppercased())
            .font(.plusJakarta(size: 11, weight: .bold))
            .foregroundStyle(tokens.accent)
            .padding(.horizontal, 10)
            .padding(.vertical, 4)
            .background(tokens.iconWell)
            .overlay(Capsule().stroke(tokens.accent, lineWidth: 1))
            .clipShape(Capsule())
    }
}

struct BusinessEmptyHeadline: View {
    let title: String
    let bodyText: String
    var tokens: BusinessEmptyTokens = .corporate

    var body: some View {
        VStack(spacing: 10) {
            Text(title)
                .font(.plusJakarta(size: 32, weight: .bold))
                .foregroundStyle(tokens.textPrimary)
                .multilineTextAlignment(.center)
                .frame(maxWidth: .infinity)
            Text(bodyText)
                .font(.plusJakarta(size: 14))
                .foregroundStyle(tokens.textSecondary)
                .multilineTextAlignment(.center)
                .frame(maxWidth: .infinity)
        }
    }
}

struct BusinessEmptyCTA: View {
    let label: String
    var tokens: BusinessEmptyTokens = .corporate
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(label)
                .font(.plusJakarta(size: 15, weight: .semibold))
                .foregroundStyle(tokens.textPrimary)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 14)
                .background(tokens.accent.opacity(0.15))
                .overlay(
                    RoundedRectangle(cornerRadius: 12)
                        .stroke(tokens.accent, lineWidth: 1.5)
                )
                .clipShape(RoundedRectangle(cornerRadius: 12))
                .shadow(color: tokens.accent.opacity(0.20), radius: 6, y: 4)
        }
        .buttonStyle(.plain)
        .accessibilityLabel(label)
    }
}

struct BusinessEmptyAssetIcon: View {
    let name: String
    var size: CGFloat = 16

    var body: some View {
        Image(name)
            .resizable()
            .scaledToFit()
            .frame(width: size, height: size)
    }
}

struct BusinessEmptyAssetImage: View {
    let name: String
    var width: CGFloat
    var height: CGFloat

    var body: some View {
        Image(name)
            .resizable()
            .scaledToFill()
            .frame(width: width, height: height)
            .clipped()
    }
}

struct BusinessEmptyAppearModifier: ViewModifier {
    @State private var shown = false

    func body(content: Content) -> some View {
        content
            .opacity(shown ? 1 : 0)
            .scaleEffect(shown ? 1 : 0.98)
            .onAppear {
                withAnimation(.easeOut(duration: 0.35)) {
                    shown = true
                }
            }
    }
}

extension View {
    func businessEmptyAppear() -> some View {
        modifier(BusinessEmptyAppearModifier())
    }
}

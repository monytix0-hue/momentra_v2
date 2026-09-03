import SwiftUI

enum BusinessEmptyTokens {
    static let accent = Color(hex: "#818CF8")
    static let textPrimary = Color(hex: "#F1F5F9")
    static let textSecondary = Color(hex: "#CBD5E1")
    static let textMuted = Color(hex: "#64748B")
    static let cardFill = Color.white.opacity(0.08)
    static let cardStroke = Color.white.opacity(0.10)
    static let iconWell = Color(hex: "#818CF8").opacity(0.10)
}

struct BusinessEmptyPill: View {
    let label: String

    var body: some View {
        Text(label.uppercased())
            .font(.system(size: 11, weight: .bold))
            .foregroundStyle(BusinessEmptyTokens.accent)
            .padding(.horizontal, 10)
            .padding(.vertical, 4)
            .background(BusinessEmptyTokens.accent.opacity(0.10))
            .overlay(Capsule().stroke(BusinessEmptyTokens.accent, lineWidth: 1))
            .clipShape(Capsule())
    }
}

struct BusinessEmptyHeadline: View {
    let title: String
    let bodyText: String

    var body: some View {
        VStack(spacing: 10) {
            Text(title)
                .font(.system(size: 32, weight: .bold))
                .foregroundStyle(BusinessEmptyTokens.textPrimary)
                .multilineTextAlignment(.center)
                .frame(maxWidth: .infinity)
            Text(bodyText)
                .font(.system(size: 14))
                .foregroundStyle(BusinessEmptyTokens.textSecondary)
                .multilineTextAlignment(.center)
                .frame(maxWidth: .infinity)
        }
    }
}

struct BusinessEmptyCTA: View {
    let label: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(label)
                .font(.system(size: 15, weight: .semibold))
                .foregroundStyle(BusinessEmptyTokens.textPrimary)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 14)
                .background(BusinessEmptyTokens.accent.opacity(0.15))
                .overlay(
                    RoundedRectangle(cornerRadius: 12)
                        .stroke(BusinessEmptyTokens.accent, lineWidth: 1.5)
                )
                .clipShape(RoundedRectangle(cornerRadius: 12))
                .shadow(color: BusinessEmptyTokens.accent.opacity(0.20), radius: 6, y: 4)
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

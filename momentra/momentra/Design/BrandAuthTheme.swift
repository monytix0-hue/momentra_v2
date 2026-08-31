import SwiftUI

/// Auth UI on the global brand canvas — mirrors `.auth-*` classes in `design/momentra_theme.css`.
enum BrandSpacing {
    static let screenHorizontal: CGFloat = MomentraBrandTokens.space6
    static let xl: CGFloat = MomentraBrandTokens.space8
    static let md: CGFloat = MomentraBrandTokens.space3
    static let scrollBottom: CGFloat = MomentraBrandTokens.space8
    static let inputPaddingX: CGFloat = MomentraBrandTokens.space4
    static let inputPaddingY: CGFloat = MomentraBrandTokens.space3
    static let buttonHeight: CGFloat = 48
    static let appleButtonHeight: CGFloat = 48
    static let inputRadius: CGFloat = MomentraBrandTokens.radiusCard
}

struct BrandAuthScreenModifier: ViewModifier {
    func body(content: Content) -> some View {
        content
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(MomentraBrandTokens.brand.ignoresSafeArea())
    }
}

extension View {
    /// `.auth-screen` — brand indigo background, text-on-dark.
    func brandAuthScreen() -> some View {
        modifier(BrandAuthScreenModifier())
    }
}

struct BrandPrimaryButtonStyle: ButtonStyle {
    func makeBody(configuration: ButtonStyle.Configuration) -> some View {
        configuration.label
            .font(.system(size: 13, weight: .medium))
            .foregroundStyle(MomentraBrandTokens.textOnEmber)
            .padding(.horizontal, 20)
            .padding(.vertical, 10)
            .frame(maxWidth: .infinity)
            .frame(minHeight: BrandSpacing.buttonHeight)
            .background(
                RoundedRectangle(cornerRadius: 100, style: .continuous)
                    .fill(MomentraBrandTokens.cta.opacity(configuration.isPressed ? 0.85 : 1))
            )
    }
}

struct BrandSecondaryButtonStyle: ButtonStyle {
    func makeBody(configuration: ButtonStyle.Configuration) -> some View {
        configuration.label
            .font(.system(size: 13, weight: .medium))
            .foregroundStyle(MomentraBrandTokens.textOnDark)
            .padding(.horizontal, 20)
            .padding(.vertical, 10)
            .frame(maxWidth: .infinity)
            .frame(minHeight: BrandSpacing.buttonHeight)
            .background(
                RoundedRectangle(cornerRadius: 100, style: .continuous)
                    .strokeBorder(Color.white.opacity(0.25), lineWidth: 1.5)
            )
    }
}

struct BrandTextFieldStyle: ViewModifier {
    func body(content: Content) -> some View {
        content
            .font(.system(size: 13))
            .foregroundStyle(MomentraBrandTokens.textOnDark)
            .padding(.horizontal, BrandSpacing.inputPaddingX)
            .padding(.vertical, BrandSpacing.inputPaddingY)
            .background(
                RoundedRectangle(cornerRadius: BrandSpacing.inputRadius, style: .continuous)
                    .fill(MomentraBrandTokens.indigo500)
            )
            .overlay(
                RoundedRectangle(cornerRadius: BrandSpacing.inputRadius, style: .continuous)
                    .strokeBorder(MomentraBrandTokens.indigo100, lineWidth: 1)
            )
    }
}

extension View {
    /// `.auth-input` — indigo-500 fill, indigo-100 border.
    func brandTextField() -> some View {
        modifier(BrandTextFieldStyle())
    }
}

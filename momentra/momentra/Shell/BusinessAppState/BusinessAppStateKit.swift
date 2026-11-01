import SwiftUI

/// Body-only primitives for the Figma "App States · Shared (all Business verticals)" kit.
///
/// The kit draws a full 402x874 screen, but the shell already owns the status bar, top bar,
/// context switcher, moment selector and bottom nav. Only the "State body" region and the glow
/// ellipse behind it are rendered here, mounted inside existing chrome.
///
/// Keep in lockstep with `BusinessAppStateKit.kt` on Android.

// MARK: - Glow

/// Soft accent wash behind the state body. Figma: 440pt ellipse, accent @12%, layer blur 160.
struct AppStateGlow: View {
    let accent: Color
    var size: CGFloat = BusinessAppStateTokens.glowSize

    var body: some View {
        Circle()
            .fill(
                RadialGradient(
                    colors: [accent.opacity(BusinessAppStateTokens.glowAlpha), .clear],
                    center: .center,
                    startRadius: 0,
                    endRadius: size / 2
                )
            )
            .frame(width: size, height: size)
            .allowsHitTesting(false)
    }
}

// MARK: - Hero mark ring

/// Hero mark ring: blurred halo, accent ring, brand-mark core, accent badge with a glyph.
///
/// Figma geometry for the 120pt variant: halo 120, ring 92 inset 14 with 3pt stroke,
/// mark 80 inset 20, badge 40 at bottom-right with a 3pt page-coloured border.
struct AppStateMarkRing: View {
    let accent: Color
    let icon: AppStateIcon
    var size: CGFloat = 120

    private var scale: CGFloat { size / 120 }

    var body: some View {
        ZStack(alignment: .topLeading) {
            // halo
            Circle()
                .fill(
                    RadialGradient(
                        colors: [accent.opacity(BusinessAppStateTokens.haloAlpha), .clear],
                        center: .center,
                        startRadius: 0,
                        endRadius: size / 2
                    )
                )
                .frame(width: size, height: size)

            // ring
            Circle()
                .stroke(accent, lineWidth: BusinessAppStateTokens.badgeBorder * scale)
                .frame(width: 92 * scale, height: 92 * scale)
                .offset(x: 14 * scale, y: 14 * scale)

            // brand mark core
            Circle()
                .fill(Color(hex: "#14121B"))
                .frame(width: 80 * scale, height: 80 * scale)
                .overlay(
                    Circle().stroke(Color.white.opacity(0.15), lineWidth: 1)
                )
                .overlay(
                    Image("MomentraOfficialLogo")
                        .resizable()
                        .scaledToFit()
                        .frame(width: 80 * scale * 0.62, height: 80 * scale * 0.62)
                )
                .offset(x: 20 * scale, y: 20 * scale)

            // badge
            Circle()
                .fill(accent)
                .overlay(
                    Circle().stroke(BusinessAppStateTokens.page, lineWidth: BusinessAppStateTokens.badgeBorder * scale)
                )
                .overlay(
                    Image(systemName: icon.systemName)
                        .font(.system(size: 18 * scale, weight: .semibold))
                        .foregroundStyle(.white)
                )
                .frame(width: 40 * scale, height: 40 * scale)
                .offset(x: 76 * scale, y: 76 * scale)
        }
        .frame(width: size, height: size, alignment: .topLeading)
        .accessibilityHidden(true)
    }
}

// MARK: - Text

/// Section eyebrow inside a glass card. Figma: 12pt ExtraBold, tracking 2, accent.
struct AppStateEyebrow: View {
    let text: String
    let accent: Color

    var body: some View {
        Text(text.uppercased())
            .font(.plusJakarta(size: BusinessAppStateTokens.eyebrowSize, weight: .heavy))
            .tracking(BusinessAppStateTokens.eyebrowTracking)
            .foregroundStyle(accent)
            .frame(maxWidth: .infinity, alignment: .leading)
    }
}

/// Title. Figma: 24pt ExtraBold, centred.
struct AppStateTitle: View {
    let text: String
    var alignment: TextAlignment = .center

    var body: some View {
        Text(text)
            .font(.plusJakarta(size: BusinessAppStateTokens.titleSize, weight: .heavy))
            .foregroundStyle(.white)
            .multilineTextAlignment(alignment)
            .lineSpacing(4)
            .frame(maxWidth: .infinity, alignment: alignment == .center ? .center : .leading)
    }
}

/// Body copy. Figma: 14pt Medium, white @70%, 1.35 line height.
struct AppStateBodyText: View {
    let text: String
    var alignment: TextAlignment = .center

    var body: some View {
        Text(text)
            .font(.plusJakarta(size: BusinessAppStateTokens.bodySize, weight: .medium))
            .foregroundStyle(Color.white.opacity(0.70))
            .multilineTextAlignment(alignment)
            .lineSpacing(4)
            .frame(maxWidth: .infinity, alignment: alignment == .center ? .center : .leading)
    }
}

/// Muted footnote under the actions. Figma: 11pt SemiBold, white @50%.
struct AppStateFootnote: View {
    let text: String

    var body: some View {
        Text(text)
            .font(.plusJakarta(size: BusinessAppStateTokens.footnoteSize, weight: .semibold))
            .foregroundStyle(Color.white.opacity(0.50))
            .multilineTextAlignment(.center)
            .lineSpacing(2)
            .frame(maxWidth: .infinity)
    }
}

// MARK: - Glass card

/// Glass card: 8% white fill, 1pt accent stroke, radius 20, 16/18 padding.
struct AppStateGlassCard<Content: View>: View {
    let accent: Color
    var strokeAlpha: Double = 1
    @ViewBuilder var content: () -> Content

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            content()
        }
        .padding(.horizontal, 18)
        .padding(.vertical, 16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(GlassBackground(radius: BusinessAppStateTokens.cardRadius))
        .overlay(
            RoundedRectangle(cornerRadius: BusinessAppStateTokens.cardRadius)
                .stroke(accent.opacity(strokeAlpha), lineWidth: 1)
        )
    }
}

/// One label/value line inside a glass card.
struct AppStateGlassRow: View {
    let label: String
    let value: String
    var valueColor: Color = BusinessAppStateTokens.text

    var body: some View {
        HStack(spacing: 10) {
            Text(label)
                .font(.plusJakarta(size: BusinessAppStateTokens.rowLabelSize, weight: .medium))
                .foregroundStyle(Color.white.opacity(0.75))
                .frame(maxWidth: .infinity, alignment: .leading)
            Text(value)
                .font(.plusJakarta(size: BusinessAppStateTokens.rowValueSize, weight: .bold))
                .foregroundStyle(valueColor)
                .fixedSize(horizontal: true, vertical: false)
        }
    }
}

// MARK: - Buttons

/// Primary CTA. Figma: orange ramp, radius 999, height 50, 14pt ExtraBold tracking 1.
struct AppStatePrimaryButton: View {
    let label: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(label.uppercased())
                .font(.plusJakarta(size: BusinessAppStateTokens.buttonLabelSize, weight: .heavy))
                .tracking(BusinessAppStateTokens.buttonTracking)
                .foregroundStyle(.white)
                .frame(maxWidth: .infinity)
                .frame(height: BusinessAppStateTokens.primaryButtonHeight)
                .background(
                    LinearGradient(
                        colors: [BusinessAppStateTokens.ctaStart, BusinessAppStateTokens.ctaEnd],
                        startPoint: .leading,
                        endPoint: .trailing
                    )
                )
                .clipShape(Capsule())
                .shadow(color: BusinessAppStateTokens.ctaShadow, radius: 10, x: 0, y: 6)
        }
        .buttonStyle(.plain)
        .accessibilityLabel(label)
    }
}

/// Secondary CTA. Figma: glass 8% fill + white 22% stroke, radius 999, height 48.
struct AppStateSecondaryButton: View {
    let label: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(label)
                .font(.plusJakarta(size: BusinessAppStateTokens.buttonLabelSize, weight: .bold))
                .foregroundStyle(.white)
                .frame(maxWidth: .infinity)
                .frame(height: BusinessAppStateTokens.secondaryButtonHeight)
                .background(GlassBackground(radius: BusinessAppStateTokens.secondaryButtonHeight / 2))
                .overlay(
                    Capsule().stroke(BusinessAppStateTokens.glassStroke, lineWidth: 1)
                )
                .clipShape(Capsule())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(label)
    }
}

// MARK: - Pills & badges

/// Diagnostic pill on error states. Figma: accent @14% fill, accent @40% stroke, 11pt ExtraBold.
struct AppStateErrorCodePill: View {
    let code: String
    let accent: Color

    var body: some View {
        Text(code)
            .font(.plusJakarta(size: BusinessAppStateTokens.errorCodeSize, weight: .heavy))
            .tracking(BusinessAppStateTokens.errorCodeTracking)
            .foregroundStyle(accent)
            .padding(.horizontal, 13)
            .padding(.vertical, 7)
            .background(Capsule().fill(accent.opacity(0.14)))
            .overlay(Capsule().stroke(accent.opacity(0.40), lineWidth: 1))
    }
}

// MARK: - Skeletons & spinners

/// Shimmering placeholder bar used by the loading skeletons.
struct AppStateSkeletonBar: View {
    var width: CGFloat
    var height: CGFloat
    var radius: CGFloat = 6
    @State private var shift: CGFloat = -1

    var body: some View {
        RoundedRectangle(cornerRadius: radius)
            .fill(Color.white.opacity(0.10))
            .overlay(
                GeometryReader { geo in
                    LinearGradient(
                        colors: [.clear, Color.white.opacity(0.28), .clear],
                        startPoint: .leading,
                        endPoint: .trailing
                    )
                    .frame(width: geo.size.width * 0.55)
                    .offset(x: shift * geo.size.width)
                }
                .clipShape(RoundedRectangle(cornerRadius: radius))
            )
            .frame(width: width, height: height)
            .onAppear {
                withAnimation(.linear(duration: 1.2).repeatForever(autoreverses: false)) {
                    shift = 1.6
                }
            }
    }
}

/// Glass skeleton container used by the loading body.
struct AppStateSkeletonBox<Content: View>: View {
    let accent: Color
    var radius: CGFloat = BusinessAppStateTokens.cardRadius
    @ViewBuilder var content: () -> Content

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            content()
        }
        .padding(.horizontal, 18)
        .padding(.vertical, 16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(GlassBackground(radius: radius))
        .overlay(
            RoundedRectangle(cornerRadius: radius)
                .stroke(accent.opacity(0.35), lineWidth: 1)
        )
    }
}

/// Small circular spinner dot with an accent glow, for the loading label.
struct AppStateSpinnerDot: View {
    let accent: Color
    @State private var dim = false

    var body: some View {
        Circle()
            .fill(accent.opacity(dim ? 0.35 : 1))
            .frame(width: 10, height: 10)
            .shadow(color: accent.opacity(dim ? 0.15 : 0.55), radius: 5)
            .onAppear {
                withAnimation(.easeInOut(duration: 0.9).repeatForever(autoreverses: true)) {
                    dim = true
                }
            }
    }
}

// MARK: - Shared chrome bits

/// Divider used by list-style shells.
struct AppStateDivider: View {
    var body: some View {
        Rectangle()
            .fill(Color.white.opacity(0.06))
            .frame(height: 1)
    }
}

/// Icon button used for close / settings affordances.
struct AppStateIconButton: View {
    let systemName: String
    let accessibilityLabel: String
    let action: () -> Void
    var size: CGFloat = 40

    var body: some View {
        Button(action: action) {
            Image(systemName: systemName)
                .font(.system(size: size * 0.4, weight: .medium))
                .foregroundStyle(.white)
                .frame(width: size, height: size)
                .background(Circle().fill(Color.white.opacity(0.10)))
                .overlay(Circle().stroke(Color.white.opacity(0.20), lineWidth: 1))
        }
        .buttonStyle(.plain)
        .accessibilityLabel(accessibilityLabel)
    }
}

// MARK: - Helpers

/// Frosted glass fill used by cards and secondary buttons.
struct GlassBackground: View {
    var radius: CGFloat

    var body: some View {
        RoundedRectangle(cornerRadius: radius)
            .fill(Color.white.opacity(0.08))
    }
}
